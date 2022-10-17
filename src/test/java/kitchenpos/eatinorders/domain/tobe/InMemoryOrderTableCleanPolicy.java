package kitchenpos.eatinorders.domain.tobe;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class InMemoryOrderTableCleanPolicy implements OrderTableCleanPolicy {
    private Map<OrderTableId, OrderTable> orderTables = new HashMap<>();

    public void save(final OrderTable orderTable) {
        orderTables.put(orderTable.getId(), orderTable);
    }

    public OrderTable getOrderTable(final OrderTableId orderTableId) {
        return orderTables.get(orderTableId);
    }

    @Override
    public void clear(OrderTableId orderTableId) {
        if (!orderTables.containsKey(orderTableId)) {
            throw new NoSuchElementException();
        }

        orderTables.get(orderTableId).clear();
    }
}