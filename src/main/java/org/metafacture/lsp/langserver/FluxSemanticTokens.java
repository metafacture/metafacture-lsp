package org.metafacture.lsp.langserver;

import java.util.List;
import org.eclipse.lsp4j.SemanticTokenTypes;

public record FluxSemanticTokens(List<String> types, List<String> modifiers) {
    public static final FluxSemanticTokens INSTANCE =
            new FluxSemanticTokens(List.of(SemanticTokenTypes.Comment), List.of());

    public int commentType() {
        return types.indexOf(SemanticTokenTypes.Comment);
    }

    public int noModifiers() {
        return 0;
    }
}
