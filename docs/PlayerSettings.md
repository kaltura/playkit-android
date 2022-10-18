
#  Player Settings


Player has settings object that can be configured for player instance

```
   interface Settings {
        /**
         * Set the Player's contentRequestAdapter.
         * @param contentRequestAdapter - request adapter.
         * @return - Player Settings.
         */
        Settings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter);

        /**
         * Enable/disable cea-608 text tracks.
         * By default they are disabled.
         * Note! Once set, this value will be applied to all mediaSources for that instance of Player.
         * In order to disable/enable it again, you should update that value once again.
         * Otherwise it will stay in the previous state.
         * @param cea608CaptionsEnabled - should cea-608 track should be enabled.
         * @return - Player Settings.
         */
        Settings setCea608CaptionsEnabled(boolean cea608CaptionsEnabled);

        /**
         * Decide if player should use {@link android.view.TextureView} as primary surface
         * to render the video content. If set to false, will use the {@link android.view.SurfaceView} instead.
         * Note!!! Use this carefully, because {@link android.view.TextureView} is more expensive and not DRM
         * protected. But it allows dynamic animations/scaling e.t.c on the player. By default it will be always set
         * to false.
         * @param useTextureView - true if should use {@link android.view.TextureView}.
         * @return - Player Settings.
         */
        Settings useTextureView(boolean useTextureView);

        /**
         * Set the Player's preferredAudioTrackConfig.
         * @param preferredAudioTrackConfig - AudioTrackConfig.
         * @return - Player Settings.
         */
        Settings setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig);

        /**
         * Set the Player's preferredTextTrackConfig.
         * @param preferredTextTrackConfig - TextTrackConfig.
         * @return - Player Settings.
         */
        Settings setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig);
    }
    
```

Once you created a player instance you can set the above settings on it.

### Example:

###create a player:
``` java
 Player player = PlayKitManager.loadPlayer(context, pluginConfigs);
```

### Apply Player Settings if required:

```
 // SELECTING if to use TextureView instead of surface view
 player.getSettings().useTextureView(false); // default is false

 // SELECTING if to consider Cea608Captions which exist stream for text track selection.
 player.getSettings().setCea608CaptionsEnabled(false); // default is false

 // SELECTING different app name in PKRequestParams.Adapter which allows adapting the request parameters before sending network requests
 KalturaPlaybackRequestAdapter.install(player, "yourApplicationName"); // default is app package name


 // SELECTING preferred TEXT TRACKS -- Default is no captions displayed.
 
 //player.getSettings().setPreferredTextTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.OFF)); // no text tracks
 //player.getSettings().setPreferredTextTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.SELECTION).setTrackLanguage("hi")); // select specific track lang if not exist select manifest default if exist else the first from manifest
 player.getSettings().setPreferredTextTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.AUTO)); // select the track by locale if does not exist manifest default

 // SELECTING preferred AUDIO TRACKS - Default is Stream's default

 //player.getSettings().setPreferredAudioTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.OFF); // default audio track (Done automatically actually)
 //player.getSettings().setPreferredAudioTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.SELECTION).setTrackLanguage("ru")); // select specific track lang if not exist select manifest default
 player.getSettings().setPreferredAudioTrack(new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.AUTO));
```


### Set Preferred Audio/Text Track

In order to configure this behaviour you have to instantiate PKTrackConfig
This object can be created via builder methods inorder to add a audio/text tracks preffered Config Mode/Language.
using:

```
 public PKTrackConfig setTrackLanguage(String trackLanguage)
 public PKTrackConfig setPreferredMode(@NonNull Mode preferredMode)
```

#### Example

```
- PKTrackConfig trackConfig = new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.OFF);
- PKTrackConfig trackConfig = new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.AUTO);
- PKTrackConfig trackConfig = new PKTrackConfig().setPreferredMode(PKTrackConfig.Mode.SELECTION).setTrackLanguage("ru")
```

Once we have the `PKTrackConfig` object we can use it as parameter for the api.

```
setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig)
setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig)
```

## Configuration possibilities

### There are 3 modes available:

 - `OFF` - for Text tracks it will cancel text tracks display for audio it will select the default from the manifest
 - `AUTO` - SDK will check if the stream has audio/text track that matches the device locale and will select it else it will take stream default
 - `SELECTION` - this mode requires to set the language explicitly (2 or 3 letters)  if this language does not exist SDK will take the streams
default Audio/Ttext track

### NOTE!!!
The languages that are expected by player should match the SO 639-X codes definition

