package com.etendorx.das;

import com.etendorx.entities.jparepo.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.openbravo.model.common.plm.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;

@RestController
public class ProductController {
  @Autowired
  private ProductRepository productRepository;

  @Operation(description = "Get products")
  @Parameter(name = "search", description = "RSQL search string", example = "name==exampleProduct;price=gt=20")
  @Parameter(name = "pageable", description = "Pagination", example = "{\n  \"page\": 0,\n  \"size\": 20,\n  \"sort\": [\n    \"id\"\n  ]\n}")
  @GetMapping("/sws/product")
  public Page<Product> product(@RequestParam(name = "search", required = false) String search, Pageable pageable) {
    return productRepository.findAll(toSpecification(search), pageable);
  }

}
