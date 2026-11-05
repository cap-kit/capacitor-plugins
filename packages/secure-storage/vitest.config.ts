import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'node',
    // Only the web-layer unit tests; example-app has its own Angular toolchain.
    include: ['tests/**/*.test.ts'],
  },
});
