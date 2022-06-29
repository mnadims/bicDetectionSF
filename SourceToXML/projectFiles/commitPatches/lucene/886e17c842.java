From 886e17c842fc898e8565b8281b096fe759657bfa Mon Sep 17 00:00:00 2001
From: Doron Cohen <doronc@apache.org>
Date: Sun, 22 May 2011 12:00:42 +0000
Subject: [PATCH] SOLR-2500: TestSolrProperties sometimes fails with "no such
 core: core0"

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1125932 13f79535-47bb-0310-9956-ffa450edef68
--
 .../solrj/embedded/TestSolrProperties.java    | 19 ++++++++++++++-----
 1 file changed, 14 insertions(+), 5 deletions(-)

diff --git a/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java b/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java
index cf3eff2f19a..2f08b1de0f7 100644
-- a/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java
++ b/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java
@@ -37,6 +37,7 @@ import org.apache.solr.client.solrj.request.UpdateRequest;
 import org.apache.solr.client.solrj.request.UpdateRequest.ACTION;
 import org.apache.solr.client.solrj.response.CoreAdminResponse;
 import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.FileUtils;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.util.AbstractSolrTestCase;
 import org.junit.After;
@@ -54,6 +55,7 @@ import org.w3c.dom.Node;
 public class TestSolrProperties extends LuceneTestCase {
   protected static Logger log = LoggerFactory.getLogger(TestSolrProperties.class);
   protected CoreContainer cores = null;
  private File home;
   private File solrXml;
   
   private static final XPathFactory xpathFactory = XPathFactory.newInstance();
@@ -62,21 +64,27 @@ public class TestSolrProperties extends LuceneTestCase {
     return "solr/shared";
   }
 
  public String getSolrXml() {
  public String getOrigSolrXml() {
     return "solr.xml";
   }
 
  public String getSolrXml() {
    return "test-solr.xml";
  }
  
   @Override
   @Before
   public void setUp() throws Exception {
     super.setUp();
     System.setProperty("solr.solr.home", getSolrHome());
     
    File home = SolrTestCaseJ4.getFile(getSolrHome());
    home = SolrTestCaseJ4.getFile(getSolrHome());
     System.setProperty("solr.solr.home", home.getAbsolutePath());
 
     log.info("pwd: " + (new File(".")).getAbsolutePath());
    File origSolrXml = new File(home, getOrigSolrXml());
     solrXml = new File(home, getSolrXml());
    FileUtils.copyFile(origSolrXml, solrXml);
     cores = new CoreContainer(home.getAbsolutePath(), solrXml);
   }
 
@@ -85,7 +93,7 @@ public class TestSolrProperties extends LuceneTestCase {
   public void tearDown() throws Exception {
     if (cores != null)
       cores.shutdown();
    File dataDir = new File(getSolrHome() + "/data");
    File dataDir = new File(home,"data");
     String skip = System.getProperty("solr.test.leavedatadir");
     if (null != skip && 0 != skip.trim().length()) {
       log.info("NOTE: per solr.test.leavedatadir, dataDir will not be removed: " + dataDir.getAbsolutePath());
@@ -94,8 +102,9 @@ public class TestSolrProperties extends LuceneTestCase {
         log.warn("!!!! WARNING: best effort to remove " + dataDir.getAbsolutePath() + " FAILED !!!!!");
       }
     }
    File persistedFile = new File(getSolrHome() + File.separator + "solr-persist.xml");
    persistedFile.delete();
    File persistedFile = new File(home,"solr-persist.xml");
    assertTrue("Failed to delete "+persistedFile, persistedFile.delete());
    assertTrue("Failed to delete "+solrXml, solrXml.delete());
     super.tearDown();
   }
 
- 
2.19.1.windows.1

