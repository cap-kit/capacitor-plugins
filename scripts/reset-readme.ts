/**
 * Reset the root README.md to a clean baseline state.
 *
 * This script overwrites README.md with a predefined template
 * that already includes the plugins table markers.
 * It is intended to be run from the `scripts` directory.
 */

import * as fs from "fs";
import * as path from "path";

// Resolve root README.md (scripts/ -> project root)
const ROOT_README = path.resolve(__dirname, "../README.md");

// Marker definitions (must match update-readme.ts)
const START_MARKER = "<!-- PLUGINS_TABLE_START -->";
const END_MARKER = "<!-- PLUGINS_TABLE_END -->";

// --- HEADER BADGES CONFIGURATION ---
const activeHeaderBadges = [
  // CI Status
  `<a href="https://github.com/cap-kit/capacitor-plugins/actions"><img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" /></a>`,

  // pnpm
  `<a href="https://pnpm.io/"><img src="https://img.shields.io/badge/maintained%20with-pnpm-cc00ff.svg?style=flat-square&logo=pnpm&logoColor=white" alt="pnpm" /></a>`,

  // Changesets
  `<a href="https://github.com/changesets/changesets"><img src="https://img.shields.io/badge/maintained%20with-changesets-176de3.svg?style=flat-square&logo=git&logoColor=white" alt="changesets" /></a>`,

  // License
  `<a href="./LICENSE"><img src="https://img.shields.io/github/license/cap-kit/capacitor-plugins?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" /></a>`,

  // Stars
  // `<a href="https://github.com/cap-kit/capacitor-plugins/stargazers"><img src="https://img.shields.io/github/stars/cap-kit/capacitor-plugins?style=flat-square&logo=github&logoColor=white" alt="Stars" /></a>`,

  // Last Commit
  `<a href="https://github.com/cap-kit/capacitor-plugins/commits/main"><img src="https://img.shields.io/github/last-commit/cap-kit/capacitor-plugins?style=flat-square&logo=git&logoColor=white&label=last%20commit&color=blue" alt="Last Commit" /></a>`,

  // --- ADD MORE BADGES AS NEEDED ---

  // Turborepo (High performance build system)
  `<a href="https://turbo.build/"><img src="https://img.shields.io/badge/maintained%20with-turborepo-EF4444?style=flat-square&logo=turborepo&logoColor=white" alt="Turborepo" /></a>`,

  // Prettier (Code Style)
  // `<a href="https://prettier.io"><img src="https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat-square&logo=prettier&logoColor=white" alt="Prettier" /></a>`,

  // Renovate (Dependency Automation)
  `<a href="https://github.com/renovatebot/renovate"><img src="https://img.shields.io/badge/renovate-enabled-brightgreen.svg?style=flat-square&logo=renovatebot&logoColor=white" alt="Renovate" /></a>`,

  // --- TECH STACK (LANGUAGES) ---

  // Force a line break before languages
  // `<br />`,

  // Swift 5.9 (Orange)
  // `<img src="https://img.shields.io/badge/Swift-5.9-F05138?style=flat-square&logo=swift&logoColor=white" alt="Swift 5.9" />`,

  // Kotlin 2.x (Purple)
  // `<img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin 2.x" />`,

  // TypeScript 5.9 (Blue)
  // Note: Current stable is ~5.4, verify if 5.9 is intended, otherwise use "5.x"
  // `<img src="https://img.shields.io/badge/TypeScript-5.9-3178C6?style=flat-square&logo=typescript&logoColor=white" alt="TypeScript 5.9" />`,

  // --- PLATFORM SUPPORT ---
  // Force a line break before platforms
  // `<br />`,

  // iOS (Black + Apple Logo)
  // `<img src="https://img.shields.io/badge/iOS-Support-000000?style=flat-square&logo=apple&logoColor=white" alt="iOS Support" />`,

  // Android (Green + Android Logo)
  // `<img src="https://img.shields.io/badge/Android-Support-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android Support" />`,

  // Web (Orange + Chrome Logo for "Browser")
  // Using 'google-chrome' logo as a generic browser sphere instead of HTML5 shield
  // `<img src="https://img.shields.io/badge/Web-Support-orange?style=flat-square&logo=google-chrome&logoColor=white" alt="Web Support" />`,
];

// Base README content (clean state)
const content = `<div align="center">
  <img src="./assets/logo.png" alt="CapKit Logo" width="160" />

  # CapKit Monorepo

  **Enterprise-grade Capacitor 8 plugins focused on performance, security and architectural determinism.**

  <br/>

  ${activeHeaderBadges.join("\n  ")}

  <br/>

  [Plugins](#-plugins-collection) •
  [Architecture](#-architecture--standards) •
  [Getting Started](#-getting-started) •
  [Contributing](#-contributing)
</div>

---

## ✨ Why CapKit

| | |
|--|--|
| ⚡ **Capacitor 8 Native-First** | Built specifically for the latest Capacitor runtime |
| 🏗 **Strict Monorepo Architecture** | Deterministic pnpm workspace + Turborepo orchestration |
| 🔒 **Security-Oriented** | Integrity signals, SSL pinning, runtime hardening |
| 🚀 **Automated Releases** | Changesets + CI-driven publishing |
| 🤖 **Dependency Safety** | Renovate with controlled production updates |

---

## 📦 Plugins Collection

Each plugin is fully cross-platform:

- Web (TypeScript)
- iOS (Swift)
- Android (Kotlin)

---

${START_MARKER}

${END_MARKER}

---

## 🧱 Architecture & Standards

This repository follows a **strict pnpm monorepo model**.

| Layer | Stack |
|-------|-------|
| Package Manager | \`pnpm 10+\` |
| Task Orchestration | \`Turborepo 2.x\` |
| CI/CD | GitHub Actions (\`macos-latest\`) |
| Versioning | Changesets |
| Minimum Capacitor | \`v8.0.0\` |

### Architectural Guarantees

- No isolated packages
- Centralized TypeScript configuration
- Deterministic CI (path-aware execution)
- Native parity enforced (Web / iOS / Android)
- No manual publishing

---

## 🚀 Getting Started

### Requirements

- Node.js ≥ 24
- pnpm ≥ 10

### Install a plugin

\`\`\`bash
pnpm add @cap-kit/<plugin-name>
npx cap sync
\`\`\`

---

## 🤝 Contributing

Please follow Conventional Commits:

\`\`\`
type(scope): Subject
\`\`\`

Example:

\`\`\`
feat(integrity): Add emulator detection
\`\`\`

Rules:

- Scope is mandatory
- Subject must be Sentence-case
- Pull Requests must be written in English

See [CONTRIBUTING.md](./CONTRIBUTING.md).

---

## 📄 License

MIT — see [LICENSE](./LICENSE).
`;

function main(): void {
  fs.writeFileSync(ROOT_README, content, { encoding: "utf-8" });
  console.log("✅ README.md reset to baseline state.");
}

main();
