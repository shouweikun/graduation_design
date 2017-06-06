package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by john_liu on 2017/6/6.
 */
public class StringEqualsTest {
    public static void main(String[] args) {
        Set a = new HashSet();
        String b = "a";
        String c = "a";
        a.add(b);
        System.out.println(a.add(c));

    }
}
