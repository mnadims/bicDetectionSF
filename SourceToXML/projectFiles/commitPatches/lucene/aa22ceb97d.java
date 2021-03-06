From aa22ceb97db65d51b2e38d2b4c7cdefcb82c0c3d Mon Sep 17 00:00:00 2001
From: Erick Erickson <erick@apache.org>
Date: Sat, 2 Mar 2013 01:01:12 +0000
Subject: [PATCH] SOLR-4196, steps toward making solr.xml obsolete

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1451797 13f79535-47bb-0310-9956-ffa450edef68
--
 .../src/java/org/apache/solr/cloud/ZkCLI.java |   33 +-
 .../org/apache/solr/cloud/ZkController.java   |   23 +-
 .../src/java/org/apache/solr/core/Config.java |   17 +-
 .../java/org/apache/solr/core/ConfigSolr.java |   83 +
 .../solr/core/ConfigSolrXmlBackCompat.java    |  358 +++++
 .../org/apache/solr/core/CoreContainer.java   | 1373 ++++++++++-------
 .../org/apache/solr/core/CoreDescriptor.java  |  225 ++-
 .../java/org/apache/solr/core/SolrCore.java   |   11 +-
 .../org/apache/solr/core/SolrProperties.java  |  575 +++++++
 .../apache/solr/core/SolrResourceLoader.java  |    3 +-
 .../solr/handler/admin/CoreAdminHandler.java  |   80 +-
 .../org/apache/solr/schema/SchemaField.java   |    2 +-
 .../java/org/apache/solr/util/DOMUtil.java    |  103 +-
 .../org/apache/solr/util/PropertiesUtil.java  |  132 ++
 .../solr/collection1/conf/schema-tiny.xml     |   38 +
 .../collection1/conf/solrconfig-minimal.xml   |   76 +
 .../solr/cloud/ChaosMonkeySafeLeaderTest.java |    2 +-
 .../apache/solr/cloud/ZkControllerTest.java   |    2 +-
 .../org/apache/solr/core/TestLazyCores.java   |  116 +-
 .../core/TestSolrDiscoveryProperties.java     |  389 +++++
 .../org/apache/solr/util/TestHarness.java     |    2 +-
 21 files changed, 2797 insertions(+), 846 deletions(-)
 create mode 100644 solr/core/src/java/org/apache/solr/core/ConfigSolr.java
 create mode 100644 solr/core/src/java/org/apache/solr/core/ConfigSolrXmlBackCompat.java
 create mode 100644 solr/core/src/java/org/apache/solr/core/SolrProperties.java
 create mode 100644 solr/core/src/java/org/apache/solr/util/PropertiesUtil.java
 create mode 100644 solr/core/src/test-files/solr/collection1/conf/schema-tiny.xml
 create mode 100644 solr/core/src/test-files/solr/collection1/conf/solrconfig-minimal.xml
 create mode 100644 solr/core/src/test/org/apache/solr/core/TestSolrDiscoveryProperties.java

diff --git a/solr/core/src/java/org/apache/solr/cloud/ZkCLI.java b/solr/core/src/java/org/apache/solr/cloud/ZkCLI.java
index eb101217b2d..8e42d09ef00 100644
-- a/solr/core/src/java/org/apache/solr/cloud/ZkCLI.java
++ b/solr/core/src/java/org/apache/solr/cloud/ZkCLI.java
@@ -1,7 +1,9 @@
 package org.apache.solr.cloud;
 
 import java.io.File;
import java.io.FileInputStream;
 import java.io.IOException;
import java.io.InputStream;
 import java.util.List;
 import java.util.concurrent.TimeoutException;
 
@@ -17,10 +19,11 @@ import org.apache.commons.cli.ParseException;
 import org.apache.commons.cli.PosixParser;
 import org.apache.solr.common.cloud.OnReconnect;
 import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.core.Config;
import org.apache.solr.core.ConfigSolr;
import org.apache.solr.core.ConfigSolrXmlBackCompat;
import org.apache.solr.core.SolrProperties;
 import org.apache.solr.core.SolrResourceLoader;
 import org.apache.zookeeper.KeeperException;
import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
 
 /*
@@ -171,12 +174,26 @@ public class ZkCLI {
           }
           SolrResourceLoader loader = new SolrResourceLoader(solrHome);
           solrHome = loader.getInstanceDir();
          
          InputSource cfgis = new InputSource(new File(solrHome, SOLR_XML)
              .toURI().toASCIIString());
          Config cfg = new Config(loader, null, cfgis, null, false);
          
          if(!ZkController.checkChrootPath(zkServerAddress, true)) {

          File configFile = new File(solrHome, SOLR_XML);
          boolean isXml = true;
          if (! configFile.exists()) {
            configFile = new File(solrHome, SolrProperties.SOLR_PROPERTIES_FILE);
            isXml = false;
          }
          InputStream is = new FileInputStream(configFile);

          //ConfigSolrXmlThunk cfg = new ConfigSolrXmlThunk(null, loader, is, false, true);

          ConfigSolr cfg;
            if (isXml) {
              cfg = new ConfigSolrXmlBackCompat(loader, null, is, null, false);
            } else {
              cfg = new SolrProperties(null, is, null);
            }


            if(!ZkController.checkChrootPath(zkServerAddress, true)) {
             System.out.println("A chroot was specified in zkHost but the znode doesn't exist. ");
             System.exit(1);
           }
diff --git a/solr/core/src/java/org/apache/solr/cloud/ZkController.java b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
index d1fe92734c6..f910bf95e57 100644
-- a/solr/core/src/java/org/apache/solr/cloud/ZkController.java
++ b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
@@ -39,8 +39,6 @@ import java.util.concurrent.TimeoutException;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
import javax.xml.xpath.XPathConstants;

 import org.apache.commons.io.FileUtils;
 import org.apache.solr.client.solrj.impl.HttpSolrServer;
 import org.apache.solr.client.solrj.request.CoreAdminRequest.WaitForState;
@@ -60,14 +58,14 @@ import org.apache.solr.common.cloud.ZkNodeProps;
 import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.cloud.ZooKeeperException;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.Config;
import org.apache.solr.core.ConfigSolr;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.CoreDescriptor;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.handler.component.ShardHandler;
 import org.apache.solr.update.UpdateLog;
 import org.apache.solr.update.UpdateShardHandler;
import org.apache.solr.util.DOMUtil;
import org.apache.solr.util.PropertiesUtil;
 import org.apache.zookeeper.CreateMode;
 import org.apache.zookeeper.KeeperException;
 import org.apache.zookeeper.KeeperException.NoNodeException;
@@ -75,8 +73,6 @@ import org.apache.zookeeper.KeeperException.SessionExpiredException;
 import org.apache.zookeeper.data.Stat;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
 
 /**
  * Handle ZooKeeper interactions.
@@ -1409,20 +1405,19 @@ public final class ZkController {
   /**
    * If in SolrCloud mode, upload config sets for each SolrCore in solr.xml.
    */
  public static void bootstrapConf(SolrZkClient zkClient, Config cfg, String solrHome) throws IOException,
  public static void bootstrapConf(SolrZkClient zkClient, ConfigSolr cfg, String solrHome) throws IOException,
       KeeperException, InterruptedException {
    log.info("bootstraping config into ZooKeeper using solr.xml");
    NodeList nodes = (NodeList)cfg.evaluate("solr/cores/core", XPathConstants.NODESET);
 
    for (int i=0; i<nodes.getLength(); i++) {
      Node node = nodes.item(i);
      String rawName = DOMUtil.substituteProperty(DOMUtil.getAttr(node, "name", null), new Properties());
      String instanceDir = DOMUtil.getAttr(node, "instanceDir", null);
    log.info("bootstraping config into ZooKeeper using solr.xml");
    List<String> allCoreNames = cfg.getAllCoreNames();
    for (String coreName : allCoreNames) {
      String rawName = PropertiesUtil.substituteProperty(cfg.getProperty(coreName, "name", null), new Properties());
      String instanceDir = cfg.getProperty(coreName, "instanceDir", null);
       File idir = new File(instanceDir);
       if (!idir.isAbsolute()) {
         idir = new File(solrHome, instanceDir);
       }
      String confName = DOMUtil.substituteProperty(DOMUtil.getAttr(node, "collection", null), new Properties());
      String confName = PropertiesUtil.substituteProperty(cfg.getProperty(coreName, "collection", null), new Properties());
       if (confName == null) {
         confName = rawName;
       }
diff --git a/solr/core/src/java/org/apache/solr/core/Config.java b/solr/core/src/java/org/apache/solr/core/Config.java
index 1fbc48cac1d..099c69d5d52 100644
-- a/solr/core/src/java/org/apache/solr/core/Config.java
++ b/solr/core/src/java/org/apache/solr/core/Config.java
@@ -34,15 +34,13 @@ import javax.xml.namespace.QName;
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
 import javax.xml.xpath.XPath;
 import javax.xml.xpath.XPathConstants;
 import javax.xml.xpath.XPathExpressionException;
 import javax.xml.xpath.XPathFactory;
 import java.io.IOException;
import java.io.InputStream;
 import java.util.Arrays;
import java.util.List;
import java.util.Locale;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 /**
@@ -67,7 +65,18 @@ public class Config {
     this( loader, name, null, null );
   }
 
  
  /**
   * For the transition from using solr.xml to solr.properties, see SOLR-4196. Remove
   * for 5.0, thus it's already deprecated
   * @param loader - Solr resource loader
   * @param cfg    - SolrConfig, for backwards compatability with solr.xml layer.
   * @throws TransformerException if the XML file is mal-formed
   */
  @Deprecated
  public Config(SolrResourceLoader loader, Config cfg) throws TransformerException {
    this(loader, null, ConfigSolrXmlBackCompat.copyDoc(cfg.getDocument()));
  }

   public Config(SolrResourceLoader loader, String name, InputSource is, String prefix) throws ParserConfigurationException, IOException, SAXException 
   {
     this(loader, name, is, prefix, true);
diff --git a/solr/core/src/java/org/apache/solr/core/ConfigSolr.java b/solr/core/src/java/org/apache/solr/core/ConfigSolr.java
new file mode 100644
index 00000000000..a4c0859d157
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/ConfigSolr.java
@@ -0,0 +1,83 @@
package org.apache.solr.core;

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

import org.apache.solr.cloud.ZkController;
import org.apache.solr.handler.component.ShardHandlerFactory;
import org.apache.solr.schema.IndexSchema;
import org.apache.zookeeper.KeeperException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigSolr is a new interface  to aid us in obsoleting solr.xml and replacing it with solr.properties. The problem here
 * is that the Config class is used for _all_ the xml file, e.g. solrconfig.xml and we can't mess with _that_ as part
 * of this issue. Primarily used in CoreContainer at present.
 * <p/>
 * This is already deprecated, it's only intended to exist for while transitioning to properties-based replacement for
 * solr.xml
 *
 * @since solr 4.2
 */
@Deprecated
public interface ConfigSolr {

  public static enum ConfLevel {
    SOLR, SOLR_CORES, SOLR_CORES_CORE, SOLR_LOGGING, SOLR_LOGGING_WATCHER
  }

  ;

  public int getInt(ConfLevel level, String tag, int def);

  public boolean getBool(ConfLevel level, String tag, boolean defValue);

  public String get(ConfLevel level, String tag, String def);

  public void substituteProperties();

  public ShardHandlerFactory initShardHandler();

  public Properties getSolrProperties(ConfigSolr cfg, String context);

  public IndexSchema getSchemaFromZk(ZkController zkController, String zkConfigName, String schemaName,
                                     SolrConfig config) throws KeeperException, InterruptedException;

  public SolrConfig getSolrConfigFromZk(ZkController zkController, String zkConfigName, String solrConfigFileName,
                                        SolrResourceLoader resourceLoader);

  public void initPersist();

  public void addPersistCore(String coreName, Properties attribs, Map<String, String> props);

  public void addPersistAllCores(Properties containerProperties, Map<String, String> rootSolrAttribs, Map<String, String> coresAttribs,
                                 File file);

  public String getCoreNameFromOrig(String origCoreName, SolrResourceLoader loader, String coreName);

  public List<String> getAllCoreNames();

  public String getProperty(String coreName, String property, String defaultVal);

  public Properties readCoreProperties(String coreName);

  public Map<String, String> readCoreAttributes(String coreName);
}
diff --git a/solr/core/src/java/org/apache/solr/core/ConfigSolrXmlBackCompat.java b/solr/core/src/java/org/apache/solr/core/ConfigSolrXmlBackCompat.java
new file mode 100644
index 00000000000..5a207e72e65
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/ConfigSolrXmlBackCompat.java
@@ -0,0 +1,358 @@
package org.apache.solr.core;

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

import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.SolrException;
import org.apache.solr.handler.component.HttpShardHandlerFactory;
import org.apache.solr.handler.component.ShardHandlerFactory;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.util.DOMUtil;
import org.apache.solr.util.PropertiesUtil;
import org.apache.solr.util.SystemIdResolver;
import org.apache.zookeeper.KeeperException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigSolrXmlBackCompat
 * <p/>
 * This class is entirely to localize the backwards compatibility for dealing with specific issues when transitioning
 * from solr.xml to a solr.properties-based, enumeration/discovery of defined cores. See SOLR-4196 for background.
 * <p/>
 * As of Solr 5.0, solr.xml will be deprecated, use SolrProperties.
 *
 * @since solr 4.2
 * @deprecated use {@link org.apache.solr.core.SolrProperties} instead
 */
@Deprecated

public class ConfigSolrXmlBackCompat extends Config implements ConfigSolr {

  private static Map<ConfLevel, String> prefixes;
  private NodeList coreNodes = null;

  static {
    prefixes = new HashMap<ConfLevel, String>();

    prefixes.put(ConfLevel.SOLR, "solr/@");
    prefixes.put(ConfLevel.SOLR_CORES, "solr/cores/@");
    prefixes.put(ConfLevel.SOLR_CORES_CORE, "solr/cores/core/@");
    prefixes.put(ConfLevel.SOLR_LOGGING, "solr/logging/@");
    prefixes.put(ConfLevel.SOLR_LOGGING_WATCHER, "solr/logging/watcher/@");
  }

  public ConfigSolrXmlBackCompat(SolrResourceLoader loader, String name, InputStream is, String prefix,
                                 boolean subProps) throws ParserConfigurationException, IOException, SAXException {
    super(loader, name, new InputSource(is), prefix, subProps);
    coreNodes = (NodeList) evaluate("solr/cores/core",
        XPathConstants.NODESET);

  }


  public ConfigSolrXmlBackCompat(SolrResourceLoader loader, Config cfg) throws TransformerException {
    super(loader, null, copyDoc(cfg.getDocument())); // Mimics a call from CoreContainer.
    coreNodes = (NodeList) evaluate("solr/cores/core",
        XPathConstants.NODESET);

  }

  public static Document copyDoc(Document doc) throws TransformerException {
    TransformerFactory tfactory = TransformerFactory.newInstance();
    Transformer tx = tfactory.newTransformer();
    DOMSource source = new DOMSource(doc);
    DOMResult result = new DOMResult();
    tx.transform(source, result);
    return (Document) result.getNode();
  }

  @Override
  public int getInt(ConfLevel level, String tag, int def) {
    return getInt(prefixes.get(level) + tag, def);
  }

  @Override
  public boolean getBool(ConfLevel level, String tag, boolean defValue) {
    return getBool(prefixes.get(level) + tag, defValue);
  }

  @Override
  public String get(ConfLevel level, String tag, String def) {
    return get(prefixes.get(level) + tag, def);
  }

  public ShardHandlerFactory initShardHandler() {
    PluginInfo info = null;
    Node shfn = getNode("solr/cores/shardHandlerFactory", false);

    if (shfn != null) {
      info = new PluginInfo(shfn, "shardHandlerFactory", false, true);
    } else {
      Map m = new HashMap();
      m.put("class", HttpShardHandlerFactory.class.getName());
      info = new PluginInfo("shardHandlerFactory", m, null, Collections.<PluginInfo>emptyList());
    }
    HttpShardHandlerFactory fac = new HttpShardHandlerFactory();
    if (info != null) {
      fac.init(info);
    }
    return fac;
  }

  @Override
  public Properties getSolrProperties(ConfigSolr cfg, String context) {
    try {
      return readProperties(((NodeList) evaluate(
          context, XPathConstants.NODESET)).item(0));
    } catch (Throwable e) {
      SolrException.log(log, null, e);
    }
    return null;

  }

  Properties readProperties(Node node) throws XPathExpressionException {
    XPath xpath = getXPath();
    NodeList props = (NodeList) xpath.evaluate("property", node, XPathConstants.NODESET);
    Properties properties = new Properties();
    for (int i = 0; i < props.getLength(); i++) {
      Node prop = props.item(i);
      properties.setProperty(DOMUtil.getAttr(prop, "name"), DOMUtil.getAttr(prop, "value"));
    }
    return properties;
  }

  @Override
  public Map<String, String> readCoreAttributes(String coreName) {
    Map<String, String> attrs = new HashMap<String, String>();

    synchronized (coreNodes) {
      for (int idx = 0; idx < coreNodes.getLength(); ++idx) {
        Node node = coreNodes.item(idx);
        if (coreName.equals(DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME, null))) {
          NamedNodeMap attributes = node.getAttributes();
          for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String val = attribute.getNodeValue();
            if (CoreDescriptor.CORE_DATADIR.equals(attribute.getNodeName()) ||
                CoreDescriptor.CORE_INSTDIR.equals(attribute.getNodeName())) {
              if (val.indexOf('$') == -1) {
                val = (val != null && !val.endsWith("/"))? val + '/' : val;
              }
            }
            attrs.put(attribute.getNodeName(), val);
          }
          return attrs;
        }
      }
    }
    return attrs;
  }

  public IndexSchema getSchemaFromZk(ZkController zkController, String zkConfigName, String schemaName,
                                     SolrConfig config)
      throws KeeperException, InterruptedException {
    byte[] configBytes = zkController.getConfigFileData(zkConfigName, schemaName);
    InputSource is = new InputSource(new ByteArrayInputStream(configBytes));
    is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(schemaName));
    IndexSchema schema = new IndexSchema(config, schemaName, is);
    return schema;
  }

  @Override
  public SolrConfig getSolrConfigFromZk(ZkController zkController, String zkConfigName, String solrConfigFileName,
                                        SolrResourceLoader resourceLoader) {
    SolrConfig cfg = null;
    try {
      byte[] config = zkController.getConfigFileData(zkConfigName, solrConfigFileName);
      InputSource is = new InputSource(new ByteArrayInputStream(config));
      is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(solrConfigFileName));
      cfg = solrConfigFileName == null ? new SolrConfig(
          resourceLoader, SolrConfig.DEFAULT_CONF_FILE, is) : new SolrConfig(
          resourceLoader, solrConfigFileName, is);
    } catch (Exception e) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
          "getSolrConfigFromZK failed for " + zkConfigName + " " + solrConfigFileName, e);
    }
    return cfg;
  }

  static List<SolrXMLSerializer.SolrCoreXMLDef> solrCoreXMLDefs = new ArrayList<SolrXMLSerializer.SolrCoreXMLDef>();
  // Do this when re-using a ConfigSolrXmlBackCompat.

  // These two methods are part of SOLR-4196 and are awkward, should go away with 5.0
  @Override
  public void initPersist() {
    initPersistStatic();
  }

  public static void initPersistStatic() {
    solrCoreXMLDefs = new ArrayList<SolrXMLSerializer.SolrCoreXMLDef>();
    solrXMLSerializer = new SolrXMLSerializer();
  }

  @Override
  public void addPersistCore(String coreName, Properties attribs, Map<String, String> props) {
    addPersistCore(attribs, props);
  }

  static void addPersistCore(Properties props, Map<String, String> attribs) {
    SolrXMLSerializer.SolrCoreXMLDef solrCoreXMLDef = new SolrXMLSerializer.SolrCoreXMLDef();
    solrCoreXMLDef.coreAttribs = attribs;
    solrCoreXMLDef.coreProperties = props;
    solrCoreXMLDefs.add(solrCoreXMLDef);
  }

  private static SolrXMLSerializer solrXMLSerializer = new SolrXMLSerializer();

  @Override
  public void addPersistAllCores(Properties containerProperties, Map<String, String> rootSolrAttribs, Map<String, String> coresAttribs,
                                 File file) {
    addPersistAllCoresStatic(containerProperties, rootSolrAttribs, coresAttribs, file);
  }

  // Fortunately, we don't iterate over these too often, so the waste is probably tolerable.

  @Override
  public String getCoreNameFromOrig(String origCoreName, SolrResourceLoader loader, String coreName) {

    // look for an existing node
    synchronized (coreNodes) {
      // first look for an exact match
      Node coreNode = null;
      for (int i = 0; i < coreNodes.getLength(); i++) {
        Node node = coreNodes.item(i);

        String name = DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME, null);
        if (origCoreName.equals(name)) {
          if (coreName.equals(origCoreName)) {
            return name;
          }
          return coreName;
        }
      }

      if (coreNode == null) {
        // see if we match with substitution
        for (int i = 0; i < coreNodes.getLength(); i++) {
          Node node = coreNodes.item(i);
          String name = DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME, null);
          if (origCoreName.equals(PropertiesUtil.substituteProperty(name,
              loader.getCoreProperties()))) {
            if (coreName.equals(origCoreName)) {
              return name;
            }
            return coreName;
          }
        }
      }
    }
    return null;
  }

  @Override
  public List<String> getAllCoreNames() {
    List<String> ret = new ArrayList<String>();
    synchronized (coreNodes) {
      for (int idx = 0; idx < coreNodes.getLength(); ++idx) {
        Node node = coreNodes.item(idx);
        ret.add(DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME, null));
      }
    }
    return ret;
  }

  @Override
  public String getProperty(String coreName, String property, String defaultVal) {
    synchronized (coreNodes) {
      for (int idx = 0; idx < coreNodes.getLength(); ++idx) {
        Node node = coreNodes.item(idx);
        if (coreName.equals(DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME, null))) {
          return DOMUtil.getAttr(node, property, defaultVal);
        }
      }
    }
    return defaultVal;
  }

  @Override
  public Properties readCoreProperties(String coreName) {
    synchronized (coreNodes) {
      for (int idx = 0; idx < coreNodes.getLength(); ++idx) {
        Node node = coreNodes.item(idx);
        if (coreName.equals(DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME, null))) {
          try {
            return readProperties(node);
          } catch (XPathExpressionException e) {
            return null;
          }
        }
      }
    }
    return null;
  }

  static void addPersistAllCoresStatic(Properties containerProperties, Map<String, String> rootSolrAttribs, Map<String, String> coresAttribs,
                                       File file) {
    SolrXMLSerializer.SolrXMLDef solrXMLDef = new SolrXMLSerializer.SolrXMLDef();
    solrXMLDef.coresDefs = solrCoreXMLDefs;
    solrXMLDef.containerProperties = containerProperties;
    solrXMLDef.solrAttribs = rootSolrAttribs;
    solrXMLDef.coresAttribs = coresAttribs;
    solrXMLSerializer.persistFile(file, solrXMLDef);

  }

  static final String DEF_SOLR_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
      + "<solr persistent=\"false\">\n"
      + "  <cores adminPath=\"/admin/cores\" defaultCoreName=\""
      + CoreContainer.DEFAULT_DEFAULT_CORE_NAME
      + "\""
      + " host=\"${host:}\" hostPort=\"${hostPort:}\" hostContext=\"${hostContext:}\" zkClientTimeout=\"${zkClientTimeout:15000}\""
      + ">\n"
      + "    <core name=\""
      + CoreContainer.DEFAULT_DEFAULT_CORE_NAME
      + "\" shard=\"${shard:}\" collection=\"${collection:}\" instanceDir=\"collection1\" />\n"
      + "  </cores>\n" + "</solr>";

}
diff --git a/solr/core/src/java/org/apache/solr/core/CoreContainer.java b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
index 4e73837df9c..369df7e7ed4 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreContainer.java
++ b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
@@ -21,9 +21,11 @@ import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
import java.io.FileNotFoundException;
 import java.io.InputStream;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Date;
