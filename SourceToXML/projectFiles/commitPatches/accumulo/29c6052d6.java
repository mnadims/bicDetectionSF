From 29c6052d64de0705729bf32136e44505e0c788cc Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Tue, 12 Jan 2016 12:09:49 -0500
Subject: [PATCH] ACCUMULO-4108 Increase zk timeout from 5s to 15s for ITs.

--
 test/src/test/java/org/apache/accumulo/test/Accumulo3010IT.java | 2 +-
 test/src/test/java/org/apache/accumulo/test/ExistingMacIT.java  | 2 +-
 .../org/apache/accumulo/test/MasterRepairsDualAssignmentIT.java | 2 +-
 .../java/org/apache/accumulo/test/MultiTableRecoveryIT.java     | 2 +-
 .../org/apache/accumulo/test/functional/MasterFailoverIT.java   | 2 +-
 .../java/org/apache/accumulo/test/functional/RestartIT.java     | 2 +-
 .../org/apache/accumulo/test/functional/RestartStressIT.java    | 2 +-
 7 files changed, 7 insertions(+), 7 deletions(-)

diff --git a/test/src/test/java/org/apache/accumulo/test/Accumulo3010IT.java b/test/src/test/java/org/apache/accumulo/test/Accumulo3010IT.java
index eff725186..8a0086b98 100644
-- a/test/src/test/java/org/apache/accumulo/test/Accumulo3010IT.java
++ b/test/src/test/java/org/apache/accumulo/test/Accumulo3010IT.java
@@ -48,7 +48,7 @@ public class Accumulo3010IT extends AccumuloClusterIT {
   @Override
   public void configureMiniCluster(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
     cfg.setNumTservers(1);
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "5s");
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "15s");
     // file system supports recovery
     hadoopCoreSite.set("fs.file.impl", RawLocalFileSystem.class.getName());
   }
diff --git a/test/src/test/java/org/apache/accumulo/test/ExistingMacIT.java b/test/src/test/java/org/apache/accumulo/test/ExistingMacIT.java
index d55e427e3..323888a56 100644
-- a/test/src/test/java/org/apache/accumulo/test/ExistingMacIT.java
++ b/test/src/test/java/org/apache/accumulo/test/ExistingMacIT.java
@@ -56,7 +56,7 @@ public class ExistingMacIT extends ConfigurableMacIT {
 
   @Override
   public void configure(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "5s");
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "15s");
 
     // use raw local file system so walogs sync and flush will work
     hadoopCoreSite.set("fs.file.impl", RawLocalFileSystem.class.getName());
diff --git a/test/src/test/java/org/apache/accumulo/test/MasterRepairsDualAssignmentIT.java b/test/src/test/java/org/apache/accumulo/test/MasterRepairsDualAssignmentIT.java
index 72dabdf04..f1372dc7f 100644
-- a/test/src/test/java/org/apache/accumulo/test/MasterRepairsDualAssignmentIT.java
++ b/test/src/test/java/org/apache/accumulo/test/MasterRepairsDualAssignmentIT.java
@@ -59,7 +59,7 @@ public class MasterRepairsDualAssignmentIT extends ConfigurableMacIT {
 
   @Override
   public void configure(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "5s");
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "15s");
     cfg.setProperty(Property.MASTER_RECOVERY_DELAY, "5s");
     // use raw local file system so walogs sync and flush will work
     hadoopCoreSite.set("fs.file.impl", RawLocalFileSystem.class.getName());
diff --git a/test/src/test/java/org/apache/accumulo/test/MultiTableRecoveryIT.java b/test/src/test/java/org/apache/accumulo/test/MultiTableRecoveryIT.java
index 3b2565507..116092be7 100644
-- a/test/src/test/java/org/apache/accumulo/test/MultiTableRecoveryIT.java
++ b/test/src/test/java/org/apache/accumulo/test/MultiTableRecoveryIT.java
@@ -45,7 +45,7 @@ public class MultiTableRecoveryIT extends ConfigurableMacIT {
 
   @Override
   protected void configure(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "5s");
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "15s");
 
     // use raw local file system so walogs sync and flush will work
     hadoopCoreSite.set("fs.file.impl", RawLocalFileSystem.class.getName());
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/MasterFailoverIT.java b/test/src/test/java/org/apache/accumulo/test/functional/MasterFailoverIT.java
index 7634f109b..0c2631f49 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/MasterFailoverIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/MasterFailoverIT.java
@@ -35,7 +35,7 @@ public class MasterFailoverIT extends AccumuloClusterIT {
 
   @Override
   public void configureMiniCluster(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
    cfg.setSiteConfig(Collections.singletonMap(Property.INSTANCE_ZK_TIMEOUT.getKey(), "5s"));
    cfg.setSiteConfig(Collections.singletonMap(Property.INSTANCE_ZK_TIMEOUT.getKey(), "15s"));
   }
 
   @Override
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/RestartIT.java b/test/src/test/java/org/apache/accumulo/test/functional/RestartIT.java
index 4e55ab477..b498412dc 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/RestartIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/RestartIT.java
@@ -67,7 +67,7 @@ public class RestartIT extends AccumuloClusterIT {
   @Override
   public void configureMiniCluster(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
     Map<String,String> props = new HashMap<String,String>();
    props.put(Property.INSTANCE_ZK_TIMEOUT.getKey(), "5s");
    props.put(Property.INSTANCE_ZK_TIMEOUT.getKey(), "15s");
     props.put(Property.GC_CYCLE_DELAY.getKey(), "1s");
     props.put(Property.GC_CYCLE_START.getKey(), "1s");
     cfg.setSiteConfig(props);
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/RestartStressIT.java b/test/src/test/java/org/apache/accumulo/test/functional/RestartStressIT.java
index b965420c4..c4b3afddd 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/RestartStressIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/RestartStressIT.java
@@ -55,7 +55,7 @@ public class RestartStressIT extends AccumuloClusterIT {
     opts.put(Property.TSERV_MAXMEM.getKey(), "100K");
     opts.put(Property.TSERV_MAJC_DELAY.getKey(), "100ms");
     opts.put(Property.TSERV_WALOG_MAX_SIZE.getKey(), "1M");
    opts.put(Property.INSTANCE_ZK_TIMEOUT.getKey(), "5s");
    opts.put(Property.INSTANCE_ZK_TIMEOUT.getKey(), "15s");
     opts.put(Property.MASTER_RECOVERY_DELAY.getKey(), "1s");
     cfg.setSiteConfig(opts);
     cfg.useMiniDFS(true);
- 
2.19.1.windows.1

