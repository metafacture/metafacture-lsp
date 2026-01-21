package launcher;

import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageIssueException;
import org.eclipse.lsp4j.jsonrpc.MessageIssueHandler;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Message;

public class WebSocketMessageHandler {
    private MessageConsumer consumer;
    private MessageJsonHandler jsonHandler;
    private MessageIssueHandler issueHandler;

    public void setConfigs(MessageConsumer consumer, MessageJsonHandler jsonHandler, MessageIssueHandler issueHandler) {
        this.consumer = consumer;
        this.issueHandler = issueHandler;
        this.jsonHandler = jsonHandler;
    }

    public void onMessage(String content) {
        try {
            Message message = jsonHandler.parseMessage(content);
            consumer.consume(message);
        } catch (MessageIssueException exception) {
            issueHandler.handle(exception.getRpcMessage(), exception.getIssues());
        }
    }
}
