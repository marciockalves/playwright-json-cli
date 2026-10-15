package com.marciockalves.cli;

import com.marciockalves.core.JsonParser;
import com.marciockalves.core.TestEngine;
import com.marciockalves.model.TestScenario;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.util.concurrent.Callable;

@Command(name = "run", description = "Executa um cenario de teste baseado em um arquivo JSON.")
public class RunCommand implements Callable<Integer> {

    @Option(names = {"-s", "--scenario"}, required = true, description = "Caminho para o arquivo JSON do cenario.")
    private String scenarioPath;

    @Override
    public Integer call() {
        System.out.println("🚀 Disparando motor de testes para o cenario: " + scenarioPath);

        try {
            TestScenario scenario = JsonParser.loadScenario(scenarioPath);
            System.out.println("📄 Cenario carregado com sucesso. Host alvo: " + scenario.host());

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