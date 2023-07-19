package com.etendorx.das.test.eventhandlertest.component;

import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreUpdateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.etendorx.das.test.eventhandlertest.domain.ParentEntity;
import com.etendorx.das.test.eventhandlertest.repository.ParentEntityRepository;
import com.etendorx.eventhandler.annotation.EventHandlerListener;

@Component
public class HibernateEventListenerComponent {

    @EventHandlerListener
    public void handlePreInsert(ParentEntity entity, PreInsertEvent event){
        System.out.println("test");
    }

    @Order(2)
    @EventHandlerListener
    public void handlePreUpdateSecond(ParentEntity entity, PreUpdateEvent event){

    }

    @Order(1)
    @EventHandlerListener
    public void handlePreUpdateFirst(ParentEntity entity, PreUpdateEvent event){
        System.out.println("test");
    }

    @EventHandlerListener
    public void handlePostUpdate(ParentEntity entity, PostUpdateEvent event){

    }
}
