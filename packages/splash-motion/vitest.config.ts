import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    // Only the web-layer unit tests; example-app has its own Angular toolchain.
    include: ['tests/**/*.test.ts'],
    // The web implementation uses timers for fade/min-hold scheduling; tests
    // replace global timers, so a hanging timer must fail the run loudly.
    teardownTimeout: 10_000,
  },
});
