package org.metafacture.lsp.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.metafacture.lsp.langserver.MetafactureTextDocumentService;

@DisplayName("MetafactureTextDocumentService Completion Tests - Basic Functionality")
class MetafactureTextDocumentServiceTests {

    private final MetafactureTextDocumentService textDocumentService =
            new MetafactureTextDocumentService();

    private CompletionParams createCompletionParams() {
        CompletionParams completionParams = new CompletionParams();
        completionParams.setTextDocument(new TextDocumentIdentifier("file:///test-document.flux"));
        completionParams.setPosition(new Position(0, 1));
        completionParams.setContext(new CompletionContext(CompletionTriggerKind.Invoked, null));
        return completionParams;
    }

    @Test
    @DisplayName("Completion returns non-null CompletableFuture")
    void testCompletionReturnsNonNullFuture() {
        CompletionParams completionParams = createCompletionParams();
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> result =
                textDocumentService.completion(completionParams);
        assertNotNull(result, "Completion should return a non-null CompletableFuture");
    }

    @Test
    @DisplayName("Completion future completes successfully without exceptions")
    void testCompletionFutureCompletesSuccessfully()
            throws ExecutionException, InterruptedException {
        CompletionParams completionParams = createCompletionParams();
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> result =
                textDocumentService.completion(completionParams);
        Either<List<CompletionItem>, CompletionList> completionResult = result.get();
        assertNotNull(completionResult, "Future should complete with non-null result");
        assertTrue(
                completionResult.isLeft(),
                "Result should contain a list of completion items (Left)");
    }

    @Test
    @DisplayName("Completion result contains a list of completion items")
    void testCompletionResultContainsItems() throws ExecutionException, InterruptedException {
        CompletionParams completionParams = createCompletionParams();
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> result =
                textDocumentService.completion(completionParams);
        List<CompletionItem> items = result.get().getLeft();
        assertNotNull(items, "Completion items list should not be null");
        assertFalse(items.isEmpty(), "Completion items list should not be empty");
        assertTrue(items.size() > 0, "Should have at least one completion item");
    }

    @Test
    @DisplayName("Completion items have non-null label and insertText")
    void testCompletionItemsHaveRequiredFields() throws ExecutionException, InterruptedException {
        CompletionParams completionParams = createCompletionParams();
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> result =
                textDocumentService.completion(completionParams);
        List<CompletionItem> items = result.get().getLeft();
        for (CompletionItem item : items) {
            assertNotNull(item.getLabel(), "Completion item label should not be null");
            assertFalse(item.getLabel().isEmpty(), "Completion item label should not be empty");
            assertNotNull(item.getInsertText(), "Completion item insertText should not be null");
            assertEquals(
                    item.getLabel(),
                    item.getInsertText(),
                    "insertText should match label for command names");
        }
    }

    @Test
    @DisplayName("Completion items have CompletionItemKind set to Function")
    void testCompletionItemsHaveCorrectKind() throws ExecutionException, InterruptedException {
        CompletionParams completionParams = createCompletionParams();
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> result =
                textDocumentService.completion(completionParams);
        List<CompletionItem> items = result.get().getLeft();
        for (CompletionItem item : items) {
            assertNotNull(item.getKind(), "Completion item kind should not be null");
            assertEquals(
                    org.eclipse.lsp4j.CompletionItemKind.Function,
                    item.getKind(),
                    "All items should have CompletionItemKind.Function");
        }
    }

    @Test
    @DisplayName("Completion items have detail information")
    void testCompletionItemsHaveDetailInformation()
            throws ExecutionException, InterruptedException {
        CompletionParams completionParams = createCompletionParams();
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> result =
                textDocumentService.completion(completionParams);
        List<CompletionItem> items = result.get().getLeft();
        for (CompletionItem item : items) {
            assertNotNull(item.getDetail(), "Completion item detail should not be null");
            assertTrue(
                    item.getDetail().contains("|"),
                    "Detail should contain pipe separators for structured format");
            assertTrue(item.getDetail().contains("In:"), "Detail should contain 'In:' label");
            assertTrue(item.getDetail().contains("Out:"), "Detail should contain 'Out:' label");
        }
    }
}
