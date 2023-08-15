/**
 * Copyright 2022 Futit Services SL
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
package org.openbravo.model.pricing.priceadjustment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Test PricingAdjustment Projection Class
 *
 * @author Sebastian Barrozo
 */
@Projection(name = "test", types = org.openbravo.model.pricing.priceadjustment.PriceAdjustment.class)
public interface PriceAdjustmentTestProjection {
    @JsonProperty("id")
    java.lang.String getId();

    @JsonProperty("name")
    java.lang.String getName();

    @JsonProperty("startingDate")
    java.util.Date getStartingDate();

}