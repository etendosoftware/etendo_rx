package com.etendorx.das.handler;

import com.etendorx.das.handler.context.*;
import com.etendorx.entities.entities.BaseRXObject;
import com.etendorx.entities.jparepo.AdClientRepository;
import com.etendorx.entities.jparepo.AdOrgRepository;
import com.etendorx.entities.jparepo.AdUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@RepositoryEventHandler
@Slf4j
@Component
public class EventHandlerEntities {
    private HttpServletRequest request;

    public EventHandlerEntities(HttpServletRequest request) {
        this.request = request;
    }

    private UserContext getUserContext() {
        UserContext userContext = AppContext.getCurrentUser();
        if (userContext == null) {
            throw new IllegalArgumentException("The user context is not defined.");
        }
        return userContext;
    }

    @HandleBeforeCreate
    public void handleBeforeCreate(BaseRXObject entity) {
        log.info("handleBeforeCreate {}", entity);
        UserContext userContext = getUserContext();

        // Set client
        var adClientRepo = SpringContext.getBean(AdClientRepository.class);
        var adClientOptional = adClientRepo.findById(userContext.getClientId());
        adClientOptional.ifPresent(entity::setClient);

        // Set organization
        var adOrgRepo = SpringContext.getBean(AdOrgRepository.class);
        var adOrgOptional = adOrgRepo.findById(userContext.getOrganizationId());
        adOrgOptional.ifPresent(entity::setOrganization);

        // Set user
        var adUserRepo = SpringContext.getBean(AdUserRepository.class);
        var adUserOptional = adUserRepo.findById(userContext.getUserId());
        adUserOptional.ifPresent(entity::setCreatedBy);
        adUserOptional.ifPresent(entity::setUpdatedBy);

        entity.setCreationDate(new Date());
        entity.setUpdated(new Date());
        entity.setActive(true);
    }

    @HandleBeforeSave
    public void handleBeforeSave(BaseRXObject entity) {
        log.info("handleBeforeSave {}", entity);
        UserContext userContext = getUserContext();

        // Set user
        var adUserRepo = SpringContext.getBean(AdUserRepository.class);
        var adUserOptional = adUserRepo.findById(userContext.getUserId());
        adUserOptional.ifPresent(entity::setUpdatedBy);
        entity.setUpdated(new Date());
    }

}
