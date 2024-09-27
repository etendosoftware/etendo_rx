package com.etendorx.entities.mapper.lib;

/**
 * Interface for setting default values and triggering event handlers on entities.
 */
public interface DefaultValuesHandler {
  /**
   * Sets default values on the given entity.
   *
   * @param entity the entity to set default values on
   * @param isNew   true if the entity is new, false otherwise
   */
  void setDefaultValues(Object entity, boolean isNew);

  /**
   * Triggers event handlers on the given entity.
   *
   * @param entity
   * @param isNew
   */
  void triggerEventHandlers(Object entity, boolean isNew);
}
