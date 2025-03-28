From e92a38af90d12e51390b4307ccbe0c24ac7b6b4e Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@gmail.com>
Date: Tue, 28 Jun 2016 18:23:11 +0200
Subject: [PATCH] LUCENE-7355: Add Analyzer#normalize() and use it in query
 parsers.

--
 lucene/CHANGES.txt                            |  10 +
 lucene/MIGRATE.txt                            |  11 +
 .../lucene/analysis/ar/ArabicAnalyzer.java    |   8 +
 .../lucene/analysis/bg/BulgarianAnalyzer.java |   7 +
 .../lucene/analysis/br/BrazilianAnalyzer.java |   7 +
 .../lucene/analysis/ca/CatalanAnalyzer.java   |   8 +
 .../lucene/analysis/cjk/CJKAnalyzer.java      |   7 +
 .../lucene/analysis/ckb/SoraniAnalyzer.java   |   9 +
 .../lucene/analysis/core/SimpleAnalyzer.java  |   6 +
 .../lucene/analysis/core/StopAnalyzer.java    |   6 +
 .../analysis/custom/CustomAnalyzer.java       |  28 +-
 .../lucene/analysis/cz/CzechAnalyzer.java     |   7 +
 .../lucene/analysis/da/DanishAnalyzer.java    |   7 +
 .../lucene/analysis/de/GermanAnalyzer.java    |   8 +
 .../lucene/analysis/el/GreekAnalyzer.java     |   7 +
 .../lucene/analysis/en/EnglishAnalyzer.java   |   7 +
 .../lucene/analysis/es/SpanishAnalyzer.java   |   7 +
 .../lucene/analysis/eu/BasqueAnalyzer.java    |   7 +
 .../lucene/analysis/fa/PersianAnalyzer.java   |  14 +-
 .../lucene/analysis/fi/FinnishAnalyzer.java   |   7 +
 .../lucene/analysis/fr/FrenchAnalyzer.java    |   8 +
 .../lucene/analysis/ga/IrishAnalyzer.java     |   8 +
 .../lucene/analysis/gl/GalicianAnalyzer.java  |   7 +
 .../lucene/analysis/hi/HindiAnalyzer.java     |  11 +
 .../lucene/analysis/hu/HungarianAnalyzer.java |   7 +
 .../lucene/analysis/hy/ArmenianAnalyzer.java  |   7 +
 .../analysis/id/IndonesianAnalyzer.java       |   7 +
 .../lucene/analysis/it/ItalianAnalyzer.java   |   8 +
 .../analysis/lt/LithuanianAnalyzer.java       |   7 +
 .../lucene/analysis/lv/LatvianAnalyzer.java   |   7 +
 .../lucene/analysis/nl/DutchAnalyzer.java     |   7 +
 .../lucene/analysis/no/NorwegianAnalyzer.java |   7 +
 .../analysis/pt/PortugueseAnalyzer.java       |   7 +
 .../lucene/analysis/ro/RomanianAnalyzer.java  |   7 +
 .../lucene/analysis/ru/RussianAnalyzer.java   |   7 +
 .../analysis/standard/ClassicAnalyzer.java    |   5 +
 .../standard/UAX29URLEmailAnalyzer.java       |   5 +
 .../lucene/analysis/sv/SwedishAnalyzer.java   |   7 +
 .../lucene/analysis/th/ThaiAnalyzer.java      |   7 +
 .../lucene/analysis/tr/TurkishAnalyzer.java   |   7 +
 .../collation/CollationKeyAnalyzer.java       |   7 +
 .../core/TestAllAnalyzersHaveFactories.java   |   2 +
 .../lucene/analysis/core/TestAnalyzers.java   |   4 +
 .../analysis/core/TestRandomChains.java       |  10 +-
 .../analysis/custom/TestCustomAnalyzer.java   | 143 ++++++++++
 .../lucene/analysis/ja/JapaneseAnalyzer.java  |   7 +
 .../morfologik/MorfologikAnalyzer.java        |   6 +
 .../cn/smart/SmartChineseAnalyzer.java        |   6 +
 .../lucene/analysis/pl/PolishAnalyzer.java    |   7 +
 .../org/apache/lucene/analysis/Analyzer.java  | 135 ++++++++-
 .../analysis/standard/StandardAnalyzer.java   |   7 +
 .../standard/TestStandardAnalyzer.java        |   6 +
 .../analyzing/AnalyzingQueryParser.java       | 202 -------------
 .../queryparser/classic/QueryParserBase.java  | 140 +++------
 .../ComplexPhraseQueryParser.java             |  17 +-
 .../CommonQueryParserConfiguration.java       |  12 -
 .../standard/StandardQueryParser.java         |  30 --
 .../config/StandardQueryConfigHandler.java    |   9 -
 .../processors/FuzzyQueryNodeProcessor.java   |  11 +-
 ...ercaseExpandedTermsQueryNodeProcessor.java | 100 -------
 .../processors/RegexpQueryNodeProcessor.java  |  56 ++++
 .../StandardQueryNodeProcessorPipeline.java   |   4 +-
 .../TermRangeQueryNodeProcessor.java          |  11 +-
 .../WildcardQueryNodeProcessor.java           |  58 +++-
 .../queryparser/simple/SimpleQueryParser.java |   9 +-
 .../analyzing/TestAnalyzingQueryParser.java   | 268 ------------------
 .../queryparser/classic/TestQueryParser.java  | 241 +++++++++++++---
 .../precedence/TestPrecedenceQueryParser.java |  44 +--
 .../flexible/standard/TestQPHelper.java       |  57 ++--
 .../flexible/standard/TestStandardQP.java     |  15 -
 .../queryparser/util/QueryParserTestBase.java |  63 ++--
 .../analysis/BaseTokenStreamTestCase.java     |   5 +-
 .../apache/lucene/analysis/MockAnalyzer.java  |  11 +-
 .../lucene/analysis/MockBytesAnalyzer.java    |   7 +
 .../lucene/analysis/MockLowerCaseFilter.java} |  28 +-
 .../apache/solr/analysis/TokenizerChain.java  |  28 +-
 76 files changed, 1162 insertions(+), 925 deletions(-)
 delete mode 100644 lucene/queryparser/src/java/org/apache/lucene/queryparser/analyzing/AnalyzingQueryParser.java
 delete mode 100644 lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/LowercaseExpandedTermsQueryNodeProcessor.java
 create mode 100644 lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/RegexpQueryNodeProcessor.java
 delete mode 100644 lucene/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java
 rename lucene/{queryparser/src/java/org/apache/lucene/queryparser/analyzing/package-info.java => test-framework/src/java/org/apache/lucene/analysis/MockLowerCaseFilter.java} (55%)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index eba11c9bef0..c520e1bbca0 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -15,6 +15,9 @@ API Changes
 
 * LUCENE-7368: Removed query normalization. (Adrien Grand)
 
* LUCENE-7355: AnalyzingQueryParser has been removed as its functionality has
  been folded into the classic QueryParser. (Adrien Grand)

 Bug Fixes
 
 Improvements
@@ -48,6 +51,9 @@ New Features
   methods Directory.rename and Directory.syncMetaData instead (Robert Muir,
   Uwe Schindler, Mike McCandless)
 
* LUCENE-7355: Added Analyzer#normalize(), which only applies normalization to
  an input string. (Adrien Grand)

 Bug Fixes
 
 * LUCENE-6662: Fixed potential resource leaks. (Rishabh Patel via Adrien Grand)
@@ -99,6 +105,10 @@ Improvements
 * LUCENE-7276: MatchNoDocsQuery now includes an optional reason for
   why it was used (Jim Ferenczi via Mike McCandless)
 
* LUCENE-7355: AnalyzingQueryParser now only applies the subset of the analysis
  chain that is about normalization for range/fuzzy/wildcard queries.
  (Adrien Grand)

 Optimizations
 
 * LUCENE-7330, LUCENE-7339: Speed up conjunction queries. (Adrien Grand)
diff --git a/lucene/MIGRATE.txt b/lucene/MIGRATE.txt
index f914529a5e7..06e6a814f12 100644
-- a/lucene/MIGRATE.txt
++ b/lucene/MIGRATE.txt
@@ -36,3 +36,14 @@ Query normalization's goal was to make scores comparable across queries, which
 was only implemented by the ClassicSimilarity. Since ClassicSimilarity is not
 the default similarity anymore, this functionality has been removed. Boosts are
 now propagated through Query#createWeight.

## AnalyzingQueryParser removed (LUCENE-7355)

The functionality of AnalyzingQueryParser has been folded into the classic
QueryParser, which now passes terms through Analyzer#normalize when generating
queries.

## CommonQueryParserConfiguration.setLowerCaseExpandedTerms removed (LUCENE-7355)

This option has been removed as expanded terms are now normalized through
Analyzer#normalize.
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ar/ArabicAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ar/ArabicAnalyzer.java
index 889a8861f50..61100dd82a6 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ar/ArabicAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ar/ArabicAnalyzer.java
@@ -143,5 +143,13 @@ public final class ArabicAnalyzer extends StopwordAnalyzerBase {
     }
     return new TokenStreamComponents(source, new ArabicStemFilter(result));
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new LowerCaseFilter(in);
    result = new DecimalDigitFilter(result);
    result = new ArabicNormalizationFilter(result);
    return result;
  }
 }
 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/bg/BulgarianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/bg/BulgarianAnalyzer.java
