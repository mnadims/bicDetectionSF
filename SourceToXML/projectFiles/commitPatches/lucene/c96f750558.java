From c96f750558a05220afc008e2e1cfb3537627dadb Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Thu, 24 Apr 2014 13:57:47 +0000
Subject: [PATCH] LUCENE-5610: add Terms.getMin/Max

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1589729 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   9 +-
 .../bloom/BloomFilteringPostingsFormat.java   |  10 +
 .../lucene/codecs/BlockTreeTermsReader.java   |  47 ++++-
 .../lucene/codecs/BlockTreeTermsWriter.java   |  42 +++-
 .../org/apache/lucene/index/CheckIndex.java   |  16 ++
 .../lucene/index/FilteredTermsEnum.java       |  10 +-
 .../org/apache/lucene/index/MultiTerms.java   |  26 +++
 .../java/org/apache/lucene/index/Terms.java   |  74 +++++++
 .../org/apache/lucene/util/NumericUtils.java  |  91 +++++++-
 .../org/apache/lucene/index/TestTerms.java    | 196 ++++++++++++++++++
 .../lucene/index/AssertingAtomicReader.java   |  14 ++
 11 files changed, 518 insertions(+), 17 deletions(-)
 create mode 100644 lucene/core/src/test/org/apache/lucene/index/TestTerms.java

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 42a2c4fa59e..b64b6b6aaed 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -77,6 +77,13 @@ Other
 
 ======================= Lucene 4.9.0 =======================
 
New Features

