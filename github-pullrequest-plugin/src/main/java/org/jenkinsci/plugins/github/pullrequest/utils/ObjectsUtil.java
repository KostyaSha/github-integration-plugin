package org.jenkinsci.plugins.github.pullrequest.utils;

/**
 * TODO: on java 1.8 migrate + resolve findbugs path check
 *
 * @author Kanstantsin Shautsou
 */
public class ObjectsUtil {
    private ObjectsUtil() {
    }

    public static boolean isNull(Object object) {
        return object == null;
    }

    public static boolean nonNull(Object obj) {
        return obj != null;
    }

}
