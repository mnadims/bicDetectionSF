From 546fe0d4eecbffd2a2f758e96cbad10b68080976 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Thu, 23 Sep 2010 11:52:16 +0000
Subject: [PATCH] JCR-2750: MultiStatusResponse should not call
 resource.getProperties

Remove unused imports

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1000415 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/jackrabbit/webdav/MultiStatusResponse.java  | 2 --
 1 file changed, 2 deletions(-)

diff --git a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatusResponse.java b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatusResponse.java
index ea8cec778..e291404d9 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatusResponse.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatusResponse.java
@@ -17,9 +17,7 @@
 package org.apache.jackrabbit.webdav;
 
 import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyIterator;
 import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameIterator;
 import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
 import org.apache.jackrabbit.webdav.property.DavPropertySet;
 import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
- 
2.19.1.windows.1

