package com.djcrontab.code;

public class DebugTerminal extends DebugBase
{

   @Override
   public void out(final boolean connect, final String... s)
   {
      out(s);
   }
}
