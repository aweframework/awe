package com.almis.awe.model.entities.queries;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

/**
 * SqlField Class
 *
 * Used to parse the files Queries.xml and Maintain.xml with XStream
 * Superclass of Field and Computed class
 *
 * @author Pablo GARCIA - 28/JUN/2010
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamInclude({Constant.class, Field.class, Case.class, Operation.class, Over.class})
public abstract class SqlField extends OutputField {

  // Field id (database id)
  @XStreamAlias("id")
  @XStreamAsAttribute
  private String id;

  // Field table
  @XStreamAlias("table")
  @XStreamAsAttribute
  private String table;

  // Function to apply to the field
  @XStreamAlias("function")
  @XStreamAsAttribute
  private String function;

  // Cast to the field
  @XStreamAlias("cast")
  @XStreamAsAttribute
  private String cast;

  // Defined if field is for audit only
  @XStreamAlias("audit")
  @XStreamAsAttribute
  private Boolean audit;

  // Defined if field is a key field
  @XStreamAlias("key")
  @XStreamAsAttribute
  private Boolean key;

	// Defined if the field key retrieves value from identy/auto-incremental
	@XStreamAlias("auto-incremental")
	@XStreamAsAttribute
	private Boolean autoIncremental;

  // Sequence
  @XStreamAlias("sequence")
  @XStreamAsAttribute
  private String sequence;

  // Variable value to set into the id
  @XStreamAlias("variable")
  @XStreamAsAttribute
  private String variable;

  /**
   * Returns if is key
   * @return Is key
   */
  public boolean isKey() {
    return key != null && key;
  }

	/**
	 * Returns if the field retrieve value from identy/auto-incremental column
	 * @return Is auto incremental
	 */
	public boolean isAutoIncremental() {
		return autoIncremental != null && autoIncremental;
	}

  /**
   * Returns if is audit
   * @return Is audit
   */
  public boolean isAudit() {
    return audit == null || audit;
  }

  /**
   * Returns if is not audit
   * @return Is not audit
   */
  public boolean isNotAudit() {
    return audit == null || !audit;
  }

  @Override
  public String getIdentifier() {
    return Optional.ofNullable(getAlias()).orElse(getId());
  }

  /**
   * Apply field modifiers to field string
   * @param field Field string
   * @return Field with function
   */
  public String applyFieldModifiers(String field) {
    String castField = getCast() != null ? "CAST (" + field + " AS " + getCast() + ")" : field;
    return getFunction() != null ? getFunction() + "(" + castField + ")" : castField;
  }
}
