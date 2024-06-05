package kitchenpos.eatinorders.tobe.domain

import java.util.*

interface EatInOrderRepository {

    fun save(eatInOrder: EatInOrder): EatInOrder

    fun existsByOrderTableIdAndStatusNot(orderTableId: UUID, status: EatInOrderStatus): Boolean
}
