package com.marciockalves.core;

import com.microsoft.playwright.*;
import com.marciockalves.model.TestScenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de execucao E2E baseado em Playwright e orientado a dados (JSON),
 * com suporte robusto e nativo a interpolacao de variaveis (.env + JSON environment).
 */
public class TestEngine {

    // Regex para encontrar padroes como ${VAR_NAME}
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * Executa um cenario de teste completo baseado no TestScenario carregado.
     */
    public static boolean execute(TestScenario scenario) {
        System.out.println("🚀 [TestEngine] Iniciando execucao do motor Playwright...");

        // 1. Carrega o .env da raiz do projeto para um mapa unificado de segredos/variaveis
        Map<String, String> runtimeEnvironment = loadDotEnvToMap();

        // 2. Se o JSON do cenario tiver um bloco 'environment', mesclamos (o JSON tem precedencia)
        if (scenario.environment() != null) {
            runtimeEnvironment.putAll(scenario.environment());
        }

        try (Playwright playwright = Playwright.create()) {

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(false);
            try (Browser browser = playwright.chromium().launch(launchOptions)) {

                BrowserContext context = browser.newContext();
                Page page = context.newPage();

                // 3. Resolve e navega para o Host global configurado no JSON usando o ambiente unificado
                String rawHost = scenario.host();
                String resolvedHost = resolveVariables(rawHost, runtimeEnvironment);

                if (resolvedHost != null && !resolvedHost.isBlank()) {
                    System.out.println("🌐 Navegando para o host base: " + resolvedHost);
                    page.navigate(resolvedHost);
                } else {
                    System.out.println("⚠️ Nenhum 'host' global definido no JSON. Prosseguindo sem navegação inicial.");
                }

                // 4. Itera por cada step do cenario
                if (scenario.steps() != null) {
                    for (TestScenario.Step step : scenario.steps()) {
                        System.out.println("\n📌 [Step " + step.stepIndex() + "]: " + step.stepTitle());

                        // Executa as acoes do step com valores interpolados pelo ambiente unificado
                        if (step.actions() != null) {
                            for (TestScenario.Action action : step.actions()) {
                                executeAction(page, action, runtimeEnvironment);
                            }
                        }

                        // Trata evidencia (atraso opcional e captura de screenshot)
                        if (step.evidence() != null && step.evidence().capture()) {
                            if (step.evidence().delayMs() > 0) {
                                page.waitForTimeout(step.evidence().delayMs());
                            }

                            Path evidenciasDir = Paths.get("evidencias");
                            java.nio.file.Files.createDirectories(evidenciasDir);

                            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                            String screenshotName = "evidencia_step_" + step.stepIndex() + "_" + timestamp + ".png";
                            Path screenshotPath = evidenciasDir.resolve(screenshotName);

                            page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
                            System.out.println("📸 Evidencia capturada e salva em: " + screenshotPath);
                        }
                    }
                }

                System.out.println("\n✅ [TestEngine] Todos os steps executados com sucesso!");
                return true;

            }
        } catch (Exception e) {
            System.err.println("❌ [TestEngine] Erro durante a execucao do teste: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Processa uma acao individual do Playwright aplicando a interpolacao de variaveis no valor.
     */
    private static void executeAction(Page page, TestScenario.Action action, Map<String, String> envConfig) {
        String act = action.action().toUpperCase();
        String selector = getSelectorString(action);

        // Resolve variaveis no valor da acao usando o ambiente unificado
        String resolvedValue = resolveVariables(action.value(), envConfig);

        System.out.println("   ⚡ Ação: " + act + " | Seletor: " + selector + " | Valor: " + (resolvedValue != null ? resolvedValue : ""));

        switch (act) {
            case "FILL":
                page.locator(selector).fill(resolvedValue);
                break;
            case "CLICK":
                page.locator(selector).click();
                break;
            case "NAVIGATE":
                page.navigate(resolvedValue);
                break;
            case "PRESS":
                page.locator(selector).press(resolvedValue);
                break;
            default:
                System.out.println("   ⚠️ Ação desconhecida ou nao suportada: " + act);
        }
    }

    /**
     * Lê o arquivo .env na raiz do projeto e o converte em um Map limpo em memória.
     */
    private static Map<String, String> loadDotEnvToMap() {
        Map<String, String> envMap = new HashMap<>();
        Path envPath = Paths.get(".env");

        // Primeiro, herda as variáveis de ambiente reais do sistema operacional
        System.getenv().forEach(envMap::put);

        if (!Files.exists(envPath)) {
            return envMap;
        }

        try {
            java.util.List<String> lines = Files.readAllLines(envPath, java.nio.charset.StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    // Remove aspas acidentais se houverem no valor do .env
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    envMap.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Aviso: Falha ao ler o arquivo .env: " + e.getMessage());
        }

        return envMap;
    }

    /**
     * Procura por padroes ${VAR_NAME} em uma string e substitui pelo valor correspondente
     * vindo do mapa de ambiente unificado (Sistema + .env + JSON).
     */
    private static String resolveVariables(String input, Map<String, String> envConfig) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Matcher matcher = ENV_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = "";

            if (envConfig != null && envConfig.containsKey(varName)) {
                replacement = envConfig.get(varName);
            } else {
                replacement = "${" + varName + "}";
                System.out.println("   ⚠️ Aviso: Variavel de ambiente '" + varName + "' nao encontrada.");
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Monta o seletor com base no tipo (id, css, xpath).
     */
    private static String getSelectorString(TestScenario.Action action) {
        String type = action.selectorType() != null ? action.selectorType().toLowerCase() : "css";
        String sel = action.selector();

        switch (type) {
            case "id":
                return "#" + sel;
            case "xpath":
                return "xpath=" + sel;
            case "css":
            default:
                return sel;
        }
    }
}