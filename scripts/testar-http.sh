#!/usr/bin/env bash
set -euo pipefail
porta="$(printenv CALC_PORT || echo 8080)"
command -v curl >/dev/null || { echo "Instale curl para testar HTTP."; exit 2; }
verificar() {
  local url="$1" esperado="$2" recebido
  recebido="$(curl --noproxy '*' -fsS --max-time 3 "$url")"
  if [[ "$recebido" != *"$esperado"* ]]; then
    echo "FALHOU: $url => $recebido"; exit 1
  fi
  echo "OK: $url => $recebido"
}
verificar "http://127.0.0.1:$porta/health" '"servico":"soma"'
verificar "http://127.0.0.2:$porta/health" '"servico":"subtracao"'
verificar "http://127.0.0.3:$porta/health" '"servico":"divisao"'
verificar "http://127.0.0.4:$porta/health" '"servico":"multiplicacao"'
verificar "http://127.0.0.1:$porta/calcular?a=10&b=2" '"resultado":12.0'
verificar "http://127.0.0.2:$porta/calcular?a=10&b=2" '"resultado":8.0'
verificar "http://127.0.0.3:$porta/calcular?a=10&b=2" '"resultado":5.0'
verificar "http://127.0.0.4:$porta/calcular?a=10&b=2" '"resultado":20.0'
status="$(curl --noproxy '*' -s -o /dev/null -w '%{http_code}' "http://127.0.0.3:$porta/calcular?a=10&b=0")"
[[ "$status" == 422 ]] || { echo "FALHOU: divisão por zero deveria retornar 422"; exit 1; }
echo "Todos os testes passaram."
