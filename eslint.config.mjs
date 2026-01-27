import js from '@eslint/js';
import importPlugin from 'eslint-plugin-import';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default [
  // 1. Global Ignores
  {
    ignores: [
      '**/dist/**',
      '**/build/**',
      '**/android/**', // Ignore Android native code
      '**/ios/**',     // Ignore iOS native code
      '**/node_modules/**',
      '**/.docusaurus/**',
    ],
  },

  // 2. Base JS Configuration
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
  },
  js.configs.recommended,

  // 3. TypeScript (Base)
  ...tseslint.configs.recommended,

  // 4. Import Plugin
  {
    plugins: {
      import: importPlugin,
    },
  },

  // 5. Ionic Base Rules (Applied globally)
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

  // 6. Ionic Strict Rules
  {
    rules: {
      '@typescript-eslint/array-type': 'error',
      '@typescript-eslint/consistent-type-assertions': 'error',
      '@typescript-eslint/consistent-type-imports': ['error', {
        prefer: 'no-type-imports'
      }],
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

  // 7. Specific Configuration for Plugins (Monorepo)
  {
    // Target all packages inside 'packages' directory
    files: ['packages/*/src/**/*.ts'], 
    languageOptions: {
      parserOptions: {
        // Use projectService to automatically find the nearest tsconfig.json
        projectService: true, 
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      '@typescript-eslint/prefer-optional-chain': 'error',
    },
  },

  // 8. Configuration Files (Disable strict TS checks)
  {
    files: ['*.config.{js,mjs,cjs}', 'eslint.config.mjs'],
    rules: {
      'no-undef': 'off',
      '@typescript-eslint/no-var-requires': 'off',
      '@typescript-eslint/no-require-imports': 'off'
    },
  },
];