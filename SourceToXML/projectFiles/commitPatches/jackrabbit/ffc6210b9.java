From ffc6210b921faa28879d626b0e2c0444fc4b9888 Mon Sep 17 00:00:00 2001
From: Alexandru Parvulescu <alexparvulescu@apache.org>
Date: Thu, 1 Dec 2011 13:05:38 +0000
Subject: [PATCH] JCR-2906 Multivalued property sorted by last/random value

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1209063 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/SharedFieldCache.java   | 122 +++++++++++++++---
 .../jackrabbit/core/query/lucene/Util.java    |  19 +++
 .../jackrabbit/core/query/OrderByTest.java    |  32 +++++
 .../core/query/SQL2OrderByTest.java           |  24 ++++
 .../query/lucene/ComparableArrayTest.java     |  53 ++++++++
 5 files changed, 232 insertions(+), 18 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ComparableArrayTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
index 554a3fc07..d9d77a673 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
@@ -17,6 +17,7 @@
 package org.apache.jackrabbit.core.query.lucene;
 
 import java.io.IOException;
import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.WeakHashMap;
@@ -112,6 +113,78 @@ public class SharedFieldCache {
         }
     }
 
    static class ComparableArray implements Comparable<ComparableArray> {

        private int offset = 0;

        private Comparable<?>[] c = new Comparable[0];

        public ComparableArray(Comparable<?> item, int index) {
            insert(item, index);
        }

        public int compareTo(ComparableArray o) {
            return Util.compare(c, o.c);
        }

        /**
         * testing purpose only.
         * 
         * @return the offset
         */
        int getOffset() {
            return offset;
        }

        public ComparableArray insert(Comparable<?> item, int index) {
            // optimize for most common scenario
            if (c.length == 0) {
                offset = index;
                c = new Comparable<?>[] { item };
                return this;
            }

            // inside
            if (index >= offset && index < offset + c.length) {
                c[index - offset] = item;
                return this;
            }

            // before
            if (index < offset) {
                int relativeOffset = offset - index;
                Comparable<?>[] newC = new Comparable[relativeOffset + c.length];
                newC[0] = item;
                System.arraycopy(c, 0, newC, relativeOffset, c.length);
                c = newC;
                offset = index;
                return this;
            }

            // after
            if (index >= offset + c.length) {
                c = Arrays.copyOf(c, index - offset + 1);
                c[index - offset] = item;
                return this;
            }
            return this;
        }

        /*
         * This is needed by {@link UpperCaseSortComparator} and {@link LowerCaseSortComparator}
         */
        @Override
        public String toString() {
            if (c == null) {
                return null;
            }
            if (c.length == 1) {
                return c[0].toString();
            }
            return Arrays.toString(c);
        }
    }

     /**
      * Reference to the single instance of <code>SharedFieldCache</code>.
      */
