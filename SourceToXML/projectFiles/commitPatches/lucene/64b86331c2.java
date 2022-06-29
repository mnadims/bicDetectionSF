From 64b86331c29d074fa7b257d65d3fda3b662bf96a Mon Sep 17 00:00:00 2001
From: Mike McCandless <mikemccand@apache.org>
Date: Fri, 13 Jan 2017 17:46:02 -0500
Subject: [PATCH] LUCENE-7626: IndexWriter no longer accepts broken offsets

--
 lucene/CHANGES.txt                            |   3 +
 .../miscellaneous/FixBrokenOffsetsFilter.java |  78 +++++++++++
 .../FixBrokenOffsetsFilterFactory.java        |  39 ++++++
 ...he.lucene.analysis.util.TokenFilterFactory |   1 +
 .../TestFixBrokenOffsetsFilter.java           |  50 +++++++
 .../apache/lucene/index/FixBrokenOffsets.java | 125 ++++++++++++++++++
 .../java/org/apache/lucene/index/package.html |  27 ++++
 .../lucene/index/TestFixBrokenOffsets.java    | 114 ++++++++++++++++
 .../lucene/index/index.630.brokenoffsets.zip  | Bin 0 -> 3203 bytes
 .../org/apache/lucene/index/CheckIndex.java   |  29 ++--
 .../lucene/index/DefaultIndexingChain.java    |  20 ++-
 .../apache/lucene/index/TestCheckIndex.java   |   5 -
 .../search/highlight/TokenSourcesTest.java    |   2 +-
 .../lucene/search/TestTermAutomatonQuery.java |   3 +
 .../index/BaseTermVectorsFormatTestCase.java  |  17 +--
 .../lucene/index/BaseTestCheckIndex.java      |  19 ---
 .../java/org/apache/lucene/util/TestUtil.java |   4 +-
 .../apache/solr/schema/PreAnalyzedField.java  |  11 ++
 .../solr/index/hdfs/CheckHdfsIndexTest.java   |   5 -
 19 files changed, 480 insertions(+), 72 deletions(-)
 create mode 100644 lucene/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/FixBrokenOffsetsFilter.java
 create mode 100644 lucene/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/FixBrokenOffsetsFilterFactory.java
 create mode 100644 lucene/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestFixBrokenOffsetsFilter.java
 create mode 100644 lucene/backward-codecs/src/java/org/apache/lucene/index/FixBrokenOffsets.java
 create mode 100644 lucene/backward-codecs/src/java/org/apache/lucene/index/package.html
 create mode 100644 lucene/backward-codecs/src/test/org/apache/lucene/index/TestFixBrokenOffsets.java
 create mode 100644 lucene/backward-codecs/src/test/org/apache/lucene/index/index.630.brokenoffsets.zip

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 040f4e0ef5b..30943d2a9d2 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -29,6 +29,9 @@ API Changes
 
 Bug Fixes
 
* LUCENE-7626: IndexWriter will no longer accept broken token offsets
  (Mike McCandless)

 Improvements
 
 * LUCENE-7489: Better storage of sparse doc-values fields with the default
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/FixBrokenOffsetsFilter.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/FixBrokenOffsetsFilter.java
new file mode 100644
index 00000000000..b0a6b1df24d
-- /dev/null
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/FixBrokenOffsetsFilter.java
@@ -0,0 +1,78 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.analysis.miscellaneous;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/** 
 * A filter to correct offsets that illegally go backwards.
 *
 * @deprecated Fix the token filters that create broken offsets in the first place.
 */
@Deprecated
public final class FixBrokenOffsetsFilter extends TokenFilter {

  private int lastStartOffset;
  private int lastEndOffset;

  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

  public FixBrokenOffsetsFilter(TokenStream in) {
    super(in);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken() == false) {
      return false;
    }
    fixOffsets();
    return true;
  }

  @Override
  public void end() throws IOException {
    super.end();
    fixOffsets();
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    lastStartOffset = 0;
    lastEndOffset = 0;
  }

  private void fixOffsets() {
    int startOffset = offsetAtt.startOffset();
    int endOffset = offsetAtt.endOffset();
    if (startOffset < lastStartOffset) {
      startOffset = lastStartOffset;
    }
    if (endOffset < startOffset) {
      endOffset = startOffset;
    }
    offsetAtt.setOffset(startOffset, endOffset);
    lastStartOffset = startOffset;
    lastEndOffset = endOffset;
  }
}
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/FixBrokenOffsetsFilterFactory.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/FixBrokenOffsetsFilterFactory.java
new file mode 100644
index 00000000000..8484b8c2d68
-- /dev/null
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/FixBrokenOffsetsFilterFactory.java
@@ -0,0 +1,39 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.analysis.miscellaneous;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * Factory for {@link FixBrokenOffsetsFilter}.
 */
