# kotlinx.serialization — keep generated serializers for type-safe navigation routes.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class io.cosgriff.glovebox.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn org.slf4j.**

# Tesseract4Android (reading the odometer off the dash). Its native code looks these classes, fields and
# methods up by name, and the library ships no rules of its own.
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }

# PDFBox for Android (reading a Carfax report). Its JPEG 2000 decoder is an optional library it doesn't ship;
# text extraction never needs it.
-dontwarn com.gemalto.jp2.JP2Decoder
