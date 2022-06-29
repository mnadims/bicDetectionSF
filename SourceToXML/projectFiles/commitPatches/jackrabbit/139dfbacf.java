From 139dfbacfa26078f2687fd58395d63633d4fccc0 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Tue, 15 Dec 2015 09:01:50 +0000
Subject: [PATCH] JCR-3937: jackrabbit-jcr-commons bundle incorrectly has
 google dependency in Export-Package uses clause

Update to bundle plugin as suggested by David Bosschaert and Julian Sedding

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1720093 13f79535-47bb-0310-9956-ffa450edef68
--
 jackrabbit-bundle/pom.xml      | 4 ++--
 jackrabbit-jcr-commons/pom.xml | 4 ++--
 jackrabbit-parent/pom.xml      | 2 +-
 3 files changed, 5 insertions(+), 5 deletions(-)

diff --git a/jackrabbit-bundle/pom.xml b/jackrabbit-bundle/pom.xml
index 5ccf02d6b..666cbf0fa 100644
-- a/jackrabbit-bundle/pom.xml
++ b/jackrabbit-bundle/pom.xml
@@ -48,7 +48,6 @@
               org.apache.jackrabbit.api.*;version=${project.version}
             </Export-Package>
             <Import-Package>
              *,
               <!-- Optional dependencies from jackrabbit-core -->
               org.apache.jackrabbit.test;resolution:=optional,
               <!-- Optional dependencies from jackrabbit-webdav -->
@@ -60,7 +59,8 @@
               org.apache.xml.utils;resolution:=optional,
               org.apache.xalan.serialize;resolution:=optional,
               org.apache.xalan.templates;resolution:=optional,
              org.apache.derby.impl.drda;resolution:=optional
              org.apache.derby.impl.drda;resolution:=optional,
              *
             </Import-Package>
             <Bundle-Activator>
               org.apache.jackrabbit.bundle.Activator
diff --git a/jackrabbit-jcr-commons/pom.xml b/jackrabbit-jcr-commons/pom.xml
index 0f09b06cf..2135e7213 100644
-- a/jackrabbit-jcr-commons/pom.xml
++ b/jackrabbit-jcr-commons/pom.xml
@@ -46,8 +46,8 @@
               org.apache.jackrabbit.util.Base64
             </Main-Class>
             <Import-Package>
              *,
              org.apache.jackrabbit.api.security.user;version="[2.2,3)";resolution:=optional
              org.apache.jackrabbit.api.security.user;version="[2.2,3)";resolution:=optional,
              *
             </Import-Package>
           </instructions>
         </configuration>
diff --git a/jackrabbit-parent/pom.xml b/jackrabbit-parent/pom.xml
index b67ed2443..3b05a8e10 100644
-- a/jackrabbit-parent/pom.xml
++ b/jackrabbit-parent/pom.xml
@@ -235,7 +235,7 @@
         <plugin>
           <groupId>org.apache.felix</groupId>
           <artifactId>maven-bundle-plugin</artifactId>
          <version>2.3.4</version>
          <version>3.0.1</version>
           <inherited>true</inherited>
           <configuration>
             <instructions>
- 
2.19.1.windows.1

