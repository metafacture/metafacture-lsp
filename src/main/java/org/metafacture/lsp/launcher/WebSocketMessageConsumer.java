package org.metafacture.lsp.launcher;

import java.io.IOException;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageIssueException;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class WebSocketMessageConsumer implements MessageConsumer {
    private MessageJsonHandler jsonHandler;
    private WebSocketSession session;

    public WebSocketMessageConsumer(MessageJsonHandler jsonHandler, WebSocketSession session) {
        this.session = session;
        this.jsonHandler = jsonHandler;
    }

    @Override
    public void consume(Message message) throws MessageIssueException, JsonRpcException {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(jsonHandler.serialize(message)));
            }
        } catch (IOException exception) {
            throw new JsonRpcException(exception);
        }
    }
}