* LUCENE-5610: Add Terms.getMin and Terms.getMax to get the lowest and
  highest terms, and NumericUtils.get{Min/Max}{Int/Long} to get the
  minimum numeric values from the provided Terms.  (Robert Muir, Mike
  McCandless)

 API Changes
 
 * LUCENE-5582: Deprecate IndexOutput.length (just use
@@ -93,7 +100,7 @@ Optimizations
   
 * LUCENE-5599: HttpReplicator did not properly delegate bulk read() to wrapped
   InputStream. (Christoph Kaser via Shai Erera)
  

 Bug fixes
 
 * LUCENE-5600: HttpClientBase did not properly consume a connection if a server
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/bloom/BloomFilteringPostingsFormat.java b/lucene/codecs/src/java/org/apache/lucene/codecs/bloom/BloomFilteringPostingsFormat.java
index b36dc6d8d38..785c780e6c7 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/bloom/BloomFilteringPostingsFormat.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/bloom/BloomFilteringPostingsFormat.java
@@ -293,6 +293,16 @@ public final class BloomFilteringPostingsFormat extends PostingsFormat {
       public boolean hasPayloads() {
         return delegateTerms.hasPayloads();
       }

      @Override
      public BytesRef getMin() throws IOException {
        return delegateTerms.getMin();
      }

      @Override
      public BytesRef getMax() throws IOException {
        return delegateTerms.getMax();
      }
     }
     
     final class BloomFilteredTermsEnum extends TermsEnum {
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsReader.java b/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsReader.java
index 624322ad47a..54e37e5a05b 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsReader.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsReader.java
@@ -163,6 +163,14 @@ public class BlockTreeTermsReader extends FieldsProducer {
         final long sumDocFreq = in.readVLong();
         final int docCount = in.readVInt();
         final int longsSize = version >= BlockTreeTermsWriter.VERSION_META_ARRAY ? in.readVInt() : 0;

        BytesRef minTerm, maxTerm;
        if (version >= BlockTreeTermsWriter.VERSION_MIN_MAX_TERMS) {
          minTerm = readBytesRef(in);
          maxTerm = readBytesRef(in);
        } else {
          minTerm = maxTerm = null;
        }
         if (docCount < 0 || docCount > info.getDocCount()) { // #docs with field must be <= #docs
           throw new CorruptIndexException("invalid docCount: " + docCount + " maxDoc: " + info.getDocCount() + " (resource=" + in + ")");
         }
@@ -173,7 +181,9 @@ public class BlockTreeTermsReader extends FieldsProducer {
           throw new CorruptIndexException("invalid sumTotalTermFreq: " + sumTotalTermFreq + " sumDocFreq: " + sumDocFreq + " (resource=" + in + ")");
         }
         final long indexStartFP = indexIn.readVLong();
        FieldReader previous = fields.put(fieldInfo.name, new FieldReader(fieldInfo, numTerms, rootCode, sumTotalTermFreq, sumDocFreq, docCount, indexStartFP, longsSize, indexIn));
        FieldReader previous = fields.put(fieldInfo.name,       
                                          new FieldReader(fieldInfo, numTerms, rootCode, sumTotalTermFreq, sumDocFreq, docCount,
                                                          indexStartFP, longsSize, indexIn, minTerm, maxTerm));
         if (previous != null) {
           throw new CorruptIndexException("duplicate field: " + fieldInfo.name + " (resource=" + in + ")");
         }
@@ -189,6 +199,14 @@ public class BlockTreeTermsReader extends FieldsProducer {
     }
   }
 
  private static BytesRef readBytesRef(IndexInput in) throws IOException {
    BytesRef bytes = new BytesRef();
    bytes.length = in.readVInt();
    bytes.bytes = new byte[bytes.length];
    in.readBytes(bytes.bytes, 0, bytes.length);
    return bytes;
  }

   /** Reads terms file header. */
   private int readHeader(IndexInput input) throws IOException {
     int version = CodecUtil.checkHeader(input, BlockTreeTermsWriter.TERMS_CODEC_NAME,
@@ -456,12 +474,15 @@ public class BlockTreeTermsReader extends FieldsProducer {
     final long indexStartFP;
     final long rootBlockFP;
     final BytesRef rootCode;
    final BytesRef minTerm;
    final BytesRef maxTerm;
     final int longsSize;
 
     private final FST<BytesRef> index;
     //private boolean DEBUG;
 
    FieldReader(FieldInfo fieldInfo, long numTerms, BytesRef rootCode, long sumTotalTermFreq, long sumDocFreq, int docCount, long indexStartFP, int longsSize, IndexInput indexIn) throws IOException {
    FieldReader(FieldInfo fieldInfo, long numTerms, BytesRef rootCode, long sumTotalTermFreq, long sumDocFreq, int docCount,
                long indexStartFP, int longsSize, IndexInput indexIn, BytesRef minTerm, BytesRef maxTerm) throws IOException {
       assert numTerms > 0;
       this.fieldInfo = fieldInfo;
       //DEBUG = BlockTreeTermsReader.DEBUG && fieldInfo.name.equals("id");
@@ -472,6 +493,8 @@ public class BlockTreeTermsReader extends FieldsProducer {
       this.indexStartFP = indexStartFP;
       this.rootCode = rootCode;
       this.longsSize = longsSize;
      this.minTerm = minTerm;
      this.maxTerm = maxTerm;
       // if (DEBUG) {
       //   System.out.println("BTTR: seg=" + segment + " field=" + fieldInfo.name + " rootBlockCode=" + rootCode + " divisor=" + indexDivisor);
       // }
@@ -498,6 +521,26 @@ public class BlockTreeTermsReader extends FieldsProducer {
       }
     }
 
    @Override
    public BytesRef getMin() throws IOException {
      if (minTerm == null) {
        // Older index that didn't store min/maxTerm
        return super.getMin();
      } else {
        return minTerm;
      }
    }

    @Override
    public BytesRef getMax() throws IOException {
      if (maxTerm == null) {
        // Older index that didn't store min/maxTerm
        return super.getMax();
      } else {
        return maxTerm;
      }
    }

     /** For debugging -- used by CheckIndex too*/
     // TODO: maybe push this into Terms?
     public Stats computeStats() throws IOException {
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsWriter.java b/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsWriter.java
index 6320ec9a979..f752f8a2fc1 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/BlockTreeTermsWriter.java
@@ -115,11 +115,12 @@ import org.apache.lucene.util.packed.PackedInts;
  *    <li>InnerNode --&gt; EntryCount, SuffixLength[,Sub?], Byte<sup>SuffixLength</sup>, StatsLength, &lt; TermStats ? &gt;<sup>EntryCount</sup>, MetaLength, &lt;<i>TermMetadata ? </i>&gt;<sup>EntryCount</sup></li>
  *    <li>TermStats --&gt; DocFreq, TotalTermFreq </li>
  *    <li>FieldSummary --&gt; NumFields, &lt;FieldNumber, NumTerms, RootCodeLength, Byte<sup>RootCodeLength</sup>,
 *                            SumTotalTermFreq?, SumDocFreq, DocCount&gt;<sup>NumFields</sup></li>
 *                            SumTotalTermFreq?, SumDocFreq, DocCount, LongsSize, MinTerm, MaxTerm&gt;<sup>NumFields</sup></li>
  *    <li>Header --&gt; {@link CodecUtil#writeHeader CodecHeader}</li>
  *    <li>DirOffset --&gt; {@link DataOutput#writeLong Uint64}</li>
 *    <li>MinTerm,MaxTerm --&gt; {@link DataOutput#writeVInt VInt} length followed by the byte[]</li>
  *    <li>EntryCount,SuffixLength,StatsLength,DocFreq,MetaLength,NumFields,
 *        FieldNumber,RootCodeLength,DocCount --&gt; {@link DataOutput#writeVInt VInt}</li>
 *        FieldNumber,RootCodeLength,DocCount,LongsSize --&gt; {@link DataOutput#writeVInt VInt}</li>
  *    <li>TotalTermFreq,NumTerms,SumTotalTermFreq,SumDocFreq --&gt; 
  *        {@link DataOutput#writeVLong VLong}</li>
  *    <li>Footer --&gt; {@link CodecUtil#writeFooter CodecFooter}</li>
@@ -138,6 +139,9 @@ import org.apache.lucene.util.packed.PackedInts;
  *    <li>SumDocFreq is the total number of postings, the number of term-document pairs across
  *        the entire field.</li>
  *    <li>DocCount is the number of documents that have at least one posting for this field.</li>
 *    <li>LongsSize records how many long values the postings writer/reader record per term
 *        (e.g., to hold freq/prox/doc file offsets).
 *    <li>MinTerm, MaxTerm are the lowest and highest term in this field.</li>
  *    <li>PostingsHeader and TermMetadata are plugged into by the specific postings implementation:
  *        these contain arbitrary per-file data (such as parameters or versioning information) 
  *        and per-term data (such as pointers to inverted files).</li>
@@ -216,8 +220,11 @@ public class BlockTreeTermsWriter extends FieldsConsumer implements Closeable {
   /** checksums */
   public static final int VERSION_CHECKSUM = 3;
 
  /** min/max term */
  public static final int VERSION_MIN_MAX_TERMS = 4;

   /** Current terms format. */
  public static final int VERSION_CURRENT = VERSION_CHECKSUM;
  public static final int VERSION_CURRENT = VERSION_MIN_MAX_TERMS;
 
   /** Extension of terms index file */
   static final String TERMS_INDEX_EXTENSION = "tip";
@@ -241,8 +248,11 @@ public class BlockTreeTermsWriter extends FieldsConsumer implements Closeable {
     public final long sumDocFreq;
     public final int docCount;
     private final int longsSize;
    public final BytesRef minTerm;
    public final BytesRef maxTerm;
 
    public FieldMetaData(FieldInfo fieldInfo, BytesRef rootCode, long numTerms, long indexStartFP, long sumTotalTermFreq, long sumDocFreq, int docCount, int longsSize) {
    public FieldMetaData(FieldInfo fieldInfo, BytesRef rootCode, long numTerms, long indexStartFP, long sumTotalTermFreq, long sumDocFreq, int docCount, int longsSize,
                         BytesRef minTerm, BytesRef maxTerm) {
       assert numTerms > 0;
       this.fieldInfo = fieldInfo;
       assert rootCode != null: "field=" + fieldInfo.name + " numTerms=" + numTerms;
@@ -253,6 +263,8 @@ public class BlockTreeTermsWriter extends FieldsConsumer implements Closeable {
       this.sumDocFreq = sumDocFreq;
       this.docCount = docCount;
       this.longsSize = longsSize;
      this.minTerm = minTerm;
      this.maxTerm = maxTerm;
     }
   }
 
@@ -354,16 +366,21 @@ public class BlockTreeTermsWriter extends FieldsConsumer implements Closeable {
         TermsEnum termsEnum = terms.iterator(null);
 
         TermsWriter termsWriter = new TermsWriter(fieldInfos.fieldInfo(field));

        BytesRef minTerm = null;
        BytesRef maxTerm = new BytesRef();
         while (true) {
           BytesRef term = termsEnum.next();
           if (term == null) {
             break;
           }
          if (minTerm == null) {
            minTerm = BytesRef.deepCopyOf(term);
          }
          maxTerm.copyBytes(term);
           termsWriter.write(term, termsEnum);
         }
 
        termsWriter.finish();
        termsWriter.finish(minTerm, minTerm == null ? null : maxTerm);
       }
       success = true;
     } finally {
@@ -1065,7 +1082,7 @@ public class BlockTreeTermsWriter extends FieldsConsumer implements Closeable {
     }
 
     // Finishes all terms in this field
    public void finish() throws IOException {
    public void finish(BytesRef minTerm, BytesRef maxTerm) throws IOException {
       if (numTerms > 0) {
         blockBuilder.finish();
 
@@ -1095,7 +1112,8 @@ public class BlockTreeTermsWriter extends FieldsConsumer implements Closeable {
                                      sumTotalTermFreq,
                                      sumDocFreq,
                                      docsSeen.cardinality(),
                                     longsSize));
                                     longsSize,
                                     minTerm, maxTerm));
       } else {
         assert sumTotalTermFreq == 0 || fieldInfo.getIndexOptions() == IndexOptions.DOCS_ONLY && sumTotalTermFreq == -1;
         assert sumDocFreq == 0;
@@ -1123,6 +1141,7 @@ public class BlockTreeTermsWriter extends FieldsConsumer implements Closeable {
       for(FieldMetaData field : fields) {
         //System.out.println("  field " + field.fieldInfo.name + " " + field.numTerms + " terms");
         out.writeVInt(field.fieldInfo.number);
        assert field.numTerms > 0;
         out.writeVLong(field.numTerms);
         out.writeVInt(field.rootCode.length);
         out.writeBytes(field.rootCode.bytes, field.rootCode.offset, field.rootCode.length);
@@ -1133,6 +1152,8 @@ public class BlockTreeTermsWriter extends FieldsConsumer implements Closeable {
         out.writeVInt(field.docCount);
         out.writeVInt(field.longsSize);
         indexOut.writeVLong(field.indexStartFP);
        writeBytesRef(out, field.minTerm);
        writeBytesRef(out, field.maxTerm);
       }
       writeTrailer(out, dirStart);
       CodecUtil.writeFooter(out);
@@ -1144,4 +1165,9 @@ public class BlockTreeTermsWriter extends FieldsConsumer implements Closeable {
       IOUtils.closeWhileHandlingException(ioe, out, indexOut, postingsWriter);
     }
   }

  private static void writeBytesRef(IndexOutput out, BytesRef bytes) throws IOException {
    out.writeVInt(bytes.length);
    out.writeBytes(bytes.bytes, bytes.offset, bytes.length);
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
index 7ae4aca5a4a..4a87726a6f1 100644
-- a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
++ b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
@@ -756,6 +756,14 @@ public class CheckIndex {
       final boolean hasPositions = terms.hasPositions();
       final boolean hasPayloads = terms.hasPayloads();
       final boolean hasOffsets = terms.hasOffsets();
      
      BytesRef bb = terms.getMin();
      assert bb.isValid();
      final BytesRef minTerm = bb == null ? null : BytesRef.deepCopyOf(bb);
      
      bb = terms.getMax();
      assert bb.isValid();
      final BytesRef maxTerm = bb == null ? null : BytesRef.deepCopyOf(bb);
 
       // term vectors cannot omit TF:
       final boolean expectedHasFreqs = (isVectors || fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0);
@@ -817,6 +825,14 @@ public class CheckIndex {
           lastTerm.copyBytes(term);
         }
         
        if (term.compareTo(minTerm) < 0) {
          throw new RuntimeException("invalid term: term=" + term + ", minTerm=" + minTerm);
        }
        
        if (term.compareTo(maxTerm) > 0) {
          throw new RuntimeException("invalid term: term=" + term + ", maxTerm=" + maxTerm);
        }
        
         final int docFreq = termsEnum.docFreq();
         if (docFreq <= 0) {
           throw new RuntimeException("docfreq: " + docFreq + " is out of bounds");
diff --git a/lucene/core/src/java/org/apache/lucene/index/FilteredTermsEnum.java b/lucene/core/src/java/org/apache/lucene/index/FilteredTermsEnum.java
index 3213a22db46..b6bfcc41532 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FilteredTermsEnum.java
++ b/lucene/core/src/java/org/apache/lucene/index/FilteredTermsEnum.java
@@ -36,11 +36,14 @@ import org.apache.lucene.util.Bits;
  */
 public abstract class FilteredTermsEnum extends TermsEnum {
 
  private BytesRef initialSeekTerm = null;
  private BytesRef initialSeekTerm;
   private boolean doSeek;
  private BytesRef actualTerm = null;
 
  private final TermsEnum tenum;
  /** Which term the enum is currently positioned to. */
  protected BytesRef actualTerm;

  /** The delegate {@link TermsEnum}. */
  protected final TermsEnum tenum;
 
   /** Return value, if term should be accepted or the iteration should
    * {@code END}. The {@code *_SEEK} values denote, that after handling the current term
@@ -246,6 +249,7 @@ public abstract class FilteredTermsEnum extends TermsEnum {
         case END:
           // we are supposed to end the enum
           return null;
        // NO: we just fall through and iterate again
       }
     }
   }
diff --git a/lucene/core/src/java/org/apache/lucene/index/MultiTerms.java b/lucene/core/src/java/org/apache/lucene/index/MultiTerms.java
index 9ad1a1a61b2..85ef653be19 100644
-- a/lucene/core/src/java/org/apache/lucene/index/MultiTerms.java
++ b/lucene/core/src/java/org/apache/lucene/index/MultiTerms.java
@@ -84,6 +84,32 @@ public final class MultiTerms extends Terms {
       return TermsEnum.EMPTY;
     }
   }
  
  @Override
  public BytesRef getMin() throws IOException {
    BytesRef minTerm = null;
    for(Terms terms : subs) {
      BytesRef term = terms.getMin();
      if (minTerm == null || term.compareTo(minTerm) < 0) {
        minTerm = term;
      }
    }

    return minTerm;
  }

  @Override
  public BytesRef getMax() throws IOException {
    BytesRef maxTerm = null;
    for(Terms terms : subs) {
      BytesRef term = terms.getMax();
      if (maxTerm == null || term.compareTo(maxTerm) > 0) {
        maxTerm = term;
      }
    }

    return maxTerm;
  }
 
   @Override
   public TermsEnum iterator(TermsEnum reuse) throws IOException {
diff --git a/lucene/core/src/java/org/apache/lucene/index/Terms.java b/lucene/core/src/java/org/apache/lucene/index/Terms.java
index c0aedfcaabc..533d51e8634 100644
-- a/lucene/core/src/java/org/apache/lucene/index/Terms.java
++ b/lucene/core/src/java/org/apache/lucene/index/Terms.java
@@ -117,4 +117,78 @@ public abstract class Terms {
 
   /** Zero-length array of {@link Terms}. */
   public final static Terms[] EMPTY_ARRAY = new Terms[0];
  
  /** Returns the smallest term (in lexicographic order) in the field. 
   *  Note that, just like other term measures, this measure does not 
   *  take deleted documents into account. */
  public BytesRef getMin() throws IOException {
    return iterator(null).next();
  }

  /** Returns the largest term (in lexicographic order) in the field. 
   *  Note that, just like other term measures, this measure does not 
   *  take deleted documents into account. */
  @SuppressWarnings("fallthrough")
  public BytesRef getMax() throws IOException {
    long size = size();
    
    if (size == 0) {
      // empty: only possible from a FilteredTermsEnum...
      return null;
    } else if (size >= 0) {
      // try to seek-by-ord
      try {
        TermsEnum iterator = iterator(null);
        iterator.seekExact(size - 1);
        return iterator.term();
      } catch (UnsupportedOperationException e) {
        // ok
      }
    }
    
    // otherwise: binary search
    TermsEnum iterator = iterator(null);
    BytesRef v = iterator.next();
    if (v == null) {
      // empty: only possible from a FilteredTermsEnum...
      return v;
    }

    BytesRef scratch = new BytesRef(1);

    scratch.length = 1;

    // Iterates over digits:
    while (true) {

      int low = 0;
      int high = 256;

      // Binary search current digit to find the highest
      // digit before END:
      while (low != high) {
        int mid = (low+high) >>> 1;
        scratch.bytes[scratch.length-1] = (byte) mid;
        if (iterator.seekCeil(scratch) == TermsEnum.SeekStatus.END) {
          // Scratch was too high
          if (mid == 0) {
            scratch.length--;
            return scratch;
          }
          high = mid;
        } else {
          // Scratch was too low; there is at least one term
          // still after it:
          if (low == mid) {
            break;
          }
          low = mid;
        }
      }

      // Recurse to next digit:
      scratch.length++;
      scratch.grow(scratch.length);
    }
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/util/NumericUtils.java b/lucene/core/src/java/org/apache/lucene/util/NumericUtils.java
index 12dcf18b6b3..98bb667d441 100644
-- a/lucene/core/src/java/org/apache/lucene/util/NumericUtils.java
++ b/lucene/core/src/java/org/apache/lucene/util/NumericUtils.java
@@ -17,12 +17,16 @@ package org.apache.lucene.util;
  * limitations under the License.
  */
 
import java.io.IOException;

 import org.apache.lucene.analysis.NumericTokenStream;
 import org.apache.lucene.document.DoubleField; // javadocs
 import org.apache.lucene.document.FloatField; // javadocs
 import org.apache.lucene.document.IntField; // javadocs
 import org.apache.lucene.document.LongField; // javadocs
import org.apache.lucene.index.FilterAtomicReader;
 import org.apache.lucene.index.FilteredTermsEnum;
import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.search.NumericRangeFilter;
 import org.apache.lucene.search.NumericRangeQuery; // for javadocs
@@ -464,14 +468,15 @@ public final class NumericUtils {
    *         terms with a shift value of <tt>0</tt>.
    */
   public static TermsEnum filterPrefixCodedLongs(TermsEnum termsEnum) {
    return new FilteredTermsEnum(termsEnum, false) {
    return new SeekingNumericFilteredTermsEnum(termsEnum) {

       @Override
       protected AcceptStatus accept(BytesRef term) {
         return NumericUtils.getPrefixCodedLongShift(term) == 0 ? AcceptStatus.YES : AcceptStatus.END;
       }
     };
   }
  

   /**
    * Filters the given {@link TermsEnum} by accepting only prefix coded 32 bit
    * terms with a shift value of <tt>0</tt>.
@@ -482,7 +487,7 @@ public final class NumericUtils {
    *         terms with a shift value of <tt>0</tt>.
    */
   public static TermsEnum filterPrefixCodedInts(TermsEnum termsEnum) {
    return new FilteredTermsEnum(termsEnum, false) {
    return new SeekingNumericFilteredTermsEnum(termsEnum) {
       
       @Override
       protected AcceptStatus accept(BytesRef term) {
@@ -490,5 +495,85 @@ public final class NumericUtils {
       }
     };
   }

  /** Just like FilteredTermsEnum, except it adds a limited
   *  seekCeil implementation that only works with {@link
   *  #filterPrefixCodedInts} and {@link
   *  #filterPrefixCodedLongs}. */
  private static abstract class SeekingNumericFilteredTermsEnum extends FilteredTermsEnum {
    public SeekingNumericFilteredTermsEnum(final TermsEnum tenum) {
      super(tenum, false);
    }

    @Override
    @SuppressWarnings("fallthrough")
    public SeekStatus seekCeil(BytesRef term) throws IOException {

      // NOTE: This is not general!!  It only handles YES
      // and END, because that's all we need for the numeric
      // case here

      SeekStatus status = tenum.seekCeil(term);
      if (status == SeekStatus.END) {
        return SeekStatus.END;
      }

      actualTerm = tenum.term();

      if (accept(actualTerm) == AcceptStatus.YES) {
        return status;
      } else {
        return SeekStatus.END;
      }
    }
  }

  private static Terms intTerms(Terms terms) {
    return new FilterAtomicReader.FilterTerms(terms) {
        @Override
        public TermsEnum iterator(TermsEnum reuse) throws IOException {
          return filterPrefixCodedInts(in.iterator(reuse));
        }
      };
  }

  private static Terms longTerms(Terms terms) {
    return new FilterAtomicReader.FilterTerms(terms) {
        @Override
        public TermsEnum iterator(TermsEnum reuse) throws IOException {
          return filterPrefixCodedLongs(in.iterator(reuse));
        }
      };
  }
    
  /** Returns the minimum int value indexed into this
   *  numeric field. */
  public static int getMinInt(Terms terms) throws IOException {
    // All shift=0 terms are sorted first, so we don't need
    // to filter the incoming terms; we can just get the
    // min: 
    return NumericUtils.prefixCodedToInt(terms.getMin());
  }

  /** Returns the maximum int value indexed into this
   *  numeric field. */
  public static int getMaxInt(Terms terms) throws IOException {
    return NumericUtils.prefixCodedToInt(intTerms(terms).getMax());
  }

  /** Returns the minimum long value indexed into this
   *  numeric field. */
  public static long getMinLong(Terms terms) throws IOException {
    // All shift=0 terms are sorted first, so we don't need
    // to filter the incoming terms; we can just get the
    // min: 
    return NumericUtils.prefixCodedToLong(terms.getMin());
  }

  /** Returns the maximum long value indexed into this
   *  numeric field. */
  public static long getMaxLong(Terms terms) throws IOException {
    return NumericUtils.prefixCodedToLong(longTerms(terms).getMax());
  }
   
 }
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestTerms.java b/lucene/core/src/test/org/apache/lucene/index/TestTerms.java
new file mode 100644
index 00000000000..ac39b1a0ec7
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/index/TestTerms.java
@@ -0,0 +1,196 @@
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

import java.util.*;

import org.apache.lucene.analysis.CannedBinaryTokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.TestUtil;

public class TestTerms extends LuceneTestCase {

  public void testTermMinMaxBasic() throws Exception {
    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(random(), dir);
    Document doc = new Document();
    doc.add(newTextField("field", "a b c cc ddd", Field.Store.NO));
    w.addDocument(doc);
    IndexReader r = w.getReader();
    Terms terms = MultiFields.getTerms(r, "field");
    assertEquals(new BytesRef("a"), terms.getMin());
    assertEquals(new BytesRef("ddd"), terms.getMax());
    r.close();
    w.close();
    dir.close();
  }

  public void testTermMinMaxRandom() throws Exception {
    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(random(), dir);
    int numDocs = atLeast(100);
    BytesRef minTerm = null;
    BytesRef maxTerm = null;
    for(int i=0;i<numDocs;i++ ){
      Document doc = new Document();
      Field field = new TextField("field", "", Field.Store.NO);
      doc.add(field);
      //System.out.println("  doc " + i);
      CannedBinaryTokenStream.BinaryToken[] tokens = new CannedBinaryTokenStream.BinaryToken[atLeast(10)];
      for(int j=0;j<tokens.length;j++) {
        byte[] bytes = new byte[TestUtil.nextInt(random(), 1, 20)];
        random().nextBytes(bytes);
        BytesRef tokenBytes = new BytesRef(bytes);
        //System.out.println("    token " + tokenBytes);
        if (minTerm == null || tokenBytes.compareTo(minTerm) < 0) {
          //System.out.println("      ** new min");
          minTerm = tokenBytes;
        }
        if (maxTerm == null || tokenBytes.compareTo(maxTerm) > 0) {
          //System.out.println("      ** new max");
          maxTerm = tokenBytes;
        }
        tokens[j] = new CannedBinaryTokenStream.BinaryToken(tokenBytes);
      }
      field.setTokenStream(new CannedBinaryTokenStream(tokens));
      w.addDocument(doc);
    }

    IndexReader r = w.getReader();
    Terms terms = MultiFields.getTerms(r, "field");
    assertEquals(minTerm, terms.getMin());
    assertEquals(maxTerm, terms.getMax());
    
    r.close();
    w.close();
    dir.close();
  }

  public void testIntFieldMinMax() throws Exception {
    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(random(), dir);
    int numDocs = atLeast(100);
    int minValue = Integer.MAX_VALUE;
    int maxValue = Integer.MIN_VALUE;
    for(int i=0;i<numDocs;i++ ){
      Document doc = new Document();
      int num = random().nextInt();
      minValue = Math.min(num, minValue);
      maxValue = Math.max(num, maxValue);
      doc.add(new IntField("field", num, Field.Store.NO));
      w.addDocument(doc);
    }
    
    IndexReader r = w.getReader();
    Terms terms = MultiFields.getTerms(r, "field");
    assertEquals(minValue, NumericUtils.getMinInt(terms));
    assertEquals(maxValue, NumericUtils.getMaxInt(terms));

    r.close();
    w.close();
    dir.close();
  }

  public void testLongFieldMinMax() throws Exception {
    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(random(), dir);
    int numDocs = atLeast(100);
    long minValue = Long.MAX_VALUE;
    long maxValue = Long.MIN_VALUE;
    for(int i=0;i<numDocs;i++ ){
      Document doc = new Document();
      long num = random().nextLong();
      minValue = Math.min(num, minValue);
      maxValue = Math.max(num, maxValue);
      doc.add(new LongField("field", num, Field.Store.NO));
      w.addDocument(doc);
    }
    
    IndexReader r = w.getReader();

    Terms terms = MultiFields.getTerms(r, "field");
    assertEquals(minValue, NumericUtils.getMinLong(terms));
    assertEquals(maxValue, NumericUtils.getMaxLong(terms));

    r.close();
    w.close();
    dir.close();
  }

  public void testFloatFieldMinMax() throws Exception {
    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(random(), dir);
    int numDocs = atLeast(100);
    float minValue = Float.POSITIVE_INFINITY;
    float maxValue = Float.NEGATIVE_INFINITY;
    for(int i=0;i<numDocs;i++ ){
      Document doc = new Document();
      float num = random().nextFloat();
      minValue = Math.min(num, minValue);
      maxValue = Math.max(num, maxValue);
      doc.add(new FloatField("field", num, Field.Store.NO));
      w.addDocument(doc);
    }
    
    IndexReader r = w.getReader();
    Terms terms = MultiFields.getTerms(r, "field");
    assertEquals(minValue, NumericUtils.sortableIntToFloat(NumericUtils.getMinInt(terms)), 0.0f);
    assertEquals(maxValue, NumericUtils.sortableIntToFloat(NumericUtils.getMaxInt(terms)), 0.0f);

    r.close();
    w.close();
    dir.close();
  }

  public void testDoubleFieldMinMax() throws Exception {
    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(random(), dir);
    int numDocs = atLeast(100);
    double minValue = Double.POSITIVE_INFINITY;
    double maxValue = Double.NEGATIVE_INFINITY;
    for(int i=0;i<numDocs;i++ ){
      Document doc = new Document();
      double num = random().nextDouble();
      minValue = Math.min(num, minValue);
      maxValue = Math.max(num, maxValue);
      doc.add(new DoubleField("field", num, Field.Store.NO));
      w.addDocument(doc);
    }
    
    IndexReader r = w.getReader();

    Terms terms = MultiFields.getTerms(r, "field");

    assertEquals(minValue, NumericUtils.sortableLongToDouble(NumericUtils.getMinLong(terms)), 0.0);
    assertEquals(maxValue, NumericUtils.sortableLongToDouble(NumericUtils.getMaxLong(terms)), 0.0);

    r.close();
    w.close();
    dir.close();
  }
}
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/AssertingAtomicReader.java b/lucene/test-framework/src/java/org/apache/lucene/index/AssertingAtomicReader.java
index 086cb21fb89..b6d7c2ad1c2 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/AssertingAtomicReader.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/AssertingAtomicReader.java
@@ -90,6 +90,20 @@ public class AssertingAtomicReader extends FilterAtomicReader {
       return new AssertingTermsEnum(termsEnum);
     }
 
    @Override
    public BytesRef getMin() throws IOException {
      BytesRef v = in.getMin();
      assert v == null || v.isValid();
      return v;
    }

    @Override
    public BytesRef getMax() throws IOException {
      BytesRef v = in.getMax();
      assert v == null || v.isValid();
      return v;
    }

     @Override
     public TermsEnum iterator(TermsEnum reuse) throws IOException {
       // TODO: should we give this thing a random to be super-evil,
- 
2.19.1.windows.1

