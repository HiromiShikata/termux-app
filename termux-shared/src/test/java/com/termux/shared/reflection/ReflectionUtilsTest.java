package com.termux.shared.reflection;

import com.termux.shared.logger.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtilsTest {

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    public static class ReflectionTarget {
        private final String name;
        private int counter;

        public ReflectionTarget() {
            this.name = "default";
            this.counter = 0;
        }

        public ReflectionTarget(String name) {
            this.name = name;
            this.counter = 0;
        }

        private String greet(String who) {
            return "hello " + who;
        }

        private void increment() {
            counter++;
        }

        private void add(int amount) {
            counter += amount;
        }

        private String noArgs() {
            return "no-args";
        }
    }

    @Before
    public void disableAndroidLogging() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_OFF);
    }

    @Test
    public void areHiddenAPIReflectionRestrictionsBypassedReturnsTrueBelowApiP() {
        Assert.assertTrue(ReflectionUtils.areHiddenAPIReflectionRestrictionsBypassed());
    }

    @Test
    public void bypassHiddenAPIReflectionRestrictionsIsNoOpBelowApiP() {
        ReflectionUtils.bypassHiddenAPIReflectionRestrictions();
        Assert.assertTrue(ReflectionUtils.areHiddenAPIReflectionRestrictionsBypassed());
    }

    @Test
    public void getDeclaredFieldReturnsAccessibleField() {
        Field field = ReflectionUtils.getDeclaredField(ReflectionTarget.class, "name");
        Assert.assertNotNull(field);
        Assert.assertEquals("name", field.getName());
        Assert.assertTrue(field.isAccessible());
    }

    @Test
    public void getDeclaredFieldReturnsNullForMissingField() {
        Assert.assertNull(ReflectionUtils.getDeclaredField(ReflectionTarget.class, "doesNotExist"));
    }

    @Test
    public void invokeFieldReadsValueFromObject() {
        ReflectionTarget target = new ReflectionTarget("custom");
        ReflectionUtils.FieldInvokeResult result = ReflectionUtils.invokeField(ReflectionTarget.class, "name", target);
        Assert.assertEquals("custom", result.value);
    }

    @Test
    public void invokeFieldReturnsResultForMissingField() {
        ReflectionTarget target = new ReflectionTarget();
        ReflectionUtils.FieldInvokeResult result = ReflectionUtils.invokeField(ReflectionTarget.class, "missing", target);
        Assert.assertFalse(result.success);
        Assert.assertNull(result.value);
    }

    @Test
    public void getDeclaredMethodWithoutParametersReturnsMethod() {
        Method method = ReflectionUtils.getDeclaredMethod(ReflectionTarget.class, "noArgs");
        Assert.assertNotNull(method);
        Assert.assertEquals("noArgs", method.getName());
        Assert.assertEquals(0, method.getParameterTypes().length);
    }

    @Test
    public void getDeclaredMethodWithParametersReturnsMethod() {
        Method method = ReflectionUtils.getDeclaredMethod(ReflectionTarget.class, "greet", String.class);
        Assert.assertNotNull(method);
        Assert.assertEquals("greet", method.getName());
        Assert.assertEquals(1, method.getParameterTypes().length);
    }

    @Test
    public void getDeclaredMethodReturnsNullForMissingMethod() {
        Assert.assertNull(ReflectionUtils.getDeclaredMethod(ReflectionTarget.class, "missingMethod"));
    }

    @Test
    public void invokeMethodReturnsValueFromMethodWithArgs() {
        Method method = ReflectionUtils.getDeclaredMethod(ReflectionTarget.class, "greet", String.class);
        ReflectionUtils.MethodInvokeResult result = ReflectionUtils.invokeMethod(method, new ReflectionTarget(), "world");
        Assert.assertEquals("hello world", result.value);
    }

    @Test
    public void invokeMethodWithoutArgsReturnsValue() {
        Method method = ReflectionUtils.getDeclaredMethod(ReflectionTarget.class, "noArgs");
        ReflectionUtils.MethodInvokeResult result = ReflectionUtils.invokeMethod(method, new ReflectionTarget());
        Assert.assertEquals("no-args", result.value);
    }

    @Test
    public void invokeMethodOnWrongObjectReturnsNullValue() {
        Method method = ReflectionUtils.getDeclaredMethod(ReflectionTarget.class, "greet", String.class);
        ReflectionUtils.MethodInvokeResult result = ReflectionUtils.invokeMethod(method, "not-a-target", "x");
        Assert.assertFalse(result.success);
        Assert.assertNull(result.value);
    }

    @Test
    public void invokeVoidMethodWithoutArgsReturnsTrue() {
        Method method = ReflectionUtils.getDeclaredMethod(ReflectionTarget.class, "increment");
        ReflectionTarget target = new ReflectionTarget();
        Assert.assertTrue(ReflectionUtils.invokeVoidMethod(method, target));
        Assert.assertEquals(1, target.counter);
    }

    @Test
    public void invokeVoidMethodWithArgsReturnsTrue() {
        Method method = ReflectionUtils.getDeclaredMethod(ReflectionTarget.class, "add", int.class);
        ReflectionTarget target = new ReflectionTarget();
        Assert.assertTrue(ReflectionUtils.invokeVoidMethod(method, target, 5));
        Assert.assertEquals(5, target.counter);
    }

    @Test
    public void invokeVoidMethodOnWrongObjectReturnsFalse() {
        Method method = ReflectionUtils.getDeclaredMethod(ReflectionTarget.class, "increment");
        Assert.assertFalse(ReflectionUtils.invokeVoidMethod(method, "not-a-target"));
    }

    @Test
    public void getConstructorWithoutParametersReturnsConstructor() {
        Constructor<?> constructor = ReflectionUtils.getConstructor(ReflectionTarget.class.getName());
        Assert.assertNotNull(constructor);
        Assert.assertEquals(0, constructor.getParameterTypes().length);
    }

    @Test
    public void getConstructorWithParametersReturnsConstructor() {
        Constructor<?> constructor = ReflectionUtils.getConstructor("java.lang.String", String.class);
        Assert.assertNotNull(constructor);
        Assert.assertEquals(1, constructor.getParameterTypes().length);
    }

    @Test
    public void getConstructorReturnsNullForMissingClass() {
        Assert.assertNull(ReflectionUtils.getConstructor("com.example.NoSuchClass"));
    }

    @Test
    public void getConstructorByClassReturnsConstructor() {
        Constructor<?> constructor = ReflectionUtils.getConstructor(String.class, String.class);
        Assert.assertNotNull(constructor);
    }

    @Test
    public void getConstructorByClassReturnsNullForMissingSignature() {
        Assert.assertNull(ReflectionUtils.getConstructor(ReflectionTarget.class, Integer.class, Integer.class));
    }

    @Test
    public void invokeConstructorWithoutArgsCreatesInstance() {
        Constructor<?> constructor = ReflectionUtils.getConstructor(ReflectionTarget.class.getName());
        Object instance = ReflectionUtils.invokeConstructor(constructor);
        Assert.assertTrue(instance instanceof ReflectionTarget);
    }

    @Test
    public void invokeConstructorWithArgsCreatesInstance() {
        Constructor<?> constructor = ReflectionUtils.getConstructor("java.lang.String", String.class);
        Object instance = ReflectionUtils.invokeConstructor(constructor, "value");
        Assert.assertEquals("value", instance);
    }

    @Test
    public void invokeConstructorReturnsNullWhenArgumentsMismatch() {
        Constructor<?> constructor = ReflectionUtils.getConstructor("java.lang.String", String.class);
        Assert.assertNull(ReflectionUtils.invokeConstructor(constructor, 123));
    }
}
