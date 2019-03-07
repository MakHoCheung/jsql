package cn.icuter.jsql.pool;

import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PooledObjectPollTimeoutException;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author edward
 * @since 2018-08-28
 */
public class DefaultObjectPoolTest {

    private static PooledObjectManager<Object> manager;

    @BeforeClass
    public static void setup() {
        manager = new PooledObjectManager<Object>() {
            @Override
            public PooledObject<Object> create() throws JSQLException {
                return new PooledObject<>(new Object());
            }
            @Override
            public void invalid(PooledObject<Object> pooledObject) throws JSQLException {
                // noop
            }
            @Override
            public boolean validate(PooledObject<Object> pooledObject) throws JSQLException {
                return pooledObject.getObject() != null;
            }
        };
    }

    @Test
    public void testPoolStat() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setIdleCheckInterval(Integer.MAX_VALUE); // assume never check idle objects
        DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg);
        DefaultObjectPool.PoolStats poolStats = pool.getPoolStats();
        Object[] borrowedObjects = new Object[cfg.getMaxPoolSize()];
        for (int i = 0; i < borrowedObjects.length; i++) {
            Object obj = pool.borrowObject();
            borrowedObjects[i] = obj;
            assertNotNull(obj);
        }
        assertEquals(poolStats.poolSize, cfg.getMaxPoolSize());
        assertEquals(poolStats.borrowedCnt, borrowedObjects.length);
        assertEquals(poolStats.returnedCnt, 0);
        assertEquals(poolStats.lastAccessTime, System.currentTimeMillis(), 1000L);
        assertEquals(poolStats.invalidCnt, 0);

        for (Object borrowedObject : borrowedObjects) {
            pool.returnObject(borrowedObject);
        }
        assertEquals(poolStats.returnedCnt, borrowedObjects.length);

        pool.close();

        assertEquals(poolStats.invalidCnt, cfg.getMaxPoolSize());
        assertTrue(pool.isPoolClosed());
    }

    @Test
    public void testPoolMaintainer() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setIdleTimeout(500L);        // 0.5s idle timeout
        cfg.setIdleCheckInterval(1000L); // check pre 1s
        try (DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg)) {
            Object[] borrowedObjects = new Object[cfg.getMaxPoolSize()];
            for (int i = 0; i < borrowedObjects.length; i++) {
                Object obj = pool.borrowObject();
                borrowedObjects[i] = obj;
            }
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
            // let pool maintainer try to purge idle object
            Thread.sleep(1200L);
            // no idle objects remove, because all pooled objects has been borrowed
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
            for (Object borrowedObject : borrowedObjects) {
                pool.returnObject(borrowedObject);
            }
            // let pool maintainer try to purge idle object again
            Thread.sleep(1200L);
            assertTrue(pool.isPoolEmpty());
        }

        cfg.setIdleTimeout(-1); // idle object never timeout
        try (DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg)) {
            Object[] borrowedObjects = new Object[cfg.getMaxPoolSize()];
            for (int i = 0; i < borrowedObjects.length; i++) {
                Object obj = pool.borrowObject();
                borrowedObjects[i] = obj;
            }
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
            // let pool maintainer try to purge idle object
            Thread.sleep(1200L);
            // no idle objects remove, because all pooled objects has been borrowed
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
            for (Object borrowedObject : borrowedObjects) {
                pool.returnObject(borrowedObject);
            }
            // let pool maintainer try to purge idle object again
            Thread.sleep(1200L);
            assertEquals(pool.getPoolStats().poolSize, cfg.getMaxPoolSize());
        }
    }

    @Test
    public void testMultiThread() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setIdleTimeout(500L);        // 0.5s idle timeout
        cfg.setIdleCheckInterval(500L); // check pre 1s
        try (DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg)) {
            Thread[] threads = new Thread[80];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    Object obj = null;
                    try {
                        obj = pool.borrowObject();
                        Thread.sleep(1000L);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (obj != null) {
                            try {
                                pool.returnObject(obj);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                threads[i].start();
            }
            for (Thread t : threads) {
                t.join();
            }
            assertTrue(pool.getPoolStats().poolSize <= cfg.getMaxPoolSize());

            Thread.sleep(2000L); // sleep enough time to purge idle objects

            assertTrue(pool.isPoolEmpty());
        }
    }

    @Test
    public void testDefaultConfig() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        try (DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg)) {
            List<Object> pooledObjectList = new LinkedList<>();
            for (int i = 0; i < cfg.getMaxPoolSize(); i++) {
                pooledObjectList.add(pool.borrowObject());
            }
            assertEquals(cfg.getMaxPoolSize(), pool.getPoolStats().getPoolSize());
            assertEquals(cfg.getMaxPoolSize(), pool.getPoolStats().createdCnt);
            assertEquals(cfg.getMaxPoolSize(), pool.getPoolStats().borrowedCnt);
            for (Object obj : pooledObjectList) {
                pool.returnObject(obj);
            }
            assertEquals(cfg.getMaxPoolSize(), pool.getPoolStats().returnedCnt);
        }
    }

    @Test
    public void testNoWait() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setPollTimeout(0);
        cfg.setMaxPoolSize(1);
        // no wait
        try (DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg)) {
            for (int i = 0; i < cfg.getMaxPoolSize(); i++) {
                pool.borrowObject();
            }
            assertNull(pool.borrowObject());
        }
    }

    @Test
    public void testWaitNeverTimeout() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setPollTimeout(-1);
        cfg.setMaxPoolSize(1);
        // never timeout
        try (DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg)) {
            Object firstBorrow = pool.borrowObject();
            new Thread() {
                @Override
                public void run() {
                    try {
                        Object secondBorrow = pool.borrowObject();
                        assertNotNull(secondBorrow);
                    } catch (JSQLException e) {
                        Assume.assumeNoException(e);
                    }
                }
            }.start();
            Thread.sleep(6000L); // assume using the first borrowed object
            pool.returnObject(firstBorrow);
        }
    }

    @Test
    public void testNoIdleTimeout() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setIdleTimeout(0);
        try (DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg)) {
            for (int i = 0; i < cfg.getMaxPoolSize(); i++) {
                Object object = pool.borrowObject();
                pool.returnObject(object);
                assertTrue(pool.isPoolEmpty());
            }
        }
    }

    @Test(expected = PooledObjectPollTimeoutException.class)
    public void testBorrowObjectTimeout() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        cfg.setPollTimeout(100L);
        cfg.setMaxPoolSize(1);
        try (DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg)) {
            pool.borrowObject();
            pool.borrowObject();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testReturnException() throws Exception {
        PoolConfiguration cfg = PoolConfiguration.defaultPoolCfg();
        try (DefaultObjectPool<Object> pool = new DefaultObjectPool<>(manager, cfg)) {
            pool.returnObject(new Object());
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
    }

}
