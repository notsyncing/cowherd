package io.github.notsyncing.cowherd.tests.validators;

import io.github.notsyncing.cowherd.validators.HTMLSanitizationValidator;
import io.github.notsyncing.cowherd.validators.LengthValidator;
import io.github.notsyncing.cowherd.validators.annotations.HTMLSanitize;
import io.github.notsyncing.cowherd.validators.annotations.Length;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HTMLSanitizationValidatorTest
{
    private Method someMethod;
    private HTMLSanitizationValidator validator = new HTMLSanitizationValidator();

    private void someMethod(@HTMLSanitize(textOnly = true) String param1,
                            @HTMLSanitize String param2)
    {
    }

    @Before
    public void setUp() throws NoSuchMethodException
    {
        someMethod = this.getClass().getDeclaredMethod("someMethod", String.class, String.class);
    }

    @Test
    public void testValidateTextOnly()
    {
        HTMLSanitize textOnlyAnno = someMethod.getParameters()[0].getAnnotation(HTMLSanitize.class);
        assertEquals("Hello", validator.filter(textOnlyAnno, "<div>Hello</div>"));
    }

    @Test
    public void testValidateHtmlSanitize()
    {
        HTMLSanitize textOnlyAnno = someMethod.getParameters()[1].getAnnotation(HTMLSanitize.class);
        assertEquals("<div>\n Hello\n</div>",
                validator.filter(textOnlyAnno, "<div>Hello</div><script>alert('xxx')</script>"));
    }
}
