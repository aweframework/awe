/** Rules of the visual screen personalization editor (#792): precedence, validation and the changes to save */

export type ScopeType = "everyone" | "profile" | "user";
export interface Scope { type: ScopeType; id: number | null }
export type ValueType = "BOOLEAN" | "INTEGER" | "ENUM" | "TEXT";
/** A stored rule (a row of AweScrCnf) */
export interface Rule { id: number; user: number | null; profile: number | null; component: string; attribute: string; value: string; active: number }
/** Pending edits of the editor: "component|attribute" -> new value, or null to go back to the inherited value */
export type Pending = Record<string, string | null>;
export interface Change { operation: "INSERT" | "UPDATE" | "DELETE"; id: number | null; screen: string; component: string; attribute: string; value: string; user: number | null; profile: number | null }

// Length of the AweScrCnf value column (the same limit as the server)
const MAX_VALUE_LENGTH = 60;
// Attributes most administrators need, in the order they are shown
const COMMON_ATTRIBUTES = ["visible", "readonly", "required", "label", "placeholder", "value", "defaultValue", "style", "title", "width", "align", "max"];
// A broader scope is overridden by a narrower one
const SCOPE_RANK: Record<ScopeType, number> = {everyone: 0, profile: 1, user: 2};

// The ids come from a computed list column and may be text: compare them as numbers
const scopeOf = (rule: Rule): Scope =>
  rule.user != null ? {type: "user", id: Number(rule.user)} : rule.profile != null ? {type: "profile", id: Number(rule.profile)} : {type: "everyone", id: null};

/**
 * Validate a value as the server does (ScreenConfigurationService.validateRule)
 * @return Error code, or null when the value is valid
 */
export function validateValue(valueType: ValueType | string, value: string, allowedValues: string[]): string | null {
  const text = value ?? "";
  if (text.length > MAX_VALUE_LENGTH) return "VALUE_TOO_LONG";
  switch (valueType) {
    case "BOOLEAN": return /^(true|false)$/i.test(text) ? null : "INVALID_BOOLEAN";
    case "INTEGER": return /^-?\d{1,9}$/.test(text) ? null : "INVALID_INTEGER";
    case "ENUM": return allowedValues.includes(text) ? null : "INVALID_ENUM";
    default: return null;
  }
}

/** The rule that is set exactly at the given scope for an attribute, or null */
export function ownRule(rules: Rule[], attribute: string, scope: Scope): Rule | null {
  return rules.find(rule => rule.active === 1 && rule.attribute === attribute && scopeOf(rule).type === scope.type && scopeOf(rule).id === scope.id) ?? null;
}

/**
 * Effective value of an attribute for a scope and where it comes from: user beats profile, profile beats everyone.
 * shortcut: a user scope does not know the profile of the user, so profile rules are not applied to it (T3 previews it)
 */
export function effectiveValue(rules: Rule[], attribute: string, scope: Scope, xmlValue: string): { value: string; origin: ScopeType | "default"; rule: Rule | null } {
  const winner = rules
    .filter(rule => rule.active === 1 && rule.attribute === attribute)
    .filter(rule => {
      const own = scopeOf(rule);
      return own.type === "everyone" || (own.type === scope.type && own.id === scope.id);
    })
    .sort((a, b) => SCOPE_RANK[scopeOf(b).type] - SCOPE_RANK[scopeOf(a).type])[0];
  return winner ? {value: winner.value, origin: scopeOf(winner).type, rule: winner} : {value: xmlValue, origin: "default", rule: null};
}

// Attributes at the top by type of element; the rest go under Advanced
const COMMON_BY_TYPE: Record<string, string[]> = {
  Criteria: ["visible", "readonly", "required", "label", "placeholder", "defaultValue"],
  Column: ["visible", "label", "width", "charLength", "align", "sortable"],
  Button: ["visible", "label", "style", "title"]
};
// Booleans that are on when nothing sets them (the rest are off)
const TRUE_BY_DEFAULT = ["visible", "printable", "sortable", "movable", "sendable"];

/** Fill the unset booleans with the real default of the attribute, so a switch shows what the screen really does */
export function withDefaults<T extends { attribute: string; valueType: string; value: string }>(list: T[]): T[] {
  return list.map(item => item.valueType === "BOOLEAN" && item.value === "" ? {...item, value: String(TRUE_BY_DEFAULT.includes(item.attribute))} : item);
}

