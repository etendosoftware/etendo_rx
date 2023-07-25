package com.etendorx.gen.generation;

import java.io.FileNotFoundException;
import java.util.Map;

import com.etendorx.gen.beans.Projection;

public interface ProjectionGenerator {

  void generate(GeneratePaths paths, Map<String, Object> data,
      Projection projection) throws FileNotFoundException;
}
