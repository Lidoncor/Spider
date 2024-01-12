package com.proj.spider.repository;

import com.proj.spider.crawler.model.LinkBetweenURL;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public interface LinkBetweenURLRepository extends CrudRepository<LinkBetweenURL, Integer> {
}
