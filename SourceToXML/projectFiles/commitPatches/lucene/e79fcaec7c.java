From e79fcaec7cd8e8708110b89bcdc388a42e2f5353 Mon Sep 17 00:00:00 2001
From: Simon Willnauer <simonw@apache.org>
Date: Wed, 12 Jan 2011 08:22:12 +0000
Subject: [PATCH] LUCENE-2831: Cut over Collector#setNextReader &
 FieldComparator#setNextReader to AtomicReaderContext

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1058019 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |  8 ++-
 .../org/apache/lucene/demo/SearchFiles.java   |  5 +-
 .../highlight/HighlighterPhraseTest.java      |  5 +-
 .../store/instantiated/TestRealTime.java      |  4 +-
 .../lucli/src/java/lucli/LuceneMethods.java   |  3 +-
 .../lucene/index/memory/MemoryIndex.java      |  3 +-
 .../lucene/index/TestFieldNormModifier.java   |  7 ++-
 .../lucene/misc/TestLengthNormModifier.java   |  5 +-
 .../surround/query/BooleanQueryTst.java       |  4 +-
 .../tier/DistanceFieldComparatorSource.java   | 20 +++----
 .../lucene/swing/models/ListSearcher.java     |  3 +-
 .../org/apache/lucene/wordnet/SynExpand.java  |  5 +-
 .../org/apache/lucene/wordnet/SynLookup.java  |  7 ++-
 .../apache/lucene/search/BooleanScorer.java   |  4 +-
 .../org/apache/lucene/search/Collector.java   | 22 +++----
 .../lucene/search/ConstantScoreQuery.java     |  4 +-
 .../apache/lucene/search/FieldComparator.java | 58 +++++++++----------
 .../apache/lucene/search/IndexSearcher.java   |  4 +-
 .../apache/lucene/search/MultiCollector.java  |  6 +-
 .../search/PositiveScoresOnlyCollector.java   |  6 +-
 .../lucene/search/TimeLimitingCollector.java  |  8 +--
 .../lucene/search/TopFieldCollector.java      | 14 ++---
 .../lucene/search/TopScoreDocCollector.java   |  6 +-
 .../lucene/search/TotalHitCountCollector.java |  4 +-
 .../org/apache/lucene/index/TestOmitTf.java   |  3 +-
 .../org/apache/lucene/search/CheckHits.java   |  9 +--
 .../lucene/search/JustCompileSearch.java      |  6 +-
 .../lucene/search/MultiCollectorTest.java     |  8 +--
 .../org/apache/lucene/search/QueryUtils.java  | 14 ++---
 .../lucene/search/TestConstantScoreQuery.java |  3 +-
 .../apache/lucene/search/TestDocBoost.java    |  5 +-
 .../search/TestElevationComparator.java       |  5 +-
 .../search/TestMultiTermConstantScore.java    |  5 +-
 .../TestScoreCachingWrappingScorer.java       |  4 +-
 .../apache/lucene/search/TestScorerPerf.java  |  5 +-
 .../org/apache/lucene/search/TestSetNorm.java |  5 +-
 .../apache/lucene/search/TestSimilarity.java  | 11 ++--
 .../org/apache/lucene/search/TestSort.java    |  4 +-
 .../lucene/search/TestSubScorerFreqs.java     |  5 +-
 .../apache/lucene/search/TestTermScorer.java  |  4 +-
 .../search/TestTimeLimitingCollector.java     |  5 +-
 .../lucene/search/TestTopDocsCollector.java   |  5 +-
 .../handler/component/QueryComponent.java     | 31 +++++-----
 .../component/QueryElevationComponent.java    |  5 +-
 .../handler/component/TermsComponent.java     |  5 +-
 .../apache/solr/schema/RandomSortField.java   |  7 ++-
 .../solr/search/DocSetHitCollector.java       | 12 ++--
 .../java/org/apache/solr/search/Grouping.java | 31 +++++-----
 .../MissingStringLastComparatorSource.java    |  9 +--
 .../apache/solr/search/SolrIndexSearcher.java | 39 ++++++-------
 .../solr/search/function/ValueSource.java     |  4 +-
 .../org/apache/solr/update/UpdateHandler.java |  4 +-
 .../org/apache/solr/search/TestDocSet.java    |  5 +-
 .../apache/solr/search/TestIndexSearcher.java | 47 ++++++++-------
 .../test/org/apache/solr/search/TestSort.java |  7 +--
 .../solr/update/DirectUpdateHandlerTest.java  | 11 +---
 56 files changed, 278 insertions(+), 260 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 7ca14de759b..888ed11b991 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -128,8 +128,8 @@ Changes in backwards compatibility policy
   ParallelMultiSearcher into IndexSearcher as an optional
   ExecutorServiced passed to its ctor.  (Mike McCandless)
 
* LUCENE-2837: Changed Weight#scorer, Weight#explain & Filter#getDocIdSet to
  operate on a ReaderContext instead of directly on IndexReader to enable
* LUCENE-2831: Changed Weight#scorer, Weight#explain & Filter#getDocIdSet to
  operate on a AtomicReaderContext instead of directly on IndexReader to enable
   searches to be aware of IndexSearcher's context. (Simon Willnauer)
   
 * LUCENE-2839: Scorer#score(Collector,int,int) is now public because it is
@@ -188,6 +188,10 @@ API Changes
 
 * LUCENE-2778: RAMDirectory now exposes newRAMFile() which allows to override
   and return a different RAMFile implementation. (Shai Erera)
  
* LUCENE-2831: Weight#scorer, Weight#explain, Filter#getDocIdSet,
  Collector#setNextReader & FieldComparator#setNextReader now expect an
  AtomicReaderContext instead of an IndexReader. (Simon Willnauer)
 
 New features
 
diff --git a/lucene/contrib/demo/src/java/org/apache/lucene/demo/SearchFiles.java b/lucene/contrib/demo/src/java/org/apache/lucene/demo/SearchFiles.java
index 422e23497d7..6a300459995 100644
-- a/lucene/contrib/demo/src/java/org/apache/lucene/demo/SearchFiles.java
++ b/lucene/contrib/demo/src/java/org/apache/lucene/demo/SearchFiles.java
@@ -28,6 +28,7 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.queryParser.QueryParser;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.IndexSearcher;
@@ -160,9 +161,9 @@ public class SearchFiles {
       }
 
       @Override
      public void setNextReader(IndexReader reader, int docBase)
      public void setNextReader(AtomicReaderContext context)
           throws IOException {
        this.docBase = docBase;
        this.docBase = context.docBase;
       }
 
       @Override
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterPhraseTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterPhraseTest.java
index 31752349dd2..1f60e6ea5c5 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterPhraseTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterPhraseTest.java
@@ -36,6 +36,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.PhraseQuery;
@@ -133,9 +134,9 @@ public class HighlighterPhraseTest extends LuceneTestCase {
           }
 
           @Override
          public void setNextReader(IndexReader indexreader, int i)
          public void setNextReader(AtomicReaderContext context)
               throws IOException {
            this.baseDoc = i;
            this.baseDoc = context.docBase;
           }
 
           @Override
