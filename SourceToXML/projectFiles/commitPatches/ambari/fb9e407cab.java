From fb9e407cab7294b73fed413108ac103b343d7610 Mon Sep 17 00:00:00 2001
From: Dipayan Bhowmick <dipayan.bhowmick@gmail.com>
Date: Wed, 22 Jun 2016 01:20:59 +0530
Subject: [PATCH] AMBARI-17317. 'tez.tez-ui.history-url.base' did not get
 updated after ambari upgrade. (dipayanb)

--
 .../upgrade/AbstractUpgradeCatalog.java       | 88 +++++++++++++++++++
 .../upgrade/AbstractUpgradeCatalogTest.java   | 69 ++++++++++++++-
 2 files changed, 153 insertions(+), 4 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalog.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalog.java
index 4a1e8d8a61..51d739aebc 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalog.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalog.java
@@ -17,6 +17,14 @@
  */
 package org.apache.ambari.server.upgrade;
 
import javax.persistence.EntityManager;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
 import java.io.StringReader;
 import java.sql.ResultSet;
 import java.sql.SQLException;
@@ -33,6 +41,8 @@ import java.util.Set;
 import java.util.Stack;
 import java.util.StringTokenizer;
 import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
 import javax.persistence.EntityManager;
 import javax.xml.parsers.DocumentBuilder;
@@ -70,6 +80,8 @@ import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
 import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
 import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
 import org.apache.ambari.server.utils.VersionUtils;
import org.apache.ambari.server.view.ViewArchiveUtility;
import org.apache.ambari.server.view.configuration.ViewConfig;
 import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -91,6 +103,8 @@ public abstract class AbstractUpgradeCatalog implements UpgradeCatalog {
   protected Configuration configuration;
   @Inject
   protected StackUpgradeUtil stackUpgradeUtil;
  @Inject
  protected ViewArchiveUtility archiveUtility;
 
   protected Injector injector;
 
@@ -883,6 +897,80 @@ public abstract class AbstractUpgradeCatalog implements UpgradeCatalog {
   @Override
   public void upgradeData() throws AmbariException, SQLException {
     executeDMLUpdates();
    updateTezHistoryUrlBase();
  }

  /**
   * Version of the Tez view changes with every new version on Ambari. Hence the 'tez.tez-ui.history-url.base' in tez-site.xml
   * has to be changed every time ambari update happens. This will read the latest tez-view jar file and find out the
   * view version by reading the view.xml file inside it and update the 'tez.tez-ui.history-url.base' property in tez-site.xml
   * with the proper value of the updated tez view version.
   */
  private void updateTezHistoryUrlBase() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();
          if (installedServices.contains("TEZ")) {
            Config tezSite = cluster.getDesiredConfigByType("tez-site");
            if (tezSite != null) {
              String currentTezHistoryUrlBase = tezSite.getProperties().get("tez.tez-ui.history-url.base");
              if(currentTezHistoryUrlBase != null && !currentTezHistoryUrlBase.isEmpty()) {
                String newTezHistoryUrlBase = getUpdatedTezHistoryUrlBase(currentTezHistoryUrlBase);
                updateConfigurationProperties("tez-site", Collections.singletonMap("tez.tez-ui.history-url.base", newTezHistoryUrlBase), true, false);
              }
            }
          }
        }
      }
    }
  }

  protected String getUpdatedTezHistoryUrlBase(String currentTezHistoryUrlBase) throws AmbariException{
    String pattern = "(.*\\/TEZ\\/)(.*)(\\/TEZ_CLUSTER_INSTANCE)";
    Pattern regex = Pattern.compile(pattern);
    Matcher matcher = regex.matcher(currentTezHistoryUrlBase);
    String prefix;
    String suffix;
    String oldVersion;
    if (matcher.find()) {
      prefix = matcher.group(1);
      oldVersion = matcher.group(2);
      suffix = matcher.group(3);
    } else {
      throw new AmbariException("Cannot prepare the new value for property: 'tez.tez-ui.history-url.base' using the old value: '" + currentTezHistoryUrlBase + "'");
    }

    String latestTezViewVersion = getLatestTezViewVersion(oldVersion);

    return prefix + latestTezViewVersion + suffix;
  }

  protected String getLatestTezViewVersion(String oldVersion) {
    File viewsDirectory = configuration.getViewsDir();
    File[] files = viewsDirectory.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("tez-view");
      }
    });

    if(files == null || files.length == 0) {
      LOG.error("Could not file tez-view jar file in '{}'. Returning the old version", viewsDirectory.getAbsolutePath());
      return oldVersion;
    }
    File tezViewFile = files[0];
    try {
      ViewConfig viewConfigFromArchive = archiveUtility.getViewConfigFromArchive(tezViewFile);
      return viewConfigFromArchive.getVersion();
    } catch (JAXBException | IOException e) {
      LOG.error("Failed to read the tez view version from: {}. Returning the old version", tezViewFile);
      return oldVersion;
    }
   }
 
   @Override
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalogTest.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalogTest.java
index 345f62ab15..161ed13ed5 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalogTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalogTest.java
@@ -19,6 +19,7 @@ package org.apache.ambari.server.upgrade;
 
 import com.google.inject.Injector;
 import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
