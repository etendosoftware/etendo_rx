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

import com.etendorx.asyncprocess.service.AsyncProcessExecutionService;
import com.etendorx.lib.kafka.model.AsyncProcessExecution;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Async process creation controller.
 */
@RestController
@RequestMapping("/async-process-execution")
@Slf4j
public class AsyncProcessExecutionController {
  private final AsyncProcessExecutionService asyncProcessExecutionService;

  @Autowired
  public AsyncProcessExecutionController(AsyncProcessExecutionService asyncProcessExecutionService) {
    this.asyncProcessExecutionService = asyncProcessExecutionService;
  }

  @PostMapping(value = "/", produces = "application/json")
  public ResponseEntity<AsyncProcessExecution> postAsyncProcess(
      @RequestBody AsyncProcessExecution asyncProcessExecution) {
    log.info("exec {}", asyncProcessExecution);
    asyncProcessExecutionService.save(asyncProcessExecution);
    return ResponseEntity.ok(asyncProcessExecution);
  }
}
