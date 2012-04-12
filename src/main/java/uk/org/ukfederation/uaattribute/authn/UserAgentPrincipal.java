/*
 * This file to You under the Apache  License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ukfederation.uaattribute.authn;

import java.io.Serializable;
import java.security.Principal;

import org.apache.commons.codec.binary.Hex;

/** A principal that identifies the user by the IP address of their user agent. */
public class UserAgentPrincipal implements Principal, Serializable {

    /** Serial version UID. */
    private static final long serialVersionUID = -2038493216591713099L;

    /** IP address of the user agent. */
    private byte[] address;

    /**
     * Constructor.
     * 
     * @param userAgentAddress IP address of the user agent, must not be null or empty
     */
    public UserAgentPrincipal(final byte[] userAgentAddress) {
        address = userAgentAddress;
    }

    /** Gets the address of the user agent. */
    public byte[] getUserAgentAddress() {
        return address;
    }

    /** {@inheritDoc} */
    public String getName() {
        return Hex.encodeHexString(address);
    }
}
