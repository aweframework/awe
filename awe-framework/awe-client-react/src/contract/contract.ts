/* tslint:disable */
/* eslint-disable */

export interface AbstractAction extends Element {
    async?: boolean;
    context?: string;
    parameters?: ActionParameters;
    reload?: boolean;
    silent?: boolean;
    target?: string;
    targetAction?: string;
    value?: string;
}

export interface AbstractButton extends AbstractCriteria {
    actions?: ButtonAction[];
    browserAction?: string;
    buttonType?: string;
    cancel?: string;
}

export interface AbstractChart extends Component {
}

export interface AbstractCriteria extends Component {
    areaRows?: number;
    capitalize?: boolean;
    capture?: string;
    checked?: boolean;
    checkEmpty?: boolean;
    checkInitial?: boolean;
    checkTarget?: string;
    dateFormat?: string;
    dateViewMode?: string;
    destination?: string;
    dropZone?: boolean;
    formule?: string;
    group?: string;
    inputType?: string;
    leftLabel?: number;
    message?: string;
    numberFormat?: string;
    optional?: boolean;
    placeholder?: string;
    presets?: string;
    printable?: boolean;
    readonly?: boolean;
    required?: boolean;
    showFutureDates?: boolean;
    showSlider?: boolean;
    showTodayButton?: boolean;
    showWeekends?: boolean;
    specific?: string;
    strict?: boolean;
    timeout?: number;
    unit?: string;
    validation?: string;
    value?: string;
}

export interface AbstractGrid extends AbstractCriteria {
    buttonModel?: Button[];
    checkboxMultiselect?: boolean;
    columnLayout?: GridColumnLayout;
    columnModel?: ScreenColumn[];
    columnsPanel?: string;
    disablePagination?: boolean;
    editable?: boolean;
    emptyAction?: string;
    emptyIcon?: string;
    emptyText?: string;
    emptyTitle?: string;
    enableFilters?: boolean;
    expandColumn?: string;
    groupBy?: string;
    groupSelector?: boolean;
    groupSubtotals?: boolean;
    headerModel?: GroupHeader[];
    iconCollapse?: string;
    iconExpand?: string;
    iconLeaf?: string;
    initialLevel?: number;
    moveRows?: boolean;
    multioperation?: boolean;
    multiselect?: boolean;
    pagerValues?: number[];
    quickFilter?: string;
    rowDetail?: { [index: string]: any };
    rowHeight?: string;
    rowNumbers?: boolean;
    screenId?: string;
    search?: string;
    sendAll?: boolean;
    showToolbar?: boolean;
    showTotals?: boolean;
    sortList?: SortColumn[];
    totalize?: string;
    treegrid?: boolean;
    treeId?: string;
    treeLeaf?: string;
    treeParent?: string;
    validateOnSave?: boolean;
}

export interface AbstractWidget extends Component {
    parameters?: { [index: string]: any };
}

export interface Accordion extends Panelable {
    autocollapse?: boolean;
    itemsSelected?: string[];
    selected?: string;
}

export interface AccordionItem extends Container {
}

export interface ActionParameters {
    label?: string;
    reload?: boolean;
    serverAction?: string;
    target?: string;
    targetAction?: string;
    value?: string;
}

export interface Advanced extends Component {
}

export interface Avatar extends AbstractButton {
    children?: number;
    image?: string;
    showLabel?: boolean;
    text?: string;
}

export interface Button extends AbstractButton {
    selection?: boolean;
    showLoading?: boolean;
}

export interface ButtonAction extends AbstractAction {
}

export interface Card extends Component {
    caption?: string;
    chip?: string;
    chipTone?: string;
    delta?: string;
    deltaUnit?: string;
    description?: string;
    footer?: string;
    goodTrend?: string;
    iconTone?: string;
    numberDecimals?: number;
    progressStyle?: string;
    sparkline?: string;
    sparklineType?: string;
    subtitle?: string;
    target?: string;
    tone?: string;
    trend?: string;
    unit?: string;
    value?: string;
}

export interface Chart extends AbstractChart {
    chartModel?: { [index: string]: any };
    echartsModel?: { [index: string]: any };
    xaxisList?: ChartAxis[];
    yaxisList?: ChartAxis[];
}

export interface ChartAxis extends AbstractChart {
    allowDecimal?: boolean;
    formatterFunction?: string;
    labelFormat?: string;
    labelRotation?: number;
    model?: { [index: string]: any };
    opposite?: boolean;
    tickInterval?: string;
}

export interface ChartConstants {
}

export interface ChartLegend extends AbstractChart {
    align?: string;
    borderWidth?: number;
    enabled?: boolean;
    floating?: boolean;
    layout?: string;
    legendParameterList?: (string | number | boolean | null)[];
    model?: { [index: string]: any };
    verticalAlign?: string;
}

