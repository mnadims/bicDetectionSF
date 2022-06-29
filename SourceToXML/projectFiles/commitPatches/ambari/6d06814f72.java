From 6d06814f729d45db11d63465d0e9d640c49d2b23 Mon Sep 17 00:00:00 2001
From: Bob Nettleton <rnettleton@hortonworks.com>
Date: Fri, 8 Jan 2016 22:25:59 -0500
Subject: [PATCH] AMBARI-14555. Add Blueprints support for starting only a
 subset of components within a host group. (rnettleton)

--
 .../render/ClusterBlueprintRenderer.java      |  5 +-
 .../StackAdvisorBlueprintProcessor.java       |  2 +-
 .../internal/BlueprintResourceProvider.java   |  6 ++
 .../internal/ExportBlueprintRequest.java      | 10 ++-
 .../HostComponentResourceProvider.java        | 38 ++++++++++-
 .../controller/internal/ProvisionAction.java  | 25 ++++++++
 .../internal/ProvisionClusterRequest.java     |  5 --
 .../entities/HostGroupComponentEntity.java    | 24 +++++++
 .../entities/HostGroupComponentEntityPK.java  |  4 ++
 .../ambari/server/topology/AmbariContext.java |  4 +-
 .../server/topology/BlueprintFactory.java     | 17 +++--
 .../ambari/server/topology/BlueprintImpl.java | 15 +++--
 .../topology/BlueprintValidatorImpl.java      |  4 +-
 .../server/topology/ClusterTopology.java      |  3 +-
 .../server/topology/ClusterTopologyImpl.java  | 15 +++--
 .../ambari/server/topology/Component.java     | 59 +++++++++++++++++
 .../ambari/server/topology/HostGroup.java     | 34 +++++++++-
 .../ambari/server/topology/HostGroupImpl.java | 64 ++++++++++++++++---
 .../ambari/server/topology/HostRequest.java   |  4 +-
 .../server/topology/LogicalRequest.java       |  2 +-
 .../topology/RequiredPasswordValidator.java   |  2 +-
 .../server/upgrade/UpgradeCatalog221.java     | 12 ++++
 .../resources/Ambari-DDL-Derby-CREATE.sql     |  1 +
 .../resources/Ambari-DDL-MySQL-CREATE.sql     |  1 +
 .../resources/Ambari-DDL-Oracle-CREATE.sql    |  1 +
 .../resources/Ambari-DDL-Postgres-CREATE.sql  |  1 +
 .../Ambari-DDL-Postgres-EMBEDDED-CREATE.sql   |  1 +
 .../Ambari-DDL-SQLAnywhere-CREATE.sql         |  1 +
 .../resources/Ambari-DDL-SQLServer-CREATE.sql |  1 +
 .../render/ClusterBlueprintRendererTest.java  |  7 +-
 .../StackAdvisorBlueprintProcessorTest.java   |  8 +--
 .../BlueprintConfigurationProcessorTest.java  |  8 ++-
 .../internal/ExportBlueprintRequestTest.java  |  2 +-
 .../HostGroupComponentEntityTest.java         |  8 +++
 .../server/topology/BlueprintFactoryTest.java |  4 +-
 .../server/topology/BlueprintImplTest.java    | 40 +++++++-----
 .../topology/BlueprintValidatorImplTest.java  |  6 +-
 .../ClusterInstallWithoutStartTest.java       |  6 +-
 .../topology/ClusterTopologyImplTest.java     | 18 +++---
 .../RequiredPasswordValidatorTest.java        |  4 +-
 .../server/topology/TopologyManagerTest.java  |  4 +-
 .../server/upgrade/UpgradeCatalog221Test.java | 28 ++++----
 42 files changed, 401 insertions(+), 103 deletions(-)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ProvisionAction.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/topology/Component.java

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/query/render/ClusterBlueprintRenderer.java b/ambari-server/src/main/java/org/apache/ambari/server/api/query/render/ClusterBlueprintRenderer.java
index 3705ceb505..1a9ea9172b 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/query/render/ClusterBlueprintRenderer.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/query/render/ClusterBlueprintRenderer.java
@@ -49,6 +49,7 @@ import org.apache.ambari.server.state.SecurityType;
 import org.apache.ambari.server.topology.AmbariContext;
 import org.apache.ambari.server.topology.ClusterTopology;
 import org.apache.ambari.server.topology.ClusterTopologyImpl;
import org.apache.ambari.server.topology.Component;
 import org.apache.ambari.server.topology.Configuration;
 import org.apache.ambari.server.topology.HostGroup;
 import org.apache.ambari.server.topology.HostGroupInfo;
