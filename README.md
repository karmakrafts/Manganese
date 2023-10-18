# Manganese

Bootstrap compiler backend for the Ferrous programming language written in Java.
This is a temporary compiler implementation used for self-hosting and thus it
only supports LLVM as its backend. **Other backends will not be supported**.

### Running
You can run the Manganese compiler by simple running the following command:

```shell
java -jar manganese-x.x.x.x.jar [options]
```

If you need a list of all available options, run the compiler with the `-?` option.

### Embedding

If you want to embed this compiler into your own project, you can simply
use the provided Maven artifact in your build system of choice. The following
examples demonstrate the usage in Gradle and under Maven respectively:

```groovy
repositories {
    maven { url = 'https://maven.covers1624.net' }
}
dependencies {
    implementation group: 'io.karma.ferrous.manganese', name: 'manganese', version: 'VERSION', classifier: 'slim'
}
```

```xml

<project>
	<repositories>
		<repository>
			<id>covers-maven</id>
			<name>Covers Maven</name>
			<url>https://maven.covers1624.net</url>
		</repository>
	</repositories>
	<dependencies>
		<dependency>
			<groupId>io.karma.ferrous.manganese</groupId>
			<artifactId>manganese</artifactId>
			<version>VERSION</version>
		</dependency>
	</dependencies>
</project>
```

You can obtain the latest version from the provided repository batch at the top of the page.  
The following demonstrates the programmatical use of the compiler:

```java
import io.karma.ferrous.manganese.Manganese;
import io.karma.ferrous.manganese.target.FileType;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;

static {
    Manganese.init();
}

public CompileStatus compileSomething(final Path inPath, final ByteBuffer outBuffer) {
    final var compiler = Manganese.createCompiler(FileType.ELF);
    // @formatter:off
    try (final var inStream = Files.newInputStream(inPath); 
         final var inChannel = Channels.newChannel(inStream); 
         final var outStream = new ByteArrayOutputStream();
         final var outChannel = Channels.newChannel(outStream)) {
        // @formatter:on
        final var result = compiler.compile(inChannel, outChannel);
        if(result.getStatus() != CompileStatus.SUCCESS) {
            // Throw first compiler error as runtime exception
            throw new RuntimeException(result.getErrors().get(0));
        }
        outBuffer.put(outStream.toByteArray());
        outBuffer.flip();
    }
    compiler.dispose();
}
```

### Building

In order to build the compiler, you can simply run the following command after
cloning the repository:

```shell
./gradlew build --info --no-daemon
```

or the following if you are using `cmd` under Windows:

```shell
gradlew build --info --no-daemon
```

This will produce three different `JAR` files under `build/libs` in the
project directory. The `slim` version can be used for development/embedding
since it does not contain all the shadowed dependencies.