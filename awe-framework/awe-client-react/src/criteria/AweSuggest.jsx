import React, { useEffect, useRef, useState } from "react";
import { AutoComplete } from "primereact/autocomplete";
import { classNames } from "../utilities/components";
import "./AweSuggest.less";
import { useTranslation } from "react-i18next";
import { useComponentState } from "../hooks/useComponentState";
import AweCriterion from "./AweCriterion";
import useSuggest from "../hooks/useSuggest";
import { translateLabel } from "../utilities";
import { Skeleton } from "primereact/skeleton";
import PropTypes from "prop-types";
import useComponent from "../hooks/useComponent";
import { Tooltip } from "primereact/tooltip";
import { autoCompletePassThrough } from "../utilities/testPassThrough";

function AweSuggest(props) {
  const { id, style: propsStyle } = props;
  const { t } = useTranslation();
  const { address } = useComponent(id);
  const { model, attributes, validationRules } = useComponentState(id);
  const autocompleteRef = useRef(null);
  const tooltipRef = useRef(null);
  const [suggestions, setSuggestions] = useState([...model?.values || []]);
  const [value, setValue] = useState({});
  const { onChange, onClear, onKeyPress, onSuggest, initialSuggest } = useSuggest(autocompleteRef, setSuggestions, value, setValue, { ...attributes, address });

  // Change model values if updated
  useEffect(() => {
    const fixedValues = (model?.values || [])
      .map(item => ({ ...item, label: item.label || item.value, needsInit: !item?.label }))
      .find(item => item.selected) || {};
    setValue(fixedValues);
  }, [model?.values]);

  // Initial suggest
  useEffect(() => {
    const { checkTarget, targetAction } = attributes || {};
    if ((checkTarget || targetAction) && value?.needsInit) {
      const query = value?.value;
      initialSuggest(query);
    }
  }, [value, attributes]);

  const { placeholder, required, readonly, timeout, size, error, virtualScroll, openPanelOnMount } = attributes || {};
  const classes = classNames("", { [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });
  const isLargeList = suggestions.length > 100;
  const enableVirtualScroll = virtualScroll !== false;

  useEffect(() => {
    if (openPanelOnMount && suggestions.length > 0) {
      autocompleteRef.current?.show?.();
    }
  }, [openPanelOnMount, suggestions.length]);

  // If address is undefined, return skeleton
  if (!address?.component) {
    return <Skeleton width="10rem" height="2rem" style={propsStyle} />;
  }

  let virtualSettings = null;
  if (isLargeList && enableVirtualScroll) {
      virtualSettings = {
          itemSize: 38,
          overscan: 10
      };
  }

  // Tooltip only if the text doesn't fit
  const handleMouseEnter = (e, label) => {
      if (!isLargeList) return;
      const element = e.currentTarget;

      if (element.scrollWidth > element.clientWidth) {
          element.setAttribute('aria-label', label);
          tooltipRef.current?.show(e);
      }
  };

  const handleMouseLeave = (e) => {
      if (!isLargeList) return;
      tooltipRef.current?.hide(e);
  };

  const itemTemplate = (item) => {
      if (isLargeList) {
            return (
                <div
                    className="awe-virtual-item"
                    style={{ cursor: 'pointer' }}
                    data-pr-tooltip={item.label}
                    onMouseEnter={(e) => handleMouseEnter(e, item.label)}
                    onMouseLeave={handleMouseLeave}
                >
                    {item.label}
                </div>
            );
        }
        return <div>{item.label}</div>;
    };

    // If large list, add tooltip because items are truncated
    return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
        {isLargeList && (
            <Tooltip
                ref={tooltipRef}
                target="dummy-disabled-target"
                position="right"
            />
        )}
      <AutoComplete
        field="label"
        ref={autocompleteRef}
        id={id}
        value={value}
        placeholder={translateLabel(placeholder, t)}
        required={required}
        disabled={readonly}
        onChange={onChange}
        onClear={onClear}
        dropdown
        invalid={error}
        onKeyDown={onKeyPress}
        delay={timeout || 300}
        suggestions={suggestions}
        completeMethod={onSuggest}
        className={classes}
        appendTo={document.body}
        forceSelection={true}
        virtualScrollerOptions={virtualSettings}
        itemTemplate={itemTemplate}
        pt={autoCompletePassThrough(id)}
      />
    </AweCriterion>
  );
}

AweSuggest.propTypes = {
  id: PropTypes.string,
  style: PropTypes.oneOfType([PropTypes.object, PropTypes.string])
};

export default AweSuggest;
