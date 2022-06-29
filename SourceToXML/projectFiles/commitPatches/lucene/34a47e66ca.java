From 34a47e66ca217f70cc76c56ab5ecd692343a62af Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Thu, 11 Sep 2014 19:40:04 +0000
Subject: [PATCH] SOLR-6501: Binary Response Writer does not return wildcard
 fields

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1624370 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  3 +
 .../solr/response/BinaryResponseWriter.java   |  2 +-
 .../solr/client/solrj/SolrExampleTests.java   | 58 +++++++++++++++++++
 3 files changed, 62 insertions(+), 1 deletion(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index c50b46c1e06..e7b01712d91 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -194,6 +194,9 @@ Bug Fixes
 * SOLR-6499: Log warning about multiple update request handlers
   (Noble Paul, Andreas Hubold, hossman)
 
* SOLR-6501: Binary Response Writer does not return wildcard fields.
  (Mike Hugo, Constantin Mitocaru, sarowe, shalin)

 Other Changes
 ---------------------
 
diff --git a/solr/core/src/java/org/apache/solr/response/BinaryResponseWriter.java b/solr/core/src/java/org/apache/solr/response/BinaryResponseWriter.java
index 25b1909922e..99dada83473 100644
-- a/solr/core/src/java/org/apache/solr/response/BinaryResponseWriter.java
++ b/solr/core/src/java/org/apache/solr/response/BinaryResponseWriter.java
@@ -142,7 +142,7 @@ public class BinaryResponseWriter implements BinaryQueryResponseWriter {
       }
       
       Set<String> fnames = returnFields.getLuceneFieldNames();
      boolean onlyPseudoFields = (fnames == null && !returnFields.wantsAllFields())
      boolean onlyPseudoFields = (fnames == null && !returnFields.wantsAllFields() && !returnFields.hasPatternMatching())
           || (fnames != null && fnames.size() == 1 && SolrReturnFields.SCORE.equals(fnames.iterator().next()));
       context.iterator = ids.iterator();
       for (int i = 0; i < sz; i++) {
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleTests.java b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleTests.java
index 2195e3e1b62..bc9cccf032c 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleTests.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleTests.java
@@ -1396,6 +1396,64 @@ abstract public class SolrExampleTests extends SolrExampleTestsBase
     }
   }
 
  @Test
  public void testFieldGlobbing() throws Exception  {
    SolrServer server = getSolrServer();

    SolrInputDocument doc = new SolrInputDocument();
    doc.addField("id", "testFieldGlobbing");
    doc.addField("x_s", "x");
    doc.addField("y_s", "y");
    doc.addField("z_s", "z");
    server.add(doc);
    server.commit();

    // id and glob
    QueryResponse response = server.query(new SolrQuery("id:testFieldGlobbing").addField("id").addField("*_s"));
    assertEquals("Document not found", 1, response.getResults().getNumFound());
    assertEquals("All requested fields were not returned", 4, response.getResults().get(0).getFieldNames().size());

    // just globs
    response = server.query(new SolrQuery("id:testFieldGlobbing").addField("*_s"));
    assertEquals("Document not found", 1, response.getResults().getNumFound());
    assertEquals("All requested fields were not returned", 3, response.getResults().get(0).getFieldNames().size());

    // just id
    response = server.query(new SolrQuery("id:testFieldGlobbing").addField("id"));
    assertEquals("Document not found", 1, response.getResults().getNumFound());
    assertEquals("All requested fields were not returned", 1, response.getResults().get(0).getFieldNames().size());

    // id and pseudo field and glob
    response = server.query(new SolrQuery("id:testFieldGlobbing").addField("id").addField("[docid]").addField("*_s"));
    assertEquals("Document not found", 1, response.getResults().getNumFound());
    assertEquals("All requested fields were not returned", 5, response.getResults().get(0).getFieldNames().size());

    // pseudo field and glob
    response = server.query(new SolrQuery("id:testFieldGlobbing").addField("[docid]").addField("*_s"));
    assertEquals("Document not found", 1, response.getResults().getNumFound());
    assertEquals("All requested fields were not returned", 4, response.getResults().get(0).getFieldNames().size());

    // just a pseudo field
    response = server.query(new SolrQuery("id:testFieldGlobbing").addField("[docid]"));
    assertEquals("Document not found", 1, response.getResults().getNumFound());
    assertEquals("All requested fields were not returned", 1, response.getResults().get(0).getFieldNames().size());

    // only score
    response = server.query(new SolrQuery("id:testFieldGlobbing").addField("score"));
    assertEquals("Document not found", 1, response.getResults().getNumFound());
    assertEquals("All requested fields were not returned", 1, response.getResults().get(0).getFieldNames().size());

    // pseudo field and score
    response = server.query(new SolrQuery("id:testFieldGlobbing").addField("score").addField("[docid]"));
    assertEquals("Document not found", 1, response.getResults().getNumFound());
    assertEquals("All requested fields were not returned", 2, response.getResults().get(0).getFieldNames().size());

    // score and globs
    response = server.query(new SolrQuery("id:testFieldGlobbing").addField("score").addField("*_s"));
    assertEquals("Document not found", 1, response.getResults().getNumFound());
    assertEquals("All requested fields were not returned", 4, response.getResults().get(0).getFieldNames().size());
  }

   /** 
    * Depth first search of a SolrInputDocument looking for a decendent by id, 
    * returns null if it's not a decendent 
- 
2.19.1.windows.1

