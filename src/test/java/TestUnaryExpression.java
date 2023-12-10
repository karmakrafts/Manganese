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

import io.karma.ferrous.manganese.ocm.constant.IntConstant;
import io.karma.ferrous.manganese.ocm.constant.NullConstant;
import io.karma.ferrous.manganese.ocm.expr.ReferenceExpression;
import io.karma.ferrous.manganese.ocm.expr.UnaryExpression;
import io.karma.ferrous.manganese.ocm.statement.LetStatement;
import io.karma.ferrous.manganese.ocm.type.IntType;
import io.karma.ferrous.manganese.ocm.type.TypeModifier;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Operator;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * @author Alexander Hinze
 * @since 10/12/2023
 */
@TestInstance(Lifecycle.PER_CLASS)
public final class TestUnaryExpression extends AbstractTest {
    @Test
    void testConstantRef() {
        final var value = new IntConstant(IntType.I32, 42, TokenSlice.EMPTY);
        final var expr = new UnaryExpression(Operator.REF, value, TokenSlice.EMPTY);
        Assertions.assertEquals(expr.getType(), IntType.I32.asPtr());
    }

    @Test
    void testLocalRef() {
        final var initValue = new IntConstant(IntType.I32, 0, TokenSlice.EMPTY);
        final var local = new LetStatement(new Identifier("foo"), IntType.I32, initValue, false, TokenSlice.EMPTY);
        Assertions.assertEquals(local.getType(), IntType.I32);
        final var ref = new ReferenceExpression(local, false, TokenSlice.EMPTY);
        final var expr = new UnaryExpression(Operator.REF, ref, TokenSlice.EMPTY);
        Assertions.assertEquals(expr.getType(), IntType.I32.asPtr());
    }

    @Test
    void testMutableLocalRef() {
        final var initValue = new IntConstant(IntType.I32, 0, TokenSlice.EMPTY);
        final var local = new LetStatement(new Identifier("foo"), IntType.I32, initValue, true, TokenSlice.EMPTY);
        Assertions.assertEquals(local.getType(), IntType.I32);
        final var ref = new ReferenceExpression(local, false, TokenSlice.EMPTY);
        final var expr = new UnaryExpression(Operator.REF, ref, TokenSlice.EMPTY);
        final var exprType = expr.getType();
        Assertions.assertEquals(exprType, IntType.I32.asPtr());
        Assertions.assertTrue(exprType.getModifiers().contains(TypeModifier.MUT));
    }

    @Test
    void testLocalDeref() {
        final var initValue = new NullConstant(TokenSlice.EMPTY);
        initValue.setContextualType(IntType.I32.asPtr());
        final var local = new LetStatement(new Identifier("foo"),
            IntType.I32.asPtr(),
            initValue,
            false,
            TokenSlice.EMPTY);
        Assertions.assertEquals(local.getType(), IntType.I32.asPtr());
        final var ref = new ReferenceExpression(local, false, TokenSlice.EMPTY);
        final var expr = new UnaryExpression(Operator.DEREF, ref, TokenSlice.EMPTY);
        Assertions.assertEquals(expr.getType(), IntType.I32);
    }

    @Test
    void testMutableLocalDeref() {
        final var initValue = new NullConstant(TokenSlice.EMPTY);
        initValue.setContextualType(IntType.I32.asPtr());
        final var local = new LetStatement(new Identifier("foo"),
            IntType.I32.asPtr(),
            initValue,
            true,
            TokenSlice.EMPTY);
        Assertions.assertEquals(local.getType(), IntType.I32.asPtr());
        final var ref = new ReferenceExpression(local, false, TokenSlice.EMPTY);
        final var expr = new UnaryExpression(Operator.DEREF, ref, TokenSlice.EMPTY);
        Assertions.assertEquals(expr.getType(), IntType.I32);
    }
}
