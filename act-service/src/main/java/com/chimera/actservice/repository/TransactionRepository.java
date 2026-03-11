package com.chimera.actservice.repository;

import com.chimera.actservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {}
