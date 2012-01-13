/*

Project Name:		Routing in SunSPOTs Wireless Sensors Nerworks
Project Purpose:	Education, Distributed Software Development Course 
Project Supervisor: 	Sami Rollings, USF Professor 
Participants:		Ali Alnajjar USF MS Web Science Student
Contact:		najjaray@gmail.com
Requirement URL:	https://sites.google.com/site/usfcs685s10/a/project-3---routing
 */

package org.sunspotworld;


import com.sun.spot.util.IEEEAddress;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.peripheral.radio.mhrp.aodv.AODVManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.io.j2me.radiogram.*;


import java.io.*;
import javax.microedition.io.*;
/**
 *
 * @author najjaray
 * this class is for the base station to collect data and save it to the main server
 */
public class AODVReceiver implements Runnable
{
public RadiogramConnection conn;
public boolean NeedInterrupt = false;
public boolean running = true;
private int Counter = 0;

public void StopRead()
{
    try
    {
        if (NeedInterrupt)
        {
            conn.close();
        }
    running = false;
    }
    catch(IOException e){
    System.out.println("Interrupting connection ...");
    }
}
public void run()
{
    long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
    String SenderAddr;
    String SenderMsg;
    System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
    AODVManager AODVReader = AODVManager.getInstance();
    RadioFactory.getRadioPolicyManager().setOutputPower(-29);
         try
         {
            RadiogramConnection conn = (RadiogramConnection) Connector.open("radiogram://:100");
            Radiogram rdg = (Radiogram)conn.newDatagram(conn.getMaximumLength());

            while (running)
            {
                try
                {
                    NeedInterrupt = true;
                    conn.receive(rdg);
                    NeedInterrupt = false;
                    SenderAddr = rdg.getAddress().toString();
                    SenderMsg = rdg.readUTF();
                    Counter++;

                    BufferedWriter temp = new BufferedWriter(new FileWriter("TemperatureData.csv", true));
                    temp.write(SenderAddr + "," + SenderMsg.substring(0,SenderMsg.indexOf("+")) + "\n");
                    temp.close();
                    BufferedWriter log = new BufferedWriter(new FileWriter(SenderAddr+"-log.csv", true));
                    log.write("(" + Counter + ") " + SenderMsg.substring(SenderMsg.indexOf("+")+1,SenderMsg.indexOf("*")) + "\n");
                    log.close();
                    BufferedWriter stat = new BufferedWriter(new FileWriter(SenderAddr+"-stat.csv", true));
                    stat.write("(" + Counter + ") " + SenderMsg.substring(SenderMsg.indexOf("*")+1) + "\n");
                    stat.close();
                }
                catch (NoRouteException e)
                {
                    System.out.println("No route to any one ...");
                }
            }
        }
        catch(Exception e){
           System.out.println("problem in the connection ...");
        }
    }
}
