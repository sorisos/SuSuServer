package com;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;


public class HttpRequest {
	/** this class is a holder/container and parser for http request headers 
	 * follows the naming convention of HttpServletRequest but is far less complete 
	 * http://download.oracle.com/javaee/1.3/api/javax/servlet/http/HttpServletRequest.html 
	 * The parser adds all header fiels to a map and is further handles/parsed when the data is requested...
	 * **/

	/**TODO: 
	 *  - dont parse on every getter method call, only on first call....
	 *  - rgex pattern and match would probably be a lot faster
	 */

	
//	final static String CHARSET = "UTF-8"; //"ISO-8859-1";

	private String method = null;
	private String requestURI = null;
	private String queryString = null;
	public Map<String,String> headerFields;

	public HttpRequest () {
		headerFields = new HashMap<String,String>();		
	}
	
	public void parseLine(String headerLine) {		
		if (headerLine.startsWith("GET ") || headerLine.startsWith("POST ")) {
			parseFirstLineOfHeader(headerLine);
		} else {
			parseHeaderFieldLine(headerLine);
		}
	}
	
	public void parseHeaderFieldLine(String headerLine) {
	//	System.out.println(headerLine);
		int splitIndex = headerLine.indexOf(":");
		if (splitIndex != -1) {
			String key = headerLine.substring(0, splitIndex);
			String value = headerLine.substring(splitIndex+1, headerLine.length());
			//System.out.println("key: "+key+", value: "+value);
			headerFields.put(key, value);
		}
	}
	
	public void parseFirstLineOfHeader(String requestLine) {
	//	log("parseFirstLineOfHeader. requestLine: "+ requestLine);
		String requestMsg[] = requestLine.split(" "); //-no errorhandling here
		if (requestMsg[0].contains("GET")) { //startsWith("POST ")...
			method = "GET";
		} else if (requestMsg[0].contains("POST")) {
			method = "POST";
		}

		if (requestMsg[1].startsWith("/") && requestMsg[1].length()>2 ) {
			try { 		
				String tmp[] = requestMsg[1].split("\\?");
				try {
					requestURI = tmp[0];
				} catch (ArrayIndexOutOfBoundsException e){
					System.out.println("no uri");
				}

				try { //- try parse query string if any	
					queryString = tmp[1];
				} catch (ArrayIndexOutOfBoundsException e){
					System.out.println("no query");
				}
			} catch (PatternSyntaxException e){ //- no query string
				//e.printStackTrace();
				requestURI = requestMsg[1];
			} 
		}	
	}
	

	public static void log (String msg) {
		System.out.println(msg);
	}
	
	public void printHeaderToConsole() {
		//TODO...
		System.out.println("--------------- HEADER ----------------");
		System.out.println("------------- END HEADER ------------");
	}
	/********** GETTER AND SETTERS *************/
	public String getMethod() { return method; }
	public String getQueryString() { return queryString; }
	public String getRequestURI() { return requestURI; }

	public int getContentLength() { // throws...
		int contentLength;
		if (headerFields.containsKey("Content-Length")) {
			try {
				contentLength = Integer.parseInt(headerFields.get("Content-Length").trim());
			} catch (NumberFormatException nfe){
				contentLength = -1;
				log("Unable to parse integear from Content-Length information");
			}
		} else {
			log("There was (probably) no 'Content-Length'-field in header");
			contentLength = -1;
		}
		return contentLength; 
	}

	public String getContentType() {
		String contentType ="";
		if (headerFields.containsKey("Content-Type")) {
			contentType = headerFields.get("Content-Length");
		} else {
			contentType = null;
			log("No Content-Type field found in header (ill formed header?)");
		}
		return contentType; 
	}
	
	/** returns boundary from header field: Content-Type: multipart/form-data;... or null if none found **/
	public String getContentTypeBoundary() {
		
		/* Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryGRw0EdLDwC6Pjcbg */
		String boundary ="";
		if (headerFields.containsKey("Content-Type")) {
			String contentType = headerFields.get("Content-Type");
			int start = contentType.indexOf("boundary=");
			boundary = contentType.substring(start + "boundary=".length());
			log("contentType: "+contentType + " start: "+start);
			
			//TODO: regex -faster...
			//Pattern contentTypeBoundaryPattern = Pattern.compile("boundary=");
			//Matcher m = contentTypeBoundaryPattern.matcher(contentType);
			//contentType.substring(m.start() + "boundary=".length());
			//log("contentType: "+contentType + " start: "+m.start());
			
			
		} else {
			boundary = null;
			log("No Content-Type field and no boundary found in header");
		}
		return boundary; 
	}
	
	
	public String getCookies() {
		//TODO...
		String cookies = null;
		if (headerFields.containsKey("Cookie")) {
			cookies = headerFields.get("Cookie");
		} 
		return cookies;
	}

	public String getSessionIdFromCookie() {
		//TODO: regex, handle svereal cookie variables...
		String sid = null;
		String cstr = getCookies();
		if(cstr != null) {
			int startpos = cstr.indexOf("sid=");
			if (startpos > 0) {
				sid = cstr.substring(startpos+"sid=".length());
			}
		}
		return sid;
	}
}
