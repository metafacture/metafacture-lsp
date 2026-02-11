package org.metafacture.lsp.launcher;

import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageIssueException;
import org.eclipse.lsp4j.jsonrpc.MessageIssueHandler;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;

public class WebSocketMessageHandler {
    private MessageConsumer consumer;
    private MessageJsonHandler jsonHandler;
    private MessageIssueHandler issueHandler;

    public void setConfigs(
            MessageConsumer consumer,
            MessageJsonHandler jsonHandler,
            MessageIssueHandler issueHandler) {
        this.consumer = consumer;
        this.issueHandler = issueHandler;
        this.jsonHandler = jsonHandler;
    }

    public void onMessage(String content) {
        try {
            consumer.consume(jsonHandler.parseMessage(content));
        } catch (MessageIssueException exception) {
            issueHandler.handle(exception.getRpcMessage(), exception.getIssues());
        }
    }
}
