package org.fog.test.perfeval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

public class Topology {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	static List<String> first = new ArrayList<String>();
	static List<String> second = new ArrayList<String>();
	static List<String> third = new ArrayList<String>();
	static List<String> fourth = new ArrayList<String>();
	static List<String> fifth = new ArrayList<String>();
	// static List<String> fifth_b = new ArrayList<String>();
	static ArrayList<String> nodes = new ArrayList<String>();
	static ArrayList<String> latencybetweenlevels = new ArrayList<String>();
	static int size, cloud, proxy, numberofDepts, numOfMobilesPerDept, iotnode = 0;
	static boolean CLOUD = false;
	static double EEG_TRANSMISSION_TIME = 5.1;
	static String[] latencyatlevel = null;

	static void readfile() throws FileNotFoundException {
		Scanner nodefile = new Scanner(new File("NodeFile.txt"));

		while (nodefile.hasNext()) {
			nodes.add(nodefile.next());
		}
		nodefile.close();
		System.out.println(nodes);
		Scanner latencies = new Scanner(new File("Latency.txt"));

		while (latencies.hasNext()) {
			latencybetweenlevels.add(latencies.next());
		}
		latencies.close();
		System.out.println(latencybetweenlevels);

	}

	static void createnodes() {
		
		System.out.println(nodes.get(0));
		String[] nodecountatlevels= nodes.get(0).split(",");
		latencyatlevel[]= latencybetweenlevels.get(0).split(",");
		
	      cloud = Integer.parseInt(nodecountatlevels[0]);
		  proxy =Integer.parseInt(nodecountatlevels[1]);
		 numberofDepts = Integer.parseInt(nodecountatlevels[2]);
		  numOfMobilesPerDept =Integer.parseInt(nodecountatlevels[3]);
		  iotnode=Integer.parseInt(nodecountatlevels[4]);
		
	}

	static void createLists() {
		size = nodes.size();
		System.out.println(size);
		for (int i = 1; i < 3; i++)
			first.add(nodes.get(i));
		System.out.println(first.size());
		System.out.println(first);

		for (int i = 3; i < 6; i++)
			second.add(nodes.get(i));
		System.out.println(second.size());
		System.out.println(second);

		for (int i = 6; i < 10; i++)
			third.add(nodes.get(i));
		System.out.println(third.size());
		System.out.println(third);

		for (int i = 10; i < 16; i++)
			fourth.add(nodes.get(i));
		System.out.println(fourth.size());
		System.out.println(fourth);

		for (int i = 16; i < size; i++)
			fifth.add(nodes.get(i));
		System.out.println(fifth.size());
		System.out.println(third);

	}

	public static void createDevice(Application application, List<FogDevice> fogDevices, int userId, String appId)
			throws IOException {
		for (int i = 0; i < cloud; i++) {
			System.out.println(first.get(i));
			FogDevice Cloud = createFogDevice(first.get(i));
			Cloud.setParentId(-1);
			fogDevices.add(Cloud);
			while (proxy < second.size()) {
				FogDevice Proxy = createFogDevice(second.get(i));
				Proxy.setParentId(Cloud.getId());
				Proxy.setUplinkLatency(Double.parseDouble(latencyatlevel[0])); // 100
				fogDevices.add(Proxy);
				proxy++;

				while (numberofDepts < third.size()) {
					addGw(userId, appId, Proxy.getId());
					numberofDepts++;
				}

			}

		}
		module(application);
	}

	private static FogDevice addGw(int userId, String appId, int parentId) throws IOException {
		FogDevice dept = createFogDevice(third.get(numberofDepts));
		fogDevices.add(dept);
		dept.setParentId(parentId);
		dept.setUplinkLatency(Double.parseDouble(latencyatlevel[1])); // 4

		while (numOfMobilesPerDept < fourth.size()) {
			// String mobileId = id+"-"+i;
			FogDevice mobile = addMobile(userId, appId, dept.getId());
			mobile.setUplinkLatency(Double.parseDouble(latencyatlevel[2])); // 2
			fogDevices.add(mobile);
			numOfMobilesPerDept++;
		}
		return dept;
	}

