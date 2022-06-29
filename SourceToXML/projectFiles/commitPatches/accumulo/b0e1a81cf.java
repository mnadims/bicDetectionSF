From b0e1a81cf205a7b18fc38793a9e712b65c0a2304 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Thu, 26 Mar 2015 18:22:34 -0400
Subject: [PATCH] ACCUMULO-1259 Move waiting for a TabletServer to die from
 call to isReady.

--
 .../master/tserverOps/ShutdownTServer.java    | 51 +++++++++++--------
 1 file changed, 31 insertions(+), 20 deletions(-)

diff --git a/server/master/src/main/java/org/apache/accumulo/master/tserverOps/ShutdownTServer.java b/server/master/src/main/java/org/apache/accumulo/master/tserverOps/ShutdownTServer.java
index e3f0b8ce5..53301977a 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/tserverOps/ShutdownTServer.java
++ b/server/master/src/main/java/org/apache/accumulo/master/tserverOps/ShutdownTServer.java
@@ -24,7 +24,6 @@ import org.apache.accumulo.core.zookeeper.ZooUtil;
 import org.apache.accumulo.fate.Repo;
 import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
 import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.master.EventCoordinator.Listener;
 import org.apache.accumulo.master.Master;
 import org.apache.accumulo.master.tableOps.MasterRepo;
 import org.apache.accumulo.server.master.LiveTServerSet.TServerConnection;
@@ -40,7 +39,7 @@ public class ShutdownTServer extends MasterRepo {
   private static final long serialVersionUID = 1L;
   private static final Logger log = LoggerFactory.getLogger(ShutdownTServer.class);
   private TServerInstance server;
  private boolean force;
  private boolean force, requestedShutdown;
 
   public ShutdownTServer(TServerInstance server, boolean force) {
     this.server = server;
@@ -48,26 +47,20 @@ public class ShutdownTServer extends MasterRepo {
   }
 
   @Override
  public long isReady(long tid, Master environment) throws Exception {
    return 0;
  }

  @Override
  public Repo<Master> call(long tid, Master master) throws Exception {
  public long isReady(long tid, Master master) throws Exception {
     // suppress assignment of tablets to the server
     if (force) {
      String path = ZooUtil.getRoot(master.getInstance()) + Constants.ZTSERVERS + "/" + server.getLocation();
      ZooLock.deleteLock(path);
      path = ZooUtil.getRoot(master.getInstance()) + Constants.ZDEADTSERVERS + "/" + server.getLocation();
      IZooReaderWriter zoo = ZooReaderWriter.getInstance();
      zoo.putPersistentData(path, "forced down".getBytes(UTF_8), NodeExistsPolicy.OVERWRITE);
      return null;
      return 0;
     }
 
    // TODO move this to isReady() and drop while loop? - ACCUMULO-1259
    Listener listener = master.getEventCoordinator().getListener();
    master.shutdownTServer(server);
    while (master.onlineTabletServers().contains(server)) {
    // Inform the master that we want this server to shutdown
    // We don't want to spam the master with shutdown requests, so
    // only send this request once
    if (!requestedShutdown) {
      master.shutdownTServer(server);
    }

    if (master.onlineTabletServers().contains(server)) {
       TServerConnection connection = master.getConnection(server);
       if (connection != null) {
         try {
@@ -76,15 +69,33 @@ public class ShutdownTServer extends MasterRepo {
             log.info("tablet server hosts no tablets " + server);
             connection.halt(master.getMasterLock());
             log.info("tablet server asked to halt " + server);
            break;
            return 0;
           }
         } catch (TTransportException ex) {
           // expected
         } catch (Exception ex) {
           log.error("Error talking to tablet server " + server + ": " + ex);
         }

        // If the connection was non-null and we could coomunicate with it
        // give the master some more time to tell it to stop and for the
        // tserver to ack the request and stop itself.
        return 1000;
       }
      listener.waitForEvents(1000);
    }

    return 0;
  }

  @Override
  public Repo<Master> call(long tid, Master master) throws Exception {
    // suppress assignment of tablets to the server
    if (force) {
      String path = ZooUtil.getRoot(master.getInstance()) + Constants.ZTSERVERS + "/" + server.getLocation();
      ZooLock.deleteLock(path);
      path = ZooUtil.getRoot(master.getInstance()) + Constants.ZDEADTSERVERS + "/" + server.getLocation();
      IZooReaderWriter zoo = ZooReaderWriter.getInstance();
      zoo.putPersistentData(path, "forced down".getBytes(UTF_8), NodeExistsPolicy.OVERWRITE);
     }
 
     return null;
- 
2.19.1.windows.1

