package org.jenkinsci.plugins.github.pullrequest.util;

import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import static java.lang.String.format;
import static org.apache.commons.lang3.ClassUtils.PACKAGE_SEPARATOR;

public final class TestUtil {
    private TestUtil() {
    }

    public static Field getPrivateField(String fieldName, Class<?> clazz) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field;
    }

    public static String classpath(Class<?> clazz, String path) {
        try {
            return IOUtils.toString(clazz.getClassLoader().getResourceAsStream(
                    clazz.getName().replace(PACKAGE_SEPARATOR, File.separator) + File.separator + path
            ), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(format("Can't load %s for class %s", path, clazz), e);
        }
    }
}