@@ -35,6 +37,7 @@ import java.util.Locale;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Set;
import java.util.TreeSet;
 import java.util.concurrent.Callable;
 import java.util.concurrent.CompletionService;
 import java.util.concurrent.ConcurrentHashMap;
@@ -46,16 +49,7 @@ import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
 
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
 import org.apache.solr.cloud.CloudDescriptor;
 import org.apache.solr.cloud.CurrentCoreDescriptorProvider;
 import org.apache.solr.cloud.SolrZkServer;
@@ -66,8 +60,7 @@ import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.cloud.ZooKeeperException;
 import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.core.SolrXMLSerializer.SolrCoreXMLDef;
import org.apache.solr.core.SolrXMLSerializer.SolrXMLDef;

 import org.apache.solr.handler.admin.CollectionsHandler;
 import org.apache.solr.handler.admin.CoreAdminHandler;
 import org.apache.solr.handler.component.HttpShardHandlerFactory;
@@ -77,25 +70,20 @@ import org.apache.solr.logging.LogWatcher;
 import org.apache.solr.logging.jul.JulWatcher;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.update.SolrCoreState;
import org.apache.solr.util.DOMUtil;
 import org.apache.solr.util.DefaultSolrThreadFactory;
 import org.apache.solr.util.FileUtils;
import org.apache.solr.util.SystemIdResolver;
import org.apache.solr.util.PropertiesUtil;
 import org.apache.zookeeper.KeeperException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.slf4j.impl.StaticLoggerBinder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
 
 
 /**
  *
  * @since solr 1.3
  */
