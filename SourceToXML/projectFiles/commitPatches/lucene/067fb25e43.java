From 067fb25e4359ed8d5673e385976da7debc0e5b77 Mon Sep 17 00:00:00 2001
From: Mike McCandless <mikemccand@apache.org>
Date: Thu, 9 Jun 2016 14:48:58 -0400
Subject: [PATCH] LUCENE-7323: compound file writing now verifies checksum and
 segment ID for the incoming sub-files, to catch hardware issues or filesystem
 bugs earlier

--
 lucene/CHANGES.txt                            |   6 +
 .../simpletext/SimpleTextDocValuesFormat.java |   2 +-
 .../simpletext/SimpleTextPostingsFormat.java  |   2 +-
 .../org.apache.lucene.codecs.DocValuesFormat  |   1 -
 .../org.apache.lucene.codecs.PostingsFormat   |   1 -
 .../TestSimpleTextCompoundFormat.java         |  10 ++
 .../org/apache/lucene/codecs/CodecUtil.java   |  55 +++++-
 .../apache/lucene/codecs/CompoundFormat.java  |   4 +-
 .../lucene50/Lucene50CompoundFormat.java      |  26 ++-
 .../lucene50/Lucene50CompoundReader.java      |   5 +-
 .../{index => codecs}/TestCodecUtil.java      |   3 +-
 .../perfield/TestPerFieldDocValuesFormat.java |   2 +-
 .../perfield/TestPerFieldPostingsFormat2.java |  12 +-
 .../apache/lucene/index/TestAddIndexes.java   |   4 +-
 .../mockrandom/MockRandomPostingsFormat.java  |  13 +-
 .../index/BaseCompoundFormatTestCase.java     | 161 ++++++++++++------
 .../org/apache/lucene/index/RandomCodec.java  |  15 +-
 .../solr/collection1/conf/schema_codec.xml    |   3 -
 .../apache/solr/core/TestCodecSupport.java    |   5 -
 19 files changed, 230 insertions(+), 100 deletions(-)
 rename lucene/core/src/test/org/apache/lucene/{index => codecs}/TestCodecUtil.java (99%)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index af74c26967b..20df7b22fae 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -12,6 +12,12 @@ Bug Fixes
 
 * LUCENE-6662: Fixed potential resource leaks. (Rishabh Patel via Adrien Grand)
 
Improvements

