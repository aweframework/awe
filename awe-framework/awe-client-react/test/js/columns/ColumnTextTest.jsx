import React from 'react';
import {fireEvent, screen, cleanup} from '@testing-library/react';
import ColumnText from '../../../src/columns/ColumnText';
import {renderWithProviders} from "../test-utils"; // Ajusta el path si es necesario

describe('awe-react-client/test/js/columns/ColumnTextTest.jsx', () => {
  let props;

  beforeEach(() => {
    props = {
      component: 'text',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModelWithDependencies"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'text', view: 'report', column: 'column', row: 'row'},
      t: (key) => key,
      settings: {},
      placeholder: 'placeholder',
      label: 'Label',
      required: true,
      readonly: false,
      data: {
        value: 'initial',
        error: ''
      }
    };
  });

  afterEach(() => {
    cleanup();
  });

  it('renderiza el input con valor inicial', () => {
    renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    expect(input).not.toBeNull();
    expect(input.required).toBeTrue();
  });

  it('llama a updateModelWithDependencies al hacer blur tras cambio', () => {
    renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    input.focus();
    fireEvent.change(input, {target: {value: 'nuevo valor'}});
    fireEvent.blur(input);

    expect(props.updateModelWithDependencies).toHaveBeenCalledWith(props.address, {values: 'nuevo valor'});
  });

  it('almacena valor al pulsar Enter', () => {
    renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    input.focus();
    fireEvent.change(input, {target: {value: 'nuevo valor 2'}});
    fireEvent.keyDown(input, {key: 'Enter', code: 'Enter'});

    expect(props.updateModelWithDependencies).toHaveBeenCalledWith(props.address, {values: 'nuevo valor 2'});
  });

  it('no actualiza modelo si no ha cambiado el valor', () => {
    renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    input.focus();
    fireEvent.blur(input);

    expect(props.updateModelWithDependencies).not.toHaveBeenCalled();
  });

  it('actualiza el valor desde props si cambia externamente y no se está escribiendo', () => {
    const {rerender} = renderWithProviders(<ColumnText {...props} />);
    expect(screen.getByDisplayValue('initial')).not.toBeNull();

    props.data.value = 'externo';
    rerender(<ColumnText {...props} />);

    expect(screen.getByDisplayValue('externo')).not.toBeNull();
  });

  it('no sobrescribe valor si se está escribiendo', () => {
    const {rerender} = renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    input.focus();
    fireEvent.change(input, {target: {value: 'escribiendo...'}});

    props.data.value = 'cambio externo';
    rerender(<ColumnText {...props} />);

    expect(screen.getByDisplayValue('escribiendo...')).not.toBeNull();
  });

  it('no rompe si hay error', () => {
    props.data.error = 'Campo obligatorio';
    renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    expect(input).not.toBeNull();
  });

  it('desactiva el input si es readonly', () => {
    props.readonly = true;
    renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    expect(input.disabled).toBeTrue();
  });
});
