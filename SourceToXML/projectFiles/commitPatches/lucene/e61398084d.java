From e61398084d3f1ca0f28c5c35d3318645d7a401ec Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@apache.org>
Date: Sat, 16 Feb 2013 18:50:20 +0000
Subject: [PATCH] SOLR-3855: Doc values support.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1446922 13f79535-47bb-0310-9956-ffa450edef68
--
 .../function/valuesource/IntFieldSource.java  |   2 +-
 .../function/valuesource/LongFieldSource.java |   9 +
 solr/CHANGES.txt                              |   2 +
 .../apache/solr/core/SchemaCodecFactory.java  |  14 +-
 .../java/org/apache/solr/core/SolrCore.java   |   5 +
 .../handler/component/FieldFacetStats.java    | 122 +++----
 .../handler/component/StatsComponent.java     |  82 ++---
 .../solr/handler/component/StatsValues.java   |  17 +-
 .../handler/component/StatsValuesFactory.java |  94 ++---
 .../apache/solr/request/NumericFacets.java    | 328 ++++++++++++++++++
 .../org/apache/solr/request/SimpleFacets.java |  99 ++++--
 .../apache/solr/request/UnInvertedField.java  |   8 +-
 .../solr/schema/AbstractSpatialFieldType.java |  28 +-
 .../org/apache/solr/schema/CurrencyField.java |  12 +-
 .../org/apache/solr/schema/DateField.java     |  15 +-
 .../apache/solr/schema/FieldProperties.java   |   3 +-
 .../org/apache/solr/schema/FieldType.java     |  70 ++--
 .../org/apache/solr/schema/LatLonType.java    |  50 ++-
 .../org/apache/solr/schema/PointType.java     |   8 +-
 .../org/apache/solr/schema/SchemaField.java   |  15 +-
 .../solr/schema/SortableDoubleField.java      |  13 +-
 .../solr/schema/SortableFloatField.java       |  13 +-
 .../apache/solr/schema/SortableIntField.java  |  13 +-
 .../apache/solr/schema/SortableLongField.java |  13 +-
 .../java/org/apache/solr/schema/StrField.java |  45 ++-
 .../org/apache/solr/schema/TrieDateField.java |  22 +-
 .../org/apache/solr/schema/TrieField.java     |  69 +++-
 .../org/apache/solr/schema/UUIDField.java     |   2 -
 .../apache/solr/update/DocumentBuilder.java   |  47 +--
 ...hema-docValues-not-required-no-default.xml |  33 ++
 .../conf/bad-schema-unsupported-docValues.xml |  30 ++
 .../collection1/conf/schema-docValues.xml     |  74 ++++
 .../solr/collection1/conf/schema.xml          |   2 +-
 .../solr/collection1/conf/schema_codec.xml    |  19 +-
 .../apache/solr/core/TestCodecSupport.java    |  27 +-
 .../handler/component/StatsComponentTest.java |   8 +
 .../solr/schema/BadIndexSchemaTest.java       |   9 +-
 .../apache/solr/schema/CurrencyFieldTest.java |  15 +-
 .../org/apache/solr/schema/DocValuesTest.java | 230 ++++++++++++
 .../org/apache/solr/schema/PolyFieldTest.java |  17 +-
 solr/example/solr/collection1/conf/schema.xml |  28 +-
 41 files changed, 1321 insertions(+), 391 deletions(-)
 create mode 100644 solr/core/src/java/org/apache/solr/request/NumericFacets.java
 create mode 100644 solr/core/src/test-files/solr/collection1/conf/bad-schema-docValues-not-required-no-default.xml
 create mode 100644 solr/core/src/test-files/solr/collection1/conf/bad-schema-unsupported-docValues.xml
 create mode 100644 solr/core/src/test-files/solr/collection1/conf/schema-docValues.xml
 create mode 100644 solr/core/src/test/org/apache/solr/schema/DocValuesTest.java

diff --git a/lucene/queries/src/java/org/apache/lucene/queries/function/valuesource/IntFieldSource.java b/lucene/queries/src/java/org/apache/lucene/queries/function/valuesource/IntFieldSource.java
index c8a9a9af82c..296432d6f9a 100644
-- a/lucene/queries/src/java/org/apache/lucene/queries/function/valuesource/IntFieldSource.java
++ b/lucene/queries/src/java/org/apache/lucene/queries/function/valuesource/IntFieldSource.java
@@ -85,7 +85,7 @@ public class IntFieldSource extends FieldCacheSource {
 
       @Override
       public String strVal(int doc) {
        return Float.toString(arr.get(doc));
        return Integer.toString(arr.get(doc));
       }
 
       @Override
diff --git a/lucene/queries/src/java/org/apache/lucene/queries/function/valuesource/LongFieldSource.java b/lucene/queries/src/java/org/apache/lucene/queries/function/valuesource/LongFieldSource.java
index 1a8a9ad666d..597efe89e97 100644
-- a/lucene/queries/src/java/org/apache/lucene/queries/function/valuesource/LongFieldSource.java
++ b/lucene/queries/src/java/org/apache/lucene/queries/function/valuesource/LongFieldSource.java
@@ -64,6 +64,10 @@ public class LongFieldSource extends FieldCacheSource {
     return val;
   }
 
  public String longToString(long val) {
    return longToObject(val).toString();
  }

   @Override
   public FunctionValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
     final FieldCache.Longs arr = cache.getLongs(readerContext.reader(), field, parser, true);
@@ -85,6 +89,11 @@ public class LongFieldSource extends FieldCacheSource {
         return valid.get(doc) ? longToObject(arr.get(doc)) : null;
       }
 
      @Override
      public String strVal(int doc) {
        return valid.get(doc) ? longToString(arr.get(doc)) : null;
      }

       @Override
       public ValueSourceScorer getRangeScorer(IndexReader reader, String lowerVal, String upperVal, boolean includeLower, boolean includeUpper) {
         long lower,upper;
diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 40f546f080b..d908578cd01 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -77,6 +77,8 @@ New Features
   under the covers -- allowing many HTTP connection related properties to be
   controlled via 'standard' java system properties.  (hossman)
 
* SOLR-3855: Doc values support. (Adrien Grand)

 Bug Fixes
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/core/SchemaCodecFactory.java b/solr/core/src/java/org/apache/solr/core/SchemaCodecFactory.java
index 5e5a81d4e4b..e075913066a 100644
-- a/solr/core/src/java/org/apache/solr/core/SchemaCodecFactory.java
++ b/solr/core/src/java/org/apache/solr/core/SchemaCodecFactory.java
@@ -1,6 +1,7 @@
 package org.apache.solr.core;
 
 import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.DocValuesFormat;
 import org.apache.lucene.codecs.PostingsFormat;
 import org.apache.lucene.codecs.lucene42.Lucene42Codec;
 import org.apache.solr.schema.IndexSchema;
@@ -55,7 +56,18 @@ public class SchemaCodecFactory extends CodecFactory implements SchemaAware {
         }
         return super.getPostingsFormatForField(field);
       }
      // TODO: when dv support is added to solr, add it here too
      @Override
      public DocValuesFormat getDocValuesFormatForField(String field) {
        final SchemaField fieldOrNull = schema.getFieldOrNull(field);
        if (fieldOrNull == null) {
          throw new IllegalArgumentException("no such field " + field);
        }
        String docValuesFormatName = fieldOrNull.getType().getDocValuesFormat();
        if (docValuesFormatName != null) {
          return DocValuesFormat.forName(docValuesFormatName);
        }
        return super.getDocValuesFormatForField(field);
      }
     };
   }
 
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCore.java b/solr/core/src/java/org/apache/solr/core/SolrCore.java
index a3dd8f9af9a..4abdb6855c5 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCore.java
@@ -834,6 +834,11 @@ public final class SolrCore implements SolrInfoMBean {
           log.error(msg);
           throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, msg);
         }
        if (null != ft.getDocValuesFormat()) {
          String msg = "FieldType '" + ft.getTypeName() + "' is configured with a docValues format, but the codec does not support it: " + factory.getClass();
          log.error(msg);
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, msg);
        }
       }
     }
     return factory.getCodec();
diff --git a/solr/core/src/java/org/apache/solr/handler/component/FieldFacetStats.java b/solr/core/src/java/org/apache/solr/handler/component/FieldFacetStats.java
index adce22e90d2..6cd9a190573 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/FieldFacetStats.java
++ b/solr/core/src/java/org/apache/solr/handler/component/FieldFacetStats.java
@@ -16,16 +16,22 @@ package org.apache.solr.handler.component;
  * limitations under the License.
  */
 
import java.io.IOException;
 import java.util.ArrayList;
