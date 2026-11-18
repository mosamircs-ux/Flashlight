# Keep Android components & widgets
-keep public class com.luminatorch.flashlight.widgets.** { *; }
-keep public class com.luminatorch.flashlight.tiles.** { *; }

# Keep Google Mobile Ads SDK classes (SDK also bundles its own consumer rules)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
