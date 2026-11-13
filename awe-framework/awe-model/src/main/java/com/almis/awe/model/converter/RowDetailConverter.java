package com.almis.awe.model.converter;

import com.almis.awe.model.entities.screen.component.grid.RowDetail;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.ReaderWrapper;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Reads and writes the row-detail tag: its attributes and child elements as any other element, and its text as the
 * template of the detail. A row detail with child elements and only blank text (the indentation) has no template.
 */
public class RowDetailConverter extends ReflectionConverter {

  /**
   * Constructor (called by XStream when it processes the annotations)
   *
   * @param mapper             Mapper
   * @param reflectionProvider Reflection provider
   */
  public RowDetailConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
    super(mapper, reflectionProvider, RowDetail.class);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    super.marshal(source, writer, context);
    RowDetail rowDetail = (RowDetail) source;
    if (!rowDetail.isComponentMode() && rowDetail.getContent() != null) {
      writer.setValue(rowDetail.getContent());
    }
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    // Read the text before the children: the DOM driver returns all the text of the tag, the pull drivers the text
    // before the first child (and their attributes are not available after it, so they are read first)
    HierarchicalStreamReader attributeReader = new AttributeReader(reader);
    String text = reader.getValue();
    RowDetail rowDetail = (RowDetail) super.unmarshal(attributeReader, context);
    boolean blank = text == null || text.isBlank();
    rowDetail.setContent(rowDetail.isComponentMode() && blank ? null : text);
    return rowDetail;
  }

  /**
   * Reader that keeps the attributes of the current tag, read before its text, and delegates everything else
   */
  private static final class AttributeReader extends ReaderWrapper {

    private final List<String> names = new ArrayList<>();
    private final List<String> values = new ArrayList<>();
    private int depth = 0;

    private AttributeReader(HierarchicalStreamReader reader) {
      super(reader);
      for (int index = 0; index < reader.getAttributeCount(); index++) {
        names.add(reader.getAttributeName(index));
        values.add(reader.getAttribute(index));
      }
    }

    @Override
    public void moveDown() {
      depth++;
      super.moveDown();
    }

    @Override
    public void moveUp() {
      depth--;
      super.moveUp();
    }

    @Override
    public String getAttribute(String name) {
      if (depth > 0) {
        return super.getAttribute(name);
      }
      int index = names.indexOf(name);
      return index < 0 ? null : values.get(index);
    }

    @Override
    public String getAttribute(int index) {
      return depth > 0 ? super.getAttribute(index) : values.get(index);
    }

    @Override
    public int getAttributeCount() {
      return depth > 0 ? super.getAttributeCount() : names.size();
    }

    @Override
    public String getAttributeName(int index) {
      return depth > 0 ? super.getAttributeName(index) : names.get(index);
    }

    @Override
    public Iterator<String> getAttributeNames() {
      return depth > 0 ? super.getAttributeNames() : names.iterator();
    }
  }
}
