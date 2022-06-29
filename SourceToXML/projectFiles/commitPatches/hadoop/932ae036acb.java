From 932ae036acb96634c5dd435d57ba02ce4d5e8918 Mon Sep 17 00:00:00 2001
From: Alejandro Abdelnur <tucu@apache.org>
Date: Mon, 15 Sep 2014 17:05:42 -0700
Subject: [PATCH] HADOOP-10868. AuthenticationFilter should support
 externalizing the secret for signing and provide rotation support. (rkanter
 via tucu)

--
 hadoop-common-project/hadoop-auth/pom.xml     |  13 +
 .../server/AuthenticationFilter.java          | 152 ++++--
 .../util/RandomSignerSecretProvider.java      |   4 +-
 .../util/RolloverSignerSecretProvider.java    |   7 +-
 .../util/SignerSecretProvider.java            |   9 +-
 .../util/StringSignerSecretProvider.java      |  15 +-
 .../util/ZKSignerSecretProvider.java          | 503 ++++++++++++++++++
 .../src/site/apt/Configuration.apt.vm         | 148 +++++-
 .../hadoop-auth/src/site/apt/index.apt.vm     |   5 +
 .../server/TestAuthenticationFilter.java      | 117 +++-
 .../util/TestJaasConfiguration.java           |  55 ++
 .../util/TestRandomSignerSecretProvider.java  |   2 +-
 .../TestRolloverSignerSecretProvider.java     |   2 +-
 .../authentication/util/TestSigner.java       |  23 +-
 .../util/TestStringSignerSecretProvider.java  |   9 +-
 .../util/TestZKSignerSecretProvider.java      | 270 ++++++++++
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../fs/http/server/TestHttpFSServer.java      |   8 +-
 hadoop-project/pom.xml                        |  11 +
 19 files changed, 1259 insertions(+), 97 deletions(-)
 create mode 100644 hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/ZKSignerSecretProvider.java
 create mode 100644 hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestJaasConfiguration.java
 create mode 100644 hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestZKSignerSecretProvider.java

diff --git a/hadoop-common-project/hadoop-auth/pom.xml b/hadoop-common-project/hadoop-auth/pom.xml
index 564518c540b..5f7d77434bc 100644
-- a/hadoop-common-project/hadoop-auth/pom.xml
++ b/hadoop-common-project/hadoop-auth/pom.xml
@@ -130,6 +130,19 @@
           </exclusion>
         </exclusions>
     </dependency>
    <dependency>
      <groupId>org.apache.zookeeper</groupId>
      <artifactId>zookeeper</artifactId>
    </dependency>
    <dependency>
      <groupId>org.apache.curator</groupId>
      <artifactId>curator-framework</artifactId>
    </dependency>
    <dependency>
      <groupId>org.apache.curator</groupId>
      <artifactId>curator-test</artifactId>
      <scope>test</scope>
    </dependency>
   </dependencies>
 
   <build>
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
index 9330444c46e..47cf54c6066 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
@@ -22,6 +22,7 @@
 import org.apache.hadoop.security.authentication.util.RandomSignerSecretProvider;
 import org.apache.hadoop.security.authentication.util.SignerSecretProvider;
 import org.apache.hadoop.security.authentication.util.StringSignerSecretProvider;
import org.apache.hadoop.security.authentication.util.ZKSignerSecretProvider;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -42,7 +43,7 @@
 
 /**
  * The {@link AuthenticationFilter} enables protecting web application resources with different (pluggable)
 * authentication mechanisms.
 * authentication mechanisms and signer secret providers.
  * <p/>
  * Out of the box it provides 2 authentication mechanisms: Pseudo and Kerberos SPNEGO.
  * <p/>
@@ -60,10 +61,13 @@
  * <li>[#PREFIX#.]type: simple|kerberos|#CLASS#, 'simple' is short for the
  * {@link PseudoAuthenticationHandler}, 'kerberos' is short for {@link KerberosAuthenticationHandler}, otherwise
  * the full class name of the {@link AuthenticationHandler} must be specified.</li>
 * <li>[#PREFIX#.]signature.secret: the secret used to sign the HTTP cookie value. The default value is a random
 * value. Unless multiple webapp instances need to share the secret the random value is adequate.</li>
 * <li>[#PREFIX#.]token.validity: time -in seconds- that the generated token is valid before a
 * new authentication is triggered, default value is <code>3600</code> seconds.</li>
 * <li>[#PREFIX#.]signature.secret: when signer.secret.provider is set to
 * "string" or not specified, this is the value for the secret used to sign the
 * HTTP cookie.</li>
 * <li>[#PREFIX#.]token.validity: time -in seconds- that the generated token is
 * valid before a new authentication is triggered, default value is
 * <code>3600</code> seconds. This is also used for the rollover interval for
 * the "random" and "zookeeper" SignerSecretProviders.</li>
  * <li>[#PREFIX#.]cookie.domain: domain to use for the HTTP cookie that stores the authentication token.</li>
  * <li>[#PREFIX#.]cookie.path: path to use for the HTTP cookie that stores the authentication token.</li>
  * </ul>
@@ -72,6 +76,49 @@
  * {@link AuthenticationFilter} will take all the properties that start with the prefix #PREFIX#, it will remove
  * the prefix from it and it will pass them to the the authentication handler for initialization. Properties that do
  * not start with the prefix will not be passed to the authentication handler initialization.
 * <p/>
 * Out of the box it provides 3 signer secret provider implementations:
 * "string", "random", and "zookeeper"
 * <p/>
 * Additional signer secret providers are supported via the
 * {@link SignerSecretProvider} class.
 * <p/>
 * For the HTTP cookies mentioned above, the SignerSecretProvider is used to
 * determine the secret to use for signing the cookies. Different
 * implementations can have different behaviors.  The "string" implementation
 * simply uses the string set in the [#PREFIX#.]signature.secret property
 * mentioned above.  The "random" implementation uses a randomly generated
 * secret that rolls over at the interval specified by the
 * [#PREFIX#.]token.validity mentioned above.  The "zookeeper" implementation
 * is like the "random" one, except that it synchronizes the random secret
 * and rollovers between multiple servers; it's meant for HA services.
 * <p/>
 * The relevant configuration properties are:
 * <ul>
 * <li>signer.secret.provider: indicates the name of the SignerSecretProvider
 * class to use. Possible values are: "string", "random", "zookeeper", or a
 * classname. If not specified, the "string" implementation will be used with
 * [#PREFIX#.]signature.secret; and if that's not specified, the "random"
 * implementation will be used.</li>
 * <li>[#PREFIX#.]signature.secret: When the "string" implementation is
 * specified, this value is used as the secret.</li>
 * <li>[#PREFIX#.]token.validity: When the "random" or "zookeeper"
 * implementations are specified, this value is used as the rollover
 * interval.</li>
 * </ul>
 * <p/>
 * The "zookeeper" implementation has additional configuration properties that
 * must be specified; see {@link ZKSignerSecretProvider} for details.
 * <p/>
 * For subclasses of AuthenticationFilter that want additional control over the
 * SignerSecretProvider, they can use the following attribute set in the
 * ServletContext:
 * <ul>
 * <li>signer.secret.provider.object: A SignerSecretProvider implementation can
 * be passed here that will be used instead of the signer.secret.provider
 * configuration property. Note that the class should already be
 * initialized.</li>
 * </ul>
  */
 
 @InterfaceAudience.Private
@@ -112,20 +159,23 @@
 
   /**
    * Constant for the configuration property that indicates the name of the
   * SignerSecretProvider class to use.  If not specified, SIGNATURE_SECRET
   * will be used or a random secret.
   * SignerSecretProvider class to use.
   * Possible values are: "string", "random", "zookeeper", or a classname.
   * If not specified, the "string" implementation will be used with
   * SIGNATURE_SECRET; and if that's not specified, the "random" implementation
   * will be used.
    */
  public static final String SIGNER_SECRET_PROVIDER_CLASS =
  public static final String SIGNER_SECRET_PROVIDER =
           "signer.secret.provider";
 
   /**
   * Constant for the attribute that can be used for providing a custom
   * object that subclasses the SignerSecretProvider.  Note that this should be
   * set in the ServletContext and the class should already be initialized.  
   * If not specified, SIGNER_SECRET_PROVIDER_CLASS will be used.
   * Constant for the ServletContext attribute that can be used for providing a
   * custom implementation of the SignerSecretProvider. Note that the class
   * should already be initialized. If not specified, SIGNER_SECRET_PROVIDER
   * will be used.
    */
  public static final String SIGNATURE_PROVIDER_ATTRIBUTE =
      "org.apache.hadoop.security.authentication.util.SignerSecretProvider";
  public static final String SIGNER_SECRET_PROVIDER_ATTRIBUTE =
      "signer.secret.provider.object";
 
   private Properties config;
   private Signer signer;
