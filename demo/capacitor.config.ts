import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'capkit.plugin.demo',
  appName: 'CapKit Plugin',
  webDir: 'dist/capkit-plugin-demo/browser',
  server: {
    cleartext: false,
  },
  plugins: {
    CapacitorHttp: {
      /**
       * Enables Capacitor's native HTTP client.
       * This must be enabled for SSL pinning to work.
       * The SSLPinning plugin hooks into CapacitorHttp
       * to intercept and validate SSL connections.
       */
      enabled: true,
    },
    SystemBars: {
      style: 'DEFAULT', // 'DARK' | 'LIGHT' | 'DEFAULT'
      insetsHandling: 'native', // 'css' | 'native' | 'disable'
    },
  },
};

export default config;
