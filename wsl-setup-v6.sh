#!/usr/bin/env bash
# WSL setup v6: pnpm (force) + dsh with local nodedir (no nodejs.org network)
set -uo pipefail
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

echo "===== SETUP v6: $(date) ====="

echo "--- [1] local node headers for node-gyp ---"
if [ -f /usr/local/include/node/node.h ]; then
  echo "headers OK at /usr/local/include/node"
else
  echo "headers missing; downloading from npmmirror"
  cd /tmp
  curl -fsSL --connect-timeout 30 -o node-headers.tar.gz "https://npmmirror.com/mirrors/node/v24.19.0/node-v24.19.0-headers.tar.gz" || echo "header download failed"
  tar -xzf node-headers.tar.gz -C /usr/local --strip-components=1 2>/dev/null || echo "header extract failed"
fi

echo "--- [2] install pnpm@11.7.0 (force overwrite corepack shim) ---"
npm install -g pnpm@11.7.0 --force
pnpm --version || true

echo "--- [3] install @deepseek-ai/dsh (nodedir=/usr/local) ---"
npm_config_nodedir=/usr/local npm install -g @deepseek-ai/dsh
command -v dsh
dsh --version || dsh --help | head -10 || true

echo "--- [4] verify (root) ---"
command -v git node npm pnpm dsh
node -v
git --version
pnpm --version || true

echo "--- [5] verify (hiner) ---"
su - hiner -c 'export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; echo "user=$(whoami)"; node -v; npm -v; pnpm --version; command -v dsh; dsh --version' || echo "hiner verify failed"

echo "===== SETUP v6 DONE: $(date) ====="