/** Split the attributes into the common ones of the element type (in their usual order) and the advanced ones (alphabetical) */
export function orderAttributes<T extends { attribute: string }>(list: T[], elementType?: string): { common: T[]; advanced: T[] } {
  const names = (elementType && COMMON_BY_TYPE[elementType]) || COMMON_ATTRIBUTES;
  const common = names.map(name => list.find(item => item.attribute === name)).filter((item): item is T => !!item);
  const advanced = list.filter(item => !names.includes(item.attribute)).sort((a, b) => a.attribute.localeCompare(b.attribute));
  return {common, advanced};
}

/** Changes to save: the pending edits compared with the rules that already exist at the scope */
export function buildChanges(rules: Rule[], pending: Pending, scope: Scope, screen: string): Change[] {
  return Object.entries(pending).flatMap<Change>(([key, value]) => {
    const [component, attribute] = key.split("|");
    const current = rules.find(rule => rule.active === 1 && rule.component === component && rule.attribute === attribute
      && scopeOf(rule).type === scope.type && scopeOf(rule).id === scope.id) ?? null;
    const base = {screen, component, attribute, user: scope.type === "user" ? scope.id : null, profile: scope.type === "profile" ? scope.id : null};
    if (value === null) {
      return current ? [{...base, operation: "DELETE" as const, id: current.id, value: current.value}] : [];
    }
    if (current) {
      return current.value === value ? [] : [{...base, operation: "UPDATE" as const, id: current.id, value}];
    }
    return [{...base, operation: "INSERT" as const, id: null, value}];
  });
}

/** Parameters of the updateScreenConfiguration maintain (the lists of the screen configuration grid) */
export function toMaintainRequest(changes: Change[]): Record<string, unknown> {
  return {
    "GrdScrCnf-RowTyp": changes.map(change => change.operation),
    IdeAweScrCnf: changes.map(change => change.id),
    IdeOpe: changes.map(change => change.user),
    IdePro: changes.map(change => change.profile),
    Scr: changes.map(change => change.screen),
    Nam: changes.map(change => change.component),
    Atr: changes.map(change => change.attribute),
    Val: changes.map(change => change.value),
    Act: changes.map(() => 1)
  };
}

/** Value of an attribute for a scope with the pending edits applied, and where it comes from (a reset pending edit shows the inherited value) */
export function resolveValue(rules: Rule[], pending: Pending, component: string, attribute: string, scope: Scope, xmlValue: string):
  { value: string; origin: ScopeType | "default" } {
  const edit = pending[`${component}|${attribute}`];
  if (typeof edit === "string") return {value: edit, origin: scope.type};
  const mine = rules.filter(rule => rule.component === component);
  const own = ownRule(mine, attribute, scope);
  const {value, origin} = effectiveValue(edit === null ? mine.filter(rule => rule !== own) : mine, attribute, scope, xmlValue);
  return {value, origin};
}

/** Translation key of the label of an attribute (the screen configuration attribute enumerated) */
export const attributeLabelKey = (attribute: string): string => `ENUM_SCR_ATR_${attribute.replace(/([a-z])([A-Z])/g, "$1_$2").toUpperCase()}`;

/** Number of pending values that the server would reject (a reset, null, is always valid) */
export function countInvalid(pending: Pending, meta: Record<string, { valueType: string; allowedValues: string[] }>): number {
  return Object.entries(pending).filter(([key, value]) => value !== null && meta[key] && validateValue(meta[key].valueType, value, meta[key].allowedValues)).length;
}

// A grid column has no visible attribute: its real one is hidden, so the editor shows Visible as the inverse of hidden
const invertBoolean = (value: string): string => (/^true$/i.test(value) ? "false" : /^false$/i.test(value) ? "true" : value);

/** Attributes of a component for the editor: a column's hidden is shown as Visible (inverted value) */
/** A column offers hidden and not visible: only a column shows Visible in place of hidden and stores it inverted */
export function isColumnCatalog(attributes: { attribute: string }[]): boolean {
  return !attributes.some(item => item.attribute === "visible") && attributes.some(item => item.attribute === "hidden");
}

