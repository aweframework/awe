import React, {useEffect, useState} from 'react';
import clsx from 'clsx';
import Translate, {translate} from '@docusaurus/Translate';
import styles from './styles.module.css';

const CYCLE_MS = 5200;

/*
 * Illustrative AWE definition files. These are code samples, not UI copy, so
 * they are intentionally not translated. Keep every line at 52 characters or
 * fewer: the code column is roughly 423px wide and a monospace glyph is about
 * 7.15px, so only ~54 characters fit before the panel scrolls.
 */
const SCREEN_XML = `<screen template="full" label="MENU_USERS">
  <tag source="center">
    <window label="SCREEN_USERS" icon="users">
      <criteria id="usrNam" component="text"
        label="PARAM_NAME"/>
      <criteria id="sta" component="select"
        initial-load="query"
        target-action="StaSel"/>
      <grid id="GrdUsrLst"
        initial-load="query" target-action="UsrLst">
        <column name="usr" label="COL_USER"/>
        <column name="nam" label="COL_NAME"/>
        <column name="sta" label="COL_STATUS"/>
      </grid>
      <button label="BUTTON_SEARCH" icon="search">
        <button-action type="filter"
          target="GrdUsrLst"/>
      </button>
    </window>
  </tag>
</screen>`;

const QUERY_XML = `<!-- Feeds GrdUsrLst — no controller code -->
<query id="UsrLst">
  <table name="AweUsr"/>
  <field id="usr" alias="usr"/>
  <field id="nam" alias="nam"/>
  <field id="sta" alias="sta"/>
  <where>
    <filter left-field="nam" condition="like"
      right-variable="name" optional="true"/>
  </where>
  <variable id="name" type="STRINGB"
    name="usrNam"/>
</query>

<!-- Same engine reads SQL, NoSQL, REST -->`;

const MAINTAIN_XML = `<!-- Insert, update, delete with audit -->
<maintain id="UsrNew">
  <insert audit="HisAweUsr">
    <table name="AweUsr"/>
    <field id="usr" variable="usr"/>
    <field id="nam" variable="nam"/>
    <field id="sta" variable="sta"/>
    <variable id="usr" type="STRING" name="usr"/>
    <variable id="nam" type="STRING" name="nam"/>
  </insert>
</maintain>

<!-- Transactions and locking are handled -->
<!-- by the engine — no DAO code needed -->`;

const TABS = [
  {id: 'screen', file: 'screen.xml', source: SCREEN_XML},
  {id: 'query', file: 'query.xml', source: QUERY_XML},
  {id: 'maintain', file: 'maintain.xml', source: MAINTAIN_XML},
];

/*
 * Minimal XML tokenizer. It runs during render and only touches the input
 * string, so it is safe on the server.
 *   1. <!-- comment -->
 *   2. opening/closing bracket + tag name
 *   3. > or />
 *   4. attribute = "value"
 */
const XML_TOKENIZER =
  /(<!--[\s\S]*?-->)|(<\/?)([A-Za-z][\w.:-]*)|(\/?>)|([A-Za-z][\w.:-]*)(=)("[^"]*")/g;

function highlightXml(source) {
  const nodes = [];
  let cursor = 0;
  let key = 0;
  let match;

  XML_TOKENIZER.lastIndex = 0;
  // eslint-disable-next-line no-cond-assign
  while ((match = XML_TOKENIZER.exec(source)) !== null) {
    if (match.index > cursor) {
      nodes.push(source.slice(cursor, match.index));
    }

    if (match[1]) {
      nodes.push(
        <span key={(key += 1)} className={styles.xComment}>
          {match[1]}
        </span>,
      );
    } else if (match[3]) {
      nodes.push(
        <span key={(key += 1)} className={styles.xPunct}>
          {match[2]}
        </span>,
        <span key={(key += 1)} className={styles.xTag}>
          {match[3]}
        </span>,
      );
    } else if (match[4]) {
      nodes.push(
        <span key={(key += 1)} className={styles.xPunct}>
          {match[4]}
        </span>,
      );
    } else {
      nodes.push(
        <span key={(key += 1)} className={styles.xAttr}>
          {match[5]}
        </span>,
        <span key={(key += 1)} className={styles.xPunct}>
          {match[6]}
        </span>,
        <span key={(key += 1)} className={styles.xValue}>
          {match[7]}
        </span>,
      );
    }

    cursor = match.index + match[0].length;
  }

  if (cursor < source.length) {
    nodes.push(source.slice(cursor));
  }

  return nodes;
}

