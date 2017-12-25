
# Playkit ProGuard HowTo

## Suggested progaurd configuration for apps that use playkit SDK. 
```
-keep class com.kaltura.playkit.api.ovp.** { *; }
-keep class com.kaltura.playkit.api.base.** { *; }
-keep class com.kaltura.playkit.api.phoenix.** { *; }
-keep class com.kaltura.playkit.plugins.ott.** { *; }
-keep class com.kaltura.playkit.plugins.ovp.** { *; }
-keep class com.kaltura.playkit.plugins.ima.IMAConfig { *; }     ### from  v4.x.x
-keep class com.kaltura.playkit.plugins.ads.ima.IMAConfig { *; } ### until v3.x.x
-keepclassmembers enum com.kaltura.playkit.ads.AdTagType { *; } 
-keep class com.kaltura.playkit.* { *; } ## needed only for apps using MockMediaProvider
-dontwarn okio.**
```

## Note

In case Download `dtglib` library is used you might need to keep it as well

```
-keep class com.kaltura.dtg.** { *; }
```
