package com.djcrontab.code;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;

public class MidiConnect {
   public MidiConnect(DebugBase debug)
   {
      mDebug = debug;
      mMidiReceiver = new MidiReceiver(mDebug);
   }

   public void getMidi()
   {
      final MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
      for (MidiDevice.Info i : info)
      {
         if (i.getName().equals("Launch Control XL"))
         {
            try
            {
               mDevice = MidiSystem.getMidiDevice(i);
               mDevice.open();
               final Transmitter mMidiTransmitter = mDevice.getTransmitter();
               mMidiTransmitter.setReceiver(this.mMidiReceiver);

            }
            catch (MidiUnavailableException e)
            {
               mDebug.out(e);
               continue;
            }

            return;
         }
      }
   }

   public void exit()
   {
      if (mDevice != null && mDevice.isOpen())
      {
         mDevice.close();
      }
   }

   private final MidiReceiver mMidiReceiver;
   private final DebugBase mDebug;
   private MidiDevice mDevice;
}
