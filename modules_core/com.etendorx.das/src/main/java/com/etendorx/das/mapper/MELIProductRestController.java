package com.etendorx.das.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.etendorx.entities.entities.BindedRestController;
import com.etendorx.entities.entities.DASRepository;

@RestController
@RequestMapping("/MELIProduct")
public class MELIProductRestController extends BindedRestController<MELIProductDTO> {

  public MELIProductRestController(
      @Autowired @Qualifier("MELI.ProductDASRepositoryDefault") DASRepository<MELIProductDTO> repository) {
    super(repository);
  }

}
