const karmaArgs = (window.__karma__ && window.__karma__.config && window.__karma__.config.args) || [];
const grepArg = karmaArgs.find((arg) => arg.startsWith('--grep='));

if (grepArg && typeof jasmine !== 'undefined') {
  const pattern = grepArg.replace('--grep=', '');
  const regex = new RegExp(pattern);
  jasmine.getEnv().configure({
    specFilter: (spec) => regex.test(spec.getFullName())
  });
}

import './redux';
import './services';
import './columns';
import './criteria';
import './components';
import './containers';
import './utilities';
import './widgets';
import './templates';
import './hooks';
