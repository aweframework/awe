package com.almis.awe.model.entities.permissions;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * Verb of the permission catalog (read, create, update...), shared by every resource that lists it
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("verb")
public class PermissionVerb implements Serializable {

  @Serial
  private static final long serialVersionUID = -2398172064455136742L;

  // Verb identifier
  @XStreamAsAttribute
  private String id;

  // Verb label (locale key)
  @XStreamAsAttribute
  private String label;
}
