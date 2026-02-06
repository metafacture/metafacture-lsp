package langserver;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
        // Provide completion item.
        return CompletableFuture.supplyAsync(
                () -> {
                    List<CompletionItem> completionItems = new ArrayList<>();

                    // Get all classes annotated with @FluxCommand
                    Reflections reflections = new Reflections("org.metafacture");
                    Set<Class<?>> annotatedClasses =
                            reflections.getTypesAnnotatedWith(FluxCommand.class);

                    for (Class<?> clazz : annotatedClasses) {
                        // Get class annotations
                        Annotation[] classAnnotations = clazz.getAnnotations();
                        String command = "";
                        String detail = "";

                        for (Annotation annotation : classAnnotations) {
                            Class<? extends Annotation> annotationType =
                                    annotation.annotationType();
                            if (annotationType == FluxCommand.class) {
                                FluxCommand fluxCommand = (FluxCommand) annotation;
                                command = fluxCommand.value();
                            } else if (annotationType == Description.class) {
                                Description description = (Description) annotation;
                                detail = description.value();
                            } else if (annotationType == In.class) {
                                In in = (In) annotation;
                                detail += "| In: " + String.join(", ", in.value().getSimpleName());
                            } else if (annotationType == Out.class) {
                                Out out = (Out) annotation;
                                detail +=
                                        "| Out: " + String.join(", ", out.value().getSimpleName());
                            } else {
                                continue;
                            }

                            CompletionItem completionItem = new CompletionItem();
                            completionItem.setInsertText(command);
                            completionItem.setLabel(command);
                            completionItem.setKind(CompletionItemKind.Function);
                            completionItem.setDetail(detail);
                            completionItems.add(completionItem);
                        }
                    }

                    // Return the list of completion items.
                    return Either.forLeft(completionItems);
                });
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
