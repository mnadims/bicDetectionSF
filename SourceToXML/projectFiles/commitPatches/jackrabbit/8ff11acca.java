From 8ff11accaa0276d1006f8a6769dcf6eb7c6c58b3 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Wed, 28 Nov 2007 09:34:48 +0000
Subject: [PATCH] JCR-1240: Index segments are only committed on close

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@598927 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/jackrabbit/core/query/lucene/AbstractIndex.java   | 5 -----
 1 file changed, 5 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractIndex.java
index b4a4fbf7a..7b4a8f1ab 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractIndex.java
@@ -318,11 +318,6 @@ abstract class AbstractIndex {
      * @throws IOException if an error occurs while commiting changes.
      */
     protected synchronized void commit(boolean optimize) throws IOException {
        // if index is not locked there are no pending changes
        if (!IndexReader.isLocked(getDirectory())) {
            return;
        }

         if (indexReader != null) {
             indexReader.commitDeleted();
         }
- 
2.19.1.windows.1

