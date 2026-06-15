package com.interview.platform.util;

import org.springframework.context.ApplicationEvent;
import com.interview.platform.models.Interview;

public class InterviewCreatedEvent extends ApplicationEvent{
     private final Interview interview;

    public InterviewCreatedEvent(Object source, Interview interview) {
        super(source);
        this.interview = interview;
    }

    public Interview getInterview() { return interview; }
    
}