export interface ChartSerie extends AbstractChart {
    color?: string;
    data?: (string | number | boolean | null)[];
    drillDown?: boolean;
    drillDownSerie?: string;
    echarts?: { [index: string]: any };
    model?: { [index: string]: any };
    xAxis?: string;
    xaxis?: string;
    xValue?: string;
    xvalue?: string;
    yAxis?: string;
    yaxis?: string;
    yValue?: string;
    yvalue?: string;
    zValue?: string;
    zvalue?: string;
}

export interface ChartTooltip extends AbstractChart {
    crosshairs?: string;
    dateFormat?: string;
    enabled?: boolean;
    model?: { [index: string]: any };
    numberDecimals?: number;
    pointFormat?: string;
    prefix?: string;
    shared?: boolean;
    suffix?: string;
}

export interface ClientAction extends Copyable {
    address?: ComponentAddress;
    async?: boolean;
    context?: string;
    destination?: string;
    parameters?: { [index: string]: any };
    silent?: boolean;
    target?: string;
    type?: string;
}

export interface Column extends AbstractCriteria {
    actions?: ButtonAction[];
    align?: string;
    avatarField?: string;
    avatarToneField?: string;
    barLeafOnly?: boolean;
    barMax?: number;
    barMin?: number;
    barSecondField?: string;
    barStackFields?: string;
    charlength?: number;
    chips?: boolean;
    chipSeparator?: string;
    dialog?: string;
    exportSecondary?: boolean;
    formatOptions?: string;
    formatter?: string;
    frozen?: boolean;
    hidden?: boolean;
    iconField?: string;
    maxChips?: number;
    maxLines?: number;
    movable?: boolean;
    pagebreak?: boolean;
    position?: string;
    printMode?: string;
    secondaryField?: string;
    secondaryLabel?: string;
    sendable?: boolean;
    sort?: SortColumn;
    sortable?: boolean;
    sortColumn?: string;
    sortField?: string;
    sparklineColor?: string;
    sparklineLastPoint?: boolean;
    sparklineMinMax?: boolean;
    sparklineNegativeColor?: string;
    sparklineType?: string;
    statusField?: string;
    summaryType?: string;
    width?: string;
    wrap?: boolean;
}

export interface Component extends Element {
    autoload?: boolean;
    autorefresh?: number;
    component?: string;
    contextMenu?: any[];
    dataList?: DataList;
    dependencies?: Dependency[];
    icon?: string;
    iconLoading?: string;
    initialLoad?: string;
    loadAll?: boolean;
    max?: number;
    name?: string;
    serverAction?: string;
    size?: string;
    targetAction?: string;
    visible?: boolean;
}

export interface ComponentAddress {
    application?: string;
    column?: string;
    component?: string;
    row?: string;
    screen?: string;
    session?: string;
    view?: string;
}

export interface ComponentModel {
    defaultValues?: (string | number | boolean | null)[];
    page?: number;
    records?: number;
    selected?: (string | number | boolean | null)[];
    total?: number;
    values?: { [index: string]: (string | number | boolean | null) }[];
}

export interface Container extends Component {
}

export interface ContextButton extends AbstractButton {
}

export interface ContextSeparator extends AbstractButton {
    separator?: boolean;
}

export interface Copyable {
}

export interface Criteria extends AbstractCriteria {
}

export interface DataList extends Copyable {
    page?: number;
    records?: number;
    rows?: { [index: string]: (string | number | boolean | null) }[];
    total?: number;
}

export interface Dependency extends Element {
    action?: string;
    actions?: DependencyAction[];
    async?: boolean;
    elements?: DependencyElement[];
    formule?: string;
    initial?: boolean;
    invert?: boolean;
    query?: string;
    serverAction?: string;
    silent?: boolean;
    source?: string;
    target?: string;
    targetAction?: string;
    value?: string;
}

export interface DependencyAction extends AbstractAction {
}

export interface DependencyElement extends Element {
    alias?: string;
    attribute1?: string;
    attribute2?: string;
    cancel?: boolean;
    checkChanges?: boolean;
    column1?: string;
    column2?: string;
    condition?: string;
    event?: string;
    id2?: string;
    name?: string;
    optional?: boolean;
    row1?: string;
    value?: string;
    view1?: string;
    view2?: string;
}

export interface Dialog extends Component {
    accept?: boolean;
    acceptOnClose?: boolean;
    deferred?: boolean;
    onClose?: string;
}

export interface EChartsKeys {
}

export interface EChartsMaps {
}

export interface Element extends XMLNode, Copyable {
    elementList?: Element[];
    elementType?: string;
    expand?: string;
    help?: string;
    helpImage?: string;
    id?: string;
    label?: string;
    style?: string;
    title?: string;
    type?: string;
}

export interface Frame extends Component {
    screen?: string;
    screenVariable?: string;
    scroll?: boolean;
}

export interface GeoMap extends Component {
    centerLatitude?: number;
    centerLongitude?: number;
    latitude?: string;
    longitude?: string;
    markerLabel?: string;
    zoom?: number;
}

export interface Grid extends AbstractGrid {
}

export interface GridColumnLayout {
    hidden?: string[];
    order?: string[];
}

export interface GridSearch extends AbstractCriteria {
}

