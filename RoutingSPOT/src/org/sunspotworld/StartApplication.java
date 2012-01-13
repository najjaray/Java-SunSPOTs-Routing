/*
Project Name:		Routing in SunSPOTs Wireless Sensors Nerworks
Project Purpose:	Education, Distributed Software Development Course 
Project Supervisor: 	Sami Rollings, USF Professor 
Participants:		Ali Alnajjar USF MS Web Science Student
Contact:		najjaray@gmail.com
Requirement URL:	https://sites.google.com/site/usfcs685s10/a/project-3---routing
 */

package org.sunspotworld;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ISwitch;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.*;
import com.sun.spot.sensorboard.peripheral.ITemperatureInput;
import com.sun.spot.peripheral.radio.mhrp.aodv.AODVManager;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.NoRouteException;
import java.io.*;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * The startApp method of this class is called by the VM to start the
 * application.
 * 
 * The manifest specifies this class as MIDlet-1, which means it will
 * be selected for execution.
 */
public class StartApplication extends MIDlet {

    private ITriColorLED [] leds = EDemoBoard.getInstance().getLEDs();
    private String Basestation_Addr = "0014.4F01.0000.2B79";
    private int sampling_rate = 3;
    public int CurVersion = 0;
    private ITemperatureInput Ftemp = EDemoBoard.getInstance().getADCTemperature();
    private double TempNow;
    private String Stemp;
    private Thread MsgLiT;
    private Thread TrickleTh;
    private Trickle TrickleObj;
    private MessageListener MsgLi;
    private RadiogramConnection MsgSender;
    private String MsgBody = "";

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("[SPOT] Main Sttr");
        new BootloaderListener().start();   // monitor the USB (if connected) and recognize commands from host

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
        AODVManager AODVSender = AODVManager.getInstance();
        RadioFactory.getRadioPolicyManager().setOutputPower(-29);
        ISwitch sw1 = EDemoBoard.getInstance().getSwitches()[EDemoBoard.SW1];
        MsgLi = new MessageListener();
        MsgLiT = new Thread(MsgLi);
        TrickleObj = new Trickle();
        TrickleTh = new Thread(TrickleObj);
        MsgLiT.start();
        TrickleTh.start();
        while (sw1.isOpen())
        {                  // done when switch is pressed
            while(true)
            {
                //MsgLi.Trickle_tc++;
                // the color of it shows the version changes if the colore changed
                // from blue to white of white to blue thats means thera is a
                // a new version implemented
                leds[7].setOff();
                if (MsgLi.CurUpdate % 2 ==0)
                {
                     leds[7].setColor(LEDColor.RED);
                }
                else
                {
                    leds[7].setColor(LEDColor.WHITE);
                }
                leds[7].setOn();

                // trun on the LEDs the LEDs shows us the sampling rate
                for (int i = 0; i<sampling_rate;i++)
                {
                    leds[i%5].setOff();
                }
                sampling_rate = MsgLi.SampleRate;
                // ******************************
                // Getting the Current Temperture
                // ******************************

                System.out.println("[SPOT] getting the Temperature");
                try
                {
                    // Get the Temperature in Fahrenheit (SPOT function)
                    TempNow = Ftemp.getFahrenheit();
                    // Putting the Temp value in Sring form.
                    Stemp = String.valueOf(TempNow);
                    // Putting the Temperature in 2 digit format
                    if (Stemp.length() > 5)
                    {
                        Stemp = Stemp.substring(0,5);
                    }
                    System.out.println("[SPOT] Temperatue = "+ Stemp);
                }
                catch (IOException ex)
                { //A problem in reading the sensors.
                        ex.printStackTrace();
                }

                // *******************************
                // Sending Temp to Basestation
                //       using SunSpot SDK AODV
                // *******************************

                try
                {
                    MsgBody = Stemp + "+" + MsgLi.log + "*";
                    if ((MsgLi.NumOfFloodedUpd %10) == 0)
                    {
                    MsgBody = MsgBody + ":Flood R=" + MsgLi.NumOfRecFloodedMsgs +",S="+ MsgLi.NumOfSenFloodedMsgs +",U="+ MsgLi.NumOfFloodedUpd;
                    }
                    if ((MsgLi.NumOfTrickleUpd % 10) == 0)
                    {
                    MsgBody = MsgBody + ":Trickle R=" + MsgLi.NumOfRecTrickleMsg +", S="+ MsgLi.NumOfSenTrickleMsg +", U="+ MsgLi.NumOfTrickleUpd;
                    MsgBody = MsgBody + ":Metadata R=" +MsgLi.NumOfMetadataReci+", S="+ MsgLi.NumOfMetadataSent ;
                    }

                    MsgSender = (RadiogramConnection)Connector.open("radiogram://" + Basestation_Addr + ":100");
                    MsgSender.setMaxBroadcastHops(1);
                    Datagram PingDG = MsgSender.newDatagram(MsgSender.getMaximumLength());
                    PingDG.writeUTF(MsgBody);

                    try
                    {

                        MsgSender.send(PingDG);
                        leds[6].setOff();
                        leds[6].setColor(LEDColor.BLUE);
                        leds[6].setOn();
                        MsgSender.close();
                        //System.out.println(AODVSender.getRoutingTable());
                    }
                    catch (NoRouteException e)
                    {
                        System.out.println("[SPOT] No route to Basestation");
                        MsgSender.close();
                        leds[6].setOff();
                        leds[6].setColor(LEDColor.RED);
                        leds[6].setOn();
                    }
                }
                catch (IOException e)
                {
                    System.out.println("[SPOT] Can not send Temp");
                        leds[6].setOff();
                        leds[6].setColor(LEDColor.RED);
                        leds[6].setOn();
                }
                // trun off the LEDs the LEDs shows us the sampling rate
                for (int i = 0; i<sampling_rate;i++)
                {
                leds[i%5].setColor(LEDColor.BLUE);
                leds[i%5].setOn();
                Utils.sleep(1000);                  // wait 1 second
                }
                

            }                    // Blink LED
        }
        leds[0].setOff();
        notifyDestroyed();                      // cause the MIDlet to exit
    }

    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system.
     * I.e. if startApp throws any exception other than MIDletStateChangeException,
     * if the isolate running the MIDlet is killed with Isolate.exit(), or
     * if VM.stopVM() is called.
     * 
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true when this method is called, the MIDlet must
     *    cleanup and release all resources. If false the MIDlet may throw
     *    MIDletStateChangeException  to indicate it does not want to be destroyed
     *    at this time.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        for (int i = 0; i < 8; i++) {
            leds[i].setOff();
        }
    }
}
