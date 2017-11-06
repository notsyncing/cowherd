package io.github.notsyncing.cowherd.flex

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.notsyncing.cowherd.models.ActionMethodInfo
import io.github.notsyncing.cowherd.models.RouteInfo
import io.github.notsyncing.cowherd.routing.RouteManager
import io.vertx.core.http.HttpMethod
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.file.*
import java.util.concurrent.Executors
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.reflect

object CowherdScriptManager {
    private const val SCRIPT_EXT = ".cf.kts"

    const val TAG_SCRIPT_PATH = "cowherd.flex.route_tag.script_path"
    const val TAG_FUNCTION = "cowherd.flex.route_tag.function"
    const val TAG_FUNCTION_CLASS = "cowherd.flex.route_tag.function_class"
    const val TAG_REAL_FUNCTION = "cowherd.flex.route_tag.real_function"
    const val TAG_HTTP_METHOD = "cowherd.flex.route_tag.http_method"

    private lateinit var engine: CowherdScriptEngine

    private val searchPaths = mutableListOf("$")

    var ignoreClasspathScripts = false

    private lateinit var watcher: WatchService
    private lateinit var watchingThread: Thread
    private val watchingPaths = mutableMapOf<WatchKey, Path>()
    private var watching = true

    private var currentScriptPath: String = ""
    private var isInit = true

    private val reloadThread = Executors.newSingleThreadExecutor {
        Thread(it).apply {
            this.name = "cowherd.flex.reloader"
            this.isDaemon = true
        }
    }

    private val logger = Logger.getLogger(javaClass.simpleName)

    fun addSearchPath(p: Path) {
        addSearchPath(p.toAbsolutePath().normalize().toString())
    }

    fun addSearchPath(p: String) {
        if (searchPaths.contains(p)) {
            logger.warning("Script search paths already contain $p, will skip adding it.")
            return
        }

        searchPaths.add(p)

        if (!isInit) {
            loadAndWatchScriptsFromDirectory(p)
        }
    }

    fun init() {
        isInit = true

        engine = KotlinScriptEngine()

        watcher = FileSystems.getDefault().newWatchService()

        watching = true
        watchingThread = thread(name = "cowherd.flex.watcher", isDaemon = true, block = this::watcherThread)

        loadAllScripts()

        isInit = false
    }

    private fun evalScript(reader: Reader, path: String) {
        reader.use {
            currentScriptPath = path

            engine.eval(it)

            currentScriptPath = ""
        }
    }

    private fun evalScript(inputStream: InputStream, path: String) {
        evalScript(InputStreamReader(inputStream), path)
    }

    private fun evalScript(file: Path) {
        val path = file.toAbsolutePath().normalize().toString()
        currentScriptPath = path

        engine.loadScript(file)

        currentScriptPath = ""
    }

    private fun loadAndWatchScriptsFromDirectory(path: String) {
        val p = Paths.get(path)
        var counter = 0

        Files.list(p)
                .filter { it.fileName.toString().endsWith(SCRIPT_EXT) }
                .forEach { f ->
                    logger.info("Loading script $f")

                    evalScript(f)

                    counter++
                }

        val watchKey = p.register(watcher, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY)
        watchingPaths[watchKey] = p

        logger.info("Loaded $counter scripts from $path and added them to the watching list.")
    }

    private fun loadAllScripts() {
        watchingPaths.keys.forEach { it.cancel() }
        watchingPaths.clear()

        for (path in searchPaths) {
            if (path == "$") {
                if (ignoreClasspathScripts) {
                    continue
                }

                val regex = "^(.*?)${SCRIPT_EXT.replace(".", "\\.")}$"

                FastClasspathScanner()
                        .matchFilenamePattern(regex) { relativePath: String, inputStream, _ ->
                            InputStreamReader(inputStream).use {
                                logger.info("Loading script $relativePath from classpath")
                                evalScript(it, relativePath)
                            }
                        }
                        .scan()
            } else {
                loadAndWatchScriptsFromDirectory(path)
            }
        }
    }

    fun registerAction(method: HttpMethod, route: String, code: Function<*>) {
        if (RouteManager.containsRoute(RouteInfo(route))) {
            logger.warning("Route map already contains an action with route $route, the previous one " +
                    "will be overwritten!")
        }

        val routeInfo = RouteInfo(route)
        routeInfo.isFastRoute = true
        routeInfo.setTag(TAG_SCRIPT_PATH, currentScriptPath)
        routeInfo.setTag(TAG_FUNCTION, code.reflect())
        routeInfo.setTag(TAG_FUNCTION_CLASS, code)
        routeInfo.setTag(TAG_REAL_FUNCTION, code.javaClass.methods.firstOrNull { it.name == "invoke" }
                ?.apply { this.isAccessible = true })
        routeInfo.setTag(TAG_HTTP_METHOD, method)

        RouteManager.addRoute(routeInfo, ActionMethodInfo(ScriptActionInvoker::invokeAction.javaMethod))
    }

    fun destroy() {
        watching = false
        watcher.close()

        watchingThread.interrupt()

        RouteManager.removeRouteIf { routeInfo, _ -> routeInfo.hasTag(TAG_SCRIPT_PATH) }
    }

    fun reset() {
        searchPaths.clear()
        searchPaths.add("$")
        watchingPaths.keys.forEach { it.cancel() }
        watchingPaths.clear()

        ignoreClasspathScripts = false
    }

    private fun updateActions(scriptFile: Path, type: WatchEvent.Kind<*>) {
        val iter = RouteManager.getRoutes().entries.iterator()
        val scriptFilePathStr = scriptFile.toString()

        if ((type == StandardWatchEventKinds.ENTRY_MODIFY) || (type == StandardWatchEventKinds.ENTRY_DELETE)) {
            while (iter.hasNext()) {
                val (info, _) = iter.next()
                val path = info.getTag(TAG_SCRIPT_PATH) as String?

                if (path == null) {
                    continue
                }

                if (Files.isDirectory(scriptFile)) {
                    if (Paths.get(path).startsWith(scriptFile)) {
                        iter.remove()
                    }
                } else {
                    if (path == scriptFilePathStr) {
                        iter.remove()
                    }
                }
            }
        }

        if (type != StandardWatchEventKinds.ENTRY_DELETE) {
            evalScript(scriptFile)
        }

        logger.info("Script file $scriptFile updated, type $type.")
    }

    private fun watcherThread() {
        while (watching) {
            val key: WatchKey

            try {
                key = watcher.take()
            } catch (x: InterruptedException) {
                continue
            } catch (x: ClosedWatchServiceException) {
                if (!watching) {
                    continue
                }

                throw x
            }

            for (event in key.pollEvents()) {
                val kind = event.kind()

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    logger.warning("Script watcher overflow, at ${watchingPaths[key]}")
                    continue
                }

                val ev = event as WatchEvent<Path>
                val filename = ev.context()

                if (!filename.toString().endsWith(SCRIPT_EXT)) {
                    continue
                }

                val dir = watchingPaths[key]

                if (dir == null) {
                    logger.warning("We are notified by a path not in the watching list, filename: $filename")
                    continue
                }

                try {
                    val fullPath = dir.resolve(filename)

                    reloadThread.submit {
                        try {
                            updateActions(fullPath, kind)
                        } catch (e: Exception) {
                            logger.warning("An exception occured when reloading script $fullPath: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (x: IOException) {
                    x.printStackTrace()
                    continue
                }
            }

            val valid = key.reset()

            if (!valid) {
                continue
            }
        }
    }
}