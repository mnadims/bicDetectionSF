From 07be2c42ba24fea7c4e84836aa4c3f8d059f71d6 Mon Sep 17 00:00:00 2001
From: Varun Thacker <varunthacker1989@gmail.com>
Date: Thu, 23 Jun 2016 23:49:14 +0530
Subject: [PATCH] SOLR-7374: Core level backup/restore now supports specifying
 a directory implementation

--
 solr/CHANGES.txt                              |   3 +
 .../OverseerCollectionMessageHandler.java     |   4 +-
 .../org/apache/solr/core/CoreContainer.java   |   9 +
 .../solr/core/HdfsDirectoryFactory.java       |   2 +-
 .../java/org/apache/solr/core/NodeConfig.java |  20 +-
 .../org/apache/solr/core/SolrXmlConfig.java   |  12 +
 .../backup/repository/BackupRepository.java   | 166 ++++++++++++
 .../repository/BackupRepositoryFactory.java   |  89 +++++++
 .../repository/HdfsBackupRepository.java      | 159 +++++++++++
 .../repository/LocalFileSystemRepository.java | 136 ++++++++++
 .../core/backup/repository/package-info.java  |  23 ++
 .../solr/handler/OldBackupDirectory.java      |  55 ++--
 .../solr/handler/ReplicationHandler.java      |  61 ++++-
 .../org/apache/solr/handler/RestoreCore.java  |  22 +-
 .../org/apache/solr/handler/SnapShooter.java  | 153 ++++++-----
 .../handler/admin/CollectionsHandler.java     |   4 +-
 .../handler/admin/CoreAdminOperation.java     |  42 ++-
 .../apache/solr/store/hdfs/HdfsDirectory.java |   5 +-
 solr/core/src/test-files/solr/solr-50-all.xml |   4 +
 .../solr/cloud/BasicDistributedZk2Test.java   |  27 +-
 .../core/TestBackupRepositoryFactory.java     | 152 +++++++++++
 .../org/apache/solr/core/TestSolrXml.java     |  13 +
 .../solr/handler/BackupRestoreUtils.java      |  69 +++++
 .../solr/handler/CheckBackupStatus.java       |  10 +-
 .../handler/TestHdfsBackupRestoreCore.java    | 251 ++++++++++++++++++
 .../handler/TestReplicationHandlerBackup.java |  39 ++-
 .../apache/solr/handler/TestRestoreCore.java  |  52 ++--
 27 files changed, 1399 insertions(+), 183 deletions(-)
 create mode 100644 solr/core/src/java/org/apache/solr/core/backup/repository/BackupRepository.java
 create mode 100644 solr/core/src/java/org/apache/solr/core/backup/repository/BackupRepositoryFactory.java
 create mode 100644 solr/core/src/java/org/apache/solr/core/backup/repository/HdfsBackupRepository.java
 create mode 100644 solr/core/src/java/org/apache/solr/core/backup/repository/LocalFileSystemRepository.java
 create mode 100644 solr/core/src/java/org/apache/solr/core/backup/repository/package-info.java
 create mode 100644 solr/core/src/test/org/apache/solr/core/TestBackupRepositoryFactory.java
 create mode 100644 solr/core/src/test/org/apache/solr/handler/BackupRestoreUtils.java
 create mode 100644 solr/core/src/test/org/apache/solr/handler/TestHdfsBackupRestoreCore.java

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 683b98baf7d..c8b6914df96 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -66,6 +66,9 @@ New Features
 
 * SOLR-8048: bin/solr script should support basic auth credentials provided in solr.in.sh (noble)
 
