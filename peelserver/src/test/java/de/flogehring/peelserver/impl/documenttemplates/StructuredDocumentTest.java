package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.api.data.template.DocumentNode;
import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.impl.run.BindingsMapper;
import de.flogehring.peelserver.impl.run.PeelExecutionService;
import de.flogehring.peelserver.impl.scripts.PeelScript;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static de.flogehring.peelserver.DocumentFixtures.calculation;
import static de.flogehring.peelserver.DocumentFixtures.doc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StructuredDocumentTest {
    final PeelExecutionService execution = spy(new PeelExecutionService(new BindingsMapper()));
    final DocumentTemplateRenderService renderer = new DocumentTemplateRenderService(execution);

    @Test
    void validatesAllScriptContractsBeforeRenderingStructuredCalculationsAndTables() {
        var calculationSource = "calculation: { amount * 2; }";
        var tableSource = "for (value in values) row: { value * factor; }";
        var table = de.flogehring.peelserver.DocumentFixtures.table("second");
        table = new DocumentNode("peelTable", Map.of("scope", "script", "scriptId", "second", "block", "row"),
                table.content(), null, List.of());
        var inputs = new Document("Inputs", Map.of(
                "first", new PeelScript("Calculation", calculationSource,
                        List.of(new de.flogehring.peelserver.impl.run.ExpectedBinding("amount", ""))),
                "second", new PeelScript("Table", tableSource,
                        List.of(new de.flogehring.peelserver.impl.run.ExpectedBinding("values", ""),
                                new de.flogehring.peelserver.impl.run.ExpectedBinding("factor", "")))),
                doc(calculation("first"), table), ExpressionRenderConfiguration.defaultConfig());
        assertThatThrownBy(() -> renderer.render(inputs, Map.of())).isInstanceOfSatisfying(
                de.flogehring.peelserver.impl.run.BindingInputException.class, error -> {
                    assertThat(error.details().get("missingBindings")).isEqualTo(List.of("amount", "factor", "values"));
                    assertThat((Map<?, ?>) error.details().get("scripts")).hasSize(2);
                });
        verifyNoInteractions(execution);
        var values = Map.<String, Object>of("amount", 3, "values", List.of(2, 4), "factor", 5);
        assertThat(renderer.render(inputs, values)).contains("<table", "<p>10</p>", "<p>20</p>");
        verify(execution).execute(calculationSource, values);
        verify(execution).execute(tableSource, values);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "detailed|<p class=\"peel-equation\">total = (a * b) = (2 * 3) = 6</p>",
            "compact|<p class=\"peel-equation\">total = (a * b) = 6</p>",
            "substituted|<p class=\"peel-equation\">total = (2 * 3) = 6</p>",
            "result|<p>6</p>"
    }, delimiter = '|')
    void calculationModesSelectFormulaSubstitutionAndResult(String mode, String expected) {
        var body = doc(node("peelCalculation", Map.of("scope", "script", "scriptId", "script", "block", "calc", "mode", mode)));
        assertThat(renderer.render(document("calc:{var total=a*b;}", body), Map.of("a", 2, "b", 3))).contains(expected);
    }

    @Test
    void substitutedModeKeepsEachIterationsValuesAndShowsSubstitutedFunctionBodies() {
        String source = "fun scale(x){return x*3;} for(i in [1,2]) calc:{var amount=scale(i);}";
        var body = doc(node("peelRepeat", Map.of("scope", "script", "scriptId", "script", "block", "calc"),
                node("peelCalculation", Map.of("scope", "current", "mode", "substituted"))));
        String html = renderer.render(document(source, body), Map.of());
        assertThat(html).containsSubsequence("amount = scale(1) = 3", "return (1 * 3) = 3", "amount = scale(2) = 6", "return (2 * 3) = 6")
                .doesNotContain("scale(i)", "return (x * 3)");
        assertThat(renderer.preview(document(source, body), Map.of()).pdfData()).isNotEmpty();
    }

    @Test
    void substitutedModeEscapesAndRoundsResolvedValuesWithoutDuplicatingLiteralResults() {
        var body = doc(node("peelCalculation", Map.of("scope", "script", "scriptId", "script", "mode", "substituted", "decimals", 2)));
        String source = "var amount=items[0];var copy=amount;var literal=7;var text=label;";
        String html = renderer.render(document(source, body), Map.of("items", List.of(1.234), "label", "<A>&"));
        assertThat(html).contains("amount = 1.23</p>", "copy = 1.23</p>", "literal = 7.00</p>", "text = &lt;A&gt;&amp;</p>")
                .doesNotContain("items[0]", "1.23 = 1.23", "7.00 = 7.00");
    }

    @Test
    void substitutedModeUsesRecordedConditionsAndDoesNotEvaluateSkippedOperands() {
        var body = doc(node("peelCalculation", Map.of("scope", "script", "scriptId", "script", "mode", "substituted")));
        String html = renderer.render(document("var n=0;while(n==0){if(False && (1/0==0)){99;}n=n+1;}", body), Map.of());
        assertThat(html).contains("while (0 == 0)", "while (1 == 0)", "(false &amp;&amp; …)")
                .doesNotContain("while (n == 0)", "1 / 0");
    }

    @Test
    void repeatsCalculationsWithTheirOwnLoopBindingAndProducesPdf() {
        String source = "for (bauteil in bauteile) nachweis: { var spannung = bauteil[\"kraft\"] / bauteil[\"flaeche\"]; }";
        var body = doc(node("peelRepeat", Map.of("scriptId", "script", "scope", "script", "block", "nachweis"),
                node("paragraph", Map.of(), text("Bauteil "), node("peelValue", Map.of("scope", "current", "binding", "bauteil.name"))),
                node("peelCalculation", Map.of("scope", "current", "mode", "detailed"))));
        var document = document(source, body);
        Map<String, Object> bindings = Map.of("bauteile", List.of(
                Map.of("name", "A & <A>", "kraft", 12, "flaeche", 3), Map.of("name", "B", "kraft", 30, "flaeche", 5)));
        String html = renderer.render(document, bindings);
        assertThat(html).contains("Bauteil A &amp; &lt;A&gt;", "Bauteil B", "(12 / 3)", "(30 / 5)");
        assertThat(html.indexOf("(12 / 3)")).isLessThan(html.indexOf("Bauteil B"));
        verify(execution, times(1)).execute(source, bindings);
        var preview = renderer.preview(document, bindings);
        assertThat(preview.pdfData()).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        assertThat(preview.warnings()).isEmpty();
    }

    @Test
    void nestedRepetitionsStayWithinTheirParentOccurrence() {
        String source = "for (outer in [1, 2]) parent: { for (inner in [3, 4]) child: { outer * inner; } }";
        var document = document(source, doc(node("peelRepeat", Map.of("scope", "script", "scriptId", "script", "block", "parent"),
                node("peelRepeat", Map.of("scope", "current", "block", "child"),
                        node("paragraph", Map.of(), node("peelValue", Map.of("scope", "current")))))));
        String html = renderer.render(document, Map.of());
        assertThat(html).containsSubsequence("<p>3</p>", "<p>4</p>", "<p>6</p>", "<p>8</p>");
        assertThat(html.split("<p>", -1)).hasSize(5);
    }

    @Test
    void differentiatesUnexecutedUnknownAndAmbiguousBlocks() {
        String source = "if (False) absent: {} for (x in [1,2]) repeated: { x; }";
        assertThatThrownBy(() -> renderer.render(document(source, doc(selected("absent"))), Map.of()))
                .hasMessageContaining("found 0").hasMessageContaining("document/0");
        assertThatThrownBy(() -> renderer.render(document(source, doc(selected("missing"))), Map.of()))
                .hasMessageContaining("Unknown block");
        assertThatThrownBy(() -> renderer.render(document(source, doc(selected("repeated"))), Map.of()))
                .hasMessageContaining("found 2");
        var empty = document(source, doc(node("peelRepeat", Map.of("scope", "script", "scriptId", "script", "block", "absent"),
                node("paragraph", Map.of(), text("SHOULD NOT APPEAR")))));
        var preview = renderer.preview(empty, Map.of());
        assertThat(preview.warnings()).singleElement().asString().contains("absent");
        assertThat(renderer.render(empty, Map.of())).doesNotContain("SHOULD NOT APPEAR");
    }

    @Test
    void wholeScriptIncludesUnnamedBlocksAndEveryLoopIteration() {
        var document = document("var sum = 0; for (x in [1,2]) { sum = sum + x; } if (True) { sum * 2; }", doc(calculation("script")));
        assertThat(renderer.render(document, Map.of())).contains("x = 1", "x = 2", "(sum * 2)", " = 6");
    }

    @Test
    void functionOccurrencesKeepParameterBindingsAndDoNotDuplicateArgumentTraces() {
        String source = "fun double(x) body: { return x * 2; } double(2); double(5);";
        var document = document(source, doc(node("peelRepeat", Map.of("scope", "script", "scriptId", "script", "block", "body"),
                node("paragraph", Map.of(), node("peelValue", Map.of("scope", "current", "binding", "x")),
                        text(":"), node("peelValue", Map.of("scope", "current"))))));
        assertThat(renderer.render(document, Map.of())).containsSubsequence("<p>2:4</p>", "<p>5:10</p>");
    }

    @Test
    void validatesStructureBeforeSavingAndRequiresAnEnclosingRepeat() {
        assertThatThrownBy(() -> doc(node("peelCalculation", Map.of("scope", "current", "mode", "detailed"))).validateDocument())
                .hasMessageContaining("requires a repetition");
        assertThatThrownBy(() -> doc(node("script", Map.of())).validateDocument()).hasMessageContaining("Unsupported");
        assertThat(doc(node("peelRepeat", Map.of("scope", "script", "scriptId", "s"),
                node("peelCalculation", Map.of("scope", "current", "scriptId", "unused", "mode", "result")))).scriptIds()).containsExactly("s");
    }

    @Test
    void detailedWholeScriptShowsUnnamedFunctionBodiesAndFormatsValuesWithoutChangingResults() {
        var document = document("fun f(x) { return x / 3; } f(1);", doc(calculation("script")));
        assertThat(renderer.render(document, Map.of())).contains("peel-function", "return (x / 3)");
        var value = node("peelValue", Map.of("scope", "script", "scriptId", "script", "decimals", 2));
        assertThat(renderer.render(document("1 / 3;", doc(node("paragraph", Map.of(), value))), Map.of())).contains("<p>0.33</p>");
    }

    @Test
    void capturesLocalFinalValuesInWhileAndProgramRoot() {
        String source = "var jahr=0;while(!(jahr==3)) runde:{var lokal=jahr+1;jahr=lokal;lokal=3;}";
        var repeated = node("peelRepeat", Map.of("scope", "script", "scriptId", "script", "block", "runde"),
                node("paragraph", Map.of(), node("peelValue", Map.of("scope", "current", "binding", "jahr")),
                        text(":"), node("peelValue", Map.of("scope", "current", "binding", "lokal"))));
        var root = node("paragraph", Map.of(), node("peelValue", Map.of("scope", "script", "scriptId", "script", "binding", "jahr")));
        var result = renderer.preview(document(source, doc(repeated, root)), Map.of());
        assertThat(result.pdfData()).isNotEmpty();
        assertThat(renderer.render(document(source, doc(repeated, root)), Map.of())).containsSubsequence("<p>1:3</p>", "<p>2:3</p>", "<p>3:3</p>", "<p>3</p>");
    }

    @Test
    void variableAndBlockResultStaySeparateUnderShadowing() {
        String source = "var wert=10;abschnitt:{var wert=20;detail:{var wert=30;wert=31;}}";
        var attrs = Map.<String, Object>of("scope", "script", "scriptId", "script", "block", "abschnitt", "binding", "wert");
        var body = doc(node("paragraph", Map.of(), node("peelValue", attrs), text(":"),
                node("peelValue", Map.of("scope", "script", "scriptId", "script", "block", "abschnitt"))));
        assertThat(renderer.render(document(source, body), Map.of())).contains("<p>20:31</p>");
    }

    @Test
    void functionCaptureDoesNotExposeCallerLoopVariable() {
        String source = "fun f(x) funktion:{x=x+1;x;}for(jahr in [1,2]){f(jahr);}";
        var body = doc(node("peelRepeat", Map.of("scope", "script", "scriptId", "script", "block", "funktion"),
                node("paragraph", Map.of(), node("peelValue", Map.of("scope", "current", "binding", "x")))));
        assertThat(renderer.render(document(source, body), Map.of())).containsSubsequence("<p>2</p>", "<p>3</p>");
        var invalid = doc(node("peelRepeat", Map.of("scope", "script", "scriptId", "script", "block", "funktion"),
                node("paragraph", Map.of(), node("peelValue", Map.of("scope", "current", "binding", "jahr")))));
        assertThatThrownBy(() -> renderer.render(document(source, invalid), Map.of())).hasMessageContaining("Unknown variable at block exit").hasMessageContaining("document/");
    }

    @Test
    void distinguishesNoneMissingAndMapPathsWithoutMutatingSnapshots() {
        String source = "fun f(){return;}teil:{var n=f();var bauteil={\"name\":\"<A>&\",\"betrag\":1.234};}";
        var value = node("peelValue", Map.of("scope", "script", "scriptId", "script", "block", "teil", "binding", "bauteil.name"));
        assertThat(renderer.render(document(source, doc(node("paragraph", Map.of(), value))), Map.of())).contains("&lt;A&gt;&amp;");
        var none = node("peelValue", Map.of("scope", "script", "scriptId", "script", "block", "teil", "binding", "n"));
        assertThat(renderer.render(document(source, doc(node("paragraph", Map.of(), none))), Map.of())).contains("<p>none</p>");
        for (String path : List.of("missing", "bauteil.missing", "n.name")) {
            var invalid = node("peelValue", Map.of("scope", "script", "scriptId", "script", "block", "teil", "binding", path));
            assertThatThrownBy(() -> renderer.render(document(source, doc(node("paragraph", Map.of(), invalid))), Map.of())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("document/");
        }
    }

    private Document document(String source, DocumentNode root) {
        return new Document("Test", Map.of("script", new PeelScript("Script", source, java.util.List.of())), root, ExpressionRenderConfiguration.defaultConfig());
    }

    private DocumentNode selected(String block) {
        return node("peelCalculation", Map.of("scope", "script", "scriptId", "script", "block", block, "mode", "detailed"));
    }

    private DocumentNode text(String text) {
        return new DocumentNode("text", Map.of(), List.of(), text, List.of());
    }

    private DocumentNode node(String type, Map<String, Object> attrs, DocumentNode... children) {
        return new DocumentNode(type, attrs, List.of(children), null, List.of());
    }
}
