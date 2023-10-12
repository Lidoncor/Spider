package com.proj.spider.crawler.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "url")
@Getter
@Setter
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class URL {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private final String value;

    @OneToMany(mappedBy = "fromURL", cascade = CascadeType.MERGE)
    private Set<LinkBetweenURL> linkBetweenURLS = new HashSet<>();

}
