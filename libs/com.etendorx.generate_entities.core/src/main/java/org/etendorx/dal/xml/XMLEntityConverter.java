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

package org.etendorx.dal.xml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.etendorx.base.exception.OBException;
import org.etendorx.base.provider.OBProvider;
import org.etendorx.base.structure.BaseOBObject;
import org.etendorx.base.util.Check;
import org.etendorx.dal.core.OBContext;
import org.etendorx.dal.security.SecurityChecker;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts a XML string to an objectgraph with objects using Dom4j. This XMLEntityConverter can be
 * used for smaller xml strings (less than 100mb). This XMLEntityConverter can handle OneToMany
 * relations, this in contrast to the {@link StaxXMLEntityConverter}. During the XML parse phase
 * this converter will match XML tags with new or existing (in the database) business objects. The
 * matching logic is implemented in the {@link EntityResolver}.
 * <p>
 * The XMLEntityConverter keeps track of which objects are new, which exist but do not need to be
 * updated or which objects exist but need to be updated.
 * <p>
 * This converter does not update the database directly. However, it changes the properties of
 * existing objects. This means that a commit after calling the process method on the converter can
 * result in database updates by Hibernate.
 *
 * @author mtaal
 * @see Entity
 */

public class XMLEntityConverter extends BaseXMLEntityConverter {
  // This class should translate the

  private static final Logger log = LogManager.getLogger();

  public static XMLEntityConverter newInstance() {
    return OBProvider.getInstance().get(XMLEntityConverter.class);
  }

  private boolean isForDefaultValues;

  /**
   * The main entry point. This method creates a Dom4j Document and then calls
   * {@link #process(Document)}.
   *
   * @param xml
   *   the xml string
   *
   * @return the list of BaseOBObject present in the root of the xml. This list contains the
   *   to-be-updated, to-be-inserted as well as the unchanged business objects
   */
  public List<BaseOBObject> process(String xml) {
    try {
      final Document doc = DocumentHelper.parseText(xml);
      final List<BaseOBObject> result = process(doc);
      return result;
    } catch (final Exception e) {
      throw new EntityXMLException(e);
    }
  }

  /**
   * The main entry point. This method walks through the elements in the root and parses them. The
   * children of a business object (in the xml) are also parsed. Referenced objects are resolved
   * through the {@link EntityResolver}.
   * <p>
   * After a call to this method the to-be-inserted objects can be retrieved through the
   * {@link #getToInsert()} method and the to-be-updated objects through the {@link #getToUpdate()}
   * method.
   *
   * @param doc
   *   the dom4j Document to process
   *
   * @return the list of BaseOBObject present in the root of the xml. This list contains the
   *   to-be-updated, to-be-inserted as well as the unchanged business objects
   */
  public List<BaseOBObject> process(Document doc, boolean filterOrganizations) {
    clear();
    //    getEntityResolver().setClient(getClient());
    //    getEntityResolver().setOrganization(getOrganization());

    // check that the rootelement is the etendorx one
    final Element rootElement = doc.getRootElement();
    if (!rootElement.getName().equals(XMLConstants.OB_ROOT_ELEMENT)) {
      throw new OBException(
        "Root tag of the xml document should be: " + XMLConstants.OB_ROOT_ELEMENT + ", but it is " + rootElement.getName());
    }

    String isForDefaultsStr = rootElement.attributeValue(XMLConstants.DEFAULT_VALUES_DATA);
    isForDefaultValues = isForDefaultsStr != null ? Boolean.parseBoolean(isForDefaultsStr) : false;

    // walk through the elements
    final Set<BaseOBObject> checkDuplicates = new HashSet<BaseOBObject>();
    final List<BaseOBObject> result = new ArrayList<BaseOBObject>();
    for (final Object o : rootElement.elements()) {
      final Element element = (Element) o;
      final BaseOBObject bob = processEntityElement(element.getName(), element, false,
        filterOrganizations);
      // only add it if okay
      if (bob != null && !checkDuplicates.contains(bob)) {
        result.add(bob);
        checkDuplicates.add(bob);
      }
    }
    repairReferences();
    checkDanglingObjects();
    return result;
  }

