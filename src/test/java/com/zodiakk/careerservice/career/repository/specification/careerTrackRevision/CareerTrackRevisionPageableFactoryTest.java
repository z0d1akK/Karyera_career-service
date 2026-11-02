package com.zodiakk.careerservice.career.repository.specification.careerTrackRevision;

import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CareerTrackRevisionPageableFactory")
class CareerTrackRevisionPageableFactoryTest {

    @Test
    @DisplayName("uses the default sort for a default page request")
    void shouldUseDefaultSort() {
        Pageable pageable = CareerTrackRevisionPageableFactory.create(new PageRequestDto());

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertDirection(pageable, "revision", Sort.Direction.ASC);
    }

    @Test
    @DisplayName("falls back to the default sort when sortBy is unknown")
    void shouldFallBackForUnknownSortBy() {
        Pageable pageable = CareerTrackRevisionPageableFactory.create(request("not-a-column", SortDirection.DESC));

        assertDirection(pageable, "revision", Sort.Direction.DESC);
    }

    @Test
    @DisplayName("sorts an allowed field ascending and descending")
    void shouldSortAllowedFieldAscendingAndDescending() {
        assertDirection(
                CareerTrackRevisionPageableFactory.create(request("status", SortDirection.ASC)),
                "status",
                Sort.Direction.ASC
        );
        assertDirection(
                CareerTrackRevisionPageableFactory.create(request("status", SortDirection.DESC)),
                "status",
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
