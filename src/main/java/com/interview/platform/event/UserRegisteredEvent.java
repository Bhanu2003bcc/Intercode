package com.interview.platform.event;

import com.interview.platform.models.User;

public record UserRegisteredEvent(User user) {
    
}
