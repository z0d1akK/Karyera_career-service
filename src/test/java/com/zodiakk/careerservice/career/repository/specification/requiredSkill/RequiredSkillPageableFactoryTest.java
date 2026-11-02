package com.zodiakk.careerservice.career.repository.specification.requiredSkill;

import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequiredSkillPageableFactory")
class RequiredSkillPageableFactoryTest {

    @Test
    @DisplayName("uses the default sort for a default page request")
    void shouldUseDefaultSort() {
        Pageable pageable = RequiredSkillPageableFactory.create(new PageRequestDto());

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertDirection(pageable, "weight", Sort.Direction.ASC);
    }

    @Test
    @DisplayName("falls back to the default sort when sortBy is unknown")
    void shouldFallBackForUnknownSortBy() {
        Pageable pageable = RequiredSkillPageableFactory.create(request("not-a-column", SortDirection.DESC));

        assertDirection(pageable, "weight", Sort.Direction.DESC);
    }

    @Test
    @DisplayName("sorts an allowed field ascending and descending")
    void shouldSortAllowedFieldAscendingAndDescending() {
        assertDirection(
                RequiredSkillPageableFactory.create(request("requiredLevel", SortDirection.ASC)),
                "requiredLevel",
                Sort.Direction.ASC
        );
        assertDirection(
                RequiredSkillPageableFactory.create(request("requiredLevel", SortDirection.DESC)),
                "requiredLevel",
                Sort.Direction.DESC
        );
    }

    private static PageRequestDto request(String sortBy, SortDirection direction) {
        return PageRequestDto.builder()
                .page(0)
                .size(20)
                .sortBy(sortBy)
                .sortDirection(direction)
                .build();
    }

    private static void assertDirection(Pageable pageable, String property, Sort.Direction direction) {
        Sort.Order order = pageable.getSort().getOrderFor(property);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(direction);
    }
}
