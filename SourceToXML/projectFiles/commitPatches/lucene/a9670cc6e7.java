From a9670cc6e7951e02d5a8aa0a66afd8612bed9c42 Mon Sep 17 00:00:00 2001
From: Mark Robert Miller <markrmiller@apache.org>
Date: Fri, 24 Jan 2014 00:53:06 +0000
Subject: [PATCH] SOLR-5658: commitWithin and overwrite are not being
 distributed to replicas now that SolrCloud uses javabin to distribute
 updates.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1560859 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  4 +
 .../solr/handler/loader/JavabinLoader.java    |  9 ++-
 .../solr/cloud/BasicDistributedZkTest.java    | 20 ++++-
 .../request/JavaBinUpdateRequestCodec.java    | 76 +++++++++----------
 .../client/solrj/request/UpdateRequest.java   |  2 +-
 .../apache/solr/common/util/JavaBinCodec.java | 41 ++++++++++
 .../solrj/request/TestUpdateRequestCodec.java |  4 +-
 7 files changed, 111 insertions(+), 45 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 7c51c9106b0..af1fbdd60fe 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -401,6 +401,10 @@ Bug Fixes
 * SOLR-5636: SolrRequestParsers does some xpath lookups on every request, which
   can cause concurrency issues. (Mark Miller)
 
* SOLR-5658: commitWithin and overwrite are not being distributed to replicas
  now that SolrCloud uses javabin to distribute updates.
  (Mark Miller, Varun Thacker, Elodie Sannier, shalin)

 Optimizations
 ----------------------  
 
