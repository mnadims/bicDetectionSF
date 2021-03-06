From e92a91ed185f0f9be9b136ecade6d5a43f1ae00f Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Thu, 24 Jun 2010 09:17:52 +0000
Subject: [PATCH] LUCENE-2410: ~2.5X speed up for exact (slop=0) PhraseQuery

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@957465 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   5 +
 .../lucene/search/ExactPhraseScorer.java      | 332 ++++++++++++++++--
 .../lucene/search/MultiPhraseQuery.java       |  81 ++++-
 .../org/apache/lucene/search/PhraseQuery.java |  57 ++-
 .../apache/lucene/search/PhraseScorer.java    |   6 +-
 .../lucene/search/SloppyPhraseScorer.java     |   8 +-
 .../lucene/search/JustCompileSearch.java      |   5 +-
 .../lucene/search/TestMultiPhraseQuery.java   |  12 +
 .../apache/lucene/search/TestPhraseQuery.java | 113 +++++-
 9 files changed, 547 insertions(+), 72 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index d6425791efd..ca458187d56 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -183,6 +183,11 @@ New features
 
 * LUCENE-2489: Added PerFieldCodecWrapper (in oal.index.codecs) which
   lets you set the Codec per field (Mike McCandless)

Optimizations

* LUCENE-2410: ~2.5X speedup on exact (slop=0) PhraseQuery matching.
  (Mike McCandless)
   
 ======================= Lucene 3.x (not yet released) =======================
 
diff --git a/lucene/src/java/org/apache/lucene/search/ExactPhraseScorer.java b/lucene/src/java/org/apache/lucene/search/ExactPhraseScorer.java
index 55825b48ad2..14ab22de1af 100644
-- a/lucene/src/java/org/apache/lucene/search/ExactPhraseScorer.java
++ b/lucene/src/java/org/apache/lucene/search/ExactPhraseScorer.java
@@ -18,39 +18,317 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.Arrays;

 import org.apache.lucene.index.*;
 
final class ExactPhraseScorer extends PhraseScorer {
final class ExactPhraseScorer extends Scorer {
  private final Weight weight;
  private final byte[] norms;
  private final float value;

  private static final int SCORE_CACHE_SIZE = 32;
  private final float[] scoreCache = new float[SCORE_CACHE_SIZE];

  private final int endMinus1;

  private final static int CHUNK = 4096;

  private int gen;
  private final int[] counts = new int[CHUNK];
  private final int[] gens = new int[CHUNK];

  boolean noDocs;
 
