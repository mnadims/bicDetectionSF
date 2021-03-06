From 0810107d1f75bf1bc6760853347da3981a566e41 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Sat, 2 Apr 2011 15:32:31 +0000
Subject: [PATCH] LUCENE-3003: refactor UnInvertedField into Lucene core

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1088049 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   7 +
 .../org/apache/lucene/index/DocTermOrds.java  | 799 ++++++++++++++++++
 .../org/apache/lucene/index/IndexReader.java  |  16 +
 .../lucene/index/RandomIndexWriter.java       |   2 +-
 .../lucene/store/MockDirectoryWrapper.java    |  20 +-
 .../org/apache/lucene/util/_TestUtil.java     |  13 +
 .../apache/lucene/index/TestDocTermOrds.java  | 517 ++++++++++++
 .../apache/solr/request/UnInvertedField.java  | 743 +++-------------
 .../org/apache/solr/request/TestFaceting.java |  59 +-
 9 files changed, 1514 insertions(+), 662 deletions(-)
 create mode 100644 lucene/src/java/org/apache/lucene/index/DocTermOrds.java
 create mode 100644 lucene/src/test/org/apache/lucene/index/TestDocTermOrds.java

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 8ea03b6dcc2..64a55afadfe 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -337,6 +337,13 @@ New features
 * LUCENE-3001: Added TrieFieldHelper to write solr compatible numeric
   fields without the solr dependency. (ryan)
   
* LUCENE-3003: Added new expert class oal.index.DocTermsOrd,
  refactored from Solr's UnInvertedField, for accessing term ords for
  multi-valued fields, per document.  This is similar to FieldCache in
  that it inverts the index to compute the ords, but differs in that
  it's able to handle multi-valued fields and does not hold the term
  bytes in RAM. (Mike McCandless)

 Optimizations
 
 * LUCENE-2588: Don't store unnecessary suffixes when writing the terms
diff --git a/lucene/src/java/org/apache/lucene/index/DocTermOrds.java b/lucene/src/java/org/apache/lucene/index/DocTermOrds.java
new file mode 100644
index 00000000000..9c5361f6dde
-- /dev/null
++ b/lucene/src/java/org/apache/lucene/index/DocTermOrds.java
@@ -0,0 +1,799 @@
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

package org.apache.lucene.index;

import org.apache.lucene.util.PagedBytes;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;

/**
 * This class enables fast access to multiple term ords for
 * a specified field across all docIDs.
 *
 * Like FieldCache, it uninverts the index and holds a
 * packed data structure in RAM to enable fast access.
 * Unlike FieldCache, it can handle multi-valued fields,
 * and, it does not hold the term bytes in RAM.  Rather, you
 * must obtain a TermsEnum from the {@link #getOrdTermsEnum}
 * method, and then seek-by-ord to get the term's bytes.
 *
 * While normally term ords are type long, in this API they are
 * int as the internal representation here cannot address
 * more than MAX_INT unique terms.  Also, typically this
 * class is used on fields with relatively few unique terms
 * vs the number of documents.  In addition, there is an
 * internal limit (16 MB) on how many bytes each chunk of
 * documents may consume.  If you trip this limit you'll hit
 * an IllegalStateException.
 *
 * Deleted documents are skipped during uninversion, and if
 * you look them up you'll get 0 ords.
 *
 * The returned per-document ords do not retain their
 * original order in the document.  Instead they are returned
 * in sorted (by ord, ie term's BytesRef comparator) order.  They
 * are also de-dup'd (ie if doc has same term more than once
 * in this field, you'll only get that ord back once).
 *
 * This class tests whether the provided reader is able to
 * retrieve terms by ord (ie, it's single segment, and it
 * uses an ord-capable terms index).  If not, this class
 * will create its own term index internally, allowing to
 * create a wrapped TermsEnum that can handle ord.  The
 * {@link #getOrdTermsEnum} method then provides this
 * wrapped enum, if necessary.
 *
 * The RAM consumption of this class can be high!
 *
 * @lucene.experimental
 */

/*
 * Final form of the un-inverted field:
 *   Each document points to a list of term numbers that are contained in that document.
 *
 *   Term numbers are in sorted order, and are encoded as variable-length deltas from the
 *   previous term number.  Real term numbers start at 2 since 0 and 1 are reserved.  A
 *   term number of 0 signals the end of the termNumber list.
 *
 *   There is a single int[maxDoc()] which either contains a pointer into a byte[] for
 *   the termNumber lists, or directly contains the termNumber list if it fits in the 4
 *   bytes of an integer.  If the first byte in the integer is 1, the next 3 bytes
 *   are a pointer into a byte[] where the termNumber list starts.
 *
 *   There are actually 256 byte arrays, to compensate for the fact that the pointers
 *   into the byte arrays are only 3 bytes long.  The correct byte array for a document
 *   is a function of it's id.
 *
 *   To save space and speed up faceting, any term that matches enough documents will
 *   not be un-inverted... it will be skipped while building the un-inverted field structure,
 *   and will use a set intersection method during faceting.
 *
 *   To further save memory, the terms (the actual string values) are not all stored in
 *   memory, but a TermIndex is used to convert term numbers to term values only
 *   for the terms needed after faceting has completed.  Only every 128th term value
 *   is stored, along with it's corresponding term number, and this is used as an
 *   index to find the closest term and iterate until the desired number is hit (very
 *   much like Lucene's own internal term index).
 *
 */

public class DocTermOrds {

  // Term ords are shifted by this, internally, to reseve
  // values 0 (end term) and 1 (index is a pointer into byte array)
  private final static int TNUM_OFFSET = 2;

  // Default: every 128th term is indexed
  public final static int DEFAULT_INDEX_INTERVAL_BITS = 7; // decrease to a low number like 2 for testing

  private int indexIntervalBits;
  private int indexIntervalMask;
  private int indexInterval;

  protected final int maxTermDocFreq;

  protected final String field;

  protected int numTermsInField;
  protected long termInstances; // total number of references to term numbers
  private long memsz;
  protected int total_time;  // total time to uninvert the field
  protected int phase1_time;  // time for phase1 of the uninvert process

  protected int[] index;
  protected byte[][] tnums = new byte[256][];
  protected long sizeOfIndexedStrings;
  protected BytesRef[] indexedTermsArray;
  protected BytesRef prefix;
  protected int ordBase;

  public long ramUsedInBytes() {
    // can cache the mem size since it shouldn't change
    if (memsz!=0) return memsz;
    long sz = 8*8 + 32; // local fields
    if (index != null) sz += index.length * 4;
    if (tnums!=null) {
      for (byte[] arr : tnums)
        if (arr != null) sz += arr.length;
    }
    memsz = sz;
    return sz;
  }

  /** Inverts all terms */
  public DocTermOrds(IndexReader reader, String field) throws IOException {
    this(reader, field, null, Integer.MAX_VALUE);
  }

  /** Inverts only terms starting w/ prefix */
  public DocTermOrds(IndexReader reader, String field, BytesRef termPrefix) throws IOException {
    this(reader, field, termPrefix, Integer.MAX_VALUE);
  }

  /** Inverts only terms starting w/ prefix, and only terms
   *  whose docFreq (not taking deletions into account) is
   *  <=  maxTermDocFreq */
  public DocTermOrds(IndexReader reader, String field, BytesRef termPrefix, int maxTermDocFreq) throws IOException {
    this(reader, field, termPrefix, maxTermDocFreq, DEFAULT_INDEX_INTERVAL_BITS);
    uninvert(reader, termPrefix);
  }

  /** Inverts only terms starting w/ prefix, and only terms
   *  whose docFreq (not taking deletions into account) is
   *  <=  maxTermDocFreq, with a custom indexing interval
   *  (default is every 128nd term). */
  public DocTermOrds(IndexReader reader, String field, BytesRef termPrefix, int maxTermDocFreq, int indexIntervalBits) throws IOException {
    this(field, maxTermDocFreq, indexIntervalBits);
    uninvert(reader, termPrefix);
  }

  /** Subclass inits w/ this, but be sure you then call
   *  uninvert, only once */
  protected DocTermOrds(String field, int maxTermDocFreq, int indexIntervalBits) throws IOException {
    //System.out.println("DTO init field=" + field + " maxTDFreq=" + maxTermDocFreq);
    this.field = field;
    this.maxTermDocFreq = maxTermDocFreq;
    this.indexIntervalBits = indexIntervalBits;
    indexIntervalMask = 0xffffffff >>> (32-indexIntervalBits);
    indexInterval = 1 << indexIntervalBits;
  }

  /** Returns a TermsEnum that implements ord.  If the
   *  provided reader supports ord, we just return its
   *  TermsEnum; if it does not, we build a "private" terms
   *  index internally (WARNING: consumes RAM) and use that
   *  index to implement ord.  This also enables ord on top
   *  of a composite reader.  The returned TermsEnum is
   *  unpositioned.  This returns null if there are no terms.
   *
   *  <p><b>NOTE</b>: you must pass the same reader that was
   *  used when creating this class */
  public TermsEnum getOrdTermsEnum(IndexReader reader) throws IOException {
    if (termInstances == 0) {
      return null;
    }
    if (indexedTermsArray == null) {
      //System.out.println("GET normal enum");
      final Terms terms = MultiFields.getTerms(reader, field);
      if (terms != null) {
        return terms.iterator();
      } else {
        return null;
      }
    } else {
      //System.out.println("GET wrapped enum ordBase=" + ordBase);
      return new OrdWrappedTermsEnum(reader);
    }
  }

  /** Subclass can override this */
  protected void visitTerm(TermsEnum te, int termNum) throws IOException {
  }

  protected void setActualDocFreq(int termNum, int df) throws IOException {
  }

