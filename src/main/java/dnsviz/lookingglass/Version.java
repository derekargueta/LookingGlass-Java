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


/**
 * Object representation of a DNSViz version.
 */
public class Version {

  // components of a version
  int major;
  int minor;

  public Version(double version) {
    String[] versionParts = Double.toString(version).split("\\.");
    this.major = Integer.parseInt(versionParts[0]);
    this.minor = Integer.parseInt(versionParts[1]);
  }

  /**
   * @return if the version is valid
   */
  public boolean isValid() {
    return this.major == 1 && this.minor == 0;
  }
}
