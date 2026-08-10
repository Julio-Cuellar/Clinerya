package com.jclinical.users.domain.ports.out;

public interface EventPublisherPort {
    void publish(Object event);
}
