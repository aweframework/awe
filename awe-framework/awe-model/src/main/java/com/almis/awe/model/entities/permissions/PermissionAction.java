package com.almis.awe.model.entities.permissions;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * Action of a resource of the permission catalog (users.unlock), for what the verbs do not name
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("action")
public class PermissionAction implements Serializable {

  @Serial
  private static final long serialVersionUID = 7016373453270290418L;

  // Action identifier
  @XStreamAsAttribute
  private String id;

  // Action label (locale key)
  @XStreamAsAttribute
  private String label;
}
