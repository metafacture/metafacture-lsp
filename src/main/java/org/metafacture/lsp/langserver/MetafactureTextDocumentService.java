package org.metafacture.lsp.langserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.metafacture.flux.parser.FluxLexer;
import org.metafacture.framework.FluxCommand;
import org.metafacture.framework.annotations.Description;
import org.metafacture.framework.annotations.In;
import org.metafacture.framework.annotations.Out;
import org.reflections.Reflections;

public class MetafactureTextDocumentService implements TextDocumentService {
    private final Map<String, String> documents = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            CompletionParams completionParams) {
        return CompletableFuture.supplyAsync(() -> Either.forLeft(getCompletionItems().toList()));
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        String text = documents.get(params.getTextDocument().getUri());
        return CompletableFuture.completedFuture(new SemanticTokens(getCommentTokens(text)));
    }

    private List<Integer> getCommentTokens(String text) {
        List<Integer> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        CommonTokenStream tokenStream =
                new CommonTokenStream(new FluxLexer(new ANTLRStringStream(text)));
        tokenStream.fill();

        int previousLine = 0;
        int previousStart = 0;
        for (Token token : tokenStream.getTokens()) {
            if (token.getType() != FluxLexer.LINE_COMMENT) {
                continue;
            }

            String comment = token.getText();
            int length = comment.length();
            while (length > 0 && endsWithLineBreak(comment, length)) {
                length--;
            }
            if (length == 0) {
                continue;
            }

            int line = token.getLine() - 1;
            int start = token.getCharPositionInLine();
            // LSP semantic token:
            // delta line (the difference from the previous comment’s line)
            tokens.add(line - previousLine);
            // delta/start column
            // on the same line as the previous comment = start difference from that comment
            // on another line than the previous comment = absolute character position
            tokens.add(line == previousLine ? start - previousStart : start);
            tokens.add(length);
            tokens.add(FluxSemanticTokens.INSTANCE.commentType());
            tokens.add(FluxSemanticTokens.INSTANCE.noModifiers());
            previousLine = line;
            previousStart = start;
        }
        return tokens;
    }

    private boolean endsWithLineBreak(String comment, int length) {
        char lastCharacter = comment.charAt(length - 1);
        return lastCharacter == '\r' || lastCharacter == '\n';
    }

    private Stream<CompletionItem> getCompletionItems() {
        return new Reflections("org.metafacture")
                .getTypesAnnotatedWith(FluxCommand.class).stream().map(toCompletionItem());
    }

    private Function<Class<?>, CompletionItem> toCompletionItem() {
        return annotatedClass -> {
            String fluxCommand = annotatedClass.getAnnotation(FluxCommand.class).value();
            CompletionItem completionItem = new CompletionItem(fluxCommand);
            completionItem.setInsertText(fluxCommand);
            completionItem.setKind(CompletionItemKind.Function);
            var optionalDesc = Optional.ofNullable(annotatedClass.getAnnotation(Description.class));
            var optionalIn = Optional.ofNullable(annotatedClass.getAnnotation(In.class));
            var optionalOut = Optional.ofNullable(annotatedClass.getAnnotation(Out.class));
            completionItem.setDetail(
                    String.format(
                            "In: %s | Out: %s",
                            optionalIn.map(in -> in.value().getSimpleName()).orElse(""),
                            optionalOut.map(out -> out.value().getSimpleName()).orElse("")));
            completionItem.setDocumentation(optionalDesc.map(Description::value).orElse(""));
            return completionItem;
        };
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem completionItem) {
        return null;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {
        documents.put(
                didOpenTextDocumentParams.getTextDocument().getUri(),
                didOpenTextDocumentParams.getTextDocument().getText());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams didChangeTextDocumentParams) {
        List<TextDocumentContentChangeEvent> changes =
                didChangeTextDocumentParams.getContentChanges();
        if (!changes.isEmpty()) {
            documents.put(
                    didChangeTextDocumentParams.getTextDocument().getUri(),
                    changes.get(changes.size() - 1).getText());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams didCloseTextDocumentParams) {
        documents.remove(didCloseTextDocumentParams.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {}
}
