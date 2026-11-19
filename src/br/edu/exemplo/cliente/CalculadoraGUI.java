package br.edu.exemplo.cliente;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CalculadoraGUI extends JFrame {
    private enum Operacao {
        SOMA("Somar (+)", "127.0.0.1"),
        SUBTRACAO("Subtrair (−)", "127.0.0.2"),
        DIVISAO("Dividir (÷)", "127.0.0.3"),
        MULTIPLICACAO("Multiplicar (×)", "127.0.0.4");
        final String titulo, ip;
        Operacao(String titulo, String ip) { this.titulo = titulo; this.ip = ip; }
        String url(String rota) { return "http://" + ip + ":" + Integer.getInteger("calc.port", 8080) + rota; }
    }

    private final JTextField numeroA = new JTextField("10", 12);
    private final JTextField numeroB = new JTextField("2", 12);
    private final JLabel resultado = new JLabel("Selecione uma operação.");
    private final Map<Operacao, JLabel> status = new EnumMap<>(Operacao.class);
    private final Map<Operacao, JButton> botoes = new EnumMap<>(Operacao.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(650)).executor(executor)
        .proxy(java.net.ProxySelector.of(null)).build();
    private final Timer monitor;

    private CalculadoraGUI() {
        super("Calculadora com Microserviços — Java");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(760, 560);
        setMinimumSize(new Dimension(650, 500));
        setLocationRelativeTo(null);
        var raiz = new JPanel(new BorderLayout(12, 12));
        raiz.setBorder(new EmptyBorder(20, 24, 20, 24));
        raiz.setBackground(new Color(247, 249, 252));
        setContentPane(raiz);

        JLabel titulo = new JLabel("Calculadora distribuída");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitulo = new JLabel("Cada operação é um serviço HTTP independente em um IP de loopback.");
        subtitulo.setForeground(new Color(85, 95, 110));
        JPanel topo = new JPanel(new GridLayout(2, 1, 0, 5));
        topo.setOpaque(false);
        topo.add(titulo); topo.add(subtitulo);
        raiz.add(topo, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(12, 15));
        centro.setOpaque(false);
        JPanel entrada = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 12));
        entrada.setBackground(Color.WHITE);
        entrada.add(new JLabel("Número A:")); entrada.add(numeroA);
        entrada.add(new JLabel("Número B:")); entrada.add(numeroB);
        centro.add(entrada, BorderLayout.NORTH);

        JPanel grade = new JPanel(new GridLayout(2, 2, 14, 14));
        grade.setOpaque(false);
        for (Operacao op : Operacao.values()) {
            JPanel cartao = new JPanel(new BorderLayout(5, 10));
            cartao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 222, 232)),
                new EmptyBorder(16, 16, 16, 16)));
            cartao.setBackground(Color.WHITE);
            JLabel nome = new JLabel(op.titulo);
            nome.setFont(new Font("SansSerif", Font.BOLD, 18));
            JLabel ip = new JLabel(op.ip + ":" + Integer.getInteger("calc.port", 8080));
            ip.setForeground(new Color(100, 110, 120));
            JLabel situacao = new JLabel("● Verificando...");
            situacao.setForeground(new Color(140, 110, 20));
            status.put(op, situacao);
            JButton botao = new JButton("Calcular");
            botao.setEnabled(false);
            botoes.put(op, botao);
            botao.addActionListener(e -> calcular(op));
            JPanel textos = new JPanel(new GridLayout(3, 1, 0, 6));
            textos.setOpaque(false);
            textos.add(nome); textos.add(ip); textos.add(situacao);
            cartao.add(textos, BorderLayout.CENTER);
            cartao.add(botao, BorderLayout.SOUTH);
            grade.add(cartao);
        }
        centro.add(grade, BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new BorderLayout(4, 8));
        rodape.setOpaque(false);
        resultado.setFont(new Font("SansSerif", Font.BOLD, 18));
        rodape.add(resultado, BorderLayout.NORTH);
        JLabel dica = new JLabel("Desligue um servidor no terminal (Ctrl+C) para observar o status mudar.");
        dica.setForeground(new Color(90, 100, 110));
        rodape.add(dica, BorderLayout.SOUTH);
        raiz.add(rodape, BorderLayout.SOUTH);

        monitor = new Timer(2000, e -> verificarTodos());
        monitor.setInitialDelay(0);
        monitor.start();
    }

    private void verificarTodos() {
        for (Operacao op : Operacao.values()) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(op.url("/health")))
                .timeout(Duration.ofMillis(1000)).GET().build();
            http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> res.statusCode() == 200 && res.body().contains("\"disponivel\""))
                .exceptionally(ex -> false)
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    status.get(op).setText(ok ? "● Disponível" : "● Indisponível");
                    status.get(op).setForeground(ok ? new Color(22, 130, 65) : new Color(190, 45, 45));
                    botoes.get(op).setEnabled(ok);
                }));
        }
    }

    private void calcular(Operacao op) {
        final double a, b;
        try {
            a = Double.parseDouble(numeroA.getText().trim().replace(',', '.'));
            b = Double.parseDouble(numeroB.getText().trim().replace(',', '.'));
            if (!Double.isFinite(a) || !Double.isFinite(b)) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            resultado.setText("Informe dois números válidos.");
            return;
        }
        if (op == Operacao.DIVISAO && b == 0) {
            resultado.setText("Não é possível dividir por zero.");
            return;
        }
        JButton botao = botoes.get(op);
        botao.setEnabled(false);
        resultado.setText("Consultando o serviço " + op.titulo + "...");
        String url = op.url("/calcular?a=" + Double.toString(a) + "&b=" + Double.toString(b));
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(2)).GET().build();
        http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .whenComplete((res, erro) -> SwingUtilities.invokeLater(() -> {
                if (erro != null || res == null) {
                    resultado.setText("Serviço " + op.titulo + " indisponível.");
                    verificarTodos();
                } else if (res.statusCode() != 200) {
                    resultado.setText("Erro do serviço (HTTP " + res.statusCode() + "): " + res.body());
                } else {
                    try {
                        String json = res.body();
                        String valor = json.split(":", 2)[1].replace("}", "").trim();
                        resultado.setText("Resultado de " + op.titulo + ": " + Double.parseDouble(valor));
                    } catch (RuntimeException ex) {
                        resultado.setText("Resposta inesperada do serviço.");
                    }
                }
                botao.setEnabled(status.get(op).getText().contains("Disponível"));
            }));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculadoraGUI().setVisible(true));
    }
}
