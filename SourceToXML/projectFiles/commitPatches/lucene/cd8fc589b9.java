From cd8fc589b9812ac1f9ee8163e7145b53600c0b38 Mon Sep 17 00:00:00 2001
From: David Wayne Smiley <dsmiley@apache.org>
Date: Tue, 6 Jan 2015 13:19:20 +0000
Subject: [PATCH] LUCENE-6155: Add payloads to MemoryIndex; add flag to HL
 QueryScorer

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1649798 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   4 +
 .../lucene/search/highlight/QueryScorer.java  |  16 ++-
 .../highlight/WeightedSpanTermExtractor.java  |  14 ++-
 .../search/highlight/HighlighterTest.java     |  54 +++++++--
 .../lucene/index/memory/MemoryIndex.java      | 103 +++++++++++-------
 .../memory/TestMemoryIndexAgainstRAMDir.java  |  37 ++++---
 6 files changed, 163 insertions(+), 65 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 14068e4ea8d..be7e505d280 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -134,6 +134,10 @@ New Features
   rate limit IO writes for each merge depending on incoming merge
   rate.  (Mike McCandless)
 
* LUCENE-6155: Add payload support to MemoryIndex. The default highlighter's
  QueryScorer and WeighedSpanTermExtractor now have setUsePayloads(bool).
  (David Smiley)

 Optimizations
 
 * LUCENE-5960: Use a more efficient bitset, not a Set<Integer>, to
