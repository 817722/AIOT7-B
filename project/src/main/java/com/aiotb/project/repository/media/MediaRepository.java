package com.aiotb.project.repository.media;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiotb.project.domain.media.Media;
import com.aiotb.project.domain.media.Media.ParentType;

public interface MediaRepository extends JpaRepository<Media, Long> {
    Optional<Media> findById(String id);

    List<Media> findByParentTypeAndParentId(ParentType parentType, Long parentId);

    Optional<Media> findByParentTypeAndParentIdAndMainTrue(ParentType parentType, Long parentId);

    // ── 기업별 접근 제한용 ──
    Optional<Media> findByNoAndCorpNo(Long no, Long corpNo);

    List<Media> findByParentTypeAndParentIdAndCorpNoOrderByNoDesc(ParentType parentType, Long parentId, Long corpNo);

    long countByParentTypeAndParentIdAndCorpNo(ParentType parentType, Long parentId, Long corpNo);
}