public class FixBrokenOffsetsFilterFactory extends TokenFilterFactory {

  /** Sole constructor */
  public FixBrokenOffsetsFilterFactory(Map<String,String> args) {
    super(args);
  }

  @Override
  public TokenStream create(TokenStream input) {
    return new FixBrokenOffsetsFilter(input);
  }
}
diff --git a/lucene/analysis/common/src/resources/META-INF/services/org.apache.lucene.analysis.util.TokenFilterFactory b/lucene/analysis/common/src/resources/META-INF/services/org.apache.lucene.analysis.util.TokenFilterFactory
index 73986d73fec..5f8894cb02b 100644
-- a/lucene/analysis/common/src/resources/META-INF/services/org.apache.lucene.analysis.util.TokenFilterFactory
++ b/lucene/analysis/common/src/resources/META-INF/services/org.apache.lucene.analysis.util.TokenFilterFactory
@@ -64,6 +64,7 @@ org.apache.lucene.analysis.miscellaneous.CapitalizationFilterFactory
 org.apache.lucene.analysis.miscellaneous.CodepointCountFilterFactory
 org.apache.lucene.analysis.miscellaneous.DateRecognizerFilterFactory
 org.apache.lucene.analysis.miscellaneous.FingerprintFilterFactory
org.apache.lucene.analysis.miscellaneous.FixBrokenOffsetsFilterFactory
 org.apache.lucene.analysis.miscellaneous.HyphenatedWordsFilterFactory
 org.apache.lucene.analysis.miscellaneous.KeepWordFilterFactory
 org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilterFactory
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestFixBrokenOffsetsFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestFixBrokenOffsetsFilter.java
new file mode 100644
index 00000000000..ada5014334f
-- /dev/null
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestFixBrokenOffsetsFilter.java
@@ -0,0 +1,50 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.analysis.miscellaneous;

import java.io.IOException;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.CannedTokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

public class TestFixBrokenOffsetsFilter extends BaseTokenStreamTestCase {

  public void testBogusTermVectors() throws IOException {
    Directory dir = newDirectory();
    IndexWriter iw = new IndexWriter(dir, newIndexWriterConfig(null));
    Document doc = new Document();
    FieldType ft = new FieldType(TextField.TYPE_NOT_STORED);
    ft.setStoreTermVectors(true);
    ft.setStoreTermVectorOffsets(true);
    Field field = new Field("foo", "", ft);
    field.setTokenStream(new FixBrokenOffsetsFilter(new CannedTokenStream(
        new Token("bar", 5, 10), new Token("bar", 1, 4)
        )));
    doc.add(field);
    iw.addDocument(doc);
    iw.close();
    dir.close();
  }
}
diff --git a/lucene/backward-codecs/src/java/org/apache/lucene/index/FixBrokenOffsets.java b/lucene/backward-codecs/src/java/org/apache/lucene/index/FixBrokenOffsets.java
new file mode 100644
index 00000000000..d4d6f85430b
-- /dev/null
++ b/lucene/backward-codecs/src/java/org/apache/lucene/index/FixBrokenOffsets.java
@@ -0,0 +1,125 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.SuppressForbidden;

/**
 * Command-line tool that reads from a source index and
 * writes to a dest index, correcting any broken offsets
 * in the process.
 *
 * @lucene.experimental
 */
public class FixBrokenOffsets {
  public SegmentInfos infos;

  FSDirectory fsDir;

  Path dir;

