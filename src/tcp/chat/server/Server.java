package tcp.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Server {

	private ServerSocket serverSocket;
	private Socket socket;
	
	private Map<String, DataOutputStream> wClients = new HashMap<>();
	private Map<String, Map<String, DataOutputStream>> rClients = new HashMap<>();
	
	public void setting() throws IOException {
		Collections.synchronizedMap(wClients);
		Collections.synchronizedMap(rClients);
		
		serverSocket = new ServerSocket(9999);
		while(true) {
			System.out.println("채팅 서버 대기중");
			socket = serverSocket.accept();
			
			Receiver receiver = new Receiver(socket);
			receiver.start();
		}
	}
	
	public void addWClients(String nickName, DataOutputStream out) {
		wClients.put(nickName, out);
		System.out.println(wClients.toString() + " 인원수: "+ wClients.size());
	}
	
	public void removeWClients(String nickName) {
		wClients.remove(nickName);
		
		System.out.println("대기실? 인원수: "+wClients.size());
	}
	
	public void addRClients(String roomNo, String nickName, DataOutputStream out) {
		if(rClients.containsKey(roomNo)) {
			rClients.get(roomNo).put(nickName, out);
			System.out.println(roomNo+"번 방 " + rClients.toString() + wClients.toString() + " 인원수: "+ wClients.size());
		} else {
			Map<String, DataOutputStream> roomClient = new HashMap<>();
			roomClient.put(nickName, out);
			rClients.put(roomNo, roomClient);
			System.out.println(rClients.toString() + roomClient.toString() + " 인원수: "+ roomClient.size());
		}
	}
	
	public void removeRClients(String roomNo, String nickName) {
		rClients.get(roomNo).remove(nickName);
		System.out.println("방? 인원수: "+rClients.get(roomNo).size());
		sendRMessage(roomNo, nickName);
		if(rClients.get(roomNo).isEmpty()) {
			rClients.remove(roomNo);	
			System.out.println("방? 방 개수: "+rClients.size());
		}
	}
	
	public void sendWMessage(String nickName, String msg) {
		Iterator<String> it = wClients.keySet().iterator();
		String key;
		while(it.hasNext()) {
			key = it.next();
			try {
				wClients.get(key).writeUTF(nickName +" : "+msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void sendRMessage(String roomNo, String nickName, String msg) {
		Iterator<String> it = rClients.get(roomNo).keySet().iterator();
		String key;
		while(it.hasNext()) {
			key = it.next();
			try {
				rClients.get(roomNo).get(key).writeUTF(nickName +" : "+msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void sendRMessage(String roomNo, String nickName) {
		Iterator<String> it = rClients.get(roomNo).keySet().iterator();
		String key;
		while(it.hasNext()) {
			key = it.next();
			try {
				rClients.get(roomNo).get(key).writeUTF(nickName + "님이 퇴장하셨습니다.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class Receiver extends Thread {
		private DataInputStream in;
		private DataOutputStream out;
		private String[] msgArray;
		private String message;
		
		public Receiver(Socket socket) throws IOException {
			
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
			String msg = in.readUTF();
			msgArray = msg.split("/");
			if(msgArray[0].equals("0")) {
				addWClients(msgArray[1], out);					//닉네임만 등록
			} else {
				addRClients(msgArray[0], msgArray[1], out);		//방 번호와 닉네임 등록
			}
		}

		public void run() {
			try {
				while(in!=null) {
					message = in.readUTF();
					System.out.println(message);
					msgArray = message.split("/");
					if(msgArray[0].equals("close")) {
						if(msgArray[1].equals("0")) {
							removeWClients(msgArray[2]);					//대기실 맵에서 제거
							break;
						} else {
							removeRClients(msgArray[1], msgArray[2]);		//방 맵에서 제거
							break;
						}
					} else {
						if(msgArray[0].equals("0")) {
							sendWMessage(msgArray[1], msgArray[2]);					//닉네임과 메세지 전송
						} else {
							sendRMessage(msgArray[0], msgArray[1], msgArray[2]);	//방번호와 닉네임과 메세지 전송
						}
					}
				}
			} catch(IOException e) {
				e.printStackTrace();
				System.out.println("연결끊기");
			}
		}
	}
	public static void main(String[] args) throws IOException {
		new Server().setting();
	}
}
