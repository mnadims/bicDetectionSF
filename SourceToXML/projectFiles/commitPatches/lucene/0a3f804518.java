From 0a3f80451848fa683ac72ecde1c1818912cc76ae Mon Sep 17 00:00:00 2001
From: Mark Robert Miller <markrmiller@apache.org>
Date: Wed, 6 Jul 2011 00:37:21 +0000
Subject: [PATCH] SOLR-2331: Refactor CoreContainer's SolrXML serialization
 code and improve testing

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1143235 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/solr/core/CoreContainer.java   | 233 ++++++------------
 .../apache/solr/core/SolrXMLSerializer.java   | 218 ++++++++++++++++
 .../solrj/embedded/TestSolrProperties.java    |   3 +-
 .../solr/core/TestSolrXMLSerializer.java      | 154 ++++++++++++
 4 files changed, 445 insertions(+), 163 deletions(-)
 create mode 100644 solr/src/java/org/apache/solr/core/SolrXMLSerializer.java
 create mode 100644 solr/src/test/org/apache/solr/core/TestSolrXMLSerializer.java

diff --git a/solr/src/java/org/apache/solr/core/CoreContainer.java b/solr/src/java/org/apache/solr/core/CoreContainer.java
index 3ec0023d70f..05bf268988a 100644
-- a/solr/src/java/org/apache/solr/core/CoreContainer.java
++ b/solr/src/java/org/apache/solr/core/CoreContainer.java
@@ -18,7 +18,6 @@
 package org.apache.solr.core;
 
 import java.io.*;
import java.nio.channels.FileChannel;
 import java.util.*;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.TimeoutException;
@@ -43,6 +42,8 @@ import org.apache.solr.common.util.DOMUtil;
 import org.apache.solr.common.util.XML;
 import org.apache.solr.common.util.FileUtils;
 import org.apache.solr.common.util.SystemIdResolver;
import org.apache.solr.core.SolrXMLSerializer.SolrCoreXMLDef;
import org.apache.solr.core.SolrXMLSerializer.SolrXMLDef;
 import org.apache.solr.handler.admin.CoreAdminHandler;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.zookeeper.KeeperException;
@@ -82,6 +83,7 @@ public class CoreContainer
   protected Integer zkClientTimeout;
   protected String solrHome;
   protected String defaultCoreName = "";
  private SolrXMLSerializer solrXMLSerializer = new SolrXMLSerializer();
   private ZkController zkController;
   private SolrZkServer zkServer;
 