public class CoreContainer 
public class CoreContainer
 {
   private static final String LEADER_VOTE_WAIT = "180000";  // 3 minutes
   private static final int CORE_LOAD_THREADS = 3;
@@ -104,34 +92,13 @@ public class CoreContainer
   private static final int DEFAULT_ZK_CLIENT_TIMEOUT = 15000;
   public static final String DEFAULT_DEFAULT_CORE_NAME = "collection1";
   private static final boolean DEFAULT_SHARE_SCHEMA = false;
  
  protected static Logger log = LoggerFactory.getLogger(CoreContainer.class);
  
  // solr.xml node constants
  private static final String CORE_NAME = "name";
  private static final String CORE_CONFIG = "config";
  private static final String CORE_INSTDIR = "instanceDir";
  private static final String CORE_DATADIR = "dataDir";
  private static final String CORE_ULOGDIR = "ulogDir";
  private static final String CORE_SCHEMA = "schema";
  private static final String CORE_SHARD = "shard";
  private static final String CORE_COLLECTION = "collection";
  private static final String CORE_ROLES = "roles";
  private static final String CORE_NODE_NAME = "coreNodeName";
  private static final String CORE_PROPERTIES = "properties";
  private static final String CORE_LOADONSTARTUP = "loadOnStartup";
  private static final String CORE_TRANSIENT = "transient";

 
  protected final Map<String, SolrCore> cores = new LinkedHashMap<String, SolrCore>(); // For "permanent" cores

  protected Map<String, SolrCore> transientCores = new LinkedHashMap<String, SolrCore>(); // For "lazily loaded" cores
  protected static Logger log = LoggerFactory.getLogger(CoreContainer.class);
 
  protected final Map<String, CoreDescriptor> dynamicDescriptors = new LinkedHashMap<String, CoreDescriptor>();
 
  protected final Set<String> pendingDynamicCoreLoads = new HashSet<String>();
  private final CoreMaps coreMaps = new CoreMaps(this);
 
  protected final Map<String,Exception> coreInitFailures = 
  protected final Map<String,Exception> coreInitFailures =
     Collections.synchronizedMap(new LinkedHashMap<String,Exception>());
   
   protected boolean persistent = false;
@@ -153,18 +120,19 @@ public class CoreContainer
   protected Integer zkClientTimeout;
   protected String solrHome;
   protected String defaultCoreName = null;
  private SolrXMLSerializer solrXMLSerializer = new SolrXMLSerializer();

   private ZkController zkController;
   private SolrZkServer zkServer;
   private ShardHandlerFactory shardHandlerFactory;
   protected LogWatcher logging = null;
   private String zkHost;
  private Map<SolrCore,String> coreToOrigName = new ConcurrentHashMap<SolrCore,String>();

   private String leaderVoteWait = LEADER_VOTE_WAIT;
   private int distribUpdateConnTimeout = 0;
   private int distribUpdateSoTimeout = 0;
   protected int transientCacheSize = Integer.MAX_VALUE; // Use as a flag too, if transientCacheSize set in solr.xml this will be changed
   private int coreLoadThreads;
  private CloserThread backgroundCloser = null;
   
   {
     log.info("New CoreContainer " + System.identityHashCode(this));
@@ -183,8 +151,7 @@ public class CoreContainer
   /**
    * Initalize CoreContainer directly from the constructor
    */
  public CoreContainer(String dir, File configFile)
  {
  public CoreContainer(String dir, File configFile) throws FileNotFoundException {
     this(dir);
     this.load(dir, configFile);
   }
@@ -253,12 +220,11 @@ public class CoreContainer
           throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR,
               "A chroot was specified in ZkHost but the znode doesn't exist. ");
         }
        
         zkController = new ZkController(this, zookeeperHost, zkClientTimeout,
             zkClientConnectTimeout, host, hostPort, hostContext,
             leaderVoteWait, distribUpdateConnTimeout, distribUpdateSoTimeout,
             new CurrentCoreDescriptorProvider() {
              

               @Override
               public List<CoreDescriptor> getCurrentDescriptors() {
                 List<CoreDescriptor> descriptors = new ArrayList<CoreDescriptor>(
@@ -269,8 +235,8 @@ public class CoreContainer
                 return descriptors;
               }
             });
        
        


         if (zkRun != null && zkServer.getServers().size() > 1 && confDir == null && boostrapConf == false) {
           // we are part of an ensemble and we are not uploading the config - pause to give the config time
           // to get up
@@ -321,24 +287,33 @@ public class CoreContainer
 
   // Helper class to initialize the CoreContainer
   public static class Initializer {
    protected String containerConfigFilename = null;  // normally "solr.xml"
    protected String containerConfigFilename = null;  // normally "solr.xml" becoming solr.properties in 5.0
     protected String dataDir = null; // override datadir for single core mode
 
     // core container instantiation
    public CoreContainer initialize() {
    public CoreContainer initialize() throws FileNotFoundException {
       CoreContainer cores = null;
       String solrHome = SolrResourceLoader.locateSolrHome();
      // ContainerConfigFilename could  could be a properties file
       File fconf = new File(solrHome, containerConfigFilename == null ? "solr.xml"
           : containerConfigFilename);
      log.info("looking for solr.xml: " + fconf.getAbsolutePath());

      log.info("looking for solr config file: " + fconf.getAbsolutePath());
       cores = new CoreContainer(solrHome);

      if (! fconf.exists()) {
        if (StringUtils.isBlank(containerConfigFilename) || containerConfigFilename.endsWith(".xml")) {
          fconf = new File(solrHome, SolrProperties.SOLR_PROPERTIES_FILE);
        }
      }
      // Either we have a config file or not. If it ends in .properties, assume new-style.
       
       if (fconf.exists()) {
         cores.load(solrHome, fconf);
       } else {
        log.info("no solr.xml file found - using default");
        log.info("no solr.xml or solr.properties file found - using default old-style solr.xml");
         try {
          cores.load(solrHome, new InputSource(new ByteArrayInputStream(DEF_SOLR_XML.getBytes("UTF-8"))));
          cores.load(solrHome, new ByteArrayInputStream(ConfigSolrXmlBackCompat.DEF_SOLR_XML.getBytes("UTF-8")), true, null);
         } catch (Exception e) {
           throw new SolrException(ErrorCode.SERVER_ERROR,
               "CoreContainer.Initialize failed when trying to load default solr.xml file", e);
@@ -352,44 +327,6 @@ public class CoreContainer
     }
   }
 
  static Properties getCoreProps(String instanceDir, String file, Properties defaults) {
    if(file == null) file = "conf"+File.separator+ "solrcore.properties";
    File corePropsFile = new File(file);
    if(!corePropsFile.isAbsolute()){
      corePropsFile = new File(instanceDir, file);
    }
    Properties p = defaults;
    if (corePropsFile.exists() && corePropsFile.isFile()) {
      p = new Properties(defaults);
      InputStream is = null;
      try {
        is = new FileInputStream(corePropsFile);
        p.load(is);
      } catch (IOException e) {
        log.warn("Error loading properties ",e);
      } finally{
        IOUtils.closeQuietly(is);        
      }
    }
    return p;
  }

  // Trivial helper method for load, note it implements LRU on transient cores
  private void allocateLazyCores(Config cfg) {
    transientCacheSize = cfg.getInt("solr/cores/@transientCacheSize", Integer.MAX_VALUE);
    if (transientCacheSize != Integer.MAX_VALUE) {
      transientCores = new LinkedHashMap<String, SolrCore>(transientCacheSize, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SolrCore> eldest) {
          if (size() > transientCacheSize) {
            eldest.getValue().close();
            return true;
          }
          return false;
        }
      };
    }
  }
 
   //-------------------------------------------------------------------
   // Initialization / Cleanup
@@ -400,18 +337,20 @@ public class CoreContainer
    * @param dir the home directory of all resources.
    * @param configFile the configuration file
    */
  public void load(String dir, File configFile ) {
  public void load(String dir, File configFile) throws FileNotFoundException {
     this.configFile = configFile;
    this.load(dir, new InputSource(configFile.toURI().toASCIIString()));
    this.load(dir, new FileInputStream(configFile), configFile.getName().endsWith(".xml"),  configFile.getName());
   } 
 
   /**
    * Load a config file listing the available solr cores.
    * 
    * @param dir the home directory of all resources.
   * @param cfgis the configuration file InputStream
   * @param is the configuration file InputStream. May be a properties file or an xml file
    */
  public void load(String dir, InputSource cfgis)  {

  // Let's keep this ugly boolean out of public circulation.
  protected void load(String dir, InputStream is, boolean isXmlFile, String fileName)  {
     ThreadPoolExecutor coreLoadExecutor = null;
     if (null == dir) {
       // don't rely on SolrResourceLoader(), determine explicitly first
@@ -421,28 +360,35 @@ public class CoreContainer
     
     this.loader = new SolrResourceLoader(dir);
     solrHome = loader.getInstanceDir();
    
    Config cfg;

    ConfigSolr cfg;
     
     // keep orig config for persist to consult
    //TODO 5.0: Remove this confusing junk, the properties file is so fast to read that there's no good reason
    //          to add this stuff. Furthermore, it would be good to persist comments when saving.....
     try {
      cfg = new Config(loader, null, cfgis, null, false);
      this.cfg = new Config(loader, null, copyDoc(cfg.getDocument()));
      if (isXmlFile) {
        cfg = new ConfigSolrXmlBackCompat(loader, null, is, null, false);
        this.cfg = new ConfigSolrXmlBackCompat(loader, (ConfigSolrXmlBackCompat)cfg);
      } else {
        cfg = new SolrProperties(this, is, fileName);
        this.cfg = new SolrProperties(this, loader, (SolrProperties)cfg);
      }
     } catch (Exception e) {
       throw new SolrException(ErrorCode.SERVER_ERROR, "", e);
     }
     // Since the cores var is now initialized to null, let's set it up right
     // now.
     cfg.substituteProperties();
    
    initShardHandler(cfg);
    
    allocateLazyCores(cfg);
    

    shardHandlerFactory = cfg.initShardHandler();

    coreMaps.allocateLazyCores(cfg, loader);

     // Initialize Logging
    if (cfg.getBool("solr/logging/@enabled", true)) {
    if (cfg.getBool(ConfigSolr.ConfLevel.SOLR_LOGGING, "enabled", true)) {
       String slf4jImpl = null;
      String fname = cfg.get("solr/logging/watcher/@class", null);
      String fname = cfg.get(ConfigSolr.ConfLevel.SOLR_LOGGING, "class", null);
       try {
         slf4jImpl = StaticLoggerBinder.getSingleton()
             .getLoggerFactoryClassStr();
@@ -475,8 +421,8 @@ public class CoreContainer
         
         if (logging != null) {
           ListenerConfig v = new ListenerConfig();
          v.size = cfg.getInt("solr/logging/watcher/@size", 50);
          v.threshold = cfg.get("solr/logging/watcher/@threshold", null);
          v.size = cfg.getInt(ConfigSolr.ConfLevel.SOLR_LOGGING_WATCHER, "size", 50);
          v.threshold = cfg.get(ConfigSolr.ConfLevel.SOLR_LOGGING_WATCHER, "threshold", null);
           if (v.size > 0) {
             log.info("Registering Log Listener");
             logging.registerListener(v, this);
@@ -485,35 +431,34 @@ public class CoreContainer
       }
     }
     
    String dcoreName = cfg.get("solr/cores/@defaultCoreName", null);
    String dcoreName = cfg.get(ConfigSolr.ConfLevel.SOLR_CORES, "defaultCoreName", null);
     if (dcoreName != null && !dcoreName.isEmpty()) {
       defaultCoreName = dcoreName;
     }
    persistent = cfg.getBool("solr/@persistent", false);
    libDir = cfg.get("solr/@sharedLib", null);
    zkHost = cfg.get("solr/@zkHost", null);
    coreLoadThreads = cfg.getInt("solr/@coreLoadThreads", CORE_LOAD_THREADS);
    
    adminPath = cfg.get("solr/cores/@adminPath", null);
    shareSchema = cfg.getBool("solr/cores/@shareSchema", DEFAULT_SHARE_SCHEMA);
    zkClientTimeout = cfg.getInt("solr/cores/@zkClientTimeout",
        DEFAULT_ZK_CLIENT_TIMEOUT);
    persistent = cfg.getBool(ConfigSolr.ConfLevel.SOLR, "persistent", false);
    libDir = cfg.get(ConfigSolr.ConfLevel.SOLR, "sharedLib", null);
    zkHost = cfg.get(ConfigSolr.ConfLevel.SOLR, "zkHost", null);
    coreLoadThreads = cfg.getInt(ConfigSolr.ConfLevel.SOLR, "coreLoadThreads", CORE_LOAD_THREADS);
     
    distribUpdateConnTimeout = cfg.getInt("solr/cores/@distribUpdateConnTimeout", 0);
    distribUpdateSoTimeout = cfg.getInt("solr/cores/@distribUpdateSoTimeout", 0);
    adminPath = cfg.get(ConfigSolr.ConfLevel.SOLR_CORES, "adminPath", null);
    shareSchema = cfg.getBool(ConfigSolr.ConfLevel.SOLR_CORES, "shareSchema", DEFAULT_SHARE_SCHEMA);
    zkClientTimeout = cfg.getInt(ConfigSolr.ConfLevel.SOLR_CORES, "zkClientTimeout", DEFAULT_ZK_CLIENT_TIMEOUT);
     
    hostPort = cfg.get("solr/cores/@hostPort", DEFAULT_HOST_PORT);
    distribUpdateConnTimeout = cfg.getInt(ConfigSolr.ConfLevel.SOLR_CORES, "distribUpdateConnTimeout", 0);
    distribUpdateSoTimeout = cfg.getInt(ConfigSolr.ConfLevel.SOLR_CORES, "distribUpdateSoTimeout", 0);
     
    hostContext = cfg.get("solr/cores/@hostContext", DEFAULT_HOST_CONTEXT);
    host = cfg.get("solr/cores/@host", null);
    hostPort = cfg.get(ConfigSolr.ConfLevel.SOLR_CORES, "hostPort", DEFAULT_HOST_PORT);

    hostContext = cfg.get(ConfigSolr.ConfLevel.SOLR_CORES, "hostContext", DEFAULT_HOST_CONTEXT);
    host = cfg.get(ConfigSolr.ConfLevel.SOLR_CORES, "host", null);
     
    leaderVoteWait = cfg.get("solr/cores/@leaderVoteWait", LEADER_VOTE_WAIT);
    leaderVoteWait = cfg.get(ConfigSolr.ConfLevel.SOLR_CORES, "leaderVoteWait", LEADER_VOTE_WAIT);
     
     if (shareSchema) {
       indexSchemaCache = new ConcurrentHashMap<String,IndexSchema>();
     }
    adminHandler = cfg.get("solr/cores/@adminHandler", null);
    managementPath = cfg.get("solr/cores/@managementPath", null);
    adminHandler = cfg.get(ConfigSolr.ConfLevel.SOLR_CORES, "adminHandler", null);
    managementPath = cfg.get(ConfigSolr.ConfLevel.SOLR_CORES, "managementPath", null);
     
     zkClientTimeout = Integer.parseInt(System.getProperty("zkClientTimeout",
         Integer.toString(zkClientTimeout)));
@@ -539,17 +484,8 @@ public class CoreContainer
     }
     
     collectionsHandler = new CollectionsHandler(this);
    
    try {
      containerProperties = readProperties(cfg, ((NodeList) cfg.evaluate(
          DEFAULT_HOST_CONTEXT, XPathConstants.NODESET)).item(0));
    } catch (Throwable e) {
      SolrException.log(log, null, e);
    }
    
    NodeList nodes = (NodeList) cfg.evaluate("solr/cores/core",
        XPathConstants.NODESET);
    
    containerProperties = cfg.getSolrProperties(cfg, DEFAULT_HOST_CONTEXT);

     // setup executor to load cores in parallel
     coreLoadExecutor = new ThreadPoolExecutor(coreLoadThreads, coreLoadThreads, 1,
         TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
@@ -559,66 +495,70 @@ public class CoreContainer
       CompletionService<SolrCore> completionService = new ExecutorCompletionService<SolrCore>(
           coreLoadExecutor);
       Set<Future<SolrCore>> pending = new HashSet<Future<SolrCore>>();
      
      for (int i = 0; i < nodes.getLength(); i++) {
        Node node = nodes.item(i);

      List<String> allCores = cfg.getAllCoreNames();

      for (String oneCoreName : allCores) {

         try {
          String rawName = DOMUtil.getAttr(node, CORE_NAME, null);
          String rawName = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_NAME, null);

           if (null == rawName) {
             throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                 "Each core in solr.xml must have a 'name'");
           }
           final String name = rawName;
           final CoreDescriptor p = new CoreDescriptor(this, name,
              DOMUtil.getAttr(node, CORE_INSTDIR, null));
              cfg.getProperty(oneCoreName, CoreDescriptor.CORE_INSTDIR, null));
           
           // deal with optional settings
          String opt = DOMUtil.getAttr(node, CORE_CONFIG, null);
          String opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_CONFIG, null);
           
           if (opt != null) {
             p.setConfigName(opt);
           }
          opt = DOMUtil.getAttr(node, CORE_SCHEMA, null);
          opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_SCHEMA, null);
           if (opt != null) {
             p.setSchemaName(opt);
           }
           
           if (zkController != null) {
            opt = DOMUtil.getAttr(node, CORE_SHARD, null);
            opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_SHARD, null);
             if (opt != null && opt.length() > 0) {
               p.getCloudDescriptor().setShardId(opt);
             }
            opt = DOMUtil.getAttr(node, CORE_COLLECTION, null);
            opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_COLLECTION, null);
             if (opt != null) {
               p.getCloudDescriptor().setCollectionName(opt);
             }
            opt = DOMUtil.getAttr(node, CORE_ROLES, null);
            opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_ROLES, null);
             if (opt != null) {
               p.getCloudDescriptor().setRoles(opt);
             }
            opt = DOMUtil.getAttr(node, CORE_NODE_NAME, null);

            opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_NODE_NAME, null);
             if (opt != null && opt.length() > 0) {
               p.getCloudDescriptor().setCoreNodeName(opt);
             }
           }
          opt = DOMUtil.getAttr(node, CORE_PROPERTIES, null);
          opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_PROPERTIES, null);
           if (opt != null) {
             p.setPropertiesName(opt);
           }
          opt = DOMUtil.getAttr(node, CORE_DATADIR, null);
          opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_DATADIR, null);
           if (opt != null) {
             p.setDataDir(opt);
           }
           
          p.setCoreProperties(readProperties(cfg, node));
          p.setCoreProperties(cfg.readCoreProperties(oneCoreName));
           
          opt = DOMUtil.getAttr(node, CORE_LOADONSTARTUP, null);
          opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_LOADONSTARTUP, null);
           if (opt != null) {
             p.setLoadOnStartup(("true".equalsIgnoreCase(opt) || "on"
                 .equalsIgnoreCase(opt)) ? true : false);
           }
           
          opt = DOMUtil.getAttr(node, CORE_TRANSIENT, null);
          opt = cfg.getProperty(oneCoreName, CoreDescriptor.CORE_TRANSIENT, null);
           if (opt != null) {
             p.setTransient(("true".equalsIgnoreCase(opt) || "on"
                 .equalsIgnoreCase(opt)) ? true : false);
@@ -632,12 +572,7 @@ public class CoreContainer
                 SolrCore c = null;
                 try {
                   c = create(p);
                  if (p.isTransient()) {
                    registerLazyCore(name, c, false);
                  } else {
                    register(name, c, false);
                  }

                  registerCore(p.isTransient(), name, c, false);
                 } catch (Throwable t) {
                   SolrException.log(log, null, t);
                   if (c != null) {
@@ -648,14 +583,12 @@ public class CoreContainer
               }
             };
 

             pending.add(completionService.submit(task));
 
            
           } else {
             // Store it away for later use. includes non-transient but not
             // loaded at startup cores.
            dynamicDescriptors.put(rawName, p);
            coreMaps.putDynamicDescriptor(rawName, p);
           }
         } catch (Throwable ex) {
           SolrException.log(log, null, ex);
@@ -664,6 +597,7 @@ public class CoreContainer
       
       while (pending != null && pending.size() > 0) {
         try {

           Future<SolrCore> future = completionService.take();
           if (future == null) return;
           pending.remove(future);
@@ -672,10 +606,10 @@ public class CoreContainer
             SolrCore c = future.get();
             // track original names
             if (c != null) {
              coreToOrigName.put(c, c.getName());
              coreMaps.putCoreToOrigName(c, c.getName());
             }
           } catch (ExecutionException e) {
            SolrException.log(SolrCore.log, "error loading core", e);
            SolrException.log(SolrCore.log, "Error loading core", e);
           }
           
         } catch (InterruptedException e) {
@@ -683,6 +617,11 @@ public class CoreContainer
               "interrupted while loading core", e);
         }
       }

      // Start the background thread
      backgroundCloser = new CloserThread(this, coreMaps, cfg);
      backgroundCloser.start();

     } finally {
       if (coreLoadExecutor != null) {
         ExecutorUtil.shutdownNowAndAwaitTermination(coreLoadExecutor);
@@ -690,50 +629,20 @@ public class CoreContainer
     }
   }
 
  protected void initShardHandler(Config cfg) {
    PluginInfo info = null;
  // To make this available to TestHarness.
  protected void initShardHandler() {
     if (cfg != null) {
      Node shfn = cfg.getNode("solr/cores/shardHandlerFactory", false);
  
      if (shfn != null) {
        info = new PluginInfo(shfn, "shardHandlerFactory", false, true);
      } else {
        Map m = new HashMap();
        m.put("class",HttpShardHandlerFactory.class.getName());
        info = new PluginInfo("shardHandlerFactory", m, null, Collections.<PluginInfo>emptyList());
      }
    }

    HttpShardHandlerFactory fac = new HttpShardHandlerFactory();
    if (info != null) {
      fac.init(info);
      cfg.initShardHandler();
    } else {
      // Cough! Hack! But tests run this way.
      HttpShardHandlerFactory fac = new HttpShardHandlerFactory();
      shardHandlerFactory = fac;
     }
    shardHandlerFactory = fac;
  }

  private Document copyDoc(Document document) throws TransformerException {
    TransformerFactory tfactory = TransformerFactory.newInstance();
    Transformer tx   = tfactory.newTransformer();
    DOMSource source = new DOMSource(document);
    DOMResult result = new DOMResult();
    tx.transform(source,result);
    return (Document)result.getNode();
   }
 
  private Properties readProperties(Config cfg, Node node) throws XPathExpressionException {
    XPath xpath = cfg.getXPath();
    NodeList props = (NodeList) xpath.evaluate("property", node, XPathConstants.NODESET);
    Properties properties = new Properties();
    for (int i=0; i<props.getLength(); i++) {
      Node prop = props.item(i);
      properties.setProperty(DOMUtil.getAttr(prop, "name"), DOMUtil.getAttr(prop, "value"));
    }
    return properties;
  }
  
   private volatile boolean isShutDown = false;
 
  private volatile Config cfg;
  private volatile ConfigSolr cfg;
   
   public boolean isShutDown() {
     return isShutDown;
@@ -756,35 +665,35 @@ public class CoreContainer
         log.warn("", e);
       }
     }
    
     isShutDown = true;
    

     if (isZooKeeperAware()) {
      coreMaps.publishCoresAsDown(zkController);
       cancelCoreRecoveries();
     }
    
    try {
      synchronized (cores) {
 
        for (SolrCore core : cores.values()) {
          try {
            core.close();
          } catch (Throwable t) {
            SolrException.log(log, "Error shutting down core", t);
          }
        }
        cores.clear();

    try {
      // First allow the closer thread to drain all the pending closes it can.
      synchronized (coreMaps.getLocker()) {
        coreMaps.getLocker().notifyAll(); // wake up anyone waiting
       }
      synchronized (transientCores) {
        for (SolrCore core : transientCores.values()) {
          try {
            core.close();
          } catch (Throwable t) {
            SolrException.log(log, "Error shutting down core", t);
          }
      if (backgroundCloser != null) { // Doesn't seem right, but tests get in here without initializing the core.
        try {
          backgroundCloser.join();
        } catch (InterruptedException e) {
          ; // Don't much care if this gets interrupted
         }
        transientCores.clear();
       }
      // Now clear all the cores that are being operated upon.
      coreMaps.clearMaps(cfg);

      // It's still possible that one of the pending dynamic load operation is waiting, so wake it up if so.
      // Since all the pending operations queues have been drained, there should be nothing to do.
      synchronized (coreMaps.getLocker()) {
        coreMaps.getLocker().notifyAll(); // wake up the thread
      }

     } finally {
       if (shardHandlerFactory != null) {
         shardHandlerFactory.close();
@@ -797,17 +706,12 @@ public class CoreContainer
       if (zkServer != null) {
         zkServer.stop();
       }
      
     }
   }
 
   public void cancelCoreRecoveries() {
     ArrayList<SolrCoreState> coreStates = new ArrayList<SolrCoreState>();
    synchronized (cores) {
      for (SolrCore core : cores.values()) {
        coreStates.add(core.getUpdateHandler().getSolrCoreState());
      }
    }
    coreMaps.addCoresToList(coreStates);
 
     // we must cancel without holding the cores sync
     // make sure we wait for any recoveries to stop
@@ -831,21 +735,7 @@ public class CoreContainer
     }
   }
 
  /**
   * Registers a SolrCore descriptor in the registry using the specified name.
   * If returnPrevNotClosed==false, the old core, if different, is closed. if true, it is returned w/o closing the core
   *
   * @return a previous core having the same name if it existed
   */
  public SolrCore register(String name, SolrCore core, boolean returnPrevNotClosed) {
    return registerCore(cores, name, core, returnPrevNotClosed);
  }

  protected SolrCore registerLazyCore(String name, SolrCore core, boolean returnPrevNotClosed) {
    return registerCore(transientCores, name, core, returnPrevNotClosed);
  }

  protected SolrCore registerCore(Map<String,SolrCore> whichCores, String name, SolrCore core, boolean returnPrevNotClosed) {
  protected SolrCore registerCore(boolean isTransientCore, String name, SolrCore core, boolean returnPrevNotClosed) {
     if( core == null ) {
       throw new RuntimeException( "Can not register a null core." );
     }
@@ -872,19 +762,26 @@ public class CoreContainer
     }
     
     SolrCore old = null;
    synchronized (whichCores) {
      if (isShutDown) {
        core.close();
        throw new IllegalStateException("This CoreContainer has been shutdown");
      }
      old = whichCores.put(name, core);
      coreInitFailures.remove(name);

    if (isShutDown) {
      core.close();
      throw new IllegalStateException("This CoreContainer has been shutdown");
    }
    if (isTransientCore) {
      old = coreMaps.putTransientCore(cfg, name, core, loader);
    } else {
      old = coreMaps.putCore(name, core);
    }
       /*
       * set both the name of the descriptor and the name of the
       * core, since the descriptors name is used for persisting.
       */
      core.setName(name);
      core.getCoreDescriptor().name = name;

    core.setName(name);
    core.getCoreDescriptor().putProperty(CoreDescriptor.CORE_NAME, name);

    synchronized (coreInitFailures) {
      coreInitFailures.remove(name);
     }
 
     if( old == null || old == core) {
@@ -940,9 +837,14 @@ public class CoreContainer
    * @return a previous core having the same name if it existed and returnPrev==true
    */
   public SolrCore register(SolrCore core, boolean returnPrev) {
    return register(core.getName(), core, returnPrev);
    return registerCore(false, core.getName(), core, returnPrev);
  }

  public SolrCore register(String name, SolrCore core, boolean returnPrev) {
    return registerCore(false, name, core, returnPrev);
   }
 

   // Helper method to separate out creating a core from ZK as opposed to the "usual" way. See create()
   private SolrCore createFromZk(String instanceDir, CoreDescriptor dcore)
   {
@@ -960,10 +862,9 @@ public class CoreContainer
         throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR,
             "Could not find config name for collection:" + collection);
       }
      solrLoader = new ZkSolrResourceLoader(instanceDir, zkConfigName, libLoader, getCoreProps(instanceDir,
          dcore.getPropertiesName(), dcore.getCoreProperties()), zkController);
      solrLoader = new ZkSolrResourceLoader(instanceDir, zkConfigName, libLoader, SolrProperties.getCoreProperties(instanceDir, dcore), zkController);
       config = getSolrConfigFromZk(zkConfigName, dcore.getConfigName(), solrLoader);
      schema = getSchemaFromZk(zkConfigName, dcore.getSchemaName(), config, solrLoader);
      schema = getSchemaFromZk(zkConfigName, dcore.getSchemaName(), config);
       return new SolrCore(dcore.getName(), null, config, schema, dcore);
 
     } catch (KeeperException e) {
@@ -984,10 +885,11 @@ public class CoreContainer
     SolrResourceLoader solrLoader = null;
 
     SolrConfig config = null;
    solrLoader = new SolrResourceLoader(instanceDir, libLoader, getCoreProps(instanceDir, dcore.getPropertiesName(), dcore.getCoreProperties()));
    solrLoader = new SolrResourceLoader(instanceDir, libLoader, SolrProperties.getCoreProperties(instanceDir, dcore));
     try {
       config = new SolrConfig(solrLoader, dcore.getConfigName(), null);
     } catch (Exception e) {
      log.error("Failed to load file {}/{}", instanceDir, dcore.getConfigName());
       throw new SolrException(ErrorCode.SERVER_ERROR, "Could not load config for " + dcore.getConfigName(), e);
     }
 
@@ -1005,11 +907,11 @@ public class CoreContainer
             schemaFile.lastModified()));
         schema = indexSchemaCache.get(key);
         if (schema == null) {
          log.info("creating new schema object for core: " + dcore.name);
          log.info("creating new schema object for core: " + dcore.getProperty(CoreDescriptor.CORE_NAME));
           schema = new IndexSchema(config, dcore.getSchemaName(), null);
           indexSchemaCache.put(key, schema);
         } else {
          log.info("re-using schema object for core: " + dcore.name);
          log.info("re-using schema object for core: " + dcore.getProperty(CoreDescriptor.CORE_NAME));
         }
       }
     }
@@ -1041,7 +943,7 @@ public class CoreContainer
       // Make the instanceDir relative to the cores instanceDir if not absolute
       File idir = new File(dcore.getInstanceDir());
       String instanceDir = idir.getPath();
      log.info("Creating SolrCore '{}' using instanceDir: {}", 
      log.info("Creating SolrCore '{}' using instanceDir: {}",
                dcore.getName(), instanceDir);
 
       // Initialize the solr config
@@ -1062,48 +964,30 @@ public class CoreContainer
    * @return a Collection of registered SolrCores
    */
   public Collection<SolrCore> getCores() {
    List<SolrCore> lst = new ArrayList<SolrCore>();
    synchronized (cores) {
      lst.addAll(this.cores.values());
    }
    return lst;
    return coreMaps.getCores();
   }
 
   /**
    * @return a Collection of the names that cores are mapped to
    */
   public Collection<String> getCoreNames() {
    List<String> lst = new ArrayList<String>();
    synchronized (cores) {
      lst.addAll(this.cores.keySet());
    }
    synchronized (transientCores) {
      lst.addAll(this.transientCores.keySet());
    }
    return lst;
    return coreMaps.getCoreNames();
   }
 
   /** This method is currently experimental.
    * @return a Collection of the names that a specific core is mapped to.
    */
   public Collection<String> getCoreNames(SolrCore core) {
    List<String> lst = new ArrayList<String>();
    synchronized (cores) {
      for (Map.Entry<String,SolrCore> entry : cores.entrySet()) {
        if (core == entry.getValue()) {
          lst.add(entry.getKey());
        }
      }
    }
    synchronized (transientCores) {
      for (Map.Entry<String,SolrCore> entry : transientCores.entrySet()) {
        if (core == entry.getValue()) {
          lst.add(entry.getKey());
        }
      }
    }
    return coreMaps.getCoreNames(core);
  }

  /**
   * get a list of all the cores that are currently loaded
   * @return a list of al lthe available core names in either permanent or transient core lists.
   */
  public Collection<String> getAllCoreNames() {
    return coreMaps.getAllCoreNames();
 
    return lst;
   }
 
   /**
@@ -1144,10 +1028,7 @@ public class CoreContainer
     try {
 
       name= checkDefault(name);
      SolrCore core;
      synchronized(cores) {
        core = cores.get(name);
      }
      SolrCore core = coreMaps.getCore(name);
       if (core == null)
         throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "No such core: " + name );
 
@@ -1160,7 +1041,7 @@ public class CoreContainer
     
       SolrResourceLoader solrLoader;
       if(zkController == null) {
        solrLoader = new SolrResourceLoader(instanceDir.getAbsolutePath(), libLoader, getCoreProps(instanceDir.getAbsolutePath(), cd.getPropertiesName(),cd.getCoreProperties()));
        solrLoader = new SolrResourceLoader(instanceDir.getAbsolutePath(), libLoader, SolrProperties.getCoreProperties(instanceDir.getAbsolutePath(), cd));
       } else {
         try {
           String collection = cd.getCloudDescriptor().getCollectionName();
@@ -1172,7 +1053,8 @@ public class CoreContainer
             throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR,
                                          "Could not find config name for collection:" + collection);
           }
          solrLoader = new ZkSolrResourceLoader(instanceDir.getAbsolutePath(), zkConfigName, libLoader, getCoreProps(instanceDir.getAbsolutePath(), cd.getPropertiesName(),cd.getCoreProperties()), zkController);
          solrLoader = new ZkSolrResourceLoader(instanceDir.getAbsolutePath(), zkConfigName, libLoader,
              SolrProperties.getCoreProperties(instanceDir.getAbsolutePath(), cd), zkController);
         } catch (KeeperException e) {
           log.error("", e);
           throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR,
@@ -1185,14 +1067,11 @@ public class CoreContainer
                                        "", e);
         }
       }
    
       SolrCore newCore = core.reload(solrLoader, core);
       // keep core to orig name link
      String origName = coreToOrigName.remove(core);
      if (origName != null) {
        coreToOrigName.put(newCore, origName);
      }
      register(name, newCore, false);
      coreMaps.removeCoreToOrigName(newCore, core);

      registerCore(false, name, newCore, false);
 
       // :TODO: Java7...
       // http://docs.oracle.com/javase/7/docs/technotes/guides/language/catch-multiple.html
@@ -1214,22 +1093,7 @@ public class CoreContainer
     }
     n0 = checkDefault(n0);
     n1 = checkDefault(n1);
    synchronized( cores ) {
      SolrCore c0 = cores.get(n0);
      SolrCore c1 = cores.get(n1);
      if (c0 == null)
        throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "No such core: " + n0 );
      if (c1 == null)
        throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "No such core: " + n1 );
      cores.put(n0, c1);
      cores.put(n1, c0);

      c0.setName(n1);
      c0.getCoreDescriptor().name = n1;
      c1.setName(n0);
      c1.getCoreDescriptor().name = n0;
    }

    coreMaps.swap(n0, n1);
 
     log.info("swapped: "+n0 + " with " + n1);
   }
@@ -1238,13 +1102,7 @@ public class CoreContainer
   public SolrCore remove( String name ) {
     name = checkDefault(name);    
 
    synchronized(cores) {
      SolrCore core = cores.remove( name );
      if (core != null) {
        coreToOrigName.remove(core);
      }
      return core;
    }
    return coreMaps.remove(name, true);
 
   }
 
@@ -1252,12 +1110,9 @@ public class CoreContainer
     SolrCore core = getCore(name);
     try {
       if (core != null) {
        register(toName, core, false);
        registerCore(false, toName, core, false);
         name = checkDefault(name);
        
        synchronized (cores) {
          cores.remove(name);
        }
        coreMaps.remove(name, false);
       }
     } finally {
       if (core != null) {
@@ -1265,84 +1120,40 @@ public class CoreContainer
       }
     }
   }
  private SolrCore getCoreFromAnyList(String name) {
    SolrCore core;
    synchronized (cores) {
      core = cores.get(name);
      if (core != null) {
        core.open();    // increment the ref count while still synchronized
        return core;
      }
    }

    if (dynamicDescriptors.size() == 0) return null; // Nobody even tried to define any transient cores, so we're done.

    // Now look for already loaded transient cores.
    synchronized (transientCores) {
      core = transientCores.get(name);
      if (core != null) {
        core.open();
        return core;
      }
    }
    return null;
  }
   /** Gets a core by name and increase its refcount.
    * @see SolrCore#close() 
    * @param name the core name
    * @return the core if found
    */
   public SolrCore getCore(String name) {

     name = checkDefault(name);
     // Do this in two phases since we don't want to lock access to the cores over a load.
    SolrCore core = getCoreFromAnyList(name);
    SolrCore core = coreMaps.getCoreFromAnyList(name);
 
     if (core != null) return core;
 
     // OK, it's not presently in any list, is it in the list of dynamic cores but not loaded yet? If so, load it.
    CoreDescriptor desc =  dynamicDescriptors.get(name);
    CoreDescriptor desc = coreMaps.getDynamicDescriptor(name);
     if (desc == null) { //Nope, no transient core with this name
       return null;
     }
 
    // Keep multiple threads from loading the same core at the same time.
    try {
      boolean isPending;
      synchronized (pendingDynamicCoreLoads) {
        isPending = pendingDynamicCoreLoads.contains(name);
        if (! isPending) {
          pendingDynamicCoreLoads.add(name);
        }
      }
    core = coreMaps.waitPendingCoreOps(name); // This will put an entry in pending core ops if the core isn't loaded
 
      while (isPending) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          return null; // Seems best not to do anything at all if the thread is interrupted
        }
    if (isShutDown) return null; // We're quitting, so stop. This needs to be after the wait above since we may come off
                                 // the wait as a consequence of shutting down.
 
        synchronized (pendingDynamicCoreLoads) {
          if (!pendingDynamicCoreLoads.contains(name)) {
            // NOTE: If, for some reason, the load failed, we'll return null here and presumably the log will show
            // why. We'll fail all over again next time if the problem isn't corrected.
            return getCoreFromAnyList(name);
          }
        }
      }
    if (core == null) {
       try {
         core = create(desc); // This should throw an error if it fails.
         core.open();
        if (desc.isTransient()) {
          registerLazyCore(name, core, false);    // This is a transient core
        } else {
          register(name, core, false); // This is a "permanent", although deferred-load core
        }
        registerCore(desc.isTransient(), name, core, false);
       } catch (Exception ex) {
        throw recordAndThrow(name, "Unable to create core" + name, ex);
        throw recordAndThrow(name, "Unable to create core: " + name, ex);
      } finally {
        coreMaps.releasePending(name);
       }
    } finally {
      pendingDynamicCoreLoads.remove(name);
     }
     return core;
   }
@@ -1417,12 +1228,31 @@ public class CoreContainer
   public File getConfigFile() {
     return configFile;
   }
  
/** Persists the cores config file in cores.xml. */

  /**
   * Determines whether the core is already loaded or not but does NOT load the core
   *
   */
  public boolean isLoaded(String name) {
    return coreMaps.isLoaded(name);
  }

  /** Persists the cores config file in cores.xml. */
   public void persist() {
     persistFile(null);
   }
 
  /**
   * Gets a solr core descriptor for a core that is not loaded. Note that if the caller calls this on a
   * loaded core, the unloaded descriptor will be returned.
   *
   * @param cname - name of the unloaded core descriptor to load. NOTE:
   * @return a coreDescriptor. May return null
   */
  public CoreDescriptor getUnloadedCoreDescriptor(String cname) {
    return coreMaps.getUnloadedCoreDescriptor(cname);
  }

   /** Persists the cores config file in a user provided file. */
   public void persistFile(File file) {
     log.info("Persisting cores config to " + (file == null ? configFile : file));
@@ -1458,122 +1288,9 @@ public class CoreContainer
     addCoresAttrib(coresAttribs, "leaderVoteWait", this.leaderVoteWait, LEADER_VOTE_WAIT);
     addCoresAttrib(coresAttribs, "coreLoadThreads", Integer.toString(this.coreLoadThreads), Integer.toString(CORE_LOAD_THREADS));
 
    List<SolrCoreXMLDef> solrCoreXMLDefs = new ArrayList<SolrCoreXMLDef>();
    
    synchronized (cores) {
      for (SolrCore solrCore : cores.values()) {
        Map<String,String> coreAttribs = new HashMap<String,String>();
        CoreDescriptor dcore = solrCore.getCoreDescriptor();

        String coreName = dcore.name;
        Node coreNode = null;
        
        if (cfg != null) {
          NodeList nodes = (NodeList) cfg.evaluate("solr/cores/core",
              XPathConstants.NODESET);
          
          String origCoreName = coreToOrigName.get(solrCore);

          if (origCoreName == null) {
            origCoreName = coreName;
          }
          
          // look for an existing node
          
          // first look for an exact match
          for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            
            String name = DOMUtil.getAttr(node, CORE_NAME, null);
            if (origCoreName.equals(name)) {
              coreNode = node;
              if (coreName.equals(origCoreName)) {
                coreName = name;
              }
              break;
            }
          }
          
          if (coreNode == null) {
            // see if we match with substitution
            for (int i = 0; i < nodes.getLength(); i++) {
              Node node = nodes.item(i);
              String name = DOMUtil.getAttr(node, CORE_NAME, null);
              if (origCoreName.equals(DOMUtil.substituteProperty(name,
                  loader.getCoreProperties()))) {
                coreNode = node;
                if (coreName.equals(origCoreName)) {
                  coreName = name;
                }
                break;
              }
            }
          }
        }
    coreMaps.persistCores(cfg, containerProperties, rootSolrAttribs, coresAttribs, file, configFile, loader);
 
        coreAttribs.put(CORE_NAME, coreName);
        
        String instanceDir = dcore.getRawInstanceDir();
        addCoreProperty(coreAttribs, coreNode, CORE_INSTDIR, instanceDir, null);
        
        // write config 
        String configName = dcore.getConfigName();
        addCoreProperty(coreAttribs, coreNode, CORE_CONFIG, configName, dcore.getDefaultConfigName());
        
        // write schema
        String schema = dcore.getSchemaName();
        addCoreProperty(coreAttribs, coreNode, CORE_SCHEMA, schema, dcore.getDefaultSchemaName());
        
        String dataDir = dcore.dataDir;
        String ulogDir = dcore.ulogDir;
        addCoreProperty(coreAttribs, coreNode, CORE_DATADIR, dataDir, null);
        addCoreProperty(coreAttribs, coreNode, CORE_ULOGDIR, ulogDir, null);
        addCoreProperty(coreAttribs, coreNode, CORE_TRANSIENT, Boolean.toString(dcore.isTransient()), null);
        addCoreProperty(coreAttribs, coreNode, CORE_LOADONSTARTUP, Boolean.toString(dcore.isLoadOnStartup()), null);

        CloudDescriptor cd = dcore.getCloudDescriptor();
        String shard = null;
        String roles = null;
        if (cd != null) {
          shard = cd.getShardId();
          roles = cd.getRoles();
        }
        addCoreProperty(coreAttribs, coreNode, CORE_SHARD, shard, null);
        
        addCoreProperty(coreAttribs, coreNode, CORE_ROLES, roles, null);
        
        String collection = null;
        // only write out the collection name if it's not the default (the
        // core
        // name)
        if (cd != null) {
          collection = cd.getCollectionName();
        }
        
        addCoreProperty(coreAttribs, coreNode, CORE_COLLECTION, collection, dcore.name);
        
        // we don't try and preserve sys prop defs in these
        String opt = dcore.getPropertiesName();
        if (opt != null) {
          coreAttribs.put(CORE_PROPERTIES, opt);
        }
        
        SolrCoreXMLDef solrCoreXMLDef = new SolrCoreXMLDef();
        solrCoreXMLDef.coreAttribs = coreAttribs;
        solrCoreXMLDef.coreProperties = dcore.getCoreProperties();
        solrCoreXMLDefs.add(solrCoreXMLDef);
      }
      
      SolrXMLDef solrXMLDef = new SolrXMLDef();
      solrXMLDef.coresDefs = solrCoreXMLDefs;
      solrXMLDef.containerProperties = containerProperties;
      solrXMLDef.solrAttribs = rootSolrAttribs;
      solrXMLDef.coresAttribs = coresAttribs;
      solrXMLSerializer.persistFile(file == null ? configFile : file,
          solrXMLDef);
    }
   }

   private String intToString(Integer integer) {
     if (integer == null) return null;
     return Integer.toString(integer);
@@ -1586,9 +1303,10 @@ public class CoreContainer
     }
     
     if (attribValue != null) {
      String rawValue = cfg.get("solr/cores/@" + attribName, null);
      String rawValue = cfg.get(ConfigSolr.ConfLevel.SOLR_CORES, attribName, null);
       if (rawValue == null && defaultValue != null && attribValue.equals(defaultValue)) return;
      if (attribValue.equals(DOMUtil.substituteProperty(rawValue, loader.getCoreProperties()))) {

      if (attribValue.equals(PropertiesUtil.substituteProperty(rawValue, loader.getCoreProperties()))) {
         coresAttribs.put(attribName, rawValue);
       } else {
         coresAttribs.put(attribName, attribValue);
@@ -1596,32 +1314,6 @@ public class CoreContainer
     }
   }
 
  private void addCoreProperty(Map<String,String> coreAttribs, Node node, String name,
      String value, String defaultValue) {
    if (node == null) {
      coreAttribs.put(name, value);
      return;
    }
    
    if (node != null) {
      String rawAttribValue = DOMUtil.getAttr(node, name, null);
      if (value == null) {
        coreAttribs.put(name, rawAttribValue);
        return;
      }
      if (rawAttribValue == null && defaultValue != null && value.equals(defaultValue)) {
        return;
      }
      if (rawAttribValue != null && value.equals(DOMUtil.substituteProperty(rawAttribValue, loader.getCoreProperties()))){
        coreAttribs.put(name, rawAttribValue);
      } else {
        coreAttribs.put(name, value);
      }
    }

  }


   public String getSolrHome() {
     return solrHome;
   }
@@ -1646,22 +1338,8 @@ public class CoreContainer
   private SolrConfig getSolrConfigFromZk(String zkConfigName, String solrConfigFileName,
       SolrResourceLoader resourceLoader)
   {
    SolrConfig cfg = null;
    try {
      byte[] config = zkController.getConfigFileData(zkConfigName, solrConfigFileName);
      InputSource is = new InputSource(new ByteArrayInputStream(config));
      is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(solrConfigFileName));
      cfg = solrConfigFileName == null ? new SolrConfig(
          resourceLoader, SolrConfig.DEFAULT_CONF_FILE, is) : new SolrConfig(
          resourceLoader, solrConfigFileName, is);
    } catch (Exception e) {
      throw new SolrException(ErrorCode.SERVER_ERROR,
          "getSolrConfigFromZK failed for " + zkConfigName + " " + solrConfigFileName, e);
    }

    return cfg;
    return cfg.getSolrConfigFromZk(zkController, zkConfigName, solrConfigFileName, resourceLoader);
   }

   // Just to tidy up the code where it did this in-line.
   private SolrException recordAndThrow(String name, String msg, Exception ex) {
     synchronized (coreInitFailures) {
@@ -1671,25 +1349,590 @@ public class CoreContainer
     log.error(msg, ex);
     return new SolrException(ErrorCode.SERVER_ERROR, msg, ex);
   }

   private IndexSchema getSchemaFromZk(String zkConfigName, String schemaName,
      SolrConfig config, SolrResourceLoader resourceLoader)
      SolrConfig config)
       throws KeeperException, InterruptedException {
    byte[] configBytes = zkController.getConfigFileData(zkConfigName, schemaName);
    InputSource is = new InputSource(new ByteArrayInputStream(configBytes));
    is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(schemaName));
    IndexSchema schema = new IndexSchema(config, schemaName, is);
    return schema;
    return cfg.getSchemaFromZk(zkController, zkConfigName, schemaName, config);
  }
}


