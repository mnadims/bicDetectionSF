From e312ee6bfa89b0658f86b11c9724e9b9a6d91c04 Mon Sep 17 00:00:00 2001
From: James Dyer <jdyer@apache.org>
Date: Fri, 14 Sep 2012 15:19:40 +0000
Subject: [PATCH] SOLR-3779: fix for DIH LineEntityProcessor

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1384816 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  4 ++
 .../handler/dataimport/EntityProcessor.java   |  3 +-
 .../dataimport/LineEntityProcessor.java       | 18 +++--
 .../TestFileListWithLineEntityProcessor.java  | 66 +++++++++++++++++++
 4 files changed, 84 insertions(+), 7 deletions(-)
 create mode 100644 solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListWithLineEntityProcessor.java

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 772530a3c12..f7e7b24aa9b 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -167,6 +167,10 @@ Bug Fixes
 
 * SOLR-3811: Query Form using wrong values for dismax, edismax (steffkes)
 
* SOLR-3779: DataImportHandler's LineEntityProcessor when used in conjunction 
  with FileListEntityProcessor would only process the first file.
  (Ahmet Arslan via James Dyer)

 Other Changes
 ----------------------
 
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessor.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessor.java
index 81f68c44fe4..5122dcb0bea 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessor.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessor.java
@@ -89,8 +89,7 @@ public abstract class EntityProcessor {
   public abstract Map<String, Object> nextModifiedParentRowKey();
 
   /**
   * Invoked for each parent-row after the last row for this entity is processed. If this is the root-most
   * entity, it will be called only once in the import, at the very end.
   * Invoked for each entity at the very end of the import to do any needed cleanup tasks.
    * 
    */
   public abstract void destroy();
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/LineEntityProcessor.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/LineEntityProcessor.java
index 00d813630b2..5b919bb1690 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/LineEntityProcessor.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/LineEntityProcessor.java
@@ -115,7 +115,11 @@ public class LineEntityProcessor extends EntityProcessorBase {
              "Problem reading from input", exp);
       }
   
      if (line == null) return null; // end of input       
      // end of input
      if (line == null) {
        closeResources();
        return null;
      }
 
       // First scan whole line to see if we want it
       if (acceptLineRegex != null && ! acceptLineRegex.matcher(line).find()) continue;
@@ -126,13 +130,17 @@ public class LineEntityProcessor extends EntityProcessorBase {
       return row;
     }
   }
  
  public void closeResources() {
    if (reader != null) {
      IOUtils.closeQuietly(reader);
    }
    reader= null;
  }
 
     @Override
     public void destroy() {
      if (reader != null) {
        IOUtils.closeQuietly(reader);
      }
      reader= null;
      closeResources();
       super.destroy();
     }
 
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListWithLineEntityProcessor.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListWithLineEntityProcessor.java
new file mode 100644
index 00000000000..6800ba83e4a
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListWithLineEntityProcessor.java
@@ -0,0 +1,66 @@
package org.apache.solr.handler.dataimport;

import java.io.File;

import org.apache.solr.request.LocalSolrQueryRequest;
import org.junit.BeforeClass;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class TestFileListWithLineEntityProcessor extends AbstractDataImportHandlerTestCase {
  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("dataimport-solrconfig.xml", "dataimport-schema.xml");
  }
  
  public void test() throws Exception {
    File tmpdir = File.createTempFile("test", "tmp", TEMP_DIR);
    tmpdir.delete();
    tmpdir.mkdir();
    tmpdir.deleteOnExit();
    createFile(tmpdir, "a.txt", "a line one\na line two\na line three".getBytes(), false);
    createFile(tmpdir, "b.txt", "b line one\nb line two".getBytes(), false);
    createFile(tmpdir, "c.txt", "c line one\nc line two\nc line three\nc line four".getBytes(), false);
    
    String config = generateConfig(tmpdir);
    LocalSolrQueryRequest request = lrf.makeRequest(
        "command", "full-import", "dataConfig", config,
        "clean", "true", "commit", "true", "synchronous", "true", "indent", "true");
    h.query("/dataimport", request);
    
    assertQ(req("*:*"), "//*[@numFound='9']");
    assertQ(req("id:?\\ line\\ one"), "//*[@numFound='3']");
    assertQ(req("id:a\\ line*"), "//*[@numFound='3']");
    assertQ(req("id:b\\ line*"), "//*[@numFound='2']");
    assertQ(req("id:c\\ line*"), "//*[@numFound='4']");    
  }
  
  private String generateConfig(File dir) {
    return
    "<dataConfig> \n"+
    "<dataSource type=\"FileDataSource\" encoding=\"UTF-8\" name=\"fds\"/> \n"+
    "    <document> \n"+
    "       <entity name=\"f\" processor=\"FileListEntityProcessor\" fileName=\".*[.]txt\" baseDir=\"" + dir.getAbsolutePath() + "\" recursive=\"false\" rootEntity=\"false\"  transformer=\"TemplateTransformer\"> \n" +
    "             <entity name=\"jc\" processor=\"LineEntityProcessor\" url=\"${f.fileAbsolutePath}\" dataSource=\"fds\"  rootEntity=\"true\" transformer=\"TemplateTransformer\"> \n" +
    "              <field column=\"rawLine\" name=\"id\" /> \n" +
    "             </entity> \n"+              
    "        </entity> \n"+
    "    </document> \n"+
    "</dataConfig> \n";
  }  
}
- 
2.19.1.windows.1

