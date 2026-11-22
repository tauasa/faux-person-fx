package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GenerateRequestTest {

    @Test
    void blankSeedMeansRandom() {
        assertNull(GenerateRequest.parseSeed(null));
        assertNull(GenerateRequest.parseSeed(""));
        assertNull(GenerateRequest.parseSeed("   "));
    }

    @Test
    void anyLongIsASeed() {
        assertEquals(42L, GenerateRequest.parseSeed(" 42 "));
        assertEquals(-7L, GenerateRequest.parseSeed("-7"));
        assertEquals(Long.MAX_VALUE, GenerateRequest.parseSeed(String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void anythingElseIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> GenerateRequest.parseSeed("abc"));
        assertThrows(IllegalArgumentException.class, () -> GenerateRequest.parseSeed("1.5"));
        assertThrows(IllegalArgumentException.class, () -> GenerateRequest.parseSeed("99999999999999999999"));
    }
}
