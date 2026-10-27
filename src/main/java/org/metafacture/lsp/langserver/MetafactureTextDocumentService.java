package org.metafacture.lsp.langserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
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

        int previousLine = 0;
        int previousStart = 0;
        int line = 0;
        int lineStart = 0;
        while (lineStart <= text.length()) {
            int newline = text.indexOf('\n', lineStart);
            int lineEnd = newline == -1 ? text.length() : newline;
            if (lineEnd > lineStart && text.charAt(lineEnd - 1) == '\r') {
                lineEnd--;
            }

            char quote = 0;
            boolean escaped = false;
            for (int index = lineStart; index < lineEnd; index++) {
                char current = text.charAt(index);
                if (quote != 0) {
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == quote) {
                        quote = 0;
                    }
                } else if (current == '\'' || current == '"') {
                    quote = current;
                } else if (current == '/'
                        && index + 1 < lineEnd
                        && text.charAt(index + 1) == '/'
                        && !isHttpUrl(text, index)
                        && (index == lineStart || Character.isWhitespace(text.charAt(index - 1)))) {
                    int start = index - lineStart;
                    tokens.add(line - previousLine);
                    tokens.add(line == previousLine ? start - previousStart : start);
                    tokens.add(lineEnd - index);
                    tokens.add(0);
                    tokens.add(0);
                    previousLine = line;
                    previousStart = start;
                    break;
                }
            }

            if (newline == -1) {
                break;
            }
            line++;
            lineStart = newline + 1;
        }
        return tokens;
    }

    private boolean isHttpUrl(String text, int slashIndex) {
        int colonIndex = slashIndex - 1;
        if (colonIndex < 0 || text.charAt(colonIndex) != ':') {
            return false;
        }

        int schemeStart = colonIndex - 1;
        while (schemeStart >= 0
                && (Character.isLetterOrDigit(text.charAt(schemeStart))
                        || text.charAt(schemeStart) == '+'
                        || text.charAt(schemeStart) == '-'
                        || text.charAt(schemeStart) == '.')) {
            schemeStart--;
        }
        String scheme = text.substring(schemeStart + 1, colonIndex);
        return scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https");
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
