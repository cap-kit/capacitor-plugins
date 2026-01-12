import * as fs from "fs";
import * as path from "path";

const PACKAGES_DIR = path.join(__dirname, "../packages");
const ROOT_README = path.join(__dirname, "../README.md");

const TABLE_HEADER = `| Package | Version | Downloads | Description |
| :--- | :--- | :--- | :--- |`;

const START_MARKER = "";
const END_MARKER = "";

async function main() {
  console.log("⚙️  Updating Root README...");

  if (!fs.existsSync(PACKAGES_DIR)) {
    console.log("No packages directory found.");
    return;
  }

  const packages = fs.readdirSync(PACKAGES_DIR).filter((file) => {
    return fs.statSync(path.join(PACKAGES_DIR, file)).isDirectory();
  });

  const tableRows: string[] = [];

  for (const pkgFolder of packages) {
    const pkgJsonPath = path.join(PACKAGES_DIR, pkgFolder, "package.json");

    if (fs.existsSync(pkgJsonPath)) {
      const pkgData = JSON.parse(fs.readFileSync(pkgJsonPath, "utf-8"));
      if (pkgData.private) continue;

      const name = pkgData.name;
      const desc = pkgData.description || "No description provided.";

      const versionBadge = `[![npm](https://img.shields.io/npm/v/${name}?style=flat-square&label=)](https://www.npmjs.com/package/${name})`;
      const downloadsBadge = `[![downloads](https://img.shields.io/npm/dm/${name}?style=flat-square&label=)](https://www.npmjs.com/package/${name})`;

      const row = `| [\`${name}\`](./packages/${pkgFolder}) | ${versionBadge} | ${downloadsBadge} | ${desc} |`;
      tableRows.push(row);
    }
  }

  const newTableContent = `${START_MARKER}\n${TABLE_HEADER}\n${tableRows.join("\n")}\n${END_MARKER}`;
  const readmeContent = fs.readFileSync(ROOT_README, "utf-8");
  const regex = new RegExp(`${START_MARKER}[\\s\\S]*?${END_MARKER}`);

  if (!regex.test(readmeContent)) {
    console.error("❌ Markers not found in README.md.");
    process.exit(1);
  }

  const updatedReadme = readmeContent.replace(regex, newTableContent);
  fs.writeFileSync(ROOT_README, updatedReadme);
  console.log(`✅ README.md updated with ${tableRows.length} plugins.`);
}

main().catch(console.error);
