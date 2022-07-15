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

package com.etendorx.asyncprocess.model;

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
