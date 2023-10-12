package com.proj.spider.crawler.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "word")
@Getter
@Setter
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private final String value;

    @OneToMany(mappedBy = "word", cascade = CascadeType.MERGE)
    private Set<Location> location = new HashSet<>();

}
