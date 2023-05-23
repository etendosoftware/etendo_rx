package com.etendorx.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "services", url = "${das.url}/ETRX_Rx_Services")
public interface ServiceClient {

  @GetMapping("/search/searchByServiceId?serviceId={serviceId}&active={active}")
  public ResponseEntity<String> searchServiceByServiceId(@PathVariable(name = "serviceId") String serviceId, @PathVariable(name = "active") boolean active, @RequestHeader HttpHeaders headers);
}

