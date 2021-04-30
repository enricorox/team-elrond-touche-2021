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
package analyzers;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import utils.StopWords;

import java.io.Reader;


/**
 * Analyzer for Task1
 * It uses the Krovetz Stemmer and a stop list
 *
 * @author Nicola Ferro (ferro@dei.unipd.it)
 * @author elrond-group
 * @version 2.0
 * @since 1.0
 */
public class KAnalyzer extends Analyzer {

	/**
	 * Creates a new instance of the analyzer.
	 */
	public KAnalyzer() {
		super();
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {

		final Tokenizer source = new StandardTokenizer();

		TokenStream tokens = new LowerCaseFilter(source);

		tokens = new StopFilter(tokens, StopWords.loadStopWords("99webtools.txt"));

		tokens = new KStemFilter(tokens);

		return new TokenStreamComponents(source, tokens);
	}

	@Override
	protected Reader initReader(String fieldName, Reader reader) {
		return super.initReader(fieldName, reader);
	}

	@Override
	protected TokenStream normalize(String fieldName, TokenStream in) {
		return new LowerCaseFilter(in);
	}

}
