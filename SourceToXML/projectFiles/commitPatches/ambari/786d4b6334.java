From 786d4b63349d2c0f18d8a06a29fe1cc5598cf8fc Mon Sep 17 00:00:00 2001
From: Dmytro Grinenko <hapylestat@apache.org>
Date: Mon, 20 Nov 2017 07:37:02 +0200
Subject: [PATCH] AMBARI-22462 Remove hard-code from StackAdvisorCommand
 requests to another resources (dgrinenko)

--
 .../commands/StackAdvisorCommand.java             | 15 +++++++++++----
 .../server/controller/KerberosHelperImpl.java     |  6 +++---
 ...iceComponentConfigurationResourceProvider.java |  8 ++++----
 .../AbstractPrepareKerberosServerAction.java      |  6 ++++--
 .../PrepareKerberosIdentitiesServerAction.java    |  5 ++++-
 .../ambari/server/topology/AmbariContext.java     |  3 ++-
 .../ambari/server/topology/BlueprintFactory.java  |  3 ++-
 .../validators/RequiredPasswordValidator.java     |  3 ++-
 8 files changed, 32 insertions(+), 17 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java
index 17591ec7d0..1b89c4f9a7 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java
@@ -47,6 +47,7 @@ import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorResponse;
 import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
 import org.apache.ambari.server.controller.RootComponent;
 import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.ambari.server.utils.DateUtils;
@@ -77,6 +78,8 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
 
   private static final String GET_HOSTS_INFO_URI = "/api/v1/hosts"
       + "?fields=Hosts/*&Hosts/host_name.in(%s)";
  static final String LDAP_CONFIGURATION_PROPERTY = "ldap-configuration";

   private static final String GET_SERVICES_INFO_URI = "/api/v1/stacks/%s/versions/%s/"
       + "?fields=Versions/stack_name,Versions/stack_version,Versions/parent_stack_version"
       + ",services/StackServices/service_name,services/StackServices/service_version"
@@ -86,9 +89,14 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
       + ",services/configurations/dependencies/StackConfigurationDependency/dependency_name"
       + ",services/configurations/dependencies/StackConfigurationDependency/dependency_type,services/configurations/StackConfigurations/type"
       + "&services/StackServices/service_name.in(%s)";
  private static final String GET_AMBARI_LDAP_CONFIG_URI = "/api/v1/services/AMBARI/components/AMBARI_SERVER/configurations" +
      "?Configuration/category=ldap-configuration" +
      "&fields=Configuration/properties";

  private static final String GET_AMBARI_LDAP_CONFIG_URI = String.format("/api/v1/services/%s/components/%s/configurations?%s=%s&fields=%s",
    RootService.AMBARI.name(),
    RootComponent.AMBARI_SERVER.name(),
    RootServiceComponentConfigurationResourceProvider.CONFIGURATION_CATEGORY_PROPERTY_ID,
    LDAP_CONFIGURATION_PROPERTY,
    RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTIES_PROPERTY_ID);

   private static final String SERVICES_PROPERTY = "services";
   private static final String SERVICES_COMPONENTS_PROPERTY = "components";
   private static final String CONFIG_GROUPS_PROPERTY = "config-groups";
@@ -100,7 +108,6 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
   private static final String CHANGED_CONFIGURATIONS_PROPERTY = "changed-configurations";
   private static final String USER_CONTEXT_PROPERTY = "user-context";
   private static final String AMBARI_SERVER_CONFIGURATIONS_PROPERTY = "ambari-server-properties";
  static final String LDAP_CONFIGURATION_PROPERTY = "ldap-configuration";
 
   private File recommendationsDir;
   private String recommendationsArtifactsLifetime;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelperImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelperImpl.java
index 474c3357b0..ab85aa1d7c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelperImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelperImpl.java
@@ -467,8 +467,8 @@ public class KerberosHelperImpl implements KerberosHelper {
     // If Ambari is managing it own identities then add AMBARI to the set of installed service so
     // that its Kerberos descriptor entries will be included.
     if (createAmbariIdentities(existingConfigurations.get(KERBEROS_ENV))) {
      installedServices = new HashMap<String, Set<String>>(installedServices);
      installedServices.put("AMBARI", Collections.singleton("AMBARI_SERVER"));
      installedServices = new HashMap<>(installedServices);
      installedServices.put(RootService.AMBARI.name(), Collections.singleton(RootComponent.AMBARI_SERVER.name()));
     }
 
     // Create the context to use for filtering Kerberos Identities based on the state of the cluster
@@ -1547,7 +1547,7 @@ public class KerberosHelperImpl implements KerberosHelper {
                 keytabFileGroupName,
                 keytabFileGroupAccess,
                 Sets.newHashSet(Pair.of(hostId, Pair.of(evaluatedPrincipal, principalType))),
                serviceName.equalsIgnoreCase("AMBARI"),
                serviceName.equalsIgnoreCase(RootService.AMBARI.name()),
                 componentName.equalsIgnoreCase("AMBARI_SERVER_SELF")
             );
             if (resolvedKeytabs.containsKey(keytabFilePath)) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProvider.java
index ea9cf4ff48..78078652f0 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProvider.java
@@ -59,10 +59,10 @@ public class RootServiceComponentConfigurationResourceProvider extends AbstractA
 
   static final String RESOURCE_KEY = "Configuration";
 
  static final String CONFIGURATION_CATEGORY_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "category");
  static final String CONFIGURATION_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "properties");
  static final String CONFIGURATION_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "component_name");
  static final String CONFIGURATION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "service_name");
  public static final String CONFIGURATION_CATEGORY_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "category");
  public static final String CONFIGURATION_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "properties");
  public static final String CONFIGURATION_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "component_name");
  public static final String CONFIGURATION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "service_name");
 
   private static final Set<String> PROPERTIES;
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/AbstractPrepareKerberosServerAction.java b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/AbstractPrepareKerberosServerAction.java
index 4008620f04..b8affb4e19 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/AbstractPrepareKerberosServerAction.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/AbstractPrepareKerberosServerAction.java
@@ -33,6 +33,8 @@ import java.util.Set;
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.agent.CommandReport;
 import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.ServiceComponentHost;
