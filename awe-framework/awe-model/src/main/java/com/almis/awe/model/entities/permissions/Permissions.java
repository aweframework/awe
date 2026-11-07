package com.almis.awe.model.entities.permissions;

import com.almis.awe.model.entities.XMLFile;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Permissions Class
 * <p>
 * Used to parse the files Permissions.xml with XStream. Each module may provide one, and AWE combines them into the
 * {@link PermissionCatalog}
 * </p>
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("permissions")
public class Permissions implements XMLFile {

  @Serial
  private static final long serialVersionUID = 4630386146470317385L;

  // Verb list
  @XStreamImplicit(itemFieldName = "verb")
  private List<PermissionVerb> verbList;

  // Resource list
  @XStreamImplicit(itemFieldName = "resource")
  private List<PermissionResource> resourceList;

  /**
   * Returns the declared verbs
   *
   * @return Verb list
   */
  public List<PermissionVerb> getVerbList() {
    return verbList == null ? new ArrayList<>() : verbList;
  }

  /**
   * Returns the declared resources
   *
   * @return Resource list
   */
  public List<PermissionResource> getResourceList() {
    return resourceList == null ? new ArrayList<>() : resourceList;
  }

  @Override
  public List<PermissionResource> getBaseElementList() {
    return getResourceList();
  }
}
