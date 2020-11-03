package com.djcrontab.code;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

class MidiReceiver implements Receiver
{

   MidiReceiver(DebugBase d)
   {
      mDebug = d;
   }

   @Override
   public void send(final MidiMessage message, final long timeStamp)
   {
      mDebug.out(message.toString());
   }

   @Override
   public void close()
   {

   }


   private final DebugBase mDebug;
}
