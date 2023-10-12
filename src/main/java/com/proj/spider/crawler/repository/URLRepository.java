package com.proj.spider.crawler.repository;

import com.proj.spider.crawler.model.URL;
import org.springframework.data.repository.CrudRepository;

public interface URLRepository extends CrudRepository<URL, Integer> {

   URL findByValue(String value);

}
