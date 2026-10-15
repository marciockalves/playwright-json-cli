package com.marciockalves.cli;

import com.marciockalves.core.JsonParser;
import com.marciockalves.core.TestEngine;
import com.marciockalves.model.TestScenario;
import picocli.CommandLine.Command;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.Callable;

@Command(name = "list", description = "Exibe um menu interativo para navegar entre modulos e executar cenarios.")
public class ListCommand implements Callable<Integer> {

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

            while (true) {
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

                while (true) {
                    java.util.List<Path> jsonFiles = Files.list(selectedModuleDir)
                            .filter(p -> p.toString().endsWith(".json"))
                            .sorted()
                            .toList();

                    if (jsonFiles.isEmpty()) {
                        System.err.println("\n⚠️ Nenhum cenário JSON encontrado no módulo '" + selectedModuleDir.getFileName() + "'.");
                        System.out.println("🔙 Voltando para a lista de módulos...");
                        break;
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

                    if (scenarioInput.equalsIgnoreCase("v")) {
                        System.out.println("🔙 Voltando para os módulos...");
                        break;
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

                    TestScenario scenario = JsonParser.loadScenario(targetJsonPathStr);
                    System.out.println("📄 Cenário carregado com sucesso. Host alvo: " + scenario.host());

                    boolean success = TestEngine.execute(scenario);

                    if (success) {
                        System.out.println("\n✨ Execução finalizada com status: SUCESSO");
                    } else {
                        System.err.println("\n💥 Execução finalizada com status: FALHA");
                    }

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