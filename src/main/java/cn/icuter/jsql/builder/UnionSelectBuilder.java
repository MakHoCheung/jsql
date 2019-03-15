package cn.icuter.jsql.builder;

import cn.icuter.jsql.dialect.Dialect;
import cn.icuter.jsql.dialect.Dialects;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author edward
 * @since 2018-12-01
 */
public class UnionSelectBuilder extends SelectBuilder {
    private static final String PAGE_TYPE_LIMIT = "limit";
    private static final String PAGE_TYPE_OFFSET_LIMIT = "offsetLimit";
    private String pageType;
    private Dialect unionDialect;
    public UnionSelectBuilder() {
        initUnionDialect();
    }

    public UnionSelectBuilder(Dialect dialect) {
        super(dialect);
        initUnionDialect();
    }

    void initUnionDialect() {
        unionDialect = builderContext.getDialect();
    }

    private List<UnionBuilderDescriptor> unionBuilderDescriptors = new LinkedList<>();

    public static Builder union(Builder... builders) {
        return union(Dialects.UNKNOWN, builders);
    }
    public static Builder unionAll(Builder... builders) {
        return unionAll(Dialects.UNKNOWN, builders);
    }
    public static Builder union(Collection<Builder> builders) {
        return union(Dialects.UNKNOWN, builders);
    }
    public static Builder unionAll(Collection<Builder> builders) {
        return unionAll(Dialects.UNKNOWN, builders);
    }
    public static Builder union(Dialect dialect, Builder... builders) {
        Objects.requireNonNull(builders);
        UnionSelectBuilder unionBuilder = new UnionSelectBuilder(dialect);
        for (Builder builder : builders) {
            UnionBuilderDescriptor descriptor = new UnionBuilderDescriptor();
            descriptor.builder = builder;
            unionBuilder.checkAndSetUnionDialect(builder);
            unionBuilder.unionBuilderDescriptors.add(descriptor);
        }
        return unionBuilder;
    }

    public static Builder union(Dialect dialect, Collection<Builder> builders) {
        Objects.requireNonNull(builders);
        UnionSelectBuilder unionBuilder = new UnionSelectBuilder(dialect);
        List<UnionBuilderDescriptor> descriptorList = builders.stream().map(builder -> {
            UnionBuilderDescriptor descriptor = new UnionBuilderDescriptor();
            descriptor.builder = builder;
            unionBuilder.checkAndSetUnionDialect(builder);
            return descriptor;
        }).collect(Collectors.toList());
        unionBuilder.unionBuilderDescriptors.addAll(descriptorList);
        return unionBuilder;
    }

    public static Builder unionAll(Dialect dialect, Builder... builders) {
        Objects.requireNonNull(builders);
        UnionSelectBuilder unionBuilder = new UnionSelectBuilder(dialect);
        for (Builder builder : builders) {
            UnionBuilderDescriptor descriptor = new UnionBuilderDescriptor();
            descriptor.isUnionAll = true;
            descriptor.builder = builder;
            unionBuilder.checkAndSetUnionDialect(builder);
            unionBuilder.unionBuilderDescriptors.add(descriptor);
        }
        return unionBuilder;
    }

    public static Builder unionAll(Dialect dialect, Collection<Builder> builders) {
        Objects.requireNonNull(builders);
        UnionSelectBuilder unionBuilder = new UnionSelectBuilder(dialect);
        List<UnionBuilderDescriptor> descriptorList = builders.stream().map(builder -> {
            UnionBuilderDescriptor descriptor = new UnionBuilderDescriptor();
            descriptor.isUnionAll = true;
            descriptor.builder = builder;
            unionBuilder.checkAndSetUnionDialect(builder);
            return descriptor;
        }).collect(Collectors.toList());
        unionBuilder.unionBuilderDescriptors.addAll(descriptorList);
        return unionBuilder;
    }

    @Override
    public Builder union(Builder builder) {
        Objects.requireNonNull(builder);
        UnionBuilderDescriptor descriptor = new UnionBuilderDescriptor();
        descriptor.builder = builder;
        checkAndSetUnionDialect(builder);
        unionBuilderDescriptors.add(descriptor);
        return this;
    }

    @Override
    public Builder unionAll(Builder builder) {
        Objects.requireNonNull(builder);
        UnionBuilderDescriptor descriptor = new UnionBuilderDescriptor();
        descriptor.isUnionAll = true;
        descriptor.builder = builder;
        checkAndSetUnionDialect(builder);
        unionBuilderDescriptors.add(descriptor);
        return this;
    }

