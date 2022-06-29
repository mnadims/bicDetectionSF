From 4ff0b2f82c6b87266d9bab90cb273dab39bd6dda Mon Sep 17 00:00:00 2001
From: Christopher John Male <chrism@apache.org>
Date: Sun, 25 Sep 2011 05:10:25 +0000
Subject: [PATCH] LUCENE-3396: Collapsing Analyzer and ReusableAnalyzerBase
 together, mandating use of TokenStreamComponents

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1175297 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   4 +
 lucene/MIGRATE.txt                            |   5 +
 .../search/highlight/HighlighterTest.java     |   2 +-
 .../highlight/OffsetLimitTokenFilterTest.java |   2 +-
 .../search/highlight/TokenSourcesTest.java    |   2 +-
 .../vectorhighlight/AbstractTestCase.java     |   2 +-
 .../vectorhighlight/IndexTimeSynonymTest.java |   2 +-
 .../org/apache/lucene/analysis/Analyzer.java  | 357 ++++++++++++++----
 .../apache/lucene/analysis/Analyzer.java.old  | 155 ++++++++
 .../lucene/analysis/AnalyzerWrapper.java      |  89 +++++
 .../lucene/analysis/ReusableAnalyzerBase.java | 308 ---------------
 .../apache/lucene/analysis/MockAnalyzer.java  |   2 +-
 .../lucene/analysis/MockPayloadAnalyzer.java  |   2 +-
 .../org/apache/lucene/TestAssertions.java     |   9 +-
 .../lucene/index/TestDocumentWriter.java      |   4 +-
 .../apache/lucene/index/TestIndexWriter.java  |   2 +-
 .../lucene/index/TestIndexWriterCommit.java   |   4 +-
 .../lucene/index/TestIndexWriterDelete.java   |   2 +-
 .../index/TestIndexWriterExceptions.java      |   8 +-
 .../lucene/index/TestLazyProxSkipping.java    |   2 +-
 .../lucene/index/TestMultiLevelSkipList.java  |   2 +-
 .../org/apache/lucene/index/TestPayloads.java |   2 +-
 .../index/TestSameTokenSamePosition.java      |   4 +-
 .../lucene/index/TestTermVectorsReader.java   |   2 +-
 .../apache/lucene/index/TestTermdocPerf.java  |   3 +-
 .../lucene/search/TestMultiPhraseQuery.java   |   3 +-
 .../apache/lucene/search/TestPhraseQuery.java |   2 +-
 .../lucene/search/TestPositionIncrement.java  |   2 +-
 .../lucene/search/TestTermRangeQuery.java     |   2 +-
 .../lucene/search/payloads/PayloadHelper.java |   2 +-
 .../search/payloads/TestPayloadNearQuery.java |   2 +-
 .../search/payloads/TestPayloadTermQuery.java |   2 +-
 .../lucene/search/spans/TestBasics.java       |   2 +-
 .../lucene/search/spans/TestPayloadSpans.java |   4 +-
 .../lucene/analysis/ar/ArabicAnalyzer.java    |   4 +-
 .../lucene/analysis/bg/BulgarianAnalyzer.java |   4 +-
 .../lucene/analysis/br/BrazilianAnalyzer.java |   4 +-
 .../lucene/analysis/ca/CatalanAnalyzer.java   |   4 +-
 .../lucene/analysis/cn/ChineseAnalyzer.java   |   7 +-
 .../lucene/analysis/core/KeywordAnalyzer.java |   4 +-
 .../lucene/analysis/core/SimpleAnalyzer.java  |   3 +-
 .../lucene/analysis/core/StopAnalyzer.java    |   4 +-
 .../analysis/core/WhitespaceAnalyzer.java     |   4 +-
 .../lucene/analysis/cz/CzechAnalyzer.java     |   7 +-
 .../lucene/analysis/da/DanishAnalyzer.java    |   4 +-
 .../lucene/analysis/de/GermanAnalyzer.java    |   4 +-
 .../lucene/analysis/el/GreekAnalyzer.java     |   4 +-
 .../lucene/analysis/en/EnglishAnalyzer.java   |   4 +-
 .../lucene/analysis/es/SpanishAnalyzer.java   |   4 +-
 .../lucene/analysis/eu/BasqueAnalyzer.java    |   4 +-
 .../lucene/analysis/fa/PersianAnalyzer.java   |   4 +-
 .../lucene/analysis/fi/FinnishAnalyzer.java   |   4 +-
 .../lucene/analysis/fr/FrenchAnalyzer.java    |   4 +-
 .../lucene/analysis/gl/GalicianAnalyzer.java  |   4 +-
 .../lucene/analysis/hi/HindiAnalyzer.java     |   4 +-
 .../lucene/analysis/hu/HungarianAnalyzer.java |   4 +-
 .../lucene/analysis/hy/ArmenianAnalyzer.java  |   4 +-
 .../analysis/id/IndonesianAnalyzer.java       |   4 +-
 .../lucene/analysis/it/ItalianAnalyzer.java   |   4 +-
 .../lucene/analysis/lv/LatvianAnalyzer.java   |   4 +-
 .../LimitTokenCountAnalyzer.java              |  33 +-
 .../miscellaneous/PatternAnalyzer.java        |   3 +-
 .../PerFieldAnalyzerWrapper.java              |  45 +--
 .../lucene/analysis/nl/DutchAnalyzer.java     |   3 +-
 .../lucene/analysis/no/NorwegianAnalyzer.java |   4 +-
 .../analysis/pt/PortugueseAnalyzer.java       |   4 +-
 .../query/QueryAutoStopWordAnalyzer.java      |  84 +----
 .../lucene/analysis/ro/RomanianAnalyzer.java  |   4 +-
 .../lucene/analysis/ru/RussianAnalyzer.java   |   4 +-
 .../shingle/ShingleAnalyzerWrapper.java       |  53 +--
 .../analysis/snowball/SnowballAnalyzer.java   |   3 +-
 .../lucene/analysis/sv/SwedishAnalyzer.java   |   4 +-
 .../lucene/analysis/th/ThaiAnalyzer.java      |   7 +-
 .../lucene/analysis/tr/TurkishAnalyzer.java   |   4 +-
 .../analysis/util/StopwordAnalyzerBase.java   |   7 +-
 .../collation/CollationKeyAnalyzer.java       |   4 +-
 .../analysis/cn/TestChineseTokenizer.java     |   4 +-
 .../commongrams/CommonGramsFilterTest.java    |   4 +-
 .../lucene/analysis/core/TestAnalyzers.java   |   2 +-
 .../analysis/core/TestStandardAnalyzer.java   |   3 +-
 .../core/TestUAX29URLEmailTokenizer.java      |   9 +-
 .../de/TestGermanLightStemFilter.java         |   3 +-
 .../de/TestGermanMinimalStemFilter.java       |   3 +-
 .../analysis/de/TestGermanStemFilter.java     |   3 +-
 .../en/TestEnglishMinimalStemFilter.java      |   3 +-
 .../lucene/analysis/en/TestKStemmer.java      |   3 +-
 .../analysis/en/TestPorterStemFilter.java     |   3 +-
 .../es/TestSpanishLightStemFilter.java        |   3 +-
 .../fi/TestFinnishLightStemFilter.java        |   3 +-
 .../fr/TestFrenchLightStemFilter.java         |   3 +-
 .../fr/TestFrenchMinimalStemFilter.java       |   3 +-
 .../analysis/gl/TestGalicianStemFilter.java   |   3 +-
 .../hu/TestHungarianLightStemFilter.java      |   3 +-
 .../analysis/id/TestIndonesianStemmer.java    |   5 +-
 .../it/TestItalianLightStemFilter.java        |   3 +-
 .../analysis/lv/TestLatvianStemmer.java       |   3 +-
 .../TestWordDelimiterFilter.java              |   6 +-
 .../pt/TestPortugueseLightStemFilter.java     |   3 +-
 .../pt/TestPortugueseMinimalStemFilter.java   |   3 +-
 .../analysis/pt/TestPortugueseStemFilter.java |   3 +-
 .../ru/TestRussianLightStemFilter.java        |   3 +-
 .../analysis/snowball/TestSnowballVocab.java  |   3 +-
 .../sv/TestSwedishLightStemFilter.java        |   3 +-
 .../synonym/TestSolrSynonymParser.java        |   5 +-
 .../synonym/TestSynonymMapFilter.java         |  21 +-
 .../synonym/TestWordnetSynonymParser.java     |   3 +-
 .../collation/TestCollationKeyFilter.java     |   2 +-
 .../collation/ICUCollationKeyAnalyzer.java    |   4 +-
 .../analysis/icu/TestICUFoldingFilter.java    |   2 +-
 .../icu/TestICUNormalizer2Filter.java         |   4 +-
 .../analysis/icu/TestICUTransformFilter.java  |   3 +-
 .../icu/segmentation/TestICUTokenizer.java    |   3 +-
 .../collation/TestICUCollationKeyFilter.java  |   2 +-
 .../morfologik/MorfologikAnalyzer.java        |   8 +-
 .../cn/smart/SmartChineseAnalyzer.java        |   3 +-
 .../lucene/analysis/pl/PolishAnalyzer.java    |   4 +-
 .../search/CategoryListIteratorTest.java      |   2 +-
 .../analyzing/TestAnalyzingQueryParser.java   |   2 +-
 .../classic/TestMultiAnalyzer.java            |   4 +-
 .../classic/TestMultiFieldQueryParser.java    |   2 +-
 .../classic/TestMultiPhraseQueryParsing.java  |   3 +-
 .../precedence/TestPrecedenceQueryParser.java |   2 +-
 .../standard/TestMultiAnalyzerQPHelper.java   |   4 +-
 .../standard/TestMultiFieldQPHelper.java      |   2 +-
 .../flexible/standard/TestQPHelper.java       |   6 +-
 .../queryparser/util/QueryParserTestBase.java |  12 +-
 .../analysis/FSTSynonymFilterFactory.java     |   3 +-
 .../apache/solr/analysis/SolrAnalyzer.java    |  44 +--
 .../apache/solr/analysis/TokenizerChain.java  |  23 +-
 .../handler/AnalysisRequestHandlerBase.java   |   4 +-
 .../org/apache/solr/schema/BoolField.java     |   5 +-
 .../org/apache/solr/schema/FieldType.java     |   4 +-
 .../org/apache/solr/schema/IndexSchema.java   |  45 +--
 .../schema/IndexSchemaRuntimeFieldTest.java   |  72 ++++
 .../apache/solr/schema/IndexSchemaTest.java   |  63 +---
 solr/webapp/web/admin/analysis.jsp            |   2 +-
 136 files changed, 890 insertions(+), 943 deletions(-)
 create mode 100644 lucene/src/java/org/apache/lucene/analysis/Analyzer.java.old
 create mode 100644 lucene/src/java/org/apache/lucene/analysis/AnalyzerWrapper.java
 delete mode 100644 lucene/src/java/org/apache/lucene/analysis/ReusableAnalyzerBase.java
 create mode 100644 solr/core/src/test/org/apache/solr/schema/IndexSchemaRuntimeFieldTest.java

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index bfc37f7bf41..84e5359b2ee 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -174,6 +174,10 @@ Changes in backwards compatibility policy
 * LUCENE-3396: ReusableAnalyzerBase.TokenStreamComponents.reset(Reader) now returns void instead
   of boolean.  If a Component cannot be reset, it should throw an Exception.  (Chris Male)
 
* LUCENE-3396: ReusableAnalyzerBase has been renamed to Analyzer.  All Analyzer implementations
  must now use Analyzer.TokenStreamComponents, rather than overriding .tokenStream() and
  .reusableTokenStream() (which are now final). (Chris Male)

 Changes in Runtime Behavior
 
 * LUCENE-2846: omitNorms now behaves like omitTermFrequencyAndPositions, if you
diff --git a/lucene/MIGRATE.txt b/lucene/MIGRATE.txt
index 8cf47a8e6f8..be1dc184ea0 100644
-- a/lucene/MIGRATE.txt
++ b/lucene/MIGRATE.txt
@@ -517,3 +517,8 @@ If you did this before (bytes is a byte[]):
 you can now do this:
 
   new BinaryField("field", bytes)

* LUCENE-3396: Analyzer.tokenStream() and .reusableTokenStream() have been made final.
  It is now necessary to use Analyzer.TokenStreamComponents to define an analysis process.
  Analyzer also has its own way of managing the reuse of TokenStreamComponents (either
  globally, or per-field).  To define another Strategy, implement Analyzer.ReuseStrategy.
\ No newline at end of file
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java
index 2b2e176942b..4f0369ad493 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java
@@ -1802,7 +1802,7 @@ public class HighlighterTest extends BaseTokenStreamTestCase implements Formatte
 // behaviour to synonyms
 // ===================================================================
 
final class SynonymAnalyzer extends ReusableAnalyzerBase {
final class SynonymAnalyzer extends Analyzer {
   private Map<String,String> synonyms;
 