@@ -138,7 +188,7 @@
   private String cookiePath;
 
   /**
   * Initializes the authentication filter.
   * Initializes the authentication filter and signer secret provider.
    * <p/>
    * It instantiates and initializes the specified {@link AuthenticationHandler}.
    * <p/>
@@ -184,35 +234,19 @@ public void init(FilterConfig filterConfig) throws ServletException {
     validity = Long.parseLong(config.getProperty(AUTH_TOKEN_VALIDITY, "36000"))
         * 1000; //10 hours
     secretProvider = (SignerSecretProvider) filterConfig.getServletContext().
        getAttribute(SIGNATURE_PROVIDER_ATTRIBUTE);
        getAttribute(SIGNER_SECRET_PROVIDER_ATTRIBUTE);
     if (secretProvider == null) {
      String signerSecretProviderClassName =
          config.getProperty(configPrefix + SIGNER_SECRET_PROVIDER_CLASS, null);
      if (signerSecretProviderClassName == null) {
        String signatureSecret =
            config.getProperty(configPrefix + SIGNATURE_SECRET, null);
        if (signatureSecret != null) {
          secretProvider = new StringSignerSecretProvider(signatureSecret);
        } else {
          secretProvider = new RandomSignerSecretProvider();
          randomSecret = true;
        }
      } else {
        try {
          Class<?> klass = Thread.currentThread().getContextClassLoader().
              loadClass(signerSecretProviderClassName);
          secretProvider = (SignerSecretProvider) klass.newInstance();
          customSecretProvider = true;
        } catch (ClassNotFoundException ex) {
          throw new ServletException(ex);
        } catch (InstantiationException ex) {
          throw new ServletException(ex);
        } catch (IllegalAccessException ex) {
          throw new ServletException(ex);
        }
      Class<? extends SignerSecretProvider> providerClass
              = getProviderClass(config);
      try {
        secretProvider = providerClass.newInstance();
      } catch (InstantiationException ex) {
        throw new ServletException(ex);
      } catch (IllegalAccessException ex) {
        throw new ServletException(ex);
       }
       try {
        secretProvider.init(config, validity);
        secretProvider.init(config, filterConfig.getServletContext(), validity);
       } catch (Exception ex) {
         throw new ServletException(ex);
       }
@@ -225,6 +259,42 @@ public void init(FilterConfig filterConfig) throws ServletException {
     cookiePath = config.getProperty(COOKIE_PATH, null);
   }
 
  @SuppressWarnings("unchecked")
  private Class<? extends SignerSecretProvider> getProviderClass(Properties config)
          throws ServletException {
    String providerClassName;
    String signerSecretProviderName
            = config.getProperty(SIGNER_SECRET_PROVIDER, null);
    // fallback to old behavior
    if (signerSecretProviderName == null) {
      String signatureSecret = config.getProperty(SIGNATURE_SECRET, null);
      if (signatureSecret != null) {
        providerClassName = StringSignerSecretProvider.class.getName();
      } else {
        providerClassName = RandomSignerSecretProvider.class.getName();
        randomSecret = true;
      }
    } else {
      if ("random".equals(signerSecretProviderName)) {
        providerClassName = RandomSignerSecretProvider.class.getName();
        randomSecret = true;
      } else if ("string".equals(signerSecretProviderName)) {
        providerClassName = StringSignerSecretProvider.class.getName();
      } else if ("zookeeper".equals(signerSecretProviderName)) {
        providerClassName = ZKSignerSecretProvider.class.getName();
      } else {
        providerClassName = signerSecretProviderName;
        customSecretProvider = true;
      }
    }
    try {
      return (Class<? extends SignerSecretProvider>) Thread.currentThread().
              getContextClassLoader().loadClass(providerClassName);
    } catch (ClassNotFoundException ex) {
      throw new ServletException(ex);
    }
  }

   /**
    * Returns the configuration properties of the {@link AuthenticationFilter}
    * without the prefix. The returned properties are the same that the
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/RandomSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/RandomSignerSecretProvider.java
index 5491a8671bf..29e5661cb0b 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/RandomSignerSecretProvider.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/RandomSignerSecretProvider.java
@@ -13,12 +13,13 @@
  */
 package org.apache.hadoop.security.authentication.util;
 
import com.google.common.annotations.VisibleForTesting;
 import java.util.Random;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
 
 /**
 * A SignerSecretProvider that uses a random number as it's secret.  It rolls
 * A SignerSecretProvider that uses a random number as its secret.  It rolls
  * the secret at a regular interval.
  */
 @InterfaceStability.Unstable
