package src.pcmonitor.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;

public final class PacketHelper {
	
	private PacketHelper() {
		
	}
	
	public static void serializePacket(BasePacket request, DatagramPacket buffer) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(request);
			oos.flush();
			buffer.setData(baos.toByteArray());
		}
		finally {
			closeStream(oos);
			closeStream(baos);
		}
	}
	
	public static BasePacket deserializePacket(DatagramPacket buffer) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer.getData());
		ObjectInputStream ois = null;
		BasePacket result = null;
		try {
			ois = new ObjectInputStream(bais);
			Object o = ois.readObject();
			if (!(o instanceof BasePacket)) {
				throw new IOException("Received object is not instance of packet");
			}
			result = (BasePacket) o;
		}
		catch (ClassNotFoundException e) {
			throw new IOException("Cannot deserialize received packet, no such class");
		}
		finally {
			closeStream(ois);
			closeStream(bais);
		}
		return result;
	}
	
	private static void closeStream(Closeable c) throws IOException {
		if (c != null) {
			c.close();
		}
	}
}
