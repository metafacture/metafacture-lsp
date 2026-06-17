# metafacture-lsp

Implementations of the Language Server Protocol for Metafacture languages.

This is a first very simple approach for a language server. It implements autocompletion.
It reads from metafacture-core to autocomplete flux commands.

It's a copy of https://github.com/NipunaMarcus/hellols/tree/websocket-launcher with some modifications in HelloTextDocumentService.java to read the flux commands from metafacture-core.

## How to Run

Make sure you have Java installed in your workspace.

Java: 21 or Above

### Start in development mode

In one terminal run

```
./gradlew compileJava -t
```

and in another terminal run

```
./gradlew bootRun
```

Websocket service will be up on ws://localhost:8080/ls.

### Start in production mode

```
./gradlew clean build
java -jar ./build/libs/metafacture-lsp-0.0.1-SNAPSHOT.jar
```

Websocket service will be up on ws://localhost:8080/ls.

**Client**

To communicate with the server and test autocompletion, follow the instructions on https://github.com/NipunaMarcus/web-editor/tree/websocket-ls to start a simple web-editor.

Note: You need to clone or switch to the `websocket-ls` branch. If `npm run build` fails, try `npm install` instead. Trigger suggestions by typing or with Ctrl+Space.
