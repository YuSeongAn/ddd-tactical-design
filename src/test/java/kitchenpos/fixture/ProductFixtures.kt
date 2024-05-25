package kitchenpos.fixture

import kitchenpos.products.tobe.domain.Product
import kitchenpos.products.tobe.domain.ProductPrice
import java.math.BigDecimal


object ProductFixtures {
    fun product(): Product {
        val price = ProductPrice(BigDecimal.valueOf(10000L))
        return Product("후라이드", price)
    }
}
