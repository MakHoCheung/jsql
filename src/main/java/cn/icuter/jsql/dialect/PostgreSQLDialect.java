package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-08-30
 */
public class PostgreSQLDialect implements Dialect {
    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getDialectName() {
        return "postgresql";
    }

    @Override
    public void injectOffsetLimit(BuilderContext builderCtx) {
        Dialects.injectWithLimitOffsetKey(builderCtx);
    }

    @Override
    public boolean supportOffsetLimit() {
        return true;
    }

    @Override
    public boolean supportBlob() {
        return false;
    }

    @Override
    public boolean supportClob() {
        return false;
    }

    @Override
    public boolean supportNClob() {
        return false;
    }
}
