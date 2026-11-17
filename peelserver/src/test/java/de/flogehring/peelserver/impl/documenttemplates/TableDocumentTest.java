package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.api.data.template.DocumentNode;
import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.impl.run.BindingsMapper;
import de.flogehring.peelserver.impl.run.PeelExecutionService;
import de.flogehring.peelserver.impl.scripts.PeelScript;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TableDocumentTest {
    final PeelExecutionService execution = spy(new PeelExecutionService(new BindingsMapper()));
    final DocumentTemplateRenderService renderer = new DocumentTemplateRenderService(execution);

    @Test
    void rendersTenRowsWithFinalValuesAndEscapedHeaders() {
        String source = "var n=0;while(!(n==10)) year:{n=n+1;var amount=n;amount=amount*10;}";
        var table = table("year", column("Year <&>", value("n")), column("Amount", value("amount")));
        String html = renderer.render(document(source, table), Map.of());
        assertThat(html).contains("<th scope=\"col\"", "Year &lt;&amp;&gt;")
                .containsSubsequence("<p>1</p>", "<p>10</p>", "<p>2</p>", "<p>20</p>", "<p>10</p>", "<p>100</p>");
        assertThat(html.split("<tbody>")[1].split("</tbody>")[0].split("<tr>", -1)).hasSize(11);
        verify(execution).execute(source, Map.of());
    }

    @Test
    void emptyTablesWarnAndUnknownNamesFail() {
        String source = "if(False) absent:{}";
        var doc = document(source, table("absent", column("Year", value(""))));
        assertThat(renderer.render(doc, Map.of())).doesNotContain("<table");
        assertThat(renderer.preview(doc, Map.of()).warnings()).singleElement().asString().contains("absent");
        assertThatThrownBy(() -> renderer.render(document(source, table("unknown", column("X", value("")))), Map.of()))
                .hasMessageContaining("Unknown block");
    }

    @Test
    void cellErrorsIdentifyRowAndColumnAndNoneRemainsAValue() {
        String source = "fun f(){return;}for(i in [1,2]) row:{var m={};if(i==1){m={\"value\":3};}}";
        assertThatThrownBy(() -> renderer.render(document(source, table("row", column("X", value("m.value")))), Map.of()))
                .hasMessageContaining("row/1/column/0").hasMessageContaining("Unknown binding field");
        assertThat(renderer.render(document("fun f(){return;}row:{var x=f();}", table("row", column("X", value("x")))), Map.of()))
                .contains("<p>none</p>");
    }

    @Test
    void tablesInRepeatsUseTheCurrentOuterOccurrenceAndRespectShadowing() {
        String source = "var x=100;for(i in [1,2]) outer:{for(j in [3,4]) inner:{var x=i*j;}}";
        var inner = node("peelTable", Map.of("scope", "current", "block", "inner"), column("X", value("x")));
        var repeat = node("peelRepeat", Map.of("scope", "script", "scriptId", "s", "block", "outer"), inner);
        String html = renderer.render(document(source, repeat), Map.of());
        assertThat(html).containsSubsequence("<p>3</p>", "<p>4</p>", "<p>6</p>", "<p>8</p>");
        assertThat(html.split("<tbody>", -1)).hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"detailed", "compact", "substituted", "result"})
    void calculationsInCellsReuseDisplayModes(String mode) {
        var calculation = node("peelCalculation", Map.of("scope", "current", "mode", mode));
        String html = renderer.render(document("for(i in [1,2]) row:{i*2;}", table("row", column("X", calculation))), Map.of());
        assertThat(html).contains("peel-calculation", "4");
        if (mode.equals("substituted")) assertThat(html).contains("(2 * 2) = 4").doesNotContain("(i * 2)");
    }

    @Test
    void formatsMapsAndRetainsScriptReferencesInsideColumns() {
        var global = node("peelValue", Map.of("scope", "script", "scriptId", "s", "binding", "input.name"));
        var col = node("peelTableColumn", Map.of("title", "Header", "align", "right"), node("paragraph", Map.of(), global,
                node("peelValue", Map.of("scope", "current", "binding", "amount", "decimals", 2))));
        var root = document("row:{var amount=1.234;}", table("row", col));
        assertThat(root.document().scriptIds()).containsExactly("s");
        String html = renderer.render(root, Map.of("input", Map.of("name", "<A>&")));
        assertThat(html).contains("text-align:right", "&lt;A&gt;&amp;", "1.23");
    }

    @Test
    void rejectsInvalidTableStructuresBeforeExecution() {
        var cell = column("X", value(""));
        for (var invalid : List.of(cell, table("row"),
                table("row", node("paragraph", Map.of())),
                table("row", column("X", table("row", cell))),
                table("row", node("peelTableColumn", Map.of("align", "bad"), node("paragraph", Map.of()))),
                table("row", node("peelTableColumn", Map.of(), node("peelRepeat", Map.of("scope", "current"), node("paragraph", Map.of())))))) {
            assertThatThrownBy(() -> document("row:{}", invalid).document().validateDocument()).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(execution);
    }

    @Test
    void sharesOneExecutionWithOtherTablesAndFreeRepetitions() {
        String source = "for(i in [1,2]) row:{i*2;}";
        var table = table("row", column("Value", value("")));
        var repeat = node("peelRepeat", Map.of("scope", "script", "scriptId", "s", "block", "row"), node("paragraph", Map.of(), value("")));
        renderer.render(document(source, table, table, repeat), Map.of());
        verify(execution, times(1)).execute(source, Map.of());
    }

    @Test
    void rendersFourInterestColumnsForTenYears() throws Exception {
        String source = "var jahr=0;var kapital=1000;while(!(jahr==10)) year:{jahr=jahr+1;var start=kapital;var zinsen=start*0.05;kapital=start+zinsen;}";
        var columns = new ArrayList<DocumentNode>();
        columns.add(column("Jahr", value("jahr")));
        for (String name : List.of("start", "zinsen", "kapital"))
            columns.add(column(name,
                    node("peelValue", Map.of("scope", "current", "binding", name, "decimals", 2))));
        var doc = document(source, table("year", columns.toArray(DocumentNode[]::new)));
        String html = renderer.render(doc, Map.of());
        assertThat(html).containsSubsequence("<p>1</p>", "<p>1000.00</p>", "<p>50.00</p>", "<p>1050.00</p>", "<p>10</p>", "<p>1628.89</p>");
        assertThat(html.split("<th scope=", -1)).hasSize(5);
        assertThat(html.split("<tbody>")[1].split("<tr>", -1)).hasSize(11);
        Files.write(Path.of("build/table-interest.pdf"), renderer.preview(doc, Map.of()).pdfData());
    }

    @Test
    void missingVariableInOnlyTheSecondOccurrenceReportsItsCell() {
        String source = "fun f(i) row:{if(i==2){return;}var x=i;}f(1);f(2);";
        assertThatThrownBy(() -> renderer.render(document(source, table("row", column("X", value("x")))), Map.of()))
                .hasMessageContaining("row/1/column/0").hasMessageContaining("Unknown variable at block exit: x");
    }

    @Test
    void repeatsHeadersAcrossPdfPagesAndRetainsAllRows() throws Exception {
        String source = "var n=0;while(!(n==100)) row:{n=n+1;}";
        var doc = document(source, table("row", column("HEADER", value("n"))));
        byte[] pdf = renderer.preview(doc, Map.of()).pdfData();
        Files.write(Path.of("build/table-pagination.pdf"), pdf);
        try (var reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
            var extractor = new PdfTextExtractor(reader);
            var text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                String pageText = extractor.getTextFromPage(page);
                assertThat(pageText).contains("HEADER");
                text.append(pageText).append('\n');
            }
            for (int n = 1; n <= 100; n++) assertThat(text.toString()).containsPattern("(?m)^\\s*" + n + "\\s*$");
        }
    }

    @Test
    void aCellTallerThanOnePageDoesNotLoseContent() throws Exception {
        var paragraphs = new ArrayList<DocumentNode>();
        for (int i = 0; i < 180; i++)
            paragraphs.add(node("paragraph", Map.of(),
                    new DocumentNode("text", Map.of(), List.of(), "LINE-" + i, List.of())));
        var column = node("peelTableColumn", Map.of("title", "LONG HEADER", "align", "left"), paragraphs.toArray(DocumentNode[]::new));
        byte[] pdf = renderer.preview(document("row:{}", table("row", column)), Map.of()).pdfData();
        Files.write(Path.of("build/table-tall-row.pdf"), pdf);
        try (var reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
            var extractor = new PdfTextExtractor(reader);
            var text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++)
                text.append(extractor.getTextFromPage(page)).append('\n');
            for (int i = 0; i < 180; i++) assertThat(text.toString()).containsPattern("(?m)^\\s*LINE-" + i + "\\s*$");
        }
    }

    private Document document(String source, DocumentNode... body) {
        return new Document("Table", Map.of("s", new PeelScript("Script", source, java.util.List.of())), node("doc", Map.of(), body), ExpressionRenderConfiguration.defaultConfig());
    }

    private DocumentNode table(String block, DocumentNode... columns) {
        return node("peelTable", Map.of("scope", "script", "scriptId", "s", "block", block), columns);
    }

    private DocumentNode column(String title, DocumentNode... children) {
        if (children.length == 1 && children[0].type().equals("peelCalculation"))
            return node("peelTableColumn", Map.of("title", title, "align", "left"), children);
        return node("peelTableColumn", Map.of("title", title, "align", "left"), node("paragraph", Map.of(), children));
    }

    private DocumentNode value(String binding) {
        return node("peelValue", Map.of("scope", "current", "binding", binding));
    }

    private DocumentNode node(String type, Map<String, Object> attrs, DocumentNode... children) {
        return new DocumentNode(type, attrs, List.of(children), null, List.of());
    }
}
