package com.etendoerp.etendorx.model.projection;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openbravo.base.model.ModelObject;

@JsonIncludeProperties({"name"})
public class ETRXEntityField extends ModelObject {

    private ETRXProjectionEntity entity;
    private String property;

    public ETRXProjectionEntity getEntity() {
        return entity;
    }

    public void setEntity(ETRXProjectionEntity entity) {
        this.entity = entity;
    }

    @JsonProperty("name")
    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

}
