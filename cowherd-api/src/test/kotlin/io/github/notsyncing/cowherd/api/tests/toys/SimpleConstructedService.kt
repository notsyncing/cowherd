package io.github.notsyncing.cowherd.api.tests.toys

class SimpleConstructedService(private val who: String) {
    fun execute(): String {
        return "Hello, new $who!"
    }
}