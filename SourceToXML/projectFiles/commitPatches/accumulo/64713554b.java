From 64713554b7c114088dcb7fd432e25bcd421cc04a Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Fri, 8 Jan 2016 00:49:44 -0500
Subject: [PATCH] ACCUMULO-4095 Hacks on CustomNonBlockingServer to restore
 client address functionality.

Closes apache/accumulo#63
--
 .../server/rpc/CustomNonBlockingServer.java   | 63 +++++++++++++++++--
 1 file changed, 58 insertions(+), 5 deletions(-)

diff --git a/server/base/src/main/java/org/apache/accumulo/server/rpc/CustomNonBlockingServer.java b/server/base/src/main/java/org/apache/accumulo/server/rpc/CustomNonBlockingServer.java
index f4737be29..ae65c1e36 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/rpc/CustomNonBlockingServer.java
++ b/server/base/src/main/java/org/apache/accumulo/server/rpc/CustomNonBlockingServer.java
@@ -16,30 +16,83 @@
  */
 package org.apache.accumulo.server.rpc;
 
import java.io.IOException;
import java.lang.reflect.Field;
 import java.net.Socket;
 import java.nio.channels.SelectionKey;
 
import org.apache.accumulo.server.rpc.TServerUtils;
 import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TNonblockingServerTransport;
 import org.apache.thrift.transport.TNonblockingSocket;
 import org.apache.thrift.transport.TNonblockingTransport;
 
 /**
  * This class implements a custom non-blocking thrift server that stores the client address in thread-local storage for the invocation.
 *
  */
 public class CustomNonBlockingServer extends THsHaServer {
 
  private final Field selectAcceptThreadField;

   public CustomNonBlockingServer(Args args) {
     super(args);

    try {
      selectAcceptThreadField = TNonblockingServer.class.getDeclaredField("selectAcceptThread_");
      selectAcceptThreadField.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException("Failed to access required field in Thrift code.", e);
    }
  }

  @Override
  protected boolean startThreads() {
    // Yet another dirty/gross hack to get access to the client's address.

    // start the selector
    try {
      // Hack in our SelectAcceptThread impl
      SelectAcceptThread selectAcceptThread_ = new CustomSelectAcceptThread((TNonblockingServerTransport) serverTransport_);
      // Set the private field before continuing.
      selectAcceptThreadField.set(this, selectAcceptThread_);

      selectAcceptThread_.start();
      return true;
    } catch (IOException e) {
      LOGGER.error("Failed to start selector thread!", e);
      return false;
    } catch (IllegalAccessException | IllegalArgumentException e) {
      throw new RuntimeException("Exception setting customer select thread in Thrift");
    }
   }
 
  protected FrameBuffer createFrameBuffer(final TNonblockingTransport trans, final SelectionKey selectionKey, final AbstractSelectThread selectThread) {
    return new CustomAsyncFrameBuffer(trans, selectionKey, selectThread);
  /**
   * Custom wrapper around {@link org.apache.thrift.server.TNonblockingServer.SelectAcceptThread} to create our {@link CustomFrameBuffer}.
   */
  private class CustomSelectAcceptThread extends SelectAcceptThread {

    public CustomSelectAcceptThread(TNonblockingServerTransport serverTransport) throws IOException {
      super(serverTransport);
    }

    @Override
    protected FrameBuffer createFrameBuffer(final TNonblockingTransport trans, final SelectionKey selectionKey, final AbstractSelectThread selectThread) {
      if (processorFactory_.isAsyncProcessor()) {
        throw new IllegalStateException("This implementation does not support AsyncProcessors");
      }

      return new CustomFrameBuffer(trans, selectionKey, selectThread);
    }
   }
 
  private class CustomAsyncFrameBuffer extends AsyncFrameBuffer {
  /**
   * Custom wrapper around {@link org.apache.thrift.server.AbstractNonblockingServer.FrameBuffer} to extract the client's network location before accepting the
   * request.
   */
  private class CustomFrameBuffer extends FrameBuffer {
 
    public CustomAsyncFrameBuffer(TNonblockingTransport trans, SelectionKey selectionKey, AbstractSelectThread selectThread) {
    public CustomFrameBuffer(TNonblockingTransport trans, SelectionKey selectionKey, AbstractSelectThread selectThread) {
       super(trans, selectionKey, selectThread);
     }
 
- 
2.19.1.windows.1

