package com.djcrontab.code;

import java.util.Arrays;


abstract public class DebugBase
{
   abstract public void out(boolean connect, String... s);

   void closeSocket()
   {
   }

   public void out(Exception e)
   {
      out(e.fillInStackTrace().getMessage());
   }

   public void out(String... s)
   {
      System.out.println(Arrays.deepToString(s));
   }
}
