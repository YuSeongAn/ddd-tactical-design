package kitchenpos.products.tobe.domain;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import kitchenpos.products.infra.PurgomalumClient;
import kitchenpos.util.StringUtils;

@Embeddable
public class ProductName {

    @Column(name = "name", nullable = false)
    private String name;

    protected ProductName() { }

    public ProductName(String name, PurgomalumClient purgomalumClient) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("상품명은 빈 값이 될 수 없습니다.");
        }

        if (purgomalumClient.containsProfanity(name)) {
            throw new IllegalArgumentException("상품명에 비속어가 포함되어 있습니다.");
        }

        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductName that = (ProductName) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String value() {
        return name;
    }
}