export function columnVisibility<T extends { attribute: string; value: string }>(attributes: T[]): T[] {
  if (!isColumnCatalog(attributes)) return attributes;
  return attributes.map(item => item.attribute === "hidden" ? {...item, attribute: "visible", value: invertBoolean(item.value)} : item);
}

/** Rules as the editor reads them: the hidden rules (of the given components, or all) become inverted visible rules */
export function visibleRules(rules: Rule[], columns?: Set<string>): Rule[] {
  return rules.map(rule => rule.attribute === "hidden" && (!columns || columns.has(rule.component))
    ? {...rule, attribute: "visible", value: invertBoolean(rule.value)} : rule);
}

/** Pending edits as they are stored: the Visible edit of a column becomes the inverted hidden value */
export function toStoragePending(pending: Pending, columns: Set<string>): Pending {
  return Object.fromEntries(Object.entries(pending).map(([key, value]) => {
    const [component, attribute] = key.split("|");
    return attribute === "visible" && columns.has(component) ? [`${component}|hidden`, value === null ? null : invertBoolean(value)] : [key, value];
  }));
}

// Width of a hidden column drawn in the inspect canvas
const REVEALED_COLUMN_WIDTH = 80;

/**
 * Column model for the inspect canvas: the hidden columns are drawn (narrow) so they can be selected. Nothing stored changes.
 * @return The model (the same one when no column is hidden) and the names of the columns that are really hidden
 */
export function revealHiddenColumns<C extends { name: string; hidden?: boolean }>(columnModel: C[]): { columnModel: C[]; hidden: string[] } {
  const hidden = columnModel.filter(column => column.hidden).map(column => column.name);
  if (hidden.length === 0) return {columnModel, hidden};
  return {columnModel: columnModel.map(column => column.hidden ? {...column, hidden: false, width: REVEALED_COLUMN_WIDTH, charlength: undefined} : column), hidden};
}

/** A pending value typed as the canvas component reads it (boolean, number or text); undefined when the server would reject it */
export function previewValue(valueType: ValueType | string, value: string, allowedValues: string[]): boolean | number | string | undefined {
  if (validateValue(valueType, value, allowedValues)) return undefined;
  if (valueType === "BOOLEAN") return /^true$/i.test(value);
  return valueType === "INTEGER" ? Number(value) : value;
}

/** Server-side search of the screen picker: one character like the AWE suggests, the same delay as the user search, a short page of matches */
export const SCREEN_SEARCH = {minChars: 1, delay: 250, limit: 50};

/** The first matches of a list (the server pages the query, this keeps the page short when it does not) */
export const limitMatches = <T,>(rows: T[], limit: number): T[] => rows.slice(0, limit);

/**
 * Out-of-order answers: each search takes a ticket, and only the latest one is still current when its answer arrives
 * @return Function that takes a ticket; the ticket tells whether it is still the latest
 */
export const latestOnly = (): (() => () => boolean) => {
  let last = 0;
  return () => {
    const mine = ++last;
    return () => mine === last;
  };
};

/** Text of the user of a user scope: only a chosen user (the search text is half typed until then) */
export const chosenUserText = (scope: Scope, text: string): string => (scope.id != null ? text : "");

/** The "View as" of the canvas: the edited scope ("scope", linked) or a subject of its own */
export interface ViewAs { type: "scope" | ScopeType; id: number | null }

/**
 * Who the canvas shows the screen to: the edited scope while View as is linked, else the chosen subject. A profile or a
 * user not chosen yet keeps the last complete subject, so the canvas does not reload on a half-typed search
 */
export function canvasSubject(scope: Scope, viewAs: ViewAs, last: Scope): Scope {
  const subject = viewAs.type === "scope" ? scope : viewAs as Scope;
  return subject.type !== "everyone" && subject.id == null ? last : subject;
}

/** Extra parameters of the screen data request that render the screen as the subject gets it */
export const subjectParams = ({type, id}: Scope): { subjectType: ScopeType; subjectId: number | null } => ({subjectType: type, subjectId: id});

