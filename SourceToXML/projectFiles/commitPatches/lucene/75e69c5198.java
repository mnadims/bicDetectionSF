From 75e69c5198c02e6635eed274b03ea759ef1c4818 Mon Sep 17 00:00:00 2001
From: Joel <joel.bernstein@alfresco.com>
Date: Thu, 22 Sep 2016 07:58:25 -0400
Subject: [PATCH] SOLR-9549: Fix bug in advancing docValues

--
 .../apache/solr/search/TextLogisticRegressionQParserPlugin.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/solr/core/src/java/org/apache/solr/search/TextLogisticRegressionQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/TextLogisticRegressionQParserPlugin.java
index 96f869faa8a..e1d3b7b8479 100644
-- a/solr/core/src/java/org/apache/solr/search/TextLogisticRegressionQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/TextLogisticRegressionQParserPlugin.java
@@ -153,7 +153,7 @@ public class TextLogisticRegressionQParserPlugin extends QParserPlugin {
     public void collect(int doc) throws IOException{
       int valuesDocID = leafOutcomeValue.docID();
       if (valuesDocID < doc) {
        valuesDocID = leafOutcomeValue.advance(valuesDocID);
        valuesDocID = leafOutcomeValue.advance(doc);
       }
       int outcome;
       if (valuesDocID == doc) {
- 
2.19.1.windows.1