@@ -152,9 +225,10 @@ public class SharedFieldCache {
         field = field.intern();
         ValueIndex ret = lookup(reader, field, prefix);
         if (ret == null) {
            Comparable<?>[] retArray = new Comparable[reader.maxDoc()];
            final int maxDocs = reader.maxDoc();
            ComparableArray[] retArray = new ComparableArray[maxDocs];
             int setValues = 0;
            if (retArray.length > 0) {
            if (maxDocs > 0) {
                 IndexFormatVersion version = IndexFormatVersion.getVersion(reader);
                 boolean hasPayloads = version.isAtLeast(IndexFormatVersion.V3);
                 TermDocs termDocs;
@@ -167,8 +241,6 @@ public class SharedFieldCache {
                     termDocs = reader.termDocs();
                 }
                 TermEnum termEnum = reader.terms(new Term(field, prefix));

                char[] tmp = new char[16];
                 try {
                     if (termEnum.term() == null) {
                         throw new RuntimeException("no terms in field " + field);
@@ -178,30 +250,28 @@ public class SharedFieldCache {
                         if (term.field() != field || !term.text().startsWith(prefix)) {
                             break;
                         }

                        // make sure term is compacted
                        String text = term.text();
                        int len = text.length() - prefix.length();
                        if (tmp.length < len) {
                            // grow tmp
                            tmp = new char[len];
                        }
                        text.getChars(prefix.length(), text.length(), tmp, 0);
                        String value = new String(tmp, 0, len);

                        termDocs.seek(termEnum);
                        final String value = termValueAsString(term, prefix);
                        termDocs.seek(term);
                         while (termDocs.next()) {
                            int termPosition = 0;
                             type = PropertyType.UNDEFINED;
                             if (hasPayloads) {
                                 TermPositions termPos = (TermPositions) termDocs;
                                termPos.nextPosition();
                                termPosition = termPos.nextPosition();
                                 if (termPos.isPayloadAvailable()) {
                                     payload = termPos.getPayload(payload, 0);
                                     type = PropertyMetaData.fromByteArray(payload).getPropertyType();
                                 }
                             }
                             setValues++;
                            retArray[termDocs.doc()] = getValue(value, type);
                            Comparable<?> v = getValue(value, type);
                            int doc = termDocs.doc();
                            ComparableArray ca = retArray[doc];
                            if (ca == null) {
                                retArray[doc] = new ComparableArray(v, termPosition);
                            } else {
                                retArray[doc] = ca.insert(v, termPosition);
                            }
                         }
                     } while (termEnum.next());
                 } finally {
@@ -216,6 +286,22 @@ public class SharedFieldCache {
         return ret;
     }
 
    /**
     * Extracts the value from a given Term as a String
     * 
     * @param term
     * @param prefix
     * @return string value contained in the term
     */
    private static String termValueAsString(Term term, String prefix) {
        // make sure term is compacted
        String text = term.text();
        int length = text.length() - prefix.length();
        char[] tmp = new char[length];
        text.getChars(prefix.length(), text.length(), tmp, 0);
        return new String(tmp, 0, length);
    }

     /**
      * See if a <code>ValueIndex</code> object is in the cache.
      */
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/Util.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/Util.java
index ba1d16626..1b83b2005 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/Util.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/Util.java
@@ -235,6 +235,25 @@ public class Util {
         }
     }
 
    /**
     * Compares two arrays of comparables.
     */
    public static int compare(Comparable<?>[] c1, Comparable<?>[] c2) {
        if (c1 == null) {
            return -1;
        }
        if (c2 == null) {
            return 1;
        }
        for (int i = 0; i < c1.length && i < c2.length; i++) {
            int d = compare(c1[i], c2[i]);
            if (d != 0) {
                return d;
            }
        }
        return c1.length - c2.length;
    }

     /**
      * Compares the two values. If the values have differing types, then an
      * attempt is made to convert the second value into the type of the first
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/OrderByTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/OrderByTest.java
index c1197632c..c7c09d6bf 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/OrderByTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/OrderByTest.java
@@ -60,6 +60,38 @@ public class OrderByTest extends AbstractIndexingTest {
         checkResult(result, 3);
     }
 
    /**
     * Test for JCR-2906
     */
    public void testOrderByMVP() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");
        Node n4 = testRootNode.addNode("node4");
        Node n5 = testRootNode.addNode("node5");

        n1.setProperty("extra", new String[] { "12345" });
        n1.setProperty("text", new String[] { "ccc" });

        n2.setProperty("text", new String[] { "eee", "bbb" });
        n3.setProperty("text", new String[] { "aaa" });
        n4.setProperty("text", new String[] { "bbb", "aaa" });
        n5.setProperty("text", new String[] { "eee", "aaa" });

        testRootNode.getSession().save();

        String sql = "SELECT value FROM nt:unstructured WHERE "
                + "jcr:path LIKE '" + testRoot + "/%' ORDER BY text";
        checkResultSequence(executeQuery(sql).getRows(), new Node[] { n3, n4,
                n1, n5, n2 });

        String xpath = "/"
                + testRoot
                + "/*[@jcr:primaryType='nt:unstructured'] order by jcr:score(), @text";
        checkResultSequence(executeQuery(xpath).getRows(), new Node[] { n3, n4,
                n1, n5, n2 });
    }

     public void testOrderByUpperCase() throws RepositoryException {
         Node n1 = testRootNode.addNode("node1");
         Node n2 = testRootNode.addNode("node2");
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/SQL2OrderByTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/SQL2OrderByTest.java
index 6a0ca6b9b..8bda83f81 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/SQL2OrderByTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/SQL2OrderByTest.java
@@ -73,6 +73,30 @@ public class SQL2OrderByTest extends AbstractQueryTest {
 
     }
 
    /**
     * SQL2 Test for JCR-2906
     */
    public void testOrderByMVP() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");
        Node n4 = testRootNode.addNode("node4");
        Node n5 = testRootNode.addNode("node5");

        n1.setProperty("text", new String[] { "ccc" });
        n2.setProperty("text", new String[] { "eee", "bbb" });
        n3.setProperty("text", new String[] { "aaa" });
        n4.setProperty("text", new String[] { "bbb", "aaa" });
        n5.setProperty("text", new String[] { "eee", "aaa" });

        testRootNode.getSession().save();

        String sql = "SELECT value FROM [nt:unstructured] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY text";

        checkSeq(executeSQL2Query(sql), new Node[] { n3, n4, n1, n5, n2 });
    }

     public void testOrderByVal() throws RepositoryException {
 
         Node n1 = testRootNode.addNode("node1");
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ComparableArrayTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ComparableArrayTest.java
new file mode 100644
index 000000000..49507d6b7
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ComparableArrayTest.java
@@ -0,0 +1,53 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.query.lucene;

import static junit.framework.Assert.assertEquals;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.SharedFieldCache.ComparableArray;
import org.junit.Test;

public class ComparableArrayTest {

    /**
     * Test for JCR-2906 to make sure the SharedFieldCache arranges the entries
     * properly and keeps the internal array creation efficient.
     */
    @Test
    public void testInsert() throws RepositoryException {
        ComparableArray ca = new ComparableArray("a", 1);
        assertEquals("a", ca.toString());
        assertEquals(1, ca.getOffset());

        // insert before
        ca.insert("b", 0);
        assertEquals("[b, a]", ca.toString());
        assertEquals(0, ca.getOffset());

        // insert after
        ca.insert("c", 3);
        assertEquals("[b, a, null, c]", ca.toString());
        assertEquals(0, ca.getOffset());

        // insert inside
        ca.insert("d", 2);
        assertEquals("[b, a, d, c]", ca.toString());
        assertEquals(0, ca.getOffset());
    }
}
- 
2.19.1.windows.1

