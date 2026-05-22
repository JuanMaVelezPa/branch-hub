package com.jm.spring_web.application.email.port;

import com.jm.spring_web.application.email.model.EmailEnqueueCommand;

public interface EmailOutboxPort {

    void enqueue(EmailEnqueueCommand command);
}
