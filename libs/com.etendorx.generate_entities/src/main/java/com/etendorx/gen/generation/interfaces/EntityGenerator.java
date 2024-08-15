package com.etendorx.gen.generation.interfaces;

import com.etendorx.gen.generation.GeneratePaths;

import java.io.FileNotFoundException;
import java.util.Map;

public interface EntityGenerator {

  /**
   * Generates the entity
   *
   * @param data
   * @param path
   * @param dataRestEnabled
   * @throws FileNotFoundException
   */
  void generate(Map<String, Object> data, GeneratePaths path, boolean dataRestEnabled)
      throws FileNotFoundException;

}