export interface GroupHeader extends Element {
    columnNames?: string[];
    numberOfColumns?: number;
    position?: string;
    startColumnName?: string;
    titleText?: string;
}

export interface HighchartsParameterTable {
}

export interface HighchartsParameterTranslator {
}

export interface Image extends Component {
    alternateUrl?: string;
    url?: string;
}

export interface Info extends AbstractButton {
    children?: number;
    dropdownStyle?: string;
    text?: string;
}

export interface InfoButton extends AbstractButton {
    infoStyle?: string;
    text?: string;
}

export interface InfoCriteria extends AbstractCriteria {
}

export interface Link extends Component {
    url?: string;
}

export interface MenuContainer extends Component {
    options?: Element[];
}

export interface Message extends Element {
    message?: string;
}

export interface Panelable extends AbstractCriteria {
    orientation?: string;
    tabValues?: { [index: string]: string };
}

export interface ParameterRule {
}

export interface ParameterTarget {
}

export interface ParameterValues {
}

export interface PivotSorter {
    sorterMap?: { [index: string]: unknown };
}

export interface PivotTable extends AbstractGrid {
    aggregationField?: string;
    aggregator?: string;
    cols?: string;
    decimalNumbers?: number;
    decimalSeparator?: string;
    renderer?: string;
    rows?: string;
    sortMethod?: string;
    thousandSeparator?: string;
    totalColumnPlacement?: string;
    totalRowPlacement?: string;
}

export interface Popover extends Dialog {
    anchor?: string;
    maxWidth?: string;
    position?: string;
}

export interface QuickFilter extends AbstractCriteria {
}

export interface Resizable extends Component {
    directions?: string;
}

export interface RowDetail extends Element {
    content?: string;
}

export interface Screen extends Element {
    keepCriteria?: boolean;
    menu?: string;
    noNamespaceSchemaLocation?: string;
    onLoad?: string;
    onUnload?: string;
    screenTemplate?: string;
    target?: string;
}

export interface ScreenColumn extends ScreenComponent {
    autoload?: boolean;
    autorefresh?: number;
    component?: string;
    contextMenu?: any[];
    dataList?: DataList;
    dependencies?: Dependency[];
    elementList?: Element[];
    elementType?: string;
    expand?: string;
    gridId?: string;
    help?: string;
    helpImage?: string;
    icon?: string;
    iconLoading?: string;
    initialLoad?: string;
    label?: string;
    loadAll?: boolean;
    max?: number;
    name?: string;
    serverAction?: string;
    size?: string;
    style?: string;
    targetAction?: string;
    title?: string;
    type?: string;
    visible?: boolean;
}

export interface ScreenComponent {
    controller?: Component;
    id?: string;
    model?: ComponentModel;
}

export interface ScreenData {
    actions?: ClientAction[];
    components?: ScreenComponent[];
    messages?: { [index: string]: Message };
    screen?: { [index: string]: string };
    screenProperties?: { [index: string]: string };
    structure?: Screen;
    template?: string;
}

export interface SeriesFinisher {
}

export interface SeriesParameterRules {
}

export interface SeriesTranslation {
}

export interface SeriesTypeResolver {
}

export interface SidePanel extends Dialog {
    headerAvatarField?: string;
    headerAvatarToneField?: string;
    headerGrid?: string;
    headerSubtitleField?: string;
    headerTags?: SidePanelHeaderTag[];
    headerTitleField?: string;
    maxWidth?: string;
    position?: string;
}

export interface SidePanelHeaderTag extends Element {
    field?: string;
    iconField?: string;
    toneField?: string;
}

export interface SortColumn {
    columnId?: string;
    direction?: string;
}

export interface Tab extends Panelable {
    maximize?: boolean;
}

export interface TabContainer extends Container {
    deferred?: boolean;
}

export interface TagList extends Component {
}

export interface Translation {
}

export interface UnsupportedOptionReporter {
}

export interface Video extends Component {
    autoplay?: string;
    controls?: string;
    loop?: string;
    poster?: string;
    preload?: string;
    src?: string;
}

export interface Widget extends AbstractWidget {
}

export interface WidgetParameter extends AbstractWidget {
    parameterList?: { [index: string]: any }[];
    parameterValue?: any;
}

export interface Window extends Component {
    advancedCriteria?: string[];
    advancedMode?: string;
    advancedOpen?: boolean;
    maximize?: boolean;
    searchCriteria?: string[];
}

export interface Wizard extends Panelable {
}

export interface WizardPanel extends Container {
}

export interface XMLNode {
}

export type DefinitionError = "TEMPLATE_AND_COMPONENTS" | "NOT_A_GRID" | "EDITABLE_GRID" | "DEPENDENCY";

export type EChartsSeriesType = "LINE" | "SPLINE" | "COLUMN" | "BAR" | "AREA" | "AREASPLINE" | "AREARANGE" | "AREASPLINERANGE" | "PIE" | "SCATTER" | "BUBBLE";

export type ParameterScope = "CHART" | "SERIES" | "AXIS" | "TOOLTIP" | "LEGEND";
