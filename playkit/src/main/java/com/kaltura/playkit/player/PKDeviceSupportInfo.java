package com.kaltura.playkit.player;

import com.kaltura.playkit.PKDrmParams;
import java.util.Set;

public class PKDeviceSupportInfo {

    private Set<PKDrmParams.Scheme> supportedDrmSchemes;
    private boolean isHardwareDrmSupported;
    private boolean provisionPerformed;
    private boolean isSoftwareHevcSupported;
    private boolean isHardwareHevcSupported;

    /**
     * @param supportedDrmSchemes supported DRM schemes
     * @param isHardwareDrmSupported is Hardware DRM Supported
     * @param provisionPerformed  true if provisioning was required and performed, false otherwise
     * @param isSoftwareHevcSupported checks if only hevc s/w decoder is available on this device
     * @param isHardwareHevcSupported checks if only hevc h/w decoder is available on this device
     */

    PKDeviceSupportInfo(Set<PKDrmParams.Scheme> supportedDrmSchemes, boolean isHardwareDrmSupported, boolean provisionPerformed,
                        boolean isSoftwareHevcSupported, boolean isHardwareHevcSupported) {
        this.supportedDrmSchemes = supportedDrmSchemes;
        this.isHardwareDrmSupported = isHardwareDrmSupported;
        this.provisionPerformed = provisionPerformed;
        this.isSoftwareHevcSupported = isSoftwareHevcSupported;
        this.isHardwareHevcSupported = isHardwareHevcSupported;
    }

    public Set<PKDrmParams.Scheme> getSupportedDrmSchemes() {
        return supportedDrmSchemes;
    }

    public boolean isHardwareDrmSupported() {
        return isHardwareDrmSupported;
    }

    public boolean isProvisionPerformed() {
        return provisionPerformed;
    }

    public boolean isSoftwareHevcSupported() {
        return isSoftwareHevcSupported;
    }

    public boolean isHardwareHevcSupported() {
        return isHardwareHevcSupported;
    }

}