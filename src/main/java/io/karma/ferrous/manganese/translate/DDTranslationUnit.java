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

import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.StructureType;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.ClassContext;
import io.karma.ferrous.vanadium.FerrousParser.EnumClassContext;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import io.karma.ferrous.vanadium.FerrousParser.StructContext;

import java.util.HashMap;

/**
 * Special translation unit ran during the pre-compilation pass
 * to discover declarations throughout the code.
 *
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public class DDTranslationUnit extends AbstractTranslationUnit {
    private final HashMap<String, Function> functions = new HashMap<>();
    private final HashMap<String, StructureType> structures = new HashMap<>();

    public DDTranslationUnit(Compiler compiler) {
        super(compiler);
    }

    @Override
    public void enterStruct(StructContext context) {
        final var name = Utils.getIdentifier(context.ident());
        Logger.INSTANCE.debugln("Found struct '%s'", name);
    }

    @Override
    public void enterClass(ClassContext context) {
        final var name = Utils.getIdentifier(context.ident());
        Logger.INSTANCE.debugln("Found class '%s'", name);
    }

    @Override
    public void enterEnumClass(EnumClassContext context) {
        final var name = Utils.getIdentifier(context.ident());
        Logger.INSTANCE.debugln("Found enum class '%s'", name);
    }

    @Override
    public void enterProtoFunction(ProtoFunctionContext context) {
        final var name = FunctionUtils.getFunctionName(context.functionIdent());
        final var type = TypeUtils.getFunctionType(compiler, context);
        final var function = new Function(name, type);
        functions.put(name, function);
        Logger.INSTANCE.debugln("Found function '%s'", function);
    }

    public HashMap<String, Function> getFunctions() {
        return functions;
    }

    public HashMap<String, StructureType> getStructures() {
        return structures;
    }
}
