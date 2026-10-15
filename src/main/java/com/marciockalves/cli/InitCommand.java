package com.marciockalves.cli;

import picocli.CommandLine.Command;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

@Command(name = "init", description = "Inicializa a estrutura de pastas e arquivos de infraestrutura no diretório atual.")
public class InitCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        System.out.println("📁 Inicializando estrutura do projeto de teste...");
        try {
            new File("features").mkdirs();
            new File("payloads").mkdirs();
            new File("evidencias").mkdirs();

            File envFile = new File(".env");
            if (!envFile.exists()) {
                String envContent = "# Configuracoes de Ambiente e Secrets (LGPD Safe)\nAPP_URL=https://exemplo.com\n";
                Files.writeString(envFile.toPath(), envContent, StandardCharsets.UTF_8);
                System.out.println("📄 Arquivo .env criado.");
            }

            String[] arquivosInfra = {"Makefile", "docker-compose.yml"};

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