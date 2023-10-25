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

package io.karma.ferrous.manganese.ocm.constant;

import io.karma.ferrous.manganese.ocm.expr.Expression;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
@API(status = Status.INTERNAL)
public interface Constant extends Expression {
    @Override
    default boolean isConstant() {
        return true;
    }

    default byte getByte() {
        if (this instanceof ByteConstant constant) {
            return constant.value();
        }
        throw new UnsupportedOperationException("Constant is not a byte");
    }

    default short getShort() {
        if (this instanceof ShortConstant constant) {
            return constant.value();
        }
        throw new UnsupportedOperationException("Constant is not a short");
    }

    default int getInt() {
        if (this instanceof IntConstant constant) {
            return constant.value();
        }
        throw new UnsupportedOperationException("Constant is not a int");
    }

    default long getLong() {
        if (this instanceof LongConstant constant) {
            return constant.value();
        }
        throw new UnsupportedOperationException("Constant is not a long");
    }

    default float getFloat() {
        if (this instanceof FloatConstant constant) {
            return constant.value();
        }
        throw new UnsupportedOperationException("Constant is not a float");
    }

    default double getDouble() {
        if (this instanceof DoubleConstant constant) {
            return constant.value();
        }
        throw new UnsupportedOperationException("Constant is not a double");
    }

    default boolean getBoolean() {
        if (this instanceof BoolConstant constant) {
            return constant.value();
        }
        throw new UnsupportedOperationException("Constant is not a boolean");
    }
}
