package election.algorithm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;

import election.algorithm.Message.MessageType;

public class Process {

	private int id;
	private boolean isParticipant;
	private String ipNeighbor;
	private String ipElectedProcess;
	private String ip;
	private List<String> ringProcessesIp;

	private static final int DEFAULT_PORT = 40;
	
	public Process(int id,String ip, String ipNeighbor,List<String> ringProcessesIp){
		
		this.ip = ip;
		this.id = id;
		this.ipNeighbor = ipNeighbor;
		
		this.isParticipant = false;
		this.ringProcessesIp = ringProcessesIp;
	
		
		initSocketCollectMessages();
	}
	

			
	public void initElection() {
		 
		writeLogMessage("---------------------------------//----------------------------------");
		writeLogMessage( "Processo "+getId()+" iniciou a eleição.");
		writeLogMessage("---------------------------------//----------------------------------");
	
		setParticipant(true);
		Message message = new Message(getId(),Message.MessageType.ELECTION);

		forwardMessage(this.ipNeighbor,message);
	}
	
	private void initSocketCollectMessages(){
		
		Process process = this;
		
		Runnable serverRunnable = new Runnable() {
			
			@Override
			public void run() {
				ServerSocket serverSocket = null;
		        
				try{
				 
					serverSocket = new ServerSocket(DEFAULT_PORT);
			        
			        while(true){
			        	Socket clientSocket = serverSocket.accept();
			        	new Thread(new ReadMessage(clientSocket,process)).start();
			        	
			        }

				} catch(Exception e){
					e.printStackTrace();
					
					if(serverSocket != null)
						try{serverSocket.close();} catch (Exception e2) {e2.printStackTrace();}
					
				}
				
			}
		};
		
		Thread serverThread = new Thread(serverRunnable);
		serverThread.start();
	
		
	}
	
	public static class ReadMessage implements Runnable{
		
		private Socket socket;
		private Process process;
		
		public ReadMessage(Socket socket,Process process) {
			this.socket = socket;
			this.process = process;
		}

		@Override
		public void run() {
			
			if(socket.isConnected()){
				
				try {
					
                    InputStream input = socket.getInputStream();
                    
                    byte[] buffer = new byte[500];
                    input.read(buffer);
                    
                    String json = new String(buffer).trim();
                    Message message = new Gson().fromJson(json, Message.class);
                     
                    if(message != null && process != null){
                    	
                    	process.writeLogMessage("Mensagem recebida pelo processo "+process.getId()+": "+json);
                   
                        if(message.getMessageType() == MessageType.ELECTION){
                        	
                        	if(message.getId() > process.getId()){
                        		process.setParticipant(true);
                        		process.forwardMessage(process.getIpNeighbor(),message);
                        		
                        	} else if (message.getId() < process.getId()){
                        		
                        		if(!process.isParticipant()){
                        			process.setParticipant(true);
                        			message.setId(process.getId());
                        			process.forwardMessage(process.getIpNeighbor(),message);
                        			
                        		}
                        	} else if(message.getId() == process.getId()){
                        		process.setParticipant(false);
                        		message.setMessageType(Message.MessageType.ELECTED);
                        		message.setCoordinatorIp(process.getIp());
                        		process.forwardMessage(process.getIpNeighbor(),message);
                        	}
                        	
                        	
                        } else if(message.getMessageType() == MessageType.ELECTED){
                        	
                        	process.setParticipant(false);
                        	process.setIpElectedProcess(message.getCoordinatorIp());
                        	
                        	if(message.getId() != process.getId()){
                        		process.forwardMessage(process.getIpNeighbor(),message);
                        		process.checkCoordinatorStatus();
                        	} else {
                        		
                        		message.setElectionTime( (System.nanoTime() - message.getElectionTime()) /1000000 );
                        		
                        		process.writeLogMessage("---------------------------------//----------------------------------");
                        		process.writeLogMessage("Id do processo eleito: "+message.getId());
                        		process.writeLogMessage("IP do processo eleito: "+message.getCoordinatorIp());
                        		process.writeLogMessage("Quantidade de mensagens trocadas: "+message.getMessagesQuantity());
                        		process.writeLogMessage("Tempo total da eleição em milisegundos: "+message.getElectionTime());
                        		process.writeLogMessage("---------------------------------//----------------------------------");

                            }
                        	
                        	process.checkCoordinatorStatus();
                        	
                        }
                    }

                    socket.close();
                    
				} catch(Exception e){
					e.printStackTrace();
				}
				
			}
			
		}
		
	}
	
