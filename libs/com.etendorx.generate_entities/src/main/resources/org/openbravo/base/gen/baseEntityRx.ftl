/*
 * Copyright 2022  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.etendorx.entities.entities;

import lombok.Getter;
import lombok.Setter;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseRXObject {
    @javax.persistence.JoinColumn(name = "ad_client_id", referencedColumnName = "AD_Client_id")
    @javax.persistence.ManyToOne(fetch=javax.persistence.FetchType.LAZY)
    @JsonBackReference
    Client client;

    @javax.persistence.JoinColumn(name = "ad_org_id", referencedColumnName = "AD_Org_id")
    @javax.persistence.ManyToOne(fetch=javax.persistence.FetchType.LAZY)
    @JsonBackReference
    Organization organization;

    @javax.persistence.Column(name = "isactive")
    @javax.persistence.Convert(converter= com.etendorx.entities.utilities.BooleanToStringConverter.class)
    @JsonBackReference
    java.lang.Boolean active;

    @javax.persistence.Column(name = "created")
    java.util.Date creationDate;

    @javax.persistence.JoinColumn(name = "createdby", referencedColumnName = "AD_User_id")
    @javax.persistence.ManyToOne(fetch=javax.persistence.FetchType.LAZY)
    @JsonBackReference
    User createdBy;

    @javax.persistence.Column(name = "updated")
    java.util.Date updated;

    @javax.persistence.JoinColumn(name = "updatedby", referencedColumnName = "AD_User_id")
    @javax.persistence.ManyToOne(fetch=javax.persistence.FetchType.LAZY)
    @JsonBackReference
    User updatedBy;

}
