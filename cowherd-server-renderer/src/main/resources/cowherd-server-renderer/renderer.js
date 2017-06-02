var webpage = require("webpage");
var system = require("system");
var server = require("webserver");

function createPage() {
    var page = webpage.create();

    page.onConsoleMessage = function (msg, ln, sid) {
        console.log("CONSOLE: " + ln + ": " + msg + " (" + sid + ")");
    };

    page.onError = function(msg, trace) {
        var msgStack = ['ERROR: ' + msg];

        if (trace && trace.length) {
            msgStack.push('TRACE:');
            trace.forEach(function(t) {
                msgStack.push(' -> ' + t.file + ': ' + t.line + (t.function ? ' (in function "' + t.function +'")' : ''));
            });
        }

        console.error(msgStack.join('\n'));
    };

    page.onInitialized = function () {
        page.evaluate(function () {
            document.addEventListener("DOMContentLoaded", function () {
                if (document.querySelector(".cowherd-server-renderer-self-call")) {
                    console.info("Self call flag found, wait for app to call renderer.");
                    return;
                }

                var injectedScript = "window.callPhantom();";
                var elem = document.createElement("script");
                elem.appendChild(document.createTextNode(injectedScript));
                document.body.appendChild(elem);
            }, false);
        });
    };

    return page;
}

function fetchUrl(urlToFetch, callback) {
    urlToFetch = decodeURIComponent(urlToFetch);

    console.info("Fetching url " + urlToFetch);

    var timeoutTimer;
    var stop = false;

    var page = createPage();

    page.onCallback = function (data) {
        console.info("Callback from page!");

        setTimeout(function () {
            page.close();
        }, 0);

        if (stop) {
            console.warn("Page " + urlToFetch + " fetched successfully after timeout!");
            return;
        }

        stop = true;
        clearTimeout(timeoutTimer);

        console.info("Page fetched.");
        callback(page.content);
    };

    timeoutTimer = setTimeout(function () {
        stop = true;

        setTimeout(function () {
            page.close();
        }, 0);

        callback(Error("Wait for fetching of URL " + urlToFetch + " time out!"));
    }, 10000);

    page.open(urlToFetch, function (status) {
        console.info("Status changed to " + status);

        if (status != "success") {
            callback(new Error("PhantomJS fetch status: " + status));
        }
    });
}

function parseQuery(qstr) {
    var query = {};
    var a = (qstr[0] === '?' ? qstr.substr(1) : qstr).split('&');
    for (var i = 0; i < a.length; i++) {
        var b = a[i].split('=');
        query[decodeURIComponent(b[0])] = decodeURIComponent(b[1] || '');
    }
    return query;
}

function parseUrl(url) {
    var pathStart = url.indexOf("/", url.indexOf("://") + 3);
    var pathEnd = url.indexOf("?");

    if (pathEnd < 0) {
        pathEnd = url.indexOf("#");

        if (pathEnd < 0) {
            pathEnd = url.length;
        }
    }

    var path = url.substring(pathStart, pathEnd);

    var queryEnd = url.indexOf("#");

    if (queryEnd < 0) {
        queryEnd = url.length;
    }

    var queryString = url.substring(pathEnd + 1, queryEnd);
    var query = parseQuery(queryString);

    return {
        pathname: path,
        query: query
    };
}

var s = server.create();
var service = s.listen(46317, function (req, resp) {
    var u = parseUrl(req.url);
    var params = u.query;

    function writeError(e) {
        console.error("Error: " + e.message + " (fetching " + params.url + ")");
        console.error(JSON.stringify(e));

        resp.statusCode = 500;
        resp.setHeader("Content-Type", "text/plain");
        resp.write("Message: " + e.message + "\n");
        resp.write(JSON.stringify(e));
        resp.write("\n" + e);
        resp.close();
    }

    try {
        if (u.pathname == "/get") {
            fetchUrl(params.url, function (r) {
                if (r instanceof Error) {
                    writeError(r);
                } else {
                    resp.statusCode = 200;
                    resp.setHeader("Content-Type", "text/html");
                    resp.write(r);
                    resp.close();
                }
            });
        } else if (u.pathname == "/exit") {
            resp.statusCode = 200;
            resp.setHeader("Content-Type", "text/html");
            resp.write("Exit");
            resp.close();

            phantom.exit();
        } else if (u.pathname == "/ping") {
            resp.statusCode = 200;
            resp.setHeader("Content-Type", "text/html");
            resp.write("Alive");
            resp.close();
        } else {
            resp.statusCode = 404;
            resp.write("404 Not found");
            resp.close();
        }
    } catch (e) {
        writeError(e);
    }
});
