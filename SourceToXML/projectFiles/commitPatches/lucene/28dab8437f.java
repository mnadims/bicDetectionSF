From 28dab8437f4b2a7b8a47e7bad1cac9dd8f0ef18c Mon Sep 17 00:00:00 2001
From: Steven Rowe <sarowe@apache.org>
Date: Thu, 7 Jul 2011 22:56:20 +0000
Subject: [PATCH] SOLR-2331: fix Windows file deletion failure due to a lock
 held by an unclosed anonymous input stream created for the purpose of debug
 printing; also, wrap the debug printing in an 'if (VERBOSE) { }' block so
 that it doesn't get ordinarily get invoked.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1144088 13f79535-47bb-0310-9956-ffa450edef68
--
 .../client/solrj/embedded/TestSolrProperties.java     | 11 +++++++++--
 1 file changed, 9 insertions(+), 2 deletions(-)

diff --git a/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java b/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java
index 975aa200f84..bf3c1687162 100644
-- a/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java
++ b/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java
@@ -202,8 +202,15 @@ public class TestSolrProperties extends LuceneTestCase {
     assertTrue("should have more recent time: " + after + "," + before, after > before);
 
     mcr = CoreAdminRequest.persist("solr-persist.xml", coreadmin);
    
    System.out.println(IOUtils.toString(new FileInputStream(new File(solrXml.getParent(), "solr-persist.xml"))));

    if (VERBOSE) {
      FileInputStream fis = new FileInputStream(new File(solrXml.getParent(), "solr-persist.xml"));
      try {
        System.out.println(IOUtils.toString(fis));
      } finally {
        fis.close();
      }
    }
     DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
     FileInputStream fis = new FileInputStream(new File(solrXml.getParent(), "solr-persist.xml"));
     try {
- 
2.19.1.windows.1

