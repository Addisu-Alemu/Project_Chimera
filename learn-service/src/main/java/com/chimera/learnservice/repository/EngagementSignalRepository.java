package com.chimera.learnservice.repository;

import com.chimera.learnservice.model.EngagementSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EngagementSignalRepository extends JpaRepository<EngagementSignal, UUID> {
    List<EngagementSignal> findByPostResultId(UUID postResultId);
}
