package com.termux.app.terminal;

import org.junit.Test;

public class InMemorySessionNewActivityPersistenceTest {

    @Test
    public void shutdownCompletesWithoutThrowing() {
        new InMemorySessionNewActivityPersistence().shutdown();
    }
}
