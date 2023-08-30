package com.tagstory.core.domain.hashtag.service;

import com.tagstory.core.domain.hashtag.Hashtag;
import com.tagstory.core.domain.hashtag.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HashtagService {
    private final HashtagRepository hashtagRepository;

    public List<Hashtag> getHashtagList(List<String> hashtagStrList) {
        List<Hashtag> hashtagList = new ArrayList<>();
        for(String name : hashtagStrList) {
            Hashtag hashtag = Hashtag.create(name);
            hashtagList.add(hashtag);
        }
        return hashtagList;
    }
}
