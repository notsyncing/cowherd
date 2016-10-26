package io.github.notsyncing.cowherd.api.tests.toys

class SimpleService {
    fun hello(): String {
        return "Hello, world!"
    }

    fun helloTo(who: String): String {
        return "Hello, $who!"
    }
}