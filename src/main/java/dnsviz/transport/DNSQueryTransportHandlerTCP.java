/*
 * This file is a part of DNSViz, a tool suite for DNS/DNSSEC monitoring,
 * analysis, and visualization.
 * Created by Casey Deccio (casey@deccio.net)
 *
 * Copyright 2016 VeriSign, Inc.
 *
 * DNSViz is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DNSViz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with DNSViz.  If not, see <http://www.gnu.org/licenses/>.
 */

package dnsviz.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;

public class DNSQueryTransportHandlerTCP extends DNSQueryTransportHandler {
	protected boolean lengthKnown = false;

	public DNSQueryTransportHandlerTCP(byte[] req, InetAddress dst, int dport, InetAddress src, int sport, long timeout) {
		super(req, dst, dport, src, sport, timeout);
	}

	public int getInitialSelectionOp() {
		return SelectionKey.OP_CONNECT;
	}

	public int getStartOfReqPayload() {
		return 2;
	}

	protected void initRequestBuffer(byte[] req) {
		byte b1 = (byte)((req.length >> 8) & 0xff);
		byte b2 = (byte)(req.length & 0xff);
		this.req = ByteBuffer.allocate(req.length + 2);
		this.req.clear();
		this.req.put(b1);
		this.req.put(b2);
		this.req.put(req);
		this.req.flip();
	}

	protected void createSocket() throws IOException {
		channel = SocketChannel.open();
	}

	protected void connect() throws IOException {
		try {
			((SocketChannel)channel).connect(new InetSocketAddress(dst, dport));
		} catch (IOException | RuntimeException e) {
			throw e;
		}
	}

	public boolean finishConnect() throws IOException {
		try {
			return ((SocketChannel)channel).finishConnect();
		} catch (IOException ex) {
			setError(ex);
			cleanup();
			return true;
		}
	}

	public boolean doRead() throws IOException {
		int bytesRead;
		ByteBuffer buf;

		try {
			bytesRead = ((ReadableByteChannel)channel).read(res);
		} catch (IOException e) {
			setError(e);
			cleanup();
			return true;
		} catch (RuntimeException e) {
			throw e;
		}

		if (bytesRead < 1) {
			setError(Errno.ECONNRESET);
			cleanup();
			return true;
		}

		if (!lengthKnown && res.position() > 1) {
			res.limit(res.position());
			byte b1 = res.get(0);
			byte b2 = res.get(1);
			int len = ((b1 & 0xff) << 8) | (b2 & 0xff);
			buf = ByteBuffer.allocate(len);
			buf.clear();
			res.rewind().position(2);
			buf.put(res);
			res = buf;
			lengthKnown = true;
		}
		if (!res.hasRemaining()) {
			cleanup();
			return true;
		}
		return false;
	}

	protected InetAddress getLocalAddress() {
		InetAddress localAddress;
		try {
			final DatagramChannel datagramChannel = DatagramChannel.open();
			try {
				datagramChannel.connect(new InetSocketAddress(dst, dport));
				localAddress = ((InetSocketAddress)datagramChannel.getLocalAddress()).getAddress();
			} catch (IOException | RuntimeException e) {
				throw e;
			}
			datagramChannel.close();
			return localAddress;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Ensures that `src` has an address by populating it with
	 * `getLocalAddress()` if `src` doesn't have a valid address.
	 */
	protected void checkSource() {
		if (src == null || src.isAnyLocalAddress()) {
			src = getLocalAddress();
		}
	}
}