index 9cb065712e9..06c7eea787e 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/bg/BulgarianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/bg/BulgarianAnalyzer.java
@@ -126,4 +126,11 @@ public final class BulgarianAnalyzer extends StopwordAnalyzerBase {
     result = new BulgarianStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/br/BrazilianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/br/BrazilianAnalyzer.java
index 5dd0cbc695d..ad1af92464e 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/br/BrazilianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/br/BrazilianAnalyzer.java
@@ -127,5 +127,12 @@ public final class BrazilianAnalyzer extends StopwordAnalyzerBase {
       result = new SetKeywordMarkerFilter(result, excltable);
     return new TokenStreamComponents(source, new BrazilianStemFilter(result));
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ca/CatalanAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ca/CatalanAnalyzer.java
index 739b61a3f91..56f36e15f9f 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ca/CatalanAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ca/CatalanAnalyzer.java
@@ -130,4 +130,12 @@ public final class CatalanAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new CatalanStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new ElisionFilter(result, DEFAULT_ARTICLES);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/cjk/CJKAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/cjk/CJKAnalyzer.java
index d500ff9c51a..d4214a111c1 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/cjk/CJKAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/cjk/CJKAnalyzer.java
@@ -92,4 +92,11 @@ public final class CJKAnalyzer extends StopwordAnalyzerBase {
     result = new CJKBigramFilter(result);
     return new TokenStreamComponents(source, new StopFilter(result, stopwords));
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new CJKWidthFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ckb/SoraniAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ckb/SoraniAnalyzer.java
index 0f283b8ff11..e7ce3f39e44 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ckb/SoraniAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ckb/SoraniAnalyzer.java
@@ -126,4 +126,13 @@ public final class SoraniAnalyzer extends StopwordAnalyzerBase {
     result = new SoraniStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new SoraniNormalizationFilter(result);
    result = new LowerCaseFilter(result);
    result = new DecimalDigitFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/SimpleAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/SimpleAnalyzer.java
index d0fdcf6cc9f..6e0f2f0b67e 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/SimpleAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/SimpleAnalyzer.java
@@ -19,6 +19,7 @@ package org.apache.lucene.analysis.core;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
 
 /** An {@link Analyzer} that filters {@link LetterTokenizer} 
  *  with {@link LowerCaseFilter} 
@@ -35,4 +36,9 @@ public final class SimpleAnalyzer extends Analyzer {
   protected TokenStreamComponents createComponents(final String fieldName) {
     return new TokenStreamComponents(new LowerCaseTokenizer());
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java
index 3fa498283e6..7d7f532b6a0 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java
@@ -25,6 +25,7 @@ import org.apache.lucene.analysis.CharArraySet;
 import org.apache.lucene.analysis.LowerCaseFilter;
 import org.apache.lucene.analysis.StopFilter;
 import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.WordlistLoader;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
@@ -79,5 +80,10 @@ public final class StopAnalyzer extends StopwordAnalyzerBase {
     final Tokenizer source = new LowerCaseTokenizer();
     return new TokenStreamComponents(source, new StopFilter(source, stopwords));
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }
 }
 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/custom/CustomAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/custom/CustomAnalyzer.java
index f2ed01f4e9f..b2de5e8b34b 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/custom/CustomAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/custom/CustomAnalyzer.java
@@ -37,6 +37,7 @@ import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
 import org.apache.lucene.analysis.util.CharFilterFactory;
 import org.apache.lucene.analysis.util.ClasspathResourceLoader;
 import org.apache.lucene.analysis.util.FilesystemResourceLoader;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
 import org.apache.lucene.analysis.util.ResourceLoader;
 import org.apache.lucene.analysis.util.ResourceLoaderAware;
 import org.apache.lucene.analysis.util.TokenFilterFactory;
@@ -117,16 +118,39 @@ public final class CustomAnalyzer extends Analyzer {
     return reader;
   }
 
  @Override
  protected Reader initReaderForNormalization(String fieldName, Reader reader) {
    for (CharFilterFactory charFilter : charFilters) {
      if (charFilter instanceof MultiTermAwareComponent) {
        charFilter = (CharFilterFactory) ((MultiTermAwareComponent) charFilter).getMultiTermComponent();
        reader = charFilter.create(reader);
      }
    }
    return reader;
  }

   @Override
   protected TokenStreamComponents createComponents(String fieldName) {
    final Tokenizer tk = tokenizer.create();
    final Tokenizer tk = tokenizer.create(attributeFactory());
     TokenStream ts = tk;
     for (final TokenFilterFactory filter : tokenFilters) {
       ts = filter.create(ts);
     }
     return new TokenStreamComponents(tk, ts);
   }
  

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = in;
    for (TokenFilterFactory filter : tokenFilters) {
      if (filter instanceof MultiTermAwareComponent) {
        filter = (TokenFilterFactory) ((MultiTermAwareComponent) filter).getMultiTermComponent();
        result = filter.create(in);
      }
    }
    return result;
  }

   @Override
   public int getPositionIncrementGap(String fieldName) {
     // use default from Analyzer base class if null
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/cz/CzechAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/cz/CzechAnalyzer.java
index 97771790939..fbb9efabdda 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/cz/CzechAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/cz/CzechAnalyzer.java
@@ -125,5 +125,12 @@ public final class CzechAnalyzer extends StopwordAnalyzerBase {
     result = new CzechStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/da/DanishAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/da/DanishAnalyzer.java
index f9c316d6dbd..ccbd9d1900e 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/da/DanishAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/da/DanishAnalyzer.java
@@ -124,4 +124,11 @@ public final class DanishAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new DanishStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/de/GermanAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/de/GermanAnalyzer.java
index 790fc48112b..8a3994530fd 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/de/GermanAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/de/GermanAnalyzer.java
@@ -139,4 +139,12 @@ public final class GermanAnalyzer extends StopwordAnalyzerBase {
     result = new GermanLightStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    result = new GermanNormalizationFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/el/GreekAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/el/GreekAnalyzer.java
index c85b6eccc92..bd09d255525 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/el/GreekAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/el/GreekAnalyzer.java
@@ -104,4 +104,11 @@ public final class GreekAnalyzer extends StopwordAnalyzerBase {
     result = new GreekStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new GreekLowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java
index 16dc0c55da8..94ba43a710e 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java
@@ -107,4 +107,11 @@ public final class EnglishAnalyzer extends StopwordAnalyzerBase {
     result = new PorterStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/es/SpanishAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/es/SpanishAnalyzer.java
index ab5b6c356d9..3b21cdde48a 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/es/SpanishAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/es/SpanishAnalyzer.java
@@ -123,4 +123,11 @@ public final class SpanishAnalyzer extends StopwordAnalyzerBase {
     result = new SpanishLightStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/eu/BasqueAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/eu/BasqueAnalyzer.java
index cff2da046ce..4bc1ba76600 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/eu/BasqueAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/eu/BasqueAnalyzer.java
@@ -121,4 +121,11 @@ public final class BasqueAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new BasqueStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/fa/PersianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/fa/PersianAnalyzer.java
index f29dfd3607e..9aebc2d675e 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/fa/PersianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/fa/PersianAnalyzer.java
@@ -29,6 +29,7 @@ import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.ar.ArabicNormalizationFilter;
 import org.apache.lucene.analysis.core.DecimalDigitFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
 
 /**
@@ -125,7 +126,18 @@ public final class PersianAnalyzer extends StopwordAnalyzerBase {
      */
     return new TokenStreamComponents(source, new StopFilter(result, stopwords));
   }
  

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    result = new DecimalDigitFilter(result);
    result = new ArabicNormalizationFilter(result);
    /* additional persian-specific normalization */
    result = new PersianNormalizationFilter(result);
    return result;
  }

   /** 
    * Wraps the Reader with {@link PersianCharFilter}
    */
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/fi/FinnishAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/fi/FinnishAnalyzer.java
index 6b001016531..69cc5378612 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/fi/FinnishAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/fi/FinnishAnalyzer.java
@@ -124,4 +124,11 @@ public final class FinnishAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new FinnishStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/fr/FrenchAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/fr/FrenchAnalyzer.java
index 5f90246b4b3..2e072be62a2 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/fr/FrenchAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/fr/FrenchAnalyzer.java
@@ -144,5 +144,13 @@ public final class FrenchAnalyzer extends StopwordAnalyzerBase {
     result = new FrenchLightStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new ElisionFilter(result, DEFAULT_ARTICLES);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ga/IrishAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ga/IrishAnalyzer.java
index 1ca3455224d..3ae366d3eac 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ga/IrishAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ga/IrishAnalyzer.java
@@ -141,4 +141,12 @@ public final class IrishAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new IrishStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new ElisionFilter(result, DEFAULT_ARTICLES);
    result = new IrishLowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/gl/GalicianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/gl/GalicianAnalyzer.java
index 372a6ec9249..4f705968e40 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/gl/GalicianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/gl/GalicianAnalyzer.java
@@ -122,4 +122,11 @@ public final class GalicianAnalyzer extends StopwordAnalyzerBase {
     result = new GalicianStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/hi/HindiAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/hi/HindiAnalyzer.java
index 8e4868b7a08..f3392954231 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/hi/HindiAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/hi/HindiAnalyzer.java
@@ -29,6 +29,7 @@ import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.DecimalDigitFilter;
 import org.apache.lucene.analysis.in.IndicNormalizationFilter;
 import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
 
 /**
@@ -125,4 +126,14 @@ public final class HindiAnalyzer extends StopwordAnalyzerBase {
     result = new HindiStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    result = new DecimalDigitFilter(result);
    result = new IndicNormalizationFilter(result);
    result = new HindiNormalizationFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/hu/HungarianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/hu/HungarianAnalyzer.java
index 0615bdc1667..e980f5a126a 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/hu/HungarianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/hu/HungarianAnalyzer.java
@@ -124,4 +124,11 @@ public final class HungarianAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new HungarianStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/hy/ArmenianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/hy/ArmenianAnalyzer.java
index 8c046392333..95506e19d1c 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/hy/ArmenianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/hy/ArmenianAnalyzer.java
@@ -121,4 +121,11 @@ public final class ArmenianAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new ArmenianStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/id/IndonesianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/id/IndonesianAnalyzer.java
index fc9b4d2550d..9804beafaa2 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/id/IndonesianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/id/IndonesianAnalyzer.java
@@ -119,4 +119,11 @@ public final class IndonesianAnalyzer extends StopwordAnalyzerBase {
     }
     return new TokenStreamComponents(source, new IndonesianStemFilter(result));
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/it/ItalianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/it/ItalianAnalyzer.java
index a18aa5d699e..32f4e30bf19 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/it/ItalianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/it/ItalianAnalyzer.java
@@ -133,4 +133,12 @@ public final class ItalianAnalyzer extends StopwordAnalyzerBase {
     result = new ItalianLightStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new ElisionFilter(result, DEFAULT_ARTICLES);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/lt/LithuanianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/lt/LithuanianAnalyzer.java
index 5e24cf988b2..4eccc51121c 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/lt/LithuanianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/lt/LithuanianAnalyzer.java
@@ -121,4 +121,11 @@ public final class LithuanianAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new LithuanianStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/lv/LatvianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/lv/LatvianAnalyzer.java
index 0a016af36be..1b08b3b5894 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/lv/LatvianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/lv/LatvianAnalyzer.java
@@ -122,4 +122,11 @@ public final class LatvianAnalyzer extends StopwordAnalyzerBase {
     result = new LatvianStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/nl/DutchAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/nl/DutchAnalyzer.java
index 0391425d6f8..900d9c64b20 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/nl/DutchAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/nl/DutchAnalyzer.java
@@ -159,4 +159,11 @@ public final class DutchAnalyzer extends Analyzer {
     result = new SnowballFilter(result, new org.tartarus.snowball.ext.DutchStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/no/NorwegianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/no/NorwegianAnalyzer.java
index c413793bc82..3570ad42a31 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/no/NorwegianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/no/NorwegianAnalyzer.java
@@ -124,5 +124,12 @@ public final class NorwegianAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new NorwegianStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/pt/PortugueseAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/pt/PortugueseAnalyzer.java
index 769e1425bbd..8f548033be8 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/pt/PortugueseAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/pt/PortugueseAnalyzer.java
@@ -123,4 +123,11 @@ public final class PortugueseAnalyzer extends StopwordAnalyzerBase {
     result = new PortugueseLightStemFilter(result);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ro/RomanianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ro/RomanianAnalyzer.java
index 06ff9999e1b..1b74184af2d 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ro/RomanianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ro/RomanianAnalyzer.java
@@ -126,4 +126,11 @@ public final class RomanianAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new RomanianStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ru/RussianAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ru/RussianAnalyzer.java
index dfe8ef38adc..76bf49506d1 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/ru/RussianAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/ru/RussianAnalyzer.java
@@ -121,4 +121,11 @@ public final class RussianAnalyzer extends StopwordAnalyzerBase {
       result = new SnowballFilter(result, new org.tartarus.snowball.ext.RussianStemmer());
       return new TokenStreamComponents(source, result);
     }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
      TokenStream result = new StandardFilter(in);
      result = new LowerCaseFilter(result);
      return result;
    }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/standard/ClassicAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/standard/ClassicAnalyzer.java
index dc6c1188fb1..ef2ef7ebb18 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/standard/ClassicAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/standard/ClassicAnalyzer.java
@@ -100,4 +100,9 @@ public final class ClassicAnalyzer extends StopwordAnalyzerBase {
       }
     };
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/standard/UAX29URLEmailAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/standard/UAX29URLEmailAnalyzer.java
index 9994884e449..fe71b7e83f1 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/standard/UAX29URLEmailAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/standard/UAX29URLEmailAnalyzer.java
@@ -97,4 +97,9 @@ public final class UAX29URLEmailAnalyzer extends StopwordAnalyzerBase {
       }
     };
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/sv/SwedishAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/sv/SwedishAnalyzer.java
index fd2aa2e54a9..3896d3ece8d 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/sv/SwedishAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/sv/SwedishAnalyzer.java
@@ -124,4 +124,11 @@ public final class SwedishAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new SwedishStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/th/ThaiAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/th/ThaiAnalyzer.java
index 11f3f779cba..c1426b81cce 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/th/ThaiAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/th/ThaiAnalyzer.java
@@ -102,4 +102,11 @@ public final class ThaiAnalyzer extends StopwordAnalyzerBase {
     result = new StopFilter(result, stopwords);
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new LowerCaseFilter(in);
    result = new DecimalDigitFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/tr/TurkishAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/tr/TurkishAnalyzer.java
index a21495fb3eb..719e4344077 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/tr/TurkishAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/tr/TurkishAnalyzer.java
@@ -127,4 +127,11 @@ public final class TurkishAnalyzer extends StopwordAnalyzerBase {
     result = new SnowballFilter(result, new TurkishStemmer());
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new TurkishLowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/collation/CollationKeyAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/collation/CollationKeyAnalyzer.java
index f7b15f6894f..ea987315b07 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/collation/CollationKeyAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/collation/CollationKeyAnalyzer.java
@@ -20,6 +20,8 @@ package org.apache.lucene.collation;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.util.AttributeFactory;

 import java.text.Collator;
 
 /**
@@ -82,6 +84,11 @@ public final class CollationKeyAnalyzer extends Analyzer {
     this.factory = new CollationAttributeFactory(collator);
   }
 
  @Override
  protected AttributeFactory attributeFactory() {
    return factory;
  }

   @Override
   protected TokenStreamComponents createComponents(String fieldName) {
     KeywordTokenizer tokenizer = new KeywordTokenizer(factory, KeywordTokenizer.DEFAULT_BUFFER_SIZE);
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestAllAnalyzersHaveFactories.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestAllAnalyzersHaveFactories.java
index d826a60d677..7099566b447 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestAllAnalyzersHaveFactories.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestAllAnalyzersHaveFactories.java
@@ -35,6 +35,7 @@ import org.apache.lucene.analysis.MockCharFilter;
 import org.apache.lucene.analysis.MockFixedLengthPayloadFilter;
 import org.apache.lucene.analysis.MockGraphTokenFilter;
 import org.apache.lucene.analysis.MockHoleInjectingTokenFilter;
import org.apache.lucene.analysis.MockLowerCaseFilter;
 import org.apache.lucene.analysis.MockRandomLookaheadTokenFilter;
 import org.apache.lucene.analysis.MockSynonymFilter;
 import org.apache.lucene.analysis.MockTokenFilter;
@@ -75,6 +76,7 @@ public class TestAllAnalyzersHaveFactories extends LuceneTestCase {
       MockFixedLengthPayloadFilter.class,
       MockGraphTokenFilter.class,
       MockHoleInjectingTokenFilter.class,
      MockLowerCaseFilter.class,
       MockRandomLookaheadTokenFilter.class,
       MockSynonymFilter.class,
       MockTokenFilter.class,
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java
index 8f7f2cd8a4b..6d514d171a8 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java
@@ -52,6 +52,7 @@ public class TestAnalyzers extends BaseTokenStreamTestCase {
                      new String[] { "b" });
     assertAnalyzesTo(a, "\"QUOTED\" word", 
                      new String[] { "quoted", "word" });
    assertEquals(new BytesRef("\"\\à3[]()! cz@"), a.normalize("dummy", "\"\\À3[]()! Cz@"));
     a.close();
   }
 
@@ -73,6 +74,7 @@ public class TestAnalyzers extends BaseTokenStreamTestCase {
                      new String[] { "2B" });
     assertAnalyzesTo(a, "\"QUOTED\" word", 
                      new String[] { "\"QUOTED\"", "word" });
    assertEquals(new BytesRef("\"\\À3[]()! Cz@"), a.normalize("dummy", "\"\\À3[]()! Cz@"));
     a.close();
   }
 
@@ -82,6 +84,8 @@ public class TestAnalyzers extends BaseTokenStreamTestCase {
                      new String[] { "foo", "bar", "foo", "bar" });
     assertAnalyzesTo(a, "foo a bar such FOO THESE BAR", 
                      new String[] { "foo", "bar", "foo", "bar" });
    assertEquals(new BytesRef("\"\\à3[]()! cz@"), a.normalize("dummy", "\"\\À3[]()! Cz@"));
    assertEquals(new BytesRef("the"), a.normalize("dummy", "the"));
     a.close();
   }
 
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestRandomChains.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestRandomChains.java
index 4effc79f167..25ca7a3a3bc 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestRandomChains.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestRandomChains.java
@@ -928,6 +928,7 @@ public class TestRandomChains extends BaseTokenStreamTestCase {
           System.out.println("Creating random analyzer:" + a);
         }
         try {
          checkNormalize(a);
           checkRandomData(random, a, 500*RANDOM_MULTIPLIER, 20, false,
               false /* We already validate our own offsets... */);
         } catch (Throwable e) {
@@ -937,7 +938,14 @@ public class TestRandomChains extends BaseTokenStreamTestCase {
       }
     }
   }
  

  public void checkNormalize(Analyzer a) {
    // normalization should not modify characters that may be used for wildcards
    // or regular expressions
    String s = "([0-9]+)?*";
    assertEquals(s, a.normalize("dummy", s).utf8ToString());
  }

   // we might regret this decision...
   public void testRandomChainsWithLargeStrings() throws Throwable {
     int numIterations = TEST_NIGHTLY ? atLeast(20) : 3;
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/custom/TestCustomAnalyzer.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/custom/TestCustomAnalyzer.java
index 5160dab73c4..aa69b709ec9 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/custom/TestCustomAnalyzer.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/custom/TestCustomAnalyzer.java
@@ -17,6 +17,8 @@
 package org.apache.lucene.analysis.custom;
 
 
import java.io.IOException;
import java.io.Reader;
 import java.nio.file.Paths;
 import java.util.Collections;
 import java.util.HashMap;
@@ -24,16 +26,25 @@ import java.util.List;
 import java.util.Map;
 
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
 import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
 import org.apache.lucene.analysis.core.StopFilterFactory;
 import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
 import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
 import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;
 import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
 import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
 import org.apache.lucene.analysis.util.TokenFilterFactory;
 import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.SetOnce.AlreadySetException;
 import org.apache.lucene.util.Version;
 
@@ -336,4 +347,136 @@ public class TestCustomAnalyzer extends BaseTokenStreamTestCase {
     });
   }
 
  private static class DummyCharFilter extends CharFilter {

    private final char match, repl;

    public DummyCharFilter(Reader input, char match, char repl) {
      super(input);
      this.match = match;
      this.repl = repl;
    }

    @Override
    protected int correct(int currentOff) {
      return currentOff;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      final int read = input.read(cbuf, off, len);
      for (int i = 0; i < read; ++i) {
        if (cbuf[off+i] == match) {
          cbuf[off+i] = repl;
        }
      }
      return read;
    }
    
  }

  public static class DummyCharFilterFactory extends CharFilterFactory {

    private final char match, repl;

    public DummyCharFilterFactory(Map<String,String> args) {
      this(args, '0', '1');
    }

    DummyCharFilterFactory(Map<String,String> args, char match, char repl) {
      super(args);
      this.match = match;
      this.repl = repl;
    }

    @Override
    public Reader create(Reader input) {
      return new DummyCharFilter(input, match, repl);
    }
    
  }

  public static class DummyMultiTermAwareCharFilterFactory extends DummyCharFilterFactory implements MultiTermAwareComponent {

    public DummyMultiTermAwareCharFilterFactory(Map<String,String> args) {
      super(args);
    }

    @Override
    public AbstractAnalysisFactory getMultiTermComponent() {
      return new DummyCharFilterFactory(Collections.emptyMap(), '0', '2');
    }

  }

  public static class DummyTokenizerFactory extends TokenizerFactory {

    public DummyTokenizerFactory(Map<String,String> args) {
      super(args);
    }

    @Override
    public Tokenizer create(AttributeFactory factory) {
      return new LowerCaseTokenizer(factory);
    }

  }

  public static class DummyMultiTermAwareTokenizerFactory extends DummyTokenizerFactory implements MultiTermAwareComponent {

    public DummyMultiTermAwareTokenizerFactory(Map<String,String> args) {
      super(args);
    }

    @Override
    public AbstractAnalysisFactory getMultiTermComponent() {
      return new KeywordTokenizerFactory(getOriginalArgs());
    }
    
  }

  public static class DummyTokenFilterFactory extends TokenFilterFactory {

    public DummyTokenFilterFactory(Map<String,String> args) {
      super(args);
    }

    @Override
    public TokenStream create(TokenStream input) {
      return input;
    }
    
  }

  public static class DummyMultiTermAwareTokenFilterFactory extends DummyTokenFilterFactory implements MultiTermAwareComponent {

    public DummyMultiTermAwareTokenFilterFactory(Map<String,String> args) {
      super(args);
    }

    @Override
    public AbstractAnalysisFactory getMultiTermComponent() {
      return new ASCIIFoldingFilterFactory(Collections.emptyMap());
    }
    
  }

  public void testNormalization() throws IOException {
    CustomAnalyzer analyzer1 = CustomAnalyzer.builder()
        // none of these components are multi-term aware so they should not be applied
        .withTokenizer(DummyTokenizerFactory.class, Collections.emptyMap())
        .addCharFilter(DummyCharFilterFactory.class, Collections.emptyMap())
        .addTokenFilter(DummyTokenFilterFactory.class, Collections.emptyMap())
        .build();
    assertEquals(new BytesRef("0À"), analyzer1.normalize("dummy", "0À"));

    CustomAnalyzer analyzer2 = CustomAnalyzer.builder()
        // these components are multi-term aware so they should be applied
        .withTokenizer(DummyMultiTermAwareTokenizerFactory.class, Collections.emptyMap())
        .addCharFilter(DummyMultiTermAwareCharFilterFactory.class, Collections.emptyMap())
        .addTokenFilter(DummyMultiTermAwareTokenFilterFactory.class, Collections.emptyMap())
        .build();
    assertEquals(new BytesRef("2A"), analyzer2.normalize("dummy", "0À"));
  }

 }
