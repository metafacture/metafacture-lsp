package langserver;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

public class MetafactureTextDocumentService implements TextDocumentService {
    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
        // Provide completion item.
        return CompletableFuture.supplyAsync(() -> {
            List<CompletionItem> completionItems = new ArrayList<>();

            String fileName = "/Users/tauber/git/metafacture-core/metafacture-biblio/src/main/resources/flux-commands.properties";
            try {
                File file = new File(fileName);
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                String line;
                while((line = br.readLine()) != null){
                    if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                        continue; // Skip comments and empty lines
                    } else {
                        String[] lineParts = line.split("\\s");
                        String command = lineParts[0].trim();
                        String className = lineParts[1].trim();
                        CompletionItem completionItem = new CompletionItem();
                        completionItem.setInsertText(command);
                        completionItem.setLabel(command);
                        completionItem.setKind(CompletionItemKind.Function);
                        completionItem.setDetail(command + "\n by " + className);
                        completionItems.add(completionItem);
                    }
                }
                br.close();
                fr.close();
            } catch (Exception e) {
                e.printStackTrace();
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
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {

    }

    @Override
    public void didChange(DidChangeTextDocumentParams didChangeTextDocumentParams) {

    }

    @Override
    public void didClose(DidCloseTextDocumentParams didCloseTextDocumentParams) {

    }

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {

    }

}
