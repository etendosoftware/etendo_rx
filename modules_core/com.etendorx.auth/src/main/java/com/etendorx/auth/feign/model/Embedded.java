package com.etendorx.auth.feign.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
public class Embedded {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<RxService> rxServiceses;
}
