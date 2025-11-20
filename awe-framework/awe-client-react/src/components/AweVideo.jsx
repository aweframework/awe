import React from "react";
import {classNames} from "../utilities/components";
import ReactPlayer from 'react-player';
import {parseBoolean, translateLabel} from "../utilities";
import "./AweVideo.less";
import {useSelector} from "react-redux";
import {useTranslation} from "react-i18next";

function AweVideo(props) {
  const { id } = props;
  const { address = {}, attributes = {} } = useSelector(state => ({
    address: state.components[id]?.address,
    attributes: state.components[id]?.attributes
  }));
  const { t } = useTranslation();
  const {src, loop, autoplay = false, poster, controls = true, style, title, visible = true} = attributes;
  const classes = classNames(style, "video-player");

  return visible ? <ReactPlayer
        className={classes}
        id={address.component}
        url={src}
        controls={parseBoolean(controls)}
        light={poster}
        playing={parseBoolean(autoplay)}
        loop={loop}
        title={translateLabel(title, t)}/> : <></>;
}

export default AweVideo;
