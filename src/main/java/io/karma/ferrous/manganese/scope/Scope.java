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

package io.karma.ferrous.manganese.scope;

import io.karma.ferrous.manganese.ocm.NameProvider;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.EnumSet;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
@API(status = Status.INTERNAL)
public interface Scope extends NameProvider {
    EnumSet<ScopeType> INVISIBLE_SCOPE_TYPES = EnumSet.of(ScopeType.GLOBAL, ScopeType.FILE, ScopeType.MODULE_FILE);

    Scope getEnclosingScope();

    default void setEnclosingScope(Scope scope) {
    }

    default ScopeType getScopeType() {
        return getEnclosingScope().getScopeType();
    }

    default Identifier getScopeName() {
        return getEnclosingScope().getScopeName();
    }

    default Identifier getQualifiedName() {
        var currentParent = getEnclosingScope();
        var result = getName();
        if (currentParent == null) {
            return getName();
        }
        while (currentParent != null) {
            if (INVISIBLE_SCOPE_TYPES.contains(currentParent.getScopeType())) {
                currentParent = currentParent.getEnclosingScope();
                continue;
            }
            result = currentParent.getName().join(result);
            currentParent = currentParent.getEnclosingScope();
        }
        return result;
    }

    default String getInternalName() {
        return getQualifiedName().toString().replace(Identifier.DELIMITER, ".");
    }

    @Override
    default Identifier getName() {
        return getScopeName();
    }
}
