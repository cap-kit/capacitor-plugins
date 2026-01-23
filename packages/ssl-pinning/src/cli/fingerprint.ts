import crypto from 'crypto';
import fs from 'fs/promises';
import https from 'https';
import { createRequire } from 'module';
import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';

const require = createRequire(import.meta.url);
const pkg = require('../../package.json');

type CertificateInfo = {
  domain: string;
  subject: Record<string, string>;
  issuer: Record<string, string>;
  validFrom: string;
  validTo: string;
  fingerprint: string;
};

type Mode = 'single' | 'multi';
type Format = 'json' | 'fingerprints' | 'capacitor' | 'capacitor-plugin' | 'capacitor-json';

async function getCertificate(domain: string, insecure: boolean): Promise<CertificateInfo> {
  return new Promise((resolve, reject) => {
    const options: https.RequestOptions = {
      host: domain,
      port: 443,
      method: 'GET',
      rejectUnauthorized: !insecure,
    };

    const req = https.request(options, (res) => {
      const socket: any = res.socket;
      const cert = socket?.getPeerCertificate?.(true);

      if (!cert?.raw) {
        reject(new Error('Unable to retrieve peer certificate'));
        return;
      }

      const fingerprint = crypto
        .createHash('sha256')
        .update(cert.raw)
        .digest('hex')
        .match(/.{2}/g)!
        .join(':')
        .toUpperCase();

      resolve({
        domain,
        subject: cert.subject ?? {},
        issuer: cert.issuer ?? {},
        validFrom: cert.valid_from,
        validTo: cert.valid_to,
        fingerprint,
      });
    });

    req.on('error', reject);
    req.end();
  });
}

function formatOutput(results: CertificateInfo[], mode: Mode, format: Format): string {
  const fingerprints = results.map((r) => r.fingerprint);

  switch (format) {
    case 'fingerprints':
      return `export const fingerprints = ${JSON.stringify(fingerprints, null, 2)};`;

    case 'capacitor': {
      if (mode === 'single') {
        return `plugins: {
  SSLPinning: {
    fingerprint: "${fingerprints[0]}"
  }
}`;
      }
      return `plugins: {
  SSLPinning: {
    fingerprints: ${JSON.stringify(fingerprints, null, 4)}
  }
}`;
    }

    case 'capacitor-plugin': {
      if (mode === 'single') {
        return `SSLPinning: {
  fingerprint: "${fingerprints[0]}"
}`;
      }
      return `SSLPinning: {
  fingerprints: ${JSON.stringify(fingerprints, null, 4)}
}`;
    }

    case 'capacitor-json': {
      if (mode === 'single') {
        return JSON.stringify(
          {
            plugins: {
              SSLPinning: {
                fingerprint: fingerprints[0],
              },
            },
          },
          null,
          2,
        );
      }
      return JSON.stringify(
        {
          plugins: {
            SSLPinning: {
              fingerprints,
            },
          },
        },
        null,
        2,
      );
    }

    case 'json':
    default:
      return JSON.stringify(results, null, 2);
  }
}

async function main() {
  const argv = await yargs(hideBin(process.argv))
    .usage('Usage: $0 <domains...> [options]')
    .version(pkg.version)
    .option('out', {
      alias: 'o',
      type: 'string',
      description: 'Output file path',
    })
    .option('format', {
      alias: 'f',
      type: 'string',
      choices: ['json', 'fingerprints', 'capacitor', 'capacitor-plugin', 'capacitor-json'],
      default: 'json',
      description: 'Output format',
    })
    .option('mode', {
      type: 'string',
      choices: ['single', 'multi'],
      default: 'single',
      description: 'Fingerprint mode (single or multi)',
    })
    .option('insecure', {
      type: 'boolean',
      default: true,
      description: 'Allow insecure TLS connections (disables certificate validation)',
    })
    .demandCommand(1, 'At least one domain is required')
    .help().argv;

  const domains = argv._ as string[];
  const results: CertificateInfo[] = [];

  console.log('Fetching certificates...\n');

  for (const domain of domains) {
    try {
      const certInfo = await getCertificate(domain, argv.insecure);
      results.push(certInfo);

      console.log(`Domain: ${certInfo.domain}`);
      console.log(`Subject: ${certInfo.subject.CN ?? '-'}`);
      console.log(`Issuer: ${certInfo.issuer.CN ?? '-'}`);
      console.log(`Valid From: ${certInfo.validFrom}`);
      console.log(`Valid To: ${certInfo.validTo}`);
      console.log(`SHA256 Fingerprint: ${certInfo.fingerprint}`);
      console.log('-------------------\n');
    } catch (err: any) {
      console.error(`Error fetching cert for ${domain}: ${err?.message ?? err}`);
      console.log('-------------------\n');
    }
  }

  const output = formatOutput(results, argv.mode as Mode, argv.format as Format);

  if (argv.out) {
    await fs.writeFile(argv.out, output);
    console.log(`Results written to ${argv.out}`);
  } else {
    console.log(output);
  }

  process.exit(0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
