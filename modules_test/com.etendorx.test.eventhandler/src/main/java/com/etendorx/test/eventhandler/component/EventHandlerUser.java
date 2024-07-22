package com.etendorx.test.eventhandler.component;

import org.hibernate.event.spi.PreUpdateEvent;
import org.openbravo.model.ad.access.User;
import org.springframework.stereotype.Component;

import com.etendorx.eventhandler.annotation.EventHandlerListener;
import com.etendorx.eventhandler.tools.EventHandlerUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EventHandlerUser {

  @EventHandlerListener
  public void handlePreUpdate(User user, PreUpdateEvent event) {
    log.info("Pre Update execute" + user.getName());
  }
}
