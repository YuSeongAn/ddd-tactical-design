package kitchenpos.eatinorders.tobe.application

import kitchenpos.eatinorders.tobe.domain.*
import kitchenpos.eatinorders.tobe.dto.`in`.EatInOrderCreateRequest
import kitchenpos.eatinorders.tobe.dto.out.EatInOrderResponse
import kitchenpos.eatinorders.tobe.dto.out.fromEatInOrder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EatInOrderService(
    private val eatInOrderRepository: EatInOrderRepository,
    private val eatInOrderCreateValidator: EatInOrderCreateValidator
) {

    @Transactional
    fun createOrder(eatInOrderCreateRequest: EatInOrderCreateRequest): EatInOrderResponse {
        val eatInOrderLineItems = eatInOrderCreateRequest.eatInOrderLineItemCreateRequests
            .map { EatInOrderLineItem(it.menuId, EatInOrderLineItemQuantity(it.quantity), it.price) }
            .let { EatInOrderLineItems(it) }

        val eatInOrder = EatInOrder.of(
            eatInOrderCreateRequest.orderTableId,
            eatInOrderLineItems,
            eatInOrderCreateValidator
        )

        return fromEatInOrder(eatInOrderRepository.save(eatInOrder))
    }

}
