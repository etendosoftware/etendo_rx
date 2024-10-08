/**
 * Copyright 2022 Futit Services SL
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbravo.model.ad.access;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.List;
import java.util.Map;

/**
 * Test ADUser Projection Class
 *
 * @author Sebastian Barrozo
 */
@Projection(name = "mapping-test", types = User.class)
public interface UserJMTestProjection {
  @JsonProperty("id")
  String getId();

  @Value("#{@userTestMapping.getName(target)}")
  Map<String, String> getName();

  @Value("#{@userTestMapping.getRoles(target)}")
  List<Map<String, Object>> getRoles();
}
