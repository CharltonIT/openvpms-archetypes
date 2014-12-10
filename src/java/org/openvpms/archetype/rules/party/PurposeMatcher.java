/*
 * Version: 1.0
 *
 * The contents of this file are subject to the OpenVPMS License Version
 * 1.0 (the 'License'); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.openvpms.org/license/
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * Copyright 2014 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.archetype.rules.party;

import org.openvpms.component.business.domain.im.lookup.Lookup;
import org.openvpms.component.business.domain.im.party.Contact;
import org.openvpms.component.business.service.archetype.IArchetypeService;

import java.util.*;

/**
 * An {@link ContactMatcher} that matches contacts on archetype and purpose.
 *
 * @author Tim Anderson
 */
public class PurposeMatcher extends ContactMatcher {

    /**
     * The purpose to match on.
     */
    private final List<String> purposes;

    /**
     * If {@code true} the contact must contain the purpose to be returned
     */
    private final boolean exact;

    /**
     * Constructs a {@link PurposeMatcher} where the contact must have the specified purpose to be considered a match.
     *
     * @param shortName the contact archetype short name
     * @param purpose   the purpose
     * @param service   the archetype service
     */
    public PurposeMatcher(String shortName, String purpose, IArchetypeService service) {
        this(shortName, purpose, true, service);
    }

    /**
     * Constructs a {@link PurposeMatcher}.
     *
     * @param shortName the contact archetype short name
     * @param purpose   the purpose. May be {@code null}
     * @param exact     if {@code true} the contact must contain the purpose in order to be considered a match
     * @param service   the archetype service
     */
    public PurposeMatcher(String shortName, String purpose, boolean exact, IArchetypeService service) {
        super(shortName, service);
        this.purposes = new ArrayList<String>();
        if (purpose != null){
            purposes.add(purpose);
        }
        this.exact = exact;
    }

    /**
     * Adds another purpose to the {@link PurposeMatcher}.
     * @param purpose
     */
    public void addPurpose(String purpose){
        if (purpose != null){
            purposes.add(purpose);
        }
    }

    /**
     * Removes a contact purpose from the {@Link PurposeMatcher}
     * @param purpose a string
     * @return {@code True} if successful
     */
    public boolean removePurpose(String purpose){
        if (purpose != null){
           return purposes.remove(purpose);
        }else{
            return false;
        }

    }

    /**
     * Determines if a contact matches the criteria.
     *
     * @param contact the contact
     * @return {@code true} if the contact is an exact match; otherwise {@code false}
     */
    @Override
    public boolean matches(Contact contact) {
        return super.matches(contact) && matchesPurpose(contact);
    }

    /**
     * Determines if a contact matches the criteria.
     *
     * @param contact the contact
     * @return {@code true} if the contact is an exact match; otherwise {@code false}
     */
    protected boolean matchesPurpose(Contact contact) {
        boolean best = false;
        boolean preferred = isPreferred(contact);
        if(purposes.size() != 0){
            if(exact){
                int priority = purposes.size();
                int i = 0;
                if(preferred){i = 1;}
                for(String purpose : purposes) {
                    if(hasContactPurpose(contact, purpose)){
                        i++;
                    }
                }
                if(i>priority && preferred){ //has matched all purposes and is preferred
                    setMatch(0,contact);
                    best=true;
                }else if(i==priority && !preferred){ //has matched all purposes and is not preferred
                    setMatch(1,contact);
                }
            }else{
                int priority = purposes.size();
                int i = 0;
                if(preferred){i = 1;}
                for(String purpose : purposes) {
                    if(hasContactPurpose(contact, purpose)){
                        i++;
                    }
                }
                if(i>priority && preferred){ //has matched all purposes and is preferred
                    setMatch(0,contact);
                    best=true;
                }else if(i==priority && !preferred){ //has matched all purposes and is not preferred
                    setMatch(1,contact);
                }else if(i>0 && !preferred){ // has match some purposes and is not preferred.
                    setMatch(priority-i,contact);
                }else if(i>1 && preferred){ // has matched some purposes and is not preferred.
                    setMatch(priority-i,contact);
                }else if(i==0 && !preferred){
                    setMatch(priority+2,contact); //matched no purposes and is not preferred
                }else if(i==1 && preferred){
                    setMatch(priority+1,contact); //matched no purposes and is preferred
                }
            }
        }else{
            if (preferred) {
                setMatch(1, contact);
                best = true;
            } else {
                setMatch(2, contact);
            }
        }
        return best;
    }
    /**
     * Determines if a contact has a particular purpose.
     *
     * @param contact the contact
     * @param purpose the contact purpose
     * @return {@code true} if the contact has the specified purpose,
     *         otherwise {@code false}
     */
    private boolean hasContactPurpose(Contact contact, String purpose) {
        for (Lookup classification : contact.getClassifications()) {
            if (classification.getCode().equals(purpose)) {
                return true;
            }
        }
        return false;
    }
}