// Introducing the two new maps (transientCores and dynamicDescriptors) introduced some locking complexities. Rather
// than try to keep them all straight in the code, use this class you need to access any of:
// cores
// transientCores
// dynamicDescriptors
//

class CoreMaps {

  private static Object locker = new Object(); // for locking around manipulating any of the core maps.
  private final Map<String, SolrCore> cores = new LinkedHashMap<String, SolrCore>(); // For "permanent" cores

  //WARNING! The _only_ place you put anything into the list of transient cores is with the putTransientCore method!
  private Map<String, SolrCore> transientCores = new LinkedHashMap<String, SolrCore>(); // For "lazily loaded" cores

  private final Map<String, CoreDescriptor> dynamicDescriptors = new LinkedHashMap<String, CoreDescriptor>();

  private int transientCacheSize = Integer.MAX_VALUE;

  private Map<SolrCore, String> coreToOrigName = new ConcurrentHashMap<SolrCore, String>();

  private final CoreContainer container;

  // It's a little clumsy to have two, but closing requires a SolrCore, whereas pending loads don't have a core.
  private static final Set<String> pendingDynamicLoads = new TreeSet<String>();

  // Holds cores from the time they're removed from the transient cache until after they're closed.
  private static final List<SolrCore> pendingDynamicCloses = new ArrayList<SolrCore>();

  CoreMaps(CoreContainer container) {
    this.container = container;
  }

  // Trivial helper method for load, note it implements LRU on transient cores. Also note, if
  // there is no setting for max size, nothing is done and all cores go in the regular "cores" list
  protected void allocateLazyCores(final ConfigSolr cfg, final SolrResourceLoader loader) {
    transientCacheSize = cfg.getInt(ConfigSolr.ConfLevel.SOLR_CORES, "transientCacheSize", Integer.MAX_VALUE);
    if (transientCacheSize != Integer.MAX_VALUE) {
      CoreContainer.log.info("Allocating transient cache for {} transient cores", transientCacheSize);
      transientCores = new LinkedHashMap<String, SolrCore>(transientCacheSize, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SolrCore> eldest) {
          if (size() > transientCacheSize) {
            synchronized (locker) {
              SolrCore closeMe = eldest.getValue();
              synchronized (locker) {
                pendingDynamicCloses.add(closeMe);
                locker.notifyAll(); // Wakes up closer thread too
              }
            }
            return true;
          }
          return false;
        }
      };
    }
  }

  protected void putDynamicDescriptor(String rawName, CoreDescriptor p) {
    synchronized (locker) {
      dynamicDescriptors.put(rawName, p);
    }
  }

  // We are shutting down. We don't want to risk deadlock, so do this manipulation the expensive way. Note, I've
  // already deadlocked with closing/opening cores while keeping locks here....
  protected void clearMaps(ConfigSolr cfg) {
    List<String> coreNames;
    List<String> transientNames;
    List<SolrCore> pendingClosers;
    synchronized (locker) {
      coreNames = new ArrayList(cores.keySet());
      transientNames = new ArrayList(transientCores.keySet());
      pendingClosers = new ArrayList(pendingDynamicCloses);
    }
    for (String coreName : coreNames) {
      SolrCore core = cores.get(coreName);
      if (core != null) {
        try {
          addPersistOneCore(cfg, core, container.loader);

          core.close();
        } catch (Throwable t) {
          SolrException.log(CoreContainer.log, "Error shutting down core", t);
        }
      }
    }
    cores.clear();

    for (String coreName : transientNames) {
      SolrCore core = transientCores.get(coreName);
      if (core != null) {
        try {
          core.close();
        } catch (Throwable t) {
          SolrException.log(CoreContainer.log, "Error shutting down core", t);
        }
      }
    }
    transientCores.clear();

    // We might have some cores that we were _thinking_ about shutting down, so take care of those too.
    for (SolrCore core : pendingClosers) {
      core.close();
    }

  }

  protected void addCoresToList(ArrayList<SolrCoreState> coreStates) {
    List<SolrCore> addCores;
    synchronized (locker) {
      addCores = new ArrayList<SolrCore>(cores.values());
    }
    for (SolrCore core : addCores) {
      coreStates.add(core.getUpdateHandler().getSolrCoreState());
    }
  }

  //WARNING! This should be the _only_ place you put anything into the list of transient cores!
  protected SolrCore putTransientCore(ConfigSolr cfg, String name, SolrCore core, SolrResourceLoader loader) {
    SolrCore retCore;
    CoreContainer.log.info("Opening transient core {}", name);
    synchronized (locker) {
      retCore = transientCores.put(name, core);
  }
    return retCore;
  }

  protected SolrCore putCore(String name, SolrCore core) {
    synchronized (locker) {
      return cores.put(name, core);
    }
  }

  List<SolrCore> getCores() {
    List<SolrCore> lst = new ArrayList<SolrCore>();

    synchronized (locker) {
      lst.addAll(cores.values());
      return lst;
    }
  }

  Set<String> getCoreNames() {
    Set<String> set = new TreeSet<String>();

    synchronized (locker) {
      set.addAll(cores.keySet());
      set.addAll(transientCores.keySet());
    }
    return set;
  }

  List<String> getCoreNames(SolrCore core) {
    List<String> lst = new ArrayList<String>();

    synchronized (locker) {
      for (Map.Entry<String, SolrCore> entry : cores.entrySet()) {
        if (core == entry.getValue()) {
          lst.add(entry.getKey());
        }
      }
      for (Map.Entry<String, SolrCore> entry : transientCores.entrySet()) {
        if (core == entry.getValue()) {
          lst.add(entry.getKey());
        }
      }
    }
    return lst;
  }

  /**
   * Gets a list of all cores, loaded and unloaded (dynamic)
   *
   * @return all cores names, whether loaded or unloaded.
   */
  public Collection<String> getAllCoreNames() {
    Set<String> set = new TreeSet<String>();
    synchronized (locker) {
      set.addAll(cores.keySet());
      set.addAll(transientCores.keySet());
      set.addAll(dynamicDescriptors.keySet());
    }
    return set;
  }

  SolrCore getCore(String name) {

    synchronized (locker) {
      return cores.get(name);
    }
  }

  protected void swap(String n0, String n1) {

    synchronized (locker) {
      SolrCore c0 = cores.get(n0);
      SolrCore c1 = cores.get(n1);
      if (c0 == null)
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "No such core: " + n0);
      if (c1 == null)
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "No such core: " + n1);
      cores.put(n0, c1);
      cores.put(n1, c0);

      c0.setName(n1);
      c0.getCoreDescriptor().putProperty(CoreDescriptor.CORE_NAME, n1);
      c1.setName(n0);
      c1.getCoreDescriptor().putProperty(CoreDescriptor.CORE_NAME, n0);
    }

  }

  protected SolrCore remove(String name, boolean removeOrig) {

    synchronized (locker) {
      SolrCore core = cores.remove(name);
      if (removeOrig && core != null) {
        coreToOrigName.remove(core);
      }

      return core;
    }
  }

  protected void putCoreToOrigName(SolrCore c, String name) {

    synchronized (locker) {
      coreToOrigName.put(c, name);
    }

  }

  protected void removeCoreToOrigName(SolrCore newCore, SolrCore core) {

    synchronized (locker) {
      String origName = coreToOrigName.remove(core);
      if (origName != null) {
        coreToOrigName.put(newCore, origName);
      }
    }
  }

  protected SolrCore getCoreFromAnyList(String name) {
    SolrCore core;

    synchronized (locker) { // This one's OK, the core.open is just an increment
      core = cores.get(name);
      if (core != null) {
        core.open();    // increment the ref count while still synchronized
        return core;
      }

      if (dynamicDescriptors.size() == 0) {
        return null; // Nobody even tried to define any transient cores, so we're done.
      }
      // Now look for already loaded transient cores.
      core = transientCores.get(name);
      if (core != null) {
        core.open();  // Just increments ref count, so it's ok that we're in a synch block
        return core;
      }
    }

    return null;

  }

  protected CoreDescriptor getDynamicDescriptor(String name) {
    synchronized (locker) {
      return dynamicDescriptors.get(name);
    }
  }

  protected boolean isLoaded(String name) {
    synchronized (locker) {
      if (cores.containsKey(name)) {
        return true;
      }
      if (transientCores.containsKey(name)) {
        return true;
      }
    }
    return false;

  }

  protected CoreDescriptor getUnloadedCoreDescriptor(String cname) {
    synchronized (locker) {
      CoreDescriptor desc = dynamicDescriptors.get(cname);
      if (desc == null) {
        return null;
      }
      return new CoreDescriptor(desc);
    }

  }

  protected String getCoreToOrigName(SolrCore solrCore) {
    synchronized (locker) {
      return coreToOrigName.get(solrCore);
    }
  }

  protected void publishCoresAsDown(ZkController zkController) {
    synchronized (locker) {
      for (SolrCore core : cores.values()) {
        try {
          zkController.publish(core.getCoreDescriptor(), ZkStateReader.DOWN);
        } catch (KeeperException e) {
          CoreContainer.log.error("", e);
        } catch (InterruptedException e) {
          CoreContainer.log.error("", e);
        }
      }
      for (SolrCore core : transientCores.values()) {
        try {
          zkController.publish(core.getCoreDescriptor(), ZkStateReader.DOWN);
        } catch (KeeperException e) {
          CoreContainer.log.error("", e);
        } catch (InterruptedException e) {
          CoreContainer.log.error("", e);
        }
      }
    }
  }

  // Irrepressably ugly bit of the transition in SOLR-4196, but there as at least one test case that follows
  // this path, presumably it's there for a reason.
  // This is really perverse, but all we need the here is to call a couple of static methods that for back-compat
  // purposes
  public void persistCores(ConfigSolr cfg, Properties containerProperties, Map<String, String> rootSolrAttribs,
                           Map<String, String> coresAttribs, File file, File configFile, SolrResourceLoader loader) {
    // This is expensive in the maximal case, but I think necessary. It should keep a reference open to all of the
    // current cores while they are saved. Remember that especially the transient core can come and go.
    //
    // Maybe the right thing to do is keep all the core descriptors NOT in the SolrCore, but keep all of the
    // core descriptors in SolrProperties exclusively.
    // TODO: 5.0 move coreDescriptors out of SolrCore and keep them only once in SolrProperties
    //
    synchronized (locker) {
      if (cfg == null) {
        ConfigSolrXmlBackCompat.initPersistStatic();
        persistCores(cfg, cores, loader);
        persistCores(cfg, transientCores, loader);
        ConfigSolrXmlBackCompat.addPersistAllCoresStatic(containerProperties, rootSolrAttribs, coresAttribs,
            (file == null ? configFile : file));
      } else {
        cfg.initPersist();
        persistCores(cfg, cores, loader);
        persistCores(cfg, transientCores, loader);
        cfg.addPersistAllCores(containerProperties, rootSolrAttribs, coresAttribs, (file == null ? configFile : file));
      }
    }
  }
  // We get here when we're being loaded, and the presumption is that we're not in the list yet.
  protected SolrCore waitPendingCoreOps(String name) {

    // Keep multiple threads from opening or closing a core at one time.
    SolrCore ret = null;

    synchronized (locker) {
      boolean pending;
      do { // We're either loading or unloading this core,
        pending = pendingDynamicLoads.contains(name); // wait for the core to be loaded
        if (! pending) {
          // Check pending closes. This is a linear search is inefficient, but maps don't work without a lot of complexity,
          // we'll live with it unless it proves to be a bottleneck. In the "usual" case, this list shouldn't be
          // very long. In the stress test associated with SOLR-4196, this hovered around 0-3, occasionally spiking
          // very briefly to around 30.
          for (SolrCore core : pendingDynamicCloses) {
            if (core.getName().equals(name)) {
              pending = true;
              break;
            }
          }
        }

        if (container.isShutDown()) return null; // Just stop already.

        if (pending) {
          try {
            locker.wait();
          } catch (InterruptedException e) {
            return null; // Seems best not to do anything at all if the thread is interrupted
          }
        }
      } while (pending);

      if (!container.isShutDown()) {
        ret = getCoreFromAnyList(name); // we might have been _unloading_ the core, so check.
        if (ret == null) {
          pendingDynamicLoads.add(name); // the caller is going to load us. If we happen to be shutting down, we don't care.
        }
      }
    }

    return ret;
  }

  // The core is loaded, remove it from the pendin gloads
  protected void releasePending(String name) {
    synchronized (locker) {
      pendingDynamicLoads.remove(name);
      locker.notifyAll();
    }
  }

  protected void persistCores(ConfigSolr cfg, Map<String, SolrCore> whichCores, SolrResourceLoader loader) {
    for (SolrCore solrCore : whichCores.values()) {
      addPersistOneCore(cfg, solrCore, loader);
    }
  }

  private void addIfNotNull(Map<String, String> coreAttribs, String key, String value) {
    if (value == null) return;
    coreAttribs.put(key, value);
  }

  protected void addPersistOneCore(ConfigSolr cfg, SolrCore solrCore, SolrResourceLoader loader) {

    CoreDescriptor dcore = solrCore.getCoreDescriptor();

    String coreName = dcore.getProperty(CoreDescriptor.CORE_NAME);

    String origCoreName = null;

    Map<String, String> coreAttribs = new HashMap<String, String>();
    Properties persistProps = new Properties();
    CloudDescriptor cd = dcore.getCloudDescriptor();
    String collection = null;
    if (cd  != null) collection = cd.getCollectionName();
    String instDir = dcore.getRawInstanceDir();

    if (cfg == null) {
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_NAME, coreName);
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_CONFIG, dcore.getDefaultConfigName());
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_SCHEMA, dcore.getDefaultSchemaName());
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_DATADIR, dcore.getProperty(CoreDescriptor.CORE_DATADIR));
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_ULOGDIR, dcore.getProperty(CoreDescriptor.CORE_ULOGDIR));
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_TRANSIENT, dcore.getProperty(CoreDescriptor.CORE_TRANSIENT));
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_LOADONSTARTUP, dcore.getProperty(CoreDescriptor.CORE_LOADONSTARTUP));
      // we don't try and preserve sys prop defs in these

      addIfNotNull(coreAttribs, CoreDescriptor.CORE_PROPERTIES, dcore.getPropertiesName());
      // Add in any non-standard bits of data
      Set<String> std = new TreeSet<String>();

      Properties allProps = dcore.getCoreProperties();

      std.addAll(Arrays.asList(CoreDescriptor.standardPropNames));

      for (String prop : allProps.stringPropertyNames()) {
        if (! std.contains(prop)) {
          persistProps.put(prop, dcore.getProperty(prop));
        }
      }
      if (StringUtils.isNotBlank(collection) && !collection.equals(coreName)) {
        coreAttribs.put(CoreDescriptor.CORE_COLLECTION, collection);
      }

    } else {

      origCoreName = getCoreToOrigName(solrCore);

      if (origCoreName == null) {
        origCoreName = coreName;
      }
      String tmp = cfg.getCoreNameFromOrig(origCoreName, loader, coreName);
      if (tmp != null) coreName = tmp;

      coreAttribs = cfg.readCoreAttributes(origCoreName);
      persistProps = cfg.readCoreProperties(origCoreName);
      if (coreAttribs != null) {
        coreAttribs.put(CoreDescriptor.CORE_NAME, coreName);
        if (coreAttribs.containsKey(CoreDescriptor.CORE_COLLECTION)) collection = coreAttribs.get(CoreDescriptor.CORE_COLLECTION);
        if (coreAttribs.containsKey(CoreDescriptor.CORE_INSTDIR)) instDir = coreAttribs.get(CoreDescriptor.CORE_INSTDIR);
      }
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_INSTDIR, dcore.getRawInstanceDir());
      coreAttribs.put(CoreDescriptor.CORE_COLLECTION, StringUtils.isNotBlank(collection) ? collection : dcore.getName());

    }

    // Default value here is same as old code.
    addIfNotNull(coreAttribs, CoreDescriptor.CORE_INSTDIR, instDir);

    // Emulating the old code, just overwrite shard and roles if present in the cloud descriptor
    if (cd != null) {
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_SHARD, cd.getShardId());
      addIfNotNull(coreAttribs, CoreDescriptor.CORE_ROLES, cd.getRoles());
    }
    coreAttribs.put(CoreDescriptor.CORE_LOADONSTARTUP, Boolean.toString(dcore.isLoadOnStartup()));
    coreAttribs.put(CoreDescriptor.CORE_TRANSIENT, Boolean.toString(dcore.isTransient()));

    // Now add back in any implicit properties that aren't in already. These are all "attribs" in this meaning
    Properties implicit = dcore.initImplicitProperties();

    if (! coreName.equals(container.getDefaultCoreName())) {
      for (String prop : implicit.stringPropertyNames()) {
        if (coreAttribs.get(prop) == null) {
          coreAttribs.put(prop, implicit.getProperty(prop));
        }
      }
    }
    if (cfg != null) {
      cfg.addPersistCore(coreName, persistProps, coreAttribs);
    } else {
      // Another awkward bit for back-compat for SOLR-4196
      ConfigSolrXmlBackCompat.addPersistCore(persistProps, coreAttribs);
    }
  }

  protected Object getLocker() { return locker; }

  // Be a little careful. We don't want to either open or close a core unless it's _not_ being opened or closed by
  // another thread. So within this lock we'll walk along the list of pending closes until we find something NOT in
  // the list of threads currently being opened. The "usual" case will probably return the very first one anyway..
  protected SolrCore getCoreToClose() {
    synchronized (locker) {
      if (pendingDynamicCloses.size() == 0) return null; // nothing to do.
      // Yes, a linear search but this is a pretty short list in the normal case and usually we'll take the first one.
      for (SolrCore core : pendingDynamicCloses) {
        if (! pendingDynamicLoads.contains(core.getName())) {  // Don't try close a core if it's being opened.
          return core;
        }
      }
    }
    return null;
  }

  protected void removeClosedFromCloser(SolrCore core) {
    synchronized (locker) {
      pendingDynamicCloses.remove(core);
      locker.notifyAll();
    }
  }
}

