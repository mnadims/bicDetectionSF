From fa52907742dc65d3145a4db11ca00fac5a5a3fca Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Thu, 8 Nov 2012 18:52:07 +0000
Subject: [PATCH] LUCENE-4547: fix hang; remove sops

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/branches/lucene4547@1407214 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/lucene/index/BytesDVWriter.java    | 115 ++++++++++++++++++
 .../lucene/index/DocFieldProcessor.java       |   1 +
 2 files changed, 116 insertions(+)
 create mode 100644 lucene/core/src/java/org/apache/lucene/index/BytesDVWriter.java

diff --git a/lucene/core/src/java/org/apache/lucene/index/BytesDVWriter.java b/lucene/core/src/java/org/apache/lucene/index/BytesDVWriter.java
new file mode 100644
index 00000000000..962bb92c254
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/index/BytesDVWriter.java
@@ -0,0 +1,115 @@
package org.apache.lucene.index;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.codecs.BinaryDocValuesConsumer;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.RamUsageEstimator;


/** Buffers up pending byte[] per doc, then flushes when
 *  segment flushes. */
// nocommit name?
// nocommit make this a consumer in the chain?
class BytesDVWriter {

  // nocommit more ram efficient?
  private final ArrayList<byte[]> pending = new ArrayList<byte[]>();
  private final Counter iwBytesUsed;
  private int bytesUsed;
  private final FieldInfo fieldInfo;

  private static final BytesRef EMPTY = new BytesRef(BytesRef.EMPTY_BYTES);

  // -2 means not set yet; -1 means length isn't fixed;
  // -otherwise it's the fixed length seen so far:
  int fixedLength = -2;
  int maxLength;

  public BytesDVWriter(FieldInfo fieldInfo, Counter iwBytesUsed) {
    this.fieldInfo = fieldInfo;
    this.iwBytesUsed = iwBytesUsed;
  }

  public void addValue(int docID, BytesRef value) {
    final int oldBytesUsed = bytesUsed;
    if (value == null) {
      // nocommit improve message
      throw new IllegalArgumentException("null binaryValue not allowed (field=" + fieldInfo.name + ")");
    }
    mergeLength(value.length);
    // Fill in any holes:
    while(pending.size() < docID) {
      pending.add(BytesRef.EMPTY_BYTES);
      bytesUsed += RamUsageEstimator.NUM_BYTES_OBJECT_REF;
      mergeLength(0);
    }
    byte[] bytes = new byte[value.length];
    System.arraycopy(value.bytes, value.offset, bytes, 0, value.length);
    pending.add(bytes);

    // estimate 25% overhead for ArrayList:
    bytesUsed += (int) (bytes.length + RamUsageEstimator.NUM_BYTES_ARRAY_HEADER + (RamUsageEstimator.NUM_BYTES_OBJECT_REF * 1.25));
    iwBytesUsed.addAndGet(bytesUsed - oldBytesUsed);
    //System.out.println("ADD: " + value);
  }

  private void mergeLength(int length) {
    if (fixedLength == -2) {
      fixedLength = length;
    } else if (fixedLength != length) {
      fixedLength = -1;
    }
    maxLength = Math.max(maxLength, length);
  }

  public void flush(FieldInfo fieldInfo, SegmentWriteState state, BinaryDocValuesConsumer consumer) throws IOException {
    final int bufferedDocCount = pending.size();
    BytesRef value = new BytesRef();

    for(int docID=0;docID<bufferedDocCount;docID++) {
      value.bytes = pending.get(docID);
      value.length = value.bytes.length;
      consumer.add(value);
    }
    final int maxDoc = state.segmentInfo.getDocCount();
    for(int docID=bufferedDocCount;docID<maxDoc;docID++) {
      consumer.add(EMPTY);
    }
    reset();
    //System.out.println("FLUSH");
  }

  public void abort() {
    reset();
  }

  private void reset() {
    pending.clear();
    pending.trimToSize();
    iwBytesUsed.addAndGet(-bytesUsed);
    bytesUsed = 0;
    fixedLength = -2;
    maxLength = 0;
  }
}
\ No newline at end of file
diff --git a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
index 0c707836c9a..fd1cf25be7c 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
++ b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
@@ -98,6 +98,7 @@ final class DocFieldProcessor extends DocConsumer {
             // Codec.simpleDocValuesFormat abstract, change
             // this to assert dvConsumer != null!
             if (fmt == null) {
              field = field.next;
               continue;
             }
 
- 
2.19.1.windows.1

