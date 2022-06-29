From 8d7f04cd96d435692726bf2e3dcdb025570e01e8 Mon Sep 17 00:00:00 2001
From: phrocker <marc.parisi@gmail.com>
Date: Wed, 24 Feb 2016 15:40:08 -0500
Subject: [PATCH] ACCUMULO-4153: Remove synchronization from Compression

Update the getCodec method to no longer be synchronized using static initializer in enum
Update so that we use a codec cache if we are not using the default buffer size for each specific codec. LZO does not need this change.
Update to improve comments and other readability concerns. Update tests to check
all codecs. Add checks for failures in executor. Instead of more unit
tests with Assume checks, we'll simply use a map and loop in existing
unit tests to check all codecs. A failure in one will cause a failure
--
 .../core/file/rfile/bcfile/Compression.java   | 306 ++++++++++++++----
 .../file/rfile/bcfile/CompressionTest.java    | 250 ++++++++++++++
 2 files changed, 497 insertions(+), 59 deletions(-)
 create mode 100644 core/src/test/java/org/apache/accumulo/core/file/rfile/bcfile/CompressionTest.java

diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/Compression.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/Compression.java
index 9defa1c63..3b8246241 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/Compression.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/Compression.java
@@ -23,6 +23,9 @@ import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
@@ -36,6 +39,11 @@ import org.apache.hadoop.io.compress.Decompressor;
 import org.apache.hadoop.io.compress.DefaultCodec;
 import org.apache.hadoop.util.ReflectionUtils;
 
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;

 /**
  * Compression related stuff.
  */
