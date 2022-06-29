From db2dda1b7b6431a2ee57148db2de74c0432b9480 Mon Sep 17 00:00:00 2001
From: Billie Rinaldi <billie@apache.org>
Date: Tue, 4 Nov 2014 10:16:11 -0800
Subject: [PATCH] ACCUMULO-898 convert accumulo to use htrace

--
 assemble/bin/stop-all.sh                      |   2 +-
 assemble/pom.xml                              |   4 +
 assemble/src/main/assemblies/component.xml    |   1 +
 core/pom.xml                                  |   4 +
 .../core/client/ClientConfiguration.java      |  34 +-
 .../apache/accumulo/core/conf/Property.java   |   4 +
 .../accumulo/core/conf/PropertyType.java      |   3 +
 .../core/trace}/AsyncSpanReceiver.java        | 101 ++-
 .../accumulo/core/trace/DistributedTrace.java | 208 ++++-
 .../core/trace}/SendSpansViaThrift.java       |  30 +-
 .../apache/accumulo/core/trace/TraceDump.java |   3 +-
 .../accumulo/core/trace/TraceFormatter.java   |  17 +-
 .../accumulo/core/trace/ZooTraceClient.java   |  78 +-
 .../apache/accumulo/core/util/ThriftUtil.java |   7 +-
 .../main/asciidoc/chapters/administration.txt | 106 ++-
 .../main/resources/distributedTracing.html    |  13 +-
 .../simple/client/TracingExample.java         |  29 +-
 .../minicluster/MiniAccumuloInstance.java     |   2 +-
 pom.xml                                       |   6 +
 .../org/apache/accumulo/server/Accumulo.java  |   9 -
 .../accumulo/server/init/Initialize.java      |   1 -
 .../server/trace/TraceFSDataInputStream.java  |  90 --
 .../server/trace/TraceFileSystem.java         | 818 ------------------
 .../accumulo/server/util/AccumuloStatus.java  |   3 +-
 .../apache/accumulo/server/util/ZooZap.java   |   7 +-
 .../accumulo/gc/SimpleGarbageCollector.java   |  17 +-
 .../org/apache/accumulo/master/Master.java    |   5 +-
 .../master/replication/ReplicationDriver.java |   9 +-
 .../org/apache/accumulo/monitor/Monitor.java  |   9 +-
 .../monitor/servlets/trace/ShowTrace.java     |  26 +-
 .../monitor/ShowTraceLinkTypeTest.java        |  60 +-
 .../apache/accumulo/tracer/TraceServer.java   |  21 +-
 .../tserver/BulkFailedCopyProcessor.java      |   7 +-
 .../apache/accumulo/tserver/InMemoryMap.java  |   5 +-
 .../apache/accumulo/tserver/TabletServer.java |   5 +-
 .../replication/AccumuloReplicaSystem.java    |   2 +-
 .../accumulo/tserver/tablet/Tablet.java       |   7 +-
 .../java/org/apache/accumulo/shell/Shell.java |   5 +-
 .../accumulo/shell/commands/TraceCommand.java |   2 +-
 .../org/apache/accumulo/test/TestIngest.java  |  12 +-
 .../apache/accumulo/test/VerifyIngest.java    |   9 +-
 .../accumulo/test/ConditionalWriterIT.java    |   3 +-
 .../accumulo/test/functional/BulkFileIT.java  |   3 +-
 .../accumulo/test/functional/ExamplesIT.java  |   2 +-
 .../accumulo/test/functional/SimpleMacIT.java |  18 +-
 trace/pom.xml                                 |   4 +
 .../trace/instrument/CloudtraceSpan.java      |  71 ++
 .../trace/instrument/CountSampler.java        |  18 +-
 .../accumulo/trace/instrument/Sampler.java    |   6 +-
 .../accumulo/trace/instrument/Span.java       | 213 ++++-
 .../accumulo/trace/instrument/Trace.java      | 133 ++-
 .../trace/instrument/TraceCallable.java       |  24 +-
 .../instrument/TraceExecutorService.java      |  12 +
 .../accumulo/trace/instrument/TraceProxy.java |  72 --
 .../trace/instrument/TraceRunnable.java       |  20 +-
 .../accumulo/trace/instrument/Tracer.java     | 127 +--
 .../trace/instrument/impl/MilliSpan.java      | 141 ---
 .../trace/instrument/impl/NullSpan.java       | 102 ---
 .../trace/instrument/impl/RootMilliSpan.java  |  43 -
 .../trace/instrument/receivers/LogSpans.java  |  63 --
 .../instrument/receivers/SpanReceiver.java    |  28 -
 .../instrument/receivers/ZooSpanClient.java   | 122 ---
 .../accumulo/trace/thrift/Annotation.java     | 502 +++++++++++
 .../accumulo/trace/thrift/RemoteSpan.java     | 224 ++++-
 trace/src/main/thrift/trace.thrift            |   8 +-
 .../accumulo/trace/instrument/TracerTest.java |  63 +-
 66 files changed, 1803 insertions(+), 2000 deletions(-)
 rename {trace/src/main/java/org/apache/accumulo/trace/instrument/receivers => core/src/main/java/org/apache/accumulo/core/trace}/AsyncSpanReceiver.java (56%)
 rename {trace/src/main/java/org/apache/accumulo/trace/instrument/receivers => core/src/main/java/org/apache/accumulo/core/trace}/SendSpansViaThrift.java (85%)
 delete mode 100644 server/base/src/main/java/org/apache/accumulo/server/trace/TraceFSDataInputStream.java
 delete mode 100644 server/base/src/main/java/org/apache/accumulo/server/trace/TraceFileSystem.java
 create mode 100644 trace/src/main/java/org/apache/accumulo/trace/instrument/CloudtraceSpan.java
 delete mode 100644 trace/src/main/java/org/apache/accumulo/trace/instrument/TraceProxy.java
 delete mode 100644 trace/src/main/java/org/apache/accumulo/trace/instrument/impl/MilliSpan.java
 delete mode 100644 trace/src/main/java/org/apache/accumulo/trace/instrument/impl/NullSpan.java
 delete mode 100644 trace/src/main/java/org/apache/accumulo/trace/instrument/impl/RootMilliSpan.java
 delete mode 100644 trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/LogSpans.java
 delete mode 100644 trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/SpanReceiver.java
 delete mode 100644 trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/ZooSpanClient.java
 create mode 100644 trace/src/main/java/org/apache/accumulo/trace/thrift/Annotation.java

diff --git a/assemble/bin/stop-all.sh b/assemble/bin/stop-all.sh
index 4bf06c033..0af0ee159 100755
-- a/assemble/bin/stop-all.sh
++ b/assemble/bin/stop-all.sh
@@ -64,5 +64,5 @@ done
 "${bin}/tdown.sh"
 
 echo "Cleaning all server entries in ZooKeeper"
"$ACCUMULO_HOME/bin/accumulo" org.apache.accumulo.server.util.ZooZap -master -tservers -tracers
"$ACCUMULO_HOME/bin/accumulo" org.apache.accumulo.server.util.ZooZap -master -tservers -tracers --site-file "$ACCUMULO_CONF_DIR/accumulo-site.xml"
 
diff --git a/assemble/pom.xml b/assemble/pom.xml
index 89a374776..c764091bd 100644
-- a/assemble/pom.xml
++ b/assemble/pom.xml
@@ -160,6 +160,10 @@
       <groupId>org.eclipse.jetty</groupId>
       <artifactId>jetty-util</artifactId>
     </dependency>
    <dependency>
      <groupId>org.htrace</groupId>
      <artifactId>htrace-core</artifactId>
    </dependency>
     <dependency>
       <groupId>org.slf4j</groupId>
       <artifactId>slf4j-api</artifactId>
diff --git a/assemble/src/main/assemblies/component.xml b/assemble/src/main/assemblies/component.xml
index 599d26c7d..3f18da376 100644
-- a/assemble/src/main/assemblies/component.xml
++ b/assemble/src/main/assemblies/component.xml
@@ -42,6 +42,7 @@
         <include>org.eclipse.jetty:jetty-server</include>
         <include>org.eclipse.jetty:jetty-servlet</include>
         <include>org.eclipse.jetty:jetty-util</include>
        <include>org.htrace:htrace-core</include>
         <include>org.slf4j:slf4j-api</include>
         <include>org.slf4j:slf4j-log4j12</include>
       </includes>
diff --git a/core/pom.xml b/core/pom.xml
index 10e7d7176..1cbc6dfdd 100644
-- a/core/pom.xml
++ b/core/pom.xml
@@ -102,6 +102,10 @@
       <groupId>org.apache.zookeeper</groupId>
       <artifactId>zookeeper</artifactId>
     </dependency>
    <dependency>
      <groupId>org.htrace</groupId>
      <artifactId>htrace-core</artifactId>
    </dependency>
     <dependency>
       <groupId>org.slf4j</groupId>
       <artifactId>slf4j-api</artifactId>
diff --git a/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java b/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java
index 39b460d67..6fe61a51b 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java
@@ -21,8 +21,11 @@ import java.io.File;
 import java.io.StringReader;
 import java.io.StringWriter;
 import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
import java.util.Map;
 import java.util.UUID;
 
 import org.apache.accumulo.core.conf.Property;
