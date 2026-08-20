import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'favicon.svg', 'apple-touch-icon.png'],
      manifest: {
        name: 'FlowOps',
        short_name: 'FlowOps',
        description: 'Self-hosted operational workflow and task management.',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        background_color: '#f4f7fb',
        theme_color: '#12344c',
        icons: [
          { src: '/flowops-icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
          { src: '/flowops-icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
          { src: '/flowops-maskable-192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
          { src: '/flowops-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        navigateFallback: '/index.html',
        navigateFallbackDenylist: [/^\/api\//],
        globPatterns: ['**/*.{js,css,html,ico,png,svg,webmanifest}'],
        runtimeCaching: [{
          urlPattern: ({ url }) => url.pathname.startsWith('/api/'),
          handler: 'NetworkOnly',
          method: 'GET',
        }],
      },
      devOptions: { enabled: false },
    }),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
