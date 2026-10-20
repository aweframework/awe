import React, {useEffect, useRef, useState} from "react";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {TestIds, testHook} from "../utilities/testIds";

/**
 * Current page of the grid paginator, between its arrows, that the user can type to jump to a page: "Page [2] of 87".
 * Unlike the PrimeReact JumpToPageInput, which moves on every keystroke, it applies the typed page on Enter (cancelling
 * the event, so the grid does not save the row) and on blur. A page out of range, or the current one, is not applied
 * and the input shows the current page again.
 */
function AweGridGotoPage({ page, totalPages, rows, onChange, ariaLabel }) {
  const { t } = useTranslation();
  const current = totalPages > 0 ? page + 1 : 0;
  const [value, setValue] = useState(String(current));
  // Page requested and not shown yet (a grid loaded from the server takes a while), so the blur after an Enter does not
  // request it again
  const requested = useRef(null);

  useEffect(() => {
    requested.current = null;
    setValue(String(current));
  }, [current]);

  const goToPage = (typed) => {
    const target = parseInt(typed, 10);
    if (target === requested.current) {
      return;
    }
    if (target >= 1 && target <= totalPages && target !== current) {
      requested.current = target;
      onChange(rows * (target - 1), rows);
    } else {
      setValue(String(current));
    }
  };

  const onKeyDown = (event) => {
    if (event.key === "Enter") {
      event.stopPropagation();
      event.preventDefault();
      goToPage(event.currentTarget.value);
    }
  };

  return <span className="pagination-goto-page">
    <span>{t("SCREEN_TEXT_GRID_PAGE")}</span>
    <input type="number" className="pagination-goto p-inputtext p-component" min={1} max={totalPages}
           value={value} disabled={totalPages <= 1} aria-label={ariaLabel}
           onChange={(event) => setValue(event.target.value)} onKeyDown={onKeyDown}
           onBlur={(event) => goToPage(event.currentTarget.value)} {...testHook(TestIds.gridGotoPage)}/>
    <span>{t("SCREEN_TEXT_GRID_OF")} {totalPages}</span>
  </span>;
}

AweGridGotoPage.propTypes = {
  page: PropTypes.number,
  totalPages: PropTypes.number,
  rows: PropTypes.number,
  onChange: PropTypes.func,
  ariaLabel: PropTypes.string
};

/**
 * Paginator template element that renders the current page input in the JumpToPageInput slot
 * @param {object} options PrimeReact JumpToPageInput options
 * @returns {JSX.Element} Current page input
 */
export const gotoPageTemplate = (options) => {
  const { page, totalPages, rows, onChange } = options.props;
  return <AweGridGotoPage page={page} totalPages={totalPages} rows={rows} onChange={onChange} ariaLabel={options["aria-label"]}/>;
};

export default AweGridGotoPage;
