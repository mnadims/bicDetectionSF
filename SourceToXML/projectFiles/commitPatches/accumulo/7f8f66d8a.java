From 7f8f66d8a066dbfe118e00e19984bb18f3adfabd Mon Sep 17 00:00:00 2001
From: Dave Marion <dlmarion@apache.org>
Date: Tue, 14 Jun 2016 10:25:06 -0400
Subject: [PATCH] ACCUMULO-4331: Add ability to specify range of ports in
 configuration. Tested and works with *.port.client properties. Modified
 tserver.port.search property to perform a linear search for free ports
 instead of random.

--
 .../core/conf/AccumuloConfiguration.java      |  36 ++-
 .../accumulo/core/conf/PropertyType.java      |  53 +++-
 .../core/conf/AccumuloConfigurationTest.java  |  75 +++++
 .../main/asciidoc/chapters/administration.txt |   5 +-
 .../java/org/apache/accumulo/proxy/Proxy.java |   4 +-
 .../org/apache/accumulo/server/Accumulo.java  |   4 +-
 .../accumulo/server/monitor/LogService.java   |   2 +-
 .../accumulo/server/rpc/TServerUtils.java     | 154 ++++++-----
 .../apache/accumulo/server/util/Admin.java    |  20 +-
 .../server/util/TServerUtilsTest.java         | 261 ++++++++++++++++++
 .../accumulo/gc/SimpleGarbageCollector.java   |  13 +-
 .../org/apache/accumulo/master/Master.java    |   2 +-
 .../accumulo/monitor/EmbeddedWebServer.java   |   4 +
 .../org/apache/accumulo/monitor/Monitor.java  |  65 +++--
 .../apache/accumulo/tracer/TraceServer.java   |  21 +-
 .../test/functional/ZombieTServer.java        |   4 +-
 .../test/performance/thrift/NullTserver.java  |   6 +-
 17 files changed, 590 insertions(+), 139 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/conf/AccumuloConfiguration.java b/core/src/main/java/org/apache/accumulo/core/conf/AccumuloConfiguration.java