class CloserThread extends Thread {
  CoreContainer container;
  CoreMaps coreMaps;
  ConfigSolr cfg;


  CloserThread(CoreContainer container, CoreMaps coreMaps, ConfigSolr cfg) {
    this.container = container;
    this.coreMaps = coreMaps;
    this.cfg = cfg;
  }

  // It's important that this be the _only_ thread removing things from pendingDynamicCloses!
  // This is single-threaded, but I tried a multi-threaded approach and didn't see any performance gains, so
  // there's no good justification for the complexity. I suspect that the locking on things like DefaultSolrCoreState
  // essentially create a single-threaded process anyway.
  @Override
  public void run() {
    while (! container.isShutDown()) {
      synchronized (coreMaps.getLocker()) { // need this so we can wait and be awoken.
        try {
          coreMaps.getLocker().wait();
        } catch (InterruptedException e) {
          // Well, if we've been told to stop, we will. Otherwise, continue on and check to see if there are
          // any cores to close.
        }
      }
      for (SolrCore removeMe = coreMaps.getCoreToClose();
           removeMe != null && !container.isShutDown();
           removeMe = coreMaps.getCoreToClose()) {
        try {
          coreMaps.addPersistOneCore(cfg, removeMe, container.loader);
          removeMe.close();
        } finally {
          coreMaps.removeClosedFromCloser(removeMe);
        }
      }
    }
   }
  
  private static final String DEF_SOLR_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
      + "<solr persistent=\"false\">\n"
      + "  <cores adminPath=\"/admin/cores\" defaultCoreName=\""
      + DEFAULT_DEFAULT_CORE_NAME
      + "\""
      + " host=\"${host:}\" hostPort=\"${hostPort:}\" hostContext=\"${hostContext:}\" zkClientTimeout=\"${zkClientTimeout:15000}\""
      + ">\n"
      + "    <core name=\""
      + DEFAULT_DEFAULT_CORE_NAME
      + "\" shard=\"${shard:}\" collection=\"${collection:}\" instanceDir=\"collection1\" />\n"
      + "  </cores>\n" + "</solr>";
 }
diff --git a/solr/core/src/java/org/apache/solr/core/CoreDescriptor.java b/solr/core/src/java/org/apache/solr/core/CoreDescriptor.java
index b5b5a6a6467..7cec3075628 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreDescriptor.java
++ b/solr/core/src/java/org/apache/solr/core/CoreDescriptor.java
@@ -20,6 +20,7 @@ package org.apache.solr.core;
 import java.util.Properties;
 import java.io.File;
 