	private static FogDevice addMobile(int userId, String appId, int parentId) throws IOException {
		FogDevice mobile = createFogDevice(fourth.get(numOfMobilesPerDept));
		mobile.setParentId(parentId);
		while (iotnode < fifth.size()) {

			String line = fifth.get(iotnode);
			String[] indv_data = line.split(",");
			Sensor eegSensor = new Sensor(indv_data[0], indv_data[1], userId, appId,
					new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor
																			// follows a deterministic distribution
			sensors.add(eegSensor);
			String line1 = fifth.get(iotnode++);
			String[] indv_data1 = line1.split(",");
			Actuator display = new Actuator(indv_data1[0], userId, appId, indv_data1[1]);
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0);
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0);
			iotnode++;
		}
		return mobile;
	}

	private static FogDevice createFogDevice(String devices) {

		String[] values = devices.split(",");
		String nodeName = values[0];

		long mips = Long.parseLong(values[1]);

		int ram = Integer.parseInt(values[2]);

		long upBw = Long.parseLong(values[3]);
		long downBw = Long.parseLong(values[4]);
		int level = Integer.parseInt(values[5]);

		double ratePerMips = Double.parseDouble(values[6]);
		double busyPower = Double.parseDouble(values[7]);
		double idlePower = Double.parseDouble(values[8]);

		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bw), storage,
				peList, new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
		// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost,
				costPerMem, costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList,
					10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		return fogdevice;
	}

	static Application createApplication(String appId, int userId) {

		Application application = Application.createApplication(appId, userId); // creates an empty application model
																				// (empty directed graph)
		application.addAppModule("client", 10);
		application.addAppModule("concentration_calculator", 10); // adding module Concentration Calculator to the
																	// application model
		application.addAppModule("connector", 10); // adding module Connector to the application model
		if (EEG_TRANSMISSION_TIME == 10)
			application.addAppEdge("EEG", "client", 2000, 500, "EEG", Tuple.UP, AppEdge.SENSOR); // adding edge from EEG
																									// (sensor) to
																									// Client module
																									// carrying tuples
																									// of type EEG
		else
			application.addAppEdge("EEG", "client", 3000, 500, "EEG", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client", "concentration_calculator", 3500, 500, "_SENSOR", Tuple.UP, AppEdge.MODULE); // adding
																														// edge
																														// from
																														// Client
																														// to
																														// Concentration
																														// Calculator
																														// module
																														// carrying
																														// tuples
																														// of
																														// type
																														// _SENSOR
		application.addAppEdge("concentration_calculator", "connector", 100, 1000, 1000, "PLAYER_GAME_STATE", Tuple.UP,
				AppEdge.MODULE); // adding periodic edge (period=1000ms) from Concentration Calculator to
									// Connector module carrying tuples of type PLAYER_GAME_STATE
		application.addAppEdge("concentration_calculator", "client", 14, 500, "CONCENTRATION", Tuple.DOWN,
				AppEdge.MODULE); // adding edge from Concentration Calculator to Client module carrying tuples of
									// type CONCENTRATION
		application.addAppEdge("connector", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", Tuple.DOWN, AppEdge.MODULE); // adding
																														// periodic
																														// edge
																														// (period=1000ms)
																														// from
																														// Connector
																														// to
																														// Client
																														// module
																														// carrying
																														// tuples
																														// of
																														// type
																														// GLOBAL_GAME_STATE
		application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR); // adding
																													// edge
																													// from
																													// Client
																													// module
																													// to
																													// Display
																													// (actuator)
																													// carrying
																													// tuples
																													// of
																													// type
																													// SELF_STATE_UPDATE
		application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR); // adding
																														// edge
																														// from
																														// Client
																														// module
																														// to
																														// Display
																														// (actuator)
																														// carrying
																														// tuples
																														// of
																														// type
																														// GLOBAL_STATE_UPDATE

		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9)); // 0.9 tuples of type
																									// _SENSOR are
																									// emitted by Client
																									// module per
																									// incoming tuple of
																									// type EEG
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0)); // 1.0
																														// tuples
																														// of
																														// type
																														// SELF_STATE_UPDATE
																														// are
																														// emitted
																														// by
																														// Client
																														// module
																														// per
																														// incoming
																														// tuple
																														// of
																														// type
																														// CONCENTRATION
		application.addTupleMapping("concentration_calculator", "_SENSOR", "CONCENTRATION",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration
													// Calculator module per incoming tuple of type _SENSOR
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE",
				new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module
													// per incoming tuple of type GLOBAL_GAME_STATE

		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("EEG");
				add("client");
				add("concentration_calculator");
				add("client");
				add("DISPLAY");
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
			}
		};
		application.setLoops(loops);

		return application;
	}

	private static void module(Application application) {
		ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping

		if (CLOUD) {

			moduleMapping.addModuleToDevice("connector", "cloud");
			moduleMapping.addModuleToDevice("concentration_calculator", "cloud");
			for (FogDevice device : fogDevices) {
				if (device.getName().startsWith("m")) {

					moduleMapping.addModuleToDevice("client", device.getName()); // fixing all instances of the Client
																					// module to the Smartphones
				}
			}
		} else {

			moduleMapping.addModuleToDevice("connector", "cloud"); // fixing all instances of the Connector module to
																	// the Cloud
			// rest of the modules will be placed by the Edge-ward placement policy
		}

		Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);

		controller.submitApplication(application, 0,
				(CLOUD) ? (new ModulePlacementMapping(fogDevices, application, moduleMapping))
						: (new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));
	}

}
