package org.jenkinsci.plugins.github.pullrequest.utils;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Utilities for IOException handling in Streams
 */
public class IOUtils {

    private IOUtils() {
    }

    /**
     * Wraps provided Function into an IOFunction. Users are expected to wrap stream terminal operations using
     * {@link #withIo(Runnable)} or {@link #withIo(Supplier)}
     */
    public static <T, R> Function<T, R> iof(IOFunction<T, R> fun) {
        return t -> {
            try {
                return fun.apply(t);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        };
    }

    /**
     * Wraps provided Predicate into an IOPredicate. Users are expected to wrap stream terminal operations using
     * {@link #withIo(Runnable)} or {@link #withIo(Supplier)}
     */
    public static <T> Predicate<T> iop(IOPredicate<T> pred) {
        return t -> {
            try {
                return pred.test(t);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        };
    }

    /**
     * Executes a terminal operation that uses IO-versions of lambdas and spits out any thrown IOException
     */
    public static <V> V withIo(Supplier<V> supp) throws IOException {
        try {
            return supp.get();
        } catch (RuntimeIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Executes a terminal operation that uses IO-versions of lambdas and spits out any thrown IOException
     */
    public static void withIo(Runnable run) throws IOException {
        try {
            run.run();
        } catch (RuntimeIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Executes a {@link Stream#forEach(Consumer)} on a stream that uses IO-versions of lambdas and spits out any thrown
     * IOException
     */
    public static <V> void forEachIo(Stream<V> stream, Consumer<V> cons) throws IOException {
        withIo(() -> stream.forEach(cons));
    }

    /**
     * Creates a stream from a single element returned by provided IOSupplier. If supplier throws an IOException, returns
     * {@link Stream#empty()}
     */
    public static <V> Stream<V> ioOptStream(IOSupplier<V> supp) {
        try {
            V v = supp.get();
            if (v != null) {
                return Stream.of(v);
            }
        } catch (IOException e) {
        }
        return Stream.empty();
    }

    public static class RuntimeIOException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public RuntimeIOException(IOException e) {
            super(e);
        }

        @Override
        public IOException getCause() {
            return (IOException) super.getCause();
        }
    }

    @FunctionalInterface
    public interface IOFunction<T, R> {
        R apply(T t) throws IOException;
    }

    @FunctionalInterface
    public interface IOPredicate<T> {
        boolean test(T t) throws IOException;
    }

    @FunctionalInterface
    public interface IOSupplier<T> {
        T get() throws IOException;
    }
}