   public SynonymAnalyzer(Map<String,String> synonyms) {
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/OffsetLimitTokenFilterTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/OffsetLimitTokenFilterTest.java
index 8afcecd6f00..d22d7215d4d 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/OffsetLimitTokenFilterTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/OffsetLimitTokenFilterTest.java
@@ -49,7 +49,7 @@ public class OffsetLimitTokenFilterTest extends BaseTokenStreamTestCase {
     assertTokenStreamContents(filter, new String[] {"short", "toolong",
         "evenmuchlongertext"});
     
    checkOneTermReuse(new ReusableAnalyzerBase() {
    checkOneTermReuse(new Analyzer() {
       
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java
index 0e0bb8585ab..f482a674c44 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java
@@ -48,7 +48,7 @@ import org.apache.lucene.util.LuceneTestCase;
 public class TokenSourcesTest extends LuceneTestCase {
   private static final String FIELD = "text";
 
  private static final class OverlapAnalyzer extends ReusableAnalyzerBase {
  private static final class OverlapAnalyzer extends Analyzer {
 
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/AbstractTestCase.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/AbstractTestCase.java
index c28b3dac6c0..080252ba7f4 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/AbstractTestCase.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/AbstractTestCase.java
@@ -194,7 +194,7 @@ public abstract class AbstractTestCase extends LuceneTestCase {
     return phraseQuery;
   }
 
  static final class BigramAnalyzer extends ReusableAnalyzerBase {
  static final class BigramAnalyzer extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       return new TokenStreamComponents(new BasicNGramTokenizer(reader));
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/IndexTimeSynonymTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/IndexTimeSynonymTest.java
index 433c6347bb2..feb06b5e80a 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/IndexTimeSynonymTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/IndexTimeSynonymTest.java
@@ -290,7 +290,7 @@ public class IndexTimeSynonymTest extends AbstractTestCase {
     return token;
   }
   
  public static final class TokenArrayAnalyzer extends ReusableAnalyzerBase {
  public static final class TokenArrayAnalyzer extends Analyzer {
     final Token[] tokens;
     public TokenArrayAnalyzer(Token... tokens) {
       this.tokens = tokens;
diff --git a/lucene/src/java/org/apache/lucene/analysis/Analyzer.java b/lucene/src/java/org/apache/lucene/analysis/Analyzer.java
index 9529250f7fc..683bd8014fd 100644
-- a/lucene/src/java/org/apache/lucene/analysis/Analyzer.java
++ b/lucene/src/java/org/apache/lucene/analysis/Analyzer.java
@@ -1,6 +1,6 @@
 package org.apache.lucene.analysis;
 
/**
/*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
@@ -17,98 +17,106 @@ package org.apache.lucene.analysis;
  * limitations under the License.
  */
 
import java.io.Reader;
import java.io.IOException;
import java.io.Closeable;
import java.lang.reflect.Modifier;

 import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.CloseableThreadLocal;
 import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.util.CloseableThreadLocal;
 
/** An Analyzer builds TokenStreams, which analyze text.  It thus represents a
 *  policy for extracting index terms from text.
 *  <p>
 *  Typical implementations first build a Tokenizer, which breaks the stream of
 *  characters from the Reader into raw Tokens.  One or more TokenFilters may
 *  then be applied to the output of the Tokenizer.
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * An Analyzer builds TokenStreams, which analyze text.  It thus represents a
 * policy for extracting index terms from text.
 * <p>
 * To prevent consistency problems, this class does not allow subclasses to
 * extend {@link #reusableTokenStream(String, Reader)} or
 * {@link #tokenStream(String, Reader)} directly. Instead, subclasses must
 * implement {@link #createComponents(String, Reader)}.
 * </p>
  * <p>The {@code Analyzer}-API in Lucene is based on the decorator pattern.
 * Therefore all non-abstract subclasses must be final or their {@link #tokenStream}
 * and {@link #reusableTokenStream} implementations must be final! This is checked
 * Therefore all non-abstract subclasses must be final! This is checked
  * when Java assertions are enabled.
  */
public abstract class Analyzer implements Closeable {
public abstract class Analyzer {
 
  protected Analyzer() {
    super();
    assert assertFinal();
  private final ReuseStrategy reuseStrategy;

  public Analyzer() {
    this(new GlobalReuseStrategy());
   }
  
  private boolean assertFinal() {
    try {
      final Class<?> clazz = getClass();
      if (!clazz.desiredAssertionStatus())
        return true;
      assert clazz.isAnonymousClass() ||
        (clazz.getModifiers() & (Modifier.FINAL | Modifier.PRIVATE)) != 0 ||
        (
          Modifier.isFinal(clazz.getMethod("tokenStream", String.class, Reader.class).getModifiers()) &&
          Modifier.isFinal(clazz.getMethod("reusableTokenStream", String.class, Reader.class).getModifiers())
        ) :
        "Analyzer implementation classes or at least their tokenStream() and reusableTokenStream() implementations must be final";
      return true;
    } catch (NoSuchMethodException nsme) {
      return false;
    }

  public Analyzer(ReuseStrategy reuseStrategy) {
    this.reuseStrategy = reuseStrategy;
   }
 
  /** Creates a TokenStream which tokenizes all the text in the provided
   * Reader.  Must be able to handle null field name for
   * backward compatibility.
   */
  public abstract TokenStream tokenStream(String fieldName, Reader reader);

  /** Creates a TokenStream that is allowed to be re-used
   *  from the previous time that the same thread called
   *  this method.  Callers that do not need to use more
   *  than one TokenStream at the same time from this
   *  analyzer should use this method for better
   *  performance.
  /**
   * Creates a new {@link TokenStreamComponents} instance for this analyzer.
   * 
   * @param fieldName
   *          the name of the fields content passed to the
   *          {@link TokenStreamComponents} sink as a reader
   * @param aReader
   *          the reader passed to the {@link Tokenizer} constructor
   * @return the {@link TokenStreamComponents} for this analyzer.
    */
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    return tokenStream(fieldName, reader);
  }
  protected abstract TokenStreamComponents createComponents(String fieldName,
      Reader aReader);
 
  private CloseableThreadLocal<Object> tokenStreams = new CloseableThreadLocal<Object>();

  /** Used by Analyzers that implement reusableTokenStream
   *  to retrieve previously saved TokenStreams for re-use
   *  by the same thread. */
  protected Object getPreviousTokenStream() {
    try {
      return tokenStreams.get();
    } catch (NullPointerException npe) {
      if (tokenStreams == null) {
        throw new AlreadyClosedException("this Analyzer is closed");
      } else {
        throw npe;
      }
  /**
   * Creates a TokenStream that is allowed to be re-use from the previous time
   * that the same thread called this method.  Callers that do not need to use
   * more than one TokenStream at the same time from this analyzer should use
   * this method for better performance.
   * <p>
   * This method uses {@link #createComponents(String, Reader)} to obtain an
   * instance of {@link TokenStreamComponents}. It returns the sink of the
   * components and stores the components internally. Subsequent calls to this
   * method will reuse the previously stored components after resetting them
   * through {@link TokenStreamComponents#reset(Reader)}.
   * </p>
   * 
   * @param fieldName the name of the field the created TokenStream is used for
   * @param reader the reader the streams source reads from
   */
  public final TokenStream reusableTokenStream(final String fieldName,
      final Reader reader) throws IOException {
    TokenStreamComponents components = reuseStrategy.getReusableComponents(fieldName);
    final Reader r = initReader(reader);
    if (components == null) {
      components = createComponents(fieldName, r);
      reuseStrategy.setReusableComponents(fieldName, components);
    } else {
      components.reset(r);
     }
    return components.getTokenStream();
   }
 
  /** Used by Analyzers that implement reusableTokenStream
   *  to save a TokenStream for later re-use by the same
   *  thread. */
  protected void setPreviousTokenStream(Object obj) {
    try {
      tokenStreams.set(obj);
    } catch (NullPointerException npe) {
      if (tokenStreams == null) {
        throw new AlreadyClosedException("this Analyzer is closed");
      } else {
        throw npe;
      }
    }
  /**
   * Creates a TokenStream which tokenizes all the text in the provided
   * Reader.
   * <p>
   * This method uses {@link #createComponents(String, Reader)} to obtain an
   * instance of {@link TokenStreamComponents} and returns the sink of the
   * components. Each calls to this method will create a new instance of
   * {@link TokenStreamComponents}. Created {@link TokenStream} instances are 
   * never reused.
   * </p>
   * 
   * @param fieldName the name of the field the created TokenStream is used for
   * @param reader the reader the streams source reads from
   */
  public final TokenStream tokenStream(final String fieldName,
      final Reader reader) {
    return createComponents(fieldName, initReader(reader)).getTokenStream();
  }
  
  /**
   * Override this if you want to add a CharFilter chain.
   */
  protected Reader initReader(Reader reader) {
    return reader;
   }
 
   /**
@@ -149,7 +157,196 @@ public abstract class Analyzer implements Closeable {
 
   /** Frees persistent resources used by this Analyzer */
   public void close() {
    tokenStreams.close();
    tokenStreams = null;
    reuseStrategy.close();
   }

  /**
   * This class encapsulates the outer components of a token stream. It provides
   * access to the source ({@link Tokenizer}) and the outer end (sink), an
   * instance of {@link TokenFilter} which also serves as the
   * {@link TokenStream} returned by
   * {@link Analyzer#tokenStream(String, Reader)} and
   * {@link Analyzer#reusableTokenStream(String, Reader)}.
   */
  public static class TokenStreamComponents {
    protected final Tokenizer source;
    protected final TokenStream sink;

    /**
     * Creates a new {@link TokenStreamComponents} instance.
     * 
     * @param source
     *          the analyzer's tokenizer
     * @param result
     *          the analyzer's resulting token stream
     */
    public TokenStreamComponents(final Tokenizer source,
        final TokenStream result) {
      this.source = source;
      this.sink = result;
    }
    
    /**
     * Creates a new {@link TokenStreamComponents} instance.
     * 
     * @param source
     *          the analyzer's tokenizer
     */
    public TokenStreamComponents(final Tokenizer source) {
      this.source = source;
      this.sink = source;
    }

    /**
     * Resets the encapsulated components with the given reader. If the components
     * cannot be reset, an Exception should be thrown.
     * 
     * @param reader
     *          a reader to reset the source component
     * @throws IOException
     *           if the component's reset method throws an {@link IOException}
     */
    protected void reset(final Reader reader) throws IOException {
      source.reset(reader);
    }

    /**
     * Returns the sink {@link TokenStream}
     * 
     * @return the sink {@link TokenStream}
     */
    public TokenStream getTokenStream() {
      return sink;
    }

    /**
     * Returns the component's {@link Tokenizer}
     *
     * @return Component's {@link Tokenizer}
     */
    public Tokenizer getTokenizer() {
      return source;
    }
  }

  /**
   * Strategy defining how TokenStreamComponents are reused per call to
   * {@link Analyzer#tokenStream(String, java.io.Reader)}.
   */
  public static abstract class ReuseStrategy {

    private CloseableThreadLocal<Object> storedValue = new CloseableThreadLocal<Object>();

    /**
     * Gets the reusable TokenStreamComponents for the field with the given name
     *
     * @param fieldName Name of the field whose reusable TokenStreamComponents
     *        are to be retrieved
     * @return Reusable TokenStreamComponents for the field, or {@code null}
     *         if there was no previous components for the field
     */
    public abstract TokenStreamComponents getReusableComponents(String fieldName);

    /**
     * Stores the given TokenStreamComponents as the reusable components for the
     * field with the give name
     *
     * @param fieldName Name of the field whose TokenStreamComponents are being set
     * @param components TokenStreamComponents which are to be reused for the field
     */
    public abstract void setReusableComponents(String fieldName, TokenStreamComponents components);

    /**
     * Returns the currently stored value
     *
     * @return Currently stored value or {@code null} if no value is stored
     */
    protected final Object getStoredValue() {
      try {
        return storedValue.get();
      } catch (NullPointerException npe) {
        if (storedValue == null) {
          throw new AlreadyClosedException("this Analyzer is closed");
        } else {
          throw npe;
        }
      }
    }

    /**
     * Sets the stored value
     *
     * @param storedValue Value to store
     */
    protected final void setStoredValue(Object storedValue) {
      try {
        this.storedValue.set(storedValue);
      } catch (NullPointerException npe) {
        if (storedValue == null) {
          throw new AlreadyClosedException("this Analyzer is closed");
        } else {
          throw npe;
        }
      }
    }

    /**
     * Closes the ReuseStrategy, freeing any resources
     */
    public void close() {
      storedValue.close();
      storedValue = null;
    }
  }

  /**
   * Implementation of {@link ReuseStrategy} that reuses the same components for
   * every field.
   */
  public final static class GlobalReuseStrategy extends ReuseStrategy {

    /**
     * {@inheritDoc}
     */
    public TokenStreamComponents getReusableComponents(String fieldName) {
      return (TokenStreamComponents) getStoredValue();
    }

    /**
     * {@inheritDoc}
     */
    public void setReusableComponents(String fieldName, TokenStreamComponents components) {
      setStoredValue(components);
    }
  }

  /**
   * Implementation of {@link ReuseStrategy} that reuses components per-field by
   * maintaining a Map of TokenStreamComponent per field name.
   */
  public static class PerFieldReuseStrategy extends ReuseStrategy {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public TokenStreamComponents getReusableComponents(String fieldName) {
      Map<String, TokenStreamComponents> componentsPerField = (Map<String, TokenStreamComponents>) getStoredValue();
      return componentsPerField != null ? componentsPerField.get(fieldName) : null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setReusableComponents(String fieldName, TokenStreamComponents components) {
      Map<String, TokenStreamComponents> componentsPerField = (Map<String, TokenStreamComponents>) getStoredValue();
      if (componentsPerField == null) {
        componentsPerField = new HashMap<String, TokenStreamComponents>();
        setStoredValue(componentsPerField);
      }
      componentsPerField.put(fieldName, components);
    }
  }

 }
diff --git a/lucene/src/java/org/apache/lucene/analysis/Analyzer.java.old b/lucene/src/java/org/apache/lucene/analysis/Analyzer.java.old
new file mode 100644
index 00000000000..9529250f7fc
-- /dev/null
++ b/lucene/src/java/org/apache/lucene/analysis/Analyzer.java.old
@@ -0,0 +1,155 @@
package org.apache.lucene.analysis;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Reader;
import java.io.IOException;
import java.io.Closeable;
import java.lang.reflect.Modifier;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.CloseableThreadLocal;
import org.apache.lucene.store.AlreadyClosedException;

/** An Analyzer builds TokenStreams, which analyze text.  It thus represents a
 *  policy for extracting index terms from text.
 *  <p>
 *  Typical implementations first build a Tokenizer, which breaks the stream of
 *  characters from the Reader into raw Tokens.  One or more TokenFilters may
 *  then be applied to the output of the Tokenizer.
 * <p>The {@code Analyzer}-API in Lucene is based on the decorator pattern.
 * Therefore all non-abstract subclasses must be final or their {@link #tokenStream}
 * and {@link #reusableTokenStream} implementations must be final! This is checked
 * when Java assertions are enabled.
 */
public abstract class Analyzer implements Closeable {

  protected Analyzer() {
    super();
    assert assertFinal();
  }
  
  private boolean assertFinal() {
    try {
      final Class<?> clazz = getClass();
      if (!clazz.desiredAssertionStatus())
        return true;
      assert clazz.isAnonymousClass() ||
        (clazz.getModifiers() & (Modifier.FINAL | Modifier.PRIVATE)) != 0 ||
        (
          Modifier.isFinal(clazz.getMethod("tokenStream", String.class, Reader.class).getModifiers()) &&
          Modifier.isFinal(clazz.getMethod("reusableTokenStream", String.class, Reader.class).getModifiers())
        ) :
        "Analyzer implementation classes or at least their tokenStream() and reusableTokenStream() implementations must be final";
      return true;
    } catch (NoSuchMethodException nsme) {
      return false;
    }
  }

  /** Creates a TokenStream which tokenizes all the text in the provided
   * Reader.  Must be able to handle null field name for
   * backward compatibility.
   */
  public abstract TokenStream tokenStream(String fieldName, Reader reader);

  /** Creates a TokenStream that is allowed to be re-used
   *  from the previous time that the same thread called
   *  this method.  Callers that do not need to use more
   *  than one TokenStream at the same time from this
   *  analyzer should use this method for better
   *  performance.
   */
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    return tokenStream(fieldName, reader);
  }

  private CloseableThreadLocal<Object> tokenStreams = new CloseableThreadLocal<Object>();

  /** Used by Analyzers that implement reusableTokenStream
   *  to retrieve previously saved TokenStreams for re-use
   *  by the same thread. */
  protected Object getPreviousTokenStream() {
    try {
      return tokenStreams.get();
    } catch (NullPointerException npe) {
      if (tokenStreams == null) {
        throw new AlreadyClosedException("this Analyzer is closed");
      } else {
        throw npe;
      }
    }
  }

  /** Used by Analyzers that implement reusableTokenStream
   *  to save a TokenStream for later re-use by the same
   *  thread. */
  protected void setPreviousTokenStream(Object obj) {
    try {
      tokenStreams.set(obj);
    } catch (NullPointerException npe) {
      if (tokenStreams == null) {
        throw new AlreadyClosedException("this Analyzer is closed");
      } else {
        throw npe;
      }
    }
  }

  /**
   * Invoked before indexing a IndexableField instance if
   * terms have already been added to that field.  This allows custom
   * analyzers to place an automatic position increment gap between
   * IndexbleField instances using the same field name.  The default value
   * position increment gap is 0.  With a 0 position increment gap and
   * the typical default token position increment of 1, all terms in a field,
   * including across IndexableField instances, are in successive positions, allowing
   * exact PhraseQuery matches, for instance, across IndexableField instance boundaries.
   *
   * @param fieldName IndexableField name being indexed.
   * @return position increment gap, added to the next token emitted from {@link #tokenStream(String,Reader)}
   */
  public int getPositionIncrementGap(String fieldName) {
    return 0;
  }

  /**
   * Just like {@link #getPositionIncrementGap}, except for
   * Token offsets instead.  By default this returns 1 for
   * tokenized fields and, as if the fields were joined
   * with an extra space character, and 0 for un-tokenized
   * fields.  This method is only called if the field
   * produced at least one token for indexing.
   *
   * @param field the field just indexed
   * @return offset gap, added to the next token emitted from {@link #tokenStream(String,Reader)}
   */
  public int getOffsetGap(IndexableField field) {
    if (field.fieldType().tokenized()) {
      return 1;
    } else {
      return 0;
    }
  }

  /** Frees persistent resources used by this Analyzer */
  public void close() {
    tokenStreams.close();
    tokenStreams = null;
  }
}
diff --git a/lucene/src/java/org/apache/lucene/analysis/AnalyzerWrapper.java b/lucene/src/java/org/apache/lucene/analysis/AnalyzerWrapper.java
new file mode 100644
index 00000000000..d9b766ac50a
-- /dev/null
++ b/lucene/src/java/org/apache/lucene/analysis/AnalyzerWrapper.java
@@ -0,0 +1,89 @@
package org.apache.lucene.analysis;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.index.IndexableField;

import java.io.Reader;

/**
 * Extension to {@link Analyzer} suitable for Analyzers which wrap
 * other Analyzers.
 * <p/>
 * {@link #getWrappedAnalyzer(String)} allows the Analyzer
 * to wrap multiple Analyzers which are selected on a per field basis.
 * <p/>
 * {@link #wrapComponents(String, Analyzer.TokenStreamComponents)} allows the
 * TokenStreamComponents of the wrapped Analyzer to then be wrapped
 * (such as adding a new {@link TokenFilter} to form new TokenStreamComponents.
 */
public abstract class AnalyzerWrapper extends Analyzer {

  /**
   * Creates a new AnalyzerWrapper.  Since the {@link Analyzer.ReuseStrategy} of
   * the wrapped Analyzers are unknown, {@link Analyzer.PerFieldReuseStrategy} is assumed
   */
  protected AnalyzerWrapper() {
    super(new PerFieldReuseStrategy());
  }

  /**
   * Retrieves the wrapped Analyzer appropriate for analyzing the field with
   * the given name
   *
   * @param fieldName Name of the field which is to be analyzed
   * @return Analyzer for the field with the given name.  Assumed to be non-null
   */
  protected abstract Analyzer getWrappedAnalyzer(String fieldName);

  /**
   * Wraps / alters the given TokenStreamComponents, taken from the wrapped
   * Analyzer, to form new components.  It is through this method that new
   * TokenFilters can be added by AnalyzerWrappers.
   *
   *
   * @param fieldName Name of the field which is to be analyzed
   * @param components TokenStreamComponents taken from the wrapped Analyzer
   * @return Wrapped / altered TokenStreamComponents.
   */
  protected abstract TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components);

  /**
   * {@inheritDoc}
   */
  @Override
  protected final TokenStreamComponents createComponents(String fieldName, Reader aReader) {
    return wrapComponents(fieldName, getWrappedAnalyzer(fieldName).createComponents(fieldName, aReader));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int getPositionIncrementGap(String fieldName) {
    return getWrappedAnalyzer(fieldName).getPositionIncrementGap(fieldName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int getOffsetGap(IndexableField field) {
    return getWrappedAnalyzer(field.name()).getOffsetGap(field);
  }
}
diff --git a/lucene/src/java/org/apache/lucene/analysis/ReusableAnalyzerBase.java b/lucene/src/java/org/apache/lucene/analysis/ReusableAnalyzerBase.java
deleted file mode 100644
index 638e7ab53d3..00000000000
-- a/lucene/src/java/org/apache/lucene/analysis/ReusableAnalyzerBase.java
++ /dev/null
@@ -1,308 +0,0 @@
package org.apache.lucene.analysis;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.util.CloseableThreadLocal;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * An convenience subclass of Analyzer that makes it easy to implement
 * {@link TokenStream} reuse.
 * <p>
 * ReusableAnalyzerBase is a simplification of Analyzer that supports easy reuse
 * for the most common use-cases. Analyzers such as
 * PerFieldAnalyzerWrapper that behave differently depending upon the
 * field name need to subclass Analyzer directly instead.
 * </p>
 * <p>
 * To prevent consistency problems, this class does not allow subclasses to
 * extend {@link #reusableTokenStream(String, Reader)} or
 * {@link #tokenStream(String, Reader)} directly. Instead, subclasses must
 * implement {@link #createComponents(String, Reader)}.
 * </p>
 */
public abstract class ReusableAnalyzerBase extends Analyzer {

  private final ReuseStrategy reuseStrategy;

  public ReusableAnalyzerBase() {
    this(new GlobalReuseStrategy());
  }

  public ReusableAnalyzerBase(ReuseStrategy reuseStrategy) {
    this.reuseStrategy = reuseStrategy;
  }

  /**
   * Creates a new {@link TokenStreamComponents} instance for this analyzer.
   * 
   * @param fieldName
   *          the name of the fields content passed to the
   *          {@link TokenStreamComponents} sink as a reader
   * @param aReader
   *          the reader passed to the {@link Tokenizer} constructor
   * @return the {@link TokenStreamComponents} for this analyzer.
   */
  protected abstract TokenStreamComponents createComponents(String fieldName,
      Reader aReader);

  /**
   * This method uses {@link #createComponents(String, Reader)} to obtain an
   * instance of {@link TokenStreamComponents}. It returns the sink of the
   * components and stores the components internally. Subsequent calls to this
   * method will reuse the previously stored components if and only if the
   * {@link TokenStreamComponents#reset(Reader)} method returned
   * <code>true</code>. Otherwise a new instance of
   * {@link TokenStreamComponents} is created.
   * 
   * @param fieldName the name of the field the created TokenStream is used for
   * @param reader the reader the streams source reads from
   */
  @Override
  public final TokenStream reusableTokenStream(final String fieldName,
      final Reader reader) throws IOException {
    TokenStreamComponents components = reuseStrategy.getReusableComponents(fieldName);
    final Reader r = initReader(reader);
    if (components == null) {
      components = createComponents(fieldName, r);
      reuseStrategy.setReusableComponents(fieldName, components);
    } else {
      components.reset(r);
    }
    return components.getTokenStream();
  }

  /**
   * This method uses {@link #createComponents(String, Reader)} to obtain an
   * instance of {@link TokenStreamComponents} and returns the sink of the
   * components. Each calls to this method will create a new instance of
   * {@link TokenStreamComponents}. Created {@link TokenStream} instances are 
   * never reused.
   * 
   * @param fieldName the name of the field the created TokenStream is used for
   * @param reader the reader the streams source reads from
   */
  @Override
  public final TokenStream tokenStream(final String fieldName,
      final Reader reader) {
    return createComponents(fieldName, initReader(reader)).getTokenStream();
  }
  
  /**
   * Override this if you want to add a CharFilter chain.
   */
  protected Reader initReader(Reader reader) {
    return reader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    super.close();
    reuseStrategy.close();
  }

  /**
   * This class encapsulates the outer components of a token stream. It provides
   * access to the source ({@link Tokenizer}) and the outer end (sink), an
   * instance of {@link TokenFilter} which also serves as the
   * {@link TokenStream} returned by
   * {@link Analyzer#tokenStream(String, Reader)} and
   * {@link Analyzer#reusableTokenStream(String, Reader)}.
   */
  public static class TokenStreamComponents {
    protected final Tokenizer source;
    protected final TokenStream sink;

    /**
     * Creates a new {@link TokenStreamComponents} instance.
     * 
     * @param source
     *          the analyzer's tokenizer
     * @param result
     *          the analyzer's resulting token stream
     */
    public TokenStreamComponents(final Tokenizer source,
        final TokenStream result) {
      this.source = source;
      this.sink = result;
    }
    
    /**
     * Creates a new {@link TokenStreamComponents} instance.
     * 
     * @param source
     *          the analyzer's tokenizer
     */
    public TokenStreamComponents(final Tokenizer source) {
      this.source = source;
      this.sink = source;
    }

    /**
     * Resets the encapsulated components with the given reader. If the components
     * cannot be reset, an Exception should be thrown.
     * 
     * @param reader
     *          a reader to reset the source component
     * @throws IOException
     *           if the component's reset method throws an {@link IOException}
     */
    protected void reset(final Reader reader) throws IOException {
      source.reset(reader);
    }

    /**
     * Returns the sink {@link TokenStream}
     * 
     * @return the sink {@link TokenStream}
     */
    protected TokenStream getTokenStream() {
      return sink;
    }

  }

  /**
   * Strategy defining how TokenStreamComponents are reused per call to
   * {@link ReusableAnalyzerBase#tokenStream(String, java.io.Reader)}.
   */
  public static abstract class ReuseStrategy {

    private CloseableThreadLocal<Object> storedValue = new CloseableThreadLocal<Object>();

    /**
     * Gets the reusable TokenStreamComponents for the field with the given name
     *
     * @param fieldName Name of the field whose reusable TokenStreamComponents
     *        are to be retrieved
     * @return Reusable TokenStreamComponents for the field, or {@code null}
     *         if there was no previous components for the field
     */
    public abstract TokenStreamComponents getReusableComponents(String fieldName);

    /**
     * Stores the given TokenStreamComponents as the reusable components for the
     * field with the give name
     *
     * @param fieldName Name of the field whose TokenStreamComponents are being set
     * @param components TokenStreamComponents which are to be reused for the field
     */
    public abstract void setReusableComponents(String fieldName, TokenStreamComponents components);

    /**
     * Returns the currently stored value
     *
     * @return Currently stored value or {@code null} if no value is stored
     */
    protected final Object getStoredValue() {
      try {
        return storedValue.get();
      } catch (NullPointerException npe) {
        if (storedValue == null) {
          throw new AlreadyClosedException("this Analyzer is closed");
        } else {
          throw npe;
        }
      }
    }

    /**
     * Sets the stored value
     *
     * @param storedValue Value to store
     */
    protected final void setStoredValue(Object storedValue) {
      try {
        this.storedValue.set(storedValue);
      } catch (NullPointerException npe) {
        if (storedValue == null) {
          throw new AlreadyClosedException("this Analyzer is closed");
        } else {
          throw npe;
        }
      }
    }

    /**
     * Closes the ReuseStrategy, freeing any resources
     */
    public void close() {
      storedValue.close();
      storedValue = null;
    }
  }

  /**
   * Implementation of {@link ReuseStrategy} that reuses the same components for
   * every field.
   */
  public final static class GlobalReuseStrategy extends ReuseStrategy {

    /**
     * {@inheritDoc}
     */
    public TokenStreamComponents getReusableComponents(String fieldName) {
      return (TokenStreamComponents) getStoredValue();
    }

    /**
     * {@inheritDoc}
     */
    public void setReusableComponents(String fieldName, TokenStreamComponents components) {
      setStoredValue(components);
    }
  }

  /**
   * Implementation of {@link ReuseStrategy} that reuses components per-field by
   * maintaining a Map of TokenStreamComponent per field name.
   */
  public static class PerFieldReuseStrategy extends ReuseStrategy {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public TokenStreamComponents getReusableComponents(String fieldName) {
      Map<String, TokenStreamComponents> componentsPerField = (Map<String, TokenStreamComponents>) getStoredValue();
      return componentsPerField != null ? componentsPerField.get(fieldName) : null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setReusableComponents(String fieldName, TokenStreamComponents components) {
      Map<String, TokenStreamComponents> componentsPerField = (Map<String, TokenStreamComponents>) getStoredValue();
      if (componentsPerField == null) {
        componentsPerField = new HashMap<String, TokenStreamComponents>();
        setStoredValue(componentsPerField);
      }
      componentsPerField.put(fieldName, components);
    }
  }

}
diff --git a/lucene/src/test-framework/org/apache/lucene/analysis/MockAnalyzer.java b/lucene/src/test-framework/org/apache/lucene/analysis/MockAnalyzer.java
index 6762bd03b89..8083a51b149 100644
-- a/lucene/src/test-framework/org/apache/lucene/analysis/MockAnalyzer.java
++ b/lucene/src/test-framework/org/apache/lucene/analysis/MockAnalyzer.java
@@ -42,7 +42,7 @@ import org.apache.lucene.util.automaton.CharacterRunAutomaton;
  * </ul>
  * @see MockTokenizer
  */
public final class MockAnalyzer extends ReusableAnalyzerBase {
public final class MockAnalyzer extends Analyzer {
   private final CharacterRunAutomaton runAutomaton;
   private final boolean lowerCase;
   private final CharacterRunAutomaton filter;
diff --git a/lucene/src/test-framework/org/apache/lucene/analysis/MockPayloadAnalyzer.java b/lucene/src/test-framework/org/apache/lucene/analysis/MockPayloadAnalyzer.java
index dbf9c2a2026..54234a0783b 100644
-- a/lucene/src/test-framework/org/apache/lucene/analysis/MockPayloadAnalyzer.java
++ b/lucene/src/test-framework/org/apache/lucene/analysis/MockPayloadAnalyzer.java
@@ -30,7 +30,7 @@ import java.io.Reader;
  *
  *
  **/
public final class MockPayloadAnalyzer extends ReusableAnalyzerBase {
public final class MockPayloadAnalyzer extends Analyzer {
 
   @Override
   public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/lucene/src/test/org/apache/lucene/TestAssertions.java b/lucene/src/test/org/apache/lucene/TestAssertions.java
index 4a3c75e1310..34138ae83f1 100644
-- a/lucene/src/test/org/apache/lucene/TestAssertions.java
++ b/lucene/src/test/org/apache/lucene/TestAssertions.java
@@ -19,7 +19,6 @@ package org.apache.lucene;
 
 import java.io.Reader;
 
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.TokenStream;
@@ -35,7 +34,7 @@ public class TestAssertions extends LuceneTestCase {
     }
   }
   
  static class TestAnalyzer1 extends ReusableAnalyzerBase {
  static class TestAnalyzer1 extends Analyzer {
 
     @Override
     protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
@@ -43,7 +42,7 @@ public class TestAssertions extends LuceneTestCase {
     }
   }
 
  static final class TestAnalyzer2 extends ReusableAnalyzerBase {
  static final class TestAnalyzer2 extends Analyzer {
 
     @Override
     protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
@@ -51,7 +50,7 @@ public class TestAssertions extends LuceneTestCase {
     }
   }
 
  static class TestAnalyzer3 extends ReusableAnalyzerBase {
  static class TestAnalyzer3 extends Analyzer {
 
     @Override
     protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
@@ -59,7 +58,7 @@ public class TestAssertions extends LuceneTestCase {
     }
   }
 
  static class TestAnalyzer4 extends ReusableAnalyzerBase {
  static class TestAnalyzer4 extends Analyzer {
 
     @Override
     protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
diff --git a/lucene/src/test/org/apache/lucene/index/TestDocumentWriter.java b/lucene/src/test/org/apache/lucene/index/TestDocumentWriter.java
index 3dc7c055a9b..a4f90814952 100644
-- a/lucene/src/test/org/apache/lucene/index/TestDocumentWriter.java
++ b/lucene/src/test/org/apache/lucene/index/TestDocumentWriter.java
@@ -103,7 +103,7 @@ public class TestDocumentWriter extends LuceneTestCase {
   }
 
   public void testPositionIncrementGap() throws IOException {
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, false));
@@ -138,7 +138,7 @@ public class TestDocumentWriter extends LuceneTestCase {
   }
 
   public void testTokenReuse() throws IOException {
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
index 9fca64934d8..31b9aa30362 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
@@ -1706,7 +1706,7 @@ public class TestIndexWriter extends LuceneTestCase {
     dir.close();
   }
 
  static final class StringSplitAnalyzer extends ReusableAnalyzerBase {
  static final class StringSplitAnalyzer extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       return new TokenStreamComponents(new StringSplitTokenizer(reader));
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriterCommit.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriterCommit.java
index 553cf076b91..9e6c9281cc3 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriterCommit.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriterCommit.java
@@ -175,7 +175,7 @@ public class TestIndexWriterCommit extends LuceneTestCase {
     Analyzer analyzer;
     if (random.nextBoolean()) {
       // no payloads
     analyzer = new ReusableAnalyzerBase() {
     analyzer = new Analyzer() {
         @Override
         public TokenStreamComponents createComponents(String fieldName, Reader reader) {
           return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
@@ -184,7 +184,7 @@ public class TestIndexWriterCommit extends LuceneTestCase {
     } else {
       // fixed length payloads
       final int length = random.nextInt(200);
      analyzer = new ReusableAnalyzerBase() {
      analyzer = new Analyzer() {
         @Override
         public TokenStreamComponents createComponents(String fieldName, Reader reader) {
           Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
index bfea6ddcea3..8bb4cf04138 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
@@ -899,7 +899,7 @@ public class TestIndexWriterDelete extends LuceneTestCase {
     final Random r = random;
     Directory dir = newDirectory();
     // note this test explicitly disables payloads
    final Analyzer analyzer = new ReusableAnalyzerBase() {
    final Analyzer analyzer = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
index d0369b0bce4..a9f9e2b6530 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
@@ -386,7 +386,7 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
     doc.add(newField("field", "a field", TextField.TYPE_STORED));
     w.addDocument(doc);
 
    Analyzer analyzer = new ReusableAnalyzerBase(new ReusableAnalyzerBase.PerFieldReuseStrategy()) {
    Analyzer analyzer = new Analyzer(new Analyzer.PerFieldReuseStrategy()) {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         MockTokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -454,7 +454,7 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
   // LUCENE-1072
   public void testExceptionFromTokenStream() throws IOException {
     Directory dir = newDirectory();
    IndexWriterConfig conf = newIndexWriterConfig( TEST_VERSION_CURRENT, new ReusableAnalyzerBase() {
    IndexWriterConfig conf = newIndexWriterConfig( TEST_VERSION_CURRENT, new Analyzer() {
 
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
@@ -591,7 +591,7 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
   }
 
   public void testDocumentsWriterExceptions() throws IOException {
    Analyzer analyzer = new ReusableAnalyzerBase(new ReusableAnalyzerBase.PerFieldReuseStrategy()) {
    Analyzer analyzer = new Analyzer(new Analyzer.PerFieldReuseStrategy()) {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         MockTokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -687,7 +687,7 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
   }
 
   public void testDocumentsWriterExceptionThreads() throws Exception {
    Analyzer analyzer = new ReusableAnalyzerBase(new ReusableAnalyzerBase.PerFieldReuseStrategy()) {
    Analyzer analyzer = new Analyzer(new Analyzer.PerFieldReuseStrategy()) {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         MockTokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
diff --git a/lucene/src/test/org/apache/lucene/index/TestLazyProxSkipping.java b/lucene/src/test/org/apache/lucene/index/TestLazyProxSkipping.java
index d5975eb2bea..65787b06803 100755
-- a/lucene/src/test/org/apache/lucene/index/TestLazyProxSkipping.java
++ b/lucene/src/test/org/apache/lucene/index/TestLazyProxSkipping.java
@@ -68,7 +68,7 @@ public class TestLazyProxSkipping extends LuceneTestCase {
     private void createIndex(int numHits) throws IOException {
         int numDocs = 500;
         
        final Analyzer analyzer = new ReusableAnalyzerBase() {
        final Analyzer analyzer = new Analyzer() {
           @Override
           public TokenStreamComponents createComponents(String fieldName, Reader reader) {
             return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
diff --git a/lucene/src/test/org/apache/lucene/index/TestMultiLevelSkipList.java b/lucene/src/test/org/apache/lucene/index/TestMultiLevelSkipList.java
index 6f4a5d97254..0a911ec6885 100644
-- a/lucene/src/test/org/apache/lucene/index/TestMultiLevelSkipList.java
++ b/lucene/src/test/org/apache/lucene/index/TestMultiLevelSkipList.java
@@ -111,7 +111,7 @@ public class TestMultiLevelSkipList extends LuceneTestCase {
     assertEquals("Wrong payload for the target " + target + ": " + b.bytes[b.offset], (byte) target, b.bytes[b.offset]);
   }
 
  private static class PayloadAnalyzer extends ReusableAnalyzerBase {
  private static class PayloadAnalyzer extends Analyzer {
     private final AtomicInteger payloadCount = new AtomicInteger(-1);
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/lucene/src/test/org/apache/lucene/index/TestPayloads.java b/lucene/src/test/org/apache/lucene/index/TestPayloads.java
index d68a8f8bbba..247d59bb483 100644
-- a/lucene/src/test/org/apache/lucene/index/TestPayloads.java
++ b/lucene/src/test/org/apache/lucene/index/TestPayloads.java
@@ -405,7 +405,7 @@ public class TestPayloads extends LuceneTestCase {
     /**
      * This Analyzer uses an WhitespaceTokenizer and PayloadFilter.
      */
    private static class PayloadAnalyzer extends ReusableAnalyzerBase {
    private static class PayloadAnalyzer extends Analyzer {
         Map<String,PayloadData> fieldToData = new HashMap<String,PayloadData>();
 
         public PayloadAnalyzer() {
diff --git a/lucene/src/test/org/apache/lucene/index/TestSameTokenSamePosition.java b/lucene/src/test/org/apache/lucene/index/TestSameTokenSamePosition.java
index b117adab69b..20184e848c5 100644
-- a/lucene/src/test/org/apache/lucene/index/TestSameTokenSamePosition.java
++ b/lucene/src/test/org/apache/lucene/index/TestSameTokenSamePosition.java
@@ -20,7 +20,7 @@ package org.apache.lucene.index;
 import java.io.IOException;
 import java.io.Reader;
 
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
@@ -63,7 +63,7 @@ public class TestSameTokenSamePosition extends LuceneTestCase {
   }
 }
 
final class BugReproAnalyzer extends ReusableAnalyzerBase {
final class BugReproAnalyzer extends Analyzer {
   @Override
   public TokenStreamComponents createComponents(String arg0, Reader arg1) {
     return new TokenStreamComponents(new BugReproAnalyzerTokenizer());
diff --git a/lucene/src/test/org/apache/lucene/index/TestTermVectorsReader.java b/lucene/src/test/org/apache/lucene/index/TestTermVectorsReader.java
index 2aad70c16f9..bde0890e1b3 100644
-- a/lucene/src/test/org/apache/lucene/index/TestTermVectorsReader.java
++ b/lucene/src/test/org/apache/lucene/index/TestTermVectorsReader.java
@@ -175,7 +175,7 @@ public class TestTermVectorsReader extends LuceneTestCase {
     }
   }
 
  private class MyAnalyzer extends ReusableAnalyzerBase {
  private class MyAnalyzer extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       return new TokenStreamComponents(new MyTokenStream());
diff --git a/lucene/src/test/org/apache/lucene/index/TestTermdocPerf.java b/lucene/src/test/org/apache/lucene/index/TestTermdocPerf.java
index dbe1b94fbd8..44c8394a1a3 100644
-- a/lucene/src/test/org/apache/lucene/index/TestTermdocPerf.java
++ b/lucene/src/test/org/apache/lucene/index/TestTermdocPerf.java
@@ -22,7 +22,6 @@ import java.io.Reader;
 import java.util.Random;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.document.Document;
@@ -77,7 +76,7 @@ public class TestTermdocPerf extends LuceneTestCase {
   void addDocs(final Random random, Directory dir, final int ndocs, String field, final String val, final int maxTF, final float percentDocs) throws IOException {
     final RepeatingTokenStream ts = new RepeatingTokenStream(val, random, percentDocs, maxTF);
 
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         return new TokenStreamComponents(ts);
diff --git a/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java b/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java
index d63296c53ef..137f6fbca60 100644
-- a/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java
@@ -17,7 +17,6 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
@@ -346,7 +345,7 @@ public class TestMultiPhraseQuery extends LuceneTestCase {
     }
   }
 
  private static class CannedAnalyzer extends ReusableAnalyzerBase {
  private static class CannedAnalyzer extends Analyzer {
     private final TokenAndPos[] tokens;
     
     public CannedAnalyzer(TokenAndPos[] tokens) {
diff --git a/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java b/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
index a60a8824377..2398c9f507f 100644
-- a/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
@@ -55,7 +55,7 @@ public class TestPhraseQuery extends LuceneTestCase {
   @BeforeClass
   public static void beforeClass() throws Exception {
     directory = newDirectory();
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, false));
diff --git a/lucene/src/test/org/apache/lucene/search/TestPositionIncrement.java b/lucene/src/test/org/apache/lucene/search/TestPositionIncrement.java
index 4e59e0f3d43..6e128cfef11 100644
-- a/lucene/src/test/org/apache/lucene/search/TestPositionIncrement.java
++ b/lucene/src/test/org/apache/lucene/search/TestPositionIncrement.java
@@ -56,7 +56,7 @@ public class TestPositionIncrement extends LuceneTestCase {
   final static boolean VERBOSE = false;
 
   public void testSetPosition() throws Exception {
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         return new TokenStreamComponents(new Tokenizer() {
diff --git a/lucene/src/test/org/apache/lucene/search/TestTermRangeQuery.java b/lucene/src/test/org/apache/lucene/search/TestTermRangeQuery.java
index 3b6fa24d10c..a93e295778e 100644
-- a/lucene/src/test/org/apache/lucene/search/TestTermRangeQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestTermRangeQuery.java
@@ -190,7 +190,7 @@ public class TestTermRangeQuery extends LuceneTestCase {
     assertFalse("queries with different inclusive are not equal", query.equals(other));
   }
 
  private static class SingleCharAnalyzer extends ReusableAnalyzerBase {
  private static class SingleCharAnalyzer extends Analyzer {
 
     private static class SingleCharTokenizer extends Tokenizer {
       char[] buffer = new char[1];
diff --git a/lucene/src/test/org/apache/lucene/search/payloads/PayloadHelper.java b/lucene/src/test/org/apache/lucene/search/payloads/PayloadHelper.java
index 4648af7871f..13352d40646 100644
-- a/lucene/src/test/org/apache/lucene/search/payloads/PayloadHelper.java
++ b/lucene/src/test/org/apache/lucene/search/payloads/PayloadHelper.java
@@ -55,7 +55,7 @@ public class PayloadHelper {
 
   public IndexReader reader;
 
  public final class PayloadAnalyzer extends ReusableAnalyzerBase {
  public final class PayloadAnalyzer extends Analyzer {
 
     public PayloadAnalyzer() {
       super(new PerFieldReuseStrategy());
diff --git a/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadNearQuery.java b/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadNearQuery.java
index 6b37b5c7d9b..510341e3347 100644
-- a/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadNearQuery.java
++ b/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadNearQuery.java
@@ -55,7 +55,7 @@ public class TestPayloadNearQuery extends LuceneTestCase {
   private static byte[] payload2 = new byte[]{2};
   private static byte[] payload4 = new byte[]{4};
 
  private static class PayloadAnalyzer extends ReusableAnalyzerBase {
  private static class PayloadAnalyzer extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
diff --git a/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadTermQuery.java b/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadTermQuery.java
index cd952447ebb..943fd4676ae 100644
-- a/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadTermQuery.java
++ b/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadTermQuery.java
@@ -64,7 +64,7 @@ public class TestPayloadTermQuery extends LuceneTestCase {
   private static final byte[] payloadMultiField2 = new byte[]{4};
   protected static Directory directory;
 
  private static class PayloadAnalyzer extends ReusableAnalyzerBase {
  private static class PayloadAnalyzer extends Analyzer {
 
     private PayloadAnalyzer() {
       super(new PerFieldReuseStrategy());
diff --git a/lucene/src/test/org/apache/lucene/search/spans/TestBasics.java b/lucene/src/test/org/apache/lucene/search/spans/TestBasics.java
index 15448e5ba49..a364135e0f5 100644
-- a/lucene/src/test/org/apache/lucene/search/spans/TestBasics.java
++ b/lucene/src/test/org/apache/lucene/search/spans/TestBasics.java
@@ -96,7 +96,7 @@ public class TestBasics extends LuceneTestCase {
     }
   }
   
  static final Analyzer simplePayloadAnalyzer = new ReusableAnalyzerBase() {
  static final Analyzer simplePayloadAnalyzer = new Analyzer() {
 
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/lucene/src/test/org/apache/lucene/search/spans/TestPayloadSpans.java b/lucene/src/test/org/apache/lucene/search/spans/TestPayloadSpans.java
index 85771161ef0..321e6fc6d79 100644
-- a/lucene/src/test/org/apache/lucene/search/spans/TestPayloadSpans.java
++ b/lucene/src/test/org/apache/lucene/search/spans/TestPayloadSpans.java
@@ -476,7 +476,7 @@ public class TestPayloadSpans extends LuceneTestCase {
     assertEquals(numSpans, cnt);
   }
 
  final class PayloadAnalyzer extends ReusableAnalyzerBase {
  final class PayloadAnalyzer extends Analyzer {
 
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
@@ -530,7 +530,7 @@ public class TestPayloadSpans extends LuceneTestCase {
     }
   }
   
  public final class TestPayloadAnalyzer extends ReusableAnalyzerBase {
  public final class TestPayloadAnalyzer extends Analyzer {
 
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/ar/ArabicAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/ar/ArabicAnalyzer.java
index 53020bd2524..4549a17e023 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/ar/ArabicAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/ar/ArabicAnalyzer.java
@@ -126,10 +126,10 @@ public final class ArabicAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link LowerCaseFilter}, {@link StopFilter},
    *         {@link ArabicNormalizationFilter}, {@link KeywordMarkerFilter}
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/bg/BulgarianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/bg/BulgarianAnalyzer.java
index 1a82aac2d2c..6f0419ec65f 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/bg/BulgarianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/bg/BulgarianAnalyzer.java
@@ -107,11 +107,11 @@ public final class BulgarianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/br/BrazilianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/br/BrazilianAnalyzer.java
index 373421da068..2ba53153998 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/br/BrazilianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/br/BrazilianAnalyzer.java
@@ -117,10 +117,10 @@ public final class BrazilianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link StandardTokenizer} filtered with
    *         {@link LowerCaseFilter}, {@link StandardFilter}, {@link StopFilter}
    *         , and {@link BrazilianStemFilter}.
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/ca/CatalanAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/ca/CatalanAnalyzer.java
index 34b9b0a502b..eaaed17030b 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/ca/CatalanAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/ca/CatalanAnalyzer.java
@@ -105,11 +105,11 @@ public final class CatalanAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/cn/ChineseAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/cn/ChineseAnalyzer.java
index 4bd985c3351..886f5e77ebb 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/cn/ChineseAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/cn/ChineseAnalyzer.java
@@ -20,7 +20,6 @@ package org.apache.lucene.analysis.cn;
 import java.io.Reader;
 
 import org.apache.lucene.analysis.standard.StandardAnalyzer; // javadoc @link
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.Tokenizer;
 
@@ -31,14 +30,14 @@ import org.apache.lucene.analysis.Tokenizer;
  * This analyzer will be removed in Lucene 5.0
  */
 @Deprecated
public final class ChineseAnalyzer extends ReusableAnalyzerBase {
public final class ChineseAnalyzer extends Analyzer {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link ChineseTokenizer} filtered with
    *         {@link ChineseFilter}
    */
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/core/KeywordAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/core/KeywordAnalyzer.java
index c61f0957a99..5bf37e32988 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/core/KeywordAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/core/KeywordAnalyzer.java
@@ -19,13 +19,13 @@ package org.apache.lucene.analysis.core;
 
 import java.io.Reader;
 
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.Analyzer;
 
 /**
  * "Tokenizes" the entire stream as a single token. This is useful
  * for data like zip codes, ids, and some product names.
  */
public final class KeywordAnalyzer extends ReusableAnalyzerBase {
public final class KeywordAnalyzer extends Analyzer {
   public KeywordAnalyzer() {
   }
 
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/core/SimpleAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/core/SimpleAnalyzer.java
index 357c9861e44..a458626317d 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/core/SimpleAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/core/SimpleAnalyzer.java
@@ -21,7 +21,6 @@ import java.io.Reader;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.Version;
 
 /** An {@link Analyzer} that filters {@link LetterTokenizer} 
@@ -36,7 +35,7 @@ import org.apache.lucene.util.Version;
  * </ul>
  * <p>
  **/
public final class SimpleAnalyzer extends ReusableAnalyzerBase {
public final class SimpleAnalyzer extends Analyzer {
 
   private final Version matchVersion;
   
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java
index 1420896d385..75fb8c4c3aa 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java
@@ -95,10 +95,10 @@ public final class StopAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link LowerCaseTokenizer} filtered with
    *         {@link StopFilter}
    */
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/core/WhitespaceAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/core/WhitespaceAnalyzer.java
index 496abd929e9..e22952d5153 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/core/WhitespaceAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/core/WhitespaceAnalyzer.java
@@ -19,8 +19,8 @@ package org.apache.lucene.analysis.core;
 
 import java.io.Reader;
 
import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.Version;
 
 /**
@@ -35,7 +35,7 @@ import org.apache.lucene.util.Version;
  * </ul>
  * <p>
  **/
public final class WhitespaceAnalyzer extends ReusableAnalyzerBase {
public final class WhitespaceAnalyzer extends Analyzer {
   
   private final Version matchVersion;
   
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/cz/CzechAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/cz/CzechAnalyzer.java
index 87a05170e26..0df03a1ed93 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/cz/CzechAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/cz/CzechAnalyzer.java
@@ -26,7 +26,6 @@ import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.standard.StandardFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
 import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
 import org.apache.lucene.analysis.util.WordlistLoader;
 import org.apache.lucene.util.Version;
@@ -122,10 +121,10 @@ public final class CzechAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , and {@link CzechStemFilter} (only if version is >= LUCENE_31). If
@@ -135,7 +134,7 @@ public final class CzechAnalyzer extends StopwordAnalyzerBase {
    *         {@link CzechStemFilter}.
    */
   @Override
  protected ReusableAnalyzerBase.TokenStreamComponents createComponents(String fieldName,
  protected TokenStreamComponents createComponents(String fieldName,
       Reader reader) {
     final Tokenizer source = new StandardTokenizer(matchVersion, reader);
     TokenStream result = new StandardFilter(matchVersion, source);
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/da/DanishAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/da/DanishAnalyzer.java
index a5c048a126e..65505dca4e9 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/da/DanishAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/da/DanishAnalyzer.java
@@ -106,11 +106,11 @@ public final class DanishAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/de/GermanAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/de/GermanAnalyzer.java
index 9a0aba0d4b5..2c69900daad 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/de/GermanAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/de/GermanAnalyzer.java
@@ -158,10 +158,10 @@ public final class GermanAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/el/GreekAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/el/GreekAnalyzer.java
index 5a1d9867706..8cbd82931e0 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/el/GreekAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/el/GreekAnalyzer.java
@@ -101,10 +101,10 @@ public final class GreekAnalyzer extends StopwordAnalyzerBase {
   
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link StandardTokenizer} filtered with
    *         {@link GreekLowerCaseFilter}, {@link StandardFilter},
    *         {@link StopFilter}, and {@link GreekStemFilter}
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java
index 8d0f6cf977d..6e71e40af86 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java
@@ -89,11 +89,11 @@ public final class EnglishAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/es/SpanishAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/es/SpanishAnalyzer.java
index a4e33c26be5..025415d9422 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/es/SpanishAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/es/SpanishAnalyzer.java
@@ -106,11 +106,11 @@ public final class SpanishAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/eu/BasqueAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/eu/BasqueAnalyzer.java
index a878182d8d6..9ed380823fa 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/eu/BasqueAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/eu/BasqueAnalyzer.java
@@ -105,11 +105,11 @@ public final class BasqueAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/fa/PersianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/fa/PersianAnalyzer.java
index f441c525f48..efdbd2e1010 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/fa/PersianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/fa/PersianAnalyzer.java
@@ -107,10 +107,10 @@ public final class PersianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link StandardTokenizer} filtered with
    *         {@link LowerCaseFilter}, {@link ArabicNormalizationFilter},
    *         {@link PersianNormalizationFilter} and Persian Stop words
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/fi/FinnishAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/fi/FinnishAnalyzer.java
index 575d460a31e..85a0e595146 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/fi/FinnishAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/fi/FinnishAnalyzer.java
@@ -106,11 +106,11 @@ public final class FinnishAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/fr/FrenchAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/fr/FrenchAnalyzer.java
index 4276aa2e771..087f6a104e8 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/fr/FrenchAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/fr/FrenchAnalyzer.java
@@ -168,10 +168,10 @@ public final class FrenchAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link ElisionFilter},
    *         {@link LowerCaseFilter}, {@link StopFilter},
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/gl/GalicianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/gl/GalicianAnalyzer.java
index 5af84635130..60dc7c3a6d2 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/gl/GalicianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/gl/GalicianAnalyzer.java
@@ -104,11 +104,11 @@ public final class GalicianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/hi/HindiAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/hi/HindiAnalyzer.java
index fbdf5d77759..ba326623b84 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/hi/HindiAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/hi/HindiAnalyzer.java
@@ -106,10 +106,10 @@ public final class HindiAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link IndicTokenizer} filtered with
    *         {@link LowerCaseFilter}, {@link IndicNormalizationFilter},
    *         {@link HindiNormalizationFilter}, {@link KeywordMarkerFilter}
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/hu/HungarianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/hu/HungarianAnalyzer.java
index 39fdb7110a2..be3a8794782 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/hu/HungarianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/hu/HungarianAnalyzer.java
@@ -106,11 +106,11 @@ public final class HungarianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/hy/ArmenianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/hy/ArmenianAnalyzer.java
index d43096a3574..76983deeba5 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/hy/ArmenianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/hy/ArmenianAnalyzer.java
@@ -105,11 +105,11 @@ public final class ArmenianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/id/IndonesianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/id/IndonesianAnalyzer.java
index 877b5fe2f00..dfea4042b4b 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/id/IndonesianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/id/IndonesianAnalyzer.java
@@ -106,10 +106,10 @@ public final class IndonesianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter},
    *         {@link StopFilter}, {@link KeywordMarkerFilter}
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/it/ItalianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/it/ItalianAnalyzer.java
index 797902a093b..22790bb3e19 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/it/ItalianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/it/ItalianAnalyzer.java
@@ -123,11 +123,11 @@ public final class ItalianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link ElisionFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/lv/LatvianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/lv/LatvianAnalyzer.java
index 2bcf036a376..d0ff1e10323 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/lv/LatvianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/lv/LatvianAnalyzer.java
@@ -104,11 +104,11 @@ public final class LatvianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/LimitTokenCountAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/LimitTokenCountAnalyzer.java
index 433ad96e154..a52ac5f2d6d 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/LimitTokenCountAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/LimitTokenCountAnalyzer.java
@@ -18,17 +18,13 @@ package org.apache.lucene.analysis.miscellaneous;
  */
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexableField;

import java.io.Reader;
import java.io.IOException;
import org.apache.lucene.analysis.AnalyzerWrapper;
 
 /**
  * This Analyzer limits the number of tokens while indexing. It is
  * a replacement for the maximum field length setting inside {@link org.apache.lucene.index.IndexWriter}.
  */
public final class LimitTokenCountAnalyzer extends Analyzer {
public final class LimitTokenCountAnalyzer extends AnalyzerWrapper {
   private final Analyzer delegate;
   private final int maxTokenCount;
 
@@ -39,29 +35,16 @@ public final class LimitTokenCountAnalyzer extends Analyzer {
     this.delegate = delegate;
     this.maxTokenCount = maxTokenCount;
   }
  
  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    return new LimitTokenCountFilter(
      delegate.tokenStream(fieldName, reader), maxTokenCount
    );
  }
  
  @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    return new LimitTokenCountFilter(
      delegate.reusableTokenStream(fieldName, reader), maxTokenCount
    );
  }
  

   @Override
  public int getPositionIncrementGap(String fieldName) {
    return delegate.getPositionIncrementGap(fieldName);
  protected Analyzer getWrappedAnalyzer(String fieldName) {
    return delegate;
   }
 
   @Override
  public int getOffsetGap(IndexableField field) {
    return delegate.getOffsetGap(field);
  protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
    return new TokenStreamComponents(components.getTokenizer(),
        new LimitTokenCountFilter(components.getTokenStream(), maxTokenCount));
   }
   
   @Override
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PatternAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PatternAnalyzer.java
index 4c12b31afb6..daf70773ece 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PatternAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PatternAnalyzer.java
@@ -27,7 +27,6 @@ import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.StopAnalyzer;
@@ -67,7 +66,7 @@ import org.apache.lucene.util.Version;
  * @deprecated (4.0) use the pattern-based analysis in the analysis/pattern package instead.
  */
 @Deprecated
public final class PatternAnalyzer extends ReusableAnalyzerBase {
public final class PatternAnalyzer extends Analyzer {
   
   /** <code>"\\W+"</code>; Divides text at non-letters (NOT Character.isLetter(c)) */
   public static final Pattern NON_WORD_PATTERN = Pattern.compile("\\W+");
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PerFieldAnalyzerWrapper.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PerFieldAnalyzerWrapper.java
index 08ec36ac17a..514f211d84f 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PerFieldAnalyzerWrapper.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PerFieldAnalyzerWrapper.java
@@ -18,14 +18,10 @@ package org.apache.lucene.analysis.miscellaneous;
  */
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.analysis.AnalyzerWrapper;
 
import java.io.Reader;
import java.io.IOException;
 import java.util.Collections;
 import java.util.Map;
import java.util.HashMap;
 
 /**
  * This analyzer is used to facilitate scenarios where different
@@ -50,7 +46,7 @@ import java.util.HashMap;
  * <p>A PerFieldAnalyzerWrapper can be used like any other analyzer, for both indexing
  * and query parsing.
  */
public final class PerFieldAnalyzerWrapper extends Analyzer {
public final class PerFieldAnalyzerWrapper extends AnalyzerWrapper {
   private final Analyzer defaultAnalyzer;
   private final Map<String, Analyzer> fieldAnalyzers;
 
@@ -74,47 +70,20 @@ public final class PerFieldAnalyzerWrapper extends Analyzer {
    * used for those fields 
    */
   public PerFieldAnalyzerWrapper(Analyzer defaultAnalyzer,
      Map<String,Analyzer> fieldAnalyzers) {
      Map<String, Analyzer> fieldAnalyzers) {
     this.defaultAnalyzer = defaultAnalyzer;
     this.fieldAnalyzers = (fieldAnalyzers != null) ? fieldAnalyzers : Collections.<String, Analyzer>emptyMap();
   }
 
   @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
  protected Analyzer getWrappedAnalyzer(String fieldName) {
     Analyzer analyzer = fieldAnalyzers.get(fieldName);
    if (analyzer == null) {
      analyzer = defaultAnalyzer;
    }

    return analyzer.tokenStream(fieldName, reader);
  }
  
  @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    Analyzer analyzer = fieldAnalyzers.get(fieldName);
    if (analyzer == null)
      analyzer = defaultAnalyzer;

    return analyzer.reusableTokenStream(fieldName, reader);
  }
  
  /** Return the positionIncrementGap from the analyzer assigned to fieldName */
  @Override
  public int getPositionIncrementGap(String fieldName) {
    Analyzer analyzer = fieldAnalyzers.get(fieldName);
    if (analyzer == null)
      analyzer = defaultAnalyzer;
    return analyzer.getPositionIncrementGap(fieldName);
    return (analyzer != null) ? analyzer : defaultAnalyzer;
   }
 
  /** Return the offsetGap from the analyzer assigned to field */
   @Override
  public int getOffsetGap(IndexableField field) {
    Analyzer analyzer = fieldAnalyzers.get(field.name());
    if (analyzer == null) {
      analyzer = defaultAnalyzer;
    }
    return analyzer.getOffsetGap(field);
  protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
    return components;
   }
   
   @Override
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/nl/DutchAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/nl/DutchAnalyzer.java
index fcf4d033b14..3931fa107c2 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/nl/DutchAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/nl/DutchAnalyzer.java
@@ -29,7 +29,6 @@ import org.apache.lucene.analysis.standard.StandardFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;  // for javadoc
 import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.util.WordlistLoader;
 import org.apache.lucene.util.Version;
 
@@ -66,7 +65,7 @@ import java.util.Map;
  * <p><b>NOTE</b>: This class uses the same {@link Version}
  * dependent settings as {@link StandardAnalyzer}.</p>
  */
public final class DutchAnalyzer extends ReusableAnalyzerBase {
public final class DutchAnalyzer extends Analyzer {
   
   /** File containing default Dutch stopwords. */
   public final static String DEFAULT_STOPWORD_FILE = "dutch_stop.txt";
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/no/NorwegianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/no/NorwegianAnalyzer.java
index b9dd5cc3bba..ecb66f6c8b2 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/no/NorwegianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/no/NorwegianAnalyzer.java
@@ -106,11 +106,11 @@ public final class NorwegianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/pt/PortugueseAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/pt/PortugueseAnalyzer.java
index b50acdb3130..3d2893313ba 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/pt/PortugueseAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/pt/PortugueseAnalyzer.java
@@ -106,11 +106,11 @@ public final class PortugueseAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/query/QueryAutoStopWordAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/query/QueryAutoStopWordAnalyzer.java
index 066b77419ed..791e994266e 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/query/QueryAutoStopWordAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/query/QueryAutoStopWordAnalyzer.java
@@ -16,20 +16,19 @@ package org.apache.lucene.analysis.query;
  * limitations under the License.
  */
 
import org.apache.lucene.analysis.AnalyzerWrapper;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.core.StopFilter;
 import org.apache.lucene.util.CharsRef;
 import org.apache.lucene.util.Version;
 import org.apache.lucene.util.BytesRef;
 
 import java.io.IOException;
import java.io.Reader;
 import java.util.*;
 
 /**
@@ -42,7 +41,7 @@ import java.util.*;
  * this term to take 2 seconds.
  * </p>
  */
public final class QueryAutoStopWordAnalyzer extends Analyzer {
public final class QueryAutoStopWordAnalyzer extends AnalyzerWrapper {
 
   private final Analyzer delegate;
   private final Map<String, Set<String>> stopWordsPerField = new HashMap<String, Set<String>>();
@@ -101,7 +100,7 @@ public final class QueryAutoStopWordAnalyzer extends Analyzer {
    */
   public QueryAutoStopWordAnalyzer(
       Version matchVersion,
      Analyzer delegate, 
      Analyzer delegate,
       IndexReader indexReader,
       float maxPercentDocs) throws IOException {
     this(matchVersion, delegate, indexReader, indexReader.getFieldNames(IndexReader.FieldOption.INDEXED), maxPercentDocs);
@@ -168,79 +167,18 @@ public final class QueryAutoStopWordAnalyzer extends Analyzer {
   }
 
   @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    TokenStream result;
    try {
      result = delegate.reusableTokenStream(fieldName, reader);
    } catch (IOException e) {
      result = delegate.tokenStream(fieldName, reader);
    }
    Set<String> stopWords = stopWordsPerField.get(fieldName);
    if (stopWords != null) {
      result = new StopFilter(matchVersion, result, stopWords);
    }
    return result;
  protected Analyzer getWrappedAnalyzer(String fieldName) {
    return delegate;
   }
  
  private class SavedStreams {
    /* the underlying stream */
    TokenStream wrapped;
 
    /*
     * when there are no stopwords for the field, refers to wrapped.
     * if there stopwords, it is a StopFilter around wrapped.
     */
    TokenStream withStopFilter;
  }

  @SuppressWarnings("unchecked")
   @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    /* map of SavedStreams for each field */
    Map<String,SavedStreams> streamMap = (Map<String,SavedStreams>) getPreviousTokenStream();
    if (streamMap == null) {
      streamMap = new HashMap<String, SavedStreams>();
      setPreviousTokenStream(streamMap);
    }

    SavedStreams streams = streamMap.get(fieldName);
    if (streams == null) {
      /* an entry for this field does not exist, create one */
      streams = new SavedStreams();
      streamMap.put(fieldName, streams);
      streams.wrapped = delegate.reusableTokenStream(fieldName, reader);

      /* if there are any stopwords for the field, save the stopfilter */
      Set<String> stopWords = stopWordsPerField.get(fieldName);
      if (stopWords != null) {
        streams.withStopFilter = new StopFilter(matchVersion, streams.wrapped, stopWords);
      } else {
        streams.withStopFilter = streams.wrapped;
      }
    } else {
      /*
      * an entry for this field exists, verify the wrapped stream has not
      * changed. if it has not, reuse it, otherwise wrap the new stream.
      */
      TokenStream result = delegate.reusableTokenStream(fieldName, reader);
      if (result == streams.wrapped) {
        /* the wrapped analyzer reused the stream */
      } else {
        /*
        * the wrapped analyzer did not. if there are any stopwords for the
        * field, create a new StopFilter around the new stream
        */
        streams.wrapped = result;
        Set<String> stopWords = stopWordsPerField.get(fieldName);
        if (stopWords != null) {
          streams.withStopFilter = new StopFilter(matchVersion, streams.wrapped, stopWords);
        } else {
          streams.withStopFilter = streams.wrapped;
        }
      }
  protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
    Set<String> stopWords = stopWordsPerField.get(fieldName);
    if (stopWords == null) {
      return components;
     }

    return streams.withStopFilter;
    StopFilter stopFilter = new StopFilter(matchVersion, components.getTokenStream(), stopWords);
    return new TokenStreamComponents(components.getTokenizer(), stopFilter);
   }
 
   /**
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/ro/RomanianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/ro/RomanianAnalyzer.java
index ef07e98e1e0..1d3c40d6dbe 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/ro/RomanianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/ro/RomanianAnalyzer.java
@@ -110,11 +110,11 @@ public final class RomanianAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/ru/RussianAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/ru/RussianAnalyzer.java
index ea19bb9f8df..6ddf665a578 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/ru/RussianAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/ru/RussianAnalyzer.java
@@ -139,10 +139,10 @@ public final class RussianAnalyzer extends StopwordAnalyzerBase
    
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/shingle/ShingleAnalyzerWrapper.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/shingle/ShingleAnalyzerWrapper.java
index 217a3622b51..2e41a146893 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/shingle/ShingleAnalyzerWrapper.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/shingle/ShingleAnalyzerWrapper.java
@@ -17,11 +17,8 @@ package org.apache.lucene.analysis.shingle;
  * limitations under the License.
  */
 
import java.io.IOException;
import java.io.Reader;

 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.AnalyzerWrapper;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.util.Version;
 
@@ -31,7 +28,7 @@ import org.apache.lucene.util.Version;
  * A shingle is another name for a token based n-gram.
  * </p>
  */
public final class ShingleAnalyzerWrapper extends Analyzer {
public final class ShingleAnalyzerWrapper extends AnalyzerWrapper {
 
   private final Analyzer defaultAnalyzer;
   private final int maxShingleSize;
@@ -140,48 +137,18 @@ public final class ShingleAnalyzerWrapper extends Analyzer {
   }
 
   @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    TokenStream wrapped;
    try {
      wrapped = defaultAnalyzer.reusableTokenStream(fieldName, reader);
    } catch (IOException e) {
      wrapped = defaultAnalyzer.tokenStream(fieldName, reader);
    }
    ShingleFilter filter = new ShingleFilter(wrapped, minShingleSize, maxShingleSize);
  protected Analyzer getWrappedAnalyzer(String fieldName) {
    return defaultAnalyzer;
  }

  @Override
  protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
    ShingleFilter filter = new ShingleFilter(components.getTokenStream(), minShingleSize, maxShingleSize);
     filter.setMinShingleSize(minShingleSize);
     filter.setMaxShingleSize(maxShingleSize);
     filter.setTokenSeparator(tokenSeparator);
     filter.setOutputUnigrams(outputUnigrams);
     filter.setOutputUnigramsIfNoShingles(outputUnigramsIfNoShingles);
    return filter;
  }
  
  private class SavedStreams {
    TokenStream wrapped;
    ShingleFilter shingle;
  }
  
  @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    SavedStreams streams = (SavedStreams) getPreviousTokenStream();
    if (streams == null) {
      streams = new SavedStreams();
      streams.wrapped = defaultAnalyzer.reusableTokenStream(fieldName, reader);
      streams.shingle = new ShingleFilter(streams.wrapped);
      setPreviousTokenStream(streams);
    } else {
      TokenStream result = defaultAnalyzer.reusableTokenStream(fieldName, reader);
      if (result != streams.wrapped) {
        /* the wrapped analyzer did not, create a new shingle around the new one */
        streams.wrapped = result;
        streams.shingle = new ShingleFilter(streams.wrapped);
      }
    }
    streams.shingle.setMaxShingleSize(maxShingleSize);
    streams.shingle.setMinShingleSize(minShingleSize);
    streams.shingle.setTokenSeparator(tokenSeparator);
    streams.shingle.setOutputUnigrams(outputUnigrams);
    streams.shingle.setOutputUnigramsIfNoShingles(outputUnigramsIfNoShingles);
    return streams.shingle;
    return new TokenStreamComponents(components.getTokenizer(), filter);
   }
 }
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/snowball/SnowballAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/snowball/SnowballAnalyzer.java
index 1233da835c2..7a6c710d849 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/snowball/SnowballAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/snowball/SnowballAnalyzer.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
 import org.apache.lucene.analysis.standard.*;
 import org.apache.lucene.analysis.tr.TurkishLowerCaseFilter;
 import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.Version;
 
 import java.io.Reader;
@@ -47,7 +46,7 @@ import java.util.Set;
  * This analyzer will be removed in Lucene 5.0
  */
 @Deprecated
public final class SnowballAnalyzer extends ReusableAnalyzerBase {
public final class SnowballAnalyzer extends Analyzer {
   private String name;
   private Set<?> stopSet;
   private final Version matchVersion;
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/sv/SwedishAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/sv/SwedishAnalyzer.java
index bc012dda195..7dd1702cde5 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/sv/SwedishAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/sv/SwedishAnalyzer.java
@@ -106,11 +106,11 @@ public final class SwedishAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/th/ThaiAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/th/ThaiAnalyzer.java
index fcf10cf4db2..979339ff9da 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/th/ThaiAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/th/ThaiAnalyzer.java
@@ -27,7 +27,6 @@ import org.apache.lucene.analysis.core.StopFilter;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.analysis.standard.StandardFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.Version;
 
 /**
@@ -36,7 +35,7 @@ import org.apache.lucene.util.Version;
  * <p><b>NOTE</b>: This class uses the same {@link Version}
  * dependent settings as {@link StandardAnalyzer}.</p>
  */
public final class ThaiAnalyzer extends ReusableAnalyzerBase {
public final class ThaiAnalyzer extends Analyzer {
   private final Version matchVersion;
 
   public ThaiAnalyzer(Version matchVersion) {
@@ -45,10 +44,10 @@ public final class ThaiAnalyzer extends ReusableAnalyzerBase {
 
   /**
    * Creates
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * used to tokenize all the text in the provided {@link Reader}.
    * 
   * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from a {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link ThaiWordFilter}, and
    *         {@link StopFilter}
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/tr/TurkishAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/tr/TurkishAnalyzer.java
index b7868490c38..e74732494ea 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/tr/TurkishAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/tr/TurkishAnalyzer.java
@@ -109,11 +109,11 @@ public final class TurkishAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link TurkishLowerCaseFilter},
    *         {@link StopFilter}, {@link KeywordMarkerFilter} if a stem
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/util/StopwordAnalyzerBase.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/util/StopwordAnalyzerBase.java
index 4daa59d76c0..c99dc54e092 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/util/StopwordAnalyzerBase.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/util/StopwordAnalyzerBase.java
@@ -20,15 +20,14 @@ package org.apache.lucene.analysis.util;
 import java.io.IOException;
 import java.util.Set;
 
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.util.Version;
 
 /**
  * Base class for Analyzers that need to make use of stopword sets. 
  * 
  */
public abstract class StopwordAnalyzerBase extends ReusableAnalyzerBase {
public abstract class StopwordAnalyzerBase extends Analyzer {
 
   /**
    * An immutable stopword set
@@ -92,7 +91,7 @@ public abstract class StopwordAnalyzerBase extends ReusableAnalyzerBase {
    *           if loading the stopwords throws an {@link IOException}
    */
   protected static CharArraySet loadStopwordSet(final boolean ignoreCase,
      final Class<? extends ReusableAnalyzerBase> aClass, final String resource,
      final Class<? extends Analyzer> aClass, final String resource,
       final String comment) throws IOException {
     final Set<String> wordSet = WordlistLoader.getWordSet(aClass, resource,
         comment);
diff --git a/modules/analysis/common/src/java/org/apache/lucene/collation/CollationKeyAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/collation/CollationKeyAnalyzer.java
index 7e75d18aaed..97b6a3f897f 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/collation/CollationKeyAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/collation/CollationKeyAnalyzer.java
@@ -18,8 +18,8 @@ package org.apache.lucene.collation;
  */
 
 
import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.IndexableBinaryStringTools; // javadoc @link
 import org.apache.lucene.util.Version;
 
@@ -82,7 +82,7 @@ import java.io.Reader;
  *   versions will encode the bytes with {@link IndexableBinaryStringTools}.
  * </ul>
  */
public final class CollationKeyAnalyzer extends ReusableAnalyzerBase {
public final class CollationKeyAnalyzer extends Analyzer {
   private final Collator collator;
   private final CollationAttributeFactory factory;
   private final Version matchVersion;
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/cn/TestChineseTokenizer.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/cn/TestChineseTokenizer.java
index 3cec2995305..15ff5a08fe3 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/cn/TestChineseTokenizer.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/cn/TestChineseTokenizer.java
@@ -64,7 +64,7 @@ public class TestChineseTokenizer extends BaseTokenStreamTestCase
      * Analyzer that just uses ChineseTokenizer, not ChineseFilter.
      * convenience to show the behavior of the tokenizer
      */
    private class JustChineseTokenizerAnalyzer extends ReusableAnalyzerBase {
    private class JustChineseTokenizerAnalyzer extends Analyzer {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         return new TokenStreamComponents(new ChineseTokenizer(reader));
@@ -75,7 +75,7 @@ public class TestChineseTokenizer extends BaseTokenStreamTestCase
      * Analyzer that just uses ChineseFilter, not ChineseTokenizer.
      * convenience to show the behavior of the filter.
      */
    private class JustChineseFilterAnalyzer extends ReusableAnalyzerBase {
    private class JustChineseFilterAnalyzer extends Analyzer {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_CURRENT, reader);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/commongrams/CommonGramsFilterTest.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/commongrams/CommonGramsFilterTest.java
index 2119fc4a909..8bdbf9aa7a7 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/commongrams/CommonGramsFilterTest.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/commongrams/CommonGramsFilterTest.java
@@ -84,7 +84,7 @@ public class CommonGramsFilterTest extends BaseTokenStreamTestCase {
    * @return Map<String,String>
    */
   public void testCommonGramsQueryFilter() throws Exception {
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String field, Reader in) {
         Tokenizer tokenizer = new MockTokenizer(in, MockTokenizer.WHITESPACE, false);
@@ -154,7 +154,7 @@ public class CommonGramsFilterTest extends BaseTokenStreamTestCase {
   }
   
   public void testCommonGramsFilter() throws Exception {
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String field, Reader in) {
         Tokenizer tokenizer = new MockTokenizer(in, MockTokenizer.WHITESPACE, false);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java
index b3fed982ead..3f3974da8f8 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java
@@ -117,7 +117,7 @@ public class TestAnalyzers extends BaseTokenStreamTestCase {
     String[] y = StandardTokenizer.TOKEN_TYPES;
   }
 
  private static class LowerCaseWhitespaceAnalyzer extends ReusableAnalyzerBase {
  private static class LowerCaseWhitespaceAnalyzer extends Analyzer {
 
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestStandardAnalyzer.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestStandardAnalyzer.java
index 08b49ef9b94..fa1a638bba6 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestStandardAnalyzer.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestStandardAnalyzer.java
@@ -5,7 +5,6 @@ import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.Version;
 
 import java.io.IOException;
@@ -43,7 +42,7 @@ public class TestStandardAnalyzer extends BaseTokenStreamTestCase {
     BaseTokenStreamTestCase.assertTokenStreamContents(tokenizer, new String[] { "testing", "1234" });
   }
 
  private Analyzer a = new ReusableAnalyzerBase() {
  private Analyzer a = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents
       (String fieldName, Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestUAX29URLEmailTokenizer.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestUAX29URLEmailTokenizer.java
index 4a5002c93ea..1af100c4098 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestUAX29URLEmailTokenizer.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestUAX29URLEmailTokenizer.java
@@ -8,7 +8,6 @@ import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
 import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.Version;
 
 import java.io.BufferedReader;
@@ -50,7 +49,7 @@ public class TestUAX29URLEmailTokenizer extends BaseTokenStreamTestCase {
     BaseTokenStreamTestCase.assertTokenStreamContents(tokenizer, new String[] { "testing", "1234" });
   }
 
  private Analyzer a = new ReusableAnalyzerBase() {
  private Analyzer a = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents
       (String fieldName, Reader reader) {
@@ -99,7 +98,7 @@ public class TestUAX29URLEmailTokenizer extends BaseTokenStreamTestCase {
     }
   }
 
  private Analyzer urlAnalyzer = new ReusableAnalyzerBase() {
  private Analyzer urlAnalyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
       UAX29URLEmailTokenizer tokenizer = new UAX29URLEmailTokenizer(TEST_VERSION_CURRENT, reader);
@@ -109,7 +108,7 @@ public class TestUAX29URLEmailTokenizer extends BaseTokenStreamTestCase {
     }
   };
 
  private Analyzer emailAnalyzer = new ReusableAnalyzerBase() {
  private Analyzer emailAnalyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
       UAX29URLEmailTokenizer tokenizer = new UAX29URLEmailTokenizer(TEST_VERSION_CURRENT, reader);
@@ -431,7 +430,7 @@ public class TestUAX29URLEmailTokenizer extends BaseTokenStreamTestCase {
   /** @deprecated remove this and sophisticated backwards layer in 5.0 */
   @Deprecated
   public void testCombiningMarksBackwards() throws Exception {
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents
         (String fieldName, Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanLightStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanLightStemFilter.java
index 20aab6b2b00..3c3528d6a9f 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanLightStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanLightStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link GermanLightStemFilter}
  */
 public class TestGermanLightStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanMinimalStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanMinimalStemFilter.java
index 521721fc074..cc1a669e341 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanMinimalStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanMinimalStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link GermanMinimalStemFilter}
  */
 public class TestGermanMinimalStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanStemFilter.java
index 5d3e6c6673a..27e9a846338 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanStemFilter.java
@@ -25,7 +25,6 @@ import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
 import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -36,7 +35,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  *
  */
 public class TestGermanStemFilter extends BaseTokenStreamTestCase {
  Analyzer analyzer = new ReusableAnalyzerBase() {
  Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestEnglishMinimalStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestEnglishMinimalStemFilter.java
index 74004805c18..43c269627e6 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestEnglishMinimalStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestEnglishMinimalStemFilter.java
@@ -24,13 +24,12 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 /**
  * Simple tests for {@link EnglishMinimalStemFilter}
  */
 public class TestEnglishMinimalStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestKStemmer.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestKStemmer.java
index 7b76ecb176f..3449f81e85a 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestKStemmer.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestKStemmer.java
@@ -25,13 +25,12 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 /**
  * Tests for {@link KStemmer}
  */
 public class TestKStemmer extends BaseTokenStreamTestCase {
  Analyzer a = new ReusableAnalyzerBase() {
  Analyzer a = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestPorterStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestPorterStemFilter.java
index 20a6c0fecab..0aec8d6771c 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestPorterStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/en/TestPorterStemFilter.java
@@ -24,7 +24,6 @@ import java.io.StringReader;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter;
 import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenStream;
@@ -36,7 +35,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Test the PorterStemFilter with Martin Porter's test data.
  */
 public class TestPorterStemFilter extends BaseTokenStreamTestCase {
  Analyzer a = new ReusableAnalyzerBase() {
  Analyzer a = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/es/TestSpanishLightStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/es/TestSpanishLightStemFilter.java
index aa05f8c6758..daaca467161 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/es/TestSpanishLightStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/es/TestSpanishLightStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link SpanishLightStemFilter}
  */
 public class TestSpanishLightStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/fi/TestFinnishLightStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/fi/TestFinnishLightStemFilter.java
index f604bab45d3..5cd64550441 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/fi/TestFinnishLightStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/fi/TestFinnishLightStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link FinnishLightStemFilter}
  */
 public class TestFinnishLightStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchLightStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchLightStemFilter.java
index 3b477b87b58..57eb8adb782 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchLightStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchLightStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link FrenchLightStemFilter}
  */
 public class TestFrenchLightStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchMinimalStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchMinimalStemFilter.java
index 4e812163692..e6fb11fb618 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchMinimalStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchMinimalStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link FrenchMinimalStemFilter}
  */
 public class TestFrenchMinimalStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/gl/TestGalicianStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/gl/TestGalicianStemFilter.java
index 17056b1021f..c48b3412742 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/gl/TestGalicianStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/gl/TestGalicianStemFilter.java
@@ -28,13 +28,12 @@ import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.LowerCaseFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 /**
  * Simple tests for {@link GalicianStemFilter}
  */
 public class TestGalicianStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/hu/TestHungarianLightStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/hu/TestHungarianLightStemFilter.java
index 7237bcf9752..90e4768db31 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/hu/TestHungarianLightStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/hu/TestHungarianLightStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link HungarianLightStemFilter}
  */
 public class TestHungarianLightStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/id/TestIndonesianStemmer.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/id/TestIndonesianStemmer.java
index cec4fa45688..2d9f832dc23 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/id/TestIndonesianStemmer.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/id/TestIndonesianStemmer.java
@@ -24,14 +24,13 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 /**
  * Tests {@link IndonesianStemmer}
  */
 public class TestIndonesianStemmer extends BaseTokenStreamTestCase {
   /* full stemming, no stopwords */
  Analyzer a = new ReusableAnalyzerBase() {
  Analyzer a = new Analyzer() {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer tokenizer = new KeywordTokenizer(reader);
@@ -112,7 +111,7 @@ public class TestIndonesianStemmer extends BaseTokenStreamTestCase {
   }
   
   /* inflectional-only stemming */
  Analyzer b = new ReusableAnalyzerBase() {
  Analyzer b = new Analyzer() {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer tokenizer = new KeywordTokenizer(reader);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/it/TestItalianLightStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/it/TestItalianLightStemFilter.java
index ac1c77f4d36..90f96168dff 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/it/TestItalianLightStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/it/TestItalianLightStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link ItalianLightStemFilter}
  */
 public class TestItalianLightStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/lv/TestLatvianStemmer.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/lv/TestLatvianStemmer.java
index d3a6b1dc35f..68201d11b9e 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/lv/TestLatvianStemmer.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/lv/TestLatvianStemmer.java
@@ -24,13 +24,12 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 /**
  * Basic tests for {@link LatvianStemmer}
  */
 public class TestLatvianStemmer extends BaseTokenStreamTestCase {
  private Analyzer a = new ReusableAnalyzerBase() {
  private Analyzer a = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestWordDelimiterFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestWordDelimiterFilter.java
index 52191ac68ea..dc988b5df92 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestWordDelimiterFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestWordDelimiterFilter.java
@@ -213,7 +213,7 @@ public class TestWordDelimiterFilter extends BaseTokenStreamTestCase {
     final CharArraySet protWords = new CharArraySet(TEST_VERSION_CURRENT, new HashSet<String>(Arrays.asList("NUTCH")), false);
     
     /* analyzer that uses whitespace + wdf */
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String field, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -241,7 +241,7 @@ public class TestWordDelimiterFilter extends BaseTokenStreamTestCase {
         new int[] { 1, 1, 1 });
     
     /* analyzer that will consume tokens with large position increments */
    Analyzer a2 = new ReusableAnalyzerBase() {
    Analyzer a2 = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String field, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -274,7 +274,7 @@ public class TestWordDelimiterFilter extends BaseTokenStreamTestCase {
         new int[] { 6, 14, 19 },
         new int[] { 1, 11, 1 });
 
    Analyzer a3 = new ReusableAnalyzerBase() {
    Analyzer a3 = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String field, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseLightStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseLightStemFilter.java
index 96a13bcf48c..a5b6ec283da 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseLightStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseLightStemFilter.java
@@ -26,7 +26,6 @@ import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.LowerCaseFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -34,7 +33,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link PortugueseLightStemFilter}
  */
 public class TestPortugueseLightStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseMinimalStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseMinimalStemFilter.java
index 410b87e8f85..1e6afe843da 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseMinimalStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseMinimalStemFilter.java
@@ -26,7 +26,6 @@ import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.LowerCaseFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -34,7 +33,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link PortugueseMinimalStemFilter}
  */
 public class TestPortugueseMinimalStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseStemFilter.java
index 9ff23409a64..c71c8d6fbf0 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseStemFilter.java
@@ -28,13 +28,12 @@ import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.LowerCaseFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 /**
  * Simple tests for {@link PortugueseStemFilter}
  */
 public class TestPortugueseStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/ru/TestRussianLightStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/ru/TestRussianLightStemFilter.java
index 852093cc94e..015a7726810 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/ru/TestRussianLightStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/ru/TestRussianLightStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link RussianLightStemFilter}
  */
 public class TestRussianLightStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/snowball/TestSnowballVocab.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/snowball/TestSnowballVocab.java
index dcaff635305..ed7f2c729df 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/snowball/TestSnowballVocab.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/snowball/TestSnowballVocab.java
@@ -23,7 +23,6 @@ import java.io.Reader;
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.LuceneTestCase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
@@ -69,7 +68,7 @@ public class TestSnowballVocab extends LuceneTestCase {
       throws IOException {
     if (VERBOSE) System.out.println("checking snowball language: " + snowballLanguage);
     
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName,
           Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/sv/TestSwedishLightStemFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/sv/TestSwedishLightStemFilter.java
index 93b40ca627f..ae30b9be3cd 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/sv/TestSwedishLightStemFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/sv/TestSwedishLightStemFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import static org.apache.lucene.analysis.VocabularyAssert.*;
 
@@ -32,7 +31,7 @@ import static org.apache.lucene.analysis.VocabularyAssert.*;
  * Simple tests for {@link SwedishLightStemFilter}
  */
 public class TestSwedishLightStemFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new ReusableAnalyzerBase() {
  private Analyzer analyzer = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestSolrSynonymParser.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestSolrSynonymParser.java
index cd2362435f3..f958f013686 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestSolrSynonymParser.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestSolrSynonymParser.java
@@ -27,7 +27,6 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.junit.Test;
 
 /**
@@ -48,7 +47,7 @@ public class TestSolrSynonymParser extends BaseTokenStreamTestCase {
     parser.add(new StringReader(testFile));
     final SynonymMap map = parser.build();
     
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
@@ -121,7 +120,7 @@ public class TestSolrSynonymParser extends BaseTokenStreamTestCase {
     SolrSynonymParser parser = new SolrSynonymParser(true, true, new MockAnalyzer(random, MockTokenizer.KEYWORD, false));
     parser.add(new StringReader(testFile));
     final SynonymMap map = parser.build();
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.KEYWORD, false);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestSynonymMapFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestSynonymMapFilter.java
index ebfb00ff172..a49a8a0135c 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestSynonymMapFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestSynonymMapFilter.java
@@ -33,7 +33,6 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.CharsRef;
 import org.apache.lucene.util._TestUtil;
 
@@ -387,7 +386,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
       final SynonymMap map = b.build();
       final boolean ignoreCase = random.nextBoolean();
       
      final Analyzer analyzer = new ReusableAnalyzerBase() {
      final Analyzer analyzer = new Analyzer() {
         @Override
         protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
           Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
@@ -409,7 +408,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
     parser.add(new StringReader(testFile));
     final SynonymMap map = parser.build();
       
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
@@ -467,7 +466,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
     add("z x c v", "zxcv", keepOrig);
     add("x c", "xc", keepOrig);
     final SynonymMap map = b.build();
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -507,7 +506,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
     add("a b", "ab", keepOrig);
     add("a b", "ab", keepOrig);
     final SynonymMap map = b.build();
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -527,7 +526,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
     add("a b", "ab", keepOrig);
     add("a b", "ab", keepOrig);
     final SynonymMap map = b.build();
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -545,7 +544,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
     final boolean keepOrig = false;
     add("zoo", "zoo", keepOrig);
     final SynonymMap map = b.build();
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -564,7 +563,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
     add("zoo", "zoo", keepOrig);
     add("zoo", "zoo zoo", keepOrig);
     final SynonymMap map = b.build();
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -588,7 +587,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
     add("z x c v", "zxcv", keepOrig);
     add("x c", "xc", keepOrig);
     final SynonymMap map = b.build();
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -633,7 +632,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
     final boolean keepOrig = true;
     add("zoo zoo", "zoo", keepOrig);
     final SynonymMap map = b.build();
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
@@ -652,7 +651,7 @@ public class TestSynonymMapFilter extends BaseTokenStreamTestCase {
     add("zoo zoo", "zoo", keepOrig);
     add("zoo", "zoo zoo", keepOrig);
     final SynonymMap map = b.build();
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestWordnetSynonymParser.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestWordnetSynonymParser.java
index fc2e4b34974..ed3472ee521 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestWordnetSynonymParser.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/synonym/TestWordnetSynonymParser.java
@@ -25,7 +25,6 @@ import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 public class TestWordnetSynonymParser extends BaseTokenStreamTestCase {
   Analyzer analyzer;
@@ -46,7 +45,7 @@ public class TestWordnetSynonymParser extends BaseTokenStreamTestCase {
     parser.add(new StringReader(synonymsFile));
     final SynonymMap map = parser.build();
     
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/collation/TestCollationKeyFilter.java b/modules/analysis/common/src/test/org/apache/lucene/collation/TestCollationKeyFilter.java
index cb9c6e49214..929f04819e8 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/collation/TestCollationKeyFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/collation/TestCollationKeyFilter.java
@@ -52,7 +52,7 @@ public class TestCollationKeyFilter extends CollationTestBase {
     (collator.getCollationKey(secondRangeEndOriginal).toByteArray()));
 
   
  public final class TestAnalyzer extends ReusableAnalyzerBase {
  public final class TestAnalyzer extends Analyzer {
     private Collator _collator;
 
     TestAnalyzer(Collator collator) {
diff --git a/modules/analysis/icu/src/java/org/apache/lucene/collation/ICUCollationKeyAnalyzer.java b/modules/analysis/icu/src/java/org/apache/lucene/collation/ICUCollationKeyAnalyzer.java
index fc41cc8e546..95fafb428d4 100644
-- a/modules/analysis/icu/src/java/org/apache/lucene/collation/ICUCollationKeyAnalyzer.java
++ b/modules/analysis/icu/src/java/org/apache/lucene/collation/ICUCollationKeyAnalyzer.java
@@ -19,8 +19,8 @@ package org.apache.lucene.collation;
 
 
 import com.ibm.icu.text.Collator;
import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.collation.CollationKeyAnalyzer; // javadocs
 import org.apache.lucene.util.IndexableBinaryStringTools; // javadocs
 import org.apache.lucene.util.Version;
@@ -75,7 +75,7 @@ import java.io.Reader;
  *   versions will encode the bytes with {@link IndexableBinaryStringTools}.
  * </ul>
  */
public final class ICUCollationKeyAnalyzer extends ReusableAnalyzerBase {
public final class ICUCollationKeyAnalyzer extends Analyzer {
   private final Collator collator;
   private final ICUCollationAttributeFactory factory;
   private final Version matchVersion;
diff --git a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUFoldingFilter.java b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUFoldingFilter.java
index 9a632d6c280..dc0f26462e8 100644
-- a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUFoldingFilter.java
++ b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUFoldingFilter.java
@@ -27,7 +27,7 @@ import org.apache.lucene.analysis.core.WhitespaceTokenizer;
  * Tests ICUFoldingFilter
  */
 public class TestICUFoldingFilter extends BaseTokenStreamTestCase {
  Analyzer a = new ReusableAnalyzerBase() {
  Analyzer a = new Analyzer() {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer tokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader);
diff --git a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUNormalizer2Filter.java b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUNormalizer2Filter.java
index a7fbbaeb714..e81c02d6a6a 100644
-- a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUNormalizer2Filter.java
++ b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUNormalizer2Filter.java
@@ -29,7 +29,7 @@ import com.ibm.icu.text.Normalizer2;
  * Tests the ICUNormalizer2Filter
  */
 public class TestICUNormalizer2Filter extends BaseTokenStreamTestCase {
  Analyzer a = new ReusableAnalyzerBase() {
  Analyzer a = new Analyzer() {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer tokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader);
@@ -59,7 +59,7 @@ public class TestICUNormalizer2Filter extends BaseTokenStreamTestCase {
   }
   
   public void testAlternate() throws IOException {
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader);
diff --git a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUTransformFilter.java b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUTransformFilter.java
index c4905be10d8..4d766155d91 100644
-- a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUTransformFilter.java
++ b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUTransformFilter.java
@@ -26,7 +26,6 @@ import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
 import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.TokenStream;
 
 import com.ibm.icu.text.Transliterator;
@@ -92,7 +91,7 @@ public class TestICUTransformFilter extends BaseTokenStreamTestCase {
   /** blast some random strings through the analyzer */
   public void testRandomStrings() throws Exception {
     final Transliterator transform = Transliterator.getInstance("Any-Latin");
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader);
diff --git a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/segmentation/TestICUTokenizer.java b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/segmentation/TestICUTokenizer.java
index ab579f39dc5..c768e0fbf52 100644
-- a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/segmentation/TestICUTokenizer.java
++ b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/segmentation/TestICUTokenizer.java
@@ -22,7 +22,6 @@ import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.icu.ICUNormalizer2Filter;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 
 import java.io.IOException;
 import java.io.Reader;
@@ -61,7 +60,7 @@ public class TestICUTokenizer extends BaseTokenStreamTestCase {
     assertTokenStreamContents(tokenizer, expected);
   }
   
  private Analyzer a = new ReusableAnalyzerBase() {
  private Analyzer a = new Analyzer() {
     @Override
     protected TokenStreamComponents createComponents(String fieldName,
         Reader reader) {
diff --git a/modules/analysis/icu/src/test/org/apache/lucene/collation/TestICUCollationKeyFilter.java b/modules/analysis/icu/src/test/org/apache/lucene/collation/TestICUCollationKeyFilter.java
index a513bba3b4f..f1a038ac6ee 100644
-- a/modules/analysis/icu/src/test/org/apache/lucene/collation/TestICUCollationKeyFilter.java
++ b/modules/analysis/icu/src/test/org/apache/lucene/collation/TestICUCollationKeyFilter.java
@@ -44,7 +44,7 @@ public class TestICUCollationKeyFilter extends CollationTestBase {
     (collator.getCollationKey(secondRangeEndOriginal).toByteArray()));
 
   
  public final class TestAnalyzer extends ReusableAnalyzerBase {
  public final class TestAnalyzer extends Analyzer {
     private Collator _collator;
 
     TestAnalyzer(Collator collator) {
diff --git a/modules/analysis/morfologik/src/java/org/apache/lucene/analysis/morfologik/MorfologikAnalyzer.java b/modules/analysis/morfologik/src/java/org/apache/lucene/analysis/morfologik/MorfologikAnalyzer.java
index ec74568f0a6..bfe0ca40519 100644
-- a/modules/analysis/morfologik/src/java/org/apache/lucene/analysis/morfologik/MorfologikAnalyzer.java
++ b/modules/analysis/morfologik/src/java/org/apache/lucene/analysis/morfologik/MorfologikAnalyzer.java
@@ -20,10 +20,10 @@ package org.apache.lucene.analysis.morfologik;
 
 import java.io.Reader;
 
import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.standard.StandardFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.Version;
 
 import morfologik.stemming.PolishStemmer.DICTIONARY;
@@ -32,7 +32,7 @@ import morfologik.stemming.PolishStemmer.DICTIONARY;
  * {@link org.apache.lucene.analysis.Analyzer} using Morfologik library.
  * @see <a href="http://morfologik.blogspot.com/">Morfologik project page</a>
  */
public class MorfologikAnalyzer extends ReusableAnalyzerBase {
public class MorfologikAnalyzer extends Analyzer {
 
   private final DICTIONARY dictionary;
   private final Version version;
@@ -62,14 +62,14 @@ public class MorfologikAnalyzer extends ReusableAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @param field ignored field name
    * @param reader source of tokens
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter} and {@link MorfologikFilter}.
    */
diff --git a/modules/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java b/modules/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java
index d805717a34b..f078b6ab1d7 100644
-- a/modules/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java
++ b/modules/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java
@@ -25,7 +25,6 @@ import java.util.Collections;
 import java.util.Set;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.en.PorterStemFilter;
 import org.apache.lucene.analysis.util.WordlistLoader;
 import org.apache.lucene.analysis.TokenStream;
@@ -55,7 +54,7 @@ import org.apache.lucene.util.Version;
  * </p>
  * @lucene.experimental
  */
public final class SmartChineseAnalyzer extends ReusableAnalyzerBase {
public final class SmartChineseAnalyzer extends Analyzer {
 
   private final Set<?> stopWords;
   
diff --git a/modules/analysis/stempel/src/java/org/apache/lucene/analysis/pl/PolishAnalyzer.java b/modules/analysis/stempel/src/java/org/apache/lucene/analysis/pl/PolishAnalyzer.java
index 107af33788e..8dc589a6936 100644
-- a/modules/analysis/stempel/src/java/org/apache/lucene/analysis/pl/PolishAnalyzer.java
++ b/modules/analysis/stempel/src/java/org/apache/lucene/analysis/pl/PolishAnalyzer.java
@@ -121,11 +121,11 @@ public final class PolishAnalyzer extends StopwordAnalyzerBase {
 
   /**
    * Creates a
   * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    * which tokenizes all the text in the provided {@link Reader}.
    * 
    * @return A
   *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
    *         built from an {@link StandardTokenizer} filtered with
    *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
    *         , {@link KeywordMarkerFilter} if a stem exclusion set is
diff --git a/modules/facet/src/test/org/apache/lucene/facet/search/CategoryListIteratorTest.java b/modules/facet/src/test/org/apache/lucene/facet/search/CategoryListIteratorTest.java
index 45103b6260d..eed8d356020 100644
-- a/modules/facet/src/test/org/apache/lucene/facet/search/CategoryListIteratorTest.java
++ b/modules/facet/src/test/org/apache/lucene/facet/search/CategoryListIteratorTest.java
@@ -137,7 +137,7 @@ public class CategoryListIteratorTest extends LuceneTestCase {
     DataTokenStream dts2 = new DataTokenStream("2",new SortingIntEncoder(
         new UniqueValuesIntEncoder(new DGapIntEncoder(new VInt8IntEncoder()))));
     // this test requires that no payloads ever be randomly present!
    final Analyzer noPayloadsAnalyzer = new ReusableAnalyzerBase() {
    final Analyzer noPayloadsAnalyzer = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.KEYWORD, false));
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java
index 1e900c0cc1e..80cfff336de 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java
@@ -134,7 +134,7 @@ final class TestFoldingFilter extends TokenFilter {
   }
 }
 
final class ASCIIAnalyzer extends ReusableAnalyzerBase {
final class ASCIIAnalyzer extends Analyzer {
 
   @Override
   public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiAnalyzer.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiAnalyzer.java
index a845161f99d..a510c9d89ed 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiAnalyzer.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiAnalyzer.java
@@ -122,7 +122,7 @@ public class TestMultiAnalyzer extends BaseTokenStreamTestCase {
    * Expands "multi" to "multi" and "multi2", both at the same position,
    * and expands "triplemulti" to "triplemulti", "multi3", and "multi2".  
    */
  private class MultiAnalyzer extends ReusableAnalyzerBase {
  private class MultiAnalyzer extends Analyzer {
 
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
@@ -192,7 +192,7 @@ public class TestMultiAnalyzer extends BaseTokenStreamTestCase {
    * Analyzes "the quick brown" as: quick(incr=2) brown(incr=1).
    * Does not work correctly for input other than "the quick brown ...".
    */
  private class PosIncrementAnalyzer extends ReusableAnalyzerBase {
  private class PosIncrementAnalyzer extends Analyzer {
 
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java
index 8ee4fcaf1b2..e3d29cccf67 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java
@@ -300,7 +300,7 @@ public class TestMultiFieldQueryParser extends LuceneTestCase {
   /**
    * Return empty tokens for field "f1".
    */
  private static class AnalyzerReturningNull extends ReusableAnalyzerBase {
  private static class AnalyzerReturningNull extends Analyzer {
     MockAnalyzer stdAnalyzer = new MockAnalyzer(random);
 
     public AnalyzerReturningNull() {
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiPhraseQueryParsing.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiPhraseQueryParsing.java
index 8da47149fa2..85471dfedf6 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiPhraseQueryParsing.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiPhraseQueryParsing.java
@@ -18,7 +18,6 @@ package org.apache.lucene.queryparser.classic;
  */
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
@@ -42,7 +41,7 @@ public class TestMultiPhraseQueryParsing extends LuceneTestCase {
       }
     }
 
  private static class CannedAnalyzer extends ReusableAnalyzerBase {
  private static class CannedAnalyzer extends Analyzer {
     private final TokenAndPos[] tokens;
 
     public CannedAnalyzer(TokenAndPos[] tokens) {
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java
index 86f27f95205..c7755da8717 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java
@@ -112,7 +112,7 @@ public class TestPrecedenceQueryParser extends LuceneTestCase {
     }
   }
 
  public static final class QPTestAnalyzer extends ReusableAnalyzerBase {
  public static final class QPTestAnalyzer extends Analyzer {
 
     /** Filters MockTokenizer with StopFilter. */
     @Override
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiAnalyzerQPHelper.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiAnalyzerQPHelper.java
index 65d4973d884..7b1d49b8d33 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiAnalyzerQPHelper.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiAnalyzerQPHelper.java
@@ -143,7 +143,7 @@ public class TestMultiAnalyzerQPHelper extends LuceneTestCase {
    * Expands "multi" to "multi" and "multi2", both at the same position, and
    * expands "triplemulti" to "triplemulti", "multi3", and "multi2".
    */
  private class MultiAnalyzer extends ReusableAnalyzerBase {
  private class MultiAnalyzer extends Analyzer {
 
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
@@ -210,7 +210,7 @@ public class TestMultiAnalyzerQPHelper extends LuceneTestCase {
    * Analyzes "the quick brown" as: quick(incr=2) brown(incr=1). Does not work
    * correctly for input other than "the quick brown ...".
    */
  private class PosIncrementAnalyzer extends ReusableAnalyzerBase {
  private class PosIncrementAnalyzer extends Analyzer {
 
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiFieldQPHelper.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiFieldQPHelper.java
index 97ef084bde8..a4354418e66 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiFieldQPHelper.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiFieldQPHelper.java
@@ -338,7 +338,7 @@ public class TestMultiFieldQPHelper extends LuceneTestCase {
   /**
    * Return empty tokens for field "f1".
    */
  private static final class AnalyzerReturningNull extends ReusableAnalyzerBase {
  private static final class AnalyzerReturningNull extends Analyzer {
     MockAnalyzer stdAnalyzer = new MockAnalyzer(random);
 
     public AnalyzerReturningNull() {
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java
index 6627609a5d5..a132341c280 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java
@@ -128,7 +128,7 @@ public class TestQPHelper extends LuceneTestCase {
     }
   }
 
  public static final class QPTestAnalyzer extends ReusableAnalyzerBase {
  public static final class QPTestAnalyzer extends Analyzer {
 
     /** Filters MockTokenizer with StopFilter. */
     @Override
@@ -345,7 +345,7 @@ public class TestQPHelper extends LuceneTestCase {
     }
   }
 
  private class SimpleCJKAnalyzer extends ReusableAnalyzerBase {
  private class SimpleCJKAnalyzer extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       return new TokenStreamComponents(new SimpleCJKTokenizer(reader));
@@ -1242,7 +1242,7 @@ public class TestQPHelper extends LuceneTestCase {
     }
   }
 
  private class CannedAnalyzer extends ReusableAnalyzerBase {
  private class CannedAnalyzer extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String ignored, Reader alsoIgnored) {
       return new TokenStreamComponents(new CannedTokenStream());
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java
index dae74708d2d..d4cf0350031 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java
@@ -98,7 +98,7 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
   }
 
   
  public static final class QPTestAnalyzer extends ReusableAnalyzerBase {
  public static final class QPTestAnalyzer extends Analyzer {
 
     /** Filters MockTokenizer with StopFilter. */
     @Override
@@ -240,7 +240,7 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
     }
   }
 
  private class SimpleCJKAnalyzer extends ReusableAnalyzerBase {
  private class SimpleCJKAnalyzer extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       return new TokenStreamComponents(new SimpleCJKTokenizer(reader));
@@ -343,7 +343,7 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
     assertQueryEquals("a OR -b", null, "a -b");
 
     // +,-,! should be directly adjacent to operand (i.e. not separated by whitespace) to be treated as an operator
    Analyzer a = new ReusableAnalyzerBase() {
    Analyzer a = new Analyzer() {
       @Override
       public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, false));
@@ -1157,7 +1157,7 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
   }
   
   /** whitespace+lowercase analyzer with synonyms */
  private class Analyzer1 extends ReusableAnalyzerBase {
  private class Analyzer1 extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
@@ -1166,7 +1166,7 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
   }
   
   /** whitespace+lowercase analyzer without synonyms */
  private class Analyzer2 extends ReusableAnalyzerBase {
  private class Analyzer2 extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
@@ -1231,7 +1231,7 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
     }
     
   }
  private class MockCollationAnalyzer extends ReusableAnalyzerBase {
  private class MockCollationAnalyzer extends Analyzer {
     @Override
     public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
diff --git a/solr/core/src/java/org/apache/solr/analysis/FSTSynonymFilterFactory.java b/solr/core/src/java/org/apache/solr/analysis/FSTSynonymFilterFactory.java
index 57779bbbd22..2cbd5204abe 100644
-- a/solr/core/src/java/org/apache/solr/analysis/FSTSynonymFilterFactory.java
++ b/solr/core/src/java/org/apache/solr/analysis/FSTSynonymFilterFactory.java
@@ -37,7 +37,6 @@ import org.apache.lucene.analysis.synonym.SynonymFilter;
 import org.apache.lucene.analysis.synonym.SynonymMap;
 import org.apache.lucene.analysis.synonym.SolrSynonymParser;
 import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.Version;
 import org.apache.solr.common.ResourceLoader;
 import org.apache.solr.common.SolrException;
@@ -70,7 +69,7 @@ final class FSTSynonymFilterFactory extends BaseTokenFilterFactory implements Re
 
     final TokenizerFactory factory = tf == null ? null : loadTokenizerFactory(loader, tf, args);
     
    Analyzer analyzer = new ReusableAnalyzerBase() {
    Analyzer analyzer = new Analyzer() {
       @Override
       protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
         Tokenizer tokenizer = factory == null ? new WhitespaceTokenizer(Version.LUCENE_31, reader) : factory.create(reader);
diff --git a/solr/core/src/java/org/apache/solr/analysis/SolrAnalyzer.java b/solr/core/src/java/org/apache/solr/analysis/SolrAnalyzer.java
index aad0807b260..6ab3fac2bb7 100644
-- a/solr/core/src/java/org/apache/solr/analysis/SolrAnalyzer.java
++ b/solr/core/src/java/org/apache/solr/analysis/SolrAnalyzer.java
@@ -26,10 +26,10 @@ import java.io.IOException;
  *
  */
 public abstract class SolrAnalyzer extends Analyzer {
  int posIncGap=0;
  
  int posIncGap = 0;

   public void setPositionIncrementGap(int gap) {
    posIncGap=gap;
    posIncGap = gap;
   }
 
   @Override
@@ -38,43 +38,13 @@ public abstract class SolrAnalyzer extends Analyzer {
   }
 
   /** wrap the reader in a CharStream, if appropriate */
  public Reader charStream(Reader reader){
  @Deprecated
  public Reader charStream(Reader reader) {
     return reader;
   }
 
   @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    return getStream(fieldName, reader).getTokenStream();
  }

  public static class TokenStreamInfo {
    private final Tokenizer tokenizer;
    private final TokenStream tokenStream;
    public TokenStreamInfo(Tokenizer tokenizer, TokenStream tokenStream) {
      this.tokenizer = tokenizer;
      this.tokenStream = tokenStream;
    }
    public Tokenizer getTokenizer() { return tokenizer; }
    public TokenStream getTokenStream() { return tokenStream; }
  }


  public abstract TokenStreamInfo getStream(String fieldName, Reader reader);

  @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    // if (true) return tokenStream(fieldName, reader);
    TokenStreamInfo tsi = (TokenStreamInfo)getPreviousTokenStream();
    if (tsi != null) {
      tsi.getTokenizer().reset(charStream(reader));
      // the consumer will currently call reset() on the TokenStream to hit all the filters.
      // this isn't necessarily guaranteed by the APIs... but is currently done
      // by lucene indexing in DocInverterPerField, and in the QueryParser
      return tsi.getTokenStream();
    } else {
      tsi = getStream(fieldName, reader);
      setPreviousTokenStream(tsi);
      return tsi.getTokenStream();
    }
  protected Reader initReader(Reader reader) {
    return charStream(reader);
   }
 }
diff --git a/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java b/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java
index cf04a82c17d..19ee0e63092 100644
-- a/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java
++ b/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java
@@ -17,10 +17,7 @@
 
 package org.apache.solr.analysis;
 
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.CharStream;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.*;
 
 import java.io.Reader;
 
@@ -52,11 +49,11 @@ public final class TokenizerChain extends SolrAnalyzer {
   public TokenFilterFactory[] getTokenFilterFactories() { return filters; }
 
   @Override
  public Reader charStream(Reader reader){
    if( charFilters != null && charFilters.length > 0 ){
  public Reader initReader(Reader reader) {
    if (charFilters != null && charFilters.length > 0) {
       CharStream cs = CharReader.get( reader );
      for (int i=0; i<charFilters.length; i++) {
        cs = charFilters[i].create(cs);
      for (CharFilterFactory charFilter : charFilters) {
        cs = charFilter.create(cs);
       }
       reader = cs;
     }
@@ -64,13 +61,13 @@ public final class TokenizerChain extends SolrAnalyzer {
   }
 
   @Override
  public TokenStreamInfo getStream(String fieldName, Reader reader) {
    Tokenizer tk = tokenizer.create(charStream(reader));
  protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
    Tokenizer tk = tokenizer.create(aReader);
     TokenStream ts = tk;
    for (int i=0; i<filters.length; i++) {
      ts = filters[i].create(ts);
    for (TokenFilterFactory filter : filters) {
      ts = filter.create(ts);
     }
    return new TokenStreamInfo(tk,ts);
    return new TokenStreamComponents(tk, ts);
   }
 
   @Override
diff --git a/solr/core/src/java/org/apache/solr/handler/AnalysisRequestHandlerBase.java b/solr/core/src/java/org/apache/solr/handler/AnalysisRequestHandlerBase.java
index 314d10e4db6..16c598fe1a1 100644
-- a/solr/core/src/java/org/apache/solr/handler/AnalysisRequestHandlerBase.java
++ b/solr/core/src/java/org/apache/solr/handler/AnalysisRequestHandlerBase.java
@@ -113,7 +113,7 @@ public abstract class AnalysisRequestHandlerBase extends RequestHandlerBase {
       }
     }
 
    TokenStream tokenStream = tfac.create(tokenizerChain.charStream(new StringReader(value)));
    TokenStream tokenStream = tfac.create(tokenizerChain.initReader(new StringReader(value)));
     List<AttributeSource> tokens = analyzeTokenStream(tokenStream);
 
     namedList.add(tokenStream.getClass().getName(), convertTokensToNamedLists(tokens, context));
@@ -197,7 +197,7 @@ public abstract class AnalysisRequestHandlerBase extends RequestHandlerBase {
   /**
    * Converts the list of Tokens to a list of NamedLists representing the tokens.
    *
   * @param tokens  Tokens to convert
   * @param tokenList  Tokens to convert
    * @param context The analysis context
    *
    * @return List of NamedLists containing the relevant information taken from the tokens
diff --git a/solr/core/src/java/org/apache/solr/schema/BoolField.java b/solr/core/src/java/org/apache/solr/schema/BoolField.java
index 02ac067aecc..ecdeeb3761b 100644
-- a/solr/core/src/java/org/apache/solr/schema/BoolField.java
++ b/solr/core/src/java/org/apache/solr/schema/BoolField.java
@@ -30,7 +30,6 @@ import org.apache.lucene.util.CharsRef;
 import org.apache.lucene.util.mutable.MutableValue;
 import org.apache.lucene.util.mutable.MutableValueBool;
 import org.apache.solr.search.QParser;
import org.apache.solr.search.function.*;
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
@@ -70,7 +69,7 @@ public class BoolField extends FieldType {
 
   protected final static Analyzer boolAnalyzer = new SolrAnalyzer() {
     @Override
    public TokenStreamInfo getStream(String fieldName, Reader reader) {
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer tokenizer = new Tokenizer(reader) {
         final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
         boolean done = false;
@@ -95,7 +94,7 @@ public class BoolField extends FieldType {
         }
       };
 
      return new TokenStreamInfo(tokenizer, tokenizer);
      return new TokenStreamComponents(tokenizer);
     }
   };
 
diff --git a/solr/core/src/java/org/apache/solr/schema/FieldType.java b/solr/core/src/java/org/apache/solr/schema/FieldType.java
index 0babcf289d9..814df41b988 100644
-- a/solr/core/src/java/org/apache/solr/schema/FieldType.java
++ b/solr/core/src/java/org/apache/solr/schema/FieldType.java
@@ -389,7 +389,7 @@ public abstract class FieldType extends FieldProperties {
     }
 
     @Override
    public TokenStreamInfo getStream(String fieldName, Reader reader) {
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       Tokenizer ts = new Tokenizer(reader) {
         final char[] cbuf = new char[maxChars];
         final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
@@ -406,7 +406,7 @@ public abstract class FieldType extends FieldProperties {
         }
       };
 
      return new TokenStreamInfo(ts, ts);
      return new TokenStreamComponents(ts);
     }
   }
 
diff --git a/solr/core/src/java/org/apache/solr/schema/IndexSchema.java b/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
index 3e1ac3bce43..1325397a37e 100644
-- a/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
++ b/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
@@ -18,7 +18,7 @@
 package org.apache.solr.schema;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.AnalyzerWrapper;
 import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.search.similarities.DefaultSimilarity;
 import org.apache.lucene.search.similarities.Similarity;
@@ -41,8 +41,6 @@ import org.xml.sax.InputSource;
 import javax.xml.xpath.XPath;
 import javax.xml.xpath.XPathConstants;
 import javax.xml.xpath.XPathExpressionException;
import java.io.Reader;
import java.io.IOException;
 import java.util.*;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -292,50 +290,38 @@ public final class IndexSchema {
     queryAnalyzer = new SolrQueryAnalyzer();
   }
 
  private class SolrIndexAnalyzer extends Analyzer {
    protected final HashMap<String,Analyzer> analyzers;
  private class SolrIndexAnalyzer extends AnalyzerWrapper {
    protected final HashMap<String, Analyzer> analyzers;
 
     SolrIndexAnalyzer() {
       analyzers = analyzerCache();
     }
 
    protected HashMap<String,Analyzer> analyzerCache() {
      HashMap<String,Analyzer> cache = new HashMap<String,Analyzer>();
       for (SchemaField f : getFields().values()) {
    protected HashMap<String, Analyzer> analyzerCache() {
      HashMap<String, Analyzer> cache = new HashMap<String, Analyzer>();
      for (SchemaField f : getFields().values()) {
         Analyzer analyzer = f.getType().getAnalyzer();
         cache.put(f.getName(), analyzer);
       }
       return cache;
     }
 
    protected Analyzer getAnalyzer(String fieldName)
    {
      Analyzer analyzer = analyzers.get(fieldName);
      return analyzer!=null ? analyzer : getDynamicFieldType(fieldName).getAnalyzer();
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader)
    {
      return getAnalyzer(fieldName).tokenStream(fieldName,reader);
    }

     @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
      return getAnalyzer(fieldName).reusableTokenStream(fieldName,reader);
    protected Analyzer getWrappedAnalyzer(String fieldName) {
      Analyzer analyzer = analyzers.get(fieldName);
      return analyzer != null ? analyzer : getDynamicFieldType(fieldName).getAnalyzer();
     }
 
     @Override
    public int getPositionIncrementGap(String fieldName) {
      return getAnalyzer(fieldName).getPositionIncrementGap(fieldName);
    protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
      return components;
     }
   }
 

   private class SolrQueryAnalyzer extends SolrIndexAnalyzer {
     @Override
    protected HashMap<String,Analyzer> analyzerCache() {
      HashMap<String,Analyzer> cache = new HashMap<String,Analyzer>();
    protected HashMap<String, Analyzer> analyzerCache() {
      HashMap<String, Analyzer> cache = new HashMap<String, Analyzer>();
        for (SchemaField f : getFields().values()) {
         Analyzer analyzer = f.getType().getQueryAnalyzer();
         cache.put(f.getName(), analyzer);
@@ -344,10 +330,9 @@ public final class IndexSchema {
     }
 
     @Override
    protected Analyzer getAnalyzer(String fieldName)
    {
    protected Analyzer getWrappedAnalyzer(String fieldName) {
       Analyzer analyzer = analyzers.get(fieldName);
      return analyzer!=null ? analyzer : getDynamicFieldType(fieldName).getQueryAnalyzer();
      return analyzer != null ? analyzer : getDynamicFieldType(fieldName).getQueryAnalyzer();
     }
   }
 
diff --git a/solr/core/src/test/org/apache/solr/schema/IndexSchemaRuntimeFieldTest.java b/solr/core/src/test/org/apache/solr/schema/IndexSchemaRuntimeFieldTest.java
new file mode 100644
index 00000000000..9b915497d38
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/schema/IndexSchemaRuntimeFieldTest.java
@@ -0,0 +1,72 @@
package org.apache.solr.schema;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

public class IndexSchemaRuntimeFieldTest extends SolrTestCaseJ4 {

  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig.xml","schema.xml");
  }

  @Test
  public void testRuntimeFieldCreation() {
    // any field manipulation needs to happen when you know the core will not
    // be accepting any requests.  Typically this is done within the inform()
    // method.  Since this is a single threaded test, we can change the fields
    // willi-nilly

    SolrCore core = h.getCore();
    IndexSchema schema = core.getSchema();
    final String fieldName = "runtimefield";
    SchemaField sf = new SchemaField( fieldName, schema.getFieldTypes().get( "string" ) );
    schema.getFields().put( fieldName, sf );

    // also register a new copy field (from our new field)
    schema.registerCopyField( fieldName, "dynamic_runtime" );
    schema.refreshAnalyzers();

    assertU(adoc("id", "10", "title", "test", fieldName, "aaa"));
    assertU(commit());

    SolrQuery query = new SolrQuery( fieldName+":aaa" );
    query.set( "indent", "true" );
    SolrQueryRequest req = new LocalSolrQueryRequest( core, query );

    assertQ("Make sure they got in", req
            ,"//*[@numFound='1']"
            ,"//result/doc[1]/int[@name='id'][.='10']"
            );

    // Check to see if our copy field made it out safely
    query.setQuery( "dynamic_runtime:aaa" );
    assertQ("Make sure they got in", req
            ,"//*[@numFound='1']"
            ,"//result/doc[1]/int[@name='id'][.='10']"
            );
    clearIndex();
  }
}
diff --git a/solr/core/src/test/org/apache/solr/schema/IndexSchemaTest.java b/solr/core/src/test/org/apache/solr/schema/IndexSchemaTest.java
index 4d54a6fd2c5..6e97833a7b9 100644
-- a/solr/core/src/test/org/apache/solr/schema/IndexSchemaTest.java
++ b/solr/core/src/test/org/apache/solr/schema/IndexSchemaTest.java
@@ -17,27 +17,26 @@
 
 package org.apache.solr.schema;
 
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.search.similarities.SimilarityProvider;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.params.MapSolrParams;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.request.LocalSolrQueryRequest;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.search.similarities.MockConfigurableSimilarityProvider;
import org.apache.lucene.search.similarities.SimilarityProvider;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
import java.util.HashMap;
import java.util.Map;

 
 public class IndexSchemaTest extends SolrTestCaseJ4 {
   @BeforeClass
   public static void beforeClass() throws Exception {
     initCore("solrconfig.xml","schema.xml");
  }    
  }
 
   /**
    * This test assumes the schema includes:
@@ -45,22 +44,22 @@ public class IndexSchemaTest extends SolrTestCaseJ4 {
    * <dynamicField name="*_dynamic" type="string" indexed="true" stored="true"/>
    */
   @Test
  public void testDynamicCopy() 
  public void testDynamicCopy()
   {
     SolrCore core = h.getCore();
     assertU(adoc("id", "10", "title", "test", "aaa_dynamic", "aaa"));
     assertU(commit());
    

     Map<String,String> args = new HashMap<String, String>();
     args.put( CommonParams.Q, "title:test" );
     args.put( "indent", "true" );
     SolrQueryRequest req = new LocalSolrQueryRequest( core, new MapSolrParams( args) );
    

     assertQ("Make sure they got in", req
             ,"//*[@numFound='1']"
             ,"//result/doc[1]/int[@name='id'][.='10']"
             );
    

     args = new HashMap<String, String>();
     args.put( CommonParams.Q, "aaa_dynamic:aaa" );
     args.put( "indent", "true" );
@@ -80,46 +79,15 @@ public class IndexSchemaTest extends SolrTestCaseJ4 {
             );
     clearIndex();
   }
  
  @Test
  public void testRuntimeFieldCreation()
  {
    // any field manipulation needs to happen when you know the core will not 
    // be accepting any requests.  Typically this is done within the inform() 
    // method.  Since this is a single threaded test, we can change the fields
    // willi-nilly
 
  @Test
  public void testSimilarityProviderFactory() {
     SolrCore core = h.getCore();
    IndexSchema schema = core.getSchema();
    final String fieldName = "runtimefield";
    SchemaField sf = new SchemaField( fieldName, schema.getFieldTypes().get( "string" ) );
    schema.getFields().put( fieldName, sf );
    
    // also register a new copy field (from our new field)
    schema.registerCopyField( fieldName, "dynamic_runtime" );
    schema.refreshAnalyzers();
    
    assertU(adoc("id", "10", "title", "test", fieldName, "aaa"));
    assertU(commit());

    SolrQuery query = new SolrQuery( fieldName+":aaa" );
    query.set( "indent", "true" );
    SolrQueryRequest req = new LocalSolrQueryRequest( core, query );
    
    assertQ("Make sure they got in", req
            ,"//*[@numFound='1']"
            ,"//result/doc[1]/int[@name='id'][.='10']"
            );
    
    // Check to see if our copy field made it out safely
    query.setQuery( "dynamic_runtime:aaa" );
    assertQ("Make sure they got in", req
            ,"//*[@numFound='1']"
            ,"//result/doc[1]/int[@name='id'][.='10']"
            );
    clearIndex();
    SimilarityProvider similarityProvider = core.getSchema().getSimilarityProvider();
    assertTrue("wrong class", similarityProvider instanceof MockConfigurableSimilarityProvider);
    assertEquals("is there an echo?", ((MockConfigurableSimilarityProvider)similarityProvider).getPassthrough());
   }
  

   @Test
   public void testIsDynamicField() throws Exception {
     SolrCore core = h.getCore();
@@ -134,6 +102,5 @@ public class IndexSchemaTest extends SolrTestCaseJ4 {
     SolrCore core = h.getCore();
     IndexSchema schema = core.getSchema();
     assertFalse(schema.getField("id").multiValued());

   }
 }
diff --git a/solr/webapp/web/admin/analysis.jsp b/solr/webapp/web/admin/analysis.jsp
index e517eb0f842..180719ffc75 100644
-- a/solr/webapp/web/admin/analysis.jsp
++ b/solr/webapp/web/admin/analysis.jsp
@@ -204,7 +204,7 @@
          }
        }
 
       TokenStream tstream = tfac.create(tchain.charStream(new StringReader(val)));
       TokenStream tstream = tfac.create(tchain.initReader(new StringReader(val)));
        List<AttributeSource> tokens = getTokens(tstream);
        if (verbose) {
          writeHeader(out, tfac.getClass(), tfac.getArgs());
- 
2.19.1.windows.1

