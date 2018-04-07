package com.kiahamedi.foxroomweb;

import java.net.URLDecoder;
import java.util.*;

import javax.websocket.Session;

import org.json.JSONObject;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import com.google.common.collect.Maps;


@ServerEndpoint("/chat")
public class SocketServer {
	
	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<Session>());
	private static final HashMap<String , String> nameSessionPair = new HashMap<>();
	private JSONUtils jsonutils = new JSONUtils();
	
	public static Map<String , String> getQueryMap(String query)
	{
		Map<String, String> map= Maps.newHashMap();
		if (query != null)
		{
			String[] params = query.split("&");
			for (String p : params)
			{
				String[] name=p.split("=");
				map.put(name[0], name[1]);
			}
			
		}
		return map;
	}
	
	public void sendMessageToAll(String sessionId,String name,String msg,boolean IsNewClint,boolean IsExit)
	{
		for(Session s: sessions)
		{
			String json=null;
			if(IsNewClint)
			{
				json=jsonutils.getNewClintJson(sessionId, name, msg, sessions.size());
			}
			else if (IsExit)
			{
				json=jsonutils.getClintExitJson(sessionId, name, msg, sessions.size());
			}else
			{
				json=jsonutils.getSendAllMessageJson(sessionId, name, msg);
			}
			try
			{
				System.out.println("Sending Message To: "+sessionId+", "+json);
				s.getBasicRemote().sendText(json);
				
			}catch(Exception ex)
			{
				System.out.println("Error In Sending Message: "+sessionId+", " + ex.toString());
				ex.printStackTrace();
			}
			
		}
	}
	
	@OnOpen
	public void onOpen(Session session)
	{
		System.out.println(session.getId()+" has opend a connection ");
		Map<String , String> queryParams = getQueryMap(session.getQueryString());
		String name="";
		if(queryParams.containsKey("name"))
		{
			name=queryParams.get("name");
			try
			{
				name=URLDecoder.decode(name,"UTF-8");
				
			}catch(Exception ex)
			{
				ex.printStackTrace();
			}
			nameSessionPair.put(session.getId(), name);

		}
		
		sessions.add(session);
		
		try
		{
			session.getBasicRemote().sendText(jsonutils.getClintDetailsJson(session.getId(), "Your Session Details"));
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		sendMessageToAll(session.getId(),name,"Joined Conversation!",true,false);
		
		
	}
	
	@OnMessage
	public void onMessage(String msg,Session session)
	{
		System.out.println("Message From" + session.getId()+ ":" + msg);
		String m = null;
		try
		{
			JSONObject jObj = new JSONObject(msg);
			m=jObj.getString("message");
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
		sendMessageToAll(session.getId(),nameSessionPair.get(session.getId()) , m, false, false);
		
		
		
	}
	
	@OnClose
	public void onClose(Session session)
	{
		System.out.println("Session" + session.getId()+ " Has Ended");
		String name=nameSessionPair.get(session.getId());
		sessions.remove(session);
		sendMessageToAll(session.getId(), name, "left Canversation!", false, true);
		
	}
	
	
	
}