    private void checkAndSetUnionDialect(Builder builder) {
        int offset = builder.getBuilderContext().getOffset();
        int limit = builder.getBuilderContext().getLimit();
        if (offset > 0) {
            if (unionDialect != Dialects.UNKNOWN) {
                checkDialectSupportOffsetLimit(builder);
            }
            if (!PAGE_TYPE_OFFSET_LIMIT.equals(pageType)) {
                pageType = PAGE_TYPE_OFFSET_LIMIT;
                unionDialect = builder.getBuilderContext().getDialect();
            }
        } else if (limit > 0 && !PAGE_TYPE_OFFSET_LIMIT.equals(pageType)) {
            if (unionDialect != Dialects.UNKNOWN) {
                checkDialectSupportOffsetLimit(builder);
            }
            if (!PAGE_TYPE_LIMIT.equals(pageType)) {
                pageType = PAGE_TYPE_LIMIT;
                unionDialect = builder.getBuilderContext().getDialect();
            }
        }
    }

    private void checkDialectSupportOffsetLimit(Builder builder) {
        Dialect dialect = builder.getBuilderContext().getDialect();
        if (!dialect.supportOffsetLimit()) {
            throw new UnsupportedOperationException(dialect.getDialectName() + " do NOT support for offset and limit operation");
        }
        if (!unionDialect.getDialectName().equals(dialect.getDialectName())) {
            throw new IllegalArgumentException("Dialect do NOT match for " + unionDialect.getDialectName()
                    + " and " + dialect.getDialectName());
        }
    }

    @Override
    public Builder build() {
        if (unionBuilderDescriptors.size() == 0) {
            throw new IllegalArgumentException("Union SQL NOT EXISTS");
        } else {
            SQLStringBuilder unionSQLBuilder = new SQLStringBuilder();
            boolean isMultipleSelectBuilder = unionBuilderDescriptors.size() > 1;
            UnionBuilderDescriptor firstDescriptor = unionBuilderDescriptors.get(0);
            unionSQLBuilder.append((isMultipleSelectBuilder ? "select * from (" : "")
                    + wrapOffsetLimit(firstDescriptor.builder) + (isMultipleSelectBuilder ? ") t" : ""));
            addCondition(firstDescriptor.builder.getConditionList());
            if (isMultipleSelectBuilder) {
                for (int i = 1; i < unionBuilderDescriptors.size(); i++) {
                    UnionBuilderDescriptor descriptor = unionBuilderDescriptors.get(i);
                    unionSQLBuilder.append(descriptor.isUnionAll ? "union all" : "union")
                            .append("select * from (" + wrapOffsetLimit(descriptor.builder) + ") t");
                    addCondition(descriptor.builder.getConditionList());
                }
            }
            builderContext.sqlLevel = unionBuilderDescriptors.size();
            // without alias will cause DB2 subselect compilation error
            unionSQLBuilder.prepend("select * from (").append(")");
            if (!unionDialect.getDialectName().equals(Dialects.DB2.getDialectName())) {
                unionSQLBuilder.append("union_alias_");
            }
            sqlStringBuilder.prepend(unionSQLBuilder.serialize());
        }
        return super.build();
    }

    @Override
    public Builder select(String... columns) {
        throw new UnsupportedOperationException("Please use SelectBuilder instead");
    }

    @Override
    public Builder from(String... tableName) {
        throw new UnsupportedOperationException("Please use SelectBuilder instead");
    }

    private String wrapOffsetLimit(Builder builder) {
        if (PAGE_TYPE_OFFSET_LIMIT.equals(pageType)) {
            if (builder.getBuilderContext().getOffset() > 0) {
                return builder.getSql();
            }
            return unionDialect.wrapOffsetLimit(builder.getBuilderContext(), builder.getSql());
        } else if (PAGE_TYPE_LIMIT.equals(pageType)) {
            if (builder.getBuilderContext().getLimit() > 0) {
                return builder.getSql();
            }
            return unionDialect.wrapLimit(builder.getBuilderContext(), builder.getSql());
        }
        return builder.getSql();
    }

    static class UnionBuilderDescriptor {
        boolean isUnionAll;
        Builder builder;
    }
}