index e74e71b3a..2e1d9bc2b 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/AccumuloConfiguration.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/AccumuloConfiguration.java
@@ -29,6 +29,8 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.conf.PropertyType.PortRange;
import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -341,18 +343,38 @@ public abstract class AccumuloConfiguration implements Iterable<Entry<String,Str
    *           if the property is of the wrong type
    * @see #getTimeInMillis(String)
    */
  public int getPort(Property property) {
  public int[] getPort(Property property) {
     checkType(property, PropertyType.PORT);
 
     String portString = get(property);
    int port = Integer.parseInt(portString);
    if (port != 0) {
      if (port < 1024 || port > 65535) {
        log.error("Invalid port number " + port + "; Using default " + property.getDefaultValue());
        port = Integer.parseInt(property.getDefaultValue());
    int[] ports = null;
    try {
      Pair<Integer,Integer> portRange = PortRange.parse(portString);
      int low = portRange.getFirst();
      int high = portRange.getSecond();
      ports = new int[high - low + 1];
      for (int i = 0, j = low; j <= high; i++, j++) {
        ports[i] = j;
      }
    } catch (IllegalArgumentException e) {
      ports = new int[1];
      try {
        int port = Integer.parseInt(portString);
        if (port != 0) {
          if (port < 1024 || port > 65535) {
            log.error("Invalid port number " + port + "; Using default " + property.getDefaultValue());
            ports[0] = Integer.parseInt(property.getDefaultValue());
          } else {
            ports[0] = port;
          }
        } else {
          ports[0] = port;
        }
      } catch (NumberFormatException e1) {
        throw new IllegalArgumentException("Invalid port syntax. Must be a single positive integers or a range (M-N) of positive integers");
       }
     }
    return port;
    return ports;
   }
 
   /**
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java b/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java
index a0d0f6874..f08ab5b0b 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java
@@ -24,7 +24,10 @@ import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.util.Pair;
 import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 import com.google.common.base.Function;
 import com.google.common.base.Predicate;
@@ -56,8 +59,10 @@ public enum PropertyType {
           + "Examples of valid host lists are 'localhost:2000,www.example.com,10.10.1.1:500' and 'localhost'.\n"
           + "Examples of invalid host lists are '', ':1000', and 'localhost:80000'"),
 
  PORT("port", Predicates.or(new Bounds(1024, 65535), in(true, "0")),
      "An positive integer in the range 1024-65535, not already in use or specified elsewhere in the configuration"),
  @SuppressWarnings("unchecked")
  PORT("port", Predicates.or(new Bounds(1024, 65535), in(true, "0"), new PortRange("\\d{4,5}-\\d{4,5}")),
      "An positive integer in the range 1024-65535 (not already in use or specified elsewhere in the configuration),\n"
          + "zero to indicate any open ephemeral port, or a range of positive integers specified as M-N"),
 
   COUNT("count", new Bounds(0, Integer.MAX_VALUE), "A non-negative integer in the range of 0-" + Integer.MAX_VALUE),
 
@@ -237,7 +242,7 @@ public enum PropertyType {
 
   private static class Matches implements Predicate<String> {
 
    private final Pattern pattern;
    protected final Pattern pattern;
 
     public Matches(final String pattern) {
       this(pattern, Pattern.DOTALL);
@@ -262,4 +267,46 @@ public enum PropertyType {
 
   }
 
  public static class PortRange extends Matches {

    private static final Logger log = LoggerFactory.getLogger(PortRange.class);

    public PortRange(final String pattern) {
      super(pattern);
    }

    @Override
    public boolean apply(final String input) {
      if (super.apply(input)) {
        try {
          PortRange.parse(input);
          return true;
        } catch (IllegalArgumentException e) {
          return false;
        }
      } else {
        return false;
      }
    }

    public static Pair<Integer,Integer> parse(String portRange) {
      int idx = portRange.indexOf('-');
      if (idx != -1) {
        int low = Integer.parseInt(portRange.substring(0, idx));
        if (low < 1024) {
          log.error("Invalid port number for low end of the range, using 1024");
          low = 1024;
        }
        int high = Integer.parseInt(portRange.substring(idx + 1));
        if (high > 65535) {
          log.error("Invalid port number for high end of the range, using 65535");
          high = 65535;
        }
        return new Pair<Integer,Integer>(low, high);
      }
      throw new IllegalArgumentException("Invalid port range specification, must use M-N notation.");
    }

  }

 }
diff --git a/core/src/test/java/org/apache/accumulo/core/conf/AccumuloConfigurationTest.java b/core/src/test/java/org/apache/accumulo/core/conf/AccumuloConfigurationTest.java
index efb080dcd..61186bd80 100644
-- a/core/src/test/java/org/apache/accumulo/core/conf/AccumuloConfigurationTest.java
++ b/core/src/test/java/org/apache/accumulo/core/conf/AccumuloConfigurationTest.java
@@ -72,4 +72,79 @@ public class AccumuloConfigurationTest {
     }
     assertTrue("test was a dud, and did nothing", found);
   }

  @Test
  public void testGetSinglePort() {
    AccumuloConfiguration c = AccumuloConfiguration.getDefaultConfiguration();
    ConfigurationCopy cc = new ConfigurationCopy(c);
    cc.set(Property.TSERV_CLIENTPORT, "9997");
    int[] ports = cc.getPort(Property.TSERV_CLIENTPORT);
    assertEquals(1, ports.length);
    assertEquals(9997, ports[0]);
  }

  @Test
  public void testGetAnyPort() {
    AccumuloConfiguration c = AccumuloConfiguration.getDefaultConfiguration();
    ConfigurationCopy cc = new ConfigurationCopy(c);
    cc.set(Property.TSERV_CLIENTPORT, "0");
    int[] ports = cc.getPort(Property.TSERV_CLIENTPORT);
    assertEquals(1, ports.length);
    assertEquals(0, ports[0]);
  }

  @Test
  public void testGetInvalidPort() {
    AccumuloConfiguration c = AccumuloConfiguration.getDefaultConfiguration();
    ConfigurationCopy cc = new ConfigurationCopy(c);
    cc.set(Property.TSERV_CLIENTPORT, "1020");
    int[] ports = cc.getPort(Property.TSERV_CLIENTPORT);
    assertEquals(1, ports.length);
    assertEquals(Integer.parseInt(Property.TSERV_CLIENTPORT.getDefaultValue()), ports[0]);
  }

  @Test
  public void testGetPortRange() {
    AccumuloConfiguration c = AccumuloConfiguration.getDefaultConfiguration();
    ConfigurationCopy cc = new ConfigurationCopy(c);
    cc.set(Property.TSERV_CLIENTPORT, "9997-9999");
    int[] ports = cc.getPort(Property.TSERV_CLIENTPORT);
    assertEquals(3, ports.length);
    assertEquals(9997, ports[0]);
    assertEquals(9998, ports[1]);
    assertEquals(9999, ports[2]);
  }

  @Test
  public void testGetPortRangeInvalidLow() {
    AccumuloConfiguration c = AccumuloConfiguration.getDefaultConfiguration();
    ConfigurationCopy cc = new ConfigurationCopy(c);
    cc.set(Property.TSERV_CLIENTPORT, "1020-1026");
    int[] ports = cc.getPort(Property.TSERV_CLIENTPORT);
    assertEquals(3, ports.length);
    assertEquals(1024, ports[0]);
    assertEquals(1025, ports[1]);
    assertEquals(1026, ports[2]);
  }

  @Test
  public void testGetPortRangeInvalidHigh() {
    AccumuloConfiguration c = AccumuloConfiguration.getDefaultConfiguration();
    ConfigurationCopy cc = new ConfigurationCopy(c);
    cc.set(Property.TSERV_CLIENTPORT, "65533-65538");
    int[] ports = cc.getPort(Property.TSERV_CLIENTPORT);
    assertEquals(3, ports.length);
    assertEquals(65533, ports[0]);
    assertEquals(65534, ports[1]);
    assertEquals(65535, ports[2]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetPortInvalidSyntax() {
    AccumuloConfiguration c = AccumuloConfiguration.getDefaultConfiguration();
    ConfigurationCopy cc = new ConfigurationCopy(c);
    cc.set(Property.TSERV_CLIENTPORT, "[65533,65538]");
    cc.getPort(Property.TSERV_CLIENTPORT);
  }

 }
diff --git a/docs/src/main/asciidoc/chapters/administration.txt b/docs/src/main/asciidoc/chapters/administration.txt
index d75c601fe..1935181d5 100644
-- a/docs/src/main/asciidoc/chapters/administration.txt
++ b/docs/src/main/asciidoc/chapters/administration.txt
@@ -63,7 +63,10 @@ In addition, the user can provide +0+ and an ephemeral port will be chosen inste
 ephemeral port is likely to be unique and not already bound. Thus, configuring ports to
 use +0+ instead of an explicit value, should, in most cases, work around any issues of
 running multiple distinct Accumulo instances (or any other process which tries to use the
same default ports) on the same hardware.
same default ports) on the same hardware. Finally, the *.port.client properties will work
with the port range syntax (M-N) allowing the user to specify a range of ports for the
service to attempt to bind. The ports in the range will be tried in a 1-up manner starting
at the low end of the range to, and including, the high end of the range.
 
 === Installation
 Choose a directory for the Accumulo installation. This directory will be referenced
diff --git a/proxy/src/main/java/org/apache/accumulo/proxy/Proxy.java b/proxy/src/main/java/org/apache/accumulo/proxy/Proxy.java
index 87e2c58d1..5f3b9915b 100644
-- a/proxy/src/main/java/org/apache/accumulo/proxy/Proxy.java
++ b/proxy/src/main/java/org/apache/accumulo/proxy/Proxy.java
@@ -265,8 +265,8 @@ public class Proxy implements KeywordExecutable {
     TimedProcessor timedProcessor = new TimedProcessor(metricsFactory, processor, serverName, threadName);
 
     // Create the thrift server with our processor and properties
    ServerAddress serverAddr = TServerUtils.startTServer(address, serverType, timedProcessor, protocolFactory, serverName, threadName, numThreads,
        simpleTimerThreadpoolSize, threadpoolResizeInterval, maxFrameSize, sslParams, saslParams, serverSocketTimeout);
    ServerAddress serverAddr = TServerUtils.startTServer(serverType, timedProcessor, protocolFactory, serverName, threadName, numThreads,
        simpleTimerThreadpoolSize, threadpoolResizeInterval, maxFrameSize, sslParams, saslParams, serverSocketTimeout, address);
 
     return serverAddr;
   }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java b/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
index f55199f60..302b6f96d 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
++ b/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
@@ -168,8 +168,8 @@ public class Accumulo {
     logConfigWatcher.start();
 
     // Makes sure the log-forwarding to the monitor is configured
    int logPort = conf.getPort(Property.MONITOR_LOG4J_PORT);
    System.setProperty("org.apache.accumulo.core.host.log.port", Integer.toString(logPort));
    int[] logPort = conf.getPort(Property.MONITOR_LOG4J_PORT);
    System.setProperty("org.apache.accumulo.core.host.log.port", Integer.toString(logPort[0]));
 
     log.info(application + " starting");
     log.info("Instance " + serverConfig.getInstance().getInstanceID());
diff --git a/server/base/src/main/java/org/apache/accumulo/server/monitor/LogService.java b/server/base/src/main/java/org/apache/accumulo/server/monitor/LogService.java
index 8acc764a8..fce1ca91f 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/monitor/LogService.java
++ b/server/base/src/main/java/org/apache/accumulo/server/monitor/LogService.java
@@ -94,7 +94,7 @@ public class LogService extends org.apache.log4j.AppenderSkeleton {
    */
   public static void startLogListener(AccumuloConfiguration conf, String instanceId, String hostAddress) {
     try {
      SocketServer server = new SocketServer(conf.getPort(Property.MONITOR_LOG4J_PORT));
      SocketServer server = new SocketServer(conf.getPort(Property.MONITOR_LOG4J_PORT)[0]);
 
       // getLocalPort will return the actual ephemeral port used when '0' was provided.
       String logForwardingAddr = hostAddress + ":" + server.getLocalPort();
diff --git a/server/base/src/main/java/org/apache/accumulo/server/rpc/TServerUtils.java b/server/base/src/main/java/org/apache/accumulo/server/rpc/TServerUtils.java
index 70e1c592c..74f4a0c5f 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/rpc/TServerUtils.java
++ b/server/base/src/main/java/org/apache/accumulo/server/rpc/TServerUtils.java
@@ -17,22 +17,18 @@
 package org.apache.accumulo.server.rpc;
 
 import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
 
 import java.io.IOException;
 import java.lang.reflect.Field;
import java.net.BindException;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.net.ServerSocket;
 import java.net.UnknownHostException;
 import java.util.Arrays;
 import java.util.HashSet;
import java.util.Random;
 import java.util.Set;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
 
 import javax.net.ssl.SSLServerSocket;
 
@@ -77,6 +73,22 @@ public class TServerUtils {
    */
   public static final ThreadLocal<String> clientAddress = new ThreadLocal<String>();
 
  /**
   *
   * @param hostname
   *          name of the host
   * @param ports
   *          array of ports
   * @return array of HostAndPort objects
   */
  public static HostAndPort[] getHostAndPorts(String hostname, int[] ports) {
    HostAndPort[] addresses = new HostAndPort[ports.length];
    for (int i = 0; i < ports.length; i++) {
      addresses[i] = HostAndPort.fromParts(hostname, ports[i]);
    }
    return addresses;
  }

   /**
    * Start a server, at the given port, or higher, if that port is not available.
    *
@@ -107,7 +119,7 @@ public class TServerUtils {
       throws UnknownHostException {
     final AccumuloConfiguration config = service.getConfiguration();
 
    final int portHint = config.getPort(portHintProperty);
    final int[] portHint = config.getPort(portHintProperty);
 
     int minThreads = 2;
     if (minThreadProperty != null)
@@ -135,42 +147,35 @@ public class TServerUtils {
     // create the TimedProcessor outside the port search loop so we don't try to register the same metrics mbean more than once
     TimedProcessor timedProcessor = new TimedProcessor(config, processor, serverName, threadName);
 
    Random random = new Random();
    for (int j = 0; j < 100; j++) {

      // Are we going to slide around, looking for an open port?
      int portsToSearch = 1;
      if (portSearch)
        portsToSearch = 1000;

      for (int i = 0; i < portsToSearch; i++) {
        int port = portHint + i;
        if (portHint != 0 && i > 0)
          port = 1024 + random.nextInt(65535 - 1024);
        if (port > 65535)
          port = 1024 + port % (65535 - 1024);
        try {
          HostAndPort addr = HostAndPort.fromParts(hostname, port);
          return TServerUtils.startTServer(addr, serverType, timedProcessor, serverName, threadName, minThreads, simpleTimerThreadpoolSize,
              timeBetweenThreadChecks, maxMessageSize, service.getServerSslParams(), service.getSaslParams(), service.getClientTimeoutInMillis());
        } catch (TTransportException ex) {
          log.error("Unable to start TServer", ex);
          if (ex.getCause() == null || ex.getCause().getClass() == BindException.class) {
            // Note: with a TNonblockingServerSocket a "port taken" exception is a cause-less
            // TTransportException, and with a TSocket created by TSSLTransportFactory, it
            // comes through as caused by a BindException.
            log.info("Unable to use port {}, retrying. (Thread Name = {})", port, threadName);
            sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
          } else {
            // thrift is passing up a nested exception that isn't a BindException,
            // so no reason to believe retrying on a different port would help.
            log.error("Unable to start TServer", ex);
    HostAndPort[] addresses = getHostAndPorts(hostname, portHint);
    try {
      return TServerUtils.startTServer(serverType, timedProcessor, serverName, threadName, minThreads, simpleTimerThreadpoolSize, timeBetweenThreadChecks,
          maxMessageSize, service.getServerSslParams(), service.getSaslParams(), service.getClientTimeoutInMillis(), addresses);
    } catch (TTransportException e) {
      if (portSearch) {
        HostAndPort last = addresses[addresses.length - 1];
        // Attempt to allocate a port outside of the specified port property
        // Search sequentially over the next 1000 ports
        for (int i = last.getPort() + 1; i < last.getPort() + 1001; i++) {
          int port = i;
          if (port > 65535) {
             break;
           }
          try {
            HostAndPort addr = HostAndPort.fromParts(hostname, port);
            return TServerUtils.startTServer(serverType, timedProcessor, serverName, threadName, minThreads, simpleTimerThreadpoolSize,
                timeBetweenThreadChecks, maxMessageSize, service.getServerSslParams(), service.getSaslParams(), service.getClientTimeoutInMillis(), addr);
          } catch (TTransportException tte) {
            log.info("Unable to use port {}, retrying. (Thread Name = {})", port, threadName);
          }
         }
        log.error("Unable to start TServer", e);
        throw new UnknownHostException("Unable to find a listen port");
      } else {
        log.error("Unable to start TServer", e);
        throw new UnknownHostException("Unable to find a listen port");
       }
     }
    throw new UnknownHostException("Unable to find a listen port");
   }
 
   /**
@@ -456,27 +461,27 @@ public class TServerUtils {
     return new ServerAddress(server, address);
   }
 
  public static ServerAddress startTServer(AccumuloConfiguration conf, HostAndPort address, ThriftServerType serverType, TProcessor processor,
      String serverName, String threadName, int numThreads, int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams,
      SaslServerConnectionParams saslParams, long serverSocketTimeout) throws TTransportException {
  public static ServerAddress startTServer(AccumuloConfiguration conf, ThriftServerType serverType, TProcessor processor, String serverName, String threadName,
      int numThreads, int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams,
      SaslServerConnectionParams saslParams, long serverSocketTimeout, HostAndPort... addresses) throws TTransportException {
 
     if (ThriftServerType.SASL == serverType) {
       processor = updateSaslProcessor(serverType, processor);
     }
 
    return startTServer(address, serverType, new TimedProcessor(conf, processor, serverName, threadName), serverName, threadName, numThreads, numSTThreads,
        timeBetweenThreadChecks, maxMessageSize, sslParams, saslParams, serverSocketTimeout);
    return startTServer(serverType, new TimedProcessor(conf, processor, serverName, threadName), serverName, threadName, numThreads, numSTThreads,
        timeBetweenThreadChecks, maxMessageSize, sslParams, saslParams, serverSocketTimeout, addresses);
   }
 
   /**
    * @see #startTServer(HostAndPort, ThriftServerType, TimedProcessor, TProtocolFactory, String, String, int, int, long, long, SslConnectionParams,
    *      SaslServerConnectionParams, long)
    */
  public static ServerAddress startTServer(HostAndPort address, ThriftServerType serverType, TimedProcessor processor, String serverName, String threadName,
      int numThreads, int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams,
      SaslServerConnectionParams saslParams, long serverSocketTimeout) throws TTransportException {
    return startTServer(address, serverType, processor, ThriftUtil.protocolFactory(), serverName, threadName, numThreads, numSTThreads,
        timeBetweenThreadChecks, maxMessageSize, sslParams, saslParams, serverSocketTimeout);
  public static ServerAddress startTServer(ThriftServerType serverType, TimedProcessor processor, String serverName, String threadName, int numThreads,
      int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams, SaslServerConnectionParams saslParams,
      long serverSocketTimeout, HostAndPort... addresses) throws TTransportException {
    return startTServer(serverType, processor, ThriftUtil.protocolFactory(), serverName, threadName, numThreads, numSTThreads, timeBetweenThreadChecks,
        maxMessageSize, sslParams, saslParams, serverSocketTimeout, addresses);
   }
 
   /**
@@ -484,35 +489,46 @@ public class TServerUtils {
    *
    * @return A ServerAddress encapsulating the Thrift server created and the host/port which it is bound to.
    */
  public static ServerAddress startTServer(HostAndPort address, ThriftServerType serverType, TimedProcessor processor, TProtocolFactory protocolFactory,
      String serverName, String threadName, int numThreads, int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams,
      SaslServerConnectionParams saslParams, long serverSocketTimeout) throws TTransportException {
  public static ServerAddress startTServer(ThriftServerType serverType, TimedProcessor processor, TProtocolFactory protocolFactory, String serverName,
      String threadName, int numThreads, int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams,
      SaslServerConnectionParams saslParams, long serverSocketTimeout, HostAndPort... addresses) throws TTransportException {
 
     // This is presently not supported. It's hypothetically possible, I believe, to work, but it would require changes in how the transports
     // work at the Thrift layer to ensure that both the SSL and SASL handshakes function. SASL's quality of protection addresses privacy issues.
     checkArgument(!(sslParams != null && saslParams != null), "Cannot start a Thrift server using both SSL and SASL");
 
    ServerAddress serverAddress;
    switch (serverType) {
      case SSL:
        log.debug("Instantiating SSL Thrift server");
        serverAddress = createSslThreadPoolServer(address, processor, protocolFactory, serverSocketTimeout, sslParams, serverName, numThreads, numSTThreads,
            timeBetweenThreadChecks);
        break;
      case SASL:
        log.debug("Instantiating SASL Thrift server");
        serverAddress = createSaslThreadPoolServer(address, processor, protocolFactory, serverSocketTimeout, saslParams, serverName, threadName, numThreads,
            numSTThreads, timeBetweenThreadChecks);
        break;
      case THREADPOOL:
        log.debug("Instantiating unsecure TThreadPool Thrift server");
        serverAddress = createBlockingServer(address, processor, protocolFactory, maxMessageSize, serverName, numThreads, numSTThreads, timeBetweenThreadChecks);
    ServerAddress serverAddress = null;
    for (HostAndPort address : addresses) {
      try {
        switch (serverType) {
          case SSL:
            log.debug("Instantiating SSL Thrift server");
            serverAddress = createSslThreadPoolServer(address, processor, protocolFactory, serverSocketTimeout, sslParams, serverName, numThreads,
                numSTThreads, timeBetweenThreadChecks);
            break;
          case SASL:
            log.debug("Instantiating SASL Thrift server");
            serverAddress = createSaslThreadPoolServer(address, processor, protocolFactory, serverSocketTimeout, saslParams, serverName, threadName,
                numThreads, numSTThreads, timeBetweenThreadChecks);
            break;
          case THREADPOOL:
            log.debug("Instantiating unsecure TThreadPool Thrift server");
            serverAddress = createBlockingServer(address, processor, protocolFactory, maxMessageSize, serverName, numThreads, numSTThreads,
                timeBetweenThreadChecks);
            break;
          case CUSTOM_HS_HA: // Intentional passthrough -- Our custom wrapper around HsHa is the default
          default:
            log.debug("Instantiating default, unsecure custom half-async Thrift server");
            serverAddress = createNonBlockingServer(address, processor, protocolFactory, serverName, threadName, numThreads, numSTThreads,
                timeBetweenThreadChecks, maxMessageSize);
        }
         break;
      case CUSTOM_HS_HA: // Intentional passthrough -- Our custom wrapper around HsHa is the default
      default:
        log.debug("Instantiating default, unsecure custom half-async Thrift server");
        serverAddress = createNonBlockingServer(address, processor, protocolFactory, serverName, threadName, numThreads, numSTThreads, timeBetweenThreadChecks,
            maxMessageSize);
      } catch (TTransportException e) {
        log.warn("Error attempting to create server at {}. Error: {}", address.toString(), e.getMessage());
      }
    }
    if (null == serverAddress) {
      throw new TTransportException("Unable to create server on addresses: " + Arrays.toString(addresses));
     }
 
     final TServer finalServer = serverAddress.server;
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/Admin.java b/server/base/src/main/java/org/apache/accumulo/server/util/Admin.java
index d43ce9254..df18fbf87 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/Admin.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/Admin.java
@@ -374,15 +374,17 @@ public class Admin implements KeywordExecutable {
     final String zTServerRoot = getTServersZkPath(instance);
     final ZooCache zc = new ZooCacheFactory().getZooCache(instance.getZooKeepers(), instance.getZooKeepersSessionTimeOut());
     for (String server : servers) {
      HostAndPort address = AddressUtil.parseAddress(server, context.getConfiguration().getPort(Property.TSERV_CLIENTPORT));
      final String finalServer = qualifyWithZooKeeperSessionId(zTServerRoot, zc, address.toString());
      log.info("Stopping server " + finalServer);
      MasterClient.execute(context, new ClientExec<MasterClientService.Client>() {
        @Override
        public void execute(MasterClientService.Client client) throws Exception {
          client.shutdownTabletServer(Tracer.traceInfo(), context.rpcCreds(), finalServer, force);
        }
      });
      for (int port : context.getConfiguration().getPort(Property.TSERV_CLIENTPORT)) {
        HostAndPort address = AddressUtil.parseAddress(server, port);
        final String finalServer = qualifyWithZooKeeperSessionId(zTServerRoot, zc, address.toString());
        log.info("Stopping server " + finalServer);
        MasterClient.execute(context, new ClientExec<MasterClientService.Client>() {
          @Override
          public void execute(MasterClientService.Client client) throws Exception {
            client.shutdownTabletServer(Tracer.traceInfo(), context.rpcCreds(), finalServer, force);
          }
        });
      }
     }
   }
 
diff --git a/server/base/src/test/java/org/apache/accumulo/server/util/TServerUtilsTest.java b/server/base/src/test/java/org/apache/accumulo/server/util/TServerUtilsTest.java
index 218d82c52..e6761a570 100644
-- a/server/base/src/test/java/org/apache/accumulo/server/util/TServerUtilsTest.java
++ b/server/base/src/test/java/org/apache/accumulo/server/util/TServerUtilsTest.java
@@ -21,16 +21,119 @@ import static org.easymock.EasyMock.createNiceMock;
 import static org.easymock.EasyMock.expect;
 import static org.easymock.EasyMock.replay;
 import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertTrue;
 
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
 import java.util.concurrent.ExecutorService;
 
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.thrift.ClientService.Iface;
import org.apache.accumulo.core.client.impl.thrift.ClientService.Processor;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.client.ClientServiceHandler;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.rpc.RpcWrapper;
import org.apache.accumulo.server.rpc.ServerAddress;
 import org.apache.accumulo.server.rpc.TServerUtils;
 import org.apache.thrift.server.TServer;
 import org.apache.thrift.transport.TServerSocket;
import org.junit.After;
 import org.junit.Test;
 
 public class TServerUtilsTest {

  protected static class TestInstance implements Instance {

    @Override
    public String getRootTabletLocation() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getMasterLocations() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getInstanceID() {
      return "1111";
    }

    @Override
    public String getInstanceName() {
      return "test";
    }

    @Override
    public String getZooKeepers() {
      return "";
    }

    @Override
    public int getZooKeepersSessionTimeOut() {
      return 30;
    }

    @Override
    public Connector getConnector(String user, byte[] pass) throws AccumuloException, AccumuloSecurityException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Connector getConnector(String user, ByteBuffer pass) throws AccumuloException, AccumuloSecurityException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Connector getConnector(String user, CharSequence pass) throws AccumuloException, AccumuloSecurityException {
      throw new UnsupportedOperationException();
    }

    @Override
    public AccumuloConfiguration getConfiguration() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setConfiguration(AccumuloConfiguration conf) {}

    @Override
    public Connector getConnector(String principal, AuthenticationToken token) throws AccumuloException, AccumuloSecurityException {
      throw new UnsupportedOperationException();
    }

  }

  protected static class TestServerConfigurationFactory extends ServerConfigurationFactory {

    private ConfigurationCopy conf = null;

    public TestServerConfigurationFactory(Instance instance) {
      super(instance);
      conf = new ConfigurationCopy(AccumuloConfiguration.getDefaultConfiguration());
    }

    @Override
    public synchronized AccumuloConfiguration getConfiguration() {
      return conf;
    }

  }

   private static class TServerWithoutES extends TServer {
     boolean stopCalled;
 
@@ -81,4 +184,162 @@ public class TServerUtilsTest {
     TServerUtils.stopTServer(null);
     // not dying is enough
   }

  private static final TestInstance instance = new TestInstance();
  private static final TestServerConfigurationFactory factory = new TestServerConfigurationFactory(instance);

  @After
  public void resetProperty() {
    ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, Property.TSERV_CLIENTPORT.getDefaultValue());
    ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_PORTSEARCH, Property.TSERV_PORTSEARCH.getDefaultValue());
  }

  @Test
  public void testStartServerZeroPort() throws Exception {
    TServer server = null;
    ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, "0");
    try {
      ServerAddress address = startServer();
      assertNotNull(address);
      server = address.getServer();
      assertNotNull(server);
      assertTrue(address.getAddress().getPort() > 1024);
    } finally {
      if (null != server) {
        TServerUtils.stopTServer(server);
      }
    }
  }

  @Test
  public void testStartServerFreePort() throws Exception {
    TServer server = null;
    int port = getFreePort(1024);
    ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, Integer.toString(port));
    try {
      ServerAddress address = startServer();
      assertNotNull(address);
      server = address.getServer();
      assertNotNull(server);
      assertEquals(port, address.getAddress().getPort());
    } finally {
      if (null != server) {
        TServerUtils.stopTServer(server);
      }
    }
  }

  @Test(expected = UnknownHostException.class)
  public void testStartServerUsedPort() throws Exception {
    int port = getFreePort(1024);
    // Bind to the port
    ServerSocket s = new ServerSocket(port);
    ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, Integer.toString(port));
    try {
      startServer();
    } finally {
      s.close();
    }
  }

  @Test
  public void testStartServerUsedPortWithSearch() throws Exception {
    TServer server = null;
    int[] port = findTwoFreeSequentialPorts(1024);
    // Bind to the port
    ServerSocket s = new ServerSocket(port[0]);
    ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, Integer.toString(port[0]));
    ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_PORTSEARCH, "true");
    try {
      ServerAddress address = startServer();
      assertNotNull(address);
      server = address.getServer();
      assertNotNull(server);
      assertEquals(port[1], address.getAddress().getPort());
    } finally {
      if (null != server) {
        TServerUtils.stopTServer(server);
      }
      s.close();
    }
  }

  @Test
  public void testStartServerPortRange() throws Exception {
    TServer server = null;
    int[] port = findTwoFreeSequentialPorts(1024);
    String portRange = Integer.toString(port[0]) + "-" + Integer.toString(port[1]);
    ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, portRange);
    try {
      ServerAddress address = startServer();
      assertNotNull(address);
      server = address.getServer();
      assertNotNull(server);
      assertTrue(port[0] == address.getAddress().getPort() || port[1] == address.getAddress().getPort());
    } finally {
      if (null != server) {
        TServerUtils.stopTServer(server);
      }
    }
  }

  @Test
  public void testStartServerPortRangeFirstPortUsed() throws Exception {
    TServer server = null;
    int[] port = findTwoFreeSequentialPorts(1024);
    String portRange = Integer.toString(port[0]) + "-" + Integer.toString(port[1]);
    // Bind to the port
    ServerSocket s = new ServerSocket(port[0]);
    ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, portRange);
    try {
      ServerAddress address = startServer();
      assertNotNull(address);
      server = address.getServer();
      assertNotNull(server);
      assertTrue(port[1] == address.getAddress().getPort());
    } finally {
      if (null != server) {
        TServerUtils.stopTServer(server);
      }
      s.close();
    }
  }

  private int[] findTwoFreeSequentialPorts(int startingAddress) {
    boolean sequential = false;
    int low = startingAddress;
    int high = 0;
    do {
      low = getFreePort(low);
      high = getFreePort(low + 1);
      sequential = ((high - low) == 1);
    } while (!sequential);
    return new int[] {low, high};
  }

  private int getFreePort(int startingAddress) {
    for (int i = startingAddress; i < 65535; i++) {
      try {
        ServerSocket s = new ServerSocket(i);
        int port = s.getLocalPort();
        s.close();
        return port;
      } catch (IOException e) {
        // keep trying
      }
    }
    throw new RuntimeException("Unable to find open port");
  }

  private ServerAddress startServer() throws Exception {
    AccumuloServerContext ctx = new AccumuloServerContext(factory);
    ClientServiceHandler clientHandler = new ClientServiceHandler(ctx, null, null);
    Iface rpcProxy = RpcWrapper.service(clientHandler, new Processor<Iface>(clientHandler));
    Processor<Iface> processor = new Processor<Iface>(rpcProxy);
    String hostname = InetAddress.getLocalHost().getHostName();

    return TServerUtils.startServer(ctx, hostname, Property.TSERV_CLIENTPORT, processor, "TServerUtilsTest", "TServerUtilsTestThread",
        Property.TSERV_PORTSEARCH, Property.TSERV_MINTHREADS, Property.TSERV_THREADCHECK, Property.GENERAL_MAX_MESSAGE_SIZE);

  }
 }
diff --git a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
index 4fc072721..c3efe5a4b 100644
-- a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
++ b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
@@ -92,6 +92,7 @@ import org.apache.accumulo.server.fs.VolumeManagerImpl;
 import org.apache.accumulo.server.fs.VolumeUtil;
 import org.apache.accumulo.server.replication.proto.Replication.Status;
 import org.apache.accumulo.server.rpc.RpcWrapper;
import org.apache.accumulo.server.rpc.ServerAddress;
 import org.apache.accumulo.server.rpc.TCredentialsUpdatingWrapper;
 import org.apache.accumulo.server.rpc.TServerUtils;
 import org.apache.accumulo.server.rpc.ThriftServerType;
@@ -722,13 +723,15 @@ public class SimpleGarbageCollector extends AccumuloServerContext implements Ifa
     } else {
       processor = new Processor<Iface>(rpcProxy);
     }
    int port = getConfiguration().getPort(Property.GC_PORT);
    int port[] = getConfiguration().getPort(Property.GC_PORT);
    HostAndPort[] addresses = TServerUtils.getHostAndPorts(this.opts.getAddress(), port);
     long maxMessageSize = getConfiguration().getMemoryInBytes(Property.GENERAL_MAX_MESSAGE_SIZE);
    HostAndPort result = HostAndPort.fromParts(opts.getAddress(), port);
    log.debug("Starting garbage collector listening on " + result);
     try {
      return TServerUtils.startTServer(getConfiguration(), result, getThriftServerType(), processor, this.getClass().getSimpleName(), "GC Monitor Service", 2,
          getConfiguration().getCount(Property.GENERAL_SIMPLETIMER_THREADPOOL_SIZE), 1000, maxMessageSize, getServerSslParams(), getSaslParams(), 0).address;
      ServerAddress server = TServerUtils.startTServer(getConfiguration(), getThriftServerType(), processor, this.getClass().getSimpleName(),
          "GC Monitor Service", 2, getConfiguration().getCount(Property.GENERAL_SIMPLETIMER_THREADPOOL_SIZE), 1000, maxMessageSize, getServerSslParams(),
          getSaslParams(), 0, addresses);
      log.debug("Starting garbage collector listening on " + server.address);
      return server.address;
     } catch (Exception ex) {
       // ACCUMULO-3651 Level changed to error and FATAL added to message for slf4j compatibility
       log.error("FATAL:", ex);
diff --git a/server/master/src/main/java/org/apache/accumulo/master/Master.java b/server/master/src/main/java/org/apache/accumulo/master/Master.java
index acc1d4ef1..f32021443 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/Master.java
++ b/server/master/src/main/java/org/apache/accumulo/master/Master.java
@@ -1339,7 +1339,7 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
   private void getMasterLock(final String zMasterLoc) throws KeeperException, InterruptedException {
     log.info("trying to get master lock");
 
    final String masterClientAddress = hostname + ":" + getConfiguration().getPort(Property.MASTER_CLIENTPORT);
    final String masterClientAddress = hostname + ":" + getConfiguration().getPort(Property.MASTER_CLIENTPORT)[0];
 
     while (true) {
 
diff --git a/server/monitor/src/main/java/org/apache/accumulo/monitor/EmbeddedWebServer.java b/server/monitor/src/main/java/org/apache/accumulo/monitor/EmbeddedWebServer.java
index f0213e73e..a292fd9b5 100644
-- a/server/monitor/src/main/java/org/apache/accumulo/monitor/EmbeddedWebServer.java
++ b/server/monitor/src/main/java/org/apache/accumulo/monitor/EmbeddedWebServer.java
@@ -129,4 +129,8 @@ public class EmbeddedWebServer {
   public boolean isUsingSsl() {
     return usingSsl;
   }

  public boolean isRunning() {
    return server.isRunning();
  }
 }
diff --git a/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java b/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
index 678ddb5d1..9dc4cef90 100644
-- a/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
++ b/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
@@ -16,9 +16,11 @@
  */
 package org.apache.accumulo.monitor;
 
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
 import static java.nio.charset.StandardCharsets.UTF_8;
 
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
@@ -98,7 +100,6 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.common.net.HostAndPort;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
 
 /**
  * Serve master statistics with an embedded web server.
@@ -454,35 +455,39 @@ public class Monitor {
     }
 
     Monitor.START_TIME = System.currentTimeMillis();
    int port = config.getConfiguration().getPort(Property.MONITOR_PORT);
    try {
      log.debug("Creating monitor on port " + port);
      server = new EmbeddedWebServer(hostname, port);
    } catch (Throwable ex) {
      log.error("Unable to start embedded web server", ex);
      throw new RuntimeException(ex);
    }

    server.addServlet(DefaultServlet.class, "/");
    server.addServlet(OperationServlet.class, "/op");
    server.addServlet(MasterServlet.class, "/master");
    server.addServlet(TablesServlet.class, "/tables");
    server.addServlet(TServersServlet.class, "/tservers");
    server.addServlet(ProblemServlet.class, "/problems");
    server.addServlet(GcStatusServlet.class, "/gc");
    server.addServlet(LogServlet.class, "/log");
    server.addServlet(XMLServlet.class, "/xml");
    server.addServlet(JSONServlet.class, "/json");
    server.addServlet(VisServlet.class, "/vis");
    server.addServlet(ScanServlet.class, "/scans");
    server.addServlet(BulkImportServlet.class, "/bulkImports");
    server.addServlet(Summary.class, "/trace/summary");
    server.addServlet(ListType.class, "/trace/listType");
    server.addServlet(ShowTrace.class, "/trace/show");
    server.addServlet(ReplicationServlet.class, "/replication");
    if (server.isUsingSsl())
      server.addServlet(ShellServlet.class, "/shell");
    server.start();
    int ports[] = config.getConfiguration().getPort(Property.MONITOR_PORT);
    for (int port : ports) {
      try {
        log.debug("Creating monitor on port " + port);
        server = new EmbeddedWebServer(hostname, port);
        server.addServlet(DefaultServlet.class, "/");
        server.addServlet(OperationServlet.class, "/op");
        server.addServlet(MasterServlet.class, "/master");
        server.addServlet(TablesServlet.class, "/tables");
        server.addServlet(TServersServlet.class, "/tservers");
        server.addServlet(ProblemServlet.class, "/problems");
        server.addServlet(GcStatusServlet.class, "/gc");
        server.addServlet(LogServlet.class, "/log");
        server.addServlet(XMLServlet.class, "/xml");
        server.addServlet(JSONServlet.class, "/json");
        server.addServlet(VisServlet.class, "/vis");
        server.addServlet(ScanServlet.class, "/scans");
        server.addServlet(BulkImportServlet.class, "/bulkImports");
        server.addServlet(Summary.class, "/trace/summary");
        server.addServlet(ListType.class, "/trace/listType");
        server.addServlet(ShowTrace.class, "/trace/show");
        server.addServlet(ReplicationServlet.class, "/replication");
        if (server.isUsingSsl())
          server.addServlet(ShellServlet.class, "/shell");
        server.start();
        break;
      } catch (Throwable ex) {
        log.error("Unable to start embedded web server", ex);
      }
    }
    if (!server.isRunning()) {
      throw new RuntimeException("Unable to start embedded web server on ports: " + Arrays.toString(ports));
    }
 
     try {
       log.debug("Using " + hostname + " to advertise monitor location in ZooKeeper");
diff --git a/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java b/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
index f4a99f89f..e74aef5cb 100644
-- a/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
++ b/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
@@ -22,6 +22,7 @@ import java.io.IOException;
 import java.net.InetSocketAddress;
 import java.net.ServerSocket;
 import java.nio.channels.ServerSocketChannel;
import java.util.Arrays;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.concurrent.TimeUnit;
@@ -231,10 +232,22 @@ public class TraceServer implements Watcher {
     // make sure we refer to the final variable from now on.
     connector = null;
 
    int port = conf.getPort(Property.TRACE_PORT);
    final ServerSocket sock = ServerSocketChannel.open().socket();
    sock.setReuseAddress(true);
    sock.bind(new InetSocketAddress(hostname, port));
    int ports[] = conf.getPort(Property.TRACE_PORT);
    ServerSocket sock = null;
    for (int port : ports) {
      ServerSocket s = ServerSocketChannel.open().socket();
      s.setReuseAddress(true);
      try {
        s.bind(new InetSocketAddress(hostname, port));
        sock = s;
        break;
      } catch (Exception e) {
        log.warn("Unable to start trace server on port {}", port);
      }
    }
    if (null == sock) {
      throw new RuntimeException("Unable to start trace server on configured ports: " + Arrays.toString(ports));
    }
     final TServerTransport transport = new TServerSocket(sock);
     TThreadPoolServer.Args options = new TThreadPoolServer.Args(transport);
     options.processor(new Processor<Iface>(new Receiver()));
diff --git a/test/src/main/java/org/apache/accumulo/test/functional/ZombieTServer.java b/test/src/main/java/org/apache/accumulo/test/functional/ZombieTServer.java
index 82f677b8d..44f2859c3 100644
-- a/test/src/main/java/org/apache/accumulo/test/functional/ZombieTServer.java
++ b/test/src/main/java/org/apache/accumulo/test/functional/ZombieTServer.java
@@ -105,8 +105,8 @@ public class ZombieTServer {
     TransactionWatcher watcher = new TransactionWatcher();
     final ThriftClientHandler tch = new ThriftClientHandler(context, watcher);
     Processor<Iface> processor = new Processor<Iface>(tch);
    ServerAddress serverPort = TServerUtils.startTServer(context.getConfiguration(), HostAndPort.fromParts("0.0.0.0", port), ThriftServerType.CUSTOM_HS_HA,
        processor, "ZombieTServer", "walking dead", 2, 1, 1000, 10 * 1024 * 1024, null, null, -1);
    ServerAddress serverPort = TServerUtils.startTServer(context.getConfiguration(), ThriftServerType.CUSTOM_HS_HA, processor, "ZombieTServer", "walking dead",
        2, 1, 1000, 10 * 1024 * 1024, null, null, -1, HostAndPort.fromParts("0.0.0.0", port));
 
     String addressString = serverPort.address.toString();
     String zPath = ZooUtil.getRoot(context.getInstance()) + Constants.ZTSERVERS + "/" + addressString;
diff --git a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
index 978110eb5..4d4402b52 100644
-- a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
++ b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
@@ -244,7 +244,7 @@ public class NullTserver {
     @Parameter(names = "--table", description = "table to adopt", required = true)
     String tableName = null;
     @Parameter(names = "--port", description = "port number to use")
    int port = DefaultConfiguration.getInstance().getPort(Property.TSERV_CLIENTPORT);
    int port = DefaultConfiguration.getInstance().getPort(Property.TSERV_CLIENTPORT)[0];
   }
 
   public static void main(String[] args) throws Exception {
@@ -258,8 +258,8 @@ public class NullTserver {
     TransactionWatcher watcher = new TransactionWatcher();
     ThriftClientHandler tch = new ThriftClientHandler(new AccumuloServerContext(new ServerConfigurationFactory(HdfsZooInstance.getInstance())), watcher);
     Processor<Iface> processor = new Processor<Iface>(tch);
    TServerUtils.startTServer(context.getConfiguration(), HostAndPort.fromParts("0.0.0.0", opts.port), ThriftServerType.CUSTOM_HS_HA, processor, "NullTServer",
        "null tserver", 2, 1, 1000, 10 * 1024 * 1024, null, null, -1);
    TServerUtils.startTServer(context.getConfiguration(), ThriftServerType.CUSTOM_HS_HA, processor, "NullTServer",
        "null tserver", 2, 1, 1000, 10 * 1024 * 1024, null, null, -1, HostAndPort.fromParts("0.0.0.0", opts.port));
 
     HostAndPort addr = HostAndPort.fromParts(InetAddress.getLocalHost().getHostName(), opts.port);
 
- 
2.19.1.windows.1

