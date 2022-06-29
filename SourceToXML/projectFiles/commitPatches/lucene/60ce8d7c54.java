From 60ce8d7c549ef90cd6aaa9297bf31aeb3dd3417e Mon Sep 17 00:00:00 2001
From: Chris Hostetter <hossman@apache.org>
Date: Fri, 9 Sep 2016 11:45:09 -0700
Subject: [PATCH] SOLR-9490: Fixed bugs in BoolField that caused it to
 erroneously return "false" for all docs depending on usage

--
 solr/CHANGES.txt                                       |  2 ++
 .../src/java/org/apache/solr/schema/BoolField.java     | 10 ++++++----
 .../org/apache/solr/client/solrj/SolrExampleTests.java |  7 +++++--
 3 files changed, 13 insertions(+), 6 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 9017af46eea..5bba3ae9a36 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -119,6 +119,8 @@ Bug Fixes
 * SOLR-9488: Shard split can fail to write commit data on shutdown/restart causing replicas to recover
   without replicating the index. This can cause data loss. (shalin)
 
* SOLR-9490: Fixed bugs in BoolField that caused it to erroneously return "false" for all docs depending
  on usage (Colvin Cowie, Dan Fox, hossman)
 
 Optimizations
 ----------------------
diff --git a/solr/core/src/java/org/apache/solr/schema/BoolField.java b/solr/core/src/java/org/apache/solr/schema/BoolField.java
index 1ecdb5961bd..a9acfc5d5d8 100644
-- a/solr/core/src/java/org/apache/solr/schema/BoolField.java
++ b/solr/core/src/java/org/apache/solr/schema/BoolField.java
@@ -128,11 +128,13 @@ public class BoolField extends PrimitiveFieldType {
 
   @Override
   public String toExternal(IndexableField f) {
    if (f.binaryValue() == null) {
      return null;
    if (null != f.binaryValue()) {
      return indexedToReadable(f.binaryValue().utf8ToString());
     }

    return indexedToReadable(f.binaryValue().utf8ToString());
    if (null != f.stringValue()) {
      return indexedToReadable(f.stringValue());
    }
    return null;
   }
 
   @Override
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleTests.java b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleTests.java
index 1dd074edf5d..4f3f83d00fa 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleTests.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleTests.java
@@ -182,12 +182,15 @@ abstract public class SolrExampleTests extends SolrExampleTestsBase
     // test a second query, test making a copy of the main query
     SolrQuery query2 = query.getCopy();
     query2.addFilterQuery("inStock:true");
    Assert.assertFalse(query.getFilterQueries() == query2.getFilterQueries());
     response = client.query( query2 );
     Assert.assertEquals(1, query2.getFilterQueries().length);
     Assert.assertEquals(0, response.getStatus());
     Assert.assertEquals(2, response.getResults().getNumFound() );
    Assert.assertFalse(query.getFilterQueries() == query2.getFilterQueries());

    for (SolrDocument outDoc : response.getResults()) {
      assertEquals(true, outDoc.getFieldValue("inStock"));
    }
    
     // sanity check round tripping of params...
     query = new SolrQuery("foo");
     query.addFilterQuery("{!field f=inStock}true");
- 
2.19.1.windows.1

