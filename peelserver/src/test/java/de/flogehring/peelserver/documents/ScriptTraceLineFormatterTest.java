package de.flogehring.peelserver.documents;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptTraceLineFormatterTest {

    private final ScriptTraceLineFormatter formatter = new ScriptTraceLineFormatter();

    @Test
    void formatsAssignmentAndExpressionLines() {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("type", "program");
        trace.put("expressions", List.of(
                assignment("a", binaryOperator("+", varRef(5), literal(6), 11), 11),
                assignment("d", binaryOperator("+", varRef(11), varRef(5), 16), 16),
                binaryOperator("+", varRef(16), varRef(11), 27)
        ));
        trace.put("result", intValue(27));

        List<String> lines = formatter.formatProgramLines(trace);

        assertThat(lines).containsExactly(
                "var a = 5 + 6;",
                "var d = 11 + 5;",
                "16 + 11"
        );
    }

    private Map<String, Object> literal(int value) {
        Map<String, Object> literal = new LinkedHashMap<>();
        literal.put("type", "literal");
        literal.put("value", intValue(value));
        return literal;
    }

    private Map<String, Object> varRef(int resolvedValue) {
        Map<String, Object> variable = new LinkedHashMap<>();
        variable.put("type", "variable_name");
        variable.put("name", "x");
        variable.put("value", intValue(resolvedValue));
        return variable;
    }

    private Map<String, Object> intValue(int value) {
        Map<String, Object> number = new LinkedHashMap<>();
        number.put("type", "integer");
        number.put("value", value);
        return number;
    }

    private Map<String, Object> assignment(String variableName, Map<String, Object> expression, int value) {
        Map<String, Object> assignment = new LinkedHashMap<>();
        assignment.put("type", "assignment");
        assignment.put("variableName", variableName);
        assignment.put("expression", expression);
        assignment.put("value", intValue(value));
        return assignment;
    }

    private Map<String, Object> binaryOperator(
            String operator,
            Map<String, Object> lhs,
            Map<String, Object> rhs,
            int value
    ) {
        Map<String, Object> expression = new LinkedHashMap<>();
        expression.put("type", "binary_operator");
        expression.put("operator", operator);
        expression.put("lhs", lhs);
        expression.put("rhs", rhs);
        expression.put("value", intValue(value));
        return expression;
    }
}
