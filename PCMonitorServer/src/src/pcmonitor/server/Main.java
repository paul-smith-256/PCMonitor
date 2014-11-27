package src.pcmonitor.server;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarPermissionDeniedException;
import org.hyperic.sigar.Swap;

import src.pcmonitor.net.BasePacket;
import src.pcmonitor.net.BaseRequestPacket;
import src.pcmonitor.net.BaseResponsePacket;
import src.pcmonitor.net.CpuLoadRequestPacket;
import src.pcmonitor.net.CpuLoadResponsePacket;
import src.pcmonitor.net.MemUsageRequestPacket;
import src.pcmonitor.net.MemUsageResponsePacket;
import src.pcmonitor.net.NetInterface;
import src.pcmonitor.net.NetworkInterfaceListRequestPacket;
import src.pcmonitor.net.NetworkInterfaceListResponsePacket;
import src.pcmonitor.net.PacketHelper;
import src.pcmonitor.net.Ports;
import src.pcmonitor.net.ProcessListRequestPacket;
import src.pcmonitor.net.ProcessListResponsePacket;
import src.pcmonitor.net.SearchRequestPacket;
import src.pcmonitor.net.SearchResponsePacket;
import src.pcmonitor.net.ServerConfig;
import src.pcmonitor.net.ServerConfigRequestPacket;
import src.pcmonitor.net.ServerConfigResponsePacket;
import src.pcmonitor.net.ServerProcess;

public class Main {

	public static void main(String[] args) {
		configureLogger();
		addTrayIcon();
		initConfiguration();
		openSocket();
		configureSigar();
		
		while (true) {
			try {
				Request request = receivePacket();
				BaseRequestPacket packet = request.packet;
				InetSocketAddress sender = request.address;
				BaseResponsePacket response;
				if (packet instanceof SearchRequestPacket) {
					response = new SearchResponsePacket(
							sConfiguration.getString(SERVER_NAME_PROP, getHostname()));
				}
				else if (packet instanceof ServerConfigRequestPacket) {
					response = getServerConfigPacket();
				}
				else if (packet instanceof CpuLoadRequestPacket) {
					response = getCpuLoadPacket();
				}
				else if (packet instanceof MemUsageRequestPacket) {
					response = getMemUsageResponsePacket();
				}
				else if (packet instanceof NetworkInterfaceListRequestPacket) {
					response = getNetworkInterfacesListResponsePacket();
				}
				else if (packet instanceof ProcessListRequestPacket) {
					response = getProcessListResponsePacket();
				}
				else {
					sLog.info("Packet not recognized " + packet);
					continue;
				}
				response.setId(packet.getId());
				sLog.fine("Sending " + response + " to " + sender);
				sendPacket(sender, response);
			}
			catch (IOException e) {
				sLog.log(WARNING, "IOException raised in main cycle", e);
			}
			catch (SigarException e) {
				sLog.log(WARNING, "Sigar exception raised in main cycle", e);
			}
		}
	}
	
	private static ServerConfigResponsePacket getServerConfigPacket() throws SigarException {
		return new ServerConfigResponsePacket(new ServerConfig(
				sSigar.getCpuPercList().length, 
				sSigar.getMem().getRam()));
	}
	
	private static CpuLoadResponsePacket getCpuLoadPacket() throws SigarException {
		CpuPerc[] percs = sSigar.getCpuPercList();
		float[] load = new float[percs.length];
		for (int i = 0; i < percs.length; i++) {
			load[i] = (float) percs[i].getCombined();
		}
		return new CpuLoadResponsePacket(load);
	}
	
	private static MemUsageResponsePacket getMemUsageResponsePacket() throws SigarException {
		Mem m = sSigar.getMem();
		Swap s = sSigar.getSwap();
		return new MemUsageResponsePacket(m.getTotal(), m.getUsed(), m.getFree(), 
				s.getUsed(), s.getFree());
	}
	
	private static NetworkInterfaceListResponsePacket getNetworkInterfacesListResponsePacket()
			throws SigarException {
		String[] ifaceNames = sSigar.getNetInterfaceList();
		ArrayList<NetInterface> ifaces = new ArrayList<NetInterface>();
		for (int i = 0; i < ifaceNames.length; i++) {
			NetInterfaceConfig cfg = sSigar.getNetInterfaceConfig(ifaceNames[i]);
			String descr;
			if (runningOnWindows() && encodeIfaceNames) {
				descr = encodeFromCP251ToUTF(cfg.getDescription());
				sLog.info("Descr = " + descr);
			}
			else {
				descr = cfg.getDescription();
			}
			if (!cfg.getAddress().equals("0.0.0.0") && !cfg.getAddress().equals("127.0.0.1")) {
				ifaces.add(new NetInterface(cfg.getName(), descr, cfg.getAddress(), 
						cfg.getNetmask(), cfg.getHwaddr(), cfg.getType(), cfg.getMtu(), cfg.getMetric()));
			}
		}
		return new NetworkInterfaceListResponsePacket(ifaces);
	}
	
	private static ProcessListResponsePacket getProcessListResponsePacket() throws SigarException {
		ArrayList<ServerProcess> result = new ArrayList<ServerProcess>();
		long[] pids = sSigar.getProcList();
		for (long pid: pids) {
			try {
				ServerProcess p = new ServerProcess(
						new File(sSigar.getProcExe(pid).getName()).getName(), 
						sSigar.getProcCredName(pid).getUser(),
						sSigar.getProcMem(pid).getResident(),
						(float) sSigar.getProcCpu(pid).getPercent());
				result.add(p);
			}
			catch (SigarPermissionDeniedException e) {
				sLog.fine("Premission denied for process with pid " + pid);
			}
		}
		return new ProcessListResponsePacket(result);
	}
	
