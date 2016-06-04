package io.github.notsyncing.cowherd.commons;

/**
 * WebSocket 服务设置信息
 */
public class WebsocketConfig
{
    private boolean enabled;
    private String path;

    /**
     * 获取 WebSocket 服务是否已启用
     * @return WebSocket 服务是否已启用
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * 获取 WebSocket 服务访问路径
     * @return WebSocket 服务访问路径
     */
    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }
}
