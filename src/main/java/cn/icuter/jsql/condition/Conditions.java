package cn.icuter.jsql.condition;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author edward
 * @since 2018-08-05
 */
public class Conditions implements Condition {

    private final Combination combination;
    private List<Condition> conditionList;

    Conditions(Combination combination) {
        this.combination = combination;
        conditionList = new LinkedList<>();
    }

    Conditions addCondition(List<Condition> conditionList) {
        this.conditionList.addAll(conditionList);
        return this;
    }

    Conditions addCondition(Condition ...condition) {
        Collections.addAll(conditionList, condition);
        return this;
    }

    public String toSql() {
        return "(" + conditionList.stream()
                .map(Condition::toSql)
                .collect(Collectors.joining(" " + combination.getSymbol() + " ")) + ")";
    }

    @Override
    public String getField() {
        throw new UnsupportedOperationException("getField does not support in Conditions");
    }

    @Override
    public Object getValue() {
        return conditionList.stream().filter(condition -> condition.prepareType() == PrepareType.PLACEHOLDER.getType())
                .map(Condition::getValue).collect(Collectors.toList());
    }

    @Override
    public int prepareType() {
        return PrepareType.PLACEHOLDER.getType();
    }

}
