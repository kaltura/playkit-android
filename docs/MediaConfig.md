
# Media Config

Once you have `PKMeidaEntry` which you have created manually or received from one of the `providers` you will have to create `PKMediaConfig` object which is the input to the playkitPlayer.prepare method.

``` java
mediaConfig = new PKMediaConfig();

 //Set media entry we received from provider.
 mediaConfig.setMediaEntry(mediaEntry);


 /* The following config is optional */

 //Add strat Position is interested to start from position different than 0
 mediaConfig.setStartPosition(30);

```

### Set Start Position
```
setStartPosition(long startPosition)
```
This API receives the start position in seconds default is position 0.


