package com.proj.spider.repository;

import com.proj.spider.crawler.model.PageRank;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRankRepository extends CrudRepository<PageRank, Integer> {
    @Modifying
    @Transactional
    @Query(
            value = """
                    INSERT INTO page_rank (url_id, score) SELECT id, 1.0 FROM url
                    """,
            nativeQuery = true)
    void defaultSettings();

    @Modifying
    @Transactional
    @Query(
            value = """
                    TRUNCATE page_rank
                    """,
            nativeQuery = true)
    void truncate();

    @Query(
            value = """
                    select id from url
                    """,
            nativeQuery = true)
    List<Integer> getAllUrlIds();

    @Query(
            value = """
                    SELECT from_url_id FROM link_between_url WHERE to_url_id = ?1
                    """,
            nativeQuery = true)
    List<Integer> getAllLinkedPagesIds(int urlId);

    @Query(
            value = """
                    SELECT score FROM page_rank WHERE url_id = ?1 LIMIT 1
                    """,
            nativeQuery = true)
    Double getScore(int urlId);

    @Query(
            value = """
                    SELECT count(*) FROM link_between_url WHERE from_url_id = ?1
                    """,
            nativeQuery = true)
    Integer linkingCount(int urlId);

    @Modifying
    @Transactional
    @Query(
            value = """
                    UPDATE page_rank SET score = ?1 WHERE url_id = ?2
                    """,
            nativeQuery = true)
    void updateScore(double score, int urlId);
}
