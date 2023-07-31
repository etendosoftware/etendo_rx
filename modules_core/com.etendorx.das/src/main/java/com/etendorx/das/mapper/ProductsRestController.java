package com.etendorx.das.mapper;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.model.common.plm.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.etendorx.entities.jparepo.ProductRepository;

@RestController
@RequestMapping("/MELIProduct")
public class ProductsRestController {
  private final ProductDASRepository productRepository;

  public ProductsRestController(
      @Autowired @Qualifier("com.etendorx.das.mapper.MELIProductDASRepository") ProductDASRepository examRepository) {
    this.productRepository = examRepository;
  }

  @GetMapping
  public Iterable<MELIProductDTO> getProducts() {
    return productRepository.findAll();
  }

  @GetMapping("/{id}")
  public MELIProductDTO getProduct(@PathVariable("id") String id) {
    var product = productRepository.findById(id);
    if (product != null) {
      return product;
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    }
  }

  @PostMapping
  public MELIProductDTO post(MELIProductDTO mELIproduct) {
    return productRepository.save(mELIproduct);
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public MELIProductDTO put(@PathVariable String id, @RequestBody MELIProductDTO mELIproduct) {
    var product = productRepository.findById(id);
    if (product == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    }
    return productRepository.updated(product);
  }
}
