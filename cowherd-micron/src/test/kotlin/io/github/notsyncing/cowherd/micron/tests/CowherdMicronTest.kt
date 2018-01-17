package io.github.notsyncing.cowherd.micron.tests

import io.github.notsyncing.cowherd.micron.tests.toys.TestMicron
import org.junit.Test

class CowherdMicronTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val micron = TestMicron()
            micron.start()
        }
    }

    @Test
    fun test() {

    }
}