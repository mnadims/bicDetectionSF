From fd2f22adec5c2f21f792045dbfde9385c21403ec Mon Sep 17 00:00:00 2001
From: Yongjun Zhang <yzhang@cloudera.com>
Date: Thu, 10 Nov 2016 22:21:54 -0800
Subject: [PATCH] HADOOP-13720. Add more info to the msgs printed in
 AbstractDelegationTokenSecretManager. Contributed by Yongjun Zhang.

--
 .../AbstractDelegationTokenSecretManager.java | 69 ++++++++++++-------
 .../java/org/apache/hadoop/util/Time.java     | 18 +++++
 .../apache/hadoop/io/file/tfile/Timer.java    | 52 +++++++-------
 .../java/org/apache/hadoop/util/TestTime.java | 50 ++++++++++++++
 4 files changed, 136 insertions(+), 53 deletions(-)
 create mode 100644 hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestTime.java

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/AbstractDelegationTokenSecretManager.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/AbstractDelegationTokenSecretManager.java
index cc2efc907f5..0e311ddeb2a 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/AbstractDelegationTokenSecretManager.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/AbstractDelegationTokenSecretManager.java
@@ -53,6 +53,10 @@
   private static final Log LOG = LogFactory
       .getLog(AbstractDelegationTokenSecretManager.class);
 
  private String formatTokenId(TokenIdent id) {
    return "(" + id + ")";
  }

   /** 
    * Cache of currently valid tokens, mapping from DelegationTokenIdentifier 
    * to DelegationTokenInformation. Protected by this object lock.
@@ -312,7 +316,8 @@ public synchronized void addPersistedDelegationToken(
     int keyId = identifier.getMasterKeyId();
     DelegationKey dKey = allKeys.get(keyId);
     if (dKey == null) {
      LOG.warn("No KEY found for persisted identifier " + identifier.toString());
      LOG.warn("No KEY found for persisted identifier "
          + formatTokenId(identifier));
       return;
     }
     byte[] password = createPassword(identifier.getBytes(), dKey.getKey());
@@ -323,7 +328,8 @@ public synchronized void addPersistedDelegationToken(
       currentTokens.put(identifier, new DelegationTokenInformation(renewDate,
           password, getTrackingIdIfEnabled(identifier)));
     } else {
      throw new IOException("Same delegation token being added twice.");
      throw new IOException("Same delegation token being added twice: "
          + formatTokenId(identifier));
     }
   }
 
@@ -393,7 +399,7 @@ private synchronized void removeExpiredKeys() {
     identifier.setMaxDate(now + tokenMaxLifetime);
     identifier.setMasterKeyId(currentKey.getKeyId());
     identifier.setSequenceNumber(sequenceNum);
    LOG.info("Creating password for identifier: " + identifier
    LOG.info("Creating password for identifier: " + formatTokenId(identifier)
         + ", currentKey: " + currentKey.getKeyId());
     byte[] password = createPassword(identifier.getBytes(), currentKey.getKey());
     DelegationTokenInformation tokenInfo = new DelegationTokenInformation(now
@@ -401,7 +407,8 @@ private synchronized void removeExpiredKeys() {
     try {
       storeToken(identifier, tokenInfo);
     } catch (IOException ioe) {
      LOG.error("Could not store token !!", ioe);
      LOG.error("Could not store token " + formatTokenId(identifier) + "!!",
          ioe);
     }
     return password;
   }
@@ -418,11 +425,14 @@ protected DelegationTokenInformation checkToken(TokenIdent identifier)
     assert Thread.holdsLock(this);
     DelegationTokenInformation info = getTokenInfo(identifier);
     if (info == null) {
      throw new InvalidToken("token (" + identifier.toString()
          + ") can't be found in cache");
      throw new InvalidToken("token " + formatTokenId(identifier)
          + " can't be found in cache");
     }
    if (info.getRenewDate() < Time.now()) {
      throw new InvalidToken("token (" + identifier.toString() + ") is expired");
    long now = Time.now();
    if (info.getRenewDate() < now) {
      throw new InvalidToken("token " + formatTokenId(identifier) + " is " +
          "expired, current time: " + Time.formatTime(now) +
          " expected renewal time: " + Time.formatTime(info.getRenewDate()));
     }
     return info;
   }
@@ -458,8 +468,8 @@ public synchronized void verifyToken(TokenIdent identifier, byte[] password)
       throws InvalidToken {
     byte[] storedPassword = retrievePassword(identifier);
     if (!Arrays.equals(password, storedPassword)) {
      throw new InvalidToken("token (" + identifier
          + ") is invalid, password doesn't match");
      throw new InvalidToken("token " + formatTokenId(identifier)
          + " is invalid, password doesn't match");
     }
   }
   
@@ -477,32 +487,39 @@ public synchronized long renewToken(Token<TokenIdent> token,
     DataInputStream in = new DataInputStream(buf);
     TokenIdent id = createIdentifier();
     id.readFields(in);
    LOG.info("Token renewal for identifier: " + id + "; total currentTokens "
        +  currentTokens.size());
    LOG.info("Token renewal for identifier: " + formatTokenId(id)
        + "; total currentTokens " +  currentTokens.size());
 
     long now = Time.now();
     if (id.getMaxDate() < now) {
      throw new InvalidToken(renewer + " tried to renew an expired token");
      throw new InvalidToken(renewer + " tried to renew an expired token "
          + formatTokenId(id) + " max expiration date: "
          + Time.formatTime(id.getMaxDate())
          + " currentTime: " + Time.formatTime(now));
     }
     if ((id.getRenewer() == null) || (id.getRenewer().toString().isEmpty())) {
       throw new AccessControlException(renewer +
          " tried to renew a token without a renewer");
          " tried to renew a token " + formatTokenId(id)
          + " without a renewer");
     }
     if (!id.getRenewer().toString().equals(renewer)) {
      throw new AccessControlException(renewer +
          " tries to renew a token with renewer " + id.getRenewer());
      throw new AccessControlException(renewer
          + " tries to renew a token " + formatTokenId(id)
          + " with non-matching renewer " + id.getRenewer());
     }
     DelegationKey key = getDelegationKey(id.getMasterKeyId());
     if (key == null) {
       throw new InvalidToken("Unable to find master key for keyId="
           + id.getMasterKeyId()
          + " from cache. Failed to renew an unexpired token"
          + " with sequenceNumber=" + id.getSequenceNumber());
          + " from cache. Failed to renew an unexpired token "
          + formatTokenId(id) + " with sequenceNumber="
          + id.getSequenceNumber());
     }
     byte[] password = createPassword(token.getIdentifier(), key.getKey());
     if (!Arrays.equals(password, token.getPassword())) {
      throw new AccessControlException(renewer +
          " is trying to renew a token with wrong password");
      throw new AccessControlException(renewer
          + " is trying to renew a token "
          + formatTokenId(id) + " with wrong password");
     }
     long renewTime = Math.min(id.getMaxDate(), now + tokenRenewInterval);
     String trackingId = getTrackingIdIfEnabled(id);
@@ -510,7 +527,8 @@ public synchronized long renewToken(Token<TokenIdent> token,
         password, trackingId);
 
     if (getTokenInfo(id) == null) {
      throw new InvalidToken("Renewal request for unknown token");
      throw new InvalidToken("Renewal request for unknown token "
          + formatTokenId(id));
     }
     updateToken(id, info);
     return renewTime;
@@ -528,10 +546,11 @@ public synchronized TokenIdent cancelToken(Token<TokenIdent> token,
     DataInputStream in = new DataInputStream(buf);
     TokenIdent id = createIdentifier();
     id.readFields(in);
    LOG.info("Token cancellation requested for identifier: " + id);
    LOG.info("Token cancellation requested for identifier: "
        + formatTokenId(id));
     
     if (id.getUser() == null) {
      throw new InvalidToken("Token with no owner");
      throw new InvalidToken("Token with no owner " + formatTokenId(id));
     }
     String owner = id.getUser().getUserName();
     Text renewer = id.getRenewer();
@@ -541,11 +560,11 @@ public synchronized TokenIdent cancelToken(Token<TokenIdent> token,
         && (renewer == null || renewer.toString().isEmpty() || !cancelerShortName
             .equals(renewer.toString()))) {
       throw new AccessControlException(canceller
          + " is not authorized to cancel the token");
          + " is not authorized to cancel the token " + formatTokenId(id));
     }
     DelegationTokenInformation info = currentTokens.remove(id);
     if (info == null) {
      throw new InvalidToken("Token not found");
      throw new InvalidToken("Token not found " + formatTokenId(id));
     }
     removeStoredToken(id);
     return id;
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Time.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Time.java
index 20e2965c0d8..e96fa77dbd7 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Time.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Time.java
@@ -17,6 +17,8 @@
  */
 package org.apache.hadoop.util;
 
