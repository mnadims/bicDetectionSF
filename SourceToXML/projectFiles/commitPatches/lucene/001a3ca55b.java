From 001a3ca55b30656e0e42f612d927a7923f5370e9 Mon Sep 17 00:00:00 2001
From: Mike McCandless <mikemccand@apache.org>
Date: Wed, 5 Oct 2016 14:18:55 -0400
Subject: [PATCH] LUCENE-7407: speed up iterating norms a bit by having default
 codec implement the iterator directly

--
 .../lucene/codecs/DocValuesConsumer.java      |  4 +-
 .../codecs/LegacyDocValuesIterables.java      | 12 ++-
 .../apache/lucene/codecs/NormsConsumer.java   |  2 +-
 .../lucene53/Lucene53NormsProducer.java       | 93 +++++++++++--------
 .../lucene/index/FilterNumericDocValues.java  |  2 +-
 .../index/LegacyNumericDocValuesWrapper.java  |  9 +-
 .../apache/lucene/index/MultiDocValues.java   |  4 +-
 .../apache/lucene/index/NumericDocValues.java |  4 +-
 .../lucene/search/SortedNumericSelector.java  |  4 +-
 .../join/ToParentBlockJoinSortField.java      |  4 +-
 .../lucene/index/AssertingLeafReader.java     |  2 +-
 .../apache/solr/request/IntervalFacets.java   |  4 +-
 .../apache/solr/request/NumericFacets.java    |  4 +-
 .../TestFieldCacheWithThreads.java            |  6 +-
 14 files changed, 85 insertions(+), 69 deletions(-)

diff --git a/lucene/core/src/java/org/apache/lucene/codecs/DocValuesConsumer.java b/lucene/core/src/java/org/apache/lucene/codecs/DocValuesConsumer.java
index 87c236cc244..35aa1002ddf 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/DocValuesConsumer.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/DocValuesConsumer.java
@@ -233,7 +233,7 @@ public abstract class DocValuesConsumer implements Closeable {
                           }
 
                           @Override
                          public long longValue() {
                          public long longValue() throws IOException {
                             return current.values.longValue();
                           }
                         };
