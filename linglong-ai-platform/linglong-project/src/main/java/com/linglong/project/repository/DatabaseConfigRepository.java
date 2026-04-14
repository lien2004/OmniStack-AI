package com.linglong.project.repository;

import com.linglong.project.entity.DatabaseConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatabaseConfigRepository extends JpaRepository<DatabaseConfig, Long> {
    List<DatabaseConfig> findByEnabledTrue();
}
