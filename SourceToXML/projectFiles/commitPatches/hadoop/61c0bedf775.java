From 61c0bedf775e6e794d4704485ec2c41a95aecae9 Mon Sep 17 00:00:00 2001
From: Xiao Chen <xiao@apache.org>
Date: Tue, 15 Nov 2016 16:26:27 -0800
Subject: [PATCH] HADOOP-13815. TestKMS#testDelegationTokensOpsSimple and
 TestKMS#testDelegationTokensOpsKerberized Fails in Trunk.

--
 .../org/apache/hadoop/crypto/key/kms/server/TestKMS.java     | 5 ++++-
 1 file changed, 4 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java b/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java
index 384d11a4855..dac91e0841b 100644
-- a/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java
++ b/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java
@@ -1821,8 +1821,11 @@ public Void run() throws Exception {
                 Assert.fail("client should not be allowed to renew token with"
                     + "renewer=client1");
               } catch (Exception e) {
                final DelegationTokenIdentifier identifier =
                    (DelegationTokenIdentifier) token.decodeIdentifier();
                 GenericTestUtils.assertExceptionContains(
                    "tries to renew a token with renewer", e);
                    "tries to renew a token (" + identifier
                        + ") with non-matching renewer", e);
               }
             }
 
- 
2.19.1.windows.1

