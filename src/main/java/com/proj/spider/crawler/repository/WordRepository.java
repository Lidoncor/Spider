package com.proj.spider.crawler.repository;

import com.proj.spider.crawler.model.Word;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WordRepository extends CrudRepository<Word, Integer> {

    Word findByValue(String value);

}