@@ -78,41 +86,90 @@ public final class Compression {
   public static final String COMPRESSION_NONE = "none";
 
   /**
   * Compression algorithms.
   * Compression algorithms. There is a static initializer, below the values defined in the enumeration, that calls the initializer of all defined codecs within
   * the Algorithm enum. This promotes a model of the following call graph of initialization by the static initializer, followed by calls to getCodec() and
   * createCompressionStream/DecompressionStream. In some cases, the compression and decompression call methods will include a different buffer size for the
   * stream. Note that if the compressed buffer size requested in these calls is zero, we will not set the buffer size for that algorithm. Instead, we will use
   * the default within the codec.
   *
   * The buffer size is configured in the Codec by way of a Hadoop Configuration reference. One approach may be to use the same Configuration object, but when
   * calls are made to createCompressionStream and DecompressionStream, with non default buffer sizes, the configuration object must be changed. In this case,
   * concurrent calls to createCompressionStream and DecompressionStream would mutate the configuration object beneath each other, requiring synchronization to
   * avoid undesirable activity via co-modification. To avoid synchronization entirely, we will create Codecs with their own Configuration object and cache them
   * for re-use. A default codec will be statically created, as mentioned above to ensure we always have a codec available at loader initialization.
   *
   * There is a Guava cache defined within Algorithm that allows us to cache Codecs for re-use. Since they will have their own configuration object and thus do
   * not need to be mutable, there is no concern for using them concurrently; however, the Guava cache exists to ensure a maximal size of the cache and
   * efficient and concurrent read/write access to the cache itself.
   *
   * To provide Algorithm specific details and to describe what is in code:
   *
   * LZO will always have the default LZO codec because the buffer size is never overridden within it.
   *
   * GZ will use the default GZ codec for the compression stream, but can potentially use a different codec instance for the decompression stream if the
   * requested buffer size does not match the default GZ buffer size of 32k.
   *
   * Snappy will use the default Snappy codec with the default buffer size of 64k for the compression stream, but will use a cached codec if the buffer size
   * differs from the default.
    */
   public static enum Algorithm {

     LZO(COMPRESSION_LZO) {
      private transient boolean checked = false;
      /**
       * determines if we've checked the codec status. ensures we don't recreate the defualt codec
       */
      private transient AtomicBoolean checked = new AtomicBoolean(false);
       private static final String defaultClazz = "org.apache.hadoop.io.compress.LzoCodec";
       private transient CompressionCodec codec = null;
 
      /**
       * Configuration option for LZO buffer size
       */
      private static final String BUFFER_SIZE_OPT = "io.compression.codec.lzo.buffersize";

      /**
       * Default buffer size
       */
      private static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

       @Override
      public synchronized boolean isSupported() {
        if (!checked) {
          checked = true;
          String extClazz = (conf.get(CONF_LZO_CLASS) == null ? System.getProperty(CONF_LZO_CLASS) : null);
          String clazz = (extClazz != null) ? extClazz : defaultClazz;
          try {
            LOG.info("Trying to load Lzo codec class: " + clazz);
            codec = (CompressionCodec) ReflectionUtils.newInstance(Class.forName(clazz), conf);
          } catch (ClassNotFoundException e) {
            // that is okay
          }
        }
      public boolean isSupported() {
         return codec != null;
       }
 
      public void initializeDefaultCodec() {
        if (!checked.get()) {
          checked.set(true);
          codec = createNewCodec(DEFAULT_BUFFER_SIZE);
        }
      }

       @Override
      CompressionCodec getCodec() throws IOException {
        if (!isSupported()) {
          throw new IOException("LZO codec class not specified. Did you forget to set property " + CONF_LZO_CLASS + "?");
      CompressionCodec createNewCodec(int bufferSize) {
        String extClazz = (conf.get(CONF_LZO_CLASS) == null ? System.getProperty(CONF_LZO_CLASS) : null);
        String clazz = (extClazz != null) ? extClazz : defaultClazz;
        try {
          LOG.info("Trying to load Lzo codec class: " + clazz);
          Configuration myConf = new Configuration(conf);
          // only use the buffersize if > 0, otherwise we'll use
          // the default defined within the codec
          if (bufferSize > 0)
            myConf.setInt(BUFFER_SIZE_OPT, bufferSize);
          codec = (CompressionCodec) ReflectionUtils.newInstance(Class.forName(clazz), myConf);
          return codec;
        } catch (ClassNotFoundException e) {
          // that is okay
         }
        return null;
      }
 
      @Override
      CompressionCodec getCodec() throws IOException {
         return codec;
       }
 
       @Override
      public synchronized InputStream createDecompressionStream(InputStream downStream, Decompressor decompressor, int downStreamBufferSize) throws IOException {
      public InputStream createDecompressionStream(InputStream downStream, Decompressor decompressor, int downStreamBufferSize) throws IOException {
         if (!isSupported()) {
           throw new IOException("LZO codec class not specified. Did you forget to set property " + CONF_LZO_CLASS + "?");
         }
@@ -122,14 +179,13 @@ public final class Compression {
         } else {
           bis1 = downStream;
         }
        conf.setInt("io.compression.codec.lzo.buffersize", 64 * 1024);
         CompressionInputStream cis = codec.createInputStream(bis1, decompressor);
         BufferedInputStream bis2 = new BufferedInputStream(cis, DATA_IBUF_SIZE);
         return bis2;
       }
 
       @Override
      public synchronized OutputStream createCompressionStream(OutputStream downStream, Compressor compressor, int downStreamBufferSize) throws IOException {
      public OutputStream createCompressionStream(OutputStream downStream, Compressor compressor, int downStreamBufferSize) throws IOException {
         if (!isSupported()) {
           throw new IOException("LZO codec class not specified. Did you forget to set property " + CONF_LZO_CLASS + "?");
         }
@@ -139,46 +195,82 @@ public final class Compression {
         } else {
           bos1 = downStream;
         }
        conf.setInt("io.compression.codec.lzo.buffersize", 64 * 1024);
         CompressionOutputStream cos = codec.createOutputStream(bos1, compressor);
         BufferedOutputStream bos2 = new BufferedOutputStream(new FinishOnFlushCompressionStream(cos), DATA_OBUF_SIZE);
         return bos2;
       }

     },
 
     GZ(COMPRESSION_GZ) {
      private transient DefaultCodec codec;
 
      @Override
      synchronized CompressionCodec getCodec() {
        if (codec == null) {
          codec = new DefaultCodec();
          codec.setConf(conf);
        }
      private transient DefaultCodec codec = null;

      /**
       * Configuration option for gz buffer size
       */
      private static final String BUFFER_SIZE_OPT = "io.file.buffer.size";

      /**
       * Default buffer size
       */
      private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
 
      @Override
      CompressionCodec getCodec() {
         return codec;
       }
 
       @Override
      public synchronized InputStream createDecompressionStream(InputStream downStream, Decompressor decompressor, int downStreamBufferSize) throws IOException {
      public void initializeDefaultCodec() {
        codec = (DefaultCodec) createNewCodec(DEFAULT_BUFFER_SIZE);
      }

      /**
       * Create a new GZ codec
       *
       * @param bufferSize
       *          buffer size to for GZ
       * @return created codec
       */
      protected CompressionCodec createNewCodec(final int bufferSize) {
        DefaultCodec myCodec = new DefaultCodec();
        Configuration myConf = new Configuration(conf);
        // only use the buffersize if > 0, otherwise we'll use
        // the default defined within the codec
        if (bufferSize > 0)
          myConf.setInt(BUFFER_SIZE_OPT, bufferSize);
        myCodec.setConf(myConf);
        return myCodec;
      }

      @Override
      public InputStream createDecompressionStream(InputStream downStream, Decompressor decompressor, int downStreamBufferSize) throws IOException {
         // Set the internal buffer size to read from down stream.
        if (downStreamBufferSize > 0) {
          codec.getConf().setInt("io.file.buffer.size", downStreamBufferSize);
        CompressionCodec decomCodec = codec;
        // if we're not using the default, let's pull from the loading cache
        if (DEFAULT_BUFFER_SIZE != downStreamBufferSize) {
          Entry<Algorithm,Integer> sizeOpt = Maps.immutableEntry(GZ, downStreamBufferSize);
          try {
            decomCodec = codecCache.get(sizeOpt);
          } catch (ExecutionException e) {
            throw new IOException(e);
          }
         }
        CompressionInputStream cis = codec.createInputStream(downStream, decompressor);
        CompressionInputStream cis = decomCodec.createInputStream(downStream, decompressor);
         BufferedInputStream bis2 = new BufferedInputStream(cis, DATA_IBUF_SIZE);
         return bis2;
       }
 
       @Override
      public synchronized OutputStream createCompressionStream(OutputStream downStream, Compressor compressor, int downStreamBufferSize) throws IOException {
      public OutputStream createCompressionStream(OutputStream downStream, Compressor compressor, int downStreamBufferSize) throws IOException {
         OutputStream bos1 = null;
         if (downStreamBufferSize > 0) {
           bos1 = new BufferedOutputStream(downStream, downStreamBufferSize);
         } else {
           bos1 = downStream;
         }
        codec.getConf().setInt("io.file.buffer.size", 32 * 1024);
        // always uses the default buffer size
         CompressionOutputStream cos = codec.createOutputStream(bos1, compressor);
         BufferedOutputStream bos2 = new BufferedOutputStream(new FinishOnFlushCompressionStream(cos), DATA_OBUF_SIZE);
         return bos2;
@@ -197,15 +289,23 @@ public final class Compression {
       }
 
       @Override
      public synchronized InputStream createDecompressionStream(InputStream downStream, Decompressor decompressor, int downStreamBufferSize) throws IOException {
      public InputStream createDecompressionStream(InputStream downStream, Decompressor decompressor, int downStreamBufferSize) throws IOException {
         if (downStreamBufferSize > 0) {
           return new BufferedInputStream(downStream, downStreamBufferSize);
         }
         return downStream;
       }
 
      public void initializeDefaultCodec() {

      }

      protected CompressionCodec createNewCodec(final int bufferSize) {
        return null;
      }

       @Override
      public synchronized OutputStream createCompressionStream(OutputStream downStream, Compressor compressor, int downStreamBufferSize) throws IOException {
      public OutputStream createCompressionStream(OutputStream downStream, Compressor compressor, int downStreamBufferSize) throws IOException {
         if (downStreamBufferSize > 0) {
           return new BufferedOutputStream(downStream, downStreamBufferSize);
         }
@@ -222,18 +322,65 @@ public final class Compression {
     SNAPPY(COMPRESSION_SNAPPY) {
       // Use base type to avoid compile-time dependencies.
       private transient CompressionCodec snappyCodec = null;
      private transient boolean checked = false;
      /**
       * determines if we've checked the codec status. ensures we don't recreate the defualt codec
       */
      private transient AtomicBoolean checked = new AtomicBoolean(false);
       private static final String defaultClazz = "org.apache.hadoop.io.compress.SnappyCodec";
 
      /**
       * Buffer size option
       */
      private static final String BUFFER_SIZE_OPT = "io.compression.codec.snappy.buffersize";

      /**
       * Default buffer size value
       */
      private static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

       public CompressionCodec getCodec() throws IOException {
        if (!isSupported()) {
          throw new IOException("SNAPPY codec class not specified. Did you forget to set property " + CONF_SNAPPY_CLASS + "?");
        }
         return snappyCodec;
       }
 
       @Override
      public synchronized OutputStream createCompressionStream(OutputStream downStream, Compressor compressor, int downStreamBufferSize) throws IOException {
      public void initializeDefaultCodec() {
        if (!checked.get()) {
          checked.set(true);
          snappyCodec = createNewCodec(DEFAULT_BUFFER_SIZE);
        }
      }

      /**
       * Creates a new snappy codec.
       *
       * @param bufferSize
       *          incoming buffer size
       * @return new codec or null, depending on if installed
       */
      protected CompressionCodec createNewCodec(final int bufferSize) {

        String extClazz = (conf.get(CONF_SNAPPY_CLASS) == null ? System.getProperty(CONF_SNAPPY_CLASS) : null);
        String clazz = (extClazz != null) ? extClazz : defaultClazz;
        try {
          LOG.info("Trying to load snappy codec class: " + clazz);

          Configuration myConf = new Configuration(conf);
          // only use the buffersize if > 0, otherwise we'll use
          // the default defined within the codec
          if (bufferSize > 0)
            myConf.setInt(BUFFER_SIZE_OPT, bufferSize);

          return (CompressionCodec) ReflectionUtils.newInstance(Class.forName(clazz), myConf);

        } catch (ClassNotFoundException e) {
          // that is okay
        }

        return null;
      }

      @Override
      public OutputStream createCompressionStream(OutputStream downStream, Compressor compressor, int downStreamBufferSize) throws IOException {
 
         if (!isSupported()) {
           throw new IOException("SNAPPY codec class not specified. Did you forget to set property " + CONF_SNAPPY_CLASS + "?");
@@ -244,44 +391,71 @@ public final class Compression {
         } else {
           bos1 = downStream;
         }
        conf.setInt("io.compression.codec.snappy.buffersize", 64 * 1024);
        // use the default codec
         CompressionOutputStream cos = snappyCodec.createOutputStream(bos1, compressor);
         BufferedOutputStream bos2 = new BufferedOutputStream(new FinishOnFlushCompressionStream(cos), DATA_OBUF_SIZE);
         return bos2;
       }
 
       @Override
      public synchronized InputStream createDecompressionStream(InputStream downStream, Decompressor decompressor, int downStreamBufferSize) throws IOException {
      public InputStream createDecompressionStream(InputStream downStream, Decompressor decompressor, int downStreamBufferSize) throws IOException {
         if (!isSupported()) {
           throw new IOException("SNAPPY codec class not specified. Did you forget to set property " + CONF_SNAPPY_CLASS + "?");
         }
        if (downStreamBufferSize > 0) {
          conf.setInt("io.file.buffer.size", downStreamBufferSize);

        CompressionCodec decomCodec = snappyCodec;
        // if we're not using the same buffer size, we'll pull the codec from the loading cache
        if (DEFAULT_BUFFER_SIZE != downStreamBufferSize) {
          Entry<Algorithm,Integer> sizeOpt = Maps.immutableEntry(SNAPPY, downStreamBufferSize);
          try {
            decomCodec = codecCache.get(sizeOpt);
          } catch (ExecutionException e) {
            throw new IOException(e);
          }
         }
        CompressionInputStream cis = snappyCodec.createInputStream(downStream, decompressor);

        CompressionInputStream cis = decomCodec.createInputStream(downStream, decompressor);
         BufferedInputStream bis2 = new BufferedInputStream(cis, DATA_IBUF_SIZE);
         return bis2;
       }
 
       @Override
      public synchronized boolean isSupported() {
        if (!checked) {
          checked = true;
          String extClazz = (conf.get(CONF_SNAPPY_CLASS) == null ? System.getProperty(CONF_SNAPPY_CLASS) : null);
          String clazz = (extClazz != null) ? extClazz : defaultClazz;
          try {
            LOG.info("Trying to load snappy codec class: " + clazz);
            snappyCodec = (CompressionCodec) ReflectionUtils.newInstance(Class.forName(clazz), conf);
          } catch (ClassNotFoundException e) {
            // that is okay
          }
        }
      public boolean isSupported() {

         return snappyCodec != null;
       }
     };

    /**
     * The model defined by the static block, below, creates a singleton for each defined codec in the Algorithm enumeration. By creating the codecs, each call
     * to isSupported shall return true/false depending on if the codec singleton is defined. The static initializer, below, will ensure this occurs when the
     * Enumeration is loaded. Furthermore, calls to getCodec will return the singleton, whether it is null or not.
     *
     * Calls to createCompressionStream and createDecompressionStream may return a different codec than getCodec, if the incoming downStreamBufferSize is
     * different than the default. In such a case, we will place the resulting codec into the codecCache, defined below, to ensure we have cache codecs.
     *
     * Since codecs are immutable, there is no concern about concurrent access to the CompressionCodec objects within the guava cache.
     */
    static {
      conf = new Configuration();
      for (final Algorithm al : Algorithm.values()) {
        al.initializeDefaultCodec();
      }
    }

    /**
     * Guava cache to have a limited factory pattern defined in the Algorithm enum.
     */
    private static LoadingCache<Entry<Algorithm,Integer>,CompressionCodec> codecCache = CacheBuilder.newBuilder().maximumSize(25)
        .build(new CacheLoader<Entry<Algorithm,Integer>,CompressionCodec>() {
          public CompressionCodec load(Entry<Algorithm,Integer> key) {
            return key.getKey().createNewCodec(key.getValue());
          }
        });

     // We require that all compression related settings are configured
     // statically in the Configuration object.
    protected static final Configuration conf = new Configuration();
    protected static final Configuration conf;
     private final String compressName;
     // data input buffer size to absorb small reads from application.
     private static final int DATA_IBUF_SIZE = 1 * 1024;
@@ -296,6 +470,20 @@ public final class Compression {
 
     abstract CompressionCodec getCodec() throws IOException;
 
    /**
     * function to create the default codec object.
     */
    abstract void initializeDefaultCodec();

    /**
     * Shared function to create new codec objects. It is expected that if buffersize is invalid, a codec will be created with the default buffer size
     *
     * @param bufferSize
     *          configured buffer size.
     * @return new codec
     */
    abstract CompressionCodec createNewCodec(int bufferSize);

     public abstract InputStream createDecompressionStream(InputStream downStream, Decompressor decompressor, int downStreamBufferSize) throws IOException;
 
     public abstract OutputStream createCompressionStream(OutputStream downStream, Compressor compressor, int downStreamBufferSize) throws IOException;
diff --git a/core/src/test/java/org/apache/accumulo/core/file/rfile/bcfile/CompressionTest.java b/core/src/test/java/org/apache/accumulo/core/file/rfile/bcfile/CompressionTest.java
new file mode 100644
index 000000000..961556424
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/file/rfile/bcfile/CompressionTest.java
@@ -0,0 +1,250 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.accumulo.core.file.rfile.bcfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.file.rfile.bcfile.Compression.Algorithm;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CompressionTest {

  HashMap<Compression.Algorithm,Boolean> isSupported = Maps.newHashMap();

  @Before
  public void testSupport() {
    // we can safely assert that GZ exists by virtue of it being the DefaultCodec
    isSupported.put(Compression.Algorithm.GZ, true);

    Configuration myConf = new Configuration();

    String extClazz = System.getProperty(Compression.Algorithm.CONF_LZO_CLASS);
    String clazz = (extClazz != null) ? extClazz : "org.apache.hadoop.io.compress.LzoCodec";
    try {
      CompressionCodec codec = (CompressionCodec) ReflectionUtils.newInstance(Class.forName(clazz), myConf);

      Assert.assertNotNull(codec);
      isSupported.put(Compression.Algorithm.LZO, true);

    } catch (ClassNotFoundException e) {
      // that is okay
    }

    extClazz = System.getProperty(Compression.Algorithm.CONF_SNAPPY_CLASS);
    clazz = (extClazz != null) ? extClazz : "org.apache.hadoop.io.compress.SnappyCodec";
    try {
      CompressionCodec codec = (CompressionCodec) ReflectionUtils.newInstance(Class.forName(clazz), myConf);

      Assert.assertNotNull(codec);

      isSupported.put(Compression.Algorithm.SNAPPY, true);

    } catch (ClassNotFoundException e) {
      // that is okay
    }

  }

  @Test
  public void testSingle() throws IOException {

    for (final Algorithm al : Algorithm.values()) {
      if (isSupported.get(al) != null && isSupported.get(al) == true) {

        // first call to issupported should be true
        Assert.assertTrue(al + " is not supported, but should be", al.isSupported());

        Assert.assertNotNull(al + " should have a non-null codec", al.getCodec());

        Assert.assertNotNull(al + " should have a non-null codec", al.getCodec());
      }
    }
  }

  @Test
  public void testSingleNoSideEffect() throws IOException {

    for (final Algorithm al : Algorithm.values()) {
      if (isSupported.get(al) != null && isSupported.get(al) == true) {

        Assert.assertTrue(al + " is not supported, but should be", al.isSupported());

        Assert.assertNotNull(al + " should have a non-null codec", al.getCodec());

        // assert that additional calls to create will not create
        // additional codecs

        Assert.assertNotEquals(al + " should have created a new codec, but did not", System.identityHashCode(al.getCodec()), al.createNewCodec(88 * 1024));
      }
    }
  }

  @Test(timeout = 60 * 1000)
  public void testManyStartNotNull() throws IOException, InterruptedException, ExecutionException {

    for (final Algorithm al : Algorithm.values()) {
      if (isSupported.get(al) != null && isSupported.get(al) == true) {

        // first call to issupported should be true
        Assert.assertTrue(al + " is not supported, but should be", al.isSupported());

        final CompressionCodec codec = al.getCodec();

        Assert.assertNotNull(al + " should not be null", codec);

        ExecutorService service = Executors.newFixedThreadPool(10);

        ArrayList<Future<Boolean>> results = Lists.newArrayList();

        for (int i = 0; i < 30; i++) {
          results.add(service.submit(new Callable<Boolean>()

          {

            @Override
            public Boolean call() throws Exception {
              Assert.assertNotNull(al + " should not be null", al.getCodec());
              return true;
            }

          }));
        }

        service.shutdown();

        Assert.assertNotNull(al + " should not be null", codec);

        while (!service.awaitTermination(1, TimeUnit.SECONDS)) {
          // wait
        }

        for (Future<Boolean> result : results) {
          Assert.assertTrue(al + " resulted in a failed call to getcodec within the thread pool", result.get());
        }
      }
    }

  }

  // don't start until we have created the codec
  @Test(timeout = 60 * 1000)
  public void testManyDontStartUntilThread() throws IOException, InterruptedException, ExecutionException {

    for (final Algorithm al : Algorithm.values()) {
      if (isSupported.get(al) != null && isSupported.get(al) == true) {

        // first call to issupported should be true
        Assert.assertTrue(al + " is not supported, but should be", al.isSupported());

        ExecutorService service = Executors.newFixedThreadPool(10);

        ArrayList<Future<Boolean>> results = Lists.newArrayList();

        for (int i = 0; i < 30; i++) {

          results.add(service.submit(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
              Assert.assertNotNull(al + " should have a non-null codec", al.getCodec());
              return true;
            }

          }));
        }

        service.shutdown();

        while (!service.awaitTermination(1, TimeUnit.SECONDS)) {
          // wait
        }

        for (Future<Boolean> result : results) {
          Assert.assertTrue(al + " resulted in a failed call to getcodec within the thread pool", result.get());
        }
      }
    }

  }

  @Test(timeout = 60 * 1000)
  public void testThereCanBeOnlyOne() throws IOException, InterruptedException, ExecutionException {

    for (final Algorithm al : Algorithm.values()) {
      if (isSupported.get(al) != null && isSupported.get(al) == true) {

        // first call to issupported should be true
        Assert.assertTrue(al + " is not supported, but should be", al.isSupported());

        ExecutorService service = Executors.newFixedThreadPool(20);

        ArrayList<Callable<Boolean>> list = Lists.newArrayList();

        ArrayList<Future<Boolean>> results = Lists.newArrayList();

        // keep track of the system's identity hashcodes.
        final HashSet<Integer> testSet = Sets.newHashSet();

        for (int i = 0; i < 40; i++) {
          list.add(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
              CompressionCodec codec = al.getCodec();
              Assert.assertNotNull(al + " resulted in a non-null codec", codec);
              // add the identity hashcode to the set.
              testSet.add(System.identityHashCode(codec));
              return true;
            }
          });
        }

        results.addAll(service.invokeAll(list));
        // ensure that we
        Assert.assertEquals(al + " created too many codecs", 1, testSet.size());
        service.shutdown();

        while (!service.awaitTermination(1, TimeUnit.SECONDS)) {
          // wait
        }

        for (Future<Boolean> result : results) {
          Assert.assertTrue(al + " resulted in a failed call to getcodec within the thread pool", result.get());
        }
      }
    }
  }

}
- 
2.19.1.windows.1

