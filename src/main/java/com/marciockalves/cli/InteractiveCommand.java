package com.marciockalves.cli;

import picocli.CommandLine.Command;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.Callable;

@Command(name = "interactive", description = "Abre o menu interativo para criacao guiada de cenarios BDD.")
public class InteractiveCommand implements Callable<Integer> {

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