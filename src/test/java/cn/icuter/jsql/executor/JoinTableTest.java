package cn.icuter.jsql.executor;

import cn.icuter.jsql.TestUtils;
import cn.icuter.jsql.datasource.JSQLDataSource;
import cn.icuter.jsql.datasource.JdbcExecutorPool;
import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.JSQLException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * @author edward
 * @since 2019-03-16
 */
public class JoinTableTest {
    public static final String TABLE_NAME = "t_orm_test";
    private static JSQLDataSource dataSource;
    private static JdbcExecutorPool pool;

    @BeforeClass
    public static void setup() throws IOException {
        dataSource = TestUtils.getDataSource();
        PoolConfiguration poolConfiguration = PoolConfiguration.defaultPoolCfg();
        poolConfiguration.setMaxPoolSize(3);
        pool = dataSource.createExecutorPool(poolConfiguration);
        try (JdbcExecutor executor = pool.getExecutor()) {
            try {
                dataSource.sql("DROP TABLE " + TABLE_NAME).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
            dataSource.sql(TestUtils.getCreateOrmTableSql()).execUpdate(executor);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Test
    public void testInnerJoin() {
    }
    @Test
    public void testOuterJoin() {
    }
    @Test
    public void testLeftJoin() {
    }
    @Test
    public void testRightJoin() {
    }
    @Test
    public void testFullJoin() {
    }
}
