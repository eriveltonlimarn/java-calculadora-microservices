#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/compilar.sh
pids=""
encerrar() { for pid in $pids; do kill "$pid" 2>/dev/null || true; done; wait 2>/dev/null || true; }
trap encerrar EXIT INT TERM
porta="$(printenv CALC_PORT || echo 8080)"
for op in soma subtracao divisao multiplicacao; do
  java -Dcalc.port="$porta" -cp out br.edu.exemplo.servico.ServidorOperacao "$op" &
  pids="$pids $!"
done
sleep 1
java -Dcalc.port="$porta" -cp out br.edu.exemplo.cliente.CalculadoraGUI
