package com.etendorx.integrations.asyncprocess.controller;


import com.etendorx.integrations.asyncprocess.model.AsyncProcess;
import com.etendorx.integrations.asyncprocess.service.AsyncProcessService;
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
