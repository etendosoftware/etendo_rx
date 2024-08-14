package com.etendorx.gen.generation.interfaces;

import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.generation.GeneratePaths;

import java.io.FileNotFoundException;
import java.util.Map;

public interface ProjectionGenerator {

  /**
   * Generates the projection
   *
   * @param paths
   * @param data
   * @param projection
   * @param dataRestEnabled
   * @throws FileNotFoundException
   */
  void generate(GeneratePaths paths, Map<String, Object> data, Projection projection,
      boolean dataRestEnabled) throws FileNotFoundException;
}
