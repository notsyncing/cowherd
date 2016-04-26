package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.annotations.Component;
import io.github.notsyncing.cowherd.service.ComponentInstantiateType;
import io.github.notsyncing.cowherd.service.DependencyInjector;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DependencyInjectorTest
{
    private static int counterA = 0;
    private static int counterD = 0;
    private static int counterE = 0;

    @Component
    public static class A
    {
        public A()
        {
            counterA++;
        }
    }

    @Component
    public static class B extends A
    {
        private IC c;
        private D d;

        public B(IC c, D d)
        {
            this.c = c;
            this.d = d;
        }

        public IC getC()
        {
            return c;
        }

        public D getD()
        {
            return d;
        }
    }

    @Component
    public static class C implements IC
    {

    }

    public static interface IC
    {

    }

    @Component(ComponentInstantiateType.Singleton)
    public static class D
    {
        public D()
        {
            counterD++;
        }
    }

    @Component
    public static class E
    {
        public E()
        {
            counterE++;
        }
    }

    @Before
    public void setUp()
    {
        counterA = 0;
        counterD = 0;
        counterE = 0;

        DependencyInjector.clear();
    }

    @Test
    public void testGet()
    {
        DependencyInjector.registerComponent(IC.class, C.class, ComponentInstantiateType.AlwaysNew, false);
        DependencyInjector.registerComponent(A.class, ComponentInstantiateType.AlwaysNew, false);
        DependencyInjector.registerComponent(B.class, ComponentInstantiateType.AlwaysNew, false);
        DependencyInjector.registerComponent(D.class, ComponentInstantiateType.Singleton, false);
        DependencyInjector.registerComponent(new E());

        try {
            B b1 = DependencyInjector.getComponent(B.class);
            assertNotNull(b1);
            assertNotNull(b1.getC());
            assertNotNull(b1.getD());
            assertEquals(C.class, b1.getC().getClass());

            B b2 = DependencyInjector.getComponent(B.class);
            assertNotNull(b2);
            assertNotNull(b2.getC());
            assertNotNull(b2.getD());
            assertEquals(C.class, b2.getC().getClass());
            assertNotEquals(b1, b2);

            B b3 = (B)DependencyInjector.getComponent(getClass().getName() + "$B");
            assertNotNull(b3);
            assertNotNull(b3.getC());
            assertNotNull(b3.getD());
            assertEquals(C.class, b3.getC().getClass());
            assertNotEquals(b2, b3);
            assertNotEquals(b1, b3);

            assertEquals(3, counterA);
            assertEquals(1, counterD);

            assertEquals(b1.getD(), b2.getD());
            assertEquals(b2.getD(), b3.getD());

            E e1 = (E)DependencyInjector.getComponent(getClass().getName() + "$E");
            assertNotNull(e1);
            assertEquals(1, counterE);

            E e2 = (E)DependencyInjector.getComponent(getClass().getName() + "$E");
            assertNotNull(e2);
            assertEquals(1, counterE);

            assertEquals(e1, e2);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}