const reactHooks = require('eslint-plugin-react-hooks');

module.exports = [
  {
    ignores: [
      'dist/**',
      'target/**',
      'node_modules/**',
      'coverage/**'
    ]
  },
  {
    files: ['**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: {
          jsx: true
        }
      },
      globals: {
        window: 'readonly',
        document: 'readonly',
        navigator: 'readonly',
        location: 'readonly',
        history: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        console: 'readonly',
        process: 'readonly',
        module: 'readonly',
        require: 'readonly',
        __dirname: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        URL: 'readonly',
        Blob: 'readonly',
        File: 'readonly',
        FormData: 'readonly',
        fetch: 'readonly',
        WebSocket: 'readonly',
        describe: 'readonly',
        it: 'readonly',
        test: 'readonly',
        expect: 'readonly',
        beforeEach: 'readonly',
        afterEach: 'readonly',
        jest: 'readonly'
      }
    },
    plugins: {
      'react-hooks': reactHooks
    },
    rules: {
      // Hooks called conditionally break React at runtime; missing effect dependencies are a review signal
      'react-hooks/rules-of-hooks': 2,
      'react-hooks/exhaustive-deps': 1,
      semi: 2,
      'no-underscore-dangle': 0,
      'arrow-body-style': 0,
      'no-shadow': 0,
      'consistent-return': 0,
      'no-nested-ternary': 0,
      'no-console': 1,
      'no-case-declarations': 0
    }
  },
  {
    // Tests and their Jest setup log and silence the console on purpose
    files: ['test/**/*.{js,jsx}'],
    rules: {
      'no-console': 0
    }
  }
];
