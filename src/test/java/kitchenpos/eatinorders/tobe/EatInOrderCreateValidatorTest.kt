package kitchenpos.eatinorders.tobe

import kitchenpos.eatinorders.tobe.domain.DefaultEatInOrderCreateValidator
import kitchenpos.eatinorders.tobe.domain.EatInOrderLineItems
import kitchenpos.fixture.EatInOrderFixtures.eatInOrderLineItem
import kitchenpos.fixture.EatInOrderFixtures.orderTable
import kitchenpos.menus.application.tobe.FakeMenuRepository
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class EatInOrderCreateValidatorTest {
    private val menuRepository = FakeMenuRepository()
    private val orderTableRepository = FakeOrderTableRepository
    private val eatInOrderCreateValidator = DefaultEatInOrderCreateValidator(menuRepository, orderTableRepository)

    @Test
    fun `빈 테이블에는 매장주문 생성 불가능`() {
        //given
        val orderTable = orderTable()
        orderTable.clear(AlwaysSuccessOrderTableClearValidator)

        orderTableRepository.save(orderTable)

        //when & then
        assertThatThrownBy { eatInOrderCreateValidator.validate(orderTable.id, EatInOrderLineItems(listOf(eatInOrderLineItem()))) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .withFailMessage("빈 테이블에는 주문을 생성할 수 없습니다: ${orderTable.id}")
    }

    @Test
    fun `주문 항목의 가격과 메뉴의 가격이 `
}