diff --git a/lucene/highlighter/src/java/org/apache/lucene/search/highlight/QueryScorer.java b/lucene/highlighter/src/java/org/apache/lucene/search/highlight/QueryScorer.java
index e855a17a585..7655e889233 100644
-- a/lucene/highlighter/src/java/org/apache/lucene/search/highlight/QueryScorer.java
++ b/lucene/highlighter/src/java/org/apache/lucene/search/highlight/QueryScorer.java
@@ -54,6 +54,7 @@ public class QueryScorer implements Scorer {
   private boolean skipInitExtractor;
   private boolean wrapToCaching = true;
   private int maxCharsToAnalyze;
  private boolean usePayloads = false;
 
   /**
    * @param query Query to use for highlighting
@@ -213,6 +214,7 @@ public class QueryScorer implements Scorer {
     qse.setMaxDocCharsToAnalyze(maxCharsToAnalyze);
     qse.setExpandMultiTermQuery(expandMultiTermQuery);
     qse.setWrapIfNotCachingTokenFilter(wrapToCaching);
    qse.setUsePayloads(usePayloads);
     if (reader == null) {
       this.fieldWeightedSpanTerms = qse.getWeightedSpanTerms(query,
           tokenStream, field);
@@ -259,7 +261,19 @@ public class QueryScorer implements Scorer {
   public void setExpandMultiTermQuery(boolean expandMultiTermQuery) {
     this.expandMultiTermQuery = expandMultiTermQuery;
   }
  

  /**
   * Whether or not we should capture payloads in {@link MemoryIndex} at each position so that queries can access them.
   * This does not apply to term vector based TokenStreams, which support payloads only when the term vector has them.
   */
  public boolean isUsePayloads() {
    return usePayloads;
  }

  public void setUsePayloads(boolean usePayloads) {
    this.usePayloads = usePayloads;
  }

   /**
    * By default, {@link TokenStream}s that are not of the type
    * {@link CachingTokenFilter} are wrapped in a {@link CachingTokenFilter} to
diff --git a/lucene/highlighter/src/java/org/apache/lucene/search/highlight/WeightedSpanTermExtractor.java b/lucene/highlighter/src/java/org/apache/lucene/search/highlight/WeightedSpanTermExtractor.java
index abbfd5f55c0..04e794aba93 100644
-- a/lucene/highlighter/src/java/org/apache/lucene/search/highlight/WeightedSpanTermExtractor.java
++ b/lucene/highlighter/src/java/org/apache/lucene/search/highlight/WeightedSpanTermExtractor.java
@@ -83,9 +83,9 @@ public class WeightedSpanTermExtractor {
   private boolean cachedTokenStream;
   private boolean wrapToCaching = true;
   private int maxDocCharsToAnalyze;
  private boolean usePayloads = false;
   private LeafReader internalReader = null;
 

   public WeightedSpanTermExtractor() {
   }
 
@@ -384,7 +384,7 @@ public class WeightedSpanTermExtractor {
 
       // Use MemoryIndex (index/invert this tokenStream now)
       if (internalReader == null) {
        final MemoryIndex indexer = new MemoryIndex(true);
        final MemoryIndex indexer = new MemoryIndex(true, usePayloads);//offsets and payloads
         if (cacheIt) {
           assert !cachedTokenStream;
           tokenStream = new CachingTokenFilter(new OffsetLimitTokenFilter(tokenStream, maxDocCharsToAnalyze));
@@ -652,7 +652,15 @@ public class WeightedSpanTermExtractor {
   public void setExpandMultiTermQuery(boolean expandMultiTermQuery) {
     this.expandMultiTermQuery = expandMultiTermQuery;
   }
  

  public boolean isUsePayloads() {
    return usePayloads;
  }

  public void setUsePayloads(boolean usePayloads) {
    this.usePayloads = usePayloads;
  }

   public boolean isCachedTokenStream() {
     return cachedTokenStream;
   }
diff --git a/lucene/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java b/lucene/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java
index 117546aa859..7072d93cb46 100644
-- a/lucene/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java
++ b/lucene/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java
@@ -17,6 +17,8 @@ package org.apache.lucene.search.highlight;
  * limitations under the License.
  */
 
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
 import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.nio.charset.StandardCharsets;
@@ -28,10 +30,17 @@ import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
 
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockPayloadAnalyzer;
import org.apache.lucene.analysis.MockTokenFilter;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
@@ -44,20 +53,43 @@ import org.apache.lucene.document.TextField;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
 import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
 import org.apache.lucene.index.StoredDocument;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.queries.CommonTermsQuery;
import org.apache.lucene.search.*;
 import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeFilter;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
 import org.apache.lucene.search.highlight.SynonymTokenizer.TestHighlightRunner;
 import org.apache.lucene.search.join.BitDocIdSetCachingWrapperFilter;
 import org.apache.lucene.search.join.BitDocIdSetFilter;
 import org.apache.lucene.search.join.ScoreMode;
 import org.apache.lucene.search.join.ToChildBlockJoinQuery;
 import org.apache.lucene.search.join.ToParentBlockJoinQuery;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanPayloadCheckQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.LuceneTestCase;
@@ -1891,7 +1923,7 @@ public class HighlighterTest extends BaseTokenStreamTestCase implements Formatte
     reader.close();
   }
 
  /** If we have term vectors, we can highlight based on payloads */
  /** We can highlight based on payloads. It's supported both via term vectors and MemoryIndex since Lucene 5. */
   public void testPayloadQuery() throws IOException, InvalidTokenOffsetsException {
     final String text = "random words and words";//"words" at positions 1 & 4
 
@@ -1900,7 +1932,7 @@ public class HighlighterTest extends BaseTokenStreamTestCase implements Formatte
       writer.deleteAll();
       Document doc = new Document();
 
      doc.add(new Field(FIELD_NAME, text, FIELD_TYPE_TV));
      doc.add(new Field(FIELD_NAME, text, fieldType));
       writer.addDocument(doc);
       writer.commit();
     }
