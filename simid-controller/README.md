# SIMID controller for Android platforms

Library/module that provides a base SIMID controller (a.k.a. as SIMID player) as specified by IAB: https://interactiveadvertisingbureau.github.io/SIMID/simid-1.1.0.html

The SIMID controller component can be integrated into any android application in order to load SIMID creatives and to support the SIMID protocol that enables communication with the SIMID creative.

In the main application, when the application wants to load and display a SIMID creative, it has to create a ``tv.broadpeak.simid.controller.SimidController`` class instance that will create a WebView and load the SIMID creative into a iframe in the WebView, communicate with it through the SIMID protocol and trigger the display of the SIMID iframe (visible state, dimensions). 

The following source code illustrates how to instantiate and manage the provided ``tv.broadpeak.simid.controller.SimidController``.

```kotlin
// 1 - Creates SIMID controller 
import tv.broadpeak.simid.controller.SimidController
val simidController = SimidController(activity, context, playerRect, creativeUri, adParameters, duration)

// 2 - Provides callback functions to the SIMID controller which delegates UI process to the application

simidController.onGetMediaState {
  // Returns media state such as duration, currentTime, paused etc
  return {
    currentTime: videoElement.currentTime
    // ...
  }
}

simidController.onAddSimid { iframe: HTMLIFrameElement -> 
  // Called by the SIMID controller when a SIMID iframe needs to be integrated into current DOM
}

simidController.onShowSimid { show: boolean ->
  // Called by the SIMID controller when to show or hide the SIMID iframe
}

simidController.onResizeSimid { dimensions: DOMRect ->
  // Called by the SIMID controller when the SIMID iframe has to be resized
}
  
simidController.onResizePlayer { dimensions: DOMRect ->
  // Called by the SIMID controller when the main player element has to be resized
}

simidController.onComplete = {skipped: Boolean ->
  // Called by the SIMID controller when the SIMID non-linear ad is completed  with indication if ad has been skipped
}

// 3 - Loads the SIMID creative
// Once SIMID iframe is loaded (see onAddSimid callback) the SIMID creative and controller will initiate the session 
simidController.load()
```
