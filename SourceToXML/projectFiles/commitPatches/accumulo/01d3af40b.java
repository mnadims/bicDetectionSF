From 01d3af40b475708b252186c5a659a2128e887442 Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Fri, 2 May 2014 16:33:24 -0400
Subject: [PATCH] ACCUMULO-1691 Support Thrift 0.9.1

  Enforce version 0.9.1 until THRIFT-2173 is implemented, and provide a custom
  server so we can access the underlying transport to get the client address.
--
 pom.xml                                       |  40 ++-
 .../server/util/CustomNonBlockingServer.java  | 268 ++++++++++++++++++
 .../accumulo/server/util/TServerUtils.java    |  41 +--
 3 files changed, 299 insertions(+), 50 deletions(-)
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/util/CustomNonBlockingServer.java

diff --git a/pom.xml b/pom.xml
index 9c9e61d3e..14ba19faf 100644
-- a/pom.xml
++ b/pom.xml
@@ -134,6 +134,8 @@
     <sealJars>false</sealJars>
     <!-- overwritten in profiles hadoop-1 or hadoop-2 -->
     <slf4j.version>1.7.5</slf4j.version>
    <!-- Thrift version -->
    <thrift.version>0.9.1</thrift.version>
     <!-- ZooKeeper 3.4.x works also, but we're not using new features yet; this ensures 3.3.x compatibility. -->
     <zookeeper.version>3.3.6</zookeeper.version>
   </properties>
@@ -380,7 +382,7 @@
       <dependency>
         <groupId>org.apache.thrift</groupId>
         <artifactId>libthrift</artifactId>
        <version>0.9.0</version>
        <version>${thrift.version}</version>
         <exclusions>
           <exclusion>
             <groupId>org.apache.httpcomponents</groupId>
@@ -788,6 +790,29 @@
             </lifecycleMappingMetadata>
           </configuration>
         </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-enforcer-plugin</artifactId>
          <configuration>
            <rules>
              <requireMavenVersion>
                <version>[${maven.min-version},)</version>
              </requireMavenVersion>
              <requireProperty>
                <property>hadoop.profile</property>
                <regex>(1|2)</regex>
                <regexMessage>You should specify the Hadoop profile by major Hadoop generation, i.e. 1 or 2, not by a version number.
  Use hadoop.version to use a particular Hadoop version within that generation. See README for more details.</regexMessage>
              </requireProperty>
              <requireProperty>
                <property>thrift.version</property>
                <regex>0[.]9[.]1</regex>
                <regexMessage>Thrift version must be 0.9.1; Any alteration requires a review of ACCUMULO-1691
                  (See server/base/src/main/java/org/apache/accumulo/server/util/CustomNonBlockingServer.java)</regexMessage>
              </requireProperty>
            </rules>
          </configuration>
        </plugin>
       </plugins>
     </pluginManagement>
     <plugins>
