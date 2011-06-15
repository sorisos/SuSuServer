package com;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegexUtil {
	public Pattern pattern;
	public int startOffset;
	public int endOffset;
	
	public RegexUtil(String regexp, int startOffset, int endOffset) {
		pattern = Pattern.compile(regexp);
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}
	
	public static String getSubString(RegexUtil ru, String str) {
		String returnString = null;
		Matcher matcher = ru.pattern.matcher(str);
		if (matcher.find()) {
			returnString = str.substring(matcher.start()+ru.startOffset, matcher.end()-ru.endOffset);
		}
		return returnString;
	}	
}