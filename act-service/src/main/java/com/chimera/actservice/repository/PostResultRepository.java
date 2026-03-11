package com.chimera.actservice.repository;

import com.chimera.actservice.model.PostResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostResultRepository extends JpaRepository<PostResult, UUID> {}
