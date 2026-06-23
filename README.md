# metafacture-lsp

Implementations of the Language Server Protocol for Metafacture languages.

This is a first very simple approach for a language server. It implements autocompletion.
It reads from metafacture-core to autocomplete flux commands.

It's a copy of https://github.com/NipunaMarcus/hellols/tree/websocket-launcher with some modifications in HelloTextDocumentService.java to read the flux commands from metafacture-core.

## How to Run

Make sure you have Java installed in your workspace.

Java: 21 or Above

### Run all tests and other checks

```
./gradlew check
```

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

### Production setup

Initial service setup (required only once):

```
sudo ln -sr build/libs/metafacture-lsp-0.0.1-SNAPSHOT.jar /opt/metafacture-lsp.jar
sudo ln -sr metafacture-lsp.service /etc/systemd/system/metafacture-lsp.service
sudo systemctl daemon-reload
```

For debugging etc. start the application manually:

```
java -Dspring.profiles.active=production -jar ./build/libs/metafacture-lsp-0.0.1-SNAPSHOT.jar
```

Websocket service will be up on ws://localhost:8080/ls.

## Client

To communicate with the server and test autocompletion, run https://github.com/NipunaMarcus/web-editor/tree/websocket-ls

```
git clone https://github.com/NipunaMarcus/web-editor.git -b websocket-ls
cd web-editor
npm run build
npm run dev
```

If `npm run build` fails, try `npm install` instead. Open http://localhost:5173/. Trigger suggestions by typing or with Ctrl+Space.
