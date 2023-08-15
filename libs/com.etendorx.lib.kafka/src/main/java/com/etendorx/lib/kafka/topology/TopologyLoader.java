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

package com.etendorx.lib.kafka.topology;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TopologyLoader {

  private final List<EtendoTopologyInstance> topologies;

  public TopologyLoader(List<EtendoTopologyInstance> topologies) {
    this.topologies = topologies;
  }

  public Topology loadTopologies() {
    StreamsBuilder streamsBuilder = new StreamsBuilder();
    topologies.forEach(topology -> topology.bind(streamsBuilder));
    return streamsBuilder.build();
  }
}
