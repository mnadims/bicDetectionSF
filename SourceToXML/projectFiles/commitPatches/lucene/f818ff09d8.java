From f818ff09d88c220dbf3eb260772ab9982019fb2c Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Mon, 19 Nov 2007 20:45:09 +0000
Subject: [PATCH] LUCENE-1057: copy payload in Token.next()

git-svn-id: https://svn.apache.org/repos/asf/lucene/java/trunk@596440 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/lucene/analysis/TokenStream.java   |  9 +++
 .../apache/lucene/analysis/TestAnalyzers.java | 68 +++++++++++++++++++
 2 files changed, 77 insertions(+)

diff --git a/src/java/org/apache/lucene/analysis/TokenStream.java b/src/java/org/apache/lucene/analysis/TokenStream.java
index 61bbe3a0409..def4e3e9b27 100644
-- a/src/java/org/apache/lucene/analysis/TokenStream.java
++ b/src/java/org/apache/lucene/analysis/TokenStream.java
@@ -17,6 +17,8 @@ package org.apache.lucene.analysis;
  * limitations under the License.
  */
 
import org.apache.lucene.index.Payload;

 import java.io.IOException;
 
 /** A TokenStream enumerates the sequence of tokens, either from
@@ -41,6 +43,13 @@ public abstract class TokenStream {
    *  than calling {@link #next(Token)} instead.. */
   public Token next() throws IOException {
     Token result = next(new Token());

    if (result != null) {
      Payload p = result.getPayload();
      if (p != null)
        result.setPayload(new Payload(p.toByteArray(), 0, p.length()));
    }

     return result;
   }
 
diff --git a/src/test/org/apache/lucene/analysis/TestAnalyzers.java b/src/test/org/apache/lucene/analysis/TestAnalyzers.java
index 76725f06bcf..163d8578ef8 100644
-- a/src/test/org/apache/lucene/analysis/TestAnalyzers.java
++ b/src/test/org/apache/lucene/analysis/TestAnalyzers.java
@@ -18,7 +18,11 @@ package org.apache.lucene.analysis;
  */
 
 import java.io.*;
import java.util.List;
import java.util.LinkedList;

 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.index.Payload;
 
 public class TestAnalyzers extends LuceneTestCase {
 
@@ -86,5 +90,69 @@ public class TestAnalyzers extends LuceneTestCase {
     assertAnalyzesTo(a, "foo a bar such FOO THESE BAR", 
                      new String[] { "foo", "bar", "foo", "bar" });
   }

  void verifyPayload(TokenStream ts) throws IOException {
    Token t = new Token();
    for(byte b=1;;b++) {
      t.clear();
      t = ts.next(t);
      if (t==null) break;
      // System.out.println("id="+System.identityHashCode(t) + " " + t);
      // System.out.println("payload=" + (int)t.getPayload().toByteArray()[0]);
      assertEquals(b, t.getPayload().toByteArray()[0]);
    }
  }

  // Make sure old style next() calls result in a new copy of payloads
  public void testPayloadCopy() throws IOException {
    String s = "how now brown cow";
    TokenStream ts;
    ts = new WhitespaceTokenizer(new StringReader(s));
    ts = new BuffTokenFilter(ts);
    ts = new PayloadSetter(ts);
    verifyPayload(ts);

    ts = new WhitespaceTokenizer(new StringReader(s));
    ts = new PayloadSetter(ts);
    ts = new BuffTokenFilter(ts);
    verifyPayload(ts);
  }

 }
 
class BuffTokenFilter extends TokenFilter {
  List lst;

  public BuffTokenFilter(TokenStream input) {
    super(input);
  }

  public Token next() throws IOException {
    if (lst == null) {
      lst = new LinkedList<Token>();
      for(;;) {
        Token t = input.next();
        if (t==null) break;
        lst.add(t);
      }
    }
    return lst.size()==0 ? null : (Token)lst.remove(0);
  }
}

class PayloadSetter extends TokenFilter {
  public  PayloadSetter(TokenStream input) {
    super(input);
  }

  byte[] data = new byte[1];
  Payload p = new Payload(data,0,1);

  public Token next(Token target) throws IOException {
    target = input.next(target);
    if (target==null) return null;
    target.setPayload(p);  // reuse the payload / byte[]
    data[0]++;
    return target;
  }
}
\ No newline at end of file
- 
2.19.1.windows.1

