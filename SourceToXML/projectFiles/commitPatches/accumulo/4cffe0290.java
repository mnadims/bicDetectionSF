From 4cffe02905527954a031b0994f68bce6faf670f5 Mon Sep 17 00:00:00 2001
From: Mike Miller <mmiller@apache.org>
Date: Wed, 5 Jul 2017 16:38:37 -0400
Subject: [PATCH] ACCUMULO-4674 Fixed ExistingMacIT

--
 .../src/main/java/org/apache/accumulo/test/ExistingMacIT.java | 4 ++++
 .../apache/accumulo/test/functional/ConfigurableMacBase.java  | 1 -
 2 files changed, 4 insertions(+), 1 deletion(-)

diff --git a/test/src/main/java/org/apache/accumulo/test/ExistingMacIT.java b/test/src/main/java/org/apache/accumulo/test/ExistingMacIT.java
index ff8a6ac41..90fb828b8 100644
-- a/test/src/main/java/org/apache/accumulo/test/ExistingMacIT.java
++ b/test/src/main/java/org/apache/accumulo/test/ExistingMacIT.java
@@ -64,6 +64,10 @@ public class ExistingMacIT extends ConfigurableMacBase {
   @Override
   public void configure(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
     cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "15s");
    // NativeMap.java was changed to fail if native lib missing in ACCUMULO-4596
    // testExistingInstance will fail because the native path is not set in MiniAccumuloConfigImpl.useExistingInstance
    // so disable Native maps for this test
    cfg.setProperty(Property.TSERV_NATIVEMAP_ENABLED, "false");
 
     // use raw local file system so walogs sync and flush will work
     hadoopCoreSite.set("fs.file.impl", RawLocalFileSystem.class.getName());
diff --git a/test/src/main/java/org/apache/accumulo/test/functional/ConfigurableMacBase.java b/test/src/main/java/org/apache/accumulo/test/functional/ConfigurableMacBase.java
index cce906f59..ecc32021f 100644
-- a/test/src/main/java/org/apache/accumulo/test/functional/ConfigurableMacBase.java
++ b/test/src/main/java/org/apache/accumulo/test/functional/ConfigurableMacBase.java
@@ -137,7 +137,6 @@ public class ConfigurableMacBase extends AccumuloITBase {
     cfg.setProperty(Property.GC_FILE_ARCHIVE, Boolean.TRUE.toString());
     Configuration coreSite = new Configuration(false);
     configure(cfg, coreSite);
    cfg.setProperty(Property.TSERV_NATIVEMAP_ENABLED, Boolean.TRUE.toString());
     configureForEnvironment(cfg, getClass(), getSslDir(baseDir));
     cluster = new MiniAccumuloClusterImpl(cfg);
     if (coreSite.size() > 0) {
- 
2.19.1.windows.1