/** A rule of the overview as the server describes it (getScreenConfigurationOverview); the ids are text, empty when not set */
export interface OverviewRow {
  ruleId: number; component: string; attribute: string; scopeType: ScopeType; profileId: string; profileName: string;
  userId: string; userName: string; value: string; problem: "" | "missingElement" | "invalidAttribute" | "missingSubject" | "duplicate"; duplicateGroup: string
}
/** What one subject sees: its scope, the id and name of the profile or user ("" for everyone) and the value */
export interface OverviewEntry { ruleId: number; scope: ScopeType; id: number | null; name: string; profileName: string; value: string }
/** The personalized option of an element, with what each subject sees */
export interface OverviewBlock { component: string; attribute: string; entries: OverviewEntry[] }
/** A rule that no longer works (a group of duplicates is one problem with several rows) */
export interface OverviewProblem { kind: OverviewRow["problem"]; component: string; attribute: string; rows: OverviewRow[] }

const PROBLEM_ORDER = ["missingElement", "invalidAttribute", "missingSubject", "duplicate"];

/**
 * Overview of a screen from the rows of the server: one block per personalized element and option, and the problems.
 * A column's hidden is shown as Visible (inverted), as the editor does
 * @param columns Components that are columns
 */
export function buildOverview(rows: OverviewRow[], columns: Set<string>): { blocks: OverviewBlock[]; changedElements: number; problems: OverviewProblem[] } {
  const shown = (row: OverviewRow): OverviewRow => row.attribute === "hidden" && columns.has(row.component)
    ? {...row, attribute: "visible", value: invertBoolean(row.value)} : row;
  const blocks = new Map<string, OverviewBlock>();
  rows.map(shown).filter(row => !row.problem).forEach(row => {
    const key = `${row.component}|${row.attribute}`;
    const block = blocks.get(key) ?? {component: row.component, attribute: row.attribute, entries: []};
    const id = row.scopeType === "user" ? row.userId : row.scopeType === "profile" ? row.profileId : "";
    block.entries.push({ruleId: row.ruleId, scope: row.scopeType, id: id === "" ? null : Number(id), name: row.scopeType === "user" ? row.userName : row.profileName,
      profileName: row.scopeType === "user" ? row.profileName : "", value: row.value});
    blocks.set(key, block);
  });
  blocks.forEach(block => block.entries.sort((a, b) => SCOPE_RANK[a.scope] - SCOPE_RANK[b.scope] || a.name.localeCompare(b.name)));
  const problems = new Map<string, OverviewProblem>();
  rows.map(shown).filter(row => row.problem).forEach(row => {
    const key = row.problem === "duplicate" ? `duplicate|${row.duplicateGroup}` : `${row.problem}|${row.ruleId}`;
    const problem = problems.get(key) ?? {kind: row.problem, component: row.component, attribute: row.attribute, rows: []};
    problem.rows.push(row);
    problems.set(key, problem);
  });
  return {
    blocks: [...blocks.values()], changedElements: new Set([...blocks.values()].map(block => block.component)).size,
    problems: [...problems.values()].sort((a, b) => PROBLEM_ORDER.indexOf(a.kind) - PROBLEM_ORDER.indexOf(b.kind))
  };
}

/** A value as the editor shows it: Visible/Hidden, Yes/No for the other booleans, a label key in the current language */
export function formatOverviewValue(attribute: string, value: string, t: (key: string) => string): string {
  if (/^(true|false)$/i.test(value)) {
    const on = /^true$/i.test(value);
    return t(attribute === "visible" ? (on ? "SCR_PER_VALUE_VISIBLE" : "SCR_PER_VALUE_HIDDEN") : (on ? "SCR_PER_VALUE_YES" : "SCR_PER_VALUE_NO"));
  }
  return TRANSLATABLE_ATTRIBUTES.includes(attribute) ? t(value) : value;
}

const TRANSLATABLE_ATTRIBUTES = ["label", "title", "placeholder"];

/** Changes that delete rules of the overview (sent to the update maintain) */
export const deleteChanges = (rows: OverviewRow[], screen: string): Change[] => rows.map(row => ({
  operation: "DELETE", id: row.ruleId, screen, component: row.component, attribute: row.attribute, value: row.value,
  user: row.userId === "" ? null : Number(row.userId), profile: row.profileId === "" ? null : Number(row.profileId)
}));

