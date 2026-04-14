package com.linglong.project.repository;

import com.linglong.project.entity.LlmProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LlmProviderRepository extends JpaRepository<LlmProvider, Long> {
    List<LlmProvider> findByEnabledTrue();
    Optional<LlmProvider> findByIsDefaultTrue();
}
