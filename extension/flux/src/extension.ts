import * as vscode from 'vscode';
import WebSocket from 'ws';
import { LanguageClient, ServerOptions, MessageTransports } from 'vscode-languageclient/node';
import { ErrorAction, CloseAction } from 'vscode-languageclient';

let client: LanguageClient | undefined;

function createWebSocketConnection(): Promise<MessageTransports> {
    return new Promise((resolve, reject) => {
        const ws = new WebSocket('wss://metafacture.org/ls');

        const timeout = setTimeout(() => {
            cleanup();
            reject(new Error('Connection timeout'));
        }, 30000);

        const cleanup = () => {
            clearTimeout(timeout);
            ws.removeAllListeners('open');
            ws.removeAllListeners('error');
            ws.removeAllListeners('close');
        };

        const handleOpen = () => {
            cleanup();
            console.log('[WebSocket] connected to wss://metafacture.org/ls');
            
            import('vscode-ws-jsonrpc').then(({ WebSocketMessageReader, WebSocketMessageWriter }) => {
                // Create a wrapper socket that suppresses errors on close code 1006
                const onCloseCallbacks: Array<(code: number, reason: string) => void> = [];
                
                const wrappedSocket = {
                    send: (content: string) => ws.send(content),
                    onMessage: (cb: (data: string) => void) => ws.on('message', cb),
                    onError: (cb: (error: any) => void) => ws.on('error', cb),
                    onClose: (cb: (code: number, reason: string) => void) => {
                        onCloseCallbacks.push(cb);
                        return { dispose: () => {
                            const idx = onCloseCallbacks.indexOf(cb);
                            if (idx > -1) onCloseCallbacks.splice(idx, 1);
                        }};
                    },
                    dispose: () => ws.close()
                };
                
                // Patch the original socket's onClose to suppress errors on code 1006
                ws.on('close', (code: number, reason: unknown) => {
                    const reasonText = typeof reason === 'string' ? reason : Buffer.isBuffer(reason) ? reason.toString('utf8') : String(reason);
                    const reportedCode = code === 1006 ? 1000 : code;
                    if (reportedCode === 1000) {
                        console.log(`[WebSocket] Closed normally${code === 1006 ? ' (mapped from 1006)' : ''}`);
                    } else {
                        console.log(`[WebSocket] Closed with code ${code}, reporting ${reportedCode}`);
                    }
                    onCloseCallbacks.forEach(cb => cb(reportedCode, reasonText));
                });
                
                const reader = new WebSocketMessageReader(wrappedSocket as any);
                const writer = new WebSocketMessageWriter(wrappedSocket as any);
                resolve({ reader, writer });
            }).catch(reject);
        };

        const handleError = (error: Error) => {
            cleanup();
            console.error('[WebSocket] connection error', error);
            reject(error);
        };

        const handleClose = (code: number, reason: Buffer) => {
            cleanup();
            console.warn('[WebSocket] closed before open', { code, reason: reason?.toString() });
            if (ws.readyState !== WebSocket.OPEN) {
                reject(new Error(`WebSocket closed before open: ${code} ${reason?.toString()}`));
            }
        };

        ws.once('open', handleOpen);
        ws.once('error', handleError);
        ws.once('close', handleClose);
    });
}

export async function activate(context: vscode.ExtensionContext) {
    console.log('Activating metafacture-lsp extension...');

    const serverOptions: ServerOptions = () => createWebSocketConnection();
    
    const clientOptions = { 
        documentSelector: [{ scheme: 'file', language: 'flux' }],
        synchronize: {
             // Notify the server about file changes to '.clientrc files contained in the workspace
            fileEvents: vscode.workspace.createFileSystemWatcher('**/.clientrc')
        },
        outputChannelName: 'metafacture-flux',
        // Handle errors/close to avoid noisy automatic restarts that cause shutdown on disposed connections
        errorHandler: {
            error: (error, message, count) => {
                return { action: ErrorAction.Continue };
            },
            closed: () => {
                // Allow the client to restart on socket close (transient server/network errors)
                return { action: CloseAction.Restart };
            }
        }
    };

    // Limit automatic restart attempts to avoid rapid infinite restart loops
    (clientOptions as any).connectionOptions = { maxRestartCount: 5 };

    client = new LanguageClient('metafacture-flux', 'Metafacture Flux Language Server', serverOptions, clientOptions);
    
    await client.start();
    console.log('[LanguageClient] Started successfully');
}

// this method is called when your extension is deactivated
export async function deactivate() { 
    console.log('Deactivating metafacture-lsp extension...');
    if (client) {
        try {
            await client.stop();
            console.log('Language client stopped successfully');
        } catch (error: any) {
            // Ignore "Connection is disposed" errors during shutdown
            if (error && (error.message?.includes('disposed') || error.message?.includes('Connection'))) {
                console.log('Language client stop skipped (connection already disposed)');
            } else {
                console.error('Error stopping language client:', error);
            }
        }
    }
}