import * as vscode from 'vscode';
import WebSocket from 'ws';
import { Disposable } from 'vscode-jsonrpc';

// Import the language client, language client options and server options from VSCode language client.
import { LanguageClient, ServerOptions, MessageTransports } from 'vscode-languageclient/node';

interface IWebSocket extends Disposable {
    send(content: string): void;
    onMessage(cb: (data: any) => void): void;
    onError(cb: (reason: any) => void): void;
    onClose(cb: (code: number, reason: string) => void): void;
}

class WebSocketWrapper implements IWebSocket {
    private ws: WebSocket;

    constructor(ws: WebSocket) {
        this.ws = ws;
    }

    send(content: string): void {
        this.ws.send(content);
    }

    onMessage(cb: (data: any) => void): void {
        this.ws.on('message', (data) => cb(data));
    }

    onError(cb: (reason: any) => void): void {
        this.ws.on('error', (error) => cb(error));
    }

    onClose(cb: (code: number, reason: string) => void): void {
        this.ws.on('close', (code, reason) => cb(code, reason));
    }

    dispose(): void {
        this.ws.close();
    }
}


let client: LanguageClient | undefined;

export async function activate(context: vscode.ExtensionContext) {
    console.log('Activating metafacture-lsp extension...');

    const ws = new WebSocket(`wss://metafacture.org/ls`);
    
    // Debug: WebSocket events
    ws.on('open', () => {
        console.log('[WebSocket] Connection established to test.metafacture.org');
    });
    ws.on('error', (error) => {
        console.error('[WebSocket] Error:', error.message);
    });
    ws.on('close', () => {
        console.log('[WebSocket] Connection closed');
    });

    const serverOptions: ServerOptions = async () => {
        // Wait for WebSocket to be open
        await new Promise<void>((resolve, reject) => {
            ws.on('open', () => resolve());
            ws.on('error', (error) => reject(error));
        });

        console.log('[WebSocket] Creating message reader/writer');

        const socketModule = await import('vscode-ws-jsonrpc/socket');
        const wrappedWs = new WebSocketWrapper(ws);
        const reader = new socketModule.WebSocketMessageReader(wrappedWs);
        const writer = new socketModule.WebSocketMessageWriter(wrappedWs);

        return {
            reader,
            writer,
        } as MessageTransports;
    };
    
    const clientOptions = { 
        documentSelector: [{ scheme: 'file', language: 'flux' }],
        synchronize: {
             // Notify the server about file changes to '.clientrc files contained in the workspace
            fileEvents: vscode.workspace.createFileSystemWatcher('**/.clientrc')
        }
    };

    client = new LanguageClient('metafacture-flux', 'Metafacture Flux Language Server', serverOptions, clientOptions);
    
    // Debug: Client state changes
    client.onDidChangeState((e) => {
        console.log('[LanguageClient] State changed:', e.oldState, '->', e.newState);
    });
    
    await client.start();
    console.log('[LanguageClient] Started successfully');
}

// this method is called when your extension is deactivated
export async function deactivate() { 
    console.log('Deactivating metafacture-lsp extension...');
    if (client) {
        await client.stop();
        console.log('Language client stopped');
    }
}