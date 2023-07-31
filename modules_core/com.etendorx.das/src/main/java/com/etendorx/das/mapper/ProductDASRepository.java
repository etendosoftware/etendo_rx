package com.etendorx.das.mapper;


public interface ProductDASRepository {

  Iterable<MELIProductDTO> findAll();

  MELIProductDTO findById(String id);

  MELIProductDTO save(MELIProductDTO mELIproduct);

  MELIProductDTO updated(MELIProductDTO product);
}
