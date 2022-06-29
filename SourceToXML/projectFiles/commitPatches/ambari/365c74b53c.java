From 365c74b53c653a96fda189846ad5e07ab268e0fc Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Mon, 29 Feb 2016 12:56:35 -0500
Subject: [PATCH] AMBARI-15173 - Express Upgrade Stuck At Manual Prompt Due To
 HRC Status Calculation Cache Problem (part3) (jonathanhurley)

--
 .../actionmanager/ActionDBAccessorImpl.java   |   7 +-
 .../orm/AmbariJpaLocalTxnInterceptor.java     |   5 +
 .../ambari/server/orm/TransactionalLocks.java |  40 ++++---
 .../server/orm/dao/HostRoleCommandDAO.java    | 102 +++++++++++-------
 .../ambari/annotations/LockAreaTest.java      |  78 ++++++++++++++
 .../TransactionalLockInterceptorTest.java     |  80 +++++++++++++-
 .../annotations/TransactionalLockTest.java    |   5 +-
 .../internal/AlertResourceProviderTest.java   |  54 +++++-----
 8 files changed, 280 insertions(+), 91 deletions(-)
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/annotations/LockAreaTest.java

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
index 003e2e6b1b..429f573a36 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
@@ -735,8 +735,6 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
 
   @Override
   public void resubmitTasks(List<Long> taskIds) {
    hostRoleCommandCache.invalidateAll(taskIds);

     List<HostRoleCommandEntity> tasks = hostRoleCommandDAO.findByPKs(taskIds);
     for (HostRoleCommandEntity task : tasks) {
       task.setStatus(HostRoleStatus.PENDING);
@@ -748,6 +746,8 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
     if (!tasks.isEmpty()) {
       hostRoleCommandDAO.mergeAll(tasks);
     }

    hostRoleCommandCache.invalidateAll(taskIds);
   }
 
   /**
@@ -756,8 +756,7 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
    */
   @Subscribe
   public void invalidateCommandCacheOnHostRemove(HostRemovedEvent event) {
    LOG.info("Invalidating command cache on host delete event." );
    LOG.debug("HostRemovedEvent => " + event);
    LOG.info("Invalidating HRC cache after receiveing {}", event);
     hostRoleCommandCache.invalidateAll();
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java
index b5442c2d42..d7ba4631da 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java
@@ -276,6 +276,11 @@ public class AmbariJpaLocalTxnInterceptor implements MethodInterceptor {
       return;
     }
 
    // no need to lock again
    if (s_transactionalLocks.get().contains(annotation)) {
      return;
    }

     // there is a lock area, so acquire the lock
     LockArea lockArea = annotation.lockArea();
     LockType lockType = annotation.lockType();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLocks.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLocks.java
index 1768dd802f..2c35b618f4 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLocks.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLocks.java
@@ -41,29 +41,26 @@ public class TransactionalLocks {
   /**
    * Used to lookup whether {@link LockArea}s are enabled.
    */
  @Inject
  private Configuration m_configuration;
  private final Configuration m_configuration;
 
   /**
    * Manages the locks for each class which uses the {@link Transactional}
    * annotation.
    */
  private final ConcurrentHashMap<LockArea, ReadWriteLock> m_locks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<LockArea, ReadWriteLock> m_locks;

 
   /**
   * Gets a lock for the specified lock area. There is a 1:1 relationship
   * between a lock area and a lock.
   * <p/>
   * If the {@link LockArea} is not enabled, then this will return an empty
   * {@link Lock} implementation which doesn't actually lock anything.
   * Constructor.
    *
   * @param lockArea
   *          the lock area to get the lock for (not {@code null}).
   * @return the lock to use for the specified lock area (never {@code null}).
    */
  public ReadWriteLock getLock(LockArea lockArea) {
    ReadWriteLock lock = m_locks.get(lockArea);
    if (null == lock) {
  @Inject
  private TransactionalLocks(Configuration configuration) {
    m_configuration = configuration;
    m_locks = new ConcurrentHashMap<>();

    for (LockArea lockArea : LockArea.values()) {
      final ReadWriteLock lock;
       if (lockArea.isEnabled(m_configuration)) {
         lock = new ReentrantReadWriteLock(true);
       } else {
@@ -72,8 +69,21 @@ public class TransactionalLocks {
 
       m_locks.put(lockArea, lock);
     }
  }
 
    return lock;
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
    return m_locks.get(lockArea);
   }
 
   /**
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
index c2ded2f6a1..c25606685c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
@@ -26,8 +26,10 @@ import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
import java.util.Set;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.ReadWriteLock;
 
@@ -161,11 +163,29 @@ public class HostRoleCommandDAO {
   }
 
   /**
   * Invalidates those entries in host role command status cache which are dependent on the passed {@link org.apache.ambari.server.orm.entities.HostRoleCommandEntity}
   * entity.
   * Invalidates the host role command status summary cache entry that
   * corresponds to each request.
   *
   * @param requestIds
   *          the requests to invalidate
   */
  protected void invalidateHostRoleCommandStatusSummaryCache(Set<Long> requestIds) {
    for (Long requestId : requestIds) {
      if (null != requestId) {
        invalidateHostRoleCommandStatusSummaryCache(requestId);
      }
    }
  }

  /**
   * Invalidates those entries in host role command status cache which are
   * dependent on the passed
   * {@link org.apache.ambari.server.orm.entities.HostRoleCommandEntity} entity.
   *
    * @param hostRoleCommandEntity
    */
  protected void invalidateHostRoleCommandStatusCache(HostRoleCommandEntity hostRoleCommandEntity) {
  protected void invalidateHostRoleCommandStatusSummaryCache(
      HostRoleCommandEntity hostRoleCommandEntity) {
     if ( !hostRoleCommandStatusSummaryCacheEnabled ) {
       return;
     }
@@ -193,36 +213,28 @@ public class HostRoleCommandDAO {
    * @return the map of stage-to-summary objects
    */
   @RequiresSession
  protected Map<Long, HostRoleCommandStatusSummaryDTO> loadAggregateCounts(Long requestId) {
  private Map<Long, HostRoleCommandStatusSummaryDTO> loadAggregateCounts(Long requestId) {
     Map<Long, HostRoleCommandStatusSummaryDTO> map = new HashMap<Long, HostRoleCommandStatusSummaryDTO>();
 
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
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<HostRoleCommandStatusSummaryDTO> query = entityManager.createQuery(SUMMARY_DTO,
        HostRoleCommandStatusSummaryDTO.class);

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
 
     return map;
@@ -244,9 +256,18 @@ public class HostRoleCommandDAO {
         @Override
         public Map<Long, HostRoleCommandStatusSummaryDTO> load(Long requestId) throws Exception {
           LOG.debug("Cache miss for host role command status summary object for request {}, fetching from JPA", requestId);
          Map<Long, HostRoleCommandStatusSummaryDTO> hrcCommandStatusByStageId = loadAggregateCounts(requestId);
 
          return hrcCommandStatusByStageId;
          // ensure that we wait for any running transactions working on this cache to
          // complete
          ReadWriteLock lock = transactionLocks.getLock(LockArea.HRC_STATUS_CACHE);
          lock.readLock().lock();

          try{
            Map<Long, HostRoleCommandStatusSummaryDTO> hrcCommandStatusByStageId = loadAggregateCounts(requestId);
            return hrcCommandStatusByStageId;
          } finally {
            lock.readLock().unlock();
          }
         }
       });
   }
@@ -581,7 +602,7 @@ public class HostRoleCommandDAO {
     EntityManager entityManager = entityManagerProvider.get();
     entityManager.persist(entity);
 
    invalidateHostRoleCommandStatusCache(entity);
    invalidateHostRoleCommandStatusSummaryCache(entity);
   }
 
   @Transactional
@@ -590,7 +611,7 @@ public class HostRoleCommandDAO {
     EntityManager entityManager = entityManagerProvider.get();
     entity = entityManager.merge(entity);
 
    invalidateHostRoleCommandStatusCache(entity);
    invalidateHostRoleCommandStatusSummaryCache(entity);
 
     return entity;
   }
@@ -606,13 +627,18 @@ public class HostRoleCommandDAO {
   @Transactional
   @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
   public List<HostRoleCommandEntity> mergeAll(Collection<HostRoleCommandEntity> entities) {
    Set<Long> requestsToInvalidate = new LinkedHashSet<>();
     List<HostRoleCommandEntity> managedList = new ArrayList<HostRoleCommandEntity>(entities.size());
     for (HostRoleCommandEntity entity : entities) {
       EntityManager entityManager = entityManagerProvider.get();
      managedList.add(entityManager.merge(entity));
      invalidateHostRoleCommandStatusCache(entity);
      entity = entityManager.merge(entity);
      managedList.add(entity);

      requestsToInvalidate.add(entity.getRequestId());
     }
 
    invalidateHostRoleCommandStatusSummaryCache(requestsToInvalidate);

     return managedList;
   }
 
@@ -621,7 +647,7 @@ public class HostRoleCommandDAO {
   public void remove(HostRoleCommandEntity entity) {
     EntityManager entityManager = entityManagerProvider.get();
     entityManager.remove(merge(entity));
    invalidateHostRoleCommandStatusCache(entity);
    invalidateHostRoleCommandStatusSummaryCache(entity);
   }
 
   @Transactional
diff --git a/ambari-server/src/test/java/org/apache/ambari/annotations/LockAreaTest.java b/ambari-server/src/test/java/org/apache/ambari/annotations/LockAreaTest.java
new file mode 100644
index 0000000000..2208ce48db
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/annotations/LockAreaTest.java
@@ -0,0 +1,78 @@
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.TransactionalLocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

/**
 * Tests {@link TransactionalLocks} and {@link LockArea} and associated classes.
 */
public class LockAreaTest {

  private Injector m_injector;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() throws AmbariException {
    m_injector.getInstance(PersistService.class).stop();
  }

  /**
   * Tests that for each {@link LockArea}, there's a single {@link Lock}.
   */
  @Test
  public void testTransactionalLockInstantiation() {
    TransactionalLocks locks = m_injector.getInstance(TransactionalLocks.class);
    List<ReadWriteLock> lockList = new ArrayList<>();
    Set<LockArea> lockAreas = EnumSet.allOf(LockArea.class);
    for (LockArea lockArea : lockAreas) {
      ReadWriteLock lock = locks.getLock(lockArea);
      Assert.assertNotNull(lock);
      lockList.add(lock);
    }

    for (LockArea lockArea : lockAreas) {
      Assert.assertTrue(lockList.contains(locks.getLock(lockArea)));
    }
  }
}
diff --git a/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockInterceptorTest.java b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockInterceptorTest.java
index 6ebdc0b6dd..7d7f47be8a 100644
-- a/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockInterceptorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockInterceptorTest.java
@@ -122,7 +122,7 @@ public class TransactionalLockInterceptorTest {
 
     // invoke method with annotations
     TestObject testObject = m_injector.getInstance(TestObject.class);
    testObject.testNestedLockMethod();
    testObject.testLockMethodAsChildOfActiveTransaction();
 
     // verify locks are called
     EasyMock.verify(transactionalLocks, readWriteLock, readLock, writeLock);
@@ -169,28 +169,98 @@ public class TransactionalLockInterceptorTest {
     EasyMock.verify(transactionalLocks, readWriteLock, readLock, writeLock);
   }
 
  /**
   * Tests that two invocations of a {@link TransactionalLock} with the same
   * {@link TransactionalLock} will only lock once on the {@link LockArea}.
   *
   * @throws Throwable
   */
  @Test
  public void testNestedMultipleLocks() throws Throwable {
    // create mocks
    TransactionalLocks transactionalLocks = m_injector.getInstance(TransactionalLocks.class);
    ReadWriteLock readWriteLock = EasyMock.createStrictMock(ReadWriteLock.class);
    Lock readLock = EasyMock.createStrictMock(Lock.class);
    Lock writeLock = EasyMock.createStrictMock(Lock.class);

    // expectations
    EasyMock.expect(transactionalLocks.getLock(LockArea.HRC_STATUS_CACHE)).andReturn(readWriteLock).times(2);
    EasyMock.expect(readWriteLock.writeLock()).andReturn(writeLock).times(2);
    writeLock.lock();
    EasyMock.expectLastCall().once();
    writeLock.unlock();
    EasyMock.expectLastCall().once();

    // replay
    EasyMock.replay(transactionalLocks, readWriteLock, readLock, writeLock);

    // invoke method with annotations
    TestObject testObject = m_injector.getInstance(TestObject.class);
    testObject.testMultipleNestedLocks();

    // verify locks are called
    EasyMock.verify(transactionalLocks, readWriteLock, readLock, writeLock);
  }

   /**
    * A test object which has methods annotated for use with this test class.
    */
   public static class TestObject {
    public void testNestedLockMethod() {
      transactionMethod();
      transactionMethodWithLock();
    /**
     * Calls:
     * <ul>
     * <li>@Transactional</li>
     * <li>-> @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType
     * = LockType.WRITE)</li>
     * </ul>
     */
    public void testLockMethodAsChildOfActiveTransaction() {
      transactionMethodCallingAnotherWithLock();
     }
 
    /**
     * Calls:
     * <ul>
     * <li>@TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType =
     * LockType.WRITE)</li>
     * <li>@TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType =
     * LockType.WRITE)</li>
     * </ul>
     */
     public void testMultipleLocks() {
       transactionMethodWithLock();
       transactionMethodWithLock();
     }
 
    /**
     * Calls:
     * <ul>
     * <li>@TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType =
     * LockType.WRITE)</li>
     * <li>-> @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType
     * = LockType.WRITE)</li>
     * </ul>
     */
    public void testMultipleNestedLocks() {
      transactionMethodWithLockCallingAnotherWithLock();
    }

     @Transactional
    public void transactionMethod() {
    public void transactionMethodCallingAnotherWithLock() {
      transactionMethodWithLock();
     }
 
     @Transactional
     @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
     public void transactionMethodWithLock() {
     }


    @Transactional
    @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
    public void transactionMethodWithLockCallingAnotherWithLock() {
      transactionMethodWithLock();
    }
   }
 
   /**
diff --git a/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java
index 186208879f..2c4b4450b2 100644
-- a/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java
@@ -64,7 +64,8 @@ public class TransactionalLockTest {
   }
 
   /**
   *
   * Tests that annotations are actually equal (more of a proof of the javadoc
   * than anything).
    */
   @Test
   public void testAnnotationEquality() {
@@ -86,7 +87,6 @@ public class TransactionalLockTest {
     Assert.assertEquals(3, annotationsFound);
   }
 

   @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.READ)
   private void transactionalHRCRead() {
   }
@@ -98,5 +98,4 @@ public class TransactionalLockTest {
   @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
   private void transactionalHRCWrite() {
   }

 }
\ No newline at end of file
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AlertResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AlertResourceProviderTest.java
index 6aa3702de2..34270527b6 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AlertResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AlertResourceProviderTest.java
@@ -17,11 +17,28 @@
  */
 package org.apache.ambari.server.controller.internal;
 
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMORY_URL;
import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMROY_DRIVER;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

 import org.apache.ambari.server.api.query.render.AlertStateSummary;
 import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
 import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer.AlertDefinitionSummary;
@@ -63,26 +80,11 @@ import org.junit.Test;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.core.context.SecurityContextHolder;
 
import javax.persistence.EntityManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMORY_URL;
import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMROY_DRIVER;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
 
 /**
  * Test the AlertResourceProvider class
@@ -721,7 +723,7 @@ public class AlertResourceProviderTest {
       binder.bind(DBAccessor.class).to(DBAccessorImpl.class);
 
       Clusters clusters = EasyMock.createNiceMock(Clusters.class);
      Configuration configuration = EasyMock.createMock(Configuration.class);
      Configuration configuration = EasyMock.createNiceMock(Configuration.class);
 
       binder.bind(Clusters.class).toInstance(clusters);
       binder.bind(Configuration.class).toInstance(configuration);
- 
2.19.1.windows.1

