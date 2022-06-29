From 586d95622fa90c6b91e59485ced8208003028edc Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 18 Jan 2011 16:05:03 +0000
Subject: [PATCH] JCR-2832: Crash when adding node to cluster with big journal
 on PSQL DB

Patch by Omid Milani

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1060431 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/journal/DatabaseJournal.java         | 26 +++++++++++++++++++
 .../core/util/db/ConnectionHelper.java        |  1 +
 2 files changed, 27 insertions(+)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java
index c7d9995f4..e39fbeaa0 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java
@@ -434,6 +434,32 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
         }
     }
 
    /**
     * Synchronize contents from journal. May be overridden by subclasses.
     * Override to do it in batchMode, since some databases (PSQL) when 
     * not in transactional mode, load all results in memory which causes
     * out of memory.
     *
     * @param startRevision start point (exclusive)
     * @throws JournalException if an error occurs
     */
    @Override
    protected void doSync(long startRevision) throws JournalException {
        try {
            conHelper.startBatch();
            super.doSync(startRevision);
        } catch (SQLException e) {
            // Should throw journal exception instead of just logging it?
            log.error("couldn't sync the cluster node", e);
        } finally {
            try {
                conHelper.endBatch(true);
            } catch (SQLException e) {
                log.warn("couldn't close connection", e);
            }
        }
    }

     /**
      * {@inheritDoc}
      * <p/>
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/db/ConnectionHelper.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/db/ConnectionHelper.java
index 21a41c849..266b3aa50 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/db/ConnectionHelper.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/db/ConnectionHelper.java
@@ -355,6 +355,7 @@ public class ConnectionHelper {
                 stmt = con.prepareStatement(sql);
             }
             stmt.setMaxRows(maxRows);
            stmt.setFetchSize(10000);
             execute(stmt, params);
             if (returnGeneratedKeys) {
                 rs = stmt.getGeneratedKeys();
- 
2.19.1.windows.1

