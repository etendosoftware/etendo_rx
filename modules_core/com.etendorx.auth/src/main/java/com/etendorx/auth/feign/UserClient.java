package com.etendorx.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "users", url = "${das.url}/ADUser")
public interface UserClient {

  @GetMapping("/search/searchByUsername?username={username}")
  public ResponseEntity<CollectionModel<UserModel>> searchUserByUsername(
      @PathVariable(name = "username") String username);

  @GetMapping("/search/searchByUsername?username={username}&active={active}&projection={projection}")
  public ResponseEntity<CollectionModel<UserModel>> searchUserByUsername(
      @PathVariable(name = "username") String username, @PathVariable(name = "active") String active,
      @PathVariable(name = "projection") String projection);

}
