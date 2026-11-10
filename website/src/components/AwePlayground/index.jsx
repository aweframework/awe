import React, {useId, useMemo, useState} from 'react';
import clsx from 'clsx';
import {useColorMode} from '@docusaurus/theme-common';
import CodeBlock from '@theme/CodeBlock';
import Link from '@docusaurus/Link';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import Translate, {translate} from '@docusaurus/Translate';
import AweLive from '../AweLive';
import Control from './Controls';
import {
  actionKey, controlKey, eventKey, initialValues, optionKey, optionValues, resolveValues, toScene, toXml, visibleControls,
} from './playgroundModel';
import styles from './styles.module.css';

// Text of the specs is English: the ids of code.json translate it
const label = (spec, control) => translate({id: controlKey(spec, control), message: control.label});
const optionLabel = (spec, control) => option =>
  translate({id: optionKey(spec, control, option), message: String(option.label)});

function StageModeToggle({mode, onChange}) {
  const name = useId();
  const modes = [
    {id: 'light', text: translate({id: 'playground.stage.light', message: 'Light'})},
    {id: 'dark', text: translate({id: 'playground.stage.dark', message: 'Dark'})},
  ];
  return (
    <div className={styles.modeToggle} role="radiogroup" aria-label={translate({id: 'playground.stage.mode', message: 'Preview colour mode'})}>
      {modes.map(item => (
        <label key={item.id} className={styles.modeOption}>
          <input type="radio" name={name} className={styles.visuallyHidden} checked={mode === item.id} onChange={() => onChange(item.id)} />
          <span>{item.text}</span>
        </label>
      ))}
    </div>
  );
}

