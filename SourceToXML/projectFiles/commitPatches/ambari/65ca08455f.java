From 65ca08455f05b7c7e9352723f2bea21e86bc43e8 Mon Sep 17 00:00:00 2001
From: Swapan Shridhar <sshridhar@hortonworks.com>
Date: Tue, 21 Nov 2017 15:57:44 -0800
Subject: [PATCH] AMBARI-22472. Ambari Upgrade 2.5 -> 2.6 : Update
 NodeManager's HSI identity 'llap_zk_hive' and 'llap_task_hive' to use
 '/HIVE/HIVE_SERVER/hive_server_hive' reference instead of creating the same
 identity again.

--
 .../server/upgrade/UpgradeCatalog260.java     | 142 +++++++++
 .../HDP/2.5/services/YARN/kerberos.json       |  12 +-
 .../HDP/2.6/services/YARN/kerberos.json       |  24 +-
 .../server/upgrade/UpgradeCatalog260Test.java | 136 ++++++++-
 .../test_kerberos_descriptor_ranger_kms.json  | 286 ++++++++++++++++++
 5 files changed, 564 insertions(+), 36 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
index 25635b660c..a7e06547ab 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
@@ -45,7 +45,9 @@ import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
 import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
 import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
 import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
 import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalType;
 import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
 import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
@@ -135,6 +137,20 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
   private static final String CORE_SITE = "core-site";
   public static final String AMS_SSL_CLIENT = "ams-ssl-client";
   public static final String METRIC_TRUSTSTORE_ALIAS = "ssl.client.truststore.alias";

  private static final String HIVE_INTERACTIVE_SITE = "hive-interactive-site";
  public static final String HIVE_LLAP_DAEMON_KEYTAB_FILE = "hive.llap.daemon.keytab.file";
  public static final String HIVE_LLAP_ZK_SM_KEYTAB_FILE = "hive.llap.zk.sm.keytab.file";
  public static final String HIVE_LLAP_TASK_KEYTAB_FILE = "hive.llap.task.keytab.file";
  public static final String HIVE_SERVER_KERBEROS_PREFIX = "/HIVE/HIVE_SERVER/";
  public static final String YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY = "llap_zk_hive";
  public static final String YARN_LLAP_TASK_HIVE_KERBEROS_IDENTITY = "llap_task_hive";
  public static final String HIVE_SERVER_HIVE_KERBEROS_IDENTITY = "hive_server_hive";

  // Used to track whether YARN -> NODEMANAGER -> 'llap_zk_hive' kerberos descriptor was updated or not.
  private List<String> yarnKerberosDescUpdatedList = new ArrayList<>();


   /**
    * Logger.
    */
@@ -497,6 +513,7 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
     ensureZeppelinProxyUserConfigs();
     updateKerberosDescriptorArtifacts();
     updateAmsConfigs();
    updateHiveConfigs();
     updateHDFSWidgetDefinition();
     updateExistingRepositoriesToBeResolved();
   }
