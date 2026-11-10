import React, {useState} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import Translate, {translate} from '@docusaurus/Translate';
import HomeReveal from '../HomeReveal';
import ComponentIcon from './icons';
import {COMPONENTS, FAMILIES, familiesInUse, filterComponents} from './catalog';
import styles from './styles.module.css';

const FAMILY_TONE = {
  forms: styles.toneBlue,
  data: styles.toneGreen,
  layout: styles.tonePurple,
  charts: styles.toneMix,
  media: styles.toneMixAlt,
};

// Strings of the catalog data are English: the ids of code.json translate them
const text = (kind, id, message) => translate({id: `catalog.${kind}.${id}`, message});

function Card({component}) {
  const ready = component.status === 'ready';
  const title = text('title', component.id, component.title);
  const family = text('family', component.family, FAMILIES.find(item => item.id === component.family).title);
  const body = (
    <>
      <span className={clsx(styles.iconBadge, FAMILY_TONE[component.family])}>
        <ComponentIcon name={component.id} />
      </span>
      <span className={styles.status}>
        {ready ? (
          <span className={styles.live}>
            <Translate id="catalog.status.live" description="Catalog card: the component has a live page">Live</Translate>
          </span>
        ) : (
          <span className={styles.soon}>
            <Translate id="catalog.status.soon" description="Catalog card: the page of the component is not published yet">Soon</Translate>
          </span>
        )}
      </span>
      <h3 className={styles.cardTitle}>{title}</h3>
      <p className={styles.cardBody}>{text('description', component.id, component.description)}</p>
      <span className={styles.cardFoot}>
        <span className={styles.family}>{family}</span>
        <code className={styles.element}>{`<${component.element}>`}</code>
      </span>
    </>
  );

  return (
    <li className={styles.item}>
      {ready ? (
        <Link className={clsx(styles.card, styles.cardReady)} to={component.to}>
          {body}
        </Link>
      ) : (
        <div className={clsx(styles.card, styles.cardSoon)}>{body}</div>
      )}
    </li>
  );
}

/**
 * Card grid of the component catalog, with family filters. The data lives in catalog.js.
 */
export default function ComponentCatalog() {
  const [family, setFamily] = useState('all');
  const visible = filterComponents(COMPONENTS, family);
  const chips = [{id: 'all', title: 'All'}, ...familiesInUse(COMPONENTS, FAMILIES)];

  return (
    <HomeReveal as="div" className={styles.catalog}>
      <div className={styles.filters} role="group" aria-label={translate({id: 'catalog.filters.label', message: 'Filter by family'})}>
        {chips.map(chip => (
          <button
            key={chip.id}
            type="button"
            className={clsx(styles.chip, family === chip.id && styles.chipOn)}
            aria-pressed={family === chip.id}
            onClick={() => setFamily(chip.id)}>
            {chip.id === 'all' ? text('family', 'all', 'All') : text('family', chip.id, chip.title)}
          </button>
        ))}
      </div>
      <ul className={styles.grid}>
        {visible.map(component => <Card key={component.id} component={component} />)}
      </ul>
      <p className={styles.count} role="status" aria-live="polite">
        {translate({id: 'catalog.count', message: '{count} components'}, {count: visible.length})}
      </p>
    </HomeReveal>
  );
}
