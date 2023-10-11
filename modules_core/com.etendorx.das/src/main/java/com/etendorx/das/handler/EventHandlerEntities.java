package com.etendorx.das.handler;

import java.util.Date;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.BaseRXObject;
import com.etendorx.entities.jparepo.ADClientRepository;
import com.etendorx.entities.jparepo.ADUserRepository;
import com.etendorx.entities.jparepo.OrganizationRepository;
import com.etendorx.utils.auth.key.context.UserContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EventHandlerEntities {
  private HttpServletRequest request;

  public EventHandlerEntities(HttpServletRequest request) {
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

  public void handleBeforeCreate(BaseRXObject entity) {
    log.info("handleBeforeCreate {}", entity);
    UserContext userContext = getUserContext();

    // Set client
    var adClientRepo = SpringContext.getBean(ADClientRepository.class);
    var adClientOptional = adClientRepo.findById(userContext.getClientId());
    adClientOptional.ifPresent(entity::setClient);

    // Set organization
    var adOrgRepo = SpringContext.getBean(OrganizationRepository.class);
    var adOrgOptional = adOrgRepo.findById(userContext.getOrganizationId());
    adOrgOptional.ifPresent(entity::setOrganization);

    // Set user
    var adUserRepo = SpringContext.getBean(ADUserRepository.class);
    var adUserOptional = adUserRepo.findById(userContext.getUserId());
    adUserOptional.ifPresent(entity::setCreatedBy);
    adUserOptional.ifPresent(entity::setUpdatedBy);

    entity.setCreationDate(new Date());
    entity.setUpdated(new Date());
    entity.setActive(true);
  }

  public void handleBeforeSave(BaseRXObject entity) {
    log.info("handleBeforeSave {}", entity);
    UserContext userContext = getUserContext();

    // Set user
    var adUserRepo = SpringContext.getBean(ADUserRepository.class);
    var adUserOptional = adUserRepo.findById(userContext.getUserId());
    adUserOptional.ifPresent(entity::setUpdatedBy);
    entity.setUpdated(new Date());
  }

}
