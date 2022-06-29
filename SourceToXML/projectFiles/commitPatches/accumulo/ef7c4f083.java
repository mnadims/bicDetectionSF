From ef7c4f0836abc9ea4fea664e0118c7ae2fdf4c30 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Wed, 1 Oct 2014 18:22:50 -0400
Subject: [PATCH] ACCUMULO-2480 Remove unnecessary curly brackets from variable
 initialization

--
 .../apache/accumulo/tserver/log/TabletServerLogger.java   | 8 ++------
 1 file changed, 2 insertions(+), 6 deletions(-)

diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java
index 86ae596c0..91ec141eb 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java
@@ -93,12 +93,8 @@ public class TabletServerLogger {
   private final AtomicLong flushCounter;
   
   private final static int HALT_AFTER_ERROR_COUNT = 5;
  private final Cache<Long, Object> walErrors;
  {
    // Die if we get 5 WAL creation errors in 10 seconds
    walErrors = CacheBuilder.newBuilder().maximumSize(HALT_AFTER_ERROR_COUNT).expireAfterWrite(10, TimeUnit.SECONDS).build();
  }

  // Die if we get 5 WAL creation errors in 10 seconds
  private final Cache<Long,Object> walErrors = CacheBuilder.newBuilder().maximumSize(HALT_AFTER_ERROR_COUNT).expireAfterWrite(10, TimeUnit.SECONDS).build();
 
   static private abstract class TestCallWithWriteLock {
     abstract boolean test();
- 
2.19.1.windows.1

