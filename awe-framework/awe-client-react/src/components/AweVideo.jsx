import React from "react";
import {classNames} from "../utilities/components";
import ReactPlayer from 'react-player';
import {parseBoolean, translateLabel} from "../utilities";
import "./AweVideo.less";
import {useComponentState} from "../hooks/useComponentState";
import {useTranslation} from "react-i18next";
import PropTypes from "prop-types";

function AweVideo(props) {
  const { id } = props;
  const { attributes = {} } = useComponentState(id);
  const { t } = useTranslation();
  const { src, loop, preload, autoplay = false, poster, controls = true, style, title, visible = true } = attributes;
  const classes = classNames(style, "video-player");

  return visible ? <ReactPlayer
    className={classes}
    id={id}
    url={src}
    controls={parseBoolean(controls)}
    light={poster}
    playing={parseBoolean(autoplay)}
    loop={loop}
    config={{file: {attributes: {preload}}}}
    title={translateLabel(title, t)} /> : <></>;
}

AweVideo.propTypes = {
  id: PropTypes.string,
};

export default AweVideo;
