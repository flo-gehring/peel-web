package de.flogehring.peelserver.run.render;

import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceExpressionKind;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionRendererTest {

    private static final ExpressionRenderer DEFAULT_RENDERER = ExpressionRenderer.ofDefaultRenderers();

    @Test
    void supportsExpressionMapByType() {
        Map<String, Object> expression = TraceMapOutput.fromExpression(new TraceExpression.Literal(TraceValue.integer(1)));

        assertThat(ExpressionRenderer.supports(expression)).isTrue();
        assertThat(ExpressionRenderer.supports(Map.of("type", "unknown"))).isFalse();
    }

    @Test
    void rendersAssignmentAndValue() {
        TraceExpression.Assignment assignment = new TraceExpression.Assignment(
                "x",
                new TraceExpression.BinaryOperator(
                        "+",
                        TraceValue.integer(3),
                        new TraceExpression.Literal(TraceValue.integer(1)),
                        new TraceExpression.Literal(TraceValue.integer(2)),
                        false
                ),
                TraceValue.integer(3)
        );

        Map<String, Object> expression = TraceMapOutput.fromExpression(assignment);
        String rendered = ExpressionRenderer.ofDefaultRenderers().render(expression);

        assertThat(rendered).isEqualTo("x = 1 + 2");
    }

    @Test
    void rendersFunctionCallArguments() {
        TraceExpression.FunctionCall functionCall = new TraceExpression.FunctionCall(
                "add",
                TraceValue.integer(3),
                List.of(
                        new TraceExpression.Literal(TraceValue.integer(1)),
                        new TraceExpression.Literal(TraceValue.integer(2))
                ),
                java.util.Optional.empty(),
                TraceExpression.CalleeSource.variable("add"),
                new TraceExpression.ResolvedCallable(de.flogehring.peel.core.trace.CallableKind.PEEL_FUNCTION, "add", 2)
        );

        Map<String, Object> expression = TraceMapOutput.fromExpression(functionCall);
        String rendered = ExpressionRenderer.ofDefaultRenderers().render(expression);
        assertThat(rendered).isEqualTo("add(1, 2)");
    }

    @Test
    void rendersLiteralWithDefaultConfiguration() {
        String rendered = DEFAULT_RENDERER.render(literal("42"));

        assertThat(rendered).isEqualTo("42");
    }

    @Test
    void rendersBinaryOperatorWithDefaultConfiguration() {
        Map<String, Object> expression = binaryOperator("+", variableName("x"), literal("2"));

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("x + 2");
    }

    @Test
    void rendersUnaryPrefixOperatorWithDefaultConfiguration() {
        Map<String, Object> expression = unaryPrefixOperator("-", variableName("value"));

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("-value");
    }

    @Test
    void rendersVariableNameWithDefaultConfiguration() {
        String rendered = DEFAULT_RENDERER.render(variableName("items"));

        assertThat(rendered).isEqualTo("items");
    }

    @Test
    void rendersFunctionCallWithDefaultConfiguration() {
        Map<String, Object> expression = functionCall("sum", "1", "2", "3");

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("sum(1, 2, 3)");
    }

    @Test
    void rendersReturnExpressionWithDefaultConfiguration() {
        Map<String, Object> expression = returnExpression(variableName("result"));

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("return result");
    }

    @Test
    void rendersAssignmentWithDefaultConfiguration() {
        Map<String, Object> expression = assignment("x", binaryOperator("+", literal("1"), literal("2")));
        String rendered = DEFAULT_RENDERER.render(expression);
        assertThat(rendered).isEqualTo("x = 1 + 2");
    }

    @Test
    void rendersIfStatementWhenFirstConditionTaken() {
        Map<String, Object> expression = ifStatement(
                List.of(variableName("isAdmin"), variableName("isOwner")),
                block(List.of(returnExpression(literal("granted"))))
        );
        String rendered = DEFAULT_RENDERER.render(expression);
        assertThat(rendered).isEqualTo("if (isAdmin || isOwner) {return granted}");
    }

    @Test
    void rendersIfStatementWhenSecondConditionTaken() {
        Map<String, Object> expression = ifStatement(
                List.of(variableName("isDenied"), variableName("isAllowed")),
                block(List.of(returnExpression(literal("allowed"))))
        );

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("if (isDenied || isAllowed) {return allowed}");
    }

    @Test
    void rendersWhileLoopWhenNoIterationIsTaken() {
        Map<String, Object> expression = whileLoop(List.of());

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("while (...) {}");
    }

    @Test
    void rendersWhileLoopWhenOneIterationIsTaken() {
        Map<String, Object> expression = whileLoop(List.of(
                Map.of(
                        "condition", binaryOperator("<", variableName("i"), literal("3")),
                        "body", block(List.of(assignment("i", binaryOperator("+", variableName("i"), literal("1")))))
                )
        ));

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("while (i < 3) {i = i + 1}");
    }

    @Test
    void rendersWhileLoopWhenMoreThanTwoIterationsAreTaken() {
        Map<String, Object> expression = whileLoop(List.of(
                Map.of(
                        "condition", binaryOperator("<", variableName("index"), literal("10")),
                        "body", block(List.of(assignment("index", binaryOperator("+", variableName("index"), literal("1")))))
                ),
                Map.of(
                        "condition", binaryOperator("<", variableName("index"), literal("10")),
                        "body", block(List.of(assignment("index", binaryOperator("+", variableName("index"), literal("1")))))
                ),
                Map.of(
                        "condition", binaryOperator("<", variableName("index"), literal("10")),
                        "body", block(List.of(assignment("index", binaryOperator("+", variableName("index"), literal("1")))))
                )
        ));

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("while (index < 10) {index = index + 1}");
    }

    @Test
    void rendersForEachLoopWhenNoIterationIsTaken() {
        Map<String, Object> expression = forEachLoop("item", variableName("items"), List.of());
        String rendered = DEFAULT_RENDERER.render(expression);
        assertThat(rendered).isEqualTo("for (item in items) {}");
    }

    @Test
    void rendersForEachLoopWhenOneIterationIsTaken() {
        Map<String, Object> expression = forEachLoop(
                "item",
                variableName("items"),
                List.of(Map.of("body", block(List.of(functionCall("print", "item")))))
        );

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("for (item in items) {print(item)}");
    }

    @Test
    void rendersForEachLoopWhenMoreThanTwoIterationsAreTaken() {
        Map<String, Object> expression = forEachLoop(
                "item",
                variableName("items"),
                List.of(
                        Map.of("body", block(List.of(functionCall("collect", "item")))),
                        Map.of("body", block(List.of(functionCall("collect", "item")))),
                        Map.of("body", block(List.of(functionCall("collect", "item"))))
                )
        );

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("for (item in items) {collect(item)}");
    }

    @Test
    void rendersListLiteralWithDefaultConfiguration() {
        Map<String, Object> expression = listLiteral(variableName("a"), literal("2"), unaryPrefixOperator("-", literal("3")));
        String rendered = DEFAULT_RENDERER.render(expression);
        assertThat(rendered).isEqualTo("[a, 2, -3]");
    }

    @Test
    void rendersMapLiteralWithDefaultConfiguration() {
        Map<String, Object> expression = mapLiteral(
                Map.of("key", literal("name"), "value", literal("peel")),
                Map.of("key", literal("version"), "value", literal("1"))
        );
        String rendered = DEFAULT_RENDERER.render(expression);
        assertThat(rendered).isEqualTo("{name: peel, version: 1}");
    }

    @Test
    void rendersSelectorWithDefaultConfiguration() {
        Map<String, Object> expression = selector(variableName("items"), literal("0"));

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo("items[0]");
    }

    @Test
    void rendersBlockWithDefaultConfiguration() {
        Map<String, Object> expression = block(List.of(
                assignment("x", literal("1")),
                returnExpression(variableName("x"))
        ));
        String rendered = DEFAULT_RENDERER.render(expression);
        assertThat(rendered).isEqualTo("{x = 1; return x}");
    }

    @Test
    void rendersIfStatementWithNestedLoops() {
        Map<String, Object> whileLoop = whileLoop(List.of(
                Map.of(
                        "condition", binaryOperator("<", variableName("i"), literal("3")),
                        "body", block(List.of(assignment("i", binaryOperator("+", variableName("i"), literal("1")))))
                )
        ));
        Map<String, Object> forEachLoop = forEachLoop(
                "item",
                variableName("items"),
                List.of(
                        Map.of(
                                "body",
                                block(List.of(assignment("sum", binaryOperator("+", variableName("sum"), variableName("item")))))
                        )
                )
        );
        Map<String, Object> expression = ifStatement(
                List.of(variableName("shouldProcess")),
                block(List.of(whileLoop, forEachLoop))
        );

        String rendered = DEFAULT_RENDERER.render(expression);

        assertThat(rendered).isEqualTo(
                "if (shouldProcess) {while (i < 3) {i = i + 1}; for (item in items) {sum = sum + item}}"
        );
    }

    @Test
    void appliesConfiguredTemplateForKind() {
        Map<TraceExpressionKind, String> templates = new java.util.EnumMap<>(TraceExpressionKind.class);
        templates.put(TraceExpressionKind.LITERAL, "lit({{ valueText }})");
        ExpressionRenderConfiguration config = ExpressionRenderConfiguration.of(templates);
        Map<String, Object> expression = TraceMapOutput.fromExpression(new TraceExpression.Literal(TraceValue.integer(5)));
        String rendered = ExpressionRenderer.of(config).render(expression);
        assertThat(rendered).isEqualTo("lit(5)");
    }

    private static Map<String, Object> literal(String valueText) {
        return expression("literal", Map.of("value", textValue(valueText)));
    }

    private static Map<String, Object> binaryOperator(String operator, Map<String, Object> lhs, Map<String, Object> rhs) {
        return expression(
                "binary_operator",
                Map.of(
                        "operator", operator,
                        "lhs", lhs,
                        "rhs", rhs,
                        "value", textValue("binary")
                )
        );
    }

    private static Map<String, Object> unaryPrefixOperator(String operator, Map<String, Object> argument) {
        return expression(
                "unary_prefix_operator",
                Map.of(
                        "operator", operator,
                        "argument", argument,
                        "value", textValue("unary")
                )
        );
    }

    private static Map<String, Object> variableName(String name) {
        return expression(
                "variable_name",
                Map.of(
                        "name", name,
                        "value", textValue(name)
                )
        );
    }

    private static Map<String, Object> functionCall(String name, String... argumentTexts) {
        List<Map<String, Object>> arguments = java.util.Arrays.stream(argumentTexts)
                .map(argument -> {
                    Map<String, Object> argumentValue = new LinkedHashMap<>();
                    argumentValue.put("value", argument);
                    Map<String, Object> argumentMap = new LinkedHashMap<>();
                    argumentMap.put("value", argumentValue);
                    return argumentMap;
                })
                .toList();
        return expression(
                "function_call",
                Map.of(
                        "name", name,
                        "arguments", arguments,
                        "value", textValue(name)
                )
        );
    }

    private static Map<String, Object> returnExpression(Map<String, Object> expression) {
        return expression(
                "return_expression",
                Map.of(
                        "expression", expression,
                        "value", textValue("return")
                )
        );
    }

    private static Map<String, Object> assignment(String variableName, Map<String, Object> expression) {
        return expression(
                "assignment",
                Map.of(
                        "variableName", variableName,
                        "expression", expression,
                        "value", textValue(variableName)
                )
        );
    }

    private static Map<String, Object> ifStatement(List<Map<String, Object>> conditions, Map<String, Object> executedBlock) {
        return expression(
                "if_statement",
                Map.of(
                        "conditions", conditions,
                        "executedBlock", executedBlock,
                        "value", textValue("if")
                )
        );
    }

    private static Map<String, Object> whileLoop(List<Map<String, Object>> iterations) {
        return expression(
                "while_loop",
                Map.of(
                        "iterations", iterations,
                        "value", textValue("while")
                )
        );
    }

    private static Map<String, Object> forEachLoop(
            String variableName,
            Map<String, Object> iterableExpression,
            List<Map<String, Object>> iterations
    ) {
        return expression(
                "for_each_loop",
                Map.of(
                        "variableName", variableName,
                        "iterableExpression", iterableExpression,
                        "iterations", iterations,
                        "value", textValue("for")
                )
        );
    }

    @SafeVarargs
    private static Map<String, Object> listLiteral(Map<String, Object>... elements) {
        return expression(
                "list_literal",
                Map.of(
                        "elements", List.of(elements),
                        "value", textValue("list")
                )
        );
    }

    @SafeVarargs
    private static Map<String, Object> mapLiteral(Map<String, Object>... entries) {
        return expression(
                "map_literal",
                Map.of(
                        "entries", List.of(entries),
                        "value", textValue("map")
                )
        );
    }

    private static Map<String, Object> selector(Map<String, Object> target, Map<String, Object> selector) {
        return expression(
                "selector",
                Map.of(
                        "target", target,
                        "selector", selector,
                        "value", textValue("selector")
                )
        );
    }

    private static Map<String, Object> block(List<Map<String, Object>> content) {
        return expression(
                "block",
                Map.of(
                        "content", content,
                        "value", textValue("block")
                )
        );
    }

    private static Map<String, Object> textValue(String value) {
        return Map.of("type", "text", "value", value);
    }

    private static Map<String, Object> expression(String type, Map<String, Object> fields) {
        Map<String, Object> expression = new LinkedHashMap<>();
        expression.put("type", type);
        expression.putAll(fields);
        return expression;
    }
}
