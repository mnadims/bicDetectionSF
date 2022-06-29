From efdbae3c127ba3fe48fad8bd03d10f5a47ff3acf Mon Sep 17 00:00:00 2001
From: Alexandru Parvulescu <alexparvulescu@apache.org>
Date: Thu, 1 Dec 2011 14:54:37 +0000
Subject: [PATCH] JCR-2906 Multivalued property sorted by last/random value  -
 fixed Java5 compliance issue

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1209111 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/query/lucene/SharedFieldCache.java      | 6 ++++--
 1 file changed, 4 insertions(+), 2 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
index d9d77a673..83dcdd509 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
@@ -163,8 +163,10 @@ public class SharedFieldCache {
 
             // after
             if (index >= offset + c.length) {
                c = Arrays.copyOf(c, index - offset + 1);
                c[index - offset] = item;
                Comparable<?>[] newC = new Comparable[index - offset + 1];
                System.arraycopy(c, 0, newC, 0, c.length);
                newC[index - offset] = item;
                c = newC;
                 return this;
             }
             return this;
- 
2.19.1.windows.1

