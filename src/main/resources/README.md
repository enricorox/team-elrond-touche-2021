# Stop Lists

This folder contains various stop lists for the English language.

Some of these stop lists and others are available at [Igor Brigadir's page](https://github.com/igorbrigadir/stopwords).

### ATIRE stop list

- _file_: `atire.txt`
- _terms_: 988
- _url_: https://github.com/andrewtrotman/ATIRE/blob/master/source/stop_word.c

### Glasgow stop list

- _file_: `glasgow.txt`
- _terms_: 319
- _url_: http://ir.dcs.gla.ac.uk/resources/linguistic_utils/stop_words

### Indri stop list

- _file_: `indri.txt`
- _terms_: 418
- _url_: https://sourceforge.net/p/lemur/code/HEAD/tree/indri/trunk/site-search/stopwords

### Lucene stop list

- _file_: `lucene.txt`
- _terms_: 33
- _url_: https://github.com/apache/lucene/blob/main/lucene/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java

### LingPipe stop list

- _file_: `lingpipe.txt`
- _terms_: 76
- _url_: http://alias-i.com/lingpipe/docs/api/com/aliasi/tokenizer/EnglishStopTokenizerFactory.html

### Okapi stop list

- _file_: `okapi.txt`
- _terms_: 222
- _url_: http://www.staff.city.ac.uk/~andym/OKAPI-PACK/appendix-d.html

### SMART stop list

- _file_: `smart.txt`
- _terms_: 571
- _url_: http://ftp.sunet.se/mirror/archive/ftp.sunet.se/pub/databases/full-text/smart/english.stop

### Snowball stop list

- _file_: `snowball.txt`
- _terms_: 174
- _url_: https://github.com/snowballstem/snowball-website/blob/master/algorithms/english/stop.txt

### Terrier stop list

- _file_: `terrier.txt`
- _terms_: 733
- _url_: http://terrier.org/docs/current/javadoc/org/terrier/terms/Stopwords.html

### Zettair stop list

- _file_: `zettair.txt`
- _terms_: 469
- _url_: http://www.seg.rmit.edu.au/zettair/download.html

# Apache OpenNLP Models

This folder contains various [Apache OpenNLP](http://opennlp.apache.org/) models for the English language.

These models are available at http://opennlp.sourceforge.net/models-1.5/.

### Sentence Detector

- _file_: `en-sent.bin`
- _description_: sentence detection is about identifying the start and the end of a sentence. For example, sentence
  detection may be quite challenging because of the ambiguous nature of the period character. A period usually denotes
  the end of a sentence but can also appear in an email address, an abbreviation, a decimal, and a lot of other places.

### Tokenizer

- _file_: `en-token.bin`
- _description_: the goal of tokenization is to divide a sentence into smaller parts called tokens. Usually, these
  tokens are words, numbers or punctuation marks.

### Part-of-Speech (POS) Tagger

- _file_: `en-pos-maxent.bin`
- _description_: a part-of-speech identifies the type of a word based on the token itself and the context of the token.
  The Apache OpenNLP PoS tagger uses the same pos tags as
  the [Penn Treebank Project](https://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html):
    + `CC`    Coordinating conjunction
    + `CD`    Cardinal number
    + `DT`    Determiner
    + `EX`    Existential there
    + `FW`    Foreign word
    + `IN`    Preposition or subordinating conjunction
    + `JJ`    Adjective
    + `JJR`    Adjective, comparative
    + `JJS`    Adjective, superlative
    + `LS`    List item marker
    + `MD`    Modal
    + `NN`    Noun, singular or mass
    + `NNS`    Noun, plural
    + `NNP`    Proper noun, singular
    + `NNPS`    Proper noun, plural
    + `PDT`    Predeterminer
    + `POS`    Possessive ending
    + `PRP`    Personal pronoun
    + `PRP$`    Possessive pronoun
    + `RB`    Adverb
    + `RBR`    Adverb, comparative
    + `RBS`    Adverb, superlative
    + `RP`    Particle
    + `SYM`    Symbol
    + `TO`    to
    + `UH`    Interjection
    + `VB`    Verb, base form
    + `VBD`    Verb, past tense
    + `VBG`    Verb, gerund or present participle
    + `VBN`    Verb, past participle
    + `VBP`    Verb, non-3rd person singular present
    + `VBZ`    Verb, 3rd person singular present
    + `WDT`    Wh-determiner
    + `WP`    Wh-pronoun
    + `WP$`    Possessive wh-pronoun
    + `WRB`    Wh-adverb

### Named Entiry Recognition (NER)

- _file_: `en-ner-date.bin`, `en-ner-location.bin`, `en-ner-money.bin`, `en-ner-organization.bin`,
  `en-ner-person.bin`, `en-ner-time.bin`
- _description_: a NER tagger finds named entities like people, locations, organizations and other named things in a
  given text.

### Lemmatizer

- _file_: `en-lemmatizer.bin`
- _description_: a lemmatizer returns, for a given word form (token) and Part of Speech tag, the dictionary form of a
  word, which is usually referred to as its lemma. A token could ambiguously be derived from several basic forms or
  dictionary words which is why the postag of the word is required to find the lemma.

  The file `en-lemmatizer.bin` has been created by directly training the Apache OpenNLP lemmatizer using the dictionary
  available at
  
  https://raw.githubusercontent.com/richardwilly98/elasticsearch-opennlp-auto-tagging/master/src/main/resources/models/en-lemmatizer.dict
  
  and executing

  ```opennlp LemmatizerTrainerME -model en-lemmatizer.bin -lang en -data en-lemmatizer.dict -encoding UTF-8```
  
