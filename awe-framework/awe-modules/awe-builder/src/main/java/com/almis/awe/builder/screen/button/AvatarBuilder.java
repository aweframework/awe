package com.almis.awe.builder.screen.button;

import com.almis.awe.builder.screen.base.AbstractButtonBuilder;
import com.almis.awe.model.entities.screen.component.button.Avatar;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author pgarcia
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class AvatarBuilder extends AbstractButtonBuilder<AvatarBuilder, Avatar> {

  private String url;

  @Override
  public Avatar build() {
    return build(new Avatar());
  }

  /**
   * Set avatar image
   * @param url Image url
   * @return Builder
   */
  public AvatarBuilder setImage(String url) {
    setUrl(url);
    return this;
  }
}
