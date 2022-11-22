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


import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.etendorx.asyncprocess.service.AsyncProcessService;
import com.etendorx.lib.kafka.KafkaMessageUtil;
import com.etendorx.lib.kafka.model.AsyncProcess;
import com.etendorx.lib.kafka.model.AsyncProcessState;
import com.etendorx.utils.auth.key.context.AppContext;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;

/**
 * Controller for rest api access to async process
 */
@RestController
@RequestMapping("/async-process")
@Slf4j
public class AsyncProcessController {

  private final AsyncProcessService asyncProcessService;
  private final KafkaMessageUtil kafkaMessageUtil;
  private final StreamBridge streamBridge;
  private final KafkaReceiver<String, AsyncProcess> kafkaReceiver;
  private ConnectableFlux<ServerSentEvent<AsyncProcess>> eventPublisher;

  public AsyncProcessController(AsyncProcessService asyncProcessService, KafkaMessageUtil kafkaMessageUtil,
      StreamBridge streamBridge, KafkaReceiver<String, AsyncProcess> kafkaReceiver) {
    this.asyncProcessService = asyncProcessService;
    this.kafkaMessageUtil = kafkaMessageUtil;
    this.streamBridge = streamBridge;

    this.kafkaReceiver = kafkaReceiver;
  }

  @Operation(summary = "Get current status of execution")
  @GetMapping(value = "/{asyncProcessId}", produces = "application/json")
  public ResponseEntity<AsyncProcess> getAsyncProcess(@PathVariable("asyncProcessId") String asyncProcessId) {
    var asyncProcess = asyncProcessService.getAsyncProcess(asyncProcessId);
    return ResponseEntity.ok(asyncProcess);
  }

  @SendTo("/topic/message")
  public AsyncProcess broadcastMessage(@Payload AsyncProcess textMessageDTO) {
    return textMessageDTO;
  }

  @Operation(summary = "Create the async process")
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

  public static String message(KafkaMessageUtil kafkaMessageUtil, StreamBridge streamBridge, String reqUid,
      String processName, Object bodyChanges) throws Exception {
    return message(kafkaMessageUtil, streamBridge, reqUid, processName, bodyChanges, null);
  }

  public static String message(
      KafkaMessageUtil kafkaMessageUtil, StreamBridge streamBridge, String requestUuid, String processName,
      Object bodyChanges, String messageDescription) throws Exception {

    String uuid;
    if (requestUuid == null) {
      uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "");
    } else {
      uuid = requestUuid;
    }

    if (bodyChanges.getClass().isAssignableFrom(LinkedHashMap.class)) {
      @SuppressWarnings("unchecked")
      var localBody = (Map<String, Map<String, String>>) bodyChanges;
      Map<String, String> session;
      if (localBody.containsKey("session")) {
        session = localBody.get("session");
      } else {
        session = new LinkedHashMap<>();
        localBody.put("session", session);
      }
      session.put("id", uuid);
    }

    kafkaMessageUtil.saveProcessExecution(
        bodyChanges, uuid, messageDescription == null ? "Sync message received" : messageDescription,
        AsyncProcessState.ACCEPTED
    );

    Message<Object> message = MessageBuilder.withPayload(bodyChanges)
        .setHeader(KafkaHeaders.MESSAGE_KEY, uuid.getBytes(StandardCharsets.UTF_8))
        .build();
    if (!streamBridge.send(processName, message)) {
      throw new Exception("Error sending message");
    }

    return uuid;
  }

  @PostConstruct
  public void init() {
    eventPublisher = kafkaReceiver.receive()
        .map(consumerRecord -> ServerSentEvent.builder(consumerRecord.value()).build())
        .publish();
    eventPublisher.connect();
  }

  @GetMapping(value = "/sse/{processId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  Flux<ServerSentEvent<AsyncProcess>> getEventsFlux(@PathVariable String processId) {
    return eventPublisher
        .filter(asyncProcessServerSentEvent -> asyncProcessServerSentEvent.data().getId().equals(processId))
        .map(ServerSentEvent::data)
        .map(this::clientEventToServerSentEvent)
        .doOnSubscribe(subscription -> log.info("[ON_SUBSCRIBE]"))
        .doOnCancel(() -> log.info("[ON_CANCEL]"))
        .doOnError(e -> log.error("[ON_ERROR= {}]", e.getMessage()))
        .doFinally(signalType -> log.info("[FINALLY] [SIGNAL_TYPE= {}]", signalType.name()));
  }

  private ServerSentEvent<AsyncProcess> clientEventToServerSentEvent(AsyncProcess event) {
    return ServerSentEvent.<AsyncProcess>builder()
        .data(event)
        .build();
  }
}
