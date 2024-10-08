/*
 * Copyright 2022-2023  Futit Services SL
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
package com.etendorx.gen.generation;

/**
 * Templates constants helper
 */
public class Templates {

  private Templates() {
  }

  public static final String BASE_SERIALIZABLE_OBJECT_FTL = "baseSerializableObject.ftl";
  public static final String BASE_ENTITY_RX_FTL = "baseEntityRx.ftl";
  public static final String BASE_DASREPOSITORY_FTL = "baseDASRepository.ftl";
  public static final String BASE_DTOREPOSITORY_FTL = "baseDTORepository.ftl";

  public static final String MAPPING_UTILS_FTL = "mappings/mappingUtils.ftl";
  public static final String AUDIT_SERVICE_INTERCEPTOR_FTL = "mappings/auditServiceInterceptor.ftl";

}
