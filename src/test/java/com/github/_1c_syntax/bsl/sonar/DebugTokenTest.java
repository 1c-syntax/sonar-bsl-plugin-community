package com.github._1c_syntax.bsl.sonar;

import com.github._1c_syntax.bsl.languageserver.BSLLSBinding;
import com.github._1c_syntax.bsl.parser.BSLLexer;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class DebugTokenTest {
    @Test
    void debugTokenPositions() {
        var baseDirName = "src/test/resources/examples";
        var fileName = "highlightCrmQuery.bsl";
        var path = Path.of(baseDirName, fileName);
        var documentContext = BSLLSBinding.getServerContext().addDocument(path.toUri());
        BSLLSBinding.getServerContext().rebuildDocument(documentContext);

        System.out.println("=== BSL Tokens (STRING types only) ===");
        for (var token : documentContext.getTokens()) {
            String typeName = BSLLexer.VOCABULARY.getSymbolicName(token.getType());
            if (typeName != null && typeName.contains("STRING")) {
                System.out.printf("BSL[%s] line=%d, charPos=%d, textLen=%d, text='%s'%n",
                    typeName, token.getLine(), token.getCharPositionInLine(),
                    token.getText().length(),
                    token.getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));
            }
        }

        System.out.println("\n=== SDBL Tokens (line 86 area, the СГРУППИРОВАТЬ line) ===");
        for (var tokenizer : documentContext.getQueries()) {
            for (var token : tokenizer.getTokens()) {
                if (token.getType() == Token.EOF) continue;
                // СГРУППИРОВАТЬ is at BSL line 86 (1800 - 1715 + 1)
                if (token.getLine() >= 83 && token.getLine() <= 90) {
                    int endChar = token.getCharPositionInLine() + (int) token.getText().stripTrailing().codePoints().count();
                    System.out.printf("SDBL[type=%d] line=%d, charPos=%d, endChar=%d, text='%s'%n",
                        token.getType(), token.getLine(), token.getCharPositionInLine(),
                        endChar,
                        token.getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));
                }
            }
        }
    }
}