* SOLR-7374: Core level Backup/Restore now supports specifying the directory implementation to use
  via the "repository" parameter. (Hrishikesh Gadre, Varun Thacker, Mark Miller)

 Bug Fixes
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionMessageHandler.java b/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionMessageHandler.java
index 2cd09d1c9a7..d7c7ad27fad 100644
-- a/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionMessageHandler.java
++ b/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionMessageHandler.java
@@ -2195,7 +2195,7 @@ public class OverseerCollectionMessageHandler implements OverseerMessageHandler
   private void processBackupAction(ZkNodeProps message, NamedList results) throws IOException, KeeperException, InterruptedException {
     String collectionName =  message.getStr(COLLECTION_PROP);
     String backupName =  message.getStr(NAME);
    String location = message.getStr("location");
    String location = message.getStr(ZkStateReader.BACKUP_LOCATION);
     ShardHandler shardHandler = shardHandlerFactory.getShardHandler();
     String asyncId = message.getStr(ASYNC);
     Map<String, String> requestMap = new HashMap<>();
@@ -2267,7 +2267,7 @@ public class OverseerCollectionMessageHandler implements OverseerMessageHandler
     // TODO maybe we can inherit createCollection's options/code
     String restoreCollectionName =  message.getStr(COLLECTION_PROP);
     String backupName =  message.getStr(NAME); // of backup
    String location = message.getStr("location");
    String location = message.getStr(ZkStateReader.BACKUP_LOCATION);
     ShardHandler shardHandler = shardHandlerFactory.getShardHandler();
     String asyncId = message.getStr(ASYNC);
     Map<String, String> requestMap = new HashMap<>();
diff --git a/solr/core/src/java/org/apache/solr/core/CoreContainer.java b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
index e5e16cba28f..bfab1b89e6a 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreContainer.java
++ b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
@@ -59,6 +59,7 @@ import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.util.ExecutorUtil;
 import org.apache.solr.common.util.IOUtils;
 import org.apache.solr.common.util.Utils;
import org.apache.solr.core.backup.repository.BackupRepositoryFactory;
 import org.apache.solr.handler.RequestHandlerBase;
 import org.apache.solr.handler.admin.CollectionsHandler;
 import org.apache.solr.handler.admin.ConfigSetsHandler;
@@ -152,6 +153,12 @@ public class CoreContainer {
 
   private SecurityPluginHolder<AuthenticationPlugin> authenticationPlugin;
 
  private BackupRepositoryFactory backupRepoFactory;

  public BackupRepositoryFactory getBackupRepoFactory() {
    return backupRepoFactory;
  }

   public ExecutorService getCoreZkRegisterExecutorService() {
     return zkSys.getCoreZkRegisterExecutorService();
   }
@@ -441,6 +448,8 @@ public class CoreContainer {
     initializeAuthorizationPlugin((Map<String, Object>) securityConfig.data.get("authorization"));
     initializeAuthenticationPlugin((Map<String, Object>) securityConfig.data.get("authentication"));
 
    this.backupRepoFactory = new BackupRepositoryFactory(cfg.getBackupRepositoryPlugins());

     containerHandlers.put(ZK_PATH, new ZookeeperInfoHandler(this));
     securityConfHandler = new SecurityConfHandler(this);
     collectionsHandler = createHandler(cfg.getCollectionsHandlerClass(), CollectionsHandler.class);
diff --git a/solr/core/src/java/org/apache/solr/core/HdfsDirectoryFactory.java b/solr/core/src/java/org/apache/solr/core/HdfsDirectoryFactory.java
index c911ac5958f..b003287aa41 100644
-- a/solr/core/src/java/org/apache/solr/core/HdfsDirectoryFactory.java
++ b/solr/core/src/java/org/apache/solr/core/HdfsDirectoryFactory.java
@@ -331,7 +331,7 @@ public class HdfsDirectoryFactory extends CachingDirectoryFactory implements Sol
     }
   }
   
  private Configuration getConf() {
  public Configuration getConf() {
     Configuration conf = new Configuration();
     confDir = getConfig(CONFIG_DIRECTORY, null);
     HdfsUtil.addHdfsResources(conf, confDir);
diff --git a/solr/core/src/java/org/apache/solr/core/NodeConfig.java b/solr/core/src/java/org/apache/solr/core/NodeConfig.java
index 0783355cfe4..e72fbc92cc9 100644
-- a/solr/core/src/java/org/apache/solr/core/NodeConfig.java
++ b/solr/core/src/java/org/apache/solr/core/NodeConfig.java
@@ -58,13 +58,15 @@ public class NodeConfig {
 
   private final String managementPath;
 
  private final PluginInfo[] backupRepositoryPlugins;

   private NodeConfig(String nodeName, Path coreRootDirectory, Path configSetBaseDirectory, String sharedLibDirectory,
                      PluginInfo shardHandlerFactoryConfig, UpdateShardHandlerConfig updateShardHandlerConfig,
                      String coreAdminHandlerClass, String collectionsAdminHandlerClass,
                      String infoHandlerClass, String configSetsHandlerClass,
                      LogWatcherConfig logWatcherConfig, CloudConfig cloudConfig, int coreLoadThreads,
                     int transientCacheSize, boolean useSchemaCache, String managementPath,
                     SolrResourceLoader loader, Properties solrProperties) {
                     int transientCacheSize, boolean useSchemaCache, String managementPath, SolrResourceLoader loader,
                     Properties solrProperties, PluginInfo[] backupRepositoryPlugins) {
     this.nodeName = nodeName;
     this.coreRootDirectory = coreRootDirectory;
     this.configSetBaseDirectory = configSetBaseDirectory;
@@ -83,6 +85,7 @@ public class NodeConfig {
     this.managementPath = managementPath;
     this.loader = loader;
     this.solrProperties = solrProperties;
    this.backupRepositoryPlugins = backupRepositoryPlugins;
 
     if (this.cloudConfig != null && this.coreLoadThreads < 2) {
       throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
@@ -165,6 +168,10 @@ public class NodeConfig {
     return loader;
   }
 
  public PluginInfo[] getBackupRepositoryPlugins() {
    return backupRepositoryPlugins;
  }

   public static class NodeConfigBuilder {
 
     private Path coreRootDirectory;
@@ -183,6 +190,7 @@ public class NodeConfig {
     private boolean useSchemaCache = false;
     private String managementPath;
     private Properties solrProperties = new Properties();
    private PluginInfo[] backupRepositoryPlugins;
 
     private final SolrResourceLoader loader;
     private final String nodeName;
@@ -283,10 +291,16 @@ public class NodeConfig {
       return this;
     }
 
    public NodeConfigBuilder setBackupRepositoryPlugins(PluginInfo[] backupRepositoryPlugins) {
      this.backupRepositoryPlugins = backupRepositoryPlugins;
      return this;
    }

     public NodeConfig build() {
       return new NodeConfig(nodeName, coreRootDirectory, configSetBaseDirectory, sharedLibDirectory, shardHandlerFactoryConfig,
                             updateShardHandlerConfig, coreAdminHandlerClass, collectionsAdminHandlerClass, infoHandlerClass, configSetsHandlerClass,
                            logWatcherConfig, cloudConfig, coreLoadThreads, transientCacheSize, useSchemaCache, managementPath, loader, solrProperties);
                            logWatcherConfig, cloudConfig, coreLoadThreads, transientCacheSize, useSchemaCache, managementPath, loader, solrProperties,
                            backupRepositoryPlugins);
     }
   }
 }
diff --git a/solr/core/src/java/org/apache/solr/core/SolrXmlConfig.java b/solr/core/src/java/org/apache/solr/core/SolrXmlConfig.java
index fedaf56d31f..65b248d6259 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrXmlConfig.java
++ b/solr/core/src/java/org/apache/solr/core/SolrXmlConfig.java
@@ -95,6 +95,7 @@ public class SolrXmlConfig {
     configBuilder.setSolrProperties(loadProperties(config));
     if (cloudConfig != null)
       configBuilder.setCloudConfig(cloudConfig);
    configBuilder.setBackupRepositoryPlugins((getBackupRepositoryPluginInfos(config)));
     return fillSolrSection(configBuilder, entries);
   }
 
@@ -154,6 +155,7 @@ public class SolrXmlConfig {
     assertSingleInstance("solrcloud", config);
     assertSingleInstance("logging", config);
     assertSingleInstance("logging/watcher", config);
    assertSingleInstance("backup", config);
   }
 
   private static void assertSingleInstance(String section, Config config) {
@@ -424,5 +426,15 @@ public class SolrXmlConfig {
     return (node == null) ? null : new PluginInfo(node, "shardHandlerFactory", false, true);
   }
 
  private static PluginInfo[] getBackupRepositoryPluginInfos(Config config) {
    NodeList nodes = (NodeList) config.evaluate("solr/backup/repository", XPathConstants.NODESET);
    if (nodes == null || nodes.getLength() == 0)
      return new PluginInfo[0];
    PluginInfo[] configs = new PluginInfo[nodes.getLength()];
    for (int i = 0; i < nodes.getLength(); i++) {
      configs[i] = new PluginInfo(nodes.item(i), "BackupRepositoryFactory", true, true);
    }
    return configs;
  }
 }
 
diff --git a/solr/core/src/java/org/apache/solr/core/backup/repository/BackupRepository.java b/solr/core/src/java/org/apache/solr/core/backup/repository/BackupRepository.java
new file mode 100644
index 00000000000..f209b874a5f
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/backup/repository/BackupRepository.java
@@ -0,0 +1,166 @@
package org.apache.solr.core.backup.repository;

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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.solr.util.plugin.NamedListInitializedPlugin;

/**
 * This interface defines the functionality required to backup/restore Solr indexes to an arbitrary storage system.
 */
public interface BackupRepository extends NamedListInitializedPlugin, Closeable {
  /**
   * A parameter to specify the name of the backup repository to be used.
   */
  String REPOSITORY_PROPERTY_NAME = "repository";


  /**
   * This enumeration defines the type of a given path.
   */
  enum PathType {
    DIRECTORY, FILE
  }

  /**
   * This method returns the value of the specified configuration property.
   */
  <T> T getConfigProperty(String name);

  /**
   * This method creates a URI using the specified path components (as method arguments).
   *
   * @param pathComponents
   *          The directory (or file-name) to be included in the URI.
   * @return A URI containing absolute path
   */
  URI createURI(String... pathComponents);

  /**
   * This method checks if the specified path exists in this repository.
   *
   * @param path
   *          The path whose existence needs to be checked.
   * @return if the specified path exists in this repository.
   * @throws IOException
   *           in case of errors
   */
  boolean exists(URI path) throws IOException;

  /**
   * This method returns the type of a specified path
   *
   * @param path
   *          The path whose type needs to be checked.
   * @return the {@linkplain PathType} for the specified path
   * @throws IOException
   *           in case of errors
   */
  PathType getPathType(URI path) throws IOException;

  /**
   * This method returns all the entries (files and directories) in the specified directory.
   *
   * @param path
   *          The directory path
   * @return an array of strings, one for each entry in the directory
   * @throws IOException
   *           in case of errors
   */
  String[] listAll(URI path) throws IOException;

  /**
   * This method returns a Lucene input stream reading an existing file.
   *
   * @param dirPath
   *          The parent directory of the file to be read
   * @param fileName
   *          The name of the file to be read
   * @param ctx
   *          the Lucene IO context
   * @return Lucene {@linkplain IndexInput} reference
   * @throws IOException
   *           in case of errors
   */
  IndexInput openInput(URI dirPath, String fileName, IOContext ctx) throws IOException;

  /**
   * This method returns a {@linkplain OutputStream} instance for the specified <code>path</code>
   *
   * @param path
   *          The path for which {@linkplain OutputStream} needs to be created
   * @return {@linkplain OutputStream} instance for the specified <code>path</code>
   * @throws IOException
   *           in case of errors
   */
  OutputStream createOutput(URI path) throws IOException;

  /**
   * This method creates a directory at the specified path.
   *
   * @param path
   *          The path where the directory needs to be created.
   * @throws IOException
   *           in case of errors
   */
  void createDirectory(URI path) throws IOException;

  /**
   * This method deletes a directory at the specified path.
   *
   * @param path
   *          The path referring to the directory to be deleted.
   * @throws IOException
   *           in case of errors
   */
  void deleteDirectory(URI path) throws IOException;

  /**
   * Copy a file from specified <code>sourceDir</code> to the destination repository (i.e. backup).
   *
   * @param sourceDir
   *          The source directory hosting the file to be copied.
   * @param fileName
   *          The name of the file to by copied
   * @param dest
   *          The destination backup location.
   * @throws IOException
   *           in case of errors
   */
  void copyFileFrom(Directory sourceDir, String fileName, URI dest) throws IOException;

  /**
   * Copy a file from specified <code>sourceRepo</code> to the destination directory (i.e. restore).
   *
   * @param sourceRepo
   *          The source URI hosting the file to be copied.
   * @param fileName
   *          The name of the file to by copied
   * @param dest
   *          The destination where the file should be copied.
   * @throws IOException
   *           in case of errors.
   */
  void copyFileTo(URI sourceRepo, String fileName, Directory dest) throws IOException;
}
diff --git a/solr/core/src/java/org/apache/solr/core/backup/repository/BackupRepositoryFactory.java b/solr/core/src/java/org/apache/solr/core/backup/repository/BackupRepositoryFactory.java
new file mode 100644
index 00000000000..d03587489e2
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/backup/repository/BackupRepositoryFactory.java
@@ -0,0 +1,89 @@
package org.apache.solr.core.backup.repository;

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

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class BackupRepositoryFactory {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String,PluginInfo> backupRepoPluginByName = new HashMap<>();
  private PluginInfo defaultBackupRepoPlugin = null;

  public BackupRepositoryFactory(PluginInfo[] backupRepoPlugins) {
    if (backupRepoPlugins != null) {
      for (int i = 0; i < backupRepoPlugins.length; i++) {
        String name = backupRepoPlugins[i].name;
        boolean isDefault = backupRepoPlugins[i].isDefault();

        if (backupRepoPluginByName.containsKey(name)) {
          throw new SolrException(ErrorCode.SERVER_ERROR, "Duplicate backup repository with name " + name);
        }
        if (isDefault) {
          if (this.defaultBackupRepoPlugin != null) {
            throw new SolrException(ErrorCode.SERVER_ERROR, "More than one backup repository is configured as default");
          }
          this.defaultBackupRepoPlugin = backupRepoPlugins[i];
        }
        backupRepoPluginByName.put(name, backupRepoPlugins[i]);
        LOG.info("Added backup repository with configuration params {}", backupRepoPlugins[i]);
      }
      if (backupRepoPlugins.length == 1) {
        this.defaultBackupRepoPlugin = backupRepoPlugins[0];
      }

      if (this.defaultBackupRepoPlugin != null) {
        LOG.info("Default configuration for backup repository is with configuration params {}",
            defaultBackupRepoPlugin);
      }
    }
  }

  public BackupRepository newInstance(SolrResourceLoader loader, String name) {
    Preconditions.checkNotNull(loader);
    Preconditions.checkNotNull(name);
    PluginInfo repo = Preconditions.checkNotNull(backupRepoPluginByName.get(name),
        "Could not find a backup repository with name " + name);

    BackupRepository result = loader.newInstance(repo.className, BackupRepository.class);
    result.init(repo.initArgs);
    return result;
  }

  public BackupRepository newInstance(SolrResourceLoader loader) {
    if (defaultBackupRepoPlugin != null) {
      return newInstance(loader, defaultBackupRepoPlugin.name);
    }

    LocalFileSystemRepository repo = new LocalFileSystemRepository();
    repo.init(new NamedList<>());
    return repo;
  }
}
diff --git a/solr/core/src/java/org/apache/solr/core/backup/repository/HdfsBackupRepository.java b/solr/core/src/java/org/apache/solr/core/backup/repository/HdfsBackupRepository.java
new file mode 100644
index 00000000000..596c2713e37
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/backup/repository/HdfsBackupRepository.java
@@ -0,0 +1,159 @@
package org.apache.solr.core.backup.repository;

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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.NoLockFactory;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.DirectoryFactory;
import org.apache.solr.core.HdfsDirectoryFactory;
import org.apache.solr.store.hdfs.HdfsDirectory;
import org.apache.solr.store.hdfs.HdfsDirectory.HdfsIndexInput;

import com.google.common.base.Preconditions;

public class HdfsBackupRepository implements BackupRepository {
  private HdfsDirectoryFactory factory;
  private Configuration hdfsConfig = null;
  private FileSystem fileSystem = null;
  private Path baseHdfsPath = null;
  private NamedList config = null;

  @SuppressWarnings("rawtypes")
  @Override
  public void init(NamedList args) {
    this.config = args;

    // We don't really need this factory instance. But we want to initialize it here to
    // make sure that all HDFS related initialization is at one place (and not duplicated here).
    factory = new HdfsDirectoryFactory();
    factory.init(args);
    this.hdfsConfig = factory.getConf();

    String hdfsSolrHome = (String) Preconditions.checkNotNull(args.get(HdfsDirectoryFactory.HDFS_HOME),
        "Please specify " + HdfsDirectoryFactory.HDFS_HOME + " property.");
    Path path = new Path(hdfsSolrHome);
    while (path != null) { // Compute the path of root file-system (without requiring an additional system property).
      baseHdfsPath = path;
      path = path.getParent();
    }

    try {
      this.fileSystem = FileSystem.get(this.baseHdfsPath.toUri(), this.hdfsConfig);
    } catch (IOException e) {
      throw new SolrException(ErrorCode.SERVER_ERROR, e);
    }
  }

  public void close() throws IOException {
    if (this.fileSystem != null) {
      this.fileSystem.close();
    }
    if (this.factory != null) {
      this.factory.close();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getConfigProperty(String name) {
    return (T) this.config.get(name);
  }

  @Override
  public URI createURI(String... pathComponents) {
    Path result = baseHdfsPath;
    for (String p : pathComponents) {
      result = new Path(result, p);
    }
    return result.toUri();
  }

  @Override
  public boolean exists(URI path) throws IOException {
    return this.fileSystem.exists(new Path(path));
  }

  @Override
  public PathType getPathType(URI path) throws IOException {
    return this.fileSystem.isDirectory(new Path(path)) ? PathType.DIRECTORY : PathType.FILE;
  }

  @Override
  public String[] listAll(URI path) throws IOException {
    FileStatus[] status = this.fileSystem.listStatus(new Path(path));
    String[] result = new String[status.length];
    for (int i = 0; i < status.length; i++) {
      result[i] = status[i].getPath().getName();
    }
    return result;
  }

  @Override
  public IndexInput openInput(URI dirPath, String fileName, IOContext ctx) throws IOException {
    Path p = new Path(new Path(dirPath), fileName);
    return new HdfsIndexInput(fileName, this.fileSystem, p, HdfsDirectory.DEFAULT_BUFFER_SIZE);
  }

  @Override
  public OutputStream createOutput(URI path) throws IOException {
    return this.fileSystem.create(new Path(path));
  }

  @Override
  public void createDirectory(URI path) throws IOException {
    if (!this.fileSystem.mkdirs(new Path(path))) {
      throw new IOException("Unable to create a directory at following location " + path);
    }
  }

  @Override
  public void deleteDirectory(URI path) throws IOException {
    if (!this.fileSystem.delete(new Path(path), true)) {
      throw new IOException("Unable to delete a directory at following location " + path);
    }
  }

  @Override
  public void copyFileFrom(Directory sourceDir, String fileName, URI dest) throws IOException {
    try (HdfsDirectory dir = new HdfsDirectory(new Path(dest), NoLockFactory.INSTANCE,
        hdfsConfig, HdfsDirectory.DEFAULT_BUFFER_SIZE)) {
      dir.copyFrom(sourceDir, fileName, fileName, DirectoryFactory.IOCONTEXT_NO_CACHE);
    }
  }

  @Override
  public void copyFileTo(URI sourceRepo, String fileName, Directory dest) throws IOException {
    try (HdfsDirectory dir = new HdfsDirectory(new Path(sourceRepo), NoLockFactory.INSTANCE,
        hdfsConfig, HdfsDirectory.DEFAULT_BUFFER_SIZE)) {
      dest.copyFrom(dir, fileName, fileName, DirectoryFactory.IOCONTEXT_NO_CACHE);
    }
  }
}
diff --git a/solr/core/src/java/org/apache/solr/core/backup/repository/LocalFileSystemRepository.java b/solr/core/src/java/org/apache/solr/core/backup/repository/LocalFileSystemRepository.java
new file mode 100644
index 00000000000..bb75a9e2680
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/backup/repository/LocalFileSystemRepository.java
@@ -0,0 +1,136 @@
package org.apache.solr.core.backup.repository;

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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.DirectoryFactory;

import com.google.common.base.Preconditions;

/**
 * A concrete implementation of {@linkplain BackupRepository} interface supporting backup/restore of Solr indexes to a
 * local file-system. (Note - This can even be used for a shared file-system if it is exposed via a local file-system
 * interface e.g. NFS).
 */
public class LocalFileSystemRepository implements BackupRepository {
  private NamedList config = null;

  @Override
  public void init(NamedList args) {
    this.config = args;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getConfigProperty(String name) {
    return (T) this.config.get(name);
  }

  @Override
  public URI createURI(String... pathComponents) {
    Preconditions.checkArgument(pathComponents.length > 0);
    Path result = Paths.get(pathComponents[0]);
    for (int i = 1; i < pathComponents.length; i++) {
      result = result.resolve(pathComponents[i]);
    }
    return result.toUri();
  }

  @Override
  public void createDirectory(URI path) throws IOException {
    Files.createDirectory(Paths.get(path));
  }

  @Override
  public void deleteDirectory(URI path) throws IOException {
    Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  @Override
  public boolean exists(URI path) throws IOException {
    return Files.exists(Paths.get(path));
  }

  @Override
  public IndexInput openInput(URI dirPath, String fileName, IOContext ctx) throws IOException {
    try (FSDirectory dir = new SimpleFSDirectory(Paths.get(dirPath), NoLockFactory.INSTANCE)) {
      return dir.openInput(fileName, ctx);
    }
  }

  @Override
  public OutputStream createOutput(URI path) throws IOException {
    return Files.newOutputStream(Paths.get(path));
  }

  @Override
  public String[] listAll(URI dirPath) throws IOException {
    try (FSDirectory dir = new SimpleFSDirectory(Paths.get(dirPath), NoLockFactory.INSTANCE)) {
      return dir.listAll();
    }
  }

  @Override
  public PathType getPathType(URI path) throws IOException {
    return Files.isDirectory(Paths.get(path)) ? PathType.DIRECTORY : PathType.FILE;
  }

  @Override
  public void copyFileFrom(Directory sourceDir, String fileName, URI dest) throws IOException {
    try (FSDirectory dir = new SimpleFSDirectory(Paths.get(dest), NoLockFactory.INSTANCE)) {
      dir.copyFrom(sourceDir, fileName, fileName, DirectoryFactory.IOCONTEXT_NO_CACHE);
    }
  }

  @Override
  public void copyFileTo(URI sourceDir, String fileName, Directory dest) throws IOException {
    try (FSDirectory dir = new SimpleFSDirectory(Paths.get(sourceDir), NoLockFactory.INSTANCE)) {
      dest.copyFrom(dir, fileName, fileName, DirectoryFactory.IOCONTEXT_NO_CACHE);
    }
  }

  @Override
  public void close() throws IOException {}
}
diff --git a/solr/core/src/java/org/apache/solr/core/backup/repository/package-info.java b/solr/core/src/java/org/apache/solr/core/backup/repository/package-info.java
new file mode 100644
index 00000000000..fb3cfd563b9
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/backup/repository/package-info.java
@@ -0,0 +1,23 @@
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


/**
* {@link org.apache.solr.core.backup.repository.BackupRepository} Providing backup/restore
* repository interfaces to plug different storage systems
*/
package org.apache.solr.core.backup.repository;
\ No newline at end of file
diff --git a/solr/core/src/java/org/apache/solr/handler/OldBackupDirectory.java b/solr/core/src/java/org/apache/solr/handler/OldBackupDirectory.java
index 7fd0fec8b8d..2b19116c926 100644
-- a/solr/core/src/java/org/apache/solr/handler/OldBackupDirectory.java
++ b/solr/core/src/java/org/apache/solr/handler/OldBackupDirectory.java
@@ -16,34 +16,55 @@
  */
 package org.apache.solr.handler;
 
import java.io.File;
import java.net.URI;
import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.Locale;
import java.util.Optional;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
import com.google.common.base.Preconditions;

 class OldBackupDirectory implements Comparable<OldBackupDirectory> {
  File dir;
  Date timestamp;
  private  final Pattern dirNamePattern = Pattern.compile("^snapshot[.](.*)$");

  OldBackupDirectory(File dir) {
    if(dir.isDirectory()) {
      Matcher m = dirNamePattern.matcher(dir.getName());
      if(m.find()) {
        try {
          this.dir = dir;
          this.timestamp = new SimpleDateFormat(SnapShooter.DATE_FMT, Locale.ROOT).parse(m.group(1));
        } catch(Exception e) {
          this.dir = null;
          this.timestamp = null;
        }
  private static final Pattern dirNamePattern = Pattern.compile("^snapshot[.](.*)$");

  private URI basePath;
  private String dirName;
  private Optional<Date> timestamp;

  public OldBackupDirectory(URI basePath, String dirName) {
    this.dirName = Preconditions.checkNotNull(dirName);
    this.basePath = Preconditions.checkNotNull(basePath);
    Matcher m = dirNamePattern.matcher(dirName);
    if (m.find()) {
      try {
        this.timestamp = Optional.of(new SimpleDateFormat(SnapShooter.DATE_FMT, Locale.ROOT).parse(m.group(1)));
      } catch (ParseException e) {
        this.timestamp = Optional.empty();
       }
     }
   }

  public URI getPath() {
    return this.basePath.resolve(dirName);
  }

  public String getDirName() {
    return dirName;
  }

  public Optional<Date> getTimestamp() {
    return timestamp;
  }

   @Override
   public int compareTo(OldBackupDirectory that) {
    return that.timestamp.compareTo(this.timestamp);
    if(this.timestamp.isPresent() && that.timestamp.isPresent()) {
      return that.timestamp.get().compareTo(this.timestamp.get());
    }
    // Use absolute value of path in case the time-stamp is missing on either side.
    return that.getPath().compareTo(this.getPath());
   }
 }
diff --git a/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java b/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java
index 9de4a781183..0e6960c68a8 100644
-- a/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java
@@ -24,6 +24,7 @@ import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.lang.invoke.MethodHandles;
import java.net.URI;
 import java.nio.ByteBuffer;
 import java.nio.channels.FileChannel;
 import java.nio.charset.StandardCharsets;
@@ -66,6 +67,7 @@ import org.apache.lucene.store.IndexInput;
 import org.apache.lucene.store.RateLimiter;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
@@ -76,11 +78,15 @@ import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.common.util.SuppressForbidden;
 import org.apache.solr.core.CloseHook;
import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.DirectoryFactory.DirContext;
 import org.apache.solr.core.IndexDeletionPolicyWrapper;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.core.SolrDeletionPolicy;
 import org.apache.solr.core.SolrEventListener;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.backup.repository.BackupRepository;
import org.apache.solr.core.backup.repository.LocalFileSystemRepository;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.response.SolrQueryResponse;
 import org.apache.solr.search.SolrIndexSearcher;
@@ -407,7 +413,7 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
     return indexFetchLock.isLocked();
   }
 
  private void restore(SolrParams params, SolrQueryResponse rsp, SolrQueryRequest req) {
  private void restore(SolrParams params, SolrQueryResponse rsp, SolrQueryRequest req) throws IOException {
     if (restoreFuture != null && !restoreFuture.isDone()) {
       throw new SolrException(ErrorCode.BAD_REQUEST, "Restore in progress. Cannot run multiple restore operations" +
           "for the same core");
@@ -415,6 +421,22 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
     String name = params.get(NAME);
     String location = params.get(LOCATION);
 
    String repoName = params.get(BackupRepository.REPOSITORY_PROPERTY_NAME);
    CoreContainer cc = core.getCoreDescriptor().getCoreContainer();
    SolrResourceLoader rl = cc.getResourceLoader();
    BackupRepository repo = null;
    if(repoName != null) {
      repo = cc.getBackupRepoFactory().newInstance(rl, repoName);
      if (location == null) {
        location = repo.getConfigProperty(ZkStateReader.BACKUP_LOCATION);
        if(location == null) {
          throw new IllegalArgumentException("location is required");
        }
      }
    } else {
      repo = new LocalFileSystemRepository();
    }

     //If location is not provided then assume that the restore index is present inside the data directory.
     if (location == null) {
       location = core.getDataDir();
@@ -423,11 +445,12 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
     //If name is not provided then look for the last unnamed( the ones with the snapshot.timestamp format)
     //snapshot folder since we allow snapshots to be taken without providing a name. Pick the latest timestamp.
     if (name == null) {
      File[] files = new File(location).listFiles();
      URI basePath = repo.createURI(location);
      String[] filePaths = repo.listAll(basePath);
       List<OldBackupDirectory> dirs = new ArrayList<>();
      for (File f : files) {
        OldBackupDirectory obd = new OldBackupDirectory(f);
        if (obd.dir != null) {
      for (String f : filePaths) {
        OldBackupDirectory obd = new OldBackupDirectory(basePath, f);
        if (obd.getTimestamp().isPresent()) {
           dirs.add(obd);
         }
       }
@@ -435,13 +458,13 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
       if (dirs.size() == 0) {
         throw new SolrException(ErrorCode.BAD_REQUEST, "No backup name specified and none found in " + core.getDataDir());
       }
      name = dirs.get(0).dir.getName();
      name = dirs.get(0).getDirName();
     } else {
       //"snapshot." is prefixed by snapshooter
       name = "snapshot." + name;
     }
 
    RestoreCore restoreCore = new RestoreCore(core, location, name);
    RestoreCore restoreCore = new RestoreCore(repo, core, location, name);
     try {
       MDC.put("RestoreCore.core", core.getName());
       MDC.put("RestoreCore.backupLocation", location);
@@ -504,8 +527,30 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
         indexCommit = req.getSearcher().getIndexReader().getIndexCommit();
       }
 
      String location = params.get(ZkStateReader.BACKUP_LOCATION);
      String repoName = params.get(BackupRepository.REPOSITORY_PROPERTY_NAME);
      CoreContainer cc = core.getCoreDescriptor().getCoreContainer();
      SolrResourceLoader rl = cc.getResourceLoader();
      BackupRepository repo = null;
      if(repoName != null) {
        repo = cc.getBackupRepoFactory().newInstance(rl, repoName);
        if (location == null) {
          location = repo.getConfigProperty(ZkStateReader.BACKUP_LOCATION);
          if(location == null) {
            throw new IllegalArgumentException("location is required");
          }
        }
      } else {
        repo = new LocalFileSystemRepository();
        if (location == null) {
          location = core.getDataDir();
        } else {
          location = core.getCoreDescriptor().getInstanceDir().resolve(location).normalize().toString();
        }
      }

       // small race here before the commit point is saved
      SnapShooter snapShooter = new SnapShooter(core, params.get("location"), params.get(NAME));
      SnapShooter snapShooter = new SnapShooter(repo, core, location, params.get(NAME));
       snapShooter.validateCreateSnapshot();
       snapShooter.createSnapAsync(indexCommit, numberToKeep, (nl) -> snapShootDetails = nl);
 
diff --git a/solr/core/src/java/org/apache/solr/handler/RestoreCore.java b/solr/core/src/java/org/apache/solr/handler/RestoreCore.java
index 34109c63e76..d3c98fac432 100644
-- a/solr/core/src/java/org/apache/solr/handler/RestoreCore.java
++ b/solr/core/src/java/org/apache/solr/handler/RestoreCore.java
@@ -17,8 +17,7 @@
 package org.apache.solr.handler;
 
 import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.Locale;
@@ -27,12 +26,12 @@ import java.util.concurrent.Future;
 
 import org.apache.lucene.codecs.CodecUtil;
 import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
 import org.apache.lucene.store.IOContext;
 import org.apache.lucene.store.IndexInput;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.core.DirectoryFactory;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.backup.repository.BackupRepository;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -43,8 +42,10 @@ public class RestoreCore implements Callable<Boolean> {
   private final String backupName;
   private final String backupLocation;
   private final SolrCore core;
  private final BackupRepository backupRepo;
 
  public RestoreCore(SolrCore core, String location, String name) {
  public RestoreCore(BackupRepository backupRepo, SolrCore core, String location, String name) {
    this.backupRepo = backupRepo;
     this.core = core;
     this.backupLocation = location;
     this.backupName = name;
@@ -57,14 +58,14 @@ public class RestoreCore implements Callable<Boolean> {
 
   public boolean doRestore() throws Exception {
 
    Path backupPath = Paths.get(backupLocation).resolve(backupName);
    URI backupPath = backupRepo.createURI(backupLocation, backupName);
     SimpleDateFormat dateFormat = new SimpleDateFormat(SnapShooter.DATE_FMT, Locale.ROOT);
     String restoreIndexName = "restore." + dateFormat.format(new Date());
     String restoreIndexPath = core.getDataDir() + restoreIndexName;
 
     Directory restoreIndexDir = null;
     Directory indexDir = null;
    try (Directory backupDir = FSDirectory.open(backupPath)) {
    try {
 
       restoreIndexDir = core.getDirectoryFactory().get(restoreIndexPath,
           DirectoryFactory.DirContext.DEFAULT, core.getSolrConfig().indexConfig.lockType);
@@ -74,10 +75,10 @@ public class RestoreCore implements Callable<Boolean> {
           DirectoryFactory.DirContext.DEFAULT, core.getSolrConfig().indexConfig.lockType);
 
       //Move all files from backupDir to restoreIndexDir
      for (String filename : backupDir.listAll()) {
      for (String filename : backupRepo.listAll(backupPath)) {
         checkInterrupted();
         log.info("Copying file {} to restore directory ", filename);
        try (IndexInput indexInput = backupDir.openInput(filename, IOContext.READONCE)) {
        try (IndexInput indexInput = backupRepo.openInput(backupPath, filename, IOContext.READONCE)) {
           Long checksum = null;
           try {
             checksum = CodecUtil.retrieveChecksum(indexInput);
@@ -88,12 +89,13 @@ public class RestoreCore implements Callable<Boolean> {
           IndexFetcher.CompareResult compareResult = IndexFetcher.compareFile(indexDir, filename, length, checksum);
           if (!compareResult.equal ||
               (IndexFetcher.filesToAlwaysDownloadIfNoChecksums(filename, length, compareResult))) {
            restoreIndexDir.copyFrom(backupDir, filename, filename, IOContext.READONCE);
            backupRepo.copyFileTo(backupPath, filename, restoreIndexDir);
           } else {
             //prefer local copy
             restoreIndexDir.copyFrom(indexDir, filename, filename, IOContext.READONCE);
           }
         } catch (Exception e) {
          log.warn("Exception while restoring the backup index ", e);
           throw new SolrException(SolrException.ErrorCode.UNKNOWN, "Exception while restoring the backup index", e);
         }
       }
@@ -108,7 +110,7 @@ public class RestoreCore implements Callable<Boolean> {
         log.info("Successfully restored to the backup index");
       } catch (Exception e) {
         //Rollback to the old index directory. Delete the restore index directory and mark the restore as failed.
        log.warn("Could not switch to restored index. Rolling back to the current index");
        log.warn("Could not switch to restored index. Rolling back to the current index", e);
         Directory dir = null;
         try {
           dir = core.getDirectoryFactory().get(core.getDataDir(), DirectoryFactory.DirContext.META_DATA,
diff --git a/solr/core/src/java/org/apache/solr/handler/SnapShooter.java b/solr/core/src/java/org/apache/solr/handler/SnapShooter.java
index 2365fca17b9..cc3f69efc0b 100644
-- a/solr/core/src/java/org/apache/solr/handler/SnapShooter.java
++ b/solr/core/src/java/org/apache/solr/handler/SnapShooter.java
@@ -16,11 +16,9 @@
  */
 package org.apache.solr.handler;
 
import java.io.File;
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collection;
@@ -32,20 +30,21 @@ import java.util.function.Consumer;
 
 import org.apache.lucene.index.IndexCommit;
 import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSDirectory;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.DirectoryFactory;
 import org.apache.solr.core.DirectoryFactory.DirContext;
 import org.apache.solr.core.IndexDeletionPolicyWrapper;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.backup.repository.BackupRepository;
import org.apache.solr.core.backup.repository.BackupRepository.PathType;
import org.apache.solr.core.backup.repository.LocalFileSystemRepository;
 import org.apache.solr.search.SolrIndexSearcher;
 import org.apache.solr.util.RefCounted;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import com.google.common.base.Preconditions;

 /**
  * <p> Provides functionality equivalent to the snapshooter script </p>
  * This is no longer used in standard replication.
@@ -55,48 +54,76 @@ import org.slf4j.LoggerFactory;
  */
 public class SnapShooter {
   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private String snapDir = null;
   private SolrCore solrCore;
   private String snapshotName = null;
   private String directoryName = null;
  private File snapShotDir = null;
  //TODO update to NIO Path API
  private URI baseSnapDirPath = null;
  private URI snapshotDirPath = null;
  private BackupRepository backupRepo = null;
 
  @Deprecated
   public SnapShooter(SolrCore core, String location, String snapshotName) {
    solrCore = core;
    String snapDirStr = null;
    // Note - This logic is only applicable to the usecase where a shared file-system is exposed via
    // local file-system interface (primarily for backwards compatibility). For other use-cases, users
    // will be required to specify "location" where the backup should be stored.
     if (location == null) {
      snapDir = core.getDataDir();
    }
    else  {
      snapDir = core.getCoreDescriptor().getInstanceDir().resolve(location).normalize().toString();
      snapDirStr = core.getDataDir();
    } else {
      snapDirStr = core.getCoreDescriptor().getInstanceDir().resolve(location).normalize().toString();
     }
    this.snapshotName = snapshotName;
    initialize(new LocalFileSystemRepository(), core, snapDirStr, snapshotName);
  }

  public SnapShooter(BackupRepository backupRepo, SolrCore core, String location, String snapshotName) {
    initialize(backupRepo, core, location, snapshotName);
  }
 
    if(snapshotName != null) {
  private void initialize(BackupRepository backupRepo, SolrCore core, String location, String snapshotName) {
    this.solrCore = Preconditions.checkNotNull(core);
    this.backupRepo = Preconditions.checkNotNull(backupRepo);
    this.baseSnapDirPath = backupRepo.createURI(Preconditions.checkNotNull(location)).normalize();
    this.snapshotName = snapshotName;
    if (snapshotName != null) {
       directoryName = "snapshot." + snapshotName;
     } else {
       SimpleDateFormat fmt = new SimpleDateFormat(DATE_FMT, Locale.ROOT);
       directoryName = "snapshot." + fmt.format(new Date());
     }
    this.snapshotDirPath = backupRepo.createURI(location, directoryName);
   }
 
  /** Gets the parent directory of the snapshots.  This is the {@code location} given in the constructor after
   * being resolved against the core instance dir. */
  public Path getLocation() {
    return Paths.get(snapDir);
  public BackupRepository getBackupRepository() {
    return backupRepo;
  }

  /**
   * Gets the parent directory of the snapshots. This is the {@code location}
   * given in the constructor.
   */
  public URI getLocation() {
    return this.baseSnapDirPath;
   }
 
   public void validateDeleteSnapshot() {
    Preconditions.checkNotNull(this.snapshotName);

     boolean dirFound = false;
    File[] files = new File(snapDir).listFiles();
    for(File f : files) {
      if (f.getName().equals("snapshot." + snapshotName)) {
        dirFound = true;
        break;
    String[] paths;
    try {
      paths = backupRepo.listAll(baseSnapDirPath);
      for (String path : paths) {
        if (path.equals(this.directoryName)
            && backupRepo.getPathType(baseSnapDirPath.resolve(path)) == PathType.DIRECTORY) {
          dirFound = true;
          break;
        }
       }
    }
    if(dirFound == false) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Snapshot cannot be found in directory: " + snapDir);
      if(dirFound == false) {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Snapshot " + snapshotName + " cannot be found in directory: " + baseSnapDirPath);
      }
    } catch (IOException e) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to find snapshot " + snapshotName + " in directory: " + baseSnapDirPath, e);
     }
   }
 
@@ -110,14 +137,16 @@ public class SnapShooter {
   }
 
   public void validateCreateSnapshot() throws IOException {
    snapShotDir = new File(snapDir, directoryName);
    if (snapShotDir.exists()) {
    // Note - Removed the current behavior of creating the directory hierarchy.
    // Do we really need to provide this support?
    if (!backupRepo.exists(baseSnapDirPath)) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
          "Snapshot directory already exists: " + snapShotDir.getAbsolutePath());
          " Directory does not exist: " + snapshotDirPath);
     }
    if (!snapShotDir.mkdirs()) { // note: TODO reconsider mkdirs vs mkdir

    if (backupRepo.exists(snapshotDirPath)) {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
          "Unable to create snapshot directory: " + snapShotDir.getAbsolutePath());
          "Snapshot directory already exists: " + snapshotDirPath);
     }
   }
 
@@ -155,7 +184,11 @@ public class SnapShooter {
           solrCore.getDeletionPolicy().releaseCommitPoint(indexCommit.getGeneration());
         }
         if (snapshotName == null) {
          deleteOldBackups(numberToKeep);
          try {
            deleteOldBackups(numberToKeep);
          } catch (IOException e) {
            LOG.warn("Unable to delete old snapshots ", e);
          }
         }
       }
     }.start();
@@ -163,7 +196,7 @@ public class SnapShooter {
 
   // note: remember to reserve the indexCommit first so it won't get deleted concurrently
   protected NamedList createSnapshot(final IndexCommit indexCommit) throws Exception {
    LOG.info("Creating backup snapshot " + (snapshotName == null ? "<not named>" : snapshotName) + " at " + snapDir);
    LOG.info("Creating backup snapshot " + (snapshotName == null ? "<not named>" : snapshotName) + " at " + baseSnapDirPath);
     boolean success = false;
     try {
       NamedList<Object> details = new NamedList<>();
@@ -172,7 +205,9 @@ public class SnapShooter {
       Collection<String> files = indexCommit.getFileNames();
       Directory dir = solrCore.getDirectoryFactory().get(solrCore.getIndexDir(), DirContext.DEFAULT, solrCore.getSolrConfig().indexConfig.lockType);
       try {
        copyFiles(dir, files, snapShotDir);
        for(String fileName : files) {
          backupRepo.copyFileFrom(dir, fileName, snapshotDirPath);
        }
       } finally {
         solrCore.getDirectoryFactory().release(dir);
       }
@@ -182,34 +217,35 @@ public class SnapShooter {
       details.add("snapshotCompletedAt", new Date().toString());//bad; should be Instant.now().toString()
       details.add("snapshotName", snapshotName);
       LOG.info("Done creating backup snapshot: " + (snapshotName == null ? "<not named>" : snapshotName) +
          " at " + snapDir);
          " at " + baseSnapDirPath);
       success = true;
       return details;
     } finally {
       if (!success) {
        IndexFetcher.delTree(snapShotDir);
        backupRepo.deleteDirectory(snapshotDirPath);
       }
     }
   }
 
  private void deleteOldBackups(int numberToKeep) {
    File[] files = new File(snapDir).listFiles();
  private void deleteOldBackups(int numberToKeep) throws IOException {
    String[] paths = backupRepo.listAll(baseSnapDirPath);
     List<OldBackupDirectory> dirs = new ArrayList<>();
    for (File f : files) {
      OldBackupDirectory obd = new OldBackupDirectory(f);
      if(obd.dir != null) {
        dirs.add(obd);
    for (String f : paths) {
      if (backupRepo.getPathType(baseSnapDirPath.resolve(f)) == PathType.DIRECTORY) {
        OldBackupDirectory obd = new OldBackupDirectory(baseSnapDirPath, f);
        if (obd.getTimestamp().isPresent()) {
          dirs.add(obd);
        }
       }
     }
     if (numberToKeep > dirs.size() -1) {
       return;
     }

     Collections.sort(dirs);
     int i=1;
     for (OldBackupDirectory dir : dirs) {
       if (i++ > numberToKeep) {
        IndexFetcher.delTree(dir.dir);
        backupRepo.deleteDirectory(dir.getPath());
       }
     }
   }
@@ -218,29 +254,22 @@ public class SnapShooter {
     LOG.info("Deleting snapshot: " + snapshotName);
 
     NamedList<Object> details = new NamedList<>();
    boolean isSuccess;
    File f = new File(snapDir, "snapshot." + snapshotName);
    isSuccess = IndexFetcher.delTree(f);
 
    if(isSuccess) {
    try {
      URI path = baseSnapDirPath.resolve("snapshot." + snapshotName);
      backupRepo.deleteDirectory(path);

       details.add("status", "success");
       details.add("snapshotDeletedAt", new Date().toString());
    } else {

    } catch (IOException e) {
       details.add("status", "Unable to delete snapshot: " + snapshotName);
      LOG.warn("Unable to delete snapshot: " + snapshotName);
      LOG.warn("Unable to delete snapshot: " + snapshotName, e);
     }

     replicationHandler.snapShootDetails = details;
   }
 
   public static final String DATE_FMT = "yyyyMMddHHmmssSSS";
 

  private static void copyFiles(Directory sourceDir, Collection<String> files, File destDir) throws IOException {
    try (FSDirectory dir = new SimpleFSDirectory(destDir.toPath(), NoLockFactory.INSTANCE)) {
      for (String indexFile : files) {
        dir.copyFrom(sourceDir, indexFile, indexFile, DirectoryFactory.IOCONTEXT_NO_CACHE);
      }
    }
  }

 }
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
index 6d501a1cb5a..6acd86a3acb 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
@@ -798,7 +798,7 @@ public class CollectionsHandler extends RequestHandlerBase implements Permission
           throw new SolrException(ErrorCode.BAD_REQUEST, "Collection '" + collectionName + "' does not exist, no action taken.");
         }
 
        String location = req.getParams().get("location");
        String location = req.getParams().get(ZkStateReader.BACKUP_LOCATION);
         if (location == null) {
           location = h.coreContainer.getZkController().getZkStateReader().getClusterProperty("location", (String) null);
         }
@@ -822,7 +822,7 @@ public class CollectionsHandler extends RequestHandlerBase implements Permission
           throw new SolrException(ErrorCode.BAD_REQUEST, "Collection '" + collectionName + "' exists, no action taken.");
         }
 
        String location = req.getParams().get("location");
        String location = req.getParams().get(ZkStateReader.BACKUP_LOCATION);
         if (location == null) {
           location = h.coreContainer.getZkController().getZkStateReader().getClusterProperty("location", (String) null);
         }
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminOperation.java b/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminOperation.java
index 3fdf3efde32..3c52beace86 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminOperation.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminOperation.java
@@ -18,7 +18,6 @@ package org.apache.solr.handler.admin;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
 import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Arrays;
@@ -60,6 +59,7 @@ import org.apache.solr.core.CoreDescriptor;
 import org.apache.solr.core.DirectoryFactory;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.backup.repository.BackupRepository;
 import org.apache.solr.handler.RestoreCore;
 import org.apache.solr.handler.SnapShooter;
 import org.apache.solr.request.LocalSolrQueryRequest;
@@ -858,20 +858,32 @@ enum CoreAdminOperation {
         throw new IllegalArgumentException(CoreAdminParams.NAME + " is required");
       }
 
      String location = params.get("location");
      SolrResourceLoader loader = callInfo.handler.coreContainer.getResourceLoader();
      BackupRepository repository;
      String repoName = params.get(BackupRepository.REPOSITORY_PROPERTY_NAME);
      if(repoName != null) {
        repository = callInfo.handler.coreContainer.getBackupRepoFactory().newInstance(loader, repoName);
      } else { // Fetch the default.
        repository = callInfo.handler.coreContainer.getBackupRepoFactory().newInstance(loader);
      }

      String location = params.get(ZkStateReader.BACKUP_LOCATION);
       if (location == null) {
        throw new IllegalArgumentException("location is required");
        location = repository.getConfigProperty(ZkStateReader.BACKUP_LOCATION);
        if (location == null) {
          throw new IllegalArgumentException("location is required");
        }
       }
 
       try (SolrCore core = callInfo.handler.coreContainer.getCore(cname)) {
        SnapShooter snapShooter = new SnapShooter(core, location, name);
        SnapShooter snapShooter = new SnapShooter(repository, core, location, name);
         // validateCreateSnapshot will create parent dirs instead of throw; that choice is dubious.
         //  But we want to throw. One reason is that
         //  this dir really should, in fact must, already exist here if triggered via a collection backup on a shared
         //  file system. Otherwise, perhaps the FS location isn't shared -- we want an error.
        if (!Files.exists(snapShooter.getLocation())) {
        if (!snapShooter.getBackupRepository().exists(snapShooter.getLocation())) {
           throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
              "Directory to contain snapshots doesn't exist: " + snapShooter.getLocation().toAbsolutePath());
              "Directory to contain snapshots doesn't exist: " + snapShooter.getLocation());
         }
         snapShooter.validateCreateSnapshot();
         snapShooter.createSnapshot();
@@ -900,13 +912,25 @@ enum CoreAdminOperation {
         throw new IllegalArgumentException(CoreAdminParams.NAME + " is required");
       }
 
      String location = params.get("location");
      SolrResourceLoader loader = callInfo.handler.coreContainer.getResourceLoader();
      BackupRepository repository;
      String repoName = params.get(BackupRepository.REPOSITORY_PROPERTY_NAME);
      if(repoName != null) {
        repository = callInfo.handler.coreContainer.getBackupRepoFactory().newInstance(loader, repoName);
      } else { // Fetch the default.
        repository = callInfo.handler.coreContainer.getBackupRepoFactory().newInstance(loader);
      }

      String location = params.get(ZkStateReader.BACKUP_LOCATION);
       if (location == null) {
        throw new IllegalArgumentException("location is required");
        location = repository.getConfigProperty(ZkStateReader.BACKUP_LOCATION);
        if (location == null) {
          throw new IllegalArgumentException("location is required");
        }
       }
 
       try (SolrCore core = callInfo.handler.coreContainer.getCore(cname)) {
        RestoreCore restoreCore = new RestoreCore(core, location, name);
        RestoreCore restoreCore = new RestoreCore(repository, core, location, name);
         boolean success = restoreCore.doRestore();
         if (!success) {
           throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Failed to restore core=" + core.getName());
diff --git a/solr/core/src/java/org/apache/solr/store/hdfs/HdfsDirectory.java b/solr/core/src/java/org/apache/solr/store/hdfs/HdfsDirectory.java
index 9a8b36ca933..0a2569210bb 100644
-- a/solr/core/src/java/org/apache/solr/store/hdfs/HdfsDirectory.java
++ b/solr/core/src/java/org/apache/solr/store/hdfs/HdfsDirectory.java
@@ -42,6 +42,7 @@ import org.slf4j.LoggerFactory;
 
 public class HdfsDirectory extends BaseDirectory {
   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final int DEFAULT_BUFFER_SIZE = 4096;
   
   private static final String LF_EXT = ".lf";
   protected final Path hdfsDirPath;
@@ -53,7 +54,7 @@ public class HdfsDirectory extends BaseDirectory {
   private final int bufferSize;
   
   public HdfsDirectory(Path hdfsDirPath, Configuration configuration) throws IOException {
    this(hdfsDirPath, HdfsLockFactory.INSTANCE, configuration, 4096);
    this(hdfsDirPath, HdfsLockFactory.INSTANCE, configuration, DEFAULT_BUFFER_SIZE);
   }
   
   public HdfsDirectory(Path hdfsDirPath, LockFactory lockFactory, Configuration configuration, int bufferSize)
@@ -190,7 +191,7 @@ public class HdfsDirectory extends BaseDirectory {
     return configuration;
   }
   
  static class HdfsIndexInput extends CustomBufferedIndexInput {
  public static class HdfsIndexInput extends CustomBufferedIndexInput {
     private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
     
     private final Path path;
diff --git a/solr/core/src/test-files/solr/solr-50-all.xml b/solr/core/src/test-files/solr/solr-50-all.xml
index a0f316548a3..e2ce9241f4e 100644
-- a/solr/core/src/test-files/solr/solr-50-all.xml
++ b/solr/core/src/test-files/solr/solr-50-all.xml
@@ -56,4 +56,8 @@
     <int name="connTimeout">${connTimeout:110}</int>
   </shardHandlerFactory>
 
  <backup>
    <repository name="local" class="a.b.C" default="true"/>
  </backup>

 </solr>
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java
index 47d8212e045..582c8b402ea 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java
@@ -399,22 +399,25 @@ public class BasicDistributedZk2Test extends AbstractFullDistribZkTestBase {
     checkShardConsistency(true, false);
     
     // try a backup command
    final HttpSolrClient client = (HttpSolrClient) shardToJetty.get(SHARD2).get(0).client.solrClient;
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set("qt", ReplicationHandler.PATH);
    params.set("command", "backup");
    Path location = createTempDir();
    location = FilterPath.unwrap(location).toRealPath();
    params.set("location", location.toString());
    try(final HttpSolrClient client = getHttpSolrClient((String) shardToJetty.get(SHARD2).get(0).info.get("base_url"))) {
      ModifiableSolrParams params = new ModifiableSolrParams();
      params.set("qt", ReplicationHandler.PATH);
      params.set("command", "backup");
      Path location = createTempDir();
      location = FilterPath.unwrap(location).toRealPath();
      params.set("location", location.toString());

      QueryRequest request = new QueryRequest(params);
      client.request(request, DEFAULT_TEST_COLLECTION_NAME);

      checkForBackupSuccess(client, location);
      client.close();
    }
 
    QueryRequest request = new QueryRequest(params);
    client.request(request);
    
    checkForBackupSuccess(client, location);
   }
 
   private void checkForBackupSuccess(HttpSolrClient client, Path location) throws InterruptedException, IOException {
    CheckBackupStatus checkBackupStatus = new CheckBackupStatus(client);
    CheckBackupStatus checkBackupStatus = new CheckBackupStatus(client, DEFAULT_TEST_COLLECTION_NAME);
     while (!checkBackupStatus.success) {
       checkBackupStatus.fetchStatus();
       Thread.sleep(1000);
diff --git a/solr/core/src/test/org/apache/solr/core/TestBackupRepositoryFactory.java b/solr/core/src/test/org/apache/solr/core/TestBackupRepositoryFactory.java
new file mode 100644
index 00000000000..81d3c40cf67
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/core/TestBackupRepositoryFactory.java
@@ -0,0 +1,152 @@
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.core.backup.repository.BackupRepository;
import org.apache.solr.core.backup.repository.BackupRepositoryFactory;
import org.apache.solr.core.backup.repository.LocalFileSystemRepository;
import org.apache.solr.schema.FieldType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import com.carrotsearch.randomizedtesting.rules.SystemPropertiesRestoreRule;

public class TestBackupRepositoryFactory extends SolrTestCaseJ4 {
  @Rule
  public TestRule solrTestRules = RuleChain.outerRule(new SystemPropertiesRestoreRule());
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  // tmp dir, cleaned up automatically.
  private static File solrHome = null;
  private static SolrResourceLoader loader = null;

  @BeforeClass
  public static void setupLoader() throws Exception {
    solrHome = createTempDir().toFile();
    loader = new SolrResourceLoader(solrHome.toPath());
  }

  @AfterClass
  public static void cleanupLoader() throws Exception {
    solrHome = null;
    loader = null;
  }

  @Test
  public void testMultipleDefaultRepositories() {
    PluginInfo[] plugins = new PluginInfo[2];

    {
      Map<String, Object> attrs = new HashMap<>();
      attrs.put(CoreAdminParams.NAME, "repo1");
      attrs.put(FieldType.CLASS_NAME, "a.b.C");
      attrs.put("default" , "true");
      plugins[0] = new PluginInfo("repository", attrs);
    }

    {
      Map<String, Object> attrs = new HashMap<>();
      attrs.put(CoreAdminParams.NAME, "repo2");
      attrs.put(FieldType.CLASS_NAME, "p.q.R");
      attrs.put("default" , "true");
      plugins[1] = new PluginInfo("repository", attrs);
    }

    expectedException.expect(SolrException.class);
    expectedException.expectMessage("More than one backup repository is configured as default");
    new BackupRepositoryFactory(plugins);
  }

  @Test
  public void testMultipleRepositoriesWithSameName() {
    PluginInfo[] plugins = new PluginInfo[2];

    {
      Map<String, Object> attrs = new HashMap<>();
      attrs.put(CoreAdminParams.NAME, "repo1");
      attrs.put(FieldType.CLASS_NAME, "a.b.C");
      attrs.put("default" , "true");
      plugins[0] = new PluginInfo("repository", attrs);
    }

    {
      Map<String, Object> attrs = new HashMap<>();
      attrs.put(CoreAdminParams.NAME, "repo1");
      attrs.put(FieldType.CLASS_NAME, "p.q.R");
      plugins[1] = new PluginInfo("repository", attrs);
    }

    expectedException.expect(SolrException.class);
    expectedException.expectMessage("Duplicate backup repository with name repo1");
    new BackupRepositoryFactory(plugins);
  }

  @Test
  public void testNonExistantBackupRepository() {
    PluginInfo[] plugins = new PluginInfo[0];
    BackupRepositoryFactory f = new BackupRepositoryFactory(plugins);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Could not find a backup repository with name repo1");
    f.newInstance(loader, "repo1");
  }

  @Test
  public void testRepositoryConfig() {
    PluginInfo[] plugins = new PluginInfo[1];

    {
      Map<String, Object> attrs = new HashMap<>();
      attrs.put(CoreAdminParams.NAME, "repo1");
      attrs.put(FieldType.CLASS_NAME, LocalFileSystemRepository.class.getName());
      attrs.put("default" , "true");
      attrs.put(ZkStateReader.BACKUP_LOCATION, "/tmp");
      plugins[0] = new PluginInfo("repository", attrs);
    }

    BackupRepositoryFactory f = new BackupRepositoryFactory(plugins);

    {
      BackupRepository repo = f.newInstance(loader);

      assertTrue(repo instanceof LocalFileSystemRepository);
      assertEquals("/tmp", repo.getConfigProperty(ZkStateReader.BACKUP_LOCATION));
    }

    {
      BackupRepository repo = f.newInstance(loader, "repo1");

      assertTrue(repo instanceof LocalFileSystemRepository);
      assertEquals("/tmp", repo.getConfigProperty(ZkStateReader.BACKUP_LOCATION));
    }
  }
}
diff --git a/solr/core/src/test/org/apache/solr/core/TestSolrXml.java b/solr/core/src/test/org/apache/solr/core/TestSolrXml.java
index 1cfeb3fe339..4343efec7c6 100644
-- a/solr/core/src/test/org/apache/solr/core/TestSolrXml.java
++ b/solr/core/src/test/org/apache/solr/core/TestSolrXml.java
@@ -67,6 +67,7 @@ public class TestSolrXml extends SolrTestCaseJ4 {
     NodeConfig cfg = SolrXmlConfig.fromSolrHome(loader, solrHome.toPath());
     CloudConfig ccfg = cfg.getCloudConfig();
     UpdateShardHandlerConfig ucfg = cfg.getUpdateShardHandlerConfig();
    PluginInfo[] backupRepoConfigs = cfg.getBackupRepositoryPlugins();
     
     assertEquals("core admin handler class", "testAdminHandler", cfg.getCoreAdminHandlerClass());
     assertEquals("collection handler class", "testCollectionsHandler", cfg.getCollectionsHandlerClass());
@@ -98,6 +99,11 @@ public class TestSolrXml extends SolrTestCaseJ4 {
     assertEquals("zk host", "testZkHost", ccfg.getZkHost());
     assertEquals("zk ACL provider", "DefaultZkACLProvider", ccfg.getZkACLProviderClass());
     assertEquals("zk credentials provider", "DefaultZkCredentialsProvider", ccfg.getZkCredentialsProviderClass());
    assertEquals(1, backupRepoConfigs.length);
    assertEquals("local", backupRepoConfigs[0].name);
    assertEquals("a.b.C", backupRepoConfigs[0].className);
    assertEquals("true", backupRepoConfigs[0].attributes.get("default"));
    assertEquals(0, backupRepoConfigs[0].initArgs.size());
   }
 
   // Test  a few property substitutions that happen to be in solr-50-all.xml.
@@ -321,4 +327,11 @@ public class TestSolrXml extends SolrTestCaseJ4 {
 
     SolrXmlConfig.fromString(loader, "<solr><solrcloud><str name=\"host\">host</str><int name=\"hostPort\">8983</int></solrcloud></solr>");
   }

  public void testMultiBackupSectionError() throws IOException {
    String solrXml = "<solr><backup></backup><backup></backup></solr>";
    expectedException.expect(SolrException.class);
    expectedException.expectMessage("Multiple instances of backup section found in solr.xml");
    SolrXmlConfig.fromString(loader, solrXml); // return not used, only for validation
  }
 }
diff --git a/solr/core/src/test/org/apache/solr/handler/BackupRestoreUtils.java b/solr/core/src/test/org/apache/solr/handler/BackupRestoreUtils.java
new file mode 100644
index 00000000000..bbc80beb692
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/handler/BackupRestoreUtils.java
@@ -0,0 +1,69 @@
package org.apache.solr.handler;

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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupRestoreUtils extends LuceneTestCase {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static int indexDocs(SolrClient masterClient, String collectionName, long docsSeed) throws IOException, SolrServerException {
    masterClient.deleteByQuery(collectionName, "*:*");

    Random random = new Random(docsSeed);// use a constant seed for the whole test run so that we can easily re-index.
    int nDocs = random.nextInt(100);
    log.info("Indexing {} test docs", nDocs);
    if (nDocs == 0) {
      return 0;
    }

    List<SolrInputDocument> docs = new ArrayList<>(nDocs);
    for (int i = 0; i < nDocs; i++) {
      SolrInputDocument doc = new SolrInputDocument();
      doc.addField("id", i);
      doc.addField("name", "name = " + i);
      docs.add(doc);
    }
    masterClient.add(collectionName, docs);
    masterClient.commit(collectionName);
    return nDocs;
  }

  public static void verifyDocs(int nDocs, SolrClient masterClient, String collectionName) throws SolrServerException, IOException {
    ModifiableSolrParams queryParams = new ModifiableSolrParams();
    queryParams.set("q", "*:*");
    QueryResponse response = masterClient.query(collectionName, queryParams);

    assertEquals(0, response.getStatus());
    assertEquals(nDocs, response.getResults().getNumFound());
  }
}
diff --git a/solr/core/src/test/org/apache/solr/handler/CheckBackupStatus.java b/solr/core/src/test/org/apache/solr/handler/CheckBackupStatus.java
index 706a2fe554a..f84d89f1eff 100644
-- a/solr/core/src/test/org/apache/solr/handler/CheckBackupStatus.java
++ b/solr/core/src/test/org/apache/solr/handler/CheckBackupStatus.java
@@ -33,18 +33,20 @@ public class CheckBackupStatus extends SolrTestCaseJ4 {
   final Pattern p = Pattern.compile("<str name=\"snapshotCompletedAt\">(.*?)</str>");
   final Pattern pException = Pattern.compile("<str name=\"snapShootException\">(.*?)</str>");
   final HttpSolrClient client;
  final String coreName;
 
  public CheckBackupStatus(final HttpSolrClient client, String lastBackupTimestamp) {
  public CheckBackupStatus(final HttpSolrClient client, String coreName, String lastBackupTimestamp) {
     this.client = client;
     this.lastBackupTimestamp = lastBackupTimestamp;
    this.coreName = coreName;
   }
 
  public CheckBackupStatus(final HttpSolrClient client) {
    this(client, null);
  public CheckBackupStatus(final HttpSolrClient client, String coreName) {
    this(client, coreName, null);
   }
 
   public void fetchStatus() throws IOException {
    String masterUrl = client.getBaseURL() + ReplicationHandler.PATH + "?command=" + ReplicationHandler.CMD_DETAILS;
    String masterUrl = client.getBaseURL() + "/"  + coreName + ReplicationHandler.PATH + "?command=" + ReplicationHandler.CMD_DETAILS;
     response = client.getHttpClient().execute(new HttpGet(masterUrl), new BasicResponseHandler());
     if(pException.matcher(response).find()) {
       fail("Failed to create backup");
diff --git a/solr/core/src/test/org/apache/solr/handler/TestHdfsBackupRestoreCore.java b/solr/core/src/test/org/apache/solr/handler/TestHdfsBackupRestoreCore.java
new file mode 100644
index 00000000000..887ebfe79c8
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/handler/TestHdfsBackupRestoreCore.java
@@ -0,0 +1,251 @@
package org.apache.solr.handler;

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

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakFilters;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
import org.apache.lucene.util.TestUtil;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.cloud.SolrCloudTestCase;
import org.apache.solr.cloud.hdfs.HdfsTestUtil;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.util.BadHdfsThreadsFilter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.solr.common.cloud.ZkStateReader.BASE_URL_PROP;

@ThreadLeakFilters(defaultFilters = true, filters = {
    BadHdfsThreadsFilter.class // hdfs currently leaks thread(s)
})
@SolrTestCaseJ4.SuppressSSL     // Currently unknown why SSL does not work with this test
public class TestHdfsBackupRestoreCore extends SolrCloudTestCase {
  public static final String HDFS_REPO_SOLR_XML = "<solr>\n" +
      "\n" +
      "  <str name=\"shareSchema\">${shareSchema:false}</str>\n" +
      "  <str name=\"configSetBaseDir\">${configSetBaseDir:configsets}</str>\n" +
      "  <str name=\"coreRootDirectory\">${coreRootDirectory:.}</str>\n" +
      "\n" +
      "  <shardHandlerFactory name=\"shardHandlerFactory\" class=\"HttpShardHandlerFactory\">\n" +
      "    <str name=\"urlScheme\">${urlScheme:}</str>\n" +
      "    <int name=\"socketTimeout\">${socketTimeout:90000}</int>\n" +
      "    <int name=\"connTimeout\">${connTimeout:15000}</int>\n" +
      "  </shardHandlerFactory>\n" +
      "\n" +
      "  <solrcloud>\n" +
      "    <str name=\"host\">127.0.0.1</str>\n" +
      "    <int name=\"hostPort\">${hostPort:8983}</int>\n" +
      "    <str name=\"hostContext\">${hostContext:solr}</str>\n" +
      "    <int name=\"zkClientTimeout\">${solr.zkclienttimeout:30000}</int>\n" +
      "    <bool name=\"genericCoreNodeNames\">${genericCoreNodeNames:true}</bool>\n" +
      "    <int name=\"leaderVoteWait\">10000</int>\n" +
      "    <int name=\"distribUpdateConnTimeout\">${distribUpdateConnTimeout:45000}</int>\n" +
      "    <int name=\"distribUpdateSoTimeout\">${distribUpdateSoTimeout:340000}</int>\n" +
      "  </solrcloud>\n" +
      "  \n" +
      "  <backup>\n" +
      "    <repository  name=\"hdfs\" class=\"org.apache.solr.core.backup.repository.HdfsBackupRepository\"> \n" +
      "      <str name=\"location\">${solr.hdfs.default.backup.path}</str>\n" +
      "      <str name=\"solr.hdfs.home\">${solr.hdfs.home:}</str>\n" +
      "      <str name=\"solr.hdfs.confdir\">${solr.hdfs.confdir:}</str>\n" +
      "    </repository>\n" +
      "  </backup>\n" +
      "  \n" +
      "</solr>\n";

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static MiniDFSCluster dfsCluster;
  private static String hdfsUri;
  private static FileSystem fs;
  private static long docsSeed; // see indexDocs()

  @BeforeClass
  public static void setupClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
    hdfsUri = HdfsTestUtil.getURI(dfsCluster);
    try {
      URI uri = new URI(hdfsUri);
      Configuration conf = HdfsTestUtil.getClientConfiguration(dfsCluster);
      conf.setBoolean("fs.hdfs.impl.disable.cache", true);
      fs = FileSystem.get(uri, conf);

      if (fs instanceof DistributedFileSystem) {
        // Make sure dfs is not in safe mode
        while (((DistributedFileSystem) fs).setSafeMode(SafeModeAction.SAFEMODE_GET, true)) {
          log.warn("The NameNode is in SafeMode - Solr will wait 5 seconds and try again.");
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            Thread.interrupted();
            // continue
          }
        }
      }

      fs.mkdirs(new org.apache.hadoop.fs.Path("/backup"));
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }

    System.setProperty("solr.hdfs.default.backup.path", "/backup");
    System.setProperty("solr.hdfs.home", hdfsUri + "/solr");
    useFactory("solr.StandardDirectoryFactory");

    configureCluster(1)// nodes
    .addConfig("conf1", TEST_PATH().resolve("configsets").resolve("cloud-minimal").resolve("conf"))
    .withSolrXml(HDFS_REPO_SOLR_XML)
    .configure();

    docsSeed = random().nextLong();
  }

  @AfterClass
  public static void teardownClass() throws Exception {
    System.clearProperty("solr.hdfs.home");
    System.clearProperty("solr.hdfs.default.backup.path");
    System.clearProperty("test.build.data");
    System.clearProperty("test.cache.data");
    IOUtils.closeQuietly(fs);
    fs = null;
    HdfsTestUtil.teardownClass(dfsCluster);
    dfsCluster = null;
  }

  @Test
  public void test() throws Exception {
    CloudSolrClient solrClient = cluster.getSolrClient();
    String collectionName = "HdfsBackupRestore";
    CollectionAdminRequest.Create create =
        CollectionAdminRequest.createCollection(collectionName, "conf1", 1, 1);
    create.process(solrClient);

    int nDocs = BackupRestoreUtils.indexDocs(solrClient, collectionName, docsSeed);

    DocCollection collectionState = solrClient.getZkStateReader().getClusterState().getCollection(collectionName);
    assertEquals(1, collectionState.getActiveSlices().size());
    Slice shard = collectionState.getActiveSlices().iterator().next();
    assertEquals(1, shard.getReplicas().size());
    Replica replica = shard.getReplicas().iterator().next();

    String replicaBaseUrl = replica.getStr(BASE_URL_PROP);
    String coreName = replica.getStr(ZkStateReader.CORE_NAME_PROP);
    String backupName = TestUtil.randomSimpleString(random(), 1, 5);

    boolean testViaReplicationHandler = random().nextBoolean();
    String baseUrl = cluster.getJettySolrRunners().get(0).getBaseUrl().toString();

    try (SolrClient masterClient = getHttpSolrClient(replicaBaseUrl)) {
      // Create a backup.
      if (testViaReplicationHandler) {
        log.info("Running Backup/restore via replication handler");
        runReplicationHandlerCommand(baseUrl, coreName, ReplicationHandler.CMD_BACKUP, "hdfs", backupName);
        CheckBackupStatus checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient, coreName, null);
        while (!checkBackupStatus.success) {
          checkBackupStatus.fetchStatus();
          Thread.sleep(1000);
        }
      } else {
        log.info("Running Backup/restore via core admin api");
        runCoreAdminCommand(replicaBaseUrl, coreName, CoreAdminAction.BACKUPCORE.toString(), "hdfs", backupName);
      }

      int numRestoreTests = nDocs > 0 ? TestUtil.nextInt(random(), 1, 5) : 1;
      for (int attempts=0; attempts<numRestoreTests; attempts++) {
        //Modify existing index before we call restore.
        if (nDocs > 0) {
          //Delete a few docs
          int numDeletes = TestUtil.nextInt(random(), 1, nDocs);
          for(int i=0; i<numDeletes; i++) {
            masterClient.deleteByQuery(collectionName, "id:" + i);
          }
          masterClient.commit(collectionName);

          //Add a few more
          int moreAdds = TestUtil.nextInt(random(), 1, 100);
          for (int i=0; i<moreAdds; i++) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", i + nDocs);
            doc.addField("name", "name = " + (i + nDocs));
            masterClient.add(collectionName, doc);
          }
          //Purposely not calling commit once in a while. There can be some docs which are not committed
          if (usually()) {
            masterClient.commit(collectionName);
          }
        }
        // Snapshooter prefixes "snapshot." to the backup name.
        if (testViaReplicationHandler) {
          // Snapshooter prefixes "snapshot." to the backup name.
          runReplicationHandlerCommand(baseUrl, coreName, ReplicationHandler.CMD_RESTORE, "hdfs", backupName);
          while (!TestRestoreCore.fetchRestoreStatus(baseUrl, coreName)) {
            Thread.sleep(1000);
          }
        } else {
          runCoreAdminCommand(replicaBaseUrl, coreName, CoreAdminAction.RESTORECORE.toString(), "hdfs", "snapshot." + backupName);
        }
        //See if restore was successful by checking if all the docs are present again
        BackupRestoreUtils.verifyDocs(nDocs, masterClient, coreName);
      }
    }
  }

  static void runCoreAdminCommand(String baseUrl, String coreName, String action, String repoName, String backupName) throws IOException {
    String masterUrl = baseUrl + "/admin/cores?action=" + action + "&core="+coreName+"&repository="+repoName+"&name="+backupName;
    executeHttpRequest(masterUrl);
  }

  static void runReplicationHandlerCommand(String baseUrl, String coreName, String action, String repoName, String backupName) throws IOException {
    String masterUrl = baseUrl + "/" + coreName + ReplicationHandler.PATH + "?command=" + action + "&repository="+repoName+"&name="+backupName;
    executeHttpRequest(masterUrl);
  }

  static void executeHttpRequest(String requestUrl) throws IOException {
    InputStream stream = null;
    try {
      URL url = new URL(requestUrl);
      stream = url.openStream();
      stream.close();
    } finally {
      IOUtils.closeQuietly(stream);
    }
  }
}
\ No newline at end of file
diff --git a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java
index bfad7825ea4..1ea16a0a817 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java
++ b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java
@@ -19,6 +19,7 @@ package org.apache.solr.handler;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
import java.lang.invoke.MethodHandles;
 import java.net.URL;
 import java.nio.file.DirectoryStream;
 import java.nio.file.Files;
@@ -41,15 +42,15 @@ import org.apache.lucene.util.TestUtil;
 import org.apache.solr.SolrJettyTestBase;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.embedded.JettyConfig;
 import org.apache.solr.client.solrj.embedded.JettySolrRunner;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.util.FileUtils;
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 @SolrTestCaseJ4.SuppressSSL     // Currently unknown why SSL does not work with this test
 public class TestReplicationHandlerBackup extends SolrJettyTestBase {
@@ -65,6 +66,8 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
 
   boolean addNumberToKeepInRequest = true;
   String backupKeepParamName = ReplicationHandler.NUMBER_BACKUPS_TO_KEEP_REQUEST_PARAM;
  private static long docsSeed; // see indexDocs()
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
   private static JettySolrRunner createJetty(TestReplicationHandler.SolrInstance instance) throws Exception {
     FileUtils.copyFile(new File(SolrTestCaseJ4.TEST_HOME(), "solr.xml"), new File(instance.getHomeDir(), "solr.xml"));
@@ -79,7 +82,7 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
   private static SolrClient createNewSolrClient(int port) {
     try {
       // setup the client...
      final String baseUrl = buildUrl(port, context) + "/" + DEFAULT_TEST_CORENAME;
      final String baseUrl = buildUrl(port, context);
       HttpSolrClient client = getHttpSolrClient(baseUrl);
       client.setConnectionTimeout(15000);
       client.setSoTimeout(60000);
@@ -107,6 +110,7 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
 
     masterJetty = createJetty(master);
     masterClient = createNewSolrClient(masterJetty.getLocalPort());
    docsSeed = random().nextLong();
   }
 
   @Override
@@ -123,10 +127,10 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
   @Test
   public void testBackupOnCommit() throws Exception {
     //Index
    int nDocs = indexDocs(masterClient);
    int nDocs = BackupRestoreUtils.indexDocs(masterClient, DEFAULT_TEST_COLLECTION_NAME, docsSeed);
 
     //Confirm if completed
    CheckBackupStatus checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient);
    CheckBackupStatus checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient, DEFAULT_TEST_CORENAME);
     while (!checkBackupStatus.success) {
       checkBackupStatus.fetchStatus();
       Thread.sleep(1000);
@@ -148,25 +152,18 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
     }
   }
 
  protected static int indexDocs(SolrClient masterClient) throws IOException, SolrServerException {
    int nDocs = TestUtil.nextInt(random(), 1, 100);
    masterClient.deleteByQuery("*:*");
    for (int i = 0; i < nDocs; i++) {
      SolrInputDocument doc = new SolrInputDocument();
      doc.addField("id", i);
      doc.addField("name", "name = " + i);
      masterClient.add(doc);
    }

    masterClient.commit();
    return nDocs;
  }

 
   @Test
   public void doTestBackup() throws Exception {
 
    int nDocs = indexDocs(masterClient);
    int nDocs = BackupRestoreUtils.indexDocs(masterClient, DEFAULT_TEST_COLLECTION_NAME, docsSeed);

    //Confirm if completed
    CheckBackupStatus checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient, DEFAULT_TEST_CORENAME);
    while (!checkBackupStatus.success) {
      checkBackupStatus.fetchStatus();
      Thread.sleep(1000);
    }
 
     Path[] snapDir = new Path[5]; //One extra for the backup on commit
     //First snapshot location
@@ -194,7 +191,7 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
         backupNames[i] = backupName;
       }
 
      CheckBackupStatus checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient, firstBackupTimestamp);
     checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient, DEFAULT_TEST_CORENAME, firstBackupTimestamp);
       while (!checkBackupStatus.success) {
         checkBackupStatus.fetchStatus();
         Thread.sleep(1000);
diff --git a/solr/core/src/test/org/apache/solr/handler/TestRestoreCore.java b/solr/core/src/test/org/apache/solr/handler/TestRestoreCore.java
index 2ee77b71fa6..eaf773a4326 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestRestoreCore.java
++ b/solr/core/src/test/org/apache/solr/handler/TestRestoreCore.java
@@ -35,13 +35,10 @@ import org.apache.lucene.util.TestUtil;
 import org.apache.solr.SolrJettyTestBase;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.embedded.JettyConfig;
 import org.apache.solr.client.solrj.embedded.JettySolrRunner;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.util.FileUtils;
 import org.junit.After;
 import org.junit.Before;
@@ -54,10 +51,11 @@ public class TestRestoreCore extends SolrJettyTestBase {
   TestReplicationHandler.SolrInstance master = null;
   SolrClient masterClient;
 
  private static final String CONF_DIR = "solr" + File.separator + "collection1" + File.separator + "conf"
  private static final String CONF_DIR = "solr" + File.separator + DEFAULT_TEST_CORENAME + File.separator + "conf"
       + File.separator;
 
   private static String context = "/solr";
  private static long docsSeed; // see indexDocs()
 
   private static JettySolrRunner createJetty(TestReplicationHandler.SolrInstance instance) throws Exception {
     FileUtils.copyFile(new File(SolrTestCaseJ4.TEST_HOME(), "solr.xml"), new File(instance.getHomeDir(), "solr.xml"));
@@ -72,7 +70,7 @@ public class TestRestoreCore extends SolrJettyTestBase {
   private static SolrClient createNewSolrClient(int port) {
     try {
       // setup the client...
      final String baseUrl = buildUrl(port, context) + "/" + DEFAULT_TEST_CORENAME;
      final String baseUrl = buildUrl(port, context);
       HttpSolrClient client = getHttpSolrClient(baseUrl);
       client.setConnectionTimeout(15000);
       client.setSoTimeout(60000);
@@ -95,6 +93,7 @@ public class TestRestoreCore extends SolrJettyTestBase {
 
     masterJetty = createJetty(master);
     masterClient = createNewSolrClient(masterJetty.getLocalPort());
    docsSeed = random().nextLong();
   }
 
   @Override
@@ -111,11 +110,12 @@ public class TestRestoreCore extends SolrJettyTestBase {
   @Test
   public void testSimpleRestore() throws Exception {
 
    int nDocs = usually() ? TestReplicationHandlerBackup.indexDocs(masterClient) : 0;
    int nDocs = usually() ? BackupRestoreUtils.indexDocs(masterClient, "collection1", docsSeed) : 0;
 
     String snapshotName;
     String location;
     String params = "";
    String baseUrl = masterJetty.getBaseUrl().toString();
 
     //Use the default backup location or an externally provided location.
     if (random().nextBoolean()) {
@@ -131,7 +131,7 @@ public class TestRestoreCore extends SolrJettyTestBase {
 
     TestReplicationHandlerBackup.runBackupCommand(masterJetty, ReplicationHandler.CMD_BACKUP, params);
 
    CheckBackupStatus checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient, null);
    CheckBackupStatus checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient, DEFAULT_TEST_CORENAME, null);
     while (!checkBackupStatus.success) {
       checkBackupStatus.fetchStatus();
       Thread.sleep(1000);
@@ -148,9 +148,9 @@ public class TestRestoreCore extends SolrJettyTestBase {
         //Delete a few docs
         int numDeletes = TestUtil.nextInt(random(), 1, nDocs);
         for(int i=0; i<numDeletes; i++) {
          masterClient.deleteByQuery("id:" + i);
          masterClient.deleteByQuery(DEFAULT_TEST_CORENAME, "id:" + i);
         }
        masterClient.commit();
        masterClient.commit(DEFAULT_TEST_CORENAME);
 
         //Add a few more
         int moreAdds = TestUtil.nextInt(random(), 1, 100);
@@ -158,37 +158,38 @@ public class TestRestoreCore extends SolrJettyTestBase {
           SolrInputDocument doc = new SolrInputDocument();
           doc.addField("id", i + nDocs);
           doc.addField("name", "name = " + (i + nDocs));
          masterClient.add(doc);
          masterClient.add(DEFAULT_TEST_CORENAME, doc);
         }
         //Purposely not calling commit once in a while. There can be some docs which are not committed
         if (usually()) {
          masterClient.commit();
          masterClient.commit(DEFAULT_TEST_CORENAME);
         }
       }
 
       TestReplicationHandlerBackup.runBackupCommand(masterJetty, ReplicationHandler.CMD_RESTORE, params);
 
      while (!fetchRestoreStatus()) {
      while (!fetchRestoreStatus(baseUrl, DEFAULT_TEST_CORENAME)) {
         Thread.sleep(1000);
       }
 
       //See if restore was successful by checking if all the docs are present again
      verifyDocs(nDocs);
      BackupRestoreUtils.verifyDocs(nDocs, masterClient, DEFAULT_TEST_CORENAME);
     }
 
   }
 
   @Test
   public void testFailedRestore() throws Exception {
    int nDocs = TestReplicationHandlerBackup.indexDocs(masterClient);
    int nDocs = BackupRestoreUtils.indexDocs(masterClient, "collection1", docsSeed);
 
     String location = createTempDir().toFile().getAbsolutePath();
     String snapshotName = TestUtil.randomSimpleString(random(), 1, 5);
     String params = "&name=" + snapshotName + "&location=" + URLEncoder.encode(location, "UTF-8");
    String baseUrl = masterJetty.getBaseUrl().toString();
 
     TestReplicationHandlerBackup.runBackupCommand(masterJetty, ReplicationHandler.CMD_BACKUP, params);
 
    CheckBackupStatus checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient, null);
    CheckBackupStatus checkBackupStatus = new CheckBackupStatus((HttpSolrClient) masterClient, DEFAULT_TEST_CORENAME, null);
     while (!checkBackupStatus.success) {
       checkBackupStatus.fetchStatus();
       Thread.sleep(1000);
@@ -205,7 +206,7 @@ public class TestRestoreCore extends SolrJettyTestBase {
     TestReplicationHandlerBackup.runBackupCommand(masterJetty, ReplicationHandler.CMD_RESTORE, params);
 
     try {
      while (!fetchRestoreStatus()) {
      while (!fetchRestoreStatus(baseUrl, DEFAULT_TEST_CORENAME)) {
         Thread.sleep(1000);
       }
       fail("Should have thrown an error because restore could not have been successful");
@@ -213,25 +214,16 @@ public class TestRestoreCore extends SolrJettyTestBase {
       //supposed to happen
     }
 
    verifyDocs(nDocs);
    BackupRestoreUtils.verifyDocs(nDocs, masterClient, DEFAULT_TEST_CORENAME);
 
     //make sure we can write to the index again
    nDocs = TestReplicationHandlerBackup.indexDocs(masterClient);
    verifyDocs(nDocs);
    nDocs = BackupRestoreUtils.indexDocs(masterClient, "collection1", docsSeed);
    BackupRestoreUtils.verifyDocs(nDocs, masterClient, DEFAULT_TEST_CORENAME);
 
   }
 
  private void verifyDocs(int nDocs) throws SolrServerException, IOException {
    ModifiableSolrParams queryParams = new ModifiableSolrParams();
    queryParams.set("q", "*:*");
    QueryResponse response = masterClient.query(queryParams);

    assertEquals(0, response.getStatus());
    assertEquals(nDocs, response.getResults().getNumFound());
  }

  private boolean fetchRestoreStatus() throws IOException {
    String masterUrl = buildUrl(masterJetty.getLocalPort(), context) + "/" + DEFAULT_TEST_CORENAME +
  public static boolean fetchRestoreStatus (String baseUrl, String coreName) throws IOException {
    String masterUrl = baseUrl + "/" + coreName +
         ReplicationHandler.PATH + "?command=" + ReplicationHandler.CMD_RESTORE_STATUS;
     final Pattern pException = Pattern.compile("<str name=\"exception\">(.*?)</str>");
 
- 
2.19.1.windows.1

