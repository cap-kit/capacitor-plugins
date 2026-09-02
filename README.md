<div align="center">
  <img src="./assets/logo.png" alt="CapKit Logo" width="160" />

# CapKit Monorepo

**Enterprise-grade Capacitor 8 plugins focused on performance, security and architectural determinism.**

  <br/>

<a href="https://github.com/cap-kit/capacitor-plugins/actions"><img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" /></a>
<a href="https://pnpm.io/"><img src="https://img.shields.io/badge/maintained%20with-pnpm-cc00ff.svg?style=flat-square&logo=pnpm&logoColor=white" alt="pnpm" /></a>
<a href="https://github.com/changesets/changesets"><img src="https://img.shields.io/badge/maintained%20with-changesets-176de3.svg?style=flat-square&logo=git&logoColor=white" alt="changesets" /></a>
<a href="./LICENSE"><img src="https://img.shields.io/github/license/cap-kit/capacitor-plugins?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" /></a>
<a href="https://github.com/cap-kit/capacitor-plugins/commits/main"><img src="https://img.shields.io/github/last-commit/cap-kit/capacitor-plugins?style=flat-square&logo=git&logoColor=white&label=last%20commit&color=blue" alt="Last Commit" /></a>
<a href="https://turbo.build/"><img src="https://img.shields.io/badge/maintained%20with-turborepo-EF4444?style=flat-square&logo=turborepo&logoColor=white" alt="Turborepo" /></a>
<a href="https://github.com/renovatebot/renovate"><img src="https://img.shields.io/badge/renovate-enabled-brightgreen.svg?style=flat-square&logo=renovatebot&logoColor=white" alt="Renovate" /></a>

  <br/>

