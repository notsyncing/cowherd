package io.github.notsyncing.cowherd.tests.stress;

import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.api.CowherdApiGatewayService;
import io.github.notsyncing.cowherd.api.CowherdApiHub;
import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.service.ServiceManager;
import org.openjdk.jmh.annotations.*;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TestApiApp
{
    @State(Scope.Benchmark)
    public static class BenchmarkState
    {
        private Cowherd server = new Cowherd();
        private CowherdApiGatewayService service;
        private List<Pair<String, String>> params = new ArrayList<>();
        private URI testUri;

        public BenchmarkState()
        {
            try {
                testUri = URI.create("http://localhost:8080/service/gateway/" +
                        TestApiService.class.getName() + "/helloTo?json=" +
                        URLEncoder.encode("{\"who\":\"everyone\"}", "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        private CowherdApiGatewayService getService() throws IllegalAccessException, InvocationTargetException, InstantiationException
        {
            return (CowherdApiGatewayService) ServiceManager.getServiceInstance(CowherdApiGatewayService.class);
        }

        @Setup(Level.Trial)
        public void setUp() throws IllegalAccessException, InstantiationException, InvocationTargetException
        {
            params.clear();
            params.add(new Pair<>("json", "{\"who\":\"everyone\"}"));

            CowherdApiHub.INSTANCE.reset();
            CowherdApiHub.INSTANCE.publish(TestApiService.class);

            server.start();

            service = getService();
        }

        @TearDown(Level.Trial)
        public void tearDown() throws ExecutionException, InterruptedException
        {
            server.stop().get();
        }
    }

    /*public static void main(String[] args) throws RunnerException
    {
        CowherdConfiguration.setWorkers(1);

        Options opts = new OptionsBuilder().include(".*" + TestApiApp.class.getSimpleName() + ".*")
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
    public void simpleRequest(BenchmarkState state) throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException, ExecutionException, InterruptedException
    {
        state.service.gateway(TestApiService.class.getName() + "/helloTo", null, state.params,
                null, null).get();
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 10)
    @Measurement(iterations = 10)
    @Fork(1)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void findRoute(BenchmarkState state) throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException, ExecutionException, InterruptedException
    {
        RouteManager.findMatchedAction(state.testUri);
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 10)
    @Measurement(iterations = 10)
    @Fork(1)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void reduceUri(BenchmarkState state) throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException, ExecutionException, InterruptedException
    {
        RouteUtils.reduceUri("/a/b/~/c/d", "http://localhost:8080/a/b/~/c/d?id=23&q=awef");
    }*/
}
