/*
 *  Version: 1.0
 *
 *  The contents of this file are subject to the OpenVPMS License Version
 *  1.0 (the 'License'); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.openvpms.org/license/
 *
 *  Software distributed under the License is distributed on an 'AS IS' basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  Copyright 2007 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */

package org.openvpms.archetype.component.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Abstract implementation of the {@link Processor} interface.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public abstract class AbstractProcessor<Action, Type, Event>
        implements Processor<Action, Type, Event> {

    /**
     * The listeners. Listen to all events.
     */
    private final List<ProcessorListener<Event>> listeners;

    /**
     * The listeners, keyed on action type.
     */
    private final Map<Action, List<ProcessorListener<Event>>> actionListeners;


    /**
     * Creates a new <tt>AbstractProcessor</tt>.
     */
    public AbstractProcessor() {
        listeners = new ArrayList<ProcessorListener<Event>>();
        actionListeners = new HashMap<Action, List<ProcessorListener<Event>>>();
    }

    /**
     * Registers a listener for all events.
     *
     * @param listener the listener to add
     */
    public void addListener(ProcessorListener<Event> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(ProcessorListener<Event> listener) {
        listeners.remove(listener);
    }

    /**
     * Registers a listener for a specific action.
     *
     * @param action   the action to register the listener for
     * @param listener the listener to add
     */
    public void addListener(Action action, ProcessorListener<Event> listener) {
        List<ProcessorListener<Event>> list = actionListeners.get(action);
        if (list == null) {
            list = new ArrayList<ProcessorListener<Event>>();
            actionListeners.put(action, list);
        }
        list.add(listener);
    }

    /**
     * Removes a listener for a specific action.
     *
     * @param action   the action to remove the listener for
     * @param listener the listener to remove
     */
    public void removeListener(Action action,
                               ProcessorListener<Event> listener) {
        List<ProcessorListener<Event>> list = actionListeners.get(action);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Notifies listeners of an event.
     *
     * @param action the action
     * @param event  the event
     */
    protected void notifyListeners(Action action, Event event) {
        notifyListeners(listeners, event);
        List<ProcessorListener<Event>> list = actionListeners.get(action);
        if (list != null) {
            notifyListeners(list, event);
        }
    }

    /**
     * Notifies listeners of an event.
     *
     * @param list  the listeners to notify
     * @param event the event
     */
    private void notifyListeners(List<ProcessorListener<Event>> list,
                                 Event event) {
        for (ProcessorListener<Event> listener : list) {
            listener.process(event);
        }
    }

}