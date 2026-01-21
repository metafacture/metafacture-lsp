package launcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WebSocket Launcher for Metafacture language server.
 */
@SpringBootApplication
public class WebSocketLauncher {
    public static void main(String[] args) {
        SpringApplication.run(WebSocketLauncher.class, args);
    }
}
