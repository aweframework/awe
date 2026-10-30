package com.almis.awe.builder.screen.base;

import com.almis.awe.builder.enumerates.Align;
import com.almis.awe.builder.enumerates.DataType;
import com.almis.awe.builder.screen.component.ColumnAttributes;
import com.almis.awe.model.entities.screen.component.grid.Column;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * @author dfuentes
 */
@Getter(AccessLevel.PRIVATE)
public abstract class AbstractColumnBuilder<T extends AbstractColumnBuilder, I extends Column> extends AbstractCriteriaBuilder<T, I> {

  private ColumnAttributes columnAttributes;

  public AbstractColumnBuilder() {
    this.columnAttributes = new ColumnAttributes<>(this);
  }

  @Override
  public I build(I column) {
    super.build(column);
    getColumnAttributes().asColumn(column);
    return column;
  }

  /**
   * Set the align
   *
   * @param align Align
   * @return This
   */
  public T setAlign(Align align) {
    getColumnAttributes().setAlign(align);
    return (T) this;
  }

  /**
   * Set the data type
   *
   * @param dataType data type
   * @return This
   */
  public T setDataType(DataType dataType) {
    getColumnAttributes().setDataType(dataType);
    return (T) this;
  }

  /**
   * Set the format options
   *
   * @param formatOptions Format options
   * @return This
   */
  public T setFormatOptions(String formatOptions) {
    getColumnAttributes().setFormatOptions(formatOptions);
    return (T) this;
  }

  /**
   * Set the formatter
   *
   * @param formatter Formatter
   * @return This
   */
  public T setFormatter(String formatter) {
    getColumnAttributes().setFormatter(formatter);
    return (T) this;
  }

  /**
   * Set the sort field
   *
   * @param sortField sort field
   * @return This
   */
  public T setSortField(String sortField) {
    getColumnAttributes().setSortField(sortField);
    return (T) this;
  }

  /**
   * Set the summary type
   *
   * @param summaryType summary type
   * @return This
   */
  public T setSummaryType(String summaryType) {
    getColumnAttributes().setSummaryType(summaryType);
    return (T) this;
  }

  /**
   * Set the column as frozen (blocked)
   *
   * @param frozen property
   * @return This
   */
  public T setFrozen(boolean frozen) {
    getColumnAttributes().setFrozen(frozen);
    return (T) this;
  }

  /**
   * Set the column as hidden
   *
   * @param hidden Column is hidden
   * @return This
   */
  public T setHidden(boolean hidden) {
    getColumnAttributes().setHidden(hidden);
    return (T) this;
  }

  /**
   * Set the the column as page break
   *
   * @param pageBreak column data break pages
   * @return This
   */
  public T setPagebreak(boolean pageBreak) {
    getColumnAttributes().setPageBreak(pageBreak);
    return (T) this;
  }

  /**
   * Set the column as sendable
   *
   * @param sendable Column is sendable
   * @return This
   */
  public T setSendable(boolean sendable) {
    getColumnAttributes().setSendable(sendable);
    return (T) this;
  }

  /**
   * Set the column as sortable
   *
   * @param sortable Column is sortable
   * @return This
   */
  public T setSortable(boolean sortable) {
    getColumnAttributes().setSortable(sortable);
    return (T) this;
  }

  /**
   * Set the column char length
   *
   * @param charLength Column size in characters
   * @return This
   */
  public T setCharLength(Integer charLength) {
    getColumnAttributes().setCharLength(charLength);
    return (T) this;
  }

  /**
   * Set the column width
   *
   * @param width Column width
   * @return This
   */
  public T setWidth(String width) {
    getColumnAttributes().setWidth(width);
    return (T) this;
  }

  /**
   * Set the text of the cells to wrap over several lines
   *
   * @param wrap Cell text wraps
   * @return This
   */
  public T setWrap(boolean wrap) {
    getColumnAttributes().setWrap(wrap);
    return (T) this;
  }

  /**
   * Set the maximum number of lines of a wrapping cell (implies wrap)
   *
   * @param maxLines Maximum number of lines
   * @return This
   */
  public T setMaxLines(Integer maxLines) {
    getColumnAttributes().setMaxLines(maxLines);
    return (T) this;
  }

  /**
   * Set the field shown under the main value of the cells
   *
   * @param secondaryField Secondary field
   * @return This
   */
  public T setSecondaryField(String secondaryField) {
    getColumnAttributes().setSecondaryField(secondaryField);
    return (T) this;
  }

  /**
   * Set to print and export the secondary field in its own column
   *
   * @param exportSecondary Export the secondary field
   * @return This
   */
  public T setExportSecondary(boolean exportSecondary) {
    getColumnAttributes().setExportSecondary(exportSecondary);
    return (T) this;
  }

  /**
   * Set the label of the exported secondary column
   *
   * @param secondaryLabel Secondary column label
   * @return This
   */
  public T setSecondaryLabel(String secondaryLabel) {
    getColumnAttributes().setSecondaryLabel(secondaryLabel);
    return (T) this;
  }

  /**
   * Set the field with the status tone of the cells
   *
   * @param statusField Status field
   * @return This
   */
  public T setStatusField(String statusField) {
    getColumnAttributes().setStatusField(statusField);
    return (T) this;
  }

  /**
   * Set to show the cell value as a list of chips
   *
   * @param chips Show chips
   * @return This
   */
  public T setChips(boolean chips) {
    getColumnAttributes().setChips(chips);
    return (T) this;
  }

  /**
   * Set the separator of the chips items
   *
   * @param chipSeparator Chips separator
   * @return This
   */
  public T setChipSeparator(String chipSeparator) {
    getColumnAttributes().setChipSeparator(chipSeparator);
    return (T) this;
  }

  /**
   * Set the maximum number of chips shown
   *
   * @param maxChips Maximum number of chips
   * @return This
   */
  public T setMaxChips(Integer maxChips) {
    getColumnAttributes().setMaxChips(maxChips);
    return (T) this;
  }

  /**
   * Set the lower end of the scale of the bar of a progress column
   *
   * @param barMin Lower end of the scale
   * @return This
   */
  public T setBarMin(Double barMin) {
    getColumnAttributes().setBarMin(barMin);
    return (T) this;
  }

  /**
   * Set the upper end of the scale of the bar of a progress column
   *
   * @param barMax Upper end of the scale
   * @return This
   */
  public T setBarMax(Double barMax) {
    getColumnAttributes().setBarMax(barMax);
    return (T) this;
  }

  /**
   * Set the field with the second value drawn under the bar
   *
   * @param barSecondField Second value field
   * @return This
   */
  public T setBarSecondField(String barSecondField) {
    getColumnAttributes().setBarSecondField(barSecondField);
    return (T) this;
  }

  /**
   * Set the comma-separated fields with the stacked segments of the bar
   *
   * @param barStackFields Stack fields
   * @return This
   */
  public T setBarStackFields(String barStackFields) {
    getColumnAttributes().setBarStackFields(barStackFields);
    return (T) this;
  }

  /**
   * Set to draw the bar only on the leaf rows of a tree grid
   *
   * @param barLeafOnly Draw the bar only on leaf rows
   * @return This
   */
  public T setBarLeafOnly(boolean barLeafOnly) {
    getColumnAttributes().setBarLeafOnly(barLeafOnly);
    return (T) this;
  }
}
