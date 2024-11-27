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

import com.etendorx.utils.auth.key.context.AppContext;
import com.etendorx.utils.auth.key.context.FilterContext;
import com.etendorx.utils.auth.key.context.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;

import static com.etendorx.utils.auth.key.context.FilterContext.setUserContextFromToken;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19091")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class DisableEnableTriggersTest {
  private static final String TOKEN = "eyJhbGciOiJFUzI1NiJ9.eyJhdWQiOiJzd3MiLCJhZF9vcmdfaWQiOiIxOTQwNEVBRDE0NEM0OUEwQUYzN0Q1NDM3N0NGNDUyRCIsImlzcyI6InN3cyIsImFkX3JvbGVfaWQiOiI0MkQwRUVCMUM2NkY0OTdBOTBERDUyNkRDNTk3RTZGMCIsImFkX3VzZXJfaWQiOiIxMDAiLCJhZF9jbGllbnRfaWQiOiIyM0M1OTU3NUI5Q0Y0NjdDOTYyMDc2MEVCMjU1QjM4OSIsIndhcmVob3VzZSI6IkIyRDQwRDhBNUQ2NDRERDg5RTMyOURDMjk3MzA5MDU1IiwiaWF0IjoxNzMyNzM2NjgyfQ.MEUCIBjP4KhsjsoD7f8vsndIuCT1aIRurjm9Kxc6RZXVAdS4AiEAvCP9EQvdSYxvvRRX33TGhNVnYcVeBSzahzdfmjlIEm0";

  @Autowired
  private UserContext userContext;
  private String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9Om8W9iL04NYBB0ZT/EagilvThZAPM9zmisnjmQioxpyUe6RIxpNGkRyp7qI0DAp24ejFA3/69rPf453/9Qv8g==";

  @Test
  void whenExecuteRequestWithoutTriggerParam() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(FilterContext.NO_ACTIVE_FILTER_PARAMETER, FilterContext.TRUE);
    request.setMethod("GET");
    setUserContextFromToken(userContext, publicKey, null, TOKEN, request);
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
    setUserContextFromToken(userContext, publicKey, null, TOKEN, request);
    AppContext.setCurrentUser(userContext);
    userContext = AppContext.getCurrentUser();
    assert !userContext.isTriggerEnabled();
  }
}
