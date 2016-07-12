package io.github.notsyncing.cowherd.service;

import io.github.notsyncing.cowherd.commons.AlternativeCookieHeaderConfig;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.files.FileStorage;
import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.github.notsyncing.cowherd.server.CowherdServer;
import io.github.notsyncing.cowherd.server.CowherdLogger;
import io.github.notsyncing.cowherd.server.RequestExecutor;
import io.github.notsyncing.cowherd.utils.CookieUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.lang.reflect.Method;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 对外服务抽象类
 * 所有服务均应当继承此类
 */
public abstract class CowherdService
{
    /**
     * 将请求转向另一个服务方法
     * @param serviceName 要转向到的服务名称
     * @param actionName 要转向到的方法名称
     * @param request 要转向的请求对象
     * @param parameters 要转向的参数列表
     * @param cookies 要转向的 Cookies
     * @param uploads 要转向的文件上传信息列表
     * @return 指示是否处理完毕的 CompletableFuture 对象
     */
    protected CompletableFuture delegateTo(String serviceName, String actionName, HttpServerRequest request,
                                           List<Pair<String, String>> parameters, List<HttpCookie> cookies,
                                           List<UploadFileInfo> uploads)
    {
        Method m = ServiceManager.getServiceAction(serviceName, actionName);

        if (m == null) {
            request.response()
                    .setStatusCode(404)
                    .setStatusMessage("Action " + serviceName + "." + actionName + " is not found!")
                    .end();

            return CompletableFuture.completedFuture(null);
        }

        return RequestExecutor.executeRequestedAction(m, request, parameters, cookies, uploads)
                .thenApply(ActionResult::getResult);
    }

    /**
     * 向客户端响应中写入一个 Cookie
     * @param request 当前的请求对象
     * @param cookie 要写入的 Cookie
     */
    protected void putCookie(HttpServerRequest request, HttpCookie cookie)
    {
        String cookieString = CookieUtils.cookieToString(cookie);
        request.response().headers().add("Set-Cookie", cookieString);

        AlternativeCookieHeaderConfig ch = CowherdConfiguration.getAlternativeCookieHeaders();

        if ((ch != null) && ((StringUtils.isEmpty(ch.getOnlyOn()))
                || ("true".equals(request.getHeader(ch.getOnlyOn()))))
                && (!StringUtils.isEmpty(ch.getSetCookie()))) {
            request.response().headers().add(ch.getSetCookie(), cookieString);

            if (request.response().headers().contains("Access-Control-Allow-Origin")) {
                request.response().headers().add("Access-Control-Expose-Headers", ch.getSetCookie());
            }
        }
    }

    /**
     * 向客户端响应中写入多个 Cookies
     * @param request 当前的请求对象
     * @param cookies 要写入的 Cookie 列表
     */
    protected void putCookies(HttpServerRequest request, HttpCookie... cookies)
    {
        if (cookies == null) {
            return;
        }

        for (HttpCookie c : cookies) {
            putCookie(request, c);
        }
    }

    /**
     * 获取当前服务器的文件存储对象
     * @return 文件存储对象
     */
    protected FileStorage getFileStorage()
    {
        try {
            return DependencyInjector.getComponent(CowherdServer.class).getFileStorage();
        } catch (Exception e) {
            getLogger().e("Failed to get file storage", e);
        }

        return null;
    }

    /**
     * 获取日志记录器
     * @return 日志记录器
     */
    protected CowherdLogger getLogger()
    {
        return CowherdLogger.getInstance(this);
    }
}
