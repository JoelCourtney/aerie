package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.List;

public class PrimitiveIntArrayValueMapper implements ValueMapper<int[]> {
    @Override
    public ValueSchema getValueSchema() {
        return ValueSchema.ofSequence(ValueSchema.INT);
    }

    @Override
    public Result<int[], String> deserializeValue(SerializedValue serializedValue) {
        var elementMapper = new IntegerValueMapper();
        return serializedValue
                .asList()
                .map(Result::<List<SerializedValue>, String>success)
                .orElseGet(() -> Result.failure("Expected list, got " + serializedValue.toString()))
                .match(
                        serializedElements -> {
                            final int[] elements = new int[serializedElements.size()];
                            int index = 0;
                            for (final var serializedElement : serializedElements) {
                                final var result = elementMapper.deserializeValue(serializedElement);
                                if (result.getKind() == Result.Kind.Failure) return result.mapSuccess(_left -> null);

                                // SAFETY: `result` must be a Success variant.
                                elements[index++] = result.getSuccessOrThrow();
                            }
                            return Result.success(elements);
                        },
                        Result::failure
                );
    }

    @Override
    public SerializedValue serializeValue(int[] elements) {
        final var serializedElements = new ArrayList<SerializedValue>(elements.length);
        for (final var element : elements) {
            serializedElements.add(SerializedValue.of(element));
        }
        return SerializedValue.of(serializedElements);
    }
}
