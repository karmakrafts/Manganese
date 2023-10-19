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

package io.karma.ferrous.manganese.analyze;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.access.Access;
import io.karma.ferrous.manganese.ocm.access.DefaultAccess;
import io.karma.ferrous.vanadium.FerrousParser.AccessModContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 17/10/2023
 */
@API(status = Status.INTERNAL)
public final class AccessAnalyzer extends ParseAdapter {
    private final Access access = DefaultAccess.PRIVATE;

    public AccessAnalyzer(final Compiler compiler) {
        super(compiler);
    }

    @Override
    public void enterAccessMod(final AccessModContext context) {

    }
}
