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

package io.karma.ferrous.manganese.ocm.type;

import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.apiguardian.api.API;

/**
 * @author Alexander Hinze
 * @since 15/11/2023
 */
@API(status = API.Status.INTERNAL)
public enum TypeModifier {
    // @formatter:off
    ATOMIC (FerrousLexer.KW_ATOMIC),
    TLS    (FerrousLexer.KW_TLS),
    MUT    (FerrousLexer.KW_MUT);
    // @formatter:on

    private final int token;
    private final String text;

    TypeModifier(final int token) {
        this.token = token;
        text = TokenUtils.getLiteral(token);
    }

    public String getText() {
        return text;
    }
}
