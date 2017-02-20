package io.github.notsyncing.cowherd.tests.stress;

public class TestApiService {
    public String hello() {
        return "Hello, world!";
    }

    public String helloTo(String who) {
        return "Hello, " + who + "!";
    }
}