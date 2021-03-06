From f19e8c58fbb2b24de6037eee84adc0cbcbc99200 Mon Sep 17 00:00:00 2001
From: Simon Willnauer <simonw@apache.org>
Date: Thu, 13 Jan 2011 07:13:23 +0000
Subject: [PATCH] LUCENE-2831:  Cut over ValueSource#docValues to
 AtomicReaderContext & removed SolrIndexReader

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1058431 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/lucene/search/IndexSearcher.java   |  28 +-
 .../org/apache/lucene/search/TermQuery.java   |  11 +-
 .../search/function/FieldCacheSource.java     |   5 +-
 .../search/function/MultiValueSource.java     |  64 +--
 .../search/function/OrdFieldSource.java       |   6 +-
 .../function/ReverseOrdFieldSource.java       |   6 +-
 .../lucene/search/function/ValueSource.java   |  21 +-
 .../search/function/ValueSourceQuery.java     |   9 +-
 .../org/apache/lucene/util/ReaderUtil.java    |  11 +
 .../function/JustCompileSearchSpans.java      |   3 +-
 .../search/function/TestFieldScoreQuery.java  |  25 +-
 .../lucene/search/function/TestOrdValues.java |  29 +-
 .../search/function/TestValueSource.java      |  15 +-
 .../java/org/apache/solr/core/SolrCore.java   |   2 +-
 .../solr/handler/MoreLikeThisHandler.java     |   2 +-
 .../solr/handler/ReplicationHandler.java      |   8 +-
 .../org/apache/solr/handler/SnapPuller.java   |   2 +-
 .../solr/handler/admin/CoreAdminHandler.java  |   2 +-
 .../handler/admin/LukeRequestHandler.java     |   4 +-
 .../handler/component/HighlightComponent.java |   2 +-
 .../component/QueryElevationComponent.java    |   4 +-
 .../component/SpellCheckComponent.java        |   4 +-
 .../handler/component/StatsComponent.java     |   4 +-
 .../component/TermVectorComponent.java        |   2 +-
 .../highlight/DefaultSolrHighlighter.java     |   6 +-
 .../org/apache/solr/request/SimpleFacets.java |   4 +-
 .../apache/solr/request/UnInvertedField.java  |   8 +-
 .../org/apache/solr/schema/DateField.java     |   6 +-
 .../org/apache/solr/schema/LatLonType.java    |  13 +-
 .../apache/solr/schema/RandomSortField.java   |  20 +-
 .../solr/schema/SortableDoubleField.java      |   6 +-
 .../solr/schema/SortableFloatField.java       |   6 +-
 .../apache/solr/schema/SortableIntField.java  |   6 +-
 .../apache/solr/schema/SortableLongField.java |   6 +-
 .../apache/solr/schema/StrFieldSource.java    |   6 +-
 .../java/org/apache/solr/search/Grouping.java |   4 +-
 .../org/apache/solr/search/SolrFilter.java    |   4 +-
 .../apache/solr/search/SolrIndexReader.java   | 505 ------------------
 .../apache/solr/search/SolrIndexSearcher.java |  21 +-
 .../apache/solr/search/ValueSourceParser.java |  14 +-
 .../solr/search/function/BoostedQuery.java    |  16 +-
 .../solr/search/function/ByteFieldSource.java |   6 +-
 .../search/function/ConstValueSource.java     |   4 +-
 .../search/function/DocFreqValueSource.java   |   4 +-
 .../function/DoubleConstValueSource.java      |   4 +-
 .../search/function/DoubleFieldSource.java    |   5 +-
 .../search/function/DualFloatFunction.java    |   8 +-
 .../solr/search/function/FileFloatSource.java |  17 +-
 .../search/function/FloatFieldSource.java     |   6 +-
 .../solr/search/function/FunctionQuery.java   |   7 +-
 .../solr/search/function/IDFValueSource.java  |   3 +-
 .../solr/search/function/IntFieldSource.java  |   5 +-
 .../function/JoinDocFreqValueSource.java      |  20 +-
 .../search/function/LinearFloatFunction.java  |   6 +-
 .../search/function/LiteralValueSource.java   |   4 +-
 .../solr/search/function/LongFieldSource.java |   5 +-
 .../search/function/MaxDocValueSource.java    |   4 +-
 .../search/function/MaxFloatFunction.java     |   6 +-
 .../search/function/MultiFloatFunction.java   |   6 +-
 .../solr/search/function/NormValueSource.java |   6 +-
 .../search/function/NumDocsValueSource.java   |  10 +-
 .../solr/search/function/OrdFieldSource.java  |  20 +-
 .../search/function/QueryValueSource.java     |  42 +-
 .../function/RangeMapFloatFunction.java       |   6 +-
 .../function/ReciprocalFloatFunction.java     |   6 +-
 .../function/ReverseOrdFieldSource.java       |  18 +-
 .../search/function/ScaleFloatFunction.java   |  15 +-
 .../search/function/ShortFieldSource.java     |   6 +-
 .../search/function/SimpleFloatFunction.java  |   6 +-
 .../search/function/StringIndexDocValues.java |   5 +-
 .../solr/search/function/TFValueSource.java   |   6 +-
 .../search/function/TermFreqValueSource.java  |   6 +-
 .../solr/search/function/ValueSource.java     |  41 +-
 .../function/ValueSourceRangeFilter.java      |   6 +-
 .../search/function/VectorValueSource.java    |  10 +-
 .../function/distance/GeohashFunction.java    |   8 +-
 .../distance/GeohashHaversineFunction.java    |   8 +-
 .../distance/HaversineConstFunction.java      |   8 +-
 .../function/distance/HaversineFunction.java  |   8 +-
 .../distance/StringDistanceFunction.java      |   8 +-
 .../distance/VectorDistanceFunction.java      |   8 +-
 .../solr/spelling/IndexBasedSpellChecker.java |   2 +-
 .../solr/spelling/suggest/Suggester.java      |   2 +-
 .../org/apache/solr/update/UpdateHandler.java |   2 +-
 .../test/org/apache/solr/core/TestConfig.java |   2 +-
 .../solr/core/TestQuerySenderListener.java    |   2 +-
 .../QueryElevationComponentTest.java          |   8 +-
 .../org/apache/solr/request/TestFaceting.java |   4 +-
 .../apache/solr/search/TestIndexSearcher.java |   2 +-
 .../spelling/DirectSolrSpellCheckerTest.java  |   2 +-
 .../spelling/FileBasedSpellCheckerTest.java   |   6 +-
 .../spelling/IndexBasedSpellCheckerTest.java  |   6 +-
 .../SignatureUpdateProcessorFactoryTest.java  |   2 +-
 .../servlet/cache/HttpCacheHeaderUtil.java    |   4 +-
 94 files changed, 394 insertions(+), 947 deletions(-)
 delete mode 100755 solr/src/java/org/apache/solr/search/SolrIndexReader.java

