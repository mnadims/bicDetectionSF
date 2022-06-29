From 0e0aa49915d3311e9104d1e699a7b31fbb5943a5 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Mon, 22 Sep 2014 10:25:10 -0700
Subject: [PATCH] OOZIE-2014 TestAuthFilterAuthOozieClient fails after
 OOZIE-1917 (rkanter)

--
 .../TestAuthFilterAuthOozieClient.java        | 46 +++++++++++--------
 release-log.txt                               |  1 +
 2 files changed, 29 insertions(+), 18 deletions(-)

diff --git a/core/src/test/java/org/apache/oozie/servlet/TestAuthFilterAuthOozieClient.java b/core/src/test/java/org/apache/oozie/servlet/TestAuthFilterAuthOozieClient.java
index 63f57e22c..ef3a505ea 100644
-- a/core/src/test/java/org/apache/oozie/servlet/TestAuthFilterAuthOozieClient.java
++ b/core/src/test/java/org/apache/oozie/servlet/TestAuthFilterAuthOozieClient.java
@@ -18,6 +18,7 @@
 
 package org.apache.oozie.servlet;
 
import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.security.authentication.client.AuthenticatedURL;
 import org.apache.hadoop.security.authentication.client.AuthenticationException;
 import org.apache.hadoop.security.authentication.client.PseudoAuthenticator;
@@ -66,10 +67,15 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
         return new URL(sb.toString());
     }
 
    protected void runTest(Callable<Void> assertions) throws Exception {
    protected void runTest(Callable<Void> assertions, Configuration additionalConf) throws Exception {
         Services services = new Services();
         try {
             services.init();
            if (additionalConf != null) {
                for (Map.Entry<String, String> prop : additionalConf) {
                    Services.get().getConf().set(prop.getKey(), prop.getValue());
                }
            }
             Services.get().setService(ForTestAuthorizationService.class);
             Services.get().setService(ForTestWorkflowStoreService.class);
             Services.get().setService(MockDagEngineService.class);
@@ -104,7 +110,9 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
     }
 
     public void testClientWithAnonymous() throws Exception {
        setSystemProperty("oozie.authentication.simple.anonymous.allowed", "true");
        Configuration conf = new Configuration(false);
        conf.set("oozie.authentication.simple.anonymous.allowed", "true");

         runTest(new Callable<Void>() {
             public Void call() throws Exception {
                 String oozieUrl = getContextURL();
@@ -112,11 +120,13 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
                 assertEquals(0, new OozieCLI().run(args));
                 return null;
             }
        });
        }, conf);
     }
 
     public void testClientWithoutAnonymous() throws Exception {
        setSystemProperty("oozie.authentication.simple.anonymous.allowed", "false");
        Configuration conf = new Configuration(false);
        conf.set("oozie.authentication.simple.anonymous.allowed", "false");

         runTest(new Callable<Void>() {
             public Void call() throws Exception {
                 String oozieUrl = getContextURL();
@@ -124,12 +134,14 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
                 assertEquals(0, new OozieCLI().run(args));
                 return null;
             }
        });
        }, conf);
     }
 
     public void testClientWithCustomAuthenticator() throws Exception {
         setSystemProperty("authenticator.class", Authenticator4Test.class.getName());
        setSystemProperty("oozie.authentication.simple.anonymous.allowed", "false");
        Configuration conf = new Configuration(false);
        conf.set("oozie.authentication.simple.anonymous.allowed", "false");

         Authenticator4Test.USED = false;
         runTest(new Callable<Void>() {
             public Void call() throws Exception {
@@ -138,14 +150,17 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
                 assertEquals(0, new OozieCLI().run(args));
                 return null;
             }
        });
        }, conf);
         assertTrue(Authenticator4Test.USED);
     }
 
 
     public void testClientAuthTokenCache() throws Exception {
        Configuration conf = new Configuration(false);
        conf.set("oozie.authentication.signature.secret", "secret");
        conf.set("oozie.authentication.simple.anonymous.allowed", "false");

         //not using cache
        setSystemProperty("oozie.authentication.simple.anonymous.allowed", "false");
         AuthOozieClient.AUTH_TOKEN_CACHE_FILE.delete();
         assertFalse(AuthOozieClient.AUTH_TOKEN_CACHE_FILE.exists());
         runTest(new Callable<Void>() {
@@ -155,13 +170,11 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
                 assertEquals(0, new OozieCLI().run(args));
                 return null;
             }
        });
        }, conf);
         assertFalse(AuthOozieClient.AUTH_TOKEN_CACHE_FILE.exists());
 
         //using cache
         setSystemProperty("oozie.auth.token.cache", "true");
        setSystemProperty("oozie.authentication.simple.anonymous.allowed", "false");
        setSystemProperty("oozie.authentication.signature.secret", "secret");
         AuthOozieClient.AUTH_TOKEN_CACHE_FILE.delete();
         assertFalse(AuthOozieClient.AUTH_TOKEN_CACHE_FILE.exists());
         runTest(new Callable<Void>() {
@@ -171,14 +184,12 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
                 assertEquals(0, new OozieCLI().run(args));
                 return null;
             }
        });
        }, conf);
         assertTrue(AuthOozieClient.AUTH_TOKEN_CACHE_FILE.exists());
         String currentCache = IOUtils.getReaderAsString(new FileReader(AuthOozieClient.AUTH_TOKEN_CACHE_FILE), -1);
 
         //re-using cache
         setSystemProperty("oozie.auth.token.cache", "true");
        setSystemProperty("oozie.authentication.simple.anonymous.allowed", "false");
        setSystemProperty("oozie.authentication.signature.secret", "secret");
         runTest(new Callable<Void>() {
             public Void call() throws Exception {
                 String oozieUrl = getContextURL();
@@ -186,7 +197,7 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
                 assertEquals(0, new OozieCLI().run(args));
                 return null;
             }
        });
        }, conf);
         assertTrue(AuthOozieClient.AUTH_TOKEN_CACHE_FILE.exists());
         String newCache = IOUtils.getReaderAsString(new FileReader(AuthOozieClient.AUTH_TOKEN_CACHE_FILE), -1);
         assertEquals(currentCache, newCache);
@@ -205,7 +216,7 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
                 assertEquals(0, new OozieCLI().run(args));
                 return null;
             }
        });
        }, null);
         // bad method
         runTest(new Callable<Void>() {
             public Void call() throws Exception {
@@ -215,7 +226,6 @@ public class TestAuthFilterAuthOozieClient extends XTestCase {
                 assertEquals(-1, new OozieCLI().run(args));
                 return null;
             }
        });

        }, null);
     }
 }
diff --git a/release-log.txt b/release-log.txt
index 054bd4732..4bc79df4e 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-2014 TestAuthFilterAuthOozieClient fails after OOZIE-1917 (rkanter)
 OOZIE-1917 Authentication secret should be random by default and needs to coordinate with HA (rkanter)
 OOZIE-1853 Improve the Credentials documentation (rkanter)
 OOZIE-1954 Add a way for the MapReduce action to be configured by Java code (rkanter)
- 
2.19.1.windows.1

