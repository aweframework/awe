import React, {useCallback, useEffect, useRef, useState} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import Translate from '@docusaurus/Translate';
import HomeReveal from '../HomeReveal';
import HomeSectionHead from '../HomeSectionHead';
import styles from './styles.module.css';

/*
 * CLI commands — illustrative code, not UI copy, so they stay untranslated.
 * Keep every line at 52 characters or fewer so the terminal never scrolls.
 */
const ARCHETYPES = [
  {
    id: 'react',
    label: 'React',
    command: `mvn -B archetype:generate \\
  -DarchetypeGroupId=com.almis.awe \\
  -DarchetypeArtifactId=awe-boot-react-archetype \\
  -DarchetypeVersion=4.12.0 \\
  -DgroupId=com.mycompany.app \\
  -DartifactId=my-app -Dversion=1.0-SNAPSHOT`,
  },
  {
    id: 'angular',
    label: 'AngularJS',
    command: `mvn -B archetype:generate \\
  -DarchetypeGroupId=com.almis.awe \\
  -DarchetypeArtifactId=awe-boot-angular-archetype \\
  -DarchetypeVersion=4.12.0 \\
  -DgroupId=com.mycompany.app \\
  -DartifactId=my-app -Dversion=1.0-SNAPSHOT`,
  },
];

function legacyCopy(text) {
  const area = document.createElement('textarea');
  area.value = text;
  area.setAttribute('readonly', '');
  area.style.position = 'fixed';
  area.style.opacity = '0';
  document.body.appendChild(area);
  area.select();
  let copied = false;
  try {
    copied = document.execCommand('copy');
  } catch (error) {
    copied = false;
  }
  document.body.removeChild(area);
  return copied;
}

export default function HomeQuickStart() {
  const [archetype, setArchetype] = useState(0);
  const [copied, setCopied] = useState(false);
  const resetTimer = useRef(null);

  useEffect(
    () => () => {
      if (resetTimer.current) {
        clearTimeout(resetTimer.current);
      }
    },
    [],
  );

  const flagCopied = useCallback(() => {
    setCopied(true);
    if (resetTimer.current) {
      clearTimeout(resetTimer.current);
    }
    resetTimer.current = setTimeout(() => setCopied(false), 2000);
  }, []);

  const onCopy = useCallback(() => {
    const {command} = ARCHETYPES[archetype];
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(command).then(flagCopied, () => {
        if (legacyCopy(command)) {
          flagCopied();
        }
      });
      return;
    }
    if (legacyCopy(command)) {
      flagCopied();
    }
  }, [archetype, flagCopied]);

  return (
    <HomeReveal className={clsx('awe-wrap', styles.block)} id="start">
      <div className={styles.termZone}>
        <div className={styles.intro}>
          <HomeSectionHead
            compact
            title={
              <Translate
                id="homepage.quickstart.title"
                description="Title of the homepage quick start section">
                Up and running in one command
              </Translate>
            }
            description={
              <Translate
                id="homepage.quickstart.description"
                description="Description of the homepage quick start section">
                Generate an AWE project from the Maven archetype — React or
                AngularJS — and start building screens immediately. Hot reload
                included.
              </Translate>
            }
          />
          <Link className={styles.cta} to="/docs/installation">
            <Translate
              id="homepage.quickstart.cta"
              description="Call to action of the homepage quick start section">
              Read the getting-started guide
            </Translate>
          </Link>
        </div>

        <div className={styles.terminal}>
          <div className={styles.termBar}>
            <i aria-hidden="true" />
            <i aria-hidden="true" />
            <i aria-hidden="true" />
            <div className={styles.termTabs} role="tablist">
              {ARCHETYPES.map((item, index) => (
                <button
                  key={item.id}
                  type="button"
                  role="tab"
                  aria-selected={index === archetype}
                  className={styles.termTab}
                  onClick={() => setArchetype(index)}>
                  {item.label}
                </button>
              ))}
            </div>
            <button
              type="button"
              className={clsx(styles.copyBtn, copied && styles.copyBtnDone)}
              onClick={onCopy}>
              {copied ? (
                <Translate
                  id="homepage.quickstart.copied"
                  description="Copy button label right after the command was copied">
                  Copied ✓
                </Translate>
              ) : (
                <Translate
                  id="homepage.quickstart.copy"
                  description="Copy button label of the homepage terminal">
                  Copy
                </Translate>
              )}
            </button>
          </div>

          <div className={styles.termBody}>
            <div className={styles.cmdStack}>
              {ARCHETYPES.map((item, index) => (
                <pre
                  key={item.id}
                  className={clsx(
                    styles.cmd,
                    index === archetype && styles.cmdActive,
                  )}>
                  <span className={styles.prompt}>$</span>{' '}
                  <code>{item.command}</code>
                </pre>
              ))}
            </div>
            <div className={styles.dim}>
              [INFO] BUILD SUCCESS — cd my-app &amp;&amp; npm start
            </div>
          </div>
        </div>
      </div>
    </HomeReveal>
  );
}
