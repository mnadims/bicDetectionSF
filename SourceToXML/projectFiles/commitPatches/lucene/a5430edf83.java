From a5430edf83ee9bd96ef4adbb7f0d7596c891e272 Mon Sep 17 00:00:00 2001
From: Robert Muir <rmuir@apache.org>
Date: Sat, 24 Jul 2010 16:56:27 +0000
Subject: [PATCH] LUCENE-2458: revert r978898

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@978900 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/src/java/org/apache/solr/search/SolrQueryParser.java | 2 --
 1 file changed, 2 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/SolrQueryParser.java b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
index 3048893bb4a..9882e220dca 100644
-- a/solr/src/java/org/apache/solr/search/SolrQueryParser.java
++ b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
@@ -79,7 +79,6 @@ public class SolrQueryParser extends QueryParser {
     this.defaultField = defaultField;
     setLowercaseExpandedTerms(false);
     setEnablePositionIncrements(true);
    setAutoGeneratePhraseQueries(true);
     checkAllowLeadingWildcards();
   }
 
@@ -94,7 +93,6 @@ public class SolrQueryParser extends QueryParser {
     this.defaultField = defaultField;
     setLowercaseExpandedTerms(false);
     setEnablePositionIncrements(true);
    setAutoGeneratePhraseQueries(true);    
     checkAllowLeadingWildcards();
   }
 
- 
2.19.1.windows.1

