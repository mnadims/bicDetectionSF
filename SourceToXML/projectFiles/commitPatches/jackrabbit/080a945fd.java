From 080a945fd2a3780e9ffb7250606ba9c569c1a6db Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Tue, 17 Nov 2009 14:15:48 +0000
Subject: [PATCH] JCR-2393: Index migration fails for property names that are
 prefixes of others

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@881299 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/IndexMigration.java     | 157 +++++++++++++++---
 .../core/query/lucene/PersistentIndex.java    |  12 +-
 .../core/query/lucene/IndexMigrationTest.java |  74 +++++++++
 .../jackrabbit/core/query/lucene/TestAll.java |   1 +
 4 files changed, 215 insertions(+), 29 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexMigrationTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java
index 7f7f40713..df577f46e 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java
@@ -16,23 +16,32 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
 import org.apache.lucene.index.FilterIndexReader;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.TermPositions;
 import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
 import org.apache.lucene.store.Directory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.Field;
import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
import org.slf4j.LoggerFactory;
 import org.slf4j.Logger;

import java.io.IOException;
import org.slf4j.LoggerFactory;
 
 /**
  * <code>IndexMigration</code> implements a utility that migrates a Jackrabbit
@@ -55,10 +64,12 @@ public class IndexMigration {
      *
      * @param index the index to check and migration if needed.
      * @param directoryManager the directory manager.
     * @param oldSeparatorChar the old separator char that needs to be replaced.
      * @throws IOException if an error occurs while migrating the index.
      */
     public static void migrate(PersistentIndex index,
                               DirectoryManager directoryManager)
                               DirectoryManager directoryManager,
                               char oldSeparatorChar)
             throws IOException {
         Directory indexDir = index.getDirectory();
         log.debug("Checking {} ...", indexDir);
@@ -76,7 +87,7 @@ public class IndexMigration {
             TermEnum terms = reader.terms(new Term(FieldNames.PROPERTIES, ""));
             try {
                 Term t = terms.term();
                if (t.text().indexOf('\uFFFF') == -1) {
                if (t.text().indexOf(oldSeparatorChar) == -1) {
                     log.debug("Index already migrated");
                     return;
                 }
@@ -102,7 +113,8 @@ public class IndexMigration {
                     IndexWriter.MaxFieldLength.UNLIMITED);
             try {
                 IndexReader r = new MigrationIndexReader(
                        IndexReader.open(index.getDirectory()));
                        IndexReader.open(index.getDirectory()),
                        oldSeparatorChar);
                 try {
                     writer.addIndexes(new IndexReader[]{r});
                     writer.close();
@@ -131,8 +143,11 @@ public class IndexMigration {
      */
     private static class MigrationIndexReader extends FilterIndexReader {
 
        public MigrationIndexReader(IndexReader in) {
        private final char oldSepChar;

        public MigrationIndexReader(IndexReader in, char oldSepChar) {
             super(in);
            this.oldSepChar = oldSepChar;
         }
 
         public Document document(int n, FieldSelector fieldSelector)
@@ -143,7 +158,7 @@ public class IndexMigration {
                 doc.removeFields(FieldNames.PROPERTIES);
                 for (Fieldable field : fields) {
                     String value = field.stringValue();
                    value = value.replace('\uFFFF', '[');
                    value = value.replace(oldSepChar, '[');
                     doc.add(new Field(FieldNames.PROPERTIES, value, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                 }
             }
@@ -151,17 +166,60 @@ public class IndexMigration {
         }
 
         public TermEnum terms() throws IOException {
            return new MigrationTermEnum(in.terms());
            List<TermEnum> enums = new ArrayList<TermEnum>();
            List<String> fieldNames = new ArrayList<String>();
            for (Object obj : in.getFieldNames(FieldOption.ALL)) {
                fieldNames.add((String) obj);
            }
            Collections.sort(fieldNames);
            for (String fieldName : fieldNames) {
                if (fieldName.equals(FieldNames.PROPERTIES)) {
                    addPropertyTerms(enums);
                } else {
                    enums.add(new RangeScan(in, new Term(fieldName, ""), new Term(fieldName, "\uFFFF")));
                }
            }
            return new MigrationTermEnum(new ChainedTermEnum(enums), oldSepChar);
         }
 
         public TermPositions termPositions() throws IOException {
            return new MigrationTermPositions(in.termPositions());
            return new MigrationTermPositions(in.termPositions(), oldSepChar);
        }

        private void addPropertyTerms(List<TermEnum> enums) throws IOException {
            SortedMap<String, TermEnum> termEnums = new TreeMap<String, TermEnum>(
                    new Comparator<String>() {
                        public int compare(String s1, String s2) {
                            s1 = s1.replace(oldSepChar, '[');
                            s2 = s2.replace(oldSepChar, '[');
                            return s1.compareTo(s2);
                        }
            });
            // scan through terms and find embedded field names
            TermEnum terms = new RangeScan(in,
                    new Term(FieldNames.PROPERTIES, ""),
                    new Term(FieldNames.PROPERTIES, "\uFFFF"));
            String previous = null;
            while (terms.next()) {
                Term t = terms.term();
                String name = t.text().substring(0, t.text().indexOf(oldSepChar) + 1);
                if (!name.equals(previous)) {
                    termEnums.put(name, new RangeScan(in,
                            new Term(FieldNames.PROPERTIES, name),
                            new Term(FieldNames.PROPERTIES, name + "\uFFFF")));
                }
                previous = name;
            }
            enums.addAll(termEnums.values());
         }
 
         private static class MigrationTermEnum extends FilterTermEnum {
 
            public MigrationTermEnum(TermEnum in) {
            private final char oldSepChar;

            public MigrationTermEnum(TermEnum in, char oldSepChar) {
                 super(in);
                this.oldSepChar = oldSepChar;
             }
 
             public Term term() {
@@ -171,7 +229,7 @@ public class IndexMigration {
                 }
                 if (t.field().equals(FieldNames.PROPERTIES)) {
                     String text = t.text();
                    return t.createTerm(text.replace('\uFFFF', '['));
                    return t.createTerm(text.replace(oldSepChar, '['));
                 } else {
                     return t;
                 }
@@ -184,14 +242,17 @@ public class IndexMigration {
 
         private static class MigrationTermPositions extends FilterTermPositions {
 
            public MigrationTermPositions(TermPositions in) {
            private final char oldSepChar;

            public MigrationTermPositions(TermPositions in, char oldSepChar) {
                 super(in);
                this.oldSepChar = oldSepChar;
             }
 
             public void seek(Term term) throws IOException {
                 if (term.field().equals(FieldNames.PROPERTIES)) {
                     char[] text = term.text().toCharArray();
                    text[term.text().indexOf('[')] = '\uFFFF';
                    text[term.text().indexOf('[')] = oldSepChar;
                     super.seek(term.createTerm(new String(text)));
                 } else {
                     super.seek(term);
@@ -207,4 +268,54 @@ public class IndexMigration {
             }
         }
     }

    private static final class ChainedTermEnum extends TermEnum {

        private Queue<TermEnum> queue = new LinkedList<TermEnum>();

        public ChainedTermEnum(Collection<TermEnum> enums) {
            super();
            queue.addAll(enums);
        }

        public boolean next() throws IOException {
            for (;;) {
                TermEnum terms = queue.peek();
                if (terms == null) {
                    // no more enums
                    break;
                }
                if (terms.next()) {
                    return true;
                } else {
                    queue.remove();
                    terms.close();
                }
            }
            return false;
        }

        public Term term() {
            TermEnum terms = queue.peek();
            if (terms != null) {
                return terms.term();
            }
            return null;
        }

        public int docFreq() {
            TermEnum terms = queue.peek();
            if (terms != null) {
                return terms.docFreq();
            }
            return 0;
        }

        public void close() throws IOException {
            // close remaining
            while (!queue.isEmpty()) {
                queue.remove().close();
            }
        }
    }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java
index bfb17f82b..28adc2a31 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java
@@ -16,17 +16,17 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;

import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexDeletionPolicy;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.search.Similarity;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.IndexInput;
 import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.search.Similarity;
import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;

import java.io.IOException;
 
 /**
  * Implements a lucene index which is based on a
@@ -79,7 +79,7 @@ class PersistentIndex extends AbstractIndex {
         this.indexDelPolicy = new IndexDeletionPolicyImpl(this,
                 generationMaxAge * 1000);
         if (isExisting()) {
            IndexMigration.migrate(this, directoryManager);
            IndexMigration.migrate(this, directoryManager, '\uFFFF');
         }
     }
 
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexMigrationTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexMigrationTest.java
new file mode 100644
index 000000000..2e37c6cea
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexMigrationTest.java
@@ -0,0 +1,74 @@
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
import org.apache.jackrabbit.core.query.lucene.directory.RAMDirectoryManager;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

/**
 * <code>IndexMigrationTest</code> contains a test case for JCR-2393.
 */
public class IndexMigrationTest extends TestCase {

    /**
     * Cannot use \uFFFF because of LUCENE-1221.
     */
    private static final char SEP_CHAR = '\uFFFE';

    public void testMigration() throws Exception {
        List<Document> docs = new ArrayList<Document>();
        docs.add(createDocument("ab", "a"));
        docs.add(createDocument("a", "b"));
        docs.add(createDocument("abcd", "c"));
        docs.add(createDocument("abc", "d"));

        DirectoryManager dirMgr = new RAMDirectoryManager();

        PersistentIndex idx = new PersistentIndex("index",
                new StandardAnalyzer(), Similarity.getDefault(),
                new DocNumberCache(100),
                new IndexingQueue(new IndexingQueueStore(new RAMDirectory())),
                dirMgr, 0);
        idx.addDocuments(docs.toArray(new Document[docs.size()]));
        idx.commit();

        IndexMigration.migrate(idx, dirMgr, SEP_CHAR);
    }

    protected static String createNamedValue14(String name, String value) {
        return name + SEP_CHAR + value;
    }

    protected static Document createDocument(String name, String value) {
        Document doc = new Document();
        doc.add(new Field(FieldNames.UUID, UUID.randomUUID().toString(), Field.Store.YES, Field.Index.NO));
        doc.add(new Field(FieldNames.PROPERTIES, createNamedValue14(name, value), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FieldNames.FULLTEXT_PREFIX + ":" + name, value, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
        return doc;
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/TestAll.java
index 684559f6e..fd61147b0 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/TestAll.java
@@ -39,6 +39,7 @@ public class TestAll extends TestCase {
         suite.addTestSuite(IndexingQueueTest.class);
         suite.addTestSuite(DecimalConvertTest.class);
         suite.addTestSuite(IndexingAggregateTest.class);
        suite.addTestSuite(IndexMigrationTest.class);
 
         return suite;
     }
- 
2.19.1.windows.1

