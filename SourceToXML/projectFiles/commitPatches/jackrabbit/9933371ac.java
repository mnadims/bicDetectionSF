From 9933371ac82dab9e36964942e70f2889c08788a2 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Fri, 20 Jul 2007 23:09:01 +0000
Subject: [PATCH] JCR-788: Upgrade to Lucene 2.2     - Applied patch from
 Fabrizio Giustina

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@558194 13f79535-47bb-0310-9956-ffa450edef68
--
 .../query/lucene/ReadOnlyIndexReader.java     | 22 +++++++++++++++++++
 pom.xml                                       |  2 +-
 2 files changed, 23 insertions(+), 1 deletion(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ReadOnlyIndexReader.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ReadOnlyIndexReader.java
index f49e4f034..ae3952934 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ReadOnlyIndexReader.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ReadOnlyIndexReader.java
@@ -256,5 +256,27 @@ class ReadOnlyIndexReader extends FilterIndexReader {
         public int nextPosition() throws IOException {
             return ((TermPositions) this.in).nextPosition();
         }

        /**
         * @inheritDoc
         */
        public int getPayloadLength() {
            return ((TermPositions) in).getPayloadLength();
        }

        /**
         * @inheritDoc
         */
        public byte[] getPayload(byte data[], int offset) throws IOException {
            return ((TermPositions) in).getPayload(data, offset);
        }

        /**
         * @inheritDoc
         */
        public boolean isPayloadAvailable() {
            return ((TermPositions) in).isPayloadAvailable();
        }

     }
 }
diff --git a/pom.xml b/pom.xml
index e1ceb3c28..18268eac8 100644
-- a/pom.xml
++ b/pom.xml
@@ -730,7 +730,7 @@
       <dependency>
         <groupId>org.apache.lucene</groupId>
         <artifactId>lucene-core</artifactId>
        <version>2.0.0</version>
        <version>2.2.0</version>
       </dependency>
       <dependency>
         <groupId>org.apache.derby</groupId>
- 
2.19.1.windows.1

