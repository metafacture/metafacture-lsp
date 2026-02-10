package org.metafacture.lsp.langserver;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.metafacture.framework.FluxCommand;
import org.metafacture.framework.annotations.Description;
import org.metafacture.framework.annotations.In;
import org.metafacture.framework.annotations.Out;
import org.reflections.Reflections;

public class MetafactureTextDocumentService implements TextDocumentService {

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            CompletionParams completionParams) {
        return CompletableFuture.supplyAsync(() -> Either.forLeft(getCompletionItems().toList()));
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
                            "%s | In: %s | Out: %s",
                            optionalDesc.map(Description::value).orElse(""),
                            optionalIn.map(in -> in.value().getSimpleName()).orElse(""),
                            optionalOut.map(out -> out.value().getSimpleName()).orElse("")));
            return completionItem;
        };
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem completionItem) {
        return null;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {}

    @Override
    public void didChange(DidChangeTextDocumentParams didChangeTextDocumentParams) {}

    @Override
    public void didClose(DidCloseTextDocumentParams didCloseTextDocumentParams) {}

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {}
}
