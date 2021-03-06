package com.github.cameltooling.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

public abstract class AbstractCamelLanguageServerTest {

	protected static final String AHC_DOCUMENTATION = "To call external HTTP services using Async Http Client.";
	protected static final String DUMMY_URI = "dummyUri";
	private String extensionUsed;
	protected CompletionItem expectedAhcCompletioncompletionItem;
	protected PublishDiagnosticsParams lastPublishedDiagnostics;

	public AbstractCamelLanguageServerTest() {
		super();
		expectedAhcCompletioncompletionItem = new CompletionItem("ahc:httpUri");
		expectedAhcCompletioncompletionItem.setDocumentation(AHC_DOCUMENTATION);
		expectedAhcCompletioncompletionItem.setDeprecated(false);
	}
	
	final class DummyLanguageClient implements LanguageClient {

		@Override
		public void telemetryEvent(Object object) {
		}

		@Override
		public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
			return null;
		}

		@Override
		public void showMessage(MessageParams messageParams) {
		}

		@Override
		public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
			AbstractCamelLanguageServerTest.this.lastPublishedDiagnostics = diagnostics;
		}

		@Override
		public void logMessage(MessageParams message) {
		}
	}

	protected CamelLanguageServer initializeLanguageServer(String text) throws URISyntaxException, InterruptedException, ExecutionException {
		return initializeLanguageServer(text, ".xml");
	}

	protected CamelLanguageServer initializeLanguageServer(String text, String suffixFileName) throws URISyntaxException, InterruptedException, ExecutionException {
		this.extensionUsed = suffixFileName;
		InitializeParams params = new InitializeParams();
		params.setProcessId(new Random().nextInt());
		params.setRootUri(getTestResource("/workspace/").toURI().toString());
		CamelLanguageServer camelLanguageServer = new CamelLanguageServer();
		camelLanguageServer.connect(new DummyLanguageClient());
		CompletableFuture<InitializeResult> initialize = camelLanguageServer.initialize(params);

		assertThat(initialize).isCompleted();
		assertThat(initialize.get().getCapabilities().getCompletionProvider().getResolveProvider()).isTrue();

		camelLanguageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(createTestTextDocument(text, suffixFileName)));

		return camelLanguageServer;
	}

	protected CamelLanguageServer initializeLanguageServer(FileInputStream stream, String suffixFileName) {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream))) {
            return initializeLanguageServer(buffer.lines().collect(Collectors.joining("\n")), suffixFileName);
        } catch (ExecutionException | InterruptedException | URISyntaxException | IOException ex) {
        	return null;
        }
	}
	
	private TextDocumentItem createTestTextDocument(String text, String suffixFileName) {
		return new TextDocumentItem(DUMMY_URI + suffixFileName, CamelLanguageServer.LANGUAGE_ID, 0, text);
	}

	protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletionFor(CamelLanguageServer camelLanguageServer, Position position) {
		TextDocumentService textDocumentService = camelLanguageServer.getTextDocumentService();
		
		CompletionParams completionParams = new CompletionParams(new TextDocumentIdentifier(DUMMY_URI+extensionUsed), position);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = textDocumentService.completion(completionParams);
		return completions;
	}
	
	protected CompletableFuture<List<? extends SymbolInformation>> getDocumentSymbolFor(CamelLanguageServer camelLanguageServer) {
		TextDocumentService textDocumentService = camelLanguageServer.getTextDocumentService();
		DocumentSymbolParams params = new DocumentSymbolParams(new TextDocumentIdentifier(DUMMY_URI+extensionUsed));
		return textDocumentService.documentSymbol(params);
	}

	public File getTestResource(String name) throws URISyntaxException {
		return Paths.get(CamelLanguageServerTest.class.getResource(name).toURI()).toFile();
	}

}