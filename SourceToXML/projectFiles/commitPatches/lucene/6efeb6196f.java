From 6efeb6196f65f4a7d59ea24672136c65ddd7b335 Mon Sep 17 00:00:00 2001
From: Koji Sekiguchi <koji@apache.org>
Date: Mon, 1 Mar 2010 14:27:45 +0000
Subject: [PATCH] SOLR-1297: fix for more deep function

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@917547 13f79535-47bb-0310-9956-ffa450edef68
--
 src/java/org/apache/solr/search/QueryParsing.java     | 2 +-
 src/test/org/apache/solr/search/QueryParsingTest.java | 6 ++++++
 2 files changed, 7 insertions(+), 1 deletion(-)

diff --git a/src/java/org/apache/solr/search/QueryParsing.java b/src/java/org/apache/solr/search/QueryParsing.java
index ea721f72a94..e7eef5467a6 100644
-- a/src/java/org/apache/solr/search/QueryParsing.java
++ b/src/java/org/apache/solr/search/QueryParsing.java
@@ -264,7 +264,7 @@ public class QueryParsing {
             needOrder = false;
           }
         }
      } else if (chars[i] == '(' && functionDepth == 0) {
      } else if (chars[i] == '(' && functionDepth >= 0) {
         buffer.append(chars[i]);
         functionDepth++;
       } else if (chars[i] == ')' && functionDepth > 0) {
diff --git a/src/test/org/apache/solr/search/QueryParsingTest.java b/src/test/org/apache/solr/search/QueryParsingTest.java
index af4ec9daea2..637253c5c05 100644
-- a/src/test/org/apache/solr/search/QueryParsingTest.java
++ b/src/test/org/apache/solr/search/QueryParsingTest.java
@@ -100,6 +100,12 @@ public class QueryParsingTest extends AbstractSolrTestCase {
     //Not thrilled about the fragility of string matching here, but...
     //the value sources get wrapped, so the out field is different than the input
     assertEquals(flds[0].getField(), "pow(float(weight),const(2.0))");
    
    //test functions (more deep)
    sort = QueryParsing.parseSort("sum(product(r_f,sum(d_f,t_f,1)),a_f) asc", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.CUSTOM);
    assertEquals(flds[0].getField(), "sum(product(float(r_f),sum(float(d_f),float(t_f),const(1.0))),float(a_f))");
 
     sort = QueryParsing.parseSort("pow(weight,                 2)         desc", schema);
     flds = sort.getSort();
- 
2.19.1.windows.1

