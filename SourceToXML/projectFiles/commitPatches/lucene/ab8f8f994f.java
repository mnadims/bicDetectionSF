From ab8f8f994f2b0c6e168e837a32e3e1a83a9b1b47 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Wed, 20 Jul 2011 08:49:32 +0000
Subject: [PATCH] SOLR-2382 Properties writer abstracted

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1148653 13f79535-47bb-0310-9956-ffa450edef68
--
 .../src/Dataimporthandler-extras.iml          |  18 +++
 .../dataimport/TestMailEntityProcessor.java   |   2 +-
 .../dataimport/DIHPropertiesWriter.java       |  34 +++++
 .../handler/dataimport/DataImportHandler.java |   6 +-
 .../solr/handler/dataimport/DataImporter.java |  32 +++--
 .../solr/handler/dataimport/DocBuilder.java   |   9 +-
 .../dataimport/SimplePropertiesWriter.java    | 123 ++++++++++++++++++
 .../solr/handler/dataimport/SolrWriter.java   |  82 +-----------
 .../handler/dataimport/TestDocBuilder.java    |   2 +-
 9 files changed, 212 insertions(+), 96 deletions(-)
 create mode 100644 solr/contrib/dataimporthandler-extras/src/Dataimporthandler-extras.iml
 create mode 100644 solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHPropertiesWriter.java
 create mode 100644 solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SimplePropertiesWriter.java

diff --git a/solr/contrib/dataimporthandler-extras/src/Dataimporthandler-extras.iml b/solr/contrib/dataimporthandler-extras/src/Dataimporthandler-extras.iml
new file mode 100644
index 00000000000..17a7750666b
-- /dev/null
++ b/solr/contrib/dataimporthandler-extras/src/Dataimporthandler-extras.iml
@@ -0,0 +1,18 @@
<?xml version="1.0" encoding="UTF-8"?>
<module type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" inherit-compiler-output="true">
    <exclude-output />
    <content url="file://$MODULE_DIR$">
      <sourceFolder url="file://$MODULE_DIR$/test" isTestSource="true" />
      <sourceFolder url="file://$MODULE_DIR$/java" isTestSource="false" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
    <orderEntry type="library" name="lib2" level="project" />
    <orderEntry type="library" name="lib1" level="project" />
    <orderEntry type="library" name="lib" level="project" />
    <orderEntry type="library" name="lib8" level="project" />
    <orderEntry type="module" module-name="Solrj" />
  </component>
</module>

