package com.etendorx.auth.feign.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
public class RxService {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private String secret;
    private String searchkey;
}


