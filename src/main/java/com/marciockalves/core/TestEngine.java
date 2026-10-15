package com.marciockalves.core;

import com.microsoft.playwright.*;
import com.marciockalves.model.TestScenario;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de execucao E2E baseado em Playwright e orientado a dados (JSON),
 * com suporte a interpolacao de variaveis de ambiente.
 */
public class TestEngine {

    // Regex para encontrar padroes como ${VAR_NAME}
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * Executa um cenario de teste completo baseado no TestScenario carregado.
     */
    public static boolean execute(TestScenario scenario) {
        System.out.println("🚀 [TestEngine] Iniciando execucao do motor Playwright...");

        try (Playwright playwright = Playwright.create()) {

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(false);
            try (Browser browser = playwright.chromium().launch(launchOptions)) {

                BrowserContext context = browser.newContext();
                Page page = context.newPage();

                // 1. Resolve e navega para o Host global configurado no JSON
                String rawHost = scenario.host();
                String resolvedHost = resolveVariables(rawHost, scenario.environment());

                if (resolvedHost != null && !resolvedHost.isBlank()) {
                    System.out.println("🌐 Navegando para o host base: " + resolvedHost);
                    page.navigate(resolvedHost);
                } else {
                    System.out.println("⚠️ Nenhum 'host' global definido no JSON. Prosseguindo sem navegação inicial.");
                }

                // 2. Itera por cada step do cenario
                if (scenario.steps() != null) {
                    for (TestScenario.Step step : scenario.steps()) {
                        System.out.println("\n📌 [Step " + step.stepIndex() + "]: " + step.stepTitle());

                        // Executa as acoes do step com valores interpolados
                        if (step.actions() != null) {
                            for (TestScenario.Action action : step.actions()) {
                                executeAction(page, action, scenario.environment());
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

        // Resolve variaveis no valor da acao (ex: ${USER_DEFAULT} vira o valor real)
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
     * Procura por padroes ${VAR_NAME} em uma string e substitui pelo valor correspondente
     * vindo do environment do JSON, do .env ou do sistema operacional.
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

            // 1. Tenta buscar no mapa de environment do JSON do cenario
            if (envConfig != null && envConfig.containsKey(varName)) {
                replacement = envConfig.get(varName);
            }
            // 2. Tenta buscar nas variaveis de ambiente do sistema operacional / .env carregado
            else if (System.getenv(varName) != null) {
                replacement = System.getenv(varName);
            }
            // 3. Se nao achar em nenhum lugar, mantem o placeholder original ou aviso
            else {
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