@@ -37,6 +38,7 @@ public RandomSignerSecretProvider() {
    * is meant for testing.
    * @param seed the seed for the random number generator
    */
  @VisibleForTesting
   public RandomSignerSecretProvider(long seed) {
     super();
     rand = new Random(seed);
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/RolloverSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/RolloverSignerSecretProvider.java
index ec6e601b4d9..bdca3e4eb94 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/RolloverSignerSecretProvider.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/RolloverSignerSecretProvider.java
@@ -17,6 +17,7 @@
 import java.util.concurrent.Executors;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
 import org.slf4j.Logger;
@@ -57,12 +58,14 @@ public RolloverSignerSecretProvider() {
    * Initialize the SignerSecretProvider.  It initializes the current secret
    * and starts the scheduler for the rollover to run at an interval of
    * tokenValidity.
   * @param config filter configuration
   * @param config configuration properties
   * @param servletContext servlet context
    * @param tokenValidity The amount of time a token is valid for
    * @throws Exception
    */
   @Override
  public void init(Properties config, long tokenValidity) throws Exception {
  public void init(Properties config, ServletContext servletContext,
          long tokenValidity) throws Exception {
     initSecrets(generateNewSecret(), null);
     startScheduler(tokenValidity, tokenValidity);
   }
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/SignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/SignerSecretProvider.java
index a4d98d784f8..2e0b9854898 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/SignerSecretProvider.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/SignerSecretProvider.java
@@ -14,6 +14,7 @@
 package org.apache.hadoop.security.authentication.util;
 
 import java.util.Properties;
import javax.servlet.ServletContext;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
 
@@ -30,13 +31,13 @@
 
   /**
    * Initialize the SignerSecretProvider
   * @param config filter configuration
   * @param config configuration properties
   * @param servletContext servlet context
    * @param tokenValidity The amount of time a token is valid for
    * @throws Exception
    */
  public abstract void init(Properties config, long tokenValidity)
      throws Exception;

  public abstract void init(Properties config, ServletContext servletContext,
          long tokenValidity) throws Exception;
   /**
    * Will be called on shutdown; subclasses should perform any cleanup here.
    */
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java
index 230059b645b..7aaccd2914c 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java
@@ -14,8 +14,10 @@
 package org.apache.hadoop.security.authentication.util;
 
 import java.util.Properties;
import javax.servlet.ServletContext;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
 
 /**
  * A SignerSecretProvider that simply creates a secret based on a given String.
@@ -27,14 +29,15 @@
   private byte[] secret;
   private byte[][] secrets;
 
  public StringSignerSecretProvider(String secretStr) {
    secret = secretStr.getBytes();
    secrets = new byte[][]{secret};
  }
  public StringSignerSecretProvider() {}
 
   @Override
  public void init(Properties config, long tokenValidity) throws Exception {
    // do nothing
  public void init(Properties config, ServletContext servletContext,
          long tokenValidity) throws Exception {
    String signatureSecret = config.getProperty(
            AuthenticationFilter.SIGNATURE_SECRET, null);
    secret = signatureSecret.getBytes();
    secrets = new byte[][]{secret};
   }
 
   @Override
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/ZKSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/ZKSignerSecretProvider.java
new file mode 100644
index 00000000000..45d4d65307a
-- /dev/null
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/ZKSignerSecretProvider.java
@@ -0,0 +1,503 @@
/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. See accompanying LICENSE file.
 */
package org.apache.hadoop.security.authentication.util;

import com.google.common.annotations.VisibleForTesting;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.servlet.ServletContext;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.imps.DefaultACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.client.ZooKeeperSaslClient;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SignerSecretProvider that synchronizes a rolling random secret between
 * multiple servers using ZooKeeper.
 * <p/>
 * It works by storing the secrets and next rollover time in a ZooKeeper znode.
 * All ZKSignerSecretProviders looking at that znode will use those
 * secrets and next rollover time to ensure they are synchronized.  There is no
 * "leader" -- any of the ZKSignerSecretProviders can choose the next secret;
 * which one is indeterminate.  Kerberos-based ACLs can also be enforced to
 * prevent a malicious third-party from getting or setting the secrets.  It uses
 * its own CuratorFramework client for talking to ZooKeeper.  If you want to use
 * your own Curator client, you can pass it to ZKSignerSecretProvider; see
 * {@link org.apache.hadoop.security.authentication.server.AuthenticationFilter}
 * for more details.
 * <p/>
 * The supported configuration properties are:
 * <ul>
 * <li>signer.secret.provider.zookeeper.connection.string: indicates the
 * ZooKeeper connection string to connect with.</li>
 * <li>signer.secret.provider.zookeeper.path: indicates the ZooKeeper path
 * to use for storing and retrieving the secrets.  All ZKSignerSecretProviders
 * that need to coordinate should point to the same path.</li>
 * <li>signer.secret.provider.zookeeper.auth.type: indicates the auth type to
 * use.  Supported values are "none" and "sasl".  The default value is "none"
 * </li>
 * <li>signer.secret.provider.zookeeper.kerberos.keytab: set this to the path
 * with the Kerberos keytab file.  This is only required if using Kerberos.</li>
 * <li>signer.secret.provider.zookeeper.kerberos.principal: set this to the
 * Kerberos principal to use.  This only required if using Kerberos.</li>
 * <li>signer.secret.provider.zookeeper.disconnect.on.close: when set to "true",
 * ZKSignerSecretProvider will close the ZooKeeper connection on shutdown.  The
 * default is "true". Only set this to "false" if a custom Curator client is
 * being provided and the disconnection is being handled elsewhere.</li>
 * </ul>
 *
 * The following attribute in the ServletContext can also be set if desired:
 * <li>signer.secret.provider.zookeeper.curator.client: A CuratorFramework
 * client object can be passed here. If given, the "zookeeper" implementation
 * will use this Curator client instead of creating its own, which is useful if
 * you already have a Curator client or want more control over its
 * configuration.</li>
 */
@InterfaceStability.Unstable
@InterfaceAudience.Private
public class ZKSignerSecretProvider extends RolloverSignerSecretProvider {

  private static final String CONFIG_PREFIX =
          "signer.secret.provider.zookeeper.";

  /**
   * Constant for the property that specifies the ZooKeeper connection string.
   */
  public static final String ZOOKEEPER_CONNECTION_STRING =
          CONFIG_PREFIX + "connection.string";

  /**
   * Constant for the property that specifies the ZooKeeper path.
   */
  public static final String ZOOKEEPER_PATH = CONFIG_PREFIX + "path";

  /**
   * Constant for the property that specifies the auth type to use.  Supported
   * values are "none" and "sasl".  The default value is "none".
   */
  public static final String ZOOKEEPER_AUTH_TYPE = CONFIG_PREFIX + "auth.type";

  /**
   * Constant for the property that specifies the Kerberos keytab file.
   */
  public static final String ZOOKEEPER_KERBEROS_KEYTAB =
          CONFIG_PREFIX + "kerberos.keytab";

  /**
   * Constant for the property that specifies the Kerberos principal.
   */
  public static final String ZOOKEEPER_KERBEROS_PRINCIPAL =
          CONFIG_PREFIX + "kerberos.principal";

  /**
   * Constant for the property that specifies whether or not the Curator client
   * should disconnect from ZooKeeper on shutdown.  The default is "true".  Only
   * set this to "false" if a custom Curator client is being provided and the
   * disconnection is being handled elsewhere.
   */
  public static final String DISCONNECT_FROM_ZOOKEEPER_ON_SHUTDOWN =
          CONFIG_PREFIX + "disconnect.on.shutdown";

  /**
   * Constant for the ServletContext attribute that can be used for providing a
   * custom CuratorFramework client. If set ZKSignerSecretProvider will use this
   * Curator client instead of creating a new one. The providing class is
   * responsible for creating and configuring the Curator client (including
   * security and ACLs) in this case.
   */
  public static final String
      ZOOKEEPER_SIGNER_SECRET_PROVIDER_CURATOR_CLIENT_ATTRIBUTE =
      CONFIG_PREFIX + "curator.client";

  private static Logger LOG = LoggerFactory.getLogger(
          ZKSignerSecretProvider.class);
  private String path;
  /**
   * Stores the next secret that will be used after the current one rolls over.
   * We do this to help with rollover performance by actually deciding the next
   * secret at the previous rollover.  This allows us to switch to the next
   * secret very quickly.  Afterwards, we have plenty of time to decide on the
   * next secret.
   */
  private volatile byte[] nextSecret;
  private final Random rand;
  /**
   * Stores the current version of the znode.
   */
  private int zkVersion;
  /**
   * Stores the next date that the rollover will occur.  This is only used
   * for allowing new servers joining later to synchronize their rollover
   * with everyone else.
   */
  private long nextRolloverDate;
  private long tokenValidity;
  private CuratorFramework client;
  private boolean shouldDisconnect;
  private static int INT_BYTES = Integer.SIZE / Byte.SIZE;
  private static int LONG_BYTES = Long.SIZE / Byte.SIZE;
  private static int DATA_VERSION = 0;

  public ZKSignerSecretProvider() {
    super();
    rand = new Random();
  }

  /**
   * This constructor lets you set the seed of the Random Number Generator and
   * is meant for testing.
   * @param seed the seed for the random number generator
   */
  @VisibleForTesting
  public ZKSignerSecretProvider(long seed) {
    super();
    rand = new Random(seed);
  }

  @Override
  public void init(Properties config, ServletContext servletContext,
          long tokenValidity) throws Exception {
    Object curatorClientObj = servletContext.getAttribute(
            ZOOKEEPER_SIGNER_SECRET_PROVIDER_CURATOR_CLIENT_ATTRIBUTE);
    if (curatorClientObj != null
            && curatorClientObj instanceof CuratorFramework) {
      client = (CuratorFramework) curatorClientObj;
    } else {
      client = createCuratorClient(config);
    }
    this.tokenValidity = tokenValidity;
    shouldDisconnect = Boolean.parseBoolean(
            config.getProperty(DISCONNECT_FROM_ZOOKEEPER_ON_SHUTDOWN, "true"));
    path = config.getProperty(ZOOKEEPER_PATH);
    if (path == null) {
      throw new IllegalArgumentException(ZOOKEEPER_PATH
              + " must be specified");
    }
    try {
      nextRolloverDate = System.currentTimeMillis() + tokenValidity;
      // everyone tries to do this, only one will succeed and only when the
      // znode doesn't already exist.  Everyone else will synchronize on the
      // data from the znode
      client.create().creatingParentsIfNeeded()
              .forPath(path, generateZKData(generateRandomSecret(),
              generateRandomSecret(), null));
      zkVersion = 0;
      LOG.info("Creating secret znode");
    } catch (KeeperException.NodeExistsException nee) {
      LOG.info("The secret znode already exists, retrieving data");
    }
    // Synchronize on the data from the znode
    // passing true tells it to parse out all the data for initing
    pullFromZK(true);
    long initialDelay = nextRolloverDate - System.currentTimeMillis();
    // If it's in the past, try to find the next interval that we should
    // be using
    if (initialDelay < 1l) {
      int i = 1;
      while (initialDelay < 1l) {
        initialDelay = nextRolloverDate + tokenValidity * i
                - System.currentTimeMillis();
        i++;
      }
    }
    super.startScheduler(initialDelay, tokenValidity);
  }

  /**
   * Disconnects from ZooKeeper unless told not to.
   */
  @Override
  public void destroy() {
    if (shouldDisconnect && client != null) {
      client.close();
    }
    super.destroy();
  }

  @Override
  protected synchronized void rollSecret() {
    super.rollSecret();
    // Try to push the information to ZooKeeper with a potential next secret.
    nextRolloverDate += tokenValidity;
    byte[][] secrets = super.getAllSecrets();
    pushToZK(generateRandomSecret(), secrets[0], secrets[1]);
    // Pull info from ZooKeeper to get the decided next secret
    // passing false tells it that we don't care about most of the data
    pullFromZK(false);
  }

  @Override
  protected byte[] generateNewSecret() {
    // We simply return nextSecret because it's already been decided on
    return nextSecret;
  }

  /**
   * Pushes proposed data to ZooKeeper.  If a different server pushes its data
   * first, it gives up.
   * @param newSecret The new secret to use
   * @param currentSecret The current secret
   * @param previousSecret  The previous secret
   */
  private synchronized void pushToZK(byte[] newSecret, byte[] currentSecret,
          byte[] previousSecret) {
    byte[] bytes = generateZKData(newSecret, currentSecret, previousSecret);
    try {
      client.setData().withVersion(zkVersion).forPath(path, bytes);
    } catch (KeeperException.BadVersionException bve) {
      LOG.debug("Unable to push to znode; another server already did it");
    } catch (Exception ex) {
      LOG.error("An unexpected exception occured pushing data to ZooKeeper",
              ex);
    }
  }

  /**
   * Serialize the data to attempt to push into ZooKeeper.  The format is this:
   * <p>
   * [DATA_VERSION, newSecretLength, newSecret, currentSecretLength, currentSecret, previousSecretLength, previousSecret, nextRolloverDate]
   * <p>
   * Only previousSecret can be null, in which case the format looks like this:
   * <p>
   * [DATA_VERSION, newSecretLength, newSecret, currentSecretLength, currentSecret, 0, nextRolloverDate]
   * <p>
   * @param newSecret The new secret to use
   * @param currentSecret The current secret
   * @param previousSecret The previous secret
   * @return The serialized data for ZooKeeper
   */
  private synchronized byte[] generateZKData(byte[] newSecret,
          byte[] currentSecret, byte[] previousSecret) {
    int newSecretLength = newSecret.length;
    int currentSecretLength = currentSecret.length;
    int previousSecretLength = 0;
    if (previousSecret != null) {
      previousSecretLength = previousSecret.length;
    }
    ByteBuffer bb = ByteBuffer.allocate(INT_BYTES + INT_BYTES + newSecretLength
        + INT_BYTES + currentSecretLength + INT_BYTES + previousSecretLength
        + LONG_BYTES);
    bb.putInt(DATA_VERSION);
    bb.putInt(newSecretLength);
    bb.put(newSecret);
    bb.putInt(currentSecretLength);
    bb.put(currentSecret);
    bb.putInt(previousSecretLength);
    if (previousSecretLength > 0) {
      bb.put(previousSecret);
    }
    bb.putLong(nextRolloverDate);
    return bb.array();
  }

  /**
   * Pulls data from ZooKeeper.  If isInit is false, it will only parse the
   * next secret and version.  If isInit is true, it will also parse the current
   * and previous secrets, and the next rollover date; it will also init the
   * secrets.  Hence, isInit should only be true on startup.
   * @param isInit  see description above
   */
  private synchronized void pullFromZK(boolean isInit) {
    try {
      Stat stat = new Stat();
      byte[] bytes = client.getData().storingStatIn(stat).forPath(path);
      ByteBuffer bb = ByteBuffer.wrap(bytes);
      int dataVersion = bb.getInt();
      if (dataVersion > DATA_VERSION) {
        throw new IllegalStateException("Cannot load data from ZooKeeper; it"
                + "was written with a newer version");
      }
      int nextSecretLength = bb.getInt();
      byte[] nextSecret = new byte[nextSecretLength];
      bb.get(nextSecret);
      this.nextSecret = nextSecret;
      zkVersion = stat.getVersion();
      if (isInit) {
        int currentSecretLength = bb.getInt();
        byte[] currentSecret = new byte[currentSecretLength];
        bb.get(currentSecret);
        int previousSecretLength = bb.getInt();
        byte[] previousSecret = null;
        if (previousSecretLength > 0) {
          previousSecret = new byte[previousSecretLength];
          bb.get(previousSecret);
        }
        super.initSecrets(currentSecret, previousSecret);
        nextRolloverDate = bb.getLong();
      }
    } catch (Exception ex) {
      LOG.error("An unexpected exception occurred while pulling data from"
              + "ZooKeeper", ex);
    }
  }

  private byte[] generateRandomSecret() {
    return Long.toString(rand.nextLong()).getBytes();
  }

  /**
   * This method creates the Curator client and connects to ZooKeeper.
   * @param config configuration properties
   * @return A Curator client
   * @throws java.lang.Exception
   */
  protected CuratorFramework createCuratorClient(Properties config)
          throws Exception {
    String connectionString = config.getProperty(
            ZOOKEEPER_CONNECTION_STRING, "localhost:2181");

    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    ACLProvider aclProvider;
    String authType = config.getProperty(ZOOKEEPER_AUTH_TYPE, "none");
    if (authType.equals("sasl")) {
      LOG.info("Connecting to ZooKeeper with SASL/Kerberos"
              + "and using 'sasl' ACLs");
      String principal = setJaasConfiguration(config);
      System.setProperty(ZooKeeperSaslClient.LOGIN_CONTEXT_NAME_KEY,
              "ZKSignerSecretProviderClient");
      System.setProperty("zookeeper.authProvider.1",
              "org.apache.zookeeper.server.auth.SASLAuthenticationProvider");
      aclProvider = new SASLOwnerACLProvider(principal);
    } else {  // "none"
      LOG.info("Connecting to ZooKeeper without authentication");
      aclProvider = new DefaultACLProvider();     // open to everyone
    }
    CuratorFramework cf = CuratorFrameworkFactory.builder()
            .connectString(connectionString)
            .retryPolicy(retryPolicy)
            .aclProvider(aclProvider)
            .build();
    cf.start();
    return cf;
  }

  private String setJaasConfiguration(Properties config) throws Exception {
    String keytabFile = config.getProperty(ZOOKEEPER_KERBEROS_KEYTAB).trim();
    if (keytabFile == null || keytabFile.length() == 0) {
      throw new IllegalArgumentException(ZOOKEEPER_KERBEROS_KEYTAB
              + " must be specified");
    }
    String principal = config.getProperty(ZOOKEEPER_KERBEROS_PRINCIPAL)
            .trim();
    if (principal == null || principal.length() == 0) {
      throw new IllegalArgumentException(ZOOKEEPER_KERBEROS_PRINCIPAL
              + " must be specified");
    }

    // This is equivalent to writing a jaas.conf file and setting the system
    // property, "java.security.auth.login.config", to point to it
    JaasConfiguration jConf =
            new JaasConfiguration("Client", principal, keytabFile);
    Configuration.setConfiguration(jConf);
    return principal.split("[/@]")[0];
  }

  /**
   * Simple implementation of an {@link ACLProvider} that simply returns an ACL
   * that gives all permissions only to a single principal.
   */
  private static class SASLOwnerACLProvider implements ACLProvider {

    private final List<ACL> saslACL;

    private SASLOwnerACLProvider(String principal) {
      this.saslACL = Collections.singletonList(
              new ACL(Perms.ALL, new Id("sasl", principal)));
    }

    @Override
    public List<ACL> getDefaultAcl() {
      return saslACL;
    }

    @Override
    public List<ACL> getAclForPath(String path) {
      return saslACL;
    }
  }

  /**
   * Creates a programmatic version of a jaas.conf file. This can be used
   * instead of writing a jaas.conf file and setting the system property,
   * "java.security.auth.login.config", to point to that file. It is meant to be
   * used for connecting to ZooKeeper.
   */
  @InterfaceAudience.Private
  public static class JaasConfiguration extends Configuration {

    private static AppConfigurationEntry[] entry;
    private String entryName;

    /**
     * Add an entry to the jaas configuration with the passed in name,
     * principal, and keytab. The other necessary options will be set for you.
     *
     * @param entryName The name of the entry (e.g. "Client")
     * @param principal The principal of the user
     * @param keytab The location of the keytab
     */
    public JaasConfiguration(String entryName, String principal, String keytab) {
      this.entryName = entryName;
      Map<String, String> options = new HashMap<String, String>();
      options.put("keyTab", keytab);
      options.put("principal", principal);
      options.put("useKeyTab", "true");
      options.put("storeKey", "true");
      options.put("useTicketCache", "false");
      options.put("refreshKrb5Config", "true");
      String jaasEnvVar = System.getenv("HADOOP_JAAS_DEBUG");
      if (jaasEnvVar != null && "true".equalsIgnoreCase(jaasEnvVar)) {
        options.put("debug", "true");
      }
      entry = new AppConfigurationEntry[]{
                  new AppConfigurationEntry(getKrb5LoginModuleName(),
                  AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                  options)};
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
      return (entryName.equals(name)) ? entry : null;
    }

    private String getKrb5LoginModuleName() {
      String krb5LoginModuleName;
      if (System.getProperty("java.vendor").contains("IBM")) {
        krb5LoginModuleName = "com.ibm.security.auth.module.Krb5LoginModule";
      } else {
        krb5LoginModuleName = "com.sun.security.auth.module.Krb5LoginModule";
      }
      return krb5LoginModuleName;
    }
  }
}
diff --git a/hadoop-common-project/hadoop-auth/src/site/apt/Configuration.apt.vm b/hadoop-common-project/hadoop-auth/src/site/apt/Configuration.apt.vm
index 63773933551..88248e52373 100644
-- a/hadoop-common-project/hadoop-auth/src/site/apt/Configuration.apt.vm
++ b/hadoop-common-project/hadoop-auth/src/site/apt/Configuration.apt.vm
@@ -45,14 +45,14 @@ Configuration
   * <<<[PREFIX.]type>>>: the authentication type keyword (<<<simple>>> or
     <<<kerberos>>>) or a Authentication handler implementation.
 
  * <<<[PREFIX.]signature.secret>>>: The secret to SHA-sign the generated
    authentication tokens. If a secret is not provided a random secret is
    generated at start up time. If using multiple web application instances
    behind a load-balancer a secret must be set for the application to work
    properly.
  * <<<[PREFIX.]signature.secret>>>: When <<<signer.secret.provider>>> is set to
    <<<string>>> or not specified, this is the value for the secret used to sign
    the HTTP cookie.
 
   * <<<[PREFIX.]token.validity>>>: The validity -in seconds- of the generated
    authentication token. The default value is <<<3600>>> seconds.
    authentication token. The default value is <<<3600>>> seconds. This is also
    used for the rollover interval when <<<signer.secret.provider>>> is set to
    <<<random>>> or <<<zookeeper>>>.
 
   * <<<[PREFIX.]cookie.domain>>>: domain to use for the HTTP cookie that stores
     the authentication token.
@@ -60,6 +60,12 @@ Configuration
   * <<<[PREFIX.]cookie.path>>>: path to use for the HTTP cookie that stores the
     authentication token.
 
  * <<<signer.secret.provider>>>: indicates the name of the SignerSecretProvider
    class to use. Possible values are: <<<string>>>, <<<random>>>,
    <<<zookeeper>>>, or a classname. If not specified, the <<<string>>>
    implementation will be used; and failing that, the <<<random>>>
    implementation will be used.

 ** Kerberos Configuration
 
   <<IMPORTANT>>: A KDC must be configured and running.
@@ -239,3 +245,133 @@ Configuration
     ...
 </web-app>
 +---+

** SignerSecretProvider Configuration

  The SignerSecretProvider is used to provide more advanced behaviors for the
  secret used for signing the HTTP Cookies.

  These are the relevant configuration properties:

    * <<<signer.secret.provider>>>: indicates the name of the
      SignerSecretProvider class to use. Possible values are: "string",
      "random", "zookeeper", or a classname. If not specified, the "string"
      implementation will be used; and failing that, the "random" implementation
      will be used.

    * <<<[PREFIX.]signature.secret>>>: When <<<signer.secret.provider>>> is set
      to <<<string>>> or not specified, this is the value for the secret used to
      sign the HTTP cookie.

    * <<<[PREFIX.]token.validity>>>: The validity -in seconds- of the generated
      authentication token. The default value is <<<3600>>> seconds. This is
      also used for the rollover interval when <<<signer.secret.provider>>> is
      set to <<<random>>> or <<<zookeeper>>>.

  The following configuration properties are specific to the <<<zookeeper>>>
  implementation:

    * <<<signer.secret.provider.zookeeper.connection.string>>>: Indicates the
      ZooKeeper connection string to connect with.

    * <<<signer.secret.provider.zookeeper.path>>>: Indicates the ZooKeeper path
      to use for storing and retrieving the secrets.  All servers
      that need to coordinate their secret should point to the same path

    * <<<signer.secret.provider.zookeeper.auth.type>>>: Indicates the auth type
      to use.  Supported values are <<<none>>> and <<<sasl>>>.  The default
      value is <<<none>>>.

    * <<<signer.secret.provider.zookeeper.kerberos.keytab>>>: Set this to the
      path with the Kerberos keytab file.  This is only required if using
      Kerberos.

    * <<<signer.secret.provider.zookeeper.kerberos.principal>>>: Set this to the
      Kerberos principal to use.  This only required if using Kerberos.

  <<Example>>:

+---+
<web-app version="2.5" xmlns="http://java.sun.com/xml/ns/javaee">
    ...

    <filter>
        <!-- AuthenticationHandler configs not shown -->
        <init-param>
            <param-name>signer.secret.provider</param-name>
            <param-value>string</param-value>
        </init-param>
        <init-param>
            <param-name>signature.secret</param-name>
            <param-value>my_secret</param-value>
        </init-param>
    </filter>

    ...
</web-app>
+---+

  <<Example>>:

+---+
<web-app version="2.5" xmlns="http://java.sun.com/xml/ns/javaee">
    ...

    <filter>
        <!-- AuthenticationHandler configs not shown -->
        <init-param>
            <param-name>signer.secret.provider</param-name>
            <param-value>random</param-value>
        </init-param>
        <init-param>
            <param-name>token.validity</param-name>
            <param-value>30</param-value>
        </init-param>
    </filter>

    ...
</web-app>
+---+

  <<Example>>:

+---+
<web-app version="2.5" xmlns="http://java.sun.com/xml/ns/javaee">
    ...

    <filter>
        <!-- AuthenticationHandler configs not shown -->
        <init-param>
            <param-name>signer.secret.provider</param-name>
            <param-value>zookeeper</param-value>
        </init-param>
        <init-param>
            <param-name>token.validity</param-name>
            <param-value>30</param-value>
        </init-param>
        <init-param>
            <param-name>signer.secret.provider.zookeeper.connection.string</param-name>
            <param-value>zoo1:2181,zoo2:2181,zoo3:2181</param-value>
        </init-param>
        <init-param>
            <param-name>signer.secret.provider.zookeeper.path</param-name>
            <param-value>/myapp/secrets</param-value>
        </init-param>
        <init-param>
            <param-name>signer.secret.provider.zookeeper.use.kerberos.acls</param-name>
            <param-value>true</param-value>
        </init-param>
        <init-param>
            <param-name>signer.secret.provider.zookeeper.kerberos.keytab</param-name>
            <param-value>/tmp/auth.keytab</param-value>
        </init-param>
        <init-param>
            <param-name>signer.secret.provider.zookeeper.kerberos.principal</param-name>
            <param-value>HTTP/localhost@LOCALHOST</param-value>
        </init-param>
    </filter>

    ...
</web-app>
+---+

diff --git a/hadoop-common-project/hadoop-auth/src/site/apt/index.apt.vm b/hadoop-common-project/hadoop-auth/src/site/apt/index.apt.vm
index 6051f8cbf2a..bf85f7f41ba 100644
-- a/hadoop-common-project/hadoop-auth/src/site/apt/index.apt.vm
++ b/hadoop-common-project/hadoop-auth/src/site/apt/index.apt.vm
@@ -44,6 +44,11 @@ Hadoop Auth, Java HTTP SPNEGO ${project.version}
   Subsequent HTTP client requests presenting the signed HTTP Cookie have access
   to the protected resources until the HTTP Cookie expires.
 
  The secret used to sign the HTTP Cookie has multiple implementations that
  provide different behaviors, including a hardcoded secret string, a rolling
  randomly generated secret, and a rolling randomly generated secret
  synchronized between multiple servers using ZooKeeper.

 * User Documentation
 
   * {{{./Examples.html}Examples}}
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
index a9a5e8c738f..5d93fcfa1c4 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
@@ -162,7 +162,8 @@ public void testInit() throws Exception {
                                  AuthenticationFilter.AUTH_TOKEN_VALIDITY)).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
       Assert.assertEquals(PseudoAuthenticationHandler.class, filter.getAuthenticationHandler().getClass());
@@ -186,7 +187,8 @@ public void testInit() throws Exception {
                                  AuthenticationFilter.SIGNATURE_SECRET)).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
       Assert.assertFalse(filter.isRandomSecret());
@@ -206,10 +208,11 @@ public void testInit() throws Exception {
                                  AuthenticationFilter.SIGNATURE_SECRET)).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(
          AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE)).thenReturn(
             new SignerSecretProvider() {
               @Override
              public void init(Properties config, long tokenValidity) {
              public void init(Properties config, ServletContext servletContext,
                      long tokenValidity) {
               }
               @Override
               public byte[] getCurrentSecret() {
@@ -241,7 +244,8 @@ public void init(Properties config, long tokenValidity) {
                                  AuthenticationFilter.COOKIE_PATH)).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
       Assert.assertEquals(".foo.com", filter.getCookieDomain());
@@ -265,7 +269,8 @@ public void init(Properties config, long tokenValidity) {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
       Assert.assertTrue(DummyAuthenticationHandler.init);
@@ -304,7 +309,8 @@ public void testInitCaseSensitivity() throws Exception {
               AuthenticationFilter.AUTH_TOKEN_VALIDITY)).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
 
       filter.init(config);
@@ -330,7 +336,8 @@ public void testGetRequestURL() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
@@ -361,13 +368,20 @@ public void testGetToken() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
       AuthenticationToken token = new AuthenticationToken("u", "p", DummyAuthenticationHandler.TYPE);
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(new StringSignerSecretProvider("secret"));
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      Properties secretProviderProps = new Properties();
      secretProviderProps.setProperty(
              AuthenticationFilter.SIGNATURE_SECRET, "secret");
      secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(secretProvider);
       String tokenSigned = signer.sign(token.toString());
 
       Cookie cookie = new Cookie(AuthenticatedURL.AUTH_COOKIE, tokenSigned);
@@ -398,14 +412,21 @@ public void testGetTokenExpired() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
       AuthenticationToken token =
           new AuthenticationToken("u", "p", DummyAuthenticationHandler.TYPE);
       token.setExpires(System.currentTimeMillis() - TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(new StringSignerSecretProvider("secret"));
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      Properties secretProviderProps = new Properties();
      secretProviderProps.setProperty(
              AuthenticationFilter.SIGNATURE_SECRET, "secret");
      secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(secretProvider);
       String tokenSigned = signer.sign(token.toString());
 
       Cookie cookie = new Cookie(AuthenticatedURL.AUTH_COOKIE, tokenSigned);
@@ -443,13 +464,20 @@ public void testGetTokenInvalidType() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
       AuthenticationToken token = new AuthenticationToken("u", "p", "invalidtype");
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(new StringSignerSecretProvider("secret"));
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      Properties secretProviderProps = new Properties();
      secretProviderProps.setProperty(
              AuthenticationFilter.SIGNATURE_SECRET, "secret");
      secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(secretProvider);
       String tokenSigned = signer.sign(token.toString());
 
       Cookie cookie = new Cookie(AuthenticatedURL.AUTH_COOKIE, tokenSigned);
@@ -485,7 +513,8 @@ public void testDoFilterNotAuthenticated() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
@@ -538,7 +567,8 @@ private void _testDoFilterAuthentication(boolean withDomainPath,
             ".return", "expired.token")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
 
     if (withDomainPath) {
@@ -593,7 +623,13 @@ public Object answer(InvocationOnMock invocation) throws Throwable {
         Mockito.verify(chain).doFilter(Mockito.any(ServletRequest.class),
                 Mockito.any(ServletResponse.class));
 
        Signer signer = new Signer(new StringSignerSecretProvider("secret"));
        StringSignerSecretProvider secretProvider
                = new StringSignerSecretProvider();
        Properties secretProviderProps = new Properties();
        secretProviderProps.setProperty(
                AuthenticationFilter.SIGNATURE_SECRET, "secret");
        secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);
        Signer signer = new Signer(secretProvider);
         String value = signer.verifyAndExtract(v);
         AuthenticationToken token = AuthenticationToken.parse(value);
         assertThat(token.getExpires(), not(0L));
@@ -662,7 +698,8 @@ public void testDoFilterAuthenticated() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
@@ -671,7 +708,13 @@ public void testDoFilterAuthenticated() throws Exception {
 
       AuthenticationToken token = new AuthenticationToken("u", "p", "t");
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(new StringSignerSecretProvider("secret"));
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      Properties secretProviderProps = new Properties();
      secretProviderProps.setProperty(
              AuthenticationFilter.SIGNATURE_SECRET, "secret");
      secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(secretProvider);
       String tokenSigned = signer.sign(token.toString());
 
       Cookie cookie = new Cookie(AuthenticatedURL.AUTH_COOKIE, tokenSigned);
@@ -716,7 +759,8 @@ public void testDoFilterAuthenticationFailure() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
@@ -783,7 +827,8 @@ public void testDoFilterAuthenticatedExpired() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
@@ -792,7 +837,13 @@ public void testDoFilterAuthenticatedExpired() throws Exception {
 
       AuthenticationToken token = new AuthenticationToken("u", "p", DummyAuthenticationHandler.TYPE);
       token.setExpires(System.currentTimeMillis() - TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(new StringSignerSecretProvider(secret));
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      Properties secretProviderProps = new Properties();
      secretProviderProps.setProperty(
              AuthenticationFilter.SIGNATURE_SECRET, secret);
      secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(secretProvider);
       String tokenSigned = signer.sign(token.toString());
 
       Cookie cookie = new Cookie(AuthenticatedURL.AUTH_COOKIE, tokenSigned);
@@ -854,7 +905,8 @@ public void testDoFilterAuthenticatedInvalidType() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
@@ -863,7 +915,13 @@ public void testDoFilterAuthenticatedInvalidType() throws Exception {
 
       AuthenticationToken token = new AuthenticationToken("u", "p", "invalidtype");
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(new StringSignerSecretProvider(secret));
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      Properties secretProviderProps = new Properties();
      secretProviderProps.setProperty(
              AuthenticationFilter.SIGNATURE_SECRET, secret);
      secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(secretProvider);
       String tokenSigned = signer.sign(token.toString());
 
       Cookie cookie = new Cookie(AuthenticatedURL.AUTH_COOKIE, tokenSigned);
@@ -893,7 +951,8 @@ public void testManagementOperation() throws Exception {
                         "management.operation.return")).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
       Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNATURE_PROVIDER_ATTRIBUTE)).thenReturn(null);
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
 
@@ -914,7 +973,13 @@ public void testManagementOperation() throws Exception {
 
       AuthenticationToken token = new AuthenticationToken("u", "p", "t");
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(new StringSignerSecretProvider("secret"));
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      Properties secretProviderProps = new Properties();
      secretProviderProps.setProperty(
              AuthenticationFilter.SIGNATURE_SECRET, "secret");
      secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);
      Signer signer = new Signer(secretProvider);
       String tokenSigned = signer.sign(token.toString());
       Cookie cookie = new Cookie(AuthenticatedURL.AUTH_COOKIE, tokenSigned);
       Mockito.when(request.getCookies()).thenReturn(new Cookie[]{cookie});
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestJaasConfiguration.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestJaasConfiguration.java
new file mode 100644
index 00000000000..2b70135800b
-- /dev/null
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestJaasConfiguration.java
@@ -0,0 +1,55 @@
/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License. See accompanying LICENSE file.
 */
package org.apache.hadoop.security.authentication.util;

import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import org.junit.Assert;
import org.junit.Test;

public class TestJaasConfiguration {

  // We won't test actually using it to authenticate because that gets messy and
  // may conflict with other tests; but we can test that it otherwise behaves
  // correctly
  @Test
  public void test() throws Exception {
    String krb5LoginModuleName;
    if (System.getProperty("java.vendor").contains("IBM")) {
      krb5LoginModuleName = "com.ibm.security.auth.module.Krb5LoginModule";
    } else {
      krb5LoginModuleName = "com.sun.security.auth.module.Krb5LoginModule";
    }

    ZKSignerSecretProvider.JaasConfiguration jConf =
            new ZKSignerSecretProvider.JaasConfiguration("foo", "foo/localhost",
            "/some/location/foo.keytab");
    AppConfigurationEntry[] entries = jConf.getAppConfigurationEntry("bar");
    Assert.assertNull(entries);
    entries = jConf.getAppConfigurationEntry("foo");
    Assert.assertEquals(1, entries.length);
    AppConfigurationEntry entry = entries[0];
    Assert.assertEquals(AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
            entry.getControlFlag());
    Assert.assertEquals(krb5LoginModuleName, entry.getLoginModuleName());
    Map<String, ?> options = entry.getOptions();
    Assert.assertEquals("/some/location/foo.keytab", options.get("keyTab"));
    Assert.assertEquals("foo/localhost", options.get("principal"));
    Assert.assertEquals("true", options.get("useKeyTab"));
    Assert.assertEquals("true", options.get("storeKey"));
    Assert.assertEquals("false", options.get("useTicketCache"));
    Assert.assertEquals("true", options.get("refreshKrb5Config"));
    Assert.assertEquals(6, options.size());
  }
}
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestRandomSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestRandomSignerSecretProvider.java
index c3384ad03bf..41d4967eace 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestRandomSignerSecretProvider.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestRandomSignerSecretProvider.java
@@ -31,7 +31,7 @@ public void testGetAndRollSecrets() throws Exception {
     RandomSignerSecretProvider secretProvider =
         new RandomSignerSecretProvider(seed);
     try {
      secretProvider.init(null, rolloverFrequency);
      secretProvider.init(null, null, rolloverFrequency);
 
       byte[] currentSecret = secretProvider.getCurrentSecret();
       byte[][] allSecrets = secretProvider.getAllSecrets();
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestRolloverSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestRolloverSignerSecretProvider.java
index 2a2986af9c1..1e40c423262 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestRolloverSignerSecretProvider.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestRolloverSignerSecretProvider.java
@@ -28,7 +28,7 @@ public void testGetAndRollSecrets() throws Exception {
         new TRolloverSignerSecretProvider(
             new byte[][]{secret1, secret2, secret3});
     try {
      secretProvider.init(null, rolloverFrequency);
      secretProvider.init(null, null, rolloverFrequency);
 
       byte[] currentSecret = secretProvider.getCurrentSecret();
       byte[][] allSecrets = secretProvider.getAllSecrets();
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestSigner.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestSigner.java
index 1e2c960a925..c6a77105715 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestSigner.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestSigner.java
@@ -14,6 +14,8 @@
 package org.apache.hadoop.security.authentication.util;
 
 import java.util.Properties;
import javax.servlet.ServletContext;
import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
 import org.junit.Assert;
 import org.junit.Test;
 
@@ -21,7 +23,7 @@
 
   @Test
   public void testNullAndEmptyString() throws Exception {
    Signer signer = new Signer(new StringSignerSecretProvider("secret"));
    Signer signer = new Signer(createStringSignerSecretProvider());
     try {
       signer.sign(null);
       Assert.fail();
@@ -42,7 +44,7 @@ public void testNullAndEmptyString() throws Exception {
 
   @Test
   public void testSignature() throws Exception {
    Signer signer = new Signer(new StringSignerSecretProvider("secret"));
    Signer signer = new Signer(createStringSignerSecretProvider());
     String s1 = signer.sign("ok");
     String s2 = signer.sign("ok");
     String s3 = signer.sign("wrong");
@@ -52,7 +54,7 @@ public void testSignature() throws Exception {
 
   @Test
   public void testVerify() throws Exception {
    Signer signer = new Signer(new StringSignerSecretProvider("secret"));
    Signer signer = new Signer(createStringSignerSecretProvider());
     String t = "test";
     String s = signer.sign(t);
     String e = signer.verifyAndExtract(s);
@@ -61,7 +63,7 @@ public void testVerify() throws Exception {
 
   @Test
   public void testInvalidSignedText() throws Exception {
    Signer signer = new Signer(new StringSignerSecretProvider("secret"));
    Signer signer = new Signer(createStringSignerSecretProvider());
     try {
       signer.verifyAndExtract("test");
       Assert.fail();
@@ -74,7 +76,7 @@ public void testInvalidSignedText() throws Exception {
 
   @Test
   public void testTampering() throws Exception {
    Signer signer = new Signer(new StringSignerSecretProvider("secret"));
    Signer signer = new Signer(createStringSignerSecretProvider());
     String t = "test";
     String s = signer.sign(t);
     s += "x";
@@ -88,6 +90,14 @@ public void testTampering() throws Exception {
     }
   }
 
  private StringSignerSecretProvider createStringSignerSecretProvider() throws Exception {
      StringSignerSecretProvider secretProvider = new StringSignerSecretProvider();
      Properties secretProviderProps = new Properties();
      secretProviderProps.setProperty(AuthenticationFilter.SIGNATURE_SECRET, "secret");
      secretProvider.init(secretProviderProps, null, -1);
      return secretProvider;
  }

   @Test
   public void testMultipleSecrets() throws Exception {
     TestSignerSecretProvider secretProvider = new TestSignerSecretProvider();
@@ -128,7 +138,8 @@ public void testMultipleSecrets() throws Exception {
     private byte[] previousSecret;
 
     @Override
    public void init(Properties config, long tokenValidity) {
    public void init(Properties config, ServletContext servletContext,
            long tokenValidity) {
     }
 
     @Override
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestStringSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestStringSignerSecretProvider.java
index c1170060baf..d8b044dcd27 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestStringSignerSecretProvider.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestStringSignerSecretProvider.java
@@ -13,6 +13,8 @@
  */
 package org.apache.hadoop.security.authentication.util;
 
import java.util.Properties;
import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
 import org.junit.Assert;
 import org.junit.Test;
 
@@ -22,8 +24,11 @@
   public void testGetSecrets() throws Exception {
     String secretStr = "secret";
     StringSignerSecretProvider secretProvider
        = new StringSignerSecretProvider(secretStr);
    secretProvider.init(null, -1);
            = new StringSignerSecretProvider();
    Properties secretProviderProps = new Properties();
    secretProviderProps.setProperty(
            AuthenticationFilter.SIGNATURE_SECRET, "secret");
    secretProvider.init(secretProviderProps, null, -1);
     byte[] secretBytes = secretStr.getBytes();
     Assert.assertArrayEquals(secretBytes, secretProvider.getCurrentSecret());
     byte[][] allSecrets = secretProvider.getAllSecrets();
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestZKSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestZKSignerSecretProvider.java
new file mode 100644
index 00000000000..d7b6e17e117
-- /dev/null
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestZKSignerSecretProvider.java
@@ -0,0 +1,270 @@
/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. See accompanying LICENSE file.
 */
package org.apache.hadoop.security.authentication.util;

import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import javax.servlet.ServletContext;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestZKSignerSecretProvider {

  private TestingServer zkServer;

  @Before
  public void setup() throws Exception {
    zkServer = new TestingServer();
  }

  @After
  public void teardown() throws Exception {
    if (zkServer != null) {
      zkServer.stop();
      zkServer.close();
    }
  }

  @Test
  // Test just one ZKSignerSecretProvider to verify that it works in the
  // simplest case
  public void testOne() throws Exception {
    long rolloverFrequency = 15 * 1000; // rollover every 15 sec
    // use the same seed so we can predict the RNG
    long seed = System.currentTimeMillis();
    Random rand = new Random(seed);
    byte[] secret2 = Long.toString(rand.nextLong()).getBytes();
    byte[] secret1 = Long.toString(rand.nextLong()).getBytes();
    byte[] secret3 = Long.toString(rand.nextLong()).getBytes();
    ZKSignerSecretProvider secretProvider = new ZKSignerSecretProvider(seed);
    Properties config = new Properties();
    config.setProperty(
        ZKSignerSecretProvider.ZOOKEEPER_CONNECTION_STRING,
        zkServer.getConnectString());
    config.setProperty(ZKSignerSecretProvider.ZOOKEEPER_PATH,
        "/secret");
    try {
      secretProvider.init(config, getDummyServletContext(), rolloverFrequency);

      byte[] currentSecret = secretProvider.getCurrentSecret();
      byte[][] allSecrets = secretProvider.getAllSecrets();
      Assert.assertArrayEquals(secret1, currentSecret);
      Assert.assertEquals(2, allSecrets.length);
      Assert.assertArrayEquals(secret1, allSecrets[0]);
      Assert.assertNull(allSecrets[1]);
      Thread.sleep((rolloverFrequency + 2000));

      currentSecret = secretProvider.getCurrentSecret();
      allSecrets = secretProvider.getAllSecrets();
      Assert.assertArrayEquals(secret2, currentSecret);
      Assert.assertEquals(2, allSecrets.length);
      Assert.assertArrayEquals(secret2, allSecrets[0]);
      Assert.assertArrayEquals(secret1, allSecrets[1]);
      Thread.sleep((rolloverFrequency + 2000));

      currentSecret = secretProvider.getCurrentSecret();
      allSecrets = secretProvider.getAllSecrets();
      Assert.assertArrayEquals(secret3, currentSecret);
      Assert.assertEquals(2, allSecrets.length);
      Assert.assertArrayEquals(secret3, allSecrets[0]);
      Assert.assertArrayEquals(secret2, allSecrets[1]);
      Thread.sleep((rolloverFrequency + 2000));
    } finally {
      secretProvider.destroy();
    }
  }

  @Test
  public void testMultipleInit() throws Exception {
    long rolloverFrequency = 15 * 1000; // rollover every 15 sec
    // use the same seed so we can predict the RNG
    long seedA = System.currentTimeMillis();
    Random rand = new Random(seedA);
    byte[] secretA2 = Long.toString(rand.nextLong()).getBytes();
    byte[] secretA1 = Long.toString(rand.nextLong()).getBytes();
    // use the same seed so we can predict the RNG
    long seedB = System.currentTimeMillis() + rand.nextLong();
    rand = new Random(seedB);
    byte[] secretB2 = Long.toString(rand.nextLong()).getBytes();
    byte[] secretB1 = Long.toString(rand.nextLong()).getBytes();
    // use the same seed so we can predict the RNG
    long seedC = System.currentTimeMillis() + rand.nextLong();
    rand = new Random(seedC);
    byte[] secretC2 = Long.toString(rand.nextLong()).getBytes();
    byte[] secretC1 = Long.toString(rand.nextLong()).getBytes();
    ZKSignerSecretProvider secretProviderA = new ZKSignerSecretProvider(seedA);
    ZKSignerSecretProvider secretProviderB = new ZKSignerSecretProvider(seedB);
    ZKSignerSecretProvider secretProviderC = new ZKSignerSecretProvider(seedC);
    Properties config = new Properties();
    config.setProperty(
        ZKSignerSecretProvider.ZOOKEEPER_CONNECTION_STRING,
        zkServer.getConnectString());
    config.setProperty(ZKSignerSecretProvider.ZOOKEEPER_PATH,
        "/secret");
    try {
      secretProviderA.init(config, getDummyServletContext(), rolloverFrequency);
      secretProviderB.init(config, getDummyServletContext(), rolloverFrequency);
      secretProviderC.init(config, getDummyServletContext(), rolloverFrequency);

      byte[] currentSecretA = secretProviderA.getCurrentSecret();
      byte[][] allSecretsA = secretProviderA.getAllSecrets();
      byte[] currentSecretB = secretProviderB.getCurrentSecret();
      byte[][] allSecretsB = secretProviderB.getAllSecrets();
      byte[] currentSecretC = secretProviderC.getCurrentSecret();
      byte[][] allSecretsC = secretProviderC.getAllSecrets();
      Assert.assertArrayEquals(currentSecretA, currentSecretB);
      Assert.assertArrayEquals(currentSecretB, currentSecretC);
      Assert.assertEquals(2, allSecretsA.length);
      Assert.assertEquals(2, allSecretsB.length);
      Assert.assertEquals(2, allSecretsC.length);
      Assert.assertArrayEquals(allSecretsA[0], allSecretsB[0]);
      Assert.assertArrayEquals(allSecretsB[0], allSecretsC[0]);
      Assert.assertNull(allSecretsA[1]);
      Assert.assertNull(allSecretsB[1]);
      Assert.assertNull(allSecretsC[1]);
      char secretChosen = 'z';
      if (Arrays.equals(secretA1, currentSecretA)) {
        Assert.assertArrayEquals(secretA1, allSecretsA[0]);
        secretChosen = 'A';
      } else if (Arrays.equals(secretB1, currentSecretB)) {
        Assert.assertArrayEquals(secretB1, allSecretsA[0]);
        secretChosen = 'B';
      }else if (Arrays.equals(secretC1, currentSecretC)) {
        Assert.assertArrayEquals(secretC1, allSecretsA[0]);
        secretChosen = 'C';
      } else {
        Assert.fail("It appears that they all agreed on the same secret, but "
                + "not one of the secrets they were supposed to");
      }
      Thread.sleep((rolloverFrequency + 2000));

      currentSecretA = secretProviderA.getCurrentSecret();
      allSecretsA = secretProviderA.getAllSecrets();
      currentSecretB = secretProviderB.getCurrentSecret();
      allSecretsB = secretProviderB.getAllSecrets();
      currentSecretC = secretProviderC.getCurrentSecret();
      allSecretsC = secretProviderC.getAllSecrets();
      Assert.assertArrayEquals(currentSecretA, currentSecretB);
      Assert.assertArrayEquals(currentSecretB, currentSecretC);
      Assert.assertEquals(2, allSecretsA.length);
      Assert.assertEquals(2, allSecretsB.length);
      Assert.assertEquals(2, allSecretsC.length);
      Assert.assertArrayEquals(allSecretsA[0], allSecretsB[0]);
      Assert.assertArrayEquals(allSecretsB[0], allSecretsC[0]);
      Assert.assertArrayEquals(allSecretsA[1], allSecretsB[1]);
      Assert.assertArrayEquals(allSecretsB[1], allSecretsC[1]);
      // The second secret used is prechosen by whoever won the init; so it
      // should match with whichever we saw before
      if (secretChosen == 'A') {
        Assert.assertArrayEquals(secretA2, currentSecretA);
      } else if (secretChosen == 'B') {
        Assert.assertArrayEquals(secretB2, currentSecretA);
      } else if (secretChosen == 'C') {
        Assert.assertArrayEquals(secretC2, currentSecretA);
      }
    } finally {
      secretProviderC.destroy();
      secretProviderB.destroy();
      secretProviderA.destroy();
    }
  }

  @Test
  public void testMultipleUnsychnronized() throws Exception {
    long rolloverFrequency = 15 * 1000; // rollover every 15 sec
    // use the same seed so we can predict the RNG
    long seedA = System.currentTimeMillis();
    Random rand = new Random(seedA);
    byte[] secretA2 = Long.toString(rand.nextLong()).getBytes();
    byte[] secretA1 = Long.toString(rand.nextLong()).getBytes();
    byte[] secretA3 = Long.toString(rand.nextLong()).getBytes();
    // use the same seed so we can predict the RNG
    long seedB = System.currentTimeMillis() + rand.nextLong();
    rand = new Random(seedB);
    byte[] secretB2 = Long.toString(rand.nextLong()).getBytes();
    byte[] secretB1 = Long.toString(rand.nextLong()).getBytes();
    byte[] secretB3 = Long.toString(rand.nextLong()).getBytes();
    ZKSignerSecretProvider secretProviderA = new ZKSignerSecretProvider(seedA);
    ZKSignerSecretProvider secretProviderB = new ZKSignerSecretProvider(seedB);
    Properties config = new Properties();
    config.setProperty(
        ZKSignerSecretProvider.ZOOKEEPER_CONNECTION_STRING,
        zkServer.getConnectString());
    config.setProperty(ZKSignerSecretProvider.ZOOKEEPER_PATH,
        "/secret");
    try {
      secretProviderA.init(config, getDummyServletContext(), rolloverFrequency);

      byte[] currentSecretA = secretProviderA.getCurrentSecret();
      byte[][] allSecretsA = secretProviderA.getAllSecrets();
      Assert.assertArrayEquals(secretA1, currentSecretA);
      Assert.assertEquals(2, allSecretsA.length);
      Assert.assertArrayEquals(secretA1, allSecretsA[0]);
      Assert.assertNull(allSecretsA[1]);
      Thread.sleep((rolloverFrequency + 2000));

      currentSecretA = secretProviderA.getCurrentSecret();
      allSecretsA = secretProviderA.getAllSecrets();
      Assert.assertArrayEquals(secretA2, currentSecretA);
      Assert.assertEquals(2, allSecretsA.length);
      Assert.assertArrayEquals(secretA2, allSecretsA[0]);
      Assert.assertArrayEquals(secretA1, allSecretsA[1]);
      Thread.sleep((rolloverFrequency / 5));

      secretProviderB.init(config, getDummyServletContext(), rolloverFrequency);

      byte[] currentSecretB = secretProviderB.getCurrentSecret();
      byte[][] allSecretsB = secretProviderB.getAllSecrets();
      Assert.assertArrayEquals(secretA2, currentSecretB);
      Assert.assertEquals(2, allSecretsA.length);
      Assert.assertArrayEquals(secretA2, allSecretsB[0]);
      Assert.assertArrayEquals(secretA1, allSecretsB[1]);
      Thread.sleep((rolloverFrequency));

      currentSecretA = secretProviderA.getCurrentSecret();
      allSecretsA = secretProviderA.getAllSecrets();
      currentSecretB = secretProviderB.getCurrentSecret();
      allSecretsB = secretProviderB.getAllSecrets();
      Assert.assertArrayEquals(currentSecretA, currentSecretB);
      Assert.assertEquals(2, allSecretsA.length);
      Assert.assertEquals(2, allSecretsB.length);
      Assert.assertArrayEquals(allSecretsA[0], allSecretsB[0]);
      Assert.assertArrayEquals(allSecretsA[1], allSecretsB[1]);
      if (Arrays.equals(secretA3, currentSecretA)) {
        Assert.assertArrayEquals(secretA3, allSecretsA[0]);
      } else if (Arrays.equals(secretB3, currentSecretB)) {
        Assert.assertArrayEquals(secretB3, allSecretsA[0]);
      } else {
        Assert.fail("It appears that they all agreed on the same secret, but "
                + "not one of the secrets they were supposed to");
      }
    } finally {
      secretProviderB.destroy();
      secretProviderA.destroy();
    }
  }

  private ServletContext getDummyServletContext() {
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    Mockito.when(servletContext.getAttribute(ZKSignerSecretProvider
            .ZOOKEEPER_SIGNER_SECRET_PROVIDER_CURATOR_CLIENT_ATTRIBUTE))
            .thenReturn(null);
    return servletContext;
  }
}
diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 89bce4dd920..2d906f7f2bf 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -520,6 +520,9 @@ Release 2.6.0 - UNRELEASED
     HADOOP-11091. Eliminate old configuration parameter names from s3a (David
     S. Wang via Colin Patrick McCabe)
 
    HADOOP-10868. AuthenticationFilter should support externalizing the 
    secret for signing and provide rotation support. (rkanter via tucu)

   OPTIMIZATIONS
 
     HADOOP-10838. Byte array native checksumming. (James Thomas via todd)
diff --git a/hadoop-hdfs-project/hadoop-hdfs-httpfs/src/test/java/org/apache/hadoop/fs/http/server/TestHttpFSServer.java b/hadoop-hdfs-project/hadoop-hdfs-httpfs/src/test/java/org/apache/hadoop/fs/http/server/TestHttpFSServer.java
index c6c0d19d2ad..763d168d198 100644
-- a/hadoop-hdfs-project/hadoop-hdfs-httpfs/src/test/java/org/apache/hadoop/fs/http/server/TestHttpFSServer.java
++ b/hadoop-hdfs-project/hadoop-hdfs-httpfs/src/test/java/org/apache/hadoop/fs/http/server/TestHttpFSServer.java
@@ -66,6 +66,8 @@
 import org.mortbay.jetty.webapp.WebAppContext;
 
 import com.google.common.collect.Maps;
import java.util.Properties;
import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
 import org.apache.hadoop.security.authentication.util.StringSignerSecretProvider;
 
 public class TestHttpFSServer extends HFSTestCase {
@@ -685,7 +687,11 @@ public void testDelegationTokenOperations() throws Exception {
       new AuthenticationToken("u", "p",
           new KerberosDelegationTokenAuthenticationHandler().getType());
     token.setExpires(System.currentTimeMillis() + 100000000);
    Signer signer = new Signer(new StringSignerSecretProvider("secret"));
    StringSignerSecretProvider secretProvider = new StringSignerSecretProvider();
    Properties secretProviderProps = new Properties();
    secretProviderProps.setProperty(AuthenticationFilter.SIGNATURE_SECRET, "secret");
    secretProvider.init(secretProviderProps, null, -1);
    Signer signer = new Signer(secretProvider);
     String tokenSigned = signer.sign(token.toString());
 
     url = new URL(TestJettyHelper.getJettyURL(),
diff --git a/hadoop-project/pom.xml b/hadoop-project/pom.xml
index 502655f7096..0f662a2049c 100644
-- a/hadoop-project/pom.xml
++ b/hadoop-project/pom.xml
@@ -849,6 +849,17 @@
        <artifactId>xercesImpl</artifactId>
        <version>2.9.1</version>
      </dependency>

     <dependency>
       <groupId>org.apache.curator</groupId>
       <artifactId>curator-framework</artifactId>
       <version>2.6.0</version>
     </dependency>
     <dependency>
       <groupId>org.apache.curator</groupId>
       <artifactId>curator-test</artifactId>
       <version>2.6.0</version>
     </dependency>
       
     </dependencies>
   </dependencyManagement>
- 
2.19.1.windows.1