@@ -846,172 +848,79 @@ public class CoreContainer
 
   /** Persists the cores config file in a user provided file. */
   public void persistFile(File file) {
    log.info("Persisting cores config to " + (file==null ? configFile : file));

    File tmpFile = null;
    try {
      // write in temp first
      if (file == null) {
        file = tmpFile = File.createTempFile("solr", ".xml", configFile.getParentFile());
      }
      java.io.FileOutputStream out = new java.io.FileOutputStream(file);
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
        persist(writer);
        writer.flush();
        writer.close();
        out.close();
        // rename over origin or copy it this fails
        if (tmpFile != null) {
          if (tmpFile.renameTo(configFile))
            tmpFile = null;
          else
            fileCopy(tmpFile, configFile);
        }
    } 
    catch(java.io.FileNotFoundException xnf) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, xnf);
    } 
    catch(java.io.IOException xio) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, xio);
    } 
    finally {
      if (tmpFile != null) {
        if (!tmpFile.delete())
          tmpFile.deleteOnExit();
      }
    }
  }
  
  /** Write the cores configuration through a writer.*/
  void persist(Writer w) throws IOException {
    w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
    w.write("<solr");
    if (this.libDir != null) {
      writeAttribute(w,"sharedLib",libDir);
    }
    if(zkHost != null) writeAttribute(w, "zkHost", zkHost);
    writeAttribute(w,"persistent",isPersistent());
    w.write(">\n");

    if (containerProperties != null && !containerProperties.isEmpty())  {
      writeProperties(w, containerProperties, "  ");
    }
    w.write("  <cores");
    writeAttribute(w, "adminPath",adminPath);
    if(adminHandler != null) writeAttribute(w, "adminHandler", adminHandler);
    if(shareSchema) writeAttribute(w, "shareSchema", "true");
    if(!defaultCoreName.equals("")) writeAttribute(w, "defaultCoreName", defaultCoreName);
    if(host != null) writeAttribute(w, "host", host);
    if(hostPort != null) writeAttribute(w, "hostPort", hostPort);
    if(zkClientTimeout != null) writeAttribute(w, "zkClientTimeout", zkClientTimeout);
    if(hostContext != null) writeAttribute(w, "hostContext", hostContext);
    w.write(">\n");

    synchronized(cores) {
    log.info("Persisting cores config to " + (file == null ? configFile : file));
    // <solr attrib="value">
    Map<String,String> rootSolrAttribs = new HashMap<String,String>();
    if (libDir != null) rootSolrAttribs.put("sharedLib", libDir);
    rootSolrAttribs.put("persistent", Boolean.toString(isPersistent()));
    
    // <solr attrib="value"> <cores attrib="value">
    Map<String,String> coresAttribs = new HashMap<String,String>();
    coresAttribs.put("adminPath", adminPath);
    if (adminHandler != null) coresAttribs.put("adminHandler", adminHandler);
    if (shareSchema) coresAttribs.put("shareSchema", "true");
    if (!defaultCoreName.equals("")) coresAttribs.put("defaultCoreName",
        defaultCoreName);
    if (host != null) coresAttribs.put("host", host);
    if (hostPort != null) coresAttribs.put("hostPort", hostPort);
    if (zkClientTimeout != null) coresAttribs.put("zkClientTimeout", Integer.toString(zkClientTimeout));
    if (hostContext != null) coresAttribs.put("hostContext", hostContext);
    
    List<SolrCoreXMLDef> solrCoreXMLDefs = new ArrayList<SolrCoreXMLDef>();
    
    synchronized (cores) {
      Map<String,String> coreAttribs = new HashMap<String,String>();
       for (SolrCore solrCore : cores.values()) {
        persist(w,solrCore.getCoreDescriptor());
        CoreDescriptor dcore = solrCore.getCoreDescriptor();
        
        coreAttribs.put("name", dcore.name.equals("") ? defaultCoreName
            : dcore.name);
        coreAttribs.put("instanceDir", dcore.getInstanceDir());
        // write config (if not default)
        String opt = dcore.getConfigName();
        if (opt != null && !opt.equals(dcore.getDefaultConfigName())) {
          coreAttribs.put("config", opt);
        }
        // write schema (if not default)
        opt = dcore.getSchemaName();
        if (opt != null && !opt.equals(dcore.getDefaultSchemaName())) {
          coreAttribs.put("schema", opt);
        }
        opt = dcore.getPropertiesName();
        if (opt != null) {
          coreAttribs.put("properties", opt);
        }
        opt = dcore.dataDir;
        if (opt != null) coreAttribs.put("dataDir", opt);
        
        CloudDescriptor cd = dcore.getCloudDescriptor();
        if (cd != null) {
          opt = cd.getShardId();
          if (opt != null) coreAttribs.put("shard", opt);
          // only write out the collection name if it's not the default (the
          // core
          // name)
          opt = cd.getCollectionName();
          if (opt != null && !opt.equals(dcore.name)) coreAttribs.put(
              "collection", opt);
        }
        
        SolrCoreXMLDef solrCoreXMLDef = new SolrCoreXMLDef();
        solrCoreXMLDef.coreAttribs = coreAttribs;
        solrCoreXMLDef.coreProperties = dcore.getCoreProperties();
        solrCoreXMLDefs.add(solrCoreXMLDef);
       }
    }

    w.write("  </cores>\n");
    w.write("</solr>\n");
  }

  private void writeAttribute(Writer w, String name, Object value) throws IOException {
    if (value == null) return;
    w.write(" ");
    w.write(name);
    w.write("=\"");
    XML.escapeAttributeValue(value.toString(), w);
    w.write("\"");
  }
  
  /** Writes the cores configuration node for a given core. */
  void persist(Writer w, CoreDescriptor dcore) throws IOException {
    w.write("    <core");
    writeAttribute(w,"name",dcore.name.equals("") ? defaultCoreName : dcore.name);
    writeAttribute(w,"instanceDir",dcore.getInstanceDir());
    //write config (if not default)
    String opt = dcore.getConfigName();
    if (opt != null && !opt.equals(dcore.getDefaultConfigName())) {
      writeAttribute(w, "config",opt);
    }
    //write schema (if not default)
    opt = dcore.getSchemaName();
    if (opt != null && !opt.equals(dcore.getDefaultSchemaName())) {
      writeAttribute(w,"schema",opt);
    }
    opt = dcore.getPropertiesName();
    if (opt != null) {
      writeAttribute(w,"properties",opt);
    }
    opt = dcore.dataDir;
    if (opt != null) writeAttribute(w,"dataDir",opt);

    CloudDescriptor cd = dcore.getCloudDescriptor();
    if (cd != null) {
      opt = cd.getShardId();
      if (opt != null)
        writeAttribute(w,"shard",opt);
      // only write out the collection name if it's not the default (the core name)
      opt = cd.getCollectionName();
      if (opt != null && !opt.equals(dcore.name))
        writeAttribute(w,"collection",opt);
    }

    if (dcore.getCoreProperties() == null || dcore.getCoreProperties().isEmpty())
      w.write("/>\n"); // core
    else  {
      w.write(">\n");
      writeProperties(w, dcore.getCoreProperties(), "      ");
      w.write("    </core>\n");
    }
  }

  private void writeProperties(Writer w, Properties props, String indent) throws IOException {
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      w.write(indent + "<property");
      writeAttribute(w,"name",entry.getKey());
      writeAttribute(w,"value",entry.getValue());
      w.write("/>\n");
      
      SolrXMLDef solrXMLDef = new SolrXMLDef();
      solrXMLDef.coresDefs = solrCoreXMLDefs;
      solrXMLDef.containerProperties = containerProperties;
      solrXMLDef.solrAttribs = rootSolrAttribs;
      solrXMLDef.coresAttribs = coresAttribs;
      solrXMLSerializer.persistFile(file == null ? configFile : file,
          solrXMLDef);
     }
   }
 
  /** Copies a src file to a dest file:
   *  used to circumvent the platform discrepancies regarding renaming files.
   */
  public static void fileCopy(File src, File dest) throws IOException {
    IOException xforward = null;
    FileInputStream fis =  null;
    FileOutputStream fos = null;
    FileChannel fcin = null;
    FileChannel fcout = null;
    try {
      fis = new FileInputStream(src);
      fos = new FileOutputStream(dest);
      fcin = fis.getChannel();
      fcout = fos.getChannel();
      // do the file copy 32Mb at a time
      final int MB32 = 32*1024*1024;
      long size = fcin.size();
      long position = 0;
      while (position < size) {
        position += fcin.transferTo(position, MB32, fcout);
      }
    } 
    catch(IOException xio) {
      xforward = xio;
    } 
    finally {
      if (fis   != null) try { fis.close(); fis = null; } catch(IOException xio) {}
      if (fos   != null) try { fos.close(); fos = null; } catch(IOException xio) {}
      if (fcin  != null && fcin.isOpen() ) try { fcin.close();  fcin = null;  } catch(IOException xio) {}
      if (fcout != null && fcout.isOpen()) try { fcout.close(); fcout = null; } catch(IOException xio) {}
    }
    if (xforward != null) {
      throw xforward;
    }
  }
 
   public String getSolrHome() {
     return solrHome;
diff --git a/solr/src/java/org/apache/solr/core/SolrXMLSerializer.java b/solr/src/java/org/apache/solr/core/SolrXMLSerializer.java
new file mode 100644
index 00000000000..e882ddfa5d0
-- /dev/null
++ b/solr/src/java/org/apache/solr/core/SolrXMLSerializer.java
@@ -0,0 +1,218 @@
package org.apache.solr.core;

/**
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrXMLSerializer {
  protected static Logger log = LoggerFactory
      .getLogger(SolrXMLSerializer.class);
  
  private final static String INDENT = "  ";
  
  
  /**
   * @param w
   *          Writer to use
   * @param defaultCoreName
   *          to use for cores with name ""
   * @param coreDescriptors
   *          to persist
   * @param rootSolrAttribs
   *          solrxml solr attribs
   * @param containerProperties
   *          to persist
   * @param coresAttribs
   *          solrxml cores attribs
   * @throws IOException
   */
  void persist(Writer w, SolrXMLDef solrXMLDef) throws IOException {
    w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
    w.write("<solr");
    Map<String,String> rootSolrAttribs = solrXMLDef.solrAttribs;
    Set<String> solrAttribKeys = rootSolrAttribs.keySet();
    for (String key : solrAttribKeys) {
      String value = rootSolrAttribs.get(key);
      writeAttribute(w, key, value);
    }
    
    w.write(">\n");
    Properties containerProperties = solrXMLDef.containerProperties;
    if (containerProperties != null && !containerProperties.isEmpty()) {
      writeProperties(w, containerProperties, "  ");
    }
    w.write(INDENT + "<cores");
    Map<String,String> coresAttribs = solrXMLDef.coresAttribs;
    Set<String> coreAttribKeys = coresAttribs.keySet();
    for (String key : coreAttribKeys) {
      String value = coresAttribs.get(key);
      writeAttribute(w, key, value);
    }
    w.write(">\n");
    
    for (SolrCoreXMLDef coreDef : solrXMLDef.coresDefs) {
      persist(w, coreDef);
    }

    w.write(INDENT + "</cores>\n");
    w.write("</solr>\n");
  }
  
  /** Writes the cores configuration node for a given core. */
  private void persist(Writer w, SolrCoreXMLDef coreDef) throws IOException {
    w.write(INDENT + INDENT + "<core");
    Set<String> keys = coreDef.coreAttribs.keySet();
    for (String key : keys) {
      writeAttribute(w, key, coreDef.coreAttribs.get(key));
    }
    Properties properties = coreDef.coreProperties;
    if (properties == null || properties.isEmpty()) w.write("/>\n"); // core
    else {
      w.write(">\n");
      writeProperties(w, properties, "      ");
      w.write(INDENT + INDENT + "</core>\n");
    }
  }
  
  private void writeProperties(Writer w, Properties props, String indent)
      throws IOException {
    for (Map.Entry<Object,Object> entry : props.entrySet()) {
      w.write(indent + "<property");
      writeAttribute(w, "name", entry.getKey());
      writeAttribute(w, "value", entry.getValue());
      w.write("/>\n");
    }
  }
  
  private void writeAttribute(Writer w, String name, Object value)
      throws IOException {
    if (value == null) return;
    w.write(" ");
    w.write(name);
    w.write("=\"");
    XML.escapeAttributeValue(value.toString(), w);
    w.write("\"");
  }
  
  void persistFile(File file, SolrXMLDef solrXMLDef) {
    log.info("Persisting cores config to " + file);
    
    File tmpFile = null;
    try {
      // write in temp first
      tmpFile = File.createTempFile("solr", ".xml", file.getParentFile());
      
      java.io.FileOutputStream out = new java.io.FileOutputStream(tmpFile);
      Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
      try {
        persist(writer, solrXMLDef);
      } finally {
        writer.close();
        out.close();
      }
      // rename over origin or copy if this fails
      if (tmpFile != null) {
        if (tmpFile.renameTo(file)) tmpFile = null;
        else fileCopy(tmpFile, file);
      }
    } catch (java.io.FileNotFoundException xnf) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, xnf);
    } catch (java.io.IOException xio) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, xio);
    } finally {
      if (tmpFile != null) {
        if (!tmpFile.delete()) tmpFile.deleteOnExit();
      }
    }
  }
  
  /**
   * Copies a src file to a dest file: used to circumvent the platform
   * discrepancies regarding renaming files.
   */
  private static void fileCopy(File src, File dest) throws IOException {
    IOException xforward = null;
    FileInputStream fis = null;
    FileOutputStream fos = null;
    FileChannel fcin = null;
    FileChannel fcout = null;
    try {
      fis = new FileInputStream(src);
      fos = new FileOutputStream(dest);
      fcin = fis.getChannel();
      fcout = fos.getChannel();
      // do the file copy 32Mb at a time
      final int MB32 = 32 * 1024 * 1024;
      long size = fcin.size();
      long position = 0;
      while (position < size) {
        position += fcin.transferTo(position, MB32, fcout);
      }
    } catch (IOException xio) {
      xforward = xio;
    } finally {
      if (fis != null) try {
        fis.close();
        fis = null;
      } catch (IOException xio) {}
      if (fos != null) try {
        fos.close();
        fos = null;
      } catch (IOException xio) {}
      if (fcin != null && fcin.isOpen()) try {
        fcin.close();
        fcin = null;
      } catch (IOException xio) {}
      if (fcout != null && fcout.isOpen()) try {
        fcout.close();
        fcout = null;
      } catch (IOException xio) {}
    }
    if (xforward != null) {
      throw xforward;
    }
  }
  
  static public class SolrXMLDef {
    Properties containerProperties;
    Map<String,String> solrAttribs;
    Map<String,String> coresAttribs;
    List<SolrCoreXMLDef> coresDefs;
  }
  
  static public class SolrCoreXMLDef {
    Properties coreProperties;
    Map<String,String> coreAttribs;
  }
}
\ No newline at end of file
diff --git a/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java b/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java
index 4df93f2d120..975aa200f84 100644
-- a/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java
++ b/solr/src/test/org/apache/solr/client/solrj/embedded/TestSolrProperties.java
@@ -27,6 +27,7 @@ import javax.xml.xpath.XPathConstants;
 import javax.xml.xpath.XPathExpressionException;
 import javax.xml.xpath.XPathFactory;
 
