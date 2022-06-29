From cf06226cb8e18640fe94bf04661aef4947a77757 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Tue, 24 Sep 2013 11:10:18 +0000
Subject: [PATCH] LUCENE-5239: don't delete same term for the wrong field

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1525851 13f79535-47bb-0310-9956-ffa450edef68
--
 .../lucene/index/FreqProxTermsWriter.java     |  2 ++
 .../apache/lucene/index/TestIndexWriter.java  | 23 +++++++++++++++++++
 2 files changed, 25 insertions(+)

diff --git a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
index 7d50b39cb48..0ed7f1c70db 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
@@ -47,6 +47,8 @@ final class FreqProxTermsWriter extends TermsHashConsumer {
           Terms terms = fields.terms(lastField);
           if (terms != null) {
             termsEnum = terms.iterator(termsEnum);
          } else {
            termsEnum = null;
           }
         }
 
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestIndexWriter.java b/lucene/core/src/test/org/apache/lucene/index/TestIndexWriter.java
index 5e35a785a3a..0f09c3ccedf 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestIndexWriter.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestIndexWriter.java
@@ -2281,4 +2281,27 @@ public class TestIndexWriter extends LuceneTestCase {
     evilWriter.close();
     dir.close();
   }

  // LUCENE-5239
  public void testDeleteSameTermAcrossFields() throws Exception {
    Directory dir = newDirectory();
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    IndexWriter w = new IndexWriter(dir, iwc);
    Document doc = new Document();
    doc.add(new TextField("a", "foo", Field.Store.NO));
    w.addDocument(doc);

    // Should not delete the document; with LUCENE-5239 the
    // "foo" from the 2nd delete term would incorrectly
    // match field a's "foo":
    w.deleteDocuments(new Term("a", "xxx"));
    w.deleteDocuments(new Term("b", "foo"));
    IndexReader r = w.getReader();
    w.close();

    // Make sure document was not (incorrectly) deleted:
    assertEquals(1, r.numDocs());
    r.close();
    dir.close();
  }
 }
- 
2.19.1.windows.1

