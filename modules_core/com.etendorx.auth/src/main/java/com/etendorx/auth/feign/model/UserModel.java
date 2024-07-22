package com.etendorx.auth.feign.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class UserModel {
  private String id;

  private Boolean active;

  private String client;

  private String organization;

  private String defaultClient;

  private String defaultOrganization;

  private String username;

  private String password;

  private List<ServiceAccess> eTRXRxServicesAccessList;

  public void seteTRXRxServicesAccessList(List<ServiceAccess> eTRXRxServicesAccessList) {
    this.eTRXRxServicesAccessList = eTRXRxServicesAccessList;
  }

  public List<ServiceAccess> geteTRXRxServicesAccessList() {
    return eTRXRxServicesAccessList;
  }
}
