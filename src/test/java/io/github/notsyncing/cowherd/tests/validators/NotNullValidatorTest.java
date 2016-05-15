package io.github.notsyncing.cowherd.tests.validators;

import io.github.notsyncing.cowherd.validators.NotNullValidator;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotNullValidatorTest
{
    @Test
    public void testValidate()
    {
        NotNullValidator validator = new NotNullValidator();

        assertTrue(validator.validate(new String()));
        assertFalse(validator.validate(null));
    }
}
