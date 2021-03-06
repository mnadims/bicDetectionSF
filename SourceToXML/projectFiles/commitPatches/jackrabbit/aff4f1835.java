From aff4f1835d3b4c60485537dd47516980529661cf Mon Sep 17 00:00:00 2001
From: amitj <amitj@unknown>
Date: Tue, 2 Sep 2014 07:01:35 +0000
Subject: [PATCH] JCR-3805 - LocalCache doesn't build up properly in JDK 7
 Patch from Shashank Gupta.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1621933 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/data/LocalCache.java      | 62 +++++++-------
 .../jackrabbit/core/data/TestLocalCache.java  | 80 +++++++++++++++++--
 2 files changed, 109 insertions(+), 33 deletions(-)

diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/LocalCache.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/LocalCache.java
index f6de89bf2..3133f7086 100644
-- a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/LocalCache.java
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/LocalCache.java
@@ -29,11 +29,10 @@ import java.util.Comparator;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
import javax.jcr.RepositoryException;

 import org.apache.commons.io.FileUtils;
 import org.apache.commons.io.IOUtils;
 import org.apache.jackrabbit.util.TransientFileFactory;
@@ -95,22 +94,39 @@ public class LocalCache {
      * @param cachePurgeResizeFactor after cache purge size of cache will be
      * just less (cachePurgeResizeFactor * maxSizeInBytes).
      * @param asyncUploadCache {@link AsyncUploadCache}
     * @throws IOException
     * @throws java.lang.ClassNotFoundException
      */
     public LocalCache(String path, String tmpPath, long maxSizeInBytes, double cachePurgeTrigFactor,
            double cachePurgeResizeFactor, AsyncUploadCache asyncUploadCache) throws IOException,
            ClassNotFoundException {
            double cachePurgeResizeFactor, AsyncUploadCache asyncUploadCache) {
         directory = new File(path);
         tmp = new File(tmpPath);
         LOG.info(
            "cachePurgeTrigFactor =[{}], cachePurgeResizeFactor =[{}],  cachePurgeTrigFactorSize =[{}], cachePurgeResizeFactorSize =[{}]",
            "cachePurgeTrigFactor =[{}], cachePurgeResizeFactor =[{}],  " +
            "cachePurgeTrigFactorSize =[{}], cachePurgeResizeFactorSize =[{}]",
             new Object[] { cachePurgeTrigFactor, cachePurgeResizeFactor,
                (cachePurgeTrigFactor * maxSizeInBytes), (cachePurgeResizeFactor * maxSizeInBytes) });
                (cachePurgeTrigFactor * maxSizeInBytes), 
                (cachePurgeResizeFactor * maxSizeInBytes) });
         cache = new LRUCache(maxSizeInBytes, cachePurgeTrigFactor, cachePurgeResizeFactor);
         this.asyncUploadCache = asyncUploadCache;

        new Thread(new CacheBuildJob()).start();
        long startTime = System.currentTimeMillis();
        ArrayList<File> allFiles = new ArrayList<File>();
        Iterator<File> it = FileUtils.iterateFiles(directory, null, true);
        while (it.hasNext()) {
            File f = it.next();
            allFiles.add(f);
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("Time taken to recursive [{}] took [{}] sec",
            allFiles.size(), ((t1 - startTime) / 1000));
        Collections.sort(allFiles, new Comparator<File>() {
            public int compare(File o1, File o2) {
                long l1 = o1.lastModified(), l2 = o2.lastModified();
                return l1 < l2 ? -1 : l1 > l2 ? 1 : 0;
            }
        });
        long t2 = System.currentTimeMillis();
        LOG.debug("Time taken to sort [{}] took [{}] sec",
            allFiles.size(), ((t2 - t1) / 1000));
        new Thread(new CacheBuildJob(allFiles)).start();
     }
 
     /**
@@ -544,27 +560,17 @@ public class LocalCache {
      * asynchronously.
      */
     private class CacheBuildJob implements Runnable {

        List<File> allFiles ;
        
        private CacheBuildJob(List<File> allFiles) {
            this.allFiles = allFiles;
        }
         public void run() {
             long startTime = System.currentTimeMillis();
            ArrayList<File> allFiles = new ArrayList<File>();
            Iterator<File> it = FileUtils.iterateFiles(directory, null, true);
            while (it.hasNext()) {
                File f = it.next();
                allFiles.add(f);
            }
            long t1 = System.currentTimeMillis();
            LOG.debug("Time taken to recursive [{}] took [{}] sec",
                allFiles.size(), ((t1 - startTime) / 1000));
            Collections.sort(allFiles, new Comparator<File>() {
                public int compare(File o1, File o2) {
                    long l1 = o1.lastModified(), l2 = o2.lastModified();
                    return l1 < l2 ? -1 : l1 > l2 ? 1 : 0;
                }
            });
            long t2 = System.currentTimeMillis();
            LOG.debug("Time taken to sort [{}] took [{}] sec",
                allFiles.size(), ((t2 - t1) / 1000));
             String dataStorePath = directory.getAbsolutePath();
            LOG.info("directoryPath = " + dataStorePath);
            dataStorePath = dataStorePath.replace("\\", "/");
             String tmpPath = tmp.getAbsolutePath();
             LOG.debug("tmp path [{}]", tmpPath); 
             long time = System.currentTimeMillis();
diff --git a/jackrabbit-data/src/test/java/org/apache/jackrabbit/core/data/TestLocalCache.java b/jackrabbit-data/src/test/java/org/apache/jackrabbit/core/data/TestLocalCache.java
index 705cc9fbe..46008705b 100644
-- a/jackrabbit-data/src/test/java/org/apache/jackrabbit/core/data/TestLocalCache.java
++ b/jackrabbit-data/src/test/java/org/apache/jackrabbit/core/data/TestLocalCache.java
@@ -24,12 +24,14 @@ import java.io.InputStream;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
 
 import junit.framework.TestCase;
 
import org.apache.jackrabbit.core.data.AsyncUploadCache;
import org.apache.jackrabbit.core.data.AsyncUploadCacheResult;
import org.apache.jackrabbit.core.data.LocalCache;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.util.NamedThreadFactory;
 import org.apache.jackrabbit.core.fs.local.FileUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -66,10 +68,14 @@ public class TestLocalCache extends TestCase {
     @Override
     protected void tearDown() throws IOException {
         File cachedir = new File(CACHE_DIR);
        if (cachedir.exists()) FileUtil.delete(cachedir);
        if (cachedir.exists()) {
            FileUtils.deleteQuietly(cachedir);
        }
 
         File tempdir = new File(TEMP_DIR);
        if (tempdir.exists()) FileUtil.delete(tempdir);
        if (tempdir.exists()) {
            FileUtils.deleteQuietly(tempdir);
        }
     }
 
     /**
@@ -273,6 +279,70 @@ public class TestLocalCache extends TestCase {
         }
     }
 
    /**
     * Test concurrent {@link LocalCache} initialization with storing
     * {@link LocalCache}
     */
    public void testConcurrentInitWithStore() {
        try {
            AsyncUploadCache pendingFiles = new AsyncUploadCache();
            pendingFiles.init(TARGET_DIR, CACHE_DIR, 100);
            pendingFiles.reset();
            LocalCache cache = new LocalCache(CACHE_DIR, TEMP_DIR, 10000000,
                0.95, 0.70, pendingFiles);
            Random random = new Random(12345);
            int fileUploads = 1000;
            Map<String, byte[]> byteMap = new HashMap<String, byte[]>(
                fileUploads);
            byte[] data;
            for (int i = 0; i < fileUploads; i++) {
                data = new byte[100];
                random.nextBytes(data);
                String key = "a" + i;
                byteMap.put(key, data);
                cache.store(key, new ByteArrayInputStream(byteMap.get(key)));
            }
            ExecutorService executor = Executors.newFixedThreadPool(10,
                new NamedThreadFactory("localcache-store-worker"));
            cache = new LocalCache(CACHE_DIR, TEMP_DIR, 10000000, 0.95, 0.70,
                pendingFiles);
            executor.execute(new StoreWorker(cache, byteMap));
            executor.shutdown();
            while (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
            }
        } catch (Exception e) {
            LOG.error("error:", e);
            fail();
        }
    }


    private class StoreWorker implements Runnable {
        Map<String, byte[]> byteMap;

        LocalCache cache;

        Random random;

        private StoreWorker(LocalCache cache, Map<String, byte[]> byteMap) {
            this.byteMap = byteMap;
            this.cache = cache;
            random = new Random(byteMap.size());
        }

        public void run() {
            try {
                for (int i = 0; i < 100; i++) {
                    String key = "a" + random.nextInt(byteMap.size());
                    LOG.debug("key=" + key);
                    cache.store(key, new ByteArrayInputStream(byteMap.get(key)));
                }
            } catch (Exception e) {
                LOG.error("error:", e);
                fail();
            }
        }
    }
     /**
      * Assert two inputstream
      */
- 
2.19.1.windows.1

