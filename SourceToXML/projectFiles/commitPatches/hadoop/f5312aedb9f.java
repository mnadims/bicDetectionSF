From f5312aedb9fa3dc895d61844b5c3202b02554f80 Mon Sep 17 00:00:00 2001
From: Daryn Sharp <daryn@apache.org>
Date: Mon, 5 Aug 2013 22:02:40 +0000
Subject: [PATCH] HADOOP-9816. RPC Sasl QOP is broken (daryn)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1510772 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  2 +
 .../java/org/apache/hadoop/ipc/Client.java    | 11 ++++
 .../java/org/apache/hadoop/ipc/Server.java    | 22 ++++---
 .../apache/hadoop/security/SaslRpcClient.java |  7 ++
 .../org/apache/hadoop/ipc/TestSaslRPC.java    | 64 ++++++++++++++-----
 5 files changed, 79 insertions(+), 27 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 8302f0df2d1..f28cfd00e13 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -686,6 +686,8 @@ Release 2.1.0-beta - 2013-08-06
     HADOOP-9507. LocalFileSystem rename() is broken in some cases when
     destination exists. (cnauroth)
 
    HADOOP-9816. RPC Sasl QOP is broken (daryn)

   BREAKDOWN OF HADOOP-8562 SUBTASKS AND RELATED JIRAS
 
     HADOOP-8924. Hadoop Common creating package-info.java must not depend on
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
index fe987bfebd4..c1ac20e9067 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
@@ -52,6 +52,7 @@
 import java.util.concurrent.atomic.AtomicLong;
 
 import javax.net.SocketFactory;
import javax.security.sasl.Sasl;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
@@ -87,6 +88,7 @@
 import org.apache.hadoop.util.StringUtils;
 import org.apache.hadoop.util.Time;
 
import com.google.common.annotations.VisibleForTesting;
 import com.google.common.base.Preconditions;
 import com.google.common.util.concurrent.ThreadFactoryBuilder;
 import com.google.protobuf.CodedOutputStream;
