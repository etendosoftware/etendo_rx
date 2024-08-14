/*
 * Copyright 2022-2024  Futit Services SL
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
package com.etendorx.entities.mapper.lib;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * This class represents a return key used in the JsonPathConverterBase class.
 * It contains information about a value read from a JSON document, including the path at which the value was read,
 * the value itself, and flags indicating whether the value was missing, null, or caused an error.
 *
 * @param <F> The type of the value.
 */
@Data
@AllArgsConstructor
public class ReturnKey<F> {

  /**
   * The path at which the value was read.
   */
  final String path;

  /**
   * The value that was read.
   */
  F value;

  /**
   * A flag indicating whether the value was missing.
   */
  final boolean missing;

  /**
   * A flag indicating whether the value was null.
   */
  final boolean nullValue;

  /**
   * A flag indicating whether an error occurred while reading the value.
   */
  final boolean error;
}
