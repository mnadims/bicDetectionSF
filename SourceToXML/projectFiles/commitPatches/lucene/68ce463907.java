From 68ce46390787a8d13ec1da92c1223ed63049eac5 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Sat, 24 Jul 2010 16:49:18 +0000
Subject: [PATCH] LUCENE-2458: implement Uwe's suggestion for restoring Solr's
 default behavior

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@978898 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/src/java/org/apache/solr/search/SolrQueryParser.java | 2 ++
 1 file changed, 2 insertions(+)

diff --git a/solr/src/java/org/apache/solr/search/SolrQueryParser.java b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
index 9882e220dca..3048893bb4a 100644
-- a/solr/src/java/org/apache/solr/search/SolrQueryParser.java
++ b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
@@ -79,6 +79,7 @@ public class SolrQueryParser extends QueryParser {
     this.defaultField = defaultField;
     setLowercaseExpandedTerms(false);
     setEnablePositionIncrements(true);
    setAutoGeneratePhraseQueries(true);
     checkAllowLeadingWildcards();
   }
 
@@ -93,6 +94,7 @@ public class SolrQueryParser extends QueryParser {
     this.defaultField = defaultField;
     setLowercaseExpandedTerms(false);
     setEnablePositionIncrements(true);
    setAutoGeneratePhraseQueries(true);    
     checkAllowLeadingWildcards();
   }
 
- 
2.19.1.windows.1

