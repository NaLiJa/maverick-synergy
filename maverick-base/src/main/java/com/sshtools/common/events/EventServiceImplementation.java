package com.sshtools.common.events;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sshtools.common.logger.Log;


/**
 * Event Service Implementation singleton, that manages J2SSH Event Listeners,
 * and allows events to be fired.
 *
 * @author david
 */
public class EventServiceImplementation implements EventService {

    /** Singleton */
    private static EventService INSTANCE = new EventServiceImplementation();

	private static boolean got;

	private static StackTraceElement[] gotStack;

    protected Collection<EventListener> globalListeners = new ConcurrentLinkedQueue<EventListener>();
    
    @SuppressWarnings("rawtypes")
    protected List<Class> eventCodeDescriptors = new ArrayList<Class>(Arrays.asList(EventCodes.class));
    boolean processAllEventsOnEventException = false;

    Map<Integer,String> cachedEventNames = new HashMap<Integer,String>();
    
    protected EventServiceImplementation() {
        try {
			registerEventCodeDescriptor(Class.forName("com.sshtools.common.events.EventCodes"));
		} catch (ClassNotFoundException e) {
		}
    }

    /**
     * Allow alternative event service to set.
     *
     * @return EventService
     */
    protected static void setInstance(EventService eventService) {
    	if(got) {
    		StringBuilder trace= new StringBuilder();
    		for(StackTraceElement el : gotStack) {
    			if(trace.length() > 0) {
    				trace.append('\n');
    			}
    			trace.append(el);
    		}
    		throw new IllegalArgumentException(EventServiceImplementation.class +
    				".setInstance() must be called before the first getInstace() which was called from :-\n" + trace.toString());
    	}
        INSTANCE = eventService;
    }


    /**
     * Get the event service instance
     *
     * @return EventService
     */
    public static EventService getInstance() {
    	// Safeguard to enforce setInstance having to be done before getInstance()
    	if(!got) {
    		got = true;
    		gotStack = Thread.currentThread().getStackTrace();
    	}
        return INSTANCE;
    }

    public void registerEventCodeDescriptor(@SuppressWarnings("rawtypes") Class cls) {
    	eventCodeDescriptors.add(cls);
    }

    public String getEventName(Integer id) {

    	if(cachedEventNames.containsKey(id)) {
    		return cachedEventNames.get(id);
    	}
    	
    	for(@SuppressWarnings("rawtypes") Class cls : eventCodeDescriptors) {
    		for(Field f : cls.getFields()) {
    			if((f.getModifiers() & (Modifier.FINAL & Modifier.STATIC)) == (Modifier.FINAL & Modifier.STATIC)) {
    				if(f.getType().isAssignableFrom(int.class)) {
    					try {
    						int val = (Integer) f.get(null);
							if(val==id) {
								cachedEventNames.put(id, f.getName());
								return f.getName();
							}
						} catch (IllegalArgumentException e) {
						} catch (IllegalAccessException e) {
						}
    				}
    			}
    		}
    	}

    	return Integer.toHexString(id);
    }


    /**
     * Send an SSH Event to each registered listener
     */
    public void fireEvent(final Event evt) {
        if (evt == null) {
            return;
        }

        if(Log.isDebugEnabled()) {
        	Log.debug("Firing {} success={} {}", getEventName(evt.getId()), evt.getState() ? "true" : "false", evt.logAttributes());
        }
        
        Object obj = (Object) evt.getAttribute(EventCodes.ATTRIBUTE_CONNECTION);
        if (obj!=null && obj instanceof EventTrigger)
        {
            ((EventTrigger)obj).fireEvent(evt);
        }

        EventException lastException = null;
        // Process global listeners
        for(EventListener mListener : globalListeners) {
        	try {
        		mListener.processEvent(evt);
        	} catch(Throwable t) {
        		if(t instanceof EventException) {
        			lastException = (EventException)t;
        			if(!processAllEventsOnEventException) {
        				throw lastException;
        			}
        		} else {
        			if(Log.isWarnEnabled()) {
        				Log.warn("Caught exception from event listener", t);
        			}
        		}
        	}
        }

        if(processAllEventsOnEventException && lastException!=null) {
        	throw lastException;
        }
    }

	public void setProcessAllEventsOnEventException(boolean processAllEventsOnEventException) {
    	this.processAllEventsOnEventException = processAllEventsOnEventException;
    }

	public void addListener(EventListener listener) {
		globalListeners.add(listener);

	}

	public void removeListener(EventListener listener) {
		globalListeners.remove(listener);
	}

}
