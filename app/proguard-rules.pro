# Add project specific ProGuard rules here.

# ──────────────────────────────────────────────
# Debugging: preserve line numbers in crash stack traces
# ──────────────────────────────────────────────
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ──────────────────────────────────────────────
# Kotlinx Serialization
# ──────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep @Serializable class companions and their KSerializer
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep KSP/kapt-generated serializer classes
-keep,includedescriptorclasses class **$$serializer { *; }

# Keep serializer() method on any class that declares it
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable data models in this app
-keep @kotlinx.serialization.Serializable class com.bossxomlut.dragspend.** { *; }

# ──────────────────────────────────────────────
# Ktor (networking client)
# ──────────────────────────────────────────────
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# ──────────────────────────────────────────────
# Supabase
# ──────────────────────────────────────────────
-dontwarn io.github.jan.supabase.**

# ──────────────────────────────────────────────
# Koin (dependency injection)
# ──────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**