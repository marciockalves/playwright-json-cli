package com.marciockalves.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Mapeamento imutavel do arquivo JSON do cenario de teste usando Records do Java 21.
 */
public record TestScenario(
        @JsonProperty("host") String host,
        @JsonProperty("environment") Map<String, String> environment,
        @JsonProperty("steps") List<Step> steps
) {

    /**
     * Representa um passo do BDD/cenario de teste.
     */
    public record Step(
            @JsonProperty("step_index") int stepIndex,
            @JsonProperty("step_title") String stepTitle,
            @JsonProperty("evidence") Evidence evidence,
            @JsonProperty("actions") List<Action> actions
    ) {}

    /**
     * Configuracoes de captura de evidencia para o step.
     */
    public record Evidence(
            @JsonProperty("capture") boolean capture,
            @JsonProperty("delay_ms") int delayMs
    ) {}

    /**
     * Representa uma acao individual do Playwright dentro do step.
     */
    public record Action(
            @JsonProperty("action") String action,
            @JsonProperty("selector_type") String selectorType,
            @JsonProperty("selector") String selector,
            @JsonProperty("index") int index,
            @JsonProperty("value") String value
    ) {}
}