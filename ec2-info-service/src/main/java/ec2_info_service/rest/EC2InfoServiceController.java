package ec2_info_service.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec2_info_service.data.EC2Instance;
import ec2_info_service.internal.Utilities;

@RestController
public class EC2InfoServiceController {

	@PostConstruct
	public void init(){
		Utilities.extract();
	}

	@RequestMapping("/names")
	public Map<String, String> names(){
		return Utilities.instanceNames;
	}
	
	@RequestMapping("/info/{apiName:.+}")
	public EC2Instance info(@PathVariable String apiName){
		String type = apiName.substring(0, apiName.indexOf('.'));
		String typeSize = apiName.substring(apiName.indexOf('.')+1);
		for(EC2Instance instance : Utilities.getInstances()){
			if(instance.getType().equals(type) && instance.getTypeSize().equals(typeSize)){
				return instance;
			}
		}
		return null;
	}
	
	@RequestMapping("/sizes")
	public Set<String> sizes(){
		return Utilities.instanceTypesBySize.keySet();
	}
	
	@RequestMapping("/types")
	public Map<String, String> types(){
		return Utilities.instanceTypes;
	}
	
	@RequestMapping("/types/{size}")
	public List<String> typesBySize(@PathVariable String size){
		return Utilities.instanceTypesBySize.get(size);
	}
	
	@RequestMapping("/ssdinstances")
	public List<String> ssdInstances(){
		return Utilities.ssdInstances;
	}
	
	@RequestMapping("/ebsinstances")
	public List<String> ebsInstances(){
		return Utilities.ebsInstances;
	}
	
	@RequestMapping({"/networkperfs", "/networkperfs/{perf}"})
	public Set<String> networkPerfs(@PathVariable Optional<String> perf){
		if(perf.isPresent()){
			return Utilities.instancesByNetworkPerf.get(perf.get());
		}
		return Utilities.instancesByNetworkPerf.keySet();
	}
	
	@RequestMapping({"/costs/{pricingModel}", "/costs/{pricingModel}/{os}"})
	public Map<String, ?> pricingCosts(@PathVariable String pricingModel, @PathVariable Optional<String> os){
		if(pricingModel.equals("reserved")){
			if(os.isPresent()){
				Map<String, Double> OSReservedCosts = new HashMap<>();
				Map<String, Map<String, Double>> reservedCosts = Utilities.reservedCosts;
				for(String key : reservedCosts.keySet()){
					OSReservedCosts.put(key, reservedCosts.get(key).get(os.get()));
				}
				return OSReservedCosts;
			}
			return Utilities.reservedCosts;
		} else if(pricingModel.equals("ondemand")){
			if(os.isPresent()){
				Map<String, Double> OSOnDemandCosts = new HashMap<>();
				Map<String, Map<String, Double>> onDemandCosts = Utilities.onDemandCosts;
				for(String key : onDemandCosts.keySet()){
					OSOnDemandCosts.put(key, onDemandCosts.get(key).get(os.get()));
				}
				return OSOnDemandCosts;
			}
			return Utilities.onDemandCosts;
		}
		return null;
	}
	
	@RequestMapping("/filterbymemory/{min:.+}")
	public List<String> filterByMemory(@PathVariable Double min){
		List<String> filteredList = new ArrayList<>();
		for(EC2Instance instance : Utilities.getInstances()){
			if(instance.getMemory() >= min.doubleValue()){
				filteredList.add(instance.getType()+"."+instance.getTypeSize());
			}
		}
		return filteredList;
	}
	
	@RequestMapping("/filterbycpus/{min}")
	public List<String> filterByCPUs(@PathVariable Integer min){
		List<String> filteredList = new ArrayList<>();
		for(EC2Instance instance : Utilities.getInstances()){
			if(instance.getvCPU() >= min.intValue()){
				filteredList.add(instance.getType()+"."+instance.getTypeSize());
			}
		}
		return filteredList;
	}
	
	@RequestMapping("/filterbystorage/{min}")
	public List<String> filterByStorage(@PathVariable Integer min){
		List<String> filteredList = new ArrayList<>();
		for(EC2Instance instance : Utilities.getInstances()){
			if(instance.getInstanceStorage() >= min.intValue()){
				filteredList.add(instance.getType()+"."+instance.getTypeSize());
			}
		}
		return filteredList;
	}
	
	@RequestMapping("/filterbycost/{pricingModel}/{os}/{max:.+}")
	public Map<String, Double> filterByCost(@PathVariable String pricingModel, @PathVariable String os, @PathVariable Double max){
		if(pricingModel.equals("reserved")){
			Map<String, Double> OSReservedCosts = new HashMap<>();
			Map<String, Map<String, Double>> reservedCosts = Utilities.reservedCosts;
			for(String key : reservedCosts.keySet()){
				if(reservedCosts.get(key).get(os) <= max){
					OSReservedCosts.put(key, reservedCosts.get(key).get(os));
				}
			}
			return OSReservedCosts;
		} else if(pricingModel.equals("ondemand")){
			Map<String, Double> OSOnDemandCosts = new HashMap<>();
			Map<String, Map<String, Double>> onDemandCosts = Utilities.onDemandCosts;
			for(String key : onDemandCosts.keySet()){
				if(onDemandCosts.get(key).get(os) <= max){
					OSOnDemandCosts.put(key, onDemandCosts.get(key).get(os));
				}
			}
			return OSOnDemandCosts;
		}
		return null;
	}
	
    @RequestMapping("/list")
    public List<EC2Instance> list() {
        return Utilities.getInstances();
    }
}
