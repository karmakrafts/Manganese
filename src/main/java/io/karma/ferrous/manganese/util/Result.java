/*
 * Copyright 2023 Karma Krafts & associates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.karma.ferrous.manganese.util;

import io.karma.ferrous.manganese.compiler.CompileStatus;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.kommons.function.XRunnable;
import io.karma.kommons.function.XSupplier;
import org.antlr.v4.runtime.Token;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 18/10/2023
 */
// TODO: move this to Kommons eventually
@API(status = Status.INTERNAL)
public final class Result<T, E> {
    private final Storage<T, E> storage;

    private Result(final Storage<T, E> storage) {
        this.storage = storage;
    }

    public static <T> Result<T, String> tryGet(final XSupplier<T, ? extends Throwable> closure) {
        try {
            return ok(closure.get());
        }
        catch (Throwable error) {
            return error(error.getLocalizedMessage());
        }
    }

    public static Result<Void, String> tryDo(final XRunnable<? extends Throwable> closure) {
        try {
            closure.run();
            return ok();
        }
        catch (Throwable error) {
            return error(error.getLocalizedMessage());
        }
    }

    public static <E> Result<Void, E> ok() {
        return new Result<>(new VoidStorage<>());
    }

    public static <T, E> Result<T, E> ok(final T value) {
        return new Result<>(new ValueStorage<>(value));
    }

    public static <T, E> Result<T, E> error(final E error) {
        return new Result<>(new ErrorStorage<>(error));
    }

    public boolean isOk() {
        return storage.hasValue();
    }

    public boolean isError() {
        return !isOk();
    }

    public T get() {
        return storage.value();
    }

    public E getError() {
        return storage.error();
    }

    public void throwIfError() {
        if (isOk()) {
            return;
        }
        throw new RuntimeException(getError().toString());
    }

    public Result<T, E> orThrow() {
        throwIfError();
        return this;
    }

    public T unwrap() {
        throwIfError();
        return get();
    }

    public Optional<T> unwrapOrReport(final Compiler compiler, final Token token, final CompileStatus status) {
        if (isError()) {
            final var context = compiler.getContext();
            context.reportError(context.makeError(token, Utils.makeCompilerMessage(storage.error().toString())),
                                status);
            return Optional.empty();
        }
        return Optional.of(storage.value());
    }

    public <XT> Result<XT, E> forwardError() {
        return error(storage.error());
    }

    private interface Storage<ST, SE> {
        ST value();

        SE error();

        boolean hasValue();
    }

    private static final class VoidStorage<E> implements Storage<Void, E> {
        @Override
        public Void value() {
            throw new UnsupportedOperationException("Result is a void result");
        }

        @Override
        public E error() {
            throw new UnsupportedOperationException("Result is not an error");
        }

        @Override
        public boolean hasValue() {
            return true;
        }
    }

    private record ValueStorage<T, E>(T value) implements Storage<T, E> {
        @Override
        public E error() {
            throw new UnsupportedOperationException("Result is not an error");
        }

        @Override
        public boolean hasValue() {
            return true;
        }
    }

    private record ErrorStorage<T, E>(E error) implements Storage<T, E> {
        @Override
        public T value() {
            throw new UnsupportedOperationException("Result is not a value");
        }

        @Override
        public boolean hasValue() {
            return false;
        }
    }
}