@@ -636,6 +653,7 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
         if (kerberosDescriptor != null) {
           fixRangerKMSKerberosDescriptor(kerberosDescriptor);
           fixIdentityReferences(getCluster(artifactEntity), kerberosDescriptor);
          fixYarnHsiKerberosDescriptorAndSiteConfig(getCluster(artifactEntity), kerberosDescriptor);
 
           artifactEntity.setArtifactData(kerberosDescriptor.toMap());
           artifactDAO.merge(artifactEntity);
@@ -662,6 +680,130 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
     }
   }
 
  /**
   * Updates YARN's NM 'llap_zk_hive' kerberos descriptor as reference and the associated config
   * hive-interactive-site/hive.llap.zk.sm.keytab.file
   */
  protected void fixYarnHsiKerberosDescriptorAndSiteConfig(Cluster cluster, KerberosDescriptor kerberosDescriptor) {
    LOG.info("Updating YARN's HSI Kerberos Descriptor ....");

    // Step 1. Get Hive -> HIVE_SERVER -> 'hive_server_hive' kerberos description for referencing later
    KerberosServiceDescriptor hiveServiceDescriptor = kerberosDescriptor.getService("HIVE");
    KerberosIdentityDescriptor hsh_identityDescriptor = null;
    KerberosPrincipalDescriptor hsh_principalDescriptor = null;
    KerberosKeytabDescriptor hsh_keytabDescriptor = null;
    if (hiveServiceDescriptor != null) {
      KerberosComponentDescriptor hiveServerKerberosDescriptor = hiveServiceDescriptor.getComponent("HIVE_SERVER");
      if (hiveServerKerberosDescriptor != null) {
        hsh_identityDescriptor = hiveServerKerberosDescriptor.getIdentity(HIVE_SERVER_HIVE_KERBEROS_IDENTITY);
        if (hsh_identityDescriptor != null) {
          LOG.info("  Retrieved HIVE->HIVE_SERVER kerberos descriptor. Name = " + hsh_identityDescriptor.getName());
          hsh_principalDescriptor = hsh_identityDescriptor.getPrincipalDescriptor();
          hsh_keytabDescriptor = hsh_identityDescriptor.getKeytabDescriptor();
        }
      }

      // Step 2. Update YARN -> NODEMANAGER's : (1). 'llap_zk_hive' and (2). 'llap_task_hive' kerberos descriptor as reference to
      // HIVE -> HIVE_SERVER -> 'hive_server_hive' (Same as YARN -> NODEMANAGER -> 'yarn_nodemanager_hive_server_hive')
      if (hsh_principalDescriptor != null && hsh_keytabDescriptor != null) {
        KerberosServiceDescriptor yarnServiceDescriptor = kerberosDescriptor.getService("YARN");
        if (yarnServiceDescriptor != null) {
          KerberosComponentDescriptor yarnNmKerberosDescriptor = yarnServiceDescriptor.getComponent("NODEMANAGER");
          if (yarnNmKerberosDescriptor != null) {
            String[] identities = {YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY, YARN_LLAP_TASK_HIVE_KERBEROS_IDENTITY};
            for (String identity : identities) {
              KerberosIdentityDescriptor identityDescriptor = yarnNmKerberosDescriptor.getIdentity(identity);

              KerberosPrincipalDescriptor principalDescriptor = null;
              KerberosKeytabDescriptor keytabDescriptor = null;
              if (identityDescriptor != null) {
                LOG.info("  Retrieved YARN->NODEMANAGER kerberos descriptor to be updated. Name = " + identityDescriptor.getName());
                principalDescriptor = identityDescriptor.getPrincipalDescriptor();
                keytabDescriptor = identityDescriptor.getKeytabDescriptor();

                identityDescriptor.setReference(HIVE_SERVER_KERBEROS_PREFIX + hsh_identityDescriptor.getName());
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' identity descriptor reference = '"
                        + identityDescriptor.getReference() + "'");
                principalDescriptor.setValue(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' principal descriptor value = '"
                        + principalDescriptor.getValue() + "'");

                // Updating keytabs now
                keytabDescriptor.setFile(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor file = '"
                        + keytabDescriptor.getFile() + "'");
                keytabDescriptor.setOwnerName(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor owner name = '" + keytabDescriptor.getOwnerName() + "'");
                keytabDescriptor.setOwnerAccess(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor owner access = '" + keytabDescriptor.getOwnerAccess() + "'");
                keytabDescriptor.setGroupName(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor group name = '" + keytabDescriptor.getGroupName() + "'");
                keytabDescriptor.setGroupAccess(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor group access = '" + keytabDescriptor.getGroupAccess() + "'");

                // Need this as trigger to update the HIVE_LLAP_ZK_SM_KEYTAB_FILE configs later.

                // Get the keytab file 'config name'.
                String[] splits = keytabDescriptor.getConfiguration().split("/");
                if (splits != null && splits.length == 2) {
                  updateYarnKerberosDescUpdatedList(splits[1]);
                  LOG.info("    Updated 'yarnKerberosDescUpdatedList' = " + getYarnKerberosDescUpdatedList());
                }
              }
            }
          }
        }
      }
    }
  }

  public void updateYarnKerberosDescUpdatedList(String val) {
    yarnKerberosDescUpdatedList.add(val);
  }

  public List<String> getYarnKerberosDescUpdatedList() {
    return yarnKerberosDescUpdatedList;
  }

  protected void updateHiveConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          // Updating YARN->NodeManager kerebros descriptor : (1). 'llap_zk_hive' and (2). 'llap_task_hive''s associated configs
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
              }
            }

            // Update step.
            if (newProperties.size() > 0) {
              try {
                updateConfigurationPropertiesForCluster(cluster, HIVE_INTERACTIVE_SITE, newProperties, true, false);
                LOG.info("Updated HSI config(s) : " + newProperties.keySet() + " with value(s) = " + newProperties.values()+" respectively.");
              } catch (AmbariException e) {
                e.printStackTrace();
              }
            }
          }
        }
      }
    }
  }

   protected void updateAmsConfigs() throws AmbariException {
     AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
     Clusters clusters = ambariManagementController.getClusters();
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.5/services/YARN/kerberos.json b/ambari-server/src/main/resources/stacks/HDP/2.5/services/YARN/kerberos.json
index fca14ab9c8..8e285e909a 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.5/services/YARN/kerberos.json
++ b/ambari-server/src/main/resources/stacks/HDP/2.5/services/YARN/kerberos.json
@@ -102,21 +102,11 @@
             },
             {
               "name": "llap_zk_hive",
              "reference": "/HIVE/HIVE_SERVER/hive_server_hive",
               "principal": {
                "value": "hive/_HOST@${realm}",
                "type" : "service",
                 "configuration": "hive-interactive-site/hive.llap.zk.sm.principal"
               },
               "keytab": {
                "file": "${keytab_dir}/hive.llap.zk.sm.keytab",
                "owner": {
                  "name": "${yarn-env/yarn_user}",
                  "access": "r"
                },
                "group": {
                  "name": "${cluster-env/user_group}",
                  "access": "r"
                },
                 "configuration": "hive-interactive-site/hive.llap.zk.sm.keytab.file"
               },
               "when" : {
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.6/services/YARN/kerberos.json b/ambari-server/src/main/resources/stacks/HDP/2.6/services/YARN/kerberos.json
index e0417bff39..bd6798cada 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.6/services/YARN/kerberos.json
++ b/ambari-server/src/main/resources/stacks/HDP/2.6/services/YARN/kerberos.json
@@ -107,21 +107,11 @@
             },
             {
               "name": "llap_task_hive",
              "reference": "/HIVE/HIVE_SERVER/hive_server_hive",
               "principal": {
                "value": "hive/_HOST@${realm}",
                "type" : "service",
                 "configuration": "hive-interactive-site/hive.llap.task.principal"
               },
               "keytab": {
                "file": "${keytab_dir}/hive.llap.task.keytab",
                "owner": {
                  "name": "${yarn-env/yarn_user}",
                  "access": "r"
                },
                "group": {
                  "name": "${cluster-env/user_group}",
                  "access": "r"
                },
                 "configuration": "hive-interactive-site/hive.llap.task.keytab.file"
               },
               "when" : {
@@ -130,21 +120,11 @@
             },
             {
               "name": "llap_zk_hive",
              "reference": "/HIVE/HIVE_SERVER/hive_server_hive",
               "principal": {
                "value": "hive/_HOST@${realm}",
                "type" : "service",
                 "configuration": "hive-interactive-site/hive.llap.zk.sm.principal"
               },
               "keytab": {
                "file": "${keytab_dir}/hive.llap.zk.sm.keytab",
                "owner": {
                  "name": "${yarn-env/yarn_user}",
                  "access": "r"
                },
                "group": {
                  "name": "${cluster-env/user_group}",
                  "access": "r"
                },
                 "configuration": "hive-interactive-site/hive.llap.zk.sm.keytab.file"
               },
               "when" : {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java
index cc58988b34..22e8ccc5ad 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java
@@ -75,6 +75,9 @@ import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
 import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
 import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
 import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalType;
 import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
 import org.apache.ambari.server.state.stack.OsFamily;
 import org.apache.commons.io.FileUtils;
@@ -648,7 +651,7 @@ public class UpgradeCatalog260Test {
     expect(artifactEntity.getArtifactData()).andReturn(kerberosDescriptor.toMap()).once();
 
     Capture<Map<String, Object>> captureMap = newCapture();
    expect(artifactEntity.getForeignKeys()).andReturn(Collections.singletonMap("cluster", "2"));
    expect(artifactEntity.getForeignKeys()).andReturn(Collections.singletonMap("cluster", "2")).times(2);
     artifactEntity.setArtifactData(capture(captureMap));
     expectLastCall().once();
 
@@ -665,11 +668,26 @@ public class UpgradeCatalog260Test {
     expect(config.getTag()).andReturn("version1").anyTimes();
     expect(config.getType()).andReturn("ranger-kms-audit").anyTimes();
 
    Map<String, String> hsiProperties = new HashMap<>();
    hsiProperties.put("hive.llap.daemon.keytab.file", "/etc/security/keytabs/hive.service.keytab");
    hsiProperties.put("hive.llap.zk.sm.keytab.file", "/etc/security/keytabs/hive.llap.zk.sm.keytab");

    Config hsiConfig = createMock(Config.class);
    expect(hsiConfig.getProperties()).andReturn(hsiProperties).anyTimes();
    expect(hsiConfig.getPropertiesAttributes()).andReturn(Collections.<String, Map<String, String>>emptyMap()).anyTimes();
    expect(hsiConfig.getTag()).andReturn("version1").anyTimes();
    expect(hsiConfig.getType()).andReturn("hive-interactive-site").anyTimes();

     Config newConfig = createMock(Config.class);
     expect(newConfig.getTag()).andReturn("version2").anyTimes();
     expect(newConfig.getType()).andReturn("ranger-kms-audit").anyTimes();
 
    Config newHsiConfig = createMock(Config.class);
    expect(newHsiConfig.getTag()).andReturn("version2").anyTimes();
    expect(newHsiConfig.getType()).andReturn("hive-interactive-site").anyTimes();

     ServiceConfigVersionResponse response = createMock(ServiceConfigVersionResponse.class);
    ServiceConfigVersionResponse response1 = createMock(ServiceConfigVersionResponse.class);
 
     StackId stackId = createMock(StackId.class);
 
@@ -683,6 +701,14 @@ public class UpgradeCatalog260Test {
     expect(cluster.getConfig(eq("ranger-kms-audit"), anyString())).andReturn(newConfig).once();
     expect(cluster.addDesiredConfig("ambari-upgrade", Collections.singleton(newConfig), "Updated ranger-kms-audit during Ambari Upgrade from 2.5.2 to 2.6.0.")).andReturn(response).once();
 
    //HIVE
    expect(cluster.getDesiredConfigByType("hive-site")).andReturn(hsiConfig).anyTimes();
    expect(cluster.getDesiredConfigByType("hive-interactive-site")).andReturn(hsiConfig).anyTimes();
    expect(cluster.getConfigsByType("hive-interactive-site")).andReturn(Collections.singletonMap("version1", hsiConfig)).anyTimes();
    expect(cluster.getServiceByConfigType("hive-interactive-site")).andReturn("HIVE").anyTimes();
    expect(cluster.getConfig(eq("hive-interactive-site"), anyString())).andReturn(newHsiConfig).anyTimes();
  

     final Clusters clusters = injector.getInstance(Clusters.class);
     expect(clusters.getCluster(2L)).andReturn(cluster).anyTimes();
 
@@ -693,12 +719,17 @@ public class UpgradeCatalog260Test {
         .andReturn(null)
         .once();
 
    replay(artifactDAO, artifactEntity, cluster, clusters, config, newConfig, response, controller, stackId);
    Capture<? extends Map<String, String>> captureHsiProperties = newCapture();

    expect(controller.createConfig(eq(cluster), eq(stackId), eq("hive-interactive-site"), capture(captureHsiProperties), anyString(), anyObject(Map.class)))
            .andReturn(null)
            .anyTimes();

    replay(artifactDAO, artifactEntity, cluster, clusters, config, newConfig, hsiConfig, newHsiConfig, response, response1, controller, stackId);
 
     UpgradeCatalog260 upgradeCatalog260 = injector.getInstance(UpgradeCatalog260.class);
     upgradeCatalog260.updateKerberosDescriptorArtifact(artifactDAO, artifactEntity);
     verify(artifactDAO, artifactEntity, cluster, clusters, config, newConfig, response, controller, stackId);

     KerberosDescriptor kerberosDescriptorUpdated = new KerberosDescriptorFactory().createInstance(captureMap.getValue());
     Assert.assertNotNull(kerberosDescriptorUpdated);
 
@@ -722,6 +753,39 @@ public class UpgradeCatalog260Test {
     Assert.assertTrue(captureProperties.hasCaptured());
     Map<String, String> newProperties = captureProperties.getValue();
     Assert.assertEquals("correct_value@EXAMPLE.COM", newProperties.get("xasecure.audit.jaas.Client.option.principal"));

    // YARN's NodeManager identities (1). 'llap_zk_hive' and (2). 'llap_task_hive' checks after modifications.
    Map<String, List<String>> identitiesMap = new HashMap<>();
    identitiesMap.put("llap_zk_hive", new ArrayList<String>() {{
      add("hive-interactive-site/hive.llap.zk.sm.keytab.file");
      add("hive-interactive-site/hive.llap.zk.sm.principal");
    }});
    identitiesMap.put("llap_task_hive", new ArrayList<String>() {{
      add("hive-interactive-site/hive.llap.task.keytab.file");
      add("hive-interactive-site/hive.llap.task.principal");
    }});
    for (String llapIdentity : identitiesMap.keySet()) {
      KerberosIdentityDescriptor yarnKerberosIdentityDescriptor = kerberosDescriptorUpdated.getService("YARN").getComponent("NODEMANAGER").getIdentity(llapIdentity);
      Assert.assertNotNull(yarnKerberosIdentityDescriptor);
      Assert.assertEquals("/HIVE/HIVE_SERVER/hive_server_hive", yarnKerberosIdentityDescriptor.getReference());

      KerberosKeytabDescriptor yarnKerberosKeytabDescriptor = yarnKerberosIdentityDescriptor.getKeytabDescriptor();
      Assert.assertNotNull(yarnKerberosKeytabDescriptor);

      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getGroupAccess());
      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getGroupName());
      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getOwnerAccess());
      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getOwnerName());
      Assert.assertEquals(null, yarnKerberosKeytabDescriptor.getFile());
      Assert.assertEquals(identitiesMap.get(llapIdentity).get(0), yarnKerberosKeytabDescriptor.getConfiguration());

      KerberosPrincipalDescriptor yarnKerberosPrincipalDescriptor = yarnKerberosIdentityDescriptor.getPrincipalDescriptor();
      Assert.assertNotNull(yarnKerberosPrincipalDescriptor);
      Assert.assertEquals(null, yarnKerberosPrincipalDescriptor.getName());
      Assert.assertEquals(KerberosPrincipalType.SERVICE, yarnKerberosPrincipalDescriptor.getType());
      Assert.assertEquals(null, yarnKerberosPrincipalDescriptor.getValue());
      Assert.assertEquals(identitiesMap.get(llapIdentity).get(1), yarnKerberosPrincipalDescriptor.getConfiguration());
    }
   }
 
   @Test
@@ -780,6 +844,72 @@ public class UpgradeCatalog260Test {
     assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
   }
 
  @Test
  public void testUpdateHiveConfigs() throws Exception {

    Map<String, String> oldProperties = new HashMap<String, String>() {
      {
        put("hive.llap.zk.sm.keytab.file", "/etc/security/keytabs/hive.llap.zk.sm.keytab");
        put("hive.llap.daemon.keytab.file", "/etc/security/keytabs/hive.service.keytab");
        put("hive.llap.task.keytab.file", "/etc/security/keytabs/hive.llap.task.keytab");
      }
    };
    Map<String, String> newProperties = new HashMap<String, String>() {
      {
        put("hive.llap.zk.sm.keytab.file", "/etc/security/keytabs/hive.service.keytab");
        put("hive.llap.daemon.keytab.file", "/etc/security/keytabs/hive.service.keytab");
        put("hive.llap.task.keytab.file", "/etc/security/keytabs/hive.service.keytab");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockHsiConfigs = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("hive-interactive-site")).andReturn(mockHsiConfigs).atLeastOnce();
    expect(mockHsiConfigs.getProperties()).andReturn(oldProperties).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();

    replay(injector, clusters, mockHsiConfigs, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
            .addMockedMethod("createConfiguration")
            .addMockedMethod("getClusters", new Class[] { })
            .addMockedMethod("createConfig")
            .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
            .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
            anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();
    replay(controller, injector2);

    // This tests the update of HSI config 'hive.llap.daemon.keytab.file'.
    UpgradeCatalog260  upgradeCatalog260 = new UpgradeCatalog260(injector2);
    // Set 'isYarnKerberosDescUpdated' value to true, implying kerberos descriptor was updated.
    upgradeCatalog260.updateYarnKerberosDescUpdatedList("hive.llap.zk.sm.keytab.file");
    upgradeCatalog260.updateYarnKerberosDescUpdatedList("hive.llap.task.keytab.file");

    upgradeCatalog260.updateHiveConfigs();

    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
  }

   @Test
    public void testHDFSWidgetUpdate() throws Exception {
          final Clusters clusters = createNiceMock(Clusters.class);
diff --git a/ambari-server/src/test/resources/kerberos/test_kerberos_descriptor_ranger_kms.json b/ambari-server/src/test/resources/kerberos/test_kerberos_descriptor_ranger_kms.json
index e17e12120d..8c27a9af4c 100644
-- a/ambari-server/src/test/resources/kerberos/test_kerberos_descriptor_ranger_kms.json
++ b/ambari-server/src/test/resources/kerberos/test_kerberos_descriptor_ranger_kms.json
@@ -104,6 +104,292 @@
           ]
         }
       ]
    },
    {
      "name": "YARN",
      "identities": [
        {
          "name": "yarn_spnego",
          "reference": "/spnego"
        },
        {
          "name": "yarn_smokeuser",
          "reference": "/smokeuser"
        }
      ],
      "configurations": [
        {
          "yarn-site": {
            "yarn.timeline-service.enabled": "true",
            "yarn.timeline-service.http-authentication.type": "kerberos",
            "yarn.acl.enable": "true",
            "yarn.admin.acl": "${yarn-env/yarn_user},dr.who",
            "yarn.timeline-service.http-authentication.signature.secret": "",
            "yarn.timeline-service.http-authentication.signature.secret.file": "",
            "yarn.timeline-service.http-authentication.signer.secret.provider": "",
            "yarn.timeline-service.http-authentication.signer.secret.provider.object": "",
            "yarn.timeline-service.http-authentication.token.validity": "",
            "yarn.timeline-service.http-authentication.cookie.domain": "",
            "yarn.timeline-service.http-authentication.cookie.path": "",
            "yarn.timeline-service.http-authentication.proxyuser.*.hosts": "",
            "yarn.timeline-service.http-authentication.proxyuser.*.users": "",
            "yarn.timeline-service.http-authentication.proxyuser.*.groups": "",
            "yarn.timeline-service.http-authentication.kerberos.name.rules": "",
            "yarn.resourcemanager.proxyuser.*.groups": "",
            "yarn.resourcemanager.proxyuser.*.hosts": "",
            "yarn.resourcemanager.proxyuser.*.users": "",
            "yarn.resourcemanager.proxy-user-privileges.enabled": "true",
            "yarn.resourcemanager.zk-acl" : "sasl:${principals/YARN/RESOURCEMANAGER/resource_manager_rm|principalPrimary()}:rwcda",
            "hadoop.registry.secure" : "true",
            "hadoop.registry.system.accounts" : "sasl:${principals/YARN/APP_TIMELINE_SERVER/app_timeline_server_yarn|principalPrimary()},sasl:${principals/MAPREDUCE2/HISTORYSERVER/history_server_jhs|principalPrimary()},sasl:${principals/HDFS/NAMENODE/hdfs|principalPrimary()},sasl:${principals/YARN/RESOURCEMANAGER/resource_manager_rm|principalPrimary()},sasl:${principals/HIVE/HIVE_SERVER/hive_server_hive|principalPrimary()}",
            "hadoop.registry.client.auth" : "kerberos",
            "hadoop.registry.jaas.context" : "Client"
          }
        },
        {
          "core-site": {
            "hadoop.proxyuser.${yarn-env/yarn_user}.groups": "*",
            "hadoop.proxyuser.${yarn-env/yarn_user}.hosts": "${clusterHostInfo/rm_host}"
          }
        },
        {
          "capacity-scheduler": {
            "yarn.scheduler.capacity.root.acl_administer_queue": "${yarn-env/yarn_user}",
            "yarn.scheduler.capacity.root.default.acl_administer_queue": "${yarn-env/yarn_user}",
            "yarn.scheduler.capacity.root.acl_administer_jobs": "${yarn-env/yarn_user}",
            "yarn.scheduler.capacity.root.default.acl_administer_jobs": "${yarn-env/yarn_user}",
            "yarn.scheduler.capacity.root.default.acl_submit_applications": "${yarn-env/yarn_user}"
          }
        },
        {
          "ranger-yarn-audit": {
            "xasecure.audit.jaas.Client.loginModuleName": "com.sun.security.auth.module.Krb5LoginModule",
            "xasecure.audit.jaas.Client.loginModuleControlFlag": "required",
            "xasecure.audit.jaas.Client.option.useKeyTab": "true",
            "xasecure.audit.jaas.Client.option.storeKey": "false",
            "xasecure.audit.jaas.Client.option.serviceName": "solr",
            "xasecure.audit.destination.solr.force.use.inmemory.jaas.config": "true"
          }
        }
      ],
      "components": [
        {
          "name": "NODEMANAGER",
          "identities": [
            {
              "name": "nodemanager_nm",
              "principal": {
                "value": "nm/_HOST@${realm}",
                "type" : "service",
                "configuration": "yarn-site/yarn.nodemanager.principal",
                "local_username": "${yarn-env/yarn_user}"
              },
              "keytab": {
                "file": "${keytab_dir}/nm.service.keytab",
                "owner": {
                  "name": "${yarn-env/yarn_user}",
                  "access": "r"
                },
                "group": {
                  "name": "${cluster-env/user_group}",
                  "access": ""
                },
                "configuration": "yarn-site/yarn.nodemanager.keytab"
              }
            },
            {
              "name": "yarn_nodemanager_hive_server_hive",
              "reference": "/HIVE/HIVE_SERVER/hive_server_hive",
              "principal": {
                "configuration": "hive-interactive-site/hive.llap.daemon.service.principal"
              },
              "keytab": {
                "configuration": "hive-interactive-site/hive.llap.daemon.keytab.file"
              },
              "when" : {
                "contains" : ["services", "HIVE"]
              }
            },
            {
              "name": "llap_task_hive",
              "principal": {
                "value": "hive/_HOST@${realm}",
                "type" : "service",
                "configuration": "hive-interactive-site/hive.llap.task.principal"
              },
              "keytab": {
                "file": "${keytab_dir}/hive.llap.task.keytab",
                "owner": {
                  "name": "${yarn-env/yarn_user}",
                  "access": "r"
                },
                "group": {
                  "name": "${cluster-env/user_group}",
                  "access": "r"
                },
                "configuration": "hive-interactive-site/hive.llap.task.keytab.file"
              },
              "when" : {
                "contains" : ["services", "HIVE"]
              }
            },
            {
              "name": "llap_zk_hive",
              "principal": {
                "value": "hive/_HOST@${realm}",
                "type" : "service",
                "configuration": "hive-interactive-site/hive.llap.zk.sm.principal"
              },
              "keytab": {
                "file": "${keytab_dir}/hive.llap.zk.sm.keytab",
                "owner": {
                  "name": "${yarn-env/yarn_user}",
                  "access": "r"
                },
                "group": {
                  "name": "${cluster-env/user_group}",
                  "access": "r"
                },
                "configuration": "hive-interactive-site/hive.llap.zk.sm.keytab.file"
              },
              "when" : {
                "contains" : ["services", "HIVE"]
              }
            },
            {
              "name": "yarn_nodemanager_spnego",
              "reference": "/spnego",
              "principal": {
                "configuration": "yarn-site/yarn.nodemanager.webapp.spnego-principal"
              },
              "keytab": {
                "configuration": "yarn-site/yarn.nodemanager.webapp.spnego-keytab-file"
              }
            }
          ],
          "configurations": [
            {
              "yarn-site": {
                "yarn.nodemanager.container-executor.class": "org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor"
              }
            }
          ]
        }
      ]
    },
    {
      "name": "HIVE",
      "identities": [
        {
          "name": "hive_spnego",
          "reference": "/spnego"
        },
        {
          "name": "hive_smokeuser",
          "reference": "/smokeuser"
        }
      ],
      "configurations": [
        {
          "hive-site": {
            "hive.metastore.sasl.enabled": "true",
            "hive.server2.authentication": "KERBEROS"
          }
        },
        {
          "ranger-hive-audit": {
            "xasecure.audit.jaas.Client.loginModuleName": "com.sun.security.auth.module.Krb5LoginModule",
            "xasecure.audit.jaas.Client.loginModuleControlFlag": "required",
            "xasecure.audit.jaas.Client.option.useKeyTab": "true",
            "xasecure.audit.jaas.Client.option.storeKey": "false",
            "xasecure.audit.jaas.Client.option.serviceName": "solr",
            "xasecure.audit.destination.solr.force.use.inmemory.jaas.config": "true"
          }
        }
      ],
      "components": [
        {
          "name": "HIVE_SERVER",
          "identities": [
            {
              "name": "hive_hive_server_hdfs",
              "reference": "/HDFS/NAMENODE/hdfs"
            },
            {
              "name": "hive_server_hive",
              "principal": {
                "value": "hive/_HOST@${realm}",
                "type": "service",
                "configuration": "hive-site/hive.server2.authentication.kerberos.principal",
                "local_username": "${hive-env/hive_user}"
              },
              "keytab": {
                "file": "${keytab_dir}/hive.service.keytab",
                "owner": {
                  "name": "${hive-env/hive_user}",
                  "access": "r"
                },
                "group": {
                  "name": "${cluster-env/user_group}",
                  "access": "r"
                },
                "configuration": "hive-site/hive.server2.authentication.kerberos.keytab"
              }
            },
            {
              "name": "atlas_kafka",
              "reference": "/HIVE/HIVE_SERVER/hive_server_hive",
              "principal": {
                "configuration": "hive-atlas-application.properties/atlas.jaas.KafkaClient.option.principal"
              },
              "keytab": {
                "configuration": "hive-atlas-application.properties/atlas.jaas.KafkaClient.option.keyTab"
              }
            },
            {
              "name": "hive_hive_server_spnego",
              "reference": "/spnego",
              "principal": {
                "configuration": "hive-site/hive.server2.authentication.spnego.principal"
              },
              "keytab": {
                "configuration": "hive-site/hive.server2.authentication.spnego.keytab"
              }
            },
            {
              "name": "ranger_audit",
              "reference": "/HIVE/HIVE_SERVER/hive_server_hive",
              "principal": {
                "configuration": "ranger-hive-audit/xasecure.audit.jaas.Client.option.principal"
              },
              "keytab": {
                "configuration": "ranger-hive-audit/xasecure.audit.jaas.Client.option.keyTab"
              }
            }
          ]
        },
        {
          "name": "HIVE_SERVER_INTERACTIVE",
          "identities": [
            {
              "name": "hive_hive_server_interactive_hdfs",
              "reference": "/HDFS/NAMENODE/hdfs"
            },
            {
              "name": "hive_hive_server_interactive_hive_server_hive",
              "reference": "/HIVE/HIVE_SERVER/hive_server_hive"
            },
            {
              "name": "hive_hive_server_interactive_spnego",
              "reference": "/HIVE/HIVE_SERVER/spnego"
            },
            {
              "name": "hive_hive_server_interactive_llap_zk_hive",
              "reference": "/YARN/NODEMANAGER/llap_zk_hive"
            }
          ]
        }
      ]
     }
   ]
 }
\ No newline at end of file
- 
2.19.1.windows.1

