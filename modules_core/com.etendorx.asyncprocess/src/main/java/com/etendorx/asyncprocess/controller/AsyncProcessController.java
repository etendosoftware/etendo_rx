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


import com.etendorx.lib.kafka.model.AsyncProcess;
import com.etendorx.asyncprocess.service.AsyncProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/async-process")
public class AsyncProcessController {

  private final AsyncProcessService asyncProcessService;

  @Autowired
  public AsyncProcessController(AsyncProcessService asyncProcessService) {
    this.asyncProcessService = asyncProcessService;
  }

  @GetMapping(value = "/{asyncProcessId}", produces = "application/json")
  public ResponseEntity<AsyncProcess> getAsyncProcess(@PathVariable("asyncProcessId") String asyncProcessId) {
    var asyncProcess = asyncProcessService.getAsyncProcess(asyncProcessId);
    return ResponseEntity.ok(asyncProcess);
  }

}