@@ -495,7 +495,6 @@ public abstract class DocValuesConsumer implements Closeable {
     for (int sub=0;sub<numReaders;sub++) {
       SortedDocValues dv = dvs[sub];
       Bits liveDocs = mergeState.liveDocs[sub];
      int maxDoc = mergeState.maxDocs[sub];
       if (liveDocs == null) {
         liveTerms[sub] = dv.termsEnum();
         weights[sub] = dv.getValueCount();
@@ -668,7 +667,6 @@ public abstract class DocValuesConsumer implements Closeable {
     for (int sub = 0; sub < liveTerms.length; sub++) {
       SortedSetDocValues dv = toMerge.get(sub);
       Bits liveDocs = mergeState.liveDocs[sub];
      int maxDoc = mergeState.maxDocs[sub];
       if (liveDocs == null) {
         liveTerms[sub] = dv.termsEnum();
         weights[sub] = dv.getValueCount();
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/LegacyDocValuesIterables.java b/lucene/core/src/java/org/apache/lucene/codecs/LegacyDocValuesIterables.java
index 9d664c6322d..63f93dbef84 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/LegacyDocValuesIterables.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/LegacyDocValuesIterables.java
@@ -406,7 +406,11 @@ public class LegacyDocValuesIterables {
             }
             Number result;
             if (docIDUpto == values.docID()) {
              result = values.longValue();
              try {
                result = values.longValue();
              } catch (IOException ioe) {
                throw new RuntimeException(ioe);
              }
             } else {
               // Unlike NumericDocValues, norms should return for missing values:
               result = 0;
@@ -501,7 +505,11 @@ public class LegacyDocValuesIterables {
             }
             Number result;
             if (docIDUpto == values.docID()) {
              result = values.longValue();
              try {
                result = values.longValue();
              } catch (IOException ioe) {
                throw new RuntimeException(ioe);
              }
             } else {
               result = null;
             }
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/NormsConsumer.java b/lucene/core/src/java/org/apache/lucene/codecs/NormsConsumer.java
index ddd5c1b532b..3a6ce2274d5 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/NormsConsumer.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/NormsConsumer.java
@@ -163,7 +163,7 @@ public abstract class NormsConsumer implements Closeable {
                           }
 
                           @Override
                          public long longValue() {
                          public long longValue() throws IOException {
                             return current.values.longValue();
                           }
                         };
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/lucene53/Lucene53NormsProducer.java b/lucene/core/src/java/org/apache/lucene/codecs/lucene53/Lucene53NormsProducer.java
index 8be0f66c9ca..a97cb5a8ea3 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/lucene53/Lucene53NormsProducer.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/lucene53/Lucene53NormsProducer.java
@@ -27,14 +27,11 @@ import org.apache.lucene.index.CorruptIndexException;
 import org.apache.lucene.index.FieldInfo;
 import org.apache.lucene.index.FieldInfos;
 import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.LegacyNumericDocValues;
 import org.apache.lucene.index.NumericDocValues;
 import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.LegacyNumericDocValuesWrapper;
 import org.apache.lucene.store.ChecksumIndexInput;
 import org.apache.lucene.store.IndexInput;
 import org.apache.lucene.store.RandomAccessInput;
import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.IOUtils;
 
 import static org.apache.lucene.codecs.lucene53.Lucene53NormsFormat.VERSION_CURRENT;
@@ -117,13 +114,11 @@ class Lucene53NormsProducer extends NormsProducer {
   public NumericDocValues getNorms(FieldInfo field) throws IOException {
     final NormsEntry entry = norms.get(field.number);
 
    LegacyNumericDocValues norms;

     if (entry.bytesPerValue == 0) {
       final long value = entry.offset;
      norms = new LegacyNumericDocValues() {
      return new NormsIterator(maxDoc) {
           @Override
          public long get(int docID) {
          public long longValue() {
             return value;
           }
         };
@@ -133,63 +128,41 @@ class Lucene53NormsProducer extends NormsProducer {
         switch (entry.bytesPerValue) {
         case 1: 
           slice = data.randomAccessSlice(entry.offset, maxDoc);
          norms = new LegacyNumericDocValues() {
          return new NormsIterator(maxDoc) {
             @Override
            public long get(int docID) {
              try {
                return slice.readByte(docID);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            public long longValue() throws IOException {
              return slice.readByte(docID);
             }
           };
          break;
         case 2: 
           slice = data.randomAccessSlice(entry.offset, maxDoc * 2L);
          norms = new LegacyNumericDocValues() {
          return new NormsIterator(maxDoc) {
             @Override
            public long get(int docID) {
              try {
                return slice.readShort(((long)docID) << 1L);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            public long longValue() throws IOException {
              return slice.readShort(((long)docID) << 1L);
             }
           };
          break;
         case 4: 
           slice = data.randomAccessSlice(entry.offset, maxDoc * 4L);
          norms = new LegacyNumericDocValues() {
          return new NormsIterator(maxDoc) {
             @Override
            public long get(int docID) {
              try {
                return slice.readInt(((long)docID) << 2L);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            public long longValue() throws IOException {
              return slice.readInt(((long)docID) << 2L);
             }
           };
          break;
         case 8: 
           slice = data.randomAccessSlice(entry.offset, maxDoc * 8L);
          norms = new LegacyNumericDocValues() {
          return new NormsIterator(maxDoc) {
             @Override
            public long get(int docID) {
              try {
                return slice.readLong(((long)docID) << 3L);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            public long longValue() throws IOException {
              return slice.readLong(((long)docID) << 3L);
             }
           };
          break;
         default:
           throw new AssertionError();
         }
       }
     }

    return new LegacyNumericDocValuesWrapper(new Bits.MatchAllBits(maxDoc), norms);
   }
 
   @Override
@@ -216,4 +189,42 @@ class Lucene53NormsProducer extends NormsProducer {
   public String toString() {
     return getClass().getSimpleName() + "(fields=" + norms.size() + ")";
   }

  private static abstract class NormsIterator extends NumericDocValues {
    private final int maxDoc;
    protected int docID = -1;
  
    public NormsIterator(int maxDoc) {
      this.maxDoc = maxDoc;
    }

    @Override
    public int docID() {
      return docID;
    }

    @Override
    public int nextDoc() {
      docID++;
      if (docID == maxDoc) {
        docID = NO_MORE_DOCS;
      }
      return docID;
    }

    @Override
    public int advance(int target) {
      docID = target;
      if (docID >= maxDoc) {
        docID = NO_MORE_DOCS;
      }
      return docID;
    }

    @Override
    public long cost() {
      // TODO
      return 0;
    }
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/index/FilterNumericDocValues.java b/lucene/core/src/java/org/apache/lucene/index/FilterNumericDocValues.java
index b128d22c2f4..0058fa6dce3 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FilterNumericDocValues.java
++ b/lucene/core/src/java/org/apache/lucene/index/FilterNumericDocValues.java
@@ -53,7 +53,7 @@ public abstract class FilterNumericDocValues extends NumericDocValues {
   }
 
   @Override
  public long longValue() {
  public long longValue() throws IOException {
     return in.longValue();
   }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/index/LegacyNumericDocValuesWrapper.java b/lucene/core/src/java/org/apache/lucene/index/LegacyNumericDocValuesWrapper.java
index 64108e178c2..a72efe8848b 100644
-- a/lucene/core/src/java/org/apache/lucene/index/LegacyNumericDocValuesWrapper.java
++ b/lucene/core/src/java/org/apache/lucene/index/LegacyNumericDocValuesWrapper.java
@@ -38,11 +38,6 @@ public final class LegacyNumericDocValuesWrapper extends NumericDocValues {
     this.maxDoc = docsWithField.length();
   }
 
  /** Constructor used only for norms */
  public LegacyNumericDocValuesWrapper(int maxDoc, LegacyNumericDocValues values) {
    this(new Bits.MatchAllBits(maxDoc), values);
  }

   @Override
   public int docID() {
     return docID;
@@ -64,9 +59,7 @@ public final class LegacyNumericDocValuesWrapper extends NumericDocValues {
 
   @Override
   public int advance(int target) {
    if (target < docID) {
      throw new IllegalArgumentException("cannot advance backwards: docID=" + docID + " target=" + target);
    }
    assert target >= docID: "target=" + target + " docID=" + docID;
     if (target == NO_MORE_DOCS) {
       this.docID = NO_MORE_DOCS;
     } else {
diff --git a/lucene/core/src/java/org/apache/lucene/index/MultiDocValues.java b/lucene/core/src/java/org/apache/lucene/index/MultiDocValues.java
index 4054e90261b..6ed257efd6d 100644
-- a/lucene/core/src/java/org/apache/lucene/index/MultiDocValues.java
++ b/lucene/core/src/java/org/apache/lucene/index/MultiDocValues.java
@@ -139,7 +139,7 @@ public class MultiDocValues {
       }
 
       @Override
      public long longValue() {
      public long longValue() throws IOException {
         return currentValues.longValue();
       }
 
@@ -244,7 +244,7 @@ public class MultiDocValues {
       }
 
       @Override
      public long longValue() {
      public long longValue() throws IOException {
         return currentValues.longValue();
       }
 
diff --git a/lucene/core/src/java/org/apache/lucene/index/NumericDocValues.java b/lucene/core/src/java/org/apache/lucene/index/NumericDocValues.java
index d40f56ac8cf..5ae2e476fa5 100644
-- a/lucene/core/src/java/org/apache/lucene/index/NumericDocValues.java
++ b/lucene/core/src/java/org/apache/lucene/index/NumericDocValues.java
@@ -17,6 +17,8 @@
 
 package org.apache.lucene.index;
 
import java.io.IOException;

 import org.apache.lucene.search.DocIdSetIterator;
 
 /**
@@ -32,5 +34,5 @@ public abstract class NumericDocValues extends DocIdSetIterator {
    * Returns the numeric value for the current document ID.
    * @return numeric value
    */
  public abstract long longValue();
  public abstract long longValue() throws IOException;
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/SortedNumericSelector.java b/lucene/core/src/java/org/apache/lucene/search/SortedNumericSelector.java
index 56626e872d5..43e97e706d9 100644
-- a/lucene/core/src/java/org/apache/lucene/search/SortedNumericSelector.java
++ b/lucene/core/src/java/org/apache/lucene/search/SortedNumericSelector.java
@@ -83,14 +83,14 @@ public class SortedNumericSelector {
       case FLOAT:
         return new FilterNumericDocValues(view) {
           @Override
          public long longValue() {
          public long longValue() throws IOException {
             return NumericUtils.sortableFloatBits((int) in.longValue());
           }
         };
       case DOUBLE:
         return new FilterNumericDocValues(view) {
           @Override
          public long longValue() {
          public long longValue() throws IOException {
             return NumericUtils.sortableDoubleBits(in.longValue());
           }
         };
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinSortField.java b/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinSortField.java
index 07771501520..1b82c0c1cdc 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinSortField.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinSortField.java
@@ -175,7 +175,7 @@ public class ToParentBlockJoinSortField extends SortField {
         }
         return new FilterNumericDocValues(BlockJoinSelector.wrap(sortedNumeric, type, parents, children)) {
           @Override
          public long longValue() {
          public long longValue() throws IOException {
             // undo the numericutils sortability
             return NumericUtils.sortableFloatBits((int) super.longValue());
           }
@@ -199,7 +199,7 @@ public class ToParentBlockJoinSortField extends SortField {
         }
         return new FilterNumericDocValues(BlockJoinSelector.wrap(sortedNumeric, type, parents, children)) {
           @Override
          public long longValue() {
          public long longValue() throws IOException {
             // undo the numericutils sortability
             return NumericUtils.sortableDoubleBits(super.longValue());
           }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/AssertingLeafReader.java b/lucene/test-framework/src/java/org/apache/lucene/index/AssertingLeafReader.java
index cafd0915afd..3cc90230465 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/AssertingLeafReader.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/AssertingLeafReader.java
@@ -440,7 +440,7 @@ public class AssertingLeafReader extends FilterLeafReader {
     }
 
     @Override
    public long longValue() {
    public long longValue() throws IOException {
       assertThread("Numeric doc values", creationThread);
       assert in.docID() != -1;
       assert in.docID() != NO_MORE_DOCS;
diff --git a/solr/core/src/java/org/apache/solr/request/IntervalFacets.java b/solr/core/src/java/org/apache/solr/request/IntervalFacets.java
index 6187feb9cd8..2cf24e5cb6f 100644
-- a/solr/core/src/java/org/apache/solr/request/IntervalFacets.java
++ b/solr/core/src/java/org/apache/solr/request/IntervalFacets.java
@@ -201,7 +201,7 @@ public class IntervalFacets implements Iterable<FacetInterval> {
             // TODO: this bit flipping should probably be moved to tie-break in the PQ comparator
             longs = new FilterNumericDocValues(DocValues.getNumeric(ctx.reader(), fieldName)) {
               @Override
              public long longValue() {
              public long longValue() throws IOException {
                 long bits = super.longValue();
                 if (bits < 0) bits ^= 0x7fffffffffffffffL;
                 return bits;
@@ -212,7 +212,7 @@ public class IntervalFacets implements Iterable<FacetInterval> {
             // TODO: this bit flipping should probably be moved to tie-break in the PQ comparator
             longs = new FilterNumericDocValues(DocValues.getNumeric(ctx.reader(), fieldName)) {
               @Override
              public long longValue() {
              public long longValue() throws IOException {
                 long bits = super.longValue();
                 if (bits < 0) bits ^= 0x7fffffffffffffffL;
                 return bits;
diff --git a/solr/core/src/java/org/apache/solr/request/NumericFacets.java b/solr/core/src/java/org/apache/solr/request/NumericFacets.java
index 82c4c832630..5c5a7103619 100644
-- a/solr/core/src/java/org/apache/solr/request/NumericFacets.java
++ b/solr/core/src/java/org/apache/solr/request/NumericFacets.java
@@ -163,7 +163,7 @@ final class NumericFacets {
             // TODO: this bit flipping should probably be moved to tie-break in the PQ comparator
             longs = new FilterNumericDocValues(DocValues.getNumeric(ctx.reader(), fieldName)) {
               @Override
              public long longValue() {
              public long longValue() throws IOException {
                 long bits = super.longValue();
                 if (bits < 0) bits ^= 0x7fffffffffffffffL;
                 return bits;
@@ -174,7 +174,7 @@ final class NumericFacets {
             // TODO: this bit flipping should probably be moved to tie-break in the PQ comparator
             longs = new FilterNumericDocValues(DocValues.getNumeric(ctx.reader(), fieldName)) {
               @Override
              public long longValue() {
              public long longValue() throws IOException {
                 long bits = super.longValue();
                 if (bits < 0) bits ^= 0x7fffffffffffffffL;
                 return bits;
diff --git a/solr/core/src/test/org/apache/solr/uninverting/TestFieldCacheWithThreads.java b/solr/core/src/test/org/apache/solr/uninverting/TestFieldCacheWithThreads.java
index 2caa1a13f4d..810e4934ee3 100644
-- a/solr/core/src/test/org/apache/solr/uninverting/TestFieldCacheWithThreads.java
++ b/solr/core/src/test/org/apache/solr/uninverting/TestFieldCacheWithThreads.java
@@ -222,7 +222,11 @@ public class TestFieldCacheWithThreads extends LuceneTestCase {
               } catch (IOException ioe) {
                 throw new RuntimeException(ioe);
               }
              docIDToIDArray[i] = (int) docIDToID.longValue();
              try {
                docIDToIDArray[i] = (int) docIDToID.longValue();
              } catch (IOException ioe) {
                throw new RuntimeException(ioe);
              }
             }
             while(System.nanoTime() < END_TIME) {
               for(int iter=0;iter<100;iter++) {
- 
2.19.1.windows.1

