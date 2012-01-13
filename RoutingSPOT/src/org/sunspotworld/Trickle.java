/*
Project Name:		Routing in SunSPOTs Wireless Sensors Nerworks
Project Purpose:	Education, Distributed Software Development Course 
Project Supervisor: 	Sami Rollings, USF Professor 
Participants:		Ali Alnajjar USF MS Web Science Student
Contact:		najjaray@gmail.com
Requirement URL:	https://sites.google.com/site/usfcs685s10/a/project-3---routing
 */

package org.sunspotworld;

import com.sun.spot.util.*;
import java.util.Random;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.sensorboard.EDemoBoard;
import javax.microedition.io.Datagram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import javax.microedition.io.Connector;
import java.io.IOException;

/**
 *
 * @author najjaray
 */
public class Trickle implements Runnable{
    private RadiogramConnection MsgSender;
    //public int Trickle_c = 0;
    public static int Trickle_k = 1;
    public static int Trickle_t = 0;
    public static int Trickle_tc = 0;

    public static int Trickle_Tl = 5;
    public static int Trickle_T = 20;
    public int Trickle_Th = 80;
    
    private String MsgPrefix = "ayalnajjar";
    private ITriColorLED [] leds = EDemoBoard.getInstance().getLEDs();
    public int CurVersion = 0;

    public void run()
    {
        while(true)
        {
            // ************************
            // Handling T expires
            // ************************
            if (Trickle_tc>=Trickle_T)
            {
                // doubling T up to Th
                Trickle_T = Trickle_T*2;
                if (Trickle_T>Trickle_Th)
                {
                    Trickle_T = Trickle_Th;
                }
                // Getting new t from the range (T/2, T)
                Random randomGenerator = new Random();
                Trickle_t = randomGenerator.nextInt(Trickle_T);
                while (Trickle_t>(Trickle_T/2))
                {
                     Trickle_t = randomGenerator.nextInt(Trickle_T);
                }
                // resetting c
                org.sunspotworld.MessageListener.Trickle_c = 0;
                // restting the timer (starting new frame)
                Trickle_tc = 0;
            }
            
            // ************************
            // Handling t expires
            // ************************
            if (Trickle_tc == Trickle_t)
            {
                for (int i=0;i<6;i++)
                {
                leds[i].setOff();
                leds[i].setColor(LEDColor.GREEN);
                leds[i].setOn();
                }
             // checking if c < k
             if (org.sunspotworld.MessageListener.Trickle_c < Trickle_k)
            {
                 // transmitting metedata
                CurVersion = org.sunspotworld.MessageListener.CurVersion;
                SendMessage(MsgPrefix + ".metada."+CurVersion+".");
                org.sunspotworld.MessageListener.NumOfMetadataSent++;
                for (int i=0;i<6;i++)
                {
                leds[i].setOff();
                leds[i].setColor(LEDColor.RED);
                leds[i].setOn();
                }
            }
            }
            // icreasing the counter (1 second)
            Utils.sleep(1000);
            Trickle_tc ++;
        }
    }
            public void SendMessage (String MessageBoday)
    {
        try
        {
            MsgSender = (RadiogramConnection)Connector.open("radiogram://broadcast:10");
            MsgSender.setMaxBroadcastHops(1);
            Datagram PingDG = MsgSender.newDatagram(MsgSender.getMaximumLength());
            PingDG.writeUTF(MessageBoday);
            MsgSender.send(PingDG);
            MsgSender.close();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
}
}



