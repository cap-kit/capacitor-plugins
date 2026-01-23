export default [
  // ---------------------------------------------------------------------------
  // 1. Capacitor Plugin Configuration
  // ---------------------------------------------------------------------------
  {
    input: 'dist/esm/index.js',
    output: [
      // IIFE Bundle for direct browser usage or CDNs
      {
        file: 'dist/plugin.js',
        format: 'iife',
        name: 'capacitorSSLPinning',
        globals: {
          '@capacitor/core': 'capacitorExports',
        },
        sourcemap: true,
        inlineDynamicImports: true,
      },
      // CJS Bundle for older bundlers and backward compatibility
      {
        file: 'dist/plugin.cjs.js',
        format: 'cjs',
        sourcemap: true,
        inlineDynamicImports: true,
      },
    ],
    external: ['@capacitor/core'],
  },

  // ---------------------------------------------------------------------------
  // 2. CLI Tool Configuration (ESM)
  // ---------------------------------------------------------------------------
  {
    input: 'dist/esm/cli/fingerprint.js',
    output: {
      file: 'dist/cli/fingerprint.js',
      format: 'es',
      banner: '#!/usr/bin/env node',
      sourcemap: true,
    },
    // Prevent bundling of Node.js built-ins and dependencies
    external: [
      'https',
      'crypto',
      'fs',
      'fs/promises',
      'path',
      'url',
      'module',
      'yargs',
      'yargs/helpers',
    ],
  },
];