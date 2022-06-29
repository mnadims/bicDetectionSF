From 43ae930b0188f22913dcc67ec5c8ea168ff4deae Mon Sep 17 00:00:00 2001
From: jspeidel <jspeidel@hortonworks.com>
Date: Tue, 28 Apr 2015 20:43:59 -0400
Subject: [PATCH] AMBARI-10811. Fix issues with config topology update in
 certain topologies which include HA and implicit MYSQL_SERVER component
 inclusion

--
 .../BlueprintConfigurationProcessor.java      | 96 +++++++++++++++----
 .../topology/ClusterConfigurationRequest.java |  7 +-
 .../server/topology/TopologyManager.java      | 25 +++--
 .../BlueprintConfigurationProcessorTest.java  | 48 +++++++++-
 4 files changed, 142 insertions(+), 34 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java
index 95e9807176..7938cc10bc 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java
@@ -636,12 +636,12 @@ public class BlueprintConfigurationProcessor {
      *
      * @return new property value
      */
    public String updateForClusterCreate(String propertyName,
    String updateForClusterCreate(String propertyName,
                                          String origValue,
                                          Map<String, Map<String, String>> properties,
                                          ClusterTopology topology);
 
    public Collection<String> getRequiredHostGroups(String origValue,
    Collection<String> getRequiredHostGroups(String origValue,
                                                     Map<String, Map<String, String>> properties,
                                                     ClusterTopology topology);
   }
@@ -792,32 +792,79 @@ public class BlueprintConfigurationProcessor {
         return Collections.singleton(hostGroupName);
       } else {
         Collection<String> matchingGroups = topology.getHostGroupsForComponent(component);
        if (matchingGroups.size() == 1) {
        int matchingGroupCount = matchingGroups.size();
        if (matchingGroupCount == 1) {
           return Collections.singleton(matchingGroups.iterator().next());
         } else {
          if (topology.isNameNodeHAEnabled() && isComponentNameNode() && (matchingGroups.size() == 2)) {
            // if this is the defaultFS property, it should reflect the nameservice name,
            // rather than a hostname (used in non-HA scenarios)
            if (properties.containsKey("core-site") && properties.get("core-site").get("fs.defaultFS").equals(origValue)) {
              return Collections.emptySet();
          Cardinality cardinality = topology.getBlueprint().getStack().getCardinality(component);
          // if no matching host groups are found for a component whose configuration
          // is handled by this updater, return an empty set
          if (matchingGroupCount == 0 && cardinality.isValidCount(0)) {
            return Collections.emptySet();
          } else {
            //todo: shouldn't have all of these hard coded HA rules here
            if (topology.isNameNodeHAEnabled() && isComponentNameNode() && (matchingGroupCount == 2)) {
              // if this is the defaultFS property, it should reflect the nameservice name,
              // rather than a hostname (used in non-HA scenarios)
              if (properties.containsKey("core-site") && properties.get("core-site").get("fs.defaultFS").equals(origValue)) {
                return Collections.emptySet();
              }

              if (properties.containsKey("hbase-site") && properties.get("hbase-site").get("hbase.rootdir").equals(origValue)) {
                // hbase-site's reference to the namenode is handled differently in HA mode, since the
                // reference must point to the logical nameservice, rather than an individual namenode
                return Collections.emptySet();
              }

              if (properties.containsKey("accumulo-site") && properties.get("accumulo-site").get("instance.volumes").equals(origValue)) {
                // accumulo-site's reference to the namenode is handled differently in HA mode, since the
                // reference must point to the logical nameservice, rather than an individual namenode
                return Collections.emptySet();
              }

              if (!origValue.contains("localhost")) {
                // if this NameNode HA property is a FDQN, then simply return it
                return Collections.emptySet();
              }
             }
 
            if (properties.containsKey("hbase-site") && properties.get("hbase-site").get("hbase.rootdir").equals(origValue)) {
              // hbase-site's reference to the namenode is handled differently in HA mode, since the
              // reference must point to the logical nameservice, rather than an individual namenode
            if (topology.isNameNodeHAEnabled() && isComponentSecondaryNameNode() && (matchingGroupCount == 0)) {
              // if HDFS HA is enabled, then no replacement is necessary for properties that refer to the SECONDARY_NAMENODE
              // eventually this type of information should be encoded in the stacks
               return Collections.emptySet();
             }
          }
 
          if (topology.isNameNodeHAEnabled() && isComponentSecondaryNameNode() && (matchingGroups.isEmpty())) {
            // if HDFS HA is enabled, then no replacement is necessary for properties that refer to the SECONDARY_NAMENODE
            // eventually this type of information should be encoded in the stacks
            return Collections.emptySet();
          }
            if (isYarnResourceManagerHAEnabled(properties) && isComponentResourceManager() && (matchingGroupCount == 2)) {
              if (!origValue.contains("localhost")) {
                // if this Yarn property is a FQDN, then simply return it
                return Collections.emptySet();
              }
            }
 
          //todo:
          throw new IllegalArgumentException("Unable to determine required host groups for component. " +
              "Component '" + component + "' is not mapped to any host group or is mapped to multiple groups.");
            if ((isOozieServerHAEnabled(properties)) && isComponentOozieServer() && (matchingGroupCount > 1)) {
              if (!origValue.contains("localhost")) {
                // if this Oozie property is a FQDN, then simply return it
                return Collections.emptySet();
              }
            }

            if ((isHiveServerHAEnabled(properties)) && isComponentHiveServer() && (matchingGroupCount > 1)) {
              if (!origValue.contains("localhost")) {
                // if this Hive property is a FQDN, then simply return it
                return Collections.emptySet();
              }
            }

            if ((isComponentHiveMetaStoreServer()) && matchingGroupCount > 1) {
              if (!origValue.contains("localhost")) {
                // if this Hive MetaStore property is a FQDN, then simply return it
                return Collections.emptySet();
              }
            }
            //todo: property name
            throw new IllegalArgumentException("Unable to update configuration property with topology information. " +
                "Component '" + component + "' is not mapped to any host group or is mapped to multiple groups.");
          }
         }
       }
     }
@@ -999,6 +1046,15 @@ public class BlueprintConfigurationProcessor {
       }
     }
 
    @Override
    public Collection<String> getRequiredHostGroups(String origValue, Map<String, Map<String, String>> properties, ClusterTopology topology) {
      if (isDatabaseManaged(properties)) {
        return super.getRequiredHostGroups(origValue, properties, topology);
      } else {
        return Collections.emptySet();
      }
    }

     /**
      * Determine if database is managed, meaning that it is a component in the cluster topology.
      *
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterConfigurationRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterConfigurationRequest.java
index 1bffbf27b8..07ea50b6a9 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterConfigurationRequest.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterConfigurationRequest.java
@@ -68,7 +68,12 @@ public class ClusterConfigurationRequest {
 
   public void process() throws AmbariException, ConfigurationTopologyException {
     // this will update the topo cluster config and all host group configs in the cluster topology
    configurationProcessor.doUpdateForClusterCreate();
    try {
      configurationProcessor.doUpdateForClusterCreate();
    } catch (ConfigurationTopologyException e) {
      //log and continue to set configs on cluster to make progress
      LOG.error("An exception occurred while doing configuration topology update: " + e, e);
    }
     setConfigurationsOnCluster(clusterTopology, "TOPOLOGY_RESOLVED");
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java
index 3e1b565a83..fb4baece17 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java
@@ -39,6 +39,8 @@ import org.apache.ambari.server.orm.entities.StageEntity;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.SecurityType;
 import org.apache.ambari.server.state.host.HostImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 import java.util.ArrayList;
 import java.util.Collection;
@@ -81,6 +83,8 @@ public class TopologyManager {
   private final static AtomicLong nextTaskId = new AtomicLong(10000);
   private final Object serviceResourceLock = new Object();
 
  protected final static Logger LOG = LoggerFactory.getLogger(TopologyManager.class);

 
   public TopologyManager() {
     pendingTasks.put(TopologyTask.Type.CONFIGURE, new HashSet<TopologyTask>());
@@ -174,7 +178,7 @@ public class TopologyManager {
 
     if (! matchedToRequest) {
       synchronized (availableHosts) {
        System.out.printf("TopologyManager: Queueing available host %s\n", hostName);
        LOG.info("TopologyManager: Queueing available host {}", hostName);
         availableHosts.add(host);
       }
     }
@@ -500,7 +504,7 @@ public class TopologyManager {
 
     @Override
     public void run() {
      System.out.println("TopologyManager.ConfigureClusterTask: Entering");
      LOG.info("TopologyManager.ConfigureClusterTask: Entering");
 
       boolean completed = false;
       boolean interrupted = false;
@@ -520,25 +524,25 @@ public class TopologyManager {
 
       if (! interrupted) {
         try {
          System.out.println("TopologyManager.ConfigureClusterTask: Setting Configuration on cluster");
          LOG.info("TopologyManager.ConfigureClusterTask: Setting Configuration on cluster");
           // sets updated configuration on topology and cluster
           configRequest.process();
         } catch (Exception e) {
           //todo: how to handle this?  If this fails, we shouldn't start any hosts.
          System.out.println("TopologyManager.ConfigureClusterTask: " +
              "An exception occurred while attempting to process cluster configs and set on cluster");
          LOG.error("TopologyManager.ConfigureClusterTask: " +
              "An exception occurred while attempting to process cluster configs and set on cluster: " + e);
           e.printStackTrace();
         }
 
         synchronized (configurationFlagLock) {
          System.out.println("TopologyManager.ConfigureClusterTask: Setting configure complete flag to true");
          LOG.info("TopologyManager.ConfigureClusterTask: Setting configure complete flag to true");
           configureComplete = true;
         }
 
         // execute all queued install/start tasks
         executor.submit(new ExecuteQueuedHostTasks());
       }
      System.out.println("TopologyManager.ConfigureClusterTask: Exiting");
      LOG.info("TopologyManager.ConfigureClusterTask: Exiting");
     }
 
     // get set of required host groups from config processor and confirm that all requests
@@ -549,9 +553,10 @@ public class TopologyManager {
       try {
         requiredHostGroups = configRequest.getRequiredHostGroups();
       } catch (RuntimeException e) {
        //todo
        System.out.println("Caught an error from Config Processor: " + e);
        throw e;
        //todo: for now if an exception occurs, log error and return true which will result in topology update
        LOG.error("An exception occurred while attempting to determine required host groups for config update " + e);
        e.printStackTrace();
        requiredHostGroups = Collections.emptyList();
       }
 
       synchronized (outstandingRequests) {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
index 34b239b40e..789847386f 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
@@ -48,6 +48,7 @@ import org.apache.ambari.server.topology.HostGroup;
 import org.apache.ambari.server.topology.HostGroupImpl;
 import org.apache.ambari.server.topology.HostGroupInfo;
 import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.commons.collections.map.HashedMap;
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
@@ -139,6 +140,8 @@ public class BlueprintConfigurationProcessorTest {
         expect(stack.getServiceForComponent(component)).andReturn(service).anyTimes();
       }
     }

    expect(stack.getCardinality("MYSQL_SERVER")).andReturn(new Cardinality("0-1")).anyTimes();
   }
 
   @After
@@ -2182,7 +2185,7 @@ public class BlueprintConfigurationProcessorTest {
 
     // verify that the properties with hostname information was correctly preserved
     assertEquals("Yarn Log Server URL was incorrectly updated",
        "http://" + expectedHostName +":19888/jobhistory/logs", yarnSiteProperties.get("yarn.log.server.url"));
        "http://" + expectedHostName + ":19888/jobhistory/logs", yarnSiteProperties.get("yarn.log.server.url"));
     assertEquals("Yarn ResourceManager hostname was incorrectly exported",
         expectedHostName, yarnSiteProperties.get("yarn.resourcemanager.hostname"));
     assertEquals("Yarn ResourceManager tracker address was incorrectly updated",
@@ -2910,7 +2913,7 @@ public class BlueprintConfigurationProcessorTest {
       expectedHostName + ":" + expectedPortNum, falconStartupProperties.get("*.broker.url"));
 
     assertEquals("Falcon Kerberos Principal property not properly exported",
      "falcon/" + expectedHostName + "@EXAMPLE.COM", falconStartupProperties.get("*.falcon.service.authentication.kerberos.principal"));
        "falcon/" + expectedHostName + "@EXAMPLE.COM", falconStartupProperties.get("*.falcon.service.authentication.kerberos.principal"));
 
     assertEquals("Falcon Kerberos HTTP Principal property not properly exported",
       "HTTP/" + expectedHostName + "@EXAMPLE.COM", falconStartupProperties.get("*.falcon.http.authentication.kerberos.principal"));
@@ -3123,7 +3126,7 @@ public class BlueprintConfigurationProcessorTest {
       "localhost", stormSiteProperties.get("supervisor.childopts"));
 
     assertEquals("nimbus startup settings not properly handled by cluster create",
      "localhost", stormSiteProperties.get("nimbus.childopts"));
        "localhost", stormSiteProperties.get("nimbus.childopts"));
 
     assertEquals("Kafka ganglia host property not properly handled by cluster create",
       "localhost", kafkaBrokerProperties.get("kafka.ganglia.metrics.host"));
@@ -3526,6 +3529,45 @@ public class BlueprintConfigurationProcessorTest {
       hdfsSiteProperties.get("dfs.namenode.shared.edits.dir"));
   }
 
  @Test
  public void testGetRequiredHostGroups___validComponentCountofZero() throws Exception {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> hiveSite = new HashMap<String, String>();
    properties.put("hive-site", hiveSite);
    Map<String, String> hiveEnv = new HashMap<String, String>();
    properties.put("hive-env", hiveEnv);

    hiveSite.put("javax.jdo.option.ConnectionURL", "localhost:1111");
    // not the exact string but we are only looking for "New"
    hiveEnv.put("hive_database", "New Database");


    Configuration clusterConfig = new Configuration(properties,
        Collections.<String, Map<String, Map<String, String>>>emptyMap());

    Collection<String> hgComponents1 = new HashSet<String>();
    hgComponents1.add("HIVE_SERVER");
    hgComponents1.add("NAMENODE");
    TestHostGroup group1 = new TestHostGroup("group1", hgComponents1, Collections.singleton("host1"));

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    TestHostGroup group2 = new TestHostGroup("group2", hgComponents2, Collections.singleton("host2"));

    Collection<TestHostGroup> hostGroups = new ArrayList<TestHostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    ClusterTopology topology = createClusterTopology("c1", bp, clusterConfig, hostGroups);
    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(topology);

    // call top-level export method
    Collection<String> requiredGroups = updater.getRequiredHostGroups();
    System.out.println("Required Groups: " + requiredGroups);


  }

   private static String createExportedAddress(String expectedPortNum, String expectedHostGroupName) {
     return createExportedHostName(expectedHostGroupName, expectedPortNum);
   }
- 
2.19.1.windows.1

