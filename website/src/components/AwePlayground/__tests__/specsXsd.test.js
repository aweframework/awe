const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const {loadSchemas, buildReference} = require('../../../../scripts/xsd-reference/xsd-model');
const {initialValues, resolveValues, buildAttributes, visibleControls} = require('../playgroundModel');
const specs = require('../specs');

const XSD_DIR = path.resolve(__dirname, '..', '..', '..', '..', '..', 'awe-framework', 'awe-generic-screens', 'src', 'main', 'resources', 'schemas', 'awe');
const reference = buildReference(loadSchemas(XSD_DIR));
const screen = reference.schemas.find(schema => schema.file === 'screen.xsd');
const declared = tag => new Set(screen.elements.find(element => element.name === tag)?.attributes.map(attribute => attribute.name));

// Every value a spec can take: the initial ones and each option of each control on its own
function variants(spec) {
  const base = initialValues(spec);
  const all = [base];
  for (const control of spec.controls) {
    const options = control.type === 'switch' ? [true, false] : (control.options ?? []).map(option => option.value ?? option);
    for (const value of options) all.push(resolveValues(spec, {...base, [control.id]: value}));
  }
  return all;
}

// Attributes the XML of a spec writes, by element: its own and the ones of its children and siblings
function written(spec) {
  const byTag = new Map();
  const add = (tag, names) => byTag.set(tag, new Set([...(byTag.get(tag) ?? []), ...names]));
  // The attributes of a child can depend on the values: every variant counts
  const attrsOf = child => (typeof child.attrs === 'function' ? variants(spec).map(child.attrs) : [child.attrs ?? {}]);
  const walk = child => {
    attrsOf(child).forEach(attrs => add(child.tag, Object.keys(attrs)));
    (child.children ?? []).forEach(walk);
  };
  for (const values of variants(spec)) add(spec.tag, buildAttributes(spec, values).map(([name]) => name));
  [...(spec.children ?? []), ...(spec.siblings ?? [])].forEach(walk);
  return byTag;
}

for (const spec of Object.values(specs)) {
  test(`the XML of the ${spec.name} playground only uses attributes the XSD declares`, () => {
    for (const [tag, names] of written(spec)) {
      const known = declared(tag);
      assert.ok(known.size > 0, `${tag} is an element of screen.xsd`);
      const unknown = [...names].filter(name => !known.has(name));
      assert.deepEqual(unknown, [], `${tag}: attributes missing in the XSD`);
    }
  });

  test(`every control of the ${spec.name} playground is visible for some values`, () => {
    for (const control of spec.controls) {
      const shown = variants(spec).some(values => visibleControls(spec, values).includes(control));
      assert.ok(shown, control.id);
    }
  });
}