@@ -56,7 +59,10 @@ public class ClientConfiguration extends CompositeConfiguration {
     INSTANCE_ZK_HOST(Property.INSTANCE_ZK_HOST),
     INSTANCE_ZK_TIMEOUT(Property.INSTANCE_ZK_TIMEOUT),
     INSTANCE_NAME("instance.name", null, PropertyType.STRING, "Name of Accumulo instance to connect to"),
    INSTANCE_ID("instance.id", null, PropertyType.STRING, "UUID of Accumulo instance to connect to"), ;
    INSTANCE_ID("instance.id", null, PropertyType.STRING, "UUID of Accumulo instance to connect to"),
    TRACE_SPAN_RECEIVERS(Property.TRACE_SPAN_RECEIVERS),
    TRACE_SPAN_RECEIVER_PREFIX(Property.TRACE_SPAN_RECEIVER_PREFIX),
    TRACE_ZK_PATH(Property.TRACE_ZK_PATH);
 
     private String key;
     private String defaultValue;
@@ -208,6 +214,32 @@ public class ClientConfiguration extends CompositeConfiguration {
       return prop.getDefaultValue();
   }
 
  private void checkType(ClientProperty property, PropertyType type) {
    if (!property.getType().equals(type)) {
      String msg = "Configuration method intended for type " + type + " called with a " + property.getType() + " argument (" + property.getKey() + ")";
      throw new IllegalArgumentException(msg);
    }
  }

  /**
   * Gets all properties under the given prefix in this configuration.
   *
   * @param property prefix property, must be of type PropertyType.PREFIX
   * @return a map of property keys to values
   * @throws IllegalArgumentException if property is not a prefix
   */
  public Map<String,String> getAllPropertiesWithPrefix(ClientProperty property) {
    checkType(property, PropertyType.PREFIX);

    Map<String,String> propMap = new HashMap<String,String>();
    Iterator<?> iter = this.getKeys(property.getKey());
    while (iter.hasNext()) {
      String p = (String)iter.next();
      propMap.put(p, getString(p));
    }
    return propMap;
  }

   /**
    * Sets the value of property to value
    *
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/Property.java b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
index a5587601f..fe313c14c 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/Property.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
@@ -27,6 +27,7 @@ import java.util.Map.Entry;
 import java.util.Properties;
 import java.util.Set;
 
import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.file.rfile.RFile;
 import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
@@ -329,6 +330,9 @@ public enum Property {
       + "the date shown on the 'Recent Logs' monitor page"),
 
   TRACE_PREFIX("trace.", null, PropertyType.PREFIX, "Properties in this category affect the behavior of distributed tracing."),
  TRACE_SPAN_RECEIVERS("trace.span.receivers", "org.apache.accumulo.core.trace.ZooTraceClient", PropertyType.CLASSNAMELIST, "A list of span receiver classes to send trace spans"),
  TRACE_SPAN_RECEIVER_PREFIX("trace.span.receiver.", null, PropertyType.PREFIX, "Prefix for span receiver configuration properties"),
  TRACE_ZK_PATH("trace.span.receiver.zookeeper.path", Constants.ZTRACERS, PropertyType.STRING, "The zookeeper node where tracers are registered"),
   TRACE_PORT("trace.port.client", "12234", PropertyType.PORT, "The listening port for the trace server"),
   TRACE_TABLE("trace.table", "trace", PropertyType.STRING, "The name of the table to store distributed traces"),
   TRACE_USER("trace.user", "root", PropertyType.STRING, "The name of the user to store distributed traces"),
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java b/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java
index fc20535c5..bf39da9b0 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java
@@ -68,6 +68,9 @@ public enum PropertyType {
   CLASSNAME("java class", "[\\w$.]*", "A fully qualified java class name representing a class on the classpath.\n"
       + "An example is 'java.lang.String', rather than 'String'"),
 
  CLASSNAMELIST("java class list", "[\\w$.,]*", "A list of fully qualified java class names representing classes on the classpath.\n"
      + "An example is 'java.lang.String', rather than 'String'"),

   DURABILITY("durability", "(?:none|log|flush|sync)", "One of 'none', 'log', 'flush' or 'sync'."),
 
   STRING("string", ".*",
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/AsyncSpanReceiver.java b/core/src/main/java/org/apache/accumulo/core/trace/AsyncSpanReceiver.java
similarity index 56%
rename from trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/AsyncSpanReceiver.java
rename to core/src/main/java/org/apache/accumulo/core/trace/AsyncSpanReceiver.java
index 4eebd6903..379302e42 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/AsyncSpanReceiver.java
++ b/core/src/main/java/org/apache/accumulo/core/trace/AsyncSpanReceiver.java
@@ -14,19 +14,29 @@
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package org.apache.accumulo.trace.instrument.receivers;
package org.apache.accumulo.core.trace;
 
import org.apache.accumulo.trace.thrift.Annotation;
import org.apache.accumulo.trace.thrift.RemoteSpan;
import org.apache.log4j.Logger;
import org.htrace.HTraceConfiguration;
import org.htrace.Span;
import org.htrace.SpanReceiver;
import org.htrace.TimelineAnnotation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
 import java.util.AbstractQueue;
import java.util.ArrayList;
 import java.util.HashMap;
import java.util.List;
 import java.util.Map;
import java.util.Map.Entry;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.concurrent.ConcurrentLinkedQueue;
 
import org.apache.accumulo.trace.thrift.RemoteSpan;
import org.apache.log4j.Logger;


 /**
  * Deliver Span information periodically to a destination.
  * <ul>
@@ -36,26 +46,28 @@ import org.apache.log4j.Logger;
  * </ul>
  */
 public abstract class AsyncSpanReceiver<SpanKey,Destination> implements SpanReceiver {
  

   private static final Logger log = Logger.getLogger(AsyncSpanReceiver.class);
   
   private final Map<SpanKey,Destination> clients = new HashMap<SpanKey,Destination>();
   
  protected final String host;
  protected final String service;
  protected String host = null;
  protected String service = null;
   
   protected abstract Destination createDestination(SpanKey key) throws Exception;
   
   protected abstract void send(Destination resource, RemoteSpan span) throws Exception;
   
  protected abstract SpanKey getSpanKey(Map<String,String> data);
  protected abstract SpanKey getSpanKey(Map<ByteBuffer,ByteBuffer> data);
   
   Timer timer = new Timer("SpanSender", true);
  final AbstractQueue<RemoteSpan> sendQueue = new ConcurrentLinkedQueue<RemoteSpan>();
  
  public AsyncSpanReceiver(String host, String service, long millis) {
    this.host = host;
    this.service = service;
  protected final AbstractQueue<RemoteSpan> sendQueue = new ConcurrentLinkedQueue<RemoteSpan>();

  public AsyncSpanReceiver() {
    this(1000);
  }

  public AsyncSpanReceiver(long millis) {
     timer.schedule(new TimerTask() {
       @Override
       public void run() {
@@ -68,18 +80,11 @@ public abstract class AsyncSpanReceiver<SpanKey,Destination> implements SpanRece
       
     }, millis, millis);
   }
  
  void sendSpans() {

  protected void sendSpans() {
     while (!sendQueue.isEmpty()) {
       boolean sent = false;
       RemoteSpan s = sendQueue.peek();
      if (s.stop - s.start < 1) {
        synchronized (sendQueue) {
          sendQueue.remove();
          sendQueue.notifyAll();
        }
        continue;
      }
       SpanKey dest = getSpanKey(s.data);
       Destination client = clients.get(dest);
       if (client == null) {
@@ -98,25 +103,48 @@ public abstract class AsyncSpanReceiver<SpanKey,Destination> implements SpanRece
           }
           sent = true;
         } catch (Exception ex) {
          log.error(ex, ex);
          log.warn("Got error sending to " + dest + ", refreshing client", ex);
          clients.remove(dest);
         }
       }
       if (!sent)
         break;
     }
   }
  

  public static Map<ByteBuffer, ByteBuffer> convertToByteBuffers(Map<byte[], byte[]> bytesMap) {
    if (bytesMap == null)
      return null;
    Map<ByteBuffer, ByteBuffer> result = new HashMap<ByteBuffer, ByteBuffer>();
    for (Entry<byte[], byte[]> bytes : bytesMap.entrySet()) {
      result.put(ByteBuffer.wrap(bytes.getKey()), ByteBuffer.wrap(bytes.getValue()));
    }
    return result;
  }

  public static List<Annotation> convertToAnnotations(List<TimelineAnnotation> annotations) {
    if (annotations == null)
      return null;
    List<Annotation> result = new ArrayList<Annotation>();
    for (TimelineAnnotation annotation : annotations) {
      result.add(new Annotation(annotation.getTime(), annotation.getMessage()));
    }
    return result;
  }

   @Override
  public void span(long traceId, long spanId, long parentId, long start, long stop, String description, Map<String,String> data) {
    
  public void receiveSpan(Span s) {
    Map<ByteBuffer, ByteBuffer> data = convertToByteBuffers(s.getKVAnnotations());
     SpanKey dest = getSpanKey(data);
     if (dest != null) {
      sendQueue.add(new RemoteSpan(host, service, traceId, spanId, parentId, start, stop, description, data));
      List<Annotation> annotations = convertToAnnotations(s.getTimelineAnnotations());
      sendQueue.add(new RemoteSpan(host, service==null ? s.getProcessId() : service, s.getTraceId(), s.getSpanId(), s.getParentId(),
          s.getStartTimeMillis(), s.getStopTimeMillis(), s.getDescription(), data, annotations));
     }
   }
  

   @Override
  public void flush() {
  public void close() {
     synchronized (sendQueue) {
       while (!sendQueue.isEmpty()) {
         try {
@@ -128,5 +156,18 @@ public abstract class AsyncSpanReceiver<SpanKey,Destination> implements SpanRece
       }
     }
   }

  @Override
  public void configure(HTraceConfiguration conf) {
    host = conf.get(DistributedTrace.TRACE_HOST_PROPERTY, host);
    if (host == null) {
      try {
        host = InetAddress.getLocalHost().getCanonicalHostName().toString();
      } catch (UnknownHostException e) {
        host = "unknown";
      }
    }
    service = conf.get(DistributedTrace.TRACE_SERVICE_PROPERTY, service);
  }
   
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/trace/DistributedTrace.java b/core/src/main/java/org/apache/accumulo/core/trace/DistributedTrace.java
index 83f5c2664..fe9377edb 100644
-- a/core/src/main/java/org/apache/accumulo/core/trace/DistributedTrace.java
++ b/core/src/main/java/org/apache/accumulo/core/trace/DistributedTrace.java
@@ -17,27 +17,213 @@
 package org.apache.accumulo.core.trace;
 
 import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
 
import org.apache.accumulo.trace.instrument.Tracer;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.accumulo.trace.instrument.Trace;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.log4j.Logger;
 import org.apache.zookeeper.KeeperException;
import org.htrace.HTraceConfiguration;
import org.htrace.SpanReceiver;
 
 
/**
 * Utility class to enable tracing for Accumulo server processes.
 *
 */
 public class DistributedTrace {
  private static final Logger log = Logger.getLogger(DistributedTrace.class);

  private static final String HTRACE_CONF_PREFIX = "hadoop.";

  public static final String TRACE_HOST_PROPERTY = "trace.host";
  public static final String TRACE_SERVICE_PROPERTY = "trace.service";

  public static final String TRACER_ZK_HOST = "tracer.zookeeper.host";
  public static final String TRACER_ZK_TIMEOUT = "tracer.zookeeper.timeout";
  public static final String TRACER_ZK_PATH = "tracer.zookeeper.path";

  private static final HashSet<SpanReceiver> receivers = new HashSet<SpanReceiver>();

  /**
   * @deprecated since 1.7, use {@link DistributedTrace#enable(String, String, org.apache.accumulo.core.client.ClientConfiguration)} instead
   */
  @Deprecated
   public static void enable(Instance instance, ZooReader zoo, String application, String address) throws IOException, KeeperException, InterruptedException {
    String path = ZooUtil.getRoot(instance) + Constants.ZTRACERS;
    if (address == null) {
    enable(address, application);
  }

  /**
   * Enable tracing by setting up SpanReceivers for the current process.
   */
  public static void enable() {
    enable(null, null);
  }

  /**
   * Enable tracing by setting up SpanReceivers for the current process.
   * If service name is null, the simple name of the class will be used.
   */
  public static void enable(String service) {
    enable(null, service);
  }

  /**
   * Enable tracing by setting up SpanReceivers for the current process.
   * If host name is null, it will be determined.
   * If service name is null, the simple name of the class will be used.
   */
  public static void enable(String hostname, String service) {
    enable(hostname, service, ClientConfiguration.loadDefault());
  }

  /**
   * Enable tracing by setting up SpanReceivers for the current process.
   * If host name is null, it will be determined.
   * If service name is null, the simple name of the class will be used.
   * Properties required in the client configuration include {@link org.apache.accumulo.core.client.ClientConfiguration.ClientProperty#TRACE_SPAN_RECEIVERS} and any properties specific to the span receiver.
   */
  public static void enable(String hostname, String service, ClientConfiguration conf) {
    String spanReceivers = conf.get(ClientProperty.TRACE_SPAN_RECEIVERS);
    String zookeepers = conf.get(ClientProperty.INSTANCE_ZK_HOST);
    long timeout = AccumuloConfiguration.getTimeInMillis(conf.get(ClientProperty.INSTANCE_ZK_TIMEOUT));
    String zkPath = conf.get(ClientProperty.TRACE_ZK_PATH);
    Map<String,String> properties = conf.getAllPropertiesWithPrefix(ClientProperty.TRACE_SPAN_RECEIVER_PREFIX);
    enableTracing(hostname, service, spanReceivers, zookeepers, timeout, zkPath, properties);
  }

  /**
   * Enable tracing by setting up SpanReceivers for the current process.
   * If host name is null, it will be determined.
   * If service name is null, the simple name of the class will be used.
   */
  public static void enable(String hostname, String service, AccumuloConfiguration conf) {
    String spanReceivers = conf.get(Property.TRACE_SPAN_RECEIVERS);
    String zookeepers = conf.get(Property.INSTANCE_ZK_HOST);
    long timeout = conf.getTimeInMillis(Property.INSTANCE_ZK_TIMEOUT);
    String zkPath = conf.get(Property.TRACE_ZK_PATH);
    Map<String,String> properties = conf.getAllPropertiesWithPrefix(Property.TRACE_SPAN_RECEIVER_PREFIX);
    enableTracing(hostname, service, spanReceivers, zookeepers, timeout, zkPath, properties);
  }

  private static void enableTracing(String hostname, String service, String spanReceivers, String zookeepers, long timeout, String zkPath,
      Map<String,String> properties) {
    Configuration conf = new Configuration(false);
    conf.set(Property.TRACE_SPAN_RECEIVERS.toString(), spanReceivers);

    // remaining properties will be parsed through an HTraceConfiguration by SpanReceivers
    setProperty(conf, TRACER_ZK_HOST, zookeepers);
    setProperty(conf, TRACER_ZK_TIMEOUT, (int) timeout);
    setProperty(conf, TRACER_ZK_PATH, zkPath);
    for (Entry<String,String> property : properties.entrySet()) {
      setProperty(conf, property.getKey().substring(Property.TRACE_SPAN_RECEIVER_PREFIX.getKey().length()), property.getValue());
    }
    if (hostname != null) {
      setProperty(conf, TRACE_HOST_PROPERTY, hostname);
    }
    if (service != null) {
      setProperty(conf, TRACE_SERVICE_PROPERTY, service);
    }
    org.htrace.Trace.setProcessId(service);
    ShutdownHookManager.get().addShutdownHook(new Runnable() {
      public void run() {
        Trace.off();
        closeReceivers();
      }
    }, 0);
    loadSpanReceivers(conf);
  }

  /**
   * Disable tracing by closing SpanReceivers for the current process.
   */
  public static void disable() {
    closeReceivers();
  }

  private static synchronized void loadSpanReceivers(Configuration conf) {
    if (!receivers.isEmpty()) {
      log.info("Already loaded span receivers, enable tracing does not need to be called again");
      return;
    }
    Class<?> implClass = null;
    String[] receiverNames = conf.getTrimmedStrings(Property.TRACE_SPAN_RECEIVERS.toString());
    if (receiverNames == null || receiverNames.length == 0) {
      return;
    }
    for (String className : receiverNames) {
      try {
        implClass = Class.forName(className);
        receivers.add(loadInstance(implClass, conf));
        log.info("SpanReceiver " + className + " was loaded successfully.");
      } catch (ClassNotFoundException e) {
        log.warn("Class " + className + " cannot be found.", e);
      } catch (IOException e) {
        log.warn("Load SpanReceiver " + className + " failed.", e);
      }
    }
    for (SpanReceiver rcvr : receivers) {
      org.htrace.Trace.addReceiver(rcvr);
    }
  }

  private static SpanReceiver loadInstance(Class<?> implClass, Configuration conf) throws IOException {
    SpanReceiver impl;
    try {
      Object o = ReflectionUtils.newInstance(implClass, conf);
      impl = (SpanReceiver)o;
      impl.configure(wrapHadoopConf(conf));
    } catch (SecurityException e) {
      throw new IOException(e);
    } catch (IllegalArgumentException e) {
      throw new IOException(e);
    } catch (RuntimeException e) {
      throw new IOException(e);
    }

    return impl;
  }

  private static void setProperty(Configuration conf, String key, String value) {
    conf.set(HTRACE_CONF_PREFIX + key, value);
  }

  private static void setProperty(Configuration conf, String key, int value) {
    conf.setInt(HTRACE_CONF_PREFIX + key, value);
  }

  private static HTraceConfiguration wrapHadoopConf(final Configuration conf) {
    return new HTraceConfiguration() {
      @Override
      public String get(String key) {
        return conf.get(HTRACE_CONF_PREFIX + key);
      }

      @Override
      public String get(String key, String defaultValue) {
        return conf.get(HTRACE_CONF_PREFIX + key, defaultValue);
      }
    };
  }

  private static synchronized void closeReceivers() {
    for (SpanReceiver rcvr : receivers) {
       try {
        address = InetAddress.getLocalHost().getHostAddress().toString();
      } catch (UnknownHostException e) {
        address = "unknown";
        rcvr.close();
      } catch (IOException e) {
        log.warn("Unable to close SpanReceiver correctly: " + e.getMessage(), e);
       }
     }
    Tracer.getInstance().addReceiver(new ZooTraceClient(zoo, path, address, application, 1000));
    receivers.clear();
   }
 }
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/SendSpansViaThrift.java b/core/src/main/java/org/apache/accumulo/core/trace/SendSpansViaThrift.java
similarity index 85%
rename from trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/SendSpansViaThrift.java
rename to core/src/main/java/org/apache/accumulo/core/trace/SendSpansViaThrift.java
index 4967d97e9..87a937856 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/SendSpansViaThrift.java
++ b/core/src/main/java/org/apache/accumulo/core/trace/SendSpansViaThrift.java
@@ -14,11 +14,9 @@
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package org.apache.accumulo.trace.instrument.receivers;
package org.apache.accumulo.core.trace;
 
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import static java.nio.charset.StandardCharsets.UTF_8;
 
 import org.apache.accumulo.trace.thrift.RemoteSpan;
 import org.apache.accumulo.trace.thrift.SpanReceiver.Client;
@@ -27,6 +25,10 @@ import org.apache.thrift.protocol.TProtocol;
 import org.apache.thrift.transport.TSocket;
 import org.apache.thrift.transport.TTransport;
 
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
 
 /**
  * Send Span data to a destination using thrift.
@@ -36,9 +38,13 @@ public class SendSpansViaThrift extends AsyncSpanReceiver<String,Client> {
   private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SendSpansViaThrift.class);
   
   private static final String THRIFT = "thrift://";
  
  public SendSpansViaThrift(String host, String service, long millis) {
    super(host, service, millis);

  public SendSpansViaThrift() {
    super();
  }

  public SendSpansViaThrift(long millis) {
    super(millis);
   }
   
   @Override
@@ -69,13 +75,15 @@ public class SendSpansViaThrift extends AsyncSpanReceiver<String,Client> {
         client.span(s);
       } catch (Exception ex) {
         client.getInputProtocol().getTransport().close();
        client = null;
        throw ex;
       }
     }
   }
  
  protected String getSpanKey(Map<String,String> data) {
    String dest = data.get("dest");

  private static final ByteBuffer DEST = ByteBuffer.wrap("dest".getBytes(UTF_8));

  protected String getSpanKey(Map<ByteBuffer,ByteBuffer> data) {
    String dest = new String(data.get(DEST).array());
     if (dest != null && dest.startsWith(THRIFT)) {
       String hostAddress = dest.substring(THRIFT.length());
       String[] hostAddr = hostAddress.split(":", 2);
diff --git a/core/src/main/java/org/apache/accumulo/core/trace/TraceDump.java b/core/src/main/java/org/apache/accumulo/core/trace/TraceDump.java
index b44cc3ea3..e3f9e5a36 100644
-- a/core/src/main/java/org/apache/accumulo/core/trace/TraceDump.java
++ b/core/src/main/java/org/apache/accumulo/core/trace/TraceDump.java
@@ -37,6 +37,7 @@ import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.hadoop.io.Text;
import org.htrace.Span;
 
 import com.beust.jcommander.Parameter;
 
@@ -134,7 +135,7 @@ public class TraceDump {
       RemoteSpan span = TraceFormatter.getRemoteSpan(entry);
       tree.addNode(span);
       start = min(start, span.start);
      if (span.parentId <= 0)
      if (span.parentId == Span.ROOT_SPAN_ID)
         count++;
     }
     out.print(String.format("Trace started at %s", TraceFormatter.formatDate(new Date(start))));
diff --git a/core/src/main/java/org/apache/accumulo/core/trace/TraceFormatter.java b/core/src/main/java/org/apache/accumulo/core/trace/TraceFormatter.java
index 9d860d99a..d6842dfd0 100644
-- a/core/src/main/java/org/apache/accumulo/core/trace/TraceFormatter.java
++ b/core/src/main/java/org/apache/accumulo/core/trace/TraceFormatter.java
@@ -16,11 +16,15 @@
  */
 package org.apache.accumulo.core.trace;
 
import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.Map.Entry;
 
import org.apache.accumulo.trace.thrift.Annotation;
 import org.apache.accumulo.trace.thrift.RemoteSpan;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
@@ -88,8 +92,17 @@ public class TraceFormatter implements Formatter {
       result.append(String.format(" %12s:%s%n", "parent", Long.toHexString(span.parentId)));
       result.append(String.format(" %12s:%s%n", "start", dateFormatter.format(span.start)));
       result.append(String.format(" %12s:%s%n", "ms", span.stop - span.start));
      for (Entry<String,String> entry : span.data.entrySet()) {
        result.append(String.format(" %12s:%s%n", entry.getKey(), entry.getValue()));
      if (span.data != null) {
        for (Entry<ByteBuffer, ByteBuffer> entry : span.data.entrySet()) {
          String key = new String(entry.getKey().array(), entry.getKey().arrayOffset(), entry.getKey().limit(), UTF_8);
          String value = new String(entry.getValue().array(), entry.getValue().arrayOffset(), entry.getValue().limit(), UTF_8);
          result.append(String.format(" %12s:%s%n", key, value));
        }
      }
      if (span.annotations != null) {
        for (Annotation annotation : span.annotations) {
          result.append(String.format(" %12s:%s:%s%n", "annotation", annotation.getMsg(), dateFormatter.format(annotation.getTime())));
        }
       }
       
       if (printTimeStamps) {
diff --git a/core/src/main/java/org/apache/accumulo/core/trace/ZooTraceClient.java b/core/src/main/java/org/apache/accumulo/core/trace/ZooTraceClient.java
index 9586eaa62..f53f13385 100644
-- a/core/src/main/java/org/apache/accumulo/core/trace/ZooTraceClient.java
++ b/core/src/main/java/org/apache/accumulo/core/trace/ZooTraceClient.java
@@ -18,57 +18,93 @@ package org.apache.accumulo.core.trace;
 
 import static java.nio.charset.StandardCharsets.UTF_8;
 
import java.io.IOException;
import java.nio.ByteBuffer;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
 
import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.accumulo.trace.instrument.receivers.SendSpansViaThrift;
 import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
 import org.apache.zookeeper.WatchedEvent;
 import org.apache.zookeeper.Watcher;

import org.htrace.HTraceConfiguration;
 
 /**
  * Find a Span collector via zookeeper and push spans there via Thrift RPC
 * 
  */
 public class ZooTraceClient extends SendSpansViaThrift implements Watcher {
  
   private static final Logger log = Logger.getLogger(ZooTraceClient.class);
  
  final ZooReader zoo;
  final String path;

  private static final int DEFAULT_TIMEOUT = 30 * 1000;

  ZooReader zoo = null;
  String path;
  boolean pathExists = false;
   final Random random = new Random();
   final List<String> hosts = new ArrayList<String>();
  
  public ZooTraceClient(ZooReader zoo, String path, String host, String service, long millis) throws IOException, KeeperException, InterruptedException {
    super(host, service, millis);
    this.path = path;
    this.zoo = zoo;
    updateHosts(path, zoo.getChildren(path, this));

  public ZooTraceClient() {
    super();
   }
  

  public ZooTraceClient(long millis) {
    super(millis);
  }

   @Override
  synchronized protected String getSpanKey(Map<String,String> data) {
  synchronized protected String getSpanKey(Map<ByteBuffer,ByteBuffer> data) {
     if (hosts.size() > 0) {
      return hosts.get(random.nextInt(hosts.size()));
      String host = hosts.get(random.nextInt(hosts.size()));
      log.debug("sending data to " + host);
      return host;
     }
     return null;
   }
  

  @Override
  public void configure(HTraceConfiguration conf) {
    super.configure(conf);
    String keepers = conf.get(DistributedTrace.TRACER_ZK_HOST);
    if (keepers == null)
      throw new IllegalArgumentException("Must configure " + DistributedTrace.TRACER_ZK_HOST);
    int timeout = conf.getInt(DistributedTrace.TRACER_ZK_TIMEOUT, DEFAULT_TIMEOUT);
    zoo = new ZooReader(keepers, timeout);
    path = conf.get(DistributedTrace.TRACER_ZK_PATH, Constants.ZTRACERS);
    process(null);
  }

   @Override
   public void process(WatchedEvent event) {
    log.debug("Processing event for trace server zk watch");
     try {
      updateHosts(path, zoo.getChildren(path, null));
      if (pathExists || zoo.exists(path)) {
        pathExists = true;
        updateHosts(path, zoo.getChildren(path, this));
      } else {
        zoo.exists(path, this);
      }
     } catch (Exception ex) {
       log.error("unable to get destination hosts in zookeeper", ex);
     }
   }
  

  @Override
  protected void sendSpans() {
    if (hosts.isEmpty()) {
      if (!sendQueue.isEmpty()) {
        log.error("No hosts to send data to, dropping queued spans");
        synchronized (sendQueue) {
          sendQueue.clear();
          sendQueue.notifyAll();
        }
      }
    } else {
      super.sendSpans();
    }
  }

   synchronized private void updateHosts(String path, List<String> children) {
     log.debug("Scanning trace hosts in zookeeper: " + path);
     try {
diff --git a/core/src/main/java/org/apache/accumulo/core/util/ThriftUtil.java b/core/src/main/java/org/apache/accumulo/core/util/ThriftUtil.java
index da4e56781..0edc88416 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/ThriftUtil.java
++ b/core/src/main/java/org/apache/accumulo/core/util/ThriftUtil.java
@@ -54,19 +54,18 @@ public class ThriftUtil {
   private static final Logger log = Logger.getLogger(ThriftUtil.class);
 
   public static class TraceProtocol extends TCompactProtocol {
    private Span span = null;
 
     @Override
     public void writeMessageBegin(TMessage message) throws TException {
      Trace.start("client:" + message.name);
      span = Trace.start("client:" + message.name);
       super.writeMessageBegin(message);
     }
 
     @Override
     public void writeMessageEnd() throws TException {
       super.writeMessageEnd();
      Span currentTrace = Trace.currentTrace();
      if (currentTrace != null)
        currentTrace.stop();
      span.stop();
     }
 
     public TraceProtocol(TTransport transport) {
diff --git a/docs/src/main/asciidoc/chapters/administration.txt b/docs/src/main/asciidoc/chapters/administration.txt
index d5e73f0e7..e9e012634 100644
-- a/docs/src/main/asciidoc/chapters/administration.txt
++ b/docs/src/main/asciidoc/chapters/administration.txt
@@ -386,47 +386,115 @@ the following properties
   trace.user
   trace.token.property.password
 
Other tracer configuration properties include

  trace.port.client
  trace.table

==== Configuring Tracing
Traces are collected via SpanReceivers. The default SpanReceiver
configured is org.apache.accumulo.core.trace.ZooTraceClient, which
sends spans to an Accumulo Tracer process, as discussed in the
previous section. This default can be changed to a different span
receiver, or additional span receivers can be added in a
comma-separated list, by modifying the property

  trace.span.receivers

Individual span receivers may require their own configuration
parameters, which are grouped under the trace.span.receiver.*
prefix.  The ZooTraceClient requires the following property that
indicates where the tracer servers will register themselves in
ZooKeeper.

  trace.span.receiver.zookeeper.path

This is configured to /tracers by default.  If multiple Accumulo
instances are sharing the same ZooKeeper quorum, take care to
configure Accumulo with unique values for this property.

Hadoop can also be configured to send traces to Accumulo, as of
Hadoop 2.6.0, by setting the following properties in Hadoop's
core-site.xml file (the path property is optional if left as the
default).

  <property>
    <name>hadoop.htrace.spanreceiver.classes</name>
    <value>org.apache.accumulo.core.trace.ZooTraceClient</value>
  </property>
  <property>
    <name>hadoop.tracer.zookeeper.host</name>
    <value>zookeeperHost:2181</value>
  </property>
  <property>
    <name>hadoop.tracer.zookeeper.path</name>
    <value>/tracers</value>
  </property>

The accumulo-core, accumulo-trace, and libthrift jars must also
be placed on Hadoop's classpath.

 ==== Instrumenting a Client
 Tracing can be used to measure a client operation, such as a scan, as
 the operation traverses the distributed system. To enable tracing for
 your application call
 
 [source,java]
DistributedTrace.enable(instance, new ZooReader(instance), hostname, "myApplication");
import org.apache.accumulo.core.trace.DistributedTrace;
...
DistributedTrace.enable(hostname, "myApplication");
// do some tracing
...
DistributedTrace.disable();
 
 Once tracing has been enabled, a client can wrap an operation in a trace.
 
 [source,java]
Trace.on("Client Scan");
import org.htrace.Sampler;
import org.htrace.Trace;
import org.htrace.TraceScope;
...
TraceScope scope = Trace.startSpan("Client Scan", Sampler.ALWAYS);
 BatchScanner scanner = conn.createBatchScanner(...);
 // Configure your scanner
 for (Entry entry : scanner) {
 }
Trace.off();
scope.close();
 
 Additionally, the user can create additional Spans within a Trace.
The sampler for the trace should only be specified with the first span, and subsequent spans will be collected depending on whether that first span was sampled.
 
 [source,java]
Trace.on("Client Update");
TraceScope scope = Trace.startSpan("Client Update", Sampler.ALWAYS);
 ...
Span readSpan = Trace.start("Read");
TraceScope readScope = Trace.startSpan("Read");
 ...
readSpan.stop();
readScope.close();
 ...
Span writeSpan = Trace.start("Write");
TraceScope writeScope = Trace.startSpan("Write");
 ...
writeSpan.stop();
Trace.off();
writeScope.close();
scope.close();
 
 Like Dapper, Accumulo tracing supports user defined annotations to associate additional data with a Trace.
Checking whether currently tracing is necessary when using a sampler other than Sampler.ALWAYS.
 
 [source,java]
 ...
 int numberOfEntriesRead = 0;
Span readSpan = Trace.start("Read");
TraceScope readScope = Trace.startSpan("Read");
 // Do the read, update the counter
 ...
readSpan.data("Number of Entries Read", String.valueOf(numberOfEntriesRead));
if (Trace.isTracing)
  readScope.getSpan().addKVAnnotation("Number of Entries Read".getBytes(StandardCharsets.UTF_8),
      String.valueOf(numberOfEntriesRead).getBytes(StandardCharsets.UTF_8));

It is also possible to add timeline annotations to your spans.
This associates a string with a given timestamp between the start and stop times for a span.

[source,java]
...
writeScope.getSpan().addTimelineAnnotation("Initiating Flush");
 
 Some client operations may have a high volume within your
 application. As such, you may wish to only sample a percentage of
@@ -434,18 +502,18 @@ operations for tracing. As seen below, the CountSampler can be used to
 help enable tracing for 1-in-1000 operations
 
 [source,java]
import org.htrace.impl.CountSampler;
...
 Sampler sampler = new CountSampler(1000);
 ...
if (sampler.next()) {
  Trace.on("Read");
}
TraceScope readScope = Trace.startSpan("Read", sampler);
 ...
Trace.offNoFlush();
readScope.close();

Remember to close all spans and disable tracing when finished.
 
It should be noted that it is safe to turn off tracing even if it
isn't currently active. The +Trace.offNoFlush()+ should be used if the
user does not wish to have +Trace.off()+ block while flushing trace
data.
[source,java]
DistributedTrace.disable();
 
 ==== Viewing Collected Traces
 To view collected traces, use the "Recent Traces" link on the Monitor
diff --git a/docs/src/main/resources/distributedTracing.html b/docs/src/main/resources/distributedTracing.html
index 54c9095d7..98438daf9 100644
-- a/docs/src/main/resources/distributedTracing.html
++ b/docs/src/main/resources/distributedTracing.html
@@ -30,13 +30,20 @@ distributed, and the typical lookup is fast.</p>
 <p>To provide insight into what accumulo is doing during your scan, you can turn on tracing before you do your operation:</p>
 
 <pre>
   DistributedTrace.enable(instance, zooReader, hostname, "myApplication");
   Trace scanTrace = Trace.on("client:scan");
   import org.apache.accumulo.core.trace.DistributedTrace;
   import org.htrace.Sampler;
   import org.htrace.Trace;
   import org.htrace.TraceScope;
   ...

   DistributedTrace.enable(hostname, "myApplication");
   TraceScope scanTrace = Trace.startSpan("client:scan", Sampler.ALWAYS);
    BatchScanner scanner = conn.createBatchScanner(...);
    // Configure your scanner
    for (Entry<Key, Value> entry : scanner) {
    }
   Trace.off();
   scanTrace.close();
   DistributedTrace.disableTracing();
 </pre>
 
 
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/client/TracingExample.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/client/TracingExample.java
index a542263fe..3a010a688 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/client/TracingExample.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/client/TracingExample.java
@@ -17,6 +17,7 @@
 
 package org.apache.accumulo.examples.simple.client;
 
import static java.nio.charset.StandardCharsets.UTF_8;
 import java.util.Map.Entry;
 
 import org.apache.accumulo.core.cli.ClientOnDefaultTable;
@@ -33,9 +34,9 @@ import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.trace.DistributedTrace;
import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.accumulo.trace.instrument.Span;
import org.apache.accumulo.trace.instrument.Trace;
import org.htrace.Sampler;
import org.htrace.Trace;
import org.htrace.TraceScope;
 
 import com.beust.jcommander.Parameter;
 
@@ -64,7 +65,7 @@ public class TracingExample {
   }
 
   public void enableTracing(Opts opts) throws Exception {
    DistributedTrace.enable(opts.getInstance(), new ZooReader(opts.getInstance().getZooKeepers(), 1000), "myHost", "myApp");
    DistributedTrace.enable("myHost", "myApp");
   }
 
   public void execute(Opts opts) throws TableNotFoundException, InterruptedException, AccumuloException, AccumuloSecurityException, TableExistsException {
@@ -91,22 +92,21 @@ public class TracingExample {
     // Trace the write operation. Note, unless you flush the BatchWriter, you will not capture
     // the write operation as it is occurs asynchronously. You can optionally create additional Spans
     // within a given Trace as seen below around the flush
    Trace.on("Client Write");
    TraceScope scope = Trace.startSpan("Client Write", Sampler.ALWAYS);
 
    System.out.println("TraceID: " + Long.toHexString(Trace.currentTrace().traceId()));
    System.out.println("TraceID: " + Long.toHexString(scope.getSpan().getTraceId()));
     BatchWriter batchWriter = opts.getConnector().createBatchWriter(opts.getTableName(), new BatchWriterConfig());
 
     Mutation m = new Mutation("row");
     m.put("cf", "cq", "value");
 
     batchWriter.addMutation(m);
    Span flushSpan = Trace.start("Client Flush");
    // You can add timeline annotations to Spans which will be able to be viewed in the Monitor
    scope.getSpan().addTimelineAnnotation("Initiating Flush");
     batchWriter.flush();
    flushSpan.stop();
 
    // Use Trace.offNoFlush() if you don't want the operation to block.
     batchWriter.close();
    Trace.off();
    scope.close();
   }
 
   private void readEntries(Opts opts) throws TableNotFoundException, AccumuloException, AccumuloSecurityException {
@@ -114,8 +114,8 @@ public class TracingExample {
     Scanner scanner = opts.getConnector().createScanner(opts.getTableName(), opts.auths);
 
     // Trace the read operation.
    Span readSpan = Trace.on("Client Read");
    System.out.println("TraceID: " + Long.toHexString(Trace.currentTrace().traceId()));
    TraceScope readScope = Trace.startSpan("Client Read", Sampler.ALWAYS);
    System.out.println("TraceID: " + Long.toHexString(readScope.getSpan().getTraceId()));
 
     int numberOfEntriesRead = 0;
     for (Entry<Key,Value> entry : scanner) {
@@ -123,9 +123,10 @@ public class TracingExample {
       ++numberOfEntriesRead;
     }
     // You can add additional metadata (key, values) to Spans which will be able to be viewed in the Monitor
    readSpan.data("Number of Entries Read", String.valueOf(numberOfEntriesRead));
    readScope.getSpan().addKVAnnotation("Number of Entries Read".getBytes(UTF_8),
        String.valueOf(numberOfEntriesRead).getBytes(UTF_8));
 
    Trace.off();
    readScope.close();
   }
 
   public static void main(String[] args) throws Exception {
diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java
index 54897cb21..e0c93a624 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java
@@ -50,7 +50,7 @@ public class MiniAccumuloInstance extends ZooKeeperInstance {
     }
   }
 
  private static String getZooKeepersFromDir(File directory) throws FileNotFoundException {
  public static String getZooKeepersFromDir(File directory) throws FileNotFoundException {
     if (!directory.isDirectory())
       throw new IllegalArgumentException("Not a directory " + directory.getPath());
     File configFile = new File(new File(directory, "conf"), "accumulo-site.xml");
diff --git a/pom.xml b/pom.xml
index ebc2f2f1f..e9338f4fe 100644
-- a/pom.xml
++ b/pom.xml
@@ -124,6 +124,7 @@
     <forkCount>1</forkCount>
     <!-- overwritten in profiles hadoop-1 or hadoop-2 -->
     <hadoop.version>2.2.0</hadoop.version>
    <htrace.version>3.0.4</htrace.version>
     <httpclient.version>3.1</httpclient.version>
     <jetty.version>9.1.5.v20140505</jetty.version>
     <!-- the maven-release-plugin makes this recommendation, due to plugin bugs -->
@@ -461,6 +462,11 @@
         <artifactId>jetty-util</artifactId>
         <version>${jetty.version}</version>
       </dependency>
      <dependency>
        <groupId>org.htrace</groupId>
        <artifactId>htrace-core</artifactId>
        <version>${htrace.version}</version>
      </dependency>
       <dependency>
         <groupId>org.mortbay.jetty</groupId>
         <artifactId>jetty</artifactId>
diff --git a/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java b/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
index 5c93a53c5..87c615ebb 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
++ b/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
@@ -32,7 +32,6 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.trace.DistributedTrace;
 import org.apache.accumulo.core.util.AddressUtil;
 import org.apache.accumulo.core.util.UtilWaitThread;
 import org.apache.accumulo.core.util.Version;
@@ -107,14 +106,6 @@ public class Accumulo {
     return ServerConstants.getInstanceIdLocation(v);
   }
 
  public static void enableTracing(String address, String application) {
    try {
      DistributedTrace.enable(HdfsZooInstance.getInstance(), ZooReaderWriter.getInstance(), application, address);
    } catch (Exception ex) {
      log.error("creating remote sink for trace spans", ex);
    }
  }

   /**
    * Finds the best log4j configuration file. A generic file is used only if an
    * application-specific file is not available. An XML file is preferred over
diff --git a/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java b/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java
index 24ff63750..a15e05e1f 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java
++ b/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java
@@ -477,7 +477,6 @@ public class Initialize {
     zoo.putPersistentData(zkInstanceRoot + RootTable.ZROOT_TABLET, EMPTY_BYTE_ARRAY, NodeExistsPolicy.FAIL);
     zoo.putPersistentData(zkInstanceRoot + RootTable.ZROOT_TABLET_WALOGS, EMPTY_BYTE_ARRAY, NodeExistsPolicy.FAIL);
     zoo.putPersistentData(zkInstanceRoot + RootTable.ZROOT_TABLET_PATH, rootTabletDir.getBytes(UTF_8), NodeExistsPolicy.FAIL);
    zoo.putPersistentData(zkInstanceRoot + Constants.ZTRACERS, EMPTY_BYTE_ARRAY, NodeExistsPolicy.FAIL);
     zoo.putPersistentData(zkInstanceRoot + Constants.ZMASTERS, EMPTY_BYTE_ARRAY, NodeExistsPolicy.FAIL);
     zoo.putPersistentData(zkInstanceRoot + Constants.ZMASTER_LOCK, EMPTY_BYTE_ARRAY, NodeExistsPolicy.FAIL);
     zoo.putPersistentData(zkInstanceRoot + Constants.ZMASTER_GOAL_STATE, MasterGoalState.NORMAL.toString().getBytes(UTF_8), NodeExistsPolicy.FAIL);
diff --git a/server/base/src/main/java/org/apache/accumulo/server/trace/TraceFSDataInputStream.java b/server/base/src/main/java/org/apache/accumulo/server/trace/TraceFSDataInputStream.java
deleted file mode 100644
index 5162e019f..000000000
-- a/server/base/src/main/java/org/apache/accumulo/server/trace/TraceFSDataInputStream.java
++ /dev/null
@@ -1,90 +0,0 @@
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
package org.apache.accumulo.server.trace;

import java.io.IOException;

import org.apache.accumulo.trace.instrument.Span;
import org.apache.accumulo.trace.instrument.Trace;
import org.apache.hadoop.fs.FSDataInputStream;


public class TraceFSDataInputStream extends FSDataInputStream {
  @Override
  public synchronized void seek(long desired) throws IOException {
    Span span = Trace.start("FSDataInputStream.seek");
    try {
      impl.seek(desired);
    } finally {
      span.stop();
    }
  }
  
  @Override
  public int read(long position, byte[] buffer, int offset, int length) throws IOException {
    Span span = Trace.start("FSDataInputStream.read");
    if (Trace.isTracing())
      span.data("length", Integer.toString(length));
    try {
      return impl.read(position, buffer, offset, length);
    } finally {
      span.stop();
    }
  }
  
  @Override
  public void readFully(long position, byte[] buffer, int offset, int length) throws IOException {
    Span span = Trace.start("FSDataInputStream.readFully");
    if (Trace.isTracing())
      span.data("length", Integer.toString(length));
    try {
      impl.readFully(position, buffer, offset, length);
    } finally {
      span.stop();
    }
  }
  
  @Override
  public void readFully(long position, byte[] buffer) throws IOException {
    Span span = Trace.start("FSDataInputStream.readFully");
    if (Trace.isTracing())
      span.data("length", Integer.toString(buffer.length));
    try {
      impl.readFully(position, buffer);
    } finally {
      span.stop();
    }
  }
  
  @Override
  public boolean seekToNewSource(long targetPos) throws IOException {
    Span span = Trace.start("FSDataInputStream.seekToNewSource");
    try {
      return impl.seekToNewSource(targetPos);
    } finally {
      span.stop();
    }
  }
  
  private final FSDataInputStream impl;
  
  public TraceFSDataInputStream(FSDataInputStream in) throws IOException {
    super(in);
    impl = in;
  }
  
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/trace/TraceFileSystem.java b/server/base/src/main/java/org/apache/accumulo/server/trace/TraceFileSystem.java
deleted file mode 100644
index d3fbad7c9..000000000
-- a/server/base/src/main/java/org/apache/accumulo/server/trace/TraceFileSystem.java
++ /dev/null
@@ -1,818 +0,0 @@
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
package org.apache.accumulo.server.trace;

import static com.google.common.base.Preconditions.checkArgument;
import java.io.IOException;
import java.net.URI;

import org.apache.accumulo.trace.instrument.Span;
import org.apache.accumulo.trace.instrument.Trace;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

// If FileSystem was an interface, we could use a Proxy, but it's not, so we have to override everything manually

public class TraceFileSystem extends FileSystem {

  @Override
  public void setConf(Configuration conf) {
    Span span = Trace.start("setConf");
    try {
      if (impl != null)
        impl.setConf(conf);
      else
        super.setConf(conf);
    } finally {
      span.stop();
    }
  }

  @Override
  public Configuration getConf() {
    Span span = Trace.start("getConf");
    try {
      return impl.getConf();
    } finally {
      span.stop();
    }
  }

  @Override
  public BlockLocation[] getFileBlockLocations(FileStatus file, long start, long len) throws IOException {
    Span span = Trace.start("getFileBlockLocations");
    try {
      return impl.getFileBlockLocations(file, start, len);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataInputStream open(Path f) throws IOException {
    Span span = Trace.start("open");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return new TraceFSDataInputStream(impl.open(f));
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f) throws IOException {
    Span span = Trace.start("create");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.create(f);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite) throws IOException {
    Span span = Trace.start("create");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.create(f, overwrite);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f, Progressable progress) throws IOException {
    Span span = Trace.start("create");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {

      return impl.create(f, progress);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f, short replication) throws IOException {
    Span span = Trace.start("create");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.create(f, replication);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f, short replication, Progressable progress) throws IOException {
    Span span = Trace.start("create");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.create(f, replication, progress);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize) throws IOException {
    Span span = Trace.start("create");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.create(f, overwrite, bufferSize);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize, Progressable progress) throws IOException {
    Span span = Trace.start("create");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.create(f, overwrite, bufferSize, progress);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize, short replication, long blockSize) throws IOException {
    Span span = Trace.start("create");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.create(f, overwrite, bufferSize, replication, blockSize);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException {
    Span span = Trace.start("create");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.create(f, overwrite, bufferSize, replication, blockSize, progress);
    } finally {
      span.stop();
    }
  }

  @Override
  public boolean createNewFile(Path f) throws IOException {
    Span span = Trace.start("createNewFile");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.createNewFile(f);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream append(Path f) throws IOException {
    Span span = Trace.start("append");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.append(f);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream append(Path f, int bufferSize) throws IOException {
    Span span = Trace.start("append");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.append(f, bufferSize);
    } finally {
      span.stop();
    }
  }

  @Deprecated
  @Override
  public short getReplication(Path src) throws IOException {
    Span span = Trace.start("getReplication");
    if (Trace.isTracing())
      span.data("path", src.toString());
    try {
      return impl.getFileStatus(src).getReplication();
    } finally {
      span.stop();
    }
  }

  @Override
  public boolean setReplication(Path src, short replication) throws IOException {
    Span span = Trace.start("setReplication");
    if (Trace.isTracing())
      span.data("path", src.toString());
    try {
      return impl.setReplication(src, replication);
    } finally {
      span.stop();
    }
  }

  @Override
  public boolean exists(Path f) throws IOException {
    Span span = Trace.start("exists");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.exists(f);
    } finally {
      span.stop();
    }
  }

  @Deprecated
  @Override
  public boolean isDirectory(Path f) throws IOException {
    Span span = Trace.start("isDirectory");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.getFileStatus(f).isDir();
    } finally {
      span.stop();
    }
  }

  @Override
  public boolean isFile(Path f) throws IOException {
    Span span = Trace.start("isFile");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.isFile(f);
    } finally {
      span.stop();
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public long getLength(Path f) throws IOException {
    Span span = Trace.start("getLength");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.getLength(f);
    } finally {
      span.stop();
    }
  }

  @Override
  public ContentSummary getContentSummary(Path f) throws IOException {
    Span span = Trace.start("getContentSummary");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.getContentSummary(f);
    } finally {
      span.stop();
    }
  }

  @Override
  public FileStatus[] listStatus(Path f, PathFilter filter) throws IOException {
    Span span = Trace.start("listStatus");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.listStatus(f, filter);
    } finally {
      span.stop();
    }
  }

  @Override
  public FileStatus[] listStatus(Path[] files) throws IOException {
    Span span = Trace.start("listStatus");
    try {
      return impl.listStatus(files);
    } finally {
      span.stop();
    }
  }

  @Override
  public FileStatus[] listStatus(Path[] files, PathFilter filter) throws IOException {
    Span span = Trace.start("listStatus");
    try {
      return impl.listStatus(files, filter);
    } finally {
      span.stop();
    }
  }

  @Override
  public FileStatus[] globStatus(Path pathPattern) throws IOException {
    Span span = Trace.start("globStatus");
    if (Trace.isTracing())
      span.data("pattern", pathPattern.toString());
    try {
      return impl.globStatus(pathPattern);
    } finally {
      span.stop();
    }
  }

  @Override
  public FileStatus[] globStatus(Path pathPattern, PathFilter filter) throws IOException {
    Span span = Trace.start("globStatus");
    if (Trace.isTracing())
      span.data("pattern", pathPattern.toString());
    try {
      return impl.globStatus(pathPattern, filter);
    } finally {
      span.stop();
    }
  }

  @Override
  public Path getHomeDirectory() {
    Span span = Trace.start("getHomeDirectory");
    try {
      return impl.getHomeDirectory();
    } finally {
      span.stop();
    }
  }

  @Override
  public boolean mkdirs(Path f) throws IOException {
    Span span = Trace.start("mkdirs");
    if (Trace.isTracing())
      span.data("path", f.toString());
    try {
      return impl.mkdirs(f);
    } finally {
      span.stop();
    }
  }

  @Override
  public void copyFromLocalFile(Path src, Path dst) throws IOException {
    Span span = Trace.start("copyFromLocalFile");
    if (Trace.isTracing()) {
      span.data("src", src.toString());
      span.data("dst", dst.toString());
    }
    try {
      impl.copyFromLocalFile(src, dst);
    } finally {
      span.stop();
    }
  }

  @Override
  public void moveFromLocalFile(Path[] srcs, Path dst) throws IOException {
    Span span = Trace.start("moveFromLocalFile");
    if (Trace.isTracing()) {
      span.data("dst", dst.toString());
    }
    try {
      impl.moveFromLocalFile(srcs, dst);
    } finally {
      span.stop();
    }
  }

  @Override
  public void moveFromLocalFile(Path src, Path dst) throws IOException {
    Span span = Trace.start("moveFromLocalFile");
    if (Trace.isTracing()) {
      span.data("src", src.toString());
      span.data("dst", dst.toString());
    }
    try {
      impl.moveFromLocalFile(src, dst);
    } finally {
      span.stop();
    }
  }

  @Override
  public void copyFromLocalFile(boolean delSrc, Path src, Path dst) throws IOException {
    Span span = Trace.start("copyFromLocalFile");
    if (Trace.isTracing()) {
      span.data("src", src.toString());
      span.data("dst", dst.toString());
    }
    try {
      impl.copyFromLocalFile(delSrc, src, dst);
    } finally {
      span.stop();
    }
  }

  @Override
  public void copyFromLocalFile(boolean delSrc, boolean overwrite, Path[] srcs, Path dst) throws IOException {
    Span span = Trace.start("copyFromLocalFile");
    if (Trace.isTracing()) {
      span.data("dst", dst.toString());
    }
    try {
      impl.copyFromLocalFile(delSrc, overwrite, srcs, dst);
    } finally {
      span.stop();
    }
  }

  @Override
  public void copyFromLocalFile(boolean delSrc, boolean overwrite, Path src, Path dst) throws IOException {
    Span span = Trace.start("copyFromLocalFile");
    if (Trace.isTracing()) {
      span.data("src", src.toString());
      span.data("dst", dst.toString());
    }
    try {
      impl.copyFromLocalFile(delSrc, overwrite, src, dst);
    } finally {
      span.stop();
    }
  }

  @Override
  public void copyToLocalFile(Path src, Path dst) throws IOException {
    Span span = Trace.start("copyFromLocalFile");
    if (Trace.isTracing()) {
      span.data("src", src.toString());
      span.data("dst", dst.toString());
    }
    try {
      impl.copyToLocalFile(src, dst);
    } finally {
      span.stop();
    }
  }

  @Override
  public void moveToLocalFile(Path src, Path dst) throws IOException {
    Span span = Trace.start("moveToLocalFile");
    if (Trace.isTracing()) {
      span.data("src", src.toString());
      span.data("dst", dst.toString());
    }
    try {
      impl.moveToLocalFile(src, dst);
    } finally {
      span.stop();
    }
  }

  @Override
  public void copyToLocalFile(boolean delSrc, Path src, Path dst) throws IOException {
    Span span = Trace.start("copyToLocalFile");
    if (Trace.isTracing()) {
      span.data("src", src.toString());
      span.data("dst", dst.toString());
    }
    try {
      impl.copyToLocalFile(delSrc, src, dst);
    } finally {
      span.stop();
    }
  }

  @Override
  public Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile) throws IOException {
    Span span = Trace.start("startLocalOutput");
    if (Trace.isTracing()) {
      span.data("out", fsOutputFile.toString());
      span.data("local", tmpLocalFile.toString());
    }
    try {
      return impl.startLocalOutput(fsOutputFile, tmpLocalFile);
    } finally {
      span.stop();
    }
  }

  @Override
  public void completeLocalOutput(Path fsOutputFile, Path tmpLocalFile) throws IOException {
    Span span = Trace.start("completeLocalOutput");
    if (Trace.isTracing()) {
      span.data("out", fsOutputFile.toString());
      span.data("local", tmpLocalFile.toString());
    }
    try {
      impl.completeLocalOutput(fsOutputFile, tmpLocalFile);
    } finally {
      span.stop();
    }
  }

  @Override
  public void close() throws IOException {
    Span span = Trace.start("close");
    try {
      impl.close();
    } finally {
      span.stop();
    }
  }

  @Override
  public long getUsed() throws IOException {
    Span span = Trace.start("getUsed");
    try {
      return impl.getUsed();
    } finally {
      span.stop();
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public long getBlockSize(Path f) throws IOException {
    Span span = Trace.start("getBlockSize");
    if (Trace.isTracing()) {
      span.data("path", f.toString());
    }
    try {
      return impl.getBlockSize(f);
    } finally {
      span.stop();
    }
  }

  @Deprecated
  @Override
  public long getDefaultBlockSize() {
    Span span = Trace.start("getDefaultBlockSize");
    try {
      return impl.getDefaultBlockSize();
    } finally {
      span.stop();
    }
  }

  @Deprecated
  @Override
  public short getDefaultReplication() {
    Span span = Trace.start("getDefaultReplication");
    try {
      return impl.getDefaultReplication();
    } finally {
      span.stop();
    }
  }

  @Override
  public FileChecksum getFileChecksum(Path f) throws IOException {
    Span span = Trace.start("getFileChecksum");
    if (Trace.isTracing()) {
      span.data("path", f.toString());
    }
    try {
      return impl.getFileChecksum(f);
    } finally {
      span.stop();
    }
  }

  @Override
  public void setVerifyChecksum(boolean verifyChecksum) {
    Span span = Trace.start("setVerifyChecksum");
    try {
      impl.setVerifyChecksum(verifyChecksum);
    } finally {
      span.stop();
    }
  }

  @Override
  public void setPermission(Path p, FsPermission permission) throws IOException {
    Span span = Trace.start("setPermission");
    if (Trace.isTracing()) {
      span.data("path", p.toString());
    }
    try {
      impl.setPermission(p, permission);
    } finally {
      span.stop();
    }
  }

  @Override
  public void setOwner(Path p, String username, String groupname) throws IOException {
    Span span = Trace.start("setOwner");
    if (Trace.isTracing()) {
      span.data("path", p.toString());
      span.data("user", username);
      span.data("group", groupname);
    }

    try {
      impl.setOwner(p, username, groupname);
    } finally {
      span.stop();
    }
  }

  @Override
  public void setTimes(Path p, long mtime, long atime) throws IOException {
    Span span = Trace.start("setTimes");
    try {
      impl.setTimes(p, mtime, atime);
    } finally {
      span.stop();
    }
  }

  final FileSystem impl;

  TraceFileSystem(FileSystem impl) {
    checkArgument(impl != null, "impl is null");
    this.impl = impl;
  }

  public FileSystem getImplementation() {
    return impl;
  }

  @Override
  public URI getUri() {
    Span span = Trace.start("getUri");
    try {
      return impl.getUri();
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataInputStream open(Path f, int bufferSize) throws IOException {
    Span span = Trace.start("open");
    try {
      return new TraceFSDataInputStream(impl.open(f, bufferSize));
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress)
      throws IOException {
    Span span = Trace.start("create");
    try {
      return impl.create(f, overwrite, bufferSize, replication, blockSize, progress);
    } finally {
      span.stop();
    }
  }

  @Override
  public void initialize(URI name, Configuration conf) throws IOException {
    Span span = Trace.start("initialize");
    try {
      impl.initialize(name, conf);
    } finally {
      span.stop();
    }
  }

  @Override
  public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) throws IOException {
    Span span = Trace.start("append");
    try {
      return impl.append(f, bufferSize, progress);
    } finally {
      span.stop();
    }
  }

  @Override
  public boolean rename(Path src, Path dst) throws IOException {
    Span span = Trace.start("rename");
    try {
      return impl.rename(src, dst);
    } finally {
      span.stop();
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean delete(Path f) throws IOException {
    Span span = Trace.start("delete");
    try {
      return impl.delete(f);
    } finally {
      span.stop();
    }
  }

  @Override
  public boolean delete(Path f, boolean recursive) throws IOException {
    Span span = Trace.start("delete");
    try {
      return impl.delete(f, recursive);
    } finally {
      span.stop();
    }
  }

  @Override
  public FileStatus[] listStatus(Path f) throws IOException {
    Span span = Trace.start("listStatus");
    try {
      return impl.listStatus(f);
    } finally {
      span.stop();
    }
  }

  @Override
  public void setWorkingDirectory(Path new_dir) {
    Span span = Trace.start("setWorkingDirectory");
    try {
      impl.setWorkingDirectory(new_dir);
    } finally {
      span.stop();
    }
  }

  @Override
  public Path getWorkingDirectory() {
    Span span = Trace.start("getWorkingDirectory");
    try {
      return impl.getWorkingDirectory();
    } finally {
      span.stop();
    }
  }

  @Override
  public boolean mkdirs(Path f, FsPermission permission) throws IOException {
    Span span = Trace.start("mkdirs");
    try {
      return impl.mkdirs(f, permission);
    } finally {
      span.stop();
    }
  }

  @Override
  public FileStatus getFileStatus(Path f) throws IOException {
    Span span = Trace.start("getFileStatus");
    try {
      return impl.getFileStatus(f);
    } finally {
      span.stop();
    }
  }

  public static FileSystem wrap(FileSystem fileSystem) {
    return new TraceFileSystem(fileSystem);
  }

  public static FileSystem getAndWrap(Configuration conf) throws IOException {
    return wrap(FileSystem.get(conf));
  }

}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/AccumuloStatus.java b/server/base/src/main/java/org/apache/accumulo/server/util/AccumuloStatus.java
index 6c7fd47e5..7bd5f6d2d 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/AccumuloStatus.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/AccumuloStatus.java
@@ -50,7 +50,8 @@ public class AccumuloStatus {
         if (!reader.getChildren(rootPath + Constants.ZTSERVERS + "/" + child).isEmpty())
           return false;
       }
      if (!reader.getChildren(rootPath + Constants.ZTRACERS).isEmpty())
      // TODO: check configured tracers location instead of default
      if (!reader.getChildren(Constants.ZTRACERS).isEmpty())
         return false;
       if (!reader.getChildren(rootPath + Constants.ZMASTER_LOCK).isEmpty())
         return false;
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/ZooZap.java b/server/base/src/main/java/org/apache/accumulo/server/util/ZooZap.java
index 1f5953177..7fdbf13e1 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/ZooZap.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/ZooZap.java
@@ -19,6 +19,7 @@ package org.apache.accumulo.server.util;
 import java.util.List;
 
 import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.server.cli.ClientOpts;
 import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
 import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
@@ -48,6 +49,10 @@ public class ZooZap {
     boolean zapTracers = false;
     @Parameter(names="-verbose", description="print out messages about progress")
     boolean verbose = false;

    String getTraceZKPath() {
      return super.getClientConfiguration().get(ClientProperty.TRACE_ZK_PATH);
    }
   }
   
   public static void main(String[] args) {
@@ -93,7 +98,7 @@ public class ZooZap {
     }
     
     if (opts.zapTracers) {
      String path = Constants.ZROOT + "/" + iid + Constants.ZTRACERS;
      String path = opts.getTraceZKPath();
       zapDirectory(zoo, path);
     }
     
diff --git a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
index 01fd2c8e7..720d18b05 100644
-- a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
++ b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
@@ -69,6 +69,7 @@ import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.security.SecurityUtil;
 import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.trace.DistributedTrace;
 import org.apache.accumulo.core.util.NamingThreadFactory;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.core.util.ServerServices;
@@ -97,7 +98,6 @@ import org.apache.accumulo.server.util.TServerUtils;
 import org.apache.accumulo.server.util.TabletIterator;
 import org.apache.accumulo.server.zookeeper.ZooLock;
 import org.apache.accumulo.trace.instrument.CountSampler;
import org.apache.accumulo.trace.instrument.Sampler;
 import org.apache.accumulo.trace.instrument.Span;
 import org.apache.accumulo.trace.instrument.Trace;
 import org.apache.accumulo.trace.thrift.TInfo;
@@ -158,8 +158,12 @@ public class SimpleGarbageCollector implements Iface {
     AccumuloConfiguration config = conf.getConfiguration();
 
     gc.init(fs, instance, SystemCredentials.get(), config);
    Accumulo.enableTracing(opts.getAddress(), app);
    gc.run();
    DistributedTrace.enable(opts.getAddress(), app, config);
    try {
      gc.run();
    } finally {
      DistributedTrace.disable();
    }
   }
 
   /**
@@ -568,11 +572,10 @@ public class SimpleGarbageCollector implements Iface {
       return;
     }
 
    Sampler sampler = new CountSampler(100);
    CountSampler sampler = new CountSampler(100);
 
     while (true) {
      if (sampler.next())
        Trace.on("gc");
      Trace.on("gc", sampler);
 
       Span gcSpan = Trace.start("loop");
       tStart = System.currentTimeMillis();
@@ -634,7 +637,7 @@ public class SimpleGarbageCollector implements Iface {
         log.warn(e, e);
       }
 
      Trace.offNoFlush();
      Trace.off();
       try {
         long gcDelay = config.getTimeInMillis(Property.GC_CYCLE_DELAY);
         log.debug("Sleeping for " + gcDelay + " milliseconds");
diff --git a/server/master/src/main/java/org/apache/accumulo/master/Master.java b/server/master/src/main/java/org/apache/accumulo/master/Master.java
index b6b96a096..bbd23962b 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/Master.java
++ b/server/master/src/main/java/org/apache/accumulo/master/Master.java
@@ -69,6 +69,7 @@ import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.security.NamespacePermission;
 import org.apache.accumulo.core.security.SecurityUtil;
 import org.apache.accumulo.core.security.TablePermission;
import org.apache.accumulo.core.trace.DistributedTrace;
 import org.apache.accumulo.core.util.Daemon;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.core.util.UtilWaitThread;
@@ -1259,11 +1260,13 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
       VolumeManager fs = VolumeManagerImpl.get();
       Accumulo.init(fs, conf, app);
       Master master = new Master(conf, fs, hostname);
      Accumulo.enableTracing(hostname, app);
      DistributedTrace.enable(hostname, app, conf.getConfiguration());
       master.run();
     } catch (Exception ex) {
       log.error("Unexpected exception, exiting", ex);
       System.exit(1);
    } finally {
      DistributedTrace.disable();
     }
   }
 
diff --git a/server/master/src/main/java/org/apache/accumulo/master/replication/ReplicationDriver.java b/server/master/src/main/java/org/apache/accumulo/master/replication/ReplicationDriver.java
index a52f7439f..e3bbafa04 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/replication/ReplicationDriver.java
++ b/server/master/src/main/java/org/apache/accumulo/master/replication/ReplicationDriver.java
@@ -25,7 +25,6 @@ import org.apache.accumulo.core.util.Daemon;
 import org.apache.accumulo.fate.util.UtilWaitThread;
 import org.apache.accumulo.master.Master;
 import org.apache.accumulo.trace.instrument.CountSampler;
import org.apache.accumulo.trace.instrument.Sampler;
 import org.apache.accumulo.trace.instrument.Trace;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -54,7 +53,7 @@ public class ReplicationDriver extends Daemon {
 
   @Override
   public void run() {
    Sampler sampler = new CountSampler(10);
    CountSampler sampler = new CountSampler(10);
 
     while (master.stillMaster()) {
       if (null == workMaker) {
@@ -73,9 +72,7 @@ public class ReplicationDriver extends Daemon {
         rcrr = new RemoveCompleteReplicationRecords(conn);
       }
 
      if (sampler.next()) {
        Trace.on("masterReplicationDriver");
      }
      Trace.on("masterReplicationDriver", sampler);
 
       // Make status markers from replication records in metadata, removing entries in
       // metadata which are no longer needed (closed records)
@@ -109,7 +106,7 @@ public class ReplicationDriver extends Daemon {
         log.error("Caught Exception trying to remove finished Replication records", e);
       }
 
      Trace.offNoFlush();
      Trace.off();
 
       // Sleep for a bit
       long sleepMillis = conf.getTimeInMillis(Property.MASTER_REPLICATION_SCAN_INTERVAL);
diff --git a/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java b/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
index 49bb56da7..7fe1af7fc 100644
-- a/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
++ b/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
@@ -54,6 +54,7 @@ import org.apache.accumulo.core.util.ServerServices;
 import org.apache.accumulo.core.util.ServerServices.Service;
 import org.apache.accumulo.core.util.ThriftUtil;
 import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.accumulo.core.trace.DistributedTrace;
 import org.apache.accumulo.core.zookeeper.ZooUtil;
 import org.apache.accumulo.fate.zookeeper.ZooLock.LockLossReason;
 import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
@@ -413,8 +414,12 @@ public class Monitor {
     config = new ServerConfigurationFactory(instance);
     Accumulo.init(fs, config, app);
     Monitor monitor = new Monitor();
    Accumulo.enableTracing(hostname, app);
    monitor.run(hostname);
    DistributedTrace.enable(hostname, app, config.getConfiguration());
    try {
      monitor.run(hostname);
    } finally {
      DistributedTrace.disable();
    }
   }
 
   private static long START_TIME;
diff --git a/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/ShowTrace.java b/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/ShowTrace.java
index a476201f4..896808874 100644
-- a/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/ShowTrace.java
++ b/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/ShowTrace.java
@@ -17,7 +17,9 @@
 package org.apache.accumulo.monitor.servlets.trace;
 
 import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
 
import java.nio.ByteBuffer;
 import java.util.Collection;
 import java.util.Map.Entry;
 import java.util.Set;
@@ -34,6 +36,7 @@ import org.apache.accumulo.core.trace.SpanTreeVisitor;
 import org.apache.accumulo.core.trace.TraceDump;
 import org.apache.accumulo.core.trace.TraceFormatter;
 import org.apache.accumulo.monitor.servlets.BasicServlet;
import org.apache.accumulo.trace.thrift.Annotation;
 import org.apache.accumulo.trace.thrift.RemoteSpan;
 import org.apache.hadoop.io.Text;
 
@@ -115,7 +118,8 @@ public class ShowTrace extends Basic {
         sb.append(String.format("<td style='text-indent: %dpx'>%s@%s</td>%n", level * 5, node.svc, node.sender));
         sb.append("<td>" + node.description + "</td>");
         boolean hasData = node.data != null && !node.data.isEmpty();
        if (hasData) {
        boolean hasAnnotations = node.annotations != null && !node.annotations.isEmpty();
        if (hasData || hasAnnotations) {
           String hexSpanId = Long.toHexString(node.spanId);
           sb.append("<td><input type='checkbox' id=\"");
           sb.append(hexSpanId);
@@ -127,11 +131,23 @@ public class ShowTrace extends Basic {
         sb.append("</tr>\n");
         sb.append("<tr id='" + Long.toHexString(node.spanId) + "' style='display:none'>");
         sb.append("<td colspan='5'>\n");
        if (hasData) {
        if (hasData || hasAnnotations) {
           sb.append("  <table class='indent,noborder'>\n");
          for (Entry<String,String> entry : node.data.entrySet()) {
            sb.append("  <tr><td>" + BasicServlet.sanitize(entry.getKey()) + "</td>");
            sb.append("<td>" + BasicServlet.sanitize(entry.getValue()) + "</td></tr>\n");
          if (hasData) {
            sb.append("  <tr><th>Key</th><th>Value</th></tr>\n");
            for (Entry<ByteBuffer, ByteBuffer> entry : node.data.entrySet()) {
              String key = new String(entry.getKey().array(), entry.getKey().arrayOffset(), entry.getKey().limit(), UTF_8);
              String value = new String(entry.getValue().array(), entry.getValue().arrayOffset(), entry.getValue().limit(), UTF_8);
              sb.append("  <tr><td>" + BasicServlet.sanitize(key) + "</td>");
              sb.append("<td>" + BasicServlet.sanitize(value) + "</td></tr>\n");
            }
          }
          if (hasAnnotations) {
            sb.append("  <tr><th>Annotation</th><th>Time Offset</th></tr>\n");
            for (Annotation entry : node.annotations) {
              sb.append("  <tr><td>" + BasicServlet.sanitize(entry.getMsg()) + "</td>");
              sb.append(String.format("<td>%d</td></tr>\n", entry.getTime() - finalStart));
            }
           }
           sb.append("  </table>");
         }
diff --git a/server/monitor/src/test/java/org/apache/accumulo/monitor/ShowTraceLinkTypeTest.java b/server/monitor/src/test/java/org/apache/accumulo/monitor/ShowTraceLinkTypeTest.java
index a63043423..effb0e637 100644
-- a/server/monitor/src/test/java/org/apache/accumulo/monitor/ShowTraceLinkTypeTest.java
++ b/server/monitor/src/test/java/org/apache/accumulo/monitor/ShowTraceLinkTypeTest.java
@@ -20,45 +20,47 @@ import java.util.ArrayList;
 import java.util.Collections;
 
 import org.apache.accumulo.trace.thrift.RemoteSpan;
import org.apache.accumulo.trace.thrift.Annotation;
 import org.junit.Assert;
 import org.junit.Test;
 
import java.nio.ByteBuffer;

 public class ShowTraceLinkTypeTest {
  
  private static RemoteSpan rs(long start, long stop, String description) {
    return new RemoteSpan("sender", "svc", 0l, 0l, 0l, start, stop, description, Collections.<ByteBuffer, ByteBuffer>emptyMap(), Collections.<Annotation>emptyList());
  }

   @Test
   public void testTraceSortingForMonitor() {
    /*
     * public RemoteSpan(String sender, String svc, long traceId, long spanId, long parentId, long start, long stop, String description, Map<String,String>
     * data)
     */
     ArrayList<RemoteSpan> spans = new ArrayList<RemoteSpan>(10), expectedOrdering = new ArrayList<RemoteSpan>(10);
    

     // "Random" ordering
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 55l, 75l, "desc5", Collections.<String,String> emptyMap()));
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 25l, 30l, "desc2", Collections.<String,String> emptyMap()));
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 85l, 90l, "desc8", Collections.<String,String> emptyMap()));
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 45l, 60l, "desc4", Collections.<String,String> emptyMap()));
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 35l, 55l, "desc3", Collections.<String,String> emptyMap()));
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 95l, 110l, "desc9", Collections.<String,String> emptyMap()));
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 65l, 80l, "desc6", Collections.<String,String> emptyMap()));
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 100l, 120l, "desc10", Collections.<String,String> emptyMap()));
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 15l, 25l, "desc1", Collections.<String,String> emptyMap()));
    spans.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 75l, 100l, "desc7", Collections.<String,String> emptyMap()));
    
    spans.add(rs(55l, 75l, "desc5"));
    spans.add(rs(25l, 30l, "desc2"));
    spans.add(rs(85l, 90l, "desc8"));
    spans.add(rs(45l, 60l, "desc4"));
    spans.add(rs(35l, 55l, "desc3"));
    spans.add(rs(95l, 110l, "desc9"));
    spans.add(rs(65l, 80l, "desc6"));
    spans.add(rs(100l, 120l, "desc10"));
    spans.add(rs(15l, 25l, "desc1"));
    spans.add(rs(75l, 100l, "desc7"));

     // We expect them to be sorted by 'start'
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 15l, 25l, "desc1", Collections.<String,String> emptyMap()));
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 25l, 30l, "desc2", Collections.<String,String> emptyMap()));
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 35l, 55l, "desc3", Collections.<String,String> emptyMap()));
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 45l, 60l, "desc4", Collections.<String,String> emptyMap()));
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 55l, 75l, "desc5", Collections.<String,String> emptyMap()));
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 65l, 80l, "desc6", Collections.<String,String> emptyMap()));
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 75l, 100l, "desc7", Collections.<String,String> emptyMap()));
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 85l, 90l, "desc8", Collections.<String,String> emptyMap()));
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 95l, 110l, "desc9", Collections.<String,String> emptyMap()));
    expectedOrdering.add(new RemoteSpan("sender", "svc", 0l, 0l, 0l, 100l, 120l, "desc10", Collections.<String,String> emptyMap()));
    
    expectedOrdering.add(rs(15l, 25l, "desc1"));
    expectedOrdering.add(rs(25l, 30l, "desc2"));
    expectedOrdering.add(rs(35l, 55l, "desc3"));
    expectedOrdering.add(rs(45l, 60l, "desc4"));
    expectedOrdering.add(rs(55l, 75l, "desc5"));
    expectedOrdering.add(rs(65l, 80l, "desc6"));
    expectedOrdering.add(rs(75l, 100l, "desc7"));
    expectedOrdering.add(rs(85l, 90l, "desc8"));
    expectedOrdering.add(rs(95l, 110l, "desc9"));
    expectedOrdering.add(rs(100l, 120l, "desc10"));

     Collections.sort(spans);
    

     Assert.assertEquals(expectedOrdering, spans);
   }
 }
