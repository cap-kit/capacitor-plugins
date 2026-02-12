const fs = require("fs");
const path = require("path");

// Automatically read folders inside 'packages' and 'apps'
const packages = fs.readdirSync(path.resolve(__dirname, "packages"));
const apps = fs.existsSync(path.resolve(__dirname, "apps"))
  ? fs.readdirSync(path.resolve(__dirname, "apps"))
  : [];

// Expanded static scopes for better granularity
const scopes = [
  ...packages,
  ...apps,
  "root",
  "deps",
  "release",
  "ci",
  "gh-actions",
  "repo",
  "docs",
  "scripts",
  "style",
  "infra",
];

module.exports = {
  /*
   * Resolve and load @commitlint/config-conventional from node_modules.
   * Referenced packages must be installed
   */
  extends: ["@commitlint/config-conventional"],
  /*
   * Any rules defined here will override rules from @commitlint/config-conventional
   */
  rules: {
    // The Scope must be one of your plugins, or root/deps/release
    "scope-enum": [2, "always", scopes],

    // The Scope cannot be empty (e.g. for global changes)
    "scope-empty": [2, "never"],

    // The rules you added (they are fine, but the defaults are already enough)
    // I removed 'type-enum' because 'config-conventional' already has that list by default!

    "subject-case": [2, "always", "sentence-case"], // Tip: sentence-case is more readable than lower-case
    "subject-max-length": [2, "always", 100], // 72 is a bit tight, 100 is more comfortable nowadays
  },
};