/** Keep one rule of a group of duplicates: delete the others */
export const keepOneChanges = (rows: OverviewRow[], keepId: number, screen: string): Change[] => deleteChanges(rows.filter(row => row.ruleId !== keepId), screen);

/** An element of the screen as the editor lists it */
export interface OverviewElement { id: string; label?: string; elementType?: string }
/** Words for an element, an option and a subject of the overview and the history */
function labelsOf(elements: Record<string, OverviewElement>, translate: (key: string) => string) {
  const element = (id: string) => elements[id] ? `${elements[id].elementType} "${elements[id].label || id}"` : id;
  const option = (attribute: string) => {
    const text = translate(attributeLabelKey(attribute));
    return text === attributeLabelKey(attribute) ? attribute : text;
  };
  const who = (type: ScopeType, name: string, profileName = "") => type === "everyone" ? translate("SCR_PER_SCOPE_EVERYONE")
    : `${translate(type === "profile" ? "SCR_PER_SCOPE_PROFILE" : "SCR_PER_SCOPE_USER")} ${name}${profileName ? ` (${profileName})` : ""}`;
  return {element, option, who};
}

export interface OverviewView {
  title: string; warning: string;
  blocks: Array<{ key: string; component: string; name: string; entries: Array<{ ruleId: number; text: string; entry: OverviewEntry }> }>;
  problems: Array<{ key: string; kind: OverviewProblem["kind"]; text: string; rows: OverviewRow[]; keep: Array<{ ruleId: number; text: string }> }>;
}

/**
 * The overview in plain words, ready to draw in either engine
 * @param screen Screen name shown in the title
 * @param elements Elements of the screen by identifier
 * @param msg Translates a locale key and fills its {0}, {1}... placeholders
 * @param translate Translates one locale key (the label of an option, a label value)
 */
export function describeOverview(screen: string, rows: OverviewRow[], elements: Record<string, OverviewElement>, msg: (key: string, ...args: Array<string | number>) => string, translate: (key: string) => string): OverviewView {
  const columns = new Set(Object.values(elements).filter(element => element.elementType === "Column").map(element => element.id));
  const {blocks, changedElements, problems} = buildOverview(rows, columns);
  const {element, option, who} = labelsOf(elements, translate);
  const text = (problem: OverviewProblem): string => {
    const [row] = problem.rows;
    switch (problem.kind) {
      case "missingElement": return msg("SCR_PER_PROBLEM_MISSING_ELEMENT", row.component, option(row.attribute));
      case "invalidAttribute": return msg("SCR_PER_PROBLEM_INVALID_ATTRIBUTE", element(row.component), option(row.attribute));
      case "missingSubject": return msg(row.userId === "" ? "SCR_PER_PROBLEM_MISSING_PROFILE" : "SCR_PER_PROBLEM_MISSING_USER", element(row.component), option(row.attribute));
      default: return msg("SCR_PER_PROBLEM_DUPLICATE", element(row.component), option(row.attribute),
        who(row.scopeType, row.scopeType === "user" ? row.userName : row.profileName), problem.rows.length);
    }
  };
  return {
    title: msg("SCR_PER_OVERVIEW_TITLE", screen, changedElements),
    warning: msg("SCR_PER_PROBLEMS", problems.length),
    blocks: blocks.map(block => ({
      key: `${block.component}|${block.attribute}`, component: block.component, name: `${element(block.component)} · ${option(block.attribute)}`,
      entries: block.entries.map(entry => ({ruleId: entry.ruleId, entry, text: `${who(entry.scope, entry.name, entry.profileName)} → ${formatOverviewValue(block.attribute, entry.value, translate)}`}))
    })),
    problems: problems.map(problem => ({
      key: `${problem.kind}|${problem.rows[0].ruleId}`, kind: problem.kind, text: text(problem), rows: problem.rows,
      keep: problem.kind === "duplicate" ? problem.rows.map(row => ({ruleId: row.ruleId, text: msg("SCR_PER_KEEP_VALUE", formatOverviewValue(row.attribute, row.value, translate))})) : []
    }))
  };
}

