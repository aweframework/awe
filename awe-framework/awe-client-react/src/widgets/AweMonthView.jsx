import React, { useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import PropTypes from "prop-types";
import { useComponentState } from "../hooks/useComponentState";
import { getIconCode, translateLabel } from "../utilities";
import { buildMonthView, getInitialMonth, readMonthDates, shiftMonth } from "../utilities/monthView";
import "./AweMonthView.less";

/**
 * Month view (#993): a read-only month, the week starting on Monday, with the dates of a grid of the same screen struck
 * through, and the strip of the months of the year with the number of dates of each one; clicking a month of the strip
 * shows it. The widget parameters name the grid (`grid`), its date column (`dateColumn`) and, optionally, the column
 * with the name of each date (`nameColumn`, the tooltip of the day). It reads the current rows of the grid, the ones
 * added or changed and not saved yet included, and draws again when they change: no server call. It opens on the month
 * of the next date from today, else on the month of today, and follows the rows until the user moves to another month.
 * The `legend` parameter replaces the locale key of the legend.
 * @category Widgets
 */
function AweMonthView({ id, parameters, style = "" }) {
  const { t } = useTranslation();
  // Only the setting the component reads: any other change of the settings does not render it again
  const language = useSelector(state => state.settings?.language);
  const { attributes = {} } = useComponentState(id);
  const { grid, dateColumn, nameColumn, legend } = parameters || attributes.parameters || {};
  const { model } = useComponentState(grid);
  const rows = model?.values;
  const [chosen, setChosen] = useState(null);

  const dates = useMemo(() => readMonthDates(rows, { dateColumn, nameColumn }), [rows, dateColumn, nameColumn]);
  const today = new Date();
  const shown = chosen || getInitialMonth(dates, today);
  const view = buildMonthView({ dates, shown, today, language, legend, translate: key => translateLabel(key, t) });
  const titleId = `${id}-title`;

  return <div className={`month-view ${style}`} id={id} role="group" aria-labelledby={titleId}>
    <div className="month-view-head">
      <button type="button" className="month-view-nav" aria-label={view.previous} title={view.previous}
        onClick={() => setChosen(shiftMonth(shown, -1))}>{getIconCode("lucide:chevron-left")}</button>
      <strong className="month-view-title" id={titleId} aria-live="polite">{view.title}</strong>
      <button type="button" className="month-view-nav" aria-label={view.next} title={view.next}
        onClick={() => setChosen(shiftMonth(shown, 1))}>{getIconCode("lucide:chevron-right")}</button>
    </div>
    <table className="month-view-days">
      <thead>
        <tr>{view.weekdays.map(day => <th key={day.long} scope="col" abbr={day.long}>{day.narrow}</th>)}</tr>
      </thead>
      <tbody>
        {view.weeks.map(week => <tr key={week.key}>
          {week.days.map(day => <td key={day.key}
            className={[day.weekend && "is-weekend", day.excluded && "is-excluded", day.today && "is-today"].filter(Boolean).join(" ") || undefined}
            aria-label={day.label} title={day.title} aria-current={day.today ? "date" : undefined}>{day.day}</td>)}
        </tr>)}
      </tbody>
    </table>
    <div className="month-view-legend"><span className="month-view-swatch" aria-hidden="true" />{view.legend}</div>
    <div className="month-view-strip" role="group" aria-label={view.strip}>
      {view.months.map(month => <button key={month.month} type="button" aria-pressed={month.pressed}
        aria-label={month.label} onClick={() => setChosen({ year: shown.year, month: month.month })}>
        {month.short}<span className={month.count ? "month-view-count" : "month-view-count is-empty"}>{month.count}</span>
      </button>)}
    </div>
  </div>;
}

AweMonthView.propTypes = {
  id: PropTypes.string.isRequired,
  parameters: PropTypes.object,
  style: PropTypes.string
};

export default AweMonthView;
