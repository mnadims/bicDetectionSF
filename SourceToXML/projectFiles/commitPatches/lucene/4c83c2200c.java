From 4c83c2200c9b2097cad09e5dd0f97033254620df Mon Sep 17 00:00:00 2001
From: Steven Rowe <sarowe@apache.org>
Date: Thu, 9 Apr 2015 04:42:30 +0000
Subject: [PATCH] SOLR-7366: fix regression in ManagedIndexSchema's handling of
 ResourceLoaderAware objects used by field types, causing example XML docs to
 not be indexable via bin/post; add a test indexing example docs that fails
 without the patch and succeeds with it

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1672238 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |   3 +
 .../org/apache/solr/schema/IndexSchema.java   |  13 +-
 .../solr/schema/ManagedIndexSchema.java       |  77 +++--------
 .../solr/cloud/SolrCloudExampleTest.java      | 128 ++++++++++++++++++
 solr/example/exampledocs/ipod_other.xml       |   2 +-
 5 files changed, 161 insertions(+), 62 deletions(-)
 create mode 100644 solr/core/src/test/org/apache/solr/cloud/SolrCloudExampleTest.java

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 469c0a25cbc..214b6e33ff9 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -407,6 +407,9 @@ Bug Fixes
 * SOLR-7338, SOLR-6583: A reloaded core will never register itself as active after a ZK session expiration
   (Mark Miller, Timothy Potter)
 
* SOLR-7366: Can't index example XML docs into the cloud example using bin/post due to regression in 
  ManagedIndexSchema's handling of ResourceLoaderAware objects used by field types (Steve Rowe, Timothy Potter)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/schema/IndexSchema.java b/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
