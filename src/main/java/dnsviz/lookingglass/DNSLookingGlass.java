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

package dnsviz.lookingglass;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import dnsviz.serializers.DNSQueryTransportHandlersDeserializer;

import dnsviz.transport.DNSQueryTransportHandler;
import dnsviz.transport.DNSQueryTransportHandlerTCP;
import dnsviz.transport.DNSQueryTransportHandlerUDP;
import dnsviz.transport.DNSQueryTransportManager;
import dnsviz.util.Base64Decoder;
import dnsviz.util.Base64Encoder;
import dnsviz.websocket.WebSocketClient;

import static dnsviz.lookingglass.Constants.*;

public class DNSLookingGlass {
	
	private WebSocketClient websocket;

	public DNSLookingGlass() {
	
	}

	public DNSLookingGlass(WebSocketClient websocket) {
		this.websocket = websocket;
	}

	public void initialize() throws IOException {
		this.interact(this.websocket);
	}

	protected JSONObject getEncodedResponses(DNSQueryTransportHandler[] qths) throws JSONException {
		JSONObject ret;

		JSONArray responses = new JSONArray();
		for (int i = 0; i < qths.length; i++) {
			JSONObject response = new JSONObject();
			response.put("res", qths[i].getEncodedResponse());
			if (qths[i].getError() != null) {
				response.put("err", qths[i].getError());
				if (qths[i].getErrno() != null) {
					response.put("errno", qths[i].getErrno());
				}
			}
			if (qths[i].getSource() != null) {
				response.put(kSource, qths[i].getSource().getHostAddress());
			} else {
				response.put(kSource, (String)null);
			}
			if (qths[i].getSPort() != 0) {
				response.put("sport", qths[i].getSPort());
			} else {
				response.put("sport", (String)null);
			}
			response.put("time_elapsed", qths[i].timeElapsed());
			responses.put(response);
		}

		ret = new JSONObject();
		ret.put(kVersion, VERSION);
		ret.put("responses", responses);
		return ret;
	}

	public void executeQueries(DNSQueryTransportHandler[] qths) throws IOException {
		DNSQueryTransportManager qtm = new DNSQueryTransportManager();
		qtm.query(qths);
		for (int i = 0; i < qths.length; i++) {
			qths[i].finalize();
		}
	}

	/**
	 * Runs a loop of reading from the server socket, running the query, and
	 * sending back the response.
	 *
	 * @param ws - the WebSocket that is being interacted with
	 */
	public void interact(WebSocketClient ws) throws IOException {
		byte[] input;
		while ((input = ws.read()).length > 0) {
			ws.write(run(new String(input)).getBytes());
		}
	}

	public String run(String json) {
		try {
			DNSQueryTransportHandlersDeserializer deserializer = new DNSQueryTransportHandlersDeserializer(json);
			DNSQueryTransportHandler[] qths = deserializer.getDecodedHandlers();
			executeQueries(qths);
			return getEncodedResponses(qths).toString();
		} catch (Exception ex) {
			JSONObject ret = new JSONObject();
			try {
				ret.put(kVersion, VERSION);
				ret.put("error", getErrorTrace(ex));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return ret.toString();
		}
	}

	protected String getErrorTrace(Exception err) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		err.printStackTrace(pw);
		return sw.toString();
	}

	public static void main(String [] args) throws IOException {
		WebSocketClient ws = new WebSocketClient(args[0], Integer.parseInt(args[1]), args[2], args[3]);
		DNSLookingGlass lg = new DNSLookingGlass();
		lg.interact(ws);
	}
}
