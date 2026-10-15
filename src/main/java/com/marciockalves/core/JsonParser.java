package com.marciockalves.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marciockalves.model.TestScenario;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Responsavel por ler e desserializar os arquivos JSON de cenario de teste usando o Jackson.
 */
public class JsonParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Carrega um arquivo JSON de cenario e o converte para o Record TestScenario.
     *
     * @param jsonPath Caminho para o arquivo JSON do cenario.
     * @return O objeto TestScenario populado.
     * @throws IOException se o arquivo nao for encontrado ou houver erro no parsing.
     */
    public static TestScenario loadScenario(String jsonPath) throws IOException {
        File file = new File(jsonPath);
        if (!file.exists()) {
            throw new IOException("Arquivo JSON de cenario nao encontrado em: " + jsonPath);
        }

        // O Jackson mapeia diretamente o JSON para o Record imutavel do Java 21
        return objectMapper.readValue(file, TestScenario.class);
    }

    /**
     * Helper para carregar a partir de um Path.
     */
    public static TestScenario loadScenario(Path jsonPath) throws IOException {
        return loadScenario(jsonPath.toString());
    }
}