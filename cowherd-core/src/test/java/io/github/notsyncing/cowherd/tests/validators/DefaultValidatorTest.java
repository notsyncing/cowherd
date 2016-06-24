package io.github.notsyncing.cowherd.tests.validators;

import io.github.notsyncing.cowherd.validators.DefaultValidator;
import io.github.notsyncing.cowherd.validators.HTMLSanitizationValidator;
import io.github.notsyncing.cowherd.validators.annotations.Default;
import io.github.notsyncing.cowherd.validators.annotations.HTMLSanitize;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class DefaultValidatorTest
{
    private Method someMethod;
    private DefaultValidator validator = new DefaultValidator();

    private void someMethod(@Default("test") String param1)
    {
    }

    @Before
    public void setUp() throws NoSuchMethodException
    {
        someMethod = this.getClass().getDeclaredMethod("someMethod", String.class);
    }

    @Test
    public void testValidateDefaultValue()
    {
        Default defaultAnno = someMethod.getParameters()[0].getAnnotation(Default.class);
        assertEquals("test", validator.filter(someMethod.getParameters()[0], defaultAnno, null));
    }
}