@@ -1908,12 +1940,16 @@ public class HighlighterTest extends BaseTokenStreamTestCase implements Formatte
       Query query = new SpanPayloadCheckQuery(new SpanTermQuery(new Term(FIELD_NAME, "words")),
           Collections.singleton("pos: 1".getBytes("UTF-8")));//just match the first "word" occurrence
       IndexSearcher searcher = newSearcher(reader);
      Scorer scorer = new QueryScorer(query, searcher.getIndexReader(), FIELD_NAME);
      QueryScorer scorer = new QueryScorer(query, searcher.getIndexReader(), FIELD_NAME);
      scorer.setUsePayloads(true);
       Highlighter h = new Highlighter(scorer);
 
       TopDocs hits = searcher.search(query, null, 10);
       assertEquals(1, hits.scoreDocs.length);
       TokenStream stream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), 0, FIELD_NAME, analyzer);
      if (random().nextBoolean()) {
        stream = new CachingTokenFilter(stream);//conceals detection of TokenStreamFromTermVector
      }
       String result = h.getBestFragment(stream, text);
       assertEquals("random <B>words</B> and words", result);//only highlight first "word"
     }
diff --git a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
index 28c6a976d17..28424c13c7c 100644
-- a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
++ b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
@@ -29,6 +29,7 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
 import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
 import org.apache.lucene.index.BinaryDocValues;
@@ -60,6 +61,8 @@ import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.ByteBlockPool;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefArray;
import org.apache.lucene.util.BytesRefBuilder;
 import org.apache.lucene.util.BytesRefHash;
 import org.apache.lucene.util.BytesRefHash.DirectBytesStartArray;
 import org.apache.lucene.util.Counter;
