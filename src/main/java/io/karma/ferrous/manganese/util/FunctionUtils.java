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

import io.karma.ferrous.vanadium.FerrousParser.FunctionIdentContext;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public final class FunctionUtils {
    // @formatter:off
    private FunctionUtils() {}
    // @formatter:on

    public static String getFunctionName(final FunctionIdentContext context) {
        final var children = context.children;
        if (children.size() == 1) {
            final var text = children.get(0).getText();
            final var op = Operator.findByText(text);
            if (op.isPresent()) {
                return op.get().getFunctionName();
            }
        }
        return context.ident().getText();
    }
}
