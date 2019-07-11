package com.transactions.playground.transactionsdemo.repo;

import com.transactions.playground.transactionsdemo.model.Event;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public class SessionEventRepository implements EventRepo {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public Event save(Event event) {

        Session currentSession = sessionFactory.getCurrentSession();
        currentSession.save(event);
        return event;
    }

    @Override
    public List<Event> findAll() {
        Session currentSession = sessionFactory.getCurrentSession();
        return currentSession.createQuery("SELECT e from Event e", Event.class).getResultList();
    }

}
