/**
* Copyright 2022-2024 Futit Services SL
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.etendorx.entities.mappings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "List of objects ${entity.externalName}")
public class ${mappingPrefix}${entity.externalName}DTO<#if entity.mappingType == "R">Read<#else>Write</#if>RequestList {
  @Schema(description = "List of objects ${entity.externalName}")
  private List<${mappingPrefix}${entity.externalName}DTO<#if entity.mappingType == "R">Read<#else>Write</#if>Request> ${entity.externalName?uncap_first}s;
}
