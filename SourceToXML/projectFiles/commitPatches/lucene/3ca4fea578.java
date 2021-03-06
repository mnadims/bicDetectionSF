From 3ca4fea5786430130f25d180440f765e96ac9c74 Mon Sep 17 00:00:00 2001
From: David Smiley <dsmiley@apache.org>
Date: Wed, 6 Jul 2016 11:24:03 -0400
Subject: [PATCH] LUCENE-7340: MemoryIndex.toString renamed to toStringDebug;
 fix NPE

--
 lucene/CHANGES.txt                            |  3 +++
 .../lucene/index/memory/MemoryIndex.java      | 27 ++++++++++++++++---
 .../lucene/index/memory/TestMemoryIndex.java  | 22 +++++++++++++++
 3 files changed, 48 insertions(+), 4 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 9f634a5c1d8..24d9f658606 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -42,6 +42,9 @@ Bug Fixes
 
 * LUCENE-6662: Fixed potential resource leaks. (Rishabh Patel via Adrien Grand)
 
* LUCENE-7340: MemoryIndex.toString() could throw NPE; fixed. Renamed to toStringDebug().
  (Daniel Collins, David Smiley)

 Improvements
 
 * LUCENE-7323: Compound file writing now verifies the incoming
diff --git a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
index cde20e57670..cdd53ed9e2f 100644
-- a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
++ b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
@@ -43,10 +43,21 @@ import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.similarities.Similarity;
 import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.*;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefArray;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.BytesRefHash;
 import org.apache.lucene.util.BytesRefHash.DirectBytesStartArray;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.IntBlockPool;
 import org.apache.lucene.util.IntBlockPool.SliceReader;
 import org.apache.lucene.util.IntBlockPool.SliceWriter;
import org.apache.lucene.util.RecyclingByteBlockAllocator;
import org.apache.lucene.util.RecyclingIntBlockAllocator;
import org.apache.lucene.util.StringHelper;
 
 /**
  * High-performance single-document main memory Apache Lucene fulltext search index. 
@@ -746,13 +757,14 @@ public class MemoryIndex {
    * Returns a String representation of the index data for debugging purposes.
    * 
    * @return the string representation
   * @lucene.experimental
    */
  @Override
  public String toString() {
  public String toStringDebug() {
     StringBuilder result = new StringBuilder(256);
     int sumPositions = 0;
     int sumTerms = 0;
     final BytesRef spare = new BytesRef();
    final BytesRefBuilder payloadBuilder = storePayloads ? new BytesRefBuilder() : null;
     for (Map.Entry<String, Info> entry : fields.entrySet()) {
       String fieldName = entry.getKey();
       Info info = entry.getValue();
@@ -778,9 +790,16 @@ public class MemoryIndex {
               result.append(", ");
             }
           }
          if (storePayloads) {
            int payloadIndex = postingsReader.readInt();
            if (payloadIndex != -1) {
                result.append(", " + payloadsBytesRefs.get(payloadBuilder, payloadIndex));
            }
          }
           result.append(")");

           if (!postingsReader.endOfSlice()) {
            result.append(",");
            result.append(", ");
           }
 
         }
diff --git a/lucene/memory/src/test/org/apache/lucene/index/memory/TestMemoryIndex.java b/lucene/memory/src/test/org/apache/lucene/index/memory/TestMemoryIndex.java
index 57514578b16..2f95a4e5cca 100644
-- a/lucene/memory/src/test/org/apache/lucene/index/memory/TestMemoryIndex.java
++ b/lucene/memory/src/test/org/apache/lucene/index/memory/TestMemoryIndex.java
@@ -464,4 +464,26 @@ public class TestMemoryIndex extends LuceneTestCase {
     assertEquals("term", leafReader.getBinaryDocValues("field").get(0).utf8ToString());
   }
 
  public void testToStringDebug() {
    MemoryIndex mi = new MemoryIndex(true, true);
    Analyzer analyzer = new MockPayloadAnalyzer();

    mi.addField("analyzedField", "aa bb aa", analyzer);

    FieldType type = new FieldType();
    type.setDimensions(1, 4);
    type.setDocValuesType(DocValuesType.BINARY);
    type.freeze();
    mi.addField(new BinaryPoint("pointAndDvField", "term".getBytes(StandardCharsets.UTF_8), type), analyzer);

    assertEquals("analyzedField:\n" +
        "\t'[61 61]':2: [(0, 0, 2, [70 6f 73 3a 20 30]), (1, 6, 8, [70 6f 73 3a 20 32])]\n" +
        "\t'[62 62]':1: [(1, 3, 5, [70 6f 73 3a 20 31])]\n" +
        "\tterms=2, positions=3\n" +
        "pointAndDvField:\n" +
        "\tterms=0, positions=0\n" +
        "\n" +
        "fields=2, terms=2, positions=3", mi.toStringDebug());
  }

 }
- 
2.19.1.windows.1

