From 11ae32183f088bc24f4f1cc797ab5ca29c70c8c5 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Thu, 24 Apr 2014 14:44:22 +0000
Subject: [PATCH] LUCENE-5610: improve CheckIndex checking; javadocs

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1589752 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/lucene/index/CheckIndex.java   | 40 +++++++++++++++----
 .../java/org/apache/lucene/index/Terms.java   |  6 ++-
 2 files changed, 37 insertions(+), 9 deletions(-)

diff --git a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
index 4a87726a6f1..cba481157ac 100644
-- a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
++ b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
@@ -758,12 +758,28 @@ public class CheckIndex {
       final boolean hasOffsets = terms.hasOffsets();
       
       BytesRef bb = terms.getMin();
      assert bb.isValid();
      final BytesRef minTerm = bb == null ? null : BytesRef.deepCopyOf(bb);
      
      BytesRef minTerm;
      if (bb != null) {
        assert bb.isValid();
        minTerm = BytesRef.deepCopyOf(bb);
      } else {
        minTerm = null;
      }

      BytesRef maxTerm;
       bb = terms.getMax();
      assert bb.isValid();
      final BytesRef maxTerm = bb == null ? null : BytesRef.deepCopyOf(bb);
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
 
       // term vectors cannot omit TF:
       final boolean expectedHasFreqs = (isVectors || fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0);
@@ -825,12 +841,18 @@ public class CheckIndex {
           lastTerm.copyBytes(term);
         }
         
        if (minTerm == null) {
          // We checked this above:
          assert maxTerm == null;
          throw new RuntimeException("field=\"" + field + "\": invalid term: term=" + term + ", minTerm=" + minTerm);
        }
        
         if (term.compareTo(minTerm) < 0) {
          throw new RuntimeException("invalid term: term=" + term + ", minTerm=" + minTerm);
          throw new RuntimeException("field=\"" + field + "\": invalid term: term=" + term + ", minTerm=" + minTerm);
         }
         
         if (term.compareTo(maxTerm) > 0) {
          throw new RuntimeException("invalid term: term=" + term + ", maxTerm=" + maxTerm);
          throw new RuntimeException("field=\"" + field + "\": invalid term: term=" + term + ", maxTerm=" + maxTerm);
         }
         
         final int docFreq = termsEnum.docFreq();
@@ -1080,6 +1102,10 @@ public class CheckIndex {
         }
       }
       
      if (minTerm != null && status.termCount + status.delTermCount == 0) {
        throw new RuntimeException("field=\"" + field + "\": minTerm is non-null yet we saw no terms: " + minTerm);
      }

       final Terms fieldTerms = fields.terms(field);
       if (fieldTerms == null) {
         // Unusual: the FieldsEnum returned a field but
diff --git a/lucene/core/src/java/org/apache/lucene/index/Terms.java b/lucene/core/src/java/org/apache/lucene/index/Terms.java
index 533d51e8634..f2b88cc904c 100644
-- a/lucene/core/src/java/org/apache/lucene/index/Terms.java
++ b/lucene/core/src/java/org/apache/lucene/index/Terms.java
@@ -120,14 +120,16 @@ public abstract class Terms {
   
   /** Returns the smallest term (in lexicographic order) in the field. 
    *  Note that, just like other term measures, this measure does not 
   *  take deleted documents into account. */
   *  take deleted documents into account.  This returns
   *  null when there are no terms. */
   public BytesRef getMin() throws IOException {
     return iterator(null).next();
   }
 
   /** Returns the largest term (in lexicographic order) in the field. 
    *  Note that, just like other term measures, this measure does not 
   *  take deleted documents into account. */
   *  take deleted documents into account.  This returns
   *  null when there are no terms. */
   @SuppressWarnings("fallthrough")
   public BytesRef getMax() throws IOException {
     long size = size();
- 
2.19.1.windows.1

