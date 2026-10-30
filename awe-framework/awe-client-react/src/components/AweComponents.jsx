import AweButton from "./AweButton";
import Tag from "./Tag";
import SubView from "./SubView";
import Empty from "./Empty";
import AweWindow from "./AweWindow";
import AweDialog from "./AweDialog";
import AweSidePanel from "./AweSidePanel";
import AwePopover from "./AwePopover";
import AweMenu from "./AweMenu";
import AweInfoDropdown from "./AweInfoDropdown";
import AweInfoButton from "./AweInfoButton";
import AweInfoCriterion from "./AweInfoCriterion";
import AweTabs from "./AweTabs";
import AweTabContainer from "./AweTabContainer";
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
import AweMap from "./AweMap";
import AweCard from "./AweCard";
import AweAdvanced from "./AweAdvanced";

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
  "SidePanel": AweSidePanel,
  "Popover": AwePopover,
  "Grid": AweGrid,
  "TreeGrid": AweTreeGrid,
  "PivotTable": AwePivotTable,
  "Tab": AweTabs,
  "Wizard": AweSteps,
  "TabContainer": AweTabContainer,
  "WizardPanel": Empty,
  "Chart": AweChart,
  "MenuContainer": AweMenu,
  "TagList": AweTagList,
  "Resizable": AweResizable,
  "Accordion": AweAccordion,
  "Widget": AweWidget,
  "Image": AweImage,
  "Link": AweLink,
  "Video": AweVideo,
  "GeoMap": AweMap,
  "Card": AweCard,
  "Advanced": AweAdvanced
};

export const IGNORE_COMPONENT_LIST = ["ButtonAction", "Dependency"];
