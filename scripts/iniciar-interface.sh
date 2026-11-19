#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f out/br/edu/exemplo/cliente/CalculadoraGUI.class ]; then bash scripts/compilar.sh; fi
exec java -Dcalc.port="$(printenv CALC_PORT || echo 8080)" -cp out br.edu.exemplo.cliente.CalculadoraGUI
