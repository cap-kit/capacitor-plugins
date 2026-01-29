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
  // `<a href="https://turbo.build/"><img src="https://img.shields.io/badge/maintained%20with-turborepo-EF4444?style=flat-square&logo=turborepo&logoColor=white" alt="Turborepo" /></a>`,

  // Prettier (Code Style)
  // `<a href="https://prettier.io"><img src="https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat-square&logo=prettier&logoColor=white" alt="Prettier" /></a>`,

  // Renovate (Dependency Automation)
  // `<a href="https://github.com/renovatebot/renovate"><img src="https://img.shields.io/badge/renovate-enabled-brightgreen.svg?style=flat-square&logo=renovatebot&logoColor=white" alt="Renovate" /></a>`,

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
  <img src="./assets/logo.png" alt="CapKit Logo" width="200" />
  <h1>CapKit Monorepo</h1>

  <p>
    <strong>High-quality, production-ready Capacitor plugins crafted with architectural precision.</strong>
  </p>

  <p>
    ${activeHeaderBadges.join("\n    ")}
  </p>
</div>

---

## 📦 Plugins Collection

The **CapKit** suite ensures parity between iOS, Android, and Web, strictly following the Capacitor v8+ architecture.

> **ℹ️ Note:** Individual plugins do not include standalone example apps.
> A centralized **CapKit Playground App** is currently in development to demonstrate the entire suite (Coming Soon).

${START_MARKER}
${END_MARKER}

## 🛠️ Architecture

This repository operates as a strict **pnpm monorepo**.

- **Core:** Capacitor v8+
- **Languages:** TypeScript, Swift, Kotlin
- **Package Manager:** \`pnpm\`
- **CI/CD:** GitHub Actions (macOS-latest runners)

## 🚀 Getting Started

### Prerequisites

- Node.js 24+
- pnpm 10+ (\`npm install -g pnpm\`)

### Installation

To install a specific plugin into your Capacitor app:

\`\`\`bash
pnpm add @cap-kit/test-plugin
npx cap sync
\`\`\`

## 🤝 Contributing

We welcome contributions! Please see our [CONTRIBUTING.md](./CONTRIBUTING.md) for details on how to set up the local environment and submit PRs.

1. Fork the repo
2. Create your feature branch (\`git checkout -b feature/amazing-feature\`)
3. Commit your changes (\`git commit -m 'feat(scope): Add some amazing feature'\`)
4. Push to the branch (\`git push origin feature/amazing-feature\`)
5. Open a Pull Request

## 📄 License

CapKit is [MIT licensed](./LICENSE).
`;

function main(): void {
  fs.writeFileSync(ROOT_README, content, { encoding: "utf-8" });
  console.log("✅ README.md reset to baseline state.");
}

main();
