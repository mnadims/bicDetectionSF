From e77bdd7de25af87c53da6e5a3727c028956c1e00 Mon Sep 17 00:00:00 2001
From: Shawn Walker <accumulo@shawn-walker.net>
Date: Fri, 15 Apr 2016 10:38:37 -0400
Subject: [PATCH] ACCUMULO-4191 Wrapped runnable with Trace.wrap in
 TabletServerBatchWriter to prevent loss of sendMutation events

Closes apache/accumulo#94

Signed-off-by: Josh Elser <elserj@apache.org>
--
 .../accumulo/core/client/impl/TabletServerBatchWriter.java    | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
index 77d830725..a8afa5af9 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
@@ -735,7 +735,7 @@ public class TabletServerBatchWriter {
     void queueMutations(final MutationSet mutationsToSend) throws InterruptedException {
       if (null == mutationsToSend)
         return;
      binningThreadPool.execute(new Runnable() {
      binningThreadPool.execute(Trace.wrap(new Runnable() {
 
         @Override
         public void run() {
@@ -748,7 +748,7 @@ public class TabletServerBatchWriter {
             }
           }
         }
      });
      }));
     }
 
     private void addMutations(MutationSet mutationsToSend) {
- 
2.19.1.windows.1

