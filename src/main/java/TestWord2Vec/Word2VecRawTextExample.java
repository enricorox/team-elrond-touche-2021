/*******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 * Copyright (c) 2015-2019 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package TestWord2Vec;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;

/**
 * Created by agibsonccc on 10/9/14.
 *
 * Neural net that processes text into wordvectors. See below url for an in-depth explanation.
 * https://deeplearning4j.konduit.ai/language-processing/word2vec
 */
public class Word2VecRawTextExample {

    private static Logger log = LoggerFactory.getLogger(Word2VecRawTextExample.class);

    public static String dataLocalPath;
    public static String wordVectorsPath = "D:/ludon/Download/GoogleNews-vectors-negative300.bin";


    public static void main(String[] args) throws Exception {

        /*dataLocalPath = DownloaderUtility.NLPDATA.Download();
        // Gets Path to Text file
        String filePath = new File(dataLocalPath,"raw_sentences.txt").getAbsolutePath();

        log.info("Load & Vectorize Sentences....");
        // Strip white space before and after for each line
        SentenceIterator iter = new BasicLineIterator(filePath);
        // Split on white spaces in the line to get words
        TokenizerFactory t = new DefaultTokenizerFactory();

        /*
            CommonPreprocessor will apply the following regex to each token: [\d\.:,"'\(\)\[\]|/?!;]+
            So, effectively all numbers, punctuation symbols and some special symbols are stripped off.
            Additionally it forces lower case for all tokens.
         */
        //t.setTokenPreProcessor(new CommonPreprocessor());

        log.info("Building model....");
        /*Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(5)
                .iterations(1)
                .layerSize(100)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

        log.info("Fitting Word2Vec model....");*/
        //vec.fit();

        log.info("Writing word vectors to text file....");

        // Prints out the closest 10 words to "day". An example on what to do with these Word Vectors.

        //WordVectors wordVectors = vec.loadStaticModel(new File(wordVectorsPath));
        File googleModelFile = new File(wordVectorsPath);
        //Method method = WordVectorSerializer.class.getDeclaredMethod("readBinaryModel", File.class, boolean.class , boolean.class);
        //method.setAccessible(true);
        //Word2Vec word2vec = (Word2Vec)method.invoke(null, googleModelFile, false, false);
        final InputStream targetStream =
                new DataInputStream(new FileInputStream(googleModelFile));
        Word2Vec word2vec=WordVectorSerializer.readBinaryModel(targetStream,false,false);
        log.info("Closest Words:");
        Collection<String> lst = word2vec.wordsNearestSum("day", 10);

        log.info("10 Words closest to 'day': {}", lst);


    }
}
