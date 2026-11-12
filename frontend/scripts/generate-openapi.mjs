import path from 'node:path';
import process from 'node:process';
import { spawnSync } from 'node:child_process';

const defaultSpecUrl = 'http://localhost:18091/v3/api-docs';
const specInput = process.env.OPENAPI_SPEC_URL ?? defaultSpecUrl;
const outputPath = path.resolve(
  process.cwd(),
  'src/app/core/models/openapi.generated.ts',
);

async function main() {
  const cmd = 'npx';
  const args = ['openapi-typescript', specInput, '-o', outputPath];
  const result = spawnSync(cmd, args, { stdio: 'inherit', shell: true });

  if (result.status !== 0) {
    console.error('openapi: generation failed.');
    console.error(
      'Ensure backend is running and OpenAPI is available, e.g. http://localhost:18091/v3/api-docs',
    );
    process.exit(1);
  }
  console.log(`openapi: generated -> ${outputPath}`);
}

await main();
