From 249c678054c32abc069ea25000b3d839ed68c220 Mon Sep 17 00:00:00 2001
From: Robert Muir <rmuir@apache.org>
Date: Sat, 24 Jul 2010 16:22:51 +0000
Subject: [PATCH] LUCENE-2458: revert r978879

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@978891 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/src/java/org/apache/solr/search/SolrQueryParser.java | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/SolrQueryParser.java b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
index 6bc0353a369..9882e220dca 100644
-- a/solr/src/java/org/apache/solr/search/SolrQueryParser.java
++ b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
@@ -73,7 +73,7 @@ public class SolrQueryParser extends QueryParser {
    * @see IndexSchema#getDefaultSearchFieldName()
    */
   public SolrQueryParser(IndexSchema schema, String defaultField) {
    super(Version.LUCENE_24, defaultField == null ? schema.getDefaultSearchFieldName() : defaultField, schema.getQueryAnalyzer());
    super(schema.getSolrConfig().getLuceneVersion("luceneMatchVersion", Version.LUCENE_24), defaultField == null ? schema.getDefaultSearchFieldName() : defaultField, schema.getQueryAnalyzer());
     this.schema = schema;
     this.parser  = null;
     this.defaultField = defaultField;
@@ -87,7 +87,7 @@ public class SolrQueryParser extends QueryParser {
   }
 
   public SolrQueryParser(QParser parser, String defaultField, Analyzer analyzer) {
    super(Version.LUCENE_24, defaultField, analyzer);
    super(parser.getReq().getSchema().getSolrConfig().getLuceneVersion("luceneMatchVersion", Version.LUCENE_24), defaultField, analyzer);
     this.schema = parser.getReq().getSchema();
     this.parser = parser;
     this.defaultField = defaultField;
- 
2.19.1.windows.1

