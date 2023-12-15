package com.etendorx.das.test.eventhandlertest.test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PreUpdateEvent;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import org.hibernate.event.spi.PreInsertEvent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.etendorx.das.test.eventhandlertest.component.HibernateEventListenerComponent;
import com.etendorx.das.test.eventhandlertest.domain.ParentEntity;
import com.etendorx.das.test.eventhandlertest.repository.ParentEntityRepository;

@EntityScan(basePackages = {"com.etendorx.das.test.eventhandlertest.*"})
@EnableJpaRepositories(basePackages = "com.etendorx.das.test.eventhandlertest.*")
@SpringBootTest
public class AnnotationTests {

  @Autowired
  private ParentEntityRepository parentEntityRepository;

  @SpyBean
  private HibernateEventListenerComponent component;

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
    registry.add("spring.datasource.url", () -> "jdbc:h2:mem:myDb;DB_CLOSE_DELAY=-1");
    registry.add("spring.datasource.password", () -> "sa");
    registry.add("spring.datasource.username", () -> "sa");
  }

  @Test
  public void createParentShouldCallInsertMethod() {
    ParentEntity parentEntity = new ParentEntity();
    parentEntity = parentEntityRepository.save(parentEntity);
    assertNotNull(parentEntity.getId());
    verify(component).handlePreInsert(eq(parentEntity), any(PreInsertEvent.class));
  }

  @Test
  public void create_parent_should_not_call_update_method() {
    ParentEntity parentEntity = new ParentEntity();
    parentEntity = parentEntityRepository.save(parentEntity);
    verify(component, never()).handlePreUpdateFirst(eq(parentEntity), any(PreUpdateEvent.class));
  }

  @Test
  public void should_call_both_pre_update_methods() {
    ParentEntity parentEntity = new ParentEntity();
    parentEntity = parentEntityRepository.save(parentEntity);

    parentEntity.setName("Test Name");
    parentEntity = parentEntityRepository.save(parentEntity);

    verify(component).handlePreUpdateFirst(eq(parentEntity), any(PreUpdateEvent.class));
    verify(component).handlePreUpdateSecond(eq(parentEntity), any(PreUpdateEvent.class));
  }

  @Test
  public void update_parent_should_call_post_update_method() {
    ParentEntity parentEntity = new ParentEntity();
    parentEntity = parentEntityRepository.save(parentEntity);

    parentEntity.setName("Test Name");
    parentEntity = parentEntityRepository.save(parentEntity);

    verify(component).handlePostUpdate(eq(parentEntity), any(PostUpdateEvent.class));
  }

  @Test
  public void should_call_pre_and_post_methods_in_order() {
    ParentEntity parentEntity = new ParentEntity();
    parentEntity = parentEntityRepository.save(parentEntity);

    parentEntity.setName("Test Name");
    parentEntity = parentEntityRepository.save(parentEntity);

    InOrder orderVerifier = Mockito.inOrder(component);
    orderVerifier.verify(component).handlePreUpdateFirst(eq(parentEntity), any(PreUpdateEvent.class));
    orderVerifier.verify(component).handlePostUpdate(eq(parentEntity), any(PostUpdateEvent.class));
  }

  @Test
  public void should_call_ordered_methods_in_order() {
    ParentEntity parentEntity = new ParentEntity();
    parentEntity = parentEntityRepository.save(parentEntity);

    parentEntity.setName("Test Name");
    parentEntity = parentEntityRepository.save(parentEntity);

    InOrder orderVerifier = Mockito.inOrder(component);
    orderVerifier.verify(component).handlePreUpdateFirst(eq(parentEntity), any(PreUpdateEvent.class));
    orderVerifier.verify(component).handlePreUpdateSecond(eq(parentEntity), any(PreUpdateEvent.class));
  }
}
