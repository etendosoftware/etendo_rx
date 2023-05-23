package com.etendorx.auth.feign.model;

import com.etendoerp.etendorx.data.RxServices;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
public class Embedded {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<RxServices> rxServiceses;
}
