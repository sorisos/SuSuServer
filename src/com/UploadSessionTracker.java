package com;

public class UploadSessionTracker {
	/** this class keeps track of upload sessions and the related comments. 
	 */
	    /**@active: set to true when a client upload is in progress **/
	    public boolean active = false;
	    
	    /**@allBytesReceived: set to true if all file bytes was received **/
	    public boolean allFileBytesReceived = false; 
	    
	    /**@timeStarted: time when upload started **/
	    public long timeStarted = 0;
	    
	    /**@timeEnded: time when upload ended **/
	    public long timeEnded = 0; 
	    
	    /**@fullFilePath: URL where the uploaded file is stored on the server **/
	    public String fullFilePath = null;
	    
	    /**@fileName: the origanal filename of the uploaded file **/
	    public String fileName = null;
	    
	    /**@sid: the sessionId the client had when uploading **/
	    public String sid = null;
	    
	    /**@comment: the users comment about the uploaded file **/ 
	    public String comment = null;
	    
	    /**@bytesReceived: updated under active upload. only used to calculate upload prgress **/
	    public int bytesReceived = 0; 
	    
	    /**@postContentLength: number of bytes of the post body (note != file size). only used to calculate upload prgress **/
	    public int postContentLength = 0;
	    
	    /** calculate and return upload progress in percent. method might not be thread safe **/
		public int getUploadProgress()  {
			if(allFileBytesReceived) { //as all file bytes  != content length and file-bytes more relevant
				return 100;
			} else {
				return (int) Math.round( (long)100*bytesReceived/postContentLength );
			}
		}
}
