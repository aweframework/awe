"use strict";

import React, { useEffect, useState } from "react";
import { PDFObject } from 'react-pdfobject';
import "./AweCarousel.less";
import { Skeleton } from "primereact/skeleton";
import { useSelector } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { useTranslation } from "react-i18next";
import { Carousel } from "primereact/carousel";
import ReactPlayer from "react-player";
import { Image } from "primereact/image";
import { translateLabel } from "../utilities";
import PropTypes from "prop-types";

function AweCarousel({ id, style = "" }) {

  const { t } = useTranslation();
  const { model = { values: [] } } = useComponentState(id);
  const [items, setItems] = useState([]);
  const videoTemplate = (item) => {
    return <ReactPlayer url={item.url} controls />;
  };

  const pdfTemplate = (item) => {
    return <PDFObject url={item.url} height={"100%"} style={{ height: "100%" }} />;
  };

  const imageTemplate = (item) => {
    return <Image src={item.url} preview alt={translateLabel(item.title, t)} />;
  };

  const valueTemplate = (item) => {
    let template;
    switch (item.type) {
      case "video":
        template = videoTemplate(item);
        break;
      case "pdf":
        template = pdfTemplate(item);
        break;
      default:
        template = imageTemplate(item);
        break;
    }
    return <div className={"flex flex-column justify-content-center align-items-center"} style={{ height: "100%", minHeight: "500px" }}>
      {item.title && <h2 className={"p-carousel-item-title"}>{translateLabel(item.title, t)}</h2>}
      {item.description && <div className={"p-carousel-item-description"}>{translateLabel(item.description, t)}</div>}
      {template}
    </div>;
  };

  useEffect(() => {
    setItems(model.values);
  }, [model]);

  if (items?.length) {
    return <div className={`carousel ${style}`}><Carousel value={items} numVisible={1} numScroll={1} itemTemplate={valueTemplate} /></div>;
  }

  // Return skeleton
  return <Skeleton className={`m-2 ${style}`} width="100%" height="100%" />;
}

AweCarousel.propTypes = {
  id: PropTypes.string.isRequired,
  style: PropTypes.string,
};

export default AweCarousel;
