From f57e0177ffd3f367de81bdf7f2ad67ad0f94264a Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@gmail.com>
Date: Fri, 20 Jan 2017 13:47:29 +0100
Subject: [PATCH] LUCENE-7643: Fix leftover.

--
 .../core/src/java/org/apache/lucene/search/PointRangeQuery.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/lucene/core/src/java/org/apache/lucene/search/PointRangeQuery.java b/lucene/core/src/java/org/apache/lucene/search/PointRangeQuery.java
index f1b85519d0b..7c997caf08a 100644
-- a/lucene/core/src/java/org/apache/lucene/search/PointRangeQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/PointRangeQuery.java
@@ -281,7 +281,7 @@ public abstract class PointRangeQuery extends Query {
 
             @Override
             public Scorer get(boolean randomAccess) throws IOException {
              if (false && values.getDocCount() == reader.maxDoc()
              if (values.getDocCount() == reader.maxDoc()
                   && values.getDocCount() == values.size()
                   && cost() > reader.maxDoc() / 2) {
                 // If all docs have exactly one value and the cost is greater
- 
2.19.1.windows.1

