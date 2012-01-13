/*
Project Name:		Routing in SunSPOTs Wireless Sensors Nerworks
Project Purpose:	Education, Distributed Software Development Course 
Project Supervisor: 	Sami Rollings, USF Professor 
Participants:		Ali Alnajjar USF MS Web Science Student
Contact:		najjaray@gmail.com
Requirement URL:	https://sites.google.com/site/usfcs685s10/a/project-3---routing
*/
package org.sunspotworld;
/**
*
* @author najjaray
*/
public class MessageListener implements Runnable
    {
    public String MsgPrefix = "ayalnajjar";
    public String MessageBody ="";
    public int SampleRate = 2;
    public static int CurVersion = 0;
    public int CurUpdate = 0;
    public int NumOfRecFloodedMsgs = 0;
    public int NumOfSenFloodedMsgs = 0;
    public int NumOfFloodedUpd = 0;
    public int NumOfRecTrickleMsg = 0;
    public int NumOfSenTrickleMsg=0;
    public int NumOfTrickleUpd = 0;
    public static int NumOfMetadataSent = 0;
    public int NumOfMetadataReci = 0;
    public static int Trickle_c = 0;
    public static String log = "";
    //public int Trickle_k = 1;
    //public int Trickle_t = 0;
    //public int Trickle_T = 5;
    //public int Trickle_tc = 0;
    public boolean NewUp = true;
    public RadiogramConnection MsgReader;
    private String OrgMsg = "";
    private RadiogramConnection MsgSender;
    private ITriColorLED [] leds = EDemoBoard.getInstance().getLEDs();


//    MessageListener(String MsgPrefix, Hashtable Pings)
//    {
//        this.MsgPrefix = MsgPrefix;
//        this.Pings = Pings;
//        this.SleepCounter = 5000;
//        NewCounter = true;
//    }
    public void run()
    {
        //Trickle_tc = Trickle_T;
        while(true)
        {
            System.out.println("Message Listener ----- Active");
            try
            {
                System.out.println("MsgReader. Open connection");
                RadioFactory.getRadioPolicyManager().setOutputPower(-29);
                MsgReader = (RadiogramConnection)Connector.open("radiogram://:10");
                Datagram ReceviedDG = MsgReader.newDatagram(MsgReader.getMaximumLength());
                System.out.println("MsgReader. waiting for messages");
                MsgReader.receive(ReceviedDG);
                MessageBody = ReceviedDG.readUTF();
                MsgReader.close();
                System.out.println("[--1--][Radio] Listener, Message Body = " + MessageBody);
                // check if the massage length is more then prefix+8
                if (MessageBody.length() >= MsgPrefix.length()+8)
                {
                    // saving the origainal message that we will resend in case of flood
                    OrgMsg = MessageBody;
                    if (MessageBody.substring(0, (MsgPrefix.length())).equals(MsgPrefix))
                    {


                        // **************************************
                        // handling retask message (newer code)
                        // **************************************
                        if (MessageBody.substring(0,(MsgPrefix.length()+8)).equals(MsgPrefix + ".retask."))
                        {
                            MessageBody = MessageBody.substring(MsgPrefix.length()+8);
                            String how = MessageBody.substring(0,MessageBody.indexOf("."));
                            // getting the retak method (flood or trickle) and save it to how
                            System.out.println("[Retask] how="+how);
                            // in case of flood increment Rec Flood counter
                            if (how.equals("flood"))
                            {
                              System.out.println("[Retask] [Flood] Msg Rec");
                              NumOfRecFloodedMsgs++;
                            }
                            // in case of trickle increment Rec trickle counter
                            else if (how.equals("trickle"))
                            {
                                System.out.println("[Retask] [Trickle] Msg Rec");
                                NumOfRecTrickleMsg++;
                            }
                            MessageBody = MessageBody.substring(MessageBody.indexOf(".")+1);
                            // getting message_id
                            String Msg_Id = MessageBody.substring(0,MessageBody.indexOf("."));
                            System.out.println("Is Msg_Id = "+Msg_Id + ">" + CurUpdate + "CurUpdate?");
                            log = log + Msg_Id + ".";
                            // if it is a new update
                            if (Integer.parseInt(Msg_Id)>CurUpdate)
                            {
                                // show it is a new flood update and increase flood update counter
                                if (how.equals("flood"))
                                {
                                    for (int i=0; i<6;i++)
                                    {
                                    leds[i].setOff();
                                    leds[i].setColor(LEDColor.WHITE);
                                    leds[i].setOn();
                                    }
                                  System.out.println("[Retask] [Flood] New Update");
                                  NumOfFloodedUpd++;
                                }
                                // show it is a new flood update and increase flood update counter
                                else if (how.equals("trickle"))
                                {
                                     for (int i=0; i<6;i++)
                                    {
                                    leds[i].setOff();
                                    leds[i].setColor(LEDColor.YELLOW);
                                    leds[i].setOn();
                                     }
                                     System.out.println("[Retask] [Trickle] New Update");
                                    NumOfTrickleUpd++;
                                }
                                CurUpdate = Integer.parseInt(Msg_Id);
                                MessageBody = MessageBody.substring(MessageBody.indexOf(".")+1);
                                String SWV = MessageBody.substring(0,MessageBody.indexOf("."));
                                // get the software verson
                                System.out.println("SWV= " +SWV);
                                CurVersion = Integer.parseInt(SWV);
                                MessageBody = MessageBody.substring(MessageBody.indexOf(".")+1);
                                String rate = MessageBody.substring(0,MessageBody.indexOf("."));
                                // get the new sampling rate
                                System.out.println("rate=" +rate);
                                SampleRate = Integer.parseInt(rate);
                                // change the update indicator to show different color in the spot
                                NewUp = ! NewUp;


                                // *************************
                                // handling newer flood code (updates)
                                // **************************
                                if (how.equals("flood"))
                                {
                                  // resending the flood update and increase flood Sent counter
                                  SendMessage(OrgMsg);
                                  NumOfSenFloodedMsgs++;
                                  System.out.println("[Retask] [Flood] Resending Flooded Msg = " + OrgMsg);
                                }

                                // **************************************
                                // handling newer trickle code (updates)
                                // **************************************
                                else
                                {
                                    // setting T = Tl
                                    org.sunspotworld.Trickle.Trickle_T = org.sunspotworld.Trickle.Trickle_Tl;
                                    // resetting c
                                    Trickle_c = 0;
                                    // Getting new t from the range (T/2, T)
                                    Random randomGenerator = new Random();
                                    org.sunspotworld.Trickle.Trickle_t = randomGenerator.nextInt(org.sunspotworld.Trickle.Trickle_T);
                                    while (org.sunspotworld.Trickle.Trickle_t>(org.sunspotworld.Trickle.Trickle_T/2))
                                    {
                                         org.sunspotworld.Trickle.Trickle_t = randomGenerator.nextInt(org.sunspotworld.Trickle.Trickle_T);
                                    }
                                }
                            }
                        }

                        
                    // **************************
                    // handling metadata messages
                    // **************************
                    else if (MessageBody.substring(0,(MsgPrefix.length()+8)).equals(MsgPrefix + ".metada."))
                    {
                        // increase metadata Rec counter
                        NumOfMetadataReci++;
                        MessageBody = MessageBody.substring(MsgPrefix.length()+8);
                        System.out.println(MessageBody);
                        int Tverson = Integer.parseInt(MessageBody.substring(0,MessageBody.indexOf(".")));
                        System.out.println(Tverson +","+ CurVersion);


                        // *************************
                        // handling newer metadata
                        // *************************
                        if (Tverson>CurVersion)
                        {
                           for (int i=0; i<6;i++)
                            {
                            leds[i].setOff();
                            leds[i].setColor(LEDColor.ORANGE);
                            leds[i].setOn();
                           }
                            org.sunspotworld.Trickle.Trickle_T = org.sunspotworld.Trickle.Trickle_Tl;
                            // resetting c
                            Trickle_c = 0;
                            // Getting new t from the range (T/2, T)
                            Random randomGenerator = new Random();
                            org.sunspotworld.Trickle.Trickle_t = randomGenerator.nextInt(org.sunspotworld.Trickle.Trickle_T);
                            while (org.sunspotworld.Trickle.Trickle_t>(org.sunspotworld.Trickle.Trickle_T/2))
                            {
                                 org.sunspotworld.Trickle.Trickle_t = randomGenerator.nextInt(org.sunspotworld.Trickle.Trickle_T);
                            }
                            // Send Metadata
                            //SendMessage(MsgPrefix + ".metada."+CurVersion+".");
                            NumOfMetadataSent++;
                        }
                            // ************************
                            // handling older metadata
                            // ************************
                            else if (Tverson<CurVersion)
                            {
                                // Send Update
                                SendMessage(MsgPrefix + ".retask.trickle." + CurUpdate + "." + CurVersion + "." + SampleRate + ".");
                                NumOfSenTrickleMsg++;
                                for (int i=0; i<6;i++)
                                {
                                leds[i].setOff();
                                leds[i].setColor(LEDColor.CYAN);
                                leds[i].setOn();
                                }
                            }
                                // *************************
                                // handling same metadata
                                // *************************
                                else
                                {
                                    for (int i=0; i<6;i++)
                                    {
                                    leds[i].setOff();
                                    leds[i].setColor(LEDColor.MAUVE);
                                    leds[i].setOn();
                                    }
                                    // incremant c
                                    Trickle_c++;
                                }
                        }
                    }
                }
            }
            catch (IOException ex)
            {
                System.out.println("MsgReader. Close connection --Interrupt");
            }
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
