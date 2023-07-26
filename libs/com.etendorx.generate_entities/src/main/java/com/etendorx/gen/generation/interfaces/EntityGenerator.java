package com.etendorx.gen.generation.interfaces;

import java.io.FileNotFoundException;
import java.util.Map;

import com.etendorx.gen.generation.GeneratePaths;

public interface EntityGenerator {

  /**
   * Generates the entity
   * @param data
   * @param path
   * @throws FileNotFoundException
   */
  void generate(Map<String, Object> data, GeneratePaths path) throws FileNotFoundException;

}
