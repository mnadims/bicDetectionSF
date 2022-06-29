From d2f82cd9258060cd89865ff4f7d5eed9187ede03 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Fri, 24 Oct 2014 21:39:03 -0400
Subject: [PATCH] ACCUMULO-3258 Make the dfs.datanode.synconclose check only
 fire once and ensure logging is properly set up.

Fixed up some initialization code so that logging is actually one of the first things
that happens so log messages aren't thrown into the .out files (as was
normally the case).
--
 .../org/apache/accumulo/server/Accumulo.java  | 67 ++++++++++++-------
 .../accumulo/server/fs/VolumeManagerImpl.java | 11 ++-
 .../server/watcher/Log4jConfiguration.java    | 49 ++++++++++++++
 .../server/watcher/MonitorLog4jWatcher.java   | 25 ++-----
 .../accumulo/gc/SimpleGarbageCollector.java   |  8 ++-
 .../org/apache/accumulo/master/Master.java    | 10 +--
 .../org/apache/accumulo/monitor/Monitor.java  | 21 +++---
 .../apache/accumulo/tracer/TraceServer.java   |  8 ++-
 .../apache/accumulo/tserver/TabletServer.java |  8 ++-
 9 files changed, 140 insertions(+), 67 deletions(-)
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/watcher/Log4jConfiguration.java

diff --git a/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java b/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
index 6ba05cc67..0dc76b222 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
++ b/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
@@ -27,6 +27,8 @@ import java.util.TreeMap;
 
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.trace.DistributedTrace;
 import org.apache.accumulo.core.util.AddressUtil;
@@ -34,13 +36,14 @@ import org.apache.accumulo.core.util.UtilWaitThread;
 import org.apache.accumulo.core.util.Version;
 import org.apache.accumulo.core.volume.Volume;
 import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.ReadOnlyTStore;
 import org.apache.accumulo.fate.ReadOnlyStore;
import org.apache.accumulo.fate.ReadOnlyTStore;
 import org.apache.accumulo.fate.ZooStore;
 import org.apache.accumulo.server.client.HdfsZooInstance;
 import org.apache.accumulo.server.conf.ServerConfiguration;
 import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.util.time.SimpleTimer;
import org.apache.accumulo.server.watcher.Log4jConfiguration;
 import org.apache.accumulo.server.watcher.MonitorLog4jWatcher;
 import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
 import org.apache.hadoop.fs.FileStatus;
@@ -52,9 +55,9 @@ import org.apache.log4j.xml.DOMConfigurator;
 import org.apache.zookeeper.KeeperException;
 
 public class Accumulo {
  

   private static final Logger log = Logger.getLogger(Accumulo.class);
  

   public static synchronized void updateAccumuloVersion(VolumeManager fs, int oldVersion) {
     for (Volume volume : fs.getVolumes()) {
       try {
@@ -73,7 +76,7 @@ public class Accumulo {
       }
     }
   }
  

   public static synchronized int getAccumuloPersistentVersion(FileSystem fs, Path path) {
     int dataVersion;
     try {
@@ -88,7 +91,7 @@ public class Accumulo {
       throw new RuntimeException("Unable to read accumulo version: an error occurred.", e);
     }
   }
  

   public static synchronized int getAccumuloPersistentVersion(VolumeManager fs) {
     // It doesn't matter which Volume is used as they should all have the data version stored
     Volume v = fs.getVolumes().iterator().next();
@@ -109,7 +112,7 @@ public class Accumulo {
       log.error("creating remote sink for trace spans", ex);
     }
   }
  

   /**
    * Finds the best log4j configuration file. A generic file is used only if an
    * application-specific file is not available. An XML file is preferred over
@@ -139,23 +142,19 @@ public class Accumulo {
     return defaultConfigFile;
   }
 
  public static void init(VolumeManager fs, ServerConfiguration config, String application) throws UnknownHostException {
    
  public static void setupLogging(String application) throws UnknownHostException {
     System.setProperty("org.apache.accumulo.core.application", application);
    

     if (System.getenv("ACCUMULO_LOG_DIR") != null)
       System.setProperty("org.apache.accumulo.core.dir.log", System.getenv("ACCUMULO_LOG_DIR"));
     else
       System.setProperty("org.apache.accumulo.core.dir.log", System.getenv("ACCUMULO_HOME") + "/logs/");
    

     String localhost = InetAddress.getLocalHost().getHostName();
     System.setProperty("org.apache.accumulo.core.ip.localhost.hostname", localhost);
    
    int logPort = config.getConfiguration().getPort(Property.MONITOR_LOG4J_PORT);
    System.setProperty("org.apache.accumulo.core.host.log.port", Integer.toString(logPort));
    

     // Use a specific log config, if it exists
    String logConfig = locateLogConfig(System.getenv("ACCUMULO_CONF_DIR"), application);
    String logConfigFile = locateLogConfig(System.getenv("ACCUMULO_CONF_DIR"), application);
     // Turn off messages about not being able to reach the remote logger... we protect against that.
     LogLog.setQuietMode(true);
 
@@ -164,31 +163,47 @@ public class Accumulo {
 
     DOMConfigurator.configureAndWatch(auditConfig, 5000);
 
    // Configure logging using information advertised in zookeeper by the monitor
    MonitorLog4jWatcher logConfigWatcher = new MonitorLog4jWatcher(config.getInstance().getInstanceID(), logConfig);
    // Set up local file-based logging right away
    Log4jConfiguration logConf = new Log4jConfiguration(logConfigFile);
    logConf.resetLogger();
  }

  public static void init(VolumeManager fs, ServerConfiguration serverConfig, String application) throws IOException {
    final AccumuloConfiguration conf = serverConfig.getConfiguration();
    final Instance instance = serverConfig.getInstance();

    // Use a specific log config, if it exists
    final String logConfigFile = locateLogConfig(System.getenv("ACCUMULO_CONF_DIR"), application);

    // Set up polling log4j updates and log-forwarding using information advertised in zookeeper by the monitor
    MonitorLog4jWatcher logConfigWatcher = new MonitorLog4jWatcher(instance.getInstanceID(), logConfigFile);
     logConfigWatcher.setDelay(5000L);
     logConfigWatcher.start();
 
    // Makes sure the log-forwarding to the monitor is configured
    int logPort = conf.getPort(Property.MONITOR_LOG4J_PORT);
    System.setProperty("org.apache.accumulo.core.host.log.port", Integer.toString(logPort));

     log.info(application + " starting");
    log.info("Instance " + config.getInstance().getInstanceID());
    log.info("Instance " + serverConfig.getInstance().getInstanceID());
     int dataVersion = Accumulo.getAccumuloPersistentVersion(fs);
     log.info("Data Version " + dataVersion);
     Accumulo.waitForZookeeperAndHdfs(fs);
    

     Version codeVersion = new Version(Constants.VERSION);
     if (!(canUpgradeFromDataVersion(dataVersion))) {
       throw new RuntimeException("This version of accumulo (" + codeVersion + ") is not compatible with files stored using data version " + dataVersion);
     }
    

     TreeMap<String,String> sortedProps = new TreeMap<String,String>();
    for (Entry<String,String> entry : config.getConfiguration())
    for (Entry<String,String> entry : conf)
       sortedProps.put(entry.getKey(), entry.getValue());
    

     for (Entry<String,String> entry : sortedProps.entrySet()) {
       String key = entry.getKey();
       log.info(key + " = " + (Property.isSensitive(key) ? "<hidden>" : entry.getValue()));
     }
    

     monitorSwappiness();
   }
 
@@ -206,9 +221,9 @@ public class Accumulo {
   public static boolean persistentVersionNeedsUpgrade(final int accumuloPersistentVersion) {
     return accumuloPersistentVersion == ServerConstants.TWO_DATA_VERSIONS_AGO || accumuloPersistentVersion == ServerConstants.PREV_DATA_VERSION;
   }
  

   /**
   * 
   *
    */
   public static void monitorSwappiness() {
     SimpleTimer.getInstance().schedule(new Runnable() {
@@ -238,7 +253,7 @@ public class Accumulo {
       }
     }, 1000, 10 * 60 * 1000);
   }
  

   public static void waitForZookeeperAndHdfs(VolumeManager fs) {
     log.info("Attempting to talk to zookeeper");
     while (true) {
diff --git a/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java b/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java
index 54d7e2a3f..38a836923 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java
++ b/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java
@@ -28,6 +28,7 @@ import java.util.Collection;
 import java.util.Collections;
 import java.util.EnumSet;
 import java.util.HashMap;
import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
@@ -66,6 +67,8 @@ public class VolumeManagerImpl implements VolumeManager {
 
   private static final Logger log = Logger.getLogger(VolumeManagerImpl.class);
 
  private static final HashSet<String> WARNED_ABOUT_SYNCONCLOSE = new HashSet<String>();

   Map<String,Volume> volumesByName;
   Multimap<URI,Volume> volumesByFileSystemUri;
   Volume defaultVolume;
@@ -262,7 +265,13 @@ public class VolumeManagerImpl implements VolumeManager {
 
           // Everything else
           if (!fs.getConf().getBoolean("dfs.datanode.synconclose", false)) {
            log.warn("dfs.datanode.synconclose set to false in hdfs-site.xml: data loss is possible on system reset or power loss");
            // Only warn once per process per volume URI
            synchronized (WARNED_ABOUT_SYNCONCLOSE) {
              if (!WARNED_ABOUT_SYNCONCLOSE.contains(entry.getKey())) {
                WARNED_ABOUT_SYNCONCLOSE.add(entry.getKey());
                log.warn("dfs.datanode.synconclose set to false in hdfs-site.xml: data loss is possible on hard system reset or power loss");
              }
            }
           }
         } catch (ClassNotFoundException ex) {
           // hadoop 1.0.X or hadoop 1.1.0
diff --git a/server/base/src/main/java/org/apache/accumulo/server/watcher/Log4jConfiguration.java b/server/base/src/main/java/org/apache/accumulo/server/watcher/Log4jConfiguration.java
new file mode 100644
index 000000000..7dea7a323
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/watcher/Log4jConfiguration.java
@@ -0,0 +1,49 @@
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
package org.apache.accumulo.server.watcher;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Encapsulate calls to PropertyConfigurator or DOMConfigurator to set up logging
 */
public class Log4jConfiguration {

  private final boolean usingProperties;
  private final String filename;

  public Log4jConfiguration(String filename) {
    usingProperties = (filename != null && filename.endsWith(".properties"));
    this.filename = filename;
  }

  public boolean isUsingProperties() {
    return usingProperties;
  }

  public void resetLogger() {
    // Force a reset on the logger's configuration
    LogManager.resetConfiguration();
    if (usingProperties) {
      new PropertyConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
    } else {
      new DOMConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
    }
  }
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/watcher/MonitorLog4jWatcher.java b/server/base/src/main/java/org/apache/accumulo/server/watcher/MonitorLog4jWatcher.java
index bc5e99feb..292129d45 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/watcher/MonitorLog4jWatcher.java
++ b/server/base/src/main/java/org/apache/accumulo/server/watcher/MonitorLog4jWatcher.java
@@ -22,9 +22,7 @@ import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
 import org.apache.log4j.Appender;
 import org.apache.log4j.LogManager;
 import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
 import org.apache.log4j.helpers.FileWatchdog;
import org.apache.log4j.xml.DOMConfigurator;
 import org.apache.zookeeper.KeeperException.NoNodeException;
 import org.apache.zookeeper.WatchedEvent;
 import org.apache.zookeeper.Watcher;
@@ -41,20 +39,22 @@ public class MonitorLog4jWatcher extends FileWatchdog implements Watcher {
   private static final String PORT_PROPERTY_NAME = "org.apache.accumulo.core.host.log.port";
 
   private final Object lock;
  private final boolean usingProperties;
  private final Log4jConfiguration logConfig;
   private boolean loggingDisabled = false;
   protected String path;
 
   public MonitorLog4jWatcher(String instance, String filename) {
     super(filename);
    usingProperties = (filename != null && filename.endsWith(".properties"));
     this.path = ZooUtil.getRoot(instance) + Constants.ZMONITOR_LOG4J_ADDR;
     this.lock = new Object();
    this.logConfig = new Log4jConfiguration(filename);
    doOnChange();
   }
 
   boolean isUsingProperties() {
    return usingProperties;
    return logConfig.isUsingProperties();
   }

   String getPath() {
     return path;
   }
@@ -74,11 +74,10 @@ public class MonitorLog4jWatcher extends FileWatchdog implements Watcher {
   }
 
   @Override
  protected void doOnChange() {
  public void doOnChange() {
     // this method gets called in the parent class' constructor
     // I'm not sure of a better way to get around this. The final modifier helps though.
     if (null == lock) {
      resetLogger();
       return;
     }
 
@@ -87,17 +86,7 @@ public class MonitorLog4jWatcher extends FileWatchdog implements Watcher {
       // Either way will result in log-forwarding being restarted
       loggingDisabled = false;
       log.info("Enabled log-forwarding");
      resetLogger();
    }
  }

  private void resetLogger() {
    // Force a reset on the logger's configuration
    LogManager.resetConfiguration();
    if (usingProperties) {
      new PropertyConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
    } else {
      new DOMConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
      logConfig.resetLogger();
     }
   }
 
diff --git a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
index d04303b5f..9b4af58f4 100644
-- a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
++ b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
@@ -137,17 +137,19 @@ public class SimpleGarbageCollector implements Iface {
 
   public static void main(String[] args) throws UnknownHostException, IOException {
     SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
    final String app = "gc";
    Accumulo.setupLogging(app);
     Instance instance = HdfsZooInstance.getInstance();
     ServerConfiguration conf = new ServerConfiguration(instance);
     final VolumeManager fs = VolumeManagerImpl.get();
    Accumulo.init(fs, conf, "gc");
    Accumulo.init(fs, conf, app);
     Opts opts = new Opts();
    opts.parseArgs("gc", args);
    opts.parseArgs(app, args);
     SimpleGarbageCollector gc = new SimpleGarbageCollector(opts);
     AccumuloConfiguration config = conf.getConfiguration();
 
     gc.init(fs, instance, SystemCredentials.get(), config);
    Accumulo.enableTracing(opts.getAddress(), "gc");
    Accumulo.enableTracing(opts.getAddress(), app);
     gc.run();
   }
 
diff --git a/server/master/src/main/java/org/apache/accumulo/master/Master.java b/server/master/src/main/java/org/apache/accumulo/master/Master.java
index f894afc02..52f116fbf 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/Master.java
++ b/server/master/src/main/java/org/apache/accumulo/master/Master.java
@@ -1139,15 +1139,17 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
     try {
       SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
 
      VolumeManager fs = VolumeManagerImpl.get();
       ServerOpts opts = new ServerOpts();
      opts.parseArgs("master", args);
      final String app = "master";
      opts.parseArgs(app, args);
       String hostname = opts.getAddress();
      Accumulo.setupLogging(app);
       Instance instance = HdfsZooInstance.getInstance();
       ServerConfiguration conf = new ServerConfiguration(instance);
      Accumulo.init(fs, conf, "master");
      VolumeManager fs = VolumeManagerImpl.get();
      Accumulo.init(fs, conf, app);
       Master master = new Master(conf, fs, hostname);
      Accumulo.enableTracing(hostname, "master");
      Accumulo.enableTracing(hostname, app);
       master.run();
     } catch (Exception ex) {
       log.error("Unexpected exception, exiting", ex);
diff --git a/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java b/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
index 948aed1c1..c4ab6e6d0 100644
-- a/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
++ b/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
@@ -29,8 +29,6 @@ import java.util.Map.Entry;
 import java.util.Set;
 import java.util.TreeMap;
 
import org.apache.accumulo.monitor.servlets.ScanServlet;
import com.google.common.net.HostAndPort;
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
@@ -64,6 +62,7 @@ import org.apache.accumulo.monitor.servlets.LogServlet;
 import org.apache.accumulo.monitor.servlets.MasterServlet;
 import org.apache.accumulo.monitor.servlets.OperationServlet;
 import org.apache.accumulo.monitor.servlets.ProblemServlet;
import org.apache.accumulo.monitor.servlets.ScanServlet;
 import org.apache.accumulo.monitor.servlets.ShellServlet;
 import org.apache.accumulo.monitor.servlets.TServersServlet;
 import org.apache.accumulo.monitor.servlets.TablesServlet;
@@ -90,6 +89,8 @@ import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.log4j.Logger;
 import org.apache.zookeeper.KeeperException;
 
import com.google.common.net.HostAndPort;

 /**
  * Serve master statistics with an embedded web server.
  */
@@ -396,17 +397,19 @@ public class Monitor {
 
   public static void main(String[] args) throws Exception {
     SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
    
    VolumeManager fs = VolumeManagerImpl.get();

     ServerOpts opts = new ServerOpts();
    opts.parseArgs("monitor", args);
    final String app = "monitor";
    opts.parseArgs(app, args);
     String hostname = opts.getAddress();
 
    Accumulo.setupLogging(app);
    VolumeManager fs = VolumeManagerImpl.get();
     instance = HdfsZooInstance.getInstance();
     config = new ServerConfiguration(instance);
    Accumulo.init(fs, config, "monitor");
    Accumulo.init(fs, config, app);
     Monitor monitor = new Monitor();
    Accumulo.enableTracing(hostname, "monitor");
    Accumulo.enableTracing(hostname, app);
     monitor.run(hostname);
   }
 
@@ -488,7 +491,7 @@ public class Monitor {
 
       }
     }), "Data fetcher").start();
    

     new Daemon(new LoggingRunnable(log, new Runnable() {
       @Override
       public void run() {
@@ -503,7 +506,7 @@ public class Monitor {
       }
     }), "Scan scanner").start();
   }
  

   public static class ScanStats {
     public final List<ActiveScan> scans;
     public final long fetched;
diff --git a/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java b/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
index 7eb950426..e3ae0c865 100644
-- a/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
++ b/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
@@ -285,14 +285,16 @@ public class TraceServer implements Watcher {
   public static void main(String[] args) throws Exception {
     SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
     ServerOpts opts = new ServerOpts();
    opts.parseArgs("tracer", args);
    final String app = "tracer";
    opts.parseArgs(app, args);
    Accumulo.setupLogging(app);
     Instance instance = HdfsZooInstance.getInstance();
     ServerConfiguration conf = new ServerConfiguration(instance);
     VolumeManager fs = VolumeManagerImpl.get();
    Accumulo.init(fs, conf, "tracer");
    Accumulo.init(fs, conf, app);
     String hostname = opts.getAddress();
     TraceServer server = new TraceServer(conf, hostname);
    Accumulo.enableTracing(hostname, "tserver");
    Accumulo.enableTracing(hostname, app);
     server.run();
     log.info("tracer stopping");
   }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index 3e89ca805..23baf43f9 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -3661,16 +3661,18 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
   public static void main(String[] args) throws IOException {
     try {
       SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
      VolumeManager fs = VolumeManagerImpl.get();
       ServerOpts opts = new ServerOpts();
       opts.parseArgs("tserver", args);
       String hostname = opts.getAddress();
      Instance instance = HdfsZooInstance.getInstance();
      final String app = "tserver";
      Accumulo.setupLogging(app);
      final Instance instance = HdfsZooInstance.getInstance();
       ServerConfiguration conf = new ServerConfiguration(instance);
      VolumeManager fs = VolumeManagerImpl.get();
       Accumulo.init(fs, conf, "tserver");
       TabletServer server = new TabletServer(conf, fs);
       server.config(hostname);
      Accumulo.enableTracing(hostname, "tserver");
      Accumulo.enableTracing(hostname, app);
       server.run();
     } catch (Exception ex) {
       log.error("Uncaught exception in TabletServer.main, exiting", ex);
- 
2.19.1.windows.1

