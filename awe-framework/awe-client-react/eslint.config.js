const reactHooks = require('eslint-plugin-react-hooks');
const tseslint = require('typescript-eslint');

module.exports = tseslint.config(
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
    // TypeScript sources: typescript-eslint parser and recommended rules, with the same hooks rules as the JavaScript ones
    files: ['**/*.{ts,tsx}'],
    extends: [tseslint.configs.recommended],
    languageOptions: {
      parserOptions: {
        ecmaFeatures: {
          jsx: true
        }
      }
    },
    plugins: {
      'react-hooks': reactHooks
    },
    rules: {
      'react-hooks/rules-of-hooks': 2,
      'react-hooks/exhaustive-deps': 1,
      semi: 2,
      'no-console': 1
    }
  },
  {
    // Tests and their Jest setup log and silence the console on purpose
    files: ['test/**/*.{js,jsx,ts,tsx}'],
    rules: {
      'no-console': 0
    }
  },
  {
    // The chart server is a Node process: the console is its log
    files: ['src/renderer/**/*.js'],
    rules: {
      'no-console': 0
    }
  }
);
