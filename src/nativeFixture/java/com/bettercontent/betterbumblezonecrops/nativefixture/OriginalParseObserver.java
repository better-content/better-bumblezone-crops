package com.bettercontent.betterbumblezonecrops.nativefixture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;

/** Calls the original public parse once, observes its return, and preserves result/exception identity. */
public final class OriginalParseObserver {
    private OriginalParseObserver() {}
    public static <A, T> DataResult<A> parse(Codec<A> codec, DynamicOps<T> ops, T input,
            Consumer<DataResult<A>> observer) {
        DataResult<A> result = codec.parse(ops, input);
        observer.accept(result);
        return result;
    }
}
