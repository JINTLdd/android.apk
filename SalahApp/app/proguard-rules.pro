# Keep model classes (used by Gson / serialization)
-keep class com.salah.app.models.** { *; }

# Keep BroadcastReceivers / Services (declared in manifest)
-keep class com.salah.app.receivers.** { *; }
-keep class com.salah.app.services.** { *; }

# Standard Android
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn java.lang.invoke.**
