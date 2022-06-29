From 5246817b844b6770e0be03b4586ffff607112bd6 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Mon, 16 Nov 2009 13:34:56 +0000
Subject: [PATCH] JCR-2385: webdav: nullpointer exception while getting the
 tikka detector

The IOManager instance is not available when a DefaultHandler is used as a PropertyHandler implementation. In such cases we can simply return application/octet-stream as the content type.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@880743 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/server/io/DefaultHandler.java  | 9 +++++----
 1 file changed, 5 insertions(+), 4 deletions(-)

diff --git a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultHandler.java b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultHandler.java
index 4dd4bc803..c0073d21a 100644
-- a/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultHandler.java
++ b/jackrabbit-jcr-server/src/main/java/org/apache/jackrabbit/server/io/DefaultHandler.java
@@ -24,9 +24,6 @@ import org.apache.jackrabbit.webdav.DavResource;
 import org.apache.jackrabbit.webdav.xml.Namespace;
 import org.apache.jackrabbit.webdav.property.DavPropertyName;
 import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
 import org.apache.tika.metadata.Metadata;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -665,7 +662,11 @@ public class DefaultHandler implements IOHandler, PropertyHandler {
         try {
             Metadata metadata = new Metadata();
             metadata.set(Metadata.RESOURCE_NAME_KEY, name);
            return ioManager.getDetector().detect(null, metadata).toString();
            if (ioManager != null && ioManager.getDetector() != null) {
                return ioManager.getDetector().detect(null, metadata).toString();
            } else {
                return "application/octet-stream";
            }
         } catch (IOException e) {
             // Can not happen since the InputStream above is null
             throw new IllegalStateException(
- 
2.19.1.windows.1