  // Call this only once (if you subclass!)
  protected void uninvert(final IndexReader reader, final BytesRef termPrefix) throws IOException {
    //System.out.println("DTO uninvert field=" + field + " prefix=" + termPrefix);
    final long startTime = System.currentTimeMillis();
    prefix = termPrefix == null ? null : new BytesRef(termPrefix);

    final int maxDoc = reader.maxDoc();
    final int[] index = new int[maxDoc];       // immediate term numbers, or the index into the byte[] representing the last number
    final int[] lastTerm = new int[maxDoc];    // last term we saw for this document
    final byte[][] bytes = new byte[maxDoc][]; // list of term numbers for the doc (delta encoded vInts)

    final Terms terms = MultiFields.getTerms(reader, field);
    if (terms == null) {
      // No terms
      return;
    }

    final TermsEnum te = terms.iterator();
    final BytesRef seekStart = termPrefix != null ? termPrefix : new BytesRef();
    //System.out.println("seekStart=" + seekStart.utf8ToString());
    if (te.seek(seekStart) == TermsEnum.SeekStatus.END) {
      // No terms match
      return;
    }

    // If we need our "term index wrapper", these will be
    // init'd below:
    List<BytesRef> indexedTerms = null;
    PagedBytes indexedTermsBytes = null;

    boolean testedOrd = false;

    final Bits delDocs = MultiFields.getDeletedDocs(reader);

    // we need a minimum of 9 bytes, but round up to 12 since the space would
    // be wasted with most allocators anyway.
    byte[] tempArr = new byte[12];

    //
    // enumerate all terms, and build an intermediate form of the un-inverted field.
    //
    // During this intermediate form, every document has a (potential) byte[]
    // and the int[maxDoc()] array either contains the termNumber list directly
    // or the *end* offset of the termNumber list in it's byte array (for faster
    // appending and faster creation of the final form).
    //
    // idea... if things are too large while building, we could do a range of docs
    // at a time (but it would be a fair amount slower to build)
    // could also do ranges in parallel to take advantage of multiple CPUs

    // OPTIONAL: remap the largest df terms to the lowest 128 (single byte)
    // values.  This requires going over the field first to find the most
    // frequent terms ahead of time.

    int termNum = 0;
    DocsEnum docsEnum = null;

    // Loop begins with te positioned to first term (we call
    // seek above):
    for (;;) {
      final BytesRef t = te.term();
      if (t == null || (termPrefix != null && !t.startsWith(termPrefix))) {
        break;
      }
      //System.out.println("visit term=" + t.utf8ToString() + " " + t + " termNum=" + termNum);

      if (!testedOrd) {
        try {
          ordBase = (int) te.ord();
          //System.out.println("got ordBase=" + ordBase);
        } catch (UnsupportedOperationException uoe) {
          // Reader cannot provide ord support, so we wrap
          // our own support by creating our own terms index:
          indexedTerms = new ArrayList<BytesRef>();
          indexedTermsBytes = new PagedBytes(15);
          //System.out.println("NO ORDS");
        }
        testedOrd = true;
      }

      visitTerm(te, termNum);

      if (indexedTerms != null && (termNum & indexIntervalMask) == 0) {
        // Index this term
        sizeOfIndexedStrings += t.length;
        BytesRef indexedTerm = new BytesRef();
        indexedTermsBytes.copy(t, indexedTerm);
        // TODO: really should 1) strip off useless suffix,
        // and 2) use FST not array/PagedBytes
        indexedTerms.add(indexedTerm);
      }

      final int df = te.docFreq();
      if (df <= maxTermDocFreq) {

        docsEnum = te.docs(delDocs, docsEnum);

        final DocsEnum.BulkReadResult bulkResult = docsEnum.getBulkResult();

        // dF, but takes deletions into account
        int actualDF = 0;

        for (;;) {
          int chunk = docsEnum.read();
          if (chunk <= 0) {
            break;
          }
          //System.out.println("  chunk=" + chunk + " docs");

          actualDF += chunk;

          for (int i=0; i<chunk; i++) {
            termInstances++;
            int doc = bulkResult.docs.ints[i];
            //System.out.println("    docID=" + doc);
            // add TNUM_OFFSET to the term number to make room for special reserved values:
            // 0 (end term) and 1 (index into byte array follows)
            int delta = termNum - lastTerm[doc] + TNUM_OFFSET;
            lastTerm[doc] = termNum;
            int val = index[doc];

            if ((val & 0xff)==1) {
              // index into byte array (actually the end of
              // the doc-specific byte[] when building)
              int pos = val >>> 8;
              int ilen = vIntSize(delta);
              byte[] arr = bytes[doc];
              int newend = pos+ilen;
              if (newend > arr.length) {
                // We avoid a doubling strategy to lower memory usage.
                // this faceting method isn't for docs with many terms.
                // In hotspot, objects have 2 words of overhead, then fields, rounded up to a 64-bit boundary.
                // TODO: figure out what array lengths we can round up to w/o actually using more memory
                // (how much space does a byte[] take up?  Is data preceded by a 32 bit length only?
                // It should be safe to round up to the nearest 32 bits in any case.
                int newLen = (newend + 3) & 0xfffffffc;  // 4 byte alignment
                byte[] newarr = new byte[newLen];
                System.arraycopy(arr, 0, newarr, 0, pos);
                arr = newarr;
                bytes[doc] = newarr;
              }
              pos = writeInt(delta, arr, pos);
              index[doc] = (pos<<8) | 1;  // update pointer to end index in byte[]
            } else {
              // OK, this int has data in it... find the end (a zero starting byte - not
              // part of another number, hence not following a byte with the high bit set).
              int ipos;
              if (val==0) {
                ipos=0;
              } else if ((val & 0x0000ff80)==0) {
                ipos=1;
              } else if ((val & 0x00ff8000)==0) {
                ipos=2;
              } else if ((val & 0xff800000)==0) {
                ipos=3;
              } else {
                ipos=4;
              }

              //System.out.println("      ipos=" + ipos);

              int endPos = writeInt(delta, tempArr, ipos);
              //System.out.println("      endpos=" + endPos);
              if (endPos <= 4) {
                //System.out.println("      fits!");
                // value will fit in the integer... move bytes back
                for (int j=ipos; j<endPos; j++) {
                  val |= (tempArr[j] & 0xff) << (j<<3);
                }
                index[doc] = val;
              } else {
                // value won't fit... move integer into byte[]
                for (int j=0; j<ipos; j++) {
                  tempArr[j] = (byte)val;
                  val >>>=8;
                }
                // point at the end index in the byte[]
                index[doc] = (endPos<<8) | 1;
                bytes[doc] = tempArr;
                tempArr = new byte[12];
              }
            }
          }
        }
        setActualDocFreq(termNum, actualDF);
      }

      termNum++;
      if (te.next() == null) {
        break;
      }
    }

    numTermsInField = termNum;

    long midPoint = System.currentTimeMillis();

    if (termInstances == 0) {
      // we didn't invert anything
      // lower memory consumption.
      tnums = null;
    } else {

      this.index = index;

      //
      // transform intermediate form into the final form, building a single byte[]
      // at a time, and releasing the intermediate byte[]s as we go to avoid
      // increasing the memory footprint.
      //

      for (int pass = 0; pass<256; pass++) {
        byte[] target = tnums[pass];
        int pos=0;  // end in target;
        if (target != null) {
          pos = target.length;
        } else {
          target = new byte[4096];
        }

        // loop over documents, 0x00ppxxxx, 0x01ppxxxx, 0x02ppxxxx
        // where pp is the pass (which array we are building), and xx is all values.
        // each pass shares the same byte[] for termNumber lists.
        for (int docbase = pass<<16; docbase<maxDoc; docbase+=(1<<24)) {
          int lim = Math.min(docbase + (1<<16), maxDoc);
          for (int doc=docbase; doc<lim; doc++) {
            //System.out.println("  pass=" + pass + " process docID=" + doc);
            int val = index[doc];
            if ((val&0xff) == 1) {
              int len = val >>> 8;
              //System.out.println("    ptr pos=" + pos);
              index[doc] = (pos<<8)|1; // change index to point to start of array
              if ((pos & 0xff000000) != 0) {
                // we only have 24 bits for the array index
                throw new IllegalStateException("Too many values for UnInvertedField faceting on field "+field);
              }
              byte[] arr = bytes[doc];
              /*
              for(byte b : arr) {
                //System.out.println("      b=" + Integer.toHexString((int) b));
              }
              */
              bytes[doc] = null;        // IMPORTANT: allow GC to avoid OOM
              if (target.length <= pos + len) {
                int newlen = target.length;
                /*** we don't have to worry about the array getting too large
                 * since the "pos" param will overflow first (only 24 bits available)
                if ((newlen<<1) <= 0) {
                  // overflow...
                  newlen = Integer.MAX_VALUE;
                  if (newlen <= pos + len) {
                    throw new SolrException(400,"Too many terms to uninvert field!");
                  }
                } else {
                  while (newlen <= pos + len) newlen<<=1;  // doubling strategy
                }
                ****/
                while (newlen <= pos + len) newlen<<=1;  // doubling strategy                 
                byte[] newtarget = new byte[newlen];
                System.arraycopy(target, 0, newtarget, 0, pos);
                target = newtarget;
              }
              System.arraycopy(arr, 0, target, pos, len);
              pos += len + 1;  // skip single byte at end and leave it 0 for terminator
            }
          }
        }

        // shrink array
        if (pos < target.length) {
          byte[] newtarget = new byte[pos];
          System.arraycopy(target, 0, newtarget, 0, pos);
          target = newtarget;
        }
        
        tnums[pass] = target;

        if ((pass << 16) > maxDoc)
          break;
      }

      if (indexedTerms != null) {
        indexedTermsArray = indexedTerms.toArray(new BytesRef[indexedTerms.size()]);
      }
    }

    long endTime = System.currentTimeMillis();

    total_time = (int)(endTime-startTime);
    phase1_time = (int)(midPoint-startTime);
  }

  /** Number of bytes to represent an unsigned int as a vint. */
  private static int vIntSize(int x) {
    if ((x & (0xffffffff << (7*1))) == 0 ) {
      return 1;
    }
    if ((x & (0xffffffff << (7*2))) == 0 ) {
      return 2;
    }
    if ((x & (0xffffffff << (7*3))) == 0 ) {
      return 3;
    }
    if ((x & (0xffffffff << (7*4))) == 0 ) {
      return 4;
    }
    return 5;
  }

  // todo: if we know the size of the vInt already, we could do
  // a single switch on the size
  private static int writeInt(int x, byte[] arr, int pos) {
    int a;
    a = (x >>> (7*4));
    if (a != 0) {
      arr[pos++] = (byte)(a | 0x80);
    }
    a = (x >>> (7*3));
    if (a != 0) {
      arr[pos++] = (byte)(a | 0x80);
    }
    a = (x >>> (7*2));
    if (a != 0) {
      arr[pos++] = (byte)(a | 0x80);
    }
    a = (x >>> (7*1));
    if (a != 0) {
      arr[pos++] = (byte)(a | 0x80);
    }
    arr[pos++] = (byte)(x & 0x7f);
    return pos;
  }

  public class TermOrdsIterator {
    private int tnum;
    private int upto;
    private byte[] arr;

