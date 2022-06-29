From 56fd858deb3d8047370649fad9df11c0b3e7ad53 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Wed, 17 Sep 2014 17:16:33 -0700
Subject: [PATCH] OOZIE-1917 Authentication secret should be random by default
 and needs to coordinate with HA (rkanter)

--
 core/src/main/conf/oozie-site.xml             | 11 --------
 .../org/apache/oozie/servlet/AuthFilter.java  | 28 +++++++++++++++++++
 core/src/main/resources/oozie-default.xml     | 11 --------
 docs/src/site/twiki/AG_Install.twiki          | 11 ++++++--
 release-log.txt                               |  1 +
 5 files changed, 38 insertions(+), 24 deletions(-)

diff --git a/core/src/main/conf/oozie-site.xml b/core/src/main/conf/oozie-site.xml
index 703d321eb..c028ca225 100644
-- a/core/src/main/conf/oozie-site.xml
++ b/core/src/main/conf/oozie-site.xml
@@ -280,17 +280,6 @@
         </description>
     </property>
 
    <property>
        <name>oozie.authentication.signature.secret</name>
        <value>oozie</value>
        <description>
            The signature secret for signing the authentication tokens.
            If not set a random secret is generated at startup time.
            In order to authentiation to work correctly across multiple hosts
            the secret must be the same across al the hosts.
        </description>
    </property>

     <property>
       <name>oozie.authentication.cookie.domain</name>
       <value></value>
diff --git a/core/src/main/java/org/apache/oozie/servlet/AuthFilter.java b/core/src/main/java/org/apache/oozie/servlet/AuthFilter.java
index 85e507216..054f48402 100644
-- a/core/src/main/java/org/apache/oozie/servlet/AuthFilter.java
++ b/core/src/main/java/org/apache/oozie/servlet/AuthFilter.java
@@ -32,6 +32,8 @@ import javax.servlet.http.HttpServletRequest;
 import java.io.IOException;
 import java.util.Map;
 import java.util.Properties;
import org.apache.oozie.service.JobsConcurrencyService;
import org.apache.oozie.util.ZKUtils;
 
 /**
  * Authentication filter that extends Hadoop-auth AuthenticationFilter to override
@@ -41,6 +43,7 @@ public class AuthFilter extends AuthenticationFilter {
     private static final String OOZIE_PREFIX = "oozie.authentication.";
 
     private HttpServlet optionsServlet;
    private ZKUtils zkUtils = null;
 
     /**
      * Initialize the filter.
@@ -50,6 +53,15 @@ public class AuthFilter extends AuthenticationFilter {
      */
     @Override
     public void init(FilterConfig filterConfig) throws ServletException {
        // If using HA, we'd like to use our Curator client with ZKSignerSecretProvider, so we have to pass it
        if (Services.get().get(JobsConcurrencyService.class).isHighlyAvailableMode()) {
            try {
                zkUtils = ZKUtils.register(this);
            } catch(Exception e) {
                throw new ServletException(e);
            }
            filterConfig.getServletContext().setAttribute("signer.secret.provider.zookeeper.curator.client", zkUtils.getClient());
        }
         super.init(filterConfig);
         optionsServlet = new HttpServlet() {};
         optionsServlet.init();
@@ -61,6 +73,9 @@ public class AuthFilter extends AuthenticationFilter {
     @Override
     public void destroy() {
         optionsServlet.destroy();
        if (zkUtils != null) {
            zkUtils.unregister(this);
        }
         super.destroy();
     }
 
@@ -94,6 +109,19 @@ public class AuthFilter extends AuthenticationFilter {
             }
         }
 
        // If using HA, we need to set some extra configs for the ZKSignerSecretProvider.  No need to bother the user with these
        // details, so we'll set them for the user (unless the user really wants to set them)
        if (Services.get().get(JobsConcurrencyService.class).isHighlyAvailableMode()) {
            if (!props.containsKey("signer.secret.provider")) {
                props.setProperty("signer.secret.provider", "zookeeper");
            }
            if (!props.containsKey("signer.secret.provider.zookeeper.path")) {
                props.setProperty("signer.secret.provider.zookeeper.path",
                        ZKUtils.ZK_BASE_SERVICES_PATH + "/signersecrets");
            }
            props.setProperty("signer.secret.provider.zookeeper.disconnect.on.shutdown", "false");
        }

         return props;
     }
 
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index 6a91dc645..874b9780f 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -1823,17 +1823,6 @@
         </description>
     </property>
 
    <property>
        <name>oozie.authentication.signature.secret</name>
        <value>oozie</value>
        <description>
            The signature secret for signing the authentication tokens.
            If not set a random secret is generated at startup time.
            In order to authentiation to work correctly across multiple hosts
            the secret must be the same across al the hosts.
        </description>
    </property>

     <property>
       <name>oozie.authentication.cookie.domain</name>
       <value></value>
diff --git a/docs/src/site/twiki/AG_Install.twiki b/docs/src/site/twiki/AG_Install.twiki
index 06659f82e..c92d53055 100644
-- a/docs/src/site/twiki/AG_Install.twiki
++ b/docs/src/site/twiki/AG_Install.twiki
@@ -281,8 +281,8 @@ simple | kerberos | #AUTHENTICATION_HANDLER_CLASSNAME#.
 The =token.validity= indicates how long (in seconds) an authentication token is valid before it has
 to be renewed.
 
The =signature.secret= is the signature secret for signing the authentication tokens. If not set a random
secret is generated at startup time.
The =signature.secret= is the signature secret for signing the authentication tokens. It is recommended to not set this, in which
case Oozie will randomly generate one on startup.
 
 The =oozie.authentication.cookie.domain= The domain to use for the HTTP cookie that stores the
 authentication token. In order to authentiation to work correctly across all Hadoop nodes web-consoles
@@ -881,6 +881,13 @@ For earlier versions of Hadoop:
 
 2b. Set =oozie.authentication.kerberos.principal= to =HTTP/load-balancer-host@realm=.
 
3. With Hadoop 2.6.0 and later, a rolling random secret that is synchronized across all Oozie servers will be used for signing the
Oozie auth tokens.  This is done automatically when HA is enabled; no additional configuration is needed.

For earlier versions of Hadoop, each server will have a different random secret.  This will still work but will likely result in
additional calls to the KDC to authenticate users to the Oozie server (because the auth tokens will not be accepted by other
servers, which will cause a fallback to Kerberos).

 
 ---++++ JobId sequence
 Oozie in HA mode, uses ZK to generate job id sequence. Job Ids are of following format.
diff --git a/release-log.txt b/release-log.txt
index 5067a5eea..054bd4732 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-1917 Authentication secret should be random by default and needs to coordinate with HA (rkanter)
 OOZIE-1853 Improve the Credentials documentation (rkanter)
 OOZIE-1954 Add a way for the MapReduce action to be configured by Java code (rkanter)
 OOZIE-2003 Checkstyle issues (rkanter via shwethags)
- 
2.19.1.windows.1