import org.apache.commons.io.IOUtils;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.client.solrj.SolrQuery;
@@ -202,7 +203,7 @@ public class TestSolrProperties extends LuceneTestCase {
 
     mcr = CoreAdminRequest.persist("solr-persist.xml", coreadmin);
     
    //System.out.println(IOUtils.toString(new FileInputStream(new File(solrXml.getParent(), "solr-persist.xml"))));
    System.out.println(IOUtils.toString(new FileInputStream(new File(solrXml.getParent(), "solr-persist.xml"))));
     DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
     FileInputStream fis = new FileInputStream(new File(solrXml.getParent(), "solr-persist.xml"));
     try {
diff --git a/solr/src/test/org/apache/solr/core/TestSolrXMLSerializer.java b/solr/src/test/org/apache/solr/core/TestSolrXMLSerializer.java
new file mode 100644
index 00000000000..d8b1877b269
-- /dev/null
++ b/solr/src/test/org/apache/solr/core/TestSolrXMLSerializer.java
@@ -0,0 +1,154 @@
package org.apache.solr.core;

/**
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.core.SolrXMLSerializer.SolrCoreXMLDef;
import org.apache.solr.core.SolrXMLSerializer.SolrXMLDef;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TestSolrXMLSerializer extends LuceneTestCase {
  private static final XPathFactory xpathFactory = XPathFactory.newInstance();
  private static final String defaultCoreNameKey = "defaultCoreName";
  private static final String defaultCoreNameVal = "collection1";
  private static final String peristentKey = "persistent";
  private static final String persistentVal = "true";
  private static final String sharedLibKey = "sharedLib";
  private static final String sharedLibVal = "true";
  private static final String adminPathKey = "adminPath";
  private static final String adminPathVal = "/admin";
  private static final String shareSchemaKey = "admin";
  private static final String shareSchemaVal = "true";
  private static final String instanceDirKey = "instanceDir";
  private static final String instanceDirVal = "core1";
  
  @Test
  public void basicUsageTest() throws Exception {
    SolrXMLSerializer serializer = new SolrXMLSerializer();
    
    SolrXMLDef solrXMLDef = getTestSolrXMLDef(defaultCoreNameKey,
        defaultCoreNameVal, peristentKey, persistentVal, sharedLibKey,
        sharedLibVal, adminPathKey, adminPathVal, shareSchemaKey,
        shareSchemaVal, instanceDirKey, instanceDirVal);
    
    Writer w = new StringWriter();
    try {
      serializer.persist(w, solrXMLDef);
    } finally {
      w.close();
    }
    
    assertResults(((StringWriter) w).getBuffer().toString().getBytes("UTF-8"));
    
    // again with default file
    File tmpFile = File.createTempFile("solr", ".xml", TEMP_DIR);
    
    serializer.persistFile(tmpFile, solrXMLDef);

    assertResults(FileUtils.readFileToString(tmpFile, "UTF-8").getBytes("UTF-8"));
  }

  private void assertResults(byte[] bytes)
      throws ParserConfigurationException, UnsupportedEncodingException,
      IOException, SAXException, XPathExpressionException {
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    BufferedInputStream is = new BufferedInputStream(new ByteArrayInputStream(bytes));
    Document document;
    try {
//      is.mark(0);
//      System.out.println("SolrXML:" + IOUtils.toString(is, "UTF-8"));
//      is.reset();
      document = builder.parse(is);
    } finally {
      is.close();
    }
    
    assertTrue(exists("/solr[@" + peristentKey + "='" + persistentVal + "']", document));
    assertTrue(exists("/solr[@" + sharedLibKey + "='" + sharedLibVal + "']", document));
    assertTrue(exists("/solr/cores[@" + defaultCoreNameKey + "='" + defaultCoreNameVal + "']", document));
    assertTrue(exists("/solr/cores[@" + adminPathKey + "='" + adminPathVal + "']", document));
    assertTrue(exists("/solr/cores/core[@" + instanceDirKey + "='" + instanceDirVal + "']", document));
  }

  private SolrXMLDef getTestSolrXMLDef(String defaultCoreNameKey,
      String defaultCoreNameVal, String peristentKey, String persistentVal,
      String sharedLibKey, String sharedLibVal, String adminPathKey,
      String adminPathVal, String shareSchemaKey, String shareSchemaVal,
      String instanceDirKey, String instanceDirVal) {
    // <solr attrib="value">
    Map<String,String> rootSolrAttribs = new HashMap<String,String>();
    rootSolrAttribs.put(sharedLibKey, sharedLibVal);
    rootSolrAttribs.put(peristentKey, persistentVal);
    
    // <solr attrib="value"> <cores attrib="value">
    Map<String,String> coresAttribs = new HashMap<String,String>();
    coresAttribs.put(adminPathKey, adminPathVal);
    coresAttribs.put(shareSchemaKey, shareSchemaVal);
    coresAttribs.put(defaultCoreNameKey, defaultCoreNameVal);
    
    SolrXMLDef solrXMLDef = new SolrXMLDef();
    
    // <solr attrib="value"> <cores attrib="value"> <core attrib="value">
    List<SolrCoreXMLDef> solrCoreXMLDefs = new ArrayList<SolrCoreXMLDef>();
    SolrCoreXMLDef coreDef = new SolrCoreXMLDef();
    Map<String,String> coreAttribs = new HashMap<String,String>();
    coreAttribs.put(instanceDirKey, instanceDirVal);
    coreDef.coreAttribs = coreAttribs ;
    coreDef.coreProperties = new Properties();
    solrCoreXMLDefs.add(coreDef);
    
    solrXMLDef.coresDefs = solrCoreXMLDefs ;
    Properties containerProperties = new Properties();
    solrXMLDef.containerProperties = containerProperties ;
    solrXMLDef.solrAttribs = rootSolrAttribs;
    solrXMLDef.coresAttribs = coresAttribs;
    return solrXMLDef;
  }
  
  public static boolean exists(String xpathStr, Node node)
      throws XPathExpressionException {
    XPath xpath = xpathFactory.newXPath();
    return (Boolean) xpath.evaluate(xpathStr, node, XPathConstants.BOOLEAN);
  }
}
- 
2.19.1.windows.1

