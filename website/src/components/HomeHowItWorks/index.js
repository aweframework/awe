import React from 'react';
import clsx from 'clsx';
import Translate from '@docusaurus/Translate';
import HomeReveal from '../HomeReveal';
import HomeSectionHead from '../HomeSectionHead';
import styles from './styles.module.css';

export default function HomeHowItWorks() {
  const steps = [
    {
      key: 'screen',
      number: '01',
      title: (
        <Translate
          id="homepage.steps.screen.title"
          description="How it works, step 1 title">
          Define the screen
        </Translate>
      ),
      body: (
        <Translate
          id="homepage.steps.screen.body"
          description="How it works, step 1 body">
          Windows, criteria, grids, charts and tabs — declared in a validated
          XML schema.
        </Translate>
      ),
    },
    {
      key: 'data',
      number: '02',
      title: (
        <Translate
          id="homepage.steps.data.title"
          description="How it works, step 2 title">
          Bind the data
        </Translate>
      ),
      body: (
        <Translate
          id="homepage.steps.data.body"
          description="How it works, step 2 body">
          Queries read, maintains write. Point them at SQL, NoSQL, REST services
          or Java beans.
        </Translate>
      ),
    },
    {
      key: 'dynamic',
      number: '03',
      title: (
        <Translate
          id="homepage.steps.dynamic.title"
          description="How it works, step 3 title">
          Make it dynamic
        </Translate>
      ),
      body: (
        <Translate
          id="homepage.steps.dynamic.body"
          description="How it works, step 3 body">
          Dependencies react to user input: validations, computed values,
          show/hide rules and chained actions — all in XML, zero JavaScript.
        </Translate>
      ),
    },
    {
      key: 'run',
      number: '04',
      title: (
        <Translate
          id="homepage.steps.run.title"
          description="How it works, step 4 title">
          Run and render
        </Translate>
      ),
      body: (
        <Translate
          id="homepage.steps.run.body"
          description="How it works, step 4 body">
          Spring Boot starters serve the app; the React client renders it live.
          AngularJS keeps legacy apps running.
        </Translate>
      ),
    },
  ];

  return (
    <HomeReveal className={clsx('awe-wrap', styles.block)}>
      <HomeSectionHead
        title={
          <Translate
            id="homepage.steps.title"
            description="Title of the how-it-works section">
            From XML to running app in four moves
          </Translate>
        }
        description={
          <Translate
            id="homepage.steps.description"
            description="Description of the how-it-works section">
            Every AWE application follows the same declarative loop — no
            controllers to wire, no JavaScript to write for dynamic behavior.
          </Translate>
        }
      />
      <div className={styles.steps}>
        {steps.map(({key, number, title, body}) => (
          <div key={key} className={styles.step}>
            <span className={styles.num}>{number}</span>
            <h3>{title}</h3>
            <p>{body}</p>
          </div>
        ))}
      </div>
    </HomeReveal>
  );
}
