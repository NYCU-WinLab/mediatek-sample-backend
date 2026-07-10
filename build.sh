#!/usr/bin/env bash
# Compiles the service into ./out. No build tool / dependency needed — just a JDK.
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac -d out $(find src -name '*.java')
echo "built -> out/ (run: PORT=8080 java -cp out tw.winlab.reportlab.Main)"
