package cn.icuter.jsql.executor;

import cn.icuter.jsql.TestUtils;
import cn.icuter.jsql.condition.Cond;
import cn.icuter.jsql.datasource.JSQLDataSource;
import cn.icuter.jsql.datasource.JdbcExecutorPool;
import cn.icuter.jsql.datasource.PoolConfiguration;
import cn.icuter.jsql.exception.JSQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author edward
 * @since 2019-03-16
 */
public class JoinTableTest {
    public static final String TABLE_NAME_FIRST = "t_join_f";
    public static final String TABLE_NAME_SECOND = "t_join_s";
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
                dataSource.sql("DROP TABLE " + TABLE_NAME_SECOND).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
            try {
                dataSource.sql("DROP TABLE " + TABLE_NAME_FIRST).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
            dataSource.sql(createSecondTableSql()).execUpdate(executor);
            dataSource.sql(createFirstTableSql()).execUpdate(executor);

            initRecords();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @AfterClass
    public static void tearDown() throws IOException {
        JdbcExecutor executor = pool.getExecutor();
        try {
            try {
                dataSource.sql("DROP TABLE " + TABLE_NAME_SECOND).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
            try {
                dataSource.sql("DROP TABLE " + TABLE_NAME_FIRST).execUpdate(executor);
            } catch (JSQLException e) {
                // ignore
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            pool.returnExecutor(executor);
            pool.close();
            pool = null;
            dataSource = null;
        }
    }

    private static String createFirstTableSql() {
        return "CREATE TABLE " + TABLE_NAME_FIRST + "\n" +
                "(\n" +
                "  f_id VARCHAR(60) NOT NULL,\n" +
                "  t_col VARCHAR(60),\n" +
                "  s_id VARCHAR(60),\n" +
                "  PRIMARY KEY (f_id), \n" +
                "  CONSTRAINT t_s_id_fk\n" +
                "  FOREIGN KEY (s_id) REFERENCES " + TABLE_NAME_SECOND + " (s_id))";
    }

    private static String createSecondTableSql() {
        return "CREATE TABLE " + TABLE_NAME_SECOND + "\n" +
                "(\n" +
                "  s_id VARCHAR(60) NOT NULL,\n" +
                "  t_col VARCHAR(60),\n" +
                "  PRIMARY KEY (s_id))";
    }

    private static void initRecords() throws JSQLException {
        JdbcExecutor executor = pool.getTransactionExecutor();
        try {
            String sId = UUID.randomUUID().toString();
            dataSource.insert(TABLE_NAME_SECOND)
                    .values(
                            Cond.eq("s_id", sId),
                            Cond.eq("t_col", "col_value")
                    ).execUpdate(executor);
            dataSource.insert(TABLE_NAME_FIRST)
                    .values(
                            Cond.eq("f_id", UUID.randomUUID().toString()),
                            Cond.eq("s_id", sId),
                            Cond.eq("t_col", "col_value")
                    ).execUpdate(executor);
        } finally {
            pool.returnExecutor(executor);
        }
    }
    @Test
    public void testInnerJoin() throws JSQLException {
        JdbcExecutor executor = pool.getExecutor();
        try {
            List<Map<String, Object>> joinResultList = dataSource.select()
                    .from(TABLE_NAME_FIRST)
                    .joinOn(TABLE_NAME_SECOND, Cond.var(TABLE_NAME_FIRST + ".s_id", TABLE_NAME_SECOND + ".s_id"))
                    .execQuery(executor);
            System.out.println(joinResultList);
        } finally {
            pool.returnExecutor(executor);
        }
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
