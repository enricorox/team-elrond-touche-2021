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
package analysis;


import opennlp.tools.util.Span;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizer;
import org.apache.lucene.analysis.opennlp.tools.NLPNERTaggerOp;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.util.AttributeSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A filter that relies on the <a href="http://opennlp.apache.org/" target="_blank">Apache OpenNLP</a>
 * Named Entity Recognizer (NER) to detect different types of entities.
 *
 * @author Nicola Ferro (ferro@dei.unipd.it)
 * @version 1.0
 * @since 1.0
 */
public final class OpenNLPNERFilter extends TokenFilter {

	/**
	 * The wrapper around the Apache OpenNLP NER tagger
	 */
	private final NLPNERTaggerOp nerTaggerOp;

	/**
	 * The classes of relevant {@code Attribute}s of a token
	 */
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
	private final FlagsAttribute flagsAtt = addAttribute(FlagsAttribute.class);

	/**
	 * The list of the actual attribute values for the tokens in the current sentence
	 */
	private final List<AttributeSource> sentenceTokens = new ArrayList<>();
	private Iterator<AttributeSource> iterator = sentenceTokens.iterator();

	/**
	 * Indicates whether the are more tokens in the stream
	 */
	private boolean moreTokensAvailable = true;

	/**
	 * Creates a new Named Entity Recognizer (NER) filter.
	 *
	 * @param input       the source of tokens for this filter.
	 * @param nerTaggerOp the NER tagger to use.
	 */
	public OpenNLPNERFilter(TokenStream input, NLPNERTaggerOp nerTaggerOp) {
		super(input);

		this.nerTaggerOp = nerTaggerOp;

		// Forget all adaptive data collected during previous calls
		this.nerTaggerOp.reset();
	}

	@Override
	public final boolean incrementToken() throws IOException {

		// if there is a token, copy it back to the stream
		if (iterator.hasNext()) {
			iterator.next().copyTo(this);
			return true;
		}

		// we have already reached the end of the stream before
		if (!moreTokensAvailable) {
			return false;
		}

		// try to process the next sentence
		if (!nextSentence()) {
			return false;
		}

		// now there should be a token but double check, copy it back to the stream
		if (iterator.hasNext()) {
			iterator.next().copyTo(this);
			return true;
		}

		// we should never get here, so be pessimistic and return false
		return false;
	}

	/**
	 * Process the next sentence as identified by the Apache OpenNLP sentence detector.
	 *
	 * @return {@code true} if there is one more sentence; {@code false} otherwise.
	 *
	 * @throws IOException if something goes wrong while processing the tokens.
	 */
	private boolean nextSentence() throws IOException {

		// empty any previous sentence
		sentenceTokens.clear();
		iterator = sentenceTokens.iterator();

		// the attributes for the tokens in the current sentence
		final List<AttributeSource> localAttrs = new ArrayList<>();

		// the list of terms in the sentence
		final List<String> termList = new ArrayList<>();

		boolean endOfSentence = false;

		// advance until  we reach either the end of a sentence or the end of the stream
		while (!endOfSentence && (moreTokensAvailable = input.incrementToken())) {

			// get the term from the current token
			termList.add(termAtt.toString());

			// check whether the current token marks the end of the sentence
			endOfSentence = 0 != (flagsAtt.getFlags() & OpenNLPTokenizer.EOS_FLAG_BIT);

			// copy all the attributes for the current token
			localAttrs.add(input.cloneAttributes());
		}

		// there was no next sentence
		if (localAttrs.isEmpty()) {
			return false;
		}

		// the identified entities, if any
		Span[] spans = null;
		int spanCount = 0;

		// Apache OpenNLP NER Tagger is not thread-safe
		synchronized (nerTaggerOp) {
			// recognize entities
			spans = nerTaggerOp.getNames(termList.toArray(new String[termList.size()]));
		}

		// go through each local token:
		// 1) if it is (part of) an entity, merge it with the other tokens in the same entity and add it back to the stream
		// 2) if it is not (part of) an entity add it back to the stream
		for (int i = 0, n = localAttrs.size(); i < n; ) {

			// the token is (part of) an entity
			if (spanCount < spans.length && spans[spanCount].getStart() == i) {

				// get the first token in the entity
				AttributeSource as = localAttrs.get(i++);

				StringBuilder tmp = new StringBuilder(as.getAttribute(CharTermAttribute.class).toString());
				String entityType = spans[spanCount].getType();
				int spannedPositions = as.getAttribute(PositionIncrementAttribute.class).getPositionIncrement();
				int startOffset = as.getAttribute(OffsetAttribute.class).startOffset();
				int flags = as.getAttribute(FlagsAttribute.class).getFlags();

				// the entity spans multiple tokens
				while (i < spans[spanCount].getEnd()) {

					// get the next token in the entity
					as = localAttrs.get(i++);

					// append the term of the token
					tmp.append(" ").append(as.getAttribute(CharTermAttribute.class).toString());

					// increase the count of the spanned tokens
					spannedPositions += as.getAttribute(PositionIncrementAttribute.class).getPositionIncrement();

					// cumulate all the flags on the tokens spanned by the entity
					// not sure if makes fully sense
					flags = flags | as.getAttribute(FlagsAttribute.class).getFlags();
				}

				int endOffset = as.getAttribute(OffsetAttribute.class).endOffset();

				// create a new token for the entity and add it back to the stream
				as.clearAttributes();
				as.addAttribute(CharTermAttribute.class).append(tmp);
				as.addAttribute(PositionIncrementAttribute.class).setPositionIncrement(1);
				as.addAttribute(PositionLengthAttribute.class).setPositionLength(spannedPositions);
				as.addAttribute(OffsetAttribute.class).setOffset(startOffset, endOffset);
				as.addAttribute(TypeAttribute.class).setType(entityType);
				as.addAttribute(KeywordAttribute.class).setKeyword(true);
				as.addAttribute(FlagsAttribute.class).setFlags(flags);

				sentenceTokens.add(as);

				// advance to the next span
				spanCount++;

			} else {
				// get the next token in the current sentence and add it back to the stream
				sentenceTokens.add(localAttrs.get(i++));
			}

		}

		// set the new iterator
		iterator = sentenceTokens.iterator();

		return true;
	}

	@Override
	public void reset() throws IOException {

		// Apache OpenNLP NER Tagger is not thread-safe
		synchronized (nerTaggerOp) {
			super.reset();
			moreTokensAvailable = true;
			sentenceTokens.clear();
			iterator = sentenceTokens.iterator();

			// Forget all adaptive data collected during previous calls
			nerTaggerOp.reset();
		}
	}

}



