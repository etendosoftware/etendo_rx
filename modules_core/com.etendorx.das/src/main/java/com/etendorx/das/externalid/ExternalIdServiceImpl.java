package com.etendorx.das.externalid;

import com.etendoerp.etendorx.data.ExternalInstanceMapping;
import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.entities.BaseRXObject;
import com.etendorx.entities.jparepo.ADTableRepository;
import com.etendorx.entities.jparepo.ETRX_Instance_ConnectorRepository;
import com.etendorx.entities.jparepo.ETRX_instance_externalidRepository;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import com.etendorx.utils.auth.key.context.AppContext;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.openbravo.model.ad.datamodel.Table;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * This class is responsible for managing external IDs in the system.
 * It implements the ExternalIdService interface.
 */
@Component
@Log4j2
public class ExternalIdServiceImpl implements ExternalIdService {

  private final ETRX_instance_externalidRepository instanceExternalIdRepository;
  private final ADTableRepository adTableRepository;
  private final ETRX_Instance_ConnectorRepository instanceConnectorRepository;

  private final ThreadLocal<Queue<EntityToStore>> currentEntity = new ThreadLocal<>();
  private final AuditServiceInterceptor auditService;

  @Value("${externalid.fail-on-missing:false}")
  private boolean externalIdRequired;

  /**
   * Constructor for the ExternalIdServiceImpl class.
   * It initializes the instanceExternalIdRepository, instanceConnectorRepository, adTableRepository, and auditService.
   *
   * @param instanceExternalIdRepository the repository for ExternalInstanceMapping objects
   * @param instanceConnectorRepository  the repository for ETRX_Instance_Connector objects
   * @param adTableRepository            the repository for Table objects
   */
  public ExternalIdServiceImpl(ETRX_instance_externalidRepository instanceExternalIdRepository,
      ETRX_Instance_ConnectorRepository instanceConnectorRepository,
      ADTableRepository adTableRepository, AuditServiceInterceptor auditService) {
    this.instanceExternalIdRepository = instanceExternalIdRepository;
    this.instanceConnectorRepository = instanceConnectorRepository;
    this.adTableRepository = adTableRepository;
    this.auditService = auditService;
  }

  @Override
  public void getExternalId(String entityName, String entityId, String externalId) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * This method adds an entity to the currentEntity queue.
   * It checks if the externalId is not null or empty before adding the entity.
   *
   * @param adTableId  the ID of the table
   * @param externalId the external ID
   * @param entity     the entity to add
   */
  @Override
  public void add(String adTableId, String externalId, Object entity) {
    synchronized (this) {
      if (currentEntity.get() == null) {
        currentEntity.set(new ConcurrentLinkedDeque<>());
      }
      if (externalId == null || externalId.isEmpty()) {
        return;
      }
      EntityToStore entityToStore = new EntityToStore();
      entityToStore.setEntity(entity);
      entityToStore.setExternalId(externalId);
      entityToStore.setAdTableId(adTableId);

      currentEntity.get().add(entityToStore);
    }
  }

  /**
   * This method flushes the currentEntity queue.
   * It processes each entity in the queue and stores the external ID.
   */
  @Override
  public void flush() {
    synchronized (this) {
      if (currentEntity.get() == null) {
        currentEntity.remove();
        return;
      }
      String externalSystemId = AppContext.getCurrentUser().getExternalSystemId();
      if (externalSystemId == null || externalSystemId.isEmpty()) {
        log.error("ExternalSystemId is not set");
        currentEntity.remove();
        return;
      }
      EntityToStore entity;
      while (currentEntity.get() != null && (entity = currentEntity.get().poll()) != null) {
        BaseRXObject baseRXObject = null;
        if (entity.getEntity() instanceof BaseRXObject objEntity) {
          baseRXObject = objEntity;
        }
        if (baseRXObject == null || baseRXObject.get_identifier() == null) {
          currentEntity.remove();
          continue;
        }
        storeExternalId(externalSystemId, entity, baseRXObject);
      }
      currentEntity.remove();

    }
  }

  /**
   * This method stores the external ID of an entity.
   * It creates an ExternalInstanceMapping object and sets its properties.
   * If the externalInstanceMapping object is valid, it is saved in the instanceExternalIdRepository.
   *
   * @param externalSystemId the ID of the external system
   * @param entity           the entity to store
   * @param baseRXObject     the BaseRXObject to store
   */
  private void storeExternalId(String externalSystemId, EntityToStore entity,
      BaseRXObject baseRXObject) {
    Table table = adTableRepository.findById(entity.getAdTableId()).orElse(null);
    ExternalInstanceMapping externalInstanceMapping = new ExternalInstanceMapping();
    externalInstanceMapping.setTable(table);
    externalInstanceMapping.setEtendoEntity(baseRXObject.get_identifier());
    externalInstanceMapping.setExternalSystemEntity(entity.getExternalId());
    externalInstanceMapping.setETRXInstanceConnector(
        instanceConnectorRepository.findById(externalSystemId).orElse(null));
    auditService.setAuditValues(externalInstanceMapping, true);
    if (isValidToStore(externalInstanceMapping)) {
      instanceExternalIdRepository.save(externalInstanceMapping);
    } else {
      log.error("ExternalInstanceMapping is not valid: " + externalInstanceMapping);
    }
  }

