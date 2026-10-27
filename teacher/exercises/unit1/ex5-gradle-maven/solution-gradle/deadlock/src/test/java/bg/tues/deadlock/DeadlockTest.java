package bg.tues.deadlock;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadlockTest {

    @Test
    void originalExampleDeadlocks() throws Exception {
        DeadlockExample.Result result = DeadlockExample.run(0, 500);
        assertTrue(result.deadlocked());
    }

    @Test
    void fixedVersionExchangesMessages() {
        DeadlockExample.Result result = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> DeadlockFixed.run(0));
        assertFalse(result.deadlocked());
        assertEquals("Hello from client", result.serverGot());
        assertEquals("Hello from server", result.clientGot());
    }
}