import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.util.BytesRef;
import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
 
 
 /**
@@ -40,101 +46,76 @@ import org.apache.solr.schema.SchemaField;
 
 public class FieldFacetStats {
   public final String name;
  final SortedDocValues si;
   final SchemaField facet_sf;
   final SchemaField field_sf;
 
  final int startTermIndex;
  final int endTermIndex;
  final int nTerms;
  public final Map<String, StatsValues> facetStatsValues;
 
  final int numStatsTerms;
  List<HashMap<String, Integer>> facetStatsTerms;
 
  public final Map<String, StatsValues> facetStatsValues;
  final AtomicReader topLevelReader;
  AtomicReaderContext leave;
  final ValueSource valueSource;
  AtomicReaderContext context;
  FunctionValues values;
 
  final List<HashMap<String, Integer>> facetStatsTerms;
  SortedDocValues topLevelSortedValues = null;
 
   private final BytesRef tempBR = new BytesRef();
 
  public FieldFacetStats(String name, SortedDocValues si, SchemaField field_sf, SchemaField facet_sf, int numStatsTerms) {
  public FieldFacetStats(SolrIndexSearcher searcher, String name, SchemaField field_sf, SchemaField facet_sf) {
     this.name = name;
    this.si = si;
     this.field_sf = field_sf;
     this.facet_sf = facet_sf;
    this.numStatsTerms = numStatsTerms;
 
    startTermIndex = 0;
    endTermIndex = si.getValueCount();
    nTerms = endTermIndex - startTermIndex;
    topLevelReader = searcher.getAtomicReader();
    valueSource = facet_sf.getType().getValueSource(facet_sf, null);
 
     facetStatsValues = new HashMap<String, StatsValues>();

    // for mv stats field, we'll want to keep track of terms
     facetStatsTerms = new ArrayList<HashMap<String, Integer>>();
    if (numStatsTerms == 0) return;
    int i = 0;
    for (; i < numStatsTerms; i++) {
      facetStatsTerms.add(new HashMap<String, Integer>());
    }
   }
 
  BytesRef getTermText(int docID, BytesRef ret) {
    final int ord = si.getOrd(docID);
    if (ord == -1) {
      return null;
    } else {
      si.lookupOrd(ord, ret);
      return ret;
  private StatsValues getStatsValues(String key) throws IOException {
    StatsValues stats = facetStatsValues.get(key);
    if (stats == null) {
      stats = StatsValuesFactory.createStatsValues(field_sf);
      facetStatsValues.put(key, stats);
      stats.setNextReader(context);
     }
    return stats;
   }
 
  public boolean facet(int docID, BytesRef v) {
    int term = si.getOrd(docID);
    int arrIdx = term - startTermIndex;
    if (arrIdx >= 0 && arrIdx < nTerms) {
      
      final BytesRef br;
      if (term == -1) {
        br = null;
      } else {
        br = tempBR;
        si.lookupOrd(term, tempBR);
      }
      String key = (br == null)?null:facet_sf.getType().indexedToReadable(br.utf8ToString());
      StatsValues stats = facetStatsValues.get(key);
      if (stats == null) {
        stats = StatsValuesFactory.createStatsValues(field_sf);
        facetStatsValues.put(key, stats);
      }

      if (v != null && v.length>0) {
        stats.accumulate(v);
      } else {
        stats.missing();
        return false;
      }
      return true;
    }
    return false;
  // docID is relative to the context
  public void facet(int docID) throws IOException {
    final String key = values.exists(docID)
        ? values.strVal(docID)
        : null;
    final StatsValues stats = getStatsValues(key);
    stats.accumulate(docID);
   }
 

   // Function to keep track of facet counts for term number.
   // Currently only used by UnInvertedField stats
  public boolean facetTermNum(int docID, int statsTermNum) {

    int term = si.getOrd(docID);
    int arrIdx = term - startTermIndex;
    if (arrIdx >= 0 && arrIdx < nTerms) {
  public boolean facetTermNum(int docID, int statsTermNum) throws IOException {
    if (topLevelSortedValues == null) {
      topLevelSortedValues = FieldCache.DEFAULT.getTermsIndex(topLevelReader, name);
    }
    
    int term = topLevelSortedValues.getOrd(docID);
    int arrIdx = term;
    if (arrIdx >= 0 && arrIdx < topLevelSortedValues.getValueCount()) {
       final BytesRef br;
       if (term == -1) {
         br = null;
       } else {
         br = tempBR;
        si.lookupOrd(term, tempBR);
        topLevelSortedValues.lookupOrd(term, tempBR);
       }
       String key = br == null ? null : br.utf8ToString();
      HashMap<String, Integer> statsTermCounts = facetStatsTerms.get(statsTermNum);
      while (facetStatsTerms.size() <= statsTermNum) {
        facetStatsTerms.add(new HashMap<String, Integer>());
      }
      final Map<String, Integer> statsTermCounts = facetStatsTerms.get(statsTermNum);
       Integer statsTermCount = statsTermCounts.get(key);
       if (statsTermCount == null) {
         statsTermCounts.put(key, 1);
@@ -148,8 +129,11 @@ public class FieldFacetStats {
 
 
   //function to accumulate counts for statsTermNum to specified value
  public boolean accumulateTermNum(int statsTermNum, BytesRef value) {
  public boolean accumulateTermNum(int statsTermNum, BytesRef value) throws IOException {
     if (value == null) return false;
    while (facetStatsTerms.size() <= statsTermNum) {
      facetStatsTerms.add(new HashMap<String, Integer>());
    }
     for (Map.Entry<String, Integer> stringIntegerEntry : facetStatsTerms.get(statsTermNum).entrySet()) {
       Map.Entry pairs = (Map.Entry) stringIntegerEntry;
       String key = (String) pairs.getKey();
@@ -166,6 +150,14 @@ public class FieldFacetStats {
     return true;
   }
 
  public void setNextReader(AtomicReaderContext ctx) throws IOException {
    this.context = ctx;
    values = valueSource.getValues(Collections.emptyMap(), ctx);
    for (StatsValues stats : facetStatsValues.values()) {
      stats.setNextReader(ctx);
    }
  }

 }
 
 
diff --git a/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java b/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java
index 521dc832b5e..dc433b42783 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java
@@ -20,12 +20,11 @@ package org.apache.solr.handler.component;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.ShardParams;
 import org.apache.solr.common.params.SolrParams;
@@ -43,14 +42,12 @@ import org.apache.solr.search.SolrIndexSearcher;
 
 /**
  * Stats component calculates simple statistics on numeric field values
 * 
 *
  * @since solr 1.4
  */
 public class StatsComponent extends SearchComponent {
 
   public static final String COMPONENT_NAME = "stats";
  

   @Override
   public void prepare(ResponseBuilder rb) throws IOException {
     if (rb.req.getParams().getBool(StatsParams.STATS,false)) {
@@ -236,25 +233,13 @@ class SimpleStats {
     }
     return res;
   }
  
  // why does this use a top-level field cache?
  public NamedList<?> getFieldCacheStats(String fieldName, String[] facet ) {
    SchemaField sf = searcher.getSchema().getField(fieldName);
    
    SortedDocValues si;
    try {
      si = FieldCache.DEFAULT.getTermsIndex(searcher.getAtomicReader(), fieldName);
    } 
    catch (IOException e) {
      throw new RuntimeException( "failed to open field cache for: "+fieldName, e );
    }
    StatsValues allstats = StatsValuesFactory.createStatsValues(sf);
    final int nTerms = si.getValueCount();
    if ( nTerms <= 0 || docs.size() <= 0 ) return allstats.getStatsValues();
 
    // don't worry about faceting if no documents match...
  public NamedList<?> getFieldCacheStats(String fieldName, String[] facet) throws IOException {
    final SchemaField sf = searcher.getSchema().getField(fieldName);

    final StatsValues allstats = StatsValuesFactory.createStatsValues(sf);

     List<FieldFacetStats> facetStats = new ArrayList<FieldFacetStats>();
    SortedDocValues facetTermsIndex;
     for( String facetField : facet ) {
       SchemaField fsf = searcher.getSchema().getField(facetField);
 
@@ -262,40 +247,32 @@ class SimpleStats {
         throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
           "Stats can only facet on single-valued fields, not: " + facetField );
       }
      
      try {
        facetTermsIndex = FieldCache.DEFAULT.getTermsIndex(searcher.getAtomicReader(), facetField);
      }
      catch (IOException e) {
        throw new RuntimeException( "failed to open field cache for: "
          + facetField, e );
      }
      facetStats.add(new FieldFacetStats(facetField, facetTermsIndex, sf, fsf, nTerms));

      facetStats.add(new FieldFacetStats(searcher, facetField, sf, fsf));
     }
    
    final BytesRef tempBR = new BytesRef();
    DocIterator iter = docs.iterator();
    while (iter.hasNext()) {
      int docID = iter.nextDoc();
      int docOrd = si.getOrd(docID);
      BytesRef raw;
      if (docOrd == -1) {
        allstats.missing();
        tempBR.length = 0;
        raw = tempBR;
      } else {
        raw = tempBR;
        si.lookupOrd(docOrd, tempBR);
        if( tempBR.length > 0 ) {
          allstats.accumulate(tempBR);
        } else {
          allstats.missing();

    final Iterator<AtomicReaderContext> ctxIt = searcher.getIndexReader().leaves().iterator();
    AtomicReaderContext ctx = null;
    for (DocIterator docsIt = docs.iterator(); docsIt.hasNext(); ) {
      final int doc = docsIt.nextDoc();
      if (ctx == null || doc >= ctx.docBase + ctx.reader().maxDoc()) {
        // advance
        do {
          ctx = ctxIt.next();
        } while (ctx == null || doc >= ctx.docBase + ctx.reader().maxDoc());
        assert doc >= ctx.docBase;

        // propagate the context among accumulators.
        allstats.setNextReader(ctx);
        for (FieldFacetStats f : facetStats) {
          f.setNextReader(ctx);
         }
       }
 
      // now update the facets
      // accumulate
      allstats.accumulate(doc - ctx.docBase);
       for (FieldFacetStats f : facetStats) {
        f.facet(docID, raw);
        f.facet(doc - ctx.docBase);
       }
     }
 
@@ -305,5 +282,4 @@ class SimpleStats {
     return allstats.getStatsValues();
   }
 

 }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/StatsValues.java b/solr/core/src/java/org/apache/solr/handler/component/StatsValues.java
index 492ef0148b6..cbcde045ea1 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/StatsValues.java
++ b/solr/core/src/java/org/apache/solr/handler/component/StatsValues.java
@@ -19,14 +19,19 @@
 package org.apache.solr.handler.component;
 
 
import java.io.IOException;
 import java.util.Map;
 
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.schema.FieldType;
 
 /**
  * StatsValue defines the interface for the collection of statistical values about fields and facets.
  */
// TODO: should implement Collector?
 public interface StatsValues {
 
   /**
@@ -36,12 +41,9 @@ public interface StatsValues {
    */
   void accumulate(NamedList stv);
 
  /**
   * Accumulate the values based on the given value
   *
   * @param value Value to use to accumulate the current values
   */
  void accumulate(BytesRef value);
  /** Accumulate the value associated with <code>docID</code>.
   *  @see #setNextReader(AtomicReaderContext) */
  void accumulate(int docID);
 
   /**
    * Accumulate the values based on the given value
@@ -77,4 +79,7 @@ public interface StatsValues {
    * @return NamedList representation of the current values
    */
   NamedList<?> getStatsValues();

  /** Set the context for {@link #accumulate(int)}. */
  void setNextReader(AtomicReaderContext ctx) throws IOException;
 }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/StatsValuesFactory.java b/solr/core/src/java/org/apache/solr/handler/component/StatsValuesFactory.java
index d4ef1c5831e..c350dd5d936 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/StatsValuesFactory.java
++ b/solr/core/src/java/org/apache/solr/handler/component/StatsValuesFactory.java
@@ -17,10 +17,15 @@
 
 package org.apache.solr.handler.component;
 
import java.io.IOException;
import java.util.Collections;
 import java.util.Date;
 import java.util.Map;
 import java.util.HashMap;
 
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.util.NamedList;
@@ -39,6 +44,7 @@ public class StatsValuesFactory {
    * @return Instance of StatsValues that will create statistics from values from a field of the given type
    */
   public static StatsValues createStatsValues(SchemaField sf) {
    // TODO: allow for custom field types
     FieldType fieldType = sf.getType();
     if (DoubleField.class.isInstance(fieldType) ||
         IntField.class.isInstance(fieldType) ||
@@ -77,6 +83,8 @@ abstract class AbstractStatsValues<T> implements StatsValues {
   protected T min;
   protected long missing;
   protected long count;
  private ValueSource valueSource;
  protected FunctionValues values;
   
   // facetField   facetValue
   protected Map<String, Map<String, StatsValues>> facets = new HashMap<String, Map<String, StatsValues>>();
@@ -121,29 +129,22 @@ abstract class AbstractStatsValues<T> implements StatsValues {
       }
     }
   }
  

   /**
    * {@inheritDoc}
    */
   @Override
  public void accumulate(BytesRef value) {
    count++;
  public void accumulate(BytesRef value, int count) {
     T typedValue = (T)ft.toObject(sf, value);
    updateMinMax(typedValue, typedValue);
    updateTypeSpecificStats(typedValue);
    accumulate(typedValue, count);
   }
 
  /**
   * {@inheritDoc}
   */
  @Override
  public void accumulate(BytesRef value, int count) {
  public void accumulate(T value, int count) {
     this.count += count;
    T typedValue = (T)ft.toObject(sf, value);
    updateMinMax(typedValue, typedValue);
    updateTypeSpecificStats(typedValue, count);
    updateMinMax(value, value);
    updateTypeSpecificStats(value, count);
   }
  

   /**
    * {@inheritDoc}
    */
@@ -194,6 +195,13 @@ abstract class AbstractStatsValues<T> implements StatsValues {
     return res;
   }
 
  public void setNextReader(AtomicReaderContext ctx) throws IOException {
    if (valueSource == null) {
      valueSource = ft.getValueSource(sf, null);
    }
    values = valueSource.getValues(Collections.emptyMap(), ctx);
  }

   /**
    * Updates the minimum and maximum statistics based on the given values
    *
@@ -202,13 +210,6 @@ abstract class AbstractStatsValues<T> implements StatsValues {
    */
   protected abstract void updateMinMax(T min, T max);
 
  /**
   * Updates the type specific statistics based on the given value
   *
   * @param value Value the statistics should be updated against
   */
  protected abstract void updateTypeSpecificStats(T value);

   /**
    * Updates the type specific statistics based on the given value
    *
@@ -246,23 +247,22 @@ class NumericStatsValues extends AbstractStatsValues<Number> {
     max = Double.NEGATIVE_INFINITY;
   }
 
  /**
   * {@inheritDoc}
   */
   @Override
  public void updateTypeSpecificStats(NamedList stv) {
    sum += ((Number)stv.get("sum")).doubleValue();
    sumOfSquares += ((Number)stv.get("sumOfSquares")).doubleValue();
  public void accumulate(int docID) {
    if (values.exists(docID)) {
      accumulate((Number) values.objectVal(docID), 1);
    } else {
      missing();
    }
   }
 
   /**
    * {@inheritDoc}
    */
   @Override
  public void updateTypeSpecificStats(Number v) {
    double value = v.doubleValue();
    sumOfSquares += (value * value); // for std deviation
    sum += value;
  public void updateTypeSpecificStats(NamedList stv) {
    sum += ((Number)stv.get("sum")).doubleValue();
    sumOfSquares += ((Number)stv.get("sumOfSquares")).doubleValue();
   }
 
   /**
@@ -323,23 +323,22 @@ class DateStatsValues extends AbstractStatsValues<Date> {
     super(sf);
   }
 
  /**
   * {@inheritDoc}
   */
   @Override
  protected void updateTypeSpecificStats(NamedList stv) {
    sum += ((Date) stv.get("sum")).getTime();
    sumOfSquares += ((Number)stv.get("sumOfSquares")).doubleValue();
  public void accumulate(int docID) {
    if (values.exists(docID)) {
      accumulate((Date) values.objectVal(docID), 1);
    } else {
      missing();
    }
   }
 
   /**
    * {@inheritDoc}
    */
   @Override
  public void updateTypeSpecificStats(Date v) {
    long value = v.getTime();
    sumOfSquares += (value * value); // for std deviation
    sum += value;
  protected void updateTypeSpecificStats(NamedList stv) {
    sum += ((Date) stv.get("sum")).getTime();
    sumOfSquares += ((Number)stv.get("sumOfSquares")).doubleValue();
   }
 
   /**
@@ -407,19 +406,20 @@ class StringStatsValues extends AbstractStatsValues<String> {
     super(sf);
   }
 
  /**
   * {@inheritDoc}
   */
   @Override
  protected void updateTypeSpecificStats(NamedList stv) {
    // No type specific stats
  public void accumulate(int docID) {
    if (values.exists(docID)) {
      accumulate(values.strVal(docID), 1);
    } else {
      missing();
    }
   }
 
   /**
    * {@inheritDoc}
    */
   @Override
  protected void updateTypeSpecificStats(String value) {
  protected void updateTypeSpecificStats(NamedList stv) {
     // No type specific stats
   }
 
diff --git a/solr/core/src/java/org/apache/solr/request/NumericFacets.java b/solr/core/src/java/org/apache/solr/request/NumericFacets.java
new file mode 100644
index 00000000000..7a6ec4c9861
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/request/NumericFacets.java
@@ -0,0 +1,328 @@
package org.apache.solr.request;

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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.PriorityQueue;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;

/** Utility class to compute facets on numeric fields. */
final class NumericFacets {

  NumericFacets() {}

  static class HashTable {

    static final float LOAD_FACTOR = 0.7f;

    long[] bits; // bits identifying a value
    int[] counts;
    int[] docIDs;
    int mask;
    int size;
    int threshold;

    HashTable() {
      final int capacity = 64; // must be a power of 2
      bits = new long[capacity];
      counts = new int[capacity];
      docIDs = new int[capacity];
      mask = capacity - 1;
      size = 0;
      threshold = (int) (capacity * LOAD_FACTOR);
    }

    private int hash(long v) {
      int h = (int) (v ^ (v >>> 32));
      h = (31 * h) & mask; // * 31 to try to use the whole table, even if values are dense
      return h;
    }

    void add(int docID, long value, int count) {
      if (size >= threshold) {
        rehash();
      }
      final int h = hash(value);
      for (int slot = h; ; slot = (slot + 1) & mask) {
        if (counts[slot] == 0) {
          bits[slot] = value;
          docIDs[slot] = docID;
          ++size;
        } else if (bits[slot] != value) {
          continue;
        }
        counts[slot] += count;
        break;
      }
    }

    private void rehash() {
      final long[] oldBits = bits;
      final int[] oldCounts = counts;
      final int[] oldDocIDs = docIDs;

      final int newCapacity = bits.length * 2;
      bits = new long[newCapacity];
      counts = new int[newCapacity];
      docIDs = new int[newCapacity];
      mask = newCapacity - 1;
      threshold = (int) (LOAD_FACTOR * newCapacity);
      size = 0;

      for (int i = 0; i < oldBits.length; ++i) {
        if (oldCounts[i] > 0) {
          add(oldDocIDs[i], oldBits[i], oldCounts[i]);
        }
      }
    }

  }

  private static class Entry {
    int docID;
    int count;
    long bits;
  }

  public static NamedList<Integer> getCounts(SolrIndexSearcher searcher, DocSet docs, String fieldName, int offset, int limit, int mincount, boolean missing, String sort) throws IOException {
    final boolean zeros = mincount <= 0;
    mincount = Math.max(mincount, 1);
    final SchemaField sf = searcher.getSchema().getField(fieldName);
    final FieldType ft = sf.getType();
    final NumericType numericType = ft.getNumericType();
    if (numericType == null) {
      throw new IllegalStateException();
    }
    final List<AtomicReaderContext> leaves = searcher.getIndexReader().leaves();

    // 1. accumulate
    final HashTable hashTable = new HashTable();
    final Iterator<AtomicReaderContext> ctxIt = leaves.iterator();
    AtomicReaderContext ctx = null;
    FieldCache.Longs longs = null;
    Bits docsWithField = null;
    int missingCount = 0;
    for (DocIterator docsIt = docs.iterator(); docsIt.hasNext(); ) {
      final int doc = docsIt.nextDoc();
      if (ctx == null || doc >= ctx.docBase + ctx.reader().maxDoc()) {
        do {
          ctx = ctxIt.next();
        } while (ctx == null || doc >= ctx.docBase + ctx.reader().maxDoc());
        assert doc >= ctx.docBase;
        switch (numericType) {
          case LONG:
            longs = FieldCache.DEFAULT.getLongs(ctx.reader(), fieldName, true);
            break;
          case INT:
            final FieldCache.Ints ints = FieldCache.DEFAULT.getInts(ctx.reader(), fieldName, true);
            longs = new FieldCache.Longs() {
              @Override
              public long get(int docID) {
                return ints.get(docID);
              }
            };
            break;
          case FLOAT:
            final FieldCache.Floats floats = FieldCache.DEFAULT.getFloats(ctx.reader(), fieldName, true);
            longs = new FieldCache.Longs() {
              @Override
              public long get(int docID) {
                return Float.floatToIntBits(floats.get(docID));
              }
            };
            break;
          case DOUBLE:
            final FieldCache.Doubles doubles = FieldCache.DEFAULT.getDoubles(ctx.reader(), fieldName, true);
            longs = new FieldCache.Longs() {
              @Override
              public long get(int docID) {
                return Double.doubleToLongBits(doubles.get(docID));
              }
            };
            break;
          default:
            throw new AssertionError();
        }
        docsWithField = FieldCache.DEFAULT.getDocsWithField(ctx.reader(), fieldName);
      }
      if (docsWithField.get(doc - ctx.docBase)) {
        hashTable.add(doc, longs.get(doc - ctx.docBase), 1);
      } else {
        ++missingCount;
      }
    }

    // 2. select top-k facet values
    final int pqSize = limit < 0 ? hashTable.size : Math.min(offset + limit, hashTable.size);
    final PriorityQueue<Entry> pq;
    if (FacetParams.FACET_SORT_COUNT.equals(sort) || FacetParams.FACET_SORT_COUNT_LEGACY.equals(sort)) {
      pq = new PriorityQueue<Entry>(pqSize) {
        @Override
        protected boolean lessThan(Entry a, Entry b) {
          if (a.count < b.count || (a.count == b.count && a.bits > b.bits)) {
            return true;
          } else {
            return false;
          }
        }
      };
    } else {
      pq = new PriorityQueue<Entry>(pqSize) {
        @Override
        protected boolean lessThan(Entry a, Entry b) {
          return a.bits > b.bits;
        }
      };
    }
    Entry e = null;
    for (int i = 0; i < hashTable.bits.length; ++i) {
      if (hashTable.counts[i] >= mincount) {
        if (e == null) {
          e = new Entry();
        }
        e.bits = hashTable.bits[i];
        e.count = hashTable.counts[i];
        e.docID = hashTable.docIDs[i];
        e = pq.insertWithOverflow(e);
      }
    }

    // 4. build the NamedList
    final ValueSource vs = ft.getValueSource(sf, null);
    final NamedList<Integer> result = new NamedList<Integer>();

    // This stuff is complicated because if facet.mincount=0, the counts needs
    // to be merged with terms from the terms dict
    if (!zeros || FacetParams.FACET_SORT_COUNT.equals(sort) || FacetParams.FACET_SORT_COUNT_LEGACY.equals(sort)) {
      // Only keep items we're interested in
      final Deque<Entry> counts = new ArrayDeque<Entry>();
      while (pq.size() > offset) {
        counts.addFirst(pq.pop());
      }
      
      // Entries from the PQ first, then using the terms dictionary
      for (Entry entry : counts) {
        final int readerIdx = ReaderUtil.subIndex(entry.docID, leaves);
        final FunctionValues values = vs.getValues(Collections.emptyMap(), leaves.get(readerIdx));
        result.add(values.strVal(entry.docID - leaves.get(readerIdx).docBase), entry.count);
      }

      if (zeros && (limit < 0 || result.size() < limit)) { // need to merge with the term dict
        if (!sf.indexed()) {
          throw new IllegalStateException("Cannot use " + FacetParams.FACET_MINCOUNT + "=0 on a field which is not indexed");
        }
        // Add zeros until there are limit results
        final Set<String> alreadySeen = new HashSet<String>();
        while (pq.size() > 0) {
          Entry entry = pq.pop();
          final int readerIdx = ReaderUtil.subIndex(entry.docID, leaves);
          final FunctionValues values = vs.getValues(Collections.emptyMap(), leaves.get(readerIdx));
          alreadySeen.add(values.strVal(entry.docID - leaves.get(readerIdx).docBase));
        }
        for (int i = 0; i < result.size(); ++i) {
          alreadySeen.add(result.getName(i));
        }
        final Terms terms = searcher.getAtomicReader().terms(fieldName);
        if (terms != null) {
          final TermsEnum termsEnum = terms.iterator(null);
          BytesRef term = termsEnum.next();
          final CharsRef spare = new CharsRef();
          for (int skipped = hashTable.size; skipped < offset && term != null; ) {
            ft.indexedToReadable(term, spare);
            final String termStr = spare.toString();
            if (!alreadySeen.contains(termStr)) {
              ++skipped;
            }
            term = termsEnum.next();
          }
          for ( ; term != null && (limit < 0 || result.size() < limit); term = termsEnum.next()) {
            ft.indexedToReadable(term, spare);
            final String termStr = spare.toString();
            if (!alreadySeen.contains(termStr)) {
              result.add(termStr, 0);
            }
          }
        }
      }
    } else {
      // sort=index, mincount=0 and we have less than limit items
      // => Merge the PQ and the terms dictionary on the fly
      if (!sf.indexed()) {
        throw new IllegalStateException("Cannot use " + FacetParams.FACET_SORT + "=" + FacetParams.FACET_SORT_INDEX + " on a field which is not indexed");
      }
      final Map<String, Integer> counts = new HashMap<String, Integer>();
      while (pq.size() > 0) {
        final Entry entry = pq.pop();
        final int readerIdx = ReaderUtil.subIndex(entry.docID, leaves);
        final FunctionValues values = vs.getValues(Collections.emptyMap(), leaves.get(readerIdx));
        counts.put(values.strVal(entry.docID - leaves.get(readerIdx).docBase), entry.count);
      }
      final Terms terms = searcher.getAtomicReader().terms(fieldName);
      if (terms != null) {
        final TermsEnum termsEnum = terms.iterator(null);
        final CharsRef spare = new CharsRef();
        BytesRef term = termsEnum.next();
        for (int i = 0; i < offset && term != null; ++i) {
          term = termsEnum.next();
        }
        for ( ; term != null && (limit < 0 || result.size() < limit); term = termsEnum.next()) {
          ft.indexedToReadable(term, spare);
          final String termStr = spare.toString();
          Integer count = counts.get(termStr);
          if (count == null) {
            count = 0;
          }
          result.add(termStr, count);
        }
      }
    }

    if (missing) {
      result.add(null, missingCount);
    }
    return result;
  }

}
diff --git a/solr/core/src/java/org/apache/solr/request/SimpleFacets.java b/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
index 5dbd054a7a7..f0f48787b14 100644
-- a/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
++ b/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
@@ -17,37 +17,83 @@
 
 package org.apache.solr.request;
 
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MultiDocsEnum;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
 import org.apache.lucene.search.grouping.AbstractAllGroupHeadsCollector;
import org.apache.lucene.search.grouping.term.TermGroupFacetCollector;
 import org.apache.lucene.search.grouping.term.TermAllGroupsCollector;
import org.apache.lucene.util.*;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.search.grouping.term.TermGroupFacetCollector;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.UnicodeUtil;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.*;
import org.apache.solr.common.params.FacetParams.FacetRangeOther;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
 import org.apache.solr.common.params.FacetParams.FacetRangeInclude;
import org.apache.solr.common.params.FacetParams.FacetRangeOther;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.RequiredSolrParams;
import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.common.util.StrUtils;
import org.apache.solr.schema.*;
import org.apache.solr.search.*;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.schema.BoolField;
import org.apache.solr.schema.DateField;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.SortableDoubleField;
import org.apache.solr.schema.SortableFloatField;
import org.apache.solr.schema.SortableIntField;
import org.apache.solr.schema.SortableLongField;
import org.apache.solr.schema.TrieField;
import org.apache.solr.search.BitDocSet;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.Grouping;
import org.apache.solr.search.HashDocSet;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SortedIntDocSet;
import org.apache.solr.search.SyntaxError;
 import org.apache.solr.search.grouping.GroupingSpecification;
 import org.apache.solr.util.BoundedTreeSet;
 import org.apache.solr.util.DateMathParser;
 import org.apache.solr.util.DefaultSolrThreadFactory;
import org.apache.solr.handler.component.ResponseBuilder;
 import org.apache.solr.util.LongPriorityQueue;
 
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

 /**
  * A class that generates simple Facet information for a request.
  *
@@ -300,7 +346,8 @@ public class SimpleFacets {
     boolean enumMethod = FacetParams.FACET_METHOD_enum.equals(method);
 
     // TODO: default to per-segment or not?
    boolean per_segment = FacetParams.FACET_METHOD_fcs.equals(method);
    boolean per_segment = FacetParams.FACET_METHOD_fcs.equals(method) // explicit
        || (ft.getNumericType() != null && sf.hasDocValues()); // numeric doc values are per-segment by default
 
     if (method == null && ft instanceof BoolField) {
       // Always use filters for booleans... we know the number of values is very small.
@@ -329,10 +376,18 @@ public class SimpleFacets {
           // TODO: future logic could use filters instead of the fieldcache if
           // the number of terms in the field is small enough.
           if (per_segment) {
            PerSegmentSingleValuedFaceting ps = new PerSegmentSingleValuedFaceting(searcher, docs, field, offset,limit, mincount, missing, sort, prefix);
            Executor executor = threads == 0 ? directExecutor : facetExecutor;
            ps.setNumThreads(threads);
            counts = ps.getFacetCounts(executor);
            if (ft.getNumericType() != null && !sf.multiValued()) {
              // force numeric faceting
              if (prefix != null && !prefix.isEmpty()) {
                throw new SolrException(ErrorCode.BAD_REQUEST, FacetParams.FACET_PREFIX + " is not supported on numeric types");
              }
              counts = NumericFacets.getCounts(searcher, docs, field, offset, limit, mincount, missing, sort);
            } else {
              PerSegmentSingleValuedFaceting ps = new PerSegmentSingleValuedFaceting(searcher, docs, field, offset,limit, mincount, missing, sort, prefix);
              Executor executor = threads == 0 ? directExecutor : facetExecutor;
              ps.setNumThreads(threads);
              counts = ps.getFacetCounts(executor);
            }
           } else {
             counts = getFieldCacheCounts(searcher, docs, field, offset,limit, mincount, missing, sort, prefix);
           }
diff --git a/solr/core/src/java/org/apache/solr/request/UnInvertedField.java b/solr/core/src/java/org/apache/solr/request/UnInvertedField.java
index 7407e790e43..0a106bb4aeb 100755
-- a/solr/core/src/java/org/apache/solr/request/UnInvertedField.java
++ b/solr/core/src/java/org/apache/solr/request/UnInvertedField.java
@@ -483,13 +483,7 @@ public class UnInvertedField extends DocTermOrds {
     SortedDocValues si;
     for (String f : facet) {
       SchemaField facet_sf = searcher.getSchema().getField(f);
      try {
        si = FieldCache.DEFAULT.getTermsIndex(searcher.getAtomicReader(), f);
      }
      catch (IOException e) {
        throw new RuntimeException("failed to open field cache for: " + f, e);
      }
      finfo[i] = new FieldFacetStats(f, si, sf, facet_sf, numTermsInField);
      finfo[i] = new FieldFacetStats(searcher, f, sf, facet_sf);
       i++;
     }
 
diff --git a/solr/core/src/java/org/apache/solr/schema/AbstractSpatialFieldType.java b/solr/core/src/java/org/apache/solr/schema/AbstractSpatialFieldType.java
index 4721e9e0800..c48ddf40fac 100644
-- a/solr/core/src/java/org/apache/solr/schema/AbstractSpatialFieldType.java
++ b/solr/core/src/java/org/apache/solr/schema/AbstractSpatialFieldType.java
@@ -51,6 +51,10 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
 import java.util.Map;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ExecutionException;
@@ -103,7 +107,7 @@ public abstract class AbstractSpatialFieldType<T extends SpatialStrategy> extend
   }
 
   @Override
  public Field[] createFields(SchemaField field, Object val, float boost) {
  public List<StorableField> createFields(SchemaField field, Object val, float boost) {
     String shapeStr = null;
     Shape shape = null;
     if (val instanceof Shape) {
@@ -114,34 +118,22 @@ public abstract class AbstractSpatialFieldType<T extends SpatialStrategy> extend
     }
     if (shape == null) {
       log.debug("Field {}: null shape for input: {}", field, val);
      return null;
      return Collections.emptyList();
     }
 
    Field[] indexableFields = null;
    List<StorableField> result = new ArrayList<StorableField>();
     if (field.indexed()) {
       T strategy = getStrategy(field.getName());
      indexableFields = strategy.createIndexableFields(shape);
      result.addAll(Arrays.asList(strategy.createIndexableFields(shape)));
     }
 
    StoredField storedField = null;
     if (field.stored()) {
       if (shapeStr == null)
         shapeStr = shapeToString(shape);
      storedField = new StoredField(field.getName(), shapeStr);
      result.add(new StoredField(field.getName(), shapeStr));
     }
 
    if (indexableFields == null) {
      if (storedField == null)
        return null;
      return new Field[]{storedField};
    } else {
      if (storedField == null)
        return indexableFields;
      Field[] result = new Field[indexableFields.length+1];
      System.arraycopy(indexableFields,0,result,0,indexableFields.length);
      result[result.length-1] = storedField;
      return result;
    }
    return result;
   }
 
   protected Shape parseShape(String shapeStr) {
diff --git a/solr/core/src/java/org/apache/solr/schema/CurrencyField.java b/solr/core/src/java/org/apache/solr/schema/CurrencyField.java
index 0575a3d9670..32b7ce7e5b9 100644
-- a/solr/core/src/java/org/apache/solr/schema/CurrencyField.java
++ b/solr/core/src/java/org/apache/solr/schema/CurrencyField.java
@@ -46,9 +46,11 @@ import javax.xml.xpath.XPathExpressionException;
 import javax.xml.xpath.XPathFactory;
 import java.io.IOException;
 import java.io.InputStream;
import java.util.ArrayList;
 import java.util.Currency;
 import java.util.HashMap;
 import java.util.HashSet;
import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
@@ -145,14 +147,14 @@ public class CurrencyField extends FieldType implements SchemaAware, ResourceLoa
   }
 
   @Override
  public StorableField[] createFields(SchemaField field, Object externalVal, float boost) {
  public List<StorableField> createFields(SchemaField field, Object externalVal, float boost) {
     CurrencyValue value = CurrencyValue.parse(externalVal.toString(), defaultCurrency);
 
    StorableField[] f = new StorableField[field.stored() ? 3 : 2];
    List<StorableField> f = new ArrayList<StorableField>();
     SchemaField amountField = getAmountField(field);
    f[0] = amountField.createField(String.valueOf(value.getAmount()), amountField.indexed() && !amountField.omitNorms() ? boost : 1F);
    f.add(amountField.createField(String.valueOf(value.getAmount()), amountField.indexed() && !amountField.omitNorms() ? boost : 1F));
     SchemaField currencyField = getCurrencyField(field);
    f[1] = currencyField.createField(value.getCurrencyCode(), currencyField.indexed() && !currencyField.omitNorms() ? boost : 1F);
    f.add(currencyField.createField(value.getCurrencyCode(), currencyField.indexed() && !currencyField.omitNorms() ? boost : 1F));
 
     if (field.stored()) {
       org.apache.lucene.document.FieldType customType = new org.apache.lucene.document.FieldType();
@@ -162,7 +164,7 @@ public class CurrencyField extends FieldType implements SchemaAware, ResourceLoa
       if (storedValue.indexOf(",") < 0) {
         storedValue += "," + defaultCurrency;
       }
      f[2] = createField(field.getName(), storedValue, customType, 1F);
      f.add(createField(field.getName(), storedValue, customType, 1F));
     }
 
     return f;
diff --git a/solr/core/src/java/org/apache/solr/schema/DateField.java b/solr/core/src/java/org/apache/solr/schema/DateField.java
index ac934cf7d36..f047e1a0650 100644
-- a/solr/core/src/java/org/apache/solr/schema/DateField.java
++ b/solr/core/src/java/org/apache/solr/schema/DateField.java
@@ -435,7 +435,7 @@ public class DateField extends PrimitiveFieldType {
   @Override
   public ValueSource getValueSource(SchemaField field, QParser parser) {
     field.checkFieldCacheSource(parser);
    return new DateFieldSource(field.getName(), field.getType());
    return new DateFieldSource(field.getName(), field);
   }
 
   /** DateField specific range query */
@@ -453,11 +453,13 @@ public class DateField extends PrimitiveFieldType {
 
 class DateFieldSource extends FieldCacheSource {
   // NOTE: this is bad for serialization... but we currently need the fieldType for toInternal()
  SchemaField sf;
   FieldType ft;
 
  public DateFieldSource(String name, FieldType ft) {
  public DateFieldSource(String name, SchemaField sf) {
     super(name);
    this.ft = ft;
    this.sf = sf;
    this.ft = sf.getType();
   }
 
   @Override
@@ -474,6 +476,11 @@ class DateFieldSource extends FieldCacheSource {
         return ft.toInternal(readableValue);
       }
 
      @Override
      public boolean exists(int doc) {
        return termsIndex.getOrd(doc) >= 0;
      }

       @Override
       public float floatVal(int doc) {
         return (float)intVal(doc);
@@ -514,7 +521,7 @@ class DateFieldSource extends FieldCacheSource {
         } else {
           final BytesRef br = new BytesRef();
           termsIndex.lookupOrd(ord, br);
          return ft.toObject(null, br);
          return ft.toObject(sf, br);
         }
       }
 
diff --git a/solr/core/src/java/org/apache/solr/schema/FieldProperties.java b/solr/core/src/java/org/apache/solr/schema/FieldProperties.java
index 137d86c2016..3a2b987b72c 100644
-- a/solr/core/src/java/org/apache/solr/schema/FieldProperties.java
++ b/solr/core/src/java/org/apache/solr/schema/FieldProperties.java
@@ -50,6 +50,7 @@ public abstract class FieldProperties {
   protected final static int OMIT_POSITIONS      = 0x00002000;
 
   protected final static int STORE_OFFSETS       = 0x00004000;
  protected final static int DOC_VALUES          = 0x00008000;
 
   static final String[] propertyNames = {
           "indexed", "tokenized", "stored",
@@ -57,7 +58,7 @@ public abstract class FieldProperties {
           "termVectors", "termPositions", "termOffsets",
           "multiValued",
           "sortMissingFirst","sortMissingLast","required", "omitPositions",
          "storeOffsetsWithPositions"
          "storeOffsetsWithPositions", "docValues"
   };
 
   static final Map<String,Integer> propertyMap = new HashMap<String,Integer>();
diff --git a/solr/core/src/java/org/apache/solr/schema/FieldType.java b/solr/core/src/java/org/apache/solr/schema/FieldType.java
index 9ba293c4a21..3bf13f17b51 100644
-- a/solr/core/src/java/org/apache/solr/schema/FieldType.java
++ b/solr/core/src/java/org/apache/solr/schema/FieldType.java
@@ -17,14 +17,20 @@
 
 package org.apache.solr.schema;
 
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.document.Field;
import org.apache.lucene.index.FieldInfo.DocValuesType;
 import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.GeneralField;
import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.StorableField;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.queries.function.ValueSource;
@@ -45,11 +51,6 @@ import org.apache.solr.search.Sorting;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

 /**
  * Base class for all field types used by an index schema.
  *
@@ -120,14 +121,6 @@ public abstract class FieldType extends FieldProperties {
 
   }
 
  protected String getArg(String n, Map<String,String> args) {
    String s = args.remove(n);
    if (s == null) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Missing parameter '"+n+"' for FieldType=" + typeName +args);
    }
    return s;
  }

   // Handle additional arguments...
   void setArgs(IndexSchema schema, Map<String,String> args) {
     // default to STORED, INDEXED, OMIT_TF_POSITIONS and MULTIVALUED depending on schema version
@@ -169,11 +162,8 @@ public abstract class FieldType extends FieldProperties {
       initArgs.remove("positionIncrementGap");
     }
 
    final String postingsFormat = initArgs.get("postingsFormat");
    if (postingsFormat != null) {
      this.postingsFormat = postingsFormat;
      initArgs.remove("postingsFormat");
    }
    this.postingsFormat = initArgs.remove("postingsFormat");
    this.docValuesFormat = initArgs.remove("docValuesFormat");
 
     if (initArgs.size() > 0) {
       throw new RuntimeException("schema fieldtype " + typeName
@@ -261,7 +251,7 @@ public abstract class FieldType extends FieldProperties {
     newType.setStoreTermVectors(field.storeTermVector());
     newType.setStoreTermVectorOffsets(field.storeTermOffsets());
     newType.setStoreTermVectorPositions(field.storeTermPositions());
    

     return createField(field.getName(), val, newType, boost);
   }
 
@@ -290,9 +280,15 @@ public abstract class FieldType extends FieldProperties {
    * @see #createField(SchemaField, Object, float)
    * @see #isPolyField()
    */
  public StorableField[] createFields(SchemaField field, Object value, float boost) {
  public List<StorableField> createFields(SchemaField field, Object value, float boost) {
     StorableField f = createField( field, value, boost);
    return f==null ? new StorableField[]{} : new StorableField[]{f};
    if (field.hasDocValues() && f.fieldType().docValueType() == null) {
      // field types that support doc values should either override createField
      // to return a field with doc values or extend createFields if this can't
      // be done in a single field instance (see StrField for example)
      throw new UnsupportedOperationException("This field type does not support doc values: " + this);
    }
    return f==null ? Collections.<StorableField>emptyList() : Collections.singletonList(f);
   }
 
   protected IndexOptions getIndexOptions(SchemaField field, String internalVal) {
@@ -513,7 +509,13 @@ public abstract class FieldType extends FieldProperties {
   public Similarity getSimilarity() {
     return similarity;
   }
  

  /** Return the numeric type of this field, or null if this field is not a
   *  numeric field. */
  public org.apache.lucene.document.FieldType.NumericType getNumericType() {
    return null;
  }

   /**
    * Sets the Similarity used when scoring fields of this type
    * @lucene.internal
@@ -530,7 +532,16 @@ public abstract class FieldType extends FieldProperties {
   public String getPostingsFormat() {
     return postingsFormat;
   }
  

  /**
   * The docvalues format used for this field type
   */
  protected String docValuesFormat;

  public final String getDocValuesFormat() {
    return docValuesFormat;
  }

   /**
    * calls back to TextResponseWriter to write the field value
    */
@@ -562,7 +573,6 @@ public abstract class FieldType extends FieldProperties {
     return new StrFieldSource(field.name);
   }
 

   /**
    * Returns a Query instance for doing range searches on this field type. {@link org.apache.solr.search.SolrQueryParser}
    * currently passes part1 and part2 as null if they are '*' respectively. minInclusive and maxInclusive are both true
@@ -615,7 +625,11 @@ public abstract class FieldType extends FieldProperties {
    * if invariants are violated by the <code>SchemaField.</code>
    * </p>
    */
  public void checkSchemaField(final SchemaField field) throws SolrException {
    // :NOOP:
  public void checkSchemaField(final SchemaField field) {
    // override if your field type supports doc values
    if (field.hasDocValues()) {
      throw new SolrException(ErrorCode.SERVER_ERROR, "Field type " + this + " does not support doc values");
    }
   }

 }
diff --git a/solr/core/src/java/org/apache/solr/schema/LatLonType.java b/solr/core/src/java/org/apache/solr/schema/LatLonType.java
index d9fe7393677..04ecd4356db 100644
-- a/solr/core/src/java/org/apache/solr/schema/LatLonType.java
++ b/solr/core/src/java/org/apache/solr/schema/LatLonType.java
@@ -16,30 +16,42 @@ package org.apache.solr.schema;
  * limitations under the License.
  */
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

 import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.StorableField;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.VectorValueSource;
import org.apache.lucene.search.*;
import com.spatial4j.core.io.ParseUtils;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.exception.InvalidShapeException;
import com.spatial4j.core.shape.Rectangle;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ComplexExplanation;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.Weight;
 import org.apache.lucene.util.Bits;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.search.*;
import org.apache.solr.search.function.distance.HaversineConstFunction;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.PostFilter;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SpatialOptions;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.exception.InvalidShapeException;
import com.spatial4j.core.io.ParseUtils;
import com.spatial4j.core.shape.Rectangle;
 
 
 /**
@@ -57,10 +69,10 @@ public class LatLonType extends AbstractSubTypeFieldType implements SpatialQuery
   }
 
   @Override
  public StorableField[] createFields(SchemaField field, Object value, float boost) {
  public List<StorableField> createFields(SchemaField field, Object value, float boost) {
     String externalVal = value.toString();
     //we could have tileDiff + 3 fields (two for the lat/lon, one for storage)
    StorableField[] f = new StorableField[(field.indexed() ? 2 : 0) + (field.stored() ? 1 : 0)];
    List<StorableField> f = new ArrayList<StorableField>(3);
     if (field.indexed()) {
       int i = 0;
       double[] latLon;
@@ -71,18 +83,18 @@ public class LatLonType extends AbstractSubTypeFieldType implements SpatialQuery
       }
       //latitude
       SchemaField lat = subField(field, i);
      f[i] = lat.createField(String.valueOf(latLon[LAT]), lat.indexed() && !lat.omitNorms() ? boost : 1f);
      f.add(lat.createField(String.valueOf(latLon[LAT]), lat.indexed() && !lat.omitNorms() ? boost : 1f));
       i++;
       //longitude
       SchemaField lon = subField(field, i);
      f[i] = lon.createField(String.valueOf(latLon[LON]), lon.indexed() && !lon.omitNorms() ? boost : 1f);
      f.add(lon.createField(String.valueOf(latLon[LON]), lon.indexed() && !lon.omitNorms() ? boost : 1f));
 
     }
 
     if (field.stored()) {
       FieldType customType = new FieldType();
       customType.setStored(true);
      f[f.length - 1] = createField(field.getName(), externalVal, customType, 1f);
      f.add(createField(field.getName(), externalVal, customType, 1f));
     }
     return f;
   }
diff --git a/solr/core/src/java/org/apache/solr/schema/PointType.java b/solr/core/src/java/org/apache/solr/schema/PointType.java
index 0697db7dfb9..d70c66194e4 100644
-- a/solr/core/src/java/org/apache/solr/schema/PointType.java
++ b/solr/core/src/java/org/apache/solr/schema/PointType.java
@@ -69,7 +69,7 @@ public class PointType extends CoordinateFieldType implements SpatialQueryable {
   }
 
   @Override
  public StorableField[] createFields(SchemaField field, Object value, float boost) {
  public List<StorableField> createFields(SchemaField field, Object value, float boost) {
     String externalVal = value.toString();
     String[] point = new String[0];
     try {
@@ -79,12 +79,12 @@ public class PointType extends CoordinateFieldType implements SpatialQueryable {
     }
 
     // TODO: this doesn't currently support polyFields as sub-field types
    StorableField[] f = new StorableField[ (field.indexed() ? dimension : 0) + (field.stored() ? 1 : 0) ];
    List<StorableField> f = new ArrayList<StorableField>(dimension+1);
 
     if (field.indexed()) {
       for (int i=0; i<dimension; i++) {
         SchemaField sf = subField(field, i);
        f[i] = sf.createField(point[i], sf.indexed() && !sf.omitNorms() ? boost : 1f);
        f.add(sf.createField(point[i], sf.indexed() && !sf.omitNorms() ? boost : 1f));
       }
     }
 
@@ -92,7 +92,7 @@ public class PointType extends CoordinateFieldType implements SpatialQueryable {
       String storedVal = externalVal;  // normalize or not?
       FieldType customType = new FieldType();
       customType.setStored(true);
      f[f.length - 1] = createField(field.getName(), storedVal, customType, 1f);
      f.add(createField(field.getName(), storedVal, customType, 1f));
     }
     
     return f;
diff --git a/solr/core/src/java/org/apache/solr/schema/SchemaField.java b/solr/core/src/java/org/apache/solr/schema/SchemaField.java
index 54335b3cf25..793374c0dd7 100644
-- a/solr/core/src/java/org/apache/solr/schema/SchemaField.java
++ b/solr/core/src/java/org/apache/solr/schema/SchemaField.java
@@ -18,13 +18,13 @@
 package org.apache.solr.schema;
 
 import org.apache.solr.common.SolrException;
import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.StorableField;
 import org.apache.lucene.search.SortField;
 import org.apache.solr.search.QParser;
 
 import org.apache.solr.response.TextResponseWriter;
 
import java.util.List;
 import java.util.Map;
 import java.io.IOException;
 
@@ -79,6 +79,7 @@ public final class SchemaField extends FieldProperties {
 
   public boolean indexed() { return (properties & INDEXED)!=0; }
   public boolean stored() { return (properties & STORED)!=0; }
  public boolean hasDocValues() { return (properties & DOC_VALUES) != 0; }
   public boolean storeTermVector() { return (properties & STORE_TERMVECTORS)!=0; }
   public boolean storeTermPositions() { return (properties & STORE_TERMPOSITIONS)!=0; }
   public boolean storeTermOffsets() { return (properties & STORE_TERMOFFSETS)!=0; }
@@ -104,8 +105,8 @@ public final class SchemaField extends FieldProperties {
   public StorableField createField(Object val, float boost) {
     return type.createField(this,val,boost);
   }
  
  public StorableField[] createFields(Object val, float boost) {

  public List<StorableField> createFields(Object val, float boost) {
     return type.createFields(this,val,boost);
   }
 
@@ -148,9 +149,9 @@ public final class SchemaField extends FieldProperties {
    * @see FieldType#getSortField
    */
   public void checkSortability() throws SolrException {
    if (! indexed() ) {
    if (! (indexed() || hasDocValues()) ) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, 
                              "can not sort on unindexed field: " 
                              "can not sort on a field which is neither indexed nor has doc values: " 
                               + getName());
     }
     if ( multiValued() ) {
@@ -169,9 +170,9 @@ public final class SchemaField extends FieldProperties {
    * @see FieldType#getValueSource
    */
   public void checkFieldCacheSource(QParser parser) throws SolrException {
    if (! indexed() ) {
    if (! (indexed() || hasDocValues()) ) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, 
                              "can not use FieldCache on unindexed field: " 
                              "can not use FieldCache on a field which is neither indexed nor has doc values: " 
                               + getName());
     }
     if ( multiValued() ) {
diff --git a/solr/core/src/java/org/apache/solr/schema/SortableDoubleField.java b/solr/core/src/java/org/apache/solr/schema/SortableDoubleField.java
index 10c1140e4a1..65436f4bd6f 100644
-- a/solr/core/src/java/org/apache/solr/schema/SortableDoubleField.java
++ b/solr/core/src/java/org/apache/solr/schema/SortableDoubleField.java
@@ -131,6 +131,11 @@ class SortableDoubleFieldSource extends FieldCacheSource {
         return NumberUtils.double2sortableStr(readableValue);
       }
 
      @Override
      public boolean exists(int doc) {
        return termsIndex.getOrd(doc) >= 0;
      }

       @Override
       public float floatVal(int doc) {
         return (float)doubleVal(doc);
@@ -164,13 +169,7 @@ class SortableDoubleFieldSource extends FieldCacheSource {
 
       @Override
       public Object objectVal(int doc) {
        int ord=termsIndex.getOrd(doc);
        if (ord==-1) {
          return null;
        } else {
          termsIndex.lookupOrd(ord, spare);
          return NumberUtils.SortableStr2double(spare);
        }
        return exists(doc) ? doubleVal(doc) : null;
       }
 
       @Override
diff --git a/solr/core/src/java/org/apache/solr/schema/SortableFloatField.java b/solr/core/src/java/org/apache/solr/schema/SortableFloatField.java
index 9635e6f1bd5..69db7616b2f 100644
-- a/solr/core/src/java/org/apache/solr/schema/SortableFloatField.java
++ b/solr/core/src/java/org/apache/solr/schema/SortableFloatField.java
@@ -135,6 +135,11 @@ class SortableFloatFieldSource extends FieldCacheSource {
         return NumberUtils.float2sortableStr(readableValue);
       }
 
      @Override
      public boolean exists(int doc) {
        return termsIndex.getOrd(doc) >= 0;
      }

       @Override
       public float floatVal(int doc) {
         int ord=termsIndex.getOrd(doc);
@@ -173,13 +178,7 @@ class SortableFloatFieldSource extends FieldCacheSource {
 
       @Override
       public Object objectVal(int doc) {
        int ord=termsIndex.getOrd(doc);
        if (ord==-1) {
          return null;
        } else {
          termsIndex.lookupOrd(ord, spare);
          return NumberUtils.SortableStr2float(spare);
        }
        return exists(doc) ? floatVal(doc) : null;
       }
 
       @Override
diff --git a/solr/core/src/java/org/apache/solr/schema/SortableIntField.java b/solr/core/src/java/org/apache/solr/schema/SortableIntField.java
index 1a850aa0c08..cbcb913ea84 100644
-- a/solr/core/src/java/org/apache/solr/schema/SortableIntField.java
++ b/solr/core/src/java/org/apache/solr/schema/SortableIntField.java
@@ -142,6 +142,11 @@ class SortableIntFieldSource extends FieldCacheSource {
         return (float)intVal(doc);
       }
 
      @Override
      public boolean exists(int doc) {
        return termsIndex.getOrd(doc) >= 0;
      }

       @Override
       public int intVal(int doc) {
         int ord=termsIndex.getOrd(doc);
@@ -175,13 +180,7 @@ class SortableIntFieldSource extends FieldCacheSource {
 
       @Override
       public Object objectVal(int doc) {
        int ord=termsIndex.getOrd(doc);
        if (ord==-1) {
          return null;
        } else {
          termsIndex.lookupOrd(ord, spare);
          return NumberUtils.SortableStr2int(spare);
        }
        return exists(doc) ? intVal(doc) : null;
       }
 
       @Override
diff --git a/solr/core/src/java/org/apache/solr/schema/SortableLongField.java b/solr/core/src/java/org/apache/solr/schema/SortableLongField.java
index 8ce95ce5dcc..0e61eef6f91 100644
-- a/solr/core/src/java/org/apache/solr/schema/SortableLongField.java
++ b/solr/core/src/java/org/apache/solr/schema/SortableLongField.java
@@ -135,6 +135,11 @@ class SortableLongFieldSource extends FieldCacheSource {
         return NumberUtils.long2sortableStr(readableValue);
       }
 
      @Override
      public boolean exists(int doc) {
        return termsIndex.getOrd(doc) >= 0;
      }

       @Override
       public float floatVal(int doc) {
         return (float)longVal(doc);
@@ -168,13 +173,7 @@ class SortableLongFieldSource extends FieldCacheSource {
 
       @Override
       public Object objectVal(int doc) {
        int ord=termsIndex.getOrd(doc);
        if (ord==-1) {
          return null;
        } else {
          termsIndex.lookupOrd(ord, spare);
          return NumberUtils.SortableStr2long(spare);
        }
        return exists(doc) ? longVal(doc) : null;
       }
 
       @Override
diff --git a/solr/core/src/java/org/apache/solr/schema/StrField.java b/solr/core/src/java/org/apache/solr/schema/StrField.java
index a6e81650e77..4f370cd3f3b 100644
-- a/solr/core/src/java/org/apache/solr/schema/StrField.java
++ b/solr/core/src/java/org/apache/solr/schema/StrField.java
@@ -17,20 +17,43 @@
 
 package org.apache.solr.schema;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.StorableField;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.search.SortField;
import org.apache.lucene.index.GeneralField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StorableField;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.response.TextResponseWriter;
 import org.apache.solr.search.QParser;
 
import java.io.IOException;
/**
 *
 */
 public class StrField extends PrimitiveFieldType {

  @Override
  protected void init(IndexSchema schema, Map<String,String> args) {
    super.init(schema, args);
  }

  @Override
  public List<StorableField> createFields(SchemaField field, Object value,
      float boost) {
    if (field.hasDocValues()) {
      List<StorableField> fields = new ArrayList<StorableField>();
      fields.add(createField(field, value, boost));
      final BytesRef bytes = new BytesRef(value.toString());
      final Field docValuesField = new SortedDocValuesField(field.getName(), bytes);
      fields.add(docValuesField);
      return fields;
    } else {
      return Collections.singletonList(createField(field, value, boost));
    }
  }

   @Override
   public SortField getSortField(SchemaField field,boolean reverse) {
     return getStringSort(field,reverse);
@@ -51,6 +74,14 @@ public class StrField extends PrimitiveFieldType {
   public Object toObject(SchemaField sf, BytesRef term) {
     return term.utf8ToString();
   }

  @Override
  public void checkSchemaField(SchemaField field) {
    // change me when multi-valued doc values are supported
    if (field.hasDocValues() && !(field.isRequired() || field.getDefaultValue() != null)) {
      throw new IllegalStateException("Field " + this + " has doc values enabled, but has no default value and is not required");
    }
  }
 }
 
 
diff --git a/solr/core/src/java/org/apache/solr/schema/TrieDateField.java b/solr/core/src/java/org/apache/solr/schema/TrieDateField.java
index fddcdbba897..3ac0c78e5f0 100755
-- a/solr/core/src/java/org/apache/solr/schema/TrieDateField.java
++ b/solr/core/src/java/org/apache/solr/schema/TrieDateField.java
@@ -17,11 +17,13 @@
 
 package org.apache.solr.schema;
 
import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.solr.search.QParser;
import org.apache.solr.common.SolrException;
 import org.apache.solr.response.TextResponseWriter;
import org.apache.lucene.index.GeneralField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.StorableField;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.Query;
@@ -29,6 +31,7 @@ import org.apache.lucene.search.NumericRangeQuery;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.CharsRef;
 
import java.util.List;
 import java.util.Map;
 import java.util.Date;
 import java.io.IOException;
@@ -73,6 +76,10 @@ public class TrieDateField extends DateField {
     return wrappedField.getPrecisionStep();
   }
 
  @Override
  public NumericType getNumericType() {
    return wrappedField.getNumericType();
  }
 
   @Override
   public void write(TextResponseWriter writer, String name, StorableField f) throws IOException {
@@ -129,6 +136,11 @@ public class TrieDateField extends DateField {
     return wrappedField.createField(field, value, boost);
   }
 
  @Override
  public List<StorableField> createFields(SchemaField field, Object value, float boost) {
    return wrappedField.createFields(field, value, boost);
  }

   @Override
   public Query getRangeQuery(QParser parser, SchemaField field, String min, String max, boolean minInclusive, boolean maxInclusive) {
     return wrappedField.getRangeQuery(parser, field, min, max, minInclusive, maxInclusive);
@@ -141,4 +153,10 @@ public class TrieDateField extends DateField {
               max == null ? null : max.getTime(),
               minInclusive, maxInclusive);
   }

  @Override
  public void checkSchemaField(SchemaField field) {
    wrappedField.checkSchemaField(field);
  }

 }
diff --git a/solr/core/src/java/org/apache/solr/schema/TrieField.java b/solr/core/src/java/org/apache/solr/schema/TrieField.java
index afe4da286b0..59c20236d1f 100644
-- a/solr/core/src/java/org/apache/solr/schema/TrieField.java
++ b/solr/core/src/java/org/apache/solr/schema/TrieField.java
@@ -17,7 +17,10 @@
 package org.apache.solr.schema;
 
 import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
 import java.util.Date;
import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 
@@ -29,13 +32,17 @@ import org.apache.lucene.document.FieldType.NumericType;
 import org.apache.lucene.document.FloatField;
 import org.apache.lucene.document.IntField;
 import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
 import org.apache.lucene.index.StorableField;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.DoubleFieldSource;
 import org.apache.lucene.queries.function.valuesource.FloatFieldSource;
 import org.apache.lucene.queries.function.valuesource.IntFieldSource;
 import org.apache.lucene.queries.function.valuesource.LongFieldSource;
import org.apache.lucene.search.*;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.CharsRef;
 import org.apache.lucene.util.NumericUtils;
@@ -101,8 +108,7 @@ public class TrieField extends PrimitiveFieldType {
                 "Invalid type specified in schema.xml for field: " + args.get("name"), e);
       }
     }
  
    

     CharFilterFactory[] filterFactories = new CharFilterFactory[0];
     TokenFilterFactory[] tokenFilterFactories = new TokenFilterFactory[0];
     analyzer = new TokenizerChain(filterFactories, new TrieTokenizerFactory(type, precisionStep), tokenFilterFactories);
@@ -236,6 +242,23 @@ public class TrieField extends PrimitiveFieldType {
     return type;
   }
 
  @Override
  public NumericType getNumericType() {
    switch (type) {
      case INTEGER:
        return NumericType.INT;
      case LONG:
      case DATE:
        return NumericType.LONG;
      case FLOAT:
        return NumericType.FLOAT;
      case DOUBLE:
        return NumericType.DOUBLE;
      default:
        throw new AssertionError();
    }
  }

   @Override
   public Query getRangeQuery(QParser parser, SchemaField field, String min, String max, boolean minInclusive, boolean maxInclusive) {
     int ps = precisionStep;
@@ -473,8 +496,9 @@ public class TrieField extends PrimitiveFieldType {
   public StorableField createField(SchemaField field, Object value, float boost) {
     boolean indexed = field.indexed();
     boolean stored = field.stored();
    boolean docValues = field.hasDocValues();
 
    if (!indexed && !stored) {
    if (!indexed && !stored && !docValues) {
       if (log.isTraceEnabled())
         log.trace("Ignoring unindexed/unstored field: " + field);
       return null;
@@ -549,6 +573,28 @@ public class TrieField extends PrimitiveFieldType {
     return f;
   }
 
  @Override
  public List<StorableField> createFields(SchemaField sf, Object value, float boost) {
    if (sf.hasDocValues()) {
      List<StorableField> fields = new ArrayList<StorableField>();
      final StorableField field = createField(sf, value, boost);
      fields.add(field);
      final long bits;
      if (field.numericValue() instanceof Integer || field.numericValue() instanceof Long) {
        bits = field.numericValue().longValue();
      } else if (field.numericValue() instanceof Float) {
        bits = Float.floatToIntBits(field.numericValue().floatValue());
      } else {
        assert field.numericValue() instanceof Double;
        bits = Double.doubleToLongBits(field.numericValue().doubleValue());
      }
      fields.add(new NumericDocValuesField(sf.getName(), bits));
      return fields;
    } else {
      return Collections.singletonList(createField(sf, value, boost));
    }
  }

   public enum TrieTypes {
     INTEGER,
     LONG,
@@ -586,6 +632,13 @@ public class TrieField extends PrimitiveFieldType {
     }
     return null;
   }

  @Override
  public void checkSchemaField(final SchemaField field) {
    if (field.hasDocValues() && !(field.isRequired() || field.getDefaultValue() != null)) {
      throw new IllegalStateException("Field " + this + " has doc values enabled, but has no default value and is not required");
    }
  }
 }
 
 class TrieDateFieldSource extends LongFieldSource {
@@ -605,14 +658,20 @@ class TrieDateFieldSource extends LongFieldSource {
   }
 
   @Override
  public Object longToObject(long val) {
  public Date longToObject(long val) {
     return new Date(val);
   }
 
  @Override
  public String longToString(long val) {
    return TrieField.dateField.toExternal(longToObject(val));
  }

   @Override
   public long externalToLong(String extVal) {
     return TrieField.dateField.parseMath(null, extVal).getTime();
   }

 }
 
 
diff --git a/solr/core/src/java/org/apache/solr/schema/UUIDField.java b/solr/core/src/java/org/apache/solr/schema/UUIDField.java
index df45a705944..33c95b72c51 100644
-- a/solr/core/src/java/org/apache/solr/schema/UUIDField.java
++ b/solr/core/src/java/org/apache/solr/schema/UUIDField.java
@@ -22,8 +22,6 @@ import java.util.Locale;
 import java.util.Map;
 import java.util.UUID;
 
import org.apache.lucene.index.GeneralField;
import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.StorableField;
 import org.apache.lucene.search.SortField;
 import org.apache.solr.common.SolrException;
diff --git a/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java b/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java
index 1729951020b..6ce5c9ede38 100644
-- a/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java
++ b/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java
@@ -23,10 +23,7 @@ import java.util.List;
 
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.StorableField;
import org.apache.solr.common.SolrDocument;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.SolrInputField;
@@ -58,33 +55,19 @@ public class DocumentBuilder {
     // we don't check for a null val ourselves because a solr.FieldType
     // might actually want to map it to something.  If createField()
     // returns null, then we don't store the field.
    if (sfield.isPolyField()) {
      StorableField[] fields = sfield.createFields(val, boost);
      if (fields.length > 0) {
        if (!sfield.multiValued()) {
          String oldValue = map.put(sfield.getName(), val);
          if (oldValue != null) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "ERROR: multiple values encountered for non multiValued field " + sfield.getName()
                    + ": first='" + oldValue + "' second='" + val + "'");
          }
        }
        // Add each field
        for (StorableField field : fields) {
          doc.add((Field) field);
    List<StorableField> fields = sfield.createFields(val, boost);
    if (!fields.isEmpty()) {
      if (!sfield.multiValued()) {
        String oldValue = map.put(sfield.getName(), val);
        if (oldValue != null) {
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "ERROR: multiple values encountered for non multiValued field " + sfield.getName()
                  + ": first='" + oldValue + "' second='" + val + "'");
         }
       }
    } else {
      StorableField field = sfield.createField(val, boost);
      if (field != null) {
        if (!sfield.multiValued()) {
          String oldValue = map.put(sfield.getName(), val);
          if (oldValue != null) {
            throw new SolrException( SolrException.ErrorCode.BAD_REQUEST,"ERROR: multiple values encountered for non multiValued field " + sfield.getName()
                    + ": first='" + oldValue + "' second='" + val + "'");
          }
        }
      // Add each field
      for (StorableField field : fields) {
        doc.add((Field) field);
       }
      doc.add((Field) field);
     }
 
   }
@@ -192,14 +175,8 @@ public class DocumentBuilder {
 
 
   private static void addField(Document doc, SchemaField field, Object val, float boost) {
    if (field.isPolyField()) {
      StorableField[] farr = field.getType().createFields(field, val, boost);
      for (StorableField f : farr) {
        if (f != null) doc.add((Field) f); // null fields are not added
      }
    } else {
      StorableField f = field.createField(val, boost);
      if (f != null) doc.add((Field) f);  // null fields are not added
    for (StorableField f : field.getType().createFields(field, val, boost)) {
      if (f != null) doc.add((Field) f); // null fields are not added
     }
   }
   
diff --git a/solr/core/src/test-files/solr/collection1/conf/bad-schema-docValues-not-required-no-default.xml b/solr/core/src/test-files/solr/collection1/conf/bad-schema-docValues-not-required-no-default.xml
new file mode 100644
index 00000000000..deadd9ac68b
-- /dev/null
++ b/solr/core/src/test-files/solr/collection1/conf/bad-schema-docValues-not-required-no-default.xml
@@ -0,0 +1,33 @@
<?xml version="1.0" ?>
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<schema name="bad-schema-docValues-not-required-no-default" version="1.0">
  <types>
    <fieldType name="string" class="solr.StrField" />
 </types>


 <fields>
   <!-- docValues must be required or have a default value -->
   <field name="id" type="string" docValues="true" multiValued="false"/>
 </fields>

 <defaultSearchField>id</defaultSearchField>
 <uniqueKey>id</uniqueKey>

</schema>
diff --git a/solr/core/src/test-files/solr/collection1/conf/bad-schema-unsupported-docValues.xml b/solr/core/src/test-files/solr/collection1/conf/bad-schema-unsupported-docValues.xml
new file mode 100644
index 00000000000..5f4d69a31a7
-- /dev/null
++ b/solr/core/src/test-files/solr/collection1/conf/bad-schema-unsupported-docValues.xml
@@ -0,0 +1,30 @@
<?xml version="1.0" ?>
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<schema name="bad-schema-docValues-unsupported" version="1.5">
  <types>
    <fieldType name="binary" class="solr.BinaryField" />
 </types>


 <fields>
   <!-- change the type if BinaryField gets doc values -->
   <field name="id" type="binary" docValues="true"/>
 </fields>

</schema>
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema-docValues.xml b/solr/core/src/test-files/solr/collection1/conf/schema-docValues.xml
new file mode 100644
index 00000000000..63d87997402
-- /dev/null
++ b/solr/core/src/test-files/solr/collection1/conf/schema-docValues.xml
@@ -0,0 +1,74 @@
<?xml version="1.0" ?>
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<!-- The Solr schema file. This file should be named "schema.xml" and
     should be located where the classloader for the Solr webapp can find it.

     This schema is used for testing, and as such has everything and the
     kitchen sink thrown in. See example/solr/conf/schema.xml for a
     more concise example.

  -->

<schema name="schema-docValues" version="1.5">
  <types>

    <!-- field type definitions... note that the "name" attribute is
         just a label to be used by field definitions.  The "class"
         attribute and any other attributes determine the real type and
         behavior of the fieldtype.
      -->

    <!-- numeric field types that store and index the text
         value verbatim (and hence don't sort correctly or support range queries.)
         These are provided more for backward compatability, allowing one
         to create a schema that matches an existing lucene index.
    -->
    <fieldType name="int" class="solr.TrieIntField" precisionStep="0" omitNorms="true" positionIncrementGap="0"/>
    <fieldType name="float" class="solr.TrieFloatField" precisionStep="0" omitNorms="true" positionIncrementGap="0"/>
    <fieldType name="long" class="solr.TrieLongField" precisionStep="0" omitNorms="true" positionIncrementGap="0"/>
    <fieldType name="double" class="solr.TrieDoubleField" precisionStep="0" omitNorms="true" positionIncrementGap="0"/>
    <!-- format for date is 1995-12-31T23:59:59.999Z and only the fractional
         seconds part (.999) is optional.
      -->
    <fieldtype name="date" class="solr.TrieDateField" precisionStep="0" omitNorms="true" positionIncrementGap="0"/>

    <fieldtype name="boolean" class="solr.BoolField" />
    <fieldtype name="string" class="solr.StrField" />

    <fieldType name="uuid" class="solr.UUIDField" />

  </types>


  <fields>

    <field name="id" type="string" required="true" />

    <field name="floatdv" type="float" indexed="false" stored="false" docValues="true" default="1" />
    <field name="intdv" type="int" indexed="false" stored="false" docValues="true" default="2" />
    <field name="doubledv" type="double" indexed="false" stored="false" docValues="true" default="3" />
    <field name="longdv" type="long" indexed="false" stored="false" docValues="true" default="4" />
    <field name="datedv" type="date" indexed="false" stored="false" docValues="true" default="1995-12-31T23:59:59.999Z" />

    <field name="stringdv" type="string" indexed="false" stored="false" docValues="true" default="solr" />
  </fields>

  <uniqueKey>id</uniqueKey>

</schema>
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema.xml b/solr/core/src/test-files/solr/collection1/conf/schema.xml
index 417d7dbf5ca..cfa31d1f1d2 100644
-- a/solr/core/src/test-files/solr/collection1/conf/schema.xml
++ b/solr/core/src/test-files/solr/collection1/conf/schema.xml
@@ -567,7 +567,7 @@
 
    <field name="textgap" type="textgap" indexed="true" stored="true"/>
 
   <field name="timestamp" type="date" indexed="true" stored="true" default="NOW" multiValued="false"/>
   <field name="timestamp" type="date" indexed="true" stored="true" docValues="true" default="NOW" multiValued="false"/>
    <field name="multiDefault" type="string" indexed="true" stored="true" default="muLti-Default" multiValued="true"/>
    <field name="intDefault" type="int" indexed="true" stored="true" default="42" multiValued="false"/>
 
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema_codec.xml b/solr/core/src/test-files/solr/collection1/conf/schema_codec.xml
index e28cec73722..15074809892 100644
-- a/solr/core/src/test-files/solr/collection1/conf/schema_codec.xml
++ b/solr/core/src/test-files/solr/collection1/conf/schema_codec.xml
@@ -20,18 +20,29 @@
   <fieldType name="string_pulsing" class="solr.StrField" postingsFormat="Pulsing41"/>
   <fieldType name="string_simpletext" class="solr.StrField" postingsFormat="SimpleText"/>
   <fieldType name="string_standard" class="solr.StrField" postingsFormat="Lucene41"/>
    <fieldType name="string" class="solr.StrField" />
  

  <fieldType name="string_disk" class="solr.StrField" docValuesFormat="Disk" />
  <fieldType name="string_memory" class="solr.StrField" docValuesFormat="Lucene42" />

  <fieldType name="string" class="solr.StrField" />

  </types>
  <fields>
    <field name="string_pulsing_f" type="string_pulsing" indexed="true" stored="true" />
    <field name="string_simpletext_f" type="string_simpletext" indexed="true" stored="true" />
    <field name="string_standard_f" type="string_standard" indexed="true" stored="true" />
   <field name="string_f" type="string" indexed="true" stored="true" />

   <field name="string_disk_f" type="string_disk" indexed="false" stored="false" docValues="true" default="" />
   <field name="string_memory_f" type="string_memory" indexed="false" stored="false" docValues="true" default="" />

   <field name="string_f" type="string" indexed="true" stored="true" docValues="true" required="true"/>

    <dynamicField name="*_simple" type="string_simpletext"  indexed="true" stored="true"/>
    <dynamicField name="*_pulsing" type="string_pulsing"  indexed="true" stored="true"/>
    <dynamicField name="*_standard" type="string_standard"  indexed="true" stored="true"/>
   

   <dynamicField name="*_disk" type="string_disk" indexed="false" stored="false" docValues="true" default="" />
   <dynamicField name="*_memory" type="string_memory" indexed="false" stored="false" docValues="true" default="" />
  </fields>
   <defaultSearchField>string_f</defaultSearchField>
  <uniqueKey>string_f</uniqueKey>
diff --git a/solr/core/src/test/org/apache/solr/core/TestCodecSupport.java b/solr/core/src/test/org/apache/solr/core/TestCodecSupport.java
index a49fbf98397..049723fb739 100644
-- a/solr/core/src/test/org/apache/solr/core/TestCodecSupport.java
++ b/solr/core/src/test/org/apache/solr/core/TestCodecSupport.java
@@ -20,6 +20,7 @@ package org.apache.solr.core;
 import java.util.Map;
 
 import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.perfield.PerFieldDocValuesFormat;
 import org.apache.lucene.codecs.perfield.PerFieldPostingsFormat;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.schema.SchemaField;
@@ -47,7 +48,21 @@ public class TestCodecSupport extends SolrTestCaseJ4 {
     assertEquals("Lucene41", format.getPostingsFormatForField(schemaField.getName()).getName());
   }
 
  public void testDynamicFields() {
  public void testDocValuesFormats() {
    Codec codec = h.getCore().getCodec();
    Map<String, SchemaField> fields = h.getCore().getSchema().getFields();
    SchemaField schemaField = fields.get("string_disk_f");
    PerFieldDocValuesFormat format = (PerFieldDocValuesFormat) codec.docValuesFormat();
    assertEquals("Disk", format.getDocValuesFormatForField(schemaField.getName()).getName());
    schemaField = fields.get("string_memory_f");
    assertEquals("Lucene42",
        format.getDocValuesFormatForField(schemaField.getName()).getName());
    schemaField = fields.get("string_f");
    assertEquals("Lucene42",
        format.getDocValuesFormatForField(schemaField.getName()).getName());
  }

  public void testDynamicFieldsPostingsFormats() {
     Codec codec = h.getCore().getCodec();
     PerFieldPostingsFormat format = (PerFieldPostingsFormat) codec.postingsFormat();
 
@@ -59,6 +74,16 @@ public class TestCodecSupport extends SolrTestCaseJ4 {
     assertEquals("Lucene41", format.getPostingsFormatForField("bar_standard").getName());
   }
 
  public void testDynamicFieldsDocValuesFormats() {
    Codec codec = h.getCore().getCodec();
    PerFieldDocValuesFormat format = (PerFieldDocValuesFormat) codec.docValuesFormat();

    assertEquals("Disk", format.getDocValuesFormatForField("foo_disk").getName());
    assertEquals("Disk", format.getDocValuesFormatForField("bar_disk").getName());
    assertEquals("Lucene42", format.getDocValuesFormatForField("foo_memory").getName());
    assertEquals("Lucene42", format.getDocValuesFormatForField("bar_memory").getName());
  }

   public void testUnknownField() {
     Codec codec = h.getCore().getCodec();
     PerFieldPostingsFormat format = (PerFieldPostingsFormat) codec.postingsFormat();
diff --git a/solr/core/src/test/org/apache/solr/handler/component/StatsComponentTest.java b/solr/core/src/test/org/apache/solr/handler/component/StatsComponentTest.java
index 78cf362bdc3..0fa83fea53e 100644
-- a/solr/core/src/test/org/apache/solr/handler/component/StatsComponentTest.java
++ b/solr/core/src/test/org/apache/solr/handler/component/StatsComponentTest.java
@@ -75,6 +75,7 @@ public class StatsComponentTest extends AbstractSolrTestCase {
   public void doTestFieldStatisticsResult(String f) throws Exception {
     assertU(adoc("id", "1", f, "-10"));
     assertU(adoc("id", "2", f, "-20"));
    assertU(commit());
     assertU(adoc("id", "3", f, "-30"));
     assertU(adoc("id", "4", f, "-40"));
     assertU(commit());
@@ -205,6 +206,7 @@ public class StatsComponentTest extends AbstractSolrTestCase {
   public void doTestFieldStatisticsMissingResult(String f) throws Exception {
     assertU(adoc("id", "1", f, "-10"));
     assertU(adoc("id", "2", f, "-20"));
    assertU(commit());
     assertU(adoc("id", "3"));
     assertU(adoc("id", "4", f, "-40"));
     assertU(commit());
@@ -224,6 +226,7 @@ public class StatsComponentTest extends AbstractSolrTestCase {
   public void doTestFacetStatisticsResult(String f) throws Exception {
     assertU(adoc("id", "1", f, "10", "active_s", "true",  "other_s", "foo"));
     assertU(adoc("id", "2", f, "20", "active_s", "true",  "other_s", "bar"));
    assertU(commit());
     assertU(adoc("id", "3", f, "30", "active_s", "false", "other_s", "foo"));
     assertU(adoc("id", "4", f, "40", "active_s", "false", "other_s", "foo"));
     assertU(commit());
@@ -257,6 +260,7 @@ public class StatsComponentTest extends AbstractSolrTestCase {
   public void doTestFacetStatisticsMissingResult(String f) throws Exception {
       assertU(adoc("id", "1", f, "10", "active_s", "true"));
       assertU(adoc("id", "2", f, "20", "active_s", "true"));
      assertU(commit());
       assertU(adoc("id", "3", "active_s", "false"));
       assertU(adoc("id", "4", f, "40", "active_s", "false"));
       assertU(commit());
@@ -288,6 +292,7 @@ public class StatsComponentTest extends AbstractSolrTestCase {
     SolrCore core = h.getCore();
     assertU(adoc("id", "1"));
     assertU(adoc("id", "2"));
    assertU(commit());
     assertU(adoc("id", "3"));
     assertU(adoc("id", "4"));
     assertU(commit());
@@ -307,6 +312,7 @@ public class StatsComponentTest extends AbstractSolrTestCase {
     SolrCore core = h.getCore();
     assertU(adoc("id", "1"));
     assertU(adoc("id", "2"));
    assertU(commit());
     assertU(adoc("id", "3"));
     assertU(adoc("id", "4"));
     assertU(commit());
@@ -328,6 +334,7 @@ public class StatsComponentTest extends AbstractSolrTestCase {
 
     assertU(adoc("id", "1"));
     assertU(adoc("id", "2"));
    assertU(commit());
     assertU(adoc("id", "3"));
     assertU(commit());
 
@@ -347,6 +354,7 @@ public class StatsComponentTest extends AbstractSolrTestCase {
     SchemaField foo_ss = core.getSchema().getField("foo_ss");
 
     assertU(adoc("id", "1", "active_i", "1", "foo_ss", "aa" ));
    assertU(commit());
     assertU(adoc("id", "2", "active_i", "1", "foo_ss", "bb" ));
     assertU(adoc("id", "3", "active_i", "5", "foo_ss", "aa" ));
     assertU(commit());
diff --git a/solr/core/src/test/org/apache/solr/schema/BadIndexSchemaTest.java b/solr/core/src/test/org/apache/solr/schema/BadIndexSchemaTest.java
index 6c9e6fbf18a..746fd48f1b1 100644
-- a/solr/core/src/test/org/apache/solr/schema/BadIndexSchemaTest.java
++ b/solr/core/src/test/org/apache/solr/schema/BadIndexSchemaTest.java
@@ -19,8 +19,6 @@ package org.apache.solr.schema;
 
 import org.apache.solr.core.AbstractBadConfigTestBase;
 
import java.util.regex.Pattern;

 public class BadIndexSchemaTest extends AbstractBadConfigTestBase {
 
   private void doTest(final String schema, final String errString) 
@@ -83,5 +81,12 @@ public class BadIndexSchemaTest extends AbstractBadConfigTestBase {
     doTest("bad-schema-codec-global-vs-ft-mismatch.xml", "codec does not support");
   }
 
  public void testDocValuesNotRequiredNoDefault() throws Exception {
    doTest("bad-schema-docValues-not-required-no-default.xml", "has no default value and is not required");
  }

  public void testDocValuesUnsupported() throws Exception {
    doTest("bad-schema-unsupported-docValues.xml", "does not support doc values");
  }
 
 }
diff --git a/solr/core/src/test/org/apache/solr/schema/CurrencyFieldTest.java b/solr/core/src/test/org/apache/solr/schema/CurrencyFieldTest.java
index 04f363730ba..4c3af76a9cc 100644
-- a/solr/core/src/test/org/apache/solr/schema/CurrencyFieldTest.java
++ b/solr/core/src/test/org/apache/solr/schema/CurrencyFieldTest.java
@@ -23,6 +23,7 @@ import org.junit.BeforeClass;
 import org.junit.Ignore;
 import org.junit.Test;
 
import java.util.List;
 import java.util.Random;
 import java.util.Set;
 
@@ -71,18 +72,18 @@ public class CurrencyFieldTest extends SolrTestCaseJ4 {
     FieldType tmp = amount.getType();
     assertTrue(tmp instanceof CurrencyField);
     String currencyValue = "1.50,EUR";
    StorableField[] fields = amount.createFields(currencyValue, 2);
    assertEquals(fields.length, 3);
    List<StorableField> fields = amount.createFields(currencyValue, 2);
    assertEquals(fields.size(), 3);
 
     // First field is currency code, second is value, third is stored.
     for (int i = 0; i < 3; i++) {
      boolean hasValue = fields[i].readerValue() != null
              || fields[i].numericValue() != null
              || fields[i].stringValue() != null;
      assertTrue("Doesn't have a value: " + fields[i], hasValue);
      boolean hasValue = fields.get(i).readerValue() != null
              || fields.get(i).numericValue() != null
              || fields.get(i).stringValue() != null;
      assertTrue("Doesn't have a value: " + fields.get(i), hasValue);
     }
 
    assertEquals(schema.getFieldTypeByName("string").toExternal(fields[2]), "1.50,EUR");
    assertEquals(schema.getFieldTypeByName("string").toExternal(fields.get(2)), "1.50,EUR");
     
     // A few tests on the provider directly
     ExchangeRateProvider p = ((CurrencyField) tmp).getProvider();
diff --git a/solr/core/src/test/org/apache/solr/schema/DocValuesTest.java b/solr/core/src/test/org/apache/solr/schema/DocValuesTest.java
new file mode 100644
index 00000000000..374abf903b5
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/schema/DocValuesTest.java
@@ -0,0 +1,230 @@
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

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FieldInfo.DocValuesType;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.junit.BeforeClass;

public class DocValuesTest extends SolrTestCaseJ4 {

  @BeforeClass
  public static void beforeTests() throws Exception {
    initCore("solrconfig-basic.xml", "schema-docValues.xml");
  }

  public void setUp() throws Exception {
    super.setUp();
    assertU(delQ("*:*"));
  }

  public void testDocValues() throws IOException {
    assertU(adoc("id", "1"));
    commit();
    SolrCore core = h.getCoreInc();
    try {
      final RefCounted<SolrIndexSearcher> searcherRef = core.openNewSearcher(true, true);
      final SolrIndexSearcher searcher = searcherRef.get();
      try {
        final AtomicReader reader = searcher.getAtomicReader();
        assertEquals(1, reader.numDocs());
        final FieldInfos infos = reader.getFieldInfos();
        assertEquals(DocValuesType.NUMERIC, infos.fieldInfo("floatdv").getDocValuesType());
        assertEquals(DocValuesType.NUMERIC, infos.fieldInfo("intdv").getDocValuesType());
        assertEquals(DocValuesType.NUMERIC, infos.fieldInfo("doubledv").getDocValuesType());
        assertEquals(DocValuesType.NUMERIC, infos.fieldInfo("longdv").getDocValuesType());
        assertEquals(DocValuesType.SORTED, infos.fieldInfo("stringdv").getDocValuesType());

        assertEquals((long) Float.floatToIntBits(1), reader.getNumericDocValues("floatdv").get(0));
        assertEquals(2L, reader.getNumericDocValues("intdv").get(0));
        assertEquals(Double.doubleToLongBits(3), reader.getNumericDocValues("doubledv").get(0));
        assertEquals(4L, reader.getNumericDocValues("longdv").get(0));

        final IndexSchema schema = core.getSchema();
        final SchemaField floatDv = schema.getField("floatdv");
        final SchemaField intDv = schema.getField("intdv");
        final SchemaField doubleDv = schema.getField("doubledv");
        final SchemaField longDv = schema.getField("longdv");

        FunctionValues values = floatDv.getType().getValueSource(floatDv, null).getValues(null, searcher.getAtomicReader().leaves().get(0));
        assertEquals(1f, values.floatVal(0), 0f);
        assertEquals(1f, values.objectVal(0));
        values = intDv.getType().getValueSource(intDv, null).getValues(null, searcher.getAtomicReader().leaves().get(0));
        assertEquals(2, values.intVal(0));
        assertEquals(2, values.objectVal(0));
        values = doubleDv.getType().getValueSource(doubleDv, null).getValues(null, searcher.getAtomicReader().leaves().get(0));
        assertEquals(3d, values.doubleVal(0), 0d);
        assertEquals(3d, values.objectVal(0));
        values = longDv.getType().getValueSource(longDv, null).getValues(null, searcher.getAtomicReader().leaves().get(0));
        assertEquals(4L, values.longVal(0));
        assertEquals(4L, values.objectVal(0));
      } finally {
        searcherRef.decref();
      }
    } finally {
      core.close();
    }
  }

  public void testDocValuesSorting() {
    assertU(adoc("id", "1", "floatdv", "2", "intdv", "3", "doubledv", "4", "longdv", "5", "datedv", "1995-12-31T23:59:59.999Z", "stringdv", "b"));
    assertU(adoc("id", "2", "floatdv", "5", "intdv", "4", "doubledv", "3", "longdv", "2", "datedv", "1997-12-31T23:59:59.999Z", "stringdv", "a"));
    assertU(adoc("id", "3", "floatdv", "3", "intdv", "1", "doubledv", "2", "longdv", "1", "datedv", "1996-12-31T23:59:59.999Z", "stringdv", "c"));
    assertU(adoc("id", "4"));
    assertU(commit());
    assertQ(req("q", "*:*", "sort", "floatdv desc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='2']");
    assertQ(req("q", "*:*", "sort", "intdv desc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='2']");
    assertQ(req("q", "*:*", "sort", "doubledv desc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='1']");
    assertQ(req("q", "*:*", "sort", "longdv desc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='1']");
    assertQ(req("q", "*:*", "sort", "datedv desc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='2']");
    assertQ(req("q", "*:*", "sort", "stringdv desc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='4']");
    assertQ(req("q", "*:*", "sort", "floatdv asc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='4']");
    assertQ(req("q", "*:*", "sort", "intdv asc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='3']");
    assertQ(req("q", "*:*", "sort", "doubledv asc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='3']");
    assertQ(req("q", "*:*", "sort", "longdv asc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='3']");
    assertQ(req("q", "*:*", "sort", "datedv asc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='1']");
    assertQ(req("q", "*:*", "sort", "stringdv asc", "rows", "1", "fl", "id"),
        "//str[@name='id'][.='2']");
  }

  public void testDocValuesFaceting() {
    for (int i = 0; i < 50; ++i) {
      assertU(adoc("id", "" + i));
    }
    for (int i = 0; i < 50; ++i) {
      if (rarely()) {
        commit(); // to have several segments
      }
      assertU(adoc("id", "1000" + i, "floatdv", "" + i, "intdv", "" + i, "doubledv", "" + i, "longdv", "" + i, "datedv", (1900+i) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i));
    }
    assertU(commit());
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "longdv", "facet.sort", "count", "facet.limit", "1"),
        "//lst[@name='longdv']/int[@name='4'][.='51']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "longdv", "facet.sort", "count", "facet.offset", "1", "facet.limit", "1"),
        "//lst[@name='longdv']/int[@name='0'][.='1']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "longdv", "facet.sort", "index", "facet.offset", "33", "facet.limit", "1", "facet.mincount", "1"),
        "//lst[@name='longdv']/int[@name='33'][.='1']");

    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "floatdv", "facet.sort", "count", "facet.limit", "1"),
        "//lst[@name='floatdv']/int[@name='1.0'][.='51']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "floatdv", "facet.sort", "count", "facet.offset", "1", "facet.limit", "-1", "facet.mincount", "1"),
        "//lst[@name='floatdv']/int[@name='0.0'][.='1']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "floatdv", "facet.sort", "index", "facet.offset", "33", "facet.limit", "1", "facet.mincount", "1"),
        "//lst[@name='floatdv']/int[@name='33.0'][.='1']");

    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "doubledv", "facet.sort", "count", "facet.limit", "1"),
        "//lst[@name='doubledv']/int[@name='3.0'][.='51']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "doubledv", "facet.sort", "count", "facet.offset", "1", "facet.limit", "-1", "facet.mincount", "1"),
        "//lst[@name='doubledv']/int[@name='0.0'][.='1']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "doubledv", "facet.sort", "index", "facet.offset", "33", "facet.limit", "1", "facet.mincount", "1"),
        "//lst[@name='doubledv']/int[@name='33.0'][.='1']");

    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "intdv", "facet.sort", "count", "facet.limit", "1"),
        "//lst[@name='intdv']/int[@name='2'][.='51']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "intdv", "facet.sort", "count", "facet.offset", "1", "facet.limit", "-1", "facet.mincount", "1"),
        "//lst[@name='intdv']/int[@name='0'][.='1']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "intdv", "facet.sort", "index", "facet.offset", "33", "facet.limit", "1", "facet.mincount", "1"),
        "//lst[@name='intdv']/int[@name='33'][.='1']");

    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "datedv", "facet.sort", "count", "facet.limit", "1"),
        "//lst[@name='datedv']/int[@name='1995-12-31T23:59:59.999Z'][.='50']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "datedv", "facet.sort", "count", "facet.offset", "1", "facet.limit", "-1", "facet.mincount", "1"),
        "//lst[@name='datedv']/int[@name='1900-12-31T23:59:59.999Z'][.='1']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "datedv", "facet.sort", "index", "facet.offset", "33", "facet.limit", "1", "facet.mincount", "1"),
        "//lst[@name='datedv']/int[@name='1933-12-31T23:59:59.999Z'][.='1']");
  }

  public void testDocValuesStats() {
    for (int i = 0; i < 50; ++i) {
      assertU(adoc("id", "1000" + i, "floatdv", "" + i%2, "intdv", "" + i%3, "doubledv", "" + i%4, "longdv", "" + i%5, "datedv", (1900+i%6) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i%7));
      if (rarely()) {
        commit(); // to have several segments
      }
    }
    assertU(commit());

    assertQ(req("q", "*:*", "stats", "true", "rows", "0", "stats.field", "stringdv"),
        "//str[@name='min'][.='abc0']",
        "//str[@name='max'][.='abc6']",
        "//long[@name='count'][.='50']");

    assertQ(req("q", "*:*", "stats", "true", "rows", "0", "stats.field", "floatdv"),
        "//double[@name='min'][.='0.0']",
        "//double[@name='max'][.='1.0']",
        "//long[@name='count'][.='50']",
        "//double[@name='sum'][.='25.0']",
        "//double[@name='mean'][.='0.5']");

    assertQ(req("q", "*:*", "stats", "true", "rows", "0", "stats.field", "intdv"),
        "//double[@name='min'][.='0.0']",
        "//double[@name='max'][.='2.0']",
        "//long[@name='count'][.='50']",
        "//double[@name='sum'][.='49.0']");

    assertQ(req("q", "*:*", "stats", "true", "rows", "0", "stats.field", "doubledv"),
        "//double[@name='min'][.='0.0']",
        "//double[@name='max'][.='3.0']",
        "//long[@name='count'][.='50']",
        "//double[@name='sum'][.='73.0']");

    assertQ(req("q", "*:*", "stats", "true", "rows", "0", "stats.field", "longdv"),
        "//double[@name='min'][.='0.0']",
        "//double[@name='max'][.='4.0']",
        "//long[@name='count'][.='50']",
        "//double[@name='sum'][.='100.0']",
        "//double[@name='mean'][.='2.0']");

    assertQ(req("q", "*:*", "stats", "true", "rows", "0", "stats.field", "datedv"),
        "//date[@name='min'][.='1900-12-31T23:59:59.999Z']",
        "//date[@name='max'][.='1905-12-31T23:59:59.999Z']",
        "//long[@name='count'][.='50']");

    assertQ(req("q", "*:*", "stats", "true", "rows", "0", "stats.field", "floatdv", "stats.facet", "intdv"),
        "//lst[@name='intdv']/lst[@name='0']/long[@name='count'][.='17']",
        "//lst[@name='intdv']/lst[@name='1']/long[@name='count'][.='17']",
        "//lst[@name='intdv']/lst[@name='2']/long[@name='count'][.='16']");

    assertQ(req("q", "*:*", "stats", "true", "rows", "0", "stats.field", "floatdv", "stats.facet", "datedv"),
        "//lst[@name='datedv']/lst[@name='1900-12-31T23:59:59.999Z']/long[@name='count'][.='9']",
        "//lst[@name='datedv']/lst[@name='1901-12-31T23:59:59.999Z']/long[@name='count'][.='9']",
        "//lst[@name='datedv']/lst[@name='1902-12-31T23:59:59.999Z']/long[@name='count'][.='8']",
        "//lst[@name='datedv']/lst[@name='1903-12-31T23:59:59.999Z']/long[@name='count'][.='8']",
        "//lst[@name='datedv']/lst[@name='1904-12-31T23:59:59.999Z']/long[@name='count'][.='8']",
        "//lst[@name='datedv']/lst[@name='1905-12-31T23:59:59.999Z']/long[@name='count'][.='8']");
  }

}
diff --git a/solr/core/src/test/org/apache/solr/schema/PolyFieldTest.java b/solr/core/src/test/org/apache/solr/schema/PolyFieldTest.java
index 87973e36293..d92487a71f8 100644
-- a/solr/core/src/test/org/apache/solr/schema/PolyFieldTest.java
++ b/solr/core/src/test/org/apache/solr/schema/PolyFieldTest.java
@@ -16,8 +16,9 @@ package org.apache.solr.schema;
  * limitations under the License.
  */
 
import java.util.List;

 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.StorableField;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
@@ -84,14 +85,14 @@ public class PolyFieldTest extends SolrTestCaseJ4 {
     assertEquals(pt.getDimension(), 2);
     double[] xy = new double[]{35.0, -79.34};
     String point = xy[0] + "," + xy[1];
    StorableField[] fields = home.createFields(point, 2);
    assertEquals(fields.length, 3);//should be 3, we have a stored field
    List<StorableField> fields = home.createFields(point, 2);
    assertEquals(fields.size(), 3);//should be 3, we have a stored field
     //first two fields contain the values, third is just stored and contains the original
     for (int i = 0; i < 3; i++) {
      boolean hasValue = fields[i].binaryValue() != null
          || fields[i].stringValue() != null
          || fields[i].numericValue() != null;
      assertTrue("Doesn't have a value: " + fields[i], hasValue);
      boolean hasValue = fields.get(i).binaryValue() != null
          || fields.get(i).stringValue() != null
          || fields.get(i).numericValue() != null;
      assertTrue("Doesn't have a value: " + fields.get(i), hasValue);
     }
     /*assertTrue("first field " + fields[0].tokenStreamValue() +  " is not 35.0", pt.getSubType().toExternal(fields[0]).equals(String.valueOf(xy[0])));
     assertTrue("second field is not -79.34", pt.getSubType().toExternal(fields[1]).equals(String.valueOf(xy[1])));
@@ -101,7 +102,7 @@ public class PolyFieldTest extends SolrTestCaseJ4 {
     home = schema.getField("home_ns");
     assertNotNull(home);
     fields = home.createFields(point, 2);
    assertEquals(fields.length, 2);//should be 2, since we aren't storing
    assertEquals(fields.size(), 2);//should be 2, since we aren't storing
 
     home = schema.getField("home_ns");
     assertNotNull(home);
diff --git a/solr/example/solr/collection1/conf/schema.xml b/solr/example/solr/collection1/conf/schema.xml
index caaf5036dce..cc87d86bd24 100755
-- a/solr/example/solr/collection1/conf/schema.xml
++ b/solr/example/solr/collection1/conf/schema.xml
@@ -70,6 +70,15 @@
        <types> fieldType section
      indexed: true if this field should be indexed (searchable or sortable)
      stored: true if this field should be retrievable
     docValues: true if this field should have doc values. Doc values are
       useful for faceting, grouping, sorting and function queries. Although not
       required, doc values will make the index faster to load, more
       NRT-friendly and more memory-efficient. They however come with some
       limitations: they are currently only supported by StrField, UUIDField
       and all Trie*Fields, and depending on the field type, they might
       require the field to be single-valued, be required or have a default
       value (check the documentation of the field type you're interested in
       for more information)
      multiValued: true if this field may contain multiple values per document
      omitNorms: (expert) set to true to omit the norms associated with
        this field (this disables length normalization and index-time
@@ -156,6 +165,17 @@
 
    <field name="_version_" type="long" indexed="true" stored="true"/>
 
   <!--
     Some fields such as popularity and manu_exact could be modified to
     leverage doc values:
     <field name="popularity" type="int" indexed="true" stored="true" docValues="true" default="0" />
     <field name="manu_exact" type="string" indexed="false" stored="false" docValues="true" default="" />

     Although it would make indexing slightly slower and the index bigger, it
     would also make the index faster to load, more memory-efficient and more
     NRT-friendly.
     -->

    <!-- Uncommenting the following will create a "timestamp" field using
         a default value of "NOW" to indicate when each document was indexed.
      -->
@@ -282,7 +302,10 @@
        standard package such as org.apache.solr.analysis
     -->
 
    <!-- The StrField type is not analyzed, but indexed/stored verbatim. -->
    <!-- The StrField type is not analyzed, but indexed/stored verbatim.
       It supports doc values but in that case the field needs to be
       single-valued and either required or have a default value.
      -->
     <fieldType name="string" class="solr.StrField" sortMissingLast="true" />
 
     <!-- boolean type: "true" or "false" -->
@@ -306,6 +329,9 @@
 
     <!--
       Default numeric field types. For faster range queries, consider the tint/tfloat/tlong/tdouble types.

      These fields support doc values, but they require the field to be
      single-valued and either be required or have a default value.
     -->
     <fieldType name="int" class="solr.TrieIntField" precisionStep="0" positionIncrementGap="0"/>
     <fieldType name="float" class="solr.TrieFloatField" precisionStep="0" positionIncrementGap="0"/>
- 
2.19.1.windows.1

