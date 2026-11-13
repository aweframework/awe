package com.almis.awe.model.entities.access;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.Copyable;
import com.almis.awe.model.entities.XMLNode;
import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.List;

/**
 * Profile Class
 *
 * Used to parse the files in profile folder with XStream
 * These files have the specific restrictions of a profile, and the permissions it gets: the permissions of the file
 * minus its exclusions
 *
 * @author Pablo GARCIA - 25/JUN/2010
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("profile")
public class Profile implements Copyable, XMLNode {

  @Serial
  private static final long serialVersionUID = -7990480714029113566L;

  // Restriction List
  @XStreamImplicit
  private List<Restriction> restrictions;

  // Permissions (catalog identifiers or wildcards)
  @XStreamImplicit(itemFieldName = "permission")
  private List<ProfilePermission> permissions;

  // Exclusions: permissions subtracted from the permissions of the file
  @XStreamImplicit(itemFieldName = "exclude")
  private List<ProfilePermission> exclusions;

  @Override
  public Profile copy() {
    return this.toBuilder()
      .restrictions(ListUtil.copyList(getRestrictions()))
      .permissions(ListUtil.copyList(getPermissions()))
      .exclusions(ListUtil.copyList(getExclusions()))
      .build();
  }

  /**
   * Retrieve the permissions of the file, not expanded
   *
   * @return Permission identifiers or wildcards
   */
  @JsonIgnore
  public List<String> getPermissionIds() {
    return getIds(permissions);
  }

  /**
   * Retrieve the exclusions of the file, not expanded
   *
   * @return Permission identifiers or wildcards
   */
  @JsonIgnore
  public List<String> getExclusionIds() {
    return getIds(exclusions);
  }

  private static List<String> getIds(List<ProfilePermission> entries) {
    return entries == null ? List.of() : entries.stream().map(ProfilePermission::getId).toList();
  }

  /**
   * @return the elementKey
   */
  @JsonIgnore
  @Override
  public String getElementKey() {
    return AweConstants.NO_KEY;
  }
}
