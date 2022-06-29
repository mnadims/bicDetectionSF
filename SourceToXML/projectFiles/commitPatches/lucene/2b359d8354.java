From 2b359d8354e86f617efc273b93a39df6538cad31 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Thu, 20 Nov 2014 20:49:08 +0000
Subject: [PATCH] LUCENE-5123: fix changes

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1640808 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 9c2d0d31f9c..1d9d55ab80c 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -41,7 +41,7 @@ New Features
 * LUCENE-5889: Add commit method to AnalyzingInfixSuggester, and allow just using .add
   to build up the suggester.  (Varun Thacker via Mike McCandless)
 
* LUCENE-5123: Add a "push" option to the postings writing API, so
* LUCENE-5123: Add a "pull" option to the postings writing API, so
   that a PostingsFormat now receives a Fields instance and it is
   responsible for iterating through all fields, terms, documents and
   positions.  (Robert Muir, Mike McCandless)
- 
2.19.1.windows.1

