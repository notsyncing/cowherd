package io.github.notsyncing.cowherd.stress;

import io.github.notsyncing.cowherd.Cowherd;

public class TestApp
{
    private Cowherd server = new Cowherd();

    public static void main(String[] args)
    {
        TestApp app = new TestApp();
        app.run();
    }

    public void run()
    {
        server.start();
    }
}
