const fs = require('fs');
const path = require('path');

// A left label never takes more than half of its criterion (#885, on par with the AngularJS engine): its width comes
// from the criterion in pixels, and in a narrow column it left the field a few pixels wide
describe('Left label width (#885)', () => {
  const less = fs.readFileSync(path.join(__dirname, '../../../src/criteria/AweCriterion.less'), 'utf8');
  const block = less.slice(less.indexOf('.field.label-left {'));

  it('measures the label against the width of its criterion', () => {
    expect(block).toMatch(/^\.field\.label-left \{[^}]*container-type: inline-size;/);
  });

  it('limits the label to half of the criterion, the field takes the rest', () => {
    expect(block).toMatch(/> label,\s*> div\.block \{[^}]*max-width: 50cqi;/);
  });
});
