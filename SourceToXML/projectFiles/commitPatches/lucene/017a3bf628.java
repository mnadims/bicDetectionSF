From 017a3bf628189bfd559dfb187f8b42ddabdb2c7e Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Thu, 19 Sep 2013 20:57:09 +0000
Subject: [PATCH] LUCENE-5123: invert postings writing API

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1524840 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   5 +
 .../codecs/blockterms/BlockTermsReader.java   |  11 -
 .../codecs/blockterms/BlockTermsWriter.java   |  13 +-
 .../bloom/BloomFilteringPostingsFormat.java   |  49 +-
 .../codecs/memory/DirectPostingsFormat.java   |  16 -
 .../codecs/memory/FSTOrdTermsReader.java      |  14 -
 .../codecs/memory/FSTOrdTermsWriter.java      |  27 +-
 .../lucene/codecs/memory/FSTTermsReader.java  |  14 -
 .../lucene/codecs/memory/FSTTermsWriter.java  |  27 +-
 .../memory/MemoryDocValuesProducer.java       |   6 -
 .../codecs/memory/MemoryPostingsFormat.java   |  19 +-
 .../simpletext/SimpleTextFieldsReader.java    |  11 -
 .../simpletext/SimpleTextFieldsWriter.java    | 288 ++++++----
 .../simpletext/SimpleTextPostingsFormat.java  |   4 +-
 .../SimpleTextTermVectorsReader.java          |  11 -
 .../SimpleTextTermVectorsWriter.java          |   6 -
 .../lucene/codecs/BlockTreeTermsReader.java   |  16 -
 .../lucene/codecs/BlockTreeTermsWriter.java   |   9 +-
 .../apache/lucene/codecs/FieldsConsumer.java  |  75 +--
 .../lucene/codecs/PostingsConsumer.java       |  81 ---
 .../apache/lucene/codecs/PostingsFormat.java  |   2 +-
 .../lucene/codecs/PushFieldsConsumer.java     | 181 ++++++
 .../lucene/codecs/TermVectorsWriter.java      |   5 -
 .../apache/lucene/codecs/TermsConsumer.java   | 141 +----
 .../CompressingTermVectorsReader.java         |  12 -
 .../CompressingTermVectorsWriter.java         |   6 -
 .../lucene40/Lucene40TermVectorsReader.java   |  13 -
 .../lucene40/Lucene40TermVectorsWriter.java   |   6 -
 .../lucene42/Lucene42DocValuesProducer.java   |   6 -
 .../lucene45/Lucene45DocValuesProducer.java   |   6 -
 .../perfield/PerFieldPostingsFormat.java      | 184 +++---
 .../lucene/index/AutomatonTermsEnum.java      |   8 +-
 .../org/apache/lucene/index/CheckIndex.java   |   4 +-
 .../org/apache/lucene/index/DocTermOrds.java  |   6 -
 .../lucene/index/FilterAtomicReader.java      |  11 -
 .../lucene/index/FilteredTermsEnum.java       |  10 +-
 .../apache/lucene/index/FreqProxFields.java   | 523 ++++++++++++++++++
 .../lucene/index/FreqProxTermsWriter.java     | 104 ++--
 .../index/FreqProxTermsWriterPerField.java    | 265 +--------
 .../lucene/index/MappedMultiFields.java       | 136 +++++
 .../MappingMultiDocsAndPositionsEnum.java     |  25 +-
 .../MappingMultiDocsEnum.java                 |  27 +-
 .../org/apache/lucene/index/MergeState.java   |   4 +
 .../org/apache/lucene/index/MultiTerms.java   |  19 -
 .../apache/lucene/index/MultiTermsEnum.java   |  31 +-
 .../apache/lucene/index/SegmentMerger.java    |  22 +-
 .../index/SortedDocValuesTermsEnum.java       |   6 -
 .../index/SortedSetDocValuesTermsEnum.java    |   6 -
 .../lucene/index/TermVectorsConsumer.java     |   6 -
 .../index/TermVectorsConsumerPerField.java    |   7 +-
 .../java/org/apache/lucene/index/Terms.java   |  10 +-
 .../org/apache/lucene/index/TermsEnum.java    |  11 +-
 .../lucene/index/TermsHashPerField.java       |  19 +-
 .../search/ConstantScoreAutoRewrite.java      |   2 +-
 .../search/DocTermOrdsRewriteMethod.java      |   7 -
 .../search/FieldCacheRewriteMethod.java       |   7 -
 .../apache/lucene/search/FuzzyTermsEnum.java  |   7 +-
 .../lucene/search/NumericRangeQuery.java      |  14 +-
 .../apache/lucene/search/PrefixTermsEnum.java |   2 +-
 .../apache/lucene/search/ScoringRewrite.java  |   2 +-
 .../lucene/search/TermCollectingRewrite.java  |   7 -
 .../lucene/search/TermRangeTermsEnum.java     |  11 +-
 .../apache/lucene/search/TopTermsRewrite.java |  20 +-
 .../org/apache/lucene/search/package.html     |   2 +-
 .../apache/lucene/util/BytesRefIterator.java  |  14 -
 .../lucene41/TestBlockPostingsFormat3.java    |   1 -
 .../org/apache/lucene/index/TestCodecs.java   | 294 ++++++++--
 .../index/TestConcurrentMergeScheduler.java   |   5 +-
 .../lucene/index/TestDirectoryReader.java     |   1 -
 .../index/TestIndexWriterExceptions.java      |  11 +-
 .../apache/lucene/index/TestLongPostings.java |   3 +
 .../lucene/index/memory/MemoryIndex.java      |  10 -
 .../SlowCollatedTermRangeTermsEnum.java       |   3 +-
 .../sandbox/queries/SlowFuzzyTermsEnum.java   |   2 +-
 .../AbstractVisitingPrefixTreeFilter.java     |  10 +-
 .../search/spell/HighFrequencyDictionary.java |  10 -
 .../search/spell/PlainTextDictionary.java     |  14 +-
 .../lucene/search/spell/TermFreqIterator.java |   6 -
 .../BufferingTermFreqIteratorWrapper.java     |  10 -
 .../lucene/search/suggest/BytesRefArray.java  |   5 -
 .../lucene/search/suggest/FileDictionary.java |   7 -
 .../SortedTermFreqIteratorWrapper.java        |   5 -
 .../search/suggest/fst/ExternalRefSorter.java |  13 +-
 .../search/suggest/jaspell/JaspellLookup.java |   6 -
 .../lucene/search/suggest/tst/TSTLookup.java  |   8 +-
 .../search/suggest/TermFreqArrayIterator.java |   6 -
 .../suggest/TermFreqPayloadArrayIterator.java |   6 -
 .../suggest/TestHighFrequencyDictionary.java  |   1 -
 .../asserting/AssertingPostingsFormat.java    | 138 ++++-
 .../asserting/AssertingTermVectorsFormat.java |   6 -
 .../codecs/ramonly/RAMOnlyPostingsFormat.java |  60 +-
 .../index/BasePostingsFormatTestCase.java     | 502 ++++++++++++++---
 .../index/BaseTermVectorsFormatTestCase.java  |   2 +-
 .../apache/lucene/util/LuceneTestCase.java    |   1 -
 94 files changed, 2165 insertions(+), 1600 deletions(-)
 create mode 100644 lucene/core/src/java/org/apache/lucene/codecs/PushFieldsConsumer.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/index/FreqProxFields.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/index/MappedMultiFields.java
 rename lucene/core/src/java/org/apache/lucene/{codecs => index}/MappingMultiDocsAndPositionsEnum.java (87%)
 rename lucene/core/src/java/org/apache/lucene/{codecs => index}/MappingMultiDocsEnum.java (86%)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index ffb94a0fea0..c2c8c224bc3 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -38,6 +38,11 @@ New Features
 * SOLR-3359: Added analyzer attribute/property to SynonymFilterFactory.
   (Ryo Onodera via Koji Sekiguchi)
 
* LUCENE-5123: Add a "push" option to the postings writing API, so
  that a PostingsFormat now receives a Fields instance and it is
  responsible for iterating through all fields, terms, documents and
  positions.  (Robert Muir, Mike McCandless)

 Optimizations
 
 * LUCENE-4848: Use Java 7 NIO2-FileChannel instead of RandomAccessFile
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/blockterms/BlockTermsReader.java b/lucene/codecs/src/java/org/apache/lucene/codecs/blockterms/BlockTermsReader.java
index a35e3c3ca5e..43786aa7331 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/blockterms/BlockTermsReader.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/blockterms/BlockTermsReader.java
@@ -19,7 +19,6 @@ package org.apache.lucene.codecs.blockterms;
 
 import java.io.IOException;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.Iterator;
 import java.util.TreeMap;
 
