package io.github.notsyncing.cowherd.exceptions;

/**
 * 请求未能通过验证器的验证时发生的异常
 */
public class AuthenticationFailedException extends RuntimeException
{
    public AuthenticationFailedException() {
        super();
    }

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
