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

package com.etendorx.asyncprocess.controller;


import com.etendorx.asyncprocess.service.AsyncProcessService;
import com.etendorx.lib.asyncprocess.models.AsyncProcess;
import com.etendorx.lib.asyncprocess.models.AsyncProcessState;
import com.etendorx.lib.asyncprocess.utils.KafkaMessageUtil;
import com.etendorx.utils.auth.key.context.AppContext;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@RestController
@RequestMapping("/async-process")
public class AsyncProcessController {

  private final AsyncProcessService asyncProcessService;
  private final KafkaMessageUtil kafkaMessageUtil;
  private final StreamBridge streamBridge;

  public AsyncProcessController(AsyncProcessService asyncProcessService, KafkaMessageUtil kafkaMessageUtil, StreamBridge streamBridge) {
    this.asyncProcessService = asyncProcessService;
    this.kafkaMessageUtil = kafkaMessageUtil;
    this.streamBridge = streamBridge;
  }

  @GetMapping(value = "/{asyncProcessId}", produces = "application/json")
  public ResponseEntity<AsyncProcess> getAsyncProcess(@PathVariable("asyncProcessId") String asyncProcessId) {
    var asyncProcess = asyncProcessService.getAsyncProcess(asyncProcessId);
    return ResponseEntity.ok(asyncProcess);
  }

  @PostMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> index(
      @RequestBody Map<String, Map<String, ?>> bodyChanges,
      @RequestParam(required = false, name = "process") String processName
  ) throws Exception {
    Map<String, String> ret = new HashMap<>();
    Map<String, Object> session = new HashMap<>();

    session.put("X-TOKEN", AppContext.getAuthToken());
    bodyChanges.put("session", session);

    try {
      var uuid = message(kafkaMessageUtil, streamBridge, null, processName, bodyChanges);
      if (uuid != null) {
        ret.put("status", "OK");
        ret.put("id", uuid);
      } else {
        ret.put("status", "ERROR");
      }
    } catch (Exception e) {
      ret.put("status", "ERROR");
      ret.put("message", e.getMessage());
      throw e;
    }
    return ret;
  }

  public static String message(KafkaMessageUtil kafkaMessageUtil, StreamBridge streamBridge, String reqUid, String processName, Object bodyChanges) throws Exception {
    return message(kafkaMessageUtil, streamBridge, reqUid, processName, bodyChanges, null);
  }
  public static String message(KafkaMessageUtil kafkaMessageUtil, StreamBridge streamBridge, String reqUid, String processName, Object bodyChanges, String messageDescription) throws Exception {

    String mid;
    if (reqUid == null) {
      UUID uuid = UUID.randomUUID();
      mid = uuid.toString().toUpperCase().replace("-", "");
    } else {
      mid = reqUid;
    }
    if (bodyChanges.getClass().isAssignableFrom(LinkedHashMap.class)) {
      var localBody = (Map<String, Map<String, String>>) bodyChanges;
      Map<String, String> session;
      if (localBody.containsKey("session")) {
        session = localBody.get("session");
      } else {
        session = new LinkedHashMap<>();
        localBody.put("session", session);
      }
      session.put("id", mid);
    }

    kafkaMessageUtil.saveProcessExecution(
        bodyChanges, mid,
        messageDescription == null ? "Sync message received" : messageDescription, AsyncProcessState.ACCEPTED);

    Message<Object> message = MessageBuilder.withPayload(bodyChanges)
        .setHeader(KafkaHeaders.MESSAGE_KEY, mid.getBytes(StandardCharsets.UTF_8))
        .build();
    if (!streamBridge.send(processName, message)) {
      throw new Exception("Error sending message");
    }

    return mid;
  }


}
