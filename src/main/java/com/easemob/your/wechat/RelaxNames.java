package com.easemob.your.wechat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

public class RelaxNames {
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("(?<=[^A-Z-])(?=[A-Z])");
    private static final Pattern HYPHEN_PATTERN = Pattern.compile("-");
    private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");

    private final List<String> names;

    public RelaxNames(String names) {
        this.names = split(names);
    }

    public static List<String> split(String name) {
        return Arrays.stream(Scheme.values()).map(s -> s.apply(name))
                .max(Comparator.comparing(List::size))
                .get();
    }

    public String toCamelCase(boolean capitalizeFirstWord) {
        if (!capitalizeFirstWord) {
            return toCamelCaseVarStyle();
        } else {
            return toCamelCaseClassStyle();
        }
    }

    public String toCamelCaseClassStyle() {
        return names.stream().map(StringUtils::capitalize).collect(Collectors.joining());

    }

    public String toCamelCaseVarStyle() {
        return names.get(0) +
                names.stream().skip(1).map(StringUtils::capitalize)
                        .collect(Collectors.joining());
    }
    public String toHyphen() {
        return names.stream().collect(Collectors.joining("-"));
    }
    public String toUnderscore() {
        return names.stream().collect(Collectors.joining("_"));
    }
    public enum Scheme {
        NONE {
            @Override
            public List<String> apply(String value) {
                return Collections.singletonList(value);
            }
        },
        CAMEL_CASE {
            @Override
            public List<String> apply(String value) {
                return CAMEL_CASE_PATTERN.splitAsStream(value)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
            }
        },
        HYPHEN {
            @Override
            public List<String> apply(String value) {
                return HYPHEN_PATTERN.splitAsStream(value)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
            }
        },
        UNDERSCORE {
            @Override
            public List<String> apply(String value) {
                return UNDERSCORE_PATTERN.splitAsStream(value)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
            }
        };
        public abstract List<String> apply(String value);
    }
}