import org.apache.commons.lang.StringUtils;
 import org.apache.solr.cloud.CloudDescriptor;
 
 /**
@@ -28,29 +29,93 @@ import org.apache.solr.cloud.CloudDescriptor;
  * @since solr 1.3
  */
 public class CoreDescriptor {
  protected String name;
  protected String instanceDir;
  protected String dataDir;
  protected String ulogDir;
  protected String configName;
  protected String propertiesName;
  protected String schemaName;
  private final CoreContainer coreContainer;
  private Properties coreProperties;
  private boolean loadOnStartup = true;
  private boolean isTransient = false;
 
  // Properties file name constants
  public static final String CORE_NAME = "name";
  public static final String CORE_CONFIG = "config";
  public static final String CORE_INSTDIR = "instanceDir";
  public static final String CORE_DATADIR = "dataDir";
  public static final String CORE_ULOGDIR = "ulogDir";
  public static final String CORE_SCHEMA = "schema";
  public static final String CORE_SHARD = "shard";
  public static final String CORE_COLLECTION = "collection";
  public static final String CORE_ROLES = "roles";
  public static final String CORE_PROPERTIES = "properties";
  public static final String CORE_LOADONSTARTUP = "loadOnStartup";
  public static final String CORE_TRANSIENT = "transient";
  public static final String CORE_NODE_NAME = "coreNodeName";

  static final String[] standardPropNames = {
      CORE_NAME,
      CORE_CONFIG,
      CORE_INSTDIR,
      CORE_DATADIR,
      CORE_ULOGDIR,
      CORE_SCHEMA,
      CORE_SHARD,
      CORE_COLLECTION,
      CORE_ROLES,
      CORE_PROPERTIES,
      CORE_LOADONSTARTUP,
      CORE_TRANSIENT
  };

  // As part of moving away from solr.xml (see SOLR-4196), it's _much_ easier to keep these as properties than set
  // them individually.
  private Properties coreProperties = new Properties();

  private final CoreContainer coreContainer;
 
   private CloudDescriptor cloudDesc;
 
  public CoreDescriptor(CoreContainer coreContainer, String name, String instanceDir) {
    this.coreContainer = coreContainer;
    this.name = name;
    
  private CoreDescriptor(CoreContainer cont) {
    // Just a place to put initialization since it's a pain to add to the descriptor in every c'tor.
    this.coreContainer = cont;
    coreProperties.put(CORE_LOADONSTARTUP, "true");
    coreProperties.put(CORE_TRANSIENT, "false");

  }
  public CoreDescriptor(CoreContainer container, String name, String instanceDir) {
    this(container);
    doInit(name, instanceDir);
  }


  public CoreDescriptor(CoreDescriptor descr) {
    this(descr.coreContainer);
    coreProperties.put(CORE_INSTDIR, descr.getInstanceDir());
    coreProperties.put(CORE_CONFIG, descr.getConfigName());
    coreProperties.put(CORE_SCHEMA, descr.getSchemaName());
    coreProperties.put(CORE_NAME, descr.getName());
    coreProperties.put(CORE_DATADIR, descr.getDataDir());
  }

  /**
   * CoreDescriptor - create a core descriptor given default properties from a core.properties file. This will be
   * used in the "solr.xml-less (See SOLR-4196) world where there are no &lt;core&gt; &lt;/core&gt; tags at all, thus  much
   * of the initialization that used to be done when reading solr.xml needs to be done here instead, particularly
   * setting any defaults (e.g. schema.xml, directories, whatever).
   *
   * @param container - the CoreContainer that holds all the information about our cores, loaded, lazy etc.
   * @param propsIn - A properties structure "core.properties" found while walking the file tree to discover cores.
   *                  Any properties set in this param will overwrite the any defaults.
   */
  public CoreDescriptor(CoreContainer container, Properties propsIn) {
    this(container);

    // Set some default, normalize a directory or two
    doInit(propsIn.getProperty(CORE_NAME), propsIn.getProperty(CORE_INSTDIR));

    coreProperties.putAll(propsIn);
  }

  private void doInit(String name, String instanceDir) {
     if (name == null) {
       throw new RuntimeException("Core needs a name");
     }
    

    coreProperties.put(CORE_NAME, name);

     if(coreContainer != null && coreContainer.getZkController() != null) {
       this.cloudDesc = new CloudDescriptor();
       // cloud collection defaults to core name
@@ -61,27 +126,18 @@ public class CoreDescriptor {
       throw new NullPointerException("Missing required \'instanceDir\'");
     }
     instanceDir = SolrResourceLoader.normalizeDir(instanceDir);
    this.instanceDir = instanceDir;
    this.configName = getDefaultConfigName();
    this.schemaName = getDefaultSchemaName();
  }

  public CoreDescriptor(CoreDescriptor descr) {
    this.instanceDir = descr.instanceDir;
    this.configName = descr.configName;
    this.schemaName = descr.schemaName;
    this.name = descr.name;
    this.dataDir = descr.dataDir;
    coreContainer = descr.coreContainer;
    coreProperties.put(CORE_INSTDIR, instanceDir);
    coreProperties.put(CORE_CONFIG, getDefaultConfigName());
    coreProperties.put(CORE_SCHEMA, getDefaultSchemaName());
   }
 
  private Properties initImplicitProperties() {
  public Properties initImplicitProperties() {
     Properties implicitProperties = new Properties(coreContainer.getContainerProperties());
    implicitProperties.setProperty("solr.core.name", name);
    implicitProperties.setProperty("solr.core.instanceDir", instanceDir);
    implicitProperties.setProperty("solr.core.dataDir", getDataDir());
    implicitProperties.setProperty("solr.core.configName", configName);
    implicitProperties.setProperty("solr.core.schemaName", schemaName);
    implicitProperties.setProperty(CORE_NAME, getName());
    implicitProperties.setProperty(CORE_INSTDIR, getInstanceDir());
    implicitProperties.setProperty(CORE_DATADIR, getDataDir());
    implicitProperties.setProperty(CORE_CONFIG, getConfigName());
    implicitProperties.setProperty(CORE_SCHEMA, getSchemaName());
     return implicitProperties;
   }
 
@@ -101,41 +157,47 @@ public class CoreDescriptor {
   }
 
   public String getPropertiesName() {
    return propertiesName;
    return coreProperties.getProperty(CORE_PROPERTIES);
   }
 
   public void setPropertiesName(String propertiesName) {
    this.propertiesName = propertiesName;
    coreProperties.put(CORE_PROPERTIES, propertiesName);
   }
 
   public String getDataDir() {
    String dataDir = this.dataDir;
    if (dataDir == null) dataDir = getDefaultDataDir();
    String dataDir = coreProperties.getProperty(CORE_DATADIR);
    if (dataDir == null) {
      dataDir = getDefaultDataDir();
    }
     if (new File(dataDir).isAbsolute()) {
       return dataDir;
     } else {
      if (new File(instanceDir).isAbsolute()) {
        return SolrResourceLoader.normalizeDir(SolrResourceLoader.normalizeDir(instanceDir) + dataDir);
      if (new File(getInstanceDir()).isAbsolute()) {
        return SolrResourceLoader.normalizeDir(SolrResourceLoader.normalizeDir(getInstanceDir()) + dataDir);
       } else  {
         return SolrResourceLoader.normalizeDir(coreContainer.getSolrHome() +
                SolrResourceLoader.normalizeDir(instanceDir) + dataDir);
                SolrResourceLoader.normalizeDir(getRawInstanceDir()) + dataDir);
       }
     }
   }
 
   public void setDataDir(String s) {
    dataDir = s;
     // normalize zero length to null.
    if (dataDir != null && dataDir.length()==0) dataDir=null;
    if (StringUtils.isBlank(s)) {
      coreProperties.remove(s);
    } else {
      coreProperties.put(CORE_DATADIR, s);
    }
   }
   
   public boolean usingDefaultDataDir() {
    return this.dataDir == null;
    // DO NOT use the getDataDir method here since it'll assign something regardless.
    return coreProperties.getProperty(CORE_DATADIR) == null;
   }
 
   /**@return the core instance directory. */
   public String getRawInstanceDir() {
    return this.instanceDir;
    return coreProperties.getProperty(CORE_INSTDIR);
   }
 
   /**
@@ -143,42 +205,44 @@ public class CoreDescriptor {
    * @return the core instance directory, prepended with solr_home if not an absolute path.
    */
   public String getInstanceDir() {
    String instDir = this.instanceDir;
    String instDir = coreProperties.getProperty(CORE_INSTDIR);
     if (instDir == null) return null; // No worse than before.
 
     if (new File(instDir).isAbsolute()) {
      return SolrResourceLoader.normalizeDir(SolrResourceLoader.normalizeDir(instanceDir));
      return SolrResourceLoader.normalizeDir(
          SolrResourceLoader.normalizeDir(instDir));
     }
     return SolrResourceLoader.normalizeDir(coreContainer.getSolrHome() +
         SolrResourceLoader.normalizeDir(instDir));
   }

   /**Sets the core configuration resource name. */
   public void setConfigName(String name) {
     if (name == null || name.length() == 0)
       throw new IllegalArgumentException("name can not be null or empty");
    this.configName = name;
    coreProperties.put(CORE_CONFIG, name);
   }
 
   /**@return the core configuration resource name. */
   public String getConfigName() {
    return this.configName;
    return coreProperties.getProperty(CORE_CONFIG);
   }
 
   /**Sets the core schema resource name. */
   public void setSchemaName(String name) {
     if (name == null || name.length() == 0)
       throw new IllegalArgumentException("name can not be null or empty");
    this.schemaName = name;
    coreProperties.put(CORE_SCHEMA, name);
   }
 
   /**@return the core schema resource name. */
   public String getSchemaName() {
    return this.schemaName;
    return coreProperties.getProperty(CORE_SCHEMA);
   }
 
   /**@return the initial core name */
   public String getName() {
    return this.name;
    return coreProperties.getProperty(CORE_NAME);
   }
 
   public CoreContainer getCoreContainer() {
@@ -192,15 +256,19 @@ public class CoreDescriptor {
   /**
    * Set this core's properties. Please note that some implicit values will be added to the
    * Properties instance passed into this method. This means that the Properties instance
   * set to this method will have different (less) key/value pairs than the Properties
   * sent to this method will have different (less) key/value pairs than the Properties
    * instance returned by #getCoreProperties method.
   *
   * Under any circumstance, the properties passed in will override any already present.Merge
    */
   public void setCoreProperties(Properties coreProperties) {
     if (this.coreProperties == null) {
       Properties p = initImplicitProperties();
       this.coreProperties = new Properties(p);
      if(coreProperties != null)
        this.coreProperties.putAll(coreProperties);
    }
    // The caller presumably wants whatever properties passed in to override the current core props, so just add them.
    if(coreProperties != null) {
      this.coreProperties.putAll(coreProperties);
     }
   }
 
@@ -212,26 +280,59 @@ public class CoreDescriptor {
     this.cloudDesc = cloudDesc;
   }
   public boolean isLoadOnStartup() {
    return loadOnStartup;
    String tmp = coreProperties.getProperty(CORE_LOADONSTARTUP, "false");
    return Boolean.parseBoolean(tmp);
   }
 
   public void setLoadOnStartup(boolean loadOnStartup) {
    this.loadOnStartup = loadOnStartup;
    coreProperties.put(CORE_LOADONSTARTUP, Boolean.toString(loadOnStartup));
   }
 
   public boolean isTransient() {
    return isTransient;
    String tmp = coreProperties.getProperty(CORE_TRANSIENT, "false");
    return (Boolean.parseBoolean(tmp));
   }
 
  public void setTransient(boolean aTransient) {
    this.isTransient = aTransient;
  public void setTransient(boolean isTransient) {
    coreProperties.put(CORE_TRANSIENT, Boolean.toString(isTransient));
   }
 
   public String getUlogDir() {
    return ulogDir;
    return coreProperties.getProperty(CORE_ULOGDIR);
   }
 
   public void setUlogDir(String ulogDir) {
    this.ulogDir = ulogDir;
    coreProperties.put(CORE_ULOGDIR, ulogDir);
  }

  /**
   * Reads a property defined in the core.properties file that's replacing solr.xml (if present).
   * @param prop    - value to read from the properties structure.
   * @param defVal  - return if no property found.
   * @return associated string. May be null.
   */
  public String getProperty(String prop, String defVal) {
    return coreProperties.getProperty(prop, defVal);
  }

  /**
   * gReads a property defined in the core.properties file that's replacing solr.xml (if present).
   * @param prop  value to read from the properties structure.
   * @return associated string. May be null.
   */
  public String getProperty(String prop) {
    return coreProperties.getProperty(prop);
  }
  /**
   * This will eventually replace _all_ of the setters. Puts a value in the "new" (obsoleting solr.xml JIRAs) properties
   * structures.
   *
   * Will replace any currently-existing property with the key "prop".
   *
   * @param prop - property name
   * @param val  - property value
   */
  public void putProperty(String prop, String val) {
    coreProperties.put(prop, val);
   }
 }
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCore.java b/solr/core/src/java/org/apache/solr/core/SolrCore.java
index 98f71b14531..4fed422e7a3 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCore.java
@@ -657,12 +657,15 @@ public final class SolrCore implements SolrInfoMBean {
     this.setName( name );
     resourceLoader = config.getResourceLoader();
     if (dataDir == null){
      if(cd.usingDefaultDataDir()) dataDir = config.getDataDir();
      if(dataDir == null) dataDir = cd.getDataDir();
      if(cd.usingDefaultDataDir()) {
        dataDir = config.getDataDir();
      }
      if(dataDir == null) {
        dataDir = cd.getDataDir();
      }
     }
 
     dataDir = SolrResourceLoader.normalizeDir(dataDir);

     log.info(logid+"Opening new SolrCore at " + resourceLoader.getInstanceDir() + ", dataDir="+dataDir);
 
     if (schema==null) {
@@ -1316,7 +1319,7 @@ public final class SolrCore implements SolrInfoMBean {
    *
    * This method acquires openSearcherLock - do not call with searckLock held!
    */
  public RefCounted<SolrIndexSearcher> openNewSearcher(boolean updateHandlerReopens, boolean realtime) {
  public RefCounted<SolrIndexSearcher>  openNewSearcher(boolean updateHandlerReopens, boolean realtime) {
     SolrIndexSearcher tmp;
     RefCounted<SolrIndexSearcher> newestSearcher = null;
     boolean nrt = solrConfig.reopenReaders && updateHandlerReopens;
diff --git a/solr/core/src/java/org/apache/solr/core/SolrProperties.java b/solr/core/src/java/org/apache/solr/core/SolrProperties.java
new file mode 100644
index 00000000000..240fd1362ae
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/SolrProperties.java
@@ -0,0 +1,575 @@
package org.apache.solr.core;

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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.HttpShardHandlerFactory;
import org.apache.solr.handler.component.ShardHandlerFactory;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.util.PropertiesUtil;
import org.apache.solr.util.SystemIdResolver;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This is the new way of dealing with solr properties replacing solr.xml. This is simply a high-level set of
 * properties. Cores are no longer defined in the solr.xml file, they are discovered by enumerating all of the
 * directories under the base path and creating cores as necessary.
 *
 * @since Solr 4.2
 */
public class SolrProperties implements ConfigSolr {
  public final static String SOLR_PROPERTIES_FILE = "solr.properties";
  public final static String SOLR_XML_FILE = "solr.xml";
  final static String CORE_PROP_FILE = "core.properties";

  private final static String SHARD_HANDLER_FACTORY = "shardHandlerFactory";
  private final static String SHARD_HANDLER_NAME = SHARD_HANDLER_FACTORY + ".name";
  private final static String SHARD_HANDLER_CLASS = SHARD_HANDLER_FACTORY + ".class";

  public static final Logger log = LoggerFactory.getLogger(SolrProperties.class);

  protected final CoreContainer container;
  protected Properties solrProperties = new Properties();
  protected final Properties origsolrprops = new Properties();
  protected String name;
  protected SolrResourceLoader loader;

  private final Map<String, CoreDescriptorPlus> coreDescriptorPlusMap = new HashMap<String, CoreDescriptorPlus>();

  private static Map<ConfLevel, String> prefixesprefixes;

  static {
    prefixesprefixes = new HashMap<ConfLevel, String>();

    prefixesprefixes.put(ConfLevel.SOLR_CORES, "cores.");
    prefixesprefixes.put(ConfLevel.SOLR_LOGGING, "logging.");
    prefixesprefixes.put(ConfLevel.SOLR_LOGGING_WATCHER, "logging.watcher.");

  }


  /**
   * Create a SolrProperties object given just the resource loader
   *
   * @param container - the container for this Solr instance. There should be one and only one...
   * @param loader    - Solr resource loader
   * @param solrCfg   - a config file whose values will be transferred to the properties object that can be changed
   * @throws IOException - It's possible to walk a very deep tree, if that process goes awry, or if reading any
   *                     of the files found doesn't work, you'll get an IO exception
   */
  SolrProperties(CoreContainer container, SolrResourceLoader loader, SolrProperties solrCfg) throws IOException {
    origsolrprops.putAll(solrCfg.getOriginalProperties());
    this.loader = loader;
    this.container = container;
    init(solrCfg.name);
  }

  /**
   * Create a SolrProperties object from an opened input stream, useful for creating defaults
   *
   * @param container - the container for this Solr instance. There should be one and only one...
   * @param is        - Input stream for loading properties.
   * @param fileName  - the name for this properties object.
   * @throws IOException - It's possible to walk a very deep tree, if that process goes awry, or if reading any
   *                     of the files found doesn't work, you'll get an IO exception
   */
  public SolrProperties(CoreContainer container, InputStream is, String fileName) throws IOException {
    origsolrprops.load(is);
    this.container = container;
    init(fileName);
  }

  //Just localize the common constructor operations
  private void init(String name) throws IOException {
    this.name = name;
    for (String s : origsolrprops.stringPropertyNames()) {
      solrProperties.put(s, System.getProperty(s, origsolrprops.getProperty(s)));
    }
    synchronized (coreDescriptorPlusMap) {
      walkFromHere(new File(container.getSolrHome()), container);
    }
  }

  // Just localizes default substitution and the ability to log an error if the value isn't present.
  private String getVal(String path, boolean errIfMissing, String defVal) {
    String val = solrProperties.getProperty(path, defVal);

    if (StringUtils.isNotBlank(val)) {
      log.debug(name + ' ' + path + val);
      return val;
    }

    if (!errIfMissing) {
      log.debug(name + "missing optional " + path);
      return null;
    }

    throw new RuntimeException(name + " missing " + path);
  }

  /**
   * Get a property and convert it to a boolean value. Does not log a message if the value is absent
   *
   * @param prop     - name of the property to fetch
   * @param defValue - value to return if the property is absent
   * @return property value or default if property is not present.
   */
  public boolean getBool(String prop, boolean defValue) {
    String def = defValue ? "true" : "false";
    String val = getVal(prop, false, def);
    return (StringUtils.equalsIgnoreCase(val, "true"));
  }

  /**
   * Fetch a string value, for the given property. Does not log a message if the valued is absent.
   *
   * @param prop - the property name to fetch
   * @param def  - the default value to return if not present
   * @return - the fetched property or the default value if the property is absent
   */
  public String get(String prop, String def) {
    String val = getVal(prop, false, def);
    if (val == null || val.length() == 0) {
      return def;
    }
    return val;
  }

  /**
   * Fetch the string value of the property. May log a message and returns null if absent
   *
   * @param prop         - the name of the property to fetch
   * @param errIfMissing - if true, log a message that the property is not present
   * @return - the property value or null if absent
   */
  public String getVal(String prop, boolean errIfMissing) {
    return getVal(prop, errIfMissing, null);
  }

  /**
   * Returns a property as an integer
   *
   * @param prop   - the name of the property to fetch
   * @param defVal - the value to return if the property is missing
   * @return - the fetch property as an int or the def value if absent
   */
  public int getInt(String prop, int defVal) {
    String val = getVal(prop, false, Integer.toString(defVal));
    return Integer.parseInt(val);
  }

  @Override
  public int getInt(ConfLevel level, String tag, int def) {
    return getInt(prefixesprefixes.get(level) + tag, def);
  }

  @Override
  public boolean getBool(ConfLevel level, String tag, boolean defValue) {
    return getBool(prefixesprefixes.get(level) + tag, defValue);
  }

  @Override
  public String get(ConfLevel level, String tag, String def) {
    return get(prefixesprefixes.get(level) + tag, def);
  }

  /**
   * For all values in the properties structure, find if any system properties are defined and substitute them.
   */
  public void substituteProperties() {
    for (String prop : solrProperties.stringPropertyNames()) {
      String subProp = PropertiesUtil.substituteProperty(solrProperties.getProperty(prop), solrProperties);
      if (subProp != null && !subProp.equals(solrProperties.getProperty(prop))) {
        solrProperties.put(prop, subProp);
      }
    }
  }

  /**
   * Fetches the properties as originally read from the properties file without any system variable substitution
   *
   * @return - a copy of the original properties.
   */
  public Properties getOriginalProperties() {
    Properties ret = new Properties();
    ret.putAll(origsolrprops);
    return ret;
  }

  @Override
  public ShardHandlerFactory initShardHandler(/*boolean isTest*/) {

    PluginInfo info = null;
    Map<String, String> attrs = new HashMap<String, String>();
    NamedList args = new NamedList();
    boolean haveHandler = false;
    for (String s : solrProperties.stringPropertyNames()) {
      String val = solrProperties.getProperty(s);
      if (s.indexOf(SHARD_HANDLER_FACTORY) != -1) {
        haveHandler = true;
        if (SHARD_HANDLER_NAME.equals(s) || SHARD_HANDLER_CLASS.equals(s)) {
          attrs.put(s, val);
        } else {
          args.add(s, val);
        }
      }
    }

    if (haveHandler) {
      //  public PluginInfo(String type, Map<String, String> attrs ,NamedList initArgs, List<PluginInfo> children) {

      info = new PluginInfo(SHARD_HANDLER_FACTORY, attrs, args, null);
    } else {
      Map m = new HashMap();
      m.put("class", HttpShardHandlerFactory.class.getName());
      info = new PluginInfo("shardHandlerFactory", m, null, Collections.<PluginInfo>emptyList());
    }
    HttpShardHandlerFactory fac = new HttpShardHandlerFactory();
    if (info != null) {
      fac.init(info);
    }
    return fac;
  }

  // Strictly for compatibility with i'face. TODO: remove for 5.0
  @Override
  public Properties getSolrProperties(ConfigSolr cfg, String context) {
    return getSolrProperties();
  }

  /**
   * Return the original properties that were defined, without substitutions from solr.properties
   *
   * @return - the Properties as originally defined.
   */
  public Properties getSolrProperties() {
    return solrProperties;
  }

  /**
   * given a core and attributes, find the core.properties file from whence it came and update it with the current
   * <p/>
   * Note, when the cores were discovered, we stored away the path that it came from for reference later. Remember
   * that these cores aren't necessarily loaded all the time, they may be transient.
   * It's not clear what the magic is that the calling methods (see CoreContainer) are doing, but they seem to be
   * "doing the right thing" so that the attribs properties are the ones that contain the correct data. All the
   * tests pass, but it's magic at this point.
   *
   * @param coreName - the core whose attributes we are to change
   * @param attribs  - the attribs to change to, see note above.
   * @param props    - ignored, here to make the i'face work in combination with ConfigSolrXmlBackCompat
   */

  @Override
  public void addPersistCore(String coreName, Properties attribs, Map<String, String> props) {
    String val = container.getContainerProperties().getProperty("solr.persistent", "false");
    if (!Boolean.parseBoolean(val)) return;

    CoreDescriptorPlus plus;
    plus = coreDescriptorPlusMap.get(coreName);
    if (plus == null) {
      log.error("Expected to find core for persisting, but we did not. Core: " + coreName);
      return;
    }

    Properties outProps = new Properties();
    // I don't quite get this, but somehow the attribs passed in are the originals (plus any newly-added ones). Never
    // one to look a gift horse in the mouth I'll just use that.

    // Take care NOT to write out properties like ${blah blah blah}
    outProps.putAll(attribs);
    Properties corePropsOrig = plus.getPropsOrig();
    for (String prop : corePropsOrig.stringPropertyNames()) {
      val = corePropsOrig.getProperty(prop);
      if (val.indexOf("$") != -1) { // it was originally a system property, keep it so
        outProps.put(prop, val);
        continue;
      }
      // Make sure anything that used to be in the properties file still is.
      if (outProps.getProperty(prop) == null) {
        outProps.put(prop, val);
      }
    }
    // Any of our standard properties that weren't in the original properties file should NOT be persisted, I think
    for (String prop : CoreDescriptor.standardPropNames) {
      if (corePropsOrig.getProperty(prop) == null) {
        outProps.remove(prop);
      }
    }

    try {
      outProps.store(new FileOutputStream(plus.getFilePath()), null);
    } catch (IOException e) {
      log.error("Failed to persist core {}, filepath {}", coreName, plus.getFilePath());
    }

  }

  /**
   * PersistSolrProperties persists the Solr.properties file only,
   * <p/>
   * The old version (i.e. using solr.xml) persisted _everything_ in a single file. This version will just
   * persist the solr.properties file for an individual core.
   * The individual cores were persisted in addPersistCore calls above.
   */
  // It seems like a lot of this could be done by using the Properties defaults

  /**
   * PersistSolrProperties persists the Solr.properties file only,
   * <p/>
   * The old version (i.e. using solr.xml) persisted _everything_ in a single file. This version will just
   * persist the solr.properties file for an individual core.
   * The individual cores were persisted in addPersistCore calls above.
   * <p/>
   * TODO: Remove all parameters for 5.0 when we obsolete ConfigSolrXmlBackCompat
   *
   * @param containerProperties - ignored, here for back compat.
   * @param rootSolrAttribs     - ignored, here for back compat.
   * @param coresAttribs        - ignored, here for back compat.
   * @param file                - ignored, here for back compat.
   */

  @Override
  public void addPersistAllCores(Properties containerProperties, Map<String, String> rootSolrAttribs,
                                 Map<String, String> coresAttribs, File file) {
    String val = container.getContainerProperties().getProperty("solr.persistent", "false");
    if (!Boolean.parseBoolean(val)) return;

    // First persist solr.properties
    File parent = new File(container.getSolrHome());
    File props = new File(parent, SOLR_PROPERTIES_FILE);
    Properties propsOut = new Properties();
    propsOut.putAll(container.getContainerProperties());
    for (String prop : origsolrprops.stringPropertyNames()) {
      String toTest = origsolrprops.getProperty(prop);
      if (toTest.indexOf("$") != -1) { // Don't store away things that should be system properties
        propsOut.put(prop, toTest);
      }
    }
    try {
      propsOut.store(new FileOutputStream(props), null);
    } catch (IOException e) {
      log.error("Failed to persist file " + props.getAbsolutePath(), e);
    }
  }


  // Copied verbatim from the old code, presumably this will be tested when we eliminate solr.xml
  @Override
  public IndexSchema getSchemaFromZk(ZkController zkController, String zkConfigName, String schemaName,
                                     SolrConfig config)
      throws KeeperException, InterruptedException {
    byte[] configBytes = zkController.getConfigFileData(zkConfigName, schemaName);
    InputSource is = new InputSource(new ByteArrayInputStream(configBytes));
    is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(schemaName));
    IndexSchema schema = new IndexSchema(config, schemaName, is);
    return schema;
  }

  // Copied verbatim from the old code, presumably this will be tested when we eliminate solr.xml
  @Override
  public SolrConfig getSolrConfigFromZk(ZkController zkController, String zkConfigName, String solrConfigFileName,
                                        SolrResourceLoader resourceLoader) {
    SolrConfig cfg = null;
    try {
      byte[] config = zkController.getConfigFileData(zkConfigName, solrConfigFileName);
      InputSource is = new InputSource(new ByteArrayInputStream(config));
      is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(solrConfigFileName));
      cfg = solrConfigFileName == null ? new SolrConfig(
          resourceLoader, SolrConfig.DEFAULT_CONF_FILE, is) : new SolrConfig(
          resourceLoader, solrConfigFileName, is);
    } catch (Exception e) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
          "getSolrConfigFromZK failed for " + zkConfigName + " " + solrConfigFileName, e);
    }
    return cfg;
  }

  @Override
  public void initPersist() {
    //NOOP
  }

  // Basic recursive tree walking, looking for "core.properties" files. Once one is found, we'll stop going any
  // deeper in the tree.
  //
  // @param file - the directory we're to either read the properties file from or recurse into.
  private void walkFromHere(File file, CoreContainer container) throws IOException {
    log.info("Looking for cores in " + file.getAbsolutePath());
    for (File childFile : file.listFiles()) {
      // This is a little tricky, we are asking if core.properties exists in a child directory of the directory passed
      // in. In other words we're looking for core.properties in the grandchild directories of the parameter passed
      // in. That allows us to gracefully top recursing deep but continue looking wide.
      File propFile = new File(childFile, CORE_PROP_FILE);
      if (propFile.exists()) { // Stop looking after processing this file!
        log.info("Discovered properties file {}, adding to cores", propFile.getAbsolutePath());
        Properties propsOrig = new Properties();
        propsOrig.load(new FileInputStream(propFile));

        Properties props = new Properties();
        for (String prop : propsOrig.stringPropertyNames()) {
          props.put(prop, PropertiesUtil.substituteProperty(propsOrig.getProperty(prop), null));
        }

        if (props.getProperty(CoreDescriptor.CORE_INSTDIR) == null) {
          props.setProperty(CoreDescriptor.CORE_INSTDIR, childFile.getPath());
        }

        if (props.getProperty(CoreDescriptor.CORE_NAME) == null) {
          // Should default to this directory
          props.setProperty(CoreDescriptor.CORE_NAME, file.getName());
        }
        CoreDescriptor desc = new CoreDescriptor(container, props);
        CoreDescriptorPlus plus = new CoreDescriptorPlus(propFile.getAbsolutePath(), desc, propsOrig);
        coreDescriptorPlusMap.put(desc.getName(), plus);
        continue; // Go on to the sibling directory
      }
      if (childFile.isDirectory()) {
        walkFromHere(childFile, container);
      }
    }
  }

  static Properties getCoreProperties(String instanceDir, CoreDescriptor dcore) {
    String file = dcore.getPropertiesName();
    if (file == null) file = "conf" + File.separator + "solrcore.properties";
    File corePropsFile = new File(file);
    if (!corePropsFile.isAbsolute()) {
      corePropsFile = new File(instanceDir, file);
    }
    Properties p = dcore.getCoreProperties();
    if (corePropsFile.exists() && corePropsFile.isFile()) {
      p = new Properties(dcore.getCoreProperties());
      InputStream is = null;
      try {
        is = new FileInputStream(corePropsFile);
        p.load(is);
      } catch (IOException e) {
        log.warn("Error loading properties ", e);
      } finally {
        IOUtils.closeQuietly(is);
      }
    }
    return p;
  }

  @Override
  public String getCoreNameFromOrig(String origCoreName, SolrResourceLoader loader, String coreName) {
    // first look for an exact match
    for (Map.Entry<String, CoreDescriptorPlus> ent : coreDescriptorPlusMap.entrySet()) {

      String name = ent.getValue().getCoreDescriptor().getProperty(CoreDescriptor.CORE_NAME, null);
      if (origCoreName.equals(name)) {
        if (coreName.equals(origCoreName)) {
          return name;
        }
        return coreName;
      }
    }

    for (Map.Entry<String, CoreDescriptorPlus> ent : coreDescriptorPlusMap.entrySet()) {
      String name = ent.getValue().getCoreDescriptor().getProperty(CoreDescriptor.CORE_NAME, null);
      // see if we match with substitution
      if (origCoreName.equals(PropertiesUtil.substituteProperty(name, loader.getCoreProperties()))) {
        if (coreName.equals(origCoreName)) {
          return name;
        }
        return coreName;
      }
    }
    return null;
  }

  @Override
  public List<String> getAllCoreNames() {
    List<String> ret;
    ret = new ArrayList<String>(coreDescriptorPlusMap.keySet());
    return ret;
  }

  @Override
  public String getProperty(String coreName, String property, String defaultVal) {
    CoreDescriptorPlus plus = coreDescriptorPlusMap.get(coreName);
    if (plus == null) return defaultVal;
    CoreDescriptor desc = plus.getCoreDescriptor();
    if (desc == null) return defaultVal;
    return desc.getProperty(property, defaultVal);
  }

  @Override
  public Properties readCoreProperties(String coreName) {
    CoreDescriptorPlus plus = coreDescriptorPlusMap.get(coreName);
    if (plus == null) return null;
    return new Properties(plus.getCoreDescriptor().getCoreProperties());
  }

  @Override
  public Map<String, String> readCoreAttributes(String coreName) {
    return new HashMap<String, String>();  // Should be a no-op.
  }
}

// It's mightily convenient to have all of the original path names and property values when persisting cores, so
// this little convenience class is just for that.
// Also, let's keep track of anything we added here, especially the instance dir for persistence purposes. We don't
// want, for instance, to persist instanceDir if it was not specified originally.
//
// I suspect that for persistence purposes, we may want to expand this idea to record, say, ${blah}
class CoreDescriptorPlus {
  private CoreDescriptor coreDescriptor;
  private String filePath;
  private Properties propsOrig;

  CoreDescriptorPlus(String filePath, CoreDescriptor descriptor, Properties propsOrig) {
    coreDescriptor = descriptor;
    this.filePath = filePath;
    this.propsOrig = propsOrig;
  }

  CoreDescriptor getCoreDescriptor() {
    return coreDescriptor;
  }

  String getFilePath() {
    return filePath;
  }

  Properties getPropsOrig() {
    return propsOrig;
  }
}
diff --git a/solr/core/src/java/org/apache/solr/core/SolrResourceLoader.java b/solr/core/src/java/org/apache/solr/core/SolrResourceLoader.java
index e6e348fd1de..27532bce77f 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrResourceLoader.java
++ b/solr/core/src/java/org/apache/solr/core/SolrResourceLoader.java
@@ -83,6 +83,7 @@ public class SolrResourceLoader implements ResourceLoader
   private final List<ResourceLoaderAware> waitingForResources = Collections.synchronizedList(new ArrayList<ResourceLoaderAware>());
   private static final Charset UTF_8 = Charset.forName("UTF-8");
 
  //TODO: Solr5. Remove this completely when you obsolete putting <core> tags in solr.xml (See Solr-4196)
   private final Properties coreProperties;
 
   private volatile boolean live;
@@ -105,7 +106,7 @@ public class SolrResourceLoader implements ResourceLoader
                this.instanceDir);
     } else{
       this.instanceDir = normalizeDir(instanceDir);
      log.info("new SolrResourceLoader for directory: '{}'", 
      log.info("new SolrResourceLoader for directory: '{}'",
                this.instanceDir);
     }
     
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java
index 6769511006c..44628e4e298 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java
@@ -32,6 +32,7 @@ import java.util.Properties;
 import javax.xml.parsers.ParserConfigurationException;
 
 import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.IOUtils;
@@ -661,7 +662,7 @@ public class CoreAdminHandler extends RequestHandlerBase {
     try {
       if (cname == null) {
         rsp.add("defaultCoreName", coreContainer.getDefaultCoreName());
        for (String name : coreContainer.getCoreNames()) {
        for (String name : coreContainer.getAllCoreNames()) {
           status.add(name, getCoreStatus(coreContainer, name, isIndexInfoNeeded));
         }
         rsp.add("initFailures", allFailures);
@@ -954,38 +955,65 @@ public class CoreAdminHandler extends RequestHandlerBase {
     
   }
 
  protected NamedList<Object> getCoreStatus(CoreContainer cores, String cname, boolean isIndexInfoNeeded) throws IOException {
  /**
   * Returns the core status for a particular core.
   * @param cores - the enclosing core container
   * @param cname - the core to return
   * @param isIndexInfoNeeded - add what may be expensive index information. NOT returned if the core is not loaded
   * @return - a named list of key/value pairs from the core.
   * @throws IOException - LukeRequestHandler can throw an I/O exception
   */
  protected NamedList<Object> getCoreStatus(CoreContainer cores, String cname, boolean isIndexInfoNeeded)  throws IOException {
     NamedList<Object> info = new SimpleOrderedMap<Object>();
    SolrCore core = cores.getCore(cname);
    if (core != null) {
      try {
        info.add("name", core.getName());
        info.add("isDefaultCore", core.getName().equals(cores.getDefaultCoreName()));
        info.add("instanceDir", normalizePath(core.getResourceLoader().getInstanceDir()));
        info.add("dataDir", normalizePath(core.getDataDir()));
        info.add("config", core.getConfigResource());
        info.add("schema", core.getSchemaResource());
        info.add("startTime", new Date(core.getStartTime()));
        info.add("uptime", System.currentTimeMillis() - core.getStartTime());
        if (isIndexInfoNeeded) {
          RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
          try {
            SimpleOrderedMap<Object> indexInfo = LukeRequestHandler.getIndexInfo(searcher.get().getIndexReader());
            long size = getIndexSize(core);
            indexInfo.add("sizeInBytes", size);
            indexInfo.add("size", NumberUtils.readableSize(size));
            info.add("index", indexInfo);
          } finally {
            searcher.decref();

    if (!cores.isLoaded(cname)) { // Lazily-loaded core, fill in what we can.
      // It would be a real mistake to load the cores just to get the status
      CoreDescriptor desc = cores.getUnloadedCoreDescriptor(cname);
      if (desc != null) {
        info.add("name", desc.getName());
        info.add("isDefaultCore", desc.getName().equals(cores.getDefaultCoreName()));
        info.add("instanceDir", desc.getInstanceDir());
        // None of the following are guaranteed to be present in a not-yet-loaded core.
        String tmp = desc.getDataDir();
        if (StringUtils.isNotBlank(tmp)) info.add("dataDir", tmp);
        tmp = desc.getConfigName();
        if (StringUtils.isNotBlank(tmp)) info.add("config", tmp);
        tmp = desc.getSchemaName();
        if (StringUtils.isNotBlank(tmp)) info.add("schema", tmp);
        info.add("isLoaded", "false");
      }
    } else {
      SolrCore core = cores.getCore(cname);
      if (core != null) {
        try {
          info.add("name", core.getName());
          info.add("isDefaultCore", core.getName().equals(cores.getDefaultCoreName()));
          info.add("instanceDir", normalizePath(core.getResourceLoader().getInstanceDir()));
          info.add("dataDir", normalizePath(core.getDataDir()));
          info.add("config", core.getConfigResource());
          info.add("schema", core.getSchemaResource());
          info.add("startTime", new Date(core.getStartTime()));
          info.add("uptime", System.currentTimeMillis() - core.getStartTime());
          if (isIndexInfoNeeded) {
            RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
            try {
              SimpleOrderedMap<Object> indexInfo = LukeRequestHandler.getIndexInfo(searcher.get().getIndexReader());
              long size = getIndexSize(core);
              indexInfo.add("sizeInBytes", size);
              indexInfo.add("size", NumberUtils.readableSize(size));
              info.add("index", indexInfo);
            } finally {
              searcher.decref();
            }
           }
        } finally {
          core.close();
         }
      } finally {
        core.close();
       }
     }
     return info;
   }
  

   private long getIndexSize(SolrCore core) {
     Directory dir;
     long size = 0;
diff --git a/solr/core/src/java/org/apache/solr/schema/SchemaField.java b/solr/core/src/java/org/apache/solr/schema/SchemaField.java
index 15cce7f1c20..173d6fa6e23 100644
-- a/solr/core/src/java/org/apache/solr/schema/SchemaField.java
++ b/solr/core/src/java/org/apache/solr/schema/SchemaField.java
@@ -58,7 +58,7 @@ public final class SchemaField extends FieldProperties {
  /** Create a new SchemaField with the given name and type,
    * and with the specified properties.  Properties are *not*
    * inherited from the type in this case, so users of this
   * constructor should derive the properties from type.getProperties()
   * constructor should derive the properties from type.getSolrProperties()
    *  using all the default properties from the type.
    */
   public SchemaField(String name, FieldType type, int properties, String defaultValue ) {
diff --git a/solr/core/src/java/org/apache/solr/util/DOMUtil.java b/solr/core/src/java/org/apache/solr/util/DOMUtil.java
index e304e1b1fb6..464b2884629 100644
-- a/solr/core/src/java/org/apache/solr/util/DOMUtil.java
++ b/solr/core/src/java/org/apache/solr/util/DOMUtil.java
@@ -289,117 +289,18 @@ public class DOMUtil {
 
       // handle child by node type
       if (child.getNodeType() == Node.TEXT_NODE) {
        child.setNodeValue(substituteProperty(child.getNodeValue(), properties));
        child.setNodeValue(PropertiesUtil.substituteProperty(child.getNodeValue(), properties));
       } else if (child.getNodeType() == Node.ELEMENT_NODE) {
         // handle child elements with recursive call
         NamedNodeMap attributes = child.getAttributes();
         for (int i = 0; i < attributes.getLength(); i++) {
           Node attribute = attributes.item(i);
          attribute.setNodeValue(substituteProperty(attribute.getNodeValue(), properties));
          attribute.setNodeValue(PropertiesUtil.substituteProperty(attribute.getNodeValue(), properties));
         }
         substituteProperties(child, properties);
       }
     }
   }
 
  /*
   * This method borrowed from Ant's PropertyHelper.replaceProperties:
   *   http://svn.apache.org/repos/asf/ant/core/trunk/src/main/org/apache/tools/ant/PropertyHelper.java
   */
  public static String substituteProperty(String value, Properties coreProperties) {
    if (value == null || value.indexOf('$') == -1) {
      return value;
    }

    List<String> fragments = new ArrayList<String>();
    List<String> propertyRefs = new ArrayList<String>();
    parsePropertyString(value, fragments, propertyRefs);

    StringBuilder sb = new StringBuilder();
    Iterator<String> i = fragments.iterator();
    Iterator<String> j = propertyRefs.iterator();

    while (i.hasNext()) {
      String fragment = i.next();
      if (fragment == null) {
        String propertyName = j.next();
        String defaultValue = null;
        int colon_index = propertyName.indexOf(':');
        if (colon_index > -1) {
          defaultValue = propertyName.substring(colon_index + 1);
          propertyName = propertyName.substring(0,colon_index);
        }
        if (coreProperties != null) {
          fragment = coreProperties.getProperty(propertyName);
        }
        if (fragment == null) {
          fragment = System.getProperty(propertyName, defaultValue);
        }
        if (fragment == null) {
          throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, "No system property or default value specified for " + propertyName + " value:" + value);
        }
      }
      sb.append(fragment);
    }
    return sb.toString();
  }

  /*
   * This method borrowed from Ant's PropertyHelper.parsePropertyStringDefault:
   *   http://svn.apache.org/repos/asf/ant/core/trunk/src/main/org/apache/tools/ant/PropertyHelper.java
   */
  private static void parsePropertyString(String value, List<String> fragments, List<String> propertyRefs) {
      int prev = 0;
      int pos;
      //search for the next instance of $ from the 'prev' position
      while ((pos = value.indexOf("$", prev)) >= 0) {

          //if there was any text before this, add it as a fragment
          //TODO, this check could be modified to go if pos>prev;
          //seems like this current version could stick empty strings
          //into the list
          if (pos > 0) {
              fragments.add(value.substring(prev, pos));
          }
          //if we are at the end of the string, we tack on a $
          //then move past it
          if (pos == (value.length() - 1)) {
              fragments.add("$");
              prev = pos + 1;
          } else if (value.charAt(pos + 1) != '{') {
              //peek ahead to see if the next char is a property or not
              //not a property: insert the char as a literal
              /*
              fragments.addElement(value.substring(pos + 1, pos + 2));
              prev = pos + 2;
              */
              if (value.charAt(pos + 1) == '$') {
                  //backwards compatibility two $ map to one mode
                  fragments.add("$");
                  prev = pos + 2;
              } else {
                  //new behaviour: $X maps to $X for all values of X!='$'
                  fragments.add(value.substring(pos, pos + 2));
                  prev = pos + 2;
              }

          } else {
              //property found, extract its name or bail on a typo
              int endName = value.indexOf('}', pos);
              if (endName < 0) {
                throw new RuntimeException("Syntax error in property: " + value);
              }
              String propertyName = value.substring(pos + 2, endName);
              fragments.add(null);
              propertyRefs.add(propertyName);
              prev = endName + 1;
          }
      }
      //no more $ signs found
      //if there is any tail to the string, append it
      if (prev < value.length()) {
          fragments.add(value.substring(prev));
      }
  }
 
 }
diff --git a/solr/core/src/java/org/apache/solr/util/PropertiesUtil.java b/solr/core/src/java/org/apache/solr/util/PropertiesUtil.java
new file mode 100644
index 00000000000..2cdb807c0e9
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/util/PropertiesUtil.java
@@ -0,0 +1,132 @@
package org.apache.solr.util;

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

import org.apache.solr.common.SolrException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Breaking out some utility methods into a separate class as part of SOLR-4196. These utils have nothing to do with
 * the DOM (they came from DomUtils) and it's really confusing to see them in something labeled DOM
 */
public class PropertiesUtil {
  /*
  * This method borrowed from Ant's PropertyHelper.replaceProperties:
  *   http://svn.apache.org/repos/asf/ant/core/trunk/src/main/org/apache/tools/ant/PropertyHelper.java
  */
  public static String substituteProperty(String value, Properties coreProperties) {
    if (value == null || value.indexOf('$') == -1) {
      return value;
    }

    List<String> fragments = new ArrayList<String>();
    List<String> propertyRefs = new ArrayList<String>();
    parsePropertyString(value, fragments, propertyRefs);

    StringBuilder sb = new StringBuilder();
    Iterator<String> i = fragments.iterator();
    Iterator<String> j = propertyRefs.iterator();

    while (i.hasNext()) {
      String fragment = i.next();
      if (fragment == null) {
        String propertyName = j.next();
        String defaultValue = null;
        int colon_index = propertyName.indexOf(':');
        if (colon_index > -1) {
          defaultValue = propertyName.substring(colon_index + 1);
          propertyName = propertyName.substring(0, colon_index);
        }
        if (coreProperties != null) {
          fragment = coreProperties.getProperty(propertyName);
        }
        if (fragment == null) {
          fragment = System.getProperty(propertyName, defaultValue);
        }
        if (fragment == null) {
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "No system property or default value specified for " + propertyName + " value:" + value);
        }
      }
      sb.append(fragment);
    }
    return sb.toString();
  }

  /*
   * This method borrowed from Ant's PropertyHelper.parsePropertyStringDefault:
   *   http://svn.apache.org/repos/asf/ant/core/trunk/src/main/org/apache/tools/ant/PropertyHelper.java
   */
  private static void parsePropertyString(String value, List<String> fragments, List<String> propertyRefs) {
    int prev = 0;
    int pos;
    //search for the next instance of $ from the 'prev' position
    while ((pos = value.indexOf("$", prev)) >= 0) {

      //if there was any text before this, add it as a fragment
      //TODO, this check could be modified to go if pos>prev;
      //seems like this current version could stick empty strings
      //into the list
      if (pos > 0) {
        fragments.add(value.substring(prev, pos));
      }
      //if we are at the end of the string, we tack on a $
      //then move past it
      if (pos == (value.length() - 1)) {
        fragments.add("$");
        prev = pos + 1;
      } else if (value.charAt(pos + 1) != '{') {
        //peek ahead to see if the next char is a property or not
        //not a property: insert the char as a literal
              /*
              fragments.addElement(value.substring(pos + 1, pos + 2));
              prev = pos + 2;
              */
        if (value.charAt(pos + 1) == '$') {
          //backwards compatibility two $ map to one mode
          fragments.add("$");
          prev = pos + 2;
        } else {
          //new behaviour: $X maps to $X for all values of X!='$'
          fragments.add(value.substring(pos, pos + 2));
          prev = pos + 2;
        }

      } else {
        //property found, extract its name or bail on a typo
        int endName = value.indexOf('}', pos);
        if (endName < 0) {
          throw new RuntimeException("Syntax error in property: " + value);
        }
        String propertyName = value.substring(pos + 2, endName);
        fragments.add(null);
        propertyRefs.add(propertyName);
        prev = endName + 1;
      }
    }
    //no more $ signs found
    //if there is any tail to the string, append it
    if (prev < value.length()) {
      fragments.add(value.substring(prev));
    }
  }

}
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema-tiny.xml b/solr/core/src/test-files/solr/collection1/conf/schema-tiny.xml
new file mode 100644
index 00000000000..08e0aebc42f
-- /dev/null
++ b/solr/core/src/test-files/solr/collection1/conf/schema-tiny.xml
@@ -0,0 +1,38 @@
<?xml version="1.0" encoding="UTF-8" ?>
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<schema name="tiny" version="1.1">
  <types>
    <fieldType name="string" class="solr.StrField"/>
  </types>
  <fields>
    <field name="id" type="string" indexed="true" stored="true" required="true"/>
    <field name="text" type="text" indexed="true" stored="true"/>
    <dynamicField name="*_t" type="text" indexed="true" stored="true"/>
    <dynamicField name="*" type="string" indexed="true" stored="true"/>
  </fields>
  <uniqueKey>id</uniqueKey>

  <types>
    <fieldtype name="text" class="solr.TextField">
      <analyzer>
        <tokenizer class="solr.WhitespaceTokenizerFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
    </fieldtype>
  </types>
</schema>
diff --git a/solr/core/src/test-files/solr/collection1/conf/solrconfig-minimal.xml b/solr/core/src/test-files/solr/collection1/conf/solrconfig-minimal.xml
new file mode 100644
index 00000000000..5fb39bb6f71
-- /dev/null
++ b/solr/core/src/test-files/solr/collection1/conf/solrconfig-minimal.xml
@@ -0,0 +1,76 @@
<?xml version="1.0" encoding="UTF-8" ?>

<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->

    <!-- For testing, I need to create some custom directories on the fly, particularly for some of the new
     discovery-based core configuration. Trying a minimal configuration to cut down the setup time.
     use in conjunction with schema-minimal.xml perhaps? -->
<config>
  <luceneMatchVersion>LUCENE_41</luceneMatchVersion>

  <dataDir>${solr.data.dir:}</dataDir>

  <directoryFactory name="DirectoryFactory"
                    class="${solr.directoryFactory:solr.NRTCachingDirectoryFactory}"/>

  <indexConfig>
  </indexConfig>

  <jmx/>
  <updateHandler class="solr.DirectUpdateHandler2">
    <!--updateLog>
      <str name="dir">${solr.ulog.dir:}</str>
    </updateLog-->
  </updateHandler>

  <query>
    <enableLazyFieldLoading>true</enableLazyFieldLoading>
    <queryResultWindowSize>20</queryResultWindowSize>
    <queryResultMaxDocsCached>20</queryResultMaxDocsCached>

    <useColdSearcher>true</useColdSearcher>

    <maxWarmingSearchers>1</maxWarmingSearchers>

  </query>

  <requestHandler name="/admin/" class="solr.admin.AdminHandlers" />

  <requestDispatcher handleSelect="false">
    <httpCaching never304="true"/>
  </requestDispatcher>
  <requestHandler name="/select" class="solr.SearchHandler">
    <lst name="defaults">
      <str name="echoParams">explicit</str>
      <str name="wt">json</str>
      <str name="indent">true</str>
      <str name="df">text</str>
    </lst>

  </requestHandler>
  <requestHandler name="/update" class="solr.UpdateRequestHandler">
  </requestHandler>

  <queryResponseWriter name="json" class="solr.JSONResponseWriter">
    <!-- For the purposes of the tutorial, JSON responses are written as
     plain text so that they are easy to read in *any* browser.
     If you expect a MIME type of "application/json" just remove this override.
    -->
    <str name="content-type">text/plain; charset=UTF-8</str>
  </queryResponseWriter>
</config>
diff --git a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeySafeLeaderTest.java b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeySafeLeaderTest.java
index 7b498e912da..19375248414 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeySafeLeaderTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeySafeLeaderTest.java
@@ -34,7 +34,7 @@ import org.junit.Before;
 import org.junit.BeforeClass;
 
 @Slow
public class ChaosMonkeySafeLeaderTest extends AbstractFullDistribZkTestBase {
public class  ChaosMonkeySafeLeaderTest extends AbstractFullDistribZkTestBase {
   
   private static final Integer RUN_LENGTH = Integer.parseInt(System.getProperty("solr.tests.cloud.cm.runlength", "-1"));
 
diff --git a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
index d59d31aa61d..dc930eade56 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
@@ -242,7 +242,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
   private CoreContainer getCoreContainer() {
     CoreContainer cc = new CoreContainer(TEMP_DIR.getAbsolutePath()) {
       {
        initShardHandler(null);
        initShardHandler();
       }
     };
     
diff --git a/solr/core/src/test/org/apache/solr/core/TestLazyCores.java b/solr/core/src/test/org/apache/solr/core/TestLazyCores.java
index 0fb95608acf..10a4d89c5e6 100644
-- a/solr/core/src/test/org/apache/solr/core/TestLazyCores.java
++ b/solr/core/src/test/org/apache/solr/core/TestLazyCores.java
@@ -20,7 +20,6 @@ package org.apache.solr.core;
 import org.apache.commons.io.FileUtils;
 import org.apache.lucene.util.IOUtils;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.request.LocalSolrQueryRequest;
 import org.apache.solr.request.SolrQueryRequest;
@@ -29,6 +28,7 @@ import org.apache.solr.update.AddUpdateCommand;
 import org.apache.solr.update.CommitUpdateCommand;
 import org.apache.solr.update.UpdateHandler;
 import org.apache.solr.util.RefCounted;
import org.junit.After;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
@@ -43,47 +43,44 @@ public class TestLazyCores extends SolrTestCaseJ4 {
 
   @BeforeClass
   public static void beforeClass() throws Exception {
    initCore("solrconfig.xml", "schema.xml");
    initCore("solrconfig-minimal.xml", "schema-tiny.xml");
   }
 
  private final File _solrHomeDirectory = new File(TEMP_DIR, "org.apache.solr.core.TestLazyCores_testlazy");

  private static String[] _necessaryConfs = {"schema.xml", "solrconfig.xml", "stopwords.txt", "synonyms.txt",
      "protwords.txt", "old_synonyms.txt", "currency.xml", "open-exchange-rates.json", "mapping-ISOLatin1Accent.txt"};
  private final File solrHomeDirectory = new File(TEMP_DIR, "org.apache.solr.core.TestLazyCores_testlazy");
 
   private void copyConfFiles(File home, String subdir) throws IOException {
 
     File subHome = new File(new File(home, subdir), "conf");
     assertTrue("Failed to make subdirectory ", subHome.mkdirs());
     String top = SolrTestCaseJ4.TEST_HOME() + "/collection1/conf";
    for (String file : _necessaryConfs) {
      FileUtils.copyFile(new File(top, file), new File(subHome, file));
    }
    FileUtils.copyFile(new File(top, "schema-tiny.xml"), new File(subHome, "schema-tiny.xml"));
    FileUtils.copyFile(new File(top, "solrconfig-minimal.xml"), new File(subHome, "solrconfig-minimal.xml"));
   }
 
   private CoreContainer init() throws Exception {
 
    if (_solrHomeDirectory.exists()) {
      FileUtils.deleteDirectory(_solrHomeDirectory);
    if (solrHomeDirectory.exists()) {
      FileUtils.deleteDirectory(solrHomeDirectory);
     }
    assertTrue("Failed to mkdirs workDir", _solrHomeDirectory.mkdirs());
    assertTrue("Failed to mkdirs workDir", solrHomeDirectory.mkdirs());
     for (int idx = 1; idx < 10; ++idx) {
      copyConfFiles(_solrHomeDirectory, "collection" + idx);
      copyConfFiles(solrHomeDirectory, "collection" + idx);
     }
 
    File solrXml = new File(_solrHomeDirectory, "solr.xml");
    File solrXml = new File(solrHomeDirectory, "solr.xml");
     FileUtils.write(solrXml, LOTS_SOLR_XML, IOUtils.CHARSET_UTF_8.toString());
    final CoreContainer cores = new CoreContainer(_solrHomeDirectory.getAbsolutePath());
    cores.load(_solrHomeDirectory.getAbsolutePath(), solrXml);
    //  h.getCoreContainer().load(_solrHomeDirectory.getAbsolutePath(), new File(_solrHomeDirectory, "solr.xml"));
    final CoreContainer cores = new CoreContainer(solrHomeDirectory.getAbsolutePath());
    cores.load(solrHomeDirectory.getAbsolutePath(), solrXml);
    //  h.getCoreContainer().load(solrHomeDirectory.getAbsolutePath(), new File(solrHomeDirectory, "solr.xml"));
 
     cores.setPersistent(false);
     return cores;
   }
 
  @After
   public void after() throws Exception {
    if (_solrHomeDirectory.exists()) {
      FileUtils.deleteDirectory(_solrHomeDirectory);
    if (solrHomeDirectory.exists()) {
      FileUtils.deleteDirectory(solrHomeDirectory);
     }
   }
 
@@ -155,29 +152,29 @@ public class TestLazyCores extends SolrTestCaseJ4 {
 
       // Just get a couple of searches to work!
       assertQ("test prefix query",
          makeReq(core4, "q", "{!prefix f=v_t}hel")
          makeReq(core4, "q", "{!prefix f=v_t}hel", "wt", "xml")
           , "//result[@numFound='2']"
       );
 
       assertQ("test raw query",
          makeReq(core4, "q", "{!raw f=v_t}hello")
          makeReq(core4, "q", "{!raw f=v_t}hello", "wt", "xml")
           , "//result[@numFound='2']"
       );
 
       // Now just insure that the normal searching on "collection1" finds _0_ on the same query that found _2_ above.
       // Use of makeReq above and req below is tricky, very tricky.
       assertQ("test raw query",
          req("q", "{!raw f=v_t}hello")
          req("q", "{!raw f=v_t}hello", "wt", "xml")
           , "//result[@numFound='0']"
       );
 
       // no analysis is done, so these should match nothing
       assertQ("test raw query",
          makeReq(core4, "q", "{!raw f=v_t}Hello")
          makeReq(core4, "q", "{!raw f=v_t}Hello", "wt", "xml")
           , "//result[@numFound='0']"
       );
       assertQ("test raw query",
          makeReq(core4, "q", "{!raw f=v_f}1.5")
          makeReq(core4, "q", "{!raw f=v_f}1.5", "wt", "xml")
           , "//result[@numFound='0']"
       );
 
@@ -196,8 +193,8 @@ public class TestLazyCores extends SolrTestCaseJ4 {
     try {
       // First check that all the cores that should be loaded at startup actually are.
 
      checkInCores(cc, "collection1",  "collectionLazy2", "collectionLazy5");
      checkNotInCores(cc,"collectionLazy3", "collectionLazy4", "collectionLazy6",
      checkInCores(cc, "collection1", "collectionLazy2", "collectionLazy5");
      checkNotInCores(cc, "collectionLazy3", "collectionLazy4", "collectionLazy6",
           "collectionLazy7", "collectionLazy8", "collectionLazy9");
 
       // By putting these in non-alpha order, we're also checking that we're  not just seeing an artifact.
@@ -251,7 +248,7 @@ public class TestLazyCores extends SolrTestCaseJ4 {
   // Test case for SOLR-4300
   @Test
   public void testRace() throws Exception {
    final List<SolrCore> _theCores = new ArrayList<SolrCore>();
    final List<SolrCore> theCores = new ArrayList<SolrCore>();
     final CoreContainer cc = init();
     try {
 
@@ -261,23 +258,20 @@ public class TestLazyCores extends SolrTestCaseJ4 {
           @Override
           public void run() {
             SolrCore core = cc.getCore("collectionLazy3");
            synchronized (_theCores) {
              _theCores.add(core);
            synchronized (theCores) {
              theCores.add(core);
             }
           }
         };
         threads[idx].start();
       }

       for (Thread thread : threads) {
         thread.join();
       }

      for (int idx = 0; idx < _theCores.size() - 1; ++idx) {
        assertEquals("Cores should be the same!", _theCores.get(idx), _theCores.get(idx + 1));
      for (int idx = 0; idx < theCores.size() - 1; ++idx) {
        assertEquals("Cores should be the same!", theCores.get(idx), theCores.get(idx + 1));
       }

      for (SolrCore core : _theCores) {
      for (SolrCore core : theCores) {
         core.close();
       }
 
@@ -286,33 +280,24 @@ public class TestLazyCores extends SolrTestCaseJ4 {
     }
   }
 
  private void checkNotInCores(CoreContainer cc, String... nameCheck) {
  public static void checkNotInCores(CoreContainer cc, String... nameCheck) {
     Collection<String> names = cc.getCoreNames();
     for (String name : nameCheck) {
       assertFalse("core " + name + " was found in the list of cores", names.contains(name));
     }
   }
 
  private void checkInCores(CoreContainer cc, String... nameCheck) {
  public static void checkInCores(CoreContainer cc, String... nameCheck) {
     Collection<String> names = cc.getCoreNames();
     for (String name : nameCheck) {
       assertTrue("core " + name + " was not found in the list of cores", names.contains(name));
     }
   }
 

   private void addLazy(SolrCore core, String... fieldValues) throws IOException {
     UpdateHandler updater = core.getUpdateHandler();
    SolrQueryRequest req = makeReq(core);
    AddUpdateCommand cmd = new AddUpdateCommand(req);
    if ((fieldValues.length % 2) != 0) {
      throw new RuntimeException("The length of the string array (query arguments) needs to be even");
    }
    cmd.solrDoc = new SolrInputDocument();
    for (int idx = 0; idx < fieldValues.length; idx += 2) {
      cmd.solrDoc.addField(fieldValues[idx], fieldValues[idx + 1]);
    }

    AddUpdateCommand cmd = new AddUpdateCommand(makeReq(core));
    cmd.solrDoc = sdoc(fieldValues);
     updater.addDoc(cmd);
   }
 
@@ -333,15 +318,32 @@ public class TestLazyCores extends SolrTestCaseJ4 {
 
   private final static String LOTS_SOLR_XML = " <solr persistent=\"false\"> " +
       "<cores adminPath=\"/admin/cores\" defaultCoreName=\"collectionLazy2\" transientCacheSize=\"4\">  " +
      "<core name=\"collection1\" instanceDir=\"collection1\" /> " +
      "<core name=\"collectionLazy2\" instanceDir=\"collection2\" transient=\"true\" loadOnStartup=\"true\"  /> " +
      "<core name=\"collectionLazy3\" instanceDir=\"collection3\" transient=\"on\" loadOnStartup=\"false\"/> " +
      "<core name=\"collectionLazy4\" instanceDir=\"collection4\" transient=\"false\" loadOnStartup=\"false\"/> " +
      "<core name=\"collectionLazy5\" instanceDir=\"collection5\" transient=\"false\" loadOnStartup=\"true\"/> " +
      "<core name=\"collectionLazy6\" instanceDir=\"collection6\" transient=\"true\" loadOnStartup=\"false\" /> " +
      "<core name=\"collectionLazy7\" instanceDir=\"collection7\" transient=\"true\" loadOnStartup=\"false\" /> " +
      "<core name=\"collectionLazy8\" instanceDir=\"collection8\" transient=\"true\" loadOnStartup=\"false\" /> " +
      "<core name=\"collectionLazy9\" instanceDir=\"collection9\" transient=\"true\" loadOnStartup=\"false\" /> " +
      "<core name=\"collection1\" instanceDir=\"collection1\" config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\" /> " +

      "<core name=\"collectionLazy2\" instanceDir=\"collection2\" transient=\"true\" loadOnStartup=\"true\"  " +
      " config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\" /> " +

      "<core name=\"collectionLazy3\" instanceDir=\"collection3\" transient=\"on\" loadOnStartup=\"false\" " +
      "config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\"  /> " +

      "<core name=\"collectionLazy4\" instanceDir=\"collection4\" transient=\"false\" loadOnStartup=\"false\" " +
      "config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\"  /> " +

      "<core name=\"collectionLazy5\" instanceDir=\"collection5\" transient=\"false\" loadOnStartup=\"true\" " +
      "config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\"  /> " +

      "<core name=\"collectionLazy6\" instanceDir=\"collection6\" transient=\"true\" loadOnStartup=\"false\" " +
      "config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\"  /> " +

      "<core name=\"collectionLazy7\" instanceDir=\"collection7\" transient=\"true\" loadOnStartup=\"false\" " +
      "config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\"  /> " +

      "<core name=\"collectionLazy8\" instanceDir=\"collection8\" transient=\"true\" loadOnStartup=\"false\" " +
      "config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\"  /> " +

      "<core name=\"collectionLazy9\" instanceDir=\"collection9\" transient=\"true\" loadOnStartup=\"false\" " +
      "config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\"  /> " +

       "</cores> " +
       "</solr>";
 }
diff --git a/solr/core/src/test/org/apache/solr/core/TestSolrDiscoveryProperties.java b/solr/core/src/test/org/apache/solr/core/TestSolrDiscoveryProperties.java
new file mode 100644
index 00000000000..2b7637af520
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/core/TestSolrDiscoveryProperties.java
@@ -0,0 +1,389 @@
package org.apache.solr.core;

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
 * WITHOUT WARRANTIES OR CONDITIONS F ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.lucene.util.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;

public class TestSolrDiscoveryProperties extends SolrTestCaseJ4 {
  private static String NEW_LINE = System.getProperty("line.separator");

  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore();
  }

  private final File solrHomeDirectory = new File(TEMP_DIR, "org.apache.solr.core.TestSolrProperties" + File.separator + "solrHome");

  private void setMeUp() throws Exception {
    if (solrHomeDirectory.exists()) {
      FileUtils.deleteDirectory(solrHomeDirectory);
    }
    assertTrue("Failed to mkdirs workDir", solrHomeDirectory.mkdirs());
    System.setProperty("solr.solr.home", solrHomeDirectory.getAbsolutePath());
  }

  private void addSolrPropertiesFile(String... extras) throws Exception {
    File solrProps = new File(solrHomeDirectory, SolrProperties.SOLR_PROPERTIES_FILE);
    Properties props = new Properties();
    props.load(new StringReader(SOLR_PROPERTIES));
    for (String extra : extras) {
      String[] parts = extra.split("=");
      props.put(parts[0], parts[1]);
    }
    props.store(new FileOutputStream(solrProps.getAbsolutePath()), null);
  }

  private void addSolrXml() throws Exception {
    File tmpFile = new File(solrHomeDirectory, SolrProperties.SOLR_XML_FILE);
    FileUtils.write(tmpFile, SOLR_XML, IOUtils.CHARSET_UTF_8.toString());
  }

  private Properties makeCorePropFile(String name, boolean isLazy, boolean loadOnStartup, String... extraProps) {
    Properties props = new Properties();
    props.put(CoreDescriptor.CORE_NAME, name);
    props.put(CoreDescriptor.CORE_SCHEMA, "schema-tiny.xml");
    props.put(CoreDescriptor.CORE_CONFIG, "solrconfig-minimal.xml");
    props.put(CoreDescriptor.CORE_TRANSIENT, Boolean.toString(isLazy));
    props.put(CoreDescriptor.CORE_LOADONSTARTUP, Boolean.toString(loadOnStartup));
    props.put(CoreDescriptor.CORE_DATADIR, "${core.dataDir:stuffandnonsense}");

    for (String extra : extraProps) {
      String[] parts = extra.split("=");
      props.put(parts[0], parts[1]);
    }

    return props;
  }

  private void addCoreWithProps(Properties stockProps) throws Exception {

    File propFile = new File(solrHomeDirectory,
        stockProps.getProperty(CoreDescriptor.CORE_NAME) + File.separator + SolrProperties.CORE_PROP_FILE);
    File parent = propFile.getParentFile();
    assertTrue("Failed to mkdirs for " + parent.getAbsolutePath(), parent.mkdirs());
    stockProps.store(new FileOutputStream(propFile), null);
    addConfFiles(new File(parent, "conf"));
  }

  private void addConfFiles(File confDir) throws Exception {
    String top = SolrTestCaseJ4.TEST_HOME() + "/collection1/conf";
    assertTrue("Failed to mkdirs for " + confDir.getAbsolutePath(), confDir.mkdirs());
    FileUtils.copyFile(new File(top, "schema-tiny.xml"), new File(confDir, "schema-tiny.xml"));
    FileUtils.copyFile(new File(top, "solrconfig-minimal.xml"), new File(confDir, "solrconfig-minimal.xml"));
  }

  private void addConfigsForBackCompat() throws Exception {
    addConfFiles(new File(solrHomeDirectory, "collection1" + File.separator + "conf"));
  }

  private CoreContainer init() throws Exception {

    CoreContainer.Initializer init = new CoreContainer.Initializer();

    final CoreContainer cores = init.initialize();

    cores.setPersistent(false);
    return cores;
  }

  @After
  public void after() throws Exception {
    if (solrHomeDirectory.exists()) {
      FileUtils.deleteDirectory(solrHomeDirectory);
    }
  }

  // Test the basic setup, create some dirs with core.properties files in them, but no solr.xml (a solr.properties
  // instead) and insure that we find all the cores and can load them.
  @Test
  public void testPropertiesFile() throws Exception {
    setMeUp();
    addSolrPropertiesFile();
    // name, isLazy, loadOnStartup
    addCoreWithProps(makeCorePropFile("core1", false, true));
    addCoreWithProps(makeCorePropFile("core2", false, false));

    // I suspect what we're adding in here is a "configset" rather than a schema or solrconfig.
    //
    addCoreWithProps(makeCorePropFile("lazy1", true, false));

    CoreContainer cc = init();
    try {
      Properties props = cc.containerProperties;

      assertEquals("/admin/cores/props", props.getProperty("cores.adminPath"));
      assertEquals("/admin/cores/props", cc.getAdminPath());
      assertEquals("defcore", props.getProperty("cores.defaultCoreName"));
      assertEquals("defcore", cc.getDefaultCoreName());
      assertEquals("222.333.444.555", props.getProperty("host"));
      assertEquals("6000", props.getProperty("port")); // getProperty actually looks at original props.
      assertEquals("/solrprop", props.getProperty("cores.hostContext"));
      assertEquals("20", props.getProperty("cores.zkClientTimeout"));

      TestLazyCores.checkInCores(cc, "core1");
      TestLazyCores.checkNotInCores(cc, "lazy1", "core2", "collection1");

      SolrCore core1 = cc.getCore("core1");
      SolrCore core2 = cc.getCore("core2");
      SolrCore lazy1 = cc.getCore("lazy1");
      TestLazyCores.checkInCores(cc, "core1", "core2", "lazy1");
      core1.close();
      core2.close();
      lazy1.close();

    } finally {
      cc.shutdown();
    }
  }


  // Check that the various flavors of persistence work, including saving the state of a core when it's being swapped
  // out. Added a test in here to insure that files that have config variables are saved with the config vars not the
  // substitutions.
  @Test
  public void testPersistTrue() throws Exception {
    setMeUp();
    addSolrPropertiesFile();
    System.setProperty("solr.persistent", "true");

    Properties special = makeCorePropFile("core1", false, true);
    special.put(CoreDescriptor.CORE_INSTDIR, "${core1inst:anothersillypath}");
    addCoreWithProps(special);
    addCoreWithProps(makeCorePropFile("core2", false, false));
    addCoreWithProps(makeCorePropFile("lazy1", true, true));
    addCoreWithProps(makeCorePropFile("lazy2", true, true));
    addCoreWithProps(makeCorePropFile("lazy3", true, false));

    System.setProperty("core1inst", "core1");
    CoreContainer cc = init();
    SolrCore coreC1 = cc.getCore("core1");
    addCoreProps(coreC1, "addedPropC1=addedC1", "addedPropC1B=foo", "addedPropC1C=bar");

    SolrCore coreC2 = cc.getCore("core2");
    addCoreProps(coreC2, "addedPropC2=addedC2", "addedPropC2B=foo", "addedPropC2C=bar");

    SolrCore coreL1 = cc.getCore("lazy1");
    addCoreProps(coreL1, "addedPropL1=addedL1", "addedPropL1B=foo", "addedPropL1C=bar");

    SolrCore coreL2 = cc.getCore("lazy2");
    addCoreProps(coreL2, "addedPropL2=addedL2", "addedPropL2B=foo", "addedPropL2C=bar");

    SolrCore coreL3 = cc.getCore("lazy3");
    addCoreProps(coreL3, "addedPropL3=addedL3", "addedPropL3B=foo", "addedPropL3C=bar");

    try {
      cc.persist();

      // Insure that one of the loaded cores was swapped out, with a cache size of 2 lazy1 should be gone.
      TestLazyCores.checkInCores(cc, "core1", "core2", "lazy2", "lazy3");
      TestLazyCores.checkNotInCores(cc, "lazy1");

      checkSolrProperties(cc);

      File xmlFile = new File(solrHomeDirectory, "solr.xml");
      assertFalse("Solr.xml should NOT exist", xmlFile.exists());

      Properties orig = makeCorePropFile("core1", false, true);
      orig.put(CoreDescriptor.CORE_INSTDIR, "${core1inst:anothersillypath}");
      checkCoreProps(orig, "addedPropC1=addedC1", "addedPropC1B=foo", "addedPropC1C=bar");

      orig = makeCorePropFile("core2", false, false);
      checkCoreProps(orig, "addedPropC2=addedC2", "addedPropC2B=foo", "addedPropC2C=bar");

      // This test insures that a core that was swapped out has its properties file persisted. Currently this happens
      // as the file is removed from the cache.
      orig = makeCorePropFile("lazy1", true, true);
      checkCoreProps(orig, "addedPropL1=addedL1", "addedPropL1B=foo", "addedPropL1C=bar");

      orig = makeCorePropFile("lazy2", true, true);
      checkCoreProps(orig, "addedPropL2=addedL2", "addedPropL2B=foo", "addedPropL2C=bar");

      orig = makeCorePropFile("lazy3", true, false);
      checkCoreProps(orig, "addedPropL3=addedL3", "addedPropL3B=foo", "addedPropL3C=bar");

      coreC1.close();
      coreC2.close();
      coreL1.close();
      coreL2.close();
      coreL3.close();

    } finally {
      cc.shutdown();
    }
  }

  // Make sure that, even if we do call persist, nothing's saved unless the flag is set in solr.properties.
  @Test
  public void testPersistFalse() throws Exception {
    setMeUp();
    addSolrPropertiesFile();

    addCoreWithProps(makeCorePropFile("core1", false, true));
    addCoreWithProps(makeCorePropFile("core2", false, false));
    addCoreWithProps(makeCorePropFile("lazy1", true, true));
    addCoreWithProps(makeCorePropFile("lazy2", false, true));

    CoreContainer cc = init();
    SolrCore coreC1 = cc.getCore("core1");
    addCoreProps(coreC1, "addedPropC1=addedC1", "addedPropC1B=foo", "addedPropC1C=bar");

    SolrCore coreC2 = cc.getCore("core2");
    addCoreProps(coreC2, "addedPropC2=addedC2", "addedPropC2B=foo", "addedPropC2C=bar");

    SolrCore coreL1 = cc.getCore("lazy1");
    addCoreProps(coreL1, "addedPropL1=addedL1", "addedPropL1B=foo", "addedPropL1C=bar");

    SolrCore coreL2 = cc.getCore("lazy2");
    addCoreProps(coreL2, "addedPropL2=addedL2", "addedPropL2B=foo", "addedPropL2C=bar");


    try {
      cc.persist();
      checkSolrProperties(cc);

      checkCoreProps(makeCorePropFile("core1", false, true));
      checkCoreProps(makeCorePropFile("core2", false, false));
      checkCoreProps(makeCorePropFile("lazy1", true, true));
      checkCoreProps(makeCorePropFile("lazy2", false, true));

      coreC1.close();
      coreC2.close();
      coreL1.close();
      coreL2.close();
    } finally {
      cc.shutdown();
    }
  }

  void addCoreProps(SolrCore core, String... propPairs) {
    for (String keyval : propPairs) {
      String[] pair = keyval.split("=");
      core.getCoreDescriptor().putProperty(pair[0], pair[1]);
    }
  }

  // Insure that the solr.properties is as it should be after persisting _and_, in some cases, different than
  // what's in memory
  void checkSolrProperties(CoreContainer cc, String... checkMemPairs) throws Exception {
    Properties orig = new Properties();
    orig.load(new StringReader(SOLR_PROPERTIES));

    Properties curr = cc.getContainerProperties();

    Properties persisted = new Properties();
    persisted.load(new FileInputStream(new File(solrHomeDirectory, SolrProperties.SOLR_PROPERTIES_FILE)));

    assertEquals("Persisted and original should be the same size", orig.size(), persisted.size());

    for (String prop : orig.stringPropertyNames()) {
      assertEquals("Values of original should match current", orig.getProperty(prop), persisted.getProperty(prop));
    }

    Properties specialProps = new Properties();
    for (String special : checkMemPairs) {
      String[] pair = special.split("=");
      specialProps.put(pair[0], pair[1]);
    }
    // OK, current should match original except if the property is "special"
    for (String prop : curr.stringPropertyNames()) {
      String val = specialProps.getProperty(prop);
      if (val != null) { // Compare curr and val
        assertEquals("Modified property should be in current container properties", val, curr.getProperty(prop));
      }
    }
  }

  // Insure that the properties in the core passed in are exactly what's in the default core.properties below plus
  // whatever extra is passed in.
  void checkCoreProps(Properties orig, String... extraProps) throws Exception {
    // Read the persisted file.
    Properties props = new Properties();
    File propParent = new File(solrHomeDirectory, orig.getProperty(CoreDescriptor.CORE_NAME));
    props.load(new FileInputStream(new File(propParent, SolrProperties.CORE_PROP_FILE)));
    Set<String> propSet = props.stringPropertyNames();

    assertEquals("Persisted properties should NOT contain extra properties", propSet.size(), orig.size());

    for (String prop : orig.stringPropertyNames()) {
      assertEquals("Original and new properties should be equal for " + prop, props.getProperty(prop), orig.getProperty(prop));
    }
    for (String prop : extraProps) {
      String[] pair = prop.split("=");
      assertNull("Modified parameters should not be present for " + prop, props.getProperty(pair[0]));
    }
  }

  // If there's a solr.xml AND a properties file, make sure that the xml file is loaded and the properties file
  // is ignored.

  @Test
  public void testBackCompatXml() throws Exception {
    setMeUp();
    addSolrPropertiesFile();
    addSolrXml();
    addConfigsForBackCompat();

    CoreContainer cc = init();
    try {
      Properties props = cc.getContainerProperties();

      assertEquals("/admin/cores", cc.getAdminPath());
      assertEquals("collectionLazy2", cc.getDefaultCoreName());

      // Shouldn't get these in properties at this point
      assertNull(props.getProperty("cores.adminPath"));
      assertNull(props.getProperty("cores.defaultCoreName"));
      assertNull(props.getProperty("host"));
      assertNull(props.getProperty("port")); // getProperty actually looks at original props.
      assertNull(props.getProperty("cores.hostContext"));
      assertNull(props.getProperty("cores.zkClientTimeout"));
    } finally {
      cc.shutdown();
    }
  }

  // For this test I want some of these to be different than what would be in solr.xml by default.
  private final static String SOLR_PROPERTIES =
      "persistent=${persistent:false}" + NEW_LINE +
          "cores.adminPath=/admin/cores/props" + NEW_LINE +
          "cores.defaultCoreName=defcore" + NEW_LINE +
          "host=222.333.444.555" + NEW_LINE +
          "port=6000" + NEW_LINE +
          "cores.hostContext=/solrprop" + NEW_LINE +
          "cores.zkClientTimeout=20" + NEW_LINE +
          "cores.transientCacheSize=2";

  // For testing whether finding a solr.xml overrides looking at solr.properties
  private final static String SOLR_XML = " <solr persistent=\"false\"> " +
      "<cores adminPath=\"/admin/cores\" defaultCoreName=\"collectionLazy2\" transientCacheSize=\"4\">  " +
      "<core name=\"collection1\" instanceDir=\"collection1\" config=\"solrconfig-minimal.xml\" schema=\"schema-tiny.xml\" /> " +
      "</cores> " +
      "</solr>";
}
diff --git a/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java b/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java
index 1588524ad9f..097bcf66169 100644
-- a/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java
++ b/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java
@@ -191,7 +191,7 @@ public class TestHarness {
           hostPort = System.getProperty("hostPort");
           hostContext = "solr";
           defaultCoreName = CoreContainer.DEFAULT_DEFAULT_CORE_NAME;
          initShardHandler(null);
          initShardHandler();
           initZooKeeper(System.getProperty("zkHost"), 10000);
         }
       };
- 
2.19.1.windows.1

