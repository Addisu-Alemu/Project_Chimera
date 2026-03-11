package com.chimera.actservice.service;

import com.chimera.actservice.alert.HumanAlertService;
import com.chimera.actservice.client.ContentCreatorClient;
import com.chimera.actservice.client.LearnServiceClient;
import com.chimera.actservice.client.dto.ContentBundleDto;
import com.chimera.actservice.client.dto.EngagementSignalDto;
import com.chimera.actservice.model.AlertType;
import com.chimera.actservice.model.PostResult;
import com.chimera.actservice.model.PostStatus;
import com.chimera.actservice.publisher.ContentPublisher;
import com.chimera.actservice.repository.PostResultRepository;
import com.chimera.actservice.validator.ContentSpecValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ActService {

    private static final Logger log = LoggerFactory.getLogger(ActService.class);
    private static final int MAX_RETRIES = 3;

    private final ContentCreatorClient contentCreatorClient;
    private final ContentSpecValidator validator;
    private final List<ContentPublisher> publishers;
    private final PostResultRepository postResultRepository;
    private final LearnServiceClient learnServiceClient;
    private final HumanAlertService humanAlertService;

    public ActService(ContentCreatorClient contentCreatorClient,
                      ContentSpecValidator validator,
                      List<ContentPublisher> publishers,
                      PostResultRepository postResultRepository,
                      LearnServiceClient learnServiceClient,
                      HumanAlertService humanAlertService) {
        this.contentCreatorClient = contentCreatorClient;
        this.validator = validator;
        this.publishers = publishers;
        this.postResultRepository = postResultRepository;
        this.learnServiceClient = learnServiceClient;
        this.humanAlertService = humanAlertService;
    }

    public PostResult publish(UUID agentId, UUID contentBundleId, String platform) {
        log.info("ActService.publish agentId={} bundleId={} platform={}", agentId, contentBundleId, platform);

        ContentBundleDto bundle = contentCreatorClient.getBundle(contentBundleId);
        validator.validate(bundle);

        ContentPublisher publisher = publishers.stream()
                .filter(p -> p.platform().equalsIgnoreCase(platform))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No publisher for platform: " + platform));

        String platformPostId = null;
        String failureReason = null;
        PostStatus status = PostStatus.FAILED;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                platformPostId = publisher.publish(bundle);
                status = PostStatus.SUCCESS;
                log.info("Published successfully attempt={} platformPostId={}", attempt, platformPostId);
                break;
            } catch (Exception e) {
                failureReason = e.getMessage();
                log.warn("Publish attempt {}/{} failed: {}", attempt, MAX_RETRIES, failureReason);
            }
        }

        UUID postResultId = UUID.randomUUID();

        if (status != PostStatus.SUCCESS) {
            status = PostStatus.HELD_FOR_HUMAN;
            humanAlertService.raise(
                    agentId, AlertType.POST_FAILURE, contentBundleId,
                    "/post-results/" + postResultId,
                    "3 retries", "3 failures"
            );
        }

        PostResult result = new PostResult(
                postResultId, agentId, contentBundleId, platform,
                status == PostStatus.SUCCESS ? Instant.now() : null,
                status, status == PostStatus.SUCCESS ? 1 : MAX_RETRIES,
                failureReason, platformPostId
        );

        PostResult saved = postResultRepository.save(result);

        if (status == PostStatus.SUCCESS) {
            EngagementSignalDto signal = new EngagementSignalDto(
                    agentId, saved.getId(), "VIEW", 0L, Instant.now()
            );
            learnServiceClient.sendEngagementSignal(signal);
            learnServiceClient.triggerAnalysis(agentId, saved.getId());
        }

        return saved;
    }
}
