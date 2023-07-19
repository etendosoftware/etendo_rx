package com.etendorx.das.test.eventhandlertest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.etendorx.das.test.eventhandlertest.domain.ParentEntity;

@Repository
public interface ParentEntityRepository extends JpaRepository<ParentEntity, Long> {
}
