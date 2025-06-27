import ColumnIcon from "./ColumnIcon";
import ColumnImage from "./ColumnImage";
import ColumnText from "./ColumnText";
import ColumnSelect from "./ColumnSelect";
import ColumnStaticCheckbox from "./ColumnStaticCheckbox";
import ColumnCheckbox from "./ColumnCheckbox";
import ColumnNumeric from "./ColumnNumeric";
import ColumnStaticSelect from "./ColumnStaticSelect";
import ColumnStaticNumeric from "./ColumnStaticNumeric";
import ColumnDate from "./ColumnDate";
import ColumnFilteredDate from "./ColumnFilteredDate";
import ColumnTime from "./ColumnTime";
import ColumnSuggest from "./ColumnSuggest";
import ColumnColor from "./ColumnColor";
import ColumnStaticColor from "./ColumnStaticColor";
import ColumnStaticUploader from "./ColumnStaticUploader";
import ColumnUploader from "./ColumnUploader";
import ColumnButton from "./ColumnButton";
import ColumnTextView from "./ColumnTextView";
import ColumnPassword from "./ColumnPassword";
import ColumnStaticPassword from "./ColumnStaticPassword";
import ColumnProgress from "./ColumnProgress";
import ColumnLink from "./ColumnLink";

import "./Columns.less";

export const Static = {
  "icon": ColumnIcon,
  "image": ColumnImage,
  "checkbox": ColumnStaticCheckbox,
  "button": ColumnButton,
  "uploader": ColumnStaticUploader,
  "color": ColumnStaticColor,
  "select": ColumnStaticSelect,
  "numeric": ColumnStaticNumeric,
  "text-view": ColumnTextView,
  "password": ColumnStaticPassword,
  "progress": ColumnProgress,
  "link": ColumnLink
};

export const Editor = {
  "text": ColumnText,
  "password": ColumnPassword,
  "textarea": ColumnText,
  "icon": ColumnIcon,
  "image": ColumnImage,
  "select": ColumnSelect,
  "suggest": ColumnSuggest,
  "numeric": ColumnNumeric,
  "checkbox": ColumnCheckbox,
  "button": ColumnButton,
  "uploader": ColumnUploader,
  "color": ColumnColor,
  "date": ColumnDate,
  "time": ColumnTime,
  "filtered-calendar": ColumnFilteredDate
};
