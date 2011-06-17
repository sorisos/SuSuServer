package com;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.net.*;
import java.util.UUID;

public class HttpServer{
	static int PORT_NR = 3001;
	public static String realm = "localhost"; // same as DNS-name

	/**@htmlDir: path to directory containing html-files or similar doc types. 
	 * uploads ends up here as well. Note that relative paths might not work outside Eclipse((?) **/
	public static String htmlDir = "../html"; 
	private static boolean running = true; // set to false to end server 

	
	/**@uploadSessions :
	 *  this map is the temporary log for aborted or completed uploads. 
	 */
	static Map<UUID, UploadSessionTracker> uploadSessions = new HashMap<UUID, UploadSessionTracker>();
	
	public static void main(String[] args) throws IOException{ 

		/** parse arguments, if any **/
		  if (args.length >= 1) {
			  try {
				  PORT_NR = Integer.parseInt(args[0].trim());
			  } catch (NumberFormatException nfe){
					System.out.println("ivalid argument for port nr");
			  }
			  if (args.length >= 2) {
				  htmlDir = args[1].trim(); //the constructor in MethodHandler will tell if this path exists
			  }
		  }
	
		ServerSocket serverSocket = null;
		new HttpMethodHandler();
		
		try {
			serverSocket = new ServerSocket(PORT_NR); 
			System.out.println("Server running. port: "+PORT_NR); 
		} catch (IOException e) {
			System.err.println("Could not listen on port:"+PORT_NR);
			System.exit(1);
		}


		System.out.println("###### ---- ######");
		while(running) {
			Socket clientSocket = serverSocket.accept();
			new ClientHandler(clientSocket);

		}//END while

	}//END main()

}//END class
