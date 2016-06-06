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
import org.junit.Before;
import org.junit.Test;

import java.rmi.server.ExportException;

import static org.junit.Assert.assertEquals;

public class StressTest
{
    private TestApp app;

    @Before
    public void setUp() throws InterruptedException
    {
        Thread.sleep(20 * 1000);

        app = new TestApp();
        app.run();
    }

    @Test
    public void testSimpleRequest()
    {
        MultithreadingTester tester = new MultithreadingTester()
                .add(new RunnableAssert("simpleRequest")
                {
                    @Override
                    public void run() throws Exception
                    {
                        try {
                            CloseableHttpClient client = HttpClients.createDefault();
                            HttpGet get = new HttpGet("http://localhost:" + CowherdConfiguration.getListenPort() + "/TestService/simpleRequest");
                            CloseableHttpResponse resp = client.execute(get);
                            HttpEntity entity = resp.getEntity();
                            String data = IOUtils.toString(entity.getContent(), "utf-8");
                            assertEquals("Hello, world!", data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .numRoundsPerThread(10)
                .numThreads(1000);

        tester.run();
    }
}