    /** Buffer must be at least 5 ints long.  Returns number
     *  of term ords placed into buffer; if this count is
     *  less than buffer.length then that is the end. */
    public int read(int[] buffer) {
      int bufferUpto = 0;
      if (arr == null) {
        // code is inlined into upto
        //System.out.println("inlined");
        int code = upto;
        int delta = 0;
        for (;;) {
          delta = (delta << 7) | (code & 0x7f);
          if ((code & 0x80)==0) {
            if (delta==0) break;
            tnum += delta - TNUM_OFFSET;
            buffer[bufferUpto++] = ordBase+tnum;
            //System.out.println("  tnum=" + tnum);
            delta = 0;
          }
          code >>>= 8;
        }
      } else {
        // code is a pointer
        for(;;) {
          int delta = 0;
          for(;;) {
            byte b = arr[upto++];
            delta = (delta << 7) | (b & 0x7f);
            //System.out.println("    cycle: upto=" + upto + " delta=" + delta + " b=" + b);
            if ((b & 0x80) == 0) break;
          }
          //System.out.println("  delta=" + delta);
          if (delta == 0) break;
          tnum += delta - TNUM_OFFSET;
          //System.out.println("  tnum=" + tnum);
          buffer[bufferUpto++] = ordBase+tnum;
          if (bufferUpto == buffer.length) {
            break;
          }
        }
      }

      return bufferUpto;
    }

    public TermOrdsIterator reset(int docID) {
      //System.out.println("  reset docID=" + docID);
      tnum = 0;
      final int code = index[docID];
      if ((code & 0xff)==1) {
        // a pointer
        upto = code>>>8;
        //System.out.println("    pointer!  upto=" + upto);
        int whichArray = (docID >>> 16) & 0xff;
        arr = tnums[whichArray];
      } else {
        //System.out.println("    inline!");
        arr = null;
        upto = code;
      }
      return this;
    }
  }

  /** Returns an iterator to step through the term ords for
   *  this document.  It's also possible to subclass this
   *  class and directly access members. */
  public TermOrdsIterator lookup(int doc, TermOrdsIterator reuse) {
    final TermOrdsIterator ret;
    if (reuse != null) {
      ret = reuse;
    } else {
      ret = new TermOrdsIterator();
    }
    return ret.reset(doc);
  }

  /* Only used if original IndexReader doesn't implement
   * ord; in this case we "wrap" our own terms index
   * around it. */
  private final class OrdWrappedTermsEnum extends TermsEnum {
    private final IndexReader reader;
    private final TermsEnum termsEnum;
    private BytesRef term;
    private long ord = -indexInterval-1;          // force "real" seek
    
    public OrdWrappedTermsEnum(IndexReader reader) throws IOException {
      this.reader = reader;
      assert indexedTermsArray != null;
      termsEnum = MultiFields.getTerms(reader, field).iterator();
    }

    @Override
    public Comparator<BytesRef> getComparator() throws IOException {
      return termsEnum.getComparator();
    }

    @Override    
    public DocsEnum docs(Bits skipDocs, DocsEnum reuse) throws IOException {
      return termsEnum.docs(skipDocs, reuse);
    }

    @Override    
    public DocsAndPositionsEnum docsAndPositions(Bits skipDocs, DocsAndPositionsEnum reuse) throws IOException {
      return termsEnum.docsAndPositions(skipDocs, reuse);
    }

    @Override
    public BytesRef term() {
      return term;
    }

    @Override
    public BytesRef next() throws IOException {
      ord++;
      if (termsEnum.next() == null) {
        term = null;
        return null;
      }
      return setTerm();  // this is extra work if we know we are in bounds...
    }

    @Override
    public int docFreq() throws IOException {
      return termsEnum.docFreq();
    }

    @Override
    public long totalTermFreq() throws IOException {
      return termsEnum.totalTermFreq();
    }

    @Override
    public long ord() throws IOException {
      return ordBase + ord;
    }

    @Override
    public SeekStatus seek(BytesRef target, boolean useCache) throws IOException {

      // already here
      if (term != null && term.equals(target)) {
        return SeekStatus.FOUND;
      }

      int startIdx = Arrays.binarySearch(indexedTermsArray, target);

      if (startIdx >= 0) {
        // we hit the term exactly... lucky us!
        TermsEnum.SeekStatus seekStatus = termsEnum.seek(target);
        assert seekStatus == TermsEnum.SeekStatus.FOUND;
        ord = startIdx << indexIntervalBits;
        setTerm();
        assert term != null;
        return SeekStatus.FOUND;
      }

      // we didn't hit the term exactly
      startIdx = -startIdx-1;
    
      if (startIdx == 0) {
        // our target occurs *before* the first term
        TermsEnum.SeekStatus seekStatus = termsEnum.seek(target);
        assert seekStatus == TermsEnum.SeekStatus.NOT_FOUND;
        ord = 0;
        setTerm();
        assert term != null;
        return SeekStatus.NOT_FOUND;
      }

      // back up to the start of the block
      startIdx--;

      if ((ord >> indexIntervalBits) == startIdx && term != null && term.compareTo(target) <= 0) {
        // we are already in the right block and the current term is before the term we want,
        // so we don't need to seek.
      } else {
        // seek to the right block
        TermsEnum.SeekStatus seekStatus = termsEnum.seek(indexedTermsArray[startIdx]);
        assert seekStatus == TermsEnum.SeekStatus.FOUND;
        ord = startIdx << indexIntervalBits;
        setTerm();
        assert term != null;  // should be non-null since it's in the index
      }

      while (term != null && term.compareTo(target) < 0) {
        next();
      }

      if (term == null) {
        return SeekStatus.END;
      } else if (term.compareTo(target) == 0) {
        return SeekStatus.FOUND;
      } else {
        return SeekStatus.NOT_FOUND;
      }
    }

    @Override
    public SeekStatus seek(long targetOrd) throws IOException {
      int delta = (int) (targetOrd - ordBase - ord);
      //System.out.println("  seek(ord) targetOrd=" + targetOrd + " delta=" + delta + " ord=" + ord);
      if (delta < 0 || delta > indexInterval) {
        final int idx = (int) (targetOrd >>> indexIntervalBits);
        final BytesRef base = indexedTermsArray[idx];
        //System.out.println("  do seek term=" + base.utf8ToString());
        ord = idx << indexIntervalBits;
        delta = (int) (targetOrd - ord);
        final TermsEnum.SeekStatus seekStatus = termsEnum.seek(base, true);
        assert seekStatus == TermsEnum.SeekStatus.FOUND;
      } else {
        //System.out.println("seek w/in block");
      }

      while (--delta >= 0) {
        BytesRef br = termsEnum.next();
        if (br == null) {
          term = null;
          return null;
        }
        ord++;
      }

      setTerm();
      return term == null ? SeekStatus.END : SeekStatus.FOUND;
      //System.out.println("  return term=" + term.utf8ToString());
    }

    private BytesRef setTerm() throws IOException {
      term = termsEnum.term();
      //System.out.println("  setTerm() term=" + term.utf8ToString() + " vs prefix=" + (prefix == null ? "null" : prefix.utf8ToString()));
      if (prefix != null && !term.startsWith(prefix)) {
        term = null;
      }
      return term;
    }
  }

  public BytesRef lookupTerm(TermsEnum termsEnum, int ord) throws IOException {
    TermsEnum.SeekStatus status = termsEnum.seek(ord);
    assert status == TermsEnum.SeekStatus.FOUND;
    return termsEnum.term();
  }
}
diff --git a/lucene/src/java/org/apache/lucene/index/IndexReader.java b/lucene/src/java/org/apache/lucene/index/IndexReader.java
index d1e37c576bd..9f3494c4654 100644
-- a/lucene/src/java/org/apache/lucene/index/IndexReader.java
++ b/lucene/src/java/org/apache/lucene/index/IndexReader.java
@@ -919,6 +919,22 @@ public abstract class IndexReader implements Cloneable,Closeable {
     }
   }
 
  /**
   * Returns <code>true</code> if an index exists at the specified directory.
   * @param  directory the directory to check for an index
   * @param  codecProvider provides a CodecProvider in case the index uses non-core codecs
   * @return <code>true</code> if an index exists; <code>false</code> otherwise
   * @throws IOException if there is a problem with accessing the index
   */
  public static boolean indexExists(Directory directory, CodecProvider codecProvider) throws IOException {
    try {
      new SegmentInfos().read(directory, codecProvider);
      return true;
    } catch (IOException ioe) {
      return false;
    }
  }

   /** Returns the number of documents in this index. */
   public abstract int numDocs();
 
diff --git a/lucene/src/test-framework/org/apache/lucene/index/RandomIndexWriter.java b/lucene/src/test-framework/org/apache/lucene/index/RandomIndexWriter.java
index 27962632acc..5c68a75951a 100644
-- a/lucene/src/test-framework/org/apache/lucene/index/RandomIndexWriter.java
++ b/lucene/src/test-framework/org/apache/lucene/index/RandomIndexWriter.java
@@ -181,7 +181,7 @@ public class RandomIndexWriter implements Closeable {
         System.out.println("RIW.getReader: open new reader");
       }
       w.commit();
      return IndexReader.open(w.getDirectory(), new KeepOnlyLastCommitDeletionPolicy(), r.nextBoolean(), _TestUtil.nextInt(r, 1, 10));
      return IndexReader.open(w.getDirectory(), new KeepOnlyLastCommitDeletionPolicy(), r.nextBoolean(), _TestUtil.nextInt(r, 1, 10), w.getConfig().getCodecProvider());
     }
   }
 
diff --git a/lucene/src/test-framework/org/apache/lucene/store/MockDirectoryWrapper.java b/lucene/src/test-framework/org/apache/lucene/store/MockDirectoryWrapper.java
index 03c07f45db3..c04871daf01 100644
-- a/lucene/src/test-framework/org/apache/lucene/store/MockDirectoryWrapper.java
++ b/lucene/src/test-framework/org/apache/lucene/store/MockDirectoryWrapper.java
@@ -32,6 +32,7 @@ import java.util.Random;
 import java.util.Set;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.codecs.CodecProvider;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.util._TestUtil;
 
