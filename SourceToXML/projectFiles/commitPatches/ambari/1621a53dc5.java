From 1621a53dc5820ba1ca5bf5204628b51d341f0caa Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Wed, 24 Feb 2016 18:02:00 -0500
Subject: [PATCH] AMBARI-15173 - Express Upgrade Stuck At Manual Prompt Due To
 HRC Status Calculation Cache Problem (jonathanhurley)

--
 .../persist/jpa/AmbariJpaPersistModule.java   |  64 ++++---
 .../ambari/annotations/TransactionalLock.java | 148 +++++++++++++++
 .../actionmanager/ActionDBAccessorImpl.java   |  28 ++-
 .../orm/AmbariJpaLocalTxnInterceptor.java     |  45 +++--
 .../orm/TransactionalLockInterceptor.java     |  84 +++++++++
 .../ambari/server/orm/TransactionalLocks.java | 170 ++++++++++++++++++
 .../server/orm/dao/HostRoleCommandDAO.java    | 137 +++++++++-----
 .../stacks/HDP/2.3/upgrades/upgrade-2.4.xml   |   8 +-
 .../annotations/TransactionalLockTest.java    | 126 +++++++++++++
 9 files changed, 710 insertions(+), 100 deletions(-)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/annotations/TransactionalLock.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLockInterceptor.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLocks.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java

diff --git a/ambari-server/src/main/java/com/google/inject/persist/jpa/AmbariJpaPersistModule.java b/ambari-server/src/main/java/com/google/inject/persist/jpa/AmbariJpaPersistModule.java
index 4e4dd35ee2..604546ca05 100644
-- a/ambari-server/src/main/java/com/google/inject/persist/jpa/AmbariJpaPersistModule.java
++ b/ambari-server/src/main/java/com/google/inject/persist/jpa/AmbariJpaPersistModule.java
@@ -17,26 +17,9 @@
  */
 package com.google.inject.persist.jpa;
 
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.google.inject.persist.finder.DynamicFinder;
import com.google.inject.persist.finder.Finder;
import com.google.inject.util.Providers;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor;
import org.apache.ambari.server.orm.AmbariLocalSessionInterceptor;
import org.apache.ambari.server.orm.RequiresSession;

 import static com.google.inject.matcher.Matchers.annotatedWith;
 import static com.google.inject.matcher.Matchers.any;
 
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
 import java.lang.reflect.AccessibleObject;
 import java.lang.reflect.InvocationHandler;
 import java.lang.reflect.Method;
@@ -44,6 +27,27 @@ import java.lang.reflect.Proxy;
 import java.util.List;
 import java.util.Properties;
 
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.annotations.TransactionalLock;
import org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor;
import org.apache.ambari.server.orm.AmbariLocalSessionInterceptor;
import org.apache.ambari.server.orm.TransactionalLockInterceptor;
import org.apache.ambari.server.orm.RequiresSession;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.google.inject.persist.finder.DynamicFinder;
import com.google.inject.persist.finder.Finder;
import com.google.inject.util.Providers;

 /**
  * Copy of guice persist module for local modifications
  */
