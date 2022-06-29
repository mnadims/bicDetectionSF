From df31c446bfa628bee9fab88addcfec5a13edda30 Mon Sep 17 00:00:00 2001
From: Arpit Agarwal <arp@apache.org>
Date: Thu, 24 Sep 2015 11:41:48 -0700
Subject: [PATCH] HADOOP-12437. Allow SecurityUtil to lookup alternate
 hostnames. (Contributed by Arpit Agarwal)

--
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../fs/CommonConfigurationKeysPublic.java     |   6 +
 .../main/java/org/apache/hadoop/net/DNS.java  | 129 +++++++++++++++---
 .../apache/hadoop/security/SecurityUtil.java  |  37 ++++-
 .../src/main/resources/core-default.xml       |  25 ++++
 .../java/org/apache/hadoop/net/TestDNS.java   | 110 ++++++++++++++-
 .../hadoop/security/TestSecurityUtil.java     |   2 +-
 .../hadoop/hdfs/server/datanode/DataNode.java |  25 +++-
 .../src/main/resources/hdfs-default.xml       |  25 ++--
 9 files changed, 316 insertions(+), 46 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 73e56b37bfe..11e4852cacd 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1147,6 +1147,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12438. Reset RawLocalFileSystem.useDeprecatedFileStatus in
     TestLocalFileSystem. (Chris Nauroth via wheat9)
 
    HADOOP-12437. Allow SecurityUtil to lookup alternate hostnames.
    (Arpit Agarwal)

 Release 2.7.2 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/CommonConfigurationKeysPublic.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/CommonConfigurationKeysPublic.java
index 9f053b8b7a1..9fff33e95c5 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/CommonConfigurationKeysPublic.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/CommonConfigurationKeysPublic.java
@@ -294,6 +294,12 @@
   /** See <a href="{@docRoot}/../core-default.html">core-default.xml</a> */
   public static final String  HADOOP_SECURITY_AUTH_TO_LOCAL =
     "hadoop.security.auth_to_local";
  /** See <a href="{@docRoot}/../core-default.html">core-default.xml</a> */
  public static final String HADOOP_SECURITY_DNS_INTERFACE_KEY =
    "hadoop.security.dns.interface";
  /** See <a href="{@docRoot}/../core-default.html">core-default.xml</a> */
  public static final String HADOOP_SECURITY_DNS_NAMESERVER_KEY =
    "hadoop.security.dns.nameserver";
 
   /** See <a href="{@docRoot}/../core-default.html">core-default.xml</a> */
   public static final String HADOOP_KERBEROS_MIN_SECONDS_BEFORE_RELOGIN =
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/net/DNS.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/net/DNS.java
index f19e80235b3..a6dc8e3d376 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/net/DNS.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/net/DNS.java
@@ -18,6 +18,8 @@
 
 package org.apache.hadoop.net;
 
import com.google.common.net.InetAddresses;
import com.sun.istack.Nullable;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.classification.InterfaceAudience;
@@ -27,9 +29,11 @@
 import java.net.NetworkInterface;
 import java.net.SocketException;
 import java.net.UnknownHostException;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.Enumeration;
 import java.util.LinkedHashSet;
import java.util.List;
 import java.util.Vector;
 
 import javax.naming.NamingException;
