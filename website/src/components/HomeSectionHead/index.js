import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

export default function HomeSectionHead({title, description, compact}) {
  return (
    <div className={clsx(styles.head, compact && styles.headCompact)}>
      <h2 className={styles.title}>{title}</h2>
      {description ? <p className={styles.description}>{description}</p> : null}
    </div>
  );
}
