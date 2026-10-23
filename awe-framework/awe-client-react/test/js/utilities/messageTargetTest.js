import {findMessageTarget} from '../../../src/utilities/messageTarget';

describe('awe-react-client/test/js/utilities/messageTargetTest.js', () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <div criterion-id="Crt"><input/></div>
      <div grid-id="Grd">
        <div row-id="1"><div column-id="Col" id="cell-1"></div></div>
        <div row-id="2"><div column-id="Col" id="cell-2"></div></div>
      </div>
      <div tree-grid-id="Tre"></div>
      <div id="Plain"></div>`;
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('finds the criterion of the address', () => {
    expect(findMessageTarget({view: 'base', component: 'Crt'})).toBe(document.querySelector('[criterion-id="Crt"]'));
  });

  it('finds a grid, a tree grid and a component by its id', () => {
    expect(findMessageTarget({view: 'base', component: 'Grd'})).toBe(document.querySelector('[grid-id="Grd"]'));
    expect(findMessageTarget({view: 'base', component: 'Tre'})).toBe(document.querySelector('[tree-grid-id="Tre"]'));
    expect(findMessageTarget({view: 'base', component: 'Plain'})).toBe(document.getElementById('Plain'));
  });

  it('finds the cell of a grid when the address has a row and a column', () => {
    expect(findMessageTarget({view: 'base', component: 'Grd', row: '2', column: 'Col'})).toBe(document.getElementById('cell-2'));
  });

  it('falls back to the grid when the cell is not rendered', () => {
    expect(findMessageTarget({view: 'base', component: 'Grd', row: '9', column: 'Col'})).toBe(document.querySelector('[grid-id="Grd"]'));
  });

  it('finds nothing without a view and a component, or when the component is not rendered', () => {
    expect(findMessageTarget(undefined)).toBeNull();
    expect(findMessageTarget({view: 'base'})).toBeNull();
    expect(findMessageTarget({component: 'Crt'})).toBeNull();
    expect(findMessageTarget({view: 'base', component: 'Missing'})).toBeNull();
  });

  it('does not break with identifiers that need escaping', () => {
    expect(findMessageTarget({view: 'base', component: 'a"b\\c'})).toBeNull();
  });

  it('does not break with identifiers that have line breaks', () => {
    const element = document.createElement('div');
    element.setAttribute('criterion-id', 'line\nbreak\r\f');
    document.body.appendChild(element);

    expect(findMessageTarget({view: 'base', component: 'line\nbreak\r\f'})).toBe(element);
    expect(findMessageTarget({view: 'base', component: 'other\nline'})).toBeNull();
  });

  describe('with the same component in several views', () => {
    beforeEach(() => {
      // A report view inside the base view, and a dialog of the report view (rendered at the end of the body)
      document.body.innerHTML = `
        <div view-id="base">
          <div view-id="report"><div criterion-id="Dup" id="report-dup"></div></div>
          <div criterion-id="Dup" id="base-dup"></div>
        </div>
        <div view-id="report" class="dialog"><div criterion-id="Only" id="dialog-only"></div></div>`;
    });

    it('finds the component of the view of the address', () => {
      expect(findMessageTarget({view: 'base', component: 'Dup'})).toBe(document.getElementById('base-dup'));
      expect(findMessageTarget({view: 'report', component: 'Dup'})).toBe(document.getElementById('report-dup'));
    });

    it('finds the component of a dialog of the view', () => {
      expect(findMessageTarget({view: 'report', component: 'Only'})).toBe(document.getElementById('dialog-only'));
    });

    it('finds nothing when the component is only rendered in another view', () => {
      expect(findMessageTarget({view: 'base', component: 'Only'})).toBeNull();
    });
  });
});
