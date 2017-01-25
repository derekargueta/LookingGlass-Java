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
import java.lang.Void;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.BindException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.Random;

import dnsviz.util.Base64Encoder;
import dnsviz.util.Base64Decoder;

public abstract class DNSQueryTransportHandler {
	private final static int MAX_PORT_BIND_ATTEMPTS = 10;

	protected ByteBuffer req = null;
	protected ByteBuffer res = null;
	protected String err = null;
	protected String errno = null;

	protected InetAddress dst = null;
	protected int dport = 0;
	protected InetAddress src = null;
	protected int sport = 0;

	protected NetworkChannel channel = null;

	protected long timeout = 0;
	protected long expiration = 0;
	protected long startTime = 0;
	protected long endTime = 0;

	protected DNSQueryTransportHandler(byte[] req, InetAddress dst, int dport, InetAddress src, int sport, long timeout) {
		this.dst = dst;
		this.dport = dport;
		this.src = src;
		this.sport = sport;

		this.timeout = timeout;

		initRequestBuffer(req);
	}

	public abstract int getInitialSelectionOp();

	public abstract int getStartOfReqPayload();

	public NetworkChannel getChannel() {
		return channel;
	}

	public long getExpiration() {
		return expiration;
	}

	public boolean hasError() {
		return err != null;
	}

	public void setError(IOException ex) throws IOException {
		if (ex instanceof SocketException) {
			String m = ex.getMessage();
			if (ex instanceof ConnectException) {
				if (m.contains("timed out")) {
					err = "TIMEOUT";
				} else if (m.contains("refused")) {
					err = "NETWORK_ERROR";
					errno = Errno.getName(Errno.ECONNREFUSED);
				}
			} else if (ex instanceof BindException) {
				if (m.contains("an't assign requested address")) {
					err = "NETWORK_ERROR";
					errno = Errno.getName(Errno.EADDRNOTAVAIL);
				} else if (m.contains("ddress already in use")) {
					err = "NETWORK_ERROR";
					errno = Errno.getName(Errno.EADDRINUSE);
				}
			} else if (ex instanceof NoRouteToHostException) {
				err = "NETWORK_ERROR";
				errno = Errno.getName(Errno.EHOSTUNREACH);
			} else if (ex instanceof PortUnreachableException) {
				err = "NETWORK_ERROR";
				errno = Errno.getName(Errno.ECONNREFUSED);
			} else if (m.contains("ermission denied")) {
				err = "NETWORK_ERROR";
				errno = Errno.getName(Errno.EACCES);
			}
		}

		/* if we weren't able to identify the error, then throw it */
		if (err == null) {
			throw ex;
		}
	}

	public void setError(int code) {
		err = "NETWORK_ERROR";
		errno = Errno.getName(code);
	}

	public void setError(String name) {
		err = name;
	}

	public String getError() {
		return err;
	}

	public String getErrno() {
		return errno;
	}

	public long timeElapsed() {
		return endTime - startTime;
	}

	public long getSPort() {
		return sport;
	}

	public InetAddress getSource() {
		return src;
	}

	protected abstract void initRequestBuffer(byte[] req);

	protected void initResponseBuffer() {
		//TODO start more conservative and dynamically grow if more buffer space is
		//needed
		res = ByteBuffer.allocate(65536);
	}

	protected abstract void createSocket() throws IOException;

	protected void configureSocket() throws IOException {
		((SelectableChannel)channel).configureBlocking(false);
	}

	protected void bindSocket() throws IOException {
		if (sport > 0) {
			try {
				channel.bind(new InetSocketAddress(src, sport));
			} catch (IOException | RuntimeException e) {
				throw e;
			}
		} else {

			int i = 0;
			while (true) {
				int randomPort = new Random().nextInt(64512) + 1024;
				try {
					channel.bind(new InetSocketAddress(src, randomPort));
					break;
				} catch (BindException e) {
					if (++i > MAX_PORT_BIND_ATTEMPTS || !e.getMessage().contains("ddress already in use")) {
						throw e;
					}
				} catch (IOException | RuntimeException e) {
					throw e;
				}
			}
		}
	}

	public void prepare() throws IOException {
		initResponseBuffer();
		try {
			createSocket();
			configureSocket();
			bindSocket();
			setStart();
			connect();
		} catch (IOException ex) {
			setError(ex);
			cleanup();
		}
	}

	protected void setSocketInfo() {
		InetSocketAddress addr;			// IP socket address

		try {
			addr = (InetSocketAddress)channel.getLocalAddress();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (RuntimeException e) {
			throw e;
		}
		src = addr.getAddress();
		sport = addr.getPort();
	}

	protected void setStart() {
		Date date = new Date();
		expiration = date.getTime() + timeout;
		startTime = date.getTime();
	}

	protected abstract void connect() throws IOException;

	protected abstract boolean finishConnect() throws IOException;

	public boolean doWrite() throws IOException {
		try {
			((WritableByteChannel)channel).write(req);
		} catch (IOException e) {
			setError((IOException)e);
			cleanup();
			return true;
		} catch (RuntimeException e) {
			throw e;
		}
		if (!req.hasRemaining()) {
			return true;
		}
		return false;
	}

	public abstract boolean doRead() throws IOException;

	public void doTimeout() {
		err = "TIMEOUT";
		cleanup();
	}

	protected void setEnd() {
		// set end (and start, if necessary) times, as appropriate
		endTime = new Date().getTime();
		if (startTime == 0) {
			startTime = endTime;
		}
	}

	/** 
	 * Closes the socket connection. Doesn't attempt anything if the socket isn't
	 * open.
	 */
	protected void closeSocket() {
		if (channel.isOpen()) {
			try {
				channel.close();
			}	catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void cleanup() {
		setEnd();
		setSocketInfo();
		closeSocket();
	}

	protected abstract void checkSource();

	public void finalize() {
		checkSource();
		if (req != null) {
			req.rewind();
		}
		if (err != null) {
			res = null;
		} else if (res != null) {
			res.rewind();
		}
	}

	/**
	 * @return a Base64 version of the `res` ByteBuffer
	 */
	public String getEncodedResponse() {
		Base64Encoder encoder = new Base64Encoder();

		if (this.res != null) {
			byte[] buf = new byte[this.res.limit()];
			this.res.get(buf);
			return new String(encoder.encode(buf));
		} else {
			return null;
		}
	}
}
