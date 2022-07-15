package com.etendorx.integrations.asyncprocess.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AsyncProcessExecution implements Comparable<AsyncProcessExecution> {

  private String id;
  private String asyncProcessId;
  private String log;
  private String description;

  @JsonFormat(shape = JsonFormat.Shape.STRING,
      pattern = "dd-MM-yyyy hh:mm:ss")
  private Date time;
  @Builder.Default
  private AsyncProcessState state = AsyncProcessState.ACCEPTED;

  @Override
  public int compareTo(AsyncProcessExecution o) {
    var r = o.time.compareTo(this.time);
    if (r == 0) return o.id == null ? -1 : o.id.compareTo(this.id);
    return r;
  }

}
