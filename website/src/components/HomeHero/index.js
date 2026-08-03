import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import Translate from '@docusaurus/Translate';
import HomeXmlToUiDemo from '../HomeXmlToUiDemo';
import HomeStatsStrip from '../HomeStatsStrip';
import styles from './styles.module.css';

/* AWE wave mark, inlined so it can carry the brand gradient and a glow. */
const AWE_WAVE_PATH =
  'M77.3,9.6c0-5.3,4.3-9.6,9.6-9.6c5.3,0,9.6,4.3,9.6,9.6s-4.3,9.6-9.6,9.6C81.6,19.3,77.3,15,77.3,9.6z M539.9,64.5C539.9,64.5,539.9,64.5,539.9,64.5c0-6.3-3.1-11.9-7.8-15.4c-3.2-2.4-7.2-3.8-11.5-3.8c-10.7,0-19.3,8.6-19.3,19.3c0,0,0,0,0,0v16.1v0.2V81c-0.1,2.5-1.1,4.8-2.7,6.4c-1.8,1.8-4.2,3-7,3c-5.3,0-9.6-4.3-9.6-9.6V82c0-10.2-7.9-18.5-17.9-19.2c-0.4,0-0.9-0.1-1.3-0.1c-10.7,0-19.3,8.6-19.3,19.3c0,0,0,0,0,0v59.3c0,0,0,0,0,0v51v19.4c0,31.9-25.8,57.7-57.7,57.7v0.1c-35.9,0-66.1-24.6-74.7-57.9c-1.2-4.8-2-9.8-2.3-14.9v-4.6v-4.3V72.4c0-4.3-0.7-8.5-2-12.4c-4.7-13.9-17.1-24.2-32.1-25.9c-0.2,0-0.5-0.1-0.7-0.1c-0.4,0-0.8-0.1-1.2-0.1c-0.7,0-1.4-0.1-2.2-0.1c-0.1,0-0.2,0-0.3,0c0,0-0.1,0-0.1,0c0,0-0.1,0-0.1,0c-0.1,0-0.2,0-0.3,0c-0.7,0-1.5,0-2.2,0.1c-0.4,0-0.8,0.1-1.2,0.1c-0.2,0-0.5,0.1-0.7,0.1c-15,1.7-27.4,12-32.1,25.9c-1.3,3.9-2,8-2,12.3c0,0,0,0.1,0,0.1h0v118.7c0-1,0-2.1-0.1-3.1v4.3v4.6c-0.3,5.1-1.1,10.1-2.3,14.9c-8.6,33.3-38.8,57.9-74.7,57.9v-0.1c-31.9,0-57.7-25.8-57.7-57.7v-19.4v-51V53.1c0-1.2-0.2-2.4-0.6-3.4c-1.4-3.6-4.9-6.2-9-6.2c-5.3,0-9.6,4.3-9.6,9.6v5.7l-0.1,15.8V111c0,5.3-4.3,9.6-9.6,9.6c-5.3,0-9.6-4.3-9.6-9.6v-5.1V72.4v-0.4c0,0,0,0,0,0C57.7,56.2,44.8,43.5,29,43.5c-14.2,0-26,10.2-28.5,23.7C0.2,68.9,0,70.6,0,72.4v120v0.1c0,2.1,0.1,4.2,0.1,6.2c1.1,27.2,9.2,52.6,22.6,74.4c4.7,7.6,10,14.7,15.8,21.4c4.3,4.9,8.9,9.5,13.7,13.8c6.7,5.9,13.9,11.2,21.5,15.9c21.8,13.4,47.2,21.5,74.4,22.6c2.1,0.1,4.1,0.1,6.2,0.1c2.1,0,4.2-0.1,6.2-0.1c27.1-1.1,52.5-9.2,74.2-22.5c7.7-4.7,14.9-10,21.6-15.9c4.8-4.2,9.3-8.7,13.5-13.5c0-0.1,0.1-0.1,0.1-0.2c4.3,4.8,8.8,9.4,13.7,13.7c6.7,5.9,13.8,11.2,21.5,15.9c21.8,13.4,47.2,21.5,74.4,22.6c2.1,0.1,4.1,0.1,6.2,0.1s4.2-0.1,6.2-0.1c27.1-1.1,52.5-9.2,74.2-22.5c7.7-4.7,14.9-10,21.6-15.9c4.8-4.2,9.3-8.7,13.5-13.5c5.9-6.7,11.3-13.9,16-21.6c11.2-18.3,18.7-39.1,21.5-61.3c0.5-4.2,0.9-8.5,1.1-12.8c0-0.1,0-0.2,0-0.3c0.1-2.1,0.1-4.1,0.1-6.2c0-2.2-0.1-4.3-0.1-6.5V64.5z M462.7,42.5c10.7,0,19.3-8.6,19.3-19.3s-8.6-19.3-19.3-19.3s-19.3,8.6-19.3,19.3S452.1,42.5,462.7,42.5z';

