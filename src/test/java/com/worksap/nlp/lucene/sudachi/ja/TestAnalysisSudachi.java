/*
 *  Copyright (c) 2017 Works Applications Co., Ltd.
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

package com.worksap.nlp.lucene.sudachi.ja;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;

import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.test.ESTestCase;
import org.junit.BeforeClass;
import org.junit.Test;

import com.worksap.nlp.elasticsearch.sudachi.index.SudachiTokenizerFactory;
import com.worksap.nlp.elasticsearch.sudachi.plugin.AnalysisSudachiPlugin;

public class TestAnalysisSudachi extends ESTestCase {
    private static final String RESOURCE_NAME_SUDACHI_ANALYSIS_JSON = "/com/worksap/nlp/lucene/sudachi/ja/sudachiAnalysis.json";

    private static Path home;

    @Test
    public void testSearchModeTokenize() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("sudachi_tokenizer");
        assertThat(tokenizerFactory, instanceOf(SudachiTokenizerFactory.class));
        
        Tokenizer tokenizer = tokenizerFactory.create();
        tokenizer.setReader(new StringReader("東京都へ行った。"));
        List<String> actual = getAsList(tokenizer);
        List<String> expected = Arrays.asList("東京都", "東京", "都", "へ", "行く", "た");
        assertThat(actual, is(expected));
    }
    
    @Test
    public void testSearchModeTokenizeReadingFormUseRomajiTrue() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("sudachi_tokenizer");
        
        Tokenizer tokenizer = tokenizerFactory.create();
        tokenizer.setReader(new StringReader("東京都へ行った。"));
        TokenStream stream = new SudachiReadingFormFilter(tokenizer, true);
        List<String> actual = getAsList(stream);
        List<String> expected = Arrays.asList("tōkyōto", "tōkyō", "to", "e", "i", "ta");
        assertThat(actual, is(expected));
    }
    
    @Test
    public void testSearchModeTokenizeReadingFormUseRomajiFalse() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("sudachi_tokenizer");
        
        Tokenizer tokenizer = tokenizerFactory.create();
        tokenizer.setReader(new StringReader("東京都へ行った。"));
        TokenStream stream = new SudachiReadingFormFilter(tokenizer, false);
        List<String> actual = getAsList(stream);
        List<String> expected = Arrays.asList("トウキョウト", "トウキョウ", "ト", "エ", "イッ", "タ");
        assertThat(actual, is(expected));
    }
    
    @Test
    public void testSearchModeTokenizeBaseForm() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("sudachi_tokenizer");
        
        Tokenizer tokenizer = tokenizerFactory.create();
        tokenizer.setReader(new StringReader("差し出ださ"));
        TokenStream stream = new SudachiBaseFormFilter(tokenizer);
        List<String> actual = getAsList(stream);
        List<String> expected = Arrays.asList("差し出だす", "差す", "出だす");
        assertThat(actual, is(expected));
    }
    
    @Test
    public void testSearchModeTokenizeStopWordDefault() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("sudachi_tokenizer");
        
        Tokenizer tokenizer = tokenizerFactory.create();
        tokenizer.setReader(new StringReader("東京都へ行った。"));
        TokenStream stream = new StopFilter(tokenizer, SudachiAnalyzer.getDefaultStopSet());
        List<String> actual = getAsList(stream);
        List<String> expected = Arrays.asList("東京都", "東京", "都", "行く");
        assertThat(actual, is(expected));
    }
    
    @Test
    public void testSearchModeTokenizePartOfSpeechDefault() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("sudachi_tokenizer");
        
        Tokenizer tokenizer = tokenizerFactory.create();
        tokenizer.setReader(new StringReader("東京都へ行った。"));
        TokenStream stream = new SudachiPartOfSpeechStopFilter(tokenizer, SudachiAnalyzer.getDefaultStopTags());
        List<String> actual = getAsList(stream);
        List<String> expected = Arrays.asList("東京都", "東京", "都", "行く");
        assertThat(actual, is(expected));
    }

    @Test
    public void testOffset() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("sudachi_tokenizer");

        Tokenizer tokenizer = tokenizerFactory.create();
        tokenizer.setReader(new StringReader("東京都へ行った。"));
        List<Integer> actual = getAsOffsetList(tokenizer);
        List<Integer> expected = Arrays.asList(0, 0, 2, 3, 4, 6);
        assertThat(actual, is(expected));
    }

    @Test
    public void testOffsetWithLongInput() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("sudachi_tokenizer");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            builder.append("東京都へ行った。");
        }
        Tokenizer tokenizer = tokenizerFactory.create();
        tokenizer.setReader(new StringReader(builder.toString()));
        int prevOffset = 0;
        for (int offset : getAsOffsetList(tokenizer)) {
            assertTrue(offset >= prevOffset);
            prevOffset = offset;
        }
    }

    private static List<String> getAsList(TokenStream stream) throws IOException {
        stream.reset();
        CharTermAttribute termAttr = stream.getAttribute(CharTermAttribute.class);
        List<String> result = new ArrayList<>();
        while (stream.incrementToken()) {
            result.add(termAttr.toString());
        }
        return result;
    }

    private static List<Integer> getAsOffsetList(TokenStream stream) throws IOException {
        stream.reset();
        OffsetAttribute offsetAttr = stream.getAttribute(OffsetAttribute.class);
        List<Integer> result = new ArrayList<>();
        while (stream.incrementToken()) {
            result.add(offsetAttr.startOffset());
        }
        return result;
    }

    private static TestAnalysis createTestAnalysis() throws IOException {
        Settings settings;
        try (InputStream input = AnalysisSudachiPlugin.class.getResourceAsStream(RESOURCE_NAME_SUDACHI_ANALYSIS_JSON)) {
            settings = Settings.builder()
                .loadFromStream(RESOURCE_NAME_SUDACHI_ANALYSIS_JSON, input, false)
                .put("index.analysis.tokenizer.sudachi_tokenizer.resources_path", home)
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .build();
        }

        Settings nodeSettings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), home).build();
        return createTestAnalysis(new Index("test", "_na_"), nodeSettings, settings, new AnalysisSudachiPlugin());
    }

    @BeforeClass
    public static void initializeTest() throws IOException {
        home = createTempDir();
    }
}
