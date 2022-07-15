package com.etendorx.integrations.asyncprocess.controller;

import com.etendorx.integrations.asyncprocess.model.AsyncProcessExecution;
import com.etendorx.integrations.asyncprocess.service.AsyncProcessExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
  public ResponseEntity<AsyncProcessExecution> postAsyncProcess(@RequestBody AsyncProcessExecution asyncProcessExecution) {
    log.info("exec {}", asyncProcessExecution);
    asyncProcessExecutionService.save(asyncProcessExecution);
    return ResponseEntity.ok(asyncProcessExecution);
  }
}