[Plugins](#-plugins-collection) •
[Demo App](#-demo-app) •
[Architecture](#-architecture--standards) •
[Getting Started](#-getting-started) •
[Contributing](#-contributing)
</div>

---

## ✨ Why CapKit

|                                    |                                                        |
| ---------------------------------- | ------------------------------------------------------ |
| ⚡ **Capacitor 8 Native-First**    | Built specifically for the latest Capacitor runtime    |
| 🏗 **Strict Monorepo Architecture** | Deterministic pnpm workspace + Turborepo orchestration |
| 🔒 **Security-Oriented**           | Integrity signals, SSL pinning, runtime hardening      |
| 🚀 **Automated Releases**          | Changesets + CI-driven publishing                      |
| 🤖 **Dependency Safety**           | Renovate with controlled production updates            |

---

## 📦 Plugins Collection

Each plugin is fully cross-platform:

- Web (TypeScript)
- iOS (Swift)
- Android (Kotlin)

Every plugin also ships a **runnable demo app** — see [Demo App](#-demo-app).

---

## 🧪 Demo App

Every plugin includes a **ready-to-run demo app** built from a shared **Angular + Ionic + Capacitor 8** template.

| Component       | Path                             | Description                                          |
| --------------- | -------------------------------- | ---------------------------------------------------- |
| Shared template | [`demo/`](./demo)                | Reference app used as the base for every plugin demo |
| Per-plugin app  | `packages/<plugin>/example-app/` | Generated demo wiring the local plugin via `file:..` |

The per-plugin `example-app/` is **generated**, never edited by hand. [`scripts/prepare-demo.ts`](./scripts/prepare-demo.ts) copies the shared `demo/` app, applies plugin-specific patches (Capacitor config, routes, native bootstrapping) and copies static pages from each plugin's `example-app/static/`.

### Regenerate a demo

```bash
pnpm install
tsx scripts/prepare-demo.ts <plugin-name>
```

### Run a demo

```bash
# From packages/<plugin>/example-app/
pnpm install
pnpm run build:ios      # build web + cap sync ios + open Xcode
pnpm run build:android  # build web + cap sync android + open Android Studio
```

---

<!-- PLUGINS_TABLE_START -->

> **Information:** All plugins are optimized for **Capacitor v8+** and tested for native parity.

<p align="center">
  📦 <strong>Total Plugins:</strong> 6 &nbsp;&bull;&nbsp; ⚡ <strong>Core:</strong> <img src="https://img.shields.io/badge/Capacitor-v8+-05f.svg?style=flat-square&logo=capacitor&logoColor=white" alt="Capacitor v8+" valign="middle" />
</p>

<table width="100%">
<tr>

<td align="center" width="33%" valign="top">

### <a href="./packages/integrity">🛡️ Integrity</a>

<a href="https://www.npmjs.com/package/@cap-kit/integrity"><code>@cap-kit/integrity</code></a>

<a href="https://www.npmjs.com/package/@cap-kit/integrity"><img src="https://img.shields.io/npm/v/@cap-kit/integrity?style=flat-square&label=npm&logo=npm" alt="npm version" /></a>
<a href="https://www.npmjs.com/package/@cap-kit/integrity"><img src="https://img.shields.io/npm/dm/@cap-kit/integrity?style=flat-square&label=downloads&logo=npm&color=orange" alt="downloads" /></a>

Runtime integrity and environment signal detection for Capacitor v8 applications.

<a href="./packages/integrity"><strong>Docs</strong></a> • 
<a href="https://www.npmjs.com/package/@cap-kit/integrity"><strong>NPM</strong></a>

</td>

<td align="center" width="33%" valign="top">

### <a href="./packages/people">👥 People</a>

<a href="https://www.npmjs.com/package/@cap-kit/people"><code>@cap-kit/people</code></a>

<a href="https://www.npmjs.com/package/@cap-kit/people"><img src="https://img.shields.io/npm/v/@cap-kit/people?style=flat-square&label=npm&logo=npm" alt="npm version" /></a>
<a href="https://www.npmjs.com/package/@cap-kit/people"><img src="https://img.shields.io/npm/dm/@cap-kit/people?style=flat-square&label=downloads&logo=npm&color=orange" alt="downloads" /></a>

Unified, high-performance contact management for Capacitor with zero-permission picking and capability-based access.

<a href="./packages/people"><strong>Docs</strong></a> • 
<a href="https://www.npmjs.com/package/@cap-kit/people"><strong>NPM</strong></a>

</td>

<td align="center" width="33%" valign="top">

### <a href="./packages/rank">⭐ Rank</a>

<a href="https://www.npmjs.com/package/@cap-kit/rank"><code>@cap-kit/rank</code></a>

<a href="https://www.npmjs.com/package/@cap-kit/rank"><img src="https://img.shields.io/npm/v/@cap-kit/rank?style=flat-square&label=npm&logo=npm" alt="npm version" /></a>
<a href="https://www.npmjs.com/package/@cap-kit/rank"><img src="https://img.shields.io/npm/dm/@cap-kit/rank?style=flat-square&label=downloads&logo=npm&color=orange" alt="downloads" /></a>

Unified Capacitor v8 plugin for native In-App Reviews and cross-platform Store navigation.

<a href="./packages/rank"><strong>Docs</strong></a> • 
<a href="https://www.npmjs.com/package/@cap-kit/rank"><strong>NPM</strong></a>

</td>
</tr>
<tr>

<td align="center" width="33%" valign="top">

### <a href="./packages/redsys">💳 Redsys</a>

<a href="https://www.npmjs.com/package/@cap-kit/redsys"><code>@cap-kit/redsys</code></a>

<a href="https://www.npmjs.com/package/@cap-kit/redsys"><img src="https://img.shields.io/npm/v/@cap-kit/redsys?style=flat-square&label=npm&logo=npm" alt="npm version" /></a>
<a href="https://www.npmjs.com/package/@cap-kit/redsys"><img src="https://img.shields.io/npm/dm/@cap-kit/redsys?style=flat-square&label=downloads&logo=npm&color=orange" alt="downloads" /></a>

Redsys InApp SDK bridge for Capacitor v8. Supports native Direct Payment and secure 3D Secure (3DS) WebView flows with unified cross-platform API and HMAC signature utilities.

<a href="./packages/redsys"><strong>Docs</strong></a> • 
<a href="https://www.npmjs.com/package/@cap-kit/redsys"><strong>NPM</strong></a>

</td>

<td align="center" width="33%" valign="top">

### <a href="./packages/settings">⚙️ Settings</a>

<a href="https://www.npmjs.com/package/@cap-kit/settings"><code>@cap-kit/settings</code></a>

<a href="https://www.npmjs.com/package/@cap-kit/settings"><img src="https://img.shields.io/npm/v/@cap-kit/settings?style=flat-square&label=npm&logo=npm" alt="npm version" /></a>
<a href="https://www.npmjs.com/package/@cap-kit/settings"><img src="https://img.shields.io/npm/dm/@cap-kit/settings?style=flat-square&label=downloads&logo=npm&color=orange" alt="downloads" /></a>

Capacitor plugin to open app and system settings on iOS and Android.

<a href="./packages/settings"><strong>Docs</strong></a> • 
<a href="https://www.npmjs.com/package/@cap-kit/settings"><strong>NPM</strong></a>

</td>

<td align="center" width="33%" valign="top">

### <a href="./packages/tls-fingerprint">🔒 Tls Fingerprint</a>

<a href="https://www.npmjs.com/package/@cap-kit/tls-fingerprint"><code>@cap-kit/tls-fingerprint</code></a>

<a href="https://www.npmjs.com/package/@cap-kit/tls-fingerprint"><img src="https://img.shields.io/npm/v/@cap-kit/tls-fingerprint?style=flat-square&label=npm&logo=npm" alt="npm version" /></a>
<a href="https://www.npmjs.com/package/@cap-kit/tls-fingerprint"><img src="https://img.shields.io/npm/dm/@cap-kit/tls-fingerprint?style=flat-square&label=downloads&logo=npm&color=orange" alt="downloads" /></a>

Runtime TLS leaf certificate SHA-256 fingerprint validation plugin for Capacitor (iOS & Android)

<a href="./packages/tls-fingerprint"><strong>Docs</strong></a> • 
<a href="https://www.npmjs.com/package/@cap-kit/tls-fingerprint"><strong>NPM</strong></a>

</td>
</tr>
</table>

<details>
<summary><strong>📱 Compact View (Mobile Friendly)</strong></summary>


### 🛡️ Integrity

`@cap-kit/integrity`

![npm](https://img.shields.io/npm/v/@cap-kit/integrity?style=flat-square&label=npm&logo=npm) ![downloads](https://img.shields.io/npm/dm/@cap-kit/integrity?style=flat-square&label=downloads&logo=npm&color=orange)

Runtime integrity and environment signal detection for Capacitor v8 applications.

[Docs](./packages/integrity) • [NPM](https://www.npmjs.com/package/@cap-kit/integrity)

---

### 👥 People

`@cap-kit/people`

![npm](https://img.shields.io/npm/v/@cap-kit/people?style=flat-square&label=npm&logo=npm) ![downloads](https://img.shields.io/npm/dm/@cap-kit/people?style=flat-square&label=downloads&logo=npm&color=orange)

Unified, high-performance contact management for Capacitor with zero-permission picking and capability-based access.

[Docs](./packages/people) • [NPM](https://www.npmjs.com/package/@cap-kit/people)

---

### ⭐ Rank

`@cap-kit/rank`

![npm](https://img.shields.io/npm/v/@cap-kit/rank?style=flat-square&label=npm&logo=npm) ![downloads](https://img.shields.io/npm/dm/@cap-kit/rank?style=flat-square&label=downloads&logo=npm&color=orange)

Unified Capacitor v8 plugin for native In-App Reviews and cross-platform Store navigation.

[Docs](./packages/rank) • [NPM](https://www.npmjs.com/package/@cap-kit/rank)

---

### 💳 Redsys

`@cap-kit/redsys`

![npm](https://img.shields.io/npm/v/@cap-kit/redsys?style=flat-square&label=npm&logo=npm) ![downloads](https://img.shields.io/npm/dm/@cap-kit/redsys?style=flat-square&label=downloads&logo=npm&color=orange)

Redsys InApp SDK bridge for Capacitor v8. Supports native Direct Payment and secure 3D Secure (3DS) WebView flows with unified cross-platform API and HMAC signature utilities.

[Docs](./packages/redsys) • [NPM](https://www.npmjs.com/package/@cap-kit/redsys)

---

### ⚙️ Settings

`@cap-kit/settings`

![npm](https://img.shields.io/npm/v/@cap-kit/settings?style=flat-square&label=npm&logo=npm) ![downloads](https://img.shields.io/npm/dm/@cap-kit/settings?style=flat-square&label=downloads&logo=npm&color=orange)

Capacitor plugin to open app and system settings on iOS and Android.

[Docs](./packages/settings) • [NPM](https://www.npmjs.com/package/@cap-kit/settings)

---

### 🔒 Tls Fingerprint

`@cap-kit/tls-fingerprint`

![npm](https://img.shields.io/npm/v/@cap-kit/tls-fingerprint?style=flat-square&label=npm&logo=npm) ![downloads](https://img.shields.io/npm/dm/@cap-kit/tls-fingerprint?style=flat-square&label=downloads&logo=npm&color=orange)

Runtime TLS leaf certificate SHA-256 fingerprint validation plugin for Capacitor (iOS & Android)

[Docs](./packages/tls-fingerprint) • [NPM](https://www.npmjs.com/package/@cap-kit/tls-fingerprint)

---

</details>

<!-- PLUGINS_TABLE_END -->

---

## 🧱 Architecture & Standards

This repository follows a **strict pnpm monorepo model**.

| Layer              | Stack                               |
| ------------------ | ----------------------------------- |
| Package Manager    | `pnpm 11+`                          |
| Task Orchestration | `Turborepo 2.x`                     |
| CI/CD              | GitHub Actions (`macos-latest`)     |
| Versioning         | Changesets                          |
| Minimum Capacitor  | `v8.0.0`                            |
| Demo Workspace     | `demo/` + `packages/*/example-app/` |

### Architectural Guarantees

- No isolated packages
- Centralized TypeScript configuration
- Deterministic CI (path-aware execution)
- Native parity enforced (Web / iOS / Android)
- No manual publishing
- Every plugin ships a runnable `example-app/` generated from the shared `demo/` template

---

## 🚀 Getting Started

### Requirements

- Node.js ≥ 24
- pnpm ≥ 11

### Install a plugin

```bash
pnpm add @cap-kit/<plugin-name>
npx cap sync
```

### Try a plugin demo

Each plugin ships a runnable demo app wired to the local source:

```bash
# 1. Build the plugin (run from its package directory)
cd packages/<plugin-name> && pnpm run build

# 2. Regenerate the demo app from the shared template
tsx scripts/prepare-demo.ts <plugin-name>

# 3. Run the generated app
cd packages/<plugin-name>/example-app
pnpm install
pnpm run build:ios   # or build:android
```

---

## 🤝 Contributing

Please follow Conventional Commits:

```
type(scope): Subject
```

Example:

```
feat(integrity): Add emulator detection
```

Rules:

- Scope is mandatory
- Subject must be Sentence-case
- Pull Requests must be written in English

See [CONTRIBUTING.md](./CONTRIBUTING.md).

---

## 📄 License

MIT — see [LICENSE](./LICENSE).
