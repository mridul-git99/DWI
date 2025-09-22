import 'normalize.css';

import { isInDevelopment } from '#utils/constants';
import * as Sentry from '@sentry/react';
import React from 'react';
import { render } from 'react-dom';
import App from './App';
import FontStyles from './assets/fonts/fonts';
import { BrowserTracing } from '@sentry/tracing';

Sentry.init({
  dsn: process.env.SENTRY_DSN,
  integrations: [
    new BrowserTracing(),
    new Sentry.Replay({
      maskAllText: false,
      blockAllMedia: false,
      maskAllInputs: false,
    }),
  ],
  enabled: !isInDevelopment,
  tracesSampleRate: 1.0,
  // Capture Replay for 10% of all sessions,
  // plus for 100% of sessions with an error
  replaysSessionSampleRate: 0.1,
  replaysOnErrorSampleRate: 1.0,
  release: process.env.APP_VERSION,
  attachStacktrace: true,
  environment: window?.location?.hostname,
});

render(
  <>
    <FontStyles />
    <App />
  </>,
  document.getElementById('root'),
);
