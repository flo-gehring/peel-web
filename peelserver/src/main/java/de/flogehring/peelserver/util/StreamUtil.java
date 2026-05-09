package de.flogehring.peelserver.util;

import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class StreamUtil {
    private StreamUtil() {
    }

    public static <S, T, U> Map<S, U> transformMapValues(
            Map<S, T> input,
            Function<T, U> valueMapper
    ) {
        return input.entrySet().stream()
                .collect(
                        toMap(Map.Entry::getKey,
                                entry -> valueMapper.apply(entry.getValue())
                        )
                );
    }
}
