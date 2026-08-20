import React from 'react';
import Layout from '@theme/Layout';
import {translate} from '@docusaurus/Translate';
import HomeHero from '../components/HomeHero';
import HomeQuickStart from '../components/HomeQuickStart';
import HomeHowItWorks from '../components/HomeHowItWorks';
import HomeBentoFeatures from '../components/HomeBentoFeatures';
import HomeDocsHub from '../components/HomeDocsHub';
import styles from './styles.module.css';

export default function Home() {
  // Imperative translate(), not <Translate>: these are string props, not
  // children. They also must not come from siteConfig, whose tagline
  // Docusaurus never extracts for translation.
  const title = translate({
    id: 'homepage.meta.title',
    description: 'Browser tab and search result title for the home page',
    message: 'Low coding complete functional web applications',
  });
  const description = translate({
    id: 'homepage.meta.description',
    description: 'Meta and social preview description for the home page',
    message:
      'AWE turns declarative XML into complete web applications: screens, ' +
      'menus, navigation and data binding, rendered with React on Spring Boot.',
  });

  return (
    <Layout title={title} description={description}>
      <div className={styles.page}>
        <HomeHero />
        <main>
          <HomeQuickStart />
          <HomeHowItWorks />
          <HomeBentoFeatures />
          <HomeDocsHub />
        </main>
      </div>
    </Layout>
  );
}
