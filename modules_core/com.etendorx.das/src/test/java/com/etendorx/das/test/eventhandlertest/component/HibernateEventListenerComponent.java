package com.etendorx.das.test.eventhandlertest.component;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreUpdateEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.etendorx.das.test.eventhandlertest.domain.ParentEntity;
import com.etendorx.eventhandler.annotation.EventHandlerListener;

@Component
public class HibernateEventListenerComponent {

    @EventHandlerListener
    public void handlePreInsert(ParentEntity entity, PreInsertEvent event){
        System.out.println("Pre Insert execute");
    }

    @EventHandlerListener
    public void handlePostInsert(ParentEntity entity, PostInsertEvent event){
        System.out.println("Post Insert execute");
    }

    @Order(2)
    @EventHandlerListener
    public void handlePreUpdateSecond(ParentEntity entity, PreUpdateEvent event){
        System.out.println("Pre Update execute in second order");
    }

    @Order(1)
    @EventHandlerListener
    public void handlePreUpdateFirst(ParentEntity entity, PreUpdateEvent event){
        System.out.println("Pre Update execute in first order");
    }

    @EventHandlerListener
    public void handlePostUpdate(ParentEntity entity, PostUpdateEvent event){
        System.out.println("Post Update execute");
    }
}