diff --git a/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java b/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
index 4858b8ad8..af1ec568a 100644
-- a/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
++ b/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
@@ -26,7 +26,6 @@ import java.util.Map.Entry;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicReference;
 
import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
@@ -43,10 +42,10 @@ import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.iterators.user.AgeOffFilter;
 import org.apache.accumulo.core.security.SecurityUtil;
import org.apache.accumulo.core.trace.TraceFormatter;
 import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.core.trace.TraceFormatter;
 import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
 import org.apache.accumulo.server.Accumulo;
 import org.apache.accumulo.server.ServerOpts;
 import org.apache.accumulo.server.client.HdfsZooInstance;
@@ -225,7 +224,7 @@ public class TraceServer implements Watcher {
     TThreadPoolServer.Args options = new TThreadPoolServer.Args(transport);
     options.processor(new Processor<Iface>(new Receiver()));
     server = new TThreadPoolServer(options);
    registerInZooKeeper(sock.getInetAddress().getHostAddress() + ":" + sock.getLocalPort());
    registerInZooKeeper(sock.getInetAddress().getHostAddress() + ":" + sock.getLocalPort(), conf.get(Property.TRACE_ZK_PATH));
     writer = new AtomicReference<BatchWriter>(this.connector.createBatchWriter(table, new BatchWriterConfig().setMaxLatency(5, TimeUnit.SECONDS)));
   }
 
