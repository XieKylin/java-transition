package com.study;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloTest {

    @Test
    void add_should_return_sum() {
        assertEquals(2, Hello.add(1, 1));
    }

    @Test
    void add_should_handle_negative() {
        assertEquals(-1, Hello.add(2, -3));
    }
}
