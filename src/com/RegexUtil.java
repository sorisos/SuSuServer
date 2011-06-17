package com;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**Simple regex-parser class to retrieve relevant info from strings. 
 * supposed to be implemented in the header parser class HttpRequest, 
 * currently only used from ClientHandler
 */

public class RegexUtil {
	
	/** the pattern to find the string **/
	public Pattern pattern;
	
	/** any initial offset. for example to only get the number from the string "someValue=32", 
	 * the offset should be ("someValue=").length()
	 */
	public int startOffset;
	
	/** same as endOffset, but to skip the last chars **/
	public int endOffset;
	
	/** constructor **/
	public RegexUtil(String regexp, int startOffset, int endOffset) {
		pattern = Pattern.compile(regexp);
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}
	/** returns the first found match or null if none found **/
	public static String getSubString(RegexUtil ru, String str) {
		String returnString = null;
		Matcher matcher = ru.pattern.matcher(str);
		if (matcher.find()) {
			returnString = str.substring(matcher.start()+ru.startOffset, matcher.end()-ru.endOffset);
		}
		return returnString;
	}	
}