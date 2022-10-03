package kitchenpos.eatinorders.tobe;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import kitchenpos.eatinorders.tobe.domain.EatInOrder;
import kitchenpos.eatinorders.tobe.domain.OrderLineItem;
import kitchenpos.eatinorders.tobe.domain.OrderLineItems;
import kitchenpos.eatinorders.tobe.domain.OrderTable;
import kitchenpos.eatinorders.tobe.domain.vo.DisplayedMenu;
import kitchenpos.eatinorders.tobe.domain.vo.EatInOrderQuantity;
import kitchenpos.eatinorders.tobe.domain.vo.NumberOfGuests;
import kitchenpos.eatinorders.tobe.dto.MenuDTO;
import kitchenpos.global.vo.Name;
import kitchenpos.global.vo.Price;
import kitchenpos.products.application.FakePurgomalumClient;

public final class OrderFixtures {

    private OrderFixtures() {
    }

    public static OrderTable orderTable(EatInOrder... orders) {
        return new OrderTable(
                name("테이블1"),
                numberOfGuests(1),
                List.of(orders),
                true
        );
    }

    public static EatInOrder eatInOrderOfAccepted(OrderTable orderTable) {
        EatInOrder order = eatInOrder(
                orderLineItems(orderLineItem(1, menu("양념치킨", 19_000L))),
                orderTable
        );
        order.accept();
        return order;
    }

    public static EatInOrder eatInOrderOfServed(OrderTable orderTable) {
        EatInOrder order = eatInOrderOfAccepted(orderTable);
        order.serve();
        return order;
    }

    public static EatInOrder eatInOrderOfCompleted(OrderTable orderTable) {
        EatInOrder order = eatInOrderOfServed(orderTable);
        order.complete();
        return order;
    }

    public static EatInOrder eatInOrder(OrderTable orderTable) {
        return new EatInOrder(
                orderLineItems(orderLineItem(1, menu("양념통닭", 16_000))),
                orderTable
        );
    }

    public static EatInOrder eatInOrder(OrderLineItem... items) {
        OrderTable table = emptyOrderTable("테이블1");
        table.sit();
        return eatInOrder(
                orderLineItems(items),
                table
        );
    }

    public static EatInOrder eatInOrder(OrderLineItems items, OrderTable table) {
        return new EatInOrder(items, table);
    }

    public static OrderLineItem orderLineItem(long quantity, DisplayedMenu menu) {
        return new OrderLineItem(
                new EatInOrderQuantity(quantity),
                menu
        );
    }

    public static OrderLineItems orderLineItems(OrderLineItem... item) {
        return new OrderLineItems(
                List.of(item)
        );
    }

    public static OrderTable emptyOrderTable(String name) {
        return OrderTable.create(name(name));
    }

    public static OrderTable orderTable(long numberOfGuests, boolean occupied) {
        return new OrderTable(
          numberOfGuests(numberOfGuests),
          occupied
        );
    }

    public static DisplayedMenu menu(String name, long price) {
        return new DisplayedMenu(
                UUID.randomUUID(),
                name(name),
                price(price)
        );
    }

    public static DisplayedMenu menu(UUID menuId, long price) {
        return new DisplayedMenu(
                menuId,
                null,
                price(price)
        );
    }

    public static MenuDTO menuDTO(String name, long price) {
        return new MenuDTO(
                UUID.randomUUID(),
                name(name),
                price(price)
        );
    }

    public static NumberOfGuests numberOfGuests(long value) {
        return new NumberOfGuests(value);
    }

    public static Name name(String value) {
        return new Name(value, new FakePurgomalumClient());
    }

    public static Price price(long value) {
        return new Price(new BigDecimal(value));
    }
}