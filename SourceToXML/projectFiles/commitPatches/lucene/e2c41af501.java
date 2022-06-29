From e2c41af5017f67c279df239a1b99a00c4c4cf9b0 Mon Sep 17 00:00:00 2001
From: Mike McCandless <mikemccand@apache.org>
Date: Sat, 14 Jan 2017 06:21:01 -0500
Subject: [PATCH] LUCENE-7626: I forgot to close the reader in this test

--
 .../src/test/org/apache/lucene/index/TestFixBrokenOffsets.java | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

diff --git a/lucene/backward-codecs/src/test/org/apache/lucene/index/TestFixBrokenOffsets.java b/lucene/backward-codecs/src/test/org/apache/lucene/index/TestFixBrokenOffsets.java
index bcd5a65aee8..4ecbd13afee 100644
-- a/lucene/backward-codecs/src/test/org/apache/lucene/index/TestFixBrokenOffsets.java
++ b/lucene/backward-codecs/src/test/org/apache/lucene/index/TestFixBrokenOffsets.java
@@ -70,7 +70,7 @@ public class TestFixBrokenOffsets extends LuceneTestCase {
     assertNotNull("Broken offsets index not found", resource);
     Path path = createTempDir("brokenoffsets");
     TestUtil.unzip(resource, path);
    Directory dir = FSDirectory.open(path);
    Directory dir = newFSDirectory(path);
 
     // OK: index is 6.3.0 so offsets not checked:
     TestUtil.checkIndex(dir);
@@ -94,6 +94,7 @@ public class TestFixBrokenOffsets extends LuceneTestCase {
       codecReaders[i] = (CodecReader) leaves.get(i).reader();
     }
     w.addIndexes(codecReaders);
    reader.close();
     w.close();
 
     // NOT OK: broken offsets were copied into a 7.0 segment:
- 
2.19.1.windows.1

