package com.etendorx.auth.feign.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceAccess {
  String id;
  String defaultRoleId;
  String defaultOrgId;
  String rxServiceId;
}