@@ -419,12 +420,27 @@ public class MockDirectoryWrapper extends Directory {
       throw new RuntimeException("MockDirectoryWrapper: cannot close: there are still open files: " + openFiles, cause);
     }
     open = false;
    if (checkIndexOnClose && IndexReader.indexExists(this)) {
      _TestUtil.checkIndex(this);
    if (checkIndexOnClose) {
      if (codecProvider != null) {
        if (IndexReader.indexExists(this, codecProvider)) {
          _TestUtil.checkIndex(this, codecProvider);
        }
      } else {
        if (IndexReader.indexExists(this)) {
          _TestUtil.checkIndex(this);
        }
      }
     }
     delegate.close();
   }
 
  private CodecProvider codecProvider;

  // We pass this CodecProvider to checkIndex when dir is closed...
  public void setCodecProvider(CodecProvider cp) {
    codecProvider = cp;
  }

   boolean open = true;
   
   public synchronized boolean isOpen() {
diff --git a/lucene/src/test-framework/org/apache/lucene/util/_TestUtil.java b/lucene/src/test-framework/org/apache/lucene/util/_TestUtil.java
index ad3efa76a21..bd49677b755 100644
-- a/lucene/src/test-framework/org/apache/lucene/util/_TestUtil.java
++ b/lucene/src/test-framework/org/apache/lucene/util/_TestUtil.java
@@ -157,6 +157,19 @@ public class _TestUtil {
     return start + r.nextInt(end-start+1);
   }
 
  public static String randomSimpleString(Random r) {
    final int end = r.nextInt(10);
    if (end == 0) {
      // allow 0 length
      return "";
    }
    final char[] buffer = new char[end];
    for (int i = 0; i < end; i++) {
      buffer[i] = (char) _TestUtil.nextInt(r, 97, 102);
    }
    return new String(buffer, 0, end);
  }

   /** Returns random string, including full unicode range. */
   public static String randomUnicodeString(Random r) {
     return randomUnicodeString(r, 20);
diff --git a/lucene/src/test/org/apache/lucene/index/TestDocTermOrds.java b/lucene/src/test/org/apache/lucene/index/TestDocTermOrds.java
new file mode 100644
index 00000000000..fc771770413
-- /dev/null
++ b/lucene/src/test/org/apache/lucene/index/TestDocTermOrds.java
@@ -0,0 +1,517 @@
package org.apache.lucene.index;

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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.DocTermOrds.TermOrdsIterator;
import org.apache.lucene.index.codecs.BlockTermsReader;
import org.apache.lucene.index.codecs.BlockTermsWriter;
import org.apache.lucene.index.codecs.Codec;
import org.apache.lucene.index.codecs.CoreCodecProvider;
import org.apache.lucene.index.codecs.FieldsConsumer;
import org.apache.lucene.index.codecs.FieldsProducer;
import org.apache.lucene.index.codecs.FixedGapTermsIndexReader;
import org.apache.lucene.index.codecs.FixedGapTermsIndexWriter;
import org.apache.lucene.index.codecs.PostingsReaderBase;
import org.apache.lucene.index.codecs.PostingsWriterBase;
import org.apache.lucene.index.codecs.TermsIndexReaderBase;
import org.apache.lucene.index.codecs.TermsIndexWriterBase;
import org.apache.lucene.index.codecs.standard.StandardPostingsReader;
import org.apache.lucene.index.codecs.standard.StandardPostingsWriter;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MockDirectoryWrapper;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;

// TODO:
//   - test w/ del docs
//   - test prefix
//   - test w/ cutoff
//   - crank docs way up so we get some merging sometimes

public class TestDocTermOrds extends LuceneTestCase {

  public void testSimple() throws Exception {
    Directory dir = newDirectory();
    final RandomIndexWriter w = new RandomIndexWriter(random, dir, newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer()).setMergePolicy(newInOrderLogMergePolicy()));
    Document doc = new Document();
    Field field = newField("field", "", Field.Index.ANALYZED);
    doc.add(field);
    field.setValue("a b c");
    w.addDocument(doc);

    field.setValue("d e f");
    w.addDocument(doc);

    field.setValue("a f");
    w.addDocument(doc);
    
    final IndexReader r = w.getReader();
    w.close();

    final DocTermOrds dto = new DocTermOrds(r, "field");

    TermOrdsIterator iter = dto.lookup(0, null);
    final int[] buffer = new int[5];
    assertEquals(3, iter.read(buffer));
    assertEquals(0, buffer[0]);
    assertEquals(1, buffer[1]);
    assertEquals(2, buffer[2]);

    iter = dto.lookup(1, iter);
    assertEquals(3, iter.read(buffer));
    assertEquals(3, buffer[0]);
    assertEquals(4, buffer[1]);
    assertEquals(5, buffer[2]);

    iter = dto.lookup(2, iter);
    assertEquals(2, iter.read(buffer));
    assertEquals(0, buffer[0]);
    assertEquals(5, buffer[1]);

    r.close();
    dir.close();
  }

  private static class StandardCodecWithOrds extends Codec {
    public StandardCodecWithOrds() {
      name = "StandardOrds";
    }

    @Override
    public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
      PostingsWriterBase docs = new StandardPostingsWriter(state);

      // TODO: should we make the terms index more easily
      // pluggable?  Ie so that this codec would record which
      // index impl was used, and switch on loading?
      // Or... you must make a new Codec for this?
      TermsIndexWriterBase indexWriter;
      boolean success = false;
      try {
        indexWriter = new FixedGapTermsIndexWriter(state);
        success = true;
      } finally {
        if (!success) {
          docs.close();
        }
      }

      success = false;
      try {
        FieldsConsumer ret = new BlockTermsWriter(indexWriter, state, docs);
        success = true;
        return ret;
      } finally {
        if (!success) {
          try {
            docs.close();
          } finally {
            indexWriter.close();
          }
        }
      }
    }

    public final static int TERMS_CACHE_SIZE = 1024;

    @Override
    public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
      PostingsReaderBase postings = new StandardPostingsReader(state.dir, state.segmentInfo, state.readBufferSize, state.codecId);
      TermsIndexReaderBase indexReader;

      boolean success = false;
      try {
        indexReader = new FixedGapTermsIndexReader(state.dir,
                                                   state.fieldInfos,
                                                   state.segmentInfo.name,
                                                   state.termsIndexDivisor,
                                                   BytesRef.getUTF8SortedAsUnicodeComparator(),
                                                   state.codecId);
        success = true;
      } finally {
        if (!success) {
          postings.close();
        }
      }

      success = false;
      try {
        FieldsProducer ret = new BlockTermsReader(indexReader,
                                                  state.dir,
                                                  state.fieldInfos,
                                                  state.segmentInfo.name,
                                                  postings,
                                                  state.readBufferSize,
                                                  TERMS_CACHE_SIZE,
                                                  state.codecId);
        success = true;
        return ret;
      } finally {
        if (!success) {
          try {
            postings.close();
          } finally {
            indexReader.close();
          }
        }
      }
    }

    /** Extension of freq postings file */
    static final String FREQ_EXTENSION = "frq";

    /** Extension of prox postings file */
    static final String PROX_EXTENSION = "prx";

    @Override
    public void files(Directory dir, SegmentInfo segmentInfo, String id, Set<String> files) throws IOException {
      StandardPostingsReader.files(dir, segmentInfo, id, files);
      BlockTermsReader.files(dir, segmentInfo, id, files);
      FixedGapTermsIndexReader.files(dir, segmentInfo, id, files);
    }

    @Override
    public void getExtensions(Set<String> extensions) {
      getStandardExtensions(extensions);
    }

    public static void getStandardExtensions(Set<String> extensions) {
      extensions.add(FREQ_EXTENSION);
      extensions.add(PROX_EXTENSION);
      BlockTermsReader.getExtensions(extensions);
      FixedGapTermsIndexReader.getIndexExtensions(extensions);
    }
  }

  public void testRandom() throws Exception {
    MockDirectoryWrapper dir = newDirectory();

    final int NUM_TERMS = 100 * RANDOM_MULTIPLIER;
    final Set<BytesRef> terms = new HashSet<BytesRef>();
    while(terms.size() < NUM_TERMS) {
      final String s = _TestUtil.randomRealisticUnicodeString(random);
      //final String s = _TestUtil.randomSimpleString(random);
      if (s.length() > 0) {
        terms.add(new BytesRef(s));
      }
    }
    final BytesRef[] termsArray = terms.toArray(new BytesRef[terms.size()]);
    Arrays.sort(termsArray);
    
    final int NUM_DOCS = 1000 * RANDOM_MULTIPLIER;

    IndexWriterConfig conf = newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer());

    // Sometimes swap in codec that impls ord():
    if (random.nextInt(10) == 7) {
      // Make sure terms index has ords:
      CoreCodecProvider cp = new CoreCodecProvider();
      cp.register(new StandardCodecWithOrds());
      cp.setDefaultFieldCodec("StandardOrds");

      // So checkIndex on close works
      dir.setCodecProvider(cp);
      conf.setCodecProvider(cp);
    }
    
    final RandomIndexWriter w = new RandomIndexWriter(random, dir, conf);

    final int[][] idToOrds = new int[NUM_DOCS][];
    final Set<Integer> ordsForDocSet = new HashSet<Integer>();

    for(int id=0;id<NUM_DOCS;id++) {
      Document doc = new Document();

      NumericField idField = new NumericField("id");
      doc.add(idField.setIntValue(id));
      
      final int termCount = _TestUtil.nextInt(random, 0, 20*RANDOM_MULTIPLIER);
      while(ordsForDocSet.size() < termCount) {
        ordsForDocSet.add(random.nextInt(termsArray.length));
      }
      final int[] ordsForDoc = new int[termCount];
      int upto = 0;
      if (VERBOSE) {
        System.out.println("TEST: doc id=" + id);
      }
      for(int ord : ordsForDocSet) {
        ordsForDoc[upto++] = ord;
        Field field = newField("field", termsArray[ord].utf8ToString(), Field.Index.NOT_ANALYZED);
        if (VERBOSE) {
          System.out.println("  f=" + termsArray[ord].utf8ToString());
        }
        doc.add(field);
      }
      ordsForDocSet.clear();
      Arrays.sort(ordsForDoc);
      idToOrds[id] = ordsForDoc;
      w.addDocument(doc);
    }
    
    final IndexReader r = w.getReader();
    w.close();

    if (VERBOSE) {
      System.out.println("TEST: reader=" + r);
    }

    for(IndexReader subR : r.getSequentialSubReaders()) {
      if (VERBOSE) {
        System.out.println("\nTEST: sub=" + subR);
      }
      verify(subR, idToOrds, termsArray, null);
    }

    // Also test top-level reader: its enum does not support
    // ord, so this forces the OrdWrapper to run:
    if (VERBOSE) {
      System.out.println("TEST: top reader");
    }
    verify(r, idToOrds, termsArray, null);

    FieldCache.DEFAULT.purge(r);

    r.close();
    dir.close();
  }

  public void testRandomWithPrefix() throws Exception {
    MockDirectoryWrapper dir = newDirectory();

    final Set<String> prefixes = new HashSet<String>();
    final int numPrefix = _TestUtil.nextInt(random, 2, 7);
    if (VERBOSE) {
      System.out.println("TEST: use " + numPrefix + " prefixes");
    }
    while(prefixes.size() < numPrefix) {
      prefixes.add(_TestUtil.randomRealisticUnicodeString(random));
      //prefixes.add(_TestUtil.randomSimpleString(random));
    }
    final String[] prefixesArray = prefixes.toArray(new String[prefixes.size()]);

    final int NUM_TERMS = 100 * RANDOM_MULTIPLIER;
    final Set<BytesRef> terms = new HashSet<BytesRef>();
    while(terms.size() < NUM_TERMS) {
      final String s = prefixesArray[random.nextInt(prefixesArray.length)] + _TestUtil.randomRealisticUnicodeString(random);
      //final String s = prefixesArray[random.nextInt(prefixesArray.length)] + _TestUtil.randomSimpleString(random);
      if (s.length() > 0) {
        terms.add(new BytesRef(s));
      }
    }
    final BytesRef[] termsArray = terms.toArray(new BytesRef[terms.size()]);
    Arrays.sort(termsArray);
    
    final int NUM_DOCS = 1000 * RANDOM_MULTIPLIER;

    IndexWriterConfig conf = newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer());

    // Sometimes swap in codec that impls ord():
    if (random.nextInt(10) == 7) {
      // Make sure terms index has ords:
      CoreCodecProvider cp = new CoreCodecProvider();
      cp.register(new StandardCodecWithOrds());
      cp.setDefaultFieldCodec("StandardOrds");

      // So checkIndex on close works
      dir.setCodecProvider(cp);
      conf.setCodecProvider(cp);
    }
    
    final RandomIndexWriter w = new RandomIndexWriter(random, dir, conf);

    final int[][] idToOrds = new int[NUM_DOCS][];
    final Set<Integer> ordsForDocSet = new HashSet<Integer>();

    for(int id=0;id<NUM_DOCS;id++) {
      Document doc = new Document();

      NumericField idField = new NumericField("id");
      doc.add(idField.setIntValue(id));
      
      final int termCount = _TestUtil.nextInt(random, 0, 20*RANDOM_MULTIPLIER);
      while(ordsForDocSet.size() < termCount) {
        ordsForDocSet.add(random.nextInt(termsArray.length));
      }
      final int[] ordsForDoc = new int[termCount];
      int upto = 0;
      if (VERBOSE) {
        System.out.println("TEST: doc id=" + id);
      }
      for(int ord : ordsForDocSet) {
        ordsForDoc[upto++] = ord;
        Field field = newField("field", termsArray[ord].utf8ToString(), Field.Index.NOT_ANALYZED);
        if (VERBOSE) {
          System.out.println("  f=" + termsArray[ord].utf8ToString());
        }
        doc.add(field);
      }
      ordsForDocSet.clear();
      Arrays.sort(ordsForDoc);
      idToOrds[id] = ordsForDoc;
      w.addDocument(doc);
    }
    
    final IndexReader r = w.getReader();
    w.close();

    if (VERBOSE) {
      System.out.println("TEST: reader=" + r);
    }
    
    for(String prefix : prefixesArray) {

      final BytesRef prefixRef = prefix == null ? null : new BytesRef(prefix);

      final int[][] idToOrdsPrefix = new int[NUM_DOCS][];
      for(int id=0;id<NUM_DOCS;id++) {
        final int[] docOrds = idToOrds[id];
        final List<Integer> newOrds = new ArrayList<Integer>();
        for(int ord : idToOrds[id]) {
          if (termsArray[ord].startsWith(prefixRef)) {
            newOrds.add(ord);
          }
        }
        final int[] newOrdsArray = new int[newOrds.size()];
        int upto = 0;
        for(int ord : newOrds) {
          newOrdsArray[upto++] = ord;
        }
        idToOrdsPrefix[id] = newOrdsArray;
      }

      for(IndexReader subR : r.getSequentialSubReaders()) {
        if (VERBOSE) {
          System.out.println("\nTEST: sub=" + subR);
        }
        verify(subR, idToOrdsPrefix, termsArray, prefixRef);
      }

      // Also test top-level reader: its enum does not support
      // ord, so this forces the OrdWrapper to run:
      if (VERBOSE) {
        System.out.println("TEST: top reader");
      }
      verify(r, idToOrdsPrefix, termsArray, prefixRef);
    }

    FieldCache.DEFAULT.purge(r);

    r.close();
    dir.close();
  }

  private void verify(IndexReader r, int[][] idToOrds, BytesRef[] termsArray, BytesRef prefixRef) throws Exception {

    final DocTermOrds dto = new DocTermOrds(r,
                                            "field",
                                            prefixRef,
                                            Integer.MAX_VALUE,
                                            _TestUtil.nextInt(random, 2, 10));
                                            

    final int[] docIDToID = FieldCache.DEFAULT.getInts(r, "id");
    /*
      for(int docID=0;docID<subR.maxDoc();docID++) {
      System.out.println("  docID=" + docID + " id=" + docIDToID[docID]);
      }
    */

    if (VERBOSE) {
      System.out.println("TEST: verify prefix=" + prefixRef.utf8ToString());
      System.out.println("TEST: all TERMS:");
      TermsEnum allTE = MultiFields.getTerms(r, "field").iterator();
      int ord = 0;
      while(allTE.next() != null) {
        System.out.println("  ord=" + (ord++) + " term=" + allTE.term().utf8ToString());
      }
    }

    //final TermsEnum te = subR.fields().terms("field").iterator();
    final TermsEnum te = dto.getOrdTermsEnum(r);
    if (te == null) {
      if (prefixRef == null) {
        assertNull(r.fields().terms("field"));
      } else {
        Terms terms = r.fields().terms("field");
        if (terms != null) {
          TermsEnum termsEnum = terms.iterator();
          TermsEnum.SeekStatus result = termsEnum.seek(prefixRef, false);
          if (result != TermsEnum.SeekStatus.END) {
            assertFalse("term=" + termsEnum.term().utf8ToString() + " matches prefix=" + prefixRef.utf8ToString(), termsEnum.term().startsWith(prefixRef));
          } else {
            // ok
          }
        } else {
          // ok
        }
      }
      return;
    }

    if (VERBOSE) {
      System.out.println("TEST: TERMS:");
      te.seek(0);
      while(true) {
        System.out.println("  ord=" + te.ord() + " term=" + te.term().utf8ToString());
        if (te.next() == null) {
          break;
        }
      }
    }

    TermOrdsIterator iter = null;
    final int[] buffer = new int[5];
    for(int docID=0;docID<r.maxDoc();docID++) {
      if (VERBOSE) {
        System.out.println("TEST: docID=" + docID + " of " + r.maxDoc() + " (id=" + docIDToID[docID] + ")");
      }
      iter = dto.lookup(docID, iter);
      final int[] answers = idToOrds[docIDToID[docID]];
      int upto = 0;
      while(true) {
        final int chunk = iter.read(buffer);
        for(int idx=0;idx<chunk;idx++) {
          assertEquals(TermsEnum.SeekStatus.FOUND, te.seek((long) buffer[idx]));
          final BytesRef expected = termsArray[answers[upto++]];
          if (VERBOSE) {
            System.out.println("  exp=" + expected.utf8ToString() + " actual=" + te.term().utf8ToString());
          }
          assertEquals("expected=" + expected.utf8ToString() + " actual=" + te.term().utf8ToString() + " ord=" + buffer[idx], expected, te.term());
        }
        
        if (chunk < buffer.length) {
          assertEquals(answers.length, upto);
          break;
        }
      }
    }
  }
}
diff --git a/solr/src/java/org/apache/solr/request/UnInvertedField.java b/solr/src/java/org/apache/solr/request/UnInvertedField.java
index ede2328b2f2..48ff9102281 100755
-- a/solr/src/java/org/apache/solr/request/UnInvertedField.java
++ b/solr/src/java/org/apache/solr/request/UnInvertedField.java
@@ -18,16 +18,11 @@
 package org.apache.solr.request;
 
 import org.apache.lucene.search.FieldCache;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DocTermOrds;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.DocsAndPositionsEnum;
 import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.PagedBytes;
 import org.apache.noggit.CharArr;
 import org.apache.solr.common.params.FacetParams;
 import org.apache.solr.common.util.NamedList;