	private static void configureSigar() {
		sSigar = new Sigar();
	}
	
	private static void addTrayIcon() {
		if (!SystemTray.isSupported()) {
			sLog.warning("No system tray");
			return;
		}
		PopupMenu menu = new PopupMenu();
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				sLog.info("Exiting");
				System.exit(0);
			}
		});
		menu.add(exitItem);
		TrayIcon icon = new TrayIcon(getIcon());
		icon.setPopupMenu(menu);
		SystemTray tray = SystemTray.getSystemTray();
		try {
			tray.add(icon);
		}
		catch (AWTException e) {
			die("Cannot set tray icon: " + e.getMessage());
		}
	}
	
	private static Image getIcon() {
		try {
			InputStream in = ClassLoader.getSystemResourceAsStream(TRAY_ICON_FILE);
			if (in == null) {
				die("Cannot load image for tray icon");
			}
			return ImageIO.read(in);
		}
		catch (IOException e) {
			die("Cannot read tray icon image: " + e.getMessage());
			return null;
		}
	}
	
	private static void initConfiguration() {
		try {
			sConfiguration = new XMLConfiguration(CONFIG_FILENAME);
		}
		catch (ConfigurationException e) {
			die("Cannot open configuration file: " + e);
		}
	}
	
	private static String getHostname() {
		if (sHostname == null) {
			try {
				sHostname = InetAddress.getLocalHost().getHostName();
			}
			catch (UnknownHostException e) {
				sLog.log(WARNING, "Cannot get hostame, setting default value 'server'", e);
			}
		}
		return sHostname;
	}
	
	private static void openSocket() {
		try {
			if (LISTEN_TO_EMULATOR) {
				sSocket = new DatagramSocket(new InetSocketAddress("127.0.0.1", Ports.SERVER_PORT));
			}
			else {
				sSocket = new DatagramSocket(Ports.SERVER_PORT);
			}
			sSocket.setBroadcast(true);
			sLog.log(INFO, "Listening on " + sSocket.getLocalSocketAddress());
		}
		catch (SocketException e) {
			die("Cannot open socket: " + e);
		}
	}
	
	private static void closeSocket() {
		if (sSocket != null) {
			sSocket.close();
		}
	}
	
	private static Request receivePacket() throws IOException {
		sSocket.receive(sReceiveBuffer);
		BasePacket basePacket = PacketHelper.deserializePacket(sReceiveBuffer);
		if (!(basePacket instanceof BaseRequestPacket)) {
			throw new IOException(basePacket.getClass().getName() + " is not instance of request packet");
		}
		BaseRequestPacket baseRequestPacket = (BaseRequestPacket) basePacket;
		InetSocketAddress sender = (InetSocketAddress) sReceiveBuffer.getSocketAddress();
		Request result = new Request(sender, baseRequestPacket);
		sLog.fine("Received request " + result);
		return result;
	}
	
	private static void sendPacket(InetSocketAddress recipient, BaseResponsePacket response)
			throws IOException {
		PacketHelper.serializePacket(response, sSendBuffer);
		sLog.fine("Packet size = " + sSendBuffer.getData().length);
		sSendBuffer.setSocketAddress(recipient);
		sSocket.send(sSendBuffer);
	}
	
	private static void die(String message) {
		sLog.severe(message);
		System.exit(1);
	}
	
	private static class Request {
		
		public Request(InetSocketAddress address, BaseRequestPacket packet) {
			this.address = address;
			this.packet = packet;
		}
		
		@Override
		public String toString() {
			return Request.class.toString() + "[" + address + ", " + packet + "]";
		}
		
		public final InetSocketAddress address;
		public final BaseRequestPacket packet;
	}
	
	private static void configureLogger() {
		try {
			InputStream loggerConfig = ClassLoader.getSystemResourceAsStream("logger.config");
			if (loggerConfig != null)
				LogManager.getLogManager().readConfiguration(loggerConfig);
			else
				sLog.log(WARNING, "Config file not found");
		}
		catch (IOException e) {
			sLog.log(WARNING, "Cannot read configuration file", e);
		}
		
	}
	
	private static boolean runningOnWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}
	
	private static String encodeFromCP251ToUTF(String s) {
		Charset ibm866 = Charset.forName("ibm866");
		Charset utf16 = Charset.forName("UTF-16BE");
		byte[] full = s.getBytes(utf16);
		byte[] compact = new byte[full.length / 2];
		for (int i = 0; i < compact.length; i++) {
			compact[i] = full[2 * i + 1];
		}
		return new String(compact, ibm866);
	}
	
	private static final Logger sLog = Logger.getLogger(Main.class.getName());
	private static final boolean LISTEN_TO_EMULATOR = true;
	private static Configuration sConfiguration;
	private static final String CONFIG_FILENAME = "config.xml";
	private static final String SERVER_NAME_PROP = "serverName";
	private static String sHostname;
	private static DatagramSocket sSocket;
	private static DatagramPacket sReceiveBuffer = new DatagramPacket(
			new byte[BasePacket.MAX_PACKET_SIZE], BasePacket.MAX_PACKET_SIZE);
	private static DatagramPacket sSendBuffer = new DatagramPacket(
			new byte[BasePacket.MAX_PACKET_SIZE], BasePacket.MAX_PACKET_SIZE);
	private static final String TRAY_ICON_FILE = "trayIcon.png";
	private static Sigar sSigar;
	private static final boolean encodeIfaceNames = true;
}