@@ -711,6 +713,9 @@ public AuthMethod run()
               // Sasl connect is successful. Let's set up Sasl i/o streams.
               inStream = saslRpcClient.getInputStream(inStream);
               outStream = saslRpcClient.getOutputStream(outStream);
              // for testing
              remoteId.saslQop =
                  (String)saslRpcClient.getNegotiatedProperty(Sasl.QOP);
             } else if (UserGroupInformation.isSecurityEnabled() &&
                        !fallbackAllowed) {
               throw new IOException("Server asks us to fall back to SIMPLE " +
@@ -1455,6 +1460,7 @@ private Connection getConnection(ConnectionId remoteId,
     private final boolean tcpNoDelay; // if T then disable Nagle's Algorithm
     private final boolean doPing; //do we need to send ping message
     private final int pingInterval; // how often sends ping to the server in msecs
    private String saslQop; // here for testing
     
     ConnectionId(InetSocketAddress address, Class<?> protocol, 
                  UserGroupInformation ticket, int rpcTimeout, int maxIdleTime, 
@@ -1509,6 +1515,11 @@ int getPingInterval() {
       return pingInterval;
     }
     
    @VisibleForTesting
    String getSaslQop() {
      return saslQop;
    }
    
     static ConnectionId getConnectionId(InetSocketAddress addr,
         Class<?> protocol, UserGroupInformation ticket, int rpcTimeout,
         Configuration conf) throws IOException {
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Server.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Server.java
index 60fecddb68a..5303655c655 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Server.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Server.java
@@ -1276,8 +1276,8 @@ private UserGroupInformation getAuthorizedUgi(String authorizedId)
       }
     }
 
    private RpcSaslProto saslReadAndProcess(DataInputStream dis) throws
        WrappedRpcServerException, InterruptedException {
    private void saslReadAndProcess(DataInputStream dis) throws
        WrappedRpcServerException, IOException, InterruptedException {
       if (saslContextEstablished) {
         throw new WrappedRpcServerException(
             RpcErrorCodeProto.FATAL_INVALID_RPC_HEADER,
@@ -1310,8 +1310,6 @@ private RpcSaslProto saslReadAndProcess(DataInputStream dis) throws
             LOG.debug("SASL server context established. Negotiated QoP is "
                 + saslServer.getNegotiatedProperty(Sasl.QOP));
           }
          String qop = (String) saslServer.getNegotiatedProperty(Sasl.QOP);
          useWrap = qop != null && !"auth".equalsIgnoreCase(qop);
           user = getAuthorizedUgi(saslServer.getAuthorizationID());
           if (LOG.isDebugEnabled()) {
             LOG.debug("SASL server successfully authenticated client: " + user);
@@ -1326,7 +1324,15 @@ private RpcSaslProto saslReadAndProcess(DataInputStream dis) throws
         throw new WrappedRpcServerException(
             RpcErrorCodeProto.FATAL_UNAUTHORIZED, ioe);
       }
      return saslResponse; 
      // send back response if any, may throw IOException
      if (saslResponse != null) {
        doSaslReply(saslResponse);
      }
      // do NOT enable wrapping until the last auth response is sent
      if (saslContextEstablished) {
        String qop = (String) saslServer.getNegotiatedProperty(Sasl.QOP);
        useWrap = (qop != null && !"auth".equalsIgnoreCase(qop));        
      }
     }
     
     private RpcSaslProto processSaslMessage(DataInputStream dis)
@@ -1906,11 +1912,7 @@ private void processRpcOutOfBandRequest(RpcRequestHeaderProto header,
               RpcErrorCodeProto.FATAL_INVALID_RPC_HEADER,
               "SASL protocol not requested by client");
         }
        RpcSaslProto response = saslReadAndProcess(dis);
        // send back response if any, may throw IOException
        if (response != null) {
          doSaslReply(response);
        }
        saslReadAndProcess(dis);
       } else {
         throw new WrappedRpcServerException(
             RpcErrorCodeProto.FATAL_INVALID_RPC_HEADER,
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java
index fe6afd23901..a6fcd97d726 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java
@@ -67,6 +67,7 @@
 import org.apache.hadoop.security.token.TokenSelector;
 import org.apache.hadoop.util.ProtoUtil;
 
import com.google.common.annotations.VisibleForTesting;
 import com.google.protobuf.ByteString;
 /**
  * A utility class that encapsulates SASL logic for RPC client
@@ -106,6 +107,12 @@ public SaslRpcClient(UserGroupInformation ugi, Class<?> protocol,
     this.conf = conf;
   }
   
  @VisibleForTesting
  @InterfaceAudience.Private
  public Object getNegotiatedProperty(String key) {
    return (saslClient != null) ? saslClient.getNegotiatedProperty(key) : null;
  }
  
   /**
    * Instantiate a sasl client for the first supported auth type in the
    * given list.  The auth type must be defined, enabled, and the user
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java
index 138e12f8518..02c3e2a561a 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java
@@ -29,6 +29,7 @@
 import java.net.InetSocketAddress;
 import java.security.PrivilegedExceptionAction;
 import java.security.Security;
import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Set;
 import java.util.regex.Pattern;
@@ -44,8 +45,6 @@
 import javax.security.sasl.SaslException;
 import javax.security.sasl.SaslServer;
 
import junit.framework.Assert;

 import org.apache.commons.lang.StringUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
@@ -62,11 +61,11 @@
 import org.apache.hadoop.security.SaslRpcClient;
 import org.apache.hadoop.security.SaslRpcServer;
 import org.apache.hadoop.security.SaslRpcServer.AuthMethod;
import org.apache.hadoop.security.SaslRpcServer.QualityOfProtection;
 import org.apache.hadoop.security.SecurityInfo;
 import org.apache.hadoop.security.SecurityUtil;
 import org.apache.hadoop.security.TestUserGroupInformation;
 import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;
 import org.apache.hadoop.security.token.SecretManager;
 import org.apache.hadoop.security.token.SecretManager.InvalidToken;
 import org.apache.hadoop.security.token.Token;
@@ -77,9 +76,28 @@
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
 
 /** Unit tests for using Sasl over RPC. */
@RunWith(Parameterized.class)
 public class TestSaslRPC {
  @Parameters
  public static Collection<Object[]> data() {
    Collection<Object[]> params = new ArrayList<Object[]>();
    for (QualityOfProtection qop : QualityOfProtection.values()) {
      params.add(new Object[]{ qop });
    }
    return params;
  }

  QualityOfProtection expectedQop;
  
  public TestSaslRPC(QualityOfProtection qop) {
    expectedQop = qop;
  }
  
   private static final String ADDRESS = "0.0.0.0";
 
   public static final Log LOG =
@@ -115,8 +133,12 @@ public static void setupKerb() {
 
   @Before
   public void setup() {
    LOG.info("---------------------------------");
    LOG.info("Testing QOP:"+expectedQop);
    LOG.info("---------------------------------");
     conf = new Configuration();
     conf.set(HADOOP_SECURITY_AUTHENTICATION, KERBEROS.toString());
    conf.set("hadoop.rpc.protection", expectedQop.name().toLowerCase());
     UserGroupInformation.setConfiguration(conf);
     enableSecretManager = null;
     forceSecretManager = null;
@@ -226,15 +248,16 @@ public TestTokenIdentifier createIdentifier() {
       serverPrincipal = SERVER_PRINCIPAL_KEY)
   @TokenInfo(TestTokenSelector.class)
   public interface TestSaslProtocol extends TestRPC.TestProtocol {
    public AuthenticationMethod getAuthMethod() throws IOException;
    public AuthMethod getAuthMethod() throws IOException;
     public String getAuthUser() throws IOException;
   }
   
   public static class TestSaslImpl extends TestRPC.TestImpl implements
       TestSaslProtocol {
     @Override
    public AuthenticationMethod getAuthMethod() throws IOException {
      return UserGroupInformation.getCurrentUser().getAuthenticationMethod();
    public AuthMethod getAuthMethod() throws IOException {
      return UserGroupInformation.getCurrentUser()
          .getAuthenticationMethod().getAuthMethod();
     }
     @Override
     public String getAuthUser() throws IOException {
@@ -341,8 +364,11 @@ private void doDigestRpc(Server server, TestTokenSecretManager sm
     try {
       proxy = RPC.getProxy(TestSaslProtocol.class,
           TestSaslProtocol.versionID, addr, conf);
      AuthMethod authMethod = proxy.getAuthMethod();
      assertEquals(TOKEN, authMethod);
       //QOP must be auth
      Assert.assertEquals(SaslRpcServer.SASL_PROPS.get(Sasl.QOP), "auth");
      assertEquals(expectedQop.saslQop,
                   RPC.getConnectionIdForProxy(proxy).getSaslQop());            
       proxy.ping();
     } finally {
       server.stop();
@@ -393,6 +419,7 @@ public void testPerConnectionConf() throws Exception {
     newConf.set(CommonConfigurationKeysPublic.
         HADOOP_RPC_SOCKET_FACTORY_CLASS_DEFAULT_KEY, "");
 
    Client client = null;
     TestSaslProtocol proxy1 = null;
     TestSaslProtocol proxy2 = null;
     TestSaslProtocol proxy3 = null;
@@ -402,7 +429,7 @@ public void testPerConnectionConf() throws Exception {
       proxy1 = RPC.getProxy(TestSaslProtocol.class,
           TestSaslProtocol.versionID, addr, newConf);
       proxy1.getAuthMethod();
      Client client = WritableRpcEngine.getClient(conf);
      client = WritableRpcEngine.getClient(newConf);
       Set<ConnectionId> conns = client.getConnectionIds();
       assertEquals("number of connections in cache is wrong", 1, conns.size());
       // same conf, connection should be re-used
@@ -428,9 +455,13 @@ public void testPerConnectionConf() throws Exception {
       assertNotSame(connsArray[2].getMaxIdleTime(), timeouts[1]);
     } finally {
       server.stop();
      RPC.stopProxy(proxy1);
      RPC.stopProxy(proxy2);
      RPC.stopProxy(proxy3);
      // this is dirty, but clear out connection cache for next run
      if (client != null) {
        client.getConnectionIds().clear();
      }
      if (proxy1 != null) RPC.stopProxy(proxy1);
      if (proxy2 != null) RPC.stopProxy(proxy2);
      if (proxy3 != null) RPC.stopProxy(proxy3);
     }
   }
   
@@ -873,14 +904,13 @@ public String run() throws IOException {
                 TestSaslProtocol.versionID, addr, clientConf);
             
             proxy.ping();
            // verify sasl completed
            if (serverAuth != SIMPLE) {
              assertEquals(SaslRpcServer.SASL_PROPS.get(Sasl.QOP), "auth");
            }
            
             // make sure the other side thinks we are who we said we are!!!
             assertEquals(clientUgi.getUserName(), proxy.getAuthUser());
            return proxy.getAuthMethod().toString();
            AuthMethod authMethod = proxy.getAuthMethod();
            // verify sasl completed with correct QOP
            assertEquals((authMethod != SIMPLE) ? expectedQop.saslQop : null,
                         RPC.getConnectionIdForProxy(proxy).getSaslQop());            
            return authMethod.toString();
           } finally {
             if (proxy != null) {
               RPC.stopProxy(proxy);
- 
2.19.1.windows.1

