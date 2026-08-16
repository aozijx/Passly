#!/usr/bin/env bash
# WSL setup v7: reinstall dsh allowing native install scripts
set -uo pipefail
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

echo "===== SETUP v7: $(date) ====="

ALLOWED="@deepseek-ai/dsh-subprocess-local,koffi,node-pty,@google/genai,protobufjs"
echo "--- reinstall with --allow-scripts (nodedir for offline node-gyp) ---"
npm_config_nodedir=/usr/local npm install -g --allow-scripts="$ALLOWED" @deepseek-ai/dsh

echo "--- check native binaries ---"
ls -la /usr/local/lib/node_modules/@deepseek-ai/dsh/node_modules/node-pty/prebuilds/linux-x64/ 2>/dev/null || echo "no prebuilds/linux-x64"
ls -la /usr/local/lib/node_modules/@deepseek-ai/dsh/node_modules/node-pty/build/Release/pty.node 2>/dev/null || echo "no build/Release/pty.node"
ls -la /usr/local/lib/node_modules/@deepseek-ai/dsh/node_modules/koffi/prebuilds/linux-x64/ 2>/dev/null || echo "no koffi linux-x64 prebuilds"

dsh --version
echo "===== SETUP v7 DONE: $(date) ====="
