package com.almis.awe.model.entities.access;

import com.almis.awe.model.entities.Copyable;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Permission or exclusion of a profile file: a permission identifier of the catalog or a wildcard ({@code *},
 * {@code users.*}, {@code *.read})
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Accessors(chain = true)
public class ProfilePermission implements Copyable {

  @Serial
  private static final long serialVersionUID = 3954168427630513970L;

  // Permission identifier or wildcard
  @XStreamAsAttribute
  private String id;

  @Override
  public ProfilePermission copy() {
    return this.toBuilder().build();
  }
}
