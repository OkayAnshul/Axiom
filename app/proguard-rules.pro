# Axiom R8 configuration.
#
# Most libraries here (Hilt, Room, Glance, WorkManager) ship consumer rules and need
# nothing from us. The rules below cover the two things R8 cannot infer on its own:
# kotlinx-serialization's generated serializers and Ktor's reflective bits.
#
# NOTE: breakage from these rules is release-only and invisible to assembleDebug.
# After changing anything here, install the release bundle and exercise the AI path
# (Companion chat / Connect AI test button) — that is where serialization actually runs.

# Keep crash-report line numbers useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Generic signatures are needed to deserialize into generic types.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

##---------------------------------------------------------------------------------
## kotlinx.serialization — used for the Groq DTOs in data/ai/dto/GroqDtos.kt and the
## curated-prompts asset in data/database/seed/CuratedPromptsAsset.kt.
##---------------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep the generated Companion + serializer() for every @Serializable class.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Our own serializable models — belt and braces, these are small.
-keep,includedescriptorclasses class com.cosmiclaboratory.axiom.**$$serializer { *; }
-keepclassmembers class com.cosmiclaboratory.axiom.** {
    *** Companion;
}
-keepclasseswithmembers class com.cosmiclaboratory.axiom.** {
    kotlinx.serialization.KSerializer serializer(...);
}

##---------------------------------------------------------------------------------
## Ktor client (Groq transport)
##---------------------------------------------------------------------------------
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Ktor pulls slf4j through its logging plugin; there is no binding on Android.
-dontwarn org.slf4j.**
-dontwarn javax.naming.**

##---------------------------------------------------------------------------------
## Room — generated implementations are static, but entities/DAOs are referenced by
## name from the generated code and TypeConverters are resolved reflectively.
##---------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

##---------------------------------------------------------------------------------
## Hilt / Dagger — ships consumer rules, but WorkManager's HiltWorkerFactory resolves
## @HiltWorker classes by name from the manifest-declared factory.
##---------------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

##---------------------------------------------------------------------------------
## Glance app widgets — receivers are instantiated by name from the manifest.
##---------------------------------------------------------------------------------
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class com.cosmiclaboratory.axiom.widget.** { *; }

# Quick Settings tile service, also resolved by name from the manifest.
-keep class com.cosmiclaboratory.axiom.tile.** { *; }

##---------------------------------------------------------------------------------
## Tink, via androidx.security:security-crypto (SecureKeyStore's EncryptedSharedPrefs).
##
## Tink is compiled against ErrorProne's annotations, which are compile-time only and
## deliberately absent at runtime. Without this, R8 hard-fails the release build.
##---------------------------------------------------------------------------------
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.**

##---------------------------------------------------------------------------------
## Compose / misc
##---------------------------------------------------------------------------------
-dontwarn org.jetbrains.annotations.**
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable *;
}
