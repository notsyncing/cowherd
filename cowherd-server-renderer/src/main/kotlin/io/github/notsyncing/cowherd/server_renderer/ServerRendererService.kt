package io.github.notsyncing.cowherd.server_renderer

import io.github.notsyncing.cowherd.annotations.ContentType
import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.Route
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpAnyMethod
import io.github.notsyncing.cowherd.service.CowherdService
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future

class ServerRendererService : CowherdService() {
    //private val engine = BrowserFactory.getWebKit()

    @Exported
    @HttpAnyMethod
    @ContentType("text/html")
    @Route("", subRoute = true)
    fun process(@Parameter("url") url: String) = future {
        /*val page = engine.navigate(url)
        page.show()
        page.document.body.innerHTML*/

        Connector.get(url).await()
    }
}