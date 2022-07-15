package com.etendorx.integrations.asyncprocess.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.TreeSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncProcess {

  private String id;
  @JsonFormat(shape = JsonFormat.Shape.STRING,
      pattern = "dd-MM-yyyy hh:mm:ss")
  private Date lastUpdate;
  private String description;
  private AsyncProcessState state = AsyncProcessState.WAITING;
  private TreeSet<AsyncProcessExecution> executions = new TreeSet<>();

  public AsyncProcess process(AsyncProcessExecution asyncProcessExecution) {
    this.id = asyncProcessExecution.getAsyncProcessId();
    addExecution(asyncProcessExecution);
    this.lastUpdate = asyncProcessExecution.getTime();
    this.state = asyncProcessExecution.getState();
    return this;
  }

  private void addExecution(AsyncProcessExecution transactionClone) {
    executions.add(transactionClone);
  }
}
