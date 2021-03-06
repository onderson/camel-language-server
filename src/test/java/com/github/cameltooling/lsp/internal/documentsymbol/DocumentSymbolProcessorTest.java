/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.documentsymbol;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.junit.Ignore;
import org.junit.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

public class DocumentSymbolProcessorTest extends AbstractCamelLanguageServerTest {
	
	@Test
	public void testRoutesProvidedAsDocumentSymbol() throws Exception {
		String textTotest =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <route id=\"a route\">\r\n" + 
				"      <from uri=\"direct:cafe\"/>\r\n" + 
				"      <split>\r\n" + 
				"        <method bean=\"orderSplitter\"/>\r\n" + 
				"        <to uri=\"direct:drink\"/>\r\n" + 
				"      </split>\r\n" + 
				"    </route>\r\n" + 
				"\r\n" + 
				"    <route id=\"another Route\">\r\n" + 
				"      <from uri=\"direct:drink\"/>\r\n" + 
				"      <recipientList>\r\n" + 
				"        <method bean=\"drinkRouter\"/>\r\n" + 
				"      </recipientList>\r\n" + 
				"    </route>\n"
				+ "</camelContext>\n";
		List<? extends SymbolInformation> documentSymbols = testRetrieveDocumentSymbol(textTotest, 2);
		SymbolInformation firstRoute = documentSymbols.get(0);
		assertThat(firstRoute.getName()).isEqualTo("a route");
		Position expectedStart = new Position(2, 24/* expecting 4 but seems a bug in Camel*/);
		Position expectedEnd = new Position(8, 12);
		assertThat(firstRoute.getLocation()).isEqualToComparingFieldByFieldRecursively(new Location(DUMMY_URI+".xml", new Range(expectedStart, expectedEnd)));
	}

	@Test
	public void testEmptyCamelContextReturnNoDocumentSymbol() throws Exception {
		String textTotest =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"</camelContext>\n";
		testRetrieveDocumentSymbol(textTotest, 0);
	}
	
	@Test
	public void testRoutesWithoutId() throws Exception {
		String textTotest =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <route id=\"a route\">\r\n" + 
				"      <from uri=\"direct:cafe\"/>\r\n" + 
				"      <split>\r\n" + 
				"        <method bean=\"orderSplitter\"/>\r\n" + 
				"        <to uri=\"direct:drink\"/>\r\n" + 
				"      </split>\r\n" + 
				"    </route>\r\n" + 
				"\r\n" + 
				"    <route id=\"another Route\">\r\n" + 
				"      <from uri=\"direct:drink\"/>\r\n" + 
				"      <recipientList>\r\n" + 
				"        <method bean=\"drinkRouter\"/>\r\n" + 
				"      </recipientList>\r\n" + 
				"    </route>\n" +
				"    <route>\r\n" + 
				"      <from uri=\"direct:drink\"/>\r\n" + 
				"      <recipientList>\r\n" + 
				"        <method bean=\"drinkRouter\"/>\r\n" + 
				"      </recipientList>\r\n" + 
				"    </route>\n"
				+ "</camelContext>\n";
		testRetrieveDocumentSymbol(textTotest, 3);
	}
	
	@Test
	@Ignore("ignore until it is fixed more globally, see https://github.com/camel-tooling/camel-language-server/issues/74")
	public void testRoutesProvidedAsDocumentSymbolWithNamespaceprefix() throws Exception {
		String textTotest =
				"<camel:camelContext id=\"camel\" xmlns:camel=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <camel:route id=\"a route\">\r\n" + 
				"      <camel:from uri=\"direct:cafe\"/>\r\n" + 
				"      <camel:split>\r\n" + 
				"        <camel:method bean=\"orderSplitter\"/>\r\n" + 
				"        <camel:to uri=\"direct:drink\"/>\r\n" + 
				"      </camel:split>\r\n" + 
				"    </camel:route>\r\n" + 
				"\r\n" + 
				"    <camel:route id=\"another Route\">\r\n" + 
				"      <camel:from uri=\"direct:drink\"/>\r\n" + 
				"      <camel:recipientList>\r\n" + 
				"        <camel:method bean=\"drinkRouter\"/>\r\n" + 
				"      </camel:recipientList>\r\n" + 
				"    </camel:route>\n"
				+ "</camel:camelContext>\n";
		testRetrieveDocumentSymbol(textTotest, 2);
	}

	private List<? extends SymbolInformation> testRetrieveDocumentSymbol(String textTotest, int expectedSize) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textTotest);
		CompletableFuture<List<? extends SymbolInformation>> documentSymbolFor = getDocumentSymbolFor(camelLanguageServer);
		List<? extends SymbolInformation> symbolsInformation = documentSymbolFor.get();
		assertThat(symbolsInformation).hasSize(expectedSize);
		return symbolsInformation;
	}

}
