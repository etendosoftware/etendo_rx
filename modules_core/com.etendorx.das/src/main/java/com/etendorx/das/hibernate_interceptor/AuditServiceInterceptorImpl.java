/**
 * Copyright 2022-2023 Futit Services SL
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.etendorx.das.hibernate_interceptor;

import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.entities.BaseRXObject;
import com.etendorx.entities.jparepo.ADClientRepository;
import com.etendorx.entities.jparepo.ADUserRepository;
import com.etendorx.entities.jparepo.OrganizationRepository;
import com.etendorx.utils.auth.key.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;

@Service
@Slf4j
public class AuditServiceInterceptorImpl implements AuditServiceInterceptor {

  private final ADUserRepository adUserRepository;
  private final ADClientRepository adClientRepository;
  private final OrganizationRepository organizationRepository;
  private final UserContext userContext;

  public AuditServiceInterceptorImpl(ADUserRepository adUserRepository,
      ADClientRepository adClientRepository, OrganizationRepository organizationRepository,
      @Autowired UserContext userContext) {
    this.adUserRepository = adUserRepository;
    this.adClientRepository = adClientRepository;
    this.organizationRepository = organizationRepository;
    this.userContext = userContext;
  }

  @Override
  public void setAuditValues(BaseRXObject baseObject, boolean newRecord) {
    log.debug("Setting audit values for {}", baseObject.getClass().getSimpleName());
    log.debug("UserContext: {}", userContext);
    log.debug("UserContext.userId {}", userContext.getUserId());
    log.debug("UserContext.clientId {}", userContext.getClientId());
    if (baseObject.getClient() == null) {
      baseObject.setClient(adClientRepository.findById(userContext.getClientId()).orElse(null));
    }
    if (baseObject.getActive() == null) {
      baseObject.setActive(true);
    }
    if (baseObject.getCreatedBy() == null) {
      baseObject.setCreatedBy(adUserRepository.findById(userContext.getUserId()).orElse(null));
    }
    if (baseObject.getCreationDate() == null) {
      baseObject.setCreationDate(new Date());
    }
    if (baseObject.getOrganization() == null) {
      baseObject.setOrganization(
          organizationRepository.findById(userContext.getOrganizationId()).orElse(null));
    }
    baseObject.setUpdatedBy(adUserRepository.findById(userContext.getUserId()).orElse(null));
    baseObject.setUpdated(new Date());
  }
}