@@ -192,11 +194,11 @@ public abstract class AbstractPrepareKerberosServerAction extends KerberosServer
               // component.
               String componentName = KerberosHelper.AMBARI_SERVER_KERBEROS_IDENTITY_NAME.equals(identity.getName())
                   ? "AMBARI_SERVER_SELF"
                  : "AMBARI_SERVER";
                  : RootComponent.AMBARI_SERVER.name();
 
               List<KerberosIdentityDescriptor> componentIdentities = Collections.singletonList(identity);
               kerberosHelper.addIdentities(kerberosIdentityDataFileWriter, componentIdentities,
                  identityFilter, StageUtils.getHostName(), ambariServerHostID(), "AMBARI", componentName, kerberosConfigurations, currentConfigurations,
                  identityFilter, StageUtils.getHostName(), ambariServerHostID(), RootService.AMBARI.name(), componentName, kerberosConfigurations, currentConfigurations,
                   resolvedKeytabs, realm);
               propertiesToIgnore = gatherPropertiesToIgnore(componentIdentities, propertiesToIgnore);
             }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/PrepareKerberosIdentitiesServerAction.java b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/PrepareKerberosIdentitiesServerAction.java
index b0fca8d07f..83a2106afd 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/PrepareKerberosIdentitiesServerAction.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/PrepareKerberosIdentitiesServerAction.java
@@ -30,6 +30,8 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.actionmanager.HostRoleStatus;
 import org.apache.ambari.server.agent.CommandReport;
 import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.ServiceComponentHost;
 import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
@@ -110,7 +112,8 @@ public class PrepareKerberosIdentitiesServerAction extends AbstractPrepareKerber
     if (serviceComponentFilter != null) {
       // If we are including the Ambari identity; then ensure that if a service/component filter is set,
       // it contains the AMBARI/AMBARI_SERVER component; else do not include the Ambari service identity.
      includeAmbariIdentity &= (serviceComponentFilter.get("AMBARI") != null) && serviceComponentFilter.get("AMBARI").contains("AMBARI_SERVER");
      includeAmbariIdentity &= (serviceComponentFilter.get(RootService.AMBARI.name()) != null)
        && serviceComponentFilter.get(RootService.AMBARI.name()).contains(RootComponent.AMBARI_SERVER.name());
 
       if((operationType != OperationType.DEFAULT)) {
         // Update the identity filter, if necessary
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java
index eb3956253c..933afa250d 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/AmbariContext.java
@@ -49,6 +49,7 @@ import org.apache.ambari.server.controller.ClusterRequest;
 import org.apache.ambari.server.controller.ConfigGroupRequest;
 import org.apache.ambari.server.controller.ConfigurationRequest;
 import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.RootComponent;
 import org.apache.ambari.server.controller.ServiceComponentHostRequest;
 import org.apache.ambari.server.controller.ServiceComponentRequest;
 import org.apache.ambari.server.controller.ServiceRequest;
@@ -402,7 +403,7 @@ public class AmbariContext {
       for (String component : entry.getValue()) {
         //todo: handle this in a generic manner.  These checks are all over the code
         try {
          if (cluster.getService(service) != null && !component.equals("AMBARI_SERVER")) {
          if (cluster.getService(service) != null && !component.equals(RootComponent.AMBARI_SERVER.name())) {
             requests.add(new ServiceComponentHostRequest(clusterName, service, component, hostName, null));
           }
         } catch(AmbariException se) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintFactory.java
index 404068d09f..24b4785562 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintFactory.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/BlueprintFactory.java
@@ -30,6 +30,7 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.ObjectNotFoundException;
 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.RootComponent;
 import org.apache.ambari.server.controller.internal.ProvisionAction;
 import org.apache.ambari.server.controller.internal.Stack;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
@@ -205,7 +206,7 @@ public class BlueprintFactory {
       allComponents.addAll(components);
     }
     // currently ambari server is no a recognized component
    allComponents.add("AMBARI_SERVER");
    allComponents.add(RootComponent.AMBARI_SERVER.name());
 
     return allComponents;
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/validators/RequiredPasswordValidator.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/validators/RequiredPasswordValidator.java
index 5b4ecc1907..3ad1a19c7f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/validators/RequiredPasswordValidator.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/validators/RequiredPasswordValidator.java
@@ -19,6 +19,7 @@ import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 
import org.apache.ambari.server.controller.RootComponent;
 import org.apache.ambari.server.controller.internal.Stack;
 import org.apache.ambari.server.state.PropertyInfo;
 import org.apache.ambari.server.topology.Blueprint;
@@ -86,7 +87,7 @@ public class RequiredPasswordValidator implements TopologyValidator {
       HostGroup hostGroup = blueprint.getHostGroup(hostGroupName);
       for (String component : hostGroup.getComponentNames()) {
         //for now, AMBARI is not recognized as a service in Stacks
        if (component.equals("AMBARI_SERVER")) {
        if (component.equals(RootComponent.AMBARI_SERVER.name())) {
           continue;
         }
 
- 
2.19.1.windows.1

