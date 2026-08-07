package com.linkforge.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Base62EncoderTest {

    @Test
    fun `test encode and decode`() {
        val id1 = 1L
        val encoded1 = Base62Encoder.encode(id1)
        assertEquals("1", encoded1)
        assertEquals(id1, Base62Encoder.decode(encoded1))

        val id2 = 1000L
        val encoded2 = Base62Encoder.encode(id2)
        assertEquals("G8", encoded2) // 1000 / 62 = 16 (G), 1000 % 62 = 8 (8)
        assertEquals(id2, Base62Encoder.decode(encoded2))

        val id3 = 1000000L
        val encoded3 = Base62Encoder.encode(id3)
        assertEquals(id3, Base62Encoder.decode(encoded3))
        
        val id4 = 0L
        val encoded4 = Base62Encoder.encode(id4)
        assertEquals("0", encoded4)
        assertEquals(id4, Base62Encoder.decode(encoded4))
    }
}