@@ -28,22 +29,30 @@ import org.apache.ambari.server.state.PropertyInfo;
 import org.apache.ambari.server.state.PropertyUpgradeBehavior;
 import org.apache.ambari.server.state.Service;
 import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.view.ViewArchiveUtility;
import org.apache.ambari.server.view.configuration.ViewConfig;
 import org.junit.Before;
 import org.junit.Test;
 
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
 import java.sql.SQLException;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.replay;
import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
 import static org.easymock.EasyMock.createNiceMock;
 import static org.easymock.EasyMock.createStrictMock;
 import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
 
 public class AbstractUpgradeCatalogTest {
   private static final String CONFIG_TYPE = "hdfs-site.xml";
@@ -178,6 +187,58 @@ public class AbstractUpgradeCatalogTest {
     verify(configHelper, amc, cluster, clusters, serviceInfo, oldConfig);
   }
 
  @Test
  public void shouldReturnLatestTezViewVersion() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    ViewArchiveUtility archiveUtility = createNiceMock(ViewArchiveUtility.class);
    File viewDirectory = createNiceMock(File.class);
    File viewJarFile = createNiceMock(File.class);
    ViewConfig viewConfig = createNiceMock(ViewConfig.class);
    expect(configuration.getViewsDir()).andReturn(viewDirectory).anyTimes();
    expect(viewDirectory.listFiles(anyObject(FilenameFilter.class))).andReturn(new File[] {viewJarFile}).anyTimes();
    expect(archiveUtility.getViewConfigFromArchive(viewJarFile)).andReturn(viewConfig).anyTimes();
    expect(viewConfig.getVersion()).andReturn("2.2").anyTimes();

    replay(configuration, archiveUtility, viewDirectory, viewJarFile, viewConfig);

    upgradeCatalog.archiveUtility = archiveUtility;
    upgradeCatalog.configuration = configuration;

    assertEquals("2.2", upgradeCatalog.getLatestTezViewVersion("2.1"));
    assertEquals("http://ambari:8080/#/main/views/TEZ/2.2/TEZ_CLUSTER_INSTANCE",
      upgradeCatalog.getUpdatedTezHistoryUrlBase("http://ambari:8080/#/main/views/TEZ/0.7.0.2.5.0.0-665/TEZ_CLUSTER_INSTANCE"));

    reset(viewDirectory, archiveUtility);

    expect(viewDirectory.listFiles(anyObject(FilenameFilter.class))).andReturn(new File[] {viewJarFile}).anyTimes();
    expect(archiveUtility.getViewConfigFromArchive(viewJarFile)).andThrow(new IOException()).anyTimes();
    replay(viewDirectory, archiveUtility);
    assertEquals("2.1", upgradeCatalog.getLatestTezViewVersion("2.1"));
    assertEquals("http://ambari:8080/#/main/views/TEZ/0.7.0.2.5.0.0-665/TEZ_CLUSTER_INSTANCE",
      upgradeCatalog.getUpdatedTezHistoryUrlBase("http://ambari:8080/#/main/views/TEZ/0.7.0.2.5.0.0-665/TEZ_CLUSTER_INSTANCE"));

    reset(viewDirectory);

    expect(viewDirectory.listFiles(anyObject(FilenameFilter.class))).andReturn(null).anyTimes();
    replay(viewDirectory);
    assertEquals("2.1", upgradeCatalog.getLatestTezViewVersion("2.1"));
    assertEquals("http://ambari:8080/#/main/views/TEZ/0.7.0.2.5.0.0-665/TEZ_CLUSTER_INSTANCE",
      upgradeCatalog.getUpdatedTezHistoryUrlBase("http://ambari:8080/#/main/views/TEZ/0.7.0.2.5.0.0-665/TEZ_CLUSTER_INSTANCE"));

    reset(viewDirectory);

    expect(viewDirectory.listFiles(anyObject(FilenameFilter.class))).andReturn(new File[] {}).anyTimes();
    replay(viewDirectory);
    assertEquals("2.1", upgradeCatalog.getLatestTezViewVersion("2.1"));
    assertEquals("http://ambari:8080/#/main/views/TEZ/0.7.0.2.5.0.0-665/TEZ_CLUSTER_INSTANCE",
      upgradeCatalog.getUpdatedTezHistoryUrlBase("http://ambari:8080/#/main/views/TEZ/0.7.0.2.5.0.0-665/TEZ_CLUSTER_INSTANCE"));
  }

  @Test(expected = AmbariException.class)
  public void shouldThrowExceptionWhenOldTezViewUrlIsInvalid() throws Exception {
    upgradeCatalog.getUpdatedTezHistoryUrlBase("Invalid URL");
  }

   private static PropertyInfo createProperty(String filename, String name, boolean add, boolean update, boolean delete) {
     PropertyInfo propertyInfo = new PropertyInfo();
     propertyInfo.setFilename(filename);
- 
2.19.1.windows.1

