package com.example;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

public class MyApplicationEventListener
implements ApplicationEventListener {
private volatile int requestCnt = 0;
private volatile int requestGET = 0;

@Override
public void onEvent(ApplicationEvent event) {
switch (event.getType()) {
case INITIALIZATION_FINISHED:
    System.out.println("Application "
            + event.getResourceConfig().getApplicationName()
            + " was initialized.");
    break;
case DESTROY_FINISHED:
    System.out.println("Application "
        + event.getResourceConfig().getApplicationName() + " destroyed.");
    break;
}
}

@Override
public RequestEventListener onRequest(RequestEvent requestEvent) {
	
	
	String requestPath = requestEvent.getUriInfo().getPath();
	System.out.println("PATH is " + requestPath);
	if (requestPath.equals("getCampaign")) requestGET++;
	System.out.println("The number of GET request is " + requestGET);
	
requestCnt++;
System.out.println("Request " + requestCnt + " started.");
	
// return the listener instance that will handle this request.
return new MyRequestEventListener(requestCnt);
}
}