diff --git a/lucene/analysis/kuromoji/src/java/org/apache/lucene/analysis/ja/JapaneseAnalyzer.java b/lucene/analysis/kuromoji/src/java/org/apache/lucene/analysis/ja/JapaneseAnalyzer.java
index 46d40b18b1e..06e119e0920 100644
-- a/lucene/analysis/kuromoji/src/java/org/apache/lucene/analysis/ja/JapaneseAnalyzer.java
++ b/lucene/analysis/kuromoji/src/java/org/apache/lucene/analysis/ja/JapaneseAnalyzer.java
@@ -94,4 +94,11 @@ public class JapaneseAnalyzer extends StopwordAnalyzerBase {
     stream = new LowerCaseFilter(stream);
     return new TokenStreamComponents(tokenizer, stream);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new CJKWidthFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/analysis/morfologik/src/java/org/apache/lucene/analysis/morfologik/MorfologikAnalyzer.java b/lucene/analysis/morfologik/src/java/org/apache/lucene/analysis/morfologik/MorfologikAnalyzer.java
index 091acfdaf03..0caca357a8c 100644
-- a/lucene/analysis/morfologik/src/java/org/apache/lucene/analysis/morfologik/MorfologikAnalyzer.java
++ b/lucene/analysis/morfologik/src/java/org/apache/lucene/analysis/morfologik/MorfologikAnalyzer.java
@@ -23,6 +23,7 @@ import morfologik.stemming.Dictionary;
 import morfologik.stemming.polish.PolishStemmer;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.standard.StandardFilter;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
@@ -69,4 +70,9 @@ public class MorfologikAnalyzer extends Analyzer {
         src, 
         new MorfologikFilter(new StandardFilter(src), dictionary));
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new StandardFilter(in);
  }
 }
diff --git a/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java b/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java
index 5f0347b2fa1..f604d4b8970 100644
-- a/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java
++ b/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java
@@ -22,6 +22,7 @@ import java.util.Set;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
 import org.apache.lucene.analysis.StopFilter;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
@@ -139,4 +140,9 @@ public final class SmartChineseAnalyzer extends Analyzer {
     }
     return new TokenStreamComponents(tokenizer, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }
 }
diff --git a/lucene/analysis/stempel/src/java/org/apache/lucene/analysis/pl/PolishAnalyzer.java b/lucene/analysis/stempel/src/java/org/apache/lucene/analysis/pl/PolishAnalyzer.java
index 6ed4fda6ba1..2d3ef4cfa36 100644
-- a/lucene/analysis/stempel/src/java/org/apache/lucene/analysis/pl/PolishAnalyzer.java
++ b/lucene/analysis/stempel/src/java/org/apache/lucene/analysis/pl/PolishAnalyzer.java
@@ -146,4 +146,11 @@ public final class PolishAnalyzer extends StopwordAnalyzerBase {
     result = new StempelFilter(result, new StempelStemmer(stemTable));
     return new TokenStreamComponents(source, result);
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java b/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java
index cce740d14dc..0d60d2495dc 100644
-- a/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java
++ b/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java
@@ -18,11 +18,18 @@ package org.apache.lucene.analysis;
 
 
 import java.io.Closeable;
import java.io.IOException;
 import java.io.Reader;
import java.io.StringReader;
 import java.util.HashMap;
 import java.util.Map;
 
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
 import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.CloseableThreadLocal;
 import org.apache.lucene.util.Version;
 
@@ -44,6 +51,12 @@ import org.apache.lucene.util.Version;
  *     filter = new BarFilter(filter);
  *     return new TokenStreamComponents(source, filter);
  *   }
 *   {@literal @Override}
 *   protected TokenStream normalize(TokenStream in) {
 *     // Assuming FooFilter is about normalization and BarFilter is about
 *     // stemming, only FooFilter should be applied
 *     return new FooFilter(in);
 *   }
  * };
  * </pre>
  * For more examples, see the {@link org.apache.lucene.analysis Analysis package documentation}.