@@ -814,19 +839,6 @@
             <goals>
               <goal>enforce</goal>
             </goals>
            <configuration>
              <rules>
                <requireMavenVersion>
                  <version>[${maven.min-version},)</version>
                </requireMavenVersion>
                <requireProperty>
                  <property>hadoop.profile</property>
                  <regex>(1|2)</regex>
                  <regexMessage>You should specify the Hadoop profile by major Hadoop generation, i.e. 1 or 2, not by a version number.
    Use hadoop.version to use a particular Hadoop version within that generation. See README for more details.</regexMessage>
                </requireProperty>
              </rules>
            </configuration>
           </execution>
         </executions>
       </plugin>
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/CustomNonBlockingServer.java b/server/base/src/main/java/org/apache/accumulo/server/util/CustomNonBlockingServer.java
new file mode 100644
index 000000000..0f01068e1
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/util/CustomNonBlockingServer.java
@@ -0,0 +1,268 @@
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
package org.apache.accumulo.server.util;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * This class implements a custom non-blocking thrift server, incorporating the {@link THsHaServer} features, and overriding the underlying
 * {@link TNonblockingServer} methods, especially {@link org.apache.thrift.server.TNonblockingServer.SelectAcceptThread}, in order to override the
 * {@link org.apache.thrift.server.AbstractNonblockingServer.FrameBuffer} and {@link org.apache.thrift.server.AbstractNonblockingServer.AsyncFrameBuffer} with
 * one that reveals the client address from its transport.
 * 
 * <p>
 * The justification for this is explained in https://issues.apache.org/jira/browse/ACCUMULO-1691, and is needed due to the repeated regressions:
 * <ul>
 * <li>https://issues.apache.org/jira/browse/THRIFT-958</li>
 * <li>https://issues.apache.org/jira/browse/THRIFT-1464</li>
 * <li>https://issues.apache.org/jira/browse/THRIFT-2173</li>
 * </ul>
 * 
 * <p>
 * This class contains a copy of {@link org.apache.thrift.server.TNonblockingServer.SelectAcceptThread} from Thrift 0.9.1, with the slight modification of
 * instantiating a custom FrameBuffer, rather than the {@link org.apache.thrift.server.AbstractNonblockingServer.FrameBuffer} and
 * {@link org.apache.thrift.server.AbstractNonblockingServer.AsyncFrameBuffer}. Because of this, any change in the implementation upstream will require a review
 * of this implementation here, to ensure any new bugfixes/features in the upstream Thrift class are also applied here, at least until
 * https://issues.apache.org/jira/browse/THRIFT-2173 is implemented. In the meantime, the maven-enforcer-plugin ensures that Thrift remains at version 0.9.1,
 * which has been reviewed and tested.
 */
public class CustomNonBlockingServer extends THsHaServer {

  private static final Logger LOGGER = Logger.getLogger(CustomNonBlockingServer.class);
  private SelectAcceptThread selectAcceptThread_;
  private volatile boolean stopped_ = false;

  public CustomNonBlockingServer(Args args) {
    super(args);
  }

  @Override
  protected Runnable getRunnable(final FrameBuffer frameBuffer) {
    return new Runnable() {
      @Override
      public void run() {
        if (frameBuffer instanceof CustomNonblockingFrameBuffer) {
          TNonblockingTransport trans = ((CustomNonblockingFrameBuffer) frameBuffer).getTransport();
          if (trans instanceof TNonblockingSocket) {
            TNonblockingSocket tsock = (TNonblockingSocket) trans;
            Socket sock = tsock.getSocketChannel().socket();
            TServerUtils.clientAddress.set(sock.getInetAddress().getHostAddress() + ":" + sock.getPort());
          }
        }
        frameBuffer.invoke();
      }
    };
  }

  @Override
  protected boolean startThreads() {
    // start the selector
    try {
      selectAcceptThread_ = new SelectAcceptThread((TNonblockingServerTransport) serverTransport_);
      selectAcceptThread_.start();
      return true;
    } catch (IOException e) {
      LOGGER.error("Failed to start selector thread!", e);
      return false;
    }
  }

  @Override
  public void stop() {
    stopped_ = true;
    if (selectAcceptThread_ != null) {
      selectAcceptThread_.wakeupSelector();
    }
  }

  @Override
  public boolean isStopped() {
    return selectAcceptThread_.isStopped();
  }

  @Override
  protected void joinSelector() {
    // wait until the selector thread exits
    try {
      selectAcceptThread_.join();
    } catch (InterruptedException e) {
      // for now, just silently ignore. technically this means we'll have less of
      // a graceful shutdown as a result.
    }
  }

  private interface CustomNonblockingFrameBuffer {
    TNonblockingTransport getTransport();
  }

  private class CustomAsyncFrameBuffer extends AsyncFrameBuffer implements CustomNonblockingFrameBuffer {
    private TNonblockingTransport trans;

