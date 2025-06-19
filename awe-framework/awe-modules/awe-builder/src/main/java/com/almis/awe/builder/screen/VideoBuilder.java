package com.almis.awe.builder.screen;

import com.almis.awe.builder.screen.base.AbstractComponentBuilder;
import com.almis.awe.model.entities.screen.component.Video;
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
public class VideoBuilder extends AbstractComponentBuilder<VideoBuilder, Video> {

  private String loop;
  private String preload;
  private String autoplay;
  private String poster;
  private String controls;
  private String src;

  @Override
  public Video build() {
    return build(new Video()
            .setLoop(getLoop())
            .setPreload(getPreload())
            .setAutoplay(getAutoplay())
            .setPoster(getPoster())
            .setControls(getControls())
            .setSrc(getSrc())
    );
  }
}
