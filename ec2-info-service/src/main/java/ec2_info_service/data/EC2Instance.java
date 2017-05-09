package ec2_info_service.data;

import java.io.Serializable;

public class EC2Instance implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String name;
	private String type;
	private String typeSize;
	private double memory;
	private int vCPU;
	private int instanceStorage;
	private boolean ssd;
	private boolean ebs;
	private String networkPerformance;
	private double linuxOnDemand;
	private double linuxReserved;
	private double windowsOnDemand;
	private double windowsReserved;

	@Override
	public String toString() {
		return name + " (" + type + ", " + typeSize + ")\n" 
				+ memory + " GB | " + vCPU + " CPUs | Network Performance: " + networkPerformance + "\n" 
				+ (ebs ? "EBS only" : instanceStorage + " (SSD: " + ssd + ")\n") 
				+ "Linux: " + linuxOnDemand + " | " + linuxReserved + "\n"
				+ "Windows: " + windowsOnDemand + " | " + windowsReserved;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTypeSize() {
		return typeSize;
	}

	public void setTypeSize(String typeSize) {
		this.typeSize = typeSize;
	}

	public double getMemory() {
		return memory;
	}

	public void setMemory(double memory) {
		this.memory = memory;
	}

	public int getvCPU() {
		return vCPU;
	}

	public void setvCPU(int vCPU) {
		this.vCPU = vCPU;
	}

	public int getInstanceStorage() {
		return instanceStorage;
	}

	public void setInstanceStorage(int instanceStorage) {
		this.instanceStorage = instanceStorage;
	}

	public boolean isSsd() {
		return ssd;
	}

	public void setSsd(boolean ssd) {
		this.ssd = ssd;
	}

	public boolean isEbs() {
		return ebs;
	}

	public void setEbs(boolean ebs) {
		this.ebs = ebs;
	}

	public String getNetworkPerformance() {
		return networkPerformance;
	}

	public void setNetworkPerformance(String networkPerformance) {
		this.networkPerformance = networkPerformance;
	}

	public double getLinuxOnDemand() {
		return linuxOnDemand;
	}

	public void setLinuxOnDemand(double linuxOnDemand) {
		this.linuxOnDemand = linuxOnDemand;
	}

	public double getLinuxReserved() {
		return linuxReserved;
	}

	public void setLinuxReserved(double linuxReserved) {
		this.linuxReserved = linuxReserved;
	}

	public double getWindowsOnDemand() {
		return windowsOnDemand;
	}

	public void setWindowsOnDemand(double windowsOnDemand) {
		this.windowsOnDemand = windowsOnDemand;
	}

	public double getWindowsReserved() {
		return windowsReserved;
	}

	public void setWindowsReserved(double windowsReserved) {
		this.windowsReserved = windowsReserved;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
