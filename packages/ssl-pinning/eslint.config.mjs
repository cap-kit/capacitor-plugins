import js from '@eslint/js';
import importPlugin from 'eslint-plugin-import';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default [
  // Ignore
  {
    ignores: [
      'dist/**',
      'build/**',
      'android/**',
      'ios/**',
      'example-app/**',
    ],
  },

  // Base JS
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
  },
  js.configs.recommended,

  // TypeScript (NO type-check)
  ...tseslint.configs.recommended,

  // Import plugin (TS)
  {
    plugins: {
      import: importPlugin,
    },
  },

  // Ionic base rules
  {
    rules: {
      'no-fallthrough': 'off',
      'no-constant-condition': 'off',

      '@typescript-eslint/no-this-alias': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/explicit-module-boundary-types': [
        'warn',
        { allowArgumentsExplicitlyTypedAsAny: true },
      ],
    },
  },

  // Ionic "recommended" rules
  {
    rules: {
      '@typescript-eslint/explicit-module-boundary-types': [
        'error',
        { allowArgumentsExplicitlyTypedAsAny: true },
      ],
      '@typescript-eslint/array-type': 'error',
      '@typescript-eslint/consistent-type-assertions': 'error',
      '@typescript-eslint/consistent-type-imports': 'error',
      '@typescript-eslint/prefer-for-of': 'error',

      'import/first': 'error',
      'import/order': [
        'error',
        {
          alphabetize: { order: 'asc', caseInsensitive: false },
          groups: [['builtin', 'external'], 'parent', ['sibling', 'index']],
          'newlines-between': 'always',
        },
      ],
      'import/newline-after-import': 'error',
      'import/no-duplicates': 'error',
      'import/no-mutable-exports': 'error',
    },
  },

  {
    files: ['src/**/*.ts'],
    languageOptions: {
      parserOptions: {
        project: './tsconfig.json',
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      '@typescript-eslint/prefer-optional-chain': 'error',
    },
  },

  // Tooling JS
  {
    files: ['*.config.{js,mjs,cjs}', 'eslint.config.mjs'],
    rules: {
      'no-undef': 'off',
    },
  },
];
