import type { CapacitorConfig } from '@capacitor/cli';
import { KeyboardResize } from '@capacitor/keyboard';

const config: CapacitorConfig = {
  appId: 'capkit.plugin.demo',
  appName: 'capkit-plugin-demo',
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
      insetsHandling: 'css', // 'css' | 'disable'
      style: 'DEFAULT', // 'DARK' | 'LIGHT' | 'DEFAULT'
    },
    Keyboard: {
      resize: KeyboardResize.None,
    },
  },
  cordova: {},
};

export default config;
