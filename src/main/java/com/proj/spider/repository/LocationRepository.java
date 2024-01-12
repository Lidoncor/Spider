package com.proj.spider.repository;

import com.proj.spider.crawler.model.Location;
import org.springframework.data.repository.CrudRepository;

public interface LocationRepository extends CrudRepository<Location, Integer> {
    boolean existsByUrlValue(String url);
}
