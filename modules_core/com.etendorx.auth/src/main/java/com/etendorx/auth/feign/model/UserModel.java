package com.etendorx.auth.feign.model;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
@Builder
public class UserModel {
  private String id;

  private Boolean active;

  private String clientId;

  private String organizationId;

  private String defaultClientId;

  private String defaultOrganizationId;

  private String username;

  private String password;

  private List<HashMap<String, String>> eTRXRxServicesAccessList;
}
