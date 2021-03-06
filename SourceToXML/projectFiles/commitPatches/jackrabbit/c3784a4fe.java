From c3784a4fec6ac43759496885566b162d6c4a88bd Mon Sep 17 00:00:00 2001
From: Amit Jain <amitj@apache.org>
Date: Fri, 3 Mar 2017 08:09:00 +0000
Subject: [PATCH] JCR-4115: Don't use SHA-1 for new DataStore binaries
 (Jackrabbit)

 Using SHA-256 instead of SHA-1 to create blob record ids


git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1785225 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/data/DataStoreTest.java   | 32 ++++++++++++++++---
 .../core/data/AbstractDataStore.java          |  7 +++-
 .../core/data/CachingDataStore.java           | 11 ++-----
 .../jackrabbit/core/data/FileDataStore.java   |  9 ++----
 .../jackrabbit/core/data/db/DbDataStore.java  |  5 ---
 5 files changed, 39 insertions(+), 25 deletions(-)

diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/data/DataStoreTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/data/DataStoreTest.java
index c7f7ea021..d50c55cde 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/data/DataStoreTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/data/DataStoreTest.java
@@ -26,6 +26,9 @@ import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
 import java.sql.DriverManager;
 import java.sql.SQLException;
 import java.util.ArrayList;
@@ -103,14 +106,35 @@ public class DataStoreTest extends JUnitTest {
         }
     }
 
    public static void main(String... args) throws NoSuchAlgorithmException {
        // create and print a "directory-collision", that is, two byte arrays
        // where the hash starts with the same bytes
        // those values can be used for testDeleteRecordWithParentCollision
        HashMap<Long, Long> map = new HashMap<Long, Long>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer input = ByteBuffer.allocate(8);
        byte[] array = input.array();
        for(long x = 0;; x++) {
            input.putLong(x).flip();
            long h = ByteBuffer.wrap(digest.digest(array)).getLong();
            Long old = map.put(h & 0xffffffffff000000L, x);
            if (old != null) {
                System.out.println(Long.toHexString(old) + " " + Long.toHexString(x));
                break;
            }
        }
    }

     public void testDeleteRecordWithParentCollision() throws Exception {
         FileDataStore fds = new FileDataStore();
         fds.init(testDir + "/fileDeleteCollision");
 
        String c1 = "06b2f82fd81b2c20";
        String c2 = "02c60cb75083ceef";
        DataRecord d1 = fds.addRecord(IOUtils.toInputStream(c1));
        DataRecord d2 = fds.addRecord(IOUtils.toInputStream(c2));
        ByteArrayInputStream c1 = new ByteArrayInputStream(ByteBuffer
                .allocate(8).putLong(0x181c7).array());
        ByteArrayInputStream c2 = new ByteArrayInputStream(ByteBuffer
                .allocate(8).putLong(0x11fd78).array());
        DataRecord d1 = fds.addRecord(c1);
        DataRecord d2 = fds.addRecord(c2);
         fds.deleteRecord(d1.getIdentifier());
         DataRecord testRecord = fds.getRecordIfStored(d2.getIdentifier());
 
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/AbstractDataStore.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/AbstractDataStore.java
index 5d5725b5c..da28a75a5 100644
-- a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/AbstractDataStore.java
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/AbstractDataStore.java
@@ -36,6 +36,11 @@ public abstract class AbstractDataStore implements DataStore {
      */
     private static final char[] HEX = "0123456789abcdef".toCharArray();
 
    /**
     * The digest algorithm used to uniquely identify records.
     */
    protected String DIGEST = System.getProperty("ds.digest.algorithm", "SHA-256");

     /**
      * Cached copy of the reference key of this data store. Initialized in
      * {@link #getReferenceKey()} when the key is first accessed.
@@ -138,4 +143,4 @@ public abstract class AbstractDataStore implements DataStore {
         return referenceKey;
     }
 
}
\ No newline at end of file
}
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/CachingDataStore.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/CachingDataStore.java
index a15d3195e..eef22103a 100644
-- a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/CachingDataStore.java
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/CachingDataStore.java
@@ -90,11 +90,6 @@ public abstract class CachingDataStore extends AbstractDataStore implements
      */
     private static final Logger LOG = LoggerFactory.getLogger(CachingDataStore.class);
 
    /**
     * The digest algorithm used to uniquely identify records.
     */
    private static final String DIGEST = "SHA-1";

     private static final String DS_STORE = ".DS_Store";
 
     /**
@@ -389,9 +384,9 @@ public abstract class CachingDataStore extends AbstractDataStore implements
 
     /**
      * Creates a new data record in {@link Backend}. The stream is first
     * consumed and the contents are saved in a temporary file and the SHA-1
     * consumed and the contents are saved in a temporary file and the {@link #DIGEST}
      * message digest of the stream is calculated. If a record with the same
     * SHA-1 digest (and length) is found then it is returned. Otherwise new
     * {@link #DIGEST} digest (and length) is found then it is returned. Otherwise new
      * record is created in {@link Backend} and the temporary file is moved in
      * place to {@link LocalCache}.
      * 
@@ -423,7 +418,7 @@ public abstract class CachingDataStore extends AbstractDataStore implements
             long currTime = System.currentTimeMillis();
             DataIdentifier identifier = new DataIdentifier(
                 encodeHexString(digest.digest()));
            LOG.debug("SHA1 of [{}], length =[{}] took [{}]ms ",
            LOG.debug("Digest of [{}], length =[{}] took [{}]ms ",
                 new Object[] { identifier, length, (currTime - startTime) });
             String fileName = getFileName(identifier);
             AsyncUploadCacheResult result = null;
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/FileDataStore.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/FileDataStore.java
index c4c45be2f..3bd11ee17 100644
-- a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/FileDataStore.java
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/FileDataStore.java
@@ -68,11 +68,6 @@ public class FileDataStore extends AbstractDataStore
      */
     private static Logger log = LoggerFactory.getLogger(FileDataStore.class);
 
    /**
     * The digest algorithm used to uniquely identify records.
     */
    private static final String DIGEST = "SHA-1";

     /**
      * The default value for the minimum object size.
      */
@@ -163,8 +158,8 @@ public class FileDataStore extends AbstractDataStore
     /**
      * Creates a new data record.
      * The stream is first consumed and the contents are saved in a temporary file
     * and the SHA-1 message digest of the stream is calculated. If a
     * record with the same SHA-1 digest (and length) is found then it is
     * and the {@link #DIGEST} message digest of the stream is calculated. If a
     * record with the same {@link #DIGEST} digest (and length) is found then it is
      * returned. Otherwise the temporary file is moved in place to become
      * the new data record that gets returned.
      *
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/DbDataStore.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/DbDataStore.java
index ffd40c82b..714d5004c 100644
-- a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/DbDataStore.java
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/DbDataStore.java
@@ -125,11 +125,6 @@ public class DbDataStore extends AbstractDataStore
      */
     public static final String STORE_SIZE_MAX = "max";
 
    /**
     * The digest algorithm used to uniquely identify records.
     */
    protected static final String DIGEST = "SHA-1";

     /**
      * The prefix used for temporary objects.
      */
- 
2.19.1.windows.1

