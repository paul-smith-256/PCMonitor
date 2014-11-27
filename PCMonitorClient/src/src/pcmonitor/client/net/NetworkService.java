package src.pcmonitor.client.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

import src.pcmonitor.net.BasePacket;
import src.pcmonitor.net.BaseRequestPacket;
import src.pcmonitor.net.BaseResponsePacket;
import src.pcmonitor.net.PacketHelper;
import src.pcmonitor.net.Ports;
import src.pcmonitor.net.ServerIdentity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class NetworkService extends Service {
	
	@Override
	public void onCreate() {
		initialize();
		registerWiFiStateReceiver();
	}
	
	private InetAddress getWifiInterfaceIpAddress() throws IOException {
		InetAddress result = null;
		
		if (!RUNNING_ON_EMULATOR) {
			WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = wm.getConnectionInfo();
			if (info == null) {
				throw new IOException("WiFi is not connected");
			}
			
			byte[] addressBytes = getAddressBytes(info.getIpAddress());
			result = InetAddress.getByAddress(addressBytes);
		}
		else {
			result = InetAddress.getByName("10.0.2.15");
		}
		
		return result;
	}
	
	private byte[] getAddressBytes(int address) {
		final int addressSize = 4;
		byte[] result = new byte[4];
		for (int i = 0; i < addressSize; i++) {
			result[i] = (byte) (address & 0xFF);
			address >>= 8;
		}
		return result;
	}
	
	private void registerWiFiStateReceiver() {
		if (RUNNING_ON_EMULATOR) {
			return;
		}
		Log.i(LOG_TAG, "Registering wi-fi state receiver");
		mWifiStateReceiver = new WiFiStateReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(mWifiStateReceiver, filter);
	}
	
	private void unregisterWiFiStateReceiver() {
		if (RUNNING_ON_EMULATOR) {
			return;
		}
		Log.i(LOG_TAG, "Unregistering wi-fi state receiver");
		unregisterReceiver(mWifiStateReceiver);
	}
	
	private class WiFiStateReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equalsIgnoreCase(
							WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				Log.i(LOG_TAG, "Network state changed, connected " + info.isConnected());
				if (info.isConnected() && !isReady()) {
						initialize();
				}
				else if (!info.isConnected() && isReady()){
					shutdown();
				}
			}
		}
	}
	
	void initialize() {
		try {
			openSocket();
			runWorkerThreads();
			mReady = true;
		}
		catch (IOException e) {
			Log.e(LOG_TAG, "Cannot open socket", e);
			shutdown();
		}
		catch (RejectedExecutionException e) {
			Log.e(LOG_TAG, "Unable to start worker threads", e);
			shutdown();
		}
	}
	
	void shutdown() {
		Log.i(LOG_TAG, "Stopping worker threads");
		if (mExecutorService != null) {
			mExecutorService.shutdownNow();
			mExecutorService = null;
		}
		Log.i(LOG_TAG, "Closing socket");
		closeSocket();
		mReady = false;
	}
	
	public boolean isReady() {
		return mReady;
	}
	
	private void openSocket() throws IOException {
		Log.i(LOG_TAG, "Creating socket");
		closeSocket();
		mSocket = new DatagramSocket(new InetSocketAddress(
				getWifiInterfaceIpAddress(), Ports.CLIENT_PORT));
		Log.i(LOG_TAG, "Opening socket");
		mSocket.setBroadcast(true);
		// Log.i(LOG_TAG, "Enabling address reusing");
		// mSocket.setReuseAddress(true);
		mSocket.setSoTimeout(SOCKET_TIMEOUT);
		Log.i(LOG_TAG, "Socket is open");
	}
	
	private boolean isSocketOpen() {
		return mSocket != null;
	}
	
	private void checkSocketOpen() throws RuntimeException {
		if (!isSocketOpen()) {
			throw new RuntimeException("Socket is not open");
		}
	}
	
	private void closeSocket() {
		if (mSocket != null) {
			mSocket.close();
			mSocket = null;
		}
	}
	
	private void runWorkerThreads() {
		mExecutorService = Executors.newFixedThreadPool(2);
		Log.i(LOG_TAG, "Starting sender thread");
		mExecutorService.execute(new SenderRunnable());
		Log.i(LOG_TAG, "Starting receiver thread");
		mExecutorService.execute(new ReceiverRunnable());
	}
	
	public long send(ServerIdentity identity, BaseRequestPacket request, Handler handler) {
		checkSocketOpen();
		long packetId = getNextPacketId();
		request.setId(packetId);
		try {
			Task task = new Task(identity, request, handler, getPacketExpirationTimestamp());
			Log.i(LOG_TAG, "Registering " + task);
			mSendTasks.put(task);
		}  
		catch (InterruptedException e) {
			// ignore
		}
		return packetId;
	}
	
	private static long getPacketExpirationTimestamp() {
		return System.currentTimeMillis() + RECEIVE_TIMEOUT;
	}
	
	private void writeToSocket(ServerIdentity identity, BaseRequestPacket request) throws IOException {
		if (mSocket != null) {
			InetAddress address = RUNNING_ON_EMULATOR ? sHostLoopback : identity.getAddress().getAddress();
			if (address == null) {
				Log.e(LOG_TAG, "Recipient address is null");
				return;
			}
			mSendPacket.setAddress(address);
			mSendPacket.setPort(identity.getAddress().getPort());
			serializePacketToSendBuffer(request);
			mSocket.send(mSendPacket);
		}
	}
	
	private Response readFromSocket() throws IOException {
		if (mSocket != null) {
			mSocket.receive(mReceivePacket);
			InetAddress senderAddress = mReceivePacket.getAddress();
			return new Response(new InetSocketAddress(senderAddress, mReceivePacket.getPort()), 
					deserializePacketFromReceiveBuffer());
		}
		else {
			return null;
		}
	}
	
	private void serializePacketToSendBuffer(BaseRequestPacket packet) throws IOException {
		PacketHelper.serializePacket(packet, mSendPacket);
	}
	
	private BaseResponsePacket deserializePacketFromReceiveBuffer() throws IOException {
		BasePacket p = PacketHelper.deserializePacket(mReceivePacket);
		if (!(p instanceof BaseResponsePacket)) {
			throw new IOException("Packet " + p.getClass().getName() + " is not instance of response packet");
		}
		return (BaseResponsePacket) p;
	}
	
	@Override
	public void onDestroy() {
		Log.i(LOG_TAG, "Destroying service");
		unregisterWiFiStateReceiver();
		shutdown();
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(LOG_TAG, "Binding request processed");
		return new NetworkServiceBinder();
	}
	
	public class NetworkServiceBinder extends Binder {
		
		public NetworkService getService() {
			return NetworkService.this;
		}
	}
	
	private long getNextPacketId() {
		return mNextPacketId++;
	}
	
	private void notifyHandler(Handler h, int code, BaseRequestPacket packet) {
		Message msg = Message.obtain(h, code, packet);
		Bundle extra = new Bundle();
		extra.putSerializable(EXTRA_REQUEST_PACKET, packet);
		msg.setData(extra);
		if (!h.sendMessage(msg)) {
			Log.w(LOG_TAG, "Message not sent");
		}
	}
	
	private void notifyHandler(Handler h, int code, BaseRequestPacket request, 
			BaseResponsePacket response, InetSocketAddress sender) {
		Message msg = Message.obtain(h, code, request);
		Bundle extra = new Bundle();
		extra.putSerializable(EXTRA_ADDRESS, sender);
		extra.putSerializable(EXTRA_REQUEST_PACKET, request);
		extra.putSerializable(EXTRA_RESPONSE_PACKET, response);
		msg.setData(extra);
		if (!h.sendMessage(msg)) {
			Log.w(LOG_TAG, "Message not sent");
		}
	}
	
	private class SenderRunnable implements Runnable {
		
		@Override
		public void run() {
			Log.i(LOG_TAG, "Sender thread started");
			try {
				while (true) {
					if (!isSocketOpen()) {
						break;
					}
					Task task = mSendTasks.take();
					boolean resending = task.retryCount < MAX_RETRY;
					boolean sent = trySend(task);
					if (!sent) {
						Log.e(LOG_TAG, "Failed to send " + task);
						notifyHandler(task.handler, 
								resending ? RESULT_NO_REPLY : RESULT_NOT_SENT, 
								task.requestPacket);
					}
				}
			}
			catch (InterruptedException e) {
				
			}
			Log.i(LOG_TAG, "Sender thread stopped");
		}
		
		private boolean trySend(Task task) {
			boolean sent = false;
			while (task.retryCount-- > 0) {
				try {
					synchronized (mReceiveTasks) {
						writeToSocket(task.serverIdentity, task.requestPacket);
						if (task.requestPacket.waitForResponse()) {
							task.expiringTimestamp = System.currentTimeMillis() + RECEIVE_TIMEOUT;
								mReceiveTasks.add(task);
							}
					}
					Log.i(LOG_TAG, task + " sent");
					sent = true;
					break;
				}
				catch (IOException e) {
					Log.e(LOG_TAG, "Sending " + task + "failed, " + 
							task.retryCount + " tries left", e);
				}
			}
			return sent;
		}
	}
	
	private class ReceiverRunnable implements Runnable {
		
		@Override
		public void run() {
			Log.i(LOG_TAG, "Receiver thread started");
			while (true) {
				if (!isSocketOpen()) {
					break;
				}
				Response response = null;
				try {
					try {
						response = readFromSocket();
					}
					catch (SocketTimeoutException e) {
						
					}
					synchronized (mReceiveTasks) {
						if (!mReceiveTasks.isEmpty()) {
							updateTaskList(response);
						}
						else if (response != null) {
							Log.i(LOG_TAG, "Dropping unexpected packet " + response.packet +
									"from " + response.address);
						}
					}
				}
				catch (InterruptedException e) {
					break;
				}
				catch (IOException e) {
					if (Thread.interrupted()) {
						break;
					}
					Log.e(LOG_TAG, "Failed to read packet", e);
				}
			}
			Log.i(LOG_TAG, "Receiver thread stopped");
		}
		
		private void updateTaskList(Response response) throws InterruptedException {
			boolean responseProcessed = false;
			for (Iterator<Task> iterator = mReceiveTasks.iterator(); iterator.hasNext(); ) {
				Task task = iterator.next();
				if (response != null && response.packet.getId() == task.requestPacket.getId())
				{
					Log.i(LOG_TAG, "Got response for packet " + task.requestPacket);
					notifyHandler(task.handler, RESULT_RESPONSE, task.requestPacket, 
							response.packet, response.address);
					task.haveResponse = true;
					if (!task.requestPacket.waitForMultipleResponses()) {
						iterator.remove();
					}
					responseProcessed = true;
				}
				else if (task.expiringTimestamp < System.currentTimeMillis()) {
					Log.i(LOG_TAG, task + " expired, removing from task list");
					if (task.haveResponse) {
						Log.i(LOG_TAG, task + " have responses");
						notifyHandler(task.handler, RESULT_SUCCSESS, task.requestPacket);
					}
					else if (task.retryCount == 0) {
						notifyHandler(task.handler, RESULT_NO_REPLY, task.requestPacket);
						Log.i(LOG_TAG, task + " have no responses");
					}
					else {
						Log.i(LOG_TAG, "Resending paket for task " + task);
						mSendTasks.put(task);
					}
					iterator.remove();
				}
			}
			if (response != null && !responseProcessed) {
				Log.i(LOG_TAG, "Dropping unexpected packet " + response.packet + 
						" from " + response.address);
			}
		}
	}
	
	private static class Task {
		
		Task(ServerIdentity si, BaseRequestPacket brp, Handler h, long et) {
			serverIdentity = si;
			requestPacket = brp;
			retryCount = MAX_RETRY;
			handler = h;
			expiringTimestamp = et;
			haveResponse = false;
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "[request = " + requestPacket + ", server = " + serverIdentity + "]";
		}
		
		ServerIdentity serverIdentity;
		BaseRequestPacket requestPacket;
		int retryCount;
		Handler handler;
		long expiringTimestamp;
		boolean haveResponse;
	}
	
	private static class Response {
			
		Response(InetSocketAddress sa, BaseResponsePacket brp) {
			address = sa;
			packet = brp;
		}
		
		InetSocketAddress address;
		BaseResponsePacket packet;
	}
	
	private DatagramSocket mSocket;
	private final DatagramPacket mReceivePacket = new DatagramPacket(
			new byte[BasePacket.MAX_PACKET_SIZE], BasePacket.MAX_PACKET_SIZE);
	private final DatagramPacket mSendPacket = new DatagramPacket(
			new byte[BasePacket.MAX_PACKET_SIZE], BasePacket.MAX_PACKET_SIZE);
	private final List<Task> mReceiveTasks = new LinkedList<Task>();
	private final BlockingQueue<Task> mSendTasks = new LinkedBlockingQueue<Task>();
	private long mNextPacketId = 0;
	private ExecutorService mExecutorService;
	private boolean mReady;
	private WiFiStateReceiver mWifiStateReceiver;
	private static final boolean RUNNING_ON_EMULATOR = true;
	
	private static final String LOG_TAG = "NetworkService";
	private static final int SOCKET_TIMEOUT = 2500;
	private static final int RECEIVE_TIMEOUT = 2500;
	private static final int MAX_RETRY = 3;
	
	private static final InetAddress sHostLoopback;
	static {
		InetAddress tmp = null;
		if (RUNNING_ON_EMULATOR) {
			try {
				tmp = InetAddress.getByName("10.0.2.2");
			}
			catch (UnknownHostException e) {
				Log.e(LOG_TAG, "Cannot get host loopback", e);
			}
		}
		sHostLoopback = tmp;
	}
	
	public static final String 
			EXTRA_ADDRESS = "address",
			EXTRA_REQUEST_PACKET = "requestPacket",
			EXTRA_RESPONSE_PACKET = "responsePacket";
	
	public static final int 
			RESULT_NOT_SENT = 0,
			RESULT_NO_REPLY = 1,
			RESULT_RESPONSE = 2,
			RESULT_SUCCSESS = 3;
}