@@ -44,15 +39,11 @@ import org.apache.solr.handler.component.StatsValues;
 import org.apache.solr.handler.component.FieldFacetStats;
 import org.apache.lucene.util.OpenBitSet;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;
 
 import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.Map;
import java.util.Comparator;
 
 import java.util.concurrent.atomic.AtomicLong;
 
@@ -86,7 +77,7 @@ import java.util.concurrent.atomic.AtomicLong;
  *   much like Lucene's own internal term index).
  *
  */
public class UnInvertedField {
public class UnInvertedField extends DocTermOrds {
   private static int TNUM_OFFSET=2;
 
   static class TopTerm {
@@ -100,362 +91,109 @@ public class UnInvertedField {
     }
   }
 
  String field;
  int numTermsInField;
  int termsInverted;  // number of unique terms that were un-inverted
  long termInstances; // total number of references to term numbers
  final TermIndex ti;
   long memsz;
  int total_time;  // total time to uninvert the field
  int phase1_time;  // time for phase1 of the uninvert process
   final AtomicLong use = new AtomicLong(); // number of uses
 
  int[] index;
  byte[][] tnums = new byte[256][];
  int[] maxTermCounts;
  int[] maxTermCounts = new int[1024];

   final Map<Integer,TopTerm> bigTerms = new LinkedHashMap<Integer,TopTerm>();
 
  private SolrIndexSearcher.DocsEnumState deState;
  private final SolrIndexSearcher searcher;

  @Override
  protected void visitTerm(TermsEnum te, int termNum) throws IOException {

    if (termNum >= maxTermCounts.length) {
      // resize by doubling - for very large number of unique terms, expanding
      // by 4K and resultant GC will dominate uninvert times.  Resize at end if material
      int[] newMaxTermCounts = new int[maxTermCounts.length*2];
      System.arraycopy(maxTermCounts, 0, newMaxTermCounts, 0, termNum);
      maxTermCounts = newMaxTermCounts;
    }

    final BytesRef term = te.term();

    if (te.docFreq() > maxTermDocFreq) {
      TopTerm topTerm = new TopTerm();
      topTerm.term = new BytesRef(term);
      topTerm.termNum = termNum;
      bigTerms.put(topTerm.termNum, topTerm);

      if (deState == null) {
        deState = new SolrIndexSearcher.DocsEnumState();
        deState.termsEnum = te;
      }

      maxTermCounts[termNum] = searcher.getDocSet(new TermQuery(new Term(field, topTerm.term)), deState).size();
      System.out.println("  big term termNum=" + termNum + " term=" + topTerm.term.utf8ToString() + " size=" + maxTermCounts[termNum] + " dF=" + te.docFreq());
    }
  }

  @Override
  protected void setActualDocFreq(int termNum, int docFreq) {
    maxTermCounts[termNum] = docFreq;
  }
 
   public long memSize() {
     // can cache the mem size since it shouldn't change
     if (memsz!=0) return memsz;
    long sz = 8*8 + 32; // local fields
    long sz = super.ramUsedInBytes();
    sz += 8*8 + 32; // local fields
     sz += bigTerms.size() * 64;
     for (TopTerm tt : bigTerms.values()) {
       sz += tt.memSize();
     }
    if (index != null) sz += index.length * 4;
    if (tnums!=null) {
      for (byte[] arr : tnums)
        if (arr != null) sz += arr.length;
    }
     if (maxTermCounts != null)
       sz += maxTermCounts.length * 4;
    sz += ti.memSize();
    if (indexedTermsArray != null) {
      // assume 8 byte references?
      sz += 8+8+8+8+(indexedTermsArray.length<<3)+sizeOfIndexedStrings;
    }
     memsz = sz;
     return sz;
   }
 

