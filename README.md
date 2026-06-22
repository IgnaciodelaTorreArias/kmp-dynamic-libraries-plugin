# Klib Native Dynamic Libraries Extractor

A Gradle plugin for Kotlin Multiplatform projects that automatically extracts and manages dynamic libraries packaged within `.klib` dependencies.

## Overview

CInterop with Kotlin multiplatform doesn't support dynamic libraries in the .def file. This project imagines how this could work.

Advantages of dynamic libraries:

- Less ABI compatibility & toolchain match & environment match issues
- More modularity
- Easier access to other projects that already provide pre-compiled dynamic libraries
- Compliment with licenses (like LGPL)
- Resource saving when running multiple instances of the same executable that uses the dynamic library

The `.def`:

```none
headers = tokenizers_proto.h
package = io.github.ignaciodelatorrearias.huggingface.tokenizers.internal.cinterop

compilerOpts = -Itokenizers_proto/

# Passing the dynamic libraries as if they were static libraries adds them to the linker and to the resulting .klib that gets published to maven.
staticLibraries.mingw_x64 = tokenizers_proto.dll.lib tokenizers_proto.dll
libraryPaths.mingw_x64 = natives/x86_64-windows/
# Alternative:
# The linker adds the platform suffix so it links with tokenizers_proto.dll.lib
# linkerOpts.mingw_x64 = -Lnatives/x86_64-windows/ -ltokenizers_proto.dll

staticLibraries.linux_x64 = libtokenizers_proto.so
libraryPaths.linux_x64 = natives/x86_64-linux-gnu/
# In linux seems like passing the dynamic library as a static one doesn't fully work so we have to pass it to the linker as arguments
# -rpath Specify the linker that the .so should be search in the same directory as the executable or the working directory
linkerOpts.linux_x64 = -Lnatives/x86_64-linux-gnu/ -ltokenizers_proto -rpath "$ORIGIN:."

# A property like:
dynamicLibraries = tokenizer_proto
# should translate to the following:
linkerOpts.mingw_x64 = -L"${project.layout.buildDirectory.dir("bin/libraries/${binary.target.konanTarget.name}")}" -ltokenizers_proto.dll
linkerOpts.linux_x64 = -L"${project.layout.buildDirectory.dir("bin/libraries/${binary.target.konanTarget.name}")}" -ltokenizers_proto -rpath "$ORIGIN:."

# The manifest of the cinterop.klib should include the dynamicLibraries and the dynamic libraries should be extracted before link tasks and copied to the output directory at the end of the link task (so the user doesn't have to worry about how and where to find the required dynamic libraries)
```

## Installation

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.ignaciodelatorrearias.kmp-dynamic-libraries-plugin") version "0.1.0"
}
```

## How It Works

The plugin intercepts `KotlinNativeLink` tasks and:

1. **Before linking**: Extracts dynamic libraries (and import libraries on Windows) from `.klib` files to `build/bin/libraries/{konanTarget}/`
2. **During linking**: Makes these libraries available to the native linker (#TODO)
3. **After linking**: Copies runtime libraries to the final binary directory

Supported library formats by platform:

- **Windows**: `.dll`, `.dll.lib` (import library)
- **Linux**: `.so`
- **macOS**: `.dylib`

## License

Apache License 2.0 - See LICENSE file for details

## Author

Ignacio de la Torre Arias
