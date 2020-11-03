package com.djcrontab.code;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class CrontabExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("5ae9d5a9-42a3-4a9a-a8f8-4d767308c09d");

    public CrontabExtensionDefinition() {
    }

    @Override
    public String getName() {
        return "dj-crontab-extensions";
    }

    @Override
    public String getAuthor() {
        return "Vincent Alsteen";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareVendor() {
        return "dj-crontab";
    }

    @Override
    public String getHardwareModel() {
        return "dj-crontab-extensions";
    }

    @Override
    public int getRequiredAPIVersion() {
        return 12;
    }

    @Override
    public int getNumMidiInPorts() {
        return CrontabExtension.MIDI_IN_PORTS;
    }

    @Override
    public int getNumMidiOutPorts() {
        return CrontabExtension.MIDI_OUT_PORTS;
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType) {
    }

    @Override
    public CrontabExtension createInstance(final ControllerHost host) {
        return new CrontabExtension(this, host);
    }
}
