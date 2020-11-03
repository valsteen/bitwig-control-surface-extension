package com.djcrontab.code;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

class DebugSocket extends DebugBase
{
   private Socket clientSocket;
   private PrintWriter mOut;

   private boolean connect()
   {
      try
      {
         clientSocket = new Socket("127.0.0.1", 5555);
         mOut = new PrintWriter(clientSocket.getOutputStream(), true);
      }
      catch (IOException e)
      {
         System.out.println(e.fillInStackTrace().getMessage());
         closeSocket();
         return false;
      }
      return true;
   }

   private boolean renewSocket()
   {
      closeSocket();
      return connect();
   }

   @Override
   public void out(boolean connect, String... s)
   {
      if (mOut != null || (connect && renewSocket()))
      {
         mOut.println(Arrays.deepToString(s));
      }
   }

   @Override
   public void out(String... s)
   {
      out(true, s);
   }

   void closeSocket()
   {
      if (mOut != null)
      {
         mOut.close();
      }
      if (clientSocket != null)
      {
         try
         {
            clientSocket.close();
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }
      mOut = null;
      clientSocket = null;
   }


}
