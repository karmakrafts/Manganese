package io.karma.ferrous.manganese.translate;

import io.karma.ferrous.manganese.util.Logger;
import org.lwjgl.llvm.LLVMCore;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
public class TranslationUnit extends AbstractTranslationUnit {
    private final long module;
    private boolean isDisposed;

    public TranslationUnit(final String name) {
        module = LLVMCore.LLVMModuleCreateWithName(name);
        if (module == NULL) {
            throw new RuntimeException("Could not allocate module");
        }
        Logger.INSTANCE.debug("Created translation unit module %s", name);
    }

    public void dispose() {
        LLVMCore.LLVMDisposeModule(module);
    }

    public long getModule() {
        return module;
    }
}
