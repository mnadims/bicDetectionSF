From a07fe82df90ebc16519e0091811bb22880fc9a22 Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Mon, 4 Jul 2016 13:26:25 +0000
Subject: [PATCH] JCR-3987: JcrUtils.getOrCreateByPath fails if session is not
 allowed to read root

(patch by Carsten Ziegeler)

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1751279 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/jackrabbit/commons/JcrUtils.java   | 21 +++++++++++++++++++
 1 file changed, 21 insertions(+)

diff --git a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/JcrUtils.java b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/JcrUtils.java
index ad99be0fd..4fefe019a 100644
-- a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/JcrUtils.java
++ b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/JcrUtils.java
@@ -1543,6 +1543,27 @@ public class JcrUtils {
             return baseNode.getNode(path);
         }
 
        // find the parent that exists
        // we can start from the deepest child in tree
        String fullPath = baseNode.getPath().equals("/") ? "/" + path : baseNode.getPath() + "/" + path;
        int currentIndex = fullPath.lastIndexOf('/');
        String temp = fullPath;
        String existingPath = null;
        while (currentIndex > 0) {
            temp = temp.substring(0, currentIndex);
            // break when first existing parent is found
            if (baseNode.getSession().itemExists(temp)) {
                existingPath = temp;
                break;
            }
            currentIndex = temp.lastIndexOf("/");
        }

        if (existingPath != null) {
            baseNode = baseNode.getSession().getNode(existingPath);
            path = path.substring(existingPath.length() + 1);
        }

         Node node = baseNode;
         int pos = path.lastIndexOf('/');
 
- 
2.19.1.windows.1

