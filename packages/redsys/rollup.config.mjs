export default {
  input: 'dist/esm/index.js',
  output: [
    {
      file: 'dist/plugin.js',
      format: 'iife',
      name: 'capacitorRedsys',
      globals: {
        '@capacitor/core': 'capacitorExports',
      },
      sourcemap: true,
      inlineDynamicImports: true,
    },
    {
      file: 'dist/plugin.cjs',
      format: 'cjs',
      sourcemap: true,
      inlineDynamicImports: true,
    },
  ],
  external: ['@capacitor/core'],
};
