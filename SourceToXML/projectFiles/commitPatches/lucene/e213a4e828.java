From e213a4e8289ebc8d65f33dfd04e90bd4b3cdf524 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Fri, 14 Nov 2008 10:02:44 +0000
Subject: [PATCH] LUCENE-1452: fixes cases during merge and lazy field access
 where binary field is truncated to 0

git-svn-id: https://svn.apache.org/repos/asf/lucene/java/trunk@713962 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |  6 ++
 .../org/apache/lucene/index/FieldsReader.java |  7 ++
 .../apache/lucene/index/TestIndexReader.java  | 97 ++++++++++++++++++-
 3 files changed, 108 insertions(+), 2 deletions(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index cec6d4df9c6..832b4e464ea 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -27,6 +27,12 @@ Bug fixes
    implementation - Leads to Solr Cache misses. 
    (Todd Feak, Mark Miller via yonik)
 
2. LUCENE-1452: Fixed silent data-loss case whereby binary fields are
   truncated to 0 bytes during merging if the segments being merged
   are non-congruent (same field name maps to different field
   numbers).  This bug was introduced with LUCENE-1219.  (Andrzej
   Bialecki via Mike McCandless).

 New features
 
  1. LUCENE-1411: Added expert API to open an IndexWriter on a prior
diff --git a/src/java/org/apache/lucene/index/FieldsReader.java b/src/java/org/apache/lucene/index/FieldsReader.java
index a45a84f598a..3b2b915b7e1 100644
-- a/src/java/org/apache/lucene/index/FieldsReader.java
++ b/src/java/org/apache/lucene/index/FieldsReader.java
@@ -423,6 +423,8 @@ final class FieldsReader {
       this.toRead = toRead;
       this.pointer = pointer;
       this.isBinary = isBinary;
      if (isBinary)
        binaryLength = toRead;
       lazy = true;
     }
 
@@ -431,6 +433,8 @@ final class FieldsReader {
       this.toRead = toRead;
       this.pointer = pointer;
       this.isBinary = isBinary;
      if (isBinary)
        binaryLength = toRead;
       lazy = true;
     }
 
@@ -619,6 +623,9 @@ final class FieldsReader {
       this.fieldsData = value;
       this.isCompressed = compressed;
       this.isBinary = binary;
      if (binary)
        binaryLength = ((byte[]) value).length;

       this.isTokenized = tokenize;
 
       this.name = fi.name.intern();
diff --git a/src/test/org/apache/lucene/index/TestIndexReader.java b/src/test/org/apache/lucene/index/TestIndexReader.java
index 2879fecb604..f6b06bd1e1a 100644
-- a/src/test/org/apache/lucene/index/TestIndexReader.java
++ b/src/test/org/apache/lucene/index/TestIndexReader.java
@@ -26,6 +26,7 @@ import java.util.Collection;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Set;
import java.util.HashSet;
 
 import junit.framework.TestSuite;
 import junit.textui.TestRunner;
@@ -34,6 +35,9 @@ import org.apache.lucene.analysis.WhitespaceAnalyzer;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
 import org.apache.lucene.index.IndexReader.FieldOption;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.ScoreDoc;
@@ -289,6 +293,96 @@ public class TestIndexReader extends LuceneTestCase
         reader2.close();
         dir.close();
     }
    
    public void testBinaryFields() throws IOException
    {
        Directory dir = new RAMDirectory();
        byte[] bin = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        
        IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
        
        for (int i = 0; i < 10; i++) {
          addDoc(writer, "document number " + (i + 1));
          addDocumentWithFields(writer);
          addDocumentWithDifferentFields(writer);
          addDocumentWithTermVectorFields(writer);
        }
        writer.close();
        writer = new IndexWriter(dir, new WhitespaceAnalyzer(), false, IndexWriter.MaxFieldLength.LIMITED);
        Document doc = new Document();
        doc.add(new Field("bin1", bin, Field.Store.YES));
        doc.add(new Field("bin2", bin, Field.Store.COMPRESS));
        doc.add(new Field("junk", "junk text", Field.Store.NO, Field.Index.ANALYZED));
        writer.addDocument(doc);
        writer.close();
        IndexReader reader = IndexReader.open(dir);
        doc = reader.document(reader.maxDoc() - 1);
        Field[] fields = doc.getFields("bin1");
        assertNotNull(fields);
        assertEquals(1, fields.length);
        Field b1 = fields[0];
        assertTrue(b1.isBinary());
        byte[] data1 = b1.getBinaryValue();
        assertEquals(bin.length, b1.getBinaryLength());
        for (int i = 0; i < bin.length; i++) {
          assertEquals(bin[i], data1[i + b1.getBinaryOffset()]);
        }
        fields = doc.getFields("bin2");
        assertNotNull(fields);
        assertEquals(1, fields.length);
        b1 = fields[0];
        assertTrue(b1.isBinary());
        data1 = b1.getBinaryValue();
        assertEquals(bin.length, b1.getBinaryLength());
        for (int i = 0; i < bin.length; i++) {
          assertEquals(bin[i], data1[i + b1.getBinaryOffset()]);
        }
        Set lazyFields = new HashSet();
        lazyFields.add("bin1");
        FieldSelector sel = new SetBasedFieldSelector(new HashSet(), lazyFields);
        doc = reader.document(reader.maxDoc() - 1, sel);
        Fieldable[] fieldables = doc.getFieldables("bin1");
        assertNotNull(fieldables);
        assertEquals(1, fieldables.length);
        Fieldable fb1 = fieldables[0];
        assertTrue(fb1.isBinary());
        assertEquals(bin.length, fb1.getBinaryLength());
        data1 = fb1.getBinaryValue();
        assertEquals(bin.length, fb1.getBinaryLength());
        for (int i = 0; i < bin.length; i++) {
          assertEquals(bin[i], data1[i + fb1.getBinaryOffset()]);
        }
        reader.close();
        // force optimize


        writer = new IndexWriter(dir, new WhitespaceAnalyzer(), false, IndexWriter.MaxFieldLength.LIMITED);
        writer.optimize();
        writer.close();
        reader = IndexReader.open(dir);
        doc = reader.document(reader.maxDoc() - 1);
        fields = doc.getFields("bin1");
        assertNotNull(fields);
        assertEquals(1, fields.length);
        b1 = fields[0];
        assertTrue(b1.isBinary());
        data1 = b1.getBinaryValue();
        assertEquals(bin.length, b1.getBinaryLength());
        for (int i = 0; i < bin.length; i++) {
          assertEquals(bin[i], data1[i + b1.getBinaryOffset()]);
        }
        fields = doc.getFields("bin2");
        assertNotNull(fields);
        assertEquals(1, fields.length);
        b1 = fields[0];
        assertTrue(b1.isBinary());
        data1 = b1.getBinaryValue();
        assertEquals(bin.length, b1.getBinaryLength());
        for (int i = 0; i < bin.length; i++) {
          assertEquals(bin[i], data1[i + b1.getBinaryOffset()]);
        }
        reader.close();
    }
 
     // Make sure attempts to make changes after reader is
     // closed throws IOException:
@@ -1403,9 +1497,8 @@ public class TestIndexReader extends LuceneTestCase
         w.close();
         assertTrue(new File(indexDir, "_0.fnm").delete());
 
        IndexReader r = null;
         try {
          r = IndexReader.open(indexDir);
          IndexReader.open(indexDir);
           fail("did not hit expected exception");
         } catch (AlreadyClosedException ace) {
           fail("should not have hit AlreadyClosedException");
- 
2.19.1.windows.1

