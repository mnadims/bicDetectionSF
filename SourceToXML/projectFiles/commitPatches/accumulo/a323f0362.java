From a323f0362417c9f62dd9d6d3ee6c513e78fa03cc Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Tue, 10 Feb 2015 20:04:01 -0500
Subject: [PATCH] ACCUMULO-3576 Use guava's preconditions, not jline's

--
 .../core/client/security/tokens/CredentialProviderToken.java  | 4 ++--
 .../accumulo/core/conf/CredentialProviderFactoryShim.java     | 4 ++--
 2 files changed, 4 insertions(+), 4 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/security/tokens/CredentialProviderToken.java b/core/src/main/java/org/apache/accumulo/core/client/security/tokens/CredentialProviderToken.java
index baae41126..d8ebc4a10 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/security/tokens/CredentialProviderToken.java
++ b/core/src/main/java/org/apache/accumulo/core/client/security/tokens/CredentialProviderToken.java
@@ -21,12 +21,12 @@ import java.nio.CharBuffer;
 import java.util.LinkedHashSet;
 import java.util.Set;
 
import jline.internal.Preconditions;

 import org.apache.accumulo.core.conf.CredentialProviderFactoryShim;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.hadoop.conf.Configuration;
 
import com.google.common.base.Preconditions;

 /**
  * An {@link AuthenticationToken} backed by a Hadoop CredentialProvider.
  */
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/CredentialProviderFactoryShim.java b/core/src/main/java/org/apache/accumulo/core/conf/CredentialProviderFactoryShim.java
index 81fe54043..3c3c05148 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/CredentialProviderFactoryShim.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/CredentialProviderFactoryShim.java
@@ -23,13 +23,13 @@ import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 
import jline.internal.Preconditions;

 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.hadoop.conf.Configuration;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import com.google.common.base.Preconditions;

 /**
  * Shim around Hadoop: tries to use the CredentialProviderFactory provided by hadoop-common, falling back to a copy inside accumulo-core.
  * <p>
- 
2.19.1.windows.1

