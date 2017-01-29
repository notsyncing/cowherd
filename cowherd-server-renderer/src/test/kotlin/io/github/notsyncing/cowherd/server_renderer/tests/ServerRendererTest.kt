package io.github.notsyncing.cowherd.server_renderer.tests

import com.mashape.unirest.http.Unirest
import io.github.notsyncing.cowherd.Cowherd
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder

class ServerRendererTest {
    private lateinit var cowherd: Cowherd

    @Before
    fun setUp() {
        cowherd = Cowherd()
        cowherd.start()
    }

    @After
    fun tearDown() {
        cowherd.stop().get()
    }

    @Test
    fun testServerRendering() {
        val url = "http://localhost:41235/se?url=${URLEncoder.encode("http://localhost:41235/test.html", "utf-8")}"
        val expected = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
    <div id="test">Hello</div>

    <script type="text/javascript">
        window.onload = function () {
            document.getElementById("test").textContent = "Hello";
        }
    </script>
</body>
</html>"""

        Thread.sleep(5000)

        val actual = Unirest.get(url).asString().body

        assertEquals(expected, actual)
    }
}