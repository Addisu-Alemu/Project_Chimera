package com.chimera.actservice.controller;

import com.chimera.actservice.model.Transaction;
import com.chimera.actservice.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionManager transactionManager;

    public TransactionController(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @PostMapping("/transactions")
    public ResponseEntity<Transaction> createTransaction(@RequestBody Map<String, Object> body) {
        UUID agentId = UUID.fromString((String) body.get("agentId"));
        String type = (String) body.get("type");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String currency = (String) body.getOrDefault("currency", "USD");
        String platform = (String) body.getOrDefault("platform", null);
        UUID contentBundleId = body.containsKey("contentBundleId")
                ? UUID.fromString((String) body.get("contentBundleId")) : null;
        String actor = (String) body.getOrDefault("actor", "system");

        log.info("POST /transactions agentId={} type={} amount={}", agentId, type, amount);
        Transaction tx = transactionManager.process(agentId, type, amount, currency, platform, contentBundleId, actor);

        int status = tx.getStatus().name().equals("PENDING_APPROVAL") ? 202 : 201;
        return ResponseEntity.status(status).body(tx);
    }

    @PostMapping("/transactions/{id}/approve")
    public ResponseEntity<Transaction> approveTransaction(@PathVariable UUID id,
                                                           @RequestBody Map<String, String> body) {
        String operatorId = body.get("operatorId");
        String decision = body.get("decision");
        log.info("POST /transactions/{}/approve operatorId={} decision={}", id, operatorId, decision);
        Transaction result = transactionManager.approve(id, operatorId, decision);
        return ResponseEntity.ok(result);
    }
}
