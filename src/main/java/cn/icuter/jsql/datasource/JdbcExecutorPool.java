package cn.icuter.jsql.datasource;

import cn.icuter.jsql.exception.BorrowObjectException;
import cn.icuter.jsql.exception.JSQLException;
import cn.icuter.jsql.exception.PoolCloseException;
import cn.icuter.jsql.exception.ReturnObjectException;
import cn.icuter.jsql.executor.DefaultJdbcExecutor;
import cn.icuter.jsql.executor.JdbcExecutor;
import cn.icuter.jsql.executor.TransactionExecutor;
import cn.icuter.jsql.pool.ObjectPool;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author edward
 * @since 2018-09-13
 */
public class JdbcExecutorPool {

    final ObjectPool<Connection> pool;

    JdbcExecutorPool(ObjectPool<Connection> pool) {
        this.pool = pool;
    }

    public JdbcExecutor getExecutor() {
        try {
            return new ConnectionJdbcExecutor(pool.borrowObject());
        } catch (JSQLException e) {
            throw new BorrowObjectException("getting JdbcExecutor error", e);
        }
    }

    public TransactionExecutor getTransactionExecutor() {
        try {
            Connection connection = pool.borrowObject();
            connection.setAutoCommit(false);
            return new ConnectionTransactionExecutor(connection);
        } catch (SQLException | JSQLException e) {
            throw new BorrowObjectException("getting TransactionExecutor error", e);
        }
    }

    public void returnExecutor(JdbcExecutor executor) {
        try {
            if (executor instanceof ConnectionExecutor) {
                ConnectionExecutor connExecutor = (ConnectionExecutor) executor;
                if (connExecutor.isTransaction()) {
                    if (connExecutor instanceof ConnectionTransactionExecutor) {
                        ConnectionTransactionExecutor transExecutor = (ConnectionTransactionExecutor) connExecutor;
                        if (!transExecutor.wasCommitted() && !transExecutor.wasRolledBack()) {
                            transExecutor.rollback();
                        }
                    }
                    // if transaction did not commit, setAutoCommit(true) will commit automatically
                    connExecutor.getConnection().setAutoCommit(true);
                }
                pool.returnObject(connExecutor.getConnection());
                connExecutor.unlinkConnection();
            }
        } catch (SQLException | JSQLException e) {
            throw new ReturnObjectException("returning Executor error", e);
        }
    }

    public void close() {
        try {
            pool.close();
        } catch (JSQLException e) {
            throw new PoolCloseException("closing ExecutorPool error", e);
        }
    }

    public String debugInfo() {
        return pool.debugInfo();
    }

    class ConnectionJdbcExecutor extends DefaultJdbcExecutor implements ConnectionExecutor {
        private Connection connection;
        ConnectionJdbcExecutor(Connection connection) {
            super(connection);
            this.connection = connection;
        }
        @Override
        public Connection getConnection() {
            return connection;
        }
        @Override
        public void unlinkConnection() {
            connection = null;
        }
        @Override
        public boolean isTransaction() {
            return false;
        }
        @Override
        public JdbcExecutor getExecutor() {
            return this;
        }
        @Override
        public void close() throws IOException {
            returnExecutor(this);
        }
    }

    class ConnectionTransactionExecutor extends TransactionExecutor implements ConnectionExecutor {
        private Connection connection;
        ConnectionTransactionExecutor(Connection connection) {
            super(connection);
            this.connection = connection;
            setStateListener(((transaction, state) -> {
                if (state == State.ERROR) {
                    transaction.rollback();
                }
            }));
        }
        @Override
        public Connection getConnection() {
            return connection;
        }
        @Override
        public void unlinkConnection() {
            connection = null;
        }
        @Override
        public boolean isTransaction() {
            return true;
        }
        @Override
        public JdbcExecutor getExecutor() {
            return this;
        }
        @Override
        public void close() throws IOException {
            returnExecutor(this);
        }
    }

    interface ConnectionExecutor {
        Connection getConnection();
        void unlinkConnection();
        boolean isTransaction();
        JdbcExecutor getExecutor();
    }
}