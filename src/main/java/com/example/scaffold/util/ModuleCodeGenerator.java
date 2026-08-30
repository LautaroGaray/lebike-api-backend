package com.example.scaffold.util;

import java.util.Random;

public class ModuleCodeGenerator {
    private static final String PREFIX = "MD";
    private static final String SUFFIX = "LB";
    private static final Random RANDOM = new Random();

    public static String generate() {
        int randomNumber = RANDOM.nextInt(1000000);
        return String.format("%s%06d%s", PREFIX, randomNumber, SUFFIX);
    }
}
