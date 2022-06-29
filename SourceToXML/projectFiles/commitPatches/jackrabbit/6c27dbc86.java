From 6c27dbc86a3caf7bd1122a106a7e2d54307b00de Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Mon, 30 Nov 2009 13:06:33 +0000
Subject: [PATCH] JCR-2413: AlreadyClosedException on initial index creation

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@885411 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/MultiIndex.java         | 25 ++++++-------------
 1 file changed, 8 insertions(+), 17 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiIndex.java
index c5ade8b2d..759021d6a 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiIndex.java
@@ -329,7 +329,7 @@ public class MultiIndex {
         flushTask = new Timer.Task() {
             public void run() {
                 // check if there are any indexing jobs finished
                checkIndexingQueue();
                checkIndexingQueue(false);
                 // check if volatile index should be flushed
                 checkFlush();
             }
@@ -1264,17 +1264,6 @@ public class MultiIndex {
         }
     }
 
    /**
     * Checks the indexing queue for finished text extrator jobs and updates the
     * index accordingly if there are any new ones. This method is synchronized
     * and should only be called by the timer task that periodically checks if
     * there are documents ready in the indexing queue. A new transaction is
     * used when documents are transfered from the indexing queue to the index.
     */
    private synchronized void checkIndexingQueue() {
        checkIndexingQueue(false);
    }

     /**
      * Checks the indexing queue for finished text extrator jobs and updates the
      * index accordingly if there are any new ones.
@@ -1304,11 +1293,13 @@ public class MultiIndex {
 
             try {
                 if (transactionPresent) {
                    for (NodeId id : finished.keySet()) {
                        executeAndLog(new DeleteNode(getTransactionId(), id));
                    }
                    for (Document document : finished.values()) {
                        executeAndLog(new AddNode(getTransactionId(), document));
                    synchronized (this) {
                        for (NodeId id : finished.keySet()) {
                            executeAndLog(new DeleteNode(getTransactionId(), id));
                        }
                        for (Document document : finished.values()) {
                            executeAndLog(new AddNode(getTransactionId(), document));
                        }
                     }
                 } else {
                     update(finished.keySet(), finished.values());
- 
2.19.1.windows.1

