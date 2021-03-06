From e9a586677731eb6634f731c63a81012b9db46390 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Mon, 19 Nov 2007 19:10:37 +0000
Subject: [PATCH] LUCENE-1057: call clear when reusing token, change clear to
 only resent essential fields, re-add Token.clone()

git-svn-id: https://svn.apache.org/repos/asf/lucene/java/trunk@596398 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/lucene/analysis/Token.java     | 21 ++++-
 .../apache/lucene/index/DocumentsWriter.java  |  6 +-
 .../lucene/index/TestDocumentWriter.java      | 83 ++++++++++++++++---
 3 files changed, 94 insertions(+), 16 deletions(-)

diff --git a/src/java/org/apache/lucene/analysis/Token.java b/src/java/org/apache/lucene/analysis/Token.java
index 873b44512c5..3bf67dfb3e1 100644
-- a/src/java/org/apache/lucene/analysis/Token.java
++ b/src/java/org/apache/lucene/analysis/Token.java
@@ -361,14 +361,29 @@ public class Token implements Cloneable {
     return sb.toString();
   }
 
  /** Reset all state for this token back to defaults. */
  /** Resets the term text, payload, and positionIncrement to default.
   * Other fields such as startOffset, endOffset and the token type are
   * not reset since they are normally overwritten by the tokenizer. */
   public void clear() {
     payload = null;
     // Leave termBuffer to allow re-use
     termLength = 0;
     termText = null;
     positionIncrement = 1;
    startOffset = endOffset = 0;
    type = DEFAULT_TYPE;
    // startOffset = endOffset = 0;
    // type = DEFAULT_TYPE;
  }

  public Object clone() {
    try {
      Token t = (Token)super.clone();
      if (termBuffer != null) {
        t.termBuffer = null;
        t.setTermBuffer(termBuffer, 0, termLength);
      }
      return t;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);  // shouldn't happen
    }
   }
 }
diff --git a/src/java/org/apache/lucene/index/DocumentsWriter.java b/src/java/org/apache/lucene/index/DocumentsWriter.java
index e8b1fa310a4..e354235ad64 100644
-- a/src/java/org/apache/lucene/index/DocumentsWriter.java
++ b/src/java/org/apache/lucene/index/DocumentsWriter.java
@@ -1281,6 +1281,7 @@ final class DocumentsWriter {
         if (!field.isTokenized()) {		  // un-tokenized field
           String stringValue = field.stringValue();
           Token token = localToken;
          token.clear();
           token.setTermText(stringValue);
           token.setStartOffset(offset);
           token.setEndOffset(offset + stringValue.length());
@@ -1319,7 +1320,10 @@ final class DocumentsWriter {
           try {
             offsetEnd = offset-1;
             Token token;
            while((token = stream.next(localToken)) != null) {
            for(;;) {
              localToken.clear();
              token = stream.next(localToken);
              if (token == null) break;
               position += (token.getPositionIncrement() - 1);
               addPosition(token);
               if (++length >= maxFieldLength) {
diff --git a/src/test/org/apache/lucene/index/TestDocumentWriter.java b/src/test/org/apache/lucene/index/TestDocumentWriter.java
index ea5961015b5..1a42d604150 100644
-- a/src/test/org/apache/lucene/index/TestDocumentWriter.java
++ b/src/test/org/apache/lucene/index/TestDocumentWriter.java
@@ -17,22 +17,17 @@ package org.apache.lucene.index;
  * limitations under the License.
  */
 
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
 import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;
 
import java.io.Reader;
 import java.io.IOException;

import java.util.Arrays;
import java.io.Reader;
 
 public class TestDocumentWriter extends LuceneTestCase {
   private RAMDirectory dir;
@@ -130,7 +125,71 @@ public class TestDocumentWriter extends LuceneTestCase {
     assertEquals(0, termPositions.nextPosition());
     assertEquals(502, termPositions.nextPosition());
   }
  

  public void testTokenReuse() throws IOException {
    Analyzer analyzer = new Analyzer() {
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new TokenFilter(new WhitespaceTokenizer(reader)) {
          boolean first=true;
          Token buffered;

          public Token next() throws IOException {
            return input.next();
          }

          public Token next(Token result) throws IOException {
            if (buffered != null) {
              Token t = buffered;
              buffered=null;
              return t;
            }
            Token t = input.next(result);
            if (t==null) return null;
            if (Character.isDigit(t.termBuffer()[0])) {
              t.setPositionIncrement(t.termBuffer()[0] - '0');
            }
            if (first) {
              // set payload on first position only
              t.setPayload(new Payload(new byte[]{100}));
              first = false;
            }

            // index a "synonym" for every token
            buffered = (Token)t.clone();
            buffered.setPayload(null);
            buffered.setPositionIncrement(0);
            buffered.setTermBuffer(new char[]{'b'}, 0, 1);

            return t;
          }
        };
      }
    };

    IndexWriter writer = new IndexWriter(dir, analyzer, true);

    Document doc = new Document();
    doc.add(new Field("f1", "a 5 a a", Field.Store.YES, Field.Index.TOKENIZED));

    writer.addDocument(doc);
    writer.flush();
    SegmentInfo info = writer.newestSegment();
    writer.close();
    SegmentReader reader = SegmentReader.get(info);

    TermPositions termPositions = reader.termPositions(new Term("f1", "a"));
    assertTrue(termPositions.next());
    int freq = termPositions.freq();
    assertEquals(3, freq);
    assertEquals(0, termPositions.nextPosition());
    assertEquals(true, termPositions.isPayloadAvailable());
    assertEquals(6, termPositions.nextPosition());
    assertEquals(false, termPositions.isPayloadAvailable());
    assertEquals(7, termPositions.nextPosition());
    assertEquals(false, termPositions.isPayloadAvailable());
  }


   public void testPreAnalyzedField() throws IOException {
     Similarity similarity = Similarity.getDefault();
     IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), true);
- 
2.19.1.windows.1

