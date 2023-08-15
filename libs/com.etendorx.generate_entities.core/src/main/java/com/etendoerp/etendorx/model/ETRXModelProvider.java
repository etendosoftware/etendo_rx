package com.etendoerp.etendorx.model;

import com.etendoerp.etendorx.model.projection.ETRXEntityField;
import com.etendoerp.etendorx.model.projection.ETRXProjection;
import com.etendoerp.etendorx.model.projection.ETRXProjectionEntity;
import com.etendoerp.etendorx.model.repository.ETRXEntitySearch;
import com.etendoerp.etendorx.model.repository.ETRXRepository;
import com.etendoerp.etendorx.model.repository.ETRXSearchParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.provider.OBProvider;
import org.etendorx.base.provider.OBSingleton;
import org.etendorx.dal.service.OBDal;
import org.hibernate.Session;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelObject;
import org.openbravo.base.model.ModelSessionFactoryController;
import org.openbravo.base.model.Module;
import org.openbravo.base.model.Table;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ETRXModelProvider implements OBSingleton {

  private static final String ETENDO_RX_MODULE = "com.etendoerp.etendorx";

  private static final Logger log = LogManager.getLogger();
  private static ETRXModelProvider instance;
  private Session initSession;

  // Etendo RX Models
  private static final List<Class<? extends ModelObject>> ETRX_MODEL_CLASSES = List.of(
    //
    ETRXModule.class,
    // Projections
    ETRXProjection.class,
    ETRXProjectionEntity.class,
    ETRXEntityField.class,
    // Repositories
    ETRXRepository.class,
    ETRXEntitySearch.class,
    ETRXSearchParam.class
  );

  public static synchronized ETRXModelProvider getInstance() {
    // set in a localInstance to prevent threading issues when
    // reseting it in setInstance()
    ETRXModelProvider localInstance = instance;
    if (localInstance == null) {
      localInstance = OBProvider.getInstance().get(ETRXModelProvider.class);
      instance = localInstance;
    }
    return localInstance;
  }

  public void close() {
    log.debug("Closing session and sessionfactory used during Etendo RX model read.");
    initSession.close();
  }

  private <T> List<T> getData(Class<T> tClass, Function<Session, List<T>> retrieve) {
    // Verify Etendo RX module is installed
    if(initSession == null) {
      final ModelSessionFactoryController sessionFactoryController = getETRXSessionFactoryController();
      initSession = sessionFactoryController.getSessionFactory().openSession();
    }
    try {
      return retrieve.apply(initSession);
    } catch (Exception e) {
      log.error("Error loading Etendo RX model.", e);
      throw e;
    }
  }

  public List<ETRXModule> getEtendoRxModules() {
    verifyModule(ETENDO_RX_MODULE);
    log.info("Building Etendo RX projections model");
    return getData(ETRXModule.class, this::retrieveRXModules);
  }

  /**
   * Generates a ModelSessionFactoryController adding the ETRX mapped classes
   *
   * @return ModelSessionFactoryController
   */
  public ModelSessionFactoryController getETRXSessionFactoryController() {
    final ModelSessionFactoryController sessionFactoryController = new ModelSessionFactoryController();
    ETRX_MODEL_CLASSES.forEach(sessionFactoryController::addAdditionalClasses);
    return sessionFactoryController;
  }

  /**
   * Generates a Map of ETRXModules with the json representation.
   *
   * @param etrxModules
   *   List of modules to parse
   *
   * @return Map of json values
   */
  public Map<ETRXModule, String> modulesToJsonMap(List<ETRXModule> etrxModules) {
    log.info("Generating map of RX modules to json.");
    Map<ETRXModule, String> map = new HashMap<>();
    final ModelSessionFactoryController sessionFactoryController = getETRXSessionFactoryController();
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      initSession = sessionFactoryController.getSessionFactory().openSession();
      for (ETRXModule etrxModule : etrxModules) {
        initSession.update(etrxModule);
        map.put(etrxModule, moduleToJson(etrxModule, mapper));
      }
    } catch (Exception e) {
      log.error("Error generating map of RX modules", e);
      throw e;
    } finally {
      initSession.close();
      sessionFactoryController.getSessionFactory().close();
    }
    return map;
  }

  public String moduleToJson(ETRXModule etrxModule, ObjectMapper mapper) {
    try {
      log.info("* Generating json metadata string for: {}", etrxModule.getJavaPackage());
      return mapper.writeValueAsString(etrxModule);
    } catch (Exception e) {
      log.error("Error parsing the module: {}", etrxModule.getJavaPackage());
      throw new RuntimeException(e);
    }
  }

  private void verifyModule(String javaPackage) {
    final ModelSessionFactoryController sessionFactoryController = new ModelSessionFactoryController();
    Session session = sessionFactoryController.getSessionFactory().openSession();
    try {
      if (retrieveModulesByPackage(session, javaPackage).isEmpty()) {
        throw new IllegalArgumentException("The module '" + javaPackage + "' is not installed.");
      }
    } finally {
      session.close();
      sessionFactoryController.getSessionFactory().close();
    }
  }

  private List<Module> retrieveModulesByPackage(Session session, String javaPackage) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<Module> criteria = builder.createQuery(Module.class);
    Root<Module> root = criteria.from(Module.class);
    criteria.select(root);
    criteria.where(builder.equal(root.get("javaPackage"), javaPackage));

    return session.createQuery(criteria).list();
  }

  private List<ETRXModule> retrieveRXModules(Session session) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<ETRXModule> criteria = builder.createQuery(ETRXModule.class);
    Root<ETRXModule> root = criteria.from(ETRXModule.class);
    criteria.select(root);
    criteria.where(builder.equal(root.get("active"), true));
    criteria.where(builder.equal(root.get("rx"), true));
    criteria.orderBy(builder.asc(root.get("seqno")));

    return session.createQuery(criteria).list();
  }

  /**
   * Retrieves a list of model objects of the class passed as parameter.
   *
   * @param session
   *   the session used to query for the objects
   * @param clazz
   *   the class of the model objects to be retrieved
   *
   * @return a list of model objects
   */
  public <T extends Object> List<T> list(Session session, Class<T> clazz) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<T> criteria = builder.createQuery(clazz);
    Root<T> root = criteria.from(clazz);
    criteria.select(root);

    return session.createQuery(criteria).list();
  }

  private List<ETRXProjection> retrieveRXProjection(Session session) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<ETRXProjection> criteria = builder.createQuery(ETRXProjection.class);
    Root<ETRXProjection> root = criteria.from(ETRXProjection.class);
    criteria.select(root);
    criteria.where(builder.equal(root.get("active"), true));
    return session.createQuery(criteria).list();
  }

  public List<ETRXProjection> getETRXProjection() {
    return getData(ETRXProjection.class, this::retrieveRXProjection);
  }

  public List<ETRXRepository> getETRXRepositories(Entity entity) {
    return getData(ETRXRepository.class, session -> retrieveETRXRepositories(session, entity));
  }

  private Table retrieveTable(Session session, String tableId) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<Table> criteria = builder.createQuery(Table.class);
    Root<Table> root = criteria.from(Table.class);
    criteria.select(root);
    criteria.where(builder.equal(root.get("id"), tableId));
    return session.createQuery(criteria).uniqueResult();
  }

  private List<ETRXRepository> retrieveETRXRepositories(Session session, Entity entity) {
    Table table = retrieveTable(session, entity.getTableId());

    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<ETRXRepository> criteria = builder.createQuery(ETRXRepository.class);
    Root<ETRXRepository> root = criteria.from(ETRXRepository.class);
    criteria.select(root);
    criteria.where(builder.equal(root.get("active"), true));
    criteria.where(builder.equal(root.get("table"), table));
    return session.createQuery(criteria).list();
  }

  private List<ETRXRepository> retrieveETRXRepositories(Session session) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<ETRXRepository> criteria = builder.createQuery(ETRXRepository.class);
    Root<ETRXRepository> root = criteria.from(ETRXRepository.class);
    criteria.select(root);
    criteria.where(builder.equal(root.get("active"), true));
    return session.createQuery(criteria).list();
  }

  public List<ETRXRepository> getETRXRepositories() {
    return getData(ETRXRepository.class, this::retrieveETRXRepositories);
  }
}
