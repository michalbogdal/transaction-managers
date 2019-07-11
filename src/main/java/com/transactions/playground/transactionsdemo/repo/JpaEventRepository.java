package com.transactions.playground.transactionsdemo.repo;

import com.transactions.playground.transactionsdemo.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEventRepository extends JpaRepository<Event, Long> {
}
