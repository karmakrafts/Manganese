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

package io.karma.ferrous.manganese.parser;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.ocm.generic.GenericConstraint;
import io.karma.ferrous.vanadium.FerrousParser.GenericGroupedExprContext;
import io.karma.ferrous.vanadium.FerrousParser.TypeContext;
import org.apiguardian.api.API;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class GenericExpressionParser extends ParseAdapter {
    private final GenericConstraint constraints = GenericConstraint.TRUE;
    private int groupLevel = 0;

    public GenericExpressionParser(final CompileContext compileContext) {
        super(compileContext);
    }

    @Override
    public void enterGenericGroupedExpr(final GenericGroupedExprContext context) {
        groupLevel++;
        super.enterGenericGroupedExpr(context);
    }

    @Override
    public void exitGenericGroupedExpr(final GenericGroupedExprContext context) {
        groupLevel--;
        super.exitGenericGroupedExpr(context);
    }

    // TODO: implement me

    @Override
    public void enterType(final TypeContext context) {
        super.enterType(context);
    }

    public GenericConstraint getConstraints() {
        return constraints;
    }
}
