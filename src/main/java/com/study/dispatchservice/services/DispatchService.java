package com.study.dispatchservice.services;

import com.study.dispatchservice.messages.OrderCreatedEvent;
import org.springframework.stereotype.Service;

@Service
public class DispatchService {

    public void process(OrderCreatedEvent payload) {
        //no-op
    }
}
