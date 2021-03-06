From 3b2e04d6a926cb5afc0bcd20c56c01dae2f6cf98 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Thu, 8 Nov 2012 20:04:19 +0000
Subject: [PATCH] LUCENE-4547: add numeric buffering too

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/branches/lucene4547@1407249 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/lucene/index/BytesDVWriter.java    |   5 +-
 .../lucene/index/DocFieldProcessor.java       |  27 ++++-
 .../index/DocFieldProcessorPerField.java      |  15 +++
 .../apache/lucene/index/NumberDVWriter.java   | 110 ++++++++++++++++++
 4 files changed, 149 insertions(+), 8 deletions(-)
 create mode 100644 lucene/core/src/java/org/apache/lucene/index/NumberDVWriter.java

diff --git a/lucene/core/src/java/org/apache/lucene/index/BytesDVWriter.java b/lucene/core/src/java/org/apache/lucene/index/BytesDVWriter.java
index 962bb92c254..8f602a75ff3 100644
-- a/lucene/core/src/java/org/apache/lucene/index/BytesDVWriter.java
++ b/lucene/core/src/java/org/apache/lucene/index/BytesDVWriter.java
@@ -39,8 +39,6 @@ class BytesDVWriter {
   private int bytesUsed;
   private final FieldInfo fieldInfo;
 
  private static final BytesRef EMPTY = new BytesRef(BytesRef.EMPTY_BYTES);

   // -2 means not set yet; -1 means length isn't fixed;
   // -otherwise it's the fixed length seen so far:
   int fixedLength = -2;
@@ -93,8 +91,9 @@ class BytesDVWriter {
       consumer.add(value);
     }
     final int maxDoc = state.segmentInfo.getDocCount();
    value.length = 0;
     for(int docID=bufferedDocCount;docID<maxDoc;docID++) {
      consumer.add(EMPTY);
      consumer.add(value);
     }
     reset();
     //System.out.println("FLUSH");
diff --git a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
index fd1cf25be7c..03fe030df6a 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
++ b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
@@ -91,7 +91,8 @@ final class DocFieldProcessor extends DocConsumer {
       while(field != null) {
         // nocommit maybe we should sort by .... somethign?
         // field name?  field number?  else this is hash order!!
        if (field.bytesDVWriter != null) {
        if (field.bytesDVWriter != null || field.numberDVWriter != null) {

           if (dvConsumer == null) {
             SimpleDocValuesFormat fmt =  state.segmentInfo.getCodec().simpleDocValuesFormat();
             // nocommit once we make
@@ -104,10 +105,19 @@ final class DocFieldProcessor extends DocConsumer {
 
             dvConsumer = fmt.fieldsConsumer(state.directory, state.segmentInfo, state.fieldInfos, state.context);
           }
          field.bytesDVWriter.flush(field.fieldInfo, state,
                                    dvConsumer.addBinaryField(field.fieldInfo,
                                                              field.bytesDVWriter.fixedLength >= 0,
                                                              field.bytesDVWriter.maxLength));

          if (field.bytesDVWriter != null) {
            field.bytesDVWriter.flush(field.fieldInfo, state,
                                      dvConsumer.addBinaryField(field.fieldInfo,
                                                                field.bytesDVWriter.fixedLength >= 0,
                                                                field.bytesDVWriter.maxLength));
          }
          if (field.numberDVWriter != null) {
            field.numberDVWriter.flush(field.fieldInfo, state,
                                       dvConsumer.addNumericField(field.fieldInfo,
                                                                  field.numberDVWriter.minValue,
                                                                  field.numberDVWriter.maxValue));
          }
         }
         field = field.next;
       }
@@ -282,6 +292,13 @@ final class DocFieldProcessor extends DocConsumer {
         case BYTES_VAR_STRAIGHT:
           fp.addBytesDVField(docState.docID, field.binaryValue());
           break;
        case VAR_INTS:
        case FIXED_INTS_8:
        case FIXED_INTS_16:
        case FIXED_INTS_32:
        case FIXED_INTS_64:
          fp.addNumberDVField(docState.docID, field.numericValue());
          break;
         default:
           break;
         }
diff --git a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessorPerField.java b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessorPerField.java
index 6f753510ca8..a77c7bb9a93 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessorPerField.java
++ b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessorPerField.java
@@ -31,7 +31,11 @@ final class DocFieldProcessorPerField {
   final DocFieldConsumerPerField consumer;
   final FieldInfo fieldInfo;
   private final Counter bytesUsed;

  // nocommit after flush we should null these out?  then we
  // don't need reset() impl'd in each...
   BytesDVWriter bytesDVWriter;
  NumberDVWriter numberDVWriter;
 
   DocFieldProcessorPerField next;
   int lastGen = -1;
@@ -53,6 +57,14 @@ final class DocFieldProcessorPerField {
     bytesDVWriter.addValue(docID, value);
   }
 
  // nocommit make this generic chain through consumer?
  public void addNumberDVField(int docID, Number value) {
    if (numberDVWriter == null) {
      numberDVWriter = new NumberDVWriter(fieldInfo, bytesUsed);
    }
    numberDVWriter.addValue(docID, value.longValue());
  }

   public void addField(IndexableField field) {
     if (fieldCount == fields.length) {
       int newSize = ArrayUtil.oversize(fieldCount + 1, RamUsageEstimator.NUM_BYTES_OBJECT_REF);
@@ -69,5 +81,8 @@ final class DocFieldProcessorPerField {
     if (bytesDVWriter != null) {
       bytesDVWriter.abort();
     }
    if (numberDVWriter != null) {
      numberDVWriter.abort();
    }
   }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/index/NumberDVWriter.java b/lucene/core/src/java/org/apache/lucene/index/NumberDVWriter.java
new file mode 100644
index 00000000000..60e99d7261e
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/index/NumberDVWriter.java
@@ -0,0 +1,110 @@
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

import org.apache.lucene.codecs.NumericDocValuesConsumer;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.RamUsageEstimator;

// nocommit pick numeric or number ... then fix all places ...

/** Buffers up pending long per doc, then flushes when
 *  segment flushes. */
// nocommit name?
// nocommit make this a consumer in the chain?
class NumberDVWriter {

  private final static Long MISSING = new Long(0);

  // nocommit more ram efficient?
  private final ArrayList<Long> pending = new ArrayList<Long>();
  private final Counter iwBytesUsed;
  private int bytesUsed;
  private final FieldInfo fieldInfo;

  long minValue;
  long maxValue;
  private boolean anyValues;

  public NumberDVWriter(FieldInfo fieldInfo, Counter iwBytesUsed) {
    this.fieldInfo = fieldInfo;
    this.iwBytesUsed = iwBytesUsed;
  }

  public void addValue(int docID, long value) {
    final int oldBytesUsed = bytesUsed;
    mergeValue(value);

    // Fill in any holes:
    while(pending.size() < docID) {
      pending.add(MISSING);
      bytesUsed += RamUsageEstimator.NUM_BYTES_OBJECT_REF;
      mergeValue(0);
    }

    pending.add(value);

    // estimate 25% overhead for ArrayList:
    bytesUsed += (int) (RamUsageEstimator.NUM_BYTES_OBJECT_HEADER + RamUsageEstimator.NUM_BYTES_LONG + (RamUsageEstimator.NUM_BYTES_OBJECT_REF * 1.25));
    iwBytesUsed.addAndGet(bytesUsed - oldBytesUsed);
    //System.out.println("ADD: " + value);
  }

  private void mergeValue(long value) {
    if (!anyValues) {
      anyValues = true;
      minValue = maxValue = value;
    } else {
      maxValue = Math.max(value, maxValue);
      minValue = Math.min(value, minValue);
    }
  }

  public void flush(FieldInfo fieldInfo, SegmentWriteState state, NumericDocValuesConsumer consumer) throws IOException {
    final int bufferedDocCount = pending.size();

    for(int docID=0;docID<bufferedDocCount;docID++) {
      consumer.add(pending.get(docID));
    }
    final int maxDoc = state.segmentInfo.getDocCount();
    for(int docID=bufferedDocCount;docID<maxDoc;docID++) {
      consumer.add(0);
    }
    reset();
    //System.out.println("FLUSH");
  }

  public void abort() {
    reset();
  }

  // nocommit do we really need this...?  can't parent alloc
  // a new instance after flush?
  private void reset() {
    pending.clear();
    pending.trimToSize();
    iwBytesUsed.addAndGet(-bytesUsed);
    anyValues = false;
    minValue = maxValue = 0;
    bytesUsed = 0;
  }
}
\ No newline at end of file
- 
2.19.1.windows.1

