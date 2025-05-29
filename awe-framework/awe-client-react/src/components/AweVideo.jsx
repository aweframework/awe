import React from "react";
import {AweComponent, connectComponent} from "./AweComponent";
import {classNames} from "../utilities/components";
import ReactPlayer from 'react-player';
import {parseBoolean, translateLabel} from "../utilities";

class AweVideo extends AweComponent {

  render() {
    const {t, attributes, address} = this.props;
    const {src, loop, autoplay, poster, controls = "true", style, title} = attributes;
    const classes = classNames(style);

    return <ReactPlayer
          className={classes}
          id={address.component}
          url={src}
          controls={parseBoolean(controls)}
          light={poster}
          playing={autoplay}
          loop={loop}
          width={"100%"}
          height={"100%"}
          title={translateLabel(title, t)}/>;
  }
}

export default connectComponent(AweVideo);
