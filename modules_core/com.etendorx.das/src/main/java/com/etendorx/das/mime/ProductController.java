package com.etendorx.das.mime;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
//@RequestMapping("/ml/product")
public class ProductController {

  //@GetMapping("/hello")
  public ResponseEntity<String> hello() {
    return ResponseEntity.ok("Hello, world!");
  }
}
