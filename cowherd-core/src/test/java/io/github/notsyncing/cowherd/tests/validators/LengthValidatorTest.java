package io.github.notsyncing.cowherd.tests.validators;

import io.github.notsyncing.cowherd.validators.LengthValidator;
import io.github.notsyncing.cowherd.validators.annotations.Length;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LengthValidatorTest
{
    private Method someMethod;
    private LengthValidator validator = new LengthValidator();

    private void someMethod(@Length(10) String param1,
                            @Length(max = 5) String param2,
                            @Length(min = 3) String param3,
                            @Length(max = 8, min = 2) String param4)
    {
    }

    @Before
    public void setUp() throws NoSuchMethodException
    {
        someMethod = this.getClass().getDeclaredMethod("someMethod", String.class, String.class, String.class, String.class);
    }

    @Test
    public void testValidateExact()
    {
        Length exactLengthAnno = someMethod.getParameters()[0].getAnnotation(Length.class);

        assertTrue(validator.validate(exactLengthAnno, "1234567890"));
        assertFalse(validator.validate(exactLengthAnno, "1234"));
        assertFalse(validator.validate(exactLengthAnno, null));
    }

    @Test
    public void testValidateMax()
    {
        Length maxLengthAnno = someMethod.getParameters()[1].getAnnotation(Length.class);

        assertFalse(validator.validate(maxLengthAnno, "1234567890"));
        assertTrue(validator.validate(maxLengthAnno, "12345"));
        assertTrue(validator.validate(maxLengthAnno, "1234"));
        assertTrue(validator.validate(maxLengthAnno, null));
    }

    @Test
    public void testValidateMin()
    {
        Length minLengthAnno = someMethod.getParameters()[2].getAnnotation(Length.class);

        assertTrue(validator.validate(minLengthAnno, "1234567890"));
        assertTrue(validator.validate(minLengthAnno, "123"));
        assertFalse(validator.validate(minLengthAnno, "1"));
        assertFalse(validator.validate(minLengthAnno, null));
    }

    @Test
    public void testValidateRange()
    {
        Length rangeLengthAnno = someMethod.getParameters()[3].getAnnotation(Length.class);

        assertFalse(validator.validate(rangeLengthAnno, "1234567890"));
        assertTrue(validator.validate(rangeLengthAnno, "12345"));
        assertFalse(validator.validate(rangeLengthAnno, "1"));
        assertFalse(validator.validate(rangeLengthAnno, null));
    }
}