diff --git a/solr/core/src/java/org/apache/solr/handler/loader/JavabinLoader.java b/solr/core/src/java/org/apache/solr/handler/loader/JavabinLoader.java
index d6671414ccf..64b851f786d 100644
-- a/solr/core/src/java/org/apache/solr/handler/loader/JavabinLoader.java
++ b/solr/core/src/java/org/apache/solr/handler/loader/JavabinLoader.java
@@ -70,7 +70,7 @@ public class JavabinLoader extends ContentStreamLoader {
       private AddUpdateCommand addCmd = null;
 
       @Override
      public void update(SolrInputDocument document, UpdateRequest updateRequest) {
      public void update(SolrInputDocument document, UpdateRequest updateRequest, Integer commitWithin, Boolean overwrite) {
         if (document == null) {
           // Perhaps commit from the parameters
           try {
@@ -85,6 +85,13 @@ public class JavabinLoader extends ContentStreamLoader {
           addCmd = getAddCommand(req, updateRequest.getParams());
         }
         addCmd.solrDoc = document;
        if (commitWithin != null) {
          addCmd.commitWithin = commitWithin;
        }
        if (overwrite != null) {
          addCmd.overwrite = overwrite;
        }
        
         try {
           processor.processAdd(addCmd);
           addCmd.clear();
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
index 0da31f46461..607c22001e0 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
@@ -21,12 +21,12 @@ import java.io.File;
 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.util.ArrayList;
import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
import java.util.Collections;
 import java.util.concurrent.Callable;
 import java.util.concurrent.CompletionService;
 import java.util.concurrent.ExecutorCompletionService;
@@ -336,6 +336,24 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     query(false, new Object[] {"q", "id:[1 TO 5]", CommonParams.DEBUG, CommonParams.RESULTS});
     query(false, new Object[] {"q", "id:[1 TO 5]", CommonParams.DEBUG, CommonParams.QUERY});
 
    // try commitWithin
    long before = cloudClient.query(new SolrQuery("*:*")).getResults().getNumFound();
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set("commitWithin", 10);
    add(cloudClient, params , getDoc("id", 300));
    
    long timeout = System.currentTimeMillis() + 15000;
    while (cloudClient.query(new SolrQuery("*:*")).getResults().getNumFound() != before + 1) {
      if (timeout <= System.currentTimeMillis()) {
        fail("commitWithin did not work");
      }
      Thread.sleep(100);
    }
    
    for (SolrServer client : clients) {
      assertEquals("commitWithin did not work", before + 1, client.query(new SolrQuery("*:*")).getResults().getNumFound());
    }
    
     // TODO: This test currently fails because debug info is obtained only
     // on shards with matches.
     // query("q","matchesnothing","fl","*,score", "debugQuery", "true");
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/request/JavaBinUpdateRequestCodec.java b/solr/solrj/src/java/org/apache/solr/client/solrj/request/JavaBinUpdateRequestCodec.java
index 5998eeccb80..744aac7245c 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/request/JavaBinUpdateRequestCodec.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/request/JavaBinUpdateRequestCodec.java
@@ -25,7 +25,6 @@ import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
import java.util.Set;
 
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.params.ModifiableSolrParams;
@@ -60,9 +59,6 @@ public class JavaBinUpdateRequestCodec {
     }
     Iterator<SolrInputDocument> docIter = null;
 
    if (updateRequest.getDocuments() != null) {
      docIter = updateRequest.getDocuments().iterator();
    }
     if(updateRequest.getDocIterator() != null){
       docIter = updateRequest.getDocIterator();
     }
@@ -70,10 +66,19 @@ public class JavaBinUpdateRequestCodec {
     Map<SolrInputDocument,Map<String,Object>> docMap = updateRequest.getDocumentsMap();
 
     nl.add("params", params);// 0: params
    nl.add("delByIdMap", updateRequest.getDeleteByIdMap());
    if (updateRequest.getDeleteByIdMap() != null) {
      nl.add("delByIdMap", updateRequest.getDeleteByIdMap());
    }
     nl.add("delByQ", updateRequest.getDeleteQuery());
    nl.add("docs", docIter);
    nl.add("docsMap", docMap);

    if (docMap != null) {
      nl.add("docsMap", docMap.entrySet().iterator());
    } else {
      if (updateRequest.getDocuments() != null) {
        docIter = updateRequest.getDocuments().iterator();
      }
      nl.add("docs", docIter);
    }
     JavaBinCodec codec = new JavaBinCodec();
     codec.marshal(nl, os);
   }
@@ -92,7 +97,7 @@ public class JavaBinUpdateRequestCodec {
   public UpdateRequest unmarshal(InputStream is, final StreamingUpdateHandler handler) throws IOException {
     final UpdateRequest updateRequest = new UpdateRequest();
     List<List<NamedList>> doclist;
    Map<SolrInputDocument,Map<String,Object>>  docMap;
    List<Entry<SolrInputDocument,Map<Object,Object>>>  docMap;
     List<String> delById;
     Map<String,Map<String,Object>> delByIdMap;
     List<String> delByQ;
@@ -132,9 +137,11 @@ public class JavaBinUpdateRequestCodec {
       }
 
       private List readOuterMostDocIterator(DataInputInputStream fis) throws IOException {
        NamedList params = (NamedList) namedList[0].getVal(0);
        NamedList params = (NamedList) namedList[0].get("params");
         updateRequest.setParams(new ModifiableSolrParams(SolrParams.toSolrParams(params)));
         if (handler == null) return super.readIterator(fis);
        Integer commitWithin = null;
        Boolean overwrite = null;
         while (true) {
           Object o = readVal(fis);
           if (o == END_OBJ) break;
@@ -144,16 +151,24 @@ public class JavaBinUpdateRequestCodec {
           } else if (o instanceof NamedList)  {
             UpdateRequest req = new UpdateRequest();
             req.setParams(new ModifiableSolrParams(SolrParams.toSolrParams((NamedList) o)));
            handler.update(null, req);
            handler.update(null, req, null, null);
          } else if (o instanceof Map.Entry){
            sdoc = (SolrInputDocument) ((Map.Entry) o).getKey();
            Map p = (Map) ((Map.Entry) o).getValue();
            if (p != null) {
              commitWithin = (Integer) p.get(UpdateRequest.COMMIT_WITHIN);
              overwrite = (Boolean) p.get(UpdateRequest.OVERWRITE);
            }
           } else  {
          
             sdoc = (SolrInputDocument) o;
           }
          handler.update(sdoc, updateRequest);
          handler.update(sdoc, updateRequest, commitWithin, overwrite);
         }
         return Collections.EMPTY_LIST;
       }
    };
 
    };
 
     codec.unmarshal(is);
     
@@ -161,6 +176,7 @@ public class JavaBinUpdateRequestCodec {
     // must be loaded now
     if(updateRequest.getParams()==null) {
       NamedList params = (NamedList) namedList[0].get("params");
      System.out.println("unmarchal params:" + params);
       if(params!=null) {
         updateRequest.setParams(new ModifiableSolrParams(SolrParams.toSolrParams(params)));
       }
@@ -169,32 +185,12 @@ public class JavaBinUpdateRequestCodec {
     delByIdMap = (Map<String,Map<String,Object>>) namedList[0].get("delByIdMap");
     delByQ = (List<String>) namedList[0].get("delByQ");
     doclist = (List) namedList[0].get("docs");
    docMap =  (Map<SolrInputDocument,Map<String,Object>>) namedList[0].get("docsMap");
    docMap =  (List<Entry<SolrInputDocument,Map<Object,Object>>>) namedList[0].get("docsMap");
    

    // we don't add any docs, because they were already processed
    // deletes are handled later, and must be passed back on the UpdateRequest
     
    if (doclist != null && !doclist.isEmpty()) {
      List<SolrInputDocument> solrInputDocs = new ArrayList<SolrInputDocument>();
      for (Object o : doclist) {
        if (o instanceof List) {
          solrInputDocs.add(listToSolrInputDocument((List<NamedList>)o));
        } else  {
          solrInputDocs.add((SolrInputDocument)o);
        }
      }
      updateRequest.add(solrInputDocs);
    }
    if (docMap != null && !docMap.isEmpty()) {
      Set<Entry<SolrInputDocument,Map<String,Object>>> entries = docMap.entrySet();
      for (Entry<SolrInputDocument,Map<String,Object>> entry : entries) {
        Map<String,Object> map = entry.getValue();
        Boolean overwrite = null;
        Integer commitWithin = null;
        if (map != null) {
          overwrite = (Boolean) map.get(UpdateRequest.OVERWRITE);
          commitWithin = (Integer) map.get(UpdateRequest.COMMIT_WITHIN);
        }
        updateRequest.add(entry.getKey(), commitWithin, overwrite);
      }
    }
     if (delById != null) {
       for (String s : delById) {
         updateRequest.deleteById(s);
@@ -204,7 +200,7 @@ public class JavaBinUpdateRequestCodec {
       for (Map.Entry<String,Map<String,Object>> entry : delByIdMap.entrySet()) {
         Map<String,Object> params = entry.getValue();
         if (params != null) {
          Long version = (Long) params.get("ver");
          Long version = (Long) params.get(UpdateRequest.VER);
           updateRequest.deleteById(entry.getKey(), version);
         } else {
           updateRequest.deleteById(entry.getKey());
@@ -217,8 +213,8 @@ public class JavaBinUpdateRequestCodec {
         updateRequest.deleteByQuery(s);
       }
     }
    
     return updateRequest;

   }
 
   private SolrInputDocument listToSolrInputDocument(List<NamedList> namedList) {
@@ -242,6 +238,6 @@ public class JavaBinUpdateRequestCodec {
   }
 
   public static interface StreamingUpdateHandler {
    public void update(SolrInputDocument document, UpdateRequest req);
    public void update(SolrInputDocument document, UpdateRequest req, Integer commitWithin, Boolean override);
   }
 }
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/request/UpdateRequest.java b/solr/solrj/src/java/org/apache/solr/client/solrj/request/UpdateRequest.java
index 81befbe1b3c..154d7e090f1 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/request/UpdateRequest.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/request/UpdateRequest.java
@@ -47,7 +47,7 @@ import org.apache.solr.common.util.XML;
  */
 public class UpdateRequest extends AbstractUpdateRequest {
   
  private static final String VER = "ver";
  public static final String VER = "ver";
   public static final String OVERWRITE = "ow";
   public static final String COMMIT_WITHIN = "cw";
   private Map<SolrInputDocument,Map<String,Object>> documents = null;
diff --git a/solr/solrj/src/java/org/apache/solr/common/util/JavaBinCodec.java b/solr/solrj/src/java/org/apache/solr/common/util/JavaBinCodec.java
index f6e322861bb..3e83f24a32f 100644
-- a/solr/solrj/src/java/org/apache/solr/common/util/JavaBinCodec.java
++ b/solr/solrj/src/java/org/apache/solr/common/util/JavaBinCodec.java
@@ -27,6 +27,7 @@ import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.util.*;
import java.util.Map.Entry;
 import java.nio.ByteBuffer;
 
 /**
@@ -65,6 +66,7 @@ public class JavaBinCodec {
           SOLRINPUTDOC = 16,
           SOLRINPUTDOC_CHILDS = 17,
           ENUM_FIELD_VALUE = 18,
          MAP_ENTRY = 19,
           // types that combine tag + length (or other info) in a single byte
           TAG_AND_LEN = (byte) (1 << 5),
           STR = (byte) (1 << 5),
@@ -227,6 +229,8 @@ public class JavaBinCodec {
         return readSolrInputDocument(dis);
       case ENUM_FIELD_VALUE:
         return readEnumFieldValue(dis);
      case MAP_ENTRY:
        return readMapEntry(dis);
     }
 
     throw new RuntimeException("Unknown type " + tagByte);
@@ -286,6 +290,10 @@ public class JavaBinCodec {
       writeEnumFieldValue((EnumFieldValue) val);
       return true;
     }
    if (val instanceof Map.Entry) {
      writeMapEntry((Map.Entry)val);
      return true;
    }
     return false;
   }
 
@@ -480,6 +488,12 @@ public class JavaBinCodec {
     writeInt(enumFieldValue.toInt());
     writeStr(enumFieldValue.toString());
   }
  
  public void writeMapEntry(Entry<Object,Object> val) throws IOException {
    writeTag(MAP_ENTRY);
    writeVal(val.getKey());
    writeVal(val.getValue());
  }
 
   /**
    * read {@link EnumFieldValue} (int+string) from input stream
@@ -491,6 +505,33 @@ public class JavaBinCodec {
     String stringValue = (String) readVal(dis);
     return new EnumFieldValue(intValue, stringValue);
   }
  

  public Map.Entry<Object,Object> readMapEntry(DataInputInputStream dis) throws IOException {
    final Object key = readVal(dis);
    final Object value = readVal(dis);
    return new Map.Entry<Object,Object>() {

      @Override
      public Object getKey() {
        return key;
      }

      @Override
      public Object getValue() {
        return value;
      }
      
      @Override
      public String toString() {
        return "MapEntry[" + key.toString() + ":" + value.toString() + "]";
      }

      @Override
      public Object setValue(Object value) {
        throw new UnsupportedOperationException();
      }};
  }
 
   /**
    * write the string as tag+length, with length being the number of UTF-8 bytes
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestUpdateRequestCodec.java b/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestUpdateRequestCodec.java
index f9b3d56dc80..5a66759b0b5 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestUpdateRequestCodec.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestUpdateRequestCodec.java
@@ -85,7 +85,7 @@ public class TestUpdateRequestCodec extends LuceneTestCase {
     final List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
     JavaBinUpdateRequestCodec.StreamingUpdateHandler handler = new JavaBinUpdateRequestCodec.StreamingUpdateHandler() {
       @Override
      public void update(SolrInputDocument document, UpdateRequest req) {
      public void update(SolrInputDocument document, UpdateRequest req, Integer commitWithin, Boolean overwrite) {
         Assert.assertNotNull(req.getParams());
         docs.add(document);
       }
@@ -136,7 +136,7 @@ public class TestUpdateRequestCodec extends LuceneTestCase {
     final List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
     JavaBinUpdateRequestCodec.StreamingUpdateHandler handler = new JavaBinUpdateRequestCodec.StreamingUpdateHandler() {
       @Override
      public void update(SolrInputDocument document, UpdateRequest req) {
      public void update(SolrInputDocument document, UpdateRequest req, Integer commitWithin, Boolean overwrite) {
         Assert.assertNotNull(req.getParams());
         docs.add(document);
       }
- 
2.19.1.windows.1

