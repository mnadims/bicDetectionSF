From 9db1834941ee24cb7c23fc8254f023aa51433a96 Mon Sep 17 00:00:00 2001
From: Robert Levas <rlevas@hortonworks.com>
Date: Tue, 19 Jul 2016 10:21:30 -0400
Subject: [PATCH] AMBARI-17772. Kerberos-related configs are not applied before
 INSTALL command is built on add service (rlevas)

--
 .../AmbariManagementControllerImpl.java       | 152 +++++++++++-------
 1 file changed, 95 insertions(+), 57 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
index b2a376119d..066acabdca 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
@@ -2421,7 +2421,7 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
     }
   }
 
  private RequestStageContainer doStageCreation(RequestStageContainer requestStages,
  protected RequestStageContainer doStageCreation(RequestStageContainer requestStages,
       Cluster cluster,
       Map<State, List<Service>> changedServices,
       Map<State, List<ServiceComponent>> changedComps,
@@ -2479,10 +2479,103 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
       stage.setAutoSkipFailureSupported(skipFailure);
       stage.setSkippable(skipFailure);
 
      Collection<ServiceComponentHost> componentsToConfigureForKerberos = new ArrayList<>();
       Collection<ServiceComponentHost> componentsToEnableKerberos = new ArrayList<>();
       Set<String> hostsToForceKerberosOperations = new HashSet<>();
 
      /* *******************************************************************************************
       * If Kerberos is enabled, pre-process the changed components to update any configurations and
       * indicate which components may need to have principals or keytab files created.
       *
       * NOTE: Configurations need to be updated before tasks are created to install components
       *       so that any configuration changes are included before the task is queued.
       *
       *       Kerberos-related stages need to be inserted between the INSTALLED and STARTED states
       *       because some services need to set up the host (i,e, create user accounts, etc...)
       *       before Kerberos-related tasks an occur (like distribute keytabs)
       * **************************************************************************************** */
      if(kerberosHelper.isClusterKerberosEnabled(cluster)) {
        Collection<ServiceComponentHost> componentsToConfigureForKerberos = new ArrayList<>();

        for (Map<State, List<ServiceComponentHost>> changedScHostStates : changedScHosts.values()) {

          if (changedScHostStates != null) {
            for (Map.Entry<State, List<ServiceComponentHost>> changedScHostState : changedScHostStates.entrySet()) {
              State newState = changedScHostState.getKey();

              if (newState == State.INSTALLED) {
                List<ServiceComponentHost> scHosts = changedScHostState.getValue();

                if (scHosts != null) {
                  for (ServiceComponentHost scHost : scHosts) {
                    State oldSchState = scHost.getState();

                    // If the state is transitioning from INIT TO INSTALLED and the cluster has Kerberos
                    // enabled, mark this ServiceComponentHost to see if anything needs to be done to
                    // make sure it is properly configured.
                    //
                    // If the component is transitioning from an INSTALL_FAILED to an INSTALLED state
                    // indicates a failure attempt on install followed by a new installation attempt and
                    // will also need consideration for Kerberos-related tasks
                    if ((oldSchState == State.INIT || oldSchState == State.INSTALL_FAILED)) {
                      // Check if the host component already exists, if it exists there is no need to
                      // reset Kerberos-related configs.
                      // Check if it's blueprint install. If it is, then do not configure this service
                      // at this time.
                      if (!hostComponentAlreadyExists(cluster, scHost) && !("INITIAL_INSTALL".equals(requestProperties.get("phase")))) {
                        componentsToConfigureForKerberos.add(scHost);
                      }

                      // Add the ServiceComponentHost to the componentsToEnableKerberos Set to indicate
                      // it may need Kerberos-related operations to be performed on its behalf.
                      // For example, creating principals and keytab files.
                      componentsToEnableKerberos.add(scHost);

                      if (Service.Type.KERBEROS.name().equalsIgnoreCase(scHost.getServiceName()) &&
                          Role.KERBEROS_CLIENT.name().equalsIgnoreCase(scHost.getServiceComponentName())) {
                        // Since the KERBEROS/KERBEROS_CLIENT is about to be moved from the INIT to the
                        // INSTALLED state (and it should be by the time the stages (in this request)
                        // that need to be execute), collect the relevant hostname to make sure the
                        // Kerberos logic doest not skip operations for it.
                        hostsToForceKerberosOperations.add(scHost.getHostName());
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // If there are any components that may need Kerberos-related configuration changes, do it
        // here - before the INSTALL tasks get created so the configuration updates are set and
        // get included in the task details.
        if (!componentsToConfigureForKerberos.isEmpty()) {
          // Build service/component filter to declare what services and components are being added
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
      }

       for (String compName : changedScHosts.keySet()) {
         for (State newState : changedScHosts.get(compName).keySet()) {
           for (ServiceComponentHost scHost :
@@ -2528,35 +2621,6 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
                         nowTimestamp,
                         scHost.getDesiredStackVersion().getStackId());
                   }

                  // If the state is transitioning from INIT TO INSTALLED and the cluster has Kerberos
                  // enabled, mark this ServiceComponentHost to see if anything needs to be done to
                  // make sure it is properly configured.
                  // If the component is transitioning from an INSTALL_FAILED to an INSTALLED state indicates a failure attempt on install
                  // followed by a new installation attempt and will also need Kerberos related configuration addressing
                  // The Kerberos-related stages needs to be
                  // between the INSTALLED and STARTED states because some services need to set up
                  // the host (i,e, create user accounts, etc...) before Kerberos-related tasks an
                  // occur (like distribute keytabs)
                  if((oldSchState == State.INIT || oldSchState == State.INSTALL_FAILED) && kerberosHelper.isClusterKerberosEnabled(cluster)) {
                    // check if host component already exists, if it exists no need to reset kerberos configs
                    // check if it's blueprint install. If it is, then do not call kerberos.configureService
                    if (!hostComponentAlreadyExists(cluster, scHost) && !("INITIAL_INSTALL".equals(requestProperties.get("phase")))) {
                      componentsToConfigureForKerberos.add(scHost);
                    }

                    componentsToEnableKerberos.add(scHost);

                    if(Service.Type.KERBEROS.name().equalsIgnoreCase(scHost.getServiceName()) &&
                        Role.KERBEROS_CLIENT.name().equalsIgnoreCase(scHost.getServiceComponentName())) {
                      // Since the KERBEROS/KERBEROS_CLIENT is about to be moved from the INIT to the
                      // INSTALLED state (and it should be by the time the stages (in this request)
                      // that need to be execute), collect the relevant hostname to make sure the
                      // Kerberos logic doest not skip operations for it.
                      hostsToForceKerberosOperations.add(scHost.getHostName());
                    }

                  }
                 } else if (oldSchState == State.STARTED
                       // TODO: oldSchState == State.INSTALLED is always false, looks like a bug
                       //|| oldSchState == State.INSTALLED
@@ -2753,32 +2817,6 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
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
- 
2.19.1.windows.1

