# Reglas de ProGuard/R8 para Domino Imperial: World Tour.
#
# NOTA IMPORTANTE: el build de release tiene `isMinifyEnabled = false`, así que
# actualmente este archivo NO se aplica. Se mantiene aquí (y referenciado desde
# app/build.gradle.kts) para que la configuración de release esté completa y para que
# activar la minificación sea cambiar una sola línea.

# ---------------------------------------------------------------------------
# Kotlinx Serialization
# ---------------------------------------------------------------------------
# La app persiste la carrera World Tour como JSON con kotlinx.serialization, que
# genera serializadores por reflexión sobre los @Serializable. Sin estas reglas, R8
# elimina los miembros generados y la carga del progreso falla en release.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.example.dominograndprix.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.dominograndprix.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.dominograndprix.**$$serializer { *; }

# Reglas oficiales de kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Jetpack Compose
# ---------------------------------------------------------------------------
# Compose se apoya en metadatos de Kotlin y en la firma de los composables.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ---------------------------------------------------------------------------
# Android / Kotlin por defecto
# ---------------------------------------------------------------------------
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn org.jetbrains.annotations.**

# ---------------------------------------------------------------------------
# Google Mobile Ads / AdMob
# ---------------------------------------------------------------------------
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ---------------------------------------------------------------------------
# Google Play Billing (IAP remove_ads)
# ---------------------------------------------------------------------------
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**
