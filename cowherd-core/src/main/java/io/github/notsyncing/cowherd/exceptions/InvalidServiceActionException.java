package io.github.notsyncing.cowherd.exceptions;

/**
 * 遇到无效的服务方法时发生的异常
 */
public class InvalidServiceActionException extends Exception
{
    public InvalidServiceActionException(String message)
    {
        super(message);
    }
}