  @SuppressForbidden(reason = "System.out required: command line tool")
  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Usage: FixBrokenOffsetse <srcDir> <destDir>");
      return;
    }
    Path srcPath = Paths.get(args[0]);
    if (!Files.exists(srcPath)) {
      throw new RuntimeException("srcPath " + srcPath.toAbsolutePath() + " doesn't exist");
    }
    Path destPath = Paths.get(args[1]);
    if (Files.exists(destPath)) {
      throw new RuntimeException("destPath " + destPath.toAbsolutePath() + " already exists; please remove it and re-run");
    }
    Directory srcDir = FSDirectory.open(srcPath);
    DirectoryReader reader = DirectoryReader.open(srcDir);

    List<LeafReaderContext> leaves = reader.leaves();
    CodecReader[] filtered = new CodecReader[leaves.size()];
    for(int i=0;i<leaves.size();i++) {
      filtered[i] = SlowCodecReaderWrapper.wrap(new FilterLeafReader(leaves.get(i).reader()) {
          @Override
          public Fields getTermVectors(int docID) throws IOException {
            Fields termVectors = in.getTermVectors(docID);
            if (termVectors == null) {
              return null;
            }
            return new FilterFields(termVectors) {
              @Override
              public Terms terms(String field) throws IOException {
                return new FilterTerms(super.terms(field)) {
                  @Override
                  public TermsEnum iterator() throws IOException {
                    return new FilterTermsEnum(super.iterator()) {
                      @Override
                      public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
                        return new FilterPostingsEnum(super.postings(reuse, flags)) {
                          int nextLastStartOffset = 0;
                          int lastStartOffset = 0;

                          @Override
                          public int nextPosition() throws IOException {
                            int pos = super.nextPosition();
                            lastStartOffset = nextLastStartOffset;
                            nextLastStartOffset = startOffset();
                            return pos;
                          }
                          
                          @Override
                          public int startOffset() throws IOException {
                            int offset = super.startOffset();
                            if (offset < lastStartOffset) {
                              offset = lastStartOffset;
                            }
                            return offset;
                          }
                          
                          @Override
                          public int endOffset() throws IOException {
                            int offset = super.endOffset();
                            if (offset < lastStartOffset) {
                              offset = lastStartOffset;
                            }
                            return offset;
                          }
                        };
                      }
                    };
                  }
                };
              }
            };
          }
        });
    }

    Directory destDir = FSDirectory.open(destPath);
    IndexWriter writer = new IndexWriter(destDir, new IndexWriterConfig());
    writer.addIndexes(filtered);
    IOUtils.close(writer, reader, srcDir, destDir);
  }
}
diff --git a/lucene/backward-codecs/src/java/org/apache/lucene/index/package.html b/lucene/backward-codecs/src/java/org/apache/lucene/index/package.html
new file mode 100644
index 00000000000..42ff91af613
-- /dev/null
++ b/lucene/backward-codecs/src/java/org/apache/lucene/index/package.html
@@ -0,0 +1,27 @@
<!doctype html public "-//w3c//dtd html 4.0 transitional//en">
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<!-- not a package-info.java, because we already defined this package in core/ -->
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
  <title>Tools for handling backwards compatibility issues with indices.</title>
</head>
<body>
Tools for handling backwards compatibility issues with indices.
</body>
</html>
diff --git a/lucene/backward-codecs/src/test/org/apache/lucene/index/TestFixBrokenOffsets.java b/lucene/backward-codecs/src/test/org/apache/lucene/index/TestFixBrokenOffsets.java
new file mode 100644
index 00000000000..bcd5a65aee8
-- /dev/null
++ b/lucene/backward-codecs/src/test/org/apache/lucene/index/TestFixBrokenOffsets.java
@@ -0,0 +1,114 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.index;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MockDirectoryWrapper;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;

public class TestFixBrokenOffsets extends LuceneTestCase {

  // Run this in Lucene 6.x:
  //
  //     ant test -Dtestcase=TestFixBrokenOffsets -Dtestmethod=testCreateBrokenOffsetsIndex -Dtests.codec=default -Dtests.useSecurityManager=false
  /*
  public void testCreateBrokenOffsetsIndex() throws IOException {

    Path indexDir = Paths.get("/tmp/brokenoffsets");
    Files.deleteIfExists(indexDir);
    Directory dir = newFSDirectory(indexDir);
    IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig());

    Document doc = new Document();
    FieldType fieldType = new FieldType(TextField.TYPE_STORED);
    fieldType.setStoreTermVectors(true);
    fieldType.setStoreTermVectorPositions(true);
    fieldType.setStoreTermVectorOffsets(true);
    Field field = new Field("foo", "bar", fieldType);
    field.setTokenStream(new CannedTokenStream(new Token("foo", 10, 13), new Token("foo", 7, 9)));
    doc.add(field);
    writer.addDocument(doc);
    writer.commit();

    // 2nd segment
    doc = new Document();
    field = new Field("foo", "bar", fieldType);
    field.setTokenStream(new CannedTokenStream(new Token("bar", 15, 17), new Token("bar", 1, 5)));
    doc.add(field);
    writer.addDocument(doc);
    
    writer.close();

    dir.close();
  }
  */