import java.text.SimpleDateFormat;

 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
 
@@ -32,6 +34,14 @@
    */
   private static final long NANOSECONDS_PER_MILLISECOND = 1000000;
 
  private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
      new ThreadLocal<SimpleDateFormat>() {
    @Override
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSZ");
    }
  };

   /**
    * Current system time.  Do not use this to calculate a duration or interval
    * to sleep, because it will be broken by settimeofday.  Instead, use
@@ -54,4 +64,12 @@ public static long now() {
   public static long monotonicNow() {
     return System.nanoTime() / NANOSECONDS_PER_MILLISECOND;
   }

  /**
   * Convert time in millisecond to human readable format.
   * @return a human readable string for the input time
   */
  public static String formatTime(long millis) {
    return DATE_FORMAT.get().format(millis);
  }
 }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/file/tfile/Timer.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/file/tfile/Timer.java
index ee2286af2cc..0987d4412c7 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/file/tfile/Timer.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/file/tfile/Timer.java
@@ -17,8 +17,6 @@
 package org.apache.hadoop.io.file.tfile;
 
 import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
 
 import org.apache.hadoop.util.Time;
 
@@ -30,36 +28,34 @@
 public  class Timer {
   long startTimeEpoch;
   long finishTimeEpoch;
  private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   
   public void startTime() throws IOException {
      startTimeEpoch = Time.now();
    }
    startTimeEpoch = Time.now();
  }
 
    public void stopTime() throws IOException {
      finishTimeEpoch = Time.now();
    }
  public void stopTime() throws IOException {
    finishTimeEpoch = Time.now();
  }
 
    public long getIntervalMillis() throws IOException {
      return finishTimeEpoch - startTimeEpoch;
    }
  
    public void printlnWithTimestamp(String message) throws IOException {
      System.out.println(formatCurrentTime() + "  " + message);
    }
  
    public String formatTime(long millis) {
      return formatter.format(millis);
    }
    
    public String getIntervalString() throws IOException {
      long time = getIntervalMillis();
      return formatTime(time);
    }
    
    public String formatCurrentTime() {
      return formatTime(Time.now());
    }
  public long getIntervalMillis() throws IOException {
    return finishTimeEpoch - startTimeEpoch;
  }

  public void printlnWithTimestamp(String message) throws IOException {
    System.out.println(formatCurrentTime() + "  " + message);
  }

  public String formatTime(long millis) {
    return Time.formatTime(millis);
  }

  public String getIntervalString() throws IOException {
    long time = getIntervalMillis();
    return formatTime(time);
  }
 
  public String formatCurrentTime() {
    return formatTime(Time.now());
  }
 }
 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestTime.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestTime.java
new file mode 100644
index 00000000000..360e5f8b107
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestTime.java
@@ -0,0 +1,50 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.util;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;

import org.junit.Test;

/**
 * A JUnit test to test {@link Time}.
 */
public class TestTime {

  private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
      new ThreadLocal<SimpleDateFormat>() {
    @Override
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSZ");
    }
  };

  /**
   * Test formatTime.
   * @throws IOException
   */
  @Test
  public void testFormatTime() {
    long time = Time.now();
    assertEquals(Time.formatTime(time),
        DATE_FORMAT.get().format(time));
  }
}
- 
2.19.1.windows.1

