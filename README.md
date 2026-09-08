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

You may want to modify the `metafacture-lsp.service` file in order to fit your needs. For example, to run the service on another port than the default `8080` add `-Dserver.port=<your-port-number>` to the `ExecStart` option.
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

## VSCode extension

The folder extension/flux contains a adapted copy of https://github.com/metafacture/metafacture-flux/tree/main/org.metafacture.flux.vsc. The project  provides an extension for Visual Studio Code / Codium for `flux` via the language server protocol (LSP). The extension connects to a language server using websockets (`wss:metafacure.org/ls`).

1. Install Visual Studio Code / alternative: VS Codium
2. Install Node.js (including npm)
3. In metafacture-lsp/extension/flux execute:
   `npm install`

To start the extension in development mode, follow A. To create an vsix file to install the extension permanently follow B.

A) Run in dev mode:
1. Open metafacture-lsp/extensions/flux/src/extension.ts in Visual Studio Code / Codium
2. Launch vscode extension by pressing F5 (opens new window of Visual Studio Code)
3. Open new file (file-ending .flux) or open existing flux-file

B) Install vsix file:
1. Install vsce: `npm install -g vsce`
2. In metafacture-lsp/extension/flux execute: `vsce package`
vsce will create a vsix file in the vsc directory which can be used for installation:
3. Open VS Code / Codium
4. Click 'Extensions' section
5. Click menu bar and choose 'Install from VSIX...'
