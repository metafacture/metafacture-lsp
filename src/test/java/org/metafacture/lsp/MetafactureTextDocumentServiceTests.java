package org.metafacture.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.metafacture.lsp.langserver.MetafactureTextDocumentService;

@DisplayName("MetafactureTextDocumentService Completion Tests")
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

    private Optional<CompletionItem> findCompletionItemByLabel(
            List<CompletionItem> items, String label) {
        return items.stream().filter(item -> label.equals(item.getLabel())).findFirst();
    }

    private List<CompletionItem> getCompletionItems()
            throws InterruptedException, ExecutionException {
        return textDocumentService.completion(createCompletionParams()).get().getLeft();
    }

    @Test
    @DisplayName("Completion returns non-null CompletableFuture")
    void testCompletionReturnsNonNullFuture() {
        assertNotNull(
                textDocumentService.completion(createCompletionParams()),
                "Completion should return a non-null CompletableFuture");
    }

    @Test
    @DisplayName("Completion future completes successfully without exceptions")
    void testCompletionFutureCompletesSuccessfully()
            throws ExecutionException, InterruptedException {
        Either<List<CompletionItem>, CompletionList> completionResult =
                textDocumentService.completion(createCompletionParams()).get();
        assertNotNull(completionResult, "Future should complete with non-null result");
        assertTrue(
                completionResult.isLeft(),
                "Result should contain a list of completion items (Left)");
    }

    @Test
    @DisplayName("Completion result contains a list of completion items")
    void testCompletionResultContainsItems() throws ExecutionException, InterruptedException {
        List<CompletionItem> items = getCompletionItems();
        assertNotNull(items, "Completion items list should not be null");
        assertFalse(items.isEmpty(), "Completion items list should not be empty");
        assertTrue(items.size() > 0, "Should have at least one completion item");
    }

    @Test
    @DisplayName("Completion items have non-null label and insertText")
    void testCompletionItemsHaveRequiredFields() throws ExecutionException, InterruptedException {
        for (CompletionItem item : getCompletionItems()) {
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
        for (CompletionItem item : getCompletionItems()) {
            assertNotNull(item.getKind(), "Completion item kind should not be null");
            assertEquals(
                    CompletionItemKind.Function,
                    item.getKind(),
                    "All items should have CompletionItemKind.Function");
        }
    }

    @Test
    @DisplayName("Completion items have detail information")
    void testCompletionItemsHaveDetailInformation()
            throws ExecutionException, InterruptedException {
        for (CompletionItem item : getCompletionItems()) {
            assertNotNull(item.getDetail(), "Completion item detail should not be null");
            assertTrue(
                    item.getDetail().contains("|"),
                    "Detail should contain pipe separators for structured format");
            assertTrue(item.getDetail().contains("In:"), "Detail should contain 'In:' label");
            assertTrue(item.getDetail().contains("Out:"), "Detail should contain 'Out:' label");
        }
    }

    // ==================== Content Verification Tests ====================

    @Test
    @DisplayName("Completion contains multiple known Metafacture commands")
    void testCompletionContainsMultipleKnownCommands()
            throws ExecutionException, InterruptedException {
        List<String> completionLabels =
                getCompletionItems().stream().map(CompletionItem::getLabel).toList();
        for (String expectedCommand :
                new String[] {"decode-marc21", "encode-marc21", "pass-through"}) {
            assertTrue(
                    completionLabels.contains(expectedCommand),
                    "Completion should contain '" + expectedCommand + "' command");
        }
    }

    @Test
    @DisplayName("'decode-marc21' completion item has correct insertText")
    void testDecodeMarc21HasCorrectInsertText() throws ExecutionException, InterruptedException {
        Optional<CompletionItem> decodeMarc21 =
                findCompletionItemByLabel(getCompletionItems(), "decode-marc21");
        assertTrue(decodeMarc21.isPresent(), "decode-marc21 command should exist");
        assertEquals(
                decodeMarc21.get().getLabel(),
                decodeMarc21.get().getInsertText(),
                "insertText should match the command label");
    }

    @Test
    @DisplayName("'decode-marc21' completion item has detail with In and Out information")
    void testDecodeMarc21HasCorrectDetail() throws ExecutionException, InterruptedException {
        Optional<CompletionItem> decodeMarc21 =
                findCompletionItemByLabel(getCompletionItems(), "decode-marc21");
        assertTrue(decodeMarc21.isPresent(), "decode-marc21 command should exist");
        assertTrue(
                decodeMarc21.get().getDetail().contains("In:"),
                "Detail should indicate input type");
        assertTrue(
                decodeMarc21.get().getDetail().contains("Out:"),
                "Detail should indicate output type");
    }

    @Test
    @DisplayName("All completion items have non-empty labels")
    void testAllCompletionItemsHaveNonEmptyLabels()
            throws ExecutionException, InterruptedException {
        for (CompletionItem item : getCompletionItems()) {
            assertFalse(
                    item.getLabel().isEmpty(), "Every completion item must have a non-empty label");
            assertFalse(
                    item.getLabel().isBlank(),
                    "Every completion item label must contain non-whitespace characters");
        }
    }

    @Test
    @DisplayName("Completion item labels are unique")
    void testCompletionItemLabelsAreUnique() throws ExecutionException, InterruptedException {
        List<String> labels = getCompletionItems().stream().map(CompletionItem::getLabel).toList();
        long uniqueLabels = labels.stream().distinct().count();
        assertEquals(
                labels.size(),
                uniqueLabels,
                "All completion item labels should be unique, but found duplicates");
    }
}
