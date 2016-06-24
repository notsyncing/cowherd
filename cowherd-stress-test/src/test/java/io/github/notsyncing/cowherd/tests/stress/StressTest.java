package io.github.notsyncing.cowherd.tests.stress;

import com.googlecode.junittoolbox.MultithreadingTester;
import com.googlecode.junittoolbox.RunnableAssert;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.stress.TestApp;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.junit.Before;
import org.junit.Test;

import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

public class StressTest
{
    private TestApp app;

    @Before
    public void setUp() throws InterruptedException
    {
        Thread.sleep(20 * 1000);

        app = new TestApp();
        CowherdConfiguration.setWorkers(1);
        app.run();
    }

    @Test
    public void testSimpleRequest()
    {
        List<Long> times = new ArrayList<>();
        final int[] counter = {0};

        int numReqPerThread = 1;
        int numThreads = 100;

        MultithreadingTester tester = new MultithreadingTester()
                .add(new RunnableAssert("simpleRequest")
                {
                    @Override
                    public void run() throws Exception
                    {
                        try {
                            CloseableHttpClient client = HttpClients.createDefault();
                            HttpGet get = new HttpGet("http://localhost:" + CowherdConfiguration.getListenPort() + "/TestService/simpleRequest");

                            long startTime = System.nanoTime();
                            CloseableHttpResponse resp = client.execute(get);
                            HttpEntity entity = resp.getEntity();
                            String data = IOUtils.toString(entity.getContent(), "utf-8");
                            long endTime = System.nanoTime();

                            times.add(endTime - startTime);

                            assertEquals("Hello, world!", data);
                            counter[0]++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .numRoundsPerThread(numReqPerThread)
                .numThreads(numThreads);

        long allStartTime = System.nanoTime();

        tester.run();

        long allEndTime = System.nanoTime();

        float avgTime = (float)times.stream().mapToLong(l -> l).average().orElse(0) / 1000 / 1000;
        long maxTime = times.stream().mapToLong(l -> l).max().orElse(0) / 1000 / 1000;
        long minTime = times.stream().mapToLong(l -> l).min().orElse(0) / 1000 / 1000;
        long medTime = times.stream().mapToLong(l -> l).sorted().toArray()[times.size() / 2] / 1000 / 1000;

        System.out.println("Total requests: " + counter[0] + "/" + (numReqPerThread * numThreads) + " (" +
                (counter[0] / (numReqPerThread * numThreads * 1.0f) * 100.0f) + "%)");
        System.out.println("All time: " + ((allEndTime - allStartTime) / 1000 / 1000) + " ms");
        System.out.println("Average time: " + avgTime + " ms");
        System.out.println("Max time: " + maxTime + " ms");
        System.out.println("Min time: " + minTime + " ms");
        System.out.println("Median time: " + medTime + " ms");
    }
}
