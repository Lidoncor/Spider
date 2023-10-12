package com.proj.spider.crawler.repository;

import com.proj.spider.crawler.model.LinkBetweenURL;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public interface LinkBetweenURLRepository extends CrudRepository<LinkBetweenURL, Integer> {

}
