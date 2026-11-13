/*
 * AWE 5 visual reference prototype - login page behaviour (states, validation, password change, language, mode).
 * Prototype rules for the demo flow: password "expired" opens the change-password step, "veloria" signs in, anything
 * else is rejected as invalid credentials.
 */
(() => {
  'use strict';

  const SPRITE = 'assets/icons/sprite.svg';
  const $ = (sel, ctx = document) => ctx.querySelector(sel);
  const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));
  const LOADING_MS = 1200;
  // After a successful sign-in the prototype always lands on its own sample screen (fixed internal path)
  const goHome = () => $('#lg-home').click();

  /* ======================================================================
     Copy (en / es / fr)
     ====================================================================== */
  const I18N = {
    en: {
      title: 'Sign in to the platform', subtitle: 'Enter your corporate credentials to continue.',
      alertTitle: 'Incorrect username or password.', alertText: 'Check your credentials. 2 attempts left before the account is locked.',
      user: 'Username', userErr: 'Enter your username', pass: 'Password', passErr: 'Enter your password', caps: 'Caps Lock is on',
      forgot: 'Forgot password?', submit: 'Sign in', submitting: 'Signing in…', or: 'or continue with',
      sso: 'Continue with your organization', back: 'Back to sign in', chTitle: 'Change your password',
      chSubtitle: 'Your password expired on 20 Oct 2026. Choose a new one to continue.', newPass: 'New password',
      newErr: 'The password does not meet every requirement', ruleLen: 'At least 12 characters',
      ruleCase: 'Upper and lower case letters', ruleDigit: 'At least one number', ruleSymbol: 'At least one symbol',
      ruleHistory: 'Different from your last 5 passwords', confirm: 'Confirm new password', confirmErr: 'The passwords do not match',
      chSubmit: 'Change password and sign in', chSubmitting: 'Changing password…', ssoTitle: 'Signing you in',
      ssoText: 'Redirecting to the Veloria Logistics identity provider. Keep this tab open.', cancel: 'Cancel',
      show: 'Show password', hide: 'Hide password', met: ', met', notMet: ', not met'
    },
    es: {
      title: 'Acceso a la plataforma', subtitle: 'Introduzca sus credenciales corporativas para continuar.',
      alertTitle: 'Usuario o contraseña incorrectos.', alertText: 'Revise sus credenciales. Quedan 2 intentos antes de bloquear la cuenta.',
      user: 'Usuario', userErr: 'Introduzca su usuario', pass: 'Contraseña', passErr: 'Introduzca su contraseña',
      caps: 'Bloq Mayús está activado', forgot: '¿Ha olvidado su contraseña?', submit: 'Acceder', submitting: 'Accediendo…',
      or: 'o continúe con', sso: 'Continuar con su organización', back: 'Volver al acceso', chTitle: 'Cambie su contraseña',
      chSubtitle: 'Su contraseña caducó el 20 oct 2026. Elija una nueva para continuar.', newPass: 'Nueva contraseña',
      newErr: 'La contraseña no cumple todos los requisitos', ruleLen: 'Al menos 12 caracteres',
      ruleCase: 'Mayúsculas y minúsculas', ruleDigit: 'Al menos un número', ruleSymbol: 'Al menos un símbolo',
      ruleHistory: 'Distinta de sus 5 últimas contraseñas', confirm: 'Confirme la nueva contraseña',
      confirmErr: 'Las contraseñas no coinciden', chSubmit: 'Cambiar contraseña y acceder', chSubmitting: 'Cambiando la contraseña…',
      ssoTitle: 'Iniciando sesión', ssoText: 'Le estamos redirigiendo al proveedor de identidad de Veloria Logistics. No cierre esta pestaña.',
      cancel: 'Cancelar', show: 'Mostrar contraseña', hide: 'Ocultar contraseña', met: ', cumplido', notMet: ', no cumplido'
    },
    fr: {
      title: 'Connexion à la plateforme', subtitle: 'Saisissez vos identifiants professionnels pour continuer.',
      alertTitle: 'Identifiant ou mot de passe incorrect.', alertText: 'Vérifiez vos identifiants. Il reste 2 tentatives avant le blocage du compte.',
      user: 'Identifiant', userErr: 'Saisissez votre identifiant', pass: 'Mot de passe', passErr: 'Saisissez votre mot de passe',
      caps: 'Verr. Maj est activé', forgot: 'Mot de passe oublié ?', submit: 'Se connecter', submitting: 'Connexion…',
      or: 'ou continuer avec', sso: 'Continuer avec votre organisation', back: 'Retour à la connexion',
      chTitle: 'Changez votre mot de passe', chSubtitle: 'Votre mot de passe a expiré le 20 oct. 2026. Choisissez-en un nouveau pour continuer.',
      newPass: 'Nouveau mot de passe', newErr: 'Le mot de passe ne respecte pas toutes les exigences',
      ruleLen: 'Au moins 12 caractères', ruleCase: 'Majuscules et minuscules', ruleDigit: 'Au moins un chiffre',
      ruleSymbol: 'Au moins un symbole', ruleHistory: 'Différent de vos 5 derniers mots de passe',
      confirm: 'Confirmez le nouveau mot de passe', confirmErr: 'Les mots de passe ne correspondent pas',
      chSubmit: 'Changer le mot de passe et se connecter', chSubmitting: 'Changement du mot de passe…',
      ssoTitle: 'Connexion en cours', ssoText: 'Redirection vers le fournisseur d’identité de Veloria Logistics. Gardez cet onglet ouvert.',
      cancel: 'Annuler', show: 'Afficher le mot de passe', hide: 'Masquer le mot de passe', met: ', respectée', notMet: ', non respectée'
    }
  };
  let lang = 'en';
  const t = (key) => I18N[lang][key];
  const applyLang = () => {
    document.documentElement.lang = lang;
    $$('[data-i18n]').forEach((el) => {
      el.textContent = t(el.dataset.i18n);
    });
    $$('[data-password-toggle]').forEach((b) => b.setAttribute('aria-label', t(b.getAttribute('aria-pressed') === 'true' ? 'hide' : 'show')));
    $$('#lg-submit, #ch-submit').forEach((b) => {
      if (b.classList.contains('is-loading')) $('[data-i18n]', b).textContent = t(b.id === 'lg-submit' ? 'submitting' : 'chSubmitting');
    });
    updateRules();
  };
  const langSelect = $('select[name="language"]');
  langSelect.addEventListener('change', () => {
    lang = langSelect.value;
    applyLang();
  });

  /* ======================================================================
     Elements and helpers
     ====================================================================== */
  const steps = { signin: $('#step-signin'), change: $('#step-change'), sso: $('#step-sso') };
  const user = $('#lg-user');
  const pass = $('#lg-pass');
  const alertBox = $('#lg-alert');
  const caps = $('#lg-caps');

  const showStep = (name) => Object.entries(steps).forEach(([k, el]) => {
    el.hidden = k !== name;
  });

  const setFieldError = (input, errId, on) => {
    input.closest('.control').classList.toggle('is-invalid', on);
    input.setAttribute('aria-invalid', String(on));
    document.getElementById(errId).hidden = !on;
  };

  const setLoading = (form, on) => {
    const fields = $('fieldset', form);
    const btn = $('button[type="submit"]', form);
    fields.disabled = on;
    form.setAttribute('aria-busy', String(on));
    btn.classList.toggle('is-loading', on);
    btn.setAttribute('aria-disabled', String(on));
    $('[data-i18n]', btn).textContent = t(on ? (form.id === 'lg-form' ? 'submitting' : 'chSubmitting')
      : (form.id === 'lg-form' ? 'submit' : 'chSubmit'));
    if (form.id === 'lg-form') $('#lg-sso').disabled = on;
  };

  const resetSignin = () => {
    setLoading($('#lg-form'), false);
    setFieldError(user, 'lg-user-err', false);
    setFieldError(pass, 'lg-pass-err', false);
    alertBox.hidden = true;
    caps.hidden = true;
    user.value = '';
    pass.value = '';
  };

  /* ======================================================================
     Sign in
     ====================================================================== */
  const validateSignin = () => {
    const userEmpty = !user.value.trim();
    const passEmpty = !pass.value;
    setFieldError(user, 'lg-user-err', userEmpty);
    setFieldError(pass, 'lg-pass-err', passEmpty);
    if (userEmpty) user.focus();
    else if (passEmpty) pass.focus();
    return !userEmpty && !passEmpty;
  };

  let timer = null;
  $('#lg-form').addEventListener('submit', (e) => {
    e.preventDefault();
    if ($('#lg-submit').classList.contains('is-loading')) return;
    alertBox.hidden = true;
    if (!validateSignin()) return;
    setLoading($('#lg-form'), true);
    clearTimeout(timer);
    timer = setTimeout(() => {
      setLoading($('#lg-form'), false);
      if (pass.value === 'expired') {
        setState('expired');
      } else if (pass.value === 'veloria') {
        goHome();
      } else {
        alertBox.hidden = false;
        pass.value = '';
        pass.focus();
      }
    }, LOADING_MS);
  });
  // Errors clear as soon as the field gets a value
  [[user, 'lg-user-err'], [pass, 'lg-pass-err']].forEach(([input, err]) => input.addEventListener('input', () => {
    if (input.value) setFieldError(input, err, false);
  }));

  // Caps Lock warning while typing the password
  const checkCaps = (e) => {
    if (typeof e.getModifierState === 'function') caps.hidden = !e.getModifierState('CapsLock');
  };
  pass.addEventListener('keydown', checkCaps);
  pass.addEventListener('keyup', checkCaps);
  pass.addEventListener('blur', () => {
    caps.hidden = true;
  });

  $$('[data-password-toggle]').forEach((btn) => btn.addEventListener('click', () => {
    const input = document.getElementById(btn.dataset.passwordToggle);
    const show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    btn.setAttribute('aria-pressed', String(show));
    btn.setAttribute('aria-label', t(show ? 'hide' : 'show'));
    $('use', btn).setAttribute('href', `${SPRITE}#${show ? 'eye-off' : 'eye'}`);
  }));

  $('#lg-sso').addEventListener('click', () => setState('sso'));
  $$('[data-back]').forEach((b) => b.addEventListener('click', () => setState('default')));
  $('.login__forgot').addEventListener('click', (e) => e.preventDefault());

  /* ======================================================================
     Change password (#849)
     ====================================================================== */
  const newPass = $('#ch-new');
  const confirmPass = $('#ch-confirm');
  const RULES = {
    len: (v) => v.length >= 12,
    case: (v) => /[a-z]/.test(v) && /[A-Z]/.test(v),
    digit: (v) => /\d/.test(v),
    symbol: (v) => /[^A-Za-z0-9]/.test(v),
    history: (v) => v.length > 0 && v !== 'Veloria-2025!'
  };
  function updateRules() {
    let all = true;
    $$('#ch-rules li').forEach((li) => {
      const ok = RULES[li.dataset.rule](newPass.value);
      all = all && ok;
      li.classList.toggle('is-met', ok);
      $('.rule-state', li).textContent = t(ok ? 'met' : 'notMet');
    });
    return all;
  }
  newPass.addEventListener('input', () => {
    if (updateRules()) setFieldError(newPass, 'ch-new-err', false);
  });
  confirmPass.addEventListener('input', () => {
    if (confirmPass.value === newPass.value) setFieldError(confirmPass, 'ch-confirm-err', false);
  });
  $('#ch-form').addEventListener('submit', (e) => {
    e.preventDefault();
    if ($('#ch-submit').classList.contains('is-loading')) return;
    const rulesOk = updateRules();
    const match = confirmPass.value.length > 0 && confirmPass.value === newPass.value;
    setFieldError(newPass, 'ch-new-err', !rulesOk);
    setFieldError(confirmPass, 'ch-confirm-err', rulesOk && !match);
    if (!rulesOk) {
      newPass.focus();
      return;
    }
    if (!match) {
      confirmPass.focus();
      return;
    }
    setLoading($('#ch-form'), true);
    timer = setTimeout(() => {
      goHome();
    }, LOADING_MS);
  });

  /* ======================================================================
     Prototype state switcher (select + URL hash)
     ====================================================================== */
  const STATES = ['default', 'errors', 'invalid', 'caps', 'loading', 'expired', 'sso'];
  const stateSelect = $('select#lg-state-native, select#lg-state');
  const syncSwitcher = (state) => {
    stateSelect.value = state;
    const trigger = document.getElementById('lg-state');
    const label = Array.from(stateSelect.options).find((o) => o.value === state)?.textContent;
    const value = trigger && $('.select-value', trigger);
    if (value && label) value.textContent = label;
  };

  function setState(state, focus = true) {
    clearTimeout(timer);
    resetSignin();
    setLoading($('#ch-form'), false);
    newPass.value = '';
    confirmPass.value = '';
    setFieldError(newPass, 'ch-new-err', false);
    setFieldError(confirmPass, 'ch-confirm-err', false);
    updateRules();
    showStep(state === 'expired' ? 'change' : state === 'sso' ? 'sso' : 'signin');
    switch (state) {
      case 'errors':
        validateSignin();
        break;
      case 'invalid':
        user.value = 'pat.vega';
        alertBox.hidden = false;
        if (focus) pass.focus();
        break;
      case 'caps':
        user.value = 'pat.vega';
        pass.value = 'VELORIA';
        caps.hidden = false;
        if (focus) pass.focus();
        break;
      case 'loading':
        user.value = 'pat.vega';
        pass.value = 'veloria-demo';
        setLoading($('#lg-form'), true);
        break;
      case 'expired':
        newPass.value = 'Veloria2026';
        updateRules();
        if (focus) newPass.focus();
        break;
      case 'sso':
        if (focus) $('#step-sso [data-back]').focus();
        break;
      default:
        if (focus) user.focus();
    }
    syncSwitcher(state);
    if (location.hash.slice(1) !== state) history.replaceState(null, '', state === 'default' ? location.pathname : `#${state}`);
  }

  stateSelect.addEventListener('change', () => setState(stateSelect.value, false));
  const initial = location.hash.slice(1);
  setState(STATES.includes(initial) ? initial : 'default', !STATES.includes(initial) || initial === 'default');

  /* ======================================================================
     Footer colour mode (kept in sync with the Theme panel)
     ====================================================================== */
  const root = document.documentElement;
  const syncMode = () => {
    const mode = root.classList.contains('dark') ? 'dark' : root.classList.contains('light') ? 'light' : 'system';
    $$('input[name="lg-mode"]').forEach((r) => {
      r.checked = r.value === mode;
    });
  };
  $$('input[name="lg-mode"]').forEach((r) => r.addEventListener('change', () => window.AweProto.setThemeMode(r.value)));
  document.addEventListener('awe-theme-change', syncMode);
  syncMode();
})();
