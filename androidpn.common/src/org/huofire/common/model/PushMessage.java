package org.huofire.common.model;

import java.util.Date;

/**
 * 推送消息格式
 * @author winters_huang
 *
 */
public class PushMessage {
	private String id = "";
	private String apiKey = "";
	private String title = "";
	private String message = "";
	private String uri = "";
	private Date date;
	
	public void setId(String id){
		this.id = id;
	}
	
	public String getId(){
		return this.id;
	}
	
	public void setApiKey(String apiKey){
		this.apiKey = apiKey;
	}
	
	public String getApiKey(){
		return this.apiKey;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public void setMessage(String message){
		this.message = message;
	}
	
	public String getMessage(){
		return this.message;
	}
	
	public void setUri(String uri){
		this.uri = uri;
	}
	
	public String getUri(){
		return  this.uri;
	}
	
	public void setDate(Date date){
		this.date = date;
	}
	
	public Date getDate(){
		return this.date;
	}
}
