From fb6b38d67d8b997eca498fc5010b037e3081ace7 Mon Sep 17 00:00:00 2001
From: Haohui Mai <wheat9@apache.org>
Date: Wed, 20 May 2015 20:10:50 -0700
Subject: [PATCH] HADOOP-11772. RPC Invoker relies on static ClientCache which
 has synchronized(this) blocks. Contributed by Haohui Mai.

--
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../java/org/apache/hadoop/ipc/Client.java    | 106 ++++++------------
 2 files changed, 35 insertions(+), 74 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 1624ce2bc35..416b81914be 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -604,6 +604,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-11970. Replace uses of ThreadLocal<Random> with JDK7
     ThreadLocalRandom.  (Sean Busbey via Colin P. McCabe)
 
    HADOOP-11772. RPC Invoker relies on static ClientCache which has
    synchronized(this) blocks. (wheat9)

   BUG FIXES
     HADOOP-11802: DomainSocketWatcher thread terminates sometimes after there
     is an I/O error during requestShortCircuitShm (cmccabe)
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
index f28d8a290ce..feb811ed3ae 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
@@ -43,6 +43,7 @@
 import java.util.Map.Entry;
 import java.util.Random;
 import java.util.Set;
import java.util.concurrent.Callable;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
@@ -56,6 +57,8 @@
 import javax.net.SocketFactory;
 import javax.security.sasl.Sasl;
 
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.classification.InterfaceAudience;
@@ -124,8 +127,8 @@ public static void setCallIdAndRetryCount(int cid, int rc) {
     retryCount.set(rc);
   }
 
  private Hashtable<ConnectionId, Connection> connections =
    new Hashtable<ConnectionId, Connection>();
  private final Cache<ConnectionId, Connection> connections =
      CacheBuilder.newBuilder().build();
 
   private Class<? extends Writable> valueClass;   // class of call values
   private AtomicBoolean running = new AtomicBoolean(true); // if client runs
@@ -1167,13 +1170,7 @@ private synchronized void close() {
         return;
       }
 
      // release the resources
      // first thing to do;take the connection out of the connection list
      synchronized (connections) {
        if (connections.get(remoteId) == this) {
          connections.remove(remoteId);
        }
      }
      connections.invalidate(remoteId);
 
       // close the streams and therefore the socket
       IOUtils.closeStream(out);
@@ -1260,14 +1257,12 @@ public void stop() {
     }
     
     // wake up all connections
    synchronized (connections) {
      for (Connection conn : connections.values()) {
        conn.interrupt();
      }
    for (Connection conn : connections.asMap().values()) {
      conn.interrupt();
     }
     
     // wait until all connections are closed
    while (!connections.isEmpty()) {
    while (connections.size() > 0) {
       try {
         Thread.sleep(100);
       } catch (InterruptedException e) {
@@ -1283,56 +1278,12 @@ public void stop() {
    */
   public Writable call(Writable param, InetSocketAddress address)
       throws IOException {
    return call(RPC.RpcKind.RPC_BUILTIN, param, address);
    
  }
  /** Make a call, passing <code>param</code>, to the IPC server running at
   * <code>address</code>, returning the value.  Throws exceptions if there are
   * network problems or if the remote code threw an exception.
   * @deprecated Use {@link #call(RPC.RpcKind, Writable,
   *  ConnectionId)} instead 
   */
  @Deprecated
  public Writable call(RPC.RpcKind rpcKind, Writable param, InetSocketAddress address)
  throws IOException {
      return call(rpcKind, param, address, null);
  }
  
  /** Make a call, passing <code>param</code>, to the IPC server running at
   * <code>address</code> with the <code>ticket</code> credentials, returning 
   * the value.  
   * Throws exceptions if there are network problems or if the remote code 
   * threw an exception.
   * @deprecated Use {@link #call(RPC.RpcKind, Writable, 
   * ConnectionId)} instead 
   */
  @Deprecated
  public Writable call(RPC.RpcKind rpcKind, Writable param, InetSocketAddress addr, 
      UserGroupInformation ticket) throws IOException {
    ConnectionId remoteId = ConnectionId.getConnectionId(addr, null, ticket, 0,
    ConnectionId remoteId = ConnectionId.getConnectionId(address, null, null, 0,
         conf);
    return call(rpcKind, param, remoteId);
  }
  
  /** Make a call, passing <code>param</code>, to the IPC server running at
   * <code>address</code> which is servicing the <code>protocol</code> protocol, 
   * with the <code>ticket</code> credentials and <code>rpcTimeout</code> as 
   * timeout, returning the value.  
   * Throws exceptions if there are network problems or if the remote code 
   * threw an exception. 
   * @deprecated Use {@link #call(RPC.RpcKind, Writable,
   *  ConnectionId)} instead 
   */
  @Deprecated
  public Writable call(RPC.RpcKind rpcKind, Writable param, InetSocketAddress addr, 
                       Class<?> protocol, UserGroupInformation ticket,
                       int rpcTimeout) throws IOException {
    ConnectionId remoteId = ConnectionId.getConnectionId(addr, protocol,
        ticket, rpcTimeout, conf);
    return call(rpcKind, param, remoteId);
    return call(RpcKind.RPC_BUILTIN, param, remoteId);

   }
 
  
   /**
    * Same as {@link #call(RPC.RpcKind, Writable, InetSocketAddress,
    * Class, UserGroupInformation, int, Configuration)}
@@ -1506,15 +1457,14 @@ public Writable call(RPC.RpcKind rpcKind, Writable rpcRequest,
   @InterfaceAudience.Private
   @InterfaceStability.Unstable
   Set<ConnectionId> getConnectionIds() {
    synchronized (connections) {
      return connections.keySet();
    }
    return connections.asMap().keySet();
   }
   
   /** Get a connection from the pool, or create a new one and add it to the
    * pool.  Connections to a given ConnectionId are reused. */
  private Connection getConnection(ConnectionId remoteId,
      Call call, int serviceClass, AtomicBoolean fallbackToSimpleAuth)
  private Connection getConnection(
      final ConnectionId remoteId,
      Call call, final int serviceClass, AtomicBoolean fallbackToSimpleAuth)
       throws IOException {
     if (!running.get()) {
       // the client is stopped
@@ -1525,15 +1475,23 @@ private Connection getConnection(ConnectionId remoteId,
      * connectionsId object and with set() method. We need to manage the
      * refs for keys in HashMap properly. For now its ok.
      */
    do {
      synchronized (connections) {
        connection = connections.get(remoteId);
        if (connection == null) {
          connection = new Connection(remoteId, serviceClass);
          connections.put(remoteId, connection);
        }
    while(true) {
      try {
        connection = connections.get(remoteId, new Callable<Connection>() {
          @Override
          public Connection call() throws Exception {
            return new Connection(remoteId, serviceClass);
          }
        });
      } catch (ExecutionException e) {
        throw new IOException(e);
      }
      if (connection.addCall(call)) {
        break;
      } else {
        connections.invalidate(remoteId);
       }
    } while (!connection.addCall(call));
    }
     
     //we don't invoke the method below inside "synchronized (connections)"
     //block above. The reason for that is if the server happens to be slow,
- 
2.19.1.windows.1

