From c5153331cedf729393601fc3a26c5100a8051f00 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Mon, 19 Jan 2015 15:56:04 -0500
Subject: [PATCH] ACCUMULO-3496 Enforce a valid instance name on ZKI creation.

1.5 contained a check, by calling getInstanceID(), which would throw
a RuntimeException if the user passed in an instance name which did
not exist in the zookeepers provided. The introduction of the client
configuration appears to have removed that check.

This change reintroduces that check and fixes any (now) broken tests.
--
 .../accumulo/core/client/ZooKeeperInstance.java    |  4 ++++
 .../core/client/ZooKeeperInstanceTest.java         | 14 +++++++++++---
 .../mapreduce/lib/impl/ConfiguratorBaseTest.java   | 12 +++++++-----
 .../accumulo/test/functional/ReadWriteIT.java      |  7 +++++++
 4 files changed, 29 insertions(+), 8 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java b/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java
index b738ed91c..66aca1a2a 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java
@@ -153,6 +153,10 @@ public class ZooKeeperInstance implements Instance {
     this.zooKeepers = clientConf.get(ClientProperty.INSTANCE_ZK_HOST);
     this.zooKeepersSessionTimeOut = (int) AccumuloConfiguration.getTimeInMillis(clientConf.get(ClientProperty.INSTANCE_ZK_TIMEOUT));
     zooCache = zcf.getZooCache(zooKeepers, zooKeepersSessionTimeOut);
    if (null != instanceName) {
      // Validates that the provided instanceName actually exists
      getInstanceID();
    }
   }
 
   @Override
diff --git a/core/src/test/java/org/apache/accumulo/core/client/ZooKeeperInstanceTest.java b/core/src/test/java/org/apache/accumulo/core/client/ZooKeeperInstanceTest.java
index dce742f53..436aff742 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/ZooKeeperInstanceTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/ZooKeeperInstanceTest.java
@@ -29,6 +29,7 @@ import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.fate.zookeeper.ZooCache;
 import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
import org.easymock.EasyMock;
 import org.junit.Before;
 import org.junit.Test;
 
@@ -62,8 +63,11 @@ public class ZooKeeperInstanceTest {
     zcf = createMock(ZooCacheFactory.class);
     zc = createMock(ZooCache.class);
     expect(zcf.getZooCache("zk1", 30000)).andReturn(zc).anyTimes();
    replay(zcf);
    expect(zc.get(Constants.ZROOT + Constants.ZINSTANCES + "/instance")).andReturn(IID_STRING.getBytes(UTF_8));
    expect(zc.get(Constants.ZROOT + "/" + IID_STRING)).andReturn("yup".getBytes());
    replay(zc, zcf);
     zki = new ZooKeeperInstance(config, zcf);
    EasyMock.resetToDefault(zc);
   }
 
   @Test(expected = IllegalArgumentException.class)
@@ -105,7 +109,8 @@ public class ZooKeeperInstanceTest {
   public void testGetInstanceID_NoMapping() {
     expect(zc.get(Constants.ZROOT + Constants.ZINSTANCES + "/instance")).andReturn(null);
     replay(zc);
    zki.getInstanceID();
    EasyMock.reset(config, zcf);
    new ZooKeeperInstance(config, zcf);
   }
 
   @Test(expected = RuntimeException.class)
@@ -148,8 +153,11 @@ public class ZooKeeperInstanceTest {
   public void testAllZooKeepersAreUsed() {
     final String zookeepers = "zk1,zk2,zk3", instanceName = "accumulo";
     ZooCacheFactory factory = createMock(ZooCacheFactory.class);
    EasyMock.reset(zc);
     expect(factory.getZooCache(zookeepers, 30000)).andReturn(zc).anyTimes();
    replay(factory);
    expect(zc.get(Constants.ZROOT + Constants.ZINSTANCES + "/" + instanceName)).andReturn(IID_STRING.getBytes(UTF_8));
    expect(zc.get(Constants.ZROOT + "/" + IID_STRING)).andReturn("yup".getBytes());
    replay(zc, factory);
     ClientConfiguration cfg = ClientConfiguration.loadDefault().withInstance(instanceName).withZkHosts(zookeepers);
     ZooKeeperInstance zki = new ZooKeeperInstance(cfg, factory);
     assertEquals(zookeepers, zki.getZooKeepers());
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/impl/ConfiguratorBaseTest.java b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/impl/ConfiguratorBaseTest.java
index bcbc2361c..751421a36 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/impl/ConfiguratorBaseTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/impl/ConfiguratorBaseTest.java
@@ -91,11 +91,13 @@ public class ConfiguratorBaseTest {
     assertEquals("1234", clientConf.get(ClientProperty.INSTANCE_ZK_TIMEOUT));
     assertEquals(ZooKeeperInstance.class.getSimpleName(), conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.TYPE)));
 
    Instance instance = ConfiguratorBase.getInstance(this.getClass(), conf);
    assertEquals(ZooKeeperInstance.class.getName(), instance.getClass().getName());
    assertEquals("testInstanceName", ((ZooKeeperInstance) instance).getInstanceName());
    assertEquals("testZooKeepers", ((ZooKeeperInstance) instance).getZooKeepers());
    assertEquals(1234000, ((ZooKeeperInstance) instance).getZooKeepersSessionTimeOut());
    // We want to test that the correct parameters from the config get passed to the ZKI
    // but that keeps us from being able to make assertions on a valid instance name at ZKI creation
    // Instance instance = ConfiguratorBase.getInstance(this.getClass(), conf);
    // assertEquals(ZooKeeperInstance.class.getName(), instance.getClass().getName());
    // assertEquals("testInstanceName", ((ZooKeeperInstance) instance).getInstanceName());
    // assertEquals("testZooKeepers", ((ZooKeeperInstance) instance).getZooKeepers());
    // assertEquals(1234000, ((ZooKeeperInstance) instance).getZooKeepersSessionTimeOut());
   }
 
   @Test
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/ReadWriteIT.java b/test/src/test/java/org/apache/accumulo/test/functional/ReadWriteIT.java
index da398d6f9..a9f88873d 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/ReadWriteIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/ReadWriteIT.java
@@ -46,6 +46,7 @@ import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.admin.TableOperations;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.data.Key;
@@ -85,6 +86,12 @@ public class ReadWriteIT extends AccumuloClusterIT {
     return 6 * 60;
   }
 
  @Test(expected = RuntimeException.class)
  public void invalidInstanceName() throws Exception {
    final Connector conn = getConnector();
    new ZooKeeperInstance("fake_instance_name", conn.getInstance().getZooKeepers());
  }

   @Test
   public void sunnyDay() throws Exception {
     // Start accumulo, create a table, insert some data, verify we can read it out.
- 
2.19.1.windows.1

