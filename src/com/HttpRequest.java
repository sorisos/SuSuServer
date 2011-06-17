package com;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;


public class HttpRequest {
	/** this class is a holder/container and parser for http requests. 
	 * follows the naming convention of HttpServletRequest to some extent but is far less complete 
	 * http://download.oracle.com/javaee/1.3/api/javax/servlet/http/HttpServletRequest.html 
	 * The parser adds all header fiels to a map and is further handles/parsed when the data is requested...
	 * 
	 * TODO: dont parse on every getter method call, only on first call....
	 * TODO: regex pattern and match would probably be a lot faster... or dont use JRE ;)
	 */

	/** http method for the current request. currently only GET or POST **/
	private String method = null;
	
	/** "/index.html" for example. or null if none given in the browser address field **/
	private String requestURI = null;
	
	/** the un-parsed querystring of the current request, if any **/
	private String queryString = null;
	
	/** all header fields are stored in this Map. the key is what comes before the ':' and  the value what comes after **/
	public Map<String,String> headerFields;

	
	
	public HttpRequest () {
		headerFields = new HashMap<String,String>();		
	}


	/** input: any header line except the first one (ie the one containing GET, POSt etc) **/
	public void parseHeaderFieldLine(String headerLine) {
		int splitIndex = headerLine.indexOf(":");
		if (splitIndex != -1) {
			String key = headerLine.substring(0, splitIndex);
			String value = headerLine.substring(splitIndex+1, headerLine.length());
			headerFields.put(key, value);
		}
	}
	
	
	/** extracts the relevant info from the first header line **/
	public void parseFirstLineOfHeader(String requestLine) {
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
				requestURI = requestMsg[1];
			} 
		}	
	}
	

	/********** GETTER AND SETTERS *************/
	public String getMethod() { return method; }

	public String getQueryString() { return queryString; }
	
	public String getRequestURI() { return requestURI; }

	/** Returns the value of the Content-Length field of from the header **/
	public int getContentLength() { 
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
	
	
	/** Returns the value of the Content-Type field of from the header **/
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
	
	
	/** Returns boundary from header field: Content-Type: multipart/form-data;... or null if none found **/
	public String getContentTypeBoundary() {
		String boundary ="";
		if (headerFields.containsKey("Content-Type")) {
			String contentType = headerFields.get("Content-Type");
			int start = contentType.indexOf("boundary=");
			boundary = contentType.substring(start + "boundary=".length());
			log("contentType: "+contentType + " start: "+start);
		} else {
			boundary = null;
			log("No Content-Type field and no boundary found in header");
		}
		return boundary; 
	}
	
	
	/** Returns the cookie string **/
	public String getCookies() {
		String cookies = null;
		if (headerFields.containsKey("Cookie")) {
			cookies = headerFields.get("Cookie");
		} 
		return cookies;
	}
	
	
	/** Returns the session id from the cookies. note this is not a 'universal' method for http **/
	public String getSessionIdFromCookie() {
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
	
	
	public static void log (String msg) {
		System.out.println(msg);
	}
}
