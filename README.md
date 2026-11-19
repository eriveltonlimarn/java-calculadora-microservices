# Calculadora Java — Arquitetura de Microserviços

Projeto didático para a unidade curricular **Projeto e Engenharia de Software**. A aplicação reúne uma interface gráfica feita em **Java Swing** e **quatro serviços HTTP executados em processos Java independentes**, um para cada operação matemática.

**Professor:** Erivelton Xavier de Lima  
**Linguagem:** Java 17 ou superior  
**Tecnologias:** Swing, `HttpServer`, `HttpClient`, HTTP e JSON  
**Frameworks externos:** nenhum

## Sumário

1. [Objetivo e funcionamento](#1-objetivo-e-funcionamento)
2. [O que é arquitetura de microserviços?](#2-o-que-é-arquitetura-de-microserviços)
3. [Arquitetura da calculadora](#3-arquitetura-da-calculadora)
4. [Organização do projeto](#4-organização-do-projeto)
5. [Requisitos para executar](#5-requisitos-para-executar)
6. [Como baixar do GitHub](#6-como-baixar-do-github)
7. [Executar todos os serviços](#7-executar-todos-os-serviços)
8. [Executar cada serviço separadamente](#8-executar-cada-serviço-separadamente)
9. [Endpoints HTTP e respostas JSON](#9-endpoints-http-e-respostas-json)
10. [Testar a indisponibilidade dos serviços](#10-testar-a-indisponibilidade-dos-serviços)
11. [Testes automatizados via HTTP](#11-testes-automatizados-via-http)
12. [Análise dos arquivos Java](#12-análise-dos-arquivos-java)
13. [Conceitos importantes e limitações](#13-conceitos-importantes-e-limitações)
14. [Solução de problemas](#14-solução-de-problemas)
15. [Atividades propostas](#15-atividades-propostas)
16. [Referências](#16-referências)

## 1. Objetivo e funcionamento

A calculadora permite realizar **soma, subtração, divisão e multiplicação**. Diferentemente de um programa em que todas as contas são executadas pela interface, cada operação é processada por um **serviço HTTP próprio**.

A janela mostra dois campos numéricos e quatro cartões. Cada cartão possui:

- Nome da operação;
- Endereço IP e porta do serviço;
- Indicador **Disponível** ou **Indisponível**;
- Botão **Calcular**, habilitado apenas quando o serviço responde ao monitoramento.

A interface verifica os quatro serviços **a cada dois segundos**. Ao parar um serviço, seu status fica **Indisponível** e seu botão é desabilitado. Os demais continuam funcionando.

A aplicação foi construída para facilitar o estudo de **comunicação cliente-servidor, processos independentes, endpoints, requisições HTTP, JSON e falhas de serviço**.

## 2. O que é arquitetura de microserviços?

A **arquitetura de microserviços** organiza um sistema como um conjunto de serviços com responsabilidades delimitadas, que se comunicam por contratos definidos. Um serviço pode ser desenvolvido, executado ou implantado sem exigir que toda a aplicação seja executada como um único processo.

Em um sistema de comércio eletrônico, por exemplo, pode haver serviços responsáveis por clientes, estoque, pedidos e pagamentos. Na nossa calculadora, a separação foi intencionalmente simplificada: há um serviço para cada operação matemática.

### Características que aparecem no projeto

- **Responsabilidade específica:** cada instância processa uma operação.
- **Processos independentes:** os quatro servidores Java podem ser iniciados e encerrados separadamente.
- **Contrato HTTP:** a interface solicita operações por URLs definidas.
- **Formato de resposta:** cada serviço devolve dados JSON.
- **Verificação de saúde:** o endpoint `/health` permite verificar se o processo responde.
- **Isolamento de falha de processo:** parar a divisão não encerra automaticamente a soma, subtração ou multiplicação.

### Diferença entre camadas, MVC e microserviços

| Abordagem | Principal preocupação |
|---|---|
| **Arquitetura em camadas** | Separar responsabilidades, como apresentação, aplicação, domínio e infraestrutura. |
| **MVC** | Organizar Model, View e Controller e o fluxo entre eles. |
| **Microserviços** | Delimitar serviços executáveis e suas interfaces de comunicação. |

Essas abordagens **podem ser combinadas**. Um microserviço pode ter suas próprias camadas ou utilizar MVC.

## 3. Arquitetura da calculadora

~~~mermaid
flowchart TB
    U["Usuário"] --> GUI["CalculadoraGUI<br/>Java Swing"]
    GUI -->|"HTTP GET"| S["Soma<br/>127.0.0.1:8080"]
    GUI -->|"HTTP GET"| SUB["Subtração<br/>127.0.0.2:8080"]
    GUI -->|"HTTP GET"| DIV["Divisão<br/>127.0.0.3:8080"]
    GUI -->|"HTTP GET"| MUL["Multiplicação<br/>127.0.0.4:8080"]
    S -->|"JSON"| GUI
    SUB -->|"JSON"| GUI
    DIV -->|"JSON"| GUI
    MUL -->|"JSON"| GUI
~~~

| Operação | IP | Porta | Expressão |
|---|---|---|---|
| Soma | `127.0.0.1` | 8080 | `a + b` |
| Subtração | `127.0.0.2` | 8080 | `a - b` |
| Divisão | `127.0.0.3` | 8080 | `a / b` |
| Multiplicação | `127.0.0.4` | 8080 | `a * b` |

**Por que todos usam a porta 8080?** Porque cada processo está vinculado a um endereço IP diferente. Os endereços `127.0.0.1` a `127.0.0.4` são endereços de **loopback**: todos apontam para **o mesmo computador**, e não para quatro máquinas físicas. A independência demonstrada é a dos **processos Java**, não a da infraestrutura física.

No Linux, a faixa de loopback normalmente permite esse uso. Em outros sistemas operacionais, pode ser necessário configurar os endereços adicionais.

## 4. Organização do projeto

~~~text
java-calculadora-microservices/
├── README.md
├── .gitignore
├── src/
│   └── br/edu/exemplo/
│       ├── cliente/
│       │   └── CalculadoraGUI.java
│       └── servico/
│           └── ServidorOperacao.java
└── scripts/
    ├── compilar.sh
    ├── iniciar-tudo.sh
    ├── iniciar-servico.sh
    ├── iniciar-interface.sh
    └── testar-http.sh
~~~

O servidor é implementado em **um único arquivo-fonte**, `ServidorOperacao.java`, mas é iniciado **quatro vezes em processos diferentes**, com os argumentos `soma`, `subtracao`, `divisao` e `multiplicacao`. É uma simplificação didática: cada instância tem operação, endereço e ciclo de execução próprios, mas compartilha a implementação.

O diretório `out/` é criado durante a compilação e contém os arquivos `.class`. Não precisa ser enviado ao GitHub.

## 5. Requisitos para executar

- JDK **17 ou superior**, com os comandos `java` e `javac`;
- Terminal **Bash**;
- Sistema Linux, preferencialmente **Arch Linux**, ou outro ambiente que suporte os endereços de loopback usados;
- Ambiente gráfico para abrir a janela Swing;
- `curl`, opcional, para testar as APIs pelo terminal.

Verifique o Java:

~~~bash
java -version
javac -version
~~~

No Arch Linux, caso o JDK ainda não esteja instalado:

~~~bash
sudo pacman -S jdk-openjdk
~~~

**Não é necessário** instalar Maven, Gradle, Docker, Spring Boot ou banco de dados.

## 6. Como baixar do GitHub

### Opção A — Clonar com Git

~~~bash
git clone https://github.com/eriveltonlimarn/java-calculadora-microservices.git
cd java-calculadora-microservices
~~~

### Opção B — Baixar como ZIP

1. Entre em [java-calculadora-microservices](https://github.com/eriveltonlimarn/java-calculadora-microservices).
2. Clique em **Code**.
3. Selecione **Download ZIP**.
4. Extraia o arquivo.
5. Abra um terminal dentro da pasta extraída.

## 7. Executar todos os serviços

Na raiz do projeto:

~~~bash
bash scripts/iniciar-tudo.sh
~~~

O script faz a compilação, inicia os **quatro serviços HTTP** e abre a interface Swing. Ao fechar a janela, o script encerra os processos que ele iniciou.

Também é possível executar os scripts diretamente após conceder permissão:

~~~bash
chmod +x scripts/*.sh
./scripts/iniciar-tudo.sh
~~~

Para usar outra porta em todos os serviços:

~~~bash
CALC_PORT=18080 bash scripts/iniciar-tudo.sh
~~~

## 8. Executar cada serviço separadamente

Este é o modo **recomendado para a prática em sala de aula**, porque cada serviço tem seu próprio terminal e pode ser interrompido sem afetar os outros.

Primeiro, na raiz do projeto:

~~~bash
bash scripts/compilar.sh
~~~

Abra **cinco terminais**, todos na raiz do repositório.

**Terminal 1 — Soma**

~~~bash
bash scripts/iniciar-servico.sh soma
~~~

**Terminal 2 — Subtração**

~~~bash
bash scripts/iniciar-servico.sh subtracao
~~~

**Terminal 3 — Divisão**

~~~bash
bash scripts/iniciar-servico.sh divisao
~~~

**Terminal 4 — Multiplicação**

~~~bash
bash scripts/iniciar-servico.sh multiplicacao
~~~

**Terminal 5 — Interface gráfica**

~~~bash
bash scripts/iniciar-interface.sh
~~~

Na janela, informe dois números e clique em **Calcular** no cartão da operação desejada.

## 9. Endpoints HTTP e respostas JSON

Cada serviço implementa duas rotas:

### `GET /health` — Disponibilidade

~~~bash
curl --noproxy '*' http://127.0.0.1:8080/health
~~~

Exemplo de resposta do serviço de soma:

~~~json
{"status":"disponivel","servico":"soma"}
~~~

Se o processo estiver desligado, a tentativa de conexão falhará: **um serviço parado não consegue retornar uma resposta HTTP**.

### `GET /calcular` — Operação matemática

A rota recebe dois parâmetros: `a` e `b`.

~~~bash
curl --noproxy '*' 'http://127.0.0.1:8080/calcular?a=10&b=2'
~~~

Resposta:

~~~json
{"resultado":12.0}
~~~

Teste as outras operações:

~~~bash
curl --noproxy '*' 'http://127.0.0.2:8080/calcular?a=10&b=2'
curl --noproxy '*' 'http://127.0.0.3:8080/calcular?a=10&b=2'
curl --noproxy '*' 'http://127.0.0.4:8080/calcular?a=10&b=2'
~~~

Resultados esperados, na ordem: `8.0`, `5.0`, `20.0`.

### Respostas de erro

| Situação | Status HTTP | Comportamento |
|---|---:|---|
| Cálculo realizado | 200 | Resposta com o resultado em JSON |
| Número ausente ou inválido | 400 | Informa parâmetros inválidos |
| Método não permitido em `/calcular` | 405 | Orienta utilizar GET |
| Divisão por zero | 422 | Rejeita a operação |
| Resultado não finito | 422 | Rejeita o resultado |

Exemplo de divisão por zero:

~~~bash
curl --noproxy '*' -i 'http://127.0.0.3:8080/calcular?a=10&b=0'
~~~

A resposta terá status **HTTP 422** e mensagem de erro em JSON.

## 10. Testar a indisponibilidade dos serviços

1. Inicie os quatro serviços **separadamente**, como na seção 8.
2. Abra a interface e confirme que os quatro indicadores estão como **Disponível**.
3. No terminal da **Divisão**, pressione **Ctrl+C**.
4. Na janela, aguarde a próxima verificação: **Dividir** passará para **Indisponível**.
5. Observe que o botão dessa operação fica desabilitado.
6. Continue utilizando **Somar**, **Subtrair** e **Multiplicar** normalmente.
7. Reinicie a divisão:

~~~bash
bash scripts/iniciar-servico.sh divisao
~~~

O monitoramento deverá identificar a recuperação e alterar o estado para **Disponível**.

**Interpretação arquitetural:** a falha de um processo é isolada em relação aos outros processos. Isso **não garante disponibilidade** caso o computador ou a infraestrutura comum falhe, porque todos os serviços estão na mesma máquina.

## 11. Testes automatizados via HTTP

Com os quatro serviços em execução, execute:

~~~bash
bash scripts/testar-http.sh
~~~

O script usa `curl` e verifica as quatro respostas de `/health`, os quatro cálculos com `a=10` e `b=2`, além do erro HTTP 422 para divisão por zero.

Se configurou outra porta, utilize:

~~~bash
CALC_PORT=18080 bash scripts/testar-http.sh
~~~

**Validação realizada no ambiente de preparação:** o código Java compilou com Java 21, os quatro cálculos retornaram `12.0`, `8.0`, `5.0` e `20.0`, e a divisão por zero respondeu com status **422**. A interação visual da janela não foi testada automaticamente.

## 12. Análise dos arquivos Java

### `ServidorOperacao.java`

Este arquivo implementa o **servidor HTTP**. Seu método `main` recebe a operação por argumento, escolhe o IP e inicializa um `HttpServer`. As chamadas a `createContext` criam as rotas `/health` e `/calcular`.

O código também interpreta os parâmetros `a` e `b`, verifica números inválidos e realiza o cálculo. A resposta enviada contém um **status HTTP** e um corpo em **JSON**.

**Fluxo:** receber requisição → verificar parâmetros → executar operação → montar resposta → enviar JSON.

### `CalculadoraGUI.java`

Esta classe monta a **janela Swing** e atua como **cliente HTTP**. Seus campos recebem os números, e seus cartões mostram o estado de cada operação.

O método `verificarTodos()` consulta a rota `/health` de cada serviço. O método `calcular(...)` envia a requisição HTTP à operação selecionada, recebe o JSON e exibe o resultado.

As chamadas são feitas com `HttpClient.sendAsync(...)`, evitando bloquear a interface enquanto aguardam a rede. As atualizações visuais são encaminhadas ao **Event Dispatch Thread** com `SwingUtilities.invokeLater(...)`.

### Fluxo de exemplo: divisão de 10 por 2

1. Usuário informa **10** e **2** na janela e clica em **Dividir**.
2. A interface chama `http://127.0.0.3:8080/calcular?a=10.0&b=2.0`.
3. O processo da divisão recebe a requisição HTTP.
4. O servidor verifica os parâmetros e executa `10 / 2`.
5. O processo responde com `{"resultado":5.0}`.
6. A interface recebe o resultado e o apresenta ao usuário.

A operação é feita pelo servidor, **não pelo código da interface**.

## 13. Conceitos importantes e limitações

### Conceitos que os alunos devem distinguir

- **Processo:** instância de um programa em execução.
- **Serviço:** funcionalidade acessível por um contrato de comunicação.
- **Endereço IP:** identifica o endpoint de rede de destino, em conjunto com a porta.
- **Porta:** identifica o ponto de comunicação no endereço.
- **Endpoint:** rota de acesso a uma operação, como `/health`.
- **API HTTP:** contrato de comunicação entre cliente e serviço.
- **JSON:** formato textual de representação dos dados retornados.
- **Health check:** consulta usada para verificar a capacidade de resposta de um serviço.
- **Disponibilidade:** capacidade de atender às solicitações em determinado momento.
- **Isolamento de falhas:** redução do impacto de uma falha em outro componente.

### O que este projeto não implementa

Não existem banco de dados, autenticação, balanceamento de carga, API Gateway, descoberta dinâmica de serviços, orquestração, observabilidade distribuída nem implantação em máquinas distintas. **Cada operação é executada por um processo separado, mas os quatro processos dependem do mesmo computador.**

Em aplicações reais, os serviços podem ter bancos próprios, contratos versionados, mecanismos de repetição, circuit breakers, logs e monitoramento centralizado. O presente código demonstra os conceitos essenciais de maneira controlada.

**Segurança:** os serviços são vinculados a IPs locais de loopback. Não exponha os endpoints à internet sem acrescentar autenticação, limites de requisição e demais controles necessários.

## 14. Solução de problemas

**`javac: command not found`:** o JDK não está instalado ou não está no `PATH`. Confirme com `javac -version`.

**`Address already in use`:** já existe um servidor usando o mesmo IP e porta. Encerre processos antigos ou consulte:

~~~bash
ss -ltnp | grep 8080
~~~

**`Cannot assign requested address`:** o sistema não aceita o IP de loopback solicitado. Confira a configuração da interface local; a configuração prevista foi testada em Linux.

**A janela não abre:** o Swing precisa de um ambiente gráfico. Uma sessão SSH sem suporte gráfico não será suficiente.

**Todos aparecem indisponíveis:** confirme que os quatro terminais de serviço seguem executando, que a porta é a mesma em cliente e servidor e que não existe bloqueio da comunicação local.

**Quero trocar a porta:** defina a variável `CALC_PORT` **em todos os terminais** antes de iniciar servidores e interface.

~~~bash
export CALC_PORT=18080
~~~

## 15. Atividades propostas

1. Identifique o trecho que associa cada serviço ao seu endereço IP.
2. Explique por que a calculadora possui quatro processos, embora o servidor tenha apenas uma classe Java.
3. Execute os quatro servidores separadamente e registre os processos no sistema operacional.
4. Pare somente a multiplicação e descreva o comportamento da interface.
5. Explique por que os IPs `127.0.0.x` não correspondem a computadores diferentes.
6. Identifique a diferença entre o endpoint `/health` e `/calcular`.
7. Explique por que o cliente usa requisições assíncronas.
8. Implemente um novo serviço de **potenciação**, com um quinto IP de loopback.
9. Proponha como executar os serviços em máquinas diferentes e quais alterações seriam necessárias.
10. Compare o projeto de microserviços com as versões da calculadora em **MVC** e **arquitetura em camadas**.

## 16. Referências

- [Documentação do Java Swing — Oracle](https://docs.oracle.com/javase/tutorial/uiswing/)
- [HttpServer — Java SE 17](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html)
- [HttpClient — Java SE 17](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html)
- [RFC 9110 — HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110)

---

**Material didático:** Projeto e Engenharia de Software  
**Professor:** Erivelton Xavier de Lima