  public void testFixBrokenOffsetsIndex() throws IOException {
    InputStream resource = getClass().getResourceAsStream("index.630.brokenoffsets.zip");
    assertNotNull("Broken offsets index not found", resource);
    Path path = createTempDir("brokenoffsets");
    TestUtil.unzip(resource, path);
    Directory dir = FSDirectory.open(path);

    // OK: index is 6.3.0 so offsets not checked:
    TestUtil.checkIndex(dir);
    
    MockDirectoryWrapper tmpDir = newMockDirectory();
    tmpDir.setCheckIndexOnClose(false);
    IndexWriter w = new IndexWriter(tmpDir, new IndexWriterConfig());
    w.addIndexes(dir);
    w.close();
    // OK: addIndexes(Directory...) also keeps version as 6.3.0, so offsets not checked:
    TestUtil.checkIndex(tmpDir);
    tmpDir.close();

    final MockDirectoryWrapper tmpDir2 = newMockDirectory();
    tmpDir2.setCheckIndexOnClose(false);
    w = new IndexWriter(tmpDir2, new IndexWriterConfig());
    DirectoryReader reader = DirectoryReader.open(dir);
    List<LeafReaderContext> leaves = reader.leaves();
    CodecReader[] codecReaders = new CodecReader[leaves.size()];
    for(int i=0;i<leaves.size();i++) {
      codecReaders[i] = (CodecReader) leaves.get(i).reader();
    }
    w.addIndexes(codecReaders);
    w.close();

    // NOT OK: broken offsets were copied into a 7.0 segment:
    ByteArrayOutputStream output = new ByteArrayOutputStream(1024);    
    RuntimeException re = expectThrows(RuntimeException.class, () -> {TestUtil.checkIndex(tmpDir2, false, true, output);});
    assertEquals("term [66 6f 6f]: doc 0: pos 1: startOffset 7 < lastStartOffset 10; consider using the FixBrokenOffsets tool in Lucene's backward-codecs module to correct your index", re.getMessage());
    tmpDir2.close();

    // Now run the tool and confirm the broken offsets are fixed:
    Path path2 = createTempDir("fixedbrokenoffsets").resolve("subdir");
    FixBrokenOffsets.main(new String[] {path.toString(), path2.toString()});
    Directory tmpDir3 = FSDirectory.open(path2);
    TestUtil.checkIndex(tmpDir3);
    tmpDir3.close();
    
    dir.close();
  }
}
diff --git a/lucene/backward-codecs/src/test/org/apache/lucene/index/index.630.brokenoffsets.zip b/lucene/backward-codecs/src/test/org/apache/lucene/index/index.630.brokenoffsets.zip
new file mode 100644
index 0000000000000000000000000000000000000000..3cf476a07d5a39f253f52c85b5f2512b698ed06d
GIT binary patch
literal 3203
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
AzyJUM

literal 0
HcmV?d00001

