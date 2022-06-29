From fe2cb1763ae7e212307edeb5bf38f243734062a0 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 22 May 2007 10:33:35 +0000
Subject: [PATCH] JCR-892: Correct use of TransformerHandler also in
 contrib/backup

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@540525 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/jackrabbit/backup/WorkspaceBackup.java  | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)

diff --git a/contrib/backup/src/main/java/org/apache/jackrabbit/backup/WorkspaceBackup.java b/contrib/backup/src/main/java/org/apache/jackrabbit/backup/WorkspaceBackup.java
index 73c17dc4b..841f8f28f 100644
-- a/contrib/backup/src/main/java/org/apache/jackrabbit/backup/WorkspaceBackup.java
++ b/contrib/backup/src/main/java/org/apache/jackrabbit/backup/WorkspaceBackup.java
@@ -85,10 +85,10 @@ public class WorkspaceBackup extends Backup {
         File temp = new File(this.getConf().getWorkFolder() + "wsp.xml");
         try {
             TransformerHandler th = stf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
            th.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setOutputProperty(OutputKeys.INDENT, "no");
             th.setResult(new StreamResult(new FileOutputStream(temp)));
            th.getTransformer().setParameter(OutputKeys.METHOD, "xml");
            th.getTransformer().setParameter(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setParameter(OutputKeys.INDENT, "no");
             
             new SysViewSAXEventGenerator(
                     s.getRootNode(), false, false, th) {
- 
2.19.1.windows.1

