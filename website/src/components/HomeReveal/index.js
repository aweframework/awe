import React, {useEffect, useRef, useState} from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

/**
 * Wraps a homepage section and fades it in the first time it enters the
 * viewport. Everything that touches the DOM lives inside `useEffect`, so the
 * component renders identically on the server.
 */
export default function HomeReveal({
  as: Tag = 'section',
  className,
  children,
  ...rest
}) {
  const nodeRef = useRef(null);
  const [revealed, setRevealed] = useState(false);

  useEffect(() => {
    const node = nodeRef.current;
    if (!node) {
      return undefined;
    }

    const reduced = window.matchMedia(
      '(prefers-reduced-motion: reduce)',
    ).matches;

    if (reduced || typeof IntersectionObserver === 'undefined') {
      setRevealed(true);
      return undefined;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setRevealed(true);
            observer.unobserve(entry.target);
          }
        });
      },
      {threshold: 0.12},
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  return (
    <Tag
      ref={nodeRef}
      className={clsx(styles.reveal, revealed && styles.revealed, className)}
      {...rest}>
      {children}
    </Tag>
  );
}
