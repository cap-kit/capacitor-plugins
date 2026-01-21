<div align="center">
  <img src="./assets/logo.png" alt="CapKit Logo" width="200" />
  <h1>CapKit Monorepo</h1>

  <p>
    <strong>High-quality, production-ready Capacitor plugins crafted with architectural precision.</strong>
  </p>

  <p>
    <a href="https://github.com/cap-kit/capacitor-plugins/actions">
      <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
    </a>
    <a href="https://pnpm.io/">
      <img src="https://img.shields.io/badge/maintained%20with-pnpm-cc00ff.svg?style=flat-square&logo=pnpm&logoColor=white" alt="pnpm" />
    </a>
    <a href="https://github.com/changesets/changesets">
      <img src="https://img.shields.io/badge/maintained%20with-changesets-176de3.svg?style=flat-square&logo=changeset&logoColor=white" alt="changesets" />
    </a>
    <a href="./LICENSE">
      <img src="https://img.shields.io/github/license/cap-kit/capacitor-plugins?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
    </a>
    <a href="https://github.com/cap-kit/capacitor-plugins/stargazers">
      <img src="https://img.shields.io/github/stars/cap-kit/capacitor-plugins?style=flat-square&logo=github&logoColor=white" alt="Stars" />
    </a>
  </p>
</div>

---

## 📦 Plugins Collection

The **CapKit** suite ensures parity between iOS, Android, and Web, strictly following the Capacitor v8+ architecture.

> **ℹ️ Note:** Individual plugins do not include standalone example apps.
> A centralized **CapKit Playground App** is currently in development to demonstrate the entire suite (Coming Soon).

Each package maintains its own documentation and setup guide.
**Click on the plugin Name** in the table below to navigate to the specific installation instructions.

Here is the current list of available plugins:

<!-- PLUGINS_TABLE_START -->

| Name                                      | Package                | Version                                                                                                                                               | Downloads                                                                                                                                                      | Description                                                  |
| :---------------------------------------- | :--------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------- |
| [**Test Plugin**](./packages/test-plugin) | `@cap-kit/test-plugin` | [![npm](https://img.shields.io/npm/v/@cap-kit/test-plugin?style=flat-square&logo=npm&color=blue)](https://www.npmjs.com/package/@cap-kit/test-plugin) | [![downloads](https://img.shields.io/npm/dm/@cap-kit/test-plugin?style=flat-square&logo=npm&color=orange)](https://www.npmjs.com/package/@cap-kit/test-plugin) | Architectural reference and boilerplate for Cap-Kit plugins. |

<!-- PLUGINS_TABLE_END -->

## 🛠️ Architecture

This repository operates as a strict **pnpm monorepo**.

- **Core:** Capacitor v8+
- **Languages:** TypeScript, Swift, Kotlin
- **Package Manager:** `pnpm`
- **CI/CD:** GitHub Actions (macOS-latest runners)

## 🚀 Getting Started

### Prerequisites

- Node.js 20+
- pnpm 9+ (`npm install -g pnpm`)

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
