package com.kaltura.playkit.player.vr

/**
 * Class gives you the ability to perform distortion on the VR lens
 * Barrel/PinCushion distortion both can be achieved by modifying the config params
 *
 * You may correct pincushion and barrel distortions simultaneously in the same image:
 * if the outer regions exhibit barrel distortion, and the inner parts pincushion,
 * you should use negative a and positive b values.
 *
 * Addition of paramA, paramB and paramC should be 1 in case if you don't want any
 * scale in the frame.
 *
 * paramA, paramB and paramC describe distortion of the frame.
 *
 * @param paramA Affects only the outermost pixels of the image. Range for is between -1.0 <-> 1.0
 * @param paramB Most cases only require b optimization. Range for is between -1.0 <-> 1.0
 * @param paramC Most uniform correction. Range for is between -1.0 <-> 1.0
 *
 * @param scale range is 0.10 <-> 1.0
 * @param defaultEnabled default is `false`
 *
 * More information about Distortion can be found here
 * https://mipav.cit.nih.gov/pubwiki/index.php/Barrel_Distortion_Correction
 */
data class VRDistortionConfig(var paramA: Double = DEFAULT_PARAM_A,
                              var paramB: Double = DEFAULT_PARAM_B,
                              var paramC: Double = DEFAULT_PARAM_C,
                              var scale: Float = DEFAULT_BARREL_DISTORTION_SCALE,
                              var defaultEnabled: Boolean = false) {

    companion object {
        const val DEFAULT_PARAM_A = -0.068
        const val DEFAULT_PARAM_B = 0.320000
        const val DEFAULT_PARAM_C = -0.2
        const val DEFAULT_BARREL_DISTORTION_SCALE = 0.95f
    }
}

