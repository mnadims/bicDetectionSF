From 47e96510cc2cd84c231986c16a9ede61e96b3017 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Wed, 17 Sep 2014 19:59:03 +0000
Subject: [PATCH] LUCENE-5945: don't hit NPE on trying to get root's fileName
 leading to strange test failure

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1625792 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/lucene/benchmark/byTask/feeds/TrecDocParser.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecDocParser.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecDocParser.java
index 62ff966e7c8..163020ea4e6 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecDocParser.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecDocParser.java
@@ -59,7 +59,7 @@ public abstract class TrecDocParser {
    */
   public static ParsePathType pathType(Path f) {
     int pathLength = 0;
    while (f != null && ++pathLength < MAX_PATH_LENGTH) {
    while (f != null && f.getFileName() != null && ++pathLength < MAX_PATH_LENGTH) {
       ParsePathType ppt = pathName2Type.get(f.getFileName().toString().toUpperCase(Locale.ROOT));
       if (ppt!=null) {
         return ppt;
- 
2.19.1.windows.1

