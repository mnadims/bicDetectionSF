From a8ef75ee024e42a12cc8993b1c326c6575979e51 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Thu, 7 May 2015 16:01:45 -0400
Subject: [PATCH] ACCUMULO-3779 Restore proper use zookeepers and add
 clientconf warning.

Warn when we don't find a client configuration file to use in
any of the normal locations and ensure that use of ZK goes from
shell option, to client conf to accumulo site.
--
 .../core/client/ClientConfiguration.java      |  4 ++
 .../java/org/apache/accumulo/shell/Shell.java | 40 +++++++++++++------
 .../apache/accumulo/shell/ShellOptionsJC.java | 11 +++++
 .../accumulo/shell/ShellConfigTest.java       | 36 +++++++++++++++++
 .../accumulo/shell/ShellSetInstanceTest.java  | 15 ++++---
 5 files changed, 87 insertions(+), 19 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java b/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java
index f008ab7b4..ed996454d 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java
@@ -222,6 +222,10 @@ public class ClientConfiguration extends CompositeConfiguration {
           configs.add(new ClientConfiguration(conf));
         }
       }
      // We couldn't find the client configuration anywhere
      if (configs.isEmpty()) {
        log.warn("Found no client.conf in default paths. Using default client configuration values.");
      }
       return new ClientConfiguration(configs);
     } catch (ConfigurationException e) {
       throw new IllegalStateException("Error loading client configuration", e);
diff --git a/shell/src/main/java/org/apache/accumulo/shell/Shell.java b/shell/src/main/java/org/apache/accumulo/shell/Shell.java
index 2db2e6378..37856ad3f 100644
-- a/shell/src/main/java/org/apache/accumulo/shell/Shell.java
++ b/shell/src/main/java/org/apache/accumulo/shell/Shell.java
@@ -456,27 +456,41 @@ public class Shell extends ShellOptions implements KeywordExecutable {
     }
   }
 
  /**
   * Get the ZooKeepers. Use the value passed in (if there was one), then fall back to the ClientConf, finally trying the accumulo-site.xml.
   *
   * @param keepers
   *          ZooKeepers passed to the shell
   * @param clientConfig
   *          ClientConfiguration instance
   * @return The ZooKeepers to connect to
   */
  static String getZooKeepers(String keepers, ClientConfiguration clientConfig, AccumuloConfiguration conf) {
    if (null != keepers) {
      return keepers;
    }

    if (clientConfig.containsKey(ClientProperty.INSTANCE_ZK_HOST.getKey())) {
      return clientConfig.get(ClientProperty.INSTANCE_ZK_HOST);
    }

    return conf.get(Property.INSTANCE_ZK_HOST);
  }

   /*
    * Takes instanceName and keepers as separate arguments, rather than just packaged into the clientConfig, so that we can fail over to accumulo-site.xml or
    * HDFS config if they're unspecified.
    */
  private static Instance getZooInstance(String instanceName, String keepers, ClientConfiguration clientConfig) {
  private static Instance getZooInstance(String instanceName, String keepersOption, ClientConfiguration clientConfig) {
     UUID instanceId = null;
     if (instanceName == null) {
       instanceName = clientConfig.get(ClientProperty.INSTANCE_NAME);
     }
    if (keepers == null) {
      keepers = clientConfig.get(ClientProperty.INSTANCE_ZK_HOST);
    }
    if (instanceName == null || keepers == null) {
      AccumuloConfiguration conf = SiteConfiguration.getInstance(ClientContext.convertClientConfig(clientConfig));
      if (instanceName == null) {
        Path instanceDir = new Path(VolumeConfiguration.getVolumeUris(conf)[0], "instance_id");
        instanceId = UUID.fromString(ZooUtil.getInstanceIDFromHdfs(instanceDir, conf));
      }
      if (keepers == null) {
        keepers = conf.get(Property.INSTANCE_ZK_HOST);
      }
    AccumuloConfiguration conf = SiteConfiguration.getInstance(ClientContext.convertClientConfig(clientConfig));
    String keepers = getZooKeepers(keepersOption, clientConfig, conf);
    if (instanceName == null) {
      Path instanceDir = new Path(VolumeConfiguration.getVolumeUris(conf)[0], "instance_id");
      instanceId = UUID.fromString(ZooUtil.getInstanceIDFromHdfs(instanceDir, conf));
     }
     if (instanceId != null) {
       return new ZooKeeperInstance(clientConfig.withInstance(instanceId).withZkHosts(keepers));
diff --git a/shell/src/main/java/org/apache/accumulo/shell/ShellOptionsJC.java b/shell/src/main/java/org/apache/accumulo/shell/ShellOptionsJC.java
index 7e6445d33..92ea1a5e0 100644
-- a/shell/src/main/java/org/apache/accumulo/shell/ShellOptionsJC.java
++ b/shell/src/main/java/org/apache/accumulo/shell/ShellOptionsJC.java
@@ -26,8 +26,12 @@ import java.util.TreeMap;
 
 import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.client.impl.ClientContext;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.KerberosToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.SiteConfiguration;
 import org.apache.commons.configuration.ConfigurationException;
 import org.apache.hadoop.security.UserGroupInformation;
 import org.slf4j.Logger;
@@ -304,6 +308,13 @@ public class ShellOptionsJC {
     if (useSasl()) {
       clientConfig.setProperty(ClientProperty.INSTANCE_RPC_SASL_ENABLED, "true");
     }

    // Automatically try to add in the proper ZK from accumulo-site for backwards compat.
    if (!clientConfig.containsKey(ClientProperty.INSTANCE_ZK_HOST.getKey())) {
      AccumuloConfiguration siteConf = SiteConfiguration.getInstance(ClientContext.convertClientConfig(clientConfig));
      clientConfig.withZkHosts(siteConf.get(Property.INSTANCE_ZK_HOST));
    }

     return clientConfig;
   }
 
diff --git a/shell/src/test/java/org/apache/accumulo/shell/ShellConfigTest.java b/shell/src/test/java/org/apache/accumulo/shell/ShellConfigTest.java
index 90b3028bc..bbeef56a8 100644
-- a/shell/src/test/java/org/apache/accumulo/shell/ShellConfigTest.java
++ b/shell/src/test/java/org/apache/accumulo/shell/ShellConfigTest.java
@@ -16,6 +16,7 @@
  */
 package org.apache.accumulo.shell;
 
import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertTrue;
 
@@ -25,10 +26,16 @@ import java.io.FileInputStream;
 import java.io.PrintStream;
 import java.io.PrintWriter;
 import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
 
 import jline.console.ConsoleReader;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.shell.ShellTest.TestOutputStream;
 import org.apache.log4j.Level;
 import org.junit.After;
@@ -111,4 +118,33 @@ public class ShellConfigTest {
     assertFalse(shell.config(args("--fake", "-tc", PasswordToken.class.getCanonicalName(), "-l", "password=foo", "-p", "bar")));
     assertTrue(output.get().contains(ParameterException.class.getCanonicalName()));
   }

  @Test
  public void testZooKeeperHostFallBackToSite() throws Exception {
    ClientConfiguration clientConfig = new ClientConfiguration();
    Map<String,String> data = new HashMap<>();
    data.put(Property.INSTANCE_ZK_HOST.getKey(), "site_hostname");
    AccumuloConfiguration conf = new ConfigurationCopy(data);
    assertEquals("site_hostname", Shell.getZooKeepers(null, clientConfig, conf));
  }

  @Test
  public void testZooKeeperHostFromClientConfig() throws Exception {
    ClientConfiguration clientConfig = new ClientConfiguration();
    clientConfig.withZkHosts("cc_hostname");
    Map<String,String> data = new HashMap<>();
    data.put(Property.INSTANCE_ZK_HOST.getKey(), "site_hostname");
    AccumuloConfiguration conf = new ConfigurationCopy(data);
    assertEquals("cc_hostname", Shell.getZooKeepers(null, clientConfig, conf));
  }

  @Test
  public void testZooKeeperHostFromOption() throws Exception {
    ClientConfiguration clientConfig = new ClientConfiguration();
    clientConfig.withZkHosts("cc_hostname");
    Map<String,String> data = new HashMap<>();
    data.put(Property.INSTANCE_ZK_HOST.getKey(), "site_hostname");
    AccumuloConfiguration conf = new ConfigurationCopy(data);
    assertEquals("opt_hostname", Shell.getZooKeepers("opt_hostname", clientConfig, conf));
  }
 }
diff --git a/shell/src/test/java/org/apache/accumulo/shell/ShellSetInstanceTest.java b/shell/src/test/java/org/apache/accumulo/shell/ShellSetInstanceTest.java
index 501b50263..1bf03b86b 100644
-- a/shell/src/test/java/org/apache/accumulo/shell/ShellSetInstanceTest.java
++ b/shell/src/test/java/org/apache/accumulo/shell/ShellSetInstanceTest.java
@@ -30,6 +30,7 @@ import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.PrintWriter;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.List;
 import java.util.UUID;
@@ -179,18 +180,14 @@ public class ShellSetInstanceTest {
       expect(clientConf.get(ClientProperty.INSTANCE_NAME)).andReturn(null);
     }
 
    if (!onlyHosts) {
      expect(clientConf.get(ClientProperty.INSTANCE_ZK_HOST)).andReturn(null);
    }

     mockStatic(ConfigSanityCheck.class);
     ConfigSanityCheck.validate(EasyMock.<AccumuloConfiguration> anyObject());
     expectLastCall().atLeastOnce();
     replay(ConfigSanityCheck.class);
 
     if (!onlyHosts) {
      expect(clientConf.containsKey(Property.INSTANCE_ZK_HOST.getKey())).andReturn(true).atLeastOnce();
      expect(clientConf.getString(Property.INSTANCE_ZK_HOST.getKey())).andReturn("host1,host2").atLeastOnce();
      expect(clientConf.containsKey(ClientProperty.INSTANCE_ZK_HOST.getKey())).andReturn(true).atLeastOnce();
      expect(clientConf.get(ClientProperty.INSTANCE_ZK_HOST)).andReturn("host1,host2").atLeastOnce();
       expect(clientConf.withZkHosts("host1,host2")).andReturn(clientConf);
     }
     if (!onlyInstance) {
@@ -240,9 +237,13 @@ public class ShellSetInstanceTest {
     expect(opts.isFake()).andReturn(false);
     expect(opts.getClientConfiguration()).andReturn(clientConf);
     expect(opts.isHdfsZooInstance()).andReturn(false);
    expect(clientConf.getKeys()).andReturn(Arrays.asList(ClientProperty.INSTANCE_NAME.getKey(), ClientProperty.INSTANCE_ZK_HOST.getKey()).iterator());
    expect(clientConf.getString(Property.GENERAL_SECURITY_CREDENTIAL_PROVIDER_PATHS.getKey())).andReturn(null);
     if (dashZ) {
       expect(clientConf.withInstance("foo")).andReturn(clientConf);
      expect(clientConf.getString(ClientProperty.INSTANCE_NAME.getKey())).andReturn("foo");
       expect(clientConf.withZkHosts("host1,host2")).andReturn(clientConf);
      expect(clientConf.getString(ClientProperty.INSTANCE_ZK_HOST.getKey())).andReturn("host1,host2");
       List<String> zl = new java.util.ArrayList<String>();
       zl.add("foo");
       zl.add("host1,host2");
@@ -250,7 +251,9 @@ public class ShellSetInstanceTest {
       expectLastCall().anyTimes();
     } else {
       expect(clientConf.withInstance("bar")).andReturn(clientConf);
      expect(clientConf.getString(ClientProperty.INSTANCE_NAME.getKey())).andReturn("bar");
       expect(clientConf.withZkHosts("host3,host4")).andReturn(clientConf);
      expect(clientConf.getString(ClientProperty.INSTANCE_ZK_HOST.getKey())).andReturn("host3,host4");
       expect(opts.getZooKeeperInstance()).andReturn(Collections.<String> emptyList());
       expect(opts.getZooKeeperInstanceName()).andReturn("bar");
       expect(opts.getZooKeeperHosts()).andReturn("host3,host4");
- 
2.19.1.windows.1

