/**
 * Pieces the criteria playgrounds share (criteria.js and the families: criteria-text.js, criteria-numbers.js,
 * criteria-choices.js, criteria-upload.js). Not a spec: specs/index.js does not list it.
 * Every attribute is one the XSD declares for `criteria` and the React client reads.
 */

// A playground with one kind has no component control: its controls always apply
const whenKind = kinds => (kinds ? {id: 'component', in: kinds} : undefined);

/** The `component` control of a family: `kinds` are [value, label] pairs. */
const componentControl = (kinds, initial) => ({
  id: 'component', type: 'select', label: 'Component', xml: 'component', client: '', default: '', initial,
  options: kinds.map(([value, label]) => ({value, label})),
});

const labelControl = (kinds, initial = 'Field') => ({
  id: 'label', type: 'text', label: 'Label', xml: 'label', default: '', initial, when: whenKind(kinds),
});

const placeholderControl = kinds => ({
  id: 'placeholder', type: 'text', label: 'Placeholder', xml: 'placeholder', default: '', initial: '',
  when: whenKind(kinds),
});

const iconControl = kinds => ({
  id: 'icon', type: 'icon', label: 'Icon', xml: 'icon', default: '', initial: '',
  options: ['', 'lucide:user', 'lucide:mail', 'lucide:search', 'lucide:calendar'], when: whenKind(kinds),
});

const sizeControl = kinds => ({
  id: 'size', type: 'segmented', label: 'Size', xml: 'size', default: '', when: whenKind(kinds),
  options: [
    {value: '', label: 'Default'},
    {value: 'sm', label: 'Small'},
    {value: 'lg', label: 'Large'},
  ],
});

const requiredControl = kinds => ({
  id: 'required', type: 'switch', label: 'Required', xml: 'validation', token: 'required', client: '', default: false,
  when: whenKind(kinds),
});

const readonlyControl = kinds => ({
  id: 'readonly', type: 'switch', label: 'Read only', xml: 'readonly', default: false, when: whenKind(kinds),
});

/**
 * The server sends the required rule with the criterion: the client reads it as an attribute and as a rule. Valid
 * only for the kinds the `required` control applies to (a hidden control keeps its value).
 */
const requiredScene = (values, kinds) => {
  const required = Boolean(values.required) && (!kinds || kinds.includes(values.component));
  return {
    attributes: required ? {required: true} : {},
    validationRules: required ? {required: true} : undefined,
  };
};

// Options that come from an enumerated list or a query on the server: the stage fills them with a static model
const COUNTRIES = [
  {value: 'es', label: 'Spain'},
  {value: 'fr', label: 'France'},
  {value: 'de', label: 'Germany'},
  {value: 'it', label: 'Italy'},
  {value: 'pt', label: 'Portugal'},
  {value: 'cl', label: 'Chile'},
];

const EVENTS = [];

const ACTIONS = [
  {name: 'reset', description: 'Empties the value of the criterion.'},
  {name: 'restore', description: 'Puts back the initial value of the criterion.'},
  {name: 'validate', description: 'Runs the validation of the criterion (required, formats) and shows its error.'},
  {name: 'set-invalid', description: 'Marks the criterion as invalid with the message of the action.'},
  {name: 'set-valid', description: 'Clears the validation error of the criterion.'},
  {name: 'copy-criterion-value-clipboard', description: 'Copies the value of the criterion to the clipboard.'},
];

module.exports = {
  componentControl, labelControl, placeholderControl, iconControl, sizeControl, requiredControl, readonlyControl,
  requiredScene, COUNTRIES, EVENTS, ACTIONS,
};
