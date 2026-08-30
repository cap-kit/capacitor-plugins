#!/usr/bin/env node

import fs from 'fs';
import path from 'path';
import process from 'process';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const packageJson = require('../package.json');
const version: string = packageJson.version;

const content: string = `export const APP_VERSION = '${version}';
`;

const filePath: string = path.join(process.cwd(), 'src', 'app', 'version.ts');

fs.writeFileSync(filePath, content, 'utf8');

console.log(`Wrote version ${version} to ${filePath}`);
