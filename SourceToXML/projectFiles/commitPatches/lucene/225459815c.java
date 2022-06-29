From 225459815caf4b8a2a8fcf81d96a56c090219950 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Sat, 22 Feb 2014 06:19:16 +0000
Subject: [PATCH] SOLR-5762 broke backward compatibility of Javabin format

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1570793 13f79535-47bb-0310-9956-ffa450edef68
--
 .../request/JavaBinUpdateRequestCodec.java    |   8 +-
 .../src/test-files/solrj/updateReq_4_5.bin    | Bin 0 -> 290 bytes
 .../solrj/request/TestUpdateRequestCodec.java |  73 ++++++++++++++++++
 3 files changed, 80 insertions(+), 1 deletion(-)
 create mode 100644 solr/solrj/src/test-files/solrj/updateReq_4_5.bin

diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/request/JavaBinUpdateRequestCodec.java b/solr/solrj/src/java/org/apache/solr/client/solrj/request/JavaBinUpdateRequestCodec.java
index 3bcbba92880..a1fc4c8add0 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/request/JavaBinUpdateRequestCodec.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/request/JavaBinUpdateRequestCodec.java
@@ -184,7 +184,13 @@ public class JavaBinUpdateRequestCodec {
     delByIdMap = (Map<String,Map<String,Object>>) namedList[0].get("delByIdMap");
     delByQ = (List<String>) namedList[0].get("delByQ");
     doclist = (List) namedList[0].get("docs");
    docMap =  (List<Entry<SolrInputDocument,Map<Object,Object>>>) namedList[0].get("docsMap");
    Object docsMapObj = namedList[0].get("docsMap");

    if (docsMapObj instanceof Map) {//SOLR-5762
      docMap =  new ArrayList(((Map)docsMapObj).entrySet());
    } else {
      docMap = (List<Entry<SolrInputDocument, Map<Object, Object>>>) docsMapObj;
    }
     
 
     // we don't add any docs, because they were already processed
diff --git a/solr/solrj/src/test-files/solrj/updateReq_4_5.bin b/solr/solrj/src/test-files/solrj/updateReq_4_5.bin
new file mode 100644
index 0000000000000000000000000000000000000000..b16cb14630f50ccca49a1afce5f89cb9132a9652
GIT binary patch
literal 290
zcmZQN`arE9u_!UOc;N%ZM8%{BS}CbHPL-Z1zKI1~%ny{cth5*&sAQ&CnKC?3G-7z5
z29^$NRx;93GSmV|7(Y-+$xklk6JX-7Z(v|}pp=>7$l<^MWT~X27AH3;=jWv=8Uke<
z6&M&^I&nAv<z6)@mz3u#8bS5EbcTwmmShyArYaf>Fha~yOUutsN-SzpNlL6T1QAC3
w57a@nfn3J|(+#l>XeQVu2JBXGF+EVqFK2w9lw9r%(}->@1JvHvNER{x03GsFRsaA1

literal 0
HcmV?d00001

diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestUpdateRequestCodec.java b/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestUpdateRequestCodec.java
index 5a66759b0b5..543dacdd314 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestUpdateRequestCodec.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestUpdateRequestCodec.java
@@ -18,6 +18,9 @@ package org.apache.solr.client.solrj.request;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
@@ -31,6 +34,7 @@ import junit.framework.Assert;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.SolrInputField;
import org.apache.solr.util.ExternalPaths;
 import org.junit.Test;
 
 /**
@@ -160,6 +164,75 @@ public class TestUpdateRequestCodec extends LuceneTestCase {
 
 
 
  public void testBackCompat4_5() throws IOException {

    UpdateRequest updateRequest = new UpdateRequest();
    updateRequest.deleteById("*:*");
    updateRequest.deleteById("id:5");
    updateRequest.deleteByQuery("2*");
    updateRequest.deleteByQuery("1*");
    updateRequest.setParam("a", "b");
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField("id", 1);
    doc.addField("desc", "one", 2.0f);
    doc.addField("desc", "1");
    updateRequest.add(doc);

    doc = new SolrInputDocument();
    doc.addField("id", 2);
    doc.setDocumentBoost(10.0f);
    doc.addField("desc", "two", 3.0f);
    doc.addField("desc", "2");
    updateRequest.add(doc);

    doc = new SolrInputDocument();
    doc.addField("id", 3);
    doc.addField("desc", "three", 3.0f);
    doc.addField("desc", "3");
    updateRequest.add(doc);

    doc = new SolrInputDocument();
    Collection<String> foobar = new HashSet<String>();
    foobar.add("baz1");
    foobar.add("baz2");
    doc.addField("foobar",foobar);
    updateRequest.add(doc);

    updateRequest.deleteById("2");
    updateRequest.deleteByQuery("id:3");



    FileInputStream is = new FileInputStream(new File(ExternalPaths.SOURCE_HOME, "solrj/src/test-files/solrj/updateReq_4_5.bin"));
    UpdateRequest updateUnmarshalled = new JavaBinUpdateRequestCodec().unmarshal(is, new JavaBinUpdateRequestCodec.StreamingUpdateHandler() {
      @Override
      public void update(SolrInputDocument document, UpdateRequest req, Integer commitWithin, Boolean override) {
        if(commitWithin == null ){
                    req.add(document);
        }
        System.err.println("Doc" + document + " ,commitWithin:"+commitWithin+ " , override:"+ override);
      }
    });

    System.err.println(updateUnmarshalled.getDocumentsMap());
    System.err.println(updateUnmarshalled.getDocuments());

    for (int i = 0; i < updateRequest.getDocuments().size(); i++) {
      SolrInputDocument inDoc = updateRequest.getDocuments().get(i);
      SolrInputDocument outDoc = updateUnmarshalled.getDocuments().get(i);
      compareDocs("doc#"+i, inDoc, outDoc);
    }
    Assert.assertEquals(updateUnmarshalled.getDeleteById().get(0) ,
        updateRequest.getDeleteById().get(0));
    Assert.assertEquals(updateUnmarshalled.getDeleteQuery().get(0) ,
        updateRequest.getDeleteQuery().get(0));

    assertEquals("b", updateUnmarshalled.getParams().get("a"));
    is.close();
  }



   private void compareDocs(String m, 
                            SolrInputDocument expectedDoc, 
                            SolrInputDocument actualDoc) {
- 
2.19.1.windows.1

