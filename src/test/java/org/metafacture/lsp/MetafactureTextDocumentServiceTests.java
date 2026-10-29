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
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.metafacture.lsp.langserver.MetafactureLanguageServer;
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
        assertNotNull(textDocumentService.completion(createCompletionParams()));
    }

    @Test
    @DisplayName("Completion future completes successfully without exceptions")
    void testCompletionFutureCompletesSuccessfully()
            throws ExecutionException, InterruptedException {
        Either<List<CompletionItem>, CompletionList> completionResult =
                textDocumentService.completion(createCompletionParams()).get();
        assertNotNull(completionResult, "Future should complete with non-null result");
        assertTrue(completionResult.isLeft());
    }

    @Test
    @DisplayName("Completion result contains a list of completion items")
    void testCompletionResultContainsItems() throws ExecutionException, InterruptedException {
        List<CompletionItem> items = getCompletionItems();
        assertNotNull(items);
        assertFalse(items.isEmpty());
        assertTrue(items.size() > 0);
    }

    @Test
    @DisplayName("Completion items have non-null label and insertText")
    void testCompletionItemsHaveRequiredFields() throws ExecutionException, InterruptedException {
        for (CompletionItem item : getCompletionItems()) {
            assertNotNull(item.getLabel(), "Completion item label should not be null");
            assertFalse(item.getLabel().isEmpty(), "Completion item label should not be empty");
            assertNotNull(item.getInsertText(), "Completion item insertText should not be null");
            assertEquals(item.getLabel(), item.getInsertText());
        }
    }

    @Test
    @DisplayName("Completion items have CompletionItemKind set to Function")
    void testCompletionItemsHaveCorrectKind() throws ExecutionException, InterruptedException {
        for (CompletionItem item : getCompletionItems()) {
            assertNotNull(item.getKind(), "Completion item kind should not be null");
            assertEquals(CompletionItemKind.Function, item.getKind());
        }
    }

    @Test
    @DisplayName("Completion items have detail information")
    void testCompletionItemsHaveDetailInformation()
            throws ExecutionException, InterruptedException {
        for (CompletionItem item : getCompletionItems()) {
            assertNotNull(item.getDetail(), "Completion item detail should not be null");
            assertTrue(item.getDetail().contains("|"));
            assertTrue(item.getDetail().contains("In:"));
            assertTrue(item.getDetail().contains("Out:"));
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
            assertTrue(completionLabels.contains(expectedCommand));
        }
    }

    @Test
    @DisplayName("'decode-marc21' completion item has correct insertText")
    void testDecodeMarc21HasCorrectInsertText() throws ExecutionException, InterruptedException {
        Optional<CompletionItem> decodeMarc21 =
                findCompletionItemByLabel(getCompletionItems(), "decode-marc21");
        assertTrue(decodeMarc21.isPresent());
        assertEquals(decodeMarc21.get().getLabel(), decodeMarc21.get().getInsertText());
    }

    @Test
    @DisplayName("'decode-marc21' completion item has detail with In and Out information")
    void testDecodeMarc21HasCorrectDetail() throws ExecutionException, InterruptedException {
        Optional<CompletionItem> decodeMarc21 =
                findCompletionItemByLabel(getCompletionItems(), "decode-marc21");
        assertTrue(decodeMarc21.isPresent(), "decode-marc21 command should exist");
        assertTrue(decodeMarc21.get().getDetail().contains("In:"));
        assertTrue(decodeMarc21.get().getDetail().contains("Out:"));
    }

    @Test
    @DisplayName("All completion items have non-empty labels")
    void testAllCompletionItemsHaveNonEmptyLabels()
            throws ExecutionException, InterruptedException {
        for (CompletionItem item : getCompletionItems()) {
            assertFalse(item.getLabel().isEmpty());
            assertFalse(item.getLabel().isBlank());
        }
    }

    @Test
    @DisplayName("Completion item labels are unique")
    void testCompletionItemLabelsAreUnique() throws ExecutionException, InterruptedException {
        List<String> labels = getCompletionItems().stream().map(CompletionItem::getLabel).toList();
        long uniqueLabels = labels.stream().distinct().count();
        assertEquals(labels.size(), uniqueLabels);
    }

    @Test
    @DisplayName("Semantic tokens highlight Flux line comments outside strings")
    void testSemanticTokensHighlightCommentsOutsideStrings()
            throws ExecutionException, InterruptedException {
        String text = "// first\r\n\"not // a comment\" pass-through // second\r\npass-through";
        String uri = "file:///semantic-tokens.flux";
        DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
        openParams.setTextDocument(new TextDocumentItem(uri, "flux", 1, text));
        textDocumentService.didOpen(openParams);

        SemanticTokensParams params = new SemanticTokensParams();
        params.setTextDocument(new TextDocumentIdentifier(uri));
        List<Integer> data = textDocumentService.semanticTokensFull(params).get().getData();
        int secondComment = text.indexOf("// second");
        int secondLineStart = text.lastIndexOf('\n', secondComment) + 1;
        int secondLineEnd = text.indexOf("\r\n", secondComment);

        assertEquals(
                List.of(
                        0,
                        0,
                        8,
                        0,
                        0,
                        1,
                        secondComment - secondLineStart,
                        secondLineEnd - secondComment,
                        0,
                        0),
                data);
    }

    @Test
    @DisplayName("Semantic tokens do not highlight HTTP URLs as comments")
    void testSemanticTokensDoNotHighlightUrlsAsComments()
            throws ExecutionException, InterruptedException {
        String url = "https://lobid.org/gnd/11942150X";
        String text = url + "\n\"" + url + "\"\n// actual comment";
        String uri = "file:///semantic-tokens-url.flux";
        DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
        openParams.setTextDocument(new TextDocumentItem(uri, "flux", 1, text));
        textDocumentService.didOpen(openParams);

        SemanticTokensParams params = new SemanticTokensParams();
        params.setTextDocument(new TextDocumentIdentifier(uri));

        assertEquals(
                List.of(2, 0, "// actual comment".length(), 0, 0),
                textDocumentService.semanticTokensFull(params).get().getData());
    }

    @Test
    @DisplayName("Semantic tokens use updated document content")
    void testSemanticTokensUpdateAfterDocumentChange()
            throws ExecutionException, InterruptedException {
        String uri = "file:///semantic-tokens-change.flux";
        DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
        openParams.setTextDocument(new TextDocumentItem(uri, "flux", 1, "pass-through"));
        textDocumentService.didOpen(openParams);

        DidChangeTextDocumentParams changeParams = new DidChangeTextDocumentParams();
        changeParams.setTextDocument(new org.eclipse.lsp4j.VersionedTextDocumentIdentifier(uri, 2));
        changeParams.setContentChanges(List.of(new TextDocumentContentChangeEvent("// updated")));
        textDocumentService.didChange(changeParams);

        SemanticTokensParams params = new SemanticTokensParams();
        params.setTextDocument(new TextDocumentIdentifier(uri));
        SemanticTokens tokens = textDocumentService.semanticTokensFull(params).get();

        assertEquals(List.of(0, 0, 10, 0, 0), tokens.getData());
    }

    @Test
    @DisplayName("Language server advertises comment semantic tokens")
    void testServerAdvertisesCommentSemanticTokens() throws Exception {
        var result = new MetafactureLanguageServer().initialize(new InitializeParams()).get();
        SemanticTokensWithRegistrationOptions options =
                result.getCapabilities().getSemanticTokensProvider();

        assertEquals(List.of(SemanticTokenTypes.Comment), options.getLegend().getTokenTypes());
        assertTrue(options.getFull().isLeft());
        assertTrue(options.getFull().getLeft());
    }
}
