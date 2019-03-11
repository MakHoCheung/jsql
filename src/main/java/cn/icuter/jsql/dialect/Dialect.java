package cn.icuter.jsql.dialect;

import cn.icuter.jsql.builder.BuilderContext;

/**
 * @author edward
 * @since 2018-08-29
 */
public interface Dialect {
    String getDriverClassName();
    default String getDialectName() {
        return null;
    }

    default void injectOffsetLimit(BuilderContext builderCtx) {
        throw new UnsupportedOperationException();
    }

    default boolean supportOffsetLimit() {
        return false;
    }

    default String wrapOffsetLimit(BuilderContext builderContext, String sql) {
        return sql;
    }
    default String wrapLimit(BuilderContext builderContext, String sql) {
        return sql;
    }
    default boolean supportConnectionIsValid() {
        return true;
    }
    default String validationSql() {
        return null;
    }
    default boolean supportSavepoint() {
        return true;
    }
    default boolean supportBlob() {
        return true;
    }
    default boolean supportClob() {
        return true;
    }
    default boolean supportNClob() {
        return true;
    }
}
