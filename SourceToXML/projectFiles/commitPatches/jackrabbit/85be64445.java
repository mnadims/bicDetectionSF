From 85be64445de287f7e93256449ee1aec3bc8a67e1 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Wed, 28 Mar 2007 10:35:22 +0000
Subject: [PATCH] JCR-788: Upgrade to Lucene 2.1 - remove reference to field
 COMMIT_LOCK_NAME in IndexWriter that was removed in 2.1

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@523265 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/jackrabbit/core/query/lucene/PersistentIndex.java    | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java
index f649956e3..333459b78 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java
@@ -42,7 +42,7 @@ class PersistentIndex extends AbstractIndex {
     private static final String WRITE_LOCK = IndexWriter.WRITE_LOCK_NAME;
 
     /** Name of the commit lock file */
    private static final String COMMIT_LOCK = IndexWriter.COMMIT_LOCK_NAME;
    private static final String COMMIT_LOCK = "commit.lock";
 
     /** The name of this persistent index */
     private final String name;
- 
2.19.1.windows.1

