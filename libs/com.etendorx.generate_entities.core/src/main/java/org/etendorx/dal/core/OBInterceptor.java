/*
 * Copyright 2022  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.etendorx.dal.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.exception.OBException;
import org.etendorx.base.structure.BaseOBObject;
import org.etendorx.base.structure.Traceable;
import org.etendorx.dal.security.SecurityChecker;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Iterator;

/*
import static org.openbravo.model.ad.system.Client.PROPERTY_ORGANIZATION;
import static org.openbravo.model.common.enterprise.Organization.*;
*/

/**
 * This interceptor is used by Hibernate as a kind of save, update and delete event listener. This
 * event listener catches save or update events to set the client and organization and the
 * updated/created fields. In addition security checks are performed.
 *
 * @author mtaal
 */

public class OBInterceptor extends EmptyInterceptor {
  private static final Logger log = LogManager.getLogger();

  private static final long serialVersionUID = 1L;

  private static ThreadLocal<Boolean> preventUpdateInfoChange = new ThreadLocal<Boolean>();

  private static ThreadLocal<Boolean> disableCheckReferencedOrganizations = new ThreadLocal<Boolean>();

  /**
   * If true is passed and we are in adminMode then the update info (updated/updatedBy) is not
   * updated when an object gets updated.
   *
   * @param value
   */
  public static void setPreventUpdateInfoChange(boolean value) {
    preventUpdateInfoChange.set(value);
  }

  /**
   * If true is passed then the checkReferencedOrganizations check is not done
   *
   * @param value
   */
  public static void setDisableCheckReferencedOrganizations(boolean value) {
    disableCheckReferencedOrganizations.set(value);
  }

  private Interceptor interceptorListener;

  /**
   * Determines if the object is transient (==new and not yet persisted in Hibernate).
   *
   * @param entity
   *     the object for which it is determined if it is new
   * @return true if the object has a null id or has been explicitly set to being new (@see
   * BaseOBObject#isNewOBObject()}, returns false otherwise.
   */
  @Override public Boolean isTransient(Object entity) {
    // special case, if the id is set but it was explicitly
    // set to new then return new
    if (entity instanceof BaseOBObject) {
      final BaseOBObject bob = (BaseOBObject) entity;
      if (bob.getId() != null && bob.isNewOBObject()) {
        return Boolean.TRUE;
      }
    }
    // let hibernate do the rest
    return null;
  }

  /**
   * Performs security checks, is the user present in the {@link org.openbravo.dal.core.OBContext} allowed to delete this
   * entity and is the entity deletable (@see {@link Entity#isDeletable()}.
   *
   * @param entity
   *     the business object which is deleted
   * @param id
   *     the id of the entity
   * @param state
   *     the value of the properties
   * @param propertyNames
   *     the name of the properties of the entity
   * @param types
   *     the hibernate type definition of the properties
   * @see BaseOBObject
   * @see Entity
   * @see Property
   */
  @Override public void onDelete(Object entity, Serializable id, Object[] state,
      String[] propertyNames, Type[] types) {
    SecurityChecker.getInstance().checkDeleteAllowed(entity);
    if (getInterceptorListener() != null) {
      getInterceptorListener().onDelete(entity, id, state, propertyNames, types);
    }
  }

