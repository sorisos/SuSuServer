package com;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.UUID;


public class ClientHandler implements Runnable {
	Thread thread = new Thread(this);
	private Socket socket;
	
	/**@charset: The charset used to convert bytes to string **/
	private String charset = "UTF-8"; //"ISO-8859-1";
	
	/**@outStream: A PrintStream that connects to the socket of this thread. **/
	private PrintStream outStream = null;
	
	/**@sessionId: The unique session id. either set form cookies or a new i created **/
	private UUID sessionId = null;
	
	/**@ust: A instance of UploadSessionTracker. initialized when a upload is started.**/
	private UploadSessionTracker ust = null;
	
	/**@request: The HttpRequest corresponding to this thread **/
	public HttpRequest request;

	/**@ClientHandler: constructor **/
	public ClientHandler (Socket sessionSocket){
		this.socket = sessionSocket;
		System.out.println("\n[ClientHandler]: New instanse created. IP: "+socket.getInetAddress());
		thread.start();
	}
	
	@Override
	public void run() {
		try {
			request = new HttpRequest();
			InputStream inputStream = socket.getInputStream();
			
			
			/** read first line of header. handle possible leading null-lines (unsure if this is needed) **/
			log("-------HEADER-------");
			String headerLine;
			int numbOfHeaderLines = 0;

			headerLine = readLineFromStream(inputStream);
			if (headerLine != null) {
				request.parseFirstLineOfHeader(headerLine);
				log(headerLine);
			} else {
				numbOfHeaderLines++;
				System.err.println("Failed to retrive header");
				MethodHandler.respondStatusCode(this, 500);
			}

			/** read rest of header **/
			while(inputStream.available() >0) {
				headerLine = readLineFromStream(inputStream);
				if (headerLine == null) {
					break;
				} else {
					//request.parseLine(headerLine);
					request.parseHeaderFieldLine(headerLine);
					log(headerLine);
					numbOfHeaderLines++;
				}
			}
			
			log("-------END HEADER-------");
			
			/** we got the header. set the sessionId form cookies or create a new in none exists.**/
			setSessionId(request);
			
			/** if no header retrieved. (it happens ;( ). Respond 500 Internal Server Error **/  
			if (numbOfHeaderLines<1) {
				MethodHandler.respondStatusCode(this, 500);
			}
		
			if (request.getMethod() == "GET") {
				MethodHandler.doGet(this);
			} else if (request.getMethod() == "POST"){
				int postMethodCode = MethodHandler.doPost(this);
				switch (postMethodCode) {
				case MethodHandler.FILE_UPLOAD:
					handleFileUpload(request, inputStream);
					break;
				case MethodHandler.FILE_COMMENTS:
					handleFileCommentPost(request, inputStream);
					break;
				default:
					break;
				}
			}
			inputStream.close();
			outStream.close(); 

		} catch (IOException e) {
			System.err.println("RequestHandler IOException occured: ");
			e.printStackTrace();
		} finally {
			try {				
				socket.close();
				log("Client socket is closed");
				//socket.shutdownInput();
			} catch (IOException e) { 
				log("Failed to close Client socket");
			}
		}
		log("End of thread end of life");
	}//END run
	private void endThread() {
		try {
			log("Closing client socket");
			socket.close();
			//socket.shutdownInput();
		} catch (IOException e) { e.printStackTrace(); }
		
	}
	
	/** Set the session id for this thread/class instance. 
	 * if none found in the cookies, then create new a new from the UUID class.
	 * Must be called first or early in the constructor as many methods rely 
	 * on a non-null sessionId.
	 * **/
	private void setSessionId (HttpRequest request){
		String sid = request.getSessionIdFromCookie();
		if (sid != null) { // doesnt handle strange cookies ie no catch for IllegalArgumentException 
			sessionId = UUID.fromString(sid);
		} else { // on first request or deleted cookie. (expired?) 
			sessionId = UUID.randomUUID();
		}
	}
	
	/** returns the sessionID **/
	public UUID getSessionId() {
		return this.sessionId;
	}
	
