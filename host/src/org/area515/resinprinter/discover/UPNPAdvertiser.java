package org.area515.resinprinter.discover;

import java.net.URI;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.resource.Resource;
import org.fourthline.cling.model.types.DLNADoc;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.connectionmanager.AbstractPeeringConnectionManagerService;

public class UPNPAdvertiser implements Advertiser {
	public static UPNPSetup INSTANCE = new UPNPSetup();
	
	public static class UPNPSetup {
		private String deviceName = "Multiprint";
		private int deviceVersion = 1;//TODO: get from build.number file
		private String manufacturer = "Wes & Sean";
		private String deviceType = "3DPrinterHost";
		private int upnpStreamPort = 5001;
		
		public String getDeviceName() {
			return deviceName;
		}
		public String getManufacturer() {
			return manufacturer;
		}
	}
	
	public UPNPSetup getSetup() {
		//TODO: Get properties from HostProperties.Instance()
		return INSTANCE;
	}
	
	@Override
	public void start(URI webPresentationURI) {
		try {
			UDN udn = UDN.uniqueSystemIdentifier(getSetup().deviceName + getSetup().deviceVersion + getSetup().manufacturer);
			DeviceType type = new UDADeviceType(getSetup().deviceType, getSetup().deviceVersion);
	
			DeviceDetails details = new DeviceDetails(getSetup().deviceName,
					new ManufacturerDetails(getSetup().manufacturer), 
						new ModelDetails(
								getSetup().deviceName, 
								getSetup().deviceName, 
							"v" + getSetup().deviceVersion), 
							webPresentationURI, 
					new DLNADoc[] {
							new DLNADoc("DMS", DLNADoc.Version.V1_5),
							new DLNADoc("M-DMS", DLNADoc.Version.V1_5) },
					null);
	
			LocalService<PrinterDirectoryService> contentManagerService = new AnnotationLocalServiceBinder().read(PrinterDirectoryService.class);
			new PrinterHostSettingsServiceManager<PrinterDirectoryService>(getSetup(), webPresentationURI, contentManagerService, PrinterDirectoryService.class);
			contentManagerService.setManager(new DefaultServiceManager<PrinterDirectoryService>(contentManagerService, PrinterDirectoryService.class));
			
			LocalService<AbstractPeeringConnectionManagerService> connectionManagerService = new AnnotationLocalServiceBinder().read(AbstractPeeringConnectionManagerService.class);
			connectionManagerService.setManager(new DefaultServiceManager<AbstractPeeringConnectionManagerService>(connectionManagerService, AbstractPeeringConnectionManagerService.class));
			
			LocalDevice printerServer = new LocalDevice(new DeviceIdentity(udn), type, details, new LocalService[]{contentManagerService, connectionManagerService});
	
			DefaultUpnpServiceConfiguration serviceConfiguration = new DefaultUpnpServiceConfiguration(getSetup().upnpStreamPort);
			UpnpServiceImpl upnpService = new UpnpServiceImpl(serviceConfiguration);
			if (!upnpService.getRouter().isEnabled()) {
				throw new IllegalArgumentException("It doesn't seem as though a network is available to publish this server, or an advertiser has already been started on this address.");
			}
			
			// Broadcast a search message for all devices
			ControlPoint controlPoint = upnpService.getControlPoint();
			controlPoint.search(new STAllHeader());
	
			// Add the bound local device to the registry
			upnpService.getRegistry().addDevice(printerServer);
			
			//NetworkAddress hooked = upnpService.getRouter().getActiveStreamServers(upnpService.getRouter().getNetworkAddressFactory().getBindAddresses()[0]).get(0);
			
			//By this point the externallyAccessableIP will be setup with the local IP if it started off null
			for (Resource device : upnpService.getRegistry().getResources()) {
				if (device.getModel() instanceof LocalDevice) {
					//LOGGER.info("===========");
					//LOGGER.info(device.getModel() + " exposed on: " + httpAddress + device.getPathQuery());
					System.out.println("Relative UPNP root descriptor:" + device.getPathQuery());
					//LOGGER.info("===========");
				} else {
					//LOGGER.info(device.getModel() + " exposed on: " + httpAddress + device.getPathQuery());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Couldn't advertise URI:" + webPresentationURI);
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
