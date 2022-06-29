From a24a286d8e547467403b03cf5297bf1364701594 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Mon, 20 Jun 2016 22:16:54 -0400
Subject: [PATCH] ACCUMULO-4349 Fix test bind logic to pass regardless of
 network configuration

ServerSocket will bind to all interfaces which can cause the test to fail
when it expects that subsequent attempts to bind the same port will fail.
--
 .../server/util/TServerUtilsTest.java         | 19 ++++++++++++-------
 1 file changed, 12 insertions(+), 7 deletions(-)

diff --git a/server/base/src/test/java/org/apache/accumulo/server/util/TServerUtilsTest.java b/server/base/src/test/java/org/apache/accumulo/server/util/TServerUtilsTest.java
index e6761a570..458118dc2 100644
-- a/server/base/src/test/java/org/apache/accumulo/server/util/TServerUtilsTest.java
++ b/server/base/src/test/java/org/apache/accumulo/server/util/TServerUtilsTest.java
@@ -232,8 +232,9 @@ public class TServerUtilsTest {
   @Test(expected = UnknownHostException.class)
   public void testStartServerUsedPort() throws Exception {
     int port = getFreePort(1024);
    InetAddress addr = InetAddress.getByName("localhost");
     // Bind to the port
    ServerSocket s = new ServerSocket(port);
    ServerSocket s = new ServerSocket(port, 50, addr);
     ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, Integer.toString(port));
     try {
       startServer();
@@ -247,7 +248,8 @@ public class TServerUtilsTest {
     TServer server = null;
     int[] port = findTwoFreeSequentialPorts(1024);
     // Bind to the port
    ServerSocket s = new ServerSocket(port[0]);
    InetAddress addr = InetAddress.getByName("localhost");
    ServerSocket s = new ServerSocket(port[0], 50, addr);
     ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, Integer.toString(port[0]));
     ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_PORTSEARCH, "true");
     try {
@@ -286,10 +288,11 @@ public class TServerUtilsTest {
   @Test
   public void testStartServerPortRangeFirstPortUsed() throws Exception {
     TServer server = null;
    InetAddress addr = InetAddress.getByName("localhost");
     int[] port = findTwoFreeSequentialPorts(1024);
     String portRange = Integer.toString(port[0]) + "-" + Integer.toString(port[1]);
     // Bind to the port
    ServerSocket s = new ServerSocket(port[0]);
    ServerSocket s = new ServerSocket(port[0], 50, addr);
     ((ConfigurationCopy) factory.getConfiguration()).set(Property.TSERV_CLIENTPORT, portRange);
     try {
       ServerAddress address = startServer();
@@ -305,7 +308,7 @@ public class TServerUtilsTest {
     }
   }
 
  private int[] findTwoFreeSequentialPorts(int startingAddress) {
  private int[] findTwoFreeSequentialPorts(int startingAddress) throws UnknownHostException {
     boolean sequential = false;
     int low = startingAddress;
     int high = 0;
@@ -317,10 +320,11 @@ public class TServerUtilsTest {
     return new int[] {low, high};
   }
 
  private int getFreePort(int startingAddress) {
  private int getFreePort(int startingAddress) throws UnknownHostException {
    final InetAddress addr = InetAddress.getByName("localhost");
     for (int i = startingAddress; i < 65535; i++) {
       try {
        ServerSocket s = new ServerSocket(i);
        ServerSocket s = new ServerSocket(i, 50, addr);
         int port = s.getLocalPort();
         s.close();
         return port;
@@ -336,7 +340,8 @@ public class TServerUtilsTest {
     ClientServiceHandler clientHandler = new ClientServiceHandler(ctx, null, null);
     Iface rpcProxy = RpcWrapper.service(clientHandler, new Processor<Iface>(clientHandler));
     Processor<Iface> processor = new Processor<Iface>(rpcProxy);
    String hostname = InetAddress.getLocalHost().getHostName();
    // "localhost" explicitly to make sure we can always bind to that interface (avoids DNS misconfiguration)
    String hostname = "localhost";
 
     return TServerUtils.startServer(ctx, hostname, Property.TSERV_CLIENTPORT, processor, "TServerUtilsTest", "TServerUtilsTestThread",
         Property.TSERV_PORTSEARCH, Property.TSERV_MINTHREADS, Property.TSERV_THREADCHECK, Property.GENERAL_MAX_MESSAGE_SIZE);
- 
2.19.1.windows.1