* LUCENE-7323: Compound file writing now verifies the incoming
  sub-files' checkums and segment IDs, to catch hardware issues or
  filesytem bugs earlier (Robert Muir, Mike McCandless)

 Other
 
 * LUCENE-4787: Fixed some highlighting javadocs. (Michael Dodsworth via Adrien
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextDocValuesFormat.java b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextDocValuesFormat.java
index 46ac9839fa6..a846dc9d36a 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextDocValuesFormat.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextDocValuesFormat.java
@@ -122,7 +122,7 @@ import org.apache.lucene.index.SegmentWriteState;
  *  and saving the offset/etc for each field. 
  *  @lucene.experimental
  */
public class SimpleTextDocValuesFormat extends DocValuesFormat {
class SimpleTextDocValuesFormat extends DocValuesFormat {
   
   public SimpleTextDocValuesFormat() {
     super("SimpleText");
diff --git a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextPostingsFormat.java b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextPostingsFormat.java
index a77050561c6..44371200585 100644
-- a/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextPostingsFormat.java
++ b/lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextPostingsFormat.java
@@ -34,7 +34,7 @@ import org.apache.lucene.index.SegmentWriteState;
  *  any text editor, and even edit it to alter your index.
  *
  *  @lucene.experimental */
public final class SimpleTextPostingsFormat extends PostingsFormat {
final class SimpleTextPostingsFormat extends PostingsFormat {
   
   public SimpleTextPostingsFormat() {
     super("SimpleText");
diff --git a/lucene/codecs/src/resources/META-INF/services/org.apache.lucene.codecs.DocValuesFormat b/lucene/codecs/src/resources/META-INF/services/org.apache.lucene.codecs.DocValuesFormat
index 3e7164d967e..daef7c58536 100644
-- a/lucene/codecs/src/resources/META-INF/services/org.apache.lucene.codecs.DocValuesFormat
++ b/lucene/codecs/src/resources/META-INF/services/org.apache.lucene.codecs.DocValuesFormat
@@ -15,4 +15,3 @@
 
 org.apache.lucene.codecs.memory.MemoryDocValuesFormat
 org.apache.lucene.codecs.memory.DirectDocValuesFormat
org.apache.lucene.codecs.simpletext.SimpleTextDocValuesFormat
diff --git a/lucene/codecs/src/resources/META-INF/services/org.apache.lucene.codecs.PostingsFormat b/lucene/codecs/src/resources/META-INF/services/org.apache.lucene.codecs.PostingsFormat
index b82f15600d8..753b6d7b3ca 100644
-- a/lucene/codecs/src/resources/META-INF/services/org.apache.lucene.codecs.PostingsFormat
++ b/lucene/codecs/src/resources/META-INF/services/org.apache.lucene.codecs.PostingsFormat
@@ -19,5 +19,4 @@ org.apache.lucene.codecs.memory.DirectPostingsFormat
 org.apache.lucene.codecs.memory.FSTOrdPostingsFormat
 org.apache.lucene.codecs.memory.FSTPostingsFormat
 org.apache.lucene.codecs.memory.MemoryPostingsFormat
org.apache.lucene.codecs.simpletext.SimpleTextPostingsFormat
 org.apache.lucene.codecs.autoprefix.AutoPrefixPostingsFormat
diff --git a/lucene/codecs/src/test/org/apache/lucene/codecs/simpletext/TestSimpleTextCompoundFormat.java b/lucene/codecs/src/test/org/apache/lucene/codecs/simpletext/TestSimpleTextCompoundFormat.java
index ea38832593e..2f54e2c9159 100644
-- a/lucene/codecs/src/test/org/apache/lucene/codecs/simpletext/TestSimpleTextCompoundFormat.java
++ b/lucene/codecs/src/test/org/apache/lucene/codecs/simpletext/TestSimpleTextCompoundFormat.java
@@ -27,4 +27,14 @@ public class TestSimpleTextCompoundFormat extends BaseCompoundFormatTestCase {
   protected Codec getCodec() {
     return codec;
   }

  @Override
  public void testCorruptFilesAreCaught() {
    // SimpleText does not catch broken sub-files in CFS!
  }

  @Override
  public void testMissingCodecHeadersAreCaught() {
    // SimpleText does not catch broken sub-files in CFS!
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/CodecUtil.java b/lucene/core/src/java/org/apache/lucene/codecs/CodecUtil.java
index 62bf2d58dc3..da487d00c91 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/CodecUtil.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/CodecUtil.java
@@ -258,6 +258,57 @@ public final class CodecUtil {
     return version;
   }
 
  /**
   * Expert: verifies the incoming {@link IndexInput} has an index header
   * and that its segment ID matches the expected one, and then copies
   * that index header into the provided {@link DataOutput}.  This is
   * useful when building compound files.
   *
   * @param in Input stream, positioned at the point where the
   *        index header was previously written. Typically this is located
   *        at the beginning of the file.
   * @param out Output stream, where the header will be copied to.
   * @param expectedID Expected segment ID
   * @throws CorruptIndexException If the first four bytes are not
   *         {@link #CODEC_MAGIC}, or if the <code>expectedID</code>
   *         does not match.
   * @throws IOException If there is an I/O error reading from the underlying medium.
   *
   * @lucene.internal 
   */
  public static void verifyAndCopyIndexHeader(IndexInput in, DataOutput out, byte[] expectedID) throws IOException {
    // make sure it's large enough to have a header and footer
    if (in.length() < footerLength() + headerLength("")) {
      throw new CorruptIndexException("compound sub-files must have a valid codec header and footer: file is too small (" + in.length() + " bytes)", in);
    }

    int actualHeader = in.readInt();
    if (actualHeader != CODEC_MAGIC) {
      throw new CorruptIndexException("compound sub-files must have a valid codec header and footer: codec header mismatch: actual header=" + actualHeader + " vs expected header=" + CodecUtil.CODEC_MAGIC, in);
    }

    // we can't verify these, so we pass-through:
    String codec = in.readString();
    int version = in.readInt();

    // verify id:
    checkIndexHeaderID(in, expectedID);

    // we can't verify extension either, so we pass-through:
    int suffixLength = in.readByte() & 0xFF;
    byte[] suffixBytes = new byte[suffixLength];
    in.readBytes(suffixBytes, 0, suffixLength);

    // now write the header we just verified
    out.writeInt(CodecUtil.CODEC_MAGIC);
    out.writeString(codec);
    out.writeInt(version);
    out.writeBytes(expectedID, 0, expectedID.length);
    out.writeByte((byte) suffixLength);
    out.writeBytes(suffixBytes, 0, suffixLength);
  }


   /** Retrieves the full index header from the provided {@link IndexInput}.
    *  This throws {@link CorruptIndexException} if this file does
    * not appear to be an index file. */
@@ -474,7 +525,7 @@ public final class CodecUtil {
    * @throws CorruptIndexException if CRC is formatted incorrectly (wrong bits set)
    * @throws IOException if an i/o error occurs
    */
  public static long readCRC(IndexInput input) throws IOException {
  static long readCRC(IndexInput input) throws IOException {
     long value = input.readLong();
     if ((value & 0xFFFFFFFF00000000L) != 0) {
       throw new CorruptIndexException("Illegal CRC-32 checksum: " + value, input);
@@ -487,7 +538,7 @@ public final class CodecUtil {
    * @throws IllegalStateException if CRC is formatted incorrectly (wrong bits set)
    * @throws IOException if an i/o error occurs
    */
  public static void writeCRC(IndexOutput output) throws IOException {
  static void writeCRC(IndexOutput output) throws IOException {
     long value = output.getChecksum();
     if ((value & 0xFFFFFFFF00000000L) != 0) {
       throw new IllegalStateException("Illegal CRC-32 checksum: " + value + " (resource=" + output + ")");
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/CompoundFormat.java b/lucene/core/src/java/org/apache/lucene/codecs/CompoundFormat.java
index 954a78e0e7f..af1cc2af5e2 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/CompoundFormat.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/CompoundFormat.java
@@ -43,7 +43,9 @@ public abstract class CompoundFormat {
   public abstract Directory getCompoundReader(Directory dir, SegmentInfo si, IOContext context) throws IOException;
   
   /**
   * Packs the provided segment's files into a compound format.
   * Packs the provided segment's files into a compound format.  All files referenced
   * by the provided {@link SegmentInfo} must have {@link CodecUtil#writeIndexHeader}
   * and {@link CodecUtil#writeFooter}.
    */
   public abstract void write(Directory dir, SegmentInfo si, IOContext context) throws IOException;
 }
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/lucene50/Lucene50CompoundFormat.java b/lucene/core/src/java/org/apache/lucene/codecs/lucene50/Lucene50CompoundFormat.java
index 2a40bde2ed5..da2b93fcee1 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/lucene50/Lucene50CompoundFormat.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/lucene50/Lucene50CompoundFormat.java
@@ -18,17 +18,17 @@ package org.apache.lucene.codecs.lucene50;
 
 
 import java.io.IOException;
import java.util.Collection;
 
 import org.apache.lucene.codecs.CodecUtil;
 import org.apache.lucene.codecs.CompoundFormat;
 import org.apache.lucene.index.IndexFileNames;
 import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.ChecksumIndexInput;
 import org.apache.lucene.store.DataOutput;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
 import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.StringHelper;
 
 /**
  * Lucene 5.0 compound file format
@@ -76,6 +76,9 @@ public final class Lucene50CompoundFormat extends CompoundFormat {
     String dataFile = IndexFileNames.segmentFileName(si.name, "", DATA_EXTENSION);
     String entriesFile = IndexFileNames.segmentFileName(si.name, "", ENTRIES_EXTENSION);
     
    byte[] expectedID = si.getId();
    byte[] id = new byte[StringHelper.ID_LENGTH];

     try (IndexOutput data =    dir.createOutput(dataFile, context);
          IndexOutput entries = dir.createOutput(entriesFile, context)) {
       CodecUtil.writeIndexHeader(data,    DATA_CODEC, VERSION_CURRENT, si.getId(), "");
@@ -87,8 +90,23 @@ public final class Lucene50CompoundFormat extends CompoundFormat {
         
         // write bytes for file
         long startOffset = data.getFilePointer();
        try (IndexInput in = dir.openInput(file, IOContext.READONCE)) {
          data.copyBytes(in, in.length());
        try (ChecksumIndexInput in = dir.openChecksumInput(file, IOContext.READONCE)) {

          // just copies the index header, verifying that its id matches what we expect
          CodecUtil.verifyAndCopyIndexHeader(in, data, si.getId());
          
          // copy all bytes except the footer
          long numBytesToCopy = in.length() - CodecUtil.footerLength() - in.getFilePointer();
          data.copyBytes(in, numBytesToCopy);

          // verify footer (checksum) matches for the incoming file we are copying
          long checksum = CodecUtil.checkFooter(in);

          // this is poached from CodecUtil.writeFooter, but we need to use our own checksum, not data.getChecksum(), but I think
          // adding a public method to CodecUtil to do that is somewhat dangerous:
          data.writeInt(CodecUtil.FOOTER_MAGIC);
          data.writeInt(0);
          data.writeLong(checksum);
         }
         long endOffset = data.getFilePointer();
         
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/lucene50/Lucene50CompoundReader.java b/lucene/core/src/java/org/apache/lucene/codecs/lucene50/Lucene50CompoundReader.java
index a4487826d62..f7de16915dc 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/lucene50/Lucene50CompoundReader.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/lucene50/Lucene50CompoundReader.java
@@ -100,7 +100,7 @@ final class Lucene50CompoundReader extends Directory {
   }
 
   /** Helper method that reads CFS entries from an input stream */
  private final Map<String, FileEntry> readEntries(byte[] segmentID, Directory dir, String entriesFileName) throws IOException {
  private Map<String, FileEntry> readEntries(byte[] segmentID, Directory dir, String entriesFileName) throws IOException {
     Map<String,FileEntry> mapping = null;
     try (ChecksumIndexInput entriesStream = dir.openChecksumInput(entriesFileName, IOContext.READONCE)) {
       Throwable priorE = null;
@@ -140,7 +140,8 @@ final class Lucene50CompoundReader extends Directory {
     final String id = IndexFileNames.stripSegmentName(name);
     final FileEntry entry = entries.get(id);
     if (entry == null) {
      throw new FileNotFoundException("No sub-file with id " + id + " found (fileName=" + name + " files: " + entries.keySet() + ")");
      String datFileName = IndexFileNames.segmentFileName(segmentName, "", Lucene50CompoundFormat.DATA_EXTENSION);
      throw new FileNotFoundException("No sub-file with id " + id + " found in compound file \"" + datFileName + "\" (fileName=" + name + " files: " + entries.keySet() + ")");
     }
     return handle.slice(name, entry.offset, entry.length);
   }
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestCodecUtil.java b/lucene/core/src/test/org/apache/lucene/codecs/TestCodecUtil.java
similarity index 99%
rename from lucene/core/src/test/org/apache/lucene/index/TestCodecUtil.java
rename to lucene/core/src/test/org/apache/lucene/codecs/TestCodecUtil.java
index 9752ce3e752..d403f81b54f 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestCodecUtil.java
++ b/lucene/core/src/test/org/apache/lucene/codecs/TestCodecUtil.java
@@ -14,13 +14,14 @@
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package org.apache.lucene.index;
 
package org.apache.lucene.codecs;
 
 import java.io.IOException;
 import java.util.concurrent.atomic.AtomicLong;
 
 import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.CorruptIndexException;
 import org.apache.lucene.store.BufferedChecksumIndexInput;
 import org.apache.lucene.store.ChecksumIndexInput;
 import org.apache.lucene.store.IndexInput;
diff --git a/lucene/core/src/test/org/apache/lucene/codecs/perfield/TestPerFieldDocValuesFormat.java b/lucene/core/src/test/org/apache/lucene/codecs/perfield/TestPerFieldDocValuesFormat.java
index 2eb0d1a94e4..1ebfb69304f 100644
-- a/lucene/core/src/test/org/apache/lucene/codecs/perfield/TestPerFieldDocValuesFormat.java
++ b/lucene/core/src/test/org/apache/lucene/codecs/perfield/TestPerFieldDocValuesFormat.java
@@ -79,7 +79,7 @@ public class TestPerFieldDocValuesFormat extends BaseDocValuesFormatTestCase {
     // we don't use RandomIndexWriter because it might add more docvalues than we expect !!!!1
     IndexWriterConfig iwc = newIndexWriterConfig(analyzer);
     final DocValuesFormat fast = TestUtil.getDefaultDocValuesFormat();
    final DocValuesFormat slow = DocValuesFormat.forName("SimpleText");
    final DocValuesFormat slow = DocValuesFormat.forName("Memory");
     iwc.setCodec(new AssertingCodec() {
       @Override
       public DocValuesFormat getDocValuesFormatForField(String field) {
diff --git a/lucene/core/src/test/org/apache/lucene/codecs/perfield/TestPerFieldPostingsFormat2.java b/lucene/core/src/test/org/apache/lucene/codecs/perfield/TestPerFieldPostingsFormat2.java
index 67d61df375e..58c37fc525b 100644
-- a/lucene/core/src/test/org/apache/lucene/codecs/perfield/TestPerFieldPostingsFormat2.java
++ b/lucene/core/src/test/org/apache/lucene/codecs/perfield/TestPerFieldPostingsFormat2.java
@@ -24,8 +24,8 @@ import org.apache.lucene.codecs.Codec;
 import org.apache.lucene.codecs.PostingsFormat;
 import org.apache.lucene.codecs.asserting.AssertingCodec;
 import org.apache.lucene.codecs.blockterms.LuceneVarGapFixedInterval;
import org.apache.lucene.codecs.memory.DirectPostingsFormat;
 import org.apache.lucene.codecs.memory.MemoryPostingsFormat;
import org.apache.lucene.codecs.simpletext.SimpleTextPostingsFormat;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.FieldType;
@@ -33,8 +33,8 @@ import org.apache.lucene.document.TextField;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.LogDocMergePolicy;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
@@ -202,13 +202,13 @@ public class TestPerFieldPostingsFormat2 extends LuceneTestCase {
 
   public static class MockCodec extends AssertingCodec {
     final PostingsFormat luceneDefault = TestUtil.getDefaultPostingsFormat();
    final PostingsFormat simpleText = new SimpleTextPostingsFormat();
    final PostingsFormat direct = new DirectPostingsFormat();
     final PostingsFormat memory = new MemoryPostingsFormat();
     
     @Override
     public PostingsFormat getPostingsFormatForField(String field) {
       if (field.equals("id")) {
        return simpleText;
        return direct;
       } else if (field.equals("content")) {
         return memory;
       } else {
@@ -219,12 +219,12 @@ public class TestPerFieldPostingsFormat2 extends LuceneTestCase {
 
   public static class MockCodec2 extends AssertingCodec {
     final PostingsFormat luceneDefault = TestUtil.getDefaultPostingsFormat();
    final PostingsFormat simpleText = new SimpleTextPostingsFormat();
    final PostingsFormat direct = new DirectPostingsFormat();
     
     @Override
     public PostingsFormat getPostingsFormatForField(String field) {
       if (field.equals("id")) {
        return simpleText;
        return direct;
       } else {
         return luceneDefault;
       }
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestAddIndexes.java b/lucene/core/src/test/org/apache/lucene/index/TestAddIndexes.java
index 9d00c3f42d2..876328a4a42 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestAddIndexes.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestAddIndexes.java
@@ -1086,14 +1086,14 @@ public class TestAddIndexes extends LuceneTestCase {
   }
 
   private static final class CustomPerFieldCodec extends AssertingCodec {
    private final PostingsFormat simpleTextFormat = PostingsFormat.forName("SimpleText");
    private final PostingsFormat directFormat = PostingsFormat.forName("Direct");
     private final PostingsFormat defaultFormat = TestUtil.getDefaultPostingsFormat();
     private final PostingsFormat memoryFormat = PostingsFormat.forName("Memory");
 
     @Override
     public PostingsFormat getPostingsFormatForField(String field) {
       if (field.equals("id")) {
        return simpleTextFormat;
        return directFormat;
       } else if (field.equals("content")) {
         return memoryFormat;
       } else {
diff --git a/lucene/test-framework/src/java/org/apache/lucene/codecs/mockrandom/MockRandomPostingsFormat.java b/lucene/test-framework/src/java/org/apache/lucene/codecs/mockrandom/MockRandomPostingsFormat.java
index 4d943e6a495..6b8793930e1 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/codecs/mockrandom/MockRandomPostingsFormat.java
++ b/lucene/test-framework/src/java/org/apache/lucene/codecs/mockrandom/MockRandomPostingsFormat.java
@@ -19,6 +19,7 @@ package org.apache.lucene.codecs.mockrandom;
 import java.io.IOException;
 import java.util.Random;
 
import org.apache.lucene.codecs.CodecUtil;
 import org.apache.lucene.codecs.FieldsConsumer;
 import org.apache.lucene.codecs.FieldsProducer;
 import org.apache.lucene.codecs.PostingsFormat;
@@ -47,6 +48,7 @@ import org.apache.lucene.index.FieldInfo;
 import org.apache.lucene.index.IndexFileNames;
 import org.apache.lucene.index.SegmentReadState;
 import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.ChecksumIndexInput;
 import org.apache.lucene.store.IndexInput;
 import org.apache.lucene.store.IndexOutput;
 import org.apache.lucene.util.BytesRef;
@@ -107,11 +109,10 @@ public final class MockRandomPostingsFormat extends PostingsFormat {
     }
 
     final String seedFileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, SEED_EXT);
    final IndexOutput out = state.directory.createOutput(seedFileName, state.context);
    try {
    try(IndexOutput out = state.directory.createOutput(seedFileName, state.context)) {
      CodecUtil.writeIndexHeader(out, "MockRandomSeed", 0, state.segmentInfo.getId(), state.segmentSuffix);
       out.writeLong(seed);
    } finally {
      out.close();
      CodecUtil.writeFooter(out);
     }
 
     final Random random = new Random(seed);
@@ -267,8 +268,10 @@ public final class MockRandomPostingsFormat extends PostingsFormat {
   public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
 
     final String seedFileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, SEED_EXT);
    final IndexInput in = state.directory.openInput(seedFileName, state.context);
    final ChecksumIndexInput in = state.directory.openChecksumInput(seedFileName, state.context);
    CodecUtil.checkIndexHeader(in, "MockRandomSeed", 0, 0, state.segmentInfo.getId(), state.segmentSuffix);
     final long seed = in.readLong();
    CodecUtil.checkFooter(in);
     if (LuceneTestCase.VERBOSE) {
       System.out.println("MockRandomCodec: reading from seg=" + state.segmentInfo.name + " formatID=" + state.segmentSuffix + " seed=" + seed);
     }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/BaseCompoundFormatTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/index/BaseCompoundFormatTestCase.java
index 7c19596aa81..256b24e7d02 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/BaseCompoundFormatTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/BaseCompoundFormatTestCase.java
@@ -25,6 +25,7 @@ import java.util.List;
 import java.util.Random;
 
 import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.CodecUtil;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.StoredField;
@@ -72,9 +73,9 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
     for (int i=0; i<data.length; i++) {
       String testfile = "_" + i + ".test";
       Directory dir = newDirectory();
      createSequenceFile(dir, testfile, (byte) 0, data[i]);
      
       SegmentInfo si = newSegmentInfo(dir, "_" + i);
      createSequenceFile(dir, testfile, (byte) 0, data[i], si.getId(), "suffix");
      
       si.setFiles(Collections.singleton(testfile));
       si.getCodec().compoundFormat().write(dir, si, IOContext.DEFAULT);
       Directory cfs = si.getCodec().compoundFormat().getCompoundReader(dir, si, IOContext.DEFAULT);
@@ -96,10 +97,10 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
   public void testTwoFiles() throws IOException {
     String files[] = { "_123.d1", "_123.d2" };
     Directory dir = newDirectory();
    createSequenceFile(dir, files[0], (byte) 0, 15);
    createSequenceFile(dir, files[1], (byte) 0, 114);
    
     SegmentInfo si = newSegmentInfo(dir, "_123");
    createSequenceFile(dir, files[0], (byte) 0, 15, si.getId(), "suffix");
    createSequenceFile(dir, files[1], (byte) 0, 114, si.getId(), "suffix");
    
     si.setFiles(Arrays.asList(files));
     si.getCodec().compoundFormat().write(dir, si, IOContext.DEFAULT);
     Directory cfs = si.getCodec().compoundFormat().getCompoundReader(dir, si, IOContext.DEFAULT);
@@ -122,11 +123,13 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
     final String testfile = "_123.test";
 
     Directory dir = newDirectory();
    IndexOutput out = dir.createOutput(testfile, IOContext.DEFAULT);
    out.writeInt(3);
    out.close();
    
     SegmentInfo si = newSegmentInfo(dir, "_123");
    try (IndexOutput out = dir.createOutput(testfile, IOContext.DEFAULT)) {
      CodecUtil.writeIndexHeader(out, "Foo", 0, si.getId(), "suffix");
      out.writeInt(3);
      CodecUtil.writeFooter(out);
    }
    
     si.setFiles(Collections.singleton(testfile));
     si.getCodec().compoundFormat().write(dir, si, IOContext.DEFAULT);
     Directory cfs = si.getCodec().compoundFormat().getCompoundReader(dir, si, IOContext.DEFAULT);
@@ -148,11 +151,13 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
         return super.createOutput(name, context);
       }
     };
    IndexOutput out = dir.createOutput(testfile, myContext);
    out.writeInt(3);
    out.close();
    
     SegmentInfo si = newSegmentInfo(dir, "_123");
    try (IndexOutput out = dir.createOutput(testfile, myContext)) {
      CodecUtil.writeIndexHeader(out, "Foo", 0, si.getId(), "suffix");
      out.writeInt(3);
      CodecUtil.writeFooter(out);
    }
    
     si.setFiles(Collections.singleton(testfile));
     si.getCodec().compoundFormat().write(dir, si, myContext);
     dir.close();
@@ -165,14 +170,16 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
 
     Directory dir = new NRTCachingDirectory(newFSDirectory(createTempDir()), 2.0, 25.0);
 
    IndexOutput out = dir.createOutput(testfile, context);
    byte[] bytes = new byte[512];
    for(int i=0;i<1024*1024;i++) {
      out.writeBytes(bytes, 0, bytes.length);
    SegmentInfo si = newSegmentInfo(dir, "_123");
    try (IndexOutput out = dir.createOutput(testfile, context)) {
      CodecUtil.writeIndexHeader(out, "Foo", 0, si.getId(), "suffix");
      byte[] bytes = new byte[512];
      for(int i=0;i<1024*1024;i++) {
        out.writeBytes(bytes, 0, bytes.length);
      }
      CodecUtil.writeFooter(out);
     }
    out.close();
     
    SegmentInfo si = newSegmentInfo(dir, "_123");
     si.setFiles(Collections.singleton(testfile));
     si.getCodec().compoundFormat().write(dir, si, context);
 
@@ -326,17 +333,19 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
     // Setup the test segment
     String segment = "_123";
     int chunk = 1024; // internal buffer size used by the stream
    createRandomFile(dir, segment + ".zero", 0);
    createRandomFile(dir, segment + ".one", 1);
    createRandomFile(dir, segment + ".ten", 10);
    createRandomFile(dir, segment + ".hundred", 100);
    createRandomFile(dir, segment + ".big1", chunk);
    createRandomFile(dir, segment + ".big2", chunk - 1);
    createRandomFile(dir, segment + ".big3", chunk + 1);
    createRandomFile(dir, segment + ".big4", 3 * chunk);
    createRandomFile(dir, segment + ".big5", 3 * chunk - 1);
    createRandomFile(dir, segment + ".big6", 3 * chunk + 1);
    createRandomFile(dir, segment + ".big7", 1000 * chunk);
    SegmentInfo si = newSegmentInfo(dir, "_123");
    byte[] segId = si.getId();
    createRandomFile(dir, segment + ".zero", 0, segId);
    createRandomFile(dir, segment + ".one", 1, segId);
    createRandomFile(dir, segment + ".ten", 10, segId);
    createRandomFile(dir, segment + ".hundred", 100, segId);
    createRandomFile(dir, segment + ".big1", chunk, segId);
    createRandomFile(dir, segment + ".big2", chunk - 1, segId);
    createRandomFile(dir, segment + ".big3", chunk + 1, segId);
    createRandomFile(dir, segment + ".big4", 3 * chunk, segId);
    createRandomFile(dir, segment + ".big5", 3 * chunk - 1, segId);
    createRandomFile(dir, segment + ".big6", 3 * chunk + 1, segId);
    createRandomFile(dir, segment + ".big7", 1000 * chunk, segId);
     
     List<String> files = new ArrayList<>();
     for (String file : dir.listAll()) {
@@ -345,7 +354,6 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
       }
     }
     
    SegmentInfo si = newSegmentInfo(dir, "_123");
     si.setFiles(files);
     si.getCodec().compoundFormat().write(dir, si, IOContext.DEFAULT);
     Directory cfs = si.getCodec().compoundFormat().getCompoundReader(dir, si, IOContext.DEFAULT);
@@ -370,17 +378,19 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
     final int FILE_COUNT = atLeast(500);
     
     List<String> files = new ArrayList<>();
    SegmentInfo si = newSegmentInfo(dir, "_123");
     for (int fileIdx = 0; fileIdx < FILE_COUNT; fileIdx++) {
       String file = "_123." + fileIdx;
       files.add(file);
      IndexOutput out = dir.createOutput(file, newIOContext(random()));
      out.writeByte((byte) fileIdx);
      out.close();
      try (IndexOutput out = dir.createOutput(file, newIOContext(random()))) {
        CodecUtil.writeIndexHeader(out, "Foo", 0, si.getId(), "suffix");
        out.writeByte((byte) fileIdx);
        CodecUtil.writeFooter(out);
      }
     }
     
     assertEquals(0, dir.getFileHandleCount());
     
    SegmentInfo si = newSegmentInfo(dir, "_123");
     si.setFiles(files);
     si.getCodec().compoundFormat().write(dir, si, IOContext.DEFAULT);
     Directory cfs = si.getCodec().compoundFormat().getCompoundReader(dir, si, IOContext.DEFAULT);
@@ -388,6 +398,7 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
     final IndexInput[] ins = new IndexInput[FILE_COUNT];
     for (int fileIdx = 0; fileIdx < FILE_COUNT; fileIdx++) {
       ins[fileIdx] = cfs.openInput("_123." + fileIdx, newIOContext(random()));
      CodecUtil.checkIndexHeader(ins[fileIdx], "Foo", 0, 0, si.getId(), "suffix");
     }
     
     assertEquals(1, dir.getFileHandleCount());
@@ -631,27 +642,31 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
   }
   
   /** Creates a file of the specified size with random data. */
  protected static void createRandomFile(Directory dir, String name, int size) throws IOException {
    IndexOutput os = dir.createOutput(name, newIOContext(random()));
  protected static void createRandomFile(Directory dir, String name, int size, byte[] segId) throws IOException {
     Random rnd = random();
    for (int i=0; i<size; i++) {
      byte b = (byte) rnd.nextInt(256);
      os.writeByte(b);
    try (IndexOutput os = dir.createOutput(name, newIOContext(random()))) {
      CodecUtil.writeIndexHeader(os, "Foo", 0, segId, "suffix");
      for (int i=0; i<size; i++) {
        byte b = (byte) rnd.nextInt(256);
        os.writeByte(b);
      }
      CodecUtil.writeFooter(os);
     }
    os.close();
   }
   
   /** Creates a file of the specified size with sequential data. The first
    *  byte is written as the start byte provided. All subsequent bytes are
    *  computed as start + offset where offset is the number of the byte.
    */
  protected static void createSequenceFile(Directory dir, String name, byte start, int size) throws IOException {
    IndexOutput os = dir.createOutput(name, newIOContext(random()));
    for (int i=0; i < size; i++) {
      os.writeByte(start);
      start ++;
  protected static void createSequenceFile(Directory dir, String name, byte start, int size, byte[] segID, String segSuffix) throws IOException {
    try (IndexOutput os = dir.createOutput(name, newIOContext(random()))) {
      CodecUtil.writeIndexHeader(os, "Foo", 0, segID, segSuffix);
      for (int i=0; i < size; i++) {
        os.writeByte(start);
        start ++;
      }
      CodecUtil.writeFooter(os);
     }
    os.close();
   }
   
   protected static void assertSameStreams(String msg, IndexInput expected, IndexInput test) throws IOException {
@@ -724,12 +739,12 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
    */
   protected static Directory createLargeCFS(Directory dir) throws IOException {
     List<String> files = new ArrayList<>();
    SegmentInfo si = newSegmentInfo(dir, "_123");
     for (int i = 0; i < 20; i++) {
      createSequenceFile(dir, "_123.f" + i, (byte) 0, 2000);
      createSequenceFile(dir, "_123.f" + i, (byte) 0, 2000, si.getId(), "suffix");
       files.add("_123.f" + i);
     }
     
    SegmentInfo si = newSegmentInfo(dir, "_123");
     si.setFiles(files);
     si.getCodec().compoundFormat().write(dir, si, IOContext.DEFAULT);
     Directory cfs = si.getCodec().compoundFormat().getCompoundReader(dir, si, IOContext.DEFAULT);
@@ -750,9 +765,9 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
   public void testResourceNameInsideCompoundFile() throws Exception {
     Directory dir = newDirectory();
     String subFile = "_123.xyz";
    createSequenceFile(dir, subFile, (byte) 0, 10);
    
     SegmentInfo si = newSegmentInfo(dir, "_123");
    createSequenceFile(dir, subFile, (byte) 0, 10, si.getId(), "suffix");
    
     si.setFiles(Collections.singletonList(subFile));
     si.getCodec().compoundFormat().write(dir, si, IOContext.DEFAULT);
     Directory cfs = si.getCodec().compoundFormat().getCompoundReader(dir, si, IOContext.DEFAULT);
@@ -762,4 +777,48 @@ public abstract class BaseCompoundFormatTestCase extends BaseIndexFileFormatTest
     cfs.close();
     dir.close();
   }

  public void testMissingCodecHeadersAreCaught() throws Exception {
    Directory dir = newDirectory();
    String subFile = "_123.xyz";

    // missing codec header
    try (IndexOutput os = dir.createOutput(subFile, newIOContext(random()))) {
      for (int i=0; i < 1024; i++) {
        os.writeByte((byte) i);
      }
    }

    SegmentInfo si = newSegmentInfo(dir, "_123");
    si.setFiles(Collections.singletonList(subFile));
    Exception e = expectThrows(CorruptIndexException.class, () -> si.getCodec().compoundFormat().write(dir, si, IOContext.DEFAULT));
    assertTrue(e.getMessage().contains("codec header mismatch"));
    dir.close();
  }

  public void testCorruptFilesAreCaught() throws Exception {
    Directory dir = newDirectory();
    String subFile = "_123.xyz";

    // wrong checksum
    SegmentInfo si = newSegmentInfo(dir, "_123");
    try (IndexOutput os = dir.createOutput(subFile, newIOContext(random()))) {
      CodecUtil.writeIndexHeader(os, "Foo", 0, si.getId(), "suffix");
      for (int i=0; i < 1024; i++) {
        os.writeByte((byte) i);
      }

      // write footer w/ wrong checksum
      os.writeInt(CodecUtil.FOOTER_MAGIC);
      os.writeInt(0);

      long checksum = os.getChecksum();
      os.writeLong(checksum+1);
    }

    si.setFiles(Collections.singletonList(subFile));
    Exception e = expectThrows(CorruptIndexException.class, () -> si.getCodec().compoundFormat().write(dir, si, IOContext.DEFAULT));
    assertTrue(e.getMessage().contains("checksum failed (hardware problem?)"));
    dir.close();
  }
 }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/RandomCodec.java b/lucene/test-framework/src/java/org/apache/lucene/index/RandomCodec.java
index c1c33f895c2..127549ff065 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/RandomCodec.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/RandomCodec.java
@@ -22,7 +22,6 @@ import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
import java.util.Locale;
 import java.util.Map;
 import java.util.Random;
 import java.util.Set;
@@ -50,8 +49,6 @@ import org.apache.lucene.codecs.memory.FSTPostingsFormat;
 import org.apache.lucene.codecs.memory.MemoryDocValuesFormat;
 import org.apache.lucene.codecs.memory.MemoryPostingsFormat;
 import org.apache.lucene.codecs.mockrandom.MockRandomPostingsFormat;
import org.apache.lucene.codecs.simpletext.SimpleTextDocValuesFormat;
import org.apache.lucene.codecs.simpletext.SimpleTextPostingsFormat;
 import org.apache.lucene.index.PointValues.IntersectVisitor;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.LuceneTestCase;
@@ -159,10 +156,6 @@ public class RandomCodec extends AssertingCodec {
     PostingsFormat codec = previousMappings.get(name);
     if (codec == null) {
       codec = formats.get(Math.abs(perFieldSeed ^ name.hashCode()) % formats.size());
      if (codec instanceof SimpleTextPostingsFormat && perFieldSeed % 5 != 0) {
        // make simpletext rarer, choose again
        codec = formats.get(Math.abs(perFieldSeed ^ name.toUpperCase(Locale.ROOT).hashCode()) % formats.size());
      }
       previousMappings.put(name, codec);
       // Safety:
       assert previousMappings.size() < 10000: "test went insane";
@@ -175,10 +168,6 @@ public class RandomCodec extends AssertingCodec {
     DocValuesFormat codec = previousDVMappings.get(name);
     if (codec == null) {
       codec = dvFormats.get(Math.abs(perFieldSeed ^ name.hashCode()) % dvFormats.size());
      if (codec instanceof SimpleTextDocValuesFormat && perFieldSeed % 5 != 0) {
        // make simpletext rarer, choose again
        codec = dvFormats.get(Math.abs(perFieldSeed ^ name.toUpperCase(Locale.ROOT).hashCode()) % dvFormats.size());
      }
       previousDVMappings.put(name, codec);
       // Safety:
       assert previousDVMappings.size() < 10000: "test went insane";
@@ -214,7 +203,7 @@ public class RandomCodec extends AssertingCodec {
         new LuceneFixedGap(TestUtil.nextInt(random, 1, 1000)),
         new LuceneVarGapFixedInterval(TestUtil.nextInt(random, 1, 1000)),
         new LuceneVarGapDocFreqInterval(TestUtil.nextInt(random, 1, 100), TestUtil.nextInt(random, 1, 1000)),
        random.nextInt(10) == 0 ? new SimpleTextPostingsFormat() : TestUtil.getDefaultPostingsFormat(),
        TestUtil.getDefaultPostingsFormat(),
         new AssertingPostingsFormat(),
         new MemoryPostingsFormat(true, random.nextFloat()),
         new MemoryPostingsFormat(false, random.nextFloat()));
@@ -223,7 +212,7 @@ public class RandomCodec extends AssertingCodec {
         TestUtil.getDefaultDocValuesFormat(),
         new DirectDocValuesFormat(), // maybe not a great idea...
         new MemoryDocValuesFormat(),
        random.nextInt(10) == 0 ? new SimpleTextDocValuesFormat() : TestUtil.getDefaultDocValuesFormat(),
        TestUtil.getDefaultDocValuesFormat(),
         new AssertingDocValuesFormat());
 
     Collections.shuffle(formats, random);
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema_codec.xml b/solr/core/src/test-files/solr/collection1/conf/schema_codec.xml
index 629396a2117..8cd07297de4 100644
-- a/solr/core/src/test-files/solr/collection1/conf/schema_codec.xml
++ b/solr/core/src/test-files/solr/collection1/conf/schema_codec.xml
@@ -17,7 +17,6 @@
 -->
 <schema name="codec" version="1.2">
   <fieldType name="string_direct" class="solr.StrField" postingsFormat="Direct"/>
  <fieldType name="string_simpletext" class="solr.StrField" postingsFormat="SimpleText"/>
   <fieldType name="string_standard" class="solr.StrField" postingsFormat="Lucene50"/>
 
   <fieldType name="string_disk" class="solr.StrField" docValuesFormat="Lucene54"/>
@@ -37,7 +36,6 @@
   </fieldType>
 
   <field name="string_direct_f" type="string_direct" indexed="true" stored="true"/>
  <field name="string_simpletext_f" type="string_simpletext" indexed="true" stored="true"/>
   <field name="string_standard_f" type="string_standard" indexed="true" stored="true"/>
 
   <field name="string_disk_f" type="string_disk" indexed="false" stored="false" docValues="true" default=""/>
@@ -46,7 +44,6 @@
   <field name="string_f" type="string" indexed="true" stored="true" docValues="true" required="true"/>
   <field name="text" type="text_general" indexed="true" stored="true"/>
 
  <dynamicField name="*_simple" type="string_simpletext" indexed="true" stored="true"/>
   <dynamicField name="*_direct" type="string_direct" indexed="true" stored="true"/>
   <dynamicField name="*_standard" type="string_standard" indexed="true" stored="true"/>
 
diff --git a/solr/core/src/test/org/apache/solr/core/TestCodecSupport.java b/solr/core/src/test/org/apache/solr/core/TestCodecSupport.java
index a1718e65d5c..0fe6a02dcca 100644
-- a/solr/core/src/test/org/apache/solr/core/TestCodecSupport.java
++ b/solr/core/src/test/org/apache/solr/core/TestCodecSupport.java
@@ -51,9 +51,6 @@ public class TestCodecSupport extends SolrTestCaseJ4 {
     SchemaField schemaField = fields.get("string_direct_f");
     PerFieldPostingsFormat format = (PerFieldPostingsFormat) codec.postingsFormat();
     assertEquals("Direct", format.getPostingsFormatForField(schemaField.getName()).getName());
    schemaField = fields.get("string_simpletext_f");
    assertEquals("SimpleText",
        format.getPostingsFormatForField(schemaField.getName()).getName());
     schemaField = fields.get("string_standard_f");
     assertEquals(TestUtil.getDefaultPostingsFormat().getName(), format.getPostingsFormatForField(schemaField.getName()).getName());
     schemaField = fields.get("string_f");
@@ -78,8 +75,6 @@ public class TestCodecSupport extends SolrTestCaseJ4 {
     Codec codec = h.getCore().getCodec();
     PerFieldPostingsFormat format = (PerFieldPostingsFormat) codec.postingsFormat();
 
    assertEquals("SimpleText", format.getPostingsFormatForField("foo_simple").getName());
    assertEquals("SimpleText", format.getPostingsFormatForField("bar_simple").getName());
     assertEquals("Direct", format.getPostingsFormatForField("foo_direct").getName());
     assertEquals("Direct", format.getPostingsFormatForField("bar_direct").getName());
     assertEquals(TestUtil.getDefaultPostingsFormat().getName(), format.getPostingsFormatForField("foo_standard").getName());
- 
2.19.1.windows.1

