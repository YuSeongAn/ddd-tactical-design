package kitchenpos.application;

import static kitchenpos.Fixtures.INVALID_ID;
import static kitchenpos.Fixtures.eatInOrder;
import static kitchenpos.Fixtures.menu;
import static kitchenpos.Fixtures.menuProduct;
import static kitchenpos.Fixtures.orderTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import kitchenpos.Fixtures;
import kitchenpos.menu.tobe.domain.Menu;
import kitchenpos.menu.tobe.domain.MenuRepository;
import kitchenpos.order.tobe.eatinorder.application.EatInOrderService;
import kitchenpos.order.tobe.eatinorder.application.dto.CreateEatInOrderRequest;
import kitchenpos.order.tobe.eatinorder.application.dto.EatInOrderLineItemDto;
import kitchenpos.order.tobe.eatinorder.domain.EatInOrder;
import kitchenpos.order.tobe.eatinorder.domain.EatInOrderRepository;
import kitchenpos.order.tobe.eatinorder.domain.EatInOrderStatus;
import kitchenpos.order.tobe.eatinorder.domain.service.MenuClient;
import kitchenpos.order.tobe.eatinorder.domain.OrderTable;
import kitchenpos.order.tobe.eatinorder.domain.OrderTableRepository;
import kitchenpos.order.tobe.eatinorder.domain.service.EatInOrderCreatePolicy;
import kitchenpos.order.tobe.eatinorder.event.EatInOrderCompleteEvent;
import kitchenpos.order.tobe.eatinorder.infra.MenuClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EatInOrderServiceTest {

    private EatInOrderRepository orderRepository;
    private MenuRepository menuRepository;
    private OrderTableRepository orderTableRepository;
    private EatInOrderService eatInOrderService;
    private EatInOrderCreatePolicy eatInOrderCreatePolicy;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryEatInOrderRepository();
        menuRepository = new InMemoryMenuRepository();
        orderTableRepository = new InMemoryOrderTableRepository();
        MenuClient menuClient = new MenuClientImpl(menuRepository);
        this.eatInOrderCreatePolicy = new EatInOrderCreatePolicy(orderTableRepository);
        eatInOrderService = new EatInOrderService(orderRepository, menuClient, eatInOrderCreatePolicy, applicationEventPublisher);
    }

    @DisplayName("1개 이상의 등록된 메뉴로 매장 주문을 등록할 수 있다.")
    @Test
    void createEatInOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final UUID orderTableId = orderTableRepository.save(orderTable(true, 4)).getId();
        final var expected = createEatInOrderRequest(orderTableId, createOrderLineItemRequest(menuId, 19_000L, 3L));
        final var actual = eatInOrderService.create(expected);
        assertThat(actual).isNotNull();
        assertAll(
            () -> assertThat(actual.getId()).isNotNull(),
            () -> assertThat(actual.getStatus()).isEqualTo(EatInOrderStatus.WAITING),
            () -> assertThat(actual.getOrderDateTime()).isNotNull(),
            () -> assertThat(actual.getOrderLineItems()).hasSize(1),
            () -> assertThat(actual.getOrderTableId()).isEqualTo(expected.getOrderTableId())
        );
    }

    @DisplayName("메뉴가 없으면 등록할 수 없다.")
    @MethodSource("orderLineItems")
    @ParameterizedTest
    void create(final List<EatInOrderLineItemDto> orderLineItems) {
        OrderTable orderTable = orderTableRepository.save(orderTable(true, 2));

        final var expected = createEatInOrderRequest(orderTable.getId(), orderLineItems);
        assertThatThrownBy(() -> eatInOrderService.create(expected))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Arguments> orderLineItems() {
        return Arrays.asList(
            null,
            Arguments.of(Collections.emptyList()),
            Arguments.of(Arrays.asList(createOrderLineItemRequest(INVALID_ID, 19_000L, 3L)))
        );
    }

    @DisplayName("매장 주문은 주문 항목의 수량이 0 미만일 수 있다.")
    @ValueSource(longs = -1L)
    @ParameterizedTest
    void createEatInOrder(final long quantity) {
        final UUID orderTableId = orderTableRepository.save(orderTable(true, 0)).getId();
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final var expected = createEatInOrderRequest(
            orderTableId, createOrderLineItemRequest(menuId, 19_000L, quantity)
        );
        assertDoesNotThrow(() -> eatInOrderService.create(expected));
    }

    @DisplayName("빈 테이블에는 매장 주문을 등록할 수 없다.")
    @Test
    void createEmptyTableEatInOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final UUID orderTableId = orderTableRepository.save(orderTable(false, 0)).getId();
        final var expected = createEatInOrderRequest(
            orderTableId, createOrderLineItemRequest(menuId, 19_000L, 3L)
        );
        assertThatThrownBy(() -> eatInOrderService.create(expected))
            .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("숨겨진 메뉴는 주문할 수 없다.")
    @Test
    void createNotDisplayedMenuOrder() {
        final UUID orderTableId = orderTableRepository.save(orderTable(true, 0)).getId();
        Menu menu = menuRepository.save(menu(19_000L, false, menuProduct()));
        final var expected = createEatInOrderRequest(orderTableId, createOrderLineItemRequest(menu.getId(), menu.getPrice().longValue(), 3L));
        assertThatThrownBy(() -> eatInOrderService.create(expected))
            .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문한 메뉴의 가격은 실제 메뉴 가격과 일치해야 한다.")
    @Test
    void createNotMatchedMenuPriceOrder() {
        final UUID orderTableId = orderTableRepository.save(orderTable(true, 0)).getId();
        final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct())).getId();
        final var expected = createEatInOrderRequest(orderTableId, createOrderLineItemRequest(menuId, 16_000L, 3L));
        assertThatThrownBy(() -> eatInOrderService.create(expected))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문을 접수한다.")
    @Test
    void accept() {
        final UUID orderId = orderRepository.save(eatInOrder(EatInOrderStatus.WAITING, orderTable(true, 4))).getId();
        final var actual = eatInOrderService.accept(orderId);
        assertThat(actual.getStatus()).isEqualTo(EatInOrderStatus.ACCEPTED);
    }

    @DisplayName("접수 대기 중인 주문만 접수할 수 있다.")
    @EnumSource(value = EatInOrderStatus.class, names = "WAITING", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void accept(final EatInOrderStatus status) {
        final UUID orderId = orderRepository.save(eatInOrder(status, orderTable(true, 4))).getId();
        assertThatThrownBy(() -> eatInOrderService.accept(orderId))
            .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문을 서빙한다.")
    @Test
    void serve() {
        final UUID orderId = orderRepository.save(Fixtures.eatInOrder(EatInOrderStatus.ACCEPTED)).getId();
        final var actual = eatInOrderService.serve(orderId);
        assertThat(actual.getStatus()).isEqualTo(EatInOrderStatus.SERVED);
    }

    @DisplayName("접수된 주문만 서빙할 수 있다.")
    @EnumSource(value = EatInOrderStatus.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void serve(final EatInOrderStatus status) {
        final UUID orderId = orderRepository.save(Fixtures.eatInOrder(status)).getId();
        assertThatThrownBy(() -> eatInOrderService.serve(orderId))
            .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문을 완료한다.")
    @Test
    void complete() {
        OrderTable orderTable = Fixtures.emptyOrderTable();
        orderTableRepository.save(orderTable);

        final EatInOrder expected = orderRepository.save(Fixtures.eatInOrder(EatInOrderStatus.SERVED, orderTable));
        final var actual = eatInOrderService.complete(expected.getId());
        assertThat(actual.getStatus()).isEqualTo(EatInOrderStatus.COMPLETED);
    }

    @DisplayName("포장 및 매장 주문의 경우 서빙된 주문만 완료할 수 있다.")
    @EnumSource(value = EatInOrderStatus.class, names = "SERVED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void completeTakeoutAndEatInOrder(final EatInOrderStatus status) {
        final UUID orderId = orderRepository.save(Fixtures.eatInOrder(status)).getId();
        assertThatThrownBy(() -> eatInOrderService.complete(orderId))
            .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("주문 테이블의 모든 매장 주문이 완료되면 빈 테이블로 설정한다.")
    @Test
    void completeEatInOrder() {
        final OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        final EatInOrder expected = orderRepository.save(eatInOrder(EatInOrderStatus.SERVED, orderTable));
        final var actual = eatInOrderService.complete(expected.getId());

        assertAll(
            () -> assertThat(actual.getStatus()).isEqualTo(EatInOrderStatus.COMPLETED),
            () -> verify(applicationEventPublisher, times(1)).publishEvent(any(EatInOrderCompleteEvent.class))
        );
    }

    @DisplayName("완료되지 않은 매장 주문이 있는 주문 테이블은 빈 테이블로 설정하지 않는다.")
    @Test
    void completeNotTable() {
        final OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        orderRepository.save(eatInOrder(EatInOrderStatus.SERVED, orderTable));
        final EatInOrder expected = orderRepository.save(eatInOrder(EatInOrderStatus.SERVED, orderTable));
        final var actual = eatInOrderService.complete(expected.getId());
        assertAll(
            () -> assertThat(actual.getStatus()).isEqualTo(EatInOrderStatus.COMPLETED),
            () -> verify(applicationEventPublisher, never()).publishEvent(any(EatInOrderCompleteEvent.class))
        );
    }

    @DisplayName("주문의 목록을 조회할 수 있다.")
    @Test
    void findAll() {
        final OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        orderRepository.save(eatInOrder(EatInOrderStatus.SERVED, orderTable));
        orderRepository.save(Fixtures.eatInOrder(EatInOrderStatus.SERVED, orderTable));
        final var actual = eatInOrderService.findAll();
        assertThat(actual).hasSize(2);
    }

    private CreateEatInOrderRequest createEatInOrderRequest(final UUID orderTableId, final List<EatInOrderLineItemDto> orderLineItems) {
        return new CreateEatInOrderRequest(orderTableId, orderLineItems);
    }

    private CreateEatInOrderRequest createEatInOrderRequest(
        final UUID orderTableId,
        final EatInOrderLineItemDto... orderLineItems
    ) {
        return new CreateEatInOrderRequest(orderTableId, Arrays.asList(orderLineItems));
    }

    private static EatInOrderLineItemDto createOrderLineItemRequest(final UUID menuId, final long price, final long quantity) {
        final var orderLineItem = new EatInOrderLineItemDto(menuId, BigDecimal.valueOf(price), quantity);
        return orderLineItem;
    }
}