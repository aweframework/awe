import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import Translate from '@docusaurus/Translate';
import styles from './styles.module.css';

/**
 * Header of the catalog page, in the style of the homepage hero: glow, gradient word, two calls to action.
 */
export default function ComponentCatalogHero() {
  return (
    <header className={styles.hero}>
      <div className={styles.heroGlow} aria-hidden="true" />
      <p className={styles.eyebrow}>
        <Translate id="catalog.hero.eyebrow" description="Catalog hero: small label above the title">Component catalog</Translate>
      </p>
      <h1 className={styles.headline}>
        <Translate
          id="catalog.hero.title"
          description="Catalog hero: headline; the second part is drawn with the brand gradient"
          values={{
            real: <span className={styles.gradientWord}><Translate id="catalog.hero.title.real" description="Gradient word of the catalog headline">real components</Translate></span>,
          }}>
          {'Play with the {real}'}
        </Translate>
      </h1>
      <p className={styles.sub}>
        <Translate id="catalog.hero.sub" description="Catalog hero: subtitle">
          Every example here is the actual AWE React client. Change an attribute, watch the component and the XML that produces it.
        </Translate>
      </p>
      <div className={styles.ctas}>
        <Link className={clsx(styles.btn, styles.btnPrimary)} to="/docs/components/button">
          <Translate id="catalog.hero.cta.start" description="Catalog hero: primary call to action">Start with the button</Translate>
        </Link>
        <Link className={clsx(styles.btn, styles.btnGhost)} to="/reference/screen">
          <Translate id="catalog.hero.cta.reference" description="Catalog hero: link to the XSD reference">XSD reference</Translate>
        </Link>
      </div>
    </header>
  );
}
