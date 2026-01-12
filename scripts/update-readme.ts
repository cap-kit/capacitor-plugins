/**
 * Update the plugins table inside the root README.md.
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

// Markdown table header for the plugins list
const TABLE_HEADER = `| Package | Version | Downloads | Description |
| :--- | :--- | :--- | :--- |`;

/**
 * Escape a string so it can be safely used inside a RegExp constructor.
 */
function escapeRegExp(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function main(): Promise<void> {
  console.log("⚙️  Updating root README.md...");

  // Abort early if the packages directory does not exist
  if (!fs.existsSync(PACKAGES_DIR)) {
    console.log("No packages directory found. Skipping README update.");
    return;
  }

  // Collect all first-level package directories and sort them alphabetically
  const packages = fs
    .readdirSync(PACKAGES_DIR)
    .filter((dir) => fs.statSync(path.join(PACKAGES_DIR, dir)).isDirectory())
    .sort();

  const tableRows: string[] = [];

  // Iterate through each package folder and extract metadata
  for (const pkgFolder of packages) {
    const pkgJsonPath = path.join(PACKAGES_DIR, pkgFolder, "package.json");

    // Skip folders without a package.json
    if (!fs.existsSync(pkgJsonPath)) continue;

    const pkgData = JSON.parse(fs.readFileSync(pkgJsonPath, "utf-8"));

    // Skip private packages
    if (pkgData.private) continue;

    const name: string = pkgData.name;
    const description: string =
      pkgData.description || "No description provided.";

    // npm version and download badges
    const versionBadge = `[![npm](https://img.shields.io/npm/v/${name}?style=flat-square&label=)](https://www.npmjs.com/package/${name})`;
    const downloadsBadge = `[![downloads](https://img.shields.io/npm/dm/${name}?style=flat-square&label=)](https://www.npmjs.com/package/${name})`;

    tableRows.push(
      `| [\`${name}\`](./packages/${pkgFolder}) | ${versionBadge} | ${downloadsBadge} | ${description} |`,
    );
  }

  // Build the full table content to be injected between the markers
  const newTableContent = [
    START_MARKER,
    TABLE_HEADER,
    ...tableRows,
    END_MARKER,
  ].join("\n");

  // Read the current README.md content
  const readmeContent = fs.readFileSync(ROOT_README, "utf-8");

  // Create a RegExp that matches everything between the markers
  const regex = new RegExp(
    `${escapeRegExp(START_MARKER)}[\\s\\S]*?${escapeRegExp(END_MARKER)}`,
  );

  // Fail explicitly if the markers are missing
  if (!regex.test(readmeContent)) {
    console.error("❌ Plugins table markers not found in README.md.");
    process.exit(1);
  }

  // Replace the existing plugins table with the newly generated one
  const updatedReadme = readmeContent.replace(regex, newTableContent);
  fs.writeFileSync(ROOT_README, updatedReadme);

  console.log(`✅ README.md updated with ${tableRows.length} packages.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
