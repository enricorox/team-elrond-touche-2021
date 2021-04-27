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
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.IOException;
import java.io.Reader;

import static Analizer.AnalyzerUtil.consumeTokenStream;
//import static Analizer.AnalyzerUtil.loadStopList;
import static Analizer.StopWords.loadStopWords;

/**
 * Introductory example on how to use write your own {@link Analyzer} by using different {@link Tokenizer}s and {@link
 * org.apache.lucene.analysis.TokenFilter}s.
 *
 * @author Nicola Ferro (ferro@dei.unipd.it)
 * @version 1.0
 * @since 1.0
 */
public class HelloAnalyzer extends Analyzer {

	/**
	 * Creates a new instance of the analyzer.
	 */
	public HelloAnalyzer() {
		super();
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {

		//final Tokenizer source = new WhitespaceTokenizer();
		//final Tokenizer source = new LetterTokenizer();
		final Tokenizer source = new StandardTokenizer();

		TokenStream tokens = new LowerCaseFilter(source);

		//tokens = new LengthFilter(tokens, 4, 10);
		//tokens = new EnglishPossessiveFilter(tokens);
		tokens = new StopFilter(tokens, StopWords.loadStopWords("smart.txt"));

		//tokens = new EnglishMinimalStemFilter(tokens);
		//tokens = new PorterStemFilter(tokens);
		//tokens = new KStemFilter(tokens);
		tokens = new LovinsStemFilter(tokens);

		//tokens = new NGramTokenFilter(tokens, 3);

		//tokens = new ShingleFilter(tokens, 2);

		return new TokenStreamComponents(source, tokens);
	}

	@Override
	protected Reader initReader(String fieldName, Reader reader) {
		// return new HTMLStripCharFilter(reader);

		return super.initReader(fieldName, reader);
	}

	@Override
	protected TokenStream normalize(String fieldName, TokenStream in) {
		return new LowerCaseFilter(in);
	}

	/**
	 * Main method of the class.
	 *
	 * @param args command line arguments.
	 *
	 * @throws IOException if something goes wrong while processing the text.
	 */
	public static void main(String[] args) throws IOException {

		// text to analyze
		final String text = "I now live in Rome where I met my wife Alice back in 2010 during a beautiful afternoon. " + "Occasionally, I fly to New York to visit the United Nations where I would like to work. The last " + "time I was there in March 2019, the flight was very inconvenient, leaving at 4:00 am, and expensive," + " over 1,500 dollars.";

		// use the analyzer to process the text and print diagnostic information about each token
		consumeTokenStream(new HelloAnalyzer(), text);


	}

}