export default function HomeHero() {
  return (
    <header className={styles.hero}>
      <div className={clsx('awe-wrap', styles.heroGrid)}>
        <div className={styles.heroCopy}>
          {/* The glow lives on the wrapper so the entrance clip-path on the
              svg cannot crop it into a rectangle. */}
          <span className={styles.heroLogoGlow}>
          <svg
            className={styles.heroLogo}
            viewBox="0 0 540 346.8"
            role="img"
            aria-label="AWE framework logo">
            <defs>
              <linearGradient
                id="aweHeroWaveGradient"
                gradientUnits="userSpaceOnUse"
                x1="-140.87"
                y1="0.99"
                x2="614.27"
                y2="353.12">
                <stop offset="0.01" stopColor="#8A4793" />
                <stop offset="0.45" stopColor="#257EC0" />
                <stop offset="1" stopColor="#40AC5C" />
              </linearGradient>
            </defs>
            <path fill="url(#aweHeroWaveGradient)" d={AWE_WAVE_PATH} />
          </svg>
          </span>

          <p className={styles.eyebrow}>
            <Translate
              id="homepage.hero.eyebrow"
              description="Small monospace kicker above the homepage headline">
              // Low-code · Spring Boot · React
            </Translate>
          </p>

          <h1 className={styles.headline}>
            <Translate
              id="homepage.hero.title"
              description="Homepage hero headline. {gradient} is the brand-gradient part of the sentence."
              values={{
                lineBreak: <br />,
                gradient: (
                  <span className={styles.gradientWord}>
                    <Translate
                      id="homepage.hero.title.highlight"
                      description="Gradient-highlighted part of the homepage headline">
                      functional web applications
                    </Translate>
                  </span>
                ),
              }}>
              {'Low coding complete{lineBreak}{gradient}.'}
            </Translate>
          </h1>

          <p className={styles.sub}>
            <Translate
              id="homepage.hero.subtitle"
              description="Paragraph below the homepage headline">
              AWE turns declarative XML into complete web applications —
              screens, menus, navigation and data binding — rendered with React,
              powered by Spring Boot and the query and maintain engine.
            </Translate>
          </p>

          <div className={styles.ctas}>
            <Link className={clsx(styles.btn, styles.btnPrimary)} to="/docs/">
              <Translate
                id="homepage.hero.cta.start"
                description="Primary call to action of the homepage hero">
                Start using AWE
              </Translate>
            </Link>
            <Link
              className={clsx(styles.btn, styles.btnGhost)}
              to="http://demo.aweframework.com">
              <Translate
                id="homepage.hero.cta.demo"
                description="Secondary call to action of the homepage hero">
                Try the demo
              </Translate>
            </Link>
          </div>
        </div>

        <HomeXmlToUiDemo />
      </div>

      <HomeStatsStrip />
    </header>
  );
}