function ReferenceTabs({spec}) {
  const xsd = `/reference/${spec.reference.page}#element-${spec.reference.element}`;
  return (
    <Tabs className={styles.tabs}>
      <TabItem value="attributes" label={translate({id: 'playground.tab.attributes', message: 'Attributes'})} default>
        <p>
          <Translate id="playground.attributes.intro" description="Playground, attributes tab: intro">
            The controls above write these attributes. The reference lists every attribute, its type and default.
          </Translate>
          {' '}
          <Link to={xsd}>
            <Translate id="playground.attributes.link" description="Playground, attributes tab: link to the XSD reference">
              Attributes in the XSD reference
            </Translate>
          </Link>
        </p>
        <table>
          <thead>
            <tr>
              <th><Translate id="playground.attributes.col.attribute" description="Playground, attributes table header">Attribute</Translate></th>
              <th><Translate id="playground.attributes.col.control" description="Playground, attributes table header">Control</Translate></th>
              <th><Translate id="playground.attributes.col.values" description="Playground, attributes table header">Values</Translate></th>
            </tr>
          </thead>
          <tbody>
            {spec.controls.map(control => (
              <tr key={control.id}>
                <td><code>{control.xml ?? control.writes ?? 'dependency'}</code></td>
                <td>{label(spec, control)}</td>
                <td>
                  {control.type === 'switch' || control.type === 'text'
                    ? (control.type === 'switch' ? 'true / false' : 'text')
                    : optionValues(control).filter(Boolean).map(option => <code key={option} className={styles.value}>{option}</code>)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TabItem>
      {(spec.events.length > 0 || spec.actions.length > 0) && (
      <TabItem value="events" label={translate({id: 'playground.tab.events', message: 'Events and actions'})}>
        {spec.events.length > 0 && (
        <table>
          <thead>
            <tr>
              <th><Translate id="playground.events.col.event" description="Playground, events table header">Event</Translate></th>
              <th><Translate id="playground.events.col.description" description="Playground, events table header">Description</Translate></th>
            </tr>
          </thead>
          <tbody>
            {spec.events.map(event => (
              <tr key={event.name}>
                <td><code>{event.name}</code></td>
                <td>{translate({id: eventKey(spec, event), message: event.description})}</td>
              </tr>
            ))}
          </tbody>
        </table>
        )}
        {spec.actions.length > 0 && (
        <table>
          <thead>
            <tr>
              <th><Translate id="playground.actions.col.action" description="Playground, actions table header">Action (type)</Translate></th>
              <th><Translate id="playground.actions.col.description" description="Playground, actions table header">What it does</Translate></th>
            </tr>
          </thead>
          <tbody>
            {spec.actions.map(action => (
              <tr key={action.name}>
                <td><code>{action.name}</code></td>
                <td>{translate({id: actionKey(spec, action), message: action.description})}</td>
              </tr>
            ))}
          </tbody>
        </table>
        )}
      </TabItem>
      )}
      <TabItem value="reference" label={translate({id: 'playground.tab.reference', message: 'Full reference'})}>
        <ul>
          <li>
            <Link to={spec.api}>
              <Translate id="playground.reference.api" description="Playground, reference tab: link to the API page">
                Full documentation of this component
              </Translate>
            </Link>
          </li>
          <li>
            <Link to={xsd}>
              <Translate id="playground.reference.xsd" description="Playground, reference tab: link to the XSD reference">
                XSD reference of the element
              </Translate>
            </Link>
          </li>
        </ul>
      </TabItem>
    </Tabs>
  );
}

/**
 * Live playground of an AWE component: the real React component, controls generated from the spec and the XML they
 * write. `spec` is described in playgroundModel.js. The stage follows the colour mode of the site until the reader
 * picks one; the AWE tokens read `color-scheme` through light-dark(), so setting it on the stage is all it takes.
 */
export default function AwePlayground({spec}) {
  const {colorMode} = useColorMode();
  const [stageMode, setStageMode] = useState(null);
  const [values, setValues] = useState(() => initialValues(spec));
  const resolved = useMemo(() => resolveValues(spec, values), [spec, values]);
  const xml = useMemo(() => toXml(spec, resolved), [spec, resolved]);
  const scene = useMemo(() => toScene(spec, resolved), [spec, resolved]);
  const scheme = stageMode ?? colorMode;
  const noneLabel = translate({id: 'playground.icon.none', message: 'No icon'});

  return (
    <div className={styles.wrap}>
      <div className={styles.glow} aria-hidden="true" />
      <div className={clsx(styles.playground, spec.layout === 'stacked' && styles.stacked)}>
        <div className={styles.stage} style={{colorScheme: scheme}}>
          <div className={styles.stageBar}>
            <span className={styles.stageTitle}>
              <Translate id="playground.stage.title" description="Playground: title of the live preview">Live preview</Translate>
            </span>
            <StageModeToggle mode={scheme} onChange={setStageMode} />
          </div>
          <div className={clsx(styles.stageBody, spec.stage === 'wide' && styles.stageBodyWide, spec.layout === 'stacked' && styles.stageBodyData)}>
            <AweLive bare colorScheme={scheme} components={scene.components} nodes={scene.nodes} files={scene.files} options={scene.options} uploads={scene.uploads} messages={scene.messages} answers={scene.answers} settings={scene.settings} height={spec.height} />
          </div>
        </div>
        <aside className={styles.controls} aria-label={translate({id: 'playground.controls.title', message: 'Controls'})}>
          <div className={styles.controlsHead}>
            <h3 className={styles.controlsTitle}>
              <Translate id="playground.controls.title" description="Playground: title of the controls panel">Controls</Translate>
            </h3>
            <button type="button" className={styles.reset} onClick={() => setValues(initialValues(spec))}>
              <Translate id="playground.controls.reset" description="Playground: restores the initial values">Reset</Translate>
            </button>
          </div>
          {visibleControls(spec, resolved).map(control => (
            <Control
              key={control.id}
              control={control}
              value={resolved[control.id]}
              onChange={value => setValues(current => ({...current, [control.id]: value}))}
              label={label(spec, control)}
              optionLabel={optionLabel(spec, control)}
              noneLabel={noneLabel}
            />
          ))}
        </aside>
        <div className={styles.xml}>
          <div className={styles.xmlTitle}>XML</div>
          <CodeBlock language="xml">{xml}</CodeBlock>
        </div>
      </div>
      <ReferenceTabs spec={spec} />
    </div>
  );
}
