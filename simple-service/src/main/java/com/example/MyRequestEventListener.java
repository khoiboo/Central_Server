package com.example;

import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

public class MyRequestEventListener implements RequestEventListener {
    private final int requestNumber;
    private final long startTime;
    //private final int requestGET;
    
 
    public MyRequestEventListener(int requestNumber) {
        this.requestNumber = requestNumber;
        startTime = System.currentTimeMillis();
    }
 
    @Override
    public void onEvent(RequestEvent event) {
    	int requestGET = requestNumber;
    	
        switch (event.getType()) {
            case RESOURCE_METHOD_START:
                System.out.println("Resource method "
                    + event.getUriInfo().getMatchedResourceMethod()
                        .getHttpMethod()
                    + " started for request " + requestNumber);
                
                System.out.println("The request is of type " + event.getUriInfo().getMatchedResourceMethod().getHttpMethod());
                
                break;
            case FINISHED:
                System.out.println("Request " + requestNumber
                    + " finished. Processing time "
                    + (System.currentTimeMillis() - startTime) + " ms.");
                break;
        }
    }
}