import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Reconstruct __dirname, which does not exist in ESM
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Correct path to the compiled CLI
const cliPath = path.resolve(__dirname, '../dist/cli/fingerprint.js');

try {
  if (fs.existsSync(cliPath)) {
    // 1. Read the file contents
    let content = fs.readFileSync(cliPath, 'utf8');

    // 2. Add shebang if missing
    if (!content.startsWith('#!/usr/bin/env node')) {
      content = '#!/usr/bin/env node\n' + content;
      fs.writeFileSync(cliPath, content);
      console.log('✅ Shebang added to CLI.');
    }

    // 3. Make the file executable
    fs.chmodSync(cliPath, '755');
    console.log('✅ CLI permissions set to 755.');
  } else {
    console.error(`❌ CLI file not found at: ${cliPath}`);
    // Do not fail the build if the file does not exist yet, just warn
  }
} catch (err) {
  console.error('❌ Error setting permissions:', err);
  process.exit(1);
}
