package com.etendorx.auth.auth;

import com.etendorx.auth.feign.model.ServiceAccess;
import com.etendorx.auth.feign.model.UserModel;
import com.etendorx.utils.auth.key.JwtKeyUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class GenerateUserClaimsTest {

  @Test
  void testGenerateUserClaims() {
    UserModel userModel = new UserModel();
    userModel.setId("123");
    userModel.setDefaultClient("456");
    userModel.setClient("789");
    ServiceAccess serviceAccess = new ServiceAccess();
    serviceAccess.setDefaultOrgId("1");
    serviceAccess.setDefaultRoleId("2");
    serviceAccess.setRxServiceId("3");
    userModel.seteTRXRxServicesAccessList(List.of(serviceAccess));

    String searchKey = "exampleSearchKey";

    AuthService authService = new AuthService();
    Claims claims = authService.generateUserClaims(userModel, searchKey);

    Assertions.assertEquals(userModel.getId(), claims.get(JwtKeyUtils.USER_ID_CLAIM));
    Assertions.assertEquals(userModel.getDefaultClient(), claims.get(JwtKeyUtils.CLIENT_ID_CLAIM));
    Assertions.assertEquals(serviceAccess.getDefaultOrgId(), claims.get(JwtKeyUtils.ORG_ID));
    Assertions.assertEquals(serviceAccess.getDefaultRoleId(), claims.get(JwtKeyUtils.ROLE_ID));
    Assertions.assertEquals(serviceAccess.getRxServiceId(), claims.get(JwtKeyUtils.SERVICE_ID));
  }
}