	private void forwardMessage(String neighborIp,Message message){
	
		boolean connectionError = false;
		try {
			
			Socket socket = new Socket(neighborIp,DEFAULT_PORT);
	
	        if (socket.isConnected()) {
	
	        	writeLogMessage("Processo "+this.id+" envia a seguinte mensagem para o vizinho: "+new Gson().toJson(message));
                
	            OutputStream out = socket.getOutputStream();
	
	            message.setMessagesQuantity(message.getMessagesQuantity() + 1);
	            String jsonMensagem = new Gson().toJson(message);
	            out.write(jsonMensagem.getBytes());

	        } else
	        	connectionError = true;
	        
	
	        socket.close();
		} catch (ConnectException ce) {
			connectionError = true;	
		} catch(IOException e){
			e.printStackTrace();
		} 
		
		
		if(connectionError) {
			
			writeLogMessage("Processo vizinho com o IP "+this.ipNeighbor+" não respondeu.");
        	
        	if(!this.ringProcessesIp.isEmpty()) {
        		
        		this.ipNeighbor = this.ringProcessesIp.get(0);
        	 	this.ringProcessesIp.remove(0);
	        	
        	 	forwardMessage(this.ipNeighbor, message);
	        	
        	} else
        		writeLogMessage("O processo "+this.id+" está sozinho no sistema(Anel)");
        	
		}
	}
	
	private void checkCoordinatorStatus(){
		 
		Timer timer = new Timer();
		Process process = this;
	
		TimerTask timerTask = new TimerTask() {
			
			@Override
			public void run() {
				
				boolean connectionError = false;
				try {
						
						Socket socket = new Socket(process.getIpElectedProcess(),DEFAULT_PORT);
				
				        if (!socket.isConnected()) 
				        	connectionError = true;
				
				        socket.close();
					} catch (ConnectException ce) {
						connectionError = true;
					} catch(IOException e){
						e.printStackTrace();
					} 
				
				if(connectionError && !process.isParticipant()) {
					
				    this.cancel();
				    timer.cancel();
				    timer.purge();
				    
					writeLogMessage("**************************************//***********************************");
					writeLogMessage("Coordenador parou de responder, uma nova eleição vai ser iniciada pelo processo "+process.getId());
					writeLogMessage("**************************************//***********************************");
					process.initElection();
				
				}
			}
		};
		
		
	    int intervalPeriod = 3 * 1000; 
	   
	    timer.scheduleAtFixedRate(timerTask, 0,
	    		intervalPeriod);
		
	}
	
	private void writeLogMessage(String log){
		
		try {

			File file = new File("election-log.txt");

			if (!file.exists()) {
				file.createNewFile();
			}
			
			FileOutputStream fileOutputStream =  new FileOutputStream(file,true);
			OutputStreamWriter outWriter = new OutputStreamWriter(fileOutputStream);
            outWriter.append(log);
            outWriter.append("\r\n");
            outWriter.close();
            fileOutputStream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isParticipant() {
		return isParticipant;
	}

	public void setParticipant(boolean isParticipant) {
		this.isParticipant = isParticipant;
	}

	public void setIpNeighbor(String ipNeighbor) {
		this.ipNeighbor = ipNeighbor;
	}
	
	public int getId() {
		return id;
	}

	public String getIpNeighbor() {
		return ipNeighbor;
	}


	public String getIpElectedProcess() {
		return ipElectedProcess;
	}

	public void setIpElectedProcess(String ipElectedProcess) {
		this.ipElectedProcess = ipElectedProcess;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
}
