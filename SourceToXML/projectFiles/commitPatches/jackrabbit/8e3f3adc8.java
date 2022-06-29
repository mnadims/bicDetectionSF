From 8e3f3adc81a7320d92eef70eb9135d68217eeed1 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Thu, 26 Nov 2009 11:16:46 +0000
Subject: [PATCH] JCR-2410: ChainedTermEnum omits initial terms

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@884522 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/IndexMigration.java     |  9 ++-
 .../query/lucene/ChainedTermEnumTest.java     | 81 +++++++++++++++++++
 .../jackrabbit/core/query/lucene/TestAll.java |  1 +
 3 files changed, 90 insertions(+), 1 deletion(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ChainedTermEnumTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java
index df577f46e..2db7c3060 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java
@@ -269,7 +269,7 @@ public class IndexMigration {
         }
     }
 
    private static final class ChainedTermEnum extends TermEnum {
    static final class ChainedTermEnum extends TermEnum {
 
         private Queue<TermEnum> queue = new LinkedList<TermEnum>();
 
@@ -279,17 +279,24 @@ public class IndexMigration {
         }
 
         public boolean next() throws IOException {
            boolean newEnum = false;
             for (;;) {
                 TermEnum terms = queue.peek();
                 if (terms == null) {
                     // no more enums
                     break;
                 }
                if (newEnum && terms.term() != null) {
                    // need to check if enum is already positioned
                    // at first term
                    return true;
                }
                 if (terms.next()) {
                     return true;
                 } else {
                     queue.remove();
                     terms.close();
                    newEnum = true;
                 }
             }
             return false;
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ChainedTermEnumTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ChainedTermEnumTest.java
new file mode 100644
index 000000000..995ac20a1
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ChainedTermEnumTest.java
@@ -0,0 +1,81 @@
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

/**
 * <code>ChainedTermEnumTest</code> implements a test for JCR-2410.
 */
public class ChainedTermEnumTest extends TestCase {

    public void testEnum() throws Exception {
        Collection<TermEnum> enums = new ArrayList<TermEnum>();
        enums.add(createTermEnum("a", 2));
        enums.add(createTermEnum("b", 1));
        enums.add(createTermEnum("c", 0));
        enums.add(createTermEnum("d", 2));
        TermEnum terms = new IndexMigration.ChainedTermEnum(enums);
        List<String> expected = new ArrayList<String>();
        expected.addAll(Arrays.asList("a0", "a1", "b0", "d0", "d1"));
        List<String> result = new ArrayList<String>();
        do {
            Term t = terms.term();
            if (t != null) {
                result.add(t.text());
            }
        } while (terms.next());
        assertEquals(expected, result);
    }

    protected TermEnum createTermEnum(String prefix, int numTerms)
            throws IOException {
        Directory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new StandardAnalyzer(),
                true, IndexWriter.MaxFieldLength.UNLIMITED);
        for (int i = 0; i < numTerms; i++) {
            Document doc = new Document();
            doc.add(new Field("field", prefix + i,
                    Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
            writer.addDocument(doc);
        }
        writer.close();
        IndexReader reader = IndexReader.open(dir);
        TermEnum terms = reader.terms();
        if (terms.term() == null) {
            // position at first term
            terms.next();
        }
        return terms;
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/TestAll.java
index fd61147b0..99142e182 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/TestAll.java
@@ -40,6 +40,7 @@ public class TestAll extends TestCase {
         suite.addTestSuite(DecimalConvertTest.class);
         suite.addTestSuite(IndexingAggregateTest.class);
         suite.addTestSuite(IndexMigrationTest.class);
        suite.addTestSuite(ChainedTermEnumTest.class);
 
         return suite;
     }
- 
2.19.1.windows.1

