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

package io.karma.ferrous.manganese.translate;

import io.karma.ferrous.manganese.CompileError;
import io.karma.ferrous.manganese.util.Utils;
import org.antlr.v4.runtime.Token;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class TranslationException extends RuntimeException {
    private final CompileError error;

    public TranslationException(final CompileError error) {
        this.error = error;
    }

    public TranslationException(final Token token, final String additionalText, final Object... params) {
        error = new CompileError(token);
        error.setAdditionalText(Utils.makeCompilerMessage(String.format(additionalText, params), null));
    }

    public TranslationException(final Token token) {
        this(new CompileError(token));
    }

    public CompileError getError() {
        return error;
    }
}
