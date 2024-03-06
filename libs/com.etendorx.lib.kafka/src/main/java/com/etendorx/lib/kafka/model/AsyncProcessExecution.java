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
package com.etendorx.lib.kafka.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * This class represents an asynchronous process execution.
 * It implements the Comparable interface to allow for comparison between different instances of this class.
 * It is annotated with Lombok annotations to automatically generate boilerplate code like getters, setters, constructors, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AsyncProcessExecution implements Comparable<AsyncProcessExecution> {

  private String id;
  private String asyncProcessId;
  private String log;
  private String description;
  private String params;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss:SSS")
  private Date time;
  @Builder.Default
  private AsyncProcessState state = AsyncProcessState.ACCEPTED;

  /**
   * This method is used to compare the current instance with another instance of AsyncProcessExecution.
   * The comparison is primarily based on the time of execution, and if they are equal, it falls back to comparing the id.
   *
   * @param o The other instance of AsyncProcessExecution to compare with.
   * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
   */
  @Override
  public int compareTo(AsyncProcessExecution o) {
    if (this.time == null) {
      return -1;
    }
    if (o.time == null) {
      return 1;
    }
    var r = o.time.compareTo(this.time);
    if (r == 0)
      return o.id == null ? -1 : o.id.compareTo(this.id);
    return r;
  }

}
