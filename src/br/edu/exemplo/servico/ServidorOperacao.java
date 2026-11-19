package br.edu.exemplo.servico;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class ServidorOperacao {
    private ServidorOperacao() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Uso: java ... ServidorOperacao soma|subtracao|divisao|multiplicacao");
            System.exit(2);
        }
        String operacao = args[0].toLowerCase();
        String endereco = switch (operacao) {
            case "soma" -> "127.0.0.1";
            case "subtracao" -> "127.0.0.2";
            case "divisao" -> "127.0.0.3";
            case "multiplicacao" -> "127.0.0.4";
            default -> throw new IllegalArgumentException("Operação desconhecida: " + operacao);
        };
        int porta = Integer.getInteger("calc.port", 8080);
        HttpServer servidor = HttpServer.create(new InetSocketAddress(endereco, porta), 0);
        servidor.createContext("/health", troca -> responder(troca, 200, "{\"status\":\"disponivel\",\"servico\":\"" + operacao + "\"}"));
        servidor.createContext("/calcular", troca -> {
            if (!"GET".equals(troca.getRequestMethod())) {
                responder(troca, 405, "{\"erro\":\"Use GET\"}");
                return;
            }
            try {
                Map<String, String> parametros = parametros(troca.getRequestURI());
                double a = Double.parseDouble(parametros.get("a"));
                double b = Double.parseDouble(parametros.get("b"));
                if (!Double.isFinite(a) || !Double.isFinite(b)) {
                    responder(troca, 400, "{\"erro\":\"Números inválidos\"}");
                    return;
                }
                if (operacao.equals("divisao") && b == 0.0) {
                    responder(troca, 422, "{\"erro\":\"Divisão por zero não permitida\"}");
                    return;
                }
                double resultado = switch (operacao) {
                    case "soma" -> a + b;
                    case "subtracao" -> a - b;
                    case "divisao" -> a / b;
                    case "multiplicacao" -> a * b;
                    default -> throw new IllegalStateException();
                };
                if (!Double.isFinite(resultado)) {
                    responder(troca, 422, "{\"erro\":\"Resultado fora do intervalo numérico\"}");
                    return;
                }
                responder(troca, 200, "{\"resultado\":" + Double.toString(resultado) + "}");
            } catch (IllegalArgumentException | NullPointerException ex) {
                responder(troca, 400, "{\"erro\":\"Informe a e b como números válidos\"}");
            }
        });
        servidor.setExecutor(Executors.newFixedThreadPool(4));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> servidor.stop(0)));
        servidor.start();
        System.out.printf("Serviço %s disponível em http://%s:%d%n", operacao, endereco, porta);
        System.out.println("Rotas: /health e /calcular?a=10&b=2");
    }

    private static Map<String, String> parametros(URI uri) {
        Map<String, String> resultado = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null) return resultado;
        for (String trecho : query.split("&")) {
            String[] par = trecho.split("=", 2);
            if (par.length == 2) resultado.put(par[0], par[1]);
        }
        return resultado;
    }

    private static void responder(HttpExchange troca, int codigo, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.sendResponseHeaders(codigo, bytes.length);
        try (var saida = troca.getResponseBody()) { saida.write(bytes); }
    }
}
