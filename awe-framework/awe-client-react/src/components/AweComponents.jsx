import AweButton from "./AweButton";
import Tag from "./Tag";
import SubView from "./SubView";
import Empty from "./Empty";
import AweWindow from "./AweWindow";
import AweDialog from "./AweDialog";
import AweMenu from "./AweMenu";
import AweInfoDropdown from "./AweInfoDropdown";
import AweInfoButton from "./AweInfoButton";
import AweInfoCriterion from "./AweInfoCriterion";
import AweTabs from "./AweTabs";
import AweSteps from "./AweSteps";
import AweChart from "./AweChart";
import AweTagList from "./AweTagList";
import AweResizable from "./AweResizable";
import AweAccordion from "./AweAccordion";
import AweWidget from "../widgets";
import AwePivotTable from "./AwePivotTable";
import AweGrid from "./AweGrid";
import AweTreeGrid from "./AweTreeGrid";
import AweAvatar from "./AweAvatar";
import AweImage from "./AweImage";
import AweLink from "./AweLink";
import AweVideo from "./AweVideo";

export const ComponentList = {
  "Button": AweButton,
  "Tag": Tag,
  "Info": AweInfoDropdown,
  "InfoButton": AweInfoButton,
  "InfoCriteria": AweInfoCriterion,
  "Avatar": AweAvatar,
  "Screen": Empty,
  "Include": Empty,
  "View": SubView,
  "Window": AweWindow,
  "Dialog": AweDialog,
  "Grid": AweGrid,
  "TreeGrid": AweTreeGrid,
  "PivotTable": AwePivotTable,
  "Tab": AweTabs,
  "Wizard": AweSteps,
  "TabContainer": Empty,
  "WizardPanel": Empty,
  "Chart": AweChart,
  "MenuContainer": AweMenu,
  "TagList": AweTagList,
  "Resizable": AweResizable,
  "Accordion": AweAccordion,
  "Widget": AweWidget,
  "Image": AweImage,
  "Link": AweLink,
  "Video": AweVideo
};

export const IGNORE_COMPONENT_LIST = ["ButtonAction", "Dependency"];
