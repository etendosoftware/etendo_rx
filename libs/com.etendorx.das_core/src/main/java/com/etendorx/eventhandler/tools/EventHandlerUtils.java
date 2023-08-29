package com.etendorx.eventhandler.tools;

import java.util.Arrays;

import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.AbstractPreDatabaseOperationEvent;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;

public class EventHandlerUtils {

  private EventHandlerUtils() {
  }

  public static int getPropertyIndex(AbstractEvent event, String property) {
    if (event instanceof AbstractPreDatabaseOperationEvent) {
      AbstractPreDatabaseOperationEvent specificEvent = (AbstractPreDatabaseOperationEvent) event;
      return getPropertyIndexByPersister(specificEvent.getPersister(), property);
    } else if (event instanceof SaveOrUpdateEvent) {
      SaveOrUpdateEvent specificEvent = (SaveOrUpdateEvent) event;
      return getPropertyIndexByPersister(specificEvent.getEntry().getPersister(), property);
    } else {
      throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
    }
  }

  private static int getPropertyIndexByPersister(EntityPersister persister, String property) {
    return Arrays.asList(persister.getPropertyNames()).indexOf(property);
  }

  public static Object getPropertyValue(AbstractEvent event, String property) {
    int index = getPropertyIndex(event, property);

    if (event instanceof AbstractPreDatabaseOperationEvent) {
      AbstractPreDatabaseOperationEvent specificEvent = (AbstractPreDatabaseOperationEvent) event;
      return specificEvent.getPersister().getPropertyValue(specificEvent.getEntity(), index);
    } else if (event instanceof SaveOrUpdateEvent) {
      SaveOrUpdateEvent specificEvent = (SaveOrUpdateEvent) event;
      return specificEvent.getEntry().getPersister().getPropertyValue(specificEvent.getEntity(), index);
    } else {
      throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
    }
  }

  public static Object getPropertyValue(AbstractEvent event, String property, Object[] data) {
    int index = getPropertyIndex(event, property);
    return data[index];
  }
}

