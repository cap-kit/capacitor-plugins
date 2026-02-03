<div align="center">
  <img src="./assets/logo.png" alt="CapKit Logo" width="200" />
  <h1>CapKit Monorepo</h1>

  <p>
    <strong>High-quality, production-ready Capacitor plugins crafted with architectural precision.</strong>
  </p>

  <p>
    <a href="https://github.com/cap-kit/capacitor-plugins/actions"><img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" /></a>
    <a href="https://pnpm.io/"><img src="https://img.shields.io/badge/maintained%20with-pnpm-cc00ff.svg?style=flat-square&logo=pnpm&logoColor=white" alt="pnpm" /></a>
    <a href="https://github.com/changesets/changesets"><img src="https://img.shields.io/badge/maintained%20with-changesets-176de3.svg?style=flat-square&logo=git&logoColor=white" alt="changesets" /></a>
    <a href="./LICENSE"><img src="https://img.shields.io/github/license/cap-kit/capacitor-plugins?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" /></a>
    <a href="https://github.com/cap-kit/capacitor-plugins/commits/main"><img src="https://img.shields.io/github/last-commit/cap-kit/capacitor-plugins?style=flat-square&logo=git&logoColor=white&label=last%20commit&color=blue" alt="Last Commit" /></a>
  </p>
</div>

---

## 📦 Plugins Collection

The **CapKit** suite ensures parity between iOS, Android, and Web, strictly following the Capacitor v8+ architecture.

> **ℹ️ Note:** Individual plugins do not include standalone example apps.
> A centralized **CapKit Playground App** is currently in development to demonstrate the entire suite (Coming Soon).

<!-- PLUGINS_TABLE_START -->

Each package maintains its own documentation and setup guide.
**Click on the plugin Name** below to navigate to the specific installation instructions.

Here is the current list of available plugins:

<p>
  <strong>Total Plugins:</strong> 3 | <strong>Weekly Downloads:</strong> <img src="https://img.shields.io/npm/dw/@cap-kit/test-plugin?style=flat-square&logo=npm&label=&color=orange" alt="Downloads" valign="middle" />
</p>
<br />

<table>
<tr>

<td align="center" width="33%">
  <h3><a href="./packages/settings">Settings</a></h3>
  <p><code>@cap-kit/settings</code></p>
  <p>
    <a href="https://www.npmjs.com/package/@cap-kit/settings"><img src="https://img.shields.io/npm/v/@cap-kit/settings?style=flat-square&color=blue&label=npm&logo=npm" alt="npm version"></a>
    <a href="https://www.npmjs.com/package/@cap-kit/settings"><img src="https://img.shields.io/npm/dm/@cap-kit/settings?style=flat-square&color=orange&label=downloads&logo=npm" alt="downloads"></a>
  </p>
  <p>Capacitor plugin to open app and system settings on iOS and Android.</p>
  <p>
    <a href="./packages/settings"><strong>Documentation</strong></a> | 
    <a href="https://www.npmjs.com/package/@cap-kit/settings"><strong>NPM</strong></a>
  </p>
</td>

<td align="center" width="33%">
  <h3><a href="./packages/ssl-pinning">Ssl Pinning</a></h3>
  <p><code>@cap-kit/ssl-pinning</code></p>
  <p>
    <a href="https://www.npmjs.com/package/@cap-kit/ssl-pinning"><img src="https://img.shields.io/npm/v/@cap-kit/ssl-pinning?style=flat-square&color=blue&label=npm&logo=npm" alt="npm version"></a>
    <a href="https://www.npmjs.com/package/@cap-kit/ssl-pinning"><img src="https://img.shields.io/npm/dm/@cap-kit/ssl-pinning?style=flat-square&color=orange&label=downloads&logo=npm" alt="downloads"></a>
  </p>
  <p>Capacitor plugin for runtime SSL certificate fingerprint pinning on iOS and Android</p>
  <p>
    <a href="./packages/ssl-pinning"><strong>Documentation</strong></a> | 
    <a href="https://www.npmjs.com/package/@cap-kit/ssl-pinning"><strong>NPM</strong></a>
  </p>
</td>

<td align="center" width="33%">
  <h3><a href="./packages/test-plugin">Test Plugin</a></h3>
  <p><code>@cap-kit/test-plugin</code></p>
  <p>
    <a href="https://www.npmjs.com/package/@cap-kit/test-plugin"><img src="https://img.shields.io/npm/v/@cap-kit/test-plugin?style=flat-square&color=blue&label=npm&logo=npm" alt="npm version"></a>
    <a href="https://www.npmjs.com/package/@cap-kit/test-plugin"><img src="https://img.shields.io/npm/dm/@cap-kit/test-plugin?style=flat-square&color=orange&label=downloads&logo=npm" alt="downloads"></a>
  </p>
  <p>Architectural reference and boilerplate for Cap-Kit plugins.</p>
  <p>
    <a href="./packages/test-plugin"><strong>Documentation</strong></a> | 
    <a href="https://www.npmjs.com/package/@cap-kit/test-plugin"><strong>NPM</strong></a>
  </p>
</td>
</tr>
</table>
<!-- PLUGINS_TABLE_END -->

## 🛠️ Architecture

This repository operates as a strict **pnpm monorepo**.

- **Core:** Capacitor v8+
- **Languages:** TypeScript, Swift, Kotlin
- **Package Manager:** `pnpm`
- **CI/CD:** GitHub Actions (macOS-latest runners)

## 🚀 Getting Started

### Prerequisites

- Node.js 24+
- pnpm 10+ (`npm install -g pnpm`)

### Installation

To install a specific plugin into your Capacitor app:

```bash
pnpm add @cap-kit/test-plugin
npx cap sync
```

## 🤝 Contributing

We welcome contributions! Please see our [CONTRIBUTING.md](./CONTRIBUTING.md) for details on how to set up the local environment and submit PRs.

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat(scope): Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

CapKit is [MIT licensed](./LICENSE).
