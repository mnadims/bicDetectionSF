From c3d738c6aed3bad135ae4aecfdab94c9fb5d7f42 Mon Sep 17 00:00:00 2001
From: Robert Levas <rlevas@hortonworks.com>
Date: Thu, 14 Jul 2016 20:40:11 -0400
Subject: [PATCH] AMBARI-17629. AUTH_TO_LOCAL rules are not updated when adding
 services to a Blueprint-installed cluster (rlevas)

--
 .../AmbariManagementControllerImpl.java       |  33 +-
 .../server/controller/KerberosHelper.java     |  42 ++-
 .../server/controller/KerberosHelperImpl.java | 287 ++++++++++--------
 ...PrepareKerberosIdentitiesServerAction.java |  19 +-
 .../topology/ClusterConfigurationRequest.java |  27 +-
 .../server/controller/KerberosHelperTest.java |  85 ++++--
 .../ClusterConfigurationRequestTest.java      |   2 +-
 7 files changed, 304 insertions(+), 191 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
index 872ec2d6e7..d57b38f444 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
@@ -2467,6 +2467,7 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
       stage.setAutoSkipFailureSupported(skipFailure);
       stage.setSkippable(skipFailure);
 
      Collection<ServiceComponentHost> componentsToConfigureForKerberos = new ArrayList<>();
       Collection<ServiceComponentHost> componentsToEnableKerberos = new ArrayList<>();
       Set<String> hostsToForceKerberosOperations = new HashSet<>();
 
@@ -2529,11 +2530,7 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
                     // check if host component already exists, if it exists no need to reset kerberos configs
                     // check if it's blueprint install. If it is, then do not call kerberos.configureService
                     if (!hostComponentAlreadyExists(cluster, scHost) && !("INITIAL_INSTALL".equals(requestProperties.get("phase")))) {
                      try {
                        kerberosHelper.configureService(cluster, scHost);
                      } catch (KerberosInvalidConfigurationException e) {
                        throw new AmbariException(e.getMessage(), e);
                      }
                      componentsToConfigureForKerberos.add(scHost);
                     }
 
                     componentsToEnableKerberos.add(scHost);