    public CustomAsyncFrameBuffer(TNonblockingTransport trans, SelectionKey selectionKey, AbstractSelectThread selectThread) {
      super(trans, selectionKey, selectThread);
      this.trans = trans;
    }

    @Override
    public TNonblockingTransport getTransport() {
      return trans;
    }
  }

  private class CustomFrameBuffer extends FrameBuffer implements CustomNonblockingFrameBuffer {
    private TNonblockingTransport trans;

    public CustomFrameBuffer(TNonblockingTransport trans, SelectionKey selectionKey, AbstractSelectThread selectThread) {
      super(trans, selectionKey, selectThread);
      this.trans = trans;
    }

    @Override
    public TNonblockingTransport getTransport() {
      return trans;
    }
  }

  // @formatter:off
  private class SelectAcceptThread extends AbstractSelectThread {

    // The server transport on which new client transports will be accepted
    private final TNonblockingServerTransport serverTransport;

    /**
     * Set up the thread that will handle the non-blocking accepts, reads, and
     * writes.
     */
    public SelectAcceptThread(final TNonblockingServerTransport serverTransport)
    throws IOException {
      this.serverTransport = serverTransport;
      serverTransport.registerSelector(selector);
    }

    public boolean isStopped() {
      return stopped_;
    }

    /**
     * The work loop. Handles both selecting (all IO operations) and managing
     * the selection preferences of all existing connections.
     */
    @Override
    public void run() {
      try {
        if (eventHandler_ != null) {
          eventHandler_.preServe();
        }

        while (!stopped_) {
          select();
          processInterestChanges();
        }
        for (SelectionKey selectionKey : selector.keys()) {
          cleanupSelectionKey(selectionKey);
        }
      } catch (Throwable t) {
        LOGGER.error("run() exiting due to uncaught error", t);
      } finally {
        stopped_ = true;
      }
    }

    /**
     * Select and process IO events appropriately:
     * If there are connections to be accepted, accept them.
     * If there are existing connections with data waiting to be read, read it,
     * buffering until a whole frame has been read.
     * If there are any pending responses, buffer them until their target client
     * is available, and then send the data.
     */
    private void select() {
      try {
        // wait for io events.
        selector.select();

        // process the io events we received
        Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
        while (!stopped_ && selectedKeys.hasNext()) {
          SelectionKey key = selectedKeys.next();
          selectedKeys.remove();

          // skip if not valid
          if (!key.isValid()) {
            cleanupSelectionKey(key);
            continue;
          }

          // if the key is marked Accept, then it has to be the server
          // transport.
          if (key.isAcceptable()) {
            handleAccept();
          } else if (key.isReadable()) {
            // deal with reads
            handleRead(key);
          } else if (key.isWritable()) {
            // deal with writes
            handleWrite(key);
          } else {
            LOGGER.warn("Unexpected state in select! " + key.interestOps());
          }
        }
      } catch (IOException e) {
        LOGGER.warn("Got an IOException while selecting!", e);
      }
    }