  /**
   * Is called when the entity object is dirty (a value of a property has changed) and the state of
   * the object is about to be flushed to the database using sql update statements. This method
   * updates the audit info fields (updated, updatedBy) and performs security checks.
   *
   * @param entity
   *     the business object which is deleted
   * @param id
   *     the id of the entity
   * @param currentState
   *     the current value of the properties
   * @param previousState
   *     the previous value of the properties, i.e. the values when the entity was loaded from
   *     the database
   * @param propertyNames
   *     the name of the properties of the entity
   * @param types
   *     the hibernate type definition of the properties
   * @return true if the state of the object has changed, this is the case when the entity has audit
   * info because the updated/updatedBy properties are updated here, false is returned in
   * other cases
   */
  @Override public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState,
      Object[] previousState, String[] propertyNames, Type[] types) {
    if (SessionHandler.isCheckingDirtySession()) {
      // onFlushDirty gets invoked on actual flushes but also when checking session dirty, in later
      // case nothing should be done here.
      return false;
    }

    // this can happen when someone has set the id of an object but has not set the
    // new object to true
    if (previousState == null) {
      log.warn(
          "The object " + entity + " is detected as not new (is its id != null?) but it does not have a current state in the database. " + "This can happen when the id is set but not setNewObject(true); has been called.");
      return false;
    }
    // disabled for now, checks are all done when a property is set
    // if (entity instanceof BaseOBObject) {
    // ((BaseOBObject) entity).validate();
    // }

    doEvent(entity, currentState, propertyNames);
    if (disableCheckReferencedOrganizations.get() == null || !disableCheckReferencedOrganizations.get()) {
      checkReferencedOrganizations(entity, currentState, previousState, propertyNames);
    }

    boolean updated = false;
    if (getInterceptorListener() != null) {
      updated = getInterceptorListener().onFlushDirty(entity, id, currentState, previousState,
          propertyNames, types);
    }
/*
    if (entity instanceof Traceable || entity instanceof ClientEnabled
        || entity instanceof OrganizationEnabled) {
      return true;
    }
 */
    return updated;
  }

  /**
   * Is called when a new entity object is persisted in the database. This method sets the audit
   * info fields (created/createdBy/updated/updatedBy) and performs several security checks.
   *
   * @param entity
   *     the business object which is deleted
   * @param id
   *     the id of the entity
   * @param currentState
   *     the current value of the properties
   * @param propertyNames
   *     the name of the properties of the entity
   * @param types
   *     the hibernate type definition of the properties
   * @return true if the state of the object has changed, this is the case when the entity has audit
   * info because the updated/updatedBy properties are updated here, false is returned in
   * other cases
   */
  @Override public boolean onSave(Object entity, Serializable id, Object[] currentState,
      String[] propertyNames, Type[] types) {
    // disabled for now, checks are all done when a property is set
    // if (entity instanceof BaseOBObject) {
    // ((BaseOBObject) entity).validate();
    // }

    doEvent(entity, currentState, propertyNames);

    // also check for new records
    // commented out for now, re-apply in MP9
    // see issue https://issues.openbravo.com/view.php?id=19273
    // if (disableCheckReferencedOrganizations.get() == null
    // || !disableCheckReferencedOrganizations.get()) {
    // checkReferencedOrganizations(entity, currentState, new Object[currentState.length],
    // propertyNames);
    // }

    boolean listenerResult = false;
    if (getInterceptorListener() != null) {
      listenerResult = getInterceptorListener().onSave(entity, id, currentState, propertyNames,
          types);
    }

    // audit info fields
    /*
    if (entity instanceof Traceable || entity instanceof ClientEnabled
        || entity instanceof OrganizationEnabled) {
      return true;
    }

     */
    return listenerResult || false;
  }

  private void checkReferencedOrganizations(Object entity, Object[] currentState,
      Object[] previousState, String[] propertyNames) {
    /*
    if (!(entity instanceof OrganizationEnabled)) {
      return;
    }
    final Organization o1 = ((OrganizationEnabled) entity).getOrganization();

     */
    final OBContext obContext = OBContext.getOBContext();
    final BaseOBObject bob = (BaseOBObject) entity;
    boolean isNew = bob.getId() == null || bob.isNewOBObject();

    // check if the organization of the current object has changed, if so
    // then check all references
    /*
    if (!isNew) {
      for (int i = 0; i < currentState.length; i++) {
        if (propertyNames[i].equals(PROPERTY_ORGANIZATION)) {
          if (currentState[i] != previousState[i]) {
            isNew = true;
            break;
          }
        }
      }
    }
    */
    /*
    for (int i = 0; i < currentState.length; i++) {
      // TODO maybe use equals

      if ((isNew || currentState[i] != previousState[i])
          && !(currentState[i] instanceof Organization)
          && (currentState[i] instanceof BaseOBObject || currentState[i] instanceof HibernateProxy)
          && currentState[i] instanceof OrganizationEnabled) {
        // get the organization from the current state
        final OrganizationEnabled oe = (OrganizationEnabled) currentState[i];
        final Organization o2 = oe.getOrganization();

        // don't do the check for references to attribute set instance
        // using a hard coded name to prevent compile time dependencies
        // attribute set instances could be saved with the wrong org
        // see:
        // https://issues.openbravo.com/view.php?id=19272
        // to not block companies with wrong data prevent the check
        // in this case
        // also see:
        // https://issues.openbravo.com/view.php?id=19273


        final BaseOBObjectDef obObject = (BaseOBObjectDef) currentState[i];
        if (obObject.getEntity().getName().equals("AttributeSetInstance")) {
          continue;
        }

        Property property = bob.getEntity().getProperty(propertyNames[i]);

        boolean skipCrossOrgCheck = (obContext.isInCrossOrgAdministratorMode()
            && property.isAllowedCrossOrgReference()) || property.isAuditInfo()
            || property.isImage();

        if (!skipCrossOrgCheck && !obObject.getEntity().isVirtualEntity()
            && !obContext.getOrganizationStructureProvider(o1.getClient().getId())
                .isInNaturalTree(o1, o2)) {
          throw new OBSecurityException("Entity " + bob.getIdentifier() + " (" + bob.getEntityName()
              + ") with organization " + o1.getIdentifier() + " references an entity "
              + obObject.getIdentifier() + " through its property " + property
              + " but this referenced entity" + " belongs to an organization " + o2.getIdentifier()
              + " which is not part of the natural tree of " + o1.getIdentifier());
        }


      }


    }

     */
  }

  // general event handler does new and update
  protected void doEvent(Object object, Object[] currentState, String[] propertyNames) {
    try {
      // not traceable but still do the security check
      if (!(object instanceof Traceable)) {
        // do a check for writeaccess
        // TODO: the question is if this is the correct
        // location as because of hibernate cascade many things are
        // written.
        SecurityChecker.getInstance().checkWriteAccess(object);
        return;
      }

      final Traceable t = (Traceable) object;
      boolean isNew = /* t.getCreatedBy() == null || */ t.getCreationDate() == null || t.getUpdated() == null /* || t.getUpdatedBy() == null */;
      if (isNew) {
        onNew(t, propertyNames, currentState);
      } else {
        onUpdate(t, propertyNames, currentState);
      }
    } catch (Exception e) {
      while (e instanceof SQLException && e != ((SQLException) e).getNextException()) {
        e = ((SQLException) e).getNextException();
      }
      throw new OBException(e);
    }

    // do a check for writeaccess
    // TODO: the question is if this is the correct
    // location as because of hibernate cascade many things are written.
    SecurityChecker.getInstance().checkWriteAccess(object);
  }

  // set created/createdby and the client and organization
  private void onNew(Traceable traceable, String[] propertyNames, Object[] currentState) {
    // note both the currentState and the Traceable t are modified
    // the object t is modified because right after this call a security
    // check is done (see onSave). This is before hibernate can copy
    // the changes from currentState to the object. This happens slighlty later.
    final OBContext obContext = OBContext.getOBContext();
    /*
    final User currentUser = getCurrentUser();
    if (log.isDebugEnabled()) {
      log.debug("OBEvent for new object " + traceable.getClass().getName() + " user "
          + currentUser.getName());
    }

    Client client = null;
    Organization org = null;
    if (traceable instanceof ClientEnabled || traceable instanceof OrganizationEnabled) {
      // Client and organization in context could have been created in another session, use proxies
      // to set them. Note DalUtil.getId won't help here as objects are already loaded in memory
      client = OBDal.getInstance().getProxy(Client.class, obContext.getCurrentClient().getId());
      org = OBDal.getInstance()
          .getProxy(Organization.class, obContext.getCurrentOrganization().getId());
    }
     */
    /*
    final Date currentDate = new Date();
    for (int i = 0; i < propertyNames.length; i++) {
      // TODO: check why this is here, seems strange
      if ("".equals(propertyNames[i])) {
        currentState[i] = currentDate;
      }

      if (PROPERTY_UPDATED.equals(propertyNames[i]) && traceable.getUpdated() == null) {
        traceable.setUpdated(currentDate);
        currentState[i] = currentDate;
      }
      if (PROPERTY_UPDATEDBY.equals(propertyNames[i]) && traceable.getUpdatedBy() == null) {
        traceable.setUpdatedBy(currentUser);
        currentState[i] = currentUser;
      }
      if (Organization.PROPERTY_CREATIONDATE.equals(propertyNames[i])
          && traceable.getCreationDate() == null) {
        traceable.setCreationDate(currentDate);
        currentState[i] = currentDate;
      }
      if (Organization.PROPERTY_CREATEDBY.equals(propertyNames[i])
          && traceable.getCreatedBy() == null) {
        traceable.setCreatedBy(currentUser);
        currentState[i] = currentUser;
      }
      if (PROPERTY_CLIENT.equals(propertyNames[i]) && currentState[i] == null) {
        ((ClientEnabled) traceable).setClient(client);
        currentState[i] = client;
      }
      if (PROPERTY_ORGANIZATION.equals(propertyNames[i]) && currentState[i] == null) {
        ((OrganizationEnabled) traceable).setOrganization(org);
        currentState[i] = org;
      }
    }
     */
  }

  // Sets the updated/updatedby
  // TODO: can the client/organization change?
  protected void onUpdate(Traceable t, String[] propertyNames, Object[] currentState) {
    if (OBContext.getOBContext()
        .isInAdministratorMode() && preventUpdateInfoChange.get() != null && preventUpdateInfoChange.get()) {
      return;
    }
    /*
    final User currentUser = getCurrentUser();
    if (log.isDebugEnabled()) {
      log.debug("OBEvent for updated object " + t.getClass().getName() + " user "
          + currentUser.getName());
    }
    for (int i = 0; i < propertyNames.length; i++) {
      if (PROPERTY_UPDATED.equals(propertyNames[i])) {
        currentState[i] = new Date();
      }
      if (PROPERTY_UPDATEDBY.equals(propertyNames[i])) {
        currentState[i] = currentUser;
      }
    }
     */
  }

  // after flushing an object is not new anymore
  @Override @SuppressWarnings({ "rawtypes" }) public void postFlush(Iterator entities) {
    while (entities.hasNext()) {
      final BaseOBObject bob = (BaseOBObject) entities.next();
      bob.setNewOBObject(false);
    }
  }

  /**
   * Returns a proxy for current session's user without need of a DB query
   */
