/**
 * Update the plugins section in root README.md.
 *
 * This script scans the `packages/` directory, collects metadata from each
 * public package.json, and regenerates the plugins table between the
 * PLUGINS_TABLE_START and PLUGINS_TABLE_END markers.
 *
 * The operation is idempotent: running the script multiple times will always
 * produce the same result without duplicating content.
 *
 * This script is intended to be executed from the `scripts` directory.
 */

import * as fs from "fs";
import * as path from "path";

// Absolute path to the packages directory (scripts/ -> packages/)
const PACKAGES_DIR = path.join(__dirname, "../packages");

// Absolute path to the root README.md (scripts/ -> project root)
const ROOT_README = path.join(__dirname, "../README.md");

// Markers used to locate the plugins table section in README.md
// These must match the markers defined in reset-readme.ts
const START_MARKER = "<!-- PLUGINS_TABLE_START -->";
const END_MARKER = "<!-- PLUGINS_TABLE_END -->";

// GitHub repository details for contributors badge
// const GITHUB_OWNER = "cap-kit";
// const GITHUB_REPO = "capacitor-plugins";

/**
 * Converts a package name (e.g., "@cap-kit/test-plugin") into a readable title (e.g., "Test Plugin").
 */
function formatPluginName(packageName: string): string {
  // Remove scope (@cap-kit/)
  const cleanName = packageName.replace(/^@[\w-]+\//, "");

  // Icons map based on keywords in the plugin name.
  const iconMap: Record<string, string> = {
    integrity: "🛡️",
    rank: "⭐",
    settings: "⚙️",
    "ssl-pinning": "🔒",
    "tls-fingerprint": "🔒",
    version: "🏷️",
    package: "📦",
    logs: "📄",
    security: "🔐",
    testing: "🧪",
    camera: "📸",
    device: "📱",
    people: "👥",
    sensors: "🌡️",
    geocoder: "📍",
    redsys: "💳",
    auto: "🚗",
    storage: "💾",
    network: "📡",
    push: "🔔",
    auth: "🔑",
    analytics: "📊",
    payment: "💰",
  };

  const emoji = iconMap[cleanName] || "🔌";

  // Split by hyphen, capitalize first letter of each word, join with space
  const displayName = cleanName
    .split("-")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

  return `${emoji} ${displayName}`;
}

/**
 * Generates the HTML for a single plugin cell (TD).
 */
function generatePluginCard(pkgData: any, folderName: string): string {
  const name = pkgData.name;
  const displayName = formatPluginName(name);
  const description = pkgData.description || "No description provided.";

  // Badges (Flat Square + NPM Logo)
  const badgeVersion = `<a href="https://www.npmjs.com/package/${name}"><img src="https://img.shields.io/npm/v/${name}?style=flat-square&label=npm&logo=npm" alt="npm version" /></a>`;
  const badgeDownloads = `<a href="https://www.npmjs.com/package/${name}"><img src="https://img.shields.io/npm/dm/${name}?style=flat-square&label=downloads&logo=npm&color=orange" alt="downloads" /></a>`;

  return `
<td align="center" width="33%" valign="top">

### <a href="./packages/${folderName}">${displayName}</a>

<a href="https://www.npmjs.com/package/${name}"><code>${name}</code></a>

${badgeVersion}
${badgeDownloads}

${description}

<a href="./packages/${folderName}"><strong>Docs</strong></a> • 
<a href="https://www.npmjs.com/package/${name}"><strong>NPM</strong></a>

</td>`;
}

/**
 *
 */
function generatePluginStacked(pkgData: any, folderName: string): string {
  const name = pkgData.name;
  const displayName = formatPluginName(name);
  const description = pkgData.description || "No description provided.";

  const badgeVersion = `![npm](https://img.shields.io/npm/v/${name}?style=flat-square&label=npm&logo=npm)`;
  const badgeDownloads = `![downloads](https://img.shields.io/npm/dm/${name}?style=flat-square&label=downloads&logo=npm&color=orange)`;

  return `
### ${displayName}

\`${name}\`

${badgeVersion} ${badgeDownloads}

${description}

[Docs](./packages/${folderName}) • [NPM](https://www.npmjs.com/package/${name})

---`;
}

async function main(): Promise<void> {
  console.log("⚙️  Updating root README.md...");

  // Abort early if the packages directory does not exist
  if (!fs.existsSync(PACKAGES_DIR)) {
    console.log("No packages directory found. Skipping README update.");
    return;
  }

  // 1. Get all public plugins
  const plugins = fs
    .readdirSync(PACKAGES_DIR)
    .filter((dir) => fs.statSync(path.join(PACKAGES_DIR, dir)).isDirectory())
    .map((folder) => {
      const pkgPath = path.join(PACKAGES_DIR, folder, "package.json");
      if (!fs.existsSync(pkgPath)) return null;
      const data = JSON.parse(fs.readFileSync(pkgPath, "utf-8"));
      if (data.private !== false) return null;
      return { folder, data };
    })
    .filter((p): p is { folder: string; data: any } => p !== null)
    .sort((a, b) => a.data.name.localeCompare(b.data.name));

  // 2. Stats Line (Total Plugins | Weekly Downloads | Contributors)
  const totalPlugins = plugins.length;

  // Downloads Badge (Orange)
  const downloadsBadge = `<img src="https://img.shields.io/npm/dw/@cap-kit/test-plugin?style=flat-square&logo=npm&label=&color=orange" alt="Downloads" valign="middle" />`;

  // Contributors Badge (Green)
  // const contributorsBadge = `<a href="https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/graphs/contributors"><img src="https://img.shields.io/github/contributors/${GITHUB_OWNER}/${GITHUB_REPO}?style=flat-square&logo=github&label=&color=green" alt="Contributors" valign="middle" /></a>`;

  // Capacitor Version Badge (Blue)
  // const capVersionBadge = `<a href="https://capacitorjs.com"><img src="https://img.shields.io/badge/Capacitor-v8+-05f.svg?style=flat-square&logo=capacitor&logoColor=white" alt="Capacitor Compatibility" valign="middle" /></a>`;

  // Size/Performance Badge (Green)
  // const sizeBadge = `<img src="https://img.shields.io/badge/Performance-Lightweight-00e676.svg?style=flat-square&logo=speedtest&logoColor=white" alt="Performance" valign="middle" />`;

  // --- STATS LINE CONSTRUCTION ---

  // Add active elements to this array.
  const activeStatsElements = [
    `📦 <strong>Total Plugins:</strong> ${totalPlugins}`,
    `📈 <strong>Weekly Downloads:</strong> ${downloadsBadge}`,
    // `👥 <strong>Contributors:</strong> ${contributorsBadge}`,
    // `⚡ <strong>Core:</strong> ${capVersionBadge}`,
    // `💎 <strong>Quality:</strong> ${sizeBadge}`,
  ];

  // Join elements with a pipe separator. This avoids trailing pipes automatically.
  const statsLine = `> **Information:** All plugins are optimized for **Capacitor v8+** and tested for native parity.

<p align="center">
  ${activeStatsElements.join(" &nbsp;&bull;&nbsp; ")}
</p>`;

  // 3. Build Grid Rows (Chunks of 3)
  const rows: string[] = [];
  let currentRow: string[] = [];

  plugins.forEach((plugin, index) => {
    currentRow.push(generatePluginCard(plugin.data, plugin.folder));

    // If row is full (3 items) or it's the last item
    if (currentRow.length === 3 || index === plugins.length - 1) {
      while (currentRow.length < 3) currentRow.push('<td width="33%"></td>');
      rows.push(`<tr>\n${currentRow.join("\n")}\n</tr>`);
      currentRow = [];
    }
  });

  const gridContent =
    plugins.length > 0
      ? `<table width="100%">\n${rows.join("\n")}\n</table>`
      : `<p align="center"><em>No public plugins available.</em></p>`;

  // --- STACKED VERSION (Mobile Friendly) ---
  const stackedContent = plugins
    .map((plugin) => generatePluginStacked(plugin.data, plugin.folder))
    .join("\n");

  // 4. Inject into README.md
  const readmeContent = fs.readFileSync(ROOT_README, "utf-8");

  // Create a RegExp that matches everything between the markers
  const regex = new RegExp(
    `${START_MARKER.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}[\\s\\S]*?${END_MARKER.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}`,
  );

  // Fail explicitly if the markers are missing
  if (!regex.test(readmeContent)) {
    console.error("❌ Plugins markers not found.");
    process.exit(1);
  }

  // Inject Stats Line BEFORE the Grid
  const newContent = [
    START_MARKER,
    "",
    statsLine,
    "",
    gridContent,
    "",
    "<details>",
    "<summary><strong>📱 Compact View (Mobile Friendly)</strong></summary>",
    "",
    stackedContent,
    "",
    "</details>",
    "",
    END_MARKER,
  ].join("\n");

  fs.writeFileSync(ROOT_README, readmeContent.replace(regex, newContent));

  console.log(`✅ README.md updated with ${totalPlugins} plugins.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
