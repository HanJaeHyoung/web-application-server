package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import model.User;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.

			requestUrlHandler(new DataOutputStream(out), in);

        } catch (IOException e) {
            log.error(e.getMessage());           
        }
    }

	private void requestUrlHandler(DataOutputStream dos, InputStream in) throws IOException {

    	String httpStatus = "200";
    	byte[] body = responseBodyMake(dos, in);
		responseXXXHeader(dos, body.length, httpStatus);
		responseBody(dos, body);

	}

	private byte[] responseBodyMake(DataOutputStream dos, InputStream in) throws IOException {

		String getReq = getRequestUrl(in);
		byte[] body = null;

		if("/".equals(getReq.trim())){
			body = "Hello World".getBytes();
		}

		if(!"/".equals(getReq.trim())){
			try{
				body = Files.readAllBytes(new File("./webapp"+getReq).toPath());
			}catch(NoSuchFileException e){
				String httpStatus = "302";
				responseXXXHeader(dos, 0, httpStatus);
				//response = true;
			}
		}

		return body;
	}

	@SuppressWarnings("deprecation")
	private User setUser(String url){
    	String id ="";    	
    	String pw ="";
    	String nm ="";
    	String email ="";
    	
    	String decUrl = URLDecoder.decode(url);
    	int idx = decUrl.indexOf("?");
    	String[] userArr = decUrl.substring(idx+1, decUrl.length()).split("&");
    	
    	id = userArr[0].replace("userId=", "");
    	pw = userArr[1].replace("password=", "");
    	nm = userArr[2].replace("name=", "");
    	email = userArr[3].replace("email=", "");
    	
    	User user = new User(id, pw, nm, email); 	
    	
    	return user;
    }
    
    private String getQueryString(BufferedReader br) throws IOException{
    	
    	String str = br.readLine();    	
    	String conLen = "";
    	String queryStr = "";
    	
    	while( str != null && !"".equals(str)){   	
    		
    		if(str.contains("Content-Length")){        			
    			conLen = str.replace("Content-Length:", "").trim();    			
    		}
    		str = br.readLine();        		
    	}  
    	
    	if(queryStr.isEmpty()){
    		queryStr = IOUtils.readData(br, Integer.valueOf(conLen));
    	}
    	
    	return queryStr;
    }
    
    private String getRequestUrl(BufferedReader br) throws IOException{
       	String str = br.readLine();    	
    	String reqUrl = "";
    	
    	while( str != null && !"".equals(str)){   		
    		
    		if(str.contains("GET ") || str.contains("POST ")){   			
    			reqUrl = str;
    			break;
    		}
    		
    		str = br.readLine();        		
    	}  
    	    	
    	return reqUrl;
    }
    
    private void setQueryStr(String reqUrl, String qs){
    	if(reqUrl.contains("user/create")){
    		DataBase.addUser(setUser(qs));
    	}else if(reqUrl.contains("user/login")){
    		//isValidUser()
		}
    }
    
    private String getRequestUrl(InputStream in) throws IOException{
    	BufferedReader br = new BufferedReader(new InputStreamReader(in));
    		
    	String getReq = "";    	
    	String queryStr = "";
    	
    	getReq = getRequestUrl(br);
    	
    	if(getReq.startsWith("GET ")){
    		queryStr = getReq.substring(getReq.indexOf("?")+1);
    	}    	
    	if(getReq.startsWith("POST ")){
    		queryStr = getQueryString(br);
    	}
    	
    	setQueryStr(getReq, queryStr);
    	
    	String[] get = getReq.split(" ");
    	
    	if(get != null && get.length > 0){
    		return get[1];
    	}
    	
    	return "/";
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

	private void response302Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 302 Found \r\n");
			dos.writeBytes("Location: /index.html \r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

    private void responseXXXHeader(DataOutputStream dos, int lengthOfBodyContent, String httpStatus) {

    	if("200".equals(httpStatus)){
    		response200Header( dos,  lengthOfBodyContent);
    	}else if("302".equals(httpStatus)){
			response302Header( dos,  lengthOfBodyContent);
    	}
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