@@ -2744,6 +2741,32 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
       rg.build(stage);
       requestStages.addStages(rg.getStages());
 
      if(!componentsToConfigureForKerberos.isEmpty()) {
        // Build service/component filter to declare what services and compoents are being added
        // so kerberosHelper.configureServices know which to work on.  Null indicates no filter
        // and all services and components will be (re)configured, however null will not be
        // passed in from here.
        Map<String, Collection<String>> serviceFilter = new HashMap<String, Collection<String>>();

        for (ServiceComponentHost scHost : componentsToConfigureForKerberos) {
          String serviceName = scHost.getServiceName();
          Collection<String> componentFilter = serviceFilter.get(serviceName);

          if (componentFilter == null) {
            componentFilter = new HashSet<String>();
            serviceFilter.put(serviceName, componentFilter);
          }

          componentFilter.add(scHost.getServiceComponentName());
        }

        try {
          kerberosHelper.configureServices(cluster, serviceFilter);
        } catch (KerberosInvalidConfigurationException e) {
          throw new AmbariException(e.getMessage(), e);
        }
      }

       if (!componentsToEnableKerberos.isEmpty()) {
         Map<String, Collection<String>> serviceFilter = new HashMap<String, Collection<String>>();
         Set<String> hostFilter = new HashSet<String>();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelper.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelper.java
index 8170f1736a..c4d21fcf34 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelper.java
@@ -222,27 +222,39 @@ public interface KerberosHelper {
       throws AmbariException, KerberosOperationException;
 
   /**
   * Updates the relevant configurations for the given Service.
   * Updates the relevant configurations for the components specified in the service filter.
    * <p/>
   * If the relevant service and its components have Kerberos descriptors, configuration values from
   * If <code>null</code> is passed in as the service filter, all installed services and components
   * will be affected.  If an empty map is passed in, no services or components will be affected.
   * <p/>
   * If the relevant services and components have Kerberos descriptors, configuration values from
    * the descriptors are used to update the relevant configuration sets.
    *
   * @param cluster              the relevant Cluster
   * @param serviceComponentHost the ServiceComponentHost
   * @param cluster       the relevant Cluster
   * @param serviceFilter a Map of service names to component names indicating the
   *                      relevant set of services and components - if null, no
   *                      filter is relevant; if empty, the filter indicates no
   *                      relevant services or components
    * @throws AmbariException
    */
  void configureService(Cluster cluster, ServiceComponentHost serviceComponentHost)
  void configureServices(Cluster cluster, Map<String, Collection<String>> serviceFilter)
       throws AmbariException, KerberosInvalidConfigurationException;
 
   /**
    * Returns the updates configurations that are expected when the given set of services are configured
    * for Kerberos.
    *
   * @param cluster                  the cluster
   * @param existingConfigurations   the cluster's existing configurations
   * @param services                 the set of services to process
   * @param kerberosEnabled          true if kerberos is (to be) enabled; otherwise false
   * @param applyStackAdvisorUpdates true to invoke the stack advisor to validate property updates; false to skip
   * @param cluster                    the cluster
   * @param existingConfigurations     the cluster's existing configurations
   * @param installedServices          the map of services and relevant components to process
   * @param serviceFilter              a Map of service names to component names indicating the
   *                                   relevant set of services and components - if null, no
   *                                   filter is relevant; if empty, the filter indicates no
   *                                   relevant services or components
   * @param previouslyExistingServices a set of previously existing service names - null or a subset of installedServices
   * @param kerberosEnabled            true if kerberos is (to be) enabled; otherwise false
   * @param applyStackAdvisorUpdates   true to invoke the stack advisor to validate property updates;
   *                                   false to skip
    * @return a map of configuration updates
    * @throws AmbariException
    * @throws KerberosInvalidConfigurationException if an issue occurs trying to get the
@@ -250,8 +262,9 @@ public interface KerberosHelper {
    */
   Map<String, Map<String, String>> getServiceConfigurationUpdates(Cluster cluster,
                                                                   Map<String, Map<String, String>> existingConfigurations,
                                                                  Set<String> services,
                                                                  boolean serviceAlreadyExists,
                                                                  Map<String, Set<String>> installedServices,
                                                                  Map<String, Collection<String>> serviceFilter,
                                                                  Set<String> previouslyExistingServices,
                                                                   boolean kerberosEnabled,
                                                                   boolean applyStackAdvisorUpdates)
       throws KerberosInvalidConfigurationException, AmbariException;
@@ -348,14 +361,15 @@ public interface KerberosHelper {
    * the cluster and their relevant Kerberos descriptors to determine the rules to be created.
    *
    * @param kerberosDescriptor     the current Kerberos descriptor
   * @param cluster                the cluster
    * @param realm                  the default realm
   * @param installedServices      the map of services and relevant components to process
    * @param existingConfigurations a map of the current configurations
    * @param kerberosConfigurations a map of the configurations to update, this where the generated
    *                               auth-to-local values will be stored
    * @throws AmbariException
    */
  void setAuthToLocalRules(KerberosDescriptor kerberosDescriptor, Cluster cluster, String realm,
  void setAuthToLocalRules(KerberosDescriptor kerberosDescriptor, String realm,
                           Map<String, Set<String>> installedServices,
                            Map<String, Map<String, String>> existingConfigurations,
                            Map<String, Map<String, String>> kerberosConfigurations)
       throws AmbariException;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelperImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelperImpl.java
index cc4824074a..70dc4c3c3d 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelperImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/KerberosHelperImpl.java
@@ -25,6 +25,7 @@ import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Date;
import java.util.EnumSet;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
@@ -129,6 +130,14 @@ public class KerberosHelperImpl implements KerberosHelper {
 
   private static final Logger LOG = LoggerFactory.getLogger(KerberosHelperImpl.class);
 
  /**
   * The set of states a component may be in, indicating that is have been previously installed on
   * the cluster.
   *
   * These values are important when trying to determine the state of the cluster when adding new components
   */
  private static final Set<State> PREVIOUSLY_INSTALLED_STATES = EnumSet.of(State.INSTALLED, State.STARTED, State.DISABLED);

   @Inject
   private AmbariCustomCommandExecutionHelper customCommandExecutionHelper;
 
@@ -284,44 +293,59 @@ public class KerberosHelperImpl implements KerberosHelper {
   }
 
   @Override
  public void configureService(Cluster cluster, ServiceComponentHost serviceComponentHost)
  public void configureServices(Cluster cluster, Map<String, Collection<String>> serviceFilter)
       throws AmbariException, KerberosInvalidConfigurationException {
    Map<String, Map<String, String>> existingConfigurations = calculateExistingConfigurations(cluster, null);
    Map<String, Set<String>> installedServices = new HashMap<String, Set<String>>();
    Set<String> previouslyExistingServices = new HashSet<String>();

    // Calculate the map of installed services to installed components
    Map<String, Service> clusterServices = cluster.getServices();
    if(clusterServices != null) {
      for (Service clusterService : clusterServices.values()) {
        Set<String> installedComponents = installedServices.get(clusterService.getName());
        if (installedComponents == null) {
          installedComponents = new HashSet<String>();
          installedServices.put(clusterService.getName(), installedComponents);
        }
 
    String serviceName = serviceComponentHost.getServiceName();
    String hostName = serviceComponentHost.getHostName();

    Map<String, Map<String, String>> existingConfigurations = calculateExistingConfigurations(cluster, hostName);
        Map<String, ServiceComponent> clusterServiceComponents = clusterService.getServiceComponents();
        if (clusterServiceComponents != null) {
          for (ServiceComponent clusterServiceComponent : clusterServiceComponents.values()) {
            installedComponents.add(clusterServiceComponent.getName());

            // Determine if this component was PREVIOUSLY installed, which implies that its containing service was PREVIOUSLY installed
            if (!previouslyExistingServices.contains(clusterService.getName())) {
              Map<String, ServiceComponentHost> clusterServiceComponentHosts = clusterServiceComponent.getServiceComponentHosts();
              if (clusterServiceComponentHosts != null) {
                for (ServiceComponentHost clusterServiceComponentHost : clusterServiceComponentHosts.values()) {
                  if (PREVIOUSLY_INSTALLED_STATES.contains(clusterServiceComponentHost.getState())) {
                    previouslyExistingServices.add(clusterService.getName());
                    break;
                  }
                }
              }
            }
          }
        }
      }
    }
 
     Map<String, Map<String, String>> updates = getServiceConfigurationUpdates(cluster,
        existingConfigurations, Collections.singleton(serviceName), serviceAlreadyExists(cluster, serviceComponentHost), true, true);
        existingConfigurations, installedServices, serviceFilter, previouslyExistingServices, true, true);
 
     for (Map.Entry<String, Map<String, String>> entry : updates.entrySet()) {
       configHelper.updateConfigType(cluster, ambariManagementController, entry.getKey(), entry.getValue(), null,
          ambariManagementController.getAuthName(), String.format("Enabling Kerberos for %s", serviceName));
          ambariManagementController.getAuthName(), "Enabling Kerberos for added components");
     }
   }
 
  private boolean serviceAlreadyExists(Cluster cluster, ServiceComponentHost sch) throws AmbariException {
    Service service = cluster.getService(sch.getServiceName());
    if (service != null) {
      Map<String, ServiceComponent> serviceComponentMap = service.getServiceComponents();
      for (ServiceComponent serviceComponent : serviceComponentMap.values()) {
        Map<String, ServiceComponentHost> serviceComponentHostMap = serviceComponent.getServiceComponentHosts();
        for (ServiceComponentHost serviceComponentHost : serviceComponentHostMap.values()) {
          if (serviceComponentHost.getState() == State.INSTALLED || serviceComponentHost.getState() == State.STARTED) {
            return true;
          }
        }
      }
    }
    return false;
  }

   @Override
   public Map<String, Map<String, String>> getServiceConfigurationUpdates(Cluster cluster,
                                                                          Map<String, Map<String, String>> existingConfigurations,
                                                                         Set<String> services,
                                                                         boolean serviceAlreadyExists,
                                                                         Map<String, Set<String>> installedServices,
                                                                         Map<String, Collection<String>> serviceFilter,
                                                                         Set<String> previouslyExistingServices,
                                                                          boolean kerberosEnabled,
                                                                          boolean applyStackAdvisorUpdates)
       throws KerberosInvalidConfigurationException, AmbariException {
@@ -339,64 +363,45 @@ public class KerberosHelperImpl implements KerberosHelper {
     // Create the context to use for filtering Kerberos Identities based on the state of the cluster
     Map<String, Object> filterContext = new HashMap<String, Object>();
     filterContext.put("configurations", configurations);
    filterContext.put("services", services);

    for (String serviceName : services) {
      // Set properties...
      KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(serviceName);

      if (serviceDescriptor != null) {
        Map<String, KerberosComponentDescriptor> componentDescriptors = serviceDescriptor.getComponents();
        for (KerberosComponentDescriptor componentDescriptor : componentDescriptors.values()) {
          if (componentDescriptor != null) {
            Map<String, Map<String, String>> identityConfigurations;

            identityConfigurations = getIdentityConfigurations(serviceDescriptor.getIdentities(true, filterContext));
            if (identityConfigurations != null) {
              for (Map.Entry<String, Map<String, String>> entry : identityConfigurations.entrySet()) {
                String configType = entry.getKey();
                Map<String, String> properties = entry.getValue();

                mergeConfigurations(kerberosConfigurations, configType, entry.getValue(), configurations);

                if ((properties != null) && !properties.isEmpty()) {
                  Set<String> propertyNames = propertiesToIgnore.get(configType);
                  if (propertyNames == null) {
                    propertyNames = new HashSet<String>();
                    propertiesToIgnore.put(configType, propertyNames);
                  }
                  propertyNames.addAll(properties.keySet());
                }
              }
            }
    filterContext.put("services", installedServices.keySet());
 
            identityConfigurations = getIdentityConfigurations(componentDescriptor.getIdentities(true, filterContext));
            if (identityConfigurations != null) {
              for (Map.Entry<String, Map<String, String>> entry : identityConfigurations.entrySet()) {
                String configType = entry.getKey();
                Map<String, String> properties = entry.getValue();
    for (Map.Entry<String, Set<String>> installedServiceEntry : installedServices.entrySet()) {
      String installedService = installedServiceEntry.getKey();
 
                mergeConfigurations(kerberosConfigurations, configType, entry.getValue(), configurations);
      if ((serviceFilter == null) || (serviceFilter.containsKey(installedService))) {
        Collection<String> componentFilter = (serviceFilter == null) ? null : serviceFilter.get(installedService);
        Set<String> installedComponents = installedServiceEntry.getValue();
 
                if ((properties != null) && !properties.isEmpty()) {
                  Set<String> propertyNames = propertiesToIgnore.get(configType);
                  if (propertyNames == null) {
                    propertyNames = new HashSet<String>();
                    propertiesToIgnore.put(configType, propertyNames);
                  }
                  propertyNames.addAll(properties.keySet());
        // Set properties...
        KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(installedService);

        if (serviceDescriptor != null) {
          if (installedComponents != null) {
            boolean servicePreviouslyExisted = (previouslyExistingServices != null) && previouslyExistingServices.contains(installedService);

            for (String installedComponent : installedComponents) {

              if ((componentFilter == null) || componentFilter.contains(installedComponent)) {
                KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent(installedComponent);
                if (componentDescriptor != null) {
                  Map<String, Map<String, String>> identityConfigurations;

                  identityConfigurations = getIdentityConfigurations(serviceDescriptor.getIdentities(true, filterContext));
                  processIdentityConfigurations(identityConfigurations, kerberosConfigurations, configurations, propertiesToIgnore);

                  identityConfigurations = getIdentityConfigurations(componentDescriptor.getIdentities(true, filterContext));
                  processIdentityConfigurations(identityConfigurations, kerberosConfigurations, configurations, propertiesToIgnore);

                  mergeConfigurations(kerberosConfigurations,
                      componentDescriptor.getConfigurations(!servicePreviouslyExisted), configurations);
                 }
               }
             }

            mergeConfigurations(kerberosConfigurations,
                componentDescriptor.getConfigurations(!serviceAlreadyExists), configurations);
           }
         }
       }
     }
 

     if (kerberosDetails.createAmbariPrincipal()) {
       KerberosIdentityDescriptor ambariServerIdentityDescriptor = kerberosDescriptor.getIdentity(KerberosHelper.AMBARI_IDENTITY_NAME);
       Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
@@ -419,10 +424,10 @@ public class KerberosHelperImpl implements KerberosHelper {
       }
     }
 
    setAuthToLocalRules(kerberosDescriptor, cluster, kerberosDetails.getDefaultRealm(), configurations, kerberosConfigurations);
    setAuthToLocalRules(kerberosDescriptor, kerberosDetails.getDefaultRealm(), installedServices, configurations, kerberosConfigurations);
 
     return (applyStackAdvisorUpdates)
        ? applyStackAdvisorUpdates(cluster, cluster.getServices().keySet(), configurations, kerberosConfigurations, propertiesToIgnore,
        ? applyStackAdvisorUpdates(cluster, installedServices.keySet(), configurations, kerberosConfigurations, propertiesToIgnore,
         null, new HashMap<String, Set<String>>(), kerberosEnabled)
         : kerberosConfigurations;
   }
@@ -800,7 +805,8 @@ public class KerberosHelperImpl implements KerberosHelper {
   }
 
   @Override
  public void setAuthToLocalRules(KerberosDescriptor kerberosDescriptor, Cluster cluster, String realm,
  public void setAuthToLocalRules(KerberosDescriptor kerberosDescriptor, String realm,
                                  Map<String, Set<String>> installedServices,
                                   Map<String, Map<String, String>> existingConfigurations,
                                   Map<String, Map<String, String>> kerberosConfigurations)
       throws AmbariException {
@@ -826,7 +832,7 @@ public class KerberosHelperImpl implements KerberosHelper {
       // Create the context to use for filtering Kerberos Identities based on the state of the cluster
       Map<String, Object> filterContext = new HashMap<String, Object>();
       filterContext.put("configurations", existingConfigurations);
      filterContext.put("services", cluster.getServices().keySet());
      filterContext.put("services", installedServices.keySet());
 
       // Determine which properties need to be set
       AuthToLocalBuilder authToLocalBuilder = new AuthToLocalBuilder(realm, additionalRealms, caseInsensitiveUser);
@@ -837,70 +843,34 @@ public class KerberosHelperImpl implements KerberosHelper {
         authToLocalPropertiesToSet.addAll(authToLocalProperties);
       }
 
      Map<String, KerberosServiceDescriptor> services = kerberosDescriptor.getServices();
      if (services != null) {
        Map<String, Service> installedServices = cluster.getServices();
      for(Map.Entry<String, Set<String>> installedService: installedServices.entrySet()) {
        String serviceName = installedService.getKey();
 
        for (KerberosServiceDescriptor service : services.values()) {
          if (installedServices.containsKey(service.getName())) {
            Service svc = installedServices.get(service.getName());
            addIdentities(authToLocalBuilder, service.getIdentities(true, filterContext), null, existingConfigurations);
        KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(serviceName);
        if(serviceDescriptor != null) {
          LOG.info("Adding identities for service {} to auth to local mapping", installedService);
 
            authToLocalProperties = service.getAuthToLocalProperties();
            if (authToLocalProperties != null) {
              authToLocalPropertiesToSet.addAll(authToLocalProperties);
            }
          // Process the service-level Kerberos descriptor
          addIdentities(authToLocalBuilder, serviceDescriptor.getIdentities(true, filterContext), null, existingConfigurations);
 
            Map<String, KerberosComponentDescriptor> components = service.getComponents();
            if (components != null) {
              Map<String, ServiceComponent> serviceComponents = svc.getServiceComponents();

              for (KerberosComponentDescriptor component : components.values()) {
                // When the cluster is provisioned by a Blueprint service components with
                // cardinality 0+ might be left out from the Blueprint thus we have to check
                // if they exist
                ServiceComponent svcComp = null;
                if (!serviceComponents.containsKey(component.getName())) {
                  continue;
                }
          authToLocalProperties = serviceDescriptor.getAuthToLocalProperties();
          if (authToLocalProperties != null) {
            authToLocalPropertiesToSet.addAll(authToLocalProperties);
          }
 
                svcComp = serviceComponents.get(component.getName());
          // Process the relevant component-level Kerberos descriptors
          Set<String> installedComponents = installedService.getValue();
          if(installedComponents != null) {
            for (String installedComponent : installedComponents) {
              KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent(installedComponent);
 
                boolean addSvcCompIdentities = false;
              if (componentDescriptor != null) {
                LOG.info("Adding identities for component {} to auth to local mapping", installedComponent);
                addIdentities(authToLocalBuilder, componentDescriptor.getIdentities(true, filterContext), null, existingConfigurations);
 
                if (cluster.isBluePrintDeployed()) {
                  if (svcComp.getDesiredState() == State.INSTALLED || svcComp.getDesiredState() == State.STARTED) {
                    addSvcCompIdentities = true;
                  }
                } else {

                  // Since when the cluster is deployed through the UI ALL service components of the selected services are created
                  // with desired state INSTALLED regardless whether the service components were associated with hosts or not thus
                  // we can not determine if the component is installed or not.
                  // We rather look at service compoent hosts
                  for (ServiceComponentHost svcCompHost : svcComp.getServiceComponentHosts().values()) {
                    if (svcCompHost.getDesiredState() != State.UNKNOWN
                        && svcCompHost.getDesiredState() != State.UNINSTALLING
                        && svcCompHost.getDesiredState() != State.UNINSTALLED
                        && svcCompHost.getDesiredState() != State.INSTALL_FAILED
                        && svcCompHost.getDesiredState() != State.WIPING_OUT) {

                      // If there is at least a host that contains the component add the identities
                      addSvcCompIdentities = true;
                      break;
                    }
                  }
                }

                if (addSvcCompIdentities) {
                  LOG.info("Adding identity for " + component.getName() + " to auth to local mapping");
                  addIdentities(authToLocalBuilder, component.getIdentities(true, filterContext), null, existingConfigurations);

                  authToLocalProperties = component.getAuthToLocalProperties();
                  if (authToLocalProperties != null) {
                    authToLocalPropertiesToSet.addAll(authToLocalProperties);

                  }
                authToLocalProperties = componentDescriptor.getAuthToLocalProperties();
                if (authToLocalProperties != null) {
                  authToLocalPropertiesToSet.addAll(authToLocalProperties);
                 }
               }
             }
@@ -2554,6 +2524,55 @@ public class KerberosHelperImpl implements KerberosHelper {
     return identitiesToRemove;
   }
 
  /**
   * Processes the configuration values related to a particular Kerberos descriptor identity definition
   * by:
   * <ol>
   * <li>
   * merging the declared properties and their values from <code>identityConfigurations</code> with the set of
   * Kerberos-related configuration updates in <code>kerberosConfigurations</code>, using the existing cluster
   * configurations in <code>configurations</code>
   * </li>
   * <li>
   * ensuring that these properties are not overwritten by recommendations by the stack advisor later
   * in the workflow by adding them to the <code>propertiesToIgnore</code> map
   * </li>
   * </ol>
   *
   * @param identityConfigurations a map of config-types to property name/value pairs to process
   * @param kerberosConfigurations a map of config-types to property name/value pairs to be applied
   *                               as configuration updates
   * @param configurations         a map of config-types to property name/value pairs representing
   *                               the existing configurations for the cluster
   * @param propertiesToIgnore     a map of config-types to property names to be ignored while
   *                               processing stack advisor recommendations
   * @throws AmbariException
   */
  private void processIdentityConfigurations(Map<String, Map<String, String>> identityConfigurations,
                                             Map<String, Map<String, String>> kerberosConfigurations,
                                             Map<String, Map<String, String>> configurations,
                                             Map<String, Set<String>> propertiesToIgnore)
      throws AmbariException {
    if (identityConfigurations != null) {
      for (Map.Entry<String, Map<String, String>> identitiyEntry : identityConfigurations.entrySet()) {
        String configType = identitiyEntry.getKey();
        Map<String, String> properties = identitiyEntry.getValue();

        mergeConfigurations(kerberosConfigurations, configType, identitiyEntry.getValue(), configurations);

        if ((properties != null) && !properties.isEmpty()) {
          Set<String> propertyNames = propertiesToIgnore.get(configType);
          if (propertyNames == null) {
            propertyNames = new HashSet<String>();
            propertiesToIgnore.put(configType, propertyNames);
          }
          propertyNames.addAll(properties.keySet());
        }
      }
    }

  }

   /* ********************************************************************************************
    * Helper classes and enums
    * ******************************************************************************************** *\
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/PrepareKerberosIdentitiesServerAction.java b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/PrepareKerberosIdentitiesServerAction.java
index f70c546b23..036888159f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/PrepareKerberosIdentitiesServerAction.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/kerberos/PrepareKerberosIdentitiesServerAction.java
@@ -33,6 +33,7 @@ import java.io.File;
 import java.io.IOException;
 import java.util.Collection;
 import java.util.HashMap;
import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
@@ -158,7 +159,7 @@ public class PrepareKerberosIdentitiesServerAction extends AbstractPrepareKerber
   }
 
   /**
   * Conditionally calls {@link KerberosHelper#setAuthToLocalRules(KerberosDescriptor, Cluster, String, Map, Map)}
   * Conditionally calls {@link KerberosHelper#setAuthToLocalRules(KerberosDescriptor, String, Map, Map, Map)}
    * if there are ServiceComponentHosts to process
    *
    * @param cluster                cluster instance
@@ -167,7 +168,7 @@ public class PrepareKerberosIdentitiesServerAction extends AbstractPrepareKerber
    * @param kerberosConfigurations the Kerberos-specific configuration map
    * @param defaultRealm           the default realm
    * @throws AmbariException
   * @see KerberosHelper#setAuthToLocalRules(KerberosDescriptor, Cluster, String, Map, Map)
   * @see KerberosHelper#setAuthToLocalRules(KerberosDescriptor, String, Map, Map, Map)
    */
   protected void processAuthToLocalRules(Cluster cluster, KerberosDescriptor kerberosDescriptor,
                                          List<ServiceComponentHost> schToProcess,
@@ -176,7 +177,19 @@ public class PrepareKerberosIdentitiesServerAction extends AbstractPrepareKerber
       throws AmbariException {
     if (!schToProcess.isEmpty()) {
       actionLog.writeStdOut("Creating auth-to-local rules");
      kerberosHelper.setAuthToLocalRules(kerberosDescriptor, cluster, defaultRealm,

      Map<String,Set<String>> services = new HashMap<String, Set<String>>();
      for(ServiceComponentHost sch: schToProcess) {
        Set<String> components = services.get(sch.getServiceName());
        if(components == null) {
          components = new HashSet<String>();
          services.put(sch.getServiceName(), components);
        }

        components.add(sch.getServiceComponentName());
      }

      kerberosHelper.setAuthToLocalRules(kerberosDescriptor, defaultRealm, services,
           kerberosHelper.calculateConfigurations(cluster, null, kerberosDescriptor.getProperties()),
           kerberosConfigurations);
     }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterConfigurationRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterConfigurationRequest.java
index 88f0e3caea..6ae08d1146 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterConfigurationRequest.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/ClusterConfigurationRequest.java
@@ -182,7 +182,7 @@ public class ClusterConfigurationRequest {
       // apply Kerberos specific configurations
       Map<String, Map<String, String>> updatedConfigs = AmbariContext.getController().getKerberosHelper()
         .getServiceConfigurationUpdates(cluster, existingConfigurations,
        new HashSet<String>(blueprint.getServices()), false, true, false);
            createServiceComponentMap(blueprint), null, null, true, false);
 
       // ******************************************************************************************
       // Since Kerberos is being enabled, make sure the cluster-env/security_enabled property is
@@ -219,6 +219,29 @@ public class ClusterConfigurationRequest {
     return updatedConfigTypes;
   }
 
  /**
   * Create a map of services and the relevant components that are specified in the Blueprint
   *
   * @param blueprint the blueprint
   * @return a map of service names to component names
   */
  private Map<String, Set<String>> createServiceComponentMap(Blueprint blueprint) {
    Map<String, Set<String>> serviceComponents = new HashMap<String, Set<String>>();
    Collection<String> services = blueprint.getServices();

    if(services != null) {
      for (String service : services) {
        Collection<String> components = blueprint.getComponents(service);
        serviceComponents.put(service,
            (components == null)
                ? Collections.<String>emptySet()
                : new HashSet<String>(blueprint.getComponents(service)));
      }
    }

    return serviceComponents;
  }

   /**
    * Returns true if the property exists in clusterConfigProperties and has a custom user defined value. Property has
    * custom value in case we there's no stack default value for it or it's not equal to stack default value.
@@ -280,7 +303,7 @@ public class ClusterConfigurationRequest {
       // apply Kerberos specific configurations
       Map<String, Map<String, String>> updatedConfigs = AmbariContext.getController().getKerberosHelper()
         .getServiceConfigurationUpdates(cluster, existingConfigurations,
          new HashSet<String>(blueprint.getServices()), false, true, false);
          createServiceComponentMap(blueprint), null, null, true, false);
 
       // retrieve hostgroup for component names extracted from variables like "{clusterHostInfo.(component_name)
       // _host}"
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java
index 66ed68da99..6387fe8328 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java
@@ -104,6 +104,7 @@ import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Date;
import java.util.EnumSet;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
@@ -1858,14 +1859,21 @@ public class KerberosHelperTest extends EasyMockSupport {
     expect(identityDescriptor3.getPrincipalDescriptor()).andReturn(principalDescriptor3).times(1);
     expect(identityDescriptor3.shouldInclude(anyObject(Map.class))).andReturn(true).anyTimes();
 
    final KerberosComponentDescriptor componentDescriptor1 = createMockComponentDescriptor(
        "COMPONENT1",
        new ArrayList<KerberosIdentityDescriptor>() {
          {
            add(identityDescriptor3);
          }
        },
        null);

     final KerberosServiceDescriptor serviceDescriptor1 = createMock(KerberosServiceDescriptor.class);
    expect(serviceDescriptor1.getName()).andReturn("SERVICE1").times(2);
     expect(serviceDescriptor1.getIdentities(eq(true), anyObject(Map.class))).andReturn(Arrays.asList(
         identityDescriptor1,
        identityDescriptor2,
        identityDescriptor3
        identityDescriptor2
     )).times(1);
    expect(serviceDescriptor1.getComponents()).andReturn(null).times(1);
    expect(serviceDescriptor1.getComponent("COMPONENT1")).andReturn(componentDescriptor1).times(1);
     expect(serviceDescriptor1.getAuthToLocalProperties()).andReturn(new HashSet<String>(Arrays.asList(
         "default",
         "explicit_multiple_lines|new_lines",
@@ -1881,12 +1889,9 @@ public class KerberosHelperTest extends EasyMockSupport {
     expect(kerberosDescriptor.getProperty("additional_realms")).andReturn(null).times(1);
     expect(kerberosDescriptor.getIdentities(eq(true), anyObject(Map.class))).andReturn(null).times(1);
     expect(kerberosDescriptor.getAuthToLocalProperties()).andReturn(null).times(1);
    expect(kerberosDescriptor.getServices()).andReturn(Collections.singletonMap("SERVICE1", serviceDescriptor1)).times(1);
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).times(1);
 
    final Service service1 = createNiceMock(Service.class);

    final Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getServices()).andReturn(Collections.singletonMap("SERVICE1", service1)).anyTimes();
    Map<String, Set<String>> installedServices = Collections.singletonMap("SERVICE1", Collections.singleton("COMPONENT1"));
 
     Map<String, Map<String, String>> kerberosConfigurations = new HashMap<String, Map<String, String>>();
 
@@ -1897,7 +1902,7 @@ public class KerberosHelperTest extends EasyMockSupport {
     Map existingConfigs = new HashMap<String, Map<String, String>>();
     existingConfigs.put("kerberos-env", new HashMap<String, String>());
 
    kerberosHelper.setAuthToLocalRules(kerberosDescriptor, cluster, "EXAMPLE.COM", existingConfigs, kerberosConfigurations);
    kerberosHelper.setAuthToLocalRules(kerberosDescriptor, "EXAMPLE.COM", installedServices, existingConfigs, kerberosConfigurations);
 
     verifyAll();
 
@@ -2113,6 +2118,8 @@ public class KerberosHelperTest extends EasyMockSupport {
           }
         }
     );
    expect(serviceDescriptor1.getComponent("COMPONENT1A")).andReturn(componentDescriptor1a).times(4);
    expect(serviceDescriptor1.getComponent("COMPONENT1B")).andReturn(componentDescriptor1b).times(4);
 
     final KerberosServiceDescriptor serviceDescriptor2 = createMockServiceDescriptor(
         "SERVICE2",
@@ -2123,6 +2130,8 @@ public class KerberosHelperTest extends EasyMockSupport {
           }
         },
         Collections.<KerberosIdentityDescriptor>emptyList());
    expect(serviceDescriptor2.getComponent("COMPONENT2A")).andReturn(componentDescriptor2a).times(2);
    expect(serviceDescriptor2.getComponent("COMPONENT2B")).andReturn(componentDescriptor2b).times(2);
 
     final KerberosServiceDescriptor serviceDescriptor3 = createMockServiceDescriptor(
         "SERVICE3",
@@ -2132,19 +2141,13 @@ public class KerberosHelperTest extends EasyMockSupport {
           }
         },
         Collections.<KerberosIdentityDescriptor>emptyList());
    expect(serviceDescriptor3.getComponent("COMPONENT3A")).andReturn(componentDescriptor3a).times(4);
 
     final Map<String, String> kerberosDescriptorProperties = new HashMap<String, String>();
     kerberosDescriptorProperties.put("realm", "${kerberos-env/realm}");
 
     final KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
     expect(kerberosDescriptor.getProperties()).andReturn(kerberosDescriptorProperties).atLeastOnce();
    expect(kerberosDescriptor.getServices()).andReturn(new HashMap<String, KerberosServiceDescriptor>() {
      {
        put("SERVICE1", serviceDescriptor1);
        put("SERVICE2", serviceDescriptor2);
        put("SERVICE3", serviceDescriptor3);
      }
    }).atLeastOnce();
     expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).atLeastOnce();
     expect(kerberosDescriptor.getService("SERVICE2")).andReturn(serviceDescriptor2).atLeastOnce();
     expect(kerberosDescriptor.getService("SERVICE3")).andReturn(serviceDescriptor3).atLeastOnce();
@@ -2192,14 +2195,14 @@ public class KerberosHelperTest extends EasyMockSupport {
             put("COMPONENT1A", createMockComponent("COMPONENT1A", true,
                 new HashMap<String, ServiceComponentHost>() {
                   {
                    put("hostA", createMockServiceComponentHost());
                    put("hostA", createMockServiceComponentHost(State.INSTALLED));
                   }
                 }));
             put("COMPONENT1B", createMockComponent("COMPONENT1B", false,
                 new HashMap<String, ServiceComponentHost>() {
                   {
                    put("hostB", createMockServiceComponentHost());
                    put("hostC", createMockServiceComponentHost());
                    put("hostB", createMockServiceComponentHost(State.INSTALLED));
                    put("hostC", createMockServiceComponentHost(State.INSTALLED));
                   }
                 }));
           }
@@ -2210,14 +2213,14 @@ public class KerberosHelperTest extends EasyMockSupport {
             put("COMPONENT2A", createMockComponent("COMPONENT2A", true,
                 new HashMap<String, ServiceComponentHost>() {
                   {
                    put("hostA", createMockServiceComponentHost());
                    put("hostA", createMockServiceComponentHost(State.INSTALLED));
                   }
                 }));
             put("COMPONENT2B", createMockComponent("COMPONENT2B", false,
                 new HashMap<String, ServiceComponentHost>() {
                   {
                    put("hostB", createMockServiceComponentHost());
                    put("hostC", createMockServiceComponentHost());
                    put("hostB", createMockServiceComponentHost(State.INSTALLED));
                    put("hostC", createMockServiceComponentHost(State.INSTALLED));
                   }
                 }));
           }
@@ -2228,7 +2231,7 @@ public class KerberosHelperTest extends EasyMockSupport {
             put("COMPONENT3A", createMockComponent("COMPONENT3A", true,
                 new HashMap<String, ServiceComponentHost>() {
                   {
                    put("hostA", createMockServiceComponentHost());
                    put("hostA", createMockServiceComponentHost(State.INSTALLED));
                   }
                 }));
           }
@@ -2248,8 +2251,7 @@ public class KerberosHelperTest extends EasyMockSupport {
 
     final Cluster cluster = createMockCluster(hosts, SecurityType.KERBEROS, krb5ConfConfig, kerberosEnvConfig);
     expect(cluster.getServices()).andReturn(services).anyTimes();
    expect(cluster.getServiceComponentHostMap(null, services.keySet())).andReturn(serviceComponentHostMap).anyTimes();
    expect(cluster.isBluePrintDeployed()).andReturn(false).atLeastOnce();
    expect(cluster.getServiceComponentHostMap(anyObject(Set.class), anyObject(Set.class))).andReturn(serviceComponentHostMap).anyTimes();
 
     final Map<String, Map<String, String>> existingConfigurations = new HashMap<String, Map<String, String>>() {
       {
@@ -2285,11 +2287,24 @@ public class KerberosHelperTest extends EasyMockSupport {
     // Needed by infrastructure
     injector.getInstance(AmbariMetaInfo.class).init();
 
    HashMap<String,Set<String>> installedServices1 = new HashMap<String, Set<String>>();
    installedServices1.put("SERVICE1", new HashSet<String>(Arrays.asList("COMPONENT1A", "COMPONENT1B")));
    installedServices1.put("SERVICE2", new HashSet<String>(Arrays.asList("COMPONENT2A", "COMPONENT2B")));
    installedServices1.put("SERVICE3", Collections.singleton("COMPONENT3A"));

     Map<String, Map<String, String>> updates1 = kerberosHelper.getServiceConfigurationUpdates(
        cluster, existingConfigurations, new HashSet<String>(Arrays.asList("SERVICE1", "SERVICE2", "SERVICE3")), false, true, true);
        cluster, existingConfigurations, installedServices1, null, null, true, true);

    HashMap<String,Set<String>> installedServices2 = new HashMap<String, Set<String>>();
    installedServices2.put("SERVICE1", new HashSet<String>(Arrays.asList("COMPONENT1A", "COMPONENT1B")));
    installedServices2.put("SERVICE3", Collections.singleton("COMPONENT3A"));

    Map<String, Collection<String>> serviceFilter2 = new HashMap<String, Collection<String>>();
    serviceFilter2.put("SERVICE1", new HashSet<String>(Arrays.asList("COMPONENT1A", "COMPONENT1B")));
    serviceFilter2.put("SERVICE3", Collections.singleton("COMPONENT3A"));
 
     Map<String, Map<String, String>> updates2 = kerberosHelper.getServiceConfigurationUpdates(
        cluster, existingConfigurations, new HashSet<String>(Arrays.asList("SERVICE1", "SERVICE3")), false, true, true);
        cluster, existingConfigurations, installedServices2, serviceFilter2, null, true, true);
 
     verifyAll();
 
@@ -2342,6 +2357,12 @@ public class KerberosHelperTest extends EasyMockSupport {
 
     expectedUpdates.remove("service2-site");
     expectedUpdates.get("core-site").put("newPropertyRecommendation", "newPropertyRecommendation");
    expectedUpdates.get("core-site").put("auth.to.local", "RULE:[1:$1@$0](.*@FOOBAR.COM)s/@.*//\n" +
        "RULE:[2:$1@$0](component1a@FOOBAR.COM)s/.*/service1user/\n" +
        "RULE:[2:$1@$0](component1b@FOOBAR.COM)s/.*/service1user/\n" +
        "RULE:[2:$1@$0](component3a@FOOBAR.COM)s/.*/service3user/\n" +
        "RULE:[2:$1@$0](service1@FOOBAR.COM)s/.*/service1user/\n" +
        "DEFAULT");
     expectedUpdates.get("service1-site").put("component1b.property", "replaced value");
     expectedUpdates.put("new-type", new HashMap<String, String>() {
       {
@@ -2417,10 +2438,10 @@ public class KerberosHelperTest extends EasyMockSupport {
     Host host3 = createMockHost("host2");
 
     Map<String, ServiceComponentHost> service1Component1HostMap = new HashMap<String, ServiceComponentHost>();
    service1Component1HostMap.put("host1", createMockServiceComponentHost());
    service1Component1HostMap.put("host1", createMockServiceComponentHost(State.INSTALLED));
 
     Map<String, ServiceComponentHost> service2Component1HostMap = new HashMap<String, ServiceComponentHost>();
    service2Component1HostMap.put("host2", createMockServiceComponentHost());
    service2Component1HostMap.put("host2", createMockServiceComponentHost(State.INSTALLED));
 
     Map<String, ServiceComponent> service1ComponentMap = new HashMap<String, ServiceComponent>();
     service1ComponentMap.put("COMPONENT11", createMockComponent("COMPONENT11", true, service1Component1HostMap));
@@ -3724,9 +3745,9 @@ public class KerberosHelperTest extends EasyMockSupport {
     return descriptor;
   }
 
  private ServiceComponentHost createMockServiceComponentHost() {
  private ServiceComponentHost createMockServiceComponentHost(State state) {
     ServiceComponentHost serviceComponentHost = createMock(ServiceComponentHost.class);
    expect(serviceComponentHost.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(serviceComponentHost.getDesiredState()).andReturn(state).anyTimes();
     return serviceComponentHost;
   }
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java
index 58919b93ea..3176e425fb 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java
@@ -230,7 +230,7 @@ public class ClusterConfigurationRequestTest {
     expect(kerberosHelper.ensureHeadlessIdentities(anyObject(Cluster.class), anyObject(Map.class), anyObject
       (Set.class))).andReturn(true).once();
     expect(kerberosHelper.getServiceConfigurationUpdates(anyObject(Cluster.class), anyObject(Map.class), anyObject
      (Set.class), anyBoolean(), anyBoolean(), eq(false))).andReturn(kerberosConfig).once();
      (Map.class), anyObject(Map.class), anyObject(Set.class), anyBoolean(), eq(false))).andReturn(kerberosConfig).once();
 
     Capture<? extends String> captureClusterName = newCapture(CaptureType.ALL);
     Capture<? extends Set<String>> captureUpdatedConfigTypes = newCapture(CaptureType.ALL);
- 
2.19.1.windows.1

