From 0d31aa07b35b1af4a6f3b6c152f119ad8cca4ba2 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Mon, 5 Jul 2010 22:36:42 +0000
Subject: [PATCH] LUCENE-2410: correct CHANGES entry -- only 20% speedup

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@960719 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 0322541ba96..a894549303b 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -191,7 +191,7 @@ New features
 
 Optimizations
 
* LUCENE-2410: ~2.5X speedup on exact (slop=0) PhraseQuery matching.
* LUCENE-2410: ~20% speedup on exact (slop=0) PhraseQuery matching.
   (Mike McCandless)
   
 ======================= Lucene 3.x (not yet released) =======================
- 
2.19.1.windows.1

