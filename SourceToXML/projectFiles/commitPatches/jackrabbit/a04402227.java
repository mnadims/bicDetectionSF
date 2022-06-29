From a0440222748b687f0a747d49b331d88cb05ab213 Mon Sep 17 00:00:00 2001
From: Amit Jain <amitj@apache.org>
Date: Thu, 22 Jun 2017 03:32:41 +0000
Subject: [PATCH] JCR-4149: change to drop SHA-1 requires version change

 Add back the protected property in DbDataStore
 Update version of org.apache.jackrabbit.core.data.db - 2.13.6
 Update version of org.apache.jackrabbit.core.data - 2.14.0

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1799538 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/jackrabbit/core/data/db/DbDataStore.java | 5 +++++
 .../org/apache/jackrabbit/core/data/db/package-info.java     | 2 +-
 .../java/org/apache/jackrabbit/core/data/package-info.java   | 2 +-
 3 files changed, 7 insertions(+), 2 deletions(-)

diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/DbDataStore.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/DbDataStore.java
index 714d5004c..22d4bb56d 100644
-- a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/DbDataStore.java
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/DbDataStore.java
@@ -125,6 +125,11 @@ public class DbDataStore extends AbstractDataStore
      */
     public static final String STORE_SIZE_MAX = "max";
 
    /**
     * The digest algorithm used to uniquely identify records.
     */
    protected static final String DIGEST = System.getProperty("ds.digest.algorithm", "SHA-256");

     /**
      * The prefix used for temporary objects.
      */
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/package-info.java
index f3466ba42..a4f3bdbbf 100755
-- a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/package-info.java
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/package-info.java
@@ -15,5 +15,5 @@
  * limitations under the License.
  */
 /* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
@aQute.bnd.annotation.Version("2.13.6")
 package org.apache.jackrabbit.core.data.db;
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/package-info.java
index 252815d34..ee51d0e13 100755
-- a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/package-info.java
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/package-info.java
@@ -15,5 +15,5 @@
  * limitations under the License.
  */
 /* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
@aQute.bnd.annotation.Version("2.14.0")
 package org.apache.jackrabbit.core.data;
- 
2.19.1.windows.1

