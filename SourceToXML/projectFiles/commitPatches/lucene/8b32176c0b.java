From 8b32176c0b7ce8392edf0c38cc590bf29101d580 Mon Sep 17 00:00:00 2001
From: Uwe Schindler <uschindler@apache.org>
Date: Wed, 4 May 2011 07:05:47 +0000
Subject: [PATCH] SOLR-2493: SolrQueryParser was fixed to not parse the
 SolrConfig DOM tree on each instantiation which is a huge slowdown

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1099340 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                                          | 3 +++
 solr/src/java/org/apache/solr/search/SolrQueryParser.java | 2 +-
 2 files changed, 4 insertions(+), 1 deletion(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 3bef05499ef..831472aa7de 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -312,6 +312,9 @@ Bug Fixes
   did not clear all attributes so they displayed incorrect attribute values for tokens
   in later filter stages. (uschindler, rmuir, yonik)
 
* SOLR-2493: SolrQueryParser was fixed to not parse the SolrConfig DOM tree on each
  instantiation which is a huge slowdown.  (Stephane Bailliez via uschindler)

 Other Changes
 ----------------------
 
diff --git a/solr/src/java/org/apache/solr/search/SolrQueryParser.java b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
index 34192b8fd8b..80db3314c7d 100644
-- a/solr/src/java/org/apache/solr/search/SolrQueryParser.java
++ b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
@@ -67,7 +67,7 @@ public class SolrQueryParser extends QueryParser {
   }
 
   public SolrQueryParser(QParser parser, String defaultField, Analyzer analyzer) {
    super(parser.getReq().getCore().getSolrConfig().getLuceneVersion("luceneMatchVersion", Version.LUCENE_30), defaultField, analyzer);
    super(parser.getReq().getCore().getSolrConfig().luceneMatchVersion, defaultField, analyzer);
     this.schema = parser.getReq().getSchema();
     this.parser = parser;
     this.defaultField = defaultField;
- 
2.19.1.windows.1

