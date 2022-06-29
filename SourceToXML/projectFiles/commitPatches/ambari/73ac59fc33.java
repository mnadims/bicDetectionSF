From 73ac59fc33e64cc1443fc19ba5360d047b0e348a Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Fri, 26 Feb 2016 12:04:23 -0500
Subject: [PATCH] AMBARI-15173 - Express Upgrade Stuck At Manual Prompt Due To
 HRC Status Calculation Cache Problem (part2) (jonathanhurley)

--
 .../persist/jpa/AmbariJpaPersistModule.java   |   9 -
 .../actionmanager/ActionDBAccessor.java       |  10 +-
 .../actionmanager/ActionDBAccessorImpl.java   |   6 +
 .../orm/AmbariJpaLocalTxnInterceptor.java     | 193 ++++++++++++----
 .../orm/TransactionalLockInterceptor.java     |  84 -------
 .../TransactionalLockInterceptorTest.java     | 209 ++++++++++++++++++
 .../annotations/TransactionalLockTest.java    |  84 +++----
 7 files changed, 404 insertions(+), 191 deletions(-)
 delete mode 100644 ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLockInterceptor.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockInterceptorTest.java

diff --git a/ambari-server/src/main/java/com/google/inject/persist/jpa/AmbariJpaPersistModule.java b/ambari-server/src/main/java/com/google/inject/persist/jpa/AmbariJpaPersistModule.java
index 604546ca05..35b0758583 100644
-- a/ambari-server/src/main/java/com/google/inject/persist/jpa/AmbariJpaPersistModule.java
++ b/ambari-server/src/main/java/com/google/inject/persist/jpa/AmbariJpaPersistModule.java
@@ -32,10 +32,8 @@ import javax.persistence.EntityManagerFactory;
 
 import org.aopalliance.intercept.MethodInterceptor;
 import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.annotations.TransactionalLock;
 import org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor;
 import org.apache.ambari.server.orm.AmbariLocalSessionInterceptor;
import org.apache.ambari.server.orm.TransactionalLockInterceptor;
 import org.apache.ambari.server.orm.RequiresSession;
 
 import com.google.common.collect.Lists;
@@ -94,13 +92,6 @@ public class AmbariJpaPersistModule extends PersistModule {
 
     bindInterceptor(annotatedWith(RequiresSession.class), any(), sessionInterceptor);
     bindInterceptor(any(), annotatedWith(RequiresSession.class), sessionInterceptor);

    // method-level binding for cross-cutting locks
    // this runs before the base class binds Transactional, so it always runs
    // first
    MethodInterceptor lockAwareInterceptor = new TransactionalLockInterceptor();
    requestInjection(lockAwareInterceptor);
    bindInterceptor(any(), annotatedWith(TransactionalLock.class), lockAwareInterceptor);
   }
 
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java
index 7f69a31e9a..9aba4c95a6 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java
@@ -26,8 +26,6 @@ import org.apache.ambari.server.agent.CommandReport;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.orm.entities.RequestEntity;
 
