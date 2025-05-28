package com.almis.awe.builder.screen.criteria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class PicklistCriteriaBuilderTest {

    @InjectMocks
    private PicklistCriteriaBuilder picklistCriteriaBuilder;

    @Test
    void build() {
        assertNotNull(picklistCriteriaBuilder.build());
    }
}