	/** creates and returns the client thread PrintStream or null if failed to create **/
	public PrintStream getOutputStream()  {
		if (this.outStream == null) {
			try {
				outStream = new PrintStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return outStream;
	}

	private void handleFileUpload(HttpRequest request, InputStream inputStream) throws IOException {
		log("Processing POST request ...");
		MethodHandler.respondStatusCode(this, 200);
		String contentDisposition = null;
		String postBodyLine;
		/** read the first lines of the post body and retrieve the Content-Disposition line **/
		while( (postBodyLine = readLineFromStream(inputStream)) != null && postBodyLine.length() > 0){
			log("bodyLine: "+postBodyLine);
			if ( postBodyLine.startsWith("Content-Disposition")) {
				contentDisposition = postBodyLine.substring(postBodyLine.indexOf(":"));
			}
		}

		int contentLength = request.getContentLength();
		String boundary = request.getContentTypeBoundary(); 
		//if (contentLength > -1 && boundary != null) {
		if (contentLength < 1) {
			System.err.println("Content-Length  <1 in post file request(sessionId: "+ sessionId +")");
			return;
		} else if (boundary == null) {
			System.err.println("No boundary found in post file request. (sessionId: "+ sessionId +")");
			return;
		} else {
			byte[] preBoundarBytes = {13,10, 45, 45}; //these are in the stream before the boundaryBytes
			byte[] boundaryBytes = boundary.getBytes(charset);
			byte[] endBoundaryBytes = new byte[preBoundarBytes.length + boundaryBytes.length];
			
			System.arraycopy(preBoundarBytes,0,endBoundaryBytes,0,preBoundarBytes.length);
			System.arraycopy(boundaryBytes, 0, endBoundaryBytes, preBoundarBytes.length, boundaryBytes.length);
		
			handleClientUploadStream(inputStream, contentLength, endBoundaryBytes, contentDisposition);
		} 
	}

	
	public void handleClientUploadStream(InputStream inputStream, int contentLength, byte[] boundaryBytes, String contentDisposition) {
		/** don't want to allocate memory for a huge buffer for the file in the post-body, 
		 * which make the while loops below somewhat unnecessary complicated and
		 * might even be counterproductive.
		 * **/

		ust = new UploadSessionTracker();
		ust.active = true;
		ust.sid = sessionId.toString();
		ust.timeStarted = System.currentTimeMillis() / 1000L;;
		ust.postContentLength = contentLength;
		HttpServer.uploadSessionLog.put(sessionId, ust);
		
		RegexUtil ru = new RegexUtil("filename=\".{3,}\"", "filename=\"".length(), 1);
		ust.fileName = ru.getSubString(ru, contentDisposition);
		
		log("Reading POST body input stream (contentLength: " + contentLength +")...");
		//postContentLength = contentLength;
		int postByteCounter = 0;


		try {
			ust.fullFilePath = "/"+ sessionId + ust.fileName;
		
			FileOutputStream fileOutput = new FileOutputStream(HttpServer.htmlDir + ust.fullFilePath);
	        	
			byte[] b = new byte[1];
			int compareIter = 0;
			byte[] compareBuffer = new byte[boundaryBytes.length];

			
			while (inputStream.available() >0 && postByteCounter<contentLength) {
				/** looking for boundary line in the byte stream:
				 * as the last byte(s) in the file might be the same as the first byte(s) in
				 * the boundary-line we need to: 
				 * 1. shift the compareBuffer 1 step left
				 * 2. add every new byte from inputstream last in compareBuffer
				 * 3. compare if the bytes in the compareBuffer matches the bytes from the boundary-line
				 * 4. if no match. write the first (ie. oldest) byte from compareBuffer to to the file
				 *    else break the loop.
				 *... there might be a better way doing this: doubble buffers or something
				 */
				inputStream.read(b);
				
				for (int i=0; i<compareBuffer.length-1; i++){
					/** 'shift' array left. sub-optimal solution... 
					 * equals to 'shift(l,1)' in python<3
					 * **/
					compareBuffer[i] = compareBuffer[i+1] ; 
				}
				compareBuffer[compareBuffer.length-1] = b[0];// add new byte last
				
				while(compareIter < boundaryBytes.length) { // compare
					//log("comparsion ("+compareIter+"): "+ compareBuffer[compareIter] +" "+ boundaryBytes[compareIter]);
					if(compareBuffer[compareIter] != boundaryBytes[compareIter]) {
						/** false alarm. this was no boundary line  **/
						compareIter=0;
						break;
					}
					compareIter++;
				}
				if(compareIter > boundaryBytes.length-3) { //possible bug. matches CR+LF-chars at end of boundary line?
					log("end of filebytes");
					ust.allFileBytesReceived = true;
					break;
				} else {
					compareIter=0;
					if (postByteCounter>compareBuffer.length-2) { //yes length-2 !
						/** don't write until buffer is full **/
						fileOutput.write(compareBuffer[0]); //write oldest
					} 
				}

				postByteCounter++;
				ust.bytesReceived = postByteCounter; //update so we can calculate progress
				
			}//END while
			
			log(postByteCounter+" of "+contentLength + " read. end of file");

			while (inputStream.available() >0 && postByteCounter<contentLength) {
				/** 
				 * read the remaining data from the input stream. 
				 * we don't need the data as it's just the last boundary-line, 
				 * but the client/browser want us to have it.
				 * **/
				inputStream.read(b);
				postByteCounter++;
				ust.bytesReceived = postByteCounter;
			}
			log(postByteCounter+" of "+contentLength+ " bytes read from post body. (last byte: " +b[0] +")");

				fileOutput.flush();
				fileOutput.close();

				if (ust.allFileBytesReceived) {
					MethodHandler.getMap.put(ust.fullFilePath, MethodHandler.FILE_DOCUMENT); //add to index
				}
				
						                                                         
					
		} catch (Exception e) {
			System.err.println("Failed to complete upload from client. (sessionId: "+ sessionId +")");
			e.printStackTrace();
		}
		
		ust.active = false; // done with the upload (aborted or complete) not active...
		ust.timeEnded = System.currentTimeMillis() / 1000L;;
	}



	public void log (String msg) {
		System.out.println("[ClientHandler, sid: "+sessionId+"]: "+msg);
		return;
	}

	private void handleFileCommentPost(HttpRequest request, InputStream inputStream) throws IOException {

		String boundaryLine = request.getContentTypeBoundary();

		while(inputStream.available()>0) {	
			/** read the leading post body ending with empty line(=null). 
			 * as we only expects the comments text. we don't need this data. 
			 * **/
			if (readLineFromStream(inputStream) != null) { break; }
		}
		String postBodyLine;
		StringBuilder comments = new StringBuilder();
		while(inputStream.available()>0) { 
			/** read the comments **/
			postBodyLine = readLineFromStream(inputStream);
			if(postBodyLine != boundaryLine) {
				comments.append(postBodyLine);
			}
		}
		if (this.ust != null) {
			ust.fileName = comments.toString();
		} else {
			System.err.println("Failed to add comments to uploaded file as there seems to be no previous uploaded file. (sessionId: "+ sessionId +")");
		}
	}
	
	/**
	 * One might argue why not use InputStreamReader.readLine()? Well it turns out 
	 * this won't work as the first bytes (about 7656 b) in the post body byte-stream 
	 * gets lost using this method. The solution is to use InputStream only.
	 * The JSP HttpServletRequest documentations states that it's unwise/forbidden to use both 
	 * InputStream and InputStreamReader and the lost bytes might relate to this, even if the 
	 * JSP-docs might not be applicable to the JRE.
	 * 
	 */
	private String readLineFromStream(InputStream inputStream) {
		/** http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4175635 **/
		StringBuilder stringBuilder = new StringBuilder();
		byte[] b = new byte[1]; // read buffer
		
		try {
			while(inputStream.available() >0) {
				inputStream.read(b);	
				if (b[0] == 13) { /** if 13: Carriage Return. CR (hex:U000D) **/
					inputStream.read(b);
					if (b[0] == 10) { /** if 10: Line Feed. LF (hex:U000A) **/
						break;
					}
				} else {
					stringBuilder.append(new String(b, charset));
				}
			}  
		} catch (IOException e) {
			e.printStackTrace();
		}
		String returnString = stringBuilder.toString();
		if (returnString.length() <1) {
			return null;
		} else {
			return returnString;
		}
	}
	
}//END class


