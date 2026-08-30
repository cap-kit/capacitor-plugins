#!/usr/bin/env node

import fs from 'fs';
import path from 'path';
import process from 'process';

// Configuration
const PLACEHOLDER_TEAM_ID: string = 'XXXXXXXXXX';
const PROJECT_FILE: string = 'ios/App/App.xcodeproj/project.pbxproj';

// Resolve path relative to the process execution (root of workspace)
const filePath: string = path.resolve(process.cwd(), PROJECT_FILE);

// Check file existence
if (!fs.existsSync(filePath)) {
  console.log(`⚠️  Project file not found: ${filePath}`);
  // Exit gracefully, as this might be running in an environment without the example app
  process.exit(0);
}

console.log(`🔧 Sanitizing ${filePath}...`);

try {
  const content: string = fs.readFileSync(filePath, 'utf8');

  // Find all DEVELOPMENT_TEAM entries
  // Regex looks for standard 10-char alphanumeric Team IDs
  const teamIdRegex: RegExp = /DEVELOPMENT_TEAM = ([A-Z0-9]{10});/g;
  const matches = [...content.matchAll(teamIdRegex)];

  if (matches.length === 0) {
    console.log(`ℹ️  No DEVELOPMENT_TEAM found in ${filePath}`);
    process.exit(0);
  }

  // Replace with placeholder
  const sanitizedContent: string = content.replace(
    teamIdRegex,
    `DEVELOPMENT_TEAM = ${PLACEHOLDER_TEAM_ID};`,
  );

  fs.writeFileSync(filePath, sanitizedContent, 'utf8');

  console.log(`✅ Sanitized ${matches.length} DEVELOPMENT_TEAM entries in ${filePath}`);
} catch (error: unknown) {
  const errorMessage = error instanceof Error ? error.message : String(error);
  console.error(`❌ Error sanitizing ${filePath}:`, errorMessage);
  process.exit(1);
}

console.log('\n✨ Sanitization complete!');
console.log('📝 Team IDs have been replaced with placeholder values.');
console.log('🔒 Your actual Team ID will not be exposed in the published package.\n');
