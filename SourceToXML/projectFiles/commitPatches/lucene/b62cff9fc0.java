From b62cff9fc0b9071b799cdca827b0d5be64c64a41 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Sat, 24 Jul 2010 14:38:58 +0000
Subject: [PATCH] LUCENE-2458: keep Solr's default QP behavior wrt compound
 words and phrases

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@978879 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/src/java/org/apache/solr/search/SolrQueryParser.java | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/SolrQueryParser.java b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
index 9882e220dca..6bc0353a369 100644
-- a/solr/src/java/org/apache/solr/search/SolrQueryParser.java
++ b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
@@ -73,7 +73,7 @@ public class SolrQueryParser extends QueryParser {
    * @see IndexSchema#getDefaultSearchFieldName()
    */
   public SolrQueryParser(IndexSchema schema, String defaultField) {
    super(schema.getSolrConfig().getLuceneVersion("luceneMatchVersion", Version.LUCENE_24), defaultField == null ? schema.getDefaultSearchFieldName() : defaultField, schema.getQueryAnalyzer());
    super(Version.LUCENE_24, defaultField == null ? schema.getDefaultSearchFieldName() : defaultField, schema.getQueryAnalyzer());
     this.schema = schema;
     this.parser  = null;
     this.defaultField = defaultField;
@@ -87,7 +87,7 @@ public class SolrQueryParser extends QueryParser {
   }
 
   public SolrQueryParser(QParser parser, String defaultField, Analyzer analyzer) {
    super(parser.getReq().getSchema().getSolrConfig().getLuceneVersion("luceneMatchVersion", Version.LUCENE_24), defaultField, analyzer);
    super(Version.LUCENE_24, defaultField, analyzer);
     this.schema = parser.getReq().getSchema();
     this.parser = parser;
     this.defaultField = defaultField;
- 
2.19.1.windows.1

