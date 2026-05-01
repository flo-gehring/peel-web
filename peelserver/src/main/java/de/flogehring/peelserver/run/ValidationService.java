package de.flogehring.peelserver.run;

import de.flogehring.peel.antlr.PeelLexer;
import de.flogehring.peel.antlr.PeelParser;
import de.flogehring.peel.parse.PeelGrammar;
import de.flogehring.peel.run.exceptions.PeelException;
import de.flogehring.peelserver.api.ValidationDiagnostic;
import de.flogehring.peelserver.api.ValidationRequest;
import de.flogehring.peelserver.api.ValidationResponse;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {

    public ValidationResponse validate(ValidationRequest request) {
        if (request == null || request.script() == null || request.script().isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        List<ValidationDiagnostic> diagnostics = new ArrayList<>();
        collectSyntaxErrors(request.script(), diagnostics);
        if (!diagnostics.isEmpty()) {
            return new ValidationResponse(List.copyOf(diagnostics));
        }
        collectSemanticErrors(request.script(), diagnostics);
        return new ValidationResponse(List.copyOf(diagnostics));
    }

    private void collectSyntaxErrors(String script, List<ValidationDiagnostic> diagnostics) {
        PeelLexer lexer = new PeelLexer(CharStreams.fromString(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PeelParser parser = new PeelParser(tokens);

        lexer.removeErrorListeners();
        parser.removeErrorListeners();

        BaseErrorListener listener = new BaseErrorListener() {
            @Override
            public void syntaxError(
                    Recognizer<?, ?> recognizer,
                    Object offendingSymbol,
                    int line,
                    int charPositionInLine,
                    String msg,
                    RecognitionException e
            ) {
                int startColumn = charPositionInLine + 1;
                int endColumn = startColumn;
                if (offendingSymbol instanceof CommonToken token) {
                    int tokenWidth = Math.max(1, token.getStopIndex() - token.getStartIndex() + 1);
                    endColumn = startColumn + tokenWidth;
                }
                diagnostics.add(new ValidationDiagnostic(
                        Math.max(1, line),
                        startColumn,
                        Math.max(1, line),
                        endColumn,
                        "error",
                        msg
                ));
            }
        };

        lexer.addErrorListener(listener);
        parser.addErrorListener(listener);
        parser.program();
    }

    private void collectSemanticErrors(String script, List<ValidationDiagnostic> diagnostics) {
        try {
            PeelGrammar.parse(script);
        } catch (PeelException ex) {
            diagnostics.add(new ValidationDiagnostic(1, 1, 1, 2, "error", ex.getMessage()));
        }
    }
}
