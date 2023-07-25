package com.etendorx.gen.generation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openbravo.base.model.Entity;

import com.etendoerp.etendorx.model.projection.ETRXProjection;
import com.etendoerp.etendorx.model.projection.ETRXProjectionEntity;
import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.ProjectionEntity;

public class ProjectionsConverter {
  public List<Projection> convert(GeneratePaths paths, List<ETRXProjection> etrxProjections) {
    List<Projection> projections = new ArrayList<>();
    for (ETRXProjection etrxProjection : etrxProjections) {
      Projection projection = new Projection(
          etrxProjection.getName(),
          etrxProjection.isGrpc(),
          etrxProjection.getModule().isReact()
      );
      projection.setModuleLocation(
          new File(paths.pathEtendoRx + File.separator + etrxProjection.getModule().getRxJavaPackage()));
      convert(projection, etrxProjection.getEntities());
      projections.add(projection);
    }
    return projections;
  }

  public void convert(Projection projection, Set<ETRXProjectionEntity> entities) {
    for (ETRXProjectionEntity entity : entities) {
      projection.getEntities().put(entity.getTable().getName(), convertEntity(entity));
    }
  }

  public void convert(Projection projection, List<Entity> entities) {
    for (Entity entity : entities) {
      projection.getEntities().put(entity.getName(), convertEntity(entity));
    }
  }

  private ProjectionEntity convertEntity(ETRXProjectionEntity entity) {
    return new ProjectionEntity(entity.getTable().getName(), entity.getIdentity());
  }

  private ProjectionEntity convertEntity(Entity entity) {
    var projectionEntity = new ProjectionEntity(entity.getName(), false);
    projectionEntity.setPackageName(entity.getPackageName());
    projectionEntity.setClassName(entity.getClassName());
    return projectionEntity;
  }

  public Map<String, ProjectionEntity> getEntitiesMap(List<Entity> entities) {
    Projection projection = new Projection("default");
    convert(projection, entities);
    return projection.getEntities();
  }
}
