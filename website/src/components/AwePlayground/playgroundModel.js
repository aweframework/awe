/**
 * Logic of the component playgrounds, free of React so that node:test can run it.
 *
 * A playground is described by a declarative SPEC:
 *
 *   {
 *     tag: 'button',                 XML element written by the playground
 *     id: 'demo',                    value of its id attribute (always written, first)
 *     controls: [Control],           what the reader can change
 *     children: [Child],             optional nested elements
 *     element: 'Card',               optional: client structure element (elementType) the stage renders
 *     layout: 'stacked',             optional: data components (grid, chart, map) put the controls under the preview
 *     height: '36rem',               optional: height of the stage (a component that fills its container needs one)
 *     xmlAttributes: values => [[name, value]],   optional fixed attributes written after the ones of the controls
 *     scene: values => Scene,        optional description of what the stage renders (see toScene)
 *   }
 *
 *   Control = {
 *     id: 'type',                    key of the value
 *     type: 'segmented' | 'select' | 'switch' | 'text' | 'icon',
 *     label: 'Button type',          text of the control (the UI translates it)
 *     xml: 'button-type',            XML attribute it writes (none: only the client attribute / a child condition)
 *     client: 'buttonType',          attribute of the React client (default: camelCase of xml)
 *     default: 'button',             value the XSD assumes when the attribute is missing: it is NOT written
 *     initial: 'submit',             value the playground starts with (default: `default`)
 *     options: ['button', ...],      allowed values of segmented / select / icon controls: strings or
 *                                    {value, label} (the label is what the reader sees)
 *     token: 'no-label',             switch only: the token it adds to a space-separated attribute (style)
 *   }
 *
 *   Child = {when?: 'controlId' | {id, in: [...]}, tag, attrs?: {name: value} | values => {name: value}, children?: [Child]}
 *     `when` names a control: the child is written only while its value is truthy (or one of `in`).
 *     `siblings: [Child]` of the spec are written after the element, at its level (a button that opens a dialog).
 *
 * Controls that write the same xml attribute join their values with a space, in the order of the spec.
 */

const isObject = value => value !== null && typeof value === 'object' && !Array.isArray(value);

/** Values a control accepts: its options are strings or {value, label} objects. */
const optionValues = control => (control.options ?? []).map(option => (isObject(option) ? option.value : option));

const kindOk = (control, value) => {
  if (control.type === 'switch') return typeof value === 'boolean';
  if (typeof value !== 'string') return false;
  return !control.options || optionValues(control).includes(value);
};

const defaultOf = control => control.default ?? (control.type === 'switch' ? false : '');
const startOf = control => control.initial ?? defaultOf(control);

/** The control applies while the value of the control it depends on is one of the allowed ones. */
const applies = (control, values) => !control.when || control.when.in.includes(values[control.when.id]);

/** Controls the reader sees for the given values, in the order of the spec. */
const visibleControls = (spec, values) => spec.controls.filter(control => applies(control, values));

const camelCase = name => name.replace(/-([a-z])/g, (match, letter) => letter.toUpperCase());

/** Escapes a value for a double-quoted XML attribute. */
function escapeXml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/** Values the playground starts with. */
function initialValues(spec) {
  return Object.fromEntries(spec.controls.map(control => [control.id, startOf(control)]));
}

/**
 * Complete and valid values: unknown ids are dropped, missing ones take their initial value and a value that is not
 * allowed by its control (wrong kind, outside the options) is replaced by the initial one.
 */
function resolveValues(spec, values = {}) {
  const given = isObject(values) ? values : {};
  return Object.fromEntries(spec.controls.map(control => {
    const value = given[control.id];
    return [control.id, kindOk(control, value) ? value : startOf(control)];
  }));
}

