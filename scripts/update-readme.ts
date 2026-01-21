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
const TABLE_HEADER = `| Name | Package | Version | Downloads | Description |
| :--- | :--- | :--- | :--- | :--- |`;

/**
 * Escape a string so it can be safely used inside a RegExp constructor.
 */
function escapeRegExp(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Converts a package name (e.g., "@cap-kit/test-plugin") into a readable title (e.g., "Test Plugin").
 */
function formatPluginName(packageName: string): string {
  // Remove scope (@cap-kit/)
  const cleanName = packageName.replace(/^@[\w-]+\//, "");
  // Split by hyphen, capitalize first letter of each word, join with space
  return cleanName
    .split("-")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
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

    const packageName: string = pkgData.name; // @cap-kit/test-plugin
    const displayName = formatPluginName(packageName); // Test Plugin
    const description: string =
      pkgData.description || "No description provided.";

    // UPDATED: Badges with Icons (Flat Square)
    // Version: Blue + npm logo
    const versionBadge = `[![npm](https://img.shields.io/npm/v/${packageName}?style=flat-square&logo=npm&color=blue)](https://www.npmjs.com/package/${packageName})`;
    // Downloads: Orange + npm logo (or generic download icon if preferred, keeping npm for consistency)
    const downloadsBadge = `[![downloads](https://img.shields.io/npm/dm/${packageName}?style=flat-square&logo=npm&color=orange)](https://www.npmjs.com/package/${packageName})`;

    // Row format: | Name (Link) | Package (`code`) | Version | Downloads | Description |
    tableRows.push(
      `| [**${displayName}**](./packages/${pkgFolder}) | \`${packageName}\` | ${versionBadge} | ${downloadsBadge} | ${description} |`,
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
