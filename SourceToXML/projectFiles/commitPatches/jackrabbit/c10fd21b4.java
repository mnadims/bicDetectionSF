From c10fd21b4f38eca0d655080836f4e5cd8132e3f0 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Wed, 22 Jul 2009 14:55:29 +0000
Subject: [PATCH] JCR-2222: Unclosed files when aggregated property states are
 indexed

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@796757 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/SearchIndex.java        | 38 ++++++++++---------
 1 file changed, 21 insertions(+), 17 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
index 63b54ff57..bad611246 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
@@ -1315,24 +1315,28 @@ public class SearchIndex extends AbstractQueryHandler {
                             String namePrefix = FieldNames.createNamedValue(getNamespaceMappings().translateName(propState.getName()), "");
                             NodeState parent = (NodeState) ism.getItemState(propState.getParentId());
                             Document aDoc = createDocument(parent, getNamespaceMappings(), getIndex().getIndexFormatVersion());
                            // find the right fields to transfer
                            Fieldable[] fields = aDoc.getFieldables(FieldNames.PROPERTIES);
                            Token t = new Token();
                            for (Fieldable field : fields) {
                                // assume properties fields use SingleTokenStream
                                t = field.tokenStreamValue().next(t);
                                String value = new String(t.termBuffer(), 0, t.termLength());
                                if (value.startsWith(namePrefix)) {
                                    // extract value
                                    value = value.substring(namePrefix.length());
                                    // create new named value
                                    Path p = getRelativePath(state, propState);
                                    String path = getNamespaceMappings().translatePath(p);
                                    value = FieldNames.createNamedValue(path, value);
                                    t.setTermBuffer(value);
                                    doc.add(new Field(field.name(), new SingletonTokenStream(t)));
                                    doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID, parent.getNodeId().toString(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                            try {
                                // find the right fields to transfer
                                Fieldable[] fields = aDoc.getFieldables(FieldNames.PROPERTIES);
                                Token t = new Token();
                                for (Fieldable field : fields) {
                                    // assume properties fields use SingleTokenStream
                                    t = field.tokenStreamValue().next(t);
                                    String value = new String(t.termBuffer(), 0, t.termLength());
                                    if (value.startsWith(namePrefix)) {
                                        // extract value
                                        value = value.substring(namePrefix.length());
                                        // create new named value
                                        Path p = getRelativePath(state, propState);
                                        String path = getNamespaceMappings().translatePath(p);
                                        value = FieldNames.createNamedValue(path, value);
                                        t.setTermBuffer(value);
                                        doc.add(new Field(field.name(), new SingletonTokenStream(t)));
                                        doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID, parent.getNodeId().toString(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                                    }
                                 }
                            } finally {
                                Util.disposeDocument(aDoc);
                             }
                         }
                     }
- 
2.19.1.windows.1

