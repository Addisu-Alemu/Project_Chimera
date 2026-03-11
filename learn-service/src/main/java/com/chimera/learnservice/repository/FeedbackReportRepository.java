package com.chimera.learnservice.repository;

import com.chimera.learnservice.model.FeedbackReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, UUID> {}