@@ -187,17 +190,19 @@ import org.apache.lucene.util.RecyclingIntBlockAllocator;
  */
 public class MemoryIndex {
 
  private static final boolean DEBUG = false;

   /** info for each field: Map&lt;String fieldName, Info field&gt; */
   private final SortedMap<String,Info> fields = new TreeMap<>();
   
   private final boolean storeOffsets;
  
  private static final boolean DEBUG = false;
  private final boolean storePayloads;
 
   private final ByteBlockPool byteBlockPool;
   private final IntBlockPool intBlockPool;
 //  private final IntBlockPool.SliceReader postingsReader;
   private final IntBlockPool.SliceWriter postingsWriter;
  private final BytesRefArray payloadsBytesRefs;//non null only when storePayloads
 
   private Counter bytesUsed;
 
@@ -206,7 +211,7 @@ public class MemoryIndex {
   private Similarity normSimilarity = IndexSearcher.getDefaultSimilarity();
 
   /**
   * Constructs an empty instance.
   * Constructs an empty instance that will not store offsets or payloads.
    */
   public MemoryIndex() {
     this(false);
@@ -215,25 +220,37 @@ public class MemoryIndex {
   /**
    * Constructs an empty instance that can optionally store the start and end
    * character offset of each token term in the text. This can be useful for
   * highlighting of hit locations with the Lucene highlighter package.
   * Protected until the highlighter package matures, so that this can actually
   * be meaningfully integrated.
   * highlighting of hit locations with the Lucene highlighter package.  But
   * it will not store payloads; use another constructor for that.
    * 
    * @param storeOffsets
    *            whether or not to store the start and end character offset of
    *            each token term in the text
    */
   public MemoryIndex(boolean storeOffsets) {
    this(storeOffsets, 0);
    this(storeOffsets, false);
   }
  

  /**
   * Constructs an empty instance with the option of storing offsets and payloads.
   *
   * @param storeOffsets store term offsets at each position
   * @param storePayloads store term payloads at each position
   */
  public MemoryIndex(boolean storeOffsets, boolean storePayloads) {
    this(storeOffsets, storePayloads, 0);
  }

   /**
    * Expert: This constructor accepts an upper limit for the number of bytes that should be reused if this instance is {@link #reset()}.
   * The payload storage, if used, is unaffected by maxReusuedBytes, however.
    * @param storeOffsets <code>true</code> if offsets should be stored
   * @param storePayloads <code>true</code> if payloads should be stored
    * @param maxReusedBytes the number of bytes that should remain in the internal memory pools after {@link #reset()} is called
    */
  MemoryIndex(boolean storeOffsets, long maxReusedBytes) {
  MemoryIndex(boolean storeOffsets, boolean storePayloads, long maxReusedBytes) {
     this.storeOffsets = storeOffsets;
    this.storePayloads = storePayloads;
     this.bytesUsed = Counter.newCounter();
     final int maxBufferedByteBlocks = (int)((maxReusedBytes/2) / ByteBlockPool.BYTE_BLOCK_SIZE );
     final int maxBufferedIntBlocks = (int) ((maxReusedBytes - (maxBufferedByteBlocks*ByteBlockPool.BYTE_BLOCK_SIZE))/(IntBlockPool.INT_BLOCK_SIZE * RamUsageEstimator.NUM_BYTES_INT));
@@ -241,6 +258,8 @@ public class MemoryIndex {
     byteBlockPool = new ByteBlockPool(new RecyclingByteBlockAllocator(ByteBlockPool.BYTE_BLOCK_SIZE, maxBufferedByteBlocks, bytesUsed));
     intBlockPool = new IntBlockPool(new RecyclingIntBlockAllocator(IntBlockPool.INT_BLOCK_SIZE, maxBufferedIntBlocks, bytesUsed));
     postingsWriter = new SliceWriter(intBlockPool);
    //TODO refactor BytesRefArray to allow us to apply maxReusedBytes option
    payloadsBytesRefs = storePayloads ? new BytesRefArray(bytesUsed) : null;
   }
   
   /**
@@ -381,8 +400,8 @@ public class MemoryIndex {
    *
    * @param fieldName
    *            a name to be associated with the text
   * @param stream
   *            the token stream to retrieve tokens from.
   * @param tokenStream
   *            the token stream to retrieve tokens from. It's guaranteed to be closed no matter what.
    * @param boost
    *            the boost factor for hits for this field
    * @param positionIncrementGap
@@ -391,16 +410,17 @@ public class MemoryIndex {
    *            the offset gap if fields with the same name are added more than once
    * @see org.apache.lucene.document.Field#setBoost(float)
    */
  public void addField(String fieldName, TokenStream stream, float boost, int positionIncrementGap, int offsetGap) {
    try {
  public void addField(String fieldName, TokenStream tokenStream, float boost, int positionIncrementGap,
                       int offsetGap) {
    try (TokenStream stream = tokenStream) {
       if (frozen)
         throw new IllegalArgumentException("Cannot call addField() when MemoryIndex is frozen");
       if (fieldName == null)
         throw new IllegalArgumentException("fieldName must not be null");
       if (stream == null)
          throw new IllegalArgumentException("token stream must not be null");
        throw new IllegalArgumentException("token stream must not be null");
       if (boost <= 0.0f)
          throw new IllegalArgumentException("boost factor must be greater than 0.0");
        throw new IllegalArgumentException("boost factor must be greater than 0.0");
       int numTokens = 0;
       int numOverlapTokens = 0;
       int pos = -1;
@@ -421,8 +441,9 @@ public class MemoryIndex {
         sliceArray = info.sliceArray;
         sumTotalTermFreq = info.sumTotalTermFreq;
       } else {
        fieldInfo = new FieldInfo(fieldName, fields.size(), false, false, false,
            this.storeOffsets ? IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS : IndexOptions.DOCS_AND_FREQS_AND_POSITIONS,
        fieldInfo = new FieldInfo(fieldName, fields.size(), false, false, this.storePayloads,
            this.storeOffsets
                ? IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS : IndexOptions.DOCS_AND_FREQS_AND_POSITIONS,
             DocValuesType.NONE, -1, null);
         sliceArray = new SliceByteStartArray(BytesRefHash.DEFAULT_CAPACITY);
         terms = new BytesRefHash(byteBlockPool, BytesRefHash.DEFAULT_CAPACITY, sliceArray);
@@ -431,6 +452,7 @@ public class MemoryIndex {
       TermToBytesRefAttribute termAtt = stream.getAttribute(TermToBytesRefAttribute.class);
       PositionIncrementAttribute posIncrAttribute = stream.addAttribute(PositionIncrementAttribute.class);
       OffsetAttribute offsetAtt = stream.addAttribute(OffsetAttribute.class);
      PayloadAttribute payloadAtt = storePayloads ? stream.addAttribute(PayloadAttribute.class) : null;
       BytesRef ref = termAtt.getBytesRef();
       stream.reset();
       
@@ -451,13 +473,16 @@ public class MemoryIndex {
         }
         sliceArray.freq[ord]++;
         sumTotalTermFreq++;
        if (!storeOffsets) {
          postingsWriter.writeInt(pos);
        } else {
          postingsWriter.writeInt(pos);
        postingsWriter.writeInt(pos);
        if (storeOffsets) {
           postingsWriter.writeInt(offsetAtt.startOffset() + offset);
           postingsWriter.writeInt(offsetAtt.endOffset() + offset);
         }
        if (storePayloads) {
          final BytesRef payload = payloadAtt.getPayload();
          int pIndex = payload == null ? -1 : payloadsBytesRefs.append(payload);
          postingsWriter.writeInt(pIndex);
        }
         sliceArray.end[ord] = postingsWriter.getCurrentOffset();
       }
       stream.end();
@@ -466,16 +491,8 @@ public class MemoryIndex {
       if (numTokens > 0) {
         fields.put(fieldName, new Info(fieldInfo, terms, sliceArray, numTokens, numOverlapTokens, boost, pos, offsetAtt.endOffset() + offset, sumTotalTermFreq));
       }
    } catch (Exception e) { // can never happen
    } catch (IOException e) {
       throw new RuntimeException(e);
    } finally {
      try {
        if (stream != null) {
          stream.close();
        }
      } catch (IOException e2) {
        throw new RuntimeException(e2);
      }
     }
   }
 
@@ -861,7 +878,7 @@ public class MemoryIndex {
 
           @Override
           public boolean hasPayloads() {
            return false;
            return storePayloads;
           }
         };
       }
@@ -1043,17 +1060,20 @@ public class MemoryIndex {
     }
     
     private class MemoryDocsAndPositionsEnum extends DocsAndPositionsEnum {
      private final SliceReader sliceReader;
       private int posUpto; // for assert
       private boolean hasNext;
       private Bits liveDocs;
       private int doc = -1;
      private SliceReader sliceReader;
       private int freq;
       private int startOffset;
       private int endOffset;
      
      private int payloadIndex;
      private final BytesRefBuilder payloadBuilder;//only non-null when storePayloads

       public MemoryDocsAndPositionsEnum() {
         this.sliceReader = new SliceReader(intBlockPool);
        this.payloadBuilder = storePayloads ? new BytesRefBuilder() : null;
       }
 
       public DocsAndPositionsEnum reset(Bits liveDocs, int start, int end, int freq) {
@@ -1096,14 +1116,15 @@ public class MemoryIndex {
       public int nextPosition() {
         assert posUpto++ < freq;
         assert !sliceReader.endOfSlice() : " stores offsets : " + startOffset;
        int pos = sliceReader.readInt();
         if (storeOffsets) {
          int pos = sliceReader.readInt();
           startOffset = sliceReader.readInt();
           endOffset = sliceReader.readInt();
          return pos;
        } else {
          return sliceReader.readInt();
         }
        if (storePayloads) {
          payloadIndex = sliceReader.readInt();
        }
        return pos;
       }
 
       @Override
@@ -1118,7 +1139,10 @@ public class MemoryIndex {
 
       @Override
       public BytesRef getPayload() {
        return null;
        if (payloadBuilder == null || payloadIndex == -1) {
          return null;
        }
        return payloadsBytesRefs.get(payloadBuilder, payloadIndex);
       }
       
       @Override
@@ -1178,6 +1202,9 @@ public class MemoryIndex {
     this.normSimilarity = IndexSearcher.getDefaultSimilarity();
     byteBlockPool.reset(false, false); // no need to 0-fill the buffers
     intBlockPool.reset(true, false); // here must must 0-fill since we use slices
    if (payloadsBytesRefs != null) {
      payloadsBytesRefs.clear();
    }
     this.frozen = false;
   }
   
diff --git a/lucene/memory/src/test/org/apache/lucene/index/memory/TestMemoryIndexAgainstRAMDir.java b/lucene/memory/src/test/org/apache/lucene/index/memory/TestMemoryIndexAgainstRAMDir.java
index 65c140cff09..a507552ce5d 100644
-- a/lucene/memory/src/test/org/apache/lucene/index/memory/TestMemoryIndexAgainstRAMDir.java
++ b/lucene/memory/src/test/org/apache/lucene/index/memory/TestMemoryIndexAgainstRAMDir.java
@@ -68,8 +68,8 @@ import org.apache.lucene.search.spans.SpanOrQuery;
 import org.apache.lucene.search.spans.SpanQuery;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.ByteBlockPool.Allocator;
 import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.ByteBlockPool.Allocator;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.LineFileDocs;
@@ -116,7 +116,7 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
    * runs random tests, up to ITERATIONS times.
    */
   public void testRandomQueries() throws Exception {
    MemoryIndex index =  new MemoryIndex(random().nextBoolean(), random().nextInt(50) * 1024 * 1024);
    MemoryIndex index = randomMemoryIndex();
     for (int i = 0; i < ITERATIONS; i++) {
       assertAgainstRAMDirectory(index);
     }
@@ -148,7 +148,8 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
     Directory ramdir = new RAMDirectory();
     Analyzer analyzer = randomAnalyzer();
     IndexWriter writer = new IndexWriter(ramdir,
                                         new IndexWriterConfig(analyzer).setCodec(TestUtil.alwaysPostingsFormat(TestUtil.getDefaultPostingsFormat())));
                                         new IndexWriterConfig(analyzer).setCodec(
                                             TestUtil.alwaysPostingsFormat(TestUtil.getDefaultPostingsFormat())));
     Document doc = new Document();
     Field field1 = newTextField("foo", fooField.toString(), Field.Store.NO);
     Field field2 = newTextField("term", termField.toString(), Field.Store.NO);
@@ -209,6 +210,10 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
                   assertEquals(iwDocsAndPos.startOffset(), memDocsAndPos.startOffset());
                   assertEquals(iwDocsAndPos.endOffset(), memDocsAndPos.endOffset());
                 }

                if (iwTerms.hasPayloads()) {
                  assertEquals(iwDocsAndPos.getPayload(), memDocsAndPos.getPayload());
                }
               }
               
             }
@@ -311,7 +316,7 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
   
   public void testDocsEnumStart() throws Exception {
     Analyzer analyzer = new MockAnalyzer(random());
    MemoryIndex memory = new MemoryIndex(random().nextBoolean(),  random().nextInt(50) * 1024 * 1024);
    MemoryIndex memory = new MemoryIndex(random().nextBoolean(), false, random().nextInt(50) * 1024 * 1024);
     memory.addField("foo", "bar", analyzer);
     LeafReader reader = (LeafReader) memory.createSearcher().getIndexReader();
     DocsEnum disi = TestUtil.docs(random(), reader, "foo", new BytesRef("bar"), null, null, DocsEnum.FLAG_NONE);
@@ -336,11 +341,15 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
       return new ByteBlockPool.DirectAllocator();
     }
   }
  

  private MemoryIndex randomMemoryIndex() {
    return new MemoryIndex(random().nextBoolean(), random().nextBoolean(), random().nextInt(50) * 1024 * 1024);
  }

   public void testDocsAndPositionsEnumStart() throws Exception {
     Analyzer analyzer = new MockAnalyzer(random());
     int numIters = atLeast(3);
    MemoryIndex memory = new MemoryIndex(true,  random().nextInt(50) * 1024 * 1024);
    MemoryIndex memory = new MemoryIndex(true, false, random().nextInt(50) * 1024 * 1024);
     for (int i = 0; i < numIters; i++) { // check reuse
       memory.addField("foo", "bar", analyzer);
       LeafReader reader = (LeafReader) memory.createSearcher().getIndexReader();
@@ -370,7 +379,7 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
     RegexpQuery regex = new RegexpQuery(new Term("field", "worl."));
     SpanQuery wrappedquery = new SpanMultiTermQueryWrapper<>(regex);
         
    MemoryIndex mindex = new MemoryIndex(random().nextBoolean(),  random().nextInt(50) * 1024 * 1024);
    MemoryIndex mindex = randomMemoryIndex();
     mindex.addField("field", new MockAnalyzer(random()).tokenStream("field", "hello there"));
 
     // This throws an NPE
@@ -382,7 +391,7 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
     RegexpQuery regex = new RegexpQuery(new Term("field", "worl."));
     SpanQuery wrappedquery = new SpanOrQuery(new SpanMultiTermQueryWrapper<>(regex));
 
    MemoryIndex mindex = new MemoryIndex(random().nextBoolean(),  random().nextInt(50) * 1024 * 1024);
    MemoryIndex mindex = randomMemoryIndex();
     mindex.addField("field", new MockAnalyzer(random()).tokenStream("field", "hello there"));
 
     // This passes though
@@ -390,7 +399,7 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
   }
   
   public void testSameFieldAddedMultipleTimes() throws IOException {
    MemoryIndex mindex = new MemoryIndex(random().nextBoolean(),  random().nextInt(50) * 1024 * 1024);
    MemoryIndex mindex = randomMemoryIndex();
     MockAnalyzer mockAnalyzer = new MockAnalyzer(random());
     mindex.addField("field", "the quick brown fox", mockAnalyzer);
     mindex.addField("field", "jumps over the", mockAnalyzer);
@@ -409,8 +418,8 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
     assertTrue("posGap" + mockAnalyzer.getPositionIncrementGap("field") , mindex.search(query) > 0.0001);
   }
   
  public void testNonExistingsField() throws IOException {
    MemoryIndex mindex = new MemoryIndex(random().nextBoolean(),  random().nextInt(50) * 1024 * 1024);
  public void testNonExistentField() throws IOException {
    MemoryIndex mindex = randomMemoryIndex();
     MockAnalyzer mockAnalyzer = new MockAnalyzer(random());
     mindex.addField("field", "the quick brown fox", mockAnalyzer);
     LeafReader reader = (LeafReader) mindex.createSearcher().getIndexReader();
@@ -420,11 +429,11 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
     assertNull(reader.termPositionsEnum(new Term("not-in-index", "foo")));
     assertNull(reader.terms("not-in-index"));
   }
  

   public void testDuellMemIndex() throws IOException {
     LineFileDocs lineFileDocs = new LineFileDocs(random());
     int numDocs = atLeast(10);
    MemoryIndex memory = new MemoryIndex(random().nextBoolean(),  random().nextInt(50) * 1024 * 1024);
    MemoryIndex memory = randomMemoryIndex();
     for (int i = 0; i < numDocs; i++) {
       Directory dir = newDirectory();
       MockAnalyzer mockAnalyzer = new MockAnalyzer(random());
@@ -535,7 +544,7 @@ public class TestMemoryIndexAgainstRAMDir extends BaseTokenStreamTestCase {
         assertThat("Position test failed" + failDesc, memPos, equalTo(pos));
         assertThat("Start offset test failed" + failDesc, memDocsPosEnum.startOffset(), equalTo(docsPosEnum.startOffset()));
         assertThat("End offset test failed" + failDesc, memDocsPosEnum.endOffset(), equalTo(docsPosEnum.endOffset()));
        assertThat("Missing payload test failed" + failDesc, docsPosEnum.getPayload(), equalTo(null));
        assertThat("Missing payload test failed" + failDesc, docsPosEnum.getPayload(), equalTo(docsPosEnum.getPayload()));
       }
     }
     assertNull("Still some tokens not processed", memTermEnum.next());
- 
2.19.1.windows.1

