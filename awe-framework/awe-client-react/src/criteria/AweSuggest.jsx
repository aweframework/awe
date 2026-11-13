import React, { useEffect, useMemo, useRef, useState } from "react";
import {criterionPlaceholder, requiredAria} from "../utilities/labels";
import { AutoComplete } from "primereact/autocomplete";
import { classNames } from "../utilities/components";
import "./AweSuggest.less";
import { useTranslation } from "react-i18next";
import { useComponentState } from "../hooks/useComponentState";
import AweCriterion from "./AweCriterion";
import useSuggest from "../hooks/useSuggest";
import { getSuggestionLabel, getVirtualItemSize, groupSuggestions, withTextLabels } from "../utilities/suggest";
import { translateLabel } from "../utilities/labels";
import { CriterionSkeleton } from "../components/AweSkeleton";
import PropTypes from "prop-types";
import useComponent from "../hooks/useComponent";
import { Tooltip } from "primereact/tooltip";
import { autoCompletePassThrough } from "../utilities/testPassThrough";

function AweSuggest(props) {
  const { id } = props;
  const { t } = useTranslation();
  const { address } = useComponent(id);
  const { model, attributes, validationRules } = useComponentState(id);
  const autocompleteRef = useRef(null);
  const tooltipRef = useRef(null);
  const [suggestions, setSuggestions] = useState([...model?.values || []]);
  // PrimeReact needs the labels as text, but the server answers numbers for a suggest over a numeric column
  const textSuggestions = useMemo(() => withTextLabels(suggestions), [suggestions]);
  // Rows with a group (a locale key) are shown under the header of their group
  const groupedSuggestions = useMemo(() => groupSuggestions(textSuggestions, key => translateLabel(key, t)),
    [textSuggestions, t]);
  const [value, setValue] = useState({});
  const { onChange, onClear, onKeyPress, onSuggest, initialSuggest } = useSuggest(autocompleteRef, setSuggestions, value, setValue, { ...attributes, address });

  // Change model values if updated
  useEffect(() => {
    const fixedValues = (model?.values || [])
      .map(item => ({ ...item, label: getSuggestionLabel(item), needsInit: !item?.label && item?.label !== 0 }))
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

  const {required, readonly, timeout, size, error, virtualScroll, openPanelOnMount } = attributes || {};
  const classes = classNames("", { [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });
  const isLargeList = suggestions.length > 100;
  const enableVirtualScroll = virtualScroll !== false;

  useEffect(() => {
    if (openPanelOnMount && suggestions.length > 0) {
      autocompleteRef.current?.show?.();
    }
  }, [openPanelOnMount, suggestions.length]);

  // PrimeReact highlights the first row of the list, which is a header in a grouped list: highlight its first option
  const highlightFirstOption = () => {
    const overlay = autocompleteRef.current?.getOverlay?.();
    const first = overlay?.querySelector('[data-pc-section="item"]');
    if (!groupedSuggestions || !first) {
      return;
    }
    overlay.querySelectorAll('[data-p-highlight="true"]').forEach(row => {
      row.classList.remove("p-highlight");
      row.setAttribute("data-p-highlight", "false");
    });
    first.classList.add("p-highlight");
    first.setAttribute("data-p-highlight", "true");
  };
  useEffect(highlightFirstOption, [groupedSuggestions]);
  // When the list opens, right after PrimeReact highlights its first row
  const highlightOnOpen = {onEntering: highlightFirstOption};

  // If the criterion is not registered yet, return skeleton
  if (!address?.component) {
    return <CriterionSkeleton attributes={attributes} />;
  }

  let virtualSettings = null;
  // A grouped list is not virtual: its headers would break the fixed height of the rows
  if (isLargeList && enableVirtualScroll && !groupedSuggestions) {
      virtualSettings = {
          itemSize: getVirtualItemSize(),
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
        placeholder={criterionPlaceholder(attributes, validationRules, t)}
        required={required}
        {...requiredAria(attributes, validationRules)}
        disabled={readonly}
        onChange={onChange}
        onClear={onClear}
        dropdown
        invalid={error}
        onKeyDown={onKeyPress}
        delay={timeout || 300}
        suggestions={groupedSuggestions || textSuggestions}
        optionGroupLabel={groupedSuggestions ? "label" : undefined}
        optionGroupChildren={groupedSuggestions ? "items" : undefined}
        transitionOptions={highlightOnOpen}
        completeMethod={onSuggest}
        className={classes}
        appendTo={document.body}
        forceSelection={true}
        autoHighlight
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
