import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import Translate from '@docusaurus/Translate';
import HomeReveal from '../HomeReveal';
import HomeSectionHead from '../HomeSectionHead';
import styles from './styles.module.css';

export default function HomeDocsHub() {
  const cards = [
    {
      key: 'start',
      to: '/docs/installation',
      go: 'docs/installation →',
      title: (
        <Translate
          id="homepage.hub.start.title"
          description="Docs hub card: getting started, title">
          Getting started
        </Translate>
      ),
      body: (
        <Translate
          id="homepage.hub.start.body"
          description="Docs hub card: getting started, body">
          Install, generate a project and run your first screen.
        </Translate>
      ),
    },
    {
      key: 'screens',
      to: '/docs/api/screens',
      go: 'docs/api/screens →',
      title: (
        <Translate
          id="homepage.hub.screens.title"
          description="Docs hub card: screen development, title">
          Screen development
        </Translate>
      ),
      body: (
        <Translate
          id="homepage.hub.screens.body"
          description="Docs hub card: screen development, body">
          The full XML vocabulary: windows, criteria, grids, charts.
        </Translate>
      ),
    },
    {
      key: 'data',
      to: '/docs/api/query',
      go: 'docs/api/query →',
      title: (
        <Translate
          id="homepage.hub.data.title"
          description="Docs hub card: data engine, title">
          Data engine
        </Translate>
      ),
      body: (
        <Translate
          id="homepage.hub.data.body"
          description="Docs hub card: data engine, body">
          Queries, maintains, enumerated lists and service calls.
        </Translate>
      ),
    },
    {
      key: 'theming',
      to: '/docs/api/i18n-internationalization',
      go: 'docs/api/i18n →',
      title: (
        <Translate
          id="homepage.hub.theming.title"
          description="Docs hub card: theming and i18n, title">
          Theming and i18n
        </Translate>
      ),
      body: (
        <Translate
          id="homepage.hub.theming.body"
          description="Docs hub card: theming and i18n, body">
          Locales, themes and CSS customization for your brand.
        </Translate>
      ),
    },
  ];

  return (
    <HomeReveal className={clsx('awe-wrap', styles.block)} id="hub">
      <HomeSectionHead
        title={
          <Translate
            id="homepage.hub.title"
            description="Title of the docs hub section">
            Find your path through the docs
          </Translate>
        }
        description={
          <Translate
            id="homepage.hub.description"
            description="Description of the docs hub section">
            Four entry points cover most journeys — from first install to
            advanced data binding.
          </Translate>
        }
      />
      <div className={styles.hub}>
        {cards.map(({key, to, go, title, body}) => (
          <Link key={key} to={to} className={styles.card}>
            <h3>{title}</h3>
            <p>{body}</p>
            <span className={styles.go}>{go}</span>
          </Link>
        ))}
      </div>
    </HomeReveal>
  );
}
