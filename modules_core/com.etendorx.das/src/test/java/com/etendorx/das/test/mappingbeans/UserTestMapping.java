package com.etendorx.das.test.mappingbeans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.springframework.stereotype.Component;

@Component
public class UserTestMapping {
  public Map<String, String> getName(User user) {
    Map<String, String> map = new java.util.HashMap<>();
    map.put("mappedName", user.getName());
    return map;
  }

  public List<Map<String, Object>> getRoles(User user) {
    List<Map<String, Object>> roles = new ArrayList<>();
    for (UserRoles userRoles : user.getADUserRolesList()) {
      Map<String, Object> role = new java.util.HashMap<>();
      role.put("name", userRoles.getRole().getName());
      role.put("isAdmin", userRoles.getRoleAdmin());
      roles.add(role);
    }
    return roles;
  }

}
