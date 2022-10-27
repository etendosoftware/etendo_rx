package com.etendorx.clientrest.base;

import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface ClientRestBase<T extends RepresentationWithId<T>> {

  @PatchMapping("/{id}")
  ResponseEntity<T> update(@RequestBody T entity, @PathVariable String id, @RequestHeader("If-Match") String version);

  @PatchMapping("/{id}")
  ResponseEntity<T> update(@RequestBody T entity, @PathVariable String id, @RequestHeader HttpHeaders headers);

  @PostMapping("/{id}")
  ResponseEntity<T> post(@RequestBody T entity, @RequestHeader HttpHeaders headers);

  @GetMapping("/")
  public PagedModel<T> findAll();

  @GetMapping("/?size={size}&projection={projection}")
  public PagedModel<T> findAll(@PathVariable Long size, @PathVariable String projection);

  @GetMapping("/{id}")
  ResponseEntity<T> findById(@PathVariable String id);

  @GetMapping("/{id}?projection={projection}")
  ResponseEntity<T> findById(@PathVariable String id, @PathVariable String projection);

  @PostMapping("/")
  public ResponseEntity<T> save(@RequestBody T entity);

  @PostMapping("/")
  public ResponseEntity<T> save(@RequestBody T entity, @RequestHeader HttpHeaders headers);

  @PostMapping("/")
  public ResponseEntity<T> saveRequest(@RequestBody RequestModel entity, @RequestHeader HttpHeaders headers);

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id);

  @GetMapping("/search/findAllCreated?startDate={startDate}&endDate={endDate}&page={page}&size={size}")
  PagedModel<T> findAllCreated(@PathVariable String startDate, @PathVariable String endDate, @PathVariable int size,
      @PathVariable int page);

  @GetMapping("/search/findAllUpdated?startDate={startDate}&endDate={endDate}&page={page}&size={size}")
  PagedModel<T> findAllUpdated(@PathVariable String startDate, @PathVariable String endDate, @PathVariable int size,
      @PathVariable int page);

  @PatchMapping("/{id}/remoteid")
  ResponseEntity<T> updateRemoteId(@RequestBody T entity, @PathVariable String id, @RequestHeader HttpHeaders headers);

}