@@ -278,9 +277,10 @@ public class TraceServer implements Watcher {
     }
   }
 
  private void registerInZooKeeper(String name) throws Exception {
    String root = ZooUtil.getRoot(serverConfiguration.getInstance()) + Constants.ZTRACERS;
  private void registerInZooKeeper(String name, String root) throws Exception {
     IZooReaderWriter zoo = ZooReaderWriter.getInstance();
    zoo.putPersistentData(root, new byte[0], NodeExistsPolicy.SKIP);
    log.info("Registering tracer " + name + " at " + root);
     String path = zoo.putEphemeralSequential(root + "/trace-", name.getBytes(UTF_8));
     zoo.exists(path, this);
   }
@@ -297,9 +297,12 @@ public class TraceServer implements Watcher {
     Accumulo.init(fs, conf, app);
     String hostname = opts.getAddress();
     TraceServer server = new TraceServer(conf, hostname);
    Accumulo.enableTracing(hostname, app);
    server.run();
    log.info("tracer stopping");
    try {
      server.run();
    } finally {
      log.info("tracer stopping");
      ZooReaderWriter.getInstance().getZooKeeper().close();
    }
   }
 
   @Override
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/BulkFailedCopyProcessor.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/BulkFailedCopyProcessor.java
index f7bda49f7..034becdde 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/BulkFailedCopyProcessor.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/BulkFailedCopyProcessor.java
@@ -23,7 +23,6 @@ import org.apache.accumulo.core.conf.SiteConfiguration;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.fs.VolumeManagerImpl;
import org.apache.accumulo.server.trace.TraceFileSystem;
 import org.apache.accumulo.server.zookeeper.DistributedWorkQueue.Processor;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.FileUtil;
@@ -53,8 +52,8 @@ public class BulkFailedCopyProcessor implements Processor {
 
     try {
       VolumeManager vm = VolumeManagerImpl.get(SiteConfiguration.getInstance());
      FileSystem origFs = TraceFileSystem.wrap(vm.getVolumeByPath(orig).getFileSystem());
      FileSystem destFs = TraceFileSystem.wrap(vm.getVolumeByPath(dest).getFileSystem());
      FileSystem origFs = vm.getVolumeByPath(orig).getFileSystem();
      FileSystem destFs = vm.getVolumeByPath(dest).getFileSystem();
       
       FileUtil.copy(origFs, orig, destFs, tmp, false, true, CachedConfiguration.getInstance());
       destFs.rename(tmp, dest);
@@ -62,7 +61,7 @@ public class BulkFailedCopyProcessor implements Processor {
     } catch (IOException ex) {
       try {
         VolumeManager vm = VolumeManagerImpl.get(SiteConfiguration.getInstance());
        FileSystem destFs = TraceFileSystem.wrap(vm.getVolumeByPath(dest).getFileSystem());
        FileSystem destFs = vm.getVolumeByPath(dest).getFileSystem();
         destFs.create(dest).close();
         log.warn(" marked " + dest + " failed", ex);
       } catch (IOException e) {
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java
index 9a1117dde..7378348ff 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java
@@ -64,7 +64,6 @@ import org.apache.accumulo.core.util.LocalityGroupUtil;
 import org.apache.accumulo.core.util.LocalityGroupUtil.LocalityGroupConfigurationError;
 import org.apache.accumulo.core.util.LocalityGroupUtil.Partitioner;
 import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.accumulo.server.trace.TraceFileSystem;
 import org.apache.commons.lang.mutable.MutableLong;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
@@ -557,7 +556,7 @@ public class InMemoryMap {
     private synchronized FileSKVIterator getReader() throws IOException {
       if (reader == null) {
         Configuration conf = CachedConfiguration.getInstance();
        FileSystem fs = TraceFileSystem.wrap(FileSystem.getLocal(conf));
        FileSystem fs = FileSystem.getLocal(conf);
         
         reader = new RFileOperations().openReader(memDumpFile, true, fs, conf, SiteConfiguration.getInstance());
         if (iflag != null)
@@ -712,7 +711,7 @@ public class InMemoryMap {
       // dump memmap exactly as is to a tmp file on disk, and switch scans to that temp file
       try {
         Configuration conf = CachedConfiguration.getInstance();
        FileSystem fs = TraceFileSystem.wrap(FileSystem.getLocal(conf));
        FileSystem fs = FileSystem.getLocal(conf);
         
         String tmpFile = memDumpDir + "/memDump" + UUID.randomUUID() + "." + RFile.EXTENSION;
         
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index 54c75f8b6..9fd255a8e 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -122,6 +122,7 @@ import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService.Iface;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService.Processor;
 import org.apache.accumulo.core.tabletserver.thrift.TabletStats;
import org.apache.accumulo.core.trace.DistributedTrace;
 import org.apache.accumulo.core.util.ByteBufferUtil;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.core.util.ColumnFQ;
@@ -2876,11 +2877,13 @@ public class TabletServer implements Runnable {
       Accumulo.init(fs, conf, app);
       TabletServer server = new TabletServer(conf, fs);
       server.config(hostname);
      Accumulo.enableTracing(hostname, app);
      DistributedTrace.enable(hostname, app, conf.getConfiguration());
       server.run();
     } catch (Exception ex) {
       log.error("Uncaught exception in TabletServer.main, exiting", ex);
       System.exit(1);
    } finally {
      DistributedTrace.disable();
     }
   }
 
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/replication/AccumuloReplicaSystem.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/replication/AccumuloReplicaSystem.java
index 732907d53..4a899efc2 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/replication/AccumuloReplicaSystem.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/replication/AccumuloReplicaSystem.java
@@ -228,7 +228,7 @@ public class AccumuloReplicaSystem implements ReplicaSystem {
       // We made no status, punt on it for now, and let it re-queue itself for work
       return status;
     } finally {
      Trace.offNoFlush();
      Trace.off();
     }
   }
 
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
index ef3a0c903..9dd432348 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
@@ -2078,11 +2078,10 @@ public class Tablet implements TabletCommitter {
           this.notifyAll();
         }
 
        Span curr = Trace.currentTrace();
        curr.data("extent", "" + getExtent());
        span.data("extent", "" + getExtent());
         if (majCStats != null) {
          curr.data("read", "" + majCStats.getEntriesRead());
          curr.data("written", "" + majCStats.getEntriesWritten());
          span.data("read", "" + majCStats.getEntriesRead());
          span.data("written", "" + majCStats.getEntriesWritten());
         }
       }
     } finally {
diff --git a/shell/src/main/java/org/apache/accumulo/shell/Shell.java b/shell/src/main/java/org/apache/accumulo/shell/Shell.java
index a0ff17abd..996b2afdd 100644
-- a/shell/src/main/java/org/apache/accumulo/shell/Shell.java
++ b/shell/src/main/java/org/apache/accumulo/shell/Shell.java
@@ -78,7 +78,6 @@ import org.apache.accumulo.core.util.format.Formatter;
 import org.apache.accumulo.core.util.format.FormatterFactory;
 import org.apache.accumulo.core.volume.VolumeConfiguration;
 import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooReader;
 import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
 import org.apache.accumulo.start.classloader.vfs.ContextManager;
 import org.apache.accumulo.shell.commands.AboutCommand;
@@ -328,8 +327,7 @@ public class Shell extends ShellOptions {
       }
 
       if (!options.isFake()) {
        ZooReader zr = new ZooReader(instance.getZooKeepers(), instance.getZooKeepersSessionTimeOut());
        DistributedTrace.enable(instance, zr, "shell", InetAddress.getLocalHost().getHostName());
        DistributedTrace.enable(InetAddress.getLocalHost().getHostName(), "shell", options.getClientConfiguration());
       }
 
       this.setTableName("");
@@ -525,6 +523,7 @@ public class Shell extends ShellOptions {
       System.exit(shell.start());
     } finally {
       shell.shutdown();
      DistributedTrace.disable();
     }
   }
 
diff --git a/shell/src/main/java/org/apache/accumulo/shell/commands/TraceCommand.java b/shell/src/main/java/org/apache/accumulo/shell/commands/TraceCommand.java
index 7f6357074..819f61c71 100644
-- a/shell/src/main/java/org/apache/accumulo/shell/commands/TraceCommand.java
++ b/shell/src/main/java/org/apache/accumulo/shell/commands/TraceCommand.java
@@ -40,7 +40,7 @@ public class TraceCommand extends DebugCommand {
         Trace.on("shell:" + shellState.getPrincipal());
       } else if (cl.getArgs()[0].equalsIgnoreCase("off")) {
         if (Trace.isTracing()) {
          final long trace = Trace.currentTrace().traceId();
          final long trace = Trace.currentTraceId();
           Trace.off();
           StringBuffer sb = new StringBuffer();
           int traceCount = 0;
diff --git a/test/src/main/java/org/apache/accumulo/test/TestIngest.java b/test/src/main/java/org/apache/accumulo/test/TestIngest.java
index 7f6c514be..47033f3d7 100644
-- a/test/src/main/java/org/apache/accumulo/test/TestIngest.java
++ b/test/src/main/java/org/apache/accumulo/test/TestIngest.java
@@ -29,7 +29,6 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.TableExistsException;
 import org.apache.accumulo.core.client.TableNotFoundException;
@@ -49,9 +48,7 @@ import org.apache.accumulo.core.security.ColumnVisibility;
 import org.apache.accumulo.core.trace.DistributedTrace;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.core.util.FastFormat;
import org.apache.accumulo.fate.zookeeper.ZooReader;
 import org.apache.accumulo.server.cli.ClientOnDefaultTable;
import org.apache.accumulo.trace.instrument.Trace;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.io.Text;
@@ -181,11 +178,9 @@ public class TestIngest {
     Opts opts = new Opts();
     BatchWriterOpts bwOpts = new BatchWriterOpts();
     opts.parseArgs(TestIngest.class.getName(), args, bwOpts);
    
    Instance instance = opts.getInstance();
    

     String name = TestIngest.class.getSimpleName();
    DistributedTrace.enable(instance, new ZooReader(instance.getZooKeepers(), instance.getZooKeepersSessionTimeOut()), name, null);
    DistributedTrace.enable(name);
     
     try {
       opts.startTracing(name);
@@ -199,7 +194,8 @@ public class TestIngest {
     } catch (Exception e) {
       throw new RuntimeException(e);
     } finally {
      Trace.off();
      opts.stopTracing();
      DistributedTrace.disable();
     }
   }
 
diff --git a/test/src/main/java/org/apache/accumulo/test/VerifyIngest.java b/test/src/main/java/org/apache/accumulo/test/VerifyIngest.java
index 74b03e437..902d49ecd 100644
-- a/test/src/main/java/org/apache/accumulo/test/VerifyIngest.java
++ b/test/src/main/java/org/apache/accumulo/test/VerifyIngest.java
@@ -25,7 +25,6 @@ import org.apache.accumulo.core.cli.ScannerOpts;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.data.Key;
@@ -34,8 +33,8 @@ import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.trace.DistributedTrace;
import org.apache.accumulo.fate.zookeeper.ZooReader;
 import org.apache.accumulo.trace.instrument.Trace;

 import org.apache.hadoop.io.Text;
 import org.apache.log4j.Logger;
 
@@ -62,19 +61,19 @@ public class VerifyIngest {
     Opts opts = new Opts();
     ScannerOpts scanOpts = new ScannerOpts();
     opts.parseArgs(VerifyIngest.class.getName(), args, scanOpts);
    Instance instance = opts.getInstance();
     try {
       if (opts.trace) {
         String name = VerifyIngest.class.getSimpleName();
        DistributedTrace.enable(instance, new ZooReader(instance.getZooKeepers(), instance.getZooKeepersSessionTimeOut()), name, null);
        DistributedTrace.enable();
         Trace.on(name);
        Trace.currentTrace().data("cmdLine", Arrays.asList(args).toString());
        Trace.data("cmdLine", Arrays.asList(args).toString());
       }
 
       verifyIngest(opts.getConnector(), opts, scanOpts);
 
     } finally {
       Trace.off();
      DistributedTrace.disable();
     }
   }
 
diff --git a/test/src/test/java/org/apache/accumulo/test/ConditionalWriterIT.java b/test/src/test/java/org/apache/accumulo/test/ConditionalWriterIT.java
index c85373d52..81e519e0c 100644
-- a/test/src/test/java/org/apache/accumulo/test/ConditionalWriterIT.java
++ b/test/src/test/java/org/apache/accumulo/test/ConditionalWriterIT.java
@@ -74,7 +74,6 @@ import org.apache.accumulo.core.trace.TraceDump.Printer;
 import org.apache.accumulo.core.util.FastFormat;
 import org.apache.accumulo.core.util.UtilWaitThread;
 import org.apache.accumulo.examples.simple.constraints.AlphaNumKeyConstraint;
import org.apache.accumulo.fate.zookeeper.ZooReader;
 import org.apache.accumulo.test.functional.BadIterator;
 import org.apache.accumulo.test.functional.SimpleMacIT;
 import org.apache.accumulo.test.functional.SlowIterator;
@@ -1234,7 +1233,7 @@ public class ConditionalWriterIT extends SimpleMacIT {
     conn.tableOperations().create(tableName);
     conn.tableOperations().deleteRows("trace", null, null);
 
    DistributedTrace.enable(conn.getInstance(), new ZooReader(conn.getInstance().getZooKeepers(), 30*1000), "testTrace", "localhost");
    DistributedTrace.enable("localhost", "testTrace", getClientConfig());
     Span root = Trace.on("traceTest");
     ConditionalWriter cw = conn.createConditionalWriter(tableName, new ConditionalWriterConfig());
 
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/BulkFileIT.java b/test/src/test/java/org/apache/accumulo/test/functional/BulkFileIT.java
index 80ee99029..13bdc7ec8 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/BulkFileIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/BulkFileIT.java
@@ -34,7 +34,6 @@ import org.apache.accumulo.core.file.FileSKVWriter;
 import org.apache.accumulo.core.file.rfile.RFile;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.volume.VolumeConfiguration;
import org.apache.accumulo.server.trace.TraceFileSystem;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
@@ -59,7 +58,7 @@ public class BulkFileIT extends SimpleMacIT {
     c.tableOperations().addSplits(tableName, splits);
     Configuration conf = new Configuration();
     AccumuloConfiguration aconf = DefaultConfiguration.getInstance();
    FileSystem fs = TraceFileSystem.wrap(VolumeConfiguration.getDefaultVolume(conf, aconf).getFileSystem());
    FileSystem fs = VolumeConfiguration.getDefaultVolume(conf, aconf).getFileSystem();
 
     String dir = rootPath() + "/bulk_test_diff_files_89723987592_" + getUniqueNames(1)[0];
 
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/ExamplesIT.java b/test/src/test/java/org/apache/accumulo/test/functional/ExamplesIT.java
index 210e05757..5b03b179c 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/ExamplesIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/ExamplesIT.java
@@ -146,7 +146,7 @@ public class ExamplesIT extends ConfigurableMacIT {
     }
     assertTrue(count > 0);
     result = FunctionalTestUtils.readAll(cluster, TraceDumpExample.class, p);
    assertTrue(result.contains("myHost@myApp"));
    assertTrue(result.contains("myApp@myHost"));
     trace.destroy();
   }
 
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/SimpleMacIT.java b/test/src/test/java/org/apache/accumulo/test/functional/SimpleMacIT.java
index 03677f4a0..3f04b9435 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/SimpleMacIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/SimpleMacIT.java
@@ -17,9 +17,11 @@
 package org.apache.accumulo.test.functional;
 
 import java.io.File;
import java.io.FileNotFoundException;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.Property;
@@ -28,6 +30,8 @@ import org.apache.accumulo.minicluster.MiniAccumuloInstance;
 import org.apache.accumulo.minicluster.impl.MiniAccumuloClusterImpl;
 import org.apache.accumulo.minicluster.impl.MiniAccumuloConfigImpl;
 import org.apache.accumulo.minicluster.impl.ZooKeeperBindException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
 import org.apache.log4j.Logger;
 import org.junit.After;
 import org.junit.AfterClass;
@@ -41,6 +45,8 @@ import org.junit.BeforeClass;
 public class SimpleMacIT extends AbstractMacIT {
   protected static final Logger log = Logger.getLogger(SimpleMacIT.class);
 
  private static final String INSTANCE_NAME = "instance1";

   private static File folder;
   private static MiniAccumuloClusterImpl cluster = null;
 
@@ -124,7 +130,7 @@ public class SimpleMacIT extends AbstractMacIT {
    */
   private static Connector getInstanceOneConnector() {
     try {
      return new MiniAccumuloInstance("instance1", getInstanceOnePath()).getConnector("root", new PasswordToken(ROOT_PASSWORD));
      return new MiniAccumuloInstance(INSTANCE_NAME, getInstanceOnePath()).getConnector("root", new PasswordToken(ROOT_PASSWORD));
     } catch (Exception e) {
       return null;
     }
@@ -134,4 +140,14 @@ public class SimpleMacIT extends AbstractMacIT {
     return new File(System.getProperty("user.dir") + "/accumulo-maven-plugin/instance1");
   }
 
  protected static ClientConfiguration getClientConfig() throws FileNotFoundException, ConfigurationException {
    if (getInstanceOneConnector() == null) {
      return new ClientConfiguration(new PropertiesConfiguration(cluster.getConfig().getClientConfFile()));
    } else {
      File directory = getInstanceOnePath();
      return new ClientConfiguration(MiniAccumuloInstance.getConfigProperties(directory)).withInstance(INSTANCE_NAME)
          .withZkHosts(MiniAccumuloInstance.getZooKeepersFromDir(directory));
    }
  }

 }
diff --git a/trace/pom.xml b/trace/pom.xml
index aacfb56c8..d64e3ccd4 100644
-- a/trace/pom.xml
++ b/trace/pom.xml
@@ -38,6 +38,10 @@
       <groupId>org.apache.zookeeper</groupId>
       <artifactId>zookeeper</artifactId>
     </dependency>
    <dependency>
      <groupId>org.htrace</groupId>
      <artifactId>htrace-core</artifactId>
    </dependency>
     <dependency>
       <groupId>org.slf4j</groupId>
       <artifactId>slf4j-api</artifactId>
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/CloudtraceSpan.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/CloudtraceSpan.java
new file mode 100644
index 000000000..fb9744eea
-- /dev/null
++ b/trace/src/main/java/org/apache/accumulo/trace/instrument/CloudtraceSpan.java
@@ -0,0 +1,71 @@
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
package org.apache.accumulo.trace.instrument;

import java.util.Map;

/**
 * Base interface for gathering and reporting statistics about a block of execution.
 */
public interface CloudtraceSpan {
  static final long ROOT_SPAN_ID = 0;
  
  /** Begin gathering timing information */
  void start();
  
  /** The block has completed, stop the clock */
  void stop();
  
  /** Get the start time, in milliseconds */
  long getStartTimeMillis();
  
  /** Get the stop time, in milliseconds */
  long getStopTimeMillis();
  
  /** Return the total amount of time elapsed since start was called, if running, or difference between stop and start */
  long accumulatedMillis();
  
  /** Has the span been started and not yet stopped? */
  boolean running();
  
  /** Return a textual description of this span */
  String description();
  
  /** A pseudo-unique (random) number assigned to this span instance */
  long spanId();
  
  /** The parent span: returns null if this is the root span */
  Span parent();
  
  /** A pseudo-unique (random) number assigned to the trace associated with this span */
  long traceId();
  
  /** Create a child span of this span with the given description */
  Span child(String description);
  
  @Override
  String toString();
  
  /** Return the pseudo-unique (random) number of the parent span, returns ROOT_SPAN_ID if this is the root span */
  long parentId();
  
  /** Add data associated with this span */
  void data(String key, String value);
  
  /** Get data associated with this span (read only) */
  Map<String,String> getData();
}
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/CountSampler.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/CountSampler.java
index 9a5bdbb53..b291ee9da 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/CountSampler.java
++ b/trace/src/main/java/org/apache/accumulo/trace/instrument/CountSampler.java
@@ -16,26 +16,16 @@
  */
 package org.apache.accumulo.trace.instrument;
 
import java.util.Random;

 /**
 * Sampler that returns true every N calls.
 * 
 * use org.htrace.impl.CountSampler instead
  */
public class CountSampler implements Sampler {
  
  final static Random random = new Random();
  
  final long frequency;
  long count = random.nextLong();
  
public class CountSampler extends org.htrace.impl.CountSampler implements Sampler {
   public CountSampler(long frequency) {
    this.frequency = frequency;
    super(frequency);
   }
   
   @Override
   public boolean next() {
    return (count++ % frequency) == 0;
    return super.next(null);
   }
  
 }
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/Sampler.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/Sampler.java
index 4abb40a19..3813530be 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/Sampler.java
++ b/trace/src/main/java/org/apache/accumulo/trace/instrument/Sampler.java
@@ -17,11 +17,9 @@
 package org.apache.accumulo.trace.instrument;
 
 /**
 * Extremely simple callback to determine the frequency that an action should be performed.
 * 
 * @see Trace#wrapAll
 * use org.htrace.Sampler instead
  */
public interface Sampler {
public interface Sampler extends org.htrace.Sampler<Object> {
   
   boolean next();
   
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/Span.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/Span.java
index 52671748e..84275cebc 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/Span.java
++ b/trace/src/main/java/org/apache/accumulo/trace/instrument/Span.java
@@ -16,56 +16,173 @@
  */
 package org.apache.accumulo.trace.instrument;
 
import static java.nio.charset.StandardCharsets.UTF_8;

import org.htrace.NullScope;
import org.htrace.TimelineAnnotation;
import org.htrace.TraceScope;

import java.util.HashMap;
import java.util.List;
 import java.util.Map;
import java.util.Map.Entry;
 
 /**
 * Base interface for gathering and reporting statistics about a block of execution.
 * This is a wrapper for a TraceScope object, which is a wrapper for a Span and its parent.
  */
public interface Span {
  static final long ROOT_SPAN_ID = 0;
  
  /** Begin gathering timing information */
  void start();
  
  /** The block has completed, stop the clock */
  void stop();
  
  /** Get the start time, in milliseconds */
  long getStartTimeMillis();
  
  /** Get the stop time, in milliseconds */
  long getStopTimeMillis();
  
  /** Return the total amount of time elapsed since start was called, if running, or difference between stop and start */
  long accumulatedMillis();
  
  /** Has the span been started and not yet stopped? */
  boolean running();
  
  /** Return a textual description of this span */
  String description();
  
  /** A pseudo-unique (random) number assigned to this span instance */
  long spanId();
  
  /** The parent span: returns null if this is the root span */
  Span parent();
  
  /** A pseudo-unique (random) number assigned to the trace associated with this span */
  long traceId();
  
  /** Create a child span of this span with the given description */
  Span child(String description);
  
  @Override
  String toString();
  
  /** Return the pseudo-unique (random) number of the parent span, returns ROOT_SPAN_ID if this is the root span */
  long parentId();
  
  /** Add data associated with this span */
  void data(String key, String value);
  
  /** Get data associated with this span (read only) */
  Map<String,String> getData();
public class Span implements org.htrace.Span, CloudtraceSpan {
  public static final long ROOT_SPAN_ID = org.htrace.Span.ROOT_SPAN_ID;
  public static final Span NULL_SPAN = new Span(NullScope.INSTANCE);
  private TraceScope scope = null;
  protected org.htrace.Span span = null;

  public Span(TraceScope scope) {
    this.scope = scope;
    this.span = scope.getSpan();
  }

  public Span(org.htrace.Span span) {
    this.span = span;
  }

  public TraceScope getScope() {
    return scope;
  }

  public org.htrace.Span getSpan() {
    return span;
  }

  public long traceId() {
    return span.getTraceId();
  }

  public void data(String k, String v) {
    if (span != null)
      span.addKVAnnotation(k.getBytes(UTF_8), v.getBytes(UTF_8));
  }

  @Override
  public void stop() {
    if (scope == null) {
      if (span != null) {
        span.stop();
      }
    } else {
      scope.close();
    }
  }

  @Override
  public long getStartTimeMillis() {
    return span.getStartTimeMillis();
  }

  @Override
  public long getStopTimeMillis() {
    return span.getStopTimeMillis();
  }

  @Override
  public long getAccumulatedMillis() {
    return span.getAccumulatedMillis();
  }

  @Override
  public boolean isRunning() {
    return span.isRunning();
  }

  @Override
  public String getDescription() {
    return span.getDescription();
  }

  @Override
  public long getSpanId() {
    return span.getSpanId();
  }

  @Override
  public long getTraceId() {
    return span.getTraceId();
  }

  @Override
  public Span child(String s) {
    return new Span(span.child(s));
  }

  @Override
  public long getParentId() {
    return span.getParentId();
  }

  @Override
  public void addKVAnnotation(byte[] k, byte[] v) {
    span.addKVAnnotation(k, v);
  }

  @Override
  public void addTimelineAnnotation(String s) {
    span.addTimelineAnnotation(s);
  }

  @Override
  public Map<byte[], byte[]> getKVAnnotations() {
    return span.getKVAnnotations();
  }

  @Override
  public List<TimelineAnnotation> getTimelineAnnotations() {
    return span.getTimelineAnnotations();
  }

  @Override
  public String getProcessId() {
    return span.getProcessId();
  }

  @Override
  public String toString() {
    return span.toString();
  }

  public void start() {
    throw new UnsupportedOperationException("can't start span");
  }

  public long accumulatedMillis() {
    return getAccumulatedMillis();
  }

  public boolean running() {
    return isRunning();
  }

  public String description() {
    return getDescription();
  }

  public long spanId() {
    return getSpanId();
  }

  public Span parent() {
    throw new UnsupportedOperationException("can't get parent");
  }

  public long parentId() {
    return getParentId();
  }

  @Override
  public Map<String,String> getData() {
    Map<byte[],byte[]> data = span.getKVAnnotations();
    HashMap<String,String> stringData = new HashMap<>();
    for (Entry<byte[],byte[]> d : data.entrySet()) {
      stringData.put(new String(d.getKey(), UTF_8), new String(d.getValue(), UTF_8));
    }
    return stringData;
  }
 }
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/Trace.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/Trace.java
index 19171c445..5ad52fb87 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/Trace.java
++ b/trace/src/main/java/org/apache/accumulo/trace/instrument/Trace.java
@@ -17,79 +17,124 @@
 package org.apache.accumulo.trace.instrument;
 
 import org.apache.accumulo.trace.thrift.TInfo;
import org.htrace.TraceInfo;
import org.htrace.wrappers.TraceProxy;

import static java.nio.charset.StandardCharsets.UTF_8;
 
 /**
 * A Trace allows a user to gather global, distributed, detailed performance information while requesting a service. The general usage for a user is to do
 * something like this:
 * 
 * Trace.on("doSomething"); try { doSomething(); } finally { Trace.off(); }
 * 
 * This updates the environment for this thread, and data collection will occur whenever the thread encounters any Span notations in the code. The information
 * about the trace will also be carried over RPC calls as well. If the thread should hand off work to another thread, the environment can be carried with it, so
 * that the trace continues on the new thread.
 * Utility class for tracing within Accumulo.  Not intended for client use!
 *
  */
 public class Trace {
  
  // Initiate tracing if it isn't already started
  /**
   * Start a trace span with a given description.
   */
   public static Span on(String description) {
    return Tracer.getInstance().on(description);
    return on(description, Sampler.ALWAYS);
   }
  
  // Turn tracing off:

  /**
   * Start a trace span with a given description with the given sampler.
   */
  public static <T> Span on(String description, org.htrace.Sampler<T> sampler) {
    return new Span(org.htrace.Trace.startSpan(description, sampler));
  }

  /**
   * Finish the current trace.
   */
   public static void off() {
    Tracer.getInstance().stopTracing();
    Tracer.getInstance().flush();
    org.htrace.Span span = org.htrace.Trace.currentSpan();
    if (span != null) {
      span.stop();
      org.htrace.Tracer.getInstance().continueSpan(null);
    }
   }
  

  /**
   * @deprecated since 1.7, use {@link #off()} instead
   */
  @Deprecated
   public static void offNoFlush() {
    Tracer.getInstance().stopTracing();
    off();
   }
  
  // Are we presently tracing?

  /**
   * Returns whether tracing is currently on.
   */
   public static boolean isTracing() {
    return Tracer.getInstance().isTracing();
    return org.htrace.Trace.isTracing();
   }
  
  // If we are tracing, return the current span, else null

  /**
   * Return the current span.
   * @deprecated since 1.7 -- it is better to save the span you create in a local variable and call its methods, rather than retrieving the current span
   */
  @Deprecated
   public static Span currentTrace() {
    return Tracer.getInstance().currentTrace();
    return new Span(org.htrace.Trace.currentSpan());
   }
  
  // Create a new time span, if tracing is on

  /**
   * Get the trace id of the current span.
   */
  public static long currentTraceId() {
    return org.htrace.Trace.currentSpan().getTraceId();
  }

  /**
   * Start a new span with a given description, if already tracing.
   */
   public static Span start(String description) {
    return Tracer.getInstance().start(description);
    return new Span(org.htrace.Trace.startSpan(description));
   }
  
  // Start a trace in the current thread from information passed via RPC

  /**
   * Continue a trace by starting a new span with a given parent and description.
   */
   public static Span trace(TInfo info, String description) {
     if (info.traceId == 0) {
      return Tracer.NULL_SPAN;
      return Span.NULL_SPAN;
     }
    return Tracer.getInstance().continueTrace(description, info.traceId, info.parentId);
    TraceInfo ti = new TraceInfo(info.traceId, info.parentId);
    return new Span(org.htrace.Trace.startSpan(description, ti));
   }
  
  // Initiate a trace in this thread, starting now

  /**
   * Start a new span with a given description and parent.
   * @deprecated since 1.7 -- use htrace API
   */
  @Deprecated
   public static Span startThread(Span parent, String description) {
    return Tracer.getInstance().startThread(parent, description);
    return new Span(org.htrace.Trace.startSpan(description, parent.getSpan()));
   }
  
  // Stop a trace in this thread, starting now
  public static void endThread(Span span) {
    Tracer.getInstance().endThread(span);

  /**
   * Add data to the current span.
   */
  public static void data(String k, String v) {
    org.htrace.Span span = org.htrace.Trace.currentSpan();
    if (span != null)
      span.addKVAnnotation(k.getBytes(UTF_8), v.getBytes(UTF_8));
   }
  
  // Wrap the runnable in a new span, if tracing

  /**
   * Wrap a runnable in a TraceRunnable, if tracing.
   */
   public static Runnable wrap(Runnable runnable) {
    if (isTracing())
      return new TraceRunnable(Trace.currentTrace(), runnable);
    return runnable;
    if (isTracing()) {
      return new TraceRunnable(org.htrace.Trace.currentSpan(), runnable);
    } else {
      return runnable;
    }
   }
  

   // Wrap all calls to the given object with spans
   public static <T> T wrapAll(T instance) {
     return TraceProxy.trace(instance);
   }
  

   // Sample trace all calls to the given object
   public static <T> T wrapAll(T instance, Sampler dist) {
     return TraceProxy.trace(instance, dist);
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceCallable.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceCallable.java
index c3072b17e..f682d613f 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceCallable.java
++ b/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceCallable.java
@@ -16,6 +16,10 @@
  */
 package org.apache.accumulo.trace.instrument;
 
import org.htrace.Span;
import org.htrace.Trace;
import org.htrace.TraceScope;

 import java.util.concurrent.Callable;
 
 /**
@@ -25,27 +29,41 @@ import java.util.concurrent.Callable;
 public class TraceCallable<V> implements Callable<V> {
   private final Callable<V> impl;
   private final Span parent;
  private final String description;
   
   TraceCallable(Callable<V> impl) {
    this(Trace.currentTrace(), impl);
    this(Trace.currentSpan(), impl);
   }
   
   TraceCallable(Span parent, Callable<V> impl) {
    this(parent, impl, null);
  }

  TraceCallable(Span parent, Callable<V> impl, String description) {
     this.impl = impl;
     this.parent = parent;
    this.description = description;
   }
   
   @Override
   public V call() throws Exception {
     if (parent != null) {
      Span chunk = Trace.startThread(parent, Thread.currentThread().getName());
      TraceScope chunk = Trace.startSpan(getDescription(), parent);
       try {
         return impl.call();
       } finally {
        Trace.endThread(chunk);
        TraceExecutorService.endThread(chunk.getSpan());
       }
     } else {
       return impl.call();
     }
   }

  public Callable<V> getImpl() {
    return impl;
  }

  private String getDescription() {
    return this.description == null ? Thread.currentThread().getName() : description;
  }
 }
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceExecutorService.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceExecutorService.java
index 04dcc39e2..36563d799 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceExecutorService.java
++ b/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceExecutorService.java
@@ -16,6 +16,9 @@
  */
 package org.apache.accumulo.trace.instrument;
 
import org.htrace.Span;
import org.htrace.Tracer;

 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.List;
@@ -108,4 +111,13 @@ public class TraceExecutorService implements ExecutorService {
     return impl.invokeAny(wrapCollection(tasks), timeout, unit);
   }
   
  /**
   * Finish a given trace and set the span for the current thread to null.
   */
  public static void endThread(Span span) {
    if (span != null) {
      span.stop();
      Tracer.getInstance().continueSpan(null);
    }
  }
 }
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceProxy.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceProxy.java
deleted file mode 100644
index cb9321039..000000000
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceProxy.java
++ /dev/null
@@ -1,72 +0,0 @@
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
package org.apache.accumulo.trace.instrument;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.log4j.Logger;

public class TraceProxy {
  private static final Logger log = Logger.getLogger(TraceProxy.class);

  static final Sampler ALWAYS = new Sampler() {
    @Override
    public boolean next() {
      return true;
    }
  };

  public static <T> T trace(T instance) {
    return trace(instance, ALWAYS);
  }

  @SuppressWarnings("unchecked")
  public static <T> T trace(final T instance, final Sampler sampler) {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
        Span span = null;
        if (sampler.next()) {
          span = Trace.on(method.getName());
        }
        try {
          return method.invoke(instance, args);
          // Can throw RuntimeException, Error, or any checked exceptions of the method.
        } catch (InvocationTargetException ite) {
          Throwable cause = ite.getCause();
          if (cause == null) {
            // This should never happen, but account for it anyway
            log.error("Invocation exception during trace with null cause: ", ite);
            throw new RuntimeException(ite);
          }
          throw cause;
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } finally {
          if (span != null) {
            span.stop();
          }
        }
      }
    };
    return (T) Proxy.newProxyInstance(instance.getClass().getClassLoader(), instance.getClass().getInterfaces(), handler);
  }

}
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceRunnable.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceRunnable.java
index 41c765d25..0ddeb9ee2 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceRunnable.java
++ b/trace/src/main/java/org/apache/accumulo/trace/instrument/TraceRunnable.java
@@ -16,6 +16,10 @@
  */
 package org.apache.accumulo.trace.instrument;
 
import org.htrace.Span;
import org.htrace.Trace;
import org.htrace.TraceScope;

 /**
  * Wrap a Runnable with a Span that survives a change in threads.
  * 
@@ -24,29 +28,39 @@ public class TraceRunnable implements Runnable, Comparable<TraceRunnable> {
   
   private final Span parent;
   private final Runnable runnable;
  private final String description;
   
   public TraceRunnable(Runnable runnable) {
    this(Trace.currentTrace(), runnable);
    this(Trace.currentSpan(), runnable);
   }
   
   public TraceRunnable(Span parent, Runnable runnable) {
    this(parent, runnable, null);
  }

  public TraceRunnable(Span parent, Runnable runnable, String description) {
     this.parent = parent;
     this.runnable = runnable;
    this.description = description;
   }
   
   @Override
   public void run() {
     if (parent != null) {
      Span chunk = Trace.startThread(parent, Thread.currentThread().getName());
      TraceScope chunk = Trace.startSpan(getDescription(), parent);
       try {
         runnable.run();
       } finally {
        Trace.endThread(chunk);
        TraceExecutorService.endThread(chunk.getSpan());
       }
     } else {
       runnable.run();
     }
   }

  private String getDescription() {
    return this.description == null ? Thread.currentThread().getName() : description;
  }
   
   @Override
   public boolean equals(Object o) {
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/Tracer.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/Tracer.java
index d70aeeacb..246d1eb17 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/Tracer.java
++ b/trace/src/main/java/org/apache/accumulo/trace/instrument/Tracer.java
@@ -16,129 +16,20 @@
  */
 package org.apache.accumulo.trace.instrument;
 
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.accumulo.trace.instrument.impl.NullSpan;
import org.apache.accumulo.trace.instrument.impl.RootMilliSpan;
import org.apache.accumulo.trace.instrument.receivers.SpanReceiver;
 import org.apache.accumulo.trace.thrift.TInfo;
import org.htrace.Span;
 

/**
 * A Tracer provides the implementation for collecting and distributing Spans within a process.
 */
 public class Tracer {
  private final static Random random = new SecureRandom();
  private final List<SpanReceiver> receivers = new ArrayList<SpanReceiver>();
  
  private static final ThreadLocal<Span> currentTrace = new ThreadLocal<Span>();
  public static final NullSpan NULL_SPAN = new NullSpan();
  private static final TInfo dontTrace = new TInfo(0, 0);
  
  private static Tracer instance = null;
  
  synchronized public static void setInstance(Tracer tracer) {
    instance = tracer;
  }
  
  synchronized public static Tracer getInstance() {
    if (instance == null) {
      instance = new Tracer();
    }
    return instance;
  }
  
  private static final TInfo DONT_TRACE = new TInfo(0, 0);

  /**
   * Obtain {@link org.apache.accumulo.trace.thrift.TInfo} for the current span.
   */
   public static TInfo traceInfo() {
    Span span = currentTrace.get();
    Span span = org.htrace.Trace.currentSpan();
     if (span != null) {
      return new TInfo(span.traceId(), span.spanId());
    }
    return dontTrace;
  }
  
  public Span start(String description) {
    Span parent = currentTrace.get();
    if (parent == null)
      return NULL_SPAN;
    return push(parent.child(description));
  }
  
  public Span on(String description) {
    Span parent = currentTrace.get();
    Span root;
    if (parent == null) {
      root = new RootMilliSpan(description, random.nextLong(), random.nextLong(), Span.ROOT_SPAN_ID);
    } else {
      root = parent.child(description);
    }
    return push(root);
  }
  
  public Span startThread(Span parent, String activity) {
    return push(parent.child(activity));
  }
  
  public void endThread(Span span) {
    if (span != null) {
      span.stop();
      currentTrace.set(null);
    }
  }
  
  public boolean isTracing() {
    return currentTrace.get() != null;
  }
  
  public Span currentTrace() {
    return currentTrace.get();
  }
  
  public void stopTracing() {
    endThread(currentTrace());
  }
  
  protected void deliver(Span span) {
    for (SpanReceiver receiver : receivers) {
      receiver.span(span.traceId(), span.spanId(), span.parentId(), span.getStartTimeMillis(), span.getStopTimeMillis(), span.description(), span.getData());
      
    }
  }
  
  public synchronized void addReceiver(SpanReceiver receiver) {
    receivers.add(receiver);
  }
  
  public synchronized void removeReceiver(SpanReceiver receiver) {
    receivers.remove(receiver);
  }
  
  public Span push(Span span) {
    if (span != null) {
      currentTrace.set(span);
      span.start();
    }
    return span;
  }
  
  public void pop(Span span) {
    if (span != null) {
      deliver(span);
      currentTrace.set(span.parent());
    } else
      currentTrace.set(null);
  }
  
  public Span continueTrace(String description, long traceId, long parentId) {
    return push(new RootMilliSpan(description, traceId, random.nextLong(), parentId));
  }
  
  public void flush() {
    for (SpanReceiver receiver : receivers) {
      receiver.flush();
      return new TInfo(span.getTraceId(), span.getSpanId());
     }
    return DONT_TRACE;
   }
  
 }
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/impl/MilliSpan.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/impl/MilliSpan.java
deleted file mode 100644
index b641a2c08..000000000
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/impl/MilliSpan.java
++ /dev/null
@@ -1,141 +0,0 @@
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
package org.apache.accumulo.trace.instrument.impl;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.accumulo.trace.instrument.Span;
import org.apache.accumulo.trace.instrument.Tracer;


/**
 * A Span implementation that stores its information in milliseconds since the epoch.
 */
public class MilliSpan implements Span {
  
  private static final Random next = new SecureRandom();
  private long start;
  private long stop;
  final private Span parent;
  final private String description;
  final private long spanId;
  final private long traceId;
  private Map<String,String> traceInfo = null;
  
  public Span child(String description) {
    return new MilliSpan(description, next.nextLong(), traceId, this);
  }
  
  public MilliSpan(String description, long id, long traceId, Span parent) {
    this.description = description;
    this.spanId = id;
    this.traceId = traceId;
    this.parent = parent;
    this.start = 0;
    this.stop = 0;
  }
  
  public synchronized void start() {
    if (start > 0)
      throw new IllegalStateException("Span for " + description + " has already been started");
    start = System.currentTimeMillis();
  }
  
  public synchronized void stop() {
    if (start == 0)
      throw new IllegalStateException("Span for " + description + " has not been started");
    stop = System.currentTimeMillis();
    Tracer.getInstance().pop(this);
  }
  
  protected long currentTimeMillis() {
    return System.currentTimeMillis();
  }
  
  public synchronized boolean running() {
    return start != 0 && stop == 0;
  }
  
  public synchronized long accumulatedMillis() {
    if (start == 0)
      return 0;
    if (stop > 0)
      return stop - start;
    return currentTimeMillis() - start;
  }
  
  public String toString() {
    long parentId = parentId();
    return ("\"" + description() + "\" trace:" + Long.toHexString(traceId()) + " span:" + spanId + (parentId > 0 ? " parent:" + parentId : "") + " start:"
        + start + " ms: " + Long.toString(accumulatedMillis()) + (running() ? "..." : ""));
    
  }
  
  public String description() {
    return description;
  }
  
  @Override
  public long spanId() {
    return spanId;
  }
  
  @Override
  public Span parent() {
    return parent;
  }
  
  @Override
  public long parentId() {
    if (parent == null)
      return -1;
    return parent.spanId();
  }
  
  @Override
  public long traceId() {
    return traceId;
  }
  
  @Override
  public long getStartTimeMillis() {
    return start;
  }
  
  @Override
  public long getStopTimeMillis() {
    return stop;
  }
  
  @Override
  public void data(String key, String value) {
    if (traceInfo == null)
      traceInfo = new HashMap<String,String>();
    traceInfo.put(key, value);
  }
  
  @Override
  public Map<String,String> getData() {
    if (traceInfo == null)
      return Collections.emptyMap();
    return Collections.unmodifiableMap(traceInfo);
  }
}
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/impl/NullSpan.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/impl/NullSpan.java
deleted file mode 100644
index 916b6cf37..000000000
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/impl/NullSpan.java
++ /dev/null
@@ -1,102 +0,0 @@
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
package org.apache.accumulo.trace.instrument.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.accumulo.trace.instrument.Span;


/**
 * A Span that does nothing. Used to avoid returning and checking for nulls when we are not tracing.
 * 
 */
public class NullSpan implements Span {
  
  public NullSpan() {}
  
  @Override
  public long accumulatedMillis() {
    return 0;
  }
  
  @Override
  public String description() {
    return "NullSpan";
  }
  
  @Override
  public long getStartTimeMillis() {
    return 0;
  }
  
  @Override
  public long getStopTimeMillis() {
    return 0;
  }
  
  @Override
  public Span parent() {
    return null;
  }
  
  @Override
  public long parentId() {
    return -1;
  }
  
  @Override
  public boolean running() {
    return false;
  }
  
  @Override
  public long spanId() {
    return -1;
  }
  
  @Override
  public void start() {}
  
  @Override
  public void stop() {}
  
  @Override
  public long traceId() {
    return -1;
  }
  
  @Override
  public Span child(String description) {
    return this;
  }
  
  @Override
  public void data(String key, String value) {}
  
  @Override
  public String toString() {
    return "Not Tracing";
  }
  
  @Override
  public Map<String,String> getData() {
    return Collections.emptyMap();
  }
  
}
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/impl/RootMilliSpan.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/impl/RootMilliSpan.java
deleted file mode 100644
index c25e64442..000000000
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/impl/RootMilliSpan.java
++ /dev/null
@@ -1,43 +0,0 @@
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
package org.apache.accumulo.trace.instrument.impl;

/**
 * Span that roots the span tree in a process, but perhaps not the whole trace.
 * 
 */
public class RootMilliSpan extends MilliSpan {
  
  final long traceId;
  final long parentId;
  
  @Override
  public long traceId() {
    return traceId;
  }
  
  public RootMilliSpan(String description, long traceId, long spanId, long parentId) {
    super(description, spanId, traceId, null);
    this.traceId = traceId;
    this.parentId = parentId;
  }
  
  public long parentId() {
    return parentId;
  }
  
}
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/LogSpans.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/LogSpans.java
deleted file mode 100644
index dfed660a9..000000000
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/LogSpans.java
++ /dev/null
@@ -1,63 +0,0 @@
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
package org.apache.accumulo.trace.instrument.receivers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Level;

/**
 * A SpanReceiver that just logs the data using log4j.
 */
public class LogSpans implements SpanReceiver {
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LogSpans.class);
  
  static public class SpanLevel extends Level {
    
    private static final long serialVersionUID = 1L;
    
    protected SpanLevel() {
      super(Level.DEBUG_INT + 150, "SPAN", Level.DEBUG_INT + 150);
    }
    
    static public Level toLevel(int val) {
      if (val == Level.DEBUG_INT + 150)
        return Level.DEBUG;
      return Level.toLevel(val);
    }
  }
  
  public final static Level SPAN = new SpanLevel();
  
  public static String format(long traceId, long spanId, long parentId, long start, long stop, String description, Map<String,String> data) {
    String parentStr = "";
    if (parentId > 0)
      parentStr = " parent:" + parentId;
    String startStr = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(start));
    return String.format("%20s:%x id:%d%s start:%s ms:%d", description, traceId, spanId, parentStr, startStr, stop - start);
  }
  
  @Override
  public void span(long traceId, long spanId, long parentId, long start, long stop, String description, Map<String,String> data) {
    log.log(SPAN, format(traceId, spanId, parentId, start, stop, description, data));
  }
  
  @Override
  public void flush() {}
}
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/SpanReceiver.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/SpanReceiver.java
deleted file mode 100644
index b44e51edf..000000000
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/SpanReceiver.java
++ /dev/null
@@ -1,28 +0,0 @@
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
package org.apache.accumulo.trace.instrument.receivers;

import java.util.Map;

/**
 * The collector within a process that is the destination of Spans when a trace is running.
 */
public interface SpanReceiver {
  void span(long traceId, long spanId, long parentId, long start, long stop, String description, Map<String,String> data);
  
  void flush();
}
diff --git a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/ZooSpanClient.java b/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/ZooSpanClient.java
deleted file mode 100644
index 84e320475..000000000
-- a/trace/src/main/java/org/apache/accumulo/trace/instrument/receivers/ZooSpanClient.java
++ /dev/null
@@ -1,122 +0,0 @@
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
package org.apache.accumulo.trace.instrument.receivers;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;

/**
 * Find a Span collector via zookeeper and push spans there via Thrift RPC
 * 
 */
public class ZooSpanClient extends SendSpansViaThrift {
  
  private static final Logger log = Logger.getLogger(ZooSpanClient.class);
  private static final int TOTAL_TIME_WAIT_CONNECT_MS = 10 * 1000;
  private static final int TIME_WAIT_CONNECT_CHECK_MS = 100;
  
  ZooKeeper zoo = null;
  final String path;
  final Random random = new Random();
  final List<String> hosts = new ArrayList<String>();
  
  public ZooSpanClient(String keepers, final String path, String host, String service, long millis) throws IOException, KeeperException, InterruptedException {
    super(host, service, millis);
    this.path = path;
    zoo = new ZooKeeper(keepers, 30 * 1000, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        try {
          if (zoo != null) {
            updateHosts(path, zoo.getChildren(path, null));
          }
        } catch (Exception ex) {
          log.error("unable to get destination hosts in zookeeper", ex);
        }
      }
    });
    for (int i = 0; i < TOTAL_TIME_WAIT_CONNECT_MS; i += TIME_WAIT_CONNECT_CHECK_MS) {
      if (zoo.getState().equals(States.CONNECTED))
        break;
      try {
        Thread.sleep(TIME_WAIT_CONNECT_CHECK_MS);
      } catch (InterruptedException ex) {
        break;
      }
    }
    zoo.getChildren(path, true);
  }
  
  @Override
  public void flush() {
    if (!hosts.isEmpty())
      super.flush();
  }
  
  @Override
  void sendSpans() {
    if (hosts.isEmpty()) {
      if (!sendQueue.isEmpty()) {
        log.error("No hosts to send data to, dropping queued spans");
        synchronized (sendQueue) {
          sendQueue.clear();
          sendQueue.notifyAll();
        }
      }
    } else {
      super.sendSpans();
    }
  }
  
  synchronized private void updateHosts(String path, List<String> children) {
    log.debug("Scanning trace hosts in zookeeper: " + path);
    try {
      List<String> hosts = new ArrayList<String>();
      for (String child : children) {
        byte[] data = zoo.getData(path + "/" + child, null, null);
        hosts.add(new String(data, UTF_8));
      }
      this.hosts.clear();
      this.hosts.addAll(hosts);
      log.debug("Trace hosts: " + this.hosts);
    } catch (Exception ex) {
      log.error("unable to get destination hosts in zookeeper", ex);
    }
  }
  
  @Override
  synchronized protected String getSpanKey(Map<String,String> data) {
    if (hosts.size() > 0) {
      String host = hosts.get(random.nextInt(hosts.size()));
      log.debug("sending data to " + host);
      return host;
    }
    return null;
  }
}
diff --git a/trace/src/main/java/org/apache/accumulo/trace/thrift/Annotation.java b/trace/src/main/java/org/apache/accumulo/trace/thrift/Annotation.java
new file mode 100644
index 000000000..c00744eee
-- /dev/null
++ b/trace/src/main/java/org/apache/accumulo/trace/thrift/Annotation.java
@@ -0,0 +1,502 @@
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
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.accumulo.trace.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all") public class Annotation implements org.apache.thrift.TBase<Annotation, Annotation._Fields>, java.io.Serializable, Cloneable, Comparable<Annotation> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Annotation");

  private static final org.apache.thrift.protocol.TField TIME_FIELD_DESC = new org.apache.thrift.protocol.TField("time", org.apache.thrift.protocol.TType.I64, (short)1);
  private static final org.apache.thrift.protocol.TField MSG_FIELD_DESC = new org.apache.thrift.protocol.TField("msg", org.apache.thrift.protocol.TType.STRING, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new AnnotationStandardSchemeFactory());
    schemes.put(TupleScheme.class, new AnnotationTupleSchemeFactory());
  }

  public long time; // required
  public String msg; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  @SuppressWarnings("all") public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    TIME((short)1, "time"),
    MSG((short)2, "msg");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // TIME
          return TIME;
        case 2: // MSG
          return MSG;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __TIME_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.TIME, new org.apache.thrift.meta_data.FieldMetaData("time", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.MSG, new org.apache.thrift.meta_data.FieldMetaData("msg", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Annotation.class, metaDataMap);
  }

  public Annotation() {
  }

  public Annotation(
    long time,
    String msg)
  {
    this();
    this.time = time;
    setTimeIsSet(true);
    this.msg = msg;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public Annotation(Annotation other) {
    __isset_bitfield = other.__isset_bitfield;
    this.time = other.time;
    if (other.isSetMsg()) {
      this.msg = other.msg;
    }
  }

  public Annotation deepCopy() {
    return new Annotation(this);
  }

  @Override
  public void clear() {
    setTimeIsSet(false);
    this.time = 0;
    this.msg = null;
  }

  public long getTime() {
    return this.time;
  }

  public Annotation setTime(long time) {
    this.time = time;
    setTimeIsSet(true);
    return this;
  }

  public void unsetTime() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __TIME_ISSET_ID);
  }

  /** Returns true if field time is set (has been assigned a value) and false otherwise */
  public boolean isSetTime() {
    return EncodingUtils.testBit(__isset_bitfield, __TIME_ISSET_ID);
  }

  public void setTimeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __TIME_ISSET_ID, value);
  }

  public String getMsg() {
    return this.msg;
  }

  public Annotation setMsg(String msg) {
    this.msg = msg;
    return this;
  }

  public void unsetMsg() {
    this.msg = null;
  }

  /** Returns true if field msg is set (has been assigned a value) and false otherwise */
  public boolean isSetMsg() {
    return this.msg != null;
  }

  public void setMsgIsSet(boolean value) {
    if (!value) {
      this.msg = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case TIME:
      if (value == null) {
        unsetTime();
      } else {
        setTime((Long)value);
      }
      break;

    case MSG:
      if (value == null) {
        unsetMsg();
      } else {
        setMsg((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case TIME:
      return Long.valueOf(getTime());

    case MSG:
      return getMsg();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case TIME:
      return isSetTime();
    case MSG:
      return isSetMsg();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof Annotation)
      return this.equals((Annotation)that);
    return false;
  }

  public boolean equals(Annotation that) {
    if (that == null)
      return false;

    boolean this_present_time = true;
    boolean that_present_time = true;
    if (this_present_time || that_present_time) {
      if (!(this_present_time && that_present_time))
        return false;
      if (this.time != that.time)
        return false;
    }

    boolean this_present_msg = true && this.isSetMsg();
    boolean that_present_msg = true && that.isSetMsg();
    if (this_present_msg || that_present_msg) {
      if (!(this_present_msg && that_present_msg))
        return false;
      if (!this.msg.equals(that.msg))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(Annotation other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetTime()).compareTo(other.isSetTime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.time, other.time);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMsg()).compareTo(other.isSetMsg());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMsg()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.msg, other.msg);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Annotation(");
    boolean first = true;

    sb.append("time:");
    sb.append(this.time);
    first = false;
    if (!first) sb.append(", ");
    sb.append("msg:");
    if (this.msg == null) {
      sb.append("null");
    } else {
      sb.append(this.msg);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class AnnotationStandardSchemeFactory implements SchemeFactory {
    public AnnotationStandardScheme getScheme() {
      return new AnnotationStandardScheme();
    }
  }

  private static class AnnotationStandardScheme extends StandardScheme<Annotation> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, Annotation struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // TIME
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.time = iprot.readI64();
              struct.setTimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // MSG
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.msg = iprot.readString();
              struct.setMsgIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, Annotation struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(TIME_FIELD_DESC);
      oprot.writeI64(struct.time);
      oprot.writeFieldEnd();
      if (struct.msg != null) {
        oprot.writeFieldBegin(MSG_FIELD_DESC);
        oprot.writeString(struct.msg);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class AnnotationTupleSchemeFactory implements SchemeFactory {
    public AnnotationTupleScheme getScheme() {
      return new AnnotationTupleScheme();
    }
  }

  private static class AnnotationTupleScheme extends TupleScheme<Annotation> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, Annotation struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetTime()) {
        optionals.set(0);
      }
      if (struct.isSetMsg()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetTime()) {
        oprot.writeI64(struct.time);
      }
      if (struct.isSetMsg()) {
        oprot.writeString(struct.msg);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, Annotation struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.time = iprot.readI64();
        struct.setTimeIsSet(true);
      }
      if (incoming.get(1)) {
        struct.msg = iprot.readString();
        struct.setMsgIsSet(true);
      }
    }
  }

}

diff --git a/trace/src/main/java/org/apache/accumulo/trace/thrift/RemoteSpan.java b/trace/src/main/java/org/apache/accumulo/trace/thrift/RemoteSpan.java
index 416ae1792..bfe183d3e 100644
-- a/trace/src/main/java/org/apache/accumulo/trace/thrift/RemoteSpan.java
++ b/trace/src/main/java/org/apache/accumulo/trace/thrift/RemoteSpan.java
@@ -59,7 +59,8 @@ import org.slf4j.LoggerFactory;
   private static final org.apache.thrift.protocol.TField START_FIELD_DESC = new org.apache.thrift.protocol.TField("start", org.apache.thrift.protocol.TType.I64, (short)6);
   private static final org.apache.thrift.protocol.TField STOP_FIELD_DESC = new org.apache.thrift.protocol.TField("stop", org.apache.thrift.protocol.TType.I64, (short)7);
   private static final org.apache.thrift.protocol.TField DESCRIPTION_FIELD_DESC = new org.apache.thrift.protocol.TField("description", org.apache.thrift.protocol.TType.STRING, (short)8);
  private static final org.apache.thrift.protocol.TField DATA_FIELD_DESC = new org.apache.thrift.protocol.TField("data", org.apache.thrift.protocol.TType.MAP, (short)9);
  private static final org.apache.thrift.protocol.TField DATA_FIELD_DESC = new org.apache.thrift.protocol.TField("data", org.apache.thrift.protocol.TType.MAP, (short)10);
  private static final org.apache.thrift.protocol.TField ANNOTATIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("annotations", org.apache.thrift.protocol.TType.LIST, (short)11);
 
   private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
   static {
@@ -75,7 +76,8 @@ import org.slf4j.LoggerFactory;
   public long start; // required
   public long stop; // required
   public String description; // required
  public Map<String,String> data; // required
  public Map<ByteBuffer,ByteBuffer> data; // required
  public List<Annotation> annotations; // required
 
   /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
   @SuppressWarnings("all") public enum _Fields implements org.apache.thrift.TFieldIdEnum {
@@ -87,7 +89,8 @@ import org.slf4j.LoggerFactory;
     START((short)6, "start"),
     STOP((short)7, "stop"),
     DESCRIPTION((short)8, "description"),
    DATA((short)9, "data");
    DATA((short)10, "data"),
    ANNOTATIONS((short)11, "annotations");
 
     private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
@@ -118,8 +121,10 @@ import org.slf4j.LoggerFactory;
           return STOP;
         case 8: // DESCRIPTION
           return DESCRIPTION;
        case 9: // DATA
        case 10: // DATA
           return DATA;
        case 11: // ANNOTATIONS
          return ANNOTATIONS;
         default:
           return null;
       }
@@ -187,8 +192,11 @@ import org.slf4j.LoggerFactory;
         new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
     tmpMap.put(_Fields.DATA, new org.apache.thrift.meta_data.FieldMetaData("data", org.apache.thrift.TFieldRequirementType.DEFAULT, 
         new org.apache.thrift.meta_data.MapMetaData(org.apache.thrift.protocol.TType.MAP, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING), 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING            , true), 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING            , true))));
    tmpMap.put(_Fields.ANNOTATIONS, new org.apache.thrift.meta_data.FieldMetaData("annotations", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, Annotation.class))));
     metaDataMap = Collections.unmodifiableMap(tmpMap);
     org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(RemoteSpan.class, metaDataMap);
   }
@@ -205,7 +213,8 @@ import org.slf4j.LoggerFactory;
     long start,
     long stop,
     String description,
    Map<String,String> data)
    Map<ByteBuffer,ByteBuffer> data,
    List<Annotation> annotations)
   {
     this();
     this.sender = sender;
@@ -222,6 +231,7 @@ import org.slf4j.LoggerFactory;
     setStopIsSet(true);
     this.description = description;
     this.data = data;
    this.annotations = annotations;
   }
 
   /**
@@ -244,9 +254,16 @@ import org.slf4j.LoggerFactory;
       this.description = other.description;
     }
     if (other.isSetData()) {
      Map<String,String> __this__data = new HashMap<String,String>(other.data);
      Map<ByteBuffer,ByteBuffer> __this__data = new HashMap<ByteBuffer,ByteBuffer>(other.data);
       this.data = __this__data;
     }
    if (other.isSetAnnotations()) {
      List<Annotation> __this__annotations = new ArrayList<Annotation>(other.annotations.size());
      for (Annotation other_element : other.annotations) {
        __this__annotations.add(new Annotation(other_element));
      }
      this.annotations = __this__annotations;
    }
   }
 
   public RemoteSpan deepCopy() {
@@ -269,6 +286,7 @@ import org.slf4j.LoggerFactory;
     this.stop = 0;
     this.description = null;
     this.data = null;
    this.annotations = null;
   }
 
   public String getSender() {
@@ -462,18 +480,18 @@ import org.slf4j.LoggerFactory;
     return (this.data == null) ? 0 : this.data.size();
   }
 
  public void putToData(String key, String val) {
  public void putToData(ByteBuffer key, ByteBuffer val) {
     if (this.data == null) {
      this.data = new HashMap<String,String>();
      this.data = new HashMap<ByteBuffer,ByteBuffer>();
     }
     this.data.put(key, val);
   }
 
  public Map<String,String> getData() {
  public Map<ByteBuffer,ByteBuffer> getData() {
     return this.data;
   }
 
  public RemoteSpan setData(Map<String,String> data) {
  public RemoteSpan setData(Map<ByteBuffer,ByteBuffer> data) {
     this.data = data;
     return this;
   }
@@ -493,6 +511,45 @@ import org.slf4j.LoggerFactory;
     }
   }
 
  public int getAnnotationsSize() {
    return (this.annotations == null) ? 0 : this.annotations.size();
  }

  public java.util.Iterator<Annotation> getAnnotationsIterator() {
    return (this.annotations == null) ? null : this.annotations.iterator();
  }

  public void addToAnnotations(Annotation elem) {
    if (this.annotations == null) {
      this.annotations = new ArrayList<Annotation>();
    }
    this.annotations.add(elem);
  }

  public List<Annotation> getAnnotations() {
    return this.annotations;
  }

  public RemoteSpan setAnnotations(List<Annotation> annotations) {
    this.annotations = annotations;
    return this;
  }

  public void unsetAnnotations() {
    this.annotations = null;
  }

  /** Returns true if field annotations is set (has been assigned a value) and false otherwise */
  public boolean isSetAnnotations() {
    return this.annotations != null;
  }

  public void setAnnotationsIsSet(boolean value) {
    if (!value) {
      this.annotations = null;
    }
  }

   public void setFieldValue(_Fields field, Object value) {
     switch (field) {
     case SENDER:
@@ -563,7 +620,15 @@ import org.slf4j.LoggerFactory;
       if (value == null) {
         unsetData();
       } else {
        setData((Map<String,String>)value);
        setData((Map<ByteBuffer,ByteBuffer>)value);
      }
      break;

    case ANNOTATIONS:
      if (value == null) {
        unsetAnnotations();
      } else {
        setAnnotations((List<Annotation>)value);
       }
       break;
 
@@ -599,6 +664,9 @@ import org.slf4j.LoggerFactory;
     case DATA:
       return getData();
 
    case ANNOTATIONS:
      return getAnnotations();

     }
     throw new IllegalStateException();
   }
@@ -628,6 +696,8 @@ import org.slf4j.LoggerFactory;
       return isSetDescription();
     case DATA:
       return isSetData();
    case ANNOTATIONS:
      return isSetAnnotations();
     }
     throw new IllegalStateException();
   }