  /**
   * This method checks if an ExternalInstanceMapping object is valid to store.
   *
   * @param externalInstanceMapping the ExternalInstanceMapping object to check
   * @return true if the ExternalInstanceMapping object is valid to store, false otherwise
   */
  private boolean isValidToStore(ExternalInstanceMapping externalInstanceMapping) {
    return externalInstanceMapping.getExternalSystemEntity() != null && StringUtils.hasLength(
        externalInstanceMapping.getExternalSystemEntity()) && StringUtils.hasLength(
        externalInstanceMapping.getEtendoEntity()) && externalInstanceMapping.getTable() != null;
  }

  /**
   * This method converts an external ID to an internal ID.
   * It creates a Specification to find the ExternalInstanceMapping and returns the internal ID.
   *
   * @param tableId    the ID of the table
   * @param key        the key
   * @param externalId the value
   */
  @Override
  public String convertExternalToInternalId(String tableId, String externalId) {
    String externalSystemId = AppContext.getCurrentUser().getExternalSystemId();
    String internalId = getInternalId(tableId, externalId, externalSystemId);

    if (internalId == null) {
      handleMissingInternalId(externalIdRequired, externalId, externalSystemId, tableId);
      // if the execution reaches here, it means that the externalId is not found in id mappings
      // so we will return the externalId as the internalId
      return externalId;
    }
    return internalId;
  }

  /**
   * This method converts an internal ID to an external ID.
   * It creates a Specification to find the ExternalInstanceMapping and returns the external ID.
   *
   * @param tableId          the ID of the table
   * @param externalId       the value to convert
   * @param externalSystemId the ID of the external system
   */
  private String getInternalId(String tableId, String externalId, String externalSystemId) {
    String internalId = getExternalIdFromDatabase(tableId, externalId, externalSystemId);
    if (internalId == null) {
      internalId = getExternalIdFromDatabase(tableId, externalId, null);
    }
    return internalId;
  }

  /**
   * This method handles the case when an internal ID is missing. It logs an error message.
   *
   * @param externalIdRequired whether the external ID is required
   * @param externalId         the external ID
   * @param externalSystemId   the ID of the external system
   * @param tableId            the ID of the table
   */
  private void handleMissingInternalId(boolean externalIdRequired, String externalId,
      String externalSystemId, String tableId) {
    String message = "ExternalIdService.convertExternalToInternalId: No internal id found for externalSystemId " + externalSystemId + " table " + tableId + " external id: " + externalId;
    log.error(message);
  }

  /**
   * This method gets the external ID from the database.
   * It creates a Specification to find the ExternalInstanceMapping and returns the external ID.
   *
   * @param tableId          the ID of the table
   * @param value            the value
   * @param externalSystemId the ID of the external system
   */
  private String getExternalIdFromDatabase(String tableId, String value, String externalSystemId) {
    Specification<ExternalInstanceMapping> spec = (root, query, criteriaBuilder) -> {
      Predicate predicate = criteriaBuilder.and(criteriaBuilder.equal(root.get("table").get("id"), tableId),
          criteriaBuilder.equal(root.get("externalSystemEntity"), value));
      if(externalSystemId == null) {
        criteriaBuilder.and(predicate, criteriaBuilder.isNull(root.get("eTRXInstanceConnector")));
      } else{
        criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("eTRXInstanceConnector").get("id"), externalSystemId));
      }
      return predicate;
    };
    return instanceExternalIdRepository.findAll(spec)
        .stream()
        .findFirst()
        .map(ExternalInstanceMapping::getEtendoEntity)
        .orElse(null);
  }

  /**
   * This class represents an entity to be stored.
   * It contains properties for adTableId, entityId, externalId, and entity.
   */
  final class EntityToStore {
    String adTableId;
    String entityId;
    String externalId;
    Object entity;

    public String getAdTableId() {
      return adTableId;
    }

    public void setAdTableId(String adTableId) {
      this.adTableId = adTableId;
    }

    public String getEntityId() {
      return entityId;
    }

    public void setEntityId(String entityId) {
      this.entityId = entityId;
    }

    public String getExternalId() {
      return externalId;
    }

    public void setExternalId(String externalId) {
      this.externalId = externalId;
    }

    public Object getEntity() {
      return entity;
    }

    public void setEntity(Object entity) {
      this.entity = entity;
    }
  }

}
