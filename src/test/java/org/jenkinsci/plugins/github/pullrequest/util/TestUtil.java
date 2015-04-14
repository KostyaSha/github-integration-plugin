package org.jenkinsci.plugins.github.pullrequest.util;

import java.lang.reflect.Field;

public class TestUtil {
    private TestUtil() {}

    public static Field getPrivateField(String fieldName, Class<?> clazz) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field;
    }
}
