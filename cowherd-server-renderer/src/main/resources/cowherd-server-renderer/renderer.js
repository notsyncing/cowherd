var page = require("webpage").create();
var system = require("system");
var server = require("webserver");

page.onConsoleMessage = function (msg, ln, sid) {
    console.log("CONSOLE: " + ln + ": " + msg + " (" + sid + ")");
};

page.onInitialized = function () {
    page.evaluate(function () {
        document.addEventListener("DOMContentLoaded", function () {
            var injectedScript = "window.callPhantom();";
            var elem = document.createElement("script");
            elem.appendChild(document.createTextNode(injectedScript));
            document.body.appendChild(elem);
        }, false);
    });
};

function fetchUrl(urlToFetch, callback) {
    console.info("Fetching url " + urlToFetch);

    page.onCallback = function (data) {
        console.info("Callback from page!");

        if (stop) {
            console.warn("Page " + urlToFetch + " fetched successfully after timeout!");
            return;
        }

        stop = true;
        clearTimeout(timeoutTimer);

        console.info("Page fetched.");
        callback(page.content);
    };

    var stop = false;

    var timeoutTimer = setTimeout(function () {
        stop = true;
        callback(Error("Wait for fetching of URL " + urlToFetch + " time out!"));
    }, 5000);

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
        }
    } catch (e) {
        writeError(e);
    }
});
