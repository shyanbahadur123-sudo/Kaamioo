-keepattributes *Annotation*, Signature, Exception, InnerClasses, EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.kaamio.nepal.data.** { *; }
-keep class com.kaamio.nepal.ui.theme.** { *; }

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

-keepclassmembers class * extends androidx.room.RoomDatabase { abstract <methods>; }

-keepclassmembers class * { @com.google.firebase.firestore.PropertyName *; @com.google.firebase.firestore.IgnoreExtraProperties *; }

-keepclassmembers class kotlinx.coroutines.** { *; }

# Coil optimization
-keep class coil.** { *; }
-dontwarn coil.**
-keepclassmembers class * {
    @coil.api.DoNotInline <methods>;
}

# Keep Compose runtime for performance
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class * { @androidx.compose.runtime.Stable *; @androidx.compose.runtime.Immutable *; }

# Khalti Checkout SDK (reflection-based callbacks)
-keep class com.khalti.** { *; }
-dontwarn com.khalti.**
-keepclassmembers class * {
    @com.khalti.checkout.* <methods>;
}

# Firebase Functions serialization
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
    @com.google.firebase.firestore.IgnoreExtraProperties *;
}
-keep class com.kaamio.nepal.payment.** { *; }
-keep class com.kaamio.nepal.service.** { *; }
