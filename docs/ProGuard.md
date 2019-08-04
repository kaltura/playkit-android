
# Playkit ProGuard HowTo

## Suggested ProGuard Configuration
```
-keep class com.kaltura.netkit.utils.** { *; }
-keep class com.kaltura.playkit.providers.** { *; }
-keep class com.kaltura.playkit.plugins.** { *; }
-keep class com.kaltura.playkit.plugins.ads.ima.** { *; }
-keepclassmembers enum com.kaltura.playkit.ads.** { *; }
-keep class com.kaltura.playkit.* { *; } ## needed only for apps using MockMediaProvider
-dontwarn okio.**
```

## Note

In case Download `dtglib` library is used you might need to keep it as well

```
-keep class com.kaltura.dtg.** { *; }
```
