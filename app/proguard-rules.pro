# Feature entry points are looked up by name (shell/FeatureRegistry, ui/feature/FeatureContract).
-keep class io.github.lenticualrstone.civilight.feature.**.*Entries {
    public static *** *Entries(...);
}

# Routes and sheet keys are saved and restored by class name (Navigation3 NavKeySerializer,
# RouteCodec), so their names and serializers must survive shrinking.
-keepnames class * implements androidx.navigation3.runtime.NavKey
-keepclassmembers class * implements androidx.navigation3.runtime.NavKey {
    public static ** INSTANCE;
    public static ** Companion;
    public final kotlinx.serialization.KSerializer serializer(...);
}
-keepnames class **$$serializer
