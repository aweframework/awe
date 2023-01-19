package com.almis.awe.builder.screen;

import com.almis.awe.builder.screen.base.AbstractElementBuilder;
import com.almis.awe.model.entities.screen.component.Image;
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
public class ImageBuilder extends AbstractElementBuilder<ImageBuilder, Image> {

  private String url;
  private String alternateUrl;

  @Override
  public Image build() {
    return build(new Image());
  }

  @Override
  public Image build(Image image) {
    super.build(image)
      .setUrl(getUrl())
      .setAlternateUrl(getAlternateUrl());

    return image;
  }
}
