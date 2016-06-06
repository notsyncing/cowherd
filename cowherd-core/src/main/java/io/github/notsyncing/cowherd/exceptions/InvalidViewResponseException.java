package io.github.notsyncing.cowherd.exceptions;

/**
 * 遇到无效的视图响应时发生的异常
 */
public class InvalidViewResponseException extends Exception
{
    public InvalidViewResponseException(String message)
    {
        super(message);
    }
}
