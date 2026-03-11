package com.chimera.learnservice.repository;

import com.chimera.learnservice.model.HumanAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HumanAlertRepository extends JpaRepository<HumanAlert, UUID> {}
