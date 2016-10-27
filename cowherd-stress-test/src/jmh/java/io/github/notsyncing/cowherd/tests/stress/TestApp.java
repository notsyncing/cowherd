package io.github.notsyncing.cowherd.tests.stress;

import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.spf4j.stackmonitor.JmhFlightRecorderProfiler;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestApp
{
    @State(Scope.Benchmark)
    public static class BenchmarkState
    {
        private Cowherd server = new Cowherd();

        @Setup(Level.Trial)
        public void setUp()
        {
            server.start();
        }

        @TearDown(Level.Trial)
        public void tearDown() throws ExecutionException, InterruptedException
        {
            server.stop().get();
        }
    }

    public static void main(String[] args) throws RunnerException
    {
        CowherdConfiguration.setWorkers(1);

        Options opts = new OptionsBuilder().include(".*" + TestApp.class.getSimpleName() + ".*")
                    .addProfiler(JmhFlightRecorderProfiler.class)
                    .build();

        new Runner(opts).run();
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 10)
    @Measurement(iterations = 10)
    @Fork(1)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void simpleRequestThroughNetwork(BenchmarkState state) throws IOException
    {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:" + CowherdConfiguration.getListenPort() + "/TestService/simpleRequest");
        CloseableHttpResponse resp = client.execute(get);
    }
}