@@ -60,7 +64,6 @@ public class AmbariJpaPersistModule extends PersistModule {
 
   private Properties properties;
   private MethodInterceptor transactionInterceptor;
  private MethodInterceptor sessionInterceptor;
 
   @Override protected void configurePersistence() {
     bindConstant().annotatedWith(Jpa.class).to(jpaUnit);
@@ -68,8 +71,7 @@ public class AmbariJpaPersistModule extends PersistModule {
     if (null != properties) {
       bind(Properties.class).annotatedWith(Jpa.class).toInstance(properties);
     } else {
      bind(Properties.class).annotatedWith(Jpa.class)
          .toProvider(Providers.<Properties>of(null));
      bind(Properties.class).annotatedWith(Jpa.class).toProvider(Providers.<Properties> of(null));
     }
 
     bind(AmbariJpaPersistService.class).in(Singleton.class);
@@ -77,16 +79,13 @@ public class AmbariJpaPersistModule extends PersistModule {
     bind(PersistService.class).to(AmbariJpaPersistService.class);
     bind(UnitOfWork.class).to(AmbariJpaPersistService.class);
     bind(EntityManager.class).toProvider(AmbariJpaPersistService.class);
    bind(EntityManagerFactory.class)
        .toProvider(JpaPersistService.EntityManagerFactoryProvider.class);


    bind(EntityManagerFactory.class).toProvider(JpaPersistService.EntityManagerFactoryProvider.class);
 
     transactionInterceptor = new AmbariJpaLocalTxnInterceptor();
     requestInjection(transactionInterceptor);
    sessionInterceptor = new AmbariLocalSessionInterceptor();
    requestInjection(sessionInterceptor);
 
    MethodInterceptor sessionInterceptor = new AmbariLocalSessionInterceptor();
    requestInjection(sessionInterceptor);
 
     // Bind dynamic finders.
     for (Class<?> finder : dynamicFinders) {
@@ -95,6 +94,13 @@ public class AmbariJpaPersistModule extends PersistModule {
 
     bindInterceptor(annotatedWith(RequiresSession.class), any(), sessionInterceptor);
     bindInterceptor(any(), annotatedWith(RequiresSession.class), sessionInterceptor);

    // method-level binding for cross-cutting locks
    // this runs before the base class binds Transactional, so it always runs
    // first
    MethodInterceptor lockAwareInterceptor = new TransactionalLockInterceptor();
    requestInjection(lockAwareInterceptor);
    bindInterceptor(any(), annotatedWith(TransactionalLock.class), lockAwareInterceptor);
   }
 
 
@@ -135,6 +141,7 @@ public class AmbariJpaPersistModule extends PersistModule {
       @Inject
       JpaFinderProxy finderProxy;
 
      @Override
       public Object invoke(final Object thisObject, final Method method, final Object[] args)
           throws Throwable {
 
@@ -146,22 +153,27 @@ public class AmbariJpaPersistModule extends PersistModule {
         }
 
         return finderProxy.invoke(new MethodInvocation() {
          @Override
           public Method getMethod() {
             return method;
           }
 
          @Override
           public Object[] getArguments() {
             return null == args ? new Object[0] : args;
           }
 
          @Override
           public Object proceed() throws Throwable {
             return method.invoke(thisObject, args);
           }
 
          @Override
           public Object getThis() {
             throw new UnsupportedOperationException("Bottomless proxies don't expose a this.");
           }
 
          @Override
           public AccessibleObject getStaticPart() {
             throw new UnsupportedOperationException();
           }
diff --git a/ambari-server/src/main/java/org/apache/ambari/annotations/TransactionalLock.java b/ambari-server/src/main/java/org/apache/ambari/annotations/TransactionalLock.java
new file mode 100644
index 0000000000..cd961ba4b5
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/annotations/TransactionalLock.java
@@ -0,0 +1,148 @@
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
package org.apache.ambari.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

/**
 * The {@link TransactionalLock} annotation is used to provide advice around a
 * joinpoint which will invoke a lock. The lock is invoked before the method
 * begins and is released after it has executed.
 * <p/>
 * This is mainly used in combination with {@link Transactional} methods to
 * provide locking around the entire transaction in order to prevent other
 * methods from performing work before a transaction has completed.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface TransactionalLock {

  /**
   * The logic unit of work being locked.
   *
   * @return
   */
  LockArea lockArea();

  /**
   * @return
   */
  LockType lockType();

  /**
   * The area that the lock is being applied to. There is exactly 1
   * {@link ReadWriteLock} for every area defined.
   */
  public enum LockArea {
    /**
     * Joinpoint lock around work performed on caching the host role command
     * status in a given stage and request.
     */
    HRC_STATUS_CACHE(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED);

    /**
     * Logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(LockArea.class);

    /**
     * The property which governs whether the lock area is enabled or disabled.
     * Because of the inherent nature of deadlocks with interceptors that lock,
     * it's wise to be able to disable a lock area dynamically.
     */
    private String m_configurationProperty;

    /**
     * {@code true} if the lock area is enabled and should be lockable.
     */
    private Boolean m_enabled = null;

    /**
     * Constructor.
     *
     * @param configurationProperty
     */
    private LockArea(String configurationProperty) {
      m_configurationProperty = configurationProperty;
    }

    /**
     * Gets whether this {@link LockArea} is enabled.
     *
     * @param configuration
     *          the configuration to read from (not {@code null}).
     * @return {@code true} if enabled, {@code false} otherwise.
     */
    public boolean isEnabled(Configuration configuration) {
      if (null != m_enabled) {
        return m_enabled.booleanValue();
      }

      // start with TRUE
      m_enabled = Boolean.TRUE;
      String property = configuration.getProperty(m_configurationProperty);

      if (null != property) {
        try {
          m_enabled = Boolean.valueOf(property);
        } catch (Exception exception) {
          LOG.error("Unable to determine if the lock area {} is enabled, defaulting to TRUE",
              m_configurationProperty, exception);
        }
      }

      LOG.info("LockArea {} is {}", name(), m_enabled ? "enabled" : "disabled");
      return m_enabled.booleanValue();
    }

    /**
     * Used for testing to clean the internal state of enabled. This should not
     * be used directly.
     */
    void clearEnabled() {
      m_enabled = null;
    }
  }

  /**
   * The type of lock which should be acquired.
   */
  public enum LockType {
    /**
     * Read Lock.
     */
    READ,

    /**
     * Write lock.
     */
    WRITE;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
index 23686c3cf1..3f4ffeb4a2 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
@@ -198,7 +198,10 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
       }
     }
 
    hostRoleCommandDAO.mergeAll(commands);
    // no need to merge if there's nothing to merge
    if (!commands.isEmpty()) {
      hostRoleCommandDAO.mergeAll(commands);
    }
   }
 
   /* (non-Javadoc)
@@ -214,7 +217,12 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
       command.setStatus(command.isRetryAllowed() ? HostRoleStatus.HOLDING_TIMEDOUT : HostRoleStatus.TIMEDOUT);
       command.setEndTime(now);
     }
    hostRoleCommandDAO.mergeAll(commands);

    // no need to merge if there's nothing to merge
    if (!commands.isEmpty()) {
      hostRoleCommandDAO.mergeAll(commands);
    }

     endRequestIfCompleted(requestId);
   }
 
@@ -475,7 +483,11 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
       }
     }
 
    hostRoleCommandDAO.mergeAll(commandEntities);
    // no need to merge if there's nothing to merge
    if (!commandEntities.isEmpty()) {
      hostRoleCommandDAO.mergeAll(commandEntities);
    }

     // Invalidate cache because of updates to ABORTED commands
     hostRoleCommandCache.invalidateAll(abortedCommandUpdates);
 
@@ -526,7 +538,10 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
       command.setExitcode(report.getExitCode());
     }
 
    hostRoleCommandDAO.mergeAll(commands);
    // no need to merge if there's nothing to merge
    if (!commands.isEmpty()) {
      hostRoleCommandDAO.mergeAll(commands);
    }
 
     if (checkRequest) {
       endRequestIfCompleted(requestId);
@@ -723,7 +738,10 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
       task.setEndTime(-1L);
     }
 
    hostRoleCommandDAO.mergeAll(tasks);
    // no need to merge if there's nothing to merge
    if (!tasks.isEmpty()) {
      hostRoleCommandDAO.mergeAll(tasks);
    }
   }
 
   /**
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java
index 6d7901c465..3c953cacc6 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java
@@ -18,32 +18,38 @@
 
 package org.apache.ambari.server.orm;
 
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import com.google.inject.persist.jpa.AmbariJpaPersistService;
import java.lang.reflect.Method;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

 import org.aopalliance.intercept.MethodInterceptor;
 import org.aopalliance.intercept.MethodInvocation;
 import org.eclipse.persistence.exceptions.EclipseLinkException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import com.google.inject.persist.jpa.AmbariJpaPersistService;
 
 public class AmbariJpaLocalTxnInterceptor implements MethodInterceptor {
 
   private static final Logger LOG = LoggerFactory.getLogger(AmbariJpaLocalTxnInterceptor.class);

   @Inject
   private final AmbariJpaPersistService emProvider = null;

   @Inject
   private final UnitOfWork unitOfWork = null;

   // Tracks if the unit of work was begun implicitly by this transaction.
   private final ThreadLocal<Boolean> didWeStartWork = new ThreadLocal<Boolean>();
 
  @Override
   public Object invoke(MethodInvocation methodInvocation) throws Throwable {
 
     // Should we start a unit of work?
@@ -53,54 +59,57 @@ public class AmbariJpaLocalTxnInterceptor implements MethodInterceptor {
     }
 
     Transactional transactional = readTransactionMetadata(methodInvocation);
    EntityManager em = this.emProvider.get();
    EntityManager em = emProvider.get();
 
     // Allow 'joining' of transactions if there is an enclosing @Transactional method.
     if (em.getTransaction().isActive()) {
       return methodInvocation.proceed();
     }
 
    Object result;

     final EntityTransaction txn = em.getTransaction();
     txn.begin();
 
    Object result;
     try {
       result = methodInvocation.proceed();
 
     } catch (Exception e) {
      //commit transaction only if rollback didn't occur
      // commit transaction only if rollback didn't occur
       if (rollbackIfNecessary(transactional, e, txn)) {
         txn.commit();
       }
 
       detailedLogForPersistenceError(e);
 
      //propagate whatever exception is thrown anyway
      // propagate whatever exception is thrown anyway
       throw e;
     } finally {
      // Close the em if necessary (guarded so this code doesn't run unless catch fired).
      // Close the em if necessary (guarded so this code doesn't run unless
      // catch fired).
       if (null != didWeStartWork.get() && !txn.isActive()) {
         didWeStartWork.remove();
         unitOfWork.end();
       }
     }
 
    //everything was normal so commit the txn (do not move into try block above as it
    //  interferes with the advised method's throwing semantics)
    // everything was normal so commit the txn (do not move into try block
    // above as it
    // interferes with the advised method's throwing semantics)
     try {
       txn.commit();
     } catch (Exception e) {
       detailedLogForPersistenceError(e);
       throw e;
     } finally {
      //close the em if necessary
      // close the em if necessary
       if (null != didWeStartWork.get()) {
         didWeStartWork.remove();
         unitOfWork.end();
       }
     }
 
    //or return result
    // or return result
     return result;
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLockInterceptor.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLockInterceptor.java
new file mode 100644
index 0000000000..0cf73cb440
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLockInterceptor.java
@@ -0,0 +1,84 @@
/*
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

package org.apache.ambari.server.orm;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.annotations.TransactionalLock;
import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

/**
 * The {@link TransactionalLockInterceptor} is a method level intercept which
 * will use the properties of {@link TransactionalLock} to acquire a
 * {@link ReadWriteLock} around a particular {@link LockArea}.
 * <p/>
 * It is mainly used to provide a lock around an method annotated with
 * {@link Transactional}. Consider the case where an action must happen after a
 * method has completed and the transaction has been committed.
 */
public class TransactionalLockInterceptor implements MethodInterceptor {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(TransactionalLockInterceptor.class);

  /**
   * Used to ensure that methods which rely on the completion of
   * {@link Transactional} can detect when they are able to run.
   *
   * @see TransactionalLock
   */
  @Inject
  private final TransactionalLocks transactionLocks = null;

  /**
   * {@inheritDoc}
   */
  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {

    TransactionalLock annotation = methodInvocation.getMethod().getAnnotation(
        TransactionalLock.class);

    LockArea lockArea = annotation.lockArea();
    LockType lockType = annotation.lockType();

    ReadWriteLock rwLock = transactionLocks.getLock(lockArea);
    Lock lock = lockType == LockType.READ ? rwLock.readLock() : rwLock.writeLock();

    lock.lock();

    try {
      Object object = methodInvocation.proceed();
      return object;
    } finally {
      lock.unlock();
    }
  }
}
\ No newline at end of file
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLocks.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLocks.java
new file mode 100644
index 0000000000..1768dd802f
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLocks.java
@@ -0,0 +1,170 @@
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
package org.apache.ambari.server.orm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.server.configuration.Configuration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link TransactionalLocks} class is used to manage the locks associated
 * with each {@link LockArea}. It's a singlegon that shoudl always be injected.
 */
@Singleton
public class TransactionalLocks {

  /**
   * Used to lookup whether {@link LockArea}s are enabled.
   */
  @Inject
  private Configuration m_configuration;

  /**
   * Manages the locks for each class which uses the {@link Transactional}
   * annotation.
   */
  private final ConcurrentHashMap<LockArea, ReadWriteLock> m_locks = new ConcurrentHashMap<>();

  /**
   * Gets a lock for the specified lock area. There is a 1:1 relationship
   * between a lock area and a lock.
   * <p/>
   * If the {@link LockArea} is not enabled, then this will return an empty
   * {@link Lock} implementation which doesn't actually lock anything.
   *
   * @param lockArea
   *          the lock area to get the lock for (not {@code null}).
   * @return the lock to use for the specified lock area (never {@code null}).
   */
  public ReadWriteLock getLock(LockArea lockArea) {
    ReadWriteLock lock = m_locks.get(lockArea);
    if (null == lock) {
      if (lockArea.isEnabled(m_configuration)) {
        lock = new ReentrantReadWriteLock(true);
      } else {
        lock = new NoOperationReadWriteLock();
      }

      m_locks.put(lockArea, lock);
    }

    return lock;
  }

  /**
   * A dummy implementation of a {@link ReadWriteLock} that returns locks which
   * only NOOP. This is used for cases where dependant code doesn't want to
   * {@code if/else} all over the place.
   */
  private final static class NoOperationReadWriteLock implements ReadWriteLock {

    private final Lock m_readLock = new NoOperationLock();
    private final Lock m_writeLock = new NoOperationLock();

    /**
     * {@inheritDoc}
     */
    @Override
    public Lock readLock() {
      return m_readLock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Lock writeLock() {
      return m_writeLock;
    }
  }

  /**
   * A dummy implementation of a {@link Lock} that only NOOPs. This is used for
   * cases where dependant code doesn't want to {@code if/else} all over the
   * place.
   */
  private final static class NoOperationLock implements Lock {

    /**
     * NOOP
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public void lock() {
    }

    /**
     * NOOP
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
    }

    /**
     * NOOP, returns {@code true} always.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public boolean tryLock() {
      return true;
    }

    /**
     * NOOP, returns {@code true} always.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      return true;
    }

    /**
     * NOOP
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public void unlock() {
    }

    @Override
    /**
     * NOOP
     * <p/>
     * {@inheritDoc}
     */
    public Condition newCondition() {
      return null;
    }
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
index deca9b1735..c2ded2f6a1 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
@@ -18,6 +18,9 @@
 
 package org.apache.ambari.server.orm.dao;
 
import static org.apache.ambari.server.orm.DBAccessor.DbType.ORACLE;
import static org.apache.ambari.server.orm.dao.DaoUtils.ORACLE_LIST_LIMIT;

 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Collection;
@@ -26,6 +29,7 @@ import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
 
 import javax.persistence.EntityManager;
 import javax.persistence.TypedQuery;
@@ -33,6 +37,9 @@ import javax.persistence.criteria.CriteriaQuery;
 import javax.persistence.criteria.Order;
 import javax.persistence.metamodel.SingularAttribute;
 
import org.apache.ambari.annotations.TransactionalLock;
import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
 import org.apache.ambari.server.RoleCommand;
 import org.apache.ambari.server.actionmanager.HostRoleStatus;
 import org.apache.ambari.server.api.query.JpaPredicateVisitor;
@@ -43,6 +50,7 @@ import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.SortRequest;
 import org.apache.ambari.server.controller.utilities.PredicateHelper;
 import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.TransactionalLocks;
 import org.apache.ambari.server.orm.entities.HostEntity;
 import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
 import org.apache.ambari.server.orm.entities.HostRoleCommandEntity_;
@@ -60,9 +68,6 @@ import com.google.inject.Singleton;
 import com.google.inject.name.Named;
 import com.google.inject.persist.Transactional;
 
import static org.apache.ambari.server.orm.DBAccessor.DbType.ORACLE;
import static org.apache.ambari.server.orm.dao.DaoUtils.ORACLE_LIST_LIMIT;

 @Singleton
 public class HostRoleCommandDAO {
 
@@ -102,9 +107,17 @@ public class HostRoleCommandDAO {
   private static final String COMPLETED_REQUESTS_SQL = "SELECT DISTINCT task.requestId FROM HostRoleCommandEntity task WHERE task.requestId NOT IN (SELECT task.requestId FROM HostRoleCommandEntity task WHERE task.status IN :notCompletedStatuses) ORDER BY task.requestId {0}";
 
   /**
   * A cache that holds {@link HostRoleCommandStatusSummaryDTO} grouped by stage id for requests by request id.
   * The JPQL computing the host role command status summary for a request is rather expensive
   * thus this cache helps reducing the load on the database
   * A cache that holds {@link HostRoleCommandStatusSummaryDTO} grouped by stage
   * id for requests by request id. The JPQL computing the host role command
   * status summary for a request is rather expensive thus this cache helps
   * reducing the load on the database.
   * <p/>
   * Methods which interact with this cache, including invalidation and
   * population, should use the {@link TransactionalLock} annotation along with
   * the {@link LockArea#HRC_STATUS_CACHE}. This will prevent stale data from
   * being read during a transaction which has updated a
   * {@link HostRoleCommandEntity}'s {@link HostRoleStatus} but has not
   * committed yet.
    */
   private final LoadingCache<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> hrcStatusSummaryCache;
 
@@ -121,6 +134,15 @@ public class HostRoleCommandDAO {
   @Inject
   DaoUtils daoUtils;
 
  /**
   * Used to ensure that methods which rely on the completion of
   * {@link Transactional} can detect when they are able to run.
   *
   * @see TransactionalLock
   */
  @Inject
  private final TransactionalLocks transactionLocks = null;

   public final static String HRC_STATUS_SUMMARY_CACHE_SIZE =  "hostRoleCommandStatusSummaryCacheSize";
   public final static String HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES = "hostRoleCommandStatusCacheExpiryDurationMins";
   public final static String HRC_STATUS_SUMMARY_CACHE_ENABLED =  "hostRoleCommandStatusSummaryCacheEnabled";
@@ -130,12 +152,12 @@ public class HostRoleCommandDAO {
    * @param requestId the key of the cache entry to be invalidated.
    */
   protected void invalidateHostRoleCommandStatusSummaryCache(Long requestId) {
    if (!hostRoleCommandStatusSummaryCacheEnabled )
    if (!hostRoleCommandStatusSummaryCacheEnabled ) {
       return;
    }
 
     LOG.debug("Invalidating host role command status summary cache for request {} !", requestId);
     hrcStatusSummaryCache.invalidate(requestId);

   }
 
   /**
@@ -144,21 +166,23 @@ public class HostRoleCommandDAO {
    * @param hostRoleCommandEntity
    */
   protected void invalidateHostRoleCommandStatusCache(HostRoleCommandEntity hostRoleCommandEntity) {
    if ( !hostRoleCommandStatusSummaryCacheEnabled )
    if ( !hostRoleCommandStatusSummaryCacheEnabled ) {
       return;
    }
 
     if (hostRoleCommandEntity != null) {
       Long requestId = hostRoleCommandEntity.getRequestId();
       if (requestId == null) {
         StageEntity stageEntity = hostRoleCommandEntity.getStage();
        if (stageEntity != null)
        if (stageEntity != null) {
           requestId = stageEntity.getRequestId();
        }
       }
 
      if (requestId != null)
      if (requestId != null) {
         invalidateHostRoleCommandStatusSummaryCache(requestId.longValue());
      }
     }

   }
 
   /**
@@ -170,42 +194,52 @@ public class HostRoleCommandDAO {
    */
   @RequiresSession
   protected Map<Long, HostRoleCommandStatusSummaryDTO> loadAggregateCounts(Long requestId) {

    TypedQuery<HostRoleCommandStatusSummaryDTO> query = entityManagerProvider.get().createQuery(
      SUMMARY_DTO, HostRoleCommandStatusSummaryDTO.class);

    query.setParameter("requestId", requestId);
    query.setParameter("aborted", HostRoleStatus.ABORTED);
    query.setParameter("completed", HostRoleStatus.COMPLETED);
    query.setParameter("failed", HostRoleStatus.FAILED);
    query.setParameter("holding", HostRoleStatus.HOLDING);
    query.setParameter("holding_failed", HostRoleStatus.HOLDING_FAILED);
    query.setParameter("holding_timedout", HostRoleStatus.HOLDING_TIMEDOUT);
    query.setParameter("in_progress", HostRoleStatus.IN_PROGRESS);
    query.setParameter("pending", HostRoleStatus.PENDING);
    query.setParameter("queued", HostRoleStatus.QUEUED);
    query.setParameter("timedout", HostRoleStatus.TIMEDOUT);
    query.setParameter("skipped_failed", HostRoleStatus.SKIPPED_FAILED);

     Map<Long, HostRoleCommandStatusSummaryDTO> map = new HashMap<Long, HostRoleCommandStatusSummaryDTO>();
 
    for (HostRoleCommandStatusSummaryDTO dto : daoUtils.selectList(query)) {
      map.put(dto.getStageId(), dto);
    // ensure that we wait for any running transactions working on this cache to
    // complete
    ReadWriteLock lock = transactionLocks.getLock(LockArea.HRC_STATUS_CACHE);
    lock.readLock().lock();

    try {
      TypedQuery<HostRoleCommandStatusSummaryDTO> query = entityManagerProvider.get().createQuery(
          SUMMARY_DTO, HostRoleCommandStatusSummaryDTO.class);

      query.setParameter("requestId", requestId);
      query.setParameter("aborted", HostRoleStatus.ABORTED);
      query.setParameter("completed", HostRoleStatus.COMPLETED);
      query.setParameter("failed", HostRoleStatus.FAILED);
      query.setParameter("holding", HostRoleStatus.HOLDING);
      query.setParameter("holding_failed", HostRoleStatus.HOLDING_FAILED);
      query.setParameter("holding_timedout", HostRoleStatus.HOLDING_TIMEDOUT);
      query.setParameter("in_progress", HostRoleStatus.IN_PROGRESS);
      query.setParameter("pending", HostRoleStatus.PENDING);
      query.setParameter("queued", HostRoleStatus.QUEUED);
      query.setParameter("timedout", HostRoleStatus.TIMEDOUT);
      query.setParameter("skipped_failed", HostRoleStatus.SKIPPED_FAILED);

      for (HostRoleCommandStatusSummaryDTO dto : daoUtils.selectList(query)) {
        map.put(dto.getStageId(), dto);
      }
    } finally {
      lock.readLock().unlock();
     }
 
     return map;
   }
 
   @Inject
  public HostRoleCommandDAO(@Named(HRC_STATUS_SUMMARY_CACHE_ENABLED) boolean hostRoleCommandStatusSummaryCacheEnabled, @Named(HRC_STATUS_SUMMARY_CACHE_SIZE) long hostRoleCommandStatusSummaryCacheLimit, @Named(HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES) long hostRoleCommandStatusSummaryCacheExpiryDurationMins) {
  public HostRoleCommandDAO(
      @Named(HRC_STATUS_SUMMARY_CACHE_ENABLED) boolean hostRoleCommandStatusSummaryCacheEnabled,
      @Named(HRC_STATUS_SUMMARY_CACHE_SIZE) long hostRoleCommandStatusSummaryCacheLimit,
      @Named(HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES) long hostRoleCommandStatusSummaryCacheExpiryDurationMins) {
     this.hostRoleCommandStatusSummaryCacheEnabled = hostRoleCommandStatusSummaryCacheEnabled;
 
     LOG.info("Host role command status summary cache {} !", hostRoleCommandStatusSummaryCacheEnabled ? "enabled" : "disabled");
 

     hrcStatusSummaryCache = CacheBuilder.newBuilder()
       .maximumSize(hostRoleCommandStatusSummaryCacheLimit)
      .expireAfterAccess(hostRoleCommandStatusSummaryCacheExpiryDurationMins, TimeUnit.MINUTES)
      .expireAfterWrite(hostRoleCommandStatusSummaryCacheExpiryDurationMins, TimeUnit.MINUTES)
       .build(new CacheLoader<Long, Map<Long, HostRoleCommandStatusSummaryDTO>>() {
         @Override
         public Map<Long, HostRoleCommandStatusSummaryDTO> load(Long requestId) throws Exception {
@@ -542,15 +576,19 @@ public class HostRoleCommandDAO {
   }
 
   @Transactional
  public void create(HostRoleCommandEntity stageEntity) {
    entityManagerProvider.get().persist(stageEntity);
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  public void create(HostRoleCommandEntity entity) {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.persist(entity);
 
    invalidateHostRoleCommandStatusCache(stageEntity);
    invalidateHostRoleCommandStatusCache(entity);
   }
 
   @Transactional
  public HostRoleCommandEntity merge(HostRoleCommandEntity stageEntity) {
    HostRoleCommandEntity entity = entityManagerProvider.get().merge(stageEntity);
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  public HostRoleCommandEntity merge(HostRoleCommandEntity entity) {
    EntityManager entityManager = entityManagerProvider.get();
    entity = entityManager.merge(entity);
 
     invalidateHostRoleCommandStatusCache(entity);
 
@@ -566,21 +604,24 @@ public class HostRoleCommandDAO {
   }
 
   @Transactional
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
   public List<HostRoleCommandEntity> mergeAll(Collection<HostRoleCommandEntity> entities) {
     List<HostRoleCommandEntity> managedList = new ArrayList<HostRoleCommandEntity>(entities.size());
     for (HostRoleCommandEntity entity : entities) {
      managedList.add(entityManagerProvider.get().merge(entity));

      EntityManager entityManager = entityManagerProvider.get();
      managedList.add(entityManager.merge(entity));
       invalidateHostRoleCommandStatusCache(entity);
     }

     return managedList;
   }
 
   @Transactional
  public void remove(HostRoleCommandEntity stageEntity) {
    entityManagerProvider.get().remove(merge(stageEntity));

    invalidateHostRoleCommandStatusCache(stageEntity);
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  public void remove(HostRoleCommandEntity entity) {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.remove(merge(entity));
    invalidateHostRoleCommandStatusCache(entity);
   }
 
   @Transactional
@@ -595,10 +636,12 @@ public class HostRoleCommandDAO {
    * @return the map of stage-to-summary objects
    */
   public Map<Long, HostRoleCommandStatusSummaryDTO> findAggregateCounts(Long requestId) {
    if (hostRoleCommandStatusSummaryCacheEnabled)
    if (hostRoleCommandStatusSummaryCacheEnabled) {
       return hrcStatusSummaryCache.getUnchecked(requestId);
    else
    }
    else {
       return loadAggregateCounts(requestId); // if caching not enabled fall back to fetching through JPA
    }
   }
 
 
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/upgrades/upgrade-2.4.xml b/ambari-server/src/main/resources/stacks/HDP/2.3/upgrades/upgrade-2.4.xml
index 29ebc1fa22..f7753145db 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.3/upgrades/upgrade-2.4.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.3/upgrades/upgrade-2.4.xml
@@ -370,7 +370,7 @@
 
     <group xsi:type="cluster" name="FINALIZE_PRE_CHECK" title="Finalize {{direction.text.proper}} Pre-Check">
       <direction>UPGRADE</direction>
      

       <execute-stage title="Check Component Versions">
         <task xsi:type="server_action" class="org.apache.ambari.server.serveraction.upgrades.ComponentVersionCheckAction" />
       </execute-stage>
@@ -548,7 +548,7 @@
           <task xsi:type="restart-task" />
         </upgrade>
       </component>
      

       <component name="PHOENIX_QUERY_SERVER">
         <upgrade>
           <task xsi:type="restart-task"/>
@@ -688,7 +688,7 @@
           <task xsi:type="configure" id="hdp_2_4_0_0_oozie_remove_service_classes" />
 
           <task xsi:type="server_action" summary="Adjusting Oozie properties" class="org.apache.ambari.server.serveraction.upgrades.OozieConfigCalculation"/>
        

           <task xsi:type="execute" hosts="all" sequential="true" summary="Shut down all Oozie servers">
             <script>scripts/oozie_server.py</script>
             <function>stop</function>
@@ -745,7 +745,7 @@
         <pre-upgrade>
           <task xsi:type="configure" id ="hdp_2_4_0_0_kafka_broker_deprecate_port"/>
         </pre-upgrade>
        

         <upgrade>
           <task xsi:type="restart-task" />
         </upgrade>
diff --git a/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java
new file mode 100644
index 0000000000..fbaa343ff5
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java
@@ -0,0 +1,126 @@
/*
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
package org.apache.ambari.annotations;

import java.util.Properties;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.TransactionalLockInterceptor;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.jpa.AmbariJpaPersistModule;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

import junit.framework.Assert;

/**
 * Tests {@link TransactionalLock} and associated classes.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(
    value = { HostRoleCommandDAO.class, AmbariJpaLocalTxnInterceptor.class,
        TransactionalLockInterceptor.class, AmbariJpaPersistModule.class,
        AmbariJpaPersistService.class })
@PowerMockIgnore("javax.management.*")
public class TransactionalLockTest {

  /**
   * Tests that {@link LockArea} is correctly enabled/disabled.
   */
  @Test
  public void testLockAreaEnabled() throws Exception {
    final Properties ambariProperties = new Properties();
    ambariProperties.put(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED, "true");
    Configuration configuration = new Configuration(ambariProperties);

    LockArea lockArea = LockArea.HRC_STATUS_CACHE;
    lockArea.clearEnabled();

    Assert.assertTrue(lockArea.isEnabled(configuration));
  }

  /**
   * Tests that {@link LockArea} is correctly enabled/disabled.
   */
  @Test
  public void testLockAreaEnabledDisabled() throws Exception {
    final Properties ambariProperties = new Properties();
    ambariProperties.put(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED, "false");
    Configuration configuration = new Configuration(ambariProperties);

    LockArea lockArea = LockArea.HRC_STATUS_CACHE;
    lockArea.clearEnabled();

    Assert.assertFalse(lockArea.isEnabled(configuration));
  }

  /**
   * Tests that the {@link Transactional} and {@link TransactionalLock}
   * annotations cause the interceptors to be called in the correct order.
   *
   * @throws Throwable
   */
  @Test
  public void testTransactionLockOrdering() throws Throwable {
    AmbariJpaLocalTxnInterceptor ambariJPAInterceptor = PowerMock.createNiceMock(
        AmbariJpaLocalTxnInterceptor.class);

    TransactionalLockInterceptor lockInterceptor = PowerMock.createNiceMock(
        TransactionalLockInterceptor.class);

    PowerMockito.whenNew(AmbariJpaLocalTxnInterceptor.class).withAnyArguments().thenReturn(
        ambariJPAInterceptor);

    PowerMockito.whenNew(TransactionalLockInterceptor.class).withAnyArguments().thenReturn(
        lockInterceptor);

    Object object = new Object();

    EasyMock.expect(lockInterceptor.invoke(EasyMock.anyObject(MethodInvocation.class))).andReturn(
        object).once();

    EasyMock.expect(
        ambariJPAInterceptor.invoke(EasyMock.anyObject(MethodInvocation.class))).andReturn(
            object).once();

    EasyMock.replay(ambariJPAInterceptor, lockInterceptor);

    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    hostRoleCommandDAO.create(new HostRoleCommandEntity());

    EasyMock.verify(lockInterceptor);
  }

}
- 
2.19.1.windows.1