const HIGHLIGHTED = TABS.map((tab) => highlightXml(tab.source));

/* Mock grid data — illustrative sample data, not UI copy. */
const MOCK_ROWS = [
  {user: 'jdoe', name: 'Jane Doe', active: true},
  {user: 'mgarcia', name: 'María García', active: true},
  {user: 'tsmith', name: 'Tom Smith', active: false},
  {user: 'lchen', name: 'Li Chen', active: true},
];

export default function HomeXmlToUiDemo() {
  const [activeTab, setActiveTab] = useState(0);
  const [autoCycle, setAutoCycle] = useState(true);
  const [paused, setPaused] = useState(false);
  const [reducedMotion, setReducedMotion] = useState(false);
  const [reactEngine, setReactEngine] = useState(true);

  useEffect(() => {
    const query = window.matchMedia('(prefers-reduced-motion: reduce)');
    setReducedMotion(query.matches);

    const onChange = (event) => setReducedMotion(event.matches);
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  const cycling = autoCycle && !paused && !reducedMotion;

  const activeLabel = (
    <Translate
      id="homepage.demo.status.active"
      description="Active status value in the mock AWE screen">
      Active
    </Translate>
  );

  useEffect(() => {
    if (!cycling) {
      return undefined;
    }
    const timer = setInterval(() => {
      setActiveTab((current) => (current + 1) % TABS.length);
    }, CYCLE_MS);
    return () => clearInterval(timer);
    // `activeTab` restarts the interval so the progress bar stays in sync.
  }, [cycling, activeTab]);

  const selectTab = (index) => {
    setActiveTab(index);
    setAutoCycle(false);
  };

  return (
    <div className={styles.demoZone} id="demo">
      <div className={styles.demoGlow} aria-hidden="true" />
      <div
        className={styles.demo}
        onMouseEnter={() => setPaused(true)}
        onMouseLeave={() => setPaused(false)}>
        <div className={styles.codeSide}>
          <div
            className={styles.codeTabs}
            role="tablist"
            aria-label={translate({
              id: 'homepage.demo.tablist.label',
              description:
                'Accessible label of the AWE definition files tab list',
              message: 'AWE definition files',
            })}>
            {TABS.map((tab, index) => (
              <button
                key={tab.id}
                type="button"
                role="tab"
                id={`awe-demo-tab-${tab.id}`}
                aria-controls={`awe-demo-panel-${tab.id}`}
                aria-selected={index === activeTab}
                className={styles.codeTab}
                onClick={() => selectTab(index)}>
                {tab.file}
              </button>
            ))}
          </div>

          <div className={styles.codeStack}>
            {TABS.map((tab, index) => (
              <pre
                key={tab.id}
                id={`awe-demo-panel-${tab.id}`}
                role="tabpanel"
                aria-labelledby={`awe-demo-tab-${tab.id}`}
                className={clsx(
                  styles.codePanel,
                  index === activeTab && styles.codePanelActive,
                )}>
                <code>{HIGHLIGHTED[index]}</code>
              </pre>
            ))}
          </div>

          <div
            className={clsx(styles.progress, cycling && styles.progressRunning)}
            aria-hidden="true">
            <div key={activeTab} className={styles.progressBar} />
          </div>
        </div>

        <div className={styles.renderSide}>
          <div className={styles.renderHead}>
            <span className={styles.renderLabel}>
              <Translate
                id="homepage.demo.rendered.label"
                description="Label above the mock AWE screen in the homepage demo">
                Rendered output
              </Translate>
            </span>
            <div
              className={styles.engineToggle}
              role="group"
              aria-label={translate({
                id: 'homepage.demo.engine.label',
                description: 'Accessible label of the rendering engine toggle',
                message: 'Rendering engine',
              })}>
              <button
                type="button"
                aria-pressed={reactEngine}
                onClick={() => setReactEngine(true)}>
                React
              </button>
              <button
                type="button"
                aria-pressed={!reactEngine}
                onClick={() => setReactEngine(false)}>
                AngularJS
              </button>
            </div>
          </div>

          <div className={styles.renderBody}>
            <div
              className={clsx(
                styles.aweWindow,
                activeTab === 0 && styles.spotWindow,
              )}>
              <div className={styles.aweWindowTitle}>
                <span className={styles.dot} aria-hidden="true" />
                <Translate
                  id="homepage.demo.window.title"
                  description="Title of the mock AWE window in the homepage demo">
                  Users
                </Translate>
              </div>

              <div className={styles.aweCriteria}>
                <div className={styles.aweField}>
                  <span className={styles.aweFieldLabel}>
                    <Translate
                      id="homepage.demo.criteria.name"
                      description="Name criterion label in the mock AWE screen">
                      Name
                    </Translate>
                  </span>
                  <div className={styles.aweBox}>jane…</div>
                </div>
                <div className={styles.aweField}>
                  <span className={styles.aweFieldLabel}>
                    <Translate
                      id="homepage.demo.criteria.status"
                      description="Status criterion label in the mock AWE screen">
                      Status
                    </Translate>
                  </span>
                  <div className={styles.aweBox}>
                    {activeLabel}
                    <span className={styles.caret} aria-hidden="true">
                      ▼
                    </span>
                  </div>
                </div>
              </div>

              <div
                className={clsx(
                  styles.aweGrid,
                  activeTab === 1 && styles.spotGrid,
                )}>
                <table>
                  <thead>
                    <tr>
                      <th>User</th>
                      <th>Name</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {MOCK_ROWS.map((row) => (
                      <tr key={row.user}>
                        <td>{row.user}</td>
                        <td>{row.name}</td>
                        <td>
                          <span
                            className={clsx(
                              styles.pill,
                              row.active ? styles.pillOn : styles.pillOff,
                            )}>
                            {row.active ? (
                              activeLabel
                            ) : (
                              <Translate
                                id="homepage.demo.status.inactive"
                                description="Inactive status value in the mock AWE screen">
                                Inactive
                              </Translate>
                            )}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className={styles.aweActions}>
                <button
                  type="button"
                  className={clsx(styles.aweBtn, styles.aweBtnPrimary)}>
                  <Translate
                    id="homepage.demo.button.search"
                    description="Search button of the mock AWE screen">
                    Search
                  </Translate>
                </button>
                <button
                  type="button"
                  className={clsx(
                    styles.aweBtn,
                    activeTab === 2 && styles.spotButton,
                  )}>
                  <Translate
                    id="homepage.demo.button.new"
                    description="New button of the mock AWE screen">
                    New
                  </Translate>
                </button>
              </div>
            </div>
          </div>

          <div className={styles.renderFoot}>
            {reactEngine ? (
              <Translate
                id="homepage.demo.footer.react"
                description="Caption below the demo when the React engine is selected"
                values={{
                  client: <em className={styles.footClient}>awe-react-client</em>,
                }}>
                {'Same XML — rendered with {client}'}
              </Translate>
            ) : (
              <Translate
                id="homepage.demo.footer.angular"
                description="Caption below the demo when the AngularJS engine is selected"
                values={{
                  client: (
                    <em className={styles.footClient}>awe-client-angular</em>
                  ),
                }}>
                {'Same XML — rendered with {client} (stable / legacy)'}
              </Translate>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
