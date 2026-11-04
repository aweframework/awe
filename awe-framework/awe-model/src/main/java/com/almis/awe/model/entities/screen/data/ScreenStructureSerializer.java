package com.almis.awe.model.entities.screen.data;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.TagList;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.action.Dependency;
import com.almis.awe.model.entities.screen.component.action.DependencyAction;
import com.almis.awe.model.entities.screen.component.action.DependencyElement;
import com.almis.awe.model.entities.screen.component.grid.AbstractGrid;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.grid.RowDetail;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.bean.BeanSerializerBase;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serializes the structure of a screen as a skeleton for the clients that build their components from the component
 * controllers (AWE React). Each component definition is already sent once, in its controller, so its node only says
 * what it is and where it is:
 * <ul>
 *   <li>A component (with component tag) keeps its element type, id, treegrid flag and children.</li>
 *   <li>Columns, dependencies and button actions are not sent: they arrive in the controller of their component.</li>
 *   <li>Tag lists and row details keep their whole subtree: the clients use it as a template that is repeated for each
 *   row, and their elements are not components of the screen.</li>
 *   <li>Any other element (screen, tags, includes, views, messages...) keeps its attributes.</li>
 * </ul>
 * Only the JSON changes: the screen object keeps all of its elements.
 */
public class ScreenStructureSerializer extends ValueSerializer<Element> {

  private static final String ELEMENT_LIST = "elementList";

  // Whether the JSON of each element class has children (some elements hide them: widgets send them as parameters)
  private final Map<Class<?>, Boolean> childrenByClass = new ConcurrentHashMap<>();

  @Override
  public void serialize(Element element, JsonGenerator generator, SerializationContext context) throws JacksonException {
    if (element instanceof TagList || element instanceof RowDetail) {
      // Whole node and subtree
      context.findValueSerializer(element.getClass()).serialize(element, generator, context);
      return;
    }

    generator.writeStartObject(element);
    boolean hasChildren;
    if (isRegisteredComponent(element)) {
      generator.writeStringProperty("elementType", element.getElementType());
      if (element.getId() != null) {
        generator.writeStringProperty("id", element.getId());
      }
      if (element instanceof AbstractGrid grid && grid.isTreegrid()) {
        generator.writeBooleanProperty("treegrid", true);
      }
      hasChildren = hasChildrenProperty(element, context);
    } else {
      hasChildren = writeAttributes(element, generator, context);
    }
    if (hasChildren) {
      writeChildren(element, generator, context);
    }
    generator.writeEndObject();
  }

  /**
   * Components with a tag have a controller in the screen components
   */
  private static boolean isRegisteredComponent(Element element) {
    return element instanceof Component component && !AweConstants.NO_TAG.equalsIgnoreCase(component.getComponentTag());
  }

  /**
   * Elements that are not sent: their data arrives in the controller of the component that holds them
   */
  private static boolean isDropped(Element element) {
    return element instanceof Column || element instanceof Dependency || element instanceof DependencyElement
      || element instanceof ButtonAction || element instanceof DependencyAction;
  }

  private boolean hasChildrenProperty(Element element, SerializationContext context) {
    return childrenByClass.computeIfAbsent(element.getClass(), type -> {
      Iterator<PropertyWriter> properties = beanSerializer(element, context).properties();
      while (properties.hasNext()) {
        if (ELEMENT_LIST.equals(properties.next().getName())) {
          return true;
        }
      }
      return false;
    });
  }

  /**
   * Write the attributes of an element, as its own serializer does, except for its children
   *
   * @return The element has children to write
   */
  private boolean writeAttributes(Element element, JsonGenerator generator, SerializationContext context) throws JacksonException {
    boolean hasChildren = false;
    Iterator<PropertyWriter> properties = beanSerializer(element, context).properties();
    try {
      while (properties.hasNext()) {
        PropertyWriter property = properties.next();
        if (ELEMENT_LIST.equals(property.getName())) {
          hasChildren = true;
        } else {
          property.serializeAsProperty(element, generator, context);
        }
      }
    } catch (JacksonException exception) {
      throw exception;
    } catch (Exception exception) {
      throw DatabindException.from(generator, "Cannot serialize the structure of " + element.getElementType(), exception);
    }
    return hasChildren;
  }

  private void writeChildren(Element element, JsonGenerator generator, SerializationContext context) throws JacksonException {
    generator.writeName(ELEMENT_LIST);
    generator.writeStartArray();
    for (Element child : element.<Element>getElementList()) {
      if (!isDropped(child)) {
        serialize(child, generator, context);
      }
    }
    generator.writeEndArray();
  }

  private static BeanSerializerBase beanSerializer(Element element, SerializationContext context) {
    if (context.findValueSerializer(element.getClass()) instanceof BeanSerializerBase bean) {
      return bean;
    }
    throw DatabindException.from(context.getGenerator(), "The element " + element.getElementType() + " is not a bean");
  }
}
