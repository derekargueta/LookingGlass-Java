
package dnsviz.serializers;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import dnsviz.transport.DNSQueryTransportHandler;
import dnsviz.transport.DNSQueryTransportHandlerTCP;
import dnsviz.transport.DNSQueryTransportHandlerUDP;
import dnsviz.lookingglass.Version;
import dnsviz.util.Base64Decoder;

import static dnsviz.lookingglass.Constants.*;


public class DNSQueryTransportHandlersDeserializer {

  private String jsonString;

  public DNSQueryTransportHandlersDeserializer(String jsonString) {
    this.jsonString = jsonString;
  }

  private void checkVersion(JSONObject obj) throws JSONException {
    Version version = new Version(obj.getDouble(kVersion));
    if (!version.isValid()) {
      throw new JSONException(VERSION_ERROR_MSG);
    }
  }

  private DNSQueryTransportHandler getDNSQueryTransportHandler(String req, String dst, int dport, String src, int sport, long timeout, boolean tcp) throws UnknownHostException {
    Base64Decoder d = new Base64Decoder();
    byte[] byteReq = d.decode(req.getBytes());
    InetAddress srcAddr = null;
    InetAddress dstAddr = null;
    if (dst != null) {
      dstAddr = InetAddress.getByName(dst);
    }
    if (src != null) {
      srcAddr = InetAddress.getByName(src);
    }
    if (tcp) {
      return new DNSQueryTransportHandlerTCP(byteReq, dstAddr, dport, srcAddr, sport, timeout);
    } else {
      return new DNSQueryTransportHandlerUDP(byteReq, dstAddr, dport, srcAddr, sport, timeout);
    }
  }

  public DNSQueryTransportHandler[] getDecodedHandlers() throws JSONException, UnknownHostException {

    JSONObject obj = new JSONObject(this.jsonString);
    this.checkVersion(obj);

    JSONArray requests = obj.getJSONArray("requests");
    DNSQueryTransportHandler[] ret = new DNSQueryTransportHandler[requests.length()];
    for (int i = 0; i < requests.length(); i++) {
      JSONObject reqObj = requests.getJSONObject(i);
      String src = reqObj.has(kSource) ? reqObj.getString(kSource) : null;
      int sport = reqObj.has("sport") ? reqObj.getInt("sport") : 0;
      String request = reqObj.getString("req");
      String destination = reqObj.getString("dst");
      int dport = reqObj.getInt("dport");
      long timeout = reqObj.getLong("timeout");
      boolean isTCP = reqObj.getBoolean("tcp");
      ret[i] = getDNSQueryTransportHandler(request, destination, dport, src, sport, timeout, isTCP);
    }
    return ret;
  }
}