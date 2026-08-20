import React from 'react';
import Translate from '@docusaurus/Translate';
import styles from './styles.module.css';

const IconGrid = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <path d="M3 9h18M9 21V9" />
  </svg>
);

const IconDatabase = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
    <ellipse cx="12" cy="5" rx="9" ry="3" />
    <path d="M21 5v14a9 3 0 0 1-18 0V5" />
    <path d="M3 12a9 3 0 0 0 18 0" />
  </svg>
);

const IconLayers = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
    <path d="M12 2 2 7l10 5 10-5-10-5z" />
    <path d="m2 17 10 5 10-5" />
    <path d="m2 12 10 5 10-5" />
  </svg>
);

const IconGlobe = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
    <circle cx="12" cy="12" r="10" />
    <path d="M2 12h20M12 2a15 15 0 0 1 0 20 15 15 0 0 1 0-20z" />
  </svg>
);

const IconServer = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
    <rect x="2" y="4" width="20" height="8" rx="2" />
    <rect x="2" y="14" width="20" height="6" rx="2" />
    <path d="M6 8h.01M6 17h.01" />
  </svg>
);

export default function HomeStatsStrip() {
  const stats = [
    {
      key: 'components',
      Icon: IconGrid,
      label: (
        <Translate
          id="homepage.stats.components.label"
          description="Stats strip: rich components label">
          Rich components
        </Translate>
      ),
      caption: (
        <Translate
          id="homepage.stats.components.caption"
          description="Stats strip: rich components caption">
          PrimeReact-based
        </Translate>
      ),
    },
    {
      key: 'databases',
      Icon: IconDatabase,
      label: (
        <Translate
          id="homepage.stats.databases.label"
          description="Stats strip: databases label">
          Any SQL database
        </Translate>
      ),
      caption: (
        <Translate
          id="homepage.stats.databases.caption"
          description="Stats strip: databases caption">
          6 engines supported
        </Translate>
      ),
    },
    {
      key: 'frontends',
      Icon: IconLayers,
      label: (
        <Translate
          id="homepage.stats.frontends.label"
          description="Stats strip: frontends label">
          Two frontends
        </Translate>
      ),
      caption: (
        <Translate
          id="homepage.stats.frontends.caption"
          description="Stats strip: frontends caption">
          React · AngularJS
        </Translate>
      ),
    },
    {
      key: 'i18n',
      Icon: IconGlobe,
      label: (
        <Translate
          id="homepage.stats.i18n.label"
          description="Stats strip: multi-language label">
          Multi-language
        </Translate>
      ),
      caption: (
        <Translate
          id="homepage.stats.i18n.caption"
          description="Stats strip: multi-language caption">
          extensible locales
        </Translate>
      ),
    },
    {
      key: 'backend',
      Icon: IconServer,
      label: (
        <Translate
          id="homepage.stats.backend.label"
          description="Stats strip: backend label">
          Spring Boot
        </Translate>
      ),
      caption: (
        <Translate
          id="homepage.stats.backend.caption"
          description="Stats strip: backend caption">
          Java backend
        </Translate>
      ),
    },
  ];

  return (
    <div className={styles.stats}>
      <div className={`awe-wrap ${styles.statsGrid}`}>
        {stats.map(({key, Icon, label, caption}) => (
          <div key={key} className={styles.stat}>
            <Icon />
            <b>{label}</b>
            <span>{caption}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