  public List<BaseOBObject> process(Document doc) {
    boolean filterOrganizations = false;
    return process(doc, filterOrganizations);
  }

  // processes a xml tag which denotes an instance of a business object
  private BaseOBObject processEntityElement(String entityName, Element obElement,
                                            boolean theReferenced, boolean filterOrganizations) {
    // note: referenced is true for both childs and many-to-one references
    // it is passed to the entityresolver to allow searches in other
    // organization

    // note id maybe null for new objects
    final String id = obElement.attributeValue(XMLConstants.ID_ATTRIBUTE);
    if (entityName == null) {
      error("Element " + obElement.getName() + " has no entityname attribute, not processing it");
      return null;
    }
    try {
      log.debug("Converting entity " + entityName);
      final boolean hasReferenceAttribute = obElement.attributeValue(
        XMLConstants.REFERENCE_ATTRIBUTE) != null;

      // resolve the entity, using the id, note that
      // resolve will create a new object if none is found
      BaseOBObject bob = resolve(entityName, id, false, filterOrganizations);

      // should never be null at this point
      Check.isNotNull(bob,
        "The business object " + entityName + " (" + id + ") can not be resolved");

      // warn/error is logged below if the entity is updated
      // update is prevented below
      final boolean writable = OBContext.getOBContext()
        .isInAdministratorMode() || bob.isNewOBObject() || SecurityChecker.getInstance()
        .isWritable(bob);

      // referenced and not new, so already there, don't update
      if (hasReferenceAttribute && !bob.isNewOBObject()) {
        return bob;
      }

      // importing only new rows and not updating existent ones
      if (isForDefaultValues && !bob.isNewOBObject()) {
        return bob;
      }

      // do some checks to determine if this one should be updated
      // a referenced instance should not be updated if it is not new
      // note that embedded children are updated but non-embedded children
      // are not updated!
      // note the !bob.isNewOBObject() is here because at the end
      // after setting the properties (and organization) there is again a writable check
      // which catches this one
      final boolean preventRealUpdate = (!writable && !bob.isNewOBObject()) || (hasReferenceAttribute && !bob.isNewOBObject());

      final Entity entity = ModelProvider.getInstance().getEntity(obElement.getName());
      boolean updated = false;
/*
      // check if there is an organization set
      final Organization originalResolvingOrganization = getEntityResolver().getOrganization();
      if (bob instanceof OrganizationEnabled) {
        Organization objectOrganization = null;
        if (bob.isNewOBObject()) {
          for (final Element childElement : obElement.elements()) {
            if (childElement.getName().equals(Client.PROPERTY_ORGANIZATION)) {
              // found
              final String refId = childElement.attributeValue(XMLConstants.ID_ATTRIBUTE);
              objectOrganization = (Organization) resolve(Organization.ENTITY_NAME, refId, true,
                  filterOrganizations);
            }
          }
        } else {
          objectOrganization = ((OrganizationEnabled) bob).getOrganization();
        }
        // ok use the organization of the object itself to
        // search for referenced objects
        if (objectOrganization != null) {
          getEntityResolver().setOrganization(objectOrganization);
        }
      }
*/
      try {
        // now parse the property elements
        for (final Element childElement : obElement.elements()) {
          final Property p = entity.getProperty(childElement.getName(), false);
          if (p == null) {
            log.warn(
              "Trying to set property " + childElement.getName() + " in entity " + entity.getName() + ". Cannot find property in entity, skipping it.");
            continue;
          }
          log.debug(">>> Exporting property " + p.getName());

          // TODO: make this option controlled
          final boolean isNotImportableProperty = p.isTransient(
            bob) || (p.isAuditInfo() && !isOptionImportAuditInfo()) || p.isInactive() || p.isProxy() || p.getEntity()
            .isVirtualEntity() || p.isComputedColumn();

          if (isNotImportableProperty) {
            log.debug("Property " + p + " is inactive, transient or auditinfo, " + "ignoring it");
            continue;
          }

          // ignore the id properties as they are already set, or should
          // not be set
          if (p.isId()) {
            continue;
          }

          final Object currentValue = bob.get(p.getName());

          // do the primitive values
          if (p.isPrimitive()) {
            Object newValue = ((PrimitiveDomainType) p.getDomainType()).createFromString(
              childElement.getText());

            // correct the value
            newValue = replaceValue(bob, p, newValue);

            log.debug("Primitive property with value " + newValue);

            // only update if changed
            if ((currentValue == null && newValue != null) || (currentValue != null && newValue == null) || (currentValue != null && newValue != null && !currentValue.equals(
              newValue))) {
              log.debug("Value changed setting it");
              if (!preventRealUpdate) {
                bob.set(p.getName(), newValue);
                updated = true;
              }
            }
          } else if (p.isOneToMany() && p.isChild()) {
            // resolve the content of the list, but do not change the list
            for (final Object o : childElement.elements()) {
              final Element listElement = (Element) o;
              processEntityElement(listElement.getName(), listElement, true, filterOrganizations);
            }
          } else {
            Check.isTrue(!p.isOneToMany(), "One to many property not allowed here");

            // determine the referenced entity
            Object newValue;

            // handle null value
            if (childElement.attribute(XMLConstants.ID_ATTRIBUTE) == null) {
              newValue = null;
            } else {
              // get the info and resolve the reference
              final String refId = childElement.attributeValue(XMLConstants.ID_ATTRIBUTE);
              final String refEntityName = p.getTargetEntity().getName();
              newValue = resolve(refEntityName, refId, true, filterOrganizations);
            }
            newValue = replaceValue(bob, p, newValue);
/*
            // never update the org or client through xml!
            final boolean clientUpdate = bob instanceof ClientEnabled
                && p.getName().equals(Organization.PROPERTY_CLIENT);
            final boolean orgUpdate = bob instanceof OrganizationEnabled
                && p.getName().equals(Client.PROPERTY_ORGANIZATION);

            boolean isAllowedUpdate = false;
            if (orgUpdate) {
              isAllowedUpdate = bob.isNewOBObject()
                  && isWritableOrganization((BaseOBObject) newValue);
            }

            if (!isAllowedUpdate && !isOptionClientImport() && currentValue != null
                && (clientUpdate || orgUpdate)) {
              continue;
            }
*/
            final boolean hasChanged = (currentValue == null && newValue != null) || (currentValue != null && newValue == null) || (currentValue != null && newValue != null && !currentValue.equals(
              newValue));
            if (hasChanged) {
              log.debug("Setting value " + newValue);
              if (!preventRealUpdate) {
                bob.set(p.getName(), newValue);
                updated = true;
              }
            }
          }
        }

        // now do this check as the object now has a correct organization
        if (bob.isNewOBObject()) {
          final boolean recheckedWritable = OBContext.getOBContext()
            .isInAdministratorMode() || SecurityChecker.getInstance().isWritable(bob);
          if (!recheckedWritable) {
            error("Object " + entityName + "(" + id + ") is new but not writable");
            return bob;
          }
        }

        // do the unique constraint matching here
        // this check can not be done earlier because
        // earlier no properties are set for a new object
        bob = replaceByUniqueObject(bob);

        // add to the correct list on the basis of different characteristics
        addToInsertOrUpdateLists(id, bob, writable, updated, hasReferenceAttribute,
          preventRealUpdate);

        // do a check that in case of a client/organization import that the
        // client and organization are indeed set
        if (isOptionClientImport()) {
          // checkClientOrganizationSet(bob);
        }

        return bob;
      } finally {
        //getEntityResolver().setOrganization(originalResolvingOrganization);
      }
    } catch (final Exception e) {
      error("Exception when parsing entity " + entityName + " (" + id + "):" + e.getMessage());
      log.error("Exception when parsing entity " + entityName + " (" + id + ")", e);
      return null;
    }
  }

  /*
  private boolean isWritableOrganization(BaseOBObject bob) {
    final String bobId = (String) bob.getId();
    if (bobId == null) {
      return false;
    }

    for (String orgId : OBContext.getOBContext().getWritableOrganizations()) {
      if (orgId.equals(bobId)) {
        return true;
      }
    }
    return false;
  }

   */
}
