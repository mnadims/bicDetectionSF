From 8f54a18e1a3af5d339e66e370c422033cf21915f Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@apache.org>
Date: Wed, 12 Dec 2012 18:40:58 +0000
Subject: [PATCH] LUCENE-4613: CompressingStoredFieldsFormat: fix .abort() when
 the segment suffix is not empty.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1420907 13f79535-47bb-0310-9956-ffa450edef68
--
 .../CompressingStoredFieldsFormat.java        | 24 ++++++++++--
 .../CompressingStoredFieldsWriter.java        |  6 ++-
 .../TestCompressingStoredFieldsFormat.java    | 37 +++++++++++++++++++
 .../codecs/compressing/CompressingCodec.java  | 37 +++++++++++++++----
 .../compressing/DummyCompressingCodec.java    |  8 ++--
 .../compressing/FastCompressingCodec.java     |  8 ++--
 .../FastDecompressionCompressingCodec.java    |  8 ++--
 .../HighCompressionCompressingCodec.java      |  8 ++--
 8 files changed, 111 insertions(+), 25 deletions(-)

diff --git a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsFormat.java b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsFormat.java
index 8e0237a4de4..d8cf4692cbf 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsFormat.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsFormat.java
@@ -48,9 +48,20 @@ import org.apache.lucene.store.IOContext;
 public class CompressingStoredFieldsFormat extends StoredFieldsFormat {
 
   private final String formatName;
  private final String segmentSuffix;
   private final CompressionMode compressionMode;
   private final int chunkSize;
 
  /**
   * Create a new {@link CompressingStoredFieldsFormat} with an empty segment 
   * suffix.
   * 
   * @see CompressingStoredFieldsFormat#CompressingStoredFieldsFormat(String, String, CompressionMode, int)
   */
  public CompressingStoredFieldsFormat(String formatName, CompressionMode compressionMode, int chunkSize) {
    this(formatName, "", compressionMode, chunkSize);
  }
  
   /**
    * Create a new {@link CompressingStoredFieldsFormat}.
    * <p>
@@ -58,6 +69,9 @@ public class CompressingStoredFieldsFormat extends StoredFieldsFormat {
    * in the file formats to perform
    * {@link CodecUtil#checkHeader(org.apache.lucene.store.DataInput, String, int, int) codec header checks}.
    * <p>
   * <code>segmentSuffix</code> is the segment suffix. This suffix is added to 
   * the result file name only if it's not the empty string.
   * <p>
    * The <code>compressionMode</code> parameter allows you to choose between
    * compression algorithms that have various compression and decompression
    * speeds so that you can pick the one that best fits your indexing and
@@ -81,25 +95,29 @@ public class CompressingStoredFieldsFormat extends StoredFieldsFormat {
    * @param chunkSize the minimum number of bytes of a single chunk of stored documents
    * @see CompressionMode
    */
  public CompressingStoredFieldsFormat(String formatName, CompressionMode compressionMode, int chunkSize) {
  public CompressingStoredFieldsFormat(String formatName, String segmentSuffix, 
                                       CompressionMode compressionMode, int chunkSize) {
     this.formatName = formatName;
    this.segmentSuffix = segmentSuffix;
     this.compressionMode = compressionMode;
     if (chunkSize < 1) {
       throw new IllegalArgumentException("chunkSize must be >= 1");
     }
     this.chunkSize = chunkSize;
    
   }
 
   @Override
   public StoredFieldsReader fieldsReader(Directory directory, SegmentInfo si,
       FieldInfos fn, IOContext context) throws IOException {
    return new CompressingStoredFieldsReader(directory, si, "", fn, context, formatName, compressionMode);
    return new CompressingStoredFieldsReader(directory, si, segmentSuffix, fn, 
        context, formatName, compressionMode);
   }
 
   @Override
   public StoredFieldsWriter fieldsWriter(Directory directory, SegmentInfo si,
       IOContext context) throws IOException {
    return new CompressingStoredFieldsWriter(directory, si, "", context,
    return new CompressingStoredFieldsWriter(directory, si, segmentSuffix, context,
         formatName, compressionMode, chunkSize);
   }
 
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsWriter.java b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsWriter.java
index 92233d71b23..d1bf861e9ea 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsWriter.java
@@ -70,6 +70,7 @@ public final class CompressingStoredFieldsWriter extends StoredFieldsWriter {
 
   private final Directory directory;
   private final String segment;
  private final String segmentSuffix;
   private CompressingStoredFieldsIndexWriter indexWriter;
   private IndexOutput fieldsStream;
 
@@ -89,6 +90,7 @@ public final class CompressingStoredFieldsWriter extends StoredFieldsWriter {
     assert directory != null;
     this.directory = directory;
     this.segment = si.name;
    this.segmentSuffix = segmentSuffix;
     this.compressionMode = compressionMode;
     this.compressor = compressionMode.newCompressor();
     this.chunkSize = chunkSize;
@@ -287,8 +289,8 @@ public final class CompressingStoredFieldsWriter extends StoredFieldsWriter {
   public void abort() {
     IOUtils.closeWhileHandlingException(this);
     IOUtils.deleteFilesIgnoringExceptions(directory,
        IndexFileNames.segmentFileName(segment, "", FIELDS_EXTENSION),
        IndexFileNames.segmentFileName(segment, "", FIELDS_INDEX_EXTENSION));
        IndexFileNames.segmentFileName(segment, segmentSuffix, FIELDS_EXTENSION),
        IndexFileNames.segmentFileName(segment, segmentSuffix, FIELDS_INDEX_EXTENSION));
   }
 
   @Override
diff --git a/lucene/core/src/test/org/apache/lucene/codecs/compressing/TestCompressingStoredFieldsFormat.java b/lucene/core/src/test/org/apache/lucene/codecs/compressing/TestCompressingStoredFieldsFormat.java
index fe7a8b854b4..0ed7cbea59c 100644
-- a/lucene/core/src/test/org/apache/lucene/codecs/compressing/TestCompressingStoredFieldsFormat.java
++ b/lucene/core/src/test/org/apache/lucene/codecs/compressing/TestCompressingStoredFieldsFormat.java
@@ -54,6 +54,7 @@ import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.util._TestUtil;
import org.junit.Test;
 
 import com.carrotsearch.randomizedtesting.generators.RandomInts;
 import com.carrotsearch.randomizedtesting.generators.RandomPicks;
@@ -362,5 +363,41 @@ public class TestCompressingStoredFieldsFormat extends LuceneTestCase {
     }
     rd.close();
   }
  
  @Test(expected=IllegalArgumentException.class)
  public void testDeletePartiallyWrittenFilesIfAbort() throws IOException {
    final Document validDoc = new Document();
    validDoc.add(new IntField("id", 0, Store.YES));
    iw.addDocument(validDoc);
    iw.commit();
    
    // make sure that #writeField will fail to trigger an abort
    final Document invalidDoc = new Document();
    FieldType fieldType = new FieldType();
    fieldType.setStored(true);
    invalidDoc.add(new Field("invalid", fieldType) {
      
      @Override
      public String stringValue() {
        return null;
      }
      
    });
    
    try {
      iw.addDocument(invalidDoc);
      iw.commit();
    }
    finally {
      int counter = 0;
      for (String fileName : dir.listAll()) {
        if (fileName.endsWith(".fdt") || fileName.endsWith(".fdx")) {
          counter++;
        }
      }
      // Only one .fdt and one .fdx files must have been found
      assertEquals(2, counter);
    }
  }
 
 }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/CompressingCodec.java b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/CompressingCodec.java
index e9eb48a400b..6a67dc34e56 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/CompressingCodec.java
++ b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/CompressingCodec.java
@@ -34,30 +34,51 @@ public abstract class CompressingCodec extends FilterCodec {
   /**
    * Create a random instance.
    */
  public static CompressingCodec randomInstance(Random random, int chunkSize) {
  public static CompressingCodec randomInstance(Random random, int chunkSize, boolean withSegmentSuffix) {
     switch (random.nextInt(4)) {
     case 0:
      return new FastCompressingCodec(chunkSize);
      return new FastCompressingCodec(chunkSize, withSegmentSuffix);
     case 1:
      return new FastDecompressionCompressingCodec(chunkSize);
      return new FastDecompressionCompressingCodec(chunkSize, withSegmentSuffix);
     case 2:
      return new HighCompressionCompressingCodec(chunkSize);
      return new HighCompressionCompressingCodec(chunkSize, withSegmentSuffix);
     case 3:
      return new DummyCompressingCodec(chunkSize);
      return new DummyCompressingCodec(chunkSize, withSegmentSuffix);
     default:
       throw new AssertionError();
     }
   }
 
  /**
   * Creates a random {@link CompressingCodec} that is using an empty segment 
   * suffix
   */
   public static CompressingCodec randomInstance(Random random) {
    return randomInstance(random, RandomInts.randomIntBetween(random, 1, 500));
    return randomInstance(random, RandomInts.randomIntBetween(random, 1, 500), false);
  }
  
  /**
   * Creates a random {@link CompressingCodec} that is using a segment suffix
   */
  public static CompressingCodec randomInstance(Random random, boolean withSegmentSuffix) {
    return randomInstance(random, RandomInts.randomIntBetween(random, 1, 500), withSegmentSuffix);
   }
 
   private final CompressingStoredFieldsFormat storedFieldsFormat;
 
  public CompressingCodec(String name, CompressionMode compressionMode, int chunkSize) {
  /**
   * Creates a compressing codec with a given segment suffix
   */
  public CompressingCodec(String name, String segmentSuffix, CompressionMode compressionMode, int chunkSize) {
     super(name, new Lucene41Codec());
    this.storedFieldsFormat = new CompressingStoredFieldsFormat(name, compressionMode, chunkSize);
    this.storedFieldsFormat = new CompressingStoredFieldsFormat(name, segmentSuffix, compressionMode, chunkSize);
  }
  
  /**
   * Creates a compressing codec with an empty segment suffix
   */
  public CompressingCodec(String name, CompressionMode compressionMode, int chunkSize) {
    this(name, "", compressionMode, chunkSize);
   }
 
   @Override
diff --git a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/DummyCompressingCodec.java b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/DummyCompressingCodec.java
index 9def78ff143..a989bd87365 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/DummyCompressingCodec.java
++ b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/DummyCompressingCodec.java
@@ -82,13 +82,15 @@ public class DummyCompressingCodec extends CompressingCodec {
   };
 
   /** Constructor that allows to configure the chunk size. */
  public DummyCompressingCodec(int chunkSize) {
    super("DummyCompressingStoredFields", DUMMY, chunkSize);
  public DummyCompressingCodec(int chunkSize, boolean withSegmentSuffix) {
    super("DummyCompressingStoredFields",
          withSegmentSuffix ? "DummyCompressingStoredFields" : "",
          DUMMY, chunkSize);
   }
 
   /** Default constructor. */
   public DummyCompressingCodec() {
    this(1 << 14);
    this(1 << 14, false);
   }
 
 }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/FastCompressingCodec.java b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/FastCompressingCodec.java
index 91bf277911e..252ba5d0fa3 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/FastCompressingCodec.java
++ b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/FastCompressingCodec.java
@@ -21,13 +21,15 @@ package org.apache.lucene.codecs.compressing;
 public class FastCompressingCodec extends CompressingCodec {
 
   /** Constructor that allows to configure the chunk size. */
  public FastCompressingCodec(int chunkSize) {
    super("FastCompressingStoredFields", CompressionMode.FAST, chunkSize);
  public FastCompressingCodec(int chunkSize, boolean withSegmentSuffix) {
    super("FastCompressingStoredFields", 
          withSegmentSuffix ? "FastCompressingStoredFields" : "",
          CompressionMode.FAST, chunkSize);
   }
 
   /** Default constructor. */
   public FastCompressingCodec() {
    this(1 << 14);
    this(1 << 14, false);
   }
 
 }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/FastDecompressionCompressingCodec.java b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/FastDecompressionCompressingCodec.java
index 25dc868e3e2..568a649289e 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/FastDecompressionCompressingCodec.java
++ b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/FastDecompressionCompressingCodec.java
@@ -21,13 +21,15 @@ package org.apache.lucene.codecs.compressing;
 public class FastDecompressionCompressingCodec extends CompressingCodec {
 
   /** Constructor that allows to configure the chunk size. */
  public FastDecompressionCompressingCodec(int chunkSize) {
    super("FastDecompressionCompressingStoredFields", CompressionMode.FAST_DECOMPRESSION, chunkSize);
  public FastDecompressionCompressingCodec(int chunkSize, boolean withSegmentSuffix) {
    super("FastDecompressionCompressingStoredFields",
          withSegmentSuffix ? "FastDecompressionCompressingStoredFields" : "",
          CompressionMode.FAST_DECOMPRESSION, chunkSize);
   }
 
   /** Default constructor. */
   public FastDecompressionCompressingCodec() {
    this(1 << 14);
    this(1 << 14, false);
   }
 
 }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/HighCompressionCompressingCodec.java b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/HighCompressionCompressingCodec.java
index abb518e0eef..fb235f95218 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/HighCompressionCompressingCodec.java
++ b/lucene/test-framework/src/java/org/apache/lucene/codecs/compressing/HighCompressionCompressingCodec.java
@@ -21,13 +21,15 @@ package org.apache.lucene.codecs.compressing;
 public class HighCompressionCompressingCodec extends CompressingCodec {
 
   /** Constructor that allows to configure the chunk size. */
  public HighCompressionCompressingCodec(int chunkSize) {
    super("HighCompressionCompressingStoredFields", CompressionMode.HIGH_COMPRESSION, chunkSize);
  public HighCompressionCompressingCodec(int chunkSize, boolean withSegmentSuffix) {
    super("HighCompressionCompressingStoredFields",
          withSegmentSuffix ? "HighCompressionCompressingStoredFields" : "",
          CompressionMode.HIGH_COMPRESSION, chunkSize);
   }
 
   /** Default constructor. */
   public HighCompressionCompressingCodec() {
    this(1 << 14);
    this(1 << 14, false);
   }
 
 }
- 
2.19.1.windows.1

