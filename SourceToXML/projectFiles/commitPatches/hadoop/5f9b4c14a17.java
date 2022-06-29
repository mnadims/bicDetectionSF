From 5f9b4c14a175873b4f82654513e289c657c694eb Mon Sep 17 00:00:00 2001
From: Daryn Sharp <daryn@apache.org>
Date: Fri, 21 Jun 2013 20:09:31 +0000
Subject: [PATCH] HADOOP-9421. [RPC v9] Convert SASL to use ProtoBuf and
 provide negotiation capabilities (daryn)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1495577 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../dev-support/findbugsExcludeFile.xml       |   9 +
 .../java/org/apache/hadoop/ipc/Client.java    |  13 +-
 .../apache/hadoop/ipc/ProtobufRpcEngine.java  | 120 +++++-
 .../java/org/apache/hadoop/ipc/Server.java    | 383 +++++++++++-------
 .../apache/hadoop/security/SaslRpcClient.java | 210 +++++++---
 .../apache/hadoop/security/SaslRpcServer.java | 106 ++++-
 .../hadoop/security/UserGroupInformation.java |   2 +-
 .../org/apache/hadoop/util/ProtoUtil.java     |   2 +-
 .../src/main/proto/RpcHeader.proto            |  23 ++
 .../org/apache/hadoop/ipc/TestSaslRPC.java    |   4 +-
 11 files changed, 625 insertions(+), 250 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 931affe09b2..29ee5e96b62 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -319,6 +319,9 @@ Release 2.1.0-beta - UNRELEASED
 
     HADOOP-9630. [RPC v9] Remove IpcSerializationType. (Junping Du via llu)
 
    HADOOP-9421. [RPC v9] Convert SASL to use ProtoBuf and provide
    negotiation capabilities (daryn)

   NEW FEATURES
 
     HADOOP-9283. Add support for running the Hadoop client on AIX. (atm)
diff --git a/hadoop-common-project/hadoop-common/dev-support/findbugsExcludeFile.xml b/hadoop-common-project/hadoop-common/dev-support/findbugsExcludeFile.xml
index 82b2963c70a..8bd26db1806 100644
-- a/hadoop-common-project/hadoop-common/dev-support/findbugsExcludeFile.xml
++ b/hadoop-common-project/hadoop-common/dev-support/findbugsExcludeFile.xml
@@ -320,6 +320,15 @@
        <Field name="in" />
        <Bug pattern="IS2_INCONSISTENT_SYNC" />
      </Match>
     <!-- 
       The switch condition for INITIATE is expected to fallthru to RESPONSE
       to process initial sasl response token included in the INITIATE
     -->
     <Match>
       <Class name="org.apache.hadoop.ipc.Server$Connection" />
       <Method name="processSaslMessage" />
       <Bug pattern="SF_SWITCH_FALLTHROUGH" />
     </Match>
 
      <!-- Synchronization performed on util.concurrent instance. -->
      <Match>
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
index 672be8dd4c7..3782eef61ac 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
@@ -62,6 +62,7 @@
 import org.apache.hadoop.io.retry.RetryPolicies;
 import org.apache.hadoop.io.retry.RetryPolicy;
 import org.apache.hadoop.io.retry.RetryPolicy.RetryAction;
import org.apache.hadoop.ipc.Server.AuthProtocol;
 import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcRequestHeaderProto;
 import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcRequestHeaderProto.OperationProto;
 import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto;
@@ -751,7 +752,7 @@ private void handleConnectionFailure(int curRetries, IOException ioe
      * +----------------------------------+
      * |  Service Class (1 byte)          |
      * +----------------------------------+
     * |  Authmethod (1 byte)             |      
     * |  AuthProtocol (1 byte)           |      
      * +----------------------------------+
      */
     private void writeConnectionHeader(OutputStream outStream)
@@ -761,7 +762,15 @@ private void writeConnectionHeader(OutputStream outStream)
       out.write(Server.HEADER.array());
       out.write(Server.CURRENT_VERSION);
       out.write(serviceClass);
      authMethod.write(out);
      final AuthProtocol authProtocol;
      switch (authMethod) {
        case SIMPLE:
          authProtocol = AuthProtocol.NONE;
          break;
        default:
          authProtocol = AuthProtocol.SASL;
      }
      out.write(authProtocol.callId);
       out.flush();
     }
     
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/ProtobufRpcEngine.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/ProtobufRpcEngine.java
index b880828d06d..5be96270a89 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/ProtobufRpcEngine.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/ProtobufRpcEngine.java
@@ -41,6 +41,8 @@
 import org.apache.hadoop.ipc.Client.ConnectionId;
 import org.apache.hadoop.ipc.RPC.RpcInvoker;
 import org.apache.hadoop.ipc.protobuf.ProtobufRpcEngineProtos.RequestHeaderProto;
import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcRequestHeaderProto;
import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto;
 import org.apache.hadoop.security.UserGroupInformation;
 import org.apache.hadoop.security.token.SecretManager;
 import org.apache.hadoop.security.token.TokenIdentifier;
@@ -48,10 +50,10 @@
 import org.apache.hadoop.util.Time;
 
 import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.AbstractMessageLite;
 import com.google.protobuf.BlockingService;
 import com.google.protobuf.CodedOutputStream;
 import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.GeneratedMessage;
 import com.google.protobuf.Message;
 import com.google.protobuf.ServiceException;
 import com.google.protobuf.TextFormat;
