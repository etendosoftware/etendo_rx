package com.etendorx.auth.feign.model;

import com.etendorx.auth.feign.model.Embedded;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicesAccessModel {

    private Embedded _embedded;
}
