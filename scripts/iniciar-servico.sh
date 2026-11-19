#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ "$#" -ne 1 ]; then echo 'Uso: scripts/iniciar-servico.sh soma|subtracao|divisao|multiplicacao'; exit 2; fi
if [ ! -f out/br/edu/exemplo/servico/ServidorOperacao.class ]; then bash scripts/compilar.sh; fi
exec java -Dcalc.port="$(printenv CALC_PORT || echo 8080)" -cp out br.edu.exemplo.servico.ServidorOperacao "$1"