/** A change of the history as the server describes it (getScreenConfigurationHistory); undoable may arrive as text */
export interface HistoryRow {
  date: string; operator: string; action: "created" | "changed" | "deleted"; ruleId: number; component: string; attribute: string; screen: string;
  scopeType: ScopeType; profileId: string; profileName: string; userId: string; userName: string; value: string; valueBefore: string; valueAfter: string; undoable: boolean | string
}

/** Changes that undo a change of the history, sent to the update maintain: a created rule is deleted, a changed one gets its value back, a deleted one is inserted again */
export function undoChanges(row: HistoryRow, screen: string): Change[] {
  const base = {screen, component: row.component, attribute: row.attribute, user: row.userId === "" ? null : Number(row.userId), profile: row.profileId === "" ? null : Number(row.profileId)};
  switch (row.action) {
    case "created": return [{...base, operation: "DELETE", id: row.ruleId, value: row.valueAfter}];
    case "changed": return [{...base, operation: "UPDATE", id: row.ruleId, value: row.valueBefore}];
    default: return [{...base, operation: "INSERT", id: null, value: row.valueBefore}];
  }
}

/** A change of the history in plain words, ready to draw in either engine */
export interface HistoryItem { key: string; when: string; operator: string; name: string; change: string; undoable: boolean; unrecorded: boolean; undo: Change[] }

/**
 * The history in plain words: newest first as the server gives it. A column's hidden is shown as Visible (inverted), as the editor does
 * @param elements Elements of the screen by identifier
 * @param msg Translates a locale key and fills its {0}, {1}... placeholders
 * @param translate Translates one locale key
 */
export function describeHistory(screen: string, rows: HistoryRow[], elements: Record<string, OverviewElement>, msg: (key: string, ...args: Array<string | number>) => string, translate: (key: string) => string): HistoryItem[] {
  const {element, option, who} = labelsOf(elements, translate);
  return rows.map((row, index) => {
    const column = row.attribute === "hidden" && elements[row.component]?.elementType === "Column";
    const attribute = column ? "visible" : row.attribute;
    const show = (value: string) => formatOverviewValue(attribute, column ? invertBoolean(value) : value, translate);
    const undoable = String(row.undoable) === "true";
    // Not undoable and nothing newer for the rule: the first change recorded of a rule that existed before the history
    const unrecorded = !undoable && row.action === "changed" && row.valueBefore === "" && !rows.slice(0, index).some(newer => newer.ruleId === row.ruleId);
    const [, day, month, time] = /^(\d{2})\/(\d{2})\/\d{4} (\d{2}:\d{2})/.exec(row.date) ?? [];
    return {
      key: `${row.ruleId}|${row.date}|${row.action}`, when: time ? `${day}/${month} ${time}` : row.date, operator: row.operator, undoable, unrecorded,
      name: `${element(row.component)} · ${option(attribute)} · ${who(row.scopeType, row.scopeType === "user" ? row.userName : row.profileName, row.scopeType === "user" ? row.profileName : "")}`,
      change: row.action === "created" ? msg("SCR_PER_HISTORY_CREATED", show(row.valueAfter)) : row.action === "deleted" ? msg("SCR_PER_HISTORY_DELETED", show(row.valueBefore))
        : unrecorded ? msg("SCR_PER_HISTORY_CHANGED_TO", show(row.valueAfter)) : `${show(row.valueBefore)} → ${show(row.valueAfter)}`,
      undo: undoChanges(row, screen)
    };
  });
}

/** The element and option of each rule an import leaves out, named as the overview and the history name them */
export function describeSkipped(skipped: ImportPlan["skipped"], elements: Record<string, OverviewElement>, translate: (key: string) => string) {
  const {element, option} = labelsOf(elements, translate);
  return skipped.map(skip => ({component: element(skip.component), attribute: option(skip.attribute)}));
}

/** A rule of the server export: who it is for is the acronym of a profile or the login of a user */
export interface ExportRow { component: string; attribute: string; value: string; scopeType: ScopeType; profile: string; user: string }
/** The export file: every rule of a screen, portable between installations */
export interface ExportFile { screen: string; version: 1; rules: Array<{ component: string; attribute: string; value: string; scope: ScopeType; profile?: string; user?: string }> }

export const buildExport = (screen: string, rows: ExportRow[]): ExportFile => ({
  screen, version: 1, rules: rows.map(row => ({component: row.component, attribute: row.attribute, value: row.value, scope: row.scopeType,
    ...(row.scopeType === "profile" ? {profile: row.profile} : {}), ...(row.scopeType === "user" ? {user: row.user} : {})}))
});