diff --git a/solr/contrib/dataimporthandler-extras/src/test/org/apache/solr/handler/dataimport/TestMailEntityProcessor.java b/solr/contrib/dataimporthandler-extras/src/test/org/apache/solr/handler/dataimport/TestMailEntityProcessor.java
index 3ca71a737ed..a89477c3dd3 100644
-- a/solr/contrib/dataimporthandler-extras/src/test/org/apache/solr/handler/dataimport/TestMailEntityProcessor.java
++ b/solr/contrib/dataimporthandler-extras/src/test/org/apache/solr/handler/dataimport/TestMailEntityProcessor.java
@@ -188,7 +188,7 @@ public class TestMailEntityProcessor extends AbstractDataImportHandlerTestCase {
     Boolean commitCalled;
 
     public SolrWriterImpl() {
      super(null, ".", null);
      super(null, null);
     }
 
     @Override
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHPropertiesWriter.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHPropertiesWriter.java
new file mode 100644
index 00000000000..83a2b04d412
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHPropertiesWriter.java
@@ -0,0 +1,34 @@
package org.apache.solr.handler.dataimport;
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
import java.io.File;
import java.util.Properties;

/**
 *
 */
public interface DIHPropertiesWriter {

    public void init(DataImporter dataImporter);

    public boolean isWritable();

	public void persist(Properties props);
	
	public Properties readIndexerProperties();
	
}
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java
index 5da8b133a06..bbf201bc34e 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java
@@ -113,7 +113,7 @@ public class DataImportHandler extends RequestHandlerBase implements
           final InputSource is = new InputSource(core.getResourceLoader().openConfig(configLoc));
           is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(configLoc));
           importer = new DataImporter(is, core,
                  dataSources, coreScopeSession);
                  dataSources, coreScopeSession, myName);
         }
       }
     } catch (Throwable e) {
@@ -165,7 +165,7 @@ public class DataImportHandler extends RequestHandlerBase implements
         try {
           processConfiguration((NamedList) initArgs.get("defaults"));
           importer = new DataImporter(new InputSource(new StringReader(requestParams.dataConfig)), req.getCore()
                  , dataSources, coreScopeSession);
                  , dataSources, coreScopeSession, myName);
         } catch (RuntimeException e) {
           rsp.add("exception", DebugLogger.getStacktraceString(e));
           importer = null;
@@ -280,7 +280,7 @@ public class DataImportHandler extends RequestHandlerBase implements
   private SolrWriter getSolrWriter(final UpdateRequestProcessor processor,
                                    final SolrResourceLoader loader, final DataImporter.RequestParams requestParams, SolrQueryRequest req) {
 
    return new SolrWriter(processor, loader.getConfigDir(), myName, req) {
    return new SolrWriter(processor, req) {
 
       @Override
       public boolean upload(SolrInputDocument document) {
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImporter.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImporter.java
index 85ad0930c5d..56ae340f1ae 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImporter.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImporter.java
@@ -39,7 +39,6 @@ import org.apache.commons.io.IOUtils;
 
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
 import java.io.StringReader;
 import java.text.SimpleDateFormat;
 import java.util.*;
@@ -80,26 +79,35 @@ public class DataImporter {
   public DocBuilder.Statistics cumulativeStatistics = new DocBuilder.Statistics();
 
   private SolrCore core;
  
  private DIHPropertiesWriter propWriter;
 
   private ReentrantLock importLock = new ReentrantLock();
 
   private final Map<String , Object> coreScopeSession;
 
   private boolean isDeltaImportSupported = false;
  private final String handlerName;
 
   /**
    * Only for testing purposes
    */
   DataImporter() {
     coreScopeSession = new ConcurrentHashMap<String, Object>();
    this.propWriter = new SimplePropertiesWriter();
    propWriter.init(this);
    this.handlerName = "dataimport" ;
   }
 
  DataImporter(InputSource dataConfig, SolrCore core, Map<String, Properties> ds, Map<String, Object> session) {
  DataImporter(InputSource dataConfig, SolrCore core, Map<String, Properties> ds, Map<String, Object> session, String handlerName) {
      this.handlerName = handlerName;
     if (dataConfig == null)
       throw new DataImportHandlerException(SEVERE,
               "Configuration not found");
     this.core = core;
     this.schema = core.getSchema();
    this.propWriter = new SimplePropertiesWriter();
    propWriter.init(this);
     dataSourceProps = ds;
     if (session == null)
       session = new HashMap<String, Object>();
@@ -120,7 +128,11 @@ public class DataImporter {
     }
   }
 
  private void verifyWithSchema(Map<String, DataConfig.Field> fields) {
   public String getHandlerName() {
        return handlerName;
    }

    private void verifyWithSchema(Map<String, DataConfig.Field> fields) {
     Map<String, SchemaField> schemaFields = schema.getFields();
     for (Map.Entry<String, SchemaField> entry : schemaFields.entrySet()) {
       SchemaField sf = entry.getValue();
@@ -353,7 +365,7 @@ public class DataImporter {
     setIndexStartTime(new Date());
 
     try {
      docBuilder = new DocBuilder(this, writer, requestParams);
      docBuilder = new DocBuilder(this, writer, propWriter, requestParams);
       checkWritablePersistFile(writer);
       docBuilder.execute();
       if (!requestParams.debug)
@@ -370,11 +382,11 @@ public class DataImporter {
   }
 
   private void checkWritablePersistFile(SolrWriter writer) {
    File persistFile = writer.getPersistFile();
    boolean isWritable = persistFile.exists() ? persistFile.canWrite() : persistFile.getParentFile().canWrite();
    if (isDeltaImportSupported && !isWritable) {
      throw new DataImportHandlerException(SEVERE, persistFile.getAbsolutePath() +
          " is not writable. Delta imports are supported by data config but will not work.");
//  	File persistFile = propWriter.getPersistFile();
//    boolean isWritable = persistFile.exists() ? persistFile.canWrite() : persistFile.getParentFile().canWrite();
    if (isDeltaImportSupported && !propWriter.isWritable()) {
      throw new DataImportHandlerException(SEVERE,
          "Properties is not writable. Delta imports are supported by data config but will not work.");
     }
   }
 
@@ -384,7 +396,7 @@ public class DataImporter {
 
     try {
       setIndexStartTime(new Date());
      docBuilder = new DocBuilder(this, writer, requestParams);
      docBuilder = new DocBuilder(this, writer, propWriter, requestParams);
       checkWritablePersistFile(writer);
       docBuilder.execute();
       if (!requestParams.debug)
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
index fed23064f5f..ead85c20aa5 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
@@ -67,16 +67,19 @@ public class DocBuilder {
   static final ThreadLocal<DocBuilder> INSTANCE = new ThreadLocal<DocBuilder>();
   Map<String, Object> functionsNamespace;
   private Properties persistedProperties;
  
  private DIHPropertiesWriter propWriter;
 
  public DocBuilder(DataImporter dataImporter, SolrWriter writer, DataImporter.RequestParams reqParams) {
  public DocBuilder(DataImporter dataImporter, SolrWriter writer, DIHPropertiesWriter propWriter, DataImporter.RequestParams reqParams) {
     INSTANCE.set(this);
     this.dataImporter = dataImporter;
     this.writer = writer;
    this.propWriter = propWriter;
     DataImporter.QUERY_COUNT.set(importStatistics.queryCount);
     requestParameters = reqParams;
     verboseDebug = requestParameters.debug && requestParameters.verbose;
     functionsNamespace = EvaluatorBag.getFunctionsNamespace(this.dataImporter.getConfig().functions, this);
    persistedProperties = writer.readIndexerProperties();
    persistedProperties = propWriter.readIndexerProperties();
   }
 
   public VariableResolverImpl getVariableResolver() {
@@ -238,7 +241,7 @@ public class DocBuilder {
         addStatusMessage("Optimized");
     }
     try {
      writer.persist(lastIndexTimeProps);
      propWriter.persist(lastIndexTimeProps);
     } catch (Exception e) {
       LOG.error("Could not write property file", e);
       statusMessages.put("error", "Could not write property file. Delta imports will not work. " +
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SimplePropertiesWriter.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SimplePropertiesWriter.java
new file mode 100644
index 00000000000..b9ab396de5f
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SimplePropertiesWriter.java
@@ -0,0 +1,123 @@
package org.apache.solr.handler.dataimport;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplePropertiesWriter implements DIHPropertiesWriter {
	private static final Logger log = LoggerFactory.getLogger(SimplePropertiesWriter.class);

	static final String IMPORTER_PROPERTIES = "dataimport.properties";

	static final String LAST_INDEX_KEY = "last_index_time";

	private String persistFilename = IMPORTER_PROPERTIES;

	private String configDir = null;



    public void init(DataImporter dataImporter) {
      SolrCore core = dataImporter.getCore();
      String configDir = core ==null ? ".": core.getResourceLoader().getConfigDir();
      String persistFileName = dataImporter.getHandlerName();

      this.configDir = configDir;
	  if(persistFileName != null){
        persistFilename = persistFileName + ".properties";
      }
    }



	
	private File getPersistFile() {
    String filePath = configDir;
    if (configDir != null && !configDir.endsWith(File.separator))
      filePath += File.separator;
    filePath += persistFilename;
    return new File(filePath);
  }

    public boolean isWritable() {
        File persistFile =  getPersistFile();
        return persistFile.exists() ? persistFile.canWrite() : persistFile.getParentFile().canWrite();

    }

    @Override
	public void persist(Properties p) {
		OutputStream propOutput = null;

		Properties props = readIndexerProperties();

		try {
			props.putAll(p);
			String filePath = configDir;
			if (configDir != null && !configDir.endsWith(File.separator))
				filePath += File.separator;
			filePath += persistFilename;
			propOutput = new FileOutputStream(filePath);
			props.store(propOutput, null);
			log.info("Wrote last indexed time to " + persistFilename);
		} catch (Exception e) {
			throw new DataImportHandlerException(DataImportHandlerException.SEVERE, "Unable to persist Index Start Time", e);
		} finally {
			try {
				if (propOutput != null)
					propOutput.close();
			} catch (IOException e) {
				propOutput = null;
			}
		}
	}

	@Override
	public Properties readIndexerProperties() {
		Properties props = new Properties();
		InputStream propInput = null;

		try {
			propInput = new FileInputStream(configDir + persistFilename);
			props.load(propInput);
			log.info("Read " + persistFilename);
		} catch (Exception e) {
			log.warn("Unable to read: " + persistFilename);
		} finally {
			try {
				if (propInput != null)
					propInput.close();
			} catch (IOException e) {
				propInput = null;
			}
		}

		return props;
	}

}
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java
index e7bbb6c000f..5184ead5705 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java
@@ -27,7 +27,6 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.io.*;
import java.util.Properties;
 
 /**
  * <p> Writes documents to SOLR as well as provides methods for loading and persisting last index time. </p>
@@ -39,33 +38,19 @@ import java.util.Properties;
 public class SolrWriter {
   private static final Logger log = LoggerFactory.getLogger(SolrWriter.class);
 
  static final String IMPORTER_PROPERTIES = "dataimport.properties";

   static final String LAST_INDEX_KEY = "last_index_time";
 
   private final UpdateRequestProcessor processor;
 
  private final String configDir;

  private String persistFilename = IMPORTER_PROPERTIES;

   DebugLogger debugLogger;
 
   SolrQueryRequest req;
 
  public SolrWriter(UpdateRequestProcessor processor, String confDir, SolrQueryRequest req) {
  public SolrWriter(UpdateRequestProcessor processor, SolrQueryRequest req) {
     this.processor = processor;
    configDir = confDir;
    this.req = req;
  }
  public SolrWriter(UpdateRequestProcessor processor, String confDir, String filePrefix, SolrQueryRequest req) {
    this.processor = processor;
    configDir = confDir;
    if(filePrefix != null){
      persistFilename = filePrefix+".properties";
    }
     this.req = req;
   }
  
 
   public boolean upload(SolrInputDocument d) {
     try {
@@ -90,44 +75,8 @@ public class SolrWriter {
       log.error("Exception while deleteing: " + id, e);
     }
   }


  void persist(Properties p) {
    OutputStream propOutput = null;

    Properties props = readIndexerProperties();

    try {
      props.putAll(p);
      File persistFile = getPersistFile();
      propOutput = new FileOutputStream(persistFile);
      props.store(propOutput, null);
      log.info("Wrote last indexed time to " + persistFile.getAbsolutePath());
    } catch (FileNotFoundException e) {
      throw new DataImportHandlerException(DataImportHandlerException.SEVERE,
              "Unable to persist Index Start Time", e);
    } catch (IOException e) {
      throw new DataImportHandlerException(DataImportHandlerException.SEVERE,
              "Unable to persist Index Start Time", e);
    } finally {
      try {
        if (propOutput != null)
          propOutput.close();
      } catch (IOException e) {
        propOutput = null;
      }
    }
  }

  File getPersistFile() {
    String filePath = configDir;
    if (configDir != null && !configDir.endsWith(File.separator))
      filePath += File.separator;
    filePath += persistFilename;
    return new File(filePath);
  }

  void finish() {
  
	void finish() {
     try {
       processor.finish();
     } catch (IOException e) {
@@ -136,29 +85,6 @@ public class SolrWriter {
     }
   }
   
  Properties readIndexerProperties() {
    Properties props = new Properties();
    InputStream propInput = null;

    try {
      propInput = new FileInputStream(configDir
              + persistFilename);
      props.load(propInput);
      log.info("Read " + persistFilename);
    } catch (Exception e) {
      log.warn("Unable to read: " + persistFilename);
    } finally {
      try {
        if (propInput != null)
          propInput.close();
      } catch (IOException e) {
        propInput = null;
      }
    }

    return props;
  }

   public void deleteByQuery(String query) {
     try {
       log.info("Deleting documents from Solr with query: " + query);
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestDocBuilder.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestDocBuilder.java
index 27ba8b39362..f6ba0b9c2e0 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestDocBuilder.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestDocBuilder.java
@@ -198,7 +198,7 @@ public class TestDocBuilder extends AbstractDataImportHandlerTestCase {
     Boolean finishCalled = Boolean.FALSE;
 
     public SolrWriterImpl() {
      super(null, ".",null);
      super(null, null);
     }
 
     @Override
- 
2.19.1.windows.1

