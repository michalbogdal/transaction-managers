package com.transactions.playground.transactionsdemo.repo;

import com.transactions.playground.transactionsdemo.model.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JDBCEventRepository implements EventRepo {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Event save(Event event) {
        jdbcTemplate.update("insert into Event(id, description) values(?,?)",
                new Object[]{event.getId(), event.getDescription()});
        return event;
    }

    @Override
    public List<Event> findAll() {
        return jdbcTemplate.query("select id, description from event", getEventMapper());
    }

    private RowMapper<Event> getEventMapper() {
        return (resultSet, i) -> {
            Event event = new Event();
            event.setId(resultSet.getLong("id"));
            event.setDescription(resultSet.getString("description"));
            return event;
        };
    }

}
