From 6e98f5b30eb1bcfba0973cb2a87c7b2c6d91cacc Mon Sep 17 00:00:00 2001
From: Alexandru Parvulescu <alexparvulescu@apache.org>
Date: Mon, 22 Oct 2012 11:50:40 +0000
Subject: [PATCH] JCR-3450 Reduce memory usage of SharedFieldCache.ValueIndex

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1400843 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/SharedFieldCache.java   | 33 ++++++++++++++++---
 1 file changed, 29 insertions(+), 4 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
index 83dcdd509..3fbc80cca 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
@@ -228,7 +228,9 @@ public class SharedFieldCache {
         ValueIndex ret = lookup(reader, field, prefix);
         if (ret == null) {
             final int maxDocs = reader.maxDoc();
            ComparableArray[] retArray = new ComparableArray[maxDocs];
            Comparable<?>[] retArray = new Comparable<?>[maxDocs];
            Map<Integer, Integer> positions = new HashMap<Integer, Integer>();
            boolean usingSimpleComparable = true;
             int setValues = 0;
             if (maxDocs > 0) {
                 IndexFormatVersion version = IndexFormatVersion.getVersion(reader);
@@ -268,11 +270,34 @@ public class SharedFieldCache {
                             setValues++;
                             Comparable<?> v = getValue(value, type);
                             int doc = termDocs.doc();
                            ComparableArray ca = retArray[doc];
                            Comparable<?> ca = retArray[doc];
                             if (ca == null) {
                                retArray[doc] = new ComparableArray(v, termPosition);
                                if (usingSimpleComparable) {
                                    // put simple value on the queue
                                    positions.put(doc, termPosition);
                                    retArray[doc] = v;
                                } else {
                                    retArray[doc] = new ComparableArray(v,
                                            termPosition);
                                }
                             } else {
                                retArray[doc] = ca.insert(v, termPosition);
                                if (ca instanceof ComparableArray) {
                                    ((ComparableArray) ca).insert(v,
                                            termPosition);
                                } else {
                                    // transform all of the existing values from
                                    // Comparable to ComparableArray
                                    for (int pos : positions.keySet()) {
                                        retArray[pos] = new ComparableArray(
                                                retArray[pos],
                                                positions.get(pos));
                                    }
                                    positions = null;
                                    usingSimpleComparable = false;
                                    ComparableArray caNew = (ComparableArray) retArray[doc];
                                    retArray[doc] = caNew.insert(v,
                                            termPosition);
                                }
                             }
                         }
                     } while (termEnum.next());
- 
2.19.1.windows.1

