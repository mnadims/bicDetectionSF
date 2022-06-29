From a8d1acd4bb34947ac49830e5a671e3f528ca2f7e Mon Sep 17 00:00:00 2001
From: phrocker <marc.parisi@gmail.com>
Date: Thu, 14 Jul 2016 14:42:31 -0400
Subject: [PATCH] ACCUMULO-4372 Fixes synchronization in CompressionTest

Also changed LZO so that creating a new codec doesn't always assign the new codec to the default variable

Signed-off-by: Josh Elser <elserj@apache.org>
--
 .../apache/accumulo/core/file/rfile/bcfile/Compression.java   | 3 +--
 .../accumulo/core/file/rfile/bcfile/CompressionTest.java      | 4 +++-
 2 files changed, 4 insertions(+), 3 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/Compression.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/Compression.java
index 3b8246241..fb0c0660a 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/Compression.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/Compression.java
@@ -155,8 +155,7 @@ public final class Compression {
           // the default defined within the codec
           if (bufferSize > 0)
             myConf.setInt(BUFFER_SIZE_OPT, bufferSize);
          codec = (CompressionCodec) ReflectionUtils.newInstance(Class.forName(clazz), myConf);
          return codec;
          return (CompressionCodec) ReflectionUtils.newInstance(Class.forName(clazz), myConf);
         } catch (ClassNotFoundException e) {
           // that is okay
         }
diff --git a/core/src/test/java/org/apache/accumulo/core/file/rfile/bcfile/CompressionTest.java b/core/src/test/java/org/apache/accumulo/core/file/rfile/bcfile/CompressionTest.java
index 961556424..683ad4850 100644
-- a/core/src/test/java/org/apache/accumulo/core/file/rfile/bcfile/CompressionTest.java
++ b/core/src/test/java/org/apache/accumulo/core/file/rfile/bcfile/CompressionTest.java
@@ -225,7 +225,9 @@ public class CompressionTest {
               CompressionCodec codec = al.getCodec();
               Assert.assertNotNull(al + " resulted in a non-null codec", codec);
               // add the identity hashcode to the set.
              testSet.add(System.identityHashCode(codec));
              synchronized (testSet) {
                testSet.add(System.identityHashCode(codec));
              }
               return true;
             }
           });
- 
2.19.1.windows.1

