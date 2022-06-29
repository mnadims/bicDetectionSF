From 5875781c52e030c221fcf9a04f2bde3c8075fe4f Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Sun, 22 Jun 2014 17:16:26 -0400
Subject: [PATCH] ACCUMULO-2935 Add a timeout to a test that otherwise hung
 indefinitely

--
 test/src/test/java/org/apache/accumulo/test/MetaSplitIT.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/test/src/test/java/org/apache/accumulo/test/MetaSplitIT.java b/test/src/test/java/org/apache/accumulo/test/MetaSplitIT.java
index 6d059212a..4ea266374 100644
-- a/test/src/test/java/org/apache/accumulo/test/MetaSplitIT.java
++ b/test/src/test/java/org/apache/accumulo/test/MetaSplitIT.java
@@ -31,7 +31,7 @@ import org.junit.Test;
 
 public class MetaSplitIT extends SimpleMacIT {
 
  @Test(expected = AccumuloException.class)
  @Test(expected = AccumuloException.class, timeout = 30000)
   public void testRootTableSplit() throws Exception {
     TableOperations opts = getConnector().tableOperations();
     SortedSet<Text> splits = new TreeSet<Text>();
- 
2.19.1.windows.1

