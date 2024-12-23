package com.etendorx.das.externalid;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.openbravo.model.ad.datamodel.Table;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.etendoerp.etendorx.data.ExternalInstanceMapping;
import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.entities.BaseRXObject;
import com.etendorx.entities.jparepo.ADTableRepository;
import com.etendorx.entities.jparepo.ETRX_Instance_ConnectorRepository;
import com.etendorx.entities.jparepo.ETRX_instance_externalidRepository;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import com.etendorx.entities.mapper.lib.PostSyncService;
import com.etendorx.utils.auth.key.context.AppContext;

import lombok.extern.log4j.Log4j2;

/**
 * This class is responsible for managing external IDs in the system.
 * It implements the ExternalIdService interface.
 */
@Component
@Log4j2
public class PostSyncServiceImpl implements PostSyncService {
  private final ThreadLocal<Queue<Runnable>> currentEntity = new ThreadLocal<>();

  /**
   * Constructor for the ExternalIdServiceImpl class.
   * It initializes the instanceExternalIdRepository, instanceConnectorRepository, adTableRepository, and auditService.
   *
   */
  public PostSyncServiceImpl() {
      super();
  }

  @Override
  public void add(Runnable entity) {
    synchronized (this) {
      if (currentEntity.get() == null) {
        currentEntity.set(new ConcurrentLinkedDeque<>());
      }
      currentEntity.get().add(entity);
    }
  }

  @Override
  public void flush() {
    synchronized (this) {
      if (currentEntity.get() == null) {
        currentEntity.remove();
        return;
      }
      Runnable entity;
      while (currentEntity.get() != null && (entity = currentEntity.get().poll()) != null) {
        entity.run();
      }
      currentEntity.remove();
    }
  }
}