index 40560534891..5454e8b2c8e 100644
-- a/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
++ b/solr/core/src/java/org/apache/solr/schema/IndexSchema.java
@@ -589,10 +589,8 @@ public class IndexSchema {
       dynamicCopyFields = new DynamicCopy[] {};
       loadCopyFields(document, xpath);
 
      //Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : schemaAware) {
        aware.inform(this);
      }
      postReadInform();

     } catch (SolrException e) {
       throw new SolrException(ErrorCode.getErrorCode(e.code()), e.getMessage() + ". Schema file is " +
           resourcePath, e);
@@ -606,6 +604,13 @@ public class IndexSchema {
     // create the field analyzers
     refreshAnalyzers();
   }
  
  protected void postReadInform() {
    //Run the callbacks on SchemaAware now that everything else is done
    for (SchemaAware aware : schemaAware) {
      aware.inform(this);
    }
  }
 
   /** 
    * Loads fields and dynamic fields.
diff --git a/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java b/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java
index b23a37a6fab..666d6bc65b8 100644
-- a/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java
++ b/solr/core/src/java/org/apache/solr/schema/ManagedIndexSchema.java
@@ -41,7 +41,6 @@ import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.ContentStream;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.Config;
 import org.apache.solr.core.SolrConfig;
 import org.apache.solr.core.SolrResourceLoader;
 import org.apache.solr.rest.schema.FieldTypeXmlAdapter;
@@ -50,13 +49,8 @@ import org.apache.solr.util.FileUtils;
 import org.apache.zookeeper.CreateMode;
 import org.apache.zookeeper.KeeperException;
 import org.apache.zookeeper.data.Stat;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
 import org.xml.sax.InputSource;
 
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
@@ -418,10 +412,8 @@ public final class ManagedIndexSchema extends IndexSchema {
         }
       }
 
      // Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.postReadInform();

       newSchema.refreshAnalyzers();
 
       if(persist) {
@@ -468,10 +460,7 @@ public final class ManagedIndexSchema extends IndexSchema {
           throw new SolrException(ErrorCode.BAD_REQUEST, msg);
         }
       }
      // Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.postReadInform();
       newSchema.refreshAnalyzers();
     } else {
       String msg = "This ManagedIndexSchema is not mutable.";
@@ -555,9 +544,7 @@ public final class ManagedIndexSchema extends IndexSchema {
         }
       }
 
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.postReadInform();
       newSchema.refreshAnalyzers();
     } else {
       String msg = "This ManagedIndexSchema is not mutable.";
@@ -595,10 +582,7 @@ public final class ManagedIndexSchema extends IndexSchema {
         }
       }
 
      // Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.postReadInform();
       newSchema.refreshAnalyzers();
       if (persist) {
         success = newSchema.persistManagedSchema(false); // don't just create - update it if it already exists
@@ -677,10 +661,7 @@ public final class ManagedIndexSchema extends IndexSchema {
         }
       }
 
      // Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.postReadInform();
       newSchema.refreshAnalyzers();
     } else {
       String msg = "This ManagedIndexSchema is not mutable.";
@@ -748,9 +729,7 @@ public final class ManagedIndexSchema extends IndexSchema {
         }
       }
 
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.postReadInform();
       newSchema.refreshAnalyzers();
     } else {
       String msg = "This ManagedIndexSchema is not mutable.";
@@ -773,11 +752,7 @@ public final class ManagedIndexSchema extends IndexSchema {
           newSchema.registerCopyField(entry.getKey(), destination);
         }
       }
      //TODO: move this common stuff out to shared methods
      // Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.postReadInform();
       newSchema.refreshAnalyzers();
       if(persist) {
         success = newSchema.persistManagedSchema(false); // don't just create - update it if it already exists
@@ -813,11 +788,7 @@ public final class ManagedIndexSchema extends IndexSchema {
           newSchema.deleteCopyField(entry.getKey(), destination);
         }
       }
      //TODO: move this common stuff out to shared methods
      // Run the callbacks on SchemaAware now that everything else is done
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      newSchema.postReadInform();
       newSchema.refreshAnalyzers();
     } else {
       String msg = "This ManagedIndexSchema is not mutable.";
@@ -960,14 +931,8 @@ public final class ManagedIndexSchema extends IndexSchema {
       newSchema.fieldTypes.put(typeName, fieldType);
     }
 
    // Run the callbacks on SchemaAware now that everything else is done
    for (SchemaAware aware : newSchema.schemaAware)
      aware.inform(newSchema);
    newSchema.postReadInform();
     
    // looks good for the add, notify ResoureLoaderAware objects
    for (FieldType fieldType : fieldTypeList)
      informResourceLoaderAwareObjectsForFieldType(fieldType);

     newSchema.refreshAnalyzers();
 
     if (persist) {
@@ -1018,12 +983,7 @@ public final class ManagedIndexSchema extends IndexSchema {
       for (String name : names) {
         newSchema.fieldTypes.remove(name);
       }
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      for (FieldType fieldType : newSchema.fieldTypes.values()) {
        informResourceLoaderAwareObjectsForFieldType(fieldType);
      }
      newSchema.postReadInform();
       newSchema.refreshAnalyzers();
     } else {
       String msg = "This ManagedIndexSchema is not mutable.";
@@ -1153,12 +1113,7 @@ public final class ManagedIndexSchema extends IndexSchema {
       }
       newSchema.rebuildCopyFields(copyFieldsToRebuild);
 
      for (SchemaAware aware : newSchema.schemaAware) {
        aware.inform(newSchema);
      }
      for (FieldType fieldType : newSchema.fieldTypes.values()) {
        newSchema.informResourceLoaderAwareObjectsForFieldType(fieldType);
      }
      newSchema.postReadInform();
       newSchema.refreshAnalyzers();
     } else {
       String msg = "This ManagedIndexSchema is not mutable.";
@@ -1167,6 +1122,14 @@ public final class ManagedIndexSchema extends IndexSchema {
     }
     return newSchema;
   }
  
  @Override
  protected void postReadInform() {
    super.postReadInform();
    for (FieldType fieldType : fieldTypes.values()) {
      informResourceLoaderAwareObjectsForFieldType(fieldType);
    }
  }
 
   /**
    * Informs analyzers used by a fieldType.
diff --git a/solr/core/src/test/org/apache/solr/cloud/SolrCloudExampleTest.java b/solr/core/src/test/org/apache/solr/cloud/SolrCloudExampleTest.java
new file mode 100644
index 00000000000..2a92b6045a1
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/cloud/SolrCloudExampleTest.java
@@ -0,0 +1,128 @@
package org.apache.solr.cloud;

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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.util.ExternalPaths;
import org.apache.solr.util.SolrCLI;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emulates bin/solr -e cloud -noprompt; bin/post -c gettingstarted example/exampledocs/*.xml;
 * this test is useful for catching regressions in indexing the example docs in collections that
 * use data-driven schema and managed schema features provided by configsets/data_driven_schema_configs.
 */
public class SolrCloudExampleTest extends AbstractFullDistribZkTestBase {

  protected static final transient Logger log = LoggerFactory.getLogger(SolrCloudExampleTest.class);

  public SolrCloudExampleTest() {
    super();
    sliceCount = 2;
  }

  @Override
  public void distribSetUp() throws Exception {
    super.distribSetUp();
    System.setProperty("numShards", Integer.toString(sliceCount));
  }

  @Test
  public void testLoadDocsIntoGettingStartedCollection() throws Exception {
    waitForThingsToLevelOut(30000);

    log.info("testLoadDocsIntoGettingStartedCollection initialized OK ... running test logic");

    String testCollectionName = "gettingstarted";
    File data_driven_schema_configs = new File(ExternalPaths.SCHEMALESS_CONFIGSET);
    assertTrue(data_driven_schema_configs.getAbsolutePath()+" not found!", data_driven_schema_configs.isDirectory());

    Set<String> liveNodes = cloudClient.getZkStateReader().getClusterState().getLiveNodes();
    if (liveNodes.isEmpty())
      fail("No live nodes found! Cannot create a collection until there is at least 1 live node in the cluster.");
    String firstLiveNode = liveNodes.iterator().next();
    String solrUrl = cloudClient.getZkStateReader().getBaseUrlForNodeName(firstLiveNode);

    // create the gettingstarted collection just like the bin/solr script would do
    String[] args = new String[] {
        "create_collection",
        "-name", testCollectionName,
        "-shards", "2",
        "-replicationFactor", "2",
        "-confname", testCollectionName,
        "-confdir", "data_driven_schema_configs",
        "-configsetsDir", data_driven_schema_configs.getParentFile().getParentFile().getAbsolutePath(),
        "-solrUrl", solrUrl
    };
    SolrCLI.CreateCollectionTool tool = new SolrCLI.CreateCollectionTool();
    CommandLine cli = SolrCLI.processCommandLineArgs(SolrCLI.joinCommonAndToolOptions(tool.getOptions()), args);
    log.info("Creating the '"+testCollectionName+"' collection using SolrCLI with: "+solrUrl);
    tool.runTool(cli);
    assertTrue("Collection '" + testCollectionName + "' doesn't exist after trying to create it!",
        cloudClient.getZkStateReader().getClusterState().hasCollection(testCollectionName));

    // verify the collection is usable ...
    ensureAllReplicasAreActive(testCollectionName, "shard1", 2, 2, 20);
    ensureAllReplicasAreActive(testCollectionName, "shard2", 2, 2, 10);
    cloudClient.setDefaultCollection(testCollectionName);

    // now index docs like bin/post would do but we can't use SimplePostTool because it uses System.exit when
    // it encounters an error, which JUnit doesn't like ...
    log.info("Created collection, now posting example docs!");
    File exampleDocsDir = new File(ExternalPaths.SOURCE_HOME, "example/exampledocs");
    assertTrue(exampleDocsDir.getAbsolutePath()+" not found!", exampleDocsDir.isDirectory());

    File[] xmlFiles = exampleDocsDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".xml");
      }
    });

    // if you add/remove example XML docs, you'll have to fix these expected values
    int expectedXmlFileCount = 14;
    int expectedXmlDocCount = 32;

    assertTrue("Expected 14 example XML files in "+exampleDocsDir.getAbsolutePath(),
        xmlFiles.length == expectedXmlFileCount);

    for (File xml : xmlFiles) {
      ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update");
      req.addFile(xml, "application/xml");
      log.info("POSTing "+xml.getAbsolutePath());
      cloudClient.request(req);
    }
    cloudClient.commit();
    Thread.sleep(1000);

    QueryResponse qr = cloudClient.query(new SolrQuery("*:*"));
    int numFound = (int)qr.getResults().getNumFound();
    assertTrue("Expected "+expectedXmlDocCount+" docs but *:* found "+numFound, numFound == expectedXmlDocCount);

    log.info("testLoadDocsIntoGettingStartedCollection succeeded ... shutting down now!");
  }
}
diff --git a/solr/example/exampledocs/ipod_other.xml b/solr/example/exampledocs/ipod_other.xml
index 0e3968fb1c4..3de32f3b71c 100644
-- a/solr/example/exampledocs/ipod_other.xml
++ b/solr/example/exampledocs/ipod_other.xml
@@ -44,7 +44,7 @@
   <field name="cat">electronics</field>
   <field name="cat">connector</field>
   <field name="features">car power adapter for iPod, white</field>
  <field name="weight">2</field>
  <field name="weight">2.0</field>
   <field name="price">11.50</field>
   <field name="popularity">1</field>
   <field name="inStock">false</field>
- 
2.19.1.windows.1

