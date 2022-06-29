From c9936ad349e3e703dcbf6f2a1644f8a3e45a23d0 Mon Sep 17 00:00:00 2001
From: Swapan Shridhar <sshridhar@hortonworks.com>
Date: Mon, 27 Nov 2017 00:47:57 -0800
Subject: [PATCH] AMBARI-22517. NPE during Ambari schema upgrade while updating
 Hive configs.

--
 .../server/upgrade/UpgradeCatalog260.java     | 38 ++++++++++---------
 1 file changed, 20 insertions(+), 18 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
index 33b62f8c5f..4d9a5dacf8 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
@@ -775,26 +775,28 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
           // hive-interactive-site/hive.llap.zk.sm.keytab.file and hive-interactive-site/hive.llap.task.keytab.file respectively,
           // based on what hive-interactive-site/hive.llap.daemon.keytab.file has.
           Config hsiSiteConfig = cluster.getDesiredConfigByType(HIVE_INTERACTIVE_SITE);
          Map<String, String> hsiSiteConfigProperties = hsiSiteConfig.getProperties();
          if (hsiSiteConfigProperties != null &&
                  hsiSiteConfigProperties.containsKey(HIVE_LLAP_DAEMON_KEYTAB_FILE)) {
            String[] identities = {HIVE_LLAP_ZK_SM_KEYTAB_FILE, HIVE_LLAP_TASK_KEYTAB_FILE};
            Map<String, String> newProperties = new HashMap<>();
            for (String identity : identities) {
              // Update only if we were able to modify the corresponding kerberos descriptor,
              // reflected in list 'getYarnKerberosDescUpdatedList'.
              if (getYarnKerberosDescUpdatedList().contains(identity) && hsiSiteConfigProperties.containsKey(identity)) {
                newProperties.put(identity, hsiSiteConfigProperties.get(HIVE_LLAP_DAEMON_KEYTAB_FILE));
          if (hsiSiteConfig != null) {
            Map<String, String> hsiSiteConfigProperties = hsiSiteConfig.getProperties();
            if (hsiSiteConfigProperties != null &&
                    hsiSiteConfigProperties.containsKey(HIVE_LLAP_DAEMON_KEYTAB_FILE)) {
              String[] identities = {HIVE_LLAP_ZK_SM_KEYTAB_FILE, HIVE_LLAP_TASK_KEYTAB_FILE};
              Map<String, String> newProperties = new HashMap<>();
              for (String identity : identities) {
                // Update only if we were able to modify the corresponding kerberos descriptor,
                // reflected in list 'getYarnKerberosDescUpdatedList'.
                if (getYarnKerberosDescUpdatedList().contains(identity) && hsiSiteConfigProperties.containsKey(identity)) {
                  newProperties.put(identity, hsiSiteConfigProperties.get(HIVE_LLAP_DAEMON_KEYTAB_FILE));
                }
               }
            }
 
            // Update step.
            if (newProperties.size() > 0) {
              try {
                updateConfigurationPropertiesForCluster(cluster, HIVE_INTERACTIVE_SITE, newProperties, true, false);
                LOG.info("Updated HSI config(s) : " + newProperties.keySet() + " with value(s) = " + newProperties.values()+" respectively.");
              } catch (AmbariException e) {
                e.printStackTrace();
              // Update step.
              if (newProperties.size() > 0) {
                try {
                  updateConfigurationPropertiesForCluster(cluster, HIVE_INTERACTIVE_SITE, newProperties, true, false);
                  LOG.info("Updated HSI config(s) : " + newProperties.keySet() + " with value(s) = " + newProperties.values() + " respectively.");
                } catch (AmbariException e) {
                  e.printStackTrace();
                }
               }
             }
           }
- 
2.19.1.windows.1

