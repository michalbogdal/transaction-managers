package com.transactions.playground.transactionsdemo.repo;

import com.transactions.playground.transactionsdemo.model.Event;

import java.util.List;

public interface EventRepo {

    Event save(Event event);

    List<Event> findAll();
}