  ExactPhraseScorer(Weight weight, DocsAndPositionsEnum[] postings, int[] offsets,
      Similarity similarity, byte[] norms) {
    super(weight, postings, offsets, similarity, norms);
  private final static class ChunkState {
    final DocsAndPositionsEnum posEnum;
    final int offset;
    final boolean useAdvance;
    int posUpto;
    int posLimit;
    int pos;
    int lastPos;

    public ChunkState(DocsAndPositionsEnum posEnum, int offset, boolean useAdvance) {
      this.posEnum = posEnum;
      this.offset = offset;
      this.useAdvance = useAdvance;
    }
  }

  private final ChunkState[] chunkStates;

  private int docID = -1;
  private int freq;

  ExactPhraseScorer(Weight weight, PhraseQuery.PostingsAndFreq[] postings,
                    Similarity similarity, byte[] norms) throws IOException {
    super(similarity);
    this.weight = weight;
    this.norms = norms;
    this.value = weight.getValue();

    chunkStates = new ChunkState[postings.length];

    endMinus1 = postings.length-1;

    for(int i=0;i<postings.length;i++) {

      // Coarse optimization: advance(target) is fairly
      // costly, so, if the relative freq of the 2nd
      // rarest term is not that much (> 1/5th) rarer than
      // the first term, then we just use .nextDoc() when
      // ANDing.  This buys ~15% gain for phrases where
      // freq of rarest 2 terms is close:
      final boolean useAdvance = postings[i].docFreq > 5*postings[0].docFreq;
      chunkStates[i] = new ChunkState(postings[i].postings, -postings[i].position, useAdvance);
      if (i > 0 && postings[i].postings.nextDoc() == DocsEnum.NO_MORE_DOCS) {
        noDocs = true;
        return;
      }
    }

    for (int i = 0; i < SCORE_CACHE_SIZE; i++) {
      scoreCache[i] = getSimilarity().tf((float) i) * value;
    }
   }
 
   @Override
  protected final float phraseFreq() throws IOException {
    // sort list with pq
    pq.clear();
    for (PhrasePositions pp = first; pp != null; pp = pp.next) {
      pp.firstPosition();
      pq.add(pp);				  // build pq from list
    }
    pqToList();					  // rebuild list from pq

    // for counting how many times the exact phrase is found in current document,
    // just count how many times all PhrasePosition's have exactly the same position.   
    int freq = 0;
    do {					  // find position w/ all terms
      while (first.position < last.position) {	  // scan forward in first
        do {
          if (!first.nextPosition())
            return freq;
        } while (first.position < last.position);
        firstToLast();
      }
      freq++;					  // all equal: a match
    } while (last.nextPosition());
  
  public int nextDoc() throws IOException {
    while(true) {

      // first (rarest) term
      final int doc = chunkStates[0].posEnum.nextDoc();
      if (doc == DocsEnum.NO_MORE_DOCS) {
        docID = doc;
        return doc;
      }

      // not-first terms
      int i = 1;
      while(i < chunkStates.length) {
        final ChunkState cs = chunkStates[i];
        int doc2 = cs.posEnum.docID();
        if (cs.useAdvance) {
          if (doc2 < doc) {
            doc2 = cs.posEnum.advance(doc);
          }
        } else {
          int iter = 0;
          while(doc2 < doc) {
            // safety net -- fallback to .advance if we've
            // done too many .nextDocs
            if (++iter == 50) {
              doc2 = cs.posEnum.advance(doc);
              break;
            } else {
              doc2 = cs.posEnum.nextDoc();
            }
          }
        }
        if (doc2 > doc) {
          break;
        }
        i++;
      }

      if (i == chunkStates.length) {
        // this doc has all the terms -- now test whether
        // phrase occurs
        docID = doc;

        freq = phraseFreq();
        if (freq != 0) {
          return docID;
        }
      }
    }
  }

  @Override
  public int advance(int target) throws IOException {

    // first term
    int doc = chunkStates[0].posEnum.advance(target);
    if (doc == DocsEnum.NO_MORE_DOCS) {
      docID = DocsEnum.NO_MORE_DOCS;
      return doc;
    }

    while(true) {
      
      // not-first terms
      int i = 1;
      while(i < chunkStates.length) {
        int doc2 = chunkStates[i].posEnum.docID();
        if (doc2 < doc) {
          doc2 = chunkStates[i].posEnum.advance(doc);
        }
        if (doc2 > doc) {
          break;
        }
        i++;
      }

      if (i == chunkStates.length) {
        // this doc has all the terms -- now test whether
        // phrase occurs
        docID = doc;
        freq = phraseFreq();
        if (freq != 0) {
          return docID;
        }
      }

      doc = chunkStates[0].posEnum.nextDoc();
      if (doc == DocsEnum.NO_MORE_DOCS) {
        docID = doc;
        return doc;
      }
    }
  }

  @Override
  public String toString() {
    return "ExactPhraseScorer(" + weight + ")";
  }

  // used by MultiPhraseQuery
  float currentFreq() {
    return freq;
  }

  @Override
  public int docID() {
    return docID;
  }

  @Override
  public float score() throws IOException {
    final float raw; // raw score
    if (freq < SCORE_CACHE_SIZE) {
      raw = scoreCache[freq];
    } else {
      raw = getSimilarity().tf((float) freq) * value;
    }
    return norms == null ? raw : raw * getSimilarity().decodeNormValue(norms[docID]); // normalize
  }

  private int phraseFreq() throws IOException {

    freq = 0;

    // init chunks
    for(int i=0;i<chunkStates.length;i++) {
      final ChunkState cs = chunkStates[i];
      cs.posLimit = cs.posEnum.freq();
      cs.pos = cs.offset + cs.posEnum.nextPosition();
      cs.posUpto = 1;
      cs.lastPos = -1;
    }

    int chunkStart = 0;
    int chunkEnd = CHUNK;

    // process chunk by chunk
    boolean end = false;

    // TODO: we could fold in chunkStart into offset and
    // save one subtract per pos incr

    while(!end) {

      gen++;

      if (gen == 0) {
        // wraparound
        Arrays.fill(gens, 0);
        gen++;
      }

      // first term
      {
        final ChunkState cs = chunkStates[0];
        while(cs.pos < chunkEnd) {
          if (cs.pos > cs.lastPos) {
            cs.lastPos = cs.pos;
            final int posIndex = cs.pos - chunkStart;
            counts[posIndex] = 1;
            assert gens[posIndex] != gen;
            gens[posIndex] = gen;
          }

          if (cs.posUpto == cs.posLimit) {
            end = true;
            break;
          }
          cs.posUpto++;
          cs.pos = cs.offset + cs.posEnum.nextPosition();
        }
      }

      // middle terms
      boolean any = true;
      for(int t=1;t<endMinus1;t++) {
        final ChunkState cs = chunkStates[t];
        any = false;
        while(cs.pos < chunkEnd) {
          if (cs.pos > cs.lastPos) {
            cs.lastPos = cs.pos;
            final int posIndex = cs.pos - chunkStart;
            if (posIndex >= 0 && gens[posIndex] == gen && counts[posIndex] == t) {
              // viable
              counts[posIndex]++;
              any = true;
            }
          }

          if (cs.posUpto == cs.posLimit) {
            end = true;
            break;
          }
          cs.posUpto++;
          cs.pos = cs.offset + cs.posEnum.nextPosition();
        }

        if (!any) {
          break;
        }
      }

      if (!any) {
        // petered out for this chunk
        chunkStart += CHUNK;
        chunkEnd += CHUNK;
        continue;
      }

      // last term

      {
        final ChunkState cs = chunkStates[endMinus1];
        while(cs.pos < chunkEnd) {
          if (cs.pos > cs.lastPos) {
            cs.lastPos = cs.pos;
            final int posIndex = cs.pos - chunkStart;
            if (posIndex >= 0 && gens[posIndex] == gen && counts[posIndex] == endMinus1) {
              freq++;
            }
          }

          if (cs.posUpto == cs.posLimit) {
            end = true;
            break;
          }
          cs.posUpto++;
          cs.pos = cs.offset + cs.posEnum.nextPosition();
        }
      }

      chunkStart += CHUNK;
      chunkEnd += CHUNK;
    }

     return freq;
   }
 }
diff --git a/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java b/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
index d8127fc3eb3..cfd4eefb268 100644
-- a/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
++ b/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
@@ -171,32 +171,64 @@ public class MultiPhraseQuery extends Query {
       if (termArrays.size() == 0)                  // optimize zero-term case
         return null;
 
      DocsAndPositionsEnum[] postings = new DocsAndPositionsEnum[termArrays.size()];
      for (int i=0; i<postings.length; i++) {
      final Bits delDocs = MultiFields.getDeletedDocs(reader);
      
      PhraseQuery.PostingsAndFreq[] postingsFreqs = new PhraseQuery.PostingsAndFreq[termArrays.size()];

      for (int i=0; i<postingsFreqs.length; i++) {
         Term[] terms = termArrays.get(i);
 
         final DocsAndPositionsEnum postingsEnum;
        int docFreq;

         if (terms.length > 1) {
           postingsEnum = new UnionDocsAndPositionsEnum(reader, terms);

          // coarse -- this overcounts since a given doc can
          // have more than one terms:
          docFreq = 0;
          for(int j=0;j<terms.length;j++) {
            docFreq += reader.docFreq(terms[i]);
          }
         } else {
          postingsEnum = reader.termPositionsEnum(MultiFields.getDeletedDocs(reader),
          final BytesRef text = new BytesRef(terms[0].text());
          postingsEnum = reader.termPositionsEnum(delDocs,
                                                   terms[0].field(),
                                                  new BytesRef(terms[0].text()));
                                                  text);

          if (postingsEnum == null) {
            if (MultiFields.getTermDocsEnum(reader, delDocs, terms[0].field(), text) != null) {
              // term does exist, but has no positions
              throw new IllegalStateException("field \"" + terms[0].field() + "\" was indexed with Field.omitTermFreqAndPositions=true; cannot run PhraseQuery (term=" + terms[0].text() + ")");
            } else {
              // term does not exist
              return null;
            }
          }

          docFreq = reader.docFreq(terms[0].field(), text);
         }
 
        if (postingsEnum == null) {
          return null;
        }
        postingsFreqs[i] = new PhraseQuery.PostingsAndFreq(postingsEnum, docFreq, positions.get(i).intValue());
      }
 
        postings[i] = postingsEnum;
      // sort by increasing docFreq order
      if (slop == 0) {
        Arrays.sort(postingsFreqs);
       }
 
      if (slop == 0)
        return new ExactPhraseScorer(this, postings, getPositions(), similarity,
                                     reader.norms(field));
      else
        return new SloppyPhraseScorer(this, postings, getPositions(), similarity,
      if (slop == 0) {
        ExactPhraseScorer s = new ExactPhraseScorer(this, postingsFreqs, similarity,
                                                    reader.norms(field));
        if (s.noDocs) {
          return null;
        } else {
          return s;
        }
      } else {
        return new SloppyPhraseScorer(this, postingsFreqs, similarity,
                                       slop, reader.norms(field));
      }
     }
 
     @Override
@@ -231,13 +263,24 @@ public class MultiPhraseQuery extends Query {
       fieldExpl.setDescription("fieldWeight("+getQuery()+" in "+doc+
                                "), product of:");
 
      PhraseScorer scorer = (PhraseScorer) scorer(reader, true, false);
      Scorer scorer = (Scorer) scorer(reader, true, false);
       if (scorer == null) {
         return new Explanation(0.0f, "no matching docs");
       }

       Explanation tfExplanation = new Explanation();
       int d = scorer.advance(doc);
      float phraseFreq = (d == doc) ? scorer.currentFreq() : 0.0f;
      float phraseFreq;
      if (d == doc) {
        if (slop == 0) {
          phraseFreq = ((ExactPhraseScorer) scorer).currentFreq();
        } else {
          phraseFreq = ((SloppyPhraseScorer) scorer).currentFreq();
        }
      } else {
        phraseFreq = 0.0f;
      }

       tfExplanation.setValue(similarity.tf(phraseFreq));
       tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq + ")");
       fieldExpl.addDetail(tfExplanation);
@@ -456,11 +499,17 @@ class UnionDocsAndPositionsEnum extends DocsAndPositionsEnum {
     List<DocsAndPositionsEnum> docsEnums = new LinkedList<DocsAndPositionsEnum>();
     final Bits delDocs = MultiFields.getDeletedDocs(indexReader);
     for (int i = 0; i < terms.length; i++) {
      final BytesRef text = new BytesRef(terms[i].text());
       DocsAndPositionsEnum postings = indexReader.termPositionsEnum(delDocs,
                                                                     terms[i].field(),
                                                                    new BytesRef(terms[i].text()));
                                                                    text);
       if (postings != null) {
         docsEnums.add(postings);
      } else {
        if (MultiFields.getTermDocsEnum(indexReader, delDocs, terms[i].field(), text) != null) {
          // term does exist, but has no positions
          throw new IllegalStateException("field \"" + terms[i].field() + "\" was indexed with Field.omitTermFreqAndPositions=true; cannot run PhraseQuery (term=" + terms[i].text() + ")");
        }
       }
     }
 
diff --git a/lucene/src/java/org/apache/lucene/search/PhraseQuery.java b/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
index 54c1d258144..f583174ba0a 100644
-- a/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
++ b/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search;
 import java.io.IOException;
 import java.util.Set;
 import java.util.ArrayList;
import java.util.Arrays;
 
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.BytesRef;
@@ -120,6 +121,22 @@ public class PhraseQuery extends Query {
       return super.rewrite(reader);
   }
 
  static class PostingsAndFreq implements Comparable<PostingsAndFreq> {
    final DocsAndPositionsEnum postings;
    final int docFreq;
    final int position;

    public PostingsAndFreq(DocsAndPositionsEnum postings, int docFreq, int position) {
      this.postings = postings;
      this.docFreq = docFreq;
      this.position = position;
    }

    public int compareTo(PostingsAndFreq other) {
      return docFreq - other.docFreq;
    }
  }

   private class PhraseWeight extends Weight {
     private final Similarity similarity;
     private float value;
@@ -163,7 +180,7 @@ public class PhraseQuery extends Query {
       if (terms.size() == 0)			  // optimize zero-term case
         return null;
 
      DocsAndPositionsEnum[] postings = new DocsAndPositionsEnum[terms.size()];
      PostingsAndFreq[] postingsFreqs = new PostingsAndFreq[terms.size()];
       final Bits delDocs = MultiFields.getDeletedDocs(reader);
       for (int i = 0; i < terms.size(); i++) {
         final Term t = terms.get(i);
@@ -183,17 +200,27 @@ public class PhraseQuery extends Query {
             return null;
           }
         }
        postings[i] = postingsEnum;
        postingsFreqs[i] = new PostingsAndFreq(postingsEnum, reader.docFreq(t.field(), text), positions.get(i).intValue());
      }

      // sort by increasing docFreq order
      if (slop == 0) {
        Arrays.sort(postingsFreqs);
       }
 
      if (slop == 0)				  // optimize exact case
        return new ExactPhraseScorer(this, postings, getPositions(), similarity,
                                     reader.norms(field));
      else
      if (slop == 0) {				  // optimize exact case
        ExactPhraseScorer s = new ExactPhraseScorer(this, postingsFreqs, similarity,
                                                    reader.norms(field));
        if (s.noDocs) {
          return null;
        } else {
          return s;
        }
      } else {
         return
          new SloppyPhraseScorer(this, postings, getPositions(), similarity, slop,
          new SloppyPhraseScorer(this, postingsFreqs, similarity, slop,
                                  reader.norms(field));

      }
     }
 
     @Override
@@ -244,13 +271,23 @@ public class PhraseQuery extends Query {
       fieldExpl.setDescription("fieldWeight("+field+":"+query+" in "+doc+
                                "), product of:");
 
      PhraseScorer scorer = (PhraseScorer) scorer(reader, true, false);
      Scorer scorer = (Scorer) scorer(reader, true, false);
       if (scorer == null) {
         return new Explanation(0.0f, "no matching docs");
       }
       Explanation tfExplanation = new Explanation();
       int d = scorer.advance(doc);
      float phraseFreq = (d == doc) ? scorer.currentFreq() : 0.0f;
      float phraseFreq;
      if (d == doc) {
        if (slop == 0) {
          phraseFreq = ((ExactPhraseScorer) scorer).currentFreq();
        } else {
          phraseFreq = ((SloppyPhraseScorer) scorer).currentFreq();
        }
      } else {
        phraseFreq = 0.0f;
      }

       tfExplanation.setValue(similarity.tf(phraseFreq));
       tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq + ")");
       
diff --git a/lucene/src/java/org/apache/lucene/search/PhraseScorer.java b/lucene/src/java/org/apache/lucene/search/PhraseScorer.java
index ab15ae926c3..4dc62cdea60 100644
-- a/lucene/src/java/org/apache/lucene/search/PhraseScorer.java
++ b/lucene/src/java/org/apache/lucene/search/PhraseScorer.java
@@ -19,8 +19,6 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.DocsAndPositionsEnum;

 /** Expert: Scoring functionality for phrase queries.
  * <br>A document is considered matching if it contains the phrase-query terms  
  * at "valid" positions. What "valid positions" are
@@ -43,7 +41,7 @@ abstract class PhraseScorer extends Scorer {
 
   private float freq; //phrase frequency in current doc as computed by phraseFreq().
 
  PhraseScorer(Weight weight, DocsAndPositionsEnum[] postings, int[] offsets,
  PhraseScorer(Weight weight, PhraseQuery.PostingsAndFreq[] postings,
       Similarity similarity, byte[] norms) {
     super(similarity);
     this.norms = norms;
@@ -56,7 +54,7 @@ abstract class PhraseScorer extends Scorer {
     // this allows to easily identify a matching (exact) phrase 
     // when all PhrasePositions have exactly the same position.
     for (int i = 0; i < postings.length; i++) {
      PhrasePositions pp = new PhrasePositions(postings[i], offsets[i]);
      PhrasePositions pp = new PhrasePositions(postings[i].postings, postings[i].position);
       if (last != null) {			  // add next to end of list
         last.next = pp;
       } else {
diff --git a/lucene/src/java/org/apache/lucene/search/SloppyPhraseScorer.java b/lucene/src/java/org/apache/lucene/search/SloppyPhraseScorer.java
index decf1c84e8c..42941214d6e 100644
-- a/lucene/src/java/org/apache/lucene/search/SloppyPhraseScorer.java
++ b/lucene/src/java/org/apache/lucene/search/SloppyPhraseScorer.java
@@ -17,8 +17,6 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.index.DocsAndPositionsEnum;

 import java.io.IOException;
 import java.util.HashMap;
 
@@ -28,9 +26,9 @@ final class SloppyPhraseScorer extends PhraseScorer {
     private PhrasePositions tmpPos[]; // for flipping repeating pps.
     private boolean checkedRepeats;
 
    SloppyPhraseScorer(Weight weight, DocsAndPositionsEnum[] postings, int[] offsets, Similarity similarity,
    SloppyPhraseScorer(Weight weight, PhraseQuery.PostingsAndFreq[] postings, Similarity similarity,
                        int slop, byte[] norms) {
        super(weight, postings, offsets, similarity, norms);
        super(weight, postings, similarity, norms);
         this.slop = slop;
     }
 
@@ -53,7 +51,7 @@ final class SloppyPhraseScorer extends PhraseScorer {
      * We may want to fix this in the future (currently not, for performance reasons).
      */
     @Override
    protected final float phraseFreq() throws IOException {
    protected float phraseFreq() throws IOException {
         int end = initPhrasePositions();
         
         float freq = 0.0f;
diff --git a/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java b/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
index e0cf8e75513..3186865b117 100644
-- a/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
++ b/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
@@ -24,7 +24,6 @@ import org.apache.lucene.document.FieldSelector;
 import org.apache.lucene.index.CorruptIndexException;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.DocsAndPositionsEnum;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.PriorityQueue;
 
@@ -300,9 +299,9 @@ final class JustCompileSearch {
 
   static final class JustCompilePhraseScorer extends PhraseScorer {
 
    JustCompilePhraseScorer(Weight weight, DocsAndPositionsEnum[] docs, int[] offsets,
    JustCompilePhraseScorer(Weight weight, PhraseQuery.PostingsAndFreq[] postings,
         Similarity similarity, byte[] norms) {
      super(weight, docs, offsets, similarity, norms);
      super(weight, postings, similarity, norms);
     }
 
     @Override
diff --git a/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java b/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java
index 33b4a0690e8..96b6142a43a 100644
-- a/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java
@@ -117,6 +117,10 @@ public class TestMultiPhraseQuery extends LuceneTestCase
         // test slop:
         query3.setSlop(1);
         result = searcher.search(query3, null, 1000).scoreDocs;

        // just make sure no exc:
        searcher.explain(query3, 0);

         assertEquals(3, result.length); // blueberry pizza, bluebird pizza, bluebird foobar pizza
 
         MultiPhraseQuery query4 = new MultiPhraseQuery();
@@ -169,6 +173,10 @@ public class TestMultiPhraseQuery extends LuceneTestCase
       ScoreDoc[] hits = searcher.search(q, null, 1000).scoreDocs;
 
       assertEquals("Wrong number of hits", 2, hits.length);

      // just make sure no exc:
      searcher.explain(q, 0);

       searcher.close();
       indexStore.close();
   }
@@ -211,6 +219,10 @@ public class TestMultiPhraseQuery extends LuceneTestCase
     q.add(new Term("body", "a"));
     q.add(new Term[] { new Term("body", "nope"), new Term("body", "nope") });
     assertEquals("Wrong number of hits", 0, searcher.search(q, null, 1).totalHits);

    // just make sure no exc:
    searcher.explain(q, 0);

     searcher.close();
     indexStore.close();
   }
diff --git a/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java b/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
index 6c470fd0e94..8d1cc5dcfd3 100644
-- a/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
@@ -19,18 +19,21 @@ package org.apache.lucene.search;
 
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.tokenattributes.*;
 import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.*;
 import org.apache.lucene.index.IndexWriterConfig.OpenMode;
 import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.*;
 import org.apache.lucene.util.Version;
import org.apache.lucene.util._TestUtil;
 
 import java.io.IOException;
 import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
 
 /**
  * Tests {@link PhraseQuery}.
@@ -331,11 +334,11 @@ public class TestPhraseQuery extends LuceneTestCase {
     writer.addDocument(doc);
     
     Document doc2 = new Document();
    doc2.add(new Field("field", "foo firstname xxx lastname foo", Field.Store.YES, Field.Index.ANALYZED));
    doc2.add(new Field("field", "foo firstname zzz lastname foo", Field.Store.YES, Field.Index.ANALYZED));
     writer.addDocument(doc2);
     
     Document doc3 = new Document();
    doc3.add(new Field("field", "foo firstname xxx yyy lastname foo", Field.Store.YES, Field.Index.ANALYZED));
    doc3.add(new Field("field", "foo firstname zzz yyy lastname foo", Field.Store.YES, Field.Index.ANALYZED));
     writer.addDocument(doc3);
     
     writer.optimize();
@@ -517,6 +520,9 @@ public class TestPhraseQuery extends LuceneTestCase {
     //System.out.println("(exact) field: one two three: "+score0);
     QueryUtils.check(query,searcher);
 
    // just make sure no exc:
    searcher.explain(query, 0);

     // search on non palyndrome, find phrase with slop 3, though no slop required here.
     query.setSlop(4); // to use sloppy scorer 
     hits = searcher.search(query, null, 1000).scoreDocs;
@@ -533,6 +539,10 @@ public class TestPhraseQuery extends LuceneTestCase {
     query.add(new Term("palindrome", "two"));
     query.add(new Term("palindrome", "three"));
     hits = searcher.search(query, null, 1000).scoreDocs;

    // just make sure no exc:
    searcher.explain(query, 0);

     assertEquals("just sloppy enough", 1, hits.length);
     //float score2 = hits[0].score;
     //System.out.println("palindrome: one two three: "+score2);
@@ -572,4 +582,93 @@ public class TestPhraseQuery extends LuceneTestCase {
     Query rewritten = pq.rewrite(searcher.getIndexReader());
     assertTrue(rewritten instanceof TermQuery);
   }

  public void testRandomPhrases() throws Exception {
    Directory dir = new MockRAMDirectory();
    Analyzer analyzer = new MockAnalyzer();

    IndexWriter w  = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, analyzer));
    List<List<String>> docs = new ArrayList<List<String>>();
    Document d = new Document();
    Field f = new Field("f", "", Field.Store.NO, Field.Index.ANALYZED);
    d.add(f);

    Random r = newRandom();

    int NUM_DOCS = 10*_TestUtil.getRandomMultiplier();
    for(int i=0;i<NUM_DOCS;i++) {
      // must be > 4096 so it spans multiple chunks
      int termCount = _TestUtil.nextInt(r, 10000, 30000);

      List<String> doc = new ArrayList<String>();

      StringBuilder sb = new StringBuilder();
      while(doc.size() < termCount) {
        if (r.nextInt(5) == 1 || docs.size() == 0) {
          // make new non-empty-string term
          String term;
          while(true) {
            term = _TestUtil.randomUnicodeString(r);
            if (term.length() > 0) {
              break;
            }
          }
          TokenStream ts = analyzer.reusableTokenStream("ignore", new StringReader(term));
          CharTermAttribute termAttr = ts.addAttribute(CharTermAttribute.class);
          while(ts.incrementToken()) {
            String text = termAttr.toString();
            doc.add(text);
            sb.append(text).append(' ');
          }
        } else {
          // pick existing sub-phrase
          List<String> lastDoc = docs.get(r.nextInt(docs.size()));
          int len = _TestUtil.nextInt(r, 1, 10);
          int start = r.nextInt(lastDoc.size()-len);
          for(int k=start;k<start+len;k++) {
            String t = lastDoc.get(k);
            doc.add(t);
            sb.append(t).append(' ');
          }
        }
      }
      docs.add(doc);
      f.setValue(sb.toString());
      w.addDocument(d);
    }

    IndexReader reader = w.getReader();
    IndexSearcher s = new IndexSearcher(reader);
    w.close();

    // now search
    for(int i=0;i<100*_TestUtil.getRandomMultiplier();i++) {
      int docID = r.nextInt(docs.size());
      List<String> doc = docs.get(docID);
      
      final int numTerm = _TestUtil.nextInt(r, 2, 20);
      final int start = r.nextInt(doc.size()-numTerm);
      PhraseQuery pq = new PhraseQuery();
      StringBuilder sb = new StringBuilder();
      for(int t=start;t<start+numTerm;t++) {
        pq.add(new Term("f", doc.get(t)));
        sb.append(doc.get(t)).append(' ');
      }

      TopDocs hits = s.search(pq, NUM_DOCS);
      boolean found = false;
      for(int j=0;j<hits.scoreDocs.length;j++) {
        if (hits.scoreDocs[j].doc == docID) {
          found = true;
          break;
        }
      }

      assertTrue("phrase '" + sb + "' not found; start=" + start, found);
    }

    reader.close();
    searcher.close();
    dir.close();
  }
 }
- 
2.19.1.windows.1

