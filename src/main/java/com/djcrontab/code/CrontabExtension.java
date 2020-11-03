package com.djcrontab.code;

import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;

import javax.sound.midi.MidiDevice;
import java.util.Date;

public class CrontabExtension extends ControllerExtension
{
   @Override
   public void init()
   {
      getHost().println("started: " + new Date().toString());
      mDebug.out("reporting in" + new Date().toString());
      visit();
   }

   void visit()
   {
      final UserControlBank controls = getHost().createUserControls(1);
      final Parameter control = controls.getControl(0);
      control.setIndication(true);
      control.setLabel("test control");
      mDebug.out("control setup");


      final Preferences preferences = getHost().getPreferences();
      final SettableBooleanValue setting = preferences.getBooleanSetting("Prout", "General", false);
      final SettableStringValue label = preferences.getStringSetting("Value", "General", 10, "");

      setting.addValueObserver(newValue -> {
         mDebug.out(String.format("set to %s", newValue));
         i++;
         label.set(String.format("%d", i));
      });

      final CursorTrack cursorTrack = getHost().createCursorTrack("Track Main", "Main", 1, 1, true);
      final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice("Device Main", "Main", 1, CursorDeviceFollowMode.FOLLOW_SELECTION);
      final CursorRemoteControlsPage page = cursorDevice.createCursorRemoteControlsPage("Cursor Page", 2, "fx");
      page.selectedPageIndex().addValueObserver(newValue -> {
         mDebug.out(String.format("page %s", newValue));
      }, 0);
      cursorDevice.name().addValueObserver(newValue -> {
         mDebug.out(String.format("device %s", newValue));
      });
      cursorDevice.position().addValueObserver(newValue -> {
         cursorDevice.selectInEditor();
      });
      cursorTrack.position().addValueObserver(newValue -> {
         cursorDevice.selectFirst();
      });
      cursorTrack.selectSlot(1);
      //cursorTrack.setCursorNavigationMode(CursorNavigationMode.GUI);
      cursorTrack.selectFirstChild();

      final PinnableCursorDevice userCursor = cursorTrack.createCursorDevice();
      userCursor.addDirectParameterIdObserver(newValue -> {
                 mDebug.out("usercursor");
                 mDebug.out(newValue);
                 cursorDevice.selectDevice(userCursor);
              }
      );
      userCursor.name().addValueObserver(newValue -> {
         mDebug.out("usercursor Value");
         mDebug.out(newValue);
         cursorDevice.selectDevice(userCursor);
      });
   }

   @Override
   public void exit()
   {
      if (mDevice != null && mDevice.isOpen())
      {
         mDevice.close();
         mDevice = null;
      }
      mDebug.out(false, "Bye!");
      mDebug.closeSocket();
   }

   @Override
   public void flush()
   {

   }

   private void initMidi()
   {
      final MidiIn inPort = getMidiInPort(0);

      for (int channel = 0; channel < 16; channel++)
      {
         final NoteInput noteInput = inPort.createNoteInput(
                 "Ch " + (channel + 1),
                 String.format("8%X????", channel),
                 String.format("9%X????", channel),
                 String.format("B%X????", channel));
         noteInput.setShouldConsumeEvents(false);
      }

      final NoteInput noteInput = inPort.createNoteInput("Omni", "8?????", "9?????", "B?????", "D?????", "E?????");
      noteInput.setShouldConsumeEvents(false);
   }

   protected CrontabExtension(
           final ControllerExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
      mDebug = new DebugSocket();

      mMidiConnect = new MidiConnect(mDebug);
      mMidiConnect.getMidi();
   }

   public static final int MIDI_OUT_PORTS = 0;
   public static final int MIDI_IN_PORTS = 0;
   private final DebugSocket mDebug;
   private MidiDevice mDevice;
   private final MidiConnect mMidiConnect;
   private int i;
}