@@ -107,6 +120,15 @@ public abstract class Analyzer implements Closeable {
    */
   protected abstract TokenStreamComponents createComponents(String fieldName);
 
  /**
   * Wrap the given {@link TokenStream} in order to apply normalization filters.
   * The default implementation returns the {@link TokenStream} as-is. This is
   * used by {@link #normalize(String, String)}.
   */
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return in;
  }

   /**
    * Returns a TokenStream suitable for <code>fieldName</code>, tokenizing
    * the contents of <code>reader</code>.
@@ -181,7 +203,65 @@ public abstract class Analyzer implements Closeable {
     components.reusableStringReader = strReader;
     return components.getTokenStream();
   }
    

  /**
   * Normalize a string down to the representation that it would have in the
   * index.
   * <p>
   * This is typically used by query parsers in order to generate a query on
   * a given term, without tokenizing or stemming, which are undesirable if
   * the string to analyze is a partial word (eg. in case of a wildcard or
   * fuzzy query).
   * <p>
   * This method uses {@link #initReaderForNormalization(String, Reader)} in
   * order to apply necessary character-level normalization and then
   * {@link #normalize(String, TokenStream)} in order to apply the normalizing
   * token filters.
   */
  public final BytesRef normalize(final String fieldName, final String text) {
    try {
      // apply char filters
      final String filteredText;
      try (Reader reader = new StringReader(text)) {
        Reader filterReader = initReaderForNormalization(fieldName, reader);
        char[] buffer = new char[64];
        StringBuilder builder = new StringBuilder();
        for (;;) {
          final int read = filterReader.read(buffer, 0, buffer.length);
          if (read == -1) {
            break;
          }
          builder.append(buffer, 0, read);
        }
        filteredText = builder.toString();
      } catch (IOException e) {
        throw new IllegalStateException("Normalization threw an unexpected exeption", e);
      }

      final AttributeFactory attributeFactory = attributeFactory();
      try (TokenStream ts = normalize(fieldName,
          new StringTokenStream(attributeFactory, filteredText, text.length()))) {
        final TermToBytesRefAttribute termAtt = ts.addAttribute(TermToBytesRefAttribute.class);
        ts.reset();
        if (ts.incrementToken() == false) {
          throw new IllegalStateException("The normalization token stream is "
              + "expected to produce exactly 1 token, but got 0 for analyzer "
              + this + " and input \"" + text + "\"");
        }
        final BytesRef term = BytesRef.deepCopyOf(termAtt.getBytesRef());
        if (ts.incrementToken()) {
          throw new IllegalStateException("The normalization token stream is "
              + "expected to produce exactly 1 token, but got 2+ for analyzer "
              + this + " and input \"" + text + "\"");
        }
        ts.end();
        return term;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Normalization threw an unexpected exeption", e);
    }
  }

   /**
    * Override this if you want to add a CharFilter chain.
    * <p>
@@ -196,6 +276,22 @@ public abstract class Analyzer implements Closeable {
     return reader;
   }
 
  /** Wrap the given {@link Reader} with {@link CharFilter}s that make sense
   *  for normalization. This is typically a subset of the {@link CharFilter}s
   *  that are applied in {@link #initReader(String, Reader)}. This is used by
   *  {@link #normalize(String, String)}. */
  protected Reader initReaderForNormalization(String fieldName, Reader reader) {
    return reader;
  }

  /** Return the {@link AttributeFactory} to be used for
   *  {@link #tokenStream analysis} and
   *  {@link #normalize(String, String) normalization}. The default
   *  implementation returns {@link AttributeFactory#DEFAULT_ATTRIBUTE_FACTORY}. */
  protected AttributeFactory attributeFactory() {
    return AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY;
  }

   /**
    * Invoked before indexing a IndexableField instance if
    * terms have already been added to that field.  This allows custom
@@ -435,4 +531,41 @@ public abstract class Analyzer implements Closeable {
     }
   };
 
  private static final class StringTokenStream extends TokenStream {

    private final String value;
    private final int length;
    private boolean used = true;
    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);

    StringTokenStream(AttributeFactory attributeFactory, String value, int length) {
      super(attributeFactory);
      this.value = value;
      this.length = length;
    }

    @Override
    public void reset() {
      used = false;
    }

    @Override
    public boolean incrementToken() {
      if (used) {
        return false;
      }
      clearAttributes();
      termAttribute.append(value);
      offsetAttribute.setOffset(0, length);
      used = true;
      return true;
    }

    @Override
    public void end() throws IOException {
      super.end();
      offsetAttribute.setOffset(length, length);
    }
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/analysis/standard/StandardAnalyzer.java b/lucene/core/src/java/org/apache/lucene/analysis/standard/StandardAnalyzer.java
index 251017d6b2b..fb57573fb4e 100644
-- a/lucene/core/src/java/org/apache/lucene/analysis/standard/StandardAnalyzer.java
++ b/lucene/core/src/java/org/apache/lucene/analysis/standard/StandardAnalyzer.java
@@ -112,4 +112,11 @@ public final class StandardAnalyzer extends StopwordAnalyzerBase {
       }
     };
   }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
 }
diff --git a/lucene/core/src/test/org/apache/lucene/analysis/standard/TestStandardAnalyzer.java b/lucene/core/src/test/org/apache/lucene/analysis/standard/TestStandardAnalyzer.java
index 6c6ddc86cfe..2cc9274ad42 100644
-- a/lucene/core/src/test/org/apache/lucene/analysis/standard/TestStandardAnalyzer.java
++ b/lucene/core/src/test/org/apache/lucene/analysis/standard/TestStandardAnalyzer.java
@@ -27,6 +27,7 @@ import org.apache.lucene.analysis.BaseTokenStreamTestCase;
 import org.apache.lucene.analysis.MockGraphTokenFilter;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.TestUtil;
 
 public class TestStandardAnalyzer extends BaseTokenStreamTestCase {
@@ -387,4 +388,9 @@ public class TestStandardAnalyzer extends BaseTokenStreamTestCase {
     checkRandomData(random, analyzer, 100*RANDOM_MULTIPLIER, 8192);
     analyzer.close();
   }

  public void testNormalize() {
    Analyzer a = new StandardAnalyzer();
    assertEquals(new BytesRef("\"\\à3[]()! cz@"), a.normalize("dummy", "\"\\À3[]()! Cz@"));
  }
 }
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/analyzing/AnalyzingQueryParser.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/analyzing/AnalyzingQueryParser.java
deleted file mode 100644
index 49690fee265..00000000000
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/analyzing/AnalyzingQueryParser.java
++ /dev/null
@@ -1,202 +0,0 @@
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
package org.apache.lucene.queryparser.analyzing;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

/**
 * Overrides Lucene's default QueryParser so that Fuzzy-, Prefix-, Range-, and WildcardQuerys
 * are also passed through the given analyzer, but wildcard characters <code>*</code> and
 * <code>?</code> don't get removed from the search terms.
 * 
 * <p><b>Warning:</b> This class should only be used with analyzers that do not use stopwords
 * or that add tokens. Also, several stemming analyzers are inappropriate: for example, GermanAnalyzer 
 * will turn <code>H&auml;user</code> into <code>hau</code>, but <code>H?user</code> will 
 * become <code>h?user</code> when using this parser and thus no match would be found (i.e.
 * using this parser will be no improvement over QueryParser in such cases). 
 */
public class AnalyzingQueryParser extends org.apache.lucene.queryparser.classic.QueryParser {
  // gobble escaped chars or find a wildcard character 
  private final Pattern wildcardPattern = Pattern.compile("(\\.)|([?*]+)");
  public AnalyzingQueryParser(String field, Analyzer analyzer) {
    super(field, analyzer);
    setAnalyzeRangeTerms(true);
  }

  /**
   * Called when parser parses an input term that contains one or more wildcard
   * characters (like <code>*</code>), but is not a prefix term (one that has
   * just a single <code>*</code> character at the end).
   * <p>
   * Example: will be called for <code>H?user</code> or for <code>H*user</code>.
   * <p>
   * Depending on analyzer and settings, a wildcard term may (most probably will)
   * be lower-cased automatically. It <b>will</b> go through the default Analyzer.
   * <p>
   * Overrides super class, by passing terms through analyzer.
   *
   * @param  field   Name of the field query will use.
   * @param  termStr Term that contains one or more wildcard
   *                 characters (? or *), but is not simple prefix term
   *
   * @return Resulting {@link Query} built for the term
   */
  @Override
  protected Query getWildcardQuery(String field, String termStr) throws ParseException {

    if (termStr == null){
      //can't imagine this would ever happen
      throw new ParseException("Passed null value as term to getWildcardQuery");
    }
    if ( ! getAllowLeadingWildcard() && (termStr.startsWith("*") || termStr.startsWith("?"))) {
      throw new ParseException("'*' or '?' not allowed as first character in WildcardQuery"
                              + " unless getAllowLeadingWildcard() returns true");
    }
    
    Matcher wildcardMatcher = wildcardPattern.matcher(termStr);
    StringBuilder sb = new StringBuilder();
    int last = 0;
  
    while (wildcardMatcher.find()){
      // continue if escaped char
      if (wildcardMatcher.group(1) != null){
        continue;
      }
     
      if (wildcardMatcher.start() > 0){
        String chunk = termStr.substring(last, wildcardMatcher.start());
        String analyzed = analyzeSingleChunk(field, termStr, chunk);
        sb.append(analyzed);
      }
      //append the wildcard character
      sb.append(wildcardMatcher.group(2));
     
      last = wildcardMatcher.end();
    }
    if (last < termStr.length()){
      sb.append(analyzeSingleChunk(field, termStr, termStr.substring(last)));
    }
    return super.getWildcardQuery(field, sb.toString());
  }
  
  /**
   * Called when parser parses an input term
   * that uses prefix notation; that is, contains a single '*' wildcard
   * character as its last character. Since this is a special case
   * of generic wildcard term, and such a query can be optimized easily,
   * this usually results in a different query object.
   * <p>
   * Depending on analyzer and settings, a prefix term may (most probably will)
   * be lower-cased automatically. It <b>will</b> go through the default Analyzer.
   * <p>
   * Overrides super class, by passing terms through analyzer.
   *
   * @param  field   Name of the field query will use.
   * @param  termStr Term to use for building term for the query
   *                 (<b>without</b> trailing '*' character!)
   *
   * @return Resulting {@link Query} built for the term
   */
  @Override
  protected Query getPrefixQuery(String field, String termStr) throws ParseException {
    String analyzed = analyzeSingleChunk(field, termStr, termStr);
    return super.getPrefixQuery(field, analyzed);
  }

  /**
   * Called when parser parses an input term that has the fuzzy suffix (~) appended.
   * <p>
   * Depending on analyzer and settings, a fuzzy term may (most probably will)
   * be lower-cased automatically. It <b>will</b> go through the default Analyzer.
   * <p>
   * Overrides super class, by passing terms through analyzer.
   *
   * @param field Name of the field query will use.
   * @param termStr Term to use for building term for the query
   *
   * @return Resulting {@link Query} built for the term
   */
  @Override
  protected Query getFuzzyQuery(String field, String termStr, float minSimilarity)
      throws ParseException {
   
    String analyzed = analyzeSingleChunk(field, termStr, termStr);
    return super.getFuzzyQuery(field, analyzed, minSimilarity);
  }

  /**
   * Returns the analyzed form for the given chunk
   * 
   * If the analyzer produces more than one output token from the given chunk,
   * a ParseException is thrown.
   *
   * @param field The target field
   * @param termStr The full term from which the given chunk is excerpted
   * @param chunk The portion of the given termStr to be analyzed
   * @return The result of analyzing the given chunk
   * @throws ParseException when analysis returns other than one output token
   */
  protected String analyzeSingleChunk(String field, String termStr, String chunk) throws ParseException{
    String analyzed = null;
    try (TokenStream stream = getAnalyzer().tokenStream(field, chunk)) {
      stream.reset();
      CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
      // get first and hopefully only output token
      if (stream.incrementToken()) {
        analyzed = termAtt.toString();
        
        // try to increment again, there should only be one output token
        StringBuilder multipleOutputs = null;
        while (stream.incrementToken()) {
          if (null == multipleOutputs) {
            multipleOutputs = new StringBuilder();
            multipleOutputs.append('"');
            multipleOutputs.append(analyzed);
            multipleOutputs.append('"');
          }
          multipleOutputs.append(',');
          multipleOutputs.append('"');
          multipleOutputs.append(termAtt.toString());
          multipleOutputs.append('"');
        }
        stream.end();
        if (null != multipleOutputs) {
          throw new ParseException(
              String.format(getLocale(),
                  "Analyzer created multiple terms for \"%s\": %s", chunk, multipleOutputs.toString()));
        }
      } else {
        // nothing returned by analyzer.  Was it a stop word and the user accidentally
        // used an analyzer with stop words?
        stream.end();
        throw new ParseException(String.format(getLocale(), "Analyzer returned nothing for \"%s\"", chunk));
      }
    } catch (IOException e){
      throw new ParseException(
          String.format(getLocale(), "IO error while trying to analyze single term: \"%s\"", termStr));
    }
    return analyzed;
  }
}
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/classic/QueryParserBase.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/classic/QueryParserBase.java
index cdfa4776175..fbe08a90770 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/classic/QueryParserBase.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/classic/QueryParserBase.java
@@ -16,14 +16,13 @@
  */
 package org.apache.lucene.queryparser.classic;
 
import java.io.IOException;
 import java.io.StringReader;
 import java.text.DateFormat;
 import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
 import org.apache.lucene.document.DateTools;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.queryparser.classic.QueryParser.Operator;
@@ -32,6 +31,7 @@ import org.apache.lucene.search.*;
 import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.search.BooleanQuery.TooManyClauses;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
 import org.apache.lucene.util.QueryBuilder;
 import org.apache.lucene.util.automaton.RegExp;
 
@@ -41,9 +41,6 @@ import static org.apache.lucene.util.automaton.Operations.DEFAULT_MAX_DETERMINIZ
  * and acts to separate the majority of the Java code from the .jj grammar file. 
  */
 public abstract class QueryParserBase extends QueryBuilder implements CommonQueryParserConfiguration {
  
  /** Do not catch this exception in your code, it means you are using methods that you should no longer use. */
  public static class MethodRemovedUseAnother extends Throwable {}
 
   static final int CONJ_NONE   = 0;
   static final int CONJ_AND    = 1;
@@ -63,7 +60,6 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
   /** The actual operator that parser uses to combine query terms */
   Operator operator = OR_OPERATOR;
 
  boolean lowercaseExpandedTerms = true;
   MultiTermQuery.RewriteMethod multiTermRewriteMethod = MultiTermQuery.CONSTANT_SCORE_REWRITE;
   boolean allowLeadingWildcard = false;
 
@@ -79,10 +75,6 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
   // maps field names to date resolutions
   Map<String,DateTools.Resolution> fieldToDateResolution = null;
 
  //Whether or not to analyze range terms when constructing RangeQuerys
  // (For example, analyzing terms into collation keys for locale-sensitive RangeQuery)
  boolean analyzeRangeTerms = false;

   boolean autoGeneratePhraseQueries;
   int maxDeterminizedStates = DEFAULT_MAX_DETERMINIZED_STATES;
 
@@ -253,24 +245,7 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
     return operator;
   }
 

  /**
   * Whether terms of wildcard, prefix, fuzzy and range queries are to be automatically
   * lower-cased or not.  Default is <code>true</code>.
   */
  @Override
  public void setLowercaseExpandedTerms(boolean lowercaseExpandedTerms) {
    this.lowercaseExpandedTerms = lowercaseExpandedTerms;
  }

  /**
   * @see #setLowercaseExpandedTerms(boolean)
   */
  @Override
  public boolean getLowercaseExpandedTerms() {
    return lowercaseExpandedTerms;
  }

  
   /**
    * By default QueryParser uses {@link org.apache.lucene.search.MultiTermQuery#CONSTANT_SCORE_REWRITE}
    * when creating a {@link PrefixQuery}, {@link WildcardQuery} or {@link TermRangeQuery}. This implementation is generally preferable because it
@@ -378,24 +353,6 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
     return resolution;
   }
 
  /**
   * Set whether or not to analyze range terms when constructing {@link TermRangeQuery}s.
   * For example, setting this to true can enable analyzing terms into 
   * collation keys for locale-sensitive {@link TermRangeQuery}.
   * 
   * @param analyzeRangeTerms whether or not terms should be analyzed for RangeQuerys
   */
  public void setAnalyzeRangeTerms(boolean analyzeRangeTerms) {
    this.analyzeRangeTerms = analyzeRangeTerms;
  }

  /**
   * @return whether or not to analyze range terms when constructing {@link TermRangeQuery}s.
   */
  public boolean getAnalyzeRangeTerms() {
    return analyzeRangeTerms;
  }

   /**
    * @param maxDeterminizedStates the maximum number of states that
    *   determinizing a regexp query can result in.  If the query results in any
@@ -558,12 +515,6 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
                                 boolean startInclusive,
                                 boolean endInclusive) throws ParseException
   {
    if (lowercaseExpandedTerms) {
      part1 = part1==null ? null : part1.toLowerCase(locale);
      part2 = part2==null ? null : part2.toLowerCase(locale);
    }


     DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
     df.setLenient(true);
     DateTools.Resolution resolution = getDateResolution(field);
@@ -640,31 +591,6 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
     return new FuzzyQuery(term,numEdits,prefixLength);
   }
 
  // TODO: Should this be protected instead?
  private BytesRef analyzeMultitermTerm(String field, String part) {
    return analyzeMultitermTerm(field, part, getAnalyzer());
  }

  protected BytesRef analyzeMultitermTerm(String field, String part, Analyzer analyzerIn) {
    if (analyzerIn == null) analyzerIn = getAnalyzer();

    try (TokenStream source = analyzerIn.tokenStream(field, part)) {
      source.reset();
      
      TermToBytesRefAttribute termAtt = source.getAttribute(TermToBytesRefAttribute.class);

      if (!source.incrementToken())
        throw new IllegalArgumentException("analyzer returned no terms for multiTerm term: " + part);
      BytesRef bytes = BytesRef.deepCopyOf(termAtt.getBytesRef());
      if (source.incrementToken())
        throw new IllegalArgumentException("analyzer returned too many terms for multiTerm term: " + part);
      source.end();
      return bytes;
    } catch (IOException e) {
      throw new RuntimeException("Error analyzing multiTerm term: " + part, e);
    }
  }

   /**
    * Builds a new {@link TermRangeQuery} instance
    * @param field Field
@@ -681,13 +607,13 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
     if (part1 == null) {
       start = null;
     } else {
      start = analyzeRangeTerms ? analyzeMultitermTerm(field, part1) : new BytesRef(part1);
      start = getAnalyzer().normalize(field, part1);
     }
      
     if (part2 == null) {
       end = null;
     } else {
      end = analyzeRangeTerms ? analyzeMultitermTerm(field, part2) : new BytesRef(part2);
      end = getAnalyzer().normalize(field, part2);
     }
       
     final TermRangeQuery query = new TermRangeQuery(field, start, end, startInclusive, endInclusive);
@@ -767,13 +693,38 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
     }
     if (!allowLeadingWildcard && (termStr.startsWith("*") || termStr.startsWith("?")))
       throw new ParseException("'*' or '?' not allowed as first character in WildcardQuery");
    if (lowercaseExpandedTerms) {
      termStr = termStr.toLowerCase(locale);
    }
    Term t = new Term(field, termStr);

    Term t = new Term(field, analyzeWildcard(field, termStr));
     return newWildcardQuery(t);
   }
 
  private static final Pattern WILDCARD_PATTERN = Pattern.compile("(\\\\.)|([?*]+)");

  private BytesRef analyzeWildcard(String field, String termStr) {
    // best effort to not pass the wildcard characters and escaped characters through #normalize
    Matcher wildcardMatcher = WILDCARD_PATTERN.matcher(termStr);
    BytesRefBuilder sb = new BytesRefBuilder();
    int last = 0;

    while (wildcardMatcher.find()){
      if (wildcardMatcher.start() > 0) {
        String chunk = termStr.substring(last, wildcardMatcher.start());
        BytesRef normalized = getAnalyzer().normalize(field, chunk);
        sb.append(normalized);
      }
      //append the matched group - without normalizing
      sb.append(new BytesRef(wildcardMatcher.group()));

      last = wildcardMatcher.end();
    }
    if (last < termStr.length()){
      String chunk = termStr.substring(last);
      BytesRef normalized = getAnalyzer().normalize(field, chunk);
      sb.append(normalized);
    }
    return sb.toBytesRef();
  }

   /**
    * Factory method for generating a query. Called when parser
    * parses an input term token that contains a regular expression
@@ -796,10 +747,11 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
    */
   protected Query getRegexpQuery(String field, String termStr) throws ParseException
   {
    if (lowercaseExpandedTerms) {
      termStr = termStr.toLowerCase(locale);
    }
    Term t = new Term(field, termStr);
    // We need to pass the whole string to #normalize, which will not work with
    // custom attribute factories for the binary term impl, and may not work
    // with some analyzers
    BytesRef term = getAnalyzer().normalize(field, termStr);
    Term t = new Term(field, term);
     return newRegexpQuery(t);
   }
 
@@ -830,10 +782,8 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
   {
     if (!allowLeadingWildcard && termStr.startsWith("*"))
       throw new ParseException("'*' not allowed as first character in PrefixQuery");
    if (lowercaseExpandedTerms) {
      termStr = termStr.toLowerCase(locale);
    }
    Term t = new Term(field, termStr);
    BytesRef term = getAnalyzer().normalize(field, termStr);
    Term t = new Term(field, term);
     return newPrefixQuery(t);
   }
 
@@ -850,10 +800,8 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
    */
   protected Query getFuzzyQuery(String field, String termStr, float minSimilarity) throws ParseException
   {
    if (lowercaseExpandedTerms) {
      termStr = termStr.toLowerCase(locale);
    }
    Term t = new Term(field, termStr);
    BytesRef term = getAnalyzer().normalize(field, termStr);
    Term t = new Term(field, term);
     return newFuzzyQuery(t, minSimilarity, fuzzyPrefixLength);
   }
 
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/complexPhrase/ComplexPhraseQueryParser.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/complexPhrase/ComplexPhraseQueryParser.java
index ac808d7a364..1a7e5e108c1 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/complexPhrase/ComplexPhraseQueryParser.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/complexPhrase/ComplexPhraseQueryParser.java
@@ -33,9 +33,9 @@ import org.apache.lucene.search.BoostQuery;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.MatchNoDocsQuery;
 import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.MultiTermQuery.RewriteMethod;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
 import org.apache.lucene.search.spans.SpanBoostQuery;
 import org.apache.lucene.search.spans.SpanNearQuery;
 import org.apache.lucene.search.spans.SpanNotQuery;
@@ -186,14 +186,15 @@ public class ComplexPhraseQueryParser extends QueryParser {
   @Override
   protected Query newRangeQuery(String field, String part1, String part2,
       boolean startInclusive, boolean endInclusive) {
    if (isPass2ResolvingPhrases) {
      // Must use old-style RangeQuery in order to produce a BooleanQuery
      // that can be turned into SpanOr clause
      TermRangeQuery rangeQuery = TermRangeQuery.newStringRange(field, part1, part2, startInclusive, endInclusive);
      rangeQuery.setRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_REWRITE);
      return rangeQuery;
    RewriteMethod originalRewriteMethod = getMultiTermRewriteMethod();
    try {
      if (isPass2ResolvingPhrases) {
        setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_REWRITE);
      }
      return super.newRangeQuery(field, part1, part2, startInclusive, endInclusive);
    } finally {
      setMultiTermRewriteMethod(originalRewriteMethod);
     }
    return super.newRangeQuery(field, part1, part2, startInclusive, endInclusive);
   }
 
   @Override
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/CommonQueryParserConfiguration.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/CommonQueryParserConfiguration.java
index 55e43cd1f65..c44e9e0e651 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/CommonQueryParserConfiguration.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/CommonQueryParserConfiguration.java
@@ -31,18 +31,6 @@ import org.apache.lucene.search.MultiTermQuery;
  */
 public interface CommonQueryParserConfiguration {
   
  /**
   * Whether terms of multi-term queries (e.g., wildcard,
   * prefix, fuzzy and range) should be automatically
   * lower-cased or not.  Default is <code>true</code>.
   */
  public void setLowercaseExpandedTerms(boolean lowercaseExpandedTerms);
  
  /**
   * @see #setLowercaseExpandedTerms(boolean)
   */
  public boolean getLowercaseExpandedTerms();
  
   /**
    * Set to <code>true</code> to allow leading wildcard characters.
    * <p>
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/StandardQueryParser.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/StandardQueryParser.java
index 2cd8084e809..32cbd0233b3 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/StandardQueryParser.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/StandardQueryParser.java
@@ -180,36 +180,6 @@ public class StandardQueryParser extends QueryParserHelper implements CommonQuer
     getQueryConfigHandler().set(ConfigurationKeys.DEFAULT_OPERATOR, operator);
   }
   
  /**
   * Set to <code>true</code> to allow leading wildcard characters.
   * <p>
   * When set, <code>*</code> or <code>?</code> are allowed as the first
   * character of a PrefixQuery and WildcardQuery. Note that this can produce
   * very slow queries on big indexes.
   * <p>
   * Default: false.
   */
  @Override
  public void setLowercaseExpandedTerms(boolean lowercaseExpandedTerms) {
    getQueryConfigHandler().set(ConfigurationKeys.LOWERCASE_EXPANDED_TERMS, lowercaseExpandedTerms);
  }
  
  /**
   * @see #setLowercaseExpandedTerms(boolean)
   */
  @Override
  public boolean getLowercaseExpandedTerms() {
    Boolean lowercaseExpandedTerms = getQueryConfigHandler().get(ConfigurationKeys.LOWERCASE_EXPANDED_TERMS);
    
    if (lowercaseExpandedTerms == null) {
      return true;
      
    } else {
      return lowercaseExpandedTerms;
    }
    
  }
  
   /**
    * Set to <code>true</code> to allow leading wildcard characters.
    * <p>
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/config/StandardQueryConfigHandler.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/config/StandardQueryConfigHandler.java
index bba95eed91f..5c53d02f2ed 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/config/StandardQueryConfigHandler.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/config/StandardQueryConfigHandler.java
@@ -55,14 +55,6 @@ public class StandardQueryConfigHandler extends QueryConfigHandler {
      * @see StandardQueryParser#getEnablePositionIncrements()
      */
     final public static ConfigurationKey<Boolean> ENABLE_POSITION_INCREMENTS = ConfigurationKey.newInstance();
    
    /**
     * Key used to set whether expanded terms should be lower-cased
     * 
     * @see StandardQueryParser#setLowercaseExpandedTerms(boolean)
     * @see StandardQueryParser#getLowercaseExpandedTerms()
     */
    final public static ConfigurationKey<Boolean> LOWERCASE_EXPANDED_TERMS = ConfigurationKey.newInstance();
 
     /**
      * Key used to set whether leading wildcards are supported
@@ -223,7 +215,6 @@ public class StandardQueryConfigHandler extends QueryConfigHandler {
     set(ConfigurationKeys.ANALYZER, null); //default value 2.4
     set(ConfigurationKeys.DEFAULT_OPERATOR, Operator.OR);
     set(ConfigurationKeys.PHRASE_SLOP, 0); //default value 2.4
    set(ConfigurationKeys.LOWERCASE_EXPANDED_TERMS, true); //default value 2.4
     set(ConfigurationKeys.ENABLE_POSITION_INCREMENTS, false); //default value 2.4
     set(ConfigurationKeys.FIELD_BOOST_MAP, new LinkedHashMap<String, Float>());
     set(ConfigurationKeys.FUZZY_CONFIG, new FuzzyConfig());
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/FuzzyQueryNodeProcessor.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/FuzzyQueryNodeProcessor.java
index 0b8a9a72c29..9479fcf65a3 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/FuzzyQueryNodeProcessor.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/FuzzyQueryNodeProcessor.java
@@ -18,6 +18,7 @@ package org.apache.lucene.queryparser.flexible.standard.processors;
 
 import java.util.List;
 
import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
 import org.apache.lucene.queryparser.flexible.core.config.QueryConfigHandler;
 import org.apache.lucene.queryparser.flexible.core.nodes.FuzzyQueryNode;
@@ -55,9 +56,17 @@ public class FuzzyQueryNodeProcessor extends QueryNodeProcessorImpl {
       FuzzyQueryNode fuzzyNode = (FuzzyQueryNode) node;
       QueryConfigHandler config = getQueryConfigHandler();
 
      Analyzer analyzer = getQueryConfigHandler().get(ConfigurationKeys.ANALYZER);
      if (analyzer != null) {
        // because we call utf8ToString, this will only work with the default TermToBytesRefAttribute
        String text = fuzzyNode.getTextAsString();
        text = analyzer.normalize(fuzzyNode.getFieldAsString(), text).utf8ToString();
        fuzzyNode.setText(text);
      }

       FuzzyConfig fuzzyConfig = null;
       
      if (config != null && (fuzzyConfig = config.get(ConfigurationKeys.FUZZY_CONFIG)) != null) {
      if ((fuzzyConfig = config.get(ConfigurationKeys.FUZZY_CONFIG)) != null) {
         fuzzyNode.setPrefixLength(fuzzyConfig.getPrefixLength());
 
         if (fuzzyNode.getSimilarity() < 0) {
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/LowercaseExpandedTermsQueryNodeProcessor.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/LowercaseExpandedTermsQueryNodeProcessor.java
deleted file mode 100644
index 3bb207584bb..00000000000
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/LowercaseExpandedTermsQueryNodeProcessor.java
++ /dev/null
@@ -1,100 +0,0 @@
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
package org.apache.lucene.queryparser.flexible.standard.processors;

import java.util.List;
import java.util.Locale;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.config.QueryConfigHandler;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FuzzyQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.RangeQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.TextableQueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryparser.flexible.core.util.UnescapedCharSequence;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.ConfigurationKeys;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.WildcardQueryNode;

/**
 * This processor verifies if 
 * {@link ConfigurationKeys#LOWERCASE_EXPANDED_TERMS} is defined in the
 * {@link QueryConfigHandler}. If it is and the expanded terms should be
 * lower-cased, it looks for every {@link WildcardQueryNode},
 * {@link FuzzyQueryNode} and children of a {@link RangeQueryNode} and lower-case its
 * term.
 * 
 * @see ConfigurationKeys#LOWERCASE_EXPANDED_TERMS
 */
public class LowercaseExpandedTermsQueryNodeProcessor extends
    QueryNodeProcessorImpl {

  public LowercaseExpandedTermsQueryNodeProcessor() {
  }

  @Override
  public QueryNode process(QueryNode queryTree) throws QueryNodeException {
    Boolean lowercaseExpandedTerms = getQueryConfigHandler().get(ConfigurationKeys.LOWERCASE_EXPANDED_TERMS);

    if (lowercaseExpandedTerms != null && lowercaseExpandedTerms) {
      return super.process(queryTree);
    }

    return queryTree;

  }

  @Override
  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
    
    Locale locale = getQueryConfigHandler().get(ConfigurationKeys.LOCALE);
    if (locale == null) {
      locale = Locale.getDefault();
    }

    if (node instanceof WildcardQueryNode
        || node instanceof FuzzyQueryNode
        || (node instanceof FieldQueryNode && node.getParent() instanceof RangeQueryNode)
        || node instanceof RegexpQueryNode) {

      TextableQueryNode txtNode = (TextableQueryNode) node;
      CharSequence text = txtNode.getText();
      txtNode.setText(text != null ? UnescapedCharSequence.toLowerCase(text, locale) : null);
    }

    return node;

  }

  @Override
  protected QueryNode preProcessNode(QueryNode node) throws QueryNodeException {

    return node;

  }

  @Override
  protected List<QueryNode> setChildrenOrder(List<QueryNode> children)
      throws QueryNodeException {

    return children;

  }

}
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/RegexpQueryNodeProcessor.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/RegexpQueryNodeProcessor.java
new file mode 100644
index 00000000000..652de875861
-- /dev/null
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/RegexpQueryNodeProcessor.java
@@ -0,0 +1,56 @@
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
package org.apache.lucene.queryparser.flexible.standard.processors;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.ConfigurationKeys;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;

/** Processor for Regexp queries. */
public class RegexpQueryNodeProcessor extends QueryNodeProcessorImpl {

  @Override
  protected QueryNode preProcessNode(QueryNode node) throws QueryNodeException {
    return node;
  }

  @Override
  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
    if (node instanceof RegexpQueryNode) {
      RegexpQueryNode regexpNode = (RegexpQueryNode) node;
      Analyzer analyzer = getQueryConfigHandler().get(ConfigurationKeys.ANALYZER);
      if (analyzer != null) {
        String text = regexpNode.getText().toString();
        // because we call utf8ToString, this will only work with the default TermToBytesRefAttribute
        text = analyzer.normalize(regexpNode.getFieldAsString(), text).utf8ToString();
        regexpNode.setText(text);
      }
    }
    return node;
  }

  @Override
  protected List<QueryNode> setChildrenOrder(List<QueryNode> children) throws QueryNodeException {
    return children;
  }

}
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/StandardQueryNodeProcessorPipeline.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/StandardQueryNodeProcessorPipeline.java
index 38a9a4715c0..5b681b41931 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/StandardQueryNodeProcessorPipeline.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/StandardQueryNodeProcessorPipeline.java
@@ -47,16 +47,16 @@ public class StandardQueryNodeProcessorPipeline extends
   public StandardQueryNodeProcessorPipeline(QueryConfigHandler queryConfig) {
     super(queryConfig);
 
    add(new WildcardQueryNodeProcessor());    
    add(new WildcardQueryNodeProcessor());   
     add(new MultiFieldQueryNodeProcessor());
     add(new FuzzyQueryNodeProcessor());
    add(new RegexpQueryNodeProcessor());
     add(new MatchAllDocsQueryNodeProcessor());
     add(new OpenRangeQueryNodeProcessor());
     add(new LegacyNumericQueryNodeProcessor());
     add(new LegacyNumericRangeQueryNodeProcessor());
     add(new PointQueryNodeProcessor());
     add(new PointRangeQueryNodeProcessor());
    add(new LowercaseExpandedTermsQueryNodeProcessor());
     add(new TermRangeQueryNodeProcessor());
     add(new AllowLeadingWildcardProcessor());    
     add(new AnalyzerQueryNodeProcessor());
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/TermRangeQueryNodeProcessor.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/TermRangeQueryNodeProcessor.java
index f9a45833bbf..557c605c159 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/TermRangeQueryNodeProcessor.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/TermRangeQueryNodeProcessor.java
@@ -23,6 +23,7 @@ import java.util.List;
 import java.util.Locale;
 import java.util.TimeZone;
 
import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.document.DateTools;
 import org.apache.lucene.document.DateTools.Resolution;
 import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
@@ -134,7 +135,15 @@ public class TermRangeQueryNodeProcessor extends QueryNodeProcessorImpl {
         }
         
       } catch (Exception e) {
        // do nothing
        // not a date
        Analyzer analyzer = getQueryConfigHandler().get(ConfigurationKeys.ANALYZER);
        if (analyzer != null) {
          // because we call utf8ToString, this will only work with the default TermToBytesRefAttribute
          part1 = analyzer.normalize(lower.getFieldAsString(), part1).utf8ToString();
          part2 = analyzer.normalize(lower.getFieldAsString(), part2).utf8ToString();
          lower.setText(part1);
          upper.setText(part2);
        }
       }
       
     }
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/WildcardQueryNodeProcessor.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/WildcardQueryNodeProcessor.java
index 718257500b7..39eb0df13ed 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/WildcardQueryNodeProcessor.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/flexible/standard/processors/WildcardQueryNodeProcessor.java
@@ -17,7 +17,10 @@
 package org.apache.lucene.queryparser.flexible.standard.processors;
 
 import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
 import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
 import org.apache.lucene.queryparser.flexible.core.nodes.FuzzyQueryNode;
