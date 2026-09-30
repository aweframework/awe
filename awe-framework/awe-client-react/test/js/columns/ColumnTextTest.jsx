import React from 'react';
import {fireEvent, screen, cleanup} from '@testing-library/react';
import ColumnText from '../../../src/columns/ColumnText';
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnTextTest.jsx', () => {
  let props;

  beforeEach(() => {
    props = {
      component: 'text',
      model: {values: []},
      numberFormat: {},
      address: {component: 'text', view: 'report', column: 'column', row: 'row'},
      settings: {},
      placeholder: 'placeholder',
      label: 'Label',
      required: true,
      readonly: false,
      data: {
        value: 'initial',
        error: ''
      },
      attrs: {}
    };
  });

  afterEach(() => {
    cleanup();
  });

  it('renderiza el input con valor inicial', () => {
    renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    expect(input).not.toBeNull();
    expect(input.required).toBe(true);
  });

  /*it('llama a updateModelWithDependencies al hacer blur tras cambio', () => {
    const {dispatchSpy} = renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    input.focus();
    fireEvent.change(input, {target: {value: 'nuevo valor'}});
    fireEvent.blur(input);

    expect(dispatchSpy).toHaveBeenCalled();
  });

  it('almacena valor al pulsar Enter', () => {
    const {dispatchSpy} = renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    input.focus();
    fireEvent.change(input, {target: {value: 'nuevo valor 2'}});
    fireEvent.keyDown(input, {key: 'Enter', code: 'Enter'});

    expect(dispatchSpy).toHaveBeenCalledWith();
  });

  it('no actualiza modelo si no ha cambiado el valor', () => {
    const {dispatchSpy} = renderWithProviders(<ColumnText {...props} />);
    const input = screen.getByDisplayValue('initial');
    input.focus();
    fireEvent.blur(input);

    expect(dispatchSpy).not.toHaveBeenCalled();
  });*/

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
    expect(input.disabled).toBe(true);
  });
});
