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

package com.etendorx.asyncprocess.topology;

import com.etendorx.lib.asyncprocess.models.AsyncProcess;
import com.etendorx.lib.asyncprocess.models.AsyncProcessConstants;
import com.etendorx.lib.asyncprocess.models.AsyncProcessExecution;
import com.etendorx.lib.kafka.model.JsonSerde;
import com.etendorx.lib.kafka.topology.EtendoTopology;
import com.etendorx.lib.kafka.topology.EtendoTopologyInstance;
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.stereotype.Component;

@Component
public class AsyncProcessTopology implements EtendoTopologyInstance {

  @Override
  public void bind(StreamsBuilder streamsBuilder) {
    new EtendoTopology<>(
        streamsBuilder,
        AsyncProcessConstants.ASYNC_PROCESS_EXECUTION,
        AsyncProcessConstants.ASYNC_PROCESS,
        AsyncProcessConstants.ASYNC_PROCESS_STORE,
        new JsonSerde<>(AsyncProcessExecution.class),
        new JsonSerde<>(AsyncProcess.class),
        AsyncProcess::new
    ).build()
        .aggregate()
        .bind(streamsBuilder);
  }
}
