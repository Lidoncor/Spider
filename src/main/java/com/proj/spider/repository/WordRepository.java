package com.proj.spider.repository;

import com.proj.spider.searcher.dto.MatchedRow;
import com.proj.spider.crawler.model.Word;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRepository extends CrudRepository<Word, Integer> {
    Word findByValue(String value);

    Word findFirstByValue(String value);

    @Query(
            value = """
                    SELECT w0.url_id as urlId, w0.position as wordPos1, w1.position as wordPos2
                    FROM location w0 INNER JOIN location w1 on w0.url_id = w1.url_id
                    WHERE w0.word_id = ?1 AND w1.word_id = ?2""",
            nativeQuery = true)
    List<MatchedRow> getMatchingRows(Integer wordId1, Integer wordId2);

    @Query(
            value = """
                    select word.value from word join location on word.id = location.word_id where url_id = ?1""",
            nativeQuery = true)
    List<String> getPageWords(int id);

}
