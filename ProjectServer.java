package myProject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
public class ProjectServer
{
	public static void main(String[] args) throws Exception
	{
		final int SERVER_PORT_NUM = 9959;	//Server port number: should be the same port as the client is sending packets to.
		byte[] receivedMessage = new byte[512];		//Receive data buffer
		DatagramPacket receivePacket = new DatagramPacket(receivedMessage,receivedMessage.length);	//Receive Datagram Packet
		DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT_NUM);	//Server socket, listening on server port number
		String tempSplit[] = new String[2];	//Creating a string array for string splitting purpose
		String sentMessage;	//Creating a string for UDP response packet
		while(true)	//Running the server forever
		{
			System.out.print("\nReceiving the request from the client...");
			serverSocket.receive(receivePacket);	//Receiving client request
			InetAddress clientAddress = receivePacket.getAddress();		//Getting client IP address from received UDP packet
			int clientPort = receivePacket.getPort();	//Getting client port number from received UDP packet
			String receivedMsg = new String(receivePacket.getData());	//Getting the received bytes in the form of string
			System.out.println("Received packet is:\n"+receivedMsg);	//Displaying the received packet			
			//Splitting the received UDP packet to get request ID
			tempSplit=receivedMsg.split("<id>");	
			tempSplit = tempSplit[1].split("</id>");
			String requestID = tempSplit[0].trim();	
			tempSplit = receivedMsg.split("</request>");
			//Verifying the integrity check value by calculation and comparison
			if(integrityCheck(tempSplit[0]+"</request>").equals(tempSplit[1].trim()))
			{	
				//Validating the syntax of received message and element values
				//Validating the opening and closing tags, element order ignoring the white spaces 
				if((receivedMsg.replaceAll("\\s","")).matches(".*<request>.*<id>.*</id>.*<measurement>.*</measurement>.*</request>.*"))
				{					
					tempSplit = receivedMsg.split("<measurement>");		//Splitting the string to get measurement ID
					tempSplit = tempSplit[1].split("</measurement>");
					int measurementValue = Integer.parseInt(tempSplit[0].trim());		//Converting the measurement ID to integer
					int measurementIndex = findMeasurementID(measurementValue);	//Finding the measurement ID in the data file
					ArrayList<Double> temperatureFile = getTemperature();		//Getting the temperature value corresponding to measurement ID from the data file
					if(measurementIndex==(-1))	//Error Code 3: Measurement ID not found in data file
			        	sentMessage=("<response>\n\t<id>"+requestID+"</id>\n\t<code>3</code>\n</response>");
			        else	//Code 0: Sending temperature value corresponding to measurement value from data file
			        	sentMessage=("<response>\n\t<id>"+requestID+"</id>\n\t<code>0</code>\n\t<measurement>"+measurementValue+"</measurement>\n\t<value>"+temperatureFile.get(measurementIndex)+"</value>\n</response>");
				}//if
				else
					sentMessage=("<response>\n\t<id>"+requestID+"</id>\n\t<code>2</code>\n</response>");
			}//if
			else	//Error Code 1: Integrity check failure
				sentMessage=("<response>\n\t<id>"+requestID+"</id>\n\t<code>1</code>\n</response>");
			sentMessage+=integrityCheck(sentMessage);		//Appending the checksum value to the resultant UDP response packet
			System.out.println("Message to send:\n"+sentMessage);	//Displaying the UDP response
			byte[] sentMessageByte = sentMessage.getBytes();		//Converting the UDP response string to byte array
			//Constructing the next UDP response packet to be sent
			DatagramPacket sentPacket = new DatagramPacket(sentMessageByte, sentMessageByte.length,clientAddress,clientPort);	
			System.out.print("\nSending the response to the client...");
			serverSocket.send(sentPacket);		//Sending the response to the client	
		}//while
	}//main
	
	public static String integrityCheck(String checkStr)
	{
		//Declaring an integer array to store paired bytes from byte array
		//Initializing this array to half the size of the byte array
		int[] checkSumArr = new int[checkStr.getBytes().length/2];
		if(checkStr.getBytes().length%2!=0)		//Checking for odd number of elements in the byte array
			checkSumArr = new int[(checkStr.getBytes().length/2)+1];	//Increasing the checksum array size by 1 for odd number of elements 
		int i=0,j=0,S=0,index,C=7919,D=65536;
		
		//Converting the character sequence to a sequence of 16-bit words by pairing consecutive bytes		
		while(i<checkStr.getBytes().length)
		{
			if(checkStr.getBytes().length%2!=0 && i==checkStr.getBytes().length-1)	//Checking for last element in case of odd number elements byte array
				checkSumArr[j++]=(int) (checkStr.getBytes()[i++] << 8 | 0);	//Appending zeros as LSB to last element in odd-length byte array  to form 16-bit word
			else
				checkSumArr[j++]=(int) (checkStr.getBytes()[i++] << 8 | checkStr.getBytes()[i++]);	//Left shifting first byte as MSB and appending the second byte as LSB to form 16-bit word					
		}//while		
		//Performing integrity checksum calculations
		for(int k=0;k<checkSumArr.length;k++)
		{
			index = S^checkSumArr[k];
			S=(C*index)%D;			
		}//for
		return Integer.toString(S&0xFFFF);	//Converting and returning resultant checksum value to 16-bit unsigned by logical AND with 0xFFFF
	}//integrityCheck
	
	public static int findMeasurementID(int measurementValue)
	{	//Returns the index of measurement ID from the data file ('-1' in case not found)
		ArrayList<Integer> measurementID = new ArrayList<Integer>();	//Creating ArrayList to store measurement IDs from data file
		BufferedReader br;	//To read the contents of data file
        try 
        {	//Reading the contents of data file
            br = new BufferedReader(new FileReader("C:\\Users\\DELL\\Google Drive\\eclipse projects\\Project_640\\data.txt"));
            try
            {	//Extracting and storing the measurement IDs from the data file to ArrayList
            	String tempStr;
            	while((tempStr = br.readLine()) != null ) 	//Reads the next line
                {
            		Scanner scanner = new Scanner(tempStr);
            		while (scanner.hasNextInt())	//Checks for next available integer	(as only measurement IDs are integer, temperature are double)
            			measurementID.add(scanner.nextInt());	//Appending the measurement ID to the ArrayList            	
                }
            } 
            catch (IOException e) 
            {
            	System.out.println(e);
            }
        } 
        catch (FileNotFoundException e) 
        {
        	System.out.println(e);
        }
        return measurementID.indexOf(measurementValue);	//Return the index of measurement ID from the data file ('-1' in case not found)
	}//findMeasurementID
	
	public static ArrayList<Double> getTemperature()
	{	//Returns the ArrayList of temperature values from data file
		ArrayList<Double> temperatureValue = new ArrayList<Double>();	//Creating ArrayList to store temperature values from data file
		BufferedReader br;	//To read the contents of data file
        try 
        {	//Reading the contents of data file
            br = new BufferedReader(new FileReader("C:\\Users\\DELL\\Google Drive\\eclipse projects\\Project_640\\data.txt"));
            try
            {	//Extracting and storing the measurement IDs from the data file to ArrayList
            	String tempStr;
            	int i=0;
            	while((tempStr = br.readLine()) != null ) //Reads the next line
                {
            		Scanner scanner = new Scanner(tempStr);
            		while (scanner.hasNextDouble())
            		{
            			if(i%2==0)	//Skip the double values at even position (these will be measurement IDs)
            				scanner.nextDouble();
            			else            				//Checks for next double at odd position (as measurement IDs are at even position, temperature at odd positions)
            				temperatureValue.add(scanner.nextDouble());		//Appending the temperature to the ArrayList
            			i++;
            		}//while
                }//while
            }//try 
            catch (IOException e) 
            {
            	System.out.println(e);
            }//catch
        }//try 
        catch (FileNotFoundException e) 
        {
        	System.out.println(e);
        }//catch
        return temperatureValue;	//Return the ArrayList of temperature values from data file
	}
}