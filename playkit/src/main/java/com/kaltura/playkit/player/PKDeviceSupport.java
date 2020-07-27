package com.kaltura.playkit.player;

import com.kaltura.playkit.PKDrmParams;

import java.util.Set;


public class PKDeviceSupport {

    private Set<PKDrmParams.Scheme> supportedDrmSchemes;
    private boolean isHardwareDrmSupported;
    private boolean provisionPerformed;
    private boolean isSoftwareHevcSupported;
    private boolean isHardwareHevcSupported;
    private Exception provisionError;

    /**
     * @param supportedDrmSchemes supported DRM schemes
     * @param isHardwareDrmSupported is Hardware DRM Supported
     * @param provisionPerformed  true if provisioning was required and performed, false otherwise
     * @param isSoftwareHevcSupported checks if only hevc s/w decoder is available on this device
     * @param isHardwareHevcSupported checks if only hevc h/w decoder is available on this device
     * @param provisionError      null if provisioning is successful, exception otherwise
     */

    PKDeviceSupport(Set<PKDrmParams.Scheme> supportedDrmSchemes, boolean isHardwareDrmSupported, boolean provisionPerformed,
                    boolean isSoftwareHevcSupported, boolean isHardwareHevcSupported, Exception provisionError) {
        this.supportedDrmSchemes = supportedDrmSchemes;
        this.isHardwareDrmSupported = isHardwareDrmSupported;
        this.provisionPerformed = provisionPerformed;
        this.isSoftwareHevcSupported = isSoftwareHevcSupported;
        this.isHardwareHevcSupported = isHardwareHevcSupported;
        this.provisionError = provisionError;
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

    public Exception getProvisionError() {
        return provisionError;
    }
}
