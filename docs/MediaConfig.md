
# Media Config

Once you have `PKMeidaEntry` which you have created manually or received from one of the `providers` you will have to create `PKMediaConfig` object which is the input to the playkitPlayer.prepare method.

``` java
mediaConfig = new PKMediaConfig();

 //Set media entry we received from provider.
 mediaConfig.setMediaEntry(mediaEntry);


 /* The following configs are optional */

 //Add strat Position is interested to start from position different than 0
 mediaConfig.setStartPosition(30);

 // --->  SELECTING preferred TEXT TRACKS
 //mediaConfig.setPreferredTextTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.OFF)); // no text tracks
 //mediaConfig.setPreferredTextTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.EXPLICIT).setTrackLanguage("hi")); // select specific track lang if not exist select manifest default
 mediaConfig.setPreferredTextTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.AUTO)); // select the track by locale if does not exist manifest default

 // --->  SELECTING preferred AUDIO TRACKS
 //mediaConfig.setPreferredTextTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.EXPLICIT).setTrackLanguage("ru")); // select specific track lang if not exist select manifest default
 mediaConfig.setPreferredAudioTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.AUTO));



```

### Set Start Position
```
setStartPosition(long startPosition)
```
This API receives the start position in seconds default is position 0.

### Set Preferred Audio/Text Track

In order to configure this behaviour you have to instantiate PKTrackConfig
This object can be created via buildr methods inorderto add the Config Mode and the config language
using:

```
 public PKTrackConfig setTrackLanguage(String trackLanguage)
 public PKTrackConfig setPreferredMode(@NonNull Mode preferredMode)
```

#### Example

```
- PKTrackConfig trackConfig = new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.OFF);
- PKTrackConfig trackConfig = new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.AUTO);
- PKTrackConfig trackConfig = new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.EXPLICIT).setTrackLanguage("ru")
```

Once we have the `PKTrackConfig` object we can use it as parameter for the api.

```
setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig)
setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig)
```

## Configuration possibilities

### There are 3 modes available:

 - `OFF` - for Text tracks it will cancel text tracks display for audio it will select the default in the manifest
 - `AUTO` - SDK will check if the stream has audio/text track that matches the device locale and will select it else it will take stream default
 - `EXPLICIT` - this mode requires to set the language explicitly (2 or 3 letters)  if this language does not exist SDK will take the streams
default Audio/Ttext track

### NOTE!!!
The languages that are expected by player should match the SO 639-X codes definition
```
