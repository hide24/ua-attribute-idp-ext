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

package uk.org.ukfederation.uaattribute.resolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.opensaml.xml.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.ukfederation.uaattribute.authn.UserAgentPrincipal;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.BaseDataConnector;
import edu.internet2.middleware.shibboleth.common.session.Session;
import edu.internet2.middleware.shibboleth.idp.util.IPRange;

/**
 * A data connector that generates certain attributes/values based on the IP address of the user's user agent at the
 * time of authentication.
 */
public class UserAgentAttributeMapDataConnector extends BaseDataConnector {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(UserAgentAttributeMapDataConnector.class);

    /** Map from IP ranges to the attribute name/value pairs that they trigger. */
    private List<Pair<Pair<String, String>, Pair<String, String>>> attributeMappings;

    /**
     * Sets the mappings from IP ranges to attributes/values.
     * 
     * @param mappings mappings from IP ranges to attributes/values
     */
    public void setAttributeMappings(List<Pair<Pair<String, String>, Pair<String, String>>> mappings) {
        attributeMappings = mappings;
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        final UserAgentPrincipal uaPrincpal = getUserAgentPrincipal(resolutionContext);
        if (uaPrincpal == null) {
            return Collections.emptyMap();
        }

        byte[] uaAddress = uaPrincpal.getUserAgentAddress();
        HashMap<String, BaseAttribute> mappedAttributes = new HashMap<String, BaseAttribute>();
        String type;
        IPRange ipRange;
        for (Pair<Pair<String, String>, Pair<String, String>> mapping : attributeMappings) {
            type = mapping.getFirst().getFirst();
            if (type.equals("CIDR")) {
                ipRange = IPRange.parseCIDRBlock(mapping.getFirst().getSecond());
                if (ipRange.contains(uaAddress)) {
                    addAttributeValue(mapping.getSecond(), mappedAttributes);
                }
            } else {
                String cidrAttributeId = mapping.getFirst().getSecond();
                List<String> values = getAttributeValue(resolutionContext, cidrAttributeId);
                for (String ipRangeString : values) {
                    log.debug("inspect IP range :" + ipRangeString.toString());
                    ipRange = IPRange.parseCIDRBlock(ipRangeString);
                    if (ipRange.contains(uaAddress)) {
                        log.debug("uaAddress matches :" + ipRangeString.toString());
                        addAttributeValue(mapping.getSecond(), mappedAttributes);
                    }
                }
            }
        }

        return mappedAttributes;
    }

    /**
     * Extracts the {@link UserAgentPrincipal} from the given resolution context.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return the extract principal or null if no such principal is associated with the current user
     */
    private UserAgentPrincipal getUserAgentPrincipal(ShibbolethResolutionContext resolutionContext) {
        final Session userSession = resolutionContext.getAttributeRequestContext().getUserSession();
        if (userSession == null) {
            log.debug("No user session available, unable to extract user agent information");
            return null;
        }

        final Set<UserAgentPrincipal> userAgentPrincipals =
                userSession.getSubject().getPrincipals(UserAgentPrincipal.class);
        if (userAgentPrincipals == null || userAgentPrincipals.isEmpty()) {
            log.debug("No user agent information information associated with user session");
            return null;
        }
        if (userAgentPrincipals.size() > 1) {
            log.debug("Multiple user agent principals found, onl the first will be used.");
        }

        return userAgentPrincipals.iterator().next();
    }

    /**
     * Adds the specified value for the specified attribute to the {@link BasicAttribute} found in the already mapped
     * attributes. If no {@link BasicAttribute} with the given ID exists, it is created and added to the mapped
     * attributes.
     * 
     * @param attributeDescriptor name/value pair of the attribute
     * @param mappedAttributes currently mapped attributes
     */
    private void addAttributeValue(Pair<String, String> attributeDescriptor,
            HashMap<String, BaseAttribute> mappedAttributes) {
        BaseAttribute attribute = mappedAttributes.get(attributeDescriptor.getFirst());
        if (attribute == null) {
            attribute = new BasicAttribute<String>(attributeDescriptor.getFirst());
            mappedAttributes.put(attributeDescriptor.getFirst(), attribute);
        }

        if (!attribute.getValues().contains(attributeDescriptor.getSecond())) {
            attribute.getValues().add(attributeDescriptor.getSecond());
        }
    }

    /**
     * Get attribute value from resolution context.
     * If no {@link BasicAttribute} with the given ID exists, it is empty list will be return.
     * 
     * @param resolutionContext current resolution context
     * @param attributeId attribute ID that you want values
     * 
     * @return list of values
     */
    private List<String> getAttributeValue(ShibbolethResolutionContext resolutionContext, String attributeId) {
        log.debug("Get attribute value of (" + attributeId.toString() + ")");
        List<String> values = new ArrayList<String>();
        try {
            BaseAttribute attribute = resolutionContext.getResolvedAttributeDefinitions().get(attributeId).resolve(resolutionContext);
            if (attribute != null) {
                for (Object value: attribute.getValues()) {
                    if (value != null && !value.toString().trim().equals("")) {
                        values.add(value.toString().trim());
                        log.debug("Detect value:" + value.toString().trim());
                    }
                }
            }
        } catch(AttributeResolutionException e) {
            
        }
//        BaseAttribute attribute = attributes.get(attributeId).get(attributeId);
        
        return values;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }
}
