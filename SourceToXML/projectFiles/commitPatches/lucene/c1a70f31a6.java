From c1a70f31a605ac254c4c5d556444659aaa3201e5 Mon Sep 17 00:00:00 2001
From: yonik <yonik@apache.org>
Date: Mon, 11 Apr 2016 23:51:41 -0400
Subject: [PATCH] LUCENE-7188: remove incorrect sanity check in
 NRTCachingDirectory.listAll() that throws IllegalStateException

--
 lucene/CHANGES.txt                                           | 4 ++++
 .../java/org/apache/lucene/store/NRTCachingDirectory.java    | 5 +----
 2 files changed, 5 insertions(+), 4 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 6b549f2d76f..e371f25aabb 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -76,6 +76,10 @@ Bug Fixes
 * LUCENE-7187: Block join queries' Weight#extractTerms(...) implementations
   should delegate to the wrapped weight. (Martijn van Groningen)
 
* LUCENE-7188: remove incorrect sanity check in NRTCachingDirectory.listAll()
  that led to IllegalStateException being thrown when nothing was wrong.
  (David Smiley, yonik)  

 Other
 
 * LUCENE-7174: Upgrade randomizedtesting to 2.3.4. (Uwe Schindler, Dawid Weiss)
diff --git a/lucene/core/src/java/org/apache/lucene/store/NRTCachingDirectory.java b/lucene/core/src/java/org/apache/lucene/store/NRTCachingDirectory.java
index 22a9571b76b..9be0b9e9c91 100644
-- a/lucene/core/src/java/org/apache/lucene/store/NRTCachingDirectory.java
++ b/lucene/core/src/java/org/apache/lucene/store/NRTCachingDirectory.java
@@ -101,10 +101,7 @@ public class NRTCachingDirectory extends FilterDirectory implements Accountable
       files.add(f);
     }
     for(String f : in.listAll()) {
      if (!files.add(f)) {
        throw new IllegalStateException("file: " + in + " appears both in delegate and in cache: " +
                                        "cache=" + Arrays.toString(cache.listAll()) + ",delegate=" + Arrays.toString(in.listAll()));
      }
      files.add(f);
     }
     String[] result = files.toArray(new String[files.size()]);
     Arrays.sort(result);
- 
2.19.1.windows.1

