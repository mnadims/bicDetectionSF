From 29dee650446c6f8f05e70e0ed00b8ad389d85670 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 18 Jan 2011 16:09:08 +0000
Subject: [PATCH] JCR-2832: Crash when adding node to cluster with big journal
 on PSQL DB

Minor cleanup (better try-finally construct, throw exception instead of logging)

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1060434 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/journal/DatabaseJournal.java    | 13 +++++--------
 1 file changed, 5 insertions(+), 8 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java
index e39fbeaa0..7c43d2ab9 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java
@@ -436,7 +436,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
 
     /**
      * Synchronize contents from journal. May be overridden by subclasses.
     * Override to do it in batchMode, since some databases (PSQL) when 
     * Override to do it in batchMode, since some databases (PSQL) when
      * not in transactional mode, load all results in memory which causes
      * out of memory.
      *
@@ -447,16 +447,13 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
     protected void doSync(long startRevision) throws JournalException {
         try {
             conHelper.startBatch();
            super.doSync(startRevision);
        } catch (SQLException e) {
            // Should throw journal exception instead of just logging it?
            log.error("couldn't sync the cluster node", e);
        } finally {
             try {
                super.doSync(startRevision);
            } finally {
                 conHelper.endBatch(true);
            } catch (SQLException e) {
                log.warn("couldn't close connection", e);
             }
        } catch (SQLException e) {
            throw new JournalException("Couldn't sync the cluster node", e);
         }
     }
 
- 
2.19.1.windows.1

