package io.github.notsyncing.cowherd.flex.tests.scripts

import io.github.notsyncing.cowherd.flex.CF
import kotlinx.coroutines.experimental.future.future

CF.get("/simpleParam") { param: String ->
    "Hello, $param!"
}

CF.get("/simple") {
    future {
        "Hello, world!"
    }
}
