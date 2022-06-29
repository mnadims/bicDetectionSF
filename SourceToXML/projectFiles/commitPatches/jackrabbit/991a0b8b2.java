From 991a0b8b2e65b7fcfdf1a8c1a03fd68c19b32866 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Fri, 24 Sep 2010 11:08:12 +0000
Subject: [PATCH] JCR-2755: ConcurrentModificationException in WebDAV UPDATE

Use synchronization to prevent the ConcurrentModificatinException

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1000806 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/webdav/MultiStatus.java    | 12 +++++++++---
 1 file changed, 9 insertions(+), 3 deletions(-)

diff --git a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatus.java b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatus.java
index b36004adb..2f3cae8c8 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatus.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/MultiStatus.java
@@ -34,7 +34,7 @@ public class MultiStatus implements DavConstants, XmlSerializable {
 
     /**
      * Map collecting the responses for this multistatus, where every href must
     * only occure one single time.
     * only occur one single time.
      */
     private Map<String, MultiStatusResponse> responses = new LinkedHashMap<String, MultiStatusResponse>();
 
@@ -105,20 +105,26 @@ public class MultiStatus implements DavConstants, XmlSerializable {
 
     /**
      * Add a <code>MultiStatusResponse</code> element to this <code>MultiStatus</code>
     * <p>
     * This method is synchronized to avoid the problem described in
     * <a href="https://issues.apache.org/jira/browse/JCR-2755">JCR-2755</a>.
      *
      * @param response
      */
    public void addResponse(MultiStatusResponse response) {
    public synchronized void addResponse(MultiStatusResponse response) {
         responses.put(response.getHref(), response);
     }
 
     /**
      * Returns the multistatus responses present as array.
     * <p>
     * This method is synchronized to avoid the problem described in
     * <a href="https://issues.apache.org/jira/browse/JCR-2755">JCR-2755</a>.
      *
      * @return array of all {@link MultiStatusResponse responses} present in this
      * multistatus.
      */
    public MultiStatusResponse[] getResponses() {
    public synchronized MultiStatusResponse[] getResponses() {
         return responses.values().toArray(new MultiStatusResponse[responses.size()]);
     }
 
- 
2.19.1.windows.1

