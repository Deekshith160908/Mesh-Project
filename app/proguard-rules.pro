# Keep Room entities and DAOs
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Keep Mesh models for JSON serialization
-keep class com.emergency.offgridmesh.data.model.** { *; }
-keepclassmembers class com.emergency.offgridmesh.data.model.** { *; }
-keepattributes *Annotation*,Signature

# Keep Google Play Services Nearby Connections
-keep class com.google.android.gms.nearby.** { *; }
-keep class com.google.android.gms.location.** { *; }
