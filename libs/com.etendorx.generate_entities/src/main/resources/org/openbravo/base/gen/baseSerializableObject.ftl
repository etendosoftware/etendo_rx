package com.etendorx.entities.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface BaseSerializableObject extends Serializable {
  @JsonProperty("_identifier")
  String get_identifier();

  @JsonIgnore
  String getTableId();
}