diff --git a/lucene/contrib/instantiated/src/test/org/apache/lucene/store/instantiated/TestRealTime.java b/lucene/contrib/instantiated/src/test/org/apache/lucene/store/instantiated/TestRealTime.java
index 4e7c59f9e6a..383cd807caf 100644
-- a/lucene/contrib/instantiated/src/test/org/apache/lucene/store/instantiated/TestRealTime.java
++ b/lucene/contrib/instantiated/src/test/org/apache/lucene/store/instantiated/TestRealTime.java
@@ -20,8 +20,8 @@ import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.util.LuceneTestCase;
 
 /**
@@ -67,7 +67,7 @@ public class TestRealTime extends LuceneTestCase {
     @Override
     public void setScorer(Scorer scorer) {}
     @Override
    public void setNextReader(IndexReader reader, int docBase) {}
    public void setNextReader(AtomicReaderContext context) {}
     @Override
     public boolean acceptsDocsOutOfOrder() { return true; }
     @Override
diff --git a/lucene/contrib/lucli/src/java/lucli/LuceneMethods.java b/lucene/contrib/lucli/src/java/lucli/LuceneMethods.java
index 9aca8ee3f27..266297b1f3b 100644
-- a/lucene/contrib/lucli/src/java/lucli/LuceneMethods.java
++ b/lucene/contrib/lucli/src/java/lucli/LuceneMethods.java
@@ -41,6 +41,7 @@ import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.Fields;
@@ -232,7 +233,7 @@ class LuceneMethods {
     }
 
     @Override
    public void setNextReader(IndexReader reader, int docBase) {}
    public void setNextReader(AtomicReaderContext context) {}
     @Override
     public boolean acceptsDocsOutOfOrder() {
       return true;
diff --git a/lucene/contrib/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java b/lucene/contrib/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
index f342c4f640f..07af85cb557 100644
-- a/lucene/contrib/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
++ b/lucene/contrib/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
@@ -38,6 +38,7 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.FieldSelector;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.index.FieldsEnum;
@@ -443,7 +444,7 @@ public class MemoryIndex implements Serializable {
         }
 
         @Override
        public void setNextReader(IndexReader reader, int docBase) { }
        public void setNextReader(AtomicReaderContext context) { }
       });
       float score = scores[0];
       return score;
diff --git a/lucene/contrib/misc/src/test/org/apache/lucene/index/TestFieldNormModifier.java b/lucene/contrib/misc/src/test/org/apache/lucene/index/TestFieldNormModifier.java
index 8066ea4e399..bdc386cefcd 100644
-- a/lucene/contrib/misc/src/test/org/apache/lucene/index/TestFieldNormModifier.java
++ b/lucene/contrib/misc/src/test/org/apache/lucene/index/TestFieldNormModifier.java
@@ -23,6 +23,7 @@ import java.util.Arrays;
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.DefaultSimilarity;
 import org.apache.lucene.search.IndexSearcher;
@@ -122,7 +123,7 @@ public class TestFieldNormModifier extends LuceneTestCase {
         scores[doc + docBase] = scorer.score();
       }
       @Override
      public void setNextReader(IndexReader reader, int docBase) {
      public void setNextReader(AtomicReaderContext context) {
         this.docBase = docBase;
       }
       @Override
@@ -157,7 +158,7 @@ public class TestFieldNormModifier extends LuceneTestCase {
         scores[doc + docBase] = scorer.score();
       }
       @Override
      public void setNextReader(IndexReader reader, int docBase) {
      public void setNextReader(AtomicReaderContext context) {
         this.docBase = docBase;
       }
       @Override
@@ -209,7 +210,7 @@ public class TestFieldNormModifier extends LuceneTestCase {
         scores[doc + docBase] = scorer.score();
       }
       @Override
      public void setNextReader(IndexReader reader, int docBase) {
      public void setNextReader(AtomicReaderContext context) {
         this.docBase = docBase;
       }
       @Override
diff --git a/lucene/contrib/misc/src/test/org/apache/lucene/misc/TestLengthNormModifier.java b/lucene/contrib/misc/src/test/org/apache/lucene/misc/TestLengthNormModifier.java
index cb424b451d3..7646bd1d531 100644
-- a/lucene/contrib/misc/src/test/org/apache/lucene/misc/TestLengthNormModifier.java
++ b/lucene/contrib/misc/src/test/org/apache/lucene/misc/TestLengthNormModifier.java
@@ -25,6 +25,7 @@ import org.apache.lucene.document.Field;
 import org.apache.lucene.index.FieldInvertState;
 import org.apache.lucene.index.FieldNormModifier;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.MultiNorms;
 import org.apache.lucene.index.Term;
@@ -139,7 +140,7 @@ public class TestLengthNormModifier extends LuceneTestCase {
       scores[doc + docBase] = scorer.score();
     }
     @Override
    public void setNextReader(IndexReader reader, int docBase) {
    public void setNextReader(AtomicReaderContext context) {
       this.docBase = docBase;
     }
     @Override
@@ -181,7 +182,7 @@ public class TestLengthNormModifier extends LuceneTestCase {
         scores[doc + docBase] = scorer.score();
       }
       @Override
      public void setNextReader(IndexReader reader, int docBase) {
      public void setNextReader(AtomicReaderContext context) {
         this.docBase = docBase;
       }
       @Override
diff --git a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/surround/query/BooleanQueryTst.java b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/surround/query/BooleanQueryTst.java
index c89127cde9d..325db2637e1 100644
-- a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/surround/query/BooleanQueryTst.java
++ b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/surround/query/BooleanQueryTst.java
@@ -19,7 +19,7 @@ package org.apache.lucene.queryParser.surround.query;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.Scorer;
@@ -77,7 +77,7 @@ public class BooleanQueryTst {
     }
 
     @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
    public void setNextReader(AtomicReaderContext context) throws IOException {
       this.docBase = docBase;
     }
     
diff --git a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/DistanceFieldComparatorSource.java b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/DistanceFieldComparatorSource.java
index 262916a9c68..dec1f88b14c 100644
-- a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/DistanceFieldComparatorSource.java
++ b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/DistanceFieldComparatorSource.java
@@ -19,7 +19,7 @@ package org.apache.lucene.spatial.tier;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.FieldComparator;
 import org.apache.lucene.search.FieldComparatorSource;
@@ -108,16 +108,14 @@ public class DistanceFieldComparatorSource extends FieldComparatorSource {
 
 		}
 
		@Override
                public FieldComparator setNextReader(IndexReader reader, int docBase)
                  throws IOException {
			
			// each reader in a segmented base
			// has an offset based on the maxDocs of previous readers
			offset = docBase;

                        return this;
		}
    @Override
    public FieldComparator setNextReader(AtomicReaderContext context)
        throws IOException {
      // each reader in a segmented base
      // has an offset based on the maxDocs of previous readers
      offset = context.docBase;
      return this;
    }
 
 		@Override
 		public Comparable<Double> value(int slot) {
diff --git a/lucene/contrib/swing/src/java/org/apache/lucene/swing/models/ListSearcher.java b/lucene/contrib/swing/src/java/org/apache/lucene/swing/models/ListSearcher.java
index 611b063baef..e8d2b7765c0 100644
-- a/lucene/contrib/swing/src/java/org/apache/lucene/swing/models/ListSearcher.java
++ b/lucene/contrib/swing/src/java/org/apache/lucene/swing/models/ListSearcher.java
@@ -32,6 +32,7 @@ import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.queryParser.MultiFieldQueryParser;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.IndexSearcher;
@@ -192,7 +193,7 @@ public class ListSearcher extends AbstractListModel {
       }
 
       @Override
      public void setNextReader(IndexReader reader, int docBase) {}
      public void setNextReader(AtomicReaderContext context) {}
       @Override
       public boolean acceptsDocsOutOfOrder() {
         return true;
diff --git a/lucene/contrib/wordnet/src/java/org/apache/lucene/wordnet/SynExpand.java b/lucene/contrib/wordnet/src/java/org/apache/lucene/wordnet/SynExpand.java
index 908cfd66eff..646abf73dbd 100755
-- a/lucene/contrib/wordnet/src/java/org/apache/lucene/wordnet/SynExpand.java
++ b/lucene/contrib/wordnet/src/java/org/apache/lucene/wordnet/SynExpand.java
@@ -33,6 +33,7 @@ import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.Collector;
@@ -160,9 +161,9 @@ public final class SynExpand {
         }
 
         @Override
        public void setNextReader(IndexReader reader, int docBase)
        public void setNextReader(AtomicReaderContext context)
             throws IOException {
          this.reader = reader;
          this.reader = context.reader;
         }
 
         @Override
diff --git a/lucene/contrib/wordnet/src/java/org/apache/lucene/wordnet/SynLookup.java b/lucene/contrib/wordnet/src/java/org/apache/lucene/wordnet/SynLookup.java
index 066df71ba02..4cc4836cc5b 100644
-- a/lucene/contrib/wordnet/src/java/org/apache/lucene/wordnet/SynLookup.java
++ b/lucene/contrib/wordnet/src/java/org/apache/lucene/wordnet/SynLookup.java
@@ -32,6 +32,7 @@ import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.Collector;
@@ -59,7 +60,7 @@ public class SynLookup {
     }
 
     @Override
    public void setNextReader(IndexReader reader, int docBase) {}
    public void setNextReader(AtomicReaderContext context) {}
     @Override
     public boolean acceptsDocsOutOfOrder() {
       return true;
@@ -169,9 +170,9 @@ public class SynLookup {
         }
 
         @Override
        public void setNextReader(IndexReader reader, int docBase)
        public void setNextReader(AtomicReaderContext context)
             throws IOException {
          this.reader = reader;
          this.reader = context.reader;
         }
 
         @Override
diff --git a/lucene/src/java/org/apache/lucene/search/BooleanScorer.java b/lucene/src/java/org/apache/lucene/search/BooleanScorer.java
index 6374e89f98b..7b244b4eb3a 100644
-- a/lucene/src/java/org/apache/lucene/search/BooleanScorer.java
++ b/lucene/src/java/org/apache/lucene/search/BooleanScorer.java
@@ -20,7 +20,7 @@ package org.apache.lucene.search;
 import java.io.IOException;
 import java.util.List;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.BooleanClause.Occur;
 
 /* Description from Doug Cutting (excerpted from
@@ -92,7 +92,7 @@ final class BooleanScorer extends Scorer {
     }
     
     @Override
    public void setNextReader(IndexReader reader, int docBase) {
    public void setNextReader(AtomicReaderContext context) {
       // not needed by this implementation
     }
     
diff --git a/lucene/src/java/org/apache/lucene/search/Collector.java b/lucene/src/java/org/apache/lucene/search/Collector.java
index 57d7b061e2d..b64abce0f4b 100644
-- a/lucene/src/java/org/apache/lucene/search/Collector.java
++ b/lucene/src/java/org/apache/lucene/search/Collector.java
@@ -19,7 +19,8 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 
 /**
  * <p>Expert: Collectors are primarily meant to be used to
@@ -98,8 +99,8 @@ import org.apache.lucene.index.IndexReader;
  *     bits.set(doc + docBase);
  *   }
  * 
 *   public void setNextReader(IndexReader reader, int docBase) {
 *     this.docBase = docBase;
 *   public void setNextReader(AtomicReaderContext context) {
 *     this.docBase = context.docBase;
  *   }
  * });
  * </pre>
@@ -143,17 +144,16 @@ public abstract class Collector {
   public abstract void collect(int doc) throws IOException;
 
   /**
   * Called before collecting from each IndexReader. All doc ids in
   * {@link #collect(int)} will correspond to reader.
   * Called before collecting from each {@link AtomicReaderContext}. All doc ids in
   * {@link #collect(int)} will correspond to {@link ReaderContext#reader}.
    * 
   * Add docBase to the current IndexReaders internal document id to re-base ids
   * in {@link #collect(int)}.
   * Add {@link AtomicReaderContext#docBase} to the current  {@link ReaderContext#reader}'s
   * internal document id to re-base ids in {@link #collect(int)}.
    * 
   * @param reader
   *          next IndexReader
   * @param docBase
   * @param context
   *          next atomic reader context
    */
  public abstract void setNextReader(IndexReader reader, int docBase) throws IOException;
  public abstract void setNextReader(AtomicReaderContext context) throws IOException;
 
   /**
    * Return <code>true</code> if this collector does not
diff --git a/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java b/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java
index c8b8c9da180..dcbb0ecb204 100644
-- a/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java
++ b/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java
@@ -222,8 +222,8 @@ public class ConstantScoreQuery extends Query {
         }
         
         @Override
        public void setNextReader(IndexReader reader, int docBase) throws IOException {
          collector.setNextReader(reader, docBase);
        public void setNextReader(AtomicReaderContext context) throws IOException {
          collector.setNextReader(context);
         }
         
         @Override
diff --git a/lucene/src/java/org/apache/lucene/search/FieldComparator.java b/lucene/src/java/org/apache/lucene/search/FieldComparator.java
index de8c59d8408..54bc20ba95a 100644
-- a/lucene/src/java/org/apache/lucene/search/FieldComparator.java
++ b/lucene/src/java/org/apache/lucene/search/FieldComparator.java
@@ -21,7 +21,7 @@ import java.io.IOException;
 import java.text.Collator;
 import java.util.Locale;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldCache.DocTermsIndex;
 import org.apache.lucene.search.FieldCache.DocTerms;
 import org.apache.lucene.search.cache.ByteValuesCreator;
@@ -82,7 +82,7 @@ import org.apache.lucene.util.packed.PackedInts;
  *       priority queue.  The {@link FieldValueHitQueue}
  *       calls this method when a new hit is competitive.
  *
 *  <li> {@link #setNextReader} Invoked
 *  <li> {@link #setNextReader(AtomicReaderContext)} Invoked
  *       when the search is switching to the next segment.
  *       You may need to update internal state of the
  *       comparator, for example retrieving new values from
@@ -150,19 +150,18 @@ public abstract class FieldComparator {
   public abstract void copy(int slot, int doc) throws IOException;
 
   /**
   * Set a new Reader. All subsequent docIDs are relative to
   * Set a new {@link AtomicReaderContext}. All subsequent docIDs are relative to
    * the current reader (you must add docBase if you need to
    * map it to a top-level docID).
    * 
   * @param reader current reader
   * @param docBase docBase of this reader 
   * @param context current reader context
    * @return the comparator to use for this segment; most
    *   comparators can just return "this" to reuse the same
    *   comparator across segments
    * @throws IOException
    * @throws IOException
    */
  public abstract FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException;
  public abstract FieldComparator setNextReader(AtomicReaderContext context) throws IOException;
 
   /** Sets the Scorer to use in case a document's score is
    *  needed.
@@ -242,8 +241,8 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      setup(FieldCache.DEFAULT.getBytes(reader, creator.field, creator));
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      setup(FieldCache.DEFAULT.getBytes(context.reader, creator.field, creator));
       docValues = cached.values;
       return this;
     }
@@ -314,8 +313,8 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      setup(FieldCache.DEFAULT.getDoubles(reader, creator.field, creator));
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      setup(FieldCache.DEFAULT.getDoubles(context.reader, creator.field, creator));
       docValues = cached.values;
       return this;
     }
@@ -388,8 +387,8 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      setup(FieldCache.DEFAULT.getFloats(reader, creator.field, creator));
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      setup(FieldCache.DEFAULT.getFloats(context.reader, creator.field, creator));
       docValues = cached.values;
       return this;
     }
@@ -444,8 +443,8 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      setup( FieldCache.DEFAULT.getShorts(reader, creator.field, creator));
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      setup( FieldCache.DEFAULT.getShorts(context.reader, creator.field, creator));
       docValues = cached.values;
       return this;
     }
@@ -522,8 +521,8 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      setup(FieldCache.DEFAULT.getInts(reader, creator.field, creator));
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      setup(FieldCache.DEFAULT.getInts(context.reader, creator.field, creator));
       docValues = cached.values;
       return this;
     }
@@ -597,8 +596,8 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      setup(FieldCache.DEFAULT.getLongs(reader, creator.field, creator));
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      setup(FieldCache.DEFAULT.getLongs(context.reader, creator.field, creator));
       docValues = cached.values;
       return this;
     }
@@ -648,7 +647,7 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) {
    public FieldComparator setNextReader(AtomicReaderContext context) {
       return this;
     }
     
@@ -700,11 +699,11 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) {
    public FieldComparator setNextReader(AtomicReaderContext context) {
       // TODO: can we "map" our docIDs to the current
       // reader? saves having to then subtract on every
       // compare call
      this.docBase = docBase;
      this.docBase = context.docBase;
       return this;
     }
     
@@ -781,8 +780,8 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      currentDocTerms = FieldCache.DEFAULT.getTerms(reader, field);
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      currentDocTerms = FieldCache.DEFAULT.getTerms(context.reader, field);
       return this;
     }
     
@@ -876,8 +875,8 @@ public abstract class FieldComparator {
     abstract class PerSegmentComparator extends FieldComparator {
       
       @Override
      public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
        return TermOrdValComparator.this.setNextReader(reader, docBase);
      public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
        return TermOrdValComparator.this.setNextReader(context);
       }
 
       @Override
@@ -1142,8 +1141,9 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      termsIndex = FieldCache.DEFAULT.getTermsIndex(reader, field);
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      final int docBase = context.docBase;
      termsIndex = FieldCache.DEFAULT.getTermsIndex(context.reader, field);
       final PackedInts.Reader docToOrd = termsIndex.getDocToOrd();
       FieldComparator perSegComp;
       if (docToOrd instanceof Direct8) {
@@ -1257,8 +1257,8 @@ public abstract class FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      docTerms = FieldCache.DEFAULT.getTerms(reader, field);
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      docTerms = FieldCache.DEFAULT.getTerms(context.reader, field);
       return this;
     }
     
diff --git a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
index 0dd8dfb85e3..9bd72d426db 100644
-- a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
++ b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
@@ -486,7 +486,7 @@ public class IndexSearcher {
     // always use single thread:
     if (filter == null) {
       for (int i = 0; i < leafContexts.length; i++) { // search each subreader
        collector.setNextReader(leafContexts[i].reader, leafContexts[i].docBase);
        collector.setNextReader(leafContexts[i]);
         Scorer scorer = weight.scorer(leafContexts[i], !collector.acceptsDocsOutOfOrder(), true);
         if (scorer != null) {
           scorer.score(collector);
@@ -494,7 +494,7 @@ public class IndexSearcher {
       }
     } else {
       for (int i = 0; i < leafContexts.length; i++) { // search each subreader
        collector.setNextReader(leafContexts[i].reader, leafContexts[i].docBase);
        collector.setNextReader(leafContexts[i]);
         searchWithFilter(leafContexts[i], weight, filter, collector);
       }
     }
diff --git a/lucene/src/java/org/apache/lucene/search/MultiCollector.java b/lucene/src/java/org/apache/lucene/search/MultiCollector.java
index ee79f549b0b..08e08403d33 100644
-- a/lucene/src/java/org/apache/lucene/search/MultiCollector.java
++ b/lucene/src/java/org/apache/lucene/search/MultiCollector.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.Scorer;
 
@@ -108,9 +108,9 @@ public class MultiCollector extends Collector {
   }
 
   @Override
  public void setNextReader(IndexReader reader, int o) throws IOException {
  public void setNextReader(AtomicReaderContext context) throws IOException {
     for (Collector c : collectors) {
      c.setNextReader(reader, o);
      c.setNextReader(context);
     }
   }
 
diff --git a/lucene/src/java/org/apache/lucene/search/PositiveScoresOnlyCollector.java b/lucene/src/java/org/apache/lucene/search/PositiveScoresOnlyCollector.java
index 2dd47bcbfe1..1e7cca99fa8 100644
-- a/lucene/src/java/org/apache/lucene/search/PositiveScoresOnlyCollector.java
++ b/lucene/src/java/org/apache/lucene/search/PositiveScoresOnlyCollector.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 /**
  * A {@link Collector} implementation which wraps another
@@ -43,8 +43,8 @@ public class PositiveScoresOnlyCollector extends Collector {
   }
 
   @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    c.setNextReader(reader, docBase);
  public void setNextReader(AtomicReaderContext context) throws IOException {
    c.setNextReader(context);
   }
 
   @Override
diff --git a/lucene/src/java/org/apache/lucene/search/TimeLimitingCollector.java b/lucene/src/java/org/apache/lucene/search/TimeLimitingCollector.java
index 405f1a09901..63ad23d9d6c 100644
-- a/lucene/src/java/org/apache/lucene/search/TimeLimitingCollector.java
++ b/lucene/src/java/org/apache/lucene/search/TimeLimitingCollector.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.ThreadInterruptedException;
 
 /**
@@ -213,9 +213,9 @@ public class TimeLimitingCollector extends Collector {
   }
   
   @Override
  public void setNextReader(IndexReader reader, int base) throws IOException {
    collector.setNextReader(reader, base);
    this.docBase = base;
  public void setNextReader(AtomicReaderContext context) throws IOException {
    collector.setNextReader(context);
    this.docBase = context.docBase;
   }
   
   @Override
diff --git a/lucene/src/java/org/apache/lucene/search/TopFieldCollector.java b/lucene/src/java/org/apache/lucene/search/TopFieldCollector.java
index 7c78274bb84..05e178d3115 100644
-- a/lucene/src/java/org/apache/lucene/search/TopFieldCollector.java
++ b/lucene/src/java/org/apache/lucene/search/TopFieldCollector.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldValueHitQueue.Entry;
 import org.apache.lucene.util.PriorityQueue;
 
@@ -92,9 +92,9 @@ public abstract class TopFieldCollector extends TopDocsCollector<Entry> {
     }
     
     @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
      this.docBase = docBase;
      queue.setComparator(0, comparator.setNextReader(reader, docBase));
    public void setNextReader(AtomicReaderContext context) throws IOException {
      this.docBase = context.docBase;
      queue.setComparator(0, comparator.setNextReader(context));
       comparator = queue.firstComparator;
     }
     
@@ -447,10 +447,10 @@ public abstract class TopFieldCollector extends TopDocsCollector<Entry> {
     }
 
     @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
      this.docBase = docBase;
    public void setNextReader(AtomicReaderContext context) throws IOException {
      this.docBase = context.docBase;
       for (int i = 0; i < comparators.length; i++) {
        queue.setComparator(i, comparators[i].setNextReader(reader, docBase));
        queue.setComparator(i, comparators[i].setNextReader(context));
       }
     }
 
diff --git a/lucene/src/java/org/apache/lucene/search/TopScoreDocCollector.java b/lucene/src/java/org/apache/lucene/search/TopScoreDocCollector.java
index 08a6897065a..d8f317592c1 100644
-- a/lucene/src/java/org/apache/lucene/search/TopScoreDocCollector.java
++ b/lucene/src/java/org/apache/lucene/search/TopScoreDocCollector.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 /**
  * A {@link Collector} implementation that collects the top-scoring hits,
@@ -155,8 +155,8 @@ public abstract class TopScoreDocCollector extends TopDocsCollector<ScoreDoc> {
   }
   
   @Override
  public void setNextReader(IndexReader reader, int base) {
    docBase = base;
  public void setNextReader(AtomicReaderContext context) {
    docBase = context.docBase;
   }
   
   @Override
diff --git a/lucene/src/java/org/apache/lucene/search/TotalHitCountCollector.java b/lucene/src/java/org/apache/lucene/search/TotalHitCountCollector.java
index 444fa67f942..533d69c65d3 100644
-- a/lucene/src/java/org/apache/lucene/search/TotalHitCountCollector.java
++ b/lucene/src/java/org/apache/lucene/search/TotalHitCountCollector.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 /**
  * Just counts the total number of hits.
@@ -38,7 +38,7 @@ public class TotalHitCountCollector extends Collector {
     totalHits++;
   }
 
  public void setNextReader(IndexReader reader, int docBase) {
  public void setNextReader(AtomicReaderContext context) {
   }
 
   public boolean acceptsDocsOutOfOrder() {
diff --git a/lucene/src/test/org/apache/lucene/index/TestOmitTf.java b/lucene/src/test/org/apache/lucene/index/TestOmitTf.java
index 78b96dc5d32..21968df695b 100644
-- a/lucene/src/test/org/apache/lucene/index/TestOmitTf.java
++ b/lucene/src/test/org/apache/lucene/index/TestOmitTf.java
@@ -26,6 +26,7 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.*;
 import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.store.Directory;
@@ -414,7 +415,7 @@ public class TestOmitTf extends LuceneTestCase {
     public static int getSum() { return sum; }
     
     @Override
    public void setNextReader(IndexReader reader, int docBase) {
    public void setNextReader(AtomicReaderContext context) {
       this.docBase = docBase;
     }
     @Override
diff --git a/lucene/src/test/org/apache/lucene/search/CheckHits.java b/lucene/src/test/org/apache/lucene/search/CheckHits.java
index dedd91949b6..fbfa0de093e 100644
-- a/lucene/src/test/org/apache/lucene/search/CheckHits.java
++ b/lucene/src/test/org/apache/lucene/search/CheckHits.java
@@ -25,6 +25,7 @@ import java.util.Random;
 import junit.framework.Assert;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.store.Directory;
 
 public class CheckHits {
@@ -120,8 +121,8 @@ public class CheckHits {
       bag.add(Integer.valueOf(doc + base));
     }
     @Override
    public void setNextReader(IndexReader reader, int docBase) {
      base = docBase;
    public void setNextReader(AtomicReaderContext context) {
      base = context.docBase;
     }
     @Override
     public boolean acceptsDocsOutOfOrder() {
@@ -483,8 +484,8 @@ public class CheckHits {
       verifyExplanation(d,doc,scorer.score(),deep,exp);
     }
     @Override
    public void setNextReader(IndexReader reader, int docBase) {
      base = docBase;
    public void setNextReader(AtomicReaderContext context) {
      base = context.docBase;
     }
     @Override
     public boolean acceptsDocsOutOfOrder() {
diff --git a/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java b/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
index 2e43904b584..f932e39bbe2 100644
-- a/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
++ b/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
@@ -44,7 +44,7 @@ final class JustCompileSearch {
     }
 
     @Override
    public void setNextReader(IndexReader reader, int docBase)
    public void setNextReader(AtomicReaderContext context)
         throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
@@ -127,7 +127,7 @@ final class JustCompileSearch {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase)
    public FieldComparator setNextReader(AtomicReaderContext context)
         throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
@@ -300,7 +300,7 @@ final class JustCompileSearch {
     }
 
     @Override
    public void setNextReader(IndexReader reader, int docBase)
    public void setNextReader(AtomicReaderContext context)
         throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
diff --git a/lucene/src/test/org/apache/lucene/search/MultiCollectorTest.java b/lucene/src/test/org/apache/lucene/search/MultiCollectorTest.java
index ae988c04ad2..a87135214ea 100644
-- a/lucene/src/test/org/apache/lucene/search/MultiCollectorTest.java
++ b/lucene/src/test/org/apache/lucene/search/MultiCollectorTest.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.util.LuceneTestCase;
@@ -46,7 +46,7 @@ public class MultiCollectorTest extends LuceneTestCase {
     }
 
     @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
    public void setNextReader(AtomicReaderContext context) throws IOException {
       setNextReaderCalled = true;
     }
 
@@ -73,7 +73,7 @@ public class MultiCollectorTest extends LuceneTestCase {
     assertTrue(c instanceof MultiCollector);
     assertTrue(c.acceptsDocsOutOfOrder());
     c.collect(1);
    c.setNextReader(null, 0);
    c.setNextReader(null);
     c.setScorer(null);
   }
 
@@ -95,7 +95,7 @@ public class MultiCollectorTest extends LuceneTestCase {
     Collector c = MultiCollector.wrap(dcs);
     assertTrue(c.acceptsDocsOutOfOrder());
     c.collect(1);
    c.setNextReader(null, 0);
    c.setNextReader(null);
     c.setScorer(null);
 
     for (DummyCollector dc : dcs) {
diff --git a/lucene/src/test/org/apache/lucene/search/QueryUtils.java b/lucene/src/test/org/apache/lucene/search/QueryUtils.java
index 27dda23ce40..1b3002211b7 100644
-- a/lucene/src/test/org/apache/lucene/search/QueryUtils.java
++ b/lucene/src/test/org/apache/lucene/search/QueryUtils.java
@@ -220,7 +220,7 @@ public class QueryUtils {
    */
   public static void checkSkipTo(final Query q, final IndexSearcher s) throws IOException {
     //System.out.println("Checking "+q);
    final AtomicReaderContext[] context = ReaderUtil.leaves(s.getTopReaderContext());
    final AtomicReaderContext[] readerContextArray = ReaderUtil.leaves(s.getTopReaderContext());
     if (q.weight(s).scoresDocsOutOfOrder()) return;  // in this case order of skipTo() might differ from that of next().
 
     final int skip_op = 0;
@@ -265,7 +265,7 @@ public class QueryUtils {
             try {
               if (scorer == null) {
                 Weight w = q.weight(s);
                scorer = w.scorer(context[leafPtr], true, false);
                scorer = w.scorer(readerContextArray[leafPtr], true, false);
               }
               
               int op = order[(opidx[0]++) % order.length];
@@ -303,7 +303,7 @@ public class QueryUtils {
           }
 
           @Override
          public void setNextReader(IndexReader reader, int docBase) throws IOException {
          public void setNextReader(AtomicReaderContext context) throws IOException {
             // confirm that skipping beyond the last doc, on the
             // previous reader, hits NO_MORE_DOCS
             if (lastReader[0] != null) {
@@ -317,8 +317,8 @@ public class QueryUtils {
               }
               leafPtr++;
             }
            lastReader[0] = reader;
            assert context[leafPtr].reader == reader;
            lastReader[0] = context.reader;
            assert readerContextArray[leafPtr].reader == context.reader;
             this.scorer = null;
             lastDoc[0] = -1;
           }
@@ -385,7 +385,7 @@ public class QueryUtils {
       }
 
       @Override
      public void setNextReader(IndexReader reader, int docBase) throws IOException {
      public void setNextReader(AtomicReaderContext context) throws IOException {
         // confirm that skipping beyond the last doc, on the
         // previous reader, hits NO_MORE_DOCS
         if (lastReader[0] != null) {
@@ -400,7 +400,7 @@ public class QueryUtils {
           leafPtr++;
         }
 
        lastReader[0] = reader;
        lastReader[0] = context.reader;
         lastDoc[0] = -1;
       }
       @Override
diff --git a/lucene/src/test/org/apache/lucene/search/TestConstantScoreQuery.java b/lucene/src/test/org/apache/lucene/search/TestConstantScoreQuery.java
index 874fc7f3a14..5849d57354f 100644
-- a/lucene/src/test/org/apache/lucene/search/TestConstantScoreQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestConstantScoreQuery.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.store.Directory;
@@ -68,7 +69,7 @@ public class TestConstantScoreQuery extends LuceneTestCase {
       }
       
       @Override
      public void setNextReader(IndexReader reader, int docBase) {
      public void setNextReader(AtomicReaderContext context) {
       }
       
       @Override
diff --git a/lucene/src/test/org/apache/lucene/search/TestDocBoost.java b/lucene/src/test/org/apache/lucene/search/TestDocBoost.java
index 2555896ac1d..c222d632bbd 100644
-- a/lucene/src/test/org/apache/lucene/search/TestDocBoost.java
++ b/lucene/src/test/org/apache/lucene/search/TestDocBoost.java
@@ -22,6 +22,7 @@ import java.io.IOException;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.document.*;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.store.Directory;
@@ -77,8 +78,8 @@ public class TestDocBoost extends LuceneTestCase {
            scores[doc + base] = scorer.score();
          }
          @Override
         public void setNextReader(IndexReader reader, int docBase) {
           base = docBase;
         public void setNextReader(AtomicReaderContext context) {
           base = context.docBase;
          }
          @Override
          public boolean acceptsDocsOutOfOrder() {
diff --git a/lucene/src/test/org/apache/lucene/search/TestElevationComparator.java b/lucene/src/test/org/apache/lucene/search/TestElevationComparator.java
index a99d2d0122b..a9eab85cc97 100644
-- a/lucene/src/test/org/apache/lucene/search/TestElevationComparator.java
++ b/lucene/src/test/org/apache/lucene/search/TestElevationComparator.java
@@ -21,6 +21,7 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldValueHitQueue.Entry;
 import org.apache.lucene.store.*;
 import org.apache.lucene.util.LuceneTestCase;
@@ -177,8 +178,8 @@ class ElevationComparatorSource extends FieldComparatorSource {
      }
 
      @Override
     public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
       idIndex = FieldCache.DEFAULT.getTermsIndex(reader, fieldname);
     public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
       idIndex = FieldCache.DEFAULT.getTermsIndex(context.reader, fieldname);
        return this;
      }
 
diff --git a/lucene/src/test/org/apache/lucene/search/TestMultiTermConstantScore.java b/lucene/src/test/org/apache/lucene/search/TestMultiTermConstantScore.java
index 7fc8ea6a799..f62d29620ae 100644
-- a/lucene/src/test/org/apache/lucene/search/TestMultiTermConstantScore.java
++ b/lucene/src/test/org/apache/lucene/search/TestMultiTermConstantScore.java
@@ -22,6 +22,7 @@ import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.store.Directory;
@@ -197,8 +198,8 @@ public class TestMultiTermConstantScore extends BaseTestRangeFilter {
         assertEquals("score for doc " + (doc + base) + " was not correct", 1.0f, scorer.score());
       }
       @Override
      public void setNextReader(IndexReader reader, int docBase) {
        base = docBase;
      public void setNextReader(AtomicReaderContext context) {
        base = context.docBase;
       }
       @Override
       public boolean acceptsDocsOutOfOrder() {
diff --git a/lucene/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java b/lucene/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java
index 69ae819c73d..a6ba9f61079 100644
-- a/lucene/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java
++ b/lucene/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.LuceneTestCase;
 
 public class TestScoreCachingWrappingScorer extends LuceneTestCase {
@@ -76,7 +76,7 @@ public class TestScoreCachingWrappingScorer extends LuceneTestCase {
       ++idx;
     }
 
    @Override public void setNextReader(IndexReader reader, int docBase)
    @Override public void setNextReader(AtomicReaderContext context)
         throws IOException {
     }
 
diff --git a/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java b/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java
index 55c4042eca0..1fc436f8b3b 100755
-- a/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java
++ b/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java
@@ -6,7 +6,6 @@ import org.apache.lucene.util.LuceneTestCase;
 import java.util.BitSet;
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
@@ -110,8 +109,8 @@ public class TestScorerPerf extends LuceneTestCase {
     public int getSum() { return sum; }
 
     @Override
    public void setNextReader(IndexReader reader, int base) {
      docBase = base;
    public void setNextReader(AtomicReaderContext context) {
      docBase = context.docBase;
     }
     @Override
     public boolean acceptsDocsOutOfOrder() {
diff --git a/lucene/src/test/org/apache/lucene/search/TestSetNorm.java b/lucene/src/test/org/apache/lucene/search/TestSetNorm.java
index a117ee30c6b..b730fce8a91 100644
-- a/lucene/src/test/org/apache/lucene/search/TestSetNorm.java
++ b/lucene/src/test/org/apache/lucene/search/TestSetNorm.java
@@ -23,6 +23,7 @@ import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.*;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.store.Directory;
@@ -74,8 +75,8 @@ public class TestSetNorm extends LuceneTestCase {
            scores[doc + base] = scorer.score();
          }
          @Override
         public void setNextReader(IndexReader reader, int docBase) {
           base = docBase;
         public void setNextReader(AtomicReaderContext context) {
           base = context.docBase;
          }
          @Override
          public boolean acceptsDocsOutOfOrder() {
diff --git a/lucene/src/test/org/apache/lucene/search/TestSimilarity.java b/lucene/src/test/org/apache/lucene/search/TestSimilarity.java
index fd459103777..c425ef504f6 100644
-- a/lucene/src/test/org/apache/lucene/search/TestSimilarity.java
++ b/lucene/src/test/org/apache/lucene/search/TestSimilarity.java
@@ -23,6 +23,7 @@ import java.util.Collection;
 
 import org.apache.lucene.index.FieldInvertState;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.store.Directory;
@@ -94,7 +95,7 @@ public class TestSimilarity extends LuceneTestCase {
            assertEquals(1.0f, scorer.score());
          }
          @Override
        public void setNextReader(IndexReader reader, int docBase) {}
        public void setNextReader(AtomicReaderContext context) {}
          @Override
         public boolean acceptsDocsOutOfOrder() {
            return true;
@@ -118,8 +119,8 @@ public class TestSimilarity extends LuceneTestCase {
            assertEquals((float)doc+base+1, scorer.score());
          }
          @Override
        public void setNextReader(IndexReader reader, int docBase) {
           base = docBase;
        public void setNextReader(AtomicReaderContext context) {
           base = context.docBase;
          }
          @Override
         public boolean acceptsDocsOutOfOrder() {
@@ -144,7 +145,7 @@ public class TestSimilarity extends LuceneTestCase {
            assertEquals(1.0f, scorer.score());
          }
          @Override
         public void setNextReader(IndexReader reader, int docBase) {}
         public void setNextReader(AtomicReaderContext context) {}
          @Override
          public boolean acceptsDocsOutOfOrder() {
            return true;
@@ -165,7 +166,7 @@ public class TestSimilarity extends LuceneTestCase {
         assertEquals(2.0f, scorer.score());
       }
       @Override
      public void setNextReader(IndexReader reader, int docBase) {}
      public void setNextReader(AtomicReaderContext context) {}
       @Override
       public boolean acceptsDocsOutOfOrder() {
         return true;
diff --git a/lucene/src/test/org/apache/lucene/search/TestSort.java b/lucene/src/test/org/apache/lucene/search/TestSort.java
index 32b58632374..9a5db0d1053 100644
-- a/lucene/src/test/org/apache/lucene/search/TestSort.java
++ b/lucene/src/test/org/apache/lucene/search/TestSort.java
@@ -506,8 +506,8 @@ public class TestSort extends LuceneTestCase implements Serializable {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      docValues = FieldCache.DEFAULT.getInts(reader, "parser", new FieldCache.IntParser() {
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      docValues = FieldCache.DEFAULT.getInts(context.reader, "parser", new FieldCache.IntParser() {
           public final int parseInt(final BytesRef term) {
             return (term.bytes[term.offset]-'A') * 123456;
           }
diff --git a/lucene/src/test/org/apache/lucene/search/TestSubScorerFreqs.java b/lucene/src/test/org/apache/lucene/search/TestSubScorerFreqs.java
index 4c2b05cb8fa..c5a3369f563 100644
-- a/lucene/src/test/org/apache/lucene/search/TestSubScorerFreqs.java
++ b/lucene/src/test/org/apache/lucene/search/TestSubScorerFreqs.java
@@ -19,6 +19,7 @@ package org.apache.lucene.search;
 
 import org.apache.lucene.document.*;
 import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.*;
 import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.search.Scorer.ScorerVisitor;
@@ -126,10 +127,10 @@ public class TestSubScorerFreqs extends LuceneTestCase {
     }
 
     @Override
    public void setNextReader(IndexReader reader, int docBase)
    public void setNextReader(AtomicReaderContext context)
         throws IOException {
       this.docBase = docBase;
      other.setNextReader(reader, docBase);
      other.setNextReader(context);
     }
 
     @Override
diff --git a/lucene/src/test/org/apache/lucene/search/TestTermScorer.java b/lucene/src/test/org/apache/lucene/search/TestTermScorer.java
index 139df5077eb..05c34758f56 100644
-- a/lucene/src/test/org/apache/lucene/search/TestTermScorer.java
++ b/lucene/src/test/org/apache/lucene/search/TestTermScorer.java
@@ -98,8 +98,8 @@ public class TestTermScorer extends LuceneTestCase {
       }
       
       @Override
      public void setNextReader(IndexReader reader, int docBase) {
        base = docBase;
      public void setNextReader(AtomicReaderContext context) {
        base = context.docBase;
       }
       
       @Override
diff --git a/lucene/src/test/org/apache/lucene/search/TestTimeLimitingCollector.java b/lucene/src/test/org/apache/lucene/search/TestTimeLimitingCollector.java
index 53d2deaa6b2..d31fd21d05e 100644
-- a/lucene/src/test/org/apache/lucene/search/TestTimeLimitingCollector.java
++ b/lucene/src/test/org/apache/lucene/search/TestTimeLimitingCollector.java
@@ -24,6 +24,7 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.queryParser.QueryParser;
 import org.apache.lucene.search.TimeLimitingCollector.TimeExceededException;
@@ -339,8 +340,8 @@ public class TestTimeLimitingCollector extends LuceneTestCase {
     }
     
     @Override
    public void setNextReader(IndexReader reader, int base) {
      docBase = base;
    public void setNextReader(AtomicReaderContext context) {
      docBase = context.docBase;
     }
     
     @Override
diff --git a/lucene/src/test/org/apache/lucene/search/TestTopDocsCollector.java b/lucene/src/test/org/apache/lucene/search/TestTopDocsCollector.java
index 348dfc30166..21164680661 100644
-- a/lucene/src/test/org/apache/lucene/search/TestTopDocsCollector.java
++ b/lucene/src/test/org/apache/lucene/search/TestTopDocsCollector.java
@@ -21,6 +21,7 @@ import java.io.IOException;
 
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.LuceneTestCase;
@@ -60,9 +61,9 @@ public class TestTopDocsCollector extends LuceneTestCase {
     }
 
     @Override
    public void setNextReader(IndexReader reader, int docBase)
    public void setNextReader(AtomicReaderContext context)
         throws IOException {
      base = docBase;
      base = context.docBase;
     }
 
     @Override
diff --git a/solr/src/java/org/apache/solr/handler/component/QueryComponent.java b/solr/src/java/org/apache/solr/handler/component/QueryComponent.java
index b5e133ec58b..18bbf49deec 100644
-- a/solr/src/java/org/apache/solr/handler/component/QueryComponent.java
++ b/solr/src/java/org/apache/solr/handler/component/QueryComponent.java
@@ -18,10 +18,13 @@
 package org.apache.solr.handler.component;
 
 import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.queryParser.ParseException;
 import org.apache.lucene.search.*;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.solr.cloud.CloudDescriptor;
 import org.apache.solr.cloud.ZkController;
 import org.apache.solr.common.SolrDocument;
@@ -444,23 +447,21 @@ public class QueryComponent extends SearchComponent
       SortField[] sortFields = sort==null ? new SortField[]{SortField.FIELD_SCORE} : sort.getSort();
       NamedList sortVals = new NamedList(); // order is important for the sort fields
       Field field = new Field("dummy", "", Field.Store.YES, Field.Index.NO); // a dummy Field

      SolrIndexReader reader = searcher.getReader();
      SolrIndexReader[] readers = reader.getLeafReaders();
      SolrIndexReader subReader = reader;
      if (readers.length==1) {
      ReaderContext topReaderContext = searcher.getTopReaderContext();
      AtomicReaderContext[] leaves = ReaderUtil.leaves(topReaderContext);
      AtomicReaderContext currentLeaf = null;
      if (leaves.length==1) {
         // if there is a single segment, use that subReader and avoid looking up each time
        subReader = readers[0];
        readers=null;
        currentLeaf = leaves[0];
        leaves=null;
       }
      int[] offsets = reader.getLeafOffsets();
 
       for (SortField sortField: sortFields) {
         int type = sortField.getType();
         if (type==SortField.SCORE || type==SortField.DOC) continue;
 
         FieldComparator comparator = null;
        FieldComparator comparators[] = (readers==null) ? null : new FieldComparator[readers.length];
        FieldComparator comparators[] = (leaves==null) ? null : new FieldComparator[leaves.length];
 
         String fieldname = sortField.getField();
         FieldType ft = fieldname==null ? null : req.getSchema().getFieldTypeNoEx(fieldname);
@@ -469,26 +470,24 @@ public class QueryComponent extends SearchComponent
         ArrayList<Object> vals = new ArrayList<Object>(docList.size());
         DocIterator it = rb.getResults().docList.iterator();
 
        int offset = 0;
         int idx = 0;
 
         while(it.hasNext()) {
           int doc = it.nextDoc();
          if (readers != null) {
            idx = SolrIndexReader.readerIndex(doc, offsets);
            subReader = readers[idx];
            offset = offsets[idx];
          if (leaves != null) {
            idx = ReaderUtil.subIndex(doc, leaves);
            currentLeaf = leaves[idx];
             comparator = comparators[idx];
           }
 
           if (comparator == null) {
             comparator = sortField.getComparator(1,0);
            comparator = comparator.setNextReader(subReader, offset);
            comparator = comparator.setNextReader(currentLeaf);
             if (comparators != null)
               comparators[idx] = comparator;
           }
 
          doc -= offset;  // adjust for what segment this is in
          doc -= currentLeaf.docBase;  // adjust for what segment this is in
           comparator.copy(0, doc);
           Object val = comparator.value(0);
 
diff --git a/solr/src/java/org/apache/solr/handler/component/QueryElevationComponent.java b/solr/src/java/org/apache/solr/handler/component/QueryElevationComponent.java
index 00eeb73c7a0..65061e231b0 100644
-- a/solr/src/java/org/apache/solr/handler/component/QueryElevationComponent.java
++ b/solr/src/java/org/apache/solr/handler/component/QueryElevationComponent.java
@@ -43,6 +43,7 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.*;
 import org.apache.lucene.util.StringHelper;
@@ -503,8 +504,8 @@ class ElevationComparatorSource extends FieldComparatorSource {
         values[slot] = docVal(doc);
       }
 
      public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
        idIndex = FieldCache.DEFAULT.getTermsIndex(reader, fieldname);
      public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
        idIndex = FieldCache.DEFAULT.getTermsIndex(context.reader, fieldname);
         return this;
       }
 
diff --git a/solr/src/java/org/apache/solr/handler/component/TermsComponent.java b/solr/src/java/org/apache/solr/handler/component/TermsComponent.java
index ee2e51f1f51..9fc48b352bb 100644
-- a/solr/src/java/org/apache/solr/handler/component/TermsComponent.java
++ b/solr/src/java/org/apache/solr/handler/component/TermsComponent.java
@@ -27,7 +27,6 @@ import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.StrField;
 import org.apache.solr.request.SimpleFacets.CountPair;
import org.apache.solr.search.SolrIndexReader;
 import org.apache.solr.util.BoundedTreeSet;
 
 import org.apache.solr.client.solrj.response.TermsResponse;
@@ -103,8 +102,8 @@ public class TermsComponent extends SearchComponent {
     boolean raw = params.getBool(TermsParams.TERMS_RAW, false);
 
 
    SolrIndexReader sr = rb.req.getSearcher().getReader();
    Fields lfields = MultiFields.getFields(sr);
    final IndexReader indexReader = rb.req.getSearcher().getTopReaderContext().reader;
    Fields lfields = MultiFields.getFields(indexReader);
 
     for (String field : fields) {
       NamedList<Integer> fieldTerms = new NamedList<Integer>();
diff --git a/solr/src/java/org/apache/solr/schema/RandomSortField.java b/solr/src/java/org/apache/solr/schema/RandomSortField.java
index 1a2891f8ecc..848e8206f9d 100644
-- a/solr/src/java/org/apache/solr/schema/RandomSortField.java
++ b/solr/src/java/org/apache/solr/schema/RandomSortField.java
@@ -22,6 +22,7 @@ import java.util.Map;
 
 import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.*;
 import org.apache.solr.response.TextResponseWriter;
 import org.apache.solr.search.QParser;
@@ -89,7 +90,7 @@ public class RandomSortField extends FieldType {
     // we use the top-level reader.
     return fieldName.hashCode() + base + (int)top.getVersion();
   }

  
   @Override
   public SortField getSortField(SchemaField field, boolean reverse) {
     return new SortField(field.getName(), randomComparatorSource, reverse);
@@ -127,8 +128,8 @@ public class RandomSortField extends FieldType {
           values[slot] = hash(doc+seed);
         }
 
        public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
          seed = getSeed(fieldname, reader);
        public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
          seed = getSeed(fieldname, context.reader);
           return this;
         }
 
diff --git a/solr/src/java/org/apache/solr/search/DocSetHitCollector.java b/solr/src/java/org/apache/solr/search/DocSetHitCollector.java
index ad32295e169..c0067a74380 100644
-- a/solr/src/java/org/apache/solr/search/DocSetHitCollector.java
++ b/solr/src/java/org/apache/solr/search/DocSetHitCollector.java
@@ -20,7 +20,7 @@ package org.apache.solr.search;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.io.IOException;
 
@@ -80,8 +80,8 @@ class DocSetCollector extends Collector {
   public void setScorer(Scorer scorer) throws IOException {
   }
 
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    this.base = docBase;
  public void setNextReader(AtomicReaderContext context) throws IOException {
    this.base = context.docBase;
   }
 
   public boolean acceptsDocsOutOfOrder() {
@@ -135,8 +135,8 @@ class DocSetDelegateCollector extends DocSetCollector {
     collector.setScorer(scorer);
   }
 
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    collector.setNextReader(reader, docBase);
    this.base = docBase;
  public void setNextReader(AtomicReaderContext context) throws IOException {
    collector.setNextReader(context);
    this.base = context.docBase;
   }
 }
diff --git a/solr/src/java/org/apache/solr/search/Grouping.java b/solr/src/java/org/apache/solr/search/Grouping.java
index c46d25ea4b5..bc7e858c2ac 100755
-- a/solr/src/java/org/apache/solr/search/Grouping.java
++ b/solr/src/java/org/apache/solr/search/Grouping.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.*;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.common.util.NamedList;
@@ -457,14 +457,15 @@ class FilterCollector extends GroupCollector {
   @Override
   public void collect(int doc) throws IOException {
     matches++;
    if (filter.exists(doc + docBase))
    if (filter.exists(doc + docBase)) {
       collector.collect(doc);
    }
   }
 
   @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    this.docBase = docBase;
    collector.setNextReader(reader, docBase);
  public void setNextReader(AtomicReaderContext context) throws IOException {
    docBase = context.docBase;
    collector.setNextReader(context);
   }
 
   @Override
@@ -685,13 +686,13 @@ class TopGroupCollector extends GroupCollector {
   }
 
   @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    this.docBase = docBase;
    docValues = vs.getValues(context, reader);
  public void setNextReader(AtomicReaderContext readerContext) throws IOException {
    this.docBase = readerContext.docBase;
    docValues = vs.getValues(context, readerContext.reader);
     filler = docValues.getValueFiller();
     mval = filler.getValue();
     for (int i=0; i<comparators.length; i++)
      comparators[i] = comparators[i].setNextReader(reader, docBase);
      comparators[i] = comparators[i].setNextReader(readerContext);
   }
 
   @Override
@@ -759,13 +760,13 @@ class Phase2GroupCollector extends Collector {
   }
 
   @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    this.docBase = docBase;
    docValues = vs.getValues(context, reader);
  public void setNextReader(AtomicReaderContext readerContext) throws IOException {
    this.docBase = readerContext.docBase;
    docValues = vs.getValues(context, readerContext.reader);
     filler = docValues.getValueFiller();
     mval = filler.getValue();
     for (SearchGroupDocs group : groupMap.values())
      group.collector.setNextReader(reader, docBase);
      group.collector.setNextReader(readerContext);
   }
 
   @Override
@@ -812,8 +813,8 @@ class Phase2StringGroupCollector extends Phase2GroupCollector {
   }
 
   @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    super.setNextReader(reader, docBase);
  public void setNextReader(AtomicReaderContext context) throws IOException {
    super.setNextReader(context);
     index = ((StringIndexDocValues)docValues).getDocTermsIndex();
 
     ordSet.clear();
diff --git a/solr/src/java/org/apache/solr/search/MissingStringLastComparatorSource.java b/solr/src/java/org/apache/solr/search/MissingStringLastComparatorSource.java
index 815dfdbf444..b8da084eca2 100644
-- a/solr/src/java/org/apache/solr/search/MissingStringLastComparatorSource.java
++ b/solr/src/java/org/apache/solr/search/MissingStringLastComparatorSource.java
@@ -19,6 +19,7 @@ package org.apache.solr.search;
 
 import org.apache.lucene.search.*;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.packed.Direct16;
 import org.apache.lucene.util.packed.Direct32;
@@ -101,8 +102,8 @@ class TermOrdValComparator_SML extends FieldComparator {
   }
 
   @Override
  public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
    return TermOrdValComparator_SML.createComparator(reader, this);
  public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
    return TermOrdValComparator_SML.createComparator(context.reader, this);
   }
 
   // Base class for specialized (per bit width of the
@@ -142,8 +143,8 @@ class TermOrdValComparator_SML extends FieldComparator {
     }
 
     @Override
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      return TermOrdValComparator_SML.createComparator(reader, parent);
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      return TermOrdValComparator_SML.createComparator(context.reader, parent);
     }
 
     @Override
diff --git a/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java b/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
index d175a712835..9a59dc78cbc 100644
-- a/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
++ b/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
@@ -21,6 +21,7 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.FieldSelector;
 import org.apache.lucene.document.FieldSelectorResult;
 import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.*;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.FSDirectory;
@@ -826,19 +827,17 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
     if (filter==null) {
       if (query instanceof TermQuery) {
         Term t = ((TermQuery)query).getTerm();
        SolrIndexReader[] readers = reader.getLeafReaders();
        int[] offsets = reader.getLeafOffsets();
        final AtomicReaderContext[] leaves = leafContexts;
 
        for (int i=0; i<readers.length; i++) {
          SolrIndexReader sir = readers[i];
          int offset = offsets[i];
          collector.setNextReader(sir, offset);
          
          Fields fields = sir.fields();
        for (int i=0; i<leaves.length; i++) {
          final AtomicReaderContext leaf = leaves[i];
          final IndexReader reader = leaf.reader;
          collector.setNextReader(leaf);
          Fields fields = reader.fields();
           Terms terms = fields.terms(t.field());
           BytesRef termBytes = t.bytes();
           
          Bits skipDocs = sir.getDeletedDocs();
          Bits skipDocs = reader.getDeletedDocs();
           DocsEnum docsEnum = terms==null ? null : terms.docs(skipDocs, termBytes, null);
 
           if (docsEnum != null) {
@@ -1126,7 +1125,7 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
           public void collect(int doc) throws IOException {
             numHits[0]++;
           }
          public void setNextReader(IndexReader reader, int docBase) throws IOException {
          public void setNextReader(AtomicReaderContext context) throws IOException {
           }
           public boolean acceptsDocsOutOfOrder() {
             return true;
@@ -1143,7 +1142,7 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
             float score = scorer.score();
             if (score > topscore[0]) topscore[0]=score;            
           }
          public void setNextReader(IndexReader reader, int docBase) throws IOException {
          public void setNextReader(AtomicReaderContext context) throws IOException {
           }
           public boolean acceptsDocsOutOfOrder() {
             return true;
@@ -1249,7 +1248,7 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
              float score = scorer.score();
              if (score > topscore[0]) topscore[0]=score;
            }
           public void setNextReader(IndexReader reader, int docBase) throws IOException {
           public void setNextReader(AtomicReaderContext context) throws IOException {
            }
            public boolean acceptsDocsOutOfOrder() {
              return false;
@@ -1570,21 +1569,21 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
     int base=0;
     int end=0;
     int readerIndex = -1;
    SolrIndexReader r=null;

    
    AtomicReaderContext leaf = null;
 
    while(iter.hasNext()) {
    for (int i = 0; i < leafContexts.length; i++) {
       int doc = iter.nextDoc();
       while (doc>=end) {
        r = reader.getLeafReaders()[++readerIndex];
        base = reader.getLeafOffsets()[readerIndex];
        end = base + r.maxDoc();
        topCollector.setNextReader(r, base);
        leaf = leafContexts[i++];
        base = leaf.docBase;
        end = base + leaf.reader.maxDoc();
        topCollector.setNextReader(leaf);
         // we should never need to set the scorer given the settings for the collector
       }
       topCollector.collect(doc-base);
     }

    
     TopDocs topDocs = topCollector.topDocs(0, nDocs);
 
     int nDocsReturned = topDocs.scoreDocs.length;
diff --git a/solr/src/java/org/apache/solr/search/function/ValueSource.java b/solr/src/java/org/apache/solr/search/function/ValueSource.java
index 80f61f6018d..9c1ac9724c3 100644
-- a/solr/src/java/org/apache/solr/search/function/ValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/ValueSource.java
@@ -187,8 +187,8 @@ public abstract class ValueSource implements Serializable {
       values[slot] = docVals.doubleVal(doc);
     }
 
    public FieldComparator setNextReader(IndexReader reader, int docBase) throws IOException {
      docVals = getValues(Collections.emptyMap(), reader);
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      docVals = getValues(Collections.emptyMap(), context.reader);
       return this;
     }
 
diff --git a/solr/src/java/org/apache/solr/update/UpdateHandler.java b/solr/src/java/org/apache/solr/update/UpdateHandler.java
index 4280b77f37a..72377f2eaf1 100644
-- a/solr/src/java/org/apache/solr/update/UpdateHandler.java
++ b/solr/src/java/org/apache/solr/update/UpdateHandler.java
@@ -18,7 +18,7 @@
 package org.apache.solr.update;
 
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
@@ -167,7 +167,7 @@ public abstract class UpdateHandler implements SolrInfoMBean {
     }
 
     @Override
    public void setNextReader(IndexReader arg0, int docBase) throws IOException {
    public void setNextReader(AtomicReaderContext arg0) throws IOException {
       this.docBase = docBase;
     }
 
diff --git a/solr/src/test/org/apache/solr/search/TestDocSet.java b/solr/src/test/org/apache/solr/search/TestDocSet.java
index 25eda5c9259..1e8d1377ee8 100644
-- a/solr/src/test/org/apache/solr/search/TestDocSet.java
++ b/solr/src/test/org/apache/solr/search/TestDocSet.java
@@ -406,7 +406,7 @@ public class TestDocSet extends LuceneTestCase {
     }
   }
 
  public void doFilterTest(SolrIndexReader reader) throws IOException {
  public void doFilterTest(IndexReader reader) throws IOException {
     ReaderContext topLevelContext = reader.getTopReaderContext();
     OpenBitSet bs = getRandomSet(reader.maxDoc(), rand.nextInt(reader.maxDoc()+1));
     DocSet a = new BitDocSet(bs);
@@ -450,8 +450,7 @@ public class TestDocSet extends LuceneTestCase {
 
     for (int i=0; i<5000; i++) {
       IndexReader r = dummyMultiReader(maxSeg, maxDoc);
      SolrIndexReader sir = new SolrIndexReader(r, null, 0);
      doFilterTest(sir);
      doFilterTest(r);
     }
   }
 }
diff --git a/solr/src/test/org/apache/solr/search/TestIndexSearcher.java b/solr/src/test/org/apache/solr/search/TestIndexSearcher.java
index 4ecd72df623..0b5e114c257 100755
-- a/solr/src/test/org/apache/solr/search/TestIndexSearcher.java
++ b/solr/src/test/org/apache/solr/search/TestIndexSearcher.java
@@ -16,6 +16,9 @@
  */
 package org.apache.solr.search;
 
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.schema.SchemaField;
@@ -38,12 +41,12 @@ public class TestIndexSearcher extends SolrTestCaseJ4 {
     ValueSource vs = sf.getType().getValueSource(sf, null);
     Map context = ValueSource.newContext(sqr.getSearcher());
     vs.createWeight(context, sqr.getSearcher());
    SolrIndexReader sr = sqr.getSearcher().getReader();
    int idx = SolrIndexReader.readerIndex(doc, sr.getLeafOffsets());
    int base = sr.getLeafOffsets()[idx];
    SolrIndexReader sub = sr.getLeafReaders()[idx];
    DocValues vals = vs.getValues(context, sub);
    return vals.strVal(doc-base);
    ReaderContext topReaderContext = sqr.getSearcher().getTopReaderContext();
    AtomicReaderContext[] leaves = ReaderUtil.leaves(topReaderContext);
    int idx = ReaderUtil.subIndex(doc, leaves);
    AtomicReaderContext leaf = leaves[idx];
    DocValues vals = vs.getValues(context, leaf.reader);
    return vals.strVal(doc-leaf.docBase);
   }
 
   public void testReopen() throws Exception {
@@ -53,7 +56,7 @@ public class TestIndexSearcher extends SolrTestCaseJ4 {
     assertU(commit());
 
     SolrQueryRequest sr1 = req("q","foo");
    SolrIndexReader r1 = sr1.getSearcher().getReader();
    ReaderContext rCtx1 = sr1.getSearcher().getTopReaderContext();
 
     String sval1 = getStringVal(sr1, "v_s",0);
     assertEquals("string1", sval1);
@@ -63,33 +66,33 @@ public class TestIndexSearcher extends SolrTestCaseJ4 {
     assertU(commit());
 
     SolrQueryRequest sr2 = req("q","foo");
    SolrIndexReader r2 = sr2.getSearcher().getReader();
    ReaderContext rCtx2 = sr2.getSearcher().getTopReaderContext();
 
     // make sure the readers share the first segment
     // Didn't work w/ older versions of lucene2.9 going from segment -> multi
    assertEquals(r1.getLeafReaders()[0], r2.getLeafReaders()[0]);
    assertEquals(ReaderUtil.leaves(rCtx1)[0].reader, ReaderUtil.leaves(rCtx2)[0].reader);
 
     assertU(adoc("id","5", "v_f","3.14159"));
     assertU(adoc("id","6", "v_f","8983", "v_s","string6"));
     assertU(commit());
 
     SolrQueryRequest sr3 = req("q","foo");
    SolrIndexReader r3 = sr3.getSearcher().getReader();
    ReaderContext rCtx3 = sr3.getSearcher().getTopReaderContext();
     // make sure the readers share segments
     // assertEquals(r1.getLeafReaders()[0], r3.getLeafReaders()[0]);
    assertEquals(r2.getLeafReaders()[0], r3.getLeafReaders()[0]);
    assertEquals(r2.getLeafReaders()[1], r3.getLeafReaders()[1]);
    assertEquals(ReaderUtil.leaves(rCtx2)[0].reader, ReaderUtil.leaves(rCtx3)[0].reader);
    assertEquals(ReaderUtil.leaves(rCtx2)[1].reader, ReaderUtil.leaves(rCtx3)[1].reader);
 
     sr1.close();
     sr2.close();            
 
     // should currently be 1, but this could change depending on future index management
    int baseRefCount = r3.getRefCount();
    int baseRefCount = rCtx3.reader.getRefCount();
     assertEquals(1, baseRefCount);
 
     assertU(commit());
     SolrQueryRequest sr4 = req("q","foo");
    SolrIndexReader r4 = sr4.getSearcher().getReader();
    ReaderContext rCtx4 = sr4.getSearcher().getTopReaderContext();
 
     // force an index change so the registered searcher won't be the one we are testing (and
     // then we should be able to test the refCount going all the way to 0
@@ -97,23 +100,23 @@ public class TestIndexSearcher extends SolrTestCaseJ4 {
     assertU(commit()); 
 
     // test that reader didn't change (according to equals at least... which uses the wrapped reader)
    assertEquals(r3,r4);
    assertEquals(baseRefCount+1, r4.getRefCount());
    assertEquals(rCtx3.reader, rCtx4.reader);
    assertEquals(baseRefCount+1, rCtx4.reader.getRefCount());
     sr3.close();
    assertEquals(baseRefCount, r4.getRefCount());
    assertEquals(baseRefCount, rCtx4.reader.getRefCount());
     sr4.close();
    assertEquals(baseRefCount-1, r4.getRefCount());
    assertEquals(baseRefCount-1, rCtx4.reader.getRefCount());
 
 
     SolrQueryRequest sr5 = req("q","foo");
    SolrIndexReader r5 = sr5.getSearcher().getReader();
    ReaderContext rCtx5 = sr5.getSearcher().getTopReaderContext();
 
     assertU(delI("1"));
     assertU(commit());
     SolrQueryRequest sr6 = req("q","foo");
    SolrIndexReader r6 = sr6.getSearcher().getReader();
    assertEquals(1, r6.getLeafReaders()[0].numDocs()); // only a single doc left in the first segment
    assertTrue( !r5.getLeafReaders()[0].equals(r6.getLeafReaders()[0]) );  // readers now different
    ReaderContext rCtx6 = sr6.getSearcher().getTopReaderContext();
    assertEquals(1, ReaderUtil.leaves(rCtx6)[0].reader.numDocs()); // only a single doc left in the first segment
    assertTrue( !ReaderUtil.leaves(rCtx5)[0].reader.equals(ReaderUtil.leaves(rCtx6)[0].reader) );  // readers now different
 
     sr5.close();
     sr6.close();
diff --git a/solr/src/test/org/apache/solr/search/TestSort.java b/solr/src/test/org/apache/solr/search/TestSort.java
index c4b0c1450c7..2fe21743973 100755
-- a/solr/src/test/org/apache/solr/search/TestSort.java
++ b/solr/src/test/org/apache/solr/search/TestSort.java
@@ -20,7 +20,6 @@ package org.apache.solr.search;
 import org.apache.lucene.analysis.core.SimpleAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
@@ -160,9 +159,9 @@ public class TestSort extends AbstractSolrTestCase {
           }
 
           @Override
          public void setNextReader(IndexReader reader, int docBase) throws IOException {
            topCollector.setNextReader(reader,docBase);
            this.docBase = docBase;
          public void setNextReader(AtomicReaderContext context) throws IOException {
            topCollector.setNextReader(context);
            docBase = context.docBase;
           }
 
           @Override
diff --git a/solr/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java b/solr/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java
index dbe1c512ae0..242ea064b3d 100644
-- a/solr/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java
++ b/solr/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java
@@ -20,18 +20,13 @@ package org.apache.solr.update;
 import java.util.HashMap;
 import java.util.Map;
 
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.params.MapSolrParams;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.request.LocalSolrQueryRequest;
 import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.SolrIndexReader;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -243,7 +238,7 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     assertU(commit());
 
     SolrQueryRequest sr = req("q","foo");
    SolrIndexReader r = sr.getSearcher().getReader();
    IndexReader r = sr.getSearcher().getTopReaderContext().reader;
     assertTrue(r.maxDoc() > r.numDocs());   // should have deletions
     assertFalse(r.getTopReaderContext().isAtomic);  // more than 1 segment
     sr.close();
@@ -251,7 +246,7 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     assertU(commit("expungeDeletes","true"));
 
     sr = req("q","foo");
    r = sr.getSearcher().getReader();
    r = sr.getSearcher().getTopReaderContext().reader;
     assertEquals(r.maxDoc(), r.numDocs());  // no deletions
     assertEquals(4,r.maxDoc());             // no dups
     assertFalse(r.getTopReaderContext().isAtomic);  //still more than 1 segment
- 
2.19.1.windows.1