@@ -726,6 +796,15 @@ import org.slf4j.LoggerFactory;
         return false;
     }
 
    boolean this_present_annotations = true && this.isSetAnnotations();
    boolean that_present_annotations = true && that.isSetAnnotations();
    if (this_present_annotations || that_present_annotations) {
      if (!(this_present_annotations && that_present_annotations))
        return false;
      if (!this.annotations.equals(that.annotations))
        return false;
    }

     return true;
   }
 
@@ -832,6 +911,16 @@ import org.slf4j.LoggerFactory;
         return lastComparison;
       }
     }
    lastComparison = Boolean.valueOf(isSetAnnotations()).compareTo(other.isSetAnnotations());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAnnotations()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.annotations, other.annotations);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
     return 0;
   }
 
@@ -903,6 +992,14 @@ import org.slf4j.LoggerFactory;
       sb.append(this.data);
     }
     first = false;
    if (!first) sb.append(", ");
    sb.append("annotations:");
    if (this.annotations == null) {
      sb.append("null");
    } else {
      sb.append(this.annotations);
    }
    first = false;
     sb.append(")");
     return sb.toString();
   }
@@ -1012,17 +1109,17 @@ import org.slf4j.LoggerFactory;
               org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
             }
             break;
          case 9: // DATA
          case 10: // DATA
             if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
               {
                 org.apache.thrift.protocol.TMap _map0 = iprot.readMapBegin();
                struct.data = new HashMap<String,String>(2*_map0.size);
                struct.data = new HashMap<ByteBuffer,ByteBuffer>(2*_map0.size);
                 for (int _i1 = 0; _i1 < _map0.size; ++_i1)
                 {
                  String _key2;
                  String _val3;
                  _key2 = iprot.readString();
                  _val3 = iprot.readString();
                  ByteBuffer _key2;
                  ByteBuffer _val3;
                  _key2 = iprot.readBinary();
                  _val3 = iprot.readBinary();
                   struct.data.put(_key2, _val3);
                 }
                 iprot.readMapEnd();
@@ -1032,6 +1129,25 @@ import org.slf4j.LoggerFactory;
               org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
             }
             break;
          case 11: // ANNOTATIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list4 = iprot.readListBegin();
                struct.annotations = new ArrayList<Annotation>(_list4.size);
                for (int _i5 = 0; _i5 < _list4.size; ++_i5)
                {
                  Annotation _elem6;
                  _elem6 = new Annotation();
                  _elem6.read(iprot);
                  struct.annotations.add(_elem6);
                }
                iprot.readListEnd();
              }
              struct.setAnnotationsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
           default:
             org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
         }
@@ -1081,15 +1197,27 @@ import org.slf4j.LoggerFactory;
         oprot.writeFieldBegin(DATA_FIELD_DESC);
         {
           oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.data.size()));
          for (Map.Entry<String, String> _iter4 : struct.data.entrySet())
          for (Map.Entry<ByteBuffer, ByteBuffer> _iter7 : struct.data.entrySet())
           {
            oprot.writeString(_iter4.getKey());
            oprot.writeString(_iter4.getValue());
            oprot.writeBinary(_iter7.getKey());
            oprot.writeBinary(_iter7.getValue());
           }
           oprot.writeMapEnd();
         }
         oprot.writeFieldEnd();
       }
      if (struct.annotations != null) {
        oprot.writeFieldBegin(ANNOTATIONS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.annotations.size()));
          for (Annotation _iter8 : struct.annotations)
          {
            _iter8.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
       oprot.writeFieldStop();
       oprot.writeStructEnd();
     }
@@ -1135,7 +1263,10 @@ import org.slf4j.LoggerFactory;
       if (struct.isSetData()) {
         optionals.set(8);
       }
      oprot.writeBitSet(optionals, 9);
      if (struct.isSetAnnotations()) {
        optionals.set(9);
      }
      oprot.writeBitSet(optionals, 10);
       if (struct.isSetSender()) {
         oprot.writeString(struct.sender);
       }
