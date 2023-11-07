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

import io.karma.ferrous.manganese.ocm.BlockContext;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.kommons.lazy.Lazy;
import io.karma.kommons.tuple.Pair;
import org.apiguardian.api.API;

import java.util.List;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class IfExpression implements Expression {
    private final List<Pair<Expression, ScopeExpression>> branches;
    private final Lazy<Type> type;
    private final TokenSlice tokenSlice;

    public IfExpression(final List<Pair<Expression, ScopeExpression>> branches, final TokenSlice tokenSlice) {
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("If expression requires at least one block");
        }
        this.branches = branches;
        // @formatter:off
        type = new Lazy<>(() -> TypeUtils.findCommonType(branches.stream()
            .map(pair -> pair.getRight().getType())
            .toArray(Type[]::new)));
        // @formatter:on
        this.tokenSlice = tokenSlice;
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public Type getType() {
        return type.getOrCreate();
    }

    @Override
    public long emit(final TargetMachine targetMachine, final BlockContext blockContext) {
        for (final var branch : branches) {
            final var statements = branch.getRight().getStatements();
            for (final var statement : statements) {
                statement.emit(targetMachine, blockContext); // Ignore result here
            }
        }
        return 0L; // Return value ref to result register
    }
}
