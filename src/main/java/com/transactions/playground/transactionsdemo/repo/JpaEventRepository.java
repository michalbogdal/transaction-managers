package com.transactions.playground.transactionsdemo.repo;

import com.transactions.playground.transactionsdemo.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface JpaEventRepository extends JpaRepository<Event, Long> {
}