import com.google.inject.persist.Transactional;

 public interface ActionDBAccessor {
 
   /**
@@ -88,21 +86,19 @@ public interface ActionDBAccessor {
 
   /**
    * Persists all tasks for a given request
   * @param request request object
   * 
   * @param request
   *          request object
    */
  @Transactional
   void persistActions(Request request) throws AmbariException;
 
  @Transactional
   void startRequest(long requestId);
 
  @Transactional
   void endRequest(long requestId);
 
   /**
    * Updates request with link to source schedule
    */
  @Transactional
   void setSourceScheduleForRequest(long requestId, long scheduleId);
 
   /**
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
index 3f4ffeb4a2..003e2e6b1b 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
@@ -29,6 +29,9 @@ import java.util.concurrent.TimeUnit;
 
 import org.apache.ambari.annotations.Experimental;
 import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.annotations.TransactionalLock;
import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.agent.CommandReport;
 import org.apache.ambari.server.agent.ExecutionCommand;
@@ -282,6 +285,7 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
 
   @Override
   @Transactional
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
   public void persistActions(Request request) throws AmbariException {
 
     RequestEntity requestEntity = request.constructNewPersistenceEntity();
@@ -376,6 +380,7 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
   }
 
   @Override
  @Transactional
   public void startRequest(long requestId) {
     RequestEntity requestEntity = getRequestEntity(requestId);
     if (requestEntity != null && requestEntity.getStartTime() == -1L) {
@@ -385,6 +390,7 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
   }
 
   @Override
  @Transactional
   public void endRequest(long requestId) {
     RequestEntity requestEntity = getRequestEntity(requestId);
     if (requestEntity != null && requestEntity.getEndTime() == -1L) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java
index 3c953cacc6..b5442c2d42 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/AmbariJpaLocalTxnInterceptor.java
@@ -20,6 +20,10 @@ package org.apache.ambari.server.orm;
 
 import java.lang.reflect.Method;
 import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
 
 import javax.persistence.EntityManager;
 import javax.persistence.EntityTransaction;
@@ -27,6 +31,9 @@ import javax.persistence.PersistenceException;
 
 import org.aopalliance.intercept.MethodInterceptor;
 import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.annotations.TransactionalLock;
import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
 import org.eclipse.persistence.exceptions.EclipseLinkException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -36,10 +43,49 @@ import com.google.inject.persist.Transactional;
 import com.google.inject.persist.UnitOfWork;
 import com.google.inject.persist.jpa.AmbariJpaPersistService;
 
/**
 * The {@link AmbariJpaLocalTxnInterceptor} is used to intercept method calls
 * annotated with the {@link Transactional} annotation. If a transaction is not
 * already in progress, then a new transaction is automatically started.
 * Otherwise, the currently active transaction will be reused.
 * <p/>
 * This interceptor also works with {@link TransactionalLock}s to lock on
 * {@link LockArea}s. If this interceptor encounters a {@link TransactionalLock}
 * it will acquire the lock and then add the {@link LockArea} to a collection of
 * areas which need to be released when the transaction is committed or rolled
 * back. This ensures that transactional methods invoke from an already running
 * transaction can have their lock invoked for the lifespan of the outer
 * "parent" transaction.
 */
 public class AmbariJpaLocalTxnInterceptor implements MethodInterceptor {
 
   private static final Logger LOG = LoggerFactory.getLogger(AmbariJpaLocalTxnInterceptor.class);
 
  /**
   * A list of all of the {@link TransactionalLock}s that this interceptor is
   * responsible for. As a thread moves through the system encountering
   * {@link Transactional} and {@link TransactionalLock} methods, this will keep
   * track of which locks the outer-most interceptor will need to release.
   */
  private static final ThreadLocal<LinkedList<TransactionalLock>> s_transactionalLocks = new ThreadLocal<LinkedList<TransactionalLock>>() {
    /**
     * {@inheritDoc}
     */
    @Override
    protected LinkedList<TransactionalLock> initialValue() {
      return new LinkedList<>();
    }
  };

  /**
   * Used to ensure that methods which rely on the completion of
   * {@link Transactional} can detect when they are able to run.
   *
   * @see TransactionalLock
   */
  @Inject
  private final TransactionalLocks transactionLocks = null;

   @Inject
   private final AmbariJpaPersistService emProvider = null;
 
@@ -49,6 +95,9 @@ public class AmbariJpaLocalTxnInterceptor implements MethodInterceptor {
   // Tracks if the unit of work was begun implicitly by this transaction.
   private final ThreadLocal<Boolean> didWeStartWork = new ThreadLocal<Boolean>();
 
  /**
   * {@inheritDoc}
   */
   @Override
   public Object invoke(MethodInvocation methodInvocation) throws Throwable {
 
@@ -61,56 +110,64 @@ public class AmbariJpaLocalTxnInterceptor implements MethodInterceptor {
     Transactional transactional = readTransactionMetadata(methodInvocation);
     EntityManager em = emProvider.get();
 
    // lock the transaction if needed
    lockTransaction(methodInvocation);

     // Allow 'joining' of transactions if there is an enclosing @Transactional method.
     if (em.getTransaction().isActive()) {
       return methodInvocation.proceed();
     }
 
    Object result;
    try {
      // this is the outer-most transactional, begin a transaction
      final EntityTransaction txn = em.getTransaction();
      txn.begin();

      Object result;
      try {
        result = methodInvocation.proceed();

      } catch (Exception e) {
        // commit transaction only if rollback didn't occur
        if (rollbackIfNecessary(transactional, e, txn)) {
          txn.commit();
        }
 
    final EntityTransaction txn = em.getTransaction();
    txn.begin();
        detailedLogForPersistenceError(e);
 
    try {
      result = methodInvocation.proceed();
        // propagate whatever exception is thrown anyway
        throw e;
      } finally {
        // Close the em if necessary (guarded so this code doesn't run unless
        // catch fired).
        if (null != didWeStartWork.get() && !txn.isActive()) {
          didWeStartWork.remove();
          unitOfWork.end();
        }
      }
 
    } catch (Exception e) {
      // commit transaction only if rollback didn't occur
      if (rollbackIfNecessary(transactional, e, txn)) {
      // everything was normal so commit the txn (do not move into try block
      // above as it
      // interferes with the advised method's throwing semantics)
      try {
         txn.commit();
      } catch (Exception e) {
        detailedLogForPersistenceError(e);
        throw e;
      } finally {
        // close the em if necessary
        if (null != didWeStartWork.get()) {
          didWeStartWork.remove();
          unitOfWork.end();
        }
       }
 
      detailedLogForPersistenceError(e);

      // propagate whatever exception is thrown anyway
      throw e;
      // or return result
      return result;
     } finally {
      // Close the em if necessary (guarded so this code doesn't run unless
      // catch fired).
      if (null != didWeStartWork.get() && !txn.isActive()) {
        didWeStartWork.remove();
        unitOfWork.end();
      }
      // unlock all lock areas for this transaction
      unlockTransaction();
     }

    // everything was normal so commit the txn (do not move into try block
    // above as it
    // interferes with the advised method's throwing semantics)
    try {
      txn.commit();
    } catch (Exception e) {
      detailedLogForPersistenceError(e);
      throw e;
    } finally {
      // close the em if necessary
      if (null != didWeStartWork.get()) {
        didWeStartWork.remove();
        unitOfWork.end();
      }
    }

    // or return result
    return result;
   }
 
   private void detailedLogForPersistenceError(Exception e) {
@@ -199,6 +256,68 @@ public class AmbariJpaLocalTxnInterceptor implements MethodInterceptor {
     return commit;
   }
 
  /**
   * Locks the {@link LockArea} specified on the {@link TransactionalLock}
   * annotation if it exists. If the annotation does not exist, then no work is
   * done.
   * <p/>
   * If a lock is acquired, then {@link #s_transactionalLocks} is updated with
   * the lock so that the outer-most interceptor can release all locks when the
   * transaction has completed.
   *
   * @param methodInvocation
   */
  private void lockTransaction(MethodInvocation methodInvocation) {
    TransactionalLock annotation = methodInvocation.getMethod().getAnnotation(
        TransactionalLock.class);

    // no work to do if the annotation is not present
    if (null == annotation) {
      return;
    }

    // there is a lock area, so acquire the lock
    LockArea lockArea = annotation.lockArea();
    LockType lockType = annotation.lockType();

    ReadWriteLock rwLock = transactionLocks.getLock(lockArea);
    Lock lock = lockType == LockType.READ ? rwLock.readLock() : rwLock.writeLock();

    lock.lock();

    // ensure that we add this lock area, otherwise it will never be released
    // when the outer most transaction is committed
    s_transactionalLocks.get().add(annotation);
  }

  /**
   * Unlocks all {@link LockArea}s associated with this transaction or any of
   * the child transactions which were joined. The order that the locks are
   * released is inverted from the order in which they were acquired.
   */
  private void unlockTransaction(){
    LinkedList<TransactionalLock> annotations = s_transactionalLocks.get();
    if (annotations.isEmpty()) {
      return;
    }

    // iterate through all locks which were encountered during the course of
    // this transaction and release them all now that the transaction is
    // committed; iterate reverse to unlock the most recently locked areas
    Iterator<TransactionalLock> iterator = annotations.descendingIterator();
    while (iterator.hasNext()) {
      TransactionalLock annotation = iterator.next();
      LockArea lockArea = annotation.lockArea();
      LockType lockType = annotation.lockType();

      ReadWriteLock rwLock = transactionLocks.getLock(lockArea);
      Lock lock = lockType == LockType.READ ? rwLock.readLock() : rwLock.writeLock();

      lock.unlock();
      iterator.remove();
    }
  }

   @Transactional
   private static class Internal {
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLockInterceptor.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLockInterceptor.java
deleted file mode 100644
index 0cf73cb440..0000000000
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/TransactionalLockInterceptor.java
++ /dev/null
@@ -1,84 +0,0 @@
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
diff --git a/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockInterceptorTest.java b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockInterceptorTest.java
new file mode 100644
index 0000000000..6ebdc0b6dd
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockInterceptorTest.java
@@ -0,0 +1,209 @@
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.TransactionalLocks;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.Transactional;
import com.google.inject.util.Modules;

/**
 * Tests {@link TransactionalLock} and associated classes.
 */
public class TransactionalLockInterceptorTest {

  private Injector m_injector;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(
        Modules.override(new InMemoryDefaultTestModule()).with(new MockModule()));

    m_injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() throws AmbariException {
    m_injector.getInstance(PersistService.class).stop();
  }

  /**
   * Tests that the {@link Transactional} and {@link TransactionalLock}
   * annotations cause the interceptors to lock the right area.
   *
   * @throws Throwable
   */
  @Test
  public void testTransactionalLockInvocation() throws Throwable {
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
    HostRoleCommandDAO hostRoleCommandDAO = m_injector.getInstance(HostRoleCommandDAO.class);
    hostRoleCommandDAO.mergeAll(new ArrayList<HostRoleCommandEntity>());

    // verify locks are called
    EasyMock.verify(transactionalLocks, readWriteLock, readLock, writeLock);
  }

  /**
   * Tests that a {@link TransactionalLock} called within the constructs of an
   * earlier transaction will still lock.
   *
   * @throws Throwable
   */
  @Test
  public void testNestedTransactional() throws Throwable {
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
    testObject.testNestedLockMethod();

    // verify locks are called
    EasyMock.verify(transactionalLocks, readWriteLock, readLock, writeLock);
  }

  /**
   * Tests that a {@link TransactionalLock} called within the constructs of an
   * earlier transaction will still lock.
   *
   * @throws Throwable
   */
  @Test
  public void testMultipleLocks() throws Throwable {
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

    // another round of expectations
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
    testObject.testMultipleLocks();

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
    }

    public void testMultipleLocks() {
      transactionMethodWithLock();
      transactionMethodWithLock();
    }

    @Transactional
    public void transactionMethod() {
    }

    @Transactional
    @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
    public void transactionMethodWithLock() {
    }
  }

  /**
  *
  */
  private class MockModule implements Module {
    /**
    *
    */
    @Override
    public void configure(Binder binder) {
      binder.bind(TransactionalLocks.class).toInstance(
          EasyMock.createNiceMock(TransactionalLocks.class));
    }
  }
}
diff --git a/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java
index fbaa343ff5..186208879f 100644
-- a/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/annotations/TransactionalLockTest.java
@@ -17,42 +17,20 @@
  */
 package org.apache.ambari.annotations;
 
import java.lang.reflect.Method;
import java.util.HashSet;
 import java.util.Properties;
 
import org.aopalliance.intercept.MethodInvocation;
 import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
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
@@ -86,41 +64,39 @@ public class TransactionalLockTest {
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
  public void testAnnotationEquality() {
    HashSet<TransactionalLock> annotations = new HashSet<>();

    int annotationsFound = 0;
    Method[] methods = getClass().getDeclaredMethods();
    for (Method method : methods) {
      TransactionalLock annotation = method.getAnnotation(TransactionalLock.class);
      if (null != annotation) {
        annotations.add(annotation);
        annotationsFound++;
      }
    }

    // there should be 3 discovered annotations, but only 2 in the hashset since
    // they were collapsed
    Assert.assertEquals(2, annotations.size());
    Assert.assertEquals(3, annotationsFound);
  }
 
    EasyMock.expect(
        ambariJPAInterceptor.invoke(EasyMock.anyObject(MethodInvocation.class))).andReturn(
            object).once();
 
    EasyMock.replay(ambariJPAInterceptor, lockInterceptor);
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.READ)
  private void transactionalHRCRead() {
  }
 
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    hostRoleCommandDAO.create(new HostRoleCommandEntity());
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.READ)
  private void transactionalHRCRead2() {
  }
 
    EasyMock.verify(lockInterceptor);
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  private void transactionalHRCWrite() {
   }
 
}
}
\ No newline at end of file
- 
2.19.1.windows.1

