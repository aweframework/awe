---
id: wizard-and-wizard-panel
title: Asistente y panel de asistente
---

## Introducción {#introduction}

Un **wizard** o **asistente** es un componente de pantalla muy útil para guiar al usuario a través de un grupo ordenado de pantallas llamadas *pasos*:

<img alt="Asistente" src={require('@docusaurus/useBaseUrl').default('img/Wizard.png')} />

## Esqueleto XML {#xml-skeleton}

La estructura del asistente es muy similar a la estructura de [pestañas y contenedor de pestañas](tab-and-tabcontainer.md):

```xml
<wizard id="[wizard-identifier]" initial-load="[initial-load]" target-action="[target-action]" label="[wizard-step-label]">
  <wizard-panel id="[panel-identifier]">
     ...
  </wizard-panel>
  <wizard-panel id="[panel-identifier]">
     ...
  </wizard-panel>
  ...
</wizard>
```

La lista del asistente se rellena con los campos `value` y `label` de la consulta/enumerado lanzado con `[target-action]`.

## Estructura del asistente {#wizard-structure}

```xml
<wizard id="[wizard-identifier]" initial-load="[initial-load]" target-action="[target-action]" label="[wizard-step-label]">
   ...
</wizard>
```

### Atributos del asistente {#wizard-attributes}

| Atributo      | Uso             | Tipo   | Descripción                                                                           | Valores                                                                                                  |
|---------------|-----------------|--------|---------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| id            | **Obligatorio** | String | Identificador del asistente. Con fines de referencia                                  |                                                                                                          |
| label         | Opcional        | String | Texto del paso del asistente (sin el número)                                          | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)                            |
| style         | Opcional        | String | Clases CSS del asistente                                                              |                                                                                                          |
| initial-load  | Opcional        | String | Llamada de acción al servidor para cargar los pasos del asistente (se lanza al generar la ventana) | `enum` (para [enumerado](enumerate-definition.md)), `query` (para [llamada de consulta](query-definition.md)) |
| target-action | Opcional        | String | Destino a llamar en el servidor                                                       |                                                                                                          |
| size          | Opcional        | String | Tamaño del asistente                                                                  | `sm` (por defecto), `md` o `lg`.                                                                         |
| help          | Opcional        | String | Texto de ayuda del criterio                                                           | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)                            |
| help-image    | Opcional        | String | Imagen de ayuda del criterio                                                          | Esto **debe** ser una ruta de imagen                                                                     |
| orientation   | Opcional        | String | Orientación de los pasos del asistente                                                | El valor por defecto es `vertical`                                                                       |

## Estructura del panel de asistente {#wizard-panel-structure}

Un panel de asistente es una ventana que se abre cuando se selecciona una etapa del asistente. Ten en cuenta que `[panel-identifier]` debe coincidir con el valor
de la consulta/lista enumerada indicada en el elemento `Wizard`.

```xml
<wizard-panel id="[panel-identifier]">
  ...
</wizard-panel>
```

### Atributos del panel de asistente {#wizard-panel-attributes}

| Atributo   | Uso             | Tipo   | Descripción                                                  | Valores                                                                       |
|------------|-----------------|--------|--------------------------------------------------------------|-------------------------------------------------------------------------------|
| id         | **Obligatorio** | String | Identificador del panel. Debe ser igual que los valores destino |                                                                            |
| style      | Opcional        | String | Clases CSS del panel                                         |                                                                               |
| help       | Opcional        | String | Texto de ayuda del criterio                                  | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) |
| help-image | Opcional        | String | Imagen de ayuda del criterio                                 | Esto **debe** ser una ruta de imagen                                          |

## Ejemplos {#examples}

* **Asistente con 4 pasos y validación en cada paso**

```xml
<wizard id="wizardTest" initial-load="enum" target-action="WizTst" label="SCREEN_TEXT_STEP">
  <wizard-panel id="WizardStep1">
    <tag type="div" style="fullHeight" expandible="vertical">
      <tag type="div" style="panel-body expand">
        ...
      </tag>
      <tag type="div" style="panel-footer">
        <tag type="div" style="pull-right">
          <button label="BUTTON_NEXT" icon="chevron-circle-right" id="FwStep2" style="btn-primary">
            <button-action type="validate" target="WizardStep1"/>
            <button-action type="next-step" target="wizardTest"/>
          </button>
        </tag>
      </tag>
    </tag>
  </wizard-panel>
  <wizard-panel id="WizardStep2">
    <tag type="div" style="fullHeight" expandible="vertical">
      <tag type="div" style="panel-body expand">
        ...
      </tag>
      <tag type="div" style="panel-footer">
        <tag type="div" style="pull-right">
          <button label="BUTTON_PREVIOUS" icon="chevron-circle-left" id="BkStep1">
            <button-action type="prev-step" target="wizardTest"/>
          </button>
          <button label="BUTTON_NEXT" icon="chevron-circle-right" id="FwStep3" style="btn-primary">
            <button-action type="validate" target="WizardStep2"/>
            <button-action type="next-step" target="wizardTest"/>
          </button>
        </tag>
      </tag>
    </tag>
  </wizard-panel>
  <wizard-panel id="WizardStep3">
    <tag type="div" style="fullHeight" expandible="vertical">
      <tag type="div" style="panel-body expand">
        ...
      </tag>
      <tag type="div" style="panel-footer">
        <tag type="div" style="pull-right">
          <button label="BUTTON_PREVIOUS" icon="chevron-circle-left" id="BkStep2">
            <button-action type="prev-step" target="wizardTest"/>
          </button>
          <button label="BUTTON_NEXT" icon="chevron-circle-right" id="FwStep4" style="btn-primary">
            <button-action type="validate" target="WizardStep3"/>
            <button-action type="next-step" target="wizardTest"/> 
          </button>
        </tag>
      </tag>
    </tag>
  </wizard-panel>
  <wizard-panel id="WizardStep4">
    <tag type="div" style="fullHeight" expandible="vertical">
      <tag type="div" style="panel-body expand">
        ...
      </tag>
      <tag type="div" style="panel-footer">
        <tag type="div" style="pull-right">
          <button label="BUTTON_PREVIOUS" icon="chevron-circle-left" id="BkStep3">
            <button-action type="prev-step" target="wizardTest"/>
          </button>
          <button label="BUTTON_FINISH" icon="check" id="Finish" style="btn-primary">
            <button-action type="validate" target="WizardStep4"/>
          </button>
        </tag>
      </tag>
    </tag>
  </wizard-panel>
</wizard>
```

Lo cual generará el siguiente asistente:

<img alt="Asistente" src={require('@docusaurus/useBaseUrl').default('img/Wizard.png')} />
