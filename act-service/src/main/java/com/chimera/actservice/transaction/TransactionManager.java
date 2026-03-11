package com.chimera.actservice.transaction;

import com.chimera.actservice.alert.HumanAlertService;
import com.chimera.actservice.model.AlertType;
import com.chimera.actservice.model.Transaction;
import com.chimera.actservice.model.TransactionStatus;
import com.chimera.actservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service("txManager")
public class TransactionManager {

    private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);
    private static final BigDecimal ALERT_THRESHOLD = new BigDecimal("500");

    private final TransactionRepository transactionRepository;
    private final HumanAlertService humanAlertService;

    public TransactionManager(TransactionRepository transactionRepository,
                               HumanAlertService humanAlertService) {
        this.transactionRepository = transactionRepository;
        this.humanAlertService = humanAlertService;
    }

    public Transaction process(UUID agentId, String type, BigDecimal amount, String currency,
                                String platform, UUID contentBundleId, String actor) {
        boolean requiresApproval = amount.compareTo(ALERT_THRESHOLD) > 0;
        TransactionStatus status = requiresApproval ? TransactionStatus.PENDING_APPROVAL : TransactionStatus.COMPLETED;

        Transaction tx = new Transaction(
                UUID.randomUUID(), agentId,
                com.chimera.actservice.model.TransactionType.valueOf(type.toUpperCase()),
                amount, currency, platform, contentBundleId, status, actor
        );
        if (status == TransactionStatus.COMPLETED) {
            tx.setCompletedAt(Instant.now());
        }

        Transaction saved = transactionRepository.save(tx);
        log.info("Transaction saved id={} status={} amount={}", saved.getId(), status, amount);

        if (requiresApproval) {
            humanAlertService.raise(
                    agentId, AlertType.TRANSACTION_THRESHOLD, saved.getId(),
                    "/transactions/" + saved.getId(),
                    "$" + ALERT_THRESHOLD, "$" + amount
            );
        }

        return saved;
    }

    public Transaction approve(UUID transactionId, String operatorId, String decision) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        TransactionStatus newStatus = "APPROVED".equalsIgnoreCase(decision)
                ? TransactionStatus.APPROVED : TransactionStatus.REJECTED;

        Transaction approvedTx = new Transaction(
                UUID.randomUUID(), tx.getAgentId(), tx.getType(), tx.getAmount(),
                tx.getCurrency(), tx.getPlatform(), tx.getContentBundleId(), newStatus, tx.getActor()
        );
        approvedTx.setApproverId(operatorId);
        approvedTx.setCompletedAt(Instant.now());

        return transactionRepository.save(approvedTx);
    }
}
