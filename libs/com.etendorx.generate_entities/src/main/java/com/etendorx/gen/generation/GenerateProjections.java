package com.etendorx.gen.generation;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;

import com.etendorx.gen.beans.Projection;

public class GenerateProjections implements EntityGenerator {
  private final ArrayList<Projection> projections;

  public GenerateProjections(ArrayList<Projection> projections) {
    this.projections = projections;
  }

  @Override
  public void generate(Map<String, Object> data, GeneratePaths path) throws FileNotFoundException {
    for (Projection projection : projections) {
      List<Entity> entities = ModelProvider.getInstance().getModel();
      data.put("anotherEntities", new ProjectionsConverter().getEntitiesMap(entities));
      if (StringUtils.equals(GenerateEntitiesConstants.PROJECTION_DEFAULT,
          projection.getName()) || projection.getEntities().containsKey(
          data.get("newClassName").toString())) {
        new GenerateProjectedEntities().generate(path, data, projection);
        if (!projection.getReact()) {
          if (!StringUtils.equals(GenerateEntitiesConstants.PROJECTION_DEFAULT, projection.getName())) {
            new GenerateClientRest().generate(path, data, projection);
          }
        } else {
          new GenerateReactCode().generate(path, data, projection);
        }
      }
    }
  }
}
