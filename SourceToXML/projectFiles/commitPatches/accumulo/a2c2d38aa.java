From a2c2d38aa248056c1cf592e8a2a0ada17eb518e2 Mon Sep 17 00:00:00 2001
From: Keith Turner <kturner@apache.org>
Date: Tue, 19 Jan 2016 15:55:34 -0500
Subject: [PATCH] ACCUMULO-4098 Fixed bug with ByteBuffers thats do not start
 at 0

--
 .../core/util/UnsynchronizedBuffer.java       |  4 +-
 .../core/util/UnsynchronizedBufferTest.java   | 56 +++++++++++++++++++
 2 files changed, 58 insertions(+), 2 deletions(-)
 create mode 100644 core/src/test/java/org/apache/accumulo/core/util/UnsynchronizedBufferTest.java

diff --git a/core/src/main/java/org/apache/accumulo/core/util/UnsynchronizedBuffer.java b/core/src/main/java/org/apache/accumulo/core/util/UnsynchronizedBuffer.java
index 6947d64f6..f35361384 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/UnsynchronizedBuffer.java
++ b/core/src/main/java/org/apache/accumulo/core/util/UnsynchronizedBuffer.java
@@ -118,8 +118,8 @@ public class UnsynchronizedBuffer {
     }
 
     public Reader(ByteBuffer buffer) {
      if (buffer.hasArray()) {
        offset = buffer.arrayOffset();
      if (buffer.hasArray() && buffer.array().length == buffer.arrayOffset() + buffer.limit()) {
        offset = buffer.arrayOffset() + buffer.position();
         data = buffer.array();
       } else {
         data = new byte[buffer.remaining()];
diff --git a/core/src/test/java/org/apache/accumulo/core/util/UnsynchronizedBufferTest.java b/core/src/test/java/org/apache/accumulo/core/util/UnsynchronizedBufferTest.java
new file mode 100644
index 000000000..64162194a
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/util/UnsynchronizedBufferTest.java
@@ -0,0 +1,56 @@
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
package org.apache.accumulo.core.util;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Charsets;

public class UnsynchronizedBufferTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testByteBufferConstructor() {
    byte[] test = "0123456789".getBytes(Charsets.UTF_8);

    ByteBuffer bb1 = ByteBuffer.wrap(test);
    UnsynchronizedBuffer.Reader ub = new UnsynchronizedBuffer.Reader(bb1);
    byte[] buf = new byte[10];
    ub.readBytes(buf);
    Assert.assertEquals("0123456789", new String(buf, Charsets.UTF_8));

    ByteBuffer bb2 = ByteBuffer.wrap(test, 3, 5);

    ub = new UnsynchronizedBuffer.Reader(bb2);
    buf = new byte[5];
    // should read data from offset 3 where the byte buffer starts
    ub.readBytes(buf);
    Assert.assertEquals("34567", new String(buf, Charsets.UTF_8));

    buf = new byte[6];
    // the byte buffer has the extra byte, but should not be able to read it...
    thrown.expect(ArrayIndexOutOfBoundsException.class);
    ub.readBytes(buf);
  }
}
- 
2.19.1.windows.1