@@ -325,10 +326,10 @@ public class ClusterBlueprintRenderer extends BaseRenderer implements Renderer {
    */
   private List<Map<String, String>> processHostGroupComponents(HostGroup group) {
     List<Map<String, String>> listHostGroupComponents = new ArrayList<Map<String, String>>();
    for (String component : group.getComponents()) {
    for (Component component : group.getComponents()) {
       Map<String, String> mapComponentProperties = new HashMap<String, String>();
       listHostGroupComponents.add(mapComponentProperties);
      mapComponentProperties.put("name", component);
      mapComponentProperties.put("name", component.getName());
     }
     return listHostGroupComponents;
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorBlueprintProcessor.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorBlueprintProcessor.java
index d57c17d718..337ad06377 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorBlueprintProcessor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorBlueprintProcessor.java
@@ -105,7 +105,7 @@ public class StackAdvisorBlueprintProcessor {
   private Map<String, Set<String>> gatherHostGroupComponents(ClusterTopology clusterTopology) {
     Map<String, Set<String>> hgComponentsMap = Maps.newHashMap();
     for (Map.Entry<String, HostGroup> hgEnrty: clusterTopology.getBlueprint().getHostGroups().entrySet()) {
      hgComponentsMap.put(hgEnrty.getKey(), Sets.newCopyOnWriteArraySet(hgEnrty.getValue().getComponents()));
      hgComponentsMap.put(hgEnrty.getKey(), Sets.newCopyOnWriteArraySet(hgEnrty.getValue().getComponentNames()));
     }
     return hgComponentsMap;
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintResourceProvider.java
index f3100b5aa1..a4b2aaabde 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintResourceProvider.java
@@ -92,6 +92,7 @@ public class BlueprintResourceProvider extends AbstractControllerResourceProvide
   // Host Group Components
   public static final String COMPONENT_PROPERTY_ID ="components";
   public static final String COMPONENT_NAME_PROPERTY_ID ="name";
  public static final String COMPONENT_PROVISION_ACTION_PROPERTY_ID = "provision_action";
 
   // Configurations
   public static final String CONFIGURATION_PROPERTY_ID = "configurations";
@@ -305,6 +306,11 @@ public class BlueprintResourceProvider extends AbstractControllerResourceProvide
       for (HostGroupComponentEntity component : components) {
         Map<String, String> mapComponentProps = new HashMap<String, String>();
         mapComponentProps.put(COMPONENT_NAME_PROPERTY_ID, component.getName());

        if (component.getProvisionAction() != null) {
          mapComponentProps.put(COMPONENT_PROVISION_ACTION_PROPERTY_ID, component.getProvisionAction().toString());
        }

         listComponentProps.add(mapComponentProps);
       }
       mapGroupProps.put(COMPONENT_PROPERTY_ID, listComponentProps);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ExportBlueprintRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ExportBlueprintRequest.java
index 8c8b89dfa7..4d8e56ffd2 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ExportBlueprintRequest.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ExportBlueprintRequest.java
@@ -29,6 +29,7 @@ import org.apache.ambari.server.state.DesiredConfig;
 import org.apache.ambari.server.state.HostConfig;
 import org.apache.ambari.server.topology.Blueprint;
 import org.apache.ambari.server.topology.BlueprintImpl;
import org.apache.ambari.server.topology.Component;
 import org.apache.ambari.server.topology.Configuration;
 import org.apache.ambari.server.topology.HostGroup;
 import org.apache.ambari.server.topology.HostGroupImpl;
@@ -130,7 +131,14 @@ public class ExportBlueprintRequest implements TopologyRequest {
 
     Collection<HostGroup> hostGroups = new ArrayList<HostGroup>();
     for (ExportedHostGroup exportedHostGroup : exportedHostGroups) {
      hostGroups.add(new HostGroupImpl(exportedHostGroup.getName(), bpName, stack, exportedHostGroup.getComponents(),

      // create Component using component name
      List<Component> componentList = new ArrayList<Component>();
      for (String component : exportedHostGroup.getComponents()) {
        componentList.add(new Component(component));
      }

      hostGroups.add(new HostGroupImpl(exportedHostGroup.getName(), bpName, stack, componentList,
           exportedHostGroup.getConfiguration(), String.valueOf(exportedHostGroup.getCardinality())));
     }
     blueprint = new BlueprintImpl(bpName, hostGroups, stack, configuration, null);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
index 194d75fdc3..a2a58e8cf0 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
@@ -392,7 +392,16 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
     return requestStages.getRequestStatusResponse();
   }
 

  // TODO, revisit this extra method, that appears to be used during Add Hosts
  // TODO, How do we determine the component list for INSTALL_ONLY during an Add Hosts operation? rwn
   public RequestStatusResponse start(String cluster, String hostName) throws  SystemException,
    UnsupportedPropertyException, NoSuchParentResourceException {

    return this.start(cluster, hostName, Collections.<String>emptySet());
  }

  public RequestStatusResponse start(String cluster, String hostName, Collection<String> installOnlyComponents) throws  SystemException,
       UnsupportedPropertyException, NoSuchParentResourceException {
 
     Map<String, String> requestInfo = new HashMap<String, String>();
@@ -416,9 +425,34 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
       Predicate notClientPredicate = new NotPredicate(new ClientComponentPredicate());
       Predicate clusterAndClientPredicate = new AndPredicate(clusterPredicate, notClientPredicate);
       Predicate hostAndStatePredicate = new AndPredicate(installedStatePredicate, hostPredicate);
      Predicate startPredicate = new AndPredicate(clusterAndClientPredicate, hostAndStatePredicate);
      Predicate startPredicate;

      if (installOnlyComponents.isEmpty()) {
        // all installed components should be started
        startPredicate = new AndPredicate(clusterAndClientPredicate, hostAndStatePredicate);
        LOG.info("Starting all non-client components on host: " + hostName);
      } else {
        // any INSTALL_ONLY components should not be started
        List<Predicate> listOfComponentPredicates =
          new ArrayList<Predicate>();

        for (String installOnlyComponent : installOnlyComponents) {
          Predicate componentNameEquals = new EqualsPredicate<String>(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, installOnlyComponent);
          // create predicate to filter out the install only component
          listOfComponentPredicates.add(new NotPredicate(componentNameEquals));
        }

        Predicate[] arrayOfInstallOnlyPredicates = new Predicate[listOfComponentPredicates.size()];
        // aggregate Predicate of all INSTALL_ONLY component names
        Predicate installOnlyComponentsPredicate = new AndPredicate(listOfComponentPredicates.toArray(arrayOfInstallOnlyPredicates));

        // start predicate must now include the INSTALL_ONLY component predicates, in
        // order to filter out those components for START attempts
        startPredicate = new AndPredicate(clusterAndClientPredicate, hostAndStatePredicate, installOnlyComponentsPredicate);
        LOG.info("Starting all non-client components on host: " + hostName + ", except for the INSTALL_ONLY components specified: " + installOnlyComponents);
      }

 
      LOG.info("Starting all non-client components on host: " + hostName);
       requestStages = doUpdateResources(null, startRequest, startPredicate, true);
       notifyUpdate(Resource.Type.HostComponent, startRequest, startPredicate);
       try {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ProvisionAction.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ProvisionAction.java
new file mode 100644
index 0000000000..9874c5ec6e
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ProvisionAction.java
@@ -0,0 +1,25 @@
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

package org.apache.ambari.server.controller.internal;


public enum ProvisionAction {
  INSTALL_ONLY,     // Skip Start
  INSTALL_AND_START // Default action
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ProvisionClusterRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ProvisionClusterRequest.java
index 7b1de26e8a..6a11b26229 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ProvisionClusterRequest.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ProvisionClusterRequest.java
@@ -99,11 +99,6 @@ public class ProvisionClusterRequest extends BaseClusterRequest {
    */
   public static final String PROVISION_ACTION_PROPERTY = "provision_action";
 
  public enum ProvisionAction {
    INSTALL_ONLY,     // Skip Start
    INSTALL_AND_START // Default action
  }

   /**
    * configuration factory
    */
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntity.java
index 984c5494aa..046bbd8827 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntity.java
@@ -47,6 +47,10 @@ public class HostGroupComponentEntity {
   @Column(name = "name", nullable = false, insertable = true, updatable = false)
   private String name;
 
  @Id
  @Column(name = "provision_action", nullable = true, insertable = true, updatable = false)
  private String provisionAction;

   @ManyToOne
   @JoinColumns({
       @JoinColumn(name = "hostgroup_name", referencedColumnName = "name", nullable = false),
@@ -126,4 +130,24 @@ public class HostGroupComponentEntity {
   public void setBlueprintName(String blueprintName) {
     this.blueprintName = blueprintName;
   }

  /**
   * Get the provision action associated with this
   *   component.
   *
   * @return provision action
   */
  public String getProvisionAction() {
    return provisionAction;
  }

  /**
   * Set the provision action associated with this
   *   component.
   *
   * @param provisionAction action associated with the component (example: INSTALL_ONLY, INSTALL_AND_START)
   */
  public void setProvisionAction(String provisionAction) {
    this.provisionAction = provisionAction;
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityPK.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityPK.java
index fb9011b783..0e97346e68 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityPK.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityPK.java
@@ -38,6 +38,10 @@ public class HostGroupComponentEntityPK {
   @Column(name = "name", nullable = false, insertable = true, updatable = false, length = 100)
   private String name;
 
  @Id
  @Column(name = "provision_action", nullable = true, insertable = true, updatable = false, length = 100)
  private String provisionAction;

   /**
    * Get the name of the associated host group.
    *
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java
index 6bfee93919..87225adc0a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java
@@ -338,9 +338,9 @@ public class AmbariContext {
     }
   }
 
  public RequestStatusResponse startHost(String hostName, String clusterName) {
  public RequestStatusResponse startHost(String hostName, String clusterName, Collection<String> installOnlyComponents) {
     try {
      return getHostComponentResourceProvider().start(clusterName, hostName);
      return getHostComponentResourceProvider().start(clusterName, hostName, installOnlyComponents);
     } catch (Exception e) {
       e.printStackTrace();
       throw new RuntimeException("START Host request submission failed: " + e, e);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintFactory.java
index b8ce74907b..cca28ca152 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintFactory.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintFactory.java
@@ -24,6 +24,7 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.ObjectNotFoundException;
 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.ProvisionAction;
 import org.apache.ambari.server.controller.internal.Stack;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
 import org.apache.ambari.server.orm.dao.BlueprintDAO;
@@ -58,6 +59,7 @@ public class BlueprintFactory {
   // Host Group Components
   protected static final String COMPONENT_PROPERTY_ID ="components";
   protected static final String COMPONENT_NAME_PROPERTY_ID ="name";
  protected static final String COMPONENT_PROVISION_ACTION_PROPERTY_ID = "provision_action";
 
   // Configurations
   protected static final String CONFIGURATION_PROPERTY_ID = "configurations";
@@ -144,7 +146,7 @@ public class BlueprintFactory {
       Collection<Map<String, String>> configProps = (Collection<Map<String, String>>)
           hostGroupProperties.get(CONFIGURATION_PROPERTY_ID);
 
      Collection<String> components = processHostGroupComponents(stack, hostGroupName, componentProps);
      Collection<Component> components = processHostGroupComponents(stack, hostGroupName, componentProps);
       Configuration configuration = configFactory.getConfiguration(configProps);
       String cardinality = String.valueOf(hostGroupProperties.get(HOST_GROUP_CARDINALITY_PROPERTY_ID));
 
@@ -155,13 +157,13 @@ public class BlueprintFactory {
     return hostGroups;
   }
 
  private Collection<String> processHostGroupComponents(Stack stack, String groupName, HashSet<HashMap<String, String>>  componentProps) {
  private Collection<Component> processHostGroupComponents(Stack stack, String groupName, HashSet<HashMap<String, String>>  componentProps) {
     if (componentProps == null || componentProps.isEmpty()) {
       throw new IllegalArgumentException("Host group '" + groupName + "' must contain at least one component");
     }
 
     Collection<String> stackComponentNames = getAllStackComponents(stack);
    Collection<String> components = new ArrayList<String>();
    Collection<Component> components = new ArrayList<Component>();
 
     for (HashMap<String, String> componentProperties : componentProps) {
       String componentName = componentProperties.get(COMPONENT_NAME_PROPERTY_ID);
@@ -174,9 +176,16 @@ public class BlueprintFactory {
         throw new IllegalArgumentException("The component '" + componentName + "' in host group '" +
             groupName + "' is not valid for the specified stack");
       }
      components.add(componentName);
 
      String componentProvisionAction = componentProperties.get(COMPONENT_PROVISION_ACTION_PROPERTY_ID);
      if (componentProvisionAction != null) {
        //TODO, might want to add some validation here, to only accept value enum types, rwn
        components.add(new Component(componentName, ProvisionAction.valueOf(componentProvisionAction)));
      } else {
        components.add(new Component(componentName));
      }
     }

     return components;
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintImpl.java
index 88052b093b..bea036421c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintImpl.java
@@ -157,7 +157,7 @@ public class BlueprintImpl implements Blueprint {
   public Collection<HostGroup> getHostGroupsForComponent(String component) {
     Collection<HostGroup> resultGroups = new HashSet<HostGroup>();
     for (HostGroup group : hostGroups.values() ) {
      if (group.getComponents().contains(component)) {
      if (group.getComponentNames().contains(component)) {
         resultGroups.add(group);
       }
     }
@@ -358,18 +358,25 @@ public class BlueprintImpl implements Blueprint {
     * Create component entities and add to parent host group.
     */
   @SuppressWarnings("unchecked")
  private void createComponentEntities(HostGroupEntity group, Collection<String> components) {
  private void createComponentEntities(HostGroupEntity group, Collection<Component> components) {
     Collection<HostGroupComponentEntity> componentEntities = new HashSet<HostGroupComponentEntity>();
     group.setComponents(componentEntities);
 
    for (String component : components) {
    for (Component component : components) {
       HostGroupComponentEntity componentEntity = new HostGroupComponentEntity();
       componentEntities.add(componentEntity);
 
      componentEntity.setName(component);
      componentEntity.setName(component.getName());
       componentEntity.setBlueprintName(group.getBlueprintName());
       componentEntity.setHostGroupEntity(group);
       componentEntity.setHostGroupName(group.getName());

      // add provision action (if specified) to entity type
      // otherwise, just leave this column null (provision_action)
      if (component.getProvisionAction() != null) {
        componentEntity.setProvisionAction(component.getProvisionAction().toString());
      }

     }
     group.setComponents(componentEntities);
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintValidatorImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintValidatorImpl.java
index 1c293eeba5..432c6f886d 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintValidatorImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintValidatorImpl.java
@@ -116,7 +116,7 @@ public class BlueprintValidatorImpl implements BlueprintValidator {
       Map<String, Map<String, String>> operationalConfiguration = new HashMap<String, Map<String, String>>(clusterConfigurations);
 
       operationalConfiguration.putAll(hostGroup.getConfiguration().getProperties());
      for (String component : hostGroup.getComponents()) {
      for (String component : hostGroup.getComponentNames()) {
         //check that MYSQL_SERVER component is not available while hive is using existing db
         if (component.equals("MYSQL_SERVER")) {
           Map<String, String> hiveEnvConfig = clusterConfigurations.get("hive-env");
@@ -228,7 +228,7 @@ public class BlueprintValidatorImpl implements BlueprintValidator {
         new HashMap<String, Collection<DependencyInfo>>();
 
     Collection<String> blueprintServices = blueprint.getServices();
    Collection<String> groupComponents = group.getComponents();
    Collection<String> groupComponents = group.getComponentNames();
     for (String component : new HashSet<String>(groupComponents)) {
       Collection<DependencyInfo> dependenciesForComponent = stack.getDependenciesForComponent(component);
       for (DependencyInfo dependency : dependenciesForComponent) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterTopology.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterTopology.java
index c3c04dbb48..4e178c0193 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterTopology.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterTopology.java
@@ -19,8 +19,7 @@
 package org.apache.ambari.server.topology;
 
 import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest.ProvisionAction;
import org.apache.ambari.server.controller.internal.ProvisionAction;
 
 import java.util.Collection;
 import java.util.Map;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterTopologyImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterTopologyImpl.java
index e78300c898..91965f1f35 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterTopologyImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterTopologyImpl.java
@@ -21,8 +21,7 @@ package org.apache.ambari.server.topology;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest.ProvisionAction;
import org.apache.ambari.server.controller.internal.ProvisionAction;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -121,7 +120,7 @@ public class ClusterTopologyImpl implements ClusterTopology {
   public Collection<String> getHostGroupsForComponent(String component) {
     Collection<String> resultGroups = new ArrayList<String>();
     for (HostGroup group : getBlueprint().getHostGroups().values() ) {
      if (group.getComponents().contains(component)) {
      if (group.getComponentNames().contains(component)) {
         resultGroups.add(group.getName());
       }
     }
@@ -231,7 +230,15 @@ public class ClusterTopologyImpl implements ClusterTopology {
   @Override
   public RequestStatusResponse startHost(String hostName) {
     try {
      return ambariContext.startHost(hostName, ambariContext.getClusterName(getClusterId()));
      String hostGroupName = getHostGroupForHost(hostName);
      HostGroup hostGroup = this.blueprint.getHostGroup(hostGroupName);

      // get the set of components that are marked as INSTALL_ONLY
      // for this hostgroup
      Collection<String> installOnlyComponents =
        hostGroup.getComponentNames(ProvisionAction.INSTALL_ONLY);

      return ambariContext.startHost(hostName, ambariContext.getClusterName(getClusterId()), installOnlyComponents);
     } catch (AmbariException e) {
       LOG.error("Cannot get cluster name for clusterId = " + getClusterId(), e);
       throw new RuntimeException(e);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/Component.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/Component.java
new file mode 100644
index 0000000000..0dfad41a06
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/Component.java
@@ -0,0 +1,59 @@
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

package org.apache.ambari.server.topology;


import org.apache.ambari.server.controller.internal.ProvisionAction;

public class Component {

  private final String name;

  private final ProvisionAction provisionAction;

  public Component(String name) {
    this(name, null);
  }


  public Component(String name, ProvisionAction provisionAction) {
    this.name = name;
    this.provisionAction = provisionAction;
  }

  /**
   * Gets the name of this component
   *
   * @return component name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the provision action associated with this component.
   *
   * @return the provision action for this component, which
   *         may be null if the default action is to be used
   */
  public ProvisionAction getProvisionAction() {
    return this.provisionAction;
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/HostGroup.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/HostGroup.java
index 07e3e8846d..c0aec68125 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/HostGroup.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/HostGroup.java
@@ -18,6 +18,7 @@
 
 package org.apache.ambari.server.topology;
 
import org.apache.ambari.server.controller.internal.ProvisionAction;
 import org.apache.ambari.server.controller.internal.Stack;
 import org.apache.ambari.server.orm.entities.HostGroupEntity;
 import org.apache.ambari.server.state.DependencyInfo;
@@ -55,9 +56,28 @@ public interface HostGroup {
   /**
    * Get all of the host group components.
    *
   * @return collection of component names
   * @return collection of component instances
    */
  public Collection<String> getComponents();
  public Collection<Component> getComponents();

  /**
   * Get all of the host group component names
   *
   * @return collection of component names as String
   */
  public Collection<String> getComponentNames();

  /**
   * Get all host group component names for instances
   *   that have the specified provision action association.
   *
   * @param provisionAction the provision action that must be associated
   *                          with the component names returned
   *
   * @return collection of component names as String that are associated with
   *           the specified provision action
   */
  public Collection<String> getComponentNames(ProvisionAction provisionAction);
 
   /**
    * Get the host group components which belong to the specified service.
@@ -77,6 +97,16 @@ public interface HostGroup {
    */
   public boolean addComponent(String component);
 
  /**
   * Add a component to the host group, with the specified name
   *   and provision action.
   *
   * @param component  component name
   * @param provisionAction provision action for this component
   * @return
   */
  public boolean addComponent(String component, ProvisionAction provisionAction);

   /**
    * Determine if the host group contains a master component.
    *
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/HostGroupImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/HostGroupImpl.java
index b89e7e4e74..ddbe0b3d9e 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/HostGroupImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/HostGroupImpl.java
@@ -20,6 +20,7 @@
 package org.apache.ambari.server.topology;
 
 import com.google.gson.Gson;
import org.apache.ambari.server.controller.internal.ProvisionAction;
 import org.apache.ambari.server.controller.internal.Stack;
 import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
 import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
@@ -50,7 +51,7 @@ public class HostGroupImpl implements HostGroup {
   /**
    * components contained in the host group
    */
  private Collection<String> components = new HashSet<String>();
  private Map<String, Component> components = new HashMap<String, Component>();
 
   /**
    * map of service to components for the host group
@@ -78,21 +79,23 @@ public class HostGroupImpl implements HostGroup {
     parseConfigurations(entity);
   }
 
  public HostGroupImpl(String name, String bpName, Stack stack, Collection<String> components, Configuration configuration, String cardinality) {
  public HostGroupImpl(String name, String bpName, Stack stack, Collection<Component> components, Configuration configuration, String cardinality) {
     this.name = name;
     this.blueprintName = bpName;
     this.stack = stack;
 
     // process each component
    for (String component : components) {
      addComponent(component);
    for (Component component : components) {
      addComponent(component.getName(), component.getProvisionAction());
     }

     this.configuration = configuration;
     if (cardinality != null && ! cardinality.equals("null")) {
       this.cardinality = cardinality;
     }
   }
 

   @Override
   public String getName() {
     return name;
@@ -110,10 +113,29 @@ public class HostGroupImpl implements HostGroup {
   }
 
   @Override
  public Collection<String> getComponents() {
    return components;
  public Collection<Component> getComponents() {
    return components.values();
  }

  @Override
  public Collection<String> getComponentNames() {
    return components.keySet();
  }

  @Override
  public Collection<String> getComponentNames(ProvisionAction provisionAction) {
    Set<String> setOfComponentNames = new HashSet<String>();
    for (String componentName : components.keySet()) {
      Component component = components.get(componentName);
      if ( (component.getProvisionAction() != null) && (component.getProvisionAction() == provisionAction) ) {
        setOfComponentNames.add(componentName);
      }
    }

    return setOfComponentNames;
   }
 

   /**
    * Get the services which are deployed to this host group.
    *
@@ -133,7 +155,27 @@ public class HostGroupImpl implements HostGroup {
    */
   @Override
   public boolean addComponent(String component) {
    boolean added = components.add(component);
    return this.addComponent(component, null);
  }

  /**
   * Add a component with the specified provision action to the
   *   host group.
   *
   * @param component  component name
   * @param provisionAction provision action for this component
   *
   * @return true if component was added; false if component already existed
   */
  public boolean addComponent(String component, ProvisionAction provisionAction) {
    boolean added;
    if (!components.containsKey(component)) {
      components.put(component, new Component(component, provisionAction));
      added = true;
    } else {
      added = false;
    }

     if (stack.isMasterComponent(component)) {
       containsMasterComponent = true;
     }
@@ -207,7 +249,13 @@ public class HostGroupImpl implements HostGroup {
    */
   private void parseComponents(HostGroupEntity entity) {
     for (HostGroupComponentEntity componentEntity : entity.getComponents() ) {
      addComponent(componentEntity.getName());
      if (componentEntity.getProvisionAction() != null) {
        addComponent(componentEntity.getName(), ProvisionAction.valueOf(componentEntity.getProvisionAction()));
      } else {
        addComponent(componentEntity.getName());
      }


     }
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/HostRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/HostRequest.java
index 440638c07d..9eb514a3ee 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/HostRequest.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/HostRequest.java
@@ -43,7 +43,7 @@ import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 
import static org.apache.ambari.server.controller.internal.ProvisionClusterRequest.ProvisionAction.INSTALL_ONLY;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_ONLY;
 
 /**
  * Represents a set of requests to a single host such as install, start, etc.
@@ -194,7 +194,7 @@ public class HostRequest implements Comparable<HostRequest> {
 
     // lower level logical component level tasks which get mapped to physical tasks
     HostGroup hostGroup = getHostGroup();
    for (String component : hostGroup.getComponents()) {
    for (String component : hostGroup.getComponentNames()) {
       if (component == null || component.equals("AMBARI_SERVER")) {
         LOG.info("Skipping component {} when creating request\n", component);
         continue;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/LogicalRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/LogicalRequest.java
index bd9f2e07ba..ad7c8fd8d1 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/LogicalRequest.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/LogicalRequest.java
@@ -188,7 +188,7 @@ public class LogicalRequest extends Request {
           hostComponents = new HashSet<String>();
           hostComponentMap.put(host, hostComponents);
         }
        hostComponents.addAll(hostGroup.getComponents());
        hostComponents.addAll(hostGroup.getComponentNames());
       }
     }
     return hostComponentMap;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/RequiredPasswordValidator.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/RequiredPasswordValidator.java
index e26de3f9b7..98eaa40e91 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/RequiredPasswordValidator.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/RequiredPasswordValidator.java
@@ -81,7 +81,7 @@ public class RequiredPasswordValidator implements TopologyValidator {
       Stack stack = blueprint.getStack();
 
       HostGroup hostGroup = blueprint.getHostGroup(hostGroupName);
      for (String component : hostGroup.getComponents()) {
      for (String component : hostGroup.getComponentNames()) {
         //for now, AMBARI is not recognized as a service in Stacks
         if (component.equals("AMBARI_SERVER")) {
           continue;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog221.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog221.java
index 5cde24b0fc..9c947a1c87 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog221.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog221.java
@@ -27,6 +27,7 @@ import com.google.inject.Inject;
 import com.google.inject.Injector;
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
 import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
 import org.apache.ambari.server.orm.dao.DaoUtils;
 import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
@@ -76,6 +77,10 @@ public class UpgradeCatalog221 extends AbstractUpgradeCatalog {
   private static final String OLD_DEFAULT_HADOOP_CONFIG_PATH = "/etc/hadoop/conf";
   private static final String NEW_DEFAULT_HADOOP_CONFIG_PATH = "{{hadoop_conf_dir}}";
 
  private static final String BLUEPRINT_HOSTGROUP_COMPONENT_TABLE_NAME = "hostgroup_component";
  private static final String BLUEPRINT_PROVISION_ACTION_COLUMN_NAME = "provision_action";


 
   // ----- Constructors ------------------------------------------------------
 
@@ -118,6 +123,13 @@ public class UpgradeCatalog221 extends AbstractUpgradeCatalog {
     dbAccessor.createIndex("idx_hrc_request_id", "host_role_command", "request_id");
     dbAccessor.createIndex("idx_rsc_request_id", "role_success_criteria", "request_id");
 
    executeBlueprintProvisionActionDDLUpdates();
  }

  private void executeBlueprintProvisionActionDDLUpdates() throws AmbariException, SQLException {
    // add provision_action column to the hostgroup_component table for Blueprints
    dbAccessor.addColumn(BLUEPRINT_HOSTGROUP_COMPONENT_TABLE_NAME, new DBAccessor.DBColumnInfo(BLUEPRINT_PROVISION_ACTION_COLUMN_NAME,
      String.class, 255, null, true));
   }
 
   @Override
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
index d93a3c2cb0..6eda26d46d 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
@@ -406,6 +406,7 @@ CREATE TABLE hostgroup_component (
   blueprint_name VARCHAR(255) NOT NULL,
   hostgroup_name VARCHAR(255) NOT NULL,
   name VARCHAR(255) NOT NULL,
  provision_action VARCHAR(255),
   PRIMARY KEY(blueprint_name, hostgroup_name, name));
 
 CREATE TABLE blueprint_configuration (
diff --git a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
index aa8ced131d..05f7c4c90d 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
@@ -414,6 +414,7 @@ CREATE TABLE hostgroup_component (
   blueprint_name VARCHAR(100) NOT NULL,
   hostgroup_name VARCHAR(100) NOT NULL,
   name VARCHAR(100) NOT NULL,
  provision_action VARCHAR(100),
   PRIMARY KEY(blueprint_name, hostgroup_name, name));
 
 CREATE TABLE blueprint_configuration (
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
index b53434449d..59fd773159 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
@@ -405,6 +405,7 @@ CREATE TABLE hostgroup_component (
   blueprint_name VARCHAR2(255) NOT NULL,
   hostgroup_name VARCHAR2(255) NOT NULL,
   name VARCHAR2(255) NOT NULL,
  provision_action VARCHAR2(255),
   PRIMARY KEY(blueprint_name, hostgroup_name, name));
 
 CREATE TABLE blueprint_configuration (
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
index 941fc6e6ec..24bf962093 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
@@ -406,6 +406,7 @@ CREATE TABLE hostgroup_component (
   blueprint_name VARCHAR(255) NOT NULL,
   hostgroup_name VARCHAR(255) NOT NULL,
   name VARCHAR(255) NOT NULL,
  provision_action VARCHAR(255),
   PRIMARY KEY(blueprint_name, hostgroup_name, name));
 
 CREATE TABLE blueprint_configuration (
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Postgres-EMBEDDED-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Postgres-EMBEDDED-CREATE.sql
index dd517f8e4f..705a241dbd 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Postgres-EMBEDDED-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Postgres-EMBEDDED-CREATE.sql
@@ -453,6 +453,7 @@ CREATE TABLE ambari.hostgroup_component (
   blueprint_name VARCHAR(255) NOT NULL,
   hostgroup_name VARCHAR(255) NOT NULL,
   name VARCHAR(255) NOT NULL,
  provision_action VARCHAR(255),
   PRIMARY KEY(blueprint_name, hostgroup_name, name));
 
 CREATE TABLE ambari.blueprint_configuration (
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
index f837f9ed0b..3bb7faf01c 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
@@ -403,6 +403,7 @@ CREATE TABLE hostgroup_component (
   blueprint_name VARCHAR(255) NOT NULL,
   hostgroup_name VARCHAR(255) NOT NULL,
   name VARCHAR(255) NOT NULL,
  provision_action VARCHAR(255),
   PRIMARY KEY(blueprint_name, hostgroup_name, name));
 
 CREATE TABLE blueprint_configuration (
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
index 239d27ef50..89acb96611 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
@@ -459,6 +459,7 @@ CREATE TABLE hostgroup_component (
   blueprint_name VARCHAR(255) NOT NULL,
   hostgroup_name VARCHAR(255) NOT NULL,
   NAME VARCHAR(255) NOT NULL,
  provision_action VARCHAR(255),
   PRIMARY KEY CLUSTERED (
     blueprint_name,
     hostgroup_name,
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/api/query/render/ClusterBlueprintRendererTest.java b/ambari-server/src/test/java/org/apache/ambari/server/api/query/render/ClusterBlueprintRendererTest.java
index 522d902d08..1fe48df062 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/api/query/render/ClusterBlueprintRendererTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/api/query/render/ClusterBlueprintRendererTest.java
@@ -40,6 +40,7 @@ import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.ambari.server.topology.AmbariContext;
 import org.apache.ambari.server.topology.Blueprint;
 import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Component;
 import org.apache.ambari.server.topology.Configuration;
 import org.apache.ambari.server.topology.HostGroup;
 import org.apache.ambari.server.topology.HostGroupInfo;
@@ -115,10 +116,10 @@ public class ClusterBlueprintRendererTest {
     clusterAttributeProps.put("propertyOne", "true");
     clusterTypeAttributes.put("final", clusterAttributeProps);
 
    Collection<String> group1Components = Arrays.asList(
        "JOBTRACKER", "TASKTRACKER", "NAMENODE", "DATANODE", "AMBARI_SERVER");
    Collection<Component> group1Components = Arrays.asList(
        new Component("JOBTRACKER"), new Component("TASKTRACKER"), new Component("NAMENODE"), new Component("DATANODE"), new Component("AMBARI_SERVER"));
 
    Collection<String> group2Components = Arrays.asList("TASKTRACKER", "DATANODE");
    Collection<Component> group2Components = Arrays.asList(new Component("TASKTRACKER"), new Component("DATANODE"));
 
     Map<String, Configuration> hostGroupConfigs = new HashMap<String, Configuration>();
     hostGroupConfigs.put("host_group_1", emptyConfiguration);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorBlueprintProcessorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorBlueprintProcessorTest.java
index 514e6ab1ec..d5531a666b 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorBlueprintProcessorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorBlueprintProcessorTest.java
@@ -20,9 +20,9 @@ package org.apache.ambari.server.api.services.stackadvisor;
 
 import com.google.common.collect.Maps;
 import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.topology.Component;
 import org.apache.ambari.server.topology.Configuration;
 import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupImpl;
 import static org.easymock.EasyMock.anyObject;
 import static org.easymock.EasyMock.createMock;
 import static org.easymock.EasyMock.expect;
@@ -81,7 +81,7 @@ public class StackAdvisorBlueprintProcessorTest {
     expect(stack.getName()).andReturn("HDP").anyTimes();
     expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
     expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(hostGroup.getComponents()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
     expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(createRecommendationResponse());
     expect(configuration.getFullProperties()).andReturn(createProps());
 
@@ -108,7 +108,7 @@ public class StackAdvisorBlueprintProcessorTest {
     expect(stack.getVersion()).andReturn("2.3").anyTimes();
     expect(stack.getName()).andReturn("HDP").anyTimes();
     expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(hostGroup.getComponents()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
     expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
     expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andThrow(new StackAdvisorException("ex"));
     expect(configuration.getFullProperties()).andReturn(createProps());
@@ -136,7 +136,7 @@ public class StackAdvisorBlueprintProcessorTest {
     expect(stack.getName()).andReturn("HDP").anyTimes();
     expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
     expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(hostGroup.getComponents()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
     expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(new RecommendationResponse());
     expect(configuration.getFullProperties()).andReturn(createProps());
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
index 0384b45aa0..ab6913ee7b 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
@@ -32,6 +32,7 @@ import org.apache.ambari.server.topology.Blueprint;
 import org.apache.ambari.server.topology.Cardinality;
 import org.apache.ambari.server.topology.ClusterTopology;
 import org.apache.ambari.server.topology.ClusterTopologyImpl;
import org.apache.ambari.server.topology.Component;
 import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
 import org.apache.ambari.server.topology.Configuration;
 import org.apache.ambari.server.topology.HostGroup;
@@ -6393,9 +6394,14 @@ public class BlueprintConfigurationProcessorTest {
       //todo: HG configs
       groupInfo.setConfiguration(hostGroup.configuration);
 
      List<Component> componentList = new ArrayList<Component>();
      for (String componentName : hostGroup.components) {
        componentList.add(new Component(componentName));
      }

       //create host group which is set on topology
       allHostGroups.put(hostGroup.name, new HostGroupImpl(hostGroup.name, "test-bp", stack,
          hostGroup.components, EMPTY_CONFIG, "1"));
          componentList, EMPTY_CONFIG, "1"));
 
       hostGroupInfo.put(hostGroup.name, groupInfo);
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ExportBlueprintRequestTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ExportBlueprintRequestTest.java
index 94ba90f654..7f37814e65 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ExportBlueprintRequestTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/ExportBlueprintRequestTest.java
@@ -129,7 +129,7 @@ public class ExportBlueprintRequestTest {
     String hg1Name = null;
     String hg2Name = null;
     for (HostGroup group : hostGroups.values()) {
      Collection<String> components = group.getComponents();
      Collection<String> components = group.getComponentNames();
       if (components.containsAll(host1ComponentsList)) {
         assertEquals(host1ComponentsList.size(), components.size());
         assertEquals("1", group.getCardinality());
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityTest.java b/ambari-server/src/test/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityTest.java
index c0efd0de1b..f3104e8642 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityTest.java
@@ -55,4 +55,12 @@ public class HostGroupComponentEntityTest {
     entity.setBlueprintName("foo");
     assertEquals("foo", entity.getBlueprintName());
   }

  @Test
  public void testSetGetProvisionAction() {
    HostGroupComponentEntity entity = new HostGroupComponentEntity();
    entity.setProvisionAction("INSTALL_ONLY");
    assertEquals("INSTALL_ONLY", entity.getProvisionAction());
  }

 }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintFactoryTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintFactoryTest.java
index 3a3b6dca8f..5e2085b7df 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintFactoryTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintFactoryTest.java
@@ -130,7 +130,7 @@ public class BlueprintFactoryTest {
     HostGroup group1 = hostGroups.get("group1");
     assertEquals("group1", group1.getName());
     assertEquals("1", group1.getCardinality());
    Collection<String> components = group1.getComponents();
    Collection<String> components = group1.getComponentNames();
     assertEquals(2, components.size());
     assertTrue(components.contains("component1"));
     assertTrue(components.contains("component2"));
@@ -147,7 +147,7 @@ public class BlueprintFactoryTest {
     HostGroup group2 = hostGroups.get("group2");
     assertEquals("group2", group2.getName());
     assertEquals("2", group2.getCardinality());
    components = group2.getComponents();
    components = group2.getComponentNames();
     assertEquals(1, components.size());
     assertTrue(components.contains("component1"));
     services = group2.getServices();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintImplTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintImplTest.java
index 3addfc4fa1..0b06eb8bd9 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintImplTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintImplTest.java
@@ -23,16 +23,14 @@ import org.apache.ambari.server.orm.entities.BlueprintEntity;
 import org.apache.ambari.server.state.SecurityType;
 import org.junit.Test;
 
import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.*;
 import static org.junit.Assert.assertTrue;
 import static org.junit.Assert.fail;
 
@@ -56,13 +54,13 @@ public class BlueprintImplTest {
 
     Stack stack = createNiceMock(Stack.class);
 
    HostGroup group1 = createNiceMock(HostGroup.class);
    HostGroup group2 = createNiceMock(HostGroup.class);
    HostGroup group1 = createMock(HostGroup.class);
    HostGroup group2 = createMock(HostGroup.class);
     Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
     hostGroups.add(group1);
     hostGroups.add(group2);
 
    Set<String> group1Components = new HashSet<String>();
    Collection<String> group1Components = new HashSet<String>();
     group1Components.add("c1");
     group1Components.add("c2");
 
@@ -95,11 +93,15 @@ public class BlueprintImplTest {
 
     expect(group1.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
     expect(group1.getName()).andReturn("group1").anyTimes();
    expect(group1.getComponents()).andReturn(group1Components).atLeastOnce();
    expect(group1.getComponentNames()).andReturn(group1Components).atLeastOnce();
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c2"))).atLeastOnce();
 
     expect(group2.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
     expect(group2.getName()).andReturn("group2").anyTimes();
    expect(group2.getComponents()).andReturn(group2Components).atLeastOnce();
    expect(group2.getComponentNames()).andReturn(group2Components).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c3"))).atLeastOnce();
 
     replay(stack, group1, group2);
 
@@ -143,11 +145,11 @@ public class BlueprintImplTest {
     hostGroups.add(group1);
     hostGroups.add(group2);
 
    Set<String> group1Components = new HashSet<String>();
    Collection<String> group1Components = new HashSet<String>();
     group1Components.add("c1");
     group1Components.add("c2");
 
    Set<String> group2Components = new HashSet<String>();
    Collection<String> group2Components = new HashSet<String>();
     group2Components.add("c1");
     group2Components.add("c3");
 
@@ -176,11 +178,11 @@ public class BlueprintImplTest {
 
     expect(group1.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
     expect(group1.getName()).andReturn("group1").anyTimes();
    expect(group1.getComponents()).andReturn(group1Components).atLeastOnce();
    expect(group1.getComponentNames()).andReturn(group1Components).atLeastOnce();
 
     expect(group2.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
     expect(group2.getName()).andReturn("group2").anyTimes();
    expect(group2.getComponents()).andReturn(group2Components).atLeastOnce();
    expect(group2.getComponentNames()).andReturn(group2Components).atLeastOnce();
 
     replay(stack, group1, group2);
 
@@ -214,8 +216,8 @@ public class BlueprintImplTest {
 
     Stack stack = createNiceMock(Stack.class);
 
    HostGroup group1 = createNiceMock(HostGroup.class);
    HostGroup group2 = createNiceMock(HostGroup.class);
    HostGroup group1 = createMock(HostGroup.class);
    HostGroup group2 = createMock(HostGroup.class);
     Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
     hostGroups.add(group1);
     hostGroups.add(group2);
@@ -258,10 +260,14 @@ public class BlueprintImplTest {
 
     expect(group1.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
     expect(group1.getName()).andReturn("group1").anyTimes();
    expect(group1.getComponents()).andReturn(group1Components).atLeastOnce();
    expect(group1.getComponentNames()).andReturn(group1Components).atLeastOnce();
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c2"))).atLeastOnce();
 
     expect(group2.getName()).andReturn("group2").anyTimes();
    expect(group2.getComponents()).andReturn(group2Components).atLeastOnce();
    expect(group2.getComponentNames()).andReturn(group2Components).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c3"))).atLeastOnce();
 
     // Blueprint config
     Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintValidatorImplTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintValidatorImplTest.java
index 304cded21d..f78d86dccf 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintValidatorImplTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/BlueprintValidatorImplTest.java
@@ -75,8 +75,8 @@ public class BlueprintValidatorImplTest{
     expect(blueprint.getHostGroups()).andReturn(hostGroups).anyTimes();
     expect(blueprint.getServices()).andReturn(services).anyTimes();
 
    expect(group1.getComponents()).andReturn(group1Components).anyTimes();
    expect(group2.getComponents()).andReturn(group2Components).anyTimes();
    expect(group1.getComponentNames()).andReturn(group1Components).anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).anyTimes();
 
     expect(stack.getDependenciesForComponent("component1")).andReturn(dependencies1).anyTimes();
     expect(stack.getDependenciesForComponent("component2")).andReturn(dependencies1).anyTimes();
@@ -98,7 +98,7 @@ public class BlueprintValidatorImplTest{
   @Test
   public void testValidateTopology_basic() throws Exception {
     group1Components.add("component1");
    group1Components.add("component2");
    group1Components.add("component1");
 
     services.addAll(Arrays.asList("service1", "service2"));
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartTest.java
index 1354a72cde..dd66b1bfba 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterInstallWithoutStartTest.java
@@ -60,7 +60,7 @@ import java.util.List;
 import java.util.Map;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Future;
import static org.apache.ambari.server.controller.internal.ProvisionClusterRequest.ProvisionAction.INSTALL_ONLY;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_ONLY;
 import static org.easymock.EasyMock.anyLong;
 import static org.easymock.EasyMock.anyObject;
 import static org.easymock.EasyMock.anyString;
@@ -269,7 +269,7 @@ public class ClusterInstallWithoutStartTest {
     expect(group1.getBlueprintName()).andReturn(BLUEPRINT_NAME).anyTimes();
     expect(group1.getCardinality()).andReturn("test cardinality").anyTimes();
     expect(group1.containsMasterComponent()).andReturn(true).anyTimes();
    expect(group1.getComponents()).andReturn(group1Components).anyTimes();
    expect(group1.getComponentNames()).andReturn(group1Components).anyTimes();
     expect(group1.getComponents("service1")).andReturn(group1ServiceComponents.get("service1")).anyTimes();
     expect(group1.getComponents("service2")).andReturn(group1ServiceComponents.get("service1")).anyTimes();
     expect(group1.getConfiguration()).andReturn(topoGroup1Config).anyTimes();
@@ -280,7 +280,7 @@ public class ClusterInstallWithoutStartTest {
     expect(group2.getBlueprintName()).andReturn(BLUEPRINT_NAME).anyTimes();
     expect(group2.getCardinality()).andReturn("test cardinality").anyTimes();
     expect(group2.containsMasterComponent()).andReturn(false).anyTimes();
    expect(group2.getComponents()).andReturn(group2Components).anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).anyTimes();
     expect(group2.getComponents("service1")).andReturn(group2ServiceComponents.get("service1")).anyTimes();
     expect(group2.getComponents("service2")).andReturn(group2ServiceComponents.get("service2")).anyTimes();
     expect(group2.getConfiguration()).andReturn(topoGroup2Config).anyTimes();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterTopologyImplTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterTopologyImplTest.java
index 08aa3d3b50..7c68482987 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterTopologyImplTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterTopologyImplTest.java
@@ -98,15 +98,15 @@ public class ClusterTopologyImplTest {
     hostGroupMap.put("group3", group3);
     hostGroupMap.put("group4", group4);
 
    Set<String> group1Components = new HashSet<String>();
    group1Components.add("component1");
    group1Components.add("component2");
    Set<String> group2Components = new HashSet<String>();
    group2Components.add("component3");
    Set<String> group3Components = new HashSet<String>();
    group3Components.add("component4");
    Set<String> group4Components = new HashSet<String>();
    group4Components.add("component5");
    Set<Component> group1Components = new HashSet<Component>();
    group1Components.add(new Component("component1"));
    group1Components.add(new Component("component2"));
    Set<Component> group2Components = new HashSet<Component>();
    group2Components.add(new Component("component3"));
    Set<Component> group3Components = new HashSet<Component>();
    group3Components.add(new Component("component4"));
    Set<Component> group4Components = new HashSet<Component>();
    group4Components.add(new Component("component5"));
 
     expect(blueprint.getHostGroups()).andReturn(hostGroupMap).anyTimes();
     expect(blueprint.getHostGroup("group1")).andReturn(group1).anyTimes();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/RequiredPasswordValidatorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/RequiredPasswordValidatorTest.java
index f4ded703d4..e8a2ff5912 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/RequiredPasswordValidatorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/RequiredPasswordValidatorTest.java
@@ -130,8 +130,8 @@ public class RequiredPasswordValidatorTest {
     expect(blueprint.getHostGroup("group2")).andReturn(group2).anyTimes();
     expect(blueprint.getStack()).andReturn(stack).anyTimes();
 
    expect(group1.getComponents()).andReturn(group1Components).anyTimes();
    expect(group2.getComponents()).andReturn(group2Components).anyTimes();
    expect(group1.getComponentNames()).andReturn(group1Components).anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).anyTimes();
     expect(group1.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();
     expect(group1.getComponents("service2")).andReturn(Arrays.asList("component3")).anyTimes();
     expect(group1.getComponents("service3")).andReturn(Collections.<String>emptySet()).anyTimes();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/TopologyManagerTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/TopologyManagerTest.java
index 47169f488d..7810f92efb 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/TopologyManagerTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/TopologyManagerTest.java
@@ -142,8 +142,8 @@ public class TopologyManagerTest {
   private HostGroupInfo group2Info = new HostGroupInfo("group2");
   private Map<String, HostGroupInfo> groupInfoMap = new HashMap<String, HostGroupInfo>();
 
  private Collection<String> group1Components = Arrays.asList("component1", "component2", "component3");
  private Collection<String> group2Components = Arrays.asList("component3", "component4");
  private Collection<Component> group1Components = Arrays.asList(new Component("component1"), new Component("component2"), new Component("component3"));
  private Collection<Component> group2Components = Arrays.asList(new Component("component3"), new Component("component4"));
 
   private Map<String, Collection<String>> group1ServiceComponents = new HashMap<String, Collection<String>>();
   private Map<String, Collection<String>> group2ServiceComponents = new HashMap<String, Collection<String>>();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog221Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog221Test.java
index 319024b42c..49484c1032 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog221Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog221Test.java
@@ -59,20 +59,9 @@ import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.*;
 import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
 import static org.junit.Assert.assertTrue;
 
 public class UpgradeCatalog221Test {
@@ -114,6 +103,10 @@ public class UpgradeCatalog221Test {
     dbAccessor.createIndex(eq("idx_rsc_request_id"), eq("role_success_criteria"), eq("request_id"));
     expectLastCall().once();
 
    Capture<DBAccessor.DBColumnInfo> capturedHostGroupComponentProvisionColumn = EasyMock.newCapture();
    dbAccessor.addColumn(eq("hostgroup_component"), capture(capturedHostGroupComponentProvisionColumn));
    expectLastCall().once();

 
     replay(dbAccessor);
     Module module = new Module() {
@@ -128,6 +121,15 @@ public class UpgradeCatalog221Test {
     Injector injector = Guice.createInjector(module);
     UpgradeCatalog221 upgradeCatalog221 = injector.getInstance(UpgradeCatalog221.class);
     upgradeCatalog221.executeDDLUpdates();

    // verify that the column was added for provision_action to the hostgroup_component table
    assertEquals("Incorrect column name added", "provision_action", capturedHostGroupComponentProvisionColumn.getValue().getName());
    assertNull("Incorrect default value added", capturedHostGroupComponentProvisionColumn.getValue().getDefaultValue());
    assertEquals("Incorrect column type added", String.class, capturedHostGroupComponentProvisionColumn.getValue().getType());
    assertEquals("Incorrect column length added", 255, capturedHostGroupComponentProvisionColumn.getValue().getLength().intValue());
    assertTrue("Incorrect column nullable state added", capturedHostGroupComponentProvisionColumn.getValue().isNullable());


     verify(dbAccessor);
   }
 
- 
2.19.1.windows.1

