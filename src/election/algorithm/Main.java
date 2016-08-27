package election.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
	

	public static void main(String[] args) {

		int processId = Integer.parseInt(args[0]);
		String processIp = args[1];
		String neighborProcessIp = args[2];
		
		List<String> ringProcessesIp = new ArrayList<String>(Arrays.asList(args[3].split(",")));
		Process process = new Process(processId,processIp,neighborProcessIp,ringProcessesIp);
		
		if(Boolean.parseBoolean(args[4])){
			process.initElection();
		}
		
	
		
	}
}