// Pieces of every attribute, in the order of the spec: [{name, value, client}]
function collect(spec, values, nameOf) {
  const attributes = new Map();
  for (const control of spec.controls) {
    const name = nameOf(control);
    if (!name || !applies(control, values)) continue;
    const value = values[control.id];
    const isToken = control.type === 'switch' && control.token;
    if (isToken ? !value : value === defaultOf(control)) continue;
    if (!isToken && value === '') continue;
    const text = isToken ? control.token : value;
    attributes.set(name, [...(attributes.get(name) ?? []), text]);
  }
  return attributes;
}

/**
 * Attributes of the XML element as [name, value] pairs: the id first, then the controls in the order of the spec.
 * A value equal to the default of its control is omitted.
 */
function buildAttributes(spec, values) {
  const written = collect(spec, values, control => control.xml);
  const fixed = spec.xmlAttributes ? spec.xmlAttributes(values) : [];
  return [['id', spec.id], ...[...written].map(([name, pieces]) => [name, pieces.join(' ')]), ...fixed];
}

const clientName = control => control.client ?? (control.xml ? camelCase(control.xml) : null);

/**
 * Attributes for the state of the React client component. Booleans stay booleans and empty values are skipped, so the
 * client applies its own defaults; a switch on by default writes false when it is turned off.
 */
function toClientAttributes(spec, values) {
  const attributes = {};
  for (const control of spec.controls) {
    const name = clientName(control);
    if (!name || !applies(control, values)) continue;
    const value = values[control.id];
    if (control.type === 'switch' && !control.token) {
      // A switch on by default stands for a client default of true: turned off, it has to say false
      if (value) attributes[name] = true;
      else if (control.default === true) attributes[name] = false;
    } else if (control.type === 'switch') {
      if (value) attributes[name] = [attributes[name], control.token].filter(Boolean).join(' ');
    } else if (value !== '') {
      attributes[name] = [attributes[name], value].filter(Boolean).join(' ');
    }
  }
  return attributes;
}

/**
 * What the stage renders for the given values: `components` is the description for AweLive (the root component under
 * its id, plus the extra ones of the scene) and `nodes` the elements of the client structure to render, in order.
 *
 * A spec describes more than its attributes with `scene(values)`, which may return:
 *   attributes        extra client attributes of the root component
 *   model, validationRules   state of the root component (model values of a select, required rule...)
 *   actions           actions of the root component (a button that opens a context menu)
 *   node              extra properties of the root node (`component` of a criterion, `elementList` of a container)
 *   components        description of other components by id (the fields inside a tab, the buttons of a dialog)
 *   nodes             other elements rendered after the root one
 *   specificAttributes  sort, first row... of the root (a grid starts sorted or on another page)
 *   files             {target action: static file url} the file viewers read instead of a server
 *   options           {target action: [{value, label}]} the suggests search instead of a server
 *   uploads           true: an uploader simulates its upload instead of sending the file
 *   messages          {id: {title, message}} the `message` elements of the screen (the `confirm` actions name them)
 *   answers           {target action: [actions]} what the server answers to a server action of that target action
 *   settings          client settings (the url of the tiles of a map)
 */
function toScene(spec, values) {
  const extra = spec.scene ? spec.scene(values) : {};
  const root = {attributes: {...toClientAttributes(spec, values), ...extra.attributes}};
  if (extra.model) root.model = extra.model;
  if (extra.validationRules) root.validationRules = extra.validationRules;
  if (extra.specificAttributes) root.specificAttributes = extra.specificAttributes;
  if (extra.actions) root.actions = extra.actions;
  return {
    components: {[spec.id]: root, ...extra.components},
    nodes: [{elementType: spec.element, id: spec.id, ...extra.node}, ...(extra.nodes ?? [])],
    ...(extra.files && {files: extra.files}),
    ...(extra.options && {options: extra.options}),
    ...(extra.uploads && {uploads: true}),
    ...(extra.messages && {messages: extra.messages}),
    ...(extra.answers && {answers: extra.answers}),
    ...(extra.settings && {settings: extra.settings}),
  };
}

/** Description for AweLive: `<AweLive components={toComponents(spec, values)}>`. */
function toComponents(spec, values) {
  return toScene(spec, values).components;
}