@@ -68,7 +72,7 @@
    * @return The host name associated with the provided IP
    * @throws NamingException If a NamingException is encountered
    */
  public static String reverseDns(InetAddress hostIp, String ns)
  public static String reverseDns(InetAddress hostIp, @Nullable String ns)
     throws NamingException {
     //
     // Builds the reverse IP lookup form
@@ -228,28 +232,44 @@ public static String getDefaultIP(String strInterface)
    *            (e.g. eth0 or eth0:0)
    * @param nameserver
    *            The DNS host name
   * @param tryfallbackResolution
   *            if true and if reverse DNS resolution fails then attempt to
   *            resolve the hostname with
   *            {@link InetAddress#getCanonicalHostName()} which includes
   *            hosts file resolution.
    * @return A string vector of all host names associated with the IPs tied to
    *         the specified interface
    * @throws UnknownHostException if the given interface is invalid
    */
  public static String[] getHosts(String strInterface, String nameserver)
    throws UnknownHostException {
    String[] ips = getIPs(strInterface);
    Vector<String> hosts = new Vector<String>();
    for (int ctr = 0; ctr < ips.length; ctr++) {
  public static String[] getHosts(String strInterface,
                                  @Nullable String nameserver,
                                  boolean tryfallbackResolution)
      throws UnknownHostException {
    final List<String> hosts = new Vector<String>();
    final List<InetAddress> addresses =
        getIPsAsInetAddressList(strInterface, true);
    for (InetAddress address : addresses) {
       try {
        hosts.add(reverseDns(InetAddress.getByName(ips[ctr]),
                             nameserver));
      } catch (UnknownHostException ignored) {
        hosts.add(reverseDns(address, nameserver));
       } catch (NamingException ignored) {
       }
     }
    if (hosts.isEmpty() && tryfallbackResolution) {
      for (InetAddress address : addresses) {
        final String canonicalHostName = address.getCanonicalHostName();
        // Don't use the result if it looks like an IP address.
        if (!InetAddresses.isInetAddress(canonicalHostName)) {
          hosts.add(canonicalHostName);
        }
      }
    }

     if (hosts.isEmpty()) {
      LOG.warn("Unable to determine hostname for interface " + strInterface);
      return new String[] { cachedHostname };
    } else {
      return hosts.toArray(new String[hosts.size()]);
      LOG.warn("Unable to determine hostname for interface " +
          strInterface);
      hosts.add(cachedHostname);
     }
    return hosts.toArray(new String[hosts.size()]);
   }
 
 
@@ -315,7 +335,7 @@ private static String resolveLocalHostIPAddress() {
    */
   public static String[] getHosts(String strInterface)
     throws UnknownHostException {
    return getHosts(strInterface, null);
    return getHosts(strInterface, null, false);
   }
 
   /**
@@ -331,17 +351,19 @@ private static String resolveLocalHostIPAddress() {
    * @throws UnknownHostException
    *             If one is encountered while querying the default interface
    */
  public static String getDefaultHost(String strInterface, String nameserver)
  public static String getDefaultHost(@Nullable String strInterface,
                                      @Nullable String nameserver,
                                      boolean tryfallbackResolution)
     throws UnknownHostException {
    if ("default".equals(strInterface)) {
    if (strInterface == null || "default".equals(strInterface)) {
       return cachedHostname;
     }
 
    if ("default".equals(nameserver)) {
      return getDefaultHost(strInterface);
    if (nameserver != null && "default".equals(nameserver)) {
      nameserver = null;
     }
 
    String[] hosts = getHosts(strInterface, nameserver);
    String[] hosts = getHosts(strInterface, nameserver, tryfallbackResolution);
     return hosts[0];
   }
 
@@ -357,9 +379,74 @@ public static String getDefaultHost(String strInterface, String nameserver)
    * @throws UnknownHostException
    *             If one is encountered while querying the default interface
    */
  public static String getDefaultHost(String strInterface)
  public static String getDefaultHost(@Nullable String strInterface)
     throws UnknownHostException {
    return getDefaultHost(strInterface, null);
    return getDefaultHost(strInterface, null, false);
   }
 
  /**
   * Returns the default (first) host name associated by the provided
   * nameserver with the address bound to the specified network interface.
   *
   * @param strInterface
   *            The name of the network interface to query (e.g. eth0)
   * @param nameserver
   *            The DNS host name
   * @throws UnknownHostException
   *             If one is encountered while querying the default interface
   */
  public static String getDefaultHost(@Nullable String strInterface,
                                      @Nullable String nameserver)
      throws UnknownHostException {
    return getDefaultHost(strInterface, nameserver, false);
  }

  /**
   * Returns all the IPs associated with the provided interface, if any, as
   * a list of InetAddress objects.
   *
   * @param strInterface
   *            The name of the network interface or sub-interface to query
   *            (eg eth0 or eth0:0) or the string "default"
   * @param returnSubinterfaces
   *            Whether to return IPs associated with subinterfaces of
   *            the given interface
   * @return A list of all the IPs associated with the provided
   *         interface. The local host IP is returned if the interface
   *         name "default" is specified or there is an I/O error looking
   *         for the given interface.
   * @throws UnknownHostException
   *             If the given interface is invalid
   *
   */
  public static List<InetAddress> getIPsAsInetAddressList(String strInterface,
      boolean returnSubinterfaces) throws UnknownHostException {
    if ("default".equals(strInterface)) {
      return Arrays.asList(InetAddress.getByName(cachedHostAddress));
    }
    NetworkInterface netIf;
    try {
      netIf = NetworkInterface.getByName(strInterface);
      if (netIf == null) {
        netIf = getSubinterface(strInterface);
      }
    } catch (SocketException e) {
      LOG.warn("I/O error finding interface " + strInterface +
          ": " + e.getMessage());
      return Arrays.asList(InetAddress.getByName(cachedHostAddress));
    }
    if (netIf == null) {
      throw new UnknownHostException("No such interface " + strInterface);
    }

    // NB: Using a LinkedHashSet to preserve the order for callers
    // that depend on a particular element being 1st in the array.
    // For example, getDefaultIP always returns the first element.
    LinkedHashSet<InetAddress> allAddrs = new LinkedHashSet<InetAddress>();
    allAddrs.addAll(Collections.list(netIf.getInetAddresses()));
    if (!returnSubinterfaces) {
      allAddrs.removeAll(getSubinterfaceInetAddrs(netIf));
    }
    return new Vector<InetAddress>(allAddrs);
  }
 }
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java
index eddf98d07ff..38096ab4715 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java
@@ -17,6 +17,8 @@
 package org.apache.hadoop.security;
 
 import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_DNS_INTERFACE_KEY;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_DNS_NAMESERVER_KEY;
 
 import java.io.IOException;
 import java.net.InetAddress;
@@ -29,6 +31,7 @@
 import java.util.List;
 import java.util.ServiceLoader;
 
import javax.annotation.Nullable;
 import javax.security.auth.kerberos.KerberosPrincipal;
 import javax.security.auth.kerberos.KerberosTicket;
 
@@ -39,6 +42,7 @@
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.CommonConfigurationKeys;
 import org.apache.hadoop.io.Text;
import org.apache.hadoop.net.DNS;
 import org.apache.hadoop.net.NetUtils;
 import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;
 import org.apache.hadoop.security.token.Token;
@@ -180,13 +184,38 @@ private static String replacePattern(String[] components, String hostname)
       throws IOException {
     String fqdn = hostname;
     if (fqdn == null || fqdn.isEmpty() || fqdn.equals("0.0.0.0")) {
      fqdn = getLocalHostName();
      fqdn = getLocalHostName(null);
     }
     return components[0] + "/" +
         StringUtils.toLowerCase(fqdn) + "@" + components[2];
   }
  
  static String getLocalHostName() throws UnknownHostException {

  /**
   * Retrieve the name of the current host. Multihomed hosts may restrict the
   * hostname lookup to a specific interface and nameserver with {@link
   * org.apache.hadoop.fs.CommonConfigurationKeysPublic#HADOOP_SECURITY_DNS_INTERFACE_KEY}
   * and {@link org.apache.hadoop.fs.CommonConfigurationKeysPublic#HADOOP_SECURITY_DNS_NAMESERVER_KEY}
   *
   * @param conf Configuration object. May be null.
   * @return
   * @throws UnknownHostException
   */
  static String getLocalHostName(@Nullable Configuration conf)
      throws UnknownHostException {
    if (conf != null) {
      String dnsInterface = conf.get(HADOOP_SECURITY_DNS_INTERFACE_KEY);
      String nameServer = conf.get(HADOOP_SECURITY_DNS_NAMESERVER_KEY);

      if (dnsInterface != null) {
        return DNS.getDefaultHost(dnsInterface, nameServer, true);
      } else if (nameServer != null) {
        throw new IllegalArgumentException(HADOOP_SECURITY_DNS_NAMESERVER_KEY +
            " requires " + HADOOP_SECURITY_DNS_INTERFACE_KEY + ". Check your" +
            "configuration.");
      }
    }

    // Fallback to querying the default hostname as we did before.
     return InetAddress.getLocalHost().getCanonicalHostName();
   }
 
@@ -207,7 +236,7 @@ static String getLocalHostName() throws UnknownHostException {
   @InterfaceStability.Evolving
   public static void login(final Configuration conf,
       final String keytabFileKey, final String userNameKey) throws IOException {
    login(conf, keytabFileKey, userNameKey, getLocalHostName());
    login(conf, keytabFileKey, userNameKey, getLocalHostName(conf));
   }
 
   /**
diff --git a/hadoop-common-project/hadoop-common/src/main/resources/core-default.xml b/hadoop-common-project/hadoop-common/src/main/resources/core-default.xml
index 410d96618ff..a57e81b2747 100644
-- a/hadoop-common-project/hadoop-common/src/main/resources/core-default.xml
++ b/hadoop-common-project/hadoop-common/src/main/resources/core-default.xml
@@ -88,6 +88,31 @@
   </description>
 </property>
 
<property>
  <name>hadoop.security.dns.interface</name>
  <description>
    The name of the Network Interface from which the service should determine
    its host name for Kerberos login. e.g. eth2. In a multi-homed environment,
    the setting can be used to affect the _HOST subsitution in the service
    Kerberos principal. If this configuration value is not set, the service
    will use its default hostname as returned by
    InetAddress.getLocalHost().getCanonicalHostName().

    Most clusters will not require this setting.
  </description>
</property>

<property>
  <name>hadoop.security.dns.nameserver</name>
  <description>
    The host name or IP address of the name server (DNS) which a service Node
    should use to determine its own host name for Kerberos Login. Requires
    hadoop.security.dns.interface.

    Most clusters will not require this setting.
  </description>
</property>

 <!-- 
 === Multiple group mapping providers configuration sample === 
   This sample illustrates a typical use case for CompositeGroupsMapping where
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/net/TestDNS.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/net/TestDNS.java
index 18316d20afa..2a3098ad234 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/net/TestDNS.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/net/TestDNS.java
@@ -18,6 +18,10 @@
 
 package org.apache.hadoop.net;
 
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.NetworkInterface;
import java.net.SocketException;
 import java.net.UnknownHostException;
 import java.net.InetAddress;
 
@@ -28,6 +32,9 @@
 import org.apache.hadoop.util.Time;
 
 import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
 import static org.junit.Assert.*;
 
 /**
@@ -38,6 +45,11 @@
   private static final Log LOG = LogFactory.getLog(TestDNS.class);
   private static final String DEFAULT = "default";
 
  // This is not a legal hostname (starts with a hyphen). It will never
  // be returned on any test machine.
  private static final String DUMMY_HOSTNAME = "-DUMMY_HOSTNAME";
  private static final String INVALID_DNS_SERVER = "0.0.0.0";

   /**
    * Test that asking for the default hostname works
    * @throws Exception if hostname lookups fail
@@ -89,12 +101,8 @@ private InetAddress getLocalIPAddr() throws UnknownHostException {
    */
   @Test
   public void testNullInterface() throws Exception {
    try {
      String host = DNS.getDefaultHost(null);
      fail("Expected a NullPointerException, got " + host);
    } catch (NullPointerException npe) {
      // Expected
    }
    String host = DNS.getDefaultHost(null);  // should work.
    assertThat(host, is(DNS.getDefaultHost(DEFAULT)));
     try {
       String ip = DNS.getDefaultIP(null);
       fail("Expected a NullPointerException, got " + ip);
@@ -103,6 +111,26 @@ public void testNullInterface() throws Exception {
     }
   }
 
  /**
   * Test that 'null' DNS server gives the same result as if no DNS
   * server was passed.
   */
  @Test
  public void testNullDnsServer() throws Exception {
    String host = DNS.getDefaultHost(getLoopbackInterface(), null);
    assertThat(host, is(DNS.getDefaultHost(getLoopbackInterface())));
  }

  /**
   * Test that "default" DNS server gives the same result as if no DNS
   * server was passed.
   */
  @Test
  public void testDefaultDnsServer() throws Exception {
    String host = DNS.getDefaultHost(getLoopbackInterface(), DEFAULT);
    assertThat(host, is(DNS.getDefaultHost(getLoopbackInterface())));
  }

   /**
    * Get the IP addresses of an unknown interface
    */
@@ -147,10 +175,80 @@ public void testRDNS() throws Exception {
                 + " Loopback=" + localhost.isLoopbackAddress()
                 + " Linklocal=" + localhost.isLinkLocalAddress());
       }
    }
  }

  /**
   * Test that when using an invalid DNS server with hosts file fallback,
   * we are able to get the hostname from the hosts file.
   *
   * This test may fail on some misconfigured test machines that don't have
   * an entry for "localhost" in their hosts file. This entry is correctly
   * configured out of the box on common Linux distributions, OS X and
   * Windows.
   *
   * @throws Exception
   */
  @Test (timeout=60000)
  public void testLookupWithHostsFallback() throws Exception {
    final String oldHostname = changeDnsCachedHostname(DUMMY_HOSTNAME);

    try {
      String hostname = DNS.getDefaultHost(
          getLoopbackInterface(), INVALID_DNS_SERVER, true);

      // Expect to get back something other than the cached host name.
      assertThat(hostname, not(DUMMY_HOSTNAME));
    } finally {
      // Restore DNS#cachedHostname for subsequent tests.
      changeDnsCachedHostname(oldHostname);
    }
  }

  /**
   * Test that when using an invalid DNS server without hosts file
   * fallback, we get back the cached host name.
   *
   * @throws Exception
   */
  @Test(timeout=60000)
  public void testLookupWithoutHostsFallback() throws Exception {
    final String oldHostname = changeDnsCachedHostname(DUMMY_HOSTNAME);
 
    try {
      String hostname = DNS.getDefaultHost(
          getLoopbackInterface(), INVALID_DNS_SERVER, false);

      // Expect to get back the cached host name since there was no hosts
      // file lookup.
      assertThat(hostname, is(DUMMY_HOSTNAME));
    } finally {
      // Restore DNS#cachedHostname for subsequent tests.
      changeDnsCachedHostname(oldHostname);
     }
   }
 
  private String getLoopbackInterface() throws SocketException {
    return NetworkInterface.getByInetAddress(
        InetAddress.getLoopbackAddress()).getDisplayName();
  }

  /**
   * Change DNS#cachedHostName to something which cannot be a real
   * host name. Uses reflection since it is a 'private final' field.
   */
  private String changeDnsCachedHostname(final String newHostname)
      throws Exception {
    final String oldCachedHostname = DNS.getDefaultHost(DEFAULT);
    Field field = DNS.class.getDeclaredField("cachedHostname");
    field.setAccessible(true);
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.set(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, newHostname);
    return oldCachedHostname;
  }

   /**
    * Test that the name "localhost" resolves to something.
    *
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestSecurityUtil.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestSecurityUtil.java
index e523e1864e3..14f9091ed9a 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestSecurityUtil.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestSecurityUtil.java
@@ -111,7 +111,7 @@ public void testPrincipalsWithLowerCaseHosts() throws IOException {
 
   @Test
   public void testLocalHostNameForNullOrWild() throws Exception {
    String local = StringUtils.toLowerCase(SecurityUtil.getLocalHostName());
    String local = StringUtils.toLowerCase(SecurityUtil.getLocalHostName(null));
     assertEquals("hdfs/" + local + "@REALM",
                  SecurityUtil.getServerPrincipal("hdfs/_HOST@REALM", (String)null));
     assertEquals("hdfs/" + local + "@REALM",
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/DataNode.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/DataNode.java
index 2aad83dd032..7c935d3574e 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/DataNode.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/DataNode.java
@@ -761,11 +761,24 @@ private static String getHostName(Configuration config)
       throws UnknownHostException {
     String name = config.get(DFS_DATANODE_HOST_NAME_KEY);
     if (name == null) {
      name = DNS.getDefaultHost(
          config.get(DFS_DATANODE_DNS_INTERFACE_KEY,
                     DFS_DATANODE_DNS_INTERFACE_DEFAULT),
          config.get(DFS_DATANODE_DNS_NAMESERVER_KEY,
                     DFS_DATANODE_DNS_NAMESERVER_DEFAULT));
      String dnsInterface = config.get(
          CommonConfigurationKeys.HADOOP_SECURITY_DNS_INTERFACE_KEY);
      String nameServer = config.get(
          CommonConfigurationKeys.HADOOP_SECURITY_DNS_NAMESERVER_KEY);
      boolean fallbackToHosts = false;

      if (dnsInterface == null) {
        // Try the legacy configuration keys.
        dnsInterface = config.get(DFS_DATANODE_DNS_INTERFACE_KEY);
        nameServer = config.get(DFS_DATANODE_DNS_NAMESERVER_KEY);
      } else {
        // If HADOOP_SECURITY_DNS_* is set then also attempt hosts file
        // resolution if DNS fails. We will not use hosts file resolution
        // by default to avoid breaking existing clusters.
        fallbackToHosts = true;
      }

      name = DNS.getDefaultHost(dnsInterface, nameServer, fallbackToHosts);
     }
     return name;
   }
@@ -2290,7 +2303,7 @@ public static DataNode instantiateDataNode(String args [], Configuration conf,
     Collection<StorageLocation> dataLocations = getStorageLocations(conf);
     UserGroupInformation.setConfiguration(conf);
     SecurityUtil.login(conf, DFS_DATANODE_KEYTAB_FILE_KEY,
        DFS_DATANODE_KERBEROS_PRINCIPAL_KEY);
        DFS_DATANODE_KERBEROS_PRINCIPAL_KEY, getHostName(conf));
     return makeInstance(dataLocations, conf, resources);
   }
 
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/resources/hdfs-default.xml b/hadoop-hdfs-project/hadoop-hdfs/src/main/resources/hdfs-default.xml
index 072b7a56b4e..77460efaabb 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/resources/hdfs-default.xml
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/resources/hdfs-default.xml
@@ -225,19 +225,28 @@
 </property>
 
  <property>
  <name>dfs.datanode.dns.interface</name>
  <value>default</value>
  <description>The name of the Network Interface from which a data node should 
  report its IP address.
  </description>
   <name>dfs.datanode.dns.interface</name>
   <value>default</value>
   <description>
     The name of the Network Interface from which a data node should
     report its IP address. e.g. eth2. This setting may be required for some
     multi-homed nodes where the DataNodes are assigned multiple hostnames
     and it is desirable for the DataNodes to use a non-default hostname.

     Prefer using hadoop.security.dns.interface over
     dfs.datanode.dns.interface.
   </description>
  </property>
  
 <property>
   <name>dfs.datanode.dns.nameserver</name>
   <value>default</value>
  <description>The host name or IP address of the name server (DNS)
  which a DataNode should use to determine the host name used by the
  NameNode for communication and display purposes.
  <description>
    The host name or IP address of the name server (DNS) which a DataNode
    should use to determine its own host name.

    Prefer using hadoop.security.dns.nameserver over
    dfs.datanode.dns.nameserver.
   </description>
  </property>
  
- 
2.19.1.windows.1

