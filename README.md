<div align="center">
  <img src="./assets/logo.png" alt="CapKit Logo" width="200" />
  <h1>CapKit Monorepo</h1>

  <p>
    <strong>High-quality, production-ready Capacitor plugins crafted with architectural precision.</strong>
  </p>

  <p>
    <a href="https://github.com/cap-kit/capacitor-plugins/actions">
      <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&style=flat-square" alt="CI Status" />
    </a>
    <a href="https://www.npmjs.com/search?q=%40cap-kit">
      <img src="https://img.shields.io/npm/l/@cap-kit/test-plugin?style=flat-square" alt="License" />
    </a>
    <a href="https://github.com/cap-kit/capacitor-plugins/stargazers">
      <img src="https://img.shields.io/github/stars/cap-kit/capacitor-plugins?style=flat-square" alt="Stars" />
    </a>
  </p>
</div>

---

## 📦 Plugins Collection

The **CapKit** suite ensures parity between iOS, Android, and Web, strictly following the Capacitor v8+ architecture.

| Package                                          | Version                                                                                                                                  | Downloads                                                                                                                                       | Description                                             |
| :----------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------ |
| [`@cap-kit/test-plugin`](./packages/test-plugin) | [![npm](https://img.shields.io/npm/v/@cap-kit/test-plugin?style=flat-square&label=)](https://www.npmjs.com/package/@cap-kit/test-plugin) | [![downloads](https://img.shields.io/npm/dm/@cap-kit/test-plugin?style=flat-square&label=)](https://www.npmjs.com/package/@cap-kit/test-plugin) | A robust test plugin to verify the CapKit architecture. |

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

We welcome contributions! Please see our [CONTRIBUTING.md](https://www.google.com/search?q=./CONTRIBUTING.md) for details on how to set up the local environment and submit PRs.

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat(scope): add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

CapKit is [MIT licensed](https://www.google.com/search?q=./LICENSE).
