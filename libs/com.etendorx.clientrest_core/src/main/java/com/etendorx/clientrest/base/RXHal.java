package com.etendorx.clientrest.base;

import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class RXHal<T extends ClientRestBase<E>, E extends RepresentationWithId<E>> {
  T service;

  public RXHal(Class<T> service) {
    this.service = SpringContext.getBean(service);
  }

  public static <T extends ClientRestBase<E>, E extends RepresentationWithId<E>> RXHal<T, E> getInstance(
      Class<T> service) {
    return new RXHal<>(service);
  }

  public T getService() {
    return service;
  }

  public E findById(String id) {
    ResponseEntity<E> m = service.findById(id);
    return m.getBody();
  }

  public E findById(String id, String projection) {
    ResponseEntity<E> m = service.findById(id, projection);
    return m.getBody();
  }

  public E save(E model) {
    if (model.getId() == null)
      return service.save(model).getBody();
    else
      return service.update(model, model.getId(), "c").getBody();
  }

  public E save(E model, HttpHeaders headers) {
    if (model.getId() == null)
      return service.save(model, headers).getBody();
    else
      return service.update(model, model.getId(), headers).getBody();
  }

  public E post(E model, HttpHeaders headers) {
    return service.post(model, headers).getBody();
  }

  public PagedModel<E> findAll() {
    return service.findAll();
  }

  public void delete(String id) {
    service.delete(id);
  }

}
