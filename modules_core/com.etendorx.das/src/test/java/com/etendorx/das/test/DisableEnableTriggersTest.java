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
  private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNjg2MDc2NjE2LCJhZF" +
      "91c2VyX2lkIjoiMTAwIiwiYWRfY2xpZW50X2lkIjoiMCIsImFkX29yZ19pZCI6IjAiLCJhZF9yb2xlX2lkIjoiMCIsInNlYXJjaF9rZXkiOiIi" +
      "LCJzZXJ2aWNlX2lkIjoiIn0.oBxwXw3Td0q1wNGVK4vSli4VGMGeRdfajwtzLCh9dVlLNFBFLJZ6EjJLUCFbZXTsxnwYHJfsHOQYcr7iWejdnP" +
      "Djy3l0CqGKFGxI-bNm_73Ky48fRdBakqzwFQExit9HfPDHd_iojp0hlpH736CWvh11v0QGja9Q0LdY4W69Np1waxUI2Qf4z2WfJaoQhIjdOq4B" +
      "cFoqqCBknVougK0J7ZMmxcOnSe6MSQ7UDzKgwunSSuT-iVeF4sxLb80hWu5dInfvn8iJVC8krJ9telWVqbo-dPoFbnFw9CtmTHpK153b4nj5U6" +
      "ZOTFP4kZqsqhvWo7wKg03O1emGmCKo1vg9Cg";

  @Autowired
  private UserContext userContext;

  @Test
  void whenExecuteRequestWithoutTriggerParam() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(FilterContext.ACTIVE_PARAMETER, FilterContext.TRUE);
    request.setMethod("GET");
    setUserContextFromToken(userContext, TOKEN, request);
    AppContext.setCurrentUser(userContext);
    userContext = AppContext.getCurrentUser();
    assert userContext.isTriggerEnabled();
  }

  @Test
  void whenExecuteRequestWithTriggerDisableParam() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(FilterContext.ACTIVE_PARAMETER, FilterContext.TRUE);
    request.setParameter(FilterContext.TRIGGER_ENABLED_PARAMETER, FilterContext.FALSE);
    request.setMethod("GET");
    UserContext userContext = new UserContext();
    setUserContextFromToken(userContext, TOKEN, request);
    AppContext.setCurrentUser(userContext);
    userContext = AppContext.getCurrentUser();
    assert !userContext.isTriggerEnabled();
  }
}
