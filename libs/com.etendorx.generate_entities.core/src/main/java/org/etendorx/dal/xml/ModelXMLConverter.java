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

import org.dom4j.Document;
import org.dom4j.Element;
import org.etendorx.base.provider.OBProvider;
import org.etendorx.base.provider.OBSingleton;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates the XML Schema which represents the REST Webservice in and output. The XML Schema is
 * generated on the basis of the in-memory model provided by the {@link ModelProvider}.
 *
 * @author mtaal
 * @see Entity
 * @see Property
 */
// TODO: Support id's with multiple values
public class ModelXMLConverter implements OBSingleton {
  // private static final Logger log =
  // LogManager.getLogger();

  private static ModelXMLConverter instance = new ModelXMLConverter();

  public static ModelXMLConverter getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(ModelXMLConverter.class);
    }
    return instance;
  }

  /**
   * Returns the list of Entities as XML representations.
   *
   * @return the Dom4j document containing the list of Entities
   */
  public Document getEntitiesAsXML() {
    final Document doc = org.etendorx.dal.xml.XMLUtil.getInstance().createDomDocument();
    final Element root = doc.addElement("Types");
    final List<String> entityNames = new ArrayList<String>();
    for (final Entity e : ModelProvider.getInstance().getModel()) {
      entityNames.add(e.getName());
    }
    Collections.sort(entityNames);

    for (final String entityName : entityNames) {
      final Element typeElement = root.addElement("Type");
      typeElement.addAttribute("entityName", entityName);
    }
    return doc;
  }

  /**
   * Generates the XML Schema representation of the in-memory model. This XML Schema represents the
   * in- and output of the REST web-services.
   *
   * @return the Dom4j document containing the XML Schema.
   */
  public Document getSchema() {
    final Document doc = XMLUtil.getInstance().createDomDocument();
    doc.addComment(
        "\n* ***********************************************************************************\n" + "* The contents of this file are subject to the etendorx  Public  License" + "* Version  1.1  (the  \"License\"),  being   the  Mozilla   Public  License" + "* Version 1.1  with a permitted attribution clause; you may not  use this" + "* file except in compliance with the License. You  may  obtain  a copy of" + "* the License at http://www.etendorx.com/legal/license.html " + "* Software distributed under the License  is  distributed  on  an \"AS IS\"" + "* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the" + "* License for the specific  language  governing  rights  and  limitations" + "* under the License. " + "* The Original Code is etendorx ERP. " + "* The Initial Developer of the Original Code is etendorx SLU" + "* All portions are Copyright (C) 2008-2011 etendorx SLU" + "* All Rights Reserved. " + "* Contributor(s):  ______________________________________." + "* ***********************************************************************************\n");
    final Element root = doc.addElement("xs:schema");
    root.addNamespace("xs", "http://www.w3.org/2001/XMLSchema");
    root.addNamespace("ob", "http://www.etendorx.com");
    root.addAttribute("targetNamespace", "http://www.etendorx.com");

    final List<String> entityNames = new ArrayList<String>();
    for (final Entity e : ModelProvider.getInstance().getModel()) {
      entityNames.add(e.getName());
    }
    Collections.sort(entityNames);

    final Element rootElement = root.addElement("xs:element");
    rootElement.addAttribute("name", org.etendorx.dal.xml.XMLConstants.OB_ROOT_ELEMENT);
    final Element complexType = rootElement.addElement("xs:complexType");
    final Element choiceElement = complexType.addElement("xs:choice");
    choiceElement.addAttribute("minOccurs", "0");
    choiceElement.addAttribute("maxOccurs", "unbounded");

    for (final String entityName : entityNames) {
      final Element entityElement = choiceElement.addElement("xs:element");
      entityElement.addAttribute("name", entityName);
      entityElement.addAttribute("type", "ob:" + entityName + "Type");
    }

    for (final String entityName : entityNames) {
      final Element typeElement = root.addElement("xs:complexType");
      typeElement.addAttribute("name", entityName + "Type");

      final Element typeSequenceElement = typeElement.addElement("xs:sequence");
      typeSequenceElement.addAttribute("minOccurs", "0");

      addPropertyElements(typeSequenceElement, ModelProvider.getInstance().getEntity(entityName));

      typeElement.addElement("xs:attribute")
          .addAttribute("name", org.etendorx.dal.xml.XMLConstants.ID_ATTRIBUTE)
          .addAttribute("type", "xs:string")
          .addAttribute("use", "optional");
      typeElement.addElement("xs:attribute")
          .addAttribute("name", org.etendorx.dal.xml.XMLConstants.IDENTIFIER_ATTRIBUTE)
          .addAttribute("type", "xs:string")
          .addAttribute("use", "optional");
      typeElement.addElement("xs:attribute")
          .addAttribute("name", org.etendorx.dal.xml.XMLConstants.REFERENCE_ATTRIBUTE)
          .addAttribute("type", "xs:boolean")
          .addAttribute("use", "optional");
      typeElement.addElement("xs:anyAttribute");
    }

    addSimpleTypeDeclarations(root);
    addReferenceType(root);
    addErrorSchema(root);
    addResultSchema(root);

    return doc;
  }

  protected void addPropertyElements(Element sequence, Entity e) {
    for (final Property p : e.getProperties()) {
      final Element element = sequence.addElement("xs:element");

      element.addAttribute("name", p.getName());

      element.addAttribute("minOccurs", "0");

      if (p.isOneToMany() && p.isChild()) {
        element.addAttribute("minOccurs", "0");
      } else {
        if ((p.isPrimitive() && p.isId()) || !p.isMandatory()) {
          // element.addAttribute("minOccurs", "0");
        } else if (p.isMandatory()) {
          // element.addAttribute("minOccurs", "1");
        }
        element.addAttribute("nillable", Boolean.toString(!p.isMandatory()));
      }

      // set the type
      if (p.isPrimitive()) {
        element.addAttribute("type", ((PrimitiveDomainType) p.getDomainType()).getXMLSchemaType());
      } else if (p.isOneToMany() && p.isChild()) {
        final Element complexChildElement = element.addElement("xs:complexType");
        final Element sequenceChildElement = complexChildElement.addElement("xs:sequence");
        final Element childElement = sequenceChildElement.addElement("xs:element");

        childElement.addAttribute("name", p.getTargetEntity().getName());
        childElement.addAttribute("type", "ob:" + p.getTargetEntity().getName() + "Type");
        childElement.addAttribute("minOccurs", "0");
        childElement.addAttribute("maxOccurs", "unbounded");
      } else {
        element.addAttribute("type", "ob:ReferenceType");
      }
    }
  }

  private void addReferenceType(Element schemaElement) {
    final Element complexElem = schemaElement.addElement("xs:complexType")
        .addAttribute("name", "ReferenceType");
    complexElem.addElement("xs:attribute")
        .addAttribute("name", org.etendorx.dal.xml.XMLConstants.ID_ATTRIBUTE)
        .addAttribute("type", "xs:string")
        .addAttribute("use", "optional");
    complexElem.addElement("xs:attribute")
        .addAttribute("name", org.etendorx.dal.xml.XMLConstants.ENTITYNAME_ATTRIBUTE)
        .addAttribute("type", "xs:string")
        .addAttribute("use", "optional");
    complexElem.addElement("xs:attribute")
        .addAttribute("name", org.etendorx.dal.xml.XMLConstants.IDENTIFIER_ATTRIBUTE)
        .addAttribute("type", "xs:string")
        .addAttribute("use", "optional");
    complexElem.addElement("xs:attribute")
        .addAttribute("name", org.etendorx.dal.xml.XMLConstants.TRANSIENT_ATTRIBUTE)
        .addAttribute("type", "xs:boolean")
        .addAttribute("use", "optional");
    complexElem.addElement("xs:attribute")
        .addAttribute("name", org.etendorx.dal.xml.XMLConstants.INACTIVE_ATTRIBUTE)
        .addAttribute("type", "xs:boolean")
        .addAttribute("use", "optional");
  }

  private void addSimpleTypeDeclarations(Element schema) {
    addSimpleTypeDeclaration(schema, "string");
    addSimpleTypeDeclaration(schema, "boolean");
    addSimpleTypeDeclaration(schema, "dateTime");
    addSimpleTypeDeclaration(schema, "decimal");
    addSimpleTypeDeclaration(schema, "long");
    addSimpleTypeDeclaration(schema, "float");
    addSimpleTypeDeclaration(schema, "base64Binary");
  }

  private void addSimpleTypeDeclaration(Element schema, String typeName) {
    final Element complex = schema.addElement("xs:complexType").addAttribute("name", typeName);
    final Element simple = complex.addElement("xs:simpleContent");
    final Element extension = simple.addElement("xs:extension")
        .addAttribute("base", "xs:" + typeName);
    extension.addElement("xs:attribute")
        .addAttribute("name", org.etendorx.dal.xml.XMLConstants.TRANSIENT_ATTRIBUTE)
        .addAttribute("type", "xs:boolean");
    extension.addElement("xs:attribute")
        .addAttribute("name", org.etendorx.dal.xml.XMLConstants.INACTIVE_ATTRIBUTE)
        .addAttribute("type", "xs:boolean")
        .addAttribute("use", "optional");
  }

  protected void addErrorSchema(Element rootSchema) {
    final Element errElem = rootSchema.addElement("xs:element").addAttribute("name", "error");
    final Element complexType = errElem.addElement("xs:complexType");
    final Element seqElement = complexType.addElement("xs:sequence");
    seqElement.addElement("xs:element")
        .addAttribute("name", "message")
        .addAttribute("type", "xs:string");
  }

  private void addResultSchema(Element schemaElement) {
    final Element resultElem = schemaElement.addElement("xs:element")
        .addAttribute("name", "result");
    final Element complexType = resultElem.addElement("xs:complexType")
        .addAttribute("mixed", "true");
    final Element seqElement = complexType.addElement("xs:sequence");
    addStringElement(seqElement, "msg");
    addStringElement(seqElement, "log");
    addStringElement(seqElement, "warning");
    addGroupTypeElement(seqElement, "inserted");
    addGroupTypeElement(seqElement, "updated");
    addGroupTypeElement(seqElement, "deleted");
    addGroupResultType(schemaElement);
  }

  private void addStringElement(Element sequence, String name) {
    sequence.addElement("xs:element")
        .addAttribute("name", name)
        .addAttribute("minOccurs", "0")
        .addAttribute("type", "xs:string");
  }

  private void addGroupTypeElement(Element sequence, String name) {
    sequence.addElement("xs:element")
        .addAttribute("name", name)
        .addAttribute("minOccurs", "0")
        .addAttribute("type", "ob:ResultEntitiesType");
  }

  private void addGroupResultType(Element schemaElement) {
    final Element complexElem = schemaElement.addElement("xs:complexType")
        .addAttribute("name", "ResultEntitiesType");
    final Element choiceElement = complexElem.addElement("xs:choice");
    choiceElement.addAttribute("minOccurs", "0");
    choiceElement.addAttribute("maxOccurs", "unbounded");

    final List<String> entityNames = new ArrayList<String>();
    for (final Entity e : ModelProvider.getInstance().getModel()) {
      entityNames.add(e.getName());
    }
    Collections.sort(entityNames);
    for (String entityName : entityNames) {
      choiceElement.addElement("xs:element")
          .addAttribute("name", entityName)
          .addAttribute("type", "ob:ResultEntityType");
    }
    addGroupTypeDef(schemaElement);
  }

  private void addGroupTypeDef(Element schemaElement) {
    final Element complexElem = schemaElement.addElement("xs:complexType")
        .addAttribute("name", "ResultEntityType");
    complexElem.addElement("xs:attribute")
        .addAttribute("name", org.etendorx.dal.xml.XMLConstants.ID_ATTRIBUTE)
        .addAttribute("type", "xs:string")
        .addAttribute("use", "optional");
    complexElem.addElement("xs:attribute")
        .addAttribute("name", XMLConstants.IDENTIFIER_ATTRIBUTE)
        .addAttribute("type", "xs:string")
        .addAttribute("use", "optional");
  }

}
