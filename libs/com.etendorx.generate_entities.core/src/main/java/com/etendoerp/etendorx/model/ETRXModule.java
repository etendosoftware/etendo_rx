package com.etendoerp.etendorx.model;

import com.etendoerp.etendorx.model.projection.ETRXProjection;
import com.etendoerp.etendorx.model.repository.ETRXRepository;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import org.openbravo.base.model.Module;
import java.util.Set;

@JsonIncludeProperties({"projections", "repositories"})
public class ETRXModule extends Module {
    private Boolean rx = false;
    private Set<ETRXProjection> projections;
    private Set<ETRXRepository> repositories;

    public Boolean isRx() {
        return rx;
    }

    public void setRx(Boolean rx) {
        this.rx = rx;
    }

    public Set<ETRXProjection> getProjections() {
        return projections;
    }

    public void setProjections(Set<ETRXProjection> projections) {
        this.projections = projections;
    }

    public Set<ETRXRepository> getRepositories() {
        return repositories;
    }

    public void setRepositories(Set<ETRXRepository> repositories) {
        this.repositories = repositories;
    }
}