@@ -1163,10 +1294,19 @@ import org.slf4j.LoggerFactory;
       if (struct.isSetData()) {
         {
           oprot.writeI32(struct.data.size());
          for (Map.Entry<String, String> _iter5 : struct.data.entrySet())
          for (Map.Entry<ByteBuffer, ByteBuffer> _iter9 : struct.data.entrySet())
           {
            oprot.writeString(_iter5.getKey());
            oprot.writeString(_iter5.getValue());
            oprot.writeBinary(_iter9.getKey());
            oprot.writeBinary(_iter9.getValue());
          }
        }
      }
      if (struct.isSetAnnotations()) {
        {
          oprot.writeI32(struct.annotations.size());
          for (Annotation _iter10 : struct.annotations)
          {
            _iter10.write(oprot);
           }
         }
       }
@@ -1175,7 +1315,7 @@ import org.slf4j.LoggerFactory;
     @Override
     public void read(org.apache.thrift.protocol.TProtocol prot, RemoteSpan struct) throws org.apache.thrift.TException {
       TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(9);
      BitSet incoming = iprot.readBitSet(10);
       if (incoming.get(0)) {
         struct.sender = iprot.readString();
         struct.setSenderIsSet(true);
@@ -1210,19 +1350,33 @@ import org.slf4j.LoggerFactory;
       }
       if (incoming.get(8)) {
         {
          org.apache.thrift.protocol.TMap _map6 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.data = new HashMap<String,String>(2*_map6.size);
          for (int _i7 = 0; _i7 < _map6.size; ++_i7)
          org.apache.thrift.protocol.TMap _map11 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.data = new HashMap<ByteBuffer,ByteBuffer>(2*_map11.size);
          for (int _i12 = 0; _i12 < _map11.size; ++_i12)
           {
            String _key8;
            String _val9;
            _key8 = iprot.readString();
            _val9 = iprot.readString();
            struct.data.put(_key8, _val9);
            ByteBuffer _key13;
            ByteBuffer _val14;
            _key13 = iprot.readBinary();
            _val14 = iprot.readBinary();
            struct.data.put(_key13, _val14);
           }
         }
         struct.setDataIsSet(true);
       }
      if (incoming.get(9)) {
        {
          org.apache.thrift.protocol.TList _list15 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.annotations = new ArrayList<Annotation>(_list15.size);
          for (int _i16 = 0; _i16 < _list15.size; ++_i16)
          {
            Annotation _elem17;
            _elem17 = new Annotation();
            _elem17.read(iprot);
            struct.annotations.add(_elem17);
          }
        }
        struct.setAnnotationsIsSet(true);
      }
     }
   }
 
