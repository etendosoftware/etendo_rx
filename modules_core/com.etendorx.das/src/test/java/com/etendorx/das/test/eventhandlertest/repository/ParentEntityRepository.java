package com.etendorx.das.test.eventhandlertest.repository;

import com.etendorx.das.test.eventhandlertest.domain.ParentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentEntityRepository extends JpaRepository<ParentEntity, Long> {
}
