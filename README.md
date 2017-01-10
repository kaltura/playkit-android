# Kaltura Player SDK
## Note: The Kaltura SDK v3 is in beta

*If you are a Kaltura customer, please contact your Kaltura Customer Success Manager to help facilitate use of this component.*

The **Kaltura Player SDK** is fully native and introduces significant performance improvements. The SDK is intended to be integrated in any Android application and includes the following features:

* Online and Offline Playback
* Live
* Multi audio tracks
* Multiple captions
* Kaltura’s uDRM support (Widevine Modular, Widevine Classic)
* VAST Support (IMA)
* Kaltura analytics
* Youbora
* Chromecast support

Further documentation is coming soon.

[![CI Status](http://img.shields.io/travis/kaltura/playkit-android.svg?style=flat)](https://travis-ci.org/kaltura/playkit-android)

## SDK integration using a compiled version:
 
You can integrate the Kaltura Android Player SDK directly into your gradle project using a compiled version by jcenter. This is useful when you intend to run automated builds on an integration server.
 
###PlayKit JCenter Available Versions exists in the following link:
 [PlayKit JCenter](https://bintray.com/kaltura/android/playkit/)
  
 In your app level _**`app/build.gradle`**_ add the following:
 
```javascript 
 
 dependencies {
    compile 'com.android.support:appcompat-v7:24.2.1'
    compile 'com.android.support:design:24.2.1'
    compile 'com.github.kaltura:playkit-dtg-android:v2.0.0.+'
    compile 'com.kaltura.playkit:playkit:0.1.3'
} 

```
If you decide to integrate the whole code than just replace `compile 'com.kaltura.playkit:playkit:0.1.3' ` 
<br/> with <br/>
`compile project(path: ':playkit')`



## License and Copyright Information
All code in this project is released under the [AGPLv3 license](http://www.gnu.org/licenses/agpl-3.0.html) unless a different license for a particular library is specified in the applicable library path.   

Copyright © Kaltura Inc. All rights reserved.   
Authors and contributors: See [GitHub contributors list](https://github.com/kaltura/playkit-android/graphs/contributors).  
