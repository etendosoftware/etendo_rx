/*
 * Copyright 2022-2023  Futit Services SL
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

import java.io.Serializable;

import jakarta.persistence.Convert;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.YesNoConverter;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

import jakarta.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseRXObject implements BaseSerializableObject {
    @jakarta.persistence.JoinColumn(name = "ad_client_id", referencedColumnName = "AD_Client_id")
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    Client client;

    @jakarta.persistence.JoinColumn(name = "ad_org_id", referencedColumnName = "AD_Org_id")
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    Organization organization;

    @jakarta.persistence.Column(name = "isactive")
    @Convert(converter = YesNoConverter.class)
    java.lang.Boolean active;

    @jakarta.persistence.Column(name = "created")
    java.util.Date creationDate;

    @jakarta.persistence.JoinColumn(name = "createdby", referencedColumnName = "AD_User_id")
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    User createdBy;

    @jakarta.persistence.Column(name = "updated")
    java.util.Date updated;

    @jakarta.persistence.JoinColumn(name = "updatedby", referencedColumnName = "AD_User_id")
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    User updatedBy;

    @JsonProperty("_identifier")
    public abstract String get_identifier();
}
