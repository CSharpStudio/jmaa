package org.jmaa.sdk.core;

import org.jmaa.sdk.Manifest;

public class MetaModule {
    String packageName;
    Manifest manifest;

    public MetaModule(String packageName, Manifest manifest) {
        this.packageName = packageName;
        this.manifest = manifest;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }
}
