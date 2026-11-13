import {
  clearPendingLoginChoices,
  LOGIN_MODE_KEY,
  loginLanguageParameters,
  loginModeParameters,
  PENDING_LOGIN_LANGUAGE_KEY,
  PENDING_LOGIN_MODE_KEY,
  storeLoginLanguage,
  storeLoginMode,
  withLoginMode
} from '../../../src/utilities/signin';

// The colour mode chosen on the sign-in page is sent with the login, which saves it as the mode of the user
describe('awe-client-react/test/js/utilities/signinLoginModeTest.js', () => {
  afterEach(() => {
    localStorage.removeItem(LOGIN_MODE_KEY);
    sessionStorage.removeItem(PENDING_LOGIN_MODE_KEY);
  });

  it('sends the mode chosen on the sign-in page with the login', () => {
    storeLoginMode('dark');

    expect(localStorage.getItem(LOGIN_MODE_KEY)).toBe('dark');
    expect(loginModeParameters('login')).toEqual({signinThemeMode: 'dark'});
  });

  it.each(['login-azure', 'login-sso'])('sends it with the SSO login %s too', (serverAction) => {
    storeLoginMode('light');

    expect(loginModeParameters(serverAction)).toEqual({signinThemeMode: 'light'});
  });

  it('forgets it when the settings of a user session load, as after the SSO login', () => {
    storeLoginMode('dark');

    expect(withLoginMode({authenticated: true, themeMode: 'light'})).toEqual({authenticated: true, themeMode: 'light'});
    expect(loginModeParameters('login')).toEqual({});
  });

  it('keeps it while the settings of the sign-in page load, before the login', () => {
    storeLoginMode('dark');

    withLoginMode({authenticated: false, themeMode: 'light'});

    expect(loginModeParameters('login')).toEqual({signinThemeMode: 'dark'});
  });

  it('sends it with no other server action', () => {
    storeLoginMode('dark');

    expect(loginModeParameters('maintain')).toEqual({});
  });

  it('sends nothing when the mode was only restored from a previous visit, not chosen', () => {
    localStorage.setItem(LOGIN_MODE_KEY, 'dark');

    expect(loginModeParameters('login')).toEqual({});
  });

  it('sends nothing once the login completed, so a later login keeps the mode of the user', () => {
    storeLoginMode('light');

    clearPendingLoginChoices();

    expect(loginModeParameters('login')).toEqual({});
    expect(localStorage.getItem(LOGIN_MODE_KEY)).toBe('light');
  });

  it('sends nothing for a pending value that is not a mode', () => {
    sessionStorage.setItem(PENDING_LOGIN_MODE_KEY, 'sepia');

    expect(loginModeParameters('login')).toEqual({});
  });
});

// The language chosen on the sign-in page is sent with the login too, which makes it the language of the user
describe('awe-client-react/test/js/utilities/signinLoginModeTest.js: sign-in language', () => {
  afterEach(() => {
    sessionStorage.removeItem(PENDING_LOGIN_LANGUAGE_KEY);
  });

  it.each(['login', 'login-azure', 'login-sso'])('sends the language chosen on the sign-in page with the login %s', (serverAction) => {
    storeLoginLanguage('es-ES', false);

    expect(sessionStorage.getItem(PENDING_LOGIN_LANGUAGE_KEY)).toBe('es-ES');
    expect(loginLanguageParameters(serverAction)).toEqual({signinLanguage: 'es-ES'});
  });

  it('sends it with no other server action', () => {
    storeLoginLanguage('es-ES', null);

    expect(loginLanguageParameters('maintain')).toEqual({});
  });

  it('sends nothing when the language was not changed on the sign-in page, so the language of the user wins', () => {
    expect(loginLanguageParameters('login')).toEqual({});
  });

  it('does not keep a language changed in a user session, which is not a sign-in choice', () => {
    storeLoginLanguage('fr-FR', true);

    expect(sessionStorage.getItem(PENDING_LOGIN_LANGUAGE_KEY)).toBeNull();
  });

  it('forgets it with the mode once the login completed or the settings of a user session load', () => {
    storeLoginLanguage('es-ES', false);
    storeLoginMode('dark');
    clearPendingLoginChoices();

    expect(loginLanguageParameters('login')).toEqual({});
    expect(loginModeParameters('login')).toEqual({});

    storeLoginLanguage('fr-FR', false);
    withLoginMode({authenticated: true, language: 'en-GB'});

    expect(loginLanguageParameters('login')).toEqual({});
  });
});
