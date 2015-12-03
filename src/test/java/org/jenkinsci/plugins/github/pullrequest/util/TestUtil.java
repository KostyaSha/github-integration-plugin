package org.jenkinsci.plugins.github.pullrequest.util;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class TestUtil {
//    private TestUtil() {
//    }

    public static Field getPrivateField(String fieldName, Class<?> clazz) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field;
    }

    @Test
    public void getFrequentElement() {
        final ArrayList<String> elements = new ArrayList<>(Arrays.asList(
                "sdf", "sdf", "sdf2", "sdf3"
        ));

        Map<String, Integer> map = new HashMap<>();
        //calculate elements
        for (String element : elements) {
            if (map.containsKey(element)) {
                Integer count = map.get(element) + 1;
                map.put(element, count);
            } else {
                map.put(element, 1);
            }
        }

        // find the most frequent
        final Set<Map.Entry<String, Integer>> entries = map.entrySet();
        Integer highest = 0;
        String highestElement = "";
        for (Map.Entry<String, Integer> entry : entries) {
            if (entry.getValue() >= highest) {
                highestElement = entry.getKey();
            }
        }

        // print
        System.out.println("Highest element " + highestElement);
    }
}
