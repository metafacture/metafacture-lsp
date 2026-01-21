package launcher;

import langserver.MetafactureLanguageServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

public class LanguageServerWebSocketHandler extends TextWebSocketHandler {
    private MetafactureLanguageServer languageServer;
    private WebSocketMessageHandler messageHandler;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println(message.toString());
        System.out.println(session.getAttributes());
        if (messageHandler != null) {
            messageHandler.onMessage(message.getPayload());
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("connection established. sessionId: " + session.getId() + ", Client: " + session.getRemoteAddress());
        try{
            languageServer = new MetafactureLanguageServer();
            messageHandler = new WebSocketMessageHandler();
            WebSocketLauncherBuilder<LanguageClient> builder = new WebSocketLauncherBuilder<>();
            builder
                    .setSession(session)
                    .setMessageHandler(messageHandler)
                    .setLocalService(languageServer)
                    .setRemoteInterface(LanguageClient.class);
            Launcher<LanguageClient> languageClientLauncher = builder.create();
            languageServer.connect(languageClientLauncher.getRemoteProxy());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.out.println("Shutdown language server due to an error.");
        languageServer.shutdown();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("Shutting down language server.");
        languageServer.shutdown();
    }
}
