package main;

import com.djcrontab.code.DebugTerminal;
import com.djcrontab.code.MidiConnect;

import java.io.IOException;

public class Main
{
    public static void main(String[] args) throws IOException
    {
        final DebugTerminal debug = new DebugTerminal();
        final MidiConnect m = new MidiConnect(debug);
        m.getMidi();
        debug.out("hi");
        System.in.read();
    }
}
