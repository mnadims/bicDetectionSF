From b549619dc870704be02ce1c67c32002368e2d369 Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@apache.org>
Date: Mon, 10 Dec 2012 13:42:02 +0000
Subject: [PATCH] LUCENE-4591: Make CompressingStoredFields{Writer,Reader}
 accept a segment suffix as a constructor parameter.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1419449 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                               |  3 +++
 .../CompressingStoredFieldsFormat.java           |  4 ++--
 .../CompressingStoredFieldsReader.java           | 16 ++++++++++++----
 .../CompressingStoredFieldsWriter.java           | 13 +++++++++----
 4 files changed, 26 insertions(+), 10 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index a3e5ba0f011..a1e69d91cfa 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -155,6 +155,9 @@ API Changes
 * LUCENE-4537: RateLimiter is now separated from FSDirectory and exposed via
   RateLimitingDirectoryWrapper. Any Directory can now be rate-limited.
   (Simon Willnauer)  

* LUCENE-4591: CompressingStoredFields{Writer,Reader} now accept a segment
  suffix as a constructor parameter. (Renaud Delbru via Adrien Grand)
   
 Bug Fixes
 
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsFormat.java b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsFormat.java
index a6a90df160a..8e0237a4de4 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsFormat.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsFormat.java
@@ -93,13 +93,13 @@ public class CompressingStoredFieldsFormat extends StoredFieldsFormat {
   @Override
   public StoredFieldsReader fieldsReader(Directory directory, SegmentInfo si,
       FieldInfos fn, IOContext context) throws IOException {
    return new CompressingStoredFieldsReader(directory, si, fn, context, formatName, compressionMode);
    return new CompressingStoredFieldsReader(directory, si, "", fn, context, formatName, compressionMode);
   }
 
   @Override
   public StoredFieldsWriter fieldsWriter(Directory directory, SegmentInfo si,
       IOContext context) throws IOException {
    return new CompressingStoredFieldsWriter(directory, si, context,
    return new CompressingStoredFieldsWriter(directory, si, "", context,
         formatName, compressionMode, chunkSize);
   }
 
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsReader.java b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsReader.java
index 695aca51e0e..797cdb7b6fc 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsReader.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsReader.java
@@ -54,7 +54,11 @@ import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.packed.PackedInts;
 
final class CompressingStoredFieldsReader extends StoredFieldsReader {
/**
 * {@link StoredFieldsReader} impl for {@link CompressingStoredFieldsFormat}.
 * @lucene.experimental
 */
public final class CompressingStoredFieldsReader extends StoredFieldsReader {
 
   private final FieldInfos fieldInfos;
   private final CompressingStoredFieldsIndexReader indexReader;
@@ -79,7 +83,8 @@ final class CompressingStoredFieldsReader extends StoredFieldsReader {
     this.closed = false;
   }
 
  public CompressingStoredFieldsReader( Directory d, SegmentInfo si, FieldInfos fn,
  /** Sole constructor. */
  public CompressingStoredFieldsReader(Directory d, SegmentInfo si, String segmentSuffix, FieldInfos fn,
       IOContext context, String formatName, CompressionMode compressionMode) throws IOException {
     this.compressionMode = compressionMode;
     final String segment = si.name;
@@ -88,8 +93,8 @@ final class CompressingStoredFieldsReader extends StoredFieldsReader {
     numDocs = si.getDocCount();
     IndexInput indexStream = null;
     try {
      fieldsStream = d.openInput(IndexFileNames.segmentFileName(segment, "", FIELDS_EXTENSION), context);
      final String indexStreamFN = IndexFileNames.segmentFileName(segment, "", FIELDS_INDEX_EXTENSION);
      fieldsStream = d.openInput(IndexFileNames.segmentFileName(segment, segmentSuffix, FIELDS_EXTENSION), context);
      final String indexStreamFN = IndexFileNames.segmentFileName(segment, segmentSuffix, FIELDS_INDEX_EXTENSION);
       indexStream = d.openInput(indexStreamFN, context);
 
       final String codecNameIdx = formatName + CODEC_SFX_IDX;
@@ -123,6 +128,9 @@ final class CompressingStoredFieldsReader extends StoredFieldsReader {
     }
   }
 
  /** 
   * Close the underlying {@link IndexInput}s.
   */
   @Override
   public void close() throws IOException {
     if (!closed) {
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsWriter.java b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsWriter.java
index f4b6f826ec4..92233d71b23 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/compressing/CompressingStoredFieldsWriter.java
@@ -47,7 +47,11 @@ import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.packed.PackedInts;
 
final class CompressingStoredFieldsWriter extends StoredFieldsWriter {
/**
 * {@link StoredFieldsWriter} impl for {@link CompressingStoredFieldsFormat}.
 * @lucene.experimental
 */
public final class CompressingStoredFieldsWriter extends StoredFieldsWriter {
 
   static final int         STRING = 0x00;
   static final int       BYTE_ARR = 0x01;
@@ -79,7 +83,8 @@ final class CompressingStoredFieldsWriter extends StoredFieldsWriter {
   private int docBase; // doc ID at the beginning of the chunk
   private int numBufferedDocs; // docBase + numBufferedDocs == current doc ID
 
  public CompressingStoredFieldsWriter(Directory directory, SegmentInfo si, IOContext context,
  /** Sole constructor. */
  public CompressingStoredFieldsWriter(Directory directory, SegmentInfo si, String segmentSuffix, IOContext context,
       String formatName, CompressionMode compressionMode, int chunkSize) throws IOException {
     assert directory != null;
     this.directory = directory;
@@ -94,9 +99,9 @@ final class CompressingStoredFieldsWriter extends StoredFieldsWriter {
     this.numBufferedDocs = 0;
 
     boolean success = false;
    IndexOutput indexStream = directory.createOutput(IndexFileNames.segmentFileName(segment, "", FIELDS_INDEX_EXTENSION), context);
    IndexOutput indexStream = directory.createOutput(IndexFileNames.segmentFileName(segment, segmentSuffix, FIELDS_INDEX_EXTENSION), context);
     try {
      fieldsStream = directory.createOutput(IndexFileNames.segmentFileName(segment, "", FIELDS_EXTENSION), context);
      fieldsStream = directory.createOutput(IndexFileNames.segmentFileName(segment, segmentSuffix, FIELDS_EXTENSION), context);
 
       final String codecNameIdx = formatName + CODEC_SFX_IDX;
       final String codecNameDat = formatName + CODEC_SFX_DAT;
- 
2.19.1.windows.1

