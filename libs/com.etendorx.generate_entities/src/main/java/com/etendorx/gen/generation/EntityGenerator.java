package com.etendorx.gen.generation;

import java.io.FileNotFoundException;
import java.util.Map;

public interface EntityGenerator {
  void generate(Map<String, Object> data, GeneratePaths path) throws FileNotFoundException;
}