const openTag = (tag, attributes) =>
  [tag, ...attributes.map(([name, value]) => `${name}="${escapeXml(value)}"`)].join(' ');

// An element longer than this writes one attribute per line
const WRAP_AT = 80;

/** The opening tag: on one line, or with the attributes aligned under the first one when the line gets too long. */
function writeOpenTag(element, attributes, pad, close) {
  const line = `${pad}<${openTag(element, attributes)}${close}`;
  if (line.length <= WRAP_AT || attributes.length < 2) return line;
  const [first, ...rest] = attributes.map(([name, value]) => `${name}="${escapeXml(value)}"`);
  const align = `${pad}${' '.repeat(element.length + 2)}`;
  return [`${pad}<${element} ${first}`, ...rest.map(item => `${align}${item}`)].join('\n') + close;
}

function writeElement(element, attributes, children, depth) {
  const pad = '  '.repeat(depth);
  if (children.length === 0) return writeOpenTag(element, attributes, pad, ' />');
  return [writeOpenTag(element, attributes, pad, '>'), ...children, `${pad}</${element}>`].join('\n');
}

// A child is written while the control named by `when` is truthy and visible (a hidden control keeps its value, which
// must not write anything), or (as a control does) while its value is one of `in`
const childApplies = (spec, child, values) => {
  if (!child.when) return true;
  if (isObject(child.when)) return applies(child, values);
  const control = spec.controls.find(item => item.id === child.when);
  return Boolean(values[child.when]) && (!control || applies(control, values));
};

function writeChild(spec, child, values, depth) {
  if (!childApplies(spec, child, values)) return null;
  const nested = (child.children ?? []).map(item => writeChild(spec, item, values, depth + 1)).filter(Boolean);
  const attrs = typeof child.attrs === 'function' ? child.attrs(values) : child.attrs;
  return writeElement(child.tag, Object.entries(attrs ?? {}), nested, depth);
}

/** XML of the component for the given values, with two-space indentation. */
function toXml(spec, values) {
  const children = (spec.children ?? []).map(child => writeChild(spec, child, values, 1)).filter(Boolean);
  const siblings = (spec.siblings ?? []).map(sibling => writeChild(spec, sibling, values, 0)).filter(Boolean);
  return [writeElement(spec.tag, buildAttributes(spec, values), children, 0), ...siblings].join('\n');
}

// Ids of the translations (code.json) of the strings a spec shows: the text of the specs is English
const controlKey = (spec, control) => `playground.${spec.name}.control.${control.id}`;
// An empty value is "none", unless the control has a real `none` option too: then it is "empty"
const optionKey = (spec, control, option) => {
  const clash = option.value === '' && optionValues(control).includes('none');
  return `playground.${spec.name}.option.${control.id}.${option.value || (clash ? 'empty' : 'none')}`;
};
const eventKey = (spec, event) => `playground.${spec.name}.event.${event.name}`;
const actionKey = (spec, action) => `playground.${spec.name}.action.${action.name}`;

/** Every string a spec shows as {translation id: English text}: controls, their options, events and actions. */
function translationTexts(spec) {
  const texts = {};
  for (const control of spec.controls) {
    texts[controlKey(spec, control)] = control.label;
    // The options of an icon picker are the names of the icons, not text
    for (const option of control.type === 'icon' ? [] : control.options ?? []) {
      const normal = isObject(option) ? option : {value: option, label: option};
      texts[optionKey(spec, control, normal)] = String(normal.label);
    }
  }
  for (const event of spec.events ?? []) texts[eventKey(spec, event)] = event.description;
  for (const action of spec.actions ?? []) texts[actionKey(spec, action)] = action.description;
  return texts;
}

module.exports = {translationTexts, controlKey, optionKey, eventKey, actionKey, escapeXml, optionValues, visibleControls, toScene, initialValues, resolveValues, buildAttributes, toClientAttributes, toComponents, toXml};