@@ -25,11 +28,13 @@ import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
 import org.apache.lucene.queryparser.flexible.core.nodes.QuotedFieldQueryNode;
 import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
 import org.apache.lucene.queryparser.flexible.core.util.UnescapedCharSequence;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler.ConfigurationKeys;
 import org.apache.lucene.queryparser.flexible.standard.nodes.PrefixWildcardQueryNode;
 import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;
 import org.apache.lucene.queryparser.flexible.standard.nodes.WildcardQueryNode;
 import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
 import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.util.BytesRef;
 
 /**
  * The {@link StandardSyntaxParser} creates {@link PrefixWildcardQueryNode} nodes which
@@ -43,6 +48,39 @@ import org.apache.lucene.search.PrefixQuery;
  */
 public class WildcardQueryNodeProcessor extends QueryNodeProcessorImpl {
 
  private static final Pattern WILDCARD_PATTERN = Pattern.compile("(\\.)|([?*]+)");

  // because we call utf8ToString, this will only work with the default TermToBytesRefAttribute
  private static String analyzeWildcard(Analyzer a, String field, String wildcard) {
    // best effort to not pass the wildcard characters through #normalize
    Matcher wildcardMatcher = WILDCARD_PATTERN.matcher(wildcard);
    StringBuilder sb = new StringBuilder();
    int last = 0;

    while (wildcardMatcher.find()){
      // continue if escaped char
      if (wildcardMatcher.group(1) != null){
        continue;
      }

      if (wildcardMatcher.start() > 0){
        String chunk = wildcard.substring(last, wildcardMatcher.start());
        BytesRef normalized = a.normalize(field, chunk);
        sb.append(normalized.utf8ToString());
      }
      //append the wildcard character
      sb.append(wildcardMatcher.group(2));

      last = wildcardMatcher.end();
    }
    if (last < wildcard.length()){
      String chunk = wildcard.substring(last);
      BytesRef normalized = a.normalize(field, chunk);
      sb.append(normalized.utf8ToString());
    }
    return sb.toString();
  }

   public WildcardQueryNodeProcessor() {
     // empty constructor
   }
@@ -67,15 +105,19 @@ public class WildcardQueryNodeProcessor extends QueryNodeProcessorImpl {
       
       // Code below simulates the old lucene parser behavior for wildcards
       
      if (isPrefixWildcard(text)) {        
        PrefixWildcardQueryNode prefixWildcardQN = new PrefixWildcardQueryNode(fqn);
        return prefixWildcardQN;
        
      } else if (isWildcard(text)){
        WildcardQueryNode wildcardQN = new WildcardQueryNode(fqn);
        return wildcardQN;
      
      if (isWildcard(text)) {
        Analyzer analyzer = getQueryConfigHandler().get(ConfigurationKeys.ANALYZER);
        if (analyzer != null) {
          text = analyzeWildcard(analyzer, fqn.getFieldAsString(), text.toString());
        }
        if (isPrefixWildcard(text)) {
          return new PrefixWildcardQueryNode(fqn.getField(), text, fqn.getBegin(), fqn.getEnd());
        } else {
          return new WildcardQueryNode(fqn.getField(), text, fqn.getBegin(), fqn.getEnd());
        }
       }
             

     }
 
     return node;
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/simple/SimpleQueryParser.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/simple/SimpleQueryParser.java
index 3f9d9a42680..a417d1b889c 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/simple/SimpleQueryParser.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/simple/SimpleQueryParser.java
@@ -26,6 +26,7 @@ import org.apache.lucene.search.MatchAllDocsQuery;
 import org.apache.lucene.search.MatchNoDocsQuery;
 import org.apache.lucene.search.PrefixQuery;
 import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.QueryBuilder;
 import org.apache.lucene.util.automaton.LevenshteinAutomata;
 
@@ -548,7 +549,9 @@ public class SimpleQueryParser extends QueryBuilder {
   protected Query newFuzzyQuery(String text, int fuzziness) {
     BooleanQuery.Builder bq = new BooleanQuery.Builder();
     for (Map.Entry<String,Float> entry : weights.entrySet()) {
      Query q = new FuzzyQuery(new Term(entry.getKey(), text), fuzziness);
      final String fieldName = entry.getKey();
      final BytesRef term = getAnalyzer().normalize(fieldName, text);
      Query q = new FuzzyQuery(new Term(fieldName, term), fuzziness);
       float boost = entry.getValue();
       if (boost != 1f) {
         q = new BoostQuery(q, boost);
@@ -582,7 +585,9 @@ public class SimpleQueryParser extends QueryBuilder {
   protected Query newPrefixQuery(String text) {
     BooleanQuery.Builder bq = new BooleanQuery.Builder();
     for (Map.Entry<String,Float> entry : weights.entrySet()) {
      Query q = new PrefixQuery(new Term(entry.getKey(), text));
      final String fieldName = entry.getKey();
      final BytesRef term = getAnalyzer().normalize(fieldName, text);
      Query q = new PrefixQuery(new Term(fieldName, term));
       float boost = entry.getValue();
       if (boost != 1f) {
         q = new BoostQuery(q, boost);
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java
deleted file mode 100644
index bf5f69f7b61..00000000000
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java
++ /dev/null
@@ -1,268 +0,0 @@
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
package org.apache.lucene.queryparser.analyzing;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockBytesAnalyzer;
import org.apache.lucene.analysis.MockTokenFilter;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;

/**
 */
public class TestAnalyzingQueryParser extends LuceneTestCase {
  private final static String FIELD = "field";
   
  private Analyzer a;

  private String[] wildcardInput;
  private String[] wildcardExpected;
  private String[] prefixInput;
  private String[] prefixExpected;
  private String[] rangeInput;
  private String[] rangeExpected;
  private String[] fuzzyInput;
  private String[] fuzzyExpected;

  private Map<String, String> wildcardEscapeHits = new TreeMap<>();
  private Map<String, String> wildcardEscapeMisses = new TreeMap<>();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    wildcardInput = new String[] { "*bersetzung über*ung",
        "Mötley Cr\u00fce Mötl?* Crü?", "Renée Zellweger Ren?? Zellw?ger" };
    wildcardExpected = new String[] { "*bersetzung uber*ung", "motley crue motl?* cru?",
        "renee zellweger ren?? zellw?ger" };

    prefixInput = new String[] { "übersetzung übersetz*",
        "Mötley Crüe Mötl* crü*", "René? Zellw*" };
    prefixExpected = new String[] { "ubersetzung ubersetz*", "motley crue motl* cru*",
        "rene? zellw*" };

    rangeInput = new String[] { "[aa TO bb]", "{Anaïs TO Zoé}" };
    rangeExpected = new String[] { "[aa TO bb]", "{anais TO zoe}" };

    fuzzyInput = new String[] { "Übersetzung Übersetzung~0.9",
        "Mötley Crüe Mötley~0.75 Crüe~0.5",
        "Renée Zellweger Renée~0.9 Zellweger~" };
    fuzzyExpected = new String[] { "ubersetzung ubersetzung~1",
        "motley crue motley~1 crue~2", "renee zellweger renee~0 zellweger~2" };

    wildcardEscapeHits.put("mö*tley", "moatley");

    // need to have at least one genuine wildcard to trigger the wildcard analysis
    // hence the * before the y
    wildcardEscapeHits.put("mö\\*tl*y", "mo*tley");

    // escaped backslash then true wildcard
    wildcardEscapeHits.put("mö\\\\*tley", "mo\\atley");
    
    // escaped wildcard then true wildcard
    wildcardEscapeHits.put("mö\\??ley", "mo?tley");

    // the first is an escaped * which should yield a miss
    wildcardEscapeMisses.put("mö\\*tl*y", "moatley");
      
    a = new ASCIIAnalyzer();
  }

  public void testSingleChunkExceptions() {
    String termStr = "the*tre";
      
    Analyzer stopsAnalyzer = new MockAnalyzer
        (random(), MockTokenizer.WHITESPACE, true, MockTokenFilter.ENGLISH_STOPSET);

    ParseException expected = expectThrows(ParseException.class, () -> {
      parseWithAnalyzingQueryParser(termStr, stopsAnalyzer, true);
    });
    assertTrue(expected.getMessage().contains("returned nothing"));
     
    AnalyzingQueryParser qp = new AnalyzingQueryParser(FIELD, a);
    expected = expectThrows(ParseException.class, () -> {
      qp.analyzeSingleChunk(FIELD, "", "not a single chunk");
    });
    assertTrue(expected.getMessage().contains("multiple terms"));
  }
   
  public void testWildcardAlone() throws ParseException {
    //seems like crazy edge case, but can be useful in concordance 
    expectThrows(ParseException.class, () -> {
      getAnalyzedQuery("*", a, false);
    });
      
    String qString = parseWithAnalyzingQueryParser("*", a, true);
    assertEquals("Every word", "*", qString);
  }
  public void testWildCardEscapes() throws ParseException, IOException {

    for (Map.Entry<String, String> entry : wildcardEscapeHits.entrySet()){
      Query q = getAnalyzedQuery(entry.getKey(), a, false);
      assertEquals("WildcardEscapeHits: " + entry.getKey(), true, isAHit(q, entry.getValue(), a));
    }
    for (Map.Entry<String, String> entry : wildcardEscapeMisses.entrySet()){
      Query q = getAnalyzedQuery(entry.getKey(), a, false);
      assertEquals("WildcardEscapeMisses: " + entry.getKey(), false, isAHit(q, entry.getValue(), a));
    }

  }
  public void testWildCardQueryNoLeadingAllowed() {
    expectThrows(ParseException.class, () -> {
      parseWithAnalyzingQueryParser(wildcardInput[0], a, false);
    });
  }

  public void testWildCardQuery() throws ParseException {
    for (int i = 0; i < wildcardInput.length; i++) {
      assertEquals("Testing wildcards with analyzer " + a.getClass() + ", input string: "
          + wildcardInput[i], wildcardExpected[i], parseWithAnalyzingQueryParser(wildcardInput[i], a, true));
    }
  }


  public void testPrefixQuery() throws ParseException {
    for (int i = 0; i < prefixInput.length; i++) {
      assertEquals("Testing prefixes with analyzer " + a.getClass() + ", input string: "
          + prefixInput[i], prefixExpected[i], parseWithAnalyzingQueryParser(prefixInput[i], a, false));
    }
  }

  public void testRangeQuery() throws ParseException {
    for (int i = 0; i < rangeInput.length; i++) {
      assertEquals("Testing ranges with analyzer " + a.getClass() + ", input string: "
          + rangeInput[i], rangeExpected[i], parseWithAnalyzingQueryParser(rangeInput[i], a, false));
    }
  }

  public void testFuzzyQuery() throws ParseException {
    for (int i = 0; i < fuzzyInput.length; i++) {
      assertEquals("Testing fuzzys with analyzer " + a.getClass() + ", input string: "
          + fuzzyInput[i], fuzzyExpected[i], parseWithAnalyzingQueryParser(fuzzyInput[i], a, false));
    }
  }


  private String parseWithAnalyzingQueryParser(String s, Analyzer a, boolean allowLeadingWildcard) throws ParseException {
    Query q = getAnalyzedQuery(s, a, allowLeadingWildcard);
    return q.toString(FIELD);
  }

  private Query getAnalyzedQuery(String s, Analyzer a, boolean allowLeadingWildcard) throws ParseException {
    AnalyzingQueryParser qp = new AnalyzingQueryParser(FIELD, a);
    qp.setAllowLeadingWildcard(allowLeadingWildcard);
    org.apache.lucene.search.Query q = qp.parse(s);
    return q;
  }

  final static class FoldingFilter extends TokenFilter {
    final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public FoldingFilter(TokenStream input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      if (input.incrementToken()) {
        char term[] = termAtt.buffer();
        for (int i = 0; i < term.length; i++)
          switch(term[i]) {
            case 'ü':
              term[i] = 'u'; 
              break;
            case 'ö': 
              term[i] = 'o'; 
              break;
            case 'é': 
              term[i] = 'e'; 
              break;
            case 'ï': 
              term[i] = 'i'; 
              break;
          }
        return true;
      } else {
        return false;
      }
    }
  }

  final static class ASCIIAnalyzer extends Analyzer {
    @Override
    public TokenStreamComponents createComponents(String fieldName) {
      Tokenizer result = new MockTokenizer(MockTokenizer.WHITESPACE, true);
      return new TokenStreamComponents(result, new FoldingFilter(result));
    }
  }
   

  // LUCENE-4176
  public void testByteTerms() throws Exception {
    String s = "เข";
    Analyzer analyzer = new MockBytesAnalyzer();
    QueryParser qp = new AnalyzingQueryParser(FIELD, analyzer);
    Query q = qp.parse("[เข TO เข]");
    assertEquals(true, isAHit(q, s, analyzer));
  }
   
  
  private boolean isAHit(Query q, String content, Analyzer analyzer) throws IOException{
    Directory ramDir = newDirectory();
    RandomIndexWriter writer = new RandomIndexWriter(random(), ramDir, analyzer);
    Document doc = new Document();
    FieldType fieldType = new FieldType();
    fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    fieldType.setTokenized(true);
    fieldType.setStored(true);
    Field field = new Field(FIELD, content, fieldType);
    doc.add(field);
    writer.addDocument(doc);
    writer.close();
    DirectoryReader ir = DirectoryReader.open(ramDir);
    IndexSearcher is = new IndexSearcher(ir);
      
    int hits = is.search(q, 10).totalHits;
    ir.close();
    ramDir.close();
    if (hits == 1){
      return true;
    } else {
      return false;
    }

  }
}
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java
index 7a988004b31..de90e29affe 100644
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java
++ b/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java
@@ -18,6 +18,8 @@ package org.apache.lucene.queryparser.classic;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockBytesAnalyzer;
import org.apache.lucene.analysis.MockLowerCaseFilter;
 import org.apache.lucene.analysis.MockSynonymAnalyzer;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
@@ -25,7 +27,13 @@ import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
 import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.queryparser.classic.QueryParser.Operator;
 import org.apache.lucene.queryparser.flexible.standard.CommonQueryParserConfiguration;
@@ -33,11 +41,14 @@ import org.apache.lucene.queryparser.util.QueryParserTestBase;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
 import org.apache.lucene.search.MultiPhraseQuery;
 import org.apache.lucene.search.PhraseQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.SynonymQuery;
 import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.automaton.TooComplexToDeterminizeException;
 
 import java.io.IOException;
@@ -48,6 +59,7 @@ import java.io.IOException;
 public class TestQueryParser extends QueryParserTestBase {
 
   protected boolean splitOnWhitespace = QueryParser.DEFAULT_SPLIT_ON_WHITESPACE;
  private static final String FIELD = "field";
 
   public static class QPTestParser extends QueryParser {
     public QPTestParser(String f, Analyzer a) {
@@ -114,14 +126,6 @@ public class TestQueryParser extends QueryParserTestBase {
     qp.setDefaultOperator(Operator.AND);
   }
   
  @Override
  public void setAnalyzeRangeTerms(CommonQueryParserConfiguration cqpC,
      boolean value) {
    assert (cqpC instanceof QueryParser);
    QueryParser qp = (QueryParser) cqpC;
    qp.setAnalyzeRangeTerms(value);
  }
  
   @Override
   public void setAutoGeneratePhraseQueries(CommonQueryParserConfiguration cqpC,
       boolean value) {
@@ -200,7 +204,7 @@ public class TestQueryParser extends QueryParserTestBase {
   @Override
   public void testStarParsing() throws Exception {
     final int[] type = new int[1];
    QueryParser qp = new QueryParser("field",
    QueryParser qp = new QueryParser(FIELD,
         new MockAnalyzer(random(), MockTokenizer.WHITESPACE, false)) {
       @Override
       protected Query getWildcardQuery(String field, String termStr) {
@@ -285,7 +289,7 @@ public class TestQueryParser extends QueryParserTestBase {
     Analyzer morePrecise = new Analyzer2();
     
     public SmartQueryParser() {
      super("field", new Analyzer1());
      super(FIELD, new Analyzer1());
     }
     
     @Override
@@ -299,9 +303,9 @@ public class TestQueryParser extends QueryParserTestBase {
   @Override
   public void testNewFieldQuery() throws Exception {
     /** ordinary behavior, synonyms form uncoordinated boolean query */
    QueryParser dumb = new QueryParser("field",
    QueryParser dumb = new QueryParser(FIELD,
         new Analyzer1());
    Query expanded = new SynonymQuery(new Term("field", "dogs"), new Term("field", "dog"));
    Query expanded = new SynonymQuery(new Term(FIELD, "dogs"), new Term(FIELD, "dog"));
     assertEquals(expanded, dumb.parse("\"dogs\""));
     /** even with the phrase operator the behavior is the same */
     assertEquals(expanded, dumb.parse("dogs"));
@@ -312,14 +316,14 @@ public class TestQueryParser extends QueryParserTestBase {
     QueryParser smart = new SmartQueryParser();
     assertEquals(expanded, smart.parse("dogs"));
     
    Query unexpanded = new TermQuery(new Term("field", "dogs"));
    Query unexpanded = new TermQuery(new Term(FIELD, "dogs"));
     assertEquals(unexpanded, smart.parse("\"dogs\""));
   }
 
   /** simple synonyms test */
   public void testSynonyms() throws Exception {
    Query expected = new SynonymQuery(new Term("field", "dogs"), new Term("field", "dog"));
    QueryParser qp = new QueryParser("field", new MockSynonymAnalyzer());
    Query expected = new SynonymQuery(new Term(FIELD, "dogs"), new Term(FIELD, "dog"));
    QueryParser qp = new QueryParser(FIELD, new MockSynonymAnalyzer());
     assertEquals(expected, qp.parse("dogs"));
     assertEquals(expected, qp.parse("\"dogs\""));
     qp.setDefaultOperator(Operator.AND);
@@ -333,9 +337,9 @@ public class TestQueryParser extends QueryParserTestBase {
   /** forms multiphrase query */
   public void testSynonymsPhrase() throws Exception {
     MultiPhraseQuery.Builder expectedQBuilder = new MultiPhraseQuery.Builder();
    expectedQBuilder.add(new Term("field", "old"));
    expectedQBuilder.add(new Term[] { new Term("field", "dogs"), new Term("field", "dog") });
    QueryParser qp = new QueryParser("field", new MockSynonymAnalyzer());
    expectedQBuilder.add(new Term(FIELD, "old"));
    expectedQBuilder.add(new Term[] { new Term(FIELD, "dogs"), new Term(FIELD, "dog") });
    QueryParser qp = new QueryParser(FIELD, new MockSynonymAnalyzer());
     assertEquals(expectedQBuilder.build(), qp.parse("\"old dogs\""));
     qp.setDefaultOperator(Operator.AND);
     assertEquals(expectedQBuilder.build(), qp.parse("\"old dogs\""));
@@ -387,8 +391,8 @@ public class TestQueryParser extends QueryParserTestBase {
   
   /** simple CJK synonym test */
   public void testCJKSynonym() throws Exception {
    Query expected = new SynonymQuery(new Term("field", "国"), new Term("field", "國"));
    QueryParser qp = new QueryParser("field", new MockCJKSynonymAnalyzer());
    Query expected = new SynonymQuery(new Term(FIELD, "国"), new Term(FIELD, "國"));
    QueryParser qp = new QueryParser(FIELD, new MockCJKSynonymAnalyzer());
     assertEquals(expected, qp.parse("国"));
     qp.setDefaultOperator(Operator.AND);
     assertEquals(expected, qp.parse("国"));
@@ -399,11 +403,11 @@ public class TestQueryParser extends QueryParserTestBase {
   /** synonyms with default OR operator */
   public void testCJKSynonymsOR() throws Exception {
     BooleanQuery.Builder expectedB = new BooleanQuery.Builder();
    expectedB.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    Query inner = new SynonymQuery(new Term("field", "国"), new Term("field", "國"));
    expectedB.add(new TermQuery(new Term(FIELD, "中")), BooleanClause.Occur.SHOULD);
    Query inner = new SynonymQuery(new Term(FIELD, "国"), new Term(FIELD, "國"));
     expectedB.add(inner, BooleanClause.Occur.SHOULD);
     Query expected = expectedB.build();
    QueryParser qp = new QueryParser("field", new MockCJKSynonymAnalyzer());
    QueryParser qp = new QueryParser(FIELD, new MockCJKSynonymAnalyzer());
     assertEquals(expected, qp.parse("中国"));
     expected = new BoostQuery(expected, 2f);
     assertEquals(expected, qp.parse("中国^2"));
@@ -412,13 +416,13 @@ public class TestQueryParser extends QueryParserTestBase {
   /** more complex synonyms with default OR operator */
   public void testCJKSynonymsOR2() throws Exception {
     BooleanQuery.Builder expectedB = new BooleanQuery.Builder();
    expectedB.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    SynonymQuery inner = new SynonymQuery(new Term("field", "国"), new Term("field", "國"));
    expectedB.add(new TermQuery(new Term(FIELD, "中")), BooleanClause.Occur.SHOULD);
    SynonymQuery inner = new SynonymQuery(new Term(FIELD, "国"), new Term(FIELD, "國"));
     expectedB.add(inner, BooleanClause.Occur.SHOULD);
    SynonymQuery inner2 = new SynonymQuery(new Term("field", "国"), new Term("field", "國"));
    SynonymQuery inner2 = new SynonymQuery(new Term(FIELD, "国"), new Term(FIELD, "國"));
     expectedB.add(inner2, BooleanClause.Occur.SHOULD);
     Query expected = expectedB.build();
    QueryParser qp = new QueryParser("field", new MockCJKSynonymAnalyzer());
    QueryParser qp = new QueryParser(FIELD, new MockCJKSynonymAnalyzer());
     assertEquals(expected, qp.parse("中国国"));
     expected = new BoostQuery(expected, 2f);
     assertEquals(expected, qp.parse("中国国^2"));
@@ -427,11 +431,11 @@ public class TestQueryParser extends QueryParserTestBase {
   /** synonyms with default AND operator */
   public void testCJKSynonymsAND() throws Exception {
     BooleanQuery.Builder expectedB = new BooleanQuery.Builder();
    expectedB.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.MUST);
    Query inner = new SynonymQuery(new Term("field", "国"), new Term("field", "國"));
    expectedB.add(new TermQuery(new Term(FIELD, "中")), BooleanClause.Occur.MUST);
    Query inner = new SynonymQuery(new Term(FIELD, "国"), new Term(FIELD, "國"));
     expectedB.add(inner, BooleanClause.Occur.MUST);
     Query expected = expectedB.build();
    QueryParser qp = new QueryParser("field", new MockCJKSynonymAnalyzer());
    QueryParser qp = new QueryParser(FIELD, new MockCJKSynonymAnalyzer());
     qp.setDefaultOperator(Operator.AND);
     assertEquals(expected, qp.parse("中国"));
     expected = new BoostQuery(expected, 2f);
@@ -441,13 +445,13 @@ public class TestQueryParser extends QueryParserTestBase {
   /** more complex synonyms with default AND operator */
   public void testCJKSynonymsAND2() throws Exception {
     BooleanQuery.Builder expectedB = new BooleanQuery.Builder();
    expectedB.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.MUST);
    Query inner = new SynonymQuery(new Term("field", "国"), new Term("field", "國"));
    expectedB.add(new TermQuery(new Term(FIELD, "中")), BooleanClause.Occur.MUST);
    Query inner = new SynonymQuery(new Term(FIELD, "国"), new Term(FIELD, "國"));
     expectedB.add(inner, BooleanClause.Occur.MUST);
    Query inner2 = new SynonymQuery(new Term("field", "国"), new Term("field", "國"));
    Query inner2 = new SynonymQuery(new Term(FIELD, "国"), new Term(FIELD, "國"));
     expectedB.add(inner2, BooleanClause.Occur.MUST);
     Query expected = expectedB.build();
    QueryParser qp = new QueryParser("field", new MockCJKSynonymAnalyzer());
    QueryParser qp = new QueryParser(FIELD, new MockCJKSynonymAnalyzer());
     qp.setDefaultOperator(Operator.AND);
     assertEquals(expected, qp.parse("中国国"));
     expected = new BoostQuery(expected, 2f);
@@ -457,9 +461,9 @@ public class TestQueryParser extends QueryParserTestBase {
   /** forms multiphrase query */
   public void testCJKSynonymsPhrase() throws Exception {
     MultiPhraseQuery.Builder expectedQBuilder = new MultiPhraseQuery.Builder();
    expectedQBuilder.add(new Term("field", "中"));
    expectedQBuilder.add(new Term[] { new Term("field", "国"), new Term("field", "國")});
    QueryParser qp = new QueryParser("field", new MockCJKSynonymAnalyzer());
    expectedQBuilder.add(new Term(FIELD, "中"));
    expectedQBuilder.add(new Term[] { new Term(FIELD, "国"), new Term(FIELD, "國")});
    QueryParser qp = new QueryParser(FIELD, new MockCJKSynonymAnalyzer());
     qp.setDefaultOperator(Operator.AND);
     assertEquals(expectedQBuilder.build(), qp.parse("\"中国\""));
     Query expected = new BoostQuery(expectedQBuilder.build(), 2f);
@@ -471,7 +475,7 @@ public class TestQueryParser extends QueryParserTestBase {
 
   /** LUCENE-6677: make sure wildcard query respects maxDeterminizedStates. */
   public void testWildcardMaxDeterminizedStates() throws Exception {
    QueryParser qp = new QueryParser("field", new MockAnalyzer(random()));
    QueryParser qp = new QueryParser(FIELD, new MockAnalyzer(random()));
     qp.setMaxDeterminizedStates(10);
     expectThrows(TooComplexToDeterminizeException.class, () -> {
       qp.parse("a*aaaaaaa");
@@ -703,4 +707,163 @@ public class TestQueryParser extends QueryParserTestBase {
     assertQueryEquals("guinea pig", new MockSynonymAnalyzer(), "Synonym(cavy guinea) pig");
     splitOnWhitespace = oldSplitOnWhitespace;
   }
}
\ No newline at end of file
   
  public void testWildcardAlone() throws ParseException {
    //seems like crazy edge case, but can be useful in concordance 
    QueryParser parser = new QueryParser(FIELD, new ASCIIAnalyzer());
    parser.setAllowLeadingWildcard(false);
    expectThrows(ParseException.class, () -> {
      parser.parse("*");
    });

    QueryParser parser2 = new QueryParser("*", new ASCIIAnalyzer());
    parser2.setAllowLeadingWildcard(false);
    assertEquals(new MatchAllDocsQuery(), parser2.parse("*"));
  }

  public void testWildCardEscapes() throws ParseException, IOException {
    Analyzer a = new ASCIIAnalyzer();
    QueryParser parser = new QueryParser(FIELD, a);
    assertTrue(isAHit(parser.parse("mö*tley"), "moatley", a));
    // need to have at least one genuine wildcard to trigger the wildcard analysis
    // hence the * before the y
    assertTrue(isAHit(parser.parse("mö\\*tl*y"), "mo*tley", a));
    // escaped backslash then true wildcard
    assertTrue(isAHit(parser.parse("mö\\\\*tley"), "mo\\atley", a));
    // escaped wildcard then true wildcard
    assertTrue(isAHit(parser.parse("mö\\??ley"), "mo?tley", a));

    // the first is an escaped * which should yield a miss
    assertFalse(isAHit(parser.parse("mö\\*tl*y"), "moatley", a));
  }

  public void testWildcardDoesNotNormalizeEscapedChars() throws Exception {
    Analyzer asciiAnalyzer = new ASCIIAnalyzer();
    Analyzer keywordAnalyzer = new MockAnalyzer(random());
    QueryParser parser = new QueryParser(FIELD, asciiAnalyzer);

    assertTrue(isAHit(parser.parse("e*e"), "étude", asciiAnalyzer));
    assertTrue(isAHit(parser.parse("é*e"), "etude", asciiAnalyzer));
    assertFalse(isAHit(parser.parse("\\é*e"), "etude", asciiAnalyzer));
    assertTrue(isAHit(parser.parse("\\é*e"), "étude", keywordAnalyzer));
  }

  public void testWildCardQuery() throws ParseException {
    Analyzer a = new ASCIIAnalyzer();
    QueryParser parser = new QueryParser(FIELD, a);
    parser.setAllowLeadingWildcard(true);
    assertEquals("*bersetzung uber*ung", parser.parse("*bersetzung über*ung").toString(FIELD));
    parser.setAllowLeadingWildcard(false);
    assertEquals("motley crue motl?* cru?", parser.parse("Mötley Cr\u00fce Mötl?* Crü?").toString(FIELD));
    assertEquals("renee zellweger ren?? zellw?ger", parser.parse("Renée Zellweger Ren?? Zellw?ger").toString(FIELD));
  }


  public void testPrefixQuery() throws ParseException {
    Analyzer a = new ASCIIAnalyzer();
    QueryParser parser = new QueryParser(FIELD, a);
    assertEquals("ubersetzung ubersetz*", parser.parse("übersetzung übersetz*").toString(FIELD));
    assertEquals("motley crue motl* cru*", parser.parse("Mötley Crüe Mötl* crü*").toString(FIELD));
    assertEquals("rene? zellw*", parser.parse("René? Zellw*").toString(FIELD));
  }

  public void testRangeQuery() throws ParseException {
    Analyzer a = new ASCIIAnalyzer();
    QueryParser parser = new QueryParser(FIELD, a);
    assertEquals("[aa TO bb]", parser.parse("[aa TO bb]").toString(FIELD));
    assertEquals("{anais TO zoe}", parser.parse("{Anaïs TO Zoé}").toString(FIELD));
  }

  public void testFuzzyQuery() throws ParseException {
    Analyzer a = new ASCIIAnalyzer();
    QueryParser parser = new QueryParser(FIELD, a);
    assertEquals("ubersetzung ubersetzung~1", parser.parse("Übersetzung Übersetzung~0.9").toString(FIELD));
    assertEquals("motley crue motley~1 crue~2", parser.parse("Mötley Crüe Mötley~0.75 Crüe~0.5").toString(FIELD));
    assertEquals("renee zellweger renee~0 zellweger~2", parser.parse("Renée Zellweger Renée~0.9 Zellweger~").toString(FIELD));
  }

  final static class FoldingFilter extends TokenFilter {
    final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public FoldingFilter(TokenStream input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      if (input.incrementToken()) {
        char term[] = termAtt.buffer();
        for (int i = 0; i < term.length; i++)
          switch(term[i]) {
            case 'ü':
              term[i] = 'u'; 
              break;
            case 'ö': 
              term[i] = 'o'; 
              break;
            case 'é': 
              term[i] = 'e'; 
              break;
            case 'ï': 
              term[i] = 'i'; 
              break;
          }
        return true;
      } else {
        return false;
      }
    }
  }

  final static class ASCIIAnalyzer extends Analyzer {
    @Override
    public TokenStreamComponents createComponents(String fieldName) {
      Tokenizer result = new MockTokenizer(MockTokenizer.WHITESPACE, true);
      return new TokenStreamComponents(result, new FoldingFilter(result));
    }
    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
      return new FoldingFilter(new MockLowerCaseFilter(in));
    }
  }

  // LUCENE-4176
  public void testByteTerms() throws Exception {
    String s = "เข";
    Analyzer analyzer = new MockBytesAnalyzer();
    QueryParser qp = new QueryParser(FIELD, analyzer);

    assertTrue(isAHit(qp.parse("[เข TO เข]"), s, analyzer));
    assertTrue(isAHit(qp.parse("เข~1"), s, analyzer));
    assertTrue(isAHit(qp.parse("เข*"), s, analyzer));
    assertTrue(isAHit(qp.parse("เ*"), s, analyzer));
    assertTrue(isAHit(qp.parse("เ??"), s, analyzer));
  }
   
  
  private boolean isAHit(Query q, String content, Analyzer analyzer) throws IOException{
    Directory ramDir = newDirectory();
    RandomIndexWriter writer = new RandomIndexWriter(random(), ramDir, analyzer);
    Document doc = new Document();
    FieldType fieldType = new FieldType();
    fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    fieldType.setTokenized(true);
    fieldType.setStored(true);
    Field field = new Field(FIELD, content, fieldType);
    doc.add(field);
    writer.addDocument(doc);
    writer.close();
    DirectoryReader ir = DirectoryReader.open(ramDir);
    IndexSearcher is = new IndexSearcher(ir);
      
    int hits = is.search(q, 10).totalHits;
    ir.close();
    ramDir.close();
    if (hits == 1){
      return true;
    } else {
      return false;
    }

  }
}
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java
index 88e8b9b3cf2..d2deaa6a4aa 100644
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java
++ b/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java
@@ -181,20 +181,7 @@ public class TestPrecedenceQueryParser extends LuceneTestCase {
     }
   }
 
  public void assertWildcardQueryEquals(String query, boolean lowercase,
      String result) throws Exception {
    PrecedenceQueryParser qp = getParser(null);
    qp.setLowercaseExpandedTerms(lowercase);
    Query q = qp.parse(query, "field");
    String s = q.toString("field");
    if (!s.equals(result)) {
      fail("WildcardQuery /" + query + "/ yielded /" + s + "/, expecting /"
          + result + "/");
    }
  }

  public void assertWildcardQueryEquals(String query, String result)
      throws Exception {
  public void assertWildcardQueryEquals(String query, String result) throws Exception {
     PrecedenceQueryParser qp = getParser(null);
     Query q = qp.parse(query, "field");
     String s = q.toString("field");
@@ -339,36 +326,23 @@ public class TestPrecedenceQueryParser extends LuceneTestCase {
      */
     // First prefix queries:
     // by default, convert to lowercase:
    assertWildcardQueryEquals("Term*", true, "term*");
    assertWildcardQueryEquals("Term*", "term*");
     // explicitly set lowercase:
    assertWildcardQueryEquals("term*", true, "term*");
    assertWildcardQueryEquals("Term*", true, "term*");
    assertWildcardQueryEquals("TERM*", true, "term*");
    // explicitly disable lowercase conversion:
    assertWildcardQueryEquals("term*", false, "term*");
    assertWildcardQueryEquals("Term*", false, "Term*");
    assertWildcardQueryEquals("TERM*", false, "TERM*");
    assertWildcardQueryEquals("term*", "term*");
    assertWildcardQueryEquals("Term*", "term*");
    assertWildcardQueryEquals("TERM*", "term*");
     // Then 'full' wildcard queries:
     // by default, convert to lowercase:
     assertWildcardQueryEquals("Te?m", "te?m");
     // explicitly set lowercase:
    assertWildcardQueryEquals("te?m", true, "te?m");
    assertWildcardQueryEquals("Te?m", true, "te?m");
    assertWildcardQueryEquals("TE?M", true, "te?m");
    assertWildcardQueryEquals("Te?m*gerM", true, "te?m*germ");
    // explicitly disable lowercase conversion:
    assertWildcardQueryEquals("te?m", false, "te?m");
    assertWildcardQueryEquals("Te?m", false, "Te?m");
    assertWildcardQueryEquals("TE?M", false, "TE?M");
    assertWildcardQueryEquals("Te?m*gerM", false, "Te?m*gerM");
    assertWildcardQueryEquals("te?m", "te?m");
    assertWildcardQueryEquals("Te?m", "te?m");
    assertWildcardQueryEquals("TE?M", "te?m");
    assertWildcardQueryEquals("Te?m*gerM", "te?m*germ");
     // Fuzzy queries:
     assertWildcardQueryEquals("Term~", "term~2");
    assertWildcardQueryEquals("Term~", true, "term~2");
    assertWildcardQueryEquals("Term~", false, "Term~2");
     // Range queries:
     assertWildcardQueryEquals("[A TO C]", "[a TO c]");
    assertWildcardQueryEquals("[A TO C]", true, "[a TO c]");
    assertWildcardQueryEquals("[A TO C]", false, "[A TO C]");
   }
 
   public void testQPA() throws Exception {
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java
index 91b799dc093..2d5ee43d452 100644
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java
++ b/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java
@@ -288,10 +288,9 @@ public class TestQPHelper extends LuceneTestCase {
     }
   }
 
  public void assertWildcardQueryEquals(String query, boolean lowercase,
  public void assertWildcardQueryEquals(String query,
       String result, boolean allowLeadingWildcard) throws Exception {
     StandardQueryParser qp = getParser(null);
    qp.setLowercaseExpandedTerms(lowercase);
     qp.setAllowLeadingWildcard(allowLeadingWildcard);
     Query q = qp.parse(query, "field");
     String s = q.toString("field");
@@ -301,20 +300,9 @@ public class TestQPHelper extends LuceneTestCase {
     }
   }
 
  public void assertWildcardQueryEquals(String query, boolean lowercase,
  public void assertWildcardQueryEquals(String query,
       String result) throws Exception {
    assertWildcardQueryEquals(query, lowercase, result, false);
  }

  public void assertWildcardQueryEquals(String query, String result)
      throws Exception {
    StandardQueryParser qp = getParser(null);
    Query q = qp.parse(query, "field");
    String s = q.toString("field");
    if (!s.equals(result)) {
      fail("WildcardQuery /" + query + "/ yielded /" + s + "/, expecting /"
          + result + "/");
    }
    assertWildcardQueryEquals(query, result, false);
   }
 
   public Query getQueryDOA(String query, Analyzer a) throws Exception {
@@ -597,32 +585,21 @@ public class TestQPHelper extends LuceneTestCase {
      */
     // First prefix queries:
     // by default, convert to lowercase:
    assertWildcardQueryEquals("Term*", true, "term*");
    assertWildcardQueryEquals("Term*", "term*");
     // explicitly set lowercase:
    assertWildcardQueryEquals("term*", true, "term*");
    assertWildcardQueryEquals("Term*", true, "term*");
    assertWildcardQueryEquals("TERM*", true, "term*");
    // explicitly disable lowercase conversion:
    assertWildcardQueryEquals("term*", false, "term*");
    assertWildcardQueryEquals("Term*", false, "Term*");
    assertWildcardQueryEquals("TERM*", false, "TERM*");
    assertWildcardQueryEquals("term*", "term*");
    assertWildcardQueryEquals("Term*", "term*");
    assertWildcardQueryEquals("TERM*", "term*");
     // Then 'full' wildcard queries:
     // by default, convert to lowercase:
     assertWildcardQueryEquals("Te?m", "te?m");
     // explicitly set lowercase:
    assertWildcardQueryEquals("te?m", true, "te?m");
    assertWildcardQueryEquals("Te?m", true, "te?m");
    assertWildcardQueryEquals("TE?M", true, "te?m");
    assertWildcardQueryEquals("Te?m*gerM", true, "te?m*germ");
    // explicitly disable lowercase conversion:
    assertWildcardQueryEquals("te?m", false, "te?m");
    assertWildcardQueryEquals("Te?m", false, "Te?m");
    assertWildcardQueryEquals("TE?M", false, "TE?M");
    assertWildcardQueryEquals("Te?m*gerM", false, "Te?m*gerM");
    assertWildcardQueryEquals("te?m", "te?m");
    assertWildcardQueryEquals("Te?m", "te?m");
    assertWildcardQueryEquals("TE?M", "te?m");
    assertWildcardQueryEquals("Te?m*gerM", "te?m*germ");
     // Fuzzy queries:
     assertWildcardQueryEquals("Term~", "term~2");
    assertWildcardQueryEquals("Term~", true, "term~2");
    assertWildcardQueryEquals("Term~", false, "Term~2");
     // Range queries:
 
     // TODO: implement this on QueryParser
@@ -630,20 +607,18 @@ public class TestQPHelper extends LuceneTestCase {
     // C]': Lexical error at line 1, column 1. Encountered: "[" (91), after
     // : ""
     assertWildcardQueryEquals("[A TO C]", "[a TO c]");
    assertWildcardQueryEquals("[A TO C]", true, "[a TO c]");
    assertWildcardQueryEquals("[A TO C]", false, "[A TO C]");
     // Test suffix queries: first disallow
     expectThrows(QueryNodeException.class, () -> {
      assertWildcardQueryEquals("*Term", true, "*term");
      assertWildcardQueryEquals("*Term", "*term");
     });
 
     expectThrows(QueryNodeException.class, () -> {
      assertWildcardQueryEquals("?Term", true, "?term");
      assertWildcardQueryEquals("?Term", "?term");
     });
 
     // Test suffix queries: then allow
    assertWildcardQueryEquals("*Term", true, "*term", true);
    assertWildcardQueryEquals("?Term", true, "?term", true);
    assertWildcardQueryEquals("*Term", "*term", true);
    assertWildcardQueryEquals("?Term", "?term", true);
   }
 
   public void testLeadingWildcardType() throws Exception {
@@ -1159,10 +1134,10 @@ public class TestQPHelper extends LuceneTestCase {
   
   public void testRegexps() throws Exception {
     StandardQueryParser qp = new StandardQueryParser();
    qp.setAnalyzer(new MockAnalyzer(random(), MockTokenizer.WHITESPACE, true));
     final String df = "field" ;
     RegexpQuery q = new RegexpQuery(new Term("field", "[a-z][123]"));
     assertEquals(q, qp.parse("/[a-z][123]/", df));
    qp.setLowercaseExpandedTerms(true);
     assertEquals(q, qp.parse("/[A-Z][123]/", df));
     assertEquals(new BoostQuery(q, 0.5f), qp.parse("/[A-Z][123]/^0.5", df));
     qp.setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_REWRITE);
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestStandardQP.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestStandardQP.java
index f678796559e..7e50eeb6631 100644
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestStandardQP.java
++ b/lucene/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestStandardQP.java
@@ -87,12 +87,6 @@ public class TestStandardQP extends QueryParserTestBase {
     qp.setDefaultOperator(Operator.AND);
   }
   
  @Override
  public void setAnalyzeRangeTerms(CommonQueryParserConfiguration cqpC,
      boolean value) {
    throw new UnsupportedOperationException();
  }
  
   @Override
   public void setAutoGeneratePhraseQueries(CommonQueryParserConfiguration cqpC,
       boolean value) {
@@ -149,15 +143,6 @@ public class TestStandardQP extends QueryParserTestBase {
     WildcardQuery q = new WildcardQuery(new Term("field", "foo?ba?r"));//TODO not correct!!
     assertEquals(q, getQuery("foo\\?ba?r", qp));
   }

  
  @Override
  public void testCollatedRange() throws Exception {
    expectThrows(UnsupportedOperationException.class, () -> {
      setAnalyzeRangeTerms(getParser(null), true);
      super.testCollatedRange();
    });
  }
   
   @Override
   public void testAutoGeneratePhraseQueriesOn() throws Exception {
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java
index d58f6605670..217019350b7 100644
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java
++ b/lucene/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java
@@ -145,8 +145,6 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
 
   public abstract void setDefaultOperatorAND(CommonQueryParserConfiguration cqpC);
 
  public abstract void setAnalyzeRangeTerms(CommonQueryParserConfiguration cqpC, boolean value);

   public abstract void setAutoGeneratePhraseQueries(CommonQueryParserConfiguration cqpC, boolean value);
 
   public abstract void setDateResolution(CommonQueryParserConfiguration cqpC, CharSequence field, DateTools.Resolution value);
@@ -203,10 +201,9 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
     }
   }
 
  public void assertWildcardQueryEquals(String query, boolean lowercase, String result, boolean allowLeadingWildcard)
  public void assertWildcardQueryEquals(String query, String result, boolean allowLeadingWildcard)
     throws Exception {
     CommonQueryParserConfiguration cqpC = getParserConfig(null);
    cqpC.setLowercaseExpandedTerms(lowercase);
     cqpC.setAllowLeadingWildcard(allowLeadingWildcard);
     Query q = getQuery(query, cqpC);
     String s = q.toString("field");
@@ -216,18 +213,9 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
     }
   }
 
  public void assertWildcardQueryEquals(String query, boolean lowercase, String result)
  public void assertWildcardQueryEquals(String query, String result)
     throws Exception {
    assertWildcardQueryEquals(query, lowercase, result, false);
  }

  public void assertWildcardQueryEquals(String query, String result) throws Exception {
    Query q = getQuery(query);
    String s = q.toString("field");
    if (!s.equals(result)) {
      fail("WildcardQuery /" + query + "/ yielded /" + s + "/, expecting /"
          + result + "/");
    }
    assertWildcardQueryEquals(query, result, false);
   }
 
   public Query getQueryDOA(String query, Analyzer a)
@@ -473,39 +461,26 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
    */
 // First prefix queries:
     // by default, convert to lowercase:
    assertWildcardQueryEquals("Term*", true, "term*");
    assertWildcardQueryEquals("Term*", "term*");
     // explicitly set lowercase:
    assertWildcardQueryEquals("term*", true, "term*");
    assertWildcardQueryEquals("Term*", true, "term*");
    assertWildcardQueryEquals("TERM*", true, "term*");
    // explicitly disable lowercase conversion:
    assertWildcardQueryEquals("term*", false, "term*");
    assertWildcardQueryEquals("Term*", false, "Term*");
    assertWildcardQueryEquals("TERM*", false, "TERM*");
    assertWildcardQueryEquals("term*", "term*");
    assertWildcardQueryEquals("Term*", "term*");
    assertWildcardQueryEquals("TERM*", "term*");
 // Then 'full' wildcard queries:
     // by default, convert to lowercase:
     assertWildcardQueryEquals("Te?m", "te?m");
     // explicitly set lowercase:
    assertWildcardQueryEquals("te?m", true, "te?m");
    assertWildcardQueryEquals("Te?m", true, "te?m");
    assertWildcardQueryEquals("TE?M", true, "te?m");
    assertWildcardQueryEquals("Te?m*gerM", true, "te?m*germ");
    // explicitly disable lowercase conversion:
    assertWildcardQueryEquals("te?m", false, "te?m");
    assertWildcardQueryEquals("Te?m", false, "Te?m");
    assertWildcardQueryEquals("TE?M", false, "TE?M");
    assertWildcardQueryEquals("Te?m*gerM", false, "Te?m*gerM");
    assertWildcardQueryEquals("te?m", "te?m");
    assertWildcardQueryEquals("Te?m", "te?m");
    assertWildcardQueryEquals("TE?M", "te?m");
    assertWildcardQueryEquals("Te?m*gerM", "te?m*germ");
 //  Fuzzy queries:
     assertWildcardQueryEquals("Term~", "term~2");
    assertWildcardQueryEquals("Term~", true, "term~2");
    assertWildcardQueryEquals("Term~", false, "Term~2");
 //  Range queries:
     assertWildcardQueryEquals("[A TO C]", "[a TO c]");
    assertWildcardQueryEquals("[A TO C]", true, "[a TO c]");
    assertWildcardQueryEquals("[A TO C]", false, "[A TO C]");
     // Test suffix queries: first disallow
     try {
      assertWildcardQueryEquals("*Term", true, "*term");
      assertWildcardQueryEquals("*Term", "*term", false);
     } catch(Exception pe) {
       // expected exception
       if(!isQueryParserException(pe)){
@@ -513,7 +488,7 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
       }
     }
     try {
      assertWildcardQueryEquals("?Term", true, "?term");
      assertWildcardQueryEquals("?Term", "?term");
       fail();
     } catch(Exception pe) {
       // expected exception
@@ -522,8 +497,8 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
       }
     }
     // Test suffix queries: then allow
    assertWildcardQueryEquals("*Term", true, "*term", true);
    assertWildcardQueryEquals("?Term", true, "?term", true);
    assertWildcardQueryEquals("*Term", "*term", true);
    assertWildcardQueryEquals("?Term", "?term", true);
   }
   
   public void testLeadingWildcardType() throws Exception {
@@ -982,10 +957,9 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
   }
   
   public void testRegexps() throws Exception {
    CommonQueryParserConfiguration qp = getParserConfig( new MockAnalyzer(random(), MockTokenizer.WHITESPACE, false));
    CommonQueryParserConfiguration qp = getParserConfig( new MockAnalyzer(random(), MockTokenizer.WHITESPACE, true));
     RegexpQuery q = new RegexpQuery(new Term("field", "[a-z][123]"));
     assertEquals(q, getQuery("/[a-z][123]/",qp));
    qp.setLowercaseExpandedTerms(true);
     assertEquals(q, getQuery("/[A-Z][123]/",qp));
     assertEquals(new BoostQuery(q, 0.5f), getQuery("/[A-Z][123]/^0.5",qp));
     qp.setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_REWRITE);
@@ -1169,11 +1143,14 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
       Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, true);
       return new TokenStreamComponents(tokenizer, new MockCollationFilter(tokenizer));
     }
    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
      return new MockCollationFilter(new LowerCaseFilter(in));
    }
   }
   
   public void testCollatedRange() throws Exception {
     CommonQueryParserConfiguration qp = getParserConfig(new MockCollationAnalyzer());
    setAnalyzeRangeTerms(qp, true);
     Query expected = TermRangeQuery.newStringRange(getDefaultField(), "collatedabc", "collateddef", true, true);
     Query actual = getQuery("[abc TO def]", qp);
     assertEquals(expected, actual);
diff --git a/lucene/test-framework/src/java/org/apache/lucene/analysis/BaseTokenStreamTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/analysis/BaseTokenStreamTestCase.java
index c57a8bca6d1..0bb623fe36d 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/analysis/BaseTokenStreamTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/analysis/BaseTokenStreamTestCase.java
@@ -883,7 +883,10 @@ public abstract class BaseTokenStreamTestCase extends LuceneTestCase {
       assertTokenStreamContents(ts, 
                                 tokens.toArray(new String[tokens.size()]));
     }
    

    a.normalize("dummy", text);
    // TODO: what can we do besides testing that the above method does not throw?

     if (field != null) {
       reader = new StringReader(text);
       random = new Random(seed);
diff --git a/lucene/test-framework/src/java/org/apache/lucene/analysis/MockAnalyzer.java b/lucene/test-framework/src/java/org/apache/lucene/analysis/MockAnalyzer.java
index e87bf45d3f5..bbeffe953fb 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/analysis/MockAnalyzer.java
++ b/lucene/test-framework/src/java/org/apache/lucene/analysis/MockAnalyzer.java
@@ -92,7 +92,16 @@ public final class MockAnalyzer extends Analyzer {
     MockTokenFilter filt = new MockTokenFilter(tokenizer, filter);
     return new TokenStreamComponents(tokenizer, maybePayload(filt, fieldName));
   }
  

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = in;
    if (lowerCase) {
      result = new MockLowerCaseFilter(result);
    }
    return result;
  }

   private synchronized TokenFilter maybePayload(TokenFilter stream, String fieldName) {
     Integer val = previousMappings.get(fieldName);
     if (val == null) {
diff --git a/lucene/test-framework/src/java/org/apache/lucene/analysis/MockBytesAnalyzer.java b/lucene/test-framework/src/java/org/apache/lucene/analysis/MockBytesAnalyzer.java
index 01f3d4d1092..b8cfc5be1d6 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/analysis/MockBytesAnalyzer.java
++ b/lucene/test-framework/src/java/org/apache/lucene/analysis/MockBytesAnalyzer.java
@@ -16,6 +16,8 @@
  */
 package org.apache.lucene.analysis;
 
import org.apache.lucene.util.AttributeFactory;

 /**
  * Analyzer for testing that encodes terms as UTF-16 bytes.
  */
@@ -26,4 +28,9 @@ public final class MockBytesAnalyzer extends Analyzer {
         MockTokenizer.KEYWORD, false, MockTokenizer.DEFAULT_MAX_TOKEN_LENGTH);
     return new TokenStreamComponents(t);
   }

  @Override
  protected AttributeFactory attributeFactory() {
    return MockUTF16TermAttributeImpl.UTF16_TERM_ATTRIBUTE_FACTORY;
  }
 }
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/analyzing/package-info.java b/lucene/test-framework/src/java/org/apache/lucene/analysis/MockLowerCaseFilter.java
similarity index 55%
rename from lucene/queryparser/src/java/org/apache/lucene/queryparser/analyzing/package-info.java
rename to lucene/test-framework/src/java/org/apache/lucene/analysis/MockLowerCaseFilter.java
index 77397b45cbf..b1aea3d9af7 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/analyzing/package-info.java
++ b/lucene/test-framework/src/java/org/apache/lucene/analysis/MockLowerCaseFilter.java
@@ -14,9 +14,27 @@
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
/** 
 * QueryParser that passes Fuzzy-, Prefix-, Range-, and WildcardQuerys through the given analyzer.
 */
package org.apache.lucene.queryparser.analyzing;
package org.apache.lucene.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/** A lowercasing {@link TokenFilter}. */
public final class MockLowerCaseFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
 
  /** Sole constructor. */
  public MockLowerCaseFilter(TokenStream in) {
    super(in);
  }
  
  @Override
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      CharacterUtils.toLowerCase(termAtt.buffer(), 0, termAtt.length());
      return true;
    } else
      return false;
  }
}
diff --git a/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java b/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java
index c9f263d44d7..a5afbeccd8e 100644
-- a/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java
++ b/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java
@@ -18,6 +18,7 @@ package org.apache.solr.analysis;
 
 import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
 import org.apache.lucene.analysis.util.TokenFilterFactory;
 import org.apache.lucene.analysis.util.TokenizerFactory;
 
@@ -83,9 +84,22 @@ public final class TokenizerChain extends SolrAnalyzer {
     return reader;
   }
 
  @Override
  protected Reader initReaderForNormalization(String fieldName, Reader reader) {
    if (charFilters != null && charFilters.length > 0) {
      for (CharFilterFactory charFilter : charFilters) {
        if (charFilter instanceof MultiTermAwareComponent) {
          charFilter = (CharFilterFactory) ((MultiTermAwareComponent) charFilter).getMultiTermComponent();
          reader = charFilter.create(reader);
        }
      }
    }
    return reader;
  }

   @Override
   protected TokenStreamComponents createComponents(String fieldName) {
    Tokenizer tk = tokenizer.create();
    Tokenizer tk = tokenizer.create(attributeFactory());
     TokenStream ts = tk;
     for (TokenFilterFactory filter : filters) {
       ts = filter.create(ts);
@@ -93,6 +107,18 @@ public final class TokenizerChain extends SolrAnalyzer {
     return new TokenStreamComponents(tk, ts);
   }
 
  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = in;
    for (TokenFilterFactory filter : filters) {
      if (filter instanceof MultiTermAwareComponent) {
        filter = (TokenFilterFactory) ((MultiTermAwareComponent) filter).getMultiTermComponent();
        result = filter.create(in);
      }
    }
    return result;
  }

   @Override
   public String toString() {
     StringBuilder sb = new StringBuilder("TokenizerChain(");
- 
2.19.1.windows.1

