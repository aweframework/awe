import React from "react";
import {AweComponent, connectComponent} from "./AweComponent";
import {classNames} from "../utilities/components";
import ReactPlayer from 'react-player';
import {parseBoolean, translateLabel} from "../utilities";
import "./AweVideo.less";

class AweVideo extends AweComponent {

  render() {
    const {t, attributes, address} = this.props;
    const {src, loop, autoplay, poster, controls = "true", style, title} = attributes;
    const classes = classNames(style, "video-player");

    return <ReactPlayer
          className={classes}
          id={address.component}
          url={src}
          controls={parseBoolean(controls)}
          light={poster}
          playing={autoplay}
          loop={loop}
          title={translateLabel(title, t)}/>;
  }
}

export default connectComponent(AweVideo);
