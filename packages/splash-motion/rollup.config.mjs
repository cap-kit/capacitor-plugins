export default {
  input: 'dist/esm/index.js',
  output: [
    {
      file: 'dist/plugin.js',
      format: 'iife',
      name: 'capacitorSplashMotion',
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
  // @lottiefiles/dotlottie-web stays external: it is loaded lazily via
  // dynamic import by the web implementation and must never be inlined
  // (the IIFE/CDN build cannot resolve it; bundling consumers chunk-split it).
  external: ['@capacitor/core', '@lottiefiles/dotlottie-web'],
};