const parseImport = (text: string): ExportFile | null => {
  try {
    const file = JSON.parse(text);
    return file?.version === 1 && typeof file.screen === "string" && Array.isArray(file.rules) ? file : null;
  } catch {
    return null;
  }
};

/** Logins of the users of an import file, to look them up here */
export const importLogins = (text: string): string[] => [...new Set((parseImport(text)?.rules ?? []).filter(rule => rule.scope === "user" && rule.user).map(rule => rule.user as string))];

/** What an import would do: the changes to send, how many of each, and the rules left out with their reason */
export interface ImportPlan {
  error: null | { code: "INVALID_FILE" } | { code: "OTHER_SCREEN"; screen: string };
  changes: Change[]; toAdd: number; toChange: number; unchanged: number;
  skipped: Array<{ component: string; attribute: string; scope: ScopeType; name: string; reason: "profile" | "user" | "element" }>
}

type ImportEntry = ExportFile["rules"][number];

/** Who an entry of an import file is for here: undefined id when the profile or user is not found, null for everyone */
const importSubject = (entry: ImportEntry, profiles: Array<{ id: number; acronym: string }>, users: Array<{ id: number; login: string }>) => {
  if (entry.scope === "profile") {
    const name = entry.profile ?? "";
    return {name, id: profiles.find(profile => profile.acronym === name)?.id};
  }
  if (entry.scope === "user") {
    const name = entry.user ?? "";
    return {name, id: users.find(user => user.login === name)?.id};
  }
  return {name: "", id: null};
};

/** Add what an entry of an import file does to the plan: a change, an unchanged rule or a skipped one */
function planEntry(plan: ImportPlan, entry: ImportEntry, screen: string, rules: Rule[], elements: Record<string, unknown>, profiles: Array<{ id: number; acronym: string }>, users: Array<{ id: number; login: string }>) {
  const {name, id} = importSubject(entry, profiles, users);
  const skip = (reason: ImportPlan["skipped"][number]["reason"], who: string) => plan.skipped.push({component: entry.component, attribute: entry.attribute, scope: entry.scope, name: who, reason});
  if (id === undefined) {
    skip(entry.scope === "profile" ? "profile" : "user", name);
    return;
  }
  if (!elements[entry.component]) {
    skip("element", entry.component);
    return;
  }
  const scope: Scope = {type: entry.scope, id: id ?? null};
  const current = rules.find(rule => rule.active === 1 && rule.component === entry.component && rule.attribute === entry.attribute && scopeOf(rule).type === scope.type && scopeOf(rule).id === scope.id);
  const base = {screen, component: entry.component, attribute: entry.attribute, value: entry.value, user: scope.type === "user" ? scope.id : null, profile: scope.type === "profile" ? scope.id : null};
  if (!current) plan.changes.push({...base, operation: "INSERT", id: null});
  else if (current.value !== entry.value) plan.changes.push({...base, operation: "UPDATE", id: current.id});
  else plan.unchanged++;
}

/**
 * Compare an import file with the rules of the screen: new rules are inserted and rules with another value are updated, nothing is ever deleted
 * @param elements Elements of the screen that can be configured, by identifier
 * @param profiles Profiles here, by acronym
 * @param users Users here that the file names, by login
 */
export function planImport(text: string, screen: string, rules: Rule[], elements: Record<string, unknown>, profiles: Array<{ id: number; acronym: string }>, users: Array<{ id: number; login: string }>): ImportPlan {
  const plan: ImportPlan = {error: null, changes: [], toAdd: 0, toChange: 0, unchanged: 0, skipped: []};
  const file = parseImport(text);
  if (!file) return {...plan, error: {code: "INVALID_FILE"}};
  if (file.screen !== screen) return {...plan, error: {code: "OTHER_SCREEN", screen: file.screen}};
  for (const entry of file.rules) planEntry(plan, entry, screen, rules, elements, profiles, users);
  plan.toAdd = plan.changes.filter(change => change.operation === "INSERT").length;
  plan.toChange = plan.changes.length - plan.toAdd;
  return plan;
}
