package ec2_info_service.internal;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import ec2_info_service.data.EC2Instance;

public class Utilities {

	private static List<EC2Instance> instances = new ArrayList<>();
	
	public static Map<String, String> instanceNames = new HashMap<>();
	public static Map<String, String> instanceTypes = new HashMap<>();
	public static Map<String, List<String>> instanceTypesBySize = new HashMap<>();
	public static List<String> ssdInstances = new ArrayList<>();
	public static List<String> ebsInstances = new ArrayList<>();
	public static Map<String, Set<String>> instancesByNetworkPerf = new HashMap<>();
	public static Map<String, Map<String, Double>> onDemandCosts = new HashMap<>();
	public static Map<String, Map<String, Double>> reservedCosts = new HashMap<>();
	
	public static void extract() {

		JSONParser parser = new JSONParser();
        try {

            Object obj = parser.parse(new FileReader("instances.json"));
            JSONArray jsonArray = (JSONArray) obj;
            for(Object jsonObject : jsonArray){
            	
            	JSONObject jsonInstance = (JSONObject) jsonObject;
            	
            	String type = (String) jsonInstance.get("instance_type");
            	double memory = (Double) jsonInstance.get("memory");
            	String networkPerf = (String) jsonInstance.get("network_performance");
            	networkPerf = networkPerf.toLowerCase();
            	networkPerf = networkPerf.replaceAll(" ", "_");
            	String name = (String) jsonInstance.get("pretty_name");
            	long vCPUs = (Long) jsonInstance.get("vCPU");
            	
            	JSONObject pricing = (JSONObject) jsonInstance.get("pricing");
            	String linuxOnDemand = "", linuxReserved = "", windowsOnDemand = "", windowsReserved = "";
            	try{
	            	JSONObject usEastPricing = (JSONObject) pricing.get("us-east-1");
	            	JSONObject linuxPricing = (JSONObject) usEastPricing.get("linux");
	            	linuxOnDemand = (String) linuxPricing.get("ondemand");
	            	JSONObject linuxReservedPricing = (JSONObject) linuxPricing.get("reserved");
	            	linuxReserved = (String) linuxReservedPricing.get("yrTerm1Standard.noUpfront");
	            	JSONObject windowsPricing = (JSONObject) usEastPricing.get("mswin");
	            	windowsOnDemand = (String) windowsPricing.get("ondemand");
	            	JSONObject windowsReservedPricing = (JSONObject) windowsPricing.get("reserved");
	            	windowsReserved = (String) windowsReservedPricing.get("yrTerm1Standard.noUpfront");
            	} catch (Exception e){
            		continue;
            	}
            	
            	JSONObject storage = (JSONObject) jsonInstance.get("storage");
            	long numDevices = 0, size = -1;
            	boolean ssd = false;
            	if(storage != null){
	            	numDevices = (Long) storage.get("devices");
	            	size = (Long) storage.get("size");
	            	ssd = (Boolean) storage.get("ssd");
            	}
            	
            	EC2Instance instance = new EC2Instance();
            	instance.setType(type.substring(0, type.indexOf('.')));
            	instance.setTypeSize(type.substring(type.indexOf('.')+1));
            	instance.setMemory(memory);
            	instance.setNetworkPerformance(networkPerf);
            	instance.setName(name);
            	instance.setvCPU(Math.toIntExact(vCPUs));
            	instance.setLinuxOnDemand(Double.valueOf(linuxOnDemand));
            	instance.setLinuxReserved(Double.valueOf(linuxReserved));
            	instance.setWindowsOnDemand(Double.valueOf(windowsOnDemand));
            	instance.setWindowsReserved(Double.valueOf(windowsReserved));
            	if(size != -1){
	            	instance.setInstanceStorage(Math.toIntExact(size*numDevices));
	            	instance.setSsd(ssd);
            	} else {
            		instance.setEbs(true);
            	}
            	instances.add(instance);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        createLists();
	}
	
	private static void createLists(){
		
		for(EC2Instance instance : instances){
			String instanceAPIName = instance.getType()+"."+instance.getTypeSize();
			instanceNames.put(instanceAPIName, instance.getName());
			if(!instanceTypes.containsKey(instance.getType())){
				instanceTypes.put(instance.getType(), getTypeDescription(instance.getType()));
			}
			if(!instanceTypesBySize.containsKey(instance.getTypeSize())){
				instanceTypesBySize.put(instance.getTypeSize(), new ArrayList<String>());
			}
			instanceTypesBySize.get(instance.getTypeSize()).add(instance.getType());
			if(instance.isSsd()){
				ssdInstances.add(instance.getType()+"."+instance.getTypeSize());
			}
			if(instance.isEbs()){
				ebsInstances.add(instance.getType()+"."+instance.getTypeSize());
			}
			if(!instancesByNetworkPerf.containsKey(instance.getNetworkPerformance())){
				instancesByNetworkPerf.put(instance.getNetworkPerformance(), new HashSet<>());
			}
			instancesByNetworkPerf.get(instance.getNetworkPerformance()).add(instance.getType()+"."+instance.getTypeSize());
			onDemandCosts.put(instanceAPIName, new HashMap<>());
			onDemandCosts.get(instanceAPIName).put("linux", instance.getLinuxOnDemand());
			onDemandCosts.get(instanceAPIName).put("windows", instance.getWindowsOnDemand());
			reservedCosts.put(instanceAPIName, new HashMap<>());
			reservedCosts.get(instanceAPIName).put("linux", instance.getLinuxReserved());
			reservedCosts.get(instanceAPIName).put("windows", instance.getWindowsReserved());
		}
	}
	
	private static String getTypeDescription(String type){
		switch(type){
			case "t2":
				return "T2 instances are Burstable Performance Instances that provide a baseline level of CPU performance with the ability to burst above the baseline.";
			case "m3":
			case "m4":
				return "This family provides a balance of compute, memory, and network resources.";
			case "c3":
			case "c4":
				return "These instances are the latest generation of Compute-optimized instances, featuring the highest performing processors and the lowest price/compute performance.";
			case "x1":
				return "X1 Instances are optimized for large-scale, enterprise-class, in-memory applications and have the lowest price per GiB of RAM.";
			case "r3":
			case "r4":
				return "These instances are optimized for memory-intensive applications and offer lower price per GiB of RAM.";
			case "p2":
				return "P2 instances are intended for general-purpose GPU compute applications.";
			case "g2":
				return "G2 instances are optimized for graphics-intensive applications.";
			case "f1":
				return "F1 instances offer customizable hardware acceleration with field programmable gate arrays (FPGAs).";
			case "i3":
				return "This family includes the High Storage Instances that provide Non-Volatile Memory Express (NVMe) SSD backed instance storage optimized for low latency, very high random I/O performance, high sequential read throughput and provide high IOPS at a low cost.";
			case "d2":
				return "D2 instances feature up to 48 TB of HDD-based local storage, deliver high disk throughput, and offer the lowest price per disk throughput performance.";
			default: 
				return "No description available. Please visit https://aws.amazon.com/ec2/instance-types/ for more info.";
		}
	}
	
	public static List<EC2Instance> getInstances(){
		return new ArrayList<EC2Instance>(instances);
	}
}