@@ -279,16 +281,16 @@ public ConnectionId getConnectionId() {
    * Protobuf. Several methods on {@link org.apache.hadoop.ipc.Server and RPC} 
    * use type Writable as a wrapper to work across multiple RpcEngine kinds.
    */
  private static class RpcRequestWrapper implements RpcWrapper {
    RequestHeaderProto requestHeader;
  private static abstract class RpcMessageWithHeader<T extends GeneratedMessage>
    implements RpcWrapper {
    T requestHeader;
     Message theRequest; // for clientSide, the request is here
     byte[] theRequestRead; // for server side, the request is here
 
    @SuppressWarnings("unused")
    public RpcRequestWrapper() {
    public RpcMessageWithHeader() {
     }
 
    RpcRequestWrapper(RequestHeaderProto requestHeader, Message theRequest) {
    public RpcMessageWithHeader(T requestHeader, Message theRequest) {
       this.requestHeader = requestHeader;
       this.theRequest = theRequest;
     }
@@ -303,21 +305,31 @@ public void write(DataOutput out) throws IOException {
 
     @Override
     public void readFields(DataInput in) throws IOException {
      int length = ProtoUtil.readRawVarint32(in);
      byte[] bytes = new byte[length];
      requestHeader = parseHeaderFrom(readVarintBytes(in));
      theRequestRead = readMessageRequest(in);
    }

    abstract T parseHeaderFrom(byte[] bytes) throws IOException;

    byte[] readMessageRequest(DataInput in) throws IOException {
      return readVarintBytes(in);
    }

    private static byte[] readVarintBytes(DataInput in) throws IOException {
      final int length = ProtoUtil.readRawVarint32(in);
      final byte[] bytes = new byte[length];
       in.readFully(bytes);
      requestHeader = RequestHeaderProto.parseFrom(bytes);
      length = ProtoUtil.readRawVarint32(in);
      theRequestRead = new byte[length];
      in.readFully(theRequestRead);
      return bytes;
     }
    
    @Override
    public String toString() {
      return requestHeader.getDeclaringClassProtocolName() + "." +
          requestHeader.getMethodName();

    public T getMessageHeader() {
      return requestHeader;
     }
 
    public byte[] getMessageBytes() {
      return theRequestRead;
    }
    
     @Override
     public int getLength() {
       int headerLen = requestHeader.getSerializedSize();
@@ -328,12 +340,78 @@ public int getLength() {
         reqLen = theRequestRead.length;
       } else {
         throw new IllegalArgumentException(
            "getLenght on uninilialized RpcWrapper");      
            "getLength on uninitialized RpcWrapper");      
       }
       return CodedOutputStream.computeRawVarint32Size(headerLen) +  headerLen
           + CodedOutputStream.computeRawVarint32Size(reqLen) + reqLen;
     }
   }
  
  private static class RpcRequestWrapper
  extends RpcMessageWithHeader<RequestHeaderProto> {
    @SuppressWarnings("unused")
    public RpcRequestWrapper() {}
    
    public RpcRequestWrapper(
        RequestHeaderProto requestHeader, Message theRequest) {
      super(requestHeader, theRequest);
    }
    
    @Override
    RequestHeaderProto parseHeaderFrom(byte[] bytes) throws IOException {
      return RequestHeaderProto.parseFrom(bytes);
    }
    
    @Override
    public String toString() {
      return requestHeader.getDeclaringClassProtocolName() + "." +
          requestHeader.getMethodName();
    }
  }

  @InterfaceAudience.LimitedPrivate({"RPC"})
  public static class RpcRequestMessageWrapper
  extends RpcMessageWithHeader<RpcRequestHeaderProto> {
    public RpcRequestMessageWrapper() {}
    
    public RpcRequestMessageWrapper(
        RpcRequestHeaderProto requestHeader, Message theRequest) {
      super(requestHeader, theRequest);
    }
    
    @Override
    RpcRequestHeaderProto parseHeaderFrom(byte[] bytes) throws IOException {
      return RpcRequestHeaderProto.parseFrom(bytes);
    }
  }

  @InterfaceAudience.LimitedPrivate({"RPC"})
  public static class RpcResponseMessageWrapper
  extends RpcMessageWithHeader<RpcResponseHeaderProto> {
    public RpcResponseMessageWrapper() {}
    
    public RpcResponseMessageWrapper(
        RpcResponseHeaderProto responseHeader, Message theRequest) {
      super(responseHeader, theRequest);
    }
    
    @Override
    byte[] readMessageRequest(DataInput in) throws IOException {
      // error message contain no message body
      switch (requestHeader.getStatus()) {
        case ERROR:
        case FATAL:
          return null;
        default:
          return super.readMessageRequest(in);
      }
    }
    
    @Override
    RpcResponseHeaderProto parseHeaderFrom(byte[] bytes) throws IOException {
      return RpcResponseHeaderProto.parseFrom(bytes);
    }
  }
 
   /**
    *  Wrapper for Protocol Buffer Responses
@@ -342,11 +420,11 @@ public int getLength() {
    * Protobuf. Several methods on {@link org.apache.hadoop.ipc.Server and RPC} 
    * use type Writable as a wrapper to work across multiple RpcEngine kinds.
    */
  private static class RpcResponseWrapper implements RpcWrapper {
  @InterfaceAudience.LimitedPrivate({"RPC"}) // temporarily exposed 
  public static class RpcResponseWrapper implements RpcWrapper {
     Message theResponse; // for senderSide, the response is here
     byte[] theResponseRead; // for receiver side, the response is here
 
    @SuppressWarnings("unused")
     public RpcResponseWrapper() {
     }
 
@@ -376,7 +454,7 @@ public int getLength() {
         resLen = theResponseRead.length;
       } else {
         throw new IllegalArgumentException(
            "getLenght on uninilialized RpcWrapper");      
            "getLength on uninitialized RpcWrapper");      
       }
       return CodedOutputStream.computeRawVarint32Size(resLen) + resLen;
     }
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Server.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Server.java
index 5beac94243e..78b3f786b4b 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Server.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Server.java
@@ -21,7 +21,6 @@
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.DataInputStream;
import java.io.DataOutput;
 import java.io.DataOutputStream;
 import java.io.IOException;
 import java.lang.reflect.UndeclaredThrowableException;
@@ -46,7 +45,6 @@
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
import java.util.EnumSet;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
@@ -59,7 +57,6 @@
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.LinkedBlockingQueue;
 
import javax.security.auth.callback.CallbackHandler;
 import javax.security.sasl.Sasl;
 import javax.security.sasl.SaslException;
 import javax.security.sasl.SaslServer;
@@ -72,11 +69,11 @@
 import org.apache.hadoop.conf.Configuration.IntegerRanges;
 import org.apache.hadoop.fs.CommonConfigurationKeys;
 import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.io.BytesWritable;
 import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.IntWritable;
 import org.apache.hadoop.io.Writable;
 import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.ipc.ProtobufRpcEngine.RpcResponseWrapper;
import org.apache.hadoop.ipc.ProtobufRpcEngine.RpcRequestMessageWrapper;
 import org.apache.hadoop.ipc.RPC.RpcInvoker;
 import org.apache.hadoop.ipc.RPC.VersionMismatch;
 import org.apache.hadoop.ipc.metrics.RpcDetailedMetrics;
@@ -84,18 +81,15 @@
 import org.apache.hadoop.ipc.protobuf.IpcConnectionContextProtos.IpcConnectionContextProto;
 import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcStatusProto;
 import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcErrorCodeProto;
import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcSaslProto.*;
 import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.*;
 import org.apache.hadoop.net.NetUtils;
 import org.apache.hadoop.security.AccessControlException;
 import org.apache.hadoop.security.SaslRpcServer;
 import org.apache.hadoop.security.SaslRpcServer.AuthMethod;
import org.apache.hadoop.security.SaslRpcServer.SaslDigestCallbackHandler;
import org.apache.hadoop.security.SaslRpcServer.SaslGssCallbackHandler;
import org.apache.hadoop.security.SaslRpcServer.SaslStatus;
 import org.apache.hadoop.security.SecurityUtil;
 import org.apache.hadoop.security.UserGroupInformation;
 import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;
import org.apache.hadoop.security.authentication.util.KerberosName;
 import org.apache.hadoop.security.authorize.AuthorizationException;
 import org.apache.hadoop.security.authorize.PolicyProvider;
 import org.apache.hadoop.security.authorize.ProxyUsers;
@@ -109,7 +103,9 @@
 import org.apache.hadoop.util.Time;
 
 import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
 import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
 
 /** An abstract IPC service.  IPC calls take a single {@link Writable} as a
  * parameter, and return a {@link Writable} as their value.  A service runs on
@@ -121,7 +117,8 @@
 @InterfaceStability.Evolving
 public abstract class Server {
   private final boolean authorize;
  private EnumSet<AuthMethod> enabledAuthMethods;
  private List<AuthMethod> enabledAuthMethods;
  private RpcSaslProto negotiateResponse;
   private ExceptionsHandler exceptionsHandler = new ExceptionsHandler();
   
   public void addTerseExceptions(Class<?>... exceptionClass) {
@@ -1065,6 +1062,26 @@ private synchronized void waitPending() throws InterruptedException {
     }
   }
 
  @InterfaceAudience.Private
  public static enum AuthProtocol {
    NONE(0),
    SASL(-33);
    
    public final int callId;
    AuthProtocol(int callId) {
      this.callId = callId;
    }
    
    static AuthProtocol valueOf(int callId) {
      for (AuthProtocol authType : AuthProtocol.values()) {
        if (authType.callId == callId) {
          return authType;
        }
      }
      return null;
    }
  };
  
   /** Reads calls from a connection and queues them for handling. */
   public class Connection {
     private boolean connectionHeaderRead = false; // connection  header is read?
@@ -1089,6 +1106,7 @@ private synchronized void waitPending() throws InterruptedException {
     String protocolName;
     SaslServer saslServer;
     private AuthMethod authMethod;
    private AuthProtocol authProtocol;
     private boolean saslContextEstablished;
     private boolean skipInitialSaslHandshake;
     private ByteBuffer connectionHeaderBuf = null;
@@ -1104,12 +1122,11 @@ private synchronized void waitPending() throws InterruptedException {
     private final Call authFailedCall = 
       new Call(AUTHORIZATION_FAILED_CALLID, null, this);
     private ByteArrayOutputStream authFailedResponse = new ByteArrayOutputStream();
    // Fake 'call' for SASL context setup
    private static final int SASL_CALLID = -33;
     
    private final Call saslCall = new Call(SASL_CALLID, null, this);
    private final Call saslCall = new Call(AuthProtocol.SASL.callId, null, this);
     private final ByteArrayOutputStream saslResponse = new ByteArrayOutputStream();
     
    private boolean sentNegotiate = false;
     private boolean useWrap = false;
     
     public Connection(SelectionKey key, SocketChannel channel, 
@@ -1183,7 +1200,7 @@ private boolean timedOut(long currentTime) {
     
     private UserGroupInformation getAuthorizedUgi(String authorizedId)
         throws IOException {
      if (authMethod == SaslRpcServer.AuthMethod.DIGEST) {
      if (authMethod == AuthMethod.TOKEN) {
         TokenIdentifier tokenId = SaslRpcServer.getIdentifier(authorizedId,
             secretManager);
         UserGroupInformation ugi = tokenId.getUser();
@@ -1201,12 +1218,9 @@ private UserGroupInformation getAuthorizedUgi(String authorizedId)
     private void saslReadAndProcess(byte[] saslToken) throws IOException,
         InterruptedException {
       if (!saslContextEstablished) {
        byte[] replyToken = null;
        RpcSaslProto saslResponse;
         try {
          if (LOG.isDebugEnabled())
            LOG.debug("Have read input token of size " + saslToken.length
                + " for processing by saslServer.evaluateResponse()");
          replyToken = saslServer.evaluateResponse(saslToken);
          saslResponse = processSaslMessage(saslToken);
         } catch (IOException e) {
           IOException sendToClient = e;
           Throwable cause = e;
@@ -1217,27 +1231,17 @@ private void saslReadAndProcess(byte[] saslToken) throws IOException,
             }
             cause = cause.getCause();
           }
          doSaslReply(SaslStatus.ERROR, null, sendToClient.getClass().getName(), 
              sendToClient.getLocalizedMessage());
           rpcMetrics.incrAuthenticationFailures();
           String clientIP = this.toString();
           // attempting user could be null
           AUDITLOG.warn(AUTH_FAILED_FOR + clientIP + ":" + attemptingUser +
             " (" + e.getLocalizedMessage() + ")");
          // wait to send response until failure is logged
          doSaslReply(sendToClient);
           throw e;
         }
        if (saslServer.isComplete() && replyToken == null) {
          // send final response for success
          replyToken = new byte[0];
        }
        if (replyToken != null) {
          if (LOG.isDebugEnabled())
            LOG.debug("Will send token of size " + replyToken.length
                + " from saslServer.");
          doSaslReply(SaslStatus.SUCCESS, new BytesWritable(replyToken), null,
              null);
        }
        if (saslServer.isComplete()) {
        
        if (saslServer != null && saslServer.isComplete()) {
           if (LOG.isDebugEnabled()) {
             LOG.debug("SASL server context established. Negotiated QoP is "
                 + saslServer.getNegotiatedProperty(Sasl.QOP));
@@ -1252,6 +1256,9 @@ private void saslReadAndProcess(byte[] saslToken) throws IOException,
           AUDITLOG.info(AUTH_SUCCESSFUL_FOR + user);
           saslContextEstablished = true;
         }
        // send reply here to avoid a successful auth being logged as a
        // failure if response can't be sent
        doSaslReply(saslResponse);
       } else {
         if (LOG.isDebugEnabled())
           LOG.debug("Have read input token of size " + saslToken.length
@@ -1267,21 +1274,101 @@ private void saslReadAndProcess(byte[] saslToken) throws IOException,
       }
     }
     
    private void doSaslReply(SaslStatus status, Writable rv,
        String errorClass, String error) throws IOException {
      saslResponse.reset();
      DataOutputStream out = new DataOutputStream(saslResponse);
      out.writeInt(status.state); // write status
      if (status == SaslStatus.SUCCESS) {
        rv.write(out);
      } else {
        WritableUtils.writeString(out, errorClass);
        WritableUtils.writeString(out, error);
    private RpcSaslProto processSaslMessage(byte[] buf)
        throws IOException, InterruptedException {
      final DataInputStream dis =
          new DataInputStream(new ByteArrayInputStream(buf));
      RpcRequestMessageWrapper requestWrapper = new RpcRequestMessageWrapper();
      requestWrapper.readFields(dis);
      
      final RpcRequestHeaderProto rpcHeader = requestWrapper.requestHeader;
      if (rpcHeader.getCallId() != AuthProtocol.SASL.callId) {
        throw new SaslException("Client sent non-SASL request");
      }      
      final RpcSaslProto saslMessage =
          RpcSaslProto.parseFrom(requestWrapper.theRequestRead);
      RpcSaslProto saslResponse = null;
      final SaslState state = saslMessage.getState(); // required      
      switch (state) {
        case NEGOTIATE: {
          if (sentNegotiate) {
            throw new AccessControlException(
                "Client already attempted negotiation");
          }
          saslResponse = buildSaslNegotiateResponse();
          break;
        }
        case INITIATE: {
          if (saslMessage.getAuthsCount() != 1) {
            throw new SaslException("Client mechanism is malformed");
          }
          String authMethodName = saslMessage.getAuths(0).getMethod();
          authMethod = createSaslServer(authMethodName);
          if (authMethod == null) { // the auth method is not supported
            if (sentNegotiate) {
              throw new AccessControlException(
                  authMethodName + " authentication is not enabled."
                      + "  Available:" + enabledAuthMethods);
            }
            saslResponse = buildSaslNegotiateResponse();
            break;
          }
          // fallthru to process sasl token
        }
        case RESPONSE: {
          if (!saslMessage.hasToken()) {
            throw new SaslException("Client did not send a token");
          }
          byte[] saslToken = saslMessage.getToken().toByteArray();
          if (LOG.isDebugEnabled()) {
            LOG.debug("Have read input token of size " + saslToken.length
                + " for processing by saslServer.evaluateResponse()");
          }
          saslToken = saslServer.evaluateResponse(saslToken);
          saslResponse = buildSaslResponse(
              saslServer.isComplete() ? SaslState.SUCCESS : SaslState.CHALLENGE,
              saslToken);
          break;
        }
        default:
          throw new SaslException("Client sent unsupported state " + state);
      }
      return saslResponse;
    }
    
    private RpcSaslProto buildSaslResponse(SaslState state, byte[] replyToken)
        throws IOException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Will send " + state + " token of size "
            + ((replyToken != null) ? replyToken.length : null)
            + " from saslServer.");
      }
      RpcSaslProto.Builder response = RpcSaslProto.newBuilder();
      response.setState(state);
      if (replyToken != null) {
        response.setToken(ByteString.copyFrom(replyToken));
      }
      return response.build();
    }
    
    private void doSaslReply(Message message)
        throws IOException {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Sending sasl message "+message);
       }
      saslCall.setResponse(ByteBuffer.wrap(saslResponse.toByteArray()));
      setupResponse(saslResponse, saslCall,
          RpcStatusProto.SUCCESS, null,
          new RpcResponseWrapper(message), null, null);
       responder.doRespond(saslCall);
     }
     
    private void doSaslReply(Exception ioe) throws IOException {
      setupResponse(authFailedResponse, authFailedCall,
          RpcStatusProto.FATAL, RpcErrorCodeProto.FATAL_UNAUTHORIZED,
          null, ioe.getClass().getName(), ioe.getLocalizedMessage());
      responder.doRespond(authFailedCall);
    }
    
     private void disposeSasl() {
       if (saslServer != null) {
         try {
@@ -1315,10 +1402,6 @@ public int readAndProcess() throws IOException, InterruptedException {
           int version = connectionHeaderBuf.get(0);
           // TODO we should add handler for service class later
           this.setServiceClass(connectionHeaderBuf.get(1));

          byte[] method = new byte[] {connectionHeaderBuf.get(2)};
          authMethod = AuthMethod.read(new DataInputStream(
              new ByteArrayInputStream(method)));
           dataLengthBuffer.flip();
           
           // Check if it looks like the user is hitting an IPC port
@@ -1339,14 +1422,10 @@ public int readAndProcess() throws IOException, InterruptedException {
             return -1;
           }
           
          dataLengthBuffer.clear();
          if (authMethod == null) {
            throw new IOException("Unable to read authentication method");
          }
  
          // this may create a SASL server, or switch us into SIMPLE
          authMethod = initializeAuthContext(authMethod);
          // this may switch us into SIMPLE
          authProtocol = initializeAuthContext(connectionHeaderBuf.get(2));          
           
          dataLengthBuffer.clear();
           connectionHeaderBuf = null;
           connectionHeaderRead = true;
           continue;
@@ -1373,14 +1452,14 @@ public int readAndProcess() throws IOException, InterruptedException {
         if (data.remaining() == 0) {
           dataLengthBuffer.clear();
           data.flip();
          if (skipInitialSaslHandshake) {
            data = null;
            skipInitialSaslHandshake = false;
            continue;
          }
           boolean isHeaderRead = connectionContextRead;
          if (saslServer != null) {
            saslReadAndProcess(data.array());
          if (authProtocol == AuthProtocol.SASL) {
            // switch to simple must ignore next negotiate or initiate
            if (skipInitialSaslHandshake) {
              authProtocol = AuthProtocol.NONE;
            } else {
              saslReadAndProcess(data.array());
            }
           } else {
             processOneRpc(data.array());
           }
@@ -1393,102 +1472,79 @@ public int readAndProcess() throws IOException, InterruptedException {
       }
     }
 
    private AuthMethod initializeAuthContext(AuthMethod authMethod)
    private AuthProtocol initializeAuthContext(int authType)
         throws IOException, InterruptedException {
      try {
        if (enabledAuthMethods.contains(authMethod)) {
          saslServer = createSaslServer(authMethod);
        } else if (enabledAuthMethods.contains(AuthMethod.SIMPLE)) {
          doSaslReply(SaslStatus.SUCCESS, new IntWritable(
              SaslRpcServer.SWITCH_TO_SIMPLE_AUTH), null, null);
          authMethod = AuthMethod.SIMPLE;
          // client has already sent the initial Sasl message and we
          // should ignore it. Both client and server should fall back
          // to simple auth from now on.
          skipInitialSaslHandshake = true;
        } else {
          throw new AccessControlException(
              authMethod + " authentication is not enabled."
                  + "  Available:" + enabledAuthMethods);
        }
      } catch (IOException ioe) {
        final String ioeClass = ioe.getClass().getName();
        final String ioeMessage  = ioe.getLocalizedMessage();
        if (authMethod == AuthMethod.SIMPLE) {
          setupResponse(authFailedResponse, authFailedCall,
              RpcStatusProto.FATAL, RpcErrorCodeProto.FATAL_UNAUTHORIZED, 
              null, ioeClass, ioeMessage);
          responder.doRespond(authFailedCall);
        } else {
          doSaslReply(SaslStatus.ERROR, null, ioeClass, ioeMessage);
        }
        throw ioe;
      AuthProtocol authProtocol = AuthProtocol.valueOf(authType);
      if (authProtocol == null) {
        IOException ioe = new IpcException("Unknown auth protocol:" + authType);
        doSaslReply(ioe);
        throw ioe;        
       }
      return authMethod;
    }

    private SaslServer createSaslServer(AuthMethod authMethod)
        throws IOException, InterruptedException {
      String hostname = null;
      String saslProtocol = null;
      CallbackHandler saslCallback = null;
      
      switch (authMethod) {
        case SIMPLE: {
          return null; // no sasl for simple
        }
        case DIGEST: {
          secretManager.checkAvailableForRead();
          hostname = SaslRpcServer.SASL_DEFAULT_REALM;
          saslCallback = new SaslDigestCallbackHandler(secretManager, this);
      boolean isSimpleEnabled = enabledAuthMethods.contains(AuthMethod.SIMPLE);
      switch (authProtocol) {
        case NONE: {
          // don't reply if client is simple and server is insecure
          if (!isSimpleEnabled) {
            IOException ioe = new AccessControlException(
                "SIMPLE authentication is not enabled."
                    + "  Available:" + enabledAuthMethods);
            doSaslReply(ioe);
            throw ioe;
          }
           break;
         }
        case KERBEROS: {
          String fullName = UserGroupInformation.getCurrentUser().getUserName();
          if (LOG.isDebugEnabled())
            LOG.debug("Kerberos principal name is " + fullName);
          KerberosName krbName = new KerberosName(fullName);
          hostname = krbName.getHostName();
          if (hostname == null) {
            throw new AccessControlException(
                "Kerberos principal name does NOT have the expected "
                    + "hostname part: " + fullName);
        case SASL: {
          if (isSimpleEnabled) { // switch to simple hack
            skipInitialSaslHandshake = true;
            doSaslReply(buildSaslResponse(SaslState.SUCCESS, null));
           }
          saslProtocol = krbName.getServiceName();
          saslCallback = new SaslGssCallbackHandler();
          // else wait for a negotiate or initiate
           break;
         }
        default:
          // we should never be able to get here
          throw new AccessControlException(
              "Server does not support SASL " + authMethod);
       }
      
      return createSaslServer(authMethod.getMechanismName(), saslProtocol,
                              hostname, saslCallback);                                    
    }

    private SaslServer createSaslServer(final String mechanism,
                                        final String protocol,
                                        final String hostname,
                                        final CallbackHandler callback
        ) throws IOException, InterruptedException {
      SaslServer saslServer = UserGroupInformation.getCurrentUser().doAs(
          new PrivilegedExceptionAction<SaslServer>() {
            @Override
            public SaslServer run() throws SaslException  {
              return Sasl.createSaslServer(mechanism, protocol, hostname,
                                           SaslRpcServer.SASL_PROPS, callback);
            }
          });
      if (saslServer == null) {
        throw new AccessControlException(
            "Unable to find SASL server implementation for " + mechanism);
      return authProtocol;
    }

    private RpcSaslProto buildSaslNegotiateResponse()
        throws IOException, InterruptedException {
      RpcSaslProto negotiateMessage = negotiateResponse;
      // accelerate token negotiation by sending initial challenge
      // in the negotiation response
      if (enabledAuthMethods.contains(AuthMethod.TOKEN)) {
        saslServer = createSaslServer(AuthMethod.TOKEN);
        byte[] challenge = saslServer.evaluateResponse(new byte[0]);
        RpcSaslProto.Builder negotiateBuilder =
            RpcSaslProto.newBuilder(negotiateResponse);
        negotiateBuilder.getAuthsBuilder(0)  // TOKEN is always first
            .setChallenge(ByteString.copyFrom(challenge));
        negotiateMessage = negotiateBuilder.build();
       }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Created SASL server with mechanism = " + mechanism);
      sentNegotiate = true;
      return negotiateMessage;
    }
    
    private AuthMethod createSaslServer(String authMethodName)
        throws IOException, InterruptedException {
      AuthMethod authMethod;
      try {
        authMethod = AuthMethod.valueOf(authMethodName);
        if (!enabledAuthMethods.contains(authMethod)) {
          authMethod = null;
        }
      } catch (IllegalArgumentException iae) {
        authMethod = null;
      }
      if (authMethod != null &&
          // sasl server for tokens may already be instantiated
          (saslServer == null || authMethod != AuthMethod.TOKEN)) {
        saslServer = createSaslServer(authMethod);
       }
      return saslServer;
      return authMethod;
    }

    private SaslServer createSaslServer(AuthMethod authMethod)
        throws IOException, InterruptedException {
      return new SaslRpcServer(authMethod).create(this, secretManager);
     }
     
     /**
@@ -1557,7 +1613,7 @@ private void processConnectionContext(byte[] buf) throws IOException {
         //this is not allowed if user authenticated with DIGEST.
         if ((protocolUser != null)
             && (!protocolUser.getUserName().equals(user.getUserName()))) {
          if (authMethod == AuthMethod.DIGEST) {
          if (authMethod == AuthMethod.TOKEN) {
             // Not allowed to doAs if token authentication is used
             throw new AccessControlException("Authenticated user (" + user
                 + ") doesn't match what the client claims to be ("
@@ -1713,7 +1769,7 @@ private boolean authorizeConnection() throws IOException {
         // authorize real user. doAs is allowed only for simple or kerberos
         // authentication
         if (user != null && user.getRealUser() != null
            && (authMethod != AuthMethod.DIGEST)) {
            && (authMethod != AuthMethod.TOKEN)) {
           ProxyUsers.authorize(user, this.getHostAddress(), conf);
         }
         authorize(user, protocolName, getHostInetAddress());
@@ -1954,6 +2010,7 @@ protected Server(String bindAddress, int port,
 
     // configure supported authentications
     this.enabledAuthMethods = getAuthMethods(secretManager, conf);
    this.negotiateResponse = buildNegotiateResponse(enabledAuthMethods);
     
     // Start the listener here and let it bind to the port
     listener = new Listener();
@@ -1973,17 +2030,33 @@ protected Server(String bindAddress, int port,
     
     this.exceptionsHandler.addTerseExceptions(StandbyException.class);
   }
  
  private RpcSaslProto buildNegotiateResponse(List<AuthMethod> authMethods)
      throws IOException {
    RpcSaslProto.Builder negotiateBuilder = RpcSaslProto.newBuilder();
    negotiateBuilder.setState(SaslState.NEGOTIATE);
    for (AuthMethod authMethod : authMethods) {
      if (authMethod == AuthMethod.SIMPLE) { // not a SASL method
        continue;
      }
      SaslRpcServer saslRpcServer = new SaslRpcServer(authMethod);      
      negotiateBuilder.addAuthsBuilder()
          .setMethod(authMethod.toString())
          .setMechanism(saslRpcServer.mechanism)
          .setProtocol(saslRpcServer.protocol)
          .setServerId(saslRpcServer.serverId);
    }
    return negotiateBuilder.build();
  }
 
   // get the security type from the conf. implicitly include token support
   // if a secret manager is provided, or fail if token is the conf value but
   // there is no secret manager
  private EnumSet<AuthMethod> getAuthMethods(SecretManager<?> secretManager,
  private List<AuthMethod> getAuthMethods(SecretManager<?> secretManager,
                                              Configuration conf) {
     AuthenticationMethod confAuthenticationMethod =
         SecurityUtil.getAuthenticationMethod(conf);        
    EnumSet<AuthMethod> authMethods =
        EnumSet.of(confAuthenticationMethod.getAuthMethod()); 
        
    List<AuthMethod> authMethods = new ArrayList<AuthMethod>();
     if (confAuthenticationMethod == AuthenticationMethod.TOKEN) {
       if (secretManager == null) {
         throw new IllegalArgumentException(AuthenticationMethod.TOKEN +
@@ -1992,8 +2065,10 @@ protected Server(String bindAddress, int port,
     } else if (secretManager != null) {
       LOG.debug(AuthenticationMethod.TOKEN +
           " authentication enabled for secret manager");
      // most preferred, go to the front of the line!
       authMethods.add(AuthenticationMethod.TOKEN.getAuthMethod());
     }
    authMethods.add(confAuthenticationMethod.getAuthMethod());        
     
     LOG.debug("Server accepts auth methods:" + authMethods);
     return authMethods;
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java
index ef97eb528c3..372e13b5a11 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java
@@ -42,14 +42,24 @@
 import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.ipc.ProtobufRpcEngine.RpcRequestMessageWrapper;
import org.apache.hadoop.ipc.ProtobufRpcEngine.RpcResponseMessageWrapper;
import org.apache.hadoop.ipc.RPC.RpcKind;
 import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.ipc.Server.AuthProtocol;
import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcRequestHeaderProto;
import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcRequestHeaderProto.OperationProto;
import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto;
import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcSaslProto;
import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcSaslProto.SaslAuth;
import org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcSaslProto.SaslState;
 import org.apache.hadoop.security.SaslRpcServer.AuthMethod;
import org.apache.hadoop.security.SaslRpcServer.SaslStatus;
 import org.apache.hadoop.security.authentication.util.KerberosName;
 import org.apache.hadoop.security.token.Token;
 import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.util.ProtoUtil;
 
import com.google.protobuf.ByteString;
 /**
  * A utility class that encapsulates SASL logic for RPC client
  */
@@ -58,9 +68,15 @@
 public class SaslRpcClient {
   public static final Log LOG = LogFactory.getLog(SaslRpcClient.class);
 
  private final AuthMethod authMethod;
   private final SaslClient saslClient;
   private final boolean fallbackAllowed;

  private static final RpcRequestHeaderProto saslHeader =
      ProtoUtil.makeRpcRequestHeader(RpcKind.RPC_PROTOCOL_BUFFER,
          OperationProto.RPC_FINAL_PACKET, AuthProtocol.SASL.callId);
  private static final RpcSaslProto negotiateRequest =
      RpcSaslProto.newBuilder().setState(SaslState.NEGOTIATE).build();
  
   /**
    * Create a SaslRpcClient for an authentication method
    * 
@@ -73,6 +89,7 @@ public SaslRpcClient(AuthMethod method,
       Token<? extends TokenIdentifier> token, String serverPrincipal,
       boolean fallbackAllowed)
       throws IOException {
    this.authMethod = method;
     this.fallbackAllowed = fallbackAllowed;
     String saslUser = null;
     String saslProtocol = null;
@@ -81,7 +98,8 @@ public SaslRpcClient(AuthMethod method,
     CallbackHandler saslCallback = null;
     
     switch (method) {
      case DIGEST: {
      case TOKEN: {
        saslProtocol = "";
         saslServerName = SaslRpcServer.SASL_DEFAULT_REALM;
         saslCallback = new SaslClientCallbackHandler(token);
         break;
@@ -107,7 +125,7 @@ public SaslRpcClient(AuthMethod method,
     
     String mechanism = method.getMechanismName();
     if (LOG.isDebugEnabled()) {
      LOG.debug("Creating SASL " + mechanism
      LOG.debug("Creating SASL " + mechanism + "(" + authMethod + ") "
           + " client to authenticate to service at " + saslServerName);
     }
     saslClient = Sasl.createSaslClient(
@@ -118,14 +136,6 @@ public SaslRpcClient(AuthMethod method,
     }
   }
 
  private static void readStatus(DataInputStream inStream) throws IOException {
    int status = inStream.readInt(); // read status
    if (status != SaslStatus.SUCCESS.state) {
      throw new RemoteException(WritableUtils.readString(inStream),
          WritableUtils.readString(inStream));
    }
  }
  
   /**
    * Do client side SASL authentication with server via the given InputStream
    * and OutputStream
@@ -143,56 +153,142 @@ public boolean saslConnect(InputStream inS, OutputStream outS)
     DataInputStream inStream = new DataInputStream(new BufferedInputStream(inS));
     DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(
         outS));

    try {
      byte[] saslToken = new byte[0];
      if (saslClient.hasInitialResponse())
        saslToken = saslClient.evaluateChallenge(saslToken);
      while (saslToken != null) {
        outStream.writeInt(saslToken.length);
        outStream.write(saslToken, 0, saslToken.length);
        outStream.flush();
        if (LOG.isDebugEnabled())
          LOG.debug("Have sent token of size " + saslToken.length
              + " from initSASLContext.");
        readStatus(inStream);
        int len = inStream.readInt();
        if (len == SaslRpcServer.SWITCH_TO_SIMPLE_AUTH) {
          if (!fallbackAllowed) {
            throw new IOException("Server asks us to fall back to SIMPLE " +
                "auth, but this client is configured to only allow secure " +
                "connections.");
          }
          if (LOG.isDebugEnabled())
            LOG.debug("Server asks us to fall back to simple auth.");
          saslClient.dispose();
          return false;
        } else if ((len == 0) && saslClient.isComplete()) {
          break;
        }
        saslToken = new byte[len];
        if (LOG.isDebugEnabled())
          LOG.debug("Will read input token of size " + saslToken.length
              + " for processing by initSASLContext");
        inStream.readFully(saslToken);
        saslToken = saslClient.evaluateChallenge(saslToken);
    
    // track if SASL ever started, or server switched us to simple
    boolean inSasl = false;
    sendSaslMessage(outStream, negotiateRequest);
    
    // loop until sasl is complete or a rpc error occurs
    boolean done = false;
    do {
      int totalLen = inStream.readInt();
      RpcResponseMessageWrapper responseWrapper =
          new RpcResponseMessageWrapper();
      responseWrapper.readFields(inStream);
      RpcResponseHeaderProto header = responseWrapper.getMessageHeader();
      switch (header.getStatus()) {
        case ERROR: // might get a RPC error during 
        case FATAL:
          throw new RemoteException(header.getExceptionClassName(),
                                    header.getErrorMsg());
        default: break;
       }
      if (!saslClient.isComplete()) { // shouldn't happen
        throw new SaslException("Internal negotiation error");
      if (totalLen != responseWrapper.getLength()) {
        throw new SaslException("Received malformed response length");
       }
      
      if (header.getCallId() != AuthProtocol.SASL.callId) {
        throw new SaslException("Non-SASL response during negotiation");
      }
      RpcSaslProto saslMessage =
          RpcSaslProto.parseFrom(responseWrapper.getMessageBytes());
       if (LOG.isDebugEnabled()) {
        LOG.debug("SASL client context established. Negotiated QoP: "
            + saslClient.getNegotiatedProperty(Sasl.QOP));
        LOG.debug("Received SASL message "+saslMessage);
      }
      // handle sasl negotiation process
      RpcSaslProto.Builder response = null;
      switch (saslMessage.getState()) {
        case NEGOTIATE: {
          inSasl = true;
          // TODO: should instantiate sasl client based on advertisement
          // but just blindly use the pre-instantiated sasl client for now
          String clientAuthMethod = authMethod.toString();
          SaslAuth saslAuthType = null;
          for (SaslAuth authType : saslMessage.getAuthsList()) {
            if (clientAuthMethod.equals(authType.getMethod())) {
              saslAuthType = authType;
              break;
            }
          }
          if (saslAuthType == null) {
            saslAuthType = SaslAuth.newBuilder()
                .setMethod(clientAuthMethod)
                .setMechanism(saslClient.getMechanismName())
                .build();
          }
          
          byte[] challengeToken = null;
          if (saslAuthType != null && saslAuthType.hasChallenge()) {
            // server provided the first challenge
            challengeToken = saslAuthType.getChallenge().toByteArray();
            saslAuthType =
              SaslAuth.newBuilder(saslAuthType).clearChallenge().build();
          } else if (saslClient.hasInitialResponse()) {
            challengeToken = new byte[0];
          }
          byte[] responseToken = (challengeToken != null)
              ? saslClient.evaluateChallenge(challengeToken)
              : new byte[0];
          
          response = createSaslReply(SaslState.INITIATE, responseToken);
          response.addAuths(saslAuthType);
          break;
        }
        case CHALLENGE: {
          inSasl = true;
          byte[] responseToken = saslEvaluateToken(saslMessage, false);
          response = createSaslReply(SaslState.RESPONSE, responseToken);
          break;
        }
        case SUCCESS: {
          if (inSasl && saslEvaluateToken(saslMessage, true) != null) {
            throw new SaslException("SASL client generated spurious token");
          }
          done = true;
          break;
        }
        default: {
          throw new SaslException(
              "RPC client doesn't support SASL " + saslMessage.getState());
        }
       }
      return true;
    } catch (IOException e) {
      try {
        saslClient.dispose();
      } catch (SaslException ignored) {
        // ignore further exceptions during cleanup
      if (response != null) {
        sendSaslMessage(outStream, response.build());
       }
      throw e;
    } while (!done);
    if (!inSasl && !fallbackAllowed) {
      throw new IOException("Server asks us to fall back to SIMPLE " +
          "auth, but this client is configured to only allow secure " +
          "connections.");
    }
    return inSasl;
  }
  
  private void sendSaslMessage(DataOutputStream out, RpcSaslProto message)
      throws IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Sending sasl message "+message);
    }
    RpcRequestMessageWrapper request =
        new RpcRequestMessageWrapper(saslHeader, message);
    out.writeInt(request.getLength());
    request.write(out);
    out.flush();    
  }
  
  private byte[] saslEvaluateToken(RpcSaslProto saslResponse,
      boolean done) throws SaslException {
    byte[] saslToken = null;
    if (saslResponse.hasToken()) {
      saslToken = saslResponse.getToken().toByteArray();
      saslToken = saslClient.evaluateChallenge(saslToken);
    } else if (!done) {
      throw new SaslException("Challenge contains no token");
    }
    if (done && !saslClient.isComplete()) {
      throw new SaslException("Client is out of sync with server");
    }
    return saslToken;
  }
  
  private RpcSaslProto.Builder createSaslReply(SaslState state,
                                               byte[] responseToken) {
    RpcSaslProto.Builder response = RpcSaslProto.newBuilder();
    response.setState(state);
    if (responseToken != null) {
      response.setToken(ByteString.copyFrom(responseToken));
     }
    return response;
   }
 
   /**
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcServer.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcServer.java
index 33942dc0885..2932bb4f064 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcServer.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcServer.java
@@ -23,6 +23,7 @@
 import java.io.DataInputStream;
 import java.io.DataOutput;
 import java.io.IOException;
import java.security.PrivilegedExceptionAction;
 import java.security.Security;
 import java.util.Map;
 import java.util.TreeMap;
@@ -35,6 +36,8 @@
 import javax.security.sasl.AuthorizeCallback;
 import javax.security.sasl.RealmCallback;
 import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
 
 import org.apache.commons.codec.binary.Base64;
 import org.apache.commons.logging.Log;
@@ -43,6 +46,8 @@
 import org.apache.hadoop.classification.InterfaceStability;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.ipc.Server.Connection;
import org.apache.hadoop.security.authentication.util.KerberosName;
 import org.apache.hadoop.security.token.SecretManager;
 import org.apache.hadoop.security.token.TokenIdentifier;
 import org.apache.hadoop.security.token.SecretManager.InvalidToken;
@@ -58,8 +63,6 @@
   public static final Map<String, String> SASL_PROPS = 
       new TreeMap<String, String>();
 
  public static final int SWITCH_TO_SIMPLE_AUTH = -88;

   public static enum QualityOfProtection {
     AUTHENTICATION("auth"),
     INTEGRITY("auth-int"),
@@ -75,7 +78,93 @@ public String getSaslQop() {
       return saslQop;
     }
   }

  @InterfaceAudience.Private
  @InterfaceStability.Unstable
  public AuthMethod authMethod;
  public String mechanism;
  public String protocol;
  public String serverId;
  
  @InterfaceAudience.Private
  @InterfaceStability.Unstable
  public SaslRpcServer(AuthMethod authMethod) throws IOException {
    this.authMethod = authMethod;
    mechanism = authMethod.getMechanismName();    
    switch (authMethod) {
      case SIMPLE: {
        return; // no sasl for simple
      }
      case TOKEN: {
        protocol = "";
        serverId = SaslRpcServer.SASL_DEFAULT_REALM;
        break;
      }
      case KERBEROS: {
        String fullName = UserGroupInformation.getCurrentUser().getUserName();
        if (LOG.isDebugEnabled())
          LOG.debug("Kerberos principal name is " + fullName);
        KerberosName krbName = new KerberosName(fullName);
        serverId = krbName.getHostName();
        if (serverId == null) {
          serverId = "";
        }
        protocol = krbName.getServiceName();
        break;
      }
      default:
        // we should never be able to get here
        throw new AccessControlException(
            "Server does not support SASL " + authMethod);
    }
  }
   
  @InterfaceAudience.Private
  @InterfaceStability.Unstable
  public SaslServer create(Connection connection,
                           SecretManager<TokenIdentifier> secretManager
      ) throws IOException, InterruptedException {
    UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
    final CallbackHandler callback;
    switch (authMethod) {
      case TOKEN: {
        secretManager.checkAvailableForRead();
        callback = new SaslDigestCallbackHandler(secretManager, connection);
        break;
      }
      case KERBEROS: {
        if (serverId.isEmpty()) {
          throw new AccessControlException(
              "Kerberos principal name does NOT have the expected "
                  + "hostname part: " + ugi.getUserName());
        }
        callback = new SaslGssCallbackHandler();
        break;
      }
      default:
        // we should never be able to get here
        throw new AccessControlException(
            "Server does not support SASL " + authMethod);
    }
    
    SaslServer saslServer = ugi.doAs(
        new PrivilegedExceptionAction<SaslServer>() {
          @Override
          public SaslServer run() throws SaslException  {
            return Sasl.createSaslServer(mechanism, protocol, serverId,
                SaslRpcServer.SASL_PROPS, callback);
          }
        });
    if (saslServer == null) {
      throw new AccessControlException(
          "Unable to find SASL server implementation for " + mechanism);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Created SASL server with mechanism = " + mechanism);
    }
    return saslServer;
  }

   public static void init(Configuration conf) {
     QualityOfProtection saslQOP = QualityOfProtection.AUTHENTICATION;
     String rpcProtection = conf.get("hadoop.rpc.protection",
@@ -124,23 +213,14 @@ static String encodeIdentifier(byte[] identifier) {
     return fullName.split("[/@]");
   }
 
  @InterfaceStability.Evolving
  public enum SaslStatus {
    SUCCESS (0),
    ERROR (1);
    
    public final int state;
    private SaslStatus(int state) {
      this.state = state;
    }
  }
  
   /** Authentication method */
   @InterfaceStability.Evolving
   public static enum AuthMethod {
     SIMPLE((byte) 80, ""),
     KERBEROS((byte) 81, "GSSAPI"),
    @Deprecated
     DIGEST((byte) 82, "DIGEST-MD5"),
    TOKEN((byte) 82, "DIGEST-MD5"),
     PLAIN((byte) 83, "PLAIN");
 
     /** The code for this method. */
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
index 990b31c9300..4760a64dff9 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
@@ -1076,7 +1076,7 @@ public static UserGroupInformation createRemoteUser(String user) {
         HadoopConfiguration.SIMPLE_CONFIG_NAME),
     KERBEROS(AuthMethod.KERBEROS,
         HadoopConfiguration.USER_KERBEROS_CONFIG_NAME),
    TOKEN(AuthMethod.DIGEST),
    TOKEN(AuthMethod.TOKEN),
     CERTIFICATE(null),
     KERBEROS_SSL(null),
     PROXY(null);
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/ProtoUtil.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/ProtoUtil.java
index bec2e85af85..ac6c572b346 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/ProtoUtil.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/ProtoUtil.java
@@ -94,7 +94,7 @@ public static IpcConnectionContextProto makeIpcConnectionContext(
         // Real user was established as part of the connection.
         // Send effective user only.
         ugiProto.setEffectiveUser(ugi.getUserName());
      } else if (authMethod == AuthMethod.DIGEST) {
      } else if (authMethod == AuthMethod.TOKEN) {
         // With token, the connection itself establishes 
         // both real and effective user. Hence send none in header.
       } else {  // Simple authentication
diff --git a/hadoop-common-project/hadoop-common/src/main/proto/RpcHeader.proto b/hadoop-common-project/hadoop-common/src/main/proto/RpcHeader.proto
index 13d3be6dc1d..872f29db730 100644
-- a/hadoop-common-project/hadoop-common/src/main/proto/RpcHeader.proto
++ b/hadoop-common-project/hadoop-common/src/main/proto/RpcHeader.proto
@@ -127,3 +127,26 @@ message RpcResponseHeaderProto {
   optional string errorMsg = 5;  // if request fails, often contains strack trace
   optional RpcErrorCodeProto errorDetail = 6; // in case of error
 }

message RpcSaslProto {
  enum SaslState {
    SUCCESS   = 0;
    NEGOTIATE = 1;
    INITIATE  = 2;
    CHALLENGE = 3;
    RESPONSE  = 4;
  }
  
  message SaslAuth {
    required string method    = 1;
    required string mechanism = 2;
    optional string protocol  = 3;
    optional string serverId  = 4;
    optional bytes  challenge = 5;
  }

  optional uint32 version  = 1;  
  required SaslState state = 2;
  optional bytes token     = 3;
  repeated SaslAuth auths  = 4;
}
\ No newline at end of file
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java
index cb3e0435ec6..ce44e421054 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java
@@ -674,6 +674,7 @@ private String getAuthMethod(
     try {
       return internalGetAuthMethod(clientAuth, serverAuth, false, false);
     } catch (Exception e) {
      LOG.warn("Auth method failure", e);
       return e.toString();
     }
   }
@@ -685,6 +686,7 @@ private String getAuthMethod(
     try {
       return internalGetAuthMethod(clientAuth, serverAuth, true, useValidToken);
     } catch (Exception e) {
      LOG.warn("Auth method failure", e);
       return e.toString();
     }
   }
@@ -702,7 +704,7 @@ private String internalGetAuthMethod(
     UserGroupInformation.setConfiguration(serverConf);
     
     final UserGroupInformation serverUgi =
        UserGroupInformation.createRemoteUser(currentUser + "-SERVER");
        UserGroupInformation.createRemoteUser(currentUser + "-SERVER/localhost@NONE");
     serverUgi.setAuthenticationMethod(serverAuth);
 
     final TestTokenSecretManager sm = new TestTokenSecretManager();
- 
2.19.1.windows.1

