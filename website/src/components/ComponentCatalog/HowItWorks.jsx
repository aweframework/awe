import React from 'react';
import clsx from 'clsx';
import Translate from '@docusaurus/Translate';
import HomeReveal from '../HomeReveal';
import styles from './styles.module.css';

// 24x24 line icons, like the ones of the cards
const STEPS = [
  {
    id: 'real',
    tone: styles.toneBlue,
    icon: <><rect x="3" y="4" width="18" height="13" rx="2" /><path d="M8 21h8M12 17v4" /></>,
    title: <Translate id="catalog.how.real.title" description="Catalog strip: first point, title">The real client</Translate>,
    text: (
      <Translate id="catalog.how.real.text" description="Catalog strip: first point, text">
        Each preview is the AWE React client itself, fed with sample data. No screenshots, no mock-ups.
      </Translate>
    ),
  },
  {
    id: 'xml',
    tone: styles.toneGreen,
    icon: <path d="m8 7-5 5 5 5M16 7l5 5-5 5M14 4l-4 16" />,
    title: <Translate id="catalog.how.xml.title" description="Catalog strip: second point, title">XML that follows you</Translate>,
    text: (
      <Translate id="catalog.how.xml.text" description="Catalog strip: second point, text">
        Change a control and the XML that declares it updates at once, ready to copy into your screen.
      </Translate>
    ),
  },
  {
    id: 'mode',
    tone: styles.tonePurple,
    icon: <><circle cx="12" cy="12" r="4" /><path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6 7 7M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4" /></>,
    title: <Translate id="catalog.how.mode.title" description="Catalog strip: third point, title">Light and dark</Translate>,
    text: (
      <Translate id="catalog.how.mode.text" description="Catalog strip: third point, text">
        Switch the colour mode of any preview and see how the component takes it, with the AWE design tokens.
      </Translate>
    ),
  },
];

/**
 * Compact strip of the catalog page that says what is different about these examples.
 */
export default function ComponentCatalogHowItWorks() {
  return (
    <HomeReveal as="ul" className={styles.how}>
      {STEPS.map(step => (
        <li key={step.id} className={styles.howItem}>
          <span className={clsx(styles.iconBadge, step.tone)}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              {step.icon}
            </svg>
          </span>
          <span>
            <strong className={styles.howTitle}>{step.title}</strong>
            <span className={styles.howText}>{step.text}</span>
          </span>
        </li>
      ))}
    </HomeReveal>
  );
}
