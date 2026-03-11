package com.chimera.actservice.repository;

import com.chimera.actservice.model.HumanAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HumanAlertRepository extends JpaRepository<HumanAlert, UUID> {}
