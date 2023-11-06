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

package io.karma.ferrous.manganese.ocm.expr;

import io.karma.ferrous.manganese.ocm.statement.ReturnStatement;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.kommons.lazy.Lazy;
import org.apiguardian.api.API;

import java.util.ArrayList;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class ScopeExpression {
    private final Statement[] statements;
    private final Lazy<Type> type = new Lazy<>(this::findReturnType);

    public ScopeExpression(final Statement... statements) {
        this.statements = statements;
    }

    public Statement[] getStatements() {
        return statements;
    }

    private Type findReturnType() {
        final var types = new ArrayList<Type>();
        for (final var statement : statements) {
            if (!(statement instanceof ReturnStatement returnStatement)) {
                continue;
            }
            final var type = returnStatement.getValue().getType();
            if (type == BuiltinType.VOID) {
                continue;
            }
            types.add(type);
        }
        return types.isEmpty() ? BuiltinType.VOID : TypeUtils.findCommonType(types.toArray(Type[]::new));
    }

    public Type getType() {
        return type.getOrCreate();
    }
}