  /** Number of bytes to represent an unsigned int as a vint. */
  static int vIntSize(int x) {
    if ((x & (0xffffffff << (7*1))) == 0 ) {
      return 1;
    }
    if ((x & (0xffffffff << (7*2))) == 0 ) {
      return 2;
    }
    if ((x & (0xffffffff << (7*3))) == 0 ) {
      return 3;
    }
    if ((x & (0xffffffff << (7*4))) == 0 ) {
      return 4;
    }
    return 5;
  }


  // todo: if we know the size of the vInt already, we could do
  // a single switch on the size
  static int writeInt(int x, byte[] arr, int pos) {
    int a;
    a = (x >>> (7*4));
    if (a != 0) {
      arr[pos++] = (byte)(a | 0x80);
    }
    a = (x >>> (7*3));
    if (a != 0) {
      arr[pos++] = (byte)(a | 0x80);
    }
    a = (x >>> (7*2));
    if (a != 0) {
      arr[pos++] = (byte)(a | 0x80);
    }
    a = (x >>> (7*1));
    if (a != 0) {
      arr[pos++] = (byte)(a | 0x80);
    }
    arr[pos++] = (byte)(x & 0x7f);
    return pos;
  }



   public UnInvertedField(String field, SolrIndexSearcher searcher) throws IOException {
    this.field = field;
    this.ti = new TermIndex(field,
            TrieField.getMainValuePrefix(searcher.getSchema().getFieldType(field)));
    uninvert(searcher);
  }


