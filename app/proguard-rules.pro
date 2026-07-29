# Preserve generic and annotation metadata used by Android and library APIs
# without keeping the application's implementation classes wholesale.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations

# ML Kit, Media3, Compose, and AndroidX ship their own consumer rules. Add
# feature-specific rules here only when a release-build test proves they are
# required; broad keep rules would defeat shrinking and obfuscation.

# Debug and verbose timing logs are not useful in production builds.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}