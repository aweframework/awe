package com.almis.awe.builder.screen;

import com.almis.awe.builder.screen.base.AbstractComponentBuilder;
import com.almis.awe.model.entities.screen.component.Link;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Build a tag list
 *
 * @author dfuentes
 */
@Getter
@Setter
@Accessors(chain = true)
public class LinkBuilder extends AbstractComponentBuilder<LinkBuilder, Link> {

  private String url;

  @Override
  public Link build() {
    return build(new Link()
            .setUrl(url)
    );
  }
}
