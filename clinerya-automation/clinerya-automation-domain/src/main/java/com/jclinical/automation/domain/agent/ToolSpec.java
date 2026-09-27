package com.jclinical.automation.domain.agent;

import java.util.List;

/** Como se le describe una herramienta al modelo. */
public record ToolSpec(String name, String description, List<Parameter> parameters) {

    public ToolSpec {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    /** {@code type} sigue los tipos de JSON Schema: string, integer, number, boolean. */
    public record Parameter(String name, String type, String description, boolean required) {}
}
