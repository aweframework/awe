import React from 'react';
import clsx from 'clsx';
import Translate from '@docusaurus/Translate';
import HomeReveal from '../HomeReveal';
import HomeSectionHead from '../HomeSectionHead';
import styles from './styles.module.css';

const IconFrame = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
  </svg>
);

const IconDatabase = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <ellipse cx="12" cy="5" rx="9" ry="3" />
    <path d="M21 5v14a9 3 0 0 1-18 0V5" />
    <path d="M3 12a9 3 0 0 0 18 0" />
  </svg>
);

const IconPlug = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M12 2v4m0 12v4M2 12h4m12 0h4" />
    <circle cx="12" cy="12" r="4" />
  </svg>
);

const IconGlobe = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <circle cx="12" cy="12" r="10" />
    <path d="M2 12h20M12 2a15 15 0 0 1 0 20 15 15 0 0 1 0-20z" />
  </svg>
);

const IconGrid = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <path d="M3 9h18M9 21V9" />
  </svg>
);

const IconShield = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
  </svg>
);

export default function HomeBentoFeatures() {
  return (
    <HomeReveal className={clsx('awe-wrap', styles.block)}>
      <HomeSectionHead
        title={
          <Translate
            id="homepage.bento.title"
            description="Title of the homepage feature grid">
            Built for enterprise applications
          </Translate>
        }
        description={
          <Translate
            id="homepage.bento.description"
            description="Description of the homepage feature grid">
            Everything a data-heavy internal platform needs, out of the box.
          </Translate>
        }
      />

      <div className={styles.bento}>
        <div className={clsx(styles.cell, styles.span2)}>
          <div
            className={clsx(styles.iconBadge, styles.badgeBlue)}
            aria-hidden="true">
            <IconFrame />
          </div>
          <h3>
            <Translate
              id="homepage.bento.frontends.title"
              description="Bento cell: one XML two frontends, title">
              One XML, two frontends
            </Translate>
          </h3>
          <p>
            <Translate
              id="homepage.bento.frontends.body"
              description="Bento cell: one XML two frontends, body">
              The same screen definition renders with the modern React client or
              the stable AngularJS client. Migrate engine by engine — your XML
              never changes.
            </Translate>
          </p>
          <div className={styles.tagRow}>
            <span className={clsx(styles.chip, styles.chipHot)}>
              awe-react-client
            </span>
            <span className={styles.chip}>awe-client-angular</span>
          </div>
        </div>

        <div className={clsx(styles.cell, styles.span2)}>
          <div
            className={clsx(styles.iconBadge, styles.badgeGreen)}
            aria-hidden="true">
            <IconDatabase />
          </div>
          <h3>
            <Translate
              id="homepage.bento.engine.title"
              description="Bento cell: query and maintain engine, title">
              Query and maintain engine
            </Translate>
          </h3>
          <p>
            <Translate
              id="homepage.bento.engine.body"
              description="Bento cell: query and maintain engine, body">
              Declarative reads and transactional writes with auditing, locking
              and pagination — across SQL, NoSQL, REST and JavaBeans.
            </Translate>
          </p>
          <div className={styles.miniCode}>
            {'<query id="UsrLst"> … </query>\n<maintain id="UsrNew"> … </maintain>'}
          </div>
        </div>

        <div className={styles.cell}>
          <div
            className={clsx(styles.iconBadge, styles.badgePurple)}
            aria-hidden="true">
            <IconPlug />
          </div>
          <h3>
            <Translate
              id="homepage.bento.starters.title"
              description="Bento cell: pluggable starters, title">
              Pluggable starters
            </Translate>
          </h3>
          <p>
            <Translate
              id="homepage.bento.starters.body"
              description="Bento cell: pluggable starters, body">
              Spring Boot starters enable exactly the modules you need —
              scheduler, notifications, printing, REST.
            </Translate>
          </p>
        </div>

        <div className={styles.cell}>
          <div
            className={clsx(styles.iconBadge, styles.badgeBlue)}
            aria-hidden="true">
            <IconGlobe />
          </div>
          <h3>
            <Translate
              id="homepage.bento.theming.title"
              description="Bento cell: i18n and themes, title">
              i18n and themes
            </Translate>
          </h3>
          <p>
            <Translate
              id="homepage.bento.theming.body"
              description="Bento cell: i18n and themes, body">
              English, Spanish and French out of the box — add any locale you
              need. Preconfigured themes, brandable with plain CSS.
            </Translate>
          </p>
        </div>

        <div className={styles.cell}>
          <div
            className={clsx(styles.iconBadge, styles.badgeGreen)}
            aria-hidden="true">
            <IconGrid />
          </div>
          <h3>
            <Translate
              id="homepage.bento.components.title"
              description="Bento cell: rich components, title">
              Rich components
            </Translate>
          </h3>
          <p>
            <Translate
              id="homepage.bento.components.body"
              description="Bento cell: rich components, body">
              Built on PrimeReact: editable grids, pivot matrices, charts,
              calendars, wizards — or plug in your own custom widgets.
            </Translate>
          </p>
        </div>

        <div className={styles.cell}>
          <div
            className={clsx(styles.iconBadge, styles.badgePurple)}
            aria-hidden="true">
            <IconShield />
          </div>
          <h3>
            <Translate
              id="homepage.bento.security.title"
              description="Bento cell: security, title">
              Security built in
            </Translate>
          </h3>
          <p>
            <Translate
              id="homepage.bento.security.body"
              description="Bento cell: security, body">
              Authentication, profiles and screen-level permissions wired
              through Spring Security.
            </Translate>
          </p>
        </div>
      </div>
    </HomeReveal>
  );
}
