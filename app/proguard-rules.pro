# kotlinx.serialization — keep generated serializers for type-safe navigation routes.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class io.cosgriff.glovebox.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn org.slf4j.**
