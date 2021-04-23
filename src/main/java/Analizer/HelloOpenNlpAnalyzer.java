/*
 * Copyright 2021 University of Padua, Italy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package Analizer;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizer;

import java.io.IOException;

import static Analizer.AnalyzerUtil.*;

/**
 * Introductory example on how to use write your own {@link Analyzer} by relying on the
 * <a href="http://opennlp.apache.org/" target="_blank">Apache OpenNLP</a> library to conduct a more advanced analysis
 * of the text.
 *
 * @author Nicola Ferro (ferro@dei.unipd.it)
 * @version 1.0
 * @since 1.0
 */
public class HelloOpenNlpAnalyzer extends Analyzer {

	/**
	 * Creates a new instance of the analyzer.
	 */
	public HelloOpenNlpAnalyzer() {
		super();
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {

		Tokenizer source;
		try {
			source = new OpenNLPTokenizer(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY,
					loadSentenceDetectorModel("en-sent.bin"), loadTokenizerModel("en-token.bin"));
		} catch (IOException e) {
			// The OpenNLPTokenizer seems to have a "wrong" signature declaring to throw an IOException which actually
			// is never thrown. This forces us to wrap everything with try-catch.

			throw new IllegalStateException(
					String.format("Unable to create the OpenNLPTokenizer: %s. This should never happen: surprised :-o",
							e.getMessage()), e);
		}

		TokenStream tokens = new OpenNLPPOSFilter(source, loadPosTaggerModel("en-pos-maxent.bin"));

		tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-location.bin"));

		tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-person.bin"));

		tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-organization.bin"));

		tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-money.bin"));

		tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-date.bin"));

		tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-time.bin"));

		// tokens = new OpenNLPLemmatizerFilter(tokens, loadLemmatizerModel("en-lemmatizer.bin"));

		// Unfortunately the TypeAttribute is not stored in the index, so we need to work around this by adding the type
		// as a synonym token, which is also convenient at search time - see e.g.
		// https://fabian-kostadinov.github.io/2018/10/01/introduction-to-lucene-opennlp-part2/]
		//tokens = new TypeAsSynonymFilter(tokens, "<nlp>");

		return new TokenStreamComponents(source, tokens);
	}

	/**
	 * Main method of the class.
	 *
	 * @param args command line arguments.  path to the run file.
	 *
	 * @throws IOException if something goes wrong while indexing and searching.
	 */
	public static void main(String[] args) throws IOException {

		// text to analyze
		final String text = "I now live in Rome where I met my wife Alice back in 2010 during a beautiful afternoon. " + "Occasionally, I fly to New York to visit the United Nations where I would like to work. The last " + "time I was there in March 2019, the flight was very inconvenient, leaving at 4:00 am, and expensive," + " over 1,500 dollars.";

		// use the analyzer to process the text and print diagnostic information about each token
		consumeTokenStream(new HelloOpenNlpAnalyzer(), text);

	}

}
