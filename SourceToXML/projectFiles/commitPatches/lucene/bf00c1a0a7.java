From bf00c1a0a75df877b163bcabf52d52e145b6dc92 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Wed, 18 Mar 2015 01:53:41 +0000
Subject: [PATCH] SOLR-7259: fix thread safety of lazy loaded plugins

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1667431 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/solr/core/PluginBag.java  | 50 ++++++++++++-------
 1 file changed, 33 insertions(+), 17 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/core/PluginBag.java b/solr/core/src/java/org/apache/solr/core/PluginBag.java
index 1122d4943e4..1a4141203e4 100644
-- a/solr/core/src/java/org/apache/solr/core/PluginBag.java
++ b/solr/core/src/java/org/apache/solr/core/PluginBag.java
@@ -235,7 +235,7 @@ public class PluginBag<T> implements AutoCloseable {
    * subclasses may choose to lazily load the plugin
    */
   public static class PluginHolder<T> implements AutoCloseable {
    protected T inst;
    private T inst;
     protected final PluginInfo pluginInfo;
 
     public PluginHolder(PluginInfo info) {
@@ -257,8 +257,14 @@ public class PluginBag<T> implements AutoCloseable {
 
     @Override
     public void close() throws Exception {
      if (inst != null && inst instanceof AutoCloseable) ((AutoCloseable) inst).close();

      // TODO: there may be a race here.  One thread can be creating a plugin
      // and another thread can come along and close everything (missing the plugin
      // that is in the state of being created and will probably never have close() called on it).
      // can close() be called concurrently with other methods?
      if (isLoaded()) {
        T myInst = get();
        if (myInst != null && myInst instanceof AutoCloseable) ((AutoCloseable) myInst).close();
      }
     }
 
     public String getClassName() {
@@ -273,6 +279,7 @@ public class PluginBag<T> implements AutoCloseable {
    * the Plugin is initialized and returned.
    */
   public static class LazyPluginHolder<T> extends PluginHolder<T> {
    private volatile T lazyInst;
     private final SolrConfig.SolrPluginInfo pluginMeta;
     protected SolrException solrException;
     private final SolrCore core;
@@ -293,37 +300,46 @@ public class PluginBag<T> implements AutoCloseable {
       }
     }
 
    @Override
    public boolean isLoaded() {
      return lazyInst != null;
    }

     @Override
     public T get() {
      if (inst != null) return inst;
      if (lazyInst != null) return lazyInst;
       if (solrException != null) throw solrException;
      createInst();
      registerMBean(inst, core, pluginInfo.name);
      return inst;
      if (createInst()) {
        // check if we created the instance to avoid registering it again
        registerMBean(lazyInst, core, pluginInfo.name);
      }
      return lazyInst;
     }
 
    protected synchronized void createInst() {
      if (inst != null) return;
    private synchronized boolean createInst() {
      if (lazyInst != null) return false;
       log.info("Going to create a new {} with {} ", pluginMeta.tag, pluginInfo.toString());
       if (resourceLoader instanceof MemClassLoader) {
         MemClassLoader loader = (MemClassLoader) resourceLoader;
         loader.loadJars();
       }
       Class<T> clazz = (Class<T>) pluginMeta.clazz;
      inst = core.createInstance(pluginInfo.className, clazz, pluginMeta.tag, null, resourceLoader);
      initInstance(inst, pluginInfo, core);
      if (inst instanceof SolrCoreAware) {
        SolrResourceLoader.assertAwareCompatibility(SolrCoreAware.class, inst);
        ((SolrCoreAware) inst).inform(core);
      T localInst = core.createInstance(pluginInfo.className, clazz, pluginMeta.tag, null, resourceLoader);
      initInstance(localInst, pluginInfo, core);
      if (localInst instanceof SolrCoreAware) {
        SolrResourceLoader.assertAwareCompatibility(SolrCoreAware.class, localInst);
        ((SolrCoreAware) localInst).inform(core);
       }
      if (inst instanceof ResourceLoaderAware) {
        SolrResourceLoader.assertAwareCompatibility(ResourceLoaderAware.class, inst);
      if (localInst instanceof ResourceLoaderAware) {
        SolrResourceLoader.assertAwareCompatibility(ResourceLoaderAware.class, localInst);
         try {
          ((ResourceLoaderAware) inst).inform(core.getResourceLoader());
          ((ResourceLoaderAware) localInst).inform(core.getResourceLoader());
         } catch (IOException e) {
           throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "error initializing component", e);
         }
       }
      lazyInst = localInst;  // only assign the volatile until after the plugin is completely ready to use
      return true;
     }
 
 
- 
2.19.1.windows.1

