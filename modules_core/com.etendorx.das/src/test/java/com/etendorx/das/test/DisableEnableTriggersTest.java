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

package com.etendorx.das.test;


import static com.etendorx.utils.auth.key.context.FilterContext.setUserContextFromToken;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.etendorx.utils.auth.key.context.AppContext;
import com.etendorx.utils.auth.key.context.FilterContext;
import com.etendorx.utils.auth.key.context.UserContext;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19091")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class DisableEnableTriggersTest {
  private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNjk3MTM1NjE4LCJhZF91c2VyX2lkIjoiMTAwIiwiYWRfY2xpZW50X2lkIjoiMCIsImFkX29yZ19pZCI6IjAiLCJhZF9yb2xlX2lkIjoiMCIsInNlYXJjaF9rZXkiOiIiLCJzZXJ2aWNlX2lkIjoiIn0.JXgTYhxyK23BZXJEOObs2n4ms4Fls3jXSsKNScT90j-22c8Ypo4ZYkjt0ucKLEjccuPSsxiyGfwH6fwtdoeAxSr9dQCQaV94jjJHXm75IgncgF1YHTUTCgZQ073prX6lyHdQZ0okhBrDMHiEo1_JWbLbEluiERzJFk0t9NYcXJcNAVvTK50zPJk4Ar0cwOEtIEP1nIPOeA8vQY2NBff5t3x_CCVNgdl3BC19nx-iWBf_6xyF0mRAs2uNQ5KI9aY63vjv_z1e_j4Wupff67oQHRwMLnJShemWnjbs71s6S5SbMrUfM2Z8TkLYC964rGlUYnmwHuCTkDoBDNKebYEINw";

  @Autowired
  private UserContext userContext;
  private String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAocs6752BX8E9sUSkP0nnQlp9QNtTsBHB/jFZOro2ayCf203u3DHCPrLpLDZyrqAasIRRxKAAMNfmhl7/Hgg5FKeLp8rKEavlDTblVfVLvBmYpoJMxE2RumW4SdyP56LNnSlY49srflyiJyd9w+m0vVxMpXPT1RWTv+FJibVB8asqyUWW5sJgQ8Cr3PLI8KDCcwSpjlkkack3vB2ZiFtZVPntj4C6+/o5hcPgUeLVOFjH1H9zJP/ELLcueZtSbRo4J1CJsLUyY3ZCIk84wZwfielygT6Yl3tNqGGnxm7moXO8+y5uJZymoMqEhV5OnlolpAb/VuGZviv932fWzEMRjwIDAQAB";

  @Test
  void whenExecuteRequestWithoutTriggerParam() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(FilterContext.NO_ACTIVE_FILTER_PARAMETER, FilterContext.TRUE);
    request.setMethod("GET");
    setUserContextFromToken(userContext, publicKey, TOKEN, request);
    AppContext.setCurrentUser(userContext);
    userContext = AppContext.getCurrentUser();
    assert userContext.isTriggerEnabled();
  }

  @Test
  void whenExecuteRequestWithTriggerDisableParam() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(FilterContext.NO_ACTIVE_FILTER_PARAMETER, FilterContext.TRUE);
    request.setParameter(FilterContext.TRIGGER_ENABLED_PARAMETER, FilterContext.FALSE);
    request.setMethod("GET");
    UserContext userContext = new UserContext();
    setUserContextFromToken(userContext, publicKey, TOKEN, request);
    AppContext.setCurrentUser(userContext);
    userContext = AppContext.getCurrentUser();
    assert !userContext.isTriggerEnabled();
  }
}
