package com.etendorx.das.mapper.mapping.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MELIProductPictures {
  @JsonProperty("source")
  String source;
}