@@ -244,11 +243,6 @@ public class BlockTermsReader extends FieldsProducer {
       this.longsSize = longsSize;
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public TermsEnum iterator(TermsEnum reuse) throws IOException {
       return new SegmentTermsEnum();
@@ -349,11 +343,6 @@ public class BlockTermsReader extends FieldsProducer {
         longs = new long[longsSize];
       }
 
      @Override
      public Comparator<BytesRef> getComparator() {
        return BytesRef.getUTF8SortedAsUnicodeComparator();
      }

       // TODO: we may want an alternate mode here which is
       // "if you are about to return NOT_FOUND I won't use
       // the terms data from that"; eg FuzzyTermsEnum will
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/blockterms/BlockTermsWriter.java b/lucene/codecs/src/java/org/apache/lucene/codecs/blockterms/BlockTermsWriter.java
index 2bd8c91b605..a8abaea349a 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/blockterms/BlockTermsWriter.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/blockterms/BlockTermsWriter.java
@@ -19,18 +19,17 @@ package org.apache.lucene.codecs.blockterms;
 
 import java.io.IOException;
 import java.util.ArrayList;
import java.util.Comparator;
 import java.util.List;
 
 import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.PostingsConsumer;
 import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.PushFieldsConsumer;
 import org.apache.lucene.codecs.TermStats;
 import org.apache.lucene.codecs.BlockTermState;
 import org.apache.lucene.codecs.TermsConsumer;
import org.apache.lucene.index.FieldInfo;
 import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.FieldInfo;
 import org.apache.lucene.index.FieldInfos;
 import org.apache.lucene.index.IndexFileNames;
 import org.apache.lucene.index.SegmentWriteState;
@@ -53,7 +52,7 @@ import org.apache.lucene.util.RamUsageEstimator;
  * @lucene.experimental
  */
 
public class BlockTermsWriter extends FieldsConsumer {
public class BlockTermsWriter extends PushFieldsConsumer {
 
   final static String CODEC_NAME = "BLOCK_TERMS_DICT";
 
@@ -100,6 +99,7 @@ public class BlockTermsWriter extends FieldsConsumer {
   public BlockTermsWriter(TermsIndexWriterBase termsIndexWriter,
       SegmentWriteState state, PostingsWriterBase postingsWriter)
       throws IOException {
    super(state);
     final String termsFileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, TERMS_EXTENSION);
     this.termsIndexWriter = termsIndexWriter;
     out = state.directory.createOutput(termsFileName, state.context);
@@ -200,11 +200,6 @@ public class BlockTermsWriter extends FieldsConsumer {
       this.longsSize = postingsWriter.setField(fieldInfo);
     }
     
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public PostingsConsumer startTerm(BytesRef text) throws IOException {
       //System.out.println("BTW: startTerm term=" + fieldInfo.name + ":" + text.utf8ToString() + " " + text + " seg=" + segment);
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/bloom/BloomFilteringPostingsFormat.java b/lucene/codecs/src/java/org/apache/lucene/codecs/bloom/BloomFilteringPostingsFormat.java
index 38b8602376e..c8af4e96575 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/bloom/BloomFilteringPostingsFormat.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/bloom/BloomFilteringPostingsFormat.java
@@ -19,18 +19,18 @@ package org.apache.lucene.codecs.bloom;
 
 import java.io.IOException;
 import java.util.ArrayList;
import java.util.Comparator;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
import java.util.Map;
 import java.util.Map.Entry;
import java.util.Map;
 
 import org.apache.lucene.codecs.CodecUtil;
 import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.FieldsProducer;
 import org.apache.lucene.codecs.PostingsConsumer;
 import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.PushFieldsConsumer;
 import org.apache.lucene.codecs.TermStats;
 import org.apache.lucene.codecs.TermsConsumer;
 import org.apache.lucene.codecs.bloom.FuzzySet.ContainsResult;
@@ -111,14 +111,16 @@ public final class BloomFilteringPostingsFormat extends PostingsFormat {
     this.delegatePostingsFormat = delegatePostingsFormat;
     this.bloomFilterFactory = bloomFilterFactory;
   }
  

   /**
    * Creates Bloom filters for a selection of fields created in the index. This
    * is recorded as a set of Bitsets held as a segment summary in an additional
    * "blm" file. This PostingsFormat delegates to a choice of delegate
    * PostingsFormat for encoding all other postings data. This choice of
    * constructor defaults to the {@link DefaultBloomFilterFactory} for
   * configuring per-field BloomFilters.
   * configuring per-field BloomFilters.  Note that the
   * wrapped PostingsFormat must use a {@link PushFieldsConsumer}
   * for writing.
    * 
    * @param delegatePostingsFormat
    *          The PostingsFormat that records all the non-bloom filter data i.e.
@@ -141,9 +143,12 @@ public final class BloomFilteringPostingsFormat extends PostingsFormat {
       throw new UnsupportedOperationException("Error - " + getClass().getName()
           + " has been constructed without a choice of PostingsFormat");
     }
    FieldsConsumer fieldsConsumer = delegatePostingsFormat.fieldsConsumer(state);
    if (!(fieldsConsumer instanceof PushFieldsConsumer)) {
      throw new UnsupportedOperationException("Wrapped PostingsFormat must return a PushFieldsConsumer");
    }
     return new BloomFilteredFieldsConsumer(
        delegatePostingsFormat.fieldsConsumer(state), state,
        delegatePostingsFormat);
              (PushFieldsConsumer) fieldsConsumer, state);
   }
   
   @Override
@@ -251,11 +256,6 @@ public final class BloomFilteringPostingsFormat extends PostingsFormat {
         return new BloomFilteredTermsEnum(delegateTerms, reuse, filter);
       }
       
      @Override
      public Comparator<BytesRef> getComparator() {
        return delegateTerms.getComparator();
      }
      
       @Override
       public long size() throws IOException {
         return delegateTerms.size();
@@ -326,11 +326,6 @@ public final class BloomFilteringPostingsFormat extends PostingsFormat {
         return delegate().next();
       }
       
      @Override
      public final Comparator<BytesRef> getComparator() {
        return delegateTerms.getComparator();
      }
      
       @Override
       public final boolean seekExact(BytesRef text)
           throws IOException {
@@ -388,8 +383,6 @@ public final class BloomFilteringPostingsFormat extends PostingsFormat {
           throws IOException {
         return delegate().docs(liveDocs, reuse, flags);
       }
      
      
     }
 
     @Override
@@ -401,17 +394,16 @@ public final class BloomFilteringPostingsFormat extends PostingsFormat {
       }
       return sizeInBytes;
     }
    
   }
   
  class BloomFilteredFieldsConsumer extends FieldsConsumer {
    private FieldsConsumer delegateFieldsConsumer;
  class BloomFilteredFieldsConsumer extends PushFieldsConsumer {
    private PushFieldsConsumer delegateFieldsConsumer;
     private Map<FieldInfo,FuzzySet> bloomFilters = new HashMap<FieldInfo,FuzzySet>();
     private SegmentWriteState state;
     
    
    public BloomFilteredFieldsConsumer(FieldsConsumer fieldsConsumer,
        SegmentWriteState state, PostingsFormat delegatePostingsFormat) {
    public BloomFilteredFieldsConsumer(PushFieldsConsumer fieldsConsumer,
        SegmentWriteState state) {
      super(state);
       this.delegateFieldsConsumer = fieldsConsumer;
       this.state = state;
     }
@@ -422,7 +414,7 @@ public final class BloomFilteringPostingsFormat extends PostingsFormat {
       if (bloomFilter != null) {
         assert bloomFilters.containsKey(field) == false;
         bloomFilters.put(field, bloomFilter);
        return new WrappedTermsConsumer(delegateFieldsConsumer.addField(field),bloomFilter);
        return new WrappedTermsConsumer(delegateFieldsConsumer.addField(field), bloomFilter);
       } else {
         // No, use the unfiltered fieldsConsumer - we are not interested in
         // recording any term Bitsets.
@@ -510,12 +502,5 @@ public final class BloomFilteringPostingsFormat extends PostingsFormat {
         throws IOException {
       delegateTermsConsumer.finish(sumTotalTermFreq, sumDocFreq, docCount);
     }
    
    @Override
    public Comparator<BytesRef> getComparator() throws IOException {
      return delegateTermsConsumer.getComparator();
    }
    
   }
  
 }
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/DirectPostingsFormat.java b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/DirectPostingsFormat.java
index 584cd9f629d..e3e370e1cf7 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/DirectPostingsFormat.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/DirectPostingsFormat.java
@@ -19,7 +19,6 @@ package org.apache.lucene.codecs.memory;
 
 import java.io.IOException;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.TreeMap;
@@ -660,11 +659,6 @@ public final class DirectPostingsFormat extends PostingsFormat {
       return docCount;
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public boolean hasOffsets() {
       return hasOffsets;
@@ -700,11 +694,6 @@ public final class DirectPostingsFormat extends PostingsFormat {
         termOrd = -1;
       }
 
      @Override
      public Comparator<BytesRef> getComparator() {
        return BytesRef.getUTF8SortedAsUnicodeComparator();
      }

       @Override
       public BytesRef next() {
         termOrd++;
@@ -1096,11 +1085,6 @@ public final class DirectPostingsFormat extends PostingsFormat {
         }
       }
 
      @Override
      public Comparator<BytesRef> getComparator() {
        return BytesRef.getUTF8SortedAsUnicodeComparator();
      }

       private void grow() {
         if (states.length == 1+stateUpto) {
           final State[] newStates = new State[states.length+1];
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTOrdTermsReader.java b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTOrdTermsReader.java
index 0a74b91c4c4..4403a308cdc 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTOrdTermsReader.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTOrdTermsReader.java
@@ -18,13 +18,10 @@ package org.apache.lucene.codecs.memory;
  */
 
 import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
 import java.util.Arrays;
 import java.util.ArrayList;
 import java.util.BitSet;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.Iterator;
 import java.util.TreeMap;
 
@@ -41,7 +38,6 @@ import org.apache.lucene.index.TermState;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.IOContext;
 import org.apache.lucene.store.IndexInput;
 import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.automaton.ByteRunAutomaton;
@@ -210,11 +206,6 @@ public class FSTOrdTermsReader extends FieldsProducer {
       blockIn.readBytes(metaBytesBlock, 0, metaBytesBlock.length);
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     public boolean hasFreqs() {
       return fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
     }
@@ -376,11 +367,6 @@ public class FSTOrdTermsReader extends FieldsProducer {
         }
       }
 
      @Override
      public Comparator<BytesRef> getComparator() {
        return BytesRef.getUTF8SortedAsUnicodeComparator();
      }

       @Override
       public TermState termState() throws IOException {
         decodeMetaData();
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTOrdTermsWriter.java b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTOrdTermsWriter.java
index c11d11e84e9..1330e8415ce 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTOrdTermsWriter.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTOrdTermsWriter.java
@@ -18,10 +18,16 @@ package org.apache.lucene.codecs.memory;
  */
 
 import java.io.IOException;
import java.util.List;
 import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
 
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.PostingsConsumer;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.PushFieldsConsumer;
import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.codecs.TermsConsumer;
 import org.apache.lucene.index.FieldInfo.IndexOptions;
 import org.apache.lucene.index.FieldInfo;
 import org.apache.lucene.index.FieldInfos;
@@ -30,7 +36,6 @@ import org.apache.lucene.index.SegmentWriteState;
 import org.apache.lucene.store.DataOutput;
 import org.apache.lucene.store.IndexOutput;
 import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.IntsRef;
@@ -38,13 +43,6 @@ import org.apache.lucene.util.fst.Builder;
 import org.apache.lucene.util.fst.FST;
 import org.apache.lucene.util.fst.PositiveIntOutputs;
 import org.apache.lucene.util.fst.Util;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.PostingsConsumer;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.TermsConsumer;
import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.codecs.CodecUtil;
 
 /** 
  * FST-based term dict, using ord as FST output.
@@ -144,7 +142,7 @@ import org.apache.lucene.codecs.CodecUtil;
  * @lucene.experimental 
  */
 
public class FSTOrdTermsWriter extends FieldsConsumer {
public class FSTOrdTermsWriter extends PushFieldsConsumer {
   static final String TERMS_INDEX_EXTENSION = "tix";
   static final String TERMS_BLOCK_EXTENSION = "tbk";
   static final String TERMS_CODEC_NAME = "FST_ORD_TERMS_DICT";
@@ -159,6 +157,7 @@ public class FSTOrdTermsWriter extends FieldsConsumer {
   IndexOutput indexOut = null;
 
   public FSTOrdTermsWriter(SegmentWriteState state, PostingsWriterBase postingsWriter) throws IOException {
    super(state);
     final String termsIndexFileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, TERMS_INDEX_EXTENSION);
     final String termsBlockFileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, TERMS_BLOCK_EXTENSION);
 
@@ -189,7 +188,6 @@ public class FSTOrdTermsWriter extends FieldsConsumer {
   public void close() throws IOException {
     IOException ioe = null;
     try {
      final long indexDirStart = indexOut.getFilePointer();
       final long blockDirStart = blockOut.getFilePointer();
 
       // write field summary
@@ -286,11 +284,6 @@ public class FSTOrdTermsWriter extends FieldsConsumer {
       this.lastMetaBytesFP = 0;
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public PostingsConsumer startTerm(BytesRef text) throws IOException {
       postingsWriter.startTerm();
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTTermsReader.java b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTTermsReader.java
index e5716f76da5..e9e9a7c8038 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTTermsReader.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTTermsReader.java
@@ -18,12 +18,9 @@ package org.apache.lucene.codecs.memory;
  */
 
 import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
 import java.util.ArrayList;
 import java.util.BitSet;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.Iterator;
 import java.util.TreeMap;
 
@@ -40,7 +37,6 @@ import org.apache.lucene.index.TermState;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.IOContext;
 import org.apache.lucene.store.IndexInput;
 import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.automaton.ByteRunAutomaton;
@@ -179,11 +175,6 @@ public class FSTTermsReader extends FieldsProducer {
       this.dict = new FST<FSTTermOutputs.TermData>(in, new FSTTermOutputs(fieldInfo, longsSize));
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public boolean hasOffsets() {
       return fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
@@ -251,11 +242,6 @@ public class FSTTermsReader extends FieldsProducer {
         // NOTE: metadata will only be initialized in child class
       }
 
      @Override
      public Comparator<BytesRef> getComparator() {
        return BytesRef.getUTF8SortedAsUnicodeComparator();
      }

       @Override
       public TermState termState() throws IOException {
         decodeMetaData();
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTTermsWriter.java b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTTermsWriter.java
index 0afa6645fb8..3edc0dfcd9b 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTTermsWriter.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTTermsWriter.java
@@ -18,10 +18,16 @@ package org.apache.lucene.codecs.memory;
  */
 
 import java.io.IOException;
import java.util.List;
 import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
 
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.PostingsConsumer;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.PushFieldsConsumer;
import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.codecs.TermsConsumer;
 import org.apache.lucene.index.FieldInfo.IndexOptions;
 import org.apache.lucene.index.FieldInfo;
 import org.apache.lucene.index.FieldInfos;
@@ -30,20 +36,12 @@ import org.apache.lucene.index.SegmentWriteState;
 import org.apache.lucene.store.DataOutput;
 import org.apache.lucene.store.IndexOutput;
 import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.IntsRef;
 import org.apache.lucene.util.fst.Builder;
 import org.apache.lucene.util.fst.FST;
 import org.apache.lucene.util.fst.Util;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.PostingsConsumer;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.TermsConsumer;
import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.codecs.CodecUtil;
 
 /**
  * FST-based term dict, using metadata as FST output.
@@ -121,7 +119,7 @@ import org.apache.lucene.codecs.CodecUtil;
  * @lucene.experimental
  */
 
public class FSTTermsWriter extends FieldsConsumer {
public class FSTTermsWriter extends PushFieldsConsumer {
   static final String TERMS_EXTENSION = "tmp";
   static final String TERMS_CODEC_NAME = "FST_TERMS_DICT";
   public static final int TERMS_VERSION_START = 0;
@@ -133,6 +131,7 @@ public class FSTTermsWriter extends FieldsConsumer {
   final List<FieldMetaData> fields = new ArrayList<FieldMetaData>();
 
   public FSTTermsWriter(SegmentWriteState state, PostingsWriterBase postingsWriter) throws IOException {
    super(state);
     final String termsFileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, TERMS_EXTENSION);
 
     this.postingsWriter = postingsWriter;
@@ -217,7 +216,6 @@ public class FSTTermsWriter extends FieldsConsumer {
     private long numTerms;
 
     private final IntsRef scratchTerm = new IntsRef();
    private final RAMOutputStream statsWriter = new RAMOutputStream();
     private final RAMOutputStream metaWriter = new RAMOutputStream();
 
     TermsWriter(FieldInfo fieldInfo) {
@@ -228,11 +226,6 @@ public class FSTTermsWriter extends FieldsConsumer {
       this.builder = new Builder<FSTTermOutputs.TermData>(FST.INPUT_TYPE.BYTE1, outputs);
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public PostingsConsumer startTerm(BytesRef text) throws IOException {
       postingsWriter.startTerm();
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/MemoryDocValuesProducer.java b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/MemoryDocValuesProducer.java
index 302197ad3a6..34380773cb8 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/MemoryDocValuesProducer.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/MemoryDocValuesProducer.java
@@ -18,7 +18,6 @@ package org.apache.lucene.codecs.memory;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 import java.util.HashMap;
 import java.util.Map;
 
@@ -565,11 +564,6 @@ class MemoryDocValuesProducer extends DocValuesProducer {
       }
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public SeekStatus seekCeil(BytesRef text) throws IOException {
       if (in.seekCeil(text) == null) {
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/MemoryPostingsFormat.java b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/MemoryPostingsFormat.java
index 8f2611ea75f..c77b389977e 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/memory/MemoryPostingsFormat.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/memory/MemoryPostingsFormat.java
@@ -19,7 +19,6 @@ package org.apache.lucene.codecs.memory;
 
 import java.io.IOException;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.SortedMap;
@@ -29,6 +28,7 @@ import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.FieldsProducer;
 import org.apache.lucene.codecs.PostingsConsumer;
 import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.PushFieldsConsumer;
 import org.apache.lucene.codecs.TermStats;
 import org.apache.lucene.codecs.TermsConsumer;
 import org.apache.lucene.index.DocsAndPositionsEnum;
@@ -278,11 +278,6 @@ public final class MemoryPostingsFormat extends PostingsFormat {
         //System.out.println("finish field=" + field.name + " fp=" + out.getFilePointer());
       }
     }

    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }
   }
 
   private static String EXTENSION = "ram";
@@ -293,7 +288,7 @@ public final class MemoryPostingsFormat extends PostingsFormat {
     final String fileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, EXTENSION);
     final IndexOutput out = state.directory.createOutput(fileName, state.context);
     
    return new FieldsConsumer() {
    return new PushFieldsConsumer(state) {
       @Override
       public TermsConsumer addField(FieldInfo field) {
         //System.out.println("\naddField field=" + field.name);
@@ -758,11 +753,6 @@ public final class MemoryPostingsFormat extends PostingsFormat {
       return totalTermFreq;
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public void seekExact(long ord) {
       // NOTE: we could add this...
@@ -826,11 +816,6 @@ public final class MemoryPostingsFormat extends PostingsFormat {
       return new FSTTermsEnum(field, fst);
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public boolean hasOffsets() {
       return field.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextFieldsReader.java b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextFieldsReader.java
index fa9a78a3120..8fbc16288c7 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextFieldsReader.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextFieldsReader.java
@@ -19,7 +19,6 @@ package org.apache.lucene.codecs.simpletext;
 
 import java.io.IOException;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
@@ -218,11 +217,6 @@ class SimpleTextFieldsReader extends FieldsProducer {
       } 
       return docsAndPositionsEnum.reset(docsStart, liveDocs, indexOptions, docFreq);
     }
    
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }
   }
 
   private class SimpleTextDocsEnum extends DocsEnum {
@@ -589,11 +583,6 @@ class SimpleTextFieldsReader extends FieldsProducer {
       }
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public long size() {
       return (long) termCount;
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextFieldsWriter.java b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextFieldsWriter.java
index 5a59399662c..ed16cd60c73 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextFieldsWriter.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextFieldsWriter.java
@@ -17,23 +17,28 @@ package org.apache.lucene.codecs.simpletext;
  * limitations under the License.
  */
 
import org.apache.lucene.util.BytesRef;
import java.io.Closeable;
import java.io.IOException;

 import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.PostingsConsumer;
import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.codecs.TermsConsumer;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
 import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
 import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
 
import java.io.IOException;
import java.util.Comparator;

class SimpleTextFieldsWriter extends FieldsConsumer {
class SimpleTextFieldsWriter extends FieldsConsumer implements Closeable {
   
   private final IndexOutput out;
   private final BytesRef scratch = new BytesRef(10);
  private final SegmentWriteState writeState;
 
   final static BytesRef END          = new BytesRef("END");
   final static BytesRef FIELD        = new BytesRef("field ");
@@ -45,134 +50,171 @@ class SimpleTextFieldsWriter extends FieldsConsumer {
   final static BytesRef END_OFFSET   = new BytesRef("      endOffset ");
   final static BytesRef PAYLOAD      = new BytesRef("        payload ");
 
  public SimpleTextFieldsWriter(SegmentWriteState state) throws IOException {
    final String fileName = SimpleTextPostingsFormat.getPostingsFileName(state.segmentInfo.name, state.segmentSuffix);
    out = state.directory.createOutput(fileName, state.context);
  }

  private void write(String s) throws IOException {
    SimpleTextUtil.write(out, s, scratch);
  }

  private void write(BytesRef b) throws IOException {
    SimpleTextUtil.write(out, b);
  }

  private void newline() throws IOException {
    SimpleTextUtil.writeNewline(out);
  public SimpleTextFieldsWriter(SegmentWriteState writeState) throws IOException {
    final String fileName = SimpleTextPostingsFormat.getPostingsFileName(writeState.segmentInfo.name, writeState.segmentSuffix);
    out = writeState.directory.createOutput(fileName, writeState.context);
    this.writeState = writeState;
   }
 
   @Override
  public TermsConsumer addField(FieldInfo field) throws IOException {
    write(FIELD);
    write(field.name);
    newline();
    return new SimpleTextTermsWriter(field);
  }

  private class SimpleTextTermsWriter extends TermsConsumer {
    private final SimpleTextPostingsWriter postingsWriter;
    
    public SimpleTextTermsWriter(FieldInfo field) {
      postingsWriter = new SimpleTextPostingsWriter(field);
    }

    @Override
    public PostingsConsumer startTerm(BytesRef term) throws IOException {
      return postingsWriter.reset(term);
    }

    @Override
    public void finishTerm(BytesRef term, TermStats stats) throws IOException {
    }

    @Override
    public void finish(long sumTotalTermFreq, long sumDocFreq, int docCount) throws IOException {
    }

    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
  public void write(Fields fields) throws IOException {
    boolean success = false;
    try {
      write(writeState.fieldInfos, fields);
      success = true;
    } finally {
      if (success) {
        IOUtils.close(this);
      } else {
        IOUtils.closeWhileHandlingException(this);
      }
     }
   }
 
  private class SimpleTextPostingsWriter extends PostingsConsumer {
    private BytesRef term;
    private boolean wroteTerm;
    private final IndexOptions indexOptions;
    private final boolean writePositions;
    private final boolean writeOffsets;

    // for assert:
    private int lastStartOffset = 0;

    public SimpleTextPostingsWriter(FieldInfo field) {
      this.indexOptions = field.getIndexOptions();
      writePositions = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
      writeOffsets = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
      //System.out.println("writeOffsets=" + writeOffsets);
      //System.out.println("writePos=" + writePositions);
    }
  public void write(FieldInfos fieldInfos, Fields fields) throws IOException {
 
    @Override
    public void startDoc(int docID, int termDocFreq) throws IOException {
      if (!wroteTerm) {
        // we lazily do this, in case the term had zero docs
        write(TERM);
        write(term);
        newline();
        wroteTerm = true;
    // for each field
    for(String field : fields) {
      Terms terms = fields.terms(field);
      if (terms == null) {
        // Annoyingly, this can happen!
        continue;
       }

      write(DOC);
      write(Integer.toString(docID));
      newline();
      if (indexOptions != IndexOptions.DOCS_ONLY) {
        write(FREQ);
        write(Integer.toString(termDocFreq));
        newline();
      FieldInfo fieldInfo = fieldInfos.fieldInfo(field);

      boolean wroteField = false;

      boolean hasPositions = terms.hasPositions();

      // TODO: shouldn't we add hasFreqs to Terms?
      // then we don't need FieldInfos here?
      boolean hasFreqs = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_ONLY) > 0;
      boolean hasPayloads = fieldInfo.hasPayloads();
      boolean hasOffsets = terms.hasOffsets();

      int flags = 0;
      if (hasPositions) {
        
        if (hasPayloads) {
          flags = flags | DocsAndPositionsEnum.FLAG_PAYLOADS;
        }
        if (hasOffsets) {
          flags = flags | DocsAndPositionsEnum.FLAG_OFFSETS;
        }
      } else {
        if (hasFreqs) {
          flags = flags | DocsEnum.FLAG_FREQS;
        }
       }
 
      lastStartOffset = 0;
    }
    
    public PostingsConsumer reset(BytesRef term) {
      this.term = term;
      wroteTerm = false;
      return this;
    }

    @Override
    public void addPosition(int position, BytesRef payload, int startOffset, int endOffset) throws IOException {
      if (writePositions) {
        write(POS);
        write(Integer.toString(position));
        newline();
      TermsEnum termsEnum = terms.iterator(null);
      DocsAndPositionsEnum posEnum = null;
      DocsEnum docsEnum = null;

      // for each term in field
      while(true) {
        BytesRef term = termsEnum.next();
        if (term == null) {
          break;
        }

        if (hasPositions) {
          posEnum = termsEnum.docsAndPositions(null, posEnum, flags);
          docsEnum = posEnum;
        } else {
          docsEnum = termsEnum.docs(null, docsEnum, flags);
        }
        assert docsEnum != null: "termsEnum=" + termsEnum + " hasPos=" + hasPositions + " flags=" + flags;

        boolean wroteTerm = false;

        // for each doc in field+term
        while(true) {
          int doc = docsEnum.nextDoc();
          if (doc == DocsEnum.NO_MORE_DOCS) {
            break;
          }

          if (!wroteTerm) {

            if (!wroteField) {
              // we lazily do this, in case the field had
              // no terms              
              write(FIELD);
              write(field);
              newline();
              wroteField = true;
            }

            // we lazily do this, in case the term had
            // zero docs
            write(TERM);
            write(term);
            newline();
            wroteTerm = true;
          }

          write(DOC);
          write(Integer.toString(doc));
          newline();
          if (hasFreqs) {
            int freq = docsEnum.freq();
            write(FREQ);
            write(Integer.toString(freq));
            newline();

            if (hasPositions) {
              // for assert:
              int lastStartOffset = 0;

              // for each pos in field+term+doc
              for(int i=0;i<freq;i++) {
                int position = posEnum.nextPosition();

                write(POS);
                write(Integer.toString(position));
                newline();

                if (hasOffsets) {
                  int startOffset = posEnum.startOffset();
                  int endOffset = posEnum.endOffset();
                  assert endOffset >= startOffset;
                  assert startOffset >= lastStartOffset: "startOffset=" + startOffset + " lastStartOffset=" + lastStartOffset;
                  lastStartOffset = startOffset;
                  write(START_OFFSET);
                  write(Integer.toString(startOffset));
                  newline();
                  write(END_OFFSET);
                  write(Integer.toString(endOffset));
                  newline();
                }

                BytesRef payload = posEnum.getPayload();

                if (payload != null && payload.length > 0) {
                  assert payload.length != 0;
                  write(PAYLOAD);
                  write(payload);
                  newline();
                }
              }
            }
          }
        }
       }
    }
  }
 
      if (writeOffsets) {
        assert endOffset >= startOffset;
        assert startOffset >= lastStartOffset: "startOffset=" + startOffset + " lastStartOffset=" + lastStartOffset;
        lastStartOffset = startOffset;
        write(START_OFFSET);
        write(Integer.toString(startOffset));
        newline();
        write(END_OFFSET);
        write(Integer.toString(endOffset));
        newline();
      }
  private void write(String s) throws IOException {
    SimpleTextUtil.write(out, s, scratch);
  }
 
      if (payload != null && payload.length > 0) {
        assert payload.length != 0;
        write(PAYLOAD);
        write(payload);
        newline();
      }
    }
  private void write(BytesRef b) throws IOException {
    SimpleTextUtil.write(out, b);
  }
 
    @Override
    public void finishDoc() {
    }
  private void newline() throws IOException {
    SimpleTextUtil.writeNewline(out);
   }
 
   @Override
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextPostingsFormat.java b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextPostingsFormat.java
index 5a5cf54a337..f23fdb69da9 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextPostingsFormat.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextPostingsFormat.java
@@ -22,9 +22,9 @@ import java.io.IOException;
 import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.FieldsProducer;
 import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.SegmentReadState;
 import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
 
 /** For debugging, curiosity, transparency only!!  Do not
  *  use this codec in production.
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextTermVectorsReader.java b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextTermVectorsReader.java
index b650f1c9683..fa053233395 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextTermVectorsReader.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextTermVectorsReader.java
@@ -19,7 +19,6 @@ package org.apache.lucene.codecs.simpletext;
 
 import java.io.IOException;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.SortedMap;
@@ -271,11 +270,6 @@ public class SimpleTextTermVectorsReader extends TermVectorsReader {
       return new SimpleTVTermsEnum(terms);
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public long size() throws IOException {
       return terms.size();
@@ -394,11 +388,6 @@ public class SimpleTextTermVectorsReader extends TermVectorsReader {
       e.reset(liveDocs, postings.positions, postings.startOffsets, postings.endOffsets, postings.payloads);
       return e;
     }

    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }
   }
   
   // note: these two enum classes are exactly like the Default impl...
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextTermVectorsWriter.java b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextTermVectorsWriter.java
index 486eda58820..04dd5523121 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextTermVectorsWriter.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextTermVectorsWriter.java
@@ -18,7 +18,6 @@ package org.apache.lucene.codecs.simpletext;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.codecs.TermVectorsWriter;
 import org.apache.lucene.index.FieldInfo;
@@ -189,11 +188,6 @@ public class SimpleTextTermVectorsWriter extends TermVectorsWriter {
     }
   }
   
  @Override
  public Comparator<BytesRef> getComparator() throws IOException {
    return BytesRef.getUTF8SortedAsUnicodeComparator();
  }
  
   private void write(String s) throws IOException {
     SimpleTextUtil.write(out, s, scratch);
   }
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsReader.java b/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsReader.java
index 9e355e3eb43..47eb520b068 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsReader.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsReader.java
@@ -22,7 +22,6 @@ import java.io.IOException;
 import java.io.PrintStream;
 import java.io.UnsupportedEncodingException;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.Iterator;
 import java.util.Locale;
 import java.util.TreeMap;
@@ -497,11 +496,6 @@ public class BlockTreeTermsReader extends FieldsProducer {
       return new SegmentTermsEnum().computeBlockStats();
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public boolean hasOffsets() {
       return fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
@@ -1238,11 +1232,6 @@ public class BlockTreeTermsReader extends FieldsProducer {
         term.length = len;
       }
 
      @Override
      public Comparator<BytesRef> getComparator() {
        return BytesRef.getUTF8SortedAsUnicodeComparator();
      }

       @Override
       public boolean seekExact(BytesRef text) {
         throw new UnsupportedOperationException();
@@ -1454,11 +1443,6 @@ public class BlockTreeTermsReader extends FieldsProducer {
         return arcs[ord];
       }
 
      @Override
      public Comparator<BytesRef> getComparator() {
        return BytesRef.getUTF8SortedAsUnicodeComparator();
      }

       // Pushes a frame we seek'd to
       Frame pushFrame(FST.Arc<BytesRef> arc, BytesRef frameData, int length) throws IOException {
         scratchReader.reset(frameData.bytes, frameData.offset, frameData.length);
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsWriter.java b/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsWriter.java
index 661e8bf6261..114f8b1f81b 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsWriter.java
@@ -19,7 +19,6 @@ package org.apache.lucene.codecs;
 
 import java.io.IOException;
 import java.util.ArrayList;
import java.util.Comparator;
 import java.util.List;
 
 import org.apache.lucene.index.FieldInfo.IndexOptions;
@@ -175,7 +174,7 @@ import org.apache.lucene.util.packed.PackedInts;
  * @lucene.experimental
  */
 
public class BlockTreeTermsWriter extends FieldsConsumer {
public class BlockTreeTermsWriter extends PushFieldsConsumer {
 
   /** Suggested default value for the {@code
    *  minItemsInBlock} parameter to {@link
@@ -274,6 +273,7 @@ public class BlockTreeTermsWriter extends FieldsConsumer {
                               int maxItemsInBlock)
     throws IOException
   {
    super(state);
     if (minItemsInBlock <= 1) {
       throw new IllegalArgumentException("minItemsInBlock must be >= 2; got " + minItemsInBlock);
     }
@@ -1017,11 +1017,6 @@ public class BlockTreeTermsWriter extends FieldsConsumer {
       this.longsSize = postingsWriter.setField(fieldInfo);
     }
     
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public PostingsConsumer startTerm(BytesRef text) throws IOException {
       //if (DEBUG) System.out.println("\nBTTW.startTerm term=" + fieldInfo.name + ":" + toString(text) + " seg=" + segment);
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/FieldsConsumer.java b/lucene/core/src/java/org/apache/lucene/codecs/FieldsConsumer.java
index c7c203abbcf..80fa473253d 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/FieldsConsumer.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/FieldsConsumer.java
@@ -17,60 +17,63 @@ package org.apache.lucene.codecs;
  * limitations under the License.
  */
 
import java.io.Closeable;
 import java.io.IOException;
 
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfo; // javadocs
 import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MergeState;
 import org.apache.lucene.index.SegmentWriteState; // javadocs
import org.apache.lucene.index.Terms;
 
 /** 
  * Abstract API that consumes terms, doc, freq, prox, offset and
  * payloads postings.  Concrete implementations of this
  * actually do "something" with the postings (write it into
  * the index in a specific format).
 * <p>
 * The lifecycle is:
 * <ol>
 *   <li>FieldsConsumer is created by 
 *       {@link PostingsFormat#fieldsConsumer(SegmentWriteState)}.
 *   <li>For each field, {@link #addField(FieldInfo)} is called,
 *       returning a {@link TermsConsumer} for the field.
 *   <li>After all fields are added, the consumer is {@link #close}d.
 * </ol>
  *
  * @lucene.experimental
  */
public abstract class FieldsConsumer implements Closeable {

public abstract class FieldsConsumer {
 
   /** Sole constructor. (For invocation by subclass 
    *  constructors, typically implicit.) */
   protected FieldsConsumer() {
   }
 
  /** Add a new field */
  public abstract TermsConsumer addField(FieldInfo field) throws IOException;
  
  /** Called when we are done adding everything. */
  @Override
  public abstract void close() throws IOException;
  // TODO: can we somehow compute stats for you...?
 
  /** Called during merging to merge all {@link Fields} from
   *  sub-readers.  This must recurse to merge all postings
   *  (terms, docs, positions, etc.).  A {@link
   *  PostingsFormat} can override this default
   *  implementation to do its own merging. */
  public void merge(MergeState mergeState, Fields fields) throws IOException {
    for (String field : fields) {
      FieldInfo info = mergeState.fieldInfos.fieldInfo(field);
      assert info != null : "FieldInfo for field is null: "+ field;
      Terms terms = fields.terms(field);
      if (terms != null) {
        final TermsConsumer termsConsumer = addField(info);
        termsConsumer.merge(mergeState, info.getIndexOptions(), terms.iterator(null));
      }
    }
  }
  // TODO: maybe we should factor out "limited" (only
  // iterables, no counts/stats) base classes from
  // Fields/Terms/Docs/AndPositions?

  /** Write all fields, terms and postings.  This the "pull"
   *  API, allowing you to iterate more than once over the
   *  postings, somewhat analogous to using a DOM API to
   *  traverse an XML tree.  Alternatively, if you subclass
   *  {@link PushFieldsConsumer}, then all postings are
   *  pushed in a single pass, somewhat analogous to using a
   *  SAX API to traverse an XML tree.
   *
   *  <p>This API is has certain restrictions vs {@link
   *  PushFieldsConsumer}:
   *
   *  <ul>
   *    <li> You must compute index statistics yourself,
   *         including each Term's docFreq and totalTermFreq,
   *         as well as the summary sumTotalTermFreq,
   *         sumTotalDocFreq and docCount.
   *
   *    <li> You must skip terms that have no docs and
   *         fields that have no terms, even though the provided
   *         Fields API will expose them; this typically
   *         requires lazily writing the field or term until
   *         you've actually seen the first term or
   *         document.
   *
   *    <li> The provided Fields instance is limited: you
   *         cannot call any methods that return
   *         statistics/counts; you cannot pass a non-null
   *         live docs when pulling docs/positions enums.
   *  </ul>
   */
  public abstract void write(Fields fields) throws IOException;
 }
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/PostingsConsumer.java b/lucene/core/src/java/org/apache/lucene/codecs/PostingsConsumer.java
index 4a746a6093b..642d107cf16 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/PostingsConsumer.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/PostingsConsumer.java
@@ -19,13 +19,7 @@ package org.apache.lucene.codecs;
 
 import java.io.IOException;
 
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
 
 /**
  * Abstract API that consumes postings for an individual term.
@@ -70,79 +64,4 @@ public abstract class PostingsConsumer {
   /** Called when we are done adding positions & payloads
    *  for each doc. */
   public abstract void finishDoc() throws IOException;

  /** Default merge impl: append documents, mapping around
   *  deletes */
  public TermStats merge(final MergeState mergeState, IndexOptions indexOptions, final DocsEnum postings, final FixedBitSet visitedDocs) throws IOException {

    int df = 0;
    long totTF = 0;

    if (indexOptions == IndexOptions.DOCS_ONLY) {
      while(true) {
        final int doc = postings.nextDoc();
        if (doc == DocIdSetIterator.NO_MORE_DOCS) {
          break;
        }
        visitedDocs.set(doc);
        this.startDoc(doc, -1);
        this.finishDoc();
        df++;
      }
      totTF = -1;
    } else if (indexOptions == IndexOptions.DOCS_AND_FREQS) {
      while(true) {
        final int doc = postings.nextDoc();
        if (doc == DocIdSetIterator.NO_MORE_DOCS) {
          break;
        }
        visitedDocs.set(doc);
        final int freq = postings.freq();
        this.startDoc(doc, freq);
        this.finishDoc();
        df++;
        totTF += freq;
      }
    } else if (indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
      final DocsAndPositionsEnum postingsEnum = (DocsAndPositionsEnum) postings;
      while(true) {
        final int doc = postingsEnum.nextDoc();
        if (doc == DocIdSetIterator.NO_MORE_DOCS) {
          break;
        }
        visitedDocs.set(doc);
        final int freq = postingsEnum.freq();
        this.startDoc(doc, freq);
        totTF += freq;
        for(int i=0;i<freq;i++) {
          final int position = postingsEnum.nextPosition();
          final BytesRef payload = postingsEnum.getPayload();
          this.addPosition(position, payload, -1, -1);
        }
        this.finishDoc();
        df++;
      }
    } else {
      assert indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;
      final DocsAndPositionsEnum postingsEnum = (DocsAndPositionsEnum) postings;
      while(true) {
        final int doc = postingsEnum.nextDoc();
        if (doc == DocIdSetIterator.NO_MORE_DOCS) {
          break;
        }
        visitedDocs.set(doc);
        final int freq = postingsEnum.freq();
        this.startDoc(doc, freq);
        totTF += freq;
        for(int i=0;i<freq;i++) {
          final int position = postingsEnum.nextPosition();
          final BytesRef payload = postingsEnum.getPayload();
          this.addPosition(position, payload, postingsEnum.startOffset(), postingsEnum.endOffset());
        }
        this.finishDoc();
        df++;
      }
    }
    return new TermStats(df, indexOptions == IndexOptions.DOCS_ONLY ? -1 : totTF);
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/PostingsFormat.java b/lucene/core/src/java/org/apache/lucene/codecs/PostingsFormat.java
index 090e3949242..f9b676f62f9 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/PostingsFormat.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/PostingsFormat.java
@@ -22,8 +22,8 @@ import java.util.ServiceLoader;
 import java.util.Set;
 
 import org.apache.lucene.codecs.perfield.PerFieldPostingsFormat; // javadocs
import org.apache.lucene.index.SegmentWriteState;
 import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
 import org.apache.lucene.util.NamedSPILoader;
 
 /** 
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/PushFieldsConsumer.java b/lucene/core/src/java/org/apache/lucene/codecs/PushFieldsConsumer.java
new file mode 100644
index 00000000000..5a10c904fb4
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/codecs/PushFieldsConsumer.java
@@ -0,0 +1,181 @@
package org.apache.lucene.codecs;

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

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.IOUtils;

/** Translates the "pull" API from {@link FieldsConsumer}
 *  into a "push" API that pushes fields, terms, postings to
 *  the consumer.
 *
 *  <p>
 *  The lifecycle is:
 *  <ol>
 *    <li>PushFieldsConsumer is created by 
 *        {@link PostingsFormat#fieldsConsumer(SegmentWriteState)}.
 *    <li>For each field, {@link #addField(FieldInfo)} is called,
 *        returning a {@link TermsConsumer} for the field.
 *    <li>After all fields are added, the consumer is {@link #close}d.
 *  </ol>
 *
 * @lucene.experimental
 */
public abstract class PushFieldsConsumer extends FieldsConsumer implements Closeable {

  final SegmentWriteState writeState;

  /** Sole constructor */
  protected PushFieldsConsumer(SegmentWriteState writeState) {
    this.writeState = writeState;
  }

  /** Add a new field */
  public abstract TermsConsumer addField(FieldInfo field) throws IOException;

  /** Called when we are done adding everything. */
  @Override
  public abstract void close() throws IOException;

  @Override
  public final void write(Fields fields) throws IOException {

    boolean success = false;
    try {
      for(String field : fields) { // for all fields
        FieldInfo fieldInfo = writeState.fieldInfos.fieldInfo(field);
        IndexOptions indexOptions = fieldInfo.getIndexOptions();
        TermsConsumer termsConsumer = addField(fieldInfo);

        Terms terms = fields.terms(field);
        if (terms != null) {

          // Holds all docs that have this field:
          FixedBitSet visitedDocs = new FixedBitSet(writeState.segmentInfo.getDocCount());

          boolean hasFreq = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
          boolean hasPositions = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
          assert hasPositions == terms.hasPositions();
          boolean hasOffsets = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
          assert hasOffsets == terms.hasOffsets();
          boolean hasPayloads = fieldInfo.hasPayloads();

          long sumTotalTermFreq = 0;
          long sumDocFreq = 0;

          int flags = 0;
          if (hasPositions == false) {
            if (hasFreq) {
              flags = flags | DocsEnum.FLAG_FREQS;
            }
          } else {
            if (hasPayloads) {
              flags = flags | DocsAndPositionsEnum.FLAG_PAYLOADS;
            }
            if (hasOffsets) {
              flags = flags | DocsAndPositionsEnum.FLAG_OFFSETS;
            }
          }

          DocsEnum docsEnum = null;
          DocsAndPositionsEnum docsAndPositionsEnum = null;
          TermsEnum termsEnum = terms.iterator(null);

          while (true) { // for all terms in this field
            BytesRef term = termsEnum.next();
            if (term == null) {
              break;
            }
            if (hasPositions) {
              docsAndPositionsEnum = termsEnum.docsAndPositions(null, docsAndPositionsEnum, flags);
              docsEnum = docsAndPositionsEnum;
            } else {
              docsEnum = termsEnum.docs(null, docsEnum, flags);
              docsAndPositionsEnum = null;
            }
            assert docsEnum != null;

            PostingsConsumer postingsConsumer = termsConsumer.startTerm(term);

            // How many documents have this term:
            int docFreq = 0;

            // How many times this term occurs:
            long totalTermFreq = 0;

            while(true) { // for all docs in this field+term
              int doc = docsEnum.nextDoc();
              if (doc == DocsEnum.NO_MORE_DOCS) {
                break;
              }
              docFreq++;
              visitedDocs.set(doc);
              if (hasFreq) {
                int freq = docsEnum.freq();
                postingsConsumer.startDoc(doc, freq);
                totalTermFreq += freq;

                if (hasPositions) {
                  for(int i=0;i<freq;i++) { // for all positions in this field+term + doc
                    int pos = docsAndPositionsEnum.nextPosition();
                    BytesRef payload = docsAndPositionsEnum.getPayload();
                    if (hasOffsets) {
                      postingsConsumer.addPosition(pos, payload, docsAndPositionsEnum.startOffset(), docsAndPositionsEnum.endOffset());
                    } else {
                      postingsConsumer.addPosition(pos, payload, -1, -1);
                    }
                  }
                }
              } else {
                postingsConsumer.startDoc(doc, -1);
              }
              postingsConsumer.finishDoc();
            }

            if (docFreq > 0) {
              termsConsumer.finishTerm(term, new TermStats(docFreq, hasFreq ? totalTermFreq : -1));
              sumTotalTermFreq += totalTermFreq;
              sumDocFreq += docFreq;
            }
          }

          termsConsumer.finish(hasFreq ? sumTotalTermFreq : -1, sumDocFreq, visitedDocs.cardinality());
        }
      }
      success = true;
    } finally {
      if (success) {
        IOUtils.close(this);
      } else {
        IOUtils.closeWhileHandlingException(this);
      }
    }
  }
}
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/TermVectorsWriter.java b/lucene/core/src/java/org/apache/lucene/codecs/TermVectorsWriter.java
index c15be770d8c..c3a30193991 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/TermVectorsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/TermVectorsWriter.java
@@ -19,7 +19,6 @@ package org.apache.lucene.codecs;
 
 import java.io.Closeable;
 import java.io.IOException;
import java.util.Comparator;
 import java.util.Iterator;
 
 import org.apache.lucene.index.AtomicReader;
@@ -293,10 +292,6 @@ public abstract class TermVectorsWriter implements Closeable {
     assert fieldCount == numFields;
     finishDocument();
   }
  
  /** Return the BytesRef Comparator used to sort terms
   *  before feeding to this API. */
  public abstract Comparator<BytesRef> getComparator() throws IOException;
 
   @Override
   public abstract void close() throws IOException;
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/TermsConsumer.java b/lucene/core/src/java/org/apache/lucene/codecs/TermsConsumer.java
index 47445182257..84fde04977b 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/TermsConsumer.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/TermsConsumer.java
@@ -18,19 +18,10 @@ package org.apache.lucene.codecs;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.index.FieldInfo; // javadocs
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.MultiDocsEnum;
import org.apache.lucene.index.MultiDocsAndPositionsEnum;
 
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
 
 /**
  * Abstract API that consumes terms for an individual field.
@@ -38,7 +29,7 @@ import org.apache.lucene.util.FixedBitSet;
  * The lifecycle is:
  * <ol>
  *   <li>TermsConsumer is returned for each field 
 *       by {@link FieldsConsumer#addField(FieldInfo)}.
 *       by {@link PushFieldsConsumer#addField(FieldInfo)}.
  *   <li>TermsConsumer returns a {@link PostingsConsumer} for
  *       each term in {@link #startTerm(BytesRef)}.
  *   <li>When the producer (e.g. IndexWriter)
@@ -73,134 +64,4 @@ public abstract class TermsConsumer {
    *  <code>sumTotalTermFreq</code> will be -1 when term 
    *  frequencies are omitted for the field. */
   public abstract void finish(long sumTotalTermFreq, long sumDocFreq, int docCount) throws IOException;

  /** Return the BytesRef Comparator used to sort terms
   *  before feeding to this API. */
  public abstract Comparator<BytesRef> getComparator() throws IOException;

  private MappingMultiDocsEnum docsEnum;
  private MappingMultiDocsEnum docsAndFreqsEnum;
  private MappingMultiDocsAndPositionsEnum postingsEnum;

  /** Default merge impl */
  public void merge(MergeState mergeState, IndexOptions indexOptions, TermsEnum termsEnum) throws IOException {

    BytesRef term;
    assert termsEnum != null;
    long sumTotalTermFreq = 0;
    long sumDocFreq = 0;
    long sumDFsinceLastAbortCheck = 0;
    FixedBitSet visitedDocs = new FixedBitSet(mergeState.segmentInfo.getDocCount());

    if (indexOptions == IndexOptions.DOCS_ONLY) {
      if (docsEnum == null) {
        docsEnum = new MappingMultiDocsEnum();
      }
      docsEnum.setMergeState(mergeState);

      MultiDocsEnum docsEnumIn = null;

      while((term = termsEnum.next()) != null) {
        // We can pass null for liveDocs, because the
        // mapping enum will skip the non-live docs:
        docsEnumIn = (MultiDocsEnum) termsEnum.docs(null, docsEnumIn, DocsEnum.FLAG_NONE);
        if (docsEnumIn != null) {
          docsEnum.reset(docsEnumIn);
          final PostingsConsumer postingsConsumer = startTerm(term);
          final TermStats stats = postingsConsumer.merge(mergeState, indexOptions, docsEnum, visitedDocs);
          if (stats.docFreq > 0) {
            finishTerm(term, stats);
            sumTotalTermFreq += stats.docFreq;
            sumDFsinceLastAbortCheck += stats.docFreq;
            sumDocFreq += stats.docFreq;
            if (sumDFsinceLastAbortCheck > 60000) {
              mergeState.checkAbort.work(sumDFsinceLastAbortCheck/5.0);
              sumDFsinceLastAbortCheck = 0;
            }
          }
        }
      }
    } else if (indexOptions == IndexOptions.DOCS_AND_FREQS) {
      if (docsAndFreqsEnum == null) {
        docsAndFreqsEnum = new MappingMultiDocsEnum();
      }
      docsAndFreqsEnum.setMergeState(mergeState);

      MultiDocsEnum docsAndFreqsEnumIn = null;

      while((term = termsEnum.next()) != null) {
        // We can pass null for liveDocs, because the
        // mapping enum will skip the non-live docs:
        docsAndFreqsEnumIn = (MultiDocsEnum) termsEnum.docs(null, docsAndFreqsEnumIn);
        assert docsAndFreqsEnumIn != null;
        docsAndFreqsEnum.reset(docsAndFreqsEnumIn);
        final PostingsConsumer postingsConsumer = startTerm(term);
        final TermStats stats = postingsConsumer.merge(mergeState, indexOptions, docsAndFreqsEnum, visitedDocs);
        if (stats.docFreq > 0) {
          finishTerm(term, stats);
          sumTotalTermFreq += stats.totalTermFreq;
          sumDFsinceLastAbortCheck += stats.docFreq;
          sumDocFreq += stats.docFreq;
          if (sumDFsinceLastAbortCheck > 60000) {
            mergeState.checkAbort.work(sumDFsinceLastAbortCheck/5.0);
            sumDFsinceLastAbortCheck = 0;
          }
        }
      }
    } else if (indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
      if (postingsEnum == null) {
        postingsEnum = new MappingMultiDocsAndPositionsEnum();
      }
      postingsEnum.setMergeState(mergeState);
      MultiDocsAndPositionsEnum postingsEnumIn = null;
      while((term = termsEnum.next()) != null) {
        // We can pass null for liveDocs, because the
        // mapping enum will skip the non-live docs:
        postingsEnumIn = (MultiDocsAndPositionsEnum) termsEnum.docsAndPositions(null, postingsEnumIn, DocsAndPositionsEnum.FLAG_PAYLOADS);
        assert postingsEnumIn != null;
        postingsEnum.reset(postingsEnumIn);

        final PostingsConsumer postingsConsumer = startTerm(term);
        final TermStats stats = postingsConsumer.merge(mergeState, indexOptions, postingsEnum, visitedDocs);
        if (stats.docFreq > 0) {
          finishTerm(term, stats);
          sumTotalTermFreq += stats.totalTermFreq;
          sumDFsinceLastAbortCheck += stats.docFreq;
          sumDocFreq += stats.docFreq;
          if (sumDFsinceLastAbortCheck > 60000) {
            mergeState.checkAbort.work(sumDFsinceLastAbortCheck/5.0);
            sumDFsinceLastAbortCheck = 0;
          }
        }
      }
    } else {
      assert indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;
      if (postingsEnum == null) {
        postingsEnum = new MappingMultiDocsAndPositionsEnum();
      }
      postingsEnum.setMergeState(mergeState);
      MultiDocsAndPositionsEnum postingsEnumIn = null;
      while((term = termsEnum.next()) != null) {
        // We can pass null for liveDocs, because the
        // mapping enum will skip the non-live docs:
        postingsEnumIn = (MultiDocsAndPositionsEnum) termsEnum.docsAndPositions(null, postingsEnumIn);
        assert postingsEnumIn != null;
        postingsEnum.reset(postingsEnumIn);

        final PostingsConsumer postingsConsumer = startTerm(term);
        final TermStats stats = postingsConsumer.merge(mergeState, indexOptions, postingsEnum, visitedDocs);
        if (stats.docFreq > 0) {
          finishTerm(term, stats);
          sumTotalTermFreq += stats.totalTermFreq;
          sumDFsinceLastAbortCheck += stats.docFreq;
          sumDocFreq += stats.docFreq;
          if (sumDFsinceLastAbortCheck > 60000) {
            mergeState.checkAbort.work(sumDFsinceLastAbortCheck/5.0);
            sumDFsinceLastAbortCheck = 0;
          }
        }
      }
    }
    finish(indexOptions == IndexOptions.DOCS_ONLY ? -1 : sumTotalTermFreq, sumDocFreq, visitedDocs.cardinality());
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingTermVectorsReader.java b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingTermVectorsReader.java
index e3bcd83c494..619541b9423 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingTermVectorsReader.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingTermVectorsReader.java
@@ -31,7 +31,6 @@ import static org.apache.lucene.codecs.compressing.CompressingTermVectorsWriter.
 
 import java.io.Closeable;
 import java.io.IOException;
import java.util.Comparator;
 import java.util.Iterator;
 import java.util.NoSuchElementException;
 
@@ -57,7 +56,6 @@ import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.LongsRef;
import org.apache.lucene.util.RamUsageEstimator;
 import org.apache.lucene.util.packed.BlockPackedReaderIterator;
 import org.apache.lucene.util.packed.PackedInts;
 
@@ -722,11 +720,6 @@ public final class CompressingTermVectorsReader extends TermVectorsReader implem
       return termsEnum;
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public long size() throws IOException {
       return numTerms;
@@ -819,11 +812,6 @@ public final class CompressingTermVectorsReader extends TermVectorsReader implem
       return term;
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public SeekStatus seekCeil(BytesRef text)
         throws IOException {
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingTermVectorsWriter.java b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingTermVectorsWriter.java
index 1fcd7a6b9dd..b3b466276d3 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingTermVectorsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingTermVectorsWriter.java
@@ -20,7 +20,6 @@ package org.apache.lucene.codecs.compressing;
 import java.io.IOException;
 import java.util.ArrayDeque;
 import java.util.Arrays;
import java.util.Comparator;
 import java.util.Deque;
 import java.util.Iterator;
 import java.util.SortedSet;
@@ -662,11 +661,6 @@ public final class CompressingTermVectorsWriter extends TermVectorsWriter {
     indexWriter.finish(numDocs);
   }
 
  @Override
  public Comparator<BytesRef> getComparator() {
    return BytesRef.getUTF8SortedAsUnicodeComparator();
  }

   @Override
   public void addProx(int numProx, DataInput positions, DataInput offsets)
       throws IOException {
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/lucene40/Lucene40TermVectorsReader.java b/lucene/core/src/java/org/apache/lucene/codecs/lucene40/Lucene40TermVectorsReader.java
index bb0b2c5dd01..89ba92eaabc 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/lucene40/Lucene40TermVectorsReader.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/lucene40/Lucene40TermVectorsReader.java
@@ -20,7 +20,6 @@ package org.apache.lucene.codecs.lucene40;
 import java.io.Closeable;
 import java.io.IOException;
 import java.util.Arrays;
import java.util.Comparator;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
@@ -360,13 +359,6 @@ public class Lucene40TermVectorsReader extends TermVectorsReader implements Clos
       return 1;
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      // TODO: really indexer hardwires
      // this...?  I guess codec could buffer and re-sort...
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public boolean hasOffsets() {
       return storeOffsets;
@@ -569,11 +561,6 @@ public class Lucene40TermVectorsReader extends TermVectorsReader implements Clos
       docsAndPositionsEnum.reset(liveDocs, positions, startOffsets, endOffsets, payloadOffsets, payloadData);
       return docsAndPositionsEnum;
     }

    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }
   }
 
   // NOTE: sort of a silly class, since you can get the
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/lucene40/Lucene40TermVectorsWriter.java b/lucene/core/src/java/org/apache/lucene/codecs/lucene40/Lucene40TermVectorsWriter.java
index 938d4c07993..cc9a24573fd 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/lucene40/Lucene40TermVectorsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/lucene40/Lucene40TermVectorsWriter.java
@@ -18,7 +18,6 @@ package org.apache.lucene.codecs.lucene40;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.codecs.CodecUtil;
 import org.apache.lucene.codecs.TermVectorsReader;
@@ -443,9 +442,4 @@ public final class Lucene40TermVectorsWriter extends TermVectorsWriter {
     IOUtils.close(tvx, tvd, tvf);
     tvx = tvd = tvf = null;
   }

  @Override
  public Comparator<BytesRef> getComparator() {
    return BytesRef.getUTF8SortedAsUnicodeComparator();
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/lucene42/Lucene42DocValuesProducer.java b/lucene/core/src/java/org/apache/lucene/codecs/lucene42/Lucene42DocValuesProducer.java
index af87be89e39..c7117a19062 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/lucene42/Lucene42DocValuesProducer.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/lucene42/Lucene42DocValuesProducer.java
@@ -18,7 +18,6 @@ package org.apache.lucene.codecs.lucene42;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 import java.util.HashMap;
 import java.util.Map;
 
@@ -510,11 +509,6 @@ class Lucene42DocValuesProducer extends DocValuesProducer {
       }
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public SeekStatus seekCeil(BytesRef text) throws IOException {
       if (in.seekCeil(text) == null) {
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/lucene45/Lucene45DocValuesProducer.java b/lucene/core/src/java/org/apache/lucene/codecs/lucene45/Lucene45DocValuesProducer.java
index 620997ac5b1..3e09281b477 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/lucene45/Lucene45DocValuesProducer.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/lucene45/Lucene45DocValuesProducer.java
@@ -26,7 +26,6 @@ import static org.apache.lucene.codecs.lucene45.Lucene45DocValuesConsumer.BINARY
 
 import java.io.Closeable; // javadocs
 import java.io.IOException;
import java.util.Comparator;
 import java.util.HashMap;
 import java.util.Map;
 
@@ -811,11 +810,6 @@ public class Lucene45DocValuesProducer extends DocValuesProducer implements Clos
         public long ord() throws IOException {
           return currentOrd;
         }
        
        @Override
        public Comparator<BytesRef> getComparator() {
          return BytesRef.getUTF8SortedAsUnicodeComparator();
        }
 
         @Override
         public int docFreq() throws IOException {
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/perfield/PerFieldPostingsFormat.java b/lucene/core/src/java/org/apache/lucene/codecs/perfield/PerFieldPostingsFormat.java
index b9695236153..a472af77633 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/perfield/PerFieldPostingsFormat.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/perfield/PerFieldPostingsFormat.java
@@ -17,26 +17,29 @@ package org.apache.lucene.codecs.perfield;
  * limitations under the License.
  */
 
import java.io.Closeable;
 import java.io.IOException;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.ServiceLoader; // javadocs
import java.util.Set;
 import java.util.TreeMap;
import java.util.TreeSet;
 
 import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.FieldsProducer;
 import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.TermsConsumer;
 import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.Fields;
 import org.apache.lucene.index.SegmentReadState;
 import org.apache.lucene.index.SegmentWriteState;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.RamUsageEstimator;
 
import static org.apache.lucene.index.FilterAtomicReader.FilterFields;

 /**
  * Enables per field postings support.
  * <p>
@@ -65,96 +68,22 @@ public abstract class PerFieldPostingsFormat extends PostingsFormat {
    *  segment suffix name for each field. */
   public static final String PER_FIELD_SUFFIX_KEY = PerFieldPostingsFormat.class.getSimpleName() + ".suffix";
 
  
   /** Sole constructor. */
   public PerFieldPostingsFormat() {
     super(PER_FIELD_NAME);
   }
 
  @Override
  public final FieldsConsumer fieldsConsumer(SegmentWriteState state)
      throws IOException {
    return new FieldsWriter(state);
  }
  
  static class FieldsConsumerAndSuffix implements Closeable {
    FieldsConsumer consumer;
  /** Group of fields written by one PostingsFormat */
  static class FieldsGroup {
    final Set<String> fields = new TreeSet<String>();
     int suffix;
    
    @Override
    public void close() throws IOException {
      consumer.close();
    }
  }
    
  private class FieldsWriter extends FieldsConsumer {

    private final Map<PostingsFormat,FieldsConsumerAndSuffix> formats = new HashMap<PostingsFormat,FieldsConsumerAndSuffix>();
    private final Map<String,Integer> suffixes = new HashMap<String,Integer>();
    
    private final SegmentWriteState segmentWriteState;
 
    public FieldsWriter(SegmentWriteState state) {
      segmentWriteState = state;
    }
    /** Custom SegmentWriteState for this group of fields,
     *  with the segmentSuffix uniqueified for this
     *  PostingsFormat */
    SegmentWriteState state;
  };
 
    @Override
    public TermsConsumer addField(FieldInfo field) throws IOException {
      final PostingsFormat format = getPostingsFormatForField(field.name);
      if (format == null) {
        throw new IllegalStateException("invalid null PostingsFormat for field=\"" + field.name + "\"");
      }
      final String formatName = format.getName();
      
      String previousValue = field.putAttribute(PER_FIELD_FORMAT_KEY, formatName);
      assert previousValue == null;
      
      Integer suffix;
      
      FieldsConsumerAndSuffix consumer = formats.get(format);
      if (consumer == null) {
        // First time we are seeing this format; create a new instance
        
        // bump the suffix
        suffix = suffixes.get(formatName);
        if (suffix == null) {
          suffix = 0;
        } else {
          suffix = suffix + 1;
        }
        suffixes.put(formatName, suffix);
        
        final String segmentSuffix = getFullSegmentSuffix(field.name,
                                                          segmentWriteState.segmentSuffix,
                                                          getSuffix(formatName, Integer.toString(suffix)));
        consumer = new FieldsConsumerAndSuffix();
        consumer.consumer = format.fieldsConsumer(new SegmentWriteState(segmentWriteState, segmentSuffix));
        consumer.suffix = suffix;
        formats.put(format, consumer);
      } else {
        // we've already seen this format, so just grab its suffix
        assert suffixes.containsKey(formatName);
        suffix = consumer.suffix;
      }
      
      previousValue = field.putAttribute(PER_FIELD_SUFFIX_KEY, Integer.toString(suffix));
      assert previousValue == null;

      // TODO: we should only provide the "slice" of FIS
      // that this PF actually sees ... then stuff like
      // .hasProx could work correctly?
      // NOTE: .hasProx is already broken in the same way for the non-perfield case,
      // if there is a fieldinfo with prox that has no postings, you get a 0 byte file.
      return consumer.consumer.addField(field);
    }

    @Override
    public void close() throws IOException {
      // Close all subs
      IOUtils.close(formats.values());
    }
  }
  
   static String getSuffix(String formatName, String suffix) {
     return formatName + "_" + suffix;
   }
@@ -169,6 +98,87 @@ public abstract class PerFieldPostingsFormat extends PostingsFormat {
       throw new IllegalStateException("cannot embed PerFieldPostingsFormat inside itself (field \"" + fieldName + "\" returned PerFieldPostingsFormat)");
     }
   }
  
  private class FieldsWriter extends FieldsConsumer {
    final SegmentWriteState writeState;

    public FieldsWriter(SegmentWriteState writeState) {
      this.writeState = writeState;
    }

    @Override
    public void write(Fields fields) throws IOException {

      // Maps a PostingsFormat instance to the suffix it
      // should use
      Map<PostingsFormat,FieldsGroup> formatToGroups = new HashMap<PostingsFormat,FieldsGroup>();

      // Holds last suffix of each PostingFormat name
      Map<String,Integer> suffixes = new HashMap<String,Integer>();

      // First pass: assign field -> PostingsFormat
      for(String field : fields) {
        FieldInfo fieldInfo = writeState.fieldInfos.fieldInfo(field);

        final PostingsFormat format = getPostingsFormatForField(field);
  
        if (format == null) {
          throw new IllegalStateException("invalid null PostingsFormat for field=\"" + field + "\"");
        }
        String formatName = format.getName();
      
        FieldsGroup group = formatToGroups.get(format);
        if (group == null) {
          // First time we are seeing this format; create a
          // new instance

          // bump the suffix
          Integer suffix = suffixes.get(formatName);
          if (suffix == null) {
            suffix = 0;
          } else {
            suffix = suffix + 1;
          }
          suffixes.put(formatName, suffix);

          String segmentSuffix = getFullSegmentSuffix(field,
                                                      writeState.segmentSuffix,
                                                      getSuffix(formatName, Integer.toString(suffix)));
          group = new FieldsGroup();
          group.state = new SegmentWriteState(writeState, segmentSuffix);
          group.suffix = suffix;
          formatToGroups.put(format, group);
        } else {
          // we've already seen this format, so just grab its suffix
          assert suffixes.containsKey(formatName);
        }

        group.fields.add(field);

        String previousValue = fieldInfo.putAttribute(PER_FIELD_FORMAT_KEY, formatName);
        assert previousValue == null;

        previousValue = fieldInfo.putAttribute(PER_FIELD_SUFFIX_KEY, Integer.toString(group.suffix));
        assert previousValue == null;
      }

      // Second pass: write postings
      for(Map.Entry<PostingsFormat,FieldsGroup> ent : formatToGroups.entrySet()) {
        PostingsFormat format = ent.getKey();
        final FieldsGroup group = ent.getValue();

        // Exposes only the fields from this group:
        Fields maskedFields = new FilterFields(fields) {
            @Override
            public Iterator<String> iterator() {
              return group.fields.iterator();
            }
          };

        format.fieldsConsumer(group.state).write(maskedFields);
      }
    }
  }
 
   private class FieldsReader extends FieldsProducer {
 
@@ -238,6 +248,12 @@ public abstract class PerFieldPostingsFormat extends PostingsFormat {
     }
   }
 
  @Override
  public final FieldsConsumer fieldsConsumer(SegmentWriteState state)
      throws IOException {
    return new FieldsWriter(state);
  }

   @Override
   public final FieldsProducer fieldsProducer(SegmentReadState state)
       throws IOException {
diff --git a/lucene/core/src/java/org/apache/lucene/index/AutomatonTermsEnum.java b/lucene/core/src/java/org/apache/lucene/index/AutomatonTermsEnum.java
index 980fce8139a..e7984e95919 100644
-- a/lucene/core/src/java/org/apache/lucene/index/AutomatonTermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/index/AutomatonTermsEnum.java
@@ -18,7 +18,6 @@ package org.apache.lucene.index;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.IntsRef;
@@ -65,7 +64,6 @@ class AutomatonTermsEnum extends FilteredTermsEnum {
   // of terms where we should simply do sequential reads instead.
   private boolean linear = false;
   private final BytesRef linearUpperBound = new BytesRef(10);
  private final Comparator<BytesRef> termComp;
 
   /**
    * Construct an enumerator based upon an automaton, enumerating the specified
@@ -85,8 +83,6 @@ class AutomatonTermsEnum extends FilteredTermsEnum {
 
     // used for path tracking, where each bit is a numbered state.
     visited = new long[runAutomaton.getSize()];

    termComp = getComparator();
   }
   
   /**
@@ -99,10 +95,10 @@ class AutomatonTermsEnum extends FilteredTermsEnum {
       if (runAutomaton.run(term.bytes, term.offset, term.length))
         return linear ? AcceptStatus.YES : AcceptStatus.YES_AND_SEEK;
       else
        return (linear && termComp.compare(term, linearUpperBound) < 0) ? 
        return (linear && term.compareTo(linearUpperBound) < 0) ? 
             AcceptStatus.NO : AcceptStatus.NO_AND_SEEK;
     } else {
      return (linear && termComp.compare(term, linearUpperBound) < 0) ? 
      return (linear && term.compareTo(linearUpperBound) < 0) ? 
           AcceptStatus.NO : AcceptStatus.NO_AND_SEEK;
     }
   }
diff --git a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
index b22a076153d..6563e03ab1a 100644
-- a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
++ b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
@@ -761,8 +761,6 @@ public class CheckIndex {
       
       BytesRef lastTerm = null;
       
      Comparator<BytesRef> termComp = terms.getComparator();
      
       long sumTotalTermFreq = 0;
       long sumDocFreq = 0;
       FixedBitSet visitedDocs = new FixedBitSet(maxDoc);
@@ -780,7 +778,7 @@ public class CheckIndex {
         if (lastTerm == null) {
           lastTerm = BytesRef.deepCopyOf(term);
         } else {
          if (termComp.compare(lastTerm, term) >= 0) {
          if (lastTerm.compareTo(term) >= 0) {
             throw new RuntimeException("terms out of order: lastTerm=" + lastTerm + " term=" + term);
           }
           lastTerm.copyBytes(term);
diff --git a/lucene/core/src/java/org/apache/lucene/index/DocTermOrds.java b/lucene/core/src/java/org/apache/lucene/index/DocTermOrds.java
index 56859ce3f2a..42f1b2164f7 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DocTermOrds.java
++ b/lucene/core/src/java/org/apache/lucene/index/DocTermOrds.java
@@ -20,7 +20,6 @@ package org.apache.lucene.index;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
import java.util.Comparator;
 import java.util.List;
 
 import org.apache.lucene.codecs.PostingsFormat; // javadocs
@@ -611,11 +610,6 @@ public class DocTermOrds {
       termsEnum = reader.fields().terms(field).iterator(null);
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return termsEnum.getComparator();
    }

     @Override    
     public DocsEnum docs(Bits liveDocs, DocsEnum reuse, int flags) throws IOException {
       return termsEnum.docs(liveDocs, reuse, flags);
diff --git a/lucene/core/src/java/org/apache/lucene/index/FilterAtomicReader.java b/lucene/core/src/java/org/apache/lucene/index/FilterAtomicReader.java
index 4a8a55a3433..01d9d50e103 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FilterAtomicReader.java
++ b/lucene/core/src/java/org/apache/lucene/index/FilterAtomicReader.java
@@ -18,7 +18,6 @@ package org.apache.lucene.index;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 import java.util.Iterator;
 
 import org.apache.lucene.search.CachingWrapperFilter;
@@ -98,11 +97,6 @@ public class FilterAtomicReader extends AtomicReader {
     public TermsEnum iterator(TermsEnum reuse) throws IOException {
       return in.iterator(reuse);
     }
    
    @Override
    public Comparator<BytesRef> getComparator() {
      return in.getComparator();
    }
 
     @Override
     public long size() throws IOException {
@@ -200,11 +194,6 @@ public class FilterAtomicReader extends AtomicReader {
     public DocsAndPositionsEnum docsAndPositions(Bits liveDocs, DocsAndPositionsEnum reuse, int flags) throws IOException {
       return in.docsAndPositions(liveDocs, reuse, flags);
     }

    @Override
    public Comparator<BytesRef> getComparator() {
      return in.getComparator();
    }
   }
 
   /** Base class for filtering {@link DocsEnum} implementations. */
diff --git a/lucene/core/src/java/org/apache/lucene/index/FilteredTermsEnum.java b/lucene/core/src/java/org/apache/lucene/index/FilteredTermsEnum.java
index 4a704d39639..3213a22db46 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FilteredTermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/index/FilteredTermsEnum.java
@@ -18,7 +18,6 @@ package org.apache.lucene.index;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.AttributeSource;
@@ -28,7 +27,7 @@ import org.apache.lucene.util.Bits;
  * Abstract class for enumerating a subset of all terms. 
  * 
  * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * {@link BytesRef#compareTo}.  Each term in the enumeration is
  * greater than all that precede it.</p>
  * <p><em>Please note:</em> Consumers of this enum cannot
  * call {@code seek()}, it is forward only; it throws
@@ -134,11 +133,6 @@ public abstract class FilteredTermsEnum extends TermsEnum {
     return tenum.term();
   }
 
  @Override
  public Comparator<BytesRef> getComparator() {
    return tenum.getComparator();
  }
    
   @Override
   public int docFreq() throws IOException {
     return tenum.docFreq();
@@ -221,7 +215,7 @@ public abstract class FilteredTermsEnum extends TermsEnum {
         final BytesRef t = nextSeekTerm(actualTerm);
         //System.out.println("  seek to t=" + (t == null ? "null" : t.utf8ToString()) + " tenum=" + tenum);
         // Make sure we always seek forward:
        assert actualTerm == null || t == null || getComparator().compare(t, actualTerm) > 0: "curTerm=" + actualTerm + " seekTerm=" + t;
        assert actualTerm == null || t == null || t.compareTo(actualTerm) > 0: "curTerm=" + actualTerm + " seekTerm=" + t;
         if (t == null || tenum.seekCeil(t) == SeekStatus.END) {
           // no more terms to seek to or enum exhausted
           //System.out.println("  return null");
diff --git a/lucene/core/src/java/org/apache/lucene/index/FreqProxFields.java b/lucene/core/src/java/org/apache/lucene/index/FreqProxFields.java
new file mode 100644
index 00000000000..ed4eddb5f71
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/index/FreqProxFields.java
@@ -0,0 +1,523 @@
package org.apache.lucene.index;

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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.FreqProxTermsWriterPerField.FreqProxPostingsArray;
import org.apache.lucene.util.AttributeSource; // javadocs
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

/** Implements limited (iterators only, no stats) {@link
 *  Fields} interface over the in-RAM buffered
 *  fields/terms/postings, to flush postings through the
 *  PostingsFormat. */

class FreqProxFields extends Fields {
  final Map<String,FreqProxTermsWriterPerField> fields = new LinkedHashMap<String,FreqProxTermsWriterPerField>();

  public FreqProxFields(List<FreqProxTermsWriterPerField> fieldList) {
    // NOTE: fields are already sorted by field name
    for(FreqProxTermsWriterPerField field : fieldList) {
      fields.put(field.fieldInfo.name, field);
    }
  }

  public Iterator<String> iterator() {
    return fields.keySet().iterator();
  }

  @Override
  public Terms terms(String field) throws IOException {
    FreqProxTermsWriterPerField perField = fields.get(field);
    return perField == null ? null : new FreqProxTerms(perField);
  }

  @Override
  public int size() {
    //return fields.size();
    throw new UnsupportedOperationException();
  }

  private static class FreqProxTerms extends Terms {
    final FreqProxTermsWriterPerField terms;

    public FreqProxTerms(FreqProxTermsWriterPerField terms) {
      this.terms = terms;
    }

    @Override
    public TermsEnum iterator(TermsEnum reuse) {
      FreqProxTermsEnum termsEnum;
      if (reuse instanceof FreqProxTermsEnum && ((FreqProxTermsEnum) reuse).terms == this.terms) {
        termsEnum = (FreqProxTermsEnum) reuse;
      } else {
        termsEnum = new FreqProxTermsEnum(terms);
      }
      termsEnum.reset();
      return termsEnum;
    }

    @Override
    public long size() {
      //return terms.termsHashPerField.bytesHash.size();
      throw new UnsupportedOperationException();
    }

    @Override
    public long getSumTotalTermFreq() {
      //return terms.sumTotalTermFreq;
      throw new UnsupportedOperationException();
    }

    @Override
    public long getSumDocFreq() {
      //return terms.sumDocFreq;
      throw new UnsupportedOperationException();
    }

    @Override
    public int getDocCount() {
      //return terms.docCount;
      throw new UnsupportedOperationException();
    }
  
    @Override
    public boolean hasOffsets() {
      // NOTE: the in-memory buffer may have indexed offsets
      // because that's what FieldInfo said when we started,
      // but during indexing this may have been downgraded:
      return terms.fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;      
    }
  
    @Override
    public boolean hasPositions() {
      // NOTE: the in-memory buffer may have indexed positions
      // because that's what FieldInfo said when we started,
      // but during indexing this may have been downgraded:
      return terms.fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
    }
  
    @Override
    public boolean hasPayloads() {
      return terms.hasPayloads;
    }
  }

  private static class FreqProxTermsEnum extends TermsEnum {
    final FreqProxTermsWriterPerField terms;
    final int[] sortedTermIDs;
    final FreqProxPostingsArray postingsArray;
    final BytesRef scratch = new BytesRef();
    final int numTerms;
    int ord;

    public FreqProxTermsEnum(FreqProxTermsWriterPerField terms) {
      this.terms = terms;
      this.numTerms = terms.termsHashPerField.bytesHash.size();
      sortedTermIDs = terms.sortedTermIDs;
      assert sortedTermIDs != null;
      postingsArray = (FreqProxPostingsArray) terms.termsHashPerField.postingsArray;
    }

    public void reset() {
      ord = -1;
    }

    public SeekStatus seekCeil(BytesRef text) {

      // TODO: we could instead keep the BytesRefHash
      // intact so this is a hash lookup

      // binary search:
      int lo = 0;
      int hi = numTerms - 1;
      while (hi >= lo) {
        int mid = (lo + hi) >>> 1;
        int textStart = postingsArray.textStarts[sortedTermIDs[mid]];
        terms.termsHashPerField.bytePool.setBytesRef(scratch, textStart);
        int cmp = scratch.compareTo(text);
        if (cmp < 0) {
          lo = mid + 1;
        } else if (cmp > 0) {
          hi = mid - 1;
        } else {
          // found:
          ord = mid;
          return SeekStatus.FOUND;
        }
      }

      // not found:
      ord = lo + 1;
      if (ord == numTerms) {
        return SeekStatus.END;
      } else {
        return SeekStatus.NOT_FOUND;
      }
    }

    public void seekExact(long ord) {
      this.ord = (int) ord;
      int textStart = postingsArray.textStarts[sortedTermIDs[this.ord]];
      terms.termsHashPerField.bytePool.setBytesRef(scratch, textStart);
    }

    @Override
    public BytesRef next() {
      ord++;
      if (ord >= numTerms) {
        return null;
      } else {
        int textStart = postingsArray.textStarts[sortedTermIDs[ord]];
        terms.termsHashPerField.bytePool.setBytesRef(scratch, textStart);
        return scratch;
      }
    }

    @Override
    public BytesRef term() {
      return scratch;
    }

    @Override
    public long ord() {
      return ord;
    }

    @Override
    public int docFreq() {
      // We do not store this per-term, and we cannot
      // implement this at merge time w/o an added pass
      // through the postings:
      throw new UnsupportedOperationException();
    }

    @Override
    public long totalTermFreq() {
      // We do not store this per-term, and we cannot
      // implement this at merge time w/o an added pass
      // through the postings:
      throw new UnsupportedOperationException();
    }

    @Override
    public DocsEnum docs(Bits liveDocs, DocsEnum reuse, int flags) {
      if (liveDocs != null) {
        throw new IllegalArgumentException("liveDocs must be null");
      }

      FreqProxDocsEnum docsEnum;

      if (!terms.hasFreq && (flags & DocsEnum.FLAG_FREQS) != 0) {
        // Caller wants freqs but we didn't index them;
        // don't lie:
        throw new IllegalArgumentException("did not index freq");
      }

      if (reuse instanceof FreqProxDocsEnum) {
        docsEnum = (FreqProxDocsEnum) reuse;
        if (docsEnum.postingsArray != postingsArray) {
          docsEnum = new FreqProxDocsEnum(terms, postingsArray);
        }
      } else {
        docsEnum = new FreqProxDocsEnum(terms, postingsArray);
      }
      docsEnum.reset(sortedTermIDs[ord]);
      return docsEnum;
    }

    @Override
    public DocsAndPositionsEnum docsAndPositions(Bits liveDocs, DocsAndPositionsEnum reuse, int flags) {
      if (liveDocs != null) {
        throw new IllegalArgumentException("liveDocs must be null");
      }
      FreqProxDocsAndPositionsEnum posEnum;

      if (!terms.hasProx) {
        // Caller wants positions but we didn't index them;
        // don't lie:
        throw new IllegalArgumentException("did not index positions");
      }

      if (!terms.hasOffsets && (flags & DocsAndPositionsEnum.FLAG_OFFSETS) != 0) {
        // Caller wants offsets but we didn't index them;
        // don't lie:
        throw new IllegalArgumentException("did not index offsets");
      }

      if (reuse instanceof FreqProxDocsAndPositionsEnum) {
        posEnum = (FreqProxDocsAndPositionsEnum) reuse;
        if (posEnum.postingsArray != postingsArray) {
          posEnum = new FreqProxDocsAndPositionsEnum(terms, postingsArray);
        }
      } else {
        posEnum = new FreqProxDocsAndPositionsEnum(terms, postingsArray);
      }
      posEnum.reset(sortedTermIDs[ord]);
      return posEnum;
    }

    /**
     * Expert: Returns the TermsEnums internal state to position the TermsEnum
     * without re-seeking the term dictionary.
     * <p>
     * NOTE: A seek by {@link TermState} might not capture the
     * {@link AttributeSource}'s state. Callers must maintain the
     * {@link AttributeSource} states separately
     * 
     * @see TermState
     * @see #seekExact(BytesRef, TermState)
     */
    public TermState termState() throws IOException {
      return new TermState() {
        @Override
        public void copyFrom(TermState other) {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  private static class FreqProxDocsEnum extends DocsEnum {

    final FreqProxTermsWriterPerField terms;
    final FreqProxPostingsArray postingsArray;
    final ByteSliceReader reader = new ByteSliceReader();
    final boolean readTermFreq;
    int docID;
    int freq;
    boolean ended;
    int termID;

    public FreqProxDocsEnum(FreqProxTermsWriterPerField terms, FreqProxPostingsArray postingsArray) {
      this.terms = terms;
      this.postingsArray = postingsArray;
      this.readTermFreq = terms.hasFreq;
    }

    public void reset(int termID) {
      this.termID = termID;
      terms.termsHashPerField.initReader(reader, termID, 0);
      ended = false;
      docID = 0;
    }

    @Override
    public int docID() {
      return docID;
    }

    @Override
    public int freq() {
      // Don't lie here ... don't want codecs writings lots
      // of wasted 1s into the index:
      if (!readTermFreq) {
        throw new IllegalStateException("freq was not indexed");
      } else {
        return freq;
      }
    }

    @Override
    public int nextDoc() throws IOException {
      if (reader.eof()) {
        if (ended) {
          return NO_MORE_DOCS;
        } else {
          ended = true;
          docID = postingsArray.lastDocIDs[termID];
          if (readTermFreq) {
            freq = postingsArray.termFreqs[termID];
          }
        }
      } else {
        int code = reader.readVInt();
        if (!readTermFreq) {
          docID += code;
        } else {
          docID += code >>> 1;
          if ((code & 1) != 0) {
            freq = 1;
          } else {
            freq = reader.readVInt();
          }
        }

        assert docID != postingsArray.lastDocIDs[termID];
      }

      return docID;
    }

    @Override
    public int advance(int target) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long cost() {
      throw new UnsupportedOperationException();
    }
  }

  private static class FreqProxDocsAndPositionsEnum extends DocsAndPositionsEnum {

    final FreqProxTermsWriterPerField terms;
    final FreqProxPostingsArray postingsArray;
    final ByteSliceReader reader = new ByteSliceReader();
    final ByteSliceReader posReader = new ByteSliceReader();
    final boolean readOffsets;
    int docID;
    int freq;
    int pos;
    int startOffset;
    int endOffset;
    int posLeft;
    int termID;
    boolean ended;
    boolean hasPayload;
    BytesRef payload = new BytesRef();

    public FreqProxDocsAndPositionsEnum(FreqProxTermsWriterPerField terms, FreqProxPostingsArray postingsArray) {
      this.terms = terms;
      this.postingsArray = postingsArray;
      this.readOffsets = terms.hasOffsets;
      assert terms.hasProx;
      assert terms.hasFreq;
    }

    public void reset(int termID) {
      this.termID = termID;
      terms.termsHashPerField.initReader(reader, termID, 0);
      terms.termsHashPerField.initReader(posReader, termID, 1);
      ended = false;
      docID = 0;
      posLeft = 0;
    }

    @Override
    public int docID() {
      return docID;
    }

    @Override
    public int freq() {
      return freq;
    }

    @Override
    public int nextDoc() throws IOException {
      while (posLeft != 0) {
        nextPosition();
      }

      if (reader.eof()) {
        if (ended) {
          return NO_MORE_DOCS;
        } else {
          ended = true;
          docID = postingsArray.lastDocIDs[termID];
          freq = postingsArray.termFreqs[termID];
        }
      } else {
        int code = reader.readVInt();
        docID += code >>> 1;
        if ((code & 1) != 0) {
          freq = 1;
        } else {
          freq = reader.readVInt();
        }

        assert docID != postingsArray.lastDocIDs[termID];
      }

      posLeft = freq;
      pos = 0;
      startOffset = 0;
      return docID;
    }

    @Override
    public int advance(int target) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long cost() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int nextPosition() throws IOException {
      assert posLeft > 0;
      posLeft--;
      int code = posReader.readVInt();
      pos += code >>> 1;
      if ((code & 1) != 0) {
        hasPayload = true;
        // has a payload
        payload.length = posReader.readVInt();
        if (payload.bytes.length < payload.length) {
          payload.grow(payload.length);
        }
        posReader.readBytes(payload.bytes, 0, payload.length);
      } else {
        hasPayload = false;
      }

      if (readOffsets) {
        startOffset += posReader.readVInt();
        endOffset = startOffset + posReader.readVInt();
      }

      return pos;
    }

    @Override
    public int startOffset() {
      if (!readOffsets) {
        throw new IllegalStateException("offsets were not indexed");
      }
      return startOffset;
    }

    @Override
    public int endOffset() {
      if (!readOffsets) {
        throw new IllegalStateException("offsets were not indexed");
      }
      return endOffset;
    }

    @Override
    public BytesRef getPayload() {
      if (hasPayload) {
        return payload;
      } else {
        return null;
      }
    }
  }
}
diff --git a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
index 476ac2aecd7..7d50b39cb48 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
@@ -19,19 +19,62 @@ package org.apache.lucene.index;
 
 import java.io.IOException;
 import java.util.ArrayList;
import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 
import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.CollectionUtil;
import org.apache.lucene.util.IOUtils;
 
 final class FreqProxTermsWriter extends TermsHashConsumer {
 
   @Override
   void abort() {}
 
  private void applyDeletes(SegmentWriteState state, Fields fields) throws IOException {
    // Process any pending Term deletes for this newly
    // flushed segment:
    if (state.segDeletes != null && state.segDeletes.terms.size() > 0) {
      Map<Term,Integer> segDeletes = state.segDeletes.terms;
      List<Term> deleteTerms = new ArrayList<Term>(segDeletes.keySet());
      Collections.sort(deleteTerms);
      String lastField = null;
      TermsEnum termsEnum = null;
      DocsEnum docsEnum = null;
      for(Term deleteTerm : deleteTerms) {
        if (deleteTerm.field().equals(lastField) == false) {
          lastField = deleteTerm.field();
          Terms terms = fields.terms(lastField);
          if (terms != null) {
            termsEnum = terms.iterator(termsEnum);
          }
        }

        if (termsEnum != null && termsEnum.seekExact(deleteTerm.bytes())) {
          docsEnum = termsEnum.docs(null, docsEnum, 0);
          int delDocLimit = segDeletes.get(deleteTerm);
          while (true) {
            int doc = docsEnum.nextDoc();
            if (doc == DocsEnum.NO_MORE_DOCS) {
              break;
            }
            if (doc < delDocLimit) {
              if (state.liveDocs == null) {
                state.liveDocs = state.segmentInfo.getCodec().liveDocsFormat().newLiveDocs(state.segmentInfo.getDocCount());
              }
              if (state.liveDocs.get(doc)) {
                state.delCountOnFlush++;
                state.liveDocs.clear(doc);
              }
            } else {
              break;
            }
          }
        }
      }
    }
  }

   // TODO: would be nice to factor out more of this, eg the
   // FreqProxFieldMergeState, and code to visit all Fields
   // under the same FieldInfo together, up into TermsHash*.
@@ -47,63 +90,20 @@ final class FreqProxTermsWriter extends TermsHashConsumer {
     for (TermsHashConsumerPerField f : fieldsToFlush.values()) {
       final FreqProxTermsWriterPerField perField = (FreqProxTermsWriterPerField) f;
       if (perField.termsHashPerField.bytesHash.size() > 0) {
        perField.sortPostings();
        assert perField.fieldInfo.isIndexed();
         allFields.add(perField);
       }
     }
 
    final int numAllFields = allFields.size();

     // Sort by field name
     CollectionUtil.introSort(allFields);
 
    final FieldsConsumer consumer = state.segmentInfo.getCodec().postingsFormat().fieldsConsumer(state);

    boolean success = false;

    try {
      TermsHash termsHash = null;
      
      /*
    Current writer chain:
      FieldsConsumer
        -> IMPL: FormatPostingsTermsDictWriter
          -> TermsConsumer
            -> IMPL: FormatPostingsTermsDictWriter.TermsWriter
              -> DocsConsumer
                -> IMPL: FormatPostingsDocsWriter
                  -> PositionsConsumer
                    -> IMPL: FormatPostingsPositionsWriter
       */
      
      for (int fieldNumber = 0; fieldNumber < numAllFields; fieldNumber++) {
        final FieldInfo fieldInfo = allFields.get(fieldNumber).fieldInfo;
        
        final FreqProxTermsWriterPerField fieldWriter = allFields.get(fieldNumber);

        // If this field has postings then add them to the
        // segment
        fieldWriter.flush(fieldInfo.name, consumer, state);
        
        TermsHashPerField perField = fieldWriter.termsHashPerField;
        assert termsHash == null || termsHash == perField.termsHash;
        termsHash = perField.termsHash;
        int numPostings = perField.bytesHash.size();
        perField.reset();
        perField.shrinkHash(numPostings);
        fieldWriter.reset();
      }
      
      if (termsHash != null) {
        termsHash.reset();
      }
      success = true;
    } finally {
      if (success) {
        IOUtils.close(consumer);
      } else {
        IOUtils.closeWhileHandlingException(consumer);
      }
    }
    Fields fields = new FreqProxFields(allFields);

    applyDeletes(state, fields);

    state.segmentInfo.getCodec().postingsFormat().fieldsConsumer(state).write(fields);
   }
 
   BytesRef payload;
diff --git a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriterPerField.java b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriterPerField.java
index 97f6df2691f..99bd4a4a5fd 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriterPerField.java
++ b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriterPerField.java
@@ -17,19 +17,10 @@ package org.apache.lucene.index;
  * limitations under the License.
  */
 
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.PostingsConsumer;
import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.codecs.TermsConsumer;
 import org.apache.lucene.index.FieldInfo.IndexOptions;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
 import org.apache.lucene.util.RamUsageEstimator;
 
 // TODO: break into separate freq and prox writers as
@@ -42,11 +33,16 @@ final class FreqProxTermsWriterPerField extends TermsHashConsumerPerField implem
   final FieldInfo fieldInfo;
   final DocumentsWriterPerThread.DocState docState;
   final FieldInvertState fieldState;
  private boolean hasFreq;
  private boolean hasProx;
  private boolean hasOffsets;
  boolean hasFreq;
  boolean hasProx;
  boolean hasOffsets;
   PayloadAttribute payloadAttribute;
   OffsetAttribute offsetAttribute;
  long sumTotalTermFreq;
  long sumDocFreq;

  // How many docs have this field:
  int docCount;
 
   public FreqProxTermsWriterPerField(TermsHashPerField termsHashPerField, FreqProxTermsWriter parent, FieldInfo fieldInfo) {
     this.termsHashPerField = termsHashPerField;
@@ -68,6 +64,12 @@ final class FreqProxTermsWriterPerField extends TermsHashConsumerPerField implem
 
   @Override
   void finish() {
    sumDocFreq += fieldState.uniqueTermCount;
    sumTotalTermFreq += fieldState.length;
    if (fieldState.length > 0) {
      docCount++;
    }

     if (hasPayloads) {
       fieldInfo.setStorePayloads();
     }
@@ -83,14 +85,6 @@ final class FreqProxTermsWriterPerField extends TermsHashConsumerPerField implem
     return fieldInfo.name.compareTo(other.fieldInfo.name);
   }
 
  // Called after flush
  void reset() {
    // Record, up front, whether our in-RAM format will be
    // with or without term freqs:
    setIndexOptions(fieldInfo.getIndexOptions());
    payloadAttribute = null;
  }

   private void setIndexOptions(IndexOptions indexOptions) {
     if (indexOptions == null) {
       // field could later be updated with indexed=true, so set everything on
@@ -318,233 +312,10 @@ final class FreqProxTermsWriterPerField extends TermsHashConsumerPerField implem
 
   BytesRef payload;
 
  /* Walk through all unique text tokens (Posting
   * instances) found in this field and serialize them
   * into a single RAM segment. */
  void flush(String fieldName, FieldsConsumer consumer,  final SegmentWriteState state)
    throws IOException {

    if (!fieldInfo.isIndexed()) {
      return; // nothing to flush, don't bother the codec with the unindexed field
    }
    
    final TermsConsumer termsConsumer = consumer.addField(fieldInfo);
    final Comparator<BytesRef> termComp = termsConsumer.getComparator();

    // CONFUSING: this.indexOptions holds the index options
    // that were current when we first saw this field.  But
    // it's possible this has changed, eg when other
    // documents are indexed that cause a "downgrade" of the
    // IndexOptions.  So we must decode the in-RAM buffer
    // according to this.indexOptions, but then write the
    // new segment to the directory according to
    // currentFieldIndexOptions:
    final IndexOptions currentFieldIndexOptions = fieldInfo.getIndexOptions();
    assert currentFieldIndexOptions != null;

    final boolean writeTermFreq = currentFieldIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
    final boolean writePositions = currentFieldIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
    final boolean writeOffsets = currentFieldIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;

    final boolean readTermFreq = this.hasFreq;
    final boolean readPositions = this.hasProx;
    final boolean readOffsets = this.hasOffsets;

    //System.out.println("flush readTF=" + readTermFreq + " readPos=" + readPositions + " readOffs=" + readOffsets);

    // Make sure FieldInfo.update is working correctly!:
    assert !writeTermFreq || readTermFreq;
    assert !writePositions || readPositions;
    assert !writeOffsets || readOffsets;

    assert !writeOffsets || writePositions;

    final Map<Term,Integer> segDeletes;
    if (state.segDeletes != null && state.segDeletes.terms.size() > 0) {
      segDeletes = state.segDeletes.terms;
    } else {
      segDeletes = null;
    }

    final int[] termIDs = termsHashPerField.sortPostings(termComp);
    final int numTerms = termsHashPerField.bytesHash.size();
    final BytesRef text = new BytesRef();
    final FreqProxPostingsArray postings = (FreqProxPostingsArray) termsHashPerField.postingsArray;
    final ByteSliceReader freq = new ByteSliceReader();
    final ByteSliceReader prox = new ByteSliceReader();

    FixedBitSet visitedDocs = new FixedBitSet(state.segmentInfo.getDocCount());
    long sumTotalTermFreq = 0;
    long sumDocFreq = 0;

    Term protoTerm = new Term(fieldName);
    for (int i = 0; i < numTerms; i++) {
      final int termID = termIDs[i];
      //System.out.println("term=" + termID);
      // Get BytesRef
      final int textStart = postings.textStarts[termID];
      termsHashPerField.bytePool.setBytesRef(text, textStart);

      termsHashPerField.initReader(freq, termID, 0);
      if (readPositions || readOffsets) {
        termsHashPerField.initReader(prox, termID, 1);
      }

      // TODO: really TermsHashPerField should take over most
      // of this loop, including merge sort of terms from
      // multiple threads and interacting with the
      // TermsConsumer, only calling out to us (passing us the
      // DocsConsumer) to handle delivery of docs/positions

      final PostingsConsumer postingsConsumer = termsConsumer.startTerm(text);

      final int delDocLimit;
      if (segDeletes != null) {
        protoTerm.bytes = text;
        final Integer docIDUpto = segDeletes.get(protoTerm);
        if (docIDUpto != null) {
          delDocLimit = docIDUpto;
        } else {
          delDocLimit = 0;
        }
      } else {
        delDocLimit = 0;
      }

      // Now termStates has numToMerge FieldMergeStates
      // which all share the same term.  Now we must
      // interleave the docID streams.
      int docFreq = 0;
      long totalTermFreq = 0;
      int docID = 0;

      while(true) {
        //System.out.println("  cycle");
        final int termFreq;
        if (freq.eof()) {
          if (postings.lastDocCodes[termID] != -1) {
            // Return last doc
            docID = postings.lastDocIDs[termID];
            if (readTermFreq) {
              termFreq = postings.termFreqs[termID];
            } else {
              termFreq = -1;
            }
            postings.lastDocCodes[termID] = -1;
          } else {
            // EOF
            break;
          }
        } else {
          final int code = freq.readVInt();
          if (!readTermFreq) {
            docID += code;
            termFreq = -1;
          } else {
            docID += code >>> 1;
            if ((code & 1) != 0) {
              termFreq = 1;
            } else {
              termFreq = freq.readVInt();
            }
          }

          assert docID != postings.lastDocIDs[termID];
        }

        docFreq++;
        assert docID < state.segmentInfo.getDocCount(): "doc=" + docID + " maxDoc=" + state.segmentInfo.getDocCount();

        // NOTE: we could check here if the docID was
        // deleted, and skip it.  However, this is somewhat
        // dangerous because it can yield non-deterministic
        // behavior since we may see the docID before we see
        // the term that caused it to be deleted.  This
        // would mean some (but not all) of its postings may
        // make it into the index, which'd alter the docFreq
        // for those terms.  We could fix this by doing two
        // passes, ie first sweep marks all del docs, and
        // 2nd sweep does the real flush, but I suspect
        // that'd add too much time to flush.
        visitedDocs.set(docID);
        postingsConsumer.startDoc(docID, writeTermFreq ? termFreq : -1);
        if (docID < delDocLimit) {
          // Mark it deleted.  TODO: we could also skip
          // writing its postings; this would be
          // deterministic (just for this Term's docs).
          
          // TODO: can we do this reach-around in a cleaner way????
          if (state.liveDocs == null) {
            state.liveDocs = docState.docWriter.codec.liveDocsFormat().newLiveDocs(state.segmentInfo.getDocCount());
          }
          if (state.liveDocs.get(docID)) {
            state.delCountOnFlush++;
            state.liveDocs.clear(docID);
          }
        }

        totalTermFreq += termFreq;
        
        // Carefully copy over the prox + payload info,
        // changing the format to match Lucene's segment
        // format.

        if (readPositions || readOffsets) {
          // we did record positions (& maybe payload) and/or offsets
          int position = 0;
          int offset = 0;
          for(int j=0;j<termFreq;j++) {
            final BytesRef thisPayload;

            if (readPositions) {
              final int code = prox.readVInt();
              position += code >>> 1;

              if ((code & 1) != 0) {

                // This position has a payload
                final int payloadLength = prox.readVInt();

                if (payload == null) {
                  payload = new BytesRef();
                  payload.bytes = new byte[payloadLength];
                } else if (payload.bytes.length < payloadLength) {
                  payload.grow(payloadLength);
                }

                prox.readBytes(payload.bytes, 0, payloadLength);
                payload.length = payloadLength;
                thisPayload = payload;

              } else {
                thisPayload = null;
              }

              if (readOffsets) {
                final int startOffset = offset + prox.readVInt();
                final int endOffset = startOffset + prox.readVInt();
                if (writePositions) {
                  if (writeOffsets) {
                    assert startOffset >=0 && endOffset >= startOffset : "startOffset=" + startOffset + ",endOffset=" + endOffset + ",offset=" + offset;
                    postingsConsumer.addPosition(position, thisPayload, startOffset, endOffset);
                  } else {
                    postingsConsumer.addPosition(position, thisPayload, -1, -1);
                  }
                }
                offset = startOffset;
              } else if (writePositions) {
                postingsConsumer.addPosition(position, thisPayload, -1, -1);
              }
            }
          }
        }
        postingsConsumer.finishDoc();
      }
      termsConsumer.finishTerm(text, new TermStats(docFreq, writeTermFreq ? totalTermFreq : -1));
      sumTotalTermFreq += totalTermFreq;
      sumDocFreq += docFreq;
    }
  int[] sortedTermIDs;
 
    termsConsumer.finish(writeTermFreq ? sumTotalTermFreq : -1, sumDocFreq, visitedDocs.cardinality());
  void sortPostings() {
    assert sortedTermIDs == null;
    sortedTermIDs = termsHashPerField.sortPostings();
   }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/index/MappedMultiFields.java b/lucene/core/src/java/org/apache/lucene/index/MappedMultiFields.java
new file mode 100644
index 00000000000..7bee81e79b0
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/index/MappedMultiFields.java
@@ -0,0 +1,136 @@
package org.apache.lucene.index;

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

import org.apache.lucene.util.Bits;

import static org.apache.lucene.index.FilterAtomicReader.FilterFields;
import static org.apache.lucene.index.FilterAtomicReader.FilterTerms;
import static org.apache.lucene.index.FilterAtomicReader.FilterTermsEnum;

/** A {@link Fields} implementation that merges multiple
 *  Fields into one, and maps around deleted documents.
 *  This is used for merging. */

class MappedMultiFields extends FilterFields {
  final MergeState mergeState;

  public MappedMultiFields(MergeState mergeState, MultiFields multiFields) {
    super(multiFields);
    this.mergeState = mergeState;
  }

  @Override
  public Terms terms(String field) throws IOException {
    MultiTerms terms = (MultiTerms) in.terms(field);
    if (terms == null) {
      return null;
    } else {
      return new MappedMultiTerms(mergeState, terms);
    }
  }

  private static class MappedMultiTerms extends FilterTerms {
    final MergeState mergeState;

    public MappedMultiTerms(MergeState mergeState, MultiTerms multiTerms) {
      super(multiTerms);
      this.mergeState = mergeState;
    }

    @Override
    public TermsEnum iterator(TermsEnum reuse) throws IOException {
      return new MappedMultiTermsEnum(mergeState, (MultiTermsEnum) in.iterator(reuse));
    }

    @Override
    public long size() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getSumTotalTermFreq() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getSumDocFreq() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getDocCount() throws IOException {
      throw new UnsupportedOperationException();
    }
  }

  private static class MappedMultiTermsEnum extends FilterTermsEnum {
    final MergeState mergeState;

    public MappedMultiTermsEnum(MergeState mergeState, MultiTermsEnum multiTermsEnum) {
      super(multiTermsEnum);
      this.mergeState = mergeState;
    }

    @Override
    public int docFreq() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public long totalTermFreq() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public DocsEnum docs(Bits liveDocs, DocsEnum reuse, int flags) throws IOException {
      if (liveDocs != null) {
        throw new IllegalArgumentException("liveDocs must be null");
      }
      MappingMultiDocsEnum mappingDocsEnum;
      if (reuse instanceof MappingMultiDocsEnum) {
        mappingDocsEnum = (MappingMultiDocsEnum) reuse;
      } else {
        mappingDocsEnum = new MappingMultiDocsEnum(mergeState);
      }
      
      MultiDocsEnum docsEnum = (MultiDocsEnum) in.docs(liveDocs, mappingDocsEnum.multiDocsEnum, flags);
      mappingDocsEnum.reset(docsEnum);
      return mappingDocsEnum;
    }

    @Override
    public DocsAndPositionsEnum docsAndPositions(Bits liveDocs, DocsAndPositionsEnum reuse, int flags) throws IOException {
      if (liveDocs != null) {
        throw new IllegalArgumentException("liveDocs must be null");
      }
      MappingMultiDocsAndPositionsEnum mappingDocsAndPositionsEnum;
      if (reuse instanceof MappingMultiDocsAndPositionsEnum) {
        mappingDocsAndPositionsEnum = (MappingMultiDocsAndPositionsEnum) reuse;
      } else {
        mappingDocsAndPositionsEnum = new MappingMultiDocsAndPositionsEnum(mergeState);
      }
      
      MultiDocsAndPositionsEnum docsAndPositionsEnum = (MultiDocsAndPositionsEnum) in.docsAndPositions(liveDocs, mappingDocsAndPositionsEnum.multiDocsAndPositionsEnum, flags);
      mappingDocsAndPositionsEnum.reset(docsAndPositionsEnum);
      return mappingDocsAndPositionsEnum;
    }
  }
}
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/MappingMultiDocsAndPositionsEnum.java b/lucene/core/src/java/org/apache/lucene/index/MappingMultiDocsAndPositionsEnum.java
similarity index 87%
rename from lucene/core/src/java/org/apache/lucene/codecs/MappingMultiDocsAndPositionsEnum.java
rename to lucene/core/src/java/org/apache/lucene/index/MappingMultiDocsAndPositionsEnum.java
index 34aa53be703..bcc3735ad5b 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/MappingMultiDocsAndPositionsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/index/MappingMultiDocsAndPositionsEnum.java
@@ -1,4 +1,4 @@
package org.apache.lucene.codecs;
package org.apache.lucene.index;
 
 /*
  * Licensed to the Apache Software Foundation (ASF) under one or more
@@ -18,9 +18,6 @@ package org.apache.lucene.codecs;
  */
 
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.MultiDocsAndPositionsEnum;
 import org.apache.lucene.index.MultiDocsAndPositionsEnum.EnumWithSlice;
 
 import java.io.IOException;
@@ -32,7 +29,7 @@ import java.io.IOException;
  * @lucene.experimental
  */
 
public final class MappingMultiDocsAndPositionsEnum extends DocsAndPositionsEnum {
final class MappingMultiDocsAndPositionsEnum extends DocsAndPositionsEnum {
   private MultiDocsAndPositionsEnum.EnumWithSlice[] subs;
   int numSubs;
   int upto;
@@ -41,9 +38,11 @@ public final class MappingMultiDocsAndPositionsEnum extends DocsAndPositionsEnum
   int currentBase;
   int doc = -1;
   private MergeState mergeState;
  MultiDocsAndPositionsEnum multiDocsAndPositionsEnum;
 
   /** Sole constructor. */
  public MappingMultiDocsAndPositionsEnum() {
  public MappingMultiDocsAndPositionsEnum(MergeState mergeState) {
    this.mergeState = mergeState;
   }
 
   MappingMultiDocsAndPositionsEnum reset(MultiDocsAndPositionsEnum postingsEnum) {
@@ -51,15 +50,10 @@ public final class MappingMultiDocsAndPositionsEnum extends DocsAndPositionsEnum
     this.subs = postingsEnum.getSubs();
     upto = -1;
     current = null;
    this.multiDocsAndPositionsEnum = postingsEnum;
     return this;
   }
 
  /** Sets the {@link MergeState}, which is used to re-map
   *  document IDs. */
  public void setMergeState(MergeState mergeState) {
    this.mergeState = mergeState;
  }
  
   /** How many sub-readers we are merging.
    *  @see #getSubs */
   public int getNumSubs() {
@@ -103,6 +97,13 @@ public final class MappingMultiDocsAndPositionsEnum extends DocsAndPositionsEnum
 
       int doc = current.nextDoc();
       if (doc != NO_MORE_DOCS) {

        mergeState.checkAbortCount++;
        if (mergeState.checkAbortCount > 60000) {
          mergeState.checkAbort.work(mergeState.checkAbortCount/5.0);
          mergeState.checkAbortCount = 0;
        }

         // compact deletions
         doc = currentMap.get(doc);
         if (doc == -1) {
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/MappingMultiDocsEnum.java b/lucene/core/src/java/org/apache/lucene/index/MappingMultiDocsEnum.java
similarity index 86%
rename from lucene/core/src/java/org/apache/lucene/codecs/MappingMultiDocsEnum.java
rename to lucene/core/src/java/org/apache/lucene/index/MappingMultiDocsEnum.java
index 7f9252a04dd..148ea5c1f9a 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/MappingMultiDocsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/index/MappingMultiDocsEnum.java
@@ -1,4 +1,4 @@
package org.apache.lucene.codecs;
package org.apache.lucene.index;
 
 /*
  * Licensed to the Apache Software Foundation (ASF) under one or more
@@ -17,9 +17,6 @@ package org.apache.lucene.codecs;
  * limitations under the License.
  */
 
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.MultiDocsEnum;
 import org.apache.lucene.index.MultiDocsEnum.EnumWithSlice;
 
 import java.io.IOException;
@@ -31,7 +28,7 @@ import java.io.IOException;
  * @lucene.experimental
  */
 
public final class MappingMultiDocsEnum extends DocsEnum {
final class MappingMultiDocsEnum extends DocsEnum {
   private MultiDocsEnum.EnumWithSlice[] subs;
   int numSubs;
   int upto;
@@ -39,26 +36,23 @@ public final class MappingMultiDocsEnum extends DocsEnum {
   DocsEnum current;
   int currentBase;
   int doc = -1;
  private MergeState mergeState;
  private final MergeState mergeState;
  MultiDocsEnum multiDocsEnum;
 
   /** Sole constructor. */
  public MappingMultiDocsEnum() {
  public MappingMultiDocsEnum(MergeState mergeState) {
    this.mergeState = mergeState;
   }
 
   MappingMultiDocsEnum reset(MultiDocsEnum docsEnum) {
     this.numSubs = docsEnum.getNumSubs();
     this.subs = docsEnum.getSubs();
    this.multiDocsEnum = docsEnum;
     upto = -1;
     current = null;
     return this;
   }
 
  /** Sets the {@link MergeState}, which is used to re-map
   *  document IDs. */
  public void setMergeState(MergeState mergeState) {
    this.mergeState = mergeState;
  }
  
   /** How many sub-readers we are merging.
    *  @see #getSubs */
   public int getNumSubs() {
@@ -103,6 +97,13 @@ public final class MappingMultiDocsEnum extends DocsEnum {
 
       int doc = current.nextDoc();
       if (doc != NO_MORE_DOCS) {

        mergeState.checkAbortCount++;
        if (mergeState.checkAbortCount > 60000) {
          mergeState.checkAbort.work(mergeState.checkAbortCount/5.0);
          mergeState.checkAbortCount = 0;
        }

         // compact deletions
         doc = currentMap.get(doc);
         if (doc == -1) {
diff --git a/lucene/core/src/java/org/apache/lucene/index/MergeState.java b/lucene/core/src/java/org/apache/lucene/index/MergeState.java
index 35e7cc810b5..cc60b8ecafa 100644
-- a/lucene/core/src/java/org/apache/lucene/index/MergeState.java
++ b/lucene/core/src/java/org/apache/lucene/index/MergeState.java
@@ -151,6 +151,10 @@ public class MergeState {
   /** InfoStream for debugging messages. */
   public final InfoStream infoStream;
 
  /** Counter used for periodic calls to checkAbort
   * @lucene.internal */
  public int checkAbortCount;

   // TODO: get rid of this? it tells you which segments are 'aligned' (e.g. for bulk merging)
   // but is this really so expensive to compute again in different components, versus once in SM?
 
diff --git a/lucene/core/src/java/org/apache/lucene/index/MultiTerms.java b/lucene/core/src/java/org/apache/lucene/index/MultiTerms.java
index cbf5c5d9bb2..96994daa9f9 100644
-- a/lucene/core/src/java/org/apache/lucene/index/MultiTerms.java
++ b/lucene/core/src/java/org/apache/lucene/index/MultiTerms.java
@@ -19,7 +19,6 @@ package org.apache.lucene.index;
 
 import java.io.IOException;
 import java.util.ArrayList;
import java.util.Comparator;
 import java.util.List;
 
 import org.apache.lucene.util.BytesRef;
@@ -36,7 +35,6 @@ import org.apache.lucene.util.automaton.CompiledAutomaton;
 public final class MultiTerms extends Terms {
   private final Terms[] subs;
   private final ReaderSlice[] subSlices;
  private final Comparator<BytesRef> termComp;
   private final boolean hasOffsets;
   private final boolean hasPositions;
   private final boolean hasPayloads;
@@ -51,28 +49,16 @@ public final class MultiTerms extends Terms {
     this.subs = subs;
     this.subSlices = subSlices;
     
    Comparator<BytesRef> _termComp = null;
     assert subs.length > 0 : "inefficient: don't use MultiTerms over one sub";
     boolean _hasOffsets = true;
     boolean _hasPositions = true;
     boolean _hasPayloads = false;
     for(int i=0;i<subs.length;i++) {
      if (_termComp == null) {
        _termComp = subs[i].getComparator();
      } else {
        // We cannot merge sub-readers that have
        // different TermComps
        final Comparator<BytesRef> subTermComp = subs[i].getComparator();
        if (subTermComp != null && !subTermComp.equals(_termComp)) {
          throw new IllegalStateException("sub-readers have different BytesRef.Comparators; cannot merge");
        }
      }
       _hasOffsets &= subs[i].hasOffsets();
       _hasPositions &= subs[i].hasPositions();
       _hasPayloads |= subs[i].hasPayloads();
     }
 
    termComp = _termComp;
     hasOffsets = _hasOffsets;
     hasPositions = _hasPositions;
     hasPayloads = hasPositions && _hasPayloads; // if all subs have pos, and at least one has payloads.
@@ -157,11 +143,6 @@ public final class MultiTerms extends Terms {
     return sum;
   }
 
  @Override
  public Comparator<BytesRef> getComparator() {
    return termComp;
  }

   @Override
   public boolean hasOffsets() {
     return hasOffsets;
diff --git a/lucene/core/src/java/org/apache/lucene/index/MultiTermsEnum.java b/lucene/core/src/java/org/apache/lucene/index/MultiTermsEnum.java
index 937804e18db..9e2abdd5706 100644
-- a/lucene/core/src/java/org/apache/lucene/index/MultiTermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/index/MultiTermsEnum.java
@@ -23,7 +23,6 @@ import org.apache.lucene.util.Bits;
 
 import java.io.IOException;
 import java.util.Arrays;
import java.util.Comparator;
 
 /**
  * Exposes {@link TermsEnum} API, merged from {@link TermsEnum} API of sub-segments.
@@ -47,7 +46,6 @@ public final class MultiTermsEnum extends TermsEnum {
   private int numTop;
   private int numSubs;
   private BytesRef current;
  private Comparator<BytesRef> termComp;
 
   static class TermsEnumIndex {
     public final static TermsEnumIndex[] EMPTY_ARRAY = new TermsEnumIndex[0];
@@ -95,36 +93,18 @@ public final class MultiTermsEnum extends TermsEnum {
     return current;
   }
 
  @Override
  public Comparator<BytesRef> getComparator() {
    return termComp;
  }

   /** The terms array must be newly created TermsEnum, ie
    *  {@link TermsEnum#next} has not yet been called. */
   public TermsEnum reset(TermsEnumIndex[] termsEnumsIndex) throws IOException {
     assert termsEnumsIndex.length <= top.length;
     numSubs = 0;
     numTop = 0;
    termComp = null;
     queue.clear();
     for(int i=0;i<termsEnumsIndex.length;i++) {
 
       final TermsEnumIndex termsEnumIndex = termsEnumsIndex[i];
       assert termsEnumIndex != null;
 
      // init our term comp
      if (termComp == null) {
        queue.termComp = termComp = termsEnumIndex.termsEnum.getComparator();
      } else {
        // We cannot merge sub-readers that have
        // different TermComps
        final Comparator<BytesRef> subTermComp = termsEnumIndex.termsEnum.getComparator();
        if (subTermComp != null && !subTermComp.equals(termComp)) {
          throw new IllegalStateException("sub-readers have different BytesRef.Comparators: " + subTermComp + " vs " + termComp + "; cannot merge");
        }
      }

       final BytesRef term = termsEnumIndex.termsEnum.next();
       if (term != null) {
         final TermsEnumWithSlice entry = subs[termsEnumIndex.subIndex];
@@ -149,7 +129,7 @@ public final class MultiTermsEnum extends TermsEnum {
     numTop = 0;
 
     boolean seekOpt = false;
    if (lastSeek != null && termComp.compare(lastSeek, term) <= 0) {
    if (lastSeek != null && lastSeek.compareTo(term) <= 0) {
       seekOpt = true;
     }
 
@@ -167,7 +147,7 @@ public final class MultiTermsEnum extends TermsEnum {
       if (seekOpt) {
         final BytesRef curTerm = currentSubs[i].current;
         if (curTerm != null) {
          final int cmp = termComp.compare(term, curTerm);
          final int cmp = term.compareTo(curTerm);
           if (cmp == 0) {
             status = true;
           } else if (cmp < 0) {
@@ -201,7 +181,7 @@ public final class MultiTermsEnum extends TermsEnum {
     lastSeekExact = false;
 
     boolean seekOpt = false;
    if (lastSeek != null && termComp.compare(lastSeek, term) <= 0) {
    if (lastSeek != null && lastSeek.compareTo(term) <= 0) {
       seekOpt = true;
     }
 
@@ -219,7 +199,7 @@ public final class MultiTermsEnum extends TermsEnum {
       if (seekOpt) {
         final BytesRef curTerm = currentSubs[i].current;
         if (curTerm != null) {
          final int cmp = termComp.compare(term, curTerm);
          final int cmp = term.compareTo(curTerm);
           if (cmp == 0) {
             status = SeekStatus.FOUND;
           } else if (cmp < 0) {
@@ -519,14 +499,13 @@ public final class MultiTermsEnum extends TermsEnum {
   }
 
   private final static class TermMergeQueue extends PriorityQueue<TermsEnumWithSlice> {
    Comparator<BytesRef> termComp;
     TermMergeQueue(int size) {
       super(size);
     }
 
     @Override
     protected boolean lessThan(TermsEnumWithSlice termsA, TermsEnumWithSlice termsB) {
      final int cmp = termComp.compare(termsA.current, termsB.current);
      final int cmp = termsA.current.compareTo(termsB.current);
       if (cmp != 0) {
         return cmp < 0;
       } else {
diff --git a/lucene/core/src/java/org/apache/lucene/index/SegmentMerger.java b/lucene/core/src/java/org/apache/lucene/index/SegmentMerger.java
index 718687bcc85..8b513139cc9 100644
-- a/lucene/core/src/java/org/apache/lucene/index/SegmentMerger.java
++ b/lucene/core/src/java/org/apache/lucene/index/SegmentMerger.java
@@ -22,9 +22,8 @@ import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.FieldInfosWriter;
import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.codecs.FieldInfosWriter;
 import org.apache.lucene.codecs.StoredFieldsWriter;
 import org.apache.lucene.codecs.TermVectorsWriter;
 import org.apache.lucene.index.FieldInfo.DocValuesType;
@@ -375,19 +374,10 @@ final class SegmentMerger {
       docBase += maxDoc;
     }
 
    final FieldsConsumer consumer = codec.postingsFormat().fieldsConsumer(segmentWriteState);
    boolean success = false;
    try {
      consumer.merge(mergeState,
                     new MultiFields(fields.toArray(Fields.EMPTY_ARRAY),
                                     slices.toArray(ReaderSlice.EMPTY_ARRAY)));
      success = true;
    } finally {
      if (success) {
        IOUtils.close(consumer);
      } else {
        IOUtils.closeWhileHandlingException(consumer);
      }
    }
    Fields mergedFields = new MappedMultiFields(mergeState, 
                                                new MultiFields(fields.toArray(Fields.EMPTY_ARRAY),
                                                                slices.toArray(ReaderSlice.EMPTY_ARRAY)));

    codec.postingsFormat().fieldsConsumer(segmentWriteState).write(mergedFields);
   }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/index/SortedDocValuesTermsEnum.java b/lucene/core/src/java/org/apache/lucene/index/SortedDocValuesTermsEnum.java
index e3d9c5cdc17..0dedfab5f38 100644
-- a/lucene/core/src/java/org/apache/lucene/index/SortedDocValuesTermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/index/SortedDocValuesTermsEnum.java
@@ -18,7 +18,6 @@ package org.apache.lucene.index;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
@@ -124,11 +123,6 @@ class SortedDocValuesTermsEnum extends TermsEnum {
     throw new UnsupportedOperationException();
   }
 
  @Override
  public Comparator<BytesRef> getComparator() {
    return BytesRef.getUTF8SortedAsUnicodeComparator();
  }

   @Override
   public void seekExact(BytesRef term, TermState state) throws IOException {
     assert state != null && state instanceof OrdTermState;
diff --git a/lucene/core/src/java/org/apache/lucene/index/SortedSetDocValuesTermsEnum.java b/lucene/core/src/java/org/apache/lucene/index/SortedSetDocValuesTermsEnum.java
index 3c04135205f..a48f3ebca9d 100644
-- a/lucene/core/src/java/org/apache/lucene/index/SortedSetDocValuesTermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/index/SortedSetDocValuesTermsEnum.java
@@ -18,7 +18,6 @@ package org.apache.lucene.index;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
@@ -124,11 +123,6 @@ class SortedSetDocValuesTermsEnum extends TermsEnum {
     throw new UnsupportedOperationException();
   }
 
  @Override
  public Comparator<BytesRef> getComparator() {
    return BytesRef.getUTF8SortedAsUnicodeComparator();
  }

   @Override
   public void seekExact(BytesRef term, TermState state) throws IOException {
     assert state != null && state instanceof OrdTermState;
diff --git a/lucene/core/src/java/org/apache/lucene/index/TermVectorsConsumer.java b/lucene/core/src/java/org/apache/lucene/index/TermVectorsConsumer.java
index f548eeab2b9..f8b9cca74fc 100644
-- a/lucene/core/src/java/org/apache/lucene/index/TermVectorsConsumer.java
++ b/lucene/core/src/java/org/apache/lucene/index/TermVectorsConsumer.java
@@ -66,12 +66,6 @@ final class TermVectorsConsumer extends TermsHashConsumer {
         hasVectors = false;
       }
     }

    for (final TermsHashConsumerPerField field : fieldsToFlush.values() ) {
      TermVectorsConsumerPerField perField = (TermVectorsConsumerPerField) field;
      perField.termsHashPerField.reset();
      perField.shrinkHash();
    }
   }
 
   /** Fills in no-term-vectors for all docs we haven't seen
diff --git a/lucene/core/src/java/org/apache/lucene/index/TermVectorsConsumerPerField.java b/lucene/core/src/java/org/apache/lucene/index/TermVectorsConsumerPerField.java
index 7dc13d56d26..d2f99f8a08d 100644
-- a/lucene/core/src/java/org/apache/lucene/index/TermVectorsConsumerPerField.java
++ b/lucene/core/src/java/org/apache/lucene/index/TermVectorsConsumerPerField.java
@@ -156,7 +156,7 @@ final class TermVectorsConsumerPerField extends TermsHashConsumerPerField {
     TermVectorsPostingsArray postings = (TermVectorsPostingsArray) termsHashPerField.postingsArray;
     final TermVectorsWriter tv = termsWriter.writer;
 
    final int[] termIDs = termsHashPerField.sortPostings(tv.getComparator());
    final int[] termIDs = termsHashPerField.sortPostings();
 
     tv.startField(fieldInfo, numPostings, doVectorPositions, doVectorOffsets, hasPayloads);
     
@@ -191,11 +191,6 @@ final class TermVectorsConsumerPerField extends TermsHashConsumerPerField {
     fieldInfo.setStoreTermVectors();
   }
 
  void shrinkHash() {
    termsHashPerField.shrinkHash(maxNumPostings);
    maxNumPostings = 0;
  }

   @Override
   void start(IndexableField f) {
     if (doVectorOffsets) {
diff --git a/lucene/core/src/java/org/apache/lucene/index/Terms.java b/lucene/core/src/java/org/apache/lucene/index/Terms.java
index 45924d40d0d..179bb0ad1b8 100644
-- a/lucene/core/src/java/org/apache/lucene/index/Terms.java
++ b/lucene/core/src/java/org/apache/lucene/index/Terms.java
@@ -18,7 +18,6 @@ package org.apache.lucene.index;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.automaton.CompiledAutomaton;
@@ -75,13 +74,6 @@ public abstract class Terms {
     }
   }
 
  /** Return the BytesRef Comparator used to sort terms
   *  provided by the iterator.  This method may return null
   *  if there are no terms.  This method may be invoked
   *  many times; it's best to cache a single instance &
   *  reuse it. */
  public abstract Comparator<BytesRef> getComparator();

   /** Returns the number of terms for this field, or -1 if this 
    *  measure isn't stored by the codec. Note that, just like 
    *  other term measures, this measure does not take deleted 
@@ -109,6 +101,8 @@ public abstract class Terms {
    *  measures, this measure does not take deleted documents
    *  into account. */
   public abstract int getDocCount() throws IOException;

  // TODO: shouldn't we have hasFreq() as well?
   
   /** Returns true if documents in this field store offsets. */
   public abstract boolean hasOffsets();
diff --git a/lucene/core/src/java/org/apache/lucene/index/TermsEnum.java b/lucene/core/src/java/org/apache/lucene/index/TermsEnum.java
index b54c19ef9e4..895018be0da 100644
-- a/lucene/core/src/java/org/apache/lucene/index/TermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/index/TermsEnum.java
@@ -18,7 +18,6 @@ package org.apache.lucene.index;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.util.AttributeSource;
 import org.apache.lucene.util.Bits;
@@ -33,8 +32,9 @@ import org.apache.lucene.util.BytesRefIterator;
  * #docs}.
  * 
  * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * greater than the one before it.</p>
 * BytesRef.compareTo, which is Unicode sort
 * order if the terms are UTF-8 bytes.  Each term in the
 * enumeration is greater than the one before it.</p>
  *
  * <p>The TermsEnum is unpositioned when you first obtain it
  * and you must first successfully call {@link #next} or one
@@ -229,11 +229,6 @@ public abstract class TermsEnum implements BytesRefIterator {
       throw new IllegalStateException("this method should never be called");
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return null;
    }
      
     @Override
     public int docFreq() {
       throw new IllegalStateException("this method should never be called");
diff --git a/lucene/core/src/java/org/apache/lucene/index/TermsHashPerField.java b/lucene/core/src/java/org/apache/lucene/index/TermsHashPerField.java
index 723253999d8..bb67d642c3b 100644
-- a/lucene/core/src/java/org/apache/lucene/index/TermsHashPerField.java
++ b/lucene/core/src/java/org/apache/lucene/index/TermsHashPerField.java
@@ -18,7 +18,6 @@ package org.apache.lucene.index;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
 import org.apache.lucene.util.ByteBlockPool;
@@ -77,13 +76,7 @@ final class TermsHashPerField extends InvertedDocConsumerPerField {
       nextPerField = null;
   }
 
  void shrinkHash(int targetSize) {
    // Fully free the bytesHash on each flush but keep the pool untouched
    // bytesHash.clear will clear the ByteStartArray and in turn the ParallelPostingsArray too
    bytesHash.clear(false);
  }

  public void reset() {
  void reset() {
     bytesHash.clear(false);
     if (nextPerField != null)
       nextPerField.reset();
@@ -107,8 +100,8 @@ final class TermsHashPerField extends InvertedDocConsumerPerField {
   }
 
   /** Collapse the hash table & sort in-place. */
  public int[] sortPostings(Comparator<BytesRef> termComp) {
   return bytesHash.sort(termComp);
  public int[] sortPostings() {
    return bytesHash.sort(BytesRef.getUTF8SortedAsUnicodeComparator());
   }
 
   private boolean doCall;
@@ -136,7 +129,8 @@ final class TermsHashPerField extends InvertedDocConsumerPerField {
 
   // Secondary entry point (for 2nd & subsequent TermsHash),
   // because token text has already been "interned" into
  // textStart, so we hash by textStart
  // textStart, so we hash by textStart.  term vectors use
  // this API.
   public void add(int textStart) throws IOException {
     int termID = bytesHash.addByPoolOffset(textStart);
     if (termID >= 0) {      // New posting
@@ -173,7 +167,8 @@ final class TermsHashPerField extends InvertedDocConsumerPerField {
     }
   }
 
  // Primary entry point (for first TermsHash)
  // Primary entry point (for first TermsHash); postings use
  // this API.
   @Override
   void add() throws IOException {
 
diff --git a/lucene/core/src/java/org/apache/lucene/search/ConstantScoreAutoRewrite.java b/lucene/core/src/java/org/apache/lucene/search/ConstantScoreAutoRewrite.java
index d36b1a37c9c..3b8ebde94ff 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ConstantScoreAutoRewrite.java
++ b/lucene/core/src/java/org/apache/lucene/search/ConstantScoreAutoRewrite.java
@@ -101,7 +101,7 @@ class ConstantScoreAutoRewrite extends TermCollectingRewrite<BooleanQuery> {
     } else {
       final BooleanQuery bq = getTopLevelQuery();
       final BytesRefHash pendingTerms = col.pendingTerms;
      final int sort[] = pendingTerms.sort(col.termsEnum.getComparator());
      final int sort[] = pendingTerms.sort(BytesRef.getUTF8SortedAsUnicodeComparator());
       for(int i = 0; i < size; i++) {
         final int pos = sort[i];
         // docFreq is not used for constant score here, we pass 1
diff --git a/lucene/core/src/java/org/apache/lucene/search/DocTermOrdsRewriteMethod.java b/lucene/core/src/java/org/apache/lucene/search/DocTermOrdsRewriteMethod.java
index 5e06af1f728..79a1c13cf69 100644
-- a/lucene/core/src/java/org/apache/lucene/search/DocTermOrdsRewriteMethod.java
++ b/lucene/core/src/java/org/apache/lucene/search/DocTermOrdsRewriteMethod.java
@@ -18,7 +18,6 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.IndexReader;
@@ -26,7 +25,6 @@ import org.apache.lucene.index.SortedSetDocValues;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.OpenBitSet;
 
 /**
@@ -90,11 +88,6 @@ public final class DocTermOrdsRewriteMethod extends MultiTermQuery.RewriteMethod
       final OpenBitSet termSet = new OpenBitSet(docTermOrds.getValueCount());
       TermsEnum termsEnum = query.getTermsEnum(new Terms() {
         
        @Override
        public Comparator<BytesRef> getComparator() {
          return BytesRef.getUTF8SortedAsUnicodeComparator();
        }
        
         @Override
         public TermsEnum iterator(TermsEnum reuse) {
           return docTermOrds.termsEnum();
diff --git a/lucene/core/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java b/lucene/core/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java
index 825bd89410a..1e96781faf3 100644
-- a/lucene/core/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java
++ b/lucene/core/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java
@@ -18,7 +18,6 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.IndexReader;
@@ -26,7 +25,6 @@ import org.apache.lucene.index.SortedDocValues;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.OpenBitSet;
 
 /**
@@ -90,11 +88,6 @@ public final class FieldCacheRewriteMethod extends MultiTermQuery.RewriteMethod
       final OpenBitSet termSet = new OpenBitSet(fcsi.getValueCount());
       TermsEnum termsEnum = query.getTermsEnum(new Terms() {
         
        @Override
        public Comparator<BytesRef> getComparator() {
          return BytesRef.getUTF8SortedAsUnicodeComparator();
        }
        
         @Override
         public TermsEnum iterator(TermsEnum reuse) {
           return fcsi.termsEnum();
diff --git a/lucene/core/src/java/org/apache/lucene/search/FuzzyTermsEnum.java b/lucene/core/src/java/org/apache/lucene/search/FuzzyTermsEnum.java
index 85698fed538..8e2bf8bc4d1 100644
-- a/lucene/core/src/java/org/apache/lucene/search/FuzzyTermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/search/FuzzyTermsEnum.java
@@ -46,7 +46,7 @@ import org.apache.lucene.util.automaton.LevenshteinAutomata;
  * to the specified filter term.
  *
  * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * {@link BytesRef#compareTo}.  Each term in the enumeration is
  * greater than all that precede it.</p>
  */
 public class FuzzyTermsEnum extends TermsEnum {
@@ -292,11 +292,6 @@ public class FuzzyTermsEnum extends TermsEnum {
     return actualEnum.termState();
   }
   
  @Override
  public Comparator<BytesRef> getComparator() {
    return actualEnum.getComparator();
  }
  
   @Override
   public long ord() throws IOException {
     return actualEnum.ord();
diff --git a/lucene/core/src/java/org/apache/lucene/search/NumericRangeQuery.java b/lucene/core/src/java/org/apache/lucene/search/NumericRangeQuery.java
index 2d7cbe402fd..1ba70306f7f 100644
-- a/lucene/core/src/java/org/apache/lucene/search/NumericRangeQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/NumericRangeQuery.java
@@ -18,7 +18,6 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 import java.util.LinkedList;
 
 import org.apache.lucene.analysis.NumericTokenStream; // for javadocs
@@ -392,7 +391,6 @@ public final class NumericRangeQuery<T extends Number> extends MultiTermQuery {
     private BytesRef currentLowerBound, currentUpperBound;
 
     private final LinkedList<BytesRef> rangeBounds = new LinkedList<BytesRef>();
    private final Comparator<BytesRef> termComp;
 
     NumericRangeTermsEnum(final TermsEnum tenum) {
       super(tenum);
@@ -481,15 +479,13 @@ public final class NumericRangeQuery<T extends Number> extends MultiTermQuery {
           // should never happen
           throw new IllegalArgumentException("Invalid NumericType");
       }

      termComp = getComparator();
     }
     
     private void nextRange() {
       assert rangeBounds.size() % 2 == 0;
 
       currentLowerBound = rangeBounds.removeFirst();
      assert currentUpperBound == null || termComp.compare(currentUpperBound, currentLowerBound) <= 0 :
      assert currentUpperBound == null || currentUpperBound.compareTo(currentLowerBound) <= 0 :
         "The current upper bound must be <= the new lower bound";
       
       currentUpperBound = rangeBounds.removeFirst();
@@ -501,10 +497,10 @@ public final class NumericRangeQuery<T extends Number> extends MultiTermQuery {
         nextRange();
         
         // if the new upper bound is before the term parameter, the sub-range is never a hit
        if (term != null && termComp.compare(term, currentUpperBound) > 0)
        if (term != null && term.compareTo(currentUpperBound) > 0)
           continue;
         // never seek backwards, so use current term if lower bound is smaller
        return (term != null && termComp.compare(term, currentLowerBound) > 0) ?
        return (term != null && term.compareTo(currentLowerBound) > 0) ?
           term : currentLowerBound;
       }
       
@@ -516,11 +512,11 @@ public final class NumericRangeQuery<T extends Number> extends MultiTermQuery {
     
     @Override
     protected final AcceptStatus accept(BytesRef term) {
      while (currentUpperBound == null || termComp.compare(term, currentUpperBound) > 0) {
      while (currentUpperBound == null || term.compareTo(currentUpperBound) > 0) {
         if (rangeBounds.isEmpty())
           return AcceptStatus.END;
         // peek next sub-range, only seek if the current term is smaller than next lower bound
        if (termComp.compare(term, rangeBounds.getFirst()) < 0)
        if (term.compareTo(rangeBounds.getFirst()) < 0)
           return AcceptStatus.NO_AND_SEEK;
         // step forward to next range without seeking, as next lower range bound is less or equal current term
         nextRange();
diff --git a/lucene/core/src/java/org/apache/lucene/search/PrefixTermsEnum.java b/lucene/core/src/java/org/apache/lucene/search/PrefixTermsEnum.java
index 96184b6611b..c50233f3003 100644
-- a/lucene/core/src/java/org/apache/lucene/search/PrefixTermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/search/PrefixTermsEnum.java
@@ -26,7 +26,7 @@ import org.apache.lucene.util.StringHelper;
  * Subclass of FilteredTermEnum for enumerating all terms that match the
  * specified prefix filter term.
  * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * {@link BytesRef#compareTo}.  Each term in the enumeration is
  * greater than all that precede it.</p>
  */
 public class PrefixTermsEnum extends FilteredTermsEnum {
diff --git a/lucene/core/src/java/org/apache/lucene/search/ScoringRewrite.java b/lucene/core/src/java/org/apache/lucene/search/ScoringRewrite.java
index 662e00f3542..152c1f8039b 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ScoringRewrite.java
++ b/lucene/core/src/java/org/apache/lucene/search/ScoringRewrite.java
@@ -109,7 +109,7 @@ public abstract class ScoringRewrite<Q extends Query> extends TermCollectingRewr
     
     final int size = col.terms.size();
     if (size > 0) {
      final int sort[] = col.terms.sort(col.termsEnum.getComparator());
      final int sort[] = col.terms.sort(BytesRef.getUTF8SortedAsUnicodeComparator());
       final float[] boost = col.array.boost;
       final TermContext[] termStates = col.array.termState;
       for (int i = 0; i < size; i++) {
diff --git a/lucene/core/src/java/org/apache/lucene/search/TermCollectingRewrite.java b/lucene/core/src/java/org/apache/lucene/search/TermCollectingRewrite.java
index a0534111e9e..07d97278c24 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TermCollectingRewrite.java
++ b/lucene/core/src/java/org/apache/lucene/search/TermCollectingRewrite.java
@@ -18,7 +18,6 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.Fields;
@@ -47,7 +46,6 @@ abstract class TermCollectingRewrite<Q extends Query> extends MultiTermQuery.Rew
   
   final void collectTerms(IndexReader reader, MultiTermQuery query, TermCollector collector) throws IOException {
     IndexReaderContext topReaderContext = reader.getContext();
    Comparator<BytesRef> lastTermComp = null;
     for (AtomicReaderContext context : topReaderContext.leaves()) {
       final Fields fields = context.reader().fields();
       if (fields == null) {
@@ -67,11 +65,6 @@ abstract class TermCollectingRewrite<Q extends Query> extends MultiTermQuery.Rew
       if (termsEnum == TermsEnum.EMPTY)
         continue;
       
      // Check comparator compatibility:
      final Comparator<BytesRef> newTermComp = termsEnum.getComparator();
      if (lastTermComp != null && newTermComp != null && newTermComp != lastTermComp)
        throw new RuntimeException("term comparator should not change between segments: "+lastTermComp+" != "+newTermComp);
      lastTermComp = newTermComp;
       collector.setReaderContext(topReaderContext, context);
       collector.setNextEnum(termsEnum);
       BytesRef bytes;
diff --git a/lucene/core/src/java/org/apache/lucene/search/TermRangeTermsEnum.java b/lucene/core/src/java/org/apache/lucene/search/TermRangeTermsEnum.java
index c6b0a202013..184413ff35d 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TermRangeTermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/search/TermRangeTermsEnum.java
@@ -17,18 +17,13 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import java.io.IOException;
import java.util.Comparator;

 import org.apache.lucene.index.FilteredTermsEnum;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.util.BytesRef;
 
 /**
  * Subclass of FilteredTermEnum for enumerating all terms that match the
 * specified range parameters.
 * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * specified range parameters.  Each term in the enumeration is
  * greater than all that precede it.</p>
  */
 public class TermRangeTermsEnum extends FilteredTermsEnum {
@@ -37,7 +32,6 @@ public class TermRangeTermsEnum extends FilteredTermsEnum {
   final private boolean includeUpper;
   final private BytesRef lowerBytesRef;
   final private BytesRef upperBytesRef;
  private final Comparator<BytesRef> termComp;
 
   /**
    * Enumerates all terms greater/equal than <code>lowerTerm</code>
@@ -82,7 +76,6 @@ public class TermRangeTermsEnum extends FilteredTermsEnum {
     }
 
     setInitialSeekTerm(lowerBytesRef);
    termComp = getComparator();
   }
 
   @Override
@@ -92,7 +85,7 @@ public class TermRangeTermsEnum extends FilteredTermsEnum {
     
     // Use this field's default sort ordering
     if (upperBytesRef != null) {
      final int cmp = termComp.compare(upperBytesRef, term);
      final int cmp = upperBytesRef.compareTo(term);
       /*
        * if beyond the upper term, or is exclusive and this is equal to
        * the upper term, break out
diff --git a/lucene/core/src/java/org/apache/lucene/search/TopTermsRewrite.java b/lucene/core/src/java/org/apache/lucene/search/TopTermsRewrite.java
index b3c6ec4b09a..0bbf3f3d000 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TopTermsRewrite.java
++ b/lucene/core/src/java/org/apache/lucene/search/TopTermsRewrite.java
@@ -70,20 +70,18 @@ public abstract class TopTermsRewrite<Q extends Query> extends TermCollectingRew
       private final Map<BytesRef,ScoreTerm> visitedTerms = new HashMap<BytesRef,ScoreTerm>();
       
       private TermsEnum termsEnum;
      private Comparator<BytesRef> termComp;
       private BoostAttribute boostAtt;        
       private ScoreTerm st;
       
       @Override
       public void setNextEnum(TermsEnum termsEnum) {
         this.termsEnum = termsEnum;
        this.termComp = termsEnum.getComparator();
         
         assert compareToLastTerm(null);
 
         // lazy init the initial ScoreTerm because comparator is not known on ctor:
         if (st == null)
          st = new ScoreTerm(this.termComp, new TermContext(topReaderContext));
          st = new ScoreTerm(new TermContext(topReaderContext));
         boostAtt = termsEnum.attributes().addAttribute(BoostAttribute.class);
       }
     
@@ -95,7 +93,7 @@ public abstract class TopTermsRewrite<Q extends Query> extends TermCollectingRew
         } else if (t == null) {
           lastTerm = null;
         } else {
          assert termsEnum.getComparator().compare(lastTerm, t) < 0: "lastTerm=" + lastTerm + " t=" + t;
          assert lastTerm.compareTo(t) < 0: "lastTerm=" + lastTerm + " t=" + t;
           lastTerm.copyBytes(t);
         }
         return true;
@@ -115,7 +113,7 @@ public abstract class TopTermsRewrite<Q extends Query> extends TermCollectingRew
           final ScoreTerm t = stQueue.peek();
           if (boost < t.boost)
             return true;
          if (boost == t.boost && termComp.compare(bytes, t.bytes) > 0)
          if (boost == t.boost && bytes.compareTo(t.bytes) > 0)
             return true;
         }
         ScoreTerm t = visitedTerms.get(bytes);
@@ -139,7 +137,7 @@ public abstract class TopTermsRewrite<Q extends Query> extends TermCollectingRew
             visitedTerms.remove(st.bytes);
             st.termState.clear(); // reset the termstate! 
           } else {
            st = new ScoreTerm(termComp, new TermContext(topReaderContext));
            st = new ScoreTerm(new TermContext(topReaderContext));
           }
           assert stQueue.size() <= maxSize : "the PQ size must be limited to maxSize";
           // set maxBoostAtt with values to help FuzzyTermsEnum to optimize
@@ -185,26 +183,22 @@ public abstract class TopTermsRewrite<Q extends Query> extends TermCollectingRew
     new Comparator<ScoreTerm>() {
       @Override
       public int compare(ScoreTerm st1, ScoreTerm st2) {
        assert st1.termComp == st2.termComp :
          "term comparator should not change between segments";
        return st1.termComp.compare(st1.bytes, st2.bytes);
        return st1.bytes.compareTo(st2.bytes);
       }
     };
 
   static final class ScoreTerm implements Comparable<ScoreTerm> {
    public final Comparator<BytesRef> termComp;
     public final BytesRef bytes = new BytesRef();
     public float boost;
     public final TermContext termState;
    public ScoreTerm(Comparator<BytesRef> termComp, TermContext termState) {
      this.termComp = termComp;
    public ScoreTerm(TermContext termState) {
       this.termState = termState;
     }
     
     @Override
     public int compareTo(ScoreTerm other) {
       if (this.boost == other.boost)
        return termComp.compare(other.bytes, this.bytes);
        return other.bytes.compareTo(this.bytes);
       else
         return Float.compare(this.boost, other.boost);
     }
diff --git a/lucene/core/src/java/org/apache/lucene/search/package.html b/lucene/core/src/java/org/apache/lucene/search/package.html
index 4be5eba1277..1a9e5773fb4 100644
-- a/lucene/core/src/java/org/apache/lucene/search/package.html
++ b/lucene/core/src/java/org/apache/lucene/search/package.html
@@ -173,7 +173,7 @@ section for more notes on the process.
     {@link org.apache.lucene.index.Term Term}
     and an upper
     {@link org.apache.lucene.index.Term Term}
    according to {@link org.apache.lucene.index.TermsEnum#getComparator TermsEnum.getComparator()}. It is not intended
    according to {@link org.apache.lucene.util.BytesRef#compareTo BytesRef.compareTo()}. It is not intended
     for numerical ranges; use {@link org.apache.lucene.search.NumericRangeQuery NumericRangeQuery} instead.
 
     For example, one could find all documents
diff --git a/lucene/core/src/java/org/apache/lucene/util/BytesRefIterator.java b/lucene/core/src/java/org/apache/lucene/util/BytesRefIterator.java
index fe9877baca7..063cc34279d 100644
-- a/lucene/core/src/java/org/apache/lucene/util/BytesRefIterator.java
++ b/lucene/core/src/java/org/apache/lucene/util/BytesRefIterator.java
@@ -18,7 +18,6 @@ package org.apache.lucene.util;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 /**
  * A simple iterator interface for {@link BytesRef} iteration.
@@ -38,14 +37,6 @@ public interface BytesRefIterator {
    */
   public BytesRef next() throws IOException;
   
  /**
   * Return the {@link BytesRef} Comparator used to sort terms provided by the
   * iterator. This may return null if there are no items or the iterator is not
   * sorted. Callers may invoke this method many times, so it's best to cache a
   * single instance & reuse it.
   */
  public Comparator<BytesRef> getComparator();

   /** Singleton BytesRefIterator that iterates over 0 BytesRefs. */
   public static final BytesRefIterator EMPTY = new BytesRefIterator() {
 
@@ -53,10 +44,5 @@ public interface BytesRefIterator {
     public BytesRef next() {
       return null;
     }
    
    @Override
    public Comparator<BytesRef> getComparator() {
      return null;
    }
   };
 }
diff --git a/lucene/core/src/test/org/apache/lucene/codecs/lucene41/TestBlockPostingsFormat3.java b/lucene/core/src/test/org/apache/lucene/codecs/lucene41/TestBlockPostingsFormat3.java
index 22276c16d26..fe683e3acf7 100644
-- a/lucene/core/src/test/org/apache/lucene/codecs/lucene41/TestBlockPostingsFormat3.java
++ b/lucene/core/src/test/org/apache/lucene/codecs/lucene41/TestBlockPostingsFormat3.java
@@ -261,7 +261,6 @@ public class TestBlockPostingsFormat3 extends LuceneTestCase {
    * checks collection-level statistics on Terms 
    */
   public void assertTermsStatistics(Terms leftTerms, Terms rightTerms) throws Exception {
    assert leftTerms.getComparator() == rightTerms.getComparator();
     if (leftTerms.getDocCount() != -1 && rightTerms.getDocCount() != -1) {
       assertEquals(leftTerms.getDocCount(), rightTerms.getDocCount());
     }
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestCodecs.java b/lucene/core/src/test/org/apache/lucene/index/TestCodecs.java
index a446e4b13be..55a5f58ca8c 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestCodecs.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestCodecs.java
@@ -25,11 +25,7 @@ import java.util.Random;
 
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsConsumer;
import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.codecs.TermsConsumer;
 import org.apache.lucene.codecs.lucene40.Lucene40RWCodec;
 import org.apache.lucene.codecs.lucene41.Lucene41RWCodec;
 import org.apache.lucene.codecs.lucene42.Lucene42RWCodec;
@@ -48,11 +44,11 @@ import org.apache.lucene.search.PhraseQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.Constants;
 import org.apache.lucene.util.InfoStream;
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.OpenBitSet;
 import org.apache.lucene.util._TestUtil;
 import org.junit.BeforeClass;
 
@@ -145,22 +141,6 @@ public class TestCodecs extends LuceneTestCase {
     public int compareTo(final FieldData other) {
       return fieldInfo.name.compareTo(other.fieldInfo.name);
     }

    public void write(final FieldsConsumer consumer) throws Throwable {
      Arrays.sort(terms);
      final TermsConsumer termsConsumer = consumer.addField(fieldInfo);
      long sumTotalTermCount = 0;
      long sumDF = 0;
      OpenBitSet visitedDocs = new OpenBitSet();
      for (final TermData term : terms) {
        for (int i = 0; i < term.docs.length; i++) {
          visitedDocs.set(term.docs[i]);
        }
        sumDF += term.docs.length;
        sumTotalTermCount += term.write(termsConsumer);
      }
      termsConsumer.finish(omitTF ? -1 : sumTotalTermCount, sumDF, (int) visitedDocs.cardinality());
    }
   }
 
   class PositionData {
@@ -191,30 +171,6 @@ public class TestCodecs extends LuceneTestCase {
     public int compareTo(final TermData o) {
       return text.compareTo(o.text);
     }

    public long write(final TermsConsumer termsConsumer) throws Throwable {
      final PostingsConsumer postingsConsumer = termsConsumer.startTerm(text);
      long totTF = 0;
      for(int i=0;i<docs.length;i++) {
        final int termDocFreq;
        if (field.omitTF) {
          termDocFreq = -1;
        } else {
          termDocFreq = positions[i].length;
        }
        postingsConsumer.startDoc(docs[i], termDocFreq);
        if (!field.omitTF) {
          totTF += positions[i].length;
          for(int j=0;j<positions[i].length;j++) {
            final PositionData pos = positions[i][j];
            postingsConsumer.addPosition(pos.pos, pos.payload, -1, -1);
          }
        }
        postingsConsumer.finishDoc();
      }
      termsConsumer.finishTerm(text, new TermStats(docs.length, field.omitTF ? -1 : totTF));
      return totTF;
    }
   }
 
   final private static String SEGMENT = "0";
@@ -588,18 +544,16 @@ public class TestCodecs extends LuceneTestCase {
           term = field.terms[upto];
           if (random().nextInt(3) == 1) {
             final DocsEnum docs;
            final DocsEnum docsAndFreqs;
             final DocsAndPositionsEnum postings;
             if (!field.omitTF) {
               postings = termsEnum.docsAndPositions(null, null);
               if (postings != null) {
                docs = docsAndFreqs = postings;
                docs = postings;
               } else {
                docs = docsAndFreqs = _TestUtil.docs(random(), termsEnum, null, null, DocsEnum.FLAG_FREQS);
                docs = _TestUtil.docs(random(), termsEnum, null, null, DocsEnum.FLAG_FREQS);
               }
             } else {
               postings = null;
              docsAndFreqs = null;
               docs = _TestUtil.docs(random(), termsEnum, null, null, DocsEnum.FLAG_NONE);
             }
             assertNotNull(docs);
@@ -657,18 +611,250 @@ public class TestCodecs extends LuceneTestCase {
     }
   }
 
  private static class DataFields extends Fields {
    private final FieldData[] fields;

    public DataFields(FieldData[] fields) {
      // already sorted:
      this.fields = fields;
    }

    @Override
    public Iterator<String> iterator() {
      return new Iterator<String>() {
        int upto = -1;

        @Override
        public boolean hasNext() {
          return upto+1 < fields.length;
        }

        @Override
        public String next() {
          upto++;
          return fields[upto].fieldInfo.name;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public Terms terms(String field) {
      // Slow linear search:
      for(FieldData fieldData : fields) {
        if (fieldData.fieldInfo.name.equals(field)) {
          return new DataTerms(fieldData);
        }
      }
      return null;
    }

    @Override
    public int size() {
      return fields.length;
    }
  }

  private static class DataTerms extends Terms {
    final FieldData fieldData;

    public DataTerms(FieldData fieldData) {
      this.fieldData = fieldData;
    }

    @Override
    public TermsEnum iterator(TermsEnum reuse) {
      return new DataTermsEnum(fieldData);
    }

    @Override
    public long size() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getSumTotalTermFreq() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getSumDocFreq() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getDocCount() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasOffsets() {
      return fieldData.fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
    }

    @Override
    public boolean hasPositions() {
      return fieldData.fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
    }

    @Override
    public boolean hasPayloads() {
      return fieldData.fieldInfo.hasPayloads();
    }
  }

  private static class DataTermsEnum extends TermsEnum {
    final FieldData fieldData;
    private int upto = -1;

    public DataTermsEnum(FieldData fieldData) {
      this.fieldData = fieldData;
    }

    @Override
    public BytesRef next() {
      upto++;
      if (upto == fieldData.terms.length) {
        return null;
      }

      return term();
    }

    @Override
    public BytesRef term() {
      return fieldData.terms[upto].text;
    }

    @Override
    public SeekStatus seekCeil(BytesRef text) {
      // Stupid linear impl:
      for(int i=0;i<fieldData.terms.length;i++) {
        int cmp = fieldData.terms[i].text.compareTo(text);
        if (cmp == 0) {
          upto = i;
          return SeekStatus.FOUND;
        } else if (cmp > 0) {
          upto = i;
          return SeekStatus.NOT_FOUND;
        }
      }

      return SeekStatus.END;
    }

    @Override
    public void seekExact(long ord) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long ord() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int docFreq() {
      throw new UnsupportedOperationException();
    }
  
    @Override
    public long totalTermFreq() {
      throw new UnsupportedOperationException();
    }

    @Override
    public DocsEnum docs(Bits liveDocs, DocsEnum reuse, int flags) {
      assert liveDocs == null;
      return new DataDocsAndPositionsEnum(fieldData.terms[upto]);
    }

    @Override
    public DocsAndPositionsEnum docsAndPositions(Bits liveDocs, DocsAndPositionsEnum reuse, int flags) {
      assert liveDocs == null;
      return new DataDocsAndPositionsEnum(fieldData.terms[upto]);
    }
  }

  private static class DataDocsAndPositionsEnum extends DocsAndPositionsEnum {
    final TermData termData;
    int docUpto = -1;
    int posUpto;

    public DataDocsAndPositionsEnum(TermData termData) {
      this.termData = termData;
    }

    @Override
    public long cost() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int nextDoc() {
      docUpto++;
      if (docUpto == termData.docs.length) {
        return NO_MORE_DOCS;
      }
      posUpto = -1;
      return docID();
    }

    @Override
    public int docID() {
      return termData.docs[docUpto];
    }

    @Override
    public int advance(int target) {
      // Slow linear impl:
      nextDoc();
      while (docID() < target) {
        nextDoc();
      }

      return docID();
    }

    @Override
    public int freq() {
      return termData.positions[docUpto].length;
    }

    @Override
    public int nextPosition() {
      posUpto++;
      return termData.positions[docUpto][posUpto].pos;
    }

    @Override
    public BytesRef getPayload() {
      return termData.positions[docUpto][posUpto].payload;
    }
    
    @Override
    public int startOffset() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int endOffset() {
      throw new UnsupportedOperationException();
    }
  }

   private void write(final FieldInfos fieldInfos, final Directory dir, final FieldData[] fields) throws Throwable {
 
     final Codec codec = Codec.getDefault();
     final SegmentInfo si = new SegmentInfo(dir, Constants.LUCENE_MAIN_VERSION, SEGMENT, 10000, false, codec, null, null);
     final SegmentWriteState state = new SegmentWriteState(InfoStream.getDefault(), dir, si, fieldInfos, null, newIOContext(random()));
 
    final FieldsConsumer consumer = codec.postingsFormat().fieldsConsumer(state);
     Arrays.sort(fields);
    for (final FieldData field : fields) {
      field.write(consumer);
    }
    consumer.close();
    codec.postingsFormat().fieldsConsumer(state).write(new DataFields(fields));
   }
   
   public void testDocsOnlyFreq() throws Exception {
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestConcurrentMergeScheduler.java b/lucene/core/src/test/org/apache/lucene/index/TestConcurrentMergeScheduler.java
index b24c53a83f0..eb77e4cd736 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestConcurrentMergeScheduler.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestConcurrentMergeScheduler.java
@@ -215,6 +215,7 @@ public class TestConcurrentMergeScheduler extends LuceneTestCase {
     IndexWriter writer = new IndexWriter(
         directory,
         newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random())).
            // Force excessive merging:
             setMaxBufferedDocs(2).
             setMergePolicy(newLogMergePolicy(100))
     );
@@ -249,7 +250,9 @@ public class TestConcurrentMergeScheduler extends LuceneTestCase {
           directory,
           newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random())).
               setOpenMode(OpenMode.APPEND).
              setMergePolicy(newLogMergePolicy(100))
              setMergePolicy(newLogMergePolicy(100)).
              // Force excessive merging:
              setMaxBufferedDocs(2)
       );
     }
     writer.close();
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestDirectoryReader.java b/lucene/core/src/test/org/apache/lucene/index/TestDirectoryReader.java
index 4f7ca8fbe6c..9aeb5364361 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestDirectoryReader.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestDirectoryReader.java
@@ -1094,7 +1094,6 @@ public void testFilesOpenClose() throws IOException {
     File tempDir = _TestUtil.getTempDir("testIndexExistsOnNonExistentDirectory");
     tempDir.delete();
     Directory dir = newFSDirectory(tempDir);
    System.out.println("dir=" + dir);
     assertFalse(DirectoryReader.indexExists(dir));
     dir.close();
   }
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java b/lucene/core/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
index a177e564b0b..7f458da0b95 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
@@ -29,6 +29,7 @@ import java.util.Random;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import org.apache.lucene.analysis.*;
import org.apache.lucene.codecs.PostingsFormat;
 import org.apache.lucene.document.BinaryDocValuesField;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
@@ -537,21 +538,15 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
     public void eval(MockDirectoryWrapper dir)  throws IOException {
       if (doFail) {
         StackTraceElement[] trace = new Exception().getStackTrace();
        boolean sawAppend = false;
         boolean sawFlush = false;
         for (int i = 0; i < trace.length; i++) {
          if (sawAppend && sawFlush) {
            break;
          }
          if (FreqProxTermsWriterPerField.class.getName().equals(trace[i].getClassName()) && "flush".equals(trace[i].getMethodName())) {
            sawAppend = true;
          }
           if ("flush".equals(trace[i].getMethodName())) {
             sawFlush = true;
            break;
           }
         }
 
        if (sawAppend && sawFlush && count++ >= 30) {
        if (sawFlush && count++ >= 30) {
           doFail = false;
           throw new IOException("now failing during flush");
         }
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestLongPostings.java b/lucene/core/src/test/org/apache/lucene/index/TestLongPostings.java
index 34f0a590fa1..38cef40276a 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestLongPostings.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestLongPostings.java
@@ -199,6 +199,9 @@ public class TestLongPostings extends LuceneTestCase {
           }
 
           if (random().nextInt(6) == 3) {
            if (VERBOSE) {
              System.out.println("    check positions");
            }
             final int freq = postings.freq();
             assertTrue(freq >=1 && freq <= 4);
             for(int pos=0;pos<freq;pos++) {
diff --git a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
index 3a0c6bbe62f..99e8e1b61fb 100644
-- a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
++ b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
@@ -803,11 +803,6 @@ public class MemoryIndex {
               return new MemoryTermsEnum(info);
             }
 
            @Override
            public Comparator<BytesRef> getComparator() {
              return BytesRef.getUTF8SortedAsUnicodeComparator();
            }

             @Override
             public long size() {
               return info.terms.size();
@@ -965,11 +960,6 @@ public class MemoryIndex {
         return ((MemoryDocsAndPositionsEnum) reuse).reset(liveDocs, info.sliceArray.start[ord], info.sliceArray.end[ord], info.sliceArray.freq[ord]);
       }
 
      @Override
      public Comparator<BytesRef> getComparator() {
        return BytesRef.getUTF8SortedAsUnicodeComparator();
      }

       @Override
       public void seekExact(BytesRef term, TermState state) throws IOException {
         assert state != null;
diff --git a/lucene/sandbox/src/java/org/apache/lucene/sandbox/queries/SlowCollatedTermRangeTermsEnum.java b/lucene/sandbox/src/java/org/apache/lucene/sandbox/queries/SlowCollatedTermRangeTermsEnum.java
index 91030c99569..fbaf5477a83 100644
-- a/lucene/sandbox/src/java/org/apache/lucene/sandbox/queries/SlowCollatedTermRangeTermsEnum.java
++ b/lucene/sandbox/src/java/org/apache/lucene/sandbox/queries/SlowCollatedTermRangeTermsEnum.java
@@ -17,7 +17,6 @@ package org.apache.lucene.sandbox.queries;
  * limitations under the License.
  */
 
import java.io.IOException;
 import java.text.Collator;
 
 import org.apache.lucene.index.TermsEnum;
@@ -28,7 +27,7 @@ import org.apache.lucene.util.BytesRef;
  * Subclass of FilteredTermEnum for enumerating all terms that match the
  * specified range parameters.
  * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * {@link BytesRef#compareTo}.  Each term in the enumeration is
  * greater than all that precede it.</p>
  * @deprecated Index collation keys with CollationKeyAnalyzer or ICUCollationKeyAnalyzer instead.
  *  This class will be removed in Lucene 5.0
diff --git a/lucene/sandbox/src/java/org/apache/lucene/sandbox/queries/SlowFuzzyTermsEnum.java b/lucene/sandbox/src/java/org/apache/lucene/sandbox/queries/SlowFuzzyTermsEnum.java
index f63c1a1503b..0bdc37deefc 100644
-- a/lucene/sandbox/src/java/org/apache/lucene/sandbox/queries/SlowFuzzyTermsEnum.java
++ b/lucene/sandbox/src/java/org/apache/lucene/sandbox/queries/SlowFuzzyTermsEnum.java
@@ -38,7 +38,7 @@ import org.apache.lucene.util.UnicodeUtil;
  * fuzzy terms enum method by calling FuzzyTermsEnum's getAutomatonEnum.
  * </p>
  * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * {@link BytesRef#compareTo}.  Each term in the enumeration is
  * greater than all that precede it.</p>
  * 
  * @deprecated Use {@link FuzzyTermsEnum} instead.
diff --git a/lucene/spatial/src/java/org/apache/lucene/spatial/prefix/AbstractVisitingPrefixTreeFilter.java b/lucene/spatial/src/java/org/apache/lucene/spatial/prefix/AbstractVisitingPrefixTreeFilter.java
index 444762e17a1..f950d737134 100644
-- a/lucene/spatial/src/java/org/apache/lucene/spatial/prefix/AbstractVisitingPrefixTreeFilter.java
++ b/lucene/spatial/src/java/org/apache/lucene/spatial/prefix/AbstractVisitingPrefixTreeFilter.java
@@ -17,7 +17,9 @@ package org.apache.lucene.spatial.prefix;
  * limitations under the License.
  */
 
import com.spatial4j.core.shape.Shape;
import java.io.IOException;
import java.util.Iterator;

 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.search.DocIdSet;
@@ -26,9 +28,7 @@ import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.StringHelper;

import java.io.IOException;
import java.util.Iterator;
import com.spatial4j.core.shape.Shape;
 
 /**
  * Traverses a {@link SpatialPrefixTree} indexed field, using the template &
@@ -176,7 +176,7 @@ public abstract class AbstractVisitingPrefixTreeFilter extends AbstractPrefixTre
         //Seek to curVNode's cell (or skip if termsEnum has moved beyond)
         curVNodeTerm.bytes = curVNode.cell.getTokenBytes();
         curVNodeTerm.length = curVNodeTerm.bytes.length;
        int compare = termsEnum.getComparator().compare(thisTerm, curVNodeTerm);
        int compare = thisTerm.compareTo(curVNodeTerm);
         if (compare > 0) {
           // leap frog (termsEnum is beyond where we would otherwise seek)
           assert ! context.reader().terms(fieldName).iterator(null).seekExact(curVNodeTerm) : "should be absent";
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/spell/HighFrequencyDictionary.java b/lucene/suggest/src/java/org/apache/lucene/search/spell/HighFrequencyDictionary.java
index c05f5831c9c..187e3271621 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/spell/HighFrequencyDictionary.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/spell/HighFrequencyDictionary.java
@@ -18,7 +18,6 @@
 package org.apache.lucene.search.spell;
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.TermsEnum;
@@ -99,14 +98,5 @@ public class HighFrequencyDictionary implements Dictionary {
       }
       return  null;
     }

    @Override
    public Comparator<BytesRef> getComparator() {
      if (termsEnum == null) {
        return null;
      } else {
        return termsEnum.getComparator();
      }
    }
   }
 }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/spell/PlainTextDictionary.java b/lucene/suggest/src/java/org/apache/lucene/search/spell/PlainTextDictionary.java
index 27ec4d214a9..7071ff7cb28 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/spell/PlainTextDictionary.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/spell/PlainTextDictionary.java
@@ -17,9 +17,11 @@ package org.apache.lucene.search.spell;
  * limitations under the License.
  */
 

import java.util.Comparator;
import java.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
 
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.BytesRefIterator;
@@ -96,11 +98,5 @@ public class PlainTextDictionary implements Dictionary {
       }
       return result;
     }
    
    @Override
    public Comparator<BytesRef> getComparator() {
      return null;
    }
   }

 }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/spell/TermFreqIterator.java b/lucene/suggest/src/java/org/apache/lucene/search/spell/TermFreqIterator.java
index 2487a0b5779..d7ce627b522 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/spell/TermFreqIterator.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/spell/TermFreqIterator.java
@@ -18,7 +18,6 @@ package org.apache.lucene.search.spell;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.BytesRefIterator;
@@ -55,10 +54,5 @@ public interface TermFreqIterator extends BytesRefIterator {
     public BytesRef next() throws IOException {
       return wrapped.next();
     }

    @Override
    public Comparator<BytesRef> getComparator() {
      return wrapped.getComparator();
    }
   }
 }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/BufferingTermFreqIteratorWrapper.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/BufferingTermFreqIteratorWrapper.java
index f4eae438778..6228667285d 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/BufferingTermFreqIteratorWrapper.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/BufferingTermFreqIteratorWrapper.java
@@ -18,7 +18,6 @@ package org.apache.lucene.search.suggest;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 import org.apache.lucene.search.spell.TermFreqIterator;
 import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.BytesRef;
@@ -37,11 +36,9 @@ public class BufferingTermFreqIteratorWrapper implements TermFreqIterator {
   /** buffered weights, parallel with {@link #entries} */
   protected long[] freqs = new long[1];
   private final BytesRef spare = new BytesRef();
  private final Comparator<BytesRef> comp;
   
   /** Creates a new iterator, buffering entries from the specified iterator */
   public BufferingTermFreqIteratorWrapper(TermFreqIterator source) throws IOException {
    this.comp = source.getComparator();
     BytesRef spare;
     int freqIndex = 0;
     while((spare = source.next()) != null) {
@@ -67,11 +64,4 @@ public class BufferingTermFreqIteratorWrapper implements TermFreqIterator {
     }
     return null;
   }

  @Override
  public Comparator<BytesRef> getComparator() {
    return comp;
  }

 
 }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/BytesRefArray.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/BytesRefArray.java
index 9fa96bac55a..e7a44fc37e0 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/BytesRefArray.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/BytesRefArray.java
@@ -187,11 +187,6 @@ public final class BytesRefArray {
         }
         return null;
       }
      
      @Override
      public Comparator<BytesRef> getComparator() {
        return comp;
      }
     };
   }
 }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/FileDictionary.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/FileDictionary.java
index 26d4e42b830..fa242ef89f7 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/FileDictionary.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/FileDictionary.java
@@ -19,7 +19,6 @@ package org.apache.lucene.search.suggest;
 
 
 import java.io.*;
import java.util.Comparator;
 
 import org.apache.lucene.search.spell.Dictionary;
 import org.apache.lucene.search.spell.TermFreqIterator;
@@ -99,11 +98,5 @@ public class FileDictionary implements Dictionary {
         return null;
       }
     }

    @Override
    public Comparator<BytesRef> getComparator() {
      return null;
    }
   }

 }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/SortedTermFreqIteratorWrapper.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/SortedTermFreqIteratorWrapper.java
index 409f4ff34c8..53c4212ac44 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/SortedTermFreqIteratorWrapper.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/SortedTermFreqIteratorWrapper.java
@@ -64,11 +64,6 @@ public class SortedTermFreqIteratorWrapper implements TermFreqIterator {
     this.reader = sort();
   }
   
  @Override
  public Comparator<BytesRef> getComparator() {
    return comparator;
  }
  
   @Override
   public BytesRef next() throws IOException {
     boolean success = false;
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/ExternalRefSorter.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/ExternalRefSorter.java
index 0c464898306..0a06b861e83 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/ExternalRefSorter.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/ExternalRefSorter.java
@@ -66,8 +66,7 @@ public class ExternalRefSorter implements BytesRefSorter, Closeable {
       input = null;
     }
     
    return new ByteSequenceIterator(new Sort.ByteSequencesReader(sorted),
        sort.getComparator());
    return new ByteSequenceIterator(new Sort.ByteSequencesReader(sorted));
   }
   
   private void closeWriter() throws IOException {
@@ -96,12 +95,9 @@ public class ExternalRefSorter implements BytesRefSorter, Closeable {
   class ByteSequenceIterator implements BytesRefIterator {
     private final ByteSequencesReader reader;
     private BytesRef scratch = new BytesRef();
    private final Comparator<BytesRef> comparator;
     
    public ByteSequenceIterator(ByteSequencesReader reader,
        Comparator<BytesRef> comparator) {
    public ByteSequenceIterator(ByteSequencesReader reader) {
       this.reader = reader;
      this.comparator = comparator;
     }
     
     @Override
@@ -128,11 +124,6 @@ public class ExternalRefSorter implements BytesRefSorter, Closeable {
         }
       }
     }
    
    @Override
    public Comparator<BytesRef> getComparator() {
      return comparator;
    }
   }
 
   @Override
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/jaspell/JaspellLookup.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/jaspell/JaspellLookup.java
index e44bbda9965..558e115440e 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/jaspell/JaspellLookup.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/jaspell/JaspellLookup.java
@@ -28,7 +28,6 @@ import java.util.List;
 import org.apache.lucene.search.spell.TermFreqIterator;
 import org.apache.lucene.search.spell.TermFreqPayloadIterator;
 import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.UnsortedTermFreqIteratorWrapper;
 import org.apache.lucene.search.suggest.jaspell.JaspellTernarySearchTrie.TSTNode;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.CharsRef;
@@ -57,11 +56,6 @@ public class JaspellLookup extends Lookup {
     if (tfit instanceof TermFreqPayloadIterator) {
       throw new IllegalArgumentException("this suggester doesn't support payloads");
     }
    if (tfit.getComparator() != null) {
      // make sure it's unsorted
      // WTF - this could result in yet another sorted iteration....
      tfit = new UnsortedTermFreqIteratorWrapper(tfit);
    }
     trie = new JaspellTernarySearchTrie();
     trie.setMatchAlmostDiff(editDistance);
     BytesRef spare;
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/tst/TSTLookup.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/tst/TSTLookup.java
index 98d1e5d3e42..852ebb56c9a 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/tst/TSTLookup.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/tst/TSTLookup.java
@@ -56,11 +56,9 @@ public class TSTLookup extends Lookup {
       throw new IllegalArgumentException("this suggester doesn't support payloads");
     }
     root = new TernaryTreeNode();
    // buffer first
    if (tfit.getComparator() != BytesRef.getUTF8SortedAsUTF16Comparator()) {
      // make sure it's sorted and the comparator uses UTF16 sort order
      tfit = new SortedTermFreqIteratorWrapper(tfit, BytesRef.getUTF8SortedAsUTF16Comparator());
    }

    // make sure it's sorted and the comparator uses UTF16 sort order
    tfit = new SortedTermFreqIteratorWrapper(tfit, BytesRef.getUTF8SortedAsUTF16Comparator());
 
     ArrayList<String> tokens = new ArrayList<String>();
     ArrayList<Number> vals = new ArrayList<Number>();
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/TermFreqArrayIterator.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/TermFreqArrayIterator.java
index 8c6862fecec..d77fa5cfca9 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/TermFreqArrayIterator.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/TermFreqArrayIterator.java
@@ -18,7 +18,6 @@ package org.apache.lucene.search.suggest;
  */
 
 import java.util.Arrays;
import java.util.Comparator;
 import java.util.Iterator;
 
 import org.apache.lucene.search.spell.TermFreqIterator;
@@ -58,9 +57,4 @@ public final class TermFreqArrayIterator implements TermFreqIterator {
     }
     return null;
   }

  @Override
  public Comparator<BytesRef> getComparator() {
    return null;
  }
 }
\ No newline at end of file
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/TermFreqPayloadArrayIterator.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/TermFreqPayloadArrayIterator.java
index 28cd0a4d5a6..5bfb073251b 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/TermFreqPayloadArrayIterator.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/TermFreqPayloadArrayIterator.java
@@ -18,7 +18,6 @@ package org.apache.lucene.search.suggest;
  */
 
 import java.util.Arrays;
import java.util.Comparator;
 import java.util.Iterator;
 
 import org.apache.lucene.search.spell.TermFreqIterator;
@@ -64,9 +63,4 @@ public final class TermFreqPayloadArrayIterator implements TermFreqPayloadIterat
   public BytesRef payload() {
     return current.payload;
   }

  @Override
  public Comparator<BytesRef> getComparator() {
    return null;
  }
 }
\ No newline at end of file
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/TestHighFrequencyDictionary.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/TestHighFrequencyDictionary.java
index b7ef1dfa6f3..576d2a55d55 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/TestHighFrequencyDictionary.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/TestHighFrequencyDictionary.java
@@ -36,7 +36,6 @@ public class TestHighFrequencyDictionary extends LuceneTestCase {
     IndexReader ir = DirectoryReader.open(dir);
     Dictionary dictionary = new HighFrequencyDictionary(ir, "bogus", 0.1f);
     BytesRefIterator tf = dictionary.getWordsIterator();
    assertNull(tf.getComparator());
     assertNull(tf.next());
     dir.close();
   }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/codecs/asserting/AssertingPostingsFormat.java b/lucene/test-framework/src/java/org/apache/lucene/codecs/asserting/AssertingPostingsFormat.java
index ea8240f3064..af227a9293d 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/codecs/asserting/AssertingPostingsFormat.java
++ b/lucene/test-framework/src/java/org/apache/lucene/codecs/asserting/AssertingPostingsFormat.java
@@ -25,15 +25,20 @@ import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.FieldsProducer;
 import org.apache.lucene.codecs.PostingsConsumer;
 import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.PushFieldsConsumer;
 import org.apache.lucene.codecs.TermStats;
 import org.apache.lucene.codecs.TermsConsumer;
 import org.apache.lucene.codecs.lucene41.Lucene41PostingsFormat;
 import org.apache.lucene.index.AssertingAtomicReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
 import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.Fields;
 import org.apache.lucene.index.SegmentReadState;
 import org.apache.lucene.index.SegmentWriteState;
 import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.OpenBitSet;
 
@@ -49,7 +54,12 @@ public final class AssertingPostingsFormat extends PostingsFormat {
   
   @Override
   public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
    return new AssertingFieldsConsumer(in.fieldsConsumer(state));
    FieldsConsumer fieldsConsumer = in.fieldsConsumer(state);
    if (fieldsConsumer instanceof PushFieldsConsumer) {
      return new AssertingPushFieldsConsumer(state, (PushFieldsConsumer) fieldsConsumer);
    } else {
      return new AssertingFieldsConsumer(state, fieldsConsumer);
    }
   }
 
   @Override
@@ -92,11 +102,12 @@ public final class AssertingPostingsFormat extends PostingsFormat {
       return in.ramBytesUsed();
     }
   }
  
  static class AssertingFieldsConsumer extends FieldsConsumer {
    private final FieldsConsumer in;

  static class AssertingPushFieldsConsumer extends PushFieldsConsumer {
    private final PushFieldsConsumer in;
     
    AssertingFieldsConsumer(FieldsConsumer in) {
    AssertingPushFieldsConsumer(SegmentWriteState writeState, PushFieldsConsumer in) {
      super(writeState);
       this.in = in;
     }
     
@@ -112,6 +123,113 @@ public final class AssertingPostingsFormat extends PostingsFormat {
       in.close();
     }
   }

  static class AssertingFieldsConsumer extends FieldsConsumer {
    private final FieldsConsumer in;
    private final SegmentWriteState writeState;

    AssertingFieldsConsumer(SegmentWriteState writeState, FieldsConsumer in) {
      this.writeState = writeState;
      this.in = in;
    }
    
    @Override
    public void write(Fields fields) throws IOException {
      in.write(fields);

      // TODO: more asserts?  can we somehow run a
      // "limited" CheckIndex here???  Or ... can we improve
      // AssertingFieldsProducer and us it also to wrap the
      // incoming Fields here?
 
      String lastField = null;
      TermsEnum termsEnum = null;

      for(String field : fields) {

        FieldInfo fieldInfo = writeState.fieldInfos.fieldInfo(field);
        assert fieldInfo != null;
        assert lastField == null || lastField.compareTo(field) < 0;
        lastField = field;

        Terms terms = fields.terms(field);
        assert terms != null;

        termsEnum = terms.iterator(termsEnum);
        BytesRef lastTerm = null;
        DocsEnum docsEnum = null;
        DocsAndPositionsEnum posEnum = null;

        boolean hasFreqs = fieldInfo.getIndexOptions().compareTo(FieldInfo.IndexOptions.DOCS_AND_FREQS) >= 0;
        boolean hasPositions = fieldInfo.getIndexOptions().compareTo(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
        boolean hasOffsets = fieldInfo.getIndexOptions().compareTo(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;

        assert hasPositions == terms.hasPositions();
        assert hasOffsets == terms.hasOffsets();

        while(true) {
          BytesRef term = termsEnum.next();
          if (term == null) {
            break;
          }
          assert lastTerm == null || lastTerm.compareTo(term) < 0;
          if (lastTerm == null) {
            lastTerm = BytesRef.deepCopyOf(term);
          } else {
            lastTerm.copyBytes(term);
          }

          if (hasPositions == false) {
            int flags = 0;
            if (hasFreqs) {
              flags = flags | DocsEnum.FLAG_FREQS;
            }
            docsEnum = termsEnum.docs(null, docsEnum, flags);
          } else {
            int flags = DocsAndPositionsEnum.FLAG_PAYLOADS;
            if (hasOffsets) {
              flags = flags | DocsAndPositionsEnum.FLAG_OFFSETS;
            }
            posEnum = termsEnum.docsAndPositions(null, posEnum, flags);
            docsEnum = posEnum;
          }

          int lastDocID = -1;

          while(true) {
            int docID = docsEnum.nextDoc();
            if (docID == DocsEnum.NO_MORE_DOCS) {
              break;
            }
            assert docID > lastDocID;
            lastDocID = docID;
            if (hasFreqs) {
              int freq = docsEnum.freq();
              assert freq > 0;

              if (hasPositions) {
                int lastPos = -1;
                int lastStartOffset = -1;
                for(int i=0;i<freq;i++) {
                  int pos = posEnum.nextPosition();
                  assert pos > lastPos;
                  lastPos = pos;

                  if (hasOffsets) {
                    int startOffset = posEnum.startOffset();
                    int endOffset = posEnum.endOffset();
                    assert endOffset > startOffset;
                    assert startOffset >= lastStartOffset;
                    lastStartOffset = startOffset;
                  }
                }
              }
            }
          }
        }
      }
    }
  }
   
   static enum TermsConsumerState { INITIAL, START, FINISHED };
   static class AssertingTermsConsumer extends TermsConsumer {
@@ -123,6 +241,7 @@ public final class AssertingPostingsFormat extends PostingsFormat {
     private long sumTotalTermFreq = 0;
     private long sumDocFreq = 0;
     private OpenBitSet visitedDocs = new OpenBitSet();
    private static final Comparator<BytesRef> termComp = BytesRef.getUTF8SortedAsUnicodeComparator();
     
     AssertingTermsConsumer(TermsConsumer in, FieldInfo fieldInfo) {
       this.in = in;
@@ -133,7 +252,7 @@ public final class AssertingPostingsFormat extends PostingsFormat {
     public PostingsConsumer startTerm(BytesRef text) throws IOException {
       assert state == TermsConsumerState.INITIAL || state == TermsConsumerState.START && lastPostingsConsumer.docFreq == 0;
       state = TermsConsumerState.START;
      assert lastTerm == null || in.getComparator().compare(text, lastTerm) > 0;
      assert lastTerm == null || termComp.compare(text, lastTerm) > 0;
       lastTerm = BytesRef.deepCopyOf(text);
       return lastPostingsConsumer = new AssertingPostingsConsumer(in.startTerm(text), fieldInfo, visitedDocs);
     }
@@ -171,11 +290,6 @@ public final class AssertingPostingsFormat extends PostingsFormat {
       }
       in.finish(sumTotalTermFreq, sumDocFreq, docCount);
     }

    @Override
    public Comparator<BytesRef> getComparator() throws IOException {
      return in.getComparator();
    }
   }
   
   static enum PostingsConsumerState { INITIAL, START };
diff --git a/lucene/test-framework/src/java/org/apache/lucene/codecs/asserting/AssertingTermVectorsFormat.java b/lucene/test-framework/src/java/org/apache/lucene/codecs/asserting/AssertingTermVectorsFormat.java
index b7bf21636e7..d6503f4d039 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/codecs/asserting/AssertingTermVectorsFormat.java
++ b/lucene/test-framework/src/java/org/apache/lucene/codecs/asserting/AssertingTermVectorsFormat.java
@@ -18,7 +18,6 @@ package org.apache.lucene.codecs.asserting;
  */
 
 import java.io.IOException;
import java.util.Comparator;
 
 import org.apache.lucene.codecs.TermVectorsFormat;
 import org.apache.lucene.codecs.TermVectorsReader;
@@ -180,11 +179,6 @@ public class AssertingTermVectorsFormat extends TermVectorsFormat {
       in.finish(fis, numDocs);
     }
 
    @Override
    public Comparator<BytesRef> getComparator() throws IOException {
      return in.getComparator();
    }

     @Override
     public void close() throws IOException {
       in.close();
diff --git a/lucene/test-framework/src/java/org/apache/lucene/codecs/ramonly/RAMOnlyPostingsFormat.java b/lucene/test-framework/src/java/org/apache/lucene/codecs/ramonly/RAMOnlyPostingsFormat.java
index 0dcc75e932a..da17ba272a9 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/codecs/ramonly/RAMOnlyPostingsFormat.java
++ b/lucene/test-framework/src/java/org/apache/lucene/codecs/ramonly/RAMOnlyPostingsFormat.java
@@ -20,7 +20,6 @@ package org.apache.lucene.codecs.ramonly;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
@@ -34,6 +33,7 @@ import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.FieldsProducer;
 import org.apache.lucene.codecs.PostingsConsumer;
 import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.PushFieldsConsumer;
 import org.apache.lucene.codecs.TermStats;
 import org.apache.lucene.codecs.TermsConsumer;
 import org.apache.lucene.index.DocsAndPositionsEnum;
@@ -60,41 +60,6 @@ import org.apache.lucene.util.RamUsageEstimator;
 
 public final class RAMOnlyPostingsFormat extends PostingsFormat {
 
  // For fun, test that we can override how terms are
  // sorted, and basic things still work -- this comparator
  // sorts in reversed unicode code point order:
  private static final Comparator<BytesRef> reverseUnicodeComparator = new Comparator<BytesRef>() {
      @Override
      public int compare(BytesRef t1, BytesRef t2) {
        byte[] b1 = t1.bytes;
        byte[] b2 = t2.bytes;
        int b1Stop;
        int b1Upto = t1.offset;
        int b2Upto = t2.offset;
        if (t1.length < t2.length) {
          b1Stop = t1.offset + t1.length;
        } else {
          b1Stop = t1.offset + t2.length;
        }
        while(b1Upto < b1Stop) {
          final int bb1 = b1[b1Upto++] & 0xff;
          final int bb2 = b2[b2Upto++] & 0xff;
          if (bb1 != bb2) {
            //System.out.println("cmp 1=" + t1 + " 2=" + t2 + " return " + (bb2-bb1));
            return bb2 - bb1;
          }
        }

        // One is prefix of another, or they are equal
        return t2.length-t1.length;
      }

      @Override
      public boolean equals(Object other) {
        return this == other;
      }
    };

   public RAMOnlyPostingsFormat() {
     super("RAMOnly");
   }
@@ -179,11 +144,6 @@ public final class RAMOnlyPostingsFormat extends PostingsFormat {
       return new RAMTermsEnum(RAMOnlyPostingsFormat.RAMField.this);
     }
 
    @Override
    public Comparator<BytesRef> getComparator() {
      return reverseUnicodeComparator;
    }

     @Override
     public boolean hasOffsets() {
       return info.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
@@ -243,12 +203,13 @@ public final class RAMOnlyPostingsFormat extends PostingsFormat {
   }
 
   // Classes for writing to the postings state
  private static class RAMFieldsConsumer extends FieldsConsumer {
  private static class RAMFieldsConsumer extends PushFieldsConsumer {
 
     private final RAMPostings postings;
     private final RAMTermsConsumer termsConsumer = new RAMTermsConsumer();
 
    public RAMFieldsConsumer(RAMPostings postings) {
    public RAMFieldsConsumer(SegmentWriteState writeState, RAMPostings postings) {
      super(writeState);
       this.postings = postings;
     }
 
@@ -286,12 +247,6 @@ public final class RAMOnlyPostingsFormat extends PostingsFormat {
       return postingsWriter;
     }
 
      
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public void finishTerm(BytesRef text, TermStats stats) {
       assert stats.docFreq > 0;
@@ -354,11 +309,6 @@ public final class RAMOnlyPostingsFormat extends PostingsFormat {
       this.ramField = field;
     }
       
    @Override
    public Comparator<BytesRef> getComparator() {
      return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

     @Override
     public BytesRef next() {
       if (it == null) {
@@ -586,7 +536,7 @@ public final class RAMOnlyPostingsFormat extends PostingsFormat {
     }
     
     final RAMPostings postings = new RAMPostings();
    final RAMFieldsConsumer consumer = new RAMFieldsConsumer(postings);
    final RAMFieldsConsumer consumer = new RAMFieldsConsumer(writeState, postings);
 
     synchronized(state) {
       state.put(id, postings);
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/BasePostingsFormatTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/index/BasePostingsFormatTestCase.java
index 70db465725e..aa8ddd1b4fb 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/BasePostingsFormatTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/BasePostingsFormatTestCase.java
@@ -23,6 +23,7 @@ import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.EnumSet;
import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
@@ -30,14 +31,17 @@ import java.util.Map;
 import java.util.NoSuchElementException;
 import java.util.Random;
 import java.util.Set;
import java.util.SortedMap;
 import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
 
import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.codecs.Codec;
 import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsConsumer;
import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.codecs.TermsConsumer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.lucene45.Lucene45Codec;
import org.apache.lucene.codecs.perfield.PerFieldPostingsFormat;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.FieldInfo.DocValuesType;
@@ -49,7 +53,9 @@ import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.Constants;
 import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.LineFileDocs;
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.RamUsageEstimator;
 import org.apache.lucene.util._TestUtil;
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
@@ -126,6 +132,7 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
     private final BytesRef payload;
     private final IndexOptions options;
     private final boolean doPositions;
    private final boolean allowPayloads;
 
     private int docID;
     private int freq;
@@ -138,11 +145,12 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
     private int posSpacing;
     private int posUpto;
 
    public SeedPostings(long seed, int minDocFreq, int maxDocFreq, Bits liveDocs, IndexOptions options) {
    public SeedPostings(long seed, int minDocFreq, int maxDocFreq, Bits liveDocs, IndexOptions options, boolean allowPayloads) {
       random = new Random(seed);
       docRandom = new Random(random.nextLong());
       docFreq = _TestUtil.nextInt(random, minDocFreq, maxDocFreq);
       this.liveDocs = liveDocs;
      this.allowPayloads = allowPayloads;
 
       // TODO: more realistic to inversely tie this to numDocs:
       maxDocSpacing = _TestUtil.nextInt(random, 1, 100);
@@ -249,6 +257,9 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
       } else {
         payload.length = 0;
       }
      if (!allowPayloads) {
        payload.length = 0;
      }
 
       startOffset = offset + random.nextInt(5);
       endOffset = startOffset + random.nextInt(10);
@@ -295,7 +306,7 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
   }
 
   // Holds all postings:
  private static Map<String,Map<BytesRef,Long>> fields;
  private static Map<String,SortedMap<BytesRef,Long>> fields;
 
   private static FieldInfos fieldInfos;
 
@@ -307,7 +318,7 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
   private static long totalPostings;
   private static long totalPayloadBytes;
 
  private static SeedPostings getSeedPostings(String term, long seed, boolean withLiveDocs, IndexOptions options) {
  private static SeedPostings getSeedPostings(String term, long seed, boolean withLiveDocs, IndexOptions options, boolean allowPayloads) {
     int minDocFreq, maxDocFreq;
     if (term.startsWith("big_")) {
       minDocFreq = RANDOM_MULTIPLIER * 50000;
@@ -323,14 +334,14 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
       maxDocFreq = 3;
     }
 
    return new SeedPostings(seed, minDocFreq, maxDocFreq, withLiveDocs ? globalLiveDocs : null, options);
    return new SeedPostings(seed, minDocFreq, maxDocFreq, withLiveDocs ? globalLiveDocs : null, options, allowPayloads);
   }
 
   @BeforeClass
   public static void createPostings() throws IOException {
     totalPostings = 0;
     totalPayloadBytes = 0;
    fields = new TreeMap<String,Map<BytesRef,Long>>();
    fields = new TreeMap<String,SortedMap<BytesRef,Long>>();
 
     final int numFields = _TestUtil.nextInt(random(), 1, 5);
     if (VERBOSE) {
@@ -351,7 +362,7 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
                                                 null, DocValuesType.NUMERIC, null);
       fieldUpto++;
 
      Map<BytesRef,Long> postings = new TreeMap<BytesRef,Long>();
      SortedMap<BytesRef,Long> postings = new TreeMap<BytesRef,Long>();
       fields.put(field, postings);
       Set<String> seenTerms = new HashSet<String>();
 
@@ -388,7 +399,7 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
 
         // NOTE: sort of silly: we enum all the docs just to
         // get the maxDoc
        DocsEnum docsEnum = getSeedPostings(term, termSeed, false, IndexOptions.DOCS_ONLY);
        DocsEnum docsEnum = getSeedPostings(term, termSeed, false, IndexOptions.DOCS_ONLY, true);
         int doc;
         int lastDoc = 0;
         while((doc = docsEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
@@ -412,7 +423,7 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
     }
 
     allTerms = new ArrayList<FieldAndTerm>();
    for(Map.Entry<String,Map<BytesRef,Long>> fieldEnt : fields.entrySet()) {
    for(Map.Entry<String,SortedMap<BytesRef,Long>> fieldEnt : fields.entrySet()) {
       String field = fieldEnt.getKey();
       for(Map.Entry<BytesRef,Long> termEnt : fieldEnt.getValue().entrySet()) {
         allTerms.add(new FieldAndTerm(field, termEnt.getKey()));
@@ -432,6 +443,206 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
     globalLiveDocs = null;
   }
 
  private static class SeedFields extends Fields {
    final Map<String,SortedMap<BytesRef,Long>> fields;
    final FieldInfos fieldInfos;
    final IndexOptions maxAllowed;
    final boolean allowPayloads;

    public SeedFields(Map<String,SortedMap<BytesRef,Long>> fields, FieldInfos fieldInfos, IndexOptions maxAllowed, boolean allowPayloads) {
      this.fields = fields;
      this.fieldInfos = fieldInfos;
      this.maxAllowed = maxAllowed;
      this.allowPayloads = allowPayloads;
    }

    @Override
    public Iterator<String> iterator() {
      return fields.keySet().iterator();
    }

    @Override
    public Terms terms(String field) {
      SortedMap<BytesRef,Long> terms = fields.get(field);
      if (terms == null) {
        return null;
      } else {
        return new SeedTerms(terms, fieldInfos.fieldInfo(field), maxAllowed, allowPayloads);
      }
    }

    @Override
    public int size() {
      return fields.size();
    }
  }

  private static class SeedTerms extends Terms {
    final SortedMap<BytesRef,Long> terms;
    final FieldInfo fieldInfo;
    final IndexOptions maxAllowed;
    final boolean allowPayloads;

    public SeedTerms(SortedMap<BytesRef,Long> terms, FieldInfo fieldInfo, IndexOptions maxAllowed, boolean allowPayloads) {
      this.terms = terms;
      this.fieldInfo = fieldInfo;
      this.maxAllowed = maxAllowed;
      this.allowPayloads = allowPayloads;
    }

    @Override
    public TermsEnum iterator(TermsEnum reuse) {
      SeedTermsEnum termsEnum;
      if (reuse != null && reuse instanceof SeedTermsEnum) {
        termsEnum = (SeedTermsEnum) reuse;
        if (termsEnum.terms != terms) {
          termsEnum = new SeedTermsEnum(terms, maxAllowed, allowPayloads);
        }
      } else {
        termsEnum = new SeedTermsEnum(terms, maxAllowed, allowPayloads);
      }
      termsEnum.reset();

      return termsEnum;
    }

    @Override
    public long size() {
      return terms.size();
    }

    @Override
    public long getSumTotalTermFreq() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getSumDocFreq() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getDocCount() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasOffsets() {
      return fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
    }
  
    @Override
    public boolean hasPositions() {
      return fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
    }
  
    @Override
    public boolean hasPayloads() {
      return fieldInfo.hasPayloads();
    }
  }

  private static class SeedTermsEnum extends TermsEnum {
    final SortedMap<BytesRef,Long> terms;
    final IndexOptions maxAllowed;
    final boolean allowPayloads;

    private Iterator<Map.Entry<BytesRef,Long>> iterator;

    private Map.Entry<BytesRef,Long> current;

    public SeedTermsEnum(SortedMap<BytesRef,Long> terms, IndexOptions maxAllowed, boolean allowPayloads) {
      this.terms = terms;
      this.maxAllowed = maxAllowed;
      this.allowPayloads = allowPayloads;
    }

    void reset() {
      iterator = terms.entrySet().iterator();
    }

    @Override
    public SeekStatus seekCeil(BytesRef text) {
      SortedMap<BytesRef,Long> tailMap = terms.tailMap(text);
      if (tailMap.isEmpty()) {
        return SeekStatus.END;
      } else {
        iterator = tailMap.entrySet().iterator();
        if (tailMap.firstKey().equals(text)) {
          return SeekStatus.FOUND;
        } else {
          return SeekStatus.NOT_FOUND;
        }
      }
    }

    @Override
    public BytesRef next() {
      if (iterator.hasNext()) {
        current = iterator.next();
        return term();
      } else {
        return null;
      }
    }

    @Override
    public void seekExact(long ord) {
      throw new UnsupportedOperationException();
    }

    @Override
    public BytesRef term() {
      return current.getKey();
    }

    @Override
    public long ord() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int docFreq() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long totalTermFreq() {
      throw new UnsupportedOperationException();
    }

    @Override
    public final DocsEnum docs(Bits liveDocs, DocsEnum reuse, int flags) throws IOException {
      if (liveDocs != null) {
        throw new IllegalArgumentException("liveDocs must be null");
      }
      if ((flags & DocsEnum.FLAG_FREQS) != 0 && maxAllowed.compareTo(IndexOptions.DOCS_AND_FREQS) < 0) {
        return null;
      }
      return getSeedPostings(current.getKey().utf8ToString(), current.getValue(), false, maxAllowed, allowPayloads);
    }

    @Override
    public final DocsAndPositionsEnum docsAndPositions(Bits liveDocs, DocsAndPositionsEnum reuse, int flags) throws IOException {
      if (liveDocs != null) {
        throw new IllegalArgumentException("liveDocs must be null");
      }
      if (maxAllowed.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) < 0) {
        System.out.println("no: max");
        return null;
      }
      if ((flags & DocsAndPositionsEnum.FLAG_OFFSETS) != 0 && maxAllowed.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) < 0) {
        System.out.println("no: offsets");
        return null;
      }
      if ((flags & DocsAndPositionsEnum.FLAG_PAYLOADS) != 0 && allowPayloads == false) {
        System.out.println("no: payloads");
        return null;
      }
      return getSeedPostings(current.getKey().utf8ToString(), current.getValue(), false, maxAllowed, allowPayloads);
    }
  }

   // TODO maybe instead of @BeforeClass just make a single test run: build postings & index & test it?
 
   private FieldInfos currentFieldInfos;
@@ -489,79 +700,10 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
     SegmentWriteState writeState = new SegmentWriteState(null, dir,
                                                          segmentInfo, newFieldInfos,
                                                          null, new IOContext(new FlushInfo(maxDoc, bytes)));
    FieldsConsumer fieldsConsumer = codec.postingsFormat().fieldsConsumer(writeState);

    for(Map.Entry<String,Map<BytesRef,Long>> fieldEnt : fields.entrySet()) {
      String field = fieldEnt.getKey();
      Map<BytesRef,Long> terms = fieldEnt.getValue();

      FieldInfo fieldInfo = newFieldInfos.fieldInfo(field);

      IndexOptions indexOptions = fieldInfo.getIndexOptions();

      if (VERBOSE) {
        System.out.println("field=" + field + " indexOtions=" + indexOptions);
      }

      boolean doFreq = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
      boolean doPos = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
      boolean doPayloads = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0 && allowPayloads;
      boolean doOffsets = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
      
      TermsConsumer termsConsumer = fieldsConsumer.addField(fieldInfo);
      long sumTotalTF = 0;
      long sumDF = 0;
      FixedBitSet seenDocs = new FixedBitSet(maxDoc);
      for(Map.Entry<BytesRef,Long> termEnt : terms.entrySet()) {
        BytesRef term = termEnt.getKey();
        SeedPostings postings = getSeedPostings(term.utf8ToString(), termEnt.getValue(), false, maxAllowed);
        if (VERBOSE) {
          System.out.println("  term=" + field + ":" + term.utf8ToString() + " docFreq=" + postings.docFreq + " seed=" + termEnt.getValue());
        }
        
        PostingsConsumer postingsConsumer = termsConsumer.startTerm(term);
        long totalTF = 0;
        int docID = 0;
        while((docID = postings.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          final int freq = postings.freq();
          if (VERBOSE) {
            System.out.println("    " + postings.upto + ": docID=" + docID + " freq=" + postings.freq);
          }
          postingsConsumer.startDoc(docID, doFreq ? postings.freq : -1);
          seenDocs.set(docID);
          if (doPos) {
            totalTF += postings.freq;
            for(int posUpto=0;posUpto<freq;posUpto++) {
              int pos = postings.nextPosition();
              BytesRef payload = postings.getPayload();
 
              if (VERBOSE) {
                if (doPayloads) {
                  System.out.println("      pos=" + pos + " payload=" + (payload == null ? "null" : payload.length + " bytes"));
                } else {
                  System.out.println("      pos=" + pos);
                }
              }
              postingsConsumer.addPosition(pos, doPayloads ? payload : null,
                                           doOffsets ? postings.startOffset() : -1,
                                           doOffsets ? postings.endOffset() : -1);
            }
          } else if (doFreq) {
            totalTF += freq;
          } else {
            totalTF++;
          }
          postingsConsumer.finishDoc();
        }
        termsConsumer.finishTerm(term, new TermStats(postings.docFreq, doFreq ? totalTF : -1));
        sumTotalTF += totalTF;
        sumDF += postings.docFreq;
      }

      termsConsumer.finish(doFreq ? sumTotalTF : -1, sumDF, seenDocs.cardinality());
    }
    Fields seedFields = new SeedFields(fields, newFieldInfos, maxAllowed, allowPayloads);
 
    fieldsConsumer.close();
    codec.postingsFormat().fieldsConsumer(writeState).write(seedFields);
 
     if (VERBOSE) {
       System.out.println("TEST: after indexing: files=");
@@ -625,7 +767,8 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
     SeedPostings expected = getSeedPostings(term.utf8ToString(), 
                                             fields.get(field).get(term),
                                             useLiveDocs,
                                            maxIndexOptions);
                                            maxIndexOptions,
                                            true);
     assertEquals(expected.docFreq, termsEnum.docFreq());
 
     boolean allowFreqs = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0 &&
@@ -1224,4 +1367,197 @@ public abstract class BasePostingsFormatTestCase extends LuceneTestCase {
     iw.close();
     dir.close();
   }

  private static class TermFreqs {
    long totalTermFreq;
    int docFreq;
  };

  // LUCENE-5123: make sure we can visit postings twice
  // during flush/merge
  public void testInvertedWrite() throws Exception {
    Directory dir = newDirectory();
    IndexWriterConfig iwc = newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));

    final Map<String,TermFreqs> termFreqs = new HashMap<String,TermFreqs>();
    final AtomicLong sumDocFreq = new AtomicLong();
    final AtomicLong sumTotalTermFreq = new AtomicLong();

    // TODO: would be better to use / delegate to the current
    // Codec returned by getCodec()

    iwc.setCodec(new Lucene45Codec() {
        @Override
        public PostingsFormat getPostingsFormatForField(String field) {

          PostingsFormat p = getCodec().postingsFormat();
          if (p instanceof PerFieldPostingsFormat) {
            p = ((PerFieldPostingsFormat) p).getPostingsFormatForField(field);
          }
          final PostingsFormat defaultPostingsFormat = p;

          if (field.equals("body")) {

            // A PF that counts up some stats and then in
            // the end we verify the stats match what the
            // final IndexReader says, just to exercise the
            // new freedom of iterating the postings more
            // than once at flush/merge:

            return new PostingsFormat(defaultPostingsFormat.getName()) {

              @Override
              public FieldsConsumer fieldsConsumer(final SegmentWriteState state) throws IOException {

                final FieldsConsumer fieldsConsumer = defaultPostingsFormat.fieldsConsumer(state);

                return new FieldsConsumer() {
                  @Override
                  public void write(Fields fields) throws IOException {
                    fieldsConsumer.write(fields);

                    boolean isMerge = state.context.context == IOContext.Context.MERGE;

                    boolean addOnSecondPass = random().nextBoolean();

                    //System.out.println("write isMerge=" + isMerge + " 2ndPass=" + addOnSecondPass);

                    // Gather our own stats:
                    Terms terms = fields.terms("body");
                    assert terms != null;

                    TermsEnum termsEnum = terms.iterator(null);
                    DocsEnum docs = null;
                    while(termsEnum.next() != null) {
                      BytesRef term = termsEnum.term();
                      if (random().nextBoolean()) {
                        docs = termsEnum.docs(null, docs, DocsEnum.FLAG_FREQS);
                      } else if (docs instanceof DocsAndPositionsEnum) {
                        docs = termsEnum.docsAndPositions(null, (DocsAndPositionsEnum) docs, 0);
                      } else {
                        docs = termsEnum.docsAndPositions(null, null, 0);
                      }
                      int docFreq = 0;
                      long totalTermFreq = 0;
                      while (docs.nextDoc() != DocsEnum.NO_MORE_DOCS) {
                        docFreq++;
                        totalTermFreq += docs.freq();
                        if (docs instanceof DocsAndPositionsEnum) {
                          DocsAndPositionsEnum posEnum = (DocsAndPositionsEnum) docs;
                          int limit = _TestUtil.nextInt(random(), 1, docs.freq());
                          for(int i=0;i<limit;i++) {
                            posEnum.nextPosition();
                          }
                        }
                      }

                      String termString = term.utf8ToString();

                      // During merge we should only see terms
                      // we had already seen during flush:
                      assertTrue(isMerge==false || termFreqs.containsKey(termString));

                      if (isMerge == false && addOnSecondPass == false) {
                        TermFreqs tf = termFreqs.get(termString);
                        if (tf == null) {
                          tf = new TermFreqs();
                          termFreqs.put(termString, tf);
                        }
                        tf.docFreq += docFreq;
                        tf.totalTermFreq += totalTermFreq;
                        sumDocFreq.addAndGet(docFreq);
                        sumTotalTermFreq.addAndGet(totalTermFreq);
                      } else if (termFreqs.containsKey(termString) == false) {
                        termFreqs.put(termString, new TermFreqs());
                      }
                    }

                    // Also test seeking the TermsEnum:
                    for(String term : termFreqs.keySet()) {
                      if (termsEnum.seekExact(new BytesRef(term))) {
                        if (random().nextBoolean()) {
                          docs = termsEnum.docs(null, docs, DocsEnum.FLAG_FREQS);
                        } else if (docs instanceof DocsAndPositionsEnum) {
                          docs = termsEnum.docsAndPositions(null, (DocsAndPositionsEnum) docs, 0);
                        } else {
                          docs = termsEnum.docsAndPositions(null, null, 0);
                        }

                        int docFreq = 0;
                        long totalTermFreq = 0;
                        while (docs.nextDoc() != DocsEnum.NO_MORE_DOCS) {
                          docFreq++;
                          totalTermFreq += docs.freq();
                          if (docs instanceof DocsAndPositionsEnum) {
                            DocsAndPositionsEnum posEnum = (DocsAndPositionsEnum) docs;
                            int limit = _TestUtil.nextInt(random(), 1, docs.freq());
                            for(int i=0;i<limit;i++) {
                              posEnum.nextPosition();
                            }
                          }
                        }

                        if (isMerge == false && addOnSecondPass) {
                          TermFreqs tf = termFreqs.get(term);
                          if (tf == null) {
                            tf = new TermFreqs();
                            termFreqs.put(term, tf);
                          }
                          tf.docFreq += docFreq;
                          tf.totalTermFreq += totalTermFreq;
                          sumDocFreq.addAndGet(docFreq);
                          sumTotalTermFreq.addAndGet(totalTermFreq);
                        }

                        //System.out.println("  term=" + term + " docFreq=" + docFreq + " ttDF=" + termToDocFreq.get(term));
                        assertTrue(docFreq <= termFreqs.get(term).docFreq);
                        assertTrue(totalTermFreq <= termFreqs.get(term).totalTermFreq);
                      }
                    }
                  }
                };
              }

              @Override
              public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
                return defaultPostingsFormat.fieldsProducer(state);
              }
            };
          } else {
            return defaultPostingsFormat;
          }
        }
      });

    RandomIndexWriter w = new RandomIndexWriter(random(), dir, iwc);

    LineFileDocs docs = new LineFileDocs(random());
    int bytesToIndex = atLeast(100) * 1024;
    int bytesIndexed = 0;
    while (bytesIndexed < bytesToIndex) {
      Document doc = docs.nextDoc();
      w.addDocument(doc);
      bytesIndexed += RamUsageEstimator.sizeOf(doc);
    }

    IndexReader r = w.getReader();
    w.close();

    Terms terms = MultiFields.getTerms(r, "body");
    assertEquals(sumDocFreq.get(), terms.getSumDocFreq());
    assertEquals(sumTotalTermFreq.get(), terms.getSumTotalTermFreq());

    TermsEnum termsEnum = terms.iterator(null);
    long termCount = 0;
    while(termsEnum.next() != null) {
      BytesRef term = termsEnum.term();
      termCount++;
      assertEquals(termFreqs.get(term.utf8ToString()).docFreq, termsEnum.docFreq());
      assertEquals(termFreqs.get(term.utf8ToString()).totalTermFreq, termsEnum.totalTermFreq());
    }
    assertEquals(termFreqs.size(), termCount);

    r.close();
    dir.close();
  }
 }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/BaseTermVectorsFormatTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/index/BaseTermVectorsFormatTestCase.java
index 150922b4ef6..d170a0e976c 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/BaseTermVectorsFormatTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/BaseTermVectorsFormatTestCase.java
@@ -406,7 +406,7 @@ public abstract class BaseTermVectorsFormatTestCase extends LuceneTestCase {
       uniqueTerms.add(new BytesRef(term));
     }
     final BytesRef[] sortedTerms = uniqueTerms.toArray(new BytesRef[0]);
    Arrays.sort(sortedTerms, terms.getComparator());
    Arrays.sort(sortedTerms);
     final TermsEnum termsEnum = terms.iterator(random().nextBoolean() ? null : this.termsEnum.get());
     this.termsEnum.set(termsEnum);
     for (int i = 0; i < sortedTerms.length; ++i) {
diff --git a/lucene/test-framework/src/java/org/apache/lucene/util/LuceneTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/util/LuceneTestCase.java
index e644a2e7d90..48546cb148c 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/util/LuceneTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/util/LuceneTestCase.java
@@ -1486,7 +1486,6 @@ public abstract class LuceneTestCase extends Assert {
    * checks collection-level statistics on Terms 
    */
   public void assertTermsStatisticsEquals(String info, Terms leftTerms, Terms rightTerms) throws IOException {
    assert leftTerms.getComparator() == rightTerms.getComparator();
     if (leftTerms.getDocCount() != -1 && rightTerms.getDocCount() != -1) {
       assertEquals(info, leftTerms.getDocCount(), rightTerms.getDocCount());
     }
- 
2.19.1.windows.1

