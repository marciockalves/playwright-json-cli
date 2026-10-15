package com.marciockalves.cli;

import com.marciockalves.core.JsonParser;
import com.marciockalves.core.TestEngine;
import com.marciockalves.model.TestScenario;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.concurrent.Callable;

@Command(
        name = "testcli",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Ferramenta CLI JSON-Driven para automacao E2E com Playwright e Java 21.",
        subcommands = {
                MainCLI.InitCommand.class,
                MainCLI.InteractiveCommand.class,
                MainCLI.RunCommand.class,
                MainCLI.ListCommand.class // <--- Adicionado aqui!
        }
)
public class MainCLI implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    // --- SUBCOMANDO 1: init ---
    @Command(name = "init", description = "Inicializa a estrutura de pastas e arquivos de infraestrutura no diretório atual.")
    static class InitCommand implements Callable<Integer> {
        @Override
        public Integer call() {
            System.out.println("📁 Inicializando estrutura do projeto de teste...");
            try {
                // 1. Criação das pastas de negócio
                new File("features").mkdirs();
                new File("payloads").mkdirs();
                new File("evidencias").mkdirs();

                // 2. Criação do arquivo .env (caso não exista)
                File envFile = new File(".env");
                if (!envFile.exists()) {
                    String envContent = "# Configuracoes de Ambiente e Secrets (LGPD Safe)\nAPP_URL=https://exemplo.com\n";
                    Files.writeString(envFile.toPath(), envContent, StandardCharsets.UTF_8);
                    System.out.println("📄 Arquivo .env criado.");
                }

                // 3. Cópia do trio de infraestrutura (Makefile, Dockerfile, docker-compose.yml)
                // de dentro de src/main/resources/templates/ para a raiz do usuário
                String[] arquivosInfra = {"Makefile", "Dockerfile", "docker-compose.yml"};

                for (String arquivo : arquivosInfra) {
                    Path destino = Paths.get(arquivo);
                    try (InputStream inputStream = InitCommand.class.getResourceAsStream("/templates/" + arquivo)) {
                        if (inputStream != null) {
                            Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("📄 " + arquivo + " copiado com sucesso.");
                        } else {
                            System.err.println("⚠️ Aviso: O template '" + arquivo + "' não foi encontrado nos recursos do jar.");
                        }
                    }
                }

                System.out.println("✅ Projeto inicializado com sucesso!");
            } catch (Exception e) {
                System.err.println("❌ Erro ao inicializar o projeto: " + e.getMessage());
                return 1;
            }
            return 0;
        }
    }

    @Command(name = "interactive", description = "Abre o menu interativo para criacao guiada de cenarios BDD.")
    static class InteractiveCommand implements Callable<Integer> {

        /**
         * Carrega os verbetes BDD internamente dos recursos da aplicação (src/main/resources/templates/bdd.dict)
         */
        private java.util.Set<String> loadInternalBddDictionary() {
            java.util.Set<String> keywords = new java.util.HashSet<>();
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("templates/bdd.dict")) {
                if (is != null) {
                    java.util.List<String> lines = new java.io.BufferedReader(
                            new java.io.InputStreamReader(is, StandardCharsets.UTF_8)
                    ).lines().toList();

                    for (String line : lines) {
                        String trimmed = line.trim().toLowerCase();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                            keywords.add(trimmed);
                        }
                    }
                } else {
                    return java.util.Set.of("given", "when", "then", "and", "but", "dado", "quando", "então", "e", "mas");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Aviso ao carregar dicionário interno, usando padrão básico: " + e.getMessage());
                return java.util.Set.of("given", "when", "then", "and", "but", "dado", "quando", "então", "e", "mas");
            }
            return keywords;
        }

        @Override
        public Integer call() {
            Scanner scanner = new Scanner(System.in);

            System.out.println("==================================================");
            System.out.println("  🖥️  TestCLI - Assistente Interativo de BDD");
            System.out.println("==================================================");

            System.out.print("📂 Digite o nome da pasta/modulo (ex: login): ");
            String moduleFolder = scanner.nextLine().trim();
            if (moduleFolder.isEmpty()) moduleFolder = "geral";

            System.out.print("✏️  Digite o titulo descritivo do cenario (ex: realizar login com sucesso): ");
            String scenarioTitle = scanner.nextLine().trim();
            if (scenarioTitle.isEmpty()) scenarioTitle = "novo-cenario";

            String fileName = scenarioTitle.toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".feature";

            try {
                Path featuresDir = Path.of("features", moduleFolder);
                Files.createDirectories(featuresDir);

                Path payloadsDir = Path.of("payloads", moduleFolder);
                Files.createDirectories(payloadsDir);

                Path targetFeaturePath = featuresDir.resolve(fileName);
                Path targetJsonPath = payloadsDir.resolve(fileName.replace(".feature", ".json"));

                InputStream templateStream = getClass().getClassLoader().getResourceAsStream("templates/bdd-template.feature");
                if (templateStream == null) {
                    System.err.println("❌ Erro: Template BDD nao encontrado nos recursos do sistema.");
                    return 1;
                }

                String templateContent = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);
                String finalContent = templateContent.replace("${scenario_title}", scenarioTitle);

                Files.writeString(targetFeaturePath, finalContent, StandardCharsets.UTF_8);
                System.out.println("✨ Cenário BDD gerado com sucesso em: " + targetFeaturePath);

                System.out.println("🚀 Abrindo o editor para refinamento dos steps...");

                ProcessBuilder processBuilder = new ProcessBuilder("micro", targetFeaturePath.toString());
                processBuilder.inheritIO();

                Process process = processBuilder.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    System.out.println("✅ Arquivo BDD salvo com sucesso!");

                    System.out.println("⚙️ Analisando steps via dicionário interno e gerando o JSON estruturado...");

                    java.util.Set<String> bddKeywords = loadInternalBddDictionary();

                    java.util.List<String> lines = Files.readAllLines(targetFeaturePath, StandardCharsets.UTF_8);
                    StringBuilder jsonBuilder = new StringBuilder();

                    // Cabeçalho do JSON compatível com o TestScenario Record
                    jsonBuilder.append("{\n");
                    jsonBuilder.append("  \"host\": \"${APP_URL}\",\n");
                    jsonBuilder.append("  \"environment\": {\n");
                    jsonBuilder.append("    \"HEADLESS\": \"true\"\n");
                    jsonBuilder.append("  },\n");
                    jsonBuilder.append("  \"steps\": [\n");

                    boolean firstStep = true;
                    int stepIndex = 1;

                    for (String line : lines) {
                        String trimmed = line.trim();
                        String[] parts = trimmed.split("\\s+", 2);

                        if (parts.length > 0) {
                            String firstWord = parts[0].toLowerCase().replaceAll("[^a-zçáéíóúãõñ]", "");

                            if (bddKeywords.contains(firstWord)) {
                                if (!firstStep) {
                                    jsonBuilder.append(",\n");
                                }

                                String stepTitle = trimmed.replace("\"", "\\\"");

                                // Estrutura detalhada do step que o motor e os records esperam
                                jsonBuilder.append("    {\n");
                                jsonBuilder.append("      \"step_index\": ").append(stepIndex++).append(",\n");
                                jsonBuilder.append("      \"step_title\": \"").append(stepTitle).append("\",\n");
                                jsonBuilder.append("      \"evidence\": {\n");
                                jsonBuilder.append("        \"capture\": true,\n");
                                jsonBuilder.append("        \"delay_ms\": 500\n");
                                jsonBuilder.append("      },\n");
                                jsonBuilder.append("      \"actions\": [\n");
                                jsonBuilder.append("        {\n");
                                jsonBuilder.append("          \"action\": \"click\",\n");
                                jsonBuilder.append("          \"selector_type\": \"css\",\n");
                                jsonBuilder.append("          \"selector\": \"\",\n");
                                jsonBuilder.append("          \"index\": 0,\n");
                                jsonBuilder.append("          \"value\": \"\"\n");
                                jsonBuilder.append("        }\n");
                                jsonBuilder.append("      ]\n");
                                jsonBuilder.append("    }");

                                firstStep = false;
                            }
                        }
                    }

                    jsonBuilder.append("\n  ]\n");
                    jsonBuilder.append("}\n");

                    Files.writeString(targetJsonPath, jsonBuilder.toString(), StandardCharsets.UTF_8);
                    System.out.println("✨ Payload JSON gerado com sucesso em: " + targetJsonPath);

                } else {
                    System.out.println("⚠️ O editor foi fechado (código " + exitCode + "). Nenhum JSON foi gerado.");
                }

            } catch (Exception e) {
                System.err.println("❌ Erro durante o fluxo interativo: " + e.getMessage());
                return 1;
            }

            return 0;
        }
    }

    // --- SUBCOMANDO 3: run ---
    // --- SUBCOMANDO 3: run (Executa o motor de testes via JSON) ---
    @Command(name = "run", description = "Executa um cenario de teste baseado em um arquivo JSON.")
    static class RunCommand implements Callable<Integer> {

        @Option(names = {"-s", "--scenario"}, required = true, description = "Caminho para o arquivo JSON do cenario.")
        private String scenarioPath;

        @Override
        public Integer call() {
            System.out.println("🚀 Disparando motor de testes para o cenario: " + scenarioPath);

            try {
                // 1. Lê e desserializa o JSON usando o nosso parser
                TestScenario scenario = JsonParser.loadScenario(scenarioPath);
                System.out.println("📄 Cenario carregado com sucesso. Host alvo: " + scenario.host());

                // 2. Dispara a engine do Playwright
                boolean success = TestEngine.execute(scenario);

                if (success) {
                    System.out.println("✨ Execucao finalizada com status: SUCESSO");
                    return 0;
                } else {
                    System.err.println("💥 Execucao finalizada com status: FALHA");
                    return 1;
                }

            } catch (Exception e) {
                System.err.println("❌ Erro ao executar o cenario: " + e.getMessage());
                return 1;
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MainCLI()).execute(args);
        System.exit(exitCode);
    }

    // --- SUBCOMANDO 4: list / menu interativo de execução com opção de voltar ---
    @Command(name = "list", description = "Exibe um menu interativo para navegar entre modulos e executar cenarios.")
    static class ListCommand implements Callable<Integer> {

        @Override
        public Integer call() {
            Scanner scanner = new Scanner(System.in);

            System.out.println("==================================================");
            System.out.println("  📋 TestCLI - Menu Interativo de Execução");
            System.out.println("==================================================");

            try {
                Path payloadsRoot = Paths.get("payloads");
                if (!Files.exists(payloadsRoot) || !Files.isDirectory(payloadsRoot)) {
                    System.err.println("❌ Nenhuma pasta 'payloads' encontrada. Execute o 'init' ou crie cenários primeiro.");
                    return 1;
                }

                // Loop externo para permitir voltar ao menu de módulos
                while (true) {
                    // 1. Lista as subpastas (módulos) dentro de payloads/
                    java.util.List<Path> modules = Files.list(payloadsRoot)
                            .filter(Files::isDirectory)
                            .sorted()
                            .toList();

                    if (modules.isEmpty()) {
                        System.err.println("⚠️ Nenhuma subpasta de módulo encontrada em 'payloads/'.");
                        return 1;
                    }

                    System.out.println("\n📁 Módulos disponíveis:");
                    for (int i = 0; i < modules.size(); i++) {
                        String moduleName = modules.get(i).getFileName().toString();
                        System.out.println("  [" + (i + 1) + "] " + moduleName);
                    }
                    System.out.println("  [0] Sair");

                    System.out.print("\n👉 Escolha o número do módulo desejado (ou 0 para sair): ");
                    String moduleInput = scanner.nextLine().trim();

                    if (moduleInput.equals("0")) {
                        System.out.println("👋 Saindo do menu interativo. Até logo!");
                        return 0;
                    }

                    int moduleIndex;
                    try {
                        moduleIndex = Integer.parseInt(moduleInput) - 1;
                    } catch (NumberFormatException e) {
                        System.err.println("❌ Entrada inválida. Digite apenas números.");
                        continue;
                    }

                    if (moduleIndex < 0 || moduleIndex >= modules.size()) {
                        System.err.println("❌ Opção de módulo inválida. Tente novamente.");
                        continue;
                    }

                    Path selectedModuleDir = modules.get(moduleIndex);

                    // Loop interno para gerenciar os cenários do módulo selecionado
                    while (true) {
                        // 2. Lista os arquivos JSON de cenário dentro da subpasta escolhida
                        java.util.List<Path> jsonFiles = Files.list(selectedModuleDir)
                                .filter(p -> p.toString().endsWith(".json"))
                                .sorted()
                                .toList();

                        if (jsonFiles.isEmpty()) {
                            System.err.println("\n⚠️ Nenhum cenário JSON encontrado no módulo '" + selectedModuleDir.getFileName() + "'.");
                            System.out.println("🔙 Voltando para a lista de módulos...");
                            break; // Sai do loop interno e volta para escolher outro módulo
                        }

                        System.out.println("\n📄 Cenários disponíveis em [" + selectedModuleDir.getFileName() + "]:");
                        for (int i = 0; i < jsonFiles.size(); i++) {
                            String rawName = jsonFiles.get(i).getFileName().toString().replace(".json", "");
                            String readableTitle = rawName.replace("-", " ");
                            readableTitle = readableTitle.substring(0, 1).toUpperCase() + readableTitle.substring(1);

                            System.out.println("  [" + (i + 1) + "] " + readableTitle);
                        }
                        System.out.println("  [v] Voltar para a lista de módulos");

                        System.out.print("\n👉 Escolha o cenário (ou digite 'v' para voltar): ");
                        String scenarioInput = scanner.nextLine().trim();

                        // Verifica se o usuário quis voltar
                        if (scenarioInput.equalsIgnoreCase("v")) {
                            System.out.println("🔙 Voltando para os módulos...");
                            break; // Sai do loop interno e retorna ao menu de módulos
                        }

                        int scenarioIndex;
                        try {
                            scenarioIndex = Integer.parseInt(scenarioInput) - 1;
                        } catch (NumberFormatException e) {
                            System.err.println("❌ Entrada inválida. Digite o número do cenário ou 'v' para voltar.");
                            continue;
                        }

                        if (scenarioIndex < 0 || scenarioIndex >= jsonFiles.size()) {
                            System.err.println("❌ Opção de cenário inválida. Tente novamente.");
                            continue;
                        }

                        Path selectedJsonFile = jsonFiles.get(scenarioIndex);
                        String targetJsonPathStr = selectedJsonFile.toAbsolutePath().toString();

                        System.out.println("\n🚀 Disparando motor de testes para o cenário: " + selectedJsonFile.getFileName() + "\n");

                        // 3. Executa o motor utilizando o mesmo fluxo do RunCommand
                        TestScenario scenario = JsonParser.loadScenario(targetJsonPathStr);
                        System.out.println("📄 Cenário carregado com sucesso. Host alvo: " + scenario.host());

                        boolean success = TestEngine.execute(scenario);

                        if (success) {
                            System.out.println("\n✨ Execução finalizada com status: SUCESSO");
                        } else {
                            System.err.println("\n💥 Execução finalizada com status: FALHA");
                        }

                        // Pergunta se quer rodar outro cenário no mesmo módulo ou voltar
                        System.out.print("\n🔄 Pressione Enter para continuar neste módulo ou digite 'v' para voltar aos módulos: ");
                        String nextAction = scanner.nextLine().trim();
                        if (nextAction.equalsIgnoreCase("v")) {
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("❌ Erro durante a navegação/execução: " + e.getMessage());
                return 1;
            }
        }
    }
}