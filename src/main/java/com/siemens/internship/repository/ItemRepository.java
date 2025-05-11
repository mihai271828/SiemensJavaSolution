package com.siemens.internship.repository;

import com.siemens.internship.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    @Query("SELECT id FROM Item")
    List<Long> findAllIds();
}
