package com.example.dogo.repository.Support;

import com.example.dogo.entity.Support.GuideImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuideImageRepository extends JpaRepository<GuideImage, Long> {
}