    /**
     * Accept a new connection.
     */
    @SuppressWarnings("unused")
    private void handleAccept() throws IOException {
      SelectionKey clientKey = null;
      TNonblockingTransport client = null;
      try {
        // accept the connection
        client = (TNonblockingTransport)serverTransport.accept();
        clientKey = client.registerSelector(selector, SelectionKey.OP_READ);

        // add this key to the map
          FrameBuffer frameBuffer = processorFactory_.isAsyncProcessor() ?
                  new CustomAsyncFrameBuffer(client, clientKey,SelectAcceptThread.this) :
                  new CustomFrameBuffer(client, clientKey,SelectAcceptThread.this);

          clientKey.attach(frameBuffer);
      } catch (TTransportException tte) {
        // something went wrong accepting.
        LOGGER.warn("Exception trying to accept!", tte);
        tte.printStackTrace();
        if (clientKey != null) cleanupSelectionKey(clientKey);
        if (client != null) client.close();
      }
    }
  } // SelectAcceptThread
  // @formatter:on
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/TServerUtils.java b/server/base/src/main/java/org/apache/accumulo/server/util/TServerUtils.java
index 36487d6b8..b2d69af4d 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/TServerUtils.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/TServerUtils.java
@@ -22,7 +22,6 @@ import java.net.BindException;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.net.ServerSocket;
import java.net.Socket;
 import java.net.UnknownHostException;
 import java.nio.channels.ServerSocketChannel;
 import java.util.Random;
@@ -47,7 +46,6 @@ import org.apache.thrift.TProcessorFactory;
 import org.apache.thrift.protocol.TProtocol;
 import org.apache.thrift.server.TServer;
 import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TNonblockingSocket;
 import org.apache.thrift.transport.TServerTransport;
 import org.apache.thrift.transport.TSocket;
 import org.apache.thrift.transport.TTransport;
@@ -198,40 +196,11 @@ public class TServerUtils {
     }
   }
 
  public static class THsHaServer extends org.apache.thrift.server.THsHaServer {
    public THsHaServer(Args args) {
      super(args);
    }

    @Override
    protected Runnable getRunnable(FrameBuffer frameBuffer) {
      return new Invocation(frameBuffer);
    }

    private class Invocation implements Runnable {

      private final FrameBuffer frameBuffer;

      public Invocation(final FrameBuffer frameBuffer) {
        this.frameBuffer = frameBuffer;
      }

      @Override
      public void run() {
        if (frameBuffer.trans_ instanceof TNonblockingSocket) {
          TNonblockingSocket tsock = (TNonblockingSocket) frameBuffer.trans_;
          Socket sock = tsock.getSocketChannel().socket();
          clientAddress.set(sock.getInetAddress().getHostAddress() + ":" + sock.getPort());
        }
        frameBuffer.invoke();
      }
    }
  }

  public static ServerAddress createHsHaServer(HostAndPort address, TProcessor processor, final String serverName, String threadName, final int numThreads,
  public static ServerAddress createNonBlockingServer(HostAndPort address, TProcessor processor, final String serverName, String threadName,
      final int numThreads,
       long timeBetweenThreadChecks, long maxMessageSize) throws TTransportException {
     TNonblockingServerSocket transport = new TNonblockingServerSocket(new InetSocketAddress(address.getHostText(), address.getPort()));
    THsHaServer.Args options = new THsHaServer.Args(transport);
    CustomNonBlockingServer.Args options = new CustomNonBlockingServer.Args(transport);
     options.protocolFactory(ThriftUtil.protocolFactory());
     options.transportFactory(ThriftUtil.transportFactory(maxMessageSize));
     options.maxReadBufferBytes = maxMessageSize;
@@ -269,7 +238,7 @@ public class TServerUtils {
     if (address.getPort() == 0) {
       address = HostAndPort.fromParts(address.getHostText(), transport.getPort());
     }
    return new ServerAddress(new THsHaServer(options), address);
    return new ServerAddress(new CustomNonBlockingServer(options), address);
   }
 
   public static ServerAddress createThreadPoolServer(HostAndPort address, TProcessor processor, String serverName, String threadName, int numThreads)
@@ -324,7 +293,7 @@ public class TServerUtils {
     if (sslParams != null) {
       serverAddress = createSslThreadPoolServer(address, processor, sslSocketTimeout, sslParams);
     } else {
      serverAddress = createHsHaServer(address, processor, serverName, threadName, numThreads, timeBetweenThreadChecks, maxMessageSize);
      serverAddress = createNonBlockingServer(address, processor, serverName, threadName, numThreads, timeBetweenThreadChecks, maxMessageSize);
     }
     final TServer finalServer = serverAddress.server;
     Runnable serveTask = new Runnable() {
- 
2.19.1.windows.1

