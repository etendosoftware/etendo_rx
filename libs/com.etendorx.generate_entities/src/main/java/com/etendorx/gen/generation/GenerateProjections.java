package com.etendorx.gen.generation;

import com.etendoerp.etendorx.model.ETRXModelProvider;
import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.generation.interfaces.EntityGenerator;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;

public class GenerateProjections implements EntityGenerator {
  private final ArrayList<Projection> projections;

  public GenerateProjections(ArrayList<Projection> projections) {
    this.projections = projections;
  }

  /**
   * Generates the projections
   *
   * @param data
   * @param path
   * @param dataRestEnabled
   * @throws FileNotFoundException
   */
  @Override
  public void generate(Map<String, Object> data, GeneratePaths path, boolean dataRestEnabled)
      throws FileNotFoundException {
    for (Projection projection : projections) {
      if (StringUtils.equals(GenerateEntitiesConstants.PROJECTION_DEFAULT,
          projection.getName()) || projection.getEntities()
          .containsKey(data.get("newClassName").toString())) {
        data.put("anotherEntities",
            ETRXModelProvider.getInstance().getETRXProjectionEntity(projection.getProjection()));
        new GenerateProjectedEntities().generate(path, data, projection, dataRestEnabled);
        if (!projection.getReact()) {
          if (!StringUtils.equals(GenerateEntitiesConstants.PROJECTION_DEFAULT,
              projection.getName())) {
            //new GenerateClientRest().generate(path, data, projection);
          }
        } else {
          new GenerateReactCode().generate(path, data, projection, dataRestEnabled);
        }
      }
    }
  }
}