diff --git a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
index fd8011d4d07..3bb10d325b5 100644
-- a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
++ b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
@@ -740,13 +740,13 @@ public final class CheckIndex implements Closeable {
           segInfoStat.fieldNormStatus = testFieldNorms(reader, infoStream, failFast);
 
           // Test the Term Index
          segInfoStat.termIndexStatus = testPostings(reader, infoStream, verbose, failFast);
          segInfoStat.termIndexStatus = testPostings(reader, infoStream, verbose, failFast, version);
 
           // Test Stored Fields
           segInfoStat.storedFieldStatus = testStoredFields(reader, infoStream, failFast);
 
           // Test Term Vectors
          segInfoStat.termVectorStatus = testTermVectors(reader, infoStream, verbose, crossCheckTermVectors, failFast);
          segInfoStat.termVectorStatus = testTermVectors(reader, infoStream, verbose, crossCheckTermVectors, failFast, version);
 
           // Test Docvalues
           segInfoStat.docValuesStatus = testDocValues(reader, infoStream, failFast);
@@ -1205,7 +1205,7 @@ public final class CheckIndex implements Closeable {
    * checks Fields api is consistent with itself.
    * searcher is optional, to verify with queries. Can be null.
    */
  private static Status.TermIndexStatus checkFields(Fields fields, Bits liveDocs, int maxDoc, FieldInfos fieldInfos, boolean doPrint, boolean isVectors, PrintStream infoStream, boolean verbose) throws IOException {
  private static Status.TermIndexStatus checkFields(Fields fields, Bits liveDocs, int maxDoc, FieldInfos fieldInfos, boolean doPrint, boolean isVectors, PrintStream infoStream, boolean verbose, Version version) throws IOException {
     // TODO: we should probably return our own stats thing...?!
     long startNS;
     if (doPrint) {
@@ -1461,14 +1461,13 @@ public final class CheckIndex implements Closeable {
               if (hasOffsets) {
                 int startOffset = postings.startOffset();
                 int endOffset = postings.endOffset();
                // NOTE: we cannot enforce any bounds whatsoever on vectors... they were a free-for-all before?
                // but for offsets in the postings lists these checks are fine: they were always enforced by IndexWriter
                if (!isVectors) {
                // In Lucene 7 we fixed IndexWriter to also enforce term vector offsets
                if (isVectors == false || version.onOrAfter(Version.LUCENE_7_0_0)) {
                   if (startOffset < 0) {
                     throw new RuntimeException("term " + term + ": doc " + doc + ": pos " + pos + ": startOffset " + startOffset + " is out of bounds");
                   }
                   if (startOffset < lastOffset) {
                    throw new RuntimeException("term " + term + ": doc " + doc + ": pos " + pos + ": startOffset " + startOffset + " < lastStartOffset " + lastOffset);
                    throw new RuntimeException("term " + term + ": doc " + doc + ": pos " + pos + ": startOffset " + startOffset + " < lastStartOffset " + lastOffset + "; consider using the FixBrokenOffsets tool in Lucene's backward-codecs module to correct your index");
                   }
                   if (endOffset < 0) {
                     throw new RuntimeException("term " + term + ": doc " + doc + ": pos " + pos + ": endOffset " + endOffset + " is out of bounds");
@@ -1742,15 +1741,15 @@ public final class CheckIndex implements Closeable {
    * Test the term index.
    * @lucene.experimental
    */
  public static Status.TermIndexStatus testPostings(CodecReader reader, PrintStream infoStream) throws IOException {
    return testPostings(reader, infoStream, false, false);
  public static Status.TermIndexStatus testPostings(CodecReader reader, PrintStream infoStream, Version version) throws IOException {
    return testPostings(reader, infoStream, false, false, version);
   }
   
   /**
    * Test the term index.
    * @lucene.experimental
    */
  public static Status.TermIndexStatus testPostings(CodecReader reader, PrintStream infoStream, boolean verbose, boolean failFast) throws IOException {
  public static Status.TermIndexStatus testPostings(CodecReader reader, PrintStream infoStream, boolean verbose, boolean failFast, Version version) throws IOException {
 
     // TODO: we should go and verify term vectors match, if
     // crossCheckTermVectors is on...
@@ -1765,7 +1764,7 @@ public final class CheckIndex implements Closeable {
 
       final Fields fields = reader.getPostingsReader().getMergeInstance();
       final FieldInfos fieldInfos = reader.getFieldInfos();
      status = checkFields(fields, reader.getLiveDocs(), maxDoc, fieldInfos, true, false, infoStream, verbose);
      status = checkFields(fields, reader.getLiveDocs(), maxDoc, fieldInfos, true, false, infoStream, verbose, version);
     } catch (Throwable e) {
       if (failFast) {
         IOUtils.reThrow(e);
@@ -2339,15 +2338,15 @@ public final class CheckIndex implements Closeable {
    * Test term vectors.
    * @lucene.experimental
    */
  public static Status.TermVectorStatus testTermVectors(CodecReader reader, PrintStream infoStream) throws IOException {
    return testTermVectors(reader, infoStream, false, false, false);
  public static Status.TermVectorStatus testTermVectors(CodecReader reader, PrintStream infoStream, Version version) throws IOException {
    return testTermVectors(reader, infoStream, false, false, false, version);
   }
 
   /**
    * Test term vectors.
    * @lucene.experimental
    */
  public static Status.TermVectorStatus testTermVectors(CodecReader reader, PrintStream infoStream, boolean verbose, boolean crossCheckTermVectors, boolean failFast) throws IOException {
  public static Status.TermVectorStatus testTermVectors(CodecReader reader, PrintStream infoStream, boolean verbose, boolean crossCheckTermVectors, boolean failFast, Version version) throws IOException {
     long startNS = System.nanoTime();
     final Status.TermVectorStatus status = new Status.TermVectorStatus();
     final FieldInfos fieldInfos = reader.getFieldInfos();
@@ -2387,7 +2386,7 @@ public final class CheckIndex implements Closeable {
           
           if (tfv != null) {
             // First run with no deletions:
            checkFields(tfv, null, 1, fieldInfos, false, true, infoStream, verbose);
            checkFields(tfv, null, 1, fieldInfos, false, true, infoStream, verbose, version);
             
             // Only agg stats if the doc is live:
             final boolean doStats = liveDocs == null || liveDocs.get(j);
diff --git a/lucene/core/src/java/org/apache/lucene/index/DefaultIndexingChain.java b/lucene/core/src/java/org/apache/lucene/index/DefaultIndexingChain.java
index 79c285b4936..197ab3155f9 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DefaultIndexingChain.java
++ b/lucene/core/src/java/org/apache/lucene/index/DefaultIndexingChain.java
@@ -27,6 +27,7 @@ import java.util.Map;
 import java.util.Set;
 
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.codecs.DocValuesConsumer;
 import org.apache.lucene.codecs.DocValuesFormat;
 import org.apache.lucene.codecs.NormsConsumer;
@@ -728,10 +729,6 @@ final class DefaultIndexingChain extends DocConsumer {
 
       final boolean analyzed = fieldType.tokenized() && docState.analyzer != null;
         
      // only bother checking offsets if something will consume them.
      // TODO: after we fix analyzers, also check if termVectorOffsets will be indexed.
      final boolean checkOffsets = indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;

       /*
        * To assist people in tracking down problems in analysis components, we wish to write the field name to the infostream
        * when we fail. We expect some caller to eventually deal with the real exception, so we don't want any 'catch' clauses,
@@ -743,6 +740,7 @@ final class DefaultIndexingChain extends DocConsumer {
         stream.reset();
         invertState.setAttributeSource(stream);
         termsHashPerField.start(field, first);
        CharTermAttribute termAtt = tokenStream.getAttribute(CharTermAttribute.class);
 
         while (stream.incrementToken()) {
 
@@ -771,15 +769,13 @@ final class DefaultIndexingChain extends DocConsumer {
             invertState.numOverlap++;
           }
               
          if (checkOffsets) {
            int startOffset = invertState.offset + invertState.offsetAttribute.startOffset();
            int endOffset = invertState.offset + invertState.offsetAttribute.endOffset();
            if (startOffset < invertState.lastStartOffset || endOffset < startOffset) {
              throw new IllegalArgumentException("startOffset must be non-negative, and endOffset must be >= startOffset, and offsets must not go backwards "
                                                 + "startOffset=" + startOffset + ",endOffset=" + endOffset + ",lastStartOffset=" + invertState.lastStartOffset + " for field '" + field.name() + "'");
            }
            invertState.lastStartOffset = startOffset;
          int startOffset = invertState.offset + invertState.offsetAttribute.startOffset();
          int endOffset = invertState.offset + invertState.offsetAttribute.endOffset();
          if (startOffset < invertState.lastStartOffset || endOffset < startOffset) {
            throw new IllegalArgumentException("startOffset must be non-negative, and endOffset must be >= startOffset, and offsets must not go backwards "
                                               + "startOffset=" + startOffset + ",endOffset=" + endOffset + ",lastStartOffset=" + invertState.lastStartOffset + " for field '" + field.name() + "'");
           }
          invertState.lastStartOffset = startOffset;
 
           invertState.length++;
           if (invertState.length < 0) {
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestCheckIndex.java b/lucene/core/src/test/org/apache/lucene/index/TestCheckIndex.java
index 7b71d3c5cfc..2559ce4d663 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestCheckIndex.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestCheckIndex.java
@@ -42,11 +42,6 @@ public class TestCheckIndex extends BaseTestCheckIndex {
     testDeletedDocs(directory);
   }
   
  @Test
  public void testBogusTermVectors() throws IOException {
    testBogusTermVectors(directory);
  }
  
   @Test
   public void testChecksumsOnly() throws IOException {
     testChecksumsOnly(directory);
diff --git a/lucene/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java b/lucene/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java
index 581ff2f77e7..d49434a248e 100644
-- a/lucene/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java
++ b/lucene/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java
@@ -377,7 +377,7 @@ public class TokenSourcesTest extends BaseTokenStreamTestCase {
     }
 
     final BaseTermVectorsFormatTestCase.RandomTokenStream rTokenStream =
        new BaseTermVectorsFormatTestCase.RandomTokenStream(TestUtil.nextInt(random(), 1, 10), terms, termBytes, false);
        new BaseTermVectorsFormatTestCase.RandomTokenStream(TestUtil.nextInt(random(), 1, 10), terms, termBytes);
     //check to see if the token streams might have non-deterministic testable result
     final boolean storeTermVectorPositions = random().nextBoolean();
     final int[] startOffsets = rTokenStream.getStartOffsets();
diff --git a/lucene/sandbox/src/test/org/apache/lucene/search/TestTermAutomatonQuery.java b/lucene/sandbox/src/test/org/apache/lucene/search/TestTermAutomatonQuery.java
index 6055e0076cb..6ef9baf284e 100644
-- a/lucene/sandbox/src/test/org/apache/lucene/search/TestTermAutomatonQuery.java
++ b/lucene/sandbox/src/test/org/apache/lucene/search/TestTermAutomatonQuery.java
@@ -45,6 +45,7 @@ import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.store.Directory;
import org.apache.lucene.util.AttributeSource;
 import org.apache.lucene.util.BitSetIterator;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.FixedBitSet;
@@ -431,7 +432,9 @@ public class TestTermAutomatonQuery extends LuceneTestCase {
     @Override
     public boolean incrementToken() throws IOException {
       if (synNext) {
        AttributeSource.State state = captureState();
         clearAttributes();
        restoreState(state);
         posIncAtt.setPositionIncrement(0);
         termAtt.append(""+((char) 97 + random().nextInt(3)));
         synNext = false;
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/BaseTermVectorsFormatTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/index/BaseTermVectorsFormatTestCase.java
index 5e6809ff703..7acee871f59 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/BaseTermVectorsFormatTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/BaseTermVectorsFormatTestCase.java
@@ -200,10 +200,6 @@ public abstract class BaseTermVectorsFormatTestCase extends BaseIndexFileFormatT
     int i = 0;
 
     public RandomTokenStream(int len, String[] sampleTerms, BytesRef[] sampleTermBytes) {
      this(len, sampleTerms, sampleTermBytes, rarely());
    }

    public RandomTokenStream(int len, String[] sampleTerms, BytesRef[] sampleTermBytes, boolean offsetsGoBackwards) {
       terms = new String[len];
       termBytes = new BytesRef[len];
       positionsIncrements = new int[len];
@@ -216,17 +212,12 @@ public abstract class BaseTermVectorsFormatTestCase extends BaseIndexFileFormatT
         terms[i] = sampleTerms[o];
         termBytes[i] = sampleTermBytes[o];
         positionsIncrements[i] = TestUtil.nextInt(random(), i == 0 ? 1 : 0, 10);
        if (offsetsGoBackwards) {
          startOffsets[i] = random().nextInt();
          endOffsets[i] = random().nextInt();
        if (i == 0) {
          startOffsets[i] = TestUtil.nextInt(random(), 0, 1 << 16);
         } else {
          if (i == 0) {
            startOffsets[i] = TestUtil.nextInt(random(), 0, 1 << 16);
          } else {
            startOffsets[i] = startOffsets[i-1] + TestUtil.nextInt(random(), 0, rarely() ? 1 << 16 : 20);
          }
          endOffsets[i] = startOffsets[i] + TestUtil.nextInt(random(), 0, rarely() ? 1 << 10 : 20);
          startOffsets[i] = startOffsets[i-1] + TestUtil.nextInt(random(), 0, rarely() ? 1 << 16 : 20);
         }
        endOffsets[i] = startOffsets[i] + TestUtil.nextInt(random(), 0, rarely() ? 1 << 10 : 20);
       }
 
       for (int i = 0; i < len; ++i) {
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/BaseTestCheckIndex.java b/lucene/test-framework/src/java/org/apache/lucene/index/BaseTestCheckIndex.java
index cdec720f134..21ccf3b777f 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/BaseTestCheckIndex.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/BaseTestCheckIndex.java
@@ -22,11 +22,8 @@ import java.io.PrintStream;
 import java.util.ArrayList;
 import java.util.List;
 
import org.apache.lucene.analysis.CannedTokenStream;
 import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.Token;
 import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
 import org.apache.lucene.document.FieldType;
 import org.apache.lucene.document.TextField;
 import org.apache.lucene.store.Directory;
@@ -105,22 +102,6 @@ public class BaseTestCheckIndex extends LuceneTestCase {
     checker.close();
   }
   
  // LUCENE-4221: we have to let these thru, for now
  public void testBogusTermVectors(Directory dir) throws IOException {
    IndexWriter iw = new IndexWriter(dir, newIndexWriterConfig(null));
    Document doc = new Document();
    FieldType ft = new FieldType(TextField.TYPE_NOT_STORED);
    ft.setStoreTermVectors(true);
    ft.setStoreTermVectorOffsets(true);
    Field field = new Field("foo", "", ft);
    field.setTokenStream(new CannedTokenStream(
        new Token("bar", 5, 10), new Token("bar", 1, 4)
    ));
    doc.add(field);
    iw.addDocument(doc);
    iw.close();
  }
  
   public void testChecksumsOnly(Directory dir) throws IOException {
     LineFileDocs lf = new LineFileDocs(random());
     MockAnalyzer analyzer = new MockAnalyzer(random());
diff --git a/lucene/test-framework/src/java/org/apache/lucene/util/TestUtil.java b/lucene/test-framework/src/java/org/apache/lucene/util/TestUtil.java
index d3351ab9f63..0ea90fc04eb 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/util/TestUtil.java
++ b/lucene/test-framework/src/java/org/apache/lucene/util/TestUtil.java
@@ -334,9 +334,9 @@ public final class TestUtil {
     CheckIndex.testLiveDocs(codecReader, infoStream, true);
     CheckIndex.testFieldInfos(codecReader, infoStream, true);
     CheckIndex.testFieldNorms(codecReader, infoStream, true);
    CheckIndex.testPostings(codecReader, infoStream, false, true);
    CheckIndex.testPostings(codecReader, infoStream, false, true, Version.LUCENE_7_0_0);
     CheckIndex.testStoredFields(codecReader, infoStream, true);
    CheckIndex.testTermVectors(codecReader, infoStream, false, crossCheckTermVectors, true);
    CheckIndex.testTermVectors(codecReader, infoStream, false, crossCheckTermVectors, true, Version.LUCENE_7_0_0);
     CheckIndex.testDocValues(codecReader, infoStream, true);
     CheckIndex.testPoints(codecReader, infoStream, true);
     
diff --git a/solr/core/src/java/org/apache/solr/schema/PreAnalyzedField.java b/solr/core/src/java/org/apache/solr/schema/PreAnalyzedField.java
index 87d40940e4c..5f125d95428 100644
-- a/solr/core/src/java/org/apache/solr/schema/PreAnalyzedField.java
++ b/solr/core/src/java/org/apache/solr/schema/PreAnalyzedField.java
@@ -27,6 +27,7 @@ import java.util.Map;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexOptions;
 import org.apache.lucene.index.IndexableField;
@@ -284,6 +285,7 @@ public class PreAnalyzedField extends TextField implements HasImplicitIndexAnaly
     private byte[] binaryValue = null;
     private PreAnalyzedParser parser;
     private IOException readerConsumptionException;
    private int lastEndOffset;
 
     public PreAnalyzedTokenizer(PreAnalyzedParser parser) {
       // we don't pack attributes: since we are used for (de)serialization and dont want bloat.
@@ -311,6 +313,8 @@ public class PreAnalyzedField extends TextField implements HasImplicitIndexAnaly
       
       AttributeSource.State state = it.next();
       restoreState(state.clone());
      // TODO: why can't I lookup the OffsetAttribute up in ctor instead?
      lastEndOffset = addAttribute(OffsetAttribute.class).endOffset();
       return true;
     }
 
@@ -329,6 +333,13 @@ public class PreAnalyzedField extends TextField implements HasImplicitIndexAnaly
       it = cachedStates.iterator();
     }
 
    @Override
    public void end() throws IOException {
      super.end();
      // we must set the end offset correctly so multi-valued fields don't try to send offsets backwards:
      addAttribute(OffsetAttribute.class).setOffset(lastEndOffset, lastEndOffset);
    }

     private void setReaderConsumptionException(IOException e) {
       readerConsumptionException = e;
     }
diff --git a/solr/core/src/test/org/apache/solr/index/hdfs/CheckHdfsIndexTest.java b/solr/core/src/test/org/apache/solr/index/hdfs/CheckHdfsIndexTest.java
index b4f69310760..61b430530e2 100644
-- a/solr/core/src/test/org/apache/solr/index/hdfs/CheckHdfsIndexTest.java
++ b/solr/core/src/test/org/apache/solr/index/hdfs/CheckHdfsIndexTest.java
@@ -120,11 +120,6 @@ public class CheckHdfsIndexTest extends AbstractFullDistribZkTestBase {
     testCheckIndex.testDeletedDocs(directory);
   }
 
  @Test
  public void testBogusTermVectors() throws IOException {
    testCheckIndex.testBogusTermVectors(directory);
  }

   @Test
   public void testChecksumsOnly() throws IOException {
     testCheckIndex.testChecksumsOnly(directory);
- 
2.19.1.windows.1

