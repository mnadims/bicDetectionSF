From 74507ae3a3da7b7cce5dda634163a6030ad79ac3 Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Mon, 25 Jul 2016 16:21:04 -0400
Subject: [PATCH] ACCUMULO-4353 Avoid fallthrough compiler warning

Avoid a newly introduced switch case fallthrough warning from the Java
compiler with an explicit method call for the common functionality.
--
 .../accumulo/master/TabletGroupWatcher.java     | 17 +++++++++++------
 1 file changed, 11 insertions(+), 6 deletions(-)

diff --git a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
index 76fda2172..3f7dc74f1 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
++ b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
@@ -322,13 +322,10 @@ abstract class TabletGroupWatcher extends Daemon {
               case SUSPENDED:
                 // Request a move to UNASSIGNED, so as to allow balancing to continue.
                 suspendedToGoneServers.add(tls);
                // Fall through to unassigned to cancel migrations.
                cancelOfflineTableMigrations(tls);
                break;
               case UNASSIGNED:
                TServerInstance dest = this.master.migrations.get(tls.extent);
                TableState tableState = TableManager.getInstance().getTableState(tls.extent.getTableId());
                if (dest != null && tableState == TableState.OFFLINE) {
                  this.master.migrations.remove(tls.extent);
                }
                cancelOfflineTableMigrations(tls);
                 break;
               case ASSIGNED_TO_DEAD_SERVER:
                 assignedToDeadServers.add(tls);
@@ -401,6 +398,14 @@ abstract class TabletGroupWatcher extends Daemon {
     }
   }
 
  private void cancelOfflineTableMigrations(TabletLocationState tls) {
    TServerInstance dest = this.master.migrations.get(tls.extent);
    TableState tableState = TableManager.getInstance().getTableState(tls.extent.getTableId());
    if (dest != null && tableState == TableState.OFFLINE) {
      this.master.migrations.remove(tls.extent);
    }
  }

   private void repairMetadata(Text row) {
     Master.log.debug("Attempting repair on " + row);
     // ACCUMULO-2261 if a dying tserver writes a location before its lock information propagates, it may cause duplicate assignment.
- 
2.19.1.windows.1

