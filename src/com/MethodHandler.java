package com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MethodHandler {
/** 
 * all allowed/possible requests are stored in the HashMap 'getMap' or postMap for fast response.
 * 
 * Note that the clients PrintStream is intitalized in this class but terminated(ie closed) in 
 * the ClientHandler class when the corresponding socket is closed.
 * (not sure this solution is thread safe)
 */
	/**@getMap: this map contains both documents stored on disc in the defined doc-directory 
	 * and CGI-scripts. scripts that are methods in this class.**/
	public static Map<String, Integer> getMap = new HashMap<String, Integer>();
	public static Map<String, Integer> postMap = new HashMap<String, Integer>();
	
	/** GET map codes **/
	final static int UNKNOWN_GET = 0;
	final static int DEFAULT_DOCUMENT = 1;
	final static int FILE_DOCUMENT = 2;
	/** GET method CGI-scripts **/
	final static int FILE_UPLOAD_PROGRESS = 3;

	/** POST map codes **/
	/** POST method CGI-scripts **/
	final static int UNKNOWN_POST = 0;
	final static int FILE_UPLOAD = 1;
	final static int FILE_COMMENTS = 2;
	
	//final static String HTTP_VER = "HTTP/1.0";
	
	/** Constructor **/
	public MethodHandler() {
		/** Init GET method map (getMap) **/
		getMap.put("/upload_progress.jrp", FILE_UPLOAD_PROGRESS);
		getMap.put(null, DEFAULT_DOCUMENT);

		/** create a index for all documents in htmlDir in getMap **/
		File docsDir = new File(HttpServer.htmlDir);
		System.out.println("Index of reachable documents in "+docsDir.getAbsolutePath()+ ":");

		String[] docs = docsDir.list();
		for (int i=0; i<docs.length; i++) {
			if (!docs[i].startsWith(".")) {
				System.out.println("    "+docs[i]);		
				getMap.put(docs[i], FILE_DOCUMENT);
			}
		}
		System.out.println("");

		/** Init POST method map (postMap) **/
		postMap.put("/file_upload.jrp", FILE_UPLOAD);
		postMap.put("/file_comments.jrp", FILE_COMMENTS);
	}

	/************** Methods handling GET *************/
	
	public static void doGet(ClientHandler client) {
		int responseCode = 0;
		String requestURI = client.request.getRequestURI();
		
		if (getMap.containsKey(requestURI)) {
			responseCode = getMap.get(requestURI);
		} else {
			responseCode = UNKNOWN_GET;
		}
		
		switch (responseCode) {

		case FILE_DOCUMENT:
			System.out.println("case DOCUMENT");
			respondRequestedDocument(client, HttpServer.htmlDir+client.request.getRequestURI());
			break;
			
		case DEFAULT_DOCUMENT:
			System.out.println("case DEFAULT_DOCUMENT");
			respondRequestedDocument(client, HttpServer.htmlDir+"/index.html");
			break;
		
		case UNKNOWN_GET:
			System.out.println("case UNKNOWN_GET");
			respondStatusCode(client, 404);
			break;
			
		case FILE_UPLOAD_PROGRESS:
			System.out.println("case UPLOAD_PROGRESS");
			respondUploadProgress(client);       
			break;
		
		default:  
			respondStatusCode(client, 404);
		break;
		}
	}

	public static void respondUploadProgress(ClientHandler client){
		UUID uuid = client.getSessionId();
		PrintStream outStream = client.getOutputStream();
		
		if (HttpServer.uploadSessionLog.containsKey(uuid)) {
			outStream.println("HTTP/1.1 200 OK"); 
			outStream.println("Server : SuUpload 0.1 Beta"); 
			outStream.println("Content-Type: text/xml");
			outStream.println("Cache-Control: no-cache, must-revalidate");
			outStream.println("Set-Cookie: sid="+uuid.toString()); //needed?
				outStream.println(); //- end of header
				UploadSessionTracker uploadLog = HttpServer.uploadSessionLog.get(uuid);
				outStream.println("<?xml version='1.0'?>");
				outStream.println("<DOCUMENT>");
				
				log("upload session found. replying upload progress percent" + uploadLog.getUploadProgress());
				
				outStream.println("<progress>"+uploadLog.getUploadProgress()+"</progress>");
				outStream.println("<active>"+uploadLog.active+"</active>");
				outStream.println("<complete>"+uploadLog.allFileBytesReceived+"</complete>");
				outStream.println("<fileurl>"+uploadLog.fullFilePath+"</fileurl>");
				outStream.println("</DOCUMENT>");
			
		}  else { // something is wrong
			System.err.println("Upload progress requested for a session that dosen't exists.");
			System.err.println("(Requested sessionId: "+ uuid +").");
			respondStatusCode(client, 404);
			
		}
	}

/** respond a document stored on disc. Note that the Content-Type: field is always set to 'text/html'
 * and it will therefor only respond such document succeful. 
 * TODO: set content type after file suffix, ie text/css, application/javascript ...
 */
	private static void respondRequestedDocument(ClientHandler client, String documentPath)  {
		System.out.println("[ClientHandler]: Processing request for document: "+documentPath);

		PrintStream outStream = client.getOutputStream();
		try { //-try find requsted document
			File doc = new File(documentPath);
			FileInputStream fis = new FileInputStream(doc); 
			byte[] b = new byte[1024];

			outStream.println("HTTP/1.0 200 OK"); 
			outStream.println("Server : SuUpload 0.1 Beta"); 
			outStream.println("Content-Type: text/html");
			String sid = client.request.getSessionIdFromCookie();
			if (sid == null) { // if no previous sid(session id) set a new
				sid = client.getSessionId().toString();
			}
			outStream.println("Set-Cookie: sid="+sid);
			outStream.println(); //- end of header
			
			while(fis.available() > 0){
				outStream.write(b,0,fis.read(b));
			}
			fis.close();
		} catch (FileNotFoundException e) { 
			respondStatusCode(client, 404);
		} catch (IOException e) {
		} 
		outStream.close();
	}

	public static void respondStatusCode(ClientHandler client, int statusCode) {
		PrintStream outStream = client.getOutputStream();		
		String errorMsg = null;
		switch (statusCode) {
		case 200: /** 200 OK **/
			outStream.println("HTTP/1.1 200 OK");
			break;
		case 304: /** 304 Not Modified **/
			outStream.println("HTTP/1.1 304 Not Modified");
			break;
		case 404: /** 404 Not Found **/
			outStream.println("HTTP/1.1 404 Not Found");
			errorMsg ="404 Not Found";
			break;
		case 500: /** 500 Internal Server Error **/
			outStream.println("HTTP/1.1 500 Internal Server Error");
			errorMsg ="500 Internal Server Error";
			break;
		case 501: /** 501 Not Implemented. Request not recognized or server can't handle it. **/
			outStream.println("HTTP/1.1 501 Not Implemented");
			errorMsg ="501 Not Implemented. Request not recognized or server can't handle it.";
			break;
		default:  /** Unimplemented/unknown status code **/
			System.err.println("The http status code "+statusCode+ "is not recognised (or not yet implemented)");
			break;
		}
		outStream.println("Server : SuUpload 0.1 Beta"); 
		outStream.println("Content-Type: text/html");
		outStream.println(); //- end of header
		if (errorMsg != null) {
			outStream.println("<html><head><title>"+errorMsg+"</title></head><body><h1>"+ errorMsg +"</h1></body></html>");
		}
	}
	

	/** 
	 * this method desire what method in ClientHandler that 
	 * should be called to handle the Post request
	 * **/
	public static int doPost(ClientHandler client) {
	
		int postMethodCode = 0;
		String requestURI = client.request.getRequestURI();
		
		if (postMap.containsKey(requestURI)) {
			postMethodCode = postMap.get(requestURI);
		} else {
			postMethodCode = UNKNOWN_POST;
		}
		return postMethodCode;
	}
	
	public static void log(String msg) {
		System.out.println("[MethodHandler]: "+ msg);
	}
	
}
