From ff886ebb70ccf627100936977f1cb78bea26ca2f Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Tue, 10 Feb 2015 22:54:22 +0000
Subject: [PATCH] LUCENE-6233: speed up CheckIndex when the index has term
 vectors

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1658831 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   3 +
 .../org/apache/lucene/index/CheckIndex.java   | 146 ++++++++++++------
 2 files changed, 100 insertions(+), 49 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 29504d9852a..b48c19ef171 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -68,6 +68,9 @@ Optimizations
 * LUCENE-6218: Don't decode frequencies or match all positions when scoring
   is not needed. (Robert Muir)
 
* LUCENE-6233 Speed up CheckIndex when the index has term vectors
  (Robert Muir, Mike McCandless)

 API Changes
 
 * LUCENE-6204, LUCENE-6208: Simplify CompoundFormat: remove files()
diff --git a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
index bc60bde217e..e8426c9b2cf 100644
-- a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
++ b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
@@ -448,6 +448,7 @@ public class CheckIndex implements Closeable {
    *  time to run. */
   public Status checkIndex(List<String> onlySegments) throws IOException {
     ensureOpen();
    long startNS = System.nanoTime();
     NumberFormat nf = NumberFormat.getInstance(Locale.ROOT);
     SegmentInfos sis = null;
     Status result = new Status();
@@ -625,17 +626,20 @@ public class CheckIndex implements Closeable {
           segInfoStat.hasDeletions = true;
           segInfoStat.deletionsGen = info.getDelGen();
         }
        
        long startOpenReaderNS = System.nanoTime();
         if (infoStream != null)
           infoStream.print("    test: open reader.........");
         reader = new SegmentReader(info, IOContext.DEFAULT);
        msg(infoStream, "OK");
        msg(infoStream, String.format(Locale.ROOT, "OK [took %.3f sec]", nsToSec(System.nanoTime()-startOpenReaderNS)));
 
         segInfoStat.openReaderPassed = true;
         
        long startIntegrityNS = System.nanoTime();
         if (infoStream != null)
           infoStream.print("    test: check integrity.....");
         reader.checkIntegrity();
        msg(infoStream, "OK");
        msg(infoStream, String.format(Locale.ROOT, "OK [took %.3f sec]", nsToSec(System.nanoTime()-startIntegrityNS)));
 
         if (reader.maxDoc() != info.info.getDocCount()) {
           throw new RuntimeException("SegmentReader.maxDoc() " + reader.maxDoc() + " != SegmentInfos.docCount " + info.info.getDocCount());
@@ -743,6 +747,8 @@ public class CheckIndex implements Closeable {
       msg(infoStream, "No problems were detected with this index.\n");
     }
 
    msg(infoStream, String.format(Locale.ROOT, "Took %.3f sec total.", nsToSec(System.nanoTime()-startNS)));

     return result;
   }
   
@@ -751,6 +757,7 @@ public class CheckIndex implements Closeable {
    * @lucene.experimental
    */
   public static Status.LiveDocStatus testLiveDocs(LeafReader reader, PrintStream infoStream, boolean failFast) throws IOException {
    long startNS = System.nanoTime();
     final Status.LiveDocStatus status = new Status.LiveDocStatus();
     
     try {
@@ -774,7 +781,7 @@ public class CheckIndex implements Closeable {
         }
         
         status.numDeleted = reader.numDeletedDocs();
        msg(infoStream, "OK [" + (status.numDeleted) + " deleted docs]");
        msg(infoStream, String.format(Locale.ROOT, "OK [%d deleted docs] [took %.3f sec]", status.numDeleted, nsToSec(System.nanoTime()-startNS)));
       } else {
         Bits liveDocs = reader.getLiveDocs();
         if (liveDocs != null) {
@@ -785,7 +792,7 @@ public class CheckIndex implements Closeable {
             }
           }
         }
        msg(infoStream, "OK");
        msg(infoStream, String.format(Locale.ROOT, "OK [took %.3f sec]", (nsToSec(System.nanoTime()-startNS))));
       }
       
     } catch (Throwable e) {
@@ -807,6 +814,7 @@ public class CheckIndex implements Closeable {
    * @lucene.experimental
    */
   public static Status.FieldInfoStatus testFieldInfos(LeafReader reader, PrintStream infoStream, boolean failFast) throws IOException {
    long startNS = System.nanoTime();
     final Status.FieldInfoStatus status = new Status.FieldInfoStatus();
     
     try {
@@ -818,7 +826,7 @@ public class CheckIndex implements Closeable {
       for (FieldInfo f : fieldInfos) {
         f.checkConsistency();
       }
      msg(infoStream, "OK [" + fieldInfos.size() + " fields]");
      msg(infoStream, String.format(Locale.ROOT, "OK [%d fields] [took %.3f sec]", fieldInfos.size(), nsToSec(System.nanoTime()-startNS)));
       status.totFields = fieldInfos.size();
     } catch (Throwable e) {
       if (failFast) {
@@ -839,6 +847,7 @@ public class CheckIndex implements Closeable {
    * @lucene.experimental
    */
   public static Status.FieldNormStatus testFieldNorms(LeafReader reader, PrintStream infoStream, boolean failFast) throws IOException {
    long startNS = System.nanoTime();
     final Status.FieldNormStatus status = new Status.FieldNormStatus();
 
     try {
@@ -857,7 +866,7 @@ public class CheckIndex implements Closeable {
         }
       }
 
      msg(infoStream, "OK [" + status.totFields + " fields]");
      msg(infoStream, String.format(Locale.ROOT, "OK [%d fields] [took %.3f sec]", status.totFields, nsToSec(System.nanoTime()-startNS)));
     } catch (Throwable e) {
       if (failFast) {
         IOUtils.reThrow(e);
@@ -878,6 +887,12 @@ public class CheckIndex implements Closeable {
    */
   private static Status.TermIndexStatus checkFields(Fields fields, Bits liveDocs, int maxDoc, FieldInfos fieldInfos, boolean doPrint, boolean isVectors, PrintStream infoStream, boolean verbose) throws IOException {
     // TODO: we should probably return our own stats thing...?!
    long startNS;
    if (doPrint) {
      startNS = System.nanoTime();
    } else {
      startNS = 0;
    }
     
     final Status.TermIndexStatus status = new Status.TermIndexStatus();
     int computedFieldCount = 0;
@@ -920,27 +935,33 @@ public class CheckIndex implements Closeable {
       final boolean hasPayloads = terms.hasPayloads();
       final boolean hasOffsets = terms.hasOffsets();
       
      BytesRef bb = terms.getMin();
      BytesRef maxTerm;
       BytesRef minTerm;
      if (bb != null) {
        assert bb.isValid();
        minTerm = BytesRef.deepCopyOf(bb);
      } else {
      if (isVectors) {
        // Term vectors impls can be very slow for getMax
        maxTerm = null;
         minTerm = null;
      }

      BytesRef maxTerm;
      bb = terms.getMax();
      if (bb != null) {
        assert bb.isValid();
        maxTerm = BytesRef.deepCopyOf(bb);
        if (minTerm == null) {
          throw new RuntimeException("field \"" + field + "\" has null minTerm but non-null maxTerm");
        }
       } else {
        maxTerm = null;
        if (minTerm != null) {
          throw new RuntimeException("field \"" + field + "\" has non-null minTerm but null maxTerm");
        BytesRef bb = terms.getMin();
        if (bb != null) {
          assert bb.isValid();
          minTerm = BytesRef.deepCopyOf(bb);
        } else {
          minTerm = null;
        }

        bb = terms.getMax();
        if (bb != null) {
          assert bb.isValid();
          maxTerm = BytesRef.deepCopyOf(bb);
          if (minTerm == null) {
            throw new RuntimeException("field \"" + field + "\" has null minTerm but non-null maxTerm");
          }
        } else {
          maxTerm = null;
          if (minTerm != null) {
            throw new RuntimeException("field \"" + field + "\" has non-null minTerm but null maxTerm");
          }
         }
       }
 
@@ -975,7 +996,7 @@ public class CheckIndex implements Closeable {
       }
 
       final TermsEnum termsEnum = terms.iterator(null);
      

       boolean hasOrd = true;
       final long termCountStart = status.delTermCount + status.termCount;
       
@@ -1005,19 +1026,21 @@ public class CheckIndex implements Closeable {
           }
           lastTerm.copyBytes(term);
         }

        if (isVectors == false) {
          if (minTerm == null) {
            // We checked this above:
            assert maxTerm == null;
            throw new RuntimeException("field=\"" + field + "\": invalid term: term=" + term + ", minTerm=" + minTerm);
          }
         
        if (minTerm == null) {
          // We checked this above:
          assert maxTerm == null;
          throw new RuntimeException("field=\"" + field + "\": invalid term: term=" + term + ", minTerm=" + minTerm);
        }
        
        if (term.compareTo(minTerm) < 0) {
          throw new RuntimeException("field=\"" + field + "\": invalid term: term=" + term + ", minTerm=" + minTerm);
        }
          if (term.compareTo(minTerm) < 0) {
            throw new RuntimeException("field=\"" + field + "\": invalid term: term=" + term + ", minTerm=" + minTerm);
          }
         
        if (term.compareTo(maxTerm) > 0) {
          throw new RuntimeException("field=\"" + field + "\": invalid term: term=" + term + ", maxTerm=" + maxTerm);
          if (term.compareTo(maxTerm) > 0) {
            throw new RuntimeException("field=\"" + field + "\": invalid term: term=" + term + ", maxTerm=" + maxTerm);
          }
         }
         
         final int docFreq = termsEnum.docFreq();
@@ -1243,6 +1266,11 @@ public class CheckIndex implements Closeable {
                 throw new RuntimeException("term " + term + ": advance(docID=" + skipDocID + "), then .next() returned docID=" + nextDocID + " vs prev docID=" + docID);
               }
             }

            if (isVectors) {
              // Only 1 doc in the postings for term vectors, so we only test 1 advance:
              break;
            }
           }
         } else {
           for(int idx=0;idx<7;idx++) {
@@ -1263,6 +1291,10 @@ public class CheckIndex implements Closeable {
                 throw new RuntimeException("term " + term + ": advance(docID=" + skipDocID + "), then .next() returned docID=" + nextDocID + " vs prev docID=" + docID);
               }
             }
            if (isVectors) {
              // Only 1 doc in the postings for term vectors, so we only test 1 advance:
              break;
            }
           }
         }
       }
@@ -1408,7 +1440,8 @@ public class CheckIndex implements Closeable {
     }
 
     if (doPrint) {
      msg(infoStream, "OK [" + status.termCount + " terms; " + status.totFreq + " terms/docs pairs; " + status.totPos + " tokens]");
      msg(infoStream, String.format(Locale.ROOT, "OK [%d terms; %d terms/docs pairs; %d tokens] [took %.3f sec]",
                                    status.termCount, status.totFreq, status.totPos, nsToSec(System.nanoTime()-startNS)));
     }
     
     if (verbose && status.blockTreeStats != null && infoStream != null && status.termCount > 0) {
@@ -1476,6 +1509,7 @@ public class CheckIndex implements Closeable {
    * @lucene.experimental
    */
   public static Status.StoredFieldStatus testStoredFields(LeafReader reader, PrintStream infoStream, boolean failFast) throws IOException {
    long startNS = System.nanoTime();
     final Status.StoredFieldStatus status = new Status.StoredFieldStatus();
 
     try {
@@ -1500,8 +1534,10 @@ public class CheckIndex implements Closeable {
         throw new RuntimeException("docCount=" + status.docCount + " but saw " + status.docCount + " undeleted docs");
       }
 
      msg(infoStream, "OK [" + status.totFields + " total field count; avg " + 
          NumberFormat.getInstance(Locale.ROOT).format((((float) status.totFields)/status.docCount)) + " fields per doc]");      
      msg(infoStream, String.format(Locale.ROOT, "OK [%d total field count; avg %.1f fields per doc] [took %.3f sec]",
                                    status.totFields,
                                    (((float) status.totFields)/status.docCount),
                                    nsToSec(System.nanoTime() - startNS)));
     } catch (Throwable e) {
       if (failFast) {
         IOUtils.reThrow(e);
@@ -1523,6 +1559,7 @@ public class CheckIndex implements Closeable {
   public static Status.DocValuesStatus testDocValues(LeafReader reader,
                                                      PrintStream infoStream,
                                                      boolean failFast) throws IOException {
    long startNS = System.nanoTime();
     final Status.DocValuesStatus status = new Status.DocValuesStatus();
     try {
       if (infoStream != null) {
@@ -1543,12 +1580,15 @@ public class CheckIndex implements Closeable {
         }
       }
 
      msg(infoStream, "OK [" + status.totalValueFields + " docvalues fields; "
                             + status.totalBinaryFields + " BINARY; " 
                             + status.totalNumericFields + " NUMERIC; "
                             + status.totalSortedFields + " SORTED; "
                             + status.totalSortedNumericFields + " SORTED_NUMERIC; "
                             + status.totalSortedSetFields + " SORTED_SET]");
      msg(infoStream, String.format(Locale.ROOT,
                                    "OK [%d docvalues fields; %d BINARY; %d NUMERIC; %d SORTED; %d SORTED_NUMERIC; %d SORTED_SET] [took %.3f sec]",
                                    status.totalValueFields,
                                    status.totalBinaryFields,
                                    status.totalNumericFields,
                                    status.totalSortedFields,
                                    status.totalSortedNumericFields,
                                    status.totalSortedSetFields,
                                    nsToSec(System.nanoTime()-startNS)));
     } catch (Throwable e) {
       if (failFast) {
         IOUtils.reThrow(e);
@@ -1797,6 +1837,7 @@ public class CheckIndex implements Closeable {
    * @lucene.experimental
    */
   public static Status.TermVectorStatus testTermVectors(LeafReader reader, PrintStream infoStream, boolean verbose, boolean crossCheckTermVectors, boolean failFast) throws IOException {
    long startNS = System.nanoTime();
     final Status.TermVectorStatus status = new Status.TermVectorStatus();
     final FieldInfos fieldInfos = reader.getFieldInfos();
     final Bits onlyDocIsDeleted = new FixedBitSet(1);
@@ -1839,8 +1880,11 @@ public class CheckIndex implements Closeable {
           // First run with no deletions:
           checkFields(tfv, null, 1, fieldInfos, false, true, infoStream, verbose);
 
          // Again, with the one doc deleted:
          checkFields(tfv, onlyDocIsDeleted, 1, fieldInfos, false, true, infoStream, verbose);
          if (j == 0) {
            // Also test with the 1 doc deleted; we only do this for first doc because this really is just looking for a [slightly] buggy
            // TermVectors impl that fails to respect the incoming live docs:
            checkFields(tfv, onlyDocIsDeleted, 1, fieldInfos, false, true, infoStream, verbose);
          }
 
           // Only agg stats if the doc is live:
           final boolean doStats = liveDocs == null || liveDocs.get(j);
@@ -2005,8 +2049,8 @@ public class CheckIndex implements Closeable {
         }
       }
       float vectorAvg = status.docCount == 0 ? 0 : status.totVectors / (float)status.docCount;
      msg(infoStream, "OK [" + status.totVectors + " total vector count; avg " + 
          NumberFormat.getInstance(Locale.ROOT).format(vectorAvg) + " term/freq vector fields per doc]");
      msg(infoStream, String.format(Locale.ROOT, "OK [%d total term vector count; avg %.1f term/freq vector fields per doc] [took %.3f sec]",
                                    status.totVectors, vectorAvg, nsToSec(System.nanoTime() - startNS)));
     } catch (Throwable e) {
       if (failFast) {
         IOUtils.reThrow(e);
@@ -2221,4 +2265,8 @@ public class CheckIndex implements Closeable {
       }
     }
   }

  private static double nsToSec(long ns) {
    return ns/1000000000.0;
  }
 }
- 
2.19.1.windows.1