diff --git a/trace/src/main/thrift/trace.thrift b/trace/src/main/thrift/trace.thrift
index 76bcafe74..b7e0abf43 100644
-- a/trace/src/main/thrift/trace.thrift
++ b/trace/src/main/thrift/trace.thrift
@@ -17,6 +17,11 @@
 namespace java org.apache.accumulo.trace.thrift
 namespace cpp org.apache.accumulo.trace.thrift
 
struct Annotation {
   1:i64 time,
   2:string msg
}

 struct RemoteSpan {
    1:string sender,
    2:string svc, 
@@ -26,7 +31,8 @@ struct RemoteSpan {
    6:i64 start, 
    7:i64 stop, 
    8:string description, 
   9:map<string, string> data
   10:map<binary, binary> data,
   11:list<Annotation> annotations
 }
 
 struct TInfo {
diff --git a/trace/src/test/java/org/apache/accumulo/trace/instrument/TracerTest.java b/trace/src/test/java/org/apache/accumulo/trace/instrument/TracerTest.java
index f338bd8fb..4afdebe01 100644
-- a/trace/src/test/java/org/apache/accumulo/trace/instrument/TracerTest.java
++ b/trace/src/test/java/org/apache/accumulo/trace/instrument/TracerTest.java
@@ -22,6 +22,7 @@ import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertTrue;
 
 import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
 import java.net.ServerSocket;
 import java.net.Socket;
 import java.util.ArrayList;
@@ -30,7 +31,6 @@ import java.util.List;
 import java.util.Map;
 import java.util.concurrent.Callable;
 
import org.apache.accumulo.trace.instrument.receivers.SpanReceiver;
 import org.apache.accumulo.trace.instrument.thrift.TraceWrap;
 import org.apache.accumulo.trace.thrift.TInfo;
 import org.apache.accumulo.trace.thrift.TestService;
@@ -43,13 +43,17 @@ import org.apache.thrift.server.TThreadPoolServer;
 import org.apache.thrift.transport.TServerSocket;
 import org.apache.thrift.transport.TSocket;
 import org.apache.thrift.transport.TTransport;
import org.htrace.HTraceConfiguration;
import org.htrace.Sampler;
import org.htrace.SpanReceiver;
import org.htrace.wrappers.TraceProxy;
 import org.junit.Before;
 import org.junit.Test;
 
 
 public class TracerTest {
   static class SpanStruct {
    public SpanStruct(long traceId, long spanId, long parentId, long start, long stop, String description, Map<String,String> data) {
    public SpanStruct(long traceId, long spanId, long parentId, long start, long stop, String description, Map<byte[],byte[]> data) {
       super();
       this.traceId = traceId;
       this.spanId = spanId;
@@ -66,7 +70,7 @@ public class TracerTest {
     public long start;
     public long stop;
     public String description;
    public Map<String,String> data;
    public Map<byte[],byte[]> data;
     
     public long millis() {
       return stop - start;
@@ -77,21 +81,29 @@ public class TracerTest {
     public Map<Long,List<SpanStruct>> traces = new HashMap<Long,List<SpanStruct>>();
     
     @Override
    public void span(long traceId, long spanId, long parentId, long start, long stop, String description, Map<String,String> data) {
      SpanStruct span = new SpanStruct(traceId, spanId, parentId, start, stop, description, data);
    public void receiveSpan(org.htrace.Span s)  {
      long traceId = s.getTraceId();
      SpanStruct span = new SpanStruct(traceId, s.getSpanId(), s.getParentId(), s.getStartTimeMillis(), s.getStopTimeMillis(), s.getDescription(),
          s.getKVAnnotations());
       if (!traces.containsKey(traceId))
         traces.put(traceId, new ArrayList<SpanStruct>());
       traces.get(traceId).add(span);
     }
     
     @Override
    public void flush() {}
    public void configure(HTraceConfiguration conf) {
    }

    @Override
    public void close() throws IOException {
    }
   }
   
  @SuppressWarnings("deprecation")
   @Test
   public void testTrace() throws Exception {
     TestReceiver tracer = new TestReceiver();
    Tracer.getInstance().addReceiver(tracer);
    org.htrace.Trace.addReceiver(tracer);
     
     assertFalse(Trace.isTracing());
     Trace.start("nop").stop();
@@ -103,12 +115,12 @@ public class TracerTest {
     assertFalse(Trace.isTracing());
     
     Span start = Trace.on("testing");
    assertEquals(Trace.currentTrace(), start);
    assertEquals(Trace.currentTrace().getSpan(), start.getScope().getSpan());
     assertTrue(Trace.isTracing());
     
    Trace.start("shortest trace ever");
    Trace.currentTrace().stop();
    long traceId = Trace.currentTrace().traceId();
    Span span = Trace.start("shortest trace ever");
    span.stop();
    long traceId = Trace.currentTraceId();
     assertNotNull(tracer.traces.get(traceId));
     assertTrue(tracer.traces.get(traceId).size() == 1);
     assertEquals("shortest trace ever", tracer.traces.get(traceId).get(0).description);
@@ -149,7 +161,7 @@ public class TracerTest {
   @Test
   public void testThrift() throws Exception {
     TestReceiver tracer = new TestReceiver();
    Tracer.getInstance().addReceiver(tracer);
    org.htrace.Trace.addReceiver(tracer);
     
     ServerSocket socket = new ServerSocket(0);
     TServerSocket transport = new TServerSocket(socket);
@@ -195,25 +207,26 @@ public class TracerTest {
   }
 
   /**
   * Verify that exceptions propagate up through the trace wrapping with sampling enabled, instead of seeing the reflexive exceptions.
   * Verify that exceptions propagate up through the trace wrapping with sampling enabled, as the cause of the reflexive exceptions.
    */
   @Test(expected = IOException.class)
  public void testTracedException() throws Exception {
    TraceProxy.trace(callable).call();
  public void testTracedException() throws Throwable {
    try {
      TraceProxy.trace(callable).call();
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
   }
 
   /**
   * Verify that exceptions propagate up through the trace wrapping with sampling disabled, instead of seeing the reflexive exceptions.
   * Verify that exceptions propagate up through the trace wrapping with sampling disabled, as the cause of the reflexive exceptions.
    */
   @Test(expected = IOException.class)
  public void testUntracedException() throws Exception {
    Sampler never = new Sampler() {
      @Override
      public boolean next() {
        return false;
      }
    };

    TraceProxy.trace(callable, never).call();
  public void testUntracedException() throws Throwable {
    try {
      TraceProxy.trace(callable, Sampler.NEVER).call();
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
   }
 }
- 
2.19.1.windows.1

