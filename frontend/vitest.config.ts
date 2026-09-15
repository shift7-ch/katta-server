import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => tag === 'altcha-widget'
        }
      }
    })
  ],
  test: {
    environment: 'happy-dom',
    coverage: {
      reporter: ['html', 'text-summary', 'lcov']
    }
  }
});
