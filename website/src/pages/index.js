import React from 'react';
import Layout from '@theme/Layout';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import HomeHero from '../components/HomeHero';
import HomeQuickStart from '../components/HomeQuickStart';
import HomeHowItWorks from '../components/HomeHowItWorks';
import HomeBentoFeatures from '../components/HomeBentoFeatures';
import HomeDocsHub from '../components/HomeDocsHub';
import styles from './styles.module.css';

export default function Home() {
  const {
    siteConfig: {customFields = {}, tagline} = {},
  } = useDocusaurusContext();

  return (
    <Layout title={tagline} description={customFields.description}>
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
