package com.kaltura.playkit.player.vr

data class VRBarrelDistortionConfig(var paramA: Double = -0.068, // affects only the outermost pixels of the image
                                    var paramB: Double = 0.320000, // most cases only require b optimization
                                    var paramC: Double = -0.2, // most uniform correction
                                    private var _scale: Float = DEFAULT_BARREL_DISTORTION_SCALE,
                                    var defaultEnabled: Boolean = false) {
    var scale = _scale
        set(value) {
            if (value < 0.10 || value > 1.0) {
                field = DEFAULT_BARREL_DISTORTION_SCALE
            } else {
                field = value
            }
        }

    companion object {
        const val DEFAULT_BARREL_DISTORTION_SCALE = 0.95f
    }
}

