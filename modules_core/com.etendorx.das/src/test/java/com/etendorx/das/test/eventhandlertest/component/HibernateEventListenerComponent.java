package com.etendorx.das.test.eventhandlertest.component;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreUpdateEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.etendorx.das.test.eventhandlertest.domain.ParentEntity;
import com.etendorx.eventhandler.annotation.EventHandlerListener;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class HibernateEventListenerComponent {

    @EventHandlerListener
    public void handlePreInsert(ParentEntity entity, PreInsertEvent event){
        log.info("Pre Insert execute");
    }

    @EventHandlerListener
    public void handlePostInsert(ParentEntity entity, PostInsertEvent event){
        log.info("Post Insert execute");
    }

    @Order(2)
    @EventHandlerListener
    public void handlePreUpdateSecond(ParentEntity entity, PreUpdateEvent event){
        log.info("Pre Update execute in second order");
    }

    @Order(1)
    @EventHandlerListener
    public void handlePreUpdateFirst(ParentEntity entity, PreUpdateEvent event){
        log.info("Pre Update execute in first order");
    }

    @EventHandlerListener
    public void handlePostUpdate(ParentEntity entity, PostUpdateEvent event){
        log.info("Post Update execute");
    }
}
