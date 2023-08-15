package com.etendorx.auth.auth;

import com.etendorx.auth.feign.model.UserModel;
import com.etendorx.utils.auth.key.JwtKeyUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.List;

public class GenerateUserClaimsTest {

    @Mock
    private UserModel userModel;

    @Mock
    private AuthService authService;

    @Test
    public void testGenerateUserClaims() {
        userModel.setId("123");
        userModel.setDefaultClientId("456");
        userModel.setClientId("789");
        HashMap<String, String> etrxRxServicesAccessList = new HashMap<>();
        etrxRxServicesAccessList.put("defaultOrgId", "1");
        etrxRxServicesAccessList.put("defaultRoleId", "2");
        etrxRxServicesAccessList.put("rxServiceId", "3");
        userModel.setETRXRxServicesAccessList(List.of(etrxRxServicesAccessList));

        String searchKey = "exampleSearchKey";

        Claims claims = authService.generateUserClaims(userModel, searchKey);

        Assertions.assertEquals(userModel.getId(), claims.get(JwtKeyUtils.USER_ID_CLAIM));
        Assertions.assertEquals(userModel.getDefaultClientId(), claims.get(JwtKeyUtils.CLIENT_ID_CLAIM));
        Assertions.assertEquals(etrxRxServicesAccessList.get("defaultOrgId"), claims.get(JwtKeyUtils.ORG_ID));
        Assertions.assertEquals(etrxRxServicesAccessList.get("defaultRoleId"), claims.get(JwtKeyUtils.ROLE_ID));
        Assertions.assertEquals(searchKey, claims.get(JwtKeyUtils.SERVICE_SEARCH_KEY));
        Assertions.assertEquals(etrxRxServicesAccessList.get("rxServiceId"), claims.get(JwtKeyUtils.SERVICE_ID));
    }
}
