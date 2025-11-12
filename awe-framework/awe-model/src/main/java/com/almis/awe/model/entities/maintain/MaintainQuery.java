package com.almis.awe.model.entities.maintain;

import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.type.MaintainType;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamInclude;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * MaintainQuery Class
 * Used to parse the file Maintain.xml with XStream
 * Parent class for Insert, Update and Delete and Service operations. Contains default attributes and methods
 *
 * @author Ismael SERRANO - 28/JUN/2010
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamInclude({Serve.class, Insert.class, Update.class, Delete.class, RetrieveData.class, Multiple.class, Commit.class, Email.class, Queue.class, IncludeTarget.class})
public abstract class MaintainQuery extends Query {

  @Serial
	private static final long serialVersionUID = 418621393719461416L;

  // Audit table name
  @XStreamAlias("audit")
  @XStreamAsAttribute
  private String auditTable;

  // Launch as batch
  @XStreamAlias("batch")
  @XStreamAsAttribute
  private Boolean batch;

  // Launch as batch
  @XStreamAlias("batch-size")
  @XStreamAsAttribute
  private Integer batchSize;

  // Variable index
  @XStreamOmitField
  private Integer variableIndex;

  // Operation id
  @XStreamOmitField
  private String operationId;

  /**
   * Returns if is batched
   *
   * @return Is batch
   */
  public boolean isBatch() {
    return batch != null && batch;
  }

	/**
	 * Returns if maintain is multiple
	 *
	 * @return Is multiple
	 */
	public boolean isMultiple() {
		return getMultiple() != null && "true".equalsIgnoreCase(getMultiple());
	}

  /**
   * Returns the maintained type
   *
   * @return Maintain type
   */
  public MaintainType getMaintainType() {
    return MaintainType.NONE;
  }

  @Override
  public String toString() {
    return "MaintainQuery{" +
            "type='" + getMaintainType() + '\'' +
            ", auditTable='" + auditTable + '\'' +
            ", batch=" + batch +
            ", batchSize=" + batchSize +
            ", variableIndex=" + variableIndex +
            ", operationId='" + operationId + '\'' +
            '}';
  }
}
