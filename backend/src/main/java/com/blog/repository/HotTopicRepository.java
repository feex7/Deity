package com.blog.repository;

import com.blog.entity.HotTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotTopicRepository extends JpaRepository<HotTopic, Long> {
}