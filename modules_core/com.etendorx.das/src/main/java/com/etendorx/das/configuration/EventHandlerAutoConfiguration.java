package com.etendorx.das.configuration;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.etendorx.eventhandler.AnnotatedEventHandlerListenerInvoker;

@Configuration
@ConditionalOnClass(EntityManagerFactory.class)
public class EventHandlerAutoConfiguration {

  @PersistenceUnit
  private EntityManagerFactory entityManagerFactory;

  @Bean
  @ConditionalOnMissingBean
  public AnnotatedEventHandlerListenerInvoker annotatedHibernateEventHandlerInvoker() {
    AnnotatedEventHandlerListenerInvoker invoker = new AnnotatedEventHandlerListenerInvoker();
    SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
    EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
    registry.prependListeners(EventType.PRE_UPDATE, invoker);
    registry.prependListeners(EventType.PRE_DELETE, invoker);
    registry.prependListeners(EventType.PRE_INSERT, invoker);
    registry.prependListeners(EventType.POST_UPDATE, invoker);
    registry.prependListeners(EventType.POST_DELETE, invoker);
    registry.prependListeners(EventType.POST_INSERT, invoker);
    registry.prependListeners(EventType.SAVE_UPDATE, invoker);
    return invoker;
  }
}