diff --git a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
index 9bd72d426db..9e587962ea6 100644
-- a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
++ b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
@@ -174,20 +174,34 @@ public class IndexSearcher {
       }
     }
   }
  
  /* Ctor for concurrent sub-searchers searching only on a specific leaf of the given top-reader context
   * - instead of searching over all leaves this searcher only searches a single leaf searcher slice. Hence, 
   * for scorer and filter this looks like an ordinary search in the hierarchy such that there is no difference
   * between single and multi-threaded */
  private IndexSearcher(ReaderContext topLevel, AtomicReaderContext leaf) {

  /**
   * Expert: Creates a searcher from a top-level {@link ReaderContext} with and
   * executes searches on the given leave slice exclusively instead of searching
   * over all leaves. This constructor should be used to run one or more leaves
   * within a single thread. Hence, for scorer and filter this looks like an
   * ordinary search in the hierarchy such that there is no difference between
   * single and multi-threaded.
   * 
   * @lucene.experimental
   * */
  public IndexSearcher(ReaderContext topLevel, AtomicReaderContext... leaves) {
    assert assertLeaves(topLevel, leaves);
     readerContext = topLevel;
     reader = topLevel.reader;
    leafContexts = new AtomicReaderContext[] {leaf};
    leafContexts = leaves;
     executor = null;
     subSearchers = null;
     closeReader = false;
   }
   
  private boolean assertLeaves(ReaderContext topLevel, AtomicReaderContext... leaves) {
    for (AtomicReaderContext leaf : leaves) {
      assert ReaderUtil.getTopLevelContext(leaf) == topLevel : "leaf context is not a leaf of the given top-level context";
    }
    return true;
  }
  
   /** Return the {@link IndexReader} this searches. */
   public IndexReader getIndexReader() {
     return reader;
diff --git a/lucene/src/java/org/apache/lucene/search/TermQuery.java b/lucene/src/java/org/apache/lucene/search/TermQuery.java
index aa41c1240a7..56d0dcd63d1 100644
-- a/lucene/src/java/org/apache/lucene/search/TermQuery.java
++ b/lucene/src/java/org/apache/lucene/search/TermQuery.java
@@ -30,6 +30,7 @@ import org.apache.lucene.index.Term;
 import org.apache.lucene.search.Explanation.IDFExplanation;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.PerReaderTermState;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.lucene.util.ToStringUtils;
 
 /** A Query that matches documents containing a term.
@@ -88,7 +89,7 @@ public class TermQuery extends Query {
     public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       final String field = term.field();
       final IndexReader reader = context.reader;
      assert assertTopReaderContext(termStates, context) : "The top-reader used to create Weight is not the same as the current reader's top-reader";
      assert termStates.topReaderContext == ReaderUtil.getTopLevelContext(context) : "The top-reader used to create Weight is not the same as the current reader's top-reader";
       final TermState state = termStates
           .get(context.ord);
       if (state == null) { // term is not present in that reader
@@ -106,14 +107,6 @@ public class TermQuery extends Query {
       return terms == null || terms.docFreq(bytes) == 0;
     }
     
    private boolean assertTopReaderContext(PerReaderTermState state, ReaderContext context) {
      while(context.parent != null) {
        context = context.parent;
      }
      return state.topReaderContext == context;
    }
    
   
     @Override
     public Explanation explain(AtomicReaderContext context, int doc)
       throws IOException {
diff --git a/lucene/src/java/org/apache/lucene/search/function/FieldCacheSource.java b/lucene/src/java/org/apache/lucene/search/function/FieldCacheSource.java
index b55ae5ff820..c079ebddb81 100644
-- a/lucene/src/java/org/apache/lucene/search/function/FieldCacheSource.java
++ b/lucene/src/java/org/apache/lucene/search/function/FieldCacheSource.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search.function;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldCache;
 
 /**
@@ -55,8 +56,8 @@ public abstract class FieldCacheSource extends ValueSource {
 
   /* (non-Javadoc) @see org.apache.lucene.search.function.ValueSource#getValues(org.apache.lucene.index.IndexReader) */
   @Override
  public final DocValues getValues(IndexReader reader) throws IOException {
    return getCachedFieldValues(FieldCache.DEFAULT, field, reader);
  public final DocValues getValues(AtomicReaderContext context) throws IOException {
    return getCachedFieldValues(FieldCache.DEFAULT, field, context.reader);
   }
 
   /* (non-Javadoc) @see org.apache.lucene.search.function.ValueSource#description() */
diff --git a/lucene/src/java/org/apache/lucene/search/function/MultiValueSource.java b/lucene/src/java/org/apache/lucene/search/function/MultiValueSource.java
index 534cd1230b9..7dbccb25a69 100644
-- a/lucene/src/java/org/apache/lucene/search/function/MultiValueSource.java
++ b/lucene/src/java/org/apache/lucene/search/function/MultiValueSource.java
@@ -20,6 +20,9 @@ package org.apache.lucene.search.function;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.Explanation;
 import org.apache.lucene.util.ReaderUtil;
 
@@ -44,16 +47,16 @@ public final class MultiValueSource extends ValueSource {
   }
 
   @Override
  public DocValues getValues(IndexReader reader) throws IOException {

    IndexReader[] subReaders = reader.getSequentialSubReaders();
    if (subReaders != null) {
      // This is a composite reader
      return new MultiDocValues(subReaders);
    } else {
  public DocValues getValues(AtomicReaderContext context) throws IOException {
       // Already an atomic reader -- just delegate
      return other.getValues(reader);
      return other.getValues(context);
  }
  
  public DocValues getValues(ReaderContext context) throws IOException {
    if (context.isAtomic) {
      return getValues((AtomicReaderContext) context);
     }
    return new MultiDocValues(ReaderUtil.leaves(context));
   }
 
   @Override
@@ -78,59 +81,56 @@ public final class MultiValueSource extends ValueSource {
   private final class MultiDocValues extends DocValues {
 
     final DocValues[] docValues;
    final int[] docStarts;

    MultiDocValues(IndexReader[] subReaders) throws IOException {
      docValues = new DocValues[subReaders.length];
      docStarts = new int[subReaders.length];
      int base = 0;
      for(int i=0;i<subReaders.length;i++) {
        docValues[i] = other.getValues(subReaders[i]);
        docStarts[i] = base;
        base += subReaders[i].maxDoc();
    final AtomicReaderContext[] leaves;

    MultiDocValues(AtomicReaderContext[] leaves) throws IOException {
      this.leaves = leaves;
      docValues = new DocValues[leaves.length];
      for(int i=0;i<leaves.length;i++) {
        docValues[i] = other.getValues(leaves[i]);
       }
     }
     
     @Override
     public float floatVal(int doc) {
      final int n = ReaderUtil.subIndex(doc, docStarts);
      return docValues[n].floatVal(doc-docStarts[n]);
      final int n = ReaderUtil.subIndex(doc, leaves);
      return docValues[n].floatVal(doc-leaves[n].docBase);
     }
 
     @Override
     public int intVal(int doc) {
      final int n = ReaderUtil.subIndex(doc, docStarts);
      return docValues[n].intVal(doc-docStarts[n]);
      final int n = ReaderUtil.subIndex(doc, leaves);
      return docValues[n].intVal(doc-leaves[n].docBase);
     }
 
     @Override
     public long longVal(int doc) {
      final int n = ReaderUtil.subIndex(doc, docStarts);
      return docValues[n].longVal(doc-docStarts[n]);
      final int n = ReaderUtil.subIndex(doc, leaves);
      return docValues[n].longVal(doc-leaves[n].docBase);
     }
 
     @Override
     public double doubleVal(int doc) {
      final int n = ReaderUtil.subIndex(doc, docStarts);
      return docValues[n].doubleVal(doc-docStarts[n]);
      final int n = ReaderUtil.subIndex(doc, leaves);
      return docValues[n].doubleVal(doc-leaves[n].docBase);
     }
 
     @Override
     public String strVal(int doc) {
      final int n = ReaderUtil.subIndex(doc, docStarts);
      return docValues[n].strVal(doc-docStarts[n]);
      final int n = ReaderUtil.subIndex(doc, leaves);
      return docValues[n].strVal(doc-leaves[n].docBase);
     }
 
     @Override
     public String toString(int doc) {
      final int n = ReaderUtil.subIndex(doc, docStarts);
      return docValues[n].toString(doc-docStarts[n]);
      final int n = ReaderUtil.subIndex(doc, leaves);
      return docValues[n].toString(doc-leaves[n].docBase);
     }
 
     @Override
     public Explanation explain(int doc) {
      final int n = ReaderUtil.subIndex(doc, docStarts);
      return docValues[n].explain(doc-docStarts[n]);
      final int n = ReaderUtil.subIndex(doc, leaves);
      return docValues[n].explain(doc-leaves[n].docBase);
     }
   }
 }
diff --git a/lucene/src/java/org/apache/lucene/search/function/OrdFieldSource.java b/lucene/src/java/org/apache/lucene/search/function/OrdFieldSource.java
index 218375d2d5b..e7817da359e 100644
-- a/lucene/src/java/org/apache/lucene/search/function/OrdFieldSource.java
++ b/lucene/src/java/org/apache/lucene/search/function/OrdFieldSource.java
@@ -17,7 +17,7 @@
 
 package org.apache.lucene.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.FieldCache.DocTermsIndex;
 
@@ -69,8 +69,8 @@ public class OrdFieldSource extends ValueSource {
 
   /*(non-Javadoc) @see org.apache.lucene.search.function.ValueSource#getValues(org.apache.lucene.index.IndexReader) */
   @Override
  public DocValues getValues(IndexReader reader) throws IOException {
    final DocTermsIndex termsIndex = FieldCache.DEFAULT.getTermsIndex(reader, field);
  public DocValues getValues(AtomicReaderContext context) throws IOException {
    final DocTermsIndex termsIndex = FieldCache.DEFAULT.getTermsIndex(context.reader, field);
     return new DocValues() {
       /*(non-Javadoc) @see org.apache.lucene.search.function.DocValues#floatVal(int) */
       @Override
diff --git a/lucene/src/java/org/apache/lucene/search/function/ReverseOrdFieldSource.java b/lucene/src/java/org/apache/lucene/search/function/ReverseOrdFieldSource.java
index 517c37a7675..30e339d5724 100644
-- a/lucene/src/java/org/apache/lucene/search/function/ReverseOrdFieldSource.java
++ b/lucene/src/java/org/apache/lucene/search/function/ReverseOrdFieldSource.java
@@ -17,7 +17,7 @@
 
 package org.apache.lucene.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldCache;
 
 import java.io.IOException;
@@ -69,8 +69,8 @@ public class ReverseOrdFieldSource extends ValueSource {
 
   /*(non-Javadoc) @see org.apache.lucene.search.function.ValueSource#getValues(org.apache.lucene.index.IndexReader) */
   @Override
  public DocValues getValues(IndexReader reader) throws IOException {
    final FieldCache.DocTermsIndex termsIndex = FieldCache.DEFAULT.getTermsIndex(reader, field);
  public DocValues getValues(AtomicReaderContext context) throws IOException {
    final FieldCache.DocTermsIndex termsIndex = FieldCache.DEFAULT.getTermsIndex(context.reader, field);
 
     final int end = termsIndex.numOrd();
 
diff --git a/lucene/src/java/org/apache/lucene/search/function/ValueSource.java b/lucene/src/java/org/apache/lucene/search/function/ValueSource.java
index 0f8c7aa1298..b2c9603694a 100755
-- a/lucene/src/java/org/apache/lucene/search/function/ValueSource.java
++ b/lucene/src/java/org/apache/lucene/search/function/ValueSource.java
@@ -17,7 +17,10 @@ package org.apache.lucene.search.function;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.CompositeReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.function.DocValues;
 
 import java.io.IOException;
@@ -39,11 +42,23 @@ public abstract class ValueSource implements Serializable {
 
   /**
    * Return the DocValues used by the function query.
   * @param reader the IndexReader used to read these values.
   * @param context the IndexReader used to read these values.
    * If any caching is involved, that caching would also be IndexReader based.  
    * @throws IOException for any error.
    */
  public abstract DocValues getValues(IndexReader reader) throws IOException;
  public abstract DocValues getValues(AtomicReaderContext context) throws IOException;
  
  /**
   * Return the DocValues used by the function query.
   * @deprecated (4.0) This method is temporary, to ease the migration to segment-based
   * searching. Please change your code to not pass {@link CompositeReaderContext} to these
   * APIs. Use {@link #getValues(AtomicReaderContext)} instead
   */
  @Deprecated
  public DocValues getValues(ReaderContext context) throws IOException {
    return getValues((AtomicReaderContext) context);
  }

 
   /** 
    * description of field, used in explain() 
diff --git a/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java b/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java
index 1a3f7706e67..38e4c95e625 100644
-- a/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java
++ b/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java
@@ -100,13 +100,13 @@ public class ValueSourceQuery extends Query {
 
     @Override
     public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new ValueSourceScorer(similarity, context.reader, this);
      return new ValueSourceScorer(similarity, context, this);
     }
 
     /*(non-Javadoc) @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader, int) */
     @Override
     public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
      DocValues vals = valSrc.getValues(context.reader);
      DocValues vals = valSrc.getValues(context);
       float sc = queryWeight * vals.floatVal(doc);
 
       Explanation result = new ComplexExplanation(
@@ -133,11 +133,12 @@ public class ValueSourceQuery extends Query {
     private int doc = -1;
 
     // constructor
    private ValueSourceScorer(Similarity similarity, IndexReader reader, ValueSourceWeight w) throws IOException {
    private ValueSourceScorer(Similarity similarity, AtomicReaderContext context, ValueSourceWeight w) throws IOException {
       super(similarity,w);
      final IndexReader reader = context.reader;
       qWeight = w.getValue();
       // this is when/where the values are first created.
      vals = valSrc.getValues(reader);
      vals = valSrc.getValues(context);
       delDocs = reader.getDeletedDocs();
       maxDoc = reader.maxDoc();
     }
diff --git a/lucene/src/java/org/apache/lucene/util/ReaderUtil.java b/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
index e1533433c1b..772b5ebf751 100644
-- a/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
++ b/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
@@ -228,6 +228,17 @@ public final class ReaderUtil {
     }
     return leaves;
   }
  
  /**
   * Walks up the reader tree and return the given context's top level reader
   * context, or in other words the reader tree's root context.
   */
  public static ReaderContext getTopLevelContext(ReaderContext context) {
    while (context.parent != null) {
      context = context.parent;
    }
    return context;
  }
 
   /**
    * Returns index of the searcher/reader for document <code>n</code> in the
diff --git a/lucene/src/test/org/apache/lucene/search/function/JustCompileSearchSpans.java b/lucene/src/test/org/apache/lucene/search/function/JustCompileSearchSpans.java
index a85f040352c..96f5032c629 100644
-- a/lucene/src/test/org/apache/lucene/search/function/JustCompileSearchSpans.java
++ b/lucene/src/test/org/apache/lucene/search/function/JustCompileSearchSpans.java
@@ -18,6 +18,7 @@ package org.apache.lucene.search.function;
  */
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldCache;
 
 import java.io.IOException;
@@ -82,7 +83,7 @@ final class JustCompileSearchFunction {
     }
 
     @Override
    public DocValues getValues(IndexReader reader) throws IOException {
    public DocValues getValues(AtomicReaderContext context) throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
 
diff --git a/lucene/src/test/org/apache/lucene/search/function/TestFieldScoreQuery.java b/lucene/src/test/org/apache/lucene/search/function/TestFieldScoreQuery.java
index 8ff2af949d9..a90be6ec36c 100755
-- a/lucene/src/test/org/apache/lucene/search/function/TestFieldScoreQuery.java
++ b/lucene/src/test/org/apache/lucene/search/function/TestFieldScoreQuery.java
@@ -19,12 +19,13 @@ package org.apache.lucene.search.function;
 
 import java.util.HashMap;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.QueryUtils;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.ReaderUtil;
 import org.junit.Test;
 
 /**
@@ -185,12 +186,12 @@ public class TestFieldScoreQuery extends FunctionTestSetup {
       FieldScoreQuery q = new FieldScoreQuery(field,tp);
       ScoreDoc[] h = s.search(q, null, 1000).scoreDocs;
       assertEquals("All docs should be matched!",N_DOCS,h.length);
      IndexReader[] readers = s.getIndexReader().getSequentialSubReaders();
      for (int j = 0; j < readers.length; j++) {
        IndexReader reader = readers[j];
      AtomicReaderContext[] leaves = ReaderUtil.leaves(s.getTopReaderContext());
      for (int j = 0; j < leaves.length; j++) {
        AtomicReaderContext leaf = leaves[j];
         try {
           if (i == 0) {
            innerArray[j] = q.valSrc.getValues(reader).getInnerArray();
            innerArray[j] = q.valSrc.getValues(leaf).getInnerArray();
             log(i + ".  compare: " + innerArray[j].getClass() + " to "
                 + expectedArrayTypes.get(tp).getClass());
             assertEquals(
@@ -198,9 +199,9 @@ public class TestFieldScoreQuery extends FunctionTestSetup {
                 innerArray[j].getClass(), expectedArrayTypes.get(tp).getClass());
           } else {
             log(i + ".  compare: " + innerArray[j] + " to "
                + q.valSrc.getValues(reader).getInnerArray());
                + q.valSrc.getValues(leaf).getInnerArray());
             assertSame("field values should be cached and reused!", innerArray[j],
                q.valSrc.getValues(reader).getInnerArray());
                q.valSrc.getValues(leaf).getInnerArray());
           }
         } catch (UnsupportedOperationException e) {
           if (!warned) {
@@ -217,15 +218,15 @@ public class TestFieldScoreQuery extends FunctionTestSetup {
     FieldScoreQuery q = new FieldScoreQuery(field,tp);
     ScoreDoc[] h = s.search(q, null, 1000).scoreDocs;
     assertEquals("All docs should be matched!",N_DOCS,h.length);
    IndexReader[] readers = s.getIndexReader().getSequentialSubReaders();
    for (int j = 0; j < readers.length; j++) {
      IndexReader reader = readers[j];
    AtomicReaderContext[] leaves = ReaderUtil.leaves(s.getTopReaderContext());
    for (int j = 0; j < leaves.length; j++) {
      AtomicReaderContext leaf = leaves[j];
       try {
         log("compare: " + innerArray + " to "
            + q.valSrc.getValues(reader).getInnerArray());
            + q.valSrc.getValues(leaf).getInnerArray());
         assertNotSame(
             "cached field values should not be reused if reader as changed!",
            innerArray, q.valSrc.getValues(reader).getInnerArray());
            innerArray, q.valSrc.getValues(leaf).getInnerArray());
       } catch (UnsupportedOperationException e) {
         if (!warned) {
           System.err.println("WARNING: " + testName()
diff --git a/lucene/src/test/org/apache/lucene/search/function/TestOrdValues.java b/lucene/src/test/org/apache/lucene/search/function/TestOrdValues.java
index 8fb7eda1484..706eca76f43 100644
-- a/lucene/src/test/org/apache/lucene/search/function/TestOrdValues.java
++ b/lucene/src/test/org/apache/lucene/search/function/TestOrdValues.java
@@ -18,8 +18,9 @@ package org.apache.lucene.search.function;
  */
 
 import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.*;
import org.apache.lucene.util.ReaderUtil;
 import org.junit.Test;
 
 /**
@@ -168,14 +169,14 @@ public class TestOrdValues extends FunctionTestSetup {
       ScoreDoc[] h = s.search(q, null, 1000).scoreDocs;
       try {
         assertEquals("All docs should be matched!", N_DOCS, h.length);
        IndexReader[] readers = s.getIndexReader().getSequentialSubReaders();
        AtomicReaderContext[] leaves = ReaderUtil.leaves(s.getTopReaderContext());
 
        for (IndexReader reader : readers) {
        for (AtomicReaderContext leaf : leaves) {
           if (i == 0) {
            innerArray = q.valSrc.getValues(reader).getInnerArray();
            innerArray = q.valSrc.getValues(leaf).getInnerArray();
           } else {
            log(i + ".  compare: " + innerArray + " to " + q.valSrc.getValues(reader).getInnerArray());
            assertSame("field values should be cached and reused!", innerArray, q.valSrc.getValues(reader).getInnerArray());
            log(i + ".  compare: " + innerArray + " to " + q.valSrc.getValues(leaf).getInnerArray());
            assertSame("field values should be cached and reused!", innerArray, q.valSrc.getValues(leaf).getInnerArray());
           }
         }
       } catch (UnsupportedOperationException e) {
@@ -201,15 +202,15 @@ public class TestOrdValues extends FunctionTestSetup {
     q = new ValueSourceQuery(vs);
     h = s.search(q, null, 1000).scoreDocs;
     assertEquals("All docs should be matched!", N_DOCS, h.length);
    IndexReader[] readers = s.getIndexReader().getSequentialSubReaders();
    AtomicReaderContext[] leaves = ReaderUtil.leaves(s.getTopReaderContext());
 
    for (IndexReader reader : readers) {
    for (AtomicReaderContext leaf : leaves) {
       try {
         log("compare (should differ): " + innerArray + " to "
                + q.valSrc.getValues(reader).getInnerArray());
                + q.valSrc.getValues(leaf).getInnerArray());
         assertNotSame(
                 "different values should be loaded for a different field!",
                innerArray, q.valSrc.getValues(reader).getInnerArray());
                innerArray, q.valSrc.getValues(leaf).getInnerArray());
       } catch (UnsupportedOperationException e) {
         if (!warned) {
           System.err.println("WARNING: " + testName()
@@ -229,15 +230,15 @@ public class TestOrdValues extends FunctionTestSetup {
     q = new ValueSourceQuery(vs);
     h = s.search(q, null, 1000).scoreDocs;
     assertEquals("All docs should be matched!", N_DOCS, h.length);
    readers = s.getIndexReader().getSequentialSubReaders();
    leaves = ReaderUtil.leaves(s.getTopReaderContext());
 
    for (IndexReader reader : readers) {
    for (AtomicReaderContext leaf : leaves) {
       try {
         log("compare (should differ): " + innerArray + " to "
                + q.valSrc.getValues(reader).getInnerArray());
                + q.valSrc.getValues(leaf).getInnerArray());
         assertNotSame(
                 "cached field values should not be reused if reader as changed!",
                innerArray, q.valSrc.getValues(reader).getInnerArray());
                innerArray, q.valSrc.getValues(leaf).getInnerArray());
       } catch (UnsupportedOperationException e) {
         if (!warned) {
           System.err.println("WARNING: " + testName()
diff --git a/lucene/src/test/org/apache/lucene/search/function/TestValueSource.java b/lucene/src/test/org/apache/lucene/search/function/TestValueSource.java
index 38b6e9cd91f..d6b9f780dae 100644
-- a/lucene/src/test/org/apache/lucene/search/function/TestValueSource.java
++ b/lucene/src/test/org/apache/lucene/search/function/TestValueSource.java
@@ -22,6 +22,7 @@ import org.apache.lucene.store.*;
 import org.apache.lucene.search.*;
 import org.apache.lucene.analysis.*;
 import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.document.*;
 
 public class TestValueSource extends LuceneTestCase {
@@ -45,11 +46,17 @@ public class TestValueSource extends LuceneTestCase {
     assertTrue(r.getSequentialSubReaders().length > 1);
 
     ValueSource s1 = new IntFieldSource("field");
    DocValues v1 = s1.getValues(r);
    DocValues v2 = new MultiValueSource(s1).getValues(r);

    AtomicReaderContext[] leaves = ReaderUtil.leaves(r.getTopReaderContext());
    DocValues v1 = null;
    DocValues v2 = new MultiValueSource(s1).getValues(r.getTopReaderContext());
    int leafOrd = -1;
     for(int i=0;i<r.maxDoc();i++) {
      assertEquals(v1.intVal(i), i);
      int subIndex = ReaderUtil.subIndex(i, leaves);
      if (subIndex != leafOrd) {
        leafOrd = subIndex;
        v1 = s1.getValues(leaves[leafOrd]);
      }
      assertEquals(v1.intVal(i - leaves[leafOrd].docBase), i);
       assertEquals(v2.intVal(i), i);
     }
 
diff --git a/solr/src/java/org/apache/solr/core/SolrCore.java b/solr/src/java/org/apache/solr/core/SolrCore.java
index 1d61661f82d..b1774338967 100644
-- a/solr/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/src/java/org/apache/solr/core/SolrCore.java
@@ -1005,7 +1005,7 @@ public final class SolrCore implements SolrInfoMBean {
       
       if (newestSearcher != null && solrConfig.reopenReaders
           && indexDirFile.equals(newIndexDirFile)) {
        IndexReader currentReader = newestSearcher.get().getReader();
        IndexReader currentReader = newestSearcher.get().getIndexReader();
         IndexReader newReader = currentReader.reopen();
 
         if (newReader == currentReader) {
diff --git a/solr/src/java/org/apache/solr/handler/MoreLikeThisHandler.java b/solr/src/java/org/apache/solr/handler/MoreLikeThisHandler.java
index bad251ccb12..e367d8922f4 100644
-- a/solr/src/java/org/apache/solr/handler/MoreLikeThisHandler.java
++ b/solr/src/java/org/apache/solr/handler/MoreLikeThisHandler.java
@@ -284,7 +284,7 @@ public class MoreLikeThisHandler extends RequestHandlerBase
     public MoreLikeThisHelper( SolrParams params, SolrIndexSearcher searcher )
     {
       this.searcher = searcher;
      this.reader = searcher.getReader();
      this.reader = searcher.getIndexReader();
       this.uniqueKeyField = searcher.getSchema().getUniqueKeyField();
       this.needDocSet = params.getBool(FacetParams.FACET,false);
       
diff --git a/solr/src/java/org/apache/solr/handler/ReplicationHandler.java b/solr/src/java/org/apache/solr/handler/ReplicationHandler.java
index a9983c798c8..d08cdb94cd2 100644
-- a/solr/src/java/org/apache/solr/handler/ReplicationHandler.java
++ b/solr/src/java/org/apache/solr/handler/ReplicationHandler.java
@@ -281,7 +281,7 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
       IndexCommit indexCommit = delPolicy.getLatestCommit();
 
       if(indexCommit == null) {
        indexCommit = req.getSearcher().getReader().getIndexCommit();
        indexCommit = req.getSearcher().getIndexReader().getIndexCommit();
       }
 
       // small race here before the commit point is saved
@@ -481,8 +481,8 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
     long version[] = new long[2];
     RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
     try {
      version[0] = searcher.get().getReader().getIndexCommit().getVersion();
      version[1] = searcher.get().getReader().getIndexCommit().getGeneration();
      version[0] = searcher.get().getIndexReader().getIndexCommit().getVersion();
      version[1] = searcher.get().getIndexReader().getIndexCommit().getGeneration();
     } catch (IOException e) {
       LOG.warn("Unable to get index version : ", e);
     } finally {
@@ -823,7 +823,7 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
         replicateOnStart = true;
         RefCounted<SolrIndexSearcher> s = core.getNewestSearcher(false);
         try {
          IndexReader reader = s==null ? null : s.get().getReader();
          IndexReader reader = s==null ? null : s.get().getIndexReader();
           if (reader!=null && reader.getIndexCommit() != null && reader.getIndexCommit().getGeneration() != 1L) {
             try {
               if(replicateOnOptimize){
diff --git a/solr/src/java/org/apache/solr/handler/SnapPuller.java b/solr/src/java/org/apache/solr/handler/SnapPuller.java
index 1a41f827680..88ac16671cf 100644
-- a/solr/src/java/org/apache/solr/handler/SnapPuller.java
++ b/solr/src/java/org/apache/solr/handler/SnapPuller.java
@@ -269,7 +269,7 @@ public class SnapPuller {
       RefCounted<SolrIndexSearcher> searcherRefCounted = null;
       try {
         searcherRefCounted = core.getNewestSearcher(false);
        commit = searcherRefCounted.get().getReader().getIndexCommit();
        commit = searcherRefCounted.get().getIndexReader().getIndexCommit();
       } finally {
         if (searcherRefCounted != null)
           searcherRefCounted.decref();
diff --git a/solr/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java b/solr/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java
index 39dc8cd136a..aa0d3ee9b9a 100644
-- a/solr/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java
++ b/solr/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java
@@ -470,7 +470,7 @@ public class CoreAdminHandler extends RequestHandlerBase {
         info.add("uptime", System.currentTimeMillis() - core.getStartTime());
         RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
         try {
          info.add("index", LukeRequestHandler.getIndexInfo(searcher.get().getReader(), false));
          info.add("index", LukeRequestHandler.getIndexInfo(searcher.get().getIndexReader(), false));
         } finally {
           searcher.decref();
         }
diff --git a/solr/src/java/org/apache/solr/handler/admin/LukeRequestHandler.java b/solr/src/java/org/apache/solr/handler/admin/LukeRequestHandler.java
index 76c28f85720..c11b0ace7f3 100644
-- a/solr/src/java/org/apache/solr/handler/admin/LukeRequestHandler.java
++ b/solr/src/java/org/apache/solr/handler/admin/LukeRequestHandler.java
@@ -97,7 +97,7 @@ public class LukeRequestHandler extends RequestHandlerBase
   {    
     IndexSchema schema = req.getSchema();
     SolrIndexSearcher searcher = req.getSearcher();
    IndexReader reader = searcher.getReader();
    IndexReader reader = searcher.getIndexReader();
     SolrParams params = req.getParams();
     int numTerms = params.getInt( NUMTERMS, DEFAULT_COUNT );
         
@@ -285,7 +285,7 @@ public class LukeRequestHandler extends RequestHandlerBase
     final SolrIndexSearcher searcher, final Set<String> fields, final int numTerms ) 
     throws Exception {
 
    IndexReader reader = searcher.getReader();
    IndexReader reader = searcher.getIndexReader();
     IndexSchema schema = searcher.getSchema();
     
     // Walk the term enum and keep a priority queue for each map in our set
diff --git a/solr/src/java/org/apache/solr/handler/component/HighlightComponent.java b/solr/src/java/org/apache/solr/handler/component/HighlightComponent.java
index 255a75691f1..10070d795f7 100644
-- a/solr/src/java/org/apache/solr/handler/component/HighlightComponent.java
++ b/solr/src/java/org/apache/solr/handler/component/HighlightComponent.java
@@ -113,7 +113,7 @@ public class HighlightComponent extends SearchComponent implements PluginInfoIni
       
       if(highlightQuery != null) {
         boolean rewrite = !(Boolean.valueOf(req.getParams().get(HighlightParams.USE_PHRASE_HIGHLIGHTER, "true")) && Boolean.valueOf(req.getParams().get(HighlightParams.HIGHLIGHT_MULTI_TERM, "true")));
        highlightQuery = rewrite ?  highlightQuery.rewrite(req.getSearcher().getReader()) : highlightQuery;
        highlightQuery = rewrite ?  highlightQuery.rewrite(req.getSearcher().getIndexReader()) : highlightQuery;
       }
       
       // No highlighting if there is no query -- consider q.alt="*:*
diff --git a/solr/src/java/org/apache/solr/handler/component/QueryElevationComponent.java b/solr/src/java/org/apache/solr/handler/component/QueryElevationComponent.java
index 65061e231b0..8b4af7d715d 100644
-- a/solr/src/java/org/apache/solr/handler/component/QueryElevationComponent.java
++ b/solr/src/java/org/apache/solr/handler/component/QueryElevationComponent.java
@@ -200,7 +200,7 @@ public class QueryElevationComponent extends SearchComponent implements SolrCore
           RefCounted<SolrIndexSearcher> searchHolder = null;
           try {
             searchHolder = core.getNewestSearcher(false);
            IndexReader reader = searchHolder.get().getReader();
            IndexReader reader = searchHolder.get().getIndexReader();
             getElevationMap( reader, core );
           } finally {
             if (searchHolder != null) searchHolder.decref();
@@ -344,7 +344,7 @@ public class QueryElevationComponent extends SearchComponent implements SolrCore
     }
 
     qstr = getAnalyzedQuery(qstr);
    IndexReader reader = req.getSearcher().getReader();
    IndexReader reader = req.getSearcher().getIndexReader();
     ElevationObj booster = null;
     try {
       booster = getElevationMap( reader, req.getCore() ).get( qstr );
diff --git a/solr/src/java/org/apache/solr/handler/component/SpellCheckComponent.java b/solr/src/java/org/apache/solr/handler/component/SpellCheckComponent.java
index efd685dba04..524fc3fa572 100644
-- a/solr/src/java/org/apache/solr/handler/component/SpellCheckComponent.java
++ b/solr/src/java/org/apache/solr/handler/component/SpellCheckComponent.java
@@ -143,7 +143,7 @@ public class SpellCheckComponent extends SearchComponent implements SolrCoreAwar
         boolean extendedResults = params.getBool(SPELLCHECK_EXTENDED_RESULTS,
             false);
         NamedList response = new SimpleOrderedMap();
        IndexReader reader = rb.req.getSearcher().getReader();
        IndexReader reader = rb.req.getSearcher().getIndexReader();
         boolean collate = params.getBool(SPELLCHECK_COLLATE, false);
         float accuracy = params.getFloat(SPELLCHECK_ACCURACY, Float.MIN_VALUE);
         SolrParams customParams = getCustomParams(getDictionaryName(params), params, shardRequest);
@@ -678,7 +678,7 @@ public class SpellCheckComponent extends SearchComponent implements SolrCoreAwar
         if (buildOnCommit)  {
           buildSpellIndex(newSearcher);
         } else if (buildOnOptimize) {
          if (newSearcher.getReader().isOptimized())  {
          if (newSearcher.getIndexReader().isOptimized())  {
             buildSpellIndex(newSearcher);
           } else  {
             LOG.info("Index is not optimized therefore skipping building spell check index for: " + checker.getDictionaryName());
diff --git a/solr/src/java/org/apache/solr/handler/component/StatsComponent.java b/solr/src/java/org/apache/solr/handler/component/StatsComponent.java
index dc0b4333047..64af5b95ebb 100644
-- a/solr/src/java/org/apache/solr/handler/component/StatsComponent.java
++ b/solr/src/java/org/apache/solr/handler/component/StatsComponent.java
@@ -248,7 +248,7 @@ class SimpleStats {
 
     FieldCache.DocTermsIndex si = null;
     try {
      si = FieldCache.DEFAULT.getTermsIndex(searcher.getReader(), fieldName);
      si = FieldCache.DEFAULT.getTermsIndex(searcher.getIndexReader(), fieldName);
     } 
     catch (IOException e) {
       throw new RuntimeException( "failed to open field cache for: "+fieldName, e );
@@ -263,7 +263,7 @@ class SimpleStats {
     for( String f : facet ) {
       ft = searcher.getSchema().getFieldType(f);
       try {
        si = FieldCache.DEFAULT.getTermsIndex(searcher.getReader(), f);
        si = FieldCache.DEFAULT.getTermsIndex(searcher.getIndexReader(), f);
       } 
       catch (IOException e) {
         throw new RuntimeException( "failed to open field cache for: "+f, e );
diff --git a/solr/src/java/org/apache/solr/handler/component/TermVectorComponent.java b/solr/src/java/org/apache/solr/handler/component/TermVectorComponent.java
index 59310c6fa14..1fea0ac618b 100644
-- a/solr/src/java/org/apache/solr/handler/component/TermVectorComponent.java
++ b/solr/src/java/org/apache/solr/handler/component/TermVectorComponent.java
@@ -174,7 +174,7 @@ public class TermVectorComponent extends SearchComponent implements SolrCoreAwar
     }
     SolrIndexSearcher searcher = rb.req.getSearcher();
 
    IndexReader reader = searcher.getReader();
    IndexReader reader = searcher.getIndexReader();
     //the TVMapper is a TermVectorMapper which can be used to optimize loading of Term Vectors
     SchemaField keyField = schema.getUniqueKeyField();
     String uniqFieldName = null;
diff --git a/solr/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java b/solr/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
index a032da6f55a..41604fab4b1 100644
-- a/solr/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
++ b/solr/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
@@ -216,7 +216,7 @@ public class DefaultSolrHighlighter extends SolrHighlighter implements PluginInf
   private Scorer getQueryScorer(Query query, String fieldName, SolrQueryRequest request) {
      boolean reqFieldMatch = request.getParams().getFieldBool(fieldName, HighlightParams.FIELD_MATCH, false);
      if (reqFieldMatch) {
        return new QueryTermScorer(query, request.getSearcher().getReader(), fieldName);
        return new QueryTermScorer(query, request.getSearcher().getIndexReader(), fieldName);
      }
      else {
         return new QueryTermScorer(query);
@@ -415,7 +415,7 @@ public class DefaultSolrHighlighter extends SolrHighlighter implements PluginInf
 
     TermOffsetsTokenStream tots = null; // to be non-null iff we're using TermOffsets optimization
     try {
        TokenStream tvStream = TokenSources.getTokenStream(searcher.getReader(), docId, fieldName);
        TokenStream tvStream = TokenSources.getTokenStream(searcher.getIndexReader(), docId, fieldName);
         if (tvStream != null) {
           tots = new TermOffsetsTokenStream(tvStream);
         }
@@ -503,7 +503,7 @@ public class DefaultSolrHighlighter extends SolrHighlighter implements PluginInf
       String fieldName ) throws IOException {
     SolrParams params = req.getParams(); 
     SolrFragmentsBuilder solrFb = getSolrFragmentsBuilder( fieldName, params );
    String[] snippets = highlighter.getBestFragments( fieldQuery, req.getSearcher().getReader(), docId, fieldName,
    String[] snippets = highlighter.getBestFragments( fieldQuery, req.getSearcher().getIndexReader(), docId, fieldName,
         params.getFieldInt( fieldName, HighlightParams.FRAGSIZE, 100 ),
         params.getFieldInt( fieldName, HighlightParams.SNIPPETS, 1 ),
         getFragListBuilder( fieldName, params ),
diff --git a/solr/src/java/org/apache/solr/request/SimpleFacets.java b/solr/src/java/org/apache/solr/request/SimpleFacets.java
index 15ef35d7c15..65c948edabe 100644
-- a/solr/src/java/org/apache/solr/request/SimpleFacets.java
++ b/solr/src/java/org/apache/solr/request/SimpleFacets.java
@@ -413,7 +413,7 @@ public class SimpleFacets {
     FieldType ft = searcher.getSchema().getFieldType(fieldName);
     NamedList<Integer> res = new NamedList<Integer>();
 
    FieldCache.DocTermsIndex si = FieldCache.DEFAULT.getTermsIndex(searcher.getReader(), fieldName);
    FieldCache.DocTermsIndex si = FieldCache.DEFAULT.getTermsIndex(searcher.getIndexReader(), fieldName);
 
     final BytesRef prefixRef;
     if (prefix == null) {
@@ -611,7 +611,7 @@ public class SimpleFacets {
 
 
     IndexSchema schema = searcher.getSchema();
    IndexReader r = searcher.getReader();
    IndexReader r = searcher.getIndexReader();
     FieldType ft = schema.getFieldType(field);
 
     boolean sortByCount = sort.equals("count") || sort.equals("true");
diff --git a/solr/src/java/org/apache/solr/request/UnInvertedField.java b/solr/src/java/org/apache/solr/request/UnInvertedField.java
index 7e6bf9978db..d724961cb22 100755
-- a/solr/src/java/org/apache/solr/request/UnInvertedField.java
++ b/solr/src/java/org/apache/solr/request/UnInvertedField.java
@@ -192,7 +192,7 @@ public class UnInvertedField {
   private void uninvert(SolrIndexSearcher searcher) throws IOException {
     long startTime = System.currentTimeMillis();
 
    IndexReader reader = searcher.getReader();
    IndexReader reader = searcher.getIndexReader();
     int maxDoc = reader.maxDoc();
 
     int[] index = new int[maxDoc];       // immediate term numbers, or the index into the byte[] representing the last number
@@ -481,7 +481,7 @@ public class UnInvertedField {
       int startTerm = 0;
       int endTerm = numTermsInField;  // one past the end
 
      NumberedTermsEnum te = ti.getEnumerator(searcher.getReader());
      NumberedTermsEnum te = ti.getEnumerator(searcher.getIndexReader());
       if (prefix != null && prefix.length() > 0) {
         BytesRef prefixBr = new BytesRef(prefix);
         te.skipTo(prefixBr);
@@ -719,7 +719,7 @@ public class UnInvertedField {
     for (String f : facet) {
       FieldType facet_ft = searcher.getSchema().getFieldType(f);
       try {
        si = FieldCache.DEFAULT.getTermsIndex(searcher.getReader(), f);
        si = FieldCache.DEFAULT.getTermsIndex(searcher.getIndexReader(), f);
       }
       catch (IOException e) {
         throw new RuntimeException("failed to open field cache for: " + f, e);
@@ -731,7 +731,7 @@ public class UnInvertedField {
     final int[] index = this.index;
     final int[] counts = new int[numTermsInField];//keep track of the number of times we see each word in the field for all the documents in the docset
 
    NumberedTermsEnum te = ti.getEnumerator(searcher.getReader());
    NumberedTermsEnum te = ti.getEnumerator(searcher.getIndexReader());
 
 
     boolean doNegative = false;
diff --git a/solr/src/java/org/apache/solr/schema/DateField.java b/solr/src/java/org/apache/solr/schema/DateField.java
index 09b091fdc8c..d60584a5f3a 100644
-- a/solr/src/java/org/apache/solr/schema/DateField.java
++ b/solr/src/java/org/apache/solr/schema/DateField.java
@@ -18,7 +18,7 @@
 package org.apache.solr.schema;
 
 import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TermRangeQuery;
@@ -427,8 +427,8 @@ class DateFieldSource extends FieldCacheSource {
     return "date(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    return new StringIndexDocValues(this, reader, field) {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    return new StringIndexDocValues(this, readerContext, field) {
       protected String toTerm(String readableValue) {
         // needed for frange queries to work properly
         return ft.toInternal(readableValue);
diff --git a/solr/src/java/org/apache/solr/schema/LatLonType.java b/solr/src/java/org/apache/solr/schema/LatLonType.java
index 2e35dcf72db..a1d0fdfeab5 100644
-- a/solr/src/java/org/apache/solr/schema/LatLonType.java
++ b/solr/src/java/org/apache/solr/schema/LatLonType.java
@@ -20,7 +20,6 @@ import org.apache.lucene.document.Field;
 import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.search.*;
 import org.apache.lucene.spatial.DistanceUtils;
 import org.apache.lucene.spatial.tier.InvalidGeoException;
@@ -372,7 +371,7 @@ class SpatialDistanceQuery extends Query {
 
     @Override
     public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new SpatialScorer(getSimilarity(searcher), context.reader, this);
      return new SpatialScorer(getSimilarity(searcher), context, this);
     }
 
     @Override
@@ -405,15 +404,15 @@ class SpatialDistanceQuery extends Query {
     int lastDistDoc;
     double lastDist;
 
    public SpatialScorer(Similarity similarity, IndexReader reader, SpatialWeight w) throws IOException {
    public SpatialScorer(Similarity similarity, AtomicReaderContext readerContext, SpatialWeight w) throws IOException {
       super(similarity);
       this.weight = w;
       this.qWeight = w.getValue();
      this.reader = reader;
      this.reader = readerContext.reader;
       this.maxDoc = reader.maxDoc();
      this.delDocs = reader.hasDeletions() ? MultiFields.getDeletedDocs(reader) : null;
      latVals = latSource.getValues(weight.latContext, reader);
      lonVals = lonSource.getValues(weight.lonContext, reader);
      this.delDocs = reader.getDeletedDocs();
      latVals = latSource.getValues(weight.latContext, readerContext);
      lonVals = lonSource.getValues(weight.lonContext, readerContext);
 
       this.lonMin = SpatialDistanceQuery.this.lonMin;
       this.lonMax = SpatialDistanceQuery.this.lonMax;
diff --git a/solr/src/java/org/apache/solr/schema/RandomSortField.java b/solr/src/java/org/apache/solr/schema/RandomSortField.java
index 848e8206f9d..fd69557e3b1 100644
-- a/solr/src/java/org/apache/solr/schema/RandomSortField.java
++ b/solr/src/java/org/apache/solr/schema/RandomSortField.java
@@ -24,11 +24,11 @@ import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.*;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.solr.response.TextResponseWriter;
 import org.apache.solr.search.QParser;
 import org.apache.solr.search.function.DocValues;
 import org.apache.solr.search.function.ValueSource;
import org.apache.solr.search.SolrIndexReader;
 
 /**
  * Utility Field used for random sorting.  It should not be passed a value.
@@ -78,17 +78,11 @@ public class RandomSortField extends FieldType {
    * Given a field name and an IndexReader, get a random hash seed.  
    * Using dynamic fields, you can force the random order to change 
    */
  private static int getSeed(String fieldName, IndexReader r) {
    SolrIndexReader top = (SolrIndexReader)r;
    int base=0;
    while (top.getParent() != null) {
      base += top.getBase();
      top = top.getParent();
    }

  private static int getSeed(String fieldName, AtomicReaderContext context) {
    final IndexReader top = ReaderUtil.getTopLevelContext(context).reader;
     // calling getVersion() on a segment will currently give you a null pointer exception, so
     // we use the top-level reader.
    return fieldName.hashCode() + base + (int)top.getVersion();
    return fieldName.hashCode() + context.docBase + (int)top.getVersion();
   }
   
   @Override
@@ -129,7 +123,7 @@ public class RandomSortField extends FieldType {
         }
 
         public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
          seed = getSeed(fieldname, context.reader);
          seed = getSeed(fieldname, context);
           return this;
         }
 
@@ -155,9 +149,9 @@ public class RandomSortField extends FieldType {
     }
 
     @Override
    public DocValues getValues(Map context, final IndexReader reader) throws IOException {
    public DocValues getValues(Map context, final AtomicReaderContext readerContext) throws IOException {
       return new DocValues() {
          private final int seed = getSeed(field, reader);
          private final int seed = getSeed(field, readerContext);
           @Override
           public float floatVal(int doc) {
             return (float)hash(doc+seed);
diff --git a/solr/src/java/org/apache/solr/schema/SortableDoubleField.java b/solr/src/java/org/apache/solr/schema/SortableDoubleField.java
index f1744efc688..411e9b5f6fc 100644
-- a/solr/src/java/org/apache/solr/schema/SortableDoubleField.java
++ b/solr/src/java/org/apache/solr/schema/SortableDoubleField.java
@@ -28,7 +28,7 @@ import org.apache.solr.search.function.FieldCacheSource;
 import org.apache.solr.search.function.DocValues;
 import org.apache.solr.search.function.StringIndexDocValues;
 import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.solr.util.ByteUtils;
 import org.apache.solr.util.NumberUtils;
 import org.apache.solr.response.TextResponseWriter;
@@ -99,10 +99,10 @@ class SortableDoubleFieldSource extends FieldCacheSource {
     return "sdouble(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     final double def = defVal;
 
    return new StringIndexDocValues(this, reader, field) {
    return new StringIndexDocValues(this, readerContext, field) {
       private final BytesRef spare = new BytesRef();
 
       protected String toTerm(String readableValue) {
diff --git a/solr/src/java/org/apache/solr/schema/SortableFloatField.java b/solr/src/java/org/apache/solr/schema/SortableFloatField.java
index 407c17f90e3..e56ffd70c2a 100644
-- a/solr/src/java/org/apache/solr/schema/SortableFloatField.java
++ b/solr/src/java/org/apache/solr/schema/SortableFloatField.java
@@ -28,7 +28,7 @@ import org.apache.solr.search.function.FieldCacheSource;
 import org.apache.solr.search.function.DocValues;
 import org.apache.solr.search.function.StringIndexDocValues;
 import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.solr.util.ByteUtils;
 import org.apache.solr.util.NumberUtils;
 import org.apache.solr.response.TextResponseWriter;
@@ -99,10 +99,10 @@ class SortableFloatFieldSource extends FieldCacheSource {
     return "sfloat(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     final float def = defVal;
 
    return new StringIndexDocValues(this, reader, field) {
    return new StringIndexDocValues(this, readerContext, field) {
       private final BytesRef spare = new BytesRef();
 
       protected String toTerm(String readableValue) {
diff --git a/solr/src/java/org/apache/solr/schema/SortableIntField.java b/solr/src/java/org/apache/solr/schema/SortableIntField.java
index 3771d3eb0e2..b6db1cff194 100644
-- a/solr/src/java/org/apache/solr/schema/SortableIntField.java
++ b/solr/src/java/org/apache/solr/schema/SortableIntField.java
@@ -28,7 +28,7 @@ import org.apache.solr.search.function.FieldCacheSource;
 import org.apache.solr.search.function.DocValues;
 import org.apache.solr.search.function.StringIndexDocValues;
 import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.solr.util.ByteUtils;
 import org.apache.solr.util.NumberUtils;
 import org.apache.solr.response.TextResponseWriter;
@@ -101,10 +101,10 @@ class SortableIntFieldSource extends FieldCacheSource {
     return "sint(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     final int def = defVal;
 
    return new StringIndexDocValues(this, reader, field) {
    return new StringIndexDocValues(this, readerContext, field) {
       private final BytesRef spare = new BytesRef();
 
       protected String toTerm(String readableValue) {
diff --git a/solr/src/java/org/apache/solr/schema/SortableLongField.java b/solr/src/java/org/apache/solr/schema/SortableLongField.java
index b9657d1451a..3be76b9b1c1 100644
-- a/solr/src/java/org/apache/solr/schema/SortableLongField.java
++ b/solr/src/java/org/apache/solr/schema/SortableLongField.java
@@ -28,7 +28,7 @@ import org.apache.solr.search.function.FieldCacheSource;
 import org.apache.solr.search.function.DocValues;
 import org.apache.solr.search.function.StringIndexDocValues;
 import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.solr.util.ByteUtils;
 import org.apache.solr.util.NumberUtils;
 import org.apache.solr.response.TextResponseWriter;
@@ -100,10 +100,10 @@ class SortableLongFieldSource extends FieldCacheSource {
     return "slong(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     final long def = defVal;
 
    return new StringIndexDocValues(this, reader, field) {
    return new StringIndexDocValues(this, readerContext, field) {
       private final BytesRef spare = new BytesRef();
 
       protected String toTerm(String readableValue) {
diff --git a/solr/src/java/org/apache/solr/schema/StrFieldSource.java b/solr/src/java/org/apache/solr/schema/StrFieldSource.java
index ab12750a084..36dcfcefcee 100755
-- a/solr/src/java/org/apache/solr/schema/StrFieldSource.java
++ b/solr/src/java/org/apache/solr/schema/StrFieldSource.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.schema;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.search.function.DocValues;
 import org.apache.solr.search.function.FieldCacheSource;
@@ -36,8 +36,8 @@ public class StrFieldSource extends FieldCacheSource {
     return "str(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    return new StringIndexDocValues(this, reader, field) {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    return new StringIndexDocValues(this, readerContext, field) {
       protected String toTerm(String readableValue) {
         return readableValue;
       }
diff --git a/solr/src/java/org/apache/solr/search/Grouping.java b/solr/src/java/org/apache/solr/search/Grouping.java
index bc7e858c2ac..894b592f3e1 100755
-- a/solr/src/java/org/apache/solr/search/Grouping.java
++ b/solr/src/java/org/apache/solr/search/Grouping.java
@@ -688,7 +688,7 @@ class TopGroupCollector extends GroupCollector {
   @Override
   public void setNextReader(AtomicReaderContext readerContext) throws IOException {
     this.docBase = readerContext.docBase;
    docValues = vs.getValues(context, readerContext.reader);
    docValues = vs.getValues(context, readerContext);
     filler = docValues.getValueFiller();
     mval = filler.getValue();
     for (int i=0; i<comparators.length; i++)
@@ -762,7 +762,7 @@ class Phase2GroupCollector extends Collector {
   @Override
   public void setNextReader(AtomicReaderContext readerContext) throws IOException {
     this.docBase = readerContext.docBase;
    docValues = vs.getValues(context, readerContext.reader);
    docValues = vs.getValues(context, readerContext);
     filler = docValues.getValueFiller();
     mval = filler.getValue();
     for (SearchGroupDocs group : groupMap.values())
diff --git a/solr/src/java/org/apache/solr/search/SolrFilter.java b/solr/src/java/org/apache/solr/search/SolrFilter.java
index 1eddff4ff43..3b57d612f73 100644
-- a/solr/src/java/org/apache/solr/search/SolrFilter.java
++ b/solr/src/java/org/apache/solr/search/SolrFilter.java
@@ -22,7 +22,7 @@ import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.util.Map;
 import java.io.IOException;
@@ -39,7 +39,7 @@ public abstract class SolrFilter extends Filter {
    * The context object will be passed to getDocIdSet() where this info can be retrieved. */
   public abstract void createWeight(Map context, IndexSearcher searcher) throws IOException;
   
  public abstract DocIdSet getDocIdSet(Map context, ReaderContext readerContext) throws IOException;
  public abstract DocIdSet getDocIdSet(Map context, AtomicReaderContext readerContext) throws IOException;
 
   @Override
   public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
diff --git a/solr/src/java/org/apache/solr/search/SolrIndexReader.java b/solr/src/java/org/apache/solr/search/SolrIndexReader.java
deleted file mode 100755
index 0440e57f7a6..00000000000
-- a/solr/src/java/org/apache/solr/search/SolrIndexReader.java
++ /dev/null
@@ -1,505 +0,0 @@
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

package org.apache.solr.search;


import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ReaderUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Solr wrapper for IndexReader that contains extra context.
 * This is currently experimental, for internal use only, and subject to change.
 */
public class SolrIndexReader extends FilterIndexReader {
  private final SolrIndexReader[] subReaders;
  private final SolrIndexReader[] leafReaders;
  private int[] leafOffsets;
  private final SolrIndexReader parent;
  private final int base; // docid offset of this reader within parent
  private final ReaderContext topLevelContext;

  private static int[] zeroIntArray = new int[]{0};

  // top level searcher for this reader tree
  // a bit if a hack currently... searcher needs to set
  SolrIndexSearcher searcher;

  // Shared info about the wrapped reader.
  private SolrReaderInfo info;

  /** Recursively wrap an IndexReader in SolrIndexReader instances.
   * @param in  the reader to wrap
   * @param parent the parent, if any (null if none)
   * @param base the docid offset in the parent (0 if top level)
   */
  public SolrIndexReader(IndexReader in, SolrIndexReader parent, int base) {
    super(in);
    assert(!(in instanceof SolrIndexReader));
    this.parent = parent;
    this.base = base;
    IndexReader subs[] = in.getSequentialSubReaders();
    if (subs != null) {
      subReaders = new SolrIndexReader[subs.length]; 
      int numLeaves = subs.length;
      leafOffsets = new int[numLeaves];
      int b=0;
      for (int i=0; i<subReaders.length; i++) {
        SolrIndexReader sir = subReaders[i] = new SolrIndexReader(subs[i], this, b);
        leafOffsets[i] = b;
        b += sir.maxDoc();
        IndexReader subLeaves[] = sir.leafReaders;
        numLeaves += subLeaves.length - 1;  // subtract 1 for the parent
      }
      leafReaders = getLeaves(numLeaves);
    } else {
      subReaders = null;
      leafReaders = new SolrIndexReader[]{this};
      leafOffsets = zeroIntArray;
    }
    topLevelContext = ReaderUtil.buildReaderContext(this);
  }

  private SolrIndexReader[] getLeaves(int numLeaves) {
    // fast path for a normal multiReader
    if (subReaders==null || numLeaves == subReaders.length) return subReaders;

    SolrIndexReader[] leaves = new SolrIndexReader[numLeaves];
    leafOffsets = new int[numLeaves];
    
    int i=0;
    int b = 0;
    for (SolrIndexReader sir : subReaders) {
      SolrIndexReader subLeaves[] = sir.leafReaders;
      if (subLeaves == null) {
        leafOffsets[i] = b;
        b += sir.maxDoc();
        leaves[i++] = sir;
      } else {
        for (SolrIndexReader subLeaf : subLeaves) {
          leafOffsets[i] = b;
          b += subLeaf.maxDoc();
          leaves[i++] = subLeaf;
        }
      }
    }
    assert(i == numLeaves && b == maxDoc());
    return leaves;
  }

  /** return the leaf readers in this reader tree, or an array of size 1 containing "this" if "this" is a leaf */
  public SolrIndexReader[] getLeafReaders() {
    return leafReaders;
  }

  /** Return the doc id offsets for each leaf reader.  This will be different than getBase() for
   * any leaf reader who is not a direct descendant of "this".
   */
  public int[] getLeafOffsets() {
    return leafOffsets;
  }

  /** Given an array of IndexReader offsets, find which contains the given doc */
  public static int readerIndex(int doc, int[] offsets) {    // find reader for doc doc:
    int high = offsets.length - 1;

    // fast-path for a big optimized index and a bunch of smaller ones.
    if (high <= 0 || doc < offsets[1]) return 0;

    int low = 1;

    while (high >= low) {
      int mid = (low + high) >>> 1;
      int offset = offsets[mid];
      // check low first since first segments are normally bigger.
      if (doc < offset)
        high = mid - 1;
      else if (doc > offset) {
        low = mid + 1;
      }
      else {
        // exact match on the offset.
        return mid;
      }
    }
    // low is the insertion point, high should be just below that (and the segment we want).
    return high;
  }

  static String shortName(Object o) {
    return o.getClass().getSimpleName()+ "@" + Integer.toHexString(o.hashCode());
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SolrIndexReader{this=").append(Integer.toHexString(this.hashCode()));
    sb.append(",r=").append(shortName(in));
    sb.append(",refCnt=").append(getRefCount());
    sb.append(",segments=");
    sb.append(subReaders == null ? 1 : subReaders.length);
    if (parent != null) {
      sb.append(",parent=").append(parent.toString());
    }
    sb.append('}');
    return sb.toString();
  }

  static void setSearcher(SolrIndexReader sr, SolrIndexSearcher searcher) {
    sr.searcher = searcher;
    SolrIndexReader[] readers = sr.getSequentialSubReaders();
    if (readers == null) return;
    for (SolrIndexReader r : readers) {
      setSearcher(r, searcher);
    }
  }

   private static void buildInfoMap(SolrIndexReader other, HashMap<IndexReader, SolrReaderInfo> map) {
     if (other == null) return;
     map.put(other.getWrappedReader(), other.info);
     SolrIndexReader[] readers = other.getSequentialSubReaders();
     if (readers == null) return;
     for (SolrIndexReader r : readers) {
       buildInfoMap(r, map);
     }     
   }

   private static void setInfo(SolrIndexReader target, HashMap<IndexReader, SolrReaderInfo> map) {
     SolrReaderInfo info = map.get(target.getWrappedReader());
     if (info == null) info = new SolrReaderInfo(target.getWrappedReader());
     target.info = info;
     SolrIndexReader[] readers = target.getSequentialSubReaders();
     if (readers == null) return;
     for (SolrIndexReader r : readers) {
       setInfo(r, map);
     }     
   }

   /** Copies SolrReaderInfo instances from the source to this SolrIndexReader */
   public void associateInfo(SolrIndexReader source) {
     // seemed safer to not mess with reopen() but simply set
     // one set of caches from another reader tree.
     HashMap<IndexReader, SolrReaderInfo> map = new HashMap<IndexReader, SolrReaderInfo>();
     buildInfoMap(source, map);
     setInfo(this, map);
   }

  public IndexReader getWrappedReader() {
    return in;
  }

  /** returns the parent reader, or null of none */
  public SolrIndexReader getParent() {
    return parent;
  }

   /** returns the docid offset within the parent reader */
  public int getBase() {
    return base;
  }

  @Override
  public Directory directory() {
    return in.directory();
  }

  @Override
  public Bits getDeletedDocs() {
    return in.getDeletedDocs();
  }

  @Override
  public TermFreqVector[] getTermFreqVectors(int docNumber) throws IOException {
    return in.getTermFreqVectors(docNumber);
  }

  @Override
  public TermFreqVector getTermFreqVector(int docNumber, String field)
          throws IOException {
    return in.getTermFreqVector(docNumber, field);
  }

  @Override
  public void getTermFreqVector(int docNumber, String field, TermVectorMapper mapper) throws IOException {
    in.getTermFreqVector(docNumber, field, mapper);

  }

  @Override
  public void getTermFreqVector(int docNumber, TermVectorMapper mapper) throws IOException {
    in.getTermFreqVector(docNumber, mapper);
  }

  @Override
  public int numDocs() {
    return in.numDocs();
  }

  @Override
  public int maxDoc() {
    return in.maxDoc();
  }

  @Override
  public Document document(int n, FieldSelector fieldSelector) throws CorruptIndexException, IOException {
    return in.document(n, fieldSelector);
  }

  @Override
  public boolean hasDeletions() {
    return in.hasDeletions();
  }

  @Override
  protected void doUndeleteAll() throws CorruptIndexException, IOException {in.undeleteAll();}

  @Override
  public boolean hasNorms(String field) throws IOException {
    return in.hasNorms(field);
  }

  @Override
  public byte[] norms(String f) throws IOException {
    return in.norms(f);
  }

  @Override
  protected void doSetNorm(int d, String f, byte b) throws CorruptIndexException, IOException {
    in.setNorm(d, f, b);
  }

  @Override
  public Fields fields() throws IOException {
    return in.fields();
  }

  @Override
  public int docFreq(Term t) throws IOException {
    ensureOpen();
    return in.docFreq(t);
  }

  @Override
  public int docFreq(String field, BytesRef t) throws IOException {
    return in.docFreq(field, t);
  }

  @Override
  public Terms terms(String field) throws IOException {
    return in.terms(field);
  }

  @Override
  public DocsEnum termDocsEnum(Bits skipDocs, String field, BytesRef term) throws IOException {
    return in.termDocsEnum(skipDocs, field, term);
  }

  @Override
  public DocsAndPositionsEnum termPositionsEnum(Bits skipDocs, String field, BytesRef term) throws IOException {
    return in.termPositionsEnum(skipDocs, field, term);
  }

  @Override
  protected void doDelete(int n) throws  CorruptIndexException, IOException { in.deleteDocument(n); }


  // Let FilterIndexReader handle commit()... we cannot override commit()
  // or call in.commit() ourselves.
  // protected void doCommit() throws IOException { in.commit(); }

  @Override
  protected void doClose() throws IOException {
    in.close();
  }

  @Override
  public Collection getFieldNames(IndexReader.FieldOption fieldNames) {
    return in.getFieldNames(fieldNames);
  }

  @Override
  public long getVersion() {
    return in.getVersion();
  }

  @Override
  public boolean isCurrent() throws CorruptIndexException, IOException {
    return in.isCurrent();
  }

  @Override
  public boolean isOptimized() {
    return in.isOptimized();
  }

  @Override
  public SolrIndexReader[] getSequentialSubReaders() {
    return subReaders;
  }

  @Override
  public int hashCode() {
    return in.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SolrIndexReader) {
      o = ((SolrIndexReader)o).in;
    }
    return in.equals(o);
  }

  @Override
  public int getRefCount() {
    return in.getRefCount();
  }

  @Override
  public IndexReader reopen(IndexCommit commit) throws CorruptIndexException, IOException {
    return in.reopen(commit);
  }

  @Override
  public Object clone() {
    // hmmm, is this right?
    return super.clone();
  }

  @Override
  public IndexReader clone(boolean openReadOnly) throws CorruptIndexException, IOException {
    // hmmm, is this right?
    return super.clone(openReadOnly);
  }

  @Override
  public Map getCommitUserData() {
    return in.getCommitUserData();
  }

  @Override
  public long getUniqueTermCount() throws IOException {
    return in.getUniqueTermCount();
  }

  @Override
  public SolrIndexReader reopen(boolean openReadOnly) throws IOException {
    IndexReader r = in.reopen(openReadOnly);
    if (r == in) {
      return this;
    }
    SolrIndexReader sr = new SolrIndexReader(r, null, 0);
    sr.associateInfo(this);
    return sr;
  }

  @Override
  public SolrIndexReader reopen() throws CorruptIndexException, IOException {
    return reopen(true);
  }

  @Override
  public void decRef() throws IOException {
    in.decRef();
  }

  @Override
  public void deleteDocument(int docNum) throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {
    in.deleteDocument(docNum);
  }

  @Override
  public int deleteDocuments(Term term) throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {
    return in.deleteDocuments(term);
  }

  @Override
  public Document document(int n) throws CorruptIndexException, IOException {
    return in.document(n);
  }

//  @Override
//  public String getCommitUserData() {
//    return in.getCommitUserData();
//  }

  @Override
  public IndexCommit getIndexCommit() throws IOException {
    return in.getIndexCommit();
  }

  @Override
  public void incRef() {
    in.incRef();
  }

  @Override
  public int numDeletedDocs() {
    return in.numDeletedDocs();
  }

  @Override
  public void setNorm(int doc, String field, byte value) throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {
    in.setNorm(doc, field, value);
  }

  @Override
  public void undeleteAll() throws StaleReaderException, CorruptIndexException, LockObtainFailedException, IOException {
    in.undeleteAll();
  }

  @Override
  public Object getCoreCacheKey() {
    return in.getCoreCacheKey();
  }

  @Override
  public int getTermInfosIndexDivisor() {
    return in.getTermInfosIndexDivisor();
  }
  
  @Override
  public ReaderContext getTopReaderContext() {
    return topLevelContext;
  }
}



/** SolrReaderInfo contains information that is the same for
 * every SolrIndexReader that wraps the same IndexReader.
 * Multiple SolrIndexReader instances will be accessing this
 * class concurrently.
 */
class SolrReaderInfo {
  private final IndexReader reader;
  public SolrReaderInfo(IndexReader reader) {
    this.reader = reader;
  }
  public IndexReader getReader() { return reader; }

}
\ No newline at end of file
diff --git a/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java b/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
index 9a59dc78cbc..6dec3f0df6b 100644
-- a/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
++ b/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
@@ -68,7 +68,7 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
   private long openTime = System.currentTimeMillis();
   private long registerTime = 0;
   private long warmupTime = 0;
  private final SolrIndexReader reader;
  private final IndexReader reader;
   private final boolean closeReader;
 
   private final int queryResultWindowSize;
@@ -117,28 +117,15 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
     this(core, schema,name,r, false, enableCache);
   }
 
  private static SolrIndexReader wrap(IndexReader r) {
    SolrIndexReader sir;
    // wrap the reader
    if (!(r instanceof SolrIndexReader)) {
      sir = new SolrIndexReader(r, null, 0);
      sir.associateInfo(null);
    } else {
      sir = (SolrIndexReader)r;
    }
    return sir;
  }
 
   public SolrIndexSearcher(SolrCore core, IndexSchema schema, String name, IndexReader r, boolean closeReader, boolean enableCache) {
    super(wrap(r));
    this.reader = (SolrIndexReader)super.getIndexReader();
    super(r);
    this.reader = getIndexReader();
     this.core = core;
     this.schema = schema;
     this.name = "Searcher@" + Integer.toHexString(hashCode()) + (name!=null ? " "+name : "");
     log.info("Opening " + this.name);
 
    SolrIndexReader.setSearcher(reader, this);

     if (r.directory() instanceof FSDirectory) {
       FSDirectory fsDirectory = (FSDirectory) r.directory();
       indexDir = fsDirectory.getDirectory().getAbsolutePath();
@@ -247,8 +234,6 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
     numCloses.incrementAndGet();
   }
 
  /** Direct access to the IndexReader used by this searcher */
  public SolrIndexReader getReader() { return reader; }
   /** Direct access to the IndexSchema for use with this searcher */
   public IndexSchema getSchema() { return schema; }
   
diff --git a/solr/src/java/org/apache/solr/search/ValueSourceParser.java b/solr/src/java/org/apache/solr/search/ValueSourceParser.java
index 5b1805ee017..b7668608f61 100755
-- a/solr/src/java/org/apache/solr/search/ValueSourceParser.java
++ b/solr/src/java/org/apache/solr/search/ValueSourceParser.java
@@ -16,7 +16,7 @@
  */
 package org.apache.solr.search;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.queryParser.ParseException;
 import org.apache.lucene.search.Query;
@@ -710,7 +710,7 @@ class LongConstValueSource extends ConstNumberSource {
     return "const(" + constant + ")";
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     return new DocValues() {
       public float floatVal(int doc) {
         return fv;
@@ -807,8 +807,8 @@ abstract class DoubleParser extends NamedParser {
     }
 
     @Override
    public DocValues getValues(Map context, IndexReader reader) throws IOException {
      final DocValues vals =  source.getValues(context, reader);
    public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
      final DocValues vals =  source.getValues(context, readerContext);
       return new DocValues() {
         public float floatVal(int doc) {
           return (float)doubleVal(doc);
@@ -862,9 +862,9 @@ abstract class Double2Parser extends NamedParser {
       return name() + "(" + a.description() + "," + b.description() + ")";
     }
 
    public DocValues getValues(Map context, IndexReader reader) throws IOException {
      final DocValues aVals =  a.getValues(context, reader);
      final DocValues bVals =  b.getValues(context, reader);
    public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
      final DocValues aVals =  a.getValues(context, readerContext);
      final DocValues bVals =  b.getValues(context, readerContext);
       return new DocValues() {
         public float floatVal(int doc) {
           return (float)doubleVal(doc);
diff --git a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
index 645f3805cb8..2ceb78ba6f7 100755
-- a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
++ b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
@@ -96,7 +96,7 @@ public class BoostedQuery extends Query {
       if(subQueryScorer == null) {
         return null;
       }
      return new BoostedQuery.CustomScorer(getSimilarity(searcher), searcher, context.reader, this, subQueryScorer, boostVal);
      return new BoostedQuery.CustomScorer(getSimilarity(searcher), context, this, subQueryScorer, boostVal);
     }
 
     @Override
@@ -105,7 +105,7 @@ public class BoostedQuery extends Query {
       if (!subQueryExpl.isMatch()) {
         return subQueryExpl;
       }
      DocValues vals = boostVal.getValues(fcontext, readerContext.reader);
      DocValues vals = boostVal.getValues(fcontext, readerContext);
       float sc = subQueryExpl.getValue() * vals.floatVal(doc);
       Explanation res = new ComplexExplanation(
         true, sc, BoostedQuery.this.toString() + ", product of:");
@@ -121,18 +121,16 @@ public class BoostedQuery extends Query {
     private final float qWeight;
     private final Scorer scorer;
     private final DocValues vals;
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final AtomicReaderContext readerContext;
 
    private CustomScorer(Similarity similarity, IndexSearcher searcher, IndexReader reader, BoostedQuery.BoostedWeight w,
    private CustomScorer(Similarity similarity, AtomicReaderContext readerContext, BoostedQuery.BoostedWeight w,
         Scorer scorer, ValueSource vs) throws IOException {
       super(similarity);
       this.weight = w;
       this.qWeight = w.getValue();
       this.scorer = scorer;
      this.reader = reader;
      this.searcher = searcher; // for explain
      this.vals = vs.getValues(weight.fcontext, reader);
      this.readerContext = readerContext;
      this.vals = vs.getValues(weight.fcontext, readerContext);
     }
 
     @Override
@@ -161,7 +159,7 @@ public class BoostedQuery extends Query {
     }
 
     public Explanation explain(int doc) throws IOException {
      Explanation subQueryExpl = weight.qWeight.explain(ValueSource.readerToContext(weight.fcontext,reader) ,doc);
      Explanation subQueryExpl = weight.qWeight.explain(readerContext ,doc);
       if (!subQueryExpl.isMatch()) {
         return subQueryExpl;
       }
diff --git a/solr/src/java/org/apache/solr/search/function/ByteFieldSource.java b/solr/src/java/org/apache/solr/search/function/ByteFieldSource.java
index 0cba6fd4c20..2dd5bb77ddf 100644
-- a/solr/src/java/org/apache/solr/search/function/ByteFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/ByteFieldSource.java
@@ -16,7 +16,7 @@ package org.apache.solr.search.function;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.cache.ByteValuesCreator;
 import org.apache.lucene.search.cache.CachedArray.ByteValues;
 
@@ -41,8 +41,8 @@ public class ByteFieldSource extends NumericFieldCacheSource<ByteValues> {
     return "byte(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final ByteValues vals = cache.getBytes(reader, field, creator);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final ByteValues vals = cache.getBytes(readerContext.reader, field, creator);
     final byte[] arr = vals.values;
     
     return new DocValues() {
diff --git a/solr/src/java/org/apache/solr/search/function/ConstValueSource.java b/solr/src/java/org/apache/solr/search/function/ConstValueSource.java
index b4a09bf0c13..846591dafb2 100755
-- a/solr/src/java/org/apache/solr/search/function/ConstValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/ConstValueSource.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.io.IOException;
 import java.util.Map;
@@ -38,7 +38,7 @@ public class ConstValueSource extends ConstNumberSource {
     return "const(" + constant + ")";
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     return new DocValues() {
       public float floatVal(int doc) {
         return constant;
diff --git a/solr/src/java/org/apache/solr/search/function/DocFreqValueSource.java b/solr/src/java/org/apache/solr/search/function/DocFreqValueSource.java
index 2fef6ac117e..641f2a9aaa8 100755
-- a/solr/src/java/org/apache/solr/search/function/DocFreqValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/DocFreqValueSource.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.util.BytesRef;
@@ -239,7 +239,7 @@ public class DocFreqValueSource extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     IndexSearcher searcher = (IndexSearcher)context.get("searcher");
     int docfreq = searcher.docFreq(new Term(indexedField, indexedBytes));
     return new ConstIntDocValues(docfreq, this);
diff --git a/solr/src/java/org/apache/solr/search/function/DoubleConstValueSource.java b/solr/src/java/org/apache/solr/search/function/DoubleConstValueSource.java
index 19c5443ca39..9df2d685f2d 100755
-- a/solr/src/java/org/apache/solr/search/function/DoubleConstValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/DoubleConstValueSource.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.io.IOException;
 import java.util.Map;
@@ -37,7 +37,7 @@ public class DoubleConstValueSource extends ConstNumberSource {
     return "const(" + constant + ")";
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     return new DocValues() {
       public float floatVal(int doc) {
         return fv;
diff --git a/solr/src/java/org/apache/solr/search/function/DoubleFieldSource.java b/solr/src/java/org/apache/solr/search/function/DoubleFieldSource.java
index de670596d84..02017aee0a6 100644
-- a/solr/src/java/org/apache/solr/search/function/DoubleFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/DoubleFieldSource.java
@@ -18,6 +18,7 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.search.cache.DoubleValuesCreator;
 import org.apache.lucene.search.cache.CachedArray.DoubleValues;
@@ -45,8 +46,8 @@ public class DoubleFieldSource extends NumericFieldCacheSource<DoubleValues> {
     return "double(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DoubleValues vals = cache.getDoubles(reader, field, creator);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DoubleValues vals = cache.getDoubles(readerContext.reader, field, creator);
     final double[] arr = vals.values;
 	final Bits valid = vals.valid;
     
diff --git a/solr/src/java/org/apache/solr/search/function/DualFloatFunction.java b/solr/src/java/org/apache/solr/search/function/DualFloatFunction.java
index 0b4b54c444f..9eaec662091 100755
-- a/solr/src/java/org/apache/solr/search/function/DualFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/DualFloatFunction.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 
 import java.io.IOException;
@@ -43,9 +43,9 @@ public abstract class DualFloatFunction extends ValueSource {
     return name() + "(" + a.description() + "," + b.description() + ")";
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues aVals =  a.getValues(context, reader);
    final DocValues bVals =  b.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues aVals =  a.getValues(context, readerContext);
    final DocValues bVals =  b.getValues(context, readerContext);
     return new DocValues() {
       public float floatVal(int doc) {
 	return func(doc, aVals, bVals);
diff --git a/solr/src/java/org/apache/solr/search/function/FileFloatSource.java b/solr/src/java/org/apache/solr/search/function/FileFloatSource.java
index c682ea8a2e0..817062c770d 100755
-- a/solr/src/java/org/apache/solr/search/function/FileFloatSource.java
++ b/solr/src/java/org/apache/solr/search/function/FileFloatSource.java
@@ -18,15 +18,17 @@ package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.lucene.util.StringHelper;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.search.QParser;
import org.apache.solr.search.SolrIndexReader;
 import org.apache.solr.util.VersionedFile;
 
 import java.io.*;
@@ -55,19 +57,12 @@ public class FileFloatSource extends ValueSource {
     return "float(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     int offset = 0;
    if (reader instanceof SolrIndexReader) {
      SolrIndexReader r = (SolrIndexReader)reader;
      while (r.getParent() != null) {
        offset += r.getBase();
        r = r.getParent();
      }
      reader = r;
    }
    ReaderContext topLevelContext = ReaderUtil.getTopLevelContext(readerContext);
     final int off = offset;
 
    final float[] arr = getCachedFloats(reader);
    final float[] arr = getCachedFloats(topLevelContext.reader);
     return new DocValues() {
       public float floatVal(int doc) {
         return arr[doc + off];
diff --git a/solr/src/java/org/apache/solr/search/function/FloatFieldSource.java b/solr/src/java/org/apache/solr/search/function/FloatFieldSource.java
index 607de808325..a985b49fe7c 100644
-- a/solr/src/java/org/apache/solr/search/function/FloatFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/FloatFieldSource.java
@@ -20,7 +20,7 @@ package org.apache.solr.search.function;
 import java.io.IOException;
 import java.util.Map;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.search.cache.FloatValuesCreator;
 import org.apache.lucene.search.cache.CachedArray.FloatValues;
@@ -45,8 +45,8 @@ public class FloatFieldSource extends NumericFieldCacheSource<FloatValues> {
     return "float(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final FloatValues vals = cache.getFloats(reader, field, creator);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final FloatValues vals = cache.getFloats(readerContext.reader, field, creator);
     final float[] arr = vals.values;
 	final Bits valid = vals.valid;
     
diff --git a/solr/src/java/org/apache/solr/search/function/FunctionQuery.java b/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
index dc0f644995e..8cd1a6f0257 100644
-- a/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
++ b/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
@@ -19,7 +19,6 @@ package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.*;
 import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.util.Bits;
@@ -115,7 +114,7 @@ public class FunctionQuery extends Query {
     final boolean hasDeletions;
     final Bits delDocs;
 
    public AllScorer(Similarity similarity, ReaderContext context, FunctionWeight w) throws IOException {
    public AllScorer(Similarity similarity, AtomicReaderContext context, FunctionWeight w) throws IOException {
       super(similarity);
       this.weight = w;
       this.qWeight = w.getValue();
@@ -124,9 +123,7 @@ public class FunctionQuery extends Query {
       this.hasDeletions = reader.hasDeletions();
       this.delDocs = MultiFields.getDeletedDocs(reader);
       assert !hasDeletions || delDocs != null;
      Map funcContext = weight.context;
      funcContext.put(reader, context);
      vals = func.getValues(funcContext, reader);
      vals = func.getValues(weight.context, context);
     }
 
     @Override
diff --git a/solr/src/java/org/apache/solr/search/function/IDFValueSource.java b/solr/src/java/org/apache/solr/search/function/IDFValueSource.java
index 5cb86fbb6b3..a9543b1c893 100755
-- a/solr/src/java/org/apache/solr/search/function/IDFValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/IDFValueSource.java
@@ -18,6 +18,7 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.util.BytesRef;
@@ -38,7 +39,7 @@ public class IDFValueSource extends DocFreqValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     IndexSearcher searcher = (IndexSearcher)context.get("searcher");
     Similarity sim = searcher.getSimilarity();
     // todo: we need docFreq that takes a BytesRef
diff --git a/solr/src/java/org/apache/solr/search/function/IntFieldSource.java b/solr/src/java/org/apache/solr/search/function/IntFieldSource.java
index 5b01abb34bf..0cee5e769cf 100644
-- a/solr/src/java/org/apache/solr/search/function/IntFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/IntFieldSource.java
@@ -18,6 +18,7 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.Bits;
 import org.apache.solr.search.MutableValueInt;
 import org.apache.solr.search.MutableValue;
@@ -45,8 +46,8 @@ public class IntFieldSource extends NumericFieldCacheSource<IntValues> {
   }
 
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final IntValues vals = cache.getInts(reader, field, creator);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final IntValues vals = cache.getInts(readerContext.reader, field, creator);
     final int[] arr = vals.values;
 	final Bits valid = vals.valid;
     
diff --git a/solr/src/java/org/apache/solr/search/function/JoinDocFreqValueSource.java b/solr/src/java/org/apache/solr/search/function/JoinDocFreqValueSource.java
index f93c9882e40..dae45789fca 100644
-- a/solr/src/java/org/apache/solr/search/function/JoinDocFreqValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/JoinDocFreqValueSource.java
@@ -21,10 +21,11 @@ import java.io.IOException;
 import java.util.Map;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldCache.DocTerms;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.solr.common.SolrException;
import org.apache.solr.search.SolrIndexReader;
 
 /**
  * Use a field value and find the Document Frequency within another field.
@@ -46,21 +47,10 @@ public class JoinDocFreqValueSource extends FieldCacheSource {
     return NAME + "(" + field +":("+qfield+"))";
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException 
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException 
   {
    final DocTerms terms = cache.getTerms(reader, field, true );
    
    int offset = 0;
    IndexReader topReader = reader;
    if (topReader instanceof SolrIndexReader) {
      SolrIndexReader r = (SolrIndexReader)topReader;
      while (r.getParent() != null) {
        offset += r.getBase();
        r = r.getParent();
      }
      topReader = r;
    }
    final IndexReader top = topReader;
    final DocTerms terms = cache.getTerms(readerContext.reader, field, true );
    final IndexReader top = ReaderUtil.getTopLevelContext(readerContext).reader;
     
     return new DocValues() {
       BytesRef ref = new BytesRef();
diff --git a/solr/src/java/org/apache/solr/search/function/LinearFloatFunction.java b/solr/src/java/org/apache/solr/search/function/LinearFloatFunction.java
index 79a3a0ac37f..7ceb07e2bf9 100644
-- a/solr/src/java/org/apache/solr/search/function/LinearFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/LinearFloatFunction.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 
 import java.io.IOException;
@@ -46,8 +46,8 @@ public class LinearFloatFunction extends ValueSource {
     return slope + "*float(" + source.description() + ")+" + intercept;
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues vals =  source.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues vals =  source.getValues(context, readerContext);
     return new DocValues() {
       public float floatVal(int doc) {
         return vals.floatVal(doc) * slope + intercept;
diff --git a/solr/src/java/org/apache/solr/search/function/LiteralValueSource.java b/solr/src/java/org/apache/solr/search/function/LiteralValueSource.java
index 0c16a838d8c..64965e74cf7 100644
-- a/solr/src/java/org/apache/solr/search/function/LiteralValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/LiteralValueSource.java
@@ -16,7 +16,7 @@ package org.apache.solr.search.function;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.util.Map;
 import java.io.IOException;
@@ -38,7 +38,7 @@ public class LiteralValueSource extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
 
     return new DocValues() {
       @Override
diff --git a/solr/src/java/org/apache/solr/search/function/LongFieldSource.java b/solr/src/java/org/apache/solr/search/function/LongFieldSource.java
index 60587d229c3..dfa92b7b2c1 100644
-- a/solr/src/java/org/apache/solr/search/function/LongFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/LongFieldSource.java
@@ -18,6 +18,7 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.search.cache.LongValuesCreator;
 import org.apache.lucene.search.cache.CachedArray.LongValues;
@@ -50,8 +51,8 @@ public class LongFieldSource extends NumericFieldCacheSource<LongValues> {
     return Long.parseLong(extVal);
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final LongValues vals = cache.getLongs(reader, field, creator);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final LongValues vals = cache.getLongs(readerContext.reader, field, creator);
     final long[] arr = vals.values;
 	final Bits valid = vals.valid;
     
diff --git a/solr/src/java/org/apache/solr/search/function/MaxDocValueSource.java b/solr/src/java/org/apache/solr/search/function/MaxDocValueSource.java
index 6f4bebcd586..30d94528a93 100755
-- a/solr/src/java/org/apache/solr/search/function/MaxDocValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/MaxDocValueSource.java
@@ -16,7 +16,7 @@
  */
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 
 import java.io.IOException;
@@ -38,7 +38,7 @@ public class MaxDocValueSource extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     IndexSearcher searcher = (IndexSearcher)context.get("searcher");
     return new ConstIntDocValues(searcher.maxDoc(), this);
   }
diff --git a/solr/src/java/org/apache/solr/search/function/MaxFloatFunction.java b/solr/src/java/org/apache/solr/search/function/MaxFloatFunction.java
index bab340f36b3..37af4d94809 100644
-- a/solr/src/java/org/apache/solr/search/function/MaxFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/MaxFloatFunction.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 
 import java.io.IOException;
@@ -45,8 +45,8 @@ public class MaxFloatFunction extends ValueSource {
     return "max(" + source.description() + "," + fval + ")";
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues vals =  source.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues vals =  source.getValues(context, readerContext);
     return new DocValues() {
       public float floatVal(int doc) {
 	float v = vals.floatVal(doc);
diff --git a/solr/src/java/org/apache/solr/search/function/MultiFloatFunction.java b/solr/src/java/org/apache/solr/search/function/MultiFloatFunction.java
index 331cd649095..f66bdfe7502 100644
-- a/solr/src/java/org/apache/solr/search/function/MultiFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/MultiFloatFunction.java
@@ -16,7 +16,7 @@ package org.apache.solr.search.function;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 
 import java.util.Map;
@@ -54,10 +54,10 @@ public abstract class MultiFloatFunction extends ValueSource {
     return sb.toString();
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     final DocValues[] valsArr = new DocValues[sources.length];
     for (int i=0; i<sources.length; i++) {
      valsArr[i] = sources[i].getValues(context, reader);
      valsArr[i] = sources[i].getValues(context, readerContext);
     }
 
     return new DocValues() {
diff --git a/solr/src/java/org/apache/solr/search/function/NormValueSource.java b/solr/src/java/org/apache/solr/search/function/NormValueSource.java
index 913f4670511..004bd14be71 100755
-- a/solr/src/java/org/apache/solr/search/function/NormValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/NormValueSource.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Similarity;
 import java.io.IOException;
@@ -44,10 +44,10 @@ public class NormValueSource extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     IndexSearcher searcher = (IndexSearcher)context.get("searcher");
     final Similarity similarity = searcher.getSimilarity();
    final byte[] norms = reader.norms(field);
    final byte[] norms = readerContext.reader.norms(field);
     if (norms == null) {
       return new ConstDoubleDocValues(0.0, this);
     }
diff --git a/solr/src/java/org/apache/solr/search/function/NumDocsValueSource.java b/solr/src/java/org/apache/solr/search/function/NumDocsValueSource.java
index c23d0968000..6b939d3b733 100755
-- a/solr/src/java/org/apache/solr/search/function/NumDocsValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/NumDocsValueSource.java
@@ -16,8 +16,8 @@
  */
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.solr.search.SolrIndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.util.ReaderUtil;
 
 import java.io.IOException;
 import java.util.Map;
@@ -33,11 +33,9 @@ public class NumDocsValueSource extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     // Searcher has no numdocs so we must use the reader instead
    SolrIndexReader topReader = (SolrIndexReader)reader;
    while (topReader.getParent() != null) topReader = topReader.getParent();
    return new ConstIntDocValues(topReader.numDocs(), this);
    return new ConstIntDocValues(ReaderUtil.getTopLevelContext(readerContext).reader.numDocs(), this);
   }
 
   @Override
diff --git a/solr/src/java/org/apache/solr/search/function/OrdFieldSource.java b/solr/src/java/org/apache/solr/search/function/OrdFieldSource.java
index d68185664cb..7c249571c48 100644
-- a/solr/src/java/org/apache/solr/search/function/OrdFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/OrdFieldSource.java
@@ -18,10 +18,11 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldCache;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.solr.search.MutableValue;
 import org.apache.solr.search.MutableValueInt;
import org.apache.solr.search.SolrIndexReader;
 
 import java.io.IOException;
 import java.util.Map;
@@ -56,21 +57,10 @@ public class OrdFieldSource extends ValueSource {
   }
 
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    int offset = 0;
    IndexReader topReader = reader;
    if (topReader instanceof SolrIndexReader) {
      SolrIndexReader r = (SolrIndexReader)topReader;
      while (r.getParent() != null) {
        offset += r.getBase();
        r = r.getParent();
      }
      topReader = r;
    }
    final int off = offset;

  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final int off = readerContext.docBase;
    final IndexReader topReader = ReaderUtil.getTopLevelContext(readerContext).reader;
     final FieldCache.DocTermsIndex sindex = FieldCache.DEFAULT.getTermsIndex(topReader, field);

     return new DocValues() {
       protected String toTerm(String readableValue) {
         return readableValue;
diff --git a/solr/src/java/org/apache/solr/search/function/QueryValueSource.java b/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
index b3b0c66b44d..f2407ab9b8c 100755
-- a/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
@@ -18,7 +18,9 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.*;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.solr.common.SolrException;
 
 import java.io.IOException;
@@ -44,8 +46,8 @@ public class QueryValueSource extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map fcontext, IndexReader reader) throws IOException {
    return new QueryDocValues(reader, q, defVal, fcontext);
  public DocValues getValues(Map fcontext, AtomicReaderContext readerContext) throws IOException {
    return new QueryDocValues(readerContext, q, defVal, fcontext);
   }
 
   public int hashCode() {
@@ -68,7 +70,8 @@ public class QueryValueSource extends ValueSource {
 
 class QueryDocValues extends DocValues {
   final Query q;
  final IndexReader reader;
//  final IndexReader reader;
  final AtomicReaderContext readerContext;
   final Weight weight;
   final float defVal;
   final Map fcontext;
@@ -79,21 +82,28 @@ class QueryDocValues extends DocValues {
   // the last document requested... start off with high value
   // to trigger a scorer reset on first access.
   int lastDocRequested=Integer.MAX_VALUE;
  
 
  public QueryDocValues(IndexReader reader, Query q, float defVal, Map fcontext) throws IOException {
    this.reader = reader;
  public QueryDocValues(AtomicReaderContext readerContext, Query q, float defVal, Map fcontext) throws IOException {
    IndexReader reader = readerContext.reader;
    this.readerContext = readerContext;
     this.q = q;
     this.defVal = defVal;
     this.fcontext = fcontext;
 
     Weight w = fcontext==null ? null : (Weight)fcontext.get(q);
    // TODO: sort by function doesn't weight (SOLR-1297 is open because of this bug)... so weightSearcher will currently be null
     if (w == null) {
       IndexSearcher weightSearcher = fcontext == null ? new IndexSearcher(reader) : (IndexSearcher)fcontext.get("searcher");

       // TODO: sort by function doesn't weight (SOLR-1297 is open because of this bug)... so weightSearcher will currently be null
       if (weightSearcher == null) weightSearcher = new IndexSearcher(reader);

       w = q.weight(weightSearcher);
      IndexSearcher weightSearcher;
      if(fcontext == null) {
        weightSearcher = new IndexSearcher(ReaderUtil.getTopLevelContext(readerContext), readerContext);
      } else {
        weightSearcher = (IndexSearcher)fcontext.get("searcher");
        if (weightSearcher == null) {
          weightSearcher = new IndexSearcher(ReaderUtil.getTopLevelContext(readerContext), readerContext);
        }
      }
      w = q.weight(weightSearcher);
     }
     weight = w;
   }
@@ -101,15 +111,7 @@ class QueryDocValues extends DocValues {
   public float floatVal(int doc) {
     try {
       if (doc < lastDocRequested) {
        // out-of-order access.... reset scorer.
        IndexReader.AtomicReaderContext ctx = ValueSource.readerToContext(fcontext, reader);

        if (ctx == null) {
          // TODO: this is because SOLR-1297 does not weight
          ctx = (IndexReader.AtomicReaderContext)reader.getTopReaderContext();  // this is the incorrect context
        }

        scorer = weight.scorer(ctx, true, false);
        scorer = weight.scorer(readerContext, true, false);
         if (scorer==null) return defVal;
         scorerDoc = -1;
       }
diff --git a/solr/src/java/org/apache/solr/search/function/RangeMapFloatFunction.java b/solr/src/java/org/apache/solr/search/function/RangeMapFloatFunction.java
index 32544e5048f..83bedeab8af 100755
-- a/solr/src/java/org/apache/solr/search/function/RangeMapFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/RangeMapFloatFunction.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 
 import java.io.IOException;
@@ -50,8 +50,8 @@ public class RangeMapFloatFunction extends ValueSource {
     return "map(" + source.description() + "," + min + "," + max + "," + target + ")";
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues vals =  source.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues vals =  source.getValues(context, readerContext);
     return new DocValues() {
       public float floatVal(int doc) {
         float val = vals.floatVal(doc);
diff --git a/solr/src/java/org/apache/solr/search/function/ReciprocalFloatFunction.java b/solr/src/java/org/apache/solr/search/function/ReciprocalFloatFunction.java
index fcfa8324c22..99627759db8 100644
-- a/solr/src/java/org/apache/solr/search/function/ReciprocalFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/ReciprocalFloatFunction.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 
 import java.io.IOException;
@@ -57,8 +57,8 @@ public class ReciprocalFloatFunction extends ValueSource {
     this.b=b;
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues vals = source.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues vals = source.getValues(context, readerContext);
     return new DocValues() {
       public float floatVal(int doc) {
         return a/(m*vals.floatVal(doc) + b);
diff --git a/solr/src/java/org/apache/solr/search/function/ReverseOrdFieldSource.java b/solr/src/java/org/apache/solr/search/function/ReverseOrdFieldSource.java
index 455fc3ab0f8..639bdbba72b 100644
-- a/solr/src/java/org/apache/solr/search/function/ReverseOrdFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/ReverseOrdFieldSource.java
@@ -18,8 +18,9 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldCache;
import org.apache.solr.search.SolrIndexReader;
import org.apache.lucene.util.ReaderUtil;
 
 import java.io.IOException;
 import java.util.Map;
@@ -56,18 +57,9 @@ public class ReverseOrdFieldSource extends ValueSource {
     return "rord("+field+')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    int offset = 0;
    IndexReader topReader = reader;
    if (topReader instanceof SolrIndexReader) {
      SolrIndexReader r = (SolrIndexReader)topReader;
      while (r.getParent() != null) {
        offset += r.getBase();
        r = r.getParent();
      }
      topReader = r;
    }
    final int off = offset;
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final IndexReader topReader = ReaderUtil.getTopLevelContext(readerContext).reader;
    final int off = readerContext.docBase;
 
     final FieldCache.DocTermsIndex sindex = FieldCache.DEFAULT.getTermsIndex(topReader, field);
     final int end = sindex.numOrd();
diff --git a/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java b/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java
index da746d6a2a0..40e3192325c 100755
-- a/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java
@@ -17,7 +17,6 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.util.ReaderUtil;
@@ -56,17 +55,15 @@ public class ScaleFloatFunction extends ValueSource {
     float maxVal;
   }
 
  private ScaleInfo createScaleInfo(Map context, IndexReader reader) throws IOException {
    IndexReader.ReaderContext ctx = ValueSource.readerToContext(context, reader);
    while (ctx.parent != null) ctx = ctx.parent;
    final AtomicReaderContext[] leaves = ReaderUtil.leaves(ctx);
  private ScaleInfo createScaleInfo(Map context, AtomicReaderContext readerContext) throws IOException {
    final AtomicReaderContext[] leaves = ReaderUtil.leaves(ReaderUtil.getTopLevelContext(readerContext));
 
     float minVal = Float.POSITIVE_INFINITY;
     float maxVal = Float.NEGATIVE_INFINITY;
 
     for (AtomicReaderContext leaf : leaves) {
       int maxDoc = leaf.reader.maxDoc();
      DocValues vals =  source.getValues(context, leaf.reader);
      DocValues vals =  source.getValues(context, leaf);
       for (int i=0; i<maxDoc; i++) {
 
       float val = vals.floatVal(i);
@@ -96,18 +93,18 @@ public class ScaleFloatFunction extends ValueSource {
     return scaleInfo;
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
 
     ScaleInfo scaleInfo = (ScaleInfo)context.get(source);
     if (scaleInfo == null) {
      scaleInfo = createScaleInfo(context, reader);
      scaleInfo = createScaleInfo(context, readerContext);
     }
 
     final float scale = (scaleInfo.maxVal-scaleInfo.minVal==0) ? 0 : (max-min)/(scaleInfo.maxVal-scaleInfo.minVal);
     final float minSource = scaleInfo.minVal;
     final float maxSource = scaleInfo.maxVal;
 
    final DocValues vals =  source.getValues(context, reader);
    final DocValues vals =  source.getValues(context, readerContext);
 
     return new DocValues() {
       public float floatVal(int doc) {
diff --git a/solr/src/java/org/apache/solr/search/function/ShortFieldSource.java b/solr/src/java/org/apache/solr/search/function/ShortFieldSource.java
index cb146d84dcb..fb4e41b5562 100644
-- a/solr/src/java/org/apache/solr/search/function/ShortFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/ShortFieldSource.java
@@ -18,7 +18,7 @@ package org.apache.solr.search.function;
 
 import org.apache.lucene.search.cache.ShortValuesCreator;
 import org.apache.lucene.search.cache.CachedArray.ShortValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.io.IOException;
 import java.util.Map;
@@ -39,8 +39,8 @@ public class ShortFieldSource extends NumericFieldCacheSource<ShortValues> {
     return "short(" + field + ')';
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final ShortValues vals = cache.getShorts(reader, field, creator);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final ShortValues vals = cache.getShorts(readerContext.reader, field, creator);
     final short[] arr = vals.values;
     
     return new DocValues() {
diff --git a/solr/src/java/org/apache/solr/search/function/SimpleFloatFunction.java b/solr/src/java/org/apache/solr/search/function/SimpleFloatFunction.java
index 4e82f005310..e4079ac2dbd 100755
-- a/solr/src/java/org/apache/solr/search/function/SimpleFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/SimpleFloatFunction.java
@@ -17,7 +17,7 @@
 
 package org.apache.solr.search.function;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.io.IOException;
 import java.util.Map;
@@ -32,8 +32,8 @@ import java.util.Map;
   protected abstract float func(int doc, DocValues vals);
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues vals =  source.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues vals =  source.getValues(context, readerContext);
     return new DocValues() {
       public float floatVal(int doc) {
 	return func(doc, vals);
diff --git a/solr/src/java/org/apache/solr/search/function/StringIndexDocValues.java b/solr/src/java/org/apache/solr/search/function/StringIndexDocValues.java
index 66e88419a38..f533a7319fc 100755
-- a/solr/src/java/org/apache/solr/search/function/StringIndexDocValues.java
++ b/solr/src/java/org/apache/solr/search/function/StringIndexDocValues.java
@@ -19,6 +19,7 @@ package org.apache.solr.search.function;
 
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.search.MutableValue;
 import org.apache.solr.search.MutableValueStr;
@@ -33,9 +34,9 @@ public abstract class StringIndexDocValues extends DocValues {
     protected final ValueSource vs;
     protected final MutableValueStr val = new MutableValueStr();
 
    public StringIndexDocValues(ValueSource vs, IndexReader reader, String field) throws IOException {
    public StringIndexDocValues(ValueSource vs, AtomicReaderContext context, String field) throws IOException {
       try {
        termsIndex = FieldCache.DEFAULT.getTermsIndex(reader, field);
        termsIndex = FieldCache.DEFAULT.getTermsIndex(context.reader, field);
       } catch (RuntimeException e) {
         throw new StringIndexException(field, e);
       }
diff --git a/solr/src/java/org/apache/solr/search/function/TFValueSource.java b/solr/src/java/org/apache/solr/search/function/TFValueSource.java
index fa82de53a25..cffb2264c59 100755
-- a/solr/src/java/org/apache/solr/search/function/TFValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/TFValueSource.java
@@ -1,6 +1,7 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Similarity;
@@ -21,9 +22,8 @@ public class TFValueSource extends TermFreqValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    // use MultiFields, just in case someone did a top() function
    Fields fields = MultiFields.getFields(reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    Fields fields = readerContext.reader.fields();
     final Terms terms = fields.terms(field);
     final Similarity similarity = ((IndexSearcher)context.get("searcher")).getSimilarity();
 
diff --git a/solr/src/java/org/apache/solr/search/function/TermFreqValueSource.java b/solr/src/java/org/apache/solr/search/function/TermFreqValueSource.java
index a5603fd451b..5f8e1432ce0 100755
-- a/solr/src/java/org/apache/solr/search/function/TermFreqValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/TermFreqValueSource.java
@@ -18,6 +18,7 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.common.SolrException;
@@ -36,9 +37,8 @@ public class TermFreqValueSource extends DocFreqValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    // use MultiFields, just in case someone did a top() function
    Fields fields = MultiFields.getFields(reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    Fields fields = readerContext.reader.fields();
     final Terms terms = fields.terms(field);
 
     return new IntDocValues(this) {
diff --git a/solr/src/java/org/apache/solr/search/function/ValueSource.java b/solr/src/java/org/apache/solr/search/function/ValueSource.java
index 9c1ac9724c3..e43ef276f1d 100644
-- a/solr/src/java/org/apache/solr/search/function/ValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/ValueSource.java
@@ -19,7 +19,6 @@ package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.FieldComparator;
 import org.apache.lucene.search.FieldComparatorSource;
 import org.apache.lucene.search.Scorer;
@@ -47,7 +46,7 @@ public abstract class ValueSource implements Serializable {
    * Gets the values for this reader and the context that was previously
    * passed to createWeight()
    */
  public abstract DocValues getValues(Map context, IndexReader reader) throws IOException;
  public abstract DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException;
 
   public abstract boolean equals(Object o);
 
@@ -66,7 +65,7 @@ public abstract class ValueSource implements Serializable {
    * EXPERIMENTAL: This method is subject to change.
    * <br>WARNING: Sorted function queries are not currently weighted.
    * <p>
   * Get the SortField for this ValueSource.  Uses the {@link #getValues(java.util.Map, org.apache.lucene.index.IndexReader)}
   * Get the SortField for this ValueSource.  Uses the {@link #getValues(java.util.Map, AtomicReaderContext)}
    * to populate the SortField.
    * 
    * @param reverse true if this is a reverse sort.
@@ -98,40 +97,6 @@ public abstract class ValueSource implements Serializable {
     return context;
   }
 
  /* @lucene.internal
   * This will most likely go away in the future.
   */
  public static AtomicReaderContext readerToContext(Map fcontext, IndexReader reader) {
    Object v = fcontext.get(reader);
    if (v == null) {
      IndexSearcher searcher = (IndexSearcher)fcontext.get("searcher");
      if (searcher == null) {
        return null;
        // TODO
        // throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "no searcher found in function context");
      }
      ReaderContext rcontext = searcher.getIndexReader().getTopReaderContext();
      if (rcontext.isAtomic) {
        assert rcontext.reader == reader;
        fcontext.put(rcontext.reader, (AtomicReaderContext)rcontext);
      } else {
        for (AtomicReaderContext subCtx : rcontext.leaves()) {
          fcontext.put(subCtx.reader, subCtx);
        }
      }

      v = fcontext.get(reader);
      if (v == null) {
        return null;
        // TODO
        // throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "reader " + reader + " is not from the top reader " + searcher.getIndexReader());
      }
    }

    return (AtomicReaderContext)v;
  }


   class ValueSourceComparatorSource extends FieldComparatorSource {
 
 
@@ -188,7 +153,7 @@ public abstract class ValueSource implements Serializable {
     }
 
     public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      docVals = getValues(Collections.emptyMap(), context.reader);
      docVals = getValues(Collections.emptyMap(), context);
       return this;
     }
 
diff --git a/solr/src/java/org/apache/solr/search/function/ValueSourceRangeFilter.java b/solr/src/java/org/apache/solr/search/function/ValueSourceRangeFilter.java
index 8813736a849..151b3878111 100755
-- a/solr/src/java/org/apache/solr/search/function/ValueSourceRangeFilter.java
++ b/solr/src/java/org/apache/solr/search/function/ValueSourceRangeFilter.java
@@ -20,7 +20,7 @@ package org.apache.solr.search.function;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.solr.search.SolrFilter;
 
 import java.io.IOException;
@@ -49,10 +49,10 @@ public class ValueSourceRangeFilter extends SolrFilter {
     this.includeUpper = upperVal != null && includeUpper;
   }
 
  public DocIdSet getDocIdSet(final Map context, final ReaderContext readerContext) throws IOException {
  public DocIdSet getDocIdSet(final Map context, final AtomicReaderContext readerContext) throws IOException {
      return new DocIdSet() {
        public DocIdSetIterator iterator() throws IOException {
         return valueSource.getValues(context, readerContext.reader).getRangeScorer(readerContext.reader, lowerVal, upperVal, includeLower, includeUpper);
         return valueSource.getValues(context, readerContext).getRangeScorer(readerContext.reader, lowerVal, upperVal, includeLower, includeUpper);
        }
      };
   }
diff --git a/solr/src/java/org/apache/solr/search/function/VectorValueSource.java b/solr/src/java/org/apache/solr/search/function/VectorValueSource.java
index 5947df8041e..523e54670a3 100644
-- a/solr/src/java/org/apache/solr/search/function/VectorValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/VectorValueSource.java
@@ -16,7 +16,7 @@ package org.apache.solr.search.function;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.solr.search.function.MultiValueSource;
 import org.apache.solr.search.function.DocValues;
@@ -53,13 +53,13 @@ public class VectorValueSource extends MultiValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     int size = sources.size();
 
     // special-case x,y and lat,lon since it's so common
     if (size==2) {
      final DocValues x = sources.get(0).getValues(context, reader);
      final DocValues y = sources.get(1).getValues(context, reader);
      final DocValues x = sources.get(0).getValues(context, readerContext);
      final DocValues y = sources.get(1).getValues(context, readerContext);
       return new DocValues() {
         @Override
         public void byteVal(int doc, byte[] vals) {
@@ -106,7 +106,7 @@ public class VectorValueSource extends MultiValueSource {
 
     final DocValues[] valsArr = new DocValues[size];
     for (int i = 0; i < size; i++) {
      valsArr[i] = sources.get(i).getValues(context, reader);
      valsArr[i] = sources.get(i).getValues(context, readerContext);
     }
 
     return new DocValues() {
diff --git a/solr/src/java/org/apache/solr/search/function/distance/GeohashFunction.java b/solr/src/java/org/apache/solr/search/function/distance/GeohashFunction.java
index 6102356b662..df146f7cb72 100644
-- a/solr/src/java/org/apache/solr/search/function/distance/GeohashFunction.java
++ b/solr/src/java/org/apache/solr/search/function/distance/GeohashFunction.java
@@ -18,7 +18,7 @@ package org.apache.solr.search.function.distance;
 
 import org.apache.solr.search.function.ValueSource;
 import org.apache.solr.search.function.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.spatial.geohash.GeoHashUtils;
 
 import java.util.Map;
@@ -46,9 +46,9 @@ public class GeohashFunction extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues latDV = lat.getValues(context, reader);
    final DocValues lonDV = lon.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues latDV = lat.getValues(context, readerContext);
    final DocValues lonDV = lon.getValues(context, readerContext);
 
 
     return new DocValues() {
diff --git a/solr/src/java/org/apache/solr/search/function/distance/GeohashHaversineFunction.java b/solr/src/java/org/apache/solr/search/function/distance/GeohashHaversineFunction.java
index cdcc182d13f..728b528e578 100644
-- a/solr/src/java/org/apache/solr/search/function/distance/GeohashHaversineFunction.java
++ b/solr/src/java/org/apache/solr/search/function/distance/GeohashHaversineFunction.java
@@ -20,7 +20,7 @@ package org.apache.solr.search.function.distance;
 import org.apache.lucene.spatial.DistanceUtils;
 import org.apache.solr.search.function.ValueSource;
 import org.apache.solr.search.function.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.spatial.geohash.GeoHashUtils;
 
@@ -54,9 +54,9 @@ public class GeohashHaversineFunction extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues gh1DV = geoHash1.getValues(context, reader);
    final DocValues gh2DV = geoHash2.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues gh1DV = geoHash1.getValues(context, readerContext);
    final DocValues gh2DV = geoHash2.getValues(context, readerContext);
 
     return new DocValues() {
       public float floatVal(int doc) {
diff --git a/solr/src/java/org/apache/solr/search/function/distance/HaversineConstFunction.java b/solr/src/java/org/apache/solr/search/function/distance/HaversineConstFunction.java
index b1796b2646a..8ee45f57064 100755
-- a/solr/src/java/org/apache/solr/search/function/distance/HaversineConstFunction.java
++ b/solr/src/java/org/apache/solr/search/function/distance/HaversineConstFunction.java
@@ -16,7 +16,7 @@ package org.apache.solr.search.function.distance;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.queryParser.ParseException;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.spatial.DistanceUtils;
@@ -190,9 +190,9 @@ public class HaversineConstFunction extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues latVals = latSource.getValues(context, reader);
    final DocValues lonVals = lonSource.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues latVals = latSource.getValues(context, readerContext);
    final DocValues lonVals = lonSource.getValues(context, readerContext);
     final double latCenterRad = this.latCenter * DistanceUtils.DEGREES_TO_RADIANS;
     final double lonCenterRad = this.lonCenter * DistanceUtils.DEGREES_TO_RADIANS;
     final double latCenterRad_cos = this.latCenterRad_cos;
diff --git a/solr/src/java/org/apache/solr/search/function/distance/HaversineFunction.java b/solr/src/java/org/apache/solr/search/function/distance/HaversineFunction.java
index 673840622c0..f54c3aa9ba4 100644
-- a/solr/src/java/org/apache/solr/search/function/distance/HaversineFunction.java
++ b/solr/src/java/org/apache/solr/search/function/distance/HaversineFunction.java
@@ -16,7 +16,7 @@ package org.apache.solr.search.function.distance;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.spatial.DistanceUtils;
 import org.apache.solr.common.SolrException;
@@ -95,10 +95,10 @@ public class HaversineFunction extends ValueSource {
 
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues vals1 = p1.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues vals1 = p1.getValues(context, readerContext);
 
    final DocValues vals2 = p2.getValues(context, reader);
    final DocValues vals2 = p2.getValues(context, readerContext);
     return new DocValues() {
       public float floatVal(int doc) {
         return (float) doubleVal(doc);
diff --git a/solr/src/java/org/apache/solr/search/function/distance/StringDistanceFunction.java b/solr/src/java/org/apache/solr/search/function/distance/StringDistanceFunction.java
index ecdcb4d75ba..f67639c9d28 100644
-- a/solr/src/java/org/apache/solr/search/function/distance/StringDistanceFunction.java
++ b/solr/src/java/org/apache/solr/search/function/distance/StringDistanceFunction.java
@@ -1,6 +1,6 @@
 package org.apache.solr.search.function.distance;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.spell.StringDistance;
 import org.apache.solr.search.function.DocValues;
 import org.apache.solr.search.function.ValueSource;
@@ -31,9 +31,9 @@ public class StringDistanceFunction extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues str1DV = str1.getValues(context, reader);
    final DocValues str2DV = str2.getValues(context, reader);
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final DocValues str1DV = str1.getValues(context, readerContext);
    final DocValues str2DV = str2.getValues(context, readerContext);
     return new DocValues() {
 
       public float floatVal(int doc) {
diff --git a/solr/src/java/org/apache/solr/search/function/distance/VectorDistanceFunction.java b/solr/src/java/org/apache/solr/search/function/distance/VectorDistanceFunction.java
index 95495bd3f8e..3b86177bc04 100644
-- a/solr/src/java/org/apache/solr/search/function/distance/VectorDistanceFunction.java
++ b/solr/src/java/org/apache/solr/search/function/distance/VectorDistanceFunction.java
@@ -16,7 +16,7 @@ package org.apache.solr.search.function.distance;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.spatial.DistanceUtils;
 import org.apache.solr.common.SolrException;
@@ -78,11 +78,11 @@ public class VectorDistanceFunction extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
  public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
 
    final DocValues vals1 = source1.getValues(context, reader);
    final DocValues vals1 = source1.getValues(context, readerContext);
 
    final DocValues vals2 = source2.getValues(context, reader);
    final DocValues vals2 = source2.getValues(context, readerContext);
 
 
     return new DocValues() {
diff --git a/solr/src/java/org/apache/solr/spelling/IndexBasedSpellChecker.java b/solr/src/java/org/apache/solr/spelling/IndexBasedSpellChecker.java
index 3f85b259baf..bac6c8cbb47 100644
-- a/solr/src/java/org/apache/solr/spelling/IndexBasedSpellChecker.java
++ b/solr/src/java/org/apache/solr/spelling/IndexBasedSpellChecker.java
@@ -73,7 +73,7 @@ public class IndexBasedSpellChecker extends AbstractLuceneSpellChecker {
     try {
       if (sourceLocation == null) {
         // Load from Solr's index
        reader = searcher.getReader();
        reader = searcher.getIndexReader();
       } else {
         // Load from Lucene index at given sourceLocation
         reader = this.reader;
diff --git a/solr/src/java/org/apache/solr/spelling/suggest/Suggester.java b/solr/src/java/org/apache/solr/spelling/suggest/Suggester.java
index 8ba1077fb3f..0eeb3141792 100644
-- a/solr/src/java/org/apache/solr/spelling/suggest/Suggester.java
++ b/solr/src/java/org/apache/solr/spelling/suggest/Suggester.java
@@ -100,7 +100,7 @@ public class Suggester extends SolrSpellChecker {
   public void build(SolrCore core, SolrIndexSearcher searcher) {
     LOG.info("build()");
     if (sourceLocation == null) {
      reader = searcher.getReader();
      reader = searcher.getIndexReader();
       dictionary = new HighFrequencyDictionary(reader, field, threshold);
     } else {
       try {
diff --git a/solr/src/java/org/apache/solr/update/UpdateHandler.java b/solr/src/java/org/apache/solr/update/UpdateHandler.java
index 7e06a2739fe..e7332349dfd 100644
-- a/solr/src/java/org/apache/solr/update/UpdateHandler.java
++ b/solr/src/java/org/apache/solr/update/UpdateHandler.java
@@ -152,7 +152,7 @@ public abstract class UpdateHandler implements SolrInfoMBean {
     @Override
     public void collect(int doc) {
       try {
        searcher.getReader().deleteDocument(doc + docBase);
        searcher.getIndexReader().deleteDocument(doc + docBase);
         deleted++;
       } catch (IOException e) {
         // don't try to close the searcher on failure for now...
diff --git a/solr/src/test/org/apache/solr/core/TestConfig.java b/solr/src/test/org/apache/solr/core/TestConfig.java
index 03676c881e6..8c4145a610d 100644
-- a/solr/src/test/org/apache/solr/core/TestConfig.java
++ b/solr/src/test/org/apache/solr/core/TestConfig.java
@@ -139,7 +139,7 @@ public class TestConfig extends SolrTestCaseJ4 {
     StandardIndexReaderFactory sirf = (StandardIndexReaderFactory) irf;
     assertEquals(12, sirf.termInfosIndexDivisor);
     SolrQueryRequest req = req();
    assertEquals(12, req.getSearcher().getReader().getTermInfosIndexDivisor());
    assertEquals(12, req.getSearcher().getIndexReader().getTermInfosIndexDivisor());
     req.close();
   }
 
diff --git a/solr/src/test/org/apache/solr/core/TestQuerySenderListener.java b/solr/src/test/org/apache/solr/core/TestQuerySenderListener.java
index b8edad8e255..70a7501c6e8 100644
-- a/solr/src/test/org/apache/solr/core/TestQuerySenderListener.java
++ b/solr/src/test/org/apache/solr/core/TestQuerySenderListener.java
@@ -75,7 +75,7 @@ public class TestQuerySenderListener extends SolrTestCaseJ4 {
     String evt = mock.req.getParams().get(EventParams.EVENT);
     assertNotNull("Event is null", evt);
     assertTrue(evt + " is not equal to " + EventParams.FIRST_SEARCHER, evt.equals(EventParams.FIRST_SEARCHER) == true);
    Directory dir = currentSearcher.getReader().directory();
    Directory dir = currentSearcher.getIndexReader().directory();
     SolrIndexSearcher newSearcher = new SolrIndexSearcher(core, core.getSchema(), "testQuerySenderListener", dir, true, false);
 
     qsl.newSearcher(newSearcher, currentSearcher);
diff --git a/solr/src/test/org/apache/solr/handler/component/QueryElevationComponentTest.java b/solr/src/test/org/apache/solr/handler/component/QueryElevationComponentTest.java
index b165298fa7d..dac3e913562 100644
-- a/solr/src/test/org/apache/solr/handler/component/QueryElevationComponentTest.java
++ b/solr/src/test/org/apache/solr/handler/component/QueryElevationComponentTest.java
@@ -71,7 +71,7 @@ public class QueryElevationComponentTest extends SolrTestCaseJ4 {
     comp.inform( core );
 
     SolrQueryRequest req = req();
    IndexReader reader = req.getSearcher().getReader();
    IndexReader reader = req.getSearcher().getIndexReader();
     Map<String, ElevationObj> map = comp.getElevationMap( reader, core );
     req.close();
 
@@ -130,7 +130,7 @@ public class QueryElevationComponentTest extends SolrTestCaseJ4 {
     args.put( "indent", "true" );
     //args.put( CommonParams.FL, "id,title,score" );
     SolrQueryRequest req = new LocalSolrQueryRequest( h.getCore(), new MapSolrParams( args) );
    IndexReader reader = req.getSearcher().getReader();
    IndexReader reader = req.getSearcher().getIndexReader();
     QueryElevationComponent booster = (QueryElevationComponent)req.getCore().getSearchComponent( "elevate" );
 
     assertQ("Make sure standard sort works as expected", req
@@ -255,7 +255,7 @@ public class QueryElevationComponentTest extends SolrTestCaseJ4 {
     comp.inform( h.getCore() );
 
     SolrQueryRequest req = req();
    IndexReader reader = req.getSearcher().getReader();
    IndexReader reader = req.getSearcher().getIndexReader();
     Map<String, ElevationObj> map = comp.getElevationMap(reader, h.getCore());
     assertTrue( map.get( "aaa" ).priority.containsKey( new BytesRef("A") ) );
     assertNull( map.get( "bbb" ) );
@@ -267,7 +267,7 @@ public class QueryElevationComponentTest extends SolrTestCaseJ4 {
     assertU(commit());
 
     req = req();
    reader = req.getSearcher().getReader();
    reader = req.getSearcher().getIndexReader();
     map = comp.getElevationMap(reader, h.getCore());
     assertNull( map.get( "aaa" ) );
     assertTrue( map.get( "bbb" ).priority.containsKey( new BytesRef("B") ) );
diff --git a/solr/src/test/org/apache/solr/request/TestFaceting.java b/solr/src/test/org/apache/solr/request/TestFaceting.java
index fa5b6cdd1e0..b9e1a5f8a9e 100755
-- a/solr/src/test/org/apache/solr/request/TestFaceting.java
++ b/solr/src/test/org/apache/solr/request/TestFaceting.java
@@ -67,14 +67,14 @@ public class TestFaceting extends SolrTestCaseJ4 {
     req = lrf.makeRequest("q","*:*");
 
     TermIndex ti = new TermIndex(proto.field());
    NumberedTermsEnum te = ti.getEnumerator(req.getSearcher().getReader());
    NumberedTermsEnum te = ti.getEnumerator(req.getSearcher().getIndexReader());
 
     // iterate through first
     while(te.term() != null) te.next();
     assertEquals(size, te.getTermNumber());
     te.close();
 
    te = ti.getEnumerator(req.getSearcher().getReader());
    te = ti.getEnumerator(req.getSearcher().getIndexReader());
 
     Random r = new Random(size);
     // test seeking by term string
diff --git a/solr/src/test/org/apache/solr/search/TestIndexSearcher.java b/solr/src/test/org/apache/solr/search/TestIndexSearcher.java
index 0b5e114c257..4ad292607f9 100755
-- a/solr/src/test/org/apache/solr/search/TestIndexSearcher.java
++ b/solr/src/test/org/apache/solr/search/TestIndexSearcher.java
@@ -45,7 +45,7 @@ public class TestIndexSearcher extends SolrTestCaseJ4 {
     AtomicReaderContext[] leaves = ReaderUtil.leaves(topReaderContext);
     int idx = ReaderUtil.subIndex(doc, leaves);
     AtomicReaderContext leaf = leaves[idx];
    DocValues vals = vs.getValues(context, leaf.reader);
    DocValues vals = vs.getValues(context, leaf);
     return vals.strVal(doc-leaf.docBase);
   }
 
diff --git a/solr/src/test/org/apache/solr/spelling/DirectSolrSpellCheckerTest.java b/solr/src/test/org/apache/solr/spelling/DirectSolrSpellCheckerTest.java
index 41b64532a8b..7e93afb8358 100644
-- a/solr/src/test/org/apache/solr/spelling/DirectSolrSpellCheckerTest.java
++ b/solr/src/test/org/apache/solr/spelling/DirectSolrSpellCheckerTest.java
@@ -62,7 +62,7 @@ public class DirectSolrSpellCheckerTest extends SolrTestCaseJ4 {
 
     RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
     Collection<Token> tokens = queryConverter.convert("fob");
    SpellingOptions spellOpts = new SpellingOptions(tokens, searcher.get().getReader());
    SpellingOptions spellOpts = new SpellingOptions(tokens, searcher.get().getIndexReader());
     SpellingResult result = checker.getSuggestions(spellOpts);
     assertTrue("result is null and it shouldn't be", result != null);
     Map<String, Integer> suggestions = result.get(tokens.iterator().next());
diff --git a/solr/src/test/org/apache/solr/spelling/FileBasedSpellCheckerTest.java b/solr/src/test/org/apache/solr/spelling/FileBasedSpellCheckerTest.java
index 1230e78aa0b..3c536058f1f 100644
-- a/solr/src/test/org/apache/solr/spelling/FileBasedSpellCheckerTest.java
++ b/solr/src/test/org/apache/solr/spelling/FileBasedSpellCheckerTest.java
@@ -78,7 +78,7 @@ public class FileBasedSpellCheckerTest extends SolrTestCaseJ4 {
 
     RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
     Collection<Token> tokens = queryConverter.convert("fob");
    SpellingOptions spellOpts = new SpellingOptions(tokens, searcher.get().getReader());
    SpellingOptions spellOpts = new SpellingOptions(tokens, searcher.get().getIndexReader());
     SpellingResult result = checker.getSuggestions(spellOpts);
     assertTrue("result is null and it shouldn't be", result != null);
     Map<String, Integer> suggestions = result.get(tokens.iterator().next());
@@ -117,7 +117,7 @@ public class FileBasedSpellCheckerTest extends SolrTestCaseJ4 {
     RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
     Collection<Token> tokens = queryConverter.convert("Solar");
 
    SpellingOptions spellOpts = new SpellingOptions(tokens, searcher.get().getReader());
    SpellingOptions spellOpts = new SpellingOptions(tokens, searcher.get().getIndexReader());
     SpellingResult result = checker.getSuggestions(spellOpts);
     assertTrue("result is null and it shouldn't be", result != null);
     //should be lowercased, b/c we are using a lowercasing analyzer
@@ -160,7 +160,7 @@ public class FileBasedSpellCheckerTest extends SolrTestCaseJ4 {
 
     RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
     Collection<Token> tokens = queryConverter.convert("solar");
    SpellingOptions spellOpts = new SpellingOptions(tokens, searcher.get().getReader());
    SpellingOptions spellOpts = new SpellingOptions(tokens, searcher.get().getIndexReader());
     SpellingResult result = checker.getSuggestions(spellOpts);
     assertTrue("result is null and it shouldn't be", result != null);
     //should be lowercased, b/c we are using a lowercasing analyzer
diff --git a/solr/src/test/org/apache/solr/spelling/IndexBasedSpellCheckerTest.java b/solr/src/test/org/apache/solr/spelling/IndexBasedSpellCheckerTest.java
index d7f8edfb95b..440142c667b 100644
-- a/solr/src/test/org/apache/solr/spelling/IndexBasedSpellCheckerTest.java
++ b/solr/src/test/org/apache/solr/spelling/IndexBasedSpellCheckerTest.java
@@ -121,7 +121,7 @@ public class IndexBasedSpellCheckerTest extends SolrTestCaseJ4 {
     try {
     checker.build(core, searcher);
 
    IndexReader reader = searcher.getReader();
    IndexReader reader = searcher.getIndexReader();
     Collection<Token> tokens = queryConverter.convert("documemt");
     SpellingOptions spellOpts = new SpellingOptions(tokens, reader);
     SpellingResult result = checker.getSuggestions(spellOpts);
@@ -196,7 +196,7 @@ public class IndexBasedSpellCheckerTest extends SolrTestCaseJ4 {
     try {
     checker.build(core, searcher);
 
    IndexReader reader = searcher.getReader();
    IndexReader reader = searcher.getIndexReader();
     Collection<Token> tokens = queryConverter.convert("documemt");
     SpellingOptions spellOpts = new SpellingOptions(tokens, reader, 1, false, true, 0.5f, null);
     SpellingResult result = checker.getSuggestions(spellOpts);
@@ -309,7 +309,7 @@ public class IndexBasedSpellCheckerTest extends SolrTestCaseJ4 {
     try {
     checker.build(core, searcher);
 
    IndexReader reader = searcher.getReader();
    IndexReader reader = searcher.getIndexReader();
     Collection<Token> tokens = queryConverter.convert("flesh");
     SpellingOptions spellOpts = new SpellingOptions(tokens, reader, 1, false, true, 0.5f, null);
     SpellingResult result = checker.getSuggestions(spellOpts);
diff --git a/solr/src/test/org/apache/solr/update/processor/SignatureUpdateProcessorFactoryTest.java b/solr/src/test/org/apache/solr/update/processor/SignatureUpdateProcessorFactoryTest.java
index 9b07cab1312..25f1a639442 100755
-- a/solr/src/test/org/apache/solr/update/processor/SignatureUpdateProcessorFactoryTest.java
++ b/solr/src/test/org/apache/solr/update/processor/SignatureUpdateProcessorFactoryTest.java
@@ -62,7 +62,7 @@ public class SignatureUpdateProcessorFactoryTest extends SolrTestCaseJ4 {
   void checkNumDocs(int n) {
     SolrQueryRequest req = req();
     try {
      assertEquals(n, req.getSearcher().getReader().numDocs());
      assertEquals(n, req.getSearcher().getIndexReader().numDocs());
     } finally {
       req.close();
     }
diff --git a/solr/src/webapp/src/org/apache/solr/servlet/cache/HttpCacheHeaderUtil.java b/solr/src/webapp/src/org/apache/solr/servlet/cache/HttpCacheHeaderUtil.java
index 6169a5cb095..3a905cc545c 100644
-- a/solr/src/webapp/src/org/apache/solr/servlet/cache/HttpCacheHeaderUtil.java
++ b/solr/src/webapp/src/org/apache/solr/servlet/cache/HttpCacheHeaderUtil.java
@@ -95,7 +95,7 @@ public final class HttpCacheHeaderUtil {
   public static String calcEtag(final SolrQueryRequest solrReq) {
     final SolrCore core = solrReq.getCore();
     final long currentIndexVersion
      = solrReq.getSearcher().getReader().getVersion();
      = solrReq.getSearcher().getIndexReader().getVersion();
 
     EtagCacheVal etagCache = etagCoreCache.get(core);
     if (null == etagCache) {
@@ -152,7 +152,7 @@ public final class HttpCacheHeaderUtil {
       // assume default, change if needed (getOpenTime() should be fast)
       lastMod =
         LastModFrom.DIRLASTMOD == lastModFrom
        ? IndexReader.lastModified(searcher.getReader().directory())
        ? IndexReader.lastModified(searcher.getIndexReader().directory())
         : searcher.getOpenTime();
     } catch (IOException e) {
       // we're pretty freaking screwed if this happens
- 
2.19.1.windows.1

