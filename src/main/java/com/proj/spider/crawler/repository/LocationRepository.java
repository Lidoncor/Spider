package com.proj.spider.crawler.repository;

import com.proj.spider.crawler.model.Location;
import com.proj.spider.crawler.model.URL;
import org.springframework.data.repository.CrudRepository;

public interface LocationRepository extends CrudRepository<Location, Integer> {

    boolean existsByUrlValue(String url);

}
