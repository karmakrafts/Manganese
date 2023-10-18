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

package io.karma.ferrous.manganese.target;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.STABLE)
public final class Target {
    private final Architecture architecture;
    private final int pointerSize;
    private final ABI abi;

    public Target(final Architecture architecture, final int pointerSize, final ABI abi) {
        this.architecture = architecture;
        this.pointerSize = pointerSize;
        this.abi = abi;
    }

    public Target(final Architecture architecture, final ABI abi) {
        this(architecture, architecture.getPointerSize(), abi);
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public int getPointerSize() {
        return pointerSize;
    }

    public ABI getAbi() {
        return abi;
    }

    public String getTriple() {
        return String.format("%s-unknown-%s", architecture.getName(), abi.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(architecture, pointerSize, abi);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Target target) {
            return pointerSize == target.pointerSize;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s (%d byte pointers)", getTriple(), pointerSize);
    }
}
