From 0dec2d10174a262283f7c32987a45295a43bc0eb Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Thu, 8 Nov 2012 18:49:14 +0000
Subject: [PATCH] LUCENE-4547: very rough prototype buffering

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/branches/lucene4547@1407212 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/lucene/codecs/Codec.java  |  6 +++
 .../lucene/codecs/SimpleDocValuesFormat.java  | 37 +++++++++++++++
 .../lucene/index/DocFieldProcessor.java       | 47 +++++++++++++++++++
 .../index/DocFieldProcessorPerField.java      | 16 +++++++
 .../index/DocumentsWriterPerThread.java       |  4 +-
 .../lucene/index/FreqProxTermsWriter.java     |  8 ++--
 6 files changed, 112 insertions(+), 6 deletions(-)
 create mode 100644 lucene/core/src/java/org/apache/lucene/codecs/SimpleDocValuesFormat.java

diff --git a/lucene/core/src/java/org/apache/lucene/codecs/Codec.java b/lucene/core/src/java/org/apache/lucene/codecs/Codec.java
index 7a473a3ed38..46b8b2901e7 100644
-- a/lucene/core/src/java/org/apache/lucene/codecs/Codec.java
++ b/lucene/core/src/java/org/apache/lucene/codecs/Codec.java
@@ -67,6 +67,12 @@ public abstract class Codec implements NamedSPILoader.NamedSPI {
   
   /** Encodes/decodes docvalues */
   public abstract DocValuesFormat docValuesFormat();

  /** Encodes/decodes streaming docvalues */
  public SimpleDocValuesFormat simpleDocValuesFormat() {
    // nocommit make this abstract
    return null;
  }
   
   /** Encodes/decodes stored fields */
   public abstract StoredFieldsFormat storedFieldsFormat();
diff --git a/lucene/core/src/java/org/apache/lucene/codecs/SimpleDocValuesFormat.java b/lucene/core/src/java/org/apache/lucene/codecs/SimpleDocValuesFormat.java
new file mode 100644
index 00000000000..d05535f3085
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/codecs/SimpleDocValuesFormat.java
@@ -0,0 +1,37 @@
package org.apache.lucene.codecs;

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

import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;

public abstract class SimpleDocValuesFormat {

  /** Sole constructor. (For invocation by subclass 
   *  constructors, typically implicit.) */
  protected SimpleDocValuesFormat() {
  }

  public abstract SimpleDVConsumer fieldsConsumer(Directory dir, SegmentInfo si, FieldInfos fis, IOContext context) throws IOException;
  // nocommit do this:
  //public abstract SimpleDVProducer fieldsProducer(Directory dir, SegmentInfo si, IOContext context) throws IOException;
}
diff --git a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
index eab9e327ffe..0c707836c9a 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
++ b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
@@ -28,11 +28,14 @@ import org.apache.lucene.codecs.Codec;
 import org.apache.lucene.codecs.DocValuesConsumer;
 import org.apache.lucene.codecs.FieldInfosWriter;
 import org.apache.lucene.codecs.PerDocConsumer;
import org.apache.lucene.codecs.SimpleDVConsumer;
import org.apache.lucene.codecs.SimpleDocValuesFormat;
 import org.apache.lucene.document.FieldType;
 import org.apache.lucene.index.DocumentsWriterPerThread.DocState;
 import org.apache.lucene.index.TypePromoter.TypeCompatibility;
 import org.apache.lucene.store.IOContext;
 import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Counter;
 import org.apache.lucene.util.IOUtils;
 
 
@@ -62,9 +65,12 @@ final class DocFieldProcessor extends DocConsumer {
   int fieldGen;
   final DocumentsWriterPerThread.DocState docState;
 
  final Counter bytesUsed;

   public DocFieldProcessor(DocumentsWriterPerThread docWriter, DocFieldConsumer consumer) {
     this.docState = docWriter.docState;
     this.codec = docWriter.codec;
    this.bytesUsed = docWriter.bytesUsed;
     this.consumer = consumer;
     fieldsWriter = new StoredFieldsConsumer(docWriter);
   }
@@ -78,6 +84,37 @@ final class DocFieldProcessor extends DocConsumer {
       childFields.put(f.getFieldInfo().name, f);
     }
 
    SimpleDVConsumer dvConsumer = null;

    for(int i=0;i<fieldHash.length;i++) {
      DocFieldProcessorPerField field = fieldHash[i];
      while(field != null) {
        // nocommit maybe we should sort by .... somethign?
        // field name?  field number?  else this is hash order!!
        if (field.bytesDVWriter != null) {
          if (dvConsumer == null) {
            SimpleDocValuesFormat fmt =  state.segmentInfo.getCodec().simpleDocValuesFormat();
            // nocommit once we make
            // Codec.simpleDocValuesFormat abstract, change
            // this to assert dvConsumer != null!
            if (fmt == null) {
              continue;
            }

            dvConsumer = fmt.fieldsConsumer(state.directory, state.segmentInfo, state.fieldInfos, state.context);
          }
          field.bytesDVWriter.flush(field.fieldInfo, state,
                                    dvConsumer.addBinaryField(field.fieldInfo,
                                                              field.bytesDVWriter.fixedLength >= 0,
                                                              field.bytesDVWriter.maxLength));
        }
        field = field.next;
      }
    }

    assert fields.size() == totalFieldCount;


     fieldsWriter.flush(state);
     consumer.flush(childFields, state);
 
@@ -235,8 +272,18 @@ final class DocFieldProcessor extends DocConsumer {
         fieldsWriter.addField(field, fp.fieldInfo);
       }
       
      // nocommit the DV indexing should be just another
      // consumer in the chain, not stuck inside here?  this
      // source should just "dispatch"
       final DocValues.Type dvType = ft.docValueType();
       if (dvType != null) {
        switch(dvType) {
        case BYTES_VAR_STRAIGHT:
          fp.addBytesDVField(docState.docID, field.binaryValue());
          break;
        default:
          break;
        }
         DocValuesConsumerHolder docValuesConsumer = docValuesConsumer(dvType,
             docState, fp.fieldInfo);
         DocValuesConsumer consumer = docValuesConsumer.docValuesConsumer;
diff --git a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessorPerField.java b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessorPerField.java
index 32fad151f19..6f753510ca8 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessorPerField.java
++ b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessorPerField.java
@@ -18,6 +18,8 @@ package org.apache.lucene.index;
  */
 
 import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Counter;
 import org.apache.lucene.util.RamUsageEstimator;
 
 /**
@@ -28,6 +30,8 @@ final class DocFieldProcessorPerField {
 
   final DocFieldConsumerPerField consumer;
   final FieldInfo fieldInfo;
  private final Counter bytesUsed;
  BytesDVWriter bytesDVWriter;
 
   DocFieldProcessorPerField next;
   int lastGen = -1;
@@ -38,6 +42,15 @@ final class DocFieldProcessorPerField {
   public DocFieldProcessorPerField(final DocFieldProcessor docFieldProcessor, final FieldInfo fieldInfo) {
     this.consumer = docFieldProcessor.consumer.addField(fieldInfo);
     this.fieldInfo = fieldInfo;
    this.bytesUsed = docFieldProcessor.bytesUsed;
  }

  // nocommit make this generic chain through consumer?
  public void addBytesDVField(int docID, BytesRef value) {
    if (bytesDVWriter == null) {
      bytesDVWriter = new BytesDVWriter(fieldInfo, bytesUsed);
    }
    bytesDVWriter.addValue(docID, value);
   }
 
   public void addField(IndexableField field) {
@@ -53,5 +66,8 @@ final class DocFieldProcessorPerField {
 
   public void abort() {
     consumer.abort();
    if (bytesDVWriter != null) {
      bytesDVWriter.abort();
    }
   }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/index/DocumentsWriterPerThread.java b/lucene/core/src/java/org/apache/lucene/index/DocumentsWriterPerThread.java
index 272e6f09b09..a78e87842c5 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DocumentsWriterPerThread.java
++ b/lucene/core/src/java/org/apache/lucene/index/DocumentsWriterPerThread.java
@@ -82,8 +82,8 @@ class DocumentsWriterPerThread {
       final TermsHashConsumer termVectorsWriter = new TermVectorsConsumer(documentsWriterPerThread);
       final TermsHashConsumer freqProxWriter = new FreqProxTermsWriter();
 
      final InvertedDocConsumer  termsHash = new TermsHash(documentsWriterPerThread, freqProxWriter, true,
                                                           new TermsHash(documentsWriterPerThread, termVectorsWriter, false, null));
      final InvertedDocConsumer termsHash = new TermsHash(documentsWriterPerThread, freqProxWriter, true,
                                                          new TermsHash(documentsWriterPerThread, termVectorsWriter, false, null));
       final NormsConsumer normsWriter = new NormsConsumer(documentsWriterPerThread);
       final DocInverter docInverter = new DocInverter(documentsWriterPerThread.docState, termsHash, normsWriter);
       return new DocFieldProcessor(documentsWriterPerThread, docInverter);
diff --git a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
index 75eeaedbf1c..6937a09a521 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
@@ -45,10 +45,10 @@ final class FreqProxTermsWriter extends TermsHashConsumer {
     List<FreqProxTermsWriterPerField> allFields = new ArrayList<FreqProxTermsWriterPerField>();
 
     for (TermsHashConsumerPerField f : fieldsToFlush.values()) {
        final FreqProxTermsWriterPerField perField = (FreqProxTermsWriterPerField) f;
        if (perField.termsHashPerField.bytesHash.size() > 0) {
          allFields.add(perField);
        }
      final FreqProxTermsWriterPerField perField = (FreqProxTermsWriterPerField) f;
      if (perField.termsHashPerField.bytesHash.size() > 0) {
        allFields.add(perField);
      }
     }
 
     final int numAllFields = allFields.size();
- 
2.19.1.windows.1

