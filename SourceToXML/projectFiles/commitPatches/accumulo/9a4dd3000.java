From 9a4dd3000ce5c8e1ebb884810b7ad3195bb1fa43 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Mon, 1 Jun 2015 18:34:47 -0400
Subject: [PATCH] ACCUMULO-3880 Remove halt on bad SystemToken.

While incorrect SystemTokens might sometimes be
the product of inconsistent system configuration,
it can also be used as an attack vector by
malicious parties. We need to treat invalid
authentications for the system user the same
as regular users (deny them and keep going).
--
 .../org/apache/accumulo/tserver/TabletServer.java     | 11 -----------
 1 file changed, 11 deletions(-)

diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index c5021668b..128aaa971 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -2206,7 +2206,6 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     private ZooCache masterLockCache = new ZooCache();
 
     private void checkPermission(TCredentials credentials, String lock, final String request) throws ThriftSecurityException {
      boolean fatal = false;
       try {
         log.debug("Got " + request + " message from user: " + credentials.getPrincipal());
         if (!security.canPerformSystemActions(credentials)) {
@@ -2217,18 +2216,8 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
         log.warn("Got " + request + " message from unauthenticatable user: " + e.getUser());
         if (SystemCredentials.get().getToken().getClass().getName().equals(credentials.getTokenClassName())) {
           log.fatal("Got message from a service with a mismatched configuration. Please ensure a compatible configuration.", e);
          fatal = true;
         }
         throw e;
      } finally {
        if (fatal) {
          Halt.halt(1, new Runnable() {
            @Override
            public void run() {
              logGCInfo(getSystemConfiguration());
            }
          });
        }
       }
 
       if (tabletServerLock == null || !tabletServerLock.wasLockAcquired()) {
- 
2.19.1.windows.1

