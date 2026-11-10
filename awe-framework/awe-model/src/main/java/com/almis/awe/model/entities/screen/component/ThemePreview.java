package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.util.data.ListUtil;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Theme preview: a sample of the AWE 5 theme described by the criteria it names (primary palette and hex, surface
 * palette and hex, mode), updated while they change and before the theme is saved (#949). Both web engines draw
 * it; it never changes the theme of the application.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("theme-preview")
public class ThemePreview extends Component {

  @Serial
  private static final long serialVersionUID = 4127355036948251376L;

  // Criterion with the primary palette (or custom)
  @XStreamAlias("primary")
  @XStreamAsAttribute
  private String primary;

  // Criterion with the primary hex, read when the primary palette is custom
  @XStreamAlias("primary-hex")
  @XStreamAsAttribute
  private String primaryHex;

  // Criterion with the surface palette (auto or custom)
  @XStreamAlias("surface")
  @XStreamAsAttribute
  private String surface;

  // Criterion with the surface hex, read when the surface palette is custom
  @XStreamAlias("surface-hex")
  @XStreamAsAttribute
  private String surfaceHex;

  // Criterion with the mode (light, dark or system)
  @XStreamAlias("mode")
  @XStreamAsAttribute
  private String mode;

  @Override
  public ThemePreview copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Retrieve component tag
   *
   * @return Component tag
   */
  @Override
  public String getComponentTag() {
    return "theme-preview";
  }
}
