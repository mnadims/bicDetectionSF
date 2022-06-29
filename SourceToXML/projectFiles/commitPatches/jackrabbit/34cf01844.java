From 34cf0184403de0a29720ea4127bdb83b96996a01 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Tue, 18 Sep 2012 07:31:59 +0000
Subject: [PATCH] JCR-3427: JCR-3138 may cause resource starvation

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1387021 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/journal/AbstractJournal.java         | 89 ++++++++++---------
 1 file changed, 47 insertions(+), 42 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/AbstractJournal.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/AbstractJournal.java
index 1f2e6ae23..a564b4601 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/AbstractJournal.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/AbstractJournal.java
@@ -181,16 +181,30 @@ public abstract class AbstractJournal implements Journal {
      * {@inheritDoc}
      */
     public void sync() throws JournalException {
        if (internalVersionManager != null) {
            VersioningLock.ReadLock lock =
                internalVersionManager.acquireReadLock();
            try {
        for (;;) {
            if (internalVersionManager != null) {
                VersioningLock.ReadLock lock =
                        internalVersionManager.acquireReadLock();
                try {
                    internalSync();
                } finally {
                    lock.release();
                }
            } else {
                 internalSync();
            } finally {
                lock.release();
             }
        } else {
            internalSync();
            if (syncAgainOnNewRecords()) {
                // sync again if there are more records available
                RecordIterator it = getRecords(getMinimalRevision());
                try {
                    if (it.hasNext()) {
                        continue;
                    }
                } finally {
                    it.close();
                }
            }
            break;
         }
     }
 
@@ -215,46 +229,37 @@ public abstract class AbstractJournal implements Journal {
      * @throws JournalException if an error occurs
      */
     protected void doSync(long startRevision) throws JournalException {
        for (;;) {
            RecordIterator iterator = getRecords(startRevision);
            long stopRevision = Long.MIN_VALUE;
    
            try {
                while (iterator.hasNext()) {
                    Record record = iterator.nextRecord();
                    if (record.getJournalId().equals(id)) {
                        log.info("Record with revision '" + record.getRevision()
                                + "' created by this journal, skipped.");
                    } else {
                        RecordConsumer consumer = getConsumer(record.getProducerId());
                        if (consumer != null) {
                            try {
                                consumer.consume(record);
                            } catch (IllegalStateException e) {
                                log.error("Could not synchronize to revision: " + record.getRevision() + " due illegal state of RecordConsumer.");
                                return;
                            }
        RecordIterator iterator = getRecords(startRevision);
        long stopRevision = Long.MIN_VALUE;

        try {
            while (iterator.hasNext()) {
                Record record = iterator.nextRecord();
                if (record.getJournalId().equals(id)) {
                    log.info("Record with revision '" + record.getRevision()
                            + "' created by this journal, skipped.");
                } else {
                    RecordConsumer consumer = getConsumer(record.getProducerId());
                    if (consumer != null) {
                        try {
                            consumer.consume(record);
                        } catch (IllegalStateException e) {
                            log.error("Could not synchronize to revision: " + record.getRevision() + " due illegal state of RecordConsumer.");
                            return;
                         }
                     }
                    stopRevision = record.getRevision();
                 }
            } finally {
                iterator.close();
                stopRevision = record.getRevision();
             }
    
            if (stopRevision > 0) {
                for (RecordConsumer consumer : consumers.values()) {
                    consumer.setRevision(stopRevision);
                }
                log.info("Synchronized to revision: " + stopRevision);
        } finally {
            iterator.close();
        }
 
                if (syncAgainOnNewRecords()) {
                    // changes detected, sync again
                    startRevision = stopRevision;
                    continue;
                }
        if (stopRevision > 0) {
            for (RecordConsumer consumer : consumers.values()) {
                consumer.setRevision(stopRevision);
             }
            break;
            log.info("Synchronized to revision: " + stopRevision);
         }
     }
     
- 
2.19.1.windows.1

