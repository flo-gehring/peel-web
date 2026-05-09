package de.flogehring.peelserver.run;

import de.flogehring.peel.core.eval.RequestBindings;
import de.flogehring.peel.core.eval.Variable;
import de.flogehring.peel.core.values.Bool;
import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.core.values.Primitives;
import de.flogehring.peel.core.values.Text;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BindingsMapper {

    public RequestBindings toRequestBindings(Map<String, Object> rawBindings) {
        if (rawBindings.isEmpty()) {
            return RequestBindings.empty();
        }
        RequestBindings.Builder builder = RequestBindings.builder();
        for (Map.Entry<String, Object> entry : rawBindings.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("binding names must not be blank");
            }
            builder.put(Variable.of(key, toPeelValue(entry.getValue())));
        }
        return builder.build();
    }

    private PeelValue toPeelValue(Object value) {
        if (value == null) {
            return None.NONE;
        }
        if (value instanceof PeelValue peelValue) {
            return peelValue;
        }
        if (value instanceof String stringValue) {
            return new Text(stringValue);
        }
        if (value instanceof Boolean boolValue) {
            return new Bool(boolValue);
        }
        if (value instanceof Integer integerValue) {
            return new Number.Integer(integerValue);
        }
        if (value instanceof Long longValue) {
            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                return new Number.Integer(longValue.intValue());
            }
            return new Number.Decimal(BigDecimal.valueOf(longValue));
        }
        if (value instanceof Float floatValue) {
            return new Number.Float(floatValue);
        }
        if (value instanceof Double doubleValue) {
            return new Number.Decimal(BigDecimal.valueOf(doubleValue));
        }
        if (value instanceof BigInteger bigInteger) {
            return new Number.Decimal(new BigDecimal(bigInteger));
        }
        if (value instanceof BigDecimal bigDecimal) {
            return new Number.Decimal(bigDecimal);
        }
        if (value instanceof List<?> listValue) {
            return PeelValue.Collection.peelList(listValue.stream().map(this::toPeelValue).toList());
        }
        if (value instanceof Map<?, ?> mapValue) {
            LinkedHashMap<Primitives, PeelValue> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                PeelValue key = toPeelValue(entry.getKey());
                if (!(key instanceof Primitives primitiveKey)) {
                    throw new IllegalArgumentException("map keys must resolve to peel primitives");
                }
                converted.put(primitiveKey, toPeelValue(entry.getValue()));
            }
            return PeelValue.Collection.peelMap(Map.copyOf(converted));
        }
        throw new IllegalArgumentException("unsupported binding value type: " + value.getClass().getName());
    }
}