From 4d83d53b0e017306594222c2b292de98ee0d78de Mon Sep 17 00:00:00 2001
From: "Eric C. Newton" <ecn@apache.org>
Date: Thu, 16 May 2013 18:35:53 +0000
Subject: [PATCH] ACCUMULO-1421 simplify reflection lookup of SafeModeAction

git-svn-id: https://svn.apache.org/repos/asf/accumulo/branches/1.5@1483494 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/accumulo/test/functional/LargeRowTest.java  | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/test/src/main/java/org/apache/accumulo/test/functional/LargeRowTest.java b/test/src/main/java/org/apache/accumulo/test/functional/LargeRowTest.java
index d76336ac4..bea4d3e49 100644
-- a/test/src/main/java/org/apache/accumulo/test/functional/LargeRowTest.java
++ b/test/src/main/java/org/apache/accumulo/test/functional/LargeRowTest.java
@@ -97,7 +97,7 @@ public class LargeRowTest extends FunctionalTest {
     
     UtilWaitThread.sleep(12000);
     Logger.getLogger(LargeRowTest.class).warn("checking splits");
    checkSplits(REG_TABLE_NAME, NUM_PRE_SPLITS/2, NUM_PRE_SPLITS);
    checkSplits(REG_TABLE_NAME, NUM_PRE_SPLITS/2, NUM_PRE_SPLITS * 2);
     
     verify(REG_TABLE_NAME);
   }
- 
2.19.1.windows.1

