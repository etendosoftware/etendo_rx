package com.etendorx.test.eventhandler.component;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.event.spi.PreUpdateEvent;
import org.openbravo.model.ad.access.User;
import org.springframework.stereotype.Component;

import com.etendorx.eventhandler.annotation.EventHandlerListener;
import com.etendorx.utils.auth.key.context.UserContext;

@Component
public class EventHandlerUser {

  private HttpServletRequest request;

  public EventHandlerUser(HttpServletRequest request) {
    this.request = request;
  }

  @Resource(name = "userContextBean")
  private UserContext userContext;

  private UserContext getUserContext() {
    if (userContext == null) {
      throw new IllegalArgumentException("The user context is not defined.");
    }
    return userContext;
  }

  @EventHandlerListener
  public void handlePreInsert(User user, PreUpdateEvent event) {
    System.out.println("Pre Insert execute" + user.getName());
  }
}