  private void uninvert(SolrIndexSearcher searcher) throws IOException {
    long startTime = System.currentTimeMillis();

    IndexReader reader = searcher.getIndexReader();
    int maxDoc = reader.maxDoc();

    int[] index = new int[maxDoc];       // immediate term numbers, or the index into the byte[] representing the last number
    this.index = index;
    final int[] lastTerm = new int[maxDoc];    // last term we saw for this document
    final byte[][] bytes = new byte[maxDoc][]; // list of term numbers for the doc (delta encoded vInts)
    maxTermCounts = new int[1024];

    NumberedTermsEnum te = ti.getEnumerator(reader);

    // threshold, over which we use set intersections instead of counting
    // to (1) save memory, and (2) speed up faceting.
    // Add 2 for testing purposes so that there will always be some terms under
    // the threshold even when the index is very small.
    int threshold = maxDoc / 20 + 2;
    // threshold = 2000000000; //////////////////////////////// USE FOR TESTING

    // we need a minimum of 9 bytes, but round up to 12 since the space would
    // be wasted with most allocators anyway.
    byte[] tempArr = new byte[12];

    //
    // enumerate all terms, and build an intermediate form of the un-inverted field.
    //
    // During this intermediate form, every document has a (potential) byte[]
    // and the int[maxDoc()] array either contains the termNumber list directly
    // or the *end* offset of the termNumber list in it's byte array (for faster
    // appending and faster creation of the final form).
    //
    // idea... if things are too large while building, we could do a range of docs
    // at a time (but it would be a fair amount slower to build)
    // could also do ranges in parallel to take advantage of multiple CPUs

    // OPTIONAL: remap the largest df terms to the lowest 128 (single byte)
    // values.  This requires going over the field first to find the most
    // frequent terms ahead of time.

    SolrIndexSearcher.DocsEnumState deState = null;

    for (;;) {
      BytesRef t = te.term();
      if (t==null) break;

      int termNum = te.getTermNumber();

      if (termNum >= maxTermCounts.length) {
        // resize by doubling - for very large number of unique terms, expanding
        // by 4K and resultant GC will dominate uninvert times.  Resize at end if material
        int[] newMaxTermCounts = new int[maxTermCounts.length*2];
        System.arraycopy(maxTermCounts, 0, newMaxTermCounts, 0, termNum);
        maxTermCounts = newMaxTermCounts;
      }

      int df = te.docFreq();
      if (df >= threshold) {
        TopTerm topTerm = new TopTerm();
        topTerm.term = new BytesRef(t);
        topTerm.termNum = termNum;
        bigTerms.put(topTerm.termNum, topTerm);

        if (deState == null) {
          deState = new SolrIndexSearcher.DocsEnumState();
          deState.termsEnum = te.tenum;
          deState.reuse = te.docsEnum;
    super(field,
          // threshold, over which we use set intersections instead of counting
          // to (1) save memory, and (2) speed up faceting.
          // Add 1 for testing purposes so that there will always be some terms under
          // the threshold even when the index is very
          // small.
          searcher.maxDoc()/20 + 1,
          DEFAULT_INDEX_INTERVAL_BITS);
    //System.out.println("maxTermDocFreq=" + maxTermDocFreq + " maxDoc=" + searcher.maxDoc());

    final String prefix = TrieField.getMainValuePrefix(searcher.getSchema().getFieldType(field));
    this.searcher = searcher;
    try {
      uninvert(searcher.getIndexReader(), prefix == null ? null : new BytesRef(prefix));
    } catch (IllegalStateException ise) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, ise.getMessage());
    }
    if (tnums != null) {
      for(byte[] target : tnums) {
        if (target != null && target.length > (1<<24)*.9) {
          SolrCore.log.warn("Approaching too many values for UnInvertedField faceting on field '"+field+"' : bucket size=" + target.length);
         }
        DocSet set = searcher.getDocSet(new TermQuery(new Term(ti.field, topTerm.term)), deState);
        te.docsEnum = deState.reuse;

        maxTermCounts[termNum] = set.size();

        te.next();
        continue;
       }

      termsInverted++;

      DocsEnum docsEnum = te.getDocsEnum();

      DocsEnum.BulkReadResult bulkResult = docsEnum.getBulkResult();

      for(;;) {
        int n = docsEnum.read();
        if (n <= 0) break;

        maxTermCounts[termNum] += n;

        for (int i=0; i<n; i++) {
          termInstances++;
          int doc = bulkResult.docs.ints[i];
          // add 2 to the term number to make room for special reserved values:
          // 0 (end term) and 1 (index into byte array follows)
          int delta = termNum - lastTerm[doc] + TNUM_OFFSET;
          lastTerm[doc] = termNum;
          int val = index[doc];

          if ((val & 0xff)==1) {
            // index into byte array (actually the end of
            // the doc-specific byte[] when building)
            int pos = val >>> 8;
            int ilen = vIntSize(delta);
            byte[] arr = bytes[doc];
            int newend = pos+ilen;
            if (newend > arr.length) {
              // We avoid a doubling strategy to lower memory usage.
              // this faceting method isn't for docs with many terms.
              // In hotspot, objects have 2 words of overhead, then fields, rounded up to a 64-bit boundary.
              // TODO: figure out what array lengths we can round up to w/o actually using more memory
              // (how much space does a byte[] take up?  Is data preceded by a 32 bit length only?
              // It should be safe to round up to the nearest 32 bits in any case.
              int newLen = (newend + 3) & 0xfffffffc;  // 4 byte alignment
              byte[] newarr = new byte[newLen];
              System.arraycopy(arr, 0, newarr, 0, pos);
              arr = newarr;
              bytes[doc] = newarr;
            }
            pos = writeInt(delta, arr, pos);
            index[doc] = (pos<<8) | 1;  // update pointer to end index in byte[]
          } else {
            // OK, this int has data in it... find the end (a zero starting byte - not
            // part of another number, hence not following a byte with the high bit set).
            int ipos;
            if (val==0) {
              ipos=0;
            } else if ((val & 0x0000ff80)==0) {
              ipos=1;
            } else if ((val & 0x00ff8000)==0) {
              ipos=2;
            } else if ((val & 0xff800000)==0) {
              ipos=3;
            } else {
              ipos=4;
            }

            int endPos = writeInt(delta, tempArr, ipos);
            if (endPos <= 4) {
              // value will fit in the integer... move bytes back
              for (int j=ipos; j<endPos; j++) {
                val |= (tempArr[j] & 0xff) << (j<<3);
              }
              index[doc] = val;
            } else {
              // value won't fit... move integer into byte[]
              for (int j=0; j<ipos; j++) {
                tempArr[j] = (byte)val;
                val >>>=8;
              }
              // point at the end index in the byte[]
              index[doc] = (endPos<<8) | 1;
              bytes[doc] = tempArr;
              tempArr = new byte[12];
            }

          }

        }

      }

      te.next();
     }
 
    numTermsInField = te.getTermNumber();
    te.close();

     // free space if outrageously wasteful (tradeoff memory/cpu) 

     if ((maxTermCounts.length - numTermsInField) > 1024) { // too much waste!
       int[] newMaxTermCounts = new int[numTermsInField];
       System.arraycopy(maxTermCounts, 0, newMaxTermCounts, 0, numTermsInField);
       maxTermCounts = newMaxTermCounts;
   }

    long midPoint = System.currentTimeMillis();

    if (termInstances == 0) {
      // we didn't invert anything
      // lower memory consumption.
      index = this.index = null;
      tnums = null;
    } else {

      //
      // transform intermediate form into the final form, building a single byte[]
      // at a time, and releasing the intermediate byte[]s as we go to avoid
      // increasing the memory footprint.
      //
      for (int pass = 0; pass<256; pass++) {
        byte[] target = tnums[pass];
        int pos=0;  // end in target;
        if (target != null) {
          pos = target.length;
        } else {
          target = new byte[4096];
        }

        // loop over documents, 0x00ppxxxx, 0x01ppxxxx, 0x02ppxxxx
        // where pp is the pass (which array we are building), and xx is all values.
        // each pass shares the same byte[] for termNumber lists.
        for (int docbase = pass<<16; docbase<maxDoc; docbase+=(1<<24)) {
          int lim = Math.min(docbase + (1<<16), maxDoc);
          for (int doc=docbase; doc<lim; doc++) {
            int val = index[doc];
            if ((val&0xff) == 1) {
              int len = val >>> 8;
              index[doc] = (pos<<8)|1; // change index to point to start of array
              if ((pos & 0xff000000) != 0) {
                // we only have 24 bits for the array index
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Too many values for UnInvertedField faceting on field "+field);
              }
              byte[] arr = bytes[doc];
              bytes[doc] = null;        // IMPORTANT: allow GC to avoid OOM
              if (target.length <= pos + len) {
                int newlen = target.length;
                /*** we don't have to worry about the array getting too large
                 * since the "pos" param will overflow first (only 24 bits available)
                if ((newlen<<1) <= 0) {
                  // overflow...
                  newlen = Integer.MAX_VALUE;
                  if (newlen <= pos + len) {
                    throw new SolrException(400,"Too many terms to uninvert field!");
                  }
                } else {
                  while (newlen <= pos + len) newlen<<=1;  // doubling strategy
                }
                ****/
                while (newlen <= pos + len) newlen<<=1;  // doubling strategy                 
                byte[] newtarget = new byte[newlen];
                System.arraycopy(target, 0, newtarget, 0, pos);
                target = newtarget;
              }
              System.arraycopy(arr, 0, target, pos, len);
              pos += len + 1;  // skip single byte at end and leave it 0 for terminator
            }
          }
        }

        // shrink array
        if (pos < target.length) {
          byte[] newtarget = new byte[pos];
          System.arraycopy(target, 0, newtarget, 0, pos);
          target = newtarget;
          if (target.length > (1<<24)*.9) {
            SolrCore.log.warn("Approaching too many values for UnInvertedField faceting on field '"+field+"' : bucket size=" + target.length);
          }
        }
        
        tnums[pass] = target;

        if ((pass << 16) > maxDoc)
          break;
      }
     }
 
    long endTime = System.currentTimeMillis();

    total_time = (int)(endTime-startTime);
    phase1_time = (int)(midPoint-startTime);

     SolrCore.log.info("UnInverted multi-valued field " + toString());
    //System.out.println("CREATED: " + toString() + " ti.index=" + ti.index);
   }
 


  public int getNumTerms() {
    return numTermsInField;
  }
 
   public NamedList<Integer> getCounts(SolrIndexSearcher searcher, DocSet baseDocs, int offset, int limit, Integer mincount, boolean missing, String sort, String prefix) throws IOException {
     use.incrementAndGet();
@@ -468,6 +206,7 @@ public class UnInvertedField {
     int baseSize = docs.size();
     int maxDoc = searcher.maxDoc();
 
    //System.out.println("GET COUNTS field=" + field + " baseSize=" + baseSize + " minCount=" + mincount + " maxDoc=" + maxDoc + " numTermsInField=" + numTermsInField);
     if (baseSize >= mincount) {
 
       final int[] index = this.index;
@@ -481,14 +220,20 @@ public class UnInvertedField {
       int startTerm = 0;
       int endTerm = numTermsInField;  // one past the end
 
      NumberedTermsEnum te = ti.getEnumerator(searcher.getIndexReader());
      TermsEnum te = getOrdTermsEnum(searcher.getIndexReader());
       if (prefix != null && prefix.length() > 0) {
         BytesRef prefixBr = new BytesRef(prefix);
        te.skipTo(prefixBr);
        startTerm = te.getTermNumber();
        if (te.seek(prefixBr, true) == TermsEnum.SeekStatus.END) {
          startTerm = numTermsInField;
        } else {
          startTerm = (int) te.ord();
        }
         prefixBr.append(ByteUtils.bigTerm);
        te.skipTo(prefixBr);
        endTerm = te.getTermNumber();
        if (te.seek(prefixBr, true) == TermsEnum.SeekStatus.END) {
          endTerm = numTermsInField;
        } else {
          endTerm = (int) te.ord();
        }
       }
 
       /***********
@@ -514,13 +259,18 @@ public class UnInvertedField {
         docs = new BitDocSet(bs, maxDoc - baseSize);
         // simply negating will mean that we have deleted docs in the set.
         // that should be OK, as their entries in our table should be empty.
        //System.out.println("  NEG");
       }
 
       // For the biggest terms, do straight set intersections
       for (TopTerm tt : bigTerms.values()) {
        //System.out.println("  do big termNum=" + tt.termNum + " term=" + tt.term.utf8ToString());
         // TODO: counts could be deferred if sorted==false
         if (tt.termNum >= startTerm && tt.termNum < endTerm) {
          counts[tt.termNum] = searcher.numDocs(new TermQuery(new Term(ti.field, tt.term)), docs);
          counts[tt.termNum] = searcher.numDocs(new TermQuery(new Term(field, tt.term)), docs);
          //System.out.println("    count=" + counts[tt.termNum]);
        } else {
          //System.out.println("SKIP term=" + tt.termNum);
         }
       }
 
@@ -537,9 +287,11 @@ public class UnInvertedField {
         DocIterator iter = docs.iterator();
         while (iter.hasNext()) {
           int doc = iter.nextDoc();
          //System.out.println("iter doc=" + doc);
           int code = index[doc];
 
           if ((code & 0xff)==1) {
            //System.out.println("  ptr");
             int pos = code>>>8;
             int whichArray = (doc >>> 16) & 0xff;
             byte[] arr = tnums[whichArray];
@@ -553,9 +305,11 @@ public class UnInvertedField {
               }
               if (delta == 0) break;
               tnum += delta - TNUM_OFFSET;
              //System.out.println("    tnum=" + tnum);
               counts[tnum]++;
             }
           } else {
            //System.out.println("  inlined");
             int tnum = 0;
             int delta = 0;
             for (;;) {
@@ -563,6 +317,7 @@ public class UnInvertedField {
               if ((code & 0x80)==0) {
                 if (delta==0) break;
                 tnum += delta - TNUM_OFFSET;
                //System.out.println("    tnum=" + tnum);
                 counts[tnum]++;
                 delta = 0;
               }
@@ -583,6 +338,7 @@ public class UnInvertedField {
         LongPriorityQueue queue = new LongPriorityQueue(Math.min(maxsize,1000), maxsize, Long.MIN_VALUE);
 
         int min=mincount-1;  // the smallest value in the top 'N' values
        //System.out.println("START=" + startTerm + " END=" + endTerm);
         for (int i=startTerm; i<endTerm; i++) {
           int c = doNegative ? maxTermCounts[i] - counts[i] : counts[i];
           if (c>min) {
@@ -641,11 +397,14 @@ public class UnInvertedField {
           }
         });
 
        // convert the term numbers to term values and set as the label
        // convert the term numbers to term values and set
        // as the label
        //System.out.println("sortStart=" + sortedIdxStart + " end=" + sortedIdxEnd);
         for (int i=sortedIdxStart; i<sortedIdxEnd; i++) {
           int idx = indirect[i];
           int tnum = (int)sorted[idx];
           String label = getReadableValue(getTermValue(te, tnum), ft, spare);
          //System.out.println("  label=" + label);
           res.setName(idx - sortedIdxStart, label);
         }
 
@@ -668,8 +427,6 @@ public class UnInvertedField {
           res.add(label, c);
         }
       }

      te.close();
     }
 
 
@@ -678,6 +435,8 @@ public class UnInvertedField {
       res.add(null, SimpleFacets.getFieldMissingCount(searcher, baseDocs, field));
     }
 
    //System.out.println("  res=" + res);

     return res;
   }
 
@@ -731,8 +490,7 @@ public class UnInvertedField {
     final int[] index = this.index;
     final int[] counts = new int[numTermsInField];//keep track of the number of times we see each word in the field for all the documents in the docset
 
    NumberedTermsEnum te = ti.getEnumerator(searcher.getIndexReader());

    TermsEnum te = getOrdTermsEnum(searcher.getIndexReader());
 
     boolean doNegative = false;
     if (finfo.length == 0) {
@@ -755,7 +513,7 @@ public class UnInvertedField {
     for (TopTerm tt : bigTerms.values()) {
       // TODO: counts could be deferred if sorted==false
       if (tt.termNum >= 0 && tt.termNum < numTermsInField) {
        final Term t = new Term(ti.field, tt.term);
        final Term t = new Term(field, tt.term);
         if (finfo.length == 0) {
           counts[tt.termNum] = searcher.numDocs(new TermQuery(t), docs);
         } else {
@@ -836,7 +594,6 @@ public class UnInvertedField {
         f.accumulateTermNum(i, value);
       }
     }
    te.close();
 
     int c = missing.size();
     allstats.addMissing(c);
@@ -870,23 +627,26 @@ public class UnInvertedField {
   }
 
   /** may return a reused BytesRef */
  BytesRef getTermValue(NumberedTermsEnum te, int termNum) throws IOException {
  BytesRef getTermValue(TermsEnum te, int termNum) throws IOException {
    //System.out.println("getTermValue termNum=" + termNum + " this=" + this + " numTerms=" + numTermsInField);
     if (bigTerms.size() > 0) {
       // see if the term is one of our big terms.
       TopTerm tt = bigTerms.get(termNum);
       if (tt != null) {
        //System.out.println("  return big " + tt.term);
         return tt.term;
       }
     }
 
    return te.skipTo(termNum);
    return lookupTerm(te, termNum);
   }
 
   @Override
   public String toString() {
    final long indexSize = indexedTermsArray == null ? 0 : (8+8+8+8+(indexedTermsArray.length<<3)+sizeOfIndexedStrings); // assume 8 byte references?
     return "{field=" + field
             + ",memSize="+memSize()
            + ",tindexSize="+ti.memSize()
            + ",tindexSize="+indexSize
             + ",time="+total_time
             + ",phase1="+phase1_time
             + ",nTerms="+numTermsInField
@@ -896,7 +656,6 @@ public class UnInvertedField {
             + "}";
   }
 

   //////////////////////////////////////////////////////////////////
   //////////////////////////// caching /////////////////////////////
   //////////////////////////////////////////////////////////////////
@@ -920,287 +679,3 @@ public class UnInvertedField {
     return uif;
   }
 }


// How to share TermDocs (int[] score[])???
// Hot to share TermPositions?
/***
class TermEnumListener {
  void doTerm(Term t) {
  }
  void done() {
  }
}
***/


class NumberedTermsEnum extends TermsEnum {
  protected final IndexReader reader;
  protected final TermIndex tindex;
  protected TermsEnum tenum;
  protected int pos=-1;
  protected BytesRef termText;
  protected DocsEnum docsEnum;
  protected Bits deletedDocs;


  NumberedTermsEnum(IndexReader reader, TermIndex tindex) throws IOException {
    this.reader = reader;
    this.tindex = tindex;
  }


  NumberedTermsEnum(IndexReader reader, TermIndex tindex, BytesRef termValue, int pos) throws IOException {
    this.reader = reader;
    this.tindex = tindex;
    this.pos = pos;
    Terms terms = MultiFields.getTerms(reader, tindex.field);
    deletedDocs = MultiFields.getDeletedDocs(reader);
    if (terms != null) {
      tenum = terms.iterator();
      tenum.seek(termValue);
      setTerm();
    }
  }

  @Override
  public Comparator<BytesRef> getComparator() throws IOException {
    return tenum.getComparator();
  }

  public DocsEnum getDocsEnum() throws IOException {
    docsEnum = tenum.docs(deletedDocs, docsEnum);
    return docsEnum;
  }

  protected BytesRef setTerm() throws IOException {
    termText = tenum.term();
    if (tindex.prefix != null && !termText.startsWith(tindex.prefix)) {
      termText = null;
    }
    return termText;
  }

  @Override
  public BytesRef next() throws IOException {
    pos++;
    if (tenum.next() == null) {
      termText = null;
      return null;
    }
    return setTerm();  // this is extra work if we know we are in bounds...
  }

  @Override
  public BytesRef term() {
    return termText;
  }

  @Override
  public int docFreq() throws IOException {
    return tenum.docFreq();
  }

  @Override
  public long totalTermFreq() throws IOException {
    return tenum.totalTermFreq();
  }

  public BytesRef skipTo(BytesRef target) throws IOException {

    // already here
    if (termText != null && termText.equals(target)) return termText;

    if (tenum == null) {
      return null;
    }

    int startIdx = Arrays.binarySearch(tindex.index,target);

    if (startIdx >= 0) {
      // we hit the term exactly... lucky us!
      TermsEnum.SeekStatus seekStatus = tenum.seek(target);
      assert seekStatus == TermsEnum.SeekStatus.FOUND;
      pos = startIdx << tindex.intervalBits;
      return setTerm();
    }

    // we didn't hit the term exactly
    startIdx=-startIdx-1;
    
    if (startIdx == 0) {
      // our target occurs *before* the first term
      TermsEnum.SeekStatus seekStatus = tenum.seek(target);
      assert seekStatus == TermsEnum.SeekStatus.NOT_FOUND;
      pos = 0;
      return setTerm();
    }

    // back up to the start of the block
    startIdx--;

    if ((pos >> tindex.intervalBits) == startIdx && termText != null && termText.compareTo(target)<=0) {
      // we are already in the right block and the current term is before the term we want,
      // so we don't need to seek.
    } else {
      // seek to the right block
      TermsEnum.SeekStatus seekStatus = tenum.seek(tindex.index[startIdx]);
      assert seekStatus == TermsEnum.SeekStatus.FOUND;
      pos = startIdx << tindex.intervalBits;
      setTerm();  // should be non-null since it's in the index
    }

    while (termText != null && termText.compareTo(target) < 0) {
      next();
    }

    return termText;
  }

  public BytesRef skipTo(int termNumber) throws IOException {
    int delta = termNumber - pos;
    if (delta < 0 || delta > tindex.interval || tenum==null) {
      int idx = termNumber >>> tindex.intervalBits;
      BytesRef base = tindex.index[idx];
      pos = idx << tindex.intervalBits;
      delta = termNumber - pos;
      TermsEnum.SeekStatus seekStatus = tenum.seek(base);
      assert seekStatus == TermsEnum.SeekStatus.FOUND;
    }
    while (--delta >= 0) {
      BytesRef br = tenum.next();
      if (br == null) {
        termText = null;
        return null;
      }
      ++pos;
    }
    return setTerm();
  }

  protected void close() throws IOException {
    // no-op, needed so the anon subclass that does indexing
    // can build its index
  }

  /** The current term number, starting at 0.
   * Only valid if the previous call to next() or skipTo() returned true.
   */
  public int getTermNumber() {
    return pos;
  }

  @Override
  public long ord() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SeekStatus seek(long ord) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DocsEnum docs(Bits skipDocs, DocsEnum reuse) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DocsAndPositionsEnum docsAndPositions(Bits skipDocs, DocsAndPositionsEnum reuse) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SeekStatus seek(BytesRef target, boolean useCache) {
    throw new UnsupportedOperationException();
  }
}


/**
 * Class to save memory by only storing every nth term (for random access), while
 * numbering the terms, allowing them to be retrieved later by number.
 * This is only valid when used with the IndexReader it was created with.
 * The IndexReader is not actually stored to facilitate caching by using it as a key in
 * a weak hash map.
 */
class TermIndex {
  final static int intervalBits = 7;  // decrease to a low number like 2 for testing
  final static int intervalMask = 0xffffffff >>> (32-intervalBits);
  final static int interval = 1 << intervalBits;

  final String field;
  final BytesRef prefix;
  BytesRef[] index;
  int nTerms;
  long sizeOfStrings;

  TermIndex(String field) {
    this(field, null);
  }

  TermIndex(String field, String prefix) {
    this.field = field;
    this.prefix = prefix == null ? null : new BytesRef(prefix);
  }

  NumberedTermsEnum getEnumerator(IndexReader reader, int termNumber) throws IOException {
    NumberedTermsEnum te = new NumberedTermsEnum(reader, this);
    te.skipTo(termNumber);
    return te;
  }

  /* The first time an enumerator is requested, it should be used
     with next() to fully traverse all of the terms so the index
     will be built.
   */
  NumberedTermsEnum getEnumerator(IndexReader reader) throws IOException {
    if (index==null) return new NumberedTermsEnum(reader,this, prefix==null?new BytesRef():prefix, 0) {
      ArrayList<BytesRef> lst;
      PagedBytes bytes;

      @Override
      protected BytesRef setTerm() throws IOException {
        BytesRef br = super.setTerm();
        if (br != null && (pos & intervalMask)==0) {
          sizeOfStrings += br.length;
          if (lst==null) {
            lst = new ArrayList<BytesRef>();
            bytes = new PagedBytes(15);
          }
          BytesRef out = new BytesRef();
          bytes.copy(br, out);
          lst.add(out);
        }
        return br;
      }

      @Override
      public BytesRef skipTo(int termNumber) throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public void close() throws IOException {
        nTerms=pos;
        super.close();
        index = lst!=null ? lst.toArray(new BytesRef[lst.size()]) : new BytesRef[0];
      }
    };
    else return new NumberedTermsEnum(reader,this,new BytesRef(),0);
  }


  /**
   * Returns the approximate amount of memory taken by this TermIndex.
   * This is only an approximation and doesn't take into account java object overhead.
   *
   * @return
   * the approximate memory consumption in bytes
   */
  public long memSize() {
    // assume 8 byte references?
    return 8+8+8+8+(index.length<<3)+sizeOfStrings;
  }
}

diff --git a/solr/src/test/org/apache/solr/request/TestFaceting.java b/solr/src/test/org/apache/solr/request/TestFaceting.java
index b9e1a5f8a9e..140de82e14a 100755
-- a/solr/src/test/org/apache/solr/request/TestFaceting.java
++ b/solr/src/test/org/apache/solr/request/TestFaceting.java
@@ -17,14 +17,17 @@
 
 package org.apache.solr.request;
 
import java.util.Locale;
import java.util.Random;

import org.apache.lucene.index.DocTermOrds;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.SolrTestCaseJ4;
 import org.junit.After;
 import org.junit.BeforeClass;
 import org.junit.Test;
import java.util.Locale;
import java.util.Random;
 
 /**
  * @version $Id$
@@ -62,43 +65,47 @@ public class TestFaceting extends SolrTestCaseJ4 {
   }
 
   void doTermEnum(int size) throws Exception {
    //System.out.println("doTermEnum size=" + size);
     close();
     createIndex(size);
     req = lrf.makeRequest("q","*:*");
 
    TermIndex ti = new TermIndex(proto.field());
    NumberedTermsEnum te = ti.getEnumerator(req.getSearcher().getIndexReader());
    UnInvertedField uif = new UnInvertedField(proto.field(), req.getSearcher());
 
    // iterate through first
    while(te.term() != null) te.next();
    assertEquals(size, te.getTermNumber());
    te.close();
    assertEquals(size, uif.getNumTerms());
 
    te = ti.getEnumerator(req.getSearcher().getIndexReader());
    TermsEnum te = uif.getOrdTermsEnum(req.getSearcher().getIndexReader());
    assertEquals(size == 0, te == null);
 
     Random r = new Random(size);
     // test seeking by term string
     for (int i=0; i<size*2+10; i++) {
       int rnum = r.nextInt(size+2);
       String s = t(rnum);
      BytesRef br = te.skipTo(new BytesRef(s));
      //System.out.println("s=" + s);
      final BytesRef br;
      if (te == null) {
        br = null;
      } else {
        TermsEnum.SeekStatus status = te.seek(new BytesRef(s));
        if (status == TermsEnum.SeekStatus.END) {
          br = null;
        } else {
          br = te.term();
        }
      }
       assertEquals(br != null, rnum < size);
       if (rnum < size) {
        assertEquals(rnum, te.pos);
        assertEquals(rnum, (int) te.ord());
         assertEquals(s, te.term().utf8ToString());
      } else {
        assertEquals(null, te.term());
        assertEquals(size, te.getTermNumber());
       }
     }
 
     // test seeking before term
    assertEquals(size>0, te.skipTo(new BytesRef("000")) != null);
    assertEquals(0, te.getTermNumber());
     if (size>0) {
      assertEquals(size>0, te.seek(new BytesRef("000"), true) != TermsEnum.SeekStatus.END);
      assertEquals(0, te.ord());
       assertEquals(t(0), te.term().utf8ToString());
    } else {
      assertEquals(null, te.term());
     }
 
     if (size>0) {
@@ -106,9 +113,10 @@ public class TestFaceting extends SolrTestCaseJ4 {
       for (int i=0; i<size*2+10; i++) {
         int rnum = r.nextInt(size);
         String s = t(rnum);
        BytesRef br = te.skipTo(rnum);
        assertTrue(te.seek((long) rnum) != TermsEnum.SeekStatus.END);
        BytesRef br = te.term();
         assertNotNull(br);
        assertEquals(rnum, te.pos);
        assertEquals(rnum, (int) te.ord());
         assertEquals(s, te.term().utf8ToString());
       }
     }
@@ -118,11 +126,12 @@ public class TestFaceting extends SolrTestCaseJ4 {
   public void testTermEnum() throws Exception {
     doTermEnum(0);
     doTermEnum(1);
    doTermEnum(TermIndex.interval - 1);  // test boundaries around the block size
    doTermEnum(TermIndex.interval);
    doTermEnum(TermIndex.interval + 1);
    doTermEnum(TermIndex.interval * 2 + 2);    
    // doTermEnum(TermIndex.interval * 3 + 3);    
    final int DEFAULT_INDEX_INTERVAL = 1 << DocTermOrds.DEFAULT_INDEX_INTERVAL_BITS;
    doTermEnum(DEFAULT_INDEX_INTERVAL - 1);  // test boundaries around the block size
    doTermEnum(DEFAULT_INDEX_INTERVAL);
    doTermEnum(DEFAULT_INDEX_INTERVAL + 1);
    doTermEnum(DEFAULT_INDEX_INTERVAL * 2 + 2);    
    // doTermEnum(DEFAULT_INDEX_INTERVAL * 3 + 3);    
   }
 
   @Test
- 
2.19.1.windows.1