/*  private User getCurrentUser() {
    return OBDal.getInstance().getProxy(User.class, OBContext.getOBContext().getUser().getId());
  }
*/
  public Interceptor getInterceptorListener() {
    return interceptorListener;
  }

  public void setInterceptorListener(Interceptor interceptorListener) {
    this.interceptorListener = interceptorListener;
  }

  @Override public void afterTransactionBegin(Transaction tx) {
    if (getInterceptorListener() != null) {
      getInterceptorListener().afterTransactionBegin(tx);
    }
    super.afterTransactionBegin(tx);
  }

  @Override public void afterTransactionCompletion(Transaction tx) {
    if (getInterceptorListener() != null) {
      getInterceptorListener().afterTransactionCompletion(tx);
    }
    super.afterTransactionCompletion(tx);
  }

  @Override public void beforeTransactionCompletion(Transaction tx) {
    if (getInterceptorListener() != null) {
      getInterceptorListener().beforeTransactionCompletion(tx);
    }
    super.beforeTransactionCompletion(tx);
  }

  @SuppressWarnings({ "rawtypes" }) @Override public void preFlush(Iterator entities) {
    if (getInterceptorListener() != null) {
      getInterceptorListener().preFlush(entities);
    }
    super.preFlush(entities);
  }

  // allow Hibernate to determine that the object is a valid entity
  // this is needed to avoid throwing an exception when evicting an already evicted BaseOBObject
  @Override public String getEntityName(Object entity) {
    if (!(entity instanceof BaseOBObject)) {
      return null;
    }
    BaseOBObject bob = (BaseOBObject) entity;
    return bob.getEntityName();
  }

}
