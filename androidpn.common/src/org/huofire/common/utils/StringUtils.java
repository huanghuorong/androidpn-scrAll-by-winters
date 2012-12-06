package org.huofire.common.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

public class StringUtils {
	public static boolean hasText(String text){
		return text!=null && text.length()>0;
	}
	public static String getStringBySize(long fileSize){
		String result="";
		if((fileSize/1024)>=1){
			fileSize/=1024;
			if(fileSize/1024>=1){
				result=new BigDecimal(fileSize*1.0/1024).setScale(2,BigDecimal.ROUND_HALF_UP).floatValue()+" MB";
			}
			else{
				result=fileSize+" KB";
			}
		}
		else{
			result=fileSize+" B";
		}
		return result;
	}
	
	public static String trim(String text){
		if(text==null){
			return null;
		}
		return text.trim();
	}
	/**
	 * desc 截取指定长度（按字节Byte）的字符串
	 * @param content
	 * @param length
	 * @return
	 * @author winters_huang
	 */
	public static String interception(String content, int length){
		if(!StringUtils.hasText(content))return "";
		//将字符串转换成字符串数组存储
		String[] contentArray = new String[content.length()];
		for (int i=0;i<content.length();i++){
			contentArray[i] = content.substring(i, i+1);
		}
		String matchCN = "[\u4e00-\u9fa5]";//汉字的正则表达式
		int countCousor = 0;//当前游标所定位到的字节总数
		int resultIndex = content.length();
		for (int i=0; i< contentArray.length; i++) {
			if (contentArray[i].matches(matchCN)){
				countCousor+=2; 
			}else{
				countCousor+=1;
			}
			if(countCousor>=length){
				resultIndex = i;
				break;
			}
		}
		String resultContent = content.substring(0, resultIndex);
		return resultContent;
		
	}
	
	/**
	 * 获取字节长度
	 * @param content
	 * @return
	 */
	public static int getByteLength(String content){
		if(!StringUtils.hasText(content))return -1;
		int byteLength = -1;
		try {
			byteLength = content.getBytes("GBK").length;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return byteLength;
	}
	
	public static boolean isNull(String str){
		if(!StringUtils.hasText(str)){
			return true;
		}
		if(str.equalsIgnoreCase("null")){
			return true;
		}
		return false;
	}
}
