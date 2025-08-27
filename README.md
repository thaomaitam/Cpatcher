# Cpatcher - Universal Android Runtime Patcher

<p align="center">
  <img src="https://img.shields.io/badge/Android-26%2B-green.svg" alt="Android 26+">
  <img src="https://img.shields.io/badge/Xposed%20API-93-blue.svg" alt="Xposed API 93">
  <img src="https://img.shields.io/badge/Kotlin-2.1.21-purple.svg" alt="Kotlin 2.1.21">
  <img src="https://img.shields.io/badge/Architecture-arm64--v8a-orange.svg" alt="arm64-v8a">
  <img src="https://github.com/thaomaitam/cpatcher/actions/workflows/main.yml/badge.svg" alt="CI Build">
</p>

## Technical Overview

Cpatcher is an advanced Xposed Framework module implementing a universal runtime patching system for Android applications. Engineered with resilience against obfuscated code through dynamic fingerprinting and cached mapping tables, it provides a robust framework for systematic application modification without requiring source code access.

### Core Capabilities

- **Dynamic Method Fingerprinting**: Leverages DexKit 2.0.6 for bytecode pattern analysis
- **Obfuscation-Resilient Architecture**: Cached fingerprint tables with versioning support
- **Zero-Overhead Hook Management**: Inline Kotlin extensions for performance-critical operations
- **Multi-Version Compatibility**: Adaptive patching strategies across Android versions
- **Hot-Loading Support**: Runtime patch injection without process restart

## Architecture Design

### Technical Stack

```
Framework Layer:
├── Xposed Framework (API 93+)
├── DexKit Native Bridge (2.0.6)
└── Kotlin Coroutines (JVM 17)

Application Layer:
├── Bridge Pattern Implementation (Java interop)
├── Handler-based Architecture (IHook abstraction)
└── Reflection Utility Framework

Runtime Layer:
├── Method Interception Engine
├── Fingerprint Cache System
└── Dynamic Class Loading
```

### Module Structure

```
io.github.cpatcher/
├── Entry.kt                    # Xposed IXposedHookLoadPackage implementation
├── arch/                       # Core architecture components
│   ├── IHook.kt               # Base handler abstraction pattern
│   ├── HookUtils.kt           # Xposed extension functions library
│   ├── ObfsUtils.kt           # DexKit fingerprinting cache system
│   ├── ReflectUtil.kt         # Type-safe reflection utilities
│   └── ExtraField.kt          # Xposed additional field storage
├── bridge/                     # Java-Xposed API wrapper layer
│   ├── HookParam.java         # Method parameter encapsulation
│   ├── MethodHookCallback.java # Callback abstraction interface
│   └── Xposed.java            # Core Xposed bridge implementation
└── handlers/                   # Application-specific patch implementations
    ├── QslockHandler.kt       # SystemUI security enhancements
    └── TermuxHandler.kt       # Terminal emulator modifications
```

## Implementation Patterns

### Handler Development Pattern

```kotlin
class CustomHandler : IHook() {
    companion object {
        private const val TABLE_VERSION = 1
        private const val KEY_TARGET_METHOD = "target_method_key"
    }
    
    override fun onHook() {
        // Phase 1: Package validation
        if (loadPackageParam.packageName != "target.package") return
        
        // Phase 2: Obfuscation-resilient fingerprinting
        val obfsTable = createObfsTable("custom", TABLE_VERSION) { bridge ->
            val targetMethod = bridge.findMethod {
                matcher {
                    usingStrings = listOf("unique_fingerprint")
                    returnType = "boolean"
                    modifiers = Modifier.PRIVATE or Modifier.FINAL
                }
            }.singleOrNull() ?: error("Fingerprint resolution failed")
            
            mapOf(KEY_TARGET_METHOD to targetMethod.toObfsInfo())
        }
        
        // Phase 3: Runtime hook application
        val methodInfo = obfsTable[KEY_TARGET_METHOD]!!
        findClass(methodInfo.className).hookAfter(methodInfo.memberName) { param ->
            // Modification logic
            param.result = processResult(param.result)
        }
        
        logI("${this::class.simpleName}: Successfully initialized")
    }
}
```

### Fingerprinting Strategy

```kotlin
// Multi-criteria fingerprinting for maximum resilience
bridge.findMethod {
    matcher {
        // Combine multiple stable characteristics
        usingStrings = listOf("stable_string_literal")
        returnType = "expected.return.Type"
        paramTypes = listOf("param.Type1", "param.Type2")
        modifiers = Modifier.PUBLIC or Modifier.FINAL
        annotations = listOf("Landroid/annotation/RequiresApi;")
    }
}
```

## Installation & Configuration

### Requirements

- **Device**: Android 8.0+ (API 26)
- **Architecture**: ARM64-v8a exclusively
- **Framework**: LSPosed/EdXposed with API 93+
- **Root Access**: Magisk 24+ recommended

### Build Instructions

```bash
# Clone repository
git clone https://github.com/thaomaitam/cpatcher.git
cd cpatcher

# Configure signing (optional)
cat > keystore.properties << EOF
storeFile=/path/to/keystore.jks
storePassword=password
keyAlias=alias
keyPassword=password
EOF

# Build release APK
./gradlew assembleRelease

# Output location
ls -la app/build/outputs/apk/release/
```

### Module Activation

1. Install APK via LSPosed Manager
2. Enable module in LSPosed settings
3. Select target applications from scope list
4. Force-stop target applications
5. Verify activation via logcat

## Current Implementations

| Handler | Target Package | Functionality | Status |
|---------|---------------|---------------|---------|
| QslockHandler | com.android.systemui | Disable Quick Settings on lockscreen | Stable |
| TermuxHandler | com.termux | Terminal view autofill bypass, task persistence | Stable |

## Development Guidelines

### Adding New Handlers

1. Create `handlers/[AppName]Handler.kt` extending `IHook`
2. Implement obfuscation-resilient fingerprinting via `createObfsTable`
3. Apply hooks using project utility functions exclusively
4. Register in `Entry.kt` with package name mapping
5. Add target to `res/values/arrays.xml` xposed_scope

### Debugging

```bash
# Monitor module initialization
adb logcat -s Cpatcher:V

# Trace hook execution
adb logcat | grep -E "Cpatcher|Xposed"

# Verify fingerprint cache
adb shell ls -la /data/data/[package]/cache/obfs_table_*.json
```

## Technical Considerations

### Security Model

- **Isolation**: Each handler operates in isolated context
- **Validation**: Strict package name verification before hook application  
- **Error Containment**: Comprehensive exception handling prevents system crashes
- **Cache Integrity**: Fingerprint tables versioned and validated

### Performance Optimization

- **Inline Functions**: Critical path hooks use Kotlin inline extensions
- **Lazy Initialization**: DexKit native library loaded on-demand
- **Cache Persistence**: Fingerprint results cached to minimize analysis overhead
- **Selective Hooking**: Conditional execution based on runtime state

## Contributing

### Code Quality Requirements

- Strict adherence to project utility functions (no raw reflection)
- Comprehensive error handling with fallback strategies
- Descriptive logging with class context prefix
- Multi-version compatibility through ObfsTable versioning

### Pull Request Guidelines

1. Follow established handler patterns
2. Include comprehensive fingerprinting criteria (minimum 3)
3. Implement primary and fallback hook strategies
4. Provide detailed commit messages with technical rationale

## License

```
Copyright (C) 2024 thaomaitam

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Acknowledgments

- [DexKit](https://luckypray.org/DexKit/en/): a high-performance dex runtime parsing library.
- [5ec1cff](https://github.com/5ec1cff/MyInjector): Perfect extension.

---

<p align="center">
<b>⚠️ Educational Purpose Only</b><br>
This project is intended for research and educational purposes.<br>
Users are responsible for compliance with applicable laws and terms of